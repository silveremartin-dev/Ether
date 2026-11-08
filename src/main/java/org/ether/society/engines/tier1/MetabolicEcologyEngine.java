/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Universal Metabolic Theory of Ecology (MTE) Engine.
 *
 * <p>Formalizes the allometric and kinetic unification established by Brown, Gillooly, Allen,
 * Savage &amp; West (2004, <i>Toward a metabolic theory of ecology</i>, Ecology) and Gillooly et al.
 * (2001, 2002, Science/Nature).</p>
 *
 * <p>Unifies Kleiber's 3/4 allometric mass-scaling with Boltzmann-Arrhenius biochemical kinetics:</p>
 * <ol>
 *   <li><b>Individual Metabolic Rate</b>:
 *       \[
 *       B(M, T) = B_0 \cdot M^{3/4} \cdot \exp\left(-\frac{E_a}{k_B T}\right)
 *       \]
 *       where \(M\) is body mass in \(\text{kg}\), \(T\) is absolute temperature in Kelvin,
 *       \(k_B = 8.617333 \times 10^{-5}\text{ eV/K}\), and \(E_a \approx 0.65\text{ eV}\) is the universal
 *       activation energy for respiratory metabolism across all aerobic organisms.</li>
 *   <li><b>Intrinsic Population Growth Rate</b>:
 *       \[
 *       r_{\text{max}}(M, T) = r_0 \cdot M^{-1/4} \cdot \exp\left(-\frac{E_a}{k_B T}\right)
 *       \]
 *   <li><b>Biological Generation Time</b>:
 *       \[
 *       t_{\text{gen}}(M, T) = t_0 \cdot M^{1/4} \cdot \exp\left(\frac{E_a}{k_B T}\right)
 *       \]
 * </ol>
 *
 * <p>Applicable across both human and non-human taxa (terrestrial mammals, megafauna, livestock,
 * marine fish, plants, and microorganisms) to compute emergent biological rates from conserved
 * physical observables (\(O(1)\) per cell/taxon).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.1890/03-9000">Brown et al. (2004), Ecology 85(7)</a>
 */
public class MetabolicEcologyEngine {
    private static final Logger logger = LoggerFactory.getLogger(MetabolicEcologyEngine.class);

    // --- Fundamental Physical & Thermodynamic Constants ---

    /** Boltzmann constant in electron-volts per Kelvin (\(k_B = 8.617333262 \times 10^{-5}\text{ eV/K}\)). */
    public static final double BOLTZMANN_EV_PER_K = 8.617333262e-5;

    /** Universal activation energy for aerobic respiratory metabolism (\(E_a \approx 0.65\text{ eV}\)). */
    public static final double ACTIVATION_ENERGY_EV = 0.65;

    /** Standard reference temperature in Kelvin (\(T_0 = 293.15\text{ K} = 20^\circ\text{C}\)). */
    public static final double REFERENCE_TEMP_K = 293.15;

    // --- Taxon-Specific Normalization Baselines (Brown et al. 2004; Savage et al. 2004) ---

    /** Endotherm (mammalian/avian) metabolic normalization coefficient \(B_{0,\text{endo}} \approx 4.85 \times 10^8\text{ W}\cdot\text{kg}^{-3/4}\). */
    public static final double B0_ENDOTHERM = 4.85e8;

    /** Ectotherm (reptile/fish/amphibian) metabolic normalization coefficient \(B_{0,\text{ecto}} \approx 1.08 \times 10^8\text{ W}\cdot\text{kg}^{-3/4}\). */
    public static final double B0_ECTOTHERM = 1.08e8;

    /** Invertebrate/unicellular metabolic normalization coefficient \(B_{0,\text{invert}} \approx 3.98 \times 10^7\text{ W}\cdot\text{kg}^{-3/4}\). */
    public static final double B0_INVERTEBRATE = 3.98e7;

    /** Vascular plant metabolic normalization coefficient \(B_{0,\text{plant}} \approx 6.57 \times 10^7\text{ W}\cdot\text{kg}^{-3/4}\). */
    public static final double B0_PLANT = 6.57e7;

    /** Intrinsic growth rate normalization constant for endotherms \(r_0 \approx 5.5 \times 10^{10}\text{ year}^{-1}\cdot\text{kg}^{1/4}\). */
    public static final double R0_ENDOTHERM_PER_YEAR = 5.5e10;

    /** Generation time normalization constant \(t_0 \approx 1.8 \times 10^{-11}\text{ year}\cdot\text{kg}^{-1/4}\). */
    public static final double T0_GENERATION_YEARS = 1.8e-11;

    /**
     * Taxonomic guild enumeration for metabolic normalization.
     */
    public enum TaxonGuild {
        ENDOTHERM_MAMMAL,
        ECTOTHERM_VERTEBRATE,
        INVERTEBRATE,
        VASCULAR_PLANT
    }

    /**
     * Calculates the Boltzmann-Arrhenius kinetic temperature factor:
     * \[
     * F_T(T) = \exp\left(-\frac{E_a}{k_B T}\right)
     * \]
     *
     * @param tempKelvin absolute temperature in Kelvin
     * @return dimensionless Boltzmann kinetic factor
     */
    public static double calculateBoltzmannFactor(double tempKelvin) {
        double safeT = Math.max(240.0, Math.min(325.0, tempKelvin));
        return Math.exp(-ACTIVATION_ENERGY_EV / (BOLTZMANN_EV_PER_K * safeT));
    }

    /**
     * Calculates whole-organism basal metabolic rate \(B\) in Watts.
     * \[
     * B(M, T) = B_0 \cdot M^{3/4} \cdot \exp\left(-\frac{E_a}{k_B T}\right)
     * \]
     *
     * @param bodyMassKg body mass in kilograms (\(M\))
     * @param tempKelvin ambient or internal body temperature in Kelvin (\(T\))
     * @param guild      taxonomic classification
     * @return metabolic power dissipation in Watts (\(\text{W} = \text{J/s}\))
     */
    public static double calculateMetabolicRateWatts(double bodyMassKg, double tempKelvin, TaxonGuild guild) {
        if (bodyMassKg <= 0.0) return 0.0;

        double b0 = switch (guild != null ? guild : TaxonGuild.ENDOTHERM_MAMMAL) {
            case ENDOTHERM_MAMMAL -> B0_ENDOTHERM;
            case ECTOTHERM_VERTEBRATE -> B0_ECTOTHERM;
            case INVERTEBRATE -> B0_INVERTEBRATE;
            case VASCULAR_PLANT -> B0_PLANT;
        };

        // Endotherms regulate internal core temperature ~310.15 K (37°C)
        double operativeTemp = (guild == TaxonGuild.ENDOTHERM_MAMMAL) ? 310.15 : tempKelvin;
        double boltzmann = calculateBoltzmannFactor(operativeTemp);

        return b0 * Math.pow(bodyMassKg, 0.75) * boltzmann;
    }

    /**
     * Calculates mass-specific metabolic rate in Watts per kilogram (\(\text{W/kg}\)):
     * \[
     * b(M, T) = \frac{B(M, T)}{M} = B_0 \cdot M^{-1/4} \cdot \exp\left(-\frac{E_a}{k_B T}\right)
     * \]
     *
     * @param bodyMassKg body mass in kilograms
     * @param tempKelvin temperature in Kelvin
     * @param guild      taxonomic classification
     * @return mass-specific metabolic power in Watts/kg
     */
    public static double calculateMassSpecificMetabolicRate(double bodyMassKg, double tempKelvin, TaxonGuild guild) {
        if (bodyMassKg <= 0.0) return 0.0;
        return calculateMetabolicRateWatts(bodyMassKg, tempKelvin, guild) / bodyMassKg;
    }

    /**
     * Calculates the theoretical maximum intrinsic population growth rate \(r_{\text{max}}\) in \(\text{year}^{-1}\):
     * \[
     * r_{\text{max}}(M, T) = r_0 \cdot M^{-1/4} \cdot \exp\left(-\frac{E_a}{k_B T}\right)
     * \]
     *
     * @param bodyMassKg mean adult body mass in kilograms
     * @param tempKelvin ambient temperature in Kelvin
     * @param isEndotherm whether the taxon regulates homeothermic temperature
     * @return maximum demographic growth rate in \(\text{year}^{-1}\)
     */
    public static double calculateIntrinsicGrowthRatePerYear(double bodyMassKg, double tempKelvin, boolean isEndotherm) {
        if (bodyMassKg <= 0.0) return 0.0;
        double operativeTemp = isEndotherm ? 310.15 : tempKelvin;
        double boltzmann = calculateBoltzmannFactor(operativeTemp);
        return R0_ENDOTHERM_PER_YEAR * Math.pow(bodyMassKg, -0.25) * boltzmann;
    }

    /**
     * Calculates characteristic biological generation time in years:
     * \[
     * t_{\text{gen}}(M, T) = t_0 \cdot M^{1/4} \cdot \exp\left(\frac{E_a}{k_B T}\right)
     * \]
     *
     * @param bodyMassKg mean adult body mass in kilograms
     * @param tempKelvin ambient temperature in Kelvin
     * @param isEndotherm whether the taxon regulates homeothermic temperature
     * @return generation time in years
     */
    public static double calculateGenerationTimeYears(double bodyMassKg, double tempKelvin, boolean isEndotherm) {
        if (bodyMassKg <= 0.0) return 0.0;
        double operativeTemp = isEndotherm ? 310.15 : tempKelvin;
        double inverseBoltzmann = Math.exp(ACTIVATION_ENERGY_EV / (BOLTZMANN_EV_PER_K * Math.max(240.0, Math.min(325.0, operativeTemp))));
        return T0_GENERATION_YEARS * Math.pow(bodyMassKg, 0.25) * inverseBoltzmann;
    }

    /**
     * Executes one time-stepped MTE rate adjustment across populated and wilderness cells.
     * Updates baseline metabolic subsistence needs and biological rates based on local thermodynamics.
     *
     * @param cells   spatial H3 cell collection
     * @param dtYears elapsed simulation step in years
     */
    public static void processMetabolicEcology(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty() || dtYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double tempC = cell.getTemperatureOr(15.0);
            double tempK = tempC + 273.15;

            // 1. Megafauna / Wild Herbivore mass-specific metabolic dissipation
            double naturalBiomass = cell.getBiomassNaturalOr(0.0);
            if (naturalBiomass > 0.0) {
                // Typical wild herbivore mass ~150 kg (deer/bison/mammoth blend)
                double herbivoreWatts = calculateMetabolicRateWatts(150.0, tempK, TaxonGuild.ENDOTHERM_MAMMAL);
                // 1 Watt continuous = 0.0315576 GJ/year
                double annualEnergyGJ = herbivoreWatts * 0.0315576;
                // Natural respiratory attrition is thermodynamic and continuous
                double metabolicRespirationTons = (annualEnergyGJ * (naturalBiomass * 0.10) / 150.0) / 18.0 * dtYears;
                cell.setBiomassNatural(Math.max(0.1, naturalBiomass - metabolicRespirationTons * 0.05));
            }

            // 2. Domestic Livestock basal energetics
            double livestockBiomass = cell.getBiomassLivestockOr(0.0);
            if (livestockBiomass > 0.0) {
                // Typical livestock mass ~350 kg (cow/ox/horse blend)
                double livestockWatts = calculateMetabolicRateWatts(350.0, tempK, TaxonGuild.ENDOTHERM_MAMMAL);
                double annualLivestockGJ = livestockWatts * 0.0315576;
                // Energy demand for livestock maintenance reduces forage or agricultural surplus
                double forageDemandTons = (annualLivestockGJ * livestockBiomass / 350.0) / 18.0 * dtYears;
                double currentAgri = cell.getBiomassAgricultureOr(0.0);
                if (currentAgri > forageDemandTons) {
                    cell.setBiomassAgriculture(currentAgri - forageDemandTons * 0.25);
                }
            }
        }
    }
}
