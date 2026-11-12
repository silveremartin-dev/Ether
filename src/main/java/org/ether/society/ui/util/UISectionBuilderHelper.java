/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Standardized UI layout and control factory helper for panel builder sections.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class UISectionBuilderHelper {

    private UISectionBuilderHelper() {}

    public static VBox createSection(Label header, VBox content) {
        VBox card = new VBox(8, header, content);
        card.getStyleClass().add("card-section");
        card.setPadding(new Insets(10));
        return card;
    }

    public static Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(false);
        slider.getStyleClass().add("slider-modern");
        return slider;
    }

    public static VBox createControlRow(Label label, Node control, String tooltipText) {
        label.getStyleClass().add("label-control");
        if (tooltipText != null && !tooltipText.isBlank()) {
            label.setTooltip(new Tooltip(tooltipText));
        }
        VBox row = new VBox(2, label, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static VBox createControlRow(Label label, Slider slider, String formatPattern, String tooltipText) {
        label.getStyleClass().add("label-control");
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("value-label");
        valueLabel.setText(String.format(formatPattern, slider.getValue()));
        slider.valueProperty().addListener((obs, old, val) ->
                valueLabel.setText(String.format(formatPattern, val.doubleValue())));

        HBox header = new HBox(8, label, valueLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(label, Priority.ALWAYS);

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            slider.setTooltip(tt);
        }

        VBox row = new VBox(2, header, slider);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
