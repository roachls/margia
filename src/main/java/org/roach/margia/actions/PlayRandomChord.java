package org.roach.margia.actions;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.roach.margia.controller.Musician;
import org.roach.margia.model.Chord;
import org.roach.margia.util.DieRoller;

/**
 * Tell the given {@link Musician} to roll the given dice, and if the resulting
 * number is less than or equal to the given <code>num</code>, then a random
 * chord will be selected in the current key and the musician will play it. If
 * the result of the die roll is greater then <code>num</code>, nothing will
 * happen.
 * 
 * @param musician                The musician
 * @param dice                    dice description. See
 *                                {@link DieRoller#rollDice(String)} for more
 *                                information.
 * @param num                     number below which a random note will be
 *                                played.
 * @param maxNotesDiceDescription dice description of maximum number of notes in
 *                                chord
 */
public record PlayRandomChord(Musician musician, String dice, int num, String maxNotesDiceDescription)
        implements MusicalAction {

    @Override
    public void perform() {
        var rand = DieRoller.rollDice(dice);
        if (rand <= num) {
            var randomNumberOfNotes = DieRoller.rollDice(maxNotesDiceDescription);
            Set<Integer> randomNotes = IntStream.range(0, randomNumberOfNotes).map(_ -> musician.getKey().randomNote())
                    .mapToObj(Integer::valueOf).sorted().distinct().collect(Collectors.toSet());
            var chord = new Chord(randomNotes, 1, Musician.START_VELOCITY);
            musician.getLogger().atDebug().log("{}: playing {}", musician.getId(), chord);
            musician.playChord(chord);
        }
    }

}
