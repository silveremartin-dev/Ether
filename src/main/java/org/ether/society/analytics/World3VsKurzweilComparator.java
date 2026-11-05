/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier2.theories.KurzweilAcceleratingReturnsEngine;
import org.ether.society.engines.tier2.theories.World3CouplingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * World3 (Meadows Limits to Growth) vs Kurzweil (Law of Accelerating Returns LOAR) Comparator.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
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
    }

    public static class DivergenceReport {
        public double world3FinalCapital;
        public double kurzweilFinalCapital;
        public double structuralDivergenceIndex;
        public String dominantParadigm;
    }

    public static ComparisonReport compareTrajectories(H3Cell initialCell, int startYear, int durationYears) {
        ComparisonReport report = new ComparisonReport();
        if (initialCell == null) return report;

        // 1. World3 Trajectory Simulation
        H3Cell w3Cell = cloneCell(initialCell);
        List<H3Cell> w3Cells = List.of(w3Cell);
        double w3MaxPop = w3Cell.getPopulationOr(0);
        int w3MaxYear = startYear;
        double initialCapital = w3Cell.getResourceCapital() != null ? w3Cell.getResourceCapital() : 1000.0;

        for (int yr = startYear; yr < startYear + durationYears; yr++) {
            World3CouplingEngine.processWorld3System(w3Cells, 1.0);
            if (w3Cell.getPopulationOr(0) > w3MaxPop) {
                w3MaxPop = w3Cell.getPopulationOr(0);
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
        double kurzMaxPop = kurzCell.getPopulationOr(0);
        int kurzMaxYear = startYear;

        for (int yr = startYear; yr < startYear + durationYears; yr++) {
            KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(kurzCells, 1.0);
            if (kurzCell.getPopulationOr(0) > kurzMaxPop) {
                kurzMaxPop = kurzCell.getPopulationOr(0);
                kurzMaxYear = yr;
            }
        }
        report.kurzweilPeakPopulation = kurzMaxPop;
        report.kurzweilPeakYear = kurzMaxYear;
        report.kurzweilTechLevel2050 = kurzCell.getTechnologyLevel() != null ? kurzCell.getTechnologyLevel() : 1.0;

        // 3. Baseline Trajectory
        H3Cell baseCell = cloneCell(initialCell);
        report.baselinePopulation2050 = baseCell.getPopulationOr(0);

        // 4. Hybrid Trajectory (World3 + Kurzweil coupled)
        H3Cell hybridCell = cloneCell(initialCell);
        List<H3Cell> hybridCells = List.of(hybridCell);
        for (int yr = startYear; yr < startYear + durationYears; yr++) {
            KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(hybridCells, 1.0);
            World3CouplingEngine.processWorld3System(hybridCells, 1.0);
        }
        report.hybridPopulation2050 = hybridCell.getPopulationOr(0);

        double popDiff = Math.abs(kurzCell.getPopulationOr(0) - w3Cell.getPopulationOr(0));
        double meanPop = (kurzCell.getPopulationOr(0) + w3Cell.getPopulationOr(0)) / 2.0;
        report.structuralDivergenceIndex = meanPop > 0 ? popDiff / meanPop : 0.0;

        if (report.structuralDivergenceIndex > 0.50) {
            report.dominantDriverSummary = "HIGH DIVERGENCE: Kurzweil technological acceleration overrides World3 resource depletion constraints.";
        } else {
            report.dominantDriverSummary = "BALANCED COUPLING: Physical thermodynamic constraints cap technological accelerating returns.";
        }

        return report;
    }

    public static DivergenceReport evaluateDivergence(List<H3Cell> baselineCells, double horizonYears) {
        DivergenceReport report = new DivergenceReport();
        if (baselineCells == null || baselineCells.isEmpty()) return report;

        List<H3Cell> w3Cells = new ArrayList<>(baselineCells.size());
        List<H3Cell> kurzCells = new ArrayList<>(baselineCells.size());

        for (H3Cell c : baselineCells) {
            w3Cells.add(cloneCell(c));
            kurzCells.add(cloneCell(c));
        }

        World3CouplingEngine.processWorld3Coupling(w3Cells, horizonYears);
        KurzweilAcceleratingReturnsEngine.processAcceleratingReturns(kurzCells, horizonYears);

        double w3Cap = 0;
        double kCap = 0;
        for (int i = 0; i < baselineCells.size(); i++) {
            w3Cap += w3Cells.get(i).getResourceCapitalOr(0.0);
            kCap += kurzCells.get(i).getResourceCapitalOr(0.0);
        }

        report.world3FinalCapital = w3Cap;
        report.kurzweilFinalCapital = kCap;
        report.structuralDivergenceIndex = Math.abs(kCap - w3Cap) / Math.max(1.0, (kCap + w3Cap) / 2.0);
        report.dominantParadigm = kCap >= w3Cap ? "Kurzweil Exponential Expansion" : "World3 Entropic Overshoot";

        logger.info("Executed World3 vs Kurzweil comparison: Divergence index = {}", report.structuralDivergenceIndex);
        return report;
    }

    private static H3Cell cloneCell(H3Cell source) {
        H3Cell clone = new H3Cell(source.getH3Index(), source.getLatitude(), source.getLongitude());
        clone.setPopulation(source.getPopulation());
        clone.setResourceCapital(source.getResourceCapital());
        clone.setResourceMetal(source.getResourceMetal());
        clone.setPollutionLevel(source.getPollutionLevel());
        clone.setFoodResource(source.getFoodResource());
        clone.setLifespan(source.getLifespan());
        return clone;
    }
}
