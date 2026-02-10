package org.roach.margia.model;

import java.util.*;

import javax.swing.event.ChangeListener;

import org.roach.margia.controller.rules.MusicianRule;
import org.roach.margia.storage.Options;
import org.roach.margia.view.ChangeEmitter;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

/**
 * Options related to a {@link MusicianRule}
 * 
 * Note to implementers: this class is persisted directly to YAML. Do not add
 * new getters/setters that you don't wanted persisted. If you must add a field
 * that won't be persisted, use non-JavaBean getters/setters for it, i.e., if
 * the field is called {@code foo}, use {@code foo()} for a getter and
 * {@code foo(Foo f)} for a setter.
 */
public class RuleOptions {
    private String name;
    private final Map<String, Object> ruleSpecificOptions = new LinkedHashMap<>();
    private final ChangeEmitter emitter = new ChangeEmitter();
    /**
     * property fired when the rule changes
     */
    public static final String RULE_NAME_PROPERTY = "ruleName";
    /**
     * property fired when any rule-specific options change
     */
    public static final String RULE_SPECIFIC_OPTIONS_PROPERTY = "rule-specific options";
    /**
     * property fired when a specific rule-specific options changes
     */
    public static final String RULE_SPECIFIC_OPTION_CHANGED_PROPERTY = "single value changed";

    /**
     * @return the name
     */
    public String getName() { return name; }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        if (name == null)
            return;
        var oldName = this.name;
        this.name = name;
        if (!this.name.equals(oldName)) {
            emitter.fireChangeEvent(RULE_NAME_PROPERTY, new ChangeSource(RULE_NAME_PROPERTY, this.name));
            Options.getInstance().setDirty();
            this.ruleSpecificOptions.clear();
        }
    }

    /**
     * @return an unmodifiable view of the ruleSpecificOptions
     * @apiNote This must be here for YAML storage, and it must be public, but it
     *          should not be used to access any properties. Use instead
     *          {@link #getRuleSpecificOptionOrDefault(String, Object)}.
     */
    public Map<String, Object> getRuleSpecificOptions() { return Collections.unmodifiableMap(ruleSpecificOptions); }

    /**
     * @param optionName   name of option
     * @param defaultValue default value if not found
     * @return the stored value for the given option, or {@code defaultValue} if not
     *         found. Normally the only reason it wouldn't be found is that someone
     *         manually edited a config file and didn't include it.
     */
    public Object getRuleSpecificOptionOrDefault(String optionName, Object defaultValue) {
        return ruleSpecificOptions.getOrDefault(optionName, defaultValue);
    }

    /**
     * @param optionName name of option
     * @param value      new value
     */
    public void setRuleSpecificOption(String optionName, Object value) {
        var oldValue = this.ruleSpecificOptions.put(optionName, value);
        if (!Objects.equals(oldValue, value)) {
            Options.getInstance().setDirty();
            emitter.fireChangeEvent(RULE_SPECIFIC_OPTIONS_PROPERTY,
                    new ChangeSource(RULE_SPECIFIC_OPTIONS_PROPERTY, this.ruleSpecificOptions));
            emitter.fireChangeEvent(RULE_SPECIFIC_OPTION_CHANGED_PROPERTY,
                    new ChangeSource(RULE_SPECIFIC_OPTION_CHANGED_PROPERTY, Map.entry(optionName, value)));
        }
    }

    /**
     * @param key      key being listened to
     * @param listener listener
     */
    public void addChangeListener(String key, ChangeListener listener) {
        this.emitter.addChangeListener(key, listener);
    }

    /**
     * @return a new {@link RuleOptions} instance that is a copy of this one
     */
    public RuleOptions copy() {
        var copy = new RuleOptions();
        copy.name = name;
        copy.ruleSpecificOptions.putAll(ruleSpecificOptions);
        return copy;
    }

    /**
     * Copies these {@link RuleOptions} into the {@code target}
     * 
     * @param target target {@link RuleOptions}
     */
    public void copyInto(RuleOptions target) {
        target.setName(this.name);
        target.ruleSpecificOptions.clear();
        target.ruleSpecificOptions.putAll(ruleSpecificOptions);
    }

    @Override
    public String toString() {
        return "RuleOptions [name=" + name + ", ruleSpecificOptions=" + ruleSpecificOptions + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ruleSpecificOptions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RuleOptions other = (RuleOptions) obj;
        return Objects.equals(name, other.name) && Objects.equals(ruleSpecificOptions, other.ruleSpecificOptions);
    }

}