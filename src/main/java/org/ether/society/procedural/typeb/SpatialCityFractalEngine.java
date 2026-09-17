/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

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
 * @version 1.0.0-beta.1
 */
public class SpatialCityFractalEngine {
    private static final Logger logger = LoggerFactory.getLogger(SpatialCityFractalEngine.class);

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.size() < 2) return;

        // Sort cells by population descending to compute spatial power law hierarchy
        List<H3Cell> sortedCells = cells.stream()
                .filter(c -> c != null && c.getPopulation() > 0)
                .sorted(Comparator.comparingInt(H3Cell::getPopulation).reversed())
                .collect(Collectors.toList());

        if (sortedCells.isEmpty()) return;

        double topCityPop = sortedCells.get(0).getPopulation();
        for (int rank = 1; rank <= sortedCells.size(); rank++) {
            H3Cell cell = sortedCells.get(rank - 1);
            // Zipf-Mori spatial power law target: Pop_target = Pop_top / rank^1.05
            double targetPop = topCityPop / Math.pow(rank, 1.05);
            double currentPop = cell.getPopulation();
            // Smoothly gravitate population toward fractal power law equilibrium
            cell.setPopulation((int) (currentPop + (targetPop - currentPop) * 0.01 * deltaYears));
        }
    }
}

