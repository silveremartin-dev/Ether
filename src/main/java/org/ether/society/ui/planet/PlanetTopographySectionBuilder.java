/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.util.DataSourceUIHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Random;

/**
 * Autonomous UI Controller and Section Builder for Planetary Topography, Hypsometry,
 * Sea Levels, Procedural Perlin Octaves, and External Heightmap / DEM Ingestion.
 *
 * <p>Extracted from {@code PlanetGeneratorPanel} to satisfy the Single Responsibility Principle (SRP).
 * Synchronizes eustatic water levels to the meter and handles digital elevation model raster assets.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class PlanetTopographySectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PlanetTopographySectionBuilder.class);

    /** Callback interface defining interaction with the parent panel */
    public interface HostContext {
        void notifyParamChange();
        void updatePreview();
        boolean isUpdatingFromPreset();
        void selectViewMode(int modeIndex);
        void applyMapSourcePreset(String sourceKey);
        void exportProceduralHeightmap();
        double getRadiusKm();
        Window getWindow();
    }

    private final HostContext context;

    // Sliders & Spinners
    private Slider minAltSlider;
    private Spinner<Double> minAltSpinner;
    private Slider maxAltSlider;
    private Spinner<Double> maxAltSpinner;
    private Slider noiseFreqSlider;
    private Slider noiseScaleSlider;
    private Slider waterSlider;
    private Spinner<Double> seaLevelMetersSpinner;

    // Radios & Combos
    private RadioButton radioProc;
    private RadioButton radioImport;
    private ComboBox<String> mapSourceCombo;
    private TextField seedField;
    private Button randSeedBtn;

    // Asset reference & UI labels
    private Image customElevImage;
    private String cachedElevB64;
    private Label elevFileLabel;
    private Button loadElevBtn;
    private Button clearElevBtn;

    // Descriptive labels
    private Label topoSecHeader;
    private Label minAltRowLabel;
    private Label maxAltRowLabel;
    private Label waterRowLabel;
    private Label seaLevelMetersLabel;
    private Label waterNormValLabel;
    private Label altRangeLabel;
    private Label freqRowLabel;
    private Label scaleRowLabel;
    private Label seedRowLabel;
    private Label mapSourceRowLabel;
    private Label elevMapRowLabel;
    private Label resolutionInfoLabel;

    private boolean isUpdatingControlSync = false;

    public PlanetTopographySectionBuilder(HostContext context) {
        this.context = context;
    }

    public VBox buildSection() {
        VBox topoControls = new VBox(10);

        minAltSlider = createSlider(-15000, -500, -11000);
        maxAltSlider = createSlider(500, 25000, 8848);
        minAltSlider.valueProperty().addListener((obs, old, val) -> updateAltRangeDisplay());
        maxAltSlider.valueProperty().addListener((obs, old, val) -> updateAltRangeDisplay());
        waterSlider = createSlider(-0.5, 1.0, 0.478);
        noiseFreqSlider = createSlider(0.1, 2.0, 1.0);
        noiseScaleSlider = createSlider(0.5, 3.0, 1.0);

        minAltRowLabel = new Label();
        maxAltRowLabel = new Label();
        waterRowLabel = new Label();
        seaLevelMetersLabel = new Label(I18n.getOrDefault("planet.param.sea_level", "Niveau de la Mer (Altitude) :"));
        freqRowLabel = new Label();
        scaleRowLabel = new Label();
        seedRowLabel = new Label();

        minAltSpinner = new Spinner<>(-20000.0, -100.0, -11000.0, 10.0);
        VBox minAltBox = createControlRowWithSpinner(minAltRowLabel, minAltSlider, minAltSpinner, "%.0f m",
                I18n.getOrDefault("planet.tooltip.min_alt", "Absolute minimum altitude (ocean floor) [Corresponds to image level 0]"));

        maxAltSpinner = new Spinner<>(100.0, 30000.0, 8848.0, 10.0);
        VBox maxAltBox = createControlRowWithSpinner(maxAltRowLabel, maxAltSlider, maxAltSpinner, "%.0f m",
                I18n.getOrDefault("planet.tooltip.max_alt", "Altitude maximale absolue (sommet montagneux) [Correspond au niveau 255 de l'image]"));

        altRangeLabel = new Label();
        altRangeLabel.getStyleClass().add("value-label");
        altRangeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 2 0 4 0;");
        updateAltRangeDisplay();

        seaLevelMetersLabel.getStyleClass().add("control-label");
        seaLevelMetersSpinner = new Spinner<>(-20000.0, 30000.0, 0.0, 1.0);
        seaLevelMetersSpinner.setEditable(true);
        seaLevelMetersSpinner.setPrefWidth(100);
        seaLevelMetersSpinner.setMaxWidth(115);
        seaLevelMetersSpinner.getStyleClass().add("dense-spinner");
        seaLevelMetersSpinner.getEditor().setOnAction(e -> commitEditorDoubleText(seaLevelMetersSpinner));
        seaLevelMetersSpinner.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) commitEditorDoubleText(seaLevelMetersSpinner);
        });

        Label seaLevelUnit = new Label("m");
        seaLevelUnit.getStyleClass().add("unit-label");
        HBox seaLevelValBox = new HBox(4, seaLevelMetersSpinner, seaLevelUnit);
        seaLevelValBox.setAlignment(Pos.CENTER_RIGHT);

        HBox seaLevelHeader = new HBox(seaLevelMetersLabel, new Pane(), seaLevelValBox);
        HBox.setHgrow(seaLevelHeader.getChildren().get(1), Priority.ALWAYS);

        waterNormValLabel = new Label();
        waterNormValLabel.getStyleClass().add("card-description-muted");
        waterNormValLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        String seaLevelTooltip = I18n.getOrDefault("planet.tooltip.sea_level_meters",
                "Niveau de la mer au mètre près. Réglez l'altitude eustatique exacte en mètres (ex: 0 m actuel, -125 m LGM). La valeur est convertie selon le datum NOAA ETOPO (0.478 à 0 m).");
        Tooltip slTt = new Tooltip(seaLevelTooltip);
        seaLevelMetersLabel.setTooltip(slTt);
        seaLevelMetersSpinner.setTooltip(slTt);
        waterSlider.setTooltip(slTt);

        VBox seaLevelBox = new VBox(4, seaLevelHeader, waterSlider, waterNormValLabel);

        seaLevelMetersSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (context.isUpdatingFromPreset() || isUpdatingControlSync || newV == null) return;
            isUpdatingControlSync = true;
            try {
                double minAlt = minAltSlider.getValue();
                double maxAlt = maxAltSlider.getValue();
                double w = PlanetPreset.metersToWaterLevel(newV, minAlt, maxAlt);
                waterSlider.setValue(w);
                updateSeaLevelDisplay(newV, w);
                context.notifyParamChange();
                context.updatePreview();
            } finally {
                isUpdatingControlSync = false;
            }
        });

        waterSlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (context.isUpdatingFromPreset() || isUpdatingControlSync || newV == null) return;
            isUpdatingControlSync = true;
            try {
                double minAlt = minAltSlider.getValue();
                double maxAlt = maxAltSlider.getValue();
                double meters = PlanetPreset.waterLevelToMeters(newV.doubleValue(), minAlt, maxAlt);
                seaLevelMetersSpinner.getValueFactory().setValue(Math.round(meters * 10.0) / 10.0);
                updateSeaLevelDisplay(meters, newV.doubleValue());
                context.notifyParamChange();
                context.updatePreview();
            } finally {
                isUpdatingControlSync = false;
            }
        });

        VBox commonTopoBox = new VBox(8, minAltBox, maxAltBox, altRangeLabel, seaLevelBox);

        seedField = new TextField("12345");
        seedField.textProperty().addListener((obs, old, val) -> {
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
        });
        randSeedBtn = new Button("🎲");
        randSeedBtn.getStyleClass().add("button-secondary");
        randSeedBtn.setTooltip(new Tooltip(I18n.getOrDefault("planet.tooltip.seed_rand", "New random seed")));
        randSeedBtn.setOnAction(e -> {
            seedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
        });
        HBox seedBox = new HBox(5, seedField, randSeedBtn);
        HBox.setHgrow(seedField, Priority.ALWAYS);

        resolutionInfoLabel = new Label();
        resolutionInfoLabel.getStyleClass().add("card-description-muted");
        resolutionInfoLabel.setWrapText(true);
        updateResolutionInfoLabel(context.getRadiusKm());

        Button exportProceduralBtn = new Button(I18n.getOrDefault("planet.btn.export_procedural", "📤 Export Procedural Heightmap (PNG)"));
        exportProceduralBtn.setMaxWidth(Double.MAX_VALUE);
        exportProceduralBtn.getStyleClass().add("button-secondary");
        exportProceduralBtn.setOnAction(e -> context.exportProceduralHeightmap());

        ToggleGroup elevSourceGroup = new ToggleGroup();
        radioProc = new RadioButton(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
        radioImport = new RadioButton(I18n.getOrDefault("planet.radio.import", "📂 Import Heightmap Externe (PNG/GeoTIFF)"));
        radioProc.setToggleGroup(elevSourceGroup);
        radioImport.setToggleGroup(elevSourceGroup);
        radioProc.setSelected(true);
        radioProc.getStyleClass().add("radio-proc");
        radioImport.getStyleClass().add("radio-import");

        VBox proceduralPanel = new VBox(8,
                createControlRow(seedRowLabel, seedBox, I18n.getOrDefault("planet.tooltip.seed", "Random seed for deterministic generation")),
                createControlRow(freqRowLabel, noiseFreqSlider, "%.2f", I18n.getOrDefault("planet.tooltip.noise_freq", "Spatial frequency of altimetric noise")),
                createControlRow(scaleRowLabel, noiseScaleSlider, "%.2f", I18n.getOrDefault("planet.tooltip.noise_scale", "Elevation amplitude scale (mountains / plains)")),
                exportProceduralBtn,
                resolutionInfoLabel
        );
        proceduralPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");

        mapSourceRowLabel = new Label();
        mapSourceCombo = new ComboBox<>();
        mapSourceCombo.getItems().addAll("", "earth", "mars", "venus", "moon", "mercury");
        mapSourceCombo.setValue("earth");
        DataSourceUIHelper.setupDetailedSourceCombo(mapSourceCombo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        mapSourceCombo.setMaxWidth(Double.MAX_VALUE);
        mapSourceCombo.setOnAction(e -> {
            if (context.isUpdatingFromPreset()) return;
            String sel = mapSourceCombo.getValue();
            if (sel != null && !sel.isEmpty()) {
                context.applyMapSourcePreset(sel);
                context.notifyParamChange();
                context.updatePreview();
            }
        });

        elevMapRowLabel = new Label();
        elevFileLabel = new Label(I18n.get("planet.map.none"));
        elevFileLabel.getStyleClass().add("value-label");
        loadElevBtn = new Button(I18n.get("planet.map.btn_load"));
        loadElevBtn.getStyleClass().add("button-secondary");
        loadElevBtn.setOnAction(e -> chooseElevMapFile());
        clearElevBtn = new Button("❌");
        clearElevBtn.getStyleClass().add("button-secondary");
        clearElevBtn.setOnAction(e -> {
            customElevImage = null;
            cachedElevB64 = null;
            elevFileLabel.setText(I18n.get("planet.map.none"));
            context.notifyParamChange();
            context.updatePreview();
        });
        HBox elevBox = new HBox(5, loadElevBtn, clearElevBtn);

        VBox importPanel = new VBox(8,
                createControlRow(mapSourceRowLabel, mapSourceCombo,
                        I18n.getOrDefault("planet.tooltip.map_source", "Celestial body preset (Earth, Mars, Venus, Moon) — selects predefined elevation map")),
                createControlRow(elevMapRowLabel, new VBox(3, elevBox, elevFileLabel),
                        I18n.getOrDefault("planet.tooltip.elev_map", "Import d'une heightmap PNG en niveaux de gris (noir=min alt, blanc=max alt)"))
        );
        importPanel.setStyle("-fx-padding: 8 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-radius: 6; -fx-border-width: 0 0 0 3;");
        importPanel.setVisible(false);
        importPanel.setManaged(false);

        elevSourceGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProc;
            proceduralPanel.setVisible(isProc);
            proceduralPanel.setManaged(isProc);
            importPanel.setVisible(!isProc);
            importPanel.setManaged(!isProc);
            if (!isProc && customElevImage == null && mapSourceCombo.getValue() != null && !mapSourceCombo.getValue().isEmpty()) {
                context.applyMapSourcePreset(mapSourceCombo.getValue());
            }
            context.selectViewMode(0);
            if (!context.isUpdatingFromPreset()) {
                context.notifyParamChange();
                context.updatePreview();
            }
        });

        radioProc.setOnAction(e -> {
            context.selectViewMode(0);
            context.updatePreview();
        });
        radioImport.setOnAction(e -> {
            context.selectViewMode(0);
            context.updatePreview();
        });

        topoControls.getChildren().addAll(
                commonTopoBox,
                new Separator(),
                radioProc,
                proceduralPanel,
                radioImport,
                importPanel
        );

        topoSecHeader = new Label(I18n.getOrDefault("planet.section.topo", "2. TOPOGRAPHY & PLANETARY RELIEF DOMAIN"));
        topoSecHeader.getStyleClass().add("label-section-header");
        VBox section = new VBox(8, topoSecHeader, topoControls);
        section.getStyleClass().add("card-section");
        return section;
    }

    public void updateResolutionInfoLabel(double radiusKm) {
        if (resolutionInfoLabel == null) return;
        double circumference = 2 * Math.PI * radiusKm;
        int rec = circumference > 60000 ? 2048 : (circumference > 20000 ? 1024 : 512);
        resolutionInfoLabel.setText(String.format(
                I18n.getOrDefault("planet.hint.map_resolution",
                        "📎 Rayon %.0f km → circonférence %.0f km. Résolution recommandée : %d×%d px min."),
                radiusKm, circumference, rec, rec / 2));
    }

    public void updateAltRangeDisplay() {
        if (minAltSlider == null || maxAltSlider == null || altRangeLabel == null) return;
        double min = minAltSlider.getValue();
        double max = maxAltSlider.getValue();
        altRangeLabel.setText(String.format("%s: %,.0f m", I18n.get("planet.param.alt_range"), max - min));
        double w = waterSlider != null ? waterSlider.getValue() : 0.478;
        double m = PlanetPreset.waterLevelToMeters(w, min, max);
        if (!isUpdatingControlSync && seaLevelMetersSpinner != null) {
            isUpdatingControlSync = true;
            try {
                seaLevelMetersSpinner.getValueFactory().setValue(Math.round(m * 10.0) / 10.0);
                updateSeaLevelDisplay(m, w);
            } finally {
                isUpdatingControlSync = false;
            }
        }
    }

    public void updateSeaLevelDisplay(double meters, double normWater) {
        if (waterNormValLabel != null) {
            waterNormValLabel.setText(String.format(
                    I18n.getOrDefault("planet.param.water_norm_info", "Seuil normalisé : %.4f | Niveau marin : %,.0f m"),
                    normWater, meters
            ));
        }
    }

    private void chooseElevMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser.elevation", "Load elevation heightmap image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(context.getWindow());
        if (file != null) {
            try {
                customElevImage = new Image(new FileInputStream(file));
                cachedElevB64 = null;
                elevFileLabel.setText("📷 " + file.getName());
                context.selectViewMode(0);
                context.notifyParamChange();
                String prefix = "📌 Fichier local : ";
                mapSourceCombo.getItems().removeIf(item -> item != null && item.startsWith(prefix));
                String localEntry = prefix + file.getName();
                mapSourceCombo.getItems().add(localEntry);
                mapSourceCombo.setValue(localEntry);
                context.updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load elevation map image", ex);
            }
        }
    }

    public void applyPreset(PlanetPreset p) {
        if (p == null) return;
        seedField.setText(String.valueOf(p.seed()));
        minAltSlider.setValue(p.minAltitudeMeters());
        if (minAltSpinner != null) minAltSpinner.getValueFactory().setValue(p.minAltitudeMeters());
        maxAltSlider.setValue(p.maxAltitudeMeters());
        if (maxAltSpinner != null) maxAltSpinner.getValueFactory().setValue(p.maxAltitudeMeters());
        noiseFreqSlider.setValue(p.noiseFrequency());
        noiseScaleSlider.setValue(p.noiseScale());
        waterSlider.setValue(p.waterLevel());
        double seaM = PlanetPreset.waterLevelToMeters(p.waterLevel(), p.minAltitudeMeters(), p.maxAltitudeMeters());
        if (seaLevelMetersSpinner != null) seaLevelMetersSpinner.getValueFactory().setValue(Math.round(seaM * 10.0) / 10.0);
        updateSeaLevelDisplay(seaM, p.waterLevel());
        updateAltRangeDisplay();

        if (p.customElevBase64() != null) {
            customElevImage = org.ether.society.data.ImageMapLoader.base64PngToImage(p.customElevBase64());
            cachedElevB64 = p.customElevBase64();
            if (elevFileLabel != null) elevFileLabel.setText(I18n.getOrDefault("planet.preset.heightmap", "📷 Preset Heightmap"));
        }

        if (p.elevationUseImport() || p.customElevBase64() != null) {
            radioImport.setSelected(true);
        } else {
            radioProc.setSelected(true);
        }
        if (mapSourceCombo != null && p.elevationMapSource() != null) {
            mapSourceCombo.setValue(p.elevationMapSource());
        }
    }

    public void updateTexts() {
        if (topoSecHeader != null) topoSecHeader.setText(I18n.getOrDefault("planet.section.topo", "TOPOGRAPHY & PLANETARY RELIEF DOMAIN"));
        if (seedRowLabel != null) seedRowLabel.setText(I18n.get("planet.param.seed"));
        if (minAltRowLabel != null) minAltRowLabel.setText(I18n.get("planet.param.min_alt"));
        if (maxAltRowLabel != null) maxAltRowLabel.setText(I18n.get("planet.param.max_alt"));
        if (waterRowLabel != null) waterRowLabel.setText(I18n.get("planet.param.water_level"));
        if (seaLevelMetersLabel != null) seaLevelMetersLabel.setText(I18n.getOrDefault("planet.param.sea_level", "Niveau de la Mer (Altitude) :"));
        if (freqRowLabel != null) freqRowLabel.setText(I18n.get("planet.param.noise_freq"));
        if (scaleRowLabel != null) scaleRowLabel.setText(I18n.get("planet.param.noise_scale"));
        if (mapSourceRowLabel != null) mapSourceRowLabel.setText(I18n.get("planet.map.preset_body"));
        if (elevMapRowLabel != null) elevMapRowLabel.setText(I18n.get("planet.map.elevation"));
        if (loadElevBtn != null) loadElevBtn.setText(I18n.get("planet.map.btn_load"));
        if (radioProc != null) radioProc.setText(I18n.getOrDefault("planet.radio.procedural", "▶ Procedural Generation (Perlin Noise)"));
        if (radioImport != null) radioImport.setText(I18n.getOrDefault("planet.radio.import", "📂 Import Heightmap Externe (PNG/GeoTIFF)"));
        if (mapSourceCombo != null && mapSourceCombo.getCellFactory() != null) {
            mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
        }
        updateAltRangeDisplay();
    }

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.valueProperty().addListener((obs, old, val) -> {
            if (context.isUpdatingFromPreset()) return;
            context.notifyParamChange();
            if (!slider.isValueChanging()) {
                context.updatePreview();
            }
        });
        slider.setOnMouseReleased(e -> {
            if (context.isUpdatingFromPreset()) return;
            context.notifyParamChange();
            context.updatePreview();
        });
        return slider;
    }

    private VBox createControlRow(Label label, javafx.scene.Node control, String tooltipText) {
        label.getStyleClass().add("control-label");
        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            if (control instanceof Control ctrl) {
                ctrl.setTooltip(tt);
            }
        }
        return new VBox(4, label, control);
    }

    private VBox createControlRow(Label label, Slider slider, String formatPattern, String tooltipText) {
        label.getStyleClass().add("control-label");
        Label valLabel = new Label(String.format(formatPattern, slider.getValue()));
        valLabel.getStyleClass().add("value-label");

        slider.valueProperty().addListener((obs, old, val) ->
                valLabel.setText(String.format(formatPattern, val.doubleValue()))
        );

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            slider.setTooltip(tt);
            valLabel.setTooltip(tt);
        }

        HBox header = new HBox(label, new Pane(), valLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
    }

    private void commitEditorDoubleText(Spinner<Double> spinner) {
        if (spinner == null || !spinner.isEditable()) return;
        String text = spinner.getEditor().getText();
        if (text == null || text.isBlank()) return;
        String cleanText = text.trim().replace(',', '.').replaceAll("[^0-9.-]", "");
        try {
            double value = Double.parseDouble(cleanText);
            SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                    (SpinnerValueFactory.DoubleSpinnerValueFactory) spinner.getValueFactory();
            value = Math.clamp(value, factory.getMin(), factory.getMax());
            factory.setValue(value);
        } catch (NumberFormatException ignored) {}
    }

    private VBox createControlRowWithSpinner(Label label, Slider slider, Spinner<Double> spinner, String formatPattern, String tooltipText) {
        label.getStyleClass().add("control-label");
        spinner.setEditable(true);
        spinner.setPrefWidth(95);
        spinner.setMaxWidth(110);
        spinner.getStyleClass().add("dense-spinner");

        spinner.getEditor().setOnAction(e -> commitEditorDoubleText(spinner));
        spinner.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) commitEditorDoubleText(spinner);
        });

        slider.valueProperty().addListener((obs, oldV, newV) -> {
            if (context.isUpdatingFromPreset() || isUpdatingControlSync) return;
            isUpdatingControlSync = true;
            try {
                spinner.getValueFactory().setValue(Math.round(newV.doubleValue() * 10.0) / 10.0);
            } finally {
                isUpdatingControlSync = false;
            }
        });

        spinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (context.isUpdatingFromPreset() || isUpdatingControlSync || newV == null) return;
            isUpdatingControlSync = true;
            try {
                slider.setValue(newV);
                context.notifyParamChange();
                context.updatePreview();
            } finally {
                isUpdatingControlSync = false;
            }
        });

        String unit = formatPattern.replaceAll("[^a-zA-Z°☉%²³/]", "").trim();
        Label unitLbl = new Label(unit);
        unitLbl.getStyleClass().add("unit-label");
        HBox valBox = new HBox(4, spinner, unitLbl);
        valBox.setAlignment(Pos.CENTER_RIGHT);

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            slider.setTooltip(tt);
            spinner.setTooltip(tt);
        }

        HBox header = new HBox(label, new Pane(), valBox);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
    }

    // Getters for building preset
    public long getSeed() {
        try {
            return Long.parseLong(seedField.getText());
        } catch (NumberFormatException e) {
            return 12345L;
        }
    }

    public double getMinAltitudeMeters() { return minAltSlider.getValue(); }
    public double getMaxAltitudeMeters() { return maxAltSlider.getValue(); }
    public double getWaterLevel() { return waterSlider.getValue(); }
    public double getNoiseFrequency() { return noiseFreqSlider.getValue(); }
    public double getNoiseScale() { return noiseScaleSlider.getValue(); }
    public boolean isElevationImportActive() { return radioImport != null && radioImport.isSelected(); }
    public String getElevationMapSource() { return mapSourceCombo != null && mapSourceCombo.getValue() != null ? mapSourceCombo.getValue() : "none"; }
    public Image getCustomElevationImage() { return customElevImage; }
    public void setCustomElevationImage(Image img) { this.customElevImage = img; }
    public String getCachedElevationBase64() { return cachedElevB64; }
    public void setCachedElevationBase64(String b64) { this.cachedElevB64 = b64; }

    public Slider getMinAltSlider() { return minAltSlider; }
    public Slider getMaxAltSlider() { return maxAltSlider; }
    public Slider getWaterSlider() { return waterSlider; }
    public Spinner<Double> getMinAltSpinner() { return minAltSpinner; }
    public Spinner<Double> getMaxAltSpinner() { return maxAltSpinner; }
    public ComboBox<String> getMapSourceCombo() { return mapSourceCombo; }
    public RadioButton getRadioProc() { return radioProc; }
    public RadioButton getRadioImport() { return radioImport; }
    public Label getElevFileLabel() { return elevFileLabel; }
    public Button getLoadElevBtn() { return loadElevBtn; }
}
