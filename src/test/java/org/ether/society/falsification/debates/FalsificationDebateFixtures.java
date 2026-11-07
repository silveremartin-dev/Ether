/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.debates;

import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.database.Polity;
import org.ether.society.engines.tier1.BiologicalDemographicsEngine;
import org.ether.society.engines.tier1.EcologicalDegradationEngine;
import org.ether.society.engines.tier1.SoilNutrientNPKEngine;
import org.ether.society.engines.tier2.historical.NordhausDicePureEngine;
import org.ether.society.engines.tier2.historical.PinkerViolenceDeclinePureEngine;
import org.ether.society.engines.tier2.historical.SmilMaterialTransitionsPureEngine;
import org.ether.society.engines.tier2.theories.BoserupAgriculturalIntensificationEngine;
import org.ether.society.engines.tier2.theories.KummelAyresExergyEngine;
import org.ether.society.engines.tier2.theories.TurchinGoldstoneSDTEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fixture grids and simulation loop helpers for the Scientific Model Falsification Suite.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class FalsificationDebateFixtures {

    private FalsificationDebateFixtures() {
        // Utility class
    }

    record SmilInertiaResult(double remainingCapital) {}
    record EroeiCliffResult(double richCap, double depletedCap) {}
    record NordhausDiceResult(double gdp, double tempAnomaly) {}
    record BoserupBifurcationResult(double initialSparseCap, double finalSparseCap, double initialDenseCap, double finalDenseCap, double sparseAgri, double denseAgri) {}

    static List<H3Cell> createGeographicalAgrarianGrid(int count, int popPerCell, double tech, double initialFood) {
        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            H3Cell c = new H3Cell(0x8828308280fffffL + i, 45.0 + (i * 0.008), 9.5 + (i * 0.02));
            c.setPopulation(popPerCell);
            c.setResourceCapital(popPerCell * 50.0);
            c.updateAgePyramidFromObservables();
            c.setFoodResource(initialFood);
            c.setBiomassAgriculture(initialFood * 1.2);
            c.setGiniIndex(0.35);
            c.setMovementFriction(1.0);
            c.setElevation(95.0 + (i % 5) * 5.0);
            c.setAccessibleAquifer(5000.0);
            c.setFreshwaterAquifer(5000.0);
            cells.add(c);
        }
        return cells;
    }

    static void runMalthusVsBoserupSimulation(
            List<H3Cell> malthusGrid,
            List<H3Cell> boserupGrid,
            BoserupAgriculturalIntensificationEngine boserupEngine,
            Map<Integer, Double> malthusTrajectory,
            Map<Integer, Double> boserupTrajectory) {

        for (int t = 0; t < 50; t++) {
            int simYear = 1750 + (t * 2);

            EcologicalDegradationEngine.processEcologicalDegradation(malthusGrid, 1.2, 2.0);
            for (H3Cell c : malthusGrid) {
                double pop = c.getPopulation();
                double food = c.getFoodResourceOr(0.0);
                if (pop * 1.5 > food) {
                    c.setFoodResource(Math.max(0.0, food - (pop * 1.5 - food) * 0.1));
                    c.setPopulation(Math.max(1, (int) (pop * 0.85)));
                }
            }

            boserupEngine.process(boserupGrid, 2.0);
            EcologicalDegradationEngine.processEcologicalDegradation(
                    boserupGrid,
                    H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(boserupGrid.get(0)),
                    2.0
            );
            BiologicalDemographicsEngine.processBiologicalDemographics(boserupGrid, 2.0);

            for (H3Cell c : boserupGrid) {
                double pop = c.getPopulation();
                double carryingCap = c.getBiomassAgriculture() * 35.0;
                if (pop < carryingCap) {
                    double netGrowth = pop * (1.0 - (pop / carryingCap)) * 0.006 * 2.0;
                    int newPop = (int) Math.round(pop + netGrowth);
                    c.setPopulation(newPop);
                }
            }

            malthusTrajectory.put(simYear, malthusGrid.stream().mapToDouble(H3Cell::getPopulation).sum());
            boserupTrajectory.put(simYear, boserupGrid.stream().mapToDouble(H3Cell::getPopulation).sum());
        }
    }

    static BoserupBifurcationResult runDensityThresholdBifurcation() {
        H3Cell sparseCell = new H3Cell(0x8428308280fffffL, 45.0, 10.0);
        sparseCell.setPopulation(1500);
        sparseCell.setResourceCapital(1500 * 50.0);
        sparseCell.setBiomassAgriculture(50.0);

        H3Cell denseCell = new H3Cell(0x8428308281fffffL, 45.0, 10.0);
        denseCell.setPopulation(100000);
        denseCell.setResourceCapital(100000 * 50.0);
        denseCell.setBiomassAgriculture(50.0);

        BoserupAgriculturalIntensificationEngine boserupEngine = new BoserupAgriculturalIntensificationEngine();
        List<H3Cell> cells = List.of(sparseCell, denseCell);

        double initialSparseCap = sparseCell.getResourceCapital();
        double initialDenseCap = denseCell.getResourceCapital();

        for (int t = 0; t < 20; t++) {
            boserupEngine.process(cells, 1.0);
        }
        return new BoserupBifurcationResult(
                initialSparseCap, sparseCell.getResourceCapital(),
                initialDenseCap, denseCell.getResourceCapital(),
                sparseCell.getBiomassAgriculture(), denseCell.getBiomassAgriculture()
        );
    }

    static double runLiebigNPKSimulation() {
        H3Cell farmCell = new H3Cell(0x8428308285fffffL, 45.0, 10.0);
        farmCell.setPopulation(50000);
        farmCell.setBiomassAgriculture(30_000_000.0);
        farmCell.setSoilOrganicCarbon(5.0);
        farmCell.setResourceMetal(0.0);
        farmCell.setResourceCapital(500.0);

        List<H3Cell> cells = List.of(farmCell);
        for (int yr = 0; yr < 10; yr++) {
            SoilNutrientNPKEngine.processSoilNutrients(cells, 1.0);
        }
        return farmCell.getBiomassAgriculture();
    }

    static double runBoserupInvolutionSimulation() {
        H3Cell intensiveTerraceCell = new H3Cell(0x8828308282fffffL, 45.0, 10.0);
        intensiveTerraceCell.setPopulation(120000);
        intensiveTerraceCell.setResourceWork(50.0);
        intensiveTerraceCell.setBiomassAgriculture(50.0);

        BoserupAgriculturalIntensificationEngine boserupEngine = new BoserupAgriculturalIntensificationEngine();
        List<H3Cell> cells = List.of(intensiveTerraceCell);

        for (int yr = 0; yr < 40; yr++) {
            boserupEngine.process(cells, 1.0);
        }
        return intensiveTerraceCell.getResourceWork();
    }

    static void runTurchinVsPinkerSimulation(
            List<H3Cell> imperialCells,
            List<H3Cell> pinkerCells,
            TurchinGoldstoneSDTEngine sdtEngine,
            Polity empire,
            List<Double> pinkerMortalityHistory,
            List<Double> turchinPsiHistory) {

        for (int year = 0; year < 150; year++) {
            double currentGini = 0.35 + 0.30 * (1.0 / (1.0 + Math.exp(-(year - 75.0) / 15.0)));
            imperialCells.forEach(c -> c.setGiniIndex(currentGini));
            pinkerCells.forEach(c -> c.setGiniIndex(currentGini));

            PinkerViolenceDeclinePureEngine.processHybrid(pinkerCells, 1.0);
            double violentDeathRate = PinkerViolenceDeclinePureEngine.calculateViolentDeathRatePerCapita(
                    0.70, 0.85, 0.50);
            pinkerMortalityHistory.add(violentDeathRate);

            empire.updateCliodynamicsCycle();
            sdtEngine.process(imperialCells, 1.0);
            turchinPsiHistory.add(empire.getPoliticalInstability());
        }
    }

    static SmilInertiaResult runSmilInertiaSimulation() {
        List<H3Cell> smilCells = createGeographicalAgrarianGrid(50, 50000, 80.0, 100000.0);
        smilCells.forEach(c -> c.setResourceCapital(10000.0));
        for (int yr = 0; yr < 20; yr++) {
            SmilMaterialTransitionsPureEngine.processHybrid(smilCells, 1.0);
        }
        return new SmilInertiaResult(smilCells.get(0).getResourceCapital());
    }

    static EroeiCliffResult runEroeiCliffSimulation() {
        KummelAyresExergyEngine exergyEngine = new KummelAyresExergyEngine();
        H3Cell richOreCell = new H3Cell(0x8428308287fffffL, 45.0, 10.0);
        richOreCell.setPopulation(10000);
        richOreCell.setResourceCapital(10000.0);
        richOreCell.setResourceMetal(5000.0);
        richOreCell.setBiomassNatural(2000.0);

        H3Cell depletedOreCell = new H3Cell(0x8428308288fffffL, 45.0, 10.0);
        depletedOreCell.setPopulation(10000);
        depletedOreCell.setResourceCapital(1000.0);
        depletedOreCell.setResourceMetal(10.0);
        depletedOreCell.setBiomassNatural(1.0);

        List<H3Cell> cells = List.of(richOreCell, depletedOreCell);
        for (int yr = 0; yr < 20; yr++) {
            exergyEngine.process(cells, 1.0);
        }
        return new EroeiCliffResult(richOreCell.getResourceCapital(), depletedOreCell.getResourceCapital());
    }

    static NordhausDiceResult runNordhausDiceSimulation() {
        NordhausDicePureEngine dice = new NordhausDicePureEngine();
        for (int yr = 0; yr < 50; yr++) {
            dice.processTick(1.0);
        }
        return new NordhausDiceResult(dice.getGdp(), dice.getTemperatureAnomaly());
    }
}
