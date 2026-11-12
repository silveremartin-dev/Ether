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

    private static org.ether.society.core.dod.WorldBuffer toWorldBuffer(List<H3Cell> cells) {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            if (c == null) continue;
            buf.getBiomassHuman()[i] = (float) c.getPopulationOr(0);
            buf.getResourceCapital()[i] = (float) c.getResourceCapitalOr(0.0);
            buf.getMetalResource()[i] = (float) c.getResourceMetalOr(0.0);
            buf.getPollutionLevel()[i] = (float) c.getPollutionLevelOr(0.0);
            buf.getFoodResource()[i] = (float) c.getFoodResourceOr(100.0);
            buf.getLifespan()[i] = (float) c.getLifespanOr(40.0);
        }
        return buf;
    }

    public static ComparisonReport compareTrajectories(H3Cell initialCell, int startYear, int durationYears) {
        ComparisonReport report = new ComparisonReport();
        if (initialCell == null) return report;

        // 1. World3 Trajectory Simulation
        H3Cell w3Cell = cloneCell(initialCell);
        org.ether.society.core.dod.WorldBuffer w3Buf = toWorldBuffer(List.of(w3Cell));
        World3CouplingEngine w3Engine = new World3CouplingEngine();
        double w3MaxPop = w3Buf.getBiomassHuman()[0];
        int w3MaxYear = startYear;
        double initialCapital = w3Cell.getResourceCapital() != null ? w3Cell.getResourceCapital() : 1000.0;

        for (int yr = startYear; yr < startYear + durationYears; yr++) {
            w3Engine.tick(w3Buf, 1.0, 1.0);
            if (w3Buf.getBiomassHuman()[0] > w3MaxPop) {
                w3MaxPop = w3Buf.getBiomassHuman()[0];
                w3MaxYear = yr;
            }
        }
        report.world3PeakPopulation = w3MaxPop;
        report.world3PeakYear = w3MaxYear;
        double finalCapitalW3 = w3Buf.getResourceCapital()[0];
        report.world3ResourceDepletionRate = Math.max(0.0, (initialCapital - finalCapitalW3) / (initialCapital * durationYears));

        // 2. Kurzweil Trajectory Simulation
        H3Cell kurzCell = cloneCell(initialCell);
        org.ether.society.core.dod.WorldBuffer kurzBuf = toWorldBuffer(List.of(kurzCell));
        KurzweilAcceleratingReturnsEngine kurzEngine = new KurzweilAcceleratingReturnsEngine();
        double kurzMaxPop = kurzBuf.getBiomassHuman()[0];
        int kurzMaxYear = startYear;

        for (int yr = startYear; yr < startYear + durationYears; yr++) {
            kurzEngine.tick(kurzBuf, 1.0, 1.0);
            if (kurzBuf.getBiomassHuman()[0] > kurzMaxPop) {
                kurzMaxPop = kurzBuf.getBiomassHuman()[0];
                kurzMaxYear = yr;
            }
        }
        report.kurzweilPeakPopulation = kurzMaxPop;
        report.kurzweilPeakYear = kurzMaxYear;
        kurzCell.setResourceCapital((double) kurzBuf.getResourceCapital()[0]);
        report.kurzweilTechLevel2050 = org.ether.society.database.H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(kurzCell);

        // 3. Baseline Trajectory
        H3Cell baseCell = cloneCell(initialCell);
        report.baselinePopulation2050 = baseCell.getPopulationOr(0);

        // 4. Hybrid Trajectory (World3 + Kurzweil coupled)
        H3Cell hybridCell = cloneCell(initialCell);
        org.ether.society.core.dod.WorldBuffer hybridBuf = toWorldBuffer(List.of(hybridCell));
        for (int yr = startYear; yr < startYear + durationYears; yr++) {
            kurzEngine.tick(hybridBuf, 1.0, 1.0);
            w3Engine.tick(hybridBuf, 1.0, 1.0);
        }
        report.hybridPopulation2050 = hybridBuf.getBiomassHuman()[0];

        double popDiff = Math.abs(kurzBuf.getBiomassHuman()[0] - w3Buf.getBiomassHuman()[0]);
        double meanPop = (kurzBuf.getBiomassHuman()[0] + w3Buf.getBiomassHuman()[0]) / 2.0;
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

        org.ether.society.core.dod.WorldBuffer w3Buf = toWorldBuffer(baselineCells);
        org.ether.society.core.dod.WorldBuffer kurzBuf = toWorldBuffer(baselineCells);

        new World3CouplingEngine().tick(w3Buf, horizonYears, horizonYears);
        new KurzweilAcceleratingReturnsEngine().tick(kurzBuf, horizonYears, horizonYears);

        double w3Cap = 0;
        double kCap = 0;
        for (int i = 0; i < baselineCells.size(); i++) {
            w3Cap += w3Buf.getResourceCapital()[i];
            kCap += kurzBuf.getResourceCapital()[i];
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
