/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.SimulationRunRecord.MetricSnapshot;
import org.ether.society.engines.tier2.theories.ProceduralPopulationEngine;

/**
 * Dispatches canonical and fallback substring cliodynamic metric queries
 * to closed-form analytical formulas in {@link CliodynamicAnalyticalFormulasCatalog}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class PostHocCliodynamicMetricDispatcher {

    private PostHocCliodynamicMetricDispatcher() {
        // Utility class
    }

    /**
     * Evaluates a single cliodynamic metric a posteriori for a given snapshot.
     *
     * @param snap metric snapshot containing fundamental physical variables
     * @param metricKey canonical metric identifier
     * @param year historical calendar year
     * @return evaluated cliodynamic metric value
     */
    public static double computeMetric(MetricSnapshot snap, String metricKey, int year) {
        if (snap == null || metricKey == null) return 0.0;

        // Check if value was already explicitly recorded (e.g. in live GUI mode)
        double preRecorded = snap.getValue(metricKey);
        if (preRecorded != 0.0 && !Double.isNaN(preRecorded)) {
            return preRecorded;
        }

        long pop = snap.getPopulation();
        double tech = snap.getAvgTech();
        double stability = snap.getStability();
        double food = snap.getFood();

        // 1. Exact Canonical Metric Dispatch (Eliminates greedy substring collisions)
        switch (metricKey) {
            case "worldPopulation", "population" -> {
                return (double) pop;
            }
            case "grossWorldProduct" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeGrossWorldProduct(pop, tech);
            }
            case "primaryEnergy" -> {
                return CliodynamicAnalyticalFormulasCatalog.computePrimaryEnergyEJ(pop, tech);
            }
            case "urbanizationRate" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeUrbanizationRate(tech);
            }
            case "literacyRate" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeLiteracyRate(tech);
            }
            case "currencyDebasement" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeCurrencyDebasement(tech, stability);
            }
            case "eliteOverproductionIndex" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeEliteOverproductionIndex(tech, stability);
            }
            case "realUnskilledWage" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeRealUnskilledWageIndex(tech);
            }
            case "politicalStressIndex" -> {
                return CliodynamicAnalyticalFormulasCatalog.computePoliticalStressIndex(tech, stability);
            }
            case "asabiyyahSocialCohesion" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeAsabiyyahSocialCohesion(tech, stability);
            }
            case "sociopoliticalInstability" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeSociopoliticalInstability(tech, stability);
            }
            case "giniInequality" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeGiniInequality(tech);
            }
            case "sovereignDebtBurden" -> {
                double trade = CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(tech);
                double lit = CliodynamicAnalyticalFormulasCatalog.computeLiteracyRate(tech);
                return CliodynamicAnalyticalFormulasCatalog.computeSovereignDebtBurden(trade, lit, stability, tech);
            }
            case "globalTradeVolume" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(tech);
            }
            case "soilErosionRate" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeTopsoilErosionLoss(tech);
            }
            case "agriculturalEroei" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeAgriculturalEroei(tech);
            }
            case "deforestationRate" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeForestCoverRemaining(tech);
            }
            case "informationSpeed" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeInformationTravelSpeed(tech);
            }
            case "megafaunaIndex" -> {
                return ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(year, (double) pop / 1e6, 0.2);
            }
            case "milankovitchInsolation" -> {
                return ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(year);
            }
            case "zeroContainmentScore" -> {
                double trade = CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(tech);
                return CliodynamicAnalyticalFormulasCatalog.computeZeroContainmentScoreFromTrade(trade);
            }
            case "lifeExpectancyAtBirth" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeLifeExpectancy(tech, food);
            }
            case "metalSmeltingPerCapita" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeMetalSmeltingPerCapita(tech);
            }
            case "fossilSharePrimaryEnergy" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeFossilSharePrimaryEnergy(tech);
            }
            case "shippingFreightCostIndex" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeShippingFreightCostIndex(tech);
            }
            case "co2Concentration" -> {
                return CliodynamicAnalyticalFormulasCatalog.computeCo2Concentration(tech, year);
            }
            case "temperatureAnomaly" -> {
                double pre = snap.getValue("temperatureAnomaly");
                if (pre != 0.0 && !Double.isNaN(pre)) return pre;
                return CliodynamicAnalyticalFormulasCatalog.computeTemperatureAnomaly(year);
            }
        }

        String lower = metricKey.toLowerCase();

        // 2. Unambiguous Substring Matching Fallback (Order-safe for composite labels)
        if (lower.contains("elite") || lower.contains("overproduction")) {
            return CliodynamicAnalyticalFormulasCatalog.computeEliteOverproductionIndex(tech, stability);
        }
        if (lower.contains("fossil")) {
            return CliodynamicAnalyticalFormulasCatalog.computeFossilSharePrimaryEnergy(tech);
        }
        if (lower.equals("population") || lower.equals("worldpopulation")) {
            return (double) pop;
        }
        if (lower.contains("grossworldproduct") || lower.equals("gdp") || lower.equals("gwp") || lower.equals("product")) {
            return CliodynamicAnalyticalFormulasCatalog.computeGrossWorldProduct(pop, tech);
        }
        if (lower.contains("primaryenergy") || lower.equals("energy") || lower.equals("primary_energy")) {
            return CliodynamicAnalyticalFormulasCatalog.computePrimaryEnergyEJ(pop, tech);
        }

        // Urbanization & Literacy
        if (lower.contains("urbanization")) {
            return CliodynamicAnalyticalFormulasCatalog.computeUrbanizationRate(tech);
        }
        if (lower.contains("literacy")) {
            return CliodynamicAnalyticalFormulasCatalog.computeLiteracyRate(tech);
        }

        // Institutional & Turchin SDT Metrics
        if (lower.contains("currency") || lower.contains("debasement")) {
            return CliodynamicAnalyticalFormulasCatalog.computeCurrencyDebasement(tech, stability);
        }
        if (lower.contains("wage") || lower.contains("salaire")) {
            return CliodynamicAnalyticalFormulasCatalog.computeRealUnskilledWageIndex(tech);
        }
        if (lower.contains("politicalstress") || lower.contains("psi")) {
            return CliodynamicAnalyticalFormulasCatalog.computePoliticalStressIndex(tech, stability);
        }
        if (lower.contains("asabiyyah") || lower.contains("cohesion")) {
            return CliodynamicAnalyticalFormulasCatalog.computeAsabiyyahSocialCohesion(tech, stability);
        }
        if (lower.contains("instability") || lower.contains("conflict")) {
            return CliodynamicAnalyticalFormulasCatalog.computeSociopoliticalInstability(tech, stability);
        }
        if (lower.contains("gini")) {
            return CliodynamicAnalyticalFormulasCatalog.computeGiniInequality(tech);
        }
        if (lower.contains("sovereigndebt") || lower.contains("debt")) {
            return CliodynamicAnalyticalFormulasCatalog.computeSovereignDebtBurden(tech);
        }

        // Trade, Transport & Environmental Energetics
        if (lower.contains("trade") || lower.contains("commerce")) {
            return CliodynamicAnalyticalFormulasCatalog.computeGlobalTradeVolume(tech);
        }
        if (lower.contains("soil") || lower.contains("erosion")) {
            return CliodynamicAnalyticalFormulasCatalog.computeTopsoilErosionLoss(tech);
        }
        if (lower.contains("eroei") || lower.contains("eroi")) {
            return CliodynamicAnalyticalFormulasCatalog.computeAgriculturalEroei(tech);
        }
        if (lower.contains("deforest") || lower.contains("forest")) {
            return CliodynamicAnalyticalFormulasCatalog.computeForestCoverRemaining(tech);
        }
        if (lower.contains("speed") || lower.contains("vitesse")) {
            return CliodynamicAnalyticalFormulasCatalog.computeInformationTravelSpeed(tech);
        }
        if (lower.contains("smelt") || lower.contains("metal")) {
            return CliodynamicAnalyticalFormulasCatalog.computeMetalSmeltingPerCapita(tech);
        }
        if (lower.contains("freight") || lower.contains("shipping")) {
            return CliodynamicAnalyticalFormulasCatalog.computeShippingFreightCostIndex(tech);
        }
        if (lower.contains("life") || lower.contains("expectancy")) {
            return CliodynamicAnalyticalFormulasCatalog.computeLifeExpectancy(tech, food);
        }

        // Paleolithic & Earth-System Cycles
        if (lower.contains("megafauna")) {
            return ProceduralPopulationEngine.calculateMegafaunaAbundanceIndex(year, (double) pop / 1e6, 0.2);
        }
        if (lower.contains("milankovitch")) {
            return ProceduralPopulationEngine.calculateMilankovitchSummerInsolation65N(year);
        }
        if (lower.contains("containment")) {
            return CliodynamicAnalyticalFormulasCatalog.computeZeroContainmentScore(year);
        }
        if (lower.contains("co2")) {
            return CliodynamicAnalyticalFormulasCatalog.computeCo2Concentration(tech, year);
        }
        if (lower.contains("temperature") || lower.contains("anom")) {
            return 0.0;
        }

        return 0.0;
    }
}
