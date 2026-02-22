package org.roach.margia.controller.rules.states;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.roach.margia.actions.MusicalAction;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.AbstractMusicianRule;
import org.roach.margia.model.Chord;

/**
 */
public class DelayedTransitionState extends AbstractMusicianState<DelayedTransitionState> {
    private final int startValue;
    private int value;
    private final MusicianState wrappedState;
    private final BlockingQueue<Chord> delayQueue = new LinkedBlockingQueue<>();

    /**
     * @param name         name of state
     * @param startValue   value to start at
     * @param wrappedState state to transition to once the value reaches 0
     */
    public DelayedTransitionState(final String name, final int startValue, final MusicianState wrappedState) {
        super(name);
        this.startValue = value = startValue;
        this.wrappedState = wrappedState;
    }

    @Override
    public List<Function<Chord, MusicalAction>> actions() {
        return wrappedState.actions();
    }

    @Override
    @SuppressWarnings("java:S899")
    public void doActions(Musician musician, AbstractMusicianRule rule, Chord chord) {
        Chord chordLocal = chord;
        delayQueue.offer(chordLocal);

        // never play the same note twice
        var lastChord = musician.getMyLastChord();
        if (lastChord != null) {
            while (lastChord.equals(chordLocal)) {
                chordLocal = delayQueue.poll();
            }
        }

        if (chord == null) {
            logger.atDebug().log("{}: chord heard was null, returning", musician.getId());
            return;
        }
        
        super.doActions(musician, rule, chordLocal);
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
