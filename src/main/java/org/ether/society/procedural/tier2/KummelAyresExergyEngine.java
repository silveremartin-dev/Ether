/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.tier2;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.PhysicalEnergyGridEngine;
import org.ether.society.procedural.ProceduralEnginePlugin;
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
 * @version 1.0.0-beta.1
 */
public class KummelAyresExergyEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(KummelAyresExergyEngine.class);

    public static final double ALPHA_K = 0.25; // Capital elasticity
    public static final double BETA_L = 0.25;  // Labor elasticity
    public static final double GAMMA_E = 0.50; // Useful exergy elasticity

    @Override
    public String getName() {
        return "Kümmel / Ayres-Warr Biophysical Exergy Economics";
    }

    @Override
    public String getDescription() {
        return "Thermodynamic macroeconomic production function Y = A · K^α · L^β · E_useful^γ where useful work/exergy is the prime driver of industrial output.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Kümmel & Ayres-Warr Useful Exergy Production Function]
               • Output Function:  Y_i = A_i · K_i^0.25 · L_i^0.25 · (E_useful,i)^0.50
               • Useful Exergy:    E_useful,i = E_primary,i · η_thermodynamic(t)
               • Constraint:       α + β + γ = 1.0 (Euler Homogeneity)
               Units: Y [$/an], K [Joules capital matériel], L [heures-homme], E_useful [Joules utiles / Watt-heures]
               Ref: R. Kümmel (2011) "The Second Law of Economics", Ayres & Warr (2009) "The Economic Growth Engine"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Biophysical Economics";
    }

    @Override
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

            // Kümmel-Ayres production function Y = A * K^α * L^β * E_useful^γ
            double tfp = 1.0;
            double economicOutput = tfp * Math.pow(capital, ALPHA_K) * Math.pow(labor, BETA_L) * Math.pow(usefulExergy, GAMMA_E);

            // Reinvest fraction of output into physical infrastructure capital
            double capitalReinvestment = economicOutput * 0.05 * deltaYears;
            cell.setResourceCapital(capital + capitalReinvestment);
        }
    }
}

