/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.ProceduralEngineRegistry;
import org.ether.society.procedural.ProceduralPopulationEngine;
import org.ether.society.procedural.typeb.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * High-Fidelity Earth Scenario Performance Benchmark.
 * Simulates 50 Million humans across a multi-cell H3 Earth grid over 10 full simulation cycles
 * with active Type B physicalist & cliodynamic plugins.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class EarthScenarioPerformanceBenchmarkTest {
    private static final Logger logger = LoggerFactory.getLogger(EarthScenarioPerformanceBenchmarkTest.class);

    @Test
    public void runEarthScenarioBenchmark() {
        logger.info("===============================================================================");
        logger.info("🌍 STARTING EARTH HIGH-FIDELITY PERFORMANCE BENCHMARK");
        logger.info("===============================================================================");

        // 1. Initialize 500 H3 Earth Grid Cells
        List<H3Cell> earthGrid = new ArrayList<>();
        int totalCells = 500;
        for (int i = 0; i < totalCells; i++) {
            double lat = -60.0 + (i % 50) * 2.4;
            double lon = -180.0 + (i / 50) * 36.0;
            H3Cell cell = new H3Cell(0x8828308281fffffL + i, lat, lon);
            cell.setTechnologyLevel(2.5);
            cell.setFoodResource(50000.0);
            cell.setResourceCapital(10000.0);
            cell.setResourceWork(2500.0);
            cell.setResourceMetal(1000.0);
            cell.setAccessibleAquifer(500.0);
            cell.setPollutionLevel(5.0);
            cell.setTemperature(15.0);
            cell.setLifespan(65.0);
            cell.setPopulation(100_000); // 500 cells * 100,000 = 50,000,000 humans
            earthGrid.add(cell);
        }

        long initialTotalPop = earthGrid.stream().mapToLong(H3Cell::getPopulation).sum();
        logger.info("Initialized Earth Map with {} H3 Cells & {} Total Population.", earthGrid.size(), initialTotalPop);

        // 3. Register Cumulative Type B Plugins
        ProceduralEngineRegistry.clearPlugins();
        ProceduralEngineRegistry.registerPlugin("B1_World3Hybrid", World3HybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("B7_HandyNasaHybrid", HandyNasaHybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("B8_NordhausDiceHybrid", NordhausDiceHybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("B16_PinkerDecline", PinkerViolenceDeclinePureEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B18_AiRegulation", AiAutonomousRegulationPureEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B21_SpatialFractal", SpatialCityFractalEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B27_MediterraneanHighway", MediterraneanSeaHighwayEngine::processHybrid);

        // Memory usage before benchmark
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memBefore = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        // 4. Run 10 Simulation Cycles (Years)
        int cycles = 10;
        long startTime = System.currentTimeMillis();

        for (int cycle = 1; cycle <= cycles; cycle++) {
            long cycleStart = System.currentTimeMillis();

            // Monthly Sub-step Climate & Physics loop
            for (int month = 1; month <= 12; month++) {
                // Monthly Procedural Extensions
                org.ether.society.procedural.RenewableEnergyPhysicsEngine.processRenewableEnergyPhysics(earthGrid);
                org.ether.society.procedural.WetBulbTemperatureEngine.processWetBulbHyperthermia(earthGrid);
                org.ether.society.procedural.BiologicalDemographicsEngine.processBiologicalDemographics(earthGrid);
            }

            // Annual Macro-Cliodynamic & Thermodynamic Execution
            org.ether.society.procedural.OreGradeThermodynamicsEngine.processOreDepletion(earthGrid, 1.0);
            org.ether.society.procedural.InfrastructureInertiaEngine.processInfrastructureInertia(earthGrid, 1.0);
            org.ether.society.procedural.EntropicMetalDissipationEngine.processEntropicDissipation(earthGrid, 1.0);
            org.ether.society.procedural.JevonsParadoxEngine.processJevonsRebound(earthGrid, 1.0);
            org.ether.society.procedural.World3CouplingEngine.processWorld3System(earthGrid, 1.0);
            org.ether.society.procedural.KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(earthGrid, 1.0);
            ProceduralEngineRegistry.processPlugins(earthGrid, 1.0);

            long cycleDuration = System.currentTimeMillis() - cycleStart;
            logger.info("Cycle {}/{} completed in {} ms.", cycle, cycles, cycleDuration);
        }

        long totalDurationMs = System.currentTimeMillis() - startTime;
        long memAfter = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        // 5. Gather Final Telemetry Statistics
        long finalPop = earthGrid.stream().mapToLong(H3Cell::getPopulation).sum();
        double finalCapital = earthGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).sum();
        double avgLifespan = earthGrid.stream().mapToDouble(c -> c.getLifespan() != null ? c.getLifespan() : 0.0).average().orElse(0.0);

        logger.info("===============================================================================");
        logger.info("📊 BENCHMARK PERFORMANCE RESULTS");
        logger.info("===============================================================================");
        logger.info("Total Simulated Cycles       : {} Years", cycles);
        logger.info("Grid Resolution              : {} H3 Cells", totalCells);
        logger.info("Simulated Population         : {} Humans", finalPop);
        logger.info("Total Execution Time         : {} ms", totalDurationMs);
        logger.info("Average Tick Duration        : {} ms/year", String.format("%.2f", (double) totalDurationMs / cycles));
        logger.info("Throughput                   : {} cell-updates/sec", String.format("%.2f", (double)(totalCells * cycles * 12) / (totalDurationMs / 1000.0)));
        logger.info("RAM Consumption              : {} MB -> {} MB (Delta: +{} MB)", memBefore, memAfter, (memAfter - memBefore));
        logger.info("Final Global Capital         : {}", String.format("%.2f", finalCapital));
        logger.info("Average Life Expectancy      : {} years", String.format("%.1f", avgLifespan));
        logger.info("===============================================================================");

        assertTrue(totalDurationMs < 10000, "10 Earth simulation cycles should execute in under 10 seconds.");
    }
}
