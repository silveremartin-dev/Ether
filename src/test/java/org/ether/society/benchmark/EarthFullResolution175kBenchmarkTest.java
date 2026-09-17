/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.benchmark;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.ether.society.procedural.ProceduralPopulationEngine;
import org.ether.society.procedural.ProceduralEngineRegistry;
import org.ether.society.procedural.typeb.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Benchmark of Full Earth Simulation at 175,000 H3 Cells & 50 Million Humans.
 * Evaluates execution ticks/second (pas par seconde) and cell-update throughput
 * under full physical, thermodynamic, and cliodynamic simulation models over max 10 seconds.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EarthFullResolution175kBenchmarkTest {
    private static final Logger logger = LoggerFactory.getLogger(EarthFullResolution175kBenchmarkTest.class);

    private static final int TARGET_CELL_COUNT = 175_000;
    private static final long TARGET_POPULATION = 50_000_000L;
    private static final long MAX_BENCHMARK_TIME_MS = 10_000L;

    @Test
    public void runFullEarth175kCellBenchmark() {
        logger.info("===============================================================================");
        logger.info("🌍 STARTING FULL EARTH 175,000 H3 CELL & 50M HUMAN BENCHMARK");
        logger.info("===============================================================================");

        // 1. Generate 175,000 H3 Earth Grid Cells with Geographic, Climatic, and Seismic Data
        List<H3Cell> earthGrid = new ArrayList<>(TARGET_CELL_COUNT);
        ProceduralGenerator generator = new ProceduralGenerator();
        PlanetPreset earthPreset = PlanetPreset.EARTH_LIKE;

        long genStart = System.currentTimeMillis();
        // Generate uniform lat/lon grid over 175,000 cells
        int rows = 350;
        int cols = 500;
        long h3Base = 0x8828308281fffffL;

        for (int r = 0; r < rows; r++) {
            double lat = -85.0 + (r * 170.0 / rows);
            for (int c = 0; c < cols; c++) {
                double lon = -180.0 + (c * 360.0 / cols);
                long cellId = h3Base + (r * cols + c);

                H3Cell cell = new H3Cell(cellId, lat, lon);
                ProceduralGenerator.PlanetPoint point = generator.getPlanetPoint(lat, lon, earthPreset);

                cell.setElevation(point.elevation());
                cell.setTemperature(point.temperature());
                cell.setRainfall(point.rainfall());
                cell.setBiome(point.biome());

                if (point.biome() != Biome.OCEAN && point.biome() != Biome.DEEP_OCEAN) {
                    cell.setFoodResource(5000.0);
                    cell.setResourceCapital(1000.0);
                    cell.setResourceWork(200.0);
                    cell.setResourceMetal(300.0);
                    cell.setAccessibleAquifer(100.0);
                }
                cell.setTechnologyLevel(2.5); // Antiquity era (-500 BCE)
                cell.setLifespan(55.0);
                cell.setPollutionLevel(2.0);

                earthGrid.add(cell);
            }
        }
        long genDuration = System.currentTimeMillis() - genStart;
        logger.info("Generated {} H3 Earth Grid Cells in {} ms.", earthGrid.size(), genDuration);

        // 2. Distribute 50,000,000 Humans across Antiquity Earth (-500 BCE)
        ProceduralPopulationEngine.distributePopulation(earthGrid, null, TARGET_POPULATION, 2.5, "EARTH", true, -500);
        long initialTotalPop = earthGrid.stream().mapToLong(H3Cell::getPopulation).sum();
        logger.info("Populated Antiquity Earth (-500 BCE) with {} Humans.", initialTotalPop);

        // 3. Register All Type B Cliodynamic & Physical Plugins
        ProceduralEngineRegistry.clearPlugins();
        ProceduralEngineRegistry.registerPlugin("B1_World3Hybrid", World3HybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("B7_HandyNasaHybrid", HandyNasaHybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("B8_NordhausDiceHybrid", NordhausDiceHybridEngine::processPlugin);
        ProceduralEngineRegistry.registerPlugin("B16_PinkerDecline", PinkerViolenceDeclinePureEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B18_AiRegulation", AiAutonomousRegulationPureEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B21_SpatialFractal", SpatialCityFractalEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B24_MilitaryTechShock", MilitaryTechShockEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B27_MaritimeHighway", MaritimeHighwayEngine::processHybrid);
        ProceduralEngineRegistry.registerPlugin("B29_FrontierAsabiyyah", FrontierAsabiyyahEngine::processHybrid);

        // Clean RAM before measuring
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memBeforeMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        // 4. Run Full Simulation Loop for up to 10 Seconds
        logger.info("🚀 Executing Full Simulation Engines Loop (Max 10 seconds)...");
        long benchStartTime = System.currentTimeMillis();
        int completedTicks = 0;

        while ((System.currentTimeMillis() - benchStartTime) < MAX_BENCHMARK_TIME_MS) {
            long tickStart = System.currentTimeMillis();

            // Monthly Physics / Radiance Sub-loop (12 months per simulated year)
            for (int month = 1; month <= 12; month++) {
                org.ether.society.procedural.RenewableEnergyPhysicsEngine.processRenewableEnergyPhysics(earthGrid);
                org.ether.society.procedural.WetBulbTemperatureEngine.processWetBulbHyperthermia(earthGrid);
                org.ether.society.procedural.BiologicalDemographicsEngine.processBiologicalDemographics(earthGrid);
            }

            // Annual Thermodynamic & Cliodynamic Engine Step
            org.ether.society.procedural.OreGradeThermodynamicsEngine.processOreDepletion(earthGrid, 1.0);
            org.ether.society.procedural.InfrastructureInertiaEngine.processInfrastructureInertia(earthGrid, 1.0);
            org.ether.society.procedural.EntropicMetalDissipationEngine.processEntropicDissipation(earthGrid, 1.0);
            org.ether.society.procedural.JevonsParadoxEngine.processJevonsRebound(earthGrid, 1.0);
            org.ether.society.procedural.World3CouplingEngine.processWorld3System(earthGrid, 1.0);
            org.ether.society.procedural.KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(earthGrid, 1.0);
            ProceduralEngineRegistry.processPlugins(earthGrid, 1.0);

            completedTicks++;
            long tickDuration = System.currentTimeMillis() - tickStart;
            logger.info("Simulated Year Tick {} completed in {} ms.", completedTicks, tickDuration);
        }

        long totalElapsedMs = System.currentTimeMillis() - benchStartTime;
        long memAfterMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        // 5. Calculate Final Benchmark Key Metrics
        double ticksPerSecond = (double) completedTicks / (totalElapsedMs / 1000.0);
        double msPerTick = (double) totalElapsedMs / completedTicks;
        double totalCellUpdates = (double) completedTicks * earthGrid.size() * 12;
        double cellUpdatesPerSec = totalCellUpdates / (totalElapsedMs / 1000.0);

        long finalPop = earthGrid.stream().mapToLong(H3Cell::getPopulation).sum();
        double finalCapital = earthGrid.stream().mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).sum();

        logger.info("===============================================================================");
        logger.info("📈 FULL EARTH 175k BENCHMARK FINAL RESULTS");
        logger.info("===============================================================================");
        logger.info("Grid Resolution              : {} H3 Cells", earthGrid.size());
        logger.info("Initial / Final Population   : {} / {} Humans", initialTotalPop, finalPop);
        logger.info("Total Benchmark Duration     : {} ms (Limit: {} ms)", totalElapsedMs, MAX_BENCHMARK_TIME_MS);
        logger.info("Completed Ticks (Years)      : {} Ticks", completedTicks);
        logger.info("Simulation Speed (TPS)       : {} Ticks/sec (Pas par seconde)", String.format("%.2f", ticksPerSecond));
        logger.info("Average Duration per Tick    : {} ms/tick", String.format("%.2f", msPerTick));
        logger.info("Throughput                   : {} cell-updates/sec", String.format("%.2f", cellUpdatesPerSec));
        logger.info("RAM Usage (Heap)             : {} MB -> {} MB (Delta: +{} MB)", memBeforeMB, memAfterMB, (memAfterMB - memBeforeMB));
        logger.info("Final Global Capital         : {}", String.format("%.2f", finalCapital));
        logger.info("===============================================================================");

        assertTrue(completedTicks > 0, "Should complete at least 1 tick within 10 seconds.");
    }
}

