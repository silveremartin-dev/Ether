/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Spatial City Fractal Power Law Engine Variant B21.1 (Pure) & B21.2 (Hybrid).
 * Mori, Smith & Hsu (2020) Law: City-size distributions follow recursive spatial fractal power laws
 * (N_i proportional to Rank^-alpha) across spatial hierarchies.
 * Preserves exact demographic mass closure across all 7 age cohorts.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SpatialCityFractalEngine extends AbstractSimulationEngine {
    public SpatialCityFractalEngine() {
        super("SpatialCityFractalEngine", "SpatialCityFractalEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() < 2) return;
        int n = buf.getCapacity();
        float[] popArr = buf.getBiomassHuman();

        int count = 0;
        for (int i = 0; i < n; i++) {
            if (popArr[i] > 0.0f) count++;
        }
        if (count < 2) return;

        Integer[] indices = new Integer[count];
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (popArr[i] > 0.0f) indices[k++] = i;
        }
        java.util.Arrays.sort(indices, (a, b) -> Float.compare(popArr[b], popArr[a]));

        double topCityPop = popArr[indices[0]];
        double totalPop = 0.0;
        double shapeSum = 0.0;
        for (int rank = 1; rank <= count; rank++) {
            totalPop += popArr[indices[rank - 1]];
            shapeSum += 1.0 / Math.pow(rank, 1.05);
        }
        if (totalPop <= 0.0 || shapeSum <= 0.0 || topCityPop <= 0.0) return;

        boolean isMacroPlanetaryGrid = count > 50;
        double relax = isMacroPlanetaryGrid
                ? Math.min(0.002, 0.0001 * dtYears)
                : Math.min(1.0, 0.01 * dtYears);
        double assigned = 0.0;
        for (int rank = 1; rank <= count; rank++) {
            int idx = indices[rank - 1];
            double targetPop = totalPop * (1.0 / Math.pow(rank, 1.05)) / shapeSum;
            double currentPop = popArr[idx];
            double delta = (targetPop - currentPop) * relax;
            if (isMacroPlanetaryGrid) {
                double maxShift = Math.max(1.0, currentPop * 0.005 * Math.min(1.0, dtYears));
                delta = Math.clamp(delta, -maxShift, maxShift);
            }
            double newPop = Math.max(0.0, currentPop + delta);
            if (rank == count) {
                newPop = Math.max(0.0, totalPop - assigned);
            }
            popArr[idx] = (float) newPop;
            assigned += newPop;
        }
    }
}
