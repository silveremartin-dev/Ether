/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.ether.society.data.TemporalMapTensorManager;
import org.ether.society.i18n.I18n;
import org.ether.society.model.AutoEpochScenarioGenerator;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Interactive Time-Travel & Historical Epoch Scenario Generation Dialog.
 * Allows users to choose any historical date (-100,000 BP to 2060 AD) or extraterrestrial context,
 * automatically resolves and aligns geophysical elevation & climate rasters (Tab 1),
 * hydrology & mineral reserves (Tab 2), and demographic baselines with Type B optional engines (Tab 3).
 *
 * Guaranteed modal positioning, dedicated scrollable layout with persistent bottom buttons,
 * dynamic explanations for all date resolution strategies, complete tooltips, and 5-language localization.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class AutoEpochScenarioDialog extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(AutoEpochScenarioDialog.class);

    private final ComboBox<String> planetSelector = new ComboBox<>();
    private final ComboBox<TemporalMapTensorManager.DataFallbackStrategy> fallbackSelector = new ComboBox<>();
    private final Spinner<Integer> yearSpinner = new Spinner<>(-100000, 2060, -8000, 100);
    private final Slider yearSlider = new Slider(-100000, 2060, -8000);
    private final FlowPane chipsPane = new FlowPane(6, 6);

    private final Label milestoneTitleLabel = new Label();
    private final Label milestoneDescLabel = new Label();
    private final Label strategyExplanationLabel = new Label();
    private final Label demoPreviewLabel = new Label();
    private final Label rasterPreviewLabel = new Label();
    private final Label enginesPreviewLabel = new Label();
    private final CheckBox cbRegenerateSpecificMaps = new CheckBox();

    private final ProgressBar progressBar = new ProgressBar();
    private final Label progressLabel = new Label();
    private final Button btnApply = new Button();
    private final Button btnCancel = new Button();

    private final Consumer<Scenario> onScenarioGeneratedCallback;
    /* Internal state variable for is updating (boolean). */
    private boolean isUpdating = false;

    /*
     * Auto epoch scenario dialog.
     * Enforces physical invariants and updates associated state variables within {@code AutoEpochScenarioDialog}.
     *
     * @param initialPlanetKey the initial planet key parameter (String)
     * @param initialYear the initial year parameter (long)
     * @param callback the callback parameter (Consumer&lt;Scenario&gt;)
     */
    public AutoEpochScenarioDialog(String initialPlanetKey, long initialYear, Consumer<Scenario> callback) {
        this.onScenarioGeneratedCallback = callback;
        initModality(Modality.APPLICATION_MODAL);
        setResizable(true);
        setMinWidth(740);
        setMinHeight(580);
        setWidth(850);
        setHeight(750);
        setTitle(I18n.getOrDefault("scenario.autodialog.title", "✨ Assistant de Scénario Temporel & Époques Historiques"));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("glass-panel");

        // 1. Header (Fixed top)
        VBox headerBox = new VBox(6);
        headerBox.setPadding(new Insets(16, 20, 10, 20));
        headerBox.setStyle("-fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-width: 0 0 1 0;");

        Label headerTitle = new Label(I18n.getOrDefault("scenario.autodialog.header", "🏛️ GÉNÉRATEUR AUTOMATIQUE DE SCÉNARIO PAR DATE"));
        headerTitle.getStyleClass().add("label-section-header");
        headerTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label headerSub = new Label(I18n.getOrDefault("scenario.autodialog.subtitle", 
                "Sélectionnez une planète et une date : Ether configure automatiquement la géophysique (Onglet 1), les ressources (Onglet 2) et les moteurs physiques & anthropiques adaptés (Onglet 3)."));
        headerSub.setWrapText(true);
        headerSub.getStyleClass().add("card-description-muted");
        headerSub.setStyle("-fx-font-size: 11px;");

        headerBox.getChildren().addAll(headerTitle, headerSub);
        root.setTop(headerBox);

        // 2. Scrollable Center Content
        VBox scrollContent = new VBox(12);
        scrollContent.setPadding(new Insets(14, 20, 14, 20));

        // Section A: Planet & Date Target
        VBox planetAndDateBox = new VBox(10);
        planetAndDateBox.getStyleClass().add("card-section");

        HBox planetRow = new HBox(10);
        planetRow.setAlignment(Pos.CENTER_LEFT);
        Label lblPlanet = new Label(I18n.getOrDefault("scenario.autodialog.planet_label", "🪐 Planète cible :"));
        lblPlanet.getStyleClass().add("control-label");
        lblPlanet.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        planetSelector.getItems().addAll("earth", "mars", "moon", "venus", "mercury");
        String pNorm = TemporalMapTensorManager.normalizePlanet(initialPlanetKey);
        planetSelector.setValue(planetSelector.getItems().contains(pNorm) ? pNorm : "earth");
        planetSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            /*
             * Update item.
             * Enforces physical invariants and updates associated state variables within {@code AutoEpochScenarioDialog}.
             *
             * @param item the item parameter (String)
             * @param empty the empty parameter (boolean)
             */
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(getLocalizedPlanetName(item));
                }
            }
        });
        planetSelector.setButtonCell(planetSelector.getCellFactory().call(null));
        planetSelector.getStyleClass().add("combo-box");
        planetSelector.setTooltip(new Tooltip(I18n.getOrDefault("scenario.autodialog.tooltip.planet", "Choisissez la planète dont les calques cartographiques et époques doivent être configurés.")));

        planetRow.getChildren().addAll(lblPlanet, planetSelector);

        HBox spinnerRow = new HBox(12);
        spinnerRow.setAlignment(Pos.CENTER_LEFT);
        Label lblYear = new Label(I18n.getOrDefault("scenario.autodialog.year_label", "📅 Date de départ (Année BP / AD) :"));
        lblYear.getStyleClass().add("control-label");
        lblYear.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        yearSpinner.setEditable(true);
        yearSpinner.getValueFactory().setValue((int) initialYear);
        yearSpinner.setPrefWidth(140);
        yearSpinner.getStyleClass().add("spinner");
        yearSpinner.setTooltip(new Tooltip(I18n.getOrDefault("scenario.autodialog.tooltip.year_spinner", "Entrez l'année de départ numérique (valeurs négatives pour BP / avant J.-C., positives pour AD / après J.-C.).")));

        milestoneTitleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #38bdf8;");
        milestoneTitleLabel.setTooltip(new Tooltip(I18n.getOrDefault("scenario.autodialog.tooltip.preview_milestone", "Ère chronologique et contexte géophysique/historique clé pour l'année ciblée.")));
        spinnerRow.getChildren().addAll(lblYear, yearSpinner, milestoneTitleLabel);

        yearSlider.setShowTickMarks(true);
        yearSlider.setShowTickLabels(false);
        yearSlider.setMajorTickUnit(20000);
        yearSlider.setBlockIncrement(100);
        yearSlider.setValue(initialYear);
        yearSlider.setMaxWidth(Double.MAX_VALUE);
        yearSlider.setTooltip(new Tooltip(I18n.getOrDefault("scenario.autodialog.tooltip.slider", "Glissez pour parcourir les époques de -100 000 BP jusqu'en 2060 AD.")));

        // Bidirectional sync between slider & spinner
        yearSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isUpdating && newV != null) {
                isUpdating = true;
                yearSpinner.getValueFactory().setValue(newV.intValue());
                updateLivePreview();
                isUpdating = false;
            }
        });

        yearSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isUpdating && newV != null) {
                isUpdating = true;
                yearSlider.setValue(newV.doubleValue());
                updateLivePreview();
                isUpdating = false;
            }
        });

        Label quickLabel = new Label(I18n.getOrDefault("scenario.autodialog.quick_milestones", "⚡ Jalons & Époques Clés :"));
        quickLabel.getStyleClass().add("card-description-muted");
        quickLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        rebuildMilestoneChips();

        planetAndDateBox.getChildren().addAll(planetRow, spinnerRow, yearSlider, quickLabel, chipsPane);

        // Section B: Strategy & Synthesis Configuration (Grouped Together)
        VBox strategySectionBox = new VBox(8);
        strategySectionBox.getStyleClass().add("card-section");

        Label strategySectionHeader = new Label(I18n.getOrDefault("scenario.autodialog.strategy_section_header", "⚙️ STRATÉGIE DE RÉSOLUTION TEMPORELLE & SYNTHÈSE CARTOGRAPHIQUE"));
        strategySectionHeader.getStyleClass().add("label-section-header");
        strategySectionHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        HBox fallbackRow = new HBox(10);
        fallbackRow.setAlignment(Pos.CENTER_LEFT);
        Label lblFallback = new Label(I18n.getOrDefault("scenario.autodialog.fallback_label", "Stratégie de résolution des dates intermédiaires :"));
        lblFallback.getStyleClass().add("control-label");
        lblFallback.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        fallbackSelector.getItems().addAll(TemporalMapTensorManager.DataFallbackStrategy.values());
        fallbackSelector.setValue(TemporalMapTensorManager.DataFallbackStrategy.CONTINUOUS_INTERPOLATION);
        fallbackSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            /*
             * Update item.
             * Enforces physical invariants and updates associated state variables within {@code AutoEpochScenarioDialog}.
             *
             * @param item the item parameter (TemporalMapTensorManager.DataFallbackStrategy)
             * @param empty the empty parameter (boolean)
             */
            protected void updateItem(TemporalMapTensorManager.DataFallbackStrategy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.getLocalizedName());
                }
            }
        });
        fallbackSelector.setButtonCell(fallbackSelector.getCellFactory().call(null));
        fallbackSelector.getStyleClass().add("combo-box");
        fallbackSelector.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fallbackSelector, Priority.ALWAYS);
        fallbackSelector.setTooltip(new Tooltip(I18n.getOrDefault("scenario.autodialog.tooltip.fallback", 
                "Choisissez comment les calques cartographiques sont résolus en l'absence de données empiriques directes pour l'année exacte.")));

        fallbackRow.getChildren().addAll(lblFallback, fallbackSelector);

        // Dynamic explanation box for the chosen fallback strategy
        VBox strategyExplainCard = new VBox(4);
        strategyExplainCard.setStyle("-fx-background-color: rgba(56, 189, 248, 0.07); -fx-background-radius: 6; -fx-padding: 8 10; -fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-radius: 6;");
        strategyExplanationLabel.setWrapText(true);
        strategyExplanationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0;");
        strategyExplainCard.getChildren().add(strategyExplanationLabel);

        // Synthesis Checkbox placed directly at the same strategy level
        cbRegenerateSpecificMaps.setText(I18n.getOrDefault("scenario.autodialog.cb_regenerate_maps", 
                "🔄 Synthétiser les cartes raster dédiées pour cette date précise (via sources brutes exactes)"));
        cbRegenerateSpecificMaps.setSelected(true);
        cbRegenerateSpecificMaps.getStyleClass().add("check-box");
        cbRegenerateSpecificMaps.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");
        cbRegenerateSpecificMaps.setTooltip(new Tooltip(I18n.getOrDefault("scenario.autodialog.tooltip.regenerate_maps", 
                "Si coché, génère un jeu de données raster exact pour l'année ciblée en s'appuyant directement sur les sources brutes (NOAA, WorldClim, HYDE 3.4), évitant tout besoin d'interpolation ou de repli approximatif.")));

        Label regenDesc = new Label(I18n.getOrDefault("scenario.autodialog.regen_desc", 
                "💡 Cette option synthétise un jeu de données exact et dédié pour la date choisie, garantissant l'alignement sans interpolation des 25 cartes."));
        regenDesc.setWrapText(true);
        regenDesc.getStyleClass().add("card-description-muted");
        regenDesc.setStyle("-fx-font-size: 10px; -fx-padding: 0 0 0 20;");

        strategySectionBox.getChildren().addAll(strategySectionHeader, fallbackRow, strategyExplainCard, cbRegenerateSpecificMaps, regenDesc);

        // Section C: Cascading Actions Breakdown (No surprises for user)
        VBox actionsCard = new VBox(8);
        actionsCard.getStyleClass().add("subcard-section");

        Label previewHeader = new Label(I18n.getOrDefault("scenario.autodialog.actions_header", "📋 ACTIONS EXÉCUTÉES EN CASCADE (ZÉRO SURPRISE)"));
        previewHeader.getStyleClass().add("label-section-header");
        previewHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        milestoneDescLabel.setWrapText(true);
        milestoneDescLabel.getStyleClass().add("card-description-muted");
        milestoneDescLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");

        rasterPreviewLabel.setWrapText(true);
        rasterPreviewLabel.getStyleClass().add("control-label");
        rasterPreviewLabel.setStyle("-fx-font-size: 11px;");

        demoPreviewLabel.setWrapText(true);
        demoPreviewLabel.getStyleClass().add("control-label");
        demoPreviewLabel.setStyle("-fx-font-size: 11px;");

        enginesPreviewLabel.setWrapText(true);
        enginesPreviewLabel.getStyleClass().add("control-label");
        enginesPreviewLabel.setStyle("-fx-font-size: 11px;");

        VBox warningCard = new VBox(4);
        warningCard.getStyleClass().add("card-section");
        warningCard.setStyle("-fx-padding: 8 10; -fx-border-color: rgba(245, 158, 11, 0.4); -fx-border-radius: 6;");

        Label warningHeader = new Label(I18n.getOrDefault("scenario.autodialog.warning_header", "⚠️ NOTE ÉPISTÉMIQUE & FIDÉLITÉ PHYSIQUE :"));
        warningHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: #d97706;");

        Label warningBody = new Label(I18n.getOrDefault("scenario.autodialog.warning_body", 
                "La précision des calques dépend directement des données empiriques disponibles (NOAA ETOPO, Paleoclim, HYDE 3.4, EPICA). Pour les planètes extraterrestres (Mars, Lune, etc.), le moteur génère une configuration procédurale cohérente avec les calques d'élévation réels (MOLA, LOLA)."));
        warningBody.setWrapText(true);
        warningBody.getStyleClass().add("card-description-muted");
        warningBody.setStyle("-fx-font-size: 10px;");
        warningCard.getChildren().addAll(warningHeader, warningBody);

        actionsCard.getChildren().addAll(previewHeader, milestoneDescLabel, rasterPreviewLabel, demoPreviewLabel, enginesPreviewLabel, warningCard);

        scrollContent.getChildren().addAll(planetAndDateBox, strategySectionBox, actionsCard);

        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge-scroll");
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        root.setCenter(scrollPane);

        // 3. Fixed Bottom Action Bar
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(12, 20, 14, 20));
        buttonBar.setStyle("-fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-width: 1 0 0 0; -fx-background-color: rgba(15, 23, 42, 0.6);");

        progressBar.setVisible(false);
        progressBar.setPrefWidth(160);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        progressLabel.setVisible(false);
        progressLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        HBox progressBox = new HBox(8, progressBar, progressLabel);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progressBox, Priority.ALWAYS);

        btnCancel.setText(I18n.getOrDefault("scenario.autodialog.btn_cancel", "Annuler"));
        btnCancel.getStyleClass().add("button-secondary");
        btnCancel.setTooltip(new Tooltip(I18n.getOrDefault("scenario.autodialog.tooltip.cancel", "Fermer l'assistant sans modifier le scénario actuel.")));
        btnCancel.setOnAction(e -> close());

        btnApply.setText(I18n.getOrDefault("scenario.autodialog.btn_apply", "⚡ Créer & Appliquer le Scénario Complet"));
        btnApply.getStyleClass().add("button-primary");
        btnApply.setStyle("-fx-font-weight: bold;");
        btnApply.setTooltip(new Tooltip(I18n.getOrDefault("scenario.autodialog.tooltip.apply", "Génère le scénario complet, applique les cartes dans les onglets 1 et 2, calibre la démographie et active les moteurs compatibles.")));
        
        btnApply.setOnAction(e -> launchScenarioGeneration());

        buttonBar.getChildren().addAll(progressBox, btnCancel, btnApply);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root);
        setScene(scene);
        Theme.applyCurrentTheme(scene);
        Theme.themeProperty().addListener((obs, oldV, newV) -> Theme.applyCurrentTheme(scene));
        WindowUtils.applyWindowIcon(this);

        planetSelector.setOnAction(e -> {
            rebuildMilestoneChips();
            updateLivePreview();
        });
        fallbackSelector.setOnAction(e -> updateLivePreview());
        updateLivePreview();
    }

    private void rebuildMilestoneChips() {
        chipsPane.getChildren().clear();
        String planet = planetSelector.getValue() != null ? planetSelector.getValue() : "earth";
        boolean isEarth = "earth".equalsIgnoreCase(TemporalMapTensorManager.normalizePlanet(planet));

        if (isEarth) {
            for (AutoEpochScenarioGenerator.EpochMilestone m : AutoEpochScenarioGenerator.EARTH_KEY_MILESTONES) {
                String localizedName = I18n.getOrDefault(m.nameKey(), m.defaultName());
                String localizedDesc = I18n.getOrDefault(m.descriptionKey(), m.defaultDesc());
                Button chip = new Button(localizedName);
                chip.getStyleClass().add("button-secondary");
                chip.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 12; -fx-cursor: hand;");
                chip.setTooltip(new Tooltip(localizedDesc));
                chip.setOnAction(e -> yearSpinner.getValueFactory().setValue((int) m.year()));
                chipsPane.getChildren().add(chip);
            }
        } else {
            // Representative milestones for extraterrestrial planets
            List<Long> alienDates = List.of(-100000L, -20000L, 0L, 1950L, 2026L, 2060L);
            for (Long yr : alienDates) {
                String label = AutoEpochScenarioGenerator.formatEpochName(yr);
                Button chip = new Button(label);
                chip.getStyleClass().add("button-secondary");
                chip.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 12; -fx-cursor: hand;");
                chip.setTooltip(new Tooltip(String.format(Locale.ROOT, "Initialiser %s pour l'époque %s", getLocalizedPlanetName(planet), label)));
                chip.setOnAction(e -> yearSpinner.getValueFactory().setValue(yr.intValue()));
                chipsPane.getChildren().add(chip);
            }
        }
    }

    private void launchScenarioGeneration() {
        String planet = planetSelector.getValue() != null ? planetSelector.getValue() : "earth";
        int year = yearSpinner.getValue() != null ? yearSpinner.getValue() : -8000;
        boolean forceRegen = cbRegenerateSpecificMaps.isSelected();
        TemporalMapTensorManager.DataFallbackStrategy strat = fallbackSelector.getValue() != null 
                ? fallbackSelector.getValue() 
                : TemporalMapTensorManager.DataFallbackStrategy.CONTINUOUS_INTERPOLATION;

        btnApply.setDisable(true);
        btnCancel.setDisable(true);
        progressBar.setVisible(true);
        progressLabel.setVisible(true);
        progressLabel.setText(I18n.getOrDefault("scenario.autodialog.status_generating", "Génération des 25 calques en cours..."));

        Task<Scenario> task = new Task<>() {
            @Override
            /*
             * Call.
             * Enforces physical invariants and updates associated state variables within {@code AutoEpochScenarioDialog}.
             *
             * @return the resulting computation or state reference
             */
            protected Scenario call() {
                return AutoEpochScenarioGenerator.generateScenarioForEpoch(planet, year, forceRegen, strat);
            }
        };

        task.setOnSucceeded(evt -> {
            Scenario generatedScenario = task.getValue();
            if (onScenarioGeneratedCallback != null) {
                onScenarioGeneratedCallback.accept(generatedScenario);
            }
            close();
        });

        task.setOnFailed(evt -> {
            logger.error("Failed to generate auto-scenario for {} at year {}", planet, year, task.getException());
            btnApply.setDisable(false);
            btnCancel.setDisable(false);
            progressBar.setVisible(false);
            progressLabel.setText(I18n.getOrDefault("scenario.autodialog.status_error", "Erreur lors de la génération."));
        });

        Thread thread = new Thread(task, "AutoEpochScenario-Generator");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateLivePreview() {
        String planet = planetSelector.getValue() != null ? planetSelector.getValue() : "earth";
        int year = yearSpinner.getValue() != null ? yearSpinner.getValue() : -8000;
        boolean isEarth = "earth".equalsIgnoreCase(TemporalMapTensorManager.normalizePlanet(planet));

        String formattedYear = AutoEpochScenarioGenerator.formatEpochName(year);
        milestoneTitleLabel.setText("📍 " + formattedYear);

        // Update strategy explanation
        TemporalMapTensorManager.DataFallbackStrategy strat = fallbackSelector.getValue() != null 
                ? fallbackSelector.getValue() 
                : TemporalMapTensorManager.DataFallbackStrategy.CONTINUOUS_INTERPOLATION;

        strategyExplanationLabel.setText(getStrategyExplanation(strat));

        // Match description
        String milestoneDesc = isEarth 
                ? I18n.getOrDefault("scenario.autodialog.interpolated_epoch", "Époque continue interpolée via TemporalMapTensorManager.")
                : String.format(Locale.ROOT, "Contexte planétaire & colonisation extraterrestre pour %s (%s).", getLocalizedPlanetName(planet), formattedYear);

        if (isEarth) {
            for (AutoEpochScenarioGenerator.EpochMilestone m : AutoEpochScenarioGenerator.EARTH_KEY_MILESTONES) {
                if (Math.abs(m.year() - year) < 500) {
                    milestoneDesc = I18n.getOrDefault(m.descriptionKey(), m.defaultDesc());
                    break;
                }
            }
        }
        milestoneDescLabel.setText("📜 " + milestoneDesc);

        List<Long> availableEpochs = TemporalMapTensorManager.getAvailableEpochYears(planet);
        String rasterFormat = I18n.getOrDefault("scenario.autodialog.raster_preview", 
                "🗺️ Onglets 1 & 2 (Cartographie & Ressources) : 25 calques résolus [%s] (%s, %d époques empiriques). Topographie, biomes, nappes & minerais.");
        rasterPreviewLabel.setText(String.format(Locale.ROOT, rasterFormat,
                strat.getLocalizedName(), getLocalizedPlanetName(planet), availableEpochs.size()));

        long pop = AutoEpochScenarioGenerator.estimatePopulation(planet, year);
        double k0 = AutoEpochScenarioGenerator.estimateCapitalPerCapita(planet, year);
        double e0 = AutoEpochScenarioGenerator.estimateEnergyPerCapita(planet, year);
        String demoFormat = I18n.getOrDefault("scenario.autodialog.demo_preview", 
                "👥 Onglet 3 (Démographie & Tenseurs) : N₀ = %,d hab. | Capital K₀ = %.1f kg/hab. | Énergie E₀ = %.1f MJ/hab. | 9 tenseurs culturels calibrés.");
        demoPreviewLabel.setText(String.format(Locale.ROOT, demoFormat, pop, k0, e0));

        Map<String, Boolean> engines = AutoEpochScenarioGenerator.calibrateTypeBEngines(planet, year);
        long activeCount = engines.values().stream().filter(Boolean::booleanValue).count();
        String activeLabel = I18n.getOrDefault("scenario.autodialog.state.active", "ACTIF");
        String disabledLabel = I18n.getOrDefault("scenario.autodialog.state.disabled", "Désactivé");
        String enginesFormat = I18n.getOrDefault("scenario.autodialog.engines_preview", 
                "⚙️ Onglet 3 (Modules Type B) : %d moteurs activés sur %d (Paléo: %s | Dynasties/SDT: %s | Industriel/IA: %s).");
        enginesPreviewLabel.setText(String.format(Locale.ROOT, enginesFormat,
                activeCount, engines.size(),
                (isEarth && year <= -10000) ? activeLabel : disabledLabel,
                (isEarth && year >= -3000 && year <= 1800) ? activeLabel : disabledLabel,
                year >= 1800 ? activeLabel : disabledLabel));
    }

    private String getStrategyExplanation(TemporalMapTensorManager.DataFallbackStrategy strategy) {
        if (strategy == null) strategy = TemporalMapTensorManager.DataFallbackStrategy.CONTINUOUS_INTERPOLATION;
        return switch (strategy) {
            case CONTINUOUS_INTERPOLATION -> I18n.getOrDefault("scenario.fallback.interpolation.desc", 
                    "🔄 Interpolation temporelle continue (Fondu barycentrique) : Effectue un fondu progressif entre les 2 époques empiriques encadrant la date. Idéal pour modéliser des transitions fluides de topographie (montée des eaux, fonte glaciaire) et de climat.");
            case PREVIOUS_EARLIER_EPOCH -> I18n.getOrDefault("scenario.fallback.earlier_epoch.desc", 
                    "⏮️ Repli conservateur sur l'époque antérieure : Verrouille strictement les données sur l'époque documentée précédente. Prévient tout anachronisme technologique, institutionnel ou géopolitique prématuré.");
            case CLOSEST_ANCHOR_EPOCH -> I18n.getOrDefault("scenario.fallback.closest_anchor.desc", 
                    "🎯 Jalon étalonné le plus proche : Aligne immédiatement l'ensemble des données sur le jalon historique ayant la distance chronologique minimale (|t - t_jalon| min). Idéal pour se baser sur une époque de référence certifiée.");
        };
    }

    private String getLocalizedPlanetName(String p) {
        if (p == null) return I18n.getOrDefault("planet.map.earth", "🌍 Terre");
        return switch (p.toLowerCase()) {
            case "earth", "terre" -> I18n.getOrDefault("planet.map.earth", "🌍 Terre");
            case "mars" -> I18n.getOrDefault("planet.map.mars", "🔴 Mars");
            case "moon", "lune" -> I18n.getOrDefault("planet.map.moon", "🌕 Lune");
            case "venus", "vénus" -> I18n.getOrDefault("planet.map.venus", "♀️ Vénus");
            case "mercury", "mercure" -> I18n.getOrDefault("planet.map.mercury", "☿️ Mercure");
            default -> "🪐 " + p;
        };
    }
}
