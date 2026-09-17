package org.ether.society.data;

import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenerateAllScenarioMapsTest {
    private static final Logger logger = LoggerFactory.getLogger(GenerateAllScenarioMapsTest.class);

    @Test
    @DisplayName("Batch Generate & Verify Precalculated Maps for All Built-in Scenarios in data/maps/Earth/<year>/")
    public void testGenerateAllScenarioMaps() throws IOException {
        List<Scenario> scenarios = Scenario.getBuiltInScenarios();
        assertTrue(scenarios.size() >= 20, "Must have all canonical scenarios defined");

        // Ensure all scenario maps are generated in data/maps/Earth/<year>/
        HistoricalMapGenerator.ensureAllScenarioMapsGenerated();

        String[] requiredTensorFiles = {
            "density.png", "isogloss.png", "kinship.png", "rituals.png",
            "sovereignty.png", "technology.png", "tradenetwork.png",
            "institutional.png", "ecological.png", "pathogen.png"
        };

        for (Scenario sc : scenarios) {
            long year = sc.getStartDateYear();
            // Primary path: data/maps/ether/earth/<year>/
            File yearFolder = new File("data/maps/ether/earth/" + year);
            // Legacy fallbacks
            if (!yearFolder.exists()) yearFolder = new File("data/maps/Earth/" + year);
            if (!yearFolder.exists()) yearFolder = new File("data/maps/earth/" + year);
            assertTrue(yearFolder.exists() && yearFolder.isDirectory(),
                    "Scenario directory 'data/maps/ether/earth/" + year + "' must exist for scenario '" + sc.getName() + "'");

            for (String mapFile : requiredTensorFiles) {
                File f = new File(yearFolder, mapFile);
                assertTrue(f.exists() && f.length() > 500,
                        "Map file '" + mapFile + "' in 'data/maps/Earth/" + year + "/' must exist and have content for " + sc.getName());
            }

            // Verify loading into Scenario instance
            HistoricalMapGenerator.populateScenarioHistoricalMaps(sc);
            assertNotNull(sc.getCustomDensityBase64(),
                    "Scenario '" + sc.getName() + "' must have customDensityBase64 populated");
            assertTrue(!sc.getCustomDensityBase64().isBlank(),
                    "customDensityBase64 must not be blank for '" + sc.getName() + "'");

            for (int i = 0; i < 9; i++) {
                assertNotNull(sc.getCustomTensorMapBase64(i),
                        "Tensor index " + i + " must not be null for '" + sc.getName() + "'");
                assertTrue(!sc.getCustomTensorMapBase64(i).isBlank(),
                        "Tensor index " + i + " must not be blank for '" + sc.getName() + "'");
            }
        }

        logger.info("Successfully verified all {} scenarios with full tensor maps in data/maps/Earth/<year>/", scenarios.size());
    }
}

