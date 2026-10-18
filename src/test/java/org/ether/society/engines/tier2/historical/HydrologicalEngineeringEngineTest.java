/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Anthropogenic Hydrological Landscape Engineering:
 * 1. Tenochtitlan / Lake Texcoco Chinampas & Urban Reclamation.
 * 2. Aral Sea Endorheic Desiccation & Desertification.
 * 3. High-Mountain Hydroelectric Dams & Water Retention Reservoirs.
 */
public class HydrologicalEngineeringEngineTest {

    @Test
    @DisplayName("Tenochtitlan Model: High tech and capital drain shallow lake into urban agricultural polders")
    public void testTenochtitlanUrbanReclamation() {
        H3Cell lakeCell = new H3Cell(2001L, 19.43, -99.13); // Mexico Basin coordinates
        lakeCell.setBiome(Biome.LAKE);
        lakeCell.setPopulation(3000);
        lakeCell.setTechnologyLevel(4.0); // > 3.5 threshold
        lakeCell.setResourceCapital(200.0); // > 150.0 threshold
        lakeCell.setFoodResource(200.0);
        lakeCell.setSoilOrganicCarbon(50.0);
        lakeCell.setIsPolder(false);

        HydrologicalEngineeringEngine.processHybrid(List.of(lakeCell), 30.0);

        assertEquals(Biome.PLAINS, lakeCell.getBiome(), "Drained lake basin must transition to PLAINS biome");
        assertTrue(lakeCell.getIsPolder(), "Reclaimed chinampa basin must be tagged as polder");
        assertEquals(250.0, lakeCell.getFoodResource(), 1e-3, "Agricultural chinampas must add 50 units of food resource");
        assertEquals(100.0, lakeCell.getSoilOrganicCarbon(), 1e-3, "Wetland infill must floor soil organic carbon at minimum fertile threshold 100.0");
    }

    @Test
    @DisplayName("Aral Sea Model: Intensive irrigation in arid warm basin desiccates lake into desert")
    public void testAralSeaEndorheicDesiccation() {
        H3Cell aralCell = new H3Cell(2002L, 45.0, 60.0);
        aralCell.setBiome(Biome.LAKE);
        aralCell.setPopulation(2000);
        aralCell.setTechnologyLevel(3.2); // > 3.0 threshold
        aralCell.setResourceCapital(120.0); // > 100.0 threshold
        aralCell.setRainfall(150.0); // < 300 mm threshold
        aralCell.setTemperature(25.0); // >= 10 C
        aralCell.setWaterResource(60.0);
        aralCell.setMovementFriction(1.0);

        HydrologicalEngineeringEngine.processHybrid(List.of(aralCell), 30.0);

        // Water depleted from 60.0 by 20.0 -> 40.0 <= 50.0
        assertEquals(40.0, aralCell.getWaterResource(), 1e-3, "Irrigation extraction must reduce water resource");
        assertEquals(Biome.DESERT, aralCell.getBiome(), "Depleted water below 50.0 must desiccate lake into DESERT");
        assertEquals(1.35, aralCell.getMovementFriction(), 1e-3, "Desiccated salt flat must increase movement friction by 35%");
    }

    @Test
    @DisplayName("Mountain Dam Model: High elevation construction creates hydroelectric retention reservoir")
    public void testMountainDamHydroelectricConstruction() {
        H3Cell mountainCell = new H3Cell(2003L, 46.0, 8.0); // Alpine coordinates
        mountainCell.setElevation(800.0); // >= 300m threshold
        mountainCell.setMovementFriction(1.5); // >= 1.3
        mountainCell.setTechnologyLevel(5.0); // >= 4.5 threshold
        mountainCell.setResourceCapital(300.0); // >= 250.0 threshold
        mountainCell.setWaterResource(100.0);
        mountainCell.setEnergySolar(50.0);
        mountainCell.setResourceWork(20.0);
        mountainCell.setIsPolder(false);

        HydrologicalEngineeringEngine.processHybrid(List.of(mountainCell), 30.0);

        assertEquals(1000.0, mountainCell.getWaterResource(), 1e-3, "Dam reservoir must stabilize water resource at 1000.0");
        assertEquals(150.0, mountainCell.getEnergySolar(), 1e-3, "Hydroelectric power generation must add 100.0 units of energy");
        assertEquals(50.0, mountainCell.getResourceWork(), 1e-3, "Dam work capacity must add 30.0 units of work");
    }
}
