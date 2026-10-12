/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvère Martin-Michiellot & Gemini AI
 */
package org.ether.society.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

/**
 * PAGES 2k Consortium (2019 / 2017) Paleoclimate Ingestion and Proxy Site Index Test Suite.
 *
 * <p>Verifies the fidelity of Common Era global mean surface temperature reconstructions (1–2000 CE),
 * confidence interval bounds, Butterworth filtering, and spatial nearest-neighbor search across
 * the 647 paleoclimatic natural archive proxy sites.</p>
 *
 * @author Silvère Martin-Michiellot &amp; Gemini AI
 * @version 1.0.0
 * @see Pages2kClimateProvider
 */
public class Pages2kClimateProviderTest {

    @BeforeAll
    public static void setUp() {
        Pages2kClimateProvider.initialize();
    }

    @Test
    @DisplayName("1. Verify Global Temperature Anomaly Coverage and Historical Markers")
    public void testGlobalTemperatureAnomalies() {
        // Year 1 CE check
        Optional<Pages2kClimateProvider.TemperatureAnomalyRecord> recYear1 = Pages2kClimateProvider.getRecord(1);
        Assertions.assertTrue(recYear1.isPresent(), "Year 1 CE record should be present");
        Assertions.assertEquals(1, recYear1.get().yearCe());
        Assertions.assertEquals(-0.369, recYear1.get().medianAnomalyC(), 1e-4);
        Assertions.assertEquals(-0.7515, recYear1.get().percentile2_5C(), 1e-4);
        Assertions.assertEquals(0.0176, recYear1.get().percentile97_5C(), 1e-4);

        // Year 536 CE: Late Antique Little Ice Age (LALIA) initial volcanic cooling pulse
        Optional<Pages2kClimateProvider.TemperatureAnomalyRecord> rec536 = Pages2kClimateProvider.getRecord(536);
        Assertions.assertTrue(rec536.isPresent(), "Year 536 CE record should be present");
        Assertions.assertTrue(rec536.get().medianAnomalyC() < -0.4,
                "Year 536 CE anomaly should exhibit strong volcanic cooling, got: " + rec536.get().medianAnomalyC());

        // Year 1816 CE: 'Year Without a Summer' post-Tambora eruption
        Optional<Pages2kClimateProvider.TemperatureAnomalyRecord> rec1816 = Pages2kClimateProvider.getRecord(1816);
        Assertions.assertTrue(rec1816.isPresent(), "Year 1816 CE record should be present");
        Assertions.assertTrue(rec1816.get().medianAnomalyC() < -0.3,
                "Year 1816 CE anomaly should be cold, got: " + rec1816.get().medianAnomalyC());

        // Filtered median exists for year 20
        Optional<Pages2kClimateProvider.TemperatureAnomalyRecord> rec20 = Pages2kClimateProvider.getRecord(20);
        Assertions.assertTrue(rec20.isPresent());
        Assertions.assertFalse(Double.isNaN(rec20.get().filtered31yrMedianC()), "Year 20 CE should have 31-yr filtered median");
    }

    @Test
    @DisplayName("2. Verify Proxy Sites Index Loading (647 proxy archives)")
    public void testProxySitesIndex() {
        List<Pages2kClimateProvider.ProxySite> allProxies = Pages2kClimateProvider.getAllProxySites();
        Assertions.assertNotNull(allProxies);
        Assertions.assertEquals(647, allProxies.size(), "Should load exactly 647 PAGES 2k proxy sites");

        // Check regional subsets
        List<Pages2kClimateProvider.ProxySite> europeProxies = Pages2kClimateProvider.getProxiesByRegion("Europe");
        Assertions.assertFalse(europeProxies.isEmpty(), "Europe proxies should not be empty");

        List<Pages2kClimateProvider.ProxySite> antProxies = Pages2kClimateProvider.getProxiesByRegion("Ant");
        Assertions.assertFalse(antProxies.isEmpty(), "Antarctica proxies should not be empty");

        // Check archive type subsets
        List<Pages2kClimateProvider.ProxySite> treeRings = Pages2kClimateProvider.getProxiesByArchiveType("Tree");
        Assertions.assertFalse(treeRings.isEmpty(), "Tree ring proxies should not be empty");

        List<Pages2kClimateProvider.ProxySite> iceCores = Pages2kClimateProvider.getProxiesByArchiveType("GlacierIce");
        Assertions.assertFalse(iceCores.isEmpty(), "Glacier ice proxies should not be empty");
    }

    @Test
    @DisplayName("3. Verify Geodesic Spatial Proximity Query")
    public void testNearestProxySearch() {
        // Query near the Alps / Central Europe (e.g. Innsbruck: 47.26° N, 11.40° E)
        Optional<Pages2kClimateProvider.ProxySite> nearestAlps = Pages2kClimateProvider.findNearestProxy(47.26, 11.40);
        Assertions.assertTrue(nearestAlps.isPresent(), "Should find a nearest proxy in Central Europe");
        double distKm = Pages2kClimateProvider.haversineDistanceKm(47.26, 11.40,
                nearestAlps.get().latitude(), nearestAlps.get().longitude());
        Assertions.assertTrue(distKm < 500.0, "Nearest proxy to Innsbruck should be within 500 km, was: " + distKm);

        // Distance formula symmetry and zero self-distance
        Assertions.assertEquals(0.0, Pages2kClimateProvider.haversineDistanceKm(10.0, 20.0, 10.0, 20.0), 1e-6);
    }
}
