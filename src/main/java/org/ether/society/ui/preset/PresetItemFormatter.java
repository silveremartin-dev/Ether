/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.preset;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.ether.society.i18n.I18n;

/**
 * <h1>PresetItemFormatter</h1>
 * <p>
 * Formatting, localized labeling, and list-cell rendering utilities for preset ComboBox items
 * (PlanetPreset, EcologyPreset, Scenario).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PresetItemFormatter {

    private PresetItemFormatter() {
        // Utility class
    }

    /**
     * Formats a preset item to its localized human-readable display name.
     *
     * @param <T>  preset type
     * @param item preset item instance
     * @return display string
     */
    public static <T> String formatPresetItem(T item) {
        if (item == null) return "";
        if (item instanceof org.ether.society.generation.PlanetPreset p) {
            return I18n.getPlanetPresetDisplayName(p.name());
        }
        if (item instanceof org.ether.society.model.EcologyPreset e) {
            return I18n.getPlanetPresetDisplayName(e.name());
        }
        if (item instanceof org.ether.society.model.Scenario s) {
            return cleanScenarioName(s.getDisplayName());
        }
        try {
            var method = item.getClass().getMethod("name");
            Object val = method.invoke(item);
            if (val != null) return val.toString();
        } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(PresetItemFormatter.class).debug("Non-fatal condition in PresetItemFormatter: {}", ignored.getMessage()); }
        try {
            var method = item.getClass().getMethod("getName");
            Object val = method.invoke(item);
            if (val != null) return val.toString();
        } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(PresetItemFormatter.class).debug("Non-fatal condition in PresetItemFormatter: {}", ignored.getMessage()); }
        return item.toString();
    }

    /**
     * Cleans scenario name by removing trailing date/model annotations.
     *
     * @param name raw scenario name
     * @return cleaned name
     */
    public static String cleanScenarioName(String name) {
        if (name == null) return "";
        return name.replaceAll("\\s*\\((?:\\-?\\d+|An 0|Year 0|SSP[0-9\\-\\.]+)\\)\\s*$", "").trim();
    }

    /**
     * Extracts formatted date text if the preset is a historical scenario.
     *
     * @param <T>  preset type
     * @param item preset item instance
     * @return formatted date string or null
     */
    public static <T> String getPresetDateText(T item) {
        if (item instanceof org.ether.society.model.Scenario s) {
            long year = s.getStartDateYear();
            if (year < 0) {
                String bcFmt = I18n.getOrDefault("scenario.date.bc", "%,d BCE");
                return String.format(bcFmt, Math.abs(year)).replace(',', ' ');
            } else if (year == 0) {
                return I18n.getOrDefault("scenario.date.year_zero", "Year 0");
            } else {
                return String.valueOf(year);
            }
        }
        return null;
    }

    /**
     * Retrieves detailed description for a preset item for tooltip display.
     *
     * @param <T>  preset type
     * @param item preset item instance
     * @return detailed description or null
     */
    public static <T> String getPresetDetailedDescription(T item) {
        if (item == null) return null;
        if (item instanceof org.ether.society.generation.PlanetPreset p) {
            return p.getPresetDescription();
        }
        if (item instanceof org.ether.society.model.EcologyPreset e) {
            return e.getPresetDescription();
        }
        if (item instanceof org.ether.society.model.Scenario s) {
            String desc = s.getDisplayDescription();
            if (desc != null && !desc.isBlank()) return desc;
            return s.getDisplayName();
        }
        return formatPresetItem(item);
    }

    /**
     * Creates a customized {@link ListCell} for preset items displaying names, dates, and tooltips.
     *
     * @param <T> preset type
     * @return list cell instance
     */
    public static <T> ListCell<T> createPresetListCell() {
        return new ListCell<>() {
            private final Label nameLabel = new Label();
            private final Label dateLabel = new Label();
            private final Region spacer = new Region();
            private final HBox container = new HBox(8, nameLabel, spacer, dateLabel);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                container.setAlignment(Pos.CENTER_LEFT);
                nameLabel.getStyleClass().add("preset-cell-name");
                dateLabel.getStyleClass().add("preset-cell-date");
                dateLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setTooltip(null);
                } else {
                    String formattedName = formatPresetItem(item);
                    String dateText = getPresetDateText(item);
                    if (dateText != null && !dateText.isBlank()) {
                        nameLabel.setText(formattedName);
                        dateLabel.setText("ðŸ“… " + dateText);
                        setGraphic(container);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(formattedName);
                    }
                    String desc = getPresetDetailedDescription(item);
                    if (desc != null && !desc.isBlank()) {
                        Tooltip tip = new Tooltip(desc);
                        tip.setWrapText(true);
                        tip.setMaxWidth(480);
                        setTooltip(tip);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        };
    }
}

