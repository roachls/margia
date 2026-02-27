package org.roach.margia.controller.rules.states;

import java.util.*;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.actions.MusicalAction;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.AbstractMusicianRule;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 * Implementation of {@link MusicianState}
 * 
 * @param <T> The subclass of this class to return for various methods
 * 
 */
public abstract class AbstractMusicianState<T extends AbstractMusicianState<T>> implements MusicianState {
    protected final String name;
    protected final List<Function<MusicianMessage, List<MusicalAction>>> actions = new ArrayList<>();
    protected final Logger logger = LogManager.getLogger(getClass());

    protected AbstractMusicianState(final String name) {
        this.name = name;
    }

    /**
     * Adds a list of actions
     * 
     * @param actions a list of actions, where an action is a {@link Function} that
     *                takes a {@link Chord} and returns one or more
     *                {@link MusicalAction MusicalActions}
     * @return this state
     */
    @SuppressWarnings("unchecked")
    public T withActions(@SuppressWarnings("hiding") List<Function<MusicianMessage, List<MusicalAction>>> actions) {
        this.actions.addAll(actions);
        return (T) this;
    }

    /**
     * Adds a single action, where an action is a {@link Function} that takes a
     * {@link Chord} and returns a {@link MusicalAction}
     * 
     * @param action the action to add
     * @return this state
     */
    @SuppressWarnings("unchecked")
    public T withAction(Function<MusicianMessage, List<MusicalAction>> action) {
        this.actions.add(action);
        return (T) this;
    }

    @Override
    public List<Function<MusicianMessage, List<MusicalAction>>> actions() {
        return Collections.unmodifiableList(actions);
    }

    @Override
    public void doActions(Musician musician, AbstractMusicianRule rule, MusicianMessage message) {
        for (var action : actions()) {
            logger.atDebug().log("{} ({}): {}", musician.getId(), name(), message);
            for (var musicianAction : action.apply(message)) {
                rule.addActionToTake(musicianAction);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractMusicianState<?> other = (AbstractMusicianState<?>) obj;
        return Objects.equals(name, other.name);
    }

    @Override
    public String name() {
        return name;
    }
}
