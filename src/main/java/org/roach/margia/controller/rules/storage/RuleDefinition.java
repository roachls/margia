package org.roach.margia.controller.rules.storage;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import org.roach.margia.controller.rules.RulePropertyDescriptor;

/**
 * Represents a rule definition that may be loaded from YAML and edited by the
 * user
 * 
 * @param name          name of rule
 * @param startingState starting state of rule
 * @param properties    property definitions of rule
 * @param states        states a rule may be in
 */
public record RuleDefinition(String name, String startingState, List<PropertyDef> properties,
        Map<String, StateDef> states) {

    /**
     * @param name          name of rule
     * @param startingState starting state of rule
     * @param properties    property definitions of rule
     * @param states        states a rule may be in
     */
    public RuleDefinition {
        properties = properties != null ? List.copyOf(properties) : List.of();
        states = states != null ? Collections.unmodifiableMap(new LinkedHashMap<>(states)) : Map.of();
    }

    /**
     * @return the list of rule descriptors
     */
    public List<RulePropertyDescriptor> buildDescriptors() {
        return properties.stream().map(PropertyDef::toDescriptor).toList();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = PropertyDef.IntProp.class, name = "INT"),
            @JsonSubTypes.Type(value = PropertyDef.DoubleProp.class, name = "DOUBLE"),
            @JsonSubTypes.Type(value = PropertyDef.BooleanProp.class, name = "BOOLEAN"),
            @JsonSubTypes.Type(value = PropertyDef.StringProp.class, name = "STRING"),
            @JsonSubTypes.Type(value = PropertyDef.StringListProp.class, name = "STRING_LIST") })
    public sealed interface PropertyDef {
        /**
         * @return this {@link PropertyDef} converted to a
         *         {@link RulePropertyDescriptor}
         */
        RulePropertyDescriptor toDescriptor();

        /**
         * An integer property
         * 
         * @param key          the key of the property
         * @param label        the label for the property
         * @param defaultValue the default value
         * @param min          the minimum value
         * @param max          the maximum value
         * @param step         the step value (for spinners)
         */
        record IntProp(String key, String label, int defaultValue, int min, int max, Integer step)
                implements PropertyDef {
            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.IntProperty(key, label, defaultValue, min, max,
                        step != null ? step : 1);
            }
        }

        /**
         * A double property
         * 
         * @param key          the key of the property
         * @param label        the label for the property
         * @param defaultValue the default value
         * @param min          the minimum value
         * @param max          the maximum value
         * @param step         the step value (for spinners)
         */
        record DoubleProp(String key, String label, double defaultValue, double min, double max, Double step)
                implements PropertyDef {
            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.DoubleProperty(key, label, defaultValue, min, max,
                        step != null ? step : 0.1);
            }
        }

        /**
         * A boolean property
         * 
         * @param key          the key of the property
         * @param label        the label for the property
         * @param defaultValue the default value
         */
        record BooleanProp(String key, String label, boolean defaultValue) implements PropertyDef {
            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.BooleanProperty(key, label, defaultValue);
            }
        }

        /**
         * A String property
         * 
         * @param key          the key of the property
         * @param label        the label for the property
         * @param defaultValue the default value for the property
         */
        record StringProp(String key, String label, String defaultValue) implements PropertyDef {
            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.StringProperty(key, label, defaultValue);
            }
        }

        /**
         * A String property that has a list of values to choose from
         * 
         * @param key          the key of the property
         * @param label        the label for the property
         * @param defaultValue the default value for the property
         * @param values       the list of available values (for combo boxes)
         */
        record StringListProp(String key, String label, String defaultValue, Set<String> values)
                implements PropertyDef {
            public StringListProp {
                values = values != null ? Collections.unmodifiableSet(new LinkedHashSet<>(values)) : Set.of();
            }

            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.StringListProperty(key, label, defaultValue, values);
            }
        }

    }

    /**
     * Represents a state definition
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = StateDef.AlwaysTransition.class, name = "ALWAYS_TRANSITION"),
            @JsonSubTypes.Type(value = StateDef.DelayedTransition.class, name = "DELAYED_TRANSITION"),
            @JsonSubTypes.Type(value = StateDef.PseudoRandom.class, name = "PSEUDO_RANDOM"),
            @JsonSubTypes.Type(value = StateDef.DieRolling.class, name = "DIE_ROLLING"),
            @JsonSubTypes.Type(value = StateDef.Countdown.class, name = "COUNTDOWN"),
            @JsonSubTypes.Type(value = StateDef.Conditional.class, name = "CONDITIONAL"),
            @JsonSubTypes.Type(value = StateDef.NeverTransition.class, name = "NEVER_TRANSITION"),
            @JsonSubTypes.Type(value = StateDef.NoteBased.class, name = "NOTE_BASED") })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public sealed interface StateDef {
        /**
         * @return list of actions available
         */
        default List<ActionDef> actions() {
            return List.of();
        }

        /**
         * @return the x-position (for the state graph view)
         */
        Double x();

        /**
         * @return the y-position (for the state graph view)
         */
        Double y();

        /**
         * Always transition to the next state (unconditional)
         * 
         * @param actions   actions to perform
         * @param nextState state to transition to
         * @param x         x-position on screen
         * @param y         y-position on screen
         */
        record AlwaysTransition(List<ActionDef> actions, String nextState, Double x, Double y) implements StateDef {
            public AlwaysTransition {
                actions = actions != null ? actions : List.of();
            }

            public AlwaysTransition(List<ActionDef> actions, String nextState) {
                this(actions, nextState, null, null);
            }
        }

        /**
         * Transitions to wrappedState after a given delay
         * 
         * @param delay        the delay time
         * @param wrappedState the state to transition to
         * @param x            the x-position on screen
         * @param y            the y-position on screen
         */
        record DelayedTransition(Object delay, List<ActionDef> actions, String wrappedState, Double x, Double y)
                implements StateDef {
            public DelayedTransition {
                actions = actions != null ? List.copyOf(actions) : List.of();
            }

            public DelayedTransition(Object delay, String wrappedState, Double x, Double y) {
                this(delay, List.of(), wrappedState, x, y);
            }

            public DelayedTransition(Object delay, String wrappedState) {
                this(delay, List.of(), wrappedState, null, null);
            }
        }

        /**
         * Transitions to the next state based on a calculated pseudo-random number. The
         * PSN is based on the musician ID, the current tick number, and the musician's
         * last notes played (if any).
         * 
         * @param maxRoll          The PSN is modded with this number
         * @param actions          actions to perform
         * @param rangeTransitions possible transitions
         * @param x                x-position on screen
         * @param y                y-position on screen
         */
        record PseudoRandom(Object maxRoll, List<ActionDef> actions, List<RangeTransition> rangeTransitions, Double x,
                Double y) implements StateDef {
            public PseudoRandom {
                actions = actions != null ? actions : List.of();
                rangeTransitions = rangeTransitions != null ? rangeTransitions : List.of();
            }

            public PseudoRandom(Object maxRoll, List<ActionDef> actions, List<RangeTransition> rangeTransitions) {
                this(maxRoll, actions, rangeTransitions, null, null);
            }
        }

        /**
         * Similar to DelayedTransition, but it counts down each time it performs an
         * action. When the countdown reaches 0, it transitions to the next state.
         * 
         * @param ticks     initial countdown state
         * @param nextState state to transition to
         * @param x         x-position on screen
         * @param y         y-position on screen
         */
        record Countdown(Object ticks, List<ActionDef> actions, String nextState, Double x, Double y)
                implements StateDef {
            public Countdown {
                actions = actions != null ? List.copyOf(actions) : List.of();
            }

            public Countdown(Object ticks, String nextState, Double x, Double y) {
                this(ticks, List.of(), nextState, x, y);
            }

            public Countdown(Object ticks, String nextState) {
                this(ticks, List.of(), nextState, null, null);
            }
        }

        /**
         * Rolls a die and decides whether to transition to a different state based on
         * the roll
         * 
         * @param dice             dice expression (e.g., "1d20")
         * @param actions          actions to perform
         * @param rangeTransitions possible transitions
         * @param x                x-position on screen
         * @param y                y-position on screen
         */
        record DieRolling(String dice, List<ActionDef> actions, List<RangeTransition> rangeTransitions, Double x,
                Double y) implements StateDef {
            public DieRolling {
                actions = actions != null ? actions : List.of();
                rangeTransitions = rangeTransitions != null ? rangeTransitions : List.of();
            }

            public DieRolling(String dice, List<ActionDef> actions, List<RangeTransition> rangeTransitions) {
                this(dice, actions, rangeTransitions, null, null);
            }
        }

        /**
         * Transitions to either {@code trueState} or {@code falseState} depending on
         * whether the condition is true or false.
         * 
         * @param condition  the condition to check
         * @param threshold  the threshold for certain built-in conditions
         * @param actions    actions to perform
         * @param trueState  state to transition to if condition is true
         * @param falseState state to transition to if condition is false
         * @param x          x-position on screen
         * @param y          y-position on screen
         */
        record Conditional(String condition, Object threshold, List<ActionDef> actions, String trueState,
                String falseState, Double x, Double y) implements StateDef {
            public Conditional {
                actions = actions != null ? actions : List.of();
            }

            public Conditional(String condition, Object threshold, List<ActionDef> actions, String trueState,
                    String falseState) {
                this(condition, threshold, actions, trueState, falseState, null, null);
            }
        }

        /**
         * Once in this state, no transition is possible
         * 
         * @param actions actions to perform
         * @param x       x-position on screen
         * @param y       y-position on screen
         */
        record NeverTransition(List<ActionDef> actions, Double x, Double y) implements StateDef {
            public NeverTransition {
                actions = actions != null ? actions : List.of();
            }

            public NeverTransition(List<ActionDef> actions) {
                this(actions, null, null);
            }
        }

        /**
         * Transitions based on notes heard
         * 
         * @param actions         actions to perform
         * @param noteTransitions list of {@link NoteTransition NoteTransitions}
         * @param x               x-position on screen
         * @param y               y-position on screen
         */
        record NoteBased(List<ActionDef> actions, List<NoteTransition> noteTransitions, Double x, Double y)
                implements StateDef {
            public NoteBased {
                actions = actions != null ? actions : List.of();
                noteTransitions = noteTransitions != null ? noteTransitions : List.of();
            }

            public NoteBased(List<ActionDef> actions, List<NoteTransition> noteTransitions) {
                this(actions, noteTransitions, null, null);
            }
        }
    }

    /**
     * @param notes  set of notes that will trigger the transition
     * @param target target state if these notes are heard
     */
    public record NoteTransition(Set<Integer> notes, String target) {
        /**
         * @param notes  set of notes that will trigger the transition
         * @param target target state if these notes are heard
         */
        public NoteTransition {
            notes = notes != null ? Set.copyOf(notes) : Set.of();
        }
    }

    /**
     * An Action definition
     * 
     * @param type   type of action to perform
     * @param params parameters of action
     */
    public record ActionDef(String type, Map<String, Object> params) {
        /**
         * @param type   type of action to perform
         * @param params parameters of action
         */
        public ActionDef {
            params = params != null ? Collections.unmodifiableMap(new LinkedHashMap<>(params)) : Map.of();
        }
    }

    /**
     * A transition based on a range
     * 
     * @param min    minimum of range
     * @param max    maximum of range
     * @param target state to transition to if range is met
     */
    public record RangeTransition(int min, int max, String target) {
    }
}
