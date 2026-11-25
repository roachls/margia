package org.roach.margia.controller.rules;

import java.util.*;
import java.util.function.Function;

import org.roach.margia.actions.*;
import org.roach.margia.controller.rules.states.*;
import org.roach.margia.controller.rules.storage.RuleDefinition;
import org.roach.margia.controller.rules.storage.RuleDefinition.*;
import org.roach.margia.model.MusicianMessage;
import org.roach.margia.model.RuleOptions;
import org.roach.margia.storage.Options;

/**
 * A {@link MusicianRule} whose states, transitions, and actions are loaded from
 * a {@link RuleDefinition} (deserialized from YAML).
 */
public class DataDrivenMusicianRule extends AbstractMusicianRule {

    private final RuleDefinition definition;
    private final List<RulePropertyDescriptor> descriptors;

    public DataDrivenMusicianRule(RuleDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
        this.descriptors = definition.buildDescriptors();
    }

    @Override
    public String getName() { return definition.name(); }

    @Override
    public List<RulePropertyDescriptor> getPropertyDescriptors() { return descriptors; }

    @Override
    public AbstractMusicianRule copy() {
        return new DataDrivenMusicianRule(this.definition);
    }

    /**
     * @return the underlying {@link RuleDefinition} (useful for in-app editing)
     */
    public RuleDefinition getDefinition() { return definition; }

    @Override
    public void initActionsAfterMusicianAssigned() {
        RuleOptions ruleOptions = Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions();

        // 1. Resolve current property values from RuleOptions (falling back to
        // descriptor defaults)
        Map<String, Object> resolvedProps = new LinkedHashMap<>();
        for (RulePropertyDescriptor desc : descriptors) {
            Object rawVal = ruleOptions.getRuleSpecificOptionOrDefault(desc.key(), desc.defaultValue());
            resolvedProps.put(desc.key(), coerceType(desc, rawVal));
        }

        // 2. Pass 1: Instantiate all non-Countdown states first so CountdownState
        // targets exist
        Map<String, MusicianState> states = new LinkedHashMap<>();
        definition.states().forEach((stateName, stateDef) -> {
            if (!(stateDef instanceof StateDef.Countdown)) {
                MusicianState stateInstance = createStateInstance(stateName, stateDef, resolvedProps);
                attachActions(stateInstance, stateDef.actions(), resolvedProps);
                states.put(stateName, stateInstance);
            }
        });

        // 3. Pass 2: Instantiate CountdownState instances now that their target states
        // exist
        definition.states().forEach((stateName, stateDef) -> {
            if (stateDef instanceof StateDef.Countdown c) {
                MusicianState target = requireTargetState(states, c.nextState());
                CountdownState countdown = new CountdownState(stateName, resolveInt(c.ticks(), resolvedProps), target);
                attachActions(countdown, stateDef.actions(), resolvedProps);
                states.put(stateName, countdown);
            }
        });

        // 4. Pass 3: Wire mutable transitions (AlwaysTransition, DelayedTransition,
        // PseudoRandom)
        definition.states().forEach((stateName, stateDef) -> {
            MusicianState stateInstance = states.get(stateName);
            wireTransitions(stateInstance, stateDef, states);
        });

        super.initActionsAfterMusicianAssigned();
        this.startingState = states.get(definition.startingState());
        if (this.startingState == null) {
            throw new IllegalStateException(
                    "Starting state '" + definition.startingState() + "' not found in rule '" + getName() + "'");
        }
        this.state = this.startingState;
    }

    private MusicianState createStateInstance(String name, StateDef def, Map<String, Object> props) {
        return switch (def) {
        case StateDef.AlwaysTransition a -> new AlwaysTransitionState(name);
        case StateDef.DelayedTransition d -> new DelayedTransitionState(name, resolveInt(d.delay(), props));
        case StateDef.PseudoRandom p -> new PseudoRandomState(name, resolveInt(p.maxRoll(), props));
        case StateDef.Countdown c -> throw new IllegalStateException("CountdownState must be instantiated in Pass 2");
        };
    }

    private void attachActions(MusicianState stateInstance, List<ActionDef> actionDefs, Map<String, Object> props) {
        if (actionDefs.isEmpty()) {
            return;
        }
        List<Function<MusicianMessage, List<MusicalAction>>> actionFns = buildActions(actionDefs, props);
        if (stateInstance instanceof AlwaysTransitionState always) {
            actionFns.forEach(always::withAction);
        } else if (stateInstance instanceof PseudoRandomState random) {
            actionFns.forEach(random::withAction);
        }
    }

    private void wireTransitions(MusicianState stateInstance, StateDef def, Map<String, MusicianState> states) {
        switch (def) {
            case StateDef.AlwaysTransition a -> {
                if (stateInstance instanceof AlwaysTransitionState always && a.nextState() != null) {
                    always.setToState(requireTargetState(states, a.nextState()));
                }
            }
            case StateDef.DelayedTransition d -> {
                if (stateInstance instanceof DelayedTransitionState delayed && d.wrappedState() != null) {
                    delayed.withWrappedState(requireTargetState(states, d.wrappedState()));
                }
            }
            case StateDef.PseudoRandom p -> {
                if (stateInstance instanceof PseudoRandomState random) {
                    for (RangeTransition rt : p.rangeTransitions()) {
                        random.withStateTransition(
                                new NumericRange(rt.min(), rt.max()),
                                requireTargetState(states, rt.target()));
                    }
                }
            }
            case StateDef.Countdown c -> {
                // Already wired via constructor in Pass 2
            }
        }
    }

    private MusicianState requireTargetState(Map<String, MusicianState> states, String targetName) {
        MusicianState target = states.get(targetName);
        if (target == null) {
            throw new IllegalStateException("Target state '" + targetName + "' not found in rule '" + getName() + "'");
        }
        return target;
    }

    private List<Function<MusicianMessage, List<MusicalAction>>> buildActions(List<ActionDef> actionDefs,
            Map<String, Object> props) {
        return actionDefs.stream()
                .<Function<MusicianMessage, List<MusicalAction>>>map(act -> switch (act.type().toUpperCase()) {
                case "PLAY_CHORD" -> c -> List.of(new PlayChord(musician, c));
                case "PLAY_CHORD_DOWN_INTERVAL" -> {
                    int interval = resolveInt(act.params().get("interval"), props);
                    yield c -> List.of(new PlayChordDownInterval(musician, c, interval));
                }
                case "PLAY_CHORD_TWICE_LENGTH" -> c -> List.of(new PlayChordTwiceLength(musician, c));
                case "PLAY_CHORD_HALF_LENGTH" -> c -> List.of(new PlayChordHalfLength(musician, c));
                case "PLAY_CHORD_UP_VELOCITY" -> {
                    int delta = resolveInt(act.params().get("delta"), props);
                    yield c -> List.of(new PlayChordUpVelocity(musician, c, delta));
                }
                case "PLAY_CHORD_DOWN_VELOCITY" -> {
                    int delta = resolveInt(act.params().get("delta"), props);
                    yield c -> List.of(new PlayChordDownVelocity(musician, c, delta));
                }
                default -> throw new IllegalArgumentException("Unknown action type: " + act.type());
                }).toList();
    }

    private int resolveInt(Object raw, Map<String, Object> props) {
        Object resolved = resolvePlaceholder(raw, props);
        if (resolved instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(resolved));
    }

    private Object resolvePlaceholder(Object raw, Map<String, Object> props) {
        if (raw instanceof String s && s.startsWith("$")) {
            String key = s.substring(1);
            if (!props.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Unknown property placeholder '$" + key + "' in rule '" + getName() + "'");
            }
            return props.get(key);
        }
        return raw;
    }

    private Object coerceType(RulePropertyDescriptor desc, Object raw) {
        if (raw == null) {
            return desc.defaultValue();
        }
        return switch (desc) {
        case RulePropertyDescriptor.IntProperty p ->
            raw instanceof Number n ? n.intValue() : Integer.parseInt(raw.toString());
        case RulePropertyDescriptor.DoubleProperty p ->
            raw instanceof Number n ? n.doubleValue() : Double.parseDouble(raw.toString());
        case RulePropertyDescriptor.BooleanProperty p ->
            raw instanceof Boolean b ? b : Boolean.parseBoolean(raw.toString());
        case RulePropertyDescriptor.StringProperty p -> raw.toString();
        case RulePropertyDescriptor.StringListProperty p -> raw.toString();
        case RulePropertyDescriptor.EnumProperty<?> p -> raw;
        };
    }
}
