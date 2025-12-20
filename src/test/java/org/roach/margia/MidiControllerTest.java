package org.roach.margia;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MidiControllerTest {

	@Test
	@Disabled("Only for local use with a DAW")
	void testPlayNote() throws InterruptedException {
		var controller = new MidiController(MidiController.LOOP_MIDI, 30);
		for (var note : Key.generateKey(Key.CHROMATIC_INTERVALS, List.of(Octave.O1)).notes()) {
			System.out.println("Playing " + note);
			controller.playNote(7, new NoteInfo(note, 70, 1));
			Thread.sleep(1000);
		}
		controller.close();
	}

}
