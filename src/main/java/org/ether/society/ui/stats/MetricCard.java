/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.stats;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Interactive telemetry card for a single physical, demographic, or institutional simulation metric.
 *
 * <p>Extracted from {@link org.ether.society.ui.StatsPanel} to satisfy the Single Responsibility Principle (SRP).
 * Features localized title, numeric formatted display, measurement unit, hover tooltip, and embedded sparkline.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MetricCard extends HBox {

    private final String key;
    private String title;
    private String category;
    private final Label titleLabel;
    private final Label valueLabel;
    private final SparklineCanvas sparkline;
    private String unit;
    private String tooltipText;
    private double lastVal = 0.0;

    /**
     * Constructs a metric card with key, localized title, category, unit, and tooltip explanation.
     *
     * @param key unique metric key identifier
     * @param title display title
     * @param category thematic grouping category
     * @param unit physical or mathematical unit
     * @param tooltipText scientific explanation and formula
     */
    public MetricCard(String key, String title, String category, String unit, String tooltipText) {
        this.key = key;
        this.title = title;
        this.category = category;
        this.unit = unit;
        this.tooltipText = tooltipText;

        setPadding(new Insets(5, 8, 5, 8));
        setSpacing(8);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("card-section");

        titleLabel = new Label(title + ":");
        titleLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        titleLabel.setMinWidth(160);
        titleLabel.setMaxWidth(180);

        valueLabel = new Label("-- " + unit);
        valueLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");
        valueLabel.setMinWidth(95);

        sparkline = new SparklineCanvas();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(titleLabel, valueLabel, spacer, sparkline);

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip.install(this, new Tooltip(tooltipText));
        }
    }

    /**
     * Updates localized textual labels.
     *
     * @param title updated title
     * @param category updated category
     * @param unit updated unit
     * @param tooltipText updated tooltip
     */
    public void updateTexts(String title, String category, String unit, String tooltipText) {
        this.title = title;
        this.category = category;
        this.unit = unit;
        this.tooltipText = tooltipText;
        titleLabel.setText(title + ":");
        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip.install(this, new Tooltip(tooltipText));
        }
    }

    /**
     * Updates numeric value display and adds a sample to the sparkline.
     *
     * @param displayValue formatted value string
     * @param rawNumericValue raw double value
     */
    public void updateValue(String displayValue, double rawNumericValue) {
        this.lastVal = rawNumericValue;
        valueLabel.setText(displayValue + (unit.isEmpty() ? "" : " " + unit));
        sparkline.addValue(rawNumericValue);
    }

    public String getKey() { return key; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getTooltipText() { return tooltipText; }
    public double getLastVal() { return lastVal; }
    public SparklineCanvas getSparkline() { return sparkline; }
}
