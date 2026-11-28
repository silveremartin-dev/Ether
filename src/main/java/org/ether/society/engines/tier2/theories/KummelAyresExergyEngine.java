/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.i18n.I18n;
import org.ether.society.model.PhysicalUnitsHelper;
import org.ether.society.model.Scenario;

/**
 * Biophysical Exergy Economics Engine (Kümmel 1982, 2011; Ayres &amp; Warr 2005, 2009).
 *
 * <p>Implements the constant-returns-to-scale thermodynamic macroeconomic production function
 * where useful exergy (physical work \(E_{\text{useful}} = \eta_{\text{exergy}} E_{\text{primary}}\))
 * is an essential factor alongside capital \(K\) and labor \(L\):</p>
 * <p>$$Y_i = A_0 \, K_i^{\alpha} \, L_i^{\beta} \, E_{\text{useful},i}^{\gamma}, \qquad \alpha + \beta + \gamma = 1$$</p>
 * <p>with Carnot-bounded Second-Law exergy conversion efficiency
 * \(\eta_{\text{exergy}} = \varepsilon_{\text{II}} \left(1 - \frac{T_C}{T_H}\right)\).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class KummelAyresExergyEngine extends AbstractSimulationEngine {

    /** Output elasticity of physical capital (\(\alpha = 0.25\), Ayres &amp; Warr 2009). */
    public static final double ALPHA_K = 0.25;

    /** Output elasticity of human labor (\(\beta = 0.25\), Ayres &amp; Warr 2009). */
    public static final double BETA_L = 0.25;

    /** Output elasticity of useful exergy/work (\(\gamma = 0.50\), Kümmel 2011; Ayres &amp; Warr 2009). */
    public static final double GAMMA_E = 0.50;

    /**
     * Constructs the Kümmel / Ayres-Warr biophysical exergy production engine.
     */
    public KummelAyresExergyEngine() {
        super("KummelAyresExergyEngine", "KummelAyresExergyEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public String id() {
        return "KummelAyresExergyEngine";
    }

    @Override
    public String name() {
        return getName();
    }

    @Override
    public EngineTier tier() {
        return EngineTier.TIER_2_CLIODYNAMIC;
    }

    @Override
    public boolean isEnabled(Scenario scenario) {
        return scenario != null && scenario.getTypeBEngineStates() != null
                && Boolean.TRUE.equals(scenario.getTypeBEngineStates().get(id()));
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || dtYears <= 0.0 || buffer.getHumanPopulation() == null) return;
        float p = buffer.getHumanPopulation()[i];
        if (p <= 0.001f) return;

        double capital = (buffer.getResourceCapital() != null) ? Math.max(1.0, buffer.getResourceCapital()[i]) : 10.0;
        double labor = p * 0.5;

        double tColdK = (buffer.getTemperature() != null)
                ? Math.clamp(PhysicalUnitsHelper.celsiusToKelvin(buffer.getTemperature()[i]), 220.0, 330.0)
                : 288.15;

        // Enthalpy-weighted adiabatic combustion reservoir temperature T_H [K] (biomass ~900 K, coal ~1450 K, hydrocarbons ~1850 K)
        double coalGJ = (buffer.getResourceCoal() != null ? Math.max(0.0, buffer.getResourceCoal()[i]) : 0.0) * 24.0;
        double oilGasGJ = (buffer.getResourceOil() != null ? Math.max(0.0, buffer.getResourceOil()[i]) * 42.0 : 0.0)
                + (buffer.getResourceGas() != null ? Math.max(0.0, buffer.getResourceGas()[i]) * 50.0 : 0.0);
        double bioGJ = Math.max(1.0, (buffer.getBiomassNatural() != null ? Math.max(0.0, buffer.getBiomassNatural()[i]) : 1.0) * 15.0);
        double totalEnthalpyPool = bioGJ + coalGJ + oilGasGJ;
        double tHotK = Math.max(tColdK + 120.0,
                (900.0 * bioGJ + 1450.0 * coalGJ + 1850.0 * oilGasGJ) / totalEnthalpyPool);

        // Carnot limit * Second-Law exergetic effectiveness epsilon_II (Ayres & Warr 2009)
        double etaCarnot = 1.0 - (tColdK / tHotK);
        double kPerCap = capital / p;
        double secondLawEffectiveness = 0.10 + 0.48 * (kPerCap / (kPerCap + 1200.0));
        double etaExergy = Math.clamp(etaCarnot * secondLawEffectiveness, 0.05, 0.50);

        double primaryWatts = p * Math.max(85.0, buffer.computeCellPowerWattsPerCapita(i));
        double usefulExergy = SmoothMath.softplus(primaryWatts * etaExergy, 1.0, 5.0);
        double economicOutput = Math.pow(capital, ALPHA_K) * Math.pow(labor, BETA_L) * Math.pow(usefulExergy, GAMMA_E);

        double netDeltaK = (economicOutput * 0.05 - capital * 0.04) * dtYears;
        acc.dResourceCapital += (float) netDeltaK;
    }

    @Override
    public String getName() {
        return I18n.getOrDefault(
                "engine.kummel_ayres.name",
                "Kümmel / Ayres-Warr Biophysical Exergy Economics");
    }

    @Override
    public String getDescription() {
        return I18n.getOrDefault(
                "engine.kummel_ayres.desc",
                "Thermodynamic macroeconomic production function Y = A · K^α · L^β · E_useful^γ where useful work/exergy is the prime driver of industrial output.");
    }

    @Override
    public String getEquationsTooltip() {
        return I18n.getOrDefault(
                "engine.kummel_ayres.tooltip",
                """
                [Kümmel & Ayres-Warr Useful Exergy Production Function]
                • Output Function:        Y_i = A_i · K_i^0.25 · L_i^0.25 · (E_useful,i)^0.50
                • Second-Law Exergy Eff.: η_exergy = ε_II · (1 - T_C / T_H)
                • Useful Exergy Flux:     E_useful,i = P_primary,i · η_exergy
                • Euler Homogeneity:      α + β + γ = 1.0
                Units: Y [output/yr], K [kg physical capital], L [person-eq], E_useful [W]
                Ref: R. Kümmel (2011) "The Second Law of Economics"; Ayres & Warr (2009) "The Economic Growth Engine"
                """);
    }

    @Override
    public String getCategory() {
        return "Tier 2: Biophysical Economics";
    }

    @Override
    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        processKummelAyresExergy(buffer, deltaYears);
    }

    /**
     * Executes pure Data-Oriented Design (DoD) Second-Law Carnot exergy production over {@link WorldBuffer}.
     *
     * @param buffer     contiguous world buffer
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processKummelAyresExergy(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0.0) return;
        KummelAyresExergyEngine instance = new KummelAyresExergyEngine();
        instance.tick(buffer, deltaYears, deltaYears * 12.0);
    }
}
