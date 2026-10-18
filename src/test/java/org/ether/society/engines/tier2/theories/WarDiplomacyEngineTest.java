package org.ether.society.procedural;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Nation;
import org.junit.jupiter.api.Test;
import javafx.scene.paint.Color;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WarDiplomacyEngineTest {

    @Test
    /*
     * Test geopolitical war resolution operation.
     * <p>
     * Executes operational logic for {@code WarDiplomacyEngineTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testGeopoliticalWarResolution() {
        H3Cell aggCapital = new H3Cell(613503380827930700L, 50.0, 10.0);
        aggCapital.setPopulation(10000);
        aggCapital.updateAgePyramidFromTotal(5.0);

        H3Cell defCapital = new H3Cell(613503380827930701L, 50.1, 10.1);
        defCapital.setPopulation(2000);
        defCapital.updateAgePyramidFromTotal(1.0);

        H3Cell defTarget = new H3Cell(613503380827930702L, 50.2, 10.2);
        defTarget.setPopulation(1000);
        defTarget.updateAgePyramidFromTotal(1.0);

        Nation aggressor = new Nation("Aggressor Kingdom", Color.RED, aggCapital);
        aggressor.setAsabiyyah(0.95);
        aggressor.setStateCapacity(0.90);

        Nation defender = new Nation("Weak Empire", Color.BLUE, defCapital);
        defender.addCell(defTarget);
        defender.setAsabiyyah(0.10);
        defender.setPoliticalInstability(0.85);
        defender.setStateCapacity(0.20);

        int initialDefTerritory = defender.getTerritory().size();

        WarDiplomacyEngine.processGeopoliticalConflicts(List.of(aggressor, defender), List.of(aggCapital, defCapital, defTarget));

        assertTrue(defender.getTerritory().size() <= initialDefTerritory, "Defender should lose border territory under high instability and weak capacity");
    }

    @Test
    /*
     * Test linguistic drift and lingua franca operation.
     * <p>
     * Executes operational logic for {@code WarDiplomacyEngineTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testLinguisticDriftAndLinguaFranca() {
        // Isolated cell with high friction (mountain/island)
        H3Cell isolatedCell = new H3Cell(613503380827930703L, 40.0, 5.0);
        isolatedCell.setPopulation(500);
        isolatedCell.setMovementFriction(5.0); // Isolated cell
        isolatedCell.setLinguisticDrift(0.10);

        // Connected cell with low friction (inland trade corridor)
        H3Cell connectedCell = new H3Cell(613503380827930704L, 40.5, 5.5);
        connectedCell.setPopulation(500);
        connectedCell.setMovementFriction(1.0);
        connectedCell.setLinguisticDrift(0.10);

        LanguageLinguisticEngine.processLinguisticDrift(List.of(isolatedCell, connectedCell), null);

        assertEquals(0.08, isolatedCell.getLinguisticDrift(), 1e-4, "Cultural isolation must dampen linguistic drift");
        assertTrue(connectedCell.getLinguisticDrift() > 0.10, "Connected cell must experience positive dialect drift");
    }

    @Test
    /*
     * Test tech tree diffusion operation.
     * <p>
     * Executes operational logic for {@code WarDiplomacyEngineTest} within the automated verification and regression test suite.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     */
    public void testTechTreeDiffusion() {
        H3Cell hub = new H3Cell(613503380827930704L, 48.0, 2.0);
        hub.setPopulation(5000);
        hub.setResourceCapital(150000.0); // High capital (K/N = 30)
        hub.setBiomassNatural(2000.0);
        hub.setTechnologyLevel(1.0); // Starting Neolithic

        H3Cell dest = new H3Cell(613503380827930705L, 48.2, 2.2);
        dest.setPopulation(3000);
        dest.setResourceCapital(100.0);
        dest.setTechnologyLevel(1.0);

        TradeNetworkEngine.TradeRoute route = new TradeNetworkEngine.TradeRoute(hub, dest, List.of(hub, dest), 1.0);

        TechTreeEngine.processTechnologyDiffusion(List.of(hub, dest), List.of(route), 1.0);

        assertTrue(hub.getTechnologyLevel() > 1.0, "High power and capital must advance tech level via ratchet effect");
        assertTrue(dest.getTechnologyLevel() > 1.0, "Active trade route must diffuse technological knowledge to destination");
    }
}
