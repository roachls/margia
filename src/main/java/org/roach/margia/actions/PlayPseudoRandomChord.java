package org.roach.margia.actions;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.roach.margia.Chord;
import org.roach.margia.Musician;
import org.roach.margia.random.DieRoller;

/**
 * 
 * @param musician                The musician
 * @param spread
 * @param num                     number below which a random note will be
 *                                played.
 * @param maxNotesDiceDescription dice description of maximum number of notes in
 *                                chord
 */
public record PlayPseudoRandomChord(Musician musician, int spread, int num, String maxNotesDiceDescription)
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
            var randomNumberOfNotes = DieRoller.rollDice(maxNotesDiceDescription);
            Set<Integer> randomNotes = IntStream.range(0, randomNumberOfNotes).map(_ -> musician.getKey().randomNote())
                    .mapToObj(Integer::valueOf).sorted().distinct().collect(Collectors.toSet());
            var chord = new Chord(randomNotes, 1, Musician.START_VELOCITY);
            musician.getLogger().atDebug().log("{}: playing {}", musician.getId(), chord);
            musician.playChord(chord);
        } else {
            musician.getLogger().atDebug().log("{}: playing rest", musician.getId());
            musician.rest();
        }
    }

}
