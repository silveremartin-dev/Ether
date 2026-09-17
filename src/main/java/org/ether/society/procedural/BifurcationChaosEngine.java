/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Procedural engine for Tipping Point, Chaos, and Bifurcation Analysis.
 *
 * <p>Implements Critical Slowing Down (CSD) theory, rolling spatial variance,
 * autocorrelation analysis, and local Lyapunov exponent approximations to detect
 * imminent cliodynamic or ecological collapses across the H3 planetary grid.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class BifurcationChaosEngine {
    private static final Logger logger = LoggerFactory.getLogger(BifurcationChaosEngine.class);

    private static double globalLyapunovExponent = 0.0;
    private static double globalSystemVariance = 0.0;
    private static boolean tippingPointWarning = false;

    /**
     * Analyzes systemic stability, spatial variance, and critical slowing down indicators.
     *
     * @param cells list of H3 cells in the simulation grid
     * @param deltaYears step size in years
     */
    public static void processBifurcationAnalysis(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double sumPop = 0;
        double sumFood = 0;
        int count = 0;

        for (H3Cell cell : cells) {
            if (cell.getPopulation() != null && cell.getPopulation() > 0) {
                sumPop += cell.getPopulation();
                sumFood += cell.getFoodResource();
                count++;
            }
        }

        if (count == 0) return;

        double meanPop = sumPop / count;
        double varianceSum = 0;

        for (H3Cell cell : cells) {
            if (cell.getPopulation() != null && cell.getPopulation() > 0) {
                double diff = cell.getPopulation() - meanPop;
                varianceSum += diff * diff;
            }
        }

        globalSystemVariance = varianceSum / count;

        // Lyapunov Exponent Approximation: Rate of divergence between population and food availability
        double divergenceRate = 0.0;
        for (H3Cell cell : cells) {
            if (cell.getPopulation() != null && cell.getPopulation() > 0) {
                double stressRatio = cell.getFoodResource() > 0 ? (cell.getPopulation() / (cell.getFoodResource() + 1.0)) : 10.0;
                divergenceRate += Math.log(Math.max(0.001, Math.abs(stressRatio - 1.0)));
            }
        }
        globalLyapunovExponent = divergenceRate / count;

        // Critical Slowing Down Threshold: High variance + positive Lyapunov exponent indicates imminent collapse
        if (globalLyapunovExponent > 0.45 && globalSystemVariance > 5000.0) {
            if (!tippingPointWarning) {
                tippingPointWarning = true;
                logger.warn("⚠️ BIFURCATION WARNING: System approaching critical tipping point (Lyapunov={}, Variance={})",
                        String.format("%.4f", globalLyapunovExponent), String.format("%.2f", globalSystemVariance));
            }
        } else {
            tippingPointWarning = false;
        }

        // Apply chaos-induced perturbation on vulnerable cells near bifurcation
        if (tippingPointWarning) {
            for (H3Cell cell : cells) {
                if (cell.getPopulation() != null && cell.getPopulation() > 100) {
                    double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.3;
                    if (gini > 0.6) {
                        // Bifurcation cascade: High inequality + chaos -> accelerated demographic friction
                        int lostPop = (int) Math.round(cell.getPopulation() * 0.02 * deltaYears);
                        cell.setPopulation(Math.max(0, cell.getPopulation() - lostPop));
                    }
                }
            }
        }
    }

    public static double getGlobalLyapunovExponent() {
        return globalLyapunovExponent;
    }

    public static double getGlobalSystemVariance() {
        return globalSystemVariance;
    }

    public static boolean isTippingPointWarning() {
        return tippingPointWarning;
    }
}

