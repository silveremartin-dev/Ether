/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for {@link DODDataGenerator}.
 * Verifies seamless cellular ingestion into native DOD {@link WorldBuffer} and {@link AgentBuffer},
 * demographic cohort subdivision, and demographic tensor initialization.
 */
class DODDataGeneratorTest {

    @Test
    @DisplayName("populateWorldBuffer faithfully transfers spatial attributes from H3Cells to WorldBuffer")
    void testPopulateWorldBuffer() {
        int cellCount = 5;
        List<H3Cell> cells = new ArrayList<>();

        for (int i = 0; i < cellCount; i++) {
            H3Cell c = new H3Cell((long) (1000 + i), 10.0 + i, 20.0 + i);
            c.setElevation(150.0 + i * 10);
            c.setBiome(Biome.PLAINS);
            c.setPopulation(5000 + i * 1000);
            c.setBiomassHuman(5000.0 + i * 1000);
            c.setFoodResource(250.0);
            c.setWaterResource(300.0);
            c.setWoodResource(100.0);
            c.setResourceMetal(45.0);
            c.setResourceClay(50.0);
            c.setBiomassLivestock(100.0);
            c.setBiomassFish(100.0);
            c.setBiomassAgriculture(100.0);
            c.setBiomassNatural(100.0);
            c.setEnergyWind(10.0);
            c.setEnergySolar(10.0);
            c.setEnergyFire(10.0);
            c.setEnergySlaves(0.0);
            c.setEnergyFoodConsumed(100.0);
            c.setTechnologyLevel(1.5 + i * 0.1);
            c.setGiniIndex(0.32);
            c.setLifespan(60.0);
            c.setFertility(2.1);
            cells.add(c);
        }

        WorldBuffer buffer = new WorldBuffer(10);
        DODDataGenerator.populateWorldBuffer(cells, buffer);

        for (int i = 0; i < cellCount; i++) {
            assertEquals(1000 + i, buffer.getH3Indexes()[i]);
            assertEquals(150.0f + i * 10, buffer.getElevation()[i], 1e-3);
            assertEquals(5000.0f + i * 1000, buffer.getBiomassHuman()[i], 1e-3);
            assertEquals(250.0f, buffer.getFoodResource()[i], 1e-3);
            assertEquals(300.0f, buffer.getWaterResource()[i], 1e-3);
            assertEquals(45.0f, buffer.getMetalResource()[i], 1e-3);
            assertEquals(1.5f + i * 0.1f, buffer.getTechnologyLevel()[i], 1e-3);
            assertEquals(0.32f, buffer.getGiniIndex()[i], 1e-3);
            assertEquals(60.0f, buffer.getLifespan()[i], 1e-3);
        }
    }

    @Test
    @DisplayName("initializeAgentBuffer creates demographic cohorts matching population and target cohort size")
    void testInitializeAgentBuffer() {
        int capacity = 2;
        WorldBuffer world = new WorldBuffer(capacity);
        world.getH3Indexes()[0] = 55555L;
        world.getBiomassHuman()[0] = 1500.0f; // 1500 people
        world.getTechnologyLevel()[0] = 2.0f;

        world.getH3Indexes()[1] = 66666L;
        world.getBiomassHuman()[1] = 0.0f; // Unpopulated cell

        AgentBuffer agents = new AgentBuffer(20, 4);
        int cohortSize = 500;

        DODDataGenerator.initializeAgentBuffer(world, agents, cohortSize);

        // 1500 pop with target 500 = 3 cohorts
        for (int c = 0; c < 3; c++) {
            assertEquals(0, agents.getHexIds()[c]);
            assertEquals(55555L, agents.getH3Indexes()[c]);
            assertEquals(500.0f, agents.getMass()[c], 1e-3);
            assertEquals(2.0f, agents.getTechLevel()[c], 1e-3);
            assertTrue(agents.getAge()[c] >= 22.0f && agents.getAge()[c] <= 28.0f,
                    "Community cohort mean age must reflect pre-industrial stable distribution (22-28 yrs)");
            
            // Genetics & culture tensors should be within [0, 1]
            for (int d = 0; d < 4; d++) {
                assertTrue(agents.getGenetics()[d][c] >= 0.0f && agents.getGenetics()[d][c] <= 1.0f);
                assertTrue(agents.getCulture()[d][c] >= 0.0f && agents.getCulture()[d][c] <= 1.0f);
            }
        }
    }
}
