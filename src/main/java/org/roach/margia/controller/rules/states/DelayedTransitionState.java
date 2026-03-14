package org.roach.margia.controller.rules.states;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.roach.margia.actions.MusicalAction;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.AbstractMusicianRule;
import org.roach.margia.model.Chord;
import org.roach.margia.model.MusicianMessage;

/**
 */
public class DelayedTransitionState extends AbstractMusicianState<DelayedTransitionState> {
    private final int startValue;
    private int value;
    private MusicianState wrappedState;
    private final BlockingQueue<Chord> delayQueue = new LinkedBlockingQueue<>();

    /**
     * @param name       name of state
     * @param startValue value to start at
     */
    public DelayedTransitionState(final String name, final int startValue) {
        super(name);
        this.startValue = value = startValue;
    }

    @Override
    public List<Function<MusicianMessage, List<MusicalAction>>> actions() {
        var list = new ArrayList<>(this.actions);
        list.addAll(wrappedState.actions());
        return list;
    }

    @Override
    @SuppressWarnings("java:S899")
    public void doActions(Musician musician, AbstractMusicianRule rule, MusicianMessage message) {
        if (!(message instanceof Chord))
            return;

        Chord chord = (Chord) message;
        delayQueue.offer(chord);

        // never play the same note twice
        var lastChord = musician.getMyLastChord();
        if (lastChord != null) {
            while (lastChord.equals(chord)) {
                chord = delayQueue.poll();
            }
        }

        if (chord == null) {
            logger.atDebug().setMessage("{}: chord heard was null, returning").addArgument(musician.getId()).log();
            return;
        }

        super.doActions(musician, rule, chord);
    }

    /**
     * @param wrappedState the state to wrap
     * @return this state
     */
    @SuppressWarnings("hiding")
    public DelayedTransitionState withWrappedState(MusicianState wrappedState) {
        this.wrappedState = wrappedState;
        return this;
    }

    @Override
    public MusicianState transition(Musician musician) {
        value--;
        if (value <= 0) {
            value = startValue;
            return wrappedState.transition(musician);
        }
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
