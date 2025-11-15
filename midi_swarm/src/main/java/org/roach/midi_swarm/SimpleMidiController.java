package org.roach.midi_swarm;

import static javax.sound.midi.ShortMessage.NOTE_OFF;
import static javax.sound.midi.ShortMessage.NOTE_ON;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Sends MIDI messages to external (or internal) MIDI instruments
 */
public class SimpleMidiController {

	private static final Logger LOGGER = LogManager.getLogger(SimpleMidiController.class);
	private static final String DEFAULT_SYNTH = "Microsoft GS Wavetable Synth";
	private MidiDevice outputDevice;
	private Receiver receiver;
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(16);

	/**
	 * @param busName name of MIDI bus to send notes on
	 */
	public SimpleMidiController(final String busName) {
		Objects.requireNonNull(busName, "bus name cannot be null");
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

			System.out.println("Using MIDI output device: " + outputDevice.getDeviceInfo().getName());

		} catch (MidiUnavailableException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Plays the given notes as a chord
	 * 
	 * @param midiChannel MIDI channel to send on
	 * @param notes       notes to send
	 * @param octave      octave of notes
	 * @param velocity    velocity to play with
	 * @param duration    duration of chord
	 */
	public void playNotes(int midiChannel, List<Note> notes, int octave, int velocity, int duration) {
		if (receiver == null) {
			System.err.println("MIDI receiver not available.");
			return;
		}

		executor.schedule(() -> play(midiChannel, notes, octave, velocity, NOTE_ON), 0, TimeUnit.MILLISECONDS);
		executor.schedule(() -> play(midiChannel, notes, octave, velocity, NOTE_OFF), duration, TimeUnit.MILLISECONDS);
	}

	private void play(int midiChannel, List<Note> notes, int octave, int velocity, int eventType) {

		try {
			for (var note : notes) {
				switch (eventType) {
				case NOTE_ON:
					receiver.send(
							new ShortMessage(eventType, midiChannel, note.getNoteNumberForOctave(octave), velocity),
							-1);
					break;
				case NOTE_OFF:
					receiver.send(new ShortMessage(eventType, midiChannel, note.getNoteNumberForOctave(octave)), -1);
					break;
				default:
					break;
				}
			}
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Close this controller
	 */
	public void close() {
		if (receiver != null) {
			receiver.close();
		}
		if (outputDevice != null && outputDevice.isOpen()) {
			outputDevice.close();
		}
	}

	public static void main(String[] args) {
//	var controller = new SimpleMidiController("loopMIDI Port");
		var controller = new SimpleMidiController(DEFAULT_SYNTH);
		var musicians = new Musician[10];
		var executor = Executors.newFixedThreadPool(10);
		for (int i = 0; i < 10; i++) {
			musicians[i] = new Musician(i, controller, Key.CMajor, 120, i);
		}
		for (int i = 0; i < 10; i++) {
			musicians[i].addPeer(musicians[(i + 1) % 10]);
			musicians[i].addPeer(musicians[(((i - 1) % 10) + 10) % 10]);
		}
		for (var musician : musicians) {
			executor.submit(musician);
		}
	}
}