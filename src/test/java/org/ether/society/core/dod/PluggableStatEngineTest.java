/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit & Integration Test Suite for {@link PluggableStatEngine}.
 * Tests formula registration, unregistration, evaluation over {@link H3Cell} grids and {@link WorldBuffer},
 * built-in defaults, and export/import serialization.
 */
public class PluggableStatEngineTest {

    @Test
    @DisplayName("Verify built-in default statistical formulas registration")
    public void testBuiltinDefaultsRegistered() {
        PluggableStatEngine engine = new PluggableStatEngine();
        Collection<PluggableStatEngine.StatDefinition> stats = engine.getRegisteredStats();

        assertNotNull(stats, "Registered stats must not be null");
        assertFalse(stats.isEmpty(), "Engine must initialize with built-in default stats");

        PluggableStatEngine.StatDefinition giniStat = engine.getStat("custom_gini_wealth");
        assertNotNull(giniStat, "custom_gini_wealth must be registered");
        assertTrue(giniStat.isBuiltin(), "Default stat must be marked as builtin");
        assertEquals("GINI(wealth)", giniStat.getExpression());

        PluggableStatEngine.StatDefinition foodStat = engine.getStat("custom_food_per_capita");
        assertNotNull(foodStat, "custom_food_per_capita must be registered");
    }

    @Test
    @DisplayName("Verify custom stat registration and unregistration lifecycle")
    public void testCustomStatLifecycle() {
        PluggableStatEngine engine = new PluggableStatEngine();

        PluggableStatEngine.StatDefinition customDef = new PluggableStatEngine.StatDefinition(
                "my_custom_indicator",
                "Mon Indicateur Personnalisé",
                "Test Category",
                "AVG(temperature) * 2.0",
                "°C",
                "Formule de test personnalisée",
                false
        );

        engine.registerStat(customDef);
        assertNotNull(engine.getStat("my_custom_indicator"));
        assertEquals("AVG(temperature) * 2.0", engine.getStat("my_custom_indicator").getExpression());

        // Unregister custom stat
        engine.unregisterStat("my_custom_indicator");
        assertNull(engine.getStat("my_custom_indicator"), "Custom stat must be removed after unregister");

        // Attempting to unregister a builtin stat should be rejected
        engine.unregisterStat("custom_gini_wealth");
        assertNotNull(engine.getStat("custom_gini_wealth"), "Builtin stats cannot be unregistered");
    }

    @Test
    @DisplayName("Verify computeValue over H3Cell list")
    public void testComputeValueOverH3Cells() {
        PluggableStatEngine engine = new PluggableStatEngine();

        List<H3Cell> cells = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            H3Cell c = new H3Cell(100L + i, 45.0 + i, 10.0);
            c.setPopulation((int) (1000 * (i + 1)));
            c.setTemperature(10.0 + (i * 2.0)); // 10, 12, 14, 16, 18 -> Avg = 14.0
            c.setFoodResource(2000.0);
            cells.add(c);
        }

        double avgTemp = engine.computeValue("AVG(temperature)", cells, null);
        assertEquals(14.0, avgTemp, 1e-4);

        double totalPop = engine.computeValue("SUM(population)", cells, null);
        // 1000 + 2000 + 3000 + 4000 + 5000 = 15000
        assertEquals(15000.0, totalPop, 1e-4);
    }

    @Test
    @DisplayName("Verify computeValue over WorldBuffer")
    public void testComputeValueOverWorldBuffer() {
        PluggableStatEngine engine = new PluggableStatEngine();

        int cap = 10;
        WorldBuffer buffer = new WorldBuffer(cap);
        for (int i = 0; i < cap; i++) {
            buffer.getTemperature()[i] = 20.0f + i;
            buffer.getFoodResource()[i] = 100.0f;
            buffer.getResourceCapital()[i] = 50.0f * (i + 1);
        }

        double maxTemp = engine.computeValue("MAX(temperature)", null, buffer);
        assertEquals(29.0, maxTemp, 1e-4);

        double sumFood = engine.computeValue("SUM(food)", null, buffer);
        assertEquals(1000.0, sumFood, 1e-4);
    }

    @Test
    @DisplayName("Verify Export and Import of user-defined formulas")
    public void testExportAndImport() throws Exception {
        PluggableStatEngine engine = new PluggableStatEngine();

        PluggableStatEngine.StatDefinition customStat = new PluggableStatEngine.StatDefinition(
                "exported_metric",
                "Exported Metric",
                "Export",
                "STDDEV(elevation)",
                "m",
                "Elevation standard deviation",
                false
        );
        engine.registerStat(customStat);

        File tempProps = File.createTempFile("stat_formulas_test", ".properties");
        tempProps.deleteOnExit();

        engine.exportFormulasToFile(tempProps);
        assertTrue(tempProps.exists() && tempProps.length() > 0, "Exported file must exist and not be empty");

        // Create new engine and import
        PluggableStatEngine importedEngine = new PluggableStatEngine();
        importedEngine.importFormulasFromFile(tempProps);

        PluggableStatEngine.StatDefinition retrieved = importedEngine.getStat("exported_metric");
        assertNotNull(retrieved, "Imported metric must be present in newly loaded engine");
        assertEquals("STDDEV(elevation)", retrieved.getExpression());
        assertEquals("m", retrieved.getUnit());
    }
}
