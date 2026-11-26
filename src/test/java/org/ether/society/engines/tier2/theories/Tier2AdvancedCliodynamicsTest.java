/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.generation.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.engines.*;
import org.ether.society.engines.tier1.*;
import org.ether.society.engines.tier2.theories.*;
import org.ether.society.engines.tier2.historical.*;
import org.ether.society.engines.compiler.*;

import org.ether.society.core.dod.WorldBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying advanced Tier 2 cliodynamic plugins:
 * 1. Price Multilevel Cultural Selection Engine
 * 2. Boserupian Agricultural Intensification Engine
 * 3. Arthur Combinatorial Technology Evolution Engine
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class Tier2AdvancedCliodynamicsTest {

    @Test
    @DisplayName("Price Equation Engine: Multilevel cultural selection on civic altruism")
    public void testPriceMultilevelSelection() {
        PriceMultilevelSelectionEngine engine = new PriceMultilevelSelectionEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Price"));

        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 1000.0f;
        buffer.getGiniIndex()[0] = 0.20f; // Low inequality -> high cooperation
        buffer.getResourceCapital()[0] = 500.0f;
        buffer.getInstitutionalComplexity()[0] = 0.2f;

        float capBefore = buffer.getResourceCapital()[0];
        float instBefore = buffer.getInstitutionalComplexity()[0];
        engine.tick(buffer, 1.0, 12.0);

        assertTrue(buffer.getResourceCapital()[0] > capBefore, "High civic prosociality must increase collective physical capital output");
        assertTrue(buffer.getInstitutionalComplexity()[0] > instBefore, "High civic prosociality must strengthen institutional complexity");
    }

    @Test
    @DisplayName("Boserup Intensification Engine: Demographic pressure triggering capital deepening")
    public void testBoserupIntensification() {
        BoserupAgriculturalIntensificationEngine engine = new BoserupAgriculturalIntensificationEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Boserup"));

        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 100_000.0f; // Very high population density
        buffer.getResourceCapital()[0] = 500.0f;

        float capBefore = buffer.getResourceCapital()[0];
        engine.tick(buffer, 1.0, 12.0);

        assertTrue(buffer.getResourceCapital()[0] > capBefore, "High demographic density must force agricultural capital intensification");
    }

    @Test
    @DisplayName("Arthur Combinatorial Technology Engine: Recombinant innovation dynamics")
    public void testArthurCombinatorialTechnology() {
        ArthurCombinatorialTechnologyEngine engine = new ArthurCombinatorialTechnologyEngine();
        assertNotNull(engine.getEquationsTooltip());
        assertTrue(engine.getEquationsTooltip().contains("Arthur"));

        WorldBuffer buffer = new WorldBuffer(1);
        buffer.getHumanPopulation()[0] = 50_000.0f;
        buffer.getResourceCapital()[0] = 10_000.0f;

        float capBefore = buffer.getResourceCapital()[0];
        engine.tick(buffer, 1.0, 12.0);

        assertTrue(buffer.getResourceCapital()[0] > capBefore, "Combinatorial capital dynamics must accelerate physical capital accumulation");
    }
}


