/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.engine.SimulationEngineModule;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Structural-Demographic Theory (SDT) &amp; Political Stress Index Engine (Jack Goldstone &amp; Peter Turchin).
 *
 * <p>Models the secular cycles of socio-political instability, elite overproduction, and state breakdown:</p>
 * <pre>
 *   PSI = MMP · EMP · SF
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class TurchinGoldstoneSDTEngine implements ProceduralEnginePlugin, SimulationEngineModule {
    private static final Logger logger = LoggerFactory.getLogger(TurchinGoldstoneSDTEngine.class);

    @Override
    public String id() {
        return "TurchinGoldstoneSDTEngine";
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
        // Stateless physical operator
    }

    @Override
    public String getName() {
        return "Turchin-Goldstone Structural-Demographic Theory (SDT)";
    }

    @Override
    public String getDescription() {
        return "Models secular political instability cycles, elite overproduction, mass immiseration, and state fiscal breakdown.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Goldstone-Turchin Political Stress Index (SDT / PSI)]
               • Political Stress Index:  PSI = MMP · EMP · SF
               • Mass Immiseration (MMP): MMP = (w_ref / RealWage) · UrbanFraction
               • Elite Overproduction:    EMP = (N_elites / Office_capacity) · Gini_index
               • State Fiscal Distress:   SF  = SovereignDebt / AnnualTaxRevenue
               • Secular Breakdown Trap:  If PSI > 10.0 -> State Fragmentation & Civil Conflict
               Units: PSI [indice sans dimension [0, +inf]], MMP, EMP, SF [multiplicateurs adimensionnels]
               Ref: P. Turchin (2016) "Ages of Discord", J. A. Goldstone (1991) "Revolution and Rebellion"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Structural Cliodynamics";
    }

    @Override
    public void processBuffer(WorldBuffer buffer, double deltaYears) {
        processSDTDynamics(buffer, deltaYears);
    }

    /**
     * Executes Turchin-Goldstone Structural-Demographic Theory directly over WorldBuffer.
     *
     * @param buffer contiguous world buffer
     * @param deltaYears time step fraction in years
     */
    public static void processSDTDynamics(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || deltaYears <= 0.0) return;
        int n = buffer.getCapacity();
        float[] popArr = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] giniArr = buffer.getGiniIndex();
        float[] foodArr = buffer.getFoodResource();
        float[] compArr = buffer.getInstitutionalComplexity();

        for (int i = 0; i < n; i++) {
            float p = popArr[i];
            if (p < 5.0f) continue;
            float capital = (capArr != null) ? capArr[i] : 10.0f;
            float gini = (giniArr != null) ? giniArr[i] : 0.35f;
            float food = (foodArr != null) ? foodArr[i] : 500.0f;

            double areaKm2 = 36.13;
            double density = (p / 0.06) / areaKm2;
            double foodPerCap = food / Math.max(1.0, (p / 0.06));
            double mmp = Math.max(0.2, (1.9 / Math.max(0.2, foodPerCap)) * Math.tanh(density / 150.0));
            double emp = Math.max(0.2, Math.pow(Math.max(0.0, gini - 0.25) / 0.20, 2.0));
            double kPerCap = capital / Math.max(1.0, (p / 0.06));
            double sf = Math.max(0.2, 0.4 * (1.9 / Math.max(0.2, foodPerCap)) * Math.tanh(kPerCap / 800.0));
            double psi = mmp * emp * sf;

            double psiStress = (psi * psi) / (25.0 + psi * psi);
            double unrestDamage = capital * 0.08 * psiStress * deltaYears;
            if (capArr != null) {
                capArr[i] = (float) Math.max(5.0, capital - unrestDamage);
            }
            if (giniArr != null) {
                double purgeIntensity = Math.pow(psi, 3.0) / (200.0 + Math.pow(psi, 3.0));
                giniArr[i] = (float) Math.max(0.25, gini - (0.04 * purgeIntensity * (gini - 0.25) * deltaYears));
            }
            if (compArr != null) {
                compArr[i] = (float) Math.max(0.0, compArr[i] * (1.0 - 0.05 * psiStress * deltaYears));
            }
        }
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop < 100) continue;

            double gini = cell.getGiniIndexOr(0.35);
            double capital = cell.getResourceCapitalOr(10.0);
            double food = cell.getFoodResourceOr(500.0);

            double areaKm2 = cell.getEffectiveSurfaceAreaKm2();
            double density = pop / Math.max(0.1, areaKm2);
            double foodPerCap = food / Math.max(1.0, pop);
            double mmp = Math.max(0.2, (1.9 / Math.max(0.2, foodPerCap)) * Math.tanh(density / 150.0));
            double emp = Math.max(0.2, Math.pow(Math.max(0.0, gini - 0.25) / 0.20, 2.0));
            double kPerCap = capital / Math.max(1.0, pop);
            double sf = Math.max(0.2, 0.4 * (1.9 / Math.max(0.2, foodPerCap)) * Math.tanh(kPerCap / 800.0));
            double psi = mmp * emp * sf;

            double psiStress = (psi * psi) / (25.0 + psi * psi);
            double unrestDamage = capital * 0.08 * psiStress * deltaYears;
            cell.setResourceCapital(Math.max(5.0, capital - unrestDamage));

            double friction = cell.getMovementFrictionOr(1.0);
            cell.setMovementFriction(Math.min(10.0, friction + (0.15 * psiStress * deltaYears)));

            double purgeIntensity = Math.pow(psi, 3.0) / (200.0 + Math.pow(psi, 3.0));
            cell.setGiniIndex(Math.max(0.25, gini - (0.04 * purgeIntensity * (gini - 0.25) * deltaYears)));
        }
    }
}
