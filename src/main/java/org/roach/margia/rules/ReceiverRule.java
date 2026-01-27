package org.roach.margia.rules;

import java.util.*;

import javax.sound.midi.ShortMessage;

import org.roach.margia.Chord;
import org.roach.margia.MidiController;
import org.roach.margia.MidiController.MidiReceiver;
import org.roach.margia.actions.PlayChord;

/**
 * Receives and enqueues messages from an external MIDI controller
 */
public class ReceiverRule extends AbstractMusicianRule implements MidiReceiver {

    @Override
    public void calculateAction(long tick) {
        var message = musician.getNextMessageReceived();
        if (message instanceof Chord chord)
            actionsToTake.add(new PlayChord(musician, chord));
    }

    @Override
    public String getName() { return "receiver"; }

    @Override
    public List<SettableParamDescription<?>> getSettableParameters() { return Collections.emptyList(); }

    @Override
    public AbstractMusicianRule copy() {
        var copy = new ReceiverRule();
        var distributor = MidiController.getInstance().getExternalReceiver();
        if (distributor != null)
            distributor.registerReceiver(copy);
        return copy;
    }

    @Override
    public void receive(ShortMessage message) {
        logger.atTrace().log("received {} on channel {}", message.getClass().getName(), message.getChannel());
        if (message.getCommand() == ShortMessage.NOTE_ON) {
            var note = message.getData1();
            var velocity = message.getData2();
            logger.atTrace().log("Received NOTE_ON note={}, velocity={}", note, velocity);
            musician.receiveMessage(new Chord(Set.of(note), 1, velocity));
        }
    }

}
