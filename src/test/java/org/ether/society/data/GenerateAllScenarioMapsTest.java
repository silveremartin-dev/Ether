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
    @DisplayName("Quick Step Benchmark")
    public void testQuickBenchmark() {
        long t0 = System.currentTimeMillis();
        logger.info("--- Starting Quick Ingestion Benchmark ---");
        List<double[]> coal = EmpiricalGeospatialDatasetIngestion.getEmpiricalCoalOccurrences();
        logger.info("Coal: {} points in {}ms", coal.size(), System.currentTimeMillis() - t0);

        long t1 = System.currentTimeMillis();
        List<double[]> oil = EmpiricalGeospatialDatasetIngestion.getEmpiricalOilOccurrences();
        logger.info("Oil: {} points in {}ms", oil.size(), System.currentTimeMillis() - t1);

        long t2 = System.currentTimeMillis();
        List<double[]> gas = EmpiricalGeospatialDatasetIngestion.getEmpiricalGasOccurrences();
        logger.info("Gas: {} points in {}ms", gas.size(), System.currentTimeMillis() - t2);

        long t3 = System.currentTimeMillis();
        List<double[]> aqu = EmpiricalGeospatialDatasetIngestion.getEmpiricalAquiferOccurrences();
        logger.info("Aquifers: {} points in {}ms", aqu.size(), System.currentTimeMillis() - t3);

        long t4 = System.currentTimeMillis();
        var heat = HistoricalMapGenerator.rasterizeMantleHeatMap("URBAN_CLUSTERS", null);
        logger.info("Mantle Heat generated in {}ms", System.currentTimeMillis() - t4);
    }

    @Test
    @DisplayName("Regenerate & Verify Precalculated Maps for Scenario -100000 (Out of Africa Baseline)")
    public void testGenerateScenarioMinus100k() throws IOException {
        List<Scenario> scenarios = Scenario.getBuiltInScenarios();
        Scenario sc100k = scenarios.stream()
                .filter(s -> s.getStartDateYear() == -100000L)
                .findFirst()
                .orElse(null);
        assertNotNull(sc100k, "Scenario -100000 must be defined");

        // Force regenerate scenario tensors
        HistoricalMapGenerator.forceGenerateScenarioHistoricalMaps(sc100k);

        File yearFolder = new File("data/maps/ether/earth/-100000");
        assertTrue(yearFolder.exists() && yearFolder.isDirectory());

        String[] requiredTensorFiles = {
            "density.png", "isogloss.png", "kinship.png", "rituals.png",
            "sovereignty.png", "technology.png", "tradenetwork.png",
            "institutional.png", "ecological.png", "pathogen.png"
        };

        for (String mapFile : requiredTensorFiles) {
            String stdFile = "earth_-100000_" + mapFile;
            File fStd = new File(yearFolder, stdFile);
            assertTrue(fStd.exists() && fStd.length() > 500, "Map file must exist: " + stdFile);
        }

        // Verify loading into Scenario instance
        HistoricalMapGenerator.populateScenarioHistoricalMaps(sc100k);
        assertNotNull(sc100k.getCustomDensityBase64(), "customDensityBase64 must be populated");
        for (int i = 0; i < 9; i++) {
            assertNotNull(sc100k.getCustomTensorMapBase64(i), "Tensor index " + i + " must not be null");
        }
        logger.info("Successfully regenerated and verified all -100000 scenario maps!");
    }

    @Test
    @DisplayName("Batch Generate & Verify Precalculated Maps for All Built-in Scenarios in data/maps/Earth/<year>/")
    public void testGenerateAllScenarioMaps() throws IOException {
        List<Scenario> scenarios = Scenario.getBuiltInScenarios();
        assertTrue(scenarios.size() >= 20, "Must have all canonical scenarios defined");

        // Ensure all scenario maps in data/maps/ether/earth/<year>/ are regenerated with true elevation mask
        HistoricalMapGenerator.ensureAllScenarioMapsGenerated(true);

        String[] requiredTensorFiles = {
            "density.png", "isogloss.png", "kinship.png", "rituals.png",
            "sovereignty.png", "technology.png", "tradenetwork.png",
            "institutional.png", "ecological.png", "pathogen.png"
        };

        for (Scenario sc : scenarios) {
            long year = sc.getStartDateYear();
            File yearFolder = new File("data/maps/ether/earth/" + year);
            assertTrue(yearFolder.exists() && yearFolder.isDirectory(),
                    "Scenario directory 'data/maps/ether/earth/" + year + "' must exist for scenario '" + sc.getName() + "'");

            for (String mapFile : requiredTensorFiles) {
                String stdFile = "earth_" + year + "_" + mapFile;
                File fStd = new File(yearFolder, stdFile);
                assertTrue(fStd.exists() && fStd.length() > 500,
                        "Standard map file '" + stdFile + "' in 'data/maps/ether/earth/" + year + "/' must exist and have content for " + sc.getName());
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

