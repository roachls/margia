package org.roach.midi_swarm.mains;

import java.util.List;
import java.util.Scanner;

import org.roach.midi_swarm.*;
import org.roach.midi_swarm.rules.StateBasedRule;

/**
 * Run with random agent and a 4x4 grid
 */
public class TestSingleAgent {

	/**
	 * Main entry point
	 * 
	 * @param args args[0] = number of musicians
	 */
	public static void main(String[] args) {
		var tempo = 60;
		var controller = new MidiController(MidiController.DEFAULT_SYNTH, tempo);
		var r1 = new StateBasedRule(4, 2);
		var r2 = new StateBasedRule(4, 2);
		var musician1 = new Musician(0, controller, tempo, 0, r1);
		r1.setMusician(musician1);
		var musician2 = new Musician(1, controller, tempo, 1, r2);
		r2.setMusician(musician2);
		musician1.addPeer(musician2);
		musician2.addPeer(musician1);

		var transport = new Transport(List.of(musician1, musician2), tempo, controller);
		transport.start();

		musician1.receiveMessage(new NoteInfo(60, 60, 1));
		musician1.receiveMessage(new NoteInfo(62, 127, 2));
		musician1.receiveMessage(new NoteInfo(63, 60, 1));
		musician1.receiveMessage(new NoteInfo(65, 60, 4));

		try (var scanner = new Scanner(System.in)) {
			scanner.nextLine();

			transport.stop();
			controller.close();
		}

	}

}
