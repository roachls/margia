package org.roach.margia.controller;

import static javax.sound.midi.ShortMessage.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;
import org.roach.margia.util.NamedThreadFactory;
import org.roach.margia.view.ChangeEmitter.ChangeSource;

/**
 * Sends MIDI messages to external (or internal) MIDI instruments
 */
@SuppressWarnings("java:S6548")
public class MidiController implements ChangeListener {

    private static final int ALL_NOTES_OFF = 123;
    private static final Logger LOGGER = LogManager.getLogger(MidiController.class);
    private static final Set<String> EXCLUDED_OUTPUT_DEVICES = Set.of("CoolSoft MIDIMapper", "Real Time Sequencer",
            "Microsoft MIDI Mapper");
    private final Map<String, MidiDevice> outputDevices = new TreeMap<>();
    private final Map<String, MidiDevice> inputDevices = new TreeMap<>();
    private final Map<String, Receiver> outputReceivers = new TreeMap<>();
    private final Map<String, ExternalReceiver> inputReceivers = new TreeMap<>();
    // one executor per MIDI channel
    private final ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new NamedThreadFactory("controller"));
    private final Map<String, Map<Integer, List<Chord>>> chordsToPlayNextPerBus = new HashMap<>();
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
    public void scanForMidiOutputDevices() {
        // Get information about all available MIDI devices
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        // Find an output device (e.g., a software synthesizer or a physical MIDI output
        // port)
        for (var info : infos) {
            if (EXCLUDED_OUTPUT_DEVICES.contains(info.getName()) || outputDevices.containsKey(info.getName()))
                continue;
            LOGGER.atTrace().log("Found {} ({}/{} {})", info.getName(), info.getDescription(), info.getVendor(),
                    info.getVersion());
            try {
                var device = MidiSystem.getMidiDevice(info);
                if (!(device instanceof Synthesizer)) {
                    if (!device.isOpen()) {
                        device.open();
                        var receiver = device.getReceiver();
                        if (receiver != null) {
                            outputDevices.put(info.getName(), device);
                            outputReceivers.put(info.getName(), device.getReceiver());
                            LOGGER.atInfo().log("Added MIDI output device: {}", info.getName());
                        }
                    } else {
                        LOGGER.atWarn().log("Device {} is already open", info.getName());
                    }
                }
            } catch (MidiUnavailableException e) {
                LOGGER.atError().log("MIDI device {} unavailable", info.getName());
                LOGGER.atDebug().withThrowable(e).log("{}", info.getName());
            }
        }

        if (outputDevices.isEmpty()) {
            LOGGER.atError().log("No MIDI output devices found");
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
        this.inputReceivers.clear();
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                device = MidiSystem.getMidiDevice(info);
                // Check if device has transmitters and isn't a software synthesizer
                LOGGER.atTrace().log("Examining input device {}", info.getName());
                if (device.getMaxTransmitters() > 0 && !(device instanceof Synthesizer)) {
                    LOGGER.atDebug().log("Found input device {}", info.getName());
                    var similarNameExists = inputDevices.keySet().stream().anyMatch(n -> n.contains(info.getName()));
                    if (similarNameExists)
                        continue;
                    LOGGER.atInfo().log("Adding input device {} ({}:{})", info.getName(), info.getVendor(),
                            info.getVersion());
                    device.open();
                    inputDevices.put(info.getName(), device);
                    var transmitter = device.getTransmitter();
                    var externalReceiver = new ExternalReceiver(info.getName());
                    transmitter.setReceiver(externalReceiver);
                    inputReceivers.put(info.getName(), externalReceiver);
                }
            } catch (MidiUnavailableException e) {
                LOGGER.atError().withThrowable(e).log("Error opening MIDI device");
            }
        }
    }

    /**
     * Set the given chord to play next on the given MIDI channel
     * 
     * @param busName     MIDI bus to send to
     * @param midiChannel MIDI channel to send on
     * @param chord       chord to send
     */
    public void playChord(String busName, int midiChannel, Chord chord) {
        if (MusicianOptions.ALL_BUSSES.equals(busName)) {
            for (var bus : outputReceivers.keySet()) {
                addChordsThisTick(bus, midiChannel, chord);
            }
        } else {
            addChordsThisTick(busName, midiChannel, chord);
        }
    }

    private void addChordsThisTick(String busName, int midiChannel, Chord chord) {
        var chordsForBus = chordsToPlayNextPerBus.computeIfAbsent(busName, _ -> new HashMap<>());
        var listOfChords = chordsForBus.computeIfAbsent(midiChannel, _ -> new ArrayList<>());
        listOfChords.add(chord);
    }

    /**
     * Actually play notes for this tick to be played
     */
    @SuppressWarnings("java:S3776")
    public void playChordsThisTick() {
        for (var busEntry : chordsToPlayNextPerBus.entrySet()) {
            LOGGER.atTrace().log("Playing notes this tick for {}", busEntry.getKey());
            var chordsToPlayNext = busEntry.getValue();
            for (var i = 0; i < 16; i++) {
                var ai = new AtomicInteger(i);
                if (chordsToPlayNext.containsKey(i)) {
                    var chordList = chordsToPlayNext.get(i);
                    // schedule all NOTE_ONs immediately
                    executor.schedule(() -> {
                        for (var chord : chordList) {
                            for (var noteInfo : chord.getNotes()) {
                                play(outputReceivers.get(busEntry.getKey()), ai.get(), noteInfo, NOTE_ON,
                                        chord.getVelocity());
                            }
                        }
                    }, 0L, TimeUnit.MILLISECONDS);
                    // schedule all NOTE_OFFs
                    for (var chord : chordList) {
                        // stop note at 95% length
                        var noteLengthInMillis = (int) (Length
                                .getMillisForTempo(chord.getLength(),
                                        Options.getInstance().getMusicOptions().getTempo())
                                .getValue().doubleValue() * 0.95);
                        executor.schedule(() -> {
                            for (var noteInfo : chord.getNotes()) {
                                play(outputReceivers.get(busEntry.getKey()), ai.get(), noteInfo, NOTE_OFF,
                                        chord.getVelocity());
                            }
                        }, noteLengthInMillis, TimeUnit.MILLISECONDS);
                    }
                }
            }
            chordsToPlayNext.clear();
        }
        chordsToPlayNextPerBus.clear();
    }

    /**
     * Send a clock pulse. Pulses should be sent 24 per beat
     */
    public void sendClockPulse() {
        for (var primaryReceiver : outputReceivers.values()) {
            primaryReceiver.send(timingPulse, -1);
        }
    }

    /**
     * Send a MIDI clock start message
     */
    public void sendStart() {
        var startMsg = new ShortMessage();
        try {
            startMsg.setMessage(ShortMessage.START);
            for (var primaryReceiver : outputReceivers.values()) {
                primaryReceiver.send(startMsg, -1);
            }
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
            for (var primaryReceiver : outputReceivers.values()) {
                primaryReceiver.send(stopMsg, -1);
            }
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().withThrowable(e).log("Error sending MIDI clock stop message");
        }
    }

    private static void play(Receiver primaryReceiver, int midiChannel, Integer note, int eventType, int velocity) {
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
        allNotesOff();
        for (var receiver : outputReceivers.values())
            receiver.close();
        for (var outputDevice : outputDevices.values()) {
            if (outputDevice.isOpen()) {
                outputDevice.close();
            }
        }

        for (var inputDevice : inputDevices.values()) {
            if (inputDevice.isOpen())
                inputDevice.close();
        }
    }

    private void allNotesOff() {
        LOGGER.atInfo().log("**** All notes off");
        try {
            for (var receiver : outputReceivers.values()) {
                receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, ALL_NOTES_OFF, 0), -1);
            }
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().log("Error turning all notes off: {}", e.getMessage());
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof ChangeSource(String property, _)
                && MidiOptions.EXTERNAL_MIDI_PROPERTY.equals(property))
            instance.scanForMidiOutputDevices(); // value of property doesn't matter

    }

    /**
     * @param receiverName device name
     * @return the {@link ExternalReceiver} with the given device name
     * @see #getInputDeviceNames()
     */
    public ExternalReceiver getExternalReceiver(String receiverName) {
        return inputReceivers.get(receiverName);
    }

    /**
     * @return all available input device names
     */
    public List<String> getInputDeviceNames() { return inputReceivers.keySet().stream().toList(); }

    /**
     * @param midiReceiver receiver that doesn't care about which device it is
     *                     connected to
     */
    public void registerWithAllExternalReceivers(MidiReceiver midiReceiver) {
        if (this.inputReceivers.isEmpty()) {
            LOGGER.atError().log("No external input devices have been registered");
            return;
        }
        for (var externalReceiver : this.inputReceivers.values()) {
            externalReceiver.registerReceiver(midiReceiver);
        }
    }

    /**
     * Interested classes may register with an instance of this class to receive
     * incoming MIDI signals
     */
    public class ExternalReceiver implements Receiver {
        private final List<MidiReceiver> receivers = new ArrayList<>();
        private final String name;

        /**
         * @param name name of the BUS that this receiver is listening to
         */
        public ExternalReceiver(String name) {
            this.name = name;
        }

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
            LOGGER.atDebug().log("Registered {} to receive messages from {}", receiver, name);
        }

        /**
         * Unregisters the given receiver from getting messages
         * 
         * @param receiver receiver to remove
         */
        public void unregisterReceiver(MidiReceiver receiver) {
            this.receivers.remove(receiver);
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

    /**
     * @return a set of names of all available output devices
     */
    public Set<String> getAvailableOutputDevices() { return outputDevices.keySet(); }
}