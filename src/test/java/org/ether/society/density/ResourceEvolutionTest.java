/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.density;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResourceEvolutionTest {

    private ResourceEvolution resourceEvolution;
    private H3Cell cell;

    @BeforeEach
    void setUp() {
        resourceEvolution = new ResourceEvolution();
        cell = new H3Cell();
        cell.setH3Index(1L);
        cell.setLatitude(45.0); // Northern hemisphere
    }

    @Test
    @DisplayName("Forests should regenerate wood")
    void testWoodRegeneration() {
        cell.setBiome(Biome.FOREST);
        cell.setWoodResource(500.0);
        cell.setPopulation(0); // No consumption

        // Tick
        resourceEvolution.tick(List.of(cell), 0); // Month 0

        // 500 + (1000 * 0.01) = 510
        assertEquals(510.0, cell.getWoodResource(), 0.01, "Wood should regenerate by 1% of max cap");
    }

    @Test
    @DisplayName("Population should consume water")
    void testWaterConsumption() {
        cell.setBiome(Biome.PLAINS);
        cell.setWaterResource(100.0);
        cell.setRainfall(0.0); // No rain
        cell.setPopulation(100);

        // Tick
        resourceEvolution.tick(List.of(cell), 0);

        // Consumption: 100 * 0.02 = 2.0
        // Evaporation: 100 * 0.02 = 2.0
        // Total lost: 4.0
        // Remaining: 96.0
        assertEquals(96.0, cell.getWaterResource(), 0.01, "Water should be consumed by population and evaporation");
    }

    @Test
    @DisplayName("Overfishing should deplete stock faster")
    void testOverfishing() {
        cell.setBiome(Biome.OCEAN);
        cell.setBiomassFish(100.0);
        cell.setPopulation(600); // 600 * 0.1 = 60 consumption

        // Ratio: 60 / 100 = 0.6 > 0.5 (Threshold) => Overfishing
        // Penalty: 60 * 1.5 = 90 consumption

        // Tick
        resourceEvolution.tick(List.of(cell), 0);
        
        // Expected: 100 - 90 = 10 (ignoring regeneration for moment or assuming slight regen)
        // Actually regen runs first in tick()
        // Regen: 100 + (500 * 0.03 * 1.0) = 100 + 15 = 115
        
        // Then CheckOverextraction on 115
        // Cons = 60
        // Ratio 60/115 = 0.52 > 0.5 => Overfishing penalty
        // Penalty: 60 * 1.5 = 90
        
        // Result: 115 - 90 = 25
        
        assertEquals(25.0, cell.getBiomassFish(), 0.1, "Overfishing should apply 1.5x penalty");
    }

    @Test
    @DisplayName("Season should affect food regeneration")
    void testSeasonalRegeneration() {
        cell.setBiome(Biome.PLAINS);
        cell.setBiomassNatural(0.0);
        cell.setPopulation(0);
        cell.setLatitude(45.0); // North

        // Test Winter (Month 0 - Jan) -> Low Factor (~0.5)
        resourceEvolution.tick(List.of(cell), 0);
        double winterFood = cell.getBiomassNatural();
        
        cell.setBiomassNatural(0.0);
        // Test Summer (Month 5 - June) -> High Factor (~1.0)
        resourceEvolution.tick(List.of(cell), 5);
        double summerFood = cell.getBiomassNatural();
        
        assertTrue(summerFood > winterFood, "Summer regeneration should be higher than winter");
    }
}
