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
    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double dtSafe = Math.max(1e-4, dtYears);
        double tempC = buffer.getTemperature()[i];

        // 1. Human DEB
        double biomassHuman = buffer.getBiomassHuman()[i];
        double pop = biomassHuman / HUMAN_LEAN_STRUCTURAL_MASS_TONS;
        if (pop > 0.0) {
            double foodStockGJ = buffer.getFoodResource()[i];
            double foodPerCapita = foodStockGJ / Math.max(1.0, pop);
            double f = calculateFunctionalResponse(foodPerCapita, HUMAN_FOOD_HALF_SATURATION_GJ);
            double maxCellDemandGJ = HUMAN_MAX_INGESTION_GJ_PER_YEAR * f * pop * dtSafe;
            double actualConsumedGJ = Math.min(foodStockGJ, maxCellDemandGJ);
            double pC = actualConsumedGJ / (Math.max(1.0, pop) * dtSafe);

            acc.dFoodResource -= (float) actualConsumedGJ;
            acc.dEnergyFoodConsumed += (float) actualConsumedGJ;

            double somaticAllocation = KAPPA_SOMA * pC;
            double reproductiveAllocation = (1.0 - KAPPA_SOMA) * pC;
            double coldThermoregulation = tempC < 18.0 ? 1.0 + 0.015 * (18.0 - tempC) : 1.0;
            double somaticDemand = HUMAN_SOMATIC_MAINTENANCE_GJ_PER_YEAR * coldThermoregulation;

            if (somaticAllocation >= somaticDemand) {
                double netReproductiveSurplus = Math.max(0.0, reproductiveAllocation - HUMAN_MATURITY_MAINTENANCE_GJ_PER_YEAR);
                double annualBirthRate = Math.min(0.055, netReproductiveSurplus / HUMAN_GESTATION_COST_GJ);
                acc.dBiomassHuman += (float) (biomassHuman * annualBirthRate * dtSafe);
            } else {
                double deficitPerCapitaGJ = (somaticDemand - somaticAllocation) * dtSafe;
                double structuralLossTons = deficitPerCapitaGJ / SPECIFIC_COST_STRUCTURE_GJ_PER_TON;
                double relativeEmaciation = Math.clamp(structuralLossTons / HUMAN_LEAN_STRUCTURAL_MASS_TONS, 0.0, 1.0);
                double normalizedEmaciation = Math.min(2.0, relativeEmaciation / CRITICAL_STRUCTURAL_LOSS_FRACTION);
                double starvationHazardRate = 0.35 * (normalizedEmaciation * normalizedEmaciation);
                double survivalFraction = Math.exp(-starvationHazardRate * dtSafe);
                acc.dBiomassHuman += (float) (biomassHuman * (survivalFraction - 1.0));
            }
        }

        // 2. Ecological DEB
        double naturalBiomass = buffer.getBiomassNatural()[i];
        if (naturalBiomass > 1.0) {
            double arrhenius = calculateArrheniusCorrection(tempC);
            double herbivoreBiomass = naturalBiomass * 0.12;
            double f = calculateFunctionalResponse(naturalBiomass, MEGAFAUNA_FORAGE_HALF_SAT_TONS);
            double forageIngestedGJ = herbivoreBiomass * MEGAFAUNA_MAX_INGESTION_GJ_PER_TON_YEAR * f * arrhenius * dtSafe;
            double dryVegetationConsumedTons = forageIngestedGJ / 18.0;
            double remainingVegetation = Math.max(0.5, naturalBiomass - dryVegetationConsumedTons);
            double somaticMaintenanceTotal = herbivoreBiomass * MEGAFAUNA_SOMATIC_MAINTENANCE_GJ_PER_TON_YEAR * dtSafe;
            double somaticAllocationTotal = KAPPA_SOMA * forageIngestedGJ;

            double updatedBiomass;
            if (somaticAllocationTotal >= somaticMaintenanceTotal) {
                double growthSurplusGJ = somaticAllocationTotal - somaticMaintenanceTotal;
                updatedBiomass = remainingVegetation + (growthSurplusGJ / SPECIFIC_COST_STRUCTURE_GJ_PER_TON);
            } else {
                double deficitGJ = somaticMaintenanceTotal - somaticAllocationTotal;
                updatedBiomass = Math.max(0.5, remainingVegetation - (deficitGJ / SPECIFIC_COST_STRUCTURE_GJ_PER_TON) * 0.5);
            }
            acc.dBiomassNatural += (float) (updatedBiomass - naturalBiomass);
        }
    }
}
