package org.roach.margia.actions;

import org.roach.margia.Musician;
import org.roach.margia.NoteInfo;

/**
 * Command to play the given note down an octave. Note that this is different
 * from {@link PlayNoteDownInterval} with an interval of 12 or 8, which may not
 * be octaves depending on the current scale. (If the scale is
 * {@link org.roach.margia.Key#Chromatic}, then this is the equivalent of
 * {@link PlayNoteDownInterval} with an interval of 12. Note that if the
 * transformed note ends up below the key's range, it will be automatically
 * adjusted back.
 * 
 * @param musician {@link Musician} that will play the note
 * @param note     note to transform
 */
public record PlayNoteDownOctave(Musician musician, NoteInfo note) implements MusicalAction {

	@Override
	public void perform() {
		var newNoteNum = musician.getKey().adjustToKeyByOctaves(note.noteNum() - 12);
		musician.playNote(note.withNote(newNoteNum));
	}

}
