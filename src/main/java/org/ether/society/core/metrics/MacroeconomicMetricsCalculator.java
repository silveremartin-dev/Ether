/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core.metrics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.SimulationMetricsAggregator;

import java.util.Objects;

/**
 * <h1>MacroeconomicMetricsCalculator</h1>
 * <p>
 * Calculates long-run economic history and macro-cliodynamic indices
 * (Ricardian land rent, currency debasement, real wages, sovereign debt burdens,
 * trade volumes, metal smelting per capita, and shipping freight costs).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class MacroeconomicMetricsCalculator {

    private final SimulationMetricsAggregator aggregator;
    private final H3SimulationEngine engine;

    /**
     * Constructs a calculator bound to the given aggregator.
     *
     * @param aggregator parent metrics aggregator, must not be null
     * @param engine     parent simulation engine, must not be null
     */
    public MacroeconomicMetricsCalculator(SimulationMetricsAggregator aggregator, H3SimulationEngine engine) {
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /**
     * Evaluates Ricardian land rent index driven by demographic density and technology.
     *
     * @return land rent index
     */
    public double getLandRentIndex() {
        long pop = engine.getTotalPopulation();
        long populatedCells = engine.getPopulatedCellCount();
        double cellAreaKm2 = (engine.getCurrentScenario() != null && engine.getCurrentScenario().getCellSizeKm2() > 0)
                ? engine.getCurrentScenario().getCellSizeKm2() : 36.12906216;
        double densityPerKm2 = populatedCells > 0 ? (double) pop / (populatedCells * Math.max(1.0, cellAreaKm2)) : 0.0;
        return Math.min(100.0, densityPerKm2 * 1.2 + engine.getAverageTechnology() * 2.5);
    }

    /**
     * Evaluates silver coinage purity (%) based on metallurgical minting technology and state fiscal stress
     * (Butcher &amp; Ponting 2014) using continuous smooth activation.
     * Enforces Deep-Time Invariant ($0.0\%$) prior to coinage emergence ($T \le 1.80$).
     *
     * @return coinage silver purity in $[0.0, 99.0]$ %
     */
    public double getCurrencyDebasement() {
        if (engine.getCurrentYear() < -600) return 0.0;
        float tech = engine.getAverageTechnology();
        double stability = aggregator.getHappinessIndex();
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeCurrencyDebasement(tech, stability);
    }

    /**
     * Evaluates Robert C. Allen (2001) Real Unskilled Worker Purchasing Power (Welfare Ratio Index, $100 = \text{Subsistence}$)
     * using the single source of truth in PostHocCliodynamicMetricsExtractor.
     *
     * @return real unskilled wage index
     */
    public double getRealUnskilledWageIndex() {
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeRealUnskilledWageIndex(engine.getAverageTechnology());
    }

    /**
     * Evaluates Reinhart &amp; Rogoff (2009) Sovereign Debt Burden (% of GWP)
     * using continuous analytical fiscal state emergence ($T_0 = 1.65$).
     *
     * @return sovereign debt burden in $[0.0, 250.0]$ %
     */
    public double getSovereignDebtBurdenPct() {
        if (engine.getCurrentYear() < -3400) return 0.0;
        double trade = getGlobalTradeVolumeIndex();
        double literacy = aggregator.getLiteracyRate();
        double stability = aggregator.getHappinessIndex();
        float tech = engine.getAverageTechnology();
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeSovereignDebtBurden(
                trade, literacy, stability, tech);
    }

    /**
     * Evaluates Federico &amp; Tena-Junguito (2017) Global Trade Volume Index ($100 = 1913\text{ Level}$)
     * using the single source of truth in PostHocCliodynamicMetricsExtractor.
     *
     * @return global trade volume index
     */
    public double getGlobalTradeVolumeIndex() {
        if (engine.getCurrentYear() < -4000) return 0.0;
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeGlobalTradeVolume(engine.getAverageTechnology());
    }

    /**
     * Evaluates Malandra (1990) / USGS Per-Capita Metal Smelting Output ($\text{kg/capita/yr}$)
     * using the single source of truth in PostHocCliodynamicMetricsExtractor.
     *
     * @return metal smelting output in $\text{kg/capita/yr}$
     */
    public double getMetalSmeltingPerCapitaKg() {
        if (engine.getCurrentYear() < -5000) return 0.0;
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeMetalSmeltingPerCapita(engine.getAverageTechnology());
    }

    /**
     * Evaluates Harley (1988) / Mohammed &amp; Williamson (2004) Real Maritime Shipping Freight Cost Index ($100 = 1913\text{ Level}$)
     * using the single source of truth in PostHocCliodynamicMetricsExtractor.
     *
     * @return real maritime freight cost index
     */
    public double getShippingFreightCostIndex() {
        if (engine.getCurrentYear() < -3000) return 0.0;
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeShippingFreightCostIndex(engine.getAverageTechnology());
    }
}
