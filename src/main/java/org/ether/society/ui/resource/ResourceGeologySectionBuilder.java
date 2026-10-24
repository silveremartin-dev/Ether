/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.ResourceDepositMapReader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowUtils;
import org.ether.society.ui.export.ResourceDistributionRasterExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.function.Consumer;

/**
 * Autonomous UI Controller and Section Builder for Geological Tensors, Mineral Endowments & Deep Earth Energy.
 *
 * <p>Extracted from {@code ResourceDistributionPanel} to satisfy the Single Responsibility Principle (SRP).
 * Encapsulates the 10 canonical planetary geological tensors:
 * <ol>
 *   <li><b>Coal</b>: Sedimentary paralic basin thickness, vitrinite reflectance (%Ro), and paleomire accumulation.</li>
 *   <li><b>Crude Oil</b>: Source rock Total Organic Carbon (%TOC), thermal oil window (60°C–175°C), and anticlinal trap density.</li>
 *   <li><b>Natural Gas</b>: Deep thermogenic cracking vs. biogenic gas, cratonic reservoir pressures, and caprock evaporite seals.</li>
 *   <li><b>Uranium</b>: Hydrothermal vein pechblende precipitation, Proterozoic unconformity redox fronts, and \( \text{Th}^{232}/\text{U}^{238} \) ratio.</li>
 *   <li><b>Helium-3</b>: Lunar/planetary regolith solar-wind implantation, ilmenite (\( \text{FeTiO}_3 \)) capture capacity, and atmospheric shielding factor.</li>
 *   <li><b>Iron & Copper</b>: Banded Iron Formations (BIF) and magmatic porphyry intrusions.</li>
 *   <li><b>Precious Metals</b>: Epithermal vein gold/silver (\( \text{Au, Ag, Pt} \)) and alluvial placer deposits.</li>
 *   <li><b>Rare Earth Elements (REE) & Critical Minerals</b>: Carbonatite complexes, pegmatites, and lithium salars.</li>
 *   <li><b>Mantle Heat Flow</b>: Radiogenic heat, tectonic lithospheric plumes, and geothermal gradients (\( \text{mW/m}^2 \)).</li>
 *   <li><b>Freshwater Aquifers</b>: Hydrogeological piezometric head, hydraulic conductivity \( K \), and interstitial rock porosity.</li>
 * </ol>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ResourceGeologySectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResourceGeologySectionBuilder.class);

    public static class TensorSliderMeta extends ResourceGeologyDepositCatalog.TensorSliderMeta {
        public TensorSliderMeta(String labelKey, String defaultLabel, String tooltipKey, String defaultTooltip,
                                double min, double max, double defaultValue, String unit, String formatPattern) {
            super(labelKey, defaultLabel, tooltipKey, defaultTooltip, min, max, defaultValue, unit, formatPattern);
        }
    }

    public static final ResourceGeologyDepositCatalog.TensorSliderMeta[][] GEOLOGY_SLIDER_SPECS = ResourceGeologyDepositCatalog.GEOLOGY_SLIDER_SPECS;

    /**
     * Interface providing host context properties and callbacks to {@code ResourceDistributionPanel}.
     */
    public interface HostContext {
        void notifyParamChange();
        void updatePreviewCanvas();
        int getGeologyViewComboIndex(int layerIdx);
        void selectViewMode(int viewModeIndex);
        PlanetPreset getActivePlanetPreset();
        Window getWindow();
        boolean isUpdatingFromPreset();
        long getEpochYear();
    }

    private final HostContext host;

    // Geological Tensors UI Models & Caches
    private final Map<Integer, Image> customGeologyLayerImages = new HashMap<>();
    private final Map<Integer, RadioButton> geologyProcRadios = new HashMap<>();
    private final Map<Integer, RadioButton> geologyImportRadios = new HashMap<>();
    private final Map<Integer, Label> geologySubTitles = new HashMap<>();
    private final Map<Integer, Label> geologyFileLabels = new HashMap<>();
    private final Map<Integer, Label> geologySourceLabels = new HashMap<>();
    private final Map<Integer, ComboBox<String>> geologySourceCombos = new HashMap<>();
    private final Map<Integer, Button> geologyLoadBtns = new HashMap<>();
    private final Map<Integer, Label> geologyFormatLabels = new HashMap<>();
    private final Map<Integer, Label> geologySeedLabels = new HashMap<>();
    private final Map<Integer, Button> geologyGenBtns = new HashMap<>();
    private final Map<Integer, List<Label>> geologySliderTitleLabels = new HashMap<>();

    // Section Headers & Global Controls
    private Label geologyDomainSecHeader;
    private Label sec41Header;
    private Label sec42Header;
    private Label geoSeedLabel;
    private VBox geologyLayersDynamicContainer;
    private TextField geologySeedInput;
    private Button btnGenerateProceduralGeologyTensors;
    private Button geologyHelpBtn;
    private Button btnExportGisMultiFormat;
    private Button btnExportProvenanceManifest;

    // Per-tensor Geology Seed & Physical Sliders (Tensors 0 to 9)
    private final Map<Integer, TextField> geologyTensorSeeds = new HashMap<>();
    private final Map<Integer, Slider> geologyAbundanceSliders = new HashMap<>();
    private final Map<Integer, Slider> geologyThresholdSliders = new HashMap<>();
    private final Map<Integer, Slider> geologyParam3Sliders = new HashMap<>();
    private final Map<Integer, Slider> geologyParam4Sliders = new HashMap<>();

    public ResourceGeologySectionBuilder(HostContext host) {
        this.host = Objects.requireNonNull(host, "host context must not be null");
    }

    public VBox buildGeologySection() {
        geologyDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.geology", "4. DOMAINE GÉOLOGIE, TECTONIQUE & MINERAIS"));
        geologyDomainSecHeader.getStyleClass().add("label-section-header");

        sec41Header = new Label(I18n.getOrDefault("resource.section.geology_4_1.title", "⚙️ 4.1 Contrôle Global, Graine & Formats"));
        sec41Header.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        btnGenerateProceduralGeologyTensors = new Button(I18n.getOrDefault("resource.btn.regen_tensors", "🪄 Régénérer tous les tenseurs géologiques"));
        btnGenerateProceduralGeologyTensors.getStyleClass().add("button");
        btnGenerateProceduralGeologyTensors.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnGenerateProceduralGeologyTensors.setMinWidth(Region.USE_PREF_SIZE);
        btnGenerateProceduralGeologyTensors.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.regen_geology_tensors",
                "Switch all geological tensors to procedural mode and regenerate maps according to parameters and stochastic seed.")));
        btnGenerateProceduralGeologyTensors.setOnAction(e -> generateProceduralGeologyTensors());

        geologySeedInput = new TextField("45678");
        geologySeedInput.setPrefWidth(80);
        geologySeedInput.setStyle("-fx-font-size: 11px;");
        geologySeedInput.textProperty().addListener((obs, oldV, newV) -> {
            if (!host.isUpdatingFromPreset()) {
                host.notifyParamChange();
            }
        });

        Button geoRandSeedBtn = new Button("🎲");
        geoRandSeedBtn.getStyleClass().add("button-secondary");
        geoRandSeedBtn.setStyle("-fx-font-size: 11px;");
        geoRandSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.random_geology_seed", "Draw a new random stochastic seed for all geological tensors.")));
        geoRandSeedBtn.setOnAction(e -> {
            String s = String.valueOf(new Random().nextLong(1000000));
            geologySeedInput.setText(s);
            generateProceduralGeologyTensors();
        });

        geoSeedLabel = new Label(I18n.getOrDefault("resource.label.geology_seed", "Graine :"));
        HBox geoSeedBox = new HBox(4, geoSeedLabel, new Label("🎲"), geologySeedInput, geoRandSeedBtn);
        geoSeedBox.setAlignment(Pos.CENTER_LEFT);

        HBox seedRow = new HBox(8, geoSeedBox, btnGenerateProceduralGeologyTensors);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnGenerateProceduralGeologyTensors, Priority.ALWAYS);

        geologyHelpBtn = new Button(I18n.getOrDefault("resource.btn.geology_format_help", "❓ Format Calques"));
        geologyHelpBtn.getStyleClass().add("button-secondary");
        geologyHelpBtn.setStyle("-fx-font-size: 11px;");
        geologyHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        geologyHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.geology_specs", "Spécifications des formats d'image et standards géologiques (PNG, GeoTIFF, NetCDF, ASC, GeoJSON).")));
        geologyHelpBtn.setOnAction(e -> showGeologyImportFormatHelp());

        btnExportGisMultiFormat = new Button(I18n.getOrDefault("resource.btn.export_gis", "🗺️ Export SIG Multi-Format"));
        btnExportGisMultiFormat.getStyleClass().add("button-secondary");
        btnExportGisMultiFormat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportGisMultiFormat.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_gis", "Exporter les 10 tenseurs géologiques sous formats SIG professionnels (GeoTIFF, NetCDF-4, GeoJSON).")));
        btnExportGisMultiFormat.setOnAction(e -> exportGisMultiFormat());

        btnExportProvenanceManifest = new Button(I18n.getOrDefault("resource.btn.provenance_manifest", "🔒 Manifeste Provenance SHA-256"));
        btnExportProvenanceManifest.getStyleClass().add("button-secondary");
        btnExportProvenanceManifest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.gen_provenance", "Générer un manifeste cryptographique JSON (provenance.json) contenant les hashs SHA-256 des jeux de données géologiques.")));
        btnExportProvenanceManifest.setOnAction(e -> exportProvenanceManifest());

        HBox formatRow = new HBox(8, geologyHelpBtn, btnExportGisMultiFormat, btnExportProvenanceManifest);
        formatRow.setAlignment(Pos.CENTER_LEFT);

        VBox sec41Box = new VBox(6, sec41Header, seedRow, formatRow);
        sec41Box.setStyle("-fx-padding: 8px 10px; -fx-background-color: rgba(148, 163, 184, 0.08); -fx-background-radius: 6px; -fx-border-color: rgba(148, 163, 184, 0.25); -fx-border-radius: 6px; -fx-border-width: 1px;");

        sec42Header = new Label(I18n.getOrDefault("resource.section.geology_4_2.title", "🗺️ 4.2 TENSEURS GÉOLOGIQUES & ÉNERGÉTIQUES CANONIQUES (10 Sous-Blocs)"));
        sec42Header.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        sec42Header.setWrapText(true);

        geologyLayersDynamicContainer = new VBox(10);
        rebuildGeologyTensorSubBlocks();
        prepopulateEarthGeologyTensors();

        VBox content = new VBox(10, sec41Box, sec42Header, geologyLayersDynamicContainer);
        VBox card = new VBox(8, geologyDomainSecHeader, content);
        card.getStyleClass().add("card-section");
        return card;
    }

    public void rebuildGeologyTensorSubBlocks() {
        if (geologyLayersDynamicContainer == null) return;
        geologyLayersDynamicContainer.getChildren().clear();

        for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
            final int layerIdx = i;

            VBox subBlock = new VBox(6);
            subBlock.setStyle("-fx-padding: 10 10 10 10; -fx-background-color: rgba(56,189,248,0.04); -fx-background-radius: 6; -fx-border-color: rgba(56,189,248,0.15); -fx-border-radius: 6;");

            Label subTitle = new Label(getGeologyTensorTitle(i));
            subTitle.getStyleClass().add("control-label");
            subTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            subTitle.setWrapText(true);
            Tooltip.install(subTitle, new Tooltip(getGeologyTensorTooltip(i)));
            geologySubTitles.put(layerIdx, subTitle);

            ToggleGroup tg = new ToggleGroup();
            RadioButton radioProc = new RadioButton(I18n.getOrDefault("resource.mode.procedural", "▶ Procedural Generation (Hotspots & Physics)"));
            RadioButton radioImport = new RadioButton(I18n.getOrDefault("resource.mode.import_file", "📂 External Source (PNG / GeoTIFF)"));
            radioProc.setToggleGroup(tg);
            radioImport.setToggleGroup(tg);
            radioProc.getStyleClass().add("radio-proc");
            radioImport.getStyleClass().add("radio-import");

            boolean hasImage = customGeologyLayerImages.containsKey(layerIdx) && customGeologyLayerImages.get(layerIdx) != null;
            if (hasImage) {
                radioImport.setSelected(true);
            } else {
                radioProc.setSelected(true);
            }

            geologyProcRadios.put(layerIdx, radioProc);
            geologyImportRadios.put(layerIdx, radioImport);

            TextField seedTF = new TextField(String.valueOf(12345 + layerIdx * 777));
            seedTF.setPrefWidth(90);
            seedTF.setStyle("-fx-font-size: 11px;");
            seedTF.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            });
            geologyTensorSeeds.put(layerIdx, seedTF);

            Button randSeedBtn = new Button("🎲");
            randSeedBtn.getStyleClass().add("button-secondary");
            randSeedBtn.setStyle("-fx-font-size: 11px;");
            randSeedBtn.setOnAction(e -> {
                seedTF.setText(String.valueOf(new Random().nextLong(1000000)));
                if (radioProc != null) radioProc.setSelected(true);
                host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            });

            Button btnExportTensor = new Button(I18n.getOrDefault("resource.btn.export_single_tensor", "📤 Exporter"));
            btnExportTensor.getStyleClass().add("button-secondary");
            btnExportTensor.setStyle("-fx-font-size: 11px;");
            btnExportTensor.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_tensor", "Exporter ce tenseur géologique sous forme d'image raster haute résolution (PNG / JPEG).")));
            btnExportTensor.setOnAction(e -> exportGeologyTensor(layerIdx));
            geologyGenBtns.put(layerIdx, btnExportTensor);

            Label seedLbl = new Label(I18n.getOrDefault("resource.label.tensor_seed", "Generation Seed:"));
            geologySeedLabels.put(layerIdx, seedLbl);
            HBox seedBox = new HBox(6, seedLbl, seedTF, randSeedBtn, btnExportTensor);
            seedBox.setAlignment(Pos.CENTER_LEFT);

            VBox slidersContainer = new VBox(4);
            List<Label> sliderTitles = new ArrayList<>();

            for (int s = 0; s < 4; s++) {
                ResourceGeologyDepositCatalog.TensorSliderMeta meta = GEOLOGY_SLIDER_SPECS[i][s];

                String labelText = I18n.getOrDefault(meta.labelKey, meta.defaultLabel);
                String tooltipText = I18n.getOrDefault(meta.tooltipKey, meta.defaultTooltip);

                Label titleLabel = new Label(labelText);
                titleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
                sliderTitles.add(titleLabel);

                Label valLabel = new Label(String.format(meta.formatPattern, meta.defaultValue));
                valLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #3b82f6; -fx-font-weight: bold;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox sliderHeader = new HBox(6, titleLabel, spacer, valLabel);

                Slider slider = new Slider(meta.min, meta.max, meta.defaultValue);
                slider.setShowTickMarks(false);
                slider.setShowTickLabels(false);

                Tooltip tt = new Tooltip(tooltipText);
                tt.setWrapText(true);
                tt.setMaxWidth(300);
                Tooltip.install(titleLabel, tt);
                Tooltip.install(valLabel, tt);
                Tooltip.install(slider, tt);

                slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    valLabel.setText(String.format(meta.formatPattern, newVal.doubleValue()));
                    if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                    host.updatePreviewCanvas();
                });

                if (s == 0) geologyAbundanceSliders.put(layerIdx, slider);
                else if (s == 1) geologyThresholdSliders.put(layerIdx, slider);
                else if (s == 2) geologyParam3Sliders.put(layerIdx, slider);
                else if (s == 3) geologyParam4Sliders.put(layerIdx, slider);

                VBox sBox = new VBox(1, sliderHeader, slider);
                slidersContainer.getChildren().add(sBox);
            }
            geologySliderTitleLabels.put(layerIdx, sliderTitles);

            VBox proceduralBox = new VBox(6, seedBox, slidersContainer);
            proceduralBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(56,189,248,0.2); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            proceduralBox.setVisible(!hasImage);
            proceduralBox.setManaged(!hasImage);

            Label sourceLbl = new Label(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            geologySourceLabels.put(layerIdx, sourceLbl);
            ComboBox<String> sourceCombo = buildGeologySourceCombo(layerIdx);
            geologySourceCombos.put(layerIdx, sourceCombo);

            Label fileLbl = new Label("—");
            fileLbl.setStyle("-fx-font-style: italic; -fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            fileLbl.setWrapText(true);
            geologyFileLabels.put(layerIdx, fileLbl);

            Button loadBtn = new Button(I18n.getOrDefault("resource.btn.import_map_action", "📁 Importer fichier..."));
            loadBtn.getStyleClass().add("button-secondary");
            loadBtn.setStyle("-fx-font-size: 11px;");
            loadBtn.setOnAction(e -> loadCustomGeologyLayer(layerIdx, img -> {
                customGeologyLayerImages.put(layerIdx, img);
                fileLbl.setText(I18n.getOrDefault("resource.status.custom_layer_active", "Fichier personnalisé chargé"));
                radioImport.setSelected(true);
                host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            }));
            geologyLoadBtns.put(layerIdx, loadBtn);

            Label formatHintLbl = new Label(getGeologyFormatHint(layerIdx));
            formatHintLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
            geologyFormatLabels.put(layerIdx, formatHintLbl);

            HBox fileRow = new HBox(8, loadBtn, fileLbl);
            fileRow.setAlignment(Pos.CENTER_LEFT);

            VBox importBox = new VBox(6, new HBox(6, sourceLbl, sourceCombo), fileRow, formatHintLbl);
            importBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(245,158,11,0.2); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            importBox.setVisible(hasImage);
            importBox.setManaged(hasImage);

            radioProc.setOnAction(e -> {
                proceduralBox.setVisible(true);
                proceduralBox.setManaged(true);
                importBox.setVisible(false);
                importBox.setManaged(false);
                host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            });

            radioImport.setOnAction(e -> {
                proceduralBox.setVisible(false);
                proceduralBox.setManaged(false);
                importBox.setVisible(true);
                importBox.setManaged(true);
                host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            });

            HBox radioBox = new HBox(12, radioProc, radioImport);
            radioBox.setAlignment(Pos.CENTER_LEFT);

            subBlock.getChildren().addAll(subTitle, radioBox, proceduralBox, importBox);
            geologyLayersDynamicContainer.getChildren().add(subBlock);
        }
    }

    public void generateProceduralGeologyTensors() {
        if (geologyProcRadios != null) {
            for (var entry : geologyProcRadios.entrySet()) {
                if (entry.getValue() != null) entry.getValue().setSelected(true);
            }
        }
        customGeologyLayerImages.clear();
        if (geologyFileLabels != null) {
            for (Integer idx : geologyFileLabels.keySet()) {
                if (geologyFileLabels.get(idx) != null) geologyFileLabels.get(idx).setText("—");
            }
        }
        if (geologySeedInput != null && !geologySeedInput.getText().isBlank()) {
            try {
                long baseSeed = Long.parseLong(geologySeedInput.getText().trim());
                if (geologyTensorSeeds != null) {
                    for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
                        TextField tf = geologyTensorSeeds.get(i);
                        if (tf != null) tf.setText(String.valueOf(baseSeed + i * 777L));
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        host.selectViewMode(3);
        if (!host.isUpdatingFromPreset()) host.notifyParamChange();
        host.updatePreviewCanvas();
    }

    public void prepopulateGeologyTensors(String planetKey, long year) {
        if (planetKey == null || planetKey.isBlank() || "none".equalsIgnoreCase(planetKey)) {
            planetKey = "earth";
        }
        String body = planetKey.toLowerCase();
        try {
            String[] layerNames = {
                    "coal", "oil", "gas", "uranium", "helium3",
                    "iron_copper", "precious_metals", "rare_earths", "geothermal", "aquifers"
            };

            for (int i = 0; i < layerNames.length; i++) {
                Image img = null;
                if ("earth".equals(body)) {
                    img = ImageMapLoader.loadMapImage("earth", year, layerNames[i]);
                }
                if (img == null || img.getWidth() <= 0) {
                    String fileName = body + "_" + layerNames[i] + ".png";
                    img = ImageMapLoader.loadMapImage(fileName);
                }
                if (img != null && img.getWidth() > 0) {
                    customGeologyLayerImages.put(i, img);
                } else {
                    BufferedImage bImg = switch (i) {
                        case 0 -> HistoricalMapGenerator.rasterizeCoalMap(body.toUpperCase(), null);
                        case 1 -> HistoricalMapGenerator.rasterizeOilMap(body.toUpperCase(), null);
                        case 2 -> HistoricalMapGenerator.rasterizeGasMap(body.toUpperCase(), null);
                        case 3 -> HistoricalMapGenerator.rasterizeUraniumMap(body.toUpperCase(), null);
                        case 4 -> HistoricalMapGenerator.rasterizeHelium3Map(body.toUpperCase(), null);
                        case 5 -> HistoricalMapGenerator.rasterizeIronCopperMap(body.toUpperCase(), null);
                        case 6 -> HistoricalMapGenerator.rasterizePreciousMetalsMap(body.toUpperCase(), null);
                        case 7 -> HistoricalMapGenerator.rasterizeRareEarthsMap(body.toUpperCase(), null);
                        case 8 -> HistoricalMapGenerator.rasterizeMantleHeatMap(body.toUpperCase(), null);
                        default -> HistoricalMapGenerator.rasterizeAquiferMap(body.toUpperCase(), null);
                    };
                    if (bImg != null) {
                        customGeologyLayerImages.put(i, ResourceGeologyProceduralRasterizer.bufferedImageToFXImage(bImg));
                    }
                }
            }

            for (int i = 0; i < 10; i++) {
                if (geologyFileLabels.containsKey(i)) {
                    geologyFileLabels.get(i).setText(getGeologyBaselineName(i, body));
                }
                if (geologyImportRadios.containsKey(i)) {
                    geologyImportRadios.get(i).setSelected(true);
                }
                if (geologySourceCombos.containsKey(i)) {
                    ComboBox<String> cb = geologySourceCombos.get(i);
                    if (cb != null) {
                        boolean matched = false;
                        for (String item : cb.getItems()) {
                            if (item == null || item.isEmpty()) continue;
                            String itmLow = item.toLowerCase();
                            boolean matches = false;
                            if (body.equals("earth") && (itmLow.contains("earth") || itmLow.contains("terre") || itmLow.contains("terran"))) {
                                matches = true;
                            } else if (body.equals("mars") && itmLow.contains("mars")) {
                                matches = true;
                            } else if (body.equals("venus") && (itmLow.contains("venus") || itmLow.contains("vénus"))) {
                                matches = true;
                            } else if (body.equals("moon") && (itmLow.contains("moon") || itmLow.contains("lune") || itmLow.contains("lunar") || itmLow.contains("lola"))) {
                                matches = true;
                            } else if (body.equals("mercury") && (itmLow.contains("mercury") || itmLow.contains("mercure") || itmLow.contains("messenger"))) {
                                matches = true;
                            } else if (itmLow.contains(body)) {
                                matches = true;
                            }
                            if (matches) {
                                cb.setValue(item);
                                matched = true;
                                break;
                            }
                        }
                        if (!matched && cb.getItems().size() > 1) {
                            cb.setValue(cb.getItems().get(1));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to pre-populate geology tensors for {}", body, e);
        }
    }

    public void prepopulateEarthGeologyTensors() {
        prepopulateGeologyTensors("earth", host.getEpochYear());
    }

    public double sampleProceduralGeologyTensor(int layerIdx, double lon, double lat, PlanetPreset planet) {
        return ResourceGeologyProceduralRasterizer.sampleProceduralGeologyTensor(
                layerIdx, lon, lat, planet,
                geologyAbundanceSliders, geologyThresholdSliders,
                geologyParam3Sliders, geologyParam4Sliders,
                geologyTensorSeeds
        );
    }

    public Color getGeologyResourceColor(int layerIdx, double val) {
        return ResourceGeologyProceduralRasterizer.getGeologyResourceColor(layerIdx, val);
    }

    public Image generateProceduralGeologyRasterImage(int layerIdx, int width, int height) {
        PlanetPreset planet = host.getActivePlanetPreset();
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        return ResourceGeologyProceduralRasterizer.generateProceduralGeologyRasterImage(
                layerIdx, width, height, planet,
                geologyAbundanceSliders, geologyThresholdSliders,
                geologyParam3Sliders, geologyParam4Sliders,
                geologyTensorSeeds
        );
    }

    public void exportGeologyTensor(int layerIdx) {
        boolean isImport = geologyImportRadios.containsKey(layerIdx) && geologyImportRadios.get(layerIdx).isSelected();
        Image img = (isImport && customGeologyLayerImages.containsKey(layerIdx) && customGeologyLayerImages.get(layerIdx) != null)
                ? customGeologyLayerImages.get(layerIdx)
                : generateProceduralGeologyRasterImage(layerIdx, 1024, 512);
        String defaultName = "geology_tensor_" + (layerIdx + 1) + ".png";
        WindowUtils.exportImageWithChooser(host.getWindow(),
                img, defaultName, I18n.getOrDefault("resource.dialog.export_geology_tensor_title", "Export Geology Tensor Map (PNG / JPEG)"));
    }

    private void loadCustomGeologyLayer(int layerIdx, Consumer<Image> onLoaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.dialog.import_geology_title", "Import geological map ") + getGeologyTensorTitle(layerIdx));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Données Spatiales & Tenseurs (PNG, ASC, GeoJSON, TIF, JPG)", "*.png", "*.asc", "*.geojson", "*.json", "*.tif", "*.tiff", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(host.getWindow());
        if (file != null) {
            try {
                java.awt.Color fallbackCol = ResourceDistributionRasterExporter.getGeologyTensorAwtColor(layerIdx);
                BufferedImage bImg = ResourceDepositMapReader.readGeologicalDataset(file, 720, 360, fallbackCol);
                if (bImg != null) {
                    onLoaded.accept(ResourceGeologyProceduralRasterizer.bufferedImageToFXImage(bImg));
                } else {
                    Image img = new Image(new FileInputStream(file));
                    onLoaded.accept(img);
                }
            } catch (Exception ex) {
                logger.error("Failed to load geology layer {}", layerIdx, ex);
            }
        }
    }

    private void showGeologyImportFormatHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.getOrDefault("resource.title.geology_help", "Geological Layer Specifications & Formats"));
        alert.setHeaderText(I18n.getOrDefault("resource.header.geology_help", "🗺️ SPECIFICATIONS FOR 10 GEOLOGICAL TENSORS (GIS & RASTERS)"));

        String content = I18n.getOrDefault("resource.help.geology_specs_body",
                "📍 STANDARDIZED PROJECTION & RESOLUTION:\n" +
                        "• Projection: Standard WGS84 Equirectangular (2:1, Longitude [-180°, +180°], Latitude [-90°, +90°]).\n" +
                        "• Supported formats: PNG (8/16-bit), GeoTIFF (.tif/.tiff), ESRI Arc/Info ASCII (.asc), GeoJSON (.geojson), NetCDF-4 (.nc.json).\n" +
                        "• Recommended resolution: 720×360 (fast), 1440×720 (standard HD), or 3600×1800 (ultra-precise).\n\n" +
                        "🛢️ 10 CANONICAL GEOLOGICAL LAYERS:\n" +
                        "1. Coal Basins (USGS World Coal Quality / BGR Energy Study)\n" +
                        "2. Oil Reservoirs (USGS DDS-69 World Petroleum Assessment)\n" +
                        "3. Natural Gas Fields (IEA Global Gas Tracker / Rystad Energy)\n" +
                        "4. Uranium Deposits (IAEA UDEPO / Red Book Resources)\n" +
                        "5. Helium-3 Regolith (NASA LRO LAMP / Apollo Lunar Samples)\n" +
                        "6. Iron & Copper Porphyry (USGS MRDS Mineral Resources)\n" +
                        "7. Precious Metals Au/Ag/Pt (GFMS Gold Survey / USGS)\n" +
                        "8. Rare Earth Elements REE & Lithium (BGS Critical Minerals)\n" +
                        "9. Mantle Heat Flow & Geothermal (IHFC Global Heat Flow Data)\n" +
                        "10. Groundwater & Deep Aquifers (UNESCO WHYMAP / GRACE-FO)\n\n" +
                        "⚙️ CALIBRATION & VALUES:\n" +
                        "• Pixel grayscale (0 to 255) indicates resource abundance [0.0 to 1.0].\n" +
                        "• The export automatically bundles ESRI World Files (.tfw) for instant GIS integration."
        );

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(640);
        textArea.setPrefHeight(420);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px;");

        alert.getDialogPane().setContent(textArea);
        if (host.getWindow() != null) alert.initOwner(host.getWindow());
        alert.showAndWait();
    }

    private void exportGisMultiFormat() {
        ResourceDistributionRasterExporter.exportGisMultiFormat(
                host.getWindow(),
                host.getActivePlanetPreset()
        );
    }

    private void exportProvenanceManifest() {
        ResourceDistributionRasterExporter.exportProvenanceManifest(
                host.getWindow(),
                geologySeedInput != null ? geologySeedInput.getText() : "45678"
        );
    }

    public String getGeologyTensorTitle(int index) {
        return ResourceGeologyDepositCatalog.getGeologyTensorTitle(index);
    }

    public String getGeologyTensorTooltip(int index) {
        return ResourceGeologyDepositCatalog.getGeologyTensorTooltip(index);
    }

    public String getGeologyBaselineName(int index, String planetKey) {
        return ResourceGeologyDepositCatalog.getGeologyBaselineName(index, planetKey);
    }

    public String getGeologyFormatHint(int index) {
        return ResourceGeologyDepositCatalog.getGeologyFormatHint(index);
    }

    private ComboBox<String> buildGeologySourceCombo(int index) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getStyleClass().add("combo-box");
        combo.setStyle("-fx-font-size: 11px;");

        combo.getItems().add(I18n.getOrDefault("resource.source.procedural", "▶ Procédural pur (Formules analytiques)"));

        switch (index) {
            case 0 -> {
                combo.getItems().add("🌍 Terre — USGS World Coal Quality Inventory & BGR Energy");
                combo.getItems().add("🌍 Terre — Paléo-Carbonifère Parallique (-300 Ma)");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 1 -> {
                combo.getItems().add("🌍 Terre — USGS DDS-69 World Petroleum Assessment");
                combo.getItems().add("🌍 Terre — Rystad Energy UCube Offshore & Onshore");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 2 -> {
                combo.getItems().add("🌍 Terre — IEA Global Gas Tracker & Deep Shales");
                combo.getItems().add("🌍 Terre — Bassins Cratoniques Conventionnels");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 3 -> {
                combo.getItems().add("🌍 Terre — IAEA UDEPO World Uranium Deposit Database");
                combo.getItems().add("🔴 Mars — MRO CRISM Uranium-Thorium Spectrometry Map");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 4 -> {
                combo.getItems().add("🌕 Lune — Chang'e / LRO LAMP Ilmenite He-3 Distribution");
                combo.getItems().add("🔴 Mars — MAVEN Solar Wind Regolith Implantation");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 5 -> {
                combo.getItems().add("🌍 Terre — USGS MRDS Mineral Resources Data System");
                combo.getItems().add("🔴 Mars — MGS TES Hematite & Iron Crust Global Map");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 6 -> {
                combo.getItems().add("🌍 Terre — World Gold Council & USGS Precious Metals");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 7 -> {
                combo.getItems().add("🌍 Terre — BGS Critical Minerals & Lithium Salars Survey");
                combo.getItems().add("🌕 Lune — KREEP Terrane Rare Earth & Thorium Procellarum");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            case 8 -> {
                combo.getItems().add("🌍 Terre — IHFC Global Heat Flow Commission Map");
                combo.getItems().add("🔴 Mars — InSight SEIS Lithospheric Thermal Gradient");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
            default -> {
                combo.getItems().add("🌍 Terre — UNESCO WHYMAP World Hydrogeological Map & GRACE-FO");
                combo.getItems().add("🔴 Mars — MARSIS ESA Subglacial Deep Aquifer Map");
                combo.getItems().add("🪐 Personnalisé — Fichier Externe Spécifié");
            }
        }

        combo.setValue(combo.getItems().size() > 1 ? combo.getItems().get(1) : combo.getItems().get(0));

        combo.setOnAction(e -> {
            String val = combo.getValue();
            if (val != null) {
                if (val.contains("USGS") || val.contains("BGR") || val.contains("IEA") || val.contains("IAEA") || val.contains("BGS") || val.contains("IHFC") || val.contains("UNESCO") || val.contains("Terre") || val.contains("Mars") || val.contains("Lune")) {
                    if (geologyImportRadios.containsKey(index)) geologyImportRadios.get(index).setSelected(true);
                } else if (val.contains("Procédural")) {
                    if (geologyProcRadios.containsKey(index)) geologyProcRadios.get(index).setSelected(true);
                }
                host.selectViewMode(host.getGeologyViewComboIndex(index));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            }
        });
        return combo;
    }

    public void updateTexts() {
        if (geologyDomainSecHeader != null) {
            geologyDomainSecHeader.setText(I18n.getOrDefault("resource.domain.geology", "4. DOMAINE GÉOLOGIE, TECTONIQUE & MINERAIS"));
        }
        if (sec41Header != null) {
            sec41Header.setText(I18n.getOrDefault("resource.section.geology_4_1.title", "⚙️ 4.1 Contrôle Global, Graine & Formats"));
        }
        if (sec42Header != null) {
            sec42Header.setText(I18n.getOrDefault("resource.section.geology_4_2.title", "🗺️ 4.2 TENSEURS GÉOLOGIQUES & ÉNERGÉTIQUES CANONIQUES (10 Sous-Blocs)"));
        }
        if (btnGenerateProceduralGeologyTensors != null) {
            btnGenerateProceduralGeologyTensors.setText(I18n.getOrDefault("resource.btn.regen_tensors", "🪄 Régénérer tous les tenseurs géologiques"));
            btnGenerateProceduralGeologyTensors.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.regen_geology_tensors", "Switch all geological tensors to procedural mode and regenerate maps.")));
        }
        if (geoSeedLabel != null) {
            geoSeedLabel.setText(I18n.getOrDefault("resource.label.geology_seed", "Graine :"));
        }
        if (geologyHelpBtn != null) {
            geologyHelpBtn.setText(I18n.getOrDefault("resource.btn.geology_format_help", "❓ Format Calques"));
        }
        if (btnExportGisMultiFormat != null) {
            btnExportGisMultiFormat.setText(I18n.getOrDefault("resource.btn.export_gis", "🗺️ Export SIG Multi-Format"));
        }
        if (btnExportProvenanceManifest != null) {
            btnExportProvenanceManifest.setText(I18n.getOrDefault("resource.btn.provenance_manifest", "🔒 Manifeste Provenance SHA-256"));
        }

        for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
            if (geologySubTitles.containsKey(i) && geologySubTitles.get(i) != null) {
                geologySubTitles.get(i).setText(getGeologyTensorTitle(i));
                Tooltip.install(geologySubTitles.get(i), new Tooltip(getGeologyTensorTooltip(i)));
            }
            if (geologyProcRadios.containsKey(i) && geologyProcRadios.get(i) != null) {
                geologyProcRadios.get(i).setText(I18n.getOrDefault("resource.mode.procedural", "▶ Procedural Generation (Hotspots & Physics)"));
            }
            if (geologyImportRadios.containsKey(i) && geologyImportRadios.get(i) != null) {
                geologyImportRadios.get(i).setText(I18n.getOrDefault("resource.mode.import_file", "📂 External Source (PNG / GeoTIFF)"));
            }
            if (geologySeedLabels.containsKey(i) && geologySeedLabels.get(i) != null) {
                geologySeedLabels.get(i).setText(I18n.getOrDefault("resource.label.tensor_seed", "Generation Seed:"));
            }
            if (geologyGenBtns.containsKey(i) && geologyGenBtns.get(i) != null) {
                geologyGenBtns.get(i).setText(I18n.getOrDefault("resource.btn.export_single_tensor", "📤 Exporter"));
            }
            if (geologySourceLabels.containsKey(i) && geologySourceLabels.get(i) != null) {
                geologySourceLabels.get(i).setText(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            }
            if (geologyLoadBtns.containsKey(i) && geologyLoadBtns.get(i) != null) {
                geologyLoadBtns.get(i).setText(I18n.getOrDefault("resource.btn.import_map_action", "📁 Importer fichier..."));
            }
            if (geologyFormatLabels.containsKey(i) && geologyFormatLabels.get(i) != null) {
                geologyFormatLabels.get(i).setText(getGeologyFormatHint(i));
            }
            if (geologySliderTitleLabels.containsKey(i)) {
                List<Label> titles = geologySliderTitleLabels.get(i);
                for (int s = 0; s < titles.size() && s < 4; s++) {
                    ResourceGeologyDepositCatalog.TensorSliderMeta meta = GEOLOGY_SLIDER_SPECS[i][s];
                    titles.get(s).setText(I18n.getOrDefault(meta.labelKey, meta.defaultLabel));
                    Tooltip.install(titles.get(s), new Tooltip(I18n.getOrDefault(meta.tooltipKey, meta.defaultTooltip)));
                }
            }
        }
    }

    public Map<Integer, Image> getCustomGeologyLayerImages() {
        return customGeologyLayerImages;
    }

    public boolean isGeologyImportActive(int layerIdx) {
        return geologyImportRadios.containsKey(layerIdx) && geologyImportRadios.get(layerIdx).isSelected();
    }

    public boolean isAnyGeologyImportActive() {
        for (int i = 0; i < 10; i++) {
            if (isGeologyImportActive(i)) return true;
        }
        return false;
    }

    public Image getGeologyLayerImage(int layerIdx) {
        return customGeologyLayerImages.get(layerIdx);
    }
}
