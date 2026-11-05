/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.ThermodynamicMigrationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Trade &amp; Infrastructure Network Engine.
 * Models historical trade routes (e.g. Silk Road, Trans-Saharan, Maritime Corridors)
 * based on H3 terrain movement friction and population hubs using continuous economic
 * gravity models (Anderson 1979; Head &amp; Mayer 2014).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see TradeCorridorArbitrageHelper
 */
public class TradeNetworkEngine {
    private static final Logger logger = LoggerFactory.getLogger(TradeNetworkEngine.class);

    /**
     * Immutable representation of an active trade corridor between two H3 population hubs.
     *
     * @param origin            origin hub cell
     * @param destination       destination hub cell
     * @param pathCells         ordered sequence of H3 cells traversed
     * @param totalFrictionCost cumulative terrain movement friction along the corridor
     */
    public record TradeRoute(H3Cell origin, H3Cell destination, List<H3Cell> pathCells, double totalFrictionCost) {}

    private static final List<TradeRoute> latestRoutes = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static double cumulativeTradedFood = 0.0;
    private static double cumulativeTradedCapital = 0.0;
    private static double cumulativeTradedMetal = 0.0;
    private static final Set<Long> previouslyScoredChokeCells = new HashSet<>();

    /**
     * Resets cumulative trade statistics and active route cache for scenario isolation.
     */
    public static void resetCumulativeStats() {
        cumulativeTradedFood = 0.0;
        cumulativeTradedCapital = 0.0;
        cumulativeTradedMetal = 0.0;
        latestRoutes.clear();
        previouslyScoredChokeCells.clear();
    }

    /** @return cumulative food traded across ticks ($\text{GJ}$) */
    public static double getCumulativeTradedFood() { return cumulativeTradedFood; }
    /** @return cumulative physical capital traded across ticks */
    public static double getCumulativeTradedCapital() { return cumulativeTradedCapital; }
    /** @return cumulative metal traded across ticks ($\text{kg}$) */
    public static double getCumulativeTradedMetal() { return cumulativeTradedMetal; }

    static void addCumulativeTradedFood(double delta) { cumulativeTradedFood += delta; }
    static void addCumulativeTradedCapital(double delta) { cumulativeTradedCapital += delta; }
    static void addCumulativeTradedMetal(double delta) { cumulativeTradedMetal += delta; }

    /**
     * Returns an unmodifiable snapshot of the latest computed trade routes.
     *
     * @return list of active {@link TradeRoute} corridors
     */
    public static List<TradeRoute> getLatestRoutes() {
        return Collections.unmodifiableList(latestRoutes);
    }

    /**
     * Backward-compatible delegation defaulting to annual time step ($\Delta t = 1.0$ year).
     *
     * @param cells     active populated cells
     * @param techLevel baseline or average technology level
     * @return list of active trade routes
     */
    public static List<TradeRoute> generateTradeNetworks(List<H3Cell> cells, double techLevel) {
        return generateTradeNetworks(cells, techLevel, 1.0);
    }

    /**
     * Simulates and computes primary trade routes between high-density population nodes.
     * Conducts bilateral physical commodity arbitrage (food, metal, capital tools) and diffuses technology.
     *
     * @param cells     active populated cells
     * @param techLevel baseline or average technology level
     * @param dtYears   simulation time-step in Julian years
     * @return list of active trade routes
     */
    public static List<TradeRoute> generateTradeNetworks(List<H3Cell> cells, double techLevel, double dtYears) {
        if (cells == null || cells.isEmpty()) return Collections.emptyList();

        // 1. Identify geographically distributed trade hubs (spatial non-maximum suppression across clusters)
        List<H3Cell> sortedCells = cells.stream()
                .filter(c -> c != null && c.getPopulation() != null && c.getPopulation() >= 150)
                .sorted(Comparator.comparingInt(H3Cell::getPopulation).reversed())
                .toList();

        List<H3Cell> hubs = new ArrayList<>();
        for (H3Cell c : sortedCells) {
            boolean tooClose = false;
            for (H3Cell h : hubs) {
                double distKm = ThermodynamicMigrationEngine.calculateHaversineDistance(
                        c.getLatitudeOr(0.0), c.getLongitudeOr(0.0),
                        h.getLatitudeOr(0.0), h.getLongitudeOr(0.0));
                if (distKm < 110.0) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                hubs.add(c);
            }
        }

        if (hubs.size() < 2) return Collections.emptyList();

        Map<Long, H3Cell> cellMap = new HashMap<>();
        for (H3Cell c : cells) {
            if (c != null && c.getH3Index() != null) {
                cellMap.put(c.getH3Index(), c);
            }
        }

        List<TradeRoute> activeRoutes = new ArrayList<>();

        // 2. Continuous Gravity Model for bilateral trade corridor establishment (Anderson 1979; Head & Mayer 2014)
        for (int i = 0; i < hubs.size() - 1; i++) {
            H3Cell origin = hubs.get(i);
            double popOrigin = origin.getPopulationOr(150);

            for (int j = i + 1; j < hubs.size(); j++) {
                H3Cell destination = hubs.get(j);
                double popDest = destination.getPopulationOr(150);
                double distKm = ThermodynamicMigrationEngine.calculateHaversineDistance(
                        origin.getLatitudeOr(0.0), origin.getLongitudeOr(0.0),
                        destination.getLatitudeOr(0.0), destination.getLongitudeOr(0.0));

                double gravityPotential = Math.sqrt(popOrigin * popDest) / (1.0 + Math.pow(distKm / 1600.0, 2.0));
                if (gravityPotential < 15.0 || distKm > 6600.0) continue;

                TradeRoute route = computeFrictionPath(origin, destination, cells, cellMap, techLevel);
                if (route != null) {
                    activeRoutes.add(route);
                    TradeCorridorArbitrageHelper.applyTradeCorridorEffects(route, techLevel, dtYears);
                }
            }
        }

        latestRoutes.clear();
        latestRoutes.addAll(activeRoutes);

        TradeCorridorArbitrageHelper.evaluateStrategicChokePoints(cellMap, activeRoutes, previouslyScoredChokeCells, dtYears);

        logger.info("Generated {} active trade corridors across {} population hubs.", activeRoutes.size(), hubs.size());
        return activeRoutes;
    }

    /**
     * Computes the lowest-friction route between two cells using cost-distance heuristics.
     */
    private static TradeRoute computeFrictionPath(H3Cell start, H3Cell end, List<H3Cell> cells,
                                                  Map<Long, H3Cell> cellMap, double techLevel) {
        double distLat = end.getLatitude() - start.getLatitude();
        double distLng = end.getLongitude() - start.getLongitude();
        double directDist = Math.sqrt(distLat * distLat + distLng * distLng);

        if (directDist > 60.0) return null;

        List<H3Cell> path = new ArrayList<>();
        Set<H3Cell> visited = new HashSet<>();
        path.add(start);
        visited.add(start);

        H3Cell current = start;
        double totalCost = 0.0;
        int steps = 0;

        while (current != end && steps < 50) {
            H3Cell nextBest = null;
            double bestScore = Double.MAX_VALUE;

            List<H3Cell> candidates = new ArrayList<>();
            if (cellMap != null && current.getH3Index() != 0L) {
                try {
                    List<Long> neighborIndices = org.ether.society.h3.H3Service.getInstance().getNeighbors(current.getH3Index());
                    if (neighborIndices != null) {
                        for (Long nIdx : neighborIndices) {
                            H3Cell nCell = cellMap.get(nIdx);
                            if (nCell != null && nCell != current) {
                                candidates.add(nCell);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (candidates.isEmpty()) {
                for (H3Cell neighbor : cells) {
                    if (neighbor == current) continue;
                    double stepDistKm = ThermodynamicMigrationEngine.calculateHaversineDistance(
                            neighbor.getLatitudeOr(0.0), neighbor.getLongitudeOr(0.0),
                            current.getLatitudeOr(0.0), current.getLongitudeOr(0.0));
                    if (stepDistKm <= 660.0) {
                        candidates.add(neighbor);
                    }
                }
            }

            for (H3Cell neighbor : candidates) {
                if (visited.contains(neighbor)) continue;

                double remainingDistKm = ThermodynamicMigrationEngine.calculateHaversineDistance(
                        end.getLatitudeOr(0.0), end.getLongitudeOr(0.0),
                        neighbor.getLatitudeOr(0.0), neighbor.getLongitudeOr(0.0));
                double friction = neighbor.calculateMovementFriction();

                double costScore = (friction * 2.0) + (remainingDistKm / 111.32);
                if (costScore < bestScore) {
                    bestScore = costScore;
                    nextBest = neighbor;
                }
            }

            if (nextBest == null) break;
            current = nextBest;
            path.add(current);
            visited.add(current);
            totalCost += current.getMovementFriction();
            steps++;

            double arrivalDistKm = ThermodynamicMigrationEngine.calculateHaversineDistance(
                    current.getLatitudeOr(0.0), current.getLongitudeOr(0.0),
                    end.getLatitudeOr(0.0), end.getLongitudeOr(0.0));
            if (arrivalDistKm < 165.0) {
                path.add(end);
                break;
            }
        }

        if (!visited.contains(end)) {
            path.add(end);
            totalCost += directDist * 1.2;
        }

        return new TradeRoute(start, end, path, totalCost);
    }
}
