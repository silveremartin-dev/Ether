/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.core.PreComputePhase;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.ether.society.ui.ScenarioSetupPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScenarioRegistryIntegrationTest {

    private List<H3Cell> mockCells;

    @BeforeEach
    public void setUp() {
        mockCells = new ArrayList<>();

        H3Cell c0 = new H3Cell(200L, 48.8, 2.35); // Paris-like
        c0.setElevation(50.0);
        c0.setBiome(Biome.PLAINS);
        c0.setTemperature(15.0);
        c0.setWaterResource(500.0);

        H3Cell c1 = new H3Cell(201L, 30.0, 31.0); // Cairo-like
        c1.setElevation(20.0);
        c1.setBiome(Biome.PLAINS);
        c1.setTemperature(22.0);
        c1.setWaterResource(900.0);

        H3Cell c2 = new H3Cell(202L, 35.0, 139.0); // Tokyo-like
        c2.setElevation(15.0);
        c2.setBiome(Biome.BEACH);
        c2.setTemperature(16.0);
        c2.setWaterResource(800.0);

        H3Cell c3 = new H3Cell(203L, -23.5, -46.6); // Sao Paulo-like
        c3.setElevation(760.0);
        c3.setBiome(Biome.PLAINS);
        c3.setTemperature(20.0);
        c3.setWaterResource(700.0);

        H3Cell c4 = new H3Cell(204L, 28.6, 77.2); // New Delhi-like
        c4.setElevation(216.0);
        c4.setBiome(Biome.PLAINS);
        c4.setTemperature(25.0);
        c4.setWaterResource(850.0);

        mockCells.add(c0);
        mockCells.add(c1);
        mockCells.add(c2);
        mockCells.add(c3);
        mockCells.add(c4);
    }

    @Test
    public void testNuclearWinterScenarioDetectionAndForcing() {
        Scenario scenario = new Scenario();
        scenario.setName("Hiver Nucléaire & Ombre Stratosphérique (2035)");
        scenario.setStartDateYear(2035);
        scenario.setInitialHumanCount(5_000_000L);
        scenario.setInitialCapitalPerCapita(18000.0);
        scenario.setPopulationDensityType("URBAN_CLUSTERS");

        FutureScenarioRegistry.PhysicalScenarioPreset preset = FutureScenarioRegistry.findPresetForScenario(scenario);
        assertNotNull(preset, "Nuclear Winter scenario must be detected by FutureScenarioRegistry");
        assertEquals("NUCLEAR_WINTER", preset.id());

        // Apply precompute phase
        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(mockCells);

        // Verify nuclear warfare climate engine global soot optical depth set
        assertEquals(1.5, NuclearWarfareClimateEngine.getGlobalSootOpticalDepth(), 0.001,
                "Global soot optical depth must be 1.5 following Nuclear Winter scenario forcing");

        // Verify population assigned
        long totalPop = mockCells.stream().mapToLong(H3Cell::getPopulation).sum();
        assertEquals(5_000_000L, totalPop, 100L, "PreComputePhase must assign target population of approximately 5 million");

        // Verify physical capital seeded
        assertTrue(mockCells.get(0).getResourceCapital() > 0, "Cell capital must be seeded from initialCapitalPerCapita");
    }

    @Test
    public void testLongSpinnerValueFactoryNoOverflowOrCastException() {
        ScenarioSetupPanel.LongSpinnerValueFactory factory =
                new ScenarioSetupPanel.LongSpinnerValueFactory(1_000L, 10_000_000_000L, 1_000_000L, 100_000L);

        factory.setValue(8_500_000_000L);
        assertEquals(8_500_000_000L, factory.getValue(), "LongSpinnerValueFactory must handle 8.5 billion without exception");

        factory.increment(1);
        assertEquals(8_500_100_000L, factory.getValue());

        factory.decrement(2);
        assertEquals(8_499_900_000L, factory.getValue());
    }

    @Test
    public void testBusinessAsUsualScenarioForcing() {
        Scenario scenario = new Scenario();
        scenario.setName("Business As Usual : Fossil Fuel Reliance & Warming (SSP5-8.5)");
        scenario.setStartDateYear(2026);
        scenario.setInitialHumanCount(8_200_000_000L);
        scenario.setInitialCapitalPerCapita(15000.0);

        FutureScenarioRegistry.PhysicalScenarioPreset preset = FutureScenarioRegistry.findPresetForScenario(scenario);
        assertNotNull(preset);
        assertEquals("BAU_SSP585", preset.id());

        double initialTemp = mockCells.get(0).getTemperature();
        FutureScenarioRegistry.applyScenarioForcing(preset, mockCells);
        assertEquals(initialTemp + 4.5, mockCells.get(0).getTemperature(), 0.01,
                "BAU scenario must apply +4.5°C temperature forcing");
    }
}
