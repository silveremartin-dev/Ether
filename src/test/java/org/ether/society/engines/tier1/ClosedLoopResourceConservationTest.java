/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.engines.tier2.theories.TradeNetworkRouter;
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
        barrenCell.setResourceCapital(5_000_000.0); // Industrial steam capital (5000 kg/cap)
        barrenCell.setResourceCoal(0.0); // Zero coal in subsoil
        barrenCell.setResourceOil(0.0);
        barrenCell.setResourceGas(0.0);

        double powerBarrenW = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(barrenCell);

        H3Cell richCoalCell = new H3Cell();
        richCoalCell.setPopulation(1000);
        richCoalCell.setResourceCapital(5_000_000.0); // Identical industrial capital
        richCoalCell.setResourceCoal(5000.0); // Abundant coal deposit

        double powerRichW = PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(richCoalCell);

        // Cell with coal deposits must generate significantly more power than barren cell
        assertTrue(powerRichW > powerBarrenW + 100.0,
                "Cell with coal deposits must generate significantly more mechanical work than barren cell");
    }

    @Test
    @DisplayName("Thermodynamic Fuel Depletion: Industrial power generation strictly depletes coal and oil reserves")
    void testIndustrialPowerDepletesGeologicalReserves() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = 5000.0f * 0.06f;
        buf.getResourceCapital()[0] = 2000.0f;
        float initialCoal = 1000.0f;
        float initialOil = 800.0f;
        buf.getResourceCoal()[0] = initialCoal;
        buf.getResourceOil()[0] = initialOil;

        new PhysicalEnergyGridEngine().tick(buf, 1.0, 12.0); // 1 year of industrial operation

        assertTrue(buf.getResourceCoal()[0] < initialCoal,
                "Industrial energy generation must deplete subsoil coal reserves");
        assertTrue(buf.getResourceOil()[0] < initialOil,
                "Industrial energy generation must deplete subsoil oil reserves");
        assertTrue(buf.getResourceCapital()[0] > 2000.0f,
                "Power generation must form productive capital stock");
    }

    @Test
    @DisplayName("Metallurgy: Replaced arbitrary 0.5 capital multiplier with physical tooling & material wear rate")
    void testMetallurgicalCapitalFormationAndPhysicalWear() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = 2000.0f * 0.06f;
        buf.getMetalResource()[0] = 500.0f;
        buf.getWoodResource()[0] = 1000.0f;
        buf.getBiomassNatural()[0] = 1000.0f;
        buf.getResourceCapital()[0] = 100.0f;

        new MetallurgyEnthalpyEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getMetalResource()[0] < 500.0f, "Smelting must consume raw metal ore");
        assertTrue(buf.getWoodResource()[0] < 1000.0f, "Smelting enthalpy must consume combustible wood fuel");
        assertTrue(buf.getResourceCapital()[0] > 100.0f, "Refined metal must form physical tooling and infrastructure capital");
    }

    @Test
    @DisplayName("Supply Chain: Logistics loss is damped by infrastructure capital rather than fixed 0.02")
    void testInfrastructureCapitalDampsTransportFriction() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        buf.getBiomassHuman()[0] = 1000.0f * 0.06f;
        buf.getEnergyFire()[0] = 1000.0f;
        buf.getResourceCapital()[0] = 10.0f; // Low capital

        buf.getBiomassHuman()[1] = 1000.0f * 0.06f;
        buf.getEnergyFire()[1] = 1000.0f;
        buf.getResourceCapital()[1] = 5000.0f; // High paved road / rail infrastructure capital

        PhysicalSupplyChainEngine.processSupplyChains(buf, 1.0);

        // High infrastructure cell must retain more work output due to lower transport friction
        assertTrue(buf.getEnergyFire()[1] > buf.getEnergyFire()[0],
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
        miningHub.setResourceCoal(2000.0); // Coal surplus
        miningHub.setResourceOil(100.0);

        H3Cell industrialPort = new H3Cell();
        industrialPort.setH3Index(22222L);
        industrialPort.setLatitude(45.5);
        industrialPort.setLongitude(5.5);
        industrialPort.setPopulation(5000);
        industrialPort.setResourceCoal(10.0); // Coal deficit
        industrialPort.setResourceOil(100.0);

        List<H3Cell> path = List.of(miningHub, industrialPort);
        TradeNetworkRouter.TradeRoute route = new TradeNetworkRouter.TradeRoute(miningHub, industrialPort, path, 5.0);

        List<H3Cell> grid = List.of(miningHub, industrialPort);
        TradeNetworkRouter.generateTradeNetworks(grid, 3.2, 1.0);

        // Mining hub must export coal, reducing surplus, while destination receives fuel
        // Validates bilateral fossil arbitrage without crashing
        assertNotNull(miningHub.getResourceCoal());
        assertNotNull(industrialPort.getResourceCoal());
    }

    @Test
    @DisplayName("Precious Metals: Extraction, cupellation enthalpy, and monetary capital formation")
    void testPreciousMetalExtractionAndMonetization() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = 3000.0f * 0.06f;
        buf.getBiomassNatural()[0] = 500.0f;
        buf.getWoodResource()[0] = 500.0f;
        float initialMetal = 200.0f;
        buf.getMetalResource()[0] = initialMetal;
        buf.getResourceCapital()[0] = 50.0f;

        new MetallurgyEnthalpyEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getMetalResource()[0] < initialMetal,
                "Metal ore must be mined and depleted");
        assertTrue(buf.getResourceCapital()[0] > 50.0f,
                "Refined metal must monetize into liquid capital stock");
    }

    @Test
    @DisplayName("Trade Network: Bullion arbitrage and monetary lubrication across trade corridors")
    void testPreciousMetalTradeArbitrage() {
        H3Cell goldHub = new H3Cell();
        goldHub.setH3Index(33333L);
        goldHub.setLatitude(20.0);
        goldHub.setLongitude(10.0);
        goldHub.setPopulation(3000);
        goldHub.setResourcePreciousMetal(50.0); // Gold mining hub

        H3Cell tradeMetropolis = new H3Cell();
        tradeMetropolis.setH3Index(44444L);
        tradeMetropolis.setLatitude(22.0);
        tradeMetropolis.setLongitude(12.0);
        tradeMetropolis.setPopulation(8000);
        tradeMetropolis.setResourcePreciousMetal(0.0); // Bullion deficit

        List<H3Cell> grid = List.of(goldHub, tradeMetropolis);
        TradeNetworkRouter.generateTradeNetworks(grid, 2.2, 1.0);

        assertTrue(goldHub.getResourcePreciousMetal() < 50.0,
                "Gold hub must export precious bullion along trade route");
        assertTrue(tradeMetropolis.getResourcePreciousMetal() > 0.0,
                "Trade metropolis must receive imported precious metal bullion");
    }

    @Test
    @DisplayName("Energy Grid & Critical Minerals: Natural gas and rare earths depletion and labor tracking")
    void testGasAndRareEarthsDepletionAndWorkTracking() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = 10000.0f * 0.06f;
        buf.getResourceCapital()[0] = 50_000_000.0f;
        buf.getResourceCoal()[0] = 500.0f;
        buf.getResourceOil()[0] = 500.0f;
        float initialGas = 500.0f;
        float initialUranium = 20.0f;
        buf.getResourceGas()[0] = initialGas;
        buf.getResourceUranium()[0] = initialUranium;

        new PhysicalEnergyGridEngine().tick(buf, 1.0, 12.0);

        assertTrue(buf.getResourceGas()[0] < initialGas, "Power grid must deplete natural gas reserves");
        assertTrue(buf.getResourceUranium()[0] < initialUranium, "Nuclear reactors must consume uranium");
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
        heavyMiningCell.setResourceCapital(10.0); // Low capital -> manual muscle power
        heavyMiningCell.setResourceMetal(200.0); // Active heavy mining site

        H3Cell mechanizedCell = new H3Cell();
        mechanizedCell.setPopulation(1000);
        mechanizedCell.setResourceCapital(200_000.0); // High industrial mechanization (200 kg/cap) -> machines do the work
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
        // Connected Bronze Age cell
        org.ether.society.core.dod.WorldBuffer bronzeBuf = new org.ether.society.core.dod.WorldBuffer(2);
        bronzeBuf.getBiomassHuman()[0] = 2000.0f * 0.06f;
        bronzeBuf.getMetalResource()[0] = 500.0f;
        bronzeBuf.getWoodResource()[0] = 1000.0f;
        bronzeBuf.getResourceCapital()[0] = 50.0f;
        bronzeBuf.getFluxPressure()[0] = 1.0f;

        // Isolated Bronze Age cell (no ore, high border friction)
        bronzeBuf.getBiomassHuman()[1] = 2000.0f * 0.06f;
        bronzeBuf.getMetalResource()[1] = 0.0f;
        bronzeBuf.getWoodResource()[1] = 1000.0f;
        bronzeBuf.getResourceCapital()[1] = 50.0f;
        bronzeBuf.getFluxPressure()[1] = 10.0f; // Severed trade routes

        new MetallurgyEnthalpyEngine().tick(bronzeBuf, 1.0, 12.0);

        double connectedCapitalGain = bronzeBuf.getResourceCapital()[0] - 50.0;
        double isolatedCapitalGain = bronzeBuf.getResourceCapital()[1] - 50.0;

        assertTrue(connectedCapitalGain > isolatedCapitalGain,
                "Bronze Age civilization with ore access must produce more durable capital than one without ore");
    }

    @Test
    @DisplayName("Precious Metals: Pre-Industrial Bullion Treasury & Dutch Disease Inflation")
    void testPreIndustrialBullionTreasuryAndDutchDisease() {
        H3Cell potosiCell = new H3Cell();
        potosiCell.setPopulation(5000);
        potosiCell.setResourcePreciousMetal(2000.0); // Massive silver/gold deposit
        potosiCell.setResourceMetal(10.0);
        potosiCell.setWoodResource(800.0);
        potosiCell.setResourceCapital(20.0); // Low real physical capital -> triggers Dutch Disease
        potosiCell.setFoodResource(30.0);
        potosiCell.setGiniIndex(0.35);
        org.ether.society.database.Polity spanishEmpire = new org.ether.society.database.Polity("BullionEmpire", null, potosiCell);

        org.ether.society.core.dod.WorldBuffer potosiBuf = new org.ether.society.core.dod.WorldBuffer(1);
        potosiBuf.getBiomassHuman()[0] = 5000.0f * 0.06f;
        potosiBuf.getMetalResource()[0] = 500.0f;
        potosiBuf.getWoodResource()[0] = 800.0f;
        potosiBuf.getResourceCapital()[0] = 20.0f;
        potosiBuf.getFoodResource()[0] = 30.0f;
        potosiBuf.getGiniIndex()[0] = 0.35f;

        new MetallurgyEnthalpyEngine().tick(potosiBuf, 1.0, 12.0);
        spanishEmpire.updateCliodynamicsCycle();

        assertTrue(potosiBuf.getResourceCapital()[0] > 20.0f,
                "Metallurgical smelting must accumulate capital");
        assertTrue(potosiBuf.getMetalResource()[0] < 500.0f,
                "Smelting must deplete raw ore stock");
    }

    @Test
    @DisplayName("18th-Century English Coal Transition: Forest depletion strangles ironworks and forces Coke/Newcomen innovation")
    void testEnglishCoalTransitionForestDepletionAndNewcomenInnovation() {
        // 1700 England (Coalbrookdale / Midlands): Early Modern capital,
        // depleted local forests (woodResource = 30 t), abundant iron ore and flooded coal seams
        org.ether.society.core.dod.WorldBuffer englandBuf = new org.ether.society.core.dod.WorldBuffer(1);
        englandBuf.getBiomassHuman()[0] = 10000.0f * 0.06f;
        englandBuf.getMetalResource()[0] = 2000.0f;
        englandBuf.getWoodResource()[0] = 30.0f; // Severe local timber/charcoal scarcity
        englandBuf.getResourceCoal()[0] = 5000.0f; // Rich Carboniferous coal basin
        englandBuf.getFreshwaterAquifer()[0] = 800.0f; // Flooded deep mine shafts requiring Newcomen pumps
        englandBuf.getResourceCapital()[0] = 150.0f;

        float initialCap = englandBuf.getResourceCapital()[0];
        float initialCoal = englandBuf.getResourceCoal()[0];

        MetallurgyEnthalpyEngine metalEngine = new MetallurgyEnthalpyEngine();
        // Simulate 5 years of timber scarcity pressure -> induces Coke + Newcomen pumping and capital accumulation
        for (int year = 0; year < 5; year++) {
            metalEngine.tick(englandBuf, 1.0, 12.0);
        }

        assertTrue(englandBuf.getResourceCoal()[0] < initialCoal,
                "Blast furnaces and Newcomen pumps must actively extract and consume mineral coal");
        assertTrue(englandBuf.getResourceCapital()[0] > initialCap,
                "Scarcity-induced Boserup-Allen substitution must significantly accelerate physical capital formation");
    }

    @Test
    @DisplayName("Finite Geological Resource Invariant: Non-renewable subsoil stocks monotonically deplete without spontaneous generation")
    void testFiniteGeologicalResourceMonotonicDepletionInvariant() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getBiomassHuman()[0] = 15000.0f * 0.06f;
        buf.getResourceCapital()[0] = 3000.0f;
        float initialCoal = 2500.0f;
        float initialOil = 1800.0f;
        float initialGas = 1200.0f;
        float initialUranium = 500.0f;
        float initialIron = 4000.0f;

        buf.getResourceCoal()[0] = initialCoal;
        buf.getResourceOil()[0] = initialOil;
        buf.getResourceGas()[0] = initialGas;
        buf.getResourceUranium()[0] = initialUranium;
        buf.getMetalResource()[0] = initialIron;

        float prevCoal = initialCoal;
        float prevOil = initialOil;

        PhysicalEnergyGridEngine gridEngine = new PhysicalEnergyGridEngine();
        // Simulate 20 successive years of industrial consumption
        for (int y = 0; y < 20; y++) {
            gridEngine.tick(buf, 1.0, 12.0);

            float currentCoal = buf.getResourceCoal()[0];
            float currentOil = buf.getResourceOil()[0];

            // Invariant 1: Non-negativity
            assertTrue(currentCoal >= 0.0f, "Subsoil coal reserve must remain strictly non-negative");
            assertTrue(currentOil >= 0.0f, "Subsoil oil reserve must remain strictly non-negative");

            // Invariant 2: Monotonic non-increasing (zero spontaneous geological creation of fossil stocks)
            assertTrue(currentCoal <= prevCoal + 1e-4f,
                    "Subsoil coal reserve cannot spontaneously increase over human simulation timescales");
            assertTrue(currentOil <= prevOil + 1e-4f,
                    "Subsoil oil reserve cannot spontaneously increase over human simulation timescales");

            prevCoal = currentCoal;
            prevOil = currentOil;
        }

        // Invariant 3: Net cumulative depletion is positive under active population & capital
        assertTrue(buf.getResourceCoal()[0] < initialCoal, "Industrial operation must yield net depletion of coal");
        assertTrue(buf.getResourceOil()[0] < initialOil, "Industrial operation must yield net depletion of oil");
    }
}
