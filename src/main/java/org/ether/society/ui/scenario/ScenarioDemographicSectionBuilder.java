/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Window;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.ScenarioSetupPanel.LongSpinnerValueFactory;

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

    public static final Map<String, String> DENSITY_LABELS = ScenarioDemographicMapIOHelper.DENSITY_LABELS;
    public static final Map<String, String> DENSITY_DESCRIPTIONS = ScenarioDemographicMapIOHelper.DENSITY_DESCRIPTIONS;

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

        VBox proceduralDemoPanel = buildProceduralDemoPanel();
        VBox importDemoPanel = buildImportDemoPanel();

        demoSourceGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            context.notifyParamChange();
            boolean isProc = demoSourceGroup.getSelectedToggle() == radioProcDemo;
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

        popSection.getChildren().addAll(popHeader, radioProcDemo, proceduralDemoPanel, radioImportDemo, importDemoPanel);
        return popSection;
    }

    private VBox buildProceduralDemoPanel() {
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
        ScenarioDemographicMapIOHelper.configureDensityPatternCombo(densityPatternCombo, context);
        densityPatternCombo.valueProperty().addListener((obs, oldV, newV) -> {
            context.notifyParamChange();
            if (context.getPreviewCells() != null) {
                context.distributeInitialPopulation(context.getPreviewCells());
                context.drawPreview();
            }
        });

        customPhysicalSubPanel = buildCustomPhysicalSubPanel();

        techPresetCombo = new ComboBox<>();
        ScenarioDemographicMapIOHelper.configureTechPresetCombo(techPresetCombo, customPhysicalSubPanel, context);

        GridPane popGrid = new GridPane();
        popGrid.setHgap(8);
        popGrid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(40);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(60);
        popGrid.getColumnConstraints().addAll(c1, c2);

        popGrid.addRow(0, new Label(I18n.getOrDefault("scenario.label.initial_pop", "Initial Population:")), initialHumanCountSpinner);
        popGrid.addRow(1, new Label(I18n.getOrDefault("scenario.label.density_pattern", "Settlement Pattern:")), densityPatternCombo);
        popGrid.addRow(2, new Label(I18n.getOrDefault("scenario.label.tech_preset", "Physical Capital & Energy Regime:")), techPresetCombo);

        exportDensityMapBtn = new Button(I18n.getOrDefault("scenario.btn.export_map_png", "📤 Export Raster (PNG / JPEG)"));
        exportDensityMapBtn.getStyleClass().add("button-secondary");
        exportDensityMapBtn.setMaxWidth(Double.MAX_VALUE);
        exportDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_map_png",
                "Export demographic density map as a high-resolution PNG or JPEG raster file.")));
        exportDensityMapBtn.setOnAction(e -> exportDensityMap());

        HBox demoBtnBar = new HBox(8, exportDensityMapBtn);
        HBox.setHgrow(exportDensityMapBtn, Priority.ALWAYS);

        VBox proceduralDemoPanel = new VBox(8, popGrid, customPhysicalSubPanel, demoBtnBar);
        proceduralDemoPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        return proceduralDemoPanel;
    }

    private VBox buildCustomPhysicalSubPanel() {
        customCapitalSpinner = createStockSpinner(0.0, 10_000.0, 120.0, 10.0);
        customEnergySpinner = createStockSpinner(0.0, 10_000_000.0, 300.0, 50.0);
        customFoodSpinner = createStockSpinner(0.0, 120.0, 6.0, 1.0);
        customInfoSpinner = createStockSpinner(0.0, 100_000_000_000.0, 2000.0, 100.0);

        GridPane customGrid = new GridPane();
        customGrid.setHgap(8);
        customGrid.setVgap(8);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25);
            customGrid.getColumnConstraints().add(col);
        }

        customGrid.addRow(0,
                new Label(I18n.getOrDefault("scenario.label.stock_k0", "🛠️ K₀ (kg/hab) :")), customCapitalSpinner,
                new Label(I18n.getOrDefault("scenario.label.stock_e0", "⚡ E₀ (MJ/hab) :")), customEnergySpinner);
        customGrid.addRow(1,
                new Label(I18n.getOrDefault("scenario.label.stock_f0", "🌾 F₀ (mois) :")), customFoodSpinner,
                new Label(I18n.getOrDefault("scenario.label.stock_i0", "🧠 I₀ (bits/hab) :")), customInfoSpinner);

        Label customTitleLabel = new Label(I18n.getOrDefault("scenario.section.custom_stocks", "⚙️ MANUAL INITIAL PHYSICAL STOCKS SETUP (CUSTOM):"));
        customTitleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");

        VBox subPanel = new VBox(6, customTitleLabel, customGrid);
        subPanel.setStyle("-fx-padding: 8px; -fx-background-color: rgba(245, 158, 11, 0.05); -fx-background-radius: 6px; -fx-border-color: rgba(245, 158, 11, 0.25); -fx-border-radius: 6px; -fx-border-width: 1px;");
        subPanel.setVisible(false);
        subPanel.setManaged(false);
        return subPanel;
    }

    private Spinner<Double> createStockSpinner(double min, double max, double initial, double step) {
        Spinner<Double> sp = new Spinner<>(min, max, initial, step);
        sp.setEditable(true);
        sp.setMaxWidth(Double.MAX_VALUE);
        sp.valueProperty().addListener((obs, oldV, newV) -> context.notifyParamChange());
        return sp;
    }

    private VBox buildImportDemoPanel() {
        demoSourceLabel = new Label("📍 " + I18n.getOrDefault("resource.label.reference_source", "Source de Référence :"));
        demoSourceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
        demoSourceCombo = ScenarioDemographicMapIOHelper.buildPopulationSourceCombo(this, context);

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
        loadDensityMapBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.import_density_map",
                "Import an external demographic density map in 2:1 equirectangular PNG format.")));
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
            if (radioProcDemo != null) radioProcDemo.setSelected(true);
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
        return importDemoPanel;
    }

    public String pickDemoSourceForEpoch(long startYear, PlanetPreset planet) {
        return ScenarioDemographicMapIOHelper.pickDemoSourceForEpoch(startYear, planet);
    }

    public void loadCustomDensityMap() {
        ScenarioDemographicMapIOHelper.loadCustomDensityMap(this, context);
    }

    public void updateDemoCompatibilityDisplay() {
        ScenarioDemographicMapIOHelper.updateDemoCompatibilityDisplay(this, context);
    }

    public void exportDensityMap() {
        ScenarioDemographicMapIOHelper.exportDensityMap(customDensityImage, context);
    }

    public void showDensityImportFormatHelp() {
        ScenarioDemographicMapIOHelper.showDensityImportFormatHelp();
    }

    public void generateProceduralPopulationDensity() {
        customDensityImage = null;
        if (radioProcDemo != null) radioProcDemo.setSelected(true);
        context.selectPreviewMode(0);
        context.updatePreviewTitleText();
        context.updateBottomLegend();

        Scenario s = context.getScenario();
        if (s != null) {
            CompletableFuture.runAsync(() -> HistoricalMapGenerator.generateProceduralMapsForScenario(s))
                    .thenRun(() -> javafx.application.Platform.runLater(() -> {
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
            CompletableFuture.runAsync(() -> context.distributeInitialPopulation(context.getPreviewCells()))
                    .thenRun(() -> javafx.application.Platform.runLater(context::drawPreview));
        }
    }

    public void applyScenario(Scenario s) {
        ScenarioDemographicMapIOHelper.applyScenario(s, this, context);
    }

    public void exportToScenario(Scenario s) {
        collectParameters(s);
    }

    /**
     * Updates all localized UI texts and tooltips across the demographic section.
     */
    public void updateTexts() {
        if (popHeader != null) popHeader.setText(I18n.getOrDefault("scenario.pop_section", "👥 2. INITIAL POPULATION IDENTITY CARD"));
        if (radioProcDemo != null) radioProcDemo.setText(I18n.getOrDefault("scenario.radio.procedural", "▶ Procedural Mode (Algorithms & Demographic Patterns)"));
        if (radioImportDemo != null) radioImportDemo.setText(I18n.getOrDefault("scenario.radio.import", "📂 Loading Mode for Existing Density Map (PNG)"));
        if (demoSourceLabel != null) demoSourceLabel.setText(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
        if (loadDensityMapBtn != null) loadDensityMapBtn.setText(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
        if (exportDensityMapBtn != null) exportDensityMapBtn.setText(I18n.getOrDefault("scenario.btn.export_density", "💾 Export"));
        updateDemoCompatibilityDisplay();
    }

    public void collectParameters(Scenario s) {
        ScenarioDemographicMapIOHelper.collectParameters(s, this);
    }

    // --- Getters & Accessors ---
    public Spinner<Long> getInitialHumanCountSpinner() { return initialHumanCountSpinner; }
    public ComboBox<String> getDensityPatternCombo() { return densityPatternCombo; }
    public boolean isDensityImportActive() { return radioImportDemo != null && radioImportDemo.isSelected(); }
    public long getInitialHumanCount() { return initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null ? initialHumanCountSpinner.getValue() : 1_000_000L; }
    public String getDensityPattern() { return densityPatternCombo != null && densityPatternCombo.getValue() != null ? densityPatternCombo.getValue() : "UNBIASED_NATURAL"; }
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
