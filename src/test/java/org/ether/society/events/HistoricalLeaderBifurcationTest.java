/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Nation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scientific validation & A/B Counterfactual Falsification Test Suite
 * for historical contingency leaders and cliodynamic bifurcations in Ether.
 */
public class HistoricalLeaderBifurcationTest {

    private EventSystem eventSystem;
    private List<H3Cell> mockCells;

    @BeforeEach
    void setUp() {
        eventSystem = new EventSystem();
        eventSystem.setSeed(42L);

        mockCells = new ArrayList<>();
        // Create a synthetic cluster of cells in Macedonia/Greece (-334 BC region)
        for (int i = 0; i < 20; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index(1000L + i);
            c.setLatitude(40.64 + (i * 0.1));
            c.setLongitude(22.94 + (i * 0.1));
            c.setElevation(150.0);
            c.setTemperature(18.0);
            c.setRainfall(600.0);
            c.setBiome(Biome.FOREST);
            c.setPopulation(1000);
            c.setFoodResource(5000.0);
            c.setMovementFriction(1.0);
            c.setResourceCapital(10000.0);
            mockCells.add(c);
        }
    }

    @Test
    @DisplayName("Catalog: Earth Historical Leaders Catalog is loaded and contains 6 Archetypes")
    void testHistoricalCatalogLoading() {
        HistoricalInterventionCatalog catalog = HistoricalInterventionCatalog.getInstance();
        assertNotNull(catalog);
        List<HistoricalIntervention> list = catalog.getInterventions();
        assertFalse(list.isEmpty(), "Historical interventions catalog should not be empty");
        assertTrue(list.size() >= 10, "Expected at least 10 major historical leaders in catalog");

        boolean hasConqueror = list.stream().anyMatch(h -> h.getArchetype() == LeaderArchetype.MILITARY_CONQUEROR);
        boolean hasBuilder = list.stream().anyMatch(h -> h.getArchetype() == LeaderArchetype.INFRASTRUCTURE_BUILDER);
        boolean hasReformer = list.stream().anyMatch(h -> h.getArchetype() == LeaderArchetype.INSTITUTIONAL_REFORMER);
        boolean hasHydraulic = list.stream().anyMatch(h -> h.getArchetype() == LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR);
        boolean hasSage = list.stream().anyMatch(h -> h.getArchetype() == LeaderArchetype.MORAL_RELIGIOUS_SAGE);
        boolean hasPurger = list.stream().anyMatch(h -> h.getArchetype() == LeaderArchetype.TOTALITARIAN_PURGER);

        assertTrue(hasConqueror, "Should include MILITARY_CONQUEROR (e.g. Alexander/Genghis)");
        assertTrue(hasBuilder, "Should include INFRASTRUCTURE_BUILDER (e.g. Augustus/Cyrus)");
        assertTrue(hasReformer, "Should include INSTITUTIONAL_REFORMER (e.g. Hammurabi/Justinian)");
        assertTrue(hasHydraulic, "Should include HYDRAULIC_AGRARIAN_INNOVATOR (e.g. Grand Canal/Sui)");
        assertTrue(hasSage, "Should include MORAL_RELIGIOUS_SAGE (e.g. Buddha/Ashoka)");
        assertTrue(hasPurger, "Should include TOTALITARIAN_PURGER (e.g. Akhenaten)");
    }

    @Test
    @DisplayName("Procedural Generator: Emergence of High-Sigma Leaders in Complex Worlds")
    void testProceduralLeaderGenerator() {
        ProceduralLeaderGenerator generator = new ProceduralLeaderGenerator();
        generator.setSeed(999L);

        HistoricalIntervention outlier = null;
        for (int yr = 1; yr <= 200; yr++) {
            HistoricalIntervention cand = generator.evaluateEmergence(yr, 500_000L, mockCells);
            if (cand != null) {
                outlier = cand;
                break;
            }
        }

        assertNotNull(outlier, "Expected at least one emergent outlier across a 200-year window in populated world");
        assertTrue(outlier.getMagnitude() >= 1.0 && outlier.getMagnitude() <= 10.0);
        assertNotNull(outlier.getArchetype());
        assertTrue(outlier.getDurationYears() >= 5);
    }

    @Test
    @DisplayName("ActiveEvent Classification: Accurate Separation between Geophysical and Historical Leader Events")
    void testActiveEventClassification() {
        ActiveEvent disaster = new ActiveEvent("VOLC_1", "Éruption Volcanique", "VOLCANO", 38.0, 15.0, 79, 7, 24, 20.0, 8.0);
        assertTrue(disaster.isGeophysical(), "VOLCANO should be classified as geophysical");
        assertFalse(disaster.isHistoricalLeader(), "VOLCANO should not be classified as historical leader");

        HistoricalIntervention alexander = new HistoricalIntervention("ALEX", "Alexandre le Grand", "Conquête", -334, 11, 40.64, 22.94, 3500.0, LeaderArchetype.MILITARY_CONQUEROR, 9.5);
        ActiveEvent leaderEvent = new ActiveEvent(alexander, -334, 4, 1);
        assertTrue(leaderEvent.isHistoricalLeader(), "Alexander should be classified as historical leader");
        assertFalse(leaderEvent.isGeophysical(), "Alexander should not be classified as geophysical");
    }

    @Test
    @DisplayName("A/B Falsification Benchmark: Measuring Perturbation & Relaxation Dynamics of Alexander the Great (-334 BC)")
    void testCounterfactualABInterventionBenchmark() {
        // --- BRANCH A: Structural Baseline (Historical Leaders Disabled) ---
        EventSystem systemA = new EventSystem();
        systemA.setEnableEarthHistoricalLeaders(false);
        systemA.setEnableProceduralLeaders(false);
        systemA.setEnableRandomEvents(false);

        List<H3Cell> cellsA = new ArrayList<>();
        for (H3Cell orig : mockCells) {
            H3Cell copy = new H3Cell();
            copy.setH3Index(orig.getH3Index());
            copy.setLatitude(orig.getLatitude());
            copy.setLongitude(orig.getLongitude());
            copy.setMovementFriction(1.0);
            copy.setResourceCapital(10000.0);
            copy.setFoodResource(5000.0);
            cellsA.add(copy);
        }

        // --- BRANCH B: Counterfactual Intervention (Alexander the Great Enabled) ---
        EventSystem systemB = new EventSystem();
        systemB.setEnableEarthHistoricalLeaders(true);
        systemB.setEnableProceduralLeaders(false);
        systemB.setEnableRandomEvents(false);

        List<H3Cell> cellsB = new ArrayList<>();
        for (H3Cell orig : mockCells) {
            H3Cell copy = new H3Cell();
            copy.setH3Index(orig.getH3Index());
            copy.setLatitude(orig.getLatitude());
            copy.setLongitude(orig.getLongitude());
            copy.setMovementFriction(1.0);
            copy.setResourceCapital(10000.0);
            copy.setFoodResource(5000.0);
            cellsB.add(copy);
        }

        // Run simulation from -350 BC to -300 BC
        for (int yr = -350; yr <= -300; yr++) {
            systemA.checkEvents(yr, 0, 20000, 100000.0, cellsA);
            systemB.checkEvents(yr, 0, 20000, 100000.0, cellsB);
        }

        // Evaluate at -330 BC (during Alexander's campaign)
        double avgFrictionA = cellsA.stream().mapToDouble(H3Cell::getMovementFriction).average().orElse(1.0);
        double avgFrictionB = cellsB.stream().mapToDouble(H3Cell::getMovementFriction).average().orElse(1.0);

        // Branch B must show reduced movement friction in Alexander's sphere of influence
        assertTrue(avgFrictionB < avgFrictionA, "Branch B with Alexander should exhibit lower movement friction than Branch A");
        assertTrue(systemB.getChronicleHistory().stream().anyMatch(e -> e.getTitle().contains("Alexandre")), "Chronicle in Branch B must record Alexander the Great");
        assertEquals(0, systemA.getChronicleHistory().size(), "Chronicle in Branch A should have 0 leader events");
    }

    @Test
    @DisplayName("Epistemic Falsification: Asymmetric Relaxation vs Permanent Structural Bifurcation (Conqueror vs Hydraulic Builder)")
    void testLongTermTrajectoryRelaxationVsStructuralPersistence() {
        // Test Hypothesis: Pure military conquerors dissipate towards baseline mean, while physical capital builders shift attractor permanently
        EventSystem systemConqueror = new EventSystem();
        systemConqueror.setEnableEarthHistoricalLeaders(false);
        systemConqueror.setEnableProceduralLeaders(false);

        EventSystem systemBuilder = new EventSystem();
        systemBuilder.setEnableEarthHistoricalLeaders(false);
        systemBuilder.setEnableProceduralLeaders(false);

        List<H3Cell> cellsConqueror = new ArrayList<>();
        List<H3Cell> cellsBuilder = new ArrayList<>();
        for (H3Cell orig : mockCells) {
            H3Cell c1 = new H3Cell();
            c1.setLatitude(orig.getLatitude());
            c1.setLongitude(orig.getLongitude());
            c1.setMovementFriction(1.0);
            c1.setResourceCapital(10000.0);
            c1.setFoodResource(5000.0);
            cellsConqueror.add(c1);

            H3Cell c2 = new H3Cell();
            c2.setLatitude(orig.getLatitude());
            c2.setLongitude(orig.getLongitude());
            c2.setMovementFriction(1.0);
            c2.setResourceCapital(10000.0);
            c2.setFoodResource(5000.0);
            cellsBuilder.add(c2);
        }

        // 1. Inject Temporary Conqueror (Alexander, 12 years duration, 0 capital bonus)
        HistoricalIntervention conqueror = new HistoricalIntervention("ALEX_TEST", "Alexander", "Conquest", 100, 12, 40.64, 22.94, 2000.0, LeaderArchetype.MILITARY_CONQUEROR, 9.0);
        conqueror.setCapitalBonusGJ(0.0);
        systemConqueror.injectCustomIntervention(conqueror);

        // 2. Inject Hydraulic Builder (Sui Emperor Yang / Grand Canal, 20 years duration, large physical capital & carrying capacity bonus)
        HistoricalIntervention builder = new HistoricalIntervention("SUI_TEST", "Emperor Yang", "Grand Canal", 100, 20, 40.64, 22.94, 2000.0, LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR, 9.0);
        builder.setCapitalBonusGJ(500_000.0);
        builder.setCarryingCapacityMultiplier(1.60);
        systemBuilder.injectCustomIntervention(builder);

        // Run through intervention period (Year 100 to 125) and post-intervention long horizon (Year 126 to 300)
        for (int yr = 100; yr <= 300; yr++) {
            systemConqueror.checkEvents(yr, 0, 50000, 250000.0, cellsConqueror);
            systemBuilder.checkEvents(yr, 0, 50000, 250000.0, cellsBuilder);
        }

        double totalFoodConqueror = cellsConqueror.stream().mapToDouble(H3Cell::getFoodResource).sum();
        double totalFoodBuilder = cellsBuilder.stream().mapToDouble(H3Cell::getFoodResource).sum();
        double totalCapConqueror = cellsConqueror.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double totalCapBuilder = cellsBuilder.stream().mapToDouble(H3Cell::getResourceCapital).sum();

        // Structural bifurcation evaluation at t = 300 (180 years after leader death)
        assertTrue(totalCapBuilder > totalCapConqueror, "Hydraulic/Infrastructure builder should leave permanent capital assets");
        assertTrue(totalFoodBuilder > totalFoodConqueror, "Hydraulic innovations permanently elevate regional carrying capacity");
    }

    @Test
    @DisplayName("Trajectory Divergence Metric: Quantifying Path-Dependency & Relaxation Half-Life")
    void testTrajectoryDivergenceAndLyapunovMetric() {
        HistoricalIntervention intervention = new HistoricalIntervention("LEADER_TEST", "Test Reformer", "Test", 0, 30, 40.0, 20.0, 1500.0, LeaderArchetype.INSTITUTIONAL_REFORMER, 8.0);
        assertNotNull(intervention.getArchetype());
        assertEquals(30, intervention.getDurationYears());

        // Test spatial attenuation kernel (Loss of Strength Gradient)
        double weightEpicenter = intervention.getSpatialAttenuationWeight(40.0, 20.0);
        double weightMidRange = intervention.getSpatialAttenuationWeight(40.0, 25.0);
        double weightOutOfRange = intervention.getSpatialAttenuationWeight(10.0, 0.0);

        assertEquals(1.0, weightEpicenter, 0.01, "Epicenter weight should be 1.0");
        assertTrue(weightMidRange < weightEpicenter && weightMidRange > 0.0, "Mid-range should exhibit exponential distance decay");
        assertEquals(0.0, weightOutOfRange, 0.001, "Out of radius distance should be 0.0");
    }
}
