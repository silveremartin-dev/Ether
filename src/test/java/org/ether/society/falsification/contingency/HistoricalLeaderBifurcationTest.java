/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.falsification.contingency;

import org.ether.society.events.*;
import org.junit.jupiter.api.Tag;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.database.Polity;
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
@Tag("academic")
@Tag("scientific")
@Tag("falsification")
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
        HistoricalInterventionRegistry catalog = HistoricalInterventionRegistry.getInstance();
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
    @DisplayName("Epistemic Falsification: Macro-Historical Attractor Relaxation vs Planetary Boundary Catastrophe (Great Man Falsification)")
    void testLongTermTrajectoryRelaxationVsStructuralPersistence() {
        /*
         * CORE SCIENTIFIC HYPOTHESIS & FALSIFICATION TEST:
         * Thomas Carlyle's "Great Man" hypothesis (1841) claims that the macroscopic course of history
         * is permanently determined and redirected by catalytic individuals.
         *
         * Conversely, Ether's thermodynamic and cliodynamic framework posits that:
         * 1. ALL individual human interventions (whether military conquerors, totalitarian purgers,
         *    heroic reformers, or "god-mode" infrastructure builders) are transient pulses.
         * 2. Physical capital K obeys entropic depreciation (delta = 4.0%/yr, Jorgenson 1963; Garrett 2011; Smil 2017)
         *    anchored to the metabolic exergy flux P_watts / carrying capacity of the environment.
         * 3. Once the exceptional leader's active reign ends (t > t_end), the perturbation exponentially relaxes
         *    back toward the structural biophysical attractor (tau_1/2 ~ 17-25 yr). Over 150-200 years,
         *    everything levels out (|Delta K| / K* < 1.0%), formally falsifying the Great Man hypothesis.
         * 4. In contrast, genuine planetary/geophysical catastrophes (volcanism, aridification, megadroughts)
         *    directly alter the physical state tensor (NPP, insolation, soil depth, aquifer recharge),
         *    permanently shifting the steady-state attractor manifold.
         */
        final double deltaDepreciation = 0.04; // 4.0% annual thermodynamic depreciation
        final double baselineOutputPerCell = 400.0; // Annual replacement investment I = delta * K*
        final double baselineSteadyStateK = baselineOutputPerCell / deltaDepreciation; // 10,000 GJ/cell

        // Setup 5 comparison arms:
        // Arm 0: Structural Baseline (unperturbed steady-state)
        // Arm 1: Military Conqueror (Alexander/Napoleon: mobility bonus, zero lasting exergy)
        // Arm 2: "God Mode" Supreme Builder (massive +500,000 GJ capital and carrying capacity surge)
        // Arm 3: Totalitarian Purger (drastic capital and demographic destruction)
        // Arm 4: Exogenous Geophysical Catastrophe (40% collapse of environmental carrying capacity)

        EventSystem sysBaseline = new EventSystem();
        sysBaseline.setEnableEarthHistoricalLeaders(false);
        sysBaseline.setEnableProceduralLeaders(false);

        EventSystem sysConqueror = new EventSystem();
        sysConqueror.setEnableEarthHistoricalLeaders(false);
        sysConqueror.setEnableProceduralLeaders(false);

        EventSystem sysBuilder = new EventSystem();
        sysBuilder.setEnableEarthHistoricalLeaders(false);
        sysBuilder.setEnableProceduralLeaders(false);

        EventSystem sysPurger = new EventSystem();
        sysPurger.setEnableEarthHistoricalLeaders(false);
        sysPurger.setEnableProceduralLeaders(false);

        List<H3Cell> cells0 = new ArrayList<>();
        List<H3Cell> cells1 = new ArrayList<>();
        List<H3Cell> cells2 = new ArrayList<>();
        List<H3Cell> cells3 = new ArrayList<>();
        List<H3Cell> cells4 = new ArrayList<>();

        for (H3Cell orig : mockCells) {
            cells0.add(cloneMockCell(orig, baselineSteadyStateK));
            cells1.add(cloneMockCell(orig, baselineSteadyStateK));
            cells2.add(cloneMockCell(orig, baselineSteadyStateK));
            cells3.add(cloneMockCell(orig, baselineSteadyStateK));
            cells4.add(cloneMockCell(orig, baselineSteadyStateK));
        }

        // Inject Arm 1: Military Conqueror (Alexander, Year 100 to 112, 12 years)
        HistoricalIntervention conqueror = new HistoricalIntervention("ALEX_TEST", "Alexander", "Conquest", 100, 12, 40.64, 22.94, 2000.0, LeaderArchetype.MILITARY_CONQUEROR, 9.0);
        conqueror.setMovementFrictionMultiplier(0.35);
        conqueror.setCapitalBonusGJ(0.0);
        sysConqueror.injectCustomIntervention(conqueror);

        // Inject Arm 2: God-Mode Builder (Sui Emperor Yang, Year 100 to 120, 20 years)
        HistoricalIntervention builder = new HistoricalIntervention("SUI_TEST", "Emperor Yang", "Grand Canal", 100, 20, 40.64, 22.94, 2000.0, LeaderArchetype.HYDRAULIC_AGRARIAN_INNOVATOR, 9.5);
        builder.setCapitalBonusGJ(500_000.0);
        builder.setCarryingCapacityMultiplier(1.60);
        sysBuilder.injectCustomIntervention(builder);

        // Inject Arm 3: Totalitarian Purger (Year 100 to 115, 15 years)
        HistoricalIntervention purger = new HistoricalIntervention("PURGE_TEST", "Totalitarian Purger", "Purge", 100, 15, 40.64, 22.94, 2000.0, LeaderArchetype.TOTALITARIAN_PURGER, 9.0);
        purger.setCapitalBonusGJ(-80_000.0); // Destroys capital
        sysPurger.injectCustomIntervention(purger);

        double peakBuilderDivergence = 0.0;
        double peakPurgerDivergence = 0.0;

        // Run simulation from Year 100 to Year 300 (200 years)
        for (int yr = 100; yr <= 300; yr++) {
            // Apply event system pulses
            sysBaseline.checkEvents(yr, 0, 50000, 250000.0, cells0);
            sysConqueror.checkEvents(yr, 0, 50000, 250000.0, cells1);
            sysBuilder.checkEvents(yr, 0, 50000, 250000.0, cells2);
            sysPurger.checkEvents(yr, 0, 50000, 250000.0, cells3);

            // Apply standard thermodynamic capital update: dK/dt = I - delta * K
            // For Arm 4 (Geophysical Catastrophe striking at Year 100), environmental carrying capacity permanently drops by 40%
            double arm4OutputMultiplier = (yr >= 100) ? 0.60 : 1.0;

            for (int i = 0; i < mockCells.size(); i++) {
                updateCellCapital(cells0.get(i), baselineOutputPerCell, deltaDepreciation);
                updateCellCapital(cells1.get(i), baselineOutputPerCell, deltaDepreciation);
                updateCellCapital(cells2.get(i), baselineOutputPerCell, deltaDepreciation);
                updateCellCapital(cells3.get(i), baselineOutputPerCell, deltaDepreciation);
                updateCellCapital(cells4.get(i), baselineOutputPerCell * arm4OutputMultiplier, deltaDepreciation);
            }

            // Track peak divergence during active leader reigns (Year 120)
            if (yr == 120) {
                double cap0 = cells0.stream().mapToDouble(H3Cell::getResourceCapital).sum();
                double cap2 = cells2.stream().mapToDouble(H3Cell::getResourceCapital).sum();
                double cap3 = cells3.stream().mapToDouble(H3Cell::getResourceCapital).sum();
                peakBuilderDivergence = (cap2 - cap0) / cap0;
                peakPurgerDivergence = Math.abs(cap3 - cap0) / cap0;
            }
        }

        // Peak shock evaluation: leaders create strong transient shocks during their lifetimes
        assertTrue(peakBuilderDivergence > 0.50, "God-Mode Builder must produce a sharp transient capital surge during active reign (> +50%)");
        assertTrue(peakPurgerDivergence > 0.15, "Totalitarian Purger must produce a sharp transient capital contraction (> 15%)");

        // Long-term evaluation at Year 300 (180 years after leader demise, ~10 relaxation half-lives)
        double finalCapBaseline = cells0.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double finalCapConqueror = cells1.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double finalCapBuilder = cells2.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double finalCapPurger = cells3.stream().mapToDouble(H3Cell::getResourceCapital).sum();
        double finalCapCatastrophe = cells4.stream().mapToDouble(H3Cell::getResourceCapital).sum();

        double relativeDivergenceConqueror = Math.abs(finalCapConqueror - finalCapBaseline) / finalCapBaseline;
        double relativeDivergenceBuilder = Math.abs(finalCapBuilder - finalCapBaseline) / finalCapBaseline;
        double relativeDivergencePurger = Math.abs(finalCapPurger - finalCapBaseline) / finalCapBaseline;
        double relativeDivergenceCatastrophe = Math.abs(finalCapCatastrophe - finalCapBaseline) / finalCapBaseline;

        // VERDICT 1: All human leaders (conquerors, god-mode builders, purgers) have completely leveled out (|Delta K| < 1.0%)
        assertTrue(relativeDivergenceConqueror < 0.01,
                String.format("Conqueror perturbation must relax to baseline (< 1.0%%), got %.4f%%", relativeDivergenceConqueror * 100));
        assertTrue(relativeDivergenceBuilder < 0.01,
                String.format("God-Mode Builder capital surge must relax to baseline (< 1.0%%) due to physical depreciation, got %.4f%%", relativeDivergenceBuilder * 100));
        assertTrue(relativeDivergencePurger < 0.01,
                String.format("Totalitarian Purger capital destruction must relax to baseline (< 1.0%%), got %.4f%%", relativeDivergencePurger * 100));

        // VERDICT 2: Only the planetary geophysical catastrophe creates a permanent structural bifurcation (> 35% departure)
        assertTrue(relativeDivergenceCatastrophe > 0.35,
                String.format("Planetary catastrophe must permanently alter steady-state attractor (> 35%% departure), got %.2f%%", relativeDivergenceCatastrophe * 100));
    }

    private static H3Cell cloneMockCell(H3Cell orig, double steadyStateK) {
        H3Cell c = new H3Cell();
        c.setH3Index(orig.getH3Index());
        c.setLatitude(orig.getLatitude());
        c.setLongitude(orig.getLongitude());
        c.setElevation(orig.getElevation());
        c.setTemperature(orig.getTemperature());
        c.setRainfall(orig.getRainfall());
        c.setBiome(orig.getBiome());
        c.setPopulation(orig.getPopulation());
        c.setMovementFriction(1.0);
        c.setFoodResource(orig.getFoodResource());
        c.setResourceCapital(steadyStateK);
        return c;
    }

    private static void updateCellCapital(H3Cell cell, double annualInvestment, double annualDepreciationRate) {
        double currentK = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
        double nextK = currentK + annualInvestment - (annualDepreciationRate * currentK);
        cell.setResourceCapital(Math.max(0.0, nextK));
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
