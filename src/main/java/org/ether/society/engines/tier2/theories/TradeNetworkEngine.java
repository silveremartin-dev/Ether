/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
  * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

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
 * @version 1.0.0-beta.2
 */
public class TradeNetworkEngine {
    private static final Logger logger = LoggerFactory.getLogger(TradeNetworkEngine.class);

    public record TradeRoute(H3Cell origin, H3Cell destination, List<H3Cell> pathCells, double totalFrictionCost) {}

    private static final List<TradeRoute> latestRoutes = new java.util.concurrent.CopyOnWriteArrayList<>();

    // Physical trade cumulative mass/value counters across ticks
    private static double cumulativeTradedFood = 0.0;
    private static double cumulativeTradedCapital = 0.0;
    private static double cumulativeTradedMetal = 0.0;

    /**
     * Resets cumulative trade statistics for scenario isolation.
     */
    public static void resetCumulativeStats() {
        cumulativeTradedFood = 0.0;
        cumulativeTradedCapital = 0.0;
        cumulativeTradedMetal = 0.0;
    }

    public static double getCumulativeTradedFood() { return cumulativeTradedFood; }
    public static double getCumulativeTradedCapital() { return cumulativeTradedCapital; }
    public static double getCumulativeTradedMetal() { return cumulativeTradedMetal; }

    /*
     * Get latest routes.
     * Enforces physical invariants and updates associated state variables within {@code TradeNetworkEngine}.
     *
     * @return the resulting computation or state reference
     */
    public static List<TradeRoute> getLatestRoutes() {
        return Collections.unmodifiableList(latestRoutes);
    }

    /**
     * Backward-compatible delegation defaulting to annual time step (dt = 1.0 year).
     */
    public static List<TradeRoute> generateTradeNetworks(List<H3Cell> cells, double techLevel) {
        return generateTradeNetworks(cells, techLevel, 1.0);
    }

    /**
     * Simulates and computes primary trade routes between high-density population nodes.
     * Conducts bilateral physical commodity arbitrage (food, metal, capital tools) and diffuses technology.
     *
     * @param cells active populated cells
     * @param techLevel baseline or average technology level
     * @param dtYears simulation time-step in Julian years
     * @return list of active trade routes
     */
    public static List<TradeRoute> generateTradeNetworks(List<H3Cell> cells, double techLevel, double dtYears) {
        if (cells == null || cells.isEmpty()) return Collections.emptyList();

        // 1. Identify geographically distributed trade hubs (spatial non-maximum suppression across islands)
        List<H3Cell> sortedCells = cells.stream()
                .filter(c -> c.getPopulation() != null && c.getPopulation() >= 150)
                .sorted(Comparator.comparingInt(H3Cell::getPopulation).reversed())
                .toList();

        List<H3Cell> hubs = new ArrayList<>();
        for (H3Cell c : sortedCells) {
            boolean tooClose = false;
            for (H3Cell h : hubs) {
                if (Math.hypot(c.getLatitude() - h.getLatitude(), c.getLongitude() - h.getLongitude()) < 1.0) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                hubs.add(c);
                if (hubs.size() >= 20) break;
            }
        }
        if (hubs.size() < 2) {
            hubs = sortedCells.stream().limit(10).toList();
        }

        if (hubs.size() < 2) return Collections.emptyList();

        Map<Long, H3Cell> cellMap = new HashMap<>();
        for (H3Cell c : cells) {
            cellMap.put(c.getH3Index(), c);
        }

        List<TradeRoute> activeRoutes = new ArrayList<>();

        // 2. Pair hubs and find minimal friction pathways
        for (int i = 0; i < hubs.size() - 1; i++) {
            H3Cell origin = hubs.get(i);
            for (int j = i + 1; j < Math.min(i + 5, hubs.size()); j++) {
                H3Cell destination = hubs.get(j);

                TradeRoute route = computeFrictionPath(origin, destination, cells, techLevel);
                if (route != null) {
                    activeRoutes.add(route);
                    applyTradeCorridorEffects(route, techLevel, dtYears);
                }
            }
        }

        latestRoutes.clear();
        latestRoutes.addAll(activeRoutes);

        logger.info("Generated {} active trade corridors across {} population hubs.", activeRoutes.size(), hubs.size());
        return activeRoutes;
    }

    /*
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
                if (stepDist > 6.0) continue; // Adjacent or archipelagic strait neighborhood

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

        // Maritime bridge: if open straits separate two island hubs, connect maritime route directly
        if (!path.contains(end)) {
            path.add(end);
            totalCost += directDist * 1.2;
        }

        return new TradeRoute(start, end, path, totalCost);
    }

    /*
     * Conducts bilateral physical commodity arbitrage between trade route origin and destination,
     * and boosts capital, technology diffusion, and infrastructure along the trade corridor.
     */
    private static void applyTradeCorridorEffects(TradeRoute route, double techLevel, double dtYears) {
        if (route == null || route.pathCells().isEmpty()) return;

        double friction = Math.max(1.0, route.totalFrictionCost() * 0.1);
        double conductance = Math.min(1.0, 1.0 / friction);

        H3Cell orig = route.origin();
        H3Cell dest = route.destination();

        // 1. Bilateral Physical Commodity Arbitrage between Origin and Destination Hubs
        if (orig != null && dest != null && orig.getPopulation() > 0 && dest.getPopulation() > 0) {
            // A. Physical Food Arbitrage (Ricardian Grain/Fish Trade):
            double origFood = orig.getFoodResource() != null ? orig.getFoodResource() : 0.0;
            double destFood = dest.getFoodResource() != null ? dest.getFoodResource() : 0.0;
            double origNeed = orig.getPopulation() * 3.362; // Annual subsistence metabolic baseline in GJ
            double destNeed = dest.getPopulation() * 3.362;

            double origSurplus = origFood - origNeed;
            double destSurplus = destFood - destNeed;

            if (origSurplus > 5.0 && destSurplus < -5.0) {
                // Origin exports food to deficit destination
                double exportPotential = Math.min(origSurplus * 0.35, -destSurplus * 0.80) * conductance * dtYears;
                if (exportPotential > 0.0) {
                    orig.setFoodResource(Math.max(0.0, origFood - exportPotential));
                    dest.setFoodResource(destFood + exportPotential * 0.95); // 5% maritime transport spoilage
                    cumulativeTradedFood += exportPotential;
                }
            } else if (destSurplus > 5.0 && origSurplus < -5.0) {
                // Destination exports food to deficit origin
                double exportPotential = Math.min(destSurplus * 0.35, -origSurplus * 0.80) * conductance * dtYears;
                if (exportPotential > 0.0) {
                    dest.setFoodResource(Math.max(0.0, destFood - exportPotential));
                    orig.setFoodResource(origFood + exportPotential * 0.95);
                    cumulativeTradedFood += exportPotential;
                }
            }

            // B. Physical Metal and Tool Arbitrage:
            double origMetal = orig.getResourceMetal() != null ? orig.getResourceMetal() : 0.0;
            double destMetal = dest.getResourceMetal() != null ? dest.getResourceMetal() : 0.0;
            if (origMetal > destMetal + 10.0) {
                double metalTrade = Math.min(origMetal * 0.20, (origMetal - destMetal) * 0.25) * conductance * dtYears;
                if (metalTrade > 0.0) {
                    orig.setResourceMetal(Math.max(0.0, origMetal - metalTrade));
                    dest.setResourceMetal(destMetal + metalTrade * 0.98); // 2% oxidation loss
                    cumulativeTradedMetal += metalTrade;
                }
            } else if (destMetal > origMetal + 10.0) {
                double metalTrade = Math.min(destMetal * 0.20, (destMetal - origMetal) * 0.25) * conductance * dtYears;
                if (metalTrade > 0.0) {
                    dest.setResourceMetal(Math.max(0.0, destMetal - metalTrade));
                    orig.setResourceMetal(origMetal + metalTrade * 0.98);
                    cumulativeTradedMetal += metalTrade;
                }
            }

            // C. Physical Capital Tool Arbitrage (Intensive K/N Rebalancing):
            double origCap = orig.getResourceCapital() != null ? orig.getResourceCapital() : 0.0;
            double destCap = dest.getResourceCapital() != null ? dest.getResourceCapital() : 0.0;
            double origCapPerCap = origCap / Math.max(1.0, orig.getPopulation());
            double destCapPerCap = destCap / Math.max(1.0, dest.getPopulation());
            if (origCapPerCap > destCapPerCap * 1.3 && origCap > 50.0) {
                double capTrade = Math.min(origCap * 0.15, (origCapPerCap - destCapPerCap) * dest.getPopulation() * 0.20) * conductance * dtYears;
                if (capTrade > 0.0) {
                    orig.setResourceCapital(Math.max(0.0, origCap - capTrade));
                    dest.setResourceCapital(destCap + capTrade * 0.98);
                    cumulativeTradedCapital += capTrade;
                }
            } else if (destCapPerCap > origCapPerCap * 1.3 && destCap > 50.0) {
                double capTrade = Math.min(destCap * 0.15, (destCapPerCap - origCapPerCap) * orig.getPopulation() * 0.20) * conductance * dtYears;
                if (capTrade > 0.0) {
                    dest.setResourceCapital(Math.max(0.0, destCap - capTrade));
                    orig.setResourceCapital(origCap + capTrade * 0.98);
                    cumulativeTradedCapital += capTrade;
                }
            }
        }

        // 2. Route Corridor Spillovers (Scaled by dtYears and local settlement capacity)
        double routeValue = 12.0 * (1.0 + techLevel * 0.35) / friction * dtYears;

        // Marine biomass coupling
        double marineFactor = 1.0;
        if (orig != null && orig.getBiomassFish() != null) {
            marineFactor = Math.clamp(orig.getBiomassFish() / 500.0, 0.2, 1.0);
        }
        routeValue *= marineFactor;

        int pathLen = Math.max(1, route.pathCells().size());
        double perCellSpillover = routeValue / Math.sqrt(pathLen);

        for (H3Cell cell : route.pathCells()) {
            int cellPop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (cellPop <= 0) continue;

            // Merchant capital accumulation along trade route bounded by local labor absorption capacity
            double curCap = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;
            double maxMerchantCap = cellPop * 15.0 * Math.max(1.0, techLevel);
            if (curCap < maxMerchantCap) {
                cell.setResourceCapital(Math.min(maxMerchantCap, curCap + perCellSpillover * 0.5));
            }

            // Technology diffusion along trade route
            double curTech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            if (curTech < techLevel) {
                cell.setTechnologyLevel(Math.min(techLevel, curTech + 0.03 * dtYears));
            }

            // Gini index slightly increases in trading hubs due to merchant wealth accumulation
            double curGini = cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.3;
            cell.setGiniIndex(Math.clamp(curGini + 0.005 * dtYears, 0.15, 0.85));
        }
    }
}


