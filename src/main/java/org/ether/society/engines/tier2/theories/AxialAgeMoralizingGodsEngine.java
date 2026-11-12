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
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop < 200.0) return;

        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 10.0;
        double kpc = capital / pop;

        double scaleActivation = SmoothMath.sigmoid(pop, SESHAT_POPULATION_THRESHOLD, POPULATION_BANDWIDTH);
        double capitalActivation = SmoothMath.sigmoid(kpc, CAPITAL_PER_CAPITA_THRESHOLD, 20.0);
        double moralTrustIndex = scaleActivation * capitalActivation;

        if (moralTrustIndex > 0.01) {
            acc.dResourceCapital += (float) (capital * 0.015 * moralTrustIndex * dtYears);
            acc.dCommercialFluxVolume += (float) (2.5 * moralTrustIndex * dtYears);
            acc.dFluxPressure -= (float) (MAX_TRANSACTION_FRICTION_ABATEMENT * 0.1 * moralTrustIndex * dtYears);
        }
    }
}
