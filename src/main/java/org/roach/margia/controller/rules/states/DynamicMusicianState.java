package org.roach.margia.controller.rules.states;

import java.util.List;
import java.util.function.*;

import org.roach.margia.actions.MusicalAction;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.AbstractMusicianRule;
import org.roach.margia.model.MusicianMessage;

public class DynamicMusicianState implements MusicianState {
    private final String name;
    private final List<Function<MusicianMessage, List<MusicalAction>>> actionList;
    private final List<TransitionBinding> transitions;

    public record TransitionBinding(Predicate<Musician> condition, Supplier<MusicianState> targetState) {}

    public DynamicMusicianState(
            String name,
            List<Function<MusicianMessage, List<MusicalAction>>> actionList,
            List<TransitionBinding> transitions) {
        this.name = name;
        this.actionList = actionList;
        this.transitions = transitions;
    }

    @Override
    public String name() { return name; }

    @Override
    public List<Function<MusicianMessage, List<MusicalAction>>> actions() { return actionList; }

    @Override
    public MusicianState transition(Musician musician) {
        for (TransitionBinding binding : transitions) {
            if (binding.condition().test(musician)) {
                return binding.targetState().get();
            }
        }
        return this;
    }

    @Override
    public void doActions(Musician musician, AbstractMusicianRule rule, MusicianMessage message) {
        for (var actionFn : actionList) {
            for (MusicalAction action : actionFn.apply(message)) {
                rule.addActionToTake(action);
            }
        }
    }
}
