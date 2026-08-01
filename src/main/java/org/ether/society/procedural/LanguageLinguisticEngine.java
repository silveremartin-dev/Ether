/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Language, Cultural Isolation & Linguistic Diffusion Engine.
 * Dynamic simulation models:
 * 1. <b>Isolat Culturel / Insularité (Cultural Isolation like Japan/Sakoku)</b>: Island & geographically isolated
 *    territories develop high internal cultural cohesion, preserving unique linguistic groups and resisting
 *    foreign assimilation and uncontrolled immigration.
 * 2. <b>Linguistic Divergence & Dialect Speciation</b>: High terrain friction accelerates local dialect formation.
 * 3. <b>Lingua Franca Diffusion</b>: Trade corridors and imperial capitals diffuse unified languages.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.7.0
 */
public class LanguageLinguisticEngine {
    private static final Logger logger = LoggerFactory.getLogger(LanguageLinguisticEngine.class);

    /**
     * Executes one linguistic drift and cultural isolation cycle across cells and trade routes.
     */
    public static void processLinguisticDrift(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        if (cells == null || cells.isEmpty()) return;

        int isolatedCount = 0;

        for (H3Cell cell : cells) {
            if (cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
            Biome biome = cell.getBiome();

            // 1. Isolat Culturel (Cultural Isolation): Island / Coastal / Mountainous isolation
            boolean isIslandOrIsolated = (biome == Biome.BEACH || friction > 3.5 || cell.getElevation() > 1200);

            if (isIslandOrIsolated) {
                isolatedCount++;
                // Resistance to external linguistic drift (preserve indigenous language)
                double drift = Math.max(0.0, cell.getLinguisticDrift() - 0.02);
                cell.setLinguisticDrift(drift);

                // Boost social cohesion / Asabiyyah in isolated nation territories
                if (cell.getOwner() != null) {
                    cell.getOwner().setAsabiyyah(Math.min(1.0, cell.getOwner().getAsabiyyah() + 0.005));
                }
            } else {
                // Inland connected cells experience standard linguistic drift
                double drift = cell.getLinguisticDrift() + (friction * 0.01);
                cell.setLinguisticDrift(drift);

                if (drift > 1.0 && cell.getLanguageGroup().equals("Proto-Human")) {
                    cell.setLanguageGroup("Dialect-" + Math.abs(cell.getH3Index() % 1000));
                }
            }

            // 2. Political Assimilation (Capital language diffusion)
            if (cell.getOwner() != null && cell.getOwner().getCapital() != null) {
                H3Cell capital = cell.getOwner().getCapital();
                if (capital.getLanguageGroup() != null && !capital.getLanguageGroup().equals(cell.getLanguageGroup())) {
                    // Isolated cells resist foreign assimilation (lower chance)
                    double assimilationChance = isIslandOrIsolated ? 0.03 : 0.15;
                    if (Math.random() < assimilationChance) {
                        cell.setLanguageGroup(capital.getLanguageGroup());
                        cell.setLinguisticDrift(0.1);
                    }
                }
            }
        }

        // 3. Trade Route Convergence (Lingua Franca Diffusion)
        if (activeRoutes != null) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                String hubLang = route.origin().getLanguageGroup();
                for (H3Cell routeCell : route.pathCells()) {
                    boolean isIsolated = routeCell.getBiome() == Biome.BEACH || routeCell.getMovementFriction() > 3.5;
                    double diffusionChance = isIsolated ? 0.05 : 0.25;

                    if (Math.random() < diffusionChance) {
                        routeCell.setLanguageGroup(hubLang);
                        routeCell.setLinguisticDrift(Math.max(0.0, routeCell.getLinguisticDrift() - 0.2));
                    }
                }
            }
        }

        if (isolatedCount > 0) {
            logger.info("Linguistic Engine: {} cultural isolate cells processed.", isolatedCount);
        }
    }
}
