/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Gerhard Lenski &amp; Branko Milanovic Inequality Possibility Frontier (IPF)
 * coupled with Thomas Piketty (2014) $r > g$ dynamic capital extraction and
 * Walter Scheidel (2017) leveling shocks.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class LenskiPureEngine extends AbstractSimulationEngine {
    public LenskiPureEngine() {
        super("LenskiPureEngine", "LenskiPureEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processLenskiPikettyInequality(buf, dtYears);
    }

    private static final Logger logger = LoggerFactory.getLogger(LenskiPureEngine.class);

    /**
     * Evaluates dynamic Piketty-Milanovic-Scheidel inequality dynamics over WorldBuffer.
     *
     * @param buffer contiguous world buffer
     * @param deltaYears time step fraction in years
     */
    public static void processLenskiPikettyInequality(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0.0) return;
        int n = buffer.getCapacity();
        float[] popArr = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] giniArr = buffer.getGiniIndex();
        float[] compArr = buffer.getInstitutionalComplexity();

        for (int i = 0; i < n; i++) {
            float p = popArr[i];
            if (p <= 0.001f) continue;
            float capital = (capArr != null) ? capArr[i] : 10.0f;
            float kPerCap = capital / p;

            // Inequality Possibility Frontier (Milanovic et al. 2011): G_max = 1 - s_min / y
            double sMin = 15.0; // minimum physical maintenance capital
            double gMax = Math.clamp(1.0 - (sMin / Math.max(sMin + 1.0, (double) kPerCap)), 0.0, 0.85);

            // Piketty (2014) r - g dynamic extraction: r = 0.05, g ~ capital growth
            double complexity = (compArr != null) ? compArr[i] : 0.0;
            double rMinusG = 0.05 - 0.02 * Math.tanh(kPerCap / 1000.0);
            double muExtract = 0.20 + 0.70 * SmoothMath.sigmoid(rMinusG, 0.0, 0.02) * SmoothMath.sigmoid(complexity, 0.25, 0.10);
            double targetGini = Math.clamp(0.20 + muExtract * gMax, 0.20, 0.75);

            if (giniArr != null) {
                float currentGini = giniArr[i] > 0.0f ? giniArr[i] : 0.35f;
                giniArr[i] = (float) Math.clamp(currentGini + 0.04 * (targetGini - currentGini) * deltaYears, 0.18, 0.80);
            }
        }
    }

    /**
     * Computes theoretical Lenski-Milanovic biophysical Gini coefficient directly from per-capita
     * physical capital stock \(k_{\text{pc}} = K/N\) (\(\text{kg/capita}\)).
     *
     * @param capitalPerCapitaKg per-capita physical capital stock \(k_{\text{pc}} = K/N\) in \(\text{kg/capita}\)
     * @return theoretical Gini index in \([0.18, 0.75]\)
     */
    public static double calculateLenskiGini(double capitalPerCapitaKg) {
        double kpc = org.ether.society.core.math.SmoothMath.softplus(capitalPerCapitaKg, 0.0, 10.0);
        double gForager = 0.25;
        double logRatio = Math.log((kpc + 10.0) / (280.0 + 10.0));
        double agrarianSurge = 0.40 * Math.exp(-(logRatio * logRatio) / (2.0 * 1.25 * 1.25));
        double industrialShift = 0.13 * (kpc * kpc) / (kpc * kpc + 2500.0 * 2500.0);
        return Math.clamp(gForager + agrarianSurge + industrialShift, 0.18, 0.70);
    }

}


