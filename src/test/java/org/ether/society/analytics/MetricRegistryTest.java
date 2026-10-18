/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit & Integration Test Suite for {@link MetricRegistry}.
 * Verifies standard metric definitions, physical units, categories, single-cell extraction,
 * and multi-cell spatial aggregations across physics, demographics, and cliodynamics.
 */
public class MetricRegistryTest {

    @Test
    @DisplayName("Verify MetricRegistry singleton and default metrics initialization")
    public void testRegistryInitialization() {
        MetricRegistry registry = MetricRegistry.getInstance();
        assertNotNull(registry, "MetricRegistry singleton must not be null");

        Collection<MetricDescriptor> metrics = registry.getAllMetrics();
        assertNotNull(metrics, "Metrics collection must not be null");
        assertFalse(metrics.isEmpty(), "MetricRegistry must contain predefined metric descriptors");
        assertTrue(metrics.size() >= 20, "MetricRegistry must define at least 20 core metrics");

        List<String> names = registry.getAllMetricNames();
        assertEquals(metrics.size(), names.size(), "Metric names list size must match all metrics collection");
    }

    @Test
    @DisplayName("Verify metric descriptor lookup by ID and Display Name")
    public void testDescriptorLookup() {
        MetricRegistry registry = MetricRegistry.getInstance();

        MetricDescriptor energyCaptured = registry.getDescriptor("energyCaptured");
        assertNotNull(energyCaptured, "energyCaptured metric descriptor must be found");
        assertEquals("MW", energyCaptured.getUnit());
        assertEquals(MetricDescriptor.Category.ENERGY_MATTER, energyCaptured.getCategory());

        MetricDescriptor byName = registry.getDescriptorByName(energyCaptured.getDisplayName());
        assertNotNull(byName, "Descriptor must be found by display name");
        assertEquals("energyCaptured", byName.getId());

        // Null / Unknown lookups
        assertNull(registry.getDescriptor(null));
        assertNull(registry.getDescriptor("non_existent_metric_xyz"));
    }

    @Test
    @DisplayName("Verify cell extraction and aggregation functions")
    public void testCellExtractionAndAggregation() {
        MetricRegistry registry = MetricRegistry.getInstance();
        MetricDescriptor energyDesc = registry.getDescriptor("energyCaptured");

        H3Cell cell1 = new H3Cell(1001L, 45.0, 10.0);
        cell1.setEnergySolar(250.0);

        H3Cell cell2 = new H3Cell(1002L, 46.0, 10.0);
        cell2.setEnergySolar(150.0);

        List<H3Cell> cells = List.of(cell1, cell2);

        // Individual cell extraction
        double val1 = energyDesc.extractCell(cell1);
        assertEquals(250.0, val1, 1e-4);

        double val2 = energyDesc.extractCell(cell2);
        assertEquals(150.0, val2, 1e-4);

        // Multi-cell aggregation
        double total = energyDesc.aggregate(cells);
        assertEquals(400.0, total, 1e-4, "Aggregated energy must be sum of cells");
    }
}
