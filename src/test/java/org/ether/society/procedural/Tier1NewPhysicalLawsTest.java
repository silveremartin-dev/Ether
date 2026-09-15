/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.core.dod.EnvironmentalKernel;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying the newly implemented Tier 1 Model Physics laws:
 * 1. Glacial Thermodynamic PDD Melt & Latent Heat of Fusion
 * 2. van Genuchten Soil Water Retention Curve
 * 3. Stokes' Law Alluvial Sediment Settling Velocity
 * 4. Beer-Lambert Canopy Light Attenuation
 *
 * @author Silvere Martin-Michiellot
 * @version 4.3.0
 */
public class Tier1NewPhysicalLawsTest {

    @Test
    @DisplayName("Glacial Thermodynamic Melt Engine: PDD and Latent Heat of Fusion L_f = 333.55 kJ/kg")
    public void testGlacialThermodynamicMelt() {
        // At 0°C or below, melt must be exactly 0
        assertEquals(0.0, GlacialThermodynamicMeltEngine.calculateGlacialMeltDepthMeters(-5.0, 1.0));
        assertEquals(0.0, GlacialThermodynamicMeltEngine.calculateGlacialMeltDepthMeters(0.0, 1.0));

        // At +5°C over 1 year (365.25 days):
        // Q = 9.8 W/(m²·K) * 5 K * (365.25 * 86400 s) = 1.5463 * 10^9 J/m²
        // Mass melted = 1.5463 * 10^9 / 333550 J/kg ≈ 4635.8 kg/m²
        // Melt depth = 4635.8 / 917 ≈ 5.05 meters
        double meltDepth = GlacialThermodynamicMeltEngine.calculateGlacialMeltDepthMeters(5.0, 1.0);
        assertTrue(meltDepth > 4.0 && meltDepth < 6.0, "Glacial melt depth at 5°C must be ~5.0 meters/year");

        // Global sea level eustatic calculation
        List<H3Cell> cells = new ArrayList<>();
        H3Cell glacialCell = new H3Cell();
        glacialCell.setBiome(Biome.TUNDRA);
        glacialCell.setTemperature(4.0);
        cells.add(glacialCell);

        double eustaticRiseMeters = GlacialThermodynamicMeltEngine.processGlacialMelt(cells, 1.0);
        assertTrue(eustaticRiseMeters > 0.0, "Melt from tundra/glacial cells must increase eustatic sea level");
    }

    @Test
    @DisplayName("Soil Water Retention Engine: van Genuchten Soil Hydraulic Characteristic")
    public void testVanGenuchtenWaterRetention() {
        // At zero suction head, water content should equal saturation θ_s ≈ 0.43
        double thetaSat = SoilWaterRetentionEngine.calculateWaterContent(0.0);
        assertEquals(SoilWaterRetentionEngine.THETA_S, thetaSat, 1e-4);

        // At Field Capacity (h = 330 cm), θ should be lower than saturation but well above residual
        double thetaFC = SoilWaterRetentionEngine.calculateWaterContent(SoilWaterRetentionEngine.HEAD_FIELD_CAPACITY_CM);
        assertTrue(thetaFC < thetaSat && thetaFC > SoilWaterRetentionEngine.THETA_R);

        // At Wilting Point (h = 15000 cm), θ should approach residual θ_r ≈ 0.078
        double thetaPWP = SoilWaterRetentionEngine.calculateWaterContent(SoilWaterRetentionEngine.HEAD_WILTING_POINT_CM);
        assertTrue(thetaPWP < thetaFC);
        assertTrue(thetaPWP >= SoilWaterRetentionEngine.THETA_R);

        // Plant available water index: high rainfall gives near 1.0, low rainfall gives near 0.0
        double wetIndex = SoilWaterRetentionEngine.calculatePlantAvailableWaterIndex(1500.0, 0.02);
        double aridIndex = SoilWaterRetentionEngine.calculatePlantAvailableWaterIndex(50.0, 0.10);
        assertTrue(wetIndex > 0.8, "High rainfall on flat land must yield high AWC index");
        assertTrue(aridIndex < 0.2, "Arid steep terrain must yield low AWC index");
    }

    @Test
    @DisplayName("Dynamic Hydrographic Siltation Engine: Stokes Law Settling Velocity")
    public void testStokesSettlingVelocity() {
        // Silt grain radius r = 20 μm at 20°C:
        // v_s = (2/9) * (2650 - 1000) * 9.80665 * (20e-6)^2 / 1.002e-3 ≈ 0.00143 m/s
        double vSilt = DynamicHydrographicSiltationEngine.calculateStokesSettlingVelocity(20.0e-6, 20.0);
        assertTrue(vSilt > 0.0005 && vSilt < 0.003, "Stokes settling velocity for silt must be in physical range ~1 mm/s");

        // Higher water temperature reduces viscosity and increases settling velocity
        double vCold = DynamicHydrographicSiltationEngine.calculateStokesSettlingVelocity(20.0e-6, 5.0);
        double vWarm = DynamicHydrographicSiltationEngine.calculateStokesSettlingVelocity(20.0e-6, 30.0);
        assertTrue(vWarm > vCold, "Warmer water with lower viscosity must increase settling velocity");
    }

    @Test
    @DisplayName("Beer-Lambert Canopy Light Attenuation")
    public void testBeerLambertCanopyTransmission() {
        // Zero LAI -> 100% transmission
        assertEquals(1.0f, EnvironmentalKernel.calculateBeerLambertCanopyTransmission(0.0f, 0.6f), 1e-4);

        // LAI = 3.0, k_ext = 0.6 -> transmission = exp(-1.8) ≈ 0.165
        float transDense = EnvironmentalKernel.calculateBeerLambertCanopyTransmission(3.0f, 0.6f);
        assertTrue(transDense > 0.15f && transDense < 0.18f);

        // LAI = 6.0 (tropical rainforest) -> transmission = exp(-3.6) ≈ 0.027
        float transJungle = EnvironmentalKernel.calculateBeerLambertCanopyTransmission(6.0f, 0.6f);
        assertTrue(transJungle < 0.05f);
    }
}
