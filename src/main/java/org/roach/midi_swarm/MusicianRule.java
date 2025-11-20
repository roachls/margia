package org.roach.midi_swarm;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A rule for a {@link Musician} to follow when it 'hears' a note
 */
public abstract class MusicianRule {

	protected Musician musician;
	protected final Logger logger = LogManager.getLogger(getClass());
	protected List<Runnable> actionsToTake = new ArrayList<>();
	protected static final NoteInfo REST = new NoteInfo(-1, 0, Length.L1_16);

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
			logger.atWarn().log("{}: no actions to take", musician.getId());
			return;
		}
		for (var action : actionsToTake) {
			action.run();
		}
		actionsToTake.clear();
	}
}
