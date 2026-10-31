/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test for {@link DeepTimePaleoEpochGenerator}.
 */
public class DeepTimePaleoEpochGeneratorTest {

    @Test
    public void testGenerateAllDeepTimeEpochs() {
        DeepTimePaleoEpochGenerator.generateAllDeepTimeEpochs();

        long[] epochs = DeepTimePaleoEpochGenerator.DEEP_TIME_EPOCHS;
        for (long epoch : epochs) {
            File epochDir = new File("data/ether/earth/" + epoch);
            assertTrue(epochDir.exists(), "Epoch dir must exist: " + epochDir);
            assertTrue(new File(epochDir, "state_1d.json").exists(), "state_1d.json must exist in " + epochDir);
            assertTrue(new File(epochDir, "cultural_registry.json").exists(), "cultural_registry.json must exist in " + epochDir);
            assertTrue(new File(epochDir, "provenance_and_sources.json").exists(), "provenance_and_sources.json must exist in " + epochDir);
            assertTrue(new File(epochDir, "README.md").exists(), "README.md must exist in " + epochDir);
            assertTrue(new File(epochDir, "earth_" + epoch + "_density.png").exists(), "Density map must exist in " + epochDir);
        }
    }
}
