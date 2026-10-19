/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spatial City Fractal Power Law Engine Variant B21.1 (Pure) & B21.2 (Hybrid).
 * Mori, Smith & Hsu (2020) Law: City-size distributions follow recursive spatial fractal power laws
 * (N_i proportional to Rank^-alpha) across spatial hierarchies.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SpatialCityFractalEngine {
    private static final Logger logger = LoggerFactory.getLogger(SpatialCityFractalEngine.class);

    /**
     * Process hybrid.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.size() < 2) return;

        // Sort cells by population descending to compute spatial power law hierarchy
        List<H3Cell> sortedCells = cells.stream()
                .filter(c -> c != null && c.getPopulation() != null && c.getPopulation() > 0)
                .sorted(Comparator.comparingInt((H3Cell c) -> c.getPopulationOr(0)).reversed())
                .collect(Collectors.toList());

        if (sortedCells.isEmpty()) return;

        double topCityPop = sortedCells.get(0).getPopulation();
        // Zipf-Mori spatial power law target shape: Pop_target(rank) proportional to 1 / rank^1.05,
        // renormalized so that the redistribution conserves total population (migration, not creation).
        long totalPop = 0L;
        double shapeSum = 0.0;
        for (int rank = 1; rank <= sortedCells.size(); rank++) {
            totalPop += sortedCells.get(rank - 1).getPopulation();
            shapeSum += 1.0 / Math.pow(rank, 1.05);
        }
        if (totalPop <= 0L || shapeSum <= 0.0 || topCityPop <= 0.0) return;
        double relax = Math.min(1.0, 0.01 * deltaYears);
        long assigned = 0L;
        for (int rank = 1; rank <= sortedCells.size(); rank++) {
            H3Cell cell = sortedCells.get(rank - 1);
            double targetPop = totalPop * (1.0 / Math.pow(rank, 1.05)) / shapeSum;
            double currentPop = cell.getPopulation();
            int newPop = (int) Math.round(currentPop + (targetPop - currentPop) * relax);
            if (rank == sortedCells.size()) {
                newPop = (int) Math.max(0L, totalPop - assigned); // absorb rounding residue to conserve the total
            }
            cell.setPopulation(Math.max(0, newPop));
            assigned += cell.getPopulation();
        }
    }
}


