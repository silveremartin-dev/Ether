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
 * Stream Power Fluvial Erosion &amp; Bagnold Alluvial Siltation Engine.
 *
 * <p>Models catchment geomorphology, bedrock and topsoil incision, and flood-basin alluvial
 * replenishment according to the canonical Stream Power Incision Law (Howard &amp; Kerby 1983;
 * Whipple &amp; Tucker 1999) and Bagnold sediment transport dynamics (Bagnold 1966; Montgomery 2007):</p>
 *
 * <p><b>Stream Power Law of Fluvial Erosion:</b>
 * $$\dot{E} = K_{\text{erod}} \cdot A^m \cdot S^n$$
 * where:
 * <ul>
 *   <li>$A$: Upstream contributing drainage area ($\text{km}^2$), coupled to the Hack river basin law
 *       from {@link ConstructalFlowArchitectureEngine} ($L \approx 1.4 A^{0.57}$).</li>
 *   <li>$S$: Local topographic slope gradient ($S = |\nabla z|$).</li>
 *   <li>$m = 0.5$, $n = 1.0$: Canonical physical stream power exponents.</li>
 *   <li>$K_{\text{erod}}$: Soil erodibility coefficient, damped exponentially by vegetative root structure:
 *       $$K_{\text{erod}} = K_0 \cdot \exp\left(-\frac{B_{\text{nat}} + B_{\text{agr}}}{B_{\text{ref}}}\right)$$
 *       Anthropogenic deforestation and overgrazing drastically elevate erodibility by an order of magnitude.</li>
 * </ul>
 * </p>
 *
 * <p><b>Bagnold Lowland Alluvial Deposition &amp; Floodplain Soil Replenishment:</b>
 * When sediment-laden rivers discharge onto flat alluvial lowlands and deltas ($S < 0.002$), sediment carrying
 * capacity collapses ($\Omega = \rho g Q S \to 0$). The precipitated mineral alluvium replenishes topsoil
 * depth and Soil Organic Carbon (SOC), reproducing the sustainable multi-millennial fertility of ancient
 * hydraulic civilizations (Nile, Tigris-Euphrates, Indus, Yellow River) despite intensive agriculture.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class StreamPowerErosionEngine extends AbstractSimulationEngine {
    public StreamPowerErosionEngine() {
        super("StreamPowerErosionEngine", "StreamPowerErosionEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(StreamPowerErosionEngine.class);

    /** Baseline soil erodibility coefficient in m/year */
    public static final double BASE_ERODIBILITY_K0 = 0.0015;

    /** Reference vegetative biomass conferring 63% root cohesion protection in g/m² */
    public static final double BIOMASS_ROOT_COHESION_REF = 250.0;

    /** Stream power exponents */
    public static final double STREAM_POWER_EXPONENT_M = 0.50;
    public static final double STREAM_POWER_EXPONENT_N = 1.00;

    /** Flat lowland deposition slope threshold */
    public static final double LOWLAND_ALLUVIAL_SLOPE_MAX = 0.008;

    /** Flat alluvial floodplain elevation ceiling in meters */
    public static final double LOWLAND_FLOODPLAIN_ELEV_MAX_M = 80.0;

    /**
     * Calculates the protective vegetative cover attenuation factor $\in [0.05, 1.0]$.
     *
     * @param totalBiomass combined natural and crop biomass
     * @return erodibility multiplier
     */
    public static double calculateVegetationProtectionFactor(double totalBiomass) {
        if (totalBiomass <= 0.0) return 1.0; // Complete bare soil exposure
        return Math.max(0.05, Math.exp(-totalBiomass / BIOMASS_ROOT_COHESION_REF));
    }

    /**
     * Estimates upstream contributing drainage area $A$ in km² from Hack's Law.
     *
     * @param riverLengthKm river reach length in km
     * @return estimated drainage basin area in km²
     */
    public static double estimateCatchmentAreaKm2(double riverLengthKm) {
        if (riverLengthKm <= 0.0) return 10.0; // Local hill-slope minimal area
        // L = 1.4 * A^0.57 => A = (L / 1.4)^(1 / 0.57)
        return Math.pow(Math.max(1.0, riverLengthKm / 1.4), 1.0 / 0.57);
    }

    /**
     * Calculates the Stream Power fluvial erosion rate $\dot{E}$ in meters per year.
     *
     * @param catchmentAreaKm2 upstream drainage area in km²
     * @param slopeGradient local topographic slope
     * @param totalBiomass vegetative biomass (natural + crops)
     * @return erosion incision rate in m/year
     */
    public static double calculateStreamPowerErosion(double catchmentAreaKm2, double slopeGradient, double totalBiomass) {
        double slopeSafe = Math.clamp(slopeGradient, 0.0005, 1.0);
        double areaNorm = Math.max(1.0, catchmentAreaKm2);
        double vegFactor = calculateVegetationProtectionFactor(totalBiomass);

        double kErod = BASE_ERODIBILITY_K0 * vegFactor;
        // Stream Power Incision: E_dot = K * (A/1000)^m * S^n
        return kErod * Math.pow(areaNorm / 1000.0, STREAM_POWER_EXPONENT_M) * Math.pow(slopeSafe, STREAM_POWER_EXPONENT_N);
    }

    /**
     * Executes one time-stepped Stream Power erosion and alluvial replenishment evaluation across land cells.
     *
     * @param cells   land cell domain
     * @param dtYears elapsed simulation step in years
     */
    public static void processStreamPowerErosion(List<H3Cell> cells, double dtYears) {
        if (cells == null || cells.isEmpty() || dtYears <= 0.0) return;

        double dtSafe = Math.clamp(dtYears, 0.0001, 10.0);
        double totalErodedSoilM = 0.0;
        int erodingCellCount = 0;

        // Pass A: Upstream catchment slope incision
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double elev = cell.getElevationOr(0.0);
            if (elev <= 0.0 || cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) continue;

            double naturalBio = cell.getBiomassNaturalOr(100.0);
            double agrBio = cell.getBiomassAgricultureOr(0.0);
            double totalBio = naturalBio + agrBio;

            // Approximate slope from elevation and terrain biome
            double slope = 0.005;
            if (cell.getBiome() == Biome.MOUNTAINS) slope = 0.15;
            else if (cell.getBiome() == Biome.HILLS) slope = 0.05;
            else if (elev > 500.0) slope = 0.02;

            // Stream length approximation via constructal channel flow
            double riverLength = Math.max(5.0, Math.sqrt(elev) * 2.5);
            double catchmentArea = estimateCatchmentAreaKm2(riverLength);

            double erosionRateMPerYr = calculateStreamPowerErosion(catchmentArea, slope, totalBio);
            double deltaElev = erosionRateMPerYr * dtSafe;

            if (deltaElev > 1e-6) {
                cell.setElevation(Math.max(0.0, elev - deltaElev));
                totalErodedSoilM += deltaElev;
                erodingCellCount++;

                // Severe erosion strips Soil Organic Carbon (topsoil degradation)
                double curSoc = cell.getSoilOrganicCarbonOr(40.0);
                double socLoss = curSoc * Math.min(0.20, deltaElev * 50.0);
                cell.setSoilOrganicCarbon(Math.max(2.0, curSoc - socLoss));
            }
        }

        // Pass B: Lowland Floodplain and Estuarine Alluvial Silt Deposition
        if (totalErodedSoilM > 0.0 && erodingCellCount > 0) {
            double totalSedimentMass = totalErodedSoilM;
            double depositBudgetPerCell = totalSedimentMass / Math.max(1, cells.size() / 8);

            for (H3Cell cell : cells) {
                if (cell == null) continue;
                double elev = cell.getElevationOr(0.0);
                if (elev < 0.0 || elev > LOWLAND_FLOODPLAIN_ELEV_MAX_M) continue; // Only flat river floodplains and lowlands
                if (cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) continue;

                // Alluvial siltation replenishes topsoil and mineral SOC
                double curSoc = cell.getSoilOrganicCarbonOr(40.0);
                double siltDepositM = Math.min(0.003 * dtSafe, depositBudgetPerCell);
                cell.setElevation(elev + siltDepositM);

                // Alluvial silt enriches organic carbon by +0.1 to +0.5 t/ha per year
                double socReplenishment = siltDepositM * 120.0;
                cell.setSoilOrganicCarbon(Math.min(120.0, curSoc + socReplenishment));
            }
        }
    }
}
