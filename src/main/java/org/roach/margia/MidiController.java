package org.roach.margia;

import static javax.sound.midi.ShortMessage.*;

import java.util.*;
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
    public static final String DEFAULT_SYNTH = "Microsoft GS Wavetable Synth";
    /**
     * External MIDI synth via loopMIDI
     */
    public static final String LOOP_MIDI = "loopMIDI Port";
    private MidiDevice outputDevice;
    private final Map<String, MidiDevice> inputDevices = new HashMap<>();
    private Receiver primaryReceiver;
    private final Map<String, ExternalReceiver> externalReceivers = new HashMap<>();
    // one executor per MIDI channel
    private final ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new NamedThreadFactory("controller"));
    private final Map<Integer, List<Chord>> chordsToPlayNext = new HashMap<>();
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
     * Load/reload midi device from {@link Options#getMidiOptions()}
     */
    public void loadMidiOutputDevice() {
        try {
            String busName = Options.getInstance().getMidiOptions().isUseExternalMidi() ? LOOP_MIDI : DEFAULT_SYNTH;
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
            primaryReceiver = outputDevice.getReceiver(); // Get the receiver to send MIDI messages

            LOGGER.atInfo().log("Using MIDI output device: {}", outputDevice.getDeviceInfo().getName());
        } catch (MidiUnavailableException e) {
            LOGGER.atError().log("MIDI device {} unavailable", outputDevice.getDeviceInfo().getName(), e);
        }
    }

    /**
     * Scans for all available MIDI input devices
     */
    public void scanForMidiInputDevices() {
        for (var inputDevice : inputDevices.values()) {
            inputDevice.close();
        }
        this.inputDevices.clear();
        this.externalReceivers.clear();
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                device = MidiSystem.getMidiDevice(info);
                // Check if device has transmitters and isn't a software synthesizer
                if (device.getMaxTransmitters() != 0 && !(device instanceof Synthesizer)
                        && info.getName().toLowerCase().contains("axiom")) {
                    LOGGER.atInfo().log("Using input device {}", info.getName());
                    device.open();
                    inputDevices.put(info.getName(), device);
                    var transmitter = device.getTransmitter();
                    var externalReceiver = new ExternalReceiver();
                    transmitter.setReceiver(externalReceiver);
                    externalReceivers.put(info.getName(), externalReceiver);
                }
            } catch (MidiUnavailableException e) {
                LOGGER.atError().withThrowable(e).log("Error opening MIDI device");
            }
        }
    }

    /**
     * Set the given chord to play next on the given MIDI channel
     * 
     * @param midiChannel MIDI channel to send on
     * @param chord       chord to send
     */
    public void playChord(int midiChannel, Chord chord) {
        if (primaryReceiver == null) {
            LOGGER.atError().log("MIDI receiver not available");
            return;
        }

        chordsToPlayNext.putIfAbsent(midiChannel, new ArrayList<>());
        chordsToPlayNext.get(midiChannel).add(chord);
    }

    /**
     * Actually play notes for this tick to be played
     */
    @SuppressWarnings("java:S3776")
    public void playChordsThisTick() {
        for (var i = 0; i < 16; i++) {
            var ai = new AtomicInteger(i);
            if (chordsToPlayNext.containsKey(i)) {
                var chordList = chordsToPlayNext.get(i);
                // schedule all NOTE_ONs immediately
                executor.schedule(() -> {
                    for (var chord : chordList) {
                        for (var noteInfo : chord.getNotes()) {
                            play(ai.get(), noteInfo, NOTE_ON, chord.getVelocity());
                        }
                    }
                }, 0L, TimeUnit.MILLISECONDS);
                // schedule all NOTE_OFFs
                for (var chord : chordList) {
                    // stop note at 95% length
                    var noteLengthInMillis = (int) (Length
                            .getMillisForTempo(chord.getLength(), Options.getInstance().getMusicOptions().getTempo())
                            .getValue().doubleValue() * 0.95);
                    executor.schedule(() -> {
                        for (var noteInfo : chord.getNotes()) {
                            play(ai.get(), noteInfo, NOTE_OFF, chord.getVelocity());
                        }
                    }, noteLengthInMillis, TimeUnit.MILLISECONDS);
                }
            }
        }
        chordsToPlayNext.clear();
    }

    /**
     * Send a clock pulse. Pulses should be sent 24 per beat
     */
    public void sendClockPulse() {
        primaryReceiver.send(timingPulse, -1);
    }

    /**
     * Send a MIDI clock start message
     */
    public void sendStart() {
        var startMsg = new ShortMessage();
        try {
            startMsg.setMessage(ShortMessage.START);
            primaryReceiver.send(startMsg, -1);
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
            primaryReceiver.send(stopMsg, -1);
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
                primaryReceiver.send(new ShortMessage(NOTE_ON, midiChannel, note, velocity),
                        System.currentTimeMillis());
                break;
            case NOTE_OFF:
                primaryReceiver.send(new ShortMessage(NOTE_OFF, midiChannel, note, 0), System.currentTimeMillis());

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
        if (primaryReceiver != null) {
            allNotesOff();
            primaryReceiver.close();
        }
        if (outputDevice != null && outputDevice.isOpen()) {
            outputDevice.close();
        }

        for (var inputDevice : inputDevices.values()) {
            if (inputDevice.isOpen())
                inputDevice.close();
        }
    }

    private void allNotesOff() {
        LOGGER.atInfo().log("**** All notes off");
        try {
            for (var i = 0; i < 16; i++) {
                for (var n = 0; n < 128; n++) {
                    primaryReceiver.send(new ShortMessage(NOTE_OFF, i, n, 0), -1);
                }
            }
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().log("Error turning all notes off: {}", e.getMessage());
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource(String property, _)
                && MidiOptions.EXTERNAL_MIDI_PROPERTY.equals(property))
            instance.loadMidiOutputDevice(); // value of property doesn't matter

    }

    /**
     * @param receiverName device name
     * @return the {@link ExternalReceiver} with the given device name
     * @see #getInputDeviceNames()
     */
    public ExternalReceiver getExternalReceiver(String receiverName) {
        return externalReceivers.get(receiverName);
    }

    /**
     * @return all available input device names
     */
    public List<String> getInputDeviceNames() { return externalReceivers.keySet().stream().toList(); }

    /**
     * @param midiReceiver receiver that doesn't care about which device it is
     *                     connected to
     */
    public void registerWithAllExternalReceivers(MidiReceiver midiReceiver) {
        for (var externalReceiver : this.externalReceivers.values()) {
            externalReceiver.registerReceiver(midiReceiver);
        }
    }

    /**
     * Interested classes may register with an instance of this class to receive
     * incoming MIDI signals
     */
    public class ExternalReceiver implements Receiver {
        private final List<MidiReceiver> receivers = new ArrayList<>();

        @Override
        public void send(MidiMessage message, long timeStamp) {
            // Process the incoming MIDI message
            if (message instanceof ShortMessage sm) {
                var channel = sm.getChannel();
                LOGGER.atTrace().log("Received a {} on channel {}", sm.getClass().getName(), channel);
                for (var receiver : receivers) {
                    receiver.receive(sm);
                }
            }
        }

        /**
         * Registers the given {@code receiver} to receive incoming messages
         * 
         * @param receiver the receiver of ShortMessages
         */
        public void registerReceiver(MidiReceiver receiver) {
            this.receivers.add(receiver);
        }

        @Override
        public void close() {
            // nothing to do
        }
    }

    /**
     * A class that can register itself with an {@link ExternalReceiver} to receive
     * messages
     */
    public interface MidiReceiver {
        /**
         * @param message an incoming MIDI {@link ShortMessage}
         */
        void receive(ShortMessage message);
    }
}