/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Nikolai Kondratiev &amp; Joseph Schumpeter Infrastructure Long-Wave &amp; Creative Destruction Engine.
 *
 * <p>Implements the secular 50-year macroeconomic infrastructure lifecycle and creative destruction
 * dynamics formulated by Nikolai Kondratiev (1925, <i>The Major Economic Cycles</i>),
 * Joseph Schumpeter (1939, <i>Business Cycles</i>), and Carlota Perez (2002):</p>
 * <ul>
 *   <li><b>Infrastructural Capital Durability:</b> Foundational macro-infrastructures (canals, steam railways,
 *       electrical grids, telecommunication backbones) possess a 45–60 year physical depreciation cycle.</li>
 *   <li><b>Installation &amp; Deployment Phases:</b> In the first half of the cycle, newly deployed infrastructure
 *       drastically reduces transport/communication friction, expanding capital productivity.</li>
 *   <li><b>Maturity, Exhaustion &amp; Creative Destruction:</b> As infrastructure reaches physical fatigue,
 *       diminishing returns set in, culminating in capital liquidation and creative destruction that clears
 *       obsolete fixed assets to make way for the next infrastructural surge.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.2307/1928486">Kondratiev, N. D. (1935), The Review of Economics and Statistics</a>
 * @see ArthurCombinatorialTechnologyEngine
 * @see KummelAyresExergyEngine
 */
public class KondratievSchumpeterLongWaveEngine extends AbstractSimulationEngine {
    public KondratievSchumpeterLongWaveEngine() {
        super("KondratievSchumpeterLongWaveEngine", "KondratievSchumpeterLongWaveEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(KondratievSchumpeterLongWaveEngine.class);

    /** Canonical wavelength of Kondratiev infrastructural wave (years). */
    public static final double KONDRATIEV_CYCLE_YEARS = 52.0;

    /** Minimum capital per capita threshold where complex infrastructure waves emerge (kg/cap). */
    public static final double INFRASTRUCTURE_KPC_FLOOR = 120.0;

    @Override
    public String getName() {
        return "KondratievSchumpeterLongWaveEngine";
    }

    @Override
    public String getDescription() {
        return "Models Kondratiev (1925) and Schumpeter (1939) 50-year long waves: physical infrastructure replacement cycles driving recurring installation, deployment, and creative destruction phases.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Kondratiev-Schumpeter Infrastructure Long Wave (Kondratiev 1925, Schumpeter 1939)]
               • Cycle Phase:        θ(t) = ((Year - 1750) mod 52) / 52.0
               • Wave Modulator:     W_k = sin(2π · θ(t)) · σ(k_pc - 120, 30)
               • Capital Momentum:   ΔK = (0.015 + 0.025 · W_k) · K · dt
               • Creative Shakeout:  If W_k < -0.40 -> Obsolete Capital Liquidation & Replacement Renewal
               • Friction Wave:      Friction = Base_Friction · (1.0 - 0.12 · W_k)
               Ref: N. D. Kondratiev (1925), "The Major Economic Cycles"; J. A. Schumpeter (1939), "Business Cycles".
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Macroeconomic Dynamics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processLongWaveDynamics(cells, 1850.0, deltaYears);
    }

    /**
     * Evaluates Kondratiev infrastructure long-wave modulation across cells.
     *
     * @param cells       simulation cells
     * @param currentYear astronomical Julian year of the simulation
     * @param deltaYears  time step duration in astronomical Julian years
     */
    public static void processLongWaveDynamics(List<H3Cell> cells, double currentYear, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        // Calculate phase angle within the 52-year infrastructural lifecycle
        double cyclePhase = ((currentYear % KONDRATIEV_CYCLE_YEARS) + KONDRATIEV_CYCLE_YEARS) % KONDRATIEV_CYCLE_YEARS;
        double normalizedAngle = (cyclePhase / KONDRATIEV_CYCLE_YEARS) * 2.0 * Math.PI;
        double waveSine = Math.sin(normalizedAngle);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 20) continue;

            double capital = cell.getResourceCapitalOr(0.0);
            double kpc = capital / (double) pop;
            if (kpc < INFRASTRUCTURE_KPC_FLOOR) continue;

            // Long-wave infrastructural dynamics emerge with physical capital density
            double waveActivation = SmoothMath.sigmoid(kpc, INFRASTRUCTURE_KPC_FLOOR, 30.0);
            double effectiveModulator = waveSine * waveActivation;

            // 1. Capital productivity modulation: installation & deployment surge vs maturity exhaustion
            double capitalGrowthRate = (0.015 + 0.025 * effectiveModulator) * deltaYears;
            double newCapital = Math.max(10.0, capital * (1.0 + capitalGrowthRate));
            cell.setResourceCapital(newCapital);

            // 2. Transport & Communication infrastructure friction: drops during deployment, rises during fatigue
            double currentFriction = cell.getMovementFrictionOr(1.0);
            double frictionDelta = -0.05 * effectiveModulator * deltaYears;
            cell.setMovementFriction(Math.clamp(currentFriction + frictionDelta, 0.20, 3.0));

            // 3. Creative Destruction Shakeout (Trough Phase, waveSine < -0.70)
            if (effectiveModulator < -0.50) {
                // Obsolete capital scrap clears space for next-generation technology
                double scrapRate = 0.015 * Math.abs(effectiveModulator) * deltaYears;
                cell.setResourceCapital(Math.max(10.0, newCapital * (1.0 - scrapRate)));

                // Metal recycling flux from dismantled legacy infrastructure
                double metal = cell.getResourceMetalOr(0.0);
                double recoveredMetal = (capital * scrapRate) * 0.15;
                cell.setResourceMetal(metal + recoveredMetal);
            }
        }
    }
}
