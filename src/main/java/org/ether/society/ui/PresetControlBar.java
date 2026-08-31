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

    private String labelKey;
    private String defaultLabelText;

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
        this(null, labelText);
    }

    public PresetControlBar(String labelKey, String defaultLabelText) {
        super(8);
        this.labelKey = labelKey;
        this.defaultLabelText = defaultLabelText;

        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(10, 12, 10, 12));
        getStyleClass().add("card-section");

        String initialLabelText = (labelKey != null ? I18n.getOrDefault(labelKey, defaultLabelText) : defaultLabelText);
        presetLabel = new Label(initialLabelText + ":");
        presetLabel.getStyleClass().add("control-label");

        // --- Inline editable name field ---
        nameField = new TextField();
        nameField.setPromptText(I18n.getOrDefault("preset.name.placeholder", "Preset name…"));
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
        presetCombo.setPromptText(I18n.getOrDefault("preset.combo.placeholder", "— Select a preset —"));
        HBox.setHgrow(presetCombo, Priority.ALWAYS);

        presetCombo.setConverter(new StringConverter<T>() {
            @Override public String toString(T object) { return formatPresetItem(object); }
            @Override public T fromString(String string) { return null; }
        });

        presetCombo.setCellFactory(p -> createPresetListCell());
        presetCombo.setButtonCell(createPresetListCell());

        presetCombo.setOnAction(e -> {
            T selected = presetCombo.getValue();
            if (selected != null) {
                // Populate name field with selected preset name
                String name = formatPresetItem(selected);
                nameField.setText(name);
                nameField.setStyle(""); // reset unsaved style
                trackingChanges = false; // clean state right after selection
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
     * Sets the preset list. Pre-selects defaultItem if non-null.
     */
    public void setPresets(List<T> items, T defaultItem) {
        presetCombo.getItems().setAll(items);
        if (defaultItem != null) {
            T found = null;
            for (T it : items) {
                if (it.equals(defaultItem) || formatPresetItem(it).equalsIgnoreCase(formatPresetItem(defaultItem))) {
                    found = it;
                    break;
                }
            }
            if (found != null) {
                presetCombo.setValue(found);
                nameField.setText(formatPresetItem(found));
                nameField.setStyle("");
                trackingChanges = false;
                return;
            }
        }
        // Start blank: no selection, empty name
        presetCombo.setValue(null);
        nameField.setText("");
        trackingChanges = false;
    }

    /**
     * Resets preset bar to a clean state matching the given preset item.
     */
    public void markClean(T item) {
        if (item != null) {
            T found = null;
            for (T it : presetCombo.getItems()) {
                if (it.equals(item) || formatPresetItem(it).equalsIgnoreCase(formatPresetItem(item))) {
                    found = it;
                    break;
                }
            }
            if (found != null) {
                presetCombo.setValue(found);
                nameField.setText(formatPresetItem(found));
            } else {
                nameField.setText(formatPresetItem(item));
            }
        }
        nameField.setStyle("");
        trackingChanges = false;
    }

    /**
     * Call this whenever a parameter slider/field is modified by the user.
     * If a preset was selected, the name field is cleared to indicate
     * the configuration is now in an unsaved custom state.
     */
    public void notifyParametersChanged() {
        if (trackingChanges || presetCombo.getValue() != null) {
            String current = nameField.getText() != null ? nameField.getText().trim() : "";
            if (presetCombo.getValue() != null) {
                String presetName = formatPresetItem(presetCombo.getValue());
                if (!presetName.contains("Personnalisé")) {
                    presetName = presetName + I18n.getOrDefault("preset.name.custom_suffix", " (Custom)");
                }
                nameField.setText(presetName);
            } else if (!current.contains("Personnalisé") && !current.equalsIgnoreCase("Custom")) {
                nameField.setText(current.isBlank() ? I18n.getOrDefault("preset.name.custom", "Custom") : current + I18n.getOrDefault("preset.name.custom_suffix", " (Custom)"));
            }
            nameField.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            trackingChanges = true;
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
                    I18n.getOrDefault("preset.dialog.default_name", "My Preset"));
            dialog.setTitle(I18n.getOrDefault("preset.dialog.save_title", "Save Preset"));
            dialog.setHeaderText(I18n.getOrDefault("preset.dialog.save_header", "Enter a name for this preset:"));
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
            confirm.setTitle(I18n.getOrDefault("preset.dialog.overwrite_title", "Preset Exists"));
            confirm.setHeaderText(I18n.getOrDefault("preset.dialog.overwrite_header", "Replace existing preset"));
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
        showToast(I18n.getOrDefault("preset.toast.saved", "✅ Preset saved: ") + name,
                "-fx-background-color: rgba(16,185,129,0.9);");
    }

    private void promptDelete() {
        T selected = presetCombo.getValue();
        if (selected == null) {
            showToast(I18n.getOrDefault("preset.toast.no_selection", "⚠️ Select a preset to delete."),
                    "-fx-background-color: rgba(234,179,8,0.9);");
            return;
        }

        String name = formatPresetItem(selected);

        toastLabel.setText(I18n.getOrDefault("preset.toast.confirm_delete", "⚠️ Supprimer « ") + name + " » ?");
        toastLabel.setStyle("-fx-background-color: rgba(239,68,68,0.92); -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 12px;");
        toastLabel.setVisible(true);
        toastLabel.setManaged(true);

        Button confirmBtn = new Button(I18n.getOrDefault("preset.btn.confirm_delete", "Confirm deletion"));
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
                showToast(I18n.getOrDefault("preset.toast.deleted", "🗑️ Preset deleted: ") + name,
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
            showToast(I18n.getOrDefault("preset.toast.no_selection", "⚠️ Select a preset to export."),
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
        chooser.setTitle(I18n.getOrDefault("preset.dialog.export_title", "Export Preset (JSON)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));
        chooser.setInitialFileName(suggestedFileName);

        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null && listener != null) {
            listener.onExportPreset(file, selected);
            showToast(I18n.getOrDefault("preset.toast.exported", "📤 Exported: ") + file.getName(),
                    "-fx-background-color: rgba(16,185,129,0.9);");
        }
    }

    private void promptImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("preset.dialog.import_title", "Import Preset (JSON)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null && listener != null) {
            listener.onImportPreset(file);
            showToast(I18n.getOrDefault("preset.toast.imported", "📥 Imported: ") + file.getName(),
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
        if (presetLabel != null && labelKey != null) {
            presetLabel.setText(I18n.getOrDefault(labelKey, defaultLabelText) + ":");
        }
        if (saveBtn != null) saveBtn.setText("💾 " + I18n.getOrDefault("preset.save", "Enregistrer"));
        if (deleteBtn != null) deleteBtn.setText("🗑️ " + I18n.getOrDefault("preset.delete", "Supprimer"));
        if (exportBtn != null) exportBtn.setText("📤 " + I18n.getOrDefault("preset.export", "Export"));
        if (importBtn != null) importBtn.setText("📥 " + I18n.getOrDefault("preset.import", "Import"));
        if (presetCombo != null)
            presetCombo.setPromptText(I18n.getOrDefault("preset.combo.placeholder", "— Select a preset —"));
        if (nameField != null)
            nameField.setPromptText(I18n.getOrDefault("preset.name.placeholder", "Preset name…"));
        updateTooltips();
    }

    private void updateTooltips() {
        if (presetCombo != null) presetCombo.setTooltip(new Tooltip(
                I18n.getOrDefault("preset.tooltip.combo", "Select an existing preset")));
        if (nameField != null) nameField.setTooltip(new Tooltip(
                I18n.getOrDefault("preset.name.tooltip", "Preset name — directly editable to name or rename")));
        if (saveBtn != null) saveBtn.setTooltip(new Tooltip(
                I18n.getOrDefault("planet.tooltip.preset_save", "Enregistrer la configuration actuelle sous ce nom")));
        if (deleteBtn != null) deleteBtn.setTooltip(new Tooltip(
                I18n.getOrDefault("planet.tooltip.preset_delete", "Delete selected preset")));
        if (exportBtn != null) exportBtn.setTooltip(new Tooltip(
                I18n.getOrDefault("planet.tooltip.preset_export", "Export preset as JSON (ether-category-name.json)")));
        if (importBtn != null) importBtn.setTooltip(new Tooltip(
                I18n.getOrDefault("planet.tooltip.preset_import", "Import JSON preset file")));
    }

    private String formatPresetItem(T item) {
        if (item == null) return "";
        if (item instanceof org.ether.society.procedural.PlanetPreset p) return I18n.getPlanetPresetDisplayName(p.name());
        if (item instanceof org.ether.society.model.EcologyPreset e) return e.name();
        if (item instanceof org.ether.society.model.Scenario s) {
            return cleanScenarioName(s.getName());
        }
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

    private String cleanScenarioName(String name) {
        if (name == null) return "";
        return name.replaceAll("\\s*\\((?:\\-?\\d+|An 0|SSP[0-9\\-\\.]+)\\)\\s*$", "").trim();
    }

    private String getPresetDateText(T item) {
        if (item instanceof org.ether.society.model.Scenario s) {
            long year = s.getStartDateYear();
            if (year < 0) {
                return String.format("%,d av. J.-C.", Math.abs(year)).replace(',', ' ');
            } else if (year == 0) {
                return "An 0";
            } else {
                return String.valueOf(year);
            }
        }
        return null;
    }

    private ListCell<T> createPresetListCell() {
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
                } else {
                    String formattedName = formatPresetItem(item);
                    String dateText = getPresetDateText(item);
                    if (dateText != null && !dateText.isBlank()) {
                        nameLabel.setText(formattedName);
                        dateLabel.setText("📅 " + dateText);
                        setGraphic(container);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(formattedName);
                    }
                }
            }
        };
    }
}
