/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.engine;

/**
 * Cellular state derivative accumulator for single-pass Kernel Fusion.
 *
 * <p>Fuses multi-engine rate-of-change updates across contiguous DOD tensors in a single memory pass,
 * eliminating redundant memory scans and maximizing CPU cache locality.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class DerivativeAccumulator {

    /** Derivative increment for human somatic biomass (\(\Delta B_{\text{human}}\)). */
    public float dBiomassHuman;

    /** Derivative increment for physical built capital stock (\(\Delta K\)). */
    public float dResourceCapital;

    /** Derivative increment for endosomatic metabolic food energy consumed (\(\Delta E_{\text{food}}\)). */
    public float dEnergyFoodConsumed;

    /** Derivative increment for exosomatic thermal and mechanical energy (\(\Delta E_{\text{fire}}\)). */
    public float dEnergyFire;

    /** Derivative increment for Lorenz Gini inequality coefficient (\(\Delta G\)). */
    public float dGiniIndex;

    /** Derivative increment for ecotoxicological pollution accumulation (\(\Delta P\)). */
    public float dPollutionLevel;

    /** Derivative increment for edible caloric food stockpile (\(\Delta F\)). */
    public float dFoodResource;

    /** Derivative increment for potable freshwater volume (\(\Delta W\)). */
    public float dWaterResource;

    /** Derivative increment for Tainter institutional complexity index (\(\Delta I\)). */
    public float dInstitutionalComplexity;

    /**
     * Resets all accumulator fields to zero before evaluating the next cell slot.
     */
    public void clear() {
        dBiomassHuman = 0.0f;
        dResourceCapital = 0.0f;
        dEnergyFoodConsumed = 0.0f;
        dEnergyFire = 0.0f;
        dGiniIndex = 0.0f;
        dPollutionLevel = 0.0f;
        dFoodResource = 0.0f;
        dWaterResource = 0.0f;
        dInstitutionalComplexity = 0.0f;
    }

    /**
     * Checks if any derivative is non-zero.
     *
     * @return {@code true} if any component has pending updates
     */
    public boolean hasNonZeroDerivatives() {
        return dBiomassHuman != 0.0f
                || dResourceCapital != 0.0f
                || dEnergyFoodConsumed != 0.0f
                || dEnergyFire != 0.0f
                || dGiniIndex != 0.0f
                || dPollutionLevel != 0.0f
                || dFoodResource != 0.0f
                || dWaterResource != 0.0f
                || dInstitutionalComplexity != 0.0f;
    }
}
