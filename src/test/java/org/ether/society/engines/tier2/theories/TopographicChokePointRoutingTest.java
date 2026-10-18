/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Topographic Trade Network Routing & Strategic Choke Points.
 * Verifies that trade bottlenecks (straits, mountain passes, transit corridors) receive
 * choke point scores and collect economic transit toll rents into productive capital.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class TopographicChokePointRoutingTest {

    @Test
    /*
     * Test strategic choke points receive funneling scores and accumulate transit toll revenue.
     */
    public void testChokePointsIdentifyAndCollectTolls() {
        TradeNetworkEngine.resetCumulativeStats();
        List<H3Cell> cells = new ArrayList<>();

        // Hub 1: Western trading city
        H3Cell hubWest = new H3Cell();
        hubWest.setH3Index(7001L);
        hubWest.setPopulation(1200);
        hubWest.setLatitude(36.0);
        hubWest.setLongitude(-6.0);
        hubWest.setElevation(10.0);
        hubWest.setResourceCapital(100.0);
        hubWest.setFoodResource(2000.0);
        hubWest.setResourceMetal(500.0);
        cells.add(hubWest);

        // Strait / Choke Point Cell (e.g. Strait of Gibraltar / Bosphorus)
        H3Cell straitCell = new H3Cell();
        straitCell.setH3Index(7002L);
        straitCell.setPopulation(300);
        straitCell.setLatitude(36.1);
        straitCell.setLongitude(-5.0);
        straitCell.setElevation(5.0);
        straitCell.setIsCoastal(true);
        straitCell.setResourceCapital(50.0);
        cells.add(straitCell);

        // Hub 2: Eastern trading city
        H3Cell hubEast = new H3Cell();
        hubEast.setH3Index(7003L);
        hubEast.setPopulation(1500);
        hubEast.setLatitude(36.2);
        hubEast.setLongitude(-4.0);
        hubEast.setElevation(15.0);
        hubEast.setResourceCapital(120.0);
        hubEast.setFoodResource(500.0);
        hubEast.setResourceMetal(2000.0);
        cells.add(hubEast);

        // Generate trade network
        List<TradeNetworkEngine.TradeRoute> routes = TradeNetworkEngine.generateTradeNetworks(cells, 3.5, 1.0);
        assertNotNull(routes);
        assertFalse(routes.isEmpty(), "Trade routes must be established between populated trade hubs.");

        // Check if intermediate corridor or strait cell funneled trade and accumulated toll
        boolean anyChokePointFound = cells.stream().anyMatch(c -> c.getChokePointScore() != null && c.getChokePointScore() > 0.0);
        assertTrue(anyChokePointFound, "Corridor cells funneling trade routes must receive a non-zero chokePointScore.");

        double totalTollCollected = cells.stream()
                .mapToDouble(c -> c.getTransitTollRevenue() != null ? c.getTransitTollRevenue() : 0.0)
                .sum();
        assertTrue(totalTollCollected > 0.0, "Active trade transit must generate economic transit toll revenues.");
    }

    @Test
    /*
     * Test dynamic route relocation when deep-sea navigation technology emerges.
     */
    public void testMaritimeTechnologyLowersOceanFrictionAndEnablesSeaLanes() {
        H3Cell oceanCell = new H3Cell();
        oceanCell.setH3Index(7005L);
        oceanCell.setElevation(-200.0); // Deep ocean cell
        oceanCell.setBiome(Biome.OCEAN);

        // Pre-maritime era (techLevel = 1.0)
        double preMaritimeFriction = oceanCell.calculateMovementFriction(1.0);
        assertEquals(25.0, preMaritimeFriction, 0.01,
                "Pre-maritime ocean friction must be an impassable barrier (friction = 25.0).");

        // Emergence of deep-sea navigation (techLevel = 4.0)
        double postMaritimeFriction = oceanCell.calculateMovementFriction(4.0);
        assertTrue(postMaritimeFriction < 2.5,
                "Discovery of navigation must dramatically reduce ocean friction (< 2.5).");

        // Advanced modern maritime shipping (techLevel = 8.0)
        double modernMaritimeFriction = oceanCell.calculateMovementFriction(8.0);
        assertTrue(modernMaritimeFriction <= 1.0,
                "Modern shipping must make ocean transit less costly than terrestrial movement (friction <= 1.0).");
    }
}
