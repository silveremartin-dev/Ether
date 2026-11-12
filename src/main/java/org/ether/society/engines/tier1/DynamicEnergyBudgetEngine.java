/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Standard Dynamic Energy Budget (DEB) Thermodynamic Engine.
 *
 * <p>Formalizes S.A.L.M. Kooijman's Dynamic Energy Budget theory for metabolic organisation
 * (Kooijman 2010; Jager, Martin &amp; Baveco 2013 DEBkiss; Nisbet et al. 2000) for cellular cohorts
 * and trophic guilds across contiguous spatial domains.</p>
 *
 * <p>Eliminates phenomenological carrying-capacity ceilings ($K$) and discrete starvation thresholds
 * in favor of conserved first-principle biophysical state equations:</p>
 * <ol>
 *   <li><b>State Separation</b>: Structure ($V$, functional tissues requiring somatic maintenance)
 *       vs. Reserves ($E$, chemical energy pool buffering environmental shocks).</li>
 *   <li><b>The $\kappa$-Rule</b>: Mobilized reserve flux $\dot{p}_C$ is split: fraction $\kappa$ covers
 *       somatic maintenance $\dot{p}_M$ and structure growth $\dot{p}_G$; fraction $(1 - \kappa)$
 *       covers maturity maintenance $\dot{p}_J$ and reproduction/gestation flux $\dot{p}_R$.</li>
 *   <li><b>$C^1$ Smooth Starvation Catabolism</b>: When reserves cannot cover somatic maintenance,
 *       structural biomass is catabolized ($\Delta V < 0$), triggering a continuous actuarial hazard
 *       scaled to relative emaciation $\mu_{\text{starv}} \propto (w / w_{\text{crit}})^2$.</li>
 *   <li><b>Thermodynamic Arrhenius Temperature Coupling</b>: Biological rate constants scale with ambient
 *       temperature via $k(T) = \exp(T_A / T_{\text{ref}} - T_A / T_{\text{kelvin}})$.</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://doi.org/10.1017/CBO9780511805400">Kooijman (2010), Dynamic Energy Budget theory</a>
 */
public class DynamicEnergyBudgetEngine extends AbstractSimulationEngine {
    public DynamicEnergyBudgetEngine() {
        super("DynamicEnergyBudgetEngine", "DynamicEnergyBudgetEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(DynamicEnergyBudgetEngine.class);

    // --- Universal Thermodynamic & Biological Constants ---

    /** Standard reference temperature $T_{\text{ref}} = 293.15\text{ K}$ ($20^\circ\text{C}$). */
    public static final double T_REF_KELVIN = 293.15;

    /** Arrhenius temperature for biological activation $T_A \approx 8000.0\text{ K}$ (Kooijman 2010). */
    public static final double ARRHENIUS_TEMP_K = 8000.0;

    /** Canonical somatic allocation fraction $\kappa \approx 0.80$ (80% soma, 20% reproduction/maturity). */
    public static final double KAPPA_SOMA = 0.80;

    /** Specific structural synthesis cost $[E_G] \approx 28.0\text{ GJ/metric ton}$ ($\approx 28\text{ MJ/kg}$). */
    public static final double SPECIFIC_COST_STRUCTURE_GJ_PER_TON = 28.0;

    /** Maximum tolerable relative structural loss before 100% mortality $w_{\text{crit}} \approx 0.40$ (Cahill 1970). */
    public static final double CRITICAL_STRUCTURAL_LOSS_FRACTION = 0.40;

    // --- Human DEB Parameters ---

    /** Healthy adult human structural lean mass in metric tons ($\approx 50\text{ kg} = 0.050\text{ t}$). */
    public static final double HUMAN_LEAN_STRUCTURAL_MASS_TONS = 0.050;

    /** Baseline human somatic maintenance demand $[\dot{p}_M] \approx 2.25\text{ GJ/(person}\cdot\text{year)}$ ($\approx 1475\text{ kcal/day}$, FAO/WHO 2004). */
    public static final double HUMAN_SOMATIC_MAINTENANCE_GJ_PER_YEAR = 2.25;

    /** Maximum human food ingestion rate per capita $\{\dot{p}_{Am}\} \approx 4.80\text{ GJ/(person}\cdot\text{year)}$ ($\approx 3140\text{ kcal/day}$). */
    public static final double HUMAN_MAX_INGESTION_GJ_PER_YEAR = 4.80;

    /** Half-saturation constant for human food resource density $X_K \approx 1.20\text{ GJ/person}$. */
    public static final double HUMAN_FOOD_HALF_SATURATION_GJ = 1.20;

    /** Bioenergetic gestation and lactation cost per live birth $E_{\text{gestation}} \approx 0.80\text{ GJ}$. */
    public static final double HUMAN_GESTATION_COST_GJ = 0.80;

    /** Maturity/immune maintenance demand $[\dot{p}_J] \approx 0.15\text{ GJ/(person}\cdot\text{year)}$. */
    public static final double HUMAN_MATURITY_MAINTENANCE_GJ_PER_YEAR = 0.15;

    // --- Megafauna / Herbivore DEB Parameters ---

    /** Typical megafauna herbivore body mass in metric tons ($\approx 1.5\text{ t}$). */
    public static final double MEGAFAUNA_MEAN_BODY_MASS_TONS = 1.5;

    /** Megafauna somatic maintenance demand $[\dot{p}_{M,\text{herb}}] \approx 12.0\text{ GJ/(ton}\cdot\text{year)}$. */
    public static final double MEGAFAUNA_SOMATIC_MAINTENANCE_GJ_PER_TON_YEAR = 12.0;

    /** Megafauna maximum forage assimilation $\{\dot{p}_{Am,\text{herb}}\} \approx 20.0\text{ GJ/(ton}\cdot\text{year)}$. */
    public static final double MEGAFAUNA_MAX_INGESTION_GJ_PER_TON_YEAR = 20.0;

    /** Forage density half-saturation constant $B_{1/2} \approx 50.0\text{ tons/cell}$. */
    public static final double MEGAFAUNA_FORAGE_HALF_SAT_TONS = 50.0;

    /**
     * Calculates the Arrhenius thermodynamic temperature correction factor:
     * \[
     * k(T) = \exp\left(\frac{T_A}{T_{\text{ref}}} - \frac{T_A}{T}\right)
     * \]
     * bounded within physically viable biological limits $[0.05, 3.0]$.
     *
     * @param tempCelsius ambient temperature in $^\circ\text{C}$
     * @return dimensionless Arrhenius kinetic multiplier
     */
    public static double calculateArrheniusCorrection(double tempCelsius) {
        double tempKelvin = Math.max(240.0, Math.min(325.0, tempCelsius + 273.15));
        double factor = Math.exp((ARRHENIUS_TEMP_K / T_REF_KELVIN) - (ARRHENIUS_TEMP_K / tempKelvin));
        return Math.clamp(factor, 0.05, 3.0);
    }

    /**
     * Calculates Holling Type II (Michaelis-Menten) scaled functional response:
     * \[
     * f(X) = \frac{X}{X + X_K}
     * \]
     *
     * @param resourceStock available consumable food stock $X$
     * @param halfSat       half-saturation constant $X_K$
     * @return dimensionless ingestion saturation fraction $f \in [0, 1)$
     */
    public static double calculateFunctionalResponse(double resourceStock, double halfSat) {
        if (resourceStock <= 0.0 || halfSat <= 0.0) return 0.0;
        return resourceStock / (resourceStock + halfSat);
    }

    /**
     * Executes one time-stepped Dynamic Energy Budget update for human populations across cells.
     * <p>
     * Connects available food stocks ($E_{\text{food}}$) directly to somatic maintenance, growth,
     * biological gestation/births, and continuous $C^1$ starvation wasting mortality.
     * </p>
     *
     * @param cells   populated cell domain
     * @param dtYears elapsed simulation step in years ($\Delta t$)
     */
    public static void processHumanDEB(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty() || dtYears <= 0.0) return;

        double dtSafe = Math.max(1e-4, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double tempC = cell.getTemperatureOr(15.0);

            double foodStockGJ = cell.getFoodResourceOr(0.0);
            double foodPerCapita = foodStockGJ / Math.max(1.0, pop);

            // 1. Scaled Functional Response & Assimilation
            // Humans are homeothermic endotherms (core ~37A°C); ambient cold increases thermoregulation load
            double f = calculateFunctionalResponse(foodPerCapita, HUMAN_FOOD_HALF_SATURATION_GJ);
            double potentialAssimilationPerCapita = HUMAN_MAX_INGESTION_GJ_PER_YEAR * f;

            // Actual consumed food cannot exceed available physical stockpile
            double maxCellDemandGJ = potentialAssimilationPerCapita * pop * dtSafe;
            double actualConsumedGJ = Math.min(foodStockGJ, maxCellDemandGJ);
            double effectiveRatePerCapita = actualConsumedGJ / (Math.max(1.0, pop) * dtSafe);

            // Update remaining food resource and track consumption
            cell.setFoodResource(Math.max(0.0, foodStockGJ - actualConsumedGJ));
            cell.setEnergyFoodConsumed(cell.getEnergyFoodConsumedOr(0.0) + actualConsumedGJ);

            // 2. Mobilization & The Kappa-Rule Allocation
            // Mobilized energy from food assimilation: p_C = effectiveRatePerCapita
            double pC = effectiveRatePerCapita;
            double somaticAllocation = KAPPA_SOMA * pC;
            double reproductiveAllocation = (1.0 - KAPPA_SOMA) * pC;

            // Somatic Maintenance Demand (scaled slightly by cold thermoregulation below 18A°C)
            double coldThermoregulation = tempC < 18.0 ? 1.0 + 0.015 * (18.0 - tempC) : 1.0;
            double somaticDemand = HUMAN_SOMATIC_MAINTENANCE_GJ_PER_YEAR * coldThermoregulation;

            if (somaticAllocation >= somaticDemand) {
                // Surplus regime: full maintenance satisfied, reproductive flux drives fertility
                double maturityMaintenance = HUMAN_MATURITY_MAINTENANCE_GJ_PER_YEAR;
                double netReproductiveSurplus = Math.max(0.0, reproductiveAllocation - maturityMaintenance);

                // Endogenous biological births emerging from caloric gestation surplus
                double annualBirthRate = Math.min(0.055, netReproductiveSurplus / HUMAN_GESTATION_COST_GJ);
                int births = (int) Math.round(pop * annualBirthRate * dtSafe);
                if (births > 0) {
                    int newPop = pop + births;
                    cell.setPopulation(newPop);
                    cell.setBiomassHuman(newPop * HUMAN_LEAN_STRUCTURAL_MASS_TONS);
                    H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, newPop);
                    pop = newPop;
                }
            } else {
                // Deficit regime: acute nutritional stress / starvation
                // Structural tissue catabolism pays the remaining somatic maintenance deficit
                double deficitPerCapitaGJ = (somaticDemand - somaticAllocation) * dtSafe;
                double structuralLossTons = deficitPerCapitaGJ / SPECIFIC_COST_STRUCTURE_GJ_PER_TON;
                double relativeEmaciation = Math.clamp(structuralLossTons / HUMAN_LEAN_STRUCTURAL_MASS_TONS, 0.0, 1.0);

                // Continuous C^1 starvation mortality hazard:
                // Actuarial risk increases quadratically with emaciation relative to Cahill critical threshold (Fogel 2004; Sen 1981)
                double normalizedEmaciation = Math.min(2.0, relativeEmaciation / CRITICAL_STRUCTURAL_LOSS_FRACTION);
                double starvationHazardRate = 0.35 * (normalizedEmaciation * normalizedEmaciation);

                double survivalFraction = Math.exp(-starvationHazardRate * dtSafe);
                int survivingPop = (int) Math.round(pop * survivalFraction);

                if (survivingPop < pop) {
                    int deaths = pop - survivingPop;
                    cell.setPopulation(survivingPop);
                    cell.setBiomassHuman(survivingPop * HUMAN_LEAN_STRUCTURAL_MASS_TONS);
                    H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, survivingPop);
                    pop = survivingPop;
                }
            }

            if (pop > 0) {
                cell.updateAgePyramidFromObservables();
            }
        }
    }

    /**
     * Executes one time-stepped Dynamic Energy Budget update for wild megafaunal and herbivore biomass.
     * <p>
     * Replaces phenomenological Verhulst capacity ($K_{\text{veg}}$) with explicit mass-energy conservation
     * between primary producers (natural biomass $B_{\text{nat}}$) and primary consumers ($B_{\text{herb}}$).
     * </p>
     *
     * @param cells   land cell domain
     * @param dtYears elapsed simulation step in years ($\Delta t$)
     */
    public static void processEcologicalDEB(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty() || dtYears <= 0.0) return;

        double dtSafe = Math.max(1e-4, dtYears);

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            double naturalBiomass = cell.getBiomassNaturalOr(0.0);
            if (naturalBiomass <= 1.0) continue;

            double tempC = cell.getTemperatureOr(15.0);
            double arrhenius = calculateArrheniusCorrection(tempC);

            // Herbivore guild occupies a natural trophic fraction of natural biomass (~10-15%)
            double herbivoreBiomass = naturalBiomass * 0.12;

            // 1. Scaled Forage Functional Response
            double f = calculateFunctionalResponse(naturalBiomass, MEGAFAUNA_FORAGE_HALF_SAT_TONS);
            double assimilationRatePerTon = MEGAFAUNA_MAX_INGESTION_GJ_PER_TON_YEAR * f * arrhenius;

            // Total forage ingested in Gigajoules
            double forageIngestedGJ = herbivoreBiomass * assimilationRatePerTon * dtSafe;
            // 1 metric ton dry plant matter ~ 18.0 GJ
            double dryVegetationConsumedTons = forageIngestedGJ / 18.0;

            // Deplete primary producer biomass by actual forage eaten
            double remainingVegetation = Math.max(0.5, naturalBiomass - dryVegetationConsumedTons);

            // 2. Kappa-rule allocation to somatic maintenance vs growth/recruitment
            double somaticMaintenanceTotal = herbivoreBiomass * MEGAFAUNA_SOMATIC_MAINTENANCE_GJ_PER_TON_YEAR * dtSafe;
            double somaticAllocationTotal = KAPPA_SOMA * forageIngestedGJ;

            if (somaticAllocationTotal >= somaticMaintenanceTotal) {
                // Surplus: recruitment adds net biomass to ecosystem
                double growthSurplusGJ = somaticAllocationTotal - somaticMaintenanceTotal;
                double newBiomassTons = growthSurplusGJ / SPECIFIC_COST_STRUCTURE_GJ_PER_TON;
                cell.setBiomassNatural(remainingVegetation + newBiomassTons);
            } else {
                // Deficit: starvation catabolism and natural herbivore attrition
                double deficitGJ = somaticMaintenanceTotal - somaticAllocationTotal;
                double biomassLostTons = deficitGJ / SPECIFIC_COST_STRUCTURE_GJ_PER_TON;
                cell.setBiomassNatural(Math.max(0.5, remainingVegetation - biomassLostTons * 0.5));
            }
        }
    }
}
