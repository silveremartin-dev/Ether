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

    /** Derivative increment for cultivated agricultural biomass (\(\Delta B_{\text{agr}}\)). */
    public float dBiomassAgriculture;

    /** Derivative increment for wild natural biomass (\(\Delta B_{\text{nat}}\)). */
    public float dBiomassNatural;

    /** Derivative increment for extractable metal ore reserve (\(\Delta M\)). */
    public float dMetalResource;

    /** Derivative increment for transport &amp; movement resistance pressure (\(\Delta P_{\text{flux}}\)). */
    public float dFluxPressure;

    /** Derivative increment for commercial logistics flux volume (\(\Delta V_{\text{trade}}\)). */
    public float dCommercialFluxVolume;

    /** Derivative increment for expected biological lifespan (\(\Delta L\), years). */
    public float dLifespan;

    /** Derivative increment for total biological fertility rate (\(\Delta TFR\)). */
    public float dFertility;

    /** Derivative increment for marine/coastal fishery biomass (\(\Delta B_{\text{fish}}\)). */
    public float dBiomassFish;

    /** Derivative increment for mineral coal reserve (\(\Delta C_{\text{coal}}\)). */
    public float dResourceCoal;

    /** Derivative increment for harvestable timber/wood reserve (\(\Delta W_{\text{wood}}\)). */
    public float dWoodResource;

    /** Derivative increment for accessible groundwater aquifer storage (\(\Delta A_{\text{gw}}\)). */
    public float dFreshwaterAquifer;

    /** Derivative increment for topsoil organic carbon (\(\Delta SOC\)). */
    public float dSoilOrganicCarbon;

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
        dBiomassAgriculture = 0.0f;
        dBiomassNatural = 0.0f;
        dMetalResource = 0.0f;
        dFluxPressure = 0.0f;
        dCommercialFluxVolume = 0.0f;
        dLifespan = 0.0f;
        dFertility = 0.0f;
        dBiomassFish = 0.0f;
        dResourceCoal = 0.0f;
        dWoodResource = 0.0f;
        dFreshwaterAquifer = 0.0f;
        dSoilOrganicCarbon = 0.0f;
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
                || dInstitutionalComplexity != 0.0f
                || dBiomassAgriculture != 0.0f
                || dBiomassNatural != 0.0f
                || dMetalResource != 0.0f
                || dFluxPressure != 0.0f
                || dCommercialFluxVolume != 0.0f
                || dLifespan != 0.0f
                || dFertility != 0.0f
                || dBiomassFish != 0.0f
                || dResourceCoal != 0.0f
                || dWoodResource != 0.0f
                || dFreshwaterAquifer != 0.0f
                || dSoilOrganicCarbon != 0.0f;
    }
}
