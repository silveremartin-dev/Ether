/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.ether.society.engines.tier1.PhysicalEnergyGridEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * W. Brian Arthur's Combinatorial Technology Evolution Engine.
 *
 * <p>Models technology as an evolving metabolic web of combinations (Arthur 2009, <i>The Nature of
 * Technology</i>). Novel techniques emerge from existing combinations, compounding into specialized
 * physical machinery, infrastructure, and tools (\(\Delta K\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ArthurCombinatorialTechnologyEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(ArthurCombinatorialTechnologyEngine.class);

    @Override
    public String getName() {
        return org.ether.society.i18n.I18n.getOrDefault(
                "engine.arthur_combinatorial.name",
                "Arthur Combinatorial Capital Recombination Engine");
    }

    @Override
    public String getDescription() {
        return org.ether.society.i18n.I18n.getOrDefault(
                "engine.arthur_combinatorial.desc",
                "Autocatalytic physical tooling and machinery accumulation driven by combinatorial diversity (1 + k_pc/k_ref)^alpha and cooperative network scale ln(1 + N/N_0).");
    }

    @Override
    public String getEquationsTooltip() {
        return org.ether.society.i18n.I18n.getOrDefault(
                "engine.arthur_combinatorial.tooltip",
                """
                [Arthur Combinatorial Physical Capital Evolution Model (2009)]
                • Capital Dynamics:     dK/dt = μ_comb · N · (1 + k_pc/k_ref)^α · ln(1 + N/N_0) / (1 + k_pc/k_max) - δ_obs · K
                • Physical Constants:   k_ref = 120 kg/cap, α = 1.35, N_0 = 100 persons, k_max = 50,000 kg/cap, δ_obs = 0.008 yr^-1
                • Units:                K [kg physical capital], k_pc = K/N [kg/capita], N [individuals]
                Ref: W. B. Arthur (2009) "The Nature of Technology"; M. Kremer (1993, QJE)
                """);
    }

    /** Reference agrarian iron/masonry tool-set mass \(k_{\text{ref}} = 120.0\,\text{kg/capita}\) (Krausmann et al. 2017). */
    public static final double REFERENCE_TOOLSET_MASS_KG_PC = 120.0;

    /** Superlinear combinatorial synergy exponent \(\alpha = 1.35\) (Arthur &amp; Polak 2006). */
    public static final double COMBINATORIAL_SYNERGY_EXPONENT = 1.35;

    /** Cooperative division-of-labor network scale \(N_0 = 100.0\,\text{persons}\) (Henrich 2004). */
    public static final double COOPERATIVE_NETWORK_SCALE_POP = 100.0;

    /** Thermodynamic saturation ceiling of physical capital \(k_{\max} = 50\,000.0\,\text{kg/capita}\). */
    public static final double THERMODYNAMIC_CAPITAL_SATURATION_KG_PC = 50000.0;

    /** Base physical tool synthesis coefficient \(\mu_{\text{comb}} = 0.045\,\text{kg/(person}\cdot\text{yr)}\). */
    public static final double BASE_SYNTHESIS_RATE = 0.045;

    /** Structural entropy and obsolescence wear rate \(\delta_{\text{obs}} = 0.008\,\text{yr}^{-1}\). */
    public static final double OBSOLESCENCE_DECAY_RATE = 0.008;

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = SmoothMath.softplus(cell.getResourceCapitalOr(10.0), 0.0, 10.0);
            double kPerCapita = capital / pop;

            // Single C^infinity combinatorial capital differential equation (W. Brian Arthur 2009; Kremer 1993; Garrett 2011):
            // dK/dt = mu_comb * N * (1 + k_pc / k_ref)^alpha * ln(1 + N / N_0) / (1 + (k_pc / k_energized)^2) - delta_obs * K
            double powerWatts = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double kEnergized = Math.min(THERMODYNAMIC_CAPITAL_SATURATION_KG_PC,
                    Math.max(REFERENCE_TOOLSET_MASS_KG_PC,
                            powerWatts / H3CellPhysicsAndStateHelper.GARRETT_CAPITAL_MAINTENANCE_WATTS_PER_UNIT));

            double combinatorialDiversity = Math.pow(1.0 + kPerCapita / REFERENCE_TOOLSET_MASS_KG_PC, COMBINATORIAL_SYNERGY_EXPONENT);
            double cooperativeNetwork = Math.log1p(pop / COOPERATIVE_NETWORK_SCALE_POP);
            double ratioToCeiling = kPerCapita / kEnergized;
            double saturationDamping = 1.0 / (1.0 + ratioToCeiling * ratioToCeiling);

            double grossSynthesis = BASE_SYNTHESIS_RATE * pop * combinatorialDiversity * cooperativeNetwork * saturationDamping;
            double netDeltaK = (grossSynthesis - OBSOLESCENCE_DECAY_RATE * capital) * deltaYears;
            cell.setResourceCapital(SmoothMath.softplus(capital + netDeltaK, 0.0, 10.0));
        }
    }
}
