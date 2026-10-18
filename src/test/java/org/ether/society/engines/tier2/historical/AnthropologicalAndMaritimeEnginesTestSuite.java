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
 * Verification test suite for Anthropological, Demographic & Evolutionary Cliodynamic Engines:
 * 1. Asymmetric Colonial Trade (Bullion drain vs perishable luxury imports).
 * 2. Marine Submersion & Early Warning Evacuation Physics.
 * 3. Human Self-Domestication (Prolonged learning & grandmother effect).
 * 4. Sexual Selection & Unmated Male Mobilization (Buss Law).
 */
public class AnthropologicalAndMaritimeEnginesTestSuite {

    @Test
    @DisplayName("Asymmetric Colonial Trade: Silver bullion drains while capital increases temporarily")
    public void testAsymmetricColonialTrade() {
        H3Cell cell = new H3Cell(5001L, 38.7, -9.1); // Lisbon coordinates
        cell.setResourceMetal(100.0);
        cell.setResourceCapital(50.0);

        AsymmetricColonialTradeEngine.processHybrid(List.of(cell), 2.0);

        // Expected metal: 100.0 - 0.5 * 2.0 = 99.0
        // Expected capital: 50.0 + 0.2 * 2.0 = 50.4
        assertEquals(99.0, cell.getResourceMetal(), 1e-4, "Colonial silver bullion drain must equal 0.5/year");
        assertEquals(50.4, cell.getResourceCapital(), 1e-4, "Colonial trade revenue must boost capital stock by 0.2/year");
    }

    @Test
    @DisplayName("Marine Submersion Engine: High tech societies evacuate up to 98% of population to safe highlands")
    public void testMarineSubmersionAndEvacuationPhysics() {
        H3Cell submergedCoastal = new H3Cell(5002L, 53.0, 6.0);
        submergedCoastal.setElevation(-2.0); // Flooded below sea level
        submergedCoastal.setSeaLevelOffsetMeters(0.0);
        submergedCoastal.setPopulation(10000);
        submergedCoastal.setResourceCapital(1000.0);
        submergedCoastal.setTechnologyLevel(5.0); // Modern emergency response

        H3Cell safeHighland = new H3Cell(5003L, 53.0, 7.0);
        safeHighland.setElevation(50.0); // Safe highland
        safeHighland.setBiome(Biome.HILLS);
        safeHighland.setPopulation(1000);
        safeHighland.setResourceCapital(100.0);

        MarineSubmersionEngine.processHybrid(List.of(submergedCoastal, safeHighland), 30.0);

        // Submerged cell should have transferred almost all citizens cleanly
        assertTrue(submergedCoastal.getPopulation() < 1000,
                "High-tech coastal population must be predominantly evacuated before drowning");
        assertTrue(safeHighland.getPopulation() > 8000,
                "Safe highland cells must absorb the evacuated refugees and transferred capital");
        assertTrue(safeHighland.getResourceCapital() > 100.0,
                "Refugee evacuation must salvage moveable capital stock to safe highlands");
    }

    @Test
    @DisplayName("Human Self-Domestication: High population density accelerates linguistic and technological learning")
    public void testSelfDomesticationLearningRate() {
        H3Cell urbanCell = new H3Cell(5004L, 48.8, 2.3);
        urbanCell.setPopulation(9000); // density = 9.0 > 1.0
        urbanCell.setTechnologyLevel(2.0);

        SelfDomesticationEngine.processHybrid(List.of(urbanCell), 1.0);

        // Expected tech: 2.0 + 0.002 * log10(1 + 9.0) * 1.0 = 2.0 + 0.002 * 1.0 = 2.002
        assertEquals(2.002, urbanCell.getTechnologyLevel(), 1e-5,
                "High social density must accelerate tech development via prolonged learning by exactly 0.002/yr");
    }

    @Test
    @DisplayName("Sexual Selection & Mating: High capital accumulation creates surplus male military mobilization")
    public void testSexualSelectionMilitaryMobilization() {
        H3Cell cell = new H3Cell(5005L, 35.0, 35.0);
        cell.setResourceCapital(1000.0); // > 500.0 threshold
        cell.setResourceWork(100.0);

        SexualSelectionMatingEngine.processHybrid(List.of(cell), 2.0);

        // Expected work: 100.0 * (1.0 + 0.05 * 2.0) = 110.0
        assertEquals(110.0, cell.getResourceWork(), 1e-4,
                "Extreme elite capital concentration must mobilize surplus unmated males into +5%/year military work");
    }
}
