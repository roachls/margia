package org.roach.margia.controller.rules;

import java.util.*;
import java.util.function.Function;

import org.roach.margia.actions.MusicalAction;
import org.roach.margia.controller.Musician;
import org.roach.margia.model.MusicianMessage;

/**
 * The registry for rules loaded from YAML
 */
public class RuleComponentRegistry {

    /**
     * Something that can create actions
     */
    @FunctionalInterface
    public interface ActionFactory {
        /**
         * @param musician       the musician to which the action will apply
         * @param resolvedParams the parameters resolved from YAML
         * @return a function that takes a {@link MusicianMessage} and returns a list of
         *         {@link MusicalAction actions}
         */
        Function<MusicianMessage, List<MusicalAction>> create(Musician musician, Map<String, Object> resolvedParams);
    }

    /**
     * A condition that drives a transition
     */
    @FunctionalInterface
    public interface TransitionCondition {
        /**
         * @param musician       the musician that will take the action(s)
         * @param resolvedParams the parameters resolved from YAML
         * @return {@code true} if the test passes
         */
        boolean test(Musician musician, Map<String, Object> resolvedParams);
    }

    private final Map<String, ActionFactory> actionFactories = new HashMap<>();
    private final Map<String, TransitionCondition> conditions = new HashMap<>();

    /**
     * Register a factory
     * 
     * @param type    type of factory
     * @param factory factory
     */
    public void registerAction(String type, ActionFactory factory) {
        actionFactories.put(type, factory);
    }

    /**
     * Registers a {@link TransitionCondition}
     * 
     * @param name      name of condition
     * @param condition the transition condition
     */
    public void registerCondition(String name, TransitionCondition condition) {
        conditions.put(name, condition);
    }

}
