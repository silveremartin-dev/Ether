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
     * Shannon informational density (I/N bits/capita), collective memory stock, and physical transmission media
     * (Goody 1986, Buringh &amp; van Zanden 2009, Graff 1987).
     *
     * <p>Universal across deep time, future eras, and exoplanetary colonization:
     * Space colonists arriving with minimal physical ground infrastructure but high knowledge stocks
     * retain their full adult literacy (e.g. 99.5%), while prehistoric oral hunter-gatherers ($I \le 20\text{ bits/hab}$)
     * evaluate smoothly to 0.0% without artificial calendar-year gating.</p>
     *
     * @return global literacy rate in $[0.0, 99.5]$ %
     */
    public double getLiteracyRate() {
        double baseI0 = (engine.getCurrentScenario() != null)
                ? engine.getCurrentScenario().getInitialInformationPerCapita() : 100.0;

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
                double usefulPowerW = Math.max(0.0, powerW - 100.0);

                // Endogenous technological information generated by local infrastructure:
                double endogenousI = (cellKpc > 30.0)
                        ? 0.20 * Math.pow(cellKpc - 30.0, 1.4) + 0.05 * Math.pow(usefulPowerW, 1.5)
                        : 0.0;
                double cellI = Math.max(baseI0, endogenousI);

                // 1. Scribal literacy (early symbolic pictograms, cuneiform, hieroglyphs, alphabets: 1-3.5% Buringh & van Zanden 2009)
                double scribal = 3.5 * Math.tanh(Math.max(0.0, cellI - 25.0) / 80.0);

                // 2. Vernacular print literacy (Gutenberg movable type press, merchant bookkeeping, early modern: 10-30%)
                double vernacular = 26.5 * Math.tanh(Math.max(0.0, cellI - 500.0) / 3000.0);

                // 3. Mass universal primary education (industrial schooling, telecommunications, digital age: up to 99.5%)
                double massEdu = 69.5 * Math.tanh(Math.max(0.0, cellI - 20000.0) / 50000.0);

                double cellLit = Math.clamp(scribal + vernacular + massEdu, 0.0, 99.5);
                weightedLit += p * cellLit;
            }
            if (totalPop > 0.0) {
                return Math.max(0.0, Math.min(99.5, weightedLit / totalPop));
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
            double usefulPowerW = Math.max(0.0, powerW - 100.0);

            double endogenousI = (cellKpc > 30.0)
                    ? 0.20 * Math.pow(cellKpc - 30.0, 1.4) + 0.05 * Math.pow(usefulPowerW, 1.5)
                    : 0.0;
            double cellI = Math.max(baseI0, endogenousI);

            double scribal = 3.5 * Math.tanh(Math.max(0.0, cellI - 25.0) / 80.0);
            double vernacular = 26.5 * Math.tanh(Math.max(0.0, cellI - 500.0) / 3000.0);
            double massEdu = 69.5 * Math.tanh(Math.max(0.0, cellI - 20000.0) / 50000.0);

            double cellLit = Math.clamp(scribal + vernacular + massEdu, 0.0, 99.5);
            weightedLit += p * cellLit;
        }
        return totalPop > 0 ? Math.max(0.0, Math.min(99.5, weightedLit / totalPop)) : 0.0;
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

