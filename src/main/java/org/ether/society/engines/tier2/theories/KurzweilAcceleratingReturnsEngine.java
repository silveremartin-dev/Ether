/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Kurzweil Law of Accelerating Returns (LOAR) &amp; Paradigm Shift S-Curve Engine.
 *
 * <p>Formally models:
 * 1. Double Exponential Knowledge Growth: \(W(t) = \exp(e^{c \cdot t})\)
 * 2. Nested S-Curve Paradigm Shifts: Logistic envelope transition when physical silicon/lithography limits are reached.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class KurzweilAcceleratingReturnsEngine extends AbstractSimulationEngine implements ProceduralEnginePlugin {

    private static final Logger logger = LoggerFactory.getLogger(KurzweilAcceleratingReturnsEngine.class);

    private static double globalKnowledgeStock = 1.0;
    private static double accelerationRate = 0.05;

    private double cachedDW = 0.0;
    private double cachedParadigmCapacity = 100_000.0;

    public KurzweilAcceleratingReturnsEngine() {
        super("KurzweilAcceleratingReturnsEngine", "Kurzweil Law of Accelerating Returns", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    /**
     * Prepares global knowledge expansion step before cellular derivative accumulation.
     *
     * @param dtYears elapsed time in years
     */
    public void prepareTick(double dtYears) {
        if (dtYears <= 0) {
            cachedDW = 0.0;
            return;
        }
        cachedDW = accelerationRate * Math.min(1e6, globalKnowledgeStock) * Math.log(globalKnowledgeStock + 1.0) * dtYears;
        globalKnowledgeStock = Math.min(1e6, globalKnowledgeStock + cachedDW);
        cachedParadigmCapacity = 100_000.0 / (1.0 + Math.exp(-0.01 * globalKnowledgeStock));
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || cachedDW <= 0) return;
        float pop = buffer.getBiomassHuman()[i];
        if (pop > 0.001f) {
            float currentCap = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0f;
            double capPerCapita = currentCap / pop;
            double headroom = Math.max(0.0, 1.0 - capPerCapita / cachedParadigmCapacity);
            double dCapPerCapita = cachedDW * 10.0 * headroom;
            acc.dResourceCapital += (float) (dCapPerCapita * pop);
        }
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (buffer == null) return;
        prepareTick(dtYears);
        super.tick(buffer, dtYears, dtMonthly);
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processAcceleratingReturns(cells, deltaYears);
    }

    public static void processAcceleratingReturns(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        double dW = accelerationRate * Math.min(1e6, globalKnowledgeStock) * Math.log(globalKnowledgeStock + 1.0) * deltaYears;
        globalKnowledgeStock = Math.min(1e6, globalKnowledgeStock + dW);

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;
            int pop = cell.getPopulation();
            double currentCap = cell.getResourceCapitalOr(0.0);
            double capPerCapita = pop > 0 ? (currentCap / pop) : 0.0;
            double paradigmCapacity = 100_000.0 / (1.0 + Math.exp(-0.01 * globalKnowledgeStock));

            double headroom = Math.max(0.0, 1.0 - capPerCapita / paradigmCapacity);
            double dCapPerCapita = dW * 10.0 * headroom;
            cell.setResourceCapital(currentCap + dCapPerCapita * pop);
        }
    }

    public static double getGlobalKnowledgeStock() {
        return globalKnowledgeStock;
    }

    public static void setGlobalKnowledgeStock(double stock) {
        globalKnowledgeStock = Math.max(0.1, stock);
    }

    public static double getAccelerationRate() {
        return accelerationRate;
    }

    public static void setAccelerationRate(double rate) {
        accelerationRate = Math.max(0.001, rate);
    }

    public static void resetState() {
        globalKnowledgeStock = 1.0;
        accelerationRate = 0.05;
    }

    @Override
    public void reset() {
        resetState();
        cachedDW = 0.0;
    }
}
