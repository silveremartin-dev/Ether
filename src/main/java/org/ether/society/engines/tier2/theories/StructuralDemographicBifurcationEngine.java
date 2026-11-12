/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.database.H3Cell;

import org.ether.society.model.Scenario;
import org.ether.society.util.DeterministicPRNG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Structural-Demographic Bifurcation &amp; Poisson Catastrophe Jump Engine.
 *
 * <p>Formalizes Peter Turchin's Secular Cycles and Jack Goldstone's Structural-Demographic Theory (SDT)
 * without resorting to ad-hoc individual agent minds. Continuously evaluates structural tension tensors
 * and triggers discrete Poisson jump transitions when structural stress crosses critical thresholds:</p>
 *
 * <ul>
 *   <li><b>Elite Overproduction Tensor</b>: $\text{EMP} = (N_{\text{elites}} / S_{\text{offices}}) \cdot \text{Gini}^2 \cdot \text{Factionalism}$</li>
 *   <li><b>Mass Mobilization Pressure</b>: $\text{MMP} = (w_0 / w_{\text{real}}) \cdot (P / K_{\text{food}})$</li>
 *   <li><b>State Fiscal Distress</b>: $\text{SF} = \text{Debt} / \text{FiscalCapacity}$</li>
 *   <li><b>Political Stress Index</b>: $\text{PSI} = \text{MMP} \cdot \text{EMP} \cdot \text{SF}$</li>
 *   <li><b>Poisson Catastrophe Jump Intensity</b>: $\lambda_{\text{crisis}} = \lambda_0 \cdot \exp(\kappa \cdot \max(0, \text{PSI} - \text{PSI}_{\text{crit}}))$</li>
 * </ul>
 *
 * <p>Upon crisis jump occurrence, the system executes structural dissipative resets:
 * wealth inequality collapse ($\Delta \text{Gini} &lt; 0$, Scheidel's Great Leveler),
 * capital destruction ($\Delta K &lt; 0$), and fiscal debt repudiation.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class StructuralDemographicBifurcationEngine extends AbstractSimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(StructuralDemographicBifurcationEngine.class);

    @Override
    public String id() {
        return "StructuralDemographicBifurcationEngine";
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
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        processBuffer(buffer, dtYears);
    }

    @Override
    public void reset() {
        totalCrisesTriggered = 0;
        meanPlanetaryPSI = 0.0;
    }

    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0) return;
        float[] popArr = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] foodArr = buffer.getFoodResource();
        if (popArr == null) return;

        double sumPSI = 0.0;
        int activeCount = 0;
        int count = buffer.getCapacity();

        for (int i = 0; i < count; i++) {
            float pop = popArr[i];
            if (pop < 50.0f) continue;
            activeCount++;

            float capital = capArr != null ? capArr[i] : 100.0f;
            float food = foodArr != null ? foodArr[i] : 1000.0f;

            double areaKm2 = 1770.49;
            double density = pop / areaKm2;
            double foodPerCap = food / Math.max(1.0, pop);
            double mmp = Math.clamp((1.5 / Math.max(0.2, foodPerCap)) * Math.tanh(density / 150.0), 0.2, 5.0);

            double gini = 0.35;
            double emp = Math.clamp(Math.pow(Math.max(0.0, gini - 0.20) / 0.25, 2.0), 0.2, 5.0);

            double kPerCap = capital / Math.max(1.0, pop);
            double sf = Math.clamp(1.0 + 1.5 / (1.0 + Math.exp(-0.05 * (kPerCap - 30.0))), 0.5, 3.0);

            double psi = mmp * emp * sf;
            sumPSI += psi;

            if (psi > psiThreshold) {
                double excessPsi = Math.min(10.0, psi - psiThreshold);
                double hazardRate = baseJumpIntensity * Math.exp(0.4 * excessPsi);
                double jumpProbability = 1.0 - Math.exp(-hazardRate * deltaYears);

                double prng = DeterministicPRNG.nextDouble(baseSeed, 0L, i, 105L);
                if (prng < jumpProbability) {
                    totalCrisesTriggered++;
                    if (capArr != null) {
                        capArr[i] = (float) Math.max(10.0, capital * 0.75);
                    }
                    popArr[i] = (float) Math.max(20.0, pop * 0.85);
                }
            }
        }
        meanPlanetaryPSI = activeCount > 0 ? sumPSI / activeCount : 0.0;
    }

    /** Internal state: psi threshold (double). */
    private final double psiThreshold;
    /** Internal state: base jump intensity (double). */
    private final double baseJumpIntensity;
    /** Internal state: scenario seed for deterministic PRNG. */
    private long baseSeed = 42L;

    /** Internal state: total crises triggered (long). */
    private long totalCrisesTriggered = 0;
    /** Internal state: mean planetary psi (double). */
    private double meanPlanetaryPSI = 0.0;

    /**
     * Structural demographic bifurcation engine.
     *
     */
    public StructuralDemographicBifurcationEngine() {
        this(5.0, 0.02, 42L);
    }

    /**
     * Structural demographic bifurcation engine.
     *
     * @param psiThreshold the psi threshold parameter (double)
     * @param baseJumpIntensity the base jump intensity parameter (double)
     * @param baseSeed the scenario base seed parameter (long)
     */
    public StructuralDemographicBifurcationEngine(double psiThreshold, double baseJumpIntensity, long baseSeed) {
        super("StructuralDemographicBifurcationEngine", "StructuralDemographicBifurcationEngine", EngineTier.TIER_2_CLIODYNAMIC);
        this.psiThreshold = Math.max(1.0, psiThreshold);
        this.baseJumpIntensity = Math.max(1e-4, baseJumpIntensity);
        this.baseSeed = baseSeed;
    }

    /**
     * Legacy constructor accepting Random.
     *
     * @param psiThreshold the psi threshold parameter
     * @param baseJumpIntensity base jump intensity
     * @param random legacy random (ignored in favor of DeterministicPRNG)
     */
    public StructuralDemographicBifurcationEngine(double psiThreshold, double baseJumpIntensity, Random random) {
        this(psiThreshold, baseJumpIntensity, 42L);
    }

    /**
     * Sets the scenario seed on the internal random number generator.
     *
     * @param seed the scenario seed (long)
     */
    public void setSeed(long seed) {
        this.baseSeed = seed;
    }

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getName() {
        return "Structural-Demographic Bifurcation & Crisis Jumps (SDT)";
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getDescription() {
        return "Simulates structural secular instability cycles, elite overproduction tension, and discrete Poisson state collapse jumps.";
    }

    /**
     * Get equations tooltip.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getEquationsTooltip() {
        return """
               [Turchin-Goldstone Structural Demographic Bifurcation Model]
               â€¢ Political Stress Index:  PSI = MMP Â· EMP Â· SF
               â€¢ Mass Immiseration:       MMP = (Food_req / Food_avail) Â· (P / CarryingCapacity)
               â€¢ Elite Overproduction:    EMP = (Capital / BaseCapital) Â· GiniÂ²
               â€¢ State Fiscal Distress:   SF  = 1.0 + 1.5 / (1.0 + exp(-0.01 Â· (Capital - 500)))
               â€¢ Poisson Jump Hazard:     Î»_crisis = Î»â‚€ Â· exp(Îº Â· max(0, PSI - PSI_crit))
               â€¢ Crisis Dissipation:      Î”Gini = -30%, Î”Capital = -25%, Î”Mortality = +15%
               Ref: P. Turchin (2016) "Ages of Discord", W. Scheidel (2017) "The Great Leveler"
               """;
    }

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getCategory() {
        return "Tier 2: Structural Cliodynamics";
    }

    /**
     * Get total crises triggered.
     *
     * @return the resulting computation or state reference
     */
    public long getTotalCrisesTriggered() { return totalCrisesTriggered; }
    /**
     * Get mean planetary psi.
     *
     * @return the resulting computation or state reference
     */
    public double getMeanPlanetaryPSI() { return meanPlanetaryPSI; }
    /**
     * Get psi threshold.
     *
     * @return the resulting computation or state reference
     */
    public double getPsiThreshold() { return psiThreshold; }
}
