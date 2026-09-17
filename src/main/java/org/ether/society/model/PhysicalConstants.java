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
    // 3. BIOLOGICAL & METABOLIC CONSTANTS
    // =========================================================================
    /** Kleiber Allometric Scaling exponent (3/4 power law) */
    public static final double KLEIBER_ALLOMETRIC_EXPONENT = 0.75;

    /** Kleiber baseline specific metabolic rate coefficient in W/kg^(3/4) */
    public static final double KLEIBER_BASELINE_METABOLIC_RATE = 3.39;

    /** Standard enzymatic activation energy E_a in J/mol for cellular respiration */
    public static final double ENZYMATIC_ACTIVATION_ENERGY_J = 54000.0;

    /** Optimal biological temperature T_opt for terrestrial enzymes in Kelvin (25°C = 298.15K) */
    public static final double OPTIMAL_BIOLOGICAL_TEMP_KELVIN = 298.15;

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

