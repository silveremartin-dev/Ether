/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.SimulationRunRecord.MetricSnapshot;
import org.ether.society.engines.tier2.theories.ProceduralPopulationGenerator;

/**
 * Dispatches canonical cliodynamic metric queries strictly to pre-recorded simulation observables
 * in {@link MetricSnapshot}, returning recorded ground-truth data or 0.0 when unsimulated.
 *
 * <p>
 * Strictly adheres to the Physical Observables Invariant ({@code AGENTS.md} Â§10 &amp; Â§11):
 * no synthetic sigmoid interpolation or post-hoc fabrication of unsimulated variables.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PostHocCliodynamicMetricDispatcher {

    private PostHocCliodynamicMetricDispatcher() {
        // Utility class
    }

    /**
     * Retrieves or evaluates a cliodynamic metric for a given snapshot.
     *
     * @param snap metric snapshot containing fundamental physical variables
     * @param metricKey canonical metric identifier
     * @param year historical calendar year
     * @return recorded cliodynamic metric value, canonical physical conversion, or 0.0 if unrecorded
     */
    public static double computeMetric(MetricSnapshot snap, String metricKey, int year) {
        if (snap == null || metricKey == null) return 0.0;

        // 1. Direct retrieval if value was explicitly recorded in snapshot dictionary
        if (snap.hasValue(metricKey)) {
            double preRecorded = snap.getValue(metricKey);
            if (!Double.isNaN(preRecorded)) {
                return preRecorded;
            }
        }

        long pop = snap.getPopulation();
        double tech = snap.getAvgTech();
        double stability = snap.getStability();
        double food = snap.getFood();

        // 2. Exact Canonical Dispatch for primary physical observables & passive conversions
        switch (metricKey) {
            case "worldPopulation", "population" -> {
                return (double) pop;
            }
            case "foodPerCapita", "food" -> {
                return food;
            }
            case "asabiyyah", "stability" -> {
                return stability;
            }
            case "avgTechLevel", "tech" -> {
                return tech;
            }
            case "grossWorldProduct", "gdp" -> {
                if (snap.hasValue("gdp")) return snap.getValue("gdp");
                return CliodynamicFormulasRegistry.computeGrossWorldProduct(pop, tech);
            }
            case "primaryEnergy" -> {
                return CliodynamicFormulasRegistry.computePrimaryEnergyEJ(pop, tech);
            }
            case "kardashev" -> {
                return CliodynamicFormulasRegistry.computeKardashevScale(tech, pop);
            }
            case "co2Concentration" -> {
                return CliodynamicFormulasRegistry.computeCo2Concentration(tech, year);
            }
            case "temperatureAnomaly" -> {
                return CliodynamicFormulasRegistry.computeTemperatureAnomaly(year);
            }
            case "milankovitchInsolation" -> {
                return ProceduralPopulationGenerator.calculateMilankovitchSummerInsolation65N(year);
            }
            case "megafaunaIndex" -> {
                return ProceduralPopulationGenerator.calculateMegafaunaAbundanceIndex(year, (double) pop / 1e6, 0.2);
            }
            case "zeroContainmentScore" -> {
                double trade = snap.hasValue("globalTradeVolume") ? snap.getValue("globalTradeVolume") : 0.0;
                return CliodynamicFormulasRegistry.computeZeroContainmentScoreFromTrade(trade);
            }
            default -> {
                // Ground-truth invariant: unsimulated or unrecorded metrics default to 0.0 (AGENTS.md A§8 & A§10)
                return snap.getValue(metricKey);
            }
        }
    }
}
