/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.falsification.debates;

import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.engines.tier2.theories.CliodynamicsEngine;
import org.ether.society.engines.tier2.theories.LanguageLinguisticEngine;
import org.ether.society.engines.tier2.theories.WarDiplomacyEngine;
import org.ether.society.model.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixtures and simulation runners for the Grataloup-Marshall Natural Orographic Boundaries debate.
 *
 * <p>Sets up multi-cell Franco-Iberian spatial lattices calibrated against HYDE 3.4
 * demographic densities and ETOPO altimetry profiles, and executes multi-tick cliodynamic
 * conflict and linguistic drift simulations.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class GrataloupBoundaryDebateHelper {

    private GrataloupBoundaryDebateHelper() {
        // Utility class
    }

    /**
     * Builds an empirical Franco-Iberian hexagonal grid capturing lowland plains,
     * high-friction orographic crests (Pyrenees/Alps), and the Iberian Meseta.
     *
     * @return 30-cell list representing Gaul plains, mountain crests, and Hispania meseta
     */
    static List<H3Cell> createEmpiricalFrancoIberianGrid() {
        List<H3Cell> cells = new ArrayList<>();
        // 1. Gaul / Francia Lowland Plain Cells (10 cells, 60-180m, friction 1.0, HYDE pop 8,000-14,000)
        for (int i = 0; i < 10; i++) {
            H3Cell c = new H3Cell(0x882830828100000L + i, 43.6 + (i * 0.15), 0.5 + ((i % 3) * 0.2));
            c.setPopulation(8000 + (i * 600));
            c.setResourceCapital(1500.0);
            c.updateAgePyramidFromObservables();
            c.setFoodResource(25000.0);
            c.setElevation(80.0 + (i * 10.0));
            c.setMovementFriction(1.0);
            c.setGiniIndex(0.35);
            c.setLanguageGroup("Dialect-Gallo-Romance");
            cells.add(c);
        }
        // 2. Pyrenean / Alpine Orographic Crest Cells (8 cells, 1,800-2,600m, friction 4.5-5.5, HYDE pop 800-1,500)
        for (int i = 0; i < 8; i++) {
            H3Cell c = new H3Cell(0x882830828200000L + i, 42.8 + (i * 0.05), 0.5 + ((i % 2) * 0.2));
            c.setPopulation(900 + (i * 70));
            c.setResourceCapital(200.0);
            c.updateAgePyramidFromObservables();
            c.setFoodResource(4000.0);
            c.setElevation(2200.0 + (i * 50.0));
            c.setMovementFriction(5.0);
            c.setGiniIndex(0.35);
            c.setLanguageGroup("Dialect-Vasconic-Proto-Basque");
            cells.add(c);
        }
        // 3. Hispania / Ebro Meseta Cells (12 cells, 350-650m, friction 1.2-1.5, HYDE pop 7,000-12,000)
        for (int i = 0; i < 12; i++) {
            H3Cell c = new H3Cell(0x882830828300000L + i, 42.1 - (i * 0.15), 0.5 + ((i % 3) * 0.2));
            c.setPopulation(7500 + (i * 400));
            c.setResourceCapital(1200.0);
            c.updateAgePyramidFromObservables();
            c.setFoodResource(22000.0);
            c.setElevation(450.0 + (i * 15.0));
            c.setMovementFriction(1.3);
            c.setGiniIndex(0.35);
            c.setLanguageGroup("Dialect-Ibero-Romance");
            cells.add(c);
        }
        return cells;
    }

    /**
     * Quantified telemetry result for the Pax Romana expansion benchmark.
     *
     * @param boundaryFriction        friction across the alpine crest pass
     * @param chiefdomRemainingCells  remaining territory of defeated chiefdom
     * @param romeControlsPass        whether Rome successfully annexed the pass
     */
    record PaxRomanaResult(double boundaryFriction, int chiefdomRemainingCells, boolean romeControlsPass) {}

    /**
     * Executes the Pax Romana multi-tick imperial expansion scenario (Debate 8.1).
     *
     * @return simulation result record
     */
    static PaxRomanaResult executePaxRomanaScenario() {
        Scenario romanScenario = new Scenario();
        romanScenario.setName("Pax Romana Expansion");
        romanScenario.setStartDateYear(100L);

        List<H3Cell> grid = createEmpiricalFrancoIberianGrid();
        H3Cell romeCapital = grid.get(0);
        romeCapital.setPopulation(80000);
        romeCapital.setPop15to24(16000);
        romeCapital.setResourceCapital(15000.0);
        romeCapital.updateAgePyramidFromTotal();

        H3Cell alpinePass = grid.get(10);
        alpinePass.setGiniIndex(0.28);
        H3Cell defCapital = grid.get(18);
        defCapital.setGiniIndex(0.28);

        Polity romanEmpire = new Polity("Roman Empire", javafx.scene.paint.Color.RED, romeCapital);
        romanEmpire.setStateCapacity(0.95);
        romanEmpire.setAsabiyyah(0.95);
        romanEmpire.setTreasuryBullion(25000.0);
        for (int i = 1; i < 6; i++) {
            romanEmpire.addCell(grid.get(i));
        }

        Polity chiefdom = new Polity("Cisalpine Chiefdom", javafx.scene.paint.Color.GREEN, defCapital);
        chiefdom.addCell(alpinePass);
        chiefdom.setStateCapacity(0.20);
        chiefdom.setPoliticalInstability(0.50);
        chiefdom.setAsabiyyah(0.55);
        chiefdom.setTreasuryBullion(10.0);

        romanScenario.getCulturalAffinityMatrix().put(romanEmpire.getId() + ":" + chiefdom.getId(), 0.0);
        double borderFriction = WarDiplomacyEngine.calculateBoundaryFriction(romeCapital, alpinePass);

        for (int tick = 0; tick < 20; tick++) {
            CliodynamicsEngine.updateCliodynamics(List.of(romanEmpire, chiefdom), grid);
            WarDiplomacyEngine.processGeopoliticalConflicts(List.of(romanEmpire, chiefdom), grid, romanScenario);
        }

        return new PaxRomanaResult(borderFriction, chiefdom.getTerritory().size(), romanEmpire.getTerritory().contains(alpinePass));
    }

    /**
     * Result holder for feudal border conquest comparisons.
     *
     * @param plainConquests    cells conquered on plains
     * @param mountainConquests cells conquered across mountain crest
     * @param gammaOrography    topographic boundary alignment index
     */
    record FeudalBifurcationResult(int plainConquests, int mountainConquests, double gammaOrography) {}

    /**
     * Executes the post-Roman feudal multi-tick conflict simulation comparing plains vs mountain crests (Debate 8.2).
     *
     * @return quantified conquest results and gamma alignment index
     */
    static FeudalBifurcationResult executeFeudalBifurcationScenario() {
        Scenario feudalScenario = new Scenario();
        feudalScenario.setName("Feudal Europe (843 CE Post-Verdun)");
        feudalScenario.setStartDateYear(843L);

        List<H3Cell> grid = createEmpiricalFrancoIberianGrid();
        H3Cell plainCellA = grid.get(0);
        H3Cell plainCellBTarget = grid.get(1);
        plainCellBTarget.setGiniIndex(0.28);
        H3Cell plainCellBCapital = grid.get(2);
        plainCellBCapital.setGiniIndex(0.28);

        Polity feudalPlainsAggressor = new Polity("Francia Plains", javafx.scene.paint.Color.BLUE, plainCellA);
        feudalPlainsAggressor.addCell(grid.get(3));
        feudalPlainsAggressor.addCell(grid.get(4));
        feudalPlainsAggressor.setStateCapacity(0.70);
        feudalPlainsAggressor.setAsabiyyah(0.95);
        feudalPlainsAggressor.setTreasuryBullion(250.0);

        Polity feudalPlainsDefender = new Polity("Burgundia Plains", javafx.scene.paint.Color.YELLOW, plainCellBCapital);
        feudalPlainsDefender.addCell(plainCellBTarget);
        feudalPlainsDefender.setStateCapacity(0.20);
        feudalPlainsDefender.setPoliticalInstability(0.50);
        feudalPlainsDefender.setAsabiyyah(0.55);
        feudalPlainsDefender.setTreasuryBullion(20.0);

        H3Cell mountainCellA = new H3Cell(0x882830828400000L, 43.1, 0.5);
        mountainCellA.setElevation(250.0);
        mountainCellA.setMovementFriction(1.5);
        mountainCellA.setPopulation(8500);
        mountainCellA.setPop15to24(1700);
        mountainCellA.setResourceCapital(1500.0);
        mountainCellA.updateAgePyramidFromTotal();
        mountainCellA.setGiniIndex(0.35);

        H3Cell mountainCrestBTarget = grid.get(10);
        mountainCrestBTarget.setElevation(2400.0);
        mountainCrestBTarget.setMovementFriction(5.0);
        mountainCrestBTarget.setGiniIndex(0.28);

        H3Cell mountainCellBCapital = grid.get(18);
        mountainCellBCapital.setElevation(500.0);
        mountainCellBCapital.setMovementFriction(2.0);
        mountainCellBCapital.setGiniIndex(0.28);

        Polity feudalMountainAggressor = new Polity("Francia South", javafx.scene.paint.Color.BLUE, mountainCellA);
        feudalMountainAggressor.addCell(grid.get(7));
        feudalMountainAggressor.addCell(grid.get(8));
        feudalMountainAggressor.setStateCapacity(0.70);
        feudalMountainAggressor.setAsabiyyah(0.95);
        feudalMountainAggressor.setTreasuryBullion(250.0);

        Polity feudalMountainDefender = new Polity("Hispania North", javafx.scene.paint.Color.ORANGE, mountainCellBCapital);
        feudalMountainDefender.addCell(mountainCrestBTarget);
        feudalMountainDefender.setStateCapacity(0.20);
        feudalMountainDefender.setPoliticalInstability(0.50);
        feudalMountainDefender.setAsabiyyah(0.55);
        feudalMountainDefender.setTreasuryBullion(20.0);

        feudalScenario.getCulturalAffinityMatrix().put(feudalPlainsAggressor.getId() + ":" + feudalPlainsDefender.getId(), 0.0);
        feudalScenario.getCulturalAffinityMatrix().put(feudalMountainAggressor.getId() + ":" + feudalMountainDefender.getId(), 0.0);

        int initialPlainDefenderCells = feudalPlainsDefender.getTerritory().size();
        int initialMountainDefenderCells = feudalMountainDefender.getTerritory().size();

        for (int tick = 0; tick < 25; tick++) {
            CliodynamicsEngine.updateCliodynamics(List.of(feudalPlainsAggressor, feudalPlainsDefender), grid);
            CliodynamicsEngine.updateCliodynamics(List.of(feudalMountainAggressor, feudalMountainDefender), grid);
            WarDiplomacyEngine.processGeopoliticalConflicts(List.of(feudalPlainsAggressor, feudalPlainsDefender), grid, feudalScenario);
            WarDiplomacyEngine.processGeopoliticalConflicts(List.of(feudalMountainAggressor, feudalMountainDefender), grid, feudalScenario);
        }

        int plainConquests = initialPlainDefenderCells - feudalPlainsDefender.getTerritory().size();
        int mountainConquests = initialMountainDefenderCells - feudalMountainDefender.getTerritory().size();
        double gamma = (double) (plainConquests - mountainConquests) / Math.max(1, plainConquests + mountainConquests);

        return new FeudalBifurcationResult(plainConquests, mountainConquests, gamma);
    }

    /**
     * Runs multi-cycle linguistic drift along mountain barriers.
     *
     * @param grid    Franco-Iberian spatial lattice
     * @param cycles  drift cycles
     * @param seed    PRNG base seed
     */
    static void runLinguisticDriftSimulation(List<H3Cell> grid, int cycles, long seed) {
        LanguageLinguisticEngine.resetState();
        LanguageLinguisticEngine.setBaseSeed(seed);
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(grid.size());
        for (int i = 0; i < grid.size(); i++) {
            H3Cell c = grid.get(i);
            buf.getBiomassHuman()[i] = (float) c.getPopulationOr(0);
            buf.getElevation()[i] = (float) c.getElevationOr(0.0);
        }
        LanguageLinguisticEngine engine = new LanguageLinguisticEngine();
        for (int cycle = 0; cycle < cycles; cycle++) {
            engine.tick(buf, 1.0, 1.0);
        }
    }
}
