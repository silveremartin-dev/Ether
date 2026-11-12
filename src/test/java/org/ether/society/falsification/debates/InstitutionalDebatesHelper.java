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
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            buf.getBiomassHuman()[i] = (float) c.getPopulationOr(0);
            buf.getFreshwaterAquifer()[i] = c.getAccessibleAquifer() != null ? c.getAccessibleAquifer().floatValue() : 0.0f;
            buf.getRainfall()[i] = (float) c.getRainfallOr(800.0);
            buf.getResourceCapital()[i] = (float) c.getResourceCapitalOr(0.0);
        }
        OstromCommonsPureEngine engine = new OstromCommonsPureEngine();
        for (int yr = 0; yr < years; yr++) {
            engine.tick(buf, 1.0, 1.0);
        }
        double sum = 0.0;
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setAccessibleAquifer((double) buf.getFreshwaterAquifer()[i]);
            sum += buf.getFreshwaterAquifer()[i];
        }
        return cells.isEmpty() ? 0.0 : sum / cells.size();
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

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        buf.getBiomassHuman()[0] = 10000.0f;
        buf.getResourceCapital()[0] = 10000.0f * 150.0f;
        buf.getLifespan()[0] = 40.0f;
        buf.getPollutionLevel()[0] = 0.0f;
        buf.getBiomassHuman()[1] = 100.0f;
        buf.getResourceCapital()[1] = 100.0f * 20.0f;
        buf.getLifespan()[1] = 40.0f;
        buf.getPollutionLevel()[1] = 0.0f;

        ScottAgainstTheGrainPureEngine engine = new ScottAgainstTheGrainPureEngine();
        for (int yr = 0; yr < 10; yr++) {
            engine.tick(buf, 1.0, 1.0);
        }
        return new ScottZoonoticResult(buf.getLifespan()[0], buf.getLifespan()[1]);
    }

    static ZomiaEscapeResult runZomiaEscapeSimulation() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        buf.getBiomassHuman()[0] = 10000.0f;
        buf.getResourceCapital()[0] = 10000.0f * 150.0f;
        buf.getLifespan()[0] = 40.0f;
        buf.getBiomassHuman()[1] = 2000.0f;
        buf.getResourceCapital()[1] = 2000.0f * 20.0f;
        buf.getLifespan()[1] = 40.0f;

        ScottAgainstTheGrainPureEngine engine = new ScottAgainstTheGrainPureEngine();
        for (int yr = 0; yr < 20; yr++) {
            engine.tick(buf, 1.0, 1.0);
            if (buf.getLifespan()[0] < 35.0f) {
                float migrants = 150.0f;
                buf.getBiomassHuman()[0] -= migrants;
                buf.getBiomassHuman()[1] += migrants;
            }
        }
        return new ZomiaEscapeResult((int) buf.getBiomassHuman()[0], (int) buf.getBiomassHuman()[1]);
    }

    static TasmanianRegressionResult runTasmanianRegressionSimulation() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(7);
        double initialSum = 0.0;
        for (int i = 0; i < 7; i++) {
            buf.getBiomassHuman()[i] = 420.0f;
            buf.getResourceCapital()[i] = 420.0f * 100.0f;
            initialSum += buf.getResourceCapital()[i];
        }
        double initialCap = initialSum / 7.0;
        TasmanianCulturalRegressionEngine engine = new TasmanianCulturalRegressionEngine();
        for (int yr = 0; yr < 30; yr++) {
            engine.tick(buf, 1.0, 1.0);
        }
        double finalSum = 0.0;
        for (int i = 0; i < 7; i++) {
            finalSum += buf.getResourceCapital()[i];
        }
        return new TasmanianRegressionResult(initialCap, finalSum / 7.0);
    }

    static ReconnectedArchipelagoResult runReconnectedArchipelagoSimulation() {
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(7);
        double initialSum = 0.0;
        for (int i = 0; i < 7; i++) {
            buf.getBiomassHuman()[i] = 3500.0f;
            buf.getResourceCapital()[i] = 3500.0f * 100.0f;
            initialSum += buf.getResourceCapital()[i];
        }
        double initialCap = initialSum / 7.0;
        TasmanianCulturalRegressionEngine engine = new TasmanianCulturalRegressionEngine();
        for (int yr = 0; yr < 30; yr++) {
            engine.tick(buf, 1.0, 1.0);
        }
        double finalSum = 0.0;
        for (int i = 0; i < 7; i++) {
            finalSum += buf.getResourceCapital()[i];
        }
        return new ReconnectedArchipelagoResult(initialCap, finalSum / 7.0);
    }
}
