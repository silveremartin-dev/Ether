/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;

import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Sakoku Edo Japan Isolationism &amp; Forest Conservation Engine Variant B35.1 (Pure) &amp; B35.2 (Hybrid).
 * Models Edo Period Japan (1603-1868):
 * 1. Sakoku isolationism prevents external trade contagion and resource drain.
 * 2. Tokugawa forest conservation &amp; organic recycling establish a zero-growth ecological equilibrium.
 * 3. High population density sustained at steady state (~30 million) with high social stability.
 * 4. Ended by external military tech shock (Perry Black Ships 1853).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class EdoJapanIsolationEngine extends AbstractSimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EdoJapanIsolationEngine.class);

    public EdoJapanIsolationEngine() {
        super("EdoJapanIsolationEngine", "Edo Japan Sakoku Isolation & Forestry", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public boolean isEnabled(Scenario scenario) {
        if (scenario != null && scenario.getTypeBEngineStates() != null) {
            return Boolean.TRUE.equals(scenario.getTypeBEngineStates().get(id()));
        }
        return sakokuIsolationActive;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (!sakokuIsolationActive) return;
        super.tick(buffer, dtYears, dtMonthly);
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || dtYears <= 0.0) return;
        float pop = buffer.getHumanPopulation() != null ? buffer.getHumanPopulation()[i] : 0.0f;
        if (pop <= 0.001f) return;

        // 1. Closed-loop urban & agrarian organic recycling (Shimoda / Edo night-soil economy)
        if (buffer.getPollutionLevel() != null) {
            float pol = buffer.getPollutionLevel()[i];
            if (pol > 0.0f) {
                acc.dPollutionLevel -= (float) Math.min(pol, 1.0 * dtYears);
            }
        }
        if (buffer.getSoilOrganicCarbon() != null) {
            double densityPerKm2 = buffer.getPopulationDensityPerKm2(i);
            float soc = buffer.getSoilOrganicCarbon()[i];
            if (soc < SOC_SATURATION) {
                double dSoc = NIGHT_SOIL_SOC_COEFF * densityPerKm2 * (1.0 - soc / SOC_SATURATION) * dtYears;
                acc.dSoilOrganicCarbon += (float) Math.max(0.0, dSoc);
            }
        }

        // 2. Managed sugi/hinoki forestry regeneration bounded by primary standing biomass (Totman 1989)
        if (buffer.getWoodResource() != null && buffer.getBiomassNatural() != null) {
            float wood = buffer.getWoodResource()[i];
            float natBio = buffer.getBiomassNatural()[i];
            double woodCap = Math.max(wood, natBio * 0.35);
            if (wood > 0.0f && wood < woodCap && natBio > 1.0f) {
                double regrowth = PLANTATION_REGROWTH_RATE * wood * (1.0 - wood / woodCap) * dtYears;
                double boundedTransfer = Math.min(natBio * 0.05 * dtYears, regrowth);
                if (boundedTransfer > 0.0) {
                    acc.dWoodResource += (float) boundedTransfer;
                    acc.dBiomassNatural -= (float) boundedTransfer;
                }
            }
        }
    }

    @Override
    public void reset() {
        resetState();
    }

    /** Internal state: sakoku isolation active (boolean). */
    private static volatile boolean sakokuIsolationActive = false;
    private static double sustainableEquilibriumCap = 30.0; // 30M population equilibrium cap

    /** Logistic regrowth rate of managed sugi/hinoki plantations, yr<sup>-1</sup> (Totman 1989). */
    private static final double PLANTATION_REGROWTH_RATE = 0.025;
    /** Night-soil soil-organic-carbon return per unit population density, (g/kg) km<sup>2</sup> hab<sup>-1</sup> yr<sup>-1</sup>. */
    private static final double NIGHT_SOIL_SOC_COEFF = 0.002;
    /** Soil organic carbon saturation level for paddy soils (g/kg). */
    private static final double SOC_SATURATION = 40.0;
    /** First-observed standing wood stock per H3 cell, used as the logistic carrying capacity W0. */
    private static final java.util.Map<Long, Double> initialWoodStock = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Resets mutable static state between scenario executions to guarantee isolation.
     */
    public static void resetState() {
        sakokuIsolationActive = false;
        sustainableEquilibriumCap = 30.0;
        initialWoodStock.clear();
    }

    /**
     * Is sakoku isolation active.
     *
     * @return the resulting computation or state reference
     */
    public static boolean isSakokuIsolationActive() { return sakokuIsolationActive; }

    /**
     * Set sakoku isolation active operation.
     * <p>
     * Executes operational logic for {@code EdoJapanIsolationEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param active the active argument (boolean)
     */
    public static void setSakokuIsolationActive(boolean active) { sakokuIsolationActive = active; }

    /**
     * Get sustainable equilibrium cap.
     *
     * @return the resulting computation or state reference
     */
    public static double getSustainableEquilibriumCap() { return sustainableEquilibriumCap; }

    /**
     * Set sustainable equilibrium cap operation.
     * <p>
     * Executes operational logic for {@code EdoJapanIsolationEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param cap the cap argument (double)
     */
    public static void setSustainableEquilibriumCap(double cap) { sustainableEquilibriumCap = Math.max(1.0, cap); }
}
