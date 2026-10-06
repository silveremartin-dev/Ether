/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Advanced Maritime Strategy & Land Reclamation Engine (PoldÃ©risation & Seasteading).
 *
 * 1. PoldÃ©risation: Transforms shallow coastal/marine cells into fertile reclaimed land (polders)
 *    when local technology level >= 4.0 and capital investment >= 100.0.
 * 2. Seasteading: Deploys deep-sea floating platforms & oceanic habitats
 *    when local technology level >= 8.5 and capital investment >= 500.0.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LandReclamationEngine {
    private static final Logger logger = LoggerFactory.getLogger(LandReclamationEngine.class);

    /* Internal state variable for default polder tech threshold (double). */
    public static final double DEFAULT_POLDER_TECH_THRESHOLD = 4.0;
    /* Internal state variable for default polder capital threshold (double). */
    public static final double DEFAULT_POLDER_CAPITAL_THRESHOLD = 100.0;

    /* Internal state variable for default seasteading tech threshold (double). */
    public static final double DEFAULT_SEASTEADING_TECH_THRESHOLD = 8.5;
    /* Internal state variable for default seasteading capital threshold (double). */
    public static final double DEFAULT_SEASTEADING_CAPITAL_THRESHOLD = 500.0;

    /*
     * Process hybrid.
     * Enforces physical invariants and updates associated state variables within {@code LandReclamationEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param timeStepDays the time step days parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double timeStepDays) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        processHybrid(cells, timeStepDays,
                DEFAULT_POLDER_TECH_THRESHOLD, DEFAULT_POLDER_CAPITAL_THRESHOLD,
                DEFAULT_SEASTEADING_TECH_THRESHOLD, DEFAULT_SEASTEADING_CAPITAL_THRESHOLD);
    }

    public static void processHybrid(List<H3Cell> cells, double timeStepDays,
                                    double polderTechThreshold, double polderCapitalThreshold,
                                    double seasteadingTechThreshold, double seasteadingCapitalThreshold) {
        if (cells == null || cells.isEmpty()) return;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            boolean isCoastal = cell.getIsCoastal();
            Biome biome = cell.getBiome();

            // 1. PoldÃ©risation check (Coastal water / shallow elevation reclamation)
            if (!cell.getIsPolder() && (isCoastal || biome == Biome.BEACH || (cell.getElevation() != null && cell.getElevation() <= 0))) {
                if (tech >= polderTechThreshold && capital >= polderCapitalThreshold) {
                    cell.setIsPolder(true);
                    if (biome == Biome.OCEAN || biome == Biome.BEACH) {
                        cell.setBiome(Biome.PLAINS);
                    }
                    cell.setSoilOrganicCarbon(Math.max(100.0, cell.getSoilOrganicCarbon() + 20.0 * timeStepDays));
                }
            }

            // 2. Deep-sea Seasteading / Floating Infrastructure check
            if (!cell.getHasFloatingInfrastructure() && (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN)) {
                if (tech >= seasteadingTechThreshold && capital >= seasteadingCapitalThreshold) {
                    cell.setHasFloatingInfrastructure(true);
                    // Floating energy & water collection bonus
                    cell.setEnergySolar(cell.getEnergySolar() + 50.0 * timeStepDays);
                }
            }
        }
    }
}


