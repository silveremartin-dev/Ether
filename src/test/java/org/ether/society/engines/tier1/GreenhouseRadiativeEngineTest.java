package org.ether.society.engines.tier1;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

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

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getH3Indexes()[0] = 613503380827930702L;
        buf.getElevation()[0] = 250.0f;
        buf.getTemperature()[0] = 15.0f;

        engine.addEmissions(280.0, 0.0); // Double CO2
        engine.tick(buf, 1.0, 12.0);

        assertTrue(buf.getTemperature()[0] > 15.0f, "Temperature should rise after emissions");
        assertTrue(buf.getElevation()[0] < 250.0f, "Elevation should decrease due to sea level rise");
    }
}
