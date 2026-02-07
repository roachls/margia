package org.roach.margia.controller.timing;

import javax.sound.midi.ShortMessage;

import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.MidiController.MidiReceiver;
import org.roach.margia.controller.Transport;
import org.roach.margia.model.MusicOptions;

public class ExternalTimingSource implements TimingSource, MidiReceiver {

    private final Transport transport;

    public ExternalTimingSource(Transport transport) {
        this.transport = transport;
        MidiController.getInstance().registerWithAllExternalReceivers(this);
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTempo() {
        // TODO Auto-generated method stub

    }

    @Override
    public int getTempo() { // TODO Auto-generated method stub
        return MusicOptions.DEFAULT_TEMPO;
    }

    @Override
    public boolean isRunning() { // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void receive(ShortMessage message) {
        System.out.printf("ET-%d: %d %d %d%n", message.getChannel(), message.getCommand(), message.getData1(),
                message.getData2());
    }

}
