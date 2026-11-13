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
import javafx.scene.paint.Color;
import javafx.stage.Window;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.CulturalAffinityMatrixDialog;
import org.ether.society.ui.WindowHelper;
import org.ether.society.ui.util.DataSourceUIHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Controller and UI builder for the N-dimensional Cultural Vector and Multi-Field Layers
 * section of the Scenario Setup Panel.
 * <p>
 * Manages procedural stochastic generation via coupled Langevin-SDE formulas:
 * \[
 *   d\mathbf{C}_i = \alpha \nabla^2 \mathbf{C}_i \, dt + \sigma \, d\mathbf{W}_t
 * \]
 * as well as external raster (PNG, JPEG) and GIS (GeoJSON) cartographic layer imports.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ScenarioCulturalSectionBuilder {

    /**
     * Callback context interface providing access to host panel state.
     */
    public interface HostContext {
        void notifyParamChange();
        void drawPreview();
        void updatePreviewModesCombo();
        Scenario getScenario();
        long getStartYear();
        PlanetPreset getActivePlanetPreset();
        Window getWindow();
        boolean isUpdatingFromPreset();
        void selectPreviewMode(int modeIndex);
        String getScenarioDescription();
    }

    private final HostContext context;
    private final ScenarioCulturalSubBlockBuilder subBlockBuilder;

    // Controls
    private Label cultureHeader;
    private Spinner<Integer> cultureVectorDimSpinner;
    private Spinner<Double> culturalDiffusionRateSpinner;
    private Spinner<Double> culturalMutationRateSpinner;
    private TextField cultSeedField;
    private Button btnGenerateProceduralTensorsSection;
    private Button culturalHelpBtn;
    private VBox layersDynamicContainer;

    // Custom maps & names
    private final Map<Integer, Image> customTensorImages = new HashMap<>();
    private final Map<Integer, String> customTensorNames = new HashMap<>();

    // Sub-block UI maps per tensor index
    private final Map<Integer, Label> tensorSubTitles = new HashMap<>();
    private final Map<Integer, RadioButton> tensorProcRadios = new HashMap<>();
    private final Map<Integer, RadioButton> tensorImportRadios = new HashMap<>();
    private final Map<Integer, ComboBox<String>> tensorSourceCombos = new HashMap<>();
    private final Map<Integer, Label> tensorSourceLabels = new HashMap<>();
    private final Map<Integer, Label> tensorFileLabels = new HashMap<>();
    private final Map<Integer, Button> tensorLoadBtns = new HashMap<>();
    private final Map<Integer, Label> tensorFormatLabels = new HashMap<>();
    private final Map<Integer, TextField> tensorSeedFields = new HashMap<>();
    private final Map<Integer, Slider> tensorParam1Sliders = new HashMap<>();
    private final Map<Integer, Slider> tensorParam2Sliders = new HashMap<>();
    private final Map<Integer, Slider> tensorParam3Sliders = new HashMap<>();
    private final Map<Integer, Label> tensorParam1ValueLabels = new HashMap<>();
    private final Map<Integer, Label> tensorParam2ValueLabels = new HashMap<>();
    private final Map<Integer, Label> tensorParam3ValueLabels = new HashMap<>();
    private final Map<Integer, Button> tensorGenSingleBtns = new HashMap<>();

    public ScenarioCulturalSectionBuilder(HostContext context) {
        this.context = context;
        this.subBlockBuilder = new ScenarioCulturalSubBlockBuilder(
                this, context, customTensorImages, customTensorNames,
                tensorSubTitles, tensorProcRadios, tensorImportRadios, tensorSourceCombos,
                tensorSourceLabels, tensorFileLabels, tensorLoadBtns, tensorFormatLabels,
                tensorSeedFields, tensorParam1Sliders, tensorParam2Sliders, tensorParam3Sliders,
                tensorParam1ValueLabels, tensorParam2ValueLabels, tensorParam3ValueLabels,
                tensorGenSingleBtns
        );
    }

    /**
     * Builds the complete JavaFX VBox section for cultural vector and multi-field layers.
     *
     * @return structured VBox section
     */
    public VBox buildSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        cultureHeader = new Label(I18n.getOrDefault("scenario.culture_section", " 3. CULTURAL VECTOR DIMENSION & MULTI-FIELD LAYERS"));
        cultureHeader.getStyleClass().add("label-section-header");

        Label desc = new Label(I18n.getOrDefault("scenario.header.cultural_tensor",
                "3.1 Cultural Tensor Kernel & Langevin-SDE (Diffusion & Mutation):\n"
                        + "Configuration of N-dimensional information tensor and import/generation of cartographic layers across all cultural dimensions."));
        desc.setStyle("-fx-font-size: 11px; -fx-font-weight: normal;");
        desc.setWrapText(true);

        GridPane grid = buildLangevinKernelGrid();

        // Layer Import Panel
        VBox layersPanel = new VBox(8);
        layersPanel.getStyleClass().add("layers-panel-card");

        Label layerTitle = new Label(I18n.getOrDefault("scenario.header.tensor_subblocks",
                " 3.2 Cartographic Sub-Blocks per Tensor (Procedural Generation / Imported Maps)"));
        layerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        layerTitle.setWrapText(true);

        VBox layerHeaderBox = new VBox(6, layerTitle, buildSeedAndActionBox());

        Label culturalFormatHintLabel = new Label(I18n.getOrDefault("scenario.desc.tensor_formats",
                "PNG / JPEG (2:1 equirectangular projection):\n  Each sub-block below allows individual choice of parameterized procedural generation or map import. In map import mode, a valid map file must be loaded."));
        culturalFormatHintLabel.setWrapText(true);
        culturalFormatHintLabel.getStyleClass().add("hint-label");

        layersDynamicContainer = new VBox(10);
        rebuildCulturalTensorSubBlocks(cultureVectorDimSpinner.getValue());

        layersPanel.getChildren().addAll(layerHeaderBox, culturalFormatHintLabel, layersDynamicContainer);
        section.getChildren().addAll(cultureHeader, desc, grid, layersPanel);
        return section;
    }

    private GridPane buildLangevinKernelGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints cg1 = new ColumnConstraints();
        cg1.setPercentWidth(55);
        ColumnConstraints cg2 = new ColumnConstraints();
        cg2.setPercentWidth(45);
        grid.getColumnConstraints().setAll(cg1, cg2);

        Label dimLbl = new Label(I18n.getOrDefault("scenario.label.cultural_dim", "Taille Tenseur Culturel (M dims) :"));
        dimLbl.getStyleClass().add("control-label");
        cultureVectorDimSpinner = new Spinner<>(0, 32, 9, 1);
        cultureVectorDimSpinner.setEditable(true);
        cultureVectorDimSpinner.setMaxWidth(Double.MAX_VALUE);
        cultureVectorDimSpinner.valueProperty().addListener((obs, o, n) -> {
            context.notifyParamChange();
            if (n != null) {
                rebuildCulturalTensorSubBlocks(n);
                context.updatePreviewModesCombo();
                context.drawPreview();
            }
        });
        Tooltip.install(cultureVectorDimSpinner, new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_dim",
                "Nombre de composantes N-dimensionnelles du vecteur d'information culturelle (ex: 9D, 16D, 32D).")));

        Label diffLbl = new Label(I18n.getOrDefault("scenario.label.cultural_diffusion", "Cultural Diffusion Conductivity (Î±):"));
        diffLbl.getStyleClass().add("control-label");
        culturalDiffusionRateSpinner = new Spinner<>(0.001, 0.50, 0.05, 0.01);
        culturalDiffusionRateSpinner.setEditable(true);
        culturalDiffusionRateSpinner.setMaxWidth(Double.MAX_VALUE);
        culturalDiffusionRateSpinner.valueProperty().addListener((obs, o, n) -> context.notifyParamChange());
        Tooltip.install(culturalDiffusionRateSpinner, new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_diffusion",
                "Diffusion rate of association energy between neighboring H3 hexagons.")));

        Label mutLbl = new Label(I18n.getOrDefault("scenario.label.cultural_mutation", "Mutation / Drift Noise (1/âˆšsN):"));
        mutLbl.getStyleClass().add("control-label");
        culturalMutationRateSpinner = new Spinner<>(0.001, 0.10, 0.01, 0.005);
        culturalMutationRateSpinner.setEditable(true);
        culturalMutationRateSpinner.setMaxWidth(Double.MAX_VALUE);
        culturalMutationRateSpinner.valueProperty().addListener((obs, o, n) -> context.notifyParamChange());
        Tooltip.install(culturalMutationRateSpinner, new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_mutation",
                "Langevin drift factor (stochastic innovation background noise).")));

        grid.add(dimLbl, 0, 0); grid.add(cultureVectorDimSpinner, 1, 0);
        grid.add(diffLbl, 0, 1); grid.add(culturalDiffusionRateSpinner, 1, 1);
        grid.add(mutLbl, 0, 2); grid.add(culturalMutationRateSpinner, 1, 2);
        return grid;
    }

    private VBox buildSeedAndActionBox() {
        btnGenerateProceduralTensorsSection = new Button(I18n.getOrDefault("scenario.btn.regen_tensors", "âš¡ Régénérer les Tenseurs"));
        btnGenerateProceduralTensorsSection.getStyleClass().add("button");
        btnGenerateProceduralTensorsSection.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnGenerateProceduralTensorsSection.setMinWidth(Region.USE_PREF_SIZE);
        btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors",
                "Bascule tous les tenseurs en mode procédural et régénère les cartes selon les paramètres et la graine stochastique.")));
        btnGenerateProceduralTensorsSection.setOnAction(e -> generateProceduralCulturalTensors());

        cultSeedField = new TextField("54321");
        cultSeedField.setPrefWidth(80);
        cultSeedField.setStyle("-fx-font-size: 11px;");
        cultSeedField.textProperty().addListener((obs, oldV, newV) -> context.notifyParamChange());

        Button cultRandSeedBtn = new Button("");
        cultRandSeedBtn.getStyleClass().add("button-secondary");
        cultRandSeedBtn.setStyle("-fx-font-size: 11px;");
        cultRandSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_tensor_seed",
                "Tirer une nouvelle graine stochastique aléatoire pour les tenseurs culturels (sans affecter la densité de population).")));
        cultRandSeedBtn.setOnAction(e -> {
            context.notifyParamChange();
            cultSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            generateProceduralCulturalTensors();
        });

        culturalHelpBtn = new Button(I18n.getOrDefault("scenario.btn.cultural_format_help", "â„¹ Format Calques"));
        culturalHelpBtn.getStyleClass().add("button-secondary");
        culturalHelpBtn.setStyle("-fx-font-size: 11px;");
        culturalHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        culturalHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_specs",
                "Specifications of image formats and cartographic data for cultural and geopolitical layer import.")));
        culturalHelpBtn.setOnAction(e -> ScenarioGisExporter.showCulturalImportFormatHelp(context.getWindow()));

        HBox cultSeedBox = new HBox(4, new Label(I18n.getOrDefault("scenario.label.cultural_seed", "Graine :")), new Label(""), cultSeedField, cultRandSeedBtn);
        cultSeedBox.setAlignment(Pos.CENTER_LEFT);

        HBox seedRow = new HBox(8, cultSeedBox, btnGenerateProceduralTensorsSection);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnGenerateProceduralTensorsSection, Priority.ALWAYS);

        Button btnExportGisMultiFormat = new Button(I18n.getOrDefault("scenario.btn.export_gis", " Export SIG Multi-Format"));
        btnExportGisMultiFormat.getStyleClass().add("button-secondary");
        btnExportGisMultiFormat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportGisMultiFormat.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_gis",
                "Export scenario density layers and 9 cultural tensors under professional GIS formats (GeoTIFF, NetCDF-4, GeoJSON).")));
        btnExportGisMultiFormat.setOnAction(e -> ScenarioGisExporter.exportGisMultiFormat(
                context.getWindow(), context.getScenarioDescription(), context.getStartYear(), getCultureVectorDimensions()));

        Button btnExportProvenanceManifest = new Button(I18n.getOrDefault("scenario.btn.provenance_manifest", " Manifeste Provenance SHA-256"));
        btnExportProvenanceManifest.getStyleClass().add("button-secondary");
        btnExportProvenanceManifest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.gen_provenance",
                "Generate a cryptographic JSON manifest (provenance.json) containing SHA-256 hashes of datasets and parameters.")));
        btnExportProvenanceManifest.setOnAction(e -> ScenarioGisExporter.exportProvenanceManifest(
                context.getWindow(), context.getStartYear(), cultSeedField != null ? cultSeedField.getText() : "54321"));

        Button btnCulturalMatrix = new Button(I18n.getOrDefault("scenario.btn.cultural_matrix_inspect", " Inspecteur des Affinités Culturelles"));
        btnCulturalMatrix.getStyleClass().add("button-secondary");
        btnCulturalMatrix.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnCulturalMatrix.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_matrix",
                "Inspect the N Ã— N Pairwise Cultural Affinity and Distance Heatmap Matrix.")));
        btnCulturalMatrix.setOnAction(e -> new CulturalAffinityMatrixDialog(context.getStartYear(), context.getScenario()).show());

        HBox actionButtonsRow = new HBox(8, culturalHelpBtn, btnExportGisMultiFormat, btnExportProvenanceManifest);
        actionButtonsRow.setAlignment(Pos.CENTER_LEFT);

        VBox matrixCard = new VBox(4);
        matrixCard.setStyle("-fx-padding: 8px 10px; -fx-background-color: rgba(30, 41, 59, 0.65); -fx-background-radius: 6px; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6px; -fx-border-width: 1px;");
        Label matrixTitle = new Label(I18n.getOrDefault("scenario.cultural_matrix.title", "Matrice d'Affinité & Distances Culturelles"));
        matrixTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #f8fafc;");
        Label matrixDesc = new Label(I18n.getOrDefault("scenario.cultural_matrix.desc",
                "Inspecter et éditer la matrice NÃ—N des distances de Mahalanobis et coefficients d'assimilation entre entités culturelles."));
        matrixDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
        matrixDesc.setWrapText(true);
        matrixCard.getChildren().addAll(matrixTitle, matrixDesc, btnCulturalMatrix);

        return new VBox(8, seedRow, actionButtonsRow, matrixCard);
    }

    public String getDefaultTensorName(int index) {
        return ScenarioCulturalTensorRegistry.getDefaultTensorName(index);
    }

    public String getCulturalTensorTitle(int index) {
        return ScenarioCulturalTensorRegistry.getCulturalTensorTitle(index, customTensorNames);
    }

    public String getCulturalTensorTooltip(int index) {
        return ScenarioCulturalTensorRegistry.getCulturalTensorTooltip(index);
    }

    public String getCulturalTensorPreviewName(int index) {
        return ScenarioCulturalTensorRegistry.getCulturalTensorPreviewName(index, customTensorNames);
    }

    public String getCulturalBaselineName(int index) {
        return ScenarioCulturalTensorRegistry.getCulturalBaselineName(index);
    }

    public String getCulturalFormatHint(int index) {
        return ScenarioCulturalTensorRegistry.getCulturalFormatHint(index);
    }

    public String pickCulturalSourceForEpoch(int tensorIdx, long startYear, PlanetPreset planet) {
        return ScenarioCulturalTensorRegistry.pickCulturalSourceForEpoch(tensorIdx, startYear, planet);
    }

    public ComboBox<String> buildCulturalSourceCombo(int tensorIdx) {
        ComboBox<String> combo = ScenarioCulturalTensorRegistry.buildCulturalSourceCombo(tensorIdx);
        String autoSrc = pickCulturalSourceForEpoch(tensorIdx, context.getStartYear(), context.getActivePlanetPreset());
        DataSourceUIHelper.selectBestSource(combo, autoSrc, "Terre");
        combo.setOnAction(e -> {
            if (context.isUpdatingFromPreset()) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (customTensorImages.get(tensorIdx) != null && tensorImportRadios.containsKey(tensorIdx)) {
                    tensorImportRadios.get(tensorIdx).setSelected(true);
                }
                context.notifyParamChange();
                context.drawPreview();
            }
        });
        return combo;
    }

    public ScenarioCulturalTensorRegistry.TensorParamDescriptor getTensorParam1Descriptor(int tensorIdx) {
        return ScenarioCulturalTensorRegistry.getTensorParam1Descriptor(tensorIdx);
    }

    public ScenarioCulturalTensorRegistry.TensorParamDescriptor getTensorParam2Descriptor(int tensorIdx) {
        return ScenarioCulturalTensorRegistry.getTensorParam2Descriptor(tensorIdx);
    }

    public ScenarioCulturalTensorRegistry.TensorParamDescriptor getTensorParam3Descriptor(int tensorIdx) {
        return ScenarioCulturalTensorRegistry.getTensorParam3Descriptor(tensorIdx);
    }

    public String getDefaultTensorSeed(int tensorIdx) {
        return ScenarioCulturalTensorRegistry.getDefaultTensorSeed(tensorIdx);
    }

    public void updateTensorFileLabel(int tensorIdx) {
        ScenarioCulturalStateIOHelper.updateTensorFileLabel(
                tensorIdx, tensorFileLabels, tensorLoadBtns, tensorProcRadios, customTensorImages);
    }

    public void exportCulturalTensor(int tensorIdx) {
        Image img = customTensorImages.get(tensorIdx);
        if (img == null) {
            img = generateProceduralTensorRasterImage(tensorIdx, 2048, 1024);
        }
        String defaultName = "cultural_tensor_" + (tensorIdx + 1) + ".png";
        String title = I18n.getOrDefault("scenario.title.export_tensor_dialog", "Export Cultural Tensor " + (tensorIdx + 1));
        WindowHelper.exportImageWithChooser(context.getWindow(), img, defaultName, title);
    }

    public Image generateProceduralTensorRasterImage(int tensorIdx, int w, int h) {
        return ScenarioProceduralTensorRasterizer.generateProceduralTensorRasterImage(
                tensorIdx, w, h,
                getTensorParam1(tensorIdx), getTensorParam2(tensorIdx), getTensorParam3(tensorIdx),
                getTensorSeed(tensorIdx));
    }

    public Color evaluateProceduralTensorColor(int tIndex, double lat, double lon, double elevation, double p1, double p2, double p3, long seed) {
        return ScenarioProceduralTensorRasterizer.evaluateProceduralTensorColor(tIndex, lat, lon, elevation, p1, p2, p3, seed);
    }

    public void deleteCulturalTensor(int indexToDelete) {
        ScenarioCulturalStateIOHelper.deleteCulturalTensor(
                indexToDelete, this, context, tensorSubTitles, tensorSourceLabels, tensorFileLabels,
                tensorFormatLabels, tensorParam1ValueLabels, tensorParam2ValueLabels, tensorParam3ValueLabels,
                tensorGenSingleBtns);
    }

    public void updateTexts() {
        if (cultureHeader != null) {
            cultureHeader.setText(I18n.getOrDefault("scenario.culture_section", " CULTURAL VECTOR DIMENSION & MULTI-FIELD LAYERS"));
        }
        updateCulturalTensorSubBlockTexts();
    }

    public void updateCulturalTensorSubBlockTexts() {
        ScenarioCulturalStateIOHelper.updateCulturalTensorSubBlockTexts(
                this, layersDynamicContainer, btnGenerateProceduralTensorsSection,
                tensorSubTitles, tensorSourceLabels, tensorFormatLabels, tensorGenSingleBtns);
    }

    public void rebuildCulturalTensorSubBlocks(int dimCount) {
        subBlockBuilder.rebuildSubBlocks(layersDynamicContainer, dimCount, btnGenerateProceduralTensorsSection);
    }

    public void loadCustomCultureLayerForTensor(int tensorIdx, Consumer<Image> onLoaded) {
        loadCustomCultureLayer(getCulturalTensorTitle(tensorIdx), onLoaded);
    }

    public void loadCustomCultureLayer(String layerName, Consumer<Image> onLoaded) {
        ScenarioCulturalStateIOHelper.loadCustomCultureLayer(context.getWindow(), layerName, onLoaded);
    }

    public void generateProceduralCulturalTensors() {
        ScenarioCulturalStateIOHelper.generateProceduralCulturalTensors(
                this, context, btnGenerateProceduralTensorsSection, tensorFileLabels);
    }

    public void applyScenario(Scenario s) {
        ScenarioCulturalStateIOHelper.applyScenario(s, this);
    }

    public void exportToScenario(Scenario s) {
        ScenarioCulturalStateIOHelper.exportToScenario(s, this);
    }

    public int getCultureVectorDimensions() {
        return cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
    }

    public double getCulturalDiffusionRate() {
        return culturalDiffusionRateSpinner != null && culturalDiffusionRateSpinner.getValue() != null ? culturalDiffusionRateSpinner.getValue() : 0.05;
    }

    public double getCulturalMutationRate() {
        return culturalMutationRateSpinner != null && culturalMutationRateSpinner.getValue() != null ? culturalMutationRateSpinner.getValue() : 0.02;
    }

    public Image getCustomTensorImage(int index) { return customTensorImages.get(index); }
    public boolean isTensorImportActive(int index) { return tensorImportRadios.containsKey(index) && tensorImportRadios.get(index).isSelected(); }
    public double getTensorParam1(int index) { return tensorParam1Sliders.containsKey(index) ? tensorParam1Sliders.get(index).getValue() : 1.0; }
    public double getTensorParam2(int index) { return tensorParam2Sliders.containsKey(index) ? tensorParam2Sliders.get(index).getValue() : 1.0; }
    public double getTensorParam3(int index) { return tensorParam3Sliders.containsKey(index) ? tensorParam3Sliders.get(index).getValue() : 1.0; }

    public long getTensorSeed(int index) {
        if (tensorSeedFields.containsKey(index)) {
            try { return Long.parseLong(tensorSeedFields.get(index).getText().trim()); } catch (Exception ignored) { org.slf4j.LoggerFactory.getLogger(ScenarioCulturalSectionBuilder.class).debug("Non-fatal condition in ScenarioCulturalSectionBuilder: {}", ignored.getMessage()); }
        }
        return 11235L + index * 11111L;
    }

    public Spinner<Integer> getCultureVectorDimSpinner() { return cultureVectorDimSpinner; }
    public Spinner<Double> getCulturalDiffusionRateSpinner() { return culturalDiffusionRateSpinner; }
    public Spinner<Double> getCulturalMutationRateSpinner() { return culturalMutationRateSpinner; }
    public TextField getCultSeedField() { return cultSeedField; }
    public Map<Integer, Image> getCustomTensorImages() { return customTensorImages; }
    public Map<Integer, String> getCustomTensorNames() { return customTensorNames; }
    public Map<Integer, RadioButton> getTensorProcRadios() { return tensorProcRadios; }
    public Map<Integer, RadioButton> getTensorImportRadios() { return tensorImportRadios; }
    public Map<Integer, Slider> getTensorParam1Sliders() { return tensorParam1Sliders; }
    public Map<Integer, Slider> getTensorParam2Sliders() { return tensorParam2Sliders; }
    public Map<Integer, Slider> getTensorParam3Sliders() { return tensorParam3Sliders; }
    public Map<Integer, TextField> getTensorSeedFields() { return tensorSeedFields; }
    public Map<Integer, ComboBox<String>> getTensorSourceCombos() { return tensorSourceCombos; }
    public Map<Integer, Button> getTensorLoadBtns() { return tensorLoadBtns; }
}


