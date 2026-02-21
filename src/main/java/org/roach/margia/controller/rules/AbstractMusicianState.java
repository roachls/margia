package org.roach.margia.controller.rules;

import java.util.*;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.actions.MusicalAction;
import org.roach.margia.controller.rules.states.MusicianState;
import org.roach.margia.model.Chord;

/**
 * Implementation of {@link MusicianState}
 * 
 * @param <T> The subclass of this class to return for various methods
 * 
 */
public abstract class AbstractMusicianState<T extends AbstractMusicianState<?>> implements MusicianState {
    protected final String name;
    protected final List<Function<Chord, MusicalAction>> actions = new ArrayList<>();
    protected final Logger logger = LogManager.getLogger(getClass());

    protected AbstractMusicianState(final String name) {
        this.name = name;
    }

    /**
     * Adds a list of actions
     * 
     * @param actions a list of actions, where an action is a {@link Function} that
     *                takes a {@link Chord} and returns a {@link MusicalAction}
     * @return this state
     */
    @SuppressWarnings("unchecked")
    public T setActions(List<Function<Chord, MusicalAction>> actions) {
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
    public T withAction(Function<Chord, MusicalAction> action) {
        this.actions.add(action);
        return (T) this;
    }

    @Override
    public List<Function<Chord, MusicalAction>> actions() {
        return Collections.unmodifiableList(actions);
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
