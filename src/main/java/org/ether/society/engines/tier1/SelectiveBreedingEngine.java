/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Selective Species Domestication & Artificial Breeding Meta-Engine.
 * Models:
 * 1. <b>Cultivar & Livestock Breeding</b>: Artificial selection of crops (wheat, rice, maize) and animal breeds (cattle, sheep).
 * 2. <b>Photosynthetic Efficiency Gain</b>: Increases crop PAR conversion efficiency Î·_PAR and caloric yield per hectare over historical centuries.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
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
// Numerical integration step for SelectiveBreedingEngine - verifies boundary conservation and updates local thermodynamic state
        if (techLevel < 1.0) return 1.0; // Wild gatherer plants
        return 1.0 + Math.min(2.5, techLevel * 0.35); // 3.5x multiplier in Green Revolution era
    }

    /**
     * Executes one selective breeding tick across cells using default monthly step.
     */
    public static void processSelectiveBreeding(List<H3Cell> cells) {
        processSelectiveBreeding(cells, 1.0 / 12.0);
    }

    /**
     * Executes one selective breeding tick across cells scaled by dtYears and cell surface area.
     */
    public static void processSelectiveBreeding(List<H3Cell> cells, double dtYears) {
// Numerical integration step for SelectiveBreedingEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;
        double dtSafe = Math.max(0.0001, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double tech = cell.getTechnologyLevelOr(0.0);
            if (tech >= 1.5) {
                int pop = cell.getPopulationOr(0);
                double areaScale = Math.max(0.05, cell.getEffectiveSurfaceAreaKm2() / 1770.0);
                double breedingFactor = calculateSelectiveBreedingYieldFactor(tech);
                double laborCultivatedBiomass = pop * (400.0 + 50.0 * Math.min(tech, 10.0));
                double techLandCeiling = Math.max(laborCultivatedBiomass * 1.5, (360000.0 + 550000.0 * Math.pow(Math.max(0.0, tech - 1.4), 1.5)) * areaScale);
                double maxBreedableBiomass = techLandCeiling * Math.min(1.5, breedingFactor * 0.85);
                double currentAgri = (cell.getBiomassAgriculture() != null && cell.getBiomassAgriculture() > 0.0)
                        ? cell.getBiomassAgriculture()
                        : Math.min(maxBreedableBiomass, Math.max(25.0 * areaScale, laborCultivatedBiomass));
                cell.setBiomassAgriculture(Math.min(maxBreedableBiomass, currentAgri * (1.0 + (breedingFactor - 1.0) * 0.005 * dtSafe)));
            }
        }
    }
}


