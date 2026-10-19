/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct unit test suite for previously untested Tier 2 historical and hybrid engines:
 * <ul>
 *   <li>{@link AiAutonomousRegulationPureEngine}</li>
 *   <li>{@link HandyNasaHybridEngine}</li>
 *   <li>{@link MaritimeHighwayEngine}</li>
 *   <li>{@link MilitaryTechShockEngine}</li>
 *   <li>{@link NordhausDiceHybridEngine}</li>
 *   <li>{@link SpatialCityFractalEngine}</li>
 *   <li>{@link World3HybridEngine}</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HistoricalAndHybridEnginesDirectTest {

    @Test
    @DisplayName("AiAutonomousRegulationPureEngine: remediates pollution and boosts work at high tech")
    public void testAiAutonomousRegulation() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell advanced = new H3Cell(0x8828308281fffffL, 45.0, 5.0);
        advanced.setTechnologyLevel(7.0);
        advanced.setResourceWork(100.0);
        advanced.setPollutionLevel(50.0);
        cells.add(advanced);

        H3Cell primitive = new H3Cell(0x8828308283fffffL, 46.0, 6.0);
        primitive.setTechnologyLevel(2.0);
        primitive.setResourceWork(100.0);
        primitive.setPollutionLevel(50.0);
        cells.add(primitive);

        AiAutonomousRegulationPureEngine.processHybrid(cells, 1.0);

        // High-tech cell has automated pollution remediation and work output boost
        assertTrue(advanced.getResourceWork() > 100.0, "Work must increase under AI regulation");
        assertTrue(advanced.getPollutionLevel() < 50.0, "Pollution must decrease under AI regulation");

        // Primitive cell remains unchanged by AI regulation
        assertEquals(100.0, primitive.getResourceWork(), 0.001);
        assertEquals(50.0, primitive.getPollutionLevel(), 0.001);
    }

    @Test
    @DisplayName("HandyNasaHybridEngine: verifies population-driven natural biomass depletion")
    public void testHandyNasaHybrid() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell cell = new H3Cell(0x8828308281fffffL, 10.0, 20.0);
        cell.setPopulation(50_000);
        cell.setBiomassNatural(100.0);
        cells.add(cell);

        HandyNasaHybridEngine.processPlugin(cells, 2.0);

        assertTrue(cell.getBiomassNatural() < 100.0, "Natural biomass must be depleted by dense population");
        assertTrue(cell.getBiomassNatural() >= 0.0, "Biomass cannot become negative");
    }

    @Test
    @DisplayName("MaritimeHighwayEngine: reduces friction and accelerates capital over open water")
    public void testMaritimeHighway() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell coastal = new H3Cell(0x8828308281fffffL, 35.0, 15.0);
        coastal.setPopulation(10_000);
        coastal.setIsCoastal(true);
        coastal.setTemperature(20.0);
        coastal.setResourceCapital(100.0);
        coastal.setMovementFriction(1.0);
        cells.add(coastal);

        H3Cell frozen = new H3Cell(0x8828308283fffffL, 75.0, 15.0);
        frozen.setPopulation(1_000);
        frozen.setIsCoastal(true);
        frozen.setTemperature(-10.0);
        frozen.setMovementFriction(1.0);
        cells.add(frozen);

        MaritimeHighwayEngine.processHybrid(cells, 1.0);

        // Warm coastal water reduces friction and generates trade capital
        assertTrue(coastal.getResourceCapital() > 100.0, "Coastal trade accelerates capital accumulation");
        assertTrue(coastal.getMovementFriction() < 1.0, "Buoyancy over water reduces transport friction");

        // Sub-zero water increases sea ice friction
        assertTrue(frozen.getMovementFriction() > 1.0, "Sub-zero ice conditions increase transport friction");
    }

    @Test
    @DisplayName("MilitaryTechShockEngine: accelerates military work and technology during shock window")
    public void testMilitaryTechShock() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell shockCell = new H3Cell(0x8828308281fffffL, 34.0, 135.0);
        shockCell.setTechnologyLevel(3.5);
        shockCell.setResourceWork(200.0);
        cells.add(shockCell);

        MilitaryTechShockEngine.processHybrid(cells, 1.0);

        assertTrue(shockCell.getResourceWork() > 200.0, "Military work must accelerate during technological shock");
        assertTrue(shockCell.getTechnologyLevel() > 3.5, "Technology consolidation must advance");
    }

    @Test
    @DisplayName("NordhausDiceHybridEngine: penalizes capital proportionally to quadratic temperature anomaly")
    public void testNordhausDiceHybrid() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell normal = new H3Cell(0x8828308281fffffL, 40.0, 0.0);
        normal.setTemperature(15.0); // No anomaly relative to 15C baseline
        normal.setResourceCapital(1000.0);
        cells.add(normal);

        H3Cell overheated = new H3Cell(0x8828308283fffffL, 41.0, 0.0);
        overheated.setTemperature(20.0); // +5C anomaly
        overheated.setResourceCapital(1000.0);
        cells.add(overheated);

        NordhausDiceHybridEngine.processPlugin(cells, 5.0);

        assertEquals(1000.0, normal.getResourceCapital(), 0.001, "Zero anomaly incurs no climate damage");
        assertTrue(overheated.getResourceCapital() < 1000.0, "Thermal anomaly must degrade capital via quadratic damage function");
    }

    @Test
    @DisplayName("SpatialCityFractalEngine: redistributes population according to Zipf-Mori spatial power law while conserving total population")
    public void testSpatialCityFractal() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell c1 = new H3Cell(0x8828308281fffffL, 48.8, 2.3);
        c1.setPopulation(100_000);
        cells.add(c1);

        H3Cell c2 = new H3Cell(0x8828308283fffffL, 45.7, 4.8);
        c2.setPopulation(50_000);
        cells.add(c2);

        H3Cell c3 = new H3Cell(0x8828308285fffffL, 43.3, 5.4);
        c3.setPopulation(20_000);
        cells.add(c3);

        long initialTotal = 100_000L + 50_000L + 20_000L;

        SpatialCityFractalEngine.processHybrid(cells, 10.0);

        long finalTotal = (long) c1.getPopulation() + c2.getPopulation() + c3.getPopulation();
        assertEquals(initialTotal, finalTotal, "Total population must be conserved during spatial fractal relaxation");
        assertTrue(c1.getPopulation() > c2.getPopulation(), "Primacy hierarchy rank 1 > rank 2 must be preserved");
        assertTrue(c2.getPopulation() > c3.getPopulation(), "Primacy hierarchy rank 2 > rank 3 must be preserved");
    }

    @Test
    @DisplayName("World3HybridEngine: enforces capital FCAOR depletion and pollution mortality penalty")
    public void testWorld3Hybrid() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell scarceAndPolluted = new H3Cell(0x8828308281fffffL, 51.5, -0.1);
        scarceAndPolluted.setPopulation(10_000);
        scarceAndPolluted.setResourceMetal(10.0); // Scarce mineral reserves (< 50.0)
        scarceAndPolluted.setResourceCapital(500.0);
        scarceAndPolluted.setPollutionLevel(40.0); // Severe pollution (> 20.0)
        scarceAndPolluted.setLifespan(70.0);
        cells.add(scarceAndPolluted);

        World3HybridEngine.processPlugin(cells, 2.0);

        assertTrue(scarceAndPolluted.getResourceCapital() < 500.0, "Capital must be sunk by high FCAOR under ore exhaustion");
        assertTrue(scarceAndPolluted.getLifespan() < 70.0, "Life expectancy must decline under heavy persistent pollution");
    }
}
