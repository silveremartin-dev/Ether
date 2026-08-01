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
 * Bio-Molecular Epidemiology & Immunoglobulin Synthesis Engine.
 * Replaces abstract health scores with bio-molecular energy allocation:
 * 1. <b>Metabolic Energy Deficiency & Immune Arrest</b>: When ingested caloric energy (E_ingested) falls below basal metabolic requirements,
 *    protein immunoglobulin synthesis is shut down to preserve neuronal metabolism.
 * 2. <b>Bio-Molecular Pathogen Vulnerability</b>: Immunological collapse increases pathogen viral load (virions/mL) and mortality.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.0.0
 */
public class BioMolecularEpidemiologyEngine {
    private static final Logger logger = LoggerFactory.getLogger(BioMolecularEpidemiologyEngine.class);

    /**
     * Executes one bio-molecular immune response and pathogen tick across cells.
     */
    public static void processBioMolecularImmunity(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int immuneCollapseCells = 0;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double foodAvailable = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            double foodRequired = pop * 1.0; // 1.0 unit per capita

            // Energy deficiency ratio
            double energyRatio = foodAvailable / Math.max(1.0, foodRequired);

            // Immunoglobulin synthesis capacity (0.0 to 1.0)
            double immunoglobulinSynthesisRate = Math.min(1.0, Math.max(0.05, energyRatio));

            // Malnutrition-induced immune collapse
            if (immunoglobulinSynthesisRate < 0.40) {
                immuneCollapseCells++;
                // Lifespan degradation from bio-molecular vulnerability
                cell.setLifespan(Math.max(12.0, cell.getLifespan() - 1.5 * (0.40 - immunoglobulinSynthesisRate)));
            }
        }

        if (immuneCollapseCells > 0) {
            logger.info("Epidemiology Engine: Malnutrition-induced immunoglobulin synthesis collapse in {} cells.", immuneCollapseCells);
        }
    }
}
