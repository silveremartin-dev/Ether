/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.math.SmoothMath;
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
    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        processKummelAyresExergy(buffer, deltaYears);
    }

    /**
     * Executes pure Data-Oriented Design (DoD) second-law Carnot exergy production over WorldBuffer.
     *
     * @param buffer contiguous world buffer
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processKummelAyresExergy(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0.0) return;
        int n = buffer.getCapacity();
        float[] popArr = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] coalArr = buffer.getResourceCoal();
        float[] oilArr = buffer.getResourceOil();
        float[] gasArr = buffer.getResourceGas();
        float[] tempArr = buffer.getTemperature();

        for (int i = 0; i < n; i++) {
            float p = popArr[i];
            if (p <= 0.001f) continue;
            float capital = (capArr != null) ? Math.max(1.0f, capArr[i]) : 10.0f;
            float labor = p * 0.5f;

            double tColdK = (tempArr != null) ? (tempArr[i] + 273.15) : 288.15;
            double coal = (coalArr != null) ? coalArr[i] : 0.0;
            double hydrocarbon = ((oilArr != null ? oilArr[i] : 0.0) + (gasArr != null ? gasArr[i] : 0.0));

            // Carnot second-law thermodynamics: effective prime-mover combustion flame temperature
            double deltaTFlame = 450.0
                    + 700.0 * SmoothMath.sigmoid(coal, 100.0, 30.0)
                    + 500.0 * SmoothMath.sigmoid(hydrocarbon, 100.0, 30.0);
            double tHotK = tColdK + deltaTFlame;
            double etaCarnot = 1.0 - (tColdK / tHotK);

            double kPerCap = capital / p;
            double recoveryFactor = 0.08 + (0.58 - 0.08) * SmoothMath.sigmoid(kPerCap, 1200.0, 400.0);
            double etaThermo = Math.clamp(etaCarnot * recoveryFactor, 0.05, 0.50);

            double primaryWatts = 100.0 * p + 50.0 * capital;
            double usefulExergy = SmoothMath.softplus(primaryWatts * etaThermo, 1.0, 5.0);

            double tfp = 1.0;
            double economicOutput = tfp * Math.pow(capital, ALPHA_K) * Math.pow(labor, BETA_L) * Math.pow(usefulExergy, GAMMA_E);

            double capitalReinvestment = economicOutput * 0.05 * deltaYears;
            double capitalDepreciation = capital * 0.04 * deltaYears;
            if (capArr != null) {
                capArr[i] = (float) Math.max(1.0, capital + capitalReinvestment - capitalDepreciation);
            }
        }
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapital() != null ? Math.max(1.0, cell.getResourceCapital()) : 10.0;
            double labor = cell.getResourceWork() != null ? Math.max(1.0, cell.getResourceWork()) : (pop * 0.5);

            double powerPerCapitaWatts = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(cell);
            double totalPrimaryWatts = powerPerCapitaWatts * pop;

            double tColdK = (cell.getTemperatureOr(15.0f)) + 273.15;
            double coal = cell.getResourceCoalOr(0.0);
            double oil = cell.getResourceOilOr(0.0);
            double deltaTFlame = 450.0
                    + 700.0 * SmoothMath.sigmoid(coal, 100.0, 30.0)
                    + 500.0 * SmoothMath.sigmoid(oil, 100.0, 30.0);
            double tHotK = tColdK + deltaTFlame;
            double etaCarnot = 1.0 - (tColdK / tHotK);

            double kPerCapita = capital / pop;
            double recoveryFactor = 0.08 + (0.58 - 0.08) * SmoothMath.sigmoid(kPerCapita, 1200.0, 400.0);
            double etaThermo = Math.clamp(etaCarnot * recoveryFactor, 0.05, 0.50);
            double usefulExergy = SmoothMath.softplus(totalPrimaryWatts * etaThermo, 1.0, 5.0);

            double tfp = 1.0;
            double economicOutput = tfp * Math.pow(capital, ALPHA_K) * Math.pow(labor, BETA_L) * Math.pow(usefulExergy, GAMMA_E);

            double capitalReinvestment = economicOutput * 0.05 * deltaYears;
            double capitalDepreciation = capital * 0.04 * deltaYears;
            cell.setResourceCapital(Math.max(1.0, capital + capitalReinvestment - capitalDepreciation));
        }
    }
}



