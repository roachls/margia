package org.roach.midi_swarm;

import static org.roach.midi_swarm.Note.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

@SuppressWarnings("javadoc")
public interface Key {
	Random RANDOM = new SecureRandom();

	Key CMajor = () -> List.of(C, D, E, F, G, A, B);
	Key CSharpMajor = () -> List.of(C_SHARP, D_SHARP, E_SHARP, F_SHARP, G_SHARP, A_SHARP, B_SHARP);
	Key DFlatMajor = () -> List.of(D_FLAT, E_FLAT, F, G_FLAT, A_FLAT, B_FLAT, C);
	Key DMajor = () -> List.of(D, E, F_SHARP, G, A, B, C_SHARP);
	Key EFlatMajor = () -> List.of(E_FLAT, F, G, A_FLAT, B_FLAT, C, D);
	Key EMajor = () -> List.of(E, F_SHARP, G_SHARP, A, B, C_SHARP, D_SHARP);
	Key FMajor = () -> List.of(F, G, A, B_FLAT, C, D, E);
	Key FSharpMajor = () -> List.of(F_SHARP, G_SHARP, A_SHARP, B, C_SHARP, D_SHARP, E_SHARP);
	Key GFlatMajor = () -> List.of(G_FLAT, A_FLAT, B_FLAT, C_FLAT, D_FLAT, E_FLAT, F);
	Key GMajor = () -> List.of(G, A, B, C, D, E, F_SHARP);
	Key AFlatMajor = () -> List.of(A_FLAT, B_FLAT, C, D_FLAT, E_FLAT, F, G);
	Key AMajor = () -> List.of(A, B, C_SHARP, D, E, F_SHARP, G_SHARP);
	Key BFlatMajor = () -> List.of(B_FLAT, C, D, E_FLAT, F, G, A);

	Key CMajorPentatonic = () -> List.of(C, D, E, G, A);
	Key Chromatic = () -> List.of(C, C_SHARP, D, D_SHARP, E, F, F_SHARP, G, G_SHARP, A, A_SHARP, B);

	List<Note> notes();

	default Note randomNote() {
		var noteNum = RANDOM.nextInt(notes().size());
		return notes().get(noteNum);
	}

	default Note upInterval(Note start, int interval) {
		List<Note> list = this.notes();
		int startIndex = list.indexOf(start);
		if (startIndex == -1)
			throw new IllegalArgumentException(start + " is not in this key");
		int endIndex = (startIndex + interval - 1) % list.size();
		return list.get(endIndex);
	}

	default Note downInterval(Note start, int interval) {
		List<Note> list = this.notes();
		int startIndex = list.indexOf(start);
		if (startIndex == -1)
			throw new IllegalArgumentException(start + " is not in this key");
		int endIndex = (startIndex - (interval - 1));
		if (endIndex < 0)
			endIndex += list.size();
		endIndex %= list.size();
		return list.get(endIndex);
	}

}
