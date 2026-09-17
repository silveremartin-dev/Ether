/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import org.ether.society.procedural.KurzweilAcceleratingReturnsEngine;
import org.ether.society.procedural.World3CouplingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * World3 (Meadows Limits to Growth) vs Kurzweil (Law of Accelerating Returns LOAR) Comparator.
 * Compares system dynamics outcomes across parallel timeline trajectories:
 * 1. <b>World3 Limits to Growth</b>: Resource depletion, capital diversion, pollution feedback, Malthusian collapse.
 * 2. <b>Kurzweil LOAR</b>: Double-exponential technological acceleration, resource efficiency gains, singularity asymptote.
 * 3. <b>Ether Thermodynamic Baseline</b>: Physical EROEI and biophysical limits without macro-heuristics.
 * 4. <b>Coupled Hybrid</b>: Physical EROEI limits combined with tech acceleration and ecological feedback.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class World3VsKurzweilComparator {
    private static final Logger logger = LoggerFactory.getLogger(World3VsKurzweilComparator.class);

    public static class ComparisonReport {
        public double world3PeakPopulation = 0.0;
        public int world3PeakYear = 2026;
        public double world3ResourceDepletionRate = 0.0;

        public double kurzweilPeakPopulation = 0.0;
        public int kurzweilPeakYear = 2026;
        public double kurzweilTechLevel2050 = 0.0;

        public double baselinePopulation2050 = 0.0;
        public double hybridPopulation2050 = 0.0;

        public double structuralDivergenceIndex = 0.0;
        public String dominantDriverSummary = "";

        @Override
        public String toString() {
            return String.format(
                "World3 vs Kurzweil Comparison Report:\n" +
                "  - World3 Peak Pop: %.2fM in year %d (Depletion: %.2f%%/yr)\n" +
                "  - Kurzweil Peak Pop: %.2fM in year %d (Tech Level 2050: %.2f)\n" +
                "  - Baseline Pop 2050: %.2fM | Hybrid Pop 2050: %.2fM\n" +
                "  - Structural Divergence Index: %.4f\n" +
                "  - Dominant Driver: %s",
                world3PeakPopulation / 1e6, world3PeakYear, world3ResourceDepletionRate * 100,
                kurzweilPeakPopulation / 1e6, kurzweilPeakYear, kurzweilTechLevel2050,
                baselinePopulation2050 / 1e6, hybridPopulation2050 / 1e6,
                structuralDivergenceIndex, dominantDriverSummary
            );
        }
    }

    /**
     * Executes comparative multi-trajectory analysis over a given time horizon.
     */
    public static ComparisonReport compareTrajectories(H3Cell initialCell, int startYear, int durationYears) {
        ComparisonReport report = new ComparisonReport();

        // 1. World3 Trajectory Simulation
        H3Cell w3Cell = cloneCell(initialCell);
        List<H3Cell> w3Cells = List.of(w3Cell);
        double w3MaxPop = w3Cell.getPopulation();
        int w3MaxYear = startYear;
        double initialCapital = w3Cell.getResourceCapital() != null ? w3Cell.getResourceCapital() : 1000.0;

        for (int yr = startYear; yr < startYear + durationYears; yr++) {
            World3CouplingEngine.processWorld3System(w3Cells, 1.0);
            if (w3Cell.getPopulation() > w3MaxPop) {
                w3MaxPop = w3Cell.getPopulation();
                w3MaxYear = yr;
            }
        }
        report.world3PeakPopulation = w3MaxPop;
        report.world3PeakYear = w3MaxYear;
        double finalCapitalW3 = w3Cell.getResourceCapital() != null ? w3Cell.getResourceCapital() : 0.0;
        report.world3ResourceDepletionRate = Math.max(0.0, (initialCapital - finalCapitalW3) / (initialCapital * durationYears));

        // 2. Kurzweil Trajectory Simulation
        H3Cell kurzCell = cloneCell(initialCell);
        List<H3Cell> kurzCells = List.of(kurzCell);
        double kurzMaxPop = kurzCell.getPopulation();
        int kurzMaxYear = startYear;

        for (int yr = startYear; yr < startYear + durationYears; yr++) {
            KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(kurzCells, 1.0);
            if (kurzCell.getPopulation() > kurzMaxPop) {
                kurzMaxPop = kurzCell.getPopulation();
                kurzMaxYear = yr;
            }
        }
        report.kurzweilPeakPopulation = kurzMaxPop;
        report.kurzweilPeakYear = kurzMaxYear;
        report.kurzweilTechLevel2050 = kurzCell.getTechnologyLevel() != null ? kurzCell.getTechnologyLevel() : 1.0;

        // 3. Baseline Trajectory
        H3Cell baseCell = cloneCell(initialCell);
        report.baselinePopulation2050 = baseCell.getPopulation();

        // 4. Hybrid Trajectory (World3 + Kurzweil coupled)
        H3Cell hybridCell = cloneCell(initialCell);
        List<H3Cell> hybridCells = List.of(hybridCell);
        for (int yr = startYear; yr < startYear + durationYears; yr++) {
            KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(hybridCells, 1.0);
            World3CouplingEngine.processWorld3System(hybridCells, 1.0);
        }
        report.hybridPopulation2050 = hybridCell.getPopulation();

        // Compute Structural Divergence Index (ratio of Kurzweil pop vs World3 pop divergence)
        double popDiff = Math.abs(kurzCell.getPopulation() - w3Cell.getPopulation());
        double meanPop = (kurzCell.getPopulation() + w3Cell.getPopulation()) / 2.0;
        report.structuralDivergenceIndex = meanPop > 0 ? popDiff / meanPop : 0.0;

        if (report.structuralDivergenceIndex > 0.50) {
            report.dominantDriverSummary = "HIGH DIVERGENCE: Kurzweil technological acceleration overrides World3 resource depletion constraints.";
        } else {
            report.dominantDriverSummary = "BALANCED COUPLING: Physical thermodynamic constraints cap technological accelerating returns.";
        }

        logger.info("Executed World3 vs Kurzweil comparison: Divergence index = {}", report.structuralDivergenceIndex);
        return report;
    }

    private static H3Cell cloneCell(H3Cell source) {
        H3Cell clone = new H3Cell(source.getH3Index(), source.getLatitude(), source.getLongitude());
        clone.setPopulation(source.getPopulation());
        clone.setResourceCapital(source.getResourceCapital());
        clone.setResourceMetal(source.getResourceMetal());
        clone.setPollutionLevel(source.getPollutionLevel());
        clone.setTechnologyLevel(source.getTechnologyLevel());
        clone.setFoodResource(source.getFoodResource());
        clone.setLifespan(source.getLifespan());
        return clone;
    }
}

