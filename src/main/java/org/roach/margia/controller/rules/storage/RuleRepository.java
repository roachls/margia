package org.roach.margia.controller.rules.storage;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.roach.margia.controller.rules.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Central registry and factory for both built-in ({@link ServiceLoader}) and
 * user-defined YAML {@link MusicianRule} implementations.
 */
public class RuleRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuleRepository.class);
    private static final RuleRepository INSTANCE = new RuleRepository();

    private final Path rulesDir;
    private final YAMLMapper yamlMapper;
    private final Map<String, AbstractMusicianRule> builtInRules = new LinkedHashMap<>();
    private final Map<String, DataDrivenMusicianRule> externalRules = new ConcurrentHashMap<>();
    private final List<Runnable> repositoryChangeListeners = new ArrayList<>();

    private RuleRepository() {
        this.rulesDir = Paths.get(System.getProperty("user.home"), ".margia", "rules");
        this.yamlMapper = YAMLMapper.builder().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES).build();
        loadBuiltInRules();
        reloadExternalRules();
    }

    public static RuleRepository getInstance() { return INSTANCE; }

    /**
     * Discovers all compiled {@link MusicianRule} implementations via
     * {@link ServiceLoader}.
     */
    private void loadBuiltInRules() {
        ServiceLoader<MusicianRule> loader = ServiceLoader.load(MusicianRule.class);
        for (MusicianRule rule : loader) {
            if (rule instanceof AbstractMusicianRule absRule) {
                builtInRules.put(absRule.getName(), absRule);
                LOGGER.info("Registered built-in rule: {}", absRule.getName());
            }
        }
    }

    /**
     * Scans the external rules directory for {@code *.yaml} / {@code *.yml} files
     * and loads them as {@link DataDrivenMusicianRule} prototypes.
     */
    public void reloadExternalRules() {
        externalRules.clear();
        try {
            Files.createDirectories(rulesDir);
            try (Stream<Path> files = Files.list(rulesDir)) {
                files.filter(this::isYamlFile).forEach(this::loadRuleFile);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan external rules directory: {}", rulesDir, e);
        }
        notifyListeners();
    }

    private void loadRuleFile(Path file) {
        try {
            RuleDefinition def = yamlMapper.readValue(file.toFile(), RuleDefinition.class);
            if (def.name() == null || def.name().isBlank()) {
                LOGGER.warn("Skipping rule file with missing 'name': {}", file);
                return;
            }
            DataDrivenMusicianRule rule = new DataDrivenMusicianRule(def);
            externalRules.put(def.name(), rule);
            LOGGER.info("Loaded external YAML rule '{}' from {}", def.name(), file.getFileName());
        } catch (Exception e) {
            LOGGER.error("Failed to parse rule file: {}", file, e);
        }
    }

    /**
     * Saves a {@link RuleDefinition} to disk as YAML and updates the active
     * registry.
     *
     * @param definition the rule definition to persist
     * @throws IOException if writing the YAML file fails
     */
    public void saveRule(RuleDefinition definition) throws IOException {
        Objects.requireNonNull(definition, "definition must not be null");
        if (definition.name() == null || definition.name().isBlank()) {
            throw new IllegalArgumentException("Rule name must not be blank");
        }
        Files.createDirectories(rulesDir);
        String sanitizedFileName = definition.name().replaceAll("[^a-zA-Z0-9._-]", "_") + ".yaml";
        Path targetFile = rulesDir.resolve(sanitizedFileName);

        yamlMapper.writeValue(targetFile.toFile(), definition);
        externalRules.put(definition.name(), new DataDrivenMusicianRule(definition));
        LOGGER.info("Saved external rule '{}' to {}", definition.name(), targetFile);
        notifyListeners();
    }

    /**
     * Deletes an external YAML rule from disk and removes it from the registry.
     *
     * @param ruleName name of the external rule to delete
     * @return true if deleted, false if not found or built-in
     * @throws IOException if file deletion fails
     */
    public boolean deleteExternalRule(String ruleName) throws IOException {
        if (!externalRules.containsKey(ruleName)) {
            return false;
        }
        String sanitizedFileName = ruleName.replaceAll("[^a-zA-Z0-9._-]", "_") + ".yaml";
        Path targetFile = rulesDir.resolve(sanitizedFileName);
        Files.deleteIfExists(targetFile);
        externalRules.remove(ruleName);
        notifyListeners();
        return true;
    }

    /**
     * Creates a fresh copy of a rule by name (checking external rules first so user
     * overrides take precedence over built-in rules of the same name).
     *
     * @param name the rule name
     * @return a new {@link AbstractMusicianRule} instance
     */
    public Optional<AbstractMusicianRule> createRuleByName(String name) {
        DataDrivenMusicianRule external = externalRules.get(name);
        if (external != null) {
            return Optional.of(external.copy());
        }
        AbstractMusicianRule builtIn = builtInRules.get(name);
        if (builtIn != null) {
            return Optional.of(builtIn.copy());
        }
        return Optional.empty();
    }

    /**
     * @return sorted list of all available rule names (built-in + external)
     */
    public List<String> getAllRuleNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(builtInRules.keySet());
        names.addAll(externalRules.keySet());
        return List.copyOf(names);
    }

    /**
     * @return unmodifiable map of external {@link RuleDefinition}s (for the in-app
     *         editor)
     */
    public Map<String, RuleDefinition> getExternalDefinitions() {
        Map<String, RuleDefinition> defs = new LinkedHashMap<>();
        externalRules.forEach((name, rule) -> defs.put(name, rule.getDefinition()));
        return Collections.unmodifiableMap(defs);
    }

    public boolean isExternalRule(String name) {
        return externalRules.containsKey(name);
    }

    public Path getRulesDirectory() { return rulesDir; }

    public void addRepositoryChangeListener(Runnable listener) {
        repositoryChangeListeners.add(listener);
    }

    private void notifyListeners() {
        repositoryChangeListeners.forEach(Runnable::run);
    }

    private boolean isYamlFile(Path path) {
        if (!Files.isRegularFile(path))
            return false;
        String lower = path.getFileName().toString().toLowerCase();
        return lower.endsWith(".yaml") || lower.endsWith(".yml");
    }

    public String toYamlString(RuleDefinition definition) throws IOException {
        return yamlMapper.writeValueAsString(definition);
    }

    public RuleDefinition fromYamlString(String yamlContent) throws IOException {
        return yamlMapper.readValue(yamlContent, RuleDefinition.class);
    }

}
