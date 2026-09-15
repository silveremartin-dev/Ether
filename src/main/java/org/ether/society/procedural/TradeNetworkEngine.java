/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
  * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Trade & Infrastructure Network Engine.
 * Models historical trade routes (e.g. Silk Road, Trans-Saharan, Maritime Corridors)
 * based on H3 terrain movement friction and population hubs.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.4.0
 */
public class TradeNetworkEngine {
    private static final Logger logger = LoggerFactory.getLogger(TradeNetworkEngine.class);

    public record TradeRoute(H3Cell origin, H3Cell destination, List<H3Cell> pathCells, double totalFrictionCost) {}

    private static final List<TradeRoute> latestRoutes = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static List<TradeRoute> getLatestRoutes() {
        return Collections.unmodifiableList(latestRoutes);
    }

    /**
     * Simulates and computes primary trade routes between high-density population nodes.
     * Enhances capital stock, labor efficiency, and technological diffusion along trade paths.
     */
    public static List<TradeRoute> generateTradeNetworks(List<H3Cell> cells, double techLevel) {
        if (cells == null || cells.isEmpty()) return Collections.emptyList();

        // 1. Identify trade hubs (cities / capitals with population > 500)
        List<H3Cell> hubs = cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() >= 200)
                .sorted(Comparator.comparingInt(H3Cell::getPopulation).reversed())
                .limit(20)
                .toList();

        if (hubs.size() < 2) return Collections.emptyList();

        Map<Long, H3Cell> cellMap = new HashMap<>();
        for (H3Cell c : cells) {
            cellMap.put(c.getH3Index(), c);
        }

        List<TradeRoute> activeRoutes = new ArrayList<>();

        // 2. Pair hubs and find minimal friction pathways
        for (int i = 0; i < hubs.size() - 1; i++) {
            H3Cell origin = hubs.get(i);
            for (int j = i + 1; j < Math.min(i + 4, hubs.size()); j++) {
                H3Cell destination = hubs.get(j);

                TradeRoute route = computeFrictionPath(origin, destination, cells, techLevel);
                if (route != null) {
                    activeRoutes.add(route);
                    applyTradeCorridorEffects(route, techLevel);
                }
            }
        }

        latestRoutes.clear();
        latestRoutes.addAll(activeRoutes);

        logger.info("Generated {} active trade corridors across {} population hubs.", activeRoutes.size(), hubs.size());
        return activeRoutes;
    }

    /**
     * Computes the lowest-friction route between two cells using cost-distance heuristics.
     */
    private static TradeRoute computeFrictionPath(H3Cell start, H3Cell end, List<H3Cell> cells, double techLevel) {
        double distLat = end.getLatitude() - start.getLatitude();
        double distLng = end.getLongitude() - start.getLongitude();
        double directDist = Math.sqrt(distLat * distLat + distLng * distLng);

        if (directDist > 60.0) return null; // Too distant for direct trade route

        List<H3Cell> path = new ArrayList<>();
        path.add(start);

        H3Cell current = start;
        double totalCost = 0.0;
        int steps = 0;

        while (current != end && steps < 50) {
            H3Cell nextBest = null;
            double bestScore = Double.MAX_VALUE;

            for (H3Cell neighbor : cells) {
                if (neighbor == current) continue;

                double stepDist = Math.hypot(neighbor.getLatitude() - current.getLatitude(), neighbor.getLongitude() - current.getLongitude());
                if (stepDist > 4.0) continue; // Only adjacent neighborhood

                double remainingDist = Math.hypot(end.getLatitude() - neighbor.getLatitude(), end.getLongitude() - neighbor.getLongitude());
                double friction = neighbor.calculateMovementFriction(techLevel);

                double costScore = (friction * 2.0) + remainingDist;
                if (costScore < bestScore && !path.contains(neighbor)) {
                    bestScore = costScore;
                    nextBest = neighbor;
                }
            }

            if (nextBest == null) break;
            current = nextBest;
            path.add(current);
            totalCost += current.getMovementFriction();
            steps++;

            if (Math.hypot(current.getLatitude() - end.getLatitude(), current.getLongitude() - end.getLongitude()) < 1.5) {
                path.add(end);
                break;
            }
        }

        return new TradeRoute(start, end, path, totalCost);
    }

    /**
     * Boosts capital, technology diffusion, and infrastructure along the trade route corridor.
     */
    private static void applyTradeCorridorEffects(TradeRoute route, double techLevel) {
        if (route == null || route.pathCells().isEmpty()) return;

        double routeValue = 50.0 * (1.0 + techLevel * 0.5) / Math.max(1.0, route.totalFrictionCost() * 0.1);

        // Marine & Ocean Acidification Coupling: depleted marine biomass diminishes route value
        double marineFactor = 1.0;
        if (route.origin() != null && route.origin().getBiomassFish() != null) {
            marineFactor = Math.clamp(route.origin().getBiomassFish() / 500.0, 0.2, 1.0);
        }
        routeValue *= marineFactor;

        for (H3Cell cell : route.pathCells()) {
            // Capital accumulation along trade route
            cell.setResourceCapital(cell.getResourceCapital() + routeValue * 0.5);

            // Technology diffusion along trade route
            if (cell.getTechnologyLevel() < techLevel) {
                cell.setTechnologyLevel(Math.min(techLevel, cell.getTechnologyLevel() + 0.05));
            }

            // Gini index slightly increases in trading hubs due to merchant wealth accumulation
            cell.setGiniIndex(Math.clamp(cell.getGiniIndex() + 0.005, 0.15, 0.85));
        }
    }
}
