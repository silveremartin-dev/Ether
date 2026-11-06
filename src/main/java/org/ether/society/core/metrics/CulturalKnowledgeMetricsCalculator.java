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
 * <h1>CulturalKnowledgeMetricsCalculator</h1>
 * <p>
 * Calculates cognitive complexity, literacy transitions, collective memory stocks,
 * Shannon information bandwidth, systemic interdependence, and post-shock reconstruction capabilities
 * (Jack Goody, Joseph Henrich, W. Brian Arthur, Peter Turchin).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class CulturalKnowledgeMetricsCalculator {

    private final SimulationMetricsAggregator aggregator;
    private final H3SimulationEngine engine;

    public CulturalKnowledgeMetricsCalculator(SimulationMetricsAggregator aggregator, H3SimulationEngine engine) {
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /**
     * Retrieves the adult educational attainment level (equivalent to universal literacy rate).
     *
     * @return adult literacy rate in percent (%)
     */
    public double getEducationLevel() {
        return getLiteracyRate();
    }

    /**
     * Calculates systemic sociocultural complexity index based on emergent technological capability
     * and division of labor (Seshat complexity characteristics).
     *
     * @return complexity index [0.0..100.0]
     */
    public double getSystemComplexityIndex() {
        double tech = getEmergentTechnologyLevel();
        double divLabor = aggregator.getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 10.0 * 0.4 + divLabor * 0.6));
    }

    /**
     * Evaluates post-catastrophe institutional reconstruction and technical resilience capability
     * based on educational capital and emergent technical expertise.
     *
     * @return reconstruction capability score [0.0..100.0]
     */
    public double getReconstructionCapabilityIndex() {
        double edu = getEducationLevel();
        double tech = getEmergentTechnologyLevel();
        return Math.min(100.0, (edu * 0.7 + tech * 3.0));
    }

    /**
     * Calculates infrastructure and supply-chain interdependence across administrative zones.
     *
     * @return interdependence index [0.0..100.0]
     */
    public double getSystemInterdependenceIndex() {
        double complexity = getSystemComplexityIndex();
        return Math.min(100.0, complexity * 0.95);
    }

    /**
     * Calculates aggregate Shannon information communication channel bandwidth.
     *
     * @return information bandwidth index
     */
    public double getShannonBandwidth() {
        double tech = getEmergentTechnologyLevel();
        return 1.0 + Math.pow(Math.max(0.1, tech), 1.4) * 0.8;
    }

    /**
     * Calculates societal cumulative collective memory stock (accumulated institutional knowledge).
     *
     * @return collective memory stock index
     */
    public double getCollectiveMemoryStock() {
        double tech = getEmergentTechnologyLevel();
        long pop = engine.getTotalPopulation();
        return (pop * 0.05 + Math.pow(Math.max(0.1, tech), 2.1));
    }

    /**
     * Calculates inter-cell innovation diffusion propagation speed.
     *
     * @return innovation diffusion speed index [0.0..100.0]
     */
    public double getInnovationDiffusionSpeed() {
        double tech = getEmergentTechnologyLevel();
        double divLabor = aggregator.getDivisionOfLaborIndex();
        return Math.min(100.0, (tech * 10.0 * 0.4 + divLabor * 0.6));
    }

    /**
     * Calculates institutional knowledge decay rate driven by sociopolitical conflict and inequality.
     *
     * @return knowledge decay rate [0.0..100.0]
     */
    public double getKnowledgeDecayRate() {
        float gini = engine.getCurrentGini();
        double conflict = aggregator.getConflictLevel();
        return Math.min(100.0, (conflict * 0.7 + gini * 30.0));
    }

    /**
     * Evaluates global literacy rate (%) endogenously across active human populations based on
     * Goody (1986), Seshat (Turchin et al. 2018), and Buringh &amp; van Zanden (2009).
     *
     * @return global literacy rate in $[0.0, 99.5]$ %
     */
    public double getLiteracyRate() {
        WorldBuffer wb = engine.getWorldBuffer();
        if (wb != null && wb.getBiomassHuman() != null && wb.getCapacity() > 0) {
            float[] pop = wb.getBiomassHuman();
            float[] capital = wb.getResourceCapital();
            int cap = wb.getCapacity();
            double totalPop = 0.0;
            double weightedLit = 0.0;
            for (int i = 0; i < cap; i++) {
                float p = pop[i];
                if (p <= 0.0f) continue;
                totalPop += p;
                float cellCapital = (capital != null && capital[i] > 0.0f) ? capital[i] : 0.0f;
                float cellKpc = cellCapital / p;
                double powerW = wb.computeCellPowerWattsPerCapita(i);

                // Scribal literacy (ancient administrative scribes, monasteries: 1-5%)
                double scribalActivation = SmoothMath.sigmoid(cellKpc, 15.0, 6.0);
                double scribalLit = 5.0 * scribalActivation;

                // Vernacular literacy (printing press, urban commerce: 10-30%)
                double vernacularActivation = SmoothMath.sigmoid(cellKpc, 400.0, 100.0);
                double vernacularLit = 30.0 * vernacularActivation;

                // Mass universal primary education (industrialization, power grids: up to 99%)
                double massEduActivation = SmoothMath.harmonicCoupling(
                        SmoothMath.sigmoid(cellKpc, 2000.0, 400.0),
                        SmoothMath.sigmoid(powerW, 1000.0, 250.0));
                double massEduLit = 99.0 * massEduActivation;

                double cellLit = SmoothMath.softmax(scribalLit, SmoothMath.softmax(vernacularLit, massEduLit, 1.0), 1.0);
                cellLit = SmoothMath.smoothClamp(cellLit, 0.0, 99.5, 0.1);
                weightedLit += p * cellLit;
            }
            if (totalPop > 0.0) {
                return SmoothMath.smoothClamp(weightedLit / totalPop, 0.0, 99.5, 0.1);
            }
        }

        if (engine.getCells() == null || engine.getCells().isEmpty()) {
            return 0.0;
        }
        double totalPop = 0.0;
        double weightedLit = 0.0;
        for (H3Cell c : engine.getCells()) {
            if (c == null) continue;
            int p = c.getPopulationOr(0);
            if (p <= 0) continue;
            totalPop += p;
            double cellCapital = c.getResourceCapitalOr(0.0);
            double cellKpc = cellCapital / p;
            double powerW = org.ether.society.engines.tier1.PhysicalEnergyGridEngine.calculatePerCapitaMechanicalPowerWatts(c);

            double scribalActivation = SmoothMath.sigmoid(cellKpc, 15.0, 6.0);
            double scribalLit = 5.0 * scribalActivation;

            double vernacularActivation = SmoothMath.sigmoid(cellKpc, 400.0, 100.0);
            double vernacularLit = 30.0 * vernacularActivation;

            double massEduActivation = SmoothMath.harmonicCoupling(
                    SmoothMath.sigmoid(cellKpc, 2000.0, 400.0),
                    SmoothMath.sigmoid(powerW, 1000.0, 250.0));
            double massEduLit = 99.0 * massEduActivation;

            double cellLit = SmoothMath.softmax(scribalLit, SmoothMath.softmax(vernacularLit, massEduLit, 1.0), 1.0);
            cellLit = SmoothMath.smoothClamp(cellLit, 0.0, 99.5, 0.1);
            weightedLit += p * cellLit;
        }
        return totalPop > 0 ? SmoothMath.smoothClamp(weightedLit / totalPop, 0.0, 99.5, 0.1) : 0.0;
    }

    /**
     * Calculates maximum overland/maritime information transmission speed in kilometers per day (km/day).
     *
     * @return travel velocity in km/day
     */
    public double getInformationTravelSpeedKmPerDay() {
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeInformationTravelSpeed(getEmergentTechnologyLevel());
    }

    /**
     * Calculates the macroscopic emergent civilizational technological index from power dissipation and capital stock.
     *
     * @return dimensionless emergent technology index in [0.0..10.0]
     */
    public double getEmergentTechnologyLevel() {
        long pop = engine.getTotalPopulation();
        double powerWatts = (pop > 0) ? (aggregator.getEnergyCaptured() * 1e6) / pop : 100.0;
        double capPerCapita = (pop > 0) ? aggregator.getBuiltCapitalTotal() / pop : 1.0;
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeEmergentTechnologyLevel(powerWatts, capPerCapita);
    }
}

