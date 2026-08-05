/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.analytics.World3VsKurzweilComparator;
import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit Test Suite validating the World3 (Meadows Limits to Growth) vs Kurzweil (Law of Accelerating Returns LOAR) comparator.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class World3VsKurzweilTestSuite {

    private H3Cell initialCell;

    @BeforeEach
    public void setUp() {
        initialCell = new H3Cell(613503380827930701L, 45.0, 10.0);
        initialCell.setPopulation(1000000000); // 1 Billion
        initialCell.setResourceCapital(5000.0);
        initialCell.setResourceMetal(1000.0);
        initialCell.setPollutionLevel(100.0);
        initialCell.setTechnologyLevel(2.0);
        initialCell.setFoodResource(10000.0);
        initialCell.setLifespan(70.0);
    }

    @Test
    public void testWorld3VsKurzweilComparisonExecution() {
        World3VsKurzweilComparator.ComparisonReport report =
            World3VsKurzweilComparator.compareTrajectories(initialCell, 2026, 30);

        assertNotNull(report, "Comparator should return a non-null comparison report.");
        assertTrue(report.world3PeakPopulation > 0, "World3 peak population should be positive.");
        assertTrue(report.kurzweilPeakPopulation > 0, "Kurzweil peak population should be positive.");
        assertTrue(report.kurzweilTechLevel2050 > initialCell.getTechnologyLevel(), "Kurzweil LOAR should accelerate tech level.");
        assertNotNull(report.dominantDriverSummary, "Summary should describe dominant system driver.");
    }
}
