/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OceanAcidificationCouplingTest {

    @Test
    public void testAcidificationReducesMarineYields() {
        List<H3Cell> cells = new ArrayList<>();
        H3Cell oceanCell = new H3Cell();
        oceanCell.setBiome(Biome.OCEAN);
        oceanCell.setTemperature(10.0);
        oceanCell.setBiomassFish(1000.0);
        cells.add(oceanCell);

        // High CO2 (700 ppm) induces ocean acidification
        OceanAcidificationEngine.processOceanAcidification(cells, 700.0);

        assertTrue(oceanCell.getBiomassFish() < 1000.0,
                "Elevated atmospheric CO2 and ocean acidification must reduce marine biomass fish yield");
    }
}