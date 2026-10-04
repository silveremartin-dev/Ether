/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Gompertz-Makeham Biological Actuarial Mortality Engine.
 * Replaces simple age cohorts with the fundamental Gompertz-Makeham actuarial mortality law:
 * Î¼(x) = Î± * e^(Î² * x) + Î³
 * 1. <b>Î± * e^(Î² * x)</b>: Exponential biological aging rate (cellular senescence).
 * 2. <b>Î³</b>: Environmental baseline hazard rate (famine, disease, trauma).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class BiologicalDemographicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(BiologicalDemographicsEngine.class);

    /** Baseline actuarial aging constant Î± */
    public static final double GOMPERTZ_ALPHA = 0.0001;

    /** Cellular senescence rate Î² */
    public static final double GOMPERTZ_BETA = 0.08;

    /**
     * Calculates Gompertz-Makeham hazard rate Î¼(x) for age x.
     *
     * @param ageYears Age in years
     * @param environmentalHazardGamma Baseline environmental hazard Î³ (pollution, disease)
     * @return Mortality hazard rate Î¼(x)
     */
    public static double calculateGompertzHazardRate(double ageYears, double environmentalHazardGamma) {
        return GOMPERTZ_ALPHA * Math.exp(GOMPERTZ_BETA * ageYears) + environmentalHazardGamma;
    }

    /**
     * Executes one biological demographic mortality tick across cells with dt.
     */
    public static void processBiologicalDemographics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double dt = Math.max(0.001, deltaYears);

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            double pollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;
            double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;
            double tempC = cell.getTemperature() != null ? cell.getTemperature() : 15.0;

            double naturalBiomass = cell.getBiomassNatural() != null ? cell.getBiomassNatural() : 0.0;
            double agBiomass = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
            double totalFoodGJ = Math.max(food, naturalBiomass + agBiomass);

            // Physical carrying capacity K = food available / annual metabolic requirement (3.362 GJ)
            double carryingCapacity = Math.max(0.05, totalFoodGJ / org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ);
            double stressRatio = (double) pop / carryingCapacity;

            // Famine / Nutritional stress factor (acute scaling with caloric deficit)
            double nutritionalStress = 0.0;
            if (stressRatio > 1.0) {
                double deficit = 1.0 - (1.0 / stressRatio); // ranges from 0.0 (at K) to 1.0 (extreme deficit)
                nutritionalStress = deficit * 0.15 + Math.pow(deficit, 3) * 1.50;
            }

            // Altitude Hypoxia Hazard (HAPE/AMS above 3000m, death zone above 5500m)
            double epas1 = cell.getMovementFriction() != null ? Math.clamp(1.0 - (cell.getMovementFriction() / 3.0), 0.0, 1.0) : 0.05;
            double hypoxiaHazard = 0.0;
            if (elev > 3000.0) {
                double excessElev = (elev - 3000.0) / 1000.0;
                double altitudeVulnerability = Math.max(0.1, 1.0 - epas1 * 0.85);
                hypoxiaHazard = excessElev * 0.08 * altitudeVulnerability;
            }

            // Extreme Cold Hazard (Hypothermia / Frostbite below -10Â°C)
            double coldHazard = tempC < -10.0 ? Math.min(0.25, (-10.0 - tempC) * 0.012) : 0.0;

            // Environmental hazard factor calculation for telemetry and age pyramid updates
            if (pop > 0) {
                cell.updateAgePyramidFromTotal(cell.getTechnologyLevel() != null && cell.getTechnologyLevel() > 0 ? cell.getTechnologyLevel() : 1.0);
            }
        }
    }

    public static void processBiologicalDemographics(List<H3Cell> cells) {
        processBiologicalDemographics(cells, 30.0 / 365.25);
    }
}


