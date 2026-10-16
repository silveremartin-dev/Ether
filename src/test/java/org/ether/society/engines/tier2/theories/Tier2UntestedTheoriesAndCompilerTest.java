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
        marsDome.setTechnologyLevel(5.0);
        marsDome.setResourceCapital(500.0);
        marsDome.setResourceMetal(200.0);
        domeCells.add(marsDome);

        double initialCapital = marsDome.getResourceCapital();
        IsruAutarkyAndSpaceColonizationEngine.processSpaceColonizationCliodynamics(domeCells, PlanetPreset.MARS_LIKE, 1.0);

        // ISRU extraction on off-world colony should generate capital and metal
        assertNotNull(marsDome.getResourceCapital());
        assertTrue(marsDome.getResourceCapital() >= initialCapital, "ISRU operations must maintain or grow capital stock in domes");
    }

    @Test
    @DisplayName("SovereignAIGovernanceEngine: Unified Leviathan reallocates capital and reduces pollution")
    void testSovereignAIGovernanceEngineLeviathan() {
        List<H3Cell> planetaryCells = new ArrayList<>();
        H3Cell pollutedCell = new H3Cell(0x851f1d4bfffffc2L, 0.0, 0.0);
        pollutedCell.setElevation(50.0);
        pollutedCell.setBiome(Biome.PLAINS);
        pollutedCell.setPopulation(50000);
        pollutedCell.setResourceCapital(10000.0);
        pollutedCell.setPollutionLevel(800.0);
        pollutedCell.setGiniIndex(0.65);
        planetaryCells.add(pollutedCell);

        SovereignAIGovernanceEngine aiGovernor = new SovereignAIGovernanceEngine(
                SovereignAIGovernanceEngine.GovernanceMode.LEVIATHAN_UNIFIED,
                0.9, 100.0, 0.25
        );

        double initialPollution = pollutedCell.getPollutionLevel();
        aiGovernor.process(planetaryCells, 1.0);

        assertTrue(pollutedCell.getPollutionLevel() < initialPollution,
                "Leviathan AI Governor must systematically scrub pollution towards target ceilings");
    }
}
