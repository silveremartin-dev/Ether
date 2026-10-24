/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.ScenarioSetupPanel.LongSpinnerValueFactory;
import org.ether.society.ui.WindowUtils;
import org.ether.society.ui.util.DataSourceUIHelper;
import org.ether.society.ui.util.ScenarioCellPopulationDistributor;
import org.ether.society.ui.util.UIControlsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Controller and UI builder for the Initial Population Identity Card and Demographic Density section.
 * <p>
 * Manages cellular demographic allocation:
 * \[
 *   P_i(t_0) = P_{\text{total}} \cdot \frac{\mathcal{W}(\mathbf{x}_i, \mathbf{b}_i, K_0)}{\sum_{j} \mathcal{W}(\mathbf{x}_j, \mathbf{b}_j, K_0)}
 * \]
 * supporting procedural environmental suitability weighting, pre-calculated historical rasters (HYDE 3.4),
 * and custom equirectangular PNG density imports.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioDemographicSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioDemographicSectionBuilder.class);

    public static final Map<String, String> DENSITY_LABELS = Map.ofEntries(
        Map.entry("UNBIASED_NATURAL", "⚖ Natural Bio-Climatic Equilibrium (Unbiased)"),
        Map.entry("COASTAL_MARITIME", "🌊 Coastal & Maritime Focus"),
        Map.entry("RIVER_VALLEYS", "🏞 River Valleys & Alluvial Basins"),
        Map.entry("HIGHLAND_MOUNTAIN", "🏔 Highland Refuges & Mountain Reliefs"),
        Map.entry("INLAND_OASIS", "🌴 Inland Hydrographic Basins & Oases"),
        Map.entry("EQUATORIAL_BELT", "☀️ Equatorial Belt & Tropical Zone"),
        Map.entry("URBAN_CLUSTERS", "🏙 Metropolises & Urban Clusters"),
        Map.entry("SPARSE_NOMADIC", "⛺ Sparse Nomadic & Pastoralist Dispersion"),
        Map.entry("UNIFORM", "🟦 Absolute Uniform Distribution"),
        Map.entry("RANDOM", "🎲 Stochastic Random Distribution")
    );

    public static final Map<String, String> DENSITY_DESCRIPTIONS = Map.ofEntries(
        Map.entry("UNBIASED_NATURAL", "⚖ Natural Bio-Climatic Equilibrium: Pure physical model without artificial bias. Population settles strictly according to real environmental suitability (biomes, temperature, waterways, relief)."),
        Map.entry("COASTAL_MARITIME", "🌊 Coastal & Maritime Focus: Encourages human settlement along ocean coastlines, river deltas, and maritime margins."),
        Map.entry("RIVER_VALLEYS", "🏞 River Valleys & Alluvial Basins: High population concentration along major hydrographic river networks and fertile alluvial basins."),
        Map.entry("HIGHLAND_MOUNTAIN", "🏔 Highland Refuges & Mountain Reliefs: Preferential densification in high valleys, mountain plateaus, and defensive elevated reliefs."),
        Map.entry("INLAND_OASIS", "🌴 Inland Basins & Oases: Settlement around endorheic depressions and accessible groundwater tables."),
        Map.entry("EQUATORIAL_BELT", "☀️ Equatorial Belt: Primary settlement concentrated around equatorial latitudes and high solar irradiance zones."),
        Map.entry("URBAN_CLUSTERS", "🏙 Metropolises & Urban Clusters: Procedural emergence of multiple dense urban aggregation hubs."),
        Map.entry("SPARSE_NOMADIC", "⛺ Sparse Nomadic Dispersion: Low-density pastoral population dispersed broadly across all viable biomes."),
        Map.entry("UNIFORM", "🟦 Absolute Uniform Distribution: Strictly constant population density across all land cells on the H3 grid."),
        Map.entry("RANDOM", "🎲 Stochastic Distribution: Uniformly random population allocation across land cells.")
    );

    /**
     * Callback interface providing host panel context.
     */
    public interface HostContext {
        void notifyParamChange();
        void drawPreview();
        void selectPreviewMode(int modeIndex);
        List<H3Cell> getPreviewCells();
        void distributeInitialPopulation(List<H3Cell> cells);
        Scenario getScenario();
        PlanetPreset getActivePlanetPreset();
        Window getWindow();
        boolean isUpdatingFromPreset();
        long getStartYear();
        double computeAutoCapitalFromYear(long year);
        void generatePreview();
        void updatePreviewTitleText();
        void updateBottomLegend();
    }

    private final HostContext context;

    // Demographic Controls
    private Label popHeader;
    private RadioButton radioProcDemo;
    private RadioButton radioImportDemo;
    private Spinner<Long> initialHumanCountSpinner;
    private ComboBox<String> densityPatternCombo;
    private ComboBox<Scenario.TechPreset> techPresetCombo;
    private Spinner<Double> customCapitalSpinner;
    private Spinner<Double> customEnergySpinner;
    private Spinner<Double> customFoodSpinner;
    private Spinner<Double> customInfoSpinner;
    private VBox customPhysicalSubPanel;

    // Density Map Import/Export Controls
    private ComboBox<String> demoSourceCombo;
    private Label demoSourceLabel;
    private Image customDensityImage;
    private Label densityMapFileLabel;
    private Label demoCompatibilityLabel;
    private Button loadDensityMapBtn;
    private Button clearDensityMapBtn;
    private Button demoHelpBtn;
    private Label densityFormatHintLabel;
    private Button exportDensityMapBtn;

    public ScenarioDemographicSectionBuilder(HostContext context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    /**
     * Builds and returns the demographic configuration VBox section.
     *
     * @return JavaFX VBox container for population parameters
     */
    public VBox buildSection() {
        VBox popSection = new VBox(10);
        popSection.getStyleClass().add("card-section");

        popHeader = new Label(I18n.getOrDefault("scenario.pop_section", "👥 2. INITIAL POPULATION IDENTITY CARD"));
        popHeader.getStyleClass().add("label-section-header");
        popHeader.setId("__popHeader");

        ToggleGroup demoSourceGroup = new ToggleGroup();
        radioProcDemo = new RadioButton(I18n.getOrDefault("scenario.radio.procedural", "▶ Procedural Mode (Algorithms & Demographic Patterns)"));
        radioImportDemo = new RadioButton(I18n.getOrDefault("scenario.radio.import", "📂 Loading Mode for Existing Density Map (PNG)"));
        radioProcDemo.setToggleGroup(demoSourceGroup);
        radioImportDemo.setToggleGroup(demoSourceGroup);
        radioProcDemo.setSelected(true);

        // Procedural Sub-Panel
        initialHumanCountSpinner = new Spinner<>(new LongSpinnerValueFactory(1_000L, 10_000_000_000L, 1_000_000L, 100_000L));
        initialHumanCountSpinner.setEditable(true);
        initialHumanCountSpinner.setMaxWidth(Double.MAX_VALUE);
        initialHumanCountSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            context.notifyParamChange();
            if (!context.isUpdatingFromPreset() && newV != null && context.getPreviewCells() != null && !context.getPreviewCells().isEmpty()) {
                context.distributeInitialPopulation(context.getPreviewCells());
                context.drawPreview();
            }
        });

        densityPatternCombo = new ComboBox<>();
        densityPatternCombo.getItems().addAll("UNBIASED_NATURAL", "COASTAL_MARITIME", "RIVER_VALLEYS", "HIGHLAND_MOUNTAIN", "INLAND_OASIS", "EQUATORIAL_BELT", "URBAN_CLUSTERS", "SPARSE_NOMADIC", "UNIFORM", "RANDOM");
        densityPatternCombo.setValue("UNBIASED_NATURAL");
        densityPatternCombo.setMaxWidth(Double.MAX_VALUE);
        densityPatternCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(String item) {
                return item == null ? "" : I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item));
            }
            @Override
            public String fromString(String string) {
                return null;
            }
        });
        densityPatternCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item)));
                    String desc = I18n.getOrDefault("scenario.density.desc." + item, DENSITY_DESCRIPTIONS.getOrDefault(item, ""));
                    setTooltip(desc.isBlank() ? null : new Tooltip(desc));
                }
            }
        });
        densityPatternCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(I18n.getOrDefault("scenario.density." + item, DENSITY_LABELS.getOrDefault(item, item)));
                }
            }
        });
        densityPatternCombo.setTooltip(new Tooltip(I18n.getOrDefault("scenario.density.desc.UNBIASED_NATURAL", DENSITY_DESCRIPTIONS.get("UNBIASED_NATURAL"))));
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> {
            context.notifyParamChange();
            if (newV != null) {
                densityPatternCombo.setTooltip(new Tooltip(I18n.getOrDefault("scenario.density.desc." + newV, DENSITY_DESCRIPTIONS.getOrDefault(newV, ""))));
            }
        });

        techPresetCombo = new ComboBox<>();
        techPresetCombo.getItems().setAll(Scenario.TechPreset.values());
        techPresetCombo.setValue(Scenario.TechPreset.AUTO_FROM_YEAR);
        techPresetCombo.setMaxWidth(Double.MAX_VALUE);
        techPresetCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Scenario.TechPreset item) {
                return item == null ? "" : I18n.getOrDefault("scenario.tech." + item.name(), item.getLabel());
            }
            @Override
            public Scenario.TechPreset fromString(String string) {
                return null;
            }
        });
        techPresetCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Scenario.TechPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(I18n.getOrDefault("scenario.tech." + item.name(), item.getLabel()));
                }
            }
        });
        techPresetCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Scenario.TechPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(I18n.getOrDefault("scenario.tech." + item.name(), item.getLabel()));
                }
            }
        });
        techPresetCombo.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.tech_preset", "Select initial physical endowment (K₀, E₀, F₀, I₀) or leave auto-calculated based on T₀ year.")));
        techPresetCombo.valueProperty().addListener((obs, oldV, newV) -> context.notifyParamChange());

        Label popCountLabel = new Label(I18n.getOrDefault("scenario.label.initial_pop", "Initial Population:"));
        Label densityPatternLabel = new Label(I18n.getOrDefault("scenario.label.density_pattern", "Settlement Pattern:"));
        Label techPresetLabel = new Label(I18n.getOrDefault("scenario.label.tech_preset", "Physical Capital & Energy Regime:"));

        GridPane popGrid = new GridPane();
        popGrid.setHgap(8);
        popGrid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(40);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(60);
        popGrid.getColumnConstraints().addAll(c1, c2);

        popGrid.addRow(0, popCountLabel, initialHumanCountSpinner);
        popGrid.addRow(1, densityPatternLabel, densityPatternCombo);
        popGrid.addRow(2, techPresetLabel, techPresetCombo);

        customCapitalSpinner = new Spinner<>(0.0, 10_000.0, 120.0, 10.0);
        customCapitalSpinner.setEditable(true);
        customCapitalSpinner.setMaxWidth(Double.MAX_VALUE);
        customCapitalSpinner.valueProperty().addListener((obs, oldV, newV) -> context.notifyParamChange());

        customEnergySpinner = new Spinner<>(0.0, 10_000_000.0, 300.0, 50.0);
        customEnergySpinner.setEditable(true);
        customEnergySpinner.setMaxWidth(Double.MAX_VALUE);
        customEnergySpinner.valueProperty().addListener((obs, oldV, newV) -> context.notifyParamChange());

        customFoodSpinner = new Spinner<>(0.0, 120.0, 6.0, 1.0);
        customFoodSpinner.setEditable(true);
        customFoodSpinner.setMaxWidth(Double.MAX_VALUE);
        customFoodSpinner.valueProperty().addListener((obs, oldV, newV) -> context.notifyParamChange());

        customInfoSpinner = new Spinner<>(0.0, 100_000_000_000.0, 2000.0, 100.0);
        customInfoSpinner.setEditable(true);
        customInfoSpinner.setMaxWidth(Double.MAX_VALUE);
        customInfoSpinner.valueProperty().addListener((obs, oldV, newV) -> context.notifyParamChange());

        GridPane customGrid = new GridPane();
        customGrid.setHgap(8);
        customGrid.setVgap(8);
        ColumnConstraints cgCol1 = new ColumnConstraints();
        cgCol1.setPercentWidth(25);
        ColumnConstraints cgCol2 = new ColumnConstraints();
        cgCol2.setPercentWidth(25);
        ColumnConstraints cgCol3 = new ColumnConstraints();
        cgCol3.setPercentWidth(25);
        ColumnConstraints cgCol4 = new ColumnConstraints();
        cgCol4.setPercentWidth(25);
        customGrid.getColumnConstraints().setAll(cgCol1, cgCol2, cgCol3, cgCol4);

        customGrid.addRow(0, new Label(I18n.getOrDefault("scenario.label.stock_k0", "🛠️ K₀ (kg/hab) :")), customCapitalSpinner, new Label(I18n.getOrDefault("scenario.label.stock_e0", "⚡ E₀ (MJ/hab) :")), customEnergySpinner);
        customGrid.addRow(1, new Label(I18n.getOrDefault("scenario.label.stock_f0", "🌾 F₀ (mois) :")), customFoodSpinner, new Label(I18n.getOrDefault("scenario.label.stock_i0", "🧠 I₀ (bits/hab) :")), customInfoSpinner);

        Label customTitleLabel = new Label(I18n.getOrDefault("scenario.section.custom_stocks", "⚙️ MANUAL INITIAL PHYSICAL STOCKS SETUP (CUSTOM):"));
        customTitleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        customPhysicalSubPanel = new VBox(6, customTitleLabel, customGrid);
        customPhysicalSubPanel.setStyle("-fx-padding: 8px; -fx-background-color: rgba(245, 158, 11, 0.05); -fx-background-radius: 6px; -fx-border-color: rgba(245, 158, 11, 0.25); -fx-border-radius: 6px; -fx-border-width: 1px;");
        customPhysicalSubPanel.setVisible(false);
        customPhysicalSubPanel.setManaged(false);

        techPresetCombo.valueProperty().addListener((obs, oldV, newV) -> {
            context.notifyParamChange();
            boolean isCustom = (newV == Scenario.TechPreset.CUSTOM);
            customPhysicalSubPanel.setVisible(isCustom);
            customPhysicalSubPanel.setManaged(isCustom);
        });

        exportDensityMapBtn = new Button(I18n.getOrDefault("scenario.btn.export_map_png", "📤 Export Raster (PNG / JPEG)"));
        exportDensityMapBtn.getStyleClass().add("button-secondary");
        exportDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        exportDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_map_png", "Export demographic density map as a high-resolution PNG or JPEG raster file.")));
        exportDensityMapBtn.setOnAction(e -> exportDensityMap());

        HBox demoBtnBar = new HBox(8, exportDensityMapBtn);
        HBox.setHgrow(exportDensityMapBtn, Priority.ALWAYS);

        VBox proceduralDemoPanel = new VBox(8, popGrid, customPhysicalSubPanel, demoBtnBar);
        proceduralDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        // Import Panel
        Label sourceRefLabel = new Label("📍 " + I18n.getOrDefault("resource.label.reference_source", "Source de Référence :"));
        sourceRefLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
        demoSourceLabel = sourceRefLabel;
        demoSourceCombo = buildPopulationSourceCombo();

        densityMapFileLabel = new Label(I18n.getOrDefault("planet.map.none", "Aucun fichier chargé"));
        densityMapFileLabel.getStyleClass().add("value-label");
        densityMapFileLabel.setStyle("-fx-font-size: 10px;");

        demoCompatibilityLabel = new Label(I18n.getOrDefault("scenario.status.terrain_no_map", "🪐 Terrain validation: No external map loaded"));
        demoCompatibilityLabel.getStyleClass().add("subcard-status-muted");
        demoCompatibilityLabel.setWrapText(true);
        demoCompatibilityLabel.setStyle("-fx-font-size: 11px;");

        densityFormatHintLabel = new Label(I18n.getOrDefault("scenario.format.density_hint",
                "PNG / JPEG (projection équirectangulaire 2:1) :\n  Noir (0) = 0 hab/km² | Blanc (255) = Densité maximale d'habitation."));
        densityFormatHintLabel.setWrapText(true);
        densityFormatHintLabel.getStyleClass().add("card-description-muted");
        densityFormatHintLabel.setStyle("-fx-font-size: 9px; -fx-font-style: italic;");

        loadDensityMapBtn = new Button(I18n.getOrDefault("resource.btn.load_map", "📂 Charger la Carte"));
        loadDensityMapBtn.getStyleClass().add("button-secondary");
        loadDensityMapBtn.setStyle("-fx-font-size: 11px;");
        loadDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_density_map", "Import an external demographic density map in 2:1 equirectangular PNG format.")));
        loadDensityMapBtn.setOnAction(e -> {
            context.notifyParamChange();
            loadCustomDensityMap();
        });

        clearDensityMapBtn = new Button("❌");
        clearDensityMapBtn.getStyleClass().add("button-secondary");
        clearDensityMapBtn.setStyle("-fx-font-size: 11px;");
        clearDensityMapBtn.setOnAction(e -> {
            customDensityImage = null;
            densityMapFileLabel.setText("—");
            if (radioProcDemo != null) {
                radioProcDemo.setSelected(true);
            }
            context.notifyParamChange();
            context.drawPreview();
        });

        demoHelpBtn = new Button(I18n.getOrDefault("scenario.btn.format_specs", "❓ Format"));
        demoHelpBtn.getStyleClass().add("button-secondary");
        demoHelpBtn.setStyle("-fx-font-size: 11px;");
        demoHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        demoHelpBtn.setOnAction(e -> showDensityImportFormatHelp());

        HBox importActionBar = new HBox(6, loadDensityMapBtn, clearDensityMapBtn, demoHelpBtn);
        importActionBar.setAlignment(Pos.CENTER_LEFT);

        VBox importDemoPanel = new VBox(6,
                demoSourceLabel,
                demoSourceCombo,
                new HBox(8, new Label(I18n.getOrDefault("scenario.label.file", "Fichier :")), densityMapFileLabel),
                importActionBar,
                densityFormatHintLabel,
                demoCompatibilityLabel
        );
        importDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        importDemoPanel.setVisible(false);
        importDemoPanel.setManaged(false);

        demoSourceGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            context.notifyParamChange();
            Toggle sel = demoSourceGroup.getSelectedToggle();
            boolean isProc = sel == radioProcDemo;
            proceduralDemoPanel.setVisible(isProc);
            proceduralDemoPanel.setManaged(isProc);
            importDemoPanel.setVisible(!isProc);
            importDemoPanel.setManaged(!isProc);
            context.selectPreviewMode(0);
            if (context.getPreviewCells() != null) {
                context.distributeInitialPopulation(context.getPreviewCells());
            }
            context.drawPreview();
        });

        initialHumanCountSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            context.notifyParamChange();
            if (context.getPreviewCells() != null) {
                context.distributeInitialPopulation(context.getPreviewCells());
                context.drawPreview();
            }
        });
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> {
            context.notifyParamChange();
            if (context.getPreviewCells() != null) {
                context.distributeInitialPopulation(context.getPreviewCells());
                context.drawPreview();
            }
        });

        popSection.getChildren().addAll(popHeader, radioProcDemo, proceduralDemoPanel, radioImportDemo, importDemoPanel);
        return popSection;
    }

    private ComboBox<String> buildPopulationSourceCombo() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().addAll(
            "",
            "🌍 Terre — HYDE 3.4 / Grille Historique Anthropocène [Global, -10 000 BC à +2023 AD]",
            "🌍 Terre — Paléo-Démographie & Expansion Sapiens [Afrique & Eurasie, -100 000 BC à -10 000 BC]",
            "🌍 Terre — CShapes & Centennia Reconstitutions Démographiques [Empires & États, -3000 BC à +2000 AD]",
            "🔴 Mars — Modèle de Colonisation Spatiale & Dômes d'Habitation [Planétaire (Mars), +2050 AD à Futur]",
            "🟡 Vénus — Stations Aérostatiques Cloud Cities (Altitude 50 km) [Planétaire (Vénus), +2100 AD à Futur]",
            "⚪ Lune — Bases Sélénites Sous-Terraines & Cratères Shackleton [Planétaire (Lune), +2040 AD à Futur]",
            "⚪ Mercure — Dômes Polaires & Habitats d'Ombre Permanente [Planétaire (Mercure), +2150 AD à Futur]"
        );
        DataSourceUIHelper.setupDetailedSourceCombo(combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        long startYr = context.getStartYear();
        String initSrc = pickDemoSourceForEpoch(startYr, context.getActivePlanetPreset());
        DataSourceUIHelper.selectBestSource(combo, initSrc, "Terre");
        combo.setOnAction(e -> {
            if (context.isUpdatingFromPreset()) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (customDensityImage != null && radioImportDemo != null) {
                    radioImportDemo.setSelected(true);
                }
                context.notifyParamChange();
                context.drawPreview();
            }
        });
        return combo;
    }

    public String pickDemoSourceForEpoch(long startYear, PlanetPreset planet) {
        String pName = planet != null ? planet.name().toLowerCase() : "";
        if (pName.contains("mars") || pName.contains("ares"))
            return "🔴 Mars — Modèle de Colonisation Spatiale & Dômes d'Habitation [Planétaire (Mars), +2050 AD à Futur]";
        if (pName.contains("vénus") || pName.contains("venus") || pName.contains("hesperos"))
            return "🟡 Vénus — Stations Aérostatiques Cloud Cities (Altitude 50 km) [Planétaire (Vénus), +2100 AD à Futur]";
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene"))
            return "⚪ Lune — Bases Sélénites Sous-Terraines & Cratères Shackleton [Planétaire (Lune), +2040 AD à Futur]";
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes"))
            return "⚪ Mercure — Dômes Polaires & Habitats d'Ombre Permanente [Planétaire (Mercure), +2150 AD à Futur]";
        if (startYear < -10000)
            return "🌍 Terre — Paléo-Démographie & Expansion Sapiens [Afrique & Eurasie, -100 000 BC à -10 000 BC]";
        if (startYear >= -3000 && startYear < 1900)
            return "🌍 Terre — CShapes & Centennia Reconstitutions Démographiques [Empires & États, -3000 BC à +2000 AD]";
        return "🌍 Terre — HYDE 3.4 / Grille Historique Anthropocène [Global, -10 000 BC à +2023 AD]";
    }

    public void loadCustomDensityMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.title.load_density_dialog", "Load Population Density Map (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(context.getWindow());
        if (file != null) {
            try {
                Image img = new Image(new FileInputStream(file));
                ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(img);
                if (!val.valid()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(I18n.getOrDefault("scenario.dialog.invalid_map_title", "Carte Incompatible ou Invalide"));
                    alert.setHeaderText(I18n.getOrDefault("scenario.dialog.invalid_map_header", "Fichier de carte non supporté ou illisible"));
                    alert.setContentText(file.getName() + " :\n" + val.message() + "\n\n" + I18n.getOrDefault("scenario.dialog.invalid_map_hint", "Veuillez fournir une image raster valide (PNG/JPG) en projection équirectangulaire (2:1)."));
                    alert.showAndWait();
                    return;
                }
                customDensityImage = img;
                densityMapFileLabel.setText(String.format("📷 %s (%dx%d)", file.getName(), val.width(), val.height()));
                if (radioImportDemo != null) radioImportDemo.setSelected(true);
                updateDemoCompatibilityDisplay();
                if (context.getPreviewCells() != null) {
                    context.distributeInitialPopulation(context.getPreviewCells());
                    context.drawPreview();
                }
            } catch (Exception ex) {
                logger.error("Failed to load custom density map", ex);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de chargement: " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    public void updateDemoCompatibilityDisplay() {
        if (demoCompatibilityLabel == null) return;
        PlanetPreset p = context.getActivePlanetPreset();
        String planetName = p != null ? p.name() : "Standard";
        if (customDensityImage != null) {
            ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(customDensityImage);
            if (!val.valid()) {
                demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_invalid", "⚠️ Carte démographique incompatible : %s"), val.message()));
                demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else {
                demoCompatibilityLabel.setText(String.format(I18n.getOrDefault("scenario.demo.map_loaded", "✅ Map loaded and compatible with selected Tab 1 world (%s)"), planetName));
                demoCompatibilityLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            }
        } else {
            demoCompatibilityLabel.setText(I18n.getOrDefault("scenario.demo.no_map", "🪐 No external map loaded — Procedural mode active"));
            demoCompatibilityLabel.getStyleClass().add("subcard-status-muted");
            demoCompatibilityLabel.setStyle("-fx-font-size: 11px;");
        }
    }

    public void exportDensityMap() {
        int w = 2048;
        int h = 1024;
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();

        if (customDensityImage != null && customDensityImage.getWidth() > 0) {
            PixelReader pr = customDensityImage.getPixelReader();
            double srcW = customDensityImage.getWidth();
            double srcH = customDensityImage.getHeight();
            for (int y = 0; y < h; y++) {
                int sy = (int) Math.clamp((y / (double) h) * srcH, 0, srcH - 1);
                for (int x = 0; x < w; x++) {
                    int sx = (int) Math.clamp((x / (double) w) * srcW, 0, srcW - 1);
                    pw.setColor(x, y, pr.getColor(sx, sy));
                }
            }
        } else if (context.getPreviewCells() != null && !context.getPreviewCells().isEmpty()) {
            double minLat = -90, maxLat = 90;
            double minLng = -180, maxLng = 180;
            double scaleX = w / (maxLng - minLng);
            double scaleY = h / (maxLat - minLat);

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pw.setColor(x, y, Color.rgb(15, 23, 42));
                }
            }

            for (H3Cell c : context.getPreviewCells()) {
                int px = (int) ((c.getLongitude() - minLng) * scaleX);
                int py = (int) ((maxLat - c.getLatitude()) * scaleY);
                if (px >= 0 && px < w && py >= 0 && py < h) {
                    pw.setColor(px, py, ScenarioCellPopulationDistributor.getReliefAndDensityColor(c));
                }
            }
        } else {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pw.setColor(x, y, Color.rgb(15, 23, 42));
                }
            }
        }
        WindowUtils.exportImageWithChooser(context.getWindow(), image, "ether-population-density.png",
                I18n.getOrDefault("scenario.title.export_density_dialog", "Export Population Density Map (PNG / JPEG)"));
    }

    public void showDensityImportFormatHelp() {
        WindowUtils.showScrollableInfoDialog(
                I18n.getOrDefault("scenario.dialog.density_help_title", "Spécifications de la Carte de Densité"),
                I18n.getOrDefault("scenario.dialog.density_help_header", "Formats d'Image Supportés pour l'Import de Densité Démographique"),
                "Vous pouvez importer une carte de répartition démographique sous forme d'image PNG/JPEG au ratio 2:1 (ex: 2048x1024 pixels en projection équirectangulaire) :\n\n" +
                "1. CARTE DE DENSITÉ NIVEAUX DE GRIS :\n" +
                "   • Noir (0) = 0 hab/km² (Zone inhabitée / désertique / aquatique)\n" +
                "   • Blanc (255) = Densité maximale d'habitation (Foyer urbain ou métropole)\n\n" +
                "2. INTÉGRATION AVEC LE TERRAIN ET L'ÉCOLOGIE :\n" +
                "   • Les zones d'eau (océans/mers) définies dans l'onglet 1 sont automatiquement filtrées pour éviter l'apparition de populations en pleine mer.\n" +
                "   • La population totale paramétrée dans le spinner est distribuée au prorata de la luminosité de chaque pixel."
        );
    }

    public void generateProceduralPopulationDensity() {
        customDensityImage = null;
        if (radioProcDemo != null) radioProcDemo.setSelected(true);
        context.selectPreviewMode(0);
        context.updatePreviewTitleText();
        context.updateBottomLegend();

        Scenario s = context.getScenario();
        if (s != null) {
            CompletableFuture.runAsync(() -> {
                org.ether.society.data.HistoricalMapGenerator.generateProceduralMapsForScenario(s);
            }).thenRun(() -> javafx.application.Platform.runLater(() -> {
                String b64 = s.getCustomDensityBase64();
                if (b64 != null && !b64.isBlank()) {
                    customDensityImage = ImageMapLoader.base64PngToImage(b64);
                }
                if (context.getPreviewCells() != null && !context.getPreviewCells().isEmpty()) {
                    context.distributeInitialPopulation(context.getPreviewCells());
                }
                context.notifyParamChange();
                context.drawPreview();
            }));
        } else if (context.getPreviewCells() == null || context.getPreviewCells().isEmpty()) {
            context.generatePreview();
        } else {
            CompletableFuture.runAsync(() -> {
                context.distributeInitialPopulation(context.getPreviewCells());
            }).thenRun(() -> javafx.application.Platform.runLater(context::drawPreview));
        }
    }

    public void applyScenario(Scenario s) {
        if (s == null) return;
        if (initialHumanCountSpinner != null && initialHumanCountSpinner.getValueFactory() != null) {
            initialHumanCountSpinner.getValueFactory().setValue(s.getInitialHumanCount());
        }
        if (s.getPopulationDensityType() != null && densityPatternCombo != null && densityPatternCombo.getItems().contains(s.getPopulationDensityType())) {
            densityPatternCombo.setValue(s.getPopulationDensityType());
        }
        if (techPresetCombo != null && s.getTechPreset() != null) {
            techPresetCombo.setValue(s.getTechPreset());
        }
        if (customCapitalSpinner != null && customCapitalSpinner.getValueFactory() != null) {
            customCapitalSpinner.getValueFactory().setValue(s.getInitialCapitalPerCapita());
        }
        if (customEnergySpinner != null && customEnergySpinner.getValueFactory() != null) {
            customEnergySpinner.getValueFactory().setValue(s.getInitialEnergyPerCapita());
        }
        if (customFoodSpinner != null && customFoodSpinner.getValueFactory() != null) {
            customFoodSpinner.getValueFactory().setValue(s.getInitialFoodReserveMonths());
        }
        if (customInfoSpinner != null && customInfoSpinner.getValueFactory() != null) {
            customInfoSpinner.getValueFactory().setValue(s.getInitialInformationPerCapita());
        }

        if (s.getCustomDensityBase64() != null && !s.getCustomDensityBase64().isBlank()) {
            try {
                customDensityImage = ImageMapLoader.base64PngToImage(s.getCustomDensityBase64());
                if (densityMapFileLabel != null) densityMapFileLabel.setText("Embedded Base64");
                if (radioImportDemo != null) radioImportDemo.setSelected(true);
            } catch (Exception ex) {
                logger.warn("Could not decode scenario embedded density map", ex);
            }
        } else {
            customDensityImage = null;
            if (densityMapFileLabel != null) densityMapFileLabel.setText("—");
            if (radioProcDemo != null) radioProcDemo.setSelected(true);
        }
        updateDemoCompatibilityDisplay();
    }

    public void exportToScenario(Scenario s) {
        collectParameters(s);
    }

    /**
     * Updates all localized UI texts and tooltips across the demographic section.
     */
    public void updateTexts() {
        if (popHeader != null) {
            popHeader.setText(I18n.getOrDefault("scenario.pop_section", "👥 2. INITIAL POPULATION IDENTITY CARD"));
        }
        if (radioProcDemo != null) {
            radioProcDemo.setText(I18n.getOrDefault("scenario.radio.procedural", "▶ Procedural Mode (Algorithms & Demographic Patterns)"));
        }
        if (radioImportDemo != null) {
            radioImportDemo.setText(I18n.getOrDefault("scenario.radio.import", "📂 Loading Mode for Existing Density Map (PNG)"));
        }
        if (demoSourceLabel != null) {
            demoSourceLabel.setText(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
        }
        if (loadDensityMapBtn != null) {
            loadDensityMapBtn.setText(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
        }
        if (exportDensityMapBtn != null) {
            exportDensityMapBtn.setText(I18n.getOrDefault("scenario.btn.export_density", "💾 Export"));
        }
        updateDemoCompatibilityDisplay();
    }

    public void collectParameters(Scenario s) {
        if (s == null) return;
        if (initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null) {
            s.setInitialHumanCount(initialHumanCountSpinner.getValue());
        }
        if (densityPatternCombo != null && densityPatternCombo.getValue() != null) {
            s.setPopulationDensityType(densityPatternCombo.getValue());
        }
        if (techPresetCombo != null && techPresetCombo.getValue() != null) {
            s.setTechPreset(techPresetCombo.getValue());
        }
        if (customCapitalSpinner != null && customCapitalSpinner.getValue() != null) {
            s.setInitialCapitalPerCapita(customCapitalSpinner.getValue());
        }
        if (customEnergySpinner != null && customEnergySpinner.getValue() != null) {
            s.setInitialEnergyPerCapita(customEnergySpinner.getValue());
        }
        if (customFoodSpinner != null && customFoodSpinner.getValue() != null) {
            s.setInitialFoodReserveMonths(customFoodSpinner.getValue());
        }
        if (customInfoSpinner != null && customInfoSpinner.getValue() != null) {
            s.setInitialInformationPerCapita(customInfoSpinner.getValue());
        }

        if (customDensityImage != null && radioImportDemo != null && radioImportDemo.isSelected()) {
            s.setCustomDensityBase64(ImageMapLoader.imageToBase64Png(customDensityImage));
        } else {
            s.setCustomDensityBase64(null);
        }
    }

    // --- Getters & Accessors ---
    public Spinner<Long> getInitialHumanCountSpinner() { return initialHumanCountSpinner; }
    public ComboBox<String> getDensityPatternCombo() { return densityPatternCombo; }
    public ComboBox<Scenario.TechPreset> getTechPresetCombo() { return techPresetCombo; }
    public Spinner<Double> getCustomCapitalSpinner() { return customCapitalSpinner; }
    public Spinner<Double> getCustomEnergySpinner() { return customEnergySpinner; }
    public Spinner<Double> getCustomFoodSpinner() { return customFoodSpinner; }
    public Spinner<Double> getCustomInfoSpinner() { return customInfoSpinner; }
    public RadioButton getRadioProcDemo() { return radioProcDemo; }
    public RadioButton getRadioImportDemo() { return radioImportDemo; }
    public Image getCustomDensityImage() { return customDensityImage; }
    public void setCustomDensityImage(Image img) { this.customDensityImage = img; }
    public ComboBox<String> getDemoSourceCombo() { return demoSourceCombo; }
    public Label getDemoCompatibilityLabel() { return demoCompatibilityLabel; }
    public Label getDensityMapFileLabel() { return densityMapFileLabel; }
    public Label getPopHeader() { return popHeader; }
    public Button getExportDensityMapBtn() { return exportDensityMapBtn; }
    public Button getLoadDensityMapBtn() { return loadDensityMapBtn; }
}
