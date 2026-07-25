/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.flux;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FluxEngineTest {

    private FluxEngine fluxEngine;
    private WorldBuffer worldBuffer;

    @BeforeEach
    void setUp() {
        fluxEngine = new FluxEngine();
        worldBuffer = new WorldBuffer(4); // 4 test cells
        
        // Initialize neighbor indexes to -1
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                worldBuffer.getNeighborIndexes()[i][j] = -1;
            }
        }
    }

    @Test
    @DisplayName("Should act as supply when high food and low population")
    void testPressureSupply() {
        // High food, zero pop for cell 0
        worldBuffer.getFoodResource()[0] = 1000.0f;
        worldBuffer.getBiomassHuman()[0] = 0.0f;
        
        fluxEngine.tick(worldBuffer, 1.0f);
        
        // Price = (Pop + 1) / (Food + 1) = 1 / 1001 ~ 0.00099
        float price = worldBuffer.getLocalPrice()[0];
        assertTrue(price < 0.01f, "Price should be low for abundant food and zero pop");
    }

    @Test
    @DisplayName("Should flow food from high supply to high demand cell")
    void testFluxFlow() {
        // Cell 0: Source (High food, zero pop -> low price)
        worldBuffer.getFoodResource()[0] = 2000.0f;
        worldBuffer.getBiomassHuman()[0] = 0.0f;
        worldBuffer.getElevation()[0] = 100.0f;

        // Cell 1: Target (Zero food, high pop -> high price)
        worldBuffer.getFoodResource()[1] = 0.0f;
        worldBuffer.getBiomassHuman()[1] = 100.0f;
        worldBuffer.getElevation()[1] = 100.0f;

        // Connect cell 0 to cell 1
        worldBuffer.getNeighborIndexes()[0][0] = 1;
        worldBuffer.getNeighborIndexes()[1][0] = 0;

        float initialFoodSource = worldBuffer.getFoodResource()[0];
        float initialFoodTarget = worldBuffer.getFoodResource()[1];

        // Act: Process Flux
        fluxEngine.tick(worldBuffer, 10.0f);

        // Assert
        assertTrue(worldBuffer.getFoodResource()[0] < initialFoodSource, "Source should have lost food");
        assertTrue(worldBuffer.getFoodResource()[1] > initialFoodTarget, "Target should have gained food");
    }

    @Test
    @DisplayName("Mountains should reduce conductivity (increase friction)")
    void testConductivity() {
        // Scenario 1: Plains->Plains (Cell 0 -> Cell 1)
        worldBuffer.getFoodResource()[0] = 2000.0f; worldBuffer.getBiomassHuman()[0] = 0.0f; worldBuffer.getElevation()[0] = 100.0f;
        worldBuffer.getFoodResource()[1] = 0.0f; worldBuffer.getBiomassHuman()[1] = 100.0f; worldBuffer.getElevation()[1] = 100.0f;
        worldBuffer.getNeighborIndexes()[0][0] = 1; worldBuffer.getNeighborIndexes()[1][0] = 0;

        // Scenario 2: Mountains->Mountains (Cell 2 -> Cell 3)
        worldBuffer.getFoodResource()[2] = 2000.0f; worldBuffer.getBiomassHuman()[2] = 0.0f; worldBuffer.getElevation()[2] = 100.0f;
        worldBuffer.getFoodResource()[3] = 0.0f; worldBuffer.getBiomassHuman()[3] = 100.0f; worldBuffer.getElevation()[3] = 3000.0f; // High mountain slope
        worldBuffer.getNeighborIndexes()[2][0] = 3; worldBuffer.getNeighborIndexes()[3][0] = 2;

        fluxEngine.tick(worldBuffer, 10.0f);

        float plainsFlux = 2000.0f - worldBuffer.getFoodResource()[0];
        float mountainFlux = 2000.0f - worldBuffer.getFoodResource()[2];

        assertTrue(mountainFlux < plainsFlux, "Flux through mountains should be less than plains condition: " + mountainFlux + " vs " + plainsFlux);
    }
}

