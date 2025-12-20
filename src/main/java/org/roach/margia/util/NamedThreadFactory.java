package org.roach.margia.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} that names threads, optionally appending a count
 */
public final class NamedThreadFactory implements ThreadFactory {
	private final String baseName;
	private final boolean counted;
	private AtomicInteger counter;

	/**
	 * @param baseName base name
	 * @param counted true to append a count to the name
	 */
	public NamedThreadFactory(final String baseName, final boolean counted) {
		this.baseName = baseName;
		this.counted = counted;
		if (counted)
			counter = new AtomicInteger(0);
	}
	
	/**
	 * @param baseName base name
	 */
	public NamedThreadFactory(final String baseName) {
		this(baseName, false);
	}

	@Override
	public Thread newThread(Runnable r) {
		var name = baseName;
		if (counted) {
			name += "_" + counter.getAndIncrement();
		}
		return new Thread(r, name);
	}
}