package org.roach.margia.controller.timing;

import java.util.Arrays;

import javax.sound.midi.ShortMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.MidiController.MidiReceiver;
import org.roach.margia.controller.Transport;
import org.roach.margia.storage.Options;

/**
 * A timing source that receives clock impulses from external MIDI
 */
public class ExternalTimingSource implements TimingSource, MidiReceiver {
    private final Logger logger = LogManager.getLogger(getClass());
    private final Transport transport;
    private long lastClockTime;
    private final double[] tempo = new double[24];
    private int tempoIndex;
    private boolean running;

    /**
     * @param transport the {@link Transport} to use
     */
    public ExternalTimingSource(Transport transport) {
        this.transport = transport;
        MidiController.getInstance().registerWithAllExternalReceivers(this);
    }

    @Override
    public void start() {
        transport.start();
        running = true;
    }

    @Override
    public void stop() {
        transport.stop();
        running = false;
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public void receive(ShortMessage message) {
        if (message.getCommand() != 240) // clock pulse
            return;

        logger.atTrace().log("ET-{}:{} {} {}", message.getChannel(), message.getStatus(), message.getData1(),
                message.getData2());
        switch (message.getStatus()) {
        case ShortMessage.TIMING_CLOCK:
            long currentTime = System.nanoTime() / 1000; // Convert to microseconds

            if (lastClockTime != 0) {
                long delta = currentTime - lastClockTime;
                // Formula: 60s / (delta_in_us * 24_clocks) * 1,000,000us/s
                // Simplified: 2,500,000 / delta_in_us
                if (delta > 0) {
                    var instantaneousTempo = 2500000.0 / delta;
                    tempo[tempoIndex] = instantaneousTempo;
                    tempoIndex = (tempoIndex + 1) % 24;
                    Options.getInstance().getMusicOptions().setTempo((int) calcTempo());
                }
            }
            lastClockTime = currentTime;

            transport.receiveClockPulse();
            break;
        case ShortMessage.START, ShortMessage.CONTINUE:
            start();
            break;
        case ShortMessage.STOP:
            stop();
            break;
        default:
            break;
        }
    }

    private double calcTempo() {
        return Arrays.stream(tempo).average().getAsDouble();
    }

}
