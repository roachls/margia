package org.roach.margia.controller.rules;

import java.util.*;
import java.util.function.Function;

import org.roach.margia.actions.MusicalAction;
import org.roach.margia.controller.Musician;
import org.roach.margia.model.MusicianMessage;

public class RuleComponentRegistry {

    @FunctionalInterface
    public interface ActionFactory {
        Function<MusicianMessage, List<MusicalAction>> create(
                Musician musician, Map<String, Object> resolvedParams);
    }

    @FunctionalInterface
    public interface TransitionCondition {
        boolean test(Musician musician, Map<String, Object> resolvedParams);
    }

    private final Map<String, ActionFactory> actionFactories = new HashMap<>();
    private final Map<String, TransitionCondition> conditions = new HashMap<>();

    public void registerAction(String type, ActionFactory factory) {
        actionFactories.put(type, factory);
    }

    public void registerCondition(String name, TransitionCondition condition) {
        conditions.put(name, condition);
    }

    // getters for lookup...
}
