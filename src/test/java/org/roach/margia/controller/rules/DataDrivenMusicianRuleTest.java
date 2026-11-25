package org.roach.margia.controller.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.storage.RuleDefinition;
import org.roach.margia.controller.rules.storage.RuleDefinition.*;
import org.roach.margia.controller.rules.storage.RuleRepository;
import org.roach.margia.controller.rules.storage.RuleRepositoryTest;
import org.roach.margia.model.MusicianOptions;
import org.roach.margia.model.RuleOptions;
import org.roach.margia.storage.Options;

class DataDrivenMusicianRuleTest {

    @Test
    void testPropertyDescriptorsAndCyclicStateInitialization() {
        RuleDefinition def = RuleRepositoryTest.buildSampleRuleDefinition("cyclicTestRule");
        DataDrivenMusicianRule rule = new DataDrivenMusicianRule(def);

        List<RulePropertyDescriptor> descriptors = rule.getPropertyDescriptors();
        assertEquals(2, descriptors.size());
        assertInstanceOf(RulePropertyDescriptor.IntProperty.class, descriptors.getFirst());
        assertEquals("sequenceLength", descriptors.getFirst().key());

        Musician musician = Musician.newInstance();
        int musicianId = musician.getId();
        boolean addedTempOptions = false;

        try {
            MusicianOptions musicianOpts = Options.getInstance().getMusicians().computeIfAbsent(musicianId,
                    _ -> new MusicianOptions());
            addedTempOptions = true;
            RuleOptions ruleOpts = musicianOpts.getRuleOptions();
            ruleOpts.setName("cyclicTestRule");
            ruleOpts.setRuleSpecificOption("sequenceLength", 8);
            ruleOpts.setRuleSpecificOption("initialTickDelay", 2);

            rule.setMusician(musician);
            assertDoesNotThrow(rule::initActionsAfterMusicianAssigned);

            AbstractMusicianRule copy = rule.copy();
            assertNotSame(rule, copy);
            assertEquals("cyclicTestRule", copy.getName());
        } finally {
            if (addedTempOptions) {
                Options.getInstance().getMusicians().remove(musicianId);
            }
        }
    }

    @Test
    void testAllStateTypesAndAllActionsWireCleanly() {
        Map<String, StateDef> states = new LinkedHashMap<>();

        // 1. DIE_ROLLING + chord manipulation actions
        states.put("dieHub", new StateDef.DieRolling("$diceSpec", List.of(new ActionDef("PLAY_CHORD", Map.of()),
                new ActionDef("PLAY_CHORD_UP_INTERVAL", Map.of("interval", 3)),
                new ActionDef("PLAY_CHORD_DOWN_INTERVAL", Map.of("interval", 5)),
                new ActionDef("PLAY_CHORD_TWICE_LENGTH", Map.of()), new ActionDef("PLAY_CHORD_HALF_LENGTH", Map.of()),
                new ActionDef("PLAY_CHORD_UP_VELOCITY", Map.of("diff", 10)),
                new ActionDef("PLAY_CHORD_DOWN_VELOCITY", Map.of("diff", 10)),
                new ActionDef("PLAY_NOTE_UP_OCTAVE", Map.of())),
                List.of(new RangeTransition(1, 3, "noteRouter"), new RangeTransition(4, 6, "condGate")), 120.0, 200.0));

        // 2. NOTE_BASED + random/procedural chord actions
        states.put("noteRouter", new StateDef.NoteBased(
                List.of(new ActionDef("PLAY_PSEUDO_RANDOM_CHORD", Map.of("spread", 20, "num", 10, "numNotes", 3)),
                        new ActionDef("PLAY_PSEUDO_RANDOM_CHORD_FLAVOR",
                                Map.of("spread", 20, "num", 10, "flavor", "MAJOR")),
                        new ActionDef("PLAY_RANDOM_CHORD",
                                Map.of("dice", "1d20", "num", 10, "maxNotesDiceDescription", "1d4"))),
                List.of(new NoteTransition(Set.of(0, 4, 7), "terminalState")), 300.0, 200.0));

        // 3. CONDITIONAL + repetition/utility actions
        states.put("condGate", new StateDef.Conditional("CHORDS_PLAYED_GTE", "$minChords",
                List.of(new ActionDef("REPEAT_LAST_CHORD", Map.of()), new ActionDef("SET_LAST_CHORD", Map.of()),
                        new ActionDef("REST_ONE_TICK", Map.of()), new ActionDef("RESET_PLAYED_CHORDS", Map.of())),
                "terminalState", "dieHub", 300.0, 360.0));

        // 4. NEVER_TRANSITION + mute/unmute actions
        states.put("terminalState",
                new StateDef.NeverTransition(
                        List.of(new ActionDef("MUTE_MUSICIAN", Map.of()), new ActionDef("UNMUTE_MUSICIAN", Map.of())),
                        500.0, 280.0));

        RuleDefinition fullDef = new RuleDefinition("allStatesAndActionsRule", "dieHub",
                List.of(new PropertyDef.StringProp("diceSpec", "Dice:", "1d6"),
                        new PropertyDef.IntProp("minChords", "Min Chords:", 4, 1, 32, 1)),
                states);

        DataDrivenMusicianRule rule = new DataDrivenMusicianRule(fullDef);
        Musician musician = Musician.newInstance();
        int musicianId = musician.getId();

        try {
            Options.getInstance().getMusicians().putIfAbsent(musicianId, new MusicianOptions());
            rule.setMusician(musician);
            assertDoesNotThrow(rule::initActionsAfterMusicianAssigned);
        } finally {
            Options.getInstance().getMusicians().remove(musicianId);
        }
    }

    @Test
    void testTickExecutionAndConditionalMuteUnmuteTransitions() {
        Map<String, StateDef> states = new LinkedHashMap<>();
        // Start unmuted -> check IS_MUTED (false) -> branch to muteAndHold
        states.put("checkMute", new StateDef.Conditional("IS_MUTED", 0, List.of(), "unmuteStep", "muteStep"));
        states.put("muteStep",
                new StateDef.AlwaysTransition(List.of(new ActionDef("MUTE_MUSICIAN", Map.of())), "checkMute"));
        states.put("unmuteStep", new StateDef.NeverTransition(List.of(new ActionDef("UNMUTE_MUSICIAN", Map.of()))));

        RuleDefinition def = new RuleDefinition("muteToggleRule", "checkMute", List.of(), states);
        DataDrivenMusicianRule rule = new DataDrivenMusicianRule(def);

        Musician musician = Musician.newInstance();
        int musicianId = musician.getId();

        try {
            MusicianOptions mOpts = Options.getInstance().getMusicians().computeIfAbsent(musicianId,
                    _ -> new MusicianOptions());
            mOpts.setMuted(false);

            rule.setMusician(musician);
            rule.initActionsAfterMusicianAssigned();

            // Tick 1: in 'checkMute', IS_MUTED is false -> transitions to 'muteStep'
            rule.calculateAction(1L);
            rule.doAction();
            assertFalse(mOpts.isMuted());

            // Tick 2: in 'muteStep', queues MUTE_MUSICIAN and transitions back to
            // 'checkMute'
            rule.calculateAction(2L);
            rule.doAction();
            assertTrue(mOpts.isMuted());

            // Tick 3: in 'checkMute', IS_MUTED is now true -> transitions to 'unmuteStep'
            rule.calculateAction(3L);
            rule.doAction();
            assertTrue(mOpts.isMuted());

            // Tick 4: in 'unmuteStep', queues UNMUTE_MUSICIAN and stays in terminal state
            rule.calculateAction(4L);
            rule.doAction();
            assertFalse(mOpts.isMuted());

            // Reset returns state back to startingState ('checkMute')
            rule.reset();
            assertFalse(mOpts.isMuted());
        } finally {
            Options.getInstance().getMusicians().remove(musicianId);
        }
    }

    @Test
    void testGraphCoordinatesPersistAcrossYamlRoundTrip(@TempDir Path tempDir) throws IOException {
        RuleRepository repo = new RuleRepository(tempDir);
        Map<String, StateDef> states = new LinkedHashMap<>();
        states.put("alpha", new StateDef.AlwaysTransition(List.of(), "beta", 123.5, 456.0));
        states.put("beta", new StateDef.NeverTransition(List.of(), 789.0, 101.5));

        RuleDefinition original = new RuleDefinition("coordsRule", "alpha", List.of(), states);
        String yaml = repo.toYamlString(original);
        assertTrue(yaml.contains("123.5"));
        assertTrue(yaml.contains("456.0"));

        RuleDefinition restored = repo.fromYamlString(yaml);
        assertEquals(123.5, restored.states().get("alpha").x());
        assertEquals(456.0, restored.states().get("alpha").y());
        assertEquals(789.0, restored.states().get("beta").x());
        assertEquals(101.5, restored.states().get("beta").y());
    }

    @Test
    void testMissingStartingStateThrowsClearException() {
        RuleDefinition invalidDef = new RuleDefinition("brokenRule", "nonExistentState", List.of(),
                Map.of("onlyState", new StateDef.AlwaysTransition(List.of(), "onlyState")));

        DataDrivenMusicianRule rule = new DataDrivenMusicianRule(invalidDef);
        Musician musician = Musician.newInstance();
        int musicianId = musician.getId();

        try {
            Options.getInstance().getMusicians().putIfAbsent(musicianId, new MusicianOptions());
            rule.setMusician(musician);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    rule::initActionsAfterMusicianAssigned);
            assertTrue(ex.getMessage().contains("nonExistentState"));
        } finally {
            Options.getInstance().getMusicians().remove(musicianId);
        }
    }

    @Test
    void testUnknownPlaceholderThrowsClearException() {
        RuleDefinition badPlaceholderDef = new RuleDefinition("badPlaceholderRule", "start", List.of(),
                Map.of("start", new StateDef.DelayedTransition("$undefinedProp", "start")));
        DataDrivenMusicianRule rule = new DataDrivenMusicianRule(badPlaceholderDef);
        Musician musician = Musician.newInstance();
        int musicianId = musician.getId();
        try {
            Options.getInstance().getMusicians().putIfAbsent(musicianId, new MusicianOptions());
            rule.setMusician(musician);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    rule::initActionsAfterMusicianAssigned);
            assertTrue(ex.getMessage().contains("$undefinedProp"));
        } finally {
            Options.getInstance().getMusicians().remove(musicianId);
        }
    }
}