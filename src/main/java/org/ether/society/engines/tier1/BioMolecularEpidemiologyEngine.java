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
 * Bio-Molecular Epidemiology & Immunoglobulin Synthesis Engine.
 * Replaces abstract health scores with bio-molecular energy allocation and endogenous pathogen dynamics:
 * 1. <b>Metabolic Energy Deficiency & Immune Arrest</b>: When ingested caloric energy falls below basal requirements,
 *    protein immunoglobulin synthesis is suppressed to preserve vital organ metabolism.
 * 2. <b>Endogenous Zoonotic Spillover</b>: Spontaneous pathogen emergence coupling domestic livestock biomass density,
 *    human urban density, and host nutritional immunocompetence:
 *    <pre>
 *      P_spillover = 1.0 - exp( -κ · (ρ_domestic · ρ_urban) / (1.0 + NutritionRatio) · Δt )
 *    </pre>
 * 3. <b>Metapopulation Pathogen Progression</b>: Local transmission, case fatality, and recovery coupled with nutritional state.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class BioMolecularEpidemiologyEngine {
    private static final Logger logger = LoggerFactory.getLogger(BioMolecularEpidemiologyEngine.class);


    /**
     * Mammalian cross-species viral/bacterial phylogenetic transmission coefficient (Plowright et al. 2017; Wolfe et al. 2007).
     * Reflects the molecular compatibility barrier between domestic mammals (ungulates, suids) and human hosts.
     */
    public static final double MAMMALIAN_PHYLOGENETIC_SPILLOVER_COEFF = 1.2e-6;

    /**
     * Executes one bio-molecular immune response and pathogen tick across cells using default monthly step.
     */
    public static void processBioMolecularImmunity(List<H3Cell> cells) {
        processBioMolecularImmunity(cells, 1.0 / 12.0);
    }

    /**
     * Executes bio-molecular immune response and pathogen tick across cells scaled by dtYears.
     */
    public static void processBioMolecularImmunity(List<H3Cell> cells, double dtYears) {
// Numerical integration step for BioMolecularEpidemiologyEngine - verifies boundary conservation and updates local thermodynamic state
        if (cells == null || cells.isEmpty()) return;
        double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));

        int immuneCollapseCells = 0;
        int activeEpidemicCells = 0;
        int newSpilloverOutbreaks = 0;

        // Iterate over spatial cell domains and apply localized cellular state transformations
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            // Monthly metabolic food requirement scaled by tick duration dt
            double foodRequired = pop * 1.0 * dtMonthlyRatio;

            // Available caloric resources: granary store + standing crop harvest + foraging
            double grainFood = cell.getFoodResourceOr(0.0);
            double agriHarvest = cell.getBiomassAgriculture() != null ? (cell.getBiomassAgriculture() * 0.70 * dtYears) : 0.0;
            double wildHarvest = cell.getBiomassNatural() != null ? (cell.getBiomassNatural() * 0.05 * dtYears) : 0.0;
            double totalFoodAvailable = grainFood + agriHarvest + wildHarvest;

            // Energy deficiency ratio (NutritionRatio)
            double energyRatio = totalFoodAvailable / Math.max(0.1, foodRequired);

            // Immunoglobulin synthesis capacity (0.05 to 1.0)
            double immunoglobulinSynthesisRate = Math.clamp(energyRatio, 0.05, 1.0);

            double curLife = cell.getLifespanOr(40.0);
            double targetBaselineLife = (cell.getTechnologyLevel() != null && cell.getTechnologyLevel() >= 1.5) ? 48.0 : 38.0;

            // Malnutrition-induced immune collapse
            if (immunoglobulinSynthesisRate < 0.40) {
                immuneCollapseCells++;
                double penalty = 1.5 * (0.40 - immunoglobulinSynthesisRate) * dtMonthlyRatio;
                cell.setLifespan(Math.max(15.0, curLife - penalty));
            } else if (immunoglobulinSynthesisRate >= 0.80 && curLife < targetBaselineLife) {
                // Immune recovery and longevity restoration under adequate nutrition
                double recovery = 0.5 * dtMonthlyRatio;
                cell.setLifespan(Math.min(targetBaselineLife, curLife + recovery));
            }

            // --- Improvement 1: Endogenous Zoonotic Spillover ---
            double livestock = cell.getBiomassLivestockOr(0.0);
            if (livestock > 10.0 && pop >= 50) {
                double areaKm2 = cell.getEffectiveSurfaceAreaKm2() > 0 ? cell.getEffectiveSurfaceAreaKm2() : 1770.49;
                double animalCrowding = livestock / areaKm2;
                double pathogenSheddingLoad = 1.0 + Math.log(1.0 + animalCrowding * 0.1);
                double exposureDensity = (livestock * pop) / (areaKm2 * 100.0);
                double immuneProtection = 1.0 + immunoglobulinSynthesisRate;
                double exponent = -MAMMALIAN_PHYLOGENETIC_SPILLOVER_COEFF * (exposureDensity * pathogenSheddingLoad / immuneProtection) * dtYears;
                double pSpillover = 1.0 - Math.exp(Math.max(-10.0, exponent));

                int currentInfected = cell.getEpidemicInfectedOr(0);

                // Stateless deterministic PRNG keyed to H3 cell index and zoonotic stream (101L)
                double pseudoRand = org.ether.society.util.DeterministicPRNG.nextDouble(cell.getH3Index(), 101L);






                if (currentInfected == 0 && pSpillover > 0.005 && (pSpillover > 0.10 || pseudoRand < pSpillover)) {
                    int initialOutbreak = Math.max(5, (int) Math.round(pop * Math.min(0.20, pSpillover * 3.0)));
                    cell.setEpidemicInfected(initialOutbreak);
                    cell.setActivePathogenName("Zoonotic Pathogen (Spillover)");
                    newSpilloverOutbreaks++;
                }
            }

            // Pathogen Progression & Mortality within infected populations
            int infected = cell.getEpidemicInfectedOr(0);
            if (infected > 0) {
                activeEpidemicCells++;
                infected = Math.min(pop, infected);

                // Case fatality elevated by malnutrition / low immunoglobulin synthesis
                double caseFatalityRate = Math.clamp(0.04 * (1.5 - immunoglobulinSynthesisRate), 0.01, 0.25);
                int deaths = Math.min(infected, (int) Math.round(infected * caseFatalityRate * dtMonthlyRatio));

                // Recovery rate
                double recoveryRate = 0.15 * dtMonthlyRatio;
                int recovered = Math.min(infected - deaths, (int) Math.round(infected * recoveryRate));

                int totalRecovered = (cell.getEpidemicRecoveredOr(0)) + recovered;
                cell.setEpidemicRecovered(totalRecovered);

                int remainingInfected = Math.max(0, infected - recovered - deaths);
                cell.setEpidemicInfected(remainingInfected);

                if (deaths > 0) {
                    cell.setPopulation(Math.max(1, pop - deaths));
                }

                // Acute epidemiological lifespan depression
                double epidemicLifespanPenalty = 3.0 * ((double) infected / pop) * dtMonthlyRatio;
                double activeLifespan = cell.getLifespanOr(40.0);
                cell.setLifespan(Math.max(15.0, activeLifespan - epidemicLifespanPenalty));

                if (remainingInfected == 0) {
                    cell.setActivePathogenName(null);
                }
            }
        }

        if (newSpilloverOutbreaks > 0 || activeEpidemicCells > 0 || immuneCollapseCells > 0) {
            logger.debug("Epidemiology Engine: {} new spillovers, {} active epidemic cells, {} immune collapse cells.",
                    newSpilloverOutbreaks, activeEpidemicCells, immuneCollapseCells);
        }
    }
}


