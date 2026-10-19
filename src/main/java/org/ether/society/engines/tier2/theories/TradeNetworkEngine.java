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
    private static final Set<Long> previouslyScoredChokeCells = new HashSet<>();

    /**
     * Resets cumulative trade statistics for scenario isolation.
     */
    public static void resetCumulativeStats() {
        cumulativeTradedFood = 0.0;
        cumulativeTradedCapital = 0.0;
        cumulativeTradedMetal = 0.0;
        latestRoutes.clear();
        previouslyScoredChokeCells.clear();
    }

    public static double getCumulativeTradedFood() { return cumulativeTradedFood; }
    public static double getCumulativeTradedCapital() { return cumulativeTradedCapital; }
    public static double getCumulativeTradedMetal() { return cumulativeTradedMetal; }

    /**
     * Get latest routes.
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
                .filter(c -> c != null && c.getPopulation() != null && c.getPopulation() >= 150)
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
            if (c != null && c.getH3Index() != null) {
                cellMap.put(c.getH3Index(), c);
            }
        }

        List<TradeRoute> activeRoutes = new ArrayList<>();

        // 2. Pair hubs and find minimal friction pathways
        for (int i = 0; i < hubs.size() - 1; i++) {
            H3Cell origin = hubs.get(i);
            for (int j = i + 1; j < Math.min(i + 5, hubs.size()); j++) {
                H3Cell destination = hubs.get(j);

                TradeRoute route = computeFrictionPath(origin, destination, cells, cellMap, techLevel);
                if (route != null) {
                    activeRoutes.add(route);
                    applyTradeCorridorEffects(route, techLevel, dtYears);
                }
            }
        }

        latestRoutes.clear();
        latestRoutes.addAll(activeRoutes);

        evaluateStrategicChokePoints(cellMap, activeRoutes, dtYears);

        logger.info("Generated {} active trade corridors across {} population hubs.", activeRoutes.size(), hubs.size());
        return activeRoutes;
    }

    /**
     * Computes the lowest-friction route between two cells using cost-distance heuristics.
     */
    private static TradeRoute computeFrictionPath(H3Cell start, H3Cell end, List<H3Cell> cells, Map<Long, H3Cell> cellMap, double techLevel) {
        double distLat = end.getLatitude() - start.getLatitude();
        double distLng = end.getLongitude() - start.getLongitude();
        double directDist = Math.sqrt(distLat * distLat + distLng * distLng);

        if (directDist > 60.0) return null; // Too distant for direct trade route

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

            // Fallback for non-indexed synthetic test cells or archipelagic strait jumps
            if (candidates.isEmpty()) {
                for (H3Cell neighbor : cells) {
                    if (neighbor == current) continue;
                    double stepDist = Math.hypot(neighbor.getLatitude() - current.getLatitude(), neighbor.getLongitude() - current.getLongitude());
                    if (stepDist <= 6.0) {
                        candidates.add(neighbor);
                    }
                }
            }

            for (H3Cell neighbor : candidates) {
                if (visited.contains(neighbor)) continue;

                double remainingDist = Math.hypot(end.getLatitude() - neighbor.getLatitude(), end.getLongitude() - neighbor.getLongitude());
                double friction = neighbor.calculateMovementFriction(techLevel);

                double costScore = (friction * 2.0) + remainingDist;
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

            if (Math.hypot(current.getLatitude() - end.getLatitude(), current.getLongitude() - end.getLongitude()) < 1.5) {
                path.add(end);
                break;
            }
        }

        // Maritime bridge: if open straits separate two island hubs, connect maritime route directly
        if (!visited.contains(end)) {
            path.add(end);
            totalCost += directDist * 1.2;
        }

        return new TradeRoute(start, end, path, totalCost);
    }

    /**
     * Conducts bilateral physical commodity arbitrage between trade route origin and destination,
     * and boosts capital, technology diffusion, and infrastructure along the trade corridor.
     */
    private static void applyTradeCorridorEffects(TradeRoute route, double techLevel, double dtYears) {
        if (route == null || route.pathCells().isEmpty()) return;

        double friction = Math.max(1.0, route.totalFrictionCost() * 0.1);

        H3Cell orig = route.origin();
        H3Cell dest = route.destination();

        // Physical transport retention derived from technology:
        // Food spoilage drops from ~20% (ancient caravans) to <0.5% (modern refrigerated intermodal)
        double foodRetention = Math.clamp(0.78 + 0.05 * techLevel, 0.78, 0.995);
        // Bulk material attrition drops from 4% to 0.1%
        double bulkRetention = Math.clamp(0.95 + 0.01 * techLevel, 0.95, 0.999);

        // Monetary lubrication: circulating bullion/specie reduces barter friction and enhances conductance
        double origPrecious = orig != null ? orig.getResourcePreciousMetalOr(0.0) : 0.0;
        double destPrecious = dest != null ? dest.getResourcePreciousMetalOr(0.0) : 0.0;
        double monetaryLubrication = 1.0 + Math.min(1.5, Math.log1p(origPrecious + destPrecious) * 0.25);
        double conductance = Math.min(1.0, (1.0 / friction) * monetaryLubrication);

        // 1. Bilateral Physical Commodity Arbitrage between Origin and Destination Hubs
        int origPop = orig != null ? orig.getPopulationOr(0) : 0;
        int destPop = dest != null ? dest.getPopulationOr(0) : 0;
        if (orig != null && dest != null && origPop > 0 && destPop > 0) {
            // A. Physical Food Arbitrage (Ricardian Grain/Fish Trade):
            double origFood = orig.getFoodResourceOr(0.0);
            double destFood = dest.getFoodResourceOr(0.0);
            // Annual subsistence metabolic baseline in GJ (age pyramid, labor intensity & hominin species)
            double origNeed = orig.calculateAnnualMetabolicNeedGJ();
            double destNeed = dest.calculateAnnualMetabolicNeedGJ();

            double origSurplus = origFood - origNeed;
            double destSurplus = destFood - destNeed;

            if (origSurplus > 5.0 && destSurplus < -5.0) {
                // Origin exports food to deficit destination
                double exportPotential = Math.min(origSurplus * 0.35, -destSurplus * 0.80) * conductance * dtYears;
                if (exportPotential > 0.0) {
                    orig.setFoodResource(Math.max(0.0, origFood - exportPotential));
                    dest.setFoodResource(destFood + exportPotential * foodRetention);
                    cumulativeTradedFood += exportPotential;
                }
            } else if (destSurplus > 5.0 && origSurplus < -5.0) {
                // Destination exports food to deficit origin
                double exportPotential = Math.min(destSurplus * 0.35, -origSurplus * 0.80) * conductance * dtYears;
                if (exportPotential > 0.0) {
                    dest.setFoodResource(Math.max(0.0, destFood - exportPotential));
                    orig.setFoodResource(origFood + exportPotential * foodRetention);
                    cumulativeTradedFood += exportPotential;
                }
            }

            // B. Physical Metal and Tool Arbitrage:
            double origMetal = orig.getResourceMetalOr(0.0);
            double destMetal = dest.getResourceMetalOr(0.0);
            if (origMetal > destMetal + 10.0) {
                double metalTrade = Math.min(origMetal * 0.20, (origMetal - destMetal) * 0.25) * conductance * dtYears;
                if (metalTrade > 0.0) {
                    orig.setResourceMetal(Math.max(0.0, origMetal - metalTrade));
                    dest.setResourceMetal(destMetal + metalTrade * bulkRetention);
                    cumulativeTradedMetal += metalTrade;
                }
            } else if (destMetal > origMetal + 10.0) {
                double metalTrade = Math.min(destMetal * 0.20, (destMetal - origMetal) * 0.25) * conductance * dtYears;
                if (metalTrade > 0.0) {
                    dest.setResourceMetal(Math.max(0.0, destMetal - metalTrade));
                    orig.setResourceMetal(origMetal + metalTrade * bulkRetention);
                    cumulativeTradedMetal += metalTrade;
                }
            }

            // B2. Physical Fossil Energy Arbitrage (Coal, Hydrocarbons, Natural Gas)
            if (techLevel >= 2.8) {
                double origCoal = orig.getResourceCoalOr(0.0);
                double destCoal = dest.getResourceCoalOr(0.0);
                if (origCoal > destCoal + 50.0) {
                    double coalTrade = Math.min(origCoal * 0.15, (origCoal - destCoal) * 0.20) * conductance * dtYears;
                    orig.setResourceCoal(Math.max(0.0, origCoal - coalTrade));
                    dest.setResourceCoal(destCoal + coalTrade * bulkRetention);
                } else if (destCoal > origCoal + 50.0) {
                    double coalTrade = Math.min(destCoal * 0.15, (destCoal - origCoal) * 0.20) * conductance * dtYears;
                    dest.setResourceCoal(Math.max(0.0, destCoal - coalTrade));
                    orig.setResourceCoal(origCoal + coalTrade * bulkRetention);
                }

                double origOil = orig.getResourceOilOr(0.0);
                double destOil = dest.getResourceOilOr(0.0);
                if (origOil > destOil + 30.0) {
                    double oilTrade = Math.min(origOil * 0.20, (origOil - destOil) * 0.25) * conductance * dtYears;
                    orig.setResourceOil(Math.max(0.0, origOil - oilTrade));
                    dest.setResourceOil(destOil + oilTrade * bulkRetention);
                } else if (destOil > origOil + 30.0) {
                    double oilTrade = Math.min(destOil * 0.20, (destOil - origOil) * 0.25) * conductance * dtYears;
                    dest.setResourceOil(Math.max(0.0, destOil - oilTrade));
                    orig.setResourceOil(origOil + oilTrade * bulkRetention);
                }

                double origGas = orig.getResourceGasOr(0.0);
                double destGas = dest.getResourceGasOr(0.0);
                if (origGas > destGas + 25.0) {
                    double gasTrade = Math.min(origGas * 0.15, (origGas - destGas) * 0.20) * conductance * dtYears;
                    orig.setResourceGas(Math.max(0.0, origGas - gasTrade));
                    dest.setResourceGas(destGas + gasTrade * bulkRetention);
                } else if (destGas > origGas + 25.0) {
                    double gasTrade = Math.min(destGas * 0.15, (destGas - origGas) * 0.20) * conductance * dtYears;
                    dest.setResourceGas(Math.max(0.0, destGas - gasTrade));
                    orig.setResourceGas(origGas + gasTrade * bulkRetention);
                }
            }

            // B3. Physical Precious Metal / Specie Arbitrage (Gold, Silver, Bullion; tech >= 1.5)
            // Long-distance high-value settlement with minimal physical transport attrition
            if (techLevel >= 1.5) {
                if (origPrecious > destPrecious + 0.2) {
                    double preciousTrade = Math.min(origPrecious * 0.25, (origPrecious - destPrecious) * 0.30) * conductance * dtYears;
                    if (preciousTrade > 0.0) {
                        orig.setResourcePreciousMetal(Math.max(0.0, origPrecious - preciousTrade));
                        dest.setResourcePreciousMetal(destPrecious + preciousTrade * 0.999);
                    }
                } else if (destPrecious > origPrecious + 0.2) {
                    double preciousTrade = Math.min(destPrecious * 0.25, (destPrecious - origPrecious) * 0.30) * conductance * dtYears;
                    if (preciousTrade > 0.0) {
                        dest.setResourcePreciousMetal(Math.max(0.0, destPrecious - preciousTrade));
                        orig.setResourcePreciousMetal(origPrecious + preciousTrade * 0.999);
                    }
                }
            }

            // B4. Modern Critical Minerals & Rare Earth Elements Arbitrage (tech >= 4.0)
            if (techLevel >= 4.0) {
                double origRee = orig.getResourceRareEarthsOr(0.0);
                double destRee = dest.getResourceRareEarthsOr(0.0);
                if (origRee > destRee + 1.0) {
                    double reeTrade = Math.min(origRee * 0.20, (origRee - destRee) * 0.25) * conductance * dtYears;
                    orig.setResourceRareEarths(Math.max(0.0, origRee - reeTrade));
                    dest.setResourceRareEarths(destRee + reeTrade * bulkRetention);
                } else if (destRee > origRee + 1.0) {
                    double reeTrade = Math.min(destRee * 0.20, (destRee - origRee) * 0.25) * conductance * dtYears;
                    dest.setResourceRareEarths(Math.max(0.0, destRee - reeTrade));
                    orig.setResourceRareEarths(origRee + reeTrade * bulkRetention);
                }
            }

            // C. Physical Capital Tool Arbitrage (Intensive K/N Rebalancing):
            double origCap = orig.getResourceCapitalOr(0.0);
            double destCap = dest.getResourceCapitalOr(0.0);
            double origCapPerCap = origCap / Math.max(1.0, origPop);
            double destCapPerCap = destCap / Math.max(1.0, destPop);
            if (origCapPerCap > destCapPerCap * 1.3 && origCap > 50.0) {
                double capTrade = Math.min(origCap * 0.15, (origCapPerCap - destCapPerCap) * destPop * 0.20) * conductance * dtYears;
                if (capTrade > 0.0) {
                    orig.setResourceCapital(Math.max(0.0, origCap - capTrade));
                    dest.setResourceCapital(destCap + capTrade * bulkRetention);
                    cumulativeTradedCapital += capTrade;
                }
            } else if (destCapPerCap > origCapPerCap * 1.3 && destCap > 50.0) {
                double capTrade = Math.min(destCap * 0.15, (destCapPerCap - origCapPerCap) * origPop * 0.20) * conductance * dtYears;
                if (capTrade > 0.0) {
                    dest.setResourceCapital(Math.max(0.0, destCap - capTrade));
                    orig.setResourceCapital(origCap + capTrade * bulkRetention);
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
            int cellPop = cell.getPopulationOr(0);
            if (cellPop <= 0) continue;

            // Merchant capital accumulation along trade route bounded by local labor absorption capacity
            double curCap = cell.getResourceCapitalOr(0.0);
            double maxMerchantCap = cellPop * 15.0 * Math.max(1.0, techLevel);
            if (curCap < maxMerchantCap) {
                cell.setResourceCapital(Math.min(maxMerchantCap, curCap + perCellSpillover * 0.5));
            }

            // Technology diffusion along trade route
            double curTech = cell.getTechnologyLevelOr(1.0);
            if (curTech < techLevel) {
                cell.setTechnologyLevel(Math.min(techLevel, curTech + 0.03 * dtYears));
            }

            // Gini index slightly increases in trading hubs due to merchant wealth accumulation
            double curGini = cell.getGiniIndexOr(0.3);
            cell.setGiniIndex(Math.clamp(curGini + 0.005 * dtYears, 0.15, 0.85));
        }
    }

    /**
     * Evaluates topological transit frequencies across trade routes and identifies strategic choke points
     * (straits, mountain passes, isthmuses, and bottleneck transit cells), allocating transit toll rent.
     */
    private static void evaluateStrategicChokePoints(Map<Long, H3Cell> cellMap, List<TradeRoute> routes, double dtYears) {
        if (cellMap == null || cellMap.isEmpty()) return;

        Map<Long, Integer> transitFrequency = new HashMap<>();
        if (routes != null && !routes.isEmpty()) {
            for (TradeRoute r : routes) {
                if (r == null || r.pathCells() == null) continue;
                for (H3Cell c : r.pathCells()) {
                    if (c != null && c.getH3Index() != 0L) {
                        transitFrequency.merge(c.getH3Index(), 1, Integer::sum);
                    }
                }
            }
        }

        int totalRoutes = (routes != null) ? routes.size() : 0;
        int chokePointCount = 0;

        Set<Long> candidateIds = new HashSet<>(transitFrequency.keySet());
        candidateIds.addAll(previouslyScoredChokeCells);
        previouslyScoredChokeCells.clear();

        for (Long cellId : candidateIds) {
            H3Cell cell = cellMap.get(cellId);
            if (cell == null) continue;

            int frequency = transitFrequency.getOrDefault(cellId, 0);
            if (frequency > 0 && totalRoutes > 0) {
                // Funnel ratio: fraction of total active planetary/regional trade passing through this single cell
                double funnelRatio = (double) frequency / (double) totalRoutes;

                // Constriction multiplier: physical straits, isthmuses or rugged passes increase strategic leverage
                boolean isStraitOrCoast = Boolean.TRUE.equals(cell.getIsCoastal()) || (cell.getElevation() != null && cell.getElevation() < 50.0);
                boolean isMountainPass = (cell.getElevation() != null && cell.getElevation() > 800.0);
                double constrictionBonus = (isStraitOrCoast || isMountainPass) ? 1.35 : 1.0;

                double rawScore = Math.min(1.0, funnelRatio * constrictionBonus * 2.0);
                cell.setChokePointScore(rawScore);
                if (rawScore > 0.01) {
                    previouslyScoredChokeCells.add(cellId);
                }

                if (rawScore >= 0.25) {
                    chokePointCount++;
                    // Economic transit toll rent collected by the choke point
                    double tollRatePerTransit = 1.5 * Math.max(0.5, 1.0 + (cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() * 0.1 : 0.0));
                    double transitToll = rawScore * frequency * tollRatePerTransit * dtYears;

                    double currentTollRev = cell.getTransitTollRevenueOr(0.0);
                    cell.setTransitTollRevenue(currentTollRev + transitToll);

                    // Transit toll accrues directly into productive capital of the controlling cell
                    double currentCapital = cell.getResourceCapitalOr(0.0);
                    cell.setResourceCapital(currentCapital + transitToll);
                }
            } else {
                // Graceful decay of choke point score when trade routes shift elsewhere
                double currentScore = cell.getChokePointScoreOr(0.0);
                if (currentScore > 0.0) {
                    double decayed = currentScore * 0.90;
                    if (decayed > 0.01) {
                        cell.setChokePointScore(decayed);
                        previouslyScoredChokeCells.add(cellId);
                    } else {
                        cell.setChokePointScore(0.0);
                    }
                }
            }
        }

        if (chokePointCount > 0) {
            logger.debug("Trade Network Engine: Identified {} strategic trade choke points across active corridors.", chokePointCount);
        }
    }
}


