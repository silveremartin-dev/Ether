/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.model.Biome;
import org.ether.society.model.PhysicalUnitsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic Trans-Oceanic Maritime Routing Graph Engine (Data-Oriented {@link WorldBuffer}).
 *
 * <p>Dynamically builds, invalidates, and updates maritime navigation routes over contiguous
 * {@link WorldBuffer} arrays based on:</p>
 * <ol>
 *   <li>Physical Capital &amp; Propulsion Capability (Coastal Cabotage -&gt; Oceanic Navigation -&gt; Icebreaking).</li>
 *   <li>Thermodynamic Sea Ice &amp; Temperature Drift (Dynamic Fluid Viscosity / Ice Drag).</li>
 *   <li>Physicalist Hydrodynamics (Great Circle Spherical Haversine Distances).</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-academic
 */
public class DynamicMaritimeRoutingGraph extends AbstractSimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(DynamicMaritimeRoutingGraph.class);

    /** Constructs the DynamicMaritimeRoutingGraph Tier 2 engine module. */
    public DynamicMaritimeRoutingGraph() {
        super("DynamicMaritimeRoutingGraph", "DynamicMaritimeRoutingGraph", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        processMaritimeBuffer(buffer, PhysicalUnitsHelper.yearsToDays(dtYears));
    }

    @Override
    public void reset() {
        clearCache();
    }

    /** Cached mean capital per cell for graph invalidation checks. */
    private static double cachedAvgCapital = -1.0;
    /** Cached maritime node count for graph invalidation checks. */
    private static int cachedNodeCount = -1;

    /** Adjacency graph: Node H3 cell ID -> Map of (Neighbor H3 cell ID -> Path Cost in hours). */
    private static final Map<Long, Map<Long, Double>> routingGraph = new ConcurrentHashMap<>();

    /**
     * Updates and processes trans-oceanic maritime routes directly over a {@link WorldBuffer}.
     *
     * @param buffer       contiguous world state buffer
     * @param timeStepDays simulation time step in days
     */
    public static void processMaritimeBuffer(WorldBuffer buffer, double timeStepDays) {
        if (buffer == null || buffer.getCapacity() == 0 || timeStepDays <= 0.0) return;

        if (shouldRecalculateGraph(buffer)) {
            rebuildRoutingGraph(buffer);
        }

        applyRoutingGraphEffects(buffer, timeStepDays);
    }

    /**
     * Determines whether the routing graph should be dynamically invalidated and rebuilt.
     */
    private static boolean shouldRecalculateGraph(WorldBuffer buffer) {
        int n = buffer.getCapacity();
        float[] capArr = buffer.getResourceCapital();
        byte[] biomeArr = buffer.getBiomes();
        float[] elevArr = buffer.getElevation();

        double sumCap = 0.0;
        int nodeCount = 0;
        for (int i = 0; i < n; i++) {
            if (capArr != null) sumCap += capArr[i];
            if (isMaritimeCell(biomeArr, elevArr, i)) {
                nodeCount++;
            }
        }
        double currentAvgCapital = sumCap / Math.max(1, n);

        boolean capitalShift = cachedAvgCapital > 0.0 && Math.abs(currentAvgCapital - cachedAvgCapital) / cachedAvgCapital > 0.15;
        boolean topologyShift = Math.abs(nodeCount - cachedNodeCount) > 25;

        if (capitalShift || topologyShift || routingGraph.isEmpty()) {
            cachedAvgCapital = currentAvgCapital;
            cachedNodeCount = nodeCount;
            return true;
        }
        return false;
    }

    private static boolean isMaritimeCell(byte[] biomeArr, float[] elevArr, int i) {
        if (biomeArr != null) {
            int ord = biomeArr[i] & 0xFF;
            if (ord == Biome.OCEAN.ordinal() || ord == Biome.DEEP_OCEAN.ordinal()) {
                return true;
            }
        }
        return elevArr != null && elevArr[i] <= 0.0f;
    }

    /**
     * Rebuilds the dynamic trans-oceanic routing graph directly from {@link WorldBuffer} neighbor rings.
     */
    private static void rebuildRoutingGraph(WorldBuffer buffer) {
        logger.debug("Recalculating Trans-Oceanic Maritime Routing Graph (Avg Capital: {} kg, Nodes: {})...",
                String.format("%.1f", cachedAvgCapital), cachedNodeCount);

        routingGraph.clear();
        int n = buffer.getCapacity();
        long[] h3Arr = buffer.getH3Indexes();
        int[][] neighbors = buffer.getNeighborIndexes();
        byte[] biomeArr = buffer.getBiomes();
        float[] elevArr = buffer.getElevation();
        float[] tempArr = buffer.getTemperature();
        float[] capArr = buffer.getResourceCapital();

        for (int i = 0; i < n; i++) {
            if (!isMaritimeCell(biomeArr, elevArr, i)) continue;
            long srcId = (h3Arr != null && h3Arr[i] != 0L) ? h3Arr[i] : (long) i + 1L;
            Map<Long, Double> neighborCosts = new HashMap<>();

            int[] ring = (neighbors != null && i < neighbors.length) ? neighbors[i] : null;
            if (ring != null) {
                for (int nb : ring) {
                    if (nb < 0 || nb >= n || nb == i) continue;
                    if (!isMaritimeCell(biomeArr, elevArr, nb)) continue;
                    long dstId = (h3Arr != null && h3Arr[nb] != 0L) ? h3Arr[nb] : (long) nb + 1L;
                    double avgTemp = tempArr != null ? 0.5 * (tempArr[i] + tempArr[nb]) : 15.0;
                    double avgCap = capArr != null ? 0.5 * (capArr[i] + capArr[nb]) : 100.0;
                    double cost = computeHydrodynamicEdgeCost(avgCap, avgTemp, 100.0);
                    if (Double.isFinite(cost)) {
                        neighborCosts.put(dstId, cost);
                    }
                }
            }
            if (!neighborCosts.isEmpty()) {
                routingGraph.put(srcId, neighborCosts);
            }
        }
    }

    private static double computeHydrodynamicEdgeCost(double avgCapital, double avgTempC, double distKm) {
        double iceDrag = avgTempC < -1.8 ? (avgCapital > 1000.0 ? 2.5 : Double.POSITIVE_INFINITY) : 1.0;
        if (Double.isInfinite(iceDrag)) return Double.POSITIVE_INFINITY;
        double vesselSpeedKmH = 8.0 + 22.0 * Math.tanh(Math.max(0.0, avgCapital) / 500.0);
        return (distKm / vesselSpeedKmH) * iceDrag;
    }

    /**
     * Applies dynamic maritime commercial flux facilitation over active sea lanes without phantom capital minting.
     */
    private static void applyRoutingGraphEffects(WorldBuffer buffer, double timeStepDays) {
        int n = buffer.getCapacity();
        long[] h3Arr = buffer.getH3Indexes();
        float[] fluxArr = buffer.getCommercialFluxVolume();
        float[] capArr = buffer.getResourceCapital();
        double dtYears = PhysicalUnitsHelper.daysToYears(timeStepDays);

        for (int i = 0; i < n; i++) {
            long cellId = (h3Arr != null && h3Arr[i] != 0L) ? h3Arr[i] : (long) i + 1L;
            Map<Long, Double> routes = routingGraph.get(cellId);
            if (routes != null && !routes.isEmpty() && fluxArr != null) {
                double connectivity = Math.min(2.0, 1.0 + 0.05 * routes.size());
                double baseCap = capArr != null ? Math.max(0.0, capArr[i]) : 0.0;
                fluxArr[i] += (float) (0.01 * connectivity * baseCap * dtYears);
            }
        }
    }

    /**
     * Returns an unmodifiable view of the active maritime routing graph for diagnostics.
     *
     * @return current adjacency map of maritime cell IDs to route traversal costs
     */
    public static Map<Long, Map<Long, Double>> getRoutingGraph() {
        return Map.copyOf(routingGraph);
    }

    /**
     * Calculates the great-circle spherical haversine distance on Earth ($R = 6\,371\,\text{km}$).
     *
     * @param lat1 origin latitude in degrees
     * @param lon1 origin longitude in degrees
     * @param lat2 destination latitude in degrees
     * @param lon2 destination longitude in degrees
     * @return spherical distance in kilometers [$\text{km}$]
     */
    public static double calculateGreatCircleDistance(double lat1, double lon1, double lat2, double lon2) {
        return calculateGreatCircleDistance(lat1, lon1, lat2, lon2, 6371.0);
    }

    /**
     * Calculates the great-circle spherical haversine distance for an arbitrary planetary radius.
     *
     * @param lat1           origin latitude in degrees
     * @param lon1           origin longitude in degrees
     * @param lat2           destination latitude in degrees
     * @param lon2           destination longitude in degrees
     * @param planetRadiusKm planetary radius in kilometers [$\text{km}$]
     * @return spherical distance in kilometers [$\text{km}$]
     */
    public static double calculateGreatCircleDistance(double lat1, double lon1, double lat2, double lon2, double planetRadiusKm) {
        double r = (planetRadiusKm > 0) ? planetRadiusKm : 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return r * c;
    }

    /**
     * Clears the cached maritime routing graph and invalidation counters.
     */
    public static void clearCache() {
        routingGraph.clear();
        cachedAvgCapital = -1.0;
        cachedNodeCount = -1;
    }
}
