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
import javafx.stage.Window;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowHelper;
import org.ether.society.ui.export.ResourceDistributionRasterExporter;

import java.util.*;

/**
 * Autonomous UI Controller and Section Builder for Geological Tensors, Mineral Endowments &amp; Deep Earth Energy.
 *
 * <p>Encapsulates the 10 canonical planetary geological tensors (Coal, Crude Oil, Natural Gas,
 * Uranium, Helium-3, Iron &amp; Copper, Precious Metals, Rare Earth Elements, Mantle Heat Flow,
 * and Freshwater Aquifers).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ResourceGeologySectionBuilder {

    public static class TensorSliderMeta extends ResourceGeologyDepositRegistry.TensorSliderMeta {
        public TensorSliderMeta(String labelKey, String defaultLabel, String tooltipKey, String defaultTooltip,
                                double min, double max, double defaultValue, String unit, String formatPattern) {
            super(labelKey, defaultLabel, tooltipKey, defaultTooltip, min, max, defaultValue, unit, formatPattern);
        }
    }

    public static final ResourceGeologyDepositRegistry.TensorSliderMeta[][] GEOLOGY_SLIDER_SPECS = ResourceGeologyDepositRegistry.GEOLOGY_SLIDER_SPECS;

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

    final HostContext host;

    final Map<Integer, Image> customGeologyLayerImages = new HashMap<>();
    final Map<Integer, RadioButton> geologyProcRadios = new HashMap<>();
    final Map<Integer, RadioButton> geologyImportRadios = new HashMap<>();
    final Map<Integer, Label> geologySubTitles = new HashMap<>();
    final Map<Integer, Label> geologyFileLabels = new HashMap<>();
    final Map<Integer, Label> geologySourceLabels = new HashMap<>();
    final Map<Integer, ComboBox<String>> geologySourceCombos = new HashMap<>();
    final Map<Integer, Button> geologyLoadBtns = new HashMap<>();
    final Map<Integer, Label> geologyFormatLabels = new HashMap<>();
    final Map<Integer, Label> geologySeedLabels = new HashMap<>();
    final Map<Integer, Button> geologyGenBtns = new HashMap<>();
    final Map<Integer, List<Label>> geologySliderTitleLabels = new HashMap<>();

    Label geologyDomainSecHeader;
    Label sec41Header;
    Label sec42Header;
    Label geoSeedLabel;
    private VBox geologyLayersDynamicContainer;
    private TextField geologySeedInput;
    Button btnGenerateProceduralGeologyTensors;
    Button geologyHelpBtn;
    Button btnExportGisMultiFormat;
    Button btnExportProvenanceManifest;

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

        sec41Header = new Label(I18n.getOrDefault("resource.section.geology_4_1.title", "âš™ï¸ 4.1 Contrôle Global, Graine & Formats"));
        sec41Header.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        btnGenerateProceduralGeologyTensors = new Button(I18n.getOrDefault("resource.btn.regen_tensors", " Régénérer tous les tenseurs géologiques"));
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
            if (!host.isUpdatingFromPreset()) host.notifyParamChange();
        });

        Button geoRandSeedBtn = new Button("");
        geoRandSeedBtn.getStyleClass().add("button-secondary");
        geoRandSeedBtn.setStyle("-fx-font-size: 11px;");
        geoRandSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.random_geology_seed", "Draw a new random stochastic seed for all geological tensors.")));
        geoRandSeedBtn.setOnAction(e -> {
            geologySeedInput.setText(String.valueOf(new Random().nextLong(1000000)));
            generateProceduralGeologyTensors();
        });

        geoSeedLabel = new Label(I18n.getOrDefault("resource.label.geology_seed", "Graine :"));
        HBox geoSeedBox = new HBox(4, geoSeedLabel, new Label(""), geologySeedInput, geoRandSeedBtn);
        geoSeedBox.setAlignment(Pos.CENTER_LEFT);

        HBox seedRow = new HBox(8, geoSeedBox, btnGenerateProceduralGeologyTensors);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnGenerateProceduralGeologyTensors, Priority.ALWAYS);

        geologyHelpBtn = new Button(I18n.getOrDefault("resource.btn.geology_format_help", "â“ Format Calques"));
        geologyHelpBtn.getStyleClass().add("button-secondary");
        geologyHelpBtn.setStyle("-fx-font-size: 11px;");
        geologyHelpBtn.setMinWidth(Region.USE_PREF_SIZE);
        geologyHelpBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.geology_specs", "Spécifications des formats d'image et standards géologiques (PNG, GeoTIFF, NetCDF, ASC, GeoJSON).")));
        geologyHelpBtn.setOnAction(e -> ResourceGeologyLayerImportHelper.showGeologyImportFormatHelp(this));

        btnExportGisMultiFormat = new Button(I18n.getOrDefault("resource.btn.export_gis", "¸ Export SIG Multi-Format"));
        btnExportGisMultiFormat.getStyleClass().add("button-secondary");
        btnExportGisMultiFormat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportGisMultiFormat.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_gis", "Exporter les 10 tenseurs géologiques sous formats SIG professionnels (GeoTIFF, NetCDF-4, GeoJSON).")));
        btnExportGisMultiFormat.setOnAction(e -> ResourceDistributionRasterExporter.exportGisMultiFormat(host.getWindow(), host.getActivePlanetPreset()));

        btnExportProvenanceManifest = new Button(I18n.getOrDefault("resource.btn.provenance_manifest", " Manifeste Provenance SHA-256"));
        btnExportProvenanceManifest.getStyleClass().add("button-secondary");
        btnExportProvenanceManifest.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        btnExportProvenanceManifest.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.gen_provenance", "Générer un manifeste cryptographique JSON (provenance.json) contenant les hashs SHA-256 des jeux de données géologiques.")));
        btnExportProvenanceManifest.setOnAction(e -> ResourceDistributionRasterExporter.exportProvenanceManifest(host.getWindow(), geologySeedInput != null ? geologySeedInput.getText() : "45678"));

        HBox formatRow = new HBox(8, geologyHelpBtn, btnExportGisMultiFormat, btnExportProvenanceManifest);
        formatRow.setAlignment(Pos.CENTER_LEFT);

        VBox sec41Box = new VBox(6, sec41Header, seedRow, formatRow);
        sec41Box.setStyle("-fx-padding: 8px 10px; -fx-background-color: rgba(148, 163, 184, 0.08); -fx-background-radius: 6px; -fx-border-color: rgba(148, 163, 184, 0.25); -fx-border-radius: 6px; -fx-border-width: 1px;");

        sec42Header = new Label(I18n.getOrDefault("resource.section.geology_4_2.title", "¸ 4.2 TENSEURS GÉOLOGIQUES & ÉNERGÉTIQUES CANONIQUES (10 Sous-Blocs)"));
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
            RadioButton radioProc = new RadioButton(I18n.getOrDefault("resource.mode.procedural", "â–¶ Procedural Generation (Hotspots & Physics)"));
            RadioButton radioImport = new RadioButton(I18n.getOrDefault("resource.mode.import_file", " External Source (PNG / GeoTIFF)"));
            radioProc.setToggleGroup(tg);
            radioImport.setToggleGroup(tg);
            radioProc.getStyleClass().add("radio-proc");
            radioImport.getStyleClass().add("radio-import");

            boolean hasImage = customGeologyLayerImages.get(layerIdx) != null;
            if (hasImage) radioImport.setSelected(true);
            else radioProc.setSelected(true);

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

            Button randSeedBtn = new Button("");
            randSeedBtn.getStyleClass().add("button-secondary");
            randSeedBtn.setStyle("-fx-font-size: 11px;");
            randSeedBtn.setOnAction(e -> {
                seedTF.setText(String.valueOf(new Random().nextLong(1000000)));
                radioProc.setSelected(true);
                host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
                if (!host.isUpdatingFromPreset()) host.notifyParamChange();
                host.updatePreviewCanvas();
            });

            Button btnExportTensor = new Button(I18n.getOrDefault("resource.btn.export_single_tensor", " Exporter"));
            btnExportTensor.getStyleClass().add("button-secondary");
            btnExportTensor.setStyle("-fx-font-size: 11px;");
            btnExportTensor.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_tensor", "Exporter ce tenseur géologique sous forme d'image raster haute résolution (PNG / JPEG).")));
            btnExportTensor.setOnAction(e -> exportGeologyTensor(layerIdx));
            geologyGenBtns.put(layerIdx, btnExportTensor);

            Label seedLbl = new Label(I18n.getOrDefault("resource.label.tensor_seed", "Generation Seed:"));
            geologySeedLabels.put(layerIdx, seedLbl);
            HBox seedBox = new HBox(6, seedLbl, seedTF, randSeedBtn, btnExportTensor);
            seedBox.setAlignment(Pos.CENTER_LEFT);

            VBox slidersContainer = buildTensorSlidersBox(layerIdx);

            VBox proceduralBox = new VBox(6, seedBox, slidersContainer);
            proceduralBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(56,189,248,0.2); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            proceduralBox.setVisible(!hasImage);
            proceduralBox.setManaged(!hasImage);

            Label sourceLbl = new Label(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            geologySourceLabels.put(layerIdx, sourceLbl);
            ComboBox<String> sourceCombo = ResourceGeologyLayerImportHelper.buildGeologySourceCombo(this, layerIdx);
            geologySourceCombos.put(layerIdx, sourceCombo);

            Label fileLbl = new Label("—");
            fileLbl.setStyle("-fx-font-style: italic; -fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            fileLbl.setWrapText(true);
            geologyFileLabels.put(layerIdx, fileLbl);

            Button loadBtn = new Button(I18n.getOrDefault("resource.btn.import_map_action", "📁 Importer fichier..."));
            loadBtn.getStyleClass().add("button-secondary");
            loadBtn.setStyle("-fx-font-size: 11px;");
            loadBtn.setOnAction(e -> ResourceGeologyLayerImportHelper.loadCustomGeologyLayer(this, layerIdx, img -> {
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

            radioProc.setOnAction(e -> toggleSubBlockMode(proceduralBox, importBox, true, layerIdx));
            radioImport.setOnAction(e -> toggleSubBlockMode(proceduralBox, importBox, false, layerIdx));

            HBox radioBox = new HBox(12, radioProc, radioImport);
            radioBox.setAlignment(Pos.CENTER_LEFT);

            subBlock.getChildren().addAll(subTitle, radioBox, proceduralBox, importBox);
            geologyLayersDynamicContainer.getChildren().add(subBlock);
        }
    }

    private VBox buildTensorSlidersBox(int layerIdx) {
        VBox slidersContainer = new VBox(4);
        List<Label> sliderTitles = new ArrayList<>();
        for (int s = 0; s < 4; s++) {
            ResourceGeologyDepositRegistry.TensorSliderMeta meta = GEOLOGY_SLIDER_SPECS[layerIdx][s];
            Label titleLabel = new Label(I18n.getOrDefault(meta.labelKey, meta.defaultLabel));
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

            Tooltip tt = new Tooltip(I18n.getOrDefault(meta.tooltipKey, meta.defaultTooltip));
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
            else geologyParam4Sliders.put(layerIdx, slider);

            slidersContainer.getChildren().add(new VBox(1, sliderHeader, slider));
        }
        geologySliderTitleLabels.put(layerIdx, sliderTitles);
        return slidersContainer;
    }

    private void toggleSubBlockMode(VBox proceduralBox, VBox importBox, boolean procedural, int layerIdx) {
        proceduralBox.setVisible(procedural);
        proceduralBox.setManaged(procedural);
        importBox.setVisible(!procedural);
        importBox.setManaged(!procedural);
        host.selectViewMode(host.getGeologyViewComboIndex(layerIdx));
        if (!host.isUpdatingFromPreset()) host.notifyParamChange();
        host.updatePreviewCanvas();
    }

    public void generateProceduralGeologyTensors() {
        for (RadioButton rb : geologyProcRadios.values()) {
            if (rb != null) rb.setSelected(true);
        }
        customGeologyLayerImages.clear();
        for (Label lbl : geologyFileLabels.values()) {
            if (lbl != null) lbl.setText("—");
        }
        if (geologySeedInput != null && !geologySeedInput.getText().isBlank()) {
            try {
                long baseSeed = Long.parseLong(geologySeedInput.getText().trim());
                for (int i = 0; i < GEOLOGY_SLIDER_SPECS.length; i++) {
                    TextField tf = geologyTensorSeeds.get(i);
                    if (tf != null) tf.setText(String.valueOf(baseSeed + i * 777L));
                }
            } catch (NumberFormatException ignored) { org.slf4j.LoggerFactory.getLogger(ResourceGeologySectionBuilder.class).debug("Non-fatal condition in ResourceGeologySectionBuilder: {}", ignored.getMessage()); }
        }
        host.selectViewMode(3);
        if (!host.isUpdatingFromPreset()) host.notifyParamChange();
        host.updatePreviewCanvas();
    }

    public void prepopulateGeologyTensors(String planetKey, long year) {
        ResourceGeologyLayerImportHelper.prepopulateGeologyTensors(this, planetKey, year);
    }

    public void prepopulateEarthGeologyTensors() {
        prepopulateGeologyTensors("earth", host.getEpochYear());
    }

    public double sampleProceduralGeologyTensor(int layerIdx, double lon, double lat, PlanetPreset planet) {
        return ResourceGeologyProceduralRasterizer.sampleProceduralGeologyTensor(
                layerIdx, lon, lat, planet,
                geologyAbundanceSliders, geologyThresholdSliders,
                geologyParam3Sliders, geologyParam4Sliders, geologyTensorSeeds);
    }

    public Color getGeologyResourceColor(int layerIdx, double val) {
        return ResourceGeologyProceduralRasterizer.getGeologyResourceColor(layerIdx, val);
    }

    public Image generateProceduralGeologyRasterImage(int layerIdx, int width, int height) {
        PlanetPreset planet = host.getActivePlanetPreset() != null ? host.getActivePlanetPreset() : PlanetPreset.EARTH_LIKE;
        return ResourceGeologyProceduralRasterizer.generateProceduralGeologyRasterImage(
                layerIdx, width, height, planet,
                geologyAbundanceSliders, geologyThresholdSliders,
                geologyParam3Sliders, geologyParam4Sliders, geologyTensorSeeds);
    }

    public void exportGeologyTensor(int layerIdx) {
        boolean isImport = isGeologyImportActive(layerIdx);
        Image img = (isImport && customGeologyLayerImages.get(layerIdx) != null)
                ? customGeologyLayerImages.get(layerIdx)
                : generateProceduralGeologyRasterImage(layerIdx, 1024, 512);
        WindowHelper.exportImageWithChooser(host.getWindow(),
                img, "geology_tensor_" + (layerIdx + 1) + ".png",
                I18n.getOrDefault("resource.dialog.export_geology_tensor_title", "Export Geology Tensor Map (PNG / JPEG)"));
    }

    public String getGeologyTensorTitle(int index) { return ResourceGeologyDepositRegistry.getGeologyTensorTitle(index); }
    public String getGeologyTensorTooltip(int index) { return ResourceGeologyDepositRegistry.getGeologyTensorTooltip(index); }
    public String getGeologyBaselineName(int index, String planetKey) { return ResourceGeologyDepositRegistry.getGeologyBaselineName(index, planetKey); }
    public String getGeologyFormatHint(int index) { return ResourceGeologyDepositRegistry.getGeologyFormatHint(index); }

    public void updateTexts() {
        ResourceGeologyLayerImportHelper.updateTexts(this);
    }

    public Map<Integer, Image> getCustomGeologyLayerImages() { return customGeologyLayerImages; }
    public boolean isGeologyImportActive(int layerIdx) {
        return geologyImportRadios.containsKey(layerIdx) && geologyImportRadios.get(layerIdx).isSelected();
    }
    public boolean isAnyGeologyImportActive() {
        for (int i = 0; i < 10; i++) {
            if (isGeologyImportActive(i)) return true;
        }
        return false;
    }
    public Image getGeologyLayerImage(int layerIdx) { return customGeologyLayerImages.get(layerIdx); }
}


