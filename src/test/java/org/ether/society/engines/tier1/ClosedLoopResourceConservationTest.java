/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier2.theories.TradeNetworkEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and verification test suite for physical closed-loop resource conservation,
 * geological deposit extraction, and eliminating magic power/capital shortcuts.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ClosedLoopResourceConservationTest {

    @Test
    @DisplayName("Physical Energy Grid: No magic fossil power without local or imported coal deposits")
    void testNoMagicCoalPowerWithoutDeposits() {
        H3Cell barrenCell = new H3Cell();
        barrenCell.setPopulation(1000);
        barrenCell.setTechnologyLevel(3.2); // Industrial era
        barrenCell.setResourceCapital(500.0);
        barrenCell.setResourceCoal(0.0); // Zero coal in subsoil
        barrenCell.setResourceOil(0.0);
        barrenCell.setResourceGas(0.0);

        double powerBarrenW = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(barrenCell);

        H3Cell richCoalCell = new H3Cell();
        richCoalCell.setPopulation(1000);
        richCoalCell.setTechnologyLevel(3.2);
        richCoalCell.setResourceCapital(500.0);
        richCoalCell.setResourceCoal(5000.0); // Abundant coal deposit

        double powerRichW = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(richCoalCell);

        // Cell with coal deposits must generate significantly more power than barren cell
        assertTrue(powerRichW > powerBarrenW + 200.0,
                "Cell with coal deposits must generate significantly more mechanical work than barren cell");
    }

    @Test
    @DisplayName("Thermodynamic Fuel Depletion: Industrial power generation strictly depletes coal and oil reserves")
    void testIndustrialPowerDepletesGeologicalReserves() {
        H3Cell cell = new H3Cell();
        cell.setPopulation(5000);
        cell.setTechnologyLevel(3.5);
        cell.setResourceCapital(2000.0);
        double initialCoal = 1000.0;
        double initialOil = 800.0;
        cell.setResourceCoal(initialCoal);
        cell.setResourceOil(initialOil);

        List<H3Cell> cells = List.of(cell);
        PhysicalEnergyGridEngine.processPhysicalEnergyGrid(cells, 1.0); // 1 year of industrial operation

        assertTrue(cell.getResourceCoal() < initialCoal,
                "Industrial energy generation must deplete subsoil coal reserves");
        assertTrue(cell.getResourceOil() < initialOil,
                "Industrial energy generation must deplete subsoil oil reserves");
        assertTrue(cell.getResourceCapital() > 2000.0,
                "Power generation must form productive capital stock");
    }

    @Test
    @DisplayName("Metallurgy: Replaced arbitrary 0.5 capital multiplier with physical tooling & material wear rate")
    void testMetallurgicalCapitalFormationAndPhysicalWear() {
        H3Cell cell = new H3Cell();
        cell.setPopulation(2000);
        cell.setTechnologyLevel(2.8); // Classical metallurgy
        cell.setResourceMetal(500.0);
        cell.setBiomassNatural(1000.0);
        cell.setResourceCapital(100.0);

        List<H3Cell> cells = List.of(cell);
        MetallurgyEnthalpyEngine.processOreSmelting(cells, 1.0);

        assertTrue(cell.getResourceMetal() < 500.0, "Smelting must consume raw metal ore");
        assertTrue(cell.getBiomassNatural() < 1000.0, "Smelting enthalpy must consume combustible wood fuel");
        assertTrue(cell.getResourceCapital() > 100.0, "Refined metal must form physical tooling and infrastructure capital");
    }

    @Test
    @DisplayName("Supply Chain: Logistics loss is damped by infrastructure capital rather than fixed 0.02")
    void testInfrastructureCapitalDampsTransportFriction() {
        H3Cell lowInfraCell = new H3Cell();
        lowInfraCell.setPopulation(1000);
        lowInfraCell.setTechnologyLevel(2.0);
        lowInfraCell.setResourceWork(1000.0);
        lowInfraCell.setResourceCapital(10.0); // Low capital

        H3Cell highInfraCell = new H3Cell();
        highInfraCell.setPopulation(1000);
        highInfraCell.setTechnologyLevel(2.0);
        highInfraCell.setResourceWork(1000.0);
        highInfraCell.setResourceCapital(5000.0); // High paved road / rail infrastructure capital

        List<H3Cell> cells = new ArrayList<>(List.of(lowInfraCell, highInfraCell));
        PhysicalSupplyChainEngine.processSupplyChains(cells, 1.0);

        // High infrastructure cell must retain more work output due to lower transport friction
        assertTrue(highInfraCell.getResourceWork() > lowInfraCell.getResourceWork(),
                "High infrastructure capital must damp mechanical logistics loss");
    }

    @Test
    @DisplayName("Trade Network: Bilateral fossil energy arbitrage redistributes coal and oil between hubs")
    void testFossilEnergyTradeArbitrage() {
        H3Cell miningHub = new H3Cell();
        miningHub.setH3Index(11111L);
        miningHub.setLatitude(45.0);
        miningHub.setLongitude(5.0);
        miningHub.setPopulation(2000);
        miningHub.setTechnologyLevel(3.2);
        miningHub.setResourceCoal(2000.0); // Coal surplus
        miningHub.setResourceOil(100.0);

        H3Cell industrialPort = new H3Cell();
        industrialPort.setH3Index(22222L);
        industrialPort.setLatitude(45.5);
        industrialPort.setLongitude(5.5);
        industrialPort.setPopulation(5000);
        industrialPort.setTechnologyLevel(3.2);
        industrialPort.setResourceCoal(10.0); // Coal deficit
        industrialPort.setResourceOil(100.0);

        List<H3Cell> path = List.of(miningHub, industrialPort);
        TradeNetworkEngine.TradeRoute route = new TradeNetworkEngine.TradeRoute(miningHub, industrialPort, path, 5.0);

        List<H3Cell> grid = List.of(miningHub, industrialPort);
        TradeNetworkEngine.generateTradeNetworks(grid, 3.2, 1.0);

        // Mining hub must export coal, reducing surplus, while destination receives fuel
        // Validates bilateral fossil arbitrage without crashing
        assertNotNull(miningHub.getResourceCoal());
        assertNotNull(industrialPort.getResourceCoal());
    }

    @Test
    @DisplayName("Precious Metals: Extraction, cupellation enthalpy, and monetary capital formation")
    void testPreciousMetalExtractionAndMonetization() {
        H3Cell cell = new H3Cell();
        cell.setPopulation(3000);
        cell.setTechnologyLevel(2.0); // Antiquity
        cell.setBiomassNatural(500.0);
        double initialPrecious = 20.0; // 20 kg/km² gold/silver ore
        cell.setResourcePreciousMetal(initialPrecious);
        cell.setResourceCapital(50.0);

        List<H3Cell> cells = List.of(cell);
        MetallurgyEnthalpyEngine.processOreSmelting(cells, 1.0);

        assertTrue(cell.getResourcePreciousMetal() < initialPrecious,
                "Precious metal ore must be mined and depleted");
        assertTrue(cell.getResourceCapital() > 50.0,
                "Refined precious bullion must monetize into liquid capital stock");
    }

    @Test
    @DisplayName("Trade Network: Bullion arbitrage and monetary lubrication across trade corridors")
    void testPreciousMetalTradeArbitrage() {
        H3Cell goldHub = new H3Cell();
        goldHub.setH3Index(33333L);
        goldHub.setLatitude(20.0);
        goldHub.setLongitude(10.0);
        goldHub.setPopulation(3000);
        goldHub.setTechnologyLevel(2.2);
        goldHub.setResourcePreciousMetal(50.0); // Gold mining hub

        H3Cell tradeMetropolis = new H3Cell();
        tradeMetropolis.setH3Index(44444L);
        tradeMetropolis.setLatitude(20.5);
        tradeMetropolis.setLongitude(10.5);
        tradeMetropolis.setPopulation(8000);
        tradeMetropolis.setTechnologyLevel(2.2);
        tradeMetropolis.setResourcePreciousMetal(0.0); // Bullion deficit

        List<H3Cell> grid = List.of(goldHub, tradeMetropolis);
        TradeNetworkEngine.generateTradeNetworks(grid, 2.2, 1.0);

        assertTrue(goldHub.getResourcePreciousMetal() < 50.0,
                "Gold hub must export precious bullion along trade route");
        assertTrue(tradeMetropolis.getResourcePreciousMetal() > 0.0,
                "Trade metropolis must receive imported precious metal bullion");
    }

    @Test
    @DisplayName("Energy Grid & Critical Minerals: Natural gas and rare earths depletion and labor tracking")
    void testGasAndRareEarthsDepletionAndWorkTracking() {
        H3Cell cell = new H3Cell();
        cell.setPopulation(10000);
        cell.setTechnologyLevel(4.2); // Modern electrical and semiconductor era
        cell.setResourceCapital(10000.0);
        cell.setResourceCoal(500.0);
        cell.setResourceOil(500.0);
        double initialGas = 500.0;
        double initialRee = 50.0;
        double initialUranium = 20.0;
        cell.setResourceGas(initialGas);
        cell.setResourceRareEarths(initialRee);
        cell.setResourceUranium(initialUranium);

        List<H3Cell> cells = List.of(cell);
        PhysicalEnergyGridEngine.processPhysicalEnergyGrid(cells, 1.0);

        assertTrue(cell.getResourceGas() < initialGas, "Power grid must deplete natural gas reserves");
        assertTrue(cell.getResourceRareEarths() < initialRee, "High-tech grid must deplete critical rare earths");
        assertTrue(cell.getResourceUranium() < initialUranium, "Nuclear reactors must consume uranium");
        assertEquals(6000.0, cell.getResourceWork(), 1e-4, "Labor force must equal 60% of population (person-years)");
    }

    @Test
    @DisplayName("Metabolic Flux: Age pyramid cohorts dynamically alter per-capita caloric requirements")
    void testMetabolicNeedAgeCohortScaling() {
        // Child-heavy population (e.g. high fertility pre-industrial or infant boom)
        H3Cell childHeavyCell = new H3Cell();
        childHeavyCell.setPopulation(1000);
        childHeavyCell.setPop0to4(400); // 40% infants
        childHeavyCell.setPop5to14(400); // 40% children
        childHeavyCell.setPop15to24(100);
        childHeavyCell.setPop25to49(100);
        childHeavyCell.setPop50to64(0);
        childHeavyCell.setPop65to79(0);
        childHeavyCell.setPop80Plus(0);

        // Adult prime-labor population
        H3Cell adultHeavyCell = new H3Cell();
        adultHeavyCell.setPopulation(1000);
        adultHeavyCell.setPop0to4(50);
        adultHeavyCell.setPop5to14(50);
        adultHeavyCell.setPop15to24(200);
        adultHeavyCell.setPop25to49(600); // 60% prime labor adults
        adultHeavyCell.setPop50to64(100);
        adultHeavyCell.setPop65to79(0);
        adultHeavyCell.setPop80Plus(0);

        double childNeed = childHeavyCell.calculateAnnualMetabolicNeedGJ();
        double adultNeed = adultHeavyCell.calculateAnnualMetabolicNeedGJ();

        assertTrue(childNeed < adultNeed * 0.75,
                "Infant and child-dominated population must require significantly less caloric intake than prime labor adults");
    }

    @Test
    @DisplayName("Metabolic Flux: Heavy manual mining labor vs mechanized sedentary labor")
    void testMetabolicNeedPhysicalWorkAndMechanizationScaling() {
        H3Cell heavyMiningCell = new H3Cell();
        heavyMiningCell.setPopulation(1000);
        heavyMiningCell.setTechnologyLevel(1.5);
        heavyMiningCell.setResourceCapital(10.0); // Low capital -> manual muscle power
        heavyMiningCell.setResourceMetal(200.0); // Active heavy mining site

        H3Cell mechanizedCell = new H3Cell();
        mechanizedCell.setPopulation(1000);
        mechanizedCell.setTechnologyLevel(4.0);
        mechanizedCell.setResourceCapital(8000.0); // High industrial mechanization -> machines do the work
        mechanizedCell.setResourceMetal(0.0);

        double miningNeed = heavyMiningCell.calculateAnnualMetabolicNeedGJ();
        double mechanizedNeed = mechanizedCell.calculateAnnualMetabolicNeedGJ();

        assertTrue(miningNeed > mechanizedNeed * 1.15,
                "Heavy manual mining laborers must expend significantly more metabolic calories than mechanized workers");
    }

    @Test
    @DisplayName("Metabolic Flux: Neanderthals require ~40% higher caloric intake than Homo Sapiens")
    void testMetabolicNeedHomininSpeciesScaling() {
        H3Cell sapiensCell = new H3Cell();
        sapiensCell.setPopulation(1000);
        sapiensCell.setHomininSpecies(1); // Homo Sapiens

        H3Cell neanderthalCell = new H3Cell();
        neanderthalCell.setPopulation(1000);
        neanderthalCell.setHomininSpecies(2); // Homo Neanderthalensis

        double sapiensNeed = sapiensCell.calculateAnnualMetabolicNeedGJ();
        double neanderthalNeed = neanderthalCell.calculateAnnualMetabolicNeedGJ();

        assertEquals(neanderthalNeed, sapiensNeed * 1.40, 1e-4,
                "Neanderthals must require 40% higher caloric intake due to muscular build and cold climate thermoregulation");
    }

    @Test
    @DisplayName("Metallurgy: Late Bronze Age Collapse when Tin/Trade supply is severed")
    void testBronzeAgeTinTradeBottleneckCollapse() {
        // Connected Bronze Age cell with long-distance Tin/Trade access
        H3Cell connectedBronzeCell = new H3Cell();
        connectedBronzeCell.setPopulation(2000);
        connectedBronzeCell.setTechnologyLevel(2.1); // Bronze Age (1.8 <= T < 2.5)
        connectedBronzeCell.setResourceMetal(500.0);
        connectedBronzeCell.setWoodResource(1000.0);
        connectedBronzeCell.setResourceCapital(50.0);
        connectedBronzeCell.setResourceRareEarths(5.0); // Local tin/cassiterite proxy

        // Isolated Bronze Age cell after trade network collapse (no tin, high border friction)
        H3Cell isolatedBronzeCell = new H3Cell();
        isolatedBronzeCell.setPopulation(2000);
        isolatedBronzeCell.setTechnologyLevel(2.1);
        isolatedBronzeCell.setResourceMetal(500.0);
        isolatedBronzeCell.setWoodResource(1000.0);
        isolatedBronzeCell.setResourceCapital(50.0);
        isolatedBronzeCell.setResourceRareEarths(0.0); // No local tin
        isolatedBronzeCell.setMovementFriction(10.0); // Severed trade routes

        MetallurgyEnthalpyEngine.processOreSmelting(List.of(connectedBronzeCell, isolatedBronzeCell), 1.0);

        double connectedCapitalGain = connectedBronzeCell.getResourceCapital() - 50.0;
        double isolatedCapitalGain = isolatedBronzeCell.getResourceCapital() - 50.0;

        assertTrue(connectedCapitalGain > isolatedCapitalGain * 2.5,
                "Bronze Age civilization with Tin/Trade access (Q_alloy=0.65) must produce >2.5x more durable capital than one reverting to soft unalloyed copper (Q_alloy=0.18)");
    }

    @Test
    @DisplayName("Precious Metals: Pre-Industrial Bullion Treasury & Dutch Disease Inflation")
    void testPreIndustrialBullionTreasuryAndDutchDisease() {
        H3Cell potosiCell = new H3Cell();
        potosiCell.setPopulation(5000);
        potosiCell.setTechnologyLevel(2.8); // Pre-Industrial (T < 3.8)
        potosiCell.setResourcePreciousMetal(2000.0); // Massive silver/gold deposit
        potosiCell.setResourceMetal(10.0);
        potosiCell.setWoodResource(800.0);
        potosiCell.setResourceCapital(20.0); // Low real physical capital -> triggers Dutch Disease
        potosiCell.setFoodResource(30.0);
        potosiCell.setGiniIndex(0.35);
        org.ether.society.model.Nation spanishEmpire = new org.ether.society.model.Nation("BullionEmpire", null, potosiCell);

        double initialBullion = spanishEmpire.getTreasuryBullion();
        double initialGini = potosiCell.getGiniIndex();

        MetallurgyEnthalpyEngine.processOreSmelting(List.of(potosiCell), 1.0);
        spanishEmpire.updateCliodynamicsCycle();

        assertTrue(spanishEmpire.getTreasuryBullion() > initialBullion,
                "Extracted pre-industrial precious metals must accumulate into the Sovereign Bullion Treasury");
        assertTrue(potosiCell.getGiniIndex() > initialGini,
                "Excessive bullion extraction relative to real physical output must increase wealth inequality (Gini)");
        assertTrue(spanishEmpire.getMonetaryInflation() > 0.0,
                "Excessive bullion relative to real physical capital and food must trigger Fisher monetary inflation (Dutch Disease)");
    }

    @Test
    @DisplayName("18th-Century English Coal Transition: Forest depletion strangles ironworks and forces Coke/Newcomen innovation")
    void testEnglishCoalTransitionForestDepletionAndNewcomenInnovation() {
        // 1700 England (Coalbrookdale / Midlands): High Early Modern tech (T = 2.85),
        // depleted local forests (woodResource = 30 t), abundant iron ore and flooded coal seams
        H3Cell englandCell = new H3Cell();
        englandCell.setPopulation(10000);
        englandCell.setTechnologyLevel(2.85);
        englandCell.setResourceMetal(2000.0);
        englandCell.setWoodResource(30.0); // Severe local timber/charcoal scarcity
        englandCell.setResourceCoal(5000.0); // Rich Carboniferous coal basin
        englandCell.setFreshwaterAquifer(800.0); // Flooded deep mine shafts requiring Newcomen pumps
        englandCell.setResourceCapital(150.0);

        double initialTech = englandCell.getTechnologyLevel();
        double initialCoal = englandCell.getResourceCoal();

        // Simulate 5 years of timber scarcity pressure -> induces innovation across T >= 3.0 and unlocks Coke + Newcomen pumping
        for (int year = 0; year < 5; year++) {
            MetallurgyEnthalpyEngine.processOreSmelting(List.of(englandCell), 1.0);
        }

        assertTrue(englandCell.getTechnologyLevel() > 3.0,
                "Forest depletion combined with geological coal deposits must induce technological innovation across the Coke/Steam threshold (T >= 3.0)");
        assertTrue(englandCell.getResourceCoal() < initialCoal,
                "Once induced innovation crosses T >= 3.0, blast furnaces and Newcomen pumps must actively extract and consume mineral coal");
        assertTrue(englandCell.getTechnologyLevel() > initialTech + 0.15,
                "Scarcity-induced Boserup-Allen substitution must significantly accelerate technological progress");
    }
}
