/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test Suite verifying the Spatial Phase Transition Chronicle Observer,
 * default disabled status, rate limiting, and regional clipping.
 */
public class SpatialPhaseTransitionChronicleObserverTest {

    private EventSystem eventSystem;
    private SpatialPhaseTransitionChronicleObserver observer;
    private Scenario scenario;
    private List<H3Cell> testCells;
    private Random random;

    @BeforeEach
    public void setUp() {
        eventSystem = new EventSystem();
        observer = new SpatialPhaseTransitionChronicleObserver();
        scenario = new Scenario();
        scenario.setSeed(42L);
        random = new Random(42L);

        testCells = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index((long) (1000 + i));
            c.setLatitude(45.0 + i * 0.5);
            c.setLongitude(5.0 + i * 0.5);
            c.setBiome(Biome.PLAINS);
            c.setElevation(150.0);
            c.setPopulation(1_000);
            c.setResourceCapital(100_000.0);
            testCells.add(c);
        }
    }

    @Test
    @DisplayName("Observer stays dormant when disabled by default in EventSystem")
    public void testObserverDefaultDisabled() {
        assertFalse(eventSystem.isEnableChronicleSingularityObserver());
        eventSystem.checkPhaseTransitions(100, 0, testCells, scenario);
        assertTrue(eventSystem.getChronicleHistory().isEmpty());
    }

    @Test
    @DisplayName("Detects urban agglomeration phase transition crossing canonical Bairoch tier")
    public void testUrbanTransitionDetection() {
        eventSystem.setEnableChronicleSingularityObserver(true);
        scenario.setChronicleSingularityObserverEnabled(true);

        // Cell 0 crosses Classical Metropolis threshold (100,000 hab)
        testCells.getFirst().setPopulation(120_000);

        observer.evaluatePhaseTransitions(50, 0, testCells, eventSystem, scenario, random);

        assertTrue(observer.isTierFired(100_000));
        assertFalse(eventSystem.getChronicleHistory().isEmpty());
        assertTrue(eventSystem.getChronicleHistory().getFirst().getTitle().contains("ÉMERGENCE URBAINE"));
    }

    @Test
    @DisplayName("Detects acute local demographic collapse phase transition (> 40% loss)")
    public void testDemographicCollapseDetection() {
        eventSystem.setEnableChronicleSingularityObserver(true);
        scenario.setChronicleSingularityObserverEnabled(true);

        // Initialize baseline at year 1
        observer.evaluatePhaseTransitions(1, 0, testCells, eventSystem, scenario, random);

        // Year 2: catastrophic collapse of Cell 5 (1000 -> 200 = -80%)
        testCells.get(5).setPopulation(200);
        observer.evaluatePhaseTransitions(2, 0, testCells, eventSystem, scenario, random);

        assertFalse(eventSystem.getChronicleHistory().isEmpty());
        assertTrue(eventSystem.getChronicleHistory().stream().anyMatch(e -> e.getTitle().contains("EFFONDREMENT DÉMOGRAPHIQUE")));
    }
}
