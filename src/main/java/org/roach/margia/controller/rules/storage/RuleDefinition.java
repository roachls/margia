package org.roach.margia.controller.rules.storage;

import java.util.*;
import com.fasterxml.jackson.annotation.*;
import org.roach.margia.controller.rules.RulePropertyDescriptor;

public record RuleDefinition(String name, String startingState, List<PropertyDef> properties,
        Map<String, StateDef> states) {

    public RuleDefinition {
        properties = properties != null ? List.copyOf(properties) : List.of();
        states = states != null ? Collections.unmodifiableMap(new LinkedHashMap<>(states)) : Map.of();
    }

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
        RulePropertyDescriptor toDescriptor();

        record IntProp(String key, String label, int defaultValue, int min, int max, Integer step)
                implements PropertyDef {
            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.IntProperty(key, label, defaultValue, min, max,
                        step != null ? step : 1);
            }
        }

        record DoubleProp(String key, String label, double defaultValue, double min, double max, Double step)
                implements PropertyDef {
            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.DoubleProperty(key, label, defaultValue, min, max,
                        step != null ? step : 0.1);
            }
        }

        record BooleanProp(String key, String label, boolean defaultValue) implements PropertyDef {
            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.BooleanProperty(key, label, defaultValue);
            }
        }

        record StringProp(String key, String label, String defaultValue) implements PropertyDef {
            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.StringProperty(key, label, defaultValue);
            }
        }

        record StringListProp(String key, String label, String defaultValue, Set<String> values)
                implements PropertyDef {
            @Override
            public RulePropertyDescriptor toDescriptor() {
                return new RulePropertyDescriptor.StringListProperty(key, label, defaultValue, values);
            }
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = StateDef.AlwaysTransition.class, name = "ALWAYS_TRANSITION"),
            @JsonSubTypes.Type(value = StateDef.DelayedTransition.class, name = "DELAYED_TRANSITION"),
            @JsonSubTypes.Type(value = StateDef.PseudoRandom.class, name = "PSEUDO_RANDOM"),
            @JsonSubTypes.Type(value = StateDef.Countdown.class, name = "COUNTDOWN") })
    public sealed interface StateDef {
        default List<ActionDef> actions() {
            return List.of();
        }

        record AlwaysTransition(List<ActionDef> actions, String nextState) implements StateDef {
            public AlwaysTransition {
                actions = actions != null ? actions : List.of();
            }
        }

        record DelayedTransition(Object delay, String wrappedState) implements StateDef {
        }

        record PseudoRandom(Object maxRoll, List<ActionDef> actions, List<RangeTransition> rangeTransitions)
                implements StateDef {
            public PseudoRandom {
                actions = actions != null ? actions : List.of();
                rangeTransitions = rangeTransitions != null ? rangeTransitions : List.of();
            }
        }

        record Countdown(Object ticks, String nextState) implements StateDef {
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
