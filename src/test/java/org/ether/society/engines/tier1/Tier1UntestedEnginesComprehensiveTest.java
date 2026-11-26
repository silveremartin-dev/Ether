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
        buf.getH3Indexes()[0] = org.ether.society.h3.H3Service.getInstance().latLngToH3(30.0, 31.0, 5);
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 100.0f;
        buf.getTemperature()[0] = 22.0f;
        buf.getHumanPopulation()[0] = 500000.0f;
        buf.getRainfall()[0] = 100.0f; // Arid: heavy extraction
        buf.getFreshwaterAquifer()[0] = 1500.0f;

        new AquiferDepletionEngine().tick(buf, 0.1, 1.2); // 0.1 year
        double remaining = buf.getFreshwaterAquifer()[0];
        assertTrue(remaining < 1500.0, "Over-extracted aquifer must deplete");
        assertTrue(remaining > 1000.0, "Aquifer depletion should remain within physical bounds for 0.1 yr");
    }

    @Test
    @DisplayName("AlbedoClimateEngine: verifies empirical baseline preservation, desert-forest contrast, mean invariance, and volcanic aerosol recovery")
    void testAlbedoClimateEngine() {
        NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(0.0);
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(3);
        org.ether.society.h3.H3Service h3 = org.ether.society.h3.H3Service.getInstance();
        // 0: Sahara Desert (23N, 12E); 1: Amazon Rainforest (-3S, -60W); 2: Antarctica Ice Sheet (-78S, 20E)
        buf.getH3Indexes()[0] = h3.latLngToH3(23.0, 12.0, 4);
        buf.getBiomes()[0] = (byte) Biome.DESERT.ordinal();
        buf.getElevation()[0] = 450.0f;
        buf.getTemperature()[0] = 32.0f;
        buf.getRainfall()[0] = 40.0f;
        buf.getBiomassNatural()[0] = 5.0f;
        buf.getHumanPopulation()[0] = 100.0f;

        buf.getH3Indexes()[1] = h3.latLngToH3(-3.0, -60.0, 4);
        buf.getBiomes()[1] = (byte) Biome.FOREST.ordinal();
        buf.getElevation()[1] = 80.0f;
        buf.getTemperature()[1] = 26.0f;
        buf.getRainfall()[1] = 2400.0f;
        buf.getBiomassNatural()[1] = 5000.0f;
        buf.getHumanPopulation()[1] = 500.0f;

        buf.getH3Indexes()[2] = h3.latLngToH3(-78.0, 20.0, 4);
        buf.getBiomes()[2] = (byte) Biome.GLACIER.ordinal();
        buf.getElevation()[2] = 2800.0f;
        buf.getTemperature()[2] = -35.0f;
        buf.getRainfall()[2] = 120.0f;
        buf.getBiomassNatural()[2] = 0.0f;

        double initialMeanTemp = (32.0 + 26.0 - 35.0) / 3.0;
        AlbedoClimateEngine engine = new AlbedoClimateEngine();
        for (int yr = 0; yr < 20; yr++) {
            engine.tick(buf, 1.0, 12.0);
        }

        double desertTemp = buf.getTemperature()[0];
        double forestTemp = buf.getTemperature()[1];
        double polarTemp = buf.getTemperature()[2];
        double finalMeanTemp = (desertTemp + forestTemp + polarTemp) / 3.0;

        assertFalse(Double.isNaN(finalMeanTemp), "Surface temperatures must remain finite");
        assertTrue(polarTemp < -25.0, "Antarctic polar baseline must never drift toward warm 22C synthetic average, got: " + polarTemp);
        assertTrue(desertTemp < 32.0 && desertTemp > 30.5, "High desert albedo (0.35) must yield mild equilibrium cooling, got: " + desertTemp);
        assertTrue(forestTemp > 26.0 && forestTemp < 27.5, "Low dense forest canopy albedo must yield mild equilibrium warming, got: " + forestTemp);
        assertTrue(Math.abs(finalMeanTemp - initialMeanTemp) < 1.5,
                "Multi-decadal global mean temperature must stay conserved within 1.5C of empirical baseline");

        // Volcanic / stratospheric soot optical pulse cools sharply and recovers asymptotically
        NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(1.5);
        engine.tick(buf, 1.0, 12.0);
        assertTrue(buf.getTemperature()[1] < forestTemp - 2.0, "Stratospheric aerosol pulse must induce rapid cooling");
        for (int yr = 0; yr < 15; yr++) {
            engine.tick(buf, 1.0, 12.0);
        }
        assertTrue(buf.getTemperature()[1] > forestTemp - 0.5, "Temperature must recover toward equilibrium once soot decays");
        NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(0.0);
    }

    @Test
    @DisplayName("BioMolecularEpidemiologyEngine: starving populations suffer immune arrest")
    void testBioMolecularEpidemiologyEngine() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = org.ether.society.h3.H3Service.getInstance().latLngToH3(30.0, 31.0, 5);
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 50.0f;
        buf.getTemperature()[0] = 20.0f;
        buf.getHumanPopulation()[0] = 10000.0f;
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
        buf.getHumanPopulation()[0] = 500.0f;
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
        buf.getHumanPopulation()[0] = 1000.0f;
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
        buf.getHumanPopulation()[0] = 2000.0f;
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
        buf.getH3Indexes()[0] = org.ether.society.h3.H3Service.getInstance().latLngToH3(48.8, 2.3, 5);
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 100.0f;
        buf.getTemperature()[0] = 18.0f;
        buf.getHumanPopulation()[0] = 5000.0f;
        buf.getResourceCapital()[0] = 5_000_000.0f; // 1000 kg/capita industrial metropolis
        buf.getInstitutionalComplexity()[0] = 1.0f;

        double initialCap = buf.getResourceCapital()[0];
        new ResourceRecyclingEngine().tick(buf, 1.0 / 12.0, 1.0);
        assertTrue(buf.getResourceCapital()[0] > initialCap, "Industrial recycling must recover capital depreciation");

        // Intensive invariance guard: Paleolithic bands (5 kg/cap) must not generate industrial circular capital
        org.ether.society.core.dod.WorldBuffer paleoBuf = new org.ether.society.core.dod.WorldBuffer(1);
        paleoBuf.getH3Indexes()[0] = org.ether.society.h3.H3Service.getInstance().latLngToH3(10.0, 38.0, 5);
        paleoBuf.getHumanPopulation()[0] = 5000.0f;
        paleoBuf.getResourceCapital()[0] = 25000.0f; // 5 kg/capita stone toolkit
        paleoBuf.getInstitutionalComplexity()[0] = 0.5f;
        new ResourceRecyclingEngine().tick(paleoBuf, 1.0, 12.0);
        assertEquals(25000.0f, paleoBuf.getResourceCapital()[0], 1e-3f,
                "Low-kpc Paleolithic bands must not trigger industrial capital recycling");
    }

    @Test
    @DisplayName("SelectiveBreedingEngine: artificial selection multiplies agricultural yield")
    void testSelectiveBreedingEngine() {
        double paleolithicFactor = SelectiveBreedingEngine.calculateSelectiveBreedingYieldFactor(0.0, 0.0);
        double greenRevFactor = SelectiveBreedingEngine.calculateSelectiveBreedingYieldFactor(2000.0, 500.0);

        assertEquals(1.0, paleolithicFactor, 0.05, "Pre-agricultural era must have ~1.0 yield factor");
        assertTrue(greenRevFactor > 2.0, "Modern breeding must multiply agricultural yields (> 2.0x)");

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = org.ether.society.h3.H3Service.getInstance().latLngToH3(45.0, 10.0, 5);
        buf.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        buf.getElevation()[0] = 100.0f;
        buf.getTemperature()[0] = 20.0f;
        buf.getHumanPopulation()[0] = 1000.0f;
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
        buf.getHumanPopulation()[0] = 200.0f;

        double initialTemp = buf.getTemperature()[0];
        // High meltwater pulse (0.45 Sv) halts overturning convection and triggers cooling
        ThermohalineStommelAMOCEngine.processThermohalineCirculationWithMeltwater(buf, 0.45, 1.0);
        assertTrue(buf.getTemperature()[0] < initialTemp,
                "AMOC collapse must trigger severe cooling shock across North Atlantic high-latitude cells");
    }
}
