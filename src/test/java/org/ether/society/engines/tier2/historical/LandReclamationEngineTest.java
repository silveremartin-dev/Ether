/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Land Reclamation (Poldérisation & Floating Seasteading Platforms).
 */
public class LandReclamationEngineTest {

    @Test
    @DisplayName("Polderisation: Coastal cell with sufficient tech and capital transforms into reclaimed plains")
    public void testPolderReclamation() {
        H3Cell coastalCell = new H3Cell(4001L, 52.5, 5.0); // Dutch coast
        coastalCell.setBiome(Biome.BEACH);
        coastalCell.setIsCoastal(true);
        coastalCell.setElevation(-1.0); // Below sea level
        coastalCell.setTechnologyLevel(4.5); // >= 4.0 threshold
        coastalCell.setResourceCapital(150.0); // >= 100.0 threshold
        coastalCell.setSoilOrganicCarbon(40.0);
        coastalCell.setIsPolder(false);

        LandReclamationEngine.processHybrid(List.of(coastalCell), 1.0);

        assertTrue(coastalCell.getIsPolder(), "Reclaimed cell must be flagged as polder");
        assertEquals(Biome.PLAINS, coastalCell.getBiome(), "Marine/beach biome must transition to PLAINS");
        assertEquals(100.0, coastalCell.getSoilOrganicCarbon(), 1e-3, "Polder drainage must floor soil organic carbon at minimum fertile threshold 100.0");
    }

    @Test
    @DisplayName("Seasteading: Deep ocean with trans-human tech & capital deploys floating infrastructure")
    public void testSeasteadingDeployment() {
        H3Cell oceanCell = new H3Cell(4002L, 0.0, -140.0); // Pacific Ocean
        oceanCell.setBiome(Biome.OCEAN);
        oceanCell.setElevation(-4000.0);
        oceanCell.setTechnologyLevel(9.0); // >= 8.5 threshold
        oceanCell.setResourceCapital(600.0); // >= 500.0 threshold
        oceanCell.setEnergySolar(20.0);
        oceanCell.setHasFloatingInfrastructure(false);

        LandReclamationEngine.processHybrid(List.of(oceanCell), 1.0);

        assertTrue(oceanCell.getHasFloatingInfrastructure(), "Deep-sea cell must deploy floating infrastructure");
        assertEquals(70.0, oceanCell.getEnergySolar(), 1e-3, "Seasteading array must capture +50 solar/oceanic energy");
    }
}
