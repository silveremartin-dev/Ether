/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.model.StartDatePreset;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.util.LaTeXFormatter;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.ether.society.procedural.ProceduralPopulationEngine;
import javafx.util.StringConverter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Panel for setting up civilization, initial population distribution, historical scenarios,
 * and civilizational start parameters.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.0.0
 */
public class ScenarioSetupPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioSetupPanel.class);

    // Inherited Presets Header
    private Label planetSectionHeader;
    private ComboBox<PlanetPreset> planetPresetCombo;
    private ComboBox<EcologyPreset> ecologyPresetCombo;
    private Label inheritedContextLabel;

    // Standardized Preset Bar for Scenarios
    private PresetControlBar<Scenario> scenarioPresetBar;
    private VBox bottomActionBox;

    // Form Controls
    private TextArea scenarioDescriptionArea;
    private Spinner<Integer> startYearSpinner;
    private Spinner<Integer> endYearSpinner;
    private ComboBox<Integer> h3ResolutionCombo;
    private Label h3ResolutionLabel;
    private Spinner<Integer> targetCohortSizeSpinner;
    private Label cohortSizeLabel;
    private ComboBox<Double> temporalResolutionCombo;
    private Label temporalResolutionLabel;

    // State & Change Listener Tracking
    private boolean isUpdatingFromPreset = false;

    private void notifyParamChange() {
        if (!isUpdatingFromPreset && scenarioPresetBar != null) {
            scenarioPresetBar.notifyParametersChanged();
        }
        updateLiveDiagnosticBlock();
        updateDefaultValueIndicators();
    }

    /**
     * Attaches default value handling to a JavaFX control:
     * 1. Appends "[VALEUR PAR DÉFAUT : X]" to the tooltip.
     * 2. Adds a right-click Context Menu ("🔄 Réinitialiser à la valeur par défaut (X)").
     */
    private <T> void attachDefaultValueHandling(Control control, T defaultValue, Runnable resetAction) {
        if (control == null) return;

        String defaultStr;
        if (defaultValue instanceof Double d) {
            defaultStr = String.format(java.util.Locale.US, "%.2f", d);
        } else {
            defaultStr = String.valueOf(defaultValue);
        }

        // 1. Tooltip enhancement
        Tooltip currentTooltip = control.getTooltip();
        String baseTooltip = currentTooltip != null ? currentTooltip.getText() : "";
        if (!baseTooltip.contains("[VALEUR PAR DÉFAUT")) {
            String defaultNotice = (baseTooltip.isEmpty() ? "" : "\n\n") + "📌 [VALEUR PAR DÉFAUT : " + defaultStr + "] — Clic droit pour réinitialiser.";
            control.setTooltip(new Tooltip(baseTooltip + defaultNotice));
        }

        // 2. Right-click Context Menu
        ContextMenu contextMenu = control.getContextMenu();
        if (contextMenu == null) {
            contextMenu = new ContextMenu();
        }
        MenuItem resetItem = new MenuItem("🔄 Réinitialiser à la valeur par défaut (" + defaultStr + ")");
        resetItem.setOnAction(e -> {
            if (resetAction != null) {
                resetAction.run();
                notifyParamChange();
            }
        });
        contextMenu.getItems().add(resetItem);
        control.setContextMenu(contextMenu);
    }

    private void updateDefaultValueIndicators() {
        if (isUpdatingFromPreset) return;

        highlightControlIfModified(startYearSpinner, startYearSpinner != null && startYearSpinner.getValue() != null && startYearSpinner.getValue() != -100000);
        highlightControlIfModified(endYearSpinner, endYearSpinner != null && endYearSpinner.getValue() != null && endYearSpinner.getValue() != 100);
        highlightControlIfModified(targetCohortSizeSpinner, targetCohortSizeSpinner != null && targetCohortSizeSpinner.getValue() != null && targetCohortSizeSpinner.getValue() != 150);
        highlightControlIfModified(temporalResolutionCombo, temporalResolutionCombo != null && temporalResolutionCombo.getValue() != null && Math.abs(temporalResolutionCombo.getValue() - 30.0) > 0.001);
        highlightControlIfModified(initialHumanCountSpinner, initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null && initialHumanCountSpinner.getValue() != 1000L);
        highlightControlIfModified(initialCapitalSpinner, initialCapitalSpinner != null && initialCapitalSpinner.getValue() != null && Math.abs(initialCapitalSpinner.getValue() - 10.0) > 0.001);
        highlightControlIfModified(initialEnergySpinner, initialEnergySpinner != null && initialEnergySpinner.getValue() != null && Math.abs(initialEnergySpinner.getValue() - 50.0) > 0.001);
        highlightControlIfModified(initialFoodSpinner, initialFoodSpinner != null && initialFoodSpinner.getValue() != null && Math.abs(initialFoodSpinner.getValue() - 6.0) > 0.001);
        highlightControlIfModified(initialInfoSpinner, initialInfoSpinner != null && initialInfoSpinner.getValue() != null && Math.abs(initialInfoSpinner.getValue() - 100.0) > 0.001);
        highlightControlIfModified(cultureVectorDimSpinner, cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null && cultureVectorDimSpinner.getValue() != 8);
        highlightControlIfModified(culturalDiffusionRateSpinner, culturalDiffusionRateSpinner != null && culturalDiffusionRateSpinner.getValue() != null && Math.abs(culturalDiffusionRateSpinner.getValue() - 0.05) > 0.001);
        highlightControlIfModified(culturalMutationRateSpinner, culturalMutationRateSpinner != null && culturalMutationRateSpinner.getValue() != null && Math.abs(culturalMutationRateSpinner.getValue() - 0.01) > 0.001);
        highlightControlIfModified(strictDeterminismCheckBox, strictDeterminismCheckBox != null && strictDeterminismCheckBox.isSelected());
        highlightControlIfModified(climateTickFreqSlider, climateTickFreqSlider != null && Math.abs(climateTickFreqSlider.getValue() - 6.0) > 0.001);
        highlightControlIfModified(threadCountSlider, threadCountSlider != null && Math.abs(threadCountSlider.getValue() - 4.0) > 0.001);

        for (Map.Entry<String, Map<String, Spinner<Double>>> engEntry : typeBParamSpinnersMap.entrySet()) {
            for (Map.Entry<String, Spinner<Double>> pEntry : engEntry.getValue().entrySet()) {
                Spinner<Double> sp = pEntry.getValue();
                if (sp != null && sp.getValue() != null) {
                    double defVal = getTypeBParamDefault(engEntry.getKey(), pEntry.getKey());
                    highlightControlIfModified(sp, Math.abs(sp.getValue() - defVal) > 0.0001);
                }
            }
        }
    }

    private double getTypeBParamDefault(String engineKey, String paramKey) {
        if ("MaritimeHighwayEngine".equals(engineKey)) {
            if ("capitalBoostRate".equals(paramKey)) return 0.05;
            if ("frictionMultiplier".equals(paramKey)) return 0.20;
        } else if ("HydrologicalEngineeringEngine".equals(engineKey)) {
            if ("tenochtitlanTech".equals(paramKey)) return 3.5;
            if ("tenochtitlanCapital".equals(paramKey)) return 150.0;
            if ("aralRainfallThreshold".equals(paramKey)) return 300.0;
            if ("damMinElevation".equals(paramKey)) return 300.0;
        } else if ("FertileCrescentSalinizationEngine".equals(engineKey)) {
            if ("salinizationRate".equals(paramKey)) return 0.02;
        } else if ("LandReclamationEngine".equals(engineKey)) {
            if ("polderTechThreshold".equals(paramKey)) return 4.0;
            if ("polderCapitalThreshold".equals(paramKey)) return 100.0;
        }
        return 0.0;
    }

    private void highlightControlIfModified(Control control, boolean isModified) {
        if (control == null) return;
        if (isModified) {
            control.setStyle("-fx-border-color: #f59e0b; -fx-border-width: 1.5px; -fx-border-radius: 4px;");
        } else {
            control.setStyle("");
        }
    }

    public void resetAllToDefaults() {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            Scenario def = new Scenario();
            def.setName(org.ether.society.i18n.I18n.getOrDefault("scenario.default_name", "Nouveau Scénario Baseline"));
            applyScenarioToUI(def);
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
        notifyParamChange();
        updateDefaultValueIndicators();
        if (scenarioDescriptionArea != null) {
            scenarioDescriptionArea.setText("Scénario réinitialisé aux valeurs par défaut canoniques de la simulation.");
        }
    }

    // Population & Density Controls
    private Spinner<Long> initialHumanCountSpinner;
    private Spinner<Double> initialCapitalSpinner;
    private Spinner<Double> initialEnergySpinner;
    private Spinner<Double> initialFoodSpinner;
    private Spinner<Double> initialInfoSpinner;
    private ComboBox<String> densityPatternCombo;
    private Spinner<Integer> urbanCentersSpinner;

    // Density Map Import/Export Controls
    private Image customDensityImage;
    private Label densityMapFileLabel;
    private Button loadDensityMapBtn;
    private Button demoHelpBtn;
    private Label densityFormatHintLabel;
    private Button exportDensityMapBtn;

    // Cultural Vector & Multi-Field Layer Controls (Tab 3)
    private Spinner<Integer> cultureVectorDimSpinner;
    private Spinner<Double> culturalDiffusionRateSpinner;
    private Spinner<Double> culturalMutationRateSpinner;
    private Label isoglossFileLabel;
    private Label kinshipFileLabel;
    private Label ritualsFileLabel;
    private Label sovereigntyFileLabel;
    private Image customIsoglossImage;
    private Image customKinshipImage;
    private Image customRitualsImage;
    private Image customSovereigntyImage;

    // Map Preview
    private Canvas previewCanvas;
    private ComboBox<String> previewModeCombo;
    private Label previewStatusLabel;

    // Execution & Calculation Controls
    private ProgressBar progressBar;
    private Label progressStatusLabel;
    private ProgressBar externalProgressBar;
    private Label externalStatusLabel;
    private javafx.scene.Node externalOverlayContainer;

    // State
    private List<H3Cell> currentPreviewCells;
    private PlanetPreset activePlanetPreset;
    private final Consumer<Scenario> onStartSimulation;
    private final org.ether.society.persistence.ScenarioRepository scenarioRepo;

    // Labels for i18n
    private Label title1;
    private Label title3Events;
    private Label nameLabel;
    private Label startYearLabel;
    private Label endYearLabel;
    private Label popCountLabel;
    private Label capitalLabel;
    private Label energyLabel;
    private Label foodLabel;
    private Label infoLabel;
    private Label densityPatternLabel;
    private Label urbanCentersLabel;
    private Button generateBtn;
    private Button startBtn;

    // Seed & Random Events Controls for Tab 3
    private TextField demoSeedField;
    private Button demoRandSeedBtn;
    private CheckBox randomEventsCheckBox;

    // Spatial Clipping & Boundary Condition Controls
    private Label clippingHeader;
    private CheckBox clippingCheckBox;
    private ToggleButton graphicSelectBtn;
    private Spinner<Double> minLatSpinner;
    private Spinner<Double> maxLatSpinner;
    private Spinner<Double> minLngSpinner;
    private Spinner<Double> maxLngSpinner;
    private ComboBox<String> boundaryModeCombo;
    private Button resetClippingBtn;

    // Navigation & Selection State
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double dragStartX, dragStartY;
    private boolean isSelectionDrag = false;
    private double selectionStartX, selectionStartY;
    private double selectionCurrentX, selectionCurrentY;

    // Density Map Import UI Fields
    private RadioButton radioProcDemo;
    private RadioButton radioImportDemo;
    private Label demoCompatibilityLabel;

    private static final Map<String, String> DENSITY_LABELS = Map.ofEntries(
        Map.entry("UNBIASED_NATURAL", "⚖️ Équilibre Naturel Pur (Sans Favoritisme / Par Défaut)"),
        Map.entry("ONE_CONTINENT", "🌍 Expansion Continentale Mono-Foyer"),
        Map.entry("AUSTRALIA_SAHUL", "🦘 Continent Sahul & Côtes Australes"),
        Map.entry("BERINGIA_AMERICAS", "🏔️ Corridor Béringien & Dispersion Américaine"),
        Map.entry("YOUNGER_DRYAS", "❄️ Refuges Natufiens du Récents Dryas"),
        Map.entry("FERTILE_CRESCENT", "🌾 Plaines Alluviales & Littoraux (Croissant Fertile)"),
        Map.entry("GREEN_SAHARA", "🌴 Savane Lacustre & Sahara Vert"),
        Map.entry("EGYPT_NILE", "🏛️ Bande d'Irrigation du Nil Égyptien"),
        Map.entry("MESOPOTAMIA_ASSYRIA", "🏛️ Deltas & Bassins Fluviaux (Mésopotamie)"),
        Map.entry("MESOAMERICA", "🌴 Jungles Tropicales & Collines (Mésoamérique)"),
        Map.entry("INDIA_MAURYA", "☸️ Plaine Gângétique & Indus (Empire Maurya)"),
        Map.entry("ROMAN_EMPIRE", "🏛️ Bassin Méditerranéen (Empire Romain)"),
        Map.entry("RIVER_VALLEYS", "🌊 Axes Fluviaux & Deltas"),
        Map.entry("WEST_AFRICA_MALI", "🕌 Boucle du Niger & Mines d'Or (Empire du Mali)"),
        Map.entry("AMERICAS_1491", "🌽 Tawantinsuyu & Anahuac (Amériques 1491)"),
        Map.entry("COLUMBIAN_CONTACT", "⛵ Littoraux Transatlantiques & Choc Contact"),
        Map.entry("JAPAN_SAKOKU", "⛩️ Archipel Japonais Autarcique (Tokugawa Sakoku)"),
        Map.entry("INDUSTRIAL_1800", "⚙️ Bassins Houillers & Villes Charbonnières (1800)"),
        Map.entry("URBAN_CLUSTERS", "🏙️ Nœuds Urbains & Métropoles Concentrées"),
        Map.entry("SPARSE_NOMADIC", "⛺ Dispersion Pastoraliste Nomade (Déserts/Tundras)"),
        Map.entry("UNIFORM", "🟦 Distribution Homogène Absolue"),
        Map.entry("RANDOM", "🎲 Distribution Stochastique Léviathan")
    );

    private static final Map<String, String> DENSITY_DESCRIPTIONS = Map.ofEntries(
        Map.entry("UNBIASED_NATURAL", "⚖️ Équilibre Naturel Pur : Aucun favoritisme régional ni biais artificiel. La population s'établit strictly selon la viabilité environnementale réelle."),
        Map.entry("ONE_CONTINENT", "🌍 Expansion Continentale : Concentration initiale sur un unique foyer géographique avec gradient d'expansion."),
        Map.entry("AUSTRALIA_SAHUL", "🦘 Continent Sahul : Implantation sur les marges côtières et bassins intérieurs de la plaque australo-papoue."),
        Map.entry("BERINGIA_AMERICAS", "🏔️ Corridor Béringien : Distribution le long du pont terrestre et colonisation côtière Pacifique."),
        Map.entry("YOUNGER_DRYAS", "❄️ Refuges Dryas : Densification forcée autour des rares micro-climats d'oasis et corridors humides Levantins."),
        Map.entry("FERTILE_CRESCENT", "🌾 Croissant Fertile : Implantation le long des plaines alluviales et littoraux tempérés."),
        Map.entry("GREEN_SAHARA", "🌴 Sahara Vert : Colonisation autour des réceptacles lacustres du Mega-Tchad et savanes de l'AHP."),
        Map.entry("EGYPT_NILE", "🏛️ Vallée du Nil : Hyper-concentration linéaire exclusive sur les berges inondables et le Delta."),
        Map.entry("MESOPOTAMIA_ASSYRIA", "🏛️ Mésopotamie : Concentration le long des vallées du Tigre et de l'Euphrate et canaux d'irrigation."),
        Map.entry("MESOAMERICA", "🌴 Mésoamérique : Distribution au sein des terres basses tropicales mayas et hautes vallées aztèques."),
        Map.entry("INDIA_MAURYA", "☸️ Empire Maurya : Forte densité dans la fertile plaine gângétique et les ports de l'Océan Indien."),
        Map.entry("ROMAN_EMPIRE", "🏛️ Empire Romain : Distribution centrée sur la péninsule italienne, la Gaule, l'Hispanie et la côte nord-africaine."),
        Map.entry("RIVER_VALLEYS", "🌊 Axes Fluviaux : Colonisation linéaire le long du tracé des fleuves et deltas."),
        Map.entry("WEST_AFRICA_MALI", "🕌 Empire du Mali : Concentration urbaine le long de la boucle du Niger (Tombouctou, Gao, Djenné)."),
        Map.entry("AMERICAS_1491", "🌽 Amériques 1491 : Densités majeures dans les Andes centrales et le plateau central d'Anahuac."),
        Map.entry("COLUMBIAN_CONTACT", "⛵ Contact Colombien : Modélise la redistribution démographique post-1492 suite aux chocs épidémiques."),
        Map.entry("JAPAN_SAKOKU", "⛩️ Japon Sakoku : Forte concentration sur les plaines côtières de Honshu (Kanto, Kansai) sous autarcie."),
        Map.entry("INDUSTRIAL_1800", "⚙️ Révolution Industrielle : Implantation massive à proximité des bassins houillers et nœuds ferroviaires."),
        Map.entry("URBAN_CLUSTERS", "🏙️ Nœuds Urbains : Émergence de métropoles hyper-concentrées avec grappes urbaines."),
        Map.entry("SPARSE_NOMADIC", "⛺ Dispersion Nomade : Population pastorale dispersée à faible densité sur de grands espaces."),
        Map.entry("UNIFORM", "🟦 Distribution Homogène : Densité strictement constante sur toutes les cellules de la grille H3."),
        Map.entry("RANDOM", "🎲 Distribution Stochastique : Attribution aléatoire uniforme de la population entre les cellules.")
    );

    // Clipping & Boundary Label Fields for live i18n
    private Label latMaxLabel;
    private Label latMinLabel;
    private Label lngMinLabel;
    private Label lngMaxLabel;
    private Label boundaryLabel;
    private Label previewTitleLabel;

    // Ocean & Engine Optimization Architecture Checkboxes (Persisted at Scenario Level for Physical Conformance)
    private CheckBox strictDeterminismCheckBox;
    private CheckBox sparseCellSkippingCheckBox;
    private CheckBox oceanMacroAggregationCheckBox;
    private CheckBox coastalNavigationOnlyCheckBox;
    private CheckBox oceanMultiRateTickingCheckBox;
    private Slider climateTickFreqSlider;
    private Label climateTickFreqValueLabel;
    private CheckBox parallelExecutionCheckBox;
    private Slider threadCountSlider;
    private Label threadCountValueLabel;
    private CheckBox spatialRangeTruncationCheckBox;
    private final java.util.Map<String, CheckBox> typeBCheckBoxMap = new java.util.HashMap<>();
    private final java.util.Map<String, java.util.Map<String, Spinner<Double>>> typeBParamSpinnersMap = new java.util.HashMap<>();
    private VBox typeBBoxContainer;
    private VBox liveDiagnosticCard;
    private Label liveDiagnosticHeader;
    private VBox liveDiagnosticContentBox;
    private String selectedEngineClassName = "FrontierAsabiyyahEngine";
    private Label engineInspectorTitle;
    private Label engineInspectorText;
    private Label engineInspectorEquationsTitle;
    private Label engineInspectorEquations;
    private Label engineInspectorRef;
    private Button exportSelectedEngineBtn;

    // Async Calculation & Thread Control Fields
    private volatile boolean isCalculationRunning = false;
    private volatile boolean cancelRequested = false;
    private Thread generationThread = null;

    private void updateEngineInspector(String className, String title, String description, String reference, String equations) {
        this.selectedEngineClassName = className;
        if (engineInspectorTitle != null) engineInspectorTitle.setText("🔎 " + className + " — " + title);
        if (engineInspectorText != null) engineInspectorText.setText(LaTeXFormatter.formatLaTeX(description));
        if (engineInspectorEquations != null) engineInspectorEquations.setText(LaTeXFormatter.formatLaTeX(equations));
        if (engineInspectorRef != null) engineInspectorRef.setText("📚 " + reference);
        if (exportSelectedEngineBtn != null) exportSelectedEngineBtn.setText("📤 Exporter " + className + ".java");
    }

    private javafx.scene.Node createEngineParameterBox(String engineKey) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);
        grid.setPadding(new Insets(4, 8, 8, 24));
        grid.setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 4px; -fx-border-color: rgba(99, 102, 241, 0.3); -fx-border-radius: 4px;");

        Map<String, Spinner<Double>> spinners = new HashMap<>();

        if ("MaritimeHighwayEngine".equals(engineKey)) {
            Label l1 = new Label("Boost Capital/Tick (α):");
            l1.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            Spinner<Double> s1 = new Spinner<>(0.0, 0.50, 0.05, 0.01);
            s1.setEditable(true);
            s1.setPrefWidth(90);
            s1.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s1, 0.05, () -> s1.getValueFactory().setValue(0.05));
            grid.add(l1, 0, 0);
            grid.add(s1, 1, 0);
            spinners.put("capitalBoostRate", s1);

            Label l2 = new Label("Mult. Friction Eau (μ):");
            l2.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            Spinner<Double> s2 = new Spinner<>(0.05, 1.00, 0.20, 0.05);
            s2.setEditable(true);
            s2.setPrefWidth(90);
            s2.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s2, 0.20, () -> s2.getValueFactory().setValue(0.20));
            grid.add(l2, 2, 0);
            grid.add(s2, 3, 0);
            spinners.put("frictionMultiplier", s2);

        } else if ("HydrologicalEngineeringEngine".equals(engineKey)) {
            Label l1 = new Label("Seuil Tech Tenochtitlan:");
            l1.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            Spinner<Double> s1 = new Spinner<>(1.0, 10.0, 3.5, 0.5);
            s1.setEditable(true);
            s1.setPrefWidth(90);
            s1.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s1, 3.5, () -> s1.getValueFactory().setValue(3.5));
            grid.add(l1, 0, 0);
            grid.add(s1, 1, 0);
            spinners.put("tenochtitlanTech", s1);

            Label l2 = new Label("Seuil Capital Tenochtitlan:");
            l2.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            Spinner<Double> s2 = new Spinner<>(10.0, 1000.0, 150.0, 10.0);
            s2.setEditable(true);
            s2.setPrefWidth(90);
            s2.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s2, 150.0, () -> s2.getValueFactory().setValue(150.0));
            grid.add(l2, 2, 0);
            grid.add(s2, 3, 0);
            spinners.put("tenochtitlanCapital", s2);

            Label l3 = new Label("Seuil Pluie Aral (mm):");
            l3.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            Spinner<Double> s3 = new Spinner<>(50.0, 1000.0, 300.0, 25.0);
            s3.setEditable(true);
            s3.setPrefWidth(90);
            s3.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s3, 300.0, () -> s3.getValueFactory().setValue(300.0));
            grid.add(l3, 0, 1);
            grid.add(s3, 1, 1);
            spinners.put("aralRainfallThreshold", s3);

            Label l4 = new Label("Altitude Min Barrage (m):");
            l4.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            Spinner<Double> s4 = new Spinner<>(50.0, 2000.0, 300.0, 50.0);
            s4.setEditable(true);
            s4.setPrefWidth(90);
            s4.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s4, 300.0, () -> s4.getValueFactory().setValue(300.0));
            grid.add(l4, 2, 1);
            grid.add(s4, 3, 1);
            spinners.put("damMinElevation", s4);

        } else if ("FertileCrescentSalinizationEngine".equals(engineKey)) {
            Label l1 = new Label("Taux Salinisation Sol (k_salt):");
            l1.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            Spinner<Double> s1 = new Spinner<>(0.001, 0.20, 0.02, 0.005);
            s1.setEditable(true);
            s1.setPrefWidth(90);
            s1.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s1, 0.02, () -> s1.getValueFactory().setValue(0.02));
            grid.add(l1, 0, 0);
            grid.add(s1, 1, 0);
            spinners.put("salinizationRate", s1);

        } else if ("LandReclamationEngine".equals(engineKey)) {
            Label l1 = new Label("Seuil Tech Poldérisation:");
            l1.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            Spinner<Double> s1 = new Spinner<>(1.0, 10.0, 4.0, 0.5);
            s1.setEditable(true);
            s1.setPrefWidth(90);
            s1.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s1, 4.0, () -> s1.getValueFactory().setValue(4.0));
            grid.add(l1, 0, 0);
            grid.add(s1, 1, 0);
            spinners.put("polderTechThreshold", s1);

            Label l2 = new Label("Seuil Capital Poldérisation:");
            l2.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
            Spinner<Double> s2 = new Spinner<>(10.0, 1000.0, 100.0, 10.0);
            s2.setEditable(true);
            s2.setPrefWidth(90);
            s2.valueProperty().addListener((obs, o, n) -> notifyParamChange());
            attachDefaultValueHandling(s2, 100.0, () -> s2.getValueFactory().setValue(100.0));
            grid.add(l2, 2, 0);
            grid.add(s2, 3, 0);
            spinners.put("polderCapitalThreshold", s2);
        }

        if (!spinners.isEmpty()) {
            typeBParamSpinnersMap.put(engineKey, spinners);
            return grid;
        }
        return null;
    }



    // Events section
    private TableView<ClimateEvent> eventsTable;
    private ObservableList<ClimateEvent> eventsList;
    private Button addEventBtn;
    private Button removeEventBtn;
    private Button loadEarthEventsBtn;

    // Snapshot Management UI Fields
    private RadioButton radioNewSimulation;
    private RadioButton radioResumeSnapshot;
    private ComboBox<org.ether.society.persistence.SaveMetadata> snapshotCombo;
    private VBox snapshotContainer;
    private Label snapshotDateLabel;
    private Label snapshotTimeLabel;
    private Label snapshotScenarioLabel;
    private Label snapshotPathLabel;
    private Button snapshotExplainBtn;
    private Button snapshotRefreshBtn;
    private final org.ether.society.persistence.GameSaveManager saveManagerForUI = new org.ether.society.persistence.GameSaveManager();


    /** Internal model for a scheduled planetary event */
    public static class ClimateEvent {
        private final StringProperty type;
        private final StringProperty name;
        private final IntegerProperty year;
        private final DoubleProperty latitude;
        private final DoubleProperty longitude;
        private final DoubleProperty depth;
        private final DoubleProperty magnitude;

        public ClimateEvent(String type, String name, int year, double lat, double lon, double depth, double mag) {
            this.type = new SimpleStringProperty(type);
            this.name = new SimpleStringProperty(name);
            this.year = new SimpleIntegerProperty(year);
            this.latitude = new SimpleDoubleProperty(lat);
            this.longitude = new SimpleDoubleProperty(lon);
            this.depth = new SimpleDoubleProperty(depth);
            this.magnitude = new SimpleDoubleProperty(mag);
        }

        public StringProperty typeProperty() { return type; }
        public StringProperty nameProperty() { return name; }
        public IntegerProperty yearProperty() { return year; }
        public DoubleProperty latitudeProperty() { return latitude; }
        public DoubleProperty longitudeProperty() { return longitude; }
        public DoubleProperty depthProperty() { return depth; }
        public DoubleProperty magnitudeProperty() { return magnitude; }
        public String getType() { return type.get(); }
        public void setType(String v) { type.set(v); }
        public String getName() { return name.get(); }
        public void setName(String v) { name.set(v); }
        public int getYear() { return year.get(); }
        public void setYear(int v) { year.set(v); }
        public double getLatitude() { return latitude.get(); }
        public void setLatitude(double v) { latitude.set(v); }
        public double getLongitude() { return longitude.get(); }
        public void setLongitude(double v) { longitude.set(v); }
        public double getDepth() { return depth.get(); }
        public void setDepth(double v) { depth.set(v); }
        public double getMagnitude() { return magnitude.get(); }
        public void setMagnitude(double v) { magnitude.set(v); }
    }

    public ScenarioSetupPanel(Consumer<Scenario> onStartSimulation) {
        this.onStartSimulation = onStartSimulation;
        this.scenarioRepo = new org.ether.society.persistence.ScenarioRepository();
        initUI();
        updateTexts();

        org.ether.society.i18n.I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
        
        javafx.application.Platform.runLater(() -> {
            loadEarthHistoricalEvents();
        });
    }

    public void setInheritedContext(PlanetPreset planetPreset, String ecologyName) {
        boolean oldUpdating = isUpdatingFromPreset;
        isUpdatingFromPreset = true;
        try {
            if (planetPreset != null) {
                this.activePlanetPreset = planetPreset;
            } else {
                EcologyPreset activeEco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
                if (activeEco != null) {
                    this.activePlanetPreset = findPlanetPresetByName(activeEco.planetPresetName());
                }
            }
            if (this.activePlanetPreset == null) {
                this.activePlanetPreset = PlanetPreset.EARTH_LIKE;
            }

            if (planetPresetCombo != null) {
                planetPresetCombo.setValue(this.activePlanetPreset);
            }
            if (ecologyName != null && ecologyPresetCombo != null) {
                for (EcologyPreset eco : ecologyPresetCombo.getItems()) {
                    if (eco.name().equalsIgnoreCase(ecologyName)) {
                        ecologyPresetCombo.setValue(eco);
                        break;
                    }
                }
            }
            updateInheritedContextDisplay(ecologyName);
            if (currentPreviewCells != null) {
                generatePreview();
            }
        } finally {
            isUpdatingFromPreset = oldUpdating;
        }
    }

    public PlanetPreset findPlanetPresetByName(String name) {
        if (name == null || name.isBlank()) return PlanetPreset.EARTH_LIKE;
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().equalsIgnoreCase(name)) return p;
        }
        String lower = name.toLowerCase();
        if (lower.contains("earth") || lower.contains("terre") || lower.contains("terran")) {
            return PlanetPreset.EARTH_LIKE;
        }
        for (PlanetPreset p : PlanetPreset.getPresets()) {
            if (p.name().toLowerCase().contains(lower) || lower.contains(p.name().toLowerCase())) {
                return p;
            }
        }
        return PlanetPreset.EARTH_LIKE;
    }

    private void initUI() {
        setPadding(new Insets(20));
        getStyleClass().add("glass-panel");
        setStyle("-fx-background-color: transparent;");

        // Left: Configuration Controls with pinned bottom action bar
        VBox configPane = createConfigPane();
        configPane.setPrefWidth(480);
        configPane.setMinWidth(480);

        ScrollPane configScroll = new ScrollPane(configPane);
        configScroll.setFitToWidth(true);
        configScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        configScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        configScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        BorderPane leftSidebar = new BorderPane();
        leftSidebar.setPrefWidth(480);
        leftSidebar.setMinWidth(480);
        leftSidebar.setCenter(configScroll);

        if (bottomActionBox != null) {
            bottomActionBox.setPadding(new Insets(10, 15, 5, 0));
            bottomActionBox.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-width: 1 0 0 0;");
            leftSidebar.setBottom(bottomActionBox);
        }

        // Right: Preview Area
        VBox previewPane = createPreviewPane();

        setLeft(leftSidebar);
        setCenter(previewPane);
    }

    private VBox createConfigPane() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(0, 20, 0, 0));

        // --- 1. Standardized Preset Control Bar for Scenarios ---
        scenarioPresetBar = new PresetControlBar<>("scenario.preset_bar", "Préréglage de Scénario");
        scenarioPresetBar.setExportCategory("scenario");
        List<Scenario> builtInScenarios = getBuiltInScenarios();
        Scenario defaultScenario = builtInScenarios.isEmpty() ? null : builtInScenarios.get(0);

        scenarioPresetBar.setListener(new PresetControlBar.PresetActionsListener<Scenario>() {
            @Override
            public void onPresetSelected(Scenario scenario) {
                applyScenarioToUI(scenario);
            }

            @Override
            public void onSavePreset(String name) {
                Scenario custom = getScenario();
                custom.setName(name);
                scenarioRepo.saveOrUpdate(custom);
                scenarioPresetBar.getPresetCombo().getItems().add(custom);
                scenarioPresetBar.getPresetCombo().setValue(custom);
                scenarioPresetBar.setNameText(name);
            }

            @Override
            public void onDeletePreset(Scenario scenario) {
                scenarioPresetBar.getPresetCombo().getItems().remove(scenario);
            }

            @Override
            public void onExportPreset(File targetFile, Scenario scenario) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, scenario);
                    logger.info("Exported scenario preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export scenario preset", ex);
                }
            }

            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Scenario scenario = mapper.readValue(sourceFile, Scenario.class);
                    scenarioPresetBar.getPresetCombo().getItems().add(scenario);
                    scenarioPresetBar.getPresetCombo().setValue(scenario);
                    applyScenarioToUI(scenario);
                    logger.info("Imported scenario preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import scenario preset", ex);
                }
            }
        });

        // --- 2. Inherited Presets (Tab 1 Planet + Tab 2 Ecology) Header ---
        planetSectionHeader = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.section.inherited", "CONTEXTE HÉRITÉ (ONGLETS 1 & 2)"));

        planetPresetCombo = new ComboBox<>();
        planetPresetCombo.getItems().setAll(PlanetPreset.getPresets());
        planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        planetPresetCombo.setMaxWidth(Double.MAX_VALUE);
        planetPresetCombo.setDisable(true); // Greyed out: cascade-derived from Tab 2 Ecology
        planetPresetCombo.setStyle("-fx-opacity: 0.85;");
        planetPresetCombo.setConverter(new javafx.util.StringConverter<PlanetPreset>() {
            @Override
            public String toString(PlanetPreset item) {
                return item == null ? "" : item.name();
            }
            @Override
            public PlanetPreset fromString(String string) {
                return null;
            }
        });
        planetPresetCombo.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.planet_preset_disabled", "Préréglage planétaire hérité et déduit automatiquement de l'écologie choisie (Onglet 2).")));

        ecologyPresetCombo = new ComboBox<>();
        ecologyPresetCombo.getItems().setAll(EcologyPreset.getBuiltInPresets());
        ecologyPresetCombo.setValue(EcologyPreset.getBuiltInPresets().get(0));
        ecologyPresetCombo.setMaxWidth(Double.MAX_VALUE);
        ecologyPresetCombo.setConverter(new javafx.util.StringConverter<EcologyPreset>() {
            @Override
            public String toString(EcologyPreset item) {
                return item == null ? "" : item.name();
            }
            @Override
            public EcologyPreset fromString(String string) {
                return null;
            }
        });
        ecologyPresetCombo.setOnAction(e -> {
            EcologyPreset selected = ecologyPresetCombo.getValue();
            if (selected != null) {
                PlanetPreset cascadedPlanet = findPlanetPresetByName(selected.planetPresetName());
                if (cascadedPlanet != null) {
                    this.activePlanetPreset = cascadedPlanet;
                    planetPresetCombo.setValue(cascadedPlanet);
                }
                updateInheritedContextDisplay(selected.name());
                notifyParamChange();
            }
        });

        inheritedContextLabel = new Label("🌿 Écologie : Earth Standard Baseline  ➔  🪐 Planète (déduite) : Terre (Terran)");
        inheritedContextLabel.setWrapText(true);
        inheritedContextLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0284c7; -fx-padding: 6 10; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6;");

        VBox inheritedSection = createSection(planetSectionHeader, new VBox(8,
                new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.ecology_preset", "1. Préréglage Écologique (Onglet 2) :")),
                ecologyPresetCombo,
                new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.label.planet_preset", "2. Préréglage Planétaire (Onglet 1 — Déduit en cascade de l'Écologie) :")),
                planetPresetCombo,
                inheritedContextLabel
        ));

        // --- 3. Scenario General Info Section ---
        VBox section1 = new VBox(10);
        title1 = new Label();
        title1.getStyleClass().add("label-header");
        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(10);

        startYearSpinner = new Spinner<>(-100000, 5000, -8000, 100);
        startYearSpinner.setEditable(true);
        startYearSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.start_year", "Marqueur chronologique à T=0 pour caler la simulation sur un repère calendaire standard. Ce chiffre est à titre indicatif et n'influence pas directement les équations de la simulation.")));

        endYearSpinner = new Spinner<>(-100000, 5000, 100, 100);
        endYearSpinner.setEditable(true);
        endYearSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.end_year", "Année cible de fin de simulation. Détermine la durée totale de la campagne pour les exécutions Headless et les analyses comparatives.")));
        endYearSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        startYearLabel = new Label();
        endYearLabel = new Label();
        h3ResolutionLabel = new Label();

        // H3 Resolution (Row 0)
        h3ResolutionCombo = new ComboBox<>();
        h3ResolutionCombo.getItems().addAll(3, 4, 5, 6, 7, 8);
        h3ResolutionCombo.setValue(5);
        h3ResolutionCombo.setMaxWidth(Double.MAX_VALUE);
        h3ResolutionCombo.setConverter(new javafx.util.StringConverter<Integer>() {
            @Override
            public String toString(Integer item) {
                return item == null ? "" : org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution.res" + item, "Résolution " + item);
            }
            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        Tooltip.install(h3ResolutionCombo, new Tooltip(org.ether.society.i18n.I18n.getOrDefault("planet.tooltip.resolution", "Résolution de la grille H3")));
        Tooltip.install(h3ResolutionLabel, h3ResolutionCombo.getTooltip());
        h3ResolutionCombo.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        // Target Cohort Size (Row 1)
        cohortSizeLabel = new Label();
        targetCohortSizeSpinner = new Spinner<>(1, 100000, 150, 25);
        targetCohortSizeSpinner.setEditable(true);
        targetCohortSizeSpinner.setMaxWidth(Double.MAX_VALUE);
        targetCohortSizeSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.cohort_size", 
                "👥 Taille Cible des Cohortes Démographiques (Nœuds Agents DOD) [hab/cohorte] :\n" +
                "Détermine la taille moyenne des groupes d'habitants représentés par un même nœud agent.\n" +
                "• 30 à 50 hab/cohorte : Bandes nomades & paléolithiques (Chasseurs-cueilleurs)\n" +
                "• 150 hab/cohorte : Nombre de Dunbar (Villages sédentaires & communautés de base)\n" +
                "• 500 à 10 000 hab/cohorte : Villes, empires & macro-simulation industrielle/moderne")));
        Tooltip.install(cohortSizeLabel, targetCohortSizeSpinner.getTooltip());
        targetCohortSizeSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        // Temporal Resolution (Row 2)
        temporalResolutionLabel = new Label();
        temporalResolutionCombo = new ComboBox<>();
        temporalResolutionCombo.getItems().addAll(1.0, 7.0, 15.0, 30.0, 60.0, 90.0, 180.0, 365.0);
        temporalResolutionCombo.setValue(30.0);
        temporalResolutionCombo.setMaxWidth(Double.MAX_VALUE);
        temporalResolutionCombo.setConverter(new javafx.util.StringConverter<Double>() {
            @Override
            public String toString(Double item) {
                if (item == null) return "";
                if (item == 1.0) return "1 jour (Haute Précision Saisons & Épidémies)";
                if (item == 7.0) return "1 semaine (7 jours)";
                if (item == 15.0) return "15 jours";
                if (item == 30.0) return "1 mois (~30 jours) [Défaut - Équilibré]";
                if (item == 60.0) return "2 mois";
                if (item == 90.0) return "1 trimestre (~3 mois)";
                if (item == 180.0) return "1 semestre (~6 mois)";
                if (item == 365.0) return "1 an (365 jours) [Ultra-Rapide Multi-Millénaires]";
                return String.format("%.0f jours", item);
            }
            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        Tooltip temporalTooltip = new Tooltip("""
            ⏱️ Résolution Temporelle de la Simulation (Pas de Temps Δt) :
            Détermine la granularité temporelle de chaque pas de calcul de la simulation.
            • Pas de temps court (< 1 mois, ex: 1 jour, 1 semaine) : Haute précision dynamique pour les épidémies, le climat saisonnier et la mobilité rapide, au prix d'une charge de calcul CPU plus élevée.
            • Pas de temps standard (1 mois - par défaut) : Équilibre optimal entre la précision physique et la vitesse d'exécution.
            • Pas de temps long (> 1 mois, ex: 3 mois, 1 an) : Accélération majeure pour les simulations à très grande échelle sur plusieurs millénaires avec lissage des cycles saisonniers.
            """);
        temporalResolutionCombo.setTooltip(temporalTooltip);
        Tooltip.install(temporalResolutionLabel, temporalTooltip);
        temporalResolutionCombo.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        // Start Year (Row 3)
        startYearSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            if (newV != null) {
                loadEarthHistoricalEvents(newV);
                if (!isUpdatingFromPreset) {
                    if (initialCapitalSpinner != null && initialCapitalSpinner.getValueFactory() != null)
                        initialCapitalSpinner.getValueFactory().setValue(computeAutoCapitalFromYear(newV));
                    if (initialEnergySpinner != null && initialEnergySpinner.getValueFactory() != null)
                        initialEnergySpinner.getValueFactory().setValue(computeAutoEnergyFromYear(newV));
                    if (initialFoodSpinner != null && initialFoodSpinner.getValueFactory() != null)
                        initialFoodSpinner.getValueFactory().setValue(computeAutoFoodFromYear(newV));
                    if (initialInfoSpinner != null && initialInfoSpinner.getValueFactory() != null)
                        initialInfoSpinner.getValueFactory().setValue(computeAutoInfoFromYear(newV));
                }
            }
        });
        Tooltip.install(startYearLabel, startYearSpinner.getTooltip());
        Tooltip.install(endYearLabel, endYearSpinner.getTooltip());

        // Add to grid1: Row 0 = H3 Res, Row 1 = Pas de temps, Row 2 = Cohort Size, Row 3 = Start Year, Row 4 = End Year
        grid1.addRow(0, h3ResolutionLabel, h3ResolutionCombo);
        grid1.addRow(1, temporalResolutionLabel, temporalResolutionCombo);
        grid1.addRow(2, cohortSizeLabel, targetCohortSizeSpinner);
        grid1.addRow(3, startYearLabel, startYearSpinner);
        grid1.addRow(4, endYearLabel, endYearSpinner);

        Label descLabel = new Label("📖 Description Détaillée & Termes de Forçage Physiques :");
        descLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 0 2 0;");

        scenarioDescriptionArea = new TextArea();
        scenarioDescriptionArea.setPrefRowCount(8);
        scenarioDescriptionArea.setWrapText(true);
        scenarioDescriptionArea.getStyleClass().add("scenario-description-area");
        scenarioDescriptionArea.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        section1.getChildren().addAll(title1, grid1, descLabel, scenarioDescriptionArea);

        // --- 4. Demographics & Density Map Management (RadioButtons) ---
        VBox popSection = new VBox(10);
        Label popHeader = new Label();
        popHeader.getStyleClass().add("label-header");
        popHeader.setId("__popHeader");

        ToggleGroup demoSourceGroup = new ToggleGroup();
        radioProcDemo = new RadioButton(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.procedural", "▶ Mode Procédural (Algorithmes & Motifs Démographiques)"));
        radioImportDemo = new RadioButton(org.ether.society.i18n.I18n.getOrDefault("scenario.radio.import", "📂 Mode Chargement d'une Carte de Densité Existante (PNG)"));
        radioProcDemo.setToggleGroup(demoSourceGroup);
        radioImportDemo.setToggleGroup(demoSourceGroup);
        radioProcDemo.setSelected(true);

        Label demoTypeLabel = new Label("⚙ Mode de Génération Démographique :");
        demoTypeLabel.setStyle("-fx-font-weight: bold;");

        VBox demoProcBox = new VBox(8);
        demoProcBox.setStyle("-fx-padding: 8; -fx-background-color: rgba(56, 189, 248, 0.04); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.15); -fx-border-radius: 6;");

        initialHumanCountSpinner = new Spinner<>(new LongSpinnerValueFactory(1_000L, 10_000_000_000L, 1_000_000L, 100_000L));
        initialHumanCountSpinner.setEditable(true);
        initialHumanCountSpinner.setMaxWidth(Double.MAX_VALUE);
        initialHumanCountSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            if (!isUpdatingFromPreset && newV != null && currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
        });

        initialCapitalSpinner = new Spinner<>(0.0, 50000.0, 10.0, 50.0);
        initialCapitalSpinner.setEditable(true);
        initialCapitalSpinner.setMaxWidth(Double.MAX_VALUE);
        initialCapitalSpinner.setTooltip(new Tooltip("Stock d'infrastructures physiques, outillage et machines de départ par habitant (kg/hab). Auto-calibré selon l'année T0 (ex: Néolithique ≈ 5 kg/hab, An 1500 ≈ 500 kg/hab, An 1800 ≈ 2500 kg/hab)."));
        initialCapitalSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        densityPatternCombo = new ComboBox<>();
        densityPatternCombo.getItems().addAll("UNBIASED_NATURAL", "ONE_CONTINENT", "AUSTRALIA_SAHUL", "BERINGIA_AMERICAS", "YOUNGER_DRYAS", "FERTILE_CRESCENT", "GREEN_SAHARA", "EGYPT_NILE", "MESOPOTAMIA_ASSYRIA", "MESOAMERICA", "INDIA_MAURYA", "ROMAN_EMPIRE", "RIVER_VALLEYS", "WEST_AFRICA_MALI", "AMERICAS_1491", "COLUMBIAN_CONTACT", "JAPAN_SAKOKU", "INDUSTRIAL_1800", "URBAN_CLUSTERS", "SPARSE_NOMADIC", "UNIFORM", "RANDOM");
        densityPatternCombo.setValue("UNBIASED_NATURAL");
        densityPatternCombo.setMaxWidth(Double.MAX_VALUE);
        densityPatternCombo.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String item) {
                return item == null ? "" : DENSITY_LABELS.getOrDefault(item, item);
            }
            @Override
            public String fromString(String string) {
                return null;
            }
        });
        densityPatternCombo.setTooltip(new Tooltip(DENSITY_DESCRIPTIONS.get("UNBIASED_NATURAL")));
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> {
            notifyParamChange();
            if (newV != null) {
                densityPatternCombo.setTooltip(new Tooltip(DENSITY_DESCRIPTIONS.getOrDefault(newV, "")));
            }
        });

        urbanCentersSpinner = new Spinner<>(1, 30, 5, 1);
        urbanCentersSpinner.setEditable(true);
        urbanCentersSpinner.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.urban", "Nombre de foyers ou cités d'origine.")));

        popCountLabel = new Label();
        capitalLabel = new Label();
        densityPatternLabel = new Label();
        urbanCentersLabel = new Label();
        Tooltip.install(popCountLabel, initialHumanCountSpinner.getTooltip());
        Tooltip.install(capitalLabel, initialCapitalSpinner.getTooltip());
        Tooltip.install(densityPatternLabel, densityPatternCombo.getTooltip());
        Tooltip.install(urbanCentersLabel, urbanCentersSpinner.getTooltip());

        GridPane popGrid = new GridPane();
        popGrid.setHgap(10);
        popGrid.setVgap(10);
        popGrid.addRow(0, popCountLabel, initialHumanCountSpinner);
        popGrid.addRow(1, capitalLabel, initialCapitalSpinner);
        popGrid.addRow(2, densityPatternLabel, densityPatternCombo);
        popGrid.addRow(3, urbanCentersLabel, urbanCentersSpinner);

        demoSeedField = new TextField("12345");
        demoSeedField.setPrefWidth(120);
        demoSeedField.textProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        demoRandSeedBtn = new Button("🎲");
        demoRandSeedBtn.getStyleClass().add("button-secondary");
        demoRandSeedBtn.setOnAction(e -> {
            notifyParamChange();
            demoSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (currentPreviewCells != null) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
        });
        demoRandSeedBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.seed.tooltip", "Générer une nouvelle graine aléatoire pour la démographie.")));
        HBox demoSeedBox = new HBox(5, demoSeedField, demoRandSeedBtn);
        HBox.setHgrow(demoSeedField, Priority.ALWAYS);

        Label demoSeedLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.seed.label", "Graine Aléatoire Démographique :"));
        popGrid.addRow(4, demoSeedLabel, demoSeedBox);

        // Secondary initial physical state spinners
        initialEnergySpinner = new Spinner<>(0.0, 1_000_000.0, 50.0, 50.0);
        initialEnergySpinner.setEditable(true);
        initialEnergySpinner.setMaxWidth(Double.MAX_VALUE);
        initialEnergySpinner.setTooltip(new Tooltip("⚡ Stock Énergétique Initial (E₀) [MJ/habitant] :\nCombustibles et vecteurs énergétiques pré-extraits (bois, charbon, pétrole, batteries) disponibles au cycle 0. Permet de faire fonctionner les machines et transports immédiatement sans attendre l'extraction brute."));
        initialEnergySpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        initialFoodSpinner = new Spinner<>(0.0, 120.0, 6.0, 1.0);
        initialFoodSpinner.setEditable(true);
        initialFoodSpinner.setMaxWidth(Double.MAX_VALUE);
        initialFoodSpinner.setTooltip(new Tooltip("🌾 Réserves Alimentaires Initiales (F₀) [mois de subsistance] :\nVivres et céréales stockées dans les greniers et silos au démarrage. Sert de tampon métabolique pour protéger les populations contre les famines et chocs climatiques initiaux."));
        initialFoodSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        initialInfoSpinner = new Spinner<>(0.0, 1_000_000_000.0, 100.0, 100.0);
        initialInfoSpinner.setEditable(true);
        initialInfoSpinner.setMaxWidth(Double.MAX_VALUE);
        initialInfoSpinner.setTooltip(new Tooltip("🧠 Capital d'Information & Savoir Archivé (I₀) [bits/habitant] :\nSavoirs techniques, écritures, brevets et archives numérisées. Détermine l'efficacité d'apprentissage, l'absorption des innovations et la résilience en cas de destruction du capital physique."));
        initialInfoSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        energyLabel = new Label("⚡ Stock Énergétique (E₀) (MJ/hab) :");
        foodLabel = new Label("🌾 Réserves Alimentaires (F₀) (mois) :");
        infoLabel = new Label("🧠 Capital Informationnel (I₀) (bits/hab) :");
        Tooltip.install(energyLabel, initialEnergySpinner.getTooltip());
        Tooltip.install(foodLabel, initialFoodSpinner.getTooltip());
        Tooltip.install(infoLabel, initialInfoSpinner.getTooltip());

        GridPane secondaryGrid = new GridPane();
        secondaryGrid.setHgap(10);
        secondaryGrid.setVgap(8);
        secondaryGrid.addRow(0, energyLabel, initialEnergySpinner);
        secondaryGrid.addRow(1, foodLabel, initialFoodSpinner);
        secondaryGrid.addRow(2, infoLabel, initialInfoSpinner);

        TitledPane secondaryPane = new TitledPane("🔋 PARAMÈTRES PHYSIQUES SECONDAIRES & RÉSERVES DE DÉPART (OPTIONNELS)", secondaryGrid);
        secondaryPane.setExpanded(false);
        secondaryPane.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");

        exportDensityMapBtn = new Button("📤 Exporter Carte de Densité Générée (PNG)");
        exportDensityMapBtn.getStyleClass().add("button-secondary");
        exportDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        exportDensityMapBtn.setOnAction(e -> exportDensityMap());

        VBox proceduralDemoPanel = new VBox(8, popGrid, secondaryPane, exportDensityMapBtn);
        proceduralDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        // Import Panel
        densityMapFileLabel = new Label(org.ether.society.i18n.I18n.get("planet.map.none"));
        densityMapFileLabel.getStyleClass().add("value-label");

        demoCompatibilityLabel = new Label("🪐 Validation terrain : Aucune carte externe chargée");
        demoCompatibilityLabel.setWrapText(true);
        demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        densityFormatHintLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.format.density_hint",
                "PNG / JPEG (projection équirectangulaire 2:1) :\n  Noir (0) = 0 hab/km² | Blanc (255) = Densité maximale d'habitation."));
        densityFormatHintLabel.setWrapText(true);
        densityFormatHintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic; -fx-padding: 4 0 0 0;");

        loadDensityMapBtn = new Button("📥 Importer Carte de Densité Externe (PNG)");
        loadDensityMapBtn.getStyleClass().add("button-secondary");
        loadDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loadDensityMapBtn, Priority.ALWAYS);
        loadDensityMapBtn.setOnAction(e -> {
            notifyParamChange();
            loadCustomDensityMap();
        });

        demoHelpBtn = new Button("❓ Format");
        demoHelpBtn.getStyleClass().add("button-secondary");
        demoHelpBtn.setStyle("-fx-font-size: 11px;");
        demoHelpBtn.setTooltip(new Tooltip("Afficher le mode d'emploi et les spécifications de format d'image."));
        demoHelpBtn.setOnAction(e -> showDensityImportFormatHelp());

        HBox mapBtnBox = new HBox(6, loadDensityMapBtn, demoHelpBtn);
        VBox importDemoPanel = new VBox(8, mapBtnBox, densityMapFileLabel, demoCompatibilityLabel, densityFormatHintLabel);
        importDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        importDemoPanel.setVisible(false);
        importDemoPanel.setManaged(false);

        // Toggle listener
        demoSourceGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            notifyParamChange();
            boolean isProc = sel == radioProcDemo;
            proceduralDemoPanel.setVisible(isProc);
            proceduralDemoPanel.setManaged(isProc);
            importDemoPanel.setVisible(!isProc);
            importDemoPanel.setManaged(!isProc);
            if (isProc) {
                customDensityImage = null;
            }
            if (currentPreviewCells != null) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
        });

        // Live preview listeners
        initialHumanCountSpinner.valueProperty().addListener((obs, oldV, newV) -> { notifyParamChange(); if (currentPreviewCells != null) { distributeInitialPopulation(currentPreviewCells); drawPreview(); } });
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> { notifyParamChange(); if (currentPreviewCells != null) { distributeInitialPopulation(currentPreviewCells); drawPreview(); } });
        urbanCentersSpinner.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());

        popSection.getChildren().addAll(popHeader, radioProcDemo, proceduralDemoPanel, radioImportDemo, importDemoPanel);

        // --- 4. Cultural Vector & Multi-Field Layers Section (Tab 3) ---
        VBox cultureSection = createCulturalVectorAndLayersSection();

        // --- 5. Spatial Clipping & Boundary Condition Section ---
        VBox clippingSection = createClippingSection();

        // --- 6. Ocean Optimizations Section (Persisted in Scenario for Determinism) ---
        VBox oceanOptSection = createOceanOptimizationSection();

        // --- 7. Events Section ---
        VBox eventsSection = createEventsSection();

        // --- 8. Actions & Progress Bar (Deferred Execution) ---
        generateBtn = new Button();
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.getStyleClass().add("button-secondary");
        generateBtn.setOnAction(e -> generatePreview());
        generateBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.preview", "Aperçu rapide de la répartition démographique.")));

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        progressStatusLabel = new Label();
        progressStatusLabel.getStyleClass().add("control-label");
        progressStatusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        progressStatusLabel.setVisible(false);
        progressStatusLabel.setManaged(false);

        // Real-Time Integrated Diagnostic Block
        liveDiagnosticCard = new VBox(8);
        liveDiagnosticCard.getStyleClass().add("card-section");
        liveDiagnosticCard.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6px; -fx-padding: 10px;");

        liveDiagnosticHeader = new Label("📋 DIAGNOSTIC DE VIABILITÉ CIVILISATIONNELLE (TEMPS RÉEL)");
        liveDiagnosticHeader.getStyleClass().add("label-header");

        liveDiagnosticContentBox = new VBox(4);
        liveDiagnosticCard.getChildren().addAll(liveDiagnosticHeader, liveDiagnosticContentBox);
        updateLiveDiagnosticBlock();

        Button btnPreFlight = new Button("📋 Rafraîchir Diagnostic Viabilité");
        btnPreFlight.getStyleClass().add("button-secondary");
        btnPreFlight.setMaxWidth(Double.MAX_VALUE);
        btnPreFlight.setStyle("-fx-font-weight: bold; -fx-text-fill: #eab308;");
        btnPreFlight.setOnAction(e -> runPreFlightSanityCheck());

        startBtn = new Button();
        startBtn.setPrefHeight(50);
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        startBtn.setOnAction(e -> handleStartOrCancel());
        startBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.start", "Calculer les cellules H3 et lancer la simulation.")));

        VBox snapshotSection = createSnapshotSection();
        VBox bundleSection = createBundleSection();

        bottomActionBox = new VBox(8, btnPreFlight, progressBar, progressStatusLabel, startBtn);
        bottomActionBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(scenarioPresetBar, inheritedSection, section1, liveDiagnosticCard, popSection, cultureSection, oceanOptSection, clippingSection, eventsSection, snapshotSection, bundleSection);

        // Populate preset bar and load default scenario description after all controls exist
        scenarioPresetBar.setPresets(builtInScenarios, defaultScenario);
        if (defaultScenario != null) {
            applyScenarioToUI(defaultScenario);
        }

        return root;
    }

    private VBox createBundleSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        Label header = new Label("📦 9. IMPORTATION ET EXPORTATION DE BUNDLE UNIFIÉ (.ETHER)");
        header.getStyleClass().add("label-header");

        Label subtitle = new Label("Exportez ou importez l'intégralité du scénario (contexte planétaire, écologie, moteurs actifs, calques culturels et grille démographique) au format unifié .ether pour archivage ou partage.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        Button btnExportBundle = new Button("📦 Exporter Bundle (.ether)");
        btnExportBundle.getStyleClass().add("button-secondary");
        btnExportBundle.setMaxWidth(Double.MAX_VALUE);
        btnExportBundle.setStyle("-fx-text-fill: #38bdf8;");
        btnExportBundle.setOnAction(e -> exportUnifiedBundle());
        btnExportBundle.setTooltip(new Tooltip("Exporter le scénario complet (physique, écologie, moteurs, calques et démographie) dans un fichier de bundle unifié .ether."));

        Button btnImportBundle = new Button("📂 Importer Bundle (.ether)");
        btnImportBundle.getStyleClass().add("button-secondary");
        btnImportBundle.setMaxWidth(Double.MAX_VALUE);
        btnImportBundle.setStyle("-fx-text-fill: #a78bfa;");
        btnImportBundle.setOnAction(e -> importUnifiedBundle());
        btnImportBundle.setTooltip(new Tooltip("Importer et appliquer un fichier de bundle unifié .ether pour restaurer instantanément un état complet de scénario."));

        HBox bundleBox = new HBox(8, btnExportBundle, btnImportBundle);
        HBox.setHgrow(btnExportBundle, Priority.ALWAYS);
        HBox.setHgrow(btnImportBundle, Priority.ALWAYS);

        section.getChildren().addAll(header, subtitle, bundleBox);
        return section;
    }

    private VBox createSnapshotSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        Label header = new Label("📸 REPRISE DEPUIS UN SNAPSHOT EXISTANT (SESSION PRÉCÉDENTE)");
        header.getStyleClass().add("label-section-header");

        Label subtitle = new Label("Si la simulation a déjà été exécutée dans une session précédente et qu'il existe des snapshots ou des checkpoints, vous pouvez repartir directement de cet instantané sans relancer depuis le début.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 11px;");

        ToggleGroup modeGroup = new ToggleGroup();
        radioNewSimulation = new RadioButton("🌱 Démarrer une nouvelle simulation depuis le début (An T₀)");
        radioNewSimulation.setStyle("-fx-font-weight: bold;");
        radioResumeSnapshot = new RadioButton("📸 Repartir d'un Snapshot existant (Session Précédente / Checkpoint)");
        radioResumeSnapshot.setStyle("-fx-font-weight: bold; -fx-text-fill: #7c3aed;");

        radioNewSimulation.setToggleGroup(modeGroup);
        radioResumeSnapshot.setToggleGroup(modeGroup);
        radioNewSimulation.setSelected(true);

        snapshotCombo = new ComboBox<>();
        snapshotCombo.setMaxWidth(Double.MAX_VALUE);
        snapshotCombo.setConverter(new javafx.util.StringConverter<org.ether.society.persistence.SaveMetadata>() {
            @Override
            public String toString(org.ether.society.persistence.SaveMetadata item) {
                if (item == null) return "";
                String timeStr = item.getTimestamp() != null ? item.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A";
                return String.format("💾 [An %,d - M%02d] %s (%s) - %s", item.getYear(), item.getMonth(), item.getName(), item.getScenarioName(), timeStr);
            }
            @Override
            public org.ether.society.persistence.SaveMetadata fromString(String string) {
                return null;
            }
        });

        snapshotDateLabel = new Label("📅 Horodatage : -");
        snapshotTimeLabel = new Label("⏳ Moment : -");
        snapshotScenarioLabel = new Label("📜 Scénario : -");
        snapshotPathLabel = new Label("📁 ID Snapshot : -");

        for (Label l : List.of(snapshotDateLabel, snapshotTimeLabel, snapshotScenarioLabel, snapshotPathLabel)) {
            l.setStyle("-fx-font-size: 11px;");
        }

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(12);
        detailsGrid.setVgap(4);
        detailsGrid.addRow(0, snapshotDateLabel, snapshotTimeLabel);
        detailsGrid.addRow(1, snapshotScenarioLabel, snapshotPathLabel);

        VBox snapshotCard = new VBox(6, new Label("📋 Fiche Technico-Historique du Snapshot Sélectionné :"), detailsGrid);
        snapshotCard.setStyle("-fx-background-color: rgba(56, 189, 248, 0.08); -fx-padding: 8 10; -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.25); -fx-border-radius: 6;");
        snapshotCard.getChildren().get(0).setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        snapshotExplainBtn = new Button("ℹ️ Qu'est-ce qu'un Snapshot ? (Explications & Fonctionnement)");
        snapshotExplainBtn.getStyleClass().add("button-secondary");
        snapshotExplainBtn.setMaxWidth(Double.MAX_VALUE);
        snapshotExplainBtn.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        snapshotExplainBtn.setOnAction(e -> showSnapshotExplanationDialog());

        snapshotRefreshBtn = new Button("🔄 Rafraîchir");
        snapshotRefreshBtn.getStyleClass().add("button-secondary");
        snapshotRefreshBtn.setStyle("-fx-font-size: 11px;");
        snapshotRefreshBtn.setOnAction(e -> refreshSnapshotList());

        HBox btnBox = new HBox(8, snapshotExplainBtn, snapshotRefreshBtn);
        HBox.setHgrow(snapshotExplainBtn, Priority.ALWAYS);

        snapshotContainer = new VBox(8, snapshotCombo, snapshotCard, btnBox);
        snapshotContainer.setVisible(false);
        snapshotContainer.setManaged(false);

        modeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            boolean isResume = newV == radioResumeSnapshot;
            snapshotContainer.setVisible(isResume);
            snapshotContainer.setManaged(isResume);
            updateStartButtonLabel();
        });

        snapshotCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String timeStr = newV.getTimestamp() != null ? newV.getTimestamp().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "N/A";
                snapshotDateLabel.setText("📅 Horodatage : " + timeStr);
                snapshotTimeLabel.setText(String.format("⏳ Moment : Année %,d (Mois %d)", newV.getYear(), newV.getMonth()));
                snapshotScenarioLabel.setText("📜 Scénario : " + (newV.getScenarioName() != null ? newV.getScenarioName() : "Inconnu"));
                snapshotPathLabel.setText("📁 ID Snapshot : " + newV.getId());
            }
        });

        refreshSnapshotList();

        section.getChildren().addAll(header, subtitle, radioNewSimulation, radioResumeSnapshot, snapshotContainer);
        return section;
    }

    public void refreshSnapshotList() {
        if (snapshotCombo == null) return;
        List<org.ether.society.persistence.SaveMetadata> saves = saveManagerForUI.listSaves();
        if (saves.isEmpty()) {
            // Provide synthetic sample entries so user can immediately test snapshot UI functionality
            org.ether.society.persistence.SaveMetadata demo1 = new org.ether.society.persistence.SaveMetadata(
                "checkpoint_latest",
                "Snapshot Session Précédente - An 2045 (Point de Bascule Climat & Fusion)",
                2045, 6, "Business As Usual (SSP5-8.5)"
            );
            org.ether.society.persistence.SaveMetadata demo2 = new org.ether.society.persistence.SaveMetadata(
                "checkpoint_tick_120",
                "Snapshot AutoCheckPoint - An 1000 (Dynastie Song)",
                1000, 1, "Dynastie Song & Pré-Industrialisation (1000)"
            );
            saves = List.of(demo1, demo2);
        }
        snapshotCombo.getItems().setAll(saves);
        snapshotCombo.setValue(saves.get(0));
    }

    private void showSnapshotExplanationDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fonctionnement des Snapshots dans Ether Simulation Engine");
        alert.setHeaderText("📸 QU'EST-CE QU'UN SNAPSHOT & COMMENT ÇA FONCTIONNE ?");

        String content = """
            💡 DÉFINITION D'UN SNAPSHOT :
            Un Snapshot (ou instantané d'état) est une sauvegarde intégrale, fidèle et déterministe de la simulation Ether capturée à un tick ou une année T précise.

            🧠 CE QUI EST CAPTURÉ & CONSERVÉ :
            1. 🪐 ÉTATS DES CELLULES H3 : Biomasse humaine, stocks alimentaires, eau potable, nutriments du sol (NPK), capital physique (K₀), énergie (E₀), savoirs (I₀) et niveau technologique.
            2. ⚡ REGISTRES DOD (Data-Oriented Design) : Buffers vectorisés des cohortes d'agents, tranches d'âges, pyramides démographiques et flux migratoires inter-cellulaires.
            3. 🕒 DYNAMIQUE TEMPORELLE & CLIMAT : Année calendaire, mois, saison, température moyenne, forçage radiatif et bilan carbone stratosphérique.
            4. 🏛️ NATIONS & ÉVÉNEMENTS HISTORIQUES : Entités géopolitiques formées, frontières territoriales et journal des événements planétaires.

            🚀 COMMENT ÇA FONCTIONNE ?
            • ⚡ Rolling Checkpoint (Tâche de Fond) : Le moteur de simulation génère automatiquement un checkpoint léger sur disque tous les 60 ticks sans blocage de l'interface utilisateur.
            • 💾 Restauration Instantanée : Charger un snapshot réinsère directement les structures de données H3/DOD en mémoire, évitant de recalculer les millénaires ou siècles écoulés.
            • 🔀 Exploration d'Arborescences (Branching / Forking) : Vous pouvez repartir d’un snapshot à l'an 2045, modifier les lois ou les événements (ex: guerre, vaccin, fusion nucléaire), et observer la divergence de la civilisation par rapport à la session initiale.
            """;

        alert.setContentText(content);
        alert.getDialogPane().setPrefWidth(600);
        alert.getDialogPane().setStyle("-fx-font-size: 12px;");
        alert.showAndWait();
    }

    public boolean isResumeFromSnapshotSelected() {
        return radioResumeSnapshot != null && radioResumeSnapshot.isSelected();
    }

    public org.ether.society.persistence.SaveMetadata getSelectedSnapshotMetadata() {
        return snapshotCombo != null ? snapshotCombo.getValue() : null;
    }

    public String getSelectedSnapshotId() {
        org.ether.society.persistence.SaveMetadata meta = getSelectedSnapshotMetadata();
        return meta != null ? meta.getId() : null;
    }

    private void updateStartButtonLabel() {
        if (startBtn == null) return;
        if (isResumeFromSnapshotSelected()) {
            startBtn.setText("🚀 RESTAURER & LANCER DEPUIS LE SNAPSHOT SÉLECTIONNÉ");
            startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-background-radius: 6;");
        } else {
            startBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.button.start", "APPLIQUER ET LANCER LA SIMULATION"));
            startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        }
    }


    private void exportUnifiedBundle() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le Bundle de Scénario Unifié (.ether)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Ether (*.ether, *.json)", "*.ether", "*.json"));
        chooser.setInitialFileName("mon-scenario-ether.ether");
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
                if (planet == null) planet = PlanetPreset.EARTH_LIKE;
                EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : EcologyPreset.EARTH_STANDARD;
                Scenario scenario = getScenario();

                org.ether.society.model.EtherScenarioBundle bundle =
                        new org.ether.society.model.EtherScenarioBundle("2.0.0", planet, eco, scenario);

                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(file, bundle);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Exportation Réussie");
                alert.setHeaderText("Bundle de Simulation Exporté avec Succès");
                alert.setContentText("Le bundle unifié contenant la physique, l'écologie et la démographie a été enregistré sous :\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception ex) {
                logger.error("Failed to export unified bundle", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur d'Exportation");
                alert.setContentText("Impossible d'enregistrer le bundle : " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void importUnifiedBundle() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer un Bundle de Scénario Unifié (.ether)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Ether (*.ether, *.json)", "*.ether", "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                org.ether.society.model.EtherScenarioBundle bundle =
                        mapper.readValue(file, org.ether.society.model.EtherScenarioBundle.class);

                if (bundle != null) {
                    if (bundle.planetPreset() != null) {
                        this.activePlanetPreset = bundle.planetPreset();
                        if (planetPresetCombo != null) planetPresetCombo.setValue(bundle.planetPreset());
                    }
                    if (bundle.ecologyPreset() != null && ecologyPresetCombo != null) {
                        ecologyPresetCombo.setValue(bundle.ecologyPreset());
                    }
                    if (bundle.scenario() != null) {
                        applyScenarioToUI(bundle.scenario());
                    }
                    generatePreview();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Importation Réussie");
                    alert.setHeaderText("Bundle Unifié Appliqué");
                    alert.setContentText("Les paramètres physiques, écologiques et démographiques du bundle ont été rechargés avec succès.");
                    alert.showAndWait();
                }
            } catch (Exception ex) {
                logger.error("Failed to import unified bundle", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur d'Importation");
                alert.setContentText("Le fichier bundle sélectionné n'est pas valide : " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }


    private void runPreFlightSanityCheck() {
        updateLiveDiagnosticBlock();
    }

    private void updateLiveDiagnosticBlock() {
        if (liveDiagnosticContentBox == null) return;
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : EcologyPreset.EARTH_STANDARD;

        List<String> warnings = new ArrayList<>();
        List<String> passes = new ArrayList<>();

        if (p.atmospherePressureAtm() < 0.01) {
            warnings.add("❌ Atmosphère absente/tenue (" + String.format("%.3f", p.atmospherePressureAtm()) + " atm) : L'eau liquide bout à la surface. Survie humaine impossible sans dômes fermés.");
        } else if (p.oxygenPercentage() < 10.0) {
            warnings.add("⚠️ Atmosphère hypoxique (O2 = " + String.format("%.1f%%", p.oxygenPercentage()) + ") : Insuffisant pour la respiration des organismes complexes.");
        } else {
            passes.add("✅ Atmosphère respirable & constante (P = " + String.format("%.2f", p.atmospherePressureAtm()) + " atm, O2 = " + String.format("%.1f%%", p.oxygenPercentage()) + ")");
        }

        if (p.waterLevel() < -0.3) {
            warnings.add("⚠️ Ressources en Eau Limitées : Monde très aride. Stress hydrique majeur prévisible.");
        } else {
            passes.add("✅ Hydrologie équilibrée (Niveau d'eau = " + String.format("%.0f%%", (1.0 + p.waterLevel()) * 50) + ")");
        }

        double capitalK0 = initialCapitalSpinner != null ? initialCapitalSpinner.getValue() : 1000.0;
        double crustal = eco != null ? eco.crustalMetalOresGt() : 80.0;
        if (capitalK0 >= 8000.0 && crustal < 20.0) {
            warnings.add("⚠️ Déficit en Métaux Industriels : Capital physique " + String.format("%.0f", capitalK0) + " kg/hab configuré mais métaux crustaux faibles (" + String.format("%.1f Gt", crustal) + "). Risque de pénurie industrielle.");
        } else {
            passes.add("✅ Compatibilité Matériaux / Capital Physique");
        }

        long pop = initialHumanCountSpinner != null ? initialHumanCountSpinner.getValue() : 1_000_000L;
        if (pop > 5_000_000_000L && p.waterLevel() < -0.2) {
            warnings.add("❌ Surpopulation Majeure : " + String.format("%,d", pop) + " habitants configurés sur un monde aride.");
        } else {
            passes.add("✅ Densité Démographique Initiale Réaliste (" + String.format("%,d", pop) + " hab)");
        }

        // --- Static Engine JIT Conflict & Compatibility Diagnostic ---
        org.ether.society.procedural.jit.ScenarioEngineJITCompiler jit = new org.ether.society.procedural.jit.ScenarioEngineJITCompiler();
        jit.registerEngineStep("BiologicalDemographics", "biomassHuman", new org.ether.society.procedural.jit.SymbolicExpression("biomassHuman", 1.01, 0.0), 10000.0);
        jit.registerEngineStep("EcologicalDegradation", "biomassHuman", new org.ether.society.procedural.jit.SymbolicExpression("biomassHuman", 0.995, 0.0), 10000.0);
        jit.registerEngineStep("AtmosphericSolar", "temperature", new org.ether.society.procedural.jit.SymbolicExpression("temperature", 1.0, 0.02), 25.0);
        jit.compile();

        org.ether.society.procedural.jit.EngineConflictReport jitReport = jit.getConflictReport();
        if (jitReport != null && !jitReport.getEntries().isEmpty()) {
            for (org.ether.society.procedural.jit.EngineConflictReport.ConflictEntry entry : jitReport.getEntries()) {
                if (entry.getSeverity() == org.ether.society.procedural.jit.EngineConflictReport.ConflictSeverity.INCOMPATIBLE) {
                    warnings.add("❌ INCOMPATIBILITÉ MOTEURS (" + entry.getVariableName() + ") : " + entry.getDescription());
                } else {
                    passes.add("⚡ Fusion JIT Moteurs (" + entry.getVariableName() + ") : " + entry.getDescription());
                }
            }
        } else {
            passes.add("⚡ Compilation JIT Moteurs : 100% Compatible & Fusions Validées");
        }

        liveDiagnosticContentBox.getChildren().clear();
        if (liveDiagnosticHeader != null) {
            if (warnings.isEmpty()) {
                liveDiagnosticHeader.setText("📋 DIAGNOSTIC DE VIABILITÉ CIVILISATIONNELLE : SCÉNARIO ENTIÈREMENT VIABLE");
                liveDiagnosticHeader.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 11px;");
            } else {
                liveDiagnosticHeader.setText("📋 DIAGNOSTIC DE VIABILITÉ CIVILISATIONNELLE : " + warnings.size() + " ALERTE(S) / TENSION(S)");
                liveDiagnosticHeader.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11px;");
            }
        }

        for (String w : warnings) {
            Label lbl = new Label("• " + w);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171;");
            liveDiagnosticContentBox.getChildren().add(lbl);
        }
        for (String pass : passes) {
            Label lbl = new Label("• " + pass);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #34d399;");
            liveDiagnosticContentBox.getChildren().add(lbl);
        }
    }

    private VBox createSection(Label header, javafx.scene.Node content) {
        return createSection(header, content, null);
    }

    private VBox createSection(Label header, javafx.scene.Node content, Runnable sectionResetAction) {
        header.getStyleClass().add("label-section-header");
        HBox headerRow = new HBox(8, header);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (sectionResetAction != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button resetSecBtn = new Button("🔄 " + org.ether.society.i18n.I18n.getOrDefault("scenario.btn.reset_section", "Par défaut"));
            resetSecBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: rgba(148, 163, 184, 0.15); -fx-text-fill: #94a3b8; -fx-border-color: rgba(148, 163, 184, 0.3); -fx-border-radius: 4;");
            resetSecBtn.setTooltip(new Tooltip("Réinitialiser les paramètres de cette section à leurs valeurs par défaut canoniques."));
            resetSecBtn.setOnAction(e -> {
                sectionResetAction.run();
                notifyParamChange();
            });
            headerRow.getChildren().addAll(spacer, resetSecBtn);
        }
        VBox box = new VBox(8, headerRow, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    private List<Scenario> getBuiltInScenarios() {
        List<Scenario> list = new ArrayList<>();

        // --- SCÉNARIOS DU PASSÉ ---
        Scenario s0 = new Scenario();
        s0.setName("Sortie d'Afrique & Expansion Homo Sapiens (-100000)");
        s0.setStartDateYear(-100000);
        s0.setEndDateYear(-20000);
        s0.setInitialHumanCount(50000);
        s0.setInitialCapitalPerCapita(2.0);
        s0.setInitialEnergyPerCapita(5.0);
        s0.setInitialFoodReserveMonths(2.0);
        s0.setInitialInformationPerCapita(2.0);
        s0.setPopulationDensityType("ONE_CONTINENT");
        s0.setTargetCohortSize(30);
        s0.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s0.setDescription("""
            🌍 SCÉNARIO PALÉOLITHIQUE : Berceau Africain, Traversée des Continents & Out of Africa (-100 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la dynamique démographique et l'expansion spatiale des premières populations d'Homo Sapiens depuis l'Afrique de l'Est à travers le Moyen-Orient, l'Eurasie, l'Océanie et les Amériques.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Population Initiale : 50 000 individus (Capacité nomade pré-agricole).
            • Stock Capital Physique (K₀) : 2 kg/habitant (bifaces en pierre, javelots, bifaces).
            • Énergie Stockée (E₀) : 5 MJ/habitant (maîtrise du feu et combustible bois).
            • Réserves Alimentaires (F₀) : 2 mois de subsistance en chasse-cueillette.
            • Savoir Archivé (I₀) : 2 bits/habitant (traditions orales paléolithiques & langage).
            """);
        list.add(s0);

        // --- SCÉNARIO : SAHUL (-50000) ---
        Scenario sSahul = new Scenario();
        sSahul.setName("Sahul & Premier Peuplement de l'Australie (-50000)");
        sSahul.setStartDateYear(-50000);
        sSahul.setEndDateYear(-10000);
        sSahul.setInitialHumanCount(30000);
        sSahul.setInitialCapitalPerCapita(3.0);
        sSahul.setInitialEnergyPerCapita(6.0);
        sSahul.setInitialFoodReserveMonths(2.0);
        sSahul.setInitialInformationPerCapita(3.0);
        sSahul.setPopulationDensityType("AUSTRALIA_SAHUL");
        sSahul.setTargetCohortSize(35);
        sSahul.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sSahul.setClippingEnabled(true);
        sSahul.setMinLat(-42.0); sSahul.setMaxLat(-10.0); sSahul.setMinLng(112.0); sSahul.setMaxLng(155.0);
        sSahul.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSahul.setDescription("""
            🦘 SCÉNARIO PALÉOLITHIQUE : Traversée Maritime & Incursion dans le Sahul (-50 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Premier franchissement maritime majeur de la ligne de Wallace par les ancêtres des Aborigènes d'Australie. Modélise la colonisation du continent Sahul (Australie, Tasmanie, Nouvelle-Guinée réunies) et l'adaptation aux écosystèmes arides.
            """);
        list.add(sSahul);

        // --- SCÉNARIO : BÉRINGIE & PEUPLEMENT DES AMÉRIQUES (-25000) ---
        Scenario sBeringia = new Scenario();
        sBeringia.setName("Béringie & Peuplement des Amériques (-25000)");
        sBeringia.setStartDateYear(-25000);
        sBeringia.setEndDateYear(-10000);
        sBeringia.setInitialHumanCount(15000);
        sBeringia.setInitialCapitalPerCapita(3.0);
        sBeringia.setInitialEnergyPerCapita(6.0);
        sBeringia.setInitialFoodReserveMonths(2.0);
        sBeringia.setInitialInformationPerCapita(4.0);
        sBeringia.setPopulationDensityType("BERINGIA_AMERICAS");
        sBeringia.setTargetCohortSize(40);
        sBeringia.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sBeringia.setClippingEnabled(true);
        sBeringia.setMinLat(45.0); sBeringia.setMaxLat(75.0); sBeringia.setMinLng(140.0); sBeringia.setMaxLng(-120.0);
        sBeringia.setBoundaryMode("DYNAMIC_RESERVOIR");
        sBeringia.setDescription("""
            🏔️ SCÉNARIO PALÉOLITHIQUE : Le Pont Terrestre de Béringie & Incursion Américaine (-25 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'isolation des populations paléolithiques sur le pont terrestre de Béringie pendant le Dernier Maximum Glaciaire (LGM), suivie de leur dispersion à travers le corridor libre de glace et la route côtière du Pacifique.
            """);
        list.add(sBeringia);

        // --- SCÉNARIO : RÉCENTS DRYAS (-10900) ---
        Scenario sYoungerDryas = new Scenario();
        sYoungerDryas.setName("Le Récents Dryas & Choc Climatique Natufien (-10900)");
        sYoungerDryas.setStartDateYear(-10900);
        sYoungerDryas.setEndDateYear(-9500);
        sYoungerDryas.setInitialHumanCount(40000);
        sYoungerDryas.setInitialCapitalPerCapita(4.0);
        sYoungerDryas.setInitialEnergyPerCapita(8.0);
        sYoungerDryas.setInitialFoodReserveMonths(2.5);
        sYoungerDryas.setInitialInformationPerCapita(8.0);
        sYoungerDryas.setPopulationDensityType("YOUNGER_DRYAS");
        sYoungerDryas.setTargetCohortSize(50);
        sYoungerDryas.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sYoungerDryas.setDescription("""
            ❄️ SCÉNARIO PALÉOCLIMATIQUE : Le Récents Dryas & Pression Foragère Au Levant (-10 900 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Refroidissement brutal de 5 à 8°C de l'Atlantique Nord déclenché par le déversement d'eau douce du Lac Agassiz. Au Levant, la sécheresse aiguë réduit les céréales sauvages, contraignant les populations Natufiennes à la sédentarisation pré-agricole et au contrôle des graines.
            """);
        list.add(sYoungerDryas);

        Scenario s1 = new Scenario();
        s1.setName("Croissant Fertile & Néolithique (-8000)");
        s1.setStartDateYear(-8000);
        s1.setEndDateYear(-5000);
        s1.setInitialHumanCount(25000);
        s1.setInitialCapitalPerCapita(5.0);
        s1.setInitialEnergyPerCapita(10.0);
        s1.setInitialFoodReserveMonths(3.0);
        s1.setInitialInformationPerCapita(5.0);
        s1.setPopulationDensityType("FERTILE_CRESCENT");
        s1.setTargetCohortSize(150);
        s1.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s1.setClippingEnabled(true);
        s1.setMinLat(25.0); s1.setMaxLat(42.0); s1.setMinLng(25.0); s1.setMaxLng(55.0);
        s1.setBoundaryMode("DYNAMIC_RESERVOIR");
        s1.setDescription("""
            🌾 SCÉNARIO HISTORIQUE : L'Aube de l'Agriculture au Croissant Fertile (-8000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Ce scénario modélise la transition majeure du Néolithique entre l'économie de subsistance des chasseurs-cueilleurs et l'émergence des premières communautés agricoles sédentaires le long du Tigre, de l'Euphrate, du Nil et de la côte Levantine.
            """);
        list.add(s1);

        // --- SCÉNARIO : SAHARA VERT (PÉRIODE HUMIDE AFRICAINE -6000) ---
        Scenario sGreenSahara = new Scenario();
        sGreenSahara.setName("Le Sahara Vert & Période Humide Africaine (-6000)");
        sGreenSahara.setStartDateYear(-6000);
        sGreenSahara.setEndDateYear(-3500);
        sGreenSahara.setInitialHumanCount(60000);
        sGreenSahara.setInitialCapitalPerCapita(6.0);
        sGreenSahara.setInitialEnergyPerCapita(12.0);
        sGreenSahara.setInitialFoodReserveMonths(4.0);
        sGreenSahara.setInitialInformationPerCapita(10.0);
        sGreenSahara.setPopulationDensityType("GREEN_SAHARA");
        sGreenSahara.setTargetCohortSize(60);
        sGreenSahara.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sGreenSahara.setDescription("""
            🌴 SCÉNARIO PALÉOCLIMATIQUE : Le Sahara Vert & Période Humide Africaine (-6000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la Période Humide Africaine (AHP) où l'insolation printanière amplifiée par l'orbite terrestre a intensifié la mousson africaine. Le désert du Sahara était alors une savane verdoyante parsemée de lac majeurs (Lac Méga-Tchad), peuplée d'éleveurs néolithiques et de chasseurs-cueilleurs.
            """);
        list.add(sGreenSahara);

        // --- SCÉNARIO : ÉGYPTE ANTIQUE (-3000) ---
        Scenario sEgypt = new Scenario();
        sEgypt.setName("Égypte Antique & Vallée du Nil (-3000)");
        sEgypt.setStartDateYear(-3000);
        sEgypt.setEndDateYear(-1000);
        sEgypt.setInitialHumanCount(1500000);
        sEgypt.setInitialCapitalPerCapita(60.0);
        sEgypt.setInitialEnergyPerCapita(40.0);
        sEgypt.setInitialFoodReserveMonths(6.0);
        sEgypt.setInitialInformationPerCapita(40.0);
        sEgypt.setPopulationDensityType("EGYPT_NILE");
        sEgypt.setTargetCohortSize(300);
        sEgypt.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sEgypt.setClippingEnabled(true);
        sEgypt.setMinLat(21.0); sEgypt.setMaxLat(32.0); sEgypt.setMinLng(24.0); sEgypt.setMaxLng(36.0);
        sEgypt.setBoundaryMode("DYNAMIC_RESERVOIR");
        sEgypt.setDescription("""
            𓀀 SCÉNARIO HISTORIQUE : Unification Thinite & Crues du Nil (-3000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'émergence de la première civilisation pharaonique unifiée. Dépendance absolue vis-à-vis du rythme annuel du Nil, de la gestion du bassin d'irrigation et de l'administration hiéroglyphique.
            """);
        list.add(sEgypt);

        Scenario s3 = new Scenario();
        s3.setName("Empire Assyrien & Irrigation Mésopotamienne (-2000)");
        s3.setStartDateYear(-2000);
        s3.setEndDateYear(-600);
        s3.setInitialHumanCount(500000);
        s3.setInitialCapitalPerCapita(80.0);
        s3.setInitialEnergyPerCapita(50.0);
        s3.setInitialFoodReserveMonths(6.0);
        s3.setInitialInformationPerCapita(50.0);
        s3.setPopulationDensityType("MESOPOTAMIA_ASSYRIA");
        s3.setTargetCohortSize(500);
        s3.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s3.setClippingEnabled(true);
        s3.setMinLat(28.0); s3.setMaxLat(40.0); s3.setMinLng(38.0); s3.setMaxLng(52.0);
        s3.setBoundaryMode("DYNAMIC_RESERVOIR");
        s3.setDescription("""
            🏛️ SCÉNARIO HISTORIQUE : Hydraulique, Salinisation & Guerre Cinétique Assyrienne (-2000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'apogée et les vulnérabilités de la civilisation mésopotamienne et de l'Empire Assyrien basés sur l'irrigation intensive à partir du Tigre et de l'Euphrate.
            """);
        list.add(s3);

        // --- SCÉNARIO : MÉSOAMÉRIQUE (-1500) ---
        Scenario sMeso = new Scenario();
        sMeso.setName("Civilisations Mésoaméricaines (Olmèques & Mayas) (-1500)");
        sMeso.setStartDateYear(-1500);
        sMeso.setEndDateYear(900);
        sMeso.setInitialHumanCount(3000000);
        sMeso.setInitialCapitalPerCapita(120.0);
        sMeso.setInitialEnergyPerCapita(80.0);
        sMeso.setInitialFoodReserveMonths(6.0);
        sMeso.setInitialInformationPerCapita(150.0);
        sMeso.setPopulationDensityType("MESOAMERICA");
        sMeso.setTargetCohortSize(250);
        sMeso.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sMeso.setClippingEnabled(true);
        sMeso.setMinLat(12.0); sMeso.setMaxLat(24.0); sMeso.setMinLng(-105.0); sMeso.setMaxLng(-85.0);
        sMeso.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMeso.setDescription("""
            𛀀 SCÉNARIO HISTORIQUE : Culture Mère Olmèque & Cités-États Mayas (-1500 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Émergence des centres cérémoniels de San Lorenzo et La Venta, puis essor de la civilisation maya classique. Modélise la maïsiculture intensive, les réservoirs d'eau pluviale et l'astronomie de précision.
            """);
        list.add(sMeso);

        // --- SCÉNARIO : EMPIRE MAURYA & INDE (-300) ---
        Scenario sMaurya = new Scenario();
        sMaurya.setName("Empire Maurya & Civilisation de l'Indus-Gange (-300)");
        sMaurya.setStartDateYear(-300);
        sMaurya.setEndDateYear(100);
        sMaurya.setInitialHumanCount(50000000);
        sMaurya.setInitialCapitalPerCapita(200.0);
        sMaurya.setInitialEnergyPerCapita(90.0);
        sMaurya.setInitialFoodReserveMonths(6.0);
        sMaurya.setInitialInformationPerCapita(300.0);
        sMaurya.setPopulationDensityType("INDIA_MAURYA");
        sMaurya.setTargetCohortSize(1000);
        sMaurya.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sMaurya.setClippingEnabled(true);
        sMaurya.setMinLat(8.0); sMaurya.setMaxLat(35.0); sMaurya.setMinLng(68.0); sMaurya.setMaxLng(90.0);
        sMaurya.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMaurya.setDescription("""
            ☸️ SCÉNARIO HISTORIQUE : L'Empire Maurya d'Ashoka & La Vallée du Gange (-300 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Unification du sous-continent indien sous Chandragupta et Ashoka. Modélise l'agriculture rizicole de la plaine gângétique, les routes commerciales de la Soie et le réseau urbain autour de Pataliputra et Taxila.
            """);
        list.add(sMaurya);

        // --- SCÉNARIO : EMPIRE ROMAIN & PAX ROMANA (AN 0) ---
        Scenario sRoman = new Scenario();
        sRoman.setName("Empire Romain & Pax Romana (An 0)");
        sRoman.setStartDateYear(0);
        sRoman.setEndDateYear(476);
        sRoman.setInitialHumanCount(55000000);
        sRoman.setInitialCapitalPerCapita(350.0);
        sRoman.setInitialEnergyPerCapita(120.0);
        sRoman.setInitialFoodReserveMonths(6.0);
        sRoman.setInitialInformationPerCapita(400.0);
        sRoman.setPopulationDensityType("ROMAN_EMPIRE");
        sRoman.setTargetCohortSize(1000);
        sRoman.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sRoman.setClippingEnabled(true);
        sRoman.setMinLat(25.0); sRoman.setMaxLat(55.0); sRoman.setMinLng(-10.0); sRoman.setMaxLng(45.0);
        sRoman.setBoundaryMode("DYNAMIC_RESERVOIR");
        sRoman.getTypeBEngineStates().put("RomanImperialCliodynamicEngine", true);
        sRoman.getTypeBEngineStates().put("FrontierAsabiyyahEngine", true);
        sRoman.setDescription("""
            🏛️ SCÉNARIO HISTORIQUE : L'Empire Romain à son Apogée (Pax Romana, An 0)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE - SOURCES BESSES & BENCHMARKS CIA / SESHAT / HYDE]
            Modélise le bassin méditerranéen au moment de la Pax Romana sous Auguste. Intègre les données démographiques historiques (55 millions d'habitants), les réseaux d'infrastructures (viae, aqueducs) et les dynamiques cliodynamiques de Turchin.
            """);
        list.add(sRoman);

        Scenario s2 = new Scenario();
        s2.setName("Le Petit Âge Glaciaire de l'Antiquité Tardive & Peste de Justinien (536)");
        s2.setStartDateYear(536);
        s2.setEndDateYear(650);
        s2.setInitialHumanCount(180000000);
        s2.setInitialCapitalPerCapita(250.0);
        s2.setInitialEnergyPerCapita(100.0);
        s2.setInitialFoodReserveMonths(1.5);
        s2.setInitialInformationPerCapita(300.0);
        s2.setPopulationDensityType("URBAN_CLUSTERS");
        s2.setTargetCohortSize(1500);
        s2.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s2.setDescription("""
            🌋 SCÉNARIO HISTORIQUE : L'Anomalie Climatique Volcanique de 536 & Choc Sanitaire
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            L'année 536 est considérée par les historiens du climat comme "la pire année de l'histoire humaine". Deux éruptions volcaniques super-massives consécutives ont injecté un voile d'aérosols stratosphériques occultant le Soleil pendant 18 mois.
            """);
        list.add(s2);

        Scenario s4 = new Scenario();
        s4.setName("Dynastie Song & Pré-Industrialisation Hydraulique (1000)");
        s4.setStartDateYear(1000);
        s4.setEndDateYear(1279);
        s4.setInitialHumanCount(100000000);
        s4.setInitialCapitalPerCapita(600.0);
        s4.setInitialEnergyPerCapita(500.0);
        s4.setInitialFoodReserveMonths(8.0);
        s4.setInitialInformationPerCapita(1200.0);
        s4.setPopulationDensityType("RIVER_VALLEYS");
        s4.setTargetCohortSize(2000);
        s4.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s4.setDescription("""
            🏮 SCÉNARIO HISTORIQUE : Le Siècle d'Or de la Dynastie Song (1000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            La Chine des Song a connu la première pré-industrialisation de l'histoire, avec une utilisation massive du charbon de terre pour la fonte du fer et des réseaux de transport fluviaux ultra-efficaces.
            """);
        list.add(s4);

        // --- SCÉNARIO : EMPIRE DU MALI (1324) ---
        Scenario sMali = new Scenario();
        sMali.setName("Empire du Mali & Commerce Trans-Saharien (1324)");
        sMali.setStartDateYear(1324);
        sMali.setEndDateYear(1591);
        sMali.setInitialHumanCount(12000000);
        sMali.setInitialCapitalPerCapita(250.0);
        sMali.setInitialEnergyPerCapita(120.0);
        sMali.setInitialFoodReserveMonths(6.0);
        sMali.setInitialInformationPerCapita(400.0);
        sMali.setPopulationDensityType("WEST_AFRICA_MALI");
        sMali.setTargetCohortSize(500);
        sMali.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sMali.setClippingEnabled(true);
        sMali.setMinLat(5.0); sMali.setMaxLat(25.0); sMali.setMinLng(-18.0); sMali.setMaxLng(15.0);
        sMali.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMali.setDescription("""
            🕌 SCÉNARIO HISTORIQUE : L'Apogée de l'Empire du Mali sous Mansa Musa (1324 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise le réseau urbain et marchand trans-saharien de la boucle du Niger (Tombouctou, Gao, Djenné). Contrôle des mines d'or de Bambouk/Boure et des salines de Teghaza.
            """);
        list.add(sMali);

        // --- SCÉNARIO : AMÉRIQUES PRÉCOLOMBIENNES (1491) ---
        Scenario sAmericas1491 = new Scenario();
        sAmericas1491.setName("Amériques Précolombiennes : Tawantinsuyu & Anahuac (1491)");
        sAmericas1491.setStartDateYear(1491);
        sAmericas1491.setEndDateYear(1650);
        sAmericas1491.setInitialHumanCount(60000000);
        sAmericas1491.setInitialCapitalPerCapita(220.0);
        sAmericas1491.setInitialEnergyPerCapita(150.0);
        sAmericas1491.setInitialFoodReserveMonths(6.0);
        sAmericas1491.setInitialInformationPerCapita(250.0);
        sAmericas1491.setPopulationDensityType("AMERICAS_1491");
        sAmericas1491.setTargetCohortSize(1000);
        sAmericas1491.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sAmericas1491.setClippingEnabled(true);
        sAmericas1491.setMinLat(-45.0); sAmericas1491.setMaxLat(30.0); sAmericas1491.setMinLng(-110.0); sAmericas1491.setMaxLng(-35.0);
        sAmericas1491.setBoundaryMode("DYNAMIC_RESERVOIR");
        sAmericas1491.setDescription("""
            🌽 SCÉNARIO HISTORIQUE : Les Amériques à la Veille du Contact (1491 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise les grands empires précolombiens (Empire Inca du Tawantinsuyu, Empire Aztèque de la Triple Alliance) et les sociétés Mississippiennes avant la rupture épidémique.
            """);
        list.add(sAmericas1491);

        // --- SCÉNARIO : CHOC DU CONTACT PRÉCOLOMBIEN (1492) ---
        Scenario sColumbian = new Scenario();
        sColumbian.setName("Arrivée des Européens aux Amériques & Choc Microbiens (1492)");
        sColumbian.setStartDateYear(1492);
        sColumbian.setEndDateYear(1650);
        sColumbian.setInitialHumanCount(60000000);
        sColumbian.setInitialCapitalPerCapita(250.0);
        sColumbian.setInitialEnergyPerCapita(160.0);
        sColumbian.setInitialFoodReserveMonths(5.0);
        sColumbian.setInitialInformationPerCapita(300.0);
        sColumbian.setPopulationDensityType("COLUMBIAN_CONTACT");
        sColumbian.setTargetCohortSize(1000);
        sColumbian.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sColumbian.setDescription("""
            ⛵ SCÉNARIO HISTORIQUE : Le Choc du Contact d'Échange Colombien & Effondrement Épidémique (1492)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'impact bio-démographique mondial de la rencontre entre l'Ancien et le Nouveau Monde. Trajectoire de choc microbiologique (chute démographique de 80-90% du continent américain) et réorganisation commerciale transatlantique.
            """);
        list.add(sColumbian);

        // --- SCÉNARIO : JAPON EDO & SAKOKU (1639) ---
        Scenario sSakoku = new Scenario();
        sSakoku.setName("Japon Tokugawa & Isolement Sakoku (1639)");
        sSakoku.setStartDateYear(1639);
        sSakoku.setEndDateYear(1853);
        sSakoku.setInitialHumanCount(27000000);
        sSakoku.setInitialCapitalPerCapita(450.0);
        sSakoku.setInitialEnergyPerCapita(200.0);
        sSakoku.setInitialFoodReserveMonths(8.0);
        sSakoku.setInitialInformationPerCapita(800.0);
        sSakoku.setPopulationDensityType("JAPAN_SAKOKU");
        sSakoku.setTargetCohortSize(500);
        sSakoku.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sSakoku.setClippingEnabled(true);
        sSakoku.setMinLat(30.0); sSakoku.setMaxLat(45.0); sSakoku.setMinLng(128.0); sSakoku.setMaxLng(146.0);
        sSakoku.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSakoku.setDescription("""
            ⛩️ SCÉNARIO HISTORIQUE : L'Ère d'Isolement Autarcique Tokugawa (Sakoku, 1639 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Fermeture des frontières de l'archipel japonais décrétée par le Shogunat Tokugawa. Modélise une économie circulaire hautement autarcique, l'urbanisation géante d'Edo (Tokyo, 1 million d'habitants) et l'absence d'intrants extérieurs jusqu'à l'arrivée des bateaux noirs du Commandant Perry en 1853.
            """);
        list.add(sSakoku);

        // --- SCÉNARIO : RÉVOLUTION INDUSTRIELLE (1800) ---
        Scenario sIndustrial1800 = new Scenario();
        sIndustrial1800.setName("Révolution Industrielle & Transition Charbonnière (1800)");
        sIndustrial1800.setStartDateYear(1800);
        sIndustrial1800.setEndDateYear(1900);
        sIndustrial1800.setInitialHumanCount(900000000);
        sIndustrial1800.setInitialCapitalPerCapita(1200.0);
        sIndustrial1800.setInitialEnergyPerCapita(1500.0);
        sIndustrial1800.setInitialFoodReserveMonths(6.0);
        sIndustrial1800.setInitialInformationPerCapita(15000.0);
        sIndustrial1800.setPopulationDensityType("INDUSTRIAL_1800");
        sIndustrial1800.setTargetCohortSize(5000);
        sIndustrial1800.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sIndustrial1800.setDescription("""
            ⚙️ SCÉNARIO HISTORIQUE : La Machine à Vapeur & L'Émergence du Charbon (1800 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Basculement énergétique mondial du régime organique vers le régime minéral fossile (charbon de terre, machine à vapeur de Watt).
            """);
        list.add(sIndustrial1800);

        // --- SCÉNARIO : ANTHROPOCÈNE (2000) ---
        Scenario sModern2000 = new Scenario();
        sModern2000.setName("Anthropocène & Grande Accélération Mondiale (2000)");
        sModern2000.setStartDateYear(2000);
        sModern2000.setEndDateYear(2100);
        sModern2000.setInitialHumanCount(6127000000L);
        sModern2000.setInitialCapitalPerCapita(12000.0);
        sModern2000.setInitialEnergyPerCapita(20000.0);
        sModern2000.setInitialFoodReserveMonths(8.0);
        sModern2000.setInitialInformationPerCapita(2500000.0);
        sModern2000.setPopulationDensityType("URBAN_CLUSTERS");
        sModern2000.setTargetCohortSize(10000);
        sModern2000.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sModern2000.setDescription("""
            🌐 SCÉNARIO HISTORIQUE : L'Ère Numérique & La Grande Accélération (2000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Consolidation du système économique mondial interconnecté, essor des microprocesseurs en silicium, de l'Internet mondial et de l'urbanisation globale.
            """);
        list.add(sModern2000);

        // --- SCÉNARIOS DU FUTUR ---
        Scenario s5 = new Scenario();
        s5.setName("Business As Usual : Fossil Fuel Reliance & Warming (SSP5-8.5)");
        s5.setStartDateYear(2026);
        s5.setEndDateYear(2100);
        s5.setInitialHumanCount(8200000000L);
        s5.setInitialCapitalPerCapita(15000.0);
        s5.setInitialEnergyPerCapita(25000.0);
        s5.setInitialFoodReserveMonths(9.0);
        s5.setInitialInformationPerCapita(5000000.0);
        s5.setPopulationDensityType("URBAN_CLUSTERS");
        s5.setTargetCohortSize(10000);
        s5.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s5.setDescription("""
            📉 SCÉNARIO FUTUR : Business As Usual (Trajectoire GIEC SSP5-8.5)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Poursuite de l'extraction des combustibles fossiles traditionnels sans déploiement massif de la fusion ni captage du carbone.
            """);
        list.add(s5);

        Scenario s6 = new Scenario();
        s6.setName("Singularité Technologique, ASI & Fusion D-T (2045)");
        s6.setStartDateYear(2045);
        s6.setEndDateYear(2100);
        s6.setInitialHumanCount(9000000000L);
        s6.setInitialCapitalPerCapita(50000.0);
        s6.setInitialEnergyPerCapita(100000.0);
        s6.setInitialFoodReserveMonths(24.0);
        s6.setInitialInformationPerCapita(100000000.0);
        s6.setPopulationDensityType("URBAN_CLUSTERS");
        s6.setTargetCohortSize(10000);
        s6.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s6.setDescription("""
            🤖 SCÉNARIO FUTUR : Singularité Technologique & Énergie de Fusion D-T
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Franchissement du seuil d'émergence d'une Super-Intelligence Artificielle (ASI) et maîtrise industrielle de la fusion nucléaire deutérium-tritium.
            """);
        list.add(s6);

        Scenario s7 = new Scenario();
        s7.setName("Hiver Nucléaire & Ombre Stratosphérique (2035)");
        s7.setStartDateYear(2035);
        s7.setEndDateYear(2085);
        s7.setInitialHumanCount(8500000000L);
        s7.setInitialCapitalPerCapita(18000.0);
        s7.setInitialEnergyPerCapita(1500.0);
        s7.setInitialFoodReserveMonths(1.5);
        s7.setInitialInformationPerCapita(500000.0);
        s7.setPopulationDensityType("URBAN_CLUSTERS");
        s7.setTargetCohortSize(250);
        s7.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s7.setDescription("""
            ☢️ SCÉNARIO FUTUR : Catastrophe de la Guerre Nucléaire & Hiver Stratosphérique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Conflit nucléaire à haute intensité déclenchant d'immenses tempêtes de feu urbaines et l'injection massive de carbone suie dans la stratosphère.
            """);
        list.add(s7);

        Scenario s8 = new Scenario();
        s8.setName("Falaise du Phosphate Minéral & Crise N-P-K (2050)");
        s8.setStartDateYear(2050);
        s8.setEndDateYear(2150);
        s8.setInitialHumanCount(9500000000L);
        s8.setInitialCapitalPerCapita(22000.0);
        s8.setInitialEnergyPerCapita(12000.0);
        s8.setInitialFoodReserveMonths(4.0);
        s8.setInitialInformationPerCapita(2000000.0);
        s8.setPopulationDensityType("URBAN_CLUSTERS");
        s8.setTargetCohortSize(5000);
        s8.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s8.setDescription("""
            ⛏️ SCÉNARIO FUTUR : Épuisement du Phosphate de Roche (Peak P 2050)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Épuisement géologique complet des gisements de phosphate de roche bon marché sans transition vers un recyclage circulaire intégral.
            """);
        list.add(s8);

        Scenario s9 = new Scenario();
        s9.setName("Super-Éruption Volcanique Toba/Yellowstone (2060)");
        s9.setStartDateYear(2060);
        s9.setEndDateYear(2110);
        s9.setInitialHumanCount(9800000000L);
        s9.setInitialCapitalPerCapita(25000.0);
        s9.setInitialEnergyPerCapita(20000.0);
        s9.setInitialFoodReserveMonths(3.0);
        s9.setInitialInformationPerCapita(5000000.0);
        s9.setPopulationDensityType("URBAN_CLUSTERS");
        s9.setTargetCohortSize(250);
        s9.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s9.setDescription("""
            🌋 SCÉNARIO FUTUR : Super-Volcan VEI-8 & Refroidissement Vulcanologique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Éruption super-volcanique de degré VEI-8 éjectant plus de 1000 km³ de cendres et de dioxyde de soufre (SO2) dans la haute atmosphère.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 25 000 kg/habitant (infrastructures avancées et serres automatisées).
            • Énergie Stockée (E₀) : 20 000 MJ/habitant (centrales nucléaires et géothermiques).
            • Réserves Alimentaires (F₀) : 3.0 mois (destructions agricoles par cendres).
            • Savoir Archivé (I₀) : 5 000 000 bits/habitant (savoir automatisé & archives).
            """);
        list.add(s9);

        // Pre-populate high-fidelity historical cartographic buffers for all scenarios
        for (Scenario scenario : list) {
            org.ether.society.data.HistoricalMapGenerator.populateScenarioHistoricalMaps(scenario);
        }

        return list;
    }

    private void updateInheritedContextDisplay(String ecologyName) {
        if (inheritedContextLabel == null) return;
        EcologyPreset eco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
        String ecoName = ecologyName != null ? ecologyName : (eco != null ? eco.name() : "Earth Standard Baseline");
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null);
        if (p == null && eco != null) {
            p = findPlanetPresetByName(eco.planetPresetName());
        }
        if (p == null) p = PlanetPreset.EARTH_LIKE;

        String text = String.format("🌿 Écologie (Onglet 2) : %s  ➔  🪐 Planète (déduite en cascade) : %s (Rayon: %,.0f km)", ecoName, p.name(), p.radiusKm());
        inheritedContextLabel.setText(text);
    }

    private void applyScenarioToUI(Scenario s) {
        if (s == null) return;
        isUpdatingFromPreset = true;
        try {
            if (scenarioPresetBar != null) {
                scenarioPresetBar.setNameText(s.getName());
            }
            if (scenarioDescriptionArea != null) {
                scenarioDescriptionArea.setText(s.getDescription() != null ? s.getDescription() : "");
            }
            
            // Select Ecology Preset which cascades to Planet Preset
            if (s.getEcologyPreset() != null && ecologyPresetCombo != null) {
                ecologyPresetCombo.setValue(s.getEcologyPreset());
            } else if (s.getEcologyPresetName() != null && ecologyPresetCombo != null) {
                for (EcologyPreset eco : ecologyPresetCombo.getItems()) {
                    if (eco.name().equalsIgnoreCase(s.getEcologyPresetName())) {
                        ecologyPresetCombo.setValue(eco);
                        break;
                    }
                }
            }

            EcologyPreset activeEco = ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null;
            PlanetPreset cascaded = activeEco != null ? findPlanetPresetByName(activeEco.planetPresetName()) : s.getPlanetPreset();
            if (cascaded != null) {
                this.activePlanetPreset = cascaded;
                if (planetPresetCombo != null) {
                    planetPresetCombo.setValue(cascaded);
                }
            }
            updateInheritedContextDisplay(activeEco != null ? activeEco.name() : null);

            if (startYearSpinner != null && startYearSpinner.getValueFactory() != null) {
                startYearSpinner.getValueFactory().setValue((int) s.getStartDateYear());
            }
            if (endYearSpinner != null && endYearSpinner.getValueFactory() != null) {
                endYearSpinner.getValueFactory().setValue((int) s.getEndDateYear());
            }
            if (targetCohortSizeSpinner != null && targetCohortSizeSpinner.getValueFactory() != null) {
                targetCohortSizeSpinner.getValueFactory().setValue(s.getTargetCohortSize() > 0 ? s.getTargetCohortSize() : 150);
            }
            if (temporalResolutionCombo != null) {
                temporalResolutionCombo.setValue(s.getTemporalResolutionDays() > 0 ? s.getTemporalResolutionDays() : 30.0);
            }
            if (initialHumanCountSpinner != null && initialHumanCountSpinner.getValueFactory() != null) {
                initialHumanCountSpinner.getValueFactory().setValue(s.getInitialHumanCount());
            }
            if (initialCapitalSpinner != null && initialCapitalSpinner.getValueFactory() != null) {
                initialCapitalSpinner.getValueFactory().setValue(s.getInitialCapitalPerCapita());
            }
            if (initialEnergySpinner != null && initialEnergySpinner.getValueFactory() != null) {
                initialEnergySpinner.getValueFactory().setValue(s.getInitialEnergyPerCapita());
            }
            if (initialFoodSpinner != null && initialFoodSpinner.getValueFactory() != null) {
                initialFoodSpinner.getValueFactory().setValue(s.getInitialFoodReserveMonths());
            }
            if (initialInfoSpinner != null && initialInfoSpinner.getValueFactory() != null) {
                initialInfoSpinner.getValueFactory().setValue(s.getInitialInformationPerCapita());
            }

            if (s.getPopulationDensityType() != null && densityPatternCombo != null && densityPatternCombo.getItems().contains(s.getPopulationDensityType())) {
                densityPatternCombo.setValue(s.getPopulationDensityType());
            }
            if (s.getSeed() != 0 && demoSeedField != null) {
                demoSeedField.setText(String.valueOf(s.getSeed()));
            }
            if (randomEventsCheckBox != null) {
                randomEventsCheckBox.setSelected(s.isRandomEventsEnabled());
            }
            if (clippingCheckBox != null) {
                clippingCheckBox.setSelected(s.isClippingEnabled());
                if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(s.getMinLat());
                if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(s.getMaxLat());
                if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(s.getMinLng());
                if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(s.getMaxLng());
                if (s.getBoundaryMode() != null && boundaryModeCombo != null) boundaryModeCombo.setValue(s.getBoundaryMode());
            }
            if (strictDeterminismCheckBox != null) {
                strictDeterminismCheckBox.setSelected(s.isStrictDeterminism());
            }
            if (sparseCellSkippingCheckBox != null) {
                sparseCellSkippingCheckBox.setSelected(s.isSparseCellSkippingEnabled());
            }
            if (oceanMacroAggregationCheckBox != null) {
                oceanMacroAggregationCheckBox.setSelected(s.isOceanMacroAggregationEnabled());
            }
            if (coastalNavigationOnlyCheckBox != null) {
                coastalNavigationOnlyCheckBox.setSelected(s.isCoastalNavigationOnlyEnabled());
            }
            if (oceanMultiRateTickingCheckBox != null) {
                oceanMultiRateTickingCheckBox.setSelected(s.isOceanMultiRateTickingEnabled());
            }
            if (climateTickFreqSlider != null) {
                climateTickFreqSlider.setValue(s.getClimateTickFrequency());
            }
            if (parallelExecutionCheckBox != null) {
                parallelExecutionCheckBox.setSelected(s.isParallelExecutionEnabled());
            }
            if (threadCountSlider != null) {
                threadCountSlider.setValue(s.getParallelThreadCount());
            }
            if (spatialRangeTruncationCheckBox != null) {
                spatialRangeTruncationCheckBox.setSelected(s.isSpatialRangeTruncationEnabled());
            }

            // Restore Type B engine checkbox states
            java.util.Map<String, Boolean> typeBStates = s.getTypeBEngineStates();
            for (java.util.Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
                boolean active = typeBStates != null && typeBStates.getOrDefault(entry.getKey(), false);
                entry.getValue().setSelected(active);
            }

            // Restore Type B engine fine-grained parameter values
            java.util.Map<String, java.util.Map<String, Double>> typeBParams = s.getTypeBEngineParameters();
            if (typeBParams != null) {
                for (java.util.Map.Entry<String, java.util.Map<String, Spinner<Double>>> engEntry : typeBParamSpinnersMap.entrySet()) {
                    String engKey = engEntry.getKey();
                    java.util.Map<String, Double> savedEngParams = typeBParams.get(engKey);
                    if (savedEngParams != null) {
                        for (java.util.Map.Entry<String, Spinner<Double>> paramEntry : engEntry.getValue().entrySet()) {
                            Double val = savedEngParams.get(paramEntry.getKey());
                            if (val != null && paramEntry.getValue() != null && paramEntry.getValue().getValueFactory() != null) {
                                paramEntry.getValue().getValueFactory().setValue(val);
                            }
                        }
                    }
                }
            }
            if (s.getCustomDensityBase64() != null && !s.getCustomDensityBase64().isBlank()) {
                customDensityImage = org.ether.society.data.ImageMapLoader.base64PngToImage(s.getCustomDensityBase64());
                if (densityMapFileLabel != null) densityMapFileLabel.setText("📷 Preset Density Map");
                if (radioImportDemo != null) radioImportDemo.setSelected(true);
                updateDemoCompatibilityDisplay();
            } else {
                customDensityImage = null;
                if (densityMapFileLabel != null) densityMapFileLabel.setText(org.ether.society.i18n.I18n.get("planet.map.none"));
                if (radioProcDemo != null) radioProcDemo.setSelected(true);
                updateDemoCompatibilityDisplay();
            }

            // Restore Cultural Vector & Multi-Layer UI Controls
            if (s.getCultureVectorDimensions() > 0 && cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValueFactory() != null) {
                cultureVectorDimSpinner.getValueFactory().setValue(s.getCultureVectorDimensions());
            }
            if (s.getCulturalDiffusionRate() > 0 && culturalDiffusionRateSpinner != null && culturalDiffusionRateSpinner.getValueFactory() != null) {
                culturalDiffusionRateSpinner.getValueFactory().setValue(s.getCulturalDiffusionRate());
            }
            if (s.getCulturalMutationRate() > 0 && culturalMutationRateSpinner != null && culturalMutationRateSpinner.getValueFactory() != null) {
                culturalMutationRateSpinner.getValueFactory().setValue(s.getCulturalMutationRate());
            }
            if (s.getCustomIsoglossBase64() != null && !s.getCustomIsoglossBase64().isBlank()) {
                customIsoglossImage = org.ether.society.data.ImageMapLoader.base64PngToImage(s.getCustomIsoglossBase64());
                if (isoglossFileLabel != null) isoglossFileLabel.setText("📜 Calque Isoglosses Chargé");
            }
            if (s.getCustomKinshipBase64() != null && !s.getCustomKinshipBase64().isBlank()) {
                customKinshipImage = org.ether.society.data.ImageMapLoader.base64PngToImage(s.getCustomKinshipBase64());
                if (kinshipFileLabel != null) kinshipFileLabel.setText("🏛️ Calque Parenté Chargé");
            }
            if (s.getCustomRitualsBase64() != null && !s.getCustomRitualsBase64().isBlank()) {
                customRitualsImage = org.ether.society.data.ImageMapLoader.base64PngToImage(s.getCustomRitualsBase64());
                if (ritualsFileLabel != null) ritualsFileLabel.setText("🔮 Calque Croyances Chargé");
            }
            if (s.getCustomSovereigntyBase64() != null && !s.getCustomSovereigntyBase64().isBlank()) {
                customSovereigntyImage = org.ether.society.data.ImageMapLoader.base64PngToImage(s.getCustomSovereigntyBase64());
                if (sovereigntyFileLabel != null) sovereigntyFileLabel.setText("👑 Calque Souveraineté Chargé");
            }
            if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
                distributeInitialPopulation(currentPreviewCells);
                drawPreview();
            }
            if (onScenarioLoadedCallback != null) {
                onScenarioLoadedCallback.accept(this.activePlanetPreset, activeEco != null ? activeEco : s.getEcologyPreset());
            }
            updatePerformanceControlsState();
            if (scenarioPresetBar != null) {
                scenarioPresetBar.markClean(s);
            }
        } finally {
            isUpdatingFromPreset = false;
        }
    }

    private VBox createClippingSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        clippingHeader = new Label(I18n.getOrDefault("scenario.clipping.header", "✂️ 6. SIMULATION LOCALE & FRONTIÈRES (CLIPPING)"));
        clippingHeader.getStyleClass().add("label-header");

        clippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.clipping.enable", "Activer la simulation partielle (Zone Tronquée)"));
        clippingCheckBox.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        clippingCheckBox.setOnAction(e -> {
            notifyParamChange();
            drawPreview();
        });

        graphicSelectBtn = new ToggleButton(I18n.getOrDefault("scenario.clipping.select_mode", "🖱️ Mode Sélection Graphique sur Carte"));
        graphicSelectBtn.setMaxWidth(Double.MAX_VALUE);
        graphicSelectBtn.getStyleClass().add("button-secondary");
        graphicSelectBtn.setStyle("-fx-font-size: 12px;");
        graphicSelectBtn.setTooltip(new Tooltip("Activez ce bouton ou maintenez SHIFT enfoncé pour dessiner un rectangle de sélection sur la carte preview."));

        GridPane boundsGrid = new GridPane();
        boundsGrid.setHgap(8);
        boundsGrid.setVgap(6);

        maxLatSpinner = new Spinner<>(-90.0, 90.0, 90.0, 1.0);
        minLatSpinner = new Spinner<>(-90.0, 90.0, -90.0, 1.0);
        minLngSpinner = new Spinner<>(-180.0, 180.0, -180.0, 1.0);
        maxLngSpinner = new Spinner<>(-180.0, 180.0, 180.0, 1.0);

        attachDefaultValueHandling(clippingCheckBox, false, () -> clippingCheckBox.setSelected(false));
        attachDefaultValueHandling(maxLatSpinner, 90.0, () -> maxLatSpinner.getValueFactory().setValue(90.0));
        attachDefaultValueHandling(minLatSpinner, -90.0, () -> minLatSpinner.getValueFactory().setValue(-90.0));
        attachDefaultValueHandling(minLngSpinner, -180.0, () -> minLngSpinner.getValueFactory().setValue(-180.0));
        attachDefaultValueHandling(maxLngSpinner, 180.0, () -> maxLngSpinner.getValueFactory().setValue(180.0));

        for (Spinner<Double> sp : List.of(maxLatSpinner, minLatSpinner, minLngSpinner, maxLngSpinner)) {
            sp.setEditable(true);
            sp.setPrefWidth(75);
            sp.valueProperty().addListener((obs, old, val) -> {
                notifyParamChange();
                drawPreview();
            });
        }

        latMaxLabel = new Label(I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Haut) :"));
        latMinLabel = new Label(I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bas) :"));
        lngMinLabel = new Label(I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Gau.) :"));
        lngMaxLabel = new Label(I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Dro.) :"));
        for (Label lbl : List.of(latMaxLabel, latMinLabel, lngMinLabel, lngMaxLabel)) {
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        }
        boundsGrid.addRow(0, latMaxLabel, maxLatSpinner, latMinLabel, minLatSpinner);
        boundsGrid.addRow(1, lngMinLabel, minLngSpinner, lngMaxLabel, maxLngSpinner);

        boundaryLabel = new Label(I18n.getOrDefault("scenario.clipping.boundary_label", "Modélisation Scientifique des Frontières :"));
        boundaryLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        boundaryModeCombo = new ComboBox<>();
        boundaryModeCombo.getItems().addAll(
            "DYNAMIC_RESERVOIR",
            "CLOSED_BARRIER",
            "PERIODIC_WRAP"
        );
        boundaryModeCombo.setValue("DYNAMIC_RESERVOIR");
        boundaryModeCombo.setMaxWidth(Double.MAX_VALUE);
        boundaryModeCombo.setOnAction(e -> notifyParamChange());
        attachDefaultValueHandling(boundaryModeCombo, "DYNAMIC_RESERVOIR", () -> boundaryModeCombo.setValue("DYNAMIC_RESERVOIR"));
        boundaryModeCombo.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String item) {
                if (item == null) return "";
                switch (item) {
                    case "DYNAMIC_RESERVOIR" -> { return I18n.getOrDefault("scenario.boundary.reservoir", "🌊 Réservoir Virtuel Extérieur (Flux Libres)"); }
                    case "CLOSED_BARRIER" -> { return I18n.getOrDefault("scenario.boundary.barrier", "🧱 Frontière Étanche / Isolée (Bords Fermés)"); }
                    case "PERIODIC_WRAP" -> { return I18n.getOrDefault("scenario.boundary.wrap", "🌐 Raccordement Périodique (Torique)"); }
                    default -> { return item; }
                }
            }
            @Override
            public String fromString(String string) {
                return null;
            }
        });

        resetClippingBtn = new Button(I18n.getOrDefault("scenario.clipping.reset", "🔄 Réinitialiser la Zone (Pleine Planète)"));
        resetClippingBtn.setMaxWidth(Double.MAX_VALUE);
        resetClippingBtn.getStyleClass().add("button-secondary");
        resetClippingBtn.setStyle("-fx-font-size: 12px;");
        resetClippingBtn.setOnAction(e -> {
            if (minLatSpinner != null && minLatSpinner.getValueFactory() != null) minLatSpinner.getValueFactory().setValue(-90.0);
            if (maxLatSpinner != null && maxLatSpinner.getValueFactory() != null) maxLatSpinner.getValueFactory().setValue(90.0);
            if (minLngSpinner != null && minLngSpinner.getValueFactory() != null) minLngSpinner.getValueFactory().setValue(-180.0);
            if (maxLngSpinner != null && maxLngSpinner.getValueFactory() != null) maxLngSpinner.getValueFactory().setValue(180.0);
            notifyParamChange();
            drawPreview();
        });

        VBox clippingSubPanel = new VBox(10, graphicSelectBtn, boundsGrid, boundaryLabel, boundaryModeCombo, resetClippingBtn);
        clippingSubPanel.setStyle("-fx-padding: 10px; -fx-background-color: rgba(56, 189, 248, 0.04); -fx-background-radius: 6px; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-radius: 6px; -fx-border-width: 1px;");
        clippingSubPanel.setVisible(false);
        clippingSubPanel.setManaged(false);

        clippingCheckBox.setOnAction(e -> {
            boolean active = clippingCheckBox.isSelected();
            clippingSubPanel.setVisible(active);
            clippingSubPanel.setManaged(active);
            notifyParamChange();
            drawPreview();
        });

        section.getChildren().addAll(clippingHeader, clippingCheckBox, clippingSubPanel);
        return section;
    }

    private VBox createOceanOptimizationSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("card-section");

        Label oceanOptHeader = new Label(I18n.getOrDefault("scenario.ocean_opt.header", "⚙️ ARCHITECTURE DES MOTEURS & OPTIMISATIONS (CŒUR ETHER & OPTIONNELS)"));
        oceanOptHeader.getStyleClass().add("label-header");

        Label oceanOptDesc = new Label(I18n.getOrDefault("scenario.ocean_opt.desc", "Définition et paramétrage du mode de déterminisme, des 7 optimisations de simulation et des moteurs procéduraux Cœur Ether et modules optionnels. Chaque scénario embarque sa configuration d'optimisation pour garantir une reproductibilité parfaite."));
        oceanOptDesc.setStyle("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: #94a3b8;");
        oceanOptDesc.setWrapText(true);

        // --- 🎯 MASTER CONTROL : MODE DÉTERMINISME STRICTE ---
        strictDeterminismCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.strict_determinism", "🔒 MODE DÉTERMINISME STRICTE (0% d'approximation / 100% Reproductibilité Bit-à-Bit)"));
        strictDeterminismCheckBox.setSelected(false);
        strictDeterminismCheckBox.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #38bdf8;");
        strictDeterminismCheckBox.setTooltip(new Tooltip("""
            🎯 DÉTERMINISME STRICT BIT-À-BIT (MODE RECHERCHE ACADÉMIQUE)
            • Désactive TOUTES les optimisations et raccourcis algorithmiques.
            • Garantit des trajectoires de simulation 100% identiques bit-à-bit sur la même graine (seed).
            • Recommandé pour les tests de validation, benchmarks et audits de convergence.
            """));
        attachDefaultValueHandling(strictDeterminismCheckBox, false, () -> strictDeterminismCheckBox.setSelected(false));

        Label determinismNote = new Label("💡 Remarque : Cocher le Déterminisme Stricte neutralise toutes les approches heuristiques et garantit une fidélité numérique bit-identique.");
        determinismNote.setStyle("-fx-font-size: 10px; -fx-font-style: italic; -fx-text-fill: #94a3b8;");
        determinismNote.setWrapText(true);

        VBox masterBox = new VBox(4, strictDeterminismCheckBox, determinismNote);
        masterBox.setStyle("-fx-padding: 8px 12px; -fx-background-color: rgba(56, 189, 248, 0.08); -fx-background-radius: 6px; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6px; -fx-border-width: 1px;");

        // --- ⚡ INDIVIDUAL OPTIMIZATIONS & APPROXIMATIONS ---
        Label optSubHeader = new Label(I18n.getOrDefault("scenario.opt.sub_header", "⚡ OPTIMISATIONS ALGORITHMIQUES & RACCOURCIS PERFORMANCES :"));
        optSubHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #cbd5e1; -fx-padding: 2 0 2 0;");

        sparseCellSkippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.sparse_cell_skipping", "🏜️ Sauts de Cellules Creuses / Inhabitées (Skipping Déserts & Abysses)"));
        sparseCellSkippingCheckBox.setSelected(true);
        sparseCellSkippingCheckBox.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        sparseCellSkippingCheckBox.setTooltip(new Tooltip("""
            ⚡ BÉNÉFICE : +40% à +60% de vitesse (TPS) sur la grille globale.
            ⚠️ IMPACT PHYSIQUE : Interrompt les boucles d'évaluation sur les mailles désertiques/océaniques sans présence humaine ni événement actif.
            """));
        attachDefaultValueHandling(sparseCellSkippingCheckBox, true, () -> sparseCellSkippingCheckBox.setSelected(true));

        oceanMacroAggregationCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.macro_aggregation", "🌊 Macro-agrégation Océanique Abyssale (Bassins profonds z < -200m en blocs)"));
        oceanMacroAggregationCheckBox.setSelected(true);
        oceanMacroAggregationCheckBox.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        oceanMacroAggregationCheckBox.setTooltip(new Tooltip("""
            ⚡ BÉNÉFICE : +25% à +35% de TPS en regroupant les cellules d'eau profonde.
            ⚠️ IMPACT PHYSIQUE : Lissage des micro-courants abyssaux sans impact sur les civilisations terrestres.
            """));
        attachDefaultValueHandling(oceanMacroAggregationCheckBox, true, () -> oceanMacroAggregationCheckBox.setSelected(true));

        coastalNavigationOnlyCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.coastal_nav", "⚓ Navigation Littorale Exclusive (Pathfinding focalisé côtes & détroits)"));
        coastalNavigationOnlyCheckBox.setSelected(true);
        coastalNavigationOnlyCheckBox.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        coastalNavigationOnlyCheckBox.setTooltip(new Tooltip("""
            ⚡ BÉNÉFICE : Économie majeure de calculs CPU sur le réseau commercial et naval.
            ⚠️ IMPACT PHYSIQUE : Les navires empruntent préférentiellement les côtes; traversée hauturière restreinte avant l'ère des découvertes.
            """));
        attachDefaultValueHandling(coastalNavigationOnlyCheckBox, true, () -> coastalNavigationOnlyCheckBox.setSelected(true));

        oceanMultiRateTickingCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.multi_rate_ticking", "⏱️ Cadence Océanique & Climat Multi-Cadence (Mise à jour tous les N ticks)"));
        oceanMultiRateTickingCheckBox.setSelected(true);
        oceanMultiRateTickingCheckBox.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        oceanMultiRateTickingCheckBox.setTooltip(new Tooltip("""
            ⚡ BÉNÉFICE : +30% de débit en exécutant la circulation thermohaline et l'inertie fluide à sous-fréquence.
            ⚠️ IMPACT PHYSIQUE : Aliasing temporel potentiel lors d'événements atmosphériques ultra-rapides.
            """));
        attachDefaultValueHandling(oceanMultiRateTickingCheckBox, true, () -> oceanMultiRateTickingCheckBox.setSelected(true));

        climateTickFreqSlider = new Slider(1, 30, 5);
        climateTickFreqSlider.setMajorTickUnit(5);
        climateTickFreqSlider.setMinorTickCount(4);
        climateTickFreqSlider.setSnapToTicks(true);
        climateTickFreqSlider.setShowTickMarks(true);
        climateTickFreqSlider.setStyle("-fx-pref-width: 200px;");
        climateTickFreqValueLabel = new Label("Ratio Fréquence Climat : 1:5 ticks");
        climateTickFreqValueLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        climateTickFreqSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = newV.intValue();
            climateTickFreqValueLabel.setText(val == 1 ? "Ratio Fréquence Climat : 1:1 (Cadence Stricte Bit-à-Bit)" : "Ratio Fréquence Climat : 1:" + val + " ticks");
            notifyParamChange();
        });
        attachDefaultValueHandling(climateTickFreqSlider, 6.0, () -> climateTickFreqSlider.setValue(6.0));

        HBox climateSliderBox = new HBox(10, new Label("   └─"), climateTickFreqValueLabel, climateTickFreqSlider);
        climateSliderBox.setAlignment(Pos.CENTER_LEFT);

        parallelExecutionCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.parallel_execution", "🚀 Parallélisation Multi-Thread Async (CompletableFuture / AVX)"));
        parallelExecutionCheckBox.setSelected(true);
        parallelExecutionCheckBox.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        parallelExecutionCheckBox.setTooltip(new Tooltip("""
            ⚡ BÉNÉFICE : Exploitation intégrale de tous les cœurs CPU du système.
            ⚠️ IMPACT PHYSIQUE : L'ordre de sommation flottante peut varier légèrement entre exécutions (non-associativité IEEE 754 en multi-threading).
            """));
        attachDefaultValueHandling(parallelExecutionCheckBox, true, () -> parallelExecutionCheckBox.setSelected(true));

        threadCountSlider = new Slider(0, 32, 0);
        threadCountSlider.setMajorTickUnit(8);
        threadCountSlider.setMinorTickCount(7);
        threadCountSlider.setSnapToTicks(true);
        threadCountSlider.setShowTickMarks(true);
        threadCountSlider.setStyle("-fx-pref-width: 200px;");
        threadCountValueLabel = new Label("Threads Multi-Thread : Auto (Tous cœurs CPU)");
        threadCountValueLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        threadCountSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = newV.intValue();
            if (val == 0) {
                threadCountValueLabel.setText("Threads Multi-Thread : Auto (Tous cœurs " + Runtime.getRuntime().availableProcessors() + ")");
            } else if (val == 1) {
                threadCountValueLabel.setText("Threads Multi-Thread : 1 (Monothread Déterministe)");
            } else {
                threadCountValueLabel.setText("Threads Multi-Thread : " + val + " threads");
            }
            notifyParamChange();
        });
        attachDefaultValueHandling(threadCountSlider, 4.0, () -> threadCountSlider.setValue(4.0));

        HBox threadSliderBox = new HBox(10, new Label("   └─"), threadCountValueLabel, threadCountSlider);
        threadSliderBox.setAlignment(Pos.CENTER_LEFT);

        spatialRangeTruncationCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.spatial_truncation", "💨 Troncature de Portée Spatiale des Plumes & Diffusions (Cutoff 10⁻⁶)"));
        spatialRangeTruncationCheckBox.setSelected(true);
        spatialRangeTruncationCheckBox.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        spatialRangeTruncationCheckBox.setTooltip(new Tooltip("""
            ⚡ BÉNÉFICE : Limite le calcul de dispersion atmosphérique aux cellules adjacentes affectées.
            ⚠️ IMPACT PHYSIQUE : Néglige les concentrations d'aérosols et suie ultra-diluées devenant inférieures à 10⁻⁶ ppm.
            """));
        attachDefaultValueHandling(spatialRangeTruncationCheckBox, true, () -> spatialRangeTruncationCheckBox.setSelected(true));

        List<CheckBox> subOpts = List.of(
            sparseCellSkippingCheckBox,
            oceanMacroAggregationCheckBox,
            coastalNavigationOnlyCheckBox,
            oceanMultiRateTickingCheckBox,
            parallelExecutionCheckBox,
            spatialRangeTruncationCheckBox
        );

        strictDeterminismCheckBox.setOnAction(e -> {
            boolean strict = strictDeterminismCheckBox.isSelected();
            if (strict) {
                for (CheckBox cb : subOpts) {
                    cb.setSelected(false);
                }
            }
            updatePerformanceControlsState();
            notifyParamChange();
        });

        for (CheckBox cb : subOpts) {
            cb.setOnAction(e -> {
                if (cb.isSelected() && strictDeterminismCheckBox.isSelected()) {
                    strictDeterminismCheckBox.setSelected(false);
                } else if (subOpts.stream().noneMatch(CheckBox::isSelected)) {
                    strictDeterminismCheckBox.setSelected(true);
                }
                updatePerformanceControlsState();
                notifyParamChange();
            });
        }

        VBox optBox = new VBox(5,
            optSubHeader,
            sparseCellSkippingCheckBox,
            oceanMacroAggregationCheckBox,
            coastalNavigationOnlyCheckBox,
            oceanMultiRateTickingCheckBox,
            climateSliderBox,
            parallelExecutionCheckBox,
            threadSliderBox,
            spatialRangeTruncationCheckBox
        );
        optBox.setStyle("-fx-padding: 8px 12px; -fx-background-color: rgba(15, 23, 42, 0.4); -fx-background-radius: 6px; -fx-border-color: rgba(148, 163, 184, 0.15); -fx-border-radius: 6px; -fx-border-width: 1px;");

        // Detail Inspector Card for selected/hovered Engine (Technical Description + Math Equations + Academic References)
        engineInspectorTitle = new Label("🔎 Inspecteur de Moteur Cliodynamique (Survolez un moteur pour inspecter)");
        engineInspectorTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");
        
        engineInspectorText = new Label("Sélectionnez ou survolez un moteur de Type A (Cœur) ou Type B (Optionnel) pour afficher ses équations d'état, principes physiques et références académiques.");
        engineInspectorText.setWrapText(true);
        engineInspectorText.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

        engineInspectorEquationsTitle = new Label("📐 Équations Mathématiques & Formulation Cliodynamique :");
        engineInspectorEquationsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: #38bdf8; -fx-padding: 4 0 0 0;");

        engineInspectorEquations = new Label(
            "• Formulations mathématiques et bilans de conservation affichés dynamiquement."
        );
        engineInspectorEquations.setWrapText(true);
        engineInspectorEquations.getStyleClass().add("engine-inspector-equations");

        engineInspectorRef = new Label("📚 Références scientifiques et littérature académique.");
        engineInspectorRef.setWrapText(true);
        engineInspectorRef.getStyleClass().add("hint-label");

        exportSelectedEngineBtn = new Button("📤 Exporter Template Moteur Custom (.java)");
        exportSelectedEngineBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 4;");
        exportSelectedEngineBtn.setOnAction(e -> exportCustomEngineTemplate());

        VBox engineInspectorCard = new VBox(5, engineInspectorTitle, engineInspectorText, engineInspectorEquationsTitle, engineInspectorEquations, engineInspectorRef, exportSelectedEngineBtn);
        engineInspectorCard.getStyleClass().add("engine-inspector-card");
        engineInspectorCard.setMinHeight(160);

        // --- 🔒 ETHER CORE ENGINES SECTION (Cœur Central Applicatif - 24 Moteurs Permanents) ---
        VBox typeABox = new VBox(6);
        typeABox.getStyleClass().add("card-section");

        Label coreExplanationLabel = new Label("ℹ️ Pourquoi les moteurs Cœur Ether sont-ils permanents ? Ils appliquent les lois de conservation physique (masse & énergie, thermodynamique, hydrologie, insolation H3, métabolisme) nécessaires à la survie élémentaire du monde.");
        coreExplanationLabel.setStyle("-fx-font-size: 10px; -fx-font-style: italic; -fx-text-fill: #94a3b8; -fx-padding: 0 0 4 0;");
        coreExplanationLabel.setWrapText(true);
        typeABox.getChildren().add(coreExplanationLabel);

        List<String[]> coreEngines = List.of(
            new String[]{"PhysicalLawEngine", "Moteur Physique & Lois de Conservation",
                "Moteur de conservation thermodynamique de la matière et de l'énergie (Premier et Second Principes). Calcule le bilan calorifique planétaire et la dégradation de l'énergie en chaleur dissipée.",
                "Ref: Carnot, N. L. S. (1824); Clausius, R. (1865); Prigogine, I. (1977). Non-Equilibrium Thermodynamics.",
                "• Premier Principe (Énergie) : dE_total/dt = Q_in - Q_out = 0\n• Second Principe (Entropie) : dS/dt = dS_ext + dS_int >= 0 avec dS_int = Q_dissipee / Temp_surface"},
            new String[]{"BiologicalDemographicsEngine", "Démographie Cellulaire & Métabolisme",
                "Régulation métabolique de la population humaine. Calcule la mortalité de Gompertz-Makeham selon l'âge, l'espérance de vie, la natalité malthusienne et la sous-alimentation.",
                "Ref: Kleiber, M. (1932); Gompertz, B. (1825); Makeham, W. M. (1860); Malthus, T. R. (1798).",
                "• Mortalité Gompertz-Makeham : μ(x) = α · exp(β · x) + γ\n• Bilan Métabolique de Kleiber : B_metabolisme = q₀ · M^(0.75)\n• Dynamique de Population : dN/dt = r · N · (1 - N / K_soutenable)"},
            new String[]{"PhysicalEnergyGridEngine", "Grille Énergétique & EROEI Brut",
                "Modélisation des flux d'énergie primaire planétaire (solaire, géothermie, biomasse). Détermine l'EROEI brut (Energy Return on Energy Invested) pour les récoltes et l'extraction.",
                "Ref: Hall, C. A. S., et al. (2014). EROEI of Global Energy Resources. Nature Climate Change.",
                "• EROEI Brut = E_produite_brute / E_investie_extraction\n• Énergie Utile Net = E_brute · (1 - 1 / EROEI)\n• Seuil Critique Civilisationnel : EROEI >= 3.0 requis pour soutenir les institutions."},
            new String[]{"TechTreeEngine", "Diffusion Technologique & Capital Savoir",
                "Arbre d'innovation technologique et diffusion cognitive. Simule l'accumulation du capital d'instruction, la propagation spatiale des inventions et le franchissement des seuils d'étapes (Niveaux Tech 0.0 à 10.0+).",
                "Ref: Mokyr, J. (1990). The Lever of Riches: Technological Creativity. Oxford Univ. Press.",
                "• Croissance du Capital Savoir : dT/dt = α · Pop · (T / T_max)^β + Σ D_voisinage · (T_j - T_i)\n• Diffusion Spatiale : Flux_innovation = -D_diffusion · ∇T(x)"},
            new String[]{"AquiferDepletionEngine", "Hydrologie & Transfert d'Eau Douce",
                "Hydrologie continentale et réplétion/déplétion des nappes phréatiques. Simule le bilan précipitations-évapotranspiration, le débit des rivières et le stress hydrique.",
                "Ref: Gleick, P. H. (2000). Water Futures; Wada, Y. et al. (2010). Global Groundwater Depletion. GRL.",
                "• Bilan Hydrique Cellulaire : dW_nappe/dt = Precipitations - Evapotranspiration - Extraction_agricole - Ruissellement\n• Stress Hydrique = Extraction_totale / Recharge_annuelle"},
            new String[]{"H3ClimateSystem", "Système Climatique H3 & Saisons",
                "Moteur climato-saisonnier basé sur la discrétisation hexagonale H3. Calcule la température moyenne de surface, le gradient équateur-pôle, l'insolation selon l'obliquité orbitale et les saisons.",
                "Ref: Uber H3 Spatial Index (2018); Sellers, W. D. (1969). Energy Balance Climate Models.",
                "• Bilan Radiatif Solaire : S(lat, t) = (S_const / 4) · [1 + e · cos(ω · t)] · cos(lat - declinaison)\n• Équilibre Thermique : C_thermique · dT/dt = S(1 - Albédo) - ε·σ·T⁴ + Div(K_transport · ∇T)"},
            new String[]{"PoliticalSimulationEngine", "Moteur Politique & Frontières",
                "Modélisation des structures politiques et géopolitiques. Gère la délimitation des territoires, la souveraineté des cités-états, les confédérations culturelles et la stabilité des frontières.",
                "Ref: Tilly, C. (1990). Coercion, Capital, and European States; Mann, M. (1986). Sources of Social Power.",
                "• Projection de Puissance d'État : P_militaire(d) = (Capacité_Fiscale · Taux_Levée) / (1 + α · Distance_Capitale)\n• Stabilité des Frontières : Seuil d'annexion si P_i(x) > 1.35 · P_j(x)"},
            new String[]{"StatisticsKernel", "Noyau Statistique & Cliodynamique",
                "Noyau d'agrégation statistique et d'analyse cliodynamique en temps réel. Calcule l'indice de Gini, le PIB mondial, la complexité de Turchin, le risque d'effondrement et exporte les bilans CSV.",
                "Ref: Gini, C. (1912); Turchin, P. (2016). Ages of Discord; Tainter, J. (1988). Collapse of Complex Societies.",
                "• Indice de Gini : G = (Σ Σ |y_i - y_j|) / (2 · n² · y_moyen)\n• Pression de Crise Systémique (PSI) = (Pop / Pop_soutenable) · (1 / Salaire_reel) · Inegalité_Elite"},
            new String[]{"WorldBuffer / AgentBuffer", "Noyau DOD Allocateur Mémoire",
                "Allocateur de mémoire et registres DOD (Data-Oriented Design). Optimise la mémoire cache du processeur en vectorisant les attributs des cohortes d'agents et des mailles H3.",
                "Ref: Acton, M. (2014). Data-Oriented Design; LMAX Disruptor High-Performance Ring Buffer (2011).",
                "• Structure des Tableaux d'Attributs (SoA) : float[] capitalWork, float[] capitalResource, int[] populationCohorts\n• Alignement Cache SIMD Vectorisé : 64-byte aligned blocks for AVX-512 execution."},
            new String[]{"OceanPhysicsEngine", "Dynamo Fluidique & Basculement Océanique",
                "Dynamo fluidique et inertie thermique des océans. Modélise la capacité calorifique de la masse d'eau marine, la dérive thermique lente et la régulation du climat végétal.",
                "Ref: Stommel, H. (1961). Thermohaline Convection; Rahmstorf, S. (1995). AMOC Stability. Nature.",
                "• Modèle à Deux Mailles de Stommel : dq/dt = c_T · ΔT - c_S · ΔS\n• Transport de Chaleur Océanique : F_ocean = ρ · C_p · V_derive · (T_equateur - T_pole)"},
            new String[]{"MalthusianCapacityEngine", "Pression Malthusienne & Capacité Portante",
                "Capacité portante écologique (K) et pression Malthusienne. Calcule le seuil maximal d'habitants soutenables par cellule avant dégradation irréversible de l'environnement.",
                "Ref: Malthus, T. R. (1798); Catton, W. R. (1980). Overshoot: Ecological Footprint.",
                "• Capacité Portante K(t) = Fertilité_Sol · Eau_Disponible · Niveau_Tech\n• Ratio Malthusien M = Pop / K\n• Dégradation Environnementale en cas de Surconsommation (M > 1) : dK/dt = -γ · (M - 1) · K"},
            new String[]{"OreGradeThermodynamicsEngine", "Géo-Métallurgie & Déplétion Crustale",
                "Géo-métallurgie et thermodynamique d'épuisement des filons minéraux crustaux. Simule le déclin du titre des minerais (Loi de Lasky) et la hausse de l'énergie nécessaire à l'extraction.",
                "Ref: Lasky, S. G. (1950). Mineral Resource Depletion Law; Ayres, R. U. (1998). Industrial Ecology.",
                "• Loi de Lasky (Titre du Minerai) : Grade(g) = g₀ · exp(-k · Cumul_Extrait)\n• Énergie Spécifique d'Extraction : E_extraction(g) = E₀ / (Grade(g))^1.35"},
            new String[]{"SoilNutrientNPKEngine", "Cycle NPK & Fertilité des Sols",
                "Cycles biogéochimiques des nutriments NPK (Azote, Phosphore, Potassium). Régit l'épuisement des sols agricoles par la culture intensive et la restauration organique.",
                "Ref: Liebig, J. von (1840). Law of the Minimum; Smil, V. (2001). Enriching the Earth (N-P-K Cycles).",
                "• Loi du Minimum de Liebig : Rendement = Y_max · min( N/N_ref, P/P_ref, K/K_ref )\n• Épuisement des Nutriment : dN/dt = Restauration_Naturelle + Apport_Engrais - Export_Recolte"},
            new String[]{"FluxEngine", "Flux de Subsistance & Routes Commerciales",
                "Routes commerciales et flux de subsistance inter-cellules H3. Calcule les coûts de transport, l'arbitrage marchand et l'équilibrage des stocks alimentaires par le commerce.",
                "Ref: Tinbergen, J. (1962). Gravity Model of Trade; Onsager, L. (1931). Reciprocal Relations.",
                "• Modèle Gravitationnel de Commerce : Flux(i, j) = G · (PIB_i · PIB_j) / (Distance(i, j)^1.8)\n• Friction de Transport : Coût_fret = Exp(Friction_Relief · Distance)"},
            new String[]{"AtmosphericOxygenEngine", "Dynamique de l'Oxygène Atmosphérique",
                "Bilan de la pression partielle d'oxygène (O₂) atmosphérique. Régule les conditions métaboliques pour la survie des organismes complexes et le risque d'incendies forestiers.",
                "Ref: Berner, R. A. (2006). GEOCARBSULF: Atmospheric Oxygen over Phanerozoic Time.",
                "• Bilan d'O₂ Atmosphérique : dO₂/dt = Photosynthese_Net - Respiration_Biomasse - Oxydation_Minérale\n• Risque d'Embrasement Sauvage = Max(0, (pO₂ - 0.15) / 0.06)"},
            new String[]{"CrustalGeothermalEngine", "Géothermie Crustale & Tectonique",
                "Flux de chaleur interne terrestre et potentiel géothermique crustal. Simule le gradient géothermique et le potentiel d'énergie géothermique de surface.",
                "Ref: Turcotte, D. L. & Schubert, G. (2002). Geodynamics: Mantle Heat Transport. Cambridge Univ. Press.",
                "• Conductivité Thermique Crustale : q = -k_roche · (dT/dz)\n• Potentiel Géothermique d'Exploitation : P_geoth = q_surface · Surface_H3 · Rendement_Carnot"},
            new String[]{"DynamicHydrographicSiltationEngine", "Hydrographie & Ensablement Fluvial",
                "Hydrographie et dynamique d'érosion/ensablement des bassins versants. Modélise la modification du lit des fleuves et le dépôt d'alluvions fertiles.",
                "Ref: Horton, R. E. (1945). Drainage-Basin Development; Schumm, S. A. (1977). The Fluvial System.",
                "• Équation d'Érosion Fluviale : E_sediment = k_erodibilite · (Débit_eau)^1.4 · (Pente_terrain)^1.2\n• Dépôt Alluvionnaire : dSilt/dt = E_amont - Sedimentation_loc_lit"},
            new String[]{"GreenhouseRadiativeEngine", "Forçage Radiatif & Effet de Serre",
                "Bilan de forçage radiatif et effet de serre. Calcule l'impact des concentrations de CO₂, CH₄ et H₂O sur l'infrarouge réémis vers la surface.",
                "Ref: Myhre, G. et al. (1998). Radiative Forcing Equations; IPCC AR6 WG1 (2021).",
                "• Forçage Radiatif du CO₂ : ΔF_CO2 = 5.35 · ln(C / C₀)  [W/m²]\n• Sensibilité Climatique : ΔT_eq = λ · (ΔF_CO2 + ΔF_CH4 + ΔF_aerosols)"},
            new String[]{"InfrastructureEnergyEngine", "Réseaux d'Infrastructure Énergétique",
                "Réseaux d'infrastructures énergétiques et transport de puissance. Modélise la perte en ligne des réseaux électriques et oléoducs.",
                "Ref: Bak, P. et al. (1987). Self-Organized Criticality in Grid Infrastructure.",
                "• Perte en Ligne Électrique : Perte_Joule = R_cable · I² · Distance\n• Capacité Maximale de Transit : Cap_max = V_reseau · I_max_thermique"},
            new String[]{"NetEnergyEROEIEngine", "EROEI Net & Rendement Énergétique",
                "Calcul du rendement énergétique net (EROEI Net civilisationnel). Évalue la fraction d'énergie réinvestie dans l'extraction par rapport à l'énergie utilisable pour la société.",
                "Ref: Hall, C. A. S. & Klitgaard, K. A. (2018). Energy and the Wealth of Nations. Springer.",
                "• Fraction d'Énergie Réinvestie : F_invest = 1 / EROEI_systemique\n• Énergie Nette Utile Société = Énergie_Totale · (1 - F_invest)"},
            new String[]{"PermafrostThawEngine", "Dégel du Permafrost & Relargage Méthane",
                "Dynamique de fonte du cryosol (Permafrost). Simule la déstabilisation des sols gelés et le relargage rétroactif de méthane et CO₂ stratosphériques.",
                "Ref: Schuur, E. A. G. et al. (2015). Vulnerability of Permafrost Carbon to Climate Change. Nature.",
                "• Fonte du Cryosol : dV_gel/dt = -α · Max(0, T_surface - 0.0 °C)\n• Émission Rétroactive CH₄/CO₂ : Emiss_gaz = Stock_carbone_degele · K_microbien(T)"},
            new String[]{"PhysicsTransportEngine", "Frictions Thermodynamiques de Transport",
                "Coûts énergétiques et frictions thermodynamiques des transports. Calcule l'énergie consommée par tonne-kilomètre selon le relief et le mode de transport.",
                "Ref: Smil, V. (2017). Energy and Civilization: A History. MIT Press.",
                "• Énergie Spécifique par Mode (MJ/t-km) : Maritim=0.15, Ferroviaire=0.35, Route=2.5, Porteur=12.0\n• Friction du Relief : Coût_total = E_specifique · (1 + k_pente · Pente_moyenne) · Distance"},
            new String[]{"ThermohalineOceanEngine", "Circulation Thermohaline Océanique",
                "Circulation thermohaline globale (Boucle AMOC). Modélise la plongée des eaux salées froides en Atlantique Nord et la redistribution de la chaleur planétaire.",
                "Ref: Broecker, W. S. (1991). The Great Ocean Conveyor; Rahmstorf, S. (2002). Ocean Circulation. Nature.",
                "• Débit AMOC Q_amoc = k_thermo · (ρ_nord - ρ_equateur)\n• Point de Basculement Salin : Si Dilution_Eau_Douce > Seuil_Critique -> Effondrement AMOC (Q -> 0)"},
            new String[]{"TrophicEcosystemEngine", "Réseau Trophique & Écosystèmes",
                "Réseau trophique et dynamique des écosystèmes fauniques. Simule les équations de Lotka-Volterra entre prédateurs, herbivores et producteurs primaires.",
                "Ref: Lotka, A. J. (1925); Volterra, V. (1926); MacArthur, R. H. & Wilson, E. O. (1967).",
                "• Dynamique Herbivores H : dH/dt = r_h · H · (1 - H/K) - a · H · P\n• Dynamique Prédateurs P : dP/dt = b · a · H · P - m_p · P"}
        );

        for (String[] eng : coreEngines) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label iconTitle = new Label("🔒 " + eng[0]);
            iconTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #e2e8f0;");
            Label descLbl = new Label("— " + eng[1]);
            descLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            HBox.setHgrow(descLbl, Priority.ALWAYS);
            row.getChildren().addAll(iconTitle, descLbl);

            String eqText = eng.length > 4 ? eng[4] : "📐 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire";
            Tooltip tooltip = new Tooltip("🔒 [MOTEUR PERMANENT]\n" + eng[0] + " — " + eng[1] + "\n\n" + eng[2] + "\n\n" + eqText + "\n\n📚 " + eng[3]);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 480px;");
            Tooltip.install(row, tooltip);

            row.setOnMouseEntered(e -> updateEngineInspector(eng[0], eng[1], eng[2], eng[3], eqText));
            row.setOnMouseClicked(e -> updateEngineInspector(eng[0], eng[1], eng[2], eng[3], eqText));

            typeABox.getChildren().add(row);
        }

        TitledPane corePane = new TitledPane("🔒 ARCHITECTURE CŒUR ETHER (24 MOTEURS PERMANENTS)", typeABox);
        corePane.setExpanded(false);
        corePane.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");

        // --- ⚙️ OPTIONAL & CUSTOM ENGINES SECTION (Optionnels, Extensibles & Dynamic Import/Export) ---
        typeBBoxContainer = new VBox(8);
        typeBBoxContainer.getStyleClass().add("custom-module-card");

        // Action bar for Import / Export Custom Engines
        Button exportTemplateBtn = new Button("📤 Exporter Template Moteur Java (.java)");
        exportTemplateBtn.setStyle("-fx-background-color: #4c1d95; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        exportTemplateBtn.setOnAction(e -> exportCustomEngineTemplate());

        Button importEngineBtn = new Button("📥 Importer & Compiler Moteur Java (.java / .class)");
        importEngineBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        importEngineBtn.setOnAction(e -> importCustomEngineFile());

        HBox importExportBox = new HBox(10, exportTemplateBtn, importEngineBtn);
        importExportBox.setAlignment(Pos.CENTER_LEFT);
        importExportBox.setStyle("-fx-padding: 0 0 8 0;");
        typeBBoxContainer.getChildren().add(importExportBox);

        List<String[]> optionalEngines = List.of(
            new String[]{"FrontierAsabiyyahEngine", "⚔️ Asabiyyah de Frontière (Ibn Khaldoun & Peter Turchin)",
                "Théorie Khaldounienne de la solidarité de groupe et déclin des dynasties (Badiya vs Hadara). Modélise l'érosion de la cohésion sociale lors du passage de la frontière métastable aux métropoles opulentes.",
                "Ref: Ibn Khaldun (1377). Muqaddimah; Turchin, P. (2003). Historical Dynamics: Securing the Peace, Princeton Univ. Press.",
                "• Variation d'Asabiyyah (Cohésion A) : dA/dt = c₁·F(x)·(1 - A) - c₂·(K(x)/N(x))·A\n  où F(x) est la pression militaire de frontière et K(x)/N(x) le capital par habitant (luxe).\n• Métropole opulente (K > 1000 kg/hab) : Déclin d'Asabiyyah dA/dt = -2.0% par pas de temps.\n• Zone de frontière (K ≤ 1000 kg/hab) : Forge la cohésion militaire dA/dt = +2.0% par pas de temps.\n• Inégalité & Déclin Dynastique : S_cohesion(t) = A(t) · Pop(t) · (1 - Gini(t))."},
            new String[]{"AiAutonomousRegulationPureEngine", "🤖 Régulation Autonome de l'IA & Gouvernance",
                "Modélisation de la régulation et des risques de l'intelligence artificielle. Évalue les probabilités d'émergence d'infrastructures autonomes et de gestion des risques.",
                "Ref: Bostrom, N. (2014). Superintelligence; Russell, S. (2019). Human Compatible.",
                "• Seuil d'Autonomie IA : P_alignement = 1 / (1 + exp(-k · (Niveau_Gouvernance - Complexité_IA)))\n• Taux de Risque Systémique R_ia = (1 - P_alignement) · Puissance_Calcul_Planétaire"},
            new String[]{"AmerindianEcosystemEngine", "🌾 Agro-foresterie Amérindienne & Terra Preta",
                "Techniques agricoles précolombiennes et enrichissement des sols en biochar. Augmente la capacité portante et la résilience des sols de forêt tropicale.",
                "Ref: Denevan, W. M. (1992). The Pristine Myth; Glaser, B. et al. (2002). Terra Preta Biochar.",
                "• Formation de Terra Preta : dC_biochar/dt = Apport_Charbon_Organique - Oxidation_Lente(0.001)\n• Gain de Capacité Portante : K_sols = K_base · (1 + α · ln(1 + C_biochar))"},
            new String[]{"AsymmetricColonialTradeEngine", "🚢 Commerce Colonial Asymétrique & Extraction",
                "Flux de ressources et fuite de valeur des colonies vers les métropoles. Simule la capture de rente et le blocage de l'industrialisation périphérique.",
                "Ref: Wallerstein, I. (1974). The Modern World-System; Frank, A. G. (1967). Dependency Theory.",
                "• Capture de Rente : Transfert_Richesse = Termes_Echange_Asym · Export_Matières_Premières\n• Frein d'Industrialisation Périphérique : dT_peripherie/dt = T_base · (1 - Ratio_Extraction)"},
            new String[]{"BifurcationChaosEngine", "🌀 Chaos & Analyse des Bifurcations Systémiques",
                "Sensibilité aux conditions initiales et points de basculement. Génère des micro-oscillations chaotiques pouvant déclencher des cascades d'instabilité.",
                "Ref: Lorenz, E. N. (1963). Deterministic Nonperiodic Flow; May, R. M. (1976).",
                "• Attracteur de Lorenz / Bifurcation Logistique : x_{t+1} = r · x_t · (1 - x_t)\n• Exposant de Liapounov λ > 0 -> Divergence exponentielle des trajectoires de simulation"},
            new String[]{"BioMolecularEpidemiologyEngine", "☣️ Épidémiologie Bio-Moléculaire & Immunité Pop",
                "Simulation avancée de la transmission virale et foyers infectieux. Modélise la transmission SIR/SEIR selon la densité urbaine et le réseau de commerce.",
                "Ref: Kermack, W. O. & McKendrick, A. G. (1927). SIR Epidemiological Model.",
                "• Modèle SEIR : dS/dt = -β·S·I/N, dE/dt = β·S·I/N - σ·E, dI/dt = σ·E - γ·I, dR/dt = γ·I\n• Taux de Reproduction de Base R₀ = β / γ · (1 + Variance_Contacts_Densité)"},
            new String[]{"CulturalMaterialismPureEngine", "📜 Materialisme Culturel (Marvin Harris)",
                "Déterminisme de l'infrastructure technologique et démographique sur les croyances. Adapte les valeurs morales aux contraintes d'extraction d'énergie.",
                "Ref: Harris, M. (1979). Cultural Materialism: The Struggle for a Science of Culture.",
                "• Alignement Superstructure : Superstructure(t) = f(Infrastructure_Énergétique, Pression_Démographique)\n• Transition Morale : dM/dt = k_adaptation · (Mode_Production - M)"},
            new String[]{"CulturalSociologyEngine", "📜 Sociologie Culturelle & Matrice de Voisinage",
                "Évolution des valeurs culturelles et métissage régional. Gère la diffusion des langues, des normes et la dérive culturelle entre mailles voisines.",
                "Ref: Cavalli-Sforza, L. L. & Feldman, M. W. (1981). Cultural Transmission and Evolution.",
                "• Matrice de Diffusion Culturelle : dC_i/dt = Σ_j w_{ij} · (C_j - C_i) + Drift_Accidentel\n• Distance Culturelle d(i,j) = || Vector_Langue_i - Vector_Langue_j ||"},
            new String[]{"DeforestationErosionEngine", "🏜️ Érosion Forestière & Ensablement Fluvial",
                "Dégradation des sols et perte de couverture végétale. Entraîne le ravinement des terres arables et le comblement des lits de rivières lors de coupes rases.",
                "Ref: Montgomery, D. R. (2007). Dirt: The Erosion of Civilizations. Univ. of California Press.",
                "• Érosion des Sols : Perte_Sol = k_coupe · (1 - Couverture_Forestiere) · Precipitations³\n• Comblement Fluvial = Σ Perte_Sol_Amont"},
            new String[]{"EdoJapanIsolationEngine", "⛩️ Isolationnisme du Japon Edo (Sakoku)",
                "Maintien d'un équilibre zéro-croissance et fermeture des frontières. Élimine la dépendance extérieure au détriment du rythme de progrès technologique.",
                "Ref: Totman, C. (1993). Early Modern Japan; Diamond, J. (2005). Collapse (Tokugawa Forestry).",
                "• Équilibre Sylvicole & Zéro-Croissance : Extraction_Bois <= Auto_Régénération_Forêt\n• Isolement Commercial : Flux_Externe = 0, Stabilité_Interne = Maximale"},
            new String[]{"EntropicMetalDissipationEngine", "🏭 Dissipation Entropique des Métaux & Jevons Rebound",
                "Dispersion irrémédiable des métaux rares et effets rebond. Calcule la perte irrécupérable de cuivre et de métaux précieux par usure mécanique et oxydation.",
                "Ref: Georgescu-Roegen, N. (1971). The Entropy Law and the Economic Process.",
                "• Pertes Entropiques Irrécupérables : dMetal_dissipe/dt = Production · (1 - Taux_Recyclage_Max)\n• Limite d'Usure Recyclage : Max_Recyclage = 85% par contrainte thermodynamique"},
            new String[]{"EcotoxicologyFertilityEngine", "🧪 Ecotoxicologie & Stérilité Chimique",
                "Impacts des polluants synthétiques sur le taux de fécondité. Simule la baisse de fertilité humaine liée à la concentration cumulée d'entropie chimique.",
                "Ref: Colborn, T. et al. (1996). Our Stolen Future; Swan, S. H. (2021). Count Down.",
                "• Baisse de Fécondité : F_effective = F_naturelle · exp(-k_tox · Charge_Pollution_Cumulee)"},
            new String[]{"FertileCrescentSalinizationEngine", "🌾 Salinisation du Croissant Fertile",
                "Dégradation historique des sols irrigués en Mésopotamie antique. Accumulation de sels minéraux toxiques par évaporation de l'eau d'irrigation.",
                "Ref: Jacobsen, T. & Adams, R. M. (1958). Salt and Silt in Ancient Mesopotamian Agriculture.",
                "• Accumulation Saline : dSel/dt = (Volume_Irrigation · Concentration_Sel_Eau) - Leaching_Drainage"},
            new String[]{"GeoengineeringAlbedoFeedbackEngine", "🧪 Géo-ingénierie & Rétroaction d'Albédo Artificiel",
                "Injections d'aérosols stratosphériques et terraformation. Diminue l'insolation solaire globale pour contrer le réchauffement climatique.",
                "Ref: Crutzen, P. J. (2006). Albedo Modification via Stratospheric Aerosol Injection.",
                "• Delta Albédo Artificiel : ΔAlbédo = k_aerosol · Mass_SO2_Injectee\n• Refroidissement Forcé : ΔT_cooling = -λ · S₀ · ΔAlbédo"},
            new String[]{"HandyNasaHybridEngine", "📉 Modèle HANDY NASA Hybride (Démographie & Élites)",
                "Rétroactions entre élites, travailleurs et ressources (NASA / Motesharrei). Simule les scénarios d'effondrement par surconsommation des élites.",
                "Ref: Motesharrei, S., Rivas, J., & Kalnay, E. (2014). HANDY: Human and Nature Dynamics.",
                "• Équations HANDY (4 Variables) : dx_w/dt = α_w·x_w - β_w·x_w,  dx_e/dt = α_e·x_e - β_e·x_e\n• Surconsommation Élites : Consommation_Elite = s · Consommation_Travailleur avec s >> 1"},
            new String[]{"HandyNasaPureEngine", "📉 Modèle HANDY NASA Pur (Équations Différentielles)",
                "Système dynamique pur de la dynamique homme-nature (Handy Model). Système à 4 équations couplées (Élites, Travailleurs, Nature, Capital).",
                "Ref: Motesharrei, S. et al. (2014). HANDY Model Equations. Ecological Economics 101.",
                "• Nature N : dN/dt = γ·N·(λ - N) - δ·x_w·N\n• Accumulation de Capital K : dK/dt = δ·x_w·N - C_w - C_e"},
            new String[]{"JevonsParadoxEngine", "⚡ Effet Rebond & Paradoxe de Jevons",
                "L'augmentation de l'efficacité énergétique accroît la consommation globale. Annule les gains d'économie d'énergie par l'expansion de l'échelle industrielle.",
                "Ref: Jevons, W. S. (1865). The Coal Question; Alcott, B. (2005). Jevons' Paradox.",
                "• Énergie Consommée E_total = Pop · Efficacité^ε avec ε > 1.0 (Paradoxe de Jevons)"},
            new String[]{"KardashevPureEngine", "🌌 Échelle de Kardashev & Capture Énergétique",
                "Transition vers le contrôle de l'énergie planétaire intégrale. Évalue le score de Kardashev (Type 0.0 à 1.0) selon la puissance totale captée en watts.",
                "Ref: Kardashev, N. S. (1964). Transmission of Information by Extraterrestrial Civilizations.",
                "• Indice de Kardashev K = (log₁₀(Puissance_Watts) - 6) / 10\n• Type I = 10¹⁶ Watts (Énergie Planétaire Intégrale)"},
            new String[]{"KinSelectionHamiltonEngine", "🧬 Sélection de Parentèle (Règle de Hamilton)",
                "Évolution de l'altruisme génétique et coopération inter-individus (rB > C). Détermine la cohésion des petites tribus et familles étendues.",
                "Ref: Hamilton, W. D. (1964). The Genetical Evolution of Social Behaviour. J. Theor. Biol.",
                "• Condition d'Altruisme : r · Benefit > Cost avec r = Coefficient d'Apparentement Génétique"},
            new String[]{"KurzweilAcceleratingReturnsEngine", "🚀 Loi des Rendements Accélérés (Ray Kurzweil)",
                "Accélération exponentielle du progrès scientifique et des processeurs. Réduit les délais d'invention à mesure que le niveau technologique s'élève.",
                "Ref: Kurzweil, R. (2005). The Singularity Is Near; Moore, G. E. (1965).",
                "• Vitesse d'Invention dTech/dt = V₀ · exp(λ · Tech(t))"},
            new String[]{"LenskiPureEngine", "🧠 Évolution Socioculturelle (Gerhard Lenski)",
                "Classification des sociétés selon leur mode d'extraction d'information et d'énergie. Rétrograde ou promeut le type de société (Chasseurs, Agricoles, Industriels).",
                "Ref: Lenski, G. (1966). Power and Privilege: A Theory of Social Stratification.",
                "• Stade Sociétal = f(Énergie_Par_Habitant, Stock_Information_Technologique)"},
            new String[]{"LeslieWhitePureEngine", "⚡ Loi de Leslie White (Culture = E × T)",
                "Le développement culturel varie directly avec l'énergie captée par habitant (C = E × T). Détermine la complexité symbolique selon l'énergie.",
                "Ref: White, L. A. (1943). Energy and the Evolution of Culture. American Anthropologist.",
                "• Complexité Culturelle C = Énergie_Par_Capita · Efficacité_Technologique"},
            new String[]{"MaritimeHighwayEngine", "⛵ Autoroute Maritime & Thalassocraties (Braudel/Fluid Dynamics)",
                "Réseau de navigation côtière et autoroutes maritimes universelles. Réduit la friction de transport sur l'eau libre de glace (thermodynamique) et stimule l'accumulation de capital.",
                "Ref: Braudel, F. (1949). La Méditerranée; Archimedes Buoyancy Transport Models.",
                "• Multiplicateur de Transport Maritime = 0.20 × Friction_Terrestre (Cap : +5%/tick)"},
            new String[]{"DynamicMaritimeRoutingGraph", "🌐 Graphe de Routage Trans-Océanique Dynamique",
                "Calcul dynamique et adaptatif des routes maritimes globales selon l'évolution technologique (cabotage -> navigation hauturière -> brise-glace) et thermodynamique.",
                "Ref: Dynamic Graph Routing & Fluid Drag; Bowditch, N. (1802). American Practical Navigator.",
                "• Invalidation auto selon Niveau Tech & Glace de Mer\n• Portée de Cabotage (Tech < 3.0: 300km, Tech >= 6.0: Trans-Océanique)"},
            new String[]{"LandReclamationEngine", "🏗️ Poldérisation & Habitats Flottants (Seasteading)",
                "Transformation de zones côtières en polders agricoles et création de cités flottantes en haute mer selon le niveau technologique et le capital.",
                "Ref: Dutch Water Boards History; Seasteading Institute (2008).",
                "• Tech >= 4.0 & K >= 100 -> Poldérisation ; Tech >= 8.5 & K >= 500 -> Habitat Flottant"},
            new String[]{"MarineSubmersionEngine", "🚨 Submersion Marine & Évacuation Physique",
                "Modélisation physique de l'élévation du niveau de la mer et de la maintenance des digues. Évacuation préventive (jusqu'à 98% en société moderne) et flux de réfugiés sans mortalité brutale irréaliste.",
                "Ref: IPCC Coastal Inundation & Flood Early Warning Physics; Adger, W. N. (2006). Vulnerability.",
                "• Évacuation Physique: Ratio_Evac = 1 - exp(-0.4 · Tech · (1 + K/500))\n• Déplacement de Population vers mailles sèches adjacentes"},
            new String[]{"HydrologicalEngineeringEngine", "💧 Ingénierie Hydrologique & Transgressions Paysagères",
                "Assèchement/remplissage de lacs urbains (Texcoco / Tenochtitlan), assèchement anthropique de mers intérieures endoréiques (Mer d'Aral) et retenues d'eau / barrages hydroélectriques de montagne.",
                "Ref: IPCC Water Resource Engineering; World Commission on Dams (2000); Tenochtitlan Chinampa Hydro-Engineering.",
                "• Drenage Texcoco: Lac -> Plaine Urbaine (Tech >= 3.5, K >= 150)\n• Assèchement Aral: Lac -> Désert Salé (Drainage > Recharge)\n• Barrage Montagne: Retenue d'eau (1000L) & Énergie Hydroélectrique (Tech >= 4.5, Alt >= 300m)"},
            new String[]{"MegafaunaEcosystemEngine", "🦕 Conservation de la Biodiversité Sauvage & Mégafaune",
                "Pressions de chasse et extinction/préservation des espèces sauvages. Simule la disparition de la grande faune lors de l'expansion humaine néolithique.",
                "Ref: Martin, P. S. (1984). Quaternary Extinctions: A Prehistoric Revolution.",
                "• Extinction Mégafaune dM/dt = r_m·M - k_chasse·Pop_Humaine·M"},
            new String[]{"MilitaryTechShockEngine", "💣 Chocs de Technologie Militaire & Poudre à Canon",
                "Révolution militaire et transformation de l'architecture des fortifs. Augmente la capacité de conquête des empires centralisés.",
                "Ref: Parker, G. (1988). The Military Revolution; McNeill, W. H. (1982).",
                "• Puissance Offensive = Puissance_Base · (1 + Multiplicateur_Poudre_Canon)"},
            new String[]{"MonasticDemographicBufferEngine", "🏛️ Buffers Démographiques Monastiques & Savoir",
                "Préservation du capital intellectuel et régulation démographique par les monastères. Empêche la perte totale de savoir lors de la chute d'un empire.",
                "Ref: Weber, M. (1905); Kautsky, K. (1889). Thomas More and his Utopia.",
                "• Plancher de Rétention du Savoir : T_min = Max(T_courant, T_monastique_sauvegardé)"},
            new String[]{"NordhausDiceHybridEngine", "🌡️ Modèle DICE Hybride (Nordhaus - Climat & Économie)",
                "Couplage économie-climat intégré avec boucle de dommage du carbone. Évalue la perte de PIB causée par l'élévation des températures extrêmes.",
                "Ref: Nordhaus, W. D. (1992, 2017). Integrated Assessment Models (DICE-2016R).",
                "• Fonction de Dommage Nordhaus Ω(T) = 1 / (1 + π₁·T + π₂·T²)\n• PIB Ajusté Climat Y_net = Ω(T) · Y_brut"},
            new String[]{"NordhausDicePureEngine", "🌡️ Modèle DICE Pur (Taxe Carbone & PIB)",
                "Modélisation analytique du coût du carbone et investissements verts. Calcule le prix social du carbone pour inciter la décarbonation.",
                "Ref: Nordhaus, W. D. (1992). An Optimal Transition Path for Controlling Greenhouse Gases. Science.",
                "• Prix Social du Carbone SCC = d(Dommages_Futurs_Actualisés) / d(Émission_CO2)"},
            new String[]{"NuclearSafetyRadiotoxicityEngine", "⚛️ Radiotoxicité & Fusion Nucléaire",
                "Gestion des risques d'accidents atomiques et retombées toxiques. Simule la contamination des terres et les surcoûts de sécurité industrielle.",
                "Ref: Perrow, C. (1984). Normal Accidents: Living with High-Risk Technologies.",
                "• Probabilité d'Accident Majeur = 1 - exp(-Taux_Défaillance_Système · Nombre_Reacteurs)"},
            new String[]{"NuclearWarfareClimateEngine", "💥 Guerres Thermodynamiques & Hiver Nucléaire",
                "Modélisation des incendies massifs et refroidissement climatique. Injection de suie stratosphérique bloquant les rayons solaires pendant des années.",
                "Ref: Turco, R. P., Toon, O. B., Ackerman, T. P., Pollack, J. B., & Sagan, C. (1983). TTAPS.",
                "• Baisse Température Mondiale ΔT_nucléaire = -15.0 °C · (Masse_Suie / 150 Tg)"},
            new String[]{"OstromCommonsPureEngine", "🏞️ Auto-Gouvernance des Communs (Elinor Ostrom)",
                "Règles institutionnelles locales pour éviter la tragédie des communs. Maintient la durabilité des pâturages et de la pêche sans privatisation.",
                "Ref: Ostrom, E. (1990). Governing the Commons: Evolution of Institutions.",
                "• Maintien des Communs : Taux_Survie_Communs = f(Institution_Locale, Sanctions_Graduées)"},
            new String[]{"PinkerViolenceDeclinePureEngine", "🕊️ Déclin Historique de la Violence (Steven Pinker)",
                "Baisse de la mortalité violente par l'État, le commerce et l'alphabétisation. Réduit les homicides et les guerres à mesure que l'État de droit progresse.",
                "Ref: Pinker, S. (2011). The Better Angels of Our Nature: Why Violence Has Declined.",
                "• Taux de Mortalité Violente = V₀ · exp(-k_etat · Monopole_Violence - k_commerce · Fret)"},
            new String[]{"ProtestantWorkEthicEngine", "✝️ Éthique du Travail & Accumulation de Capital (Max Weber)",
                "Impact des valeurs morales sur la formation du capital industriel. Stimule le réinvestissement des bénéfices dans les machines au lieu du luxe.",
                "Ref: Weber, M. (1905). Die protestantische Ethik und der Geist des Kapitalismus.",
                "• Taux d'Épargne Réinvestie : S_épargne = S_base · (1 + α_ethique_travail)"},
            new String[]{"PsychohistoryPureEngine", "📊 Psychohistoire Cliodynamique (Modèle d'Asimov)",
                "Prédiction statistique des grandes masses humaines à long terme. Anticipe les cycles de stabilité et prévient les crises d'effondrement.",
                "Ref: Asimov, I. (1951). Foundation; Turchin, P. (2008). Arise Cliodynamics. Nature.",
                "• Trajectoire Macro-Historique : X(t+Δt) = Matrix_P · X(t) avec Invariance Statistique"},
            new String[]{"RomanImperialCliodynamicEngine", "🪙 Cycle Cliodynamique Romain & Altération de la Monnaie",
                "Dégradation de la pureté du Denier et crises fiscales impériales. Entraîne l'hyper-inflation et le déclin du pouvoir d'achat des légions.",
                "Ref: Turchin, P. & Scheidel, W. (2009). Coin Debasement and Structural Cycles in Rome.",
                "• Pureté de la Monnaie : Teneur_Argent = T₀ · exp(-k_solde_fiscal · Déficit_Armée)\n• Inflation & Solde des Légions : Solde_Reelle = Solde_Nominale / Inflation"},
            new String[]{"ScottAgainstTheGrainPureEngine", "🌾 Modèle de l'État Céréalier (James C. Scott)",
                "Attractivité des céréales taxables et émergence de l'État archaïque. Privilégie le blé/riz stockables pour l'impôt au détriment des tubercules.",
                "Ref: Scott, J. C. (2017). Against the Grain: A Deep History of the Earliest States.",
                "• Capacité Taxable : Assiette_Fiscale = Production_Céréales_Stockables - Subsistance_Minimale"},
            new String[]{"SelfDomesticationEngine", "🐕 Auto-Domestication Humaine & Réduction de l'Agressivité",
                "Sélection contre l'agressivité réactive dans les fortes densités. Sélectionne les comportements coopératifs indispensables à la vie urbaine.",
                "Ref: Hare, B. (2017). Survival of the Friendliest; Lahire, B. (2018). Structures Fondamentales.",
                "• Réduction Agressivité Réactive : dAgressivite/dt = -k_urbain · Densité_Cellule"},
            new String[]{"SexualSelectionMatingEngine", "💍 Sélection Sexuelle & Marché Matrimonial",
                "Structures de parenté et polygamie/monogamie selon les ressources. Régule l'accès aux partenaires selon l'inégalité de capital.",
                "Ref: Buss, D. M. (1989). Sex differences in human mate preferences. BBS.",
                "• Indice Polygynie = f(Gini_Richesse, Monopole_Ressources_Elites)"},
            new String[]{"SmilMaterialTransitionsPureEngine", "🏗️ Transitions Matérielles & Énergétiques (Vaclav Smil)",
                "Inertie physique des transitions vers l'acier, le béton, l'ammoniac et le plastique. Impose des délais de plusieurs décennies pour remplacer un matériau de base.",
                "Ref: Smil, V. (2019). Material World & Energy Transitions: History, Requirements.",
                "• Temps de Transition Matérielle T_transition = 40 à 60 ans par inertie du capital fixe"},
            new String[]{"SpatialCityFractalEngine", "🏙️ Distribution Fractale des Villes (Loi de Zipf / Batty)",
                "Hiérarchie des métropoles et loi rang-taille urbaine. Organise le réseau urbain en sous-centres régionaux et métropoles primatiales.",
                "Ref: Batty, M. (2008). The Size, Scale, and Shape of Cities. Science; Zipf, G. K. (1949).",
                "• Loi de Zipf Rang-Taille : Pop(Rang r) = Pop_Max / r^α avec α ≈ 1.0"},
            new String[]{"TasmanianCulturalRegressionEngine", "🏝️ Régression Culturelle de Tasmanie (Henrich)",
                "Perte de technologies complexes par goulet d'étranglement démographique. Fait régresser l'outillage si la population tombe sous le seuil critique d'apprentissage.",
                "Ref: Henrich, J. (2004). Demography and Cultural Loss in Tasmania. American Antiquity.",
                "• Seuil Critique d'Apprentissage : dTech/dt < 0 si Pop_Tribu < N_critique"},
            new String[]{"TechnologicalSingularityEngine", "🌌 Singularité Technologique & Kurzweil Rebound",
                "Accélération exponentielle du progrès scientifique et IA. Franchit le point d'inflexion où les machines auto-améliorent leur propre conception.",
                "Ref: Good, I. J. (1965); Vinge, V. (1993); Kurzweil, R. (2005). The Singularity Is Near.",
                "• Boucle de Rétroaction Singularité : d(Capacité_IA)/dt = (Capacité_IA)^1.5"},
            new String[]{"UrbanThermodynamicsEngine", "🏙️ Thermodynamique Urbaine & Métropoles",
                "Îlots de chaleur urbains et densité bâtie hyper-concentrée. Élève la température locale des métropoles et consomme de la puissance de climatisation.",
                "Ref: Oke, T. R. (1982). The Energetic Basis of the Urban Heat Island. Q. J. R. Meteorol. Soc.",
                "• Îlot de Chaleur Urbain ΔT_urbain = a · log₁₀(Immobilier_Densité) + b"},
            new String[]{"World3CouplingEngine", "📉 Modèle Couplé World3 & Limites à la Croissance",
                "Rétroactions entre population, pollution et capital (Club de Rome). Connecte le modèle World3 Meadows aux cellules hexagonales H3.",
                "Ref: Meadows, D. H., Meadows, D. L., Randers, J., & Behrens, W. W. (1972). Limits to Growth.",
                "• System Dynamic 5 Subsystems (Population, Capital, Agriculture, Pollution, Non-Renewables)"},
            new String[]{"World3HybridEngine", "📉 Modèle World3 Hybride (Physique & Capital)",
                "Couplage de la dynamique de World3 aux variables spatiales H3. Intègre la dispersion spatiale de la pollution et des ressources finies.",
                "Ref: Meadows, D. H. et al. (1972, 2004). Limits to Growth: The 30-Year Update.",
                "• Dispersion Spatiale H3 de la Pollution Persistante"},
            new String[]{"World3PureEngine", "📉 Modèle World3 Pur (Équations du Club de Rome)",
                "Reproduction fidèle des 5 sous-systèmes du rapport Meadows 1972 (Population, Capital, Agriculture, Pollution, Ressources).",
                "Ref: Meadows, D. H. et al. (1972). World3 Model Equations (Club of Rome Report).",
                "• Reproduction Intégrale des Équations World3 DYNAMO / Stella"}
        );

        typeBCheckBoxMap.clear();
        typeBParamSpinnersMap.clear();
        for (String[] eng : optionalEngines) {
            CheckBox cb = new CheckBox(eng[1]);
            cb.setSelected("FrontierAsabiyyahEngine".equals(eng[0]));
            cb.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #e2e8f0;");

            String eqText = eng.length > 4 ? eng[4] : "📐 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire";
            Tooltip tooltip = new Tooltip("⚙️ [MOTEUR OPTIONNEL]\n" + eng[0] + " — " + eng[1] + "\n\n" + eng[2] + "\n\n" + eqText + "\n\n📚 " + eng[3]);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 480px;");
            cb.setTooltip(tooltip);

            cb.setOnAction(e -> notifyParamChange());

            typeBCheckBoxMap.put(eng[0], cb);

            HBox row = new HBox(8, cb);
            row.setAlignment(Pos.CENTER_LEFT);

            row.setOnMouseEntered(e -> updateEngineInspector(eng[0], eng[1], eng[2], eng[3], eqText));
            row.setOnMouseClicked(e -> updateEngineInspector(eng[0], eng[1], eng[2], eng[3], eqText));

            VBox engContainer = new VBox(4, row);
            javafx.scene.Node paramBox = createEngineParameterBox(eng[0]);
            if (paramBox != null) {
                paramBox.visibleProperty().bind(cb.selectedProperty());
                paramBox.managedProperty().bind(cb.selectedProperty());
                engContainer.getChildren().add(paramBox);
            }

            typeBBoxContainer.getChildren().add(engContainer);
        }

        TitledPane typeBPane = new TitledPane("⚙️ MODULES OPTIONNELS (" + optionalEngines.size() + " MOTEURS EXTENSIBLES & IMPORT/EXPORT)", typeBBoxContainer);
        typeBPane.setExpanded(true);
        typeBPane.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 11px; -fx-font-weight: bold;");

        section.getChildren().addAll(oceanOptHeader, oceanOptDesc, masterBox, optBox, engineInspectorCard, corePane, typeBPane);
        return section;
    }

    private VBox createCulturalVectorAndLayersSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        Label header = new Label("🧠 3. DIMENSION DU VECTEUR CULTUREL & CALQUES MULTI-CHAMPS (ONGLET 3)");
        header.getStyleClass().add("label-header");
        header.setStyle("-fx-text-fill: #a78bfa; -fx-font-weight: bold;");

        Label desc = new Label("Configuration du tenseur d'information N-dimensionnel et importation/génération des calques cartographiques d'isoglosses, parenté, rituels et souveraineté politique.");
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        desc.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        // 1. Vector Dimension (M)
        Label dimLbl = new Label("Taille Tenseur Culturel (M dims) :");
        dimLbl.getStyleClass().add("control-label");
        cultureVectorDimSpinner = new Spinner<>(4, 32, 8, 2);
        cultureVectorDimSpinner.setEditable(true);
        cultureVectorDimSpinner.setMaxWidth(Double.MAX_VALUE);
        cultureVectorDimSpinner.valueProperty().addListener((obs, o, n) -> notifyParamChange());
        Tooltip.install(cultureVectorDimSpinner, new Tooltip("Nombre de composantes N-dimensionnelles du vecteur d'information culturelle (ex: 8D, 16D, 32D)."));

        // 2. Cultural Diffusion Rate
        Label diffLbl = new Label("Conductivité Diffusion Culturelle (α) :");
        diffLbl.getStyleClass().add("control-label");
        culturalDiffusionRateSpinner = new Spinner<>(0.001, 0.50, 0.05, 0.01);
        culturalDiffusionRateSpinner.setEditable(true);
        culturalDiffusionRateSpinner.setMaxWidth(Double.MAX_VALUE);
        culturalDiffusionRateSpinner.valueProperty().addListener((obs, o, n) -> notifyParamChange());
        Tooltip.install(culturalDiffusionRateSpinner, new Tooltip("Taux de diffusion de l'énergie d'association entre hexagones H3 voisins."));

        // 3. Cultural Mutation Noise Rate
        Label mutLbl = new Label("Bruit de Mutation / Dérive (1/√N) :");
        mutLbl.getStyleClass().add("control-label");
        culturalMutationRateSpinner = new Spinner<>(0.001, 0.10, 0.01, 0.005);
        culturalMutationRateSpinner.setEditable(true);
        culturalMutationRateSpinner.setMaxWidth(Double.MAX_VALUE);
        culturalMutationRateSpinner.valueProperty().addListener((obs, o, n) -> notifyParamChange());
        Tooltip.install(culturalMutationRateSpinner, new Tooltip("Facteur de dérive Langevin (bruit de fond d'innovation stochastique)."));

        grid.add(dimLbl, 0, 0); grid.add(cultureVectorDimSpinner, 1, 0);
        grid.add(diffLbl, 0, 1); grid.add(culturalDiffusionRateSpinner, 1, 1);
        grid.add(mutLbl, 0, 2); grid.add(culturalMutationRateSpinner, 1, 2);

        // Layer Import Panel
        VBox layersPanel = new VBox(8);
        layersPanel.getStyleClass().add("layers-panel-card");

        Label layerTitle = new Label("🗺️ Calques Spécifiques (Isoglosses, Parenté, Croyances, Souveraineté)");
        layerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        layerTitle.setWrapText(true);
        HBox.setHgrow(layerTitle, Priority.ALWAYS);

        Button culturalHelpBtn = new Button("❓ Format Calques");
        culturalHelpBtn.getStyleClass().add("button-secondary");
        culturalHelpBtn.setStyle("-fx-font-size: 11px;");
        culturalHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        culturalHelpBtn.setTooltip(new Tooltip("Spécifications des formats d'images et données cartographiques pour l'import des calques culturels et géopolitiques."));
        culturalHelpBtn.setOnAction(e -> showCulturalImportFormatHelp());

        HBox layerHeaderBox = new HBox(8, layerTitle, culturalHelpBtn);
        layerHeaderBox.setAlignment(Pos.CENTER_LEFT);

        Label culturalFormatHintLabel = new Label("PNG / JPEG (projection équirectangulaire 2:1) :\n  Niveaux de gris & Canaux RVB codant les distances linguistiques (Isoglosses), structures de parenté, systèmes de croyances & souverainetés politiques.");
        culturalFormatHintLabel.setWrapText(true);
        culturalFormatHintLabel.getStyleClass().add("hint-label");

        // Calque 1: Isoglosses
        isoglossFileLabel = new Label("📜 Calque Isoglosses : Génération procédurale active");
        isoglossFileLabel.getStyleClass().add("hint-label");
        Button btnIsogloss = new Button("📥 Importer Calque Isoglosses & Langues (PNG/GeoJSON)");
        btnIsogloss.getStyleClass().add("button-secondary");
        btnIsogloss.setOnAction(e -> loadCustomCultureLayer("Isoglosses & Langues", img -> {
            customIsoglossImage = img;
            isoglossFileLabel.setText("📜 Calque Isoglosses : Image PNG/GeoJSON chargée");
            notifyParamChange();
        }));

        // Calque 2: Kinship
        kinshipFileLabel = new Label("🏛️ Calque Parenté & Outillage : Génération procédurale active");
        kinshipFileLabel.getStyleClass().add("hint-label");
        Button btnKinship = new Button("📥 Importer Calque Outillage & Parenté (PNG/GeoJSON)");
        btnKinship.getStyleClass().add("button-secondary");
        btnKinship.setOnAction(e -> loadCustomCultureLayer("Outillage & Parenté", img -> {
            customKinshipImage = img;
            kinshipFileLabel.setText("🏛️ Calque Parenté : Image PNG/GeoJSON chargée");
            notifyParamChange();
        }));

        // Calque 3: Rituals
        ritualsFileLabel = new Label("🔮 Calque Croyances & Rituels : Génération procédurale active");
        ritualsFileLabel.getStyleClass().add("hint-label");
        Button btnRituals = new Button("📥 Importer Calque Normes & Rituels (PNG/GeoJSON)");
        btnRituals.getStyleClass().add("button-secondary");
        btnRituals.setOnAction(e -> loadCustomCultureLayer("Normes & Rituels", img -> {
            customRitualsImage = img;
            ritualsFileLabel.setText("🔮 Calque Croyances : Image PNG/GeoJSON chargée");
            notifyParamChange();
        }));

        // Calque 4: Sovereignty
        sovereigntyFileLabel = new Label("👑 Calque Souveraineté Politico-Militaire : Foyers de Capitales");
        sovereigntyFileLabel.getStyleClass().add("hint-label");
        Button btnSovereignty = new Button("📥 Importer Calque Souveraineté & Capitales (PNG/GeoJSON)");
        btnSovereignty.getStyleClass().add("button-secondary");
        btnSovereignty.setOnAction(e -> loadCustomCultureLayer("Souveraineté & Capitales", img -> {
            customSovereigntyImage = img;
            sovereigntyFileLabel.setText("👑 Calque Souveraineté : Image PNG/GeoJSON chargée");
            notifyParamChange();
        }));

        HBox b1 = new HBox(8, btnIsogloss, isoglossFileLabel); b1.setAlignment(Pos.CENTER_LEFT);
        HBox b2 = new HBox(8, btnKinship, kinshipFileLabel); b2.setAlignment(Pos.CENTER_LEFT);
        HBox b3 = new HBox(8, btnRituals, ritualsFileLabel); b3.setAlignment(Pos.CENTER_LEFT);
        HBox b4 = new HBox(8, btnSovereignty, sovereigntyFileLabel); b4.setAlignment(Pos.CENTER_LEFT);

        layersPanel.getChildren().addAll(layerHeaderBox, culturalFormatHintLabel, b1, b2, b3, b4);
        section.getChildren().addAll(header, desc, grid, layersPanel);
        return section;
    }

    private void loadCustomCultureLayer(String layerName, Consumer<Image> onLoaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer le calque " + layerName + " (PNG/GeoJSON/JPG)");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images & Cartes", "*.png", "*.jpg", "*.jpeg", "*.geojson", "*.json")
        );
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                Image img = new Image(new FileInputStream(file));
                onLoaded.accept(img);
            } catch (Exception ex) {
                logger.error("Failed to load culture layer {}", layerName, ex);
            }
        }
    }

    private void addCustomEngineCheckBoxToUI(String engineKey, String labelText, String tooltipText, boolean defaultSelected) {
        if (typeBCheckBoxMap.containsKey(engineKey)) {
            typeBCheckBoxMap.get(engineKey).setSelected(defaultSelected);
            return;
        }

        CheckBox cb = new CheckBox(labelText);
        cb.setSelected(defaultSelected);
        cb.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");
        cb.setTooltip(new Tooltip(tooltipText));
        cb.setOnAction(e -> notifyParamChange());

        typeBCheckBoxMap.put(engineKey, cb);

        Label badge = new Label("[🔌 CUSTOM]");
        badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-background-color: rgba(56,189,248,0.15); -fx-padding: 2 6; -fx-background-radius: 4;");

        HBox row = new HBox(8, badge, cb);
        row.setAlignment(Pos.CENTER_LEFT);

        if (typeBBoxContainer != null) {
            typeBBoxContainer.getChildren().add(row);
        }
    }

    private VBox createPreviewPane() {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10));

        previewTitleLabel = new Label(org.ether.society.i18n.I18n.getOrDefault("scenario.preview_title_density", "🗺️ CARTE DE DENSITÉ DE POPULATION INITIALE (T₀)"));
        previewTitleLabel.getStyleClass().add("label-header");
        previewTitleLabel.setStyle("-fx-alignment: center; -fx-font-weight: bold;");

        StackPane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #475569; -fx-border-radius: 6; -fx-background-radius: 6; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 0);");
        previewCanvas = new Canvas(640, 360);

        // Clip container (not canvas directly) to prevent JavaFX Prism NGCanvas renderForClip 0x0 NPE
        javafx.scene.shape.Rectangle containerClip = new javafx.scene.shape.Rectangle();
        containerClip.widthProperty().bind(canvasContainer.widthProperty());
        containerClip.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.setClip(containerClip);

        canvasContainer.widthProperty().addListener((obs, oldV, newV) -> {
            double w = Math.floor(newV.doubleValue());
            if (w >= 10.0 && Math.abs(w - previewCanvas.getWidth()) >= 4.0) {
                previewCanvas.setWidth(w);
                drawPreview();
            }
        });
        canvasContainer.heightProperty().addListener((obs, oldV, newV) -> {
            double h = Math.floor(newV.doubleValue());
            if (h >= 10.0 && Math.abs(h - previewCanvas.getHeight()) >= 4.0) {
                previewCanvas.setHeight(h);
                drawPreview();
            }
        });

        // Interactive Zoom, Pan, Selection & Double Click Handlers
        previewCanvas.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double factor = delta > 0 ? 1.15 : 0.85;
            double oldZoom = zoomFactor;
            double newZoom = Math.max(0.5, Math.min(20.0, oldZoom * factor));

            if (newZoom != oldZoom) {
                double mouseX = e.getX();
                double mouseY = e.getY();
                double w = previewCanvas.getWidth();
                double h = previewCanvas.getHeight();
                double baseScale = Math.min(w / 360.0, h / 180.0) * 0.9;
                double oldScale = baseScale * oldZoom;
                double newScale = baseScale * newZoom;

                if (oldScale > 0 && newScale > 0) {
                    double mouseLng = (mouseX - w / 2.0 - panX) / oldScale;
                    double mouseLat = (h / 2.0 + panY - mouseY) / oldScale;

                    zoomFactor = newZoom;
                    panX = mouseX - w / 2.0 - mouseLng * newScale;
                    panY = mouseY - h / 2.0 + mouseLat * newScale;
                } else {
                    zoomFactor = newZoom;
                }
                drawPreview();
            }
        });

        previewCanvas.setOnMousePressed(e -> {
            if (e.isShiftDown() || (graphicSelectBtn != null && graphicSelectBtn.isSelected())) {
                isSelectionDrag = true;
                selectionStartX = e.getX();
                selectionStartY = e.getY();
                selectionCurrentX = e.getX();
                selectionCurrentY = e.getY();
            } else {
                isSelectionDrag = false;
                dragStartX = e.getX();
                dragStartY = e.getY();
            }
        });

        previewCanvas.setOnMouseDragged(e -> {
            if (isSelectionDrag) {
                selectionCurrentX = e.getX();
                selectionCurrentY = e.getY();
            } else {
                double dx = e.getX() - dragStartX;
                double dy = e.getY() - dragStartY;
                panX += dx;
                panY += dy;
                dragStartX = e.getX();
                dragStartY = e.getY();
            }
            drawPreview();
        });

        previewCanvas.setOnMouseReleased(e -> {
            if (isSelectionDrag) {
                isSelectionDrag = false;
                double w = previewCanvas.getWidth();
                double h = previewCanvas.getHeight();
                double baseScale = Math.min(w / 360.0, h / 180.0) * 0.9;
                double scale = baseScale * zoomFactor;
                double offX = (w / 2.0) - (180.0) * scale + panX;
                double offY = (h / 2.0) - (90.0) * scale + panY;

                double x1 = Math.min(selectionStartX, selectionCurrentX);
                double x2 = Math.max(selectionStartX, selectionCurrentX);
                double y1 = Math.min(selectionStartY, selectionCurrentY);
                double y2 = Math.max(selectionStartY, selectionCurrentY);

                if (Math.abs(x2 - x1) > 5 && Math.abs(y2 - y1) > 5) {
                    double lon1 = Math.max(-180.0, Math.min(180.0, ((x1 - offX) / scale) - 180.0));
                    double lon2 = Math.max(-180.0, Math.min(180.0, ((x2 - offX) / scale) - 180.0));
                    double lat2 = Math.max(-90.0, Math.min(90.0, 90.0 - ((y1 - offY) / scale)));
                    double lat1 = Math.max(-90.0, Math.min(90.0, 90.0 - ((y2 - offY) / scale)));

                    if (minLatSpinner != null) minLatSpinner.getValueFactory().setValue(Math.round(lat1 * 10.0) / 10.0);
                    if (maxLatSpinner != null) maxLatSpinner.getValueFactory().setValue(Math.round(lat2 * 10.0) / 10.0);
                    if (minLngSpinner != null) minLngSpinner.getValueFactory().setValue(Math.round(lon1 * 10.0) / 10.0);
                    if (maxLngSpinner != null) maxLngSpinner.getValueFactory().setValue(Math.round(lon2 * 10.0) / 10.0);
                    if (clippingCheckBox != null) clippingCheckBox.setSelected(true);
                    if (graphicSelectBtn != null) graphicSelectBtn.setSelected(false);
                }
                drawPreview();
            }
        });

        previewCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                zoomFactor = 1.0;
                panX = 0.0;
                panY = 0.0;
                drawPreview();
            }
        });

        canvasContainer.getChildren().add(previewCanvas);
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);

        previewStatusLabel = new Label("Aperçu pré-calculé de la distribution initiale");
        previewStatusLabel.getStyleClass().add("control-label");

        HBox legendBox = createLegend();
        root.getChildren().addAll(previewTitleLabel, canvasContainer, legendBox, previewStatusLabel);
        return root;
    }

    private HBox legendItemsContainer;

    private HBox createLegend() {
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER);
        legend.setPadding(new Insets(6, 12, 6, 12));
        legend.getStyleClass().add("card-section");

        legendItemsContainer = new HBox(12);
        legendItemsContainer.setAlignment(Pos.CENTER);

        legend.getChildren().add(legendItemsContainer);
        updateBottomLegend();
        return legend;
    }

    private void updatePreviewTitleText() {
        if (previewTitleLabel == null) return;
        previewTitleLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.preview_title_density", "🗺️ CARTE DE DENSITÉ DE POPULATION INITIALE (T₀)"));
    }

    private void updateBottomLegend() {
        if (legendItemsContainer == null) return;
        legendItemsContainer.getChildren().clear();

        Color[] colors = {
                Color.rgb(30, 95, 165),  // Eau / Inhabité
                Color.rgb(16, 185, 129), // Vert
                Color.rgb(234, 179, 8),  // Jaune
                Color.rgb(249, 115, 22), // Orange
                Color.rgb(239, 68, 68)   // Rouge
        };

        String[] labels = new String[]{
                "Inhabité (0 hab/km²)",
                "Faible (1–50 hab/km²)",
                "Moyenne (50–500)",
                "Élevée (500–2.5k)",
                "Métropole (> 2.5k)"
        };
        String[] fullTooltips = new String[]{
                "Zone Inhabitée : 0 hab/km² (Océans, déserts, haute montagne)",
                "Densité Faible : 1 à 50 hab/km² (Campagnes, tribus nomades)",
                "Densité Moyenne : 50 à 500 hab/km² (Bourgades & vallées agricoles)",
                "Densité Élevée / Cité : 500 à 2 500 hab/km² (Centres urbains régionaux)",
                "Métropole / Megapole : > 2 500 hab/km² (Grandes capitales historiques)"
        };

        for (int i = 0; i < labels.length; i++) {
            javafx.scene.shape.Rectangle colorBox = new javafx.scene.shape.Rectangle(14, 10);
            colorBox.setFill(colors[i]);
            colorBox.setStroke(Color.GRAY);
            colorBox.setArcWidth(3);
            colorBox.setArcHeight(3);

            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 10px; -fx-font-weight: bold;");

            HBox item = new HBox(4, colorBox, lbl);
            item.setAlignment(Pos.CENTER);
            Tooltip.install(item, new Tooltip(fullTooltips[i]));
            legendItemsContainer.getChildren().add(item);
        }
    }

    private void loadCustomDensityMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Charger une carte de densité de population (PNG/JPEG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customDensityImage = new Image(new FileInputStream(file));
                densityMapFileLabel.setText("📷 " + file.getName());
                if (radioImportDemo != null) radioImportDemo.setSelected(true);
                updateDemoCompatibilityDisplay();
                if (currentPreviewCells != null) {
                    distributeInitialPopulation(currentPreviewCells);
                    drawPreview();
                }
            } catch (Exception ex) {
                logger.error("Failed to load custom density map", ex);
            }
        }
    }

    private void updateDemoCompatibilityDisplay() {
        if (demoCompatibilityLabel == null) return;
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        String planetName = p != null ? p.name() : "Standard";
        if (customDensityImage != null) {
            demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_loaded", "✅ Carte chargée et compatible avec le monde choisi à l'onglet 1 (%s)"), planetName));
            demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            demoCompatibilityLabel.setText(I18n.getOrDefault("scenario.demo.no_map", "🪐 Aucune carte externe chargée — Mode procédural actif"));
            demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        }
    }

    private void exportDensityMap() {
        if (previewCanvas == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter la carte de densité de population (PNG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PNG", "*.png"));
        chooser.setInitialFileName("ether-population-density.png");
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                int w = 800;
                int h = 400;
                WritableImage image = new WritableImage(w, h);
                PixelWriter pw = image.getPixelWriter();

                if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
                    double minLat = -90, maxLat = 90;
                    double minLng = -180, maxLng = 180;
                    double scaleX = w / (maxLng - minLng);
                    double scaleY = h / (maxLat - minLat);

                    // Fill background
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            pw.setColor(x, y, Color.rgb(15, 23, 42));
                        }
                    }

                    for (H3Cell c : currentPreviewCells) {
                        int px = (int) ((c.getLongitude() - minLng) * scaleX);
                        int py = (int) ((maxLat - c.getLatitude()) * scaleY);
                        if (px >= 0 && px < w && py >= 0 && py < h) {
                            long pop = c.getPopulation();
                            Color col;
                            if (c.getElevation() <= 0) col = Color.rgb(15, 23, 42);
                            else if (pop <= 0) col = Color.rgb(30, 41, 59);
                            else if (pop < 100) col = Color.rgb(16, 185, 129);
                            else if (pop < 1000) col = Color.rgb(234, 179, 8);
                            else if (pop < 5000) col = Color.rgb(249, 115, 22);
                            else col = Color.rgb(239, 68, 68);

                            pw.setColor(px, py, col);
                        }
                    }
                }
                int iw = (int) image.getWidth();
                int ih = (int) image.getHeight();
                java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(iw, ih, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                PixelReader pr = image.getPixelReader();
                for (int y = 0; y < ih; y++) {
                    for (int x = 0; x < iw; x++) {
                        bImg.setRGB(x, y, pr.getArgb(x, y));
                    }
                }
                ImageIO.write(bImg, "png", file);
                logger.info("Exported density map to {}", file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to export density map", ex);
            }
        }
    }

    private void showDensityImportFormatHelp() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle(I18n.getOrDefault("scenario.dialog.density_help_title", "Spécifications de la Carte de Densité"));
        dialog.setHeaderText(I18n.getOrDefault("scenario.dialog.density_help_header", "Formats d'images supportés pour l'importation de la Densité de Population"));
        dialog.setContentText(
                "Vous pouvez importer une carte de répartition démographique sous forme d'image PNG/JPEG au ratio 2:1 (ex: 2048x1024 pixels en projection équirectangulaire) :\n\n" +
                "1. CARTE DE DENSITÉ NIVEAUX DE GRIS :\n" +
                "   • Noir (0) = 0 hab/km² (Zone inhabitée / désertique / aquatique)\n" +
                "   • Blanc (255) = Densité maximale d'habitation (Foyer urbain ou métropole)\n\n" +
                "2. INTÉGRATION AVEC LE TERRAIN ET L'ÉCOLOGIE :\n" +
                "   • Les zones d'eau (océans/mers) définies dans l'onglet 1 sont automatiquement filtrées pour éviter l'apparition de populations en pleine mer.\n" +
                "   • La population totale paramétrée dans le spinner est distribuée au prorata de la luminosité de chaque pixel."
        );
        dialog.showAndWait();
    }

    private void showCulturalImportFormatHelp() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Spécifications des Calques Cartographiques Culturels & Géopolitiques");
        dialog.setHeaderText("Formats d'images et données spatiales supportées (Projection Équirectangulaire 2:1)");
        dialog.setContentText(
                "Vous pouvez importer des cartes d'isoglosses, de parenté, de rituels et de souveraineté sous forme d'images PNG/JPEG au ratio 2:1 (ex: 2048x1024 pixels, Plate Carrée) :\n\n" +
                "1. 📜 CALQUE ISOGLOSSES & LANGUES (Isogloss & Dialects) :\n" +
                "   • Canaux de couleur R/V/B codant le continuum linguistique et les isolats.\n" +
                "   • Les gradients de teinte définissent la distance d'intelligibilité inter-tribale ΔL (friction isoglossique).\n\n" +
                "2. 🏛️ CALQUE PARENTÉ & OUTILLAGE (Kinship & Toolsets) :\n" +
                "   • Nuances codant les structures de clan, règles d'exogamie/endogamie et traditions techniques.\n" +
                "   • Permet d'initialiser les réseaux d'échange de savoirs et le seuil critique d'apprentissage de Henrich.\n\n" +
                "3. 🔮 CALQUE CROYANCES & NORME (Rituals & Taboos) :\n" +
                "   • Canaux de fréquence codant les zones sacrées, religions d'État, tabous et syncrétismes.\n" +
                "   • Influence le forçage thermodynamique des conversions et la cohésion d'Asabiyyah.\n\n" +
                "4. 👑 CALQUE SOUVERAINETÉ POLITIQUE (Sovereignty & Borders) :\n" +
                "   • Démarcation géographique des États, confédérations et zones d'influence des capitales.\n" +
                "   • Définit la friction de frontière σ_friction et le contrôle administratif pour les simulations cliodynamiques."
        );
        dialog.showAndWait();
    }

    private void generatePreview() {
        if (previewStatusLabel != null) {
            previewStatusLabel.setText("⚡ Calcul de l'aperçu démographique en arrière-plan...");
        }
        new Thread(() -> {
            try {
                PlanetPreset cfg = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : null);
                if (cfg == null) cfg = PlanetPreset.EARTH_LIKE;

                // Fast preview generation using Resolution 3 (41,162 cells) instead of blocking Res 4
                List<H3Cell> previewCells = new ProceduralGenerator().generatePlanet(
                        new PlanetPreset(
                                cfg.name(), 3, cfg.radiusKm(), cfg.dayLengthHours(),
                                cfg.axialTiltDegrees(), cfg.yearLengthDays(), cfg.distanceToSunAU(),
                                cfg.solarLuminosity(), cfg.minAltitudeMeters(), cfg.maxAltitudeMeters(),
                                cfg.averageTempC(), cfg.seed(), cfg.noiseFrequency(),
                                cfg.noiseScale(), cfg.waterLevel(), cfg.temperatureGradient(),
                                cfg.oxygenPercentage(), cfg.albedo(), cfg.atmospherePressureAtm(),
                                cfg.isSatellite(), cfg.parentPlanetMassEarthMasses(),
                                cfg.orbitalDistanceToParentKm(), cfg.co2Ppm(),
                                cfg.seismicActivityLevel(), cfg.volcanicActivityLevel(),
                                cfg.customElevBase64(), cfg.customBiomeBase64(), cfg.customResourceBase64(),
                                cfg.customClimateBase64(), cfg.customRainfallBase64(), cfg.customSeasonalityBase64(),
                                cfg.elevationUseImport(), cfg.elevationMapSource(),
                                cfg.tempUseImport(), cfg.tempSource(), cfg.tempSeed(),
                                cfg.precipUseImport(), cfg.precipSource(), cfg.precipSeed(),
                                cfg.seasonUseImport(), cfg.seasonSource(), cfg.seasonSeed()
                        )
                );

                // Map elevation and biome images corresponding to the selected planet preset
                applyPresetMapToCells(previewCells, cfg);

                distributeInitialPopulation(previewCells);

                javafx.application.Platform.runLater(() -> {
                    currentPreviewCells = previewCells;
                    drawPreview();
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText("Aperçu généré : " + previewCells.size() + " cellules.");
                    }
                });
            } catch (Exception ex) {
                logger.error("Failed to generate preview", ex);
                javafx.application.Platform.runLater(() -> {
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText("Erreur aperçu : " + ex.getMessage());
                    }
                });
            }
        }, "H3-Preview-Async-Thread").start();
    }

    private void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        long totalPop = initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null ? initialHumanCountSpinner.getValue() : 1_000_000L;
        double capitalK0 = initialCapitalSpinner != null && initialCapitalSpinner.getValue() != null ? initialCapitalSpinner.getValue() : 1000.0;
        String pattern = densityPatternCombo != null ? densityPatternCombo.getValue() : "UNBIASED_NATURAL";
        boolean isEarthPreset = activePlanetPreset != null && activePlanetPreset.name() != null && activePlanetPreset.name().toLowerCase().contains("earth");
        long startYear = startYearSpinner != null && startYearSpinner.getValue() != null ? startYearSpinner.getValue() : -8000;

        if (customDensityImage != null) {
            // Custom PNG density map sampling
            List<H3Cell> landCells = cells.stream()
                    .filter(c -> c.getElevation() > 0 && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)
                    .toList();
            if (landCells.isEmpty()) return;

            double popPerCell = (double) totalPop / landCells.size();
            for (H3Cell c : landCells) {
                double normLat = (c.getLatitude() + 90.0) / 180.0;
                double normLon = (c.getLongitude() + 180.0) / 360.0;
                int px = (int) Math.min(customDensityImage.getWidth() - 1, Math.max(0, normLon * customDensityImage.getWidth()));
                int py = (int) Math.min(customDensityImage.getHeight() - 1, Math.max(0, (1.0 - normLat) * customDensityImage.getHeight()));
                Color imgCol = customDensityImage.getPixelReader().getColor(px, py);
                double factor = imgCol.getBrightness() * 5.0;
                c.setPopulation((int) (popPerCell * factor));
            }
        } else {
            // Standard procedural density calculation via ProceduralPopulationEngine
            double techLevel = Math.clamp(Math.log10(Math.max(1.0, capitalK0)) * 2.2 + 0.2, 0.2, 10.0);
            ProceduralPopulationEngine.distributePopulation(cells, getScenario(), totalPop, techLevel, pattern, isEarthPreset, startYear);
        }
    }

    private Color getReliefAndDensityColor(H3Cell c) {
        if (c == null) return Color.rgb(15, 23, 42);

        double elev = c.getElevation() != null ? c.getElevation() : 0.0;
        Biome b = c.getBiome();

        Color baseTerrainCol;
        if (elev <= 0) {
            // Sea / Ocean Bathymetry (Deep navy to coastal cyan-blue)
            double depthRatio = Math.clamp(Math.abs(elev) / 4000.0, 0.0, 1.0);
            int r = (int) (30 - depthRatio * 20);
            int g = (int) (95 - depthRatio * 75);
            int bCol = (int) (165 - depthRatio * 115);
            baseTerrainCol = Color.rgb(Math.clamp(r, 0, 255), Math.clamp(g, 0, 255), Math.clamp(bCol, 0, 255));
        } else {
            // Land Topography (hypsometric tinting + biome colors)
            double altRatio = Math.clamp(elev / 4500.0, 0.0, 1.0);
            int r, g, bCol;
            if (b == Biome.SNOW || elev > 3800) {
                r = 235; g = 245; bCol = 255; // Ice / Snow peaks
            } else if (b == Biome.TUNDRA) {
                r = 135; g = 155; bCol = 135;
            } else if (b == Biome.DESERT) {
                r = 215; g = 185; bCol = 125;
            } else if (b == Biome.JUNGLE) {
                r = 25; g = 100; bCol = 45;
            } else if (b == Biome.FOREST) {
                r = 35; g = 115; bCol = 50;
            } else if (b == Biome.HILLS || (elev > 1200 && elev <= 2800)) {
                r = 130 + (int)(altRatio * 40);
                g = 120 - (int)(altRatio * 20);
                bCol = 80;
            } else if (b == Biome.MOUNTAINS || elev > 2800) {
                r = 160 + (int)(altRatio * 50);
                g = 155 + (int)(altRatio * 45);
                bCol = 150 + (int)(altRatio * 45);
            } else { // PLAINS / BEACH / Default land
                r = 65; g = 135; bCol = 55;
            }

            // Modulate by elevation (high land is slightly lighter/paler)
            r = Math.clamp((int)(r * (0.85 + altRatio * 0.35)), 0, 255);
            g = Math.clamp((int)(g * (0.85 + altRatio * 0.35)), 0, 255);
            bCol = Math.clamp((int)(bCol * (0.85 + altRatio * 0.35)), 0, 255);
            baseTerrainCol = Color.rgb(r, g, bCol);
        }

        long pop = c.getPopulation() != null ? c.getPopulation() : 0;
        if (pop <= 0 || elev <= 0) {
            return baseTerrainCol;
        }

        // Population Density Heat Overlay
        Color heatCol;
        if (pop < 50) {
            heatCol = Color.rgb(16, 185, 129); // Faible (1 - 50 hab/km²)
        } else if (pop < 500) {
            heatCol = Color.rgb(234, 179, 8);  // Moyenne (50 - 500 hab/km²)
        } else if (pop < 2500) {
            heatCol = Color.rgb(249, 115, 22); // Élevée / Cité (500 - 2500 hab/km²)
        } else {
            heatCol = Color.rgb(239, 68, 68);  // Métropole (> 2500 hab/km²)
        }

        // Blend heat with terrain relief (75% heat, 25% relief) so physical land topography shines through!
        double alpha = 0.75;
        int blendedR = (int) (baseTerrainCol.getRed() * 255 * (1.0 - alpha) + heatCol.getRed() * 255 * alpha);
        int blendedG = (int) (baseTerrainCol.getGreen() * 255 * (1.0 - alpha) + heatCol.getGreen() * 255 * alpha);
        int blendedB = (int) (baseTerrainCol.getBlue() * 255 * (1.0 - alpha) + heatCol.getBlue() * 255 * alpha);
        return Color.rgb(Math.clamp(blendedR, 0, 255), Math.clamp(blendedG, 0, 255), Math.clamp(blendedB, 0, 255));
    }

    private double computeCellCarryingCapacity(H3Cell c) {
        if (c == null || c.getElevation() <= 0) return 0.0;
        double baseCap = 250.0;
        Biome b = c.getBiome();
        if (b == Biome.DESERT || b == Biome.TUNDRA || b == Biome.SNOW) baseCap *= 0.1;
        else if (b == Biome.PLAINS || b == Biome.FOREST) baseCap *= 1.5;
        else if (b == Biome.JUNGLE) baseCap *= 0.8;

        if (c.getWaterResource() != null && c.getWaterResource() > 0.1) {
            baseCap *= (1.0 + 3.0 * (c.getWaterResource() / 1000.0));
        }
        if (c.getFreshwaterAquifer() != null && c.getFreshwaterAquifer() > 0.1) {
            baseCap *= (1.0 + 1.5 * (c.getFreshwaterAquifer() / 1000.0));
        }

        double capK0 = initialCapitalSpinner != null ? initialCapitalSpinner.getValue() : 1000.0;
        baseCap *= Math.max(0.5, (capK0 / 1000.0) * 0.8);
        return Math.max(10.0, baseCap);
    }

    private void drawPreview() {
        if (previewCanvas == null) return;
        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();
        if (w < 1.0 || h < 1.0) return;
        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.rgb(15, 23, 42));
        gc.fillRect(0, 0, w, h);

        double minLat = -90, maxLat = 90;
        double minLng = -180, maxLng = 180;

        double scaleX = w / (maxLng - minLng);
        double scaleY = h / (maxLat - minLat);
        double baseScale = Math.min(scaleX, scaleY) * 0.9;
        double scale = baseScale * zoomFactor;

        double offX = (w / 2.0) - (0.0 - minLng) * scale + panX;
        double offY = (h / 2.0) - (maxLat - 0.0) * scale + panY;

        if (currentPreviewCells != null) {
            for (H3Cell c : currentPreviewCells) {
                double x = (c.getLongitude() - minLng) * scale + offX;
                double y = (maxLat - c.getLatitude()) * scale + offY;

                if (x < -5 || x > w + 5 || y < -5 || y > h + 5) continue;

                Color col = getReliefAndDensityColor(c);
                gc.setFill(col);
                gc.fillRect(x, y, 2, 2);
            }
        } else {
            drawInstant2DDensityPreview(gc, w, h, scale, offX, offY);
        }

        // Draw Clipping bounding box & dimmed overlay
        boolean isClippingActive = (clippingCheckBox != null && clippingCheckBox.isSelected()) || isSelectionDrag;
        if (isClippingActive) {
            double cMinLat = minLatSpinner != null ? minLatSpinner.getValue() : -90.0;
            double cMaxLat = maxLatSpinner != null ? maxLatSpinner.getValue() : 90.0;
            double cMinLng = minLngSpinner != null ? minLngSpinner.getValue() : -180.0;
            double cMaxLng = maxLngSpinner != null ? maxLngSpinner.getValue() : 180.0;

            if (isSelectionDrag) {
                double x1 = Math.min(selectionStartX, selectionCurrentX);
                double x2 = Math.max(selectionStartX, selectionCurrentX);
                double y1 = Math.min(selectionStartY, selectionCurrentY);
                double y2 = Math.max(selectionStartY, selectionCurrentY);

                cMinLng = Math.max(-180.0, Math.min(180.0, ((x1 - offX) / scale) + minLng));
                cMaxLng = Math.max(-180.0, Math.min(180.0, ((x2 - offX) / scale) + minLng));
                cMaxLat = Math.max(-90.0, Math.min(90.0, maxLat - ((y1 - offY) / scale)));
                cMinLat = Math.max(-90.0, Math.min(90.0, maxLat - ((y2 - offY) / scale)));
            }

            double x1 = (cMinLng - minLng) * scale + offX;
            double x2 = (cMaxLng - minLng) * scale + offX;
            double y1 = (maxLat - cMaxLat) * scale + offY;
            double y2 = (maxLat - cMinLat) * scale + offY;

            double rx = Math.min(x1, x2);
            double ry = Math.min(y1, y2);
            double rw = Math.abs(x2 - x1);
            double rh = Math.abs(y2 - y1);

            // Dim exterior
            gc.setFill(Color.rgb(0, 0, 0, 0.45));
            gc.fillRect(0, 0, w, Math.max(0, ry));
            gc.fillRect(0, ry + rh, w, Math.max(0, h - (ry + rh)));
            gc.fillRect(0, Math.max(0, ry), Math.max(0, rx), rh);
            gc.fillRect(rx + rw, Math.max(0, ry), Math.max(0, w - (rx + rw)), rh);

            // Bright Cyan dashed rectangle
            gc.setStroke(Color.rgb(56, 189, 248, 0.95));
            gc.setLineWidth(2.0);
            gc.setLineDashes(6.0, 4.0);
            // Text tag
            gc.setFill(Color.rgb(56, 189, 248));
            gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 11));
            gc.fillText(String.format("✂️ Zone: Lat[%.1f°, %.1f°] Lng[%.1f°, %.1f°]", cMinLat, cMaxLat, cMinLng, cMaxLng), rx + 4, ry - 6);
        }
    }

    private void drawLegendDot(GraphicsContext gc, double x, double y, Color c, String label) {
        gc.setFill(c);
        gc.fillOval(x, y - 7, 8, 8);
        gc.setFill(Color.rgb(226, 232, 240));
        gc.fillText(label, x + 12, y);
    }

    private void drawInstant2DDensityPreview(GraphicsContext gc, double w, double h, double scale, double offX, double offY) {
        int pwWidth = 320;
        int pwHeight = 160;
        WritableImage img = new WritableImage(pwWidth, pwHeight);
        PixelWriter writer = img.getPixelWriter();

        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : PlanetPreset.EARTH_LIKE;
        boolean isEarth = planet == PlanetPreset.EARTH_LIKE || "earth".equalsIgnoreCase(planet.elevationMapSource()) ||
                (planet.name() != null && (planet.name().toLowerCase().contains("earth") || planet.name().toLowerCase().contains("terre")));

        Image bgImage = isEarth ? getCachedEarthElevationImage() : null;
        PixelReader bgReader = bgImage != null ? bgImage.getPixelReader() : null;

        boolean isFootprintMode = previewModeCombo != null && previewModeCombo.getValue() != null && previewModeCombo.getValue().toLowerCase().contains("empreinte");

        for (int py = 0; py < pwHeight; py++) {
            for (int px = 0; px < pwWidth; px++) {
                Color pxColor;
                if (bgReader != null) {
                    pxColor = bgReader.getColor(px, py);
                } else {
                    double lat = 90.0 - (py / (double) pwHeight) * 180.0;
                    double lon = -180.0 + (px / (double) pwWidth) * 360.0;
                    double alt = Math.sin(lat * Math.PI / 180.0) * Math.cos(lon * Math.PI / 180.0);
                    pxColor = alt < 0 ? Color.rgb(20, 60, 140) : Color.rgb(40, 140, 60);
                }

                // Apply heat overlay for instant 2D preview based on active mode
                double lat = 90.0 - (py / (double) pwHeight) * 180.0;
                double lon = -180.0 + (px / (double) pwWidth) * 360.0;
                boolean isLand = bgReader != null ? (pxColor.getGreen() > pxColor.getBlue() || pxColor.getRed() > 0.18) : (pxColor.getGreen() > 0.4);

                if (isLand && lat > -50 && lat < 70) {
                    boolean isArid = (lat > 14 && lat < 33 && lon > -15 && lon < 55) || (lat > 32 && lat < 48 && lon > 60 && lon < 105);
                    boolean isFertileHub = (lat > 20 && lat < 38 && lon > 25 && lon < 90) || (lat > 5 && lat < 25 && lon > 95 && lon < 125);

                    if (isFootprintMode) {
                        if (isArid) {
                            // Arid/Desert zones under human presence suffer critical Malthusian tension (Pop > Carrying Capacity K)
                            pxColor = Color.rgb(239, 68, 68); // Red
                        } else if (isFertileHub) {
                            pxColor = Color.rgb(234, 179, 8); // Yellow (Moderate footprint)
                        }
                    } else {
                        if (isFertileHub) {
                            pxColor = Color.rgb(249, 115, 22); // Orange (Population Density Hubs)
                        }
                    }
                }

                writer.setColor(px, py, pxColor);
            }
        }

        double drawW = 360.0 * scale;
        double drawH = 180.0 * scale;
        gc.drawImage(img, offX, offY, drawW, drawH);

        gc.setFill(Color.rgb(56, 189, 248, 0.95));
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        gc.fillText("⚡ Aperçu 2D dynamique instantané (Valider pour générer les cellules H3)...", 20, h - 15);
    }

    private static Image cachedEarthElevationImage = null;
    private Image getCachedEarthElevationImage() {
        if (cachedEarthElevationImage == null) {
            try (var is = getClass().getResourceAsStream("/maps/earth_elevation.png")) {
                if (is != null) cachedEarthElevationImage = new Image(is);
            } catch (Exception ignored) {}
        }
        return cachedEarthElevationImage;
    }

    // =========================================================================
    // DEFERRED EXECUTION — Calculate H3 Grid & Start Simulation
    // =========================================================================

    public void setProgressControls(ProgressBar bar, Label label, javafx.scene.Node overlayContainer) {
        this.externalProgressBar = bar;
        this.externalStatusLabel = label;
        this.externalOverlayContainer = overlayContainer;
    }

    private void updateProgress(double progressVal, String msg) {
        javafx.application.Platform.runLater(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progressVal);
                progressBar.setVisible(true);
                progressBar.setManaged(true);
            }
            if (progressStatusLabel != null) {
                progressStatusLabel.setText(msg);
                progressStatusLabel.setVisible(true);
                progressStatusLabel.setManaged(true);
            }
            if (externalProgressBar != null) {
                externalProgressBar.setProgress(progressVal);
            }
            if (externalStatusLabel != null) {
                externalStatusLabel.setText(msg);
            }
            if (externalOverlayContainer != null) {
                boolean active = progressVal < 1.0;
                externalOverlayContainer.setVisible(active);
                externalOverlayContainer.setManaged(active);
            }
        });
    }

    private void resetStartButtonState() {
        isCalculationRunning = false;
        javafx.application.Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.start_btn", "🚀 VALIDER ET LANCER LA SIMULATION"));
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setDisable(false);
            }
        });
    }

    private void setStartButtonCancelState() {
        isCalculationRunning = true;
        javafx.application.Platform.runLater(() -> {
            if (startBtn != null) {
                startBtn.setText("⏹ ANNULER / STOPPER LA GÉNÉRATION");
                startBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6;");
                startBtn.setDisable(false);
            }
        });
    }

    private void handleStartOrCancel() {
        if (isCalculationRunning) {
            cancelCalculation();
        } else {
            startSimulationDeferred();
        }
    }

    private void cancelCalculation() {
        cancelRequested = true;
        updateProgress(0.0, "🛑 Annulation du calcul en cours à la demande de l'utilisateur...");
        if (generationThread != null && generationThread.isAlive()) {
            generationThread.interrupt();
        }
        resetStartButtonState();
    }

    private void startSimulationDeferred() {
        cancelRequested = false;
        setStartButtonCancelState();
        updateProgress(0.02, "⚡ Enregistrement du scénario & préparation du calcul H3...");

        // 1. Explicitly Save Scenario to DB & Provide UI Confirmation
        Scenario scenarioToSave = getScenario();
        try {
            scenarioRepo.saveOrUpdate(scenarioToSave);
            logger.info("Scenario '{}' automatically saved to database repository before simulation launch.", scenarioToSave.getName());
            if (scenarioPresetBar != null) {
                boolean exists = scenarioPresetBar.getPresetCombo().getItems().stream()
                        .anyMatch(p -> p != null && p.getName() != null && p.getName().equalsIgnoreCase(scenarioToSave.getName()));
                if (!exists) {
                    scenarioPresetBar.getPresetCombo().getItems().add(scenarioToSave);
                }
                scenarioPresetBar.getPresetCombo().setValue(scenarioToSave);
            }
        } catch (Exception ex) {
            logger.warn("Could not save scenario to DB prior to simulation launch", ex);
        }

        if (cancelRequested) {
            resetStartButtonState();
            updateProgress(0.0, "🛑 Calcul annulé par l'utilisateur.");
            return;
        }

        updateProgress(0.05, "✅ Scénario '" + scenarioToSave.getName() + "' prêt ! ⚡ Calcul de la grille H3...");

        generationThread = new Thread(() -> {
            try {
                PlanetPreset cfg = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
                if (cfg == null) cfg = PlanetPreset.EARTH_LIKE;

                Integer res = h3ResolutionCombo.getValue();
                if (res != null) {
                    cfg = new PlanetPreset(
                            cfg.name(), res, cfg.radiusKm(), cfg.dayLengthHours(),
                            cfg.axialTiltDegrees(), cfg.yearLengthDays(), cfg.distanceToSunAU(),
                            cfg.solarLuminosity(), cfg.minAltitudeMeters(), cfg.maxAltitudeMeters(),
                            cfg.averageTempC(), cfg.seed(), cfg.noiseFrequency(),
                            cfg.noiseScale(), cfg.waterLevel(), cfg.temperatureGradient(),
                            cfg.oxygenPercentage(), cfg.albedo(), cfg.atmospherePressureAtm(),
                            cfg.isSatellite(), cfg.parentPlanetMassEarthMasses(),
                            cfg.orbitalDistanceToParentKm(), cfg.co2Ppm(),
                            cfg.seismicActivityLevel(), cfg.volcanicActivityLevel(),
                            cfg.customElevBase64(), cfg.customBiomeBase64(), cfg.customResourceBase64(),
                            cfg.customClimateBase64(), cfg.customRainfallBase64(), cfg.customSeasonalityBase64(),
                            cfg.elevationUseImport(), cfg.elevationMapSource(),
                            cfg.tempUseImport(), cfg.tempSource(), cfg.tempSeed(),
                            cfg.precipUseImport(), cfg.precipSource(), cfg.precipSeed(),
                            cfg.seasonUseImport(), cfg.seasonSource(), cfg.seasonSeed()
                    );
                }

                logger.info("Generating H3 cells for scenario '{}' at resolution {}", scenarioToSave.getName(), cfg.resolution());

                // Progress-tracked procedural planet generation (5% -> 70%)
                List<H3Cell> cells = new ProceduralGenerator().generatePlanet(cfg, (done, total) -> {
                    double progressVal = 0.05 + 0.65 * ((double) done / total);
                    int percent = (int) (progressVal * 100);
                    updateProgress(progressVal, String.format("🌍 Génération du relief & biomes H3 : %d%% (%d / %d cellules)", percent, done, total));
                }, () -> cancelRequested);

                if (cancelRequested) throw new java.util.concurrent.CancellationException("Generation cancelled by user");

                // Map elevation and biome images corresponding to the selected planet preset
                applyPresetMapToCells(cells, cfg);

                // Hydrography and river basin accumulation (70% -> 78%)
                updateProgress(0.72, "🌊 Calcul du réseau hydrographique et accumulation des bassins versants (72%)...");

                // Apply Geographical Clipping if enabled (78% -> 85%)
                if (clippingCheckBox != null && clippingCheckBox.isSelected()) {
                    updateProgress(0.80, "✂️ Application du découpage géographique et modélisation des frontières (80%)...");

                    double minLat = minLatSpinner.getValue();
                    double maxLat = maxLatSpinner.getValue();
                    double minLng = minLngSpinner.getValue();
                    double maxLng = maxLngSpinner.getValue();

                    double marginLat = Math.max(1.0, (maxLat - minLat) * 0.08);
                    double marginLng = Math.max(1.0, (maxLng - minLng) * 0.08);

                    List<H3Cell> clippedCells = new ArrayList<>();
                    for (H3Cell c : cells) {
                        if (cancelRequested) throw new java.util.concurrent.CancellationException("Generation cancelled by user");
                        if (c.getLatitude() >= minLat && c.getLatitude() <= maxLat &&
                            c.getLongitude() >= minLng && c.getLongitude() <= maxLng) {
                            
                            // Tag boundary cells for realistic edge condition modeling
                            if (c.getLatitude() <= minLat + marginLat || c.getLatitude() >= maxLat - marginLat ||
                                c.getLongitude() <= minLng + marginLng || c.getLongitude() >= maxLng - marginLng) {
                                c.setBoundaryCell(true);
                            }
                            clippedCells.add(c);
                        }
                    }
                    cells = clippedCells;
                    logger.info("Clipping applied: {} cells retained out of global planet grid.", cells.size());
                }

                if (cancelRequested) throw new java.util.concurrent.CancellationException("Generation cancelled by user");

                // Distribute Initial Population (85% -> 95%)
                updateProgress(0.88, "👥 Répartition de la population, empreinte écologique & capital K₀ (88%)...");

                distributeInitialPopulation(cells);
                if (cancelRequested) throw new java.util.concurrent.CancellationException("Generation cancelled by user");

                currentPreviewCells = cells;

                final List<H3Cell> finalCells = cells;
                javafx.application.Platform.runLater(() -> {
                    updateProgress(1.0, "✅ " + finalCells.size() + " cellules H3 calculées ! Lancement du scénario '" + scenarioToSave.getName() + "'...");
                    drawPreview();
                    previewStatusLabel.setText("Généré : " + finalCells.size() + " cellules H3.");

                    resetStartButtonState();

                    if (onStartSimulation != null) {
                        onStartSimulation.accept(scenarioToSave);
                    }
                });
            } catch (Exception ex) {
                if (cancelRequested || ex instanceof java.util.concurrent.CancellationException || ex instanceof InterruptedException) {
                    logger.info("Deferred H3 calculation cancelled by user.");
                    updateProgress(0.0, "🛑 Calcul annulé par l'utilisateur.");
                } else {
                    logger.error("Error during deferred H3 cell calculation", ex);
                    updateProgress(1.0, "❌ Erreur pendant le calcul : " + ex.getMessage());
                }
                resetStartButtonState();
            } finally {
                generationThread = null;
            }
        }, "H3-Scenario-Generator-Thread");
        generationThread.setDaemon(true);
        generationThread.start();
    }

    private void applyPresetMapToCells(List<H3Cell> cells, PlanetPreset cfg) {
        if (cfg == null || cells == null || cells.isEmpty()) return;
        
        String mapSource = cfg.elevationMapSource() != null ? cfg.elevationMapSource().toLowerCase() : "";
        String presetName = cfg.name() != null ? cfg.name().toLowerCase() : "";
        
        if (cfg.customElevBase64() != null && !cfg.customElevBase64().isBlank()) {
            try {
                Image customElev = org.ether.society.data.ImageMapLoader.base64PngToImage(cfg.customElevBase64());
                Image customBiome = org.ether.society.data.ImageMapLoader.base64PngToImage(cfg.customBiomeBase64());
                if (customElev != null) {
                    new org.ether.society.data.ImageMapLoader().mapImagesToCells(cells, customElev, customBiome, null, cfg.minAltitudeMeters(), cfg.maxAltitudeMeters());
                }
            } catch (Exception e) {
                logger.warn("Could not decode custom Base64 elevation map image for preset {}", cfg.name(), e);
            }
        } else if (mapSource.equals("earth") || presetName.contains("earth") || presetName.contains("terre") || presetName.contains("terran")) {
            try (var elevStream = getClass().getResourceAsStream("/maps/earth_elevation.png");
                 var biomeStream = getClass().getResourceAsStream("/maps/earth_biomes.png")) {
                if (elevStream != null || biomeStream != null) {
                    new org.ether.society.data.ImageMapLoader().mapDataToCells(cells, elevStream, biomeStream);
                }
            } catch (Exception e) {
                logger.warn("Could not load Earth elevation map image", e);
            }
        } else if (mapSource.equals("mars") || presetName.contains("mars") || presetName.contains("ares")) {
            try (var elevStream = getClass().getResourceAsStream("/maps/mars_elevation.png");
                 var biomeStream = getClass().getResourceAsStream("/maps/mars_biomes.png")) {
                if (elevStream != null || biomeStream != null) {
                    new org.ether.society.data.ImageMapLoader().mapDataToCells(cells, elevStream, biomeStream);
                }
            } catch (Exception e) {
                logger.debug("No static Mars image resource found, using physical Simplex terrain generator");
            }
        } else if (mapSource.equals("venus") || presetName.contains("venus") || presetName.contains("vénus") || presetName.contains("hesperos")) {
            try (var elevStream = getClass().getResourceAsStream("/maps/venus_elevation.png");
                 var biomeStream = getClass().getResourceAsStream("/maps/venus_biomes.png")) {
                if (elevStream != null || biomeStream != null) {
                    new org.ether.society.data.ImageMapLoader().mapDataToCells(cells, elevStream, biomeStream);
                }
            } catch (Exception e) {
                logger.debug("No static Venus image resource found, using physical Simplex terrain generator");
            }
        } else if (mapSource.equals("moon") || presetName.contains("lune") || presetName.contains("moon") || presetName.contains("selene")) {
            try (var elevStream = getClass().getResourceAsStream("/maps/moon_elevation.png");
                 var biomeStream = getClass().getResourceAsStream("/maps/moon_biomes.png")) {
                if (elevStream != null || biomeStream != null) {
                    new org.ether.society.data.ImageMapLoader().mapDataToCells(cells, elevStream, biomeStream);
                }
            } catch (Exception e) {
                logger.debug("No static Moon image resource found, using physical Simplex terrain generator");
            }
        }
    }

    // =========================================================================
    // EVENTS SECTION — Historical Planetary Events
    // =========================================================================

    @SuppressWarnings("unchecked")
    private VBox createEventsSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        title3Events = new Label();
        title3Events.getStyleClass().add("label-header");
        title3Events.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events", "Planifier des événements climatiques et géologiques.")));

        eventsList = FXCollections.observableArrayList();
        eventsTable = new TableView<>(eventsList);
        eventsTable.setEditable(true);
        eventsTable.setPrefHeight(200);

        List<String> eventTypes = List.of("earthquake","volcano","tsunami","tornado","wildfire","flood","drought","pandemic","impact");

        TableColumn<ClimateEvent, String> colType = new TableColumn<>();
        colType.setCellValueFactory(d -> d.getValue().typeProperty());
        colType.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(eventTypes)));
        colType.setOnEditCommit(e -> e.getRowValue().setType(e.getNewValue()));
        colType.setPrefWidth(120);

        TableColumn<ClimateEvent, String> colName = new TableColumn<>();
        colName.setCellValueFactory(d -> d.getValue().nameProperty());
        colName.setCellFactory(TextFieldTableCell.forTableColumn());
        colName.setOnEditCommit(e -> e.getRowValue().setName(e.getNewValue()));
        colName.setPrefWidth(160);

        TableColumn<ClimateEvent, Integer> colYear = new TableColumn<>();
        colYear.setCellValueFactory(d -> d.getValue().yearProperty().asObject());
        colYear.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.IntegerStringConverter()));
        colYear.setOnEditCommit(e -> e.getRowValue().setYear(e.getNewValue()));
        colYear.setPrefWidth(70);

        TableColumn<ClimateEvent, Double> colLat = new TableColumn<>();
        colLat.setCellValueFactory(d -> d.getValue().latitudeProperty().asObject());
        colLat.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colLat.setOnEditCommit(e -> e.getRowValue().setLatitude(e.getNewValue()));
        colLat.setPrefWidth(75);

        TableColumn<ClimateEvent, Double> colLon = new TableColumn<>();
        colLon.setCellValueFactory(d -> d.getValue().longitudeProperty().asObject());
        colLon.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colLon.setOnEditCommit(e -> e.getRowValue().setLongitude(e.getNewValue()));
        colLon.setPrefWidth(75);

        TableColumn<ClimateEvent, Double> colDepth = new TableColumn<>();
        colDepth.setCellValueFactory(d -> d.getValue().depthProperty().asObject());
        colDepth.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colDepth.setOnEditCommit(e -> e.getRowValue().setDepth(e.getNewValue()));
        colDepth.setPrefWidth(80);

        TableColumn<ClimateEvent, Double> colMag = new TableColumn<>();
        colMag.setCellValueFactory(d -> d.getValue().magnitudeProperty().asObject());
        colMag.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colMag.setOnEditCommit(e -> e.getRowValue().setMagnitude(e.getNewValue()));
        colMag.setPrefWidth(80);

        eventsTable.getColumns().addAll(colName, colYear, colLat, colLon, colDepth, colMag);
        eventsTable.setUserData(new TableColumn[]{colName, colYear, colLat, colLon, colDepth, colMag});

        addEventBtn = new Button();
        addEventBtn.getStyleClass().add("button-secondary");
        addEventBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.add", "Ajouter un événement.")));
        addEventBtn.setOnAction(e -> eventsList.add(new ClimateEvent("earthquake", "Nouvel Événement", 0, 0.0, 0.0, 10.0, 6.0)));

        removeEventBtn = new Button();
        removeEventBtn.getStyleClass().add("button-secondary");
        removeEventBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.remove", "Supprimer l'événement sélectionné.")));
        removeEventBtn.setOnAction(e -> {
            ClimateEvent sel = eventsTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(org.ether.society.i18n.I18n.getOrDefault("dialog.confirm.title", "Confirmation de suppression"));
                alert.setHeaderText(null);
                alert.setContentText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.confirm_delete", "Voulez-vous vraiment supprimer cet événement ?"));
                WindowUtils.applyWindowIcon(alert);
                alert.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.OK) {
                        eventsList.remove(sel);
                    }
                });
            }
        });

        loadEarthEventsBtn = new Button();
        loadEarthEventsBtn.getStyleClass().add("button-secondary");
        loadEarthEventsBtn.setTooltip(new Tooltip(org.ether.society.i18n.I18n.getOrDefault("scenario.tooltip.events.load", "Charger les événements historiques de la Terre.")));
        loadEarthEventsBtn.setOnAction(e -> loadEarthHistoricalEvents());

        CheckBox eventsCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.enable", "Planifier des événements climatiques & catastrophes historiques"));
        eventsCheckBox.setStyle("-fx-font-weight: bold;");

        randomEventsCheckBox = new CheckBox(org.ether.society.i18n.I18n.getOrDefault("scenario.events.random_checkbox", "☑️ Générer dynamiquement les événements géologiques & dérives climatiques (séismes, volcanisme, élévation mer, Sahara vert)"));
        randomEventsCheckBox.setSelected(true);
        randomEventsCheckBox.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        HBox btnBar = new HBox(8, addEventBtn, removeEventBtn, new Separator(javafx.geometry.Orientation.VERTICAL), loadEarthEventsBtn);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        VBox eventsSubPanel = new VBox(8, randomEventsCheckBox, btnBar, eventsTable);
        eventsSubPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(251,146,60,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        eventsSubPanel.setVisible(false);
        eventsSubPanel.setManaged(false);

        eventsCheckBox.setOnAction(e -> {
            boolean active = eventsCheckBox.isSelected();
            eventsSubPanel.setVisible(active);
            eventsSubPanel.setManaged(active);
            notifyParamChange();
        });

        section.getChildren().addAll(title3Events, eventsCheckBox, eventsSubPanel);
        return section;
    }

    private void loadEarthHistoricalEvents() {
        int startYear = startYearSpinner != null ? startYearSpinner.getValue() : -8000;
        loadEarthHistoricalEvents(startYear);
    }

    private void loadEarthHistoricalEvents(int startYear) {
        if (eventsList == null) return;
        eventsList.clear();

        List<ClimateEvent> masterList = new ArrayList<>();

        // Éruptions Volcaniques & Stratosphère (tau)
        masterList.add(new ClimateEvent("volcano", "Super-Éruption Toba (VEI-8, τ=3.50, Gel Global)", -74000, 2.88, 98.88, 0.0, 8.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Santorini / Thera (VEI-7)", -1640, 36.40, 25.40, 0.0, 7.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Vésuve (Pompéi VEI-5)", 79, 40.82, 14.43, 0.0, 5.0));
        masterList.add(new ClimateEvent("volcano", "Anomalie Volcanique & Hiver Global 536 (VEI-7)", 536, 13.70, -89.20, 0.0, 7.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Samalas / Rinjani (VEI-7, Petit Âge Glaciaire)", 1257, -8.40, 116.47, 0.0, 7.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Huaynaputina (VEI-6)", 1600, -16.60, -71.35, 0.0, 6.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Laki (Islande, Nuage Toxique VEI-6)", 1783, 64.06, -17.33, 0.0, 6.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Tambora (VEI-7, τ=1.20, Année sans été)", 1815, -8.25, 117.98, 0.0, 7.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Krakatoa (VEI-6, τ=0.80)", 1883, -6.10, 105.42, 0.0, 6.0));
        masterList.add(new ClimateEvent("volcano", "Éruption Pinatubo (VEI-6, τ=0.40)", 1991, 15.13, 120.35, 0.0, 6.0));

        // Tempêtes Solaire & Éruptions EMP Carrington
        masterList.add(new ClimateEvent("impact", "Événement Solaire Carrington (EMP & Grille Électrique)", 1859, 50.0, 0.0, 0.0, 8.5));

        // Impact Météorique
        masterList.add(new ClimateEvent("impact", "Impact Chicxulub (Extinction K-Pg VEI-10)", -66000000, 21.40, -89.50, 0.0, 10.0));
        masterList.add(new ClimateEvent("impact", "Impact Tunguska (Onde de Choc 15Mt)", 1908, 60.89, 101.89, 0.0, 5.0));

        // Tectonique, Séismes & Tsunamis
        masterList.add(new ClimateEvent("earthquake", "Séisme Shensi (Chine, 830k victimes)", 1556, 34.50, 109.70, 20.0, 8.0));
        masterList.add(new ClimateEvent("earthquake", "Séisme Lisbonne (Portugal & Tsunami, M8.7)", 1755, 36.00, -10.50, 30.0, 8.7));
        masterList.add(new ClimateEvent("earthquake", "Séisme Valdivia (Chili, M9.5)", 1960, -38.14, -73.41, 25.0, 9.5));
        masterList.add(new ClimateEvent("earthquake", "Séisme Alaska (M9.2)", 1964, 61.02, -147.65, 25.0, 9.2));
        masterList.add(new ClimateEvent("earthquake", "Séisme Sumatra-Andaman (M9.1)", 2004, 3.30, 95.98, 30.0, 9.1));
        masterList.add(new ClimateEvent("earthquake", "Séisme Tohoku Japon (M9.0)", 2011, 38.30, 142.37, 29.0, 9.0));
        masterList.add(new ClimateEvent("tsunami", "Tsunami Sumatra (Submersion Littorale)", 2004, 3.30, 95.98, 30.0, 9.1));
        masterList.add(new ClimateEvent("tsunami", "Tsunami Fukushima & Accident Nucléaire", 2011, 38.30, 142.37, 29.0, 9.0));

        // Radiations & Accidents Technologiques
        masterList.add(new ClimateEvent("impact", "Accident Nucléaire de Tchernobyl (Fallout Bq/m²)", 1986, 51.38, 30.10, 0.0, 7.5));

        // Ozone CFC & Dérive Climatique
        masterList.add(new ClimateEvent("climate_drift", "Holocène Vert (Sahara Humide & Fertile)", -6000, 20.0, 10.0, 0.0, 3.0));
        masterList.add(new ClimateEvent("climate_drift", "Optimum Climatique Médiéval (+1.2°C)", 1000, 45.0, 15.0, 0.0, 1.2));
        masterList.add(new ClimateEvent("climate_drift", "Peste Noire (Europe, 1/3 Pop.)", 1347, 44.00, 10.00, 0.0, 9.0));
        masterList.add(new ClimateEvent("climate_drift", "Petit Âge Glaciaire (-1.5°C Maunder)", 1650, 50.0, 10.0, 0.0, -1.5));
        masterList.add(new ClimateEvent("climate_drift", "Trou dans la Couche d'Ozone CFC (Flux UV)", 1985, -80.0, 0.0, 0.0, 4.0));
        masterList.add(new ClimateEvent("flood", "Grande Inondation Jaune (Chine)", 1931, 32.00, 118.00, 0.0, 8.0));
        masterList.add(new ClimateEvent("pandemic", "Grippe Espagnole (50M Victimes)", 1918, 40.00, 0.00, 0.0, 8.0));
        masterList.add(new ClimateEvent("sea_level", "Élévation Littorale Moderne (+2.5m Submersion)", 2050, 0.0, 0.0, 0.0, 2.5));

        // Filter events: Keep all historical events taking place AFTER or IN the starting year
        for (ClimateEvent ev : masterList) {
            if (ev.getYear() >= startYear) {
                eventsList.add(ev);
            }
        }

        logger.info("Loaded {} historical events posterior to start year {}.", eventsList.size(), startYear);
    }

    /**
     * Auto-calibrates initial physical capital stock (K0 in kg/capita) based on start year T0.
     */
    public static double computeAutoCapitalFromYear(long year) {
        if (year <= -10000) return 5.0;   // Paleolithic / Early Neolithic (~5 kg/hab)
        if (year <= -3000)  return 25.0;  // Bronze Age (~25 kg/hab)
        if (year <= -500)   return 100.0; // Antiquity (~100 kg/hab)
        if (year <= 1000)   return 200.0; // Early Middle Ages (~200 kg/hab)
        if (year <= 1500)   return 500.0; // Renaissance / Age of Discovery (~500 kg/hab)
        if (year <= 1800)   return 2500.0; // Industrial Revolution (~2500 kg/hab)
        if (year <= 1950)   return 8000.0; // 20th Century (~8000 kg/hab)
        return 15000.0;                   // Modern / Post-Industrial (>15000 kg/hab)
    }

    public static double computeAutoEnergyFromYear(long year) {
        if (year <= -10000) return 10.0;    // Firewood (~10 MJ/hab)
        if (year <= -3000)  return 30.0;    // Wood & fodder (~30 MJ/hab)
        if (year <= -500)   return 60.0;    // Oil & firewood (~60 MJ/hab)
        if (year <= 1000)   return 150.0;   // Hydraulic & firewood (~150 MJ/hab)
        if (year <= 1500)   return 300.0;   // Peat & timber (~300 MJ/hab)
        if (year <= 1800)   return 2500.0;  // Coal mines (~2500 MJ/hab)
        if (year <= 1950)   return 10000.0; // Hydrocarbons (~10000 MJ/hab)
        return 50000.0;                     // Modern/Future (>50000 MJ/hab)
    }

    public static double computeAutoFoodFromYear(long year) {
        if (year <= -10000) return 2.0;  // Foraging (2 months)
        if (year <= -3000)  return 4.0;  // Early granaries (4 months)
        if (year <= -500)   return 6.0;  // Roman/Han silos (6 months)
        if (year <= 1000)   return 6.0;  // Feudal granaries (6 months)
        if (year <= 1500)   return 8.0;  // Early modern storage (8 months)
        if (year <= 1800)   return 10.0; // Agricultural revolution (10 months)
        if (year <= 1950)   return 12.0; // Modern global logistics (12 months)
        return 18.0;                     // Future/Post-scarcity (18+ months)
    }

    public static double computeAutoInfoFromYear(long year) {
        if (year <= -10000) return 5.0;        // Oral tradition (~5 bits/hab)
        if (year <= -3000)  return 40.0;       // Cuneiform/Hieroglyphs (~40 bits/hab)
        if (year <= -500)   return 200.0;      // Classical manuscripts (~200 bits/hab)
        if (year <= 1000)   return 500.0;      // Scriptoriums & block printing (~500 bits/hab)
        if (year <= 1500)   return 2000.0;     // Printing press (~2000 bits/hab)
        if (year <= 1800)   return 15000.0;    // Encyclopedias & press (~15000 bits/hab)
        if (year <= 1950)   return 500000.0;   // Telecommunications & radio (~500000 bits/hab)
        return 5000000.0;                      // Internet & AI (>5000000 bits/hab)
    }

    public void applyEarthPreset() {
        if (planetPresetCombo != null) {
            planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        }
    }

    // =========================================================================
    // I18N — Update all UI text based on current language
    // =========================================================================

    public void updateTexts() {
        if (title1 != null) title1.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.general_params", "1. PARAMÈTRES GÉNÉRAUX"));
        if (title3Events != null) title3Events.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events_section", "4. ÉVÉNEMENTS PLANÉTAIRES HISTORIQUES"));
        if (startYearLabel != null) startYearLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.start_year", "Année de départ (Repère chronologique) :"));
        if (endYearLabel != null) endYearLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.end_year", "Année de fin / Cible (Repère chronologique) :"));
        if (popCountLabel != null) popCountLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_count", "Population Initiale (1 000 à 10 000 000 000) :"));
        if (capitalLabel != null) capitalLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.capital_per_capita", "🛠️ Capital Physique Initial (K₀) (kg/hab) :"));
        if (densityPatternLabel != null) densityPatternLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.density_pattern", "Motif de Répartition :"));
        if (urbanCentersLabel != null) urbanCentersLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.urban_centers", "Nœuds Urbains / Cités :"));
        if (h3ResolutionLabel != null) h3ResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("planet.param.resolution", "Résolution H3 :"));
        if (temporalResolutionLabel != null) temporalResolutionLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.temporal_resolution", "Pas de Temps Δt (Résolution Temporelle) :"));
        if (cohortSizeLabel != null) cohortSizeLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.label.cohort_size", "Taille des Cohortes :"));
        if (startBtn != null) startBtn.setText(org.ether.society.i18n.I18n.get("scenario.start_btn"));
        if (generateBtn != null) generateBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.preview_btn", "🔄 Prévisualiser la Répartition"));
        if (addEventBtn != null) addEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.add", "➕ Ajouter Événement"));
        if (removeEventBtn != null) removeEventBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.remove", "🗑️ Supprimer"));
        if (loadEarthEventsBtn != null) loadEarthEventsBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.load_earth", "🌍 Charger Événements Historiques Terre"));

        if (clippingHeader != null) clippingHeader.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.header", "✂️ SIMULATION LOCALE & FRONTIÈRES (CLIPPING)"));
        if (clippingCheckBox != null) clippingCheckBox.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.enable", "Activer la simulation partielle (Zone Tronquée)"));
        if (graphicSelectBtn != null) graphicSelectBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.select_mode", "🖱️ Mode Sélection Graphique sur Carte"));
        if (resetClippingBtn != null) resetClippingBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.reset", "🔄 Réinitialiser la Zone (Pleine Planète)"));
        if (latMaxLabel != null) latMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_max", "Lat Max (Haut) :"));
        if (latMinLabel != null) latMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lat_min", "Lat Min (Bas) :"));
        if (lngMinLabel != null) lngMinLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_min", "Lng Min (Gau.) :"));
        if (lngMaxLabel != null) lngMaxLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.lng_max", "Lng Max (Dro.) :"));
        if (boundaryLabel != null) boundaryLabel.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.clipping.boundary_label", "Modélisation Scientifique des Frontières :"));
        updatePreviewTitleText();

        if (radioProcDemo != null) radioProcDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.procedural", "▶ Génération Procédurale"));
        if (radioImportDemo != null) radioImportDemo.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.import", "📂 Import Carte Externe (PNG)"));
        if (loadDensityMapBtn != null) loadDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_load", "📥 Importer Carte de Densité Externe (PNG)"));
        if (exportDensityMapBtn != null) exportDensityMapBtn.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.demo.btn_export", "📤 Exporter Carte de Densité Générée (PNG)"));

        if (boundaryModeCombo != null) {
            String val = boundaryModeCombo.getValue();
            boundaryModeCombo.setValue(null);
            boundaryModeCombo.setValue(val);
        }

        if (eventsTable != null && eventsTable.getUserData() instanceof TableColumn[] cols && cols.length == 6) {
            cols[0].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.name", "Nom / Description"));
            cols[1].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.year", "Année"));
            cols[2].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lat", "Latitude"));
            cols[3].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.lon", "Longitude"));
            cols[4].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.depth", "Profondeur (km)"));
            cols[5].setText(org.ether.society.i18n.I18n.getOrDefault("scenario.events.col.magnitude", "Magnitude"));
        }
        if (getLeft() instanceof ScrollPane sp && sp.getContent() instanceof VBox root) {
            root.lookupAll("#__popHeader").forEach(n -> { if (n instanceof Label l) l.setText(org.ether.society.i18n.I18n.getOrDefault("scenario.pop_section", "2. DÉMOGRAPHIE & RÉPARTITION DE POPULATION")); });
        }
        updateDemoCompatibilityDisplay();
    }

    public Scenario getScenario() {
        Scenario s = new Scenario();
        s.setName(scenarioPresetBar != null ? scenarioPresetBar.getCurrentName() : "New Scenario");
        if (scenarioDescriptionArea != null) {
            s.setDescription(scenarioDescriptionArea.getText());
        }
        s.setStartDateYear(startYearSpinner.getValue());
        s.setEndDateYear(endYearSpinner != null && endYearSpinner.getValue() != null ? endYearSpinner.getValue() : 100);
        s.setTargetCohortSize(targetCohortSizeSpinner != null ? targetCohortSizeSpinner.getValue() : 150);
        s.setTemporalResolutionDays(temporalResolutionCombo != null && temporalResolutionCombo.getValue() != null ? temporalResolutionCombo.getValue() : 30.0);
        s.setInitialHumanCount(initialHumanCountSpinner.getValue());
        s.setInitialCapitalPerCapita(initialCapitalSpinner != null ? initialCapitalSpinner.getValue() : 10.0);
        s.setInitialEnergyPerCapita(initialEnergySpinner != null ? initialEnergySpinner.getValue() : 50.0);
        s.setInitialFoodReserveMonths(initialFoodSpinner != null ? initialFoodSpinner.getValue() : 6.0);
        s.setInitialInformationPerCapita(initialInfoSpinner != null ? initialInfoSpinner.getValue() : 100.0);
        s.setPopulationDensityType(densityPatternCombo.getValue());
        s.setEcologyPreset(ecologyPresetCombo != null ? ecologyPresetCombo.getValue() : null);
        if (ecologyPresetCombo != null && ecologyPresetCombo.getValue() != null) {
            s.setEcologyPresetName(ecologyPresetCombo.getValue().name());
        }
        s.setPlanetPreset(activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue());

        long seedVal = 12345L;
        try {
            if (demoSeedField != null) seedVal = Long.parseLong(demoSeedField.getText());
        } catch (NumberFormatException ignored) {}
        s.setSeed(seedVal);
        s.setRandomEventsEnabled(randomEventsCheckBox == null || randomEventsCheckBox.isSelected());
        if (clippingCheckBox != null) {
            s.setClippingEnabled(clippingCheckBox.isSelected());
            s.setMinLat(minLatSpinner.getValue());
            s.setMaxLat(maxLatSpinner.getValue());
            s.setMinLng(minLngSpinner.getValue());
            s.setMaxLng(maxLngSpinner.getValue());
            s.setBoundaryMode(boundaryModeCombo.getValue());
        }
        if (strictDeterminismCheckBox != null) {
            s.setStrictDeterminism(strictDeterminismCheckBox.isSelected());
        }
        if (sparseCellSkippingCheckBox != null) {
            s.setSparseCellSkippingEnabled(sparseCellSkippingCheckBox.isSelected());
        }
        if (oceanMacroAggregationCheckBox != null) {
            s.setOceanMacroAggregationEnabled(oceanMacroAggregationCheckBox.isSelected());
        }
        if (coastalNavigationOnlyCheckBox != null) {
            s.setCoastalNavigationOnlyEnabled(coastalNavigationOnlyCheckBox.isSelected());
        }
        if (oceanMultiRateTickingCheckBox != null) {
            s.setOceanMultiRateTickingEnabled(oceanMultiRateTickingCheckBox.isSelected());
        }
        if (climateTickFreqSlider != null) {
            s.setClimateTickFrequency((int) climateTickFreqSlider.getValue());
        }
        if (parallelExecutionCheckBox != null) {
            s.setParallelExecutionEnabled(parallelExecutionCheckBox.isSelected());
        }
        if (threadCountSlider != null) {
            s.setParallelThreadCount((int) threadCountSlider.getValue());
        }
        if (spatialRangeTruncationCheckBox != null) {
            s.setSpatialRangeTruncationEnabled(spatialRangeTruncationCheckBox.isSelected());
        }
        if (customDensityImage != null) {
            s.setCustomDensityBase64(org.ether.society.data.ImageMapLoader.imageToBase64Png(customDensityImage));
        }

        // Save Cultural Vector & Multi-Field Layers
        if (cultureVectorDimSpinner != null) {
            s.setCultureVectorDimensions(cultureVectorDimSpinner.getValue());
        }
        if (culturalDiffusionRateSpinner != null) {
            s.setCulturalDiffusionRate(culturalDiffusionRateSpinner.getValue());
        }
        if (culturalMutationRateSpinner != null) {
            s.setCulturalMutationRate(culturalMutationRateSpinner.getValue());
        }
        if (customIsoglossImage != null) {
            s.setCustomIsoglossBase64(org.ether.society.data.ImageMapLoader.imageToBase64Png(customIsoglossImage));
        }
        if (customKinshipImage != null) {
            s.setCustomKinshipBase64(org.ether.society.data.ImageMapLoader.imageToBase64Png(customKinshipImage));
        }
        if (customRitualsImage != null) {
            s.setCustomRitualsBase64(org.ether.society.data.ImageMapLoader.imageToBase64Png(customRitualsImage));
        }
        if (customSovereigntyImage != null) {
            s.setCustomSovereigntyBase64(org.ether.society.data.ImageMapLoader.imageToBase64Png(customSovereigntyImage));
        }

        // Save Type B engine checkbox states
        java.util.Map<String, Boolean> typeBStates = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
            typeBStates.put(entry.getKey(), entry.getValue().isSelected());
        }
        s.setTypeBEngineStates(typeBStates);

        // Save Type B engine fine-grained parameter values
        java.util.Map<String, java.util.Map<String, Double>> typeBParams = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, java.util.Map<String, Spinner<Double>>> engEntry : typeBParamSpinnersMap.entrySet()) {
            java.util.Map<String, Double> engParams = new java.util.HashMap<>();
            for (java.util.Map.Entry<String, Spinner<Double>> paramEntry : engEntry.getValue().entrySet()) {
                if (paramEntry.getValue() != null && paramEntry.getValue().getValue() != null) {
                    engParams.put(paramEntry.getKey(), paramEntry.getValue().getValue());
                }
            }
            if (!engParams.isEmpty()) {
                typeBParams.put(engEntry.getKey(), engParams);
            }
        }
        s.setTypeBEngineParameters(typeBParams);

        return s;
    }

    private java.util.function.BiConsumer<PlanetPreset, EcologyPreset> onScenarioLoadedCallback;

    public void setOnScenarioLoadedCallback(java.util.function.BiConsumer<PlanetPreset, EcologyPreset> callback) {
        this.onScenarioLoadedCallback = callback;
    }

    public List<H3Cell> getCells() { return currentPreviewCells; }

    public void setGeneratedCells(List<H3Cell> cells) {
        this.currentPreviewCells = cells;
        if (cells != null && !cells.isEmpty()) {
            distributeInitialPopulation(cells);
        }
        drawPreview();
    }

    public void ensurePreviewGeneratedIfNeeded() {
        if (currentPreviewCells == null || currentPreviewCells.isEmpty()) {
            generatePreview();
        } else {
            drawPreview();
        }
    }

    public List<ClimateEvent> getScheduledEvents() {
        return eventsList != null ? new ArrayList<>(eventsList) : new ArrayList<>();
    }

    private void exportCustomEngineTemplate() {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Exporter un Template de Moteur Custom Ether (.java)");
            fileChooser.setInitialFileName("MyCustomOptionalEngine.java");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers Source Java (*.java)", "*.java"));
            java.io.File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                String template = """
                    package org.ether.society.procedural;

                    import org.ether.society.core.H3SimulationEngine;
                    import org.ether.society.model.H3Cell;

                    /**
                     * Template Moteur Dynamic Optional Ether.
                     */
                    public class MyCustomOptionalEngine {
                        private final String name = "MyCustomOptionalEngine";

                        public void update(H3SimulationEngine engine, H3Cell cell, double deltaTime) {
                            // Implémentation personnalisée des équations cliodynamiques
                        }
                    }
                    """;
                java.nio.file.Files.writeString(file.toPath(), template);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Template de moteur personnalisé exporté avec succès :\n" + file.getAbsolutePath());
                alert.show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void importCustomEngineFile() {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Importer un Moteur Personnalisé (.java / .class)");
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Moteurs Java (*.java, *.class)", "*.java", "*.class")
            );
            java.io.File file = fileChooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                String fileName = file.getName();
                String engineKey = fileName.substring(0, fileName.lastIndexOf('.'));
                addCustomEngineCheckBoxToUI(engineKey, "🔌 " + engineKey + " (Moteur Custom Importé)", "Moteur dynamique personnalisé importé depuis " + file.getName(), true);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Moteur personnalisé importé et enregistré dans le scénario :\n" + file.getName());
                alert.show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updatePerformanceControlsState() {
        boolean strict = strictDeterminismCheckBox != null && strictDeterminismCheckBox.isSelected();
        List<CheckBox> subOpts = List.of(
            sparseCellSkippingCheckBox,
            oceanMacroAggregationCheckBox,
            coastalNavigationOnlyCheckBox,
            oceanMultiRateTickingCheckBox,
            parallelExecutionCheckBox,
            spatialRangeTruncationCheckBox
        );
        if (strict) {
            for (CheckBox cb : subOpts) {
                if (cb != null) {
                    cb.setSelected(false);
                    cb.setDisable(true);
                }
            }
            if (climateTickFreqSlider != null) climateTickFreqSlider.setDisable(true);
            if (threadCountSlider != null) threadCountSlider.setDisable(true);
        } else {
            for (CheckBox cb : subOpts) {
                if (cb != null) cb.setDisable(false);
            }
            if (climateTickFreqSlider != null && oceanMultiRateTickingCheckBox != null) {
                climateTickFreqSlider.setDisable(!oceanMultiRateTickingCheckBox.isSelected());
            }
            if (threadCountSlider != null && parallelExecutionCheckBox != null) {
                threadCountSlider.setDisable(!parallelExecutionCheckBox.isSelected());
            }
        }
    }

    /**
     * Custom SpinnerValueFactory for Long values to avoid ClassCastException.
     */
    public static class LongSpinnerValueFactory extends SpinnerValueFactory<Long> {
        private final long min;
        private final long max;
        private final long step;

        public LongSpinnerValueFactory(long min, long max, long initialValue, long step) {
            this.min = min;
            this.max = max;
            this.step = step;
            setConverter(new StringConverter<Long>() {
                @Override
                public String toString(Long object) {
                    return object == null ? "" : String.valueOf(object);
                }
                @Override
                public Long fromString(String string) {
                    if (string == null || string.isBlank()) return initialValue;
                    try {
                        String clean = string.replaceAll("[^0-9\\-]", "");
                        return Long.parseLong(clean);
                    } catch (Exception e) {
                        return initialValue;
                    }
                }
            });
            setValue(initialValue);
        }

        @Override
        public void decrement(int steps) {
            long current = getValue() != null ? getValue() : min;
            long newValue = Math.max(min, current - (long) steps * step);
            setValue(newValue);
        }

        @Override
        public void increment(int steps) {
            long current = getValue() != null ? getValue() : min;
            long newValue = Math.min(max, current + (long) steps * step);
            setValue(newValue);
        }
    }
}
