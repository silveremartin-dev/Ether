package org.ether.society.generation;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier2.theories.ProceduralPopulationEngine;
import org.ether.society.model.FutureAndExoplanetScenarioCatalog;
import org.ether.society.model.Scenario;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MarsColonyDistributionTest {

    @Test
    public void testMarsPopulationDistribution() {
        Scenario marsScenario = FutureAndExoplanetScenarioCatalog.getBuiltInScenarios().stream()
                .filter(s -> s.getPresetKey() != null && s.getPresetKey().contains("mars"))
                .findFirst()
                .orElse(null);

        assertNotNull(marsScenario, "Mars scenario should exist in catalog");
        System.out.println("Mars scenario name: " + marsScenario.getName());
        System.out.println("Mars initial pop: " + marsScenario.getInitialHumanCount());
        System.out.println("Mars density pattern: " + marsScenario.getPopulationDensityType());

        // Create sample cells representing Mars surface
        List<H3Cell> sampleCells = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            H3Cell c = new H3Cell();
            c.setH3Index((long) i);
            c.setLatitude(-80.0 + (i / 500.0) * 160.0);
            c.setLongitude(-180.0 + (i % 25) * 14.4);
            c.setElevation(-2000.0 + (i % 10) * 500.0);
            c.setBiome(org.ether.society.model.Biome.DESERT);
            c.setAccessibleAquifer(100.0);
            c.setWaterResource(50.0);
            c.setResourceMetal(500.0);
            sampleCells.add(c);
        }

        ProceduralPopulationEngine.distributePopulation(
                sampleCells,
                marsScenario,
                marsScenario.getInitialHumanCount(),
                marsScenario.getInitialTechLevel(),
                marsScenario.getPopulationDensityType(),
                false,
                marsScenario.getStartDateYear()
        );

        long populatedCellCount = sampleCells.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).count();
        int maxPop = sampleCells.stream().mapToInt(c -> c.getPopulationOr(0)).max().orElse(0);
        int minPopInPopulated = sampleCells.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).mapToInt(c -> c.getPopulationOr(0)).min().orElse(0);

        System.out.println("Populated cell count: " + populatedCellCount + " out of " + sampleCells.size());
        System.out.println("Max population on single cell: " + maxPop);
        System.out.println("Min population in populated cell: " + minPopInPopulated);

        org.junit.jupiter.api.Assertions.assertEquals(2, populatedCellCount, "Exactly 2 outposts must be populated on Mars");
        org.junit.jupiter.api.Assertions.assertEquals(300, maxPop, "Chryse Outpost must have 300 colonists (60%)");
        org.junit.jupiter.api.Assertions.assertEquals(200, minPopInPopulated, "Jezero Outpost must have 200 colonists (40%)");
    }
}
