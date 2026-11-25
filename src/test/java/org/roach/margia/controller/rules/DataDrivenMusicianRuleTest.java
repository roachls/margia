package org.roach.margia.controller.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.roach.margia.controller.Musician;
import org.roach.margia.controller.rules.storage.RuleDefinition;
import org.roach.margia.controller.rules.storage.RuleDefinition.*;
import org.roach.margia.controller.rules.storage.RuleRepositoryTest;
import org.roach.margia.model.MusicianOptions;
import org.roach.margia.model.RuleOptions;
import org.roach.margia.storage.Options;

class DataDrivenMusicianRuleTest {

    @Test
    void testPropertyDescriptorsAndCyclicStateInitialization() {
        RuleDefinition def = RuleRepositoryTest.buildSampleRuleDefinition("cyclicTestRule");
        DataDrivenMusicianRule rule = new DataDrivenMusicianRule(def);

        // Verify RulePropertyDescriptor mapping
        List<RulePropertyDescriptor> descriptors = rule.getPropertyDescriptors();
        assertEquals(2, descriptors.size());
        assertInstanceOf(RulePropertyDescriptor.IntProperty.class, descriptors.get(0));
        assertEquals("sequenceLength", descriptors.get(0).key());

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

            // Verify copy() creates an independent instance with the same definition
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
