package org.roach.margia.storage;

import java.util.*;

import javax.swing.event.ChangeListener;

import org.roach.margia.rules.MusicianRule;
import org.roach.margia.ui.ChangeEmitter;

/**
 * Options related to a {@link MusicianRule}
 */
public class RuleOptions {
    private String name;
    private final Map<String, Object> ruleSpecificOptions = new LinkedHashMap<>();
    private final ChangeEmitter emitter = new ChangeEmitter();

    /**
     * @return the name
     */
    public String getName() { return name; }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        var oldName = this.name;
        this.name = name;
        if (oldName != null && !oldName.equals(this.name))
            Options.getInstance().setDirty();
    }

    /**
     * @return an unmodifiable view of the ruleSpecificOptions
     */
    public Map<String, Object> getRuleSpecificOptions() { return Collections.unmodifiableMap(ruleSpecificOptions); }

    /**
     * @param optionName name of option
     * @param value      new value
     */
    public void setRuleSpecificOption(String optionName, Object value) {
        var oldValue = this.ruleSpecificOptions.put(optionName, value);
        if (!Objects.equals(oldValue, value))
            Options.getInstance().setDirty();
    }

    /**
     * @param key      key being listened to
     * @param listener listener
     */
    public void addChangeListener(String key, ChangeListener listener) {
        this.emitter.addChangeListener(key, listener);
    }

    @Override
    public String toString() {
        return "RuleOptions [name=" + name + ", ruleSpecificOptions=" + ruleSpecificOptions + "]";
    }

}