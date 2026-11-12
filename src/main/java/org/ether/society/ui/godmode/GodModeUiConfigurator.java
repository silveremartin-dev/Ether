/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.godmode;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.ether.society.ui.godmode.GodModeEventRegistry.EventTypeItem;

import java.util.List;

/**
 * UI layout and control factory helper for {@link org.ether.society.ui.GodModePanel}.
 *
 * <p>Configures custom {@link ComboBox} cell factories with localized tooltips and constructs
 * the physical event injector grid and card sections.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class GodModeUiConfigurator {

    /**
     * Private constructor preventing instantiation of utility configurator class.
     */
    private GodModeUiConfigurator() {
        // Utility configurator class
    }

    /**
     * Configures string conversion and tooltip-enriched list cells for the event catalog {@link ComboBox}.
     *
     * @param eventTypeCombo target combo box
     * @param catalogItems available event catalog items
     */
    public static void updateBrushItems(ComboBox<String> brushModeCombo) {
        int selIdx = brushModeCombo.getSelectionModel().getSelectedIndex();
        brushModeCombo.getItems().clear();
        brushModeCombo.getItems().addAll(
                org.ether.society.i18n.I18n.getOrDefault("godmode.brush.pop", "👥 Population Boost (+50,000 cap)"),
                org.ether.society.i18n.I18n.getOrDefault("godmode.brush.agri", "🌾 Agricultural & Silo Boost (+500 t)"),
                org.ether.society.i18n.I18n.getOrDefault("godmode.brush.water", "🚰 Aquifer Recharge (+2,000 m³)"),
                org.ether.society.i18n.I18n.getOrDefault("godmode.brush.heat", "🔥 Local Heatwave (+10.0°C)"),
                org.ether.society.i18n.I18n.getOrDefault("godmode.brush.cold", "❄️ Local Cooling (-10.0°C)"),
                org.ether.society.i18n.I18n.getOrDefault("godmode.brush.clean", "🧼 Complete Ecological Cleanup (0.0)")
        );
        brushModeCombo.getSelectionModel().select(selIdx >= 0 ? selIdx : 0);
        brushModeCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("godmode.tooltip.brush_mode", "Select local effect to apply.")));
    }

    public static void configureEventTypeCombo(ComboBox<EventTypeItem> eventTypeCombo, List<EventTypeItem> catalogItems) {
        eventTypeCombo.getItems().addAll(catalogItems);
        eventTypeCombo.setPrefWidth(220);
        eventTypeCombo.setMaxWidth(280);

        eventTypeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(EventTypeItem item) {
                return item != null ? item.getDisplayName() : "";
            }

            @Override
            public EventTypeItem fromString(String string) {
                if (string == null) return null;
                for (EventTypeItem item : catalogItems) {
                    if (item.getDisplayName().equalsIgnoreCase(string) || item.id().equalsIgnoreCase(string)) {
                        return item;
                    }
                }
                return null;
            }
        });

        eventTypeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EventTypeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.getDisplayName());
                    setTooltip(new Tooltip(item.getDescription()));
                }
            }
        });
        eventTypeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EventTypeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item.getDisplayName());
                    setTooltip(new Tooltip(item.getDescription()));
                }
            }
        });
    }

    /**
     * Assembles the Climate &amp; Physical Event Injector form grid section.
     *
     * @param injectorTitleLabel section header label
     * @param lblEventType event type label
     * @param eventTypeCombo event type selector
     * @param eventDescriptionLabel live description badge
     * @param lblEventTitle custom name label
     * @param eventNameField custom name text field
     * @param lblTargetYear target year label
     * @param targetYearSpinner target year spinner
     * @param targetYearSlider target year slider
     * @param dateRangeLabel valid date range hint label
     * @param lblLat latitude label
     * @param latSpinner latitude spinner
     * @param lblLng longitude label
     * @param lngSpinner longitude spinner
     * @param lblMag magnitude label
     * @param magnitudeSpinner magnitude spinner
     * @param lblRadius radius label
     * @param radiusKmSpinner radius spinner
     * @param lblDurationDays duration label
     * @param durationDaysSpinner duration spinner
     * @param lblDurationUnit duration unit suffix label
     * @param scheduleBtn schedule in timeline button
     * @param triggerNowBtn trigger immediately button
     * @return assembled {@link VBox} card section
     */
    public static VBox buildInjectorSection(
            Label injectorTitleLabel,
            Label lblEventType,
            ComboBox<EventTypeItem> eventTypeCombo,
            Label eventDescriptionLabel,
            Label lblEventTitle,
            TextField eventNameField,
            Label lblTargetYear,
            Spinner<Integer> targetYearSpinner,
            Spinner<Integer> targetMonthSpinner,
            Spinner<Integer> targetDaySpinner,
            Slider targetYearSlider,
            Label dateRangeLabel,
            Label lblLat,
            Spinner<Double> latSpinner,
            Label lblLng,
            Spinner<Double> lngSpinner,
            Label lblMag,
            Spinner<Double> magnitudeSpinner,
            Label lblRadius,
            Spinner<Double> radiusKmSpinner,
            Label lblDurationDays,
            Spinner<Integer> durationDaysSpinner,
            Label lblDurationUnit,
            Button scheduleBtn,
            Button triggerNowBtn) {
        VBox box = new VBox(8);
        box.getStyleClass().add("card-section");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(140);
        col0.setPrefWidth(150);
        col0.setHgrow(Priority.NEVER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col0, col1);

        grid.addRow(0, lblEventType, eventTypeCombo);
        grid.add(eventDescriptionLabel, 1, 1);
        grid.addRow(2, lblEventTitle, eventNameField);

        Label lblM = new Label("M:");
        lblM.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        Label lblJ = new Label("J:");
        lblJ.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        HBox dateRow = new HBox(6, targetYearSpinner, lblM, targetMonthSpinner, lblJ, targetDaySpinner, targetYearSlider);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        VBox yearBox = new VBox(3, dateRow, dateRangeLabel);
        grid.addRow(3, lblTargetYear, yearBox);

        grid.addRow(4, lblLat, latSpinner);
        grid.addRow(5, lblLng, lngSpinner);
        grid.addRow(6, lblMag, magnitudeSpinner);
        grid.addRow(7, lblRadius, radiusKmSpinner);

        HBox durationBox = new HBox(8, durationDaysSpinner, lblDurationUnit);
        durationBox.setAlignment(Pos.CENTER_LEFT);
        grid.addRow(8, lblDurationDays, durationBox);

        HBox btnBox = new HBox(8, scheduleBtn, triggerNowBtn);
        HBox.setHgrow(scheduleBtn, Priority.ALWAYS);
        HBox.setHgrow(triggerNowBtn, Priority.ALWAYS);

        box.getChildren().addAll(injectorTitleLabel, grid, btnBox);
        return box;
    }

    /**
     * Creates a standardized fixed-width form control label.
     *
     * @param minWidth minimum and preferred width in pixels
     * @return styled {@link Label}
     */
    public static Label createFormLabel(double minWidth) {
        Label l = new Label("");
        l.getStyleClass().add("control-label");
        l.setMinWidth(minWidth);
        l.setPrefWidth(minWidth);
        l.setWrapText(false);
        return l;
    }

    /**
     * Creates an editable double-precision {@link Spinner} with standardized dimensions.
     *
     * @param min minimum value
     * @param max maximum value
     * @param initial initial value
     * @param step increment step
     * @return configured {@link Spinner}
     */
    public static Spinner<Double> createDoubleSpinner(double min, double max, double initial, double step) {
        Spinner<Double> sp = new Spinner<>(min, max, initial, step);
        sp.setEditable(true);
        sp.setPrefWidth(95);
        sp.setMaxWidth(110);
        return sp;
    }
}
