package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial unit test suite for {@link ArthurCombinatorialTechnologyEngine}.
 */
public class ArthurCombinatorialTechnologyEngineTest {

    @Test
    public void testCombinatorialInnovationAdvance() {
        ArthurCombinatorialTechnologyEngine engine = new ArthurCombinatorialTechnologyEngine();
        H3Cell cell = new H3Cell(613503380827930850L, 51.5, -0.1);
        cell.setPopulation(5000);
        cell.setTechnologyLevel(2.0);
        cell.setResourceCapital(1000.0);

        engine.process(List.of(cell), 1.0);

        assertTrue(cell.getTechnologyLevel() > 2.0, "Combinatorial innovation must advance technology level");
        assertTrue(Double.isFinite(cell.getTechnologyLevel()), "Technology level must remain finite");
    }

    @Test
    public void testNegativeCapitalRobustness() {
        ArthurCombinatorialTechnologyEngine engine = new ArthurCombinatorialTechnologyEngine();
        H3Cell cell = new H3Cell(613503380827930851L, 51.6, -0.2);
        cell.setPopulation(1000);
        cell.setTechnologyLevel(1.5);
        cell.setResourceCapital(-100.0); // Negative capital (debt/deficit)

        assertDoesNotThrow(() -> {
            engine.process(List.of(cell), 1.0);
        }, "Negative capital must not cause Math.sqrt(negative) NaN exception or crash");

        assertFalse(Double.isNaN(cell.getTechnologyLevel()), "Technology level must never become NaN under negative capital");
    }

    @Test
    public void testNullSafetyGuards() {
        ArthurCombinatorialTechnologyEngine engine = new ArthurCombinatorialTechnologyEngine();
        assertDoesNotThrow(() -> engine.process(null, 1.0));
        assertDoesNotThrow(() -> engine.process(new ArrayList<>(), 1.0));

        H3Cell nullCell = new H3Cell(613503380827930852L, 0.0, 0.0);
        nullCell.setPopulation(null);
        nullCell.setTechnologyLevel(null);
        nullCell.setResourceCapital(null);

        assertDoesNotThrow(() -> {
            engine.process(List.of(nullCell), 1.0);
        }, "Engine must safely process cells with null fields without throwing NPE");
    }
}
