/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @version 1.0.0-beta.3
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

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processStreamPowerErosion(buf, dtYears);
    }

    /**
     * Executes one time-stepped Stream Power erosion and alluvial replenishment evaluation across DoD land cells.
     *
     * @param buffer  WorldBuffer DoD state
     * @param dtYears elapsed simulation step in years
     */
    public static void processStreamPowerErosion(WorldBuffer buffer, double dtYears) {
        if (buffer == null || buffer.getCapacity() == 0 || dtYears <= 0.0) return;

        double dtSafe = Math.clamp(dtYears, 0.0001, 10.0);
        double totalErodedSoilM = 0.0;
        int erodingCellCount = 0;

        float[] elevArr = buffer.getElevation();
        byte[] biomes = buffer.getBiomes();
        float[] bioNatArr = buffer.getBiomassNatural();
        float[] bioAgriArr = buffer.getBiomassAgriculture();
        float[] socArr = buffer.getSoilOrganicCarbon();

        byte oceanOrd = (byte) Biome.OCEAN.ordinal();
        byte deepOceanOrd = (byte) Biome.DEEP_OCEAN.ordinal();
        byte mountainsOrd = (byte) Biome.MOUNTAINS.ordinal();
        byte hillsOrd = (byte) Biome.HILLS.ordinal();
        int n = buffer.getCapacity();

        // Pass A: Upstream catchment slope incision
        for (int i = 0; i < n; i++) {
            double elev = elevArr[i];
            byte biomeOrd = biomes[i];
            if (elev <= 0.0 || biomeOrd == oceanOrd || biomeOrd == deepOceanOrd) continue;

            double naturalBio = bioNatArr[i];
            double agrBio = bioAgriArr[i];
            double totalBio = naturalBio + agrBio;

            double slope = 0.005;
            if (biomeOrd == mountainsOrd) slope = 0.15;
            else if (biomeOrd == hillsOrd) slope = 0.05;
            else if (elev > 500.0) slope = 0.02;

            double riverLength = Math.max(5.0, Math.sqrt(elev) * 2.5);
            double catchmentArea = estimateCatchmentAreaKm2(riverLength);

            double erosionRateMPerYr = calculateStreamPowerErosion(catchmentArea, slope, totalBio);
            double deltaElev = erosionRateMPerYr * dtSafe;

            if (deltaElev > 1e-6) {
                elevArr[i] = (float) Math.max(0.0, elev - deltaElev);
                totalErodedSoilM += deltaElev;
                erodingCellCount++;

                double curSoc = socArr[i];
                double socLoss = curSoc * Math.min(0.20, deltaElev * 50.0);
                socArr[i] = (float) Math.max(2.0, curSoc - socLoss);
            }
        }

        // Pass B: Lowland Floodplain and Estuarine Alluvial Silt Deposition
        if (totalErodedSoilM > 0.0 && erodingCellCount > 0) {
            double depositBudgetPerCell = totalErodedSoilM / Math.max(1, n / 8);

            for (int i = 0; i < n; i++) {
                double elev = elevArr[i];
                byte biomeOrd = biomes[i];
                if (elev < 0.0 || elev > LOWLAND_FLOODPLAIN_ELEV_MAX_M) continue;
                if (biomeOrd == oceanOrd || biomeOrd == deepOceanOrd) continue;

                double curSoc = socArr[i];
                double siltDepositM = Math.min(0.003 * dtSafe, depositBudgetPerCell);
                elevArr[i] = (float) (elev + siltDepositM);

                double socReplenishment = siltDepositM * 120.0;
                socArr[i] = (float) Math.min(120.0, curSoc + socReplenishment);
            }
        }
    }
}
