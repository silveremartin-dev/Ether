/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core.metrics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.SimulationMetricsAggregator;
import org.ether.society.database.H3Cell;

import java.util.Objects;

/**
 * <h1>MacroeconomicMetricsHelper</h1>
 * <p>
 * Calculates long-run economic history and macro-cliodynamic indices
 * (Ricardian land rent, currency debasement, real wages, sovereign debt burdens,
 * trade volumes, metal smelting per capita, and shipping freight costs).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class MacroeconomicMetricsHelper {

    private final SimulationMetricsAggregator aggregator;
    private final H3SimulationEngine engine;

    /**
     * Constructs a calculator bound to the given aggregator.
     *
     * @param aggregator parent metrics aggregator, must not be null
     * @param engine     parent simulation engine, must not be null
     */
    public MacroeconomicMetricsHelper(SimulationMetricsAggregator aggregator, H3SimulationEngine engine) {
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /**
     * Evaluates the Garrett-Lotka Exergy-Capital Index $\Phi_{\text{exergy}}(p, k_{\text{pc}})$ directly
     * from physical cellular power dissipation $p = P_{\text{watts}}/N$ and built capital $k_{\text{pc}} = K/N$.
     */
    private double computePhysicalExergyCapitalIndex() {
        long pop = Math.max(1L, engine.getTotalPopulation());
        double kpc = aggregator.getBuiltCapitalTotal() / pop;
        double powerW = aggregator.getEnergyPerCapita();
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeEmergentTechnologyLevel(powerW, kpc);
    }

    /**
     * Evaluates Ricardian land rent index (Ricardo 1817; Von Thünen 1826) driven by demographic density
     * and physical infrastructure capital per capita $k_{\text{pc}} = K/N$.
     *
     * @return land rent index
     */
    public double getLandRentIndex() {
        long pop = engine.getTotalPopulation();
        long populatedCells = engine.getPopulatedCellCount();
        double cellAreaKm2 = (engine.getCurrentScenario() != null && engine.getCurrentScenario().getCellSizeKm2() > 0)
                ? engine.getCurrentScenario().getCellSizeKm2() : 36.12906216;
        double densityPerKm2 = populatedCells > 0 ? (double) pop / (populatedCells * Math.max(1.0, cellAreaKm2)) : 0.0;
        double kpc = pop > 0 ? aggregator.getBuiltCapitalTotal() / pop : 0.0;
        return Math.min(100.0, densityPerKm2 * 1.2 + 2.5 * Math.log1p(kpc / 20.0));
    }

    /**
     * Evaluates silver coinage purity (%) based on metallurgical minting capital and state fiscal stress
     * (Butcher &amp; Ponting 2014) using continuous smooth activation.
     *
     * @return coinage silver purity in $[0.0, 99.0]$ %
     */
    public double getCurrencyDebasement() {
        double stability = aggregator.getHappinessIndex();
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeCurrencyDebasement(
                computePhysicalExergyCapitalIndex(), stability);
    }

    /**
     * Evaluates Robert C. Allen (2001) Real Unskilled Worker Purchasing Power (Welfare Ratio Index, $100 = \text{Subsistence}$).
     *
     * @return real unskilled wage index
     */
    public double getRealUnskilledWageIndex() {
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeRealUnskilledWageIndex(
                computePhysicalExergyCapitalIndex());
    }

    /**
     * Evaluates Reinhart &amp; Rogoff (2009) Sovereign Debt Burden (% of GWP).
     *
     * @return sovereign debt burden in $[0.0, 250.0]$ %
     */
    public double getSovereignDebtBurdenPct() {
        double trade = getGlobalTradeVolumeIndex();
        double literacy = aggregator.getLiteracyRate();
        double stability = aggregator.getHappinessIndex();
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeSovereignDebtBurden(
                trade, literacy, stability, computePhysicalExergyCapitalIndex());
    }

    /**
     * Evaluates Federico &amp; Tena-Junguito (2017) Global Trade Volume Index ($100 = 1913\text{ Level}$).
     * Couples the maritime shipping conductance baseline with active oceanic trade corridors from
     * {@link org.ether.society.engines.tier2.theories.TradeNetworkRouter} so that domestic inland fluvial
     * freight (e.g. Yangtze / Grand Canal) does not inflate international trans-oceanic trade.
     *
     * @return global trade volume index
     */
    public double getGlobalTradeVolumeIndex() {
        double baseMaritimeVolume = org.ether.society.analytics.CliodynamicFormulasRegistry.computeGlobalTradeVolume(
                computePhysicalExergyCapitalIndex());
        double activeOceanicCorridors = org.ether.society.engines.tier2.theories.TradeNetworkRouter.getLatestMaritimeOceanicVolume();
        if (activeOceanicCorridors > 0.0) {
            double corridorBonus = 0.05 * Math.log1p(activeOceanicCorridors / 1000.0);
            return Math.clamp(baseMaritimeVolume * (1.0 + Math.min(0.25, corridorBonus)), 0.0, 3500.0);
        }
        return baseMaritimeVolume;
    }

    /**
     * Evaluates domestic inland fluvial and canal freight transport volume index, combining
     * physical inland waterway capacity with active continental/fluvial corridors from
     * {@link org.ether.society.engines.tier2.theories.TradeNetworkRouter}.
     *
     * @return inland fluvial freight index
     */
    public double getInlandFluvialFreightVolume() {
        double baseFluvialVolume = org.ether.society.analytics.CliodynamicFormulasRegistry.computeInlandFluvialFreightVolume(
                computePhysicalExergyCapitalIndex());
        double activeFluvialCorridors = org.ether.society.engines.tier2.theories.TradeNetworkRouter.getLatestInlandFluvialVolume();
        if (activeFluvialCorridors > 0.0) {
            return Math.clamp(baseFluvialVolume + 0.10 * Math.log1p(activeFluvialCorridors), 0.0, 5000.0);
        }
        return baseFluvialVolume;
    }

    /**
     * Evaluates Malandra (1990) / USGS Per-Capita Metal Smelting Output ($\text{kg/capita/yr}$).
     *
     * @return metal smelting output in $\text{kg/capita/yr}$
     */
    public double getMetalSmeltingPerCapitaKg() {
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeMetalSmeltingPerCapita(
                computePhysicalExergyCapitalIndex());
    }

    /**
     * Evaluates Harley (1988) / Mohammed &amp; Williamson (2004) Real Maritime Shipping Freight Cost Index ($100 = 1913\text{ Level}$).
     *
     * @return real maritime freight cost index
     */
    public double getShippingFreightCostIndex() {
        return org.ether.society.analytics.CliodynamicFormulasRegistry.computeShippingFreightCostIndex(
                computePhysicalExergyCapitalIndex());
    }
}
