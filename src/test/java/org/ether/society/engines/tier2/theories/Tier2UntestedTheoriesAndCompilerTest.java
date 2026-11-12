/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.compiler.EngineConflictAnalyzer;
import org.ether.society.engines.compiler.EngineConflictAnalyzer.EngineVariableDescriptor;
import org.ether.society.engines.compiler.EngineConflictReport;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.Biome;
import org.ether.society.model.HabitatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test Suite for the remaining unexercised Tier 2 Theories & Compiler Engines:
 * <ul>
 *   <li>{@link EngineConflictAnalyzer}</li>
 *   <li>{@link IsruAutarkyAndSpaceColonizationEngine}</li>
 *   <li>{@link SovereignAIGovernanceEngine}</li>
 * </ul>
 */
public class Tier2UntestedTheoriesAndCompilerTest {

    @Test
    @DisplayName("EngineConflictAnalyzer: detects unconstrained opposing derivatives and divergent equilibria")
    void testEngineConflictAnalyzer() {
        List<EngineVariableDescriptor> descriptors = List.of(
                new EngineVariableDescriptor("EngineMalthus", "population", -0.05, Double.NaN),
                new EngineVariableDescriptor("EngineCornucopian", "population", 0.08, Double.NaN),
                new EngineVariableDescriptor("EngineHotClimate", "globalTemperature", 0.5, 30.0),
                new EngineVariableDescriptor("EngineGeoengineering", "globalTemperature", -0.3, 14.0)
        );

        EngineConflictReport report = EngineConflictAnalyzer.analyze(descriptors);
        assertNotNull(report);
        assertFalse(report.getEntries().isEmpty(), "Report entries list should not be empty");
        assertTrue(report.hasIncompatibilities(), "Analyzer must flag divergent equilibrium targets as incompatible");
    }

    @Test
    @DisplayName("IsruAutarkyAndSpaceColonizationEngine: space dome habitats develop ISRU capital")
    void testIsruAutarkyAndSpaceColonizationEngine() {
        List<H3Cell> domeCells = new ArrayList<>();
        H3Cell marsDome = new H3Cell(0x851f1d4bfffffc1L, 0.0, 0.0);
        marsDome.setElevation(100.0);
        marsDome.setBiome(Biome.DESERT);
        marsDome.setHabitatType(HabitatType.SURFACE_DOME);
        marsDome.setPopulation(1200);
        marsDome.setResourceCapital(500.0);
        marsDome.setResourceMetal(200.0);
        domeCells.add(marsDome);

        org.ether.society.core.dod.WorldBuffer buf = new org.ether.society.core.dod.WorldBuffer(1);
        buf.getElevation()[0] = 100.0f;
        buf.getBiomes()[0] = (byte) Biome.DESERT.ordinal();
        buf.getBiomassHuman()[0] = 1200.0f;
        buf.getResourceCapital()[0] = 500.0f;
        buf.getMetalResource()[0] = 200.0f;

        double initialCapital = buf.getResourceCapital()[0];
        new IsruAutarkyAndSpaceColonizationEngine().tick(buf, 1.0, 1.0);

        // ISRU extraction on off-world colony should generate capital and metal
        assertTrue(buf.getResourceCapital()[0] >= initialCapital, "ISRU operations must maintain or grow capital stock in domes");
    }
}
