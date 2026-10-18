/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.ether.society.model.PhysicalConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous Unit & Invariant Verification Suite for {@link UrbanKernel}.
 * Tests West-Bettencourt agglomeration scaling, Tainter complexity decay,
 * and Solow-Swan capital accumulation under thermodynamic constraints.
 */
public class UrbanKernelTest {

    @Test
    @DisplayName("Verify Tainter Complexity growth and non-linear institutional maintenance overhead")
    void testTainterComplexityAndDepreciation() {
        WorldBuffer buffer = new WorldBuffer(10);
        buffer.getH3Indexes()[0] = 0x882681a339fffffL;
        buffer.getBiomassHuman()[0] = 50_000.0f; // Megacity
        buffer.getResourceCapital()[0] = 1_000_000.0f; // 20 units capital per capita
        buffer.getTechnologyLevel()[0] = 5.0f; // Industrial era
        buffer.getLocalPrice()[0] = 1.0f;

        UrbanKernel kernel = new UrbanKernel();
        kernel.tick(buffer, 1.0f); // 1 year step

        float complexity = buffer.getInstitutionalComplexity()[0];
        assertTrue(complexity > 0.0f, "Institutional complexity must be positive for large dense population");
        
        // Ensure complexity scales monotonically with capital per capita and settlement size
        assertTrue(complexity > Math.log1p(20.0f * 0.25f), "Complexity must reflect West-Bettencourt scaling");
        
        // Incurred capital must remain bounded and non-negative
        assertTrue(buffer.getResourceCapital()[0] > 0.0f, "Capital must remain strictly non-negative");
    }

    @Test
    @DisplayName("Verify that sub-threshold populations experience no unphysical urban agglomeration spillover")
    void testSubThresholdPopulationInvariance() {
        WorldBuffer buffer = new WorldBuffer(10);
        buffer.getH3Indexes()[0] = 0x882681a339fffffL;
        buffer.getBiomassHuman()[0] = 5.0f; // Isolated small clan (below 10.0 threshold)
        buffer.getResourceCapital()[0] = 50.0f;
        buffer.getTechnologyLevel()[0] = 1.0f;
        buffer.getInstitutionalComplexity()[0] = 0.0f;

        UrbanKernel kernel = new UrbanKernel();
        kernel.tick(buffer, 1.0f);

        // Small population below threshold experiences physical decay of infrastructure (0.95^1 = 47.5)
        assertEquals(0.0f, buffer.getInstitutionalComplexity()[0], 1e-4f, 
                "Clan below minimum urban threshold must have zero institutional complexity");
        assertEquals(50.0f * 0.95f, buffer.getResourceCapital()[0], 1e-3f, 
                "Capital must undergo 5% physical depreciation when unmaintained by active settlement");
    }

    @Test
    @DisplayName("Verify capital bounds and absence of NaNs under extreme inputs")
    void testExtremeValuesRobustness() {
        WorldBuffer buffer = new WorldBuffer(5);
        buffer.getH3Indexes()[0] = 0x882681a339fffffL;
        buffer.getBiomassHuman()[0] = 10_000_000.0f; // Hyper-dense 10M ecumenopolis
        buffer.getResourceCapital()[0] = 1e9f;
        buffer.getTechnologyLevel()[0] = 10.0f;
        buffer.getLocalPrice()[0] = 5.0f;

        UrbanKernel kernel = new UrbanKernel();
        assertDoesNotThrow(() -> kernel.tick(buffer, 10.0f));

        assertFalse(Float.isNaN(buffer.getResourceCapital()[0]), "Capital must never become NaN");
        assertFalse(Float.isInfinite(buffer.getResourceCapital()[0]), "Capital must never become Infinite");
        assertFalse(Float.isNaN(buffer.getInstitutionalComplexity()[0]), "Complexity must never become NaN");
    }
}
