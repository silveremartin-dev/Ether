/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core.metrics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.SimulationMetricsAggregator;
import org.ether.society.model.PhysicalConstants;

import java.util.Objects;

/**
 * <h1>ThermodynamicMetricsCalculator</h1>
 * <p>
 * Calculates macroscopic thermodynamic, bioenergetic, and physical exergy metrics
 * for the planetary civilization engine (Leslie White's law, Vaclav Smil exergy transitions,
 * EROI, trophic multiplier, and Kardashev scale).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ThermodynamicMetricsCalculator {

    private final SimulationMetricsAggregator aggregator;
    private final H3SimulationEngine engine;

    /**
     * Constructs a calculator bound to the given aggregator.
     *
     * @param aggregator parent metrics aggregator, must not be null
     * @param engine     parent simulation engine, must not be null
     */
    public ThermodynamicMetricsCalculator(SimulationMetricsAggregator aggregator, H3SimulationEngine engine) {
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /**
     * Evaluates total power captured by human civilization in Megawatts ($\text{MW}$) according to
     * Vaclav Smil's (2017) exergy transitions and Leslie White's Energy Law:
     * $$P_{\text{cap}} = \sum_i P_{i, \text{watts}} \times N_i / 10^6$$
     * <p>
     * Evaluates directly from first-order physical cellular buffers (power dissipation in Watts/capita
     * and population per cell) in strict compliance with AGENTS.md Â§10.4, eliminating circular
     * dependencies with emergent technological diagnostics.
     * </p>
     *
     * @return total captured power in $\text{MW}$
     */
    public double getEnergyCaptured() {
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0.0;

        org.ether.society.core.dod.WorldBuffer wb = engine.getWorldBuffer();
        if (wb != null) {
            double totalWatts = 0.0;
            float[] popArr = wb.getBiomassHuman();
            int cap = wb.getCapacity();
            for (int i = 0; i < cap; i++) {
                float p = popArr[i];
                if (p > 0.001f) {
                    totalWatts += wb.computeCellPowerWattsPerCapita(i) * p;
                }
            }
            return totalWatts / 1e6;
        }

        java.util.List<org.ether.society.database.H3Cell> cells = engine.getCells();
        if (cells != null && !cells.isEmpty()) {
            double totalWatts = 0.0;
            for (org.ether.society.database.H3Cell c : cells) {
                int p = c.getPopulationOr(0);
                if (p > 0) {
                    totalWatts += (double) p * org.ether.society.engines.tier1.PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(c);
                }
            }
            return totalWatts / 1e6;
        }

        // Direct physical fallback from active capital per capita (Smil 2017; Garrett 2011)
        double kpc = aggregator.getBuiltCapitalTotal() / pop;
        double wattsPerCapita = 385.0 + 80.0 * Math.tanh(kpc / 100.0) + 1200.0 * Math.tanh(kpc / 1500.0);
        return (pop * wattsPerCapita) / 1e6;
    }

    /**
     * Evaluates the global non-renewable resource depletion percentage ($0\%$ to $100\%$).
     *
     * @return resource depletion percentage
     */
    public double getResourceDepletionRate() {
        if (engine.getWorldBuffer() == null || engine.getWorldBuffer().getCapacity() == 0) return 0;
        double maxRes = engine.getWorldBuffer().getCapacity() * 1000.0;
        double currentRes;
        if (engine.getLastCacheTick() == engine.getTickCounter() && engine.getCachedResourceCapital() >= 0) {
            currentRes = engine.getCachedResourceCapital();
        } else {
            currentRes = 0;
            float[] res = engine.getWorldBuffer().getResourceCapital();
            for (int i = 0; i < engine.getWorldBuffer().getCapacity(); i++) currentRes += res[i];
        }
        return Math.max(0, Math.min(100.0, (1.0 - currentRes / Math.max(1, maxRes)) * 100.0));
    }

    /**
     * Evaluates per-capita energy capture in Watts ($\text{J/s}$).
     *
     * @return per-capita power in Watts
     */
    public double getEnergyPerCapita() {
        long pop = engine.getTotalPopulation();
        return pop > 0 ? (getEnergyCaptured() * 1e6) / pop : 300.0;
    }

    /**
     * Evaluates per-capita food reserves in Gigajoules ($\text{GJ/capita}$).
     *
     * @return food stockpile per capita
     */
    public double getFoodPerCapita() {
        long pop = engine.getTotalPopulation();
        return pop > 0 ? engine.getTotalFood() / pop : 0;
    }

    /**
     * Evaluates total domesticated anthropic and livestock biomass.
     *
     * @return domesticated biomass index
     */
    public double getBiomassDomesticated() {
        return engine.getTotalPopulation() * 0.15 + engine.getTotalFood() * 0.4;
    }

    /**
     * Evaluates total accessible potable water across all cells.
     *
     * @return total potable water volume
     */
    public double getPotableWaterTotal() {
        if (engine.getLastCacheTick() == engine.getTickCounter() && engine.getCachedPotableWater() >= 0) {
            return engine.getCachedPotableWater();
        }
        if (engine.getWorldBuffer() == null) return 0;
        double water = 0;
        float[] w = engine.getWorldBuffer().getWaterResource();
        for (int i = 0; i < engine.getWorldBuffer().getCapacity(); i++) water += w[i];
        return water;
    }

    /**
     * Evaluates the remaining planetary resource stock ratio ($0\%$ to $100\%$).
     *
     * @return remaining resources percentage
     */
    public double getRemainingResourcesRatio() {
        return 100.0 - getResourceDepletionRate();
    }

    /**
     * Evaluates systemic Boltzmann-Shannon socio-thermodynamic entropy.
     *
     * @return systemic entropy index in $[0, 1000]$
     */
    public double getSystemicEntropy() {
        float tech = engine.getAverageTechnology();
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0.0;
        return Math.min(1000.0, (Math.log1p(pop) * 25.0) + (tech * 2.5));
    }

    /**
     * Evaluates industrial and anthropogenic pollution level.
     *
     * @return global pollution index
     */
    public double getPollutionLevel() {
        float tech = engine.getAverageTechnology();
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0.0;
        // Continuous logistic industrial emissions response (Smil 2008) with smooth activation
        double industrialIntensity = Math.tanh(Math.max(0.0, tech - 2.5) * 0.8) / (1.0 + Math.exp(-2.5 * (tech - 4.5)));
        return (pop / 100000.0) * industrialIntensity * 50.0;
    }

    /**
     * Evaluates Energy Return on Investment for the food/subsistence system ($\text{EROI}_{\text{alim}} = E_{\text{out}} / E_{\text{in}}$)
     * using an authentic continuous inverse logistic response (Boserup 1965; Hall et al. 2014; Giampietro 2013).
     *
     * @return food system EROI ratio in $[0.12, 7.5]$
     */
    public double getEroiAlimentaire() {
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0.0;
        double kpc = aggregator.getBuiltCapitalTotal() / pop;
        // Continuous biophysical activation of agriculture (hoes/granaries/plows, kpc >= 20 kg/capita)
        double agActivation = Math.tanh(Math.max(0.0, kpc - 20.0) / 30.0);
        if (agActivation <= 0.0) return 0.0;
        // Pre-industrial draft animal traction equilibrium: ~4.2 - 4.8 (Smil 2008, Giampietro et al. 2013)
        double traditionalEquilibrium = 4.2 + 0.6 * Math.tanh(kpc / 350.0);
        // Early Neolithic flood-recession horticulture bonus (~14 at kpc ~ 60-120; Leach 1976, Hall & Klitgaard 2012):
        double neolithicBonus = 9.8 * Math.exp(-Math.pow(kpc - 75.0, 2) / 1500.0);
        // Modern mechanization and Green Revolution expansion (Smil 2017): ~10-15
        double greenRevolutionBoom = 10.2 * org.ether.society.core.math.SmoothMath.sigmoid(kpc, 2500.0, 600.0);
        double eroei = traditionalEquilibrium + neolithicBonus + greenRevolutionBoom;
        return Math.clamp(agActivation * eroei, 0.0, 25.0);
    }

    /**
     * Evaluates net societal energy surplus fraction $\Phi = 1 - 1 / \text{EROI}_{\text{alim}}$ (Tainter 1988, Hall et al. 2014).
     *
     * @return net surplus fraction $\Phi \in [0, 1)$
     */
    public double getNetSurplusFraction() {
        double eroi = getEroiAlimentaire();
        return Math.max(0.0, 1.0 - (1.0 / Math.max(0.1, eroi)));
    }

    /**
     * Evaluates trophic footprint multiplier $\mu = \text{Mobilized Raw Biomass} / \text{Ingested Energy}$
     * as a continuous dietary transition function across technological stages.
     *
     * @return continuous trophic multiplier in $[1.05, 3.20]$
     */
    public double getTrophicMultiplier() {
        float tech = engine.getAverageTechnology();
        double mu = 1.05 + 2.15 / (1.0 + Math.exp(-1.25 * (tech - 2.80)));
        return Math.clamp(mu, 1.05, 3.20);
    }

    /**
     * Evaluates gross raw biomass mobilized per capita per year in $\text{kg/capita/yr}$.
     *
     * @return mobilized biomass per capita in $\text{kg/yr}$
     */
    public double getBiomassMobilizedPerCapitaKg() {
        double trophicMul = getTrophicMultiplier();
        float tech = engine.getAverageTechnology();
        // Continuous caloric density transition from wild game/tubers (17.5 MJ/kg) to domestic grains/fats (14.0 MJ/kg)
        double energyDensity = 17.5 - 3.5 / (1.0 + Math.exp(-1.50 * (tech - 2.20)));
        return (PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_MJ * trophicMul) / energyDensity;
    }

    /**
     * Evaluates Sagan-Kardashev civilizational energy scale index:
     * $$K = \frac{\log_{10}(P_{\text{Watts}}) - 6}{10}$$
     *
     * @return Kardashev scale index in $[0.0, 3.0]$
     */
    public double getKardashevScale() {
        double energyMW = getEnergyCaptured();
        if (energyMW <= 0) return 0.0;
        double watts = energyMW * 1e6;
        long pop = engine.getTotalPopulation();
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeKardashevScale(watts / Math.max(1, pop), pop);
    }

    /**
     * Evaluates total built physical capital stock across the planet directly from first-order
     * physical cellular buffers in strict compliance with AGENTS.md Â§10.4.
     *
     * @return total built capital stock in physical units
     */
    public double getBuiltCapitalTotal() {
        org.ether.society.core.dod.WorldBuffer wb = engine.getWorldBuffer();
        if (wb != null) {
            double total = 0.0;
            float[] capArr = wb.getResourceCapital();
            int cap = wb.getCapacity();
            for (int i = 0; i < cap; i++) {
                total += capArr[i];
            }
            return total;
        }

        java.util.List<org.ether.society.database.H3Cell> cells = engine.getCells();
        if (cells != null && !cells.isEmpty()) {
            double total = 0.0;
            for (org.ether.society.database.H3Cell c : cells) {
                total += c.getResourceCapitalOr(0.0);
            }
            return total;
        }

        return 0.0;
    }

    /**
     * Evaluates total count of active physical tools and machines directly from capital stock per capita.
     *
     * @return tools count
     */
    public long getToolsCount() {
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 0L;
        double kpc = getBuiltCapitalTotal() / pop;
        return (long) (pop * (0.50 + 0.15 * Math.log1p(kpc / 10.0)));
    }

    /**
     * Evaluates Arthur combinatorial diversity of distinct economic products directly from
     * physical capital deepening (Arthur 2009).
     *
     * @return distinct products count
     */
    public long getProductsCount() {
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 3L;
        double kpc = getBuiltCapitalTotal() / pop;
        return (long) Math.max(3, 5.0 + 3.2 * Math.log1p(kpc / 5.0));
    }
}
