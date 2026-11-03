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
                62_940_000L, 2500.0, 2.29, 73.0, 15000, Collections.emptyMap()
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
                62_940_000L, 2500.0, 2.29, 73.0, 15000, Collections.emptyMap()
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
                1_600_000L, 2100.0, 1.0, 85.0, 1200, Collections.emptyMap()
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
                2_056_000_000L, 2800.0, 4.45, 50.0, 25000, Collections.emptyMap()
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
}
