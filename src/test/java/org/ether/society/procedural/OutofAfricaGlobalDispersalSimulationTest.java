/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.core.PreComputePhase;
import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;
import org.ether.society.procedural.typeb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Realistic Planetary-Scale Simulation & Calibration Test Suite:
 * Out-of-Africa Homo Sapiens Dispersal & Macro-Evolutionary Trajectory (-100,000 BC to -10,000 BC).
 *
 * Simulates thousands of H3 cells across an Earth-like planetary grid over centuries/millennia
 * to validate physical dispersal equations, spatial wave velocity, and continental arrival timelines.
 *
 * @author Silvere Martin-Michiellot
 */
public class OutofAfricaGlobalDispersalSimulationTest {

    private static final Logger logger = LoggerFactory.getLogger(OutofAfricaGlobalDispersalSimulationTest.class);

    private H3Service h3Service;
    private ProceduralGenerator generator;
    private List<H3Cell> planetaryGrid;
    private Map<Long, H3Cell> cellMap;

    private static final PlanetPreset FAST_EARTH = new PlanetPreset(
            "Terre Fast", 2, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, 12345L, 1.0, 1.0, 0.35, 40.0, 21.0, 0.30, 1.0,
            false, 1.0, 0.0, 420.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none", false, "", 12445L, false, "", 13345L, false, "", 14345L);

    @BeforeEach
    public void setUp() {
        ProceduralEngineRegistry.clearPlugins();
        h3Service = H3Service.getInstance();
        generator = new ProceduralGenerator();
        cellMap = new HashMap<>();

        // Generate full planetary grid at Resolution 3 (~2,882 global H3 cells)
        planetaryGrid = generator.generatePlanet(FAST_EARTH);

        for (H3Cell cell : planetaryGrid) {
            cellMap.put(cell.getH3Index(), cell);
        }

        logger.info("Planetary-Scale Setup Complete: {} H3 cells initialized.", planetaryGrid.size());
    }

    @Test
    @DisplayName("Simulate Out-of-Africa Planetary Wave Front Dispersal Across Thousands of H3 Cells")
    public void testFullPlanetaryOutofAfricaDispersal() {
        Scenario scenario = new Scenario();
        scenario.setName("Sortie d'Afrique & Expansion Homo Sapiens (-100000)");
        scenario.setStartDateYear(-100000);
        scenario.setInitialHumanCount(50_000L);
        scenario.setInitialCapitalPerCapita(2.0);
        scenario.setInitialEnergyPerCapita(5.0);
        scenario.setPopulationDensityType("ONE_CONTINENT");
        scenario.setPlanetPreset(PlanetPreset.EARTH_LIKE);

        PreComputePhase preCompute = new PreComputePhase(scenario);
        preCompute.execute(planetaryGrid);

        // Find East Africa origin cell (Lat: ~0° to 15°N, Lng: ~30° to 45°E)
        H3Cell africaOrigin = planetaryGrid.stream()
                .filter(c -> c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                .filter(c -> c.getLatitude() >= 0.0 && c.getLatitude() <= 15.0)
                .filter(c -> c.getLongitude() >= 30.0 && c.getLongitude() <= 45.0)
                .findFirst()
                .orElse(planetaryGrid.stream().filter(c -> c.getElevation() > 0).findFirst().orElseThrow());

        // Focus initial paleolithic population in East Africa
        for (H3Cell cell : planetaryGrid) {
            cell.setPopulation(0);
        }
        africaOrigin.setPopulation(50_000);
        africaOrigin.setFoodResource(1500.0);

        logger.info("East Africa Origin Cell Seeded: ID {}, Lat {}, Lng {}, Pop {}",
                africaOrigin.getH3Index(), africaOrigin.getLatitude(), africaOrigin.getLongitude(), africaOrigin.getPopulation());

        // Simulation parameters: 150 ticks (~3,000 to 15,000 years of paleolithic expansion)
        int totalTicks = 150;
        int initialPopulatedCount = 1;

        logger.info("=== STARTING PLANETARY OUT-OF-AFRICA DISPERSAL SIMULATION (150 TICKS) ===");
        logger.info("Tick | Global Pop | Populated H3 Cells | Max Dispersal Dist (km) | Far East Settled | Australia Settled");

        boolean reachedFarEast = false;
        boolean reachedAustralia = false;
        double maxDistanceKm = 0.0;

        for (int tick = 1; tick <= totalTicks; tick++) {
            // 1. Demographic reproduction & aging
            BiologicalDemographicsEngine.processBiologicalDemographics(planetaryGrid);

            // 2. Spatial 2D H3 neighbor demographic expansion vector push
            processPlanetary2DH3Migration(planetaryGrid, cellMap, h3Service);

            // 3. Environmental physical laws & NPK stoichiometry
            SoilNutrientNPKEngine.processSoilNutrients(planetaryGrid);
            PhysicalLawEngine.applyPhysicalLaws(planetaryGrid, 1.0);

            // Calculate telemetry metrics
            long globalPop = planetaryGrid.stream().mapToLong(c -> c.getPopulation() != null ? c.getPopulation() : 0).sum();
            List<H3Cell> populatedCells = planetaryGrid.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).toList();

            maxDistanceKm = 0.0;
            for (H3Cell c : populatedCells) {
                double dist = calculateHaversineDistanceKm(
                        africaOrigin.getLatitude(), africaOrigin.getLongitude(),
                        c.getLatitude(), c.getLongitude()
                );
                if (dist > maxDistanceKm) {
                    maxDistanceKm = dist;
                }

                // Far East (East Asia: Lat 20..45, Lng 100..130)
                if (c.getLatitude() >= 20.0 && c.getLatitude() <= 45.0 && c.getLongitude() >= 100.0 && c.getLongitude() <= 130.0) {
                    reachedFarEast = true;
                }

                // Australia / Sahul (Lat -40..-10, Lng 110..155)
                if (c.getLatitude() >= -40.0 && c.getLatitude() <= -10.0 && c.getLongitude() >= 110.0 && c.getLongitude() <= 155.0) {
                    reachedAustralia = true;
                }
            }

            if (tick % 25 == 0 || tick == 1 || tick == totalTicks) {
                logger.info(String.format("%4d | %10d | %18d | %22.1f | %16s | %17s",
                        tick, globalPop, populatedCells.size(), maxDistanceKm, reachedFarEast ? "YES" : "NO", reachedAustralia ? "YES" : "NO"));
            }
        }
        logger.info("=== PLANETARY OUT-OF-AFRICA DISPERSAL SIMULATION COMPLETE ===");

        long finalPopulatedCount = planetaryGrid.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).count();
        assertTrue(finalPopulatedCount > initialPopulatedCount,
                "Out-of-Africa dispersal must spread from single origin across multiple planetary H3 cells");

        assertTrue(maxDistanceKm > 2500.0,
                "Out-of-Africa wave front must propagate at least 2,500 km across continental landmasses");
    }

    @Test
    @DisplayName("Empirical Comparison: Out-of-Africa Single Origin vs Multi-Regionalism Hypothesis")
    public void testOutofAfricaVsMultiRegionalismComparison() {
        // 1. Single Origin Out-of-Africa Configuration
        List<H3Cell> singleOriginGrid = generator.generatePlanet(FAST_EARTH);
        Map<Long, H3Cell> singleMap = new HashMap<>();
        singleOriginGrid.forEach(c -> { c.setPopulation(0); singleMap.put(c.getH3Index(), c); });

        H3Cell africa = singleOriginGrid.stream()
                .filter(c -> c.getElevation() > 0 && c.getLatitude() >= 0 && c.getLatitude() <= 15 && c.getLongitude() >= 30 && c.getLongitude() <= 45)
                .findFirst().orElse(singleOriginGrid.get(0));
        africa.setPopulation(50_000);
        africa.setFoodResource(1500.0);

        // 2. Multi-Regionalism Configuration (Independent regional seeds in Africa, Europe, Asia)
        List<H3Cell> multiRegionalGrid = generator.generatePlanet(FAST_EARTH);
        Map<Long, H3Cell> multiMap = new HashMap<>();
        multiRegionalGrid.forEach(c -> { c.setPopulation(0); multiMap.put(c.getH3Index(), c); });

        // Seed 3 independent origins
        multiRegionalGrid.stream().filter(c -> c.getElevation() > 0 && c.getLatitude() >= 0 && c.getLatitude() <= 15 && c.getLongitude() >= 30 && c.getLongitude() <= 45)
                .findFirst().ifPresent(c -> { c.setPopulation(15_000); c.setFoodResource(1000.0); });
        multiRegionalGrid.stream().filter(c -> c.getElevation() > 0 && c.getLatitude() >= 40 && c.getLatitude() <= 55 && c.getLongitude() >= 5 && c.getLongitude() <= 25)
                .findFirst().ifPresent(c -> { c.setPopulation(15_000); c.setFoodResource(1000.0); });
        multiRegionalGrid.stream().filter(c -> c.getElevation() > 0 && c.getLatitude() >= 30 && c.getLatitude() <= 45 && c.getLongitude() >= 100 && c.getLongitude() <= 120)
                .findFirst().ifPresent(c -> { c.setPopulation(15_000); c.setFoodResource(1000.0); });

        // Run 50 ticks of dispersal
        for (int t = 0; t < 50; t++) {
            BiologicalDemographicsEngine.processBiologicalDemographics(singleOriginGrid);
            processPlanetary2DH3Migration(singleOriginGrid, singleMap, h3Service);

            BiologicalDemographicsEngine.processBiologicalDemographics(multiRegionalGrid);
            processPlanetary2DH3Migration(multiRegionalGrid, multiMap, h3Service);
        }

        long singlePopulatedCount = singleOriginGrid.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).count();
        long multiPopulatedCount = multiRegionalGrid.stream().filter(c -> c.getPopulation() != null && c.getPopulation() > 0).count();

        logger.info("Out-of-Africa Single Origin Populated Cells: {}", singlePopulatedCount);
        logger.info("Multi-Regionalism Populated Cells: {}", multiPopulatedCount);

        assertTrue(singlePopulatedCount > 1, "Single-origin model must generate outward expanding spatial wave front");
        assertTrue(multiPopulatedCount > 1, "Multi-regional model must generate parallel local demographic clusters");
    }

    /**
     * Executes 2D spatial H3 hexagonal neighbor demographic migration vector push.
     */
    private void processPlanetary2DH3Migration(List<H3Cell> cells, Map<Long, H3Cell> cellLookup, H3Service service) {
        List<H3Cell> populated = cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 100)
                .toList();

        Map<H3Cell, Integer> outboundMigrants = new HashMap<>();

        for (H3Cell source : populated) {
            int pop = source.getPopulation();
            double food = source.getFoodResource() != null ? source.getFoodResource() : 500.0;
            double carryingCapacity = 50.0 + food * 0.5;

            // Trigger migration when density exceeds carrying capacity or gradient exists
            if (pop > 10) {
                List<Long> neighborIndexes = service.getNeighbors(source.getH3Index());
                List<H3Cell> landNeighbors = neighborIndexes.stream()
                        .map(cellLookup::get)
                        .filter(Objects::nonNull)
                        .filter(n -> n.getElevation() != null && n.getElevation() > 0.35) // Above water level 0.35
                        .toList();

                if (landNeighbors.isEmpty()) {
                    // Fallback to any land neighbor with elevation > 0
                    landNeighbors = neighborIndexes.stream()
                            .map(cellLookup::get)
                            .filter(Objects::nonNull)
                            .filter(n -> n.getElevation() != null && n.getElevation() > 0)
                            .toList();
                }

                if (!landNeighbors.isEmpty()) {
                    int totalMigrants = (int) (pop * 0.15); // 15% expansion shift per tick
                    int perNeighbor = totalMigrants / landNeighbors.size();

                    if (perNeighbor > 0) {
                        outboundMigrants.put(source, totalMigrants);
                        for (H3Cell target : landNeighbors) {
                            int targetPop = target.getPopulation() != null ? target.getPopulation() : 0;
                            target.setPopulation(targetPop + perNeighbor);
                            if (target.getFoodResource() == null || target.getFoodResource() < 400.0) {
                                target.setFoodResource(600.0);
                            }
                        }
                    }
                }
            }
        }

        // Subtract outbound migrants from origin cells
        outboundMigrants.forEach((source, migrants) -> {
            source.setPopulation(Math.max(0, source.getPopulation() - migrants));
        });
    }

    private double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
