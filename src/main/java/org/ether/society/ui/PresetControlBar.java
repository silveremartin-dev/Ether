/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.preset.PresetDialogAndToastHelper;
import org.ether.society.ui.preset.PresetItemFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Standardized Preset Toolbar with inline editable name field.
 *
 * @param <T> Preset type
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PresetControlBar<T> extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(PresetControlBar.class);

    private String exportCategory = "preset";
    private String labelKey;
    private String defaultLabelText;

    private final Label presetLabel;
    private final ComboBox<T> presetCombo;
    private final TextField nameField;

    private final Button saveBtn;
    private final Button deleteBtn;
    private final Button exportBtn;
    private final Button importBtn;

    private boolean trackingChanges = false;
    private boolean dirty = false;
    private Label toastLabel;
    private PresetActionsListener<T> listener;

    public interface PresetActionsListener<T> {
        void onPresetSelected(T preset);
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

        presetCombo = new ComboBox<>();
        presetCombo.setMaxWidth(Double.MAX_VALUE);
        presetCombo.setPrefWidth(220);
        presetCombo.setPromptText(I18n.getOrDefault("preset.combo.placeholder", "— Select a preset —"));
        HBox.setHgrow(presetCombo, Priority.ALWAYS);

        presetCombo.setConverter(new StringConverter<T>() {
            @Override public String toString(T object) { return PresetItemFormatter.formatPresetItem(object); }
            @Override public T fromString(String string) { return null; }
        });

        presetCombo.setCellFactory(p -> PresetItemFormatter.createPresetListCell());
        presetCombo.setButtonCell(PresetItemFormatter.createPresetListCell());

        presetCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String desc = PresetItemFormatter.getPresetDetailedDescription(newV);
                if (desc != null && !desc.isBlank()) {
                    Tooltip tip = new Tooltip(desc);
                    tip.setWrapText(true);
                    tip.setMaxWidth(480);
                    presetCombo.setTooltip(tip);
                } else {
                    presetCombo.setTooltip(null);
                }
            } else {
                presetCombo.setTooltip(null);
            }
        });

        nameField = new TextField();
        nameField.setPromptText(I18n.getOrDefault("preset.name.placeholder", "Preset name…"));
        nameField.setMaxWidth(Double.MAX_VALUE);
        nameField.setTooltip(new Tooltip(I18n.getOrDefault("preset.name.tooltip",
                "Nom du préréglage. Modifiez-le librement puis cliquez sur Enregistrer.")));
        HBox.setHgrow(nameField, Priority.ALWAYS);

        nameField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isBlank() && !trackingChanges && presetCombo.getValue() != null) {
                String selectedName = PresetItemFormatter.formatPresetItem(presetCombo.getValue());
                String cleanVal = PresetItemFormatter.cleanScenarioName(val);
                String cleanSel = PresetItemFormatter.cleanScenarioName(selectedName);
                if (!val.trim().equalsIgnoreCase(selectedName) && !cleanVal.equalsIgnoreCase(cleanSel)) {
                    dirty = true;
                }
            }
        });

        bindComboSelectionHandler();

        HBox topRow = new HBox(8, presetCombo); topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(presetCombo, Priority.ALWAYS);
        HBox nameRow = new HBox(8, presetLabel, nameField); nameRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameField, Priority.ALWAYS);

        saveBtn = createActionButton(this::doSave);
        deleteBtn = createActionButton(this::promptDelete);
        exportBtn = createActionButton(this::promptExport);
        importBtn = createActionButton(this::promptImport);

        HBox row1 = new HBox(6, saveBtn, deleteBtn); row1.setAlignment(Pos.CENTER);
        HBox row2 = new HBox(6, exportBtn, importBtn); row2.setAlignment(Pos.CENTER);
        VBox btnBox = new VBox(6, row1, row2);

        toastLabel = PresetDialogAndToastHelper.createToastLabel();
        getChildren().addAll(topRow, nameRow, btnBox, toastLabel);

        updateTexts();
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    private Button createActionButton(Runnable action) {
        Button btn = new Button();
        btn.getStyleClass().add("button-secondary");
        btn.setOnAction(e -> action.run());
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private void bindComboSelectionHandler() {
        final boolean[] reverting = {false};
        presetCombo.setOnAction(e -> {
            if (reverting[0]) return;
            T selected = presetCombo.getValue();
            if (selected != null) {
                if (dirty) {
                    Window owner = getScene() != null ? getScene().getWindow() : null;
                    if (!promptSavePresetIfDirty(owner)) {
                        String baseName = nameField.getText() != null
                                ? nameField.getText().trim().replace(I18n.getOrDefault("preset.name.custom_suffix", " (Custom)"), "").trim()
                                : "";
                        T previous = presetCombo.getItems().stream()
                                .filter(it -> it != null && PresetItemFormatter.formatPresetItem(it).equalsIgnoreCase(baseName))
                                .findFirst().orElse(null);
                        reverting[0] = true;
                        try {
                            presetCombo.setValue(previous);
                        } finally {
                            reverting[0] = false;
                        }
                        return;
                    }
                }
                nameField.setText(PresetItemFormatter.formatPresetItem(selected));
                nameField.setStyle("");
                trackingChanges = false;
                dirty = false;
                if (listener != null) listener.onPresetSelected(selected);
            }
        });
    }

    public void setExportCategory(String category) { this.exportCategory = category; }
    public String getExportCategory() { return exportCategory; }

    public void setPresets(List<T> items, T defaultItem) {
        presetCombo.getItems().setAll(items);
        if (defaultItem != null) {
            String target = PresetItemFormatter.formatPresetItem(defaultItem);
            for (T it : items) {
                if (it.equals(defaultItem) || PresetItemFormatter.formatPresetItem(it).equalsIgnoreCase(target)) {
                    presetCombo.setValue(it);
                    nameField.setText(PresetItemFormatter.formatPresetItem(it));
                    nameField.setStyle("");
                    trackingChanges = false;
                    dirty = false;
                    return;
                }
            }
        }
        presetCombo.setValue(null);
        nameField.setText("");
        trackingChanges = false;
        dirty = false;
    }

    public boolean isDirty() { return dirty; }

    public void markDirty() {
        this.dirty = true;
        this.trackingChanges = true;
        if (nameField != null) nameField.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
    }

    public void markClean() {
        this.dirty = false;
        this.trackingChanges = false;
        if (nameField != null) nameField.setStyle("");
    }

    public void markClean(T item) {
        if (item != null) {
            String target = PresetItemFormatter.formatPresetItem(item);
            for (T it : presetCombo.getItems()) {
                if (it.equals(item) || PresetItemFormatter.formatPresetItem(it).equalsIgnoreCase(target)) {
                    presetCombo.setValue(it);
                    nameField.setText(PresetItemFormatter.formatPresetItem(it));
                    break;
                }
            }
            if (nameField.getText().isBlank()) nameField.setText(target);
        }
        nameField.setStyle("");
        trackingChanges = false;
        dirty = false;
    }

    public void notifyParametersChanged() {
        dirty = true;
        if (trackingChanges || presetCombo.getValue() != null) {
            String current = nameField.getText() != null ? nameField.getText().trim() : "";
            if (presetCombo.getValue() != null) {
                String presetName = PresetItemFormatter.formatPresetItem(presetCombo.getValue());
                if (!presetName.contains("Personnalisé") && !presetName.contains("Custom")) {
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

    public boolean promptSavePresetIfDirty(Window ownerWindow) {
        if (!dirty) return true;
        ButtonType btnSave = new ButtonType("💾 " + I18n.getOrDefault("preset.save", "Enregistrer"), ButtonBar.ButtonData.YES);
        ButtonType btnDiscard = new ButtonType(I18n.getOrDefault("preset.dialog.discard", "Ignorer"), ButtonBar.ButtonData.NO);
        ButtonType btnCancel = new ButtonType(I18n.getOrDefault("preset.btn.cancel", "Annuler"), ButtonBar.ButtonData.CANCEL_CLOSE);

        Optional<ButtonType> res = PresetDialogAndToastHelper.showUnsavedChangesAlert(ownerWindow, presetCombo, nameField, btnSave, btnDiscard, btnCancel);
        if (res.isPresent() && res.get() == btnSave) {
            if (!doSave()) return false;
            markClean();
            return true;
        } else if (res.isPresent() && res.get() == btnDiscard) {
            dirty = false;
            trackingChanges = false;
            if (nameField != null) nameField.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
            return true;
        }
        return false;
    }

    public void setNameText(String name) {
        if (nameField != null) {
            nameField.setText(name != null ? name : "");
            nameField.setStyle("");
            dirty = false;
        }
    }

    public ComboBox<T> getPresetCombo() { return presetCombo; }
    public String getCurrentName() { return nameField.getText().trim(); }
    public void setListener(PresetActionsListener<T> listener) { this.listener = listener; }

    public boolean doSave() {
        String name = nameField.getText().trim();
        if (name.isBlank() || name.contains("(Custom)") || name.contains("(Personnalisé)")) {
            Optional<String> entered = PresetDialogAndToastHelper.promptSavePresetName(name);
            if (entered.isPresent() && !entered.get().isBlank()) {
                String finalName = entered.get();
                nameField.setText(finalName);
                nameField.setStyle("");
                return performSave(finalName);
            }
            return false;
        }
        return performSave(name);
    }

    private boolean performSave(String name) {
        boolean exists = presetCombo.getItems().stream().anyMatch(item -> item != null && PresetItemFormatter.formatPresetItem(item).equalsIgnoreCase(name));
        if (exists && !PresetDialogAndToastHelper.confirmOverwriteExistingPreset(name)) return false;
        dirty = false; trackingChanges = false;
        if (listener != null) listener.onSavePreset(name);
        PresetDialogAndToastHelper.showToast(toastLabel, I18n.getOrDefault("preset.toast.saved", "✅ Preset saved: ") + name, "-fx-background-color: rgba(16,185,129,0.9);");
        return true;
    }

    private void promptDelete() {
        T selected = presetCombo.getValue();
        PresetDialogAndToastHelper.promptInlineDelete(this, toastLabel, selected, name -> {
            if (listener != null) {
                listener.onDeletePreset(selected);
                presetCombo.getItems().remove(selected);
                nameField.clear();
                trackingChanges = false;
                PresetDialogAndToastHelper.showToast(toastLabel, I18n.getOrDefault("preset.toast.deleted", "🗑️ Preset deleted: ") + name, "-fx-background-color: rgba(239,68,68,0.85);");
            }
        });
    }

    private void promptExport() {
        PresetDialogAndToastHelper.promptExportPresetFile(getScene() != null ? getScene().getWindow() : null, exportCategory, presetCombo.getValue(), nameField, toastLabel, (file, sel) -> {
            if (listener != null) listener.onExportPreset(file, sel);
        });
    }

    private void promptImport() {
        PresetDialogAndToastHelper.promptImportPresetFile(getScene() != null ? getScene().getWindow() : null, toastLabel, file -> {
            if (listener != null) listener.onImportPreset(file);
        });
    }

    public void updateTexts() {
        if (presetLabel != null && labelKey != null) presetLabel.setText(I18n.getOrDefault(labelKey, defaultLabelText) + ":");
        if (saveBtn != null) saveBtn.setText("💾 " + I18n.getOrDefault("preset.save", "Enregistrer"));
        if (deleteBtn != null) deleteBtn.setText("🗑️ " + I18n.getOrDefault("preset.delete", "Supprimer"));
        if (exportBtn != null) exportBtn.setText("📤 " + I18n.getOrDefault("preset.export", "Export"));
        if (importBtn != null) importBtn.setText("📥 " + I18n.getOrDefault("preset.import", "Import"));
        if (presetCombo != null) {
            presetCombo.setPromptText(I18n.getOrDefault("preset.combo.placeholder", "— Select a preset —"));
            presetCombo.setButtonCell(PresetItemFormatter.createPresetListCell());
            T cur = presetCombo.getValue();
            if (cur != null && nameField != null && !dirty) nameField.setText(PresetItemFormatter.formatPresetItem(cur));
        }
        if (nameField != null) nameField.setPromptText(I18n.getOrDefault("preset.name.placeholder", "Preset name…"));
        if (presetCombo != null) presetCombo.setTooltip(new Tooltip(I18n.getOrDefault("preset.tooltip.combo", "Select an existing preset")));
        if (nameField != null) nameField.setTooltip(new Tooltip(I18n.getOrDefault("preset.name.tooltip", "Preset name — directly editable to name or rename")));
        if (saveBtn != null) saveBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset_save", "Enregistrer la configuration actuelle sous ce nom")));
        if (deleteBtn != null) deleteBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset_delete", "Delete selected preset")));
        if (exportBtn != null) exportBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset_export", "Export preset as JSON (ether-category-name.json)")));
        if (importBtn != null) importBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.preset_import", "Import JSON preset file")));
    }
}
