/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.scenarios;

import org.ether.society.core.PreComputePhase;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.BiologicalDemographicsEngine;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import org.ether.society.model.Scenario;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Spatial dispersal, Haversine geodesic distance, and planetary scenario initialization support
 * for {@link HistoricalPlanetaryScenarioValidationSuite}.
 *
 * @author Silvere Martin-Michiellot
 * @see HistoricalPlanetaryScenarioValidationSuite
 */
final class HistoricalPlanetaryScenarioSupport {

    /** Canonical Earth Resolution-2 planetary preset used across macro-historical scenario benchmarks. */
    static final PlanetPreset PLANETARY_EARTH = new PlanetPreset(
            "Terre Planetary Res2", 2, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, 12345L,
            1.0, 1.0, 0.35, 40.0, 21.0, 0.30, 1.0,
            false, 1.0, 0.0, 420.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none", false, "", 12445L, false, "", 13345L, false, "", 14345L);

    /** Mean spherical Earth radius \(R_{\oplus} = 6371\text{ km}\) for Haversine great-circle metrics. */
    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * Summary telemetry record produced by multi-tick continental and maritime dispersal runs.
     *
     * @param populatedCellCount total number of H3 cells with positive human population
     * @param maxDispersalKm     maximum great-circle Haversine distance (km) reached from origin
     * @param targetSettled      whether the target continental bounding box was colonized
     */
    record DispersalSummary(long populatedCellCount, double maxDispersalKm, boolean targetSettled) {
    }

    /**
     * Private constructor preventing instantiation of stateless support class.
     */
    private HistoricalPlanetaryScenarioSupport() {
    }

    /**
     * Generates the Resolution-2 Earth H3 mesh and executes {@link PreComputePhase} initialization
     * for the specified historical scenario parameters.
     *
     * @param generator     procedural planetary mesh generator
     * @param name          scenario display name
     * @param startYear     historical start year (negative for BCE/BP)
     * @param initialHumans initial global human population count
     * @param densityType   population density distribution archetype key
     * @param capitalPerCap optional initial capital per capita (ignored if {@code <= 0})
     * @param energyPerCap  optional initial energy per capita (ignored if {@code <= 0})
     * @return initialized list of planetary {@link H3Cell} instances
     */
    static List<H3Cell> buildInitializedPlanetaryGrid(
            ProceduralGenerator generator,
            String name,
            long startYear,
            long initialHumans,
            String densityType,
            double capitalPerCap,
            double energyPerCap) {
        List<H3Cell> planetaryGrid = generator.generatePlanet(PLANETARY_EARTH);
        Scenario scenario = new Scenario();
        scenario.setName(name);
        scenario.setStartDateYear(startYear);
        scenario.setInitialHumanCount(initialHumans);
        if (capitalPerCap > 0.0) {
            scenario.setInitialCapitalPerCapita(capitalPerCap);
        }
        if (energyPerCap > 0.0) {
            scenario.setInitialEnergyPerCapita(energyPerCap);
        }
        scenario.setPopulationDensityType(densityType);
        new PreComputePhase(scenario).execute(planetaryGrid);
        return planetaryGrid;
    }

    /**
     * Runs a multi-tick demographic growth and neighbor dispersal simulation and evaluates
     * maximum geodesic wave-front expansion from an origin cell toward a target geographic box.
     *
     * @param planetaryGrid  initialized planetary H3 mesh
     * @param h3Service      H3 topological neighbor lookup service
     * @param origin         demographic hearth origin cell
     * @param ticks          number of dispersal ticks to step
     * @param allowSeafaring whether watercraft crossing into oceanic cells is enabled
     * @param minLat         minimum latitude (degrees) of target continent bounding box
     * @param maxLat         maximum latitude (degrees) of target continent bounding box
     * @param minLon         minimum longitude (degrees) of target continent bounding box
     * @param maxLon         maximum longitude (degrees) of target continent bounding box
     * @return {@link DispersalSummary} containing populated cell count, max distance, and target settlement flag
     */
    static DispersalSummary runDispersalSimulation(
            List<H3Cell> planetaryGrid,
            H3Service h3Service,
            H3Cell origin,
            int ticks,
            boolean allowSeafaring,
            double minLat,
            double maxLat,
            double minLon,
            double maxLon) {
        Map<Long, H3Cell> cellMap = new HashMap<>(planetaryGrid.size() * 2);
        planetaryGrid.forEach(c -> cellMap.put(c.getH3Index(), c));

        boolean targetSettled = false;
        double maxDispersalKm = 0.0;

        BiologicalDemographicsEngine bioEngine = new BiologicalDemographicsEngine();
        for (int tick = 1; tick <= ticks; tick++) {
            org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(planetaryGrid.size());
            org.ether.society.data.DODDataGenerator.populateWorldBuffer(planetaryGrid, buf, false);
            bioEngine.tick(buf, 1.0, 12.0);
            for (int i = 0; i < planetaryGrid.size(); i++) {
                planetaryGrid.get(i).setPopulation((int) Math.round(buf.getBiomassHuman()[i] / 0.06));
            }
            processMaritimeAndTerrestrialDispersal(planetaryGrid, cellMap, h3Service, allowSeafaring);

            for (H3Cell c : planetaryGrid) {
                if (c.getPopulation() != null && c.getPopulation() > 0) {
                    double dist = calculateHaversineDistanceKm(
                            origin.getLatitude(), origin.getLongitude(),
                            c.getLatitude(), c.getLongitude());
                    if (dist > maxDispersalKm) {
                        maxDispersalKm = dist;
                    }
                    if (c.getLatitude() >= minLat && c.getLatitude() <= maxLat
                            && c.getLongitude() >= minLon && c.getLongitude() <= maxLon) {
                        targetSettled = true;
                    }
                }
            }
        }

        long populatedCellCount = planetaryGrid.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 0)
                .count();
        return new DispersalSummary(populatedCellCount, maxDispersalKm, targetSettled);
    }

    /**
     * Propagates band fission and migration flux (12% outbound cohort) from cells exceeding
     * 50 inhabitants across terrestrial neighbors and optional maritime crossings.
     *
     * @param cells          planetary H3 cell collection
     * @param lookup         index-to-cell hash map for \(O(1)\) neighbor resolution
     * @param service        H3 topology service
     * @param allowSeafaring {@code true} if coastal watercraft navigation across ocean cells is enabled
     */
    static void processMaritimeAndTerrestrialDispersal(
            List<H3Cell> cells, Map<Long, H3Cell> lookup, H3Service service, boolean allowSeafaring) {
        List<H3Cell> sourceCells = cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() > 50)
                .toList();

        Map<H3Cell, Integer> outboundMap = new HashMap<>();

        for (H3Cell src : sourceCells) {
            int pop = src.getPopulation();
            List<Long> neighborIdxs = service.getNeighbors(src.getH3Index());

            List<H3Cell> validTargets = neighborIdxs.stream()
                    .map(lookup::get)
                    .filter(Objects::nonNull)
                    .filter(n -> {
                        if (n.getBiome() != Biome.OCEAN && n.getBiome() != Biome.DEEP_OCEAN) {
                            return true;
                        }
                        return allowSeafaring;
                    })
                    .toList();

            if (!validTargets.isEmpty()) {
                int migrants = (int) (pop * 0.12);
                int perNeighbor = migrants / validTargets.size();

                if (perNeighbor > 0) {
                    outboundMap.put(src, migrants);
                    for (H3Cell target : validTargets) {
                        int targetPop = target.getPopulation() != null ? target.getPopulation() : 0;
                        target.setPopulation(targetPop + perNeighbor);
                        if (target.getFoodResource() == null || target.getFoodResource() < 300.0) {
                            target.setFoodResource(500.0);
                        }
                    }
                }
            }
        }

        outboundMap.forEach((src, m) -> src.setPopulation(Math.max(0, src.getPopulation() - m)));
    }

    /**
     * Computes great-circle geodesic distance in kilometers between two geographic coordinates
     * using the spherical Haversine formula:
     * \[d = 2 R_{\oplus} \operatorname{atan2}\left(\sqrt{a}, \sqrt{1 - a}\right)\]
     *
     * @param lat1 origin latitude (degrees)
     * @param lon1 origin longitude (degrees)
     * @param lat2 destination latitude (degrees)
     * @param lon2 destination longitude (degrees)
     * @return geodesic distance \(d\) in kilometers
     */
    static double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
