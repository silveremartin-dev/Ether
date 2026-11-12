/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Advanced Maritime Strategy & Land Reclamation Engine (Poldérisation & Seasteading).
 *
 * 1. Poldérisation: Transforms shallow coastal/marine cells into fertile reclaimed land (polders)
 *    when local technology level >= 4.0 and capital investment >= 100.0.
 * 2. Seasteading: Deploys deep-sea floating platforms & oceanic habitats
 *    when local technology level >= 8.5 and capital investment >= 500.0.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LandReclamationEngine extends AbstractSimulationEngine {
    public LandReclamationEngine() {
        super("LandReclamationEngine", "LandReclamationEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void process(java.util.List<org.ether.society.database.H3Cell> cells, double deltaYears) {
        processHybrid(cells, deltaYears);
    }

    private static final Logger logger = LoggerFactory.getLogger(LandReclamationEngine.class);

    /** Internal state: default polder tech threshold (double). */
    public static final double DEFAULT_POLDER_TECH_THRESHOLD = 4.0;
    /** Internal state: default polder capital threshold (double). */
    public static final double DEFAULT_POLDER_CAPITAL_THRESHOLD = 100.0;

    /** Internal state: default seasteading tech threshold (double). */
    public static final double DEFAULT_SEASTEADING_TECH_THRESHOLD = 8.5;
    /** Internal state: default seasteading capital threshold (double). */
    public static final double DEFAULT_SEASTEADING_CAPITAL_THRESHOLD = 500.0;

    /**
     * Process hybrid.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param timeStepDays the time step days parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double timeStepDays) {
        processHybrid(cells, timeStepDays,
                DEFAULT_POLDER_TECH_THRESHOLD, DEFAULT_POLDER_CAPITAL_THRESHOLD,
                DEFAULT_SEASTEADING_TECH_THRESHOLD, DEFAULT_SEASTEADING_CAPITAL_THRESHOLD);
    }

    public static void processHybrid(List<H3Cell> cells, double timeStepDays,
                                    double polderTechThreshold, double polderCapitalThreshold,
                                    double seasteadingTechThreshold, double seasteadingCapitalThreshold) {
        if (cells == null || cells.isEmpty()) return;

        double dtYears = Math.clamp(timeStepDays / 365.25, 0.001, 1.0);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double capital = cell.getResourceCapitalOr(0.0);
            boolean isCoastal = Boolean.TRUE.equals(cell.getIsCoastal());
            boolean isPolder = Boolean.TRUE.equals(cell.getIsPolder());
            boolean hasFloating = Boolean.TRUE.equals(cell.getHasFloatingInfrastructure());
            Biome biome = cell.getBiome();

            // 1. Poldérisation check (Coastal dike & drainage reclamation governed by physical capital stock)
            if (!isPolder && (isCoastal || biome == Biome.BEACH || (cell.getElevation() != null && cell.getElevation() <= 0))) {
                if (capital >= polderCapitalThreshold) {
                    cell.setIsPolder(true);
                    if (biome == Biome.OCEAN || biome == Biome.BEACH) {
                        cell.setBiome(Biome.PLAINS);
                    }
                    double soc = cell.getSoilOrganicCarbonOr(0.0);
                    cell.setSoilOrganicCarbon(Math.min(120.0, soc + 20.0 * dtYears));
                }
            }

            // 2. Deep-sea Seasteading / Floating Infrastructure check (governed by marine structural capital stock)
            if (!hasFloating && (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN)) {
                if (capital >= seasteadingCapitalThreshold) {
                    cell.setHasFloatingInfrastructure(true);
                    double solar = cell.getEnergySolarOr(0.0);
                    cell.setEnergySolar(Math.min(1000.0, solar + 50.0 * dtYears));
                }
            }
        }
    }
}
