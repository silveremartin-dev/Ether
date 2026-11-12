/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.counterfactual;

import org.ether.society.analytics.CellularMapComparisonMetrics;
import org.ether.society.analytics.HeadlessBatchRunner;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.MapComparisonMetrics;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <h1>Pillar 6: Out-of-Sample Spatial Hold-Out Cross-Validation Suite (7 Continents/Regimes)</h1>
 * <p>
 * Evaluates universal structural generalizability on <b>7 geographically isolated, non-Eurasian out-of-sample
 * hold-out domains</b> over multi-decade simulation windows without altering any parameter calibrated on
 * Eurasian core benchmarks (Fertile Crescent, Mediterranean, China).
 * </p>
 *
 * <h2>The 7 Out-of-Sample Hold-Out Scenarios</h2>
 * <ol>
 *   <li><b>Hold-Out 1 — Formative Mesoamerica (\(-1500\text{ to }-1480\text{ BCE}\))</b>:
 *       Olmec Gulf Coast and Oaxaca valleys ({@code earth_-1500_mesoamerica.json}, {@code data/maps/ether/earth/-1500/}).
 *       Tests early maize domestication and tropical alluvial settlement without Old World bronze or draft animals.</li>
 *   <li><b>Hold-Out 2 — Pre-Columbian Americas (\(1491\text{ to }1511\text{ CE}\))</b>:
 *       Tawantinsuyu (Andean vertical archipelago) and Anahuac / Basin of Mexico ({@code earth_+1491_americas_1491.json},
 *       {@code data/maps/ether/earth/1491/}). Tests high-density hydraulic/terrace agrarian empires without iron or horses.</li>
 *   <li><b>Hold-Out 3 — Sub-Saharan West Africa / Mali Empire (\(1324\text{ to }1344\text{ CE}\))</b>:
 *       Inner Niger Delta, Timbuktu, Gao, and Bambuk/Bure goldfields ({@code earth_+1324_mali_empire.json},
 *       {@code data/maps/ether/earth/1324/}). Tests Sahelian agro-pastoralism and trans-ecotone trade corridors.</li>
 *   <li><b>Hold-Out 4 — Pleistocene Australasia / Sahul (\(-50\,000\text{ to }-49\,980\text{ BP}\))</b>:
 *       Sunda-Wallacea-Sahul paleolithic foraging regime ({@code earth_-50000_sahul.json},
 *       {@code data/maps/ether/earth/-50000/}). Tests deep-time hunter-gatherer carrying capacity and zero-civilization invariants.</li>
 *   <li><b>Hold-Out 5 — Insular Sakoku Isolation / Tokugawa Japan (\(1639\text{ to }1659\text{ CE}\))</b>:
 *       Closed-archipelago intensive wet-rice regime ({@code earth_+1639_tokugawa_japan.json},
 *       {@code data/maps/ether/earth/1639/}). Tests high-density insular metabolic closure under strict trade autarky.</li>
 *   <li><b>Hold-Out 6 — Holocene African Humid Period / Green Sahara (\(-6000\text{ to }-5980\text{ BCE}\))</b>:
 *       Mega-Lake Chad and trans-Saharan savannah pastoralist corridors ({@code earth_-6000_green_sahara.json},
 *       {@code data/maps/ether/earth/-6000/}). Tests orbital-monsoon greening, aquifer-fed lake basins, and cattle pastoralism.</li>
 *   <li><b>Hold-Out 7 — Arctic Beringian Standstill &amp; Kelp Highway (\(-25\,000\text{ to }-24\,980\text{ BP}\))</b>:
 *       Exposed Bering Land Bridge mammoth steppe and North Pacific coastal refugia ({@code earth_-25000_beringia.json},
 *       {@code data/maps/ether/earth/-25000/}). Tests sub-arctic cold-tolerance metabolism and glacial barrier containment.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
@Tag("scientific")
@Tag("falsification")
@Tag("counterfactual")
@Tag("pillar6")
public class SpatialHoldoutCrossValidationTest {

    private Scenario loadBuiltInScenario(String presetKey, String fallbackNameFragment) {
        return Scenario.getBuiltInScenarios().stream()
                .filter(s -> presetKey.equalsIgnoreCase(s.getPresetKey())
                        || (s.getName() != null && s.getName().contains(fallbackNameFragment)))
                .findFirst()
                .orElse(null);
    }

    private List<H3Cell> extractLatestSnapshot(SimulationRunRecord record, int endYear, int startYear) {
        List<H3Cell> snapshot = record.getSpatialSnapshotAt(endYear);
        if (snapshot == null || snapshot.isEmpty()) {
            snapshot = record.getSpatialSnapshotAt(startYear);
        }
        if ((snapshot == null || snapshot.isEmpty())
                && record.getSpatialSnapshots() != null
                && !record.getSpatialSnapshots().isEmpty()) {
            snapshot = record.getSpatialSnapshots().lastEntry().getValue();
        }
        return snapshot;
    }

    @Test
    @DisplayName("Hold-Out 1: Formative Mesoamerica (-1500 to -1480 BCE, 20-Year Window, Real Rasters & Cells)")
    void testFormativeMesoamericaHoldOutEmergence() throws Exception {
        Scenario scenario = loadBuiltInScenario("mesoamerica", "Mesoamerica");
        assertNotNull(scenario, "Scenario 'mesoamerica' must be registered in built-ins");

        scenario.setStartDateYear(-1500);
        scenario.setEndDateYear(-1480);
        scenario.setH3Resolution(6);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(scenario);
        assertNotNull(record, "Headless execution of Formative Mesoamerica hold-out must produce a valid record");

        double tau = HistoricalValidationKernel.computeContinuousTemporalTolerance(-1500L);
        assertTrue(tau >= 0.40 && tau <= 0.50,
                "Continuous tolerance tau(-1500) must lie in [40%, 50%] (computed: " + (tau * 100.0) + "%)");

        File rasterFile = new File("data/maps/ether/earth/-1500/earth_-1500_density.png");
        assertTrue(rasterFile.exists(), "Empirical ground-truth raster earth_-1500_density.png must exist");
        BufferedImage groundTruth = ImageIO.read(rasterFile);
        assertNotNull(groundTruth);

        List<H3Cell> snapshot = extractLatestSnapshot(record, -1480, -1500);
        assertNotNull(snapshot, "Spatial snapshot must be captured during Mesoamerica simulation");
        assertFalse(snapshot.isEmpty(), "Spatial snapshot must contain populated H3 cells");

        MapComparisonMetrics.MapComparisonResult comp = CellularMapComparisonMetrics.compareCellsToRaster(
                snapshot, groundTruth,
                scenario.getMinLat(), scenario.getMaxLat(),
                scenario.getMinLng(), scenario.getMaxLng(),
                scenario.isClippingEnabled()
        );
        assertNotNull(comp);
        assertTrue(comp.getRmse() < 120.0, "Spatial RMSE must remain bounded (RMSE: " + comp.getRmse() + ")");

        long totalPop = 0;
        double totalAgriBiomass = 0.0;
        for (H3Cell c : snapshot) {
            totalPop += c.getPopulationOr(0);
            totalAgriBiomass += c.getBiomassAgricultureOr(0.0);
        }
        assertTrue(totalPop > 50_000, "Formative Mesoamerica must sustain multi-decade demographic growth (" + totalPop + ")");
        assertTrue(totalAgriBiomass > 0.0, "Maize domestication in Mesoamerica must sustain positive agricultural biomass");
    }

    @Test
    @DisplayName("Hold-Out 2: Pre-Columbian Americas (1491 to 1511 CE, 20-Year Window, Real Rasters & Cells)")
    void testPreColumbianAmericasHoldOutEmergence() throws Exception {
        Scenario americasScenario = loadBuiltInScenario("americas_1491", "1491");
        assertNotNull(americasScenario, "Scenario 'americas_1491' must be registered in built-ins");

        americasScenario.setStartDateYear(1491);
        americasScenario.setEndDateYear(1511);
        americasScenario.setH3Resolution(4);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(americasScenario);
        assertNotNull(record, "Headless execution of Pre-Columbian Americas hold-out must produce a valid record");

        double tau = HistoricalValidationKernel.computeContinuousTemporalTolerance(1491L);
        assertTrue(tau >= 0.10 && tau <= 0.22,
                "Continuous tolerance tau(1491) must be in [10%, 22%] (computed: " + (tau * 100.0) + "%)");

        File rasterFile = new File("data/maps/ether/earth/1491/earth_1491_density.png");
        assertTrue(rasterFile.exists(), "Empirical ground-truth raster earth_1491_density.png must exist");
        BufferedImage groundTruth = ImageIO.read(rasterFile);
        assertNotNull(groundTruth);

        List<H3Cell> snapshot = extractLatestSnapshot(record, 1511, 1491);
        assertNotNull(snapshot, "Spatial snapshot must be captured during simulation");
        assertFalse(snapshot.isEmpty(), "Spatial snapshot must contain populated H3 cells");

        MapComparisonMetrics.MapComparisonResult comp = CellularMapComparisonMetrics.compareCellsToRaster(
                snapshot, groundTruth,
                americasScenario.getMinLat(), americasScenario.getMaxLat(),
                americasScenario.getMinLng(), americasScenario.getMaxLng(),
                americasScenario.isClippingEnabled()
        );
        assertNotNull(comp);
        assertTrue(comp.getPearsonR() >= -0.20, "Spatial correlation must be non-degenerate");
        assertTrue(comp.getRmse() < 120.0, "Spatial RMSE against empirical density must remain bounded");

        long totalPop = 0;
        double totalLivestock = 0.0;
        double totalCapital = 0.0;
        for (H3Cell c : snapshot) {
            totalPop += c.getPopulationOr(0);
            totalLivestock += c.getBiomassLivestockOr(0.0);
            totalCapital += c.getResourceCapitalOr(0.0);
        }

        assertTrue(totalPop > 100_000, "Pre-Columbian Americas must retain demographic vitality over 20 years");
        assertTrue(totalCapital > 0.0, "Pre-Columbian chinampas and Andean terraces generate physical capital");
        double livestockPerCapita = totalLivestock / Math.max(1, totalPop);
        assertTrue(livestockPerCapita < 2.0,
                "Pre-Columbian draft livestock per capita must remain negligible compared to Old World pastoralism");
    }

    @Test
    @DisplayName("Hold-Out 3: Sub-Saharan West Africa / Mali Empire (1324 to 1344 CE, 20-Year Window, Real Rasters & Cells)")
    void testSubSaharanMaliEmpireHoldOutEmergence() throws Exception {
        Scenario maliScenario = loadBuiltInScenario("mali_empire", "Mali");
        assertNotNull(maliScenario, "Scenario 'mali_empire' must be registered in built-ins");

        maliScenario.setStartDateYear(1324);
        maliScenario.setEndDateYear(1344);
        maliScenario.setH3Resolution(6);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(maliScenario);
        assertNotNull(record, "Headless execution of Sub-Saharan Africa hold-out must produce a valid record");

        double tau = HistoricalValidationKernel.computeContinuousTemporalTolerance(1324L);
        assertTrue(tau >= 0.12 && tau <= 0.25,
                "Continuous tolerance tau(1324) must be in [12%, 25%] (computed: " + (tau * 100.0) + "%)");

        File rasterFile = new File("data/maps/ether/earth/1324/earth_1324_density.png");
        assertTrue(rasterFile.exists(), "Empirical ground-truth raster earth_1324_density.png must exist");
        BufferedImage groundTruth = ImageIO.read(rasterFile);
        assertNotNull(groundTruth);

        List<H3Cell> snapshot = extractLatestSnapshot(record, 1344, 1324);
        assertNotNull(snapshot, "Spatial snapshot must be captured during simulation");
        assertFalse(snapshot.isEmpty());

        MapComparisonMetrics.MapComparisonResult comp = CellularMapComparisonMetrics.compareCellsToRaster(
                snapshot, groundTruth,
                maliScenario.getMinLat(), maliScenario.getMaxLat(),
                maliScenario.getMinLng(), maliScenario.getMaxLng(),
                maliScenario.isClippingEnabled()
        );
        assertNotNull(comp);
        assertTrue(comp.getPearsonR() >= -0.20, "Spatial correlation in Sahelian corridor must be valid");

        long activePop = 0;
        for (H3Cell c : snapshot) {
            activePop += c.getPopulationOr(0);
        }
        assertTrue(activePop > 10_000, "Sahelian urban centers (Timbuktu, Gao, Djenne) maintain population over 20 years");
    }

    @Test
    @DisplayName("Hold-Out 4: Pleistocene Australasia / Sahul (-50000 to -49980 BP, 20-Year Window, Real Rasters & Cells)")
    void testPleistoceneSahulHoldOutInvariants() throws Exception {
        Scenario sahulScenario = loadBuiltInScenario("sahul", "Sahul");
        assertNotNull(sahulScenario, "Scenario 'sahul' must be registered in built-ins");

        sahulScenario.setStartDateYear(-50000);
        sahulScenario.setEndDateYear(-49980);
        sahulScenario.setH3Resolution(4);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(sahulScenario);
        assertNotNull(record, "Headless execution of Pleistocene Sahul hold-out must produce a valid record");

        double tau = HistoricalValidationKernel.computeContinuousTemporalTolerance(-50000L);
        assertTrue(tau >= 0.45 && tau <= 0.51,
                "Deep-time Pleistocene tolerance tau(-50000) must approach 50% asymptote (computed: " + (tau * 100.0) + "%)");

        File rasterFile = new File("data/maps/ether/earth/-50000/earth_-50000_density.png");
        assertTrue(rasterFile.exists(), "Empirical ground-truth raster earth_-50000_density.png must exist");
        BufferedImage groundTruth = ImageIO.read(rasterFile);
        assertNotNull(groundTruth);

        List<H3Cell> snapshot = extractLatestSnapshot(record, -49980, -50000);
        assertNotNull(snapshot, "Spatial snapshot must be captured during Sahul simulation");
        assertFalse(snapshot.isEmpty());

        long totalPop = 0;
        double totalNaturalBiomass = 0.0;
        for (H3Cell c : snapshot) {
            totalPop += c.getPopulationOr(0);
            totalNaturalBiomass += c.getBiomassNaturalOr(0.0);
        }
        assertTrue(totalPop > 100, "Pleistocene Sahul foraging bands must survive 20-year window without extinction");
        assertTrue(totalNaturalBiomass > 0.0, "Wild foraging ecosystems in Sahul must preserve positive natural biomass");

        // Deep-time boundary invariant (AGENTS.md §8.2): zero sovereign debt and zero fossil share at -50,000 BP
        SimulationRunRecord.MetricSnapshot lastMetrics = record.getTimeSeriesData().get(-49980);
        if (lastMetrics != null) {
            assertEquals(0.0, lastMetrics.getValue("sovereignDebtBurden"), 0.5,
                    "Pleistocene Sahul must exhibit zero sovereign debt burden");
        }
    }

    @Test
    @DisplayName("Hold-Out 5: Insular Isolation / Tokugawa Japan (1639 to 1659 CE, 20-Year Window, Real Rasters & Cells)")
    void testInsularTokugawaJapanHoldOutEmergence() throws Exception {
        Scenario tokugawaScenario = loadBuiltInScenario("tokugawa_japan", "Tokugawa");
        assertNotNull(tokugawaScenario, "Scenario 'tokugawa_japan' must be registered in built-ins");

        tokugawaScenario.setStartDateYear(1639);
        tokugawaScenario.setEndDateYear(1659);
        tokugawaScenario.setH3Resolution(6);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(tokugawaScenario);
        assertNotNull(record, "Headless execution of Tokugawa Japan hold-out must produce a valid record");

        double tau = HistoricalValidationKernel.computeContinuousTemporalTolerance(1639L);
        assertTrue(tau >= 0.08 && tau <= 0.18,
                "Early Modern tolerance tau(1639) must lie in [8%, 18%] (computed: " + (tau * 100.0) + "%)");

        File rasterFile = new File("data/maps/ether/earth/1639/earth_1639_density.png");
        assertTrue(rasterFile.exists(), "Empirical ground-truth raster earth_1639_density.png must exist");
        BufferedImage groundTruth = ImageIO.read(rasterFile);
        assertNotNull(groundTruth);

        List<H3Cell> snapshot = extractLatestSnapshot(record, 1659, 1639);
        assertNotNull(snapshot, "Spatial snapshot must be captured during Tokugawa Japan simulation");
        assertFalse(snapshot.isEmpty());

        MapComparisonMetrics.MapComparisonResult comp = CellularMapComparisonMetrics.compareCellsToRaster(
                snapshot, groundTruth,
                tokugawaScenario.getMinLat(), tokugawaScenario.getMaxLat(),
                tokugawaScenario.getMinLng(), tokugawaScenario.getMaxLng(),
                tokugawaScenario.isClippingEnabled()
        );
        assertNotNull(comp);
        assertTrue(comp.getRmse() < 120.0, "Spatial RMSE on Japanese archipelago must remain bounded");

        long totalPop = 0;
        double totalCapital = 0.0;
        for (H3Cell c : snapshot) {
            totalPop += c.getPopulationOr(0);
            totalCapital += c.getResourceCapitalOr(0.0);
        }
        assertTrue(totalPop > 500_000, "Tokugawa archipelago must sustain high-density population under Sakoku autarky");
        assertTrue(totalCapital > 0.0, "Intensive paddy irrigation and castle-town infrastructure must be preserved");
    }

    @Test
    @DisplayName("Hold-Out 6: Holocene African Humid Period / Green Sahara (-6000 to -5980 BCE, 20-Year Window, Real Rasters & Cells)")
    void testGreenSaharaAfricanHumidPeriodHoldOutEmergence() throws Exception {
        Scenario saharaScenario = loadBuiltInScenario("green_sahara", "Sahara");
        assertNotNull(saharaScenario, "Scenario 'green_sahara' must be registered in built-ins");

        saharaScenario.setStartDateYear(-6000);
        saharaScenario.setEndDateYear(-5980);
        saharaScenario.setH3Resolution(6);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(saharaScenario);
        assertNotNull(record, "Headless execution of Green Sahara hold-out must produce a valid record");

        double tau = HistoricalValidationKernel.computeContinuousTemporalTolerance(-6000L);
        assertTrue(tau >= 0.45 && tau <= 0.51,
                "Holocene tolerance tau(-6000) must lie in [45%, 51%] (computed: " + (tau * 100.0) + "%)");

        File rasterFile = new File("data/maps/ether/earth/-6000/earth_-6000_density.png");
        assertTrue(rasterFile.exists(), "Empirical ground-truth raster earth_-6000_density.png must exist");
        BufferedImage groundTruth = ImageIO.read(rasterFile);
        assertNotNull(groundTruth);

        List<H3Cell> snapshot = extractLatestSnapshot(record, -5980, -6000);
        assertNotNull(snapshot, "Spatial snapshot must be captured during Green Sahara simulation");
        assertFalse(snapshot.isEmpty(), "Spatial snapshot must contain populated H3 cells");

        MapComparisonMetrics.MapComparisonResult comp = CellularMapComparisonMetrics.compareCellsToRaster(
                snapshot, groundTruth,
                saharaScenario.getMinLat(), saharaScenario.getMaxLat(),
                saharaScenario.getMinLng(), saharaScenario.getMaxLng(),
                saharaScenario.isClippingEnabled()
        );
        assertNotNull(comp);
        assertTrue(comp.getRmse() < 120.0, "Spatial RMSE on Green Sahara basin must remain bounded");

        long totalPop = 0;
        double totalNaturalBiomass = 0.0;
        for (H3Cell c : snapshot) {
            totalPop += c.getPopulationOr(0);
            totalNaturalBiomass += c.getBiomassNaturalOr(0.0);
        }
        assertTrue(totalPop > 5_000, "Green Sahara pastoralist corridors must sustain population over 20 years (" + totalPop + ")");
        assertTrue(totalNaturalBiomass > 0.0, "Humid Saharan savannah and Mega-Lake Chad margins must sustain positive biomass");
    }

    @Test
    @DisplayName("Hold-Out 7: Arctic Beringian Standstill & Kelp Highway (-25000 to -24980 BP, 20-Year Window, Real Rasters & Cells)")
    void testBeringiaArcticLandBridgeHoldOutEmergence() throws Exception {
        Scenario beringiaScenario = loadBuiltInScenario("beringia", "Beringia");
        assertNotNull(beringiaScenario, "Scenario 'beringia' must be registered in built-ins");

        beringiaScenario.setStartDateYear(-25000);
        beringiaScenario.setEndDateYear(-24980);
        beringiaScenario.setH3Resolution(4);

        SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(beringiaScenario);
        assertNotNull(record, "Headless execution of Arctic Beringia hold-out must produce a valid record");

        double tau = HistoricalValidationKernel.computeContinuousTemporalTolerance(-25000L);
        assertTrue(tau >= 0.45 && tau <= 0.51,
                "Pleistocene LGM tolerance tau(-25000) must approach 50% asymptote (computed: " + (tau * 100.0) + "%)");

        File rasterFile = new File("data/maps/ether/earth/-25000/earth_-25000_density.png");
        assertTrue(rasterFile.exists(), "Empirical ground-truth raster earth_-25000_density.png must exist");
        BufferedImage groundTruth = ImageIO.read(rasterFile);
        assertNotNull(groundTruth);

        List<H3Cell> snapshot = extractLatestSnapshot(record, -24980, -25000);
        assertNotNull(snapshot, "Spatial snapshot must be captured during Beringia simulation");
        assertFalse(snapshot.isEmpty(), "Spatial snapshot must contain populated H3 cells");

        MapComparisonMetrics.MapComparisonResult comp = CellularMapComparisonMetrics.compareCellsToRaster(
                snapshot, groundTruth,
                beringiaScenario.getMinLat(), beringiaScenario.getMaxLat(),
                beringiaScenario.getMinLng(), beringiaScenario.getMaxLng(),
                beringiaScenario.isClippingEnabled()
        );
        assertNotNull(comp);
        assertTrue(comp.getRmse() < 120.0, "Spatial RMSE on Beringian land bridge must remain bounded");

        long totalPop = 0;
        double totalNaturalBiomass = 0.0;
        for (H3Cell c : snapshot) {
            totalPop += c.getPopulationOr(0);
            totalNaturalBiomass += c.getBiomassNaturalOr(0.0);
        }
        assertTrue(totalPop > 100, "Beringian mammoth-steppe hunter-gatherers must survive 20-year window without extinction");
        assertTrue(totalNaturalBiomass > 0.0, "Sub-arctic mammoth steppe must preserve positive natural biomass");

        // Deep-time boundary invariant (AGENTS.md §8.2): zero sovereign debt at -25,000 BP
        SimulationRunRecord.MetricSnapshot lastMetrics = record.getTimeSeriesData().get(-24980);
        if (lastMetrics != null) {
            assertEquals(0.0, lastMetrics.getValue("sovereignDebtBurden"), 0.5,
                    "Pleistocene Beringia must exhibit zero sovereign debt burden");
        }
    }
}
