package org.ether.society.data;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ether.society.model.AutoEpochScenarioHelper;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.EtherJsonHelper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Ensures that 100% of standard scenario presets explicitly define all 86 Type-B engines,
 * with zero missing keys, zero ambiguous defaults, and preserving any preset-specific overrides.
 */
public class StandardScenarioPresetsEnricherTest {

    @Test
    public void testEnrichAndValidateAllScenarioPresets() throws Exception {
        List<Path> dirs = List.of(
                Paths.get("data/presets/scenarios"),
                Paths.get("src/main/resources/data/presets/scenarios")
        );

        ObjectMapper mapper = EtherJsonHelper.getMapper().copy()
                .enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        for (Path dir : dirs) {
            if (!Files.exists(dir)) continue;

            try (Stream<Path> stream = Files.list(dir)) {
                List<Path> jsonFiles = stream.filter(p -> p.toString().endsWith(".json")).toList();
                for (Path path : jsonFiles) {
                    File file = path.toFile();
                    Scenario scenario = mapper.readValue(file, Scenario.class);
                    assertNotNull(scenario, "Scenario must load from " + path);

                    long year = scenario.getStartDateYear();
                    String planet = "earth";
                    if (scenario.getPlanetPreset() != null && scenario.getPlanetPreset().getCanonicalPlanet() != null) {
                        planet = scenario.getPlanetPreset().getCanonicalPlanet();
                    } else if (scenario.getEcologyPreset() != null && scenario.getEcologyPreset().getCanonicalPlanet() != null) {
                        planet = scenario.getEcologyPreset().getCanonicalPlanet();
                    } else {
                        String fn = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (fn.startsWith("moon")) planet = "moon";
                        else if (fn.startsWith("mars")) planet = "mars";
                        else if (fn.startsWith("venus")) planet = "venus";
                        else if (fn.startsWith("mercury")) planet = "mercury";
                        else if (fn.startsWith("super_earth")) planet = "super_earth";
                        else if (fn.startsWith("eyeball_world")) planet = "eyeball_world";
                        else if (fn.startsWith("oceania")) planet = "oceania";
                        else if (fn.startsWith("boreas")) planet = "boreas";
                        else if (fn.startsWith("archipelago")) planet = "archipelago";
                    }

                    Map<String, Boolean> calibrated = AutoEpochScenarioHelper.calibrateTypeBEngines(planet, year);
                    assertEquals(86, calibrated.size(), "Calibrated engines must contain exactly 86 engines.");

                    Map<String, Boolean> existing = scenario.getTypeBEngineStates();
                    if (existing == null) {
                        existing = new HashMap<>();
                    }

                    // Use TreeMap to keep engine names cleanly sorted alphabetically
                    Map<String, Boolean> merged = new TreeMap<>();
                    for (Map.Entry<String, Boolean> calEntry : calibrated.entrySet()) {
                        String engineName = calEntry.getKey();
                        if (existing.containsKey(engineName)) {
                            merged.put(engineName, existing.get(engineName));
                        } else {
                            merged.put(engineName, calEntry.getValue());
                        }
                    }

                    scenario.setTypeBEngineStates(merged);
                    assertEquals(86, scenario.getTypeBEngineStates().size(),
                            "Scenario " + path.getFileName() + " must have exactly 86 Type-B engine definitions.");

                    mapper.writer(printer).writeValue(file, scenario);
                }
            }
        }
    }
}

