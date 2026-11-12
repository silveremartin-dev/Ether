/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
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
 * @version 1.0.0-beta.3
 */
public class BifurcationChaosEngine extends AbstractSimulationEngine {
    public BifurcationChaosEngine() {
        super("BifurcationChaosEngine", "BifurcationChaosEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(BifurcationChaosEngine.class);

    /** Internal state: global lyapunov exponent (double). */
    private static double globalLyapunovExponent = 0.0;
    /** Internal state: global system variance (double). */
    private static double globalSystemVariance = 0.0;
    /** Internal state: tipping point warning (boolean). */
    private static boolean tippingPointWarning = false;

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() == 0) return;
        int n = buf.getCapacity();
        float[] popArr = buf.getBiomassHuman();
        float[] foodArr = buf.getFoodResource();
        float[] giniArr = buf.getGiniIndex();

        double sumPop = 0.0;
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (popArr[i] > 0.0f) {
                sumPop += popArr[i];
                count++;
            }
        }
        if (count == 0) return;

        double meanPop = sumPop / count;
        double varianceSum = 0.0;
        double divergenceRate = 0.0;
        for (int i = 0; i < n; i++) {
            float pop = popArr[i];
            if (pop > 0.0f) {
                double diff = pop - meanPop;
                varianceSum += diff * diff;
                double food = foodArr[i];
                double stressRatio = food > 0.0 ? (pop / (food + 1.0)) : 10.0;
                divergenceRate += Math.log(Math.max(0.001, Math.abs(stressRatio - 1.0)));
            }
        }
        globalSystemVariance = varianceSum / count;
        globalLyapunovExponent = divergenceRate / count;

        if (globalLyapunovExponent > 0.45 && globalSystemVariance > 5000.0) {
            if (!tippingPointWarning) {
                tippingPointWarning = true;
                logger.warn("[WARNING] BIFURCATION: System approaching critical tipping point (Lyapunov={}, Variance={})",
                        String.format("%.4f", globalLyapunovExponent), String.format("%.2f", globalSystemVariance));
            }
            for (int i = 0; i < n; i++) {
                float pop = popArr[i];
                if (pop > 100.0f && giniArr[i] > 0.6f) {
                    popArr[i] = (float) Math.max(0.0, pop * (1.0 - 0.02 * dtYears));
                }
            }
        } else {
            tippingPointWarning = false;
        }
    }

    /**
     * Get global lyapunov exponent.
     *
     * @return the resulting computation or state reference
     */
    public static double getGlobalLyapunovExponent() {
        return globalLyapunovExponent;
    }

    /**
     * Get global system variance.
     *
     * @return the resulting computation or state reference
     */
    public static double getGlobalSystemVariance() {
        return globalSystemVariance;
    }

    /**
     * Is tipping point warning.
     *
     * @return the resulting computation or state reference
     */
    public static boolean isTippingPointWarning() {
        return tippingPointWarning;
    }

    public static void resetState() {
        globalLyapunovExponent = 0.0;
        globalSystemVariance = 0.0;
        tippingPointWarning = false;
    }
}


