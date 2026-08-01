/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Selective Species Domestication & Artificial Breeding Meta-Engine.
 * Models:
 * 1. <b>Cultivar & Livestock Breeding</b>: Artificial selection of crops (wheat, rice, maize) and animal breeds (cattle, sheep).
 * 2. <b>Photosynthetic Efficiency Gain</b>: Increases crop PAR conversion efficiency η_PAR and caloric yield per hectare over historical centuries.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.5.0
 */
public class SelectiveBreedingEngine {
    private static final Logger logger = LoggerFactory.getLogger(SelectiveBreedingEngine.class);

    /**
     * Calculates dynamic artificial selection yield multiplier.
     *
     * @param techLevel Technology era level
     * @return Crop yield breeding factor (1.0 to 3.5)
     */
    public static double calculateSelectiveBreedingYieldFactor(double techLevel) {
        if (techLevel < 1.0) return 1.0; // Wild gatherer plants
        return 1.0 + Math.min(2.5, techLevel * 0.35); // 3.5x multiplier in Green Revolution era
    }

    /**
     * Executes one selective breeding tick across cells.
     */
    public static void processSelectiveBreeding(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            if (tech >= 1.0) {
                double breedingFactor = calculateSelectiveBreedingYieldFactor(tech);
                double currentAgri = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
                cell.setBiomassAgriculture(currentAgri * (1.0 + (breedingFactor - 1.0) * 0.02));
            }
        }
    }
}
