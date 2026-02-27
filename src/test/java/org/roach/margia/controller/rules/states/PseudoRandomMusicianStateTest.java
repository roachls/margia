package org.roach.margia.controller.rules.states;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.roach.margia.controller.rules.NumericRange;

class PseudoRandomMusicianStateTest {
    private static org.roach.margia.controller.rules.states.PseudoRandomMusicianStateTest.CapturingAppender appender;

    private static class CapturingAppender extends AbstractAppender {
        final Queue<LogEvent> events = new LinkedBlockingQueue<>();

        protected CapturingAppender() {
            super("capture", null, null, false, new Property[0]);
        }

        @Override
        public void append(LogEvent event) {
            this.events.offer(event);
        }

    }

    @BeforeAll
    static void setup() {
        Logger logger = (Logger) LogManager.getLogger(PseudoRandomState.class);
        Configuration configuration = ((LoggerContext) LogManager.getContext()).getConfiguration();
        appender = new CapturingAppender();
        appender.start();
        configuration.addLoggerAppender(logger, appender);
        logger.addAppender(configuration.getAppender("capture"));
    }

    @Test
    void testWithStateTransition() {
        var throwawayState = new AlwaysTransitionState("hmm");
        var baseState = new PseudoRandomState("test", 30);
        assumeTrue(baseState.getStateMap().isEmpty());
        var minTooHigh = new NumericRange(31, 40);
        var ex = assertThrows(IllegalArgumentException.class,
                () -> baseState.withStateTransition(minTooHigh, throwawayState));
        assertTrue(baseState.getStateMap().isEmpty());
        assertThat(ex, isA(IllegalArgumentException.class));
        ex = null;
        var maxTooHigh = new NumericRange(15, 40);
        ex = assertThrows(IllegalArgumentException.class,
                () -> baseState.withStateTransition(maxTooHigh, throwawayState));
        assertTrue(baseState.getStateMap().isEmpty());
        assertThat(ex, isA(IllegalArgumentException.class));
        baseState.withStateTransition(new NumericRange(1, 5), throwawayState);
        assertEquals(1, baseState.getStateMap().size());
        assertTrue(appender.events.isEmpty());
        baseState.withStateTransition(new NumericRange(7, 10), throwawayState);
        assertTrue(appender.events.isEmpty());
        assertEquals(2, baseState.getStateMap().size());
        baseState.withStateTransition(new NumericRange(1, 5), throwawayState);
        assertEquals(2, baseState.getStateMap().size());
        assertCorrectLog();
        baseState.withStateTransition(new NumericRange(1, 2), throwawayState);
        assertEquals(2, baseState.getStateMap().size());
        assertCorrectLog();
        baseState.withStateTransition(new NumericRange(3, 5), throwawayState);
        assertEquals(2, baseState.getStateMap().size());
        assertCorrectLog();
        baseState.withStateTransition(new NumericRange(2, 4), throwawayState);
        assertEquals(2, baseState.getStateMap().size());
        assertCorrectLog();
    }

    private static void assertCorrectLog() {
        var latestLog = appender.events.poll();
        assertNotNull(latestLog);
        assertEquals("Error adding state transition", latestLog.getMessage().getFormattedMessage());
    }

}
