/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.analytics;

import org.ether.society.database.H3Cell;

import java.util.List;
import java.util.function.Function;

/**
 * Descriptor defining metadata, spatial extractors (2D), and spatial aggregators (1D)
 * for a single physical, demographic, economic, or cliodynamic metric.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class MetricDescriptor {

    public enum Category {
        ENERGY_MATTER("⚡ Énergie & Matière"),
        DEMOGRAPHICS("👥 Démographie & Santé"),
        SOCIETY_POLITICS("🏛️ Société & Institutions"),
        ECONOMY("💎 Économie & Richesse"),
        COGNITION("🧠 Cognition & Information"),
        ECOLOGY("🌍 Écologie & Frontières Planétaires"),
        CLIODYNAMICS("⏳ Cliodynamique & Risques Systémiques"),
        COMPLEXITY("⚙️ Complexité Systémique"),
        PERFORMANCE("💻 Performances Techniques");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return org.ether.society.i18n.I18n.getOrDefault("metric.category." + name().toLowerCase(), displayName);
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    private final String id;
    private final String displayName;
    private final Category category;
    private final String unit;
    private final String description;
    private final Function<H3Cell, Double> spatialExtractor;
    private final Function<List<H3Cell>, Double> spatialAggregator;

    public MetricDescriptor(String id, String displayName, Category category, String unit, String description,
                            Function<H3Cell, Double> spatialExtractor,
                            Function<List<H3Cell>, Double> spatialAggregator) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.unit = unit;
        this.description = description;
        this.spatialExtractor = spatialExtractor != null ? spatialExtractor : (cell -> 0.0);
        this.spatialAggregator = spatialAggregator != null ? spatialAggregator : (cells -> 0.0);
    }

    public String getId() { return id; }
    public String getDisplayName() {
        return org.ether.society.i18n.I18n.getOrDefault("metric." + id, displayName);
    }
    public Category getCategory() { return category; }
    public String getUnit() { return unit; }
    public String getDescription() { return description; }
    public Function<H3Cell, Double> getSpatialExtractor() { return spatialExtractor; }
    public Function<List<H3Cell>, Double> getSpatialAggregator() { return spatialAggregator; }

    public double extractCell(H3Cell cell) {
        if (cell == null) return 0.0;
        return spatialExtractor.apply(cell);
    }

    public double aggregate(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return 0.0;
        return spatialAggregator.apply(cells);
    }
}

