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
 * Language & Linguistic Diffusion Engine.
 * Models:
 * 1. Dialect divergence in geographically isolated cells (high terrain friction).
 * 2. Linguistic convergence and lingua franca spread along trade corridors and political borders.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.6.0
 */
public class LanguageLinguisticEngine {
    private static final Logger logger = LoggerFactory.getLogger(LanguageLinguisticEngine.class);

    /**
     * Executes one linguistic drift cycle across cells and trade routes.
     */
    public static void processLinguisticDrift(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            // 1. High friction increases linguistic drift (isolation creates regional dialects)
            double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
            double drift = cell.getLinguisticDrift() + (friction * 0.01);
            cell.setLinguisticDrift(drift);

            // Dialect speciation threshold
            if (drift > 1.0 && cell.getLanguageGroup().equals("Proto-Human")) {
                cell.setLanguageGroup("Dialect-" + Math.abs(cell.getH3Index() % 1000));
            }

            // 2. Political assimilation: Nation capitals spread national language to territory
            if (cell.getOwner() != null && cell.getOwner().getCapital() != null) {
                H3Cell capital = cell.getOwner().getCapital();
                if (capital.getLanguageGroup() != null && !capital.getLanguageGroup().equals(cell.getLanguageGroup())) {
                    if (Math.random() < 0.15) {
                        cell.setLanguageGroup(capital.getLanguageGroup());
                        cell.setLinguisticDrift(0.1);
                    }
                }
            }
        }

        // 3. Trade route convergence (Lingua Franca diffusion)
        if (activeRoutes != null) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                String hubLang = route.origin().getLanguageGroup();
                for (H3Cell routeCell : route.pathCells()) {
                    if (Math.random() < 0.25) {
                        routeCell.setLanguageGroup(hubLang);
                        routeCell.setLinguisticDrift(Math.max(0.0, routeCell.getLinguisticDrift() - 0.2));
                    }
                }
            }
        }
    }
}
