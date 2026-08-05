/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.i18n.I18n;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Standardized Preset Toolbar with inline editable name field.
 *
 * Behaviour:
 * - Starts empty (no preset selected by default).
 * - Selecting a preset from the ComboBox populates the name field.
 * - If the user modifies any parameter (notified via {@link #notifyParametersChanged()}),
 *   the name field is cleared to signal an unsaved custom state.
 * - The name field is always directly editable (user can type a new name or rename).
 * - Saving uses the text currently in the name field.
 * - Renaming an existing preset: edit the name field + Save → updates the preset name.
 *
 * @param <T> Preset type
 * @author Silvere Martin-Michiellot
 */
public class PresetControlBar<T> extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(PresetControlBar.class);

    /** Prefix category used for export file naming (e.g. "planetgenerator", "ecology", "scenario") */
    private String exportCategory = "preset";

    private final Label presetLabel;
    private final ComboBox<T> presetCombo;

    /**
     * Inline editable name field.
     * - Populated when a preset is selected.
     * - Cleared when parameters change (unsaved custom state).
     * - Always writable to allow naming/renaming.
     */
    private final TextField nameField;

    private final Button saveBtn;
    private final Button deleteBtn;
    private final Button exportBtn;
    private final Button importBtn;

    /** When true, parameter changes should clear the name field. */
    private boolean trackingChanges = false;

    /** Floating toast notification shown inside the control bar */
    private Label toastLabel;

    private PresetActionsListener<T> listener;

    public interface PresetActionsListener<T> {
        void onPresetSelected(T preset);
        /** Called when user saves with the given name (new or renamed). */
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

        // --- Inline editable name field ---
        nameField = new TextField();
        nameField.setPromptText(I18n.getOrDefault("preset.name.placeholder", "Nom du préréglage…"));
        nameField.setMaxWidth(Double.MAX_VALUE);
        nameField.setTooltip(new Tooltip(I18n.getOrDefault("preset.name.tooltip",
                "Nom du préréglage. Modifiez-le librement puis cliquez sur Enregistrer.")));
        HBox.setHgrow(nameField, Priority.ALWAYS);

        nameField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isBlank()) {
                nameField.setStyle(""); // has name → normal style
            }
        });

        // --- ComboBox (no default selection — starts blank) ---
        presetCombo = new ComboBox<>();
        presetCombo.setMaxWidth(Double.MAX_VALUE);
        presetCombo.setPrefWidth(220);
        presetCombo.setPromptText(I18n.getOrDefault("preset.combo.placeholder", "— Choisir un préréglage —"));
        HBox.setHgrow(presetCombo, Priority.ALWAYS);

        presetCombo.setConverter(new StringConverter<T>() {
            @Override public String toString(T object) { return formatPresetItem(object); }
            @Override public T fromString(String string) { return null; }
        });

        presetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatPresetItem(item));
            }
        });

        presetCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : formatPresetItem(item));
            }
        });

        presetCombo.setOnAction(e -> {
            T selected = presetCombo.getValue();
            if (selected != null) {
                // Populate name field with selected preset name
                String name = formatPresetItem(selected);
                nameField.setText(name);
                nameField.setStyle(""); // reset unsaved style
                trackingChanges = true;
                if (listener != null) {
                    listener.onPresetSelected(selected);
                }
            }
        });

        HBox topRow = new HBox(8, presetCombo);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(presetCombo, Priority.ALWAYS);

        HBox nameRow = new HBox(8, presetLabel, nameField);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameField, Priority.ALWAYS);

        // --- Action buttons ---
        saveBtn = new Button();
        saveBtn.getStyleClass().add("button-secondary");
        saveBtn.setOnAction(e -> doSave());
        HBox.setHgrow(saveBtn, Priority.ALWAYS);
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        deleteBtn = new Button();
        deleteBtn.getStyleClass().add("button-secondary");
        deleteBtn.setOnAction(e -> promptDelete());
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

        // Toast label (hidden by default)
        toastLabel = new Label();
        toastLabel.setStyle("-fx-background-color: rgba(239,68,68,0.9); -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 12px;");
        toastLabel.setVisible(false);
        toastLabel.setManaged(false);

        getChildren().addAll(topRow, nameRow, btnBox, toastLabel);

        updateTexts();
        updateTooltips();
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Set the export category used for file name prefix. */
    public void setExportCategory(String category) {
        this.exportCategory = category;
    }

    public String getExportCategory() {
        return exportCategory;
    }

    /**
     * Sets the preset list. No item is selected by default (blank state).
     * Pass a non-null defaultItem to pre-select one.
     */
    public void setPresets(List<T> items, T defaultItem) {
        presetCombo.getItems().setAll(items);
        if (defaultItem != null && items.contains(defaultItem)) {
            presetCombo.setValue(defaultItem);
            nameField.setText(formatPresetItem(defaultItem));
            trackingChanges = true;
        } else {
            // Start blank: no selection, empty name
            presetCombo.setValue(null);
            nameField.setText("");
            trackingChanges = false;
        }
    }

    /**
     * Call this whenever a parameter slider/field is modified by the user.
     * If a preset was selected, the name field is cleared to indicate
     * the configuration is now in an unsaved custom state.
     */
    public void notifyParametersChanged() {
        if (trackingChanges || presetCombo.getValue() != null) {
            nameField.clear();
            nameField.setPromptText(I18n.getOrDefault("preset.name.unsaved", "Configuration non sauvegardée…"));
            nameField.setStyle("-fx-prompt-text-fill: #f59e0b; -fx-font-style: italic;");
            // Deselect combo without triggering its action
            presetCombo.getSelectionModel().clearSelection();
            trackingChanges = false;
        }
    }

    /**
     * Sets the text of the inline name field.
     */
    public void setNameText(String name) {
        if (nameField != null) {
            nameField.setText(name != null ? name : "");
            nameField.setStyle("");
        }
    }

    public ComboBox<T> getPresetCombo() {
        return presetCombo;
    }

    /** Returns the current name typed in the name field. */
    public String getCurrentName() {
        return nameField.getText().trim();
    }

    public void setListener(PresetActionsListener<T> listener) {
        this.listener = listener;
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Save with the name currently in the name field.
     * If the field is empty, shows an inline prompt asking for a name.
     */
    private void doSave() {
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            // Prompt inline if no name
            TextInputDialog dialog = new TextInputDialog(
                    I18n.getOrDefault("preset.dialog.default_name", "Mon Préréglage"));
            dialog.setTitle(I18n.getOrDefault("preset.dialog.save_title", "Enregistrer le Préréglage"));
            dialog.setHeaderText(I18n.getOrDefault("preset.dialog.save_header", "Entrez un nom pour ce préréglage :"));
            dialog.setContentText(I18n.getOrDefault("preset.dialog.save_label", "Nom :"));
            dialog.showAndWait().ifPresent(enteredName -> {
                if (!enteredName.isBlank()) {
                    nameField.setText(enteredName.trim());
                    nameField.setStyle("");
                    performSave(enteredName.trim());
                }
            });
        } else {
            performSave(name);
        }
    }

    private void performSave(String name) {
        boolean exists = presetCombo.getItems().stream()
                .anyMatch(item -> item != null && formatPresetItem(item).equalsIgnoreCase(name));

        if (exists) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle(I18n.getOrDefault("preset.dialog.overwrite_title", "Préréglage Existant"));
            confirm.setHeaderText(I18n.getOrDefault("preset.dialog.overwrite_header", "Remplacement de préréglage"));
            confirm.setContentText(I18n.getOrDefault("preset.dialog.overwrite_content",
                    "Un préréglage nommé '" + name + "' existe déjà. Voulez-vous l'écraser ?"));
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        trackingChanges = true;
        if (listener != null) {
            listener.onSavePreset(name);
        }
        showToast(I18n.getOrDefault("preset.toast.saved", "✅ Préréglage enregistré : ") + name,
                "-fx-background-color: rgba(16,185,129,0.9);");
    }

    private void promptDelete() {
        T selected = presetCombo.getValue();
        if (selected == null) {
            showToast(I18n.getOrDefault("preset.toast.no_selection", "⚠️ Sélectionnez un préréglage à supprimer."),
                    "-fx-background-color: rgba(234,179,8,0.9);");
            return;
        }

        String name = formatPresetItem(selected);

        toastLabel.setText(I18n.getOrDefault("preset.toast.confirm_delete", "⚠️ Supprimer « ") + name + " » ?");
        toastLabel.setStyle("-fx-background-color: rgba(239,68,68,0.92); -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 12px;");
        toastLabel.setVisible(true);
        toastLabel.setManaged(true);

        Button confirmBtn = new Button(I18n.getOrDefault("preset.btn.confirm_delete", "Confirmer la suppression"));
        confirmBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button(I18n.getOrDefault("preset.btn.cancel", "Annuler"));
        cancelBtn.getStyleClass().add("button-secondary");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);

        HBox confirmRow = new HBox(6, confirmBtn, cancelBtn);
        confirmRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(confirmBtn, Priority.ALWAYS);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);
        getChildren().add(confirmRow);

        confirmBtn.setOnAction(ev -> {
            getChildren().remove(confirmRow);
            hideToast();
            if (listener != null) {
                listener.onDeletePreset(selected);
                presetCombo.getItems().remove(selected);
                nameField.clear();
                trackingChanges = false;
                showToast(I18n.getOrDefault("preset.toast.deleted", "🗑️ Préréglage supprimé : ") + name,
                        "-fx-background-color: rgba(239,68,68,0.85);");
            }
        });

        cancelBtn.setOnAction(ev -> {
            getChildren().remove(confirmRow);
            hideToast();
        });
    }

    private void promptExport() {
        T selected = presetCombo.getValue();
        if (selected == null) {
            showToast(I18n.getOrDefault("preset.toast.no_selection", "⚠️ Sélectionnez un préréglage à exporter."),
                    "-fx-background-color: rgba(234,179,8,0.9);");
            return;
        }

        String presetName = nameField.getText().trim().isBlank()
                ? formatPresetItem(selected)
                : nameField.getText().trim();

        String safeName = presetName.toLowerCase()
                .replaceAll("[^a-z0-9\\-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (safeName.isBlank()) safeName = "custom";

        String suggestedFileName = "ether-" + exportCategory + "-" + safeName + ".json";

        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("preset.dialog.export_title", "Exporter le Préréglage (JSON)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));
        chooser.setInitialFileName(suggestedFileName);

        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null && listener != null) {
            listener.onExportPreset(file, selected);
            showToast(I18n.getOrDefault("preset.toast.exported", "📤 Exporté : ") + file.getName(),
                    "-fx-background-color: rgba(16,185,129,0.9);");
        }
    }

    private void promptImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("preset.dialog.import_title", "Importer un Préréglage (JSON)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null && listener != null) {
            listener.onImportPreset(file);
            showToast(I18n.getOrDefault("preset.toast.imported", "📥 Importé : ") + file.getName(),
                    "-fx-background-color: rgba(16,185,129,0.9);");
        }
    }

    // -------------------------------------------------------------------------
    // Toast notification helpers
    // -------------------------------------------------------------------------

    private void showToast(String message, String styleOverride) {
        toastLabel.setText(message);
        toastLabel.setStyle(styleOverride + " -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 12px;");
        toastLabel.setVisible(true);
        toastLabel.setManaged(true);
        toastLabel.setOpacity(1.0);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(500), toastLabel);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(f -> hideToast());
            fade.play();
        });
        pause.play();
    }

    private void hideToast() {
        toastLabel.setVisible(false);
        toastLabel.setManaged(false);
        toastLabel.setOpacity(1.0);
    }

    // -------------------------------------------------------------------------
    // i18n
    // -------------------------------------------------------------------------

    public void updateTexts() {
        if (saveBtn != null) saveBtn.setText("💾 " + I18n.get("preset.save"));
        if (deleteBtn != null) deleteBtn.setText("🗑️ " + I18n.get("preset.delete"));
        if (exportBtn != null) exportBtn.setText("📤 " + I18n.get("preset.export"));
        if (importBtn != null) importBtn.setText("📥 " + I18n.get("preset.import"));
        if (presetCombo != null)
            presetCombo.setPromptText(I18n.getOrDefault("preset.combo.placeholder", "— Choisir un préréglage —"));
        if (nameField != null)
            nameField.setPromptText(I18n.getOrDefault("preset.name.placeholder", "Nom du préréglage…"));
        updateTooltips();
    }

    private void updateTooltips() {
        if (presetCombo != null) presetCombo.setTooltip(new Tooltip(
                I18n.getOrDefault("preset.tooltip.combo", "Sélectionner un préréglage existant")));
        if (nameField != null) nameField.setTooltip(new Tooltip(
                I18n.getOrDefault("preset.name.tooltip", "Nom du préréglage — éditable directement pour nommer ou renommer")));
        if (saveBtn != null) saveBtn.setTooltip(new Tooltip(
                I18n.getOrDefault("planet.tooltip.preset_save", "Enregistrer la configuration actuelle sous ce nom")));
        if (deleteBtn != null) deleteBtn.setTooltip(new Tooltip(
                I18n.getOrDefault("planet.tooltip.preset_delete", "Supprimer le préréglage sélectionné")));
        if (exportBtn != null) exportBtn.setTooltip(new Tooltip(
                I18n.getOrDefault("planet.tooltip.preset_export", "Exporter le préréglage en JSON (ether-category-name.json)")));
        if (importBtn != null) importBtn.setTooltip(new Tooltip(
                I18n.getOrDefault("planet.tooltip.preset_import", "Importer un fichier de préréglage JSON")));
    }

    private String formatPresetItem(T item) {
        if (item == null) return "";
        if (item instanceof org.ether.society.procedural.PlanetPreset p) return p.name();
        if (item instanceof org.ether.society.model.EcologyPreset e) return e.name();
        try {
            var method = item.getClass().getMethod("name");
            Object val = method.invoke(item);
            if (val != null) return val.toString();
        } catch (Exception ignored) {}
        try {
            var method = item.getClass().getMethod("getName");
            Object val = method.invoke(item);
            if (val != null) return val.toString();
        } catch (Exception ignored) {}
        return item.toString();
    }
}
