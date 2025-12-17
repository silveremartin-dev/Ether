/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;

import org.ether.society.procedural.PlanetPreset;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Editor for Simulation Scenarios.
 */
public class ScenarioEditorDialog extends Stage {

    private TextField nameField;

    // Planet Physics
    private ComboBox<PlanetPreset> planetPresetCombo;
    private TextField radiusField;
    private Spinner<Double> rotationSpinner;
    private Spinner<Double> revolutionSpinner;
    private Slider tiltSlider;

    // Human Start
    private Spinner<Integer> humanCountSpinner;
    private Slider techLevelSlider;
    private ComboBox<String> densityTypeCombo;

    // Climate & Time
    private Slider climateHarshnessSlider;
    private TextField startDateField;

    // Result
    private Scenario result = null;

    public ScenarioEditorDialog() {
        setTitle("Scenario Editor");
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Content
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().add(createGeneralsTab());
        tabs.getTabs().add(createPlanetPhysicsTab());
        tabs.getTabs().add(createCivilizationTab());

        root.setCenter(tabs);

        // Buttons
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        Button saveBtn = new Button("Save & Use");
        saveBtn.setOnAction(e -> {
            buildScenario();
            close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> {
            result = null;
            close();
        });

        buttons.getChildren().addAll(saveBtn, cancelBtn);
        root.setBottom(buttons);

        Scene scene = new Scene(root, 600, 500);
        setScene(scene);
    }

    private Tab createGeneralsTab() {
        Tab tab = new Tab("General");
        GridPane grid = createGrid();

        nameField = new TextField("New Scenario");

        startDateField = new TextField("-100000");

        climateHarshnessSlider = new Slider(0.0, 1.0, 0.5);
        climateHarshnessSlider.setShowTickLabels(true);
        climateHarshnessSlider.setShowTickMarks(true);

        grid.addRow(0, new Label("Scenario Name:"), nameField);
        grid.addRow(1, new Label("Start Year:"), startDateField);
        grid.addRow(2, new Label("Climate Harshness:"), climateHarshnessSlider);

        tab.setContent(grid);
        return tab;
    }

    private Tab createPlanetPhysicsTab() {
        Tab tab = new Tab("Planet Physics");
        GridPane grid = createGrid();

        planetPresetCombo = new ComboBox<>();
        planetPresetCombo.getItems().addAll(PlanetPreset.getPresets());
        planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);

        radiusField = new TextField("6371"); // Earth km

        rotationSpinner = new Spinner<>(0.0, 1000.0, 24.0, 0.5);
        rotationSpinner.setEditable(true);

        revolutionSpinner = new Spinner<>(0.0, 10000.0, 365.25, 1.0);
        revolutionSpinner.setEditable(true);

        tiltSlider = new Slider(0, 90, 23.5);
        tiltSlider.setShowTickLabels(true);
        tiltSlider.setShowTickMarks(true);

        grid.addRow(0, new Label("Planet Preset:"), planetPresetCombo);
        grid.addRow(1, new Label("Radius (km):"), radiusField);
        grid.addRow(2, new Label("Rotation Period (h):"), rotationSpinner);
        grid.addRow(3, new Label("Revolution (days):"), revolutionSpinner);
        grid.addRow(4, new Label("Axial Tilt (deg):"), tiltSlider);

        tab.setContent(grid);
        return tab;
    }

    private Tab createCivilizationTab() {
        Tab tab = new Tab("Civilization");
        GridPane grid = createGrid();

        humanCountSpinner = new Spinner<>(0, 10000000, 1000, 100);
        humanCountSpinner.setEditable(true);

        techLevelSlider = new Slider(0.0, 10.0, 0.0);
        techLevelSlider.setShowTickLabels(true);
        techLevelSlider.setShowTickMarks(true);

        densityTypeCombo = new ComboBox<>();
        densityTypeCombo.getItems().addAll("SPARSE", "DENSE", "RIVER_VALLEYS", "ONE_CONTINENT");
        densityTypeCombo.setValue("SPARSE");

        grid.addRow(0, new Label("Initial Population:"), humanCountSpinner);
        grid.addRow(1, new Label("Tech Level:"), techLevelSlider);
        grid.addRow(2, new Label("Density Pattern:"), densityTypeCombo);

        tab.setContent(grid);
        return tab;
    }

    private GridPane createGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        return grid;
    }

    private void buildScenario() {
        result = new Scenario();
        result.setName(nameField.getText());
        try {
            result.setStartDateYear(Long.parseLong(startDateField.getText()));
        } catch (NumberFormatException e) {
            result.setStartDateYear(-100000);
        }
        result.setClimateHarshness(climateHarshnessSlider.getValue());

        try {
            result.setPlanetRadiusKm(Double.parseDouble(radiusField.getText()));
        } catch (NumberFormatException e) {
            result.setPlanetRadiusKm(6371);
        }

        result.setRotationPeriodHours(rotationSpinner.getValue());
        result.setRevolutionPeriodDays(revolutionSpinner.getValue());
        result.setAxialTiltDegrees(tiltSlider.getValue());

        // For now, create a default planet config based on preset, or use what was
        // generated
        // In a real app, this would link to the selected PlanetConfig
        PlanetPreset preset = planetPresetCombo.getValue();
        if (preset != null) {
            result.setPlanetPreset(preset);
        }

        result.setInitialHumanCount(humanCountSpinner.getValue());
        result.setInitialTechLevel(techLevelSlider.getValue());
        result.setPopulationDensityType(densityTypeCombo.getValue());
    }

    public Optional<Scenario> showAndWaitForResult() {
        showAndWait();
        return Optional.ofNullable(result);
    }
}
