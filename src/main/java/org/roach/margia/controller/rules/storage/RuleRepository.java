package org.roach.margia.controller.rules.storage;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private final Path rulesDir;
    private final YAMLMapper yamlMapper;
    private final Map<String, AbstractMusicianRule> builtInRules = new LinkedHashMap<>();
    private final Map<String, DataDrivenMusicianRule> templateRules = new LinkedHashMap<>();
    private final Map<String, DataDrivenMusicianRule> externalRules = new ConcurrentHashMap<>();
    private final List<Runnable> repositoryChangeListeners = new CopyOnWriteArrayList<>();

    private static final RuleRepository INSTANCE = new RuleRepository();

    private RuleRepository() {
        this(Paths.get(System.getProperty("user.home"), ".margia", "rules"));
    }

    /**
     * Constructor for unit testing against a temporary directory.
     * 
     * @param rulesDir directory to look for rules
     */
    public RuleRepository(Path rulesDir) {
        this.rulesDir = Objects.requireNonNull(rulesDir, "rulesDir must not be null");
        this.yamlMapper = YAMLMapper.builder().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES).build();
        loadBuiltInRules();
        loadTemplateRules();
        reloadExternalRules();
    }

    /**
     * @return the Singleton instance of {@link RuleRepository}
     */
    public static RuleRepository getInstance() { return INSTANCE; }

    private void loadTemplateRules() {
        templateRules.clear();
        try (var indexStream = getClass().getResourceAsStream("/rules/templates.txt")) {
            if (indexStream == null) {
                LOGGER.info("No bundled /rules/templates.txt manifest found.");
                return;
            }
            List<String> fileNames = new String(indexStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();

            for (String fileName : fileNames) {
                try (var yamlStream = getClass().getResourceAsStream("/rules/" + fileName)) {
                    if (yamlStream == null) {
                        LOGGER.warn("Bundled template not found: /rules/{}", fileName);
                        continue;
                    }
                    RuleDefinition def = yamlMapper.readValue(yamlStream, RuleDefinition.class);
                    templateRules.put(def.name(), new DataDrivenMusicianRule(def));
                    LOGGER.info("Loaded bundled YAML template rule: {}", def.name());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load bundled template rules", e);
        }
    }

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
            DataDrivenMusicianRule.validate(def);
            externalRules.put(def.name(), new DataDrivenMusicianRule(def));
            LOGGER.info("Loaded external YAML rule '{}' from {}", def.name(), file.getFileName());
        } catch (Exception e) {
            LOGGER.error("Failed to load rule file: {}", file, e);
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
        DataDrivenMusicianRule template = templateRules.get(name);
        if (template != null) {
            return Optional.of(template.copy());
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
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()));
        names.addAll(builtInRules.keySet());
        names.addAll(templateRules.keySet());
        names.addAll(externalRules.keySet());
        return List.copyOf(names);
    }

    /**
     * @return a map of template names and definitions
     */
    public Map<String, RuleDefinition> getTemplateDefinitions() {
        Map<String, RuleDefinition> defs = new LinkedHashMap<>();
        templateRules.forEach((name, rule) -> defs.put(name, rule.getDefinition()));
        return Collections.unmodifiableMap(defs);
    }

    /**
     * @param name the name of a rule
     * @return whether the rule is built-in (Java), a template
     *         (src/main/resources/rules) or user-defined
     *         (${user.home}/.margia/rules)
     */
    public RuleSource getRuleSource(String name) {
        if (externalRules.containsKey(name))
            return RuleSource.USER_YAML;
        if (templateRules.containsKey(name))
            return RuleSource.TEMPLATE_YAML;
        return RuleSource.BUILT_IN_JAVA;
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

    /**
     * @param name the name of the rule
     * @return {@code true} if the rule was loaded from a YAML file, versus
     *         internally in Java
     */
    public boolean isExternalRule(String name) {
        return externalRules.containsKey(name);
    }

    /**
     * @return the directory from which rules will be loaded
     */
    public Path getRulesDirectory() { return rulesDir; }

    /**
     * @param listener a listener for changes to the repository
     */
    public void addRepositoryChangeListener(Runnable listener) {
        if (listener != null) {
            repositoryChangeListeners.add(listener);
        }
    }

    /**
     * @param listener listener to be removed
     */
    public void removeRepositoryChangeListener(Runnable listener) {
        repositoryChangeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : repositoryChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                LOGGER.warn("Error notifying repository change listener", e);
            }
        }
    }

    private boolean isYamlFile(Path path) {
        if (!Files.isRegularFile(path))
            return false;
        String lower = path.getFileName().toString().toLowerCase();
        return lower.endsWith(".yaml") || lower.endsWith(".yml");
    }

    /**
     * @param definition a {@link RuleDefinition}
     * @return a YAML String representing the rule definition
     * @throws IOException if there is an error producing the YAML
     */
    public String toYamlString(RuleDefinition definition) throws IOException {
        return yamlMapper.writeValueAsString(definition);
    }

    /**
     * @param yamlContent The YAML string to parse
     * @return a {@link RuleDefinition} parsed from the string
     * @throws IOException if there is an error parsing the YAML
     */
    public RuleDefinition fromYamlString(String yamlContent) throws IOException {
        return yamlMapper.readValue(yamlContent, RuleDefinition.class);
    }

    /**
     * The source of a rule
     */
    public enum RuleSource {
        /**
         * Rule loaded from user's home directory .margia/rules
         */
        USER_YAML,
        /**
         * rule loaded from classpath (src/main/resources/rules)
         */
        TEMPLATE_YAML,
        /**
         * Rule hard-coded in Java
         */
        BUILT_IN_JAVA
    }

}
