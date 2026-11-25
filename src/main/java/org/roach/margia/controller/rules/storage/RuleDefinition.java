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
            @JsonSubTypes.Type(value = StateDef.Countdown.class, name = "COUNTDOWN") })
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

        record AlwaysTransition(List<ActionDef> actions, String nextState, Double x, Double y) implements StateDef {
            public AlwaysTransition {
                actions = actions != null ? actions : List.of();
            }

            public AlwaysTransition(List<ActionDef> actions, String nextState) {
                this(actions, nextState, null, null);
            }
        }

        record DelayedTransition(Object delay, String wrappedState, Double x, Double y) implements StateDef {
            public DelayedTransition(Object delay, String wrappedState) {
                this(delay, wrappedState, null, null);
            }
        }

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

        record Countdown(Object ticks, String nextState, Double x, Double y) implements StateDef {
            public Countdown(Object ticks, String nextState) {
                this(ticks, nextState, null, null);
            }
        }
    }

    public record ActionDef(String type, Map<String, Object> params) {
        public ActionDef {
            params = params != null ? params : Map.of();
        }
    }

    public record RangeTransition(int min, int max, String target) {
    }
}
