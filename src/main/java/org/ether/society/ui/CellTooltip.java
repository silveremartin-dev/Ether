/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Tooltip panel that displays detailed information about an H3 cell.
 * Shows when hovering over cells in the map view.
 */
public class CellTooltip extends VBox {

    private final Label biomeLabel;
    private final Label elevationLabel;
    private final Label temperatureLabel;
    private final Label rainfallLabel;
    private final Label popLabel;
    private final Label foodLabel;
    private final Label waterLabel;
    private final Label techLabel;
    private final Label malthusLabel;
    private final Label wealthVarianceLabel;
    private final Label coordLabel;
    private final Label h3Label;

    public CellTooltip() {
        // Container styling - glassmorphism dark HUD panel
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.95);" +
                "-fx-padding: 12;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: rgba(56, 189, 248, 0.4);" +
                "-fx-border-radius: 8;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 12, 0, 0, 3);");

        setPadding(new Insets(10));
        setSpacing(4);

        // Create labels with consistent styling
        biomeLabel = createLabel();
        elevationLabel = createLabel();
        temperatureLabel = createLabel();
        rainfallLabel = createLabel();
        popLabel = createLabel();
        popLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        foodLabel = createLabel();
        waterLabel = createLabel();
        techLabel = createLabel();
        wealthVarianceLabel = createLabel();
        wealthVarianceLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 11px; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        malthusLabel = createLabel();
        coordLabel = createLabel();
        h3Label = createLabel();

        // Add all labels to container
        getChildren().addAll(
                biomeLabel,
                elevationLabel,
                temperatureLabel,
                rainfallLabel,
                popLabel,
                foodLabel,
                waterLabel,
                techLabel,
                wealthVarianceLabel,
                malthusLabel,
                coordLabel,
                h3Label);

        // Initially hidden
        setVisible(false);
        setManaged(false); // Don't affect parent layout
    }

    /**
     * Create a styled label for tooltip content.
     */
    private Label createLabel() {
        Label label = new Label();
        label.setStyle("-fx-text-fill: #e2e8f0;" +
                "-fx-font-size: 11px;" +
                "-fx-font-family: 'Consolas', 'Monaco', monospace;");
        return label;
    }

    /**
     * Update tooltip with cell data and previous population state for trend calculation.
     * 
     * @param cell The H3 cell to display information for
     * @param prevPop Previous tick/year population count (can be null if unknown)
     */
    public void updateCell(H3Cell cell, Integer prevPop) {
        if (cell == null) {
            setVisible(false);
            return;
        }

        biomeLabel.setText(String.format("%-14s %s", I18n.getOrDefault("ui.tooltip.biome", "Biomes :"),
                getBiomeName(cell.getBiome().name())));

        elevationLabel.setText(String.format("%-14s %,d m", I18n.getOrDefault("ui.tooltip.elevation", "Altitude :"),
                cell.getElevation() != null ? cell.getElevation().intValue() : 0));

        temperatureLabel.setText(String.format("%-14s %.1f°C", I18n.getOrDefault("ui.tooltip.temperature", "Temperature:"),
                cell.getTemperature() != null ? cell.getTemperature() : 0.0));

        rainfallLabel.setText(String.format("%-14s %,d mm/an", I18n.getOrDefault("ui.tooltip.rainfall", "Precipitation:"),
                cell.getRainfall() != null ? cell.getRainfall().intValue() : 0));

        // Population & Trend Arrow
        int currentPop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        String trendStr = "";
        if (prevPop != null && prevPop > 0) {
            int diff = currentPop - prevPop;
            double pct = ((double) diff / prevPop) * 100.0;
            if (diff > 0) {
                trendStr = String.format(" (🟢 ⬆ +%,d | +%.1f%%)", diff, pct);
            } else if (diff < 0) {
                trendStr = String.format(" (🔴 ⬇ %,d | %.1f%%)", diff, pct);
            } else {
                trendStr = " (⚪ ➡ 0.0%)";
            }
        }
        popLabel.setText(String.format("%-14s %,d hab%s", "Population :", currentPop, trendStr));

        // Resources
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
        foodLabel.setText(String.format("%-14s %.1f t", "Alimentation :", food));

        double water = cell.getWaterResource() != null ? cell.getWaterResource() : 0.0;
        double aquifer = cell.getFreshwaterAquifer() != null ? cell.getFreshwaterAquifer() : 0.0;
        waterLabel.setText(String.format("%-14s %.1f m³ (Aquifer: %.0f)", I18n.getOrDefault("tooltip.water_aquifer", "Water / Aquifer:"), water, aquifer));

        // Tech level
        double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
        techLabel.setText(String.format("%-14s Niv. %.2f", "Technologie :", tech));

        // Wealth & Individual Variance Indicator
        double wealth = currentPop * tech * 15.0;
        wealthVarianceLabel.setText(String.format("%-14s G$ %,.0f (Tech Level x Pop)", I18n.getOrDefault("tooltip.mesh_wealth", "Cell Wealth:"), wealth));

        // Carrying Capacity (K) & Malthusian Ratio
        double capK = computeCarryingCapacity(cell);
        double ratio = capK > 0 ? ((double) currentPop / capK) * 100.0 : 0.0;
        String malthusStatus = ratio > 150.0 ? "🚨 SURPOPULATION" : ratio > 100.0 ? "⚠️ TENSION" : "✅ SOUTENABLE";
        malthusLabel.setText(String.format("%-14s %.0f hab (Load: %.1f%% %s)", I18n.getOrDefault("tooltip.capacity_k", "Carrying Cap K:"), capK, ratio, malthusStatus));

        String latDir = cell.getLatitude() >= 0 ? "N" : "S";
        String lngDir = cell.getLongitude() >= 0 ? "E" : "W";
        coordLabel.setText(String.format("%-14s %.4f°%s, %.4f°%s", I18n.getOrDefault("ui.tooltip.coords", "Coordinates:"),
                Math.abs(cell.getLatitude()), latDir,
                Math.abs(cell.getLongitude()), lngDir));

        h3Label.setText(String.format("%-14s %s", I18n.getOrDefault("ui.tooltip.h3", "H3 Index :"),
                Long.toHexString(cell.getH3Index()).toUpperCase()));

        setVisible(true);
    }

    public void updateCell(H3Cell cell) {
        updateCell(cell, null);
    }

    private double computeCarryingCapacity(H3Cell c) {
        if (c == null || c.getElevation() == null || c.getElevation() <= 0) return 0.0;
        double baseCap = 250.0;
        if (c.getBiome() != null) {
            switch (c.getBiome()) {
                case DESERT, TUNDRA, SNOW -> baseCap *= 0.1;
                case PLAINS, FOREST -> baseCap *= 1.5;
                case JUNGLE -> baseCap *= 0.8;
                default -> {}
            }
        }
        if (c.getWaterResource() != null && c.getWaterResource() > 0.1) {
            baseCap *= (1.0 + 3.0 * (c.getWaterResource() / 1000.0));
        }
        if (c.getFreshwaterAquifer() != null && c.getFreshwaterAquifer() > 0.1) {
            baseCap *= (1.0 + 1.5 * (c.getFreshwaterAquifer() / 1000.0));
        }
        double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
        baseCap *= Math.max(0.5, tech * 0.8);
        return Math.max(10.0, baseCap);
    }

    /**
     * Update tooltip from DOD buffers.
     */
    public void updateFromBuffer(org.ether.society.core.dod.WorldBuffer world, int index) {
        if (world == null || index < 0) {
            setVisible(false);
            return;
        }

        biomeLabel.setText(String.format("%-14s %s", I18n.getOrDefault("ui.tooltip.biome", "Biomes :"),
                getBiomeName(org.ether.society.model.Biome.values()[world.getBiomes()[index]].name())));

        elevationLabel.setText(String.format("%-14s %,d m", I18n.getOrDefault("ui.tooltip.elevation", "Altitude :"),
                (int)world.getElevation()[index]));

        temperatureLabel.setText(String.format("%-14s %.1f°C", I18n.getOrDefault("ui.tooltip.temperature", "Temperature:"),
                world.getTemperature()[index]));

        rainfallLabel.setText(String.format("%-14s %,d mm/an", I18n.getOrDefault("ui.tooltip.rainfall", "Precipitation:"),
                (int)world.getRainfall()[index]));

        popLabel.setText(String.format("%-14s %,.0f hab", "Population :", world.getBiomassHuman()[index]));
        foodLabel.setText("");
        waterLabel.setText("");
        techLabel.setText("");
        malthusLabel.setText("");

        coordLabel.setText(String.format("%-14s Index #%d", "Index DOD :", index));

        h3Label.setText(String.format("%-14s %s", I18n.getOrDefault("ui.tooltip.h3", "H3 Index :"),
                Long.toHexString(world.getH3Indexes()[index]).toUpperCase()));

        setVisible(true);
    }

    /**
     * Get localized biome name.
     */
    private String getBiomeName(String biomeEnumName) {
        String key = "biome." + biomeEnumName.toLowerCase();
        return I18n.getOrDefault(key, biomeEnumName);
    }

    /**
     * Position the tooltip at screen coordinates, ensuring it stays within bounds.
     * 
     * @param sceneX X coordinate in scene
     * @param sceneY Y coordinate in scene
     * @param maxX   Maximum X (container width)
     * @param maxY   Maximum Y (container height)
     */
    public void position(double sceneX, double sceneY, double maxX, double maxY) {
        // Offset from cursor
        double offsetX = 15;
        double offsetY = 15;

        // Calculate position
        double x = sceneX + offsetX;
        double y = sceneY + offsetY;

        // Get tooltip dimensions (may not be accurate until rendered once)
        double width = getWidth() > 0 ? getWidth() : 250; // Estimated width
        double height = getHeight() > 0 ? getHeight() : 150; // Estimated height

        // Keep within bounds
        if (x + width > maxX) {
            x = sceneX - width - 5; // Show on left of cursor
        }
        if (y + height > maxY) {
            y = sceneY - height - 5; // Show above cursor
        }

        // Ensure not negative
        x = Math.max(0, x);
        y = Math.max(0, y);

        setLayoutX(x);
        setLayoutY(y);
    }

    /**
     * Hide the tooltip.
     */
    public void hide() {
        setVisible(false);
    }
}
