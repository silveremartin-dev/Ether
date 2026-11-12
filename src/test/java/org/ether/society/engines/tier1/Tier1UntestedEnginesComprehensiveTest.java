/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.PhysicalConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Unit Test Suite covering the 11 previously untested Tier 1 Physical & Environmental Engines:
 * <ul>
 *   <li>{@link AquiferDepletionEngine}</li>
 *   <li>{@link AlbedoClimateEngine}</li>
 *   <li>{@link BioMolecularEpidemiologyEngine}</li>
 *   <li>{@link CrustalGeothermalEngine}</li>
 *   <li>{@link EcotoxicologyFertilityEngine}</li>
 *   <li>{@link InfrastructureEnergyEngine}</li>
 *   <li>{@link PermafrostThawEngine}</li>
 *   <li>{@link PhysicsTransportEngine}</li>
 *   <li>{@link ResourceRecyclingEngine}</li>
 *   <li>{@link SelectiveBreedingEngine}</li>
 *   <li>{@link ThermohalineStommelAMOCEngine}</li>
 * </ul>
 */
public class Tier1UntestedEnginesComprehensiveTest {

    private H3Cell createMockCell(long h3Index, Biome biome, double elevation, double temp, int pop) {
        H3Cell cell = new H3Cell(h3Index, 0.0, 0.0);
        cell.setElevation(elevation);
        cell.setBiome(biome);
        cell.setTemperature(temp);
        cell.setPopulation(pop);
        cell.setFoodResource(1000.0);
        cell.setBiomassAgriculture(500.0);
        cell.setBiomassNatural(300.0);
        cell.setAccessibleAquifer(1000.0);
        cell.setPollutionLevel(0.0);
        cell.setResourceCapital(100.0);
        cell.setSoilOrganicCarbon(20.0);
        cell.setMovementFriction(0.25);
        return cell;
    }

    @Test
    @DisplayName("AquiferDepletionEngine: verifies Darcy piezometric flow and replenishment")
    void testAquiferDepletionEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x851f1d4bffffffL;
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 100.0f;
        buf.getTemperature()[0] = 22.0f;
        buf.getBiomassHuman()[0] = 500000.0f * 0.06f;
        buf.getRainfall()[0] = 100.0f; // Arid: heavy extraction
        buf.getFreshwaterAquifer()[0] = 1500.0f;

        new AquiferDepletionEngine().tick(buf, 0.1, 1.2); // 0.1 year
        double remaining = buf.getFreshwaterAquifer()[0];
        assertTrue(remaining < 1500.0, "Over-extracted aquifer must deplete");
        assertTrue(remaining > 1000.0, "Aquifer depletion should remain within physical bounds for 0.1 yr");
    }

    @Test
    @DisplayName("AlbedoClimateEngine: verifies dynamic albedo and thermal relaxation response")
    void testAlbedoClimateEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x851f1d4bffffff1L;
        buf.getBiomes()[0] = (byte) Biome.DESERT.ordinal();
        buf.getElevation()[0] = 200.0f;
        buf.getTemperature()[0] = 32.0f;
        buf.getBiomassHuman()[0] = 100.0f * 0.06f;
        buf.getRainfall()[0] = 50.0f;

        new AlbedoClimateEngine().tick(buf, 1.0, 12.0);
        assertTrue(buf.getTemperature()[0] != 32.0f,
                "Albedo-climate feedback must update surface temperature toward radiative equilibrium");
    }

    @Test
    @DisplayName("BioMolecularEpidemiologyEngine: starving populations suffer immune arrest")
    void testBioMolecularEpidemiologyEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x851f1d4bffffff2L;
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 50.0f;
        buf.getTemperature()[0] = 20.0f;
        buf.getBiomassHuman()[0] = 10000.0f * 0.06f;
        buf.getFoodResource()[0] = 0.0f;
        buf.getBiomassAgriculture()[0] = 0.0f;
        buf.getBiomassNatural()[0] = 0.0f;
        buf.getLifespan()[0] = 65.0f;

        double initialLifespan = buf.getLifespan()[0];
        new BioMolecularEpidemiologyEngine().tick(buf, 0.5, 6.0); // 6 months of absolute famine
        assertTrue(buf.getLifespan()[0] < initialLifespan,
                "Starvation must induce immunological collapse and reduce biological lifespan");
    }

    @Test
    @DisplayName("CrustalGeothermalEngine: deep mining cutoff scales with technological era")
    void testCrustalGeothermalEngine() {
        double depthAncient = CrustalGeothermalEngine.calculateMaxMiningDepthMeters(1.0);
        double depthIndustrial = CrustalGeothermalEngine.calculateMaxMiningDepthMeters(4.0);
        double depthModern = CrustalGeothermalEngine.calculateMaxMiningDepthMeters(8.0);

        assertTrue(depthAncient >= 50.0 && depthAncient < 500.0, "Ancient surface mining should be bounded to shallow depths");
        assertTrue(depthIndustrial > depthAncient && depthIndustrial < 3000.0, "Industrial mining should reach intermediate depths");
        assertTrue(depthModern > depthIndustrial && depthModern <= 4000.0, "Modern air-conditioned deep mining should approach 4000m");

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x851f1d4bffffff3L;
        buf.getBiomes()[0] = (byte) Biome.HILLS.ordinal();
        buf.getElevation()[0] = 800.0f;
        buf.getTemperature()[0] = 15.0f;
        buf.getBiomassHuman()[0] = 500.0f * 0.06f;
        assertDoesNotThrow(() -> new CrustalGeothermalEngine().tick(buf, 1.0 / 12.0, 1.0));
    }

    @Test
    @DisplayName("EcotoxicologyFertilityEngine: exponential fertility decay under heavy pollution")
    void testEcotoxicologyFertilityEngine() {
        double cleanFertility = EcotoxicologyFertilityEngine.calculateEcotoxicFertilityFactor(0.0);
        double heavyPollutionFertility = EcotoxicologyFertilityEngine.calculateEcotoxicFertilityFactor(5000.0);
        double extremePollutionFertility = EcotoxicologyFertilityEngine.calculateEcotoxicFertilityFactor(50000.0);

        assertEquals(1.0, cleanFertility, 1e-4, "Clean environment should have 100% baseline fertility");
        assertTrue(heavyPollutionFertility < 1.0, "Pollution must decrease effective fertility");
        assertEquals(0.35, extremePollutionFertility, 1e-2, "Extreme pollution must truncate at floor 0.35");

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x851f1d4bffffff4L;
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 50.0f;
        buf.getTemperature()[0] = 20.0f;
        buf.getBiomassHuman()[0] = 1000.0f * 0.06f;
        buf.getPollutionLevel()[0] = 2000.0f;
        assertDoesNotThrow(() -> new EcotoxicologyFertilityEngine().tick(buf, 1.0 / 12.0, 1.0));
    }

    @Test
    @DisplayName("InfrastructureEnergyEngine: capital investment lowers overland friction")
    void testInfrastructureEnergyEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x851f1d4bffffff5L;
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 100.0f;
        buf.getTemperature()[0] = 18.0f;
        buf.getBiomassHuman()[0] = 2000.0f * 0.06f;
        buf.getResourceCapital()[0] = 50000.0f;
        buf.getFluxPressure()[0] = 1.0f;

        new InfrastructureEnergyEngine().tick(buf, 1.0 / 12.0, 1.0);
        assertTrue(buf.getFluxPressure()[0] < 1.0f,
                "Infrastructure investment must reduce transport friction towards paved road levels");
    }

    @Test
    @DisplayName("PermafrostThawEngine: temperatures above 0°C release greenhouse methane")
    void testPermafrostThawEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        buf.getH3Indexes()[0] = 0x851f1d4bffffff6L;
        buf.getBiomes()[0] = (byte) Biome.TUNDRA.ordinal();
        buf.getElevation()[0] = 50.0f;
        buf.getTemperature()[0] = -5.0f;
        buf.getSoilOrganicCarbon()[0] = 20.0f;

        buf.getH3Indexes()[1] = 0x851f1d4bffffff7L;
        buf.getBiomes()[1] = (byte) Biome.TUNDRA.ordinal();
        buf.getElevation()[1] = 50.0f;
        buf.getTemperature()[1] = 6.0f;
        buf.getSoilOrganicCarbon()[1] = 20.0f;

        double ch4 = PermafrostThawEngine.processPermafrostThaw(buf);
        assertTrue(ch4 > 0.0, "Positive temperature over tundra must trigger methane outgassing");
        assertTrue(buf.getSoilOrganicCarbon()[1] < 20.0f, "Thawing tundra should lose soil carbon");
    }

    @Test
    @DisplayName("PhysicsTransportEngine: hydrodynamic fluid drag is orders of magnitude lower than land friction")
    void testPhysicsTransportEngine() {
        H3Cell ocean = createMockCell(0x851f1d4bffffff8L, Biome.OCEAN, -200.0, 15.0, 0);
        H3Cell mountain = createMockCell(0x851f1d4bffffff9L, Biome.MOUNTAINS, 2500.0, 5.0, 50);

        double oceanWork = PhysicsTransportEngine.calculateTransportWorkJoules(ocean);
        double mountainWork = PhysicsTransportEngine.calculateTransportWorkJoules(mountain);

        assertTrue(oceanWork < mountainWork, "Ocean transport work must be vastly lower than mountain work");
        assertEquals(0.001 * 9.81 * 1000.0, oceanWork, 1e-3, "Ocean hydrodynamic drag work formula verification");
    }

    @Test
    @DisplayName("ResourceRecyclingEngine: recycling efficiency scales with capital and tech")
    void testResourceRecyclingEngine() {
        double lowTechEff = ResourceRecyclingEngine.calculateContinuousRecyclingDiscount(100.0, 1.0);
        double highTechEff = ResourceRecyclingEngine.calculateContinuousRecyclingDiscount(10000.0, 8.0);

        assertTrue(lowTechEff < highTechEff, "High technology and capital must yield higher recycling efficiency");

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x851f1d4bfffffa1L;
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 100.0f;
        buf.getTemperature()[0] = 18.0f;
        buf.getBiomassHuman()[0] = 5000.0f * 0.06f;
        buf.getResourceCapital()[0] = 30000.0f;
        buf.getInstitutionalComplexity()[0] = 1.0f;

        double initialCap = buf.getResourceCapital()[0];
        new ResourceRecyclingEngine().tick(buf, 1.0 / 12.0, 1.0);
        assertTrue(buf.getResourceCapital()[0] > initialCap, "Recycling must return a portion of capital into production");
    }

    @Test
    @DisplayName("SelectiveBreedingEngine: artificial selection multiplies agricultural yield")
    void testSelectiveBreedingEngine() {
        double paleolithicFactor = SelectiveBreedingEngine.calculateSelectiveBreedingYieldFactor(0.0, 0.0);
        double greenRevFactor = SelectiveBreedingEngine.calculateSelectiveBreedingYieldFactor(2000.0, 500.0);

        assertEquals(1.0, paleolithicFactor, 0.05, "Pre-agricultural era must have ~1.0 yield factor");
        assertTrue(greenRevFactor > 2.0, "Modern breeding must multiply agricultural yields (> 2.0x)");

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x851f1d4bfffffa2L;
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 100.0f;
        buf.getTemperature()[0] = 20.0f;
        buf.getBiomassHuman()[0] = 1000.0f * 0.06f;
        buf.getResourceCapital()[0] = 5000.0f;
        buf.getBiomassAgriculture()[0] = 500.0f;

        SelectiveBreedingEngine.processSelectiveBreeding(buf);
        assertTrue(buf.getBiomassAgriculture()[0] > 500.0f, "Selective breeding must increase standing agricultural biomass");
    }

    @Test
    @DisplayName("ThermohalineStommelAMOCEngine: meltwater anomaly triggers AMOC collapse and North Atlantic cooling")
    void testThermohalineStommelAMOCEngine() {
        ThermohalineStommelAMOCEngine.resetState();
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = org.ether.society.h3.H3Service.getInstance().latLngToH3(60.0, -20.0, 5);
        buf.getBiomes()[0] = (byte) Biome.FOREST.ordinal();
        buf.getElevation()[0] = 100.0f;
        buf.getTemperature()[0] = 10.0f;
        buf.getBiomassHuman()[0] = 200.0f * 0.06f;

        double initialTemp = buf.getTemperature()[0];
        // High meltwater pulse (0.45 Sv) halts overturning convection and triggers cooling
        ThermohalineStommelAMOCEngine.processThermohalineCirculationWithMeltwater(buf, 0.45, 1.0);
        assertTrue(buf.getTemperature()[0] < initialTemp,
                "AMOC collapse must trigger severe cooling shock across North Atlantic high-latitude cells");
    }
}
