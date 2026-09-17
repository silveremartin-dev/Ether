/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Physically Realistic Marine Submersion & Evacuation Engine.
 * 
 * Models sea level rise, coastal flooding, and polder dike breaches with thermodynamic and physical realism:
 * 1. Early Warning & Evacuation Physics: High-tech societies anticipate marine submersion, evacuating
 *    up to 95-98% of vulnerable populations to neighboring higher-elevation cells.
 * 2. Population Relocation & Refugee Flux: Displaced populations and moveable capital are physically
 *    transferred to safe adjacent cells rather than suffering unrealistic sudden mass extinction.
 * 3. Partial Loss & Infrastructure Immersion: Fixed capital infrastructure suffers water immersion damage,
 *    while human life is prioritized through technological foresight and emergency response.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MarineSubmersionEngine {
    private static final Logger logger = LoggerFactory.getLogger(MarineSubmersionEngine.class);

    public static final double DEFAULT_DIKE_MAINTENANCE_BASE_CAPITAL = 50.0;
    public static final double DEFAULT_EVACUATION_TECH_EFFICIENCY = 0.40;

    /**
     * Executes marine submersion and evacuation using default parameters.
     */
    public static void processHybrid(List<H3Cell> cells, double timeStepDays) {
        processHybrid(cells, timeStepDays, DEFAULT_DIKE_MAINTENANCE_BASE_CAPITAL, DEFAULT_EVACUATION_TECH_EFFICIENCY);
    }

    /**
     * Executes marine submersion and evacuation with parameterizable physical thresholds.
     */
    public static void processHybrid(List<H3Cell> cells, double timeStepDays, 
                                    double dikeMaintenanceCapital, double evacuationTechEfficiency) {
        if (cells == null || cells.isEmpty()) return;

        Map<Long, H3Cell> cellMap = new HashMap<>();
        for (H3Cell c : cells) {
            cellMap.put(c.getH3Index(), c);
        }

        for (H3Cell cell : cells) {
            double elevation = cell.getElevation() != null ? cell.getElevation() : 0.0;
            double seaLevelOffset = cell.getSeaLevelOffsetMeters();
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            int pop = cell.getPopulation();

            // Determine if polder/coastal cell is flooded (elevation below sea level or dike failure)
            boolean isSubmerged = (elevation < seaLevelOffset);
            if (cell.getIsPolder() && seaLevelOffset > 0.0) {
                // Polders require capital investment to pump water and maintain dikes
                double requiredMaint = dikeMaintenanceCapital * (1.0 + 0.1 * seaLevelOffset);
                if (capital < requiredMaint) {
                    isSubmerged = true; // Dike breach due to capital deficit
                } else {
                    // Maintenance cost deducted from capital reserve
                    cell.setResourceCapital(Math.max(0.0, capital - requiredMaint * (timeStepDays / 30.0)));
                }
            }

            if (isSubmerged && pop > 0) {
                // 1. Calculate Physically Realistic Evacuation Ratio (Foresight & Warning Systems)
                // Modern/high-tech societies with accumulated capital predict flooding and evacuate early.
                double capitalFactor = 1.0 + Math.min(5.0, capital / 500.0);
                double evacuationRatio = 1.0 - Math.exp(-evacuationTechEfficiency * tech * capitalFactor);
                evacuationRatio = Math.clamp(evacuationRatio, 0.0, 0.98); // Max 98% evacuated cleanly

                int evacuatedPop = (int) (pop * evacuationRatio);
                int remainingPop = pop - evacuatedPop;

                // 2. Locate Safe Neighbor Cells (Elevation > seaLevelOffset)
                List<H3Cell> safeNeighbors = new ArrayList<>();
                for (H3Cell candidate : cells) {
                    if (candidate.getH3Index() != cell.getH3Index() 
                            && (candidate.getElevation() != null && candidate.getElevation() > seaLevelOffset)
                            && candidate.getBiome() != Biome.OCEAN && candidate.getBiome() != Biome.DEEP_OCEAN) {
                        safeNeighbors.add(candidate);
                        if (safeNeighbors.size() >= 6) break; // Limit search radius
                    }
                }

                if (!safeNeighbors.isEmpty() && evacuatedPop > 0) {
                    int popPerNeighbor = evacuatedPop / safeNeighbors.size();
                    double capitalToTransfer = (cell.getResourceCapital() * 0.50) / safeNeighbors.size();

                    for (H3Cell target : safeNeighbors) {
                        target.setPopulation(target.getPopulation() + popPerNeighbor);
                        target.setResourceCapital(target.getResourceCapital() + capitalToTransfer);
                    }

                    // Update flooded cell population and capital after evacuation
                    cell.setPopulation(remainingPop);
                    cell.setResourceCapital(cell.getResourceCapital() * 0.50); // 50% fixed capital submerged

                    logger.info("🌊 Marine Submersion Evacuation: Cell {} evacuated {} humans ({}) to {} safe neighbor cells.",
                            cell.getH3Index(), evacuatedPop, String.format("%.1f%%", evacuationRatio * 100), safeNeighbors.size());
                }

                // 3. Partial Mortality on Un-evacuated Population (Flood depth proportional)
                if (remainingPop > 0) {
                    double floodDepth = seaLevelOffset - elevation;
                    double mortalityRate = Math.clamp(0.05 * floodDepth, 0.01, 0.30); // 1% to 30% max mortality on unprepared residual
                    int casualtyCount = (int) (remainingPop * mortalityRate);
                    cell.setPopulation(remainingPop - casualtyCount);
                }

                // 4. Biome update if permanently flooded
                if (cell.getElevation() != null && cell.getElevation() < -5.0) {
                    cell.setBiome(Biome.OCEAN);
                    cell.setIsPolder(false);
                }
            }
        }
    }
}

