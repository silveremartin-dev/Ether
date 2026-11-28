/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.dod.WorldBufferComponentAdapter;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.model.Scenario;
import org.ether.society.util.DeterministicPRNG;

/**
 * Structural-Demographic Bifurcation &amp; Poisson Catastrophe Jump Engine.
 *
 * <p>Formalizes Peter Turchin's Secular Cycles and Jack Goldstone's Structural-Demographic Theory (SDT)
 * via cellular Poisson jump transitions when Political Stress Index (\(\text{PSI}\)) crosses critical
 * thresholds, participating in {@code TypeBEnginePassRunner}'s non-diluting capital envelope
 * ({@code AGENTS.md} §4.2).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class StructuralDemographicBifurcationEngine extends AbstractSimulationEngine {

    private final double psiThreshold;
    private final double baseJumpIntensity;
    private long baseSeed = 42L;
    private long totalCrisesTriggered = 0;
    private double meanPlanetaryPSI = 0.0;
    private transient double tickPsiAccumulator = 0.0;
    private transient int tickActiveCells = 0;

    /**
     * Constructs the SDT bifurcation engine with default threshold and jump hazard parameters.
     */
    public StructuralDemographicBifurcationEngine() {
        this(5.0, 0.02, 42L);
    }

    /**
     * Constructs the SDT bifurcation engine with custom parameters.
     *
     * @param psiThreshold      critical PSI threshold above which Poisson crisis hazard activates
     * @param baseJumpIntensity baseline annual Poisson crisis rate (\(\text{yr}^{-1}\))
     * @param baseSeed          deterministic scenario PRNG seed
     */
    public StructuralDemographicBifurcationEngine(double psiThreshold, double baseJumpIntensity, long baseSeed) {
        super("StructuralDemographicBifurcationEngine", "StructuralDemographicBifurcationEngine", EngineTier.TIER_2_CLIODYNAMIC);
        this.psiThreshold = Math.max(1.0, psiThreshold);
        this.baseJumpIntensity = Math.max(1e-4, baseJumpIntensity);
        this.baseSeed = baseSeed;
    }

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
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void prepareTick(WorldBuffer buffer, double dtYears) {
        tickPsiAccumulator = 0.0;
        tickActiveCells = 0;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || dtYears <= 0.0 || i < 0 || i >= buffer.getCapacity()) return;
        float pop = buffer.getHumanPopulation()[i];
        if (pop < 50.0f) return;

        float capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 100.0f;
        double availFoodGJ = WorldBufferComponentAdapter.computeAvailableEdibleCaloricGJ(buffer, i);
        double density = buffer.getPopulationDensityPerKm2(i);
        double foodPerCap = availFoodGJ / Math.max(1.0, pop);
        double mmp = Math.clamp((1.5 / Math.max(0.2, foodPerCap)) * Math.tanh(density / 150.0), 0.2, 5.0);

        float[] giniArr = buffer.getGiniIndex();
        double gini = (giniArr != null && giniArr[i] > 0.0f) ? giniArr[i] : 0.35;
        double emp = Math.clamp(Math.pow(Math.max(0.0, gini - 0.20) / 0.25, 2.0), 0.2, 5.0);

        double kPerCap = capital / Math.max(1.0, pop);
        double sf = Math.clamp(1.0 + 1.5 / (1.0 + Math.exp(-0.01 * (kPerCap - 500.0))), 0.5, 3.0);

        double psi = mmp * emp * sf;
        tickPsiAccumulator += psi;
        tickActiveCells++;
        meanPlanetaryPSI = tickPsiAccumulator / tickActiveCells;

        if (psi > psiThreshold) {
            double excessPsi = Math.min(10.0, psi - psiThreshold);
            double hazardRate = baseJumpIntensity * Math.exp(0.4 * excessPsi);
            double jumpProbability = 1.0 - Math.exp(-hazardRate * dtYears);

            double prng = DeterministicPRNG.nextDouble(baseSeed, buffer.getSimulationTick(), i, 105L);
            if (prng < jumpProbability) {
                totalCrisesTriggered++;
                double targetCap = Math.max(10.0, capital * 0.75);
                acc.dResourceCapital += (float) (targetCap - capital);
                if (giniArr != null) {
                    double targetGini = Math.max(0.20, gini * 0.70);
                    acc.dGiniIndex += (float) (targetGini - gini);
                }
                double targetPop = Math.max(20.0, pop * 0.85);
                acc.dHumanPopulation += (float) (targetPop - pop);
            }
        }
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        processBuffer(buffer, dtYears);
    }

    @Override
    public void reset() {
        totalCrisesTriggered = 0;
        meanPlanetaryPSI = 0.0;
        tickPsiAccumulator = 0.0;
        tickActiveCells = 0;
    }

    /**
     * Executes standalone buffer evaluation for unit verification suites.
     *
     * @param buffer     target world buffer
     * @param deltaYears time step in Julian years
     */
    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0.0) return;
        prepareTick(buffer, deltaYears);
        DerivativeAccumulator acc = new DerivativeAccumulator();
        int count = buffer.getCapacity();
        for (int i = 0; i < count; i++) {
            acc.clear();
            accumulateCellularDerivatives(buffer, i, deltaYears, deltaYears * 12.0, acc);
            if (acc.hasNonZeroDerivatives()) {
                AbstractSimulationEngine.applyAccumulatorToCell(buffer, i, acc);
            }
        }
    }

    /**
     * Sets the deterministic base PRNG seed.
     *
     * @param seed scenario base seed
     */
    public void setSeed(long seed) {
        this.baseSeed = seed;
    }

    @Override
    public String getName() {
        return "Structural-Demographic Bifurcation & Crisis Jumps (SDT)";
    }

    @Override
    public String getDescription() {
        return "Simulates structural secular instability cycles, elite overproduction tension, and discrete Poisson state collapse jumps.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Turchin-Goldstone Structural Demographic Bifurcation Model]
               • Political Stress Index:  PSI = MMP · EMP · SF
               • Mass Immiseration:       MMP = (Food_req / Food_avail) · tanh(Density / 150)
               • Elite Overproduction:    EMP = ((Gini - 0.20) / 0.25)^2
               • State Fiscal Distress:   SF  = 1.0 + 1.5 / (1.0 + exp(-0.01 · (k_pc - 500)))
               • Poisson Jump Hazard:     λ_crisis = λ_0 · exp(κ · max(0, PSI - PSI_crit))
               • Crisis Dissipation:      ΔGini = -30%, ΔCapital = -25%, ΔMortality = +15%
               Ref: P. Turchin (2016) "Ages of Discord", W. Scheidel (2017) "The Great Leveler"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Structural Cliodynamics";
    }

    /**
     * Gets total crisis jumps triggered since last reset.
     *
     * @return crisis count
     */
    public long getTotalCrisesTriggered() {
        return totalCrisesTriggered;
    }

    /**
     * Gets mean planetary Political Stress Index across active populated cells.
     *
     * @return mean PSI
     */
    public double getMeanPlanetaryPSI() {
        return meanPlanetaryPSI;
    }
}
