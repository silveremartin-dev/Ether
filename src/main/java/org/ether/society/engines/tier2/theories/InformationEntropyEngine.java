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
 * @version 1.0.0-beta.3
 */
public class InformationEntropyEngine extends AbstractSimulationEngine {
    public InformationEntropyEngine() {
        super("InformationEntropyEngine", "InformationEntropyEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

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

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double pop = Math.max(1.0, buffer.getBiomassHuman()[i]);
        double kPerCapita = buffer.getResourceCapital()[i] / pop;
        double totalWatts = buffer.getEnergyFire()[i] + buffer.getEnergyWind()[i] + buffer.getEnergySolar()[i]
                + (buffer.getResourceCoal()[i] * 24.0) + (buffer.getResourceOil()[i] * 42.0);
        double powerWattsPerCapita = (totalWatts > 0.0) ? (totalWatts / pop) : SOMATIC_POWER_FLOOR_WATTS;
        double friction = 1.0;

        double surplusPower = org.ether.society.core.math.SmoothMath.softplus(
                powerWattsPerCapita, SOMATIC_POWER_FLOOR_WATTS, 0.5);
        double bandwidthHz = 1.0 + 0.05 * kPerCapita + 0.002 * surplusPower * surplusPower;
        double snr = 10.0 / org.ether.society.core.math.SmoothMath.softplus(friction, 0.0, 10.0);

        double channelCapacityBitsPerSec = calculateShannonCapacityBitsPerSec(bandwidthHz, snr);
        double homogenizationRate = 0.015 * (channelCapacityBitsPerSec / (channelCapacityBitsPerSec + 100.0));
        acc.dInstitutionalComplexity += (float) (homogenizationRate * dtYears);
    }
}


