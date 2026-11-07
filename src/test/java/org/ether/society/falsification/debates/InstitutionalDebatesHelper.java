/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.debates;

import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.engines.tier2.historical.OstromCommonsPureEngine;
import org.ether.society.engines.tier2.historical.ScottAgainstTheGrainPureEngine;
import org.ether.society.engines.tier2.historical.TasmanianCulturalRegressionEngine;
import org.ether.society.engines.tier2.theories.AcemogluRobinsonInstitutionsEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixtures and multi-tick runners for Institutional, Common Pool, and Cultural Evolution debates.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class InstitutionalDebatesHelper {

    private InstitutionalDebatesHelper() {
        // Utility class
    }

    record AcemogluCellPair(H3Cell highlandInclusive, H3Cell coastalExtractive) {}
    record AcemogluShortTermResult(double highlandCapital, double coastalCapital) {}
    record AcemogluLongTermResult(double highlandCapital, double coastalCapital, double highlandTech, double coastalTech) {}
    record ScottZoonoticResult(double earlyGrainTownLifespan, double mobileForagerCampLifespan) {}
    record ZomiaEscapeResult(int valleyPop, int highlandZomiaPop) {}
    record TasmanianRegressionResult(double initialCap, double avgFinalCap) {}
    record ReconnectedArchipelagoResult(double initialCap, double avgCap) {}

    static List<H3Cell> createAquiferGrid(int count, double initialWater) {
        List<H3Cell> cells = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            H3Cell c = new H3Cell(0x8428308287fffffL + i, 45.0 + (i * 0.05), 10.0 + (i * 0.05));
            c.setAccessibleAquifer(initialWater);
            cells.add(c);
        }
        return cells;
    }

    static AcemogluCellPair createAcemogluPair(double capital) {
        H3Cell highland = new H3Cell(0x8828308281fffffL, 5.0, 30.0);
        highland.setPopulation(10000);
        highland.setGiniIndex(0.25);
        highland.setMovementFriction(4.5);
        highland.setResourceCapital(capital);

        H3Cell coastal = new H3Cell(0x8828308283fffffL, 45.0, 5.0);
        coastal.setPopulation(10000);
        coastal.setGiniIndex(0.75);
        coastal.setMovementFriction(1.0);
        coastal.setResourceCapital(capital);

        return new AcemogluCellPair(highland, coastal);
    }

    static double runOstromWatershedSimulation(List<H3Cell> cells, int years) {
        for (int yr = 0; yr < years; yr++) {
            OstromCommonsPureEngine.processHybrid(cells, 1.0);
        }
        return cells.stream().mapToDouble(H3Cell::getAccessibleAquifer).average().orElse(0.0);
    }

    static double runHardinDepletionSimulation(List<H3Cell> cells, int years, double perCapitaPumping) {
        for (int yr = 0; yr < years; yr++) {
            for (H3Cell c : cells) {
                double pumping = c.getPopulation() * perCapitaPumping;
                c.setAccessibleAquifer(Math.max(0.0, c.getAccessibleAquifer() - pumping));
            }
        }
        return cells.stream().mapToDouble(H3Cell::getAccessibleAquifer).average().orElse(0.0);
    }

    static AcemogluShortTermResult runAcemogluShortTermSimulation() {
        AcemogluRobinsonInstitutionsEngine instEngine = new AcemogluRobinsonInstitutionsEngine();
        AcemogluCellPair pair = createAcemogluPair(100.0);
        H3Cell highland = pair.highlandInclusive();
        H3Cell coastal = pair.coastalExtractive();

        List<H3Cell> testCells = List.of(highland, coastal);
        for (int yr = 0; yr < 50; yr++) {
            instEngine.process(testCells, 1.0);
            highland.setResourceCapital(highland.getResourceCapital() + (5.0 / highland.getMovementFriction()));
            coastal.setResourceCapital(coastal.getResourceCapital() + (5.0 / coastal.getMovementFriction()));
        }
        return new AcemogluShortTermResult(highland.getResourceCapital(), coastal.getResourceCapital());
    }

    static AcemogluLongTermResult runAcemogluLongTermSimulation() {
        AcemogluRobinsonInstitutionsEngine instEngine = new AcemogluRobinsonInstitutionsEngine();
        AcemogluCellPair pair = createAcemogluPair(10000 * 100.0);
        H3Cell highland = pair.highlandInclusive();
        H3Cell coastal = pair.coastalExtractive();

        List<H3Cell> testCells = List.of(highland, coastal);
        for (int yr = 0; yr < 300; yr++) {
            instEngine.process(testCells, 1.0);
            double effFrictionA = Math.max(1.0, highland.getMovementFriction() / (1.0 + 0.1 * H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(highland)));
            highland.setResourceCapital(highland.getResourceCapital() + (5.0 / effFrictionA));
            coastal.setResourceCapital(coastal.getResourceCapital() + (5.0 / 1.0));
        }
        return new AcemogluLongTermResult(
                highland.getResourceCapital(),
                coastal.getResourceCapital(),
                H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(highland),
                H3CellPhysicsAndStateHelper.computeEmergentTechnologyDiagnostic(coastal)
        );
    }

    static ScottZoonoticResult runEarlyGrainStateSimulation() {
        H3Cell earlyGrainTown = new H3Cell(0x8828308285fffffL, 31.3, 45.6);
        earlyGrainTown.setPopulation(5000);
        earlyGrainTown.setResourceCapital(5000 * 150.0);
        earlyGrainTown.setLifespan(40.0);
        earlyGrainTown.setPollutionLevel(0.0);

        H3Cell mobileForagerCamp = new H3Cell(0x8828308287fffffL, 31.0, 47.0);
        mobileForagerCamp.setPopulation(100);
        mobileForagerCamp.setResourceCapital(100 * 20.0);
        mobileForagerCamp.setLifespan(40.0);
        mobileForagerCamp.setPollutionLevel(0.0);

        List<H3Cell> cells = List.of(earlyGrainTown, mobileForagerCamp);
        for (int yr = 0; yr < 10; yr++) {
            ScottAgainstTheGrainPureEngine.processHybrid(cells, 1.0);
        }
        return new ScottZoonoticResult(earlyGrainTown.getLifespan(), mobileForagerCamp.getLifespan());
    }

    static ZomiaEscapeResult runZomiaEscapeSimulation() {
        H3Cell valleyCell = new H3Cell(0x8828308285fffffL, 31.3, 45.6);
        valleyCell.setPopulation(10000);
        valleyCell.setResourceCapital(10000 * 150.0);
        valleyCell.setLifespan(40.0);

        H3Cell highlandZomia = new H3Cell(0x8828308287fffffL, 31.0, 47.0);
        highlandZomia.setPopulation(2000);
        highlandZomia.setResourceCapital(2000 * 20.0);
        highlandZomia.setLifespan(40.0);

        List<H3Cell> cells = List.of(valleyCell, highlandZomia);
        for (int yr = 0; yr < 20; yr++) {
            ScottAgainstTheGrainPureEngine.processHybrid(cells, 1.0);
            if (valleyCell.getLifespan() != null && valleyCell.getLifespan() < 35.0) {
                int migrants = 150;
                valleyCell.setPopulation(valleyCell.getPopulation() - migrants);
                highlandZomia.setPopulation(highlandZomia.getPopulation() + migrants);
            }
        }
        return new ZomiaEscapeResult(valleyCell.getPopulation(), highlandZomia.getPopulation());
    }

    static TasmanianRegressionResult runTasmanianRegressionSimulation() {
        List<H3Cell> islandCells = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            H3Cell cell = new H3Cell(0x8428308289fffffL + i, -42.0 + (i * 0.1), 147.0 + (i * 0.1));
            cell.setPopulation(420);
            cell.setResourceCapital(420 * 100.0);
            islandCells.add(cell);
        }
        double initialCap = islandCells.stream().mapToDouble(H3Cell::getResourceCapital).average().orElse(0.0);
        for (int yr = 0; yr < 30; yr++) {
            TasmanianCulturalRegressionEngine.processHybrid(islandCells, 1.0);
        }
        double avgFinalCap = islandCells.stream().mapToDouble(H3Cell::getResourceCapital).average().orElse(0.0);
        return new TasmanianRegressionResult(initialCap, avgFinalCap);
    }

    static ReconnectedArchipelagoResult runReconnectedArchipelagoSimulation() {
        List<H3Cell> connectedGrid = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            H3Cell cell = new H3Cell(0x8428308289fffffL + i, -42.0 + (i * 0.1), 147.0 + (i * 0.1));
            cell.setPopulation(3500);
            cell.setResourceCapital(3500 * 100.0);
            connectedGrid.add(cell);
        }
        double initialCap = connectedGrid.stream().mapToDouble(H3Cell::getResourceCapital).average().orElse(0.0);
        for (int yr = 0; yr < 30; yr++) {
            TasmanianCulturalRegressionEngine.processHybrid(connectedGrid, 1.0);
        }
        double avgCap = connectedGrid.stream().mapToDouble(H3Cell::getResourceCapital).average().orElse(0.0);
        return new ReconnectedArchipelagoResult(initialCap, avgCap);
    }
}
