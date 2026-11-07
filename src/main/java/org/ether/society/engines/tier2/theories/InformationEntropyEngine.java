/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.PhysicalEnergyGridEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Shannon Information Entropy &amp; Command Bandwidth Engine (Shannon, 1948; Brillouin, 1956).
 *
 * <p>Replaces abstract cultural drift heuristics with physical Shannon-Hartley Information Theory
 * anchored to per-capita electromagnetic/mechanical power dissipation $p = P_{\text{watts}}/N$ and
 * telecommunication infrastructure stock $k_{\text{pc}} = K/N$:</p>
 * <ul>
 *   <li><b>Shannon-Hartley Channel Capacity</b>: $C = B(p, k_{\text{pc}}) \cdot \log_2(1 + \text{SNR})$, measuring
 *   maximum physical information transmission rate ($\text{bits/s}$) across terrain friction.</li>
 *   <li><b>Brillouin Physical Carrier Bandwidth</b>: Carrier frequency bandwidth $B$ scales with non-somatic
 *   exergy dissipation $\Delta p = \max(0, p - p_{\text{somatic}})$ and physical network capital $k_{\text{pc}}$
 *   (from acoustic/courier relay $\sim 1\,\text{Hz}$ to telegraph/optical fiber $\sim 10^5\,\text{Hz}$).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class InformationEntropyEngine {
    private static final Logger logger = LoggerFactory.getLogger(InformationEntropyEngine.class);

    /**
     * Basal somatic power $p_{\text{somatic}} = 348.0\,\text{W/capita}$ (human metabolism + domestic hearth).
     * Electromagnetic and mechanical signal relays require surplus exergy above this biological floor.
     */
    public static final double SOMATIC_POWER_FLOOR_WATTS = 348.0;

    /**
     * Calculates Shannon Channel Capacity ($\text{bits/sec}$) across terrain friction.
     *
     * @param bandwidthHz Frequency bandwidth in $\text{Hz}$
     * @param signalToNoiseRatio Signal-to-noise ratio (dimensionless linear power ratio)
     * @return Channel capacity $C$ in $\text{bits/second}$
     */
    public static double calculateShannonCapacityBitsPerSec(double bandwidthHz, double signalToNoiseRatio) {
        if (signalToNoiseRatio <= 0.0) return 0.0;
        return bandwidthHz * (Math.log(1.0 + signalToNoiseRatio) / Math.log(2.0));
    }

    /**
     * Executes one information channel capacity update across cells using physical power and capital observables.
     *
     * @param cells list of spatial cells
     */
    public static void processInformationEntropy(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = Math.max(1, cell.getPopulationOr(1));
            double kPerCapita = cell.getResourceCapitalOr(0.0) / pop;
            double powerWattsPerCapita = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double friction = Math.max(0.1, cell.getMovementFrictionOr(1.0));

            // Physical carrier bandwidth B (Hz) scales continuously with non-somatic exergy flux and built relay capital
            // (Brillouin 1956, "Science and Information Theory"):
            // B_carrier(k_pc, p) = B_0 + alpha_k * k_pc + alpha_p * (p_exo)^2
            double surplusPower = org.ether.society.core.math.SmoothMath.softplus(
                    powerWattsPerCapita, SOMATIC_POWER_FLOOR_WATTS, 0.5);
            double bandwidthHz = 1.0 + 0.05 * kPerCapita + 0.002 * surplusPower * surplusPower;
            double snr = 10.0 / org.ether.society.core.math.SmoothMath.softplus(friction, 0.0, 10.0);

            double channelCapacityBitsPerSec = calculateShannonCapacityBitsPerSec(bandwidthHz, snr);

            // Continuous Michaelis-Menten linguistic homogenization driven by Shannon channel capacity C (bits/s):
            // Zero if-then-else threshold; dialectal drift is smoothly damped as C approaches C_1/2 = 100 bits/s.
            double drift = cell.getLinguisticDriftOr(0.0);
            double homogenizationRate = 0.015 * (channelCapacityBitsPerSec / (channelCapacityBitsPerSec + 100.0));
            cell.setLinguisticDrift(org.ether.society.core.math.SmoothMath.softplus(drift - homogenizationRate, 0.0, 200.0));
        }
    }
}


