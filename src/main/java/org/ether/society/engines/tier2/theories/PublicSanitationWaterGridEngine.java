/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.Scenario;

/**
 * <h1>Public Sanitation &amp; Municipal Clean Water Grid Engine</h1>
 * <p>
 * Implements the Concave Preston Curve (Preston 1975) and Clean-Water Epidemiological
 * Mortality Attenuation (Omran 1971; Cutler &amp; Miller 2005):
 * </p>
 * <ul>
 *   <li><b>Preston Logarithmic Life Expectancy Law (Preston 1975, <i>Population Studies</i> 29:231)</b>:
 *       $$e_0^*(k_{\text{pc}}) = e_{0,\text{base}} + \beta_P \ln\!\left(1 + \frac{k_{\text{san}}}{k_0}\right)$$</li>
 *   <li><b>Cutler-Miller Waterborne Pathogen Attenuation (Cutler &amp; Miller 2005, <i>Demography</i> 42:1)</b>:
 *       Filtration, chlorination, and closed sewerage infrastructure \(S(k_{\text{pc}}, W) \in [0, 1)\)
 *       abate urban fecal-oral crowding mortality \(\Delta e_{\text{crowd}} = -\mu_{\text{crowd}} \frac{\rho}{\rho + \rho_0} (1 - S)\).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class PublicSanitationWaterGridEngine extends AbstractSimulationEngine {

    /**
     * Constructs the public sanitation and clean water grid engine.
     */
    public PublicSanitationWaterGridEngine() {
        super("PublicSanitationWaterGridEngine", "PublicSanitationWaterGridEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public String id() {
        return "PublicSanitationWaterGridEngine";
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
    public String getName() {
        return "PublicSanitationWaterGridEngine";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Preston Curve & Cutler-Miller Clean Water Transition]
               • Sanitation Coverage:    S(k_pc) = k_san / (k_san + k_half)
               • Crowding Penalty:       Δe_crowd = -8.0 · (ρ / (ρ + 200)) · (1 - S)
               • Preston Equilibrium:    e_0* = 30.0 + 18.5 · ln(1 + k_san / 250) · min(1, W) + Δe_crowd
               Ref: S. H. Preston (1975, Pop. Stud. 29:231); D. Cutler & G. Miller (2005, Demography 42:1)
               """;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || dtYears <= 0.0 || buffer.getHumanPopulation() == null || buffer.getLifespan() == null) {
            return;
        }
        float p = buffer.getHumanPopulation()[i];
        if (p <= 0.001f) return;

        double capital = (buffer.getResourceCapital() != null) ? Math.max(0.0, buffer.getResourceCapital()[i]) : 0.0;
        double kPerCap = capital / p;
        double waterAvail = (buffer.getWaterResource() != null) ? Math.clamp(buffer.getWaterResource()[i], 0.0, 1.0) : 1.0;

        double densityPerKm2 = buffer.getPopulationDensityPerKm2(i);
        double crowdingPathogenPressure = densityPerKm2 / (densityPerKm2 + 200.0);

        // Smooth C^infty municipal aqueduct/sewerage/filtration infrastructure above urban masonry threshold (200 kg/cap)
        double kSanitary = SmoothMath.softplus(kPerCap - 200.0, 0.0, 25.0);
        double sanitationCoverage = (kSanitary / (kSanitary + 1200.0)) * waterAvail;

        // Concave Preston (1975) logarithmic health return + Cutler-Miller (2005) crowding pathogen abatement
        double prestonGainYears = 18.5 * Math.log1p(kSanitary / 250.0) * waterAvail;
        double crowdingLossYears = 8.0 * crowdingPathogenPressure * (1.0 - sanitationCoverage);
        double targetLifespan = Math.clamp(30.0 + prestonGainYears - crowdingLossYears, 22.0, 85.0);

        double currentLifespan = buffer.getLifespan()[i] > 10.0f ? buffer.getLifespan()[i] : 35.0;
        double relaxation = 1.0 - Math.exp(-0.10 * dtYears);
        acc.dLifespan += (float) ((targetLifespan - currentLifespan) * relaxation);
    }

    @Override
    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        processSanitationGrids(buffer, deltaYears);
    }

    /**
     * Executes one tick of sanitation coverage and pathogen mortality attenuation directly over {@link WorldBuffer}.
     *
     * @param buffer     contiguous world buffer
     * @param deltaYears time step fraction in Julian years
     */
    public static void processSanitationGrids(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0.0) return;
        PublicSanitationWaterGridEngine instance = new PublicSanitationWaterGridEngine();
        instance.tick(buffer, deltaYears, deltaYears * 12.0);
    }
}
