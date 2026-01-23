package org.roach.margia;

import static javax.sound.midi.ShortMessage.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.storage.MidiOptions;
import org.roach.margia.storage.Options;
import org.roach.margia.ui.ChangeEmitter.ChangeSource;
import org.roach.margia.util.NamedThreadFactory;

/**
 * Sends MIDI messages to external (or internal) MIDI instruments
 */
@SuppressWarnings("java:S6548")
public class MidiController implements ChangeListener {

    private static final Logger LOGGER = LogManager.getLogger(MidiController.class);
    /**
     * Default Windows synth
     */
    private static final String DEFAULT_SYNTH = "Microsoft GS Wavetable Synth";
    /**
     * External MIDI synth via loopMIDI
     */
    private static final String LOOP_MIDI = "loopMIDI Port";
    private MidiDevice outputDevice;
    private Receiver receiver;
    // one executor per MIDI channel
    private final ScheduledExecutorService executor = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("controller"));
    private final Map<Integer, Chord> chordsToPlayNext = new HashMap<>();
    private final ShortMessage timingPulse;
    private static MidiController instance;

    /**
     * @return the singleton MIDI controller
     */
    public static MidiController getInstance() {
        if (instance == null)
            instance = new MidiController();
        return instance;
    }

    /**
     * @param busName name of MIDI bus to send notes on
     */
    private MidiController() {
        this.timingPulse = new ShortMessage();
        try {
            timingPulse.setMessage(ShortMessage.TIMING_CLOCK);
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().withThrowable(e).log("Error setting MIDI timing pulse");
        }
        Options.getInstance().getMidiOptions().addChangeListener(MidiOptions.EXTERNAL_MIDI_PROPERTY, this);
    }

    /**
     * @param external set to {@code true} to send MIDI externally, {@code false} to
     *                 use built-in OS
     */
    public void setMidiDevice(final boolean external) {
        try {
            String busName = external ? LOOP_MIDI : DEFAULT_SYNTH;
            // Get information about all available MIDI devices
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            MidiDevice selectedDevice = null;
            MidiDevice defaultDevice = null;

            // Find an output device (e.g., a software synthesizer or a physical MIDI output
            // port)
            for (var info : infos) {
                if (DEFAULT_SYNTH.equals(info.getName())) {
                    defaultDevice = MidiSystem.getMidiDevice(info);
                }
                if (busName.equals(info.getName())) {
                    selectedDevice = MidiSystem.getMidiDevice(info);
                    break;
                }
            }

            if (selectedDevice == null) {
                LOGGER.atError().log("No MIDI device found with name {}", busName);
                if (defaultDevice != null) {
                    selectedDevice = defaultDevice;
                    LOGGER.atInfo().log("Using default device: %s (%s)" + selectedDevice.getDeviceInfo().getName(),
                            selectedDevice.getDeviceInfo().getVendor());
                }
                if (selectedDevice == null)
                    return;
            }

            outputDevice = selectedDevice;
            outputDevice.open(); // Open the device to use it
            receiver = outputDevice.getReceiver(); // Get the receiver to send MIDI messages

            LOGGER.atInfo().log("Using MIDI output device: {}", outputDevice.getDeviceInfo().getName());
        } catch (MidiUnavailableException e) {
            LOGGER.atError().log("MIDI device {} unavailable", outputDevice.getDeviceInfo().getName(), e);
        }
    }

    /**
     * Set the given chord to play next on the given MIDI channel
     * 
     * @param midiChannel MIDI channel to send on
     * @param chord       chord to send
     */
    public void playChord(int midiChannel, Chord chord) {
        if (receiver == null) {
            LOGGER.atError().log("MIDI receiver not available");
            return;
        }

        chordsToPlayNext.put(midiChannel, chord);
    }

    /**
     * Actually play notes for this tick to be played
     */
    public void playChordsThisTick() {
        for (var i = 0; i < 16; i++) {
            var ai = new AtomicInteger(i);
            if (chordsToPlayNext.containsKey(i)) {
                var chord = chordsToPlayNext.get(i);
                // stop note at 95% length
                var noteLengthInMillis = (int) (Length
                        .getMillisForTempo(chord.getLength(), Options.getInstance().getMusicOptions().getTempo())
                        .getValue().doubleValue() * 0.95);
                for (var noteInfo : chord.getNotes()) {
                    executor.schedule(() -> play(ai.get(), noteInfo, NOTE_ON, chord.getVelocity()), 0L,
                            TimeUnit.MILLISECONDS);
                    executor.schedule(() -> play(ai.get(), noteInfo, NOTE_OFF, chord.getVelocity()), noteLengthInMillis,
                            TimeUnit.MILLISECONDS);
                }
            }
        }
        chordsToPlayNext.clear();
    }

    /**
     * Send a clock pulse. Pulses should be sent 24 per beat
     */
    public void sendClockPulse() {
        receiver.send(timingPulse, -1);
    }

    /**
     * Send a MIDI clock start message
     */
    public void sendStart() {
        var startMsg = new ShortMessage();
        try {
            startMsg.setMessage(ShortMessage.START);
            receiver.send(startMsg, -1);
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().withThrowable(e).log("Error sending MIDI clock start message");
        }
    }

    /**
     * Send a MIDI clock stop message
     */
    public void sendStop() {
        var stopMsg = new ShortMessage();
        try {
            stopMsg.setMessage(ShortMessage.STOP);
            receiver.send(stopMsg, -1);
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().withThrowable(e).log("Error sending MIDI clock stop message");
        }
    }

    private void play(int midiChannel, Integer note, int eventType, int velocity) {
        if (note == null)
            return;
        try {
            switch (eventType) {
            case NOTE_ON:
                receiver.send(new ShortMessage(NOTE_ON, midiChannel, note, velocity), System.currentTimeMillis());
                break;
            case NOTE_OFF:
                receiver.send(new ShortMessage(NOTE_OFF, midiChannel, note, 0), System.currentTimeMillis());

                break;
            default:
                break;
            }
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().withThrowable(e).log("invalid MIDI data");
        }
    }

    /**
     * Close this controller
     */
    public void close() {
        try {
            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
        if (receiver != null) {
            allNotesOff();
            receiver.close();
        }
        if (outputDevice != null && outputDevice.isOpen()) {
            outputDevice.close();
        }
    }

    private void allNotesOff() {
        LOGGER.atInfo().log("**** All notes off");
        try {
            for (var i = 0; i < 16; i++) {
                for (var n = 0; n < 128; n++) {
                    receiver.send(new ShortMessage(NOTE_OFF, i, n, 0), -1);
                }
            }
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().log("Error turning all notes off: {}", e.getMessage());
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource(String property, Object newVal)
                && MidiOptions.EXTERNAL_MIDI_PROPERTY.equals(property))
            instance.setMidiDevice((boolean) newVal);
    }

}