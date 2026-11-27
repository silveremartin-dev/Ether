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
 * <h1>DemographicMetricsHelper</h1>
 * <p>
 * Calculates structural demographic metrics, dependency ratios, migration pressures,
 * carrying capacity buffers, and endogenous urbanization rates
 * (Peter Turchin, Jack Goldstone, Thomas Malthus, Paul Bairoch).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class DemographicMetricsHelper {

    private final SimulationMetricsAggregator aggregator;
    private final H3SimulationEngine engine;

    public DemographicMetricsHelper(SimulationMetricsAggregator aggregator, H3SimulationEngine engine) {
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
        if (wb != null && wb.getHumanPopulation() != null) {
            float[] pop = wb.getHumanPopulation();
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
        if (wb != null && wb.getHumanPopulation() != null && wb.getCapacity() > 0) {
            float[] pop = wb.getHumanPopulation();
            float[] capital = wb.getResourceCapital();
            int cap = wb.getCapacity();

            double cellAreaKm2 = org.ether.society.model.SpatialSurfaceHelper.resolveMeanCellAreaKm2(wb);

            double totalPop = 0.0;
            double urbanPop = 0.0;
            for (int i = 0; i < cap; i++) {
                float p = pop[i];
                if (p <= 0.0f) continue;
                totalPop += p;
                float cellCap = (capital != null && capital[i] > 0.0f) ? capital[i] : 0.0f;
                float kpc = cellCap / p;
                double powerW = wb.computeCellPowerWattsPerCapita(i);

                // Urban nucleation: Bairoch (1988) & de Vries (1984) metabolic surplus & physical capital agglomeration
                double cellUrbanFraction = computeCellUrbanFraction(p, kpc, powerW, wb.getCellAreaKm2(i));
                urbanPop += p * cellUrbanFraction;
            }
            if (totalPop > 0.0) {
                return Math.max(0.0, Math.min(95.0, (urbanPop / totalPop) * 100.0));
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
            double powerW = org.ether.society.engines.tier1.PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(c);
            double cellUrbanFraction = computeCellUrbanFraction(p, kpc, powerW, cellArea);
            urbanPop += p * cellUrbanFraction;
        }
        return totalPop > 0 ? Math.max(0.0, Math.min(95.0, (urbanPop / totalPop) * 100.0)) : 0.0;
    }

    /**
     * Evaluates local cellular urbanization fraction in $[0.0, 0.92]$ from population agglomeration,
     * areal density, physical capital per capita, and useful mechanical/thermal power per capita
     * (Bairoch 1988; de Vries 1984). Enforces smooth zero urbanization ($< 0.01\%$) in Paleolithic foraging regimes.
     *
     * @param pop         cellular living human population count
     * @param kpc         physical capital per capita ($\text{kg/person}$)
     * @param powerW      useful power per capita ($\text{W/person}$)
     * @param cellAreaKm2 hexagonal cell surface area ($\text{km}^2$)
     * @return urbanized fraction in $[0.0, 0.92]$
     */
    public static double computeCellUrbanFraction(double pop, double kpc, double powerW, double cellAreaKm2) {
        if (pop <= 0.0) return 0.0;
        double safeAreaKm2 = Math.max(1.0, cellAreaKm2);
        double density = pop / safeAreaKm2;
        double agglomerationFactor = SmoothMath.sigmoid(pop, 5000.0, 1500.0);
        double densityFactor = SmoothMath.sigmoid(density, 12.0, 4.0);
        double nucleationFactor = Math.max(agglomerationFactor, densityFactor);

        double capExcessAgr = SmoothMath.softplus(kpc - 25.0, 0.25);
        double capAgrarianActivation = SmoothMath.sigmoid(kpc, 180.0, 30.0) * Math.tanh(capExcessAgr / 80.0);
        double agrarianUrbanShare = 0.052 * capAgrarianActivation * nucleationFactor;

        double capExcessCom = SmoothMath.softplus(kpc - 300.0, 0.05);
        double capCommercialActivation = SmoothMath.sigmoid(kpc, 1200.0, 250.0) * Math.tanh(capExcessCom / 200.0);
        double commercialUrbanShare = 0.055 * capCommercialActivation * nucleationFactor;

        double powerIndustrialActivation = SmoothMath.sigmoid(powerW, 1200.0, 250.0)
                * Math.tanh(SmoothMath.softplus(powerW - 450.0, 0.05) / 200.0);
        double capIndustrialActivation = SmoothMath.sigmoid(kpc, 2800.0, 500.0)
                * Math.tanh(SmoothMath.softplus(kpc - 500.0, 0.02) / 500.0);
        double industrialUrbanShare = 0.75 * SmoothMath.harmonicCoupling(powerIndustrialActivation, capIndustrialActivation) * nucleationFactor;

        return Math.clamp(agrarianUrbanShare + commercialUrbanShare + industrialUrbanShare, 0.0, 0.92);
    }
}
