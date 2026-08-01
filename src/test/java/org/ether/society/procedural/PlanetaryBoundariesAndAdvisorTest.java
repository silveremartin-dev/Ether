/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating Planetary Boundaries and Cliodynamic Advisor.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class PlanetaryBoundariesAndAdvisorTest {

    private List<H3Cell> testCells;

    @BeforeEach
    public void setUp() {
        testCells = new ArrayList<>();
        H3Cell cell = new H3Cell(613503380827930701L, 45.0, 10.0);
        cell.setPopulation(10000);
        cell.setTemperature(20.0);
        cell.setSoilOrganicCarbon(20.0);
        cell.setAccessibleAquifer(50.0);
        testCells.add(cell);
    }

    @Test
    public void testPlanetaryBoundariesAssessment() {
        var status = PlanetaryBoundariesEngine.assessBoundaries(testCells);
        assertNotNull(status);
        assertTrue(status.climateChangeRisk() > 0.5, "Temperature offset should trigger climate change risk.");
    }

    @Test
    public void testCliodynamicAdvisorAlerts() {
        var alerts = CliodynamicAdvisorEngine.generateAdvisorAlerts(testCells, 2026);
        assertNotNull(alerts);
        assertFalse(alerts.isEmpty());
    }
}
