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
    private static long baseSeed = 42L;

    /**
     * Resets mutable static state between scenario executions to guarantee isolation.
     */
    public static void resetState() {
        baseSeed = 42L;
    }

    /**
     * Sets the scenario-derived random seed for deterministic language assimilation and diffusion.
     *
     * @param seed the scenario seed (long)
     */
    public static void setBaseSeed(long seed) {
        baseSeed = seed;
    }

    /**
     * Gets the scenario-derived random seed for deterministic language assimilation and diffusion.
     *
     * @return the base seed (long)
     */
    public static long getBaseSeed() {
        return baseSeed;
    }

    /**
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

            double friction = cell.getMovementFrictionOr(1.0);
            Biome biome = cell.getBiome();
            double elevation = cell.getElevationOr(0.0);
            double curDrift = cell.getLinguisticDriftOr(0.0);

            // 1. Isolat Culturel (Cultural Isolation): Island / Coastal / Mountainous isolation
            boolean isIslandOrIsolated = (friction > 3.5 || elevation > 1200.0);

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
                    // Continuous political language assimilation inversely scaling with terrain friction (Cavalli-Sforza & Feldman 1981):
                    double frict = cell.getMovementFrictionOr(1.0);
                    double assimilationChance = 0.02 + 0.13 / (1.0 + 0.6 * frict * frict);
                    long cellId = cell.getH3Index() != null ? cell.getH3Index() : 0L;
                    if (org.ether.society.util.DeterministicPRNG.nextDouble(baseSeed, 0L, cellId, 311L) < assimilationChance) {
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
                    double rFriction = routeCell.getMovementFrictionOr(1.0);
                    // Continuous lingua franca diffusion along trade routes scaling with route conductance (Nichols 1992):
                    double diffusionChance = 0.04 + 0.21 / (1.0 + 0.4 * rFriction * rFriction);

                    long routeCellId = routeCell.getH3Index() != null ? routeCell.getH3Index() : 0L;
                    if (org.ether.society.util.DeterministicPRNG.nextDouble(baseSeed, 0L, routeCellId, 313L) < diffusionChance) {
                        routeCell.setLanguageGroup(hubLang);
                        double routeDrift = routeCell.getLinguisticDriftOr(0.0);
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


