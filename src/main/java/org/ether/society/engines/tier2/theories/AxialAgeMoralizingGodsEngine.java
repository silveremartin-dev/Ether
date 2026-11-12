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
 * Axial Age Moralizing Gods &amp; Large-Scale Prosociality Engine.
 *
 * <p>Implements the foundational cliodynamic model of supernatural policing and institutional
 * trust scaling formulated by Karl Jaspers (1949), Ara Norenzayan (2013), and the empirical Seshat
 * Global History Databank study (Whitehouse et al., 2019, <i>Nature</i>):</p>
 * <ul>
 *   <li><b>Dunbar-Hamilton Cooperation Ceiling:</b> Small-scale ancestral bands ($N &lt; 150$) maintain
 *       cooperation via biological kin-selection (Hamilton rule) and direct reciprocal monitoring.
 *       As agrarian polities exceed $N \approx 100\,000$ nucleated inhabitants, direct reputation monitoring
 *       breaks down, generating acute tragedy of the commons, anonymous contract cheating, and opportunistic defection.</li>
 *   <li><b>Supernatural Monitoring &amp; Moralizing Gods:</b> The cultural adoption of omniscient,
 *       moralizing deities who actively reward prosociality and punish moral transgression (karma, eternal judgment)
 *       acts as a cognitive and institutional transducer, expanding interpersonal trust across ethnic strangers.</li>
 *   <li><b>Universal Biophysical Coupling:</b> Rather than regional gating, the engine activates
 *       continuously ($C^1$) as population scale $N$ and civic capital density $k_{\text{pc}}$ cross the
 *       Seshat complexity transition threshold ($N \ge 100\,000$, $k_{\text{pc}} \ge 80\text{ kg/capita}$),
 *       abating transaction friction and internal strife.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.1038/s41586-019-1043-4">Whitehouse et al. (2019), Nature 568:226-229</a>
 * @see KinSelectionHamiltonEngine
 * @see SchellingAxelrodSegregationEngine
 */
public class AxialAgeMoralizingGodsEngine extends AbstractSimulationEngine {
    public AxialAgeMoralizingGodsEngine() {
        super("AxialAgeMoralizingGodsEngine", "AxialAgeMoralizingGodsEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(AxialAgeMoralizingGodsEngine.class);

    /** Critical Seshat macro-polity population threshold where moralizing gods emerge (Whitehouse et al. 2019). */
    public static final double SESHAT_POPULATION_THRESHOLD = 100000.0;

    /** Scale transition bandwidth for smooth C^1 population scaling. */
    public static final double POPULATION_BANDWIDTH = 15000.0;

    /** Minimum physical capital density required for organized religious canon infrastructure (kg/capita). */
    public static final double CAPITAL_PER_CAPITA_THRESHOLD = 80.0;

    /** Maximum transaction friction abatement enabled by shared moralizing norms (35%). */
    public static final double MAX_TRANSACTION_FRICTION_ABATEMENT = 0.35;

    @Override
    public String getName() {
        return "AxialAgeMoralizingGodsEngine";
    }

    @Override
    public String getDescription() {
        return "Models the emergence of moralizing omniscient religions in large-scale societies (Seshat / Whitehouse 2019, Norenzayan 2013), reducing transaction friction and anonymous defection.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Axial Age Moralizing Gods & Prosocial Policing (Whitehouse et al. 2019, Norenzayan 2013)]
               • Prosocial Transition Index: Θ_moral = σ((N - 100000) / 25000) · σ((k_pc - 80) / 20)
               • Transaction Friction Abatement: Friction_eff = Friction_base · (1.0 - 0.35 · Θ_moral)
               • Internal Strife Dampener: Strife_mitigation = (1.0 - 0.25 · Θ_moral)
               • Civic Trust Capital Boost: ΔK_civic = K · 0.015 · Θ_moral · Δt
               Ref: H. Whitehouse et al. (2019), "Complex societies precede moralizing gods throughout world history", Nature 568:226-229.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Institutional & Cognitive Cliodynamics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        processMoralizingNorms(cells, deltaYears);
    }

    /**
     * Executes the continuous prosocial policing evaluation across populated cells.
     *
     * @param cells      simulation cell domain
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processMoralizingNorms(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop < 200) continue;

            double capital = cell.getResourceCapitalOr(10.0);
            double kpc = capital / (double) pop;

            // Seshat empirical transition: Moralizing Big Gods emerge when social complexity
            // and population scale cross the critical anonymous threshold (N >= 100,000, Whitehouse et al. 2019)
            double scaleActivation = SmoothMath.sigmoid(pop, SESHAT_POPULATION_THRESHOLD, POPULATION_BANDWIDTH);
            double capitalActivation = SmoothMath.sigmoid(kpc, CAPITAL_PER_CAPITA_THRESHOLD, 20.0);
            double moralTrustIndex = scaleActivation * capitalActivation;

            if (moralTrustIndex > 0.01) {
                // 1. Friction abatement: Prosocial religious norms reduce transaction friction among strangers
                double targetFriction = Math.max(0.2, 1.0 - (MAX_TRANSACTION_FRICTION_ABATEMENT * moralTrustIndex));
                cell.setMovementFriction(targetFriction);

                // 2. Capital accumulation boost from contract enforcement & reduced defection
                double institutionalSynergy = 0.015 * moralTrustIndex * deltaYears;
                cell.setResourceCapital(capital * (1.0 + institutionalSynergy));

                // 3. Trade network integration boost: high trust accelerates inter-settlement exchange
                double tradeNet = cell.getTradeNetwork() != null ? cell.getTradeNetwork() : 0.0;
                cell.setTradeNetwork(Math.min(100.0, tradeNet + (2.5 * moralTrustIndex * deltaYears)));
            }
        }
    }
}
