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
    private final Label coordLabel;
    private final Label h3Label;

    public CellTooltip() {
        // Container styling - semi-transparent dark background
        setStyle("-fx-background-color: rgba(40, 40, 40, 0.95);" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: rgba(255, 255, 255, 0.3);" +
                "-fx-border-radius: 8;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 2);");

        setPadding(new Insets(8));
        setSpacing(4);

        // Create labels with consistent styling
        biomeLabel = createLabel();
        elevationLabel = createLabel();
        temperatureLabel = createLabel();
        rainfallLabel = createLabel();
        coordLabel = createLabel();
        h3Label = createLabel();

        // Add all labels to container
        getChildren().addAll(
                biomeLabel,
                elevationLabel,
                temperatureLabel,
                rainfallLabel,
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
        label.setStyle("-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-font-family: 'Consolas', 'Monaco', monospace;");
        return label;
    }

    /**
     * Update tooltip with cell data.
     * 
     * @param cell The H3 cell to display information for
     */
    public void updateCell(H3Cell cell) {
        if (cell == null) {
            setVisible(false);
            return;
        }

        // Format and display each field
        biomeLabel.setText(String.format("%-12s %s", I18n.get("ui.tooltip.biome"),
                getBiomeName(cell.getBiome().name())));

        elevationLabel.setText(String.format("%-12s %,d m", I18n.get("ui.tooltip.elevation"),
                cell.getElevation() != null ? cell.getElevation().intValue() : 0));

        temperatureLabel.setText(String.format("%-12s %.1fÂ°C", I18n.get("ui.tooltip.temperature"),
                cell.getTemperature() != null ? cell.getTemperature() : 0.0));

        rainfallLabel.setText(String.format("%-12s %,d mm/year", I18n.get("ui.tooltip.rainfall"),
                cell.getRainfall() != null ? cell.getRainfall().intValue() : 0));

        // Format coordinates with hemisphere indicators
        String latDir = cell.getLatitude() >= 0 ? "N" : "S";
        String lngDir = cell.getLongitude() >= 0 ? "E" : "W";
        coordLabel.setText(String.format("%-12s %.4fÂ°%s, %.4fÂ°%s", I18n.get("ui.tooltip.coords"),
                Math.abs(cell.getLatitude()), latDir,
                Math.abs(cell.getLongitude()), lngDir));

        h3Label.setText(String.format("%-12s %s", I18n.get("ui.tooltip.h3"),
                cell.getH3Index()));

        setVisible(true);
    }

    /**
     * Get localized biome name.
     */
    private String getBiomeName(String biomeEnumName) {
        String key = "biome." + biomeEnumName.toLowerCase();
        return I18n.get(key);
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

