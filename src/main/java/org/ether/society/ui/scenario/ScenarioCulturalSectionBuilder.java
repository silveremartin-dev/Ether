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
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.CulturalAffinityMatrixDialog;
import org.ether.society.ui.WindowUtils;
import org.ether.society.ui.util.DataSourceUIHelper;
import org.ether.society.ui.util.UIControlsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
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
 * @version 1.0.0-beta.2
 */
public class ScenarioCulturalSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioCulturalSectionBuilder.class);

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
    }

    /**
     * Builds the complete JavaFX VBox section for cultural vector and multi-field layers.
     *
     * @return structured VBox section
     */
    public VBox buildSection() {
        VBox section = new VBox(10);
        section.getStyleClass().add("card-section");

        cultureHeader = new Label(I18n.getOrDefault("scenario.culture_section", "🏛 3. CULTURAL VECTOR DIMENSION & MULTI-FIELD LAYERS"));
        cultureHeader.getStyleClass().add("label-section-header");

        Label desc = new Label(I18n.getOrDefault("scenario.header.cultural_tensor",
                "3.1 Cultural Tensor Kernel & Langevin-SDE (Diffusion & Mutation):\n"
                        + "Configuration of N-dimensional information tensor and import/generation of cartographic layers across all cultural dimensions."));
        desc.setStyle("-fx-font-size: 11px; -fx-font-weight: normal;");
        desc.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints cg1 = new ColumnConstraints();
        cg1.setPercentWidth(55);
        ColumnConstraints cg2 = new ColumnConstraints();
        cg2.setPercentWidth(45);
        grid.getColumnConstraints().setAll(cg1, cg2);

        // 1. Vector Dimension (M)
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

        // 2. Cultural Diffusion Rate (\alpha)
        Label diffLbl = new Label(I18n.getOrDefault("scenario.label.cultural_diffusion", "Cultural Diffusion Conductivity (α):"));
        diffLbl.getStyleClass().add("control-label");
        culturalDiffusionRateSpinner = new Spinner<>(0.001, 0.50, 0.05, 0.01);
        culturalDiffusionRateSpinner.setEditable(true);
        culturalDiffusionRateSpinner.setMaxWidth(Double.MAX_VALUE);
        culturalDiffusionRateSpinner.valueProperty().addListener((obs, o, n) -> context.notifyParamChange());
        Tooltip.install(culturalDiffusionRateSpinner, new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_diffusion",
                "Diffusion rate of association energy between neighboring H3 hexagons.")));

        // 3. Cultural Mutation Noise Rate (\sigma)
        Label mutLbl = new Label(I18n.getOrDefault("scenario.label.cultural_mutation", "Mutation / Drift Noise (1/√sN):"));
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

        // Layer Import Panel
        VBox layersPanel = new VBox(8);
        layersPanel.getStyleClass().add("layers-panel-card");

        Label layerTitle = new Label(I18n.getOrDefault("scenario.header.tensor_subblocks",
                "🗺 3.2 Cartographic Sub-Blocks per Tensor (Procedural Generation / Imported Maps)"));
        layerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        layerTitle.setWrapText(true);

        btnGenerateProceduralTensorsSection = new Button(I18n.getOrDefault("scenario.btn.regen_tensors", "⚡ Régénérer les Tenseurs"));
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

        Button cultRandSeedBtn = new Button("🎲");
        cultRandSeedBtn.getStyleClass().add("button-secondary");
        cultRandSeedBtn.setStyle("-fx-font-size: 11px;");
        cultRandSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_tensor_seed",
                "Tirer une nouvelle graine stochastique aléatoire pour les tenseurs culturels (sans affecter la densité de population).")));

        cultRandSeedBtn.setOnAction(e -> {
            context.notifyParamChange();
            String newSeed = String.valueOf(new Random().nextLong(1000000));
            cultSeedField.setText(newSeed);
            generateProceduralCulturalTensors();
        });

        culturalHelpBtn = new Button(I18n.getOrDefault("scenario.btn.cultural_format_help", "ℹ Format Calques"));
        culturalHelpBtn.getStyleClass().add("button-secondary");
        culturalHelpBtn.setStyle("-fx-font-size: 11px;");
        culturalHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        culturalHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_specs",
                "Specifications of image formats and cartographic data for cultural and geopolitical layer import.")));
        culturalHelpBtn.setOnAction(e -> ScenarioGisExporter.showCulturalImportFormatHelp(context.getWindow()));

        HBox cultSeedBox = new HBox(4, new Label(I18n.getOrDefault("scenario.label.cultural_seed", "Graine :")), new Label("🎲"), cultSeedField, cultRandSeedBtn);
        cultSeedBox.setAlignment(Pos.CENTER_LEFT);

        HBox seedRow = new HBox(8, cultSeedBox, btnGenerateProceduralTensorsSection);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnGenerateProceduralTensorsSection, Priority.ALWAYS);

        Button btnExportGisMultiFormat = new Button(I18n.getOrDefault("scenario.btn.export_gis", "🗺 Export SIG Multi-Format"));
        btnExportGisMultiFormat.getStyleClass().add("button-secondary");
        btnExportGisMultiFormat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportGisMultiFormat.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_gis",
                "Export scenario density layers and 9 cultural tensors under professional GIS formats (GeoTIFF, NetCDF-4, GeoJSON).")));
        btnExportGisMultiFormat.setOnAction(e -> ScenarioGisExporter.exportGisMultiFormat(
                context.getWindow(),
                context.getScenarioDescription(),
                context.getStartYear(),
                cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9
        ));

        Button btnExportProvenanceManifest = new Button(I18n.getOrDefault("scenario.btn.provenance_manifest", "📜 Manifeste Provenance SHA-256"));
        btnExportProvenanceManifest.getStyleClass().add("button-secondary");
        btnExportProvenanceManifest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.gen_provenance",
                "Generate a cryptographic JSON manifest (provenance.json) containing SHA-256 hashes of datasets and parameters.")));
        btnExportProvenanceManifest.setOnAction(e -> ScenarioGisExporter.exportProvenanceManifest(
                context.getWindow(),
                context.getStartYear(),
                cultSeedField != null ? cultSeedField.getText() : "54321"
        ));

        Button btnCulturalMatrix = new Button(I18n.getOrDefault("scenario.btn.cultural_matrix_inspect", "🧬 Inspecteur des Affinités Culturelles"));
        btnCulturalMatrix.getStyleClass().add("button-secondary");
        btnCulturalMatrix.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnCulturalMatrix.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_matrix",
                "Inspect the N × N Pairwise Cultural Affinity and Distance Heatmap Matrix.")));
        btnCulturalMatrix.setOnAction(e -> {
            long epoch = context.getStartYear();
            new CulturalAffinityMatrixDialog(epoch, context.getScenario()).show();
        });

        HBox actionButtonsRow = new HBox(8, culturalHelpBtn, btnExportGisMultiFormat, btnExportProvenanceManifest);
        actionButtonsRow.setAlignment(Pos.CENTER_LEFT);

        VBox matrixCard = new VBox(4);
        matrixCard.setStyle("-fx-padding: 8px 10px; -fx-background-color: rgba(30, 41, 59, 0.65); -fx-background-radius: 6px; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6px; -fx-border-width: 1px;");
        Label matrixTitle = new Label(I18n.getOrDefault("scenario.cultural_matrix.title", "Matrice d'Affinité & Distances Culturelles"));
        matrixTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #f8fafc;");
        Label matrixDesc = new Label(I18n.getOrDefault("scenario.cultural_matrix.desc",
                "Inspecter et éditer la matrice N×N des distances de Mahalanobis et coefficients d'assimilation entre entités culturelles."));
        matrixDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
        matrixDesc.setWrapText(true);
        matrixCard.getChildren().addAll(matrixTitle, matrixDesc, btnCulturalMatrix);

        VBox seedAndActionBox = new VBox(8, seedRow, actionButtonsRow, matrixCard);
        VBox layerHeaderBox = new VBox(6, layerTitle, seedAndActionBox);

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

    public String getDefaultTensorName(int index) {
        return ScenarioCulturalTensorCatalog.getDefaultTensorName(index);
    }

    public String getCulturalTensorTitle(int index) {
        return ScenarioCulturalTensorCatalog.getCulturalTensorTitle(index, customTensorNames);
    }

    public String getCulturalTensorTooltip(int index) {
        return ScenarioCulturalTensorCatalog.getCulturalTensorTooltip(index);
    }

    public String getCulturalTensorPreviewName(int index) {
        return ScenarioCulturalTensorCatalog.getCulturalTensorPreviewName(index, customTensorNames);
    }

    public String getCulturalBaselineName(int index) {
        return ScenarioCulturalTensorCatalog.getCulturalBaselineName(index);
    }

    public String getCulturalFormatHint(int index) {
        return ScenarioCulturalTensorCatalog.getCulturalFormatHint(index);
    }

    public String pickCulturalSourceForEpoch(int tensorIdx, long startYear, PlanetPreset planet) {
        return ScenarioCulturalTensorCatalog.pickCulturalSourceForEpoch(tensorIdx, startYear, planet);
    }

    public ComboBox<String> buildCulturalSourceCombo(int tensorIdx) {
        ComboBox<String> combo = ScenarioCulturalTensorCatalog.buildCulturalSourceCombo(tensorIdx);
        long scYear = context.getStartYear();
        String autoSrc = pickCulturalSourceForEpoch(tensorIdx, scYear, context.getActivePlanetPreset());
        DataSourceUIHelper.selectBestSource(combo, autoSrc, "Terre");
        combo.setOnAction(e -> {
            if (context.isUpdatingFromPreset()) return;
            String val = combo.getValue();
            if (val != null && !val.isEmpty()) {
                if (customTensorImages.containsKey(tensorIdx) && customTensorImages.get(tensorIdx) != null && tensorImportRadios.containsKey(tensorIdx)) {
                    tensorImportRadios.get(tensorIdx).setSelected(true);
                }
                context.notifyParamChange();
                context.drawPreview();
            }
        });
        return combo;
    }

    public ScenarioCulturalTensorCatalog.TensorParamDescriptor getTensorParam1Descriptor(int tensorIdx) {
        return ScenarioCulturalTensorCatalog.getTensorParam1Descriptor(tensorIdx);
    }

    public ScenarioCulturalTensorCatalog.TensorParamDescriptor getTensorParam2Descriptor(int tensorIdx) {
        return ScenarioCulturalTensorCatalog.getTensorParam2Descriptor(tensorIdx);
    }

    public ScenarioCulturalTensorCatalog.TensorParamDescriptor getTensorParam3Descriptor(int tensorIdx) {
        return ScenarioCulturalTensorCatalog.getTensorParam3Descriptor(tensorIdx);
    }

    public String getDefaultTensorSeed(int tensorIdx) {
        return ScenarioCulturalTensorCatalog.getDefaultTensorSeed(tensorIdx);
    }

    public void updateTensorFileLabel(int tensorIdx) {
        Label fileLbl = tensorFileLabels.get(tensorIdx);
        Button loadBtn = tensorLoadBtns.get(tensorIdx);
        if (fileLbl == null) return;
        boolean isProc = tensorProcRadios.containsKey(tensorIdx) && tensorProcRadios.get(tensorIdx).isSelected();
        Image img = customTensorImages.get(tensorIdx);

        if (isProc) {
            fileLbl.setText(I18n.getOrDefault("scenario.tensor.status.procedural", "⚡ Mode procédural actif"));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            if (loadBtn != null) loadBtn.setStyle("");
            return;
        }

        if (img == null) {
            fileLbl.setText(I18n.getOrDefault("scenario.tensor.file.none", "⚠ Aucune carte chargée — Fichier requis en mode import"));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #f59e0b;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            return;
        }

        ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(img);
        if (!val.valid()) {
            fileLbl.setText(String.format(I18n.getOrDefault("scenario.tensor.file.invalid", "⚠ Carte incompatible : %s"), val.message()));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
        } else {
            fileLbl.setText(String.format(I18n.getOrDefault("scenario.tensor.file.loaded_res", "✔ Carte chargée : %s (%dx%d)"), getCulturalBaselineName(tensorIdx), val.width(), val.height()));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8;");
            if (loadBtn != null) loadBtn.setStyle("");
        }
    }

    public void exportCulturalTensor(int tensorIdx) {
        Image img = customTensorImages.get(tensorIdx);
        if (img == null) {
            img = generateProceduralTensorRasterImage(tensorIdx, 2048, 1024);
        }
        String defaultName = "cultural_tensor_" + (tensorIdx + 1) + ".png";
        String title = I18n.getOrDefault("scenario.title.export_tensor_dialog", "Export Cultural Tensor " + (tensorIdx + 1));
        WindowUtils.exportImageWithChooser(context.getWindow(), img, defaultName, title);
    }

    public Image generateProceduralTensorRasterImage(int tensorIdx, int w, int h) {
        double p1 = tensorParam1Sliders.containsKey(tensorIdx) ? tensorParam1Sliders.get(tensorIdx).getValue() : 1.0;
        double p2 = tensorParam2Sliders.containsKey(tensorIdx) ? tensorParam2Sliders.get(tensorIdx).getValue() : 1.0;
        double p3 = tensorParam3Sliders.containsKey(tensorIdx) ? tensorParam3Sliders.get(tensorIdx).getValue() : 1.0;
        long seed = 11235L + tensorIdx * 11111L;
        if (tensorSeedFields.containsKey(tensorIdx)) {
            try { seed = Long.parseLong(tensorSeedFields.get(tensorIdx).getText().trim()); } catch (Exception ignored) {}
        }
        return ScenarioProceduralTensorRasterizer.generateProceduralTensorRasterImage(tensorIdx, w, h, p1, p2, p3, seed);
    }

    public Color evaluateProceduralTensorColor(int tIndex, double lat, double lon, double elevation, double p1, double p2, double p3, long seed) {
        return ScenarioProceduralTensorRasterizer.evaluateProceduralTensorColor(tIndex, lat, lon, elevation, p1, p2, p3, seed);
    }

    private HBox createTensorParamRow(ScenarioCulturalTensorCatalog.TensorParamDescriptor desc, Slider slider, Label valueLabel, Runnable onChange) {
        Label label = new Label(I18n.getOrDefault(desc.labelKey, desc.defaultLabel));
        label.getStyleClass().add("control-label");
        label.setStyle("-fx-font-size: 10px;");
        label.setMinWidth(140);
        label.setMaxWidth(140);
        label.setWrapText(true);

        slider.setMin(desc.min);
        slider.setMax(desc.max);
        slider.setValue(desc.defVal);
        slider.setBlockIncrement(desc.step);
        slider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(slider, Priority.ALWAYS);

        valueLabel.setText(String.format(java.util.Locale.ROOT, desc.format, desc.defVal));
        valueLabel.getStyleClass().add("value-label");
        valueLabel.setStyle("-fx-font-size: 10px;");
        valueLabel.setMinWidth(65);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLabel.setText(String.format(java.util.Locale.ROOT, desc.format, newVal.doubleValue()));
            if (!context.isUpdatingFromPreset()) {
                onChange.run();
            }
        });

        String tip = I18n.getOrDefault(desc.tooltipKey, desc.defaultTooltip);
        if (tip != null && !tip.isEmpty()) {
            Tooltip.install(label, new Tooltip(tip));
            Tooltip.install(slider, new Tooltip(tip));
        }

        UIControlsHelper.attachDefaultValueHandling(slider, desc.defVal, () -> slider.setValue(desc.defVal));

        HBox row = new HBox(6, label, slider, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public void deleteCulturalTensor(int indexToDelete) {
        int curDims = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        if (indexToDelete < 0 || indexToDelete >= curDims) return;

        // Shift all configurations from indexToDelete up to curDims - 2
        for (int j = indexToDelete; j < curDims - 1; j++) {
            if (customTensorNames.containsKey(j + 1)) {
                customTensorNames.put(j, customTensorNames.get(j + 1));
            } else {
                customTensorNames.remove(j);
            }
            if (customTensorImages.containsKey(j + 1)) {
                customTensorImages.put(j, customTensorImages.get(j + 1));
            } else {
                customTensorImages.remove(j);
            }
            if (tensorSeedFields.containsKey(j + 1)) {
                String s = tensorSeedFields.get(j + 1).getText();
                if (tensorSeedFields.containsKey(j)) tensorSeedFields.get(j).setText(s);
                else tensorSeedFields.put(j, new TextField(s));
            }
            if (tensorParam1Sliders.containsKey(j + 1) && tensorParam1Sliders.containsKey(j)) {
                tensorParam1Sliders.get(j).setValue(tensorParam1Sliders.get(j + 1).getValue());
            }
            if (tensorParam2Sliders.containsKey(j + 1) && tensorParam2Sliders.containsKey(j)) {
                tensorParam2Sliders.get(j).setValue(tensorParam2Sliders.get(j + 1).getValue());
            }
            if (tensorParam3Sliders.containsKey(j + 1) && tensorParam3Sliders.containsKey(j)) {
                tensorParam3Sliders.get(j).setValue(tensorParam3Sliders.get(j + 1).getValue());
            }
            if (tensorImportRadios.containsKey(j + 1) && tensorImportRadios.get(j + 1).isSelected()) {
                if (tensorImportRadios.containsKey(j)) tensorImportRadios.get(j).setSelected(true);
            } else {
                if (tensorProcRadios.containsKey(j)) tensorProcRadios.get(j).setSelected(true);
            }
        }

        int lastIdx = curDims - 1;
        customTensorNames.remove(lastIdx);
        customTensorImages.remove(lastIdx);
        tensorSeedFields.remove(lastIdx);
        tensorParam1Sliders.remove(lastIdx);
        tensorParam2Sliders.remove(lastIdx);
        tensorParam3Sliders.remove(lastIdx);
        tensorParam1ValueLabels.remove(lastIdx);
        tensorParam2ValueLabels.remove(lastIdx);
        tensorParam3ValueLabels.remove(lastIdx);
        tensorProcRadios.remove(lastIdx);
        tensorImportRadios.remove(lastIdx);
        tensorSourceCombos.remove(lastIdx);
        tensorSourceLabels.remove(lastIdx);
        tensorFileLabels.remove(lastIdx);
        tensorLoadBtns.remove(lastIdx);
        tensorFormatLabels.remove(lastIdx);
        tensorGenSingleBtns.remove(lastIdx);
        tensorSubTitles.remove(lastIdx);

        int newDims = Math.max(0, curDims - 1);
        if (cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValueFactory() != null) {
            cultureVectorDimSpinner.getValueFactory().setValue(newDims);
        } else {
            rebuildCulturalTensorSubBlocks(newDims);
            context.updatePreviewModesCombo();
            context.notifyParamChange();
            context.drawPreview();
        }
    }

    /**
     * Updates all localized UI texts and tooltips across the cultural section.
     */
    public void updateTexts() {
        if (cultureHeader != null) {
            cultureHeader.setText(I18n.getOrDefault("scenario.culture_section", "🎭 CULTURAL VECTOR DIMENSION & MULTI-FIELD LAYERS"));
        }
        updateCulturalTensorSubBlockTexts();
    }

    public void updateCulturalTensorSubBlockTexts() {
        if (btnGenerateProceduralTensorsSection != null) {
            btnGenerateProceduralTensorsSection.setText(I18n.getOrDefault("scenario.btn.regen_tensors", "⚡ Régénérer les Tenseurs"));
            btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors",
                    "Bascule tous les tenseurs en mode procédural et régénère les cartes selon les paramètres et la graine stochastique.")));
        }
        int dims = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        if (layersDynamicContainer == null || layersDynamicContainer.getChildren().size() != dims) {
            rebuildCulturalTensorSubBlocks(dims);
            return;
        }
        for (int i = 0; i < dims; i++) {
            Label subTitle = tensorSubTitles.get(i);
            if (subTitle != null) {
                subTitle.setText(getCulturalTensorTitle(i));
                Tooltip.install(subTitle, new Tooltip(getCulturalTensorTooltip(i)));
            }
            RadioButton radioProc = tensorProcRadios.get(i);
            if (radioProc != null) {
                radioProc.setText(I18n.getOrDefault("scenario.mode.procedural_sde", "⚡ Mode Procédural (Paramètres adaptatifs & Graine)"));
                radioProc.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_proc",
                        "Génère procéduralement ce tenseur culturel via des équations stochastiques et les paramètres ci-dessous.")));
            }
            RadioButton radioImport = tensorImportRadios.get(i);
            if (radioImport != null) {
                radioImport.setText(I18n.getOrDefault("scenario.mode.spatial_import", "📁 Importation Carte Spatiale (PNG / GeoJSON)"));
                radioImport.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_import",
                        "Importe une image matricielle ou une couche SIG externe pour modéliser ce tenseur culturel.")));
            }
            Label srcLbl = tensorSourceLabels.get(i);
            if (srcLbl != null) {
                srcLbl.setText(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            }
            Button loadBtn = tensorLoadBtns.get(i);
            if (loadBtn != null) {
                loadBtn.setText(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
            }
            Button genBtn = tensorGenSingleBtns.get(i);
            if (genBtn != null) {
                genBtn.setText(I18n.getOrDefault("scenario.tensor.btn.export_single", "💾 Export"));
            }
            Label fmtLbl = tensorFormatLabels.get(i);
            if (fmtLbl != null) {
                fmtLbl.setText(getCulturalFormatHint(i));
            }
            ComboBox<String> sc = tensorSourceCombos.get(i);
            if (sc != null && sc.getCellFactory() != null) {
                sc.setButtonCell(sc.getCellFactory().call(null));
            }
        }
    }

    public void rebuildCulturalTensorSubBlocks(int dimCount) {
        if (btnGenerateProceduralTensorsSection != null) {
            btnGenerateProceduralTensorsSection.setText(I18n.getOrDefault("scenario.btn.regen_tensors", "⚡ Régénérer les Tenseurs"));
            btnGenerateProceduralTensorsSection.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors",
                    "Bascule tous les tenseurs en mode procédural et régénère les cartes selon les paramètres et la graine stochastique.")));
        }
        if (layersDynamicContainer == null) return;
        layersDynamicContainer.getChildren().clear();

        for (int i = 0; i < dimCount; i++) {
            final int tensorIdx = i;

            VBox subBlock = new VBox(6);
            subBlock.setStyle("-fx-padding: 10; -fx-background-color: rgba(167,139,250,0.04); -fx-background-radius: 6; -fx-border-color: rgba(167,139,250,0.15); -fx-border-radius: 6;");

            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label subTitle = new Label(getCulturalTensorTitle(i));
            subTitle.getStyleClass().add("control-label");
            subTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            subTitle.setWrapText(true);
            HBox.setHgrow(subTitle, Priority.ALWAYS);
            Tooltip.install(subTitle, new Tooltip(getCulturalTensorTooltip(i)));
            tensorSubTitles.put(tensorIdx, subTitle);

            Button renameBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.rename", "✏ Renommer"));
            renameBtn.getStyleClass().add("button-secondary");
            renameBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            renameBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.rename_tooltip",
                    "Cliquez pour personnaliser le nom de ce calque de tenseur culturel.")));
            renameBtn.setOnAction(e -> {
                String cur = customTensorNames.getOrDefault(tensorIdx, "");
                TextInputDialog dlg = new TextInputDialog(cur);
                dlg.setTitle(I18n.getOrDefault("scenario.tensor.rename.title", "Personnaliser le Nom du Tenseur"));
                dlg.setHeaderText(I18n.getOrDefault("scenario.tensor.rename.prompt", "Nom personnalisé du Tenseur N°{0} :", (tensorIdx + 1)));
                WindowUtils.applyWindowIcon(dlg);
                dlg.showAndWait().ifPresent(strVal -> {
                    if (strVal.isBlank()) {
                        customTensorNames.remove(tensorIdx);
                    } else {
                        customTensorNames.put(tensorIdx, strVal.trim());
                    }
                    subTitle.setText(getCulturalTensorTitle(tensorIdx));
                    context.updatePreviewModesCombo();
                    context.notifyParamChange();
                });
            });

            Button deleteBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.delete", "✕"));
            deleteBtn.getStyleClass().add("button-secondary");
            deleteBtn.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 2 6; -fx-font-weight: bold; -fx-cursor: hand;");
            deleteBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.delete_tooltip",
                    "Supprimer ce tenseur culturel (décrémente le nombre total de tenseurs).")));
            deleteBtn.setOnAction(e -> deleteCulturalTensor(tensorIdx));

            titleRow.getChildren().addAll(subTitle, renameBtn, deleteBtn);
            subBlock.getChildren().add(titleRow);

            ToggleGroup tg = new ToggleGroup();
            RadioButton radioProc = new RadioButton(I18n.getOrDefault("scenario.mode.procedural_sde", "⚡ Mode Procédural (Paramètres adaptatifs & Graine)"));
            RadioButton radioImport = new RadioButton(I18n.getOrDefault("scenario.mode.spatial_import", "📁 Importation Carte Spatiale (PNG / GeoJSON)"));
            radioProc.setToggleGroup(tg);
            radioImport.setToggleGroup(tg);
            radioProc.getStyleClass().add("radio-proc");
            radioImport.getStyleClass().add("radio-import");
            radioProc.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_proc",
                    "Génère procéduralement ce tenseur culturel via des équations stochastiques et les paramètres ci-dessous.")));
            radioImport.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_import",
                    "Importe une image matricielle ou une couche SIG externe pour modéliser ce tenseur culturel.")));

            boolean hasImage = customTensorImages.containsKey(tensorIdx) && customTensorImages.get(tensorIdx) != null;
            if (hasImage) {
                radioImport.setSelected(true);
            } else {
                radioProc.setSelected(true);
            }

            tensorProcRadios.put(tensorIdx, radioProc);
            tensorImportRadios.put(tensorIdx, radioImport);

            // --- 1. Procedural Configuration Sub-Box ---
            Label seedLbl = new Label(I18n.getOrDefault("scenario.tensor.seed.label", "Graine :"));
            seedLbl.getStyleClass().add("control-label");
            seedLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
            seedLbl.setMinWidth(60);

            TextField seedField = tensorSeedFields.computeIfAbsent(tensorIdx, k -> new TextField(getDefaultTensorSeed(tensorIdx)));
            seedField.setStyle("-fx-font-size: 10px; -fx-pref-width: 80px;");
            seedField.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.tensor_seed_field",
                    "Graine stochastique déterministe pour initialiser ce champ de tenseur culturel.")));
            seedField.textProperty().addListener((obs, o, n) -> {
                if (!context.isUpdatingFromPreset()) {
                    context.notifyParamChange();
                    context.drawPreview();
                }
            });

            Button randBtn = new Button("🎲");
            randBtn.getStyleClass().add("button-secondary");
            randBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            randBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.random_tensor_seed", "Tirer une nouvelle graine aléatoire pour ce tenseur.")));
            randBtn.setOnAction(e -> {
                seedField.setText(String.valueOf(new Random().nextLong(1000000)));
                if (radioProc != null) {
                    radioProc.setSelected(true);
                }
                context.selectPreviewMode(tensorIdx + 1);
                context.notifyParamChange();
                context.drawPreview();
            });

            Button exportSingleBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.export_single", "💾 Export"));
            exportSingleBtn.getStyleClass().add("button-secondary");
            exportSingleBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 8; -fx-font-weight: bold;");
            exportSingleBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.export_single_tooltip",
                    "Export this cultural tensor raster as a high-resolution PNG or JPEG file.")));
            tensorGenSingleBtns.put(tensorIdx, exportSingleBtn);
            exportSingleBtn.setOnAction(e -> exportCulturalTensor(tensorIdx));

            HBox seedRow = new HBox(6, seedLbl, seedField, randBtn, exportSingleBtn);
            seedRow.setAlignment(Pos.CENTER_LEFT);

            Slider p1Slider = tensorParam1Sliders.computeIfAbsent(tensorIdx, k -> new Slider());
            Label p1Val = tensorParam1ValueLabels.computeIfAbsent(tensorIdx, k -> new Label());
            HBox p1Row = createTensorParamRow(getTensorParam1Descriptor(tensorIdx), p1Slider, p1Val, () -> { context.notifyParamChange(); context.drawPreview(); });

            Slider p2Slider = tensorParam2Sliders.computeIfAbsent(tensorIdx, k -> new Slider());
            Label p2Val = tensorParam2ValueLabels.computeIfAbsent(tensorIdx, k -> new Label());
            HBox p2Row = createTensorParamRow(getTensorParam2Descriptor(tensorIdx), p2Slider, p2Val, () -> { context.notifyParamChange(); context.drawPreview(); });

            Slider p3Slider = tensorParam3Sliders.computeIfAbsent(tensorIdx, k -> new Slider());
            Label p3Val = tensorParam3ValueLabels.computeIfAbsent(tensorIdx, k -> new Label());
            HBox p3Row = createTensorParamRow(getTensorParam3Descriptor(tensorIdx), p3Slider, p3Val, () -> { context.notifyParamChange(); context.drawPreview(); });

            Label procStatusLbl = new Label(I18n.getOrDefault("scenario.status.proc_gen_tensor_prefix", "✔ Modélisation procédurale dynamique active (Tenseur ") + (tensorIdx + 1) + ")");
            procStatusLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #10b981;");

            VBox procBox = new VBox(5, seedRow, p1Row, p2Row, p3Row, procStatusLbl);
            procBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            procBox.setVisible(!hasImage);
            procBox.setManaged(!hasImage);

            // --- 2. Import Configuration Sub-Box ---
            Label sourceLbl = new Label(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            tensorSourceLabels.put(tensorIdx, sourceLbl);

            ComboBox<String> sourceCombo = buildCulturalSourceCombo(tensorIdx);
            tensorSourceCombos.put(tensorIdx, sourceCombo);

            Button btnLoad = new Button(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
            btnLoad.getStyleClass().add("button-secondary");
            btnLoad.setStyle("-fx-font-size: 11px;");
            btnLoad.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.load_cultural_tensor_map",
                    "Ouvre un sélecteur de fichier pour importer une carte raster externe (PNG/GeoTIFF) pour ce tenseur.")));
            tensorLoadBtns.put(tensorIdx, btnLoad);

            Button btnClear = new Button("🗑");
            btnClear.getStyleClass().add("button-secondary");
            btnClear.setStyle("-fx-font-size: 11px;");
            btnClear.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.clear_cultural_tensor_map",
                    "Efface l'image importée et réinitialise le tenseur au mode procédural par défaut.")));

            HBox btnBox = new HBox(6, btnLoad, btnClear);
            btnBox.setAlignment(Pos.CENTER_LEFT);

            Label fileLbl = new Label();
            fileLbl.getStyleClass().add("value-label");
            fileLbl.setStyle("-fx-font-size: 10px;");
            tensorFileLabels.put(tensorIdx, fileLbl);
            updateTensorFileLabel(tensorIdx);

            Label formatHintLbl = new Label(getCulturalFormatHint(tensorIdx));
            formatHintLbl.getStyleClass().add("card-description-muted");
            formatHintLbl.setStyle("-fx-font-size: 9px; -fx-font-style: italic;");
            formatHintLbl.setWrapText(true);
            tensorFormatLabels.put(tensorIdx, formatHintLbl);

            VBox importBox = new VBox(6, sourceLbl, sourceCombo, btnBox, fileLbl, formatHintLbl);
            importBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            importBox.setVisible(hasImage);
            importBox.setManaged(hasImage);

            tg.selectedToggleProperty().addListener((obs, oldV, sel) -> {
                boolean isProc = sel == radioProc;
                procBox.setVisible(isProc);
                procBox.setManaged(isProc);
                importBox.setVisible(!isProc);
                importBox.setManaged(!isProc);
                updateTensorFileLabel(tensorIdx);
                context.selectPreviewMode(tensorIdx + 1);
                if (!context.isUpdatingFromPreset()) {
                    context.notifyParamChange();
                    context.drawPreview();
                }
            });

            btnLoad.setOnAction(e -> loadCustomCultureLayerForTensor(tensorIdx, img -> {
                customTensorImages.put(tensorIdx, img);
                updateTensorFileLabel(tensorIdx);
                context.selectPreviewMode(tensorIdx + 1);
                radioImport.setSelected(true);
                context.notifyParamChange();
                context.drawPreview();
            }));

            btnClear.setOnAction(e -> {
                customTensorImages.remove(tensorIdx);
                updateTensorFileLabel(tensorIdx);
                context.selectPreviewMode(tensorIdx + 1);
                radioProc.setSelected(true);
                context.notifyParamChange();
                context.drawPreview();
            });

            subBlock.getChildren().addAll(subTitle, radioProc, procBox, radioImport, importBox);
            layersDynamicContainer.getChildren().add(subBlock);
        }
    }

    public void loadCustomCultureLayerForTensor(int tensorIdx, Consumer<Image> onLoaded) {
        String name = getCulturalTensorTitle(tensorIdx);
        loadCustomCultureLayer(name, onLoaded);
    }

    public void loadCustomCultureLayer(String layerName, Consumer<Image> onLoaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.dialog.import_layer_title", "Import layer ") + layerName + " (PNG/GeoJSON/JPG)");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images & Cartes", "*.png", "*.jpg", "*.jpeg", "*.geojson", "*.json")
        );
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
                onLoaded.accept(img);
            } catch (Exception ex) {
                logger.error("Failed to load culture layer {}", layerName, ex);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de chargement: " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    public void generateProceduralCulturalTensors() {
        if (tensorProcRadios != null) {
            for (var entry : tensorProcRadios.entrySet()) {
                if (entry.getValue() != null) {
                    entry.getValue().setSelected(true);
                }
            }
        }
        customTensorImages.clear();

        context.selectPreviewMode(1); // Select first cultural tensor (isogloss)
        if (tensorFileLabels != null) {
            for (Integer idx : tensorFileLabels.keySet()) {
                updateTensorFileLabel(idx);
            }
        }
        context.notifyParamChange();

        Scenario s = context.getScenario();
        if (s == null) return;
        if (btnGenerateProceduralTensorsSection != null) btnGenerateProceduralTensorsSection.setDisable(true);
        CompletableFuture.runAsync(() -> {
            org.ether.society.data.HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(s);
            org.ether.society.data.HistoricalMapGenerator.generateProceduralMapsForScenario(s);
        }).thenRun(() -> javafx.application.Platform.runLater(() -> {
            if (btnGenerateProceduralTensorsSection != null) btnGenerateProceduralTensorsSection.setDisable(false);
            int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
            for (int i = 0; i < dims; i++) {
                String b64 = s.getCustomTensorMapBase64(i);
                if (b64 != null && !b64.isBlank()) {
                    Image img = ImageMapLoader.base64PngToImage(b64);
                    if (img != null) {
                        customTensorImages.put(i, img);
                    }
                }
                updateTensorFileLabel(i);
            }
            context.notifyParamChange();
            context.drawPreview();
        }));
    }

    /**
     * Applies state from a Scenario entity into UI controls.
     *
     * @param s source scenario
     */
    public void applyScenario(Scenario s) {
        if (s == null) return;
        if (s.getCultureVectorDimensions() > 0 && cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValueFactory() != null) {
            cultureVectorDimSpinner.getValueFactory().setValue(s.getCultureVectorDimensions());
        }
        if (s.getCulturalDiffusionRate() > 0 && culturalDiffusionRateSpinner != null && culturalDiffusionRateSpinner.getValueFactory() != null) {
            culturalDiffusionRateSpinner.getValueFactory().setValue(s.getCulturalDiffusionRate());
        }
        if (s.getCulturalMutationRate() > 0 && culturalMutationRateSpinner != null && culturalMutationRateSpinner.getValueFactory() != null) {
            culturalMutationRateSpinner.getValueFactory().setValue(s.getCulturalMutationRate());
        }
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        customTensorImages.clear();
        customTensorNames.clear();
        if (s.getCustomTensorNames() != null) {
            customTensorNames.putAll(s.getCustomTensorNames());
        }

        for (int i = 0; i < dims; i++) {
            String b64 = s.getCustomTensorMapBase64(i);
            if (b64 != null && !b64.isBlank()) {
                Image img = ImageMapLoader.base64PngToImage(b64);
                if (img != null) {
                    customTensorImages.put(i, img);
                }
            }
        }

        rebuildCulturalTensorSubBlocks(dims);

        // Procedural mode flags and parameters
        if (s.getTensorProceduralModes() != null) {
            for (int i = 0; i < Math.min(dims, s.getTensorProceduralModes().size()); i++) {
                boolean isProc = s.getTensorProceduralModes().get(i);
                if (isProc && tensorProcRadios.containsKey(i)) {
                    tensorProcRadios.get(i).setSelected(true);
                } else if (!isProc && tensorImportRadios.containsKey(i)) {
                    tensorImportRadios.get(i).setSelected(true);
                }
            }
        }

        if (s.getTensorSeeds() != null) {
            for (var entry : s.getTensorSeeds().entrySet()) {
                if (tensorSeedFields.containsKey(entry.getKey())) {
                    tensorSeedFields.get(entry.getKey()).setText(String.valueOf(entry.getValue()));
                }
            }
        }

        if (s.getTensorProceduralParameters() != null) {
            for (var entry : s.getTensorProceduralParameters().entrySet()) {
                int tIdx = entry.getKey();
                var pMap = entry.getValue();
                if (pMap == null) continue;
                ScenarioCulturalTensorCatalog.TensorParamDescriptor d1 = getTensorParam1Descriptor(tIdx);
                ScenarioCulturalTensorCatalog.TensorParamDescriptor d2 = getTensorParam2Descriptor(tIdx);
                ScenarioCulturalTensorCatalog.TensorParamDescriptor d3 = getTensorParam3Descriptor(tIdx);
                if (pMap.containsKey(d1.labelKey) && tensorParam1Sliders.containsKey(tIdx)) {
                    tensorParam1Sliders.get(tIdx).setValue(pMap.get(d1.labelKey));
                }
                if (pMap.containsKey(d2.labelKey) && tensorParam2Sliders.containsKey(tIdx)) {
                    tensorParam2Sliders.get(tIdx).setValue(pMap.get(d2.labelKey));
                }
                if (pMap.containsKey(d3.labelKey) && tensorParam3Sliders.containsKey(tIdx)) {
                    tensorParam3Sliders.get(tIdx).setValue(pMap.get(d3.labelKey));
                }
            }
        }

        for (int i = 0; i < dims; i++) {
            updateTensorFileLabel(i);
        }
    }

    /**
     * Exports cultural section parameters into a Scenario instance.
     *
     * @param s target scenario
     */
    public void exportToScenario(Scenario s) {
        if (s == null) return;
        int dims = cultureVectorDimSpinner != null ? cultureVectorDimSpinner.getValue() : 9;
        s.setCultureVectorDimensions(dims);
        s.setCustomTensorNames(new HashMap<>(customTensorNames));
        if (culturalDiffusionRateSpinner != null) {
            s.setCulturalDiffusionRate(culturalDiffusionRateSpinner.getValue());
        }
        if (culturalMutationRateSpinner != null) {
            s.setCulturalMutationRate(culturalMutationRateSpinner.getValue());
        }
        Map<Integer, Long> tSeeds = new HashMap<>();
        Map<Integer, Map<String, Double>> tProcParams = new HashMap<>();

        for (int i = 0; i < dims; i++) {
            if (customTensorImages.get(i) != null) {
                s.setCustomTensorMapBase64(i, ImageMapLoader.imageToBase64Png(customTensorImages.get(i)));
            }
            boolean isProc = tensorProcRadios.containsKey(i) && tensorProcRadios.get(i).isSelected();
            while (s.getTensorProceduralModes().size() <= i) {
                s.getTensorProceduralModes().add(true);
            }
            s.getTensorProceduralModes().set(i, isProc);

            // Tensor Seeds
            if (tensorSeedFields.containsKey(i) && tensorSeedFields.get(i) != null) {
                try {
                    tSeeds.put(i, Long.parseLong(tensorSeedFields.get(i).getText().trim()));
                } catch (NumberFormatException ignored) {
                    try {
                        tSeeds.put(i, Long.parseLong(getDefaultTensorSeed(i)));
                    } catch (Exception e) {
                        tSeeds.put(i, 12345L);
                    }
                }
            } else {
                try {
                    tSeeds.put(i, Long.parseLong(getDefaultTensorSeed(i)));
                } catch (Exception e) {
                    tSeeds.put(i, 12345L);
                }
            }

            // Tensor Procedural Parameters
            Map<String, Double> pMap = new HashMap<>();
            ScenarioCulturalTensorCatalog.TensorParamDescriptor d1 = getTensorParam1Descriptor(i);
            ScenarioCulturalTensorCatalog.TensorParamDescriptor d2 = getTensorParam2Descriptor(i);
            ScenarioCulturalTensorCatalog.TensorParamDescriptor d3 = getTensorParam3Descriptor(i);
            if (tensorParam1Sliders.containsKey(i) && tensorParam1Sliders.get(i) != null) {
                pMap.put(d1.labelKey, tensorParam1Sliders.get(i).getValue());
            }
            if (tensorParam2Sliders.containsKey(i) && tensorParam2Sliders.get(i) != null) {
                pMap.put(d2.labelKey, tensorParam2Sliders.get(i).getValue());
            }
            if (tensorParam3Sliders.containsKey(i) && tensorParam3Sliders.get(i) != null) {
                pMap.put(d3.labelKey, tensorParam3Sliders.get(i).getValue());
            }
            tProcParams.put(i, pMap);
        }
        s.setTensorSeeds(tSeeds);
        s.setTensorProceduralParameters(tProcParams);
    }

    // --- Getters & Accessors ---

    public int getCultureVectorDimensions() {
        return cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
    }

    public double getCulturalDiffusionRate() {
        return culturalDiffusionRateSpinner != null && culturalDiffusionRateSpinner.getValue() != null ? culturalDiffusionRateSpinner.getValue() : 0.05;
    }

    public double getCulturalMutationRate() {
        return culturalMutationRateSpinner != null && culturalMutationRateSpinner.getValue() != null ? culturalMutationRateSpinner.getValue() : 0.02;
    }

    public Image getCustomTensorImage(int index) {
        return customTensorImages.get(index);
    }

    public boolean isTensorImportActive(int index) {
        return tensorImportRadios.containsKey(index) && tensorImportRadios.get(index).isSelected();
    }

    public double getTensorParam1(int index) {
        return tensorParam1Sliders.containsKey(index) ? tensorParam1Sliders.get(index).getValue() : 1.0;
    }

    public double getTensorParam2(int index) {
        return tensorParam2Sliders.containsKey(index) ? tensorParam2Sliders.get(index).getValue() : 1.0;
    }

    public double getTensorParam3(int index) {
        return tensorParam3Sliders.containsKey(index) ? tensorParam3Sliders.get(index).getValue() : 1.0;
    }

    public long getTensorSeed(int index) {
        if (tensorSeedFields.containsKey(index)) {
            try {
                return Long.parseLong(tensorSeedFields.get(index).getText().trim());
            } catch (Exception ignored) {}
        }
        return 11235L + index * 11111L;
    }

    public Spinner<Integer> getCultureVectorDimSpinner() {
        return cultureVectorDimSpinner;
    }

    public Spinner<Double> getCulturalDiffusionRateSpinner() {
        return culturalDiffusionRateSpinner;
    }

    public Spinner<Double> getCulturalMutationRateSpinner() {
        return culturalMutationRateSpinner;
    }

    public TextField getCultSeedField() {
        return cultSeedField;
    }

    public Map<Integer, Image> getCustomTensorImages() {
        return customTensorImages;
    }

    public Map<Integer, String> getCustomTensorNames() {
        return customTensorNames;
    }

    public Map<Integer, RadioButton> getTensorProcRadios() {
        return tensorProcRadios;
    }

    public Map<Integer, RadioButton> getTensorImportRadios() {
        return tensorImportRadios;
    }

    public Map<Integer, Slider> getTensorParam1Sliders() {
        return tensorParam1Sliders;
    }

    public Map<Integer, Slider> getTensorParam2Sliders() {
        return tensorParam2Sliders;
    }

    public Map<Integer, Slider> getTensorParam3Sliders() {
        return tensorParam3Sliders;
    }

    public Map<Integer, TextField> getTensorSeedFields() {
        return tensorSeedFields;
    }

    public Map<Integer, ComboBox<String>> getTensorSourceCombos() {
        return tensorSourceCombos;
    }

    public Map<Integer, Button> getTensorLoadBtns() {
        return tensorLoadBtns;
    }
}
