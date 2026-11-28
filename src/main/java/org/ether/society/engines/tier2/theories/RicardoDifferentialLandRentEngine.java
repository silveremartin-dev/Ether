/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;

/**
 * David Ricardo Differential Land Rent &amp; Extensive Agricultural Margin Engine (Ricardo 1817; Samuelson 1959).
 *
 * <p>Implements the classical Ricardian political economy of diminishing marginal productivity on fixed land
 * and differential rent extraction (Ricardo 1817, Ch. 2; Samuelson 1959, <i>Quarterly Journal of Economics</i> 73(1): 1–35):</p>
 * <ul>
 *   <li><b>Diminishing Marginal Product on Fixed Land</b>: For agricultural output \(Y_A = B_{\text{agr}}\) under
 *       labor-land elasticity \(\beta_A \in (0, 1)\), average product is \(y_{\text{avg}} = Y_A / L\) and marginal product
 *       on the extensive/intensive margin is \(MP_L = \beta_A \, (Y_A / L) < y_{\text{avg}}\).</li>
 *   <li><b>Differential Land Rent</b>: Landlords capture the surplus between intramarginal output and the
 *       marginal product of labor: \(R_{\text{land}} = Y_A - L \cdot MP_L = (1 - \beta_A) Y_A\).</li>
 *   <li><b>Ricardian Stationary Profit Squeeze</b>: As demographic pressure lowers \(MP_L\) below the corn
 *       subsistence wage \(w_{\text{sub}}\) (\(\theta_w = w_{\text{sub}} / MP_L > 1\)), rising corn prices compress
 *       capitalist profit \(r = r_0 (1 - w_{\text{sub}} / MP_L)\), retarding capital accumulation and elevating Gini rent share.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class RicardoDifferentialLandRentEngine extends AbstractSimulationEngine {

    /** Classical agricultural labor elasticity \(\beta_A \in (0, 1)\) under fixed land factor (\(1 - \beta_A = 0.35\) landlord rent share). */
    public static final double AGRICULTURAL_LABOR_ELASTICITY_BETA = 0.65;

    /** Subsistence grain wage norm \(w_{\text{sub}}\) in \(\text{kg/person}\) of standing crop equivalent. */
    public static final double SUBSISTENCE_CORN_WAGE_KG = 0.78;

    /** Minimum landed estate capital per capita where institutionalized landlord rent extraction operates (\(\text{kg/cap}\)). */
    public static final double RENT_KPC_FLOOR = 40.0;

    /**
     * Constructs the Ricardian differential land rent engine.
     */
    public RicardoDifferentialLandRentEngine() {
        super("RicardoDifferentialLandRentEngine", "RicardoDifferentialLandRentEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public String getName() {
        return "RicardoDifferentialLandRentEngine";
    }

    @Override
    public String getDescription() {
        return "Models David Ricardo (1817) & Samuelson (1959) differential land rent: diminishing marginal returns on scarce land elevate landlord rent shares and squeeze capitalist profit.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [David Ricardo Differential Land Rent (Ricardo 1817; Samuelson 1959)]
               • Marginal Product:     MP_L = β_A · (Y_agr / L),  0 < β_A < 1
               • Differential Rent:    R_land = Y_agr - L · MP_L = (1 - β_A) · Y_agr
               • Corn-Wage Squeeze:    θ_w = w_sub / MP_L
               • Profit Compression:   r_net = r_0 · (1 - θ_w)  => Stationary state as MP_L -> w_sub
               Parameters: β_A = 0.65, w_sub = 0.78 kg/cap
               Ref: D. Ricardo (1817) "Principles of Political Economy"; P. A. Samuelson (1959, QJE 73:1-35)
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Classical Macroeconomics";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || dtYears <= 0.0) return;

        double pop = buffer.getHumanPopulation()[i];
        if (pop <= 20.0) return;

        double agriBiomass = buffer.getBiomassAgriculture()[i];
        if (agriBiomass <= 50.0) return;

        double capital = buffer.getResourceCapital()[i];
        double kpc = capital / pop;
        if (kpc < RENT_KPC_FLOOR) return;

        // Samuelson (1959) Ricardian marginal product MP_L = beta_A * (Y_agr / N)
        double marginalProductKgPerCap = AGRICULTURAL_LABOR_ELASTICITY_BETA * (agriBiomass / pop);
        double wageToMarginalProductRatio = SUBSISTENCE_CORN_WAGE_KG / Math.max(1.0e-4, marginalProductKgPerCap);

        // Smooth C^infty profit-squeeze intensity when MP_L falls below subsistence corn wage (theta_w > 1)
        double squeezeExcess = SmoothMath.softplus(wageToMarginalProductRatio - 1.0, 0.0, 8.0);
        double squeezeFactor = squeezeExcess / (1.0 + squeezeExcess);

        if (squeezeFactor > 0.02) {
            double marginalExhaustion = (1.0 - AGRICULTURAL_LABOR_ELASTICITY_BETA) * 0.035 * squeezeFactor * dtYears;
            double newAgri = Math.max(20.0, agriBiomass * (1.0 - marginalExhaustion));
            acc.dBiomassAgriculture += (float) (newAgri - agriBiomass);

            double capitalProfitDrag = 0.035 * squeezeFactor * dtYears;
            double newCap = Math.max(10.0, capital * (1.0 - capitalProfitDrag));
            acc.dResourceCapital += (float) (newCap - capital);

            double currentGini = buffer.getGiniIndex()[i];
            double newGini = Math.min(0.85, currentGini + 0.02 * squeezeFactor * dtYears);
            acc.dGiniIndex += (float) (newGini - currentGini);

            acc.dFluxPressure += (float) (0.015 * squeezeFactor * dtYears);
        }
    }
}
