package org.roach.margia.actions;

import java.util.List;

import org.roach.margia.*;

/**
 * 
 * @param musician The musician
 * @param spread
 * @param num      number below which a random note will be played.
 */
public record PlayPseudoRandomNote(Musician musician, int spread, int num) implements MusicalAction {

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
            var randomNote = musician.getKey().randomNote();
            musician.getLogger().atDebug().log("{}: playing {}", musician.getId(), randomNote);
            musician.playChord(new Chord(List.of(randomNote), 1, Musician.START_VELOCITY));
        } else {
            musician.getLogger().atDebug().log("{}: playing rest", musician.getId());
            musician.rest();
        }
    }

}
