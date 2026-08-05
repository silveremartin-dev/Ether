package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GreenhouseRadiativeEngineTest {

    @Test
    @DisplayName("Baseline greenhouse gas concentrations yield zero temperature anomaly")
    void testBaselineZeroAnomaly() {
        GreenhouseRadiativeEngine engine = new GreenhouseRadiativeEngine(280.0, 720.0);
        assertEquals(0.0, engine.computeRadiativeForcingWpm2(), 1e-4);
        assertEquals(0.0, engine.computeTemperatureAnomalyC(), 1e-4);
        assertEquals(0.0, engine.computeSeaLevelDeltaMeters(), 1e-4);
    }

    @Test
    @DisplayName("Doubling CO2 produces positive radiative forcing and temperature increase")
    void testCo2Doubling() {
        GreenhouseRadiativeEngine engine = new GreenhouseRadiativeEngine(560.0, 720.0);
        double forcing = engine.computeRadiativeForcingWpm2();
        double deltaT = engine.computeTemperatureAnomalyC();

        assertTrue(forcing > 3.5 && forcing < 4.0, "CO2 doubling should yield ~3.7 W/m² forcing");
        assertTrue(deltaT > 2.5 && deltaT < 3.5, "CO2 doubling temperature anomaly should be ~3.0°C");
        assertTrue(engine.computeSeaLevelDeltaMeters() > 100.0, "Sea level should rise significantly");
    }

    @Test
    @DisplayName("Applying emissions updates grid temperatures and sea elevations")
    void testApplyToGrid() {
        GreenhouseRadiativeEngine engine = new GreenhouseRadiativeEngine();

        H3Cell cell = new H3Cell(613503380827930702L, 45.0, 5.0);
        cell.setElevation(250.0);

        List<H3Cell> list = new ArrayList<>();
        list.add(cell);

        engine.addEmissions(280.0, 0.0); // Double CO2
        engine.applyToGrid(list);

        assertTrue(cell.getTemperature() > 15.0, "Temperature should rise after emissions");
        assertTrue(cell.getElevation() < 250.0, "Elevation should decrease due to sea level rise");
    }
}
