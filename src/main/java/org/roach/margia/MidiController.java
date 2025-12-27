package org.roach.margia;

import static javax.sound.midi.ShortMessage.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.util.NamedThreadFactory;

/**
 * Sends MIDI messages to external (or internal) MIDI instruments
 */
public class MidiController {

    private static final Logger LOGGER = LogManager.getLogger(MidiController.class);
    /**
     * Default Windows synth
     */
    public static final String DEFAULT_SYNTH = "Microsoft GS Wavetable Synth";
    /**
     * External MIDI synth via loopMIDI
     */
    public static final String LOOP_MIDI = "loopMIDI Port";
    private MidiDevice outputDevice;
    private Receiver receiver;
    // one executor per MIDI channel
    private final ScheduledExecutorService executor = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("controller"));
    private final Map<Integer, NoteInfo> notesToPlayNext = new HashMap<>();
    private final int tempo;
    private final ShortMessage timingPulse;

    /**
     * @param busName name of MIDI bus to send notes on
     * @param tempo   tempo at which the song will be played
     */
    public MidiController(final String busName, final int tempo) {
        Objects.requireNonNull(busName, "bus name cannot be null");
        this.tempo = tempo;
        this.timingPulse = new ShortMessage();
        try {
            timingPulse.setMessage(ShortMessage.TIMING_CLOCK);
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().withThrowable(e).log("Error sending MIDI timing pulse");
        }
        try {
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
     * Set the given note to play next on the given MIDI channel
     * 
     * @param midiChannel MIDI channel to send on
     * @param note        note to send
     */
    public void playNote(int midiChannel, NoteInfo note) {
        if (note.noteNum() == -1)
            return;
        if (receiver == null) {
            LOGGER.atError().log("MIDI receiver not available");
            return;
        }

        notesToPlayNext.put(midiChannel, note);
    }

    /**
     * Actually play notes for this tick to be played
     */
    public void playNotesThisTick() {
        for (var i = 0; i < 16; i++) {
            var ai = new AtomicInteger(i);
            if (notesToPlayNext.containsKey(i)) {
                var noteInfo = notesToPlayNext.get(i);
                // stop note at 95% length
                var noteLengthInMillis = (int) (Length.getMillisForTempo(noteInfo.length(), tempo).getValue()
                        .doubleValue() * 0.95);
                executor.schedule(() -> play(ai.get(), noteInfo, NOTE_ON), 0L, TimeUnit.MILLISECONDS);
                executor.schedule(() -> play(ai.get(), noteInfo, NOTE_OFF), noteLengthInMillis, TimeUnit.MILLISECONDS);
            }
        }
        notesToPlayNext.clear();
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

    private void play(int midiChannel, NoteInfo note, int eventType) {
        int noteNumber = note.noteNum();
        try {
            switch (eventType) {
            case NOTE_ON:
                receiver.send(new ShortMessage(NOTE_ON, midiChannel, noteNumber, note.velocity()),
                        System.currentTimeMillis());
                break;
            case NOTE_OFF:
                receiver.send(new ShortMessage(NOTE_OFF, midiChannel, noteNumber, 0), System.currentTimeMillis());

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

}