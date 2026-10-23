/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.database;

import org.ether.society.database.components.ClimateState;
import org.ether.society.database.components.BiomassState;
import org.ether.society.database.components.DemographicState;
import org.ether.society.database.components.MineralState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for H3Cell Data-Oriented Design (DOD) component records.
 */
public class H3CellComponentTest {

    @Test
    public void testClimateStateRoundtrip() {
        H3Cell cell = new H3Cell(0x882681e33ffffL, 45.0, 5.0);
        cell.setTemperature(18.5);
        cell.setRainfall(850.0);
        cell.setDynamicAlbedo(0.22);
        cell.setIceSheetThicknessMeters(50.0);
        cell.setSeaLevelOffsetMeters(-120.0);

        ClimateState climate = cell.getClimateState();
        assertEquals(18.5, climate.temperature(), 1e-4);
        assertEquals(850.0, climate.rainfall(), 1e-4);
        assertEquals(0.22, climate.dynamicAlbedo(), 1e-4);
        assertEquals(50.0, climate.iceSheetThicknessMeters(), 1e-4);
        assertEquals(-120.0, climate.seaLevelOffsetMeters(), 1e-4);

        ClimateState updated = new ClimateState(22.0, 900.0, 0.20, 0.0, 0.0);
        cell.setClimateState(updated);
        assertEquals(22.0, cell.getTemperature(), 1e-4);
        assertEquals(900.0, cell.getRainfall(), 1e-4);
    }

    @Test
    public void testDemographicStateRoundtrip() {
        H3Cell cell = new H3Cell(0x882681e33ffffL, 45.0, 5.0);
        cell.setPopulation(5000);
        cell.setLifespan(42.5);
        cell.setFertility(5.8);
        cell.setGiniIndex(0.35);
        cell.setTechnologyLevel(2.5);
        cell.setHomininSpecies(1);

        DemographicState demo = cell.getDemographicState();
        assertEquals(5000, demo.population());
        assertEquals(42.5, demo.lifespan(), 1e-4);
        assertEquals(5.8, demo.fertility(), 1e-4);
        assertEquals(0.35, demo.giniIndex(), 1e-4);
        assertEquals(2.5, demo.technologyLevel(), 1e-4);
        assertEquals(1, demo.homininSpecies());

        DemographicState updated = new DemographicState(8000, 50.0, 4.5, 0.40, 3.2, 1);
        cell.setDemographicState(updated);
        assertEquals(8000, cell.getPopulation());
        assertEquals(50.0, cell.getLifespan(), 1e-4);
    }

    @Test
    public void testBiomassStateRoundtrip() {
        H3Cell cell = new H3Cell(0x882681e33ffffL, 45.0, 5.0);
        cell.setFoodResource(350.0);
        cell.setWoodResource(1200.0);
        cell.setWaterResource(500.0);

        BiomassState bio = cell.getBiomassState();
        assertEquals(350.0, bio.foodResource(), 1e-4);
        assertEquals(1200.0, bio.woodResource(), 1e-4);
        assertEquals(500.0, bio.waterResource(), 1e-4);

        BiomassState updated = new BiomassState(25.0, 10.0, 5.0, 150.0, 200.0, 400.0, 600.0, 1500.0, 12.0, 0.2);
        cell.setBiomassState(updated);
        assertEquals(400.0, cell.getFoodResource(), 1e-4);
        assertEquals(1500.0, cell.getWoodResource(), 1e-4);
    }

    @Test
    public void testMineralStateRoundtrip() {
        H3Cell cell = new H3Cell(0x882681e33ffffL, 45.0, 5.0);
        cell.setResourceCoal(150.0);
        cell.setResourceOil(80.0);
        cell.setResourceMetal(200.0);

        MineralState min = cell.getMineralState();
        assertEquals(150.0, min.resourceCoal(), 1e-4);
        assertEquals(80.0, min.resourceOil(), 1e-4);
        assertEquals(200.0, min.resourceMetal(), 1e-4);

        MineralState updated = new MineralState(250.0, 120.0, 50.0, 10.0, 300.0, 5.0, 2.0, 95.0, 400.0, 200.0);
        cell.setMineralState(updated);
        assertEquals(250.0, cell.getResourceCoal(), 1e-4);
        assertEquals(300.0, cell.getResourceMetal(), 1e-4);
    }
}
