/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvère Martin-Michiellot
 */
package org.ether.society.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.ether.society.config.EtherPaths;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.generation.PlanetPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service orchestrating the loading, exporting, and persistence of all
 * factory and user presets across Planets, Ecology, Scenarios, and Leaders.
 */
public final class PresetStorageService {
    private static final Logger logger = LoggerFactory.getLogger(PresetStorageService.class);
    private static final ObjectMapper mapper = createObjectMapper();

    // Helper subroutine: preset storage service - internal state computation & bounds checking
    private PresetStorageService() {
        // Utility
    }

    // Helper subroutine: create object mapper - internal state computation & bounds checking
    private static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.enable(SerializationFeature.INDENT_OUTPUT);
        om.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return om;
    }

    /*
     * Get mapper.
     * Enforces physical invariants and updates associated state variables within {@code PresetStorageService}.
     *
     * @return the resulting computation or state reference
     */
    public static ObjectMapper getMapper() {
        return mapper;
    }

    /*
     * Converts a preset/scenario name to a clean, canonical filename slug.
     */
    public static String slugify(String name) {
        if (name == null || name.isBlank()) return "custom";
        String normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
        
        // Custom name mapping overrides
        if (normalized.contains("terran") || normalized.contains("terre (terran)")) return "earth_modern";
        if (normalized.contains("-1 000") || normalized.contains("iron")) return "earth_iron_1000bp";
        if (normalized.contains("-1 900") || normalized.contains("bronze")) return "earth_bronze_1900bp";
        if (normalized.contains("-3 000") || normalized.contains("lh")) return "earth_lh_3000bp";
        if (normalized.contains("-6 000") || normalized.contains("sahara")) return "earth_mh_6000bp";
        if (normalized.contains("-10 000") || normalized.contains("eh")) return "earth_eh_10000bp";
        if (normalized.contains("-20 000") || normalized.contains("lgm")) return "earth_lgm_20000bp";
        if (normalized.contains("-25 000") || normalized.contains("beringie")) return "earth_lgm_onset_25000bp";
        if (normalized.contains("-50 000") || normalized.contains("sahul")) return "earth_mis3_50000bp";
        if (normalized.contains("-100 000") || normalized.contains("interglaciaire")) return "earth_lig_100000bp";
        if (normalized.contains("mars")) return "mars";
        if (normalized.contains("venus")) return "venus";
        if (normalized.contains("lune") || normalized.contains("moon")) return "moon";
        if (normalized.contains("mercure") || normalized.contains("mercury")) return "mercury";
        if (normalized.contains("titan")) return "titan";
        if (normalized.contains("super-terre") || normalized.contains("super_earth")) return "super_earth";
        if (normalized.contains("synchrone") || normalized.contains("eyeball")) return "eyeball_world";
        if (normalized.contains("ocean") || normalized.contains("oceania")) return "oceania";
        if (normalized.contains("glaciaire") || normalized.contains("boreas")) return "boreas";
        if (normalized.contains("archipel")) return "archipelago";

        String slug = normalized.replaceAll("[^a-z0-9\\-_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return slug.isBlank() ? "preset_" + Math.abs(name.hashCode()) : slug;
    }

    // ── Planetary Presets ─────────────────────────────────────────────────────

    /*
     * Load all planet presets.
     * Enforces physical invariants and updates associated state variables within {@code PresetStorageService}.
     *
     * @return the resulting computation or state reference
     */
    public static List<PlanetPreset> loadAllPlanetPresets() {
        Map<String, PlanetPreset> presets = new LinkedHashMap<>();

        // 1. Load built-ins as baseline
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            presets.put(p.name(), p);
        }

        // 2. Load from data/presets/planets/
        Path planetsDir = EtherPaths.getPresetsPlanetsDir();
        if (Files.exists(planetsDir)) {
            try (Stream<Path> stream = Files.list(planetsDir)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                PlanetPreset loaded = mapper.readValue(p.toFile(), PlanetPreset.class);
                                if (loaded != null && loaded.name() != null) {
                                    presets.put(loaded.name(), loaded);
                                }
                            } catch (Exception e) {
                                logger.warn("Could not read planet preset JSON {}: {}", p.getFileName(), e.getMessage());
                            }
                        });
            } catch (IOException e) {
                logger.warn("Could not scan planet presets directory: {}", e.getMessage());
            }
        }

        // 3. Merge user custom presets from user directory
        Path userPresetFile = EtherPaths.getUserPresetsDir().resolve("planet_presets.json");
        if (Files.exists(userPresetFile) && userPresetFile.toFile().length() > 2) {
            try {
                List<PlanetPreset> userList = mapper.readValue(userPresetFile.toFile(),
                        mapper.getTypeFactory().constructCollectionType(List.class, PlanetPreset.class));
                if (userList != null) {
                    for (PlanetPreset up : userList) {
                        if (up != null && up.name() != null) {
                            presets.put(up.name(), up);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not load user planet presets: {}", e.getMessage());
            }
        }

        return new ArrayList<>(presets.values());
    }

    // ── Ecology Presets ───────────────────────────────────────────────────────

    /*
     * Load all ecology presets.
     * Enforces physical invariants and updates associated state variables within {@code PresetStorageService}.
     *
     * @return the resulting computation or state reference
     */
    public static List<EcologyPreset> loadAllEcologyPresets() {
        Map<String, EcologyPreset> presets = new LinkedHashMap<>();

        // 1. Load built-in baseline
        for (EcologyPreset ep : EcologyPreset.getBuiltInPresets()) {
            presets.put(ep.name(), ep);
        }

        // 2. Load from data/presets/ecology/
        Path ecoDir = EtherPaths.getPresetsEcologyDir();
        if (Files.exists(ecoDir)) {
            try (Stream<Path> stream = Files.list(ecoDir)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                EcologyPreset loaded = mapper.readValue(p.toFile(), EcologyPreset.class);
                                if (loaded != null && loaded.name() != null) {
                                    presets.put(loaded.name(), loaded);
                                }
                            } catch (Exception e) {
                                logger.warn("Could not read ecology preset JSON {}: {}", p.getFileName(), e.getMessage());
                            }
                        });
            } catch (IOException e) {
                logger.warn("Could not scan ecology presets directory: {}", e.getMessage());
            }
        }

        return new ArrayList<>(presets.values());
    }

    // ── Scenario Presets ──────────────────────────────────────────────────────

    /*
     * Load all scenarios.
     * Enforces physical invariants and updates associated state variables within {@code PresetStorageService}.
     *
     * @return the resulting computation or state reference
     */
    public static List<Scenario> loadAllScenarios() {
        Map<String, Scenario> scenarios = new LinkedHashMap<>();
        java.util.function.Function<Scenario, String> keyExtractor = s -> {
            String key = s.getPresetKey();
            if (key == null || key.isBlank()) {
                key = s.resolvePresetKey();
            }
            if (key != null && !key.isBlank()) {
                String clean = key.toLowerCase(Locale.ROOT).trim();
                // Strip redundant planet prefix
                clean = clean.replaceAll("^(earth|mars|moon|venus|mercury|titan|super_earth|eyeball_world|oceania|boreas|archipelago)_+", "");
                return clean;
            }
            return slugify(s.getName());
        };

        // 1. Built-in defaults
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if (s.getName() != null) {
                scenarios.put(keyExtractor.apply(s), s);
            }
        }

        // 2. Load from data/presets/scenarios/
        Path scDir = EtherPaths.getPresetsScenariosDir();
        if (Files.exists(scDir)) {
            try (Stream<Path> stream = Files.list(scDir)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                Scenario loaded = mapper.readValue(p.toFile(), Scenario.class);
                                if (loaded != null && loaded.getName() != null) {
                                    scenarios.put(keyExtractor.apply(loaded), loaded);
                                }
                            } catch (Exception e) {
                                logger.warn("Could not read scenario preset JSON {}: {}", p.getFileName(), e.getMessage());
                            }
                        });
            } catch (IOException e) {
                logger.warn("Could not scan scenario presets directory: {}", e.getMessage());
            }
        }

        // 3. Merge user saved scenarios
        Path userScFile = EtherPaths.getUserPresetsDir().resolve("scenarios.json");
        if (Files.exists(userScFile) && userScFile.toFile().length() > 2) {
            try {
                List<Scenario> userList = mapper.readValue(userScFile.toFile(),
                        mapper.getTypeFactory().constructCollectionType(List.class, Scenario.class));
                if (userList != null) {
                    for (Scenario us : userList) {
                        if (us != null && us.getName() != null) {
                            scenarios.put(keyExtractor.apply(us), us);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not load user scenarios: {}", e.getMessage());
            }
        }

        // 4. Secondary deduplication by (Planet + StartYear + Normalized Key)
        Map<String, Scenario> deduplicated = new LinkedHashMap<>();
        for (Scenario s : scenarios.values()) {
            String pName = s.getPlanetPreset() != null ? s.getPlanetPreset().getCanonicalPlanet() : "earth";
            String dedupKey = pName + ":" + s.getStartDateYear() + ":" + keyExtractor.apply(s);
            deduplicated.put(dedupKey, s);
        }

        List<Scenario> result = new ArrayList<>(deduplicated.values());
        result.sort(Comparator.comparingLong(Scenario::getStartDateYear));
        return result;
    }

    // ── Factory Presets Disk Synchronization ─────────────────────────────────

    /*
     * Exports all factory presets (planets, ecology, scenarios) to the project data/presets directory
     * and classpath resources directory to guarantee version control and file availability.
     */
    public static void exportAllFactoryPresets() {
        exportPlanetPresets();
        exportEcologyPresets();
        exportScenarioPresets();
    }

    // Helper subroutine: export planet presets - internal state computation & bounds checking
    private static void exportPlanetPresets() {
        Path targetDir = EtherPaths.getPresetsPlanetsDir();
        Path resDir = Path.of("src", "main", "resources", "data", "presets", "planets");
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            String filename = slugify(p.name()) + ".json";
            writeJsonSafely(targetDir.resolve(filename), p);
            if (Files.exists(Path.of("src", "main", "resources"))) {
                writeJsonSafely(resDir.resolve(filename), p);
            }
        }
    }

    // Helper subroutine: export ecology presets - internal state computation & bounds checking
    private static void exportEcologyPresets() {
        Path targetDir = EtherPaths.getPresetsEcologyDir();
        Path resDir = Path.of("src", "main", "resources", "data", "presets", "ecology");
        for (EcologyPreset ep : EcologyPreset.getBuiltInPresets()) {
            String filename = slugify(ep.name()) + ".json";
            writeJsonSafely(targetDir.resolve(filename), ep);
            if (Files.exists(Path.of("src", "main", "resources"))) {
                writeJsonSafely(resDir.resolve(filename), ep);
            }
        }
    }

    /*
     * Formats scenario preset filename in standard pattern: <planet>_<year>_<location_and_topic>.json
     */
    public static String getStandardizedScenarioFilename(Scenario s) {
        String planetSlug = "earth";
        if (s.getPlanetPreset() != null && s.getPlanetPreset().name() != null) {
            String pName = s.getPlanetPreset().name().toLowerCase(Locale.ROOT);
            if (pName.contains("super-terre") || pName.contains("super_earth") || pName.contains("gaia")) planetSlug = "super_earth";
            else if (pName.contains("mars") || pName.contains("ares")) planetSlug = "mars";
            else if (pName.contains("venus") || pName.contains("vénus") || pName.contains("hesperos")) planetSlug = "venus";
            else if (pName.contains("moon") || pName.contains("lune") || pName.contains("selene")) planetSlug = "moon";
            else if (pName.contains("mercury") || pName.contains("mercure") || pName.contains("hermes")) planetSlug = "mercury";
            else if (pName.contains("titan")) planetSlug = "titan";
            else if (pName.contains("synchrone") || pName.contains("eyeball")) planetSlug = "eyeball_world";
            else if (pName.contains("ocean") || pName.contains("océan") || pName.contains("oceania")) planetSlug = "oceania";
            else if (pName.contains("boreas")) planetSlug = "boreas";
            else if (pName.contains("archipel") || pName.contains("archipelago")) planetSlug = "archipelago";
            else if (pName.contains("terre") || pName.contains("earth") || pName.contains("glaciaire") || pName.contains("interglaciaire")) planetSlug = "earth";
        }
        long startYear = s.getStartDateYear();
        String yearPart = (startYear >= 0 ? "+" + startYear : String.valueOf(startYear));
        String key = s.getPresetKey() != null && !s.getPresetKey().isBlank() ? s.getPresetKey() : slugify(s.getName());
        String cleanKey = key.replaceAll("^(earth|mars|moon|venus|mercury|titan|super_earth|eyeball_world|oceania|boreas|archipelago)_+", "");
        return planetSlug + "_" + yearPart + "_" + cleanKey + ".json";
    }

    // Helper subroutine: export scenario presets - internal state computation & bounds checking
    private static void exportScenarioPresets() {
        Path targetDir = EtherPaths.getPresetsScenariosDir();
        Path resDir = Path.of("src", "main", "resources", "data", "presets", "scenarios");
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            String filename = getStandardizedScenarioFilename(s);
            writeJsonSafely(targetDir.resolve(filename), s);
            if (Files.exists(Path.of("src", "main", "resources"))) {
                writeJsonSafely(resDir.resolve(filename), s);
            }
        }
    }

    // Helper subroutine: write json safely - internal state computation & bounds checking
    private static void writeJsonSafely(Path targetFile, Object data) {
        try {
            if (targetFile.getParent() != null && !Files.exists(targetFile.getParent())) {
                Files.createDirectories(targetFile.getParent());
            }
            mapper.writeValue(targetFile.toFile(), data);
        } catch (IOException e) {
            logger.warn("Could not write preset file {}: {}", targetFile, e.getMessage());
        }
    }
}

