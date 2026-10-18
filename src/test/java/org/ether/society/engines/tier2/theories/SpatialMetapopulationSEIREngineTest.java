package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial and dynamical verification suite for {@link SpatialMetapopulationSEIREngine}.
 */
public class SpatialMetapopulationSEIREngineTest {

    @Test
    public void testSEIRTransmissionAndDecay() {
        SpatialMetapopulationSEIREngine engine = new SpatialMetapopulationSEIREngine();

        H3Cell cell = new H3Cell(613503380827930810L, 45.0, 10.0);
        cell.setPopulation(10000);
        cell.setEpidemicInfected(200);

        engine.process(List.of(cell), 0.1); // ~36 days

        assertNotNull(cell.getEpidemicInfected());
        assertTrue(cell.getEpidemicInfected() >= 0, "Infected population cannot be negative");
        assertTrue(cell.getEpidemicInfected() <= cell.getPopulation(), "Infected cannot exceed total population");
        assertTrue(cell.getPopulation() <= 10000, "Fatalities must decrement total population");
    }

    @Test
    public void testInversionResistanceWhenInfectedExceedsPopulation() {
        SpatialMetapopulationSEIREngine engine = new SpatialMetapopulationSEIREngine();

        // Edge case: Inconsistent or corrupted data where infected > population
        H3Cell cell = new H3Cell(613503380827930811L, 45.1, 10.1);
        cell.setPopulation(100);
        cell.setEpidemicInfected(500); // 500 > 100

        assertDoesNotThrow(() -> {
            engine.process(List.of(cell), 0.05);
        }, "Engine must safely clamp infected count without producing negative exposed cohorts");

        assertTrue(cell.getEpidemicInfected() <= cell.getPopulation(), "Infected must be bounded by population");
        assertTrue(cell.getEpidemicInfected() >= 0, "Infected must remain non-negative");
    }

    @Test
    public void testNullSafetyAndEmptyList() {
        SpatialMetapopulationSEIREngine engine = new SpatialMetapopulationSEIREngine();

        assertDoesNotThrow(() -> engine.process(null, 1.0));
        assertDoesNotThrow(() -> engine.process(new ArrayList<>(), 1.0));

        H3Cell emptyCell = new H3Cell(613503380827930812L, 0.0, 0.0);
        emptyCell.setPopulation(null);
        emptyCell.setEpidemicInfected(null);

        assertDoesNotThrow(() -> engine.process(List.of(emptyCell), 1.0));
    }
}
