/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.generation;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;

/**
 * Mineral, fossil, agricultural and biological resource initializer for planetary cells.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PlanetResourcePopulator {

    private PlanetResourcePopulator() {
    }

    public static void populateResources(H3Cell cell, ProceduralGenerator.PlanetPoint p, PlanetPreset preset) {
        Biome b = cell.getBiome();
        if (b == null) return;

        double tidalForce = ProceduralGenerator.computeTidalForceIntensity(preset);

        if (preset.atmospherePressureAtm() < 0.01) {
            if (b == Biome.MOUNTAINS || b == Biome.HILLS) {
                cell.setResourceMetal(600.0 + p.metalDensity() * 800.0);
            } else {
                cell.setResourceMetal(250.0);
            }
            return;
        }

        double tidalBiomassUpwelling = 1.0 + 0.3 * Math.min(3.0, tidalForce);

        switch (b) {
            case FOREST  -> cell.setWoodResource(1000.0);
            case JUNGLE  -> { cell.setWoodResource(2000.0); cell.setFoodResource(400.0); }
            case PLAINS  -> cell.setFoodResource(600.0);
            case HILLS   -> { cell.setFoodResource(200.0); cell.setWoodResource(400.0); }
            case MOUNTAINS -> {
                double metalBase = 400.0 + p.metalDensity() * 600.0;
                cell.setResourceMetal(metalBase);
            }
            case OCEAN, DEEP_OCEAN -> {
                cell.setBiomassFish(800.0 * tidalBiomassUpwelling);
                cell.setFoodResource(200.0 * tidalBiomassUpwelling);
            }
            case BEACH -> {
                cell.setFoodResource(150.0 * tidalBiomassUpwelling);
                cell.setBiomassFish(300.0 * tidalBiomassUpwelling);
            }
            case DESERT -> {
                cell.setResourceMetal(200.0);
                cell.setResourceOil(1500.0);
                cell.setResourceGas(1200.0);
            }
            default -> { }
        }

        double crustMetal = p.metalDensity();
        if (b != Biome.OCEAN && b != Biome.DEEP_OCEAN) {
            cell.setResourceClay(500.0 * (1.0 + p.riverFlow() * 1.5));
        }
        if (b == Biome.FOREST || b == Biome.JUNGLE || b == Biome.PLAINS) {
            cell.setResourceCoal(2500.0 * (0.5 + 0.5 * crustMetal));
            cell.setResourcePreciousMetal(5.0 * (0.1 + 0.9 * p.riverFlow()));
        }
        if (b == Biome.PLAINS || b == Biome.BEACH || b == Biome.DESERT) {
            cell.setResourceOil(Math.max(cell.getResourceOil() != null ? cell.getResourceOil() : 0.0, 1800.0 * crustMetal));
            cell.setResourceGas(Math.max(cell.getResourceGas() != null ? cell.getResourceGas() : 0.0, 1500.0 * crustMetal));
        }
        if (b == Biome.MOUNTAINS || b == Biome.HILLS) {
            cell.setResourceUranium(150.0 * (0.4 + 0.8 * crustMetal));
            cell.setResourceRareEarths(120.0 * (0.3 + 0.7 * crustMetal));
            cell.setResourcePreciousMetal(25.0 * (0.3 + 0.7 * crustMetal));
        }
    }
}
