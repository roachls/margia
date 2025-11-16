package org.roach.midi_swarm;

import static javax.sound.midi.ShortMessage.*;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import javax.sound.midi.*;

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
	private final int tempo;

	/**
	 * @param busName name of MIDI bus to send notes on
	 * @param tempo   tempo at which the song will be played
	 */
	public SimpleMidiController(final String busName, final int tempo) {
		Objects.requireNonNull(busName, "bus name cannot be null");
		this.tempo = tempo;
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
			e.printStackTrace();
		}
	}

	/**
	 * Plays the given notes as a chord
	 * 
	 * @param midiChannel MIDI channel to send on
	 * @param note        note to send
	 */
	public void playNote(int midiChannel, NoteInfo note) {
		if (receiver == null) {
			System.err.println("MIDI receiver not available.");
			return;
		}

		executor.schedule(() -> play(midiChannel, note, NOTE_ON), 0, TimeUnit.MILLISECONDS);
		executor.schedule(() -> play(midiChannel, note, NOTE_OFF), note.length().getMillisForTempo(tempo),
				TimeUnit.MILLISECONDS);
	}

	private void play(int midiChannel, NoteInfo note, int eventType) {

		try {
			switch (eventType) {
			case NOTE_ON:
				receiver.send(new ShortMessage(eventType, midiChannel,
						note.note().getNoteNumberForOctave(note.octave()), note.velocity()), -1);
				break;
			case NOTE_OFF:
				receiver.send(
						new ShortMessage(eventType, midiChannel, note.note().getNoteNumberForOctave(note.octave())),
						-1);
				break;
			default:
				break;
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
		if (args.length < 1)
			return;
		var numMusicians = Integer.parseInt(args[0]);
		var controller = new SimpleMidiController("loopMIDI Port", 120);
//		var controller = new SimpleMidiController(DEFAULT_SYNTH, 120);
		var musicians = new Musician[numMusicians];
		var executor = Executors.newFixedThreadPool(numMusicians);
		for (int i = 0; i < numMusicians; i++) {
			musicians[i] = new Musician(i, controller, Key.CMajorPentatonic, 120, i);
		}
//		for (int i = 0; i < numMusicians; i++) {
//			musicians[i].addPeer(musicians[(i + 1) % numMusicians]);
//			musicians[i].addPeer(musicians[(((i - 1) % numMusicians) + numMusicians) % numMusicians]);
//		}
		
		/*
		 * 0 1
		 * 2 3
		 */
		musicians[0].addPeer(musicians[1]);
		musicians[0].addPeer(musicians[2]);
		
		musicians[1].addPeer(musicians[0]);
		musicians[1].addPeer(musicians[3]);

		musicians[2].addPeer(musicians[0]);
		musicians[2].addPeer(musicians[3]);
		
		musicians[3].addPeer(musicians[1]);
		musicians[3].addPeer(musicians[2]);

		for (var musician : musicians) {
			executor.submit(musician);
		}
	}

}