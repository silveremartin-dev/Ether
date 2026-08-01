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
 * Technology Breakthrough & Spatiotemporal Diffusion Engine.
 * Models:
 * 1. Emergence of technological innovations (Agriculture, Metallurgy, Printing, Industrial Steam)
 *    in high-capital / high-density hubs.
 * 2. Spatiotemporal diffusion along trade corridors and low-friction plains.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.6.0
 */
public class TechTreeEngine {
    private static final Logger logger = LoggerFactory.getLogger(TechTreeEngine.class);

    public record TechEra(String name, double minTechLevel, double unlockCapitalCost) {}

    public static final TechEra NEOLITHIC = new TechEra("Agriculture & Pottery", 1.0, 100.0);
    public static final TechEra BRONZE_AGE = new TechEra("Metallurgy & Writing", 3.0, 500.0);
    public static final TechEra RENAISSANCE = new TechEra("Printing Press & Navigation", 6.0, 2500.0);
    public static final TechEra INDUSTRIAL = new TechEra("Steam Engine & Mechanization", 8.5, 10000.0);

    /**
     * Executes one technology innovation and diffusion step.
     */
    public static void processTechnologyDiffusion(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        if (cells == null || cells.isEmpty()) return;

        int innovationCount = 0;

        for (H3Cell cell : cells) {
            double currentTech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;

            // 1. Breakthrough condition: High capital + urban density
            if (capital > 1000.0 && cell.getPopulation() > 500) {
                if (currentTech < INDUSTRIAL.minTechLevel() && capital > INDUSTRIAL.unlockCapitalCost()) {
                    cell.setTechnologyLevel(INDUSTRIAL.minTechLevel());
                    innovationCount++;
                    logger.info("Technological Breakthrough: Industrial Steam unlocked at Cell {}.", cell.getH3Index());
                } else if (currentTech < RENAISSANCE.minTechLevel() && capital > RENAISSANCE.unlockCapitalCost()) {
                    cell.setTechnologyLevel(RENAISSANCE.minTechLevel());
                    innovationCount++;
                } else {
                    // Gradual local innovation
                    cell.setTechnologyLevel(currentTech + 0.01);
                }
            }
        }

        // 2. Spatial Diffusion along Trade Corridors
        if (activeRoutes != null) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                double maxTech = Math.max(route.origin().getTechnologyLevel(), route.destination().getTechnologyLevel());
                for (H3Cell pathCell : route.pathCells()) {
                    if (pathCell.getTechnologyLevel() < maxTech) {
                        double stepTech = pathCell.getTechnologyLevel() + 0.02;
                        pathCell.setTechnologyLevel(Math.min(maxTech, stepTech));
                    }
                }
            }
        }

        if (innovationCount > 0) {
            logger.info("TechTree Engine: {} major innovations spawned.", innovationCount);
        }
    }
}
