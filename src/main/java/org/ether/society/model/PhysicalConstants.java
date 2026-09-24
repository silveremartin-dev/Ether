/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

/**
 * Fundamental Physical, Thermodynamic, Biological, and Cliodynamic Constants.
 * 
 * Provides SI units for thermodynamics, energy transport, allometric scaling,
 * actuarial kinetics, and planetary radiative balance across all kernels.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public final class PhysicalConstants {

    private PhysicalConstants() {
        // Utility class
    }

    // =========================================================================
    // 1. THERMODYNAMIC & UNIVERSAL CONSTANTS
    // =========================================================================
    /** Universal gas constant R in J/(mol·K) */
    public static final double R_GAS_CONSTANT = 8.314462618;

    /** Boltzmann constant kB in J/K */
    public static final double K_BOLTZMANN = 1.380649e-23;

    /** Stefan-Boltzmann constant σ in W/(m²·K⁴) */
    public static final double STEFAN_BOLTZMANN_SIGMA = 5.670374419e-8;

    /** Avogadro constant N_A in mol⁻¹ */
    public static final double AVOGADRO_NUMBER = 6.02214076e23;

    /** Standard atmospheric pressure at sea level P_0 in Pascals */
    public static final double STANDARD_ATM_PASCALS = 101325.0;

    /** Standard zero Celsius in Kelvin */
    public static final double KELVIN_ZERO_CELSIUS = 273.15;

    // =========================================================================
    // 2. ASTRONOMICAL & RADIATIVE CONSTANTS
    // =========================================================================
    /** Solar constant / Top of atmosphere irradiance S_0 in W/m² */
    public static final double SOLAR_CONSTANT_WPM2 = 1361.0;

    /** Duration of one Julian year in SI seconds (365.25 days * 86400 s/day) */
    public static final double SECONDS_PER_JULIAN_YEAR = 31557600.0;

    /** Duration of one standard Earth day in SI seconds */
    public static final double SECONDS_PER_DAY = 86400.0;

    /** Mean planetary Earth radius in kilometers */
    public static final double EARTH_RADIUS_KM = 6371.0;

    // =========================================================================
    // 3. BIOLOGICAL, METABOLIC & TROPHIC ENERGY CONSTANTS
    // =========================================================================
    /** Kleiber Allometric Scaling exponent (3/4 power law) */
    public static final double KLEIBER_ALLOMETRIC_EXPONENT = 0.75;

    /** Kleiber baseline specific metabolic rate coefficient in W/kg^(3/4) */
    public static final double KLEIBER_BASELINE_METABOLIC_RATE = 3.39;

    /** Standard daily basal + active human metabolic intake in Joules (2,200 kcal/day = 9,204.8 kJ) */
    public static final double HUMAN_DAILY_METABOLIC_ENERGY_JOULES = 9.2048e6;

    /** Standard annual human metabolic intake in Joules per capita (365.25 days * 9.2048 MJ = 3.362 * 10^9 J) */
    public static final double HUMAN_ANNUAL_METABOLIC_ENERGY_JOULES = 3.362e9;

    /** Standard annual human metabolic intake in Gigajoules per capita (3.362 GJ/hab/an) */
    public static final double HUMAN_ANNUAL_METABOLIC_ENERGY_GJ = 3.362;

    /** Standard annual human metabolic intake in Megajoules per capita (3,362 MJ/hab/an) */
    public static final double HUMAN_ANNUAL_METABOLIC_ENERGY_MJ = 3362.0;

    /** Standard annual human metabolic intake in Kilojoules per capita (3,362,000 kJ/hab/an) */
    public static final double HUMAN_ANNUAL_METABOLIC_ENERGY_KJ = 3.362e6;

    /** Standard enzymatic activation energy E_a in J/mol for cellular respiration */
    public static final double ENZYMATIC_ACTIVATION_ENERGY_J = 54000.0;

    /** Optimal biological temperature T_opt for terrestrial enzymes in Kelvin (25°C = 298.15K) */
    public static final double OPTIMAL_BIOLOGICAL_TEMP_KELVIN = 298.15;

    /** Standard maternal bioenergetic cost of gestation and lactation per birth in Gigajoules (~80k kcal gestation + ~110k kcal lactation) */
    public static final double HUMAN_GESTATION_LACTATION_ENERGY_GJ = 0.80;

    /** Biological baseline minimum age of primiparity (first child) in years */
    public static final double HUMAN_MIN_PRIMIPARITY_AGE_YEARS = 14.0;

    // --- Biomass Energy Densities (MJ / kg fresh weight) (Smil 2008, 2013) ---
    /** Terrestrial mammalian fauna energy density in MJ/kg (muscle, fat, offal, marrow) */
    public static final double BIOMASS_ENERGY_DENSITY_FAUNA_MJ_PER_KG = 8.0;

    /** Wild edible flora, roots, berries and tubers in MJ/kg fresh weight */
    public static final double BIOMASS_ENERGY_DENSITY_FLORA_WILD_MJ_PER_KG = 4.5;

    /** Cultivated dry grains and legumes (wheat, barley, rice, maize) in MJ/kg */
    public static final double BIOMASS_ENERGY_DENSITY_GRAIN_DRY_MJ_PER_KG = 15.0;

    /** Marine and freshwater aquatic biomass (fish, shellfish, seals) in MJ/kg */
    public static final double BIOMASS_ENERGY_DENSITY_MARINE_MJ_PER_KG = 7.0;

    /** Pasture herbage and forage biomass (ruminant feed) in MJ/kg */
    public static final double BIOMASS_ENERGY_DENSITY_HERBAGE_MJ_PER_KG = 4.0;

    // --- Trophic Footprint Multipliers (Mobilized Raw Biomass / Ingested Energy) ---
    /** Terrestrial hunter-gatherers footprint multiplier (Speth & Spielmann 1983) */
    public static final double TROPHIC_MULTIPLIER_HUNTER_GATHERER = 2.25;

    /** Coastal & marine foragers footprint multiplier (higher oxidation & thermal cost) */
    public static final double TROPHIC_MULTIPLIER_COASTAL_FORAGER = 2.75;

    /** Nomadic pastoralists ecological footprint multiplier (ruminant conversion 10:1) */
    public static final double TROPHIC_MULTIPLIER_NOMADIC_PASTORALIST = 12.5;

    /** Early Neolithic agrarian footprint multiplier (storage mold/pest 20% + seed 15%) */
    public static final double TROPHIC_MULTIPLIER_NEOLITHIC_EARLY_AGRARIAN = 2.25;

    /** Advanced preindustrial agrarian with draft animals (horse/ox feed load) */
    public static final double TROPHIC_MULTIPLIER_PREINDUSTRIAL_ADVANCED_AGRARIAN = 5.0;

    /** Industrial urban & factory workers footprint multiplier */
    public static final double TROPHIC_MULTIPLIER_INDUSTRIAL_WORKER = 6.0;

    /** Modern post-industrial globalized food system footprint multiplier */
    public static final double TROPHIC_MULTIPLIER_POST_INDUSTRIAL = 20.0;

    // --- Structural Carcass & Agricultural Fractions ---
    /** Carcass non-alimentary material fraction converted to physical Capital (Binford 1978: bones, sinew, hide) */
    public static final double CARCASS_MATERIAL_BYPRODUCT_FRACTION = 0.20;

    /** Arable land fraction allocated to draft animal fodder in preindustrial agrarianism */
    public static final double PREINDUSTRIAL_FODDER_LAND_FRACTION = 0.35;

    /** Gompertz-Makeham baseline background mortality α (yr⁻¹) */
    public static final double GOMPERTZ_BACKGROUND_MORTALITY_ALPHA = 0.0002;

    /** Gompertz-Makeham initial senescent mortality coefficient β (yr⁻¹) */
    public static final double GOMPERTZ_INITIAL_SENESCENCE_BETA = 0.00003;

    /** Gompertz-Makeham senescent aging rate γ (yr⁻¹) */
    public static final double GOMPERTZ_AGING_RATE_GAMMA = 0.085;

    // =========================================================================
    // 4. THERMODYNAMIC MIGRATION & ONSAGER CONSTANTS
    // =========================================================================
    /** Baseline Onsager transport mobility coefficient M_0 */
    public static final double ONSAGER_BASELINE_MOBILITY = 0.05;

    /** Tainter institutional complexity scaling exponent θ */
    public static final double TAINTER_COMPLEXITY_EXPONENT = 1.15;
}

