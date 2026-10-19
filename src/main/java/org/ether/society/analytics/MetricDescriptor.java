*
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
 * @version 1.0.0-beta.2
 */
public class MetricDescriptor {

    public enum Category {
        ENERGY_MATTER("⚡ Energy & Matter"),
        DEMOGRAPHICS("👥 Demographics & Health"),
        SOCIETY_POLITICS("🏛️ Society & Institutions"),
        ECONOMY("💎 Economy & Wealth"),
        COGNITION("🧠 Cognition & Information"),
        ECOLOGY("🌍 Ecology & Planetary Boundaries"),
        CLIODYNAMICS("⏳ Cliodynamics & Systemic Risks"),
        COMPLEXITY("⚙️ Systemic Complexity"),
        PERFORMANCE("💻 Technical Performance");

        /** Internal state: display name (String). */
        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Get display name.
         *
         * @return the resulting computation or state reference
         */
        public String getDisplayName() {
            return org.ether.society.i18n.I18n.getOrDefault("metric.category." + name().toLowerCase(), displayName);
        }

        /**
         * To string.
         *
         * @return the resulting computation or state reference
         */
        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    /** Internal state: id (String). */
    private final String id;
    /** Internal state: display name (String). */
    private final String displayName;
    private final Category category;
    /** Internal state: unit (String). */
    private final String unit;
    /** Internal state: description (String). */
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

    /**
     * Get id.
     *
     * @return the resulting computation or state reference
     */
    public String getId() { return id; }
    /**
     * Get display name.
     *
     * @return the resulting computation or state reference
     */
    public String getDisplayName() {
        return org.ether.society.i18n.I18n.getOrDefault("metric." + id, displayName);
    }
    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    public Category getCategory() { return category; }
    /**
     * Get unit.
     *
     * @return the resulting computation or state reference
     */
    public String getUnit() { return unit; }
    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return org.ether.society.i18n.I18n.getOrDefault("metric." + id + ".desc", description);
    }
    /**
     * Get spatial extractor.
     *
     * @return the resulting computation or state reference
     */
    public Function<H3Cell, Double> getSpatialExtractor() { return spatialExtractor; }
    /**
     * Get spatial aggregator.
     *
     * @return the resulting computation or state reference
     */
    public Function<List<H3Cell>, Double> getSpatialAggregator() { return spatialAggregator; }

    /**
     * Extract cell.
     *
     * @param cell the cell parameter (H3Cell)
     * @return the resulting computation or state reference
     */
    public double extractCell(H3Cell cell) {
        if (cell == null) return 0.0;
        return spatialExtractor.apply(cell);
    }

    /**
     * Aggregate.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @return the resulting computation or state reference
     */
    public double aggregate(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return 0.0;
        return spatialAggregator.apply(cells);
    }
}

