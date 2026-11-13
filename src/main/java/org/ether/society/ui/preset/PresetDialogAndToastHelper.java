/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.preset;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowHelper;

import java.io.File;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Modal dialog, JSON import/export file chooser, inline confirmation bar, and toast notification
 * helper for {@link org.ether.society.ui.PresetControlBar}.
 *
 * <p>Extracted in accordance with the Single Responsibility Principle (SRP) to decouple user
 * confirmation workflows and transient toast animations from the core preset toolbar control.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class PresetDialogAndToastHelper {

    /**
     * Private constructor preventing instantiation of this stateless UI helper class.
     */
    private PresetDialogAndToastHelper() {
    }

    /**
     * Creates a styled, initially hidden toast notification label for preset status messages.
     *
     * @return configured {@link Label} ready to be attached to the preset bar layout
     */
    public static Label createToastLabel() {
        Label toastLabel = new Label();
        toastLabel.setStyle("-fx-background-color: rgba(239,68,68,0.9); -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 6; -fx-font-size: 12px;");
        toastLabel.setVisible(false);
        toastLabel.setManaged(false);
        return toastLabel;
    }

    /**
     * Displays a transient toast notification inside the preset control bar with an automatic
     * 3-second hold followed by a 500-ms opacity fade-out transition.
     *
     * @param toastLabel    the target toast {@link Label}
     * @param message       localized notification text
     * @param styleOverride CSS background style prefix overriding the default alert background
     */
    public static void showToast(Label toastLabel, String message, String styleOverride) {
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
            fade.setOnFinished(f -> hideToast(toastLabel));
            fade.play();
        });
        pause.play();
    }

    /**
     * Immediately hides the toast notification label and resets its layout management and opacity.
     *
     * @param toastLabel the target toast {@link Label}
     */
    public static void hideToast(Label toastLabel) {
        toastLabel.setVisible(false);
        toastLabel.setManaged(false);
        toastLabel.setOpacity(1.0);
    }

    /**
     * Displays a modal confirmation dialog prompting the user to save, discard, or cancel
     * unsaved preset modifications before switching tabs or presets.
     *
     * @param <T>         preset item type
     * @param ownerWindow optional owner {@link Window} for modal anchoring
     * @param presetCombo the preset {@link ComboBox}
     * @param nameField   the inline preset name {@link TextField}
     * @param btnSave     the Save {@link ButtonType}
     * @param btnDiscard  the Discard {@link ButtonType}
     * @param btnCancel   the Cancel {@link ButtonType}
     * @return the user's selected {@link ButtonType}, if any
     */
    public static <T> Optional<ButtonType> showUnsavedChangesAlert(
            Window ownerWindow,
            ComboBox<T> presetCombo,
            TextField nameField,
            ButtonType btnSave,
            ButtonType btnDiscard,
            ButtonType btnCancel) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        WindowHelper.applyWindowIcon(alert);
        WindowHelper.styleDialogPane(alert.getDialogPane());
        if (ownerWindow != null) {
            alert.initOwner(ownerWindow);
        }

        alert.setTitle(I18n.getOrDefault("preset.dialog.unsaved_title", "Modifications non enregistrées"));
        String rawName = (presetCombo.getValue() != null)
                ? PresetItemFormatter.formatPresetItem(presetCombo.getValue())
                : (nameField.getText() != null ? nameField.getText() : "");
        String cleanName = rawName.replace("(Custom)", "").replace("(Personnalisé)", "").trim();
        if (cleanName.isBlank()) {
            cleanName = I18n.getOrDefault("preset.name.custom", "Personnalisé");
        }
        alert.setContentText(I18n.get("preset.dialog.unsaved_content", cleanName));
        alert.getButtonTypes().setAll(btnSave, btnDiscard, btnCancel);
        return alert.showAndWait();
    }

    /**
     * Prompts the user with a themed {@link TextInputDialog} to specify a clean preset name
     * when saving an unnamed or custom-suffixed configuration.
     *
     * @param rawCurrentName current text in the inline preset name field
     * @return optional trimmed preset name if confirmed and non-blank; empty otherwise
     */
    public static Optional<String> promptSavePresetName(String rawCurrentName) {
        String defaultName = rawCurrentName.isBlank()
                ? I18n.getOrDefault("preset.dialog.default_name", "My Preset")
                : rawCurrentName.replace("(Custom)", "").replace("(Personnalisé)", "").trim();
        TextInputDialog dialog = new TextInputDialog(defaultName);
        WindowHelper.applyWindowIcon(dialog);
        WindowHelper.styleDialogPane(dialog.getDialogPane());
        dialog.setTitle(I18n.getOrDefault("preset.dialog.save_title", "Save Preset"));
        dialog.setHeaderText(I18n.getOrDefault("preset.dialog.save_header", "Enter a name for this preset:"));
        dialog.setContentText(I18n.getOrDefault("preset.dialog.save_label", "Nom :"));
        Optional<String> entered = dialog.showAndWait();
        if (entered.isPresent() && !entered.get().isBlank()) {
            return Optional.of(entered.get().trim());
        }
        return Optional.empty();
    }

    /**
     * Displays a modal confirmation dialog asking whether to overwrite an existing preset
     * with the same name.
     *
     * @param name target preset name
     * @return {@code true} if the user confirmed overwriting; {@code false} otherwise
     */
    public static boolean confirmOverwriteExistingPreset(String name) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        WindowHelper.applyWindowIcon(confirm);
        WindowHelper.styleDialogPane(confirm.getDialogPane());
        confirm.setTitle(I18n.getOrDefault("preset.dialog.overwrite_title", "Preset Exists"));
        confirm.setHeaderText(I18n.getOrDefault("preset.dialog.overwrite_header", "Replace existing preset"));
        confirm.setContentText(I18n.getOrDefault("preset.dialog.overwrite_content",
                "Un préréglage nommé '" + name + "' existe déjà. Voulez-vous l'écraser ?"));
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Renders an inline confirmation row inside the preset bar to confirm or cancel deletion
     * of the currently selected preset.
     *
     * @param <T>             preset item type
     * @param hostContainer   parent {@link VBox} hosting the inline confirmation row
     * @param toastLabel      toast notification {@link Label}
     * @param selected        currently selected preset item (may be {@code null})
     * @param onConfirmDelete callback invoked if the user confirms deletion
     */
    public static <T> void promptInlineDelete(
            VBox hostContainer,
            Label toastLabel,
            T selected,
            Consumer<String> onConfirmDelete) {
        if (selected == null) {
            showToast(toastLabel, I18n.getOrDefault("preset.toast.no_selection", "âš ï¸ Select a preset to delete."),
                    "-fx-background-color: rgba(234,179,8,0.9);");
            return;
        }

        String name = PresetItemFormatter.formatPresetItem(selected);
        toastLabel.setText(I18n.getOrDefault("preset.toast.confirm_delete", "âš ï¸ Supprimer Â« ") + name + " Â» ?");
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
        hostContainer.getChildren().add(confirmRow);

        confirmBtn.setOnAction(ev -> {
            hostContainer.getChildren().remove(confirmRow);
            hideToast(toastLabel);
            onConfirmDelete.accept(name);
        });

        cancelBtn.setOnAction(ev -> {
            hostContainer.getChildren().remove(confirmRow);
            hideToast(toastLabel);
        });
    }

    /**
     * Opens a JSON save {@link FileChooser} to export the selected preset to disk.
     *
     * @param <T>            preset item type
     * @param ownerWindow    optional owner {@link Window}
     * @param exportCategory category slug used in suggested filename ({@code ether-<category>-<name>.json})
     * @param selected       currently selected preset item
     * @param nameField      inline preset name {@link TextField}
     * @param toastLabel     toast notification {@link Label}
     * @param onExport       callback receiving target {@link File} and selected preset
     */
    public static <T> void promptExportPresetFile(
            Window ownerWindow,
            String exportCategory,
            T selected,
            TextField nameField,
            Label toastLabel,
            BiConsumer<File, T> onExport) {
        if (selected == null) {
            showToast(toastLabel, I18n.getOrDefault("preset.toast.no_selection", "âš ï¸ Select a preset to export."),
                    "-fx-background-color: rgba(234,179,8,0.9);");
            return;
        }

        String presetName = nameField.getText().trim().isBlank()
                ? PresetItemFormatter.formatPresetItem(selected)
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

        File file = chooser.showSaveDialog(ownerWindow);
        if (file != null && onExport != null) {
            onExport.accept(file, selected);
            showToast(toastLabel, I18n.getOrDefault("preset.toast.exported", " Exported: ") + file.getName(),
                    "-fx-background-color: rgba(16,185,129,0.9);");
        }
    }

    /**
     * Opens a JSON open {@link FileChooser} to import a preset configuration file from disk.
     *
     * @param ownerWindow optional owner {@link Window}
     * @param toastLabel  toast notification {@link Label}
     * @param onImport    callback receiving selected source {@link File}
     */
    public static void promptImportPresetFile(Window ownerWindow, Label toastLabel, Consumer<File> onImport) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("preset.dialog.import_title", "Import Preset (JSON)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers JSON", "*.json"));
        File file = chooser.showOpenDialog(ownerWindow);
        if (file != null && onImport != null) {
            onImport.accept(file);
            showToast(toastLabel, I18n.getOrDefault("preset.toast.imported", " Imported: ") + file.getName(),
                    "-fx-background-color: rgba(16,185,129,0.9);");
        }
    }
}

