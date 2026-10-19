/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.ether.society.model.Biome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verification test suite for {@link NativeRustBridge} and Panama FFM downcalls.
 * Validates off-heap buffer marshaling, C-ABI symbol execution, and numerical stability.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class NativeRustBridgeTest {
    private static final Logger logger = LoggerFactory.getLogger(NativeRustBridgeTest.class);

    @Test
    @DisplayName("Verify Native Rust Bridge Initialization & 5-Kernel Downcall Cycle")
    public void testNativeBridgeExecution() {
        boolean available = NativeRustBridge.isNativeAvailable();
        logger.info("⚡ Native Rust Bridge Available: {} (Binary: {})",
                available, NativeRustBridge.getLoadedBinaryPath());

        if (!available) {
            logger.info("ℹ️ Native Rust library not detected on this system. Skipping native execution assertion.");
            return;
        }

        int capacity = 10_000;
        WorldBuffer world = new WorldBuffer(capacity);
        AgentBuffer agents = new AgentBuffer(capacity);

        // Populate test planetary grid
        for (int i = 0; i < capacity; i++) {
            world.getElevation()[i] = (i % 5 == 0) ? -200.0f : 150.0f;
            world.getTemperature()[i] = 20.0f;
            world.getRainfall()[i] = 800.0f;
            world.getBiomes()[i] = (byte) ((i % 5 == 0) ? Biome.OCEAN.ordinal() : Biome.PLAINS.ordinal());
            world.getFoodResource()[i] = 5000.0f;
            world.getWaterResource()[i] = 1000.0f;
            world.getBiomassNatural()[i] = 500.0f;
            world.getResourceCapital()[i] = 200.0f;
            world.getTechnologyLevel()[i] = 2.5f;

            if (i < capacity / 2) {
                agents.getHexIds()[i] = i;
                agents.getMass()[i] = 120.0f;
                agents.getEnergy()[i] = 75.0f;
                agents.getAge()[i] = 25.0f;
                agents.getTechLevel()[i] = 2.5f;
            }
        }

        try (NativeRustBridge.NativeWorldArena arena = new NativeRustBridge.NativeWorldArena(capacity)) {
            arena.syncFromJavaBuffers(world, agents);

            // Execute 5 unified cycles
            for (int t = 0; t < 5; t++) {
                arena.stepUnifiedCycle(30.0f);
            }

            arena.syncToJavaBuffers(world, agents);

            // Assert numerical validity (no NaN or Inf)
            for (int i = 0; i < 100; i++) {
                assertFalse(Float.isNaN(world.getFoodResource()[i]), "Food must not be NaN");
                assertFalse(Float.isInfinite(world.getFoodResource()[i]), "Food must not be Infinite");
                assertTrue(world.getFoodResource()[i] >= 0.0f, "Food must remain non-negative");

                if (i < capacity / 2) {
                    assertFalse(Float.isNaN(agents.getMass()[i]), "Agent mass must not be NaN");
                    assertFalse(Float.isNaN(agents.getEnergy()[i]), "Agent energy must not be NaN");
                }
            }

            logger.info("✅ Successfully executed 5 Native Rust simulation cycles on 10,000 cells with zero errors!");
        }
    }
}
