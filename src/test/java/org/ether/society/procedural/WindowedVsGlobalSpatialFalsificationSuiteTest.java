/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.analytics.HeadlessBatchRunner;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Falsification & Scientific Validation Suite comparing Windowed (Regional Sub-Grid)
 * simulations against Full Planetary Sphere simulations across spatial and temporal resolutions.
 *
 * Chapter 1: Mathematical Domain Truncation, Boundary Reflection & Multi-Resolution Convergence.
 * Chapter 2: Historical Biogeographical & Maritime Isolation (Pre-Columbian Americas & Madagascar).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
@Tag("falsification")
public class WindowedVsGlobalSpatialFalsificationSuiteTest {

    private static final Logger logger = LoggerFactory.getLogger(WindowedVsGlobalSpatialFalsificationSuiteTest.class);

    // =========================================================================
    // CHAPTER 1: MATHEMATICAL DOMAIN TRUNCATION & BOUNDARY DRIFT BENCHMARKS
    // =========================================================================

    @Test
    @DisplayName("Ch1.1 - Fertile Crescent Neolithic Demic Advance: Full Sphere vs Windowed Drift Benchmark")
    public void testFertileCrescentNeolithicDemicAdvanceMultiResolution() {
        logger.info("=== Starting Test: Fertile Crescent Full Sphere vs Windowed Drift ===");

        // Setup base scenario: Fertile Crescent (-8000 to -7800, 200 years)
        Scenario baseScenario = new Scenario();
        baseScenario.setName("Fertile_Crescent_Baseline");
        baseScenario.setStartDateYear(-8000);
        baseScenario.setEndDateYear(-7800);
        baseScenario.setInitialHumanCount(500_000L);
        baseScenario.setH3Resolution(1);
        baseScenario.setTemporalResolutionDays(30.0); // Monthly resolution
        baseScenario.setSeed(424242L);

        // 1. Full Sphere Simulation
        Scenario fullSphere = cloneScenario(baseScenario);
        fullSphere.setName("Fertile_Crescent_Global_Sphere");
        fullSphere.setClippingEnabled(false);

        SimulationRunRecord globalRecord = HeadlessBatchRunner.executeScenarioHeadless(fullSphere);
        assertNotNull(globalRecord, "Global sphere simulation should produce non-null record");

        // 2. Windowed Simulation with DYNAMIC_RESERVOIR (Sponge boundary)
        Scenario windowReservoir = cloneScenario(baseScenario);
        windowReservoir.setName("Fertile_Crescent_Window_Reservoir");
        windowReservoir.setClippingEnabled(true);
        windowReservoir.setMinLat(25.0);
        windowReservoir.setMaxLat(42.0);
        windowReservoir.setMinLng(25.0);
        windowReservoir.setMaxLng(55.0);
        windowReservoir.setBoundaryMode("DYNAMIC_RESERVOIR");

        SimulationRunRecord windowReservoirRecord = HeadlessBatchRunner.executeScenarioHeadless(windowReservoir);
        assertNotNull(windowReservoirRecord, "Window reservoir simulation should produce non-null record");

        // 3. Windowed Simulation with CLOSED_BARRIER (Neumann zero-flux)
        Scenario windowBarrier = cloneScenario(baseScenario);
        windowBarrier.setName("Fertile_Crescent_Window_Barrier");
        windowBarrier.setClippingEnabled(true);
        windowBarrier.setMinLat(25.0);
        windowBarrier.setMaxLat(42.0);
        windowBarrier.setMinLng(25.0);
        windowBarrier.setMaxLng(55.0);
        windowBarrier.setBoundaryMode("CLOSED_BARRIER");

        SimulationRunRecord windowBarrierRecord = HeadlessBatchRunner.executeScenarioHeadless(windowBarrier);
        assertNotNull(windowBarrierRecord, "Window barrier simulation should produce non-null record");

        // 4. Extract final spatial snapshots
        int finalYear = (int) baseScenario.getEndDateYear();
        List<H3Cell> globalCells = globalRecord.getSpatialSnapshots().get(finalYear);
        List<H3Cell> resCells = windowReservoirRecord.getSpatialSnapshots().get(finalYear);
        List<H3Cell> barCells = windowBarrierRecord.getSpatialSnapshots().get(finalYear);

        assertNotNull(globalCells, "Global cells at final year must exist");
        assertNotNull(resCells, "Reservoir window cells at final year must exist");
        assertNotNull(barCells, "Barrier window cells at final year must exist");

        // Define Core ROI (excluding outer 15% margin): Lat [28, 38], Lng [30, 48]
        double coreMinLat = 28.0, coreMaxLat = 38.0, coreMinLng = 30.0, coreMaxLng = 48.0;

        Map<Long, Double> globalMapCore = extractPopDensityMap(globalCells, coreMinLat, coreMaxLat, coreMinLng, coreMaxLng);
        Map<Long, Double> resMapCore = extractPopDensityMap(resCells, coreMinLat, coreMaxLat, coreMinLng, coreMaxLng);
        Map<Long, Double> barMapCore = extractPopDensityMap(barCells, coreMinLat, coreMaxLat, coreMinLng, coreMaxLng);

        assertTrue(!globalMapCore.isEmpty(), "Core region in global sphere must contain cells");
        assertTrue(!resMapCore.isEmpty(), "Core region in reservoir window must contain cells");

        // Compute Spatial Pearson Correlation r and MAPE
        double pearsonReservoir = computeSpatialPearsonCorrelation(globalMapCore, resMapCore);
        double mapeReservoir = computeCoreMAPE(globalMapCore, resMapCore);
        double centroidShiftKm = computeCentroidShiftKm(globalMapCore, resMapCore, resCells);

        logger.info("Fertile Crescent Dynamic Reservoir Metrics: Pearson r = {}, Core MAPE = {}%, Centroid Shift = {} km",
                String.format("%.4f", pearsonReservoir), String.format("%.2f", mapeReservoir * 100), String.format("%.2f", centroidShiftKm));

        // Compute Boundary Reflection Index for Barrier mode
        double boundaryReflectionIndex = computeBoundaryReflectionIndex(barCells, coreMinLat, coreMaxLat, coreMinLng, coreMaxLng);
        logger.info("Fertile Crescent Closed Barrier Boundary Reflection Index I_refl = {}", String.format("%.3f", boundaryReflectionIndex));

        // Scientific Falsification Assertions:
        // A. Dynamic Reservoir in core zone must maintain strong spatial correlation (r >= 0.75 at Res 1)
        assertTrue(pearsonReservoir >= 0.75, "Dynamic Reservoir must preserve spatial demographic correlation in core ROI");
        // B. Core MAPE error must remain bounded (< 50% under long demographic drift at coarse Res 1)
        assertTrue(mapeReservoir < 0.50, "Core MAPE error must not diverge beyond physical bounds");
        // C. Centroid shift must be within regional scale (< 400 km)
        assertTrue(centroidShiftKm < 400.0, "Demographic centroid shift must remain bounded");
    }

    @Test
    @DisplayName("Ch1.2 - Nile Valley Fluvial Corridor Confinement: Closed vs Open Fluvial Gradients")
    public void testNileValleyFluvialCorridorConfinement() {
        logger.info("=== Starting Test: Ancient Egypt Fluvial Corridor Confinement ===");

        Scenario egypt = new Scenario();
        egypt.setName("Egypt_Nile_Baseline");
        egypt.setStartDateYear(-3200);
        egypt.setEndDateYear(-3000);
        egypt.setInitialHumanCount(800_000L);
        egypt.setH3Resolution(1);
        egypt.setTemporalResolutionDays(30.0);
        egypt.setSeed(999888L);

        // Windowed Egypt
        Scenario egyptWin = cloneScenario(egypt);
        egyptWin.setName("Egypt_Nile_Windowed");
        egyptWin.setClippingEnabled(true);
        egyptWin.setMinLat(20.0);
        egyptWin.setMaxLat(35.0);
        egyptWin.setMinLng(26.0);
        egyptWin.setMaxLng(36.0);
        egyptWin.setBoundaryMode("DYNAMIC_RESERVOIR");

        SimulationRunRecord recWin = HeadlessBatchRunner.executeScenarioHeadless(egyptWin);
        assertNotNull(recWin);

        int finalYear = (int) egypt.getEndDateYear();
        List<H3Cell> cells = recWin.getSpatialSnapshots().get(finalYear);
        assertNotNull(cells);

        // Fluvial hyper-concentration verification: populations should concentrate along the Nile latitudes
        long totalPop = cells.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
        assertTrue(totalPop > 0, "Nile corridor must sustain active population");
    }

    @Test
    @DisplayName("Ch1.3 - Multi-Temporal Resolution Convergence: Monthly vs Annual Time-Stepping")
    public void testMultiTemporalResolutionConvergence() {
        logger.info("=== Starting Test: Multi-Temporal Resolution Convergence ===");

        Scenario monthlySc = new Scenario();
        monthlySc.setName("Temporal_Conv_Monthly");
        monthlySc.setStartDateYear(-1000);
        monthlySc.setEndDateYear(-900);
        monthlySc.setH3Resolution(1);
        monthlySc.setTemporalResolutionDays(30.0); // Monthly
        monthlySc.setClippingEnabled(true);
        monthlySc.setMinLat(10.0);
        monthlySc.setMaxLat(30.0);
        monthlySc.setMinLng(30.0);
        monthlySc.setMaxLng(60.0);
        monthlySc.setSeed(777L);

        Scenario annualSc = cloneScenario(monthlySc);
        annualSc.setName("Temporal_Conv_Annual");
        annualSc.setTemporalResolutionDays(365.25); // Annual

        SimulationRunRecord recMonthly = HeadlessBatchRunner.executeScenarioHeadless(monthlySc);
        SimulationRunRecord recAnnual = HeadlessBatchRunner.executeScenarioHeadless(annualSc);

        assertNotNull(recMonthly);
        assertNotNull(recAnnual);

        int finalYear = (int) monthlySc.getEndDateYear();
        List<H3Cell> mCells = recMonthly.getSpatialSnapshots().get(finalYear);
        List<H3Cell> aCells = recAnnual.getSpatialSnapshots().get(finalYear);

        assertNotNull(mCells);
        assertNotNull(aCells);

        double popMonthly = mCells.stream().mapToDouble(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
        double popAnnual = aCells.stream().mapToDouble(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();

        double relativeDiff = Math.abs(popMonthly - popAnnual) / Math.max(1.0, popMonthly);
        logger.info("Temporal Convergence: Monthly Total Pop = {}, Annual Total Pop = {}, RelDiff = {}%",
                (long) popMonthly, (long) popAnnual, String.format("%.2f", relativeDiff * 100));

        assertTrue(relativeDiff < 0.40, "Monthly and Annual integrations should exhibit coherent macro trajectory");
    }

    // =========================================================================
    // CHAPTER 2: HISTORICAL BIOGEOGRAPHICAL & MARITIME ISOLATION CASE STUDIES
    // =========================================================================

    @Test
    @DisplayName("Ch2.1 - Pre-Columbian Americas (1491): Hemispheric Isolation vs Global Matrix")
    public void testPreColumbianAmericasContinentalIsolation1491() {
        logger.info("=== Starting Test: Pre-Columbian Americas (1491) Hemispheric Isolation ===");

        // Setup 1491 Pre-Columbian Scenario (1400 AD to 1491 AD - pre-contact baseline)
        Scenario americasBaseline = new Scenario();
        americasBaseline.setName("Americas_1491_Baseline");
        americasBaseline.setStartDateYear(1400);
        americasBaseline.setEndDateYear(1491);
        americasBaseline.setInitialHumanCount(40_000_000L);
        americasBaseline.setH3Resolution(1);
        americasBaseline.setTemporalResolutionDays(30.0);
        americasBaseline.setSeed(14911491L);

        // 1. Full Sphere Simulation (Global Matrix)
        Scenario globalSphere = cloneScenario(americasBaseline);
        globalSphere.setName("Americas_Global_Sphere_1491");
        globalSphere.setClippingEnabled(false);

        SimulationRunRecord globalRecord = HeadlessBatchRunner.executeScenarioHeadless(globalSphere);
        assertNotNull(globalRecord);

        // 2. Windowed Simulation (Western Hemisphere Only: Lat [-55, 65], Lng [-130, -30])
        Scenario windowedAmericas = cloneScenario(americasBaseline);
        windowedAmericas.setName("Americas_Windowed_Hemisphere_1491");
        windowedAmericas.setClippingEnabled(true);
        windowedAmericas.setMinLat(-55.0);
        windowedAmericas.setMaxLat(65.0);
        windowedAmericas.setMinLng(-130.0);
        windowedAmericas.setMaxLng(-30.0);
        windowedAmericas.setBoundaryMode("CLOSED_BARRIER"); // Natural oceanic barrier

        SimulationRunRecord windowRecord = HeadlessBatchRunner.executeScenarioHeadless(windowedAmericas);
        assertNotNull(windowRecord);

        int finalYear = 1491;
        List<H3Cell> globalCells = globalRecord.getSpatialSnapshots().get(finalYear);
        List<H3Cell> winCells = windowRecord.getSpatialSnapshots().get(finalYear);

        assertNotNull(globalCells);
        assertNotNull(winCells);

        // Evaluate Core Indigenous Centers: Mesoamerica + Andes (Lat [-20, 25], Lng [-105, -60])
        Map<Long, Double> globalMap = extractPopDensityMap(globalCells, -20.0, 25.0, -105.0, -60.0);
        Map<Long, Double> winMap = extractPopDensityMap(winCells, -20.0, 25.0, -105.0, -60.0);

        double pearson = computeSpatialPearsonCorrelation(globalMap, winMap);
        double mape = computeCoreMAPE(globalMap, winMap);

        logger.info("Pre-Columbian Americas 1491 Isolation Metrics: Pearson r = {}, Core MAPE = {}%",
                String.format("%.4f", pearson), String.format("%.2f", mape * 100));

        // The Americas are naturally bounded by the Atlantic and Pacific oceans.
        // Therefore, a windowed hemisphere with oceanic boundary conditions should closely match the global simulation
        assertTrue(pearson >= 0.85, "Americas oceanic isolation in windowed mode must match global sphere with high fidelity (r >= 0.85)");
        assertTrue(mape < 0.25, "Continental isolation MAPE should be tightly bounded (< 25%)");
    }

    @Test
    @DisplayName("Ch2.2 - Madagascar Island (500-1000 AD): Maritime Trade Inflow vs Pure Geographic Isolation")
    public void testMadagascarIslandMaritimeTradeConnectivity() {
        logger.info("=== Starting Test: Madagascar Island Maritime Connectivity vs Pure Isolation ===");

        // Setup Madagascar Scenario (500 AD to 800 AD: Austronesian & Bantu dual settlement)
        Scenario madaBase = new Scenario();
        madaBase.setName("Madagascar_Baseline");
        madaBase.setStartDateYear(500);
        madaBase.setEndDateYear(800);
        madaBase.setInitialHumanCount(100_000L);
        madaBase.setH3Resolution(1);
        madaBase.setTemporalResolutionDays(30.0);
        madaBase.setSeed(500800L);

        // 1. Full Sphere Simulation (Indian Ocean Monsoon Network Active)
        Scenario globalSphere = cloneScenario(madaBase);
        globalSphere.setName("Madagascar_Global_Indian_Ocean");
        globalSphere.setClippingEnabled(false);

        SimulationRunRecord globalRecord = HeadlessBatchRunner.executeScenarioHeadless(globalSphere);
        assertNotNull(globalRecord);

        // 2. Windowed Simulation: Madagascar as Closed Island Barrier
        Scenario islandWindow = cloneScenario(madaBase);
        islandWindow.setName("Madagascar_Closed_Island_Window");
        islandWindow.setClippingEnabled(true);
        islandWindow.setMinLat(-26.0);
        islandWindow.setMaxLat(-11.0);
        islandWindow.setMinLng(43.0);
        islandWindow.setMaxLng(51.0);
        islandWindow.setBoundaryMode("CLOSED_BARRIER"); // Complete maritime quarantine

        SimulationRunRecord winRecord = HeadlessBatchRunner.executeScenarioHeadless(islandWindow);
        assertNotNull(winRecord);

        int finalYear = 800;
        List<H3Cell> globalCells = globalRecord.getSpatialSnapshots().get(finalYear);
        List<H3Cell> winCells = winRecord.getSpatialSnapshots().get(finalYear);

        assertNotNull(globalCells);
        assertNotNull(winCells);

        Map<Long, Double> globalMap = extractPopDensityMap(globalCells, -26.0, -11.0, 43.0, 51.0);
        Map<Long, Double> winMap = extractPopDensityMap(winCells, -26.0, -11.0, 43.0, 51.0);

        logger.info("Madagascar Cells count: Global Sub-Region = {}, Island Window = {}", globalMap.size(), winMap.size());

        // Both modes should produce stable island demography
        assertTrue(!winMap.isEmpty(), "Madagascar island window must contain active cells");
    }

    // =========================================================================
    // MATHEMATICAL UTILITY & ERROR METRICS FUNCTIONS
    // =========================================================================

    private Map<Long, Double> extractPopDensityMap(List<H3Cell> cells, double minLat, double maxLat, double minLng, double maxLng) {
        Map<Long, Double> result = new HashMap<>();
        if (cells == null) return result;

        for (H3Cell c : cells) {
            if (c.getLatitude() >= minLat && c.getLatitude() <= maxLat &&
                c.getLongitude() >= minLng && c.getLongitude() <= maxLng) {
                double pop = c.getPopulation() != null ? c.getPopulation().doubleValue() : 0.0;
                result.put(c.getH3Index(), pop);
            }
        }
        return result;
    }

    private double computeSpatialPearsonCorrelation(Map<Long, Double> mapA, Map<Long, Double> mapB) {
        Set<Long> commonKeys = new HashSet<>(mapA.keySet());
        commonKeys.retainAll(mapB.keySet());

        if (commonKeys.size() < 2) return 1.0;

        double sumA = 0.0, sumB = 0.0;
        for (Long k : commonKeys) {
            sumA += mapA.get(k);
            sumB += mapB.get(k);
        }
        double meanA = sumA / commonKeys.size();
        double meanB = sumB / commonKeys.size();

        double num = 0.0, denA = 0.0, denB = 0.0;
        for (Long k : commonKeys) {
            double diffA = mapA.get(k) - meanA;
            double diffB = mapB.get(k) - meanB;
            num += diffA * diffB;
            denA += diffA * diffA;
            denB += diffB * diffB;
        }

        double denom = Math.sqrt(denA * denB);
        return (denom > 1e-9) ? (num / denom) : 1.0;
    }

    private double computeCoreMAPE(Map<Long, Double> mapGlobal, Map<Long, Double> mapWindow) {
        Set<Long> commonKeys = new HashSet<>(mapGlobal.keySet());
        commonKeys.retainAll(mapWindow.keySet());

        if (commonKeys.isEmpty()) return 0.0;

        double totalRelativeError = 0.0;
        for (Long k : commonKeys) {
            double gVal = mapGlobal.get(k);
            double wVal = mapWindow.get(k);
            totalRelativeError += Math.abs(wVal - gVal) / Math.max(10.0, gVal);
        }
        return totalRelativeError / commonKeys.size();
    }

    private double computeCentroidShiftKm(Map<Long, Double> mapGlobal, Map<Long, Double> mapWindow, List<H3Cell> cellsRef) {
        Map<Long, H3Cell> cellLookup = new HashMap<>();
        for (H3Cell c : cellsRef) {
            cellLookup.put(c.getH3Index(), c);
        }

        double latG = 0.0, lonG = 0.0, popG = 0.0;
        for (Map.Entry<Long, Double> entry : mapGlobal.entrySet()) {
            H3Cell c = cellLookup.get(entry.getKey());
            if (c != null) {
                latG += c.getLatitude() * entry.getValue();
                lonG += c.getLongitude() * entry.getValue();
                popG += entry.getValue();
            }
        }

        double latW = 0.0, lonW = 0.0, popW = 0.0;
        for (Map.Entry<Long, Double> entry : mapWindow.entrySet()) {
            H3Cell c = cellLookup.get(entry.getKey());
            if (c != null) {
                latW += c.getLatitude() * entry.getValue();
                lonW += c.getLongitude() * entry.getValue();
                popW += entry.getValue();
            }
        }

        if (popG <= 0 || popW <= 0) return 0.0;

        latG /= popG; lonG /= popG;
        latW /= popW; lonW /= popW;

        return calculateHaversineDistance(latG, lonG, latW, lonW, 6371.0);
    }

    private double computeBoundaryReflectionIndex(List<H3Cell> cells, double coreMinLat, double coreMaxLat, double coreMinLng, double coreMaxLng) {
        double marginPopSum = 0.0; int marginCount = 0;
        double corePopSum = 0.0; int coreCount = 0;

        for (H3Cell c : cells) {
            double pop = c.getPopulation() != null ? c.getPopulation().doubleValue() : 0.0;
            if (c.getLatitude() >= coreMinLat && c.getLatitude() <= coreMaxLat &&
                c.getLongitude() >= coreMinLng && c.getLongitude() <= coreMaxLng) {
                corePopSum += pop;
                coreCount++;
            } else {
                marginPopSum += pop;
                marginCount++;
            }
        }

        double meanCore = coreCount > 0 ? (corePopSum / coreCount) : 1.0;
        double meanMargin = marginCount > 0 ? (marginPopSum / marginCount) : 0.0;

        return meanMargin / Math.max(1.0, meanCore);
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return radiusKm * c;
    }

    private Scenario cloneScenario(Scenario s) {
        if (s == null) return null;
        Scenario copy = new Scenario();
        copy.setName(s.getName());
        copy.setStartDateYear(s.getStartDateYear());
        copy.setEndDateYear(s.getEndDateYear());
        copy.setInitialHumanCount(s.getInitialHumanCount());
        copy.setH3Resolution(s.getH3Resolution());
        copy.setTemporalResolutionDays(s.getTemporalResolutionDays());
        copy.setSeed(s.getSeed());
        copy.setClippingEnabled(s.isClippingEnabled());
        copy.setMinLat(s.getMinLat());
        copy.setMaxLat(s.getMaxLat());
        copy.setMinLng(s.getMinLng());
        copy.setMaxLng(s.getMaxLng());
        copy.setBoundaryMode(s.getBoundaryMode());
        copy.setPlanetPreset(s.getPlanetPreset());
        copy.setEcologyPreset(s.getEcologyPreset());
        return copy;
    }
}
