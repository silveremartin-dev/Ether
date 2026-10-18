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
 *   <li>{@link ThermohalineOceanEngine}</li>
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
        cell.setTechnologyLevel(1.0);
        cell.setResourceCapital(100.0);
        cell.setSoilOrganicCarbon(20.0);
        cell.setMovementFriction(0.25);
        return cell;
    }

    @Test
    @DisplayName("AquiferDepletionEngine: verifies Darcy piezometric flow and replenishment")
    void testAquiferDepletionEngine() {
        List<H3Cell> cells = new ArrayList<>();
        // Arid cell with high population -> heavy pumping
        H3Cell cellA = createMockCell(0x851f1d4bffffffL, Biome.PLAINS, 100.0, 22.0, 5000);
        cellA.setRainfall(100.0); // Arid: per-capita demand = 0.80 -> 4000 units/year extraction
        cellA.setAccessibleAquifer(1500.0);
        cells.add(cellA);

        AquiferDepletionEngine.processAquiferDepletion(cells, 0.1); // 0.1 year
        // naturalRecharge = (100 * 0.15) * 0.1 = 1.5
        // irrigationExtraction = (5000 * 0.80) * 0.1 = 400
        // expected net change = -398.5
        double remaining = cellA.getAccessibleAquifer();
        assertEquals(1500.0 - 398.5, remaining, 1.0, "Aquifer extraction must match net Darcy/recharge balance");
        assertTrue(remaining < 1500.0, "Over-extracted aquifer must deplete");
    }

    @Test
    @DisplayName("AlbedoClimateEngine: verifies orbital Milankovitch precession & albedo response")
    void testAlbedoClimateEngine() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell saharaCell = createMockCell(0x851f1d4bffffff1L, Biome.DESERT, 200.0, 32.0, 100);
        saharaCell.setLatitude(20.0); // Sub-tropical band
        saharaCell.setRainfall(50.0);
        cells.add(saharaCell);

        // Under peak precession year (5,250 = 21,000 / 4, sin(pi/2) = 1.0), insolation triggers Green Sahara transition
        AlbedoClimateEngine.processAlbedoAndClimateEvents(cells, 5250L, 0.0, 1.0);
        assertTrue(saharaCell.getRainfall() > 50.0, 
                "Milankovitch orbital precession peak must intensify sub-tropical monsoons and rainfall");
    }

    @Test
    @DisplayName("BioMolecularEpidemiologyEngine: starving populations suffer immune arrest")
    void testBioMolecularEpidemiologyEngine() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell starvingCell = createMockCell(0x851f1d4bffffff2L, Biome.PLAINS, 50.0, 20.0, 10000);
        starvingCell.setFoodResource(0.0);
        starvingCell.setBiomassAgriculture(0.0);
        starvingCell.setBiomassNatural(0.0);
        cells.add(starvingCell);

        double initialLifespan = starvingCell.getLifespan();
        BioMolecularEpidemiologyEngine.processBioMolecularImmunity(cells, 0.5); // 6 months of absolute famine
        assertTrue(starvingCell.getLifespan() < initialLifespan,
                "Starvation must induce immunological collapse and reduce biological lifespan");
    }

    @Test
    @DisplayName("CrustalGeothermalEngine: deep mining cutoff scales with technological era")
    void testCrustalGeothermalEngine() {
        double depthAncient = CrustalGeothermalEngine.calculateMaxMiningDepthMeters(1.0);
        double depthIndustrial = CrustalGeothermalEngine.calculateMaxMiningDepthMeters(4.0);
        double depthModern = CrustalGeothermalEngine.calculateMaxMiningDepthMeters(8.0);

        assertEquals(100.0, depthAncient, 1e-3, "Ancient surface mining should be bounded to 100m");
        assertEquals(500.0, depthIndustrial, 1e-3, "Industrial shaft mining should reach 500m");
        assertEquals(4000.0, depthModern, 1e-3, "Modern air-conditioned mining should reach 4000m");

        List<H3Cell> cells = List.of(createMockCell(0x851f1d4bffffff3L, Biome.HILLS, 800.0, 15.0, 500));
        assertDoesNotThrow(() -> CrustalGeothermalEngine.processCrustalGeothermal(cells));
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

        List<H3Cell> cells = List.of(createMockCell(0x851f1d4bffffff4L, Biome.PLAINS, 50.0, 20.0, 1000));
        cells.get(0).setPollutionLevel(2000.0);
        assertDoesNotThrow(() -> EcotoxicologyFertilityEngine.processEcotoxicologyFertility(cells));
    }

    @Test
    @DisplayName("InfrastructureEnergyEngine: capital investment lowers overland friction")
    void testInfrastructureEnergyEngine() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell developedCell = createMockCell(0x851f1d4bffffff5L, Biome.PLAINS, 100.0, 18.0, 2000);
        developedCell.setTechnologyLevel(3.5);
        developedCell.setResourceCapital(5000.0);
        developedCell.setMovementFriction(0.25);
        cells.add(developedCell);

        InfrastructureEnergyEngine.processInfrastructureEnergy(cells);
        assertTrue(developedCell.getMovementFriction() < 0.25,
                "Infrastructure investment must reduce transport friction towards paved road levels");
    }

    @Test
    @DisplayName("PermafrostThawEngine: temperatures above 0°C release greenhouse methane")
    void testPermafrostThawEngine() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell frozenTundra = createMockCell(0x851f1d4bffffff6L, Biome.TUNDRA, 50.0, -5.0, 10);
        H3Cell thawingTundra = createMockCell(0x851f1d4bffffff7L, Biome.TUNDRA, 50.0, 6.0, 10);
        cells.add(frozenTundra);
        cells.add(thawingTundra);

        double ch4 = PermafrostThawEngine.processPermafrostThaw(cells);
        assertTrue(ch4 > 0.0, "Positive temperature over tundra must trigger methane outgassing");
        assertTrue(thawingTundra.getSoilOrganicCarbon() < 20.0, "Thawing tundra should lose soil carbon");
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

        List<H3Cell> cells = new ArrayList<>();
        H3Cell circularCity = createMockCell(0x851f1d4bfffffa1L, Biome.PLAINS, 100.0, 18.0, 5000);
        circularCity.setTechnologyLevel(6.0);
        circularCity.setResourceCapital(3000.0);
        cells.add(circularCity);

        double initialCap = circularCity.getResourceCapital();
        ResourceRecyclingEngine.processResourceRecycling(cells);
        assertTrue(circularCity.getResourceCapital() > initialCap, "Recycling must return a portion of capital into production");
    }

    @Test
    @DisplayName("SelectiveBreedingEngine: artificial selection multiplies agricultural yield")
    void testSelectiveBreedingEngine() {
        double paleolithicFactor = SelectiveBreedingEngine.calculateSelectiveBreedingYieldFactor(0.5);
        double greenRevFactor = SelectiveBreedingEngine.calculateSelectiveBreedingYieldFactor(6.0);

        assertEquals(1.0, paleolithicFactor, 1e-4, "Pre-agricultural era must have 1.0 yield factor");
        assertTrue(greenRevFactor > 2.5, "Modern breeding must multiply agricultural yields (> 2.5x)");

        List<H3Cell> cells = new ArrayList<>();
        H3Cell farmCell = createMockCell(0x851f1d4bfffffa2L, Biome.PLAINS, 100.0, 20.0, 1000);
        farmCell.setTechnologyLevel(4.0);
        farmCell.setBiomassAgriculture(500.0);
        cells.add(farmCell);

        SelectiveBreedingEngine.processSelectiveBreeding(cells);
        assertTrue(farmCell.getBiomassAgriculture() > 500.0, "Selective breeding must increase standing agricultural biomass");
    }

    @Test
    @DisplayName("ThermohalineOceanEngine: high global warming triggers AMOC salinity collapse")
    void testThermohalineOceanEngine() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell highLatCell = createMockCell(0x851f1d4bfffffa3L, Biome.FOREST, 100.0, 10.0, 200);
        highLatCell.setLatitude(60.0); // High latitude
        cells.add(highLatCell);

        double initialTemp = highLatCell.getTemperature();
        // High temperature anomaly (+4.0°C) melts polar ice, dropping PSU below 32.0 and triggering AMOC collapse
        ThermohalineOceanEngine.processThermohalineCirculation(cells, 4.0);
        assertTrue(highLatCell.getTemperature() < initialTemp,
                "AMOC collapse must trigger severe cooling shock across high-latitude cells");
    }
}
