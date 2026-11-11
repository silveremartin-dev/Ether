/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.telemetry;

import com.uber.h3core.H3Core;
import org.ether.society.analytics.GroundTruthAuditGenerator;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.RegionalRasterBenchmarkExtractor;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.analytics.SnapshotTelemetryExtractor;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.network.codec.CellTopologyWireCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests for zero-default telemetry, DAG/AST compilation, tick subsampling,
 * offline snapshot extraction (1D scalars + 2D spatial queries), and regional benchmark hygiene.
 */
class TelemetryAstAndSnapshotTest {

    private static List<H3Cell> buildSampleWorld() throws Exception {
        H3Core h3 = H3Core.newInstance();
        H3Cell paris = new H3Cell();
        paris.setH3Index(h3.latLngToCell(48.8566, 2.3522, 4));
        paris.setLatitude(48.8566);
        paris.setLongitude(2.3522);
        paris.setBiome(Biome.PLAINS);
        paris.setElevation(50.0);
        paris.setPopulation(250_000);
        paris.setBiomassHuman(250_000.0);
        paris.setBiomassAgriculture(15_000.0);
        paris.setBiomassNatural(8_000.0);
        paris.setTemperature(12.5);
        paris.setRainfall(750.0);
        paris.setFoodResource(500_000.0);
        paris.setWaterResource(200_000.0);
        paris.setResourceCapital(250_000_000.0);
        paris.setEnergyFire(5_000.0);

        H3Cell madrid = new H3Cell();
        madrid.setH3Index(h3.latLngToCell(40.4168, -3.7038, 4));
        madrid.setLatitude(40.4168);
        madrid.setLongitude(-3.7038);
        madrid.setBiome(Biome.PLAINS);
        madrid.setElevation(650.0);
        madrid.setPopulation(120_000);
        madrid.setBiomassHuman(120_000.0);
        madrid.setBiomassAgriculture(9_000.0);
        madrid.setBiomassNatural(6_000.0);
        madrid.setTemperature(15.0);
        madrid.setRainfall(450.0);
        madrid.setFoodResource(240_000.0);
        madrid.setWaterResource(100_000.0);
        madrid.setResourceCapital(100_000_000.0);
        madrid.setEnergyFire(2_500.0);

        H3Cell cusco = new H3Cell();
        cusco.setH3Index(h3.latLngToCell(-13.5319, -71.9675, 4));
        cusco.setLatitude(-13.5319);
        cusco.setLongitude(-71.9675);
        cusco.setBiome(Biome.HILLS);
        cusco.setElevation(3399.0);
        cusco.setPopulation(60_000);
        cusco.setBiomassHuman(60_000.0);
        cusco.setBiomassAgriculture(4_000.0);
        cusco.setBiomassNatural(12_000.0);
        cusco.setTemperature(11.0);
        cusco.setRainfall(700.0);
        cusco.setFoodResource(120_000.0);
        cusco.setWaterResource(80_000.0);
        cusco.setResourceCapital(30_000_000.0);
        cusco.setEnergyFire(1_000.0);

        return List.of(paris, madrid, cusco);
    }

    @Test
    @DisplayName("Default engine has zero telemetry enabled and zero runtime telemetry cost")
    void testZeroTelemetryByDefault() throws Exception {
        H3SimulationEngine engine = new H3SimulationEngine(ConfigurationLoader.loadDefault());
        try {
            assertFalse(engine.isHeadlessTelemetrySubscribed());
            assertTrue(engine.getTelemetrySubscription().isEmpty());
            assertTrue(engine.getCompiledTelemetryPlan().isNoOp());

            engine.setCells(buildSampleWorld());
            engine.stepForward(3);

            assertTrue(engine.getCachedTelemetryResult().isEmpty(),
                    "No telemetry calculation should happen when default subscription is NONE");
            assertEquals(-1L, engine.getLastTelemetryTick());
            assertTrue(engine.getHistoryManager().getHistory().getSnapshots().isEmpty(),
                    "Scalar timeseries snapshots must not record when unsubscribed");
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("AST compiler computes minimal bitmasks, enforces subsampling, and evaluates full 27 benchmarks in one pass")
    void testAstMinimalBitmaskAndSubsampling() throws Exception {
        TelemetrySubscription tinySub = TelemetrySubscription.ofMetrics(
                Set.of("population", "temperatureAnomaly"), 5);
        CompiledTelemetryPlan tinyPlan = TelemetryAstCompiler.compile(tinySub);

        assertFalse(tinyPlan.isNoOp());
        int mask = tinyPlan.getInputMask();
        assertEquals(0, mask & CompiledTelemetryPlan.MASK_STAT_GINI, "Gini kernel must NOT be scheduled for pop+temp");
        assertEquals(0, mask & CompiledTelemetryPlan.MASK_STAT_GDP, "GDP kernel must NOT be scheduled for pop+temp");
        assertEquals(0, mask & CompiledTelemetryPlan.MASK_AGE_PYRAMID, "Age pyramid must NOT be scheduled");
        assertEquals(0, mask & CompiledTelemetryPlan.MASK_STAT_DENSITY_BIN, "Histogram must NOT be scheduled");
        assertTrue(tinySub.shouldEvaluateAtTick(0));
        assertFalse(tinySub.shouldEvaluateAtTick(1));
        assertFalse(tinySub.shouldEvaluateAtTick(4));
        assertTrue(tinySub.shouldEvaluateAtTick(5));

        TelemetrySubscription fullSub = TelemetrySubscription.allCanonicalBenchmarks(1);
        CompiledTelemetryPlan fullPlan = TelemetryAstCompiler.compile(fullSub);
        assertEquals(TelemetrySubscription.CANONICAL_BENCHMARK_METRICS.size(), fullSub.getMetricIds().size());

        List<H3Cell> cells = buildSampleWorld();
        TelemetryEvaluationResult res = SnapshotTelemetryExtractor.extractFromCells(cells, fullPlan, 10L, 1500L, 400_000L, 280.0, 100.0);
        assertEquals(430_000.0, res.getMetric("population"), 1.0);
        for (String key : TelemetrySubscription.CANONICAL_BENCHMARK_METRICS) {
            Double val = res.getMetric(key);
            assertNotNull(val, "Expected canonical benchmark metric present: " + key);
            assertTrue(Double.isFinite(val), "Expected finite metric value for: " + key);
        }
    }

    @Test
    @DisplayName("SnapshotTelemetryExtractor extracts 1D metrics, 2D rendered raster, and windowed zonal statistics from saved snapshot")
    void testSnapshotTelemetryExtractor1DAnd2DMaps(@TempDir Path tempDir) throws Exception {
        List<H3Cell> cells = buildSampleWorld();
        Path snapFile = tempDir.resolve("snapshot_tick_0000000010.bin.gz");
        CellTopologyWireCodec.saveToFile(snapFile, cells);

        TelemetrySubscription sub = new TelemetrySubscription(
                Set.of("population", "grossWorldProduct", "gini"),
                List.of(
                        TelemetrySubscription.SpatialRasterQuery.renderedMap(
                                "global_pop_img", TelemetrySubscription.SpatialChannel.POPULATION_DENSITY, 128, 64),
                        TelemetrySubscription.SpatialRasterQuery.regionalZonal(
                                "europe_pop_box", TelemetrySubscription.SpatialChannel.POPULATION_DENSITY,
                                36.0, 55.0, -10.0, 15.0)
                ),
                1);

        TelemetryEvaluationResult res = SnapshotTelemetryExtractor.extractFromSnapshotFile(snapFile, sub, 10L, 1500L);
        assertEquals(430_000.0, res.getMetric("population"), 1.0);
        assertTrue(res.getMetric("grossWorldProduct") > 0.0);

        var globalRaster = res.getSpatialResults().get("global_pop_img");
        assertNotNull(globalRaster);
        assertEquals(3, globalRaster.cellCount());
        assertEquals(430_000.0, globalRaster.sum(), 1.0);
        assertNotNull(globalRaster.renderedMap());
        assertEquals(128, globalRaster.renderedMap().getWidth());
        assertEquals(64, globalRaster.renderedMap().getHeight());

        var europeZonal = res.getSpatialResults().get("europe_pop_box");
        assertNotNull(europeZonal);
        assertEquals(2, europeZonal.cellCount(), "Only Paris & Madrid fall in Europe window");
        assertEquals(370_000.0, europeZonal.sum(), 1.0);
        assertEquals(120_000.0, europeZonal.min(), 1.0);
        assertEquals(250_000.0, europeZonal.max(), 1.0);
    }

    @Test
    @DisplayName("Regional extractor integrates conserved window directly and GroundTruthAudit rejects calendar synthesis & ratio scaling")
    void testRegionalExtractorAndAuditHygiene() {
        float[][] counts = new float[16][32];
        // Populate Europe window cell (~45N, 0E) and East Asia cell (~35N, 115E)
        counts[4][16] = 2_500_000.0f;
        counts[5][26] = 8_000_000.0f;
        double europeSum = RegionalRasterBenchmarkExtractor.sumConservedRegion(counts, 35.0, 60.0, -10.0, 20.0);
        assertEquals(2_500_000.0, europeSum, 1.0);

        Map<String, Double> recorded = new LinkedHashMap<>();
        recorded.put("population", 2_500_000.0);
        SimulationRunRecord.MetricSnapshot snap = new SimulationRunRecord.MetricSnapshot(
                2_500_000L, 2.0, 1.2, 70.0, 25, recorded);

        // Must NOT synthesize milankovitchInsolation or zeroContainmentScore by calendar formula
        assertTrue(Double.isNaN(GroundTruthAuditGenerator.extractValue(snap, "milankovitchInsolation", 1500)));
        assertTrue(Double.isNaN(GroundTruthAuditGenerator.extractValue(snap, "zeroContainmentScore", 1500)));

        // Extensive non-rastered global metrics must be reported honestly as lacking 2D regional ground-truth
        Map<Integer, SimulationRunRecord.MetricSnapshot> ts = Map.of(1500, snap);
        double[] europeBounds = new double[]{35.0, 60.0, -10.0, 20.0};
        var gwpDrift = GroundTruthAuditGenerator.computeVariableDrift(
                "GWP", "grossWorldProduct", Map.of(1500, 250.0), ts,
                HistoricalValidationKernel.InterpolationMethod.PCHIP_MONOTONE_CUBIC, 1.0, europeBounds);
        assertTrue(gwpDrift.status.contains("Regional"), "Expected regional non-rastered status, got: " + gwpDrift.status);
        assertEquals(0, gwpDrift.sampleCount);
    }

    @Test
    @DisplayName("Cultural affinity matrix and 2D spatial maps require explicit subscription and evaluate with zero overhead when unsubscribed")
    void testCulturalMatrixAndSpatialSubscriptionContract() throws Exception {
        // 1. Unsubscribed contract: Cultural matrix and 2D spatial queries are null/empty
        TelemetrySubscription emptySub = TelemetrySubscription.NONE;
        assertFalse(emptySub.isCulturalMatrixSubscribed());
        assertFalse(emptySub.isSpatialSubscribed());
        assertTrue(emptySub.isEmpty());

        CompiledTelemetryPlan emptyPlan = TelemetryAstCompiler.compile(emptySub);
        assertTrue(emptyPlan.isNoOp());

        List<H3Cell> cells = buildSampleWorld();
        TelemetryEvaluationResult unsubResult = SnapshotTelemetryExtractor.extractFromCells(
                cells, emptyPlan, 1L, 0L, 430_000L, 280.0, 10.0);
        assertTrue(unsubResult.isEmpty());
        assertNull(unsubResult.getCulturalMatrixResult(), "Unsubscribed cultural matrix must be strictly null");
        assertTrue(unsubResult.getSpatialResults().isEmpty(), "Unsubscribed spatial results must be strictly empty");

        // 2. Explicit subscription contract: Subscribing cultural matrix with delta & 2D spatial query
        TelemetrySubscription subWithCulturalAnd2D = new TelemetrySubscription(
                Set.of("population"),
                List.of(TelemetrySubscription.SpatialRasterQuery.globalZonal("pop_density", TelemetrySubscription.SpatialChannel.POPULATION_DENSITY)),
                TelemetrySubscription.CulturalMatrixQuery.withDelta(-1000L),
                1
        );
        assertTrue(subWithCulturalAnd2D.isCulturalMatrixSubscribed());
        assertTrue(subWithCulturalAnd2D.isSpatialSubscribed());
        assertFalse(subWithCulturalAnd2D.isEmpty());

        CompiledTelemetryPlan plan = TelemetryAstCompiler.compile(subWithCulturalAnd2D);
        assertFalse(plan.isNoOp());

        TelemetryEvaluationResult subResult = SnapshotTelemetryExtractor.extractFromCells(
                cells, plan, 1L, 0L, 430_000L, 280.0, 10.0);
        assertFalse(subResult.isEmpty());

        // Verify 2D spatial query evaluated
        var spatialResult = subResult.getSpatialResults().get("pop_density");
        assertNotNull(spatialResult, "Subscribed 2D spatial result must be present");
        assertEquals(3, spatialResult.cellCount());
        assertEquals(430_000.0, spatialResult.sum(), 1.0);

        // Verify Cultural Matrix evaluated
        var culturalResult = subResult.getCulturalMatrixResult();
        assertNotNull(culturalResult, "Subscribed cultural matrix result must be present");
        assertEquals(0L, culturalResult.epoch());
        assertEquals("earth", culturalResult.planet());
        assertFalse(culturalResult.entities().isEmpty(), "Active cultural entities must be loaded for year 0");
        int n = culturalResult.entities().size();
        assertEquals(n, culturalResult.affinities().length);
        assertEquals(n, culturalResult.deltas().length);

        // Diagonal self-affinity must be 1.0 (or normalized high)
        for (int i = 0; i < n; i++) {
            assertEquals(1.0, culturalResult.affinities()[i][i], 0.01, "Self affinity on diagonal must be 1.0");
        }
        assertTrue(culturalResult.meanAffinity() >= 0.0 && culturalResult.meanAffinity() <= 1.0);
        assertTrue(culturalResult.polarizationIndex() >= 0.0);

        // 3. String-based promotion via AST compiler (convenience contract)
        TelemetrySubscription aliasSub = TelemetrySubscription.ofMetrics(
                Set.of("culturalMatrix", "spatialMap2D"), 1);
        CompiledTelemetryPlan aliasPlan = TelemetryAstCompiler.compile(aliasSub);
        assertTrue(aliasPlan.getSubscription().isCulturalMatrixSubscribed(),
                "String 'culturalMatrix' in metricIds must be promoted to cultural matrix subscription");
        assertTrue(aliasPlan.getSubscription().isSpatialSubscribed(),
                "String 'spatialMap2D' in metricIds must be promoted to spatial raster subscription");
    }
}
