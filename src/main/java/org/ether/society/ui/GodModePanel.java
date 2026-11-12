/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.NuclearWarfareClimateEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.ui.godmode.GodModeDisasterExecutor;
import org.ether.society.ui.godmode.GodModeEventRegistry;
import org.ether.society.ui.godmode.GodModeEventRegistry.EventTypeItem;
import org.ether.society.ui.godmode.GodModeInterventionCoordinator;
import org.ether.society.ui.godmode.GodModeUiConfigurator;

import java.util.List;
import java.util.Locale;

/**
 * God Mode &amp; Interactive Timeline Controller.
 *
 * <p>Allows pausing live simulation, injecting real physical events (volcanoes, EMP, heatwaves),
 * selecting active duration, applying spatial brushes, and viewing/navigating the chronological
 * scenario timeline audit trail. Delegates UI form construction to {@link GodModeUiConfigurator}
 * and forcing execution / timeline audit coordination to {@link GodModeInterventionCoordinator}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class GodModePanel extends VBox {

    /** Canonical catalog of available God Mode physical and climatic event types. */
    public static final List<EventTypeItem> EVENT_TYPES = GodModeEventRegistry.EVENT_TYPES;

    private final H3SimulationEngine engine;
    private final ScenarioTimeline timeline;
    private H3MapCanvas mapCanvas;
    @SuppressWarnings("unused")
    private NotificationOverlay notificationOverlay;

    private final Label headerLabel;
    private final Label pauseNoticeLabel;
    private final ListView<String> timelineListView;
    private final Label timelineHeaderLabel;
    private final Label timelineHintLabel;

    // Configurable Form Controls
    private final ComboBox<EventTypeItem> eventTypeCombo;
    private final Label eventDescriptionLabel;
    private final TextField eventNameField;
    private final Label dateRangeLabel;
    private final Spinner<Integer> targetYearSpinner;
    private final Spinner<Integer> targetMonthSpinner;
    private final Spinner<Integer> targetDaySpinner;
    private final Slider targetYearSlider;
    private final Spinner<Double> latSpinner;
    private final Spinner<Double> lngSpinner;
    private final Spinner<Double> magnitudeSpinner;
    private final Spinner<Integer> durationDaysSpinner;
    private final Spinner<Double> radiusKmSpinner;

    // Injector section labels & buttons
    private final Label injectorTitleLabel;
    private final Label lblEventType;
    private final Label lblEventTitle;
    private final Label lblTargetYear;
    private final Label lblLat;
    private final Label lblLng;
    private final Label lblMag;
    private final Label lblDurationDays;
    private final Label lblDurationUnit = new Label();
    private final Label lblRadius;
    private final Button scheduleBtn;
    private final Button triggerNowBtn;

    // Spawner section
    private final Label spawnerTitleLabel;
    private final Button injectPopBtn;
    private final Button injectFoodBtn;
    private final Button massExtinctionBtn;

    // Terraform section
    private final Label terraformTitleLabel;
    private final ComboBox<String> brushModeCombo;
    private final Button applyBrushBtn;

    // Reset section
    private final Label resetTitleLabel;
    private final Button resetDisastersBtn;

    /**
     * Constructs the God Mode &amp; Chronological Audit Panel.
     *
     * @param engine active H3 simulation engine
     * @param timeline scenario chronological timeline
     */
    public GodModePanel(H3SimulationEngine engine, ScenarioTimeline timeline) {
        this.engine = engine;
        this.timeline = timeline != null ? timeline : new ScenarioTimeline();

        setPadding(new Insets(12, 12, 35, 12));
        setSpacing(10);
        getStyleClass().add("glass-panel");

        headerLabel = new Label();
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #38bdf8;");

        pauseNoticeLabel = new Label();
        pauseNoticeLabel.setWrapText(true);
        pauseNoticeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #fcd34d; -fx-padding: 8 10; -fx-background-color: rgba(245, 158, 11, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(245, 158, 11, 0.4); -fx-border-radius: 6; -fx-line-spacing: 2px;");

        eventTypeCombo = new ComboBox<>();
        GodModeUiConfigurator.configureEventTypeCombo(eventTypeCombo, EVENT_TYPES);

        eventDescriptionLabel = new Label();
        eventDescriptionLabel.setWrapText(true);
        eventDescriptionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1; -fx-padding: 6 8; -fx-background-color: rgba(15, 23, 42, 0.7); -fx-background-radius: 4; -fx-border-color: rgba(56, 189, 248, 0.35); -fx-border-radius: 4;");

        eventNameField = new TextField();
        eventNameField.setPrefWidth(220);
        eventNameField.setMaxWidth(280);

        int currentYr = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        int minYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getStartDateYear() : -100000;
        int maxYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getEndDateYear() : 2100;
        if (minYr >= maxYr) { minYr = -100000; maxYr = 2100; }

        dateRangeLabel = new Label(String.format(I18n.getOrDefault("godmode.label.date_range", "📅 Allowed Range: [Year %,d ➔ Year %,d]"), minYr, maxYr));
        dateRangeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        targetYearSpinner = new Spinner<>(minYr, maxYr, Math.max(minYr, Math.min(maxYr, currentYr)), 1);
        targetYearSpinner.setEditable(true);
        targetYearSpinner.setPrefWidth(95);
        targetYearSpinner.setMaxWidth(110);

        int currentMo = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentMonth() + 1 : 1;
        int currentD = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentDay() : 1;
        targetMonthSpinner = new Spinner<>(1, 12, Math.max(1, Math.min(12, currentMo)), 1);
        targetMonthSpinner.setEditable(true);
        targetMonthSpinner.setPrefWidth(60);
        targetMonthSpinner.setMaxWidth(70);

        targetDaySpinner = new Spinner<>(1, 31, Math.max(1, Math.min(31, currentD)), 1);
        targetDaySpinner.setEditable(true);
        targetDaySpinner.setPrefWidth(60);
        targetDaySpinner.setMaxWidth(70);

        targetYearSlider = new Slider(minYr, maxYr, Math.max(minYr, Math.min(maxYr, currentYr)));
        targetYearSlider.setBlockIncrement(1);
        targetYearSlider.setMajorTickUnit(Math.max(1, (maxYr - minYr) / 5.0));
        targetYearSlider.setMinorTickCount(4);
        targetYearSlider.setShowTickMarks(false);
        targetYearSlider.setPrefWidth(120);
        targetYearSlider.setMaxWidth(150);

        targetYearSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && Math.abs(targetYearSpinner.getValue() - newV.intValue()) > 0) {
                targetYearSpinner.getValueFactory().setValue(newV.intValue());
            }
        });
        targetYearSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && Math.abs(targetYearSlider.getValue() - newV) > 0) {
                targetYearSlider.setValue(newV);
            }
        });

        latSpinner = GodModeUiConfigurator.createDoubleSpinner(-90.0, 90.0, 0.0, 1.0);
        lngSpinner = GodModeUiConfigurator.createDoubleSpinner(-180.0, 180.0, 0.0, 1.0);
        magnitudeSpinner = GodModeUiConfigurator.createDoubleSpinner(0.1, 10.0, 7.0, 0.5);
        radiusKmSpinner = GodModeUiConfigurator.createDoubleSpinner(50.0, 5000.0, 1000.0, 50.0);

        durationDaysSpinner = new Spinner<>(0, 36500, 365, 1);
        durationDaysSpinner.setEditable(true);
        durationDaysSpinner.setPrefWidth(95);
        durationDaysSpinner.setMaxWidth(110);

        eventTypeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            eventNameField.setText(newV.getDefaultTitle());
            eventDescriptionLabel.setText(newV.getDescription());
            if (magnitudeSpinner.getValueFactory() != null) {
                magnitudeSpinner.getValueFactory().setValue(newV.defaultMagnitude());
            }
            if (durationDaysSpinner.getValueFactory() != null) {
                durationDaysSpinner.getValueFactory().setValue(newV.defaultDurationDays());
            }
            durationDaysSpinner.setDisable(newV.defaultDurationDays() == 0);
        });
        eventTypeCombo.getSelectionModel().select(0);

        injectorTitleLabel = new Label();
        injectorTitleLabel.getStyleClass().add("label-section-header");
        lblEventType = GodModeUiConfigurator.createFormLabel(140);
        lblEventTitle = GodModeUiConfigurator.createFormLabel(140);
        lblTargetYear = GodModeUiConfigurator.createFormLabel(140);
        lblLat = GodModeUiConfigurator.createFormLabel(140);
        lblLng = GodModeUiConfigurator.createFormLabel(140);
        lblMag = GodModeUiConfigurator.createFormLabel(140);
        lblDurationDays = GodModeUiConfigurator.createFormLabel(140);
        lblDurationUnit.setText(I18n.getOrDefault("godmode.unit.days", "jours"));
        lblDurationUnit.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        lblRadius = GodModeUiConfigurator.createFormLabel(140);

        scheduleBtn = new Button();
        scheduleBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 12; -fx-background-radius: 6;");
        scheduleBtn.setMaxWidth(Double.MAX_VALUE);
        scheduleBtn.setOnAction(e -> scheduleEvent(false));

        triggerNowBtn = new Button();
        triggerNowBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 12; -fx-background-radius: 6;");
        triggerNowBtn.setMaxWidth(Double.MAX_VALUE);
        triggerNowBtn.setOnAction(e -> scheduleEvent(true));

        VBox injectorBox = GodModeUiConfigurator.buildInjectorSection(
                injectorTitleLabel, lblEventType, eventTypeCombo, eventDescriptionLabel,
                lblEventTitle, eventNameField, lblTargetYear, targetYearSpinner, targetMonthSpinner, targetDaySpinner, targetYearSlider, dateRangeLabel,
                lblLat, latSpinner, lblLng, lngSpinner, lblMag, magnitudeSpinner,
                lblRadius, radiusKmSpinner, lblDurationDays, durationDaysSpinner, lblDurationUnit,
                scheduleBtn, triggerNowBtn
        );

        // Spawner components
        spawnerTitleLabel = new Label();
        spawnerTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981;");

        injectPopBtn = new Button();
        injectPopBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        injectPopBtn.setMaxWidth(Double.MAX_VALUE);
        injectPopBtn.setOnAction(e -> handlePopulationInjection());

        injectFoodBtn = new Button();
        injectFoodBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        injectFoodBtn.setMaxWidth(Double.MAX_VALUE);
        injectFoodBtn.setOnAction(e -> {
            GodModeDisasterExecutor.injectGlobalFood(engine, 1000.0);
            recordIntervention(
                    "FOOD_INJECT",
                    I18n.getOrDefault("godmode.spawner.food_title", "🌾 Ravitaillement Alimentaire Mondial (+1000 t)"),
                    I18n.getOrDefault("godmode.spawner.food_details", "Remplissage des silos céréaliers et réserves vivrières mondiales"),
                    0.0, 0.0, 1000.0, 365
            );
        });

        massExtinctionBtn = new Button();
        massExtinctionBtn.setStyle("-fx-background-color: #991b1b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        massExtinctionBtn.setMaxWidth(Double.MAX_VALUE);
        massExtinctionBtn.setOnAction(e -> handleMassExtinctionConfirmation());

        HBox spawnerBtnGrid = new HBox(8, injectPopBtn, injectFoodBtn);
        HBox.setHgrow(injectPopBtn, Priority.ALWAYS);
        HBox.setHgrow(injectFoodBtn, Priority.ALWAYS);
        VBox spawnerBox = new VBox(8, spawnerTitleLabel, spawnerBtnGrid, massExtinctionBtn);
        spawnerBox.getStyleClass().add("card-section");

        // Terraform components
        terraformTitleLabel = new Label();
        terraformTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        brushModeCombo = new ComboBox<>();
        brushModeCombo.setPrefWidth(210);
        brushModeCombo.setMaxWidth(260);

        applyBrushBtn = new Button();
        applyBrushBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6;");
        applyBrushBtn.setMaxWidth(Double.MAX_VALUE);
        applyBrushBtn.setOnAction(e -> {
            if (engine == null || engine.getCells() == null) return;
            GodModeDisasterExecutor.BrushOutcome outcome = GodModeDisasterExecutor.applyTerraformBrush(
                    engine, brushModeCombo.getSelectionModel().getSelectedIndex(),
                    latSpinner.getValue(), lngSpinner.getValue()
            );
            if (outcome != null) {
                recordIntervention(outcome.type(), outcome.title(), outcome.description(),
                        outcome.cell().getLatitude(), outcome.cell().getLongitude(), outcome.magnitude(), 365);
            }
        });

        VBox terraformBox = new VBox(8, terraformTitleLabel, brushModeCombo, applyBrushBtn);
        terraformBox.getStyleClass().add("card-section");

        // Reset components
        resetTitleLabel = new Label();
        resetTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        resetDisastersBtn = new Button();
        resetDisastersBtn.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6;");
        resetDisastersBtn.setMaxWidth(Double.MAX_VALUE);
        resetDisastersBtn.setOnAction(e -> {
            NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(0.0);
            recordIntervention("RESET_CLIMATE", I18n.getOrDefault("godmode.reset.title", "Aerosol Dissipation"),
                    I18n.getOrDefault("godmode.reset.details", "Return to climate equilibrium and standard stratospheric transparency (τ = 0.0)"));
        });

        VBox resetBox = new VBox(8, resetTitleLabel, resetDisastersBtn);
        resetBox.getStyleClass().add("card-section");

        // Scenario Timeline Audit Log
        timelineHeaderLabel = new Label();
        timelineHeaderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a78bfa; -fx-font-size: 12px;");

        timelineHintLabel = new Label();
        timelineHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
        timelineHintLabel.setWrapText(true);

        timelineListView = new ListView<>();
        timelineListView.setPrefHeight(180);
        timelineListView.setMinHeight(140);

        GodModeInterventionCoordinator.configureTimelineListView(timelineListView, this.timeline, this.engine, (lat, lng) -> {
            updateCoordinates(lat, lng);
            if (mapCanvas != null) {
                mapCanvas.flyTo(lat, lng);
            }
        });

        refreshTimelineView();

        VBox timelineBox = new VBox(6, timelineHeaderLabel, timelineHintLabel, timelineListView);
        timelineBox.getStyleClass().add("card-section");

        getChildren().addAll(headerLabel, pauseNoticeLabel, injectorBox, spawnerBox, terraformBox, resetBox, timelineBox);

        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    private void handlePopulationInjection() {
        double targetLat = latSpinner.getValue();
        double targetLng = lngSpinner.getValue();
        H3Cell nearest = GodModeDisasterExecutor.injectPopulationAtEpicenter(engine, targetLat, targetLng, 100_000);
        if (nearest != null) {
            targetLat = nearest.getLatitude();
            targetLng = nearest.getLongitude();
        }
        recordIntervention(
                "POP_INJECT",
                I18n.getOrDefault("godmode.spawner.pop_title", "👥 Injection Démographique (+100k hab)"),
                String.format(Locale.ROOT, "Ajout de +100 000 habitants à l'épicentre (Lat: %.2f°, Lng: %.2f°)", targetLat, targetLng),
                targetLat, targetLng, 100000.0, 365
        );
    }

    private void handleMassExtinctionConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.getOrDefault("godmode.dialog.extinction_title", "Confirmation d'Extinction Massive"));
        alert.setHeaderText(I18n.getOrDefault("godmode.dialog.extinction_header", "⚠️ Action Destructive en Mode Dieu"));
        alert.setContentText(I18n.getOrDefault("godmode.dialog.extinction_desc",
                "Are you sure you want to eliminate 50% of world population? This action will be logged irreversibly in the audit trail."));
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                GodModeDisasterExecutor.triggerMassExtinction(engine, 0.5);
                recordIntervention(
                        "MASS_EXTINCTION",
                        I18n.getOrDefault("godmode.spawner.extinction_title", "💀 Extinction Cataclysmique (-50% Pop)"),
                        I18n.getOrDefault("godmode.spawner.extinction_details", "Réduction instantanée de 50% de la biomasse humaine mondiale"),
                        0.0, 0.0, 50.0, 365
                );
            }
        });
    }

    /**
     * Binds the interactive cartographic canvas for camera fly-to operations.
     *
     * @param mapCanvas cartographic H3 canvas
     */
    public void setMapCanvas(H3MapCanvas mapCanvas) {
        this.mapCanvas = mapCanvas;
    }

    /**
     * Binds the floating notification overlay.
     *
     * @param notificationOverlay notification toast overlay
     */
    public void setNotificationOverlay(NotificationOverlay notificationOverlay) {
        this.notificationOverlay = notificationOverlay;
    }

    /**
     * Updates all localized UI texts and controls across all 5 languages.
     */
    public void updateTexts() {
        headerLabel.setText(I18n.getOrDefault("godmode.title", "⚡ 5. GOD MODE & CHRONOLOGY"));
        pauseNoticeLabel.setText(I18n.getOrDefault("godmode.pause_notice", "⏸️ Simulation is automatically paused on this tab to allow peaceful configuration of climatic disturbances without temporal drift."));

        eventTypeCombo.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.event_type", "Type of physical or climate disturbance to inject into ecosystem.")));
        eventNameField.setPromptText(I18n.getOrDefault("godmode.prompt.event_title", "Event Title or Name...")); eventNameField.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.event_title", "Custom title that will appear in chronological log and audit trail.")));
        targetYearSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.target_year", "Exact target year for triggering event in simulation calendar."))); targetYearSlider.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.target_year_slider", "Exact target year for triggering event in simulation calendar.")));
        latSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.lat", "Latitude of physical phenomenon epicenter (-90° South to +90° North)."))); lngSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.lng", "Longitude of physical phenomenon epicenter (-180° West to +180° East).")));
        magnitudeSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.magnitude", "Shock intensity / magnitude (determines depth and spatial impact of perturbation)."))); durationDaysSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.duration_days", "Active duration of the physical phenomenon in simulation days.")));

        injectorTitleLabel.setText(I18n.getOrDefault("godmode.injector.title", "🛠️ CLIMATE EVENT EDITING & PROGRAMMING:"));
        lblEventType.setText(I18n.getOrDefault("godmode.label.event_type", "Event Type:")); lblEventTitle.setText(I18n.getOrDefault("godmode.label.event_title", "Name / Title:")); lblTargetYear.setText(I18n.getOrDefault("godmode.label.target_year", "Target Year (Date):"));
        lblLat.setText(I18n.getOrDefault("godmode.label.lat", "Latitude (-90 to +90°):")); lblLng.setText(I18n.getOrDefault("godmode.label.lng", "Longitude (-180 to +180°):")); lblMag.setText(I18n.getOrDefault("godmode.label.magnitude", "Intensity / Magnitude:"));
        lblRadius.setText(I18n.getOrDefault("godmode.label.radius", "Action Radius (km):")); lblDurationDays.setText(I18n.getOrDefault("godmode.label.duration_days", "Active Duration:")); lblDurationUnit.setText(I18n.getOrDefault("godmode.unit.days", "jours"));
        radiusKmSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.radius", "Geographical impact or influence radius of the event (in km).")));

        scheduleBtn.setText(I18n.getOrDefault("godmode.btn.schedule", "📅 Schedule in Timeline"));
        scheduleBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.schedule", "Schedules event in the scenario calendar for automatic triggering.")));
        triggerNowBtn.setText(I18n.getOrDefault("godmode.btn.trigger_now", "⚡ Trigger Immediately"));
        triggerNowBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.trigger_now", "Instantly applies physical and climate disturbances to the current world in real time.")));

        spawnerTitleLabel.setText(I18n.getOrDefault("godmode.spawner.header", "🌱 DIRECT POPULATION & RESOURCE INJECTION:"));
        injectPopBtn.setText(I18n.getOrDefault("godmode.btn.inject_pop", "👥 Inject 100,000 Inhabitants (Epicenter)"));
        injectPopBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.inject_pop", "Injects a cohort of 100,000 inhabitants at the specified geographical position.")));
        injectFoodBtn.setText(I18n.getOrDefault("godmode.btn.inject_food", "🌾 Inject Food Stock (Silos)"));
        injectFoodBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.inject_food", "Fills grain silos to 100% to prevent immediate famines.")));
        massExtinctionBtn.setText(I18n.getOrDefault("godmode.btn.mass_extinction", "💀 Trigger Mass Extinction (-50% Population)"));
        massExtinctionBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.mass_extinction", "Instantly reduces active world population by 50% (Cataclysmic Shock).")));

        terraformTitleLabel.setText(I18n.getOrDefault("godmode.terraform.header", "🖌️ SPATIAL BRUSH & LOCAL DYNAMICS:"));
        GodModeUiConfigurator.updateBrushItems(brushModeCombo);

        setBtn(applyBrushBtn, "godmode.btn.apply_brush", "🖌️ Apply to Epicenter Coordinates", "godmode.tooltip.apply_brush", "Applies selected action directly to the targeted H3 cell.");
        resetTitleLabel.setText(I18n.getOrDefault("godmode.reset.header", "🛑 PHYSICAL NORMALIZATION & RESET:"));
        setBtn(resetDisastersBtn, "godmode.btn.reset_disasters", "🛑 Stop All Disasters & Dissipate Soot", "godmode.tooltip.reset_disasters", "Resets soot optical depth (τ = 0.0) and cancels active perturbations.");

        int minYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getStartDateYear() : -100000;
        int maxYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getEndDateYear() : 2100;
        if (minYr >= maxYr) { minYr = -100000; maxYr = 2100; }
        dateRangeLabel.setText(String.format(I18n.getOrDefault("godmode.label.date_range", "📅 Allowed Range: [Year %,d ➔ Year %,d]"), minYr, maxYr));

        EventTypeItem currentType = eventTypeCombo.getValue();
        if (currentType != null) {
            eventNameField.setText(currentType.getDefaultTitle());
            eventDescriptionLabel.setText(currentType.getDescription());
        }
        if (eventTypeCombo.getCellFactory() != null) {
            eventTypeCombo.setButtonCell(eventTypeCombo.getCellFactory().call(null));
        }

        timelineHeaderLabel.setText(I18n.getOrDefault("godmode.timeline.title", "📜 SCENARIO CHRONOLOGY & AUDIT TRAIL:"));
        timelineHintLabel.setText(I18n.getOrDefault("godmode.timeline.click_hint", "💡 Click on a timeline event to recenter 2D / 3D camera view on its coordinates."));
        timelineListView.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.timeline", "Chronological audit log of all scenario events and divine interventions.")));

        refreshTimelineView();
    }

    private void scheduleEvent(boolean immediate) {
        GodModeInterventionCoordinator.scheduleOrExecuteEvent(
                immediate,
                eventTypeCombo.getValue(),
                eventNameField.getText(),
                targetYearSpinner.getValue(),
                targetMonthSpinner.getValue(),
                targetDaySpinner.getValue(),
                latSpinner.getValue(),
                lngSpinner.getValue(),
                magnitudeSpinner.getValue(),
                radiusKmSpinner != null ? radiusKmSpinner.getValue() : 1000.0,
                durationDaysSpinner.getValue(),
                engine,
                timeline,
                mapCanvas,
                this::refreshTimelineView
        );
    }

    /**
     * Records a divine intervention in the scenario timeline using current spinner values.
     *
     * @param type event identifier
     * @param title human-readable title
     * @param details descriptive details
     */
    public void recordIntervention(String type, String title, String details) {
        recordIntervention(type, title, details, latSpinner != null ? latSpinner.getValue() : 0.0, lngSpinner != null ? lngSpinner.getValue() : 0.0, magnitudeSpinner != null ? magnitudeSpinner.getValue() : 5.0, durationDaysSpinner != null ? durationDaysSpinner.getValue() : 30);
    }
    public void recordIntervention(String type, String title, String details, double lat, double lng, double mag, int durationDays) {
        GodModeInterventionCoordinator.recordIntervention(engine, timeline, type, title, details, lat, lng, mag, durationDays, this::refreshTimelineView);
    }
    public void refreshTimelineView() { GodModeInterventionCoordinator.refreshTimelineView(timelineListView, timeline); }
    public void updateCoordinates(double lat, double lng) {
        if (latSpinner != null && latSpinner.getValueFactory() != null) latSpinner.getValueFactory().setValue(Math.max(-90.0, Math.min(90.0, lat)));
        if (lngSpinner != null && lngSpinner.getValueFactory() != null) lngSpinner.getValueFactory().setValue(Math.max(-180.0, Math.min(180.0, lng)));
    }

    private static void setBtn(Button b, String tk, String td, String tipk, String tipd) {
        if (b != null) { b.setText(I18n.getOrDefault(tk, td)); b.setTooltip(new Tooltip(I18n.getOrDefault(tipk, tipd))); }
    }
}