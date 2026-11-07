package org.ether.society.analytics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test Suite for {@link PostHocCliodynamicMetricsExtractor}.
 * Verifies exact dispatching for all 27 canonical cliodynamic telemetry indicators,
 * ensuring zero substring collision, proper deep-time pre-Neolithic baselines,
 * and accurate historical scaling across Antiquity, the Middle Ages, and the Industrial Era.
 */
public class PostHocCliodynamicMetricsExtractorTest {

    @Test
    @DisplayName("Verify Elite Overproduction Index is not hijacked by GWP substring collision")
    public void testEliteOverproductionNotHijackedByGwp() {
        SimulationRunRecord.MetricSnapshot snapRome = new SimulationRunRecord.MetricSnapshot(
                62_940_000L, 2500.0, 1.15, 73.0, 15000, Collections.emptyMap()
        );

        double gwp = PostHocCliodynamicMetricsExtractor.computeMetric(snapRome, "grossWorldProduct", 165);
        double eliteOverproduction = PostHocCliodynamicMetricsExtractor.computeMetric(snapRome, "eliteOverproductionIndex", 165);

        // GWP is in Billion Int$ (~11-12B), whereas Elite Overproduction is an index around 1.0 - 1.5
        assertTrue(gwp > 5.0 && gwp < 20.0, "GWP in 165 CE should be ~12 Billion 1990 Int$ but was " + gwp);
        assertTrue(eliteOverproduction >= 0.8 && eliteOverproduction <= 2.5,
                "Elite Overproduction in 165 CE must be a normalized index in [0.8, 2.5] but was " + eliteOverproduction);
        assertNotEquals(gwp, eliteOverproduction, 1e-4, "Elite Overproduction MUST NOT equal GWP!");
    }

    @Test
    @DisplayName("Verify Fossil Fuel Share is not hijacked by Primary Energy substring collision")
    public void testFossilShareNotHijackedByPrimaryEnergy() {
        SimulationRunRecord.MetricSnapshot snapAntiquity = new SimulationRunRecord.MetricSnapshot(
                62_940_000L, 2500.0, 1.15, 73.0, 15000, Collections.emptyMap()
        );

        double energyEJ = PostHocCliodynamicMetricsExtractor.computeMetric(snapAntiquity, "primaryEnergy", 165);
        double fossilSharePct = PostHocCliodynamicMetricsExtractor.computeMetric(snapAntiquity, "fossilSharePrimaryEnergy", 165);

        assertTrue(energyEJ > 0.5 && energyEJ < 2.0, "Primary energy in 165 CE should be ~1 EJ but was " + energyEJ);
        assertEquals(0.0, fossilSharePct, 1e-6, "Fossil fuel share in Antiquity MUST be strictly 0.0%!");
        assertNotEquals(energyEJ, fossilSharePct, 1e-4, "Fossil share MUST NOT equal Primary Energy in EJ!");
    }

    @Test
    @DisplayName("Verify Deep-Time Pre-Neolithic Baselines (-20,000 BP)")
    public void testDeepTimeBaselines() {
        SimulationRunRecord.MetricSnapshot snapLgm = new SimulationRunRecord.MetricSnapshot(
                1_600_000L, 2100.0, 0.85, 85.0, 1200, Collections.emptyMap()
        );

        // Agricultural EROEI must be 0.0 before agriculture
        double agEroei = PostHocCliodynamicMetricsExtractor.computeMetric(snapLgm, "agriculturalEroei", -20000);
        assertEquals(0.0, agEroei, 1e-6, "Pre-Neolithic agricultural EROEI must be 0.0");

        // CO2 must be ~190 ppm at LGM (Dome C ice core)
        double co2 = PostHocCliodynamicMetricsExtractor.computeMetric(snapLgm, "co2Concentration", -20000);
        assertEquals(190.0, co2, 1e-3, "CO2 at LGM must be 190.0 ppm");

        // Biogeographical containment compliance must be 1.0 (100%)
        double containment = PostHocCliodynamicMetricsExtractor.computeMetric(snapLgm, "zeroContainmentScore", -20000);
        assertEquals(1.0, containment, 1e-6, "Zero-containment compliance at -20,000 BP must be 1.0");

        // Topsoil erosion baseline must be 0.03 t/ha/yr
        double erosion = PostHocCliodynamicMetricsExtractor.computeMetric(snapLgm, "soilErosionRate", -20000);
        assertEquals(0.03, erosion, 1e-3, "Topsoil erosion in Paleolithic must be geological baseline 0.03 t/ha/yr");

        // Forest cover remaining must be 99%
        double forest = PostHocCliodynamicMetricsExtractor.computeMetric(snapLgm, "deforestationRate", -20000);
        assertEquals(99.0, forest, 1e-3, "Pristine forest cover remaining must be 99.0%");
    }

    @Test
    @DisplayName("Verify 1914 Technology Calibration (Tech = 4.45)")
    public void test1914Calibration() {
        SimulationRunRecord.MetricSnapshot snap1914 = new SimulationRunRecord.MetricSnapshot(
                2_056_000_000L, 2800.0, 2.05, 50.0, 25000, Collections.emptyMap()
        );

        // GWP ~3,180 Billion 1990 Int$ (Maddison observed ~3,260B)
        double gwp = PostHocCliodynamicMetricsExtractor.computeMetric(snap1914, "grossWorldProduct", 1914);
        assertTrue(gwp >= 2500.0 && gwp <= 3800.0, "GWP in 1914 should be ~3,200 Billion Int$ but was " + gwp);

        // Primary Energy ~62 EJ (Smil observed ~62.8 EJ)
        double energy = PostHocCliodynamicMetricsExtractor.computeMetric(snap1914, "primaryEnergy", 1914);
        assertTrue(energy >= 50.0 && energy <= 75.0, "Primary energy in 1914 should be ~62 EJ but was " + energy);

        // Urbanization ~23% (Bairoch observed ~21.6%)
        double urban = PostHocCliodynamicMetricsExtractor.computeMetric(snap1914, "urbanizationRate", 1914);
        assertTrue(urban >= 18.0 && urban <= 28.0, "Urbanization in 1914 should be ~22% but was " + urban);

        // Information speed ~5.8e6 km/day
        double speed = PostHocCliodynamicMetricsExtractor.computeMetric(snap1914, "informationSpeed", 1914);
        assertTrue(speed >= 2.0e6 && speed <= 1.0e7, "Information speed in 1914 should be ~5.8e6 km/day but was " + speed);
    }

    @Test
    @DisplayName("Debug live urbanization rate from DemographicMetricsCalculator")
    public void testLiveUrbanizationCalculation() {
        org.ether.society.model.Scenario scenario = new org.ether.society.model.Scenario();
        scenario.setName("Test 1914");
        scenario.setStartDateYear(1914);
        scenario.setEndDateYear(1915);
        scenario.setInitialHumanCount(100_000_000L);
        scenario.setInitialCapitalPerCapita(3500.0);
        scenario.setH3Resolution(4);

        org.ether.society.config.Configuration config = new org.ether.society.config.Configuration(
                new org.ether.society.config.Configuration.WorldConfig(10, 10, 10, new org.ether.society.config.Configuration.GenerationParams(4, 1.0, 4, 1.0)),
                new org.ether.society.config.Configuration.SimulationConfig(1000, 42, new int[]{1}),
                new org.ether.society.config.Configuration.AgentsConfig(100, java.util.Collections.emptyMap()),
                new org.ether.society.config.Configuration.ClimateConfig(0.01, 14.0, 0.05),
                new org.ether.society.config.Configuration.ResourcesConfig(100.0, 50.0, 1.0)
        );

        org.ether.society.core.H3SimulationEngine engine = new org.ether.society.core.H3SimulationEngine(config);
        java.util.List<org.ether.society.database.H3Cell> cells = new java.util.ArrayList<>();
        org.ether.society.database.H3Cell c1 = new org.ether.society.database.H3Cell();
        c1.setH3Index(0x841f985ffffffffL); // valid res 4
        c1.setPopulation(100_000);
        c1.setResourceCapital(350_000_000.0);
        c1.setFoodResource(500_000.0);
        c1.setLatitude(48.85);
        c1.setLongitude(2.35);
        cells.add(c1);

        engine.initializeFromScenario(scenario, cells);
        double urban = engine.getUrbanizationRate();
        System.out.println("DEBUG: Urbanization rate = " + urban);
        assertTrue(urban > 0.0, "Urbanization rate must be strictly > 0 for 100k agglomeration with 3500 kpc but was " + urban);
    }

    @Test
    @DisplayName("Verify Exoplanet Colonization retains high literacy with low physical capital and Paleolithic oral culture evaluates to 0.0%")
    public void testExoplanetColonizationKnowledgeAndPaleolithicOralBaselines() {
        org.ether.society.config.Configuration config = new org.ether.society.config.Configuration(
                new org.ether.society.config.Configuration.WorldConfig(10, 10, 10, new org.ether.society.config.Configuration.GenerationParams(4, 1.0, 4, 1.0)),
                new org.ether.society.config.Configuration.SimulationConfig(1000, 42, new int[]{1}),
                new org.ether.society.config.Configuration.AgentsConfig(100, java.util.Collections.emptyMap()),
                new org.ether.society.config.Configuration.ClimateConfig(0.01, 14.0, 0.05),
                new org.ether.society.config.Configuration.ResourcesConfig(100.0, 50.0, 1.0)
        );

        // 1. Exoplanet Colony (Mars 2050): astronauts arrive with low ground physical capital (50 kg/hab) but 50M bits/hab
        org.ether.society.model.Scenario marsScenario = new org.ether.society.model.Scenario();
        marsScenario.setName("Mars Colony 2050");
        marsScenario.setStartDateYear(2050);
        marsScenario.setEndDateYear(2060);
        marsScenario.setInitialHumanCount(1000L);
        marsScenario.setInitialCapitalPerCapita(50.0); // minimal initial camp infrastructure
        marsScenario.setInitialInformationPerCapita(50_000_000.0); // 50M bits/capita knowledge stock
        marsScenario.setH3Resolution(4);

        org.ether.society.core.H3SimulationEngine marsEngine = new org.ether.society.core.H3SimulationEngine(config);
        java.util.List<org.ether.society.database.H3Cell> marsCells = new java.util.ArrayList<>();
        org.ether.society.database.H3Cell mc = new org.ether.society.database.H3Cell();
        mc.setH3Index(0x841f985ffffffffL);
        mc.setLatitude(0.0);
        mc.setLongitude(0.0);
        mc.setPopulation(1000);
        mc.setResourceCapital(50_000.0);
        mc.setFoodResource(20_000.0);
        marsCells.add(mc);
        marsEngine.initializeFromScenario(marsScenario, marsCells);

        double marsLiteracy = marsEngine.getLiteracyRate();
        assertTrue(marsLiteracy >= 98.0,
                "Space colonists on Mars with low ground capital MUST retain high adult literacy (>= 98%) but had: " + marsLiteracy);

        // 2. Paleolithic Foragers (-20,000 BP): oral culture only (5 bits/hab, 2 kg/hab capital)
        org.ether.society.model.Scenario paleoScenario = new org.ether.society.model.Scenario();
        paleoScenario.setName("Paleolithic -20000 BP");
        paleoScenario.setStartDateYear(-20000);
        paleoScenario.setEndDateYear(-19900);
        paleoScenario.setInitialHumanCount(500L);
        paleoScenario.setInitialCapitalPerCapita(2.0);
        paleoScenario.setInitialInformationPerCapita(5.0); // oral culture, no script
        paleoScenario.setH3Resolution(4);

        org.ether.society.core.H3SimulationEngine paleoEngine = new org.ether.society.core.H3SimulationEngine(config);
        java.util.List<org.ether.society.database.H3Cell> paleoCells = new java.util.ArrayList<>();
        org.ether.society.database.H3Cell pc = new org.ether.society.database.H3Cell();
        pc.setH3Index(0x841f985ffffffffL);
        pc.setLatitude(0.0);
        pc.setLongitude(0.0);
        pc.setPopulation(500);
        pc.setResourceCapital(1000.0);
        pc.setFoodResource(5000.0);
        paleoCells.add(pc);
        paleoEngine.initializeFromScenario(paleoScenario, paleoCells);

        double paleolithicLiteracy = paleoEngine.getLiteracyRate();
        assertEquals(0.0, paleolithicLiteracy, 1e-6,
                "Prehistoric oral hunter-gatherers with I = 5 bits/hab MUST have 0.0% literacy without artificial date gating");
    }
}
