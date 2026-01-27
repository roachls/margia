package org.roach.margia;

import javax.sound.midi.*;

public class MidiInputReceiver implements Receiver {

    public static void main(String[] args) throws MidiUnavailableException {
        // Find the correct MIDI input device
        MidiDevice inputDevice = findMidiInputDevice();

        if (inputDevice == null) {
            System.out.println("No suitable MIDI input device found.");
            return;
        }

        // Open the device
        inputDevice.open();
        System.out.println("Opened device: " + inputDevice.getDeviceInfo().getName());

        // Get the transmitter and set our custom receiver
        Transmitter transmitter = inputDevice.getTransmitter();
        transmitter.setReceiver(new MidiInputReceiver());

        System.out.println("Listening for MIDI messages. Press Ctrl+C to exit.");
        // Keep the program running to receive messages
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        // Don't forget to close the device when done (e.g., in a shutdown hook)
        // inputDevice.close();
    }

    /**
     * Scans for an available MIDI input device.
     */
    private static MidiDevice findMidiInputDevice() throws MidiUnavailableException {
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            device = MidiSystem.getMidiDevice(info);
            // Check if device has transmitters and isn't a software synthesizer
            if (device.getMaxTransmitters() != 0 && !(device instanceof Synthesizer)
                    && !"Real Time Sequencer".equals(info.getName())) {
                // You might need more sophisticated filtering to get the *right* device
                System.out.println("Found input device candidate: " + info.getName());
//                return device;
            }
        }
        return null;
    }

    // Implementation of the Receiver interface methods
    @Override
    public void send(MidiMessage message, long timeStamp) {
        // Process the incoming MIDI message
        if (message instanceof ShortMessage sm) {
            if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData1() > 0) {
                System.out.println("Note On: channel " + sm.getChannel() + ", key " + sm.getData1() + ", velocity "
                        + sm.getData2());
            } else if (sm.getCommand() == ShortMessage.NOTE_OFF
                    || (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() == 0)) {
                System.out.println("Note Off: channel " + sm.getChannel() + ", key " + sm.getData1());
            }
        } else if (message instanceof SysexMessage) {
            System.out.println("System Exclusive Message received.");
        }
    }

    @Override
    public void close() {
        System.out.println("Receiver closed.");
    }
}
