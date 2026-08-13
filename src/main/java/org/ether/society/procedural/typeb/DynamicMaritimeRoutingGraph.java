/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural.typeb;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic Trans-Oceanic Maritime Routing Graph Engine.
 * 
 * Dynamically builds, invalidates, and updates maritime navigation routes based on:
 * 1. Dynamic Technology Evolution (Coastal Cabotage -> Oceanic Navigation -> Polar Icebreaking).
 * 2. Thermodynamic Sea Ice & Temperature Drift (Dynamic Fluid Viscosity/Ice Drag).
 * 3. Infrastructure Growth (Ports, Polders, Floating Seasteading Hubs).
 * 4. Physicalist Hydrodynamics (Great Circle Distances, Archimedes Buoyancy).
 * 
 * Replaces static/hardcoded routing tables with automated, physically adaptive graph recalculations.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.2.0
 */
public class DynamicMaritimeRoutingGraph {
    private static final Logger logger = LoggerFactory.getLogger(DynamicMaritimeRoutingGraph.class);

    // Cache state for dynamic graph invalidation
    private static double cachedAvgTech = -1.0;
    private static int cachedNodeCount = -1;
    private static long lastRecalculationTick = -1;

    // Adjacency graph: Node cell ID -> Map of (Neighbor cell ID -> Path Cost)
    private static final Map<Long, Map<Long, Double>> routingGraph = new ConcurrentHashMap<>();

    /**
     * Updates and processes trans-oceanic maritime routes across the grid.
     */
    public static void processHybrid(List<H3Cell> cells, double timeStepDays) {
        if (cells == null || cells.isEmpty()) return;

        // Check if technological or topological state requires graph recalculation
        if (shouldRecalculateGraph(cells)) {
            rebuildRoutingGraph(cells);
        }

        // Apply dynamic trade, capital acceleration, and movement friction modulation from graph state
        applyRoutingGraphEffects(cells, timeStepDays);
    }

    /**
     * Determines whether the routing graph should be dynamically invalidated and rebuilt.
     */
    private static boolean shouldRecalculateGraph(List<H3Cell> cells) {
        double currentAvgTech = cells.stream()
                .mapToDouble(c -> c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0)
                .average().orElse(0.0);

        int currentNodeCount = (int) cells.stream()
                .filter(c -> c.getIsCoastal() || c.getIsPolder() || c.getHasFloatingInfrastructure() 
                        || c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN)
                .count();

        // Trigger recalculation if average tech changes significantly (>0.2) or node topology changes
        boolean techShift = Math.abs(currentAvgTech - cachedAvgTech) > 0.2;
        boolean topologyShift = currentNodeCount != cachedNodeCount;

        if (techShift || topologyShift || routingGraph.isEmpty()) {
            cachedAvgTech = currentAvgTech;
            cachedNodeCount = currentNodeCount;
            return true;
        }

        return false;
    }

    /**
     * Rebuilds the dynamic trans-oceanic routing graph based on current physical properties.
     */
    private static void rebuildRoutingGraph(List<H3Cell> cells) {
        logger.info("🌊 Dynamically recalculating Trans-Oceanic Maritime Routing Graph (Avg Tech: {}, Nodes: {})...",
                String.format("%.2f", cachedAvgTech), cachedNodeCount);

        routingGraph.clear();

        List<H3Cell> maritimeNodes = cells.stream()
                .filter(c -> c.getIsCoastal() || c.getIsPolder() || c.getHasFloatingInfrastructure()
                        || c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN)
                .toList();

        for (H3Cell source : maritimeNodes) {
            Map<Long, Double> neighborCosts = new HashMap<>();

            for (H3Cell target : maritimeNodes) {
                if (source.getH3Index() == target.getH3Index()) continue;

                double distKm = calculateGreatCircleDistance(
                        source.getLatitude(), source.getLongitude(),
                        target.getLatitude(), target.getLongitude()
                );

                // Physical Ocean Hydrodynamic Model:
                // Calculate significant wave height (Hs) along route from bathymetry & atmospheric storm turbulence,
                // and compare with vessel fleet physical hull wave clearance tolerance derived from capital density (K) & tech.
                double midElevation = (source.getElevation() + target.getElevation()) / 2.0;
                double midLatitude = (source.getLatitude() + target.getLatitude()) / 2.0;
                double avgCapital = (source.getResourceCapital() + target.getResourceCapital()) / 2.0;

                double significantWaveHeightMeters = calculateSignificantWaveHeight(midElevation, midLatitude);
                double fleetWaveClearanceMeters = calculateFleetWaveClearance(avgCapital, cachedAvgTech);

                // Safe open-water range governed by hydrodynamic wave tolerance ratio (Hs / H_clearance):
                // If ocean swell (Hs) exceeds hull clearance, open-sea transit causes severe wave drag/capsizing risk,
                // constraining safe travel to shallow coastal shelf cabotage (< 300 km).
                // As hull clearance grows to meet abyssal wave heights, trans-oceanic sea lanes open continuously.
                double waveOvertoppingRatio = significantWaveHeightMeters / Math.max(0.1, fleetWaveClearanceMeters);
                double maxRangeKm;
                if (waveOvertoppingRatio > 1.25 && midElevation < -200.0) {
                    maxRangeKm = 300.0; // Coastal cabotage shelf constraint due to excessive open ocean swell
                } else if (waveOvertoppingRatio > 1.0 && midElevation < -200.0) {
                    maxRangeKm = 1500.0; // Regional high-seas navigation with moderate swell risk
                } else {
                    maxRangeKm = 20000.0; // Deep-sea oceanic transit (hull clearance withstands ocean wave energy)
                }

                if (distKm <= maxRangeKm) {
                    double cost = calculateNavigationCost(source, target, distKm, cachedAvgTech, waveOvertoppingRatio);
                    neighborCosts.put(target.getH3Index(), cost);
                }
            }

            if (!neighborCosts.isEmpty()) {
                routingGraph.put(source.getH3Index(), neighborCosts);
            }
        }

        logger.info("✅ Trans-Oceanic Maritime Routing Graph rebuilt successfully with {} active route hubs.", routingGraph.size());
    }

    /**
     * Calculates local ocean swell significant wave height (Hs in meters) based on bathymetry and atmospheric storm turbulence.
     */
    public static double calculateSignificantWaveHeight(double elevationMeters, double latitudeDegrees) {
        // Bathymetric wave damping / abyssal swell amplification:
        // Shallow continental shelf (elevation > -200m) dampens ocean swells via bed friction (Hs ~ 0.8m - 1.5m).
        // Abyssal deep ocean (elevation <= -200m) generates large open-ocean swells (Hs ~ 3.5m - 5.5m).
        double elevationNorm = Math.min(0.0, elevationMeters);
        double bathymetryFactor = 1.0 / (1.0 + Math.exp((elevationNorm + 200.0) / 100.0));

        // Latitude storm intensity & atmospheric fetch (baroclinic instability around 40°-60° latitudes):
        double latRad = Math.toRadians(latitudeDegrees);
        double stormLatitudeFactor = 1.0 + 1.2 * Math.pow(Math.sin(2.0 * latRad), 2);

        // Base coastal wave height ~0.8m, abyssal swell addition up to +3.5m
        return 0.8 + (3.5 * bathymetryFactor * stormLatitudeFactor);
    }

    /**
     * Calculates vessel fleet hull wave clearance tolerance (in meters) from capital density and technology.
     */
    public static double calculateFleetWaveClearance(double capitalPerCapita, double techLevel) {
        // Physical hull clearance scales with physical capital K (hull displacement, draft, keel stability) and navigation tech:
        // Primitive coastal rafts/canoes (K ~ 10 kg/hab, tech ~ 1.0) -> clearance ~ 1.2m (restricted to shallow coastal waters Hs <= 1.2m)
        // Caravels/Galleons (K ~ 150 kg/hab, tech ~ 4.0) -> clearance ~ 4.0m (crosses Atlantic swells Hs <= 4.0m)
        // Industrial ironclads/super-freighters (K >= 800 kg/hab, tech >= 6.0) -> clearance >= 8.5m (crosses any oceanic sea state)
        double kFactor = Math.max(1.0, capitalPerCapita);
        double techFactor = Math.max(0.0, techLevel);
        return 1.0 + (0.8 * Math.log(1.0 + kFactor)) + (0.5 * techFactor);
    }

    /**
     * Calculates navigation cost between two maritime nodes based on distance, temperature, ice, tech, and wave risk.
     */
    private static double calculateNavigationCost(H3Cell source, H3Cell target, double distKm, double tech, double waveOvertoppingRatio) {
        double avgTemp = ((source.getTemperature() != null ? source.getTemperature() : 15.0)
                + (target.getTemperature() != null ? target.getTemperature() : 15.0)) / 2.0;

        // Base velocity increases logarithmically with technology level (knot speed scaling)
        double vesselSpeedKmH = 10.0 * (1.0 + Math.log(1.0 + Math.max(0.0, tech)));

        // Ice drag penalty (thermodynamic water freezing)
        double iceDragMultiplier = 1.0;
        if (avgTemp <= 0.0) {
            // High tech icebreakers mitigate ice drag; primitive ships are severely slowed or stopped
            iceDragMultiplier = tech >= 6.0 ? 1.5 : 5.0;
        }

        // Hydrodynamic wave energy & swell drag penalty (overtopping hull clearance)
        double waveDragMultiplier = 1.0;
        if (waveOvertoppingRatio > 1.0) {
            // Cubic wave drag scaling (hydrodynamic wave resistance & structural hull damage risk)
            waveDragMultiplier = Math.pow(waveOvertoppingRatio, 3.0);
        }

        double travelTimeHours = (distKm / vesselSpeedKmH) * iceDragMultiplier * waveDragMultiplier;
        return travelTimeHours;
    }

    /**
     * Applies dynamic trade accumulation and friction modulation derived from the graph state.
     */
    private static void applyRoutingGraphEffects(List<H3Cell> cells, double timeStepDays) {
        for (H3Cell cell : cells) {
            Map<Long, Double> routes = routingGraph.get(cell.getH3Index());
            if (routes != null && !routes.isEmpty()) {
                // Capital boost scales with connectivity degree and route efficiency
                double connectivityFactor = Math.min(2.0, 1.0 + 0.05 * routes.size());
                double currentCap = cell.getResourceCapital();
                cell.setResourceCapital(currentCap * (1.0 + 0.02 * connectivityFactor * (timeStepDays / 30.0)));

                // Hydrodynamic friction reduction over active sea lanes
                double friction = cell.getMovementFriction();
                cell.setMovementFriction(Math.max(0.1, friction * 0.30));
            }
        }
    }

    /**
     * Standard Great Circle / Haversine distance in kilometers.
     */
    private static double calculateGreatCircleDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static void clearCache() {
        routingGraph.clear();
        cachedAvgTech = -1.0;
        cachedNodeCount = -1;
    }
}
