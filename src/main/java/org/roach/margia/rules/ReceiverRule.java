package org.roach.margia.rules;

import java.util.List;
import java.util.Set;

import javax.sound.midi.ShortMessage;

import org.roach.margia.Chord;
import org.roach.margia.MidiController;
import org.roach.margia.MidiController.MidiReceiver;
import org.roach.margia.actions.PlayChord;
import org.roach.margia.storage.Options;
import org.roach.margia.storage.RuleOptions;
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

    @Override
    public void calculateAction(long tick) {
        var message = musician.getNextMessageReceived();
        if (message instanceof Chord chord)
            actionsToTake.add(new PlayChord(musician, chord));
    }

    @Override
    public String getName() { return "receiver"; }

    @Override
    public List<SettableParamDescription> getSettableParameters() {
        return List.of(new StringListParamDescription(DEVICE_NAME_PROPERTY, "Device Name",
                MidiController.getInstance().getInputDeviceNames()));
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

    @Override
    public void restoreFromStorage(RuleOptions ruleOptions) {
        super.restoreFromStorage(ruleOptions);
        if (ruleOptions.getRuleSpecificOptions().containsKey(DEVICE_NAME_PROPERTY)) {
            this.deviceName = (String) ruleOptions.getRuleSpecificOptions().get(DEVICE_NAME_PROPERTY);
        }
    }

    @Override
    public void initActionsAfterMusicianAssigned() {
        registerWithExternalReceiver();
    }

}
