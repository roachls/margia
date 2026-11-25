package org.roach.margia.controller.rules;

import java.util.*;
import java.util.function.Function;

import org.roach.margia.actions.*;
import org.roach.margia.controller.rules.states.*;
import org.roach.margia.controller.rules.storage.RuleDefinition;
import org.roach.margia.controller.rules.storage.RuleDefinition.*;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;

/**
 * A {@link MusicianRule} whose states, transitions, and actions are loaded from
 * a {@link RuleDefinition} (deserialized from YAML).
 */
public class DataDrivenMusicianRule extends AbstractMusicianRule {

    private final RuleDefinition definition;
    private final List<RulePropertyDescriptor> descriptors;

    /**
     * @param definition the rule definition to create this rule from
     */
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

        // 0. Resolve current property values from RuleOptions (falling back to
        // descriptor defaults)
        Map<String, Object> resolvedProps = new LinkedHashMap<>();
        for (RulePropertyDescriptor desc : descriptors) {
            Object rawVal = ruleOptions.getRuleSpecificOptionOrDefault(desc.key(), desc.defaultValue());
            resolvedProps.put(desc.key(), coerceType(desc, rawVal));
        }

        // Pass 1: Instantiate states that don't require target MusicianState references
        // in their constructor
        Map<String, MusicianState> states = new LinkedHashMap<>();
        definition.states().forEach((stateName, stateDef) -> {
            if (!(stateDef instanceof StateDef.Countdown) && !(stateDef instanceof StateDef.Conditional)) {
                MusicianState stateInstance = createStateInstance(stateName, stateDef, resolvedProps);
                attachActions(stateInstance, stateDef.actions(), resolvedProps);
                states.put(stateName, stateInstance);
            }
        });

        // Pass 2: Instantiate constructor-wired states (Countdown, Conditional) now
        // that Pass 1 targets exist
        definition.states().forEach((stateName, stateDef) -> {
            if (stateDef instanceof StateDef.Countdown c) {
                MusicianState target = requireTargetState(states, c.nextState());
                CountdownState countdown = new CountdownState(stateName, resolveInt(c.ticks(), resolvedProps), target);
                attachActions(countdown, stateDef.actions(), resolvedProps);
                states.put(stateName, countdown);
            } else if (stateDef instanceof StateDef.Conditional cond) {
                MusicianState onTrue = requireTargetState(states, cond.trueState());
                MusicianState onFalse = (cond.falseState() != null && !cond.falseState().isBlank())
                        ? requireTargetState(states, cond.falseState())
                        : null;
                int threshold = cond.threshold() != null ? resolveInt(cond.threshold(), resolvedProps) : 0;

                FunctionBasedState fnState = new FunctionBasedState(stateName, (self, m) -> {
                    boolean matched = switch (cond.condition().toUpperCase()) {
                    case "CHORDS_PLAYED_GTE" -> m.getChordsIvePlayed() >= threshold;
                    case "TICK_MODULO" -> threshold > 0 && (m.getCurrentTick() % threshold == 0);
                    case "IS_MUTED" -> Options.getInstance().getMusicians().get(m.getId()).isMuted();
                    case "HAS_LAST_CHORD" -> m.getMyLastChord() != null;
                    case "QUEUE_EMPTY" -> m.getQueueSize() == 0;
                    default -> throw new IllegalArgumentException("Unknown condition: " + cond.condition());
                    };
                    return matched ? onTrue : (onFalse != null ? onFalse : self);
                });
                attachActions(fnState, stateDef.actions(), resolvedProps);
                states.put(stateName, fnState);
            }
        });

        // Pass 3: Wire mutable transitions (AlwaysTransition, DelayedTransition,
        // PseudoRandom, DieRolling)
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
        case StateDef.AlwaysTransition _ -> new AlwaysTransitionState(name);
        case StateDef.DelayedTransition d -> new DelayedTransitionState(name, resolveInt(d.delay(), props));
        case StateDef.PseudoRandom p -> new PseudoRandomState(name, resolveInt(p.maxRoll(), props));
        case StateDef.DieRolling dr -> new DieRollingState(name, resolveString(dr.dice(), props));
        case StateDef.NeverTransition _ -> new NeverTransitionState(name);
        case StateDef.NoteBased _ -> new NoteBasedTransitionState(name);
        default -> throw new IllegalStateException(
                "State type must be instantiated in Pass 2: " + def.getClass().getSimpleName());
        };
    }

    private void attachActions(MusicianState stateInstance, List<ActionDef> actionDefs, Map<String, Object> props) {
        if (actionDefs.isEmpty()) {
            return;
        }
        List<Function<MusicianMessage, List<MusicalAction>>> actionFns = buildActions(actionDefs, props);
        if (stateInstance instanceof AbstractMusicianState<?> absState) {
            actionFns.forEach(absState::withAction);
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
                    random.withStateTransition(new NumericRange(rt.min(), rt.max()),
                            requireTargetState(states, rt.target()));
                }
            }
        }
        case StateDef.DieRolling dr -> {
            if (stateInstance instanceof DieRollingState dieRolling) {
                for (RangeTransition rt : dr.rangeTransitions()) {
                    dieRolling.withStateTransition(new NumericRange(rt.min(), rt.max()),
                            requireTargetState(states, rt.target()));
                }
            }
        }
        case StateDef.NoteBased nb -> {
            if (stateInstance instanceof NoteBasedTransitionState noteState) {
                for (NoteTransition nt : nb.noteTransitions()) {
                    noteState.withStateTransition(nt.notes(), requireTargetState(states, nt.target()));
                }
            }
        }
        default -> {
            // Pass 2 states (Countdown, Conditional) are already wired via constructor
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
                case "PLAY_CHORD_UP_INTERVAL" -> {
                    int interval = resolveInt(act.params().get("interval"), props);
                    yield c -> List.of(new PlayChordUpInterval(musician, c, interval));
                }
                case "PLAY_CHORD_DOWN_INTERVAL" -> {
                    int interval = resolveInt(act.params().get("interval"), props);
                    yield c -> List.of(new PlayChordDownInterval(musician, c, interval));
                }
                case "PLAY_CHORD_TWICE_LENGTH" -> c -> List.of(new PlayChordTwiceLength(musician, c));
                case "PLAY_CHORD_HALF_LENGTH" -> c -> List.of(new PlayChordHalfLength(musician, c));
                case "PLAY_CHORD_UP_VELOCITY" -> {
                    Object rawDiff = act.params().getOrDefault("diff", act.params().get("delta"));
                    int diff = resolveInt(rawDiff, props);
                    yield c -> List.of(new PlayChordUpVelocity(musician, c, diff));
                }
                case "PLAY_CHORD_DOWN_VELOCITY" -> {
                    Object rawDiff = act.params().getOrDefault("diff", act.params().get("delta"));
                    int diff = resolveInt(rawDiff, props);
                    yield c -> List.of(new PlayChordDownVelocity(musician, c, diff));
                }
                case "PLAY_NOTE_UP_OCTAVE" -> c -> List.of(new PlayNoteUpOctave(musician, c));
                case "PLAY_PSEUDO_RANDOM_CHORD" -> {
                    int spread = resolveInt(act.params().get("spread"), props);
                    int num = resolveInt(act.params().get("num"), props);
                    int numNotes = resolveInt(act.params().get("numNotes"), props);
                    yield _ -> List.of(new PlayPseudoRandomChord(musician, spread, num, numNotes));
                }
                case "PLAY_PSEUDO_RANDOM_CHORD_FLAVOR" -> {
                    int spread = resolveInt(act.params().get("spread"), props);
                    int num = resolveInt(act.params().get("num"), props);
                    String flavorStr = resolveString(act.params().get("flavor"), props);
                    ChordFlavor flavor = ChordFlavor.valueOf(flavorStr.toUpperCase());
                    yield _ -> List.of(new PlayPseudoRandomChordFlavor(musician, spread, num, flavor));
                }
                case "PLAY_RANDOM_CHORD" -> {
                    String dice = resolveString(act.params().get("dice"), props);
                    int num = resolveInt(act.params().get("num"), props);
                    String maxNotesDice = resolveString(act.params().get("maxNotesDiceDescription"), props);
                    yield _ -> List.of(new PlayRandomChord(musician, dice, num, maxNotesDice));
                }
                case "REPEAT_LAST_CHORD" -> _ -> List.of(new RepeatLastChord(musician));
                case "SET_LAST_CHORD" -> c -> List.of(new SetLastChord(musician, c));
                case "REST_ONE_TICK" -> _ -> List.of(new RestOneTick(musician));
                case "RESET_PLAYED_CHORDS" -> _ -> List.of(new ResetPlayedChords(musician));
                case "MUTE_MUSICIAN" -> {
                    Object rawId = act.params().get("musicianId");
                    int targetId = (rawId != null) ? resolveInt(rawId, props) : musician.getId();
                    yield _ -> List.of(new MuteMusician(targetId));
                }
                case "UNMUTE_MUSICIAN" -> {
                    Object rawId = act.params().get("musicianId");
                    int targetId = (rawId != null) ? resolveInt(rawId, props) : musician.getId();
                    yield _ -> List.of(new UnmuteMusician(targetId));
                }
                case "CONSUME_MESSAGE" -> _ -> List.of(() -> musician.getNextMessageReceived(true));
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

    private String resolveString(Object raw, Map<String, Object> props) {
        Object resolved = resolvePlaceholder(raw, props);
        if (resolved == null) {
            throw new IllegalArgumentException("Missing required string parameter in rule '" + getName() + "'");
        }
        return String.valueOf(resolved);
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

    private static Object coerceType(RulePropertyDescriptor desc, Object raw) {
        if (raw == null) {
            return desc.defaultValue();
        }
        return switch (desc) {
        case RulePropertyDescriptor.IntProperty _ ->
            raw instanceof Number n ? n.intValue() : Integer.parseInt(raw.toString());
        case RulePropertyDescriptor.DoubleProperty _ ->
            raw instanceof Number n ? n.doubleValue() : Double.parseDouble(raw.toString());
        case RulePropertyDescriptor.BooleanProperty _ ->
            raw instanceof Boolean b ? b : Boolean.parseBoolean(raw.toString());
        case RulePropertyDescriptor.StringProperty _ -> raw.toString();
        case RulePropertyDescriptor.StringListProperty _ -> raw.toString();
        case RulePropertyDescriptor.EnumProperty<?> _ -> raw;
        };
    }
}
