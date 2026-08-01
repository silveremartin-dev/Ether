/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProceduralPopulationEngineTest {

    private List<H3Cell> mockCells;

    @BeforeEach
    public void setUp() {
        mockCells = new ArrayList<>();

        // Create mock cells with different biomes, elevations, temperatures, and water levels
        // Cell 0: Fertile River Plains (High suitability)
        H3Cell c0 = new H3Cell(100L, 30.0, 35.0);
        c0.setElevation(100.0);
        c0.setBiome(Biome.PLAINS);
        c0.setTemperature(20.0);
        c0.setWaterResource(800.0);
        c0.setFoodResource(600.0);

        // Cell 1: High Mountain (Low suitability at low tech)
        H3Cell c1 = new H3Cell(101L, 32.0, 36.0);
        c1.setElevation(2800.0);
        c1.setBiome(Biome.MOUNTAINS);
        c1.setTemperature(2.0);
        c1.setWaterResource(100.0);

        // Cell 2: Arid Desert far from water
        H3Cell c2 = new H3Cell(102L, 25.0, 30.0);
        c2.setElevation(200.0);
        c2.setBiome(Biome.DESERT);
        c2.setTemperature(38.0);
        c2.setWaterResource(20.0);

        // Cell 3: Coastal Plains
        H3Cell c3 = new H3Cell(103L, 28.0, 33.0);
        c3.setElevation(10.0);
        c3.setBiome(Biome.BEACH);
        c3.setTemperature(22.0);
        c3.setWaterResource(900.0);

        mockCells.add(c0);
        mockCells.add(c1);
        mockCells.add(c2);
        mockCells.add(c3);
    }

    @Test
    public void testProceduralDistributionNeolithic() {
        Scenario s = new Scenario();
        long totalPop = 10000;
        double techLevel = 1.0; // Neolithic / stone age

        ProceduralPopulationEngine.distributePopulation(mockCells, s, totalPop, techLevel, "FERTILE_CRESCENT", false, -10000);

        long sumPop = mockCells.stream().mapToLong(H3Cell::getPopulation).sum();
        assertEquals(totalPop, sumPop, "Total population must sum to target population");

        // Cell 0 (plains/river) & Cell 3 (coastal) should receive almost all population in Neolithic
        assertTrue(mockCells.get(0).getPopulation() > mockCells.get(1).getPopulation(), "River plains should have much higher pop than high mountains");
        assertTrue(mockCells.get(0).getPopulation() > mockCells.get(2).getPopulation(), "River plains should have much higher pop than desert far from water");
    }

    @Test
    public void testProceduralDistributionModernTechAdaptation() {
        Scenario s = new Scenario();
        long totalPop = 100000;
        double highTech = 6.0; // 18th century / Industrial

        ProceduralPopulationEngine.distributePopulation(mockCells, s, totalPop, highTech, "URBAN_CLUSTERS", false, 1800);

        long sumPop = mockCells.stream().mapToLong(H3Cell::getPopulation).sum();
        assertEquals(totalPop, sumPop, "Total population must match target population");

        // High tech allows population to spread to mountains/deserts more than at low tech
        assertTrue(mockCells.get(1).getPopulation() > 0, "High tech enables survival in mountain regions");
        assertTrue(mockCells.get(2).getPopulation() > 0, "High tech enables survival in arid regions");
    }

    @Test
    public void testEarthHistoricalDistribution() {
        Scenario s = new Scenario();
        long totalPop = 50000000;

        // Test Bronze Age Earth preset (-3000)
        ProceduralPopulationEngine.distributePopulation(mockCells, s, totalPop, 2.5, "FERTILE_CRESCENT", true, -3000);

        long sumPop = mockCells.stream().mapToLong(H3Cell::getPopulation).sum();
        assertEquals(totalPop, sumPop, "Earth preset population distribution must sum to total population");
    }
}
