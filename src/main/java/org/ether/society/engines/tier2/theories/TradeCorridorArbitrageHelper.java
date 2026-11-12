/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bilateral physical commodity arbitrage, corridor spillover diffusion, and topological
 * strategic choke-point rent evaluator for {@link TradeNetworkRouter}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see TradeNetworkRouter
 */
final class TradeCorridorArbitrageHelper {
    private static final Logger logger = LoggerFactory.getLogger(TradeCorridorArbitrageHelper.class);

    private TradeCorridorArbitrageHelper() {
        // Static helper class
    }

    /**
     * Conducts bilateral physical commodity arbitrage (food, base metals, fossil hydrocarbons,
     * precious bullion, rare earths, and intensive capital tools) between route endpoints and
     * diffuses merchant capital and technology along intermediate corridor cells.
     *
     * @param route     active trade route
     * @param techLevel technological index
     * @param dtYears   simulation time step in Julian years
     */
    static void applyTradeCorridorEffects(TradeNetworkRouter.TradeRoute route, double techLevel, double dtYears) {
        if (route == null || route.pathCells().isEmpty()) return;

        double friction = Math.max(1.0, route.totalFrictionCost() * 0.1);
        H3Cell orig = route.origin();
        H3Cell dest = route.destination();

        double foodRetention = Math.clamp(0.78 + 0.05 * techLevel, 0.78, 0.995);
        double bulkRetention = Math.clamp(0.95 + 0.01 * techLevel, 0.95, 0.999);

        double origPrecious = orig != null ? orig.getResourcePreciousMetalOr(0.0) : 0.0;
        double destPrecious = dest != null ? dest.getResourcePreciousMetalOr(0.0) : 0.0;
        double monetaryLubrication = 1.0 + Math.min(1.5, Math.log1p(origPrecious + destPrecious) * 0.25);
        double conductance = Math.min(1.0, (1.0 / friction) * monetaryLubrication);

        int origPop = orig != null ? orig.getPopulationOr(0) : 0;
        int destPop = dest != null ? dest.getPopulationOr(0) : 0;
        if (orig != null && dest != null && origPop > 0 && destPop > 0) {
            arbitrageFood(orig, dest, conductance, foodRetention, dtYears);
            arbitrageBaseMetals(orig, dest, conductance, bulkRetention, dtYears);
            arbitragePreciousSpecie(orig, dest, origPrecious, destPrecious, conductance, dtYears);
            arbitrageFossilEnergy(orig, dest, conductance, bulkRetention, dtYears);
            arbitrageRareEarths(orig, dest, conductance, bulkRetention, dtYears);
            arbitrageCapitalTools(orig, dest, origPop, destPop, conductance, bulkRetention, dtYears);
        }

        // Route corridor spillovers & separation of Inland Fluvial Freight vs. Maritime Trans-Oceanic Trade
        double routeValue = 12.0 * (1.0 + techLevel * 0.35) / friction * dtYears;
        double marineFactor = 1.0;
        if (orig != null && orig.getBiomassFish() != null) {
            marineFactor = Math.clamp(orig.getBiomassFish() / 500.0, 0.2, 1.0);
        }
        routeValue *= marineFactor;

        if (route.isMaritimeRoute()) {
            TradeNetworkRouter.addMaritimeOceanicVolume(routeValue * conductance);
        } else {
            TradeNetworkRouter.addInlandFluvialVolume(routeValue * conductance);
        }

        int pathLen = Math.max(1, route.pathCells().size());
        double perCellSpillover = routeValue / Math.sqrt(pathLen);

        for (H3Cell cell : route.pathCells()) {
            if (cell == null) continue;
            // Record active trade corridor intensity on the cell tensor for spatial raster telemetry
            double curTradeIntensity = cell.getTradeNetworkOr(0.0);
            cell.setTradeNetwork(Math.clamp(curTradeIntensity * 0.85 + Math.min(1.0, conductance * 2.5), 0.0, 10.0));

            int cellPop = cell.getPopulationOr(0);
            if (cellPop <= 0) continue;

            double curCap = cell.getResourceCapitalOr(0.0);
            double maxMerchantCap = cellPop * 15.0 * Math.max(1.0, techLevel);
            if (curCap < maxMerchantCap) {
                cell.setResourceCapital(Math.min(maxMerchantCap, curCap + perCellSpillover * 0.5));
            }

            double curGini = cell.getGiniIndexOr(0.3);
            cell.setGiniIndex(Math.clamp(curGini + 0.005 * dtYears, 0.15, 0.85));
        }
    }

    private static void arbitrageFood(H3Cell orig, H3Cell dest, double conductance, double foodRetention, double dtYears) {
        double origFood = orig.getFoodResourceOr(0.0);
        double destFood = dest.getFoodResourceOr(0.0);
        double origSurplus = origFood - orig.calculateAnnualMetabolicNeedGJ();
        double destSurplus = destFood - dest.calculateAnnualMetabolicNeedGJ();

        if (origSurplus > 5.0 && destSurplus < -5.0) {
            double exportPotential = Math.min(origSurplus * 0.35, -destSurplus * 0.80) * conductance * dtYears;
            if (exportPotential > 0.0) {
                orig.setFoodResource(Math.max(0.0, origFood - exportPotential));
                dest.setFoodResource(destFood + exportPotential * foodRetention);
                TradeNetworkRouter.addCumulativeTradedFood(exportPotential);
            }
        } else if (destSurplus > 5.0 && origSurplus < -5.0) {
            double exportPotential = Math.min(destSurplus * 0.35, -origSurplus * 0.80) * conductance * dtYears;
            if (exportPotential > 0.0) {
                dest.setFoodResource(Math.max(0.0, destFood - exportPotential));
                orig.setFoodResource(origFood + exportPotential * foodRetention);
                TradeNetworkRouter.addCumulativeTradedFood(exportPotential);
            }
        }
    }

    private static void arbitrageBaseMetals(H3Cell orig, H3Cell dest, double conductance, double bulkRetention, double dtYears) {
        double origMetal = orig.getResourceMetalOr(0.0);
        double destMetal = dest.getResourceMetalOr(0.0);
        if (origMetal > destMetal + 10.0) {
            double metalTrade = Math.min(origMetal * 0.20, (origMetal - destMetal) * 0.25) * conductance * dtYears;
            if (metalTrade > 0.0) {
                orig.setResourceMetal(Math.max(0.0, origMetal - metalTrade));
                dest.setResourceMetal(destMetal + metalTrade * bulkRetention);
                TradeNetworkRouter.addCumulativeTradedMetal(metalTrade);
            }
        } else if (destMetal > origMetal + 10.0) {
            double metalTrade = Math.min(destMetal * 0.20, (destMetal - origMetal) * 0.25) * conductance * dtYears;
            if (metalTrade > 0.0) {
                dest.setResourceMetal(Math.max(0.0, destMetal - metalTrade));
                orig.setResourceMetal(origMetal + metalTrade * bulkRetention);
                TradeNetworkRouter.addCumulativeTradedMetal(metalTrade);
            }
        }
    }

    private static void arbitrageFossilEnergy(H3Cell orig, H3Cell dest, double conductance, double bulkRetention, double dtYears) {
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

    private static void arbitragePreciousSpecie(H3Cell orig, H3Cell dest, double origPrecious, double destPrecious,
                                                double conductance, double dtYears) {
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

    private static void arbitrageRareEarths(H3Cell orig, H3Cell dest, double conductance, double bulkRetention, double dtYears) {
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

    private static void arbitrageCapitalTools(H3Cell orig, H3Cell dest, int origPop, int destPop,
                                              double conductance, double bulkRetention, double dtYears) {
        double origCap = orig.getResourceCapitalOr(0.0);
        double destCap = dest.getResourceCapitalOr(0.0);
        double origCapPerCap = origCap / Math.max(1.0, origPop);
        double destCapPerCap = destCap / Math.max(1.0, destPop);
        if (origCapPerCap > destCapPerCap * 1.3 && origCap > 50.0) {
            double capTrade = Math.min(origCap * 0.15, (origCapPerCap - destCapPerCap) * destPop * 0.20) * conductance * dtYears;
            if (capTrade > 0.0) {
                orig.setResourceCapital(Math.max(0.0, origCap - capTrade));
                dest.setResourceCapital(destCap + capTrade * bulkRetention);
                TradeNetworkRouter.addCumulativeTradedCapital(capTrade);
            }
        } else if (destCapPerCap > origCapPerCap * 1.3 && destCap > 50.0) {
            double capTrade = Math.min(destCap * 0.15, (destCapPerCap - origCapPerCap) * origPop * 0.20) * conductance * dtYears;
            if (capTrade > 0.0) {
                dest.setResourceCapital(Math.max(0.0, destCap - capTrade));
                orig.setResourceCapital(origCap + capTrade * bulkRetention);
                TradeNetworkRouter.addCumulativeTradedCapital(capTrade);
            }
        }
    }

    /**
     * Evaluates topological transit frequencies across trade routes and identifies strategic choke points
     * (straits, mountain passes, isthmuses), allocating transit toll rent into productive capital.
     *
     * @param cellMap                    H3 index to cell lookup map
     * @param routes                     active trade routes
     * @param previouslyScoredChokeCells mutable set tracking previously active choke-point indices
     * @param dtYears                    simulation time step in Julian years
     */
    static void evaluateStrategicChokePoints(Map<Long, H3Cell> cellMap, List<TradeNetworkRouter.TradeRoute> routes,
                                             Set<Long> previouslyScoredChokeCells, double dtYears) {
        if (cellMap == null || cellMap.isEmpty()) return;

        Map<Long, Integer> transitFrequency = new HashMap<>();
        if (routes != null && !routes.isEmpty()) {
            for (TradeNetworkRouter.TradeRoute r : routes) {
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
                double funnelRatio = (double) frequency / (double) totalRoutes;
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
                    int pop = Math.max(1, cell.getPopulationOr(1));
                    double kPerCapita = cell.getResourceCapitalOr(0.0) / pop;
                    // Port, customs house, and canal lock infrastructure capital scales transit toll extraction capacity
                    double portInfrastructureFactor = 1.0 + 0.08 * Math.log1p(kPerCapita / 50.0);
                    double tollRatePerTransit = 1.5 * Math.max(0.5, portInfrastructureFactor);
                    double transitToll = rawScore * frequency * tollRatePerTransit * dtYears;

                    cell.setTransitTollRevenue(cell.getTransitTollRevenueOr(0.0) + transitToll);
                    cell.setResourceCapital(cell.getResourceCapitalOr(0.0) + transitToll);
                }
            } else {
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
