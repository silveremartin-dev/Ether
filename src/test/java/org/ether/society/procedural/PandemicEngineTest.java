package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PandemicEngineTest {

    @Test
    @DisplayName("Injected outbreak progresses SIR state and reduces population")
    void testInjectedOutbreakProgression() {
        PandemicEngine engine = new PandemicEngine();
        H3Cell cell = new H3Cell(613503380827930702L, 30.0, 31.0);
        cell.setPopulation(100_000);

        List<H3Cell> list = new ArrayList<>();
        list.add(cell);

        engine.injectOutbreak(cell.getH3Index(), 0.05, 2.5);

        PandemicEngine.EpidemiologicalState init = engine.getCellEpidemicState(cell.getH3Index());
        assertNotNull(init);
        assertEquals(0.05, init.infected(), 1e-4);

        // Step simulation turn
        engine.stepPandemicTurn(list, 1.0);

        PandemicEngine.EpidemiologicalState updated = engine.getCellEpidemicState(cell.getH3Index());
        assertNotNull(updated);
        assertTrue(updated.recovered() > 0.0, "Recovered ratio should increase over turn");
        assertTrue(cell.getPopulation() <= 100_000, "Cell population should decrease due to disease mortality");
    }
}
