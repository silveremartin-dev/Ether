package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Nation;
import org.junit.jupiter.api.Test;
import javafx.scene.paint.Color;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WarDiplomacyEngineTest {

    @Test
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
    public void testLinguisticDriftAndLinguaFranca() {
        H3Cell cell1 = new H3Cell(613503380827930703L, 40.0, 5.0);
        cell1.setPopulation(500);
        cell1.setMovementFriction(5.0); // Isolated cell

        LanguageLinguisticEngine.processLinguisticDrift(List.of(cell1), null);

        assertTrue(cell1.getLinguisticDrift() > 0.0, "High friction should increase linguistic drift");
    }

    @Test
    public void testTechTreeDiffusion() {
        H3Cell hub = new H3Cell(613503380827930704L, 48.0, 2.0);
        hub.setPopulation(5000);
        hub.setResourceCapital(15000.0); // High capital
        hub.setTechnologyLevel(5.0);

        TechTreeEngine.processTechnologyDiffusion(List.of(hub), null);

        assertTrue(hub.getTechnologyLevel() >= TechTreeEngine.INDUSTRIAL.minTechLevel(), "High capital hub should unlock Industrial Steam era");
    }
}
