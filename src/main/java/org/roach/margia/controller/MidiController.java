package org.roach.margia.controller;

import static javax.sound.midi.ShortMessage.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sound.midi.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.roach.margia.controller.rules.ReceiverRule;
import org.roach.margia.model.*;
import org.roach.margia.storage.Options;
import org.roach.margia.util.NamedThreadFactory;
import org.roach.margia.view.ChangeEmitter.ChangeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends MIDI messages to external (or internal) MIDI instruments
 */
@SuppressWarnings("java:S6548")
public class MidiController implements ChangeListener {

    private static final int ALL_NOTES_OFF = 123;
    private static final Logger LOGGER = LoggerFactory.getLogger(MidiController.class);
    /*
     * All of the following MIDI devices will cause errors if you try to even open
     * them to look at them, so we exclude them from the list of available devices.
     * 
     * TODO: this is admittedly Windows-specific, and even specific to my device.
     * Need to find a better way.
     */
    private static final Set<String> EXCLUDED_OUTPUT_DEVICES = Set.of("CoolSoft MIDIMapper", "Real Time Sequencer",
            "Microsoft MIDI Mapper", "MidiView");
    /*
     * All devices that are available for output (by name), even if they aren't
     * being used
     */
    private final Map<String, MidiDevice> outputDevices = new TreeMap<>();
    /*
     * All devices that are available for input (by name), even if they aren't being
     * used
     */
    private final Map<String, MidiDevice> inputDevices = new TreeMap<>();
    /*
     * Devices that are actually being used for output will have their receivers
     * here by name
     */
    private final Map<String, Receiver> outputReceivers = new TreeMap<>();
    /*
     * Devices that are actually being used for input will have their
     * ExternalReceivers here by name
     */
    private final Map<String, ExternalReceiver> inputReceivers = new TreeMap<>();
    // one executor per available processor
    private final ScheduledExecutorService scheduledExecutor = Executors
            .newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new NamedThreadFactory("controller"));
    private final ExecutorService immediateExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Map<Integer, List<Chord>>> chordsToPlayNextPerDevice = new HashMap<>();
    private final ShortMessage timingPulse;
    private AtomicBoolean sending = new AtomicBoolean();
    private static MidiController instance;
    // @formatter:off
    private static final Map<Integer, String> MIDI_COMMANDS = Map.of(
            NOTE_OFF, "NOTE_OFF",
            NOTE_ON, "NOTE_ON",
            POLY_PRESSURE, "POLY_PRESSURE",
            CONTROL_CHANGE, "CONTROL_CHANGE", 
            PROGRAM_CHANGE, "PROGRAM_CHANGE",
            CHANNEL_PRESSURE, "CHANNEL_PRESSURE",
            PITCH_BEND, "PITCH_BEND",
            240, "CLOCK_PULSE");
    private static final Map<Integer, String> MIDI_STATUSES = Map.ofEntries(
            Map.entry(MIDI_TIME_CODE, "MIDI_TIME_CODE"),
            Map.entry(SONG_POSITION_POINTER, "SONG_POSITION_POINTER"),
            Map.entry(SONG_SELECT, "SONG_SELECT"), 
            Map.entry(TUNE_REQUEST, "TUNE_REQUEST"),
            Map.entry(END_OF_EXCLUSIVE, "END_OF_EXCLUSIVE"),
            Map.entry(TIMING_CLOCK, "TIMING_CLOCK"),
            Map.entry(START, "START"),
            Map.entry(CONTINUE, "CONTINUE"),
            Map.entry(STOP, "STOP"),
            Map.entry(ACTIVE_SENSING, "ACTIVE_SENSING"), 
            Map.entry(SYSTEM_RESET, "SYSTEM_RESET")
            // @formatter:on
    );

    /**
     * @return the singleton MIDI controller
     */
    public static MidiController getInstance() {
        if (instance == null)
            instance = new MidiController();
        return instance;
    }

    private MidiController() {
        this.timingPulse = new ShortMessage();
        try {
            timingPulse.setMessage(ShortMessage.TIMING_CLOCK);
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().setCause(e).log("Error setting MIDI timing pulse");
        }
        Options.getInstance().getMidiOptions().addChangeListener(MidiOptions.EXTERNAL_MIDI_PROPERTY, this);
    }

    /**
     * Load/reload midi device from {@link Options#getMidiOptions()}
     */
    @SuppressWarnings("java:S135")
    public void scanForMidiOutputDevices() {
        for (var outputDevice : outputDevices.values()) {
            outputDevice.close();
        }
        this.outputDevices.clear();
        this.outputReceivers.clear();

        // Get information about all available MIDI devices
        List<MidiDevice.Info> infos = Arrays.stream(MidiSystem.getMidiDeviceInfo())
                .filter(i -> !EXCLUDED_OUTPUT_DEVICES.contains(i.getName())).filter(i -> !(i instanceof Synthesizer))
                .sorted((i1, i2) -> i1.getName().compareTo(i2.getName())).distinct().toList();

        // all devices actually being used in loaded options
        Set<String> registeredOutputDevices = new HashSet<>(Options.getInstance().getMusicians().values().stream()
                .map(mo -> mo.getDeviceName()).collect(Collectors.toSet()));
        registeredOutputDevices.addAll(Options.getInstance().getMidiOptions().getDevicesToSendTiming());

        // Find an output device (e.g., a software synthesizer or a physical MIDI output
        // port)
        for (var info : infos) {
            LOGGER.atTrace().log("Found '{}' ({}/{} {})", info.getName(), info.getDescription(), info.getVendor(),
                    info.getVersion());
            MidiDevice device;
            try {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException e) {
                LOGGER.atError().setMessage("Unable to obtain information about device {}").addArgument(info.getName())
                        .log();
                LOGGER.atTrace().setCause(e).log();
                continue;
            }
            if (!device.isOpen()) {
                try {
                    device.open();
                } catch (MidiUnavailableException e) {
                    LOGGER.atError().setMessage("MIDI device {} unavailable").addArgument(info.getName()).log();
                    LOGGER.atDebug().setCause(e).setMessage("{}").addArgument(info::getName).log();
                    continue;
                }
                Receiver receiver = null;
                try {
                    receiver = device.getReceiver();
                } catch (MidiUnavailableException _) {
                    LOGGER.atDebug().setMessage("No receiver available for MIDI device {}").addArgument(info::getName)
                            .log();
                    continue;
                }
                // a device with no receiver is not an output device
                if (receiver != null) {
                    // always add devices to list of possible output devices
                    outputDevices.put(info.getName(), device);
                    LOGGER.atInfo().setMessage("Added potential MIDI output device: {}").addArgument(info::getName)
                            .log();
                    // only added receivers that are actually in use
                    if (registeredOutputDevices.contains(info.getName())
                            && !outputReceivers.containsKey(info.getName())) {
                        outputReceivers.put(info.getName(), receiver);
                        LOGGER.atInfo().setMessage("Registered '{}' as output device").addArgument(info::getName).log();
                        if (inputReceivers.containsKey(info.getName())) {
                            LOGGER.atError()
                                    .setMessage("'{}' is being used for both input and output! Loopback will occur!")
                                    .addArgument(info.getName()).log();
                        }
                    }
                }
            } else {
                LOGGER.atWarn().setMessage("Device {} is already open").addArgument(info.getName()).log();
            }
        }

        if (outputDevices.isEmpty()) {
            LOGGER.atError().log("No MIDI output devices found");
        }

    }

    /**
     * Scans for all available MIDI input devices
     */
    @SuppressWarnings({ "java:S3776", "java:S135" })
    public void scanForMidiInputDevices() {
        for (var inputDevice : inputDevices.values()) {
            inputDevice.close();
        }
        this.inputDevices.clear();
        // devices actually registered as inputs
        var registeredInputDevices = new HashSet<String>();
        if (Options.getInstance().getMidiOptions().isUsingExternalTiming())
            registeredInputDevices.add(Options.getInstance().getMidiOptions().getExternalTimingDevice());
        registeredInputDevices.addAll(Options.getInstance().getMusicians().values().stream()
                .map(mo -> mo.getRuleOptions()).filter(ro -> ro.getName().equals("receiver"))
                .map(ro -> (String) ro.getRuleSpecificOptionOrDefault(ReceiverRule.DEVICE_NAME_PROPERTY, null))
                .filter(b -> b != null).collect(Collectors.toSet()));
        LOGGER.atDebug().setMessage("MIDI devices configured for input: {}").addArgument(() -> registeredInputDevices)
                .log();
        var oldInputReceivers = new HashMap<String, ExternalReceiver>(this.inputReceivers);
        this.inputReceivers.clear();
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        var usedOutputDevices = outputReceivers.keySet();
        LOGGER.atDebug().setMessage("MIDI devices configured for output: {}").addArgument(() -> usedOutputDevices)
                .log();
        for (MidiDevice.Info info : infos) {
            try {
                device = MidiSystem.getMidiDevice(info);
            } catch (MidiUnavailableException e) {
                LOGGER.atError().setCause(e).setMessage("Error obtaining MIDI device '{}'").addArgument(info.getName())
                        .log();
                continue;
            }
            // Check if device has transmitters and isn't a software synthesizer
            LOGGER.atTrace().setMessage("Examining input device '{}'").addArgument(info::getName).log();
            if (device.getMaxTransmitters() != 0 && !(device instanceof Synthesizer)) {
                LOGGER.atDebug().setMessage("Found input device '{}'").addArgument(info::getName).log();
                var similarNameExists = inputDevices.keySet().stream().anyMatch(n -> n.contains(info.getName()));
                if (similarNameExists)
                    continue;
                LOGGER.atInfo().setMessage("Adding potential input device '{}' ({}:{})").addArgument(info::getName)
                        .addArgument(info::getVendor).addArgument(info::getVersion).log();
                inputDevices.put(info.getName(), device);
            }
        }

        // add transmitters for registered input devices
        for (var registeredInputDevice : registeredInputDevices) {
            device = inputDevices.get(registeredInputDevice);
            if (device == null) {
                LOGGER.atError().setMessage("No device named '{}' found, are you sure it's connected?")
                        .addArgument(registeredInputDevice).log();
                continue;
            }
            try {
                device.open();
            } catch (MidiUnavailableException e) {
                LOGGER.atError().setCause(e).setMessage("Error opening MIDI device '{}'")
                        .addArgument(registeredInputDevice).log();
                continue;
            }
            Transmitter transmitter = null;
            try {
                transmitter = device.getTransmitter();
            } catch (MidiUnavailableException _) {
                LOGGER.atError().setMessage("No transmitter available for '{}'").addArgument(registeredInputDevice)
                        .log();
                continue;
            }
            var externalReceiver = new ExternalReceiver(registeredInputDevice);
            if (oldInputReceivers.containsKey(registeredInputDevice)) {
                // re-register any listeners
                var oldReceiver = oldInputReceivers.get(registeredInputDevice);
                for (var receiver : oldReceiver.getRegisteredReceivers()) {
                    externalReceiver.registerReceiver(receiver);
                }
            }
            transmitter.setReceiver(externalReceiver);
            inputReceivers.put(registeredInputDevice, externalReceiver);
            LOGGER.atInfo().setMessage("Registered '{}' as input device").addArgument(registeredInputDevice).log();
            if (outputReceivers.containsKey(registeredInputDevice)) {
                LOGGER.atError().setMessage("'{}' has been registered for both input and output, loopback will occur!")
                        .addArgument(registeredInputDevice).log();
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
        if (!outputReceivers.containsKey(busName)) {
            tryToObtainReceiverForBus(busName);
        }
        addChordsThisTick(busName, midiChannel, chord);
    }

    @SuppressWarnings("java:S3824")
    private void tryToObtainReceiverForBus(String busName) {
        var device = outputDevices.get(busName);
        if (device == null) {
            LOGGER.atError().setMessage("No such device: '{}'").addArgument(busName).log();
            return;
        }
        Receiver receiver = null;
        try {
            receiver = device.getReceiver();
        } catch (MidiUnavailableException _) {
            LOGGER.atDebug().setMessage("No receiver available for MIDI device '{}'").addArgument(busName).log();
            return;
        }
        // a device with no receiver is not an output device
        if (receiver != null) {
            // always add devices to list of possible output devices
            outputDevices.put(busName, device);
            LOGGER.atInfo().setMessage("Added MIDI output device: '{}'").addArgument(busName).log();
            // only added receivers that are actually in use
            if (!outputReceivers.containsKey(busName)) {
                outputReceivers.put(busName, receiver);
            }
        }
    }

    /**
     * Sends a control MIDI signal
     * 
     * @param deviceName device to send signal to
     * @param channel    MIDI channel (0-15)
     * @param controller controller number to send (0-127)
     * @param amount     controller value to send (0-127)
     */
    public void sendControlChange(String deviceName, int channel, int controller, int amount) {
        if (deviceName == null)
            return;
        if (amount < 0 || amount > 127 || channel < 0 || channel > 15 || controller < 0 || controller > 127) {
            LOGGER.atDebug().setMessage("out of range; amount: {}, channel: {}, controller: {}").addArgument(amount)
                    .addArgument(channel).addArgument(controller).log();
            return;
        }
        try {
            var panMessage = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, controller, amount);
            if (outputReceivers.containsKey(deviceName)) {
                immediateExecutor.submit(() -> outputReceivers.get(deviceName).send(panMessage, -1));
                LOGGER.atTrace().setMessage("Sending control message on {}:{} with controller {}, amount {}")
                        .addArgument(deviceName).addArgument(channel + 1).addArgument(controller).addArgument(amount)
                        .log();
            }
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().setCause(e).log("Invalid MIDI data");
        }
    }

    private void addChordsThisTick(String busName, int midiChannel, Chord chord) {
        var chordsForBus = chordsToPlayNextPerDevice.computeIfAbsent(busName, _ -> new HashMap<>());
        var listOfChords = chordsForBus.computeIfAbsent(midiChannel, _ -> new ArrayList<>());
        listOfChords.add(chord);
    }

    /**
     * Actually play notes for this tick to be played
     */
    @SuppressWarnings("java:S3776")
    public void playChordsThisTick() {
        for (var deviceEntry : chordsToPlayNextPerDevice.entrySet()) {
            LOGGER.atTrace().setMessage("Playing chords this tick for '{}'").addArgument(deviceEntry::getKey).log();
            var chordsToPlayNext = deviceEntry.getValue();
            for (var i = 0; i < 16; i++) {
                var ai = new AtomicInteger(i);
                if (chordsToPlayNext.containsKey(i)) {
                    var chordList = chordsToPlayNext.get(i);
                    // schedule all NOTE_ONs immediately
                    immediateExecutor.submit(() -> {
                        for (var chord : chordList) {
                            for (var noteInfo : chord.getNotes()) {
                                play(outputReceivers.get(deviceEntry.getKey()), ai.get(), noteInfo, NOTE_ON,
                                        chord.getVelocity());
                            }
                        }
                    });
                    // schedule all NOTE_OFFs
                    for (var chord : chordList) {
                        // stop note at 95% length
                        var noteLengthInMillis = (int) (Length
                                .getMillisForTempo(chord.getLength(),
                                        Options.getInstance().getMusicOptions().getTempo())
                                .getValue().doubleValue() * 0.95);
                        scheduledExecutor.schedule(() -> {
                            for (var noteInfo : chord.getNotes()) {
                                play(outputReceivers.get(deviceEntry.getKey()), ai.get(), noteInfo, NOTE_OFF,
                                        chord.getVelocity());
                            }
                        }, noteLengthInMillis, TimeUnit.MILLISECONDS);
                    }
                }
            }
            chordsToPlayNext.clear();
        }
        chordsToPlayNextPerDevice.clear();
    }

    /**
     * Send a clock pulse. Pulses should be sent 24 per beat
     */
    public void sendClockPulse() {
        if (Options.getInstance().getMidiOptions().getDevicesToSendTiming().isEmpty()) {
            LOGGER.atError().setMessage(
                    "Configured to send MIDI timecode, but no external devices have been configured with the 'midiOptions:devicesToSendTiming' parameter")
                    .log();
        } else {
            for (var deviceName : Options.getInstance().getMidiOptions().getDevicesToSendTiming()) {
                var outputDevice = outputReceivers.get(deviceName);
                LOGGER.atTrace().setMessage("Sending timecode to '{}'").addArgument(deviceName).log();
                outputDevice.send(timingPulse, -1);
            }
        }
    }

    /**
     * Send a MIDI clock start message
     */
    public void sendStart() {
        sending.set(true);
        var startMsg = new ShortMessage();
        try {
            startMsg.setMessage(ShortMessage.START);
            for (var primaryReceiver : outputReceivers.values()) {
                primaryReceiver.send(startMsg, -1);
            }
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().setCause(e).log("Error sending MIDI clock start message");
        }
    }

    /**
     * Send a MIDI clock stop message
     */
    public void sendStop() {
        sending.set(false);
        var stopMsg = new ShortMessage();
        try {
            stopMsg.setMessage(ShortMessage.STOP);
            for (var primaryReceiver : outputReceivers.values()) {
                primaryReceiver.send(stopMsg, -1);
            }
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().setCause(e).log("Error sending MIDI clock stop message");
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
            LOGGER.atError().setCause(e).log("invalid MIDI data");
        }
    }

    /**
     * Close this controller
     */
    public void close() {
        try {
            scheduledExecutor.shutdownNow();
            scheduledExecutor.awaitTermination(1, TimeUnit.SECONDS);
            immediateExecutor.shutdownNow();
            immediateExecutor.awaitTermination(1, TimeUnit.SECONDS);
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

    /**
     * Send MIDI ALL_NOTES_OFF command
     */
    public void allNotesOff() {
        LOGGER.atInfo().log("**** All notes off");
        try {
            for (var receiver : outputReceivers.values()) {
                receiver.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, ALL_NOTES_OFF, 0), -1);
            }
        } catch (InvalidMidiDataException e) {
            LOGGER.atError().setMessage("Error turning all notes off: {}").addArgument(e.getMessage()).log();
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

        /**
         * @return the receivers registered this {@link ExternalReceiver}
         */
        Iterable<MidiReceiver> getRegisteredReceivers() { return Collections.unmodifiableList(receivers); }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            // Process the incoming MIDI message
            if (message instanceof ShortMessage sm) {
                var channel = sm.getChannel();
                LOGGER.atTrace().log("Received a {} command on channel {}: {}/{}, status={}",
                        MIDI_COMMANDS.getOrDefault(sm.getCommand(), Integer.toString(sm.getCommand())), channel + 1,
                        sm.getData1(), sm.getData2(),
                        MIDI_STATUSES.getOrDefault(sm.getStatus(), Integer.toString(message.getStatus())));
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
            LOGGER.atDebug().log("Registered {} to receive messages from '{}'", receiver, name);
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