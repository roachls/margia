package org.roach.margia.controller.rules;

import java.util.List;
import java.util.Set;

/**
 * Describes a configurable rule-specific property for dynamic UI generation.
 */
public sealed interface RulePropertyDescriptor {
    /**
     * @return the unique key associated with this option
     */
    String key();

    /**
     * @return the display label for this option
     */
    String label();

    /**
     * @return the default option for the rule
     */
    Object defaultValue();

    /**
     * An integer property
     * 
     * @param key          the unique key
     * @param label        the display label
     * @param defaultValue the default value
     * @param min          the minimum selectable value
     * @param max          the maximum selectable value
     * @param step         the step value for UI spinners
     */
    record IntProperty(String key, String label, Integer defaultValue, int min, int max, int step)
            implements RulePropertyDescriptor {
        public IntProperty(String key, String label, int defaultValue, int min, int max) {
            this(key, label, defaultValue, min, max, 1);
        }
    }

    /**
     * A double property
     * 
     * @param key          the unique key
     * @param label        the display label
     * @param defaultValue the default value
     * @param min          the minimum selectable value
     * @param max          the maximum selectable value
     * @param step         the step value for UI spinners
     */
    record DoubleProperty(String key, String label, Double defaultValue, double min, double max, double step)
            implements RulePropertyDescriptor {
    }

    /**
     * A boolean property
     * 
     * @param key          the unique key
     * @param label        the display label
     * @param defaultValue the default value
     */
    record BooleanProperty(String key, String label, Boolean defaultValue) implements RulePropertyDescriptor {
    }

    /**
     * An Enum value
     * 
     * @param <E>          the type of enum
     * @param key          the unique key
     * @param label        the display label
     * @param defaultValue the default value
     * @param values       the list of available values
     */
    record EnumProperty<E extends Enum<E>>(String key, String label, E defaultValue, List<E> values)
            implements RulePropertyDescriptor {
        public EnumProperty(String key, String label, E defaultValue, Class<E> enumClass) {
            this(key, label, defaultValue, List.of(enumClass.getEnumConstants()));
        }
    }

    /**
     * A String property
     * 
     * @param key          the unique key
     * @param label        the display label
     * @param defaultValue the default value
     */
    record StringProperty(String key, String label, String defaultValue) implements RulePropertyDescriptor {
    }

    /**
     * A String property with a limited list of selectable options
     * 
     * @param key          the unique key
     * @param label        the display label
     * @param defaultValue the default value
     * @param values       the list of selectable values
     */
    record StringListProperty(String key, String label, String defaultValue, Set<String> values)
            implements RulePropertyDescriptor {
    }

}
