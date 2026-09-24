package org.ether.society.model;

import org.ether.society.procedural.PlanetPreset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that Earth presets have accurate physical sea levels per epoch,
 * and that all built-in scenarios are assigned the exact matching PlanetPreset and EcologyPreset.
 */
class EarthPresetEpochConsistencyTest {

    @Test
    @DisplayName("Earth PlanetPresets have physically sound sea level thresholds (waterLevel) per epoch")
    void testEarthPresetSeaLevels() {
        // Modern Earth / Holocene baseline: waterLevel = 0.478 (~0m datum, ~70.8% ocean coverage)
        assertEquals(0.478, PlanetPreset.EARTH_MODERN.waterLevel(), 0.0001);
        assertEquals(0.478, PlanetPreset.EARTH_IRON_1000BP.waterLevel(), 0.0001);
        assertEquals(0.478, PlanetPreset.EARTH_BRONZE_1900BP.waterLevel(), 0.0001);
        assertEquals(0.478, PlanetPreset.EARTH_LH_3000BP.waterLevel(), 0.0001);
        assertEquals(0.478, PlanetPreset.EARTH_MH_6000BP.waterLevel(), 0.0001);

        // Early Holocene (-10000 BP): post-glacial sea level rising (~ -35m) -> waterLevel ~ 0.476479
        assertEquals(0.476479, PlanetPreset.EARTH_EH_10000BP.waterLevel(), 0.0001);
        assertEquals(-35.0, PlanetPreset.EARTH_EH_10000BP.seaLevelMeters(), 0.1);
        assertTrue(PlanetPreset.EARTH_EH_10000BP.waterLevel() < PlanetPreset.EARTH_MODERN.waterLevel(),
                "Early Holocene sea level should be lower than modern");

        // Last Glacial Maximum peak (-20000 BP): sea level regression of -125m -> waterLevel = 0.472568
        assertEquals(0.472568, PlanetPreset.EARTH_LGM_20000BP.waterLevel(), 0.0001);
        assertEquals(-125.0, PlanetPreset.EARTH_LGM_20000BP.seaLevelMeters(), 0.1);
        assertTrue(PlanetPreset.EARTH_LGM_20000BP.waterLevel() < PlanetPreset.EARTH_EH_10000BP.waterLevel());

        // LGM Onset / Beringia (-25000 BP): intermediate regression (-100m) -> waterLevel = 0.473655
        assertEquals(0.473655, PlanetPreset.EARTH_LGM_ONSET_25000BP.waterLevel(), 0.0001);
        assertEquals(-100.0, PlanetPreset.EARTH_LGM_ONSET_25000BP.seaLevelMeters(), 0.1);

        // Marine Isotope Stage 3 / Sahul (-50000 BP): intermediate regression (-60m) -> waterLevel = 0.475393
        assertEquals(0.475393, PlanetPreset.EARTH_MIS3_50000BP.waterLevel(), 0.0001);
        assertEquals(-60.0, PlanetPreset.EARTH_MIS3_50000BP.seaLevelMeters(), 0.1);

        // Last Interglacial / Eemian (-100000 BP): 0m datum (preserving coastal corridors)
        assertEquals(0.478, PlanetPreset.EARTH_LIG_100000BP.waterLevel(), 0.002);
        assertEquals(0.0, PlanetPreset.EARTH_LIG_100000BP.seaLevelMeters(), 0.5);
    }

    @Test
    @DisplayName("All built-in scenarios use the correct PlanetPreset and EcologyPreset corresponding to their historical epoch")
    void testScenariosMatchHistoricalEpochs() {
        List<Scenario> scenarios = Scenario.getBuiltInScenarios();
        assertNotNull(scenarios);
        assertFalse(scenarios.isEmpty());

        for (Scenario sc : scenarios) {
            PlanetPreset planet = sc.getPlanetPreset();
            EcologyPreset ecology = sc.getEcologyPreset();

            assertNotNull(planet, "Scenario '" + sc.getName() + "' must have a non-null PlanetPreset");
            assertNotNull(ecology, "Scenario '" + sc.getName() + "' must have a non-null EcologyPreset");

            long year = sc.getStartDateYear();

            if (year <= -70000) {
                // Out of Africa / Paleolithic (-100,000 BP)
                assertEquals(PlanetPreset.EARTH_LIG_100000BP.name(), planet.name());
                assertEquals(EcologyPreset.EARTH_LIG_100000BP.name(), ecology.name());
            } else if (year <= -40000) {
                // Sahul (-50,000 BP)
                assertEquals(PlanetPreset.EARTH_MIS3_50000BP.name(), planet.name());
                assertEquals(EcologyPreset.EARTH_MIS3_50000BP.name(), ecology.name());
            } else if (year <= -22000) {
                // Beringia / LGM onset (-25,000 BP)
                assertEquals(PlanetPreset.EARTH_LGM_ONSET_25000BP.name(), planet.name());
                assertEquals(EcologyPreset.EARTH_LGM_ONSET_25000BP.name(), ecology.name());
            } else if (year <= -15000) {
                // LGM Solutrean (-20,000 BP)
                assertEquals(PlanetPreset.EARTH_LGM_20000BP.name(), planet.name());
                assertEquals(EcologyPreset.EARTH_LGM_20000BP.name(), ecology.name());
            } else if (year <= -8000) {
                // Younger Dryas (-10,900) & Fertile Crescent (-8000)
                assertEquals(PlanetPreset.EARTH_EH_10000BP.name(), planet.name());
                assertEquals(EcologyPreset.EARTH_EH_10000BP.name(), ecology.name());
            } else if (year <= -5000) {
                // Green Sahara (-6000)
                assertEquals(PlanetPreset.EARTH_MH_6000BP.name(), planet.name());
                assertEquals(EcologyPreset.EARTH_MH_6000BP.name(), ecology.name());
            } else if (year <= -2500) {
                // Ancient Egypt (-3000)
                assertEquals(PlanetPreset.EARTH_LH_3000BP.name(), planet.name());
                assertEquals(EcologyPreset.EARTH_LH_3000BP.name(), ecology.name());
            } else if (year <= -1200) {
                // Middle Bronze (-1900) & Mesoamerica (-1500)
                assertEquals(PlanetPreset.EARTH_BRONZE_1900BP.name(), planet.name());
                assertEquals(EcologyPreset.EARTH_BRONZE_1900BP.name(), ecology.name());
            } else if (year <= -200) {
                // Early Iron Age (-1000) & Maurya (-300)
                assertEquals(PlanetPreset.EARTH_IRON_1000BP.name(), planet.name());
                assertEquals(EcologyPreset.EARTH_IRON_1000BP.name(), ecology.name());
            } else {
                // Common Era to Modern (0 to 2060)
                assertEquals(PlanetPreset.EARTH_LIKE.name(), planet.name());
            }
        }
    }

    @Test
    @DisplayName("Ecology presets link strictly to their corresponding PlanetPresets")
    void testEcologyPresetsLinkToPlanetPresets() {
        for (EcologyPreset eco : EcologyPreset.getBuiltInPresets()) {
            assertNotNull(eco.planetPresetName());
            assertFalse(eco.planetPresetName().isBlank());
            if (eco.embeddedPlanetPreset() != null) {
                assertEquals(eco.planetPresetName(), eco.embeddedPlanetPreset().name());
            }
        }
    }
}
