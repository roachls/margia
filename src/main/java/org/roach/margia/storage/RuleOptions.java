package org.roach.margia.storage;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public void setName(String name) { this.name = name; }

    /**
     * @return the ruleSpecificOptions
     */
    public Map<String, Object> getRuleSpecificOptions() { return ruleSpecificOptions; }

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