package org.roach.margia.controller.rules;

import java.util.*;
import java.util.function.Function;

import org.roach.margia.actions.*;
import org.roach.margia.controller.Musician;
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

    @FunctionalInterface
    private interface ActionBuilder {
        Function<MusicianMessage, List<MusicalAction>> build(Musician musician, ParamResolver params);
    }

    private static final Map<String, ActionSpec> ACTION_SPECS = Map.ofEntries(
            Map.entry("PLAY_CHORD", new ActionSpec((m, _) -> c -> List.of(new PlayChord(m, c)))),
            Map.entry("PLAY_CHORD_UP_INTERVAL", new ActionSpec(
                    List.of(new ActionParamSpec("interval", "5", "Semitones to transpose up (or $prop)")), (m, p) -> {
                        int interval = p.requireInt("interval");
                        return c -> List.of(new PlayChordUpInterval(m, c, interval));
                    })),
            Map.entry("PLAY_CHORD_DOWN_INTERVAL", new ActionSpec(
                    List.of(new ActionParamSpec("interval", "5", "Semitones to transpose down (or $prop)")), (m, p) -> {
                        int interval = p.requireInt("interval");
                        return c -> List.of(new PlayChordDownInterval(m, c, interval));
                    })),
            Map.entry("PLAY_CHORD_TWICE_LENGTH",
                    new ActionSpec((m, _) -> c -> List.of(new PlayChordTwiceLength(m, c)))),
            Map.entry("PLAY_CHORD_HALF_LENGTH", new ActionSpec((m, _) -> c -> List.of(new PlayChordHalfLength(m, c)))),
            Map.entry("PLAY_CHORD_UP_VELOCITY", new ActionSpec(
                    List.of(new ActionParamSpec("diff", "15", "Velocity increase (or $prop)")), (m, p) -> {
                        int diff = p.requireIntAny("diff", "delta");
                        return c -> List.of(new PlayChordUpVelocity(m, c, diff));
                    })),
            Map.entry("PLAY_CHORD_DOWN_VELOCITY", new ActionSpec(
                    List.of(new ActionParamSpec("diff", "15", "Velocity decrease (or $prop)")), (m, p) -> {
                        int diff = p.requireIntAny("diff", "delta");
                        return c -> List.of(new PlayChordDownVelocity(m, c, diff));
                    })),
            Map.entry("PLAY_NOTE_UP_OCTAVE", new ActionSpec((m, _) -> c -> List.of(new PlayNoteUpOctave(m, c)))),
            Map.entry("PLAY_PSEUDO_RANDOM_CHORD",
                    new ActionSpec(List.of(new ActionParamSpec("spread", "20", "MIDI pitch spread"),
                            new ActionParamSpec("num", "10", "Base MIDI note / offset"),
                            new ActionParamSpec("numNotes", "3", "Number of notes in chord")), (m, p) -> {
                                int spread = p.requireInt("spread");
                                int num = p.requireInt("num");
                                int numNotes = p.requireInt("numNotes");
                                return _ -> List.of(new PlayPseudoRandomChord(m, spread, num, numNotes));
                            })),
            Map.entry("PLAY_PSEUDO_RANDOM_CHORD_FLAVOR",
                    new ActionSpec(
                            List.of(new ActionParamSpec("spread", "20", "MIDI pitch spread"),
                                    new ActionParamSpec("num", "10", "Base MIDI note / offset"), new ActionParamSpec(
                                            "flavor", "MAJOR", "ChordFlavor enum (e.g. MAJOR, MINOR, DIMINISHED)")),
                            (m, p) -> {
                                int spread = p.requireInt("spread");
                                int num = p.requireInt("num");
                                ChordFlavor flavor = p.requireEnum("flavor", ChordFlavor.class);
                                return _ -> List.of(new PlayPseudoRandomChordFlavor(m, spread, num, flavor));
                            })),
            Map.entry("PLAY_RANDOM_CHORD",
                    new ActionSpec(
                            List.of(new ActionParamSpec("dice", "1d20", "Dice expression for pitch (e.g. 1d20)"),
                                    new ActionParamSpec("num", "10", "Base offset"), new ActionParamSpec(
                                            "maxNotesDiceDescription", "1d4", "Dice expression for note count")),
                            (m, p) -> {
                                String dice = p.requireString("dice");
                                int num = p.requireInt("num");
                                String maxNotesDice = p.requireString("maxNotesDiceDescription");
                                return _ -> List.of(new PlayRandomChord(m, dice, num, maxNotesDice));
                            })),
            Map.entry("REPEAT_LAST_CHORD", new ActionSpec((m, _) -> _ -> List.of(new RepeatLastChord(m)))),
            Map.entry("SET_LAST_CHORD", new ActionSpec((m, _) -> c -> List.of(new SetLastChord(m, c)))),
            Map.entry("REST_ONE_TICK", new ActionSpec((m, _) -> _ -> List.of(new RestOneTick(m)))),
            Map.entry("RESET_PLAYED_CHORDS", new ActionSpec((m, _) -> _ -> List.of(new ResetPlayedChords(m)))),
            Map.entry("MUTE_MUSICIAN",
                    new ActionSpec(List
                            .of(new ActionParamSpec("musicianId", "", "Optional target musician ID (blank = self)")),
                            (m, p) -> {
                                int targetId = p.optionalInt("musicianId", m.getId());
                                return _ -> List.of(new MuteMusician(targetId));
                            })),
            Map.entry("UNMUTE_MUSICIAN",
                    new ActionSpec(List
                            .of(new ActionParamSpec("musicianId", "", "Optional target musician ID (blank = self)")),
                            (m, p) -> {
                                int targetId = p.optionalInt("musicianId", m.getId());
                                return _ -> List.of(new UnmuteMusician(targetId));
                            })),
            Map.entry("CONSUME_MESSAGE", new ActionSpec((m, _) -> _ -> List.of(() -> m.getNextMessageReceived(true)))));

    private final RuleDefinition definition;
    private final List<RulePropertyDescriptor> descriptors;

    /**
     * @param definition the definition to use for defining the rule
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
     * @return the definition used to make this rule
     */
    public RuleDefinition getDefinition() { return definition; }

    @Override
    public void initActionsAfterMusicianAssigned() {
        Map<String, Object> resolvedProps = resolveRuleProperties();
        Map<String, MusicianState> states = new LinkedHashMap<>();
        Set<String> instantiating = new HashSet<>();

        // 1. Instantiate all states (resolving constructor dependencies recursively)
        for (String stateName : definition.states().keySet()) {
            getOrCreateState(stateName, states, instantiating, resolvedProps);
        }

        // 2. Wire mutable post-construction transitions
        definition.states().forEach((stateName, stateDef) -> wireTransitions(states.get(stateName), stateDef, states));

        super.initActionsAfterMusicianAssigned();
        this.startingState = requireTargetState(states, definition.startingState());
        this.state = this.startingState;
    }

    private Map<String, Object> resolveRuleProperties() {
        RuleOptions ruleOptions = Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions();
        Map<String, Object> resolvedProps = new LinkedHashMap<>();
        for (RulePropertyDescriptor desc : descriptors) {
            Object rawVal = ruleOptions.getRuleSpecificOptionOrDefault(desc.key(), desc.defaultValue());
            resolvedProps.put(desc.key(), coerceType(desc, rawVal));
        }
        return resolvedProps;
    }

    private MusicianState getOrCreateState(String stateName, Map<String, MusicianState> states,
            Set<String> instantiating, Map<String, Object> props) {

        MusicianState existing = states.get(stateName);
        if (existing != null) {
            return existing;
        }

        StateDef def = definition.states().get(stateName);
        if (def == null) {
            throw new IllegalStateException("Target state '" + stateName + "' not found in rule '" + getName() + "'");
        }
        if (!instantiating.add(stateName)) {
            throw new IllegalStateException(
                    "Circular constructor dependency involving state '" + stateName + "' in rule '" + getName() + "'");
        }

        ParamResolver resolver = new ParamResolver(Map.of(), props, "state '" + stateName + "'");
        MusicianState created = switch (def) {
        case StateDef.AlwaysTransition _ -> new AlwaysTransitionState(stateName);
        case StateDef.DelayedTransition d ->
            new DelayedTransitionState(stateName, resolver.resolveInt(d.delay(), "delay"));
        case StateDef.PseudoRandom p -> new PseudoRandomState(stateName, resolver.resolveInt(p.maxRoll(), "maxRoll"));
        case StateDef.DieRolling dr -> new DieRollingState(stateName, resolver.resolveString(dr.dice(), "dice"));
        case StateDef.NeverTransition _ -> new NeverTransitionState(stateName);
        case StateDef.NoteBased _ -> new NoteBasedTransitionState(stateName);
        case StateDef.Countdown c -> {
            MusicianState target = getOrCreateState(c.nextState(), states, instantiating, props);
            yield new CountdownState(stateName, resolver.resolveInt(c.ticks(), "ticks"), target);
        }
        case StateDef.Conditional cond ->
            createConditionalState(stateName, cond, states, instantiating, props, resolver);
        };

        instantiating.remove(stateName);
        attachActions(created, def.actions(), props, stateName);
        states.put(stateName, created);
        return created;
    }

    private FunctionBasedState createConditionalState(String stateName, StateDef.Conditional cond,
            Map<String, MusicianState> states, Set<String> instantiating, Map<String, Object> props,
            ParamResolver resolver) {

        MusicianState onTrue = getOrCreateState(cond.trueState(), states, instantiating, props);
        MusicianState onFalse = (cond.falseState() != null && !cond.falseState().isBlank())
                ? getOrCreateState(cond.falseState(), states, instantiating, props)
                : null;
        int threshold = cond.threshold() != null ? resolver.resolveInt(cond.threshold(), "threshold") : 0;
        String conditionKey = cond.condition().toUpperCase(Locale.ROOT);

        return new FunctionBasedState(stateName, (self, m) -> {
            boolean matched = switch (conditionKey) {
            case "CHORDS_PLAYED_GTE" -> m.getChordsIvePlayed() >= threshold;
            case "TICK_MODULO" -> threshold > 0 && (m.getCurrentTick() % threshold == 0);
            case "IS_MUTED" -> Options.getInstance().getMusicians().get(m.getId()).isMuted();
            case "HAS_LAST_CHORD" -> m.getMyLastChord() != null;
            case "QUEUE_EMPTY" -> m.getQueueSize() == 0;
            default -> throw new IllegalArgumentException(
                    "Unknown condition '" + cond.condition() + "' in state '" + stateName + "'");
            };
            return matched ? onTrue : (onFalse != null ? onFalse : self);
        });
    }

    private void attachActions(MusicianState stateInstance, List<ActionDef> actionDefs, Map<String, Object> props,
            String stateName) {

        if (actionDefs == null || actionDefs.isEmpty()) {
            return;
        }
        if (!(stateInstance instanceof AbstractMusicianState<?> absState)) {
            throw new IllegalStateException("State '" + stateName + "' (" + stateInstance.getClass().getSimpleName()
                    + ") does not support actions");
        }
        for (ActionDef act : actionDefs) {
            String typeKey = act.type().toUpperCase(Locale.ROOT);
            ActionSpec spec = ACTION_SPECS.get(typeKey);
            if (spec == null) {
                throw new IllegalArgumentException(
                        "Unknown action type '" + act.type() + "' in state '" + stateName + "'");
            }
            ParamResolver resolver = new ParamResolver(act.params(), props,
                    "action '" + act.type() + "' in state '" + stateName + "'");
            absState.withAction(spec.builder().build(musician, resolver));
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
        case StateDef.Countdown _,StateDef.Conditional _,StateDef.NeverTransition _ -> {
            // Handled during construction or has no outgoing transitions
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
        case RulePropertyDescriptor.StringListProperty _ -> switch (raw) {
        case List<?> list -> list;
        default -> List.of(raw.toString().split("\\s*,\\s*"));
        };
        case RulePropertyDescriptor.EnumProperty<?> _ -> raw;
        };
    }

    /**
     * Validates a {@link RuleDefinition} without requiring an assigned
     * {@link Musician}: checks property descriptors, starting state, state graph
     * references, circular constructor dependencies, action names/parameters, and
     * $property placeholders.
     * 
     * @param definition the definition to validate
     */
    public static void validate(RuleDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        if (definition.name() == null || definition.name().isBlank()) {
            throw new IllegalArgumentException("Rule name cannot be empty.");
        }
        if (definition.states() == null || definition.states().isEmpty()) {
            throw new IllegalArgumentException("Rule '" + definition.name() + "' must define at least one state.");
        }

        List<RulePropertyDescriptor> descriptors = definition.buildDescriptors();
        Map<String, Object> defaultProps = new LinkedHashMap<>();
        for (RulePropertyDescriptor desc : descriptors) {
            defaultProps.put(desc.key(), coerceType(desc, desc.defaultValue()));
        }

        DataDrivenMusicianRule dryRun = new DataDrivenMusicianRule(definition);
        Map<String, MusicianState> states = new LinkedHashMap<>();
        Set<String> instantiating = new HashSet<>();

        for (String stateName : definition.states().keySet()) {
            dryRun.getOrCreateState(stateName, states, instantiating, defaultProps);
        }
        definition.states()
                .forEach((stateName, stateDef) -> dryRun.wireTransitions(states.get(stateName), stateDef, states));

        dryRun.requireTargetState(states, definition.startingState());
    }

    /**
     * Helper for resolving YAML parameters and $property placeholders with
     * descriptive error messages.
     */
    private final class ParamResolver {
        private final Map<String, Object> params;
        private final Map<String, Object> props;
        private final String context;

        ParamResolver(Map<String, Object> params, Map<String, Object> props, String context) {
            this.params = params != null ? params : Map.of();
            this.props = props;
            this.context = context;
        }

        int requireInt(String key) {
            return resolveInt(requireRaw(key), key);
        }

        int requireIntAny(String... keys) {
            for (String key : keys) {
                if (params.containsKey(key)) {
                    return resolveInt(params.get(key), key);
                }
            }
            throw new IllegalArgumentException("Missing required parameter " + Arrays.toString(keys) + " for " + context
                    + " in rule '" + getName() + "'");
        }

        int optionalInt(String key, int defaultValue) {
            Object raw = params.get(key);
            return raw != null ? resolveInt(raw, key) : defaultValue;
        }

        String requireString(String key) {
            return resolveString(requireRaw(key), key);
        }

        <E extends Enum<E>> E requireEnum(String key, Class<E> enumClass) {
            Object resolved = resolvePlaceholder(requireRaw(key), key);
            if (resolved instanceof Collection<?> col && !col.isEmpty()) {
                resolved = col.iterator().next();
            }
            if (enumClass.isInstance(resolved)) {
                return enumClass.cast(resolved);
            }
            String val = String.valueOf(resolved).trim();
            try {
                return Enum.valueOf(enumClass, val.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid value '" + val + "' for parameter '" + key + "' in " + context + " of rule '" + getName() + "'", e);
            }
        }

        int resolveInt(Object raw, String paramName) {
            Object resolved = resolvePlaceholder(raw, paramName);
            if (resolved instanceof Number n) {
                return n.intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(resolved));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Expected integer for '" + paramName + "' in " + context
                        + " of rule '" + getName() + "', got: " + resolved, e);
            }
        }

        String resolveString(Object raw, String paramName) {
            Object resolved = resolvePlaceholder(raw, paramName);
            if (resolved == null) {
                throw new IllegalArgumentException("Missing required string for '" + paramName + "' in " + context
                        + " of rule '" + getName() + "'");
            }
            return String.valueOf(resolved);
        }

        private Object requireRaw(String key) {
            if (!params.containsKey(key) || params.get(key) == null) {
                throw new IllegalArgumentException(
                        "Missing required parameter '" + key + "' for " + context + " in rule '" + getName() + "'");
            }
            return params.get(key);
        }

        private Object resolvePlaceholder(Object raw, String paramName) {
            if (raw instanceof String s && s.startsWith("$")) {
                String propKey = s.substring(1);
                if (!props.containsKey(propKey)) {
                    throw new IllegalArgumentException("Unknown property placeholder '$" + propKey + "' for '"
                            + paramName + "' in " + context + " of rule '" + getName() + "'");
                }
                return props.get(propKey);
            }
            return raw;
        }
    }

    /**
     * Represents parameter specs for a given type of action
     * 
     * @param name         name of action
     * @param defaultValue default value of parameter
     * @param description  description of parameter
     */
    public record ActionParamSpec(String name, String defaultValue, String description) {
    }

    private record ActionSpec(List<ActionParamSpec> params, ActionBuilder builder) {
        ActionSpec(ActionBuilder builder) {
            this(List.of(), builder);
        }
    }

    /**
     * @return a list of names of all supported actions
     */
    public static List<String> supportedActionNames() {
        return ACTION_SPECS.keySet().stream().sorted().toList();
    }

    /**
     * @param actionType the type of action
     * @return the parameter specifications for this type of action
     */
    public static List<ActionParamSpec> getActionParamSpecs(String actionType) {
        if (actionType == null) {
            return List.of();
        }
        ActionSpec spec = ACTION_SPECS.get(actionType.toUpperCase(Locale.ROOT));
        return spec != null ? spec.params() : List.of();
    }

}