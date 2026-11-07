/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.PhysicalEnergyGridEngine;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Biophysical Exergy Economics Engine (Kümmel, 1982 / Ayres & Warr, 2009).
 *
 * <p>Models macroeconomic production with thermodynamic exergy as an essential physical factor:</p>
 * <pre>
 *   Y_i = A_i · K_i^α · L_i^β · (E_useful,i)^γ
 * </pre>
 * where:
 * <ul>
 *   <li><b>α + β + γ = 1</b> (Constant returns to scale).</li>
 *   <li><b>γ ≈ 0.45 - 0.50</b>: Output elasticity of useful exergy (much higher than neoclassical cost share ~5%).</li>
 *   <li><b>E_useful = E_primary · η_thermo(t)</b>: Primary energy multiplied by thermodynamic conversion efficiency.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class KummelAyresExergyEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(KummelAyresExergyEngine.class);

    public static final double ALPHA_K = 0.25; // Capital elasticity
    public static final double BETA_L = 0.25;  // Labor elasticity
    public static final double GAMMA_E = 0.50; // Useful exergy elasticity

    /**
     * Returns the localized display name of the Kümmel-Ayres-Warr biophysical exergy engine.
     *
     * @return localized engine name
     */
    @Override
    public String getName() {
        return org.ether.society.i18n.I18n.getOrDefault(
                "engine.kummel_ayres.name",
                "Kümmel / Ayres-Warr Biophysical Exergy Economics");
    }

    /**
     * Returns the localized scientific description of the engine.
     *
     * @return localized engine description
     */
    @Override
    public String getDescription() {
        return org.ether.society.i18n.I18n.getOrDefault(
                "engine.kummel_ayres.desc",
                "Thermodynamic macroeconomic production function Y = A · K^α · L^β · E_useful^γ where useful work/exergy is the prime driver of industrial output.");
    }

    /**
     * Returns the localized mathematical tooltip detailing equations, physical units, and references.
     *
     * @return localized equations tooltip
     */
    @Override
    public String getEquationsTooltip() {
        return org.ether.society.i18n.I18n.getOrDefault(
                "engine.kummel_ayres.tooltip",
                """
                [Kümmel & Ayres-Warr Useful Exergy Production Function]
                • Output Function:        Y_i = A_i · K_i^0.25 · L_i^0.25 · (E_useful,i)^0.50
                • Transducer Efficiency:  η_exergy(k_pc) = 0.05 + 0.40 · tanh(k_pc / 2500 kg/cap)
                • Useful Exergy Flux:     E_useful,i = P_primary,i · η_exergy(k_pc)
                • Euler Homogeneity:      α + β + γ = 1.0
                Units: Y [Int$/yr], K [kg physical capital], L [person-hours], E_useful [Watts]
                Ref: R. Kümmel (2011) "The Second Law of Economics"; Ayres & Warr (2009) "The Economic Growth Engine"
                """);
    }

    /**
     * Returns the engine ontological tier and domain classification.
     *
     * @return localized category label
     */
    @Override
    public String getCategory() {
        return "Tier 2: Biophysical Economics";
    }

    /**
     * Integrates thermodynamic useful exergy production and capital formation over \(\Delta t\) years.
     *
     * @param cells      contiguous list of active H3 hexagonal cells
     * @param deltaYears integration time step \(\Delta t\) in Julian years
     */
    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapital() != null ? Math.max(1.0, cell.getResourceCapital()) : 10.0;
            double labor = cell.getResourceWork() != null ? Math.max(1.0, cell.getResourceWork()) : (pop * 0.5);

            // Calculate primary exergy from mechanical/electrical grid engine
            double powerPerCapitaWatts = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double totalPrimaryWatts = powerPerCapitaWatts * pop;

            // Thermodynamic conversion efficiency eta_exergy(k_pc) (Ayres & Warr 2005; Kümmel 2011):
            // Single C^infinity hyperbolic tangent saturation from somatic/open-fire baseline (eta_0 = 0.05)
            // to Carnot/combined-cycle limit (eta_max = 0.45) with characteristic machinery mass k_transducer = 2500 kg/cap.
            double kPerCapita = capital / pop;
            double etaThermo = 0.05 + (0.45 - 0.05) * Math.tanh(kPerCapita / 2500.0);
            double usefulExergy = org.ether.society.core.math.SmoothMath.softplus(totalPrimaryWatts * etaThermo, 1.0, 5.0);

            // Kümmel-Ayres production function Y = A * K^α * L^β * E_useful^γ
            double tfp = 1.0;
            double economicOutput = tfp * Math.pow(capital, ALPHA_K) * Math.pow(labor, BETA_L) * Math.pow(usefulExergy, GAMMA_E);

            // Reinvest fraction of output into physical infrastructure capital with physical depreciation (Ayres & Warr 2009)
            // dK/dt = s · Y - δ · K (where savings rate s = 0.05, depreciation rate δ = 0.04/yr)
            double capitalReinvestment = economicOutput * 0.05 * deltaYears;
            double capitalDepreciation = capital * 0.04 * deltaYears;
            cell.setResourceCapital(Math.max(1.0, capital + capitalReinvestment - capitalDepreciation));
        }
    }
}



