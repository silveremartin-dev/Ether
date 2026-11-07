/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Jevons Paradox & Rebound Effect Engine (Jancovici & Bihouix Rebound Law).
 * Models how technological efficiency gains increase total aggregate resource consumption (William Stanley Jevons, 1865; Alcott, 2005).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class JevonsParadoxEngine {
    private static final Logger logger = LoggerFactory.getLogger(JevonsParadoxEngine.class);

    private static double reboundCoefficient = 0.80; // 80% rebound effect

    /**
     * Characteristic physical transducer capital $K_{\text{Jevons}} = 350.0\,\text{kg/capita}$ (waterwheels, furnaces,
     * early steam engines; Jevons 1865, <i>The Coal Question</i>; Saunders 1992, Khazzoom-Brookes postulate).
     */
    public static final double JEVONS_HALF_SATURATION_CAPITAL_KG_PC = 350.0;

    /**
     * Process Jevons rebound across spatial cells with continuous $C^1$ physical transducer efficiency activation.
     *
     * <p>Grounded in the Khazzoom-Brookes postulate (Saunders 1992) and Ayres-Warr (2009) thermodynamic conversion
     * efficiency $\eta_{\text{transducer}}(k_{\text{pc}})$: as physical machinery density $k_{\text{pc}} = K/N$ rises,
     * the marginal cost of useful work falls, triggering a rebound in aggregate primary energy demand.</p>
     *
     * @param cells the spatial cells list
     * @param deltaYears time step in years
     */
    public static void processJevonsRebound(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;

            int pop = Math.max(1, cell.getPopulationOr(100));
            double kPerCapita = cell.getResourceCapitalOr(0.0) / pop;

            // Continuous C^1 Hill transducer efficiency factor eta_eff in [0, 1] driven by physical machinery k_pc
            double kRatio = kPerCapita / JEVONS_HALF_SATURATION_CAPITAL_KG_PC;
            double transducerEfficiencyFactor = (kRatio * kRatio) / (1.0 + kRatio * kRatio);
            if (transducerEfficiencyFactor > 0.05) {
                // Log-machinery intensity replaces synthetic tech scalar: log(1 + k_pc / 50)
                double machineryIntensity = Math.log1p(kPerCapita / 50.0);
                double reboundDemand = machineryIntensity * reboundCoefficient * transducerEfficiencyFactor * 0.005 * deltaYears;

                // Boost electrical/mechanical capacity in the cell (industrial exergy rebound)
                double habitatKw = cell.getHabitatEnergyKwOr(0.0);
                cell.setHabitatEnergyKw(habitatKw + reboundDemand * 100.0);

                // Biologically bounded somatic food intake: human metabolic limits (~2500 kcal/day)
                // prevent runaway somatic calorie consumption
                double currentFoodEnergy = cell.getEnergyFoodConsumedOr(0.0);
                double maxMetabolicFood = Math.max(10.0, pop * 1.5);
                cell.setEnergyFoodConsumed(Math.min(maxMetabolicFood, currentFoodEnergy + reboundDemand));
            }
        }
    }

    /**
     * Get rebound coefficient.
     *
     * @return the resulting computation or state reference
     */
    public static double getReboundCoefficient() { return reboundCoefficient; }

    /**
     * Set rebound coefficient operation.
     *
     * @param coeff the coeff argument (double)
     */
    public static void setReboundCoefficient(double coeff) { reboundCoefficient = Math.max(0.0, Math.min(2.0, coeff)); }

    /**
     * Resets rebound coefficient for scenario isolation.
     */
    public static void resetState() {
        reboundCoefficient = 0.80;
    }
}
