/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite validating core spatial domain models:
 * {@link CityPoint}, {@link RiverRibbon}, {@link LanguageZone}, {@link EmpireTerritory}, and {@link ConfessionalPocket}.
 */
class SpatialDomainModelsTest {

    @Test
    @DisplayName("CityPoint stores coordinates, weight, and Gaussian sigma correctly")
    void testCityPoint() {
        CityPoint city = new CityPoint("Rome", 41.9, 12.5, 4.0, 2.5);
        assertEquals("Rome", city.name);
        assertEquals(41.9, city.lat);
        assertEquals(12.5, city.lng);
        assertEquals(4.0, city.weight);
        assertEquals(2.5, city.sigma);
    }

    @Test
    @DisplayName("RiverRibbon accurately calculates distance to point along curved meridian/parallel geometry")
    void testRiverRibbonDistance() {
        RiverRibbon river = new RiverRibbon(10.0, 45.0, 12.0, 45.0, 2.0, 0.5);

        // Point directly on midpoint of river
        double distMid = river.distanceToPoint(11.0, 45.0);
        assertEquals(0.0, distMid, 1e-4, "Distance to midpoint on segment should be 0");

        // Point offset by 1 degree north
        double distNorth = river.distanceToPoint(11.0, 46.0);
        assertEquals(1.0, distNorth, 1e-2, "Distance 1 deg north should be ~1.0 degree");
    }

    @Test
    @DisplayName("EmpireTerritory correctly performs bounding box containment checks")
    void testEmpireTerritoryContainment() {
        EmpireTerritory romanEmpire = new EmpireTerritory("Imperium Romanum", new Color(168, 85, 247));
        romanEmpire.addBoundingBox(-10.0, 30.0, 40.0, 55.0);

        assertTrue(romanEmpire.contains(12.5, 41.9), "Rome must be inside Imperium Romanum bounds");
        assertFalse(romanEmpire.contains(120.0, 30.0), "China must not be inside Roman bounds");
    }

    @Test
    @DisplayName("LanguageZone stores linguistic center coordinates and visual palette")
    void testLanguageZone() {
        LanguageZone latin = new LanguageZone("Latin", 12.5, 41.9, Color.RED);
        assertEquals("Latin", latin.name);
        assertEquals(12.5, latin.centerLng);
        assertEquals(41.9, latin.centerLat);
        assertEquals(Color.RED, latin.color);
    }

    @Test
    @DisplayName("ConfessionalPocket record stores religious cluster center, radius, and multi-color distributions")
    void testConfessionalPocket() {
        int[] colors = new int[]{0xFF0000, 0x00FF00};
        double[] weights = new double[]{0.7, 0.3};
        ConfessionalPocket pocket = new ConfessionalPocket(35.2, 31.8, 1.5, colors, weights);

        assertEquals(35.2, pocket.lon());
        assertEquals(31.8, pocket.lat());
        assertEquals(1.5, pocket.radiusDeg());
        assertArrayEquals(colors, pocket.colors());
        assertArrayEquals(weights, pocket.weights());
    }
}
