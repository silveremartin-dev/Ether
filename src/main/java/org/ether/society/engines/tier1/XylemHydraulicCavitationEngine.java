/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Plant Xylem Hydraulic Cavitation & Runaway Embolism Engine.
 *
 * <p>Formalizes the biophysical principles of vascular plant water transport and drought-induced
 * hydraulic failure established by Tyree &amp; Sperry (1989), Choat et al. (2012, <i>Nature</i>),
 * and McDowell et al. (2008).</p>
 *
 * <p>Key Biophysical Mechanics:</p>
 * <ol>
 *   <li><b>Vulnerability Curve (Weibull / Logistic)</b>:
 *       \[
 *       \text{PLC}(\Psi_{\text{soil}}) = \frac{100.0}{1.0 + \left(\frac{|\Psi_{\text{soil}}|}{|\Psi_{50}|}\right)^a}
 *       \]
 *       where \(\text{PLC}\) is the Percentage Loss of Hydraulic Conductivity (\(\%\)),
 *       \(\Psi_{50}\) is the species/biome-specific water potential causing \(50\%\) embolism in \(\text{MPa}\),
 *       and \(a \approx 3.5\) is the steepness shape parameter.</li>
 *   <li><b>Soil Matric Potential Coupling (Campbell 1974 / van Genuchten 1980)</b>:
 *       \[
 *       \Psi_{\text{soil}}(S_w) = -\Psi_e \cdot S_w^{-b} \quad [\text{MPa}]
 *       \]
 *       linking relative soil moisture saturation \(S_w \in (0, 1]\) to suction potential.</li>
 *   <li><b>Runaway Cavitation &amp; Sudden Canopy Dieback</b>:
 *       When \(\text{PLC} > 60\%\), xylem tension causes irreversible air bubble seeding (embolism),
 *       choking transpiration and driving quadratic agricultural harvest loss and forest tree mortality:
 *       \[
 *       \mu_{\text{dieback}} = \mu_{\max} \cdot \left(\frac{\max(0, \text{PLC} - 50.0)}{50.0}\right)^2
 *       \]</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.1038/nature11688">Choat et al. (2012), Nature 491</a>
 * @see <a href="https://doi.org/10.1146/annurev.pp.40.060189.000315">Tyree &amp; Sperry (1989)</a>
 */
public class XylemHydraulicCavitationEngine extends AbstractSimulationEngine {
    public XylemHydraulicCavitationEngine() {
        super("XylemHydraulicCavitationEngine", "XylemHydraulicCavitationEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(XylemHydraulicCavitationEngine.class);

    // --- Universal Biophysical Constants & Xylem Safety Margins ---

    /** Air-entry suction potential for standard loam soils in MPa (\(\Psi_e \approx 0.035\text{ MPa}\)). */
    public static final double AIR_ENTRY_SUCTION_MPA = 0.035;

    /** Campbell soil water retention exponent \(b \approx 4.5\) for agricultural soils. */
    public static final double CAMPBELL_B_EXPONENT = 4.5;

    /** Vulnerability sigmoidal slope exponent \(a \approx 3.5\) (Choat et al. 2012). */
    public static final double VULNERABILITY_SHAPE_EXPONENT = 3.5;

    /** Agricultural crop (cereal) critical water potential \(\Psi_{50,\text{crop}} \approx -2.50\text{ MPa}\). */
    public static final double PSI_50_CROPS_MPA = -2.50;

    /** Temperate deciduous broadleaf forest \(\Psi_{50,\text{temp}} \approx -4.20\text{ MPa}\). */
    public static final double PSI_50_TEMPERATE_FOREST_MPA = -4.20;

    /** Boreal conifer needleleaf forest \(\Psi_{50,\text{boreal}} \approx -5.80\text{ MPa}\). */
    public static final double PSI_50_BOREAL_FOREST_MPA = -5.80;

    /** Sclerophyllous Mediterranean / Arid shrubland \(\Psi_{50,\text{arid}} \approx -8.50\text{ MPa}\). */
    public static final double PSI_50_ARID_SHRUB_MPA = -8.50;

    /** Maximum annual forest drought canopy dieback rate under complete cavitation (\(\mu_{\max} \approx 0.40\text{ yr}^{-1}\)). */
    public static final double MAX_CANOPY_DIEBACK_RATE = 0.40;

    /**
     * Estimates soil matric water potential \(\Psi_{\text{soil}}\) in \(\text{MPa}\) from soil water saturation.
     *
     * @param moistureFraction relative soil moisture saturation \(S_w \in [0.01, 1.0]\)
     * @return negative soil water potential in \(\text{MPa}\) (e.g. \(-0.05\text{ MPa}\) wet, \(-4.0\text{ MPa}\) arid)
     */
    public static double estimateSoilWaterPotentialMPa(double moistureFraction) {
        double safeSat = Math.clamp(moistureFraction, 0.02, 1.0);
        return -AIR_ENTRY_SUCTION_MPA * Math.pow(safeSat, -CAMPBELL_B_EXPONENT);
    }

    /**
     * Calculates the Percentage Loss of Xylem Hydraulic Conductivity (\(\text{PLC}\)):
     * \[
     * \text{PLC} = \frac{100.0}{1.0 + \left(\frac{|\Psi_{\text{soil}}|}{|\Psi_{50}|}\right)^a}
     * \]
     *
     * @param psiSoilMPa soil matric water potential in \(\text{MPa}\) (negative)
     * @param psi50MPa   critical water potential causing 50% loss of conductivity in \(\text{MPa}\) (negative)
     * @param slope      curve steepness exponent \(a\)
     * @return percentage loss of conductivity in \([0.0, 100.0]\%\)
     */
    public static double calculatePLC(double psiSoilMPa, double psi50MPa, double slope) {
        double absPsi = Math.abs(psiSoilMPa);
        double absPsi50 = Math.max(0.1, Math.abs(psi50MPa));
        double safeSlope = Math.max(1.0, slope);

        double ratio = absPsi / absPsi50;
        // Invert logistic function: at absPsi << absPsi50, PLC ~ 0%; at absPsi = absPsi50, PLC = 50%; at absPsi >> absPsi50, PLC -> 100%
        double denominator = 1.0 + Math.pow(ratio, -safeSlope);
        return Math.clamp(100.0 / denominator, 0.0, 100.0);
    }

    /**
     * Alias for {@link #calculatePLC(double, double, double)}.
     *
     * @param psiSoilMPa soil matric water potential in \(\text{MPa}\)
     * @param psi50MPa   critical water potential causing 50% loss of conductivity
     * @param slope      curve steepness exponent
     * @return percentage loss of conductivity in \([0, 100]\%\)
     */
    public static double calculatePercentageLossOfConductivity(double psiSoilMPa, double psi50MPa, double slope) {
        return calculatePLC(psiSoilMPa, psi50MPa, slope);
    }

    /**
     * Calculates relative hydraulic conductivity retention fraction \(k_{\text{xylem}} \in [0, 1]\).
     *
     * @param plcPercent percentage loss of conductivity in \([0, 100]\)
     * @return fractional water transport capacity remaining
     */
    public static double calculateHydraulicRetention(double plcPercent) {
        return Math.clamp(1.0 - (plcPercent / 100.0), 0.0, 1.0);
    }

    /**
     * Retrieves the characteristic xylem vulnerability threshold \(\Psi_{50}\) for a given biome.
     *
     * @param biome terrestrial biome classification
     * @return critical water potential in \(\text{MPa}\)
     */
    public static double getBiomePsi50MPa(Biome biome) {
        if (biome == null) return PSI_50_CROPS_MPA;
        return switch (biome) {
            case TUNDRA, SNOW -> PSI_50_BOREAL_FOREST_MPA;
            case DESERT -> PSI_50_ARID_SHRUB_MPA;
            case FOREST, JUNGLE -> PSI_50_TEMPERATE_FOREST_MPA;
            case PLAINS, SAVANNAH, HILLS, MOUNTAINS -> PSI_50_CROPS_MPA;
            default -> PSI_50_CROPS_MPA;
        };
    }

    /**
     * Alias for {@link #getBiomePsi50MPa(Biome)}.
     *
     * @param biome terrestrial biome classification
     * @return critical water potential in \(\text{MPa}\)
     */
    public static double getCriticalWaterPotentialMPa(Biome biome) {
        return getBiomePsi50MPa(biome);
    }

    /**
     * Executes one time-stepped xylem hydraulic cavitation update across terrestrial cells.
     * <p>
     * Throttles agricultural crop yield and induces catastrophic canopy dieback during megadroughts
     * when soil water potential drops below hydraulic safety margins.
     * </p>
     *
     * @param cells   terrestrial cell domain
     * @param dtYears elapsed simulation step in years
     */
    public static void processXylemCavitation(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty() || dtYears <= 0.0) return;

        double dtSafe = Math.max(1e-4, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double rainfall = cell.getRainfallOr(500.0);
            double tempC = cell.getTemperatureOr(15.0);

            // 1. Soil Moisture Saturation Approximation from Water Balance
            // Mean evapotranspiration demand ~ 600 mm/yr at 15A°C
            double petDemand = Math.max(100.0, (tempC + 10.0) * 25.0);
            double aridityIndex = rainfall / petDemand;
            double moistureSaturation = Math.clamp(aridityIndex * 0.75, 0.02, 1.0);

            double psiSoil = estimateSoilWaterPotentialMPa(moistureSaturation);

            // 2. Crop Hydraulic Failure & Food Supply Impact
            double agriBiomass = cell.getBiomassAgricultureOr(0.0);
            if (agriBiomass > 0.0) {
                double plcCrop = calculatePLC(psiSoil, PSI_50_CROPS_MPA, VULNERABILITY_SHAPE_EXPONENT);
                double retention = calculateHydraulicRetention(plcCrop);

                // Runaway cavitation threshold (PLC > 60% causes non-linear vascular collapse)
                if (plcCrop > 60.0) {
                    double cropLossFactor = Math.pow((plcCrop - 60.0) / 40.0, 1.5) * 0.85 * dtSafe;
                    double survivingAgri = Math.max(0.0, agriBiomass * (1.0 - cropLossFactor));
                    cell.setBiomassAgriculture(survivingAgri);

                    // Throttles available food resources proportionately to agricultural collapse
                    double currentFood = cell.getFoodResourceOr(0.0);
                    cell.setFoodResource(Math.max(0.0, currentFood * (1.0 - cropLossFactor * 0.5)));
                }
            }

            // 3. Natural Forest & Vegetation Cavitation Dieback
            double naturalBiomass = cell.getBiomassNaturalOr(0.0);
            if (naturalBiomass > 1.0) {
                Biome biome = cell.getBiome();
                double psi50 = getBiomePsi50MPa(biome);
                double plcForest = calculatePLC(psiSoil, psi50, VULNERABILITY_SHAPE_EXPONENT);

                if (plcForest > 50.0) {
                    double normalizedStress = (plcForest - 50.0) / 50.0;
                    double diebackRate = MAX_CANOPY_DIEBACK_RATE * (normalizedStress * normalizedStress);
                    double survivingBiomass = naturalBiomass * Math.exp(-diebackRate * dtSafe);
                    cell.setBiomassNatural(Math.max(0.5, survivingBiomass));
                }
            }
        }
    }
}
