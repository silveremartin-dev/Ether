/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import org.ether.society.data.DataSourceMetadataRegistry;
import org.ether.society.i18n.I18n;

/**
 * JavaFX presentation helper for displaying data sources, dropdown formatting,
 * and binding scientific metadata tooltips to ComboBox controls.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class DataSourceUIHelper {

    private DataSourceUIHelper() {
        // Utility class
    }

    /**
     * Configures a JavaFX ComboBox with rich scientific dataset labels and hover tooltips.
     *
     * @param combo target JavaFX ComboBox
     * @param promptKey prompt message localization key
     * @param defaultTooltipKey default tooltip localization key
     */
    public static void setupDetailedSourceCombo(ComboBox<String> combo, String promptKey, String defaultTooltipKey) {
        if (combo == null) return;

        combo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank() || "none".equalsIgnoreCase(item.trim())) {
                    setText(I18n.getOrDefault(promptKey, "— Select Data Source —"));
                    setTooltip(null);
                } else {
                    String label = item;
                    if ("earth".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.earth", "🌍 Terre — NOAA ETOPO2022 / GMTED2010 [Global, -100ka à Actuel]");
                    else if ("mars".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.mars", "🔴 Mars — MGS MOLA Global Elevation [Planétaire, Éon Actuel]");
                    else if ("venus".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.venus", "🟡 Vénus — Magellan SAR & VIRTIS [Planétaire, Éon Actuel]");
                    else if ("moon".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.moon", "⚪ Lune — LRO LOLA Altimetry [Planétaire, Éon Actuel]");
                    else if ("mercury".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.mercury", "⚪ Mercure — MESSENGER MLA Topography [Planétaire, Éon Actuel]");

                    setText(label);
                    String desc = DataSourceMetadataRegistry.getDetailedSourceDescription(item);
                    if (desc != null && !desc.isBlank()) {
                        Tooltip cellTooltip = new Tooltip(desc);
                        cellTooltip.setWrapText(true);
                        cellTooltip.setMaxWidth(480);
                        setTooltip(cellTooltip);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });

        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank() || "none".equalsIgnoreCase(item.trim())) {
                    setText(I18n.getOrDefault(promptKey, "— Select Data Source —"));
                } else {
                    String label = item;
                    if ("earth".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.earth", "🌍 Terre — NOAA ETOPO2022 / GMTED2010 [Global, -100ka à Actuel]");
                    else if ("mars".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.mars", "🔴 Mars — MGS MOLA Global Elevation [Planétaire, Éon Actuel]");
                    else if ("venus".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.venus", "🟡 Vénus — Magellan SAR & VIRTIS [Planétaire, Éon Actuel]");
                    else if ("moon".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.moon", "⚪ Lune — LRO LOLA Altimetry [Planétaire, Éon Actuel]");
                    else if ("mercury".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.mercury", "⚪ Mercure — MESSENGER MLA Topography [Planétaire, Éon Actuel]");
                    setText(label);
                }
            }
        });

        Runnable updateComboTooltip = () -> {
            String val = combo.getValue();
            if (val == null || val.isBlank() || "none".equalsIgnoreCase(val.trim())) {
                Tooltip tip = new Tooltip(I18n.getOrDefault(defaultTooltipKey,
                        "Select a reference scientific data source from the dropdown list."));
                tip.setWrapText(true);
                tip.setMaxWidth(480);
                combo.setTooltip(tip);
            } else {
                String desc = DataSourceMetadataRegistry.getDetailedSourceDescription(val);
                Tooltip tip = new Tooltip(desc);
                tip.setWrapText(true);
                tip.setMaxWidth(480);
                combo.setTooltip(tip);
            }
        };

        combo.valueProperty().addListener((obs, oldV, newV) -> updateComboTooltip.run());
        updateComboTooltip.run();
    }

    /**
     * Selects the best matching item in a ComboBox based on target string or fallback keyword.
     *
     * @param combo target ComboBox
     * @param target target item or substring
     * @param fallbackKeyword fallback keyword if target match fails
     */
    public static void selectBestSource(ComboBox<String> combo, String target, String fallbackKeyword) {
        if (combo == null || combo.getItems() == null || combo.getItems().isEmpty()) return;
        if (target != null && combo.getItems().contains(target)) {
            combo.setValue(target);
            return;
        }
        if (target != null && !target.isBlank()) {
            String cleanTarget = target.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            for (String it : combo.getItems()) {
                if (it != null && !it.isEmpty()) {
                    String cleanIt = it.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                    if (cleanIt.contains(cleanTarget) || cleanTarget.contains(cleanIt)) {
                        combo.setValue(it);
                        return;
                    }
                }
            }
        }
        if (fallbackKeyword != null && !fallbackKeyword.isBlank()) {
            String kw = fallbackKeyword.toLowerCase();
            for (String it : combo.getItems()) {
                if (it != null && it.toLowerCase().contains(kw)) {
                    combo.setValue(it);
                    return;
                }
            }
        }
    }
}
