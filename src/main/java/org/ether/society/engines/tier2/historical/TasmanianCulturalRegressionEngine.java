/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Tasmanian Cultural Regression &amp; Demographic Isolation Engine.
 *
 * <p>Formalizes Joseph Henrich's model of cultural loss (2004, <i>American Antiquity</i> 69:197):
 * when effective demographic pool size falls below the critical threshold required to sustain
 * specialized complex crafts, incomplete cultural transmission leads to the decay and loss of
 * complex physical tooling and capital stock (\(K\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class TasmanianCulturalRegressionEngine extends AbstractSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(TasmanianCulturalRegressionEngine.class);

    private static double isolationPopulationThreshold = 5000.0;
    private double currentLossRate = 0.0;

    public TasmanianCulturalRegressionEngine() {
        super("TasmanianCulturalRegressionEngine", "Tasmanian Cultural Regression", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    /**
     * Prepares the global demographic threshold before cellular derivative sweeps.
     *
     * @param buffer contiguous world memory buffer
     * @param dtYears elapsed time in years
     */
    public void prepareTick(WorldBuffer buffer, double dtYears) {
        if (buffer == null || buffer.getBiomassHuman() == null) {
            currentLossRate = 0.0;
            return;
        }
        float[] pop = buffer.getBiomassHuman();
        double totalPop = 0.0;
        int cap = buffer.getCapacity();
        for (int i = 0; i < cap; i++) {
            if (pop[i] > 0.0f) totalPop += pop[i];
        }
        double lossIntensity = SmoothMath.sigmoid(2500.0 - totalPop, 0.0, 800.0);
        currentLossRate = 0.04 * lossIntensity * dtYears;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (currentLossRate > 1e-6 && buffer != null && buffer.getResourceCapital() != null) {
            float cap = buffer.getResourceCapital()[i];
            if (cap > 0.0f) {
                acc.dResourceCapital -= (float) (cap * currentLossRate);
            }
        }
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (buffer == null) return;
        prepareTick(buffer, dtYears);
        super.tick(buffer, dtYears, dtMonthly);
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processHybrid(cells, deltaYears);
    }

    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        double totalPop = 0.0;
        for (H3Cell c : cells) {
            if (c != null && c.getPopulation() != null && c.getPopulation() > 0) {
                totalPop += c.getPopulation();
            }
        }

        double lossIntensity = SmoothMath.sigmoid(2500.0 - totalPop, 0.0, 800.0);
        double lossRate = 0.04 * lossIntensity * deltaYears;
        if (lossRate > 1e-6) {
            for (H3Cell cell : cells) {
                if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;
                double cap = cell.getResourceCapitalOr(0.0);
                if (cap > 0.0) {
                    cell.setResourceCapital(Math.max(0.0, cap * (1.0 - lossRate)));
                }
            }
        }
    }

    public static double getIsolationPopulationThreshold() {
        return isolationPopulationThreshold;
    }

    public static void setIsolationPopulationThreshold(double threshold) {
        isolationPopulationThreshold = Math.max(100.0, threshold);
    }

    public static void resetState() {
        isolationPopulationThreshold = 5000.0;
    }

    @Override
    public void reset() {
        resetState();
        currentLossRate = 0.0;
    }
}
