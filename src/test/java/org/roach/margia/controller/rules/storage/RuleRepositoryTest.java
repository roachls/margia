package org.roach.margia.controller.rules.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.roach.margia.controller.rules.AbstractMusicianRule;
import org.roach.margia.controller.rules.DataDrivenMusicianRule;
import org.roach.margia.controller.rules.storage.RuleDefinition.*;

/**
 * This test class is public because portions of it are called by another test
 * class
 */
public class RuleRepositoryTest {

    @TempDir
    Path tempRulesDir;

    private RuleRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RuleRepository(tempRulesDir);
    }

    @Test
    void testYamlRoundTripSerialization() throws IOException {
        RuleDefinition original = buildSampleRuleDefinition("roundTripRule");

        String yaml = repository.toYamlString(original);
        assertNotNull(yaml);
        assertTrue(yaml.contains("name: roundTripRule"));
        assertTrue(yaml.contains("COUNTDOWN"), () -> "Unexpected YAML output:\n" + yaml);

        RuleDefinition deserialized = repository.fromYamlString(yaml);
        assertEquals(original.name(), deserialized.name());
        assertEquals(original.startingState(), deserialized.startingState());
        assertEquals(original.properties().size(), deserialized.properties().size());
        assertEquals(original.states().keySet(), deserialized.states().keySet());
    }

    @Test
    void testSaveLoadAndDeleteInRepository() throws IOException {
        RuleDefinition def = buildSampleRuleDefinition("customRepoRule");

        repository.saveRule(def);
        assertTrue(Files.exists(tempRulesDir.resolve("customRepoRule.yaml")));
        assertTrue(repository.isExternalRule("customRepoRule"));
        assertTrue(repository.getAllRuleNames().contains("customRepoRule"));

        // Verify a fresh repository instance discovers the file on disk
        RuleRepository reloadedRepo = new RuleRepository(tempRulesDir);
        Optional<AbstractMusicianRule> loaded = reloadedRepo.createRuleByName("customRepoRule");
        assertTrue(loaded.isPresent());
        assertInstanceOf(DataDrivenMusicianRule.class, loaded.get());
        assertEquals("customRepoRule", loaded.get().getName());

        // Delete rule
        assertTrue(reloadedRepo.deleteExternalRule("customRepoRule"));
        assertFalse(Files.exists(tempRulesDir.resolve("customRepoRule.yaml")));
        assertTrue(reloadedRepo.createRuleByName("customRepoRule").isEmpty());
    }

    /**
     * @param name name of rule
     * @return new rule
     */
    public static RuleDefinition buildSampleRuleDefinition(String name) {
        Map<String, StateDef> states = new LinkedHashMap<>();
        states.put("tickDelay", new StateDef.Countdown("$initialTickDelay", "loopDelay"));
        states.put("loopDelay", new StateDef.DelayedTransition("$sequenceLength", "randomHub"));
        states.put("randomHub", new StateDef.PseudoRandom(20, List.of(new ActionDef("PLAY_CHORD", Map.of())),
                List.of(new RangeTransition(1, 10, "stepDown"))));
        states.put("stepDown", new StateDef.AlwaysTransition(
                List.of(new ActionDef("PLAY_CHORD_DOWN_INTERVAL", Map.of("interval", 5))), "loopDelay"));

        return new RuleDefinition(name, "tickDelay",
                List.of(new PropertyDef.IntProp("sequenceLength", "Sequence Length:", 16, 1, 256, 1),
                        new PropertyDef.IntProp("initialTickDelay", "Initial Tick Delay:", 0, 0, 256, 1)),
                states);
    }

    @Test
    void testBundledTemplatesLoadCleanly() {
        var manifestUrl = RuleRepository.class.getResource("/rules/templates.txt");
        assertNotNull(manifestUrl,
                "/rules/templates.txt not found on classpath (check target/classes/rules/ and pom.xml <resources>)");

        RuleRepository prodRepo = RuleRepository.getInstance();
        assertFalse(prodRepo.getTemplateDefinitions().isEmpty(), "Manifest found at " + manifestUrl
                + ", but 0 templates loaded (check console logs for YAML parse errors)");
    }

}
