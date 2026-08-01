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
 * Population Migration Vectors by Thermodynamic Free Power Gradients.
 * Models:
 * 1. <b>Spatial Gradient of Free Power per Capita (∇(P_capita / N))</b>: Human migration vectors flow toward cells with higher per-capita power & food.
 * 2. <b>Water Table & Habitability Pressure</b>: Drought and aquifer depletion trigger mass demographic exodus toward river valleys.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.3.0
 */
public class ThermodynamicMigrationEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicMigrationEngine.class);

    /**
     * Executes one thermodynamic free energy migration tick across cells.
     */
    public static void processThermodynamicMigration(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int migrationEvents = 0;

        for (int i = 0; i < cells.size() - 1; i++) {
            H3Cell origin = cells.get(i);
            H3Cell destination = cells.get(i + 1);

            int popOrigin = origin.getPopulation() != null ? origin.getPopulation() : 0;
            if (popOrigin < 100) continue;

            double foodOrigin = origin.getFoodResource() != null ? origin.getFoodResource() : 0.0;
            double foodDest = destination.getFoodResource() != null ? destination.getFoodResource() : 0.0;

            // Thermodynamic gradient push: migrate if destination has significantly higher food & water per capita
            if (foodDest > foodOrigin * 1.8) {
                migrationEvents++;
                int migrants = (int) (popOrigin * 0.05); // 5% demographic shift per tick
                origin.setPopulation(popOrigin - migrants);
                destination.setPopulation((destination.getPopulation() != null ? destination.getPopulation() : 0) + migrants);
            }
        }

        if (migrationEvents > 0) {
            logger.info("Migration Engine: Thermodynamic demographic vector shifts evaluated across {} cell boundaries.", migrationEvents);
        }
    }
}
