/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.HabitatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous physical verification suite for Tier 1 Extraterrestrial Pressurized Habitats,
 * Environmental Life Support Systems (ECLSS), Modular Bulkhead Containment,
 * Maintenance Thermodynamics, and Hypoxia/Exposure Mortality Dynamics.
 */
public class PressurizedHabitatEngineTest {

    @Test
    @DisplayName("1. Environmental Hostility: Classifies vacuum, acid atmospheres, and thermal extremes")
    public void testHostilePlanetDetection() {
        assertTrue(PressurizedHabitatEngine.isHostileEnvironment(PlanetPreset.MARS_LIKE),
                "Mars (0.006 atm, -63°C, 0.1% O2) must be classified as hostile");
        assertTrue(PressurizedHabitatEngine.isHostileEnvironment(PlanetPreset.MOON_LIKE),
                "Moon (hard vacuum, extreme thermal cycle) must be classified as hostile");
        assertTrue(PressurizedHabitatEngine.isHostileEnvironment(PlanetPreset.VENUS_LIKE),
                "Venus (92 atm, 465°C, supercritical CO2/acid) must be classified as hostile");
        assertFalse(PressurizedHabitatEngine.isHostileEnvironment(PlanetPreset.EARTH_MODERN),
                "Modern Earth (1.0 atm, 15°C, 21% O2) must be habitable without pressurized domes");
    }

    @Test
    @DisplayName("2. Geological Adaptation: Pioneer habitats adapt structural morphology to local terrain")
    public void testPioneerOutpostInitialization() {
        // High volcanic ridge on Mars -> Lava Tube
        H3Cell marsRidge = new H3Cell(0x881f1d4887fffffL, 18.0, 77.0);
        marsRidge.setElevation(2500.0);
        PressurizedHabitatEngine.initializePioneerOutpost(marsRidge, PlanetPreset.MARS_LIKE, 500);

        assertEquals(HabitatType.LAVA_TUBE, marsRidge.getHabitatType(),
                "Volcanic high ground on Mars must utilize natural subsurface lava tubes");
        assertEquals(1.0, marsRidge.getHabitatIntegrity(), 1e-6);
        assertEquals(1000.0, marsRidge.getHabitatCapacity(), "Baseline capacity floor is 1,000 for pioneer outposts");
        assertEquals(500 * HabitatType.LAVA_TUBE.getBaseEnergyKwPerCapita(), marsRidge.getHabitatEnergyKw(), 1e-3);

        // Venusian dense atmosphere -> Aerostat Cloud City
        H3Cell venusAtm = new H3Cell(0x881f1d4887ffffeL, 0.0, 0.0);
        PressurizedHabitatEngine.initializePioneerOutpost(venusAtm, PlanetPreset.VENUS_LIKE, 4000);
        assertEquals(HabitatType.VENUS_AEROSTAT, venusAtm.getHabitatType(),
                "Venus super-critical surface forces aerostat buoyancy at 55 km altitude");
        assertEquals(6000.0, venusAtm.getHabitatCapacity(), "Capacity adapts to 1.5x initial population");
    }

    @Test
    @DisplayName("3. Unprotected Exposure Dynamics: Mass mortality and cohort synchronization on hostile worlds")
    public void testUnprotectedExposureMortality() {
        H3Cell exposedCell = new H3Cell(0x881f1d4887fffffL, 0.0, 0.0);
        exposedCell.setPopulation(1000);
        exposedCell.setBiomassHuman(1000.0);
        exposedCell.setHabitatType(HabitatType.NONE);

        // 1 month exposure (dt = 1/12 yr)
        double dt = 1.0 / 12.0;
        PressurizedHabitatEngine.processPressurizedHabitats(List.of(exposedCell), PlanetPreset.MARS_LIKE, dt);

        // Mortality rate = 0.95 * dt -> ~7.9% mortality per month unprotected
        int popAfter1Month = exposedCell.getPopulation();
        assertTrue(popAfter1Month < 1000, "Unprotected population must suffer immediate mortality");
        assertEquals((double) popAfter1Month, exposedCell.getBiomassHuman(), 1e-5,
                "Human biomass must remain synchronized with surviving population");

        // Full year exposure (dt = 1.0)
        exposedCell.setPopulation(1000);
        PressurizedHabitatEngine.processPressurizedHabitats(List.of(exposedCell), PlanetPreset.MARS_LIKE, 1.0);
        assertEquals(50, exposedCell.getPopulation(),
                "1 full year in Martian vacuum without habitat leaves only 5% survivors (95% annual mortality)");
    }

    @Test
    @DisplayName("4. Maintenance Thermodynamics: Capital/metal funding mitigates wear and repairs structural hull")
    public void testHabitatWearAndMaintenance() {
        // Case A: Well-funded habitat (abundant capital & metal)
        H3Cell funded = new H3Cell(0x881f1d4887fffffL, 0.0, 0.0);
        funded.setPopulation(100);
        funded.setHabitatType(HabitatType.SURFACE_DOME);
        funded.setHabitatCapacity(200.0);
        funded.setHabitatIntegrity(0.90);
        funded.setResourceCapital(50000.0); // Abundant maintenance capital
        funded.setResourceMetal(10000.0);

        PressurizedHabitatEngine.processPressurizedHabitats(List.of(funded), PlanetPreset.MARS_LIKE, 1.0);

        assertTrue(funded.getHabitatIntegrity() > 0.90,
                "Fully funded maintenance (coverage > 95%) must actively repair damaged hull (+5%/yr)");
        assertTrue(funded.getResourceCapital() < 50000.0, "Capital must be deducted for maintenance work");
        assertTrue(funded.getResourceMetal() < 10000.0, "Metal stocks must be consumed for seal/hull repairs");

        // Case B: Abandoned habitat (0 capital, 0 metal)
        H3Cell starved = new H3Cell(0x881f1d4887ffffeL, 0.0, 0.0);
        starved.setPopulation(100);
        starved.setHabitatType(HabitatType.SURFACE_DOME);
        starved.setHabitatCapacity(200.0);
        starved.setHabitatIntegrity(0.90);
        starved.setResourceCapital(0.0);
        starved.setResourceMetal(0.0);

        PressurizedHabitatEngine.processPressurizedHabitats(List.of(starved), PlanetPreset.MARS_LIKE, 1.0);

        assertTrue(starved.getHabitatIntegrity() < 0.90,
                "Unmaintained habitat in hostile environment must suffer unmitigated structural decay");
    }

    @Test
    @DisplayName("5. ECLSS Energetics: Life-support power demand surges as structural integrity degrades")
    public void testEclssPowerDemandSurgeOnIntegrityLoss() {
        H3Cell pristine = new H3Cell(0x881f1d4887fffffL, 0.0, 0.0);
        pristine.setPopulation(500);
        pristine.setHabitatType(HabitatType.SURFACE_DOME);
        pristine.setHabitatCapacity(1000.0);
        pristine.setHabitatIntegrity(1.0);
        pristine.setResourceCapital(10000.0);
        pristine.setResourceMetal(5000.0);

        H3Cell degraded = new H3Cell(0x881f1d4887ffffeL, 0.0, 0.0);
        degraded.setPopulation(500);
        degraded.setHabitatType(HabitatType.SURFACE_DOME);
        degraded.setHabitatCapacity(1000.0);
        degraded.setHabitatIntegrity(0.40); // 60% hull degradation
        degraded.setResourceCapital(10000.0);
        degraded.setResourceMetal(5000.0);

        PressurizedHabitatEngine.processPressurizedHabitats(List.of(pristine, degraded), PlanetPreset.MARS_LIKE, 0.1);

        double powerPristine = pristine.getHabitatEnergyKw();
        double powerDegraded = degraded.getHabitatEnergyKw();

        // Formula: energy = pop * baseEnergy * (1.0 + 0.50 * (1 - integrity))
        assertTrue(powerDegraded > powerPristine,
                "Degraded habitat must draw substantially more ECLSS power to compensate for thermal/air leakage");
        double expectedDegradedPower = 500.0 * HabitatType.SURFACE_DOME.getBaseEnergyKwPerCapita()
                * (1.0 + 0.50 * (1.0 - degraded.getHabitatIntegrity()));
        assertEquals(expectedDegradedPower, powerDegraded, 1e-2,
                "ECLSS energy penalty must strictly follow the thermodynamic degradation coefficient");
    }

    @Test
    @DisplayName("6. Modular Bulkhead Containment: Localized hull breach prevents instantaneous total colony wipeout")
    public void testModularBulkheadContainmentUnderBreach() {
        H3Cell breachedCell = new H3Cell(0x881f1d4887fffffL, 0.0, 0.0);
        breachedCell.setPopulation(600); // 600 colonists in modular complex
        breachedCell.setHabitatType(HabitatType.SURFACE_DOME);
        breachedCell.setHabitatCapacity(1000.0);
        breachedCell.setHabitatIntegrity(0.15); // Severe hull breach (integrity < 0.30)
        breachedCell.setResourceCapital(0.0);
        breachedCell.setResourceMetal(0.0);

        // 1 month breach exposure (dt = 1/12)
        PressurizedHabitatEngine.processPressurizedHabitats(List.of(breachedCell), PlanetPreset.MARS_LIKE, 1.0 / 12.0);

        int survivors = breachedCell.getPopulation();
        assertTrue(survivors > 0,
                "Automatic airlock bulkheads must isolate the depressurized compartment, saving adjacent modules");
        assertTrue(survivors < 600,
                "Depressurization in the compromised compartment must inflict localized casualties");
    }

    @Test
    @DisplayName("7. Overcrowding Hypoxia Kinetics: Excess population above life-support rating suffers analytical decay")
    public void testHypoxiaOvercrowdingKinetics() {
        H3Cell overcrowded = new H3Cell(0x881f1d4887fffffL, 0.0, 0.0);
        overcrowded.setPopulation(1500); // 500 excess souls above ECLSS rating
        overcrowded.setHabitatType(HabitatType.SURFACE_DOME);
        overcrowded.setHabitatCapacity(1000.0);
        overcrowded.setHabitatIntegrity(1.0);
        overcrowded.setResourceCapital(100.0); // Insufficient resources to trigger auto-expansion
        overcrowded.setResourceMetal(10.0);

        double dt = 0.5; // 6 months
        PressurizedHabitatEngine.processPressurizedHabitats(List.of(overcrowded), PlanetPreset.MARS_LIKE, dt);

        // Theoretical deaths: excess * (1 - exp(-2.5 * dt)) = 500 * (1 - exp(-1.25)) ≈ 500 * 0.7135 ≈ 357 deaths
        int expectedRemaining = 1500 - (int) Math.round(500.0 * (1.0 - Math.exp(-2.5 * dt)));
        assertEquals(expectedRemaining, overcrowded.getPopulation(), 2,
                "Hypoxia casualties must match the analytical life-support depletion curve excess * (1 - exp(-2.5*dt))");
    }

    @Test
    @DisplayName("8. Autonomous Habitat Expansion: Reaching 80% capacity with adequate industrial surplus upgrades tier")
    public void testAutonomousHabitatExpansion() {
        H3Cell expanding = new H3Cell(0x881f1d4887fffffL, 0.0, 0.0);
        expanding.setPopulation(850); // > 80% of 1000
        expanding.setHabitatType(HabitatType.SURFACE_DOME);
        expanding.setHabitatTier(1);
        expanding.setHabitatCapacity(1000.0);
        expanding.setHabitatIntegrity(1.0);
        expanding.setResourceCapital(2000.0); // Capital surplus
        expanding.setResourceMetal(500.0);    // Metal surplus

        PressurizedHabitatEngine.processPressurizedHabitats(List.of(expanding), PlanetPreset.MARS_LIKE, 0.1);

        assertEquals(2, expanding.getHabitatTier(), "Surplus capital and demographic pressure must trigger Tier 2 expansion");
        assertEquals(5000.0, expanding.getHabitatCapacity(), "Tier 2 dome capacity must expand to 5,000 colonists");
        assertEquals(1200.0, expanding.getResourceCapital(), 1e-3, "Capital cost of expansion (800) must be deducted");
        assertEquals(350.0, expanding.getResourceMetal(), 1e-3, "Metal cost of expansion (150) must be deducted");
    }
}
