/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.analytics.SimulationRunRecord.MetricSnapshot;
import org.ether.society.engines.tier2.theories.ProceduralPopulationGenerator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * <h1>Post-Hoc Cliodynamic Metrics Extractor</h1>
 * <p>
 * Decouples physical simulation execution from cliodynamic academic evaluation.<br>
 * Evaluates macro-historical and cliodynamic indicators (Seshat complexity, EROEI,
 * Turchin structural-demographic stress, Marchetti fossil shares, ORBIS speeds)
 * <b>strictly a posteriori</b> from saved physical snapshots, eliminating all heavy
 * telemetry computation from the headless simulation loop.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PostHocCliodynamicMetricsExtractor {

    private PostHocCliodynamicMetricsExtractor() {
        // Utility static class
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
        return PostHocCliodynamicMetricDispatcher.computeMetric(snap, metricKey, year);
    }

    // --- ANALYTICAL CLOSED-FORM DELEGATION CONTRACTS ---

    /**
     * Calculates atmospheric CO2 concentration in parts per million (ppm).
     *
     * @param tech emergent technological index
     * @param year calendar year CE
     * @return atmospheric CO2 concentration in ppm
     */
    public static double computeCo2Concentration(double tech, int year) {
        return CliodynamicFormulasRegistry.computeCo2Concentration(tech, year);
    }

    /**
     * Calculates global mean surface temperature anomaly relative to 1850-1900 pre-industrial baseline in °C.
     *
     * @param year calendar year CE
     * @return temperature anomaly in degrees Celsius (°C)
     */
    public static double computeTemperatureAnomaly(int year) {
        return CliodynamicFormulasRegistry.computeTemperatureAnomaly(year);
    }

    /**
     * Calculates the zero containment score measuring institutional biosecurity and hazard mitigation.
     *
     * @param year calendar year CE
     * @return normalized containment score [0.0..100.0]
     */
    public static double computeZeroContainmentScore(int year) {
        return CliodynamicFormulasRegistry.computeZeroContainmentScore(year);
    }

    /**
     * Calculates Gross World Product (GWP) in billions of 2011 US Dollars (Maddison Project 2020).
     *
     * @param pop total world population
     * @param tech emergent technological progress index
     * @return Gross World Product in billion USD (constant 2011)
     */
    public static double computeGrossWorldProduct(long pop, double tech) {
        return CliodynamicFormulasRegistry.computeGrossWorldProduct(pop, tech);
    }

    /**
     * Calculates global primary energy consumption in Exajoules per year (EJ/yr).
     *
     * @param pop total world population
     * @param tech emergent technological progress index
     * @return global primary energy consumption in EJ/yr
     */
    public static double computePrimaryEnergyEJ(long pop, double tech) {
        return CliodynamicFormulasRegistry.computePrimaryEnergyEJ(pop, tech);
    }



    /**
     * Calculates silver/gold currency debasement index (Turchin &amp; Nefedov 2009).
     *
     * @param tech emergent technological progress index
     * @param stability institutional stability index [0.0..100.0]
     * @return debasement percentage [0.0..100.0]
     */
    public static double computeCurrencyDebasement(double tech, double stability) {
        return CliodynamicFormulasRegistry.computeCurrencyDebasement(tech, stability);
    }

    /**
     * Calculates Turchin's Structural-Demographic Theory elite overproduction index.
     *
     * @param tech emergent technological progress index
     * @param stability institutional stability index [0.0..100.0]
     * @return elite overproduction index
     */
    public static double computeEliteOverproductionIndex(double tech, double stability) {
        return CliodynamicFormulasRegistry.computeEliteOverproductionIndex(tech, stability);
    }

    /**
     * Calculates the real unskilled wage index (Allen 2001, Clark 2007).
     *
     * @param tech emergent technological progress index
     * @return real wage index (1900 baseline = 100.0)
     */
    public static double computeRealUnskilledWageIndex(double tech) {
        return CliodynamicFormulasRegistry.computeRealUnskilledWageIndex(tech);
    }

    /**
     * Calculates Turchin's Political Stress Index (PSI) combining immiseration, elite competition, and fiscal distress.
     *
     * @param tech emergent technological progress index
     * @param stability institutional stability index [0.0..100.0]
     * @return political stress index
     */
    public static double computePoliticalStressIndex(double tech, double stability) {
        return CliodynamicFormulasRegistry.computePoliticalStressIndex(tech, stability);
    }

    /**
     * Calculates Ibn Khaldun's Asabiyyah (social cohesion / solidarity) index.
     *
     * @param tech emergent technological progress index
     * @param stability institutional stability index [0.0..100.0]
     * @return social cohesion index [0.0..100.0]
     */
    public static double computeAsabiyyahSocialCohesion(double tech, double stability) {
        return CliodynamicFormulasRegistry.computeAsabiyyahSocialCohesion(tech, stability);
    }

    /**
     * Calculates composite sociopolitical instability index (Turchin 2016 Ages of Discord).
     *
     * @param tech emergent technological progress index
     * @param stability institutional stability index [0.0..100.0]
     * @return sociopolitical instability index
     */
    public static double computeSociopoliticalInstability(double tech, double stability) {
        return CliodynamicFormulasRegistry.computeSociopoliticalInstability(tech, stability);
    }



    /**
     * Calculates multi-variable coupled sovereign debt burden based on fiscal apparatus, trade volume, and literacy.
     *
     * @param tradeVolume global trade volume index
     * @param literacy adult literacy rate in percent
     * @param stability institutional stability index
     * @param tech emergent technological progress index
     * @return sovereign debt percentage (% of GWP)
     */
    public static double computeSovereignDebtBurden(double tradeVolume, double literacy, double stability, double tech) {
        return CliodynamicFormulasRegistry.computeSovereignDebtBurden(tradeVolume, literacy, stability, tech);
    }

    /**
     * Calculates global commercial trade volume index (Findlay &amp; O'Rourke 2007).
     *
     * @param tech emergent technological progress index
     * @return trade volume index
     */
    public static double computeGlobalTradeVolume(double tech) {
        return CliodynamicFormulasRegistry.computeGlobalTradeVolume(tech);
    }

    /**
     * Calculates annual agricultural topsoil erosion loss (Montgomery 2007).
     *
     * @param tech emergent technological progress index
     * @return soil erosion loss in metric tons per hectare per year (t/ha/yr)
     */
    public static double computeTopsoilErosionLoss(double tech) {
        return CliodynamicFormulasRegistry.computeTopsoilErosionLoss(tech);
    }

    /**
     * Calculates agricultural Energy Return on Energy Invested (EROEI, Hall et al. 2014).
     *
     * @param tech emergent technological progress index
     * @return dimensionless EROEI ratio
     */
    public static double computeAgriculturalEroei(double tech) {
        return CliodynamicFormulasRegistry.computeAgriculturalEroei(tech);
    }

    /**
     * Calculates remaining global virgin forest cover fraction relative to prehistoric climax baseline.
     *
     * @param tech emergent technological progress index
     * @return remaining forest cover percentage (%)
     */
    public static double computeForestCoverRemaining(double tech) {
        return CliodynamicFormulasRegistry.computeForestCoverRemaining(tech);
    }

    /**
     * Calculates maximum long-distance information propagation velocity (ORBIS, Scheidel 2014).
     *
     * @param tech emergent technological progress index
     * @return travel speed in kilometers per day (km/day)
     */
    public static double computeInformationTravelSpeed(double tech) {
        return CliodynamicFormulasRegistry.computeInformationTravelSpeed(tech);
    }

    /**
     * Calculates extractive metal smelting output per capita (copper, bronze, iron, steel, aluminum).
     *
     * @param tech emergent technological progress index
     * @return annual smelted metal in kilograms per capita per year (kg/cap/yr)
     */
    public static double computeMetalSmeltingPerCapita(double tech) {
        return CliodynamicFormulasRegistry.computeMetalSmeltingPerCapita(tech);
    }

    /**
     * Calculates fossil energy fraction of total primary energy supply (Marchetti &amp; Nakicenovic 1979).
     *
     * @param tech emergent technological progress index
     * @return fossil energy share in percent (%)
     */
    public static double computeFossilSharePrimaryEnergy(double tech) {
        return CliodynamicFormulasRegistry.computeFossilSharePrimaryEnergy(tech);
    }

    /**
     * Calculates international maritime shipping freight cost index (Harley 1988, Mohammed &amp; Williamson 2004).
     *
     * @param tech emergent technological progress index
     * @return freight cost index (1913 baseline = 100.0)
     */
    public static double computeShippingFreightCostIndex(double tech) {
        return CliodynamicFormulasRegistry.computeShippingFreightCostIndex(tech);
    }

    /**
     * Calculates cohort life expectancy at birth based on Preston (1975) epidemiological curve.
     *
     * @param tech emergent technological progress index
     * @param food available food energy stock in Gcal
     * @return life expectancy in years (yr)
     */
    public static double computeLifeExpectancy(double tech, double food) {
        return CliodynamicFormulasRegistry.computeLifeExpectancy(tech, food);
    }

    /**
     * Calculates planetary Kardashev civilizational energy scale (Kardashev 1964, Sagan 1973).
     *
     * @param powerWattsPerCapita continuous power dissipation per capita in Watts
     * @param population global conscious living agent population count
     * @return continuous Kardashev scale index
     */
    public static double computeKardashevScale(double powerWattsPerCapita, long population) {
        return CliodynamicFormulasRegistry.computeKardashevScale(powerWattsPerCapita, population);
    }

    /**
     * Computes the dimensionless emergent technological index from physical observables.
     *
     * @param powerWattsPerCapita continuous power dissipation per capita in Watts
     * @param capitalPerCapita gross physical built capital stock per capita
     * @return emergent technological index bounded in [0.0..10.0]
     */
    public static double computeEmergentTechnologyLevel(double powerWattsPerCapita, double capitalPerCapita) {
        return CliodynamicFormulasRegistry.computeEmergentTechnologyLevel(powerWattsPerCapita, capitalPerCapita);
    }
}
