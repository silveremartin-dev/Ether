/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.PresetControlBar;

/**
 * Controller assembling the left scrollable configuration pane containing all 12
 * scenario parameter sections, header, validation banner, and pinned action bar.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ScenarioConfigPaneBuilder {

    private ScenarioConfigPaneBuilder() {
        // Utility class
    }

    /**
     * Builds a standardized card section with an optional reset button.
     *
     * @param header section header label
     * @param content section body content node
     * @param sectionResetAction callback to reset section parameters
     * @param onParamChange callback invoked after parameter reset
     * @return styled JavaFX VBox card container
     */
    public static VBox createSection(Label header, Node content, Runnable sectionResetAction, Runnable onParamChange) {
        if (header != null) {
            header.getStyleClass().add("label-section-header");
        }
        HBox headerRow = new HBox(8, header);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        if (sectionResetAction != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button resetSecBtn = new Button("🔄 " + I18n.getOrDefault("scenario.btn.reset_section", "Default"));
            resetSecBtn.getStyleClass().add("button-secondary");
            resetSecBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            resetSecBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.reset_section", "Reset section parameters to canonical default values.")));
            resetSecBtn.setOnAction(e -> {
                sectionResetAction.run();
                if (onParamChange != null) {
                    onParamChange.run();
                }
            });
            headerRow.getChildren().addAll(spacer, resetSecBtn);
        }
        VBox box = new VBox(8, headerRow, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    /**
     * Builds and returns the validation error banner VBox.
     *
     * @param headerLabel banner title label
     * @param errorLabel dynamic error text label
     * @return container VBox
     */
    public static VBox createValidationErrorBanner(Label headerLabel, Label errorLabel) {
        VBox banner = new VBox(6, headerLabel, errorLabel);
        banner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");
        banner.setVisible(false);
        banner.setManaged(false);
        return banner;
    }

    /**
     * Builds the preset card section.
     *
     * @param header section header
     * @param presetBar preset control bar
     * @param autoWizardBtn auto epoch wizard button
     * @param descLabel description title
     * @param descArea description text area
     * @return styled VBox card
     */
    public static VBox buildScenarioPresetSection(Label header, PresetControlBar<Scenario> presetBar, Button autoWizardBtn, Label descLabel, TextArea descArea) {
        return createSection(header, new VBox(8, presetBar, autoWizardBtn, descLabel, descArea), null, null);
    }

    /**
     * Builds the pinned bottom action bar containing progress and start button.
     *
     * @param progressBar progress bar
     * @param statusLabel status label
     * @param startBtn start button
     * @return styled VBox container
     */
    public static VBox buildBottomActionBox(ProgressBar progressBar, Label statusLabel, Button startBtn) {
        VBox box = new VBox(8, progressBar, statusLabel, startBtn);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /**
     * Assembles all configuration sections into the master scrollable VBox root.
     *
     * @param headerLabel panel title header
     * @param validationBanner validation error banner
     * @param presetSection global presets section
     * @param inheritedSection inherited planet & ecology section
     * @param spatiotemporalSection timeline & resolution section
     * @param demographicSection population identity card section
     * @param culturalSection cultural vector & layers section
     * @param clippingSection spatial clipping section
     * @param oceanOptSection engine architecture & optimization section
     * @param eventsSection scheduled events section
     * @param snapshotSection snapshot resumption section
     * @param bundleSection unified scenario bundle section
     * @param liveDiagnosticCard live viability diagnostic card
     * @return master configuration VBox
     */
    public static VBox assembleConfigPane(
            Label headerLabel,
            VBox validationBanner,
            VBox presetSection,
            VBox inheritedSection,
            VBox spatiotemporalSection,
            VBox demographicSection,
            VBox culturalSection,
            VBox clippingSection,
            VBox oceanOptSection,
            VBox eventsSection,
            VBox snapshotSection,
            VBox bundleSection,
            VBox liveDiagnosticCard
    ) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(0, 20, 0, 0));
        root.getChildren().addAll(
                headerLabel,
                validationBanner,
                presetSection,
                inheritedSection,
                spatiotemporalSection,
                demographicSection,
                culturalSection,
                clippingSection,
                oceanOptSection,
                eventsSection,
                snapshotSection,
                bundleSection,
                liveDiagnosticCard
        );
        return root;
    }
}
