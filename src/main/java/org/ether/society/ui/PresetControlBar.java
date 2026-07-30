/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.i18n.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Standardized Preset Toolbar component containing:
 * - Label & Preset ComboBox
 * - Save (Enregistrer)
 * - Delete (Supprimer)
 * - Export (Exporter JSON)
 * - Import (Importer JSON)
 *
 * @param <T> Preset type
 * @author Silvere Martin-Michiellot
 */
public class PresetControlBar<T> extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(PresetControlBar.class);

    private final Label presetLabel;
    private final ComboBox<T> presetCombo;
    private final Button saveBtn;
    private final Button deleteBtn;
    private final Button exportBtn;
    private final Button importBtn;

    private PresetActionsListener<T> listener;

    public interface PresetActionsListener<T> {
        void onPresetSelected(T preset);
        void onSavePreset(String name);
        void onDeletePreset(T preset);
        void onExportPreset(File targetFile, T preset);
        void onImportPreset(File sourceFile);
    }

    public PresetControlBar(String labelText) {
        super(8);
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(10, 12, 10, 12));
        getStyleClass().add("card-section");

        presetLabel = new Label(labelText + ":");
        presetLabel.getStyleClass().add("control-label");

        presetCombo = new ComboBox<>();
        presetCombo.setMaxWidth(Double.MAX_VALUE);
        presetCombo.setPrefWidth(250);
        HBox.setHgrow(presetCombo, Priority.ALWAYS);

        presetCombo.setOnAction(e -> {
            if (listener != null && presetCombo.getValue() != null) {
                listener.onPresetSelected(presetCombo.getValue());
            }
        });

        HBox topRow = new HBox(8, presetLabel, presetCombo);
        topRow.setAlignment(Pos.CENTER_LEFT);

        saveBtn = new Button();
        saveBtn.getStyleClass().add("button-secondary");
        saveBtn.setOnAction(e -> promptSave());
        HBox.setHgrow(saveBtn, Priority.ALWAYS);
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        deleteBtn = new Button();
        deleteBtn.getStyleClass().add("button-secondary");
        deleteBtn.setOnAction(e -> {
            T selected = presetCombo.getValue();
            if (selected != null && listener != null) {
                listener.onDeletePreset(selected);
            }
        });
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setMaxWidth(Double.MAX_VALUE);

        exportBtn = new Button();
        exportBtn.getStyleClass().add("button-secondary");
        exportBtn.setOnAction(e -> promptExport());
        HBox.setHgrow(exportBtn, Priority.ALWAYS);
        exportBtn.setMaxWidth(Double.MAX_VALUE);

        importBtn = new Button();
        importBtn.getStyleClass().add("button-secondary");
        importBtn.setOnAction(e -> promptImport());
        HBox.setHgrow(importBtn, Priority.ALWAYS);
        importBtn.setMaxWidth(Double.MAX_VALUE);

        HBox row1 = new HBox(6, saveBtn, deleteBtn);
        row1.setAlignment(Pos.CENTER);
        HBox row2 = new HBox(6, exportBtn, importBtn);
        row2.setAlignment(Pos.CENTER);

        VBox btnBox = new VBox(6, row1, row2);

        getChildren().addAll(topRow, btnBox);

        updateTexts();
        updateTooltips();

        // Update texts on language change
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void setPresets(List<T> items, T defaultItem) {
        presetCombo.getItems().setAll(items);
        if (defaultItem != null && items.contains(defaultItem)) {
            presetCombo.setValue(defaultItem);
        } else if (!items.isEmpty()) {
            presetCombo.setValue(items.get(0));
        }
    }

    public ComboBox<T> getPresetCombo() {
        return presetCombo;
    }

    public void setListener(PresetActionsListener<T> listener) {
        this.listener = listener;
    }

    private void promptSave() {
        TextInputDialog dialog = new TextInputDialog("Mon Preset");
        dialog.setTitle("Enregistrer le Preset");
        dialog.setHeaderText("Entrez un nom pour ce preset :");
        dialog.setContentText("Nom :");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isBlank() && listener != null) {
                listener.onSavePreset(name.trim());
            }
        });
    }

    private void promptExport() {
        T selected = presetCombo.getValue();
        if (selected == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le Preset (JSON)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null && listener != null) {
            listener.onExportPreset(file, selected);
        }
    }

    private void promptImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer un Preset (JSON)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null && listener != null) {
            listener.onImportPreset(file);
        }
    }

    public void updateTexts() {
        if (saveBtn != null) saveBtn.setText("💾 " + I18n.get("preset.save"));
        if (deleteBtn != null) deleteBtn.setText("🗑️ " + I18n.get("preset.delete"));
        if (exportBtn != null) exportBtn.setText("📤 " + I18n.get("preset.export"));
        if (importBtn != null) importBtn.setText("📥 " + I18n.get("preset.import"));
        updateTooltips();
    }

    private void updateTooltips() {
        presetCombo.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset", "Préréglage complet des paramètres de simulation")));
        presetLabel.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset", "Préréglage complet des paramètres de simulation")));
        saveBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset_save", "Sauvegarder la configuration actuelle sous forme de préréglage")));
        deleteBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset_delete", "Supprimer le préréglage sélectionné")));
        exportBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset_export", "Exporter le préréglage dans un fichier JSON externe")));
        importBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset_import", "Importer un fichier de préréglage JSON")));
    }
}
