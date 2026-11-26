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
 * @version 1.0.0-beta.3
 */
final class FalsificationDebateFixtures {

    private FalsificationDebateFixtures() {
        // Utility class
    }

    record SmilInertiaResult(double remainingCapital) {}
    record EroeiCliffResult(double richCap, double depletedCap) {}
    record NordhausDiceResult(double gdp, double tempAnomaly) {}
    record BoserupBifurcationResult(double initialSparseCap, double finalSparseCap, double initialDenseCap, double finalDenseCap) {}

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

        org.ether.society.core.dod.WorldBuffer malthusBuf = new org.ether.society.core.dod.WorldBuffer(malthusGrid.size());
        org.ether.society.core.dod.WorldBuffer boserupBuf = new org.ether.society.core.dod.WorldBuffer(boserupGrid.size());
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(malthusGrid, malthusBuf, false);
        org.ether.society.data.DODDataGenerator.populateWorldBuffer(boserupGrid, boserupBuf, false);
        EcologicalDegradationEngine ecoEngine = new EcologicalDegradationEngine();
        BiologicalDemographicsEngine bioEngine = new BiologicalDemographicsEngine();

        for (int t = 0; t < 50; t++) {
            int simYear = 1750 + (t * 2);

            ecoEngine.tick(malthusBuf, 2.0, 24.0);
            for (int i = 0; i < malthusGrid.size(); i++) {
                double pop = malthusBuf.getHumanPopulation()[i];
                double food = malthusBuf.getFoodResource()[i];
                if (pop * 1.5 > food) {
                    malthusBuf.getFoodResource()[i] = (float) Math.max(0.0, food - (pop * 1.5 - food) * 0.1);
                    malthusBuf.getHumanPopulation()[i] = (float) (Math.max(1.0, pop * 0.85));
                }
            }

            boserupEngine.tick(boserupBuf, 2.0, 24.0);
            ecoEngine.tick(boserupBuf, 2.0, 24.0);
            bioEngine.tick(boserupBuf, 2.0, 24.0);

            double malthusPopSum = 0.0;
            double boserupPopSum = 0.0;
            for (int i = 0; i < boserupGrid.size(); i++) {
                double pop = boserupBuf.getHumanPopulation()[i];
                double carryingCap = boserupBuf.getBiomassAgriculture()[i] * 35.0;
                if (pop < carryingCap && carryingCap > 0.0) {
                    double netGrowth = pop * (1.0 - (pop / carryingCap)) * 0.006 * 2.0;
                    boserupBuf.getHumanPopulation()[i] = (float) ((pop + netGrowth));
                }
                malthusPopSum += malthusBuf.getHumanPopulation()[i];
                boserupPopSum += boserupBuf.getHumanPopulation()[i];
            }

            malthusTrajectory.put(simYear, malthusPopSum);
            boserupTrajectory.put(simYear, boserupPopSum);
        }

        for (int i = 0; i < malthusGrid.size(); i++) {
            malthusGrid.get(i).setPopulation((int) Math.max(0, Math.round(malthusBuf.getHumanPopulation()[i])));
            malthusGrid.get(i).setResourceCapital((double) malthusBuf.getResourceCapital()[i]);
            malthusGrid.get(i).setFoodResource((double) malthusBuf.getFoodResource()[i]);
            malthusGrid.get(i).setBiomassAgriculture((double) malthusBuf.getBiomassAgriculture()[i]);
        }
        for (int i = 0; i < boserupGrid.size(); i++) {
            boserupGrid.get(i).setPopulation((int) Math.max(0, Math.round(boserupBuf.getHumanPopulation()[i])));
            boserupGrid.get(i).setResourceCapital((double) boserupBuf.getResourceCapital()[i]);
            boserupGrid.get(i).setFoodResource((double) boserupBuf.getFoodResource()[i]);
            boserupGrid.get(i).setBiomassAgriculture((double) boserupBuf.getBiomassAgriculture()[i]);
        }
    }

    static BoserupBifurcationResult runDensityThresholdBifurcation() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        // Cell 0: Sparse low-pressure frontier (pop = 200, k_pc = 2500 -> density = 2.0 << 20.0)
        buf.getHumanPopulation()[0] = 200.0f;
        buf.getResourceCapital()[0] = 500_000.0f;
        // Cell 1: Dense agrarian floodplain (pop = 100,000 -> density = 1000.0 >> 20.0)
        buf.getHumanPopulation()[1] = 100_000.0f;
        buf.getResourceCapital()[1] = 1_000_000.0f;

        BoserupAgriculturalIntensificationEngine boserupEngine = new BoserupAgriculturalIntensificationEngine();

        double initialSparseCap = buf.getResourceCapital()[0];
        double initialDenseCap = buf.getResourceCapital()[1];

        for (int t = 0; t < 20; t++) {
            boserupEngine.tick(buf, 1.0, 12.0);
        }
        return new BoserupBifurcationResult(
                initialSparseCap, buf.getResourceCapital()[0],
                initialDenseCap, buf.getResourceCapital()[1]
        );
    }

    static double runLiebigNPKSimulation() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 0x8428308285fffffL;
        buf.getHumanPopulation()[0] = 50000.0f;
        buf.getBiomassAgriculture()[0] = 30_000_000.0f;
        buf.getSoilOrganicCarbon()[0] = 5.0f;
        buf.getMetalResource()[0] = 0.0f;
        buf.getResourceCapital()[0] = 500.0f;

        SoilNutrientNPKEngine npkEngine = new SoilNutrientNPKEngine();
        for (int yr = 0; yr < 10; yr++) {
            npkEngine.tick(buf, 1.0, 12.0);
        }
        return buf.getBiomassAgriculture()[0];
    }

    static double runBoserupInvolutionSimulation() {
        // Measure marginal per-capita intensification return ratio (hyper-dense / transition threshold)
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(4);
        buf.getHumanPopulation()[0] = 1900.0f;
        buf.getHumanPopulation()[1] = 2100.0f;
        buf.getHumanPopulation()[2] = 119_900.0f;
        buf.getHumanPopulation()[3] = 120_100.0f;

        BoserupAgriculturalIntensificationEngine boserupEngine = new BoserupAgriculturalIntensificationEngine();
        boserupEngine.tick(buf, 1.0, 12.0);

        double rateTransitionDelta = (buf.getResourceCapital()[1] / 2100.0) - (buf.getResourceCapital()[0] / 1900.0);
        double rateHyperDenseDelta = (buf.getResourceCapital()[3] / 120_100.0) - (buf.getResourceCapital()[2] / 119_900.0);
        return Math.abs(rateHyperDenseDelta) / Math.max(1e-9, Math.abs(rateTransitionDelta));
    }

    static void runTurchinVsPinkerSimulation(
            List<H3Cell> imperialCells,
            List<H3Cell> pinkerCells,
            TurchinGoldstoneSDTEngine sdtEngine,
            Polity empire,
            List<Double> pinkerMortalityHistory,
            List<Double> turchinPsiHistory) {

        org.ether.society.core.dod.WorldBuffer pinkerBuf = new org.ether.society.core.dod.WorldBuffer(pinkerCells.size());
        org.ether.society.core.dod.WorldBuffer sdtBuf = new org.ether.society.core.dod.WorldBuffer(imperialCells.size());
        for (int i = 0; i < pinkerCells.size(); i++) {
            pinkerBuf.getHumanPopulation()[i] = (float) pinkerCells.get(i).getPopulationOr(0);
            pinkerBuf.getResourceCapital()[i] = (float) pinkerCells.get(i).getResourceCapitalOr(0.0);
        }
        for (int i = 0; i < imperialCells.size(); i++) {
            sdtBuf.getHumanPopulation()[i] = (float) imperialCells.get(i).getPopulationOr(0);
            sdtBuf.getResourceCapital()[i] = (float) imperialCells.get(i).getResourceCapitalOr(0.0);
            sdtBuf.getFoodResource()[i] = (float) imperialCells.get(i).getFoodResourceOr(500.0);
            sdtBuf.getInstitutionalComplexity()[i] = 1.0f;
        }
        PinkerViolenceDeclinePureEngine pinkerEngine = new PinkerViolenceDeclinePureEngine();
        for (int year = 0; year < 150; year++) {
            double currentGini = 0.35 + 0.30 * (1.0 / (1.0 + Math.exp(-(year - 75.0) / 15.0)));
            imperialCells.forEach(c -> c.setGiniIndex(currentGini));
            for (int i = 0; i < imperialCells.size(); i++) {
                sdtBuf.getGiniIndex()[i] = (float) currentGini;
            }
            for (int i = 0; i < pinkerCells.size(); i++) {
                pinkerCells.get(i).setGiniIndex(currentGini);
                pinkerBuf.getGiniIndex()[i] = (float) currentGini;
            }

            pinkerEngine.tick(pinkerBuf, 1.0, 1.0);
            double violentDeathRate = PinkerViolenceDeclinePureEngine.calculateViolentDeathRatePerCapita(
                    0.70, 0.85, 0.50);
            pinkerMortalityHistory.add(violentDeathRate);

            empire.updateCliodynamicsCycle();
            sdtEngine.tick(sdtBuf, 1.0, 12.0);
            turchinPsiHistory.add(empire.getPoliticalInstability());
        }
    }

    static SmilInertiaResult runSmilInertiaSimulation() {
        List<H3Cell> smilCells = createGeographicalAgrarianGrid(50, 50000, 80.0, 100000.0);
        org.ether.society.core.dod.WorldBuffer smilBuf = new org.ether.society.core.dod.WorldBuffer(smilCells.size());
        for (int i = 0; i < smilCells.size(); i++) {
            smilBuf.getHumanPopulation()[i] = (float) smilCells.get(i).getPopulationOr(0);
            smilBuf.getResourceCapital()[i] = 10000.0f;
        }
        SmilMaterialTransitionsPureEngine smilEngine = new SmilMaterialTransitionsPureEngine();
        for (int yr = 0; yr < 20; yr++) {
            smilEngine.tick(smilBuf, 1.0, 1.0);
        }
        return new SmilInertiaResult(smilBuf.getResourceCapital()[0]);
    }

    static EroeiCliffResult runEroeiCliffSimulation() {
        KummelAyresExergyEngine exergyEngine = new KummelAyresExergyEngine();
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        buf.getHumanPopulation()[0] = 10000.0f;
        buf.getResourceCapital()[0] = 10000.0f;
        buf.getResourceCoal()[0] = 5000.0f;
        buf.getResourceOil()[0] = 2000.0f;
        buf.getTemperature()[0] = 15.0f;

        buf.getHumanPopulation()[1] = 10000.0f;
        buf.getResourceCapital()[1] = 1000.0f;
        buf.getResourceCoal()[1] = 0.0f;
        buf.getResourceOil()[1] = 0.0f;
        buf.getTemperature()[1] = 15.0f;

        for (int yr = 0; yr < 20; yr++) {
            exergyEngine.tick(buf, 1.0, 12.0);
        }
        return new EroeiCliffResult(buf.getResourceCapital()[0], buf.getResourceCapital()[1]);
    }

    static NordhausDiceResult runNordhausDiceSimulation() {
        NordhausDicePureEngine dice = new NordhausDicePureEngine();
        for (int yr = 0; yr < 50; yr++) {
            dice.processTick(1.0);
        }
        return new NordhausDiceResult(dice.getGdp(), dice.getTemperatureAnomaly());
    }
}
