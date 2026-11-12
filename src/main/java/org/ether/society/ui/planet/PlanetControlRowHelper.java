/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * UI row builder and bidirectional synchronization helper for planet generator sliders and spinners.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PlanetControlRowHelper {

    private PlanetControlRowHelper() {
    }

    public static VBox createControlRow(Label label, javafx.scene.Node control, String tooltipText) {
        label.getStyleClass().add("control-label");
        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            if (control instanceof Control ctrl) ctrl.setTooltip(tt);
        }
        return new VBox(4, label, control);
    }

    public static VBox createControlRow(Label label, Slider slider, String formatPattern, String tooltipText) {
        label.getStyleClass().add("control-label");
        Label valLabel = new Label(String.format(formatPattern, slider.getValue()));
        valLabel.getStyleClass().add("value-label");
        slider.valueProperty().addListener((obs, old, val) -> valLabel.setText(String.format(formatPattern, val.doubleValue())));

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            slider.setTooltip(tt);
            valLabel.setTooltip(tt);
        }

        HBox header = new HBox(label, new Pane(), valLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
    }

    public static void commitEditorDoubleText(Spinner<Double> spinner) {
        if (spinner == null || !spinner.isEditable()) return;
        String text = spinner.getEditor().getText();
        if (text == null || text.isBlank()) return;
        String cleanText = text.trim().replace(',', '.').replaceAll("[^0-9.-]", "");
        try {
            double value = Double.parseDouble(cleanText);
            SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                    (SpinnerValueFactory.DoubleSpinnerValueFactory) spinner.getValueFactory();
            value = Math.clamp(value, factory.getMin(), factory.getMax());
            factory.setValue(value);
        } catch (NumberFormatException ignored) { org.slf4j.LoggerFactory.getLogger(PlanetControlRowHelper.class).debug("Non-fatal condition in PlanetControlRowHelper: {}", ignored.getMessage()); }
    }

    public static VBox createControlRowWithSpinner(
            Label label, Slider slider, Spinner<Double> spinner, String formatPattern, String tooltipText,
            BooleanSupplier isPresetUpdating, Runnable onNotifyChangeAndPreview) {

        label.getStyleClass().add("control-label");
        spinner.setEditable(true);
        spinner.setPrefWidth(95);
        spinner.setMaxWidth(110);
        spinner.getStyleClass().add("dense-spinner");

        spinner.getEditor().setOnAction(e -> commitEditorDoubleText(spinner));
        spinner.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) commitEditorDoubleText(spinner);
        });

        final boolean[] syncing = {false};
        slider.valueProperty().addListener((obs, oldV, newV) -> {
            if (isPresetUpdating.getAsBoolean() || syncing[0]) return;
            syncing[0] = true;
            try {
                spinner.getValueFactory().setValue(Math.round(newV.doubleValue() * 10.0) / 10.0);
            } finally {
                syncing[0] = false;
            }
        });

        spinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (isPresetUpdating.getAsBoolean() || syncing[0] || newV == null) return;
            syncing[0] = true;
            try {
                slider.setValue(newV);
                onNotifyChangeAndPreview.run();
            } finally {
                syncing[0] = false;
            }
        });

        String unit = formatPattern.replaceAll("[^a-zA-ZÂ°â˜‰%Â²Â³/]", "").trim();
        Label unitLbl = new Label(unit);
        unitLbl.getStyleClass().add("unit-label");
        HBox valBox = new HBox(4, spinner, unitLbl);
        valBox.setAlignment(Pos.CENTER_RIGHT);

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            slider.setTooltip(tt);
            spinner.setTooltip(tt);
        }

        HBox header = new HBox(label, new Pane(), valBox);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
    }
}

