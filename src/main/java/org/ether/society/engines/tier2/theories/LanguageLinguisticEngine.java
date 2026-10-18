/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

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
 * @version 1.0.0-beta.2
 */
public class LanguageLinguisticEngine {
    private static final Logger logger = LoggerFactory.getLogger(LanguageLinguisticEngine.class);

    /*
     * Executes one linguistic drift and cultural isolation cycle across cells and trade routes.
     */
    public static void processLinguisticDrift(List<H3Cell> cells, List<TradeNetworkEngine.TradeRoute> activeRoutes) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        int isolatedCount = 0;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;

            double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
            Biome biome = cell.getBiome();
            double elevation = cell.getElevation() != null ? cell.getElevation() : 0.0;
            double curDrift = cell.getLinguisticDrift() != null ? cell.getLinguisticDrift() : 0.0;

            // 1. Isolat Culturel (Cultural Isolation): Island / Coastal / Mountainous isolation
            boolean isIslandOrIsolated = (biome == Biome.BEACH || friction > 3.5 || elevation > 1200);

            if (isIslandOrIsolated) {
                isolatedCount++;
                // Resistance to external linguistic drift (preserve indigenous language)
                double drift = Math.max(0.0, curDrift - 0.02);
                cell.setLinguisticDrift(drift);

                // Boost social cohesion / Asabiyyah in isolated nation territories
                if (cell.getOwner() != null) {
                    cell.getOwner().setAsabiyyah(Math.min(1.0, cell.getOwner().getAsabiyyah() + 0.005));
                }
            } else {
                // Inland connected cells experience standard linguistic drift
                double drift = curDrift + (friction * 0.01);
                cell.setLinguisticDrift(drift);

                String langGroup = cell.getLanguageGroup() != null ? cell.getLanguageGroup() : "Proto-Human";
                if (drift > 1.0 && "Proto-Human".equals(langGroup)) {
                    long cellId = cell.getH3Index() != null ? cell.getH3Index() : 0L;
                    cell.setLanguageGroup("Dialect-" + Math.abs(cellId % 1000));
                }
            }

            // 2. Political Assimilation (Capital language diffusion)
            if (cell.getOwner() != null && cell.getOwner().getCapital() != null) {
                H3Cell capital = cell.getOwner().getCapital();
                if (capital.getLanguageGroup() != null && !capital.getLanguageGroup().equals(cell.getLanguageGroup())) {
                    // Isolated cells resist foreign assimilation (lower chance)
                    double assimilationChance = isIslandOrIsolated ? 0.03 : 0.15;
                    long cellId = cell.getH3Index() != null ? cell.getH3Index() : 0L;
                    if (org.ether.society.util.DeterministicPRNG.nextDouble(42L, 0L, cellId, 311L) < assimilationChance) {
                        cell.setLanguageGroup(capital.getLanguageGroup());
                        cell.setLinguisticDrift(0.1);
                    }
                }
            }
        }

        // 3. Trade Route Convergence (Lingua Franca Diffusion)
        if (activeRoutes != null) {
            for (TradeNetworkEngine.TradeRoute route : activeRoutes) {
                if (route == null || route.origin() == null) continue;
                String hubLang = route.origin().getLanguageGroup();
                if (hubLang == null || route.pathCells() == null) continue;

                // Iterate over spatial cell domains and apply localized cellular state transformations
                for (H3Cell routeCell : route.pathCells()) {
                    if (routeCell == null) continue;
                    double rFriction = routeCell.getMovementFriction() != null ? routeCell.getMovementFriction() : 1.0;
                    boolean isIsolated = routeCell.getBiome() == Biome.BEACH || rFriction > 3.5;
                    double diffusionChance = isIsolated ? 0.05 : 0.25;

                    long routeCellId = routeCell.getH3Index() != null ? routeCell.getH3Index() : 0L;
                    if (org.ether.society.util.DeterministicPRNG.nextDouble(42L, 0L, routeCellId, 313L) < diffusionChance) {
                        routeCell.setLanguageGroup(hubLang);
                        double routeDrift = routeCell.getLinguisticDrift() != null ? routeCell.getLinguisticDrift() : 0.0;
                        routeCell.setLinguisticDrift(Math.max(0.0, routeDrift - 0.2));
                    }
                }
            }
        }

        if (isolatedCount > 0) {
            logger.info("Linguistic Engine: {} cultural isolate cells processed.", isolatedCount);
        }
    }
}


