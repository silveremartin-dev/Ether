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
import org.ether.society.database.Polity;
import org.ether.society.model.PhysicalConstants;
import org.ether.society.core.math.SmoothMath;

import java.util.Comparator;
import java.util.Objects;

/**
 * <h1>CliodynamicMetricsCalculator</h1>
 * <p>
 * Calculates Structural-Demographic Theory (SDT) vulnerability indicators,
 * elite overproduction, fiscal distress, collective solidarity (Ibn Khaldun's Asabiyyah),
 * institutional maturity, and societal collapse risk (Peter Turchin, Jack Goldstone, Joseph Tainter).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class CliodynamicMetricsCalculator {

    private final SimulationMetricsAggregator aggregator;
    private final H3SimulationEngine engine;

    /**
     * Constructs a calculator bound to the given aggregator.
     *
     * @param aggregator parent metrics aggregator, must not be null
     * @param engine     parent simulation engine, must not be null
     */
    public CliodynamicMetricsCalculator(SimulationMetricsAggregator aggregator, H3SimulationEngine engine) {
        this.aggregator = Objects.requireNonNull(aggregator, "aggregator must not be null");
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /**
     * Evaluates internal social conflict and violent instability level ($0$ to $100$).
     *
     * @return conflict level index
     */
    public double getConflictLevel() {
        float gini = engine.getCurrentGini();
        double happiness = aggregator.getHappinessIndex();
        double foodPerCap = aggregator.getFoodPerCapita();
        double annualReq = PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
        double scarcity = (foodPerCap > 0 && foodPerCap < annualReq) ? (1.0 - foodPerCap / annualReq) * 25.0 : 0.0;
        return Math.max(0.0, Math.min(100.0, (gini * 35.0) + (100.0 - happiness) * 0.25 + scarcity));
    }

    /**
     * Evaluates the Sociopolitical Instability Index (PSI) based on Peter Turchin's Structural-Demographic Theory (SDT).
     *
     * @return sociopolitical instability index in $[0, 100]$
     */
    public double getSociopoliticalInstabilityIndex() {
        float tech = engine.getAverageTechnology();
        float gini = engine.getCurrentGini();
        double safeGini = Double.isNaN(gini) ? 0.17 : gini;
        double urbanRate = aggregator.getUrbanizationRate();

        // 1. Anthropological pre-state inter-band skirmish baseline (Keeley 1996; Kohler et al. 2017)
        double bandSkirmishBaseline = 5.0 + 11.0 * safeGini;

        // 2. Endogenous state polity emergence factor Phi_state(T, U) (Turchin 2003, 2016):
        double stateEmergenceFactor = Math.tanh(Math.max(0.0, tech - 1.55) * 1.35 + (urbanRate / 12.0));
        if (stateEmergenceFactor <= 0.001) {
            return Math.clamp(bandSkirmishBaseline, 0.0, 100.0);
        }

        double rawConflict = getConflictLevel();
        double eoi = getEliteOverproductionIndex();
        double fiscal = getFiscalStressIndex();
        double geo = getGeopoliticalTension();
        double stateDistress = 0.5 * fiscal + 0.5 * geo;
        double stateInstability = stateEmergenceFactor * (0.65 * rawConflict + 2.0 * eoi + 0.35 * stateDistress);
        return Math.clamp(bandSkirmishBaseline + stateInstability, 0.0, 100.0);
    }

    /**
     * Evaluates the number of active urban city-states (emerging endogenously with urban agglomeration).
     *
     * @return count of city-states
     */
    public int getCityStatesCount() {
        double urbanRate = aggregator.getUrbanizationRate();
        float tech = engine.getAverageTechnology();
        if (urbanRate < 0.5 || tech < 1.6f) {
            return 0;
        }
        if (engine.getNations() != null && !engine.getNations().isEmpty()) {
            return (int) engine.getNations().stream().filter(p -> p.getTerritory() != null && !p.getTerritory().isEmpty()).count();
        }
        long popCells = engine.getPopulatedCellCount();
        return (int) Math.min(80, Math.max(1, popCells / 350));
    }

    /**
     * Evaluates institutional governance maturity index ($0$ to $100$).
     *
     * @return institutional maturity
     */
    public double getInstitutionalMaturity() {
        float tech = engine.getAverageTechnology();
        return Math.min(100.0, tech * 0.45);
    }

    /**
     * Evaluates Durkheimian division of labor and occupational specialization index ($0$ to $100$).
     *
     * @return division of labor index
     */
    public double getDivisionOfLaborIndex() {
        float tech = engine.getAverageTechnology();
        long pop = engine.getTotalPopulation();
        double surplus = aggregator.getNetSurplusFraction();
        return Math.min(100.0, (surplus * 60.0) + (tech * 0.25) + Math.log10(Math.max(1, pop)) * 3.0);
    }

    /**
     * Evaluates Seshat administrative hierarchy complexity level ($1.0$ to $6.0$) as a continuous scalar.
     *
     * @return hierarchy level score in $[1, 6]$
     */
    public int getMaxHierarchyLevel() {
        float tech = engine.getAverageTechnology();
        double h = 1.0 + 5.0 / (1.0 + Math.exp(-1.35 * (tech - 2.90)));
        return (int) Math.round(Math.clamp(h, 1.0, 6.0));
    }

    /**
     * Evaluates population size of the largest polity or cultural unit.
     *
     * @return largest polity population
     */
    public long getLargestCulturalUnitSize() {
        if (!engine.getNations().isEmpty()) {
            return engine.getNations().stream()
                    .mapToLong(Polity::getTotalPopulation)
                    .max()
                    .orElse((long) (engine.getTotalPopulation() * Math.min(0.85, 0.2 + (engine.getAverageTechnology() / 300.0))));
        }
        long pop = engine.getTotalPopulation();
        return (long) (pop * Math.min(0.85, 0.2 + (engine.getAverageTechnology() / 300.0)));
    }

    /**
     * Evaluates Tainter organizational complexity of the largest polity.
     *
     * @return largest organization complexity score
     */
    public double getLargestOrganizationComplexity() {
        long largestPop = getLargestCulturalUnitSize();
        if (largestPop <= 0) return 0.0;
        float avgTech = engine.getAverageTechnology();
        int hierarchy = getMaxHierarchyLevel();
        double stateCap = 0.5;
        if (!engine.getNations().isEmpty()) {
            Polity largest = engine.getNations().stream()
                    .max(Comparator.comparingLong(Polity::getTotalPopulation))
                    .orElse(null);
            if (largest != null) {
                stateCap = largest.getStateCapacity();
            }
        }
        return largestPop * (1.0 + avgTech * 0.4) * stateCap * (Math.log(1.0 + hierarchy) / Math.log(2.0));
    }

    /**
     * Evaluates thermodynamic maintenance entropy of the largest organization.
     *
     * @return organization entropy
     */
    public double getLargestOrganizationEntropy() {
        double complexity = getLargestOrganizationComplexity();
        double pollution = aggregator.getPollutionLevel();
        float tech = engine.getAverageTechnology();
        return (complexity * 0.05 + tech * 1.2) * (1.0 + pollution / 100.0);
    }

    /**
     * Evaluates Turchin elite population share percentage ($0.5\%$ to $25\%$).
     *
     * @return elite formation ratio
     */
    public double getEliteFormationRatio() {
        float gini = engine.getCurrentGini();
        return Math.min(25.0, Math.max(0.5, 1.0 + gini * 15.0));
    }

    /**
     * Evaluates share of capital held by senior cohorts ($30\%$ to $90\%$).
     *
     * @return elder capital share percentage
     */
    public double getElderCapitalShare() {
        float gini = engine.getCurrentGini();
        float life = engine.getCurrentLifeExpectancy();
        return Math.min(90.0, Math.max(30.0, 40.0 + (life / 80.0) * 30.0 + gini * 20.0));
    }

    /**
     * Evaluates Turchin elite overproduction index ($0.0$ to $4.5$).
     *
     * @return elite overproduction index
     */
    public double getEliteOverproductionIndex() {
        float tech = engine.getAverageTechnology();
        double stateActivation = SmoothMath.sigmoid(tech, 1.02, 0.04);
        if (stateActivation <= 0.01) return 0.0;
        float gini = engine.getCurrentGini();
        double safeGini = Double.isNaN(gini) ? 0.40 : gini;
        double eoi = stateActivation * (1.05 + 1.25 * Math.max(0.0, safeGini - 0.30) + 0.25 * Math.tanh(Math.max(0.0, tech - 2.50)));
        return Double.isNaN(eoi) ? 0.0 : Math.clamp(eoi, 0.0, 4.5);
    }

    /**
     * Evaluates Goldstone/Turchin state fiscal stress index ($0.0$ to $100.0$).
     *
     * @return fiscal stress index
     */
    public double getFiscalStressIndex() {
        double landRent = aggregator.getLandRentIndex();
        float gini = engine.getCurrentGini();
        double val = ((Double.isNaN(gini) ? 0.0 : gini) * 50.0) + (landRent * 0.3);
        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));
    }

    /**
     * Evaluates interstate geopolitical tension index ($0.0$ to $100.0$).
     *
     * @return geopolitical tension index
     */
    public double getGeopoliticalTension() {
        int cityStates = getCityStatesCount();
        double conflict = getConflictLevel();
        double val = (Math.min(50.0, cityStates * 0.8)) + (conflict * 0.5);
        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));
    }

    /**
     * Evaluates composite civilizational collapse vulnerability index ($0.0$ to $100.0$).
     *
     * @return collapse vulnerability index
     */
    public double getCollapseVulnerability() {
        double resDep = aggregator.getResourceDepletionRate();
        double psi = getEliteOverproductionIndex();
        double entropy = aggregator.getSystemicEntropy();
        double eroi = aggregator.getEroiAlimentaire();
        double thermodynamicInversionRisk = (eroi < 1.0)
                ? (1.0 / Math.max(0.05, eroi)) * ((Double.isNaN(resDep) ? 0.0 : resDep) / 100.0) * 15.0 : 0.0;
        double val = ((Double.isNaN(resDep) ? 0.0 : resDep) * 0.25)
                + ((Double.isNaN(psi) ? 0.0 : psi) * 3.5)
                + ((Double.isNaN(entropy) ? 0.0 : entropy) / 20.0)
                + thermodynamicInversionRisk;
        return Double.isNaN(val) ? 0.0 : Math.max(0.0, Math.min(100.0, val));
    }

    /**
     * Evaluates average Ibn Khaldun social cohesion (Asabiyyah) across owned cells ($0\%$ to $100\%$).
     *
     * @return average Asabiyyah percentage
     */
    public double getAverageAsabiyyah() {
        if (engine.getCells() == null || engine.getCells().isEmpty()) return 80.0;
        double sum = 0;
        int count = 0;
        for (H3Cell c : engine.getCells()) {
            if (c != null && c.getOwner() != null) {
                sum += c.getOwner().getAsabiyyah();
                count++;
            }
        }
        return count > 0 ? (sum / count) * 100.0 : 80.0;
    }

    /**
     * Evaluates Turchin &amp; Nefedov (2009) Political Stress Index.
     *
     * @return political stress index in $[0.0, 100.0]$
     */
    public double getPoliticalStressIndex() {
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computePoliticalStressIndex(
                engine.getAverageTechnology(), aggregator.getHappinessIndex());
    }

    /**
     * Evaluates Ibn Khaldun (1377) normalized Asabiyyah / Collective Solidarity Score in $[0.0, 1.0]$.
     *
     * @return normalized social cohesion score in $[0.0, 1.0]$
     */
    public double getAsabiyyahSocialCohesionScore() {
        return org.ether.society.analytics.PostHocCliodynamicMetricsExtractor.computeAsabiyyahSocialCohesion(
                engine.getAverageTechnology(), aggregator.getHappinessIndex());
    }
}
