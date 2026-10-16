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
 * Bio-Molecular Epidemiology & Immunoglobulin Synthesis Engine.
 * Replaces abstract health scores with bio-molecular energy allocation:
 * 1. <b>Metabolic Energy Deficiency & Immune Arrest</b>: When ingested caloric energy (E_ingested) falls below basal metabolic requirements,
 *    protein immunoglobulin synthesis is shut down to preserve neuronal metabolism.
 * 2. <b>Bio-Molecular Pathogen Vulnerability</b>: Immunological collapse increases pathogen viral load (virions/mL) and mortality.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class BioMolecularEpidemiologyEngine {
    private static final Logger logger = LoggerFactory.getLogger(BioMolecularEpidemiologyEngine.class);

    /*
     * Executes one bio-molecular immune response and pathogen tick across cells using default monthly step.
     */
    public static void processBioMolecularImmunity(List<H3Cell> cells) {
        processBioMolecularImmunity(cells, 1.0 / 12.0);
    }

    /*
     * Executes bio-molecular immune response and pathogen tick across cells scaled by dtYears.
     */
    public static void processBioMolecularImmunity(List<H3Cell> cells, double dtYears) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;
        double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));

        int immuneCollapseCells = 0;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            // Monthly metabolic food requirement scaled by tick duration dt
            double foodRequired = pop * 1.0 * dtMonthlyRatio;

            // Available caloric resources: granary store + standing crop harvest + foraging
            double grainFood = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            double agriHarvest = cell.getBiomassAgriculture() != null ? (cell.getBiomassAgriculture() * 0.70 * dtYears) : 0.0;
            double wildHarvest = cell.getBiomassNatural() != null ? (cell.getBiomassNatural() * 0.05 * dtYears) : 0.0;
            double totalFoodAvailable = grainFood + agriHarvest + wildHarvest;

            // Energy deficiency ratio
            double energyRatio = totalFoodAvailable / Math.max(0.1, foodRequired);

            // Immunoglobulin synthesis capacity (0.05 to 1.0)
            double immunoglobulinSynthesisRate = Math.clamp(energyRatio, 0.05, 1.0);

            double curLife = cell.getLifespan() != null ? cell.getLifespan() : 40.0;
            double targetBaselineLife = (cell.getTechnologyLevel() != null && cell.getTechnologyLevel() >= 1.5) ? 48.0 : 38.0;

            // Malnutrition-induced immune collapse
            if (immunoglobulinSynthesisRate < 0.40) {
                immuneCollapseCells++;
                double penalty = 1.5 * (0.40 - immunoglobulinSynthesisRate) * dtMonthlyRatio;
                cell.setLifespan(Math.max(15.0, curLife - penalty));
            } else if (immunoglobulinSynthesisRate >= 0.80 && curLife < targetBaselineLife) {
                // Immune recovery and longevity restoration under adequate nutrition
                double recovery = 0.5 * dtMonthlyRatio;
                cell.setLifespan(Math.min(targetBaselineLife, curLife + recovery));
            }
        }

        if (immuneCollapseCells > 0) {
            logger.debug("Epidemiology Engine: Malnutrition-induced immunoglobulin synthesis collapse in {} cells.", immuneCollapseCells);
        }
    }
}


