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
 * Biophysical Exergy Economics Engine (KÃ¼mmel, 1982 / Ayres & Warr, 2009).
 *
 * <p>Models macroeconomic production with thermodynamic exergy as an essential physical factor:</p>
 * <pre>
 *   Y_i = A_i Â· K_i^Î± Â· L_i^Î² Â· (E_useful,i)^Î³
 * </pre>
 * where:
 * <ul>
 *   <li><b>Î± + Î² + Î³ = 1</b> (Constant returns to scale).</li>
 *   <li><b>Î³ â‰ˆ 0.45 - 0.50</b>: Output elasticity of useful exergy (much higher than neoclassical cost share ~5%).</li>
 *   <li><b>E_useful = E_primary Â· Î·_thermo(t)</b>: Primary energy multiplied by thermodynamic conversion efficiency.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class KummelAyresExergyEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(KummelAyresExergyEngine.class);

    public static final double ALPHA_K = 0.25; // Capital elasticity
    public static final double BETA_L = 0.25;  // Labor elasticity
    public static final double GAMMA_E = 0.50; // Useful exergy elasticity

    @Override
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code KummelAyresExergyEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return "KÃ¼mmel / Ayres-Warr Biophysical Exergy Economics";
    }

    @Override
    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code KummelAyresExergyEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return "Thermodynamic macroeconomic production function Y = A Â· K^Î± Â· L^Î² Â· E_useful^Î³ where useful work/exergy is the prime driver of industrial output.";
    }

    @Override
    /*
     * Get equations tooltip.
     * Enforces physical invariants and updates associated state variables within {@code KummelAyresExergyEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getEquationsTooltip() {
        return """
               [KÃ¼mmel & Ayres-Warr Useful Exergy Production Function]
               â€¢ Output Function:  Y_i = A_i Â· K_i^0.25 Â· L_i^0.25 Â· (E_useful,i)^0.50
               â€¢ Useful Exergy:    E_useful,i = E_primary,i Â· Î·_thermodynamic(t)
               â€¢ Constraint:       Î± + Î² + Î³ = 1.0 (Euler Homogeneity)
               Units: Y [$/an], K [Joules capital matÃ©riel], L [heures-homme], E_useful [Joules utiles / Watt-heures]
               Ref: R. KÃ¼mmel (2011) "The Second Law of Economics", Ayres & Warr (2009) "The Economic Growth Engine"
               """;
    }

    @Override
    /*
     * Get category.
     * Enforces physical invariants and updates associated state variables within {@code KummelAyresExergyEngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getCategory() {
        return "Tier 2: Biophysical Economics";
    }

    @Override
    /*
     * Process.
     * Enforces physical invariants and updates associated state variables within {@code KummelAyresExergyEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double capital = cell.getResourceCapital() != null ? Math.max(1.0, cell.getResourceCapital()) : 10.0;
            double labor = cell.getResourceWork() != null ? Math.max(1.0, cell.getResourceWork()) : (pop * 0.5);

            // Calculate primary exergy from mechanical/electrical grid engine
            double powerPerCapitaWatts = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double totalPrimaryWatts = powerPerCapitaWatts * pop;

            // Thermodynamic conversion efficiency: ranges from 5% (muscular/fire) to 40% (modern thermal cycle)
            double techLevel = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            double etaThermo = Math.min(0.45, 0.05 + 0.05 * Math.log(1.0 + techLevel));
            double usefulExergy = Math.max(1.0, totalPrimaryWatts * etaThermo);

            // KÃ¼mmel-Ayres production function Y = A * K^Î± * L^Î² * E_useful^Î³
            double tfp = 1.0;
            double economicOutput = tfp * Math.pow(capital, ALPHA_K) * Math.pow(labor, BETA_L) * Math.pow(usefulExergy, GAMMA_E);

            // Reinvest fraction of output into physical infrastructure capital
            double capitalReinvestment = economicOutput * 0.05 * deltaYears;
            cell.setResourceCapital(capital + capitalReinvestment);
        }
    }
}



