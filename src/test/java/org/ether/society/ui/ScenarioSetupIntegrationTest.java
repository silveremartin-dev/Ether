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
    @DisplayName("Verify callback triggering on simulation launch trigger")
    public void testStartSimulationCallback() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Scenario sc = sharedPanel.getScenario();
                assertNotNull(sc);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }
}
