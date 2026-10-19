package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.database.Polity;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Adversarial unit test suite for {@link LanguageLinguisticEngine}.
 */
public class LanguageLinguisticEngineTest {

    @Test
    public void testMountainIslandIsolationPreservesLanguage() {
        H3Cell highMountain = new H3Cell(613503380827930840L, 46.0, 7.0);
        highMountain.setPopulation(1000);
        highMountain.setElevation(2500.0); // High mountain isolate
        highMountain.setMovementFriction(6.0);
        highMountain.setLinguisticDrift(0.20);
        highMountain.setLanguageGroup("Indigenous-Alps");

        LanguageLinguisticEngine.processLinguisticDrift(List.of(highMountain), null);

        assertTrue(highMountain.getLinguisticDrift() < 0.20, "Cultural isolate must actively dampen linguistic drift");
        assertEquals("Indigenous-Alps", highMountain.getLanguageGroup(), "Language group must remain unchanged");
    }

    @Test
    public void testDialectDivergenceOnInlandCorridor() {
        H3Cell valley = new H3Cell(613503380827930841L, 46.5, 7.5);
        valley.setPopulation(5000);
        valley.setElevation(200.0);
        valley.setMovementFriction(1.0);
        valley.setLinguisticDrift(1.05); // High cumulative drift
        valley.setLanguageGroup("Proto-Human");

        LanguageLinguisticEngine.processLinguisticDrift(List.of(valley), null);

        assertNotEquals("Proto-Human", valley.getLanguageGroup(), "High drift on connected plain must trigger dialect bifurcation");
        assertTrue(valley.getLanguageGroup().startsWith("Dialect-"), "Bifurcated language must follow dialect naming schema");
    }

    @Test
    public void testNullFieldRobustness() {
        assertDoesNotThrow(() -> LanguageLinguisticEngine.processLinguisticDrift(null, null));
        assertDoesNotThrow(() -> LanguageLinguisticEngine.processLinguisticDrift(new ArrayList<>(), new ArrayList<>()));

        H3Cell nullCell = new H3Cell(613503380827930842L, 0.0, 0.0);
        nullCell.setPopulation(null);
        nullCell.setElevation(null);
        nullCell.setMovementFriction(null);
        nullCell.setLinguisticDrift(null);
        nullCell.setLanguageGroup(null);
        nullCell.setBiome(null);

        assertDoesNotThrow(() -> {
            LanguageLinguisticEngine.processLinguisticDrift(List.of(nullCell), null);
        }, "Linguistic engine must cleanly tolerate cells with completely null fields");
    }
}
