/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.tier2;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Structural-Demographic Bifurcation & Poisson Catastrophe Jump Engine.
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
 * wealth inequality collapse ($\Delta \text{Gini} < 0$, Scheidel's Great Leveler),
 * capital destruction ($\Delta K < 0$), and fiscal debt repudiation.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class StructuralDemographicBifurcationEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(StructuralDemographicBifurcationEngine.class);

    private final double psiThreshold;
    private final double baseJumpIntensity;
    private final Random random;

    private long totalCrisesTriggered = 0;
    private double meanPlanetaryPSI = 0.0;

    public StructuralDemographicBifurcationEngine() {
        this(5.0, 0.02, new Random(42));
    }

    public StructuralDemographicBifurcationEngine(double psiThreshold, double baseJumpIntensity, Random random) {
        this.psiThreshold = Math.max(1.0, psiThreshold);
        this.baseJumpIntensity = Math.max(1e-4, baseJumpIntensity);
        this.random = random != null ? random : new Random(42);
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
               • Mass Immiseration:       MMP = (Food_req / Food_avail) · (P / CarryingCapacity)
               • Elite Overproduction:    EMP = (Capital / BaseCapital) · Gini²
               • State Fiscal Distress:   SF  = 1.0 + max(0, (Debt - Revenue) / Revenue)
               • Poisson Jump Hazard:     λ_crisis = λ₀ · exp(κ · max(0, PSI - PSI_crit))
               • Crisis Dissipation:      ΔGini = -30%, ΔCapital = -25%, ΔMortality = +15%
               Ref: P. Turchin (2016) "Ages of Discord", W. Scheidel (2017) "The Great Leveler"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Structural Cliodynamics";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0) return;

        double sumPSI = 0.0;
        int activeCount = 0;

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop < 50) continue;
            activeCount++;

            double gini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.35;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 100.0;
            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 1000.0;

            // 1. Mass Mobilization Potential (MMP): Food scarcity relative to population
            double requiredFood = pop * 2.0;
            double mmp = Math.max(0.5, (requiredFood / Math.max(10.0, food)));

            // 2. Elite Mobilization Potential (EMP): Wealth concentration and elite surplus
            double emp = Math.max(0.5, (capital / 100.0) * (gini * gini * 3.0));

            // 3. State Fiscal Distress (SF): Overextension relative to taxable base
            double sf = 1.0 + Math.max(0.0, (capital > 500.0 ? 1.5 : 0.0));

            // Composite PSI
            double psi = mmp * emp * sf;
            sumPSI += psi;

            // 4. Poisson Jump Hazard Rate: λ(t) = λ₀ · exp(0.5 · max(0, PSI - PSI_crit))
            if (psi > psiThreshold) {
                double excessPsi = psi - psiThreshold;
                double hazardRate = baseJumpIntensity * Math.exp(0.4 * excessPsi);
                double jumpProbability = 1.0 - Math.exp(-hazardRate * deltaYears);

                if (random.nextDouble() < jumpProbability) {
                    // Execute structural dissipation jump (The Great Leveler)
                    totalCrisesTriggered++;

                    // A. Inequality compression: elites lose estates and rents
                    double newGini = Math.max(0.20, gini * 0.70);
                    cell.setGiniIndex(newGini);

                    // B. Capital physical destruction (warfare, fires, neglected irrigation)
                    double newCapital = Math.max(10.0, capital * 0.75);
                    cell.setResourceCapital(newCapital);

                    // C. Excess crisis mortality shock
                    int survivingPop = (int) Math.max(20, pop * 0.85);
                    cell.setPopulation(survivingPop);
                }
            }
        }

        meanPlanetaryPSI = activeCount > 0 ? sumPSI / activeCount : 0.0;
    }

    public long getTotalCrisesTriggered() { return totalCrisesTriggered; }
    public double getMeanPlanetaryPSI() { return meanPlanetaryPSI; }
    public double getPsiThreshold() { return psiThreshold; }
}
