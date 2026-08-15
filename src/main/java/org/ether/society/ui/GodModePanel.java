package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.ScenarioTimeline;
import org.ether.society.procedural.NuclearWarfareClimateEngine;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * God Mode & Interactive Timeline Controller.
 * Allows pausing live simulation, injecting real physical events (volcanoes, EMP, heatwaves),
 * and viewing the chronological scenario timeline audit trail.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class GodModePanel extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(GodModePanel.class);

    private final H3SimulationEngine engine;
    private final ScenarioTimeline timeline;

    private final Label headerLabel;
    private final ListView<String> timelineListView;
    private final Label timelineHeaderLabel;

    // Configurable Form Controls
    private final ComboBox<String> eventTypeCombo;
    private final Label eventDescriptionLabel;
    private final TextField eventNameField;
    private final Label dateRangeLabel;
    private final Spinner<Integer> targetYearSpinner;
    private final Slider targetYearSlider;
    private final Spinner<Double> latSpinner;
    private final Spinner<Double> lngSpinner;
    private final Spinner<Double> magnitudeSpinner;

    // Injector section
    private final Label injectorTitleLabel;
    private final Label lblEventType;
    private final Label lblEventTitle;
    private final Label lblTargetYear;
    private final Label lblLat;
    private final Label lblLng;
    private final Label lblMag;
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

        setPadding(new Insets(12));
        setSpacing(10);
        getStyleClass().add("glass-panel");

        // Title Header
        headerLabel = new Label();
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #38bdf8;");

        // Form fields initialization
        // Form fields initialization
        eventTypeCombo = new ComboBox<>();
        eventTypeCombo.getItems().addAll(
            "VOLCANO",
            "HEATWAVE",
            "SOLAR_EMP",
            "PANDEMIC",
            "METEOR",
            "NUCLEAR_WINTER",
            "FAMINE",
            "TSUNAMI",
            "EARTHQUAKE",
            "ICE_AGE",
            "CYBER_ATTACK",
            "ECONOMIC_CRASH",
            "BIODIVERSITY_COLLAPSE",
            "GEOENGINEERING",
            "RENAISSANCE_BOOM",
            "TECH_SINGULARITY",
            "ALIEN_CONTACT"
        );
        eventTypeCombo.setValue("VOLCANO");
        eventTypeCombo.setMaxWidth(Double.MAX_VALUE);

        eventDescriptionLabel = new Label();
        eventDescriptionLabel.setWrapText(true);
        eventDescriptionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1; -fx-padding: 4 6; -fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 4; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 4;");

        eventNameField = new TextField("Éruption Stratosphérique SO₂");

        int currentYr = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        int minYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getStartDateYear() : -100000;
        int maxYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getEndDateYear() : 2100;
        if (minYr >= maxYr) { minYr = -100000; maxYr = 2100; }

        dateRangeLabel = new Label(String.format("📅 Plage Autorisée : [An %,d ➔ An %,d]", minYr, maxYr));
        dateRangeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        targetYearSpinner = new Spinner<>(minYr, maxYr, Math.max(minYr, Math.min(maxYr, currentYr)), 1);
        targetYearSpinner.setEditable(true);
        targetYearSpinner.setMaxWidth(Double.MAX_VALUE);

        targetYearSlider = new Slider(minYr, maxYr, Math.max(minYr, Math.min(maxYr, currentYr)));
        targetYearSlider.setBlockIncrement(1);
        targetYearSlider.setMajorTickUnit(Math.max(1, (maxYr - minYr) / 5.0));
        targetYearSlider.setMinorTickCount(4);
        targetYearSlider.setShowTickMarks(false);
        targetYearSlider.setMaxWidth(Double.MAX_VALUE);

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
        latSpinner.setMaxWidth(Double.MAX_VALUE);

        lngSpinner = new Spinner<>(-180.0, 180.0, 0.0, 1.0);
        lngSpinner.setEditable(true);
        lngSpinner.setMaxWidth(Double.MAX_VALUE);

        magnitudeSpinner = new Spinner<>(0.1, 10.0, 1.5, 0.5);
        magnitudeSpinner.setEditable(true);
        magnitudeSpinner.setMaxWidth(Double.MAX_VALUE);

        // Update default event name and description when type changes
        eventTypeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            switch (newV) {
                case "VOLCANO" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.volcano", "Éruption Stratosphérique SO₂"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.volcano.desc", "🌋 Éruption volcanique majeure injectant du dioxyde de soufre dans la stratosphère, provoquant un refroidissement global temporaire."));
                }
                case "HEATWAVE" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.heatwave", "Canicule Globale & Forçage Radiatif"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.heatwave.desc", "☀️ Canicule extrême augmentant la température régionale, provoquant du stress hydrique et des mortalités par bulbe humide."));
                }
                case "SOLAR_EMP" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.solar_emp", "Tempête Solaire Carrington (EMP)"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.solar_emp.desc", "⚡ Éjection de masse coronale détruisant les réseaux électriques et réduisant la mémoire collective & le débit Shannon."));
                }
                case "PANDEMIC" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.pandemic", "Épidémie Zoonotique Bio-Moléculaire"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.pandemic.desc", "🦠 Maladie infectieuse à forte contagiosité réduisant la population et perturbant l'espérance de vie."));
                }
                case "METEOR" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.meteor", "Impact d'Astéroïde Majeur"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.meteor.desc", "☄️ Choc d'astéroïde projetant des poussières opaques, détruisant la biomasse et refroidissant le climat."));
                }
                case "NUCLEAR_WINTER" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.nuclear_winter", "Hiver Nucléaire / Glaciation"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.nuclear_winter.desc", "☢️ Incendies massifs et suie stratosphérique occultant le rayonnement solaire pendant plusieurs décennies."));
                }
                case "FAMINE" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.famine", "Sécheresse & Famine Répandue"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.famine.desc", "🌾 Effondrement des rendements agricoles et dégradation NPK des sols déclenchant une crise alimentaire."));
                }
                case "TSUNAMI" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.tsunami", "Mégatsunami & Submersion Côtière"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.tsunami.desc", "🌊 Vague géante dévastant les zones côtières et les infrastructures portuaires."));
                }
                case "EARTHQUAKE" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.earthquake", "Séisme Majeur de Tectonique"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.earthquake.desc", "🏚️ Tremblement de terre détruisant le capital bâti et perturbant l'économie locale."));
                }
                case "ICE_AGE" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.ice_age", "Glaciation Abrupte Younger Dryas"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.ice_age.desc", "❄️ Refroidissement brutal du climat planétaire réduisant les zones cultivables."));
                }
                case "CYBER_ATTACK" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.cyber_attack", "Panne Numérique & Effondrement Réseau"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.cyber_attack.desc", "💻 Attaque informatique globale paralysant la division du travail et les chaînes logistiques."));
                }
                case "ECONOMIC_CRASH" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.economic_crash", "Krach Boursier & Panique Monétaire"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.economic_crash.desc", "📉 Crise financière systémique augmentant l'inégalité de Gini et le stress fiscal."));
                }
                case "BIODIVERSITY_COLLAPSE" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.biodiversity_collapse", "Effondrement de la Chaîne Trophique"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.biodiversity_collapse.desc", "🦋 Disparition des pollinisateurs et baisse drastique de la biodiversité sauvage."));
                }
                case "GEOENGINEERING" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.geoengineering", "Injection Stratosphérique d'Aérosols"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.geoengineering.desc", "🌍 Intervention humaine directe d'injection d'aérosols pour contrer le réchauffement."));
                }
                case "RENAISSANCE_BOOM" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.renaissance_boom", "Révolution Industrielle & Technologique"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.renaissance_boom.desc", "🚀 Éruption d'innovations scientifiques accélérant le niveau technologique et la productivité."));
                }
                case "TECH_SINGULARITY" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.tech_singularity", "Émergence d'une Superintelligence Artificielle"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.tech_singularity.desc", "🤖 Singularité technologique démultipliant le savoir et la bande passante Shannon."));
                }
                case "ALIEN_CONTACT" -> {
                    eventNameField.setText(I18n.getOrDefault("godmode.event.alien_contact", "Signal Extraterrestre Exogène"));
                    eventDescriptionLabel.setText(I18n.getOrDefault("godmode.event.alien_contact.desc", "🛸 Découverte d'un signal intelligent exogène unifiant l'humanité et stimulant la recherche."));
                }
            }
        });
        // Trigger initial text
        eventTypeCombo.setValue("VOLCANO");

        // Injector components
        injectorTitleLabel = new Label();
        injectorTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        lblEventType = createLabel();
        lblEventTitle = createLabel();
        lblTargetYear = createLabel();
        lblLat = createLabel();
        lblLng = createLabel();
        lblMag = createLabel();

        scheduleBtn = new Button();
        scheduleBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6;");
        scheduleBtn.setMaxWidth(Double.MAX_VALUE);
        scheduleBtn.setOnAction(e -> scheduleEvent(false));

        triggerNowBtn = new Button();
        triggerNowBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6;");
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
            recordIntervention("POP_INJECT", I18n.getOrDefault("godmode.spawner.pop_title", "Injection Démographique"),
                    I18n.getOrDefault("godmode.spawner.pop_details", "Ajout de +100,000 habitants aux coordonnées (Lat: ") + latSpinner.getValue() + ", Lng: " + lngSpinner.getValue() + ")");
        });

        injectFoodBtn = new Button();
        injectFoodBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        injectFoodBtn.setMaxWidth(Double.MAX_VALUE);
        injectFoodBtn.setOnAction(e -> {
            recordIntervention("FOOD_INJECT", I18n.getOrDefault("godmode.spawner.food_title", "Injection Alimentaire"),
                    I18n.getOrDefault("godmode.spawner.food_details", "Remplissage des stocks céréaliers mondiaux (+12 mois)"));
        });

        massExtinctionBtn = new Button();
        massExtinctionBtn.setStyle("-fx-background-color: #991b1b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 10; -fx-background-radius: 4;");
        massExtinctionBtn.setMaxWidth(Double.MAX_VALUE);
        massExtinctionBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("godmode.dialog.extinction_title", "Confirmation d'Extinction Massive"));
            alert.setHeaderText(I18n.getOrDefault("godmode.dialog.extinction_header", "⚠️ Action Destructive en Mode Dieu"));
            alert.setContentText(I18n.getOrDefault("godmode.dialog.extinction_desc", "Êtes-vous sûr de vouloir éliminer 50% de la population mondiale ? Cette intervention sera enregistrée de façon irréversible dans l'audit trail."));
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    recordIntervention("MASS_EXTINCTION", I18n.getOrDefault("godmode.spawner.extinction_title", "Extinction Cataclysmique"),
                            I18n.getOrDefault("godmode.spawner.extinction_details", "Réduction immédiate de 50% de la biomasse humaine mondiale"));
                }
            });
        });

        VBox spawnerBox = createSpawnerSection();
        spawnerBox.getStyleClass().add("card-section");

        // Terraform components
        terraformTitleLabel = new Label();
        terraformTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        brushModeCombo = new ComboBox<>();
        brushModeCombo.setMaxWidth(Double.MAX_VALUE);

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
                        I18n.getOrDefault("godmode.brush.details", "Action de pinceau appliquée sur la maille Lat ") + String.format("%.2f", nearest.getLatitude()) + "°, Lng " + String.format("%.2f", nearest.getLongitude()) + "°");
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
            recordIntervention("RESET_CLIMATE", I18n.getOrDefault("godmode.reset.title", "Dissipation des Aérosols"),
                    I18n.getOrDefault("godmode.reset.details", "Retour à l'équilibre climatique et transparence stratosphérique standard (τ = 0.0)"));
        });

        VBox resetBox = createResetSection();
        resetBox.getStyleClass().add("card-section");

        // Section 5: Scenario Timeline Audit Log
        timelineHeaderLabel = new Label();
        timelineHeaderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #a78bfa; -fx-font-size: 12px;");

        timelineListView = new ListView<>();
        timelineListView.setPrefHeight(160);

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
                        setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");
                    } else if (eventYear == currentYear) {
                        setStyle("-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-style: italic; -fx-font-size: 11px;");
                    }
                }
            }
        });
        refreshTimelineView();

        VBox timelineBox = new VBox(6, timelineHeaderLabel, timelineListView);
        timelineBox.getStyleClass().add("card-section");

        getChildren().addAll(headerLabel, injectorBox, spawnerBox, terraformBox, resetBox, timelineBox);

        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    public void updateTexts() {
        headerLabel.setText(I18n.getOrDefault("godmode.title", "⚡ 5. MODE DIEU & CHRONOLOGIE"));

        eventTypeCombo.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.event_type", "Type de perturbation physique ou climatique à injecter dans l'écosystème.")));
        eventNameField.setPromptText(I18n.getOrDefault("godmode.prompt.event_title", "Titre ou Nom de l'événement..."));
        eventNameField.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.event_title", "Titre personnalisé qui apparaîtra dans le registre chronologique et l'audit trail.")));
        targetYearSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.target_year", "Année cible exacte de déclenchement de l'événement dans le calendrier de la simulation.")));
        latSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.lat", "Latitude de l'épicentre du phénomène physique (-90° Sud à +90° Nord).")));
        lngSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.lng", "Longitude de l'épicentre du phénomène physique (-180° Ouest à +180° Est).")));
        magnitudeSpinner.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.magnitude", "Intensité / Magnitude du choc (détermine la profondeur et l'impact spatial de la perturbation).")));

        injectorTitleLabel.setText(I18n.getOrDefault("godmode.injector.title", "🛠️ ÉDITION & PROGRAMMATION D'ÉVÉNEMENTS CLIMATIQUES :"));
        lblEventType.setText(I18n.getOrDefault("godmode.label.event_type", "Type d'Événement :"));
        lblEventTitle.setText(I18n.getOrDefault("godmode.label.event_title", "Nom / Titre :"));
        lblTargetYear.setText(I18n.getOrDefault("godmode.label.target_year", "Année Cible (Date) :"));
        lblLat.setText(I18n.getOrDefault("godmode.label.lat", "Latitude (-90 à +90°) :"));
        lblLng.setText(I18n.getOrDefault("godmode.label.lng", "Longitude (-180 à +180°) :"));
        lblMag.setText(I18n.getOrDefault("godmode.label.magnitude", "Intensité / Magnitude :"));

        scheduleBtn.setText(I18n.getOrDefault("godmode.btn.schedule", "📅 Programmer dans la Chronologie"));
        scheduleBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.schedule", "Inscrit l'événement dans le calendrier du scénario pour un déclenchement automatique à l'année cible spécifiée.")));
        triggerNowBtn.setText(I18n.getOrDefault("godmode.btn.trigger_now", "⚡ Déclencher Immédiatement"));
        triggerNowBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.trigger_now", "Applique instantanément les perturbations climatiques et physiques sur le monde à l'année courante en direct.")));

        spawnerTitleLabel.setText(I18n.getOrDefault("godmode.spawner.header", "🌱 INJECTION DIRECTE DE POPULATION & RESSOURCES :"));
        injectPopBtn.setText(I18n.getOrDefault("godmode.btn.inject_pop", "👥 Injecter 100 000 Habitants (Épicentre)"));
        injectPopBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.inject_pop", "Injecte une cohorte de 100 000 habitants à la position géographique spécifiée par les spinners Lat/Lng.")));
        injectFoodBtn.setText(I18n.getOrDefault("godmode.btn.inject_food", "🌾 Injecter Stock Alimentaire (Silos)"));
        injectFoodBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.inject_food", "Remplit les stocks alimentaires à 100% pour éviter les famines immédiates.")));
        massExtinctionBtn.setText(I18n.getOrDefault("godmode.btn.mass_extinction", "💀 Déclencher Extinction Massive (Extinction 50%)"));
        massExtinctionBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.mass_extinction", "Réduit instantanément de 50% la population mondiale active (Choc de Cataclysme).")));

        terraformTitleLabel.setText(I18n.getOrDefault("godmode.terraform.header", "🖌️ PINCEAU SPATIAL & DYNAMIQUES LOCALES :"));
        int selIdx = brushModeCombo.getSelectionModel().getSelectedIndex();
        brushModeCombo.getItems().clear();
        brushModeCombo.getItems().addAll(
            I18n.getOrDefault("godmode.brush.pop", "👥 Boost Population (+50 000 hab)"),
            I18n.getOrDefault("godmode.brush.agri", "🌾 Injection Agricole & Silos (+500 t)"),
            I18n.getOrDefault("godmode.brush.water", "🚰 Recharge Nappe Aquifère (+2 000 m³)"),
            I18n.getOrDefault("godmode.brush.heat", "🔥 Vague de Chaleur Locale (+10.0°C)"),
            I18n.getOrDefault("godmode.brush.cold", "❄️ Refroidissement Local (-10.0°C)"),
            I18n.getOrDefault("godmode.brush.clean", "🧼 Dépollution Écologique Total (0.0)")
        );
        brushModeCombo.getSelectionModel().select(selIdx >= 0 ? selIdx : 0);
        brushModeCombo.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.brush_mode", "Sélectionnez l'effet local à appliquer (démographie, agriculture, eau, température ou dépollution).")));

        applyBrushBtn.setText(I18n.getOrDefault("godmode.btn.apply_brush", "🖌️ Appliquer aux Coordonnées Épicentre"));
        applyBrushBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.apply_brush", "Applique l'action sélectionnée directement sur la maille H3 ciblée par les coordonnées épicentre (Latitude / Longitude) définies ci-dessus.")));

        resetTitleLabel.setText(I18n.getOrDefault("godmode.reset.header", "🛑 NORMALISATION & RÉINITIALISATION PHYSIQUE :"));
        resetDisastersBtn.setText(I18n.getOrDefault("godmode.btn.reset_disasters", "🛑 Stopper Tous les Désastres & Dissiper l'Ombre Stratosphérique"));
        resetDisastersBtn.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.reset_disasters", "Réinitialise la profondeur optique de la suie stratosphérique (τ = 0.0) et annule les perturbations caniculaires/volcaniques actives.")));

        int minYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getStartDateYear() : -100000;
        int maxYr = engine != null && engine.getCurrentScenario() != null ? (int) engine.getCurrentScenario().getEndDateYear() : 2100;
        if (minYr >= maxYr) { minYr = -100000; maxYr = 2100; }
        if (dateRangeLabel != null) {
            dateRangeLabel.setText(String.format(I18n.getOrDefault("godmode.label.date_range", "📅 Plage Autorisée : [An %,d ➔ An %,d]"), minYr, maxYr));
        }

        if (eventTypeCombo != null && eventTypeCombo.getValue() != null) {
            String currentType = eventTypeCombo.getValue();
            eventTypeCombo.setValue(null);
            eventTypeCombo.setValue(currentType);
        }

        timelineHeaderLabel.setText(I18n.getOrDefault("godmode.timeline.title", "📜 CHRONOLOGIE DU SCÉNARIO & REGISTRE D'AUDIT EN DIRECT :"));
        timelineListView.setTooltip(new Tooltip(I18n.getOrDefault("godmode.tooltip.timeline", "Registre d'audit temporel : Liste chronologique de tous les forçages et événements du scénario.")));

        refreshTimelineView();
    }

    private VBox createInjectorSection() {
        VBox box = new VBox(8);
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);

        grid.addRow(0, lblEventType, eventTypeCombo);
        grid.add(eventDescriptionLabel, 1, 1);
        grid.addRow(2, lblEventTitle, eventNameField);
        
        VBox yearBox = new VBox(4, targetYearSpinner, targetYearSlider, dateRangeLabel);
        grid.addRow(3, lblTargetYear, yearBox);

        grid.addRow(4, lblLat, latSpinner);
        grid.addRow(5, lblLng, lngSpinner);
        grid.addRow(6, lblMag, magnitudeSpinner);

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
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        return l;
    }

    private void scheduleEvent(boolean immediate) {
        String type = eventTypeCombo.getValue();
        String name = eventNameField.getText();
        if (name == null || name.isBlank()) name = type;

        int currentYear = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentYear() : 2026;
        int currentMonth = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentMonth() : 0;
        int currentDay = engine != null && engine.getTimeManager() != null ? engine.getTimeManager().getCurrentDay() : 1;
        int targetYear = immediate ? currentYear : targetYearSpinner.getValue();

        double lat = latSpinner.getValue();
        double lng = lngSpinner.getValue();
        double mag = magnitudeSpinner.getValue();

        String details = String.format(java.util.Locale.US, "Lat: %.2f°, Lng: %.2f°, Mag: %.1f", lat, lng, mag);

        timeline.addEntry(targetYear, type, name, details, true);
        refreshTimelineView();

        if (engine != null && engine.getEventSystem() != null) {
            org.ether.society.events.ActiveEvent ae = new org.ether.society.events.ActiveEvent(
                "GM_" + System.currentTimeMillis(),
                "⚡ GOD MODE: " + name,
                type, lat, lng, targetYear, currentMonth, currentDay, 30.0
            );
            engine.getEventSystem().recordSpatialEvent(ae);
        }

        if (immediate || targetYear <= currentYear) {
            executePhysicalForcing(type, mag, lat, lng);
            logger.info("God Mode intervention EXECUTED immediately (Year {}): {} - {}", currentYear, name, details);
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
        timeline.addEntry(currentYear, type, title, details, true);
        refreshTimelineView();

        if (engine != null && engine.getEventSystem() != null) {
            double lat = latSpinner != null ? latSpinner.getValue() : 0.0;
            double lng = lngSpinner != null ? lngSpinner.getValue() : 0.0;
            org.ether.society.events.ActiveEvent ae = new org.ether.society.events.ActiveEvent(
                "GM_" + System.currentTimeMillis(),
                "⚡ GOD MODE: " + title,
                "GOD_MODE", lat, lng, (int) currentYear, currentMonth, currentDay, 30.0
            );
            engine.getEventSystem().recordSpatialEvent(ae);
        }

        logger.info("God Mode intervention recorded at Year {}: {} - {}", currentYear, title, details);
    }

    public void refreshTimelineView() {
        if (timelineListView == null) return;
        timelineListView.getItems().clear();
        String yearPrefix = I18n.getOrDefault("godmode.timeline.year_prefix", "Année");
        for (ScenarioTimeline.TimelineEntry entry : timeline.getEntries()) {
            String badge = entry.isGodModeIntervention() ? "⚡ [GOD MODE]" : "📜 [HISTORIQUE]";
            timelineListView.getItems().add(String.format("%s %5d | %s %s : %s", yearPrefix, entry.year(), badge, entry.title(), entry.details()));
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

