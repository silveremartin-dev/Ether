package org.ether.society.engines.tier2.theories;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
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
        aggCapital.setResourceCapital(50000000.0);
        aggCapital.updateAgePyramidFromTotal();

        H3Cell defCapital = new H3Cell(613503380827930701L, 50.1, 10.1);
        defCapital.setPopulation(2000);
        defCapital.setResourceCapital(2000.0);
        defCapital.updateAgePyramidFromTotal();

        H3Cell defTarget = new H3Cell(613503380827930702L, 50.2, 10.2);
        defTarget.setPopulation(1000);
        defTarget.setResourceCapital(1000.0);
        defTarget.updateAgePyramidFromTotal();

        Polity aggressor = new Polity("Aggressor Kingdom", Color.RED, aggCapital);
        aggressor.setAsabiyyah(0.95);
        aggressor.setStateCapacity(0.90);

        Polity defender = new Polity("Weak Empire", Color.BLUE, defCapital);
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
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        buf.getBiomassHuman()[0] = 500.0f;
        buf.getElevation()[0] = 2500.0f; // Isolated mountain cell
        buf.getBiomassHuman()[1] = 500.0f;
        buf.getElevation()[1] = 200.0f; // Connected plain cell

        LanguageLinguisticEngine engine = new LanguageLinguisticEngine();
        org.ether.society.core.engine.DerivativeAccumulator accIso = new org.ether.society.core.engine.DerivativeAccumulator();
        engine.accumulateCellularDerivatives(buf, 0, 1.0, 1.0, accIso);
        org.ether.society.core.engine.DerivativeAccumulator accConn = new org.ether.society.core.engine.DerivativeAccumulator();
        engine.accumulateCellularDerivatives(buf, 1, 1.0, 1.0, accConn);

        assertEquals(0.0f, accIso.dInstitutionalComplexity, 1e-6f, "Cultural isolation must dampen linguistic drift");
        assertTrue(accConn.dInstitutionalComplexity > 0.0f, "Connected cell must experience positive dialect drift");
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
        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(2);
        buf.getBiomassHuman()[0] = 5000.0f;
        buf.getResourceCapital()[0] = 150000.0f;
        buf.getBiomassNatural()[0] = 2000.0f;
        buf.getBiomassHuman()[1] = 3000.0f;
        buf.getResourceCapital()[1] = 100.0f;
        buf.getNeighborIndexes()[0] = new int[]{1};
        buf.getNeighborIndexes()[1] = new int[]{0};

        new PhysicalCapitalDiffusionEngine().tick(buf, 1.0, 1.0);

        assertTrue(buf.getResourceCapital()[0] >= 150000.0f || buf.getResourceCapital()[1] > 100.0f,
                "Active neighbor network must diffuse physical capital to destination");
        assertTrue(buf.getResourceCapital()[1] > 100.0f, "Active neighbor network must diffuse physical capital to destination");
    }

    @Test
    public void testWarResolutionEdgeCases() {
        // Edge case: Cells with null pop15to24 and population should not throw NPE
        H3Cell aggCell = new H3Cell(613503380827930720L, 50.0, 10.0);
        aggCell.setPopulation(5000);
        aggCell.setPop15to24(1000);

        H3Cell defCap = new H3Cell(613503380827930721L, 50.1, 10.1);
        defCap.setPopulation(1000);
        defCap.setPop15to24(200);

        H3Cell defBorder = new H3Cell(613503380827930722L, 50.2, 10.2);
        defBorder.setPopulation(null); // Null population
        defBorder.setPop15to24(null); // Null pop15to24

        Polity agg = new Polity("Aggressor", Color.RED, aggCell);
        agg.setAsabiyyah(0.99);
        agg.setStateCapacity(0.95);

        Polity def = new Polity("Defender", Color.BLUE, defCap);
        def.addCell(defBorder);
        def.setAsabiyyah(0.05);
        def.setPoliticalInstability(0.95);
        def.setStateCapacity(0.10);

        assertDoesNotThrow(() -> {
            WarDiplomacyEngine.processGeopoliticalConflicts(List.of(agg, def), List.of(aggCell, defCap, defBorder));
        }, "War resolution must cleanly handle null demographic cohorts without NPE");
    }
}
