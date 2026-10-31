/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.HistoricalValidationKernel.BenchmarkVariableMeta;
import org.ether.society.analytics.HistoricalValidationKernel.InterpolationMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Automated Generator & Verification Test for 1D State Baseline Variables across all Cartographic Epochs.
 *
 * <p>Generates standardized continuous 1D state vectors for every single epoch currently
 * existing in {@code data/ether/<planet>/<year>/}:</p>
 * <ul>
 *   <li>Earth: 257 epochs (-100,000 BP to +2060 CE)</li>
 *   <li>Mars: 2026, 2050</li>
 *   <li>Moon: 2026, 2035</li>
 *   <li>Mercury: 2026, 2120</li>
 *   <li>Titan: 2150</li>
 *   <li>Venus: 2026, 2080</li>
 * </ul>
 *
 * <p>Output locations:</p>
 * <ul>
 *   <li>{@code data/variables/ether/<planet>/<year>.json}</li>
 *   <li>{@code data/ether/<planet>/<year>/state_1d.json}</li>
 * </ul>
 */
public class GenerateAllEpochVariables1DTest {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    @DisplayName("Generate and Validate 1D State Variables for All Planetary Epochs with Maps")
    void testGenerateAllEpochVariables1D() throws IOException {
        File mapsEtherDir = new File("data/ether");
        assertTrue(mapsEtherDir.exists() && mapsEtherDir.isDirectory(), "data/ether directory must exist");

        File[] planets = mapsEtherDir.listFiles(File::isDirectory);
        assertTrue(planets != null && planets.length > 0, "Planets must exist in data/ether");

        int totalGenerated = 0;
        Set<String> benchmarkKeys = HistoricalValidationKernel.getAllBenchmarkKeys();

        for (File planetDir : planets) {
            String planetName = planetDir.getName().toLowerCase(Locale.ROOT);
            File[] epochDirs = planetDir.listFiles(File::isDirectory);
            if (epochDirs == null) continue;

            File varPlanetDir = new File("data/variables/ether/" + planetName);
            if (!varPlanetDir.exists()) varPlanetDir.mkdirs();

            for (File epochDir : epochDirs) {
                String epochStr = epochDir.getName();
                int year;
                try {
                    year = Integer.parseInt(epochStr);
                } catch (NumberFormatException e) {
                    continue; // Skip non-year directories if any
                }

                Map<String, Object> state1D = buildEpochState1D(planetName, year, benchmarkKeys);

                // 1. Write to data/variables/ether/<planet>/<year>.json
                File varFile = new File(varPlanetDir, epochStr + ".json");
                mapper.writeValue(varFile, state1D);

                // 2. Write to data/ether/<planet>/<year>/state_1d.json
                File mapStateFile = new File(epochDir, "state_1d.json");
                mapper.writeValue(mapStateFile, state1D);

                totalGenerated++;
            }
        }

        System.out.println("âœ… Successfully generated 1D baseline state files for " + totalGenerated + " planetary epochs!");
        assertTrue(totalGenerated >= 250, "Expected at least 250 planetary epochs to be generated");
    }

    private Map<String, Object> buildEpochState1D(String planet, int year, Set<String> benchmarkKeys) {
        Map<String, Object> root = new LinkedHashMap<>();

        // Metadata block
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("planet", planet);
        meta.put("year", year);
        meta.put("temporalWindow", year < 0 ? Math.abs(year) + " BP" : year + " CE");
        
        String refModel;
        if (!"earth".equalsIgnoreCase(planet)) {
            refModel = "MISSION_PIONEER_SPECS";
        } else if (year < -10000) {
            refModel = "PALEO_SOURCES";
        } else if (year < 2025) {
            refModel = "HYDE_MADDISON";
        } else {
            refModel = "SSP2";
        }
        meta.put("referenceModel", refModel);
        meta.put("modelDescription", HistoricalValidationKernel.getReferenceModelDisplayLabel(refModel));
        root.put("metadata", meta);

        // Variables block
        Map<String, Object> vars = new LinkedHashMap<>();

        if ("earth".equalsIgnoreCase(planet)) {
            for (String key : benchmarkKeys) {
                double val = HistoricalValidationKernel.getInterpolatedBenchmarkValue(key, year, InterpolationMethod.PCHIP_MONOTONE_CUBIC);
                BenchmarkVariableMeta varMeta = HistoricalValidationKernel.getBenchmarkMetadata(key);

                Map<String, Object> varEntry = new LinkedHashMap<>();
                varEntry.put("value", Double.isNaN(val) ? 0.0 : val);
                varEntry.put("unit", varMeta != null ? varMeta.unit() : "");
                varEntry.put("name", varMeta != null ? varMeta.name() : key);
                varEntry.put("source", varMeta != null ? varMeta.source() : refModel);
                vars.put(key, varEntry);
            }
        } else {
            // Extraterrestrial mission pioneer baseline
            vars.putAll(buildExtraterrestrialMissionVariables(planet, year));
        }

        root.put("variables", vars);
        return root;
    }

    private Map<String, Object> buildExtraterrestrialMissionVariables(String planet, int year) {
        Map<String, Object> vars = new LinkedHashMap<>();
        double pop;
        double capital;
        double energy;
        double tech;

        switch (planet) {
            case "mars" -> {
                pop = year >= 2050 ? 5000.0 : 120.0;
                capital = 45000.0;
                energy = 180000.0;
                tech = 8.5;
            }
            case "moon" -> {
                pop = year >= 2035 ? 150.0 : 24.0;
                capital = 65000.0;
                energy = 220000.0;
                tech = 8.2;
            }
            case "venus" -> {
                pop = year >= 2080 ? 1200.0 : 50.0;
                capital = 85000.0;
                energy = 300000.0;
                tech = 9.2;
            }
            case "mercury" -> {
                pop = 300.0;
                capital = 95000.0;
                energy = 500000.0;
                tech = 9.5;
            }
            case "titan" -> {
                pop = 800.0;
                capital = 110000.0;
                energy = 400000.0;
                tech = 9.6;
            }
            default -> {
                pop = 100.0;
                capital = 50000.0;
                energy = 200000.0;
                tech = 8.0;
            }
        }

        vars.put("regionalPopulation", Map.of("value", pop, "unit", "people", "name", "Colony Population"));
        vars.put("capitalPerCapita", Map.of("value", capital, "unit", "2011 US$ PPP", "name", "Initial Physical Capital"));
        vars.put("energyPerCapita", Map.of("value", energy, "unit", "MJ/capita/year", "name", "Primary Energy Allocation"));
        vars.put("technologyLevel", Map.of("value", tech, "unit", "index (0-10)", "name", "Technological Level"));
        vars.put("potableWaterPerCapita", Map.of("value", 150.0, "unit", "m3/capita/year", "name", "Recycled Potable Water"));
        vars.put("lifeExpectancy", Map.of("value", 84.0, "unit", "years", "name", "Life Expectancy"));
        vars.put("urbanizationRate", Map.of("value", 100.0, "unit", "%", "name", "Enclosed Urbanization Rate"));
        vars.put("literacyRate", Map.of("value", 100.0, "unit", "%", "name", "Mission Crew Literacy"));

        return vars;
    }
}

