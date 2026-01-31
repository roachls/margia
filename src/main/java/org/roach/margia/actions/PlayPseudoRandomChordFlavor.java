package org.roach.margia.actions;

import java.util.HashSet;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.view.ChordFlavor;

/**
 * 
 * @param musician The musician
 * @param spread
 * @param num      number below which a random note will be played.
 * @param flavor   the flavor of chord to play
 */
public record PlayPseudoRandomChordFlavor(Musician musician, int spread, int num, ChordFlavor flavor)
        implements MusicalAction {

    @Override
    public void perform() {
        var rand = musician.getId() + musician.getCurrentTick();
        if (musician.getMyLastChord() != null) {
            for (var note : musician.getMyLastChord().getNotes()) {
                rand += note;
            }
        }
        rand %= spread;
        if (rand <= num) {
            var rootNote = musician.getKey().randomNote();
            var set = new HashSet<Integer>();
            set.add(rootNote);
            set.add(rootNote + 4);
            set.add(rootNote + 7);
            var chord = new Chord(set, 1, Musician.START_VELOCITY);
            musician.getLogger().atDebug().log("{}: playing {}", musician.getId(), chord);
            musician.playChord(chord);
        } else {
            musician.getLogger().atDebug().log("{}: playing rest", musician.getId());
            musician.rest();
        }
    }

}
