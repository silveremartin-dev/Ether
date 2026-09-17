package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.procedural.NuclearWarfareClimateEngine;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * God Mode & Interactive Timeline Controller.
 * Allows pausing live simulation, injecting real physical events (volcanoes, EMP, heatwaves),
 * selecting duration, and viewing/navigating the chronological scenario timeline audit trail.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.1.0
 */
public class GodModePanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(GodModePanel.class);

    public record EventTypeItem(
        String id,
        String typeI18nKey,
        String nameI18nKey,
        String descI18nKey,
        int defaultDurationDays,
        double defaultMagnitude
    ) {
        public String getDisplayName() {
            return I18n.getOrDefault(typeI18nKey, id);
        }

        public String getDefaultTitle() {
            return I18n.getOrDefault(nameI18nKey, id);
        }

        public String getDescription() {
            return I18n.getOrDefault(descI18nKey, "");
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    private static final List<EventTypeItem> EVENT_TYPES = List.of(
        new EventTypeItem("VOLCANO", "godmode.type.volcano", "godmode.event.volcano", "godmode.event.volcano.desc", 365, 7.0),
        new EventTypeItem("HEATWAVE", "godmode.type.heatwave", "godmode.event.heatwave", "godmode.event.heatwave.desc", 30, 5.0),
        new EventTypeItem("SOLAR_EMP", "godmode.type.solar_emp", "godmode.event.solar_emp", "godmode.event.solar_emp.desc", 30, 8.5),
        new EventTypeItem("PANDEMIC", "godmode.type.pandemic", "godmode.event.pandemic", "godmode.event.pandemic.desc", 730, 8.0),
        new EventTypeItem("METEOR", "godmode.type.meteor", "godmode.event.meteor", "godmode.event.meteor.desc", 7, 9.0),
        new EventTypeItem("NUCLEAR_WINTER", "godmode.type.nuclear_winter", "godmode.event.nuclear_winter", "godmode.event.nuclear_winter.desc", 3650, 7.5),
        new EventTypeItem("FAMINE", "godmode.type.famine", "godmode.event.famine", "godmode.event.famine.desc", 180, 6.0),
        new EventTypeItem("TSUNAMI", "godmode.type.tsunami", "godmode.event.tsunami", "godmode.event.tsunami.desc", 1, 8.5),
        new EventTypeItem("EARTHQUAKE", "godmode.type.earthquake", "godmode.event.earthquake", "godmode.event.earthquake.desc", 3, 8.0),
        new EventTypeItem("ICE_AGE", "godmode.type.ice_age", "godmode.event.ice_age", "godmode.event.ice_age.desc", 18250, 5.0),
        new EventTypeItem("CYBER_ATTACK", "godmode.type.cyber_attack", "godmode.event.cyber_attack", "godmode.event.cyber_attack.desc", 14, 6.5),
        new EventTypeItem("ECONOMIC_CRASH", "godmode.type.economic_crash", "godmode.event.economic_crash", "godmode.event.economic_crash.desc", 365, 7.0),
        new EventTypeItem("BIODIVERSITY_COLLAPSE", "godmode.type.biodiversity_collapse", "godmode.event.biodiversity_collapse", "godmode.event.biodiversity_collapse.desc", 3650, 6.0),
        new EventTypeItem("GEOENGINEERING", "godmode.type.geoengineering", "godmode.event.geoengineering", "godmode.event.geoengineering.desc", 730, 4.0),
        new EventTypeItem("RENAISSANCE_BOOM", "godmode.type.renaissance_boom", "godmode.event.renaissance_boom", "godmode.event.renaissance_boom.desc", 7300, 8.0),
        new EventTypeItem("TECH_SINGULARITY", "godmode.type.tech_singularity", "godmode.event.tech_singularity", "godmode.event.tech_singularity.desc", 3650, 9.5),
        new EventTypeItem("ALIEN_CONTACT", "godmode.type.alien_contact", "godmode.event.alien_contact", "godmode.event.alien_contact.desc", 1825, 10.0)
    );

    private final H3SimulationEngine engine;
    private final ScenarioTimeline timeline;
    private H3MapCanvas mapCanvas;
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
    private final Slider targetYearSlider;
    private final Spinner<Double> latSpinner;
    private final Spinner<Double> lngSpinner;
    private final Spinner<Double> magnitudeSpinner;
    private final Spinner<Integer> durationDaysSpinner;

    // Injector section labels & buttons
    private final Label injectorTitleLabel;
    private final Label lblEventType;
    private final Label lblEventTitle;
    private final Label lblTargetYear;
    private final Label lblLat;
    private final Label lblLng;
    private final Label lblMag;
    private final Label lblDurationDays;
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

    public GodModePanel(H3SimulationEngine engine, ScenarioTimeline timeline) {
        this.engine = engine;
        this.timeline = timeline != null ? timeline : new ScenarioTimeline();

        setPadding(new Insets(12, 12, 35, 12));
        setSpacing(10);
        getStyleClass().add("glass-panel");

        // Title Header
        headerLabel = new Label();
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #38bdf8;");

        // Explanatory Pause Banner
        pauseNoticeLabel = new Label();
        pauseNoticeLabel.setWrapText(true);
        pauseNoticeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #fcd34d; -fx-padding: 8 10; -fx-background-color: rgba(245, 158, 11, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(245, 158, 11, 0.4); -fx-border-radius: 6; -fx-line-spacing: 2px;");

        // Form fields initialization
        eventTypeCombo = new ComboBox<>();
        eventTypeCombo.getItems().addAll(EVENT_TYPES);
        eventTypeCombo.setPrefWidth(220);
        eventTypeCombo.setMaxWidth(280);

        eventTypeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(EventTypeItem item) {
                return item != null ? item.getDisplayName() : "";
            }
            @Override
            public EventTypeItem fromString(String string) {
                if (string == null) return null;
                for (EventTypeItem item : EVENT_TYPES) {
                    if (item.getDisplayName().equalsIgnoreCase(string) || item.id().equalsIgnoreCase(string)) {
                        return item;
                    }
                }
                return null;
            }
        });

        eventTypeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(EventTypeItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        eventTypeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(EventTypeItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });

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

        targetYearSlider = new Slider(minYr, maxYr, Math.max(minYr, Math.min(maxYr, currentYr)));
        targetYearSlider.setBlockIncrement(1);
        targetYearSlider.setMajorTickUnit(Math.max(1, (maxYr - minYr) / 5.0));
        targetYearSlider.setMinorTickCount(4);
        targetYearSlider.setShowTickMarks(false);
        targetYearSlider.setPrefWidth(120);
        targetYearSlider.setMaxWidth(150);

        // Synchronize Spinner & Slider
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

        latSpinner = new Spinner<>(-90.0, 90.0, 0.0, 1.0);
        latSpinner.setEditable(true);
        latSpinner.setPrefWidth(95);
        latSpinner.setMaxWidth(110);

        lngSpinner = new Spinner<>(-180.0, 180.0, 0.0, 1.0);
        lngSpinner.setEditable(true);
        lngSpinner.setPrefWidth(95);
        lngSpinner.setMaxWidth(110);

        magnitudeSpinner = new Spinner<>(0.1, 10.0, 7.0, 0.5);
        magnitudeSpinner.setEditable(true);
        magnitudeSpinner.setPrefWidth(95);
        magnitudeSpinner.setMaxWidth(110);

        durationDaysSpinner = new Spinner<>(1, 36500, 365, 1);
        durationDaysSpinner.setEditable(true);
        durationDaysSpinner.setPrefWidth(95);
        durationDaysSpinner.setMaxWidth(110);

        // Update defaults when event type selection changes
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
        });
        eventTypeCombo.getSelectionModel().select(0);

        // Injector section labels
        injectorTitleLabel = new Label();
        injectorTitleLabel.getStyleClass().add("label-section-header");
        lblEventType = createLabel();
        lblEventTitle = createLabel();
        lblTargetYear = createLabel();
        lblLat = createLabel();
        lblLng = createLabel();
        lblMag = createLabel();
        lblDurationDays = createLabel();

        scheduleBtn = new Button();
        scheduleBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 12; -fx-background-radius: 6;");
        scheduleBtn.setMaxWidth(Double.MAX_VALUE);
        scheduleBtn.setOnAction(e -> scheduleEvent(false));

        triggerNowBtn = new Button();
        triggerNowBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 12; -fx-background-radius: 6;");
        triggerNowBtn.setMaxWidth(Double.MAX_VALUE);
        triggerNowBtn.setOnAction(e -> scheduleEvent(true));

        VBox injectorBox = createInjectorSection();
        injectorBox.getStyleClass().add("card-section");

        // Spawner components
        spawnerTitleLabel = new Label();
        spawnerTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10b981;");

        injectPopBtn = new Button();
        injectPopBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        injectPopBtn.setMaxWidth(Double.MAX_VALUE);
        injectPopBtn.setOnAction(e -> {
            if (engine != null && engine.getCells() != null) {
                double targetLat = latSpinner.getValue();
                double targetLng = lngSpinner.getValue();
                H3Cell nearest = null;
                double minDist = Double.MAX_VALUE;
                for (H3Cell c : engine.getCells()) {
                    double dist = Math.hypot(c.getLatitude() - targetLat, c.getLongitude() - targetLng);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = c;
                    }
                }
                if (nearest != null) {
                    nearest.setPopulation(nearest.getPopulation() + 100_000);
                    if (engine.getWorldBuffer() != null) {
                        org.ether.society.data.DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
                    }
                }
            }
            recordIntervention("POP_INJECT", I18n.getOrDefault("godmode.spawner.pop_title", "Demographic Injection"),
                    I18n.getOrDefault("godmode.spawner.pop_details", "Addition of +100,000 inhabitants at coordinates (Lat: ") + latSpinner.getValue() + ", Lng: " + lngSpinner.getValue() + ")");
        });

        injectFoodBtn = new Button();
        injectFoodBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        injectFoodBtn.setMaxWidth(Double.MAX_VALUE);
        injectFoodBtn.setOnAction(e -> {
            if (engine != null && engine.getCells() != null) {
                for (H3Cell c : engine.getCells()) {
                    c.setFoodResource((c.getFoodResource() != null ? c.getFoodResource() : 0.0) + 1000.0);
                }
                if (engine.getWorldBuffer() != null) {
                    org.ether.society.data.DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
                }
            }
            recordIntervention("FOOD_INJECT", I18n.getOrDefault("godmode.spawner.food_title", "Injection Alimentaire"),
                    I18n.getOrDefault("godmode.spawner.food_details", "Refilling global grain stocks (+12 months)"));
        });

        massExtinctionBtn = new Button();
        massExtinctionBtn.setStyle("-fx-background-color: #991b1b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        massExtinctionBtn.setMaxWidth(Double.MAX_VALUE);
        massExtinctionBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("godmode.dialog.extinction_title", "Confirmation d'Extinction Massive"));
            alert.setHeaderText(I18n.getOrDefault("godmode.dialog.extinction_header", "⚠️ Action Destructive en Mode Dieu"));
            alert.setContentText(I18n.getOrDefault("godmode.dialog.extinction_desc", "Are you sure you want to eliminate 50% of world population? This action will be logged irreversibly in the audit trail."));
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (engine != null && engine.getCells() != null) {
                        for (H3Cell c : engine.getCells()) {
                            c.setPopulation((int) (c.getPopulation() * 0.5));
                        }
                        if (engine.getWorldBuffer() != null) {
                            org.ether.society.data.DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
                        }
                    }
                    recordIntervention("MASS_EXTINCTION", I18n.getOrDefault("godmode.spawner.extinction_title", "Extinction Cataclysmique"),
                            I18n.getOrDefault("godmode.spawner.extinction_details", "Immediate 50% reduction in global human biomass"));
                }
            });
        });

        VBox spawnerBox = createSpawnerSection();
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
            double targetLat = latSpinner.getValue();
            double targetLng = lngSpinner.getValue();
            int selectedIdx = brushModeCombo.getSelectionModel().getSelectedIndex();

            H3Cell nearest = null;
            double minDist = Double.MAX_VALUE;
            for (H3Cell c : engine.getCells()) {
                double dist = Math.hypot(c.getLatitude() - targetLat, c.getLongitude() - targetLng);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = c;
                }
            }

            if (nearest != null) {
                switch (selectedIdx) {
                    case 0 -> nearest.setPopulation(nearest.getPopulation() + 50000);
                    case 1 -> nearest.setFoodResource((nearest.getFoodResource() != null ? nearest.getFoodResource() : 0.0) + 500.0);
                    case 2 -> nearest.setFreshwaterAquifer((nearest.getFreshwaterAquifer() != null ? nearest.getFreshwaterAquifer() : 0.0) + 2000.0);
                    case 3 -> nearest.setTemperature((nearest.getTemperature() != null ? nearest.getTemperature() : 15.0) + 10.0);
                    case 4 -> nearest.setTemperature((nearest.getTemperature() != null ? nearest.getTemperature() : 15.0) - 10.0);
                    case 5 -> nearest.setPollutionLevel(0.0);
                }

                recordIntervention("TERRAFORM_BRUSH", I18n.getOrDefault("godmode.brush.title", "Pinceau Spatial"),
                        I18n.getOrDefault("godmode.brush.details", "Brush action applied on mesh Lat ") + String.format(Locale.ROOT, "%.2f", nearest.getLatitude()) + "°, Lng " + String.format(Locale.ROOT, "%.2f", nearest.getLongitude()) + "°");
            }
        });

        VBox terraformBox = createTerraformSection();
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

        VBox resetBox = createResetSection();
        resetBox.getStyleClass().add("card-section");

        // Section 5: Scenario Timeline Audit Log
        timelineHeaderLabel = new Label();
        timelineHeaderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a78bfa; -fx-font-size: 12px;");

        timelineHintLabel = new Label();
        timelineHintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
        timelineHintLabel.setWrapText(true);

        timelineListView = new ListView<>();
        timelineListView.setPrefHeight(180);
        timelineListView.setMinHeight(140);

        timelineListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    int currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
                    int eventYear = currentYear;
                    try {
                        if (item.contains("|")) {
                            String yearPart = item.substring(0, item.indexOf('|')).replaceAll("[^0-9-]", "").trim();
                            if (!yearPart.isEmpty()) {
                                eventYear = Integer.parseInt(yearPart);
                            }
                        }
                    } catch (Exception ignored) {}

                    if (eventYear < currentYear) {
                        setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-cursor: hand;");
                    } else if (eventYear == currentYear) {
                        setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
                    } else {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-style: italic; -fx-font-size: 11px; -fx-cursor: hand;");
                    }
                }
            }
        });

        // Click on timeline event recenters 2D/3D camera on the event
        timelineListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (newIdx == null || newIdx.intValue() < 0) return;
            List<ScenarioTimeline.TimelineEntry> entries = timeline.getEntries();
            if (newIdx.intValue() < entries.size()) {
                ScenarioTimeline.TimelineEntry entry = entries.get(newIdx.intValue());
                double lat = entry.latitude();
                double lng = entry.longitude();

                // If coordinates were 0, try parsing from details
                if (lat == 0.0 && lng == 0.0 && entry.details() != null) {
                    try {
                        if (entry.details().contains("Lat:") && entry.details().contains("Lng:")) {
                            String d = entry.details();
                            int latStart = d.indexOf("Lat:") + 4;
                            int latEnd = d.indexOf("°", latStart);
                            int lngStart = d.indexOf("Lng:") + 4;
                            int lngEnd = d.indexOf("°", lngStart);
                            if (latStart > 3 && latEnd > latStart && lngStart > 3 && lngEnd > lngStart) {
                                lat = Double.parseDouble(d.substring(latStart, latEnd).trim());
                                lng = Double.parseDouble(d.substring(lngStart, lngEnd).trim());
                            }
                        }
                    } catch (Exception ignored) {}
                }

                updateCoordinates(lat, lng);

                if (mapCanvas != null) {
                    mapCanvas.flyTo(lat, lng);
                }

                if (notificationOverlay != null) {
                    notificationOverlay.showNotification(
                        "🎯 " + entry.title() + "\n" + String.format(Locale.ROOT, "📍 Lat: %.2f°, Lng: %.2f°", lat, lng),
                        "#38bdf8"
                    );
                }
            }
        });

        refreshTimelineView();

        VBox timelineBox = new VBox(6, timelineHeaderLabel, timelineHintLabel, timelineListView);
        timelineBox.getStyleClass().add("card-section");

        getChildren().addAll(headerLabel, pauseNoticeLabel, injectorBox, spawnerBox, terraformBox, resetBox, timelineBox);

        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    public void setMapCanvas(H3MapCanvas mapCanvas) {
        this.mapCanvas = mapCanvas;
    }

    public void setNotificationOverlay(NotificationOverlay notificationOverlay) {
        this.notificationOverlay = notificationOverlay;
    }

    public void updateTexts() {
        headerLabel.setText(I18n.getOrDefault("godmode.title", "⚡ 5. MODE DIEU & CHRONOLOGIE"));
        pauseNoticeLabel.setText(I18n.getOrDefault("godmode.pause_notice", "⏸️ La simulation est automatiquement mise en pause sur cet onglet pour vous permettre de configurer et programmer sereinement vos événements climatiques et interventions sans décalage temporel."));

        eventTypeCombo.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.event_type", "Type of physical or climate disturbance to inject into ecosystem.")));
        eventNameField.setPromptText(I18n.getOrDefault("godmode.prompt.event_title", "Event Title or Name..."));
        eventNameField.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.event_title", "Custom title that will appear in chronological log and audit trail.")));
        targetYearSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.target_year", "Exact target year for triggering event in simulation calendar.")));
        targetYearSlider.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.target_year_slider", "Exact target year for triggering event in simulation calendar.")));
        latSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.lat", "Latitude of physical phenomenon epicenter (-90° South to +90° North).")));
        lngSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.lng", "Longitude of physical phenomenon epicenter (-180° West to +180° East).")));
        magnitudeSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.magnitude", "Shock intensity / magnitude (determines depth and spatial impact of perturbation).")));
        durationDaysSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.duration_days", "Active duration of the physical phenomenon in simulation days.")));

        injectorTitleLabel.setText(I18n.getOrDefault("godmode.injector.title", "🛠️ ÉDITION & PROGRAMMATION D'ÉVÉNEMENTS CLIMATIQUES :"));
        lblEventType.setText(I18n.getOrDefault("godmode.label.event_type", "Type d'Événement :"));
        lblEventTitle.setText(I18n.getOrDefault("godmode.label.event_title", "Nom / Titre :"));
        lblTargetYear.setText(I18n.getOrDefault("godmode.label.target_year", "Année Cible (Date) :"));
        lblLat.setText(I18n.getOrDefault("godmode.label.lat", "Latitude (-90 à +90°) :"));
        lblLng.setText(I18n.getOrDefault("godmode.label.lng", "Longitude (-180 à +180°) :"));
        lblMag.setText(I18n.getOrDefault("godmode.label.magnitude", "Intensité / Magnitude :"));
        lblDurationDays.setText(I18n.getOrDefault("godmode.label.duration_days", "Durée (en jours) :"));

        scheduleBtn.setText(I18n.getOrDefault("godmode.btn.schedule", "📅 Programmer dans la Chronologie"));
        scheduleBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.schedule", "Planifie l'événement dans le calendrier du scénario pour un déclenchement automatique.")));
        triggerNowBtn.setText(I18n.getOrDefault("godmode.btn.trigger_now", "⚡ Déclencher Immédiatement"));
        triggerNowBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.trigger_now", "Applique instantanément les perturbations physiques et climatiques au monde actuel en temps réel.")));

        spawnerTitleLabel.setText(I18n.getOrDefault("godmode.spawner.header", "🌱 INJECTION DIRECTE DE POPULATION & RESSOURCES :"));
        injectPopBtn.setText(I18n.getOrDefault("godmode.btn.inject_pop", "👥 Injecter 100 000 Habitants (Épicentre)"));
        injectPopBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.inject_pop", "Injecte une cohorte de 100 000 habitants à la position géographique spécifiée.")));
        injectFoodBtn.setText(I18n.getOrDefault("godmode.btn.inject_food", "🌾 Injecter Stock Alimentaire (Silos)"));
        injectFoodBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.inject_food", "Remplit les silos céréaliers à 100% pour prévenir les famines immédiates.")));
        massExtinctionBtn.setText(I18n.getOrDefault("godmode.btn.mass_extinction", "💀 Déclencher Extinction Massive (-50% Population)"));
        massExtinctionBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.mass_extinction", "Réduit instantanément de 50% la population mondiale active (Choc Cataclysmique).")));

        terraformTitleLabel.setText(I18n.getOrDefault("godmode.terraform.header", "🖌️ PINCEAU SPATIAL & DYNAMIQUES LOCALES :"));
        int selIdx = brushModeCombo.getSelectionModel().getSelectedIndex();
        brushModeCombo.getItems().clear();
        brushModeCombo.getItems().addAll(
            I18n.getOrDefault("godmode.brush.pop", "👥 Boost Population (+50 000 hab)"),
            I18n.getOrDefault("godmode.brush.agri", "🌾 Injection Agricole & Silos (+500 t)"),
            I18n.getOrDefault("godmode.brush.water", "🚰 Recharge Aquifère (+2 000 m³)"),
            I18n.getOrDefault("godmode.brush.heat", "🔥 Vague de Chaleur Locale (+10,0°C)"),
            I18n.getOrDefault("godmode.brush.cold", "❄️ Refroidissement Local (-10,0°C)"),
            I18n.getOrDefault("godmode.brush.clean", "🧼 Nettoyage Écologique Intégral (0.0)")
        );
        brushModeCombo.getSelectionModel().select(selIdx >= 0 ? selIdx : 0);
        brushModeCombo.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.brush_mode", "Sélectionnez l'effet local à appliquer.")));

        applyBrushBtn.setText(I18n.getOrDefault("godmode.btn.apply_brush", "🖌️ Appliquer aux Coordonnées de l'Épicentre"));
        applyBrushBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.apply_brush", "Applique l'action sélectionnée directement sur la maille H3 ciblée.")));

        resetTitleLabel.setText(I18n.getOrDefault("godmode.reset.header", "🛑 NORMALISATION & RÉINITIALISATION PHYSIQUE :"));
        resetDisastersBtn.setText(I18n.getOrDefault("godmode.btn.reset_disasters", "🛑 Stopper Tous les Désastres & Dissiper les Suies"));
        resetDisastersBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.reset_disasters", "Réinitialise la profondeur optique des suies (τ = 0.0) et annule les perturbations actives.")));

        int minYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getStartDateYear() : -100000;
        int maxYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getEndDateYear() : 2100;
        if (minYr >= maxYr) { minYr = -100000; maxYr = 2100; }
        if (dateRangeLabel != null) {
            dateRangeLabel.setText(String.format(I18n.getOrDefault("godmode.label.date_range", "📅 Allowed Range: [Year %,d ➔ Year %,d]"), minYr, maxYr));
        }

        // Refresh event type display
        EventTypeItem currentType = eventTypeCombo.getValue();
        if (currentType != null) {
            eventNameField.setText(currentType.getDefaultTitle());
            eventDescriptionLabel.setText(currentType.getDescription());
        }

        timelineHeaderLabel.setText(I18n.getOrDefault("godmode.timeline.title", "📜 CHRONOLOGIE DU SCÉNARIO & AUDIT TRAIL :"));
        timelineHintLabel.setText(I18n.getOrDefault("godmode.timeline.click_hint", "💡 Cliquez sur un événement de la chronologie pour recentrer la vue 2D / 3D sur ses coordonnées."));
        timelineListView.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.timeline", "Piste d'audit temporel : Liste chronologique de tous les forçages et événements du scénario.")));

        refreshTimelineView();
    }

    private VBox createInjectorSection() {
        VBox box = new VBox(8);
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(140);
        col0.setPrefWidth(150);
        col0.setHgrow(Priority.NEVER);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col0, col1);

        grid.addRow(0, lblEventType, eventTypeCombo);
        grid.add(eventDescriptionLabel, 1, 1);
        grid.addRow(2, lblEventTitle, eventNameField);

        HBox yearRow = new HBox(8, targetYearSpinner, targetYearSlider);
        yearRow.setAlignment(Pos.CENTER_LEFT);
        VBox yearBox = new VBox(3, yearRow, dateRangeLabel);
        grid.addRow(3, lblTargetYear, yearBox);

        grid.addRow(4, lblLat, latSpinner);
        grid.addRow(5, lblLng, lngSpinner);
        grid.addRow(6, lblMag, magnitudeSpinner);
        grid.addRow(7, lblDurationDays, durationDaysSpinner);

        HBox btnBox = new HBox(8, scheduleBtn, triggerNowBtn);
        HBox.setHgrow(scheduleBtn, Priority.ALWAYS);
        HBox.setHgrow(triggerNowBtn, Priority.ALWAYS);

        box.getChildren().addAll(injectorTitleLabel, grid, btnBox);
        return box;
    }

    private VBox createSpawnerSection() {
        VBox box = new VBox(8);
        HBox btnGrid = new HBox(8, injectPopBtn, injectFoodBtn);
        HBox.setHgrow(injectPopBtn, Priority.ALWAYS);
        HBox.setHgrow(injectFoodBtn, Priority.ALWAYS);

        box.getChildren().addAll(spawnerTitleLabel, btnGrid, massExtinctionBtn);
        return box;
    }

    private VBox createTerraformSection() {
        VBox box = new VBox(8);
        box.getChildren().addAll(terraformTitleLabel, brushModeCombo, applyBrushBtn);
        return box;
    }

    private VBox createResetSection() {
        VBox box = new VBox(8);
        box.getChildren().addAll(resetTitleLabel, resetDisastersBtn);
        return box;
    }

    private Label createLabel() {
        Label l = new Label();
        l.getStyleClass().add("control-label");
        l.setMinWidth(Region.USE_PREF_SIZE);
        l.setWrapText(false);
        return l;
    }

    private void scheduleEvent(boolean immediate) {
        EventTypeItem item = eventTypeCombo.getValue();
        String type = item != null ? item.id() : "VOLCANO";
        String name = eventNameField.getText();
        if (name == null || name.isBlank()) name = item != null ? item.getDefaultTitle() : type;

        int currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        int currentMonth = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentMonth() : 0;
        int currentDay = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentDay() : 1;
        long currentTicks = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getTotalTicks() : 0;
        int targetYear = immediate ? currentYear : targetYearSpinner.getValue();

        double lat = latSpinner.getValue();
        double lng = lngSpinner.getValue();
        double mag = magnitudeSpinner.getValue();
        int durationDays = durationDaysSpinner.getValue();

        String daysUnit = durationDays > 1 ? "jours" : "jour";
        String details = String.format(Locale.ROOT, "Lat: %.2f°, Lng: %.2f°, Mag: %.1f, Durée: %d %s", lat, lng, mag, durationDays, daysUnit);

        if (immediate || targetYear <= currentYear) {
            if (engine != null && engine.getHistoryManager() != null) {
                engine.getHistoryManager().truncateAfter(currentYear, currentMonth, currentTicks);
            }
            timeline.truncateAfter(currentYear);
        }

        timeline.addEntry(targetYear, type, name, details, true, lat, lng, mag, durationDays);
        refreshTimelineView();

        if (engine != null && engine.getEventSystem() != null) {
            org.ether.society.events.ActiveEvent ae = new org.ether.society.events.ActiveEvent(
                "GM_" + System.currentTimeMillis(),
                "⚡ GOD MODE: " + name,
                type, lat, lng, targetYear, currentMonth, currentDay, Math.max(15.0, durationDays * 0.5), mag, durationDays
            );
            engine.getEventSystem().recordSpatialEvent(ae);
        }

        if (immediate || targetYear <= currentYear) {
            executePhysicalForcing(type, mag, lat, lng);
            if (engine != null && engine.getWorldBuffer() != null && engine.getCells() != null) {
                org.ether.society.data.DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
                if (engine.getHistoryManager() != null) {
                    engine.getHistoryManager().captureSnapshot(engine);
                    engine.getHistoryManager().captureWorldSnapshot(engine);
                }
            }
            logger.info("God Mode intervention EXECUTED immediately (Year {}): {} - {}", currentYear, name, details);
            if (mapCanvas != null) {
                mapCanvas.flyTo(lat, lng);
            }
        } else {
            logger.info("God Mode intervention SCHEDULED for Year {}: {} - {}", targetYear, name, details);
        }
    }

    private void executePhysicalForcing(String type, double mag, double lat, double lng) {
        if (type == null) return;
        switch (type) {
            case "VOLCANO" -> NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(mag);
            case "HEATWAVE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        c.setTemperature((c.getTemperature() != null ? c.getTemperature() : 15.0) + mag * 2.0);
                    }
                }
            }
            case "NUCLEAR_WINTER", "ICE_AGE" -> NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(mag * 2.5);
            case "METEOR" -> NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(mag * 3.5);
            case "GEOENGINEERING" -> NuclearWarfareClimateEngine.setGlobalSootOpticalDepth(Math.max(0, NuclearWarfareClimateEngine.getGlobalSootOpticalDepth() - mag));
            case "FAMINE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        c.setFoodResource(Math.max(0.0, (c.getFoodResource() != null ? c.getFoodResource() : 100.0) * (1.0 - mag * 0.08)));
                    }
                }
            }
            case "TSUNAMI", "EARTHQUAKE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        double dist = Math.hypot(c.getLatitude() - lat, c.getLongitude() - lng);
                        if (dist < mag * 2.0) {
                            int pop = c.getPopulation();
                            c.setPopulation((int) (pop * Math.max(0.1, 1.0 - (mag * 0.15))));
                        }
                    }
                }
            }
            case "PANDEMIC", "BIODIVERSITY_COLLAPSE" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        int pop = c.getPopulation();
                        c.setPopulation((int) (pop * Math.max(0.2, 1.0 - (mag * 0.05))));
                    }
                }
            }
            case "SOLAR_EMP", "CYBER_ATTACK", "ECONOMIC_CRASH" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        c.setTechnologyLevel(Math.max(0.0, (c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 10.0) * (1.0 - mag * 0.05)));
                    }
                }
            }
            case "RENAISSANCE_BOOM", "TECH_SINGULARITY", "ALIEN_CONTACT" -> {
                if (engine != null && engine.getCells() != null) {
                    for (H3Cell c : engine.getCells()) {
                        c.setTechnologyLevel((c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 10.0) + mag * 5.0);
                    }
                }
            }
        }
    }

    public void recordIntervention(String type, String title, String details) {
        int currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        int currentMonth = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentMonth() : 0;
        int currentDay = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentDay() : 1;
        long currentTicks = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getTotalTicks() : 0;

        if (engine != null && engine.getHistoryManager() != null) {
            engine.getHistoryManager().truncateAfter(currentYear, currentMonth, currentTicks);
            if (engine.getWorldBuffer() != null && engine.getCells() != null) {
                org.ether.society.data.DODDataGenerator.populateWorldBuffer(engine.getCells(), engine.getWorldBuffer());
            }
            engine.getHistoryManager().captureSnapshot(engine);
            engine.getHistoryManager().captureWorldSnapshot(engine);
        }

        double lat = latSpinner != null ? latSpinner.getValue() : 0.0;
        double lng = lngSpinner != null ? lngSpinner.getValue() : 0.0;
        double mag = magnitudeSpinner != null ? magnitudeSpinner.getValue() : 5.0;
        int durationDays = durationDaysSpinner != null ? durationDaysSpinner.getValue() : 30;

        timeline.truncateAfter(currentYear);
        timeline.addEntry(currentYear, type, title, details, true, lat, lng, mag, durationDays);
        refreshTimelineView();

        if (engine != null && engine.getEventSystem() != null) {
            org.ether.society.events.ActiveEvent ae = new org.ether.society.events.ActiveEvent(
                "GM_" + System.currentTimeMillis(),
                "⚡ GOD MODE: " + title,
                "GOD_MODE", lat, lng, currentYear, currentMonth, currentDay, Math.max(15.0, durationDays * 0.5), mag, durationDays
            );
            engine.getEventSystem().recordSpatialEvent(ae);
        }

        logger.info("God Mode intervention recorded at Year {}: {} - {}", currentYear, title, details);
    }

    public void refreshTimelineView() {
        if (timelineListView == null) return;
        int selected = timelineListView.getSelectionModel().getSelectedIndex();
        timelineListView.getItems().clear();
        String yearPrefix = I18n.getOrDefault("godmode.timeline.year_prefix", "Year");
        for (ScenarioTimeline.TimelineEntry entry : timeline.getEntries()) {
            String badge = entry.isGodModeIntervention() ? "⚡ [GOD MODE]" : "📜 [HISTORIQUE]";
            timelineListView.getItems().add(String.format("%s %5d | %s %s : %s", yearPrefix, entry.year(), badge, entry.title(), entry.details()));
        }
        if (selected >= 0 && selected < timelineListView.getItems().size()) {
            timelineListView.getSelectionModel().select(selected);
        }
    }

    public void updateCoordinates(double lat, double lng) {
        if (latSpinner != null && latSpinner.getValueFactory() != null) {
            latSpinner.getValueFactory().setValue(Math.max(-90.0, Math.min(90.0, lat)));
        }
        if (lngSpinner != null && lngSpinner.getValueFactory() != null) {
            lngSpinner.getValueFactory().setValue(Math.max(-180.0, Math.min(180.0, lng)));
        }
    }
}

