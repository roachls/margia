package org.roach.margia.controller.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sound.midi.ShortMessage;

import org.roach.margia.actions.PlayChord;
import org.roach.margia.controller.MidiController;
import org.roach.margia.controller.MidiController.MidiReceiver;
import org.roach.margia.model.Chord;
import org.roach.margia.model.RuleOptions;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.params.SettableParamDescription;
import org.roach.margia.storage.params.StringListParamDescription;

/**
 * Receives and enqueues messages from an external MIDI controller
 */
public class ReceiverRule extends AbstractMusicianRule implements MidiReceiver {
    /**
     * device name property
     */
    public static final String DEVICE_NAME_PROPERTY = "deviceName";
    /**
     * special deviceName property representing all devices
     */
    public static final String ALL_DEVICES = "All Available Devices";

    private String deviceName;

    private final List<Integer> notesThisTick = new ArrayList<>();
    private int latestVelocity;

    @Override
    public void calculateAction(long tick) {
        if (!notesThisTick.isEmpty()) {
            // gather up all notes received during this tick into a chord
            var chord = new Chord(notesThisTick.stream().collect(Collectors.toSet()), 1, latestVelocity);
            logger.atTrace().log("{}: Playing chord {}", musician.getCurrentTick(), chord);
            actionsToTake.add(new PlayChord(musician, chord));
        }
        notesThisTick.clear();
    }

    @Override
    public String getName() { return "receiver"; }

    @Override
    public List<SettableParamDescription> getSettableParameters() {
        return List.of(new StringListParamDescription(DEVICE_NAME_PROPERTY, "Device Name",
                MidiController.getInstance().getInputDeviceNames(), ALL_DEVICES));
    }

    /**
     * @param deviceName the name of the external device from which this object
     *                   should parse messages
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        Options.getInstance().getMusicians().get(musician.getId()).getRuleOptions()
                .setRuleSpecificOption(DEVICE_NAME_PROPERTY, deviceName);
        registerWithExternalReceiver();
    }

    private void registerWithExternalReceiver() {
        if (this.deviceName != null) {
            if (!ALL_DEVICES.equals(this.deviceName)) {
                var distributor = MidiController.getInstance().getExternalReceiver(this.deviceName);
                if (distributor != null) {
                    distributor.registerReceiver(this);
                } else
                    musician.getLogger().atWarn().log(
                            "Device {} is not available, musician {} will not be able to receive", deviceName,
                            musician.getId());
            } else {
                MidiController.getInstance().registerWithAllExternalReceivers(this);
            }
        }
    }

    @Override
    public AbstractMusicianRule copy() {
        var copy = new ReceiverRule();
        copy.deviceName = this.deviceName;
        return copy;
    }

    /**
     * This method could be called at any time during a tick because it is dependent
     * on a human musician, so we add the note to a list to be concatenated later by
     * {@link #calculateAction(long)}.
     */
    @Override
    public void receive(ShortMessage message) {
        if (message.getCommand() == ShortMessage.NOTE_OFF) {
            var note = message.getData1();
            var velocity = message.getData2();
            logger.atTrace().log("{}: Received NOTE_OFF note={}, velocity={}", musician.getCurrentTick(), note,
                    velocity);
            notesThisTick.add(note);
        } else if (message.getCommand() == ShortMessage.NOTE_ON) {
            // we use velocity of NOTE_ON
            this.latestVelocity = message.getData2();
        }
    }

    @Override
    public void restoreFromStorage(RuleOptions ruleOptions) {
        super.restoreFromStorage(ruleOptions);
        this.deviceName = (String) ruleOptions.getRuleSpecificOptionOrDefault(DEVICE_NAME_PROPERTY, ALL_DEVICES);
    }

    @Override
    public void initActionsAfterMusicianAssigned() {
        registerWithExternalReceiver();
    }

}
