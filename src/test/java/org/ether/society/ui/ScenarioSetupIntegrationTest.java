/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.application.Platform;
import org.ether.society.database.H3Cell;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.procedural.PlanetPreset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Integration Test Suite verifying ScenarioSetupPanel setup,
 * dynamic cell preview rendering, state propagation, and callback synchronization.
 */
public class ScenarioSetupIntegrationTest {

    private static boolean jfxInitialized = false;
    private static ScenarioSetupPanel sharedPanel;

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        if (!jfxInitialized) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(() -> {
                    try {
                        jfxInitialized = true;
                        Platform.setImplicitExit(false);
                        sharedPanel = new ScenarioSetupPanel(sc -> {});
                    } catch (Throwable t) {
                        t.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            } catch (IllegalStateException e) {
                jfxInitialized = true;
                Platform.runLater(() -> {
                    try {
                        if (sharedPanel == null) {
                            sharedPanel = new ScenarioSetupPanel(sc -> {});
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(30, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Verify ScenarioSetupPanel construction and default Scenario state generation")
    public void testScenarioGeneration() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Scenario scenario = sharedPanel.getScenario();
                assertNotNull(scenario, "Generated scenario should not be null");
                assertTrue(scenario.getInitialHumanCount() > 0, "Initial human count must be > 0");
                assertNotNull(scenario.getPlanetPreset(), "Planet preset must not be null");
                assertNotNull(scenario.getStartDateYear(), "Start date year must be present");
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Verify ScenarioSetupPanel setInheritedContext and preset updates")
    public void testInheritedContextSynchronization() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                sharedPanel.setInheritedContext(PlanetPreset.EARTH_LIKE, "Holocène Standard (-10 000 BC)");

                Scenario scenario = sharedPanel.getScenario();
                assertNotNull(scenario.getPlanetPreset());
                assertEquals("Terre (Terran)", scenario.getPlanetPreset().name());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Verify preview cell generation and dynamic rendering pipeline")
    public void testPreviewCellDynamicScalingPipeline() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                List<H3Cell> sampleCells = new ArrayList<>();
                for (int i = 0; i < 100; i++) {
                    H3Cell c = new H3Cell((long) i, (i % 10) * 10.0 - 45.0, (i / 10) * 20.0 - 90.0);
                    c.setPopulation(1000 + i * 50);
                    c.setElevation(100.0 + i * 10.0);
                    sampleCells.add(c);
                }

                sharedPanel.setGeneratedCells(sampleCells);
                assertEquals(100, sharedPanel.getCells().size(), "Generated cells count must match set cells");
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Verify Earth paleoclimate ocean waterLevel datum conforms to physical bathymetry (0.478 datum)")
    public void testEarthPaleoclimateOceanLevelDatum() {
        assertEquals(0.478, PlanetPreset.EARTH_MODERN.waterLevel(), 0.001);
        assertEquals(0.478, PlanetPreset.EARTH_LIG_100000BP.waterLevel(), 0.001);
        assertEquals(0.478, PlanetPreset.EARTH_MH_6000BP.waterLevel(), 0.001);
        assertEquals(0.476479, PlanetPreset.EARTH_EH_10000BP.waterLevel(), 0.0001);
        assertEquals(0.472568, PlanetPreset.EARTH_LGM_20000BP.waterLevel(), 0.0001);
    }

    @Test
    @DisplayName("Verify Sea Level in meters to normalized waterLevel conversion to the single meter precision")
    public void testSeaLevelMetersConversionAccuracy() {
        double minAlt = -11000.0;
        double maxAlt = 8848.0;

        // 0m MSL should map exactly to 0.478
        assertEquals(0.0, PlanetPreset.waterLevelToMeters(0.478, minAlt, maxAlt), 0.01);
        assertEquals(0.478, PlanetPreset.metersToWaterLevel(0.0, minAlt, maxAlt), 0.0001);

        // LGM -125m should map to ~0.472568 (rounded 0.473)
        double lgmW = PlanetPreset.metersToWaterLevel(-125.0, minAlt, maxAlt);
        assertEquals(0.472568, lgmW, 0.0001);
        assertEquals(-125.0, PlanetPreset.waterLevelToMeters(lgmW, minAlt, maxAlt), 0.01);

        // Early Holocene -35m
        double ehW = PlanetPreset.metersToWaterLevel(-35.0, minAlt, maxAlt);
        assertEquals(-35.0, PlanetPreset.waterLevelToMeters(ehW, minAlt, maxAlt), 0.01);

        // Positive Sea Level +50m
        double posW = PlanetPreset.metersToWaterLevel(50.0, minAlt, maxAlt);
        assertEquals(50.0, PlanetPreset.waterLevelToMeters(posW, minAlt, maxAlt), 0.01);

        // Full round-trip test across every meter from -1000m to +1000m
        for (int m = -1000; m <= 1000; m += 5) {
            double w = PlanetPreset.metersToWaterLevel(m, minAlt, maxAlt);
            double backMeters = PlanetPreset.waterLevelToMeters(w, minAlt, maxAlt);
            assertEquals((double) m, backMeters, 0.001, "Round-trip conversion failed for " + m + " meters");
        }
    }

    @Test
    @DisplayName("Verify Out of Africa scenario validation with Earth -100 000 preset")
    public void testOutOfAfricaCompatibilityValidation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                sharedPanel.setInheritedContext(PlanetPreset.EARTH_LIG_100000BP, "Terre (-100 000 / Dernier Interglaciaire)");
                boolean valid = sharedPanel.validateScenarioSetup();
                assertTrue(valid, "Scenario setup should be valid for Out of Africa with Earth -100 000");
                List<String> errors = sharedPanel.getScenarioValidationErrors();
                assertTrue(errors.isEmpty(), "There should be no validation errors: " + errors);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }
}

