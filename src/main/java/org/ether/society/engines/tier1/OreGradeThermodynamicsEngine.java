/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ore Grade Thermodynamics Engine (Bihouix Ore Depletion Law).
 * Modifies extraction energy requirements exponentially as ore mass concentration (C_ore) depletes.
 * Formula: E_extract = E_base * (C_ref / C_ore)^1.5
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class OreGradeThermodynamicsEngine extends AbstractSimulationEngine {
    public OreGradeThermodynamicsEngine() {
        super("OreGradeThermodynamicsEngine", "OreGradeThermodynamicsEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    @Override
    public void reset() {
        resetState();
    }

    private static final Logger logger = LoggerFactory.getLogger(OreGradeThermodynamicsEngine.class);

    private static double referenceOreConcentration = 0.05; // 5% reference metal concentration
    private static double minOreConcentrationFloor = 0.001; // 0.1% physical limit floor

    /**
     * Gets pristine reference ore concentration \(C_{\text{ref}}\).
     *
     * @return dimensionless mass fraction (default 0.05 = 5%)
     */
    public static double getReferenceOreConcentration() { return referenceOreConcentration; }

    /**
     * Sets reference ore concentration clamped above 0.001.
     *
     * @param conc dimensionless mass fraction
     */
    public static void setReferenceOreConcentration(double conc) { referenceOreConcentration = Math.max(0.001, conc); }

    /**
     * Resets reference ore concentration parameters for scenario isolation.
     */
    public static void resetState() {
        referenceOreConcentration = 0.05;
        minOreConcentrationFloor = 0.001;
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 0.0) return;

        double currentMetalStock = buffer.getMetalResource()[i];
        if (currentMetalStock > 0.0) {
            double oreConcentration = Math.max(minOreConcentrationFloor, referenceOreConcentration * (currentMetalStock / 100.0));
            double energyMultiplier = Math.pow(referenceOreConcentration / oreConcentration, 1.5);
            acc.dResourceCapital -= (float) Math.min(buffer.getResourceCapital()[i] * 0.01, 0.01 * energyMultiplier * dtYears);
        }
    }
}
