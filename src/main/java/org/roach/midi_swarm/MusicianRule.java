package org.roach.midi_swarm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.roach.midi_swarm.actions.MusicalAction;

/**
 * A rule for a {@link Musician} to follow when it 'hears' a note
 */
public abstract class MusicianRule {

	protected Musician musician;
	protected final Logger logger = LogManager.getLogger(getClass());
	protected List<MusicalAction> actionsToTake = new ArrayList<>();
	/**
	 * Generates a rest of the given length in ticks
	 */
	public static final Function<Integer, NoteInfo> REST = l -> new NoteInfo(-1, 0, l);

	/**
	 * @param musician the {@link Musician} that this rule applies to
	 */
	public void setMusician(Musician musician) {
		this.musician = musician;
	}

	/**
	 * Calculate action to be taken when doAction is called. This should set
	 * nextNote.
	 * 
	 * @param tick the tick number
	 */
	public abstract void calculateAction(long tick);

	/**
	 * @param tick
	 */
	public void doAction(long tick) {
		if (actionsToTake.isEmpty()) {
			logger.atDebug().log("{}: no actions to take", musician.getId());
			return;
		}
		for (var action : actionsToTake) {
			action.perform();
		}
		actionsToTake.clear();
	}
}
