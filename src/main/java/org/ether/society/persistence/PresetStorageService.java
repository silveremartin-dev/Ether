/*
 * MIT License
 *
 * Copyright (c) 2024-2026 SilvÃ¨re Martin-Michiellot
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

    private PresetStorageService() {
        // Utility
    }

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

    // â”€â”€ Planetary Presets â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ Ecology Presets â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ Scenario Presets â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /*
     * Load all scenarios.
     * Enforces physical invariants and updates associated state variables within {@code PresetStorageService}.
     *
     * @return the resulting computation or state reference
     */
    public static List<Scenario> loadAllScenarios() {
        Map<String, Scenario> scenarios = new LinkedHashMap<>();

        // 1. Built-in defaults
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            if (s.getName() != null) {
                scenarios.put(s.getName(), s);
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
                                    scenarios.put(loaded.getName(), loaded);
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
                            scenarios.put(us.getName(), us);
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not load user scenarios: {}", e.getMessage());
            }
        }

        List<Scenario> result = new ArrayList<>(scenarios.values());
        result.sort(Comparator.comparingLong(Scenario::getStartDateYear));
        return result;
    }

    // â”€â”€ Factory Presets Disk Synchronization â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /*
     * Exports all factory presets (planets, ecology, scenarios) to the project data/presets directory
     * and classpath resources directory to guarantee version control and file availability.
     */
    public static void exportAllFactoryPresets() {
        exportPlanetPresets();
        exportEcologyPresets();
        exportScenarioPresets();
    }

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

    private static void exportScenarioPresets() {
        Path targetDir = EtherPaths.getPresetsScenariosDir();
        Path resDir = Path.of("src", "main", "resources", "data", "presets", "scenarios");
        for (Scenario s : Scenario.getBuiltInScenarios()) {
            String slug = s.getPresetKey() != null && !s.getPresetKey().isBlank()
                    ? s.getPresetKey()
                    : slugify(s.getName());
            String filename = slug + ".json";
            writeJsonSafely(targetDir.resolve(filename), s);
            if (Files.exists(Path.of("src", "main", "resources"))) {
                writeJsonSafely(resDir.resolve(filename), s);
            }
        }
    }

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

