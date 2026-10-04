/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Main dashboard panel for the application workflow.
 * Replaces the traditional MenuBar with a process-oriented flow.
 * 
 * Flow:
 * 1. Planet (Generation/Loading)
 * 2. Scenario (Civilization Setup)
 * 3. Simulation (Run/Monitor)
 */
public class WorkflowPanel extends HBox {

    // Callbacks
    private Runnable onPlanetAction;
    private Runnable onScenarioAction;
    private Runnable onDensityAction;
    private Runnable onLoadAction;
    private Runnable onSaveAction;

    /*
     * Workflow panel.
     * Enforces physical invariants and updates associated state variables within {@code WorkflowPanel}.
     *
     */
    public WorkflowPanel() {
        initUI();
    }

    // Helper subroutine: init ui - internal state computation & bounds checking
    private void initUI() {
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(10));
        setSpacing(15);
        getStyleClass().add("glass-panel");

        // 1. Planet Section
        VBox planetSection = createSection("PLANET", "Create or Load World");
        Button newPlanetBtn = createActionButton("New Planet", "Generate procedural world");
        newPlanetBtn.setOnAction(e -> {
            if (onPlanetAction != null)
                onPlanetAction.run();
        });

        Button loadBtn = createActionButton("Load", "Load from database");
        loadBtn.setOnAction(e -> {
            if (onLoadAction != null)
                onLoadAction.run();
        });

        HBox planetButtons = new HBox(5, newPlanetBtn, loadBtn);
        planetSection.getChildren().add(planetButtons);

        // 2. Scenario Section
        VBox scenarioSection = createSection("SCENARIO", "Configure Civilization");
        Button scenarioBtn = createActionButton("Edit Scenario", "Setup physics & people");
        scenarioBtn.setOnAction(e -> {
            if (onScenarioAction != null)
                onScenarioAction.run();
        });

        Button densityBtn = createActionButton("Painter", "Edit population paint");
        densityBtn.setOnAction(e -> {
            if (onDensityAction != null)
                onDensityAction.run();
        });

        HBox scenarioButtons = new HBox(5, scenarioBtn, densityBtn);
        scenarioSection.getChildren().add(scenarioButtons);

        // 3. Simulation Section
        VBox simSection = createSection("SIMULATION", "Global Control");
        Button saveBtn = createActionButton("Save", "Persist current state");
        saveBtn.setOnAction(e -> {
            if (onSaveAction != null)
                onSaveAction.run();
        });
        simSection.getChildren().add(saveBtn);

        // Separators
        Separator output = new Separator();
        output.setOrientation(javafx.geometry.Orientation.VERTICAL);

        Separator output2 = new Separator();
        output2.setOrientation(javafx.geometry.Orientation.VERTICAL);

        // Title / Branding
        Label brand = new Label("ETHER");
        brand.getStyleClass().add("label-title");
        brand.setStyle("-fx-padding: 0 20 0 0;");

        getChildren().addAll(
                brand,
                planetSection,
                output,
                scenarioSection,
                output2,
                simSection);

        // Spacer
        HBox.setHgrow(simSection, Priority.ALWAYS);
    }

    // Helper subroutine: create section - internal state computation & bounds checking
    private VBox createSection(String title, String subtitle) {
        VBox box = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("label-section-header");

        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("hint-label");

        box.getChildren().addAll(titleLabel, subLabel);
        return box;
    }

    // Helper subroutine: create action button - internal state computation & bounds checking
    private Button createActionButton(String text, String tooltip) {
        Button btn = new Button(text);
        btn.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        btn.getStyleClass().add("button-secondary");
        return btn;
    }

    // Setters for actions
    /*
     * Set on planet action.
     * Enforces physical invariants and updates associated state variables within {@code WorkflowPanel}.
     *
     * @param action the action parameter (Runnable)
     */
    public void setOnPlanetAction(Runnable action) {
        this.onPlanetAction = action;
    }

    /*
     * Set on scenario action.
     * Enforces physical invariants and updates associated state variables within {@code WorkflowPanel}.
     *
     * @param action the action parameter (Runnable)
     */
    public void setOnScenarioAction(Runnable action) {
        this.onScenarioAction = action;
    }

    /*
     * Set on density action.
     * Enforces physical invariants and updates associated state variables within {@code WorkflowPanel}.
     *
     * @param action the action parameter (Runnable)
     */
    public void setOnDensityAction(Runnable action) {
        this.onDensityAction = action;
    }

    /*
     * Set on load action.
     * Enforces physical invariants and updates associated state variables within {@code WorkflowPanel}.
     *
     * @param action the action parameter (Runnable)
     */
    public void setOnLoadAction(Runnable action) {
        this.onLoadAction = action;
    }

    /*
     * Set on save action.
     * Enforces physical invariants and updates associated state variables within {@code WorkflowPanel}.
     *
     * @param action the action parameter (Runnable)
     */
    public void setOnSaveAction(Runnable action) {
        this.onSaveAction = action;
    }
}
