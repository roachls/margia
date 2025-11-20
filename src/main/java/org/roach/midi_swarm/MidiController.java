package org.roach.midi_swarm;

import static javax.sound.midi.ShortMessage.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.midi.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Sends MIDI messages to external (or internal) MIDI instruments
 */
public class MidiController {

	private static final Logger LOGGER = LogManager.getLogger(MidiController.class);
	private static final String DEFAULT_SYNTH = "Microsoft GS Wavetable Synth";
	private MidiDevice outputDevice;
	private Receiver receiver;
	// one executor per MIDI channel
	private final ScheduledExecutorService[] executors = new ScheduledExecutorService[16];
	private final int tempo;

	private void initExecutors() {
		for (var i = 0; i < 16; i++) {
			var wrappedNum = new AtomicInteger(i);
			executors[i] = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "player_" + wrappedNum.get()));
		}
	}

	/**
	 * @param busName name of MIDI bus to send notes on
	 * @param tempo   tempo at which the song will be played
	 */
	public MidiController(final String busName, final int tempo) {
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
			initExecutors();
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
		if (note.note() == -1)
			return;
		if (receiver == null) {
			System.err.println("MIDI receiver not available.");
			return;
		}

		// cut off note before start of next note to avoid notes that never get cut off
		var noteCutoffTime = note.length().getMillisForTempo(tempo) - 10;
		executors[midiChannel].schedule(() -> {
			play(midiChannel, note, NOTE_ON);
			try {
				TimeUnit.MILLISECONDS.sleep(noteCutoffTime);
				play(midiChannel, note, NOTE_OFF);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, 0, TimeUnit.MILLISECONDS);
	}

	private void play(int midiChannel, NoteInfo note, int eventType) {
		int noteNumber = note.note();
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
			for (var executor : executors) {
				executor.shutdown();
				executor.awaitTermination(2, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
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
		System.out.println("**** All notes off");
		try {
			for (int i = 0; i < 16; i++) {
				for (int n = 0; n < 128; n++) {
					receiver.send(new ShortMessage(NOTE_OFF, i, n, 0), -1);
				}
			}
		} catch (InvalidMidiDataException e) {
			LOGGER.atError().log("Error turning all notes off: {}", e.getMessage());
		}
	}

}