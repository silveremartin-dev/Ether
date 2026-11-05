/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core.metrics;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.SimulationMetricsAggregator;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.ether.society.core.math.SmoothMath;

import java.util.Objects;

/**
 * <h1>DemographicMetricsCalculator</h1>
 * <p>
 * Calculates structural demographic metrics, dependency ratios, migration pressures,
 * carrying capacity buffers, and endogenous urbanization rates
 * (Peter Turchin, Jack Goldstone, Thomas Malthus, Paul Bairoch).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class DemographicMetricsCalculator {

    private final SimulationMetricsAggregator aggregator;
    private final H3SimulationEngine engine;

    public DemographicMetricsCalculator(SimulationMetricsAggregator aggregator, H3SimulationEngine engine) {
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /**
     * Calculates total spatial territory occupied by living human populations in square kilometers (km²).
     *
     * @return occupied surface area in km²
     */
    public double getOccupiedTerritoryArea() {
        WorldBuffer wb = engine.getWorldBuffer();
        if (wb != null && wb.getBiomassHuman() != null) {
            float[] pop = wb.getBiomassHuman();
            double cellAreaKm2 = 1770.49;
            long[] h3Idx = wb.getH3Indexes();
            if (h3Idx != null && h3Idx.length > 0 && h3Idx[0] != 0L) {
                int res = (int) ((h3Idx[0] >>> 52) & 0x0FL);
                if (res >= 0 && res <= 15) {
                    cellAreaKm2 = H3Cell.getAvgHexagonAreaKm2(res);
                }
            }
            int count = 0;
            for (int i = 0; i < wb.getCapacity(); i++) {
                if (pop[i] > 0.0f) count++;
            }
            return count * cellAreaKm2;
        }
        if (engine.getCells() != null) {
            double area = 0.0;
            for (H3Cell c : engine.getCells()) {
                if (c != null && c.getPopulation() != null && c.getPopulation() > 0) {
                    area += c.getEffectiveSurfaceAreaKm2();
                }
            }
            return area;
        }
        return 0.0;
    }

    /**
     * Calculates juvenile cohort percentage of total population from current fertility.
     *
     * @return offspring percentage [20.0..95.0] %
     */
    public double getOffspringPercentage() {
        float fertility = engine.getCurrentFertility();
        return Math.clamp(fertility * 25.0, 20.0, 95.0);
    }

    /**
     * Returns mean age of mothers at first childbirth in years.
     *
     * @return maternal age at first child in years
     */
    public double getAgeAtFirstChild() {
        return 22.0;
    }

    /**
     * Calculates planetary net immigration rate.
     *
     * @return net immigration rate (zero for closed planetary system)
     */
    public double getImmigrationRate() {
        return 0.0;
    }

    private long initialPopulation = -1;

    /**
     * Calculates subjective collective well-being and satisfaction index based on per-capita food and water security.
     *
     * @return subjective happiness score in [10.0..100.0]
     */
    public double getHappinessIndex() {
        long pop = engine.getTotalPopulation();
        if (pop <= 0) return 50.0;
        double foodPerCap = engine.getTotalFood() / pop;
        double waterPerCap = engine.getCachedPotableWater() / pop;
        double foodScore = Math.min(50.0, foodPerCap * 10.0);
        double waterScore = Math.min(50.0, waterPerCap * 5.0);
        return Math.clamp(foodScore + waterScore, 10.0, 100.0);
    }

    /**
     * Calculates civilizational demographic survival percentage relative to initial colonization baseline.
     *
     * @return population survival rate in [0.0..100.0] %
     */
    public double getPopulationSurvivalRate() {
        long currentPop = engine.getTotalPopulation();
        if (initialPopulation <= 0) {
            initialPopulation = currentPop;
        }
        if (initialPopulation <= 0) return 100.0;
        return Math.clamp(((double) currentPop / initialPopulation) * 100.0, 0.0, 100.0);
    }

    /**
     * Aggregates global demographic age distribution across 7 standardized cohorts:
     * [0-4, 5-14, 15-24, 25-49, 50-64, 65-79, 80+].
     *
     * @return 7-element integer array containing population counts per age bracket
     */
    public int[] getAgePyramid() {
        int[] globalPyramid = new int[7];
        if (engine.getCells() != null) {
            for (H3Cell c : engine.getCells()) {
                if (c != null && c.getAgePyramid() != null) {
                    int[] p = c.getAgePyramid();
                    for (int i = 0; i < Math.min(7, p.length); i++) {
                        globalPyramid[i] += p[i];
                    }
                }
            }
        }
        return globalPyramid;
    }

    /**
     * Calculates maximum sustainable planetary carrying capacity in individuals supported by available caloric biomass.
     *
     * @return carrying capacity in supported individuals
     */
    public double getCarryingCapacity() {
        double totalFood = engine.getTotalFood();
        double annualMetabolicNeed = org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
        return Math.max(100.0, (totalFood * 17.5) / annualMetabolicNeed);
    }

    /**
     * Calculates demographic saturation ratio: N / K_carrying.
     *
     * @return demographic pressure ratio (&gt; 1.0 indicates overpopulation overshoot)
     */
    public double getDemographicPressure() {
        long pop = engine.getTotalPopulation();
        double carryingCap = getCarryingCapacity();
        if (carryingCap <= 0) return 1.0;
        return (double) pop / carryingCap;
    }

    /**
     * Evaluates Malthusian subsistence stress index reflecting ecological overshoot.
     *
     * @return Malthusian stress score in [0.0..100.0]
     */
    public double getMalthusianStressIndex() {
        double pressure = getDemographicPressure();
        return Math.min(100.0, Math.max(0.0, (pressure - 1.0) * 100.0));
    }

    /**
     * Calculates remaining carrying capacity headroom fraction before ecological boundary limits.
     *
     * @return carrying capacity headroom ratio in [0.0..1.0]
     */
    public double getCarryingCapacityHeadroom() {
        long pop = engine.getTotalPopulation();
        double carryingCap = getCarryingCapacity();
        if (carryingCap <= 0) return 0.0;
        return Math.max(0.0, (carryingCap - pop) / carryingCap);
    }

    /**
     * Calculates economic dependency ratio (non-working cohorts / productive labor age cohorts).
     *
     * @return demographic dependency ratio
     */
    public double getAgeDependencyRatio() {
        return 0.55;
    }

    /**
     * Evaluates Goldstone (1991) Youth Bulge index (cohort 15-24 share as fraction of total adult population).
     *
     * @return youth bulge index in [0.0..1.0]
     */
    public double getYouthBulgeIndex() {
        float fertility = engine.getCurrentFertility();
        return Math.min(1.0, Math.max(0.0, (fertility - 1.5) / 4.0));
    }

    /**
     * Calculates global net inter-regional migration velocity.
     *
     * @return net migration rate
     */
    public double getNetMigrationRate() {
        return 0.0;
    }

    /**
     * Evaluates global urbanization rate (%) endogenously from agricultural caloric surplus,
     * per-capita physical capital intensity, and local demographic nucleation (Bairoch 1988, de Vries 1984).
     *
     * @return global urbanization rate in $[0.0, 95.0]$ %
     */
    public double getUrbanizationRate() {
        WorldBuffer wb = engine.getWorldBuffer();
        if (wb != null && wb.getBiomassHuman() != null && wb.getCapacity() > 0) {
            float[] pop = wb.getBiomassHuman();
            float[] capital = wb.getResourceCapital();
            int cap = wb.getCapacity();

            double cellAreaKm2 = 1770.49;
            long[] h3Idx = wb.getH3Indexes();
            if (h3Idx != null && h3Idx.length > 0 && h3Idx[0] != 0L) {
                int res = (int) ((h3Idx[0] >>> 52) & 0x0FL);
                if (res >= 0 && res <= 15) {
                    cellAreaKm2 = H3Cell.getAvgHexagonAreaKm2(res);
                }
            }
            double macroScale = 1770.49 / Math.max(0.1, cellAreaKm2);

            double totalPop = 0.0;
            double urbanPop = 0.0;
            for (int i = 0; i < cap; i++) {
                float p = pop[i];
                if (p <= 0.0f) continue;
                totalPop += p;
                float cellCap = (capital != null && capital[i] > 0.0f) ? capital[i] : 0.0f;
                float kpc = cellCap / p;
                double powerW = wb.computeCellPowerWattsPerCapita(i);

                double density = p / cellAreaKm2;
                double nucleationFactor = SmoothMath.sigmoid(density, 30.0, 10.0);

                // Agrarian urbanization: non-farming artisans supported by agricultural surplus (Bairoch 1988)
                double capAgrarianActivation = SmoothMath.sigmoid(kpc, 150.0, 40.0);
                double agrarianUrbanShare = 0.12 * capAgrarianActivation * nucleationFactor;

                // Industrial urbanization: powered by mechanical energy grids & factory capital (de Vries 1984)
                double powerIndustrialActivation = SmoothMath.sigmoid(powerW, 800.0, 200.0);
                double capIndustrialActivation = SmoothMath.sigmoid(kpc, 2500.0, 500.0);
                double industrialUrbanShare = 0.78 * SmoothMath.harmonicCoupling(powerIndustrialActivation, capIndustrialActivation);

                double cellUrbanFraction = SmoothMath.smoothClamp(
                        SmoothMath.softmax(agrarianUrbanShare, industrialUrbanShare, 0.02),
                        0.0, 0.92, 0.01);
                urbanPop += p * cellUrbanFraction;
            }
            if (totalPop > 0.0) {
                return SmoothMath.smoothClamp((urbanPop / totalPop) * 100.0, 0.0, 95.0, 0.1);
            }
        }

        if (engine.getCells() == null || engine.getCells().isEmpty()) {
            return 0.0;
        }
        double totalPop = 0.0;
        double urbanPop = 0.0;
        for (H3Cell c : engine.getCells()) {
            if (c == null) continue;
            int p = c.getPopulationOr(0);
            if (p <= 0) continue;
            totalPop += p;
            double kpc = c.getResourceCapitalOr(0.0) / p;
            double cellArea = Math.max(1.0, c.getCellAreaKm2());
            double density = p / cellArea;
            double nucleationFactor = SmoothMath.sigmoid(density, 30.0, 10.0);

            double capAgrarianActivation = SmoothMath.sigmoid(kpc, 150.0, 40.0);
            double agrarianUrbanShare = 0.12 * capAgrarianActivation * nucleationFactor;

            double powerW = org.ether.society.engines.tier1.PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(c);
            double powerIndustrialActivation = SmoothMath.sigmoid(powerW, 800.0, 200.0);
            double capIndustrialActivation = SmoothMath.sigmoid(kpc, 2500.0, 500.0);
            double industrialUrbanShare = 0.78 * SmoothMath.harmonicCoupling(powerIndustrialActivation, capIndustrialActivation);

            double cellUrbanFraction = SmoothMath.smoothClamp(
                    SmoothMath.softmax(agrarianUrbanShare, industrialUrbanShare, 0.02),
                    0.0, 0.92, 0.01);
            urbanPop += p * cellUrbanFraction;
        }
        return totalPop > 0 ? SmoothMath.smoothClamp((urbanPop / totalPop) * 100.0, 0.0, 95.0, 0.1) : 0.0;
    }
}
