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
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    /**
     * Gets default tensor name based on index.
     *
     * @param index cultural tensor index
     * @return localized or default tensor name
     */
    public String getDefaultTensorName(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.name.0", "Isoglosses & Continua Linguistiques");
            case 1 -> I18n.getOrDefault("scenario.tensor.name.1", "Structures de Parenté & Lignages");
            case 2 -> I18n.getOrDefault("scenario.tensor.name.2", "Rituels, Croyances & Sacré");
            case 3 -> I18n.getOrDefault("scenario.tensor.name.3", "Souveraineté Politico-Militaire & Capitales");
            case 4 -> I18n.getOrDefault("scenario.tensor.name.4", "Outillage, Matérialité & Technologies");
            case 5 -> I18n.getOrDefault("scenario.tensor.name.5", "Corridors Commerciaux & Réseaux d'Échange");
            case 6 -> I18n.getOrDefault("scenario.tensor.name.6", "Complexité Institutionnelle & Normes");
            case 7 -> I18n.getOrDefault("scenario.tensor.name.7", "Empreinte Écologique & Pression Malthusienne");
            case 8 -> I18n.getOrDefault("scenario.tensor.name.8", "Immunité aux Pathogènes & Mémoire Sanitaire");
            default -> I18n.getOrDefault("scenario.tensor.name.ext", "Substrat Culturel Extensible {0}", (index + 1));
        };
    }

    /**
     * Formats cultural tensor title.
     *
     * @param index zero-based cultural tensor index
     * @return localized title string
     */
    public String getCulturalTensorTitle(int index) {
        if (customTensorNames.containsKey(index) && !customTensorNames.get(index).isBlank()) {
            return "🏛 3.2." + (index + 1) + " " + I18n.getOrDefault("scenario.tensor.label_prefix", "Tenseur ") + (index + 1) + " : " + customTensorNames.get(index);
        }
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.title.0", "🗣 3.2.1 Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.title.1", "👨‍👩‍👧 3.2.2 Tensor 2: Kinship & Clan Structures (Lineage)");
            case 2 -> I18n.getOrDefault("scenario.tensor.title.2", "🕯 3.2.3 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.title.3", "👑 3.2.4 Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.title.4", "⚒ 3.2.5 Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.title.5", "⚖ 3.2.6 Tensor 6: Trade Corridors & Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.title.6", "📜 3.2.7 Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.title.7", "🌾 3.2.8 Tensor 8: Ecological Footprint & Malthusian Stress (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.title.8", "🧪 3.2.9 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> I18n.getOrDefault("scenario.tensor.title.ext", "🏛 3.2.{0} Tensor {0}: Extensible Cultural Substrate {0}", (index + 1));
        };
    }

    /**
     * Formats cultural tensor tooltip.
     *
     * @param index tensor index
     * @return localized tooltip
     */
    public String getCulturalTensorTooltip(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.tooltip.0", "Linguistic component: Dialect continua, mutual intelligibility, and phonetic barriers.");
            case 1 -> I18n.getOrDefault("scenario.tensor.tooltip.1", "Organizational component: Lineage structures, exogamy, clan networks, and alliances.");
            case 2 -> I18n.getOrDefault("scenario.tensor.tooltip.2", "Sacred component: Religious norms, integration rites, taboos, and Asabiyyah social cohesion.");
            case 3 -> I18n.getOrDefault("scenario.tensor.tooltip.3", "Geopolitical component: Administrative centers, allegiance to capitals, and spheres of influence.");
            case 4 -> I18n.getOrDefault("scenario.tensor.tooltip.4", "Material component: Craft traditions, domestic tools, pottery, metallurgy, and architecture.");
            case 5 -> I18n.getOrDefault("scenario.tensor.tooltip.5", "Economic component: Trade corridors (Silk Road, Trans-Sahara, Ocean), trade hubs, and fairs.");
            case 6 -> I18n.getOrDefault("scenario.tensor.tooltip.6", "Institutional component (Seshat): Administrative complexity, codification of customary law, and bureaucracy.");
            case 7 -> I18n.getOrDefault("scenario.tensor.tooltip.7", "Ecological component: Soil degradation, deforestation, salinization, and Malthusian pressure.");
            case 8 -> I18n.getOrDefault("scenario.tensor.tooltip.8", "Sanitary component: Acquired immunity barriers, zoonotic reservoirs, and epidemic vulnerability.");
            default -> String.format(I18n.getOrDefault("scenario.tensor.tooltip.ext", "Composante culturelle multichamp extensible N°%d."), (index + 1));
        };
    }

    /**
     * Formats cultural tensor title for preview dropdown.
     *
     * @param index tensor index
     * @return preview display name
     */
    public String getCulturalTensorPreviewName(int index) {
        if (customTensorNames.containsKey(index) && !customTensorNames.get(index).isBlank()) {
            return "🏛 " + I18n.getOrDefault("scenario.tensor.custom.preview_prefix", "Tensor ") + (index + 1) + " : " + customTensorNames.get(index);
        }
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.1.preview", "🗣 Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.2.preview", "👨‍👩‍👧 Tensor 2: Kinship & Clan Structures (Kinship)");
            case 2 -> I18n.getOrDefault("scenario.tensor.3.preview", "🕯 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.4.preview", "👑 Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.5.preview", "⚒ Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.6.preview", "⚖ Tensor 6: Corridors & Trade Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.7.preview", "📜 Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.8.preview", "🌾 Tensor 8: Ecological Footprint & Malthusian Tension (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.9.preview", "🧪 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> I18n.getOrDefault("scenario.tensor.custom.preview_prefix", "🏛 Tensor ") + (index + 1) + I18n.getOrDefault("scenario.tensor.custom.preview_mid", " : Cultural Substrate ") + (index + 1);
        };
    }

    public String getCulturalBaselineName(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.0.source.baseline", "Glottolog 4.8 / Linguistic Continua (Earth Baseline)");
            case 1 -> I18n.getOrDefault("scenario.tensor.1.source.baseline", "Murdock Ethnographic Atlas / Kinship (Earth Baseline)");
            case 2 -> I18n.getOrDefault("scenario.tensor.2.source.baseline", "Seshat Historical Databank / Sacred (Earth Baseline)");
            case 3 -> I18n.getOrDefault("scenario.tensor.3.source.baseline", "Centennia Historical Atlas / Sovereignty (Earth Baseline)");
            case 4 -> I18n.getOrDefault("scenario.tensor.4.source.baseline", "ArchaeoGLOBE Project / Material Culture (Earth Baseline)");
            case 5 -> I18n.getOrDefault("scenario.tensor.5.source.baseline", "ORBIS / Old World Trade Networks (Earth Baseline)");
            case 6 -> I18n.getOrDefault("scenario.tensor.6.source.baseline", "Seshat Historical Databank / Law & Norms (Earth Baseline)");
            case 7 -> I18n.getOrDefault("scenario.tensor.7.source.baseline", "HYDE 3.4 / Historical Land Use & Degradation (Earth Baseline)");
            case 8 -> I18n.getOrDefault("scenario.tensor.8.source.baseline", "GADM / Historical Pathogen Memory (Earth Baseline)");
            default -> I18n.getOrDefault("scenario.status.empirical_loaded", "📁 Empirical baseline layer loaded");
        };
    }

    public String getCulturalFormatHint(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.0.format", "RGB 24-bit Catégoriel + Dithering Spatial (2:1 équirectangulaire) :\n  Valeurs RGB = Familles linguistiques pures & continuums dialectaux (Glottolog 5.0 / WALS)");
            case 1 -> I18n.getOrDefault("scenario.tensor.1.format", "RGB 24-bit Catégoriel (2:1 équirectangulaire) :\n  Valeurs RGB = Organisation sociale & familiale (Todd / Murdock : Souche #8B5CF6, Nucléaire #2563EB, Communautaire #DC2626, Matrilinéaire #F43F5E)");
            case 2 -> I18n.getOrDefault("scenario.tensor.2.format", "RGB 24-bit Catégoriel + Mosaïque Multiconfessionnelle (2:1 équirectangulaire) :\n  Valeurs RGB = Systèmes sacrés (Islam #10B981, Catholicisme #EC4899, Orthodoxie #8B5CF6, Culte Gréco-Romain #EA580C, Hindouisme #F59E0B, Bouddhisme #EAB308, Judaïsme #2563EB)");
            case 3 -> I18n.getOrDefault("scenario.tensor.3.format", "RGB 24-bit Catégoriel (2:1 équirectangulaire) :\n  Valeurs RGB = Souveraineté politique étatique & 22 domaines claniques régionaux (Seshat ClioPatria / CShapes)");
            case 4 -> I18n.getOrDefault("scenario.tensor.4.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Noir (0) = Prélèvement / Faible capture exergétique | Blanc (255) = Métallurgie & outillage avancé (ArchaeoGLOBE)");
            case 5 -> I18n.getOrDefault("scenario.tensor.5.format", "RGB 24-bit Réseau Multi-Modal & Emporia Hiérarchisés (2:1 équirectangulaire) :\n  Cyan = Fleuves navigables (#00D2E6) | Or = Routes de la Soie (#FFC832) | Terracotta = Voies Impériales (#FFAA28) | Bleu = Maritime (#1EB4D2) | Disques blancs = Emporia");
            case 6 -> I18n.getOrDefault("scenario.tensor.6.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Valeurs = Portée administrative SESHAT (Rome/Han 160, 2026 ≈ 220, headroom [226-255]) avec extension infrastructurelle");
            case 7 -> I18n.getOrDefault("scenario.tensor.7.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Noir (0) = Biome intact | Blanc (255) = Dégradation malthusienne & empreinte agricole (HYDE 3.4)");
            case 8 -> I18n.getOrDefault("scenario.tensor.8.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Valeurs = Potentiel de transmission vectorielle R₀, foyers zoonotiques et promiscuité urbaine (Mordecai / CDC)");
            default -> I18n.getOrDefault("scenario.hint.cultural_format", "PNG / GeoJSON en projection équirectangulaire 2:1 (RGB Catégoriel ou Niveaux de Gris 8-bit)");
        };
    }

    public String pickCulturalSourceForEpoch(int tensorIdx, long startYear, PlanetPreset planet) {
        String pName = planet != null ? planet.name().toLowerCase() : "";
        if (pName.contains("mars") || pName.contains("ares")) {
            return switch (tensorIdx) {
                case 0 -> "🔴 Mars — Cartographie Linguistique Coloniale Martienne [Planétaire (Mars), +2060 AD → Futur]";
                case 1 -> "🔴 Mars — Démographie & Lignages de Première Vague [Planétaire (Mars), +2050 AD → Futur]";
                case 2 -> "🔴 Mars — Asabiyyah & Éthique Martienne de Survie [Planétaire (Mars), +2060 AD → Futur]";
                case 3 -> "🔴 Mars — Conseil Spatial & Chartes Constitutionnelles [Planétaire (Mars), +2060 AD → Futur]";
                case 4 -> "🔴 Mars — Fonderies In-Situ (ISRU) & FabLabs Métalliques [Planétaire (Mars), +2050 AD → Futur]";
                case 5 -> "🔴 Mars — Réseau de Maglevs des Tubes de Lave [Planétaire (Mars), +2060 AD → Futur]";
                case 6 -> "🔴 Mars — Administration des Dômes & Quotas d'Oxygène [Planétaire (Mars), +2050 AD → Futur]";
                case 7 -> "🔴 Mars — Bioregenerative Life Support (BLSS) & Dégradation [Planétaire (Mars), +2050 AD → Futur]";
                case 8 -> "🔴 Mars — Microbiome Artificiel Confiné & Résistance [Planétaire (Mars), +2050 AD → Futur]";
                default -> null;
            };
        }
        if (pName.contains("vénus") || pName.contains("venus") || pName.contains("hesperos")) {
            return switch (tensorIdx) {
                case 0 -> "🟡 Vénus — Jargons Stratosphériques des Stations Flottantes [Planétaire (Vénus), +2110 AD → Futur]";
                case 1 -> "🟡 Vénus — Castes Techniques & Syndicats Flottants [Planétaire (Vénus), +2100 AD → Futur]";
                case 2 -> "🟡 Vénus — Culte Acidophile & Idéologie Flottante [Planétaire (Vénus), +2120 AD → Futur]";
                case 3 -> "🟡 Vénus — Syndicats Flottants & Corporations Aérostats [Planétaire (Vénus), +2110 AD → Futur]";
                case 4 -> "🟡 Vénus — Raffineries d'Acide & Métallurgie Aérostatique [Planétaire (Vénus), +2100 AD → Futur]";
                case 5 -> "🟡 Vénus — Lignes Aériennes & Corridors Thermiques [Planétaire (Vénus), +2110 AD → Futur]";
                case 6 -> "🟡 Vénus — Droit Aérostatique & Sécurité de Pression [Planétaire (Vénus), +2100 AD → Futur]";
                case 7 -> "🟡 Vénus — Érosion Chimique & Recyclage Fermé [Planétaire (Vénus), +2100 AD → Futur]";
                case 8 -> "🟡 Vénus — Immunologie en Atmosphère Confinée [Planétaire (Vénus), +2110 AD → Futur]";
                default -> null;
            };
        }
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene")) {
            return switch (tensorIdx) {
                case 0 -> "🌕 Lune — Dialectes Sélénites des Stations Cratériques [Planétaire (Lune), +2050 AD → Futur]";
                case 1 -> "🌕 Lune — Associations d'Équipages & Clans Sélénites [Planétaire (Lune), +2045 AD → Futur]";
                case 2 -> "🌕 Lune — Philosophie Cosmique & Rituels du Clair de Terre [Planétaire (Lune), +2050 AD → Futur]";
                case 3 -> "🌕 Lune — Secteurs Traité de l'Espace & Bases Nationales [Planétaire (Lune), +2050 AD → Futur]";
                case 4 -> "🌕 Lune — Fonderies Régolithes & Extraction Sélénite [Planétaire (Lune), +2045 AD → Futur]";
                case 5 -> "🌕 Lune — Tunnels de Transport Magnétique Sélénite [Planétaire (Lune), +2050 AD → Futur]";
                case 6 -> "🌕 Lune — Protocoles Légaux des Habitats Sélénites [Planétaire (Lune), +2050 AD → Futur]";
                case 7 -> "🌕 Lune — Épuisement des Volatils & Poussière Régolithe [Planétaire (Lune), +2045 AD → Futur]";
                case 8 -> "🌕 Lune — Pathogènes d'Isolement & Régime Stérile [Planétaire (Lune), +2045 AD → Futur]";
                default -> null;
            };
        }
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes")) {
            return switch (tensorIdx) {
                case 0 -> "🌑 Mercure — Protocoles Herméens & Terminologie d'Ombre [Planétaire (Mercure), +2160 AD → Futur]";
                case 1 -> "🌑 Mercure — Confréries de Maintenance & Lignages Thermiques [Planétaire (Mercure), +2150 AD → Futur]";
                case 2 -> "🌑 Mercure — Ordres d'Énergie & Croyances de Haute Radiation [Planétaire (Mercure), +2170 AD → Futur]";
                case 3 -> "🌑 Mercure — Domaines Miniers & Enclaves Polaires [Planétaire (Mercure), +2150 AD → Futur]";
                case 4 -> "🌑 Mercure — Collecteurs Haute Énergie & Fours Directs [Planétaire (Mercure), +2150 AD → Futur]";
                case 5 -> "🌑 Mercure — Réseau de Convois Électromagnétiques [Planétaire (Mercure), +2160 AD → Futur]";
                case 6 -> "🌑 Mercure — Administration Thermique & Urgences [Planétaire (Mercure), +2160 AD → Futur]";
                case 7 -> "🌑 Mercure — Usure Thermique & Contraintes Matérielles [Planétaire (Mercure), +2150 AD → Futur]";
                case 8 -> "🌑 Mercure — Filtrage Radiatif & Microbiote Synthétique [Planétaire (Mercure), +2160 AD → Futur]";
                default -> null;
            };
        }
        // Earth based on epoch
        if (startYear < -10000) {
            return switch (tensorIdx) {
                case 0 -> "🌍 Terre — Automated Phonological Distance Model (ASJP) [Global, -10 000 BC → Actuel (Macro-Familles)]";
                case 1 -> "🌍 Terre — Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC → Actuel (186 Cultures)]";
                case 2 -> "🌍 Terre — Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC → +2000 AD]";
                case 3 -> "🌍 Terre — GADM Administrative Sovereign Centers [Global, 1950 AD → Actuel]";
                case 4 -> "🌍 Terre — Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC → +2000 AD]";
                case 5 -> "🌍 Terre — Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC → +1800 AD]";
                case 6 -> "🌍 Terre — Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC → +1800 AD]";
                case 7 -> "🌍 Terre — Malthusian Carrying Capacity Model [Global, -100 000 BC → +2100 AD]";
                case 8 -> "🌍 Terre — Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC → +2100 AD]";
                default -> null;
            };
        }
        return switch (tensorIdx) {
            case 0 -> "🌍 Terre — Glottolog 4.8 / WALS Language Families [Global, -10 000 BC → Actuel (8 500+ Langues)]";
            case 1 -> "🌍 Terre — Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC → Actuel (1 267 Sociétés)]";
            case 2 -> "🌍 Terre — Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC → +1900 AD]";
            case 3 -> "🌍 Terre — Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / Amériques, -1000 BC → +2000 AD]";
            case 4 -> "🌍 Terre — ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC → +1850 AD]";
            case 5 -> "🌍 Terre — ORBIS Stanford Geospatial Network (Trade Routes) [Bassin Méditerranéen & Proche-Orient, -300 BC → +500 AD]";
            case 6 -> "🌍 Terre — Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC → +1900 AD]";
            case 7 -> "🌍 Terre — HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC → +2023 AD]";
            case 8 -> "🌍 Terre — GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC → +2023 AD]";
            default -> null;
        };
    }

    public ComboBox<String> buildCulturalSourceCombo(int tensorIdx) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().add("");
        switch (tensorIdx) {
            case 0 -> combo.getItems().addAll(
                    "🌍 Terre — Glottolog 4.8 / WALS Language Families [Global, -10 000 BC → Actuel (8 500+ Langues)]",
                    "🌍 Terre — Ethnologue World Linguistic Tree [Global, -3000 BC → Actuel (7 100+ Langues)]",
                    "🌍 Terre — Automated Phonological Distance Model (ASJP) [Global, -10 000 BC → Actuel (Macro-Familles)]",
                    "🔴 Mars — Cartographie Linguistique Coloniale Martienne [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Jargons Stratosphériques des Stations Flottantes [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Dialectes Sélénites des Stations Cratériques [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Protocoles Herméens & Terminologie d'Ombre [Planétaire (Mercure), +2160 AD → Futur]"
            );
            case 1 -> combo.getItems().addAll(
                    "🌍 Terre — Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC → Actuel (1 267 Sociétés)]",
                    "🌍 Terre — Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC → Actuel (186 Cultures)]",
                    "🌍 Terre — Todd Family Systems Historical Typology [Eurasie & Afrique, -1000 BC → +2000 AD]",
                    "🔴 Mars — Démographie & Lignages de Première Vague [Planétaire (Mars), +2050 AD → Futur]",
                    "🟡 Vénus — Castes Techniques & Syndicats Flottants [Planétaire (Vénus), +2100 AD → Futur]",
                    "🌕 Lune — Associations d'Équipages & Clans Sélénites [Planétaire (Lune), +2045 AD → Futur]",
                    "🌑 Mercure — Confréries de Maintenance & Lignages Thermiques [Planétaire (Mercure), +2150 AD → Futur]"
            );
            case 2 -> combo.getItems().addAll(
                    "🌍 Terre — Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC → +1900 AD]",
                    "🌍 Terre — World Religion Database (WRD) Historical Faiths [Global, -2000 BC → +2020 AD]",
                    "🌍 Terre — Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC → +2000 AD]",
                    "🔴 Mars — Asabiyyah & Éthique Martienne de Survie [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Culte Acidophile & Idéologie Flottante [Planétaire (Vénus), +2120 AD → Futur]",
                    "🌕 Lune — Philosophie Cosmique & Rituels du Clair de Terre [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Ordres d'Énergie & Croyances de Haute Radiation [Planétaire (Mercure), +2170 AD → Futur]"
            );
            case 3 -> combo.getItems().addAll(
                    "🌍 Terre — Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / Amériques, -1000 BC → +2000 AD]",
                    "🌍 Terre — CShapes 2.0 Historical State Boundaries [Global, 1886 AD → 2019 AD]",
                    "🌍 Terre — GADM Administrative Sovereign Centers [Global, 1950 AD → Actuel]",
                    "🔴 Mars — Conseil Spatial & Chartes Constitutionnelles [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Syndicats Flottants & Corporations Aérostats [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Secteurs Traité de l'Espace & Bases Nationales [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Domaines Miniers & Enclaves Polaires [Planétaire (Mercure), +2150 AD → Futur]"
            );
            case 4 -> combo.getItems().addAll(
                    "🌍 Terre — ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC → +1850 AD]",
                    "🌍 Terre — Neolithic & Bronze Age Lithic/Metallurgy Databases [Eurasie, -8000 BC → -500 BC]",
                    "🌍 Terre — Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC → +2000 AD]",
                    "🔴 Mars — Fonderies In-Situ (ISRU) & FabLabs Métalliques [Planétaire (Mars), +2050 AD → Futur]",
                    "🟡 Vénus — Raffineries d'Acide & Métallurgie Aérostatique [Planétaire (Vénus), +2100 AD → Futur]",
                    "🌕 Lune — Fonderies Régolithes & Extraction Sélénite [Planétaire (Lune), +2045 AD → Futur]",
                    "🌑 Mercure — Collecteurs Haute Énergie & Fours Directs [Planétaire (Mercure), +2150 AD → Futur]"
            );
            case 5 -> combo.getItems().addAll(
                    "🌍 Terre — ORBIS Stanford Geospatial Network (Trade Routes) [Bassin Méditerranéen & Proche-Orient, -300 BC → +500 AD]",
                    "🌍 Terre — Maritime Silk Road & Ancient Navigation Hubs [Océan Indien & Asie, -200 BC → +1500 AD]",
                    "🌍 Terre — Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC → +1800 AD]",
                    "🔴 Mars — Réseau de Maglevs des Tubes de Lave [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Lignes Aériennes & Corridors Thermiques [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Tunnels de Transport Magnétique Sélénite [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Réseau de Convois Électromagnétiques [Planétaire (Mercure), +2160 AD → Futur]"
            );
            case 6 -> combo.getItems().addAll(
                    "🌍 Terre — Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC → +1900 AD]",
                    "🌍 Terre — Cross-National Time-Series Data (CNTS Bureaucracy) [Global, 1815 AD → 2022 AD]",
                    "🌍 Terre — Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC → +1800 AD]",
                    "🔴 Mars — Conseil Spatial & Chartes Constitutionnelles [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Syndicats Flottants & Corporations Aérostats [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Protocoles Légaux des Habitats Sélénites [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Administration Thermique & Urgences [Planétaire (Mercure), +2160 AD → Futur]"
            );
            case 7 -> combo.getItems().addAll(
                    "🌍 Terre — HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC → +2023 AD]",
                    "🌍 Terre — Anthromes 2.0 Global Anthropogenic Biomes [Global, -8000 BC → +2000 AD]",
                    "🌍 Terre — Malthusian Carrying Capacity Model [Global, -100 000 BC → +2100 AD]",
                    "🔴 Mars — Bioregenerative Life Support (BLSS) & Dégradation [Planétaire (Mars), +2050 AD → Futur]",
                    "🟡 Vénus — Érosion Chimique & Recyclage Fermé [Planétaire (Vénus), +2100 AD → Futur]",
                    "🌕 Lune — Épuisement des Volatils & Poussière Régolithe [Planétaire (Lune), +2045 AD → Futur]",
                    "🌑 Mercure — Usure Thermique & Contraintes Matérielles [Planétaire (Mercure), +2150 AD → Futur]"
            );
            case 8 -> combo.getItems().addAll(
                    "🌍 Terre — GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC → +2023 AD]",
                    "🌍 Terre — Global Infectious Disease Vector Database [Zones Tropicales & Tempérées, -1000 BC → +2020 AD]",
                    "🌍 Terre — Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC → +2100 AD]",
                    "🔴 Mars — Microbiome Artificiel Confiné & Résistance [Planétaire (Mars), +2050 AD → Futur]",
                    "🟡 Vénus — Immunologie en Atmosphère Confinée [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Pathogènes d'Isolement & Régime Stérile [Planétaire (Lune), +2045 AD → Futur]",
                    "🌑 Mercure — Filtrage Radiatif & Microbiote Synthétique [Planétaire (Mercure), +2160 AD → Futur]"
            );
            default -> combo.getItems().addAll(
                    "🌍 Terre — Seshat / Global Databank Substrate [Global, -5000 BC → +2000 AD]",
                    "🌍 Terre — Historical Empirical Baseline [Global, -100 000 BP → Actuel]",
                    "🔴 Mars — Modèle Cartographique Martien Dérivé [Planétaire (Mars), +2050 AD → Futur]"
            );
        }
        DataSourceUIHelper.setupDetailedSourceCombo(combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
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

    public static class TensorParamDescriptor {
        public final String labelKey;
        public final String defaultLabel;
        public final String tooltipKey;
        public final String defaultTooltip;
        public final double min;
        public final double max;
        public final double defVal;
        public final double step;
        public final String format;

        public TensorParamDescriptor(String labelKey, String defaultLabel, String tooltipKey, String defaultTooltip,
                                     double min, double max, double defVal, double step, String format) {
            this.labelKey = labelKey;
            this.defaultLabel = defaultLabel;
            this.tooltipKey = tooltipKey;
            this.defaultTooltip = defaultTooltip;
            this.min = min;
            this.max = max;
            this.defVal = defVal;
            this.step = step;
            this.format = format;
        }
    }

    public TensorParamDescriptor getTensorParam1Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p1.label", "Dispersion dialectale (α) :", "scenario.tensor.0.p1.tooltip", "Échelle spatiale de diffusion des variantes phonétiques et lexicales.", 0.01, 0.25, 0.05, 0.01, "%.2f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p1.label", "Rayon clanique :", "scenario.tensor.1.p1.tooltip", "Rayon spatial d'influence et de solidarité des lignages et clans.", 10.0, 500.0, 120.0, 10.0, "%.0f km");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p1.label", "Force Asabiyyah :", "scenario.tensor.2.p1.tooltip", "Niveau de cohésion sociale et de solidarité sacrée (Asabiyyah d'Ibn Khaldoun).", 0.10, 1.00, 0.70, 0.05, "%.2f");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p1.label", "Portée des capitales :", "scenario.tensor.3.p1.tooltip", "Rayon d'action direct de l'autorité politico-militaire centrale.", 50.0, 2500.0, 600.0, 50.0, "%.0f km");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p1.label", "Foyers d'innovation :", "scenario.tensor.4.p1.tooltip", "Nombre de centres artisanaux et métallurgiques initiaux.", 1.0, 20.0, 5.0, 1.0, "%.0f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p1.label", "Comptoirs & carrefours :", "scenario.tensor.5.p1.tooltip", "Nombre de carrefours marchands et comptoirs d'échange.", 2.0, 30.0, 8.0, 1.0, "%.0f");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p1.label", "Niveaux bureaucratiques :", "scenario.tensor.6.p1.tooltip", "Profondeur de la hiérarchie administrative (Seshat Databank).", 1.0, 8.0, 3.0, 1.0, "%.0f niv.");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p1.label", "Surexploitation des sols :", "scenario.tensor.7.p1.tooltip", "Intensité du forçage anthropique et de la déforestation.", 0.0, 1.00, 0.40, 0.05, "%.2f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p1.label", "Pression pathogène :", "scenario.tensor.8.p1.tooltip", "Pression endémique virale et bactérienne régionale.", 0.0, 1.00, 0.35, 0.05, "%.2f");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p1.label", "Fréquence spatiale :", "scenario.tensor.ext.p1.tooltip", "Échelle d'ondulation du substrat procédural.", 0.001, 0.05, 0.01, 0.001, "%.3f");
        };
    }

    public TensorParamDescriptor getTensorParam2Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p2.label", "Foyers linguistiques :", "scenario.tensor.0.p2.tooltip", "Nombre de foyers indépendants et familles linguistiques initiales.", 1.0, 16.0, 4.0, 1.0, "%.0f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p2.label", "Perméabilité exogamique :", "scenario.tensor.1.p2.tooltip", "Taux d'alliances matrimoniales inter-clans et exogamie.", 0.0, 1.00, 0.35, 0.05, "%.2f");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p2.label", "Sanctuaires sacrés :", "scenario.tensor.2.p2.tooltip", "Nombre de hauts lieux rituels et sanctuaires émergents.", 1.0, 25.0, 6.0, 1.0, "%.0f");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p2.label", "Centralisation régalienne :", "scenario.tensor.3.p2.tooltip", "Degré de concentration du pouvoir politique et fiscal.", 0.0, 1.00, 0.75, 0.05, "%.2f");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p2.label", "Technicité matérielle :", "scenario.tensor.4.p2.tooltip", "Niveau initial de complexité des artéfacts et outillages.", 0.0, 10.0, 2.5, 0.5, "%.1f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p2.label", "Portée des routes :", "scenario.tensor.5.p2.tooltip", "Longueur maximale des routes commerciales et caravanes.", 100.0, 4000.0, 1200.0, 100.0, "%.0f km");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p2.label", "Codification juridique :", "scenario.tensor.6.p2.tooltip", "Niveau de formalisation et de codification des lois écrites.", 0.0, 1.00, 0.45, 0.05, "%.2f");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p2.label", "Taux d'épuisement :", "scenario.tensor.7.p2.tooltip", "Vitesse d'érosion des sols et d'amenuisement des ressources.", 0.001, 0.10, 0.02, 0.005, "%.3f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p2.label", "Réservoirs zoonotiques :", "scenario.tensor.8.p2.tooltip", "Rayon d'influence des foyers sauvages et réservoirs animaux.", 20.0, 600.0, 180.0, 20.0, "%.0f km");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p2.label", "Amplitude du signal :", "scenario.tensor.ext.p2.tooltip", "Intensité relative du tenseur extensible.", 0.10, 1.00, 0.80, 0.05, "%.2f");
        };
    }

    public TensorParamDescriptor getTensorParam3Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p3.label", "Barrière de relief :", "scenario.tensor.0.p3.tooltip", "Impact du relief et des chaînes de montagnes sur l'isolation linguistique.", 0.0, 1.00, 0.60, 0.05, "%.2f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p3.label", "Hiérarchie lignagère :", "scenario.tensor.1.p3.tooltip", "Degré de structuration et de segmentation patriarcale/matriarcale.", 0.0, 1.00, 0.50, 0.05, "%.2f");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p3.label", "Diffusion théologique :", "scenario.tensor.2.p3.tooltip", "Portée spatiale d'expansion des doctrines et rites sacrés.", 50.0, 2000.0, 400.0, 50.0, "%.0f km");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p3.label", "Friction frontalière :", "scenario.tensor.3.p3.tooltip", "Tension militaire et friction aux marches de l'empire.", 0.0, 1.00, 0.40, 0.05, "%.2f");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p3.label", "Diffusion technique :", "scenario.tensor.4.p3.tooltip", "Conductance de propagation des savoirs artisanaux.", 0.01, 0.30, 0.08, 0.01, "%.2f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p3.label", "Pondération maritime :", "scenario.tensor.5.p3.tooltip", "Attractivité des voies navigables, côtières et maritimes.", 0.0, 1.00, 0.65, 0.05, "%.2f");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p3.label", "Seuil d'intégration :", "scenario.tensor.6.p3.tooltip", "Taille de population déclenchant l'émergence des institutions.", 100.0, 10000.0, 1500.0, 100.0, "%.0f hab.");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p3.label", "Résilience biocapacité :", "scenario.tensor.7.p3.tooltip", "Capacité de régénération naturelle du biome.", 0.10, 2.00, 1.00, 0.10, "%.2f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p3.label", "Mémoire immunitaire :", "scenario.tensor.8.p3.tooltip", "Vitesse d'acquisition et persistance de l'immunité de groupe.", 0.01, 0.25, 0.06, 0.01, "%.2f");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p3.label", "Taux de diffusion :", "scenario.tensor.ext.p3.tooltip", "Conductance de diffusion spatiale.", 0.01, 0.20, 0.05, 0.01, "%.2f");
        };
    }

    public String getDefaultTensorSeed(int tensorIdx) {
        return String.valueOf(11235L + tensorIdx * 11111L);
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
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        double p1 = tensorParam1Sliders.containsKey(tensorIdx) ? tensorParam1Sliders.get(tensorIdx).getValue() : 1.0;
        double p2 = tensorParam2Sliders.containsKey(tensorIdx) ? tensorParam2Sliders.get(tensorIdx).getValue() : 1.0;
        double p3 = tensorParam3Sliders.containsKey(tensorIdx) ? tensorParam3Sliders.get(tensorIdx).getValue() : 1.0;
        long seed = 11235L + tensorIdx * 11111L;
        if (tensorSeedFields.containsKey(tensorIdx)) {
            try { seed = Long.parseLong(tensorSeedFields.get(tensorIdx).getText().trim()); } catch (Exception ignored) {}
        }
        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y / (double) h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x / (double) w) * 360.0;
                Color c = evaluateProceduralTensorColor(tensorIdx, lat, lon, 500.0, p1, p2, p3, seed);
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    public Color evaluateProceduralTensorColor(int tIndex, double lat, double lon, double elevation, double p1, double p2, double p3, long seed) {
        double noiseX = (lon + 180.0) / 360.0;
        double noiseY = (lat + 90.0) / 180.0;
        double pseudoNoise = Math.sin(noiseX * 12.0 + seed % 100) * Math.cos(noiseY * 12.0 + (seed / 100) % 100);
        double pseudoNoise2 = Math.sin(noiseX * 24.0 + (seed / 10) % 100) * Math.cos(noiseY * 24.0 + seed % 50);
        double combinedNoise = Math.clamp(0.5 + 0.35 * pseudoNoise + 0.15 * pseudoNoise2, 0.0, 1.0);

        return switch (tIndex % 9) {
            case 0 -> {
                double reliefFactor = Math.clamp(elevation / 3000.0, 0.0, 1.0) * p3;
                double val = Math.clamp((noiseY * 0.7 + noiseX * 0.3 + combinedNoise * 0.3 * p1 + reliefFactor * 0.2) * (p2 / 4.0), 0.0, 1.0);
                yield Color.hsb(val * 300.0, 0.75, 0.90);
            }
            case 1 -> {
                double val = Math.clamp(Math.abs(Math.sin((lat + p1 * 0.01) * 0.1) * Math.cos((lon + p2 * 10.0) * 0.1) + combinedNoise * p3 * 0.3), 0.0, 1.0);
                yield Color.hsb(180.0 + val * 120.0, 0.80, 0.85);
            }
            case 2 -> {
                double altNorm = Math.clamp(elevation / 3000.0, 0.0, 1.0);
                double val = Math.clamp(altNorm * 0.5 + combinedNoise * 0.5 * p1, 0.0, 1.0);
                yield Color.hsb(40.0 + val * 200.0, 0.85, 0.95);
            }
            case 3 -> {
                double val = Math.clamp(combinedNoise * p2, 0.0, 1.0);
                yield Color.hsb((val * 360.0 + seed % 360) % 360.0, 0.75, 0.85);
            }
            case 4 -> {
                double val = Math.clamp(combinedNoise * (p2 / 5.0) + p1 * 0.05, 0.0, 1.0);
                yield Color.gray(val);
            }
            case 5 -> {
                double val = Math.clamp(combinedNoise * p3 + (p1 / 30.0), 0.0, 1.0);
                yield Color.hsb(35.0 + val * 30.0, 0.85, 0.95);
            }
            case 6 -> {
                double val = Math.clamp(combinedNoise * p2 + (p1 / 8.0) * 0.5, 0.0, 1.0);
                yield Color.gray(val);
            }
            case 7 -> {
                double val = Math.clamp(combinedNoise * p1 + p2 * 5.0, 0.0, 1.0);
                yield Color.gray(val);
            }
            case 8 -> {
                double val = Math.clamp(combinedNoise * p1 + p3 * 2.0, 0.0, 1.0);
                yield Color.gray(val);
            }
            default -> {
                double val = Math.clamp(combinedNoise, 0.0, 1.0);
                yield Color.gray(val);
            }
        };
    }

    private HBox createTensorParamRow(TensorParamDescriptor desc, Slider slider, Label valueLabel, Runnable onChange) {
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
            long scYear = context.getStartYear();
            String autoSrc = pickCulturalSourceForEpoch(tensorIdx, scYear, context.getActivePlanetPreset());
            DataSourceUIHelper.selectBestSource(sourceCombo, autoSrc, "Terre");
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
                TensorParamDescriptor d1 = getTensorParam1Descriptor(tIdx);
                TensorParamDescriptor d2 = getTensorParam2Descriptor(tIdx);
                TensorParamDescriptor d3 = getTensorParam3Descriptor(tIdx);
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
            TensorParamDescriptor d1 = getTensorParam1Descriptor(i);
            TensorParamDescriptor d2 = getTensorParam2Descriptor(i);
            TensorParamDescriptor d3 = getTensorParam3Descriptor(i);
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
