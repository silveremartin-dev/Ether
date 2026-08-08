/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.PhysicalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Population Migration Vectors by Thermodynamic Free Power Gradients.
 * 
 * <h2>Onsager Reciprocal Transport Equations</h2>
 * <pre>
 *   Φ_i = (Food_i + Water_i) / (Pop_i + 1)
 *   ΔΦ_ij = Φ_j - Φ_i
 *   J_ij = M_0 * ΔΦ_ij * Pop_i
 *   d(Pop_i)/dt = - ∑_j J_ij
 *   d(Pop_j)/dt = + ∑_j J_ij
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class ThermodynamicMigrationEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicMigrationEngine.class);

    /**
     * Executes one thermodynamic free energy migration tick across cells using Onsager flux relations.
     */
    public static void processThermodynamicMigration(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int migrationEvents = 0;

        for (int i = 0; i < cells.size() - 1; i++) {
            H3Cell origin = cells.get(i);
            H3Cell destination = cells.get(i + 1);

            int popOrigin = origin.getPopulation() != null ? origin.getPopulation() : 0;
            int popDest = destination.getPopulation() != null ? destination.getPopulation() : 0;
            if (popOrigin < 10) continue;

            double foodOrigin = origin.getFoodResource() != null ? origin.getFoodResource() : 0.0;
            double waterOrigin = origin.getWaterResource() != null ? origin.getWaterResource() : 0.0;

            double foodDest = destination.getFoodResource() != null ? destination.getFoodResource() : 0.0;
            double waterDest = destination.getWaterResource() != null ? destination.getWaterResource() : 0.0;

            double phiOrigin = (foodOrigin + waterOrigin) / (popOrigin + 1.0);
            double phiDest = (foodDest + waterDest) / (popDest + 1.0);

            double deltaPhi = phiDest - phiOrigin;

            // Thermodynamic Onsager gradient push: migrate if destination potential per capita is strictly higher
            if (deltaPhi > 0.05) {
                migrationEvents++;
                double fluxFraction = Math.clamp(PhysicalConstants.ONSAGER_BASELINE_MOBILITY * deltaPhi, 0.01, 0.20);
                int migrants = (int) Math.clamp((long) (popOrigin * fluxFraction), 1, popOrigin / 2);

                origin.setPopulation(popOrigin - migrants);
                destination.setPopulation(popDest + migrants);
            }
        }

        if (migrationEvents > 0) {
            logger.info("Migration Engine: Thermodynamic Onsager demographic vector shifts evaluated across {} cell boundaries.", migrationEvents);
        }
    }
}
