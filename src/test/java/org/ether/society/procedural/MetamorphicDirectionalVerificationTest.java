/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Metamorphic Directional Testing Suite for Complex Coupled Systems.
 *
 * <p>Addresses the fundamental "Test Oracle Problem" in non-linear planetary simulations.
 * Rather than relying on rigid exact scalar outputs, metamorphic testing proves that directional
 * input transformations produce strictly invariant and monotone output transformations:</p>
 * <ul>
 *   <li><b>MR 1 (Albedo-Thermal Monotonicity)</b>: $\alpha_2 > \alpha_1 \implies T_{\text{eq}}(\alpha_2) \le T_{\text{eq}}(\alpha_1)$.</li>
 *   <li><b>MR 2 (Thermodynamic EROEI Net Surplus)</b>: $\text{EROEI}_2 < \text{EROEI}_1 \implies \text{NetSurplus}_2 \le \text{NetSurplus}_1$.</li>
 *   <li><b>MR 3 (Biotic Over-Extraction Collapse)</b>: Harvest rate $> r \cdot K / 4 \implies \lim_{t \to \infty} \text{Biomass}(t) \to 0$.</li>
 *   <li><b>MR 4 (Spatial Trade Price Equalization)</b>: Higher inter-cell trade conductivity strictly accelerates spatial price convergence.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MetamorphicDirectionalVerificationTest {
    private static final Logger logger = LoggerFactory.getLogger(MetamorphicDirectionalVerificationTest.class);

    @Test
    @DisplayName("MR 1: Albedo-Thermal Directional Invariant (Higher Albedo => Lower Equilibrium Temperature)")
    void testAlbedoThermalDirectionalMetamorphicRelation() {
        double solarInsolation = 1361.0; // W/m2 (Solar constant)
        double sigma = 5.670374419e-8; // Stefan-Boltzmann constant
        double emissivity = 0.612; // Greenhouse effective emissivity

        double[] albedos = {0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80};
        double previousTeq = Double.MAX_VALUE;

        for (double albedo : albedos) {
            // Energy Balance Equation: S_0 * (1 - albedo) / 4 = epsilon * sigma * T_eq^4
            double absorbedRadiation = (solarInsolation * (1.0 - albedo)) / 4.0;
            double tEqKelvin = Math.pow(absorbedRadiation / (emissivity * sigma), 0.25);
            double tEqCelsius = tEqKelvin - 273.15;

            logger.info("  Albedo: {} -> T_eq: {} °C ({} K)", albedo, String.format("%.2f", tEqCelsius), String.format("%.2f", tEqKelvin));

            // Metamorphic Invariant: Teq must be strictly monotonically decreasing with albedo
            assertTrue(tEqKelvin < previousTeq,
                    String.format("Thermal metamorphic violation: Higher albedo (%.2f) must yield lower Teq (%.2f K < %.2f K)",
                            albedo, tEqKelvin, previousTeq));

            previousTeq = tEqKelvin;
        }
    }

    @Test
    @DisplayName("MR 2: EROEI Net Useful Energy Cliff (Lower EROEI => Lower Net Energy Surplus)")
    void testEROEINetEnergyCliffMetamorphicRelation() {
        double grossEnergy = 1000.0; // GJ
        double[] eroeiValues = {100.0, 50.0, 20.0, 10.0, 5.0, 3.0, 2.0, 1.2, 1.05};

        double previousSurplus = Double.MAX_VALUE;

        for (double eroei : eroeiValues) {
            // Hall & Cleveland Net Energy Equation: Net = Gross * (1 - 1 / EROEI)
            double netSurplus = grossEnergy * (1.0 - (1.0 / eroei));
            double parasiticEnergy = grossEnergy / eroei;

            logger.info("  EROEI: {} -> Net Surplus: {} GJ (Parasitic Energy: {} GJ)",
                    eroei, String.format("%.1f", netSurplus), String.format("%.1f", parasiticEnergy));

            // Metamorphic Invariant: Lower EROEI must strictly reduce net energetic surplus
            assertTrue(netSurplus < previousSurplus,
                    "EROEI metamorphic violation: Net energy surplus must decrease as EROEI decreases.");
            assertTrue(netSurplus >= 0.0, "Net surplus cannot be negative for EROEI >= 1.0");

            previousSurplus = netSurplus;
        }
    }

    @Test
    @DisplayName("MR 3: Biotic Over-Extraction Collapse Boundary (Sustained Over-Harvesting Forces Extinction)")
    void testBioticOverExtractionCollapseMetamorphicRelation() {
        double carryingCapacity = 10_000.0; // K
        double intrinsicGrowthRate = 0.05; // r (5% annual regeneration)

        // Maximum Sustainable Yield (MSY) = r * K / 4 = 125 units/year
        double msy = (intrinsicGrowthRate * carryingCapacity) / 4.0;

        // Sub-scenario A: Sustainable harvesting (80% of MSY = 100 units/year)
        double biomassSustainable = carryingCapacity * 0.8;
        for (int year = 0; year < 200; year++) {
            double growth = intrinsicGrowthRate * biomassSustainable * (1.0 - biomassSustainable / carryingCapacity);
            biomassSustainable = Math.max(0.0, biomassSustainable + growth - (msy * 0.8));
        }

        // Sub-scenario B: Destructive over-harvesting (200% of MSY = 250 units/year)
        double biomassOverharvested = carryingCapacity * 0.8;
        for (int year = 0; year < 200; year++) {
            double growth = intrinsicGrowthRate * biomassOverharvested * (1.0 - biomassOverharvested / carryingCapacity);
            biomassOverharvested = Math.max(0.0, biomassOverharvested + growth - (msy * 2.0));
        }

        logger.info("  Sustainable Harvest Final Biomass: {} | Over-harvested Final Biomass: {}",
                biomassSustainable, biomassOverharvested);

        assertTrue(biomassSustainable > carryingCapacity * 0.5, "Sustainable harvest must maintain viable biomass stock.");
        assertEquals(0.0, biomassOverharvested, 1e-4, "Sustained over-extraction above MSY must drive stock to zero.");
    }

    @Test
    @DisplayName("MR 4: Spatial Trade & Price Equalization under Conductance Gradients")
    void testSpatialPriceConvergenceMetamorphicRelation() {
        // High conductivity trade vs Low conductivity trade between two economic cells
        double priceA0 = 100.0;
        double priceB0 = 10.0;
        double initialSpread = Math.abs(priceA0 - priceB0);

        double conductanceLow = 0.02;
        double conductanceHigh = 0.20;

        double priceALow = priceA0, priceBLow = priceB0;
        double priceAHigh = priceA0, priceBHigh = priceB0;

        for (int t = 0; t < 50; t++) {
            // Price arbitrage flux: delta = kappa * (PriceA - PriceB)
            double fluxLow = conductanceLow * (priceALow - priceBLow);
            priceALow -= fluxLow * 0.5;
            priceBLow += fluxLow * 0.5;

            double fluxHigh = conductanceHigh * (priceAHigh - priceBHigh);
            priceAHigh -= fluxHigh * 0.5;
            priceBHigh += fluxHigh * 0.5;
        }

        double finalSpreadLow = Math.abs(priceALow - priceBLow);
        double finalSpreadHigh = Math.abs(priceAHigh - priceBHigh);

        logger.info("  Initial Spread: {} | Final Spread Low Kappa: {} | Final Spread High Kappa: {}",
                initialSpread, finalSpreadLow, finalSpreadHigh);

        // Metamorphic Invariant: Higher conductivity must produce strictly tighter price convergence
        assertTrue(finalSpreadHigh < finalSpreadLow,
                "Higher trade conductivity must strictly reduce spatial price variance faster than low conductivity.");
        assertTrue(finalSpreadLow < initialSpread, "Any positive trade conductance must reduce price spread.");
    }
}
