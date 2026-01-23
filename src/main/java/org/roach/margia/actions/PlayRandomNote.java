package org.roach.margia.actions;

import java.util.List;

import org.roach.margia.*;
import org.roach.margia.random.DieRoller;

/**
 * Tell the given {@link Musician} to roll the given dice, and if the resulting
 * number is less than or equal to the given <code>num</code>, then a random
 * note will be selected in the current key and the musician will play it. If
 * the result of the die roll is greater then <code>num</code>, nothing will
 * happen.
 * 
 * @param musician The musician
 * @param dice     dice description. See {@link DieRoller#rollDice(String)} for
 *                 more information.
 * @param num      number below which a random note will be played.
 */
public record PlayRandomNote(Musician musician, String dice, int num) implements MusicalAction {

    @Override
    public void perform() {
        var rand = DieRoller.rollDice(dice);
        if (rand <= num) {
            var randomNote = musician.getKey().randomNote();
            musician.getLogger().atDebug().log("{}: playing {}", musician.getId(), randomNote);
            musician.playChord(new Chord(List.of(randomNote), 1, Musician.START_VELOCITY));
        }
    }

}
