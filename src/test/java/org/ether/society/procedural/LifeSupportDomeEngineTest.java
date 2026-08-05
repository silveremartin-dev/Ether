package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LifeSupportDomeEngineTest {

    @Test
    @DisplayName("Vacuum planet correctly detected as requiring life support domes")
    void testRequiresLifeSupportDomes() {
        LifeSupportDomeEngine engine = new LifeSupportDomeEngine();
        assertTrue(engine.requiresLifeSupportDomes(PlanetPreset.MARS_LIKE) || engine.requiresLifeSupportDomes(PlanetPreset.MOON_LIKE));
        assertFalse(engine.requiresLifeSupportDomes(PlanetPreset.EARTH_LIKE));
    }

    @Test
    @DisplayName("Constructing dome enables population protection on vacuum world")
    void testDomeConstructionAndProtection() {
        LifeSupportDomeEngine engine = new LifeSupportDomeEngine();

        H3Cell cell = new H3Cell(613503380827930702L, 0.0, 0.0);
        cell.setPopulation(100_000);

        List<H3Cell> list = new ArrayList<>();
        list.add(cell);

        // Low tech / low metal fails construction
        assertFalse(engine.constructDome(cell, 1.0, 100.0));

        // High tech + high metal succeeds
        assertTrue(engine.constructDome(cell, 4.0, 1000.0));

        LifeSupportDomeEngine.DomeInfrastructure dome = engine.getDome(cell.getH3Index());
        assertNotNull(dome);
        assertEquals(1, dome.domeTier());

        // Enforce turn with vacuum/hostile preset
        PlanetPreset vacuumPlanet = PlanetPreset.MARS_LIKE;
        engine.enforceLifeSupportTurn(list, vacuumPlanet);

        // Population protected within dome capacity (50,000 max for Tier 1)
        assertEquals(50_000, cell.getPopulation());
    }
}
