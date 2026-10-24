/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Random;

/**
 * Autonomous UI Controller and Section Builder for Hydrographic Domains, River Networks, and Freshwater Aquifers.
 *
 * <p>Extracted from {@code ResourceDistributionPanel} to satisfy the Single Responsibility Principle (SRP).
 * Encapsulates the hydrological modeling layer:</p>
 * <ul>
 *   <li><b>Surface Runoff & Fluvial Networks</b>: Watershed flow accumulation, Strahler stream order, and channel slope.</li>
 *   <li><b>Groundwater & Subsurface Aquifers</b>: Piezometric head volume (\(\times 10^3\ \mathrm{km}^3\)) and hydraulic recharge.</li>
 *   <li><b>Empirical GIS Ingestion</b>: Shuttle Radar Topography Mission (SRTM) SWBD / USGS HydroSHEDS raster ingestion.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ResourceHydroSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResourceHydroSectionBuilder.class);

    /**
     * Host callback context required by the hydro builder to decouple from the parent panel.
     */
    public interface HostContext {
        void notifyParamChange();
        void updatePreviewCanvas();
        void selectViewMode(int viewModeIndex);
        PlanetPreset getActivePlanetPreset();
        Window getWindow();
        boolean isUpdatingFromPreset();
        OnlineMapService getOnlineMapService();
        String getMapSourceComboValue();
        void setMapStatus(String status);
    }

    private final HostContext host;
    private final ProceduralGenerator generator = new ProceduralGenerator();

    // UI State & Controls
    private Image customHydroImage;
    private RadioButton radioProcHydro;
    private RadioButton radioImportHydro;
    private TextField hydroSeedField;
    private Button autoDeriveHydroBtn;
    private Label hydroStatusLabel;
    private Slider freshwaterAquiferSlider;
    private Button exportHydroBtn;
    private ComboBox<String> hydroSourceCombo;
    private Label hydroSourcePromptLabel;
    private Label hydroMapRowLabel;
    private Label hydroFileLabel;
    private Label hydroFormatHintLabel;
    private Button loadHydroBtn;
    private Button clearHydroBtn;
    private Label hydroDomainSecHeader;
    private Label freshwaterAquiferRowLabel;

    /**
     * Constructs a hydro section builder attached to a host context.
     *
     * @param host host panel callback interface
     */
    public ResourceHydroSectionBuilder(HostContext host) {
        this.host = host;
    }

    /**
     * Assembles and returns the Hydrography & Freshwater Domain section.
     *
     * @return JavaFX VBox layout container
     */
    public VBox buildHydroSection() {
        ToggleGroup hydroGroup = new ToggleGroup();
        radioProcHydro = new RadioButton(I18n.getOrDefault("resource.mode.procedural_hydro", "▶ Procedural Hydrography"));
        radioImportHydro = new RadioButton(I18n.getOrDefault("resource.mode.import_hydro", "📂 Hydrographic Map (PNG)"));
        radioProcHydro.setToggleGroup(hydroGroup);
        radioImportHydro.setToggleGroup(hydroGroup);
        radioProcHydro.setSelected(true);
        radioProcHydro.getStyleClass().add("radio-proc");
        radioImportHydro.getStyleClass().add("radio-import");

        hydroSeedField = new TextField("23456");
        hydroSeedField.textProperty().addListener((obs, old, val) -> {
            if (!host.isUpdatingFromPreset()) host.notifyParamChange();
            host.updatePreviewCanvas();
        });

        Button hydroRandBtn = new Button("🎲");
        hydroRandBtn.getStyleClass().add("button-secondary");
        hydroRandBtn.setOnAction(e -> {
            hydroSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (!host.isUpdatingFromPreset()) host.notifyParamChange();
            host.updatePreviewCanvas();
        });

        HBox hydroSeedBox = new HBox(5, hydroSeedField, hydroRandBtn);
        HBox.setHgrow(hydroSeedField, Priority.ALWAYS);

        autoDeriveHydroBtn = new Button(I18n.getOrDefault("resource.btn.auto_derive_hydro", "💧 Auto-derive Aquifers & Freshwater from Physics"));
        autoDeriveHydroBtn.getStyleClass().add("button-secondary");
        autoDeriveHydroBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveHydroBtn.setOnAction(e -> autoDeriveHydroFromPlanet());

        hydroStatusLabel = new Label();
        hydroStatusLabel.getStyleClass().add("subcard-status-label");
        hydroStatusLabel.setWrapText(true);

        freshwaterAquiferRowLabel = new Label(I18n.getOrDefault("resource.param.freshwater_aquifers", "Groundwater / Continental Aquifers:"));
        freshwaterAquiferSlider = createSlider(0, 100000, 15000);
        freshwaterAquiferSlider.valueProperty().addListener((obs, old, val) -> {
            if (!host.isUpdatingFromPreset()) host.notifyParamChange();
            host.updatePreviewCanvas();
        });

        exportHydroBtn = new Button(I18n.getOrDefault("resource.btn.export_hydro", "📤 Export Hydrography Map (PNG / JPEG)"));
        exportHydroBtn.setMaxWidth(Double.MAX_VALUE);
        exportHydroBtn.getStyleClass().add("button-secondary");
        exportHydroBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_hydro", "Export the active hydrographic map as high-resolution raster (PNG / JPEG).")));
        exportHydroBtn.setOnAction(e -> exportHydroMap());

        VBox hydroProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), hydroSeedBox,
                        I18n.getOrDefault("resource.tooltip.seed_hydro", "Random seed for procedural rivers and watercourses")),
                autoDeriveHydroBtn,
                hydroStatusLabel,
                createControlRow(freshwaterAquiferRowLabel, freshwaterAquiferSlider, "%.0f x10³ km³",
                        I18n.getOrDefault("resource.desc.freshwater_aquifer", "Total volume of groundwater and continental aquifers")),
                exportHydroBtn
        );
        hydroProcBox.getStyleClass().add("subcard-procedural-box");

        hydroSourceCombo = new ComboBox<>();
        hydroSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon", "mercury");
        hydroSourceCombo.setValue("none");
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
                hydroSourceCombo, "common.combo.prompt_source", "resource.desc.import_hydro_map");
        hydroSourceCombo.setMaxWidth(Double.MAX_VALUE);
        hydroSourceCombo.setOnAction(e -> {
            if (host.isUpdatingFromPreset()) return;
            String val = hydroSourceCombo.getValue();
            if (val != null && !"none".equals(val)) {
                radioImportHydro.setSelected(true);
                fetchOnlineHydroData();
                host.selectViewMode(2);
            }
        });

        hydroMapRowLabel = new Label(I18n.getOrDefault("resource.param.hydro_map", "Hydrographic & Rivers Map (PNG Watercourses):"));
        hydroFileLabel = new Label(I18n.getOrDefault("planet.map.none_file", "- Aucun fichier -"));
        hydroFileLabel.getStyleClass().add("value-label");

        loadHydroBtn = new Button(I18n.get("planet.map.btn_load"));
        loadHydroBtn.getStyleClass().add("button-secondary");
        loadHydroBtn.setOnAction(e -> loadCustomHydroMap());

        clearHydroBtn = new Button("🗑");
        clearHydroBtn.getStyleClass().add("button-secondary");
        clearHydroBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("resource.confirm.title", "Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_hydro", "Do you really want to clear the imported hydrographic map?"));
            alert.initOwner(host.getWindow());
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    customHydroImage = null;
                    hydroFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "- Aucun fichier -"));
                    hydroSourceCombo.setValue("none");
                    host.updatePreviewCanvas();
                    host.notifyParamChange();
                }
            });
        });

        HBox hydroBox = new HBox(5, loadHydroBtn, clearHydroBtn);
        hydroFormatHintLabel = new Label(I18n.getOrDefault("resource.format.hydro_hint",
                "PNG / JPEG (2:1 equirectangular projection):\n  Black (0) = no water | White (255) = river network / max flow."));
        hydroFormatHintLabel.getStyleClass().add("subcard-format-hint");
        hydroFormatHintLabel.setWrapText(true);

        hydroSourcePromptLabel = new Label(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
        VBox hydroImportBox = new VBox(8,
                createControlRow(hydroSourcePromptLabel, hydroSourceCombo,
                        I18n.getOrDefault("resource.desc.import_hydro_map", "Satellite source for hydrographic map")),
                createControlRow(hydroMapRowLabel, new VBox(3, hydroBox, hydroFileLabel, hydroFormatHintLabel),
                        I18n.getOrDefault("resource.desc.import_hydro_map", "Import a hydrographic map and river network"))
        );
        hydroImportBox.getStyleClass().add("subcard-import-box");
        hydroImportBox.setVisible(false);
        hydroImportBox.setManaged(false);

        hydroGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcHydro;
            hydroProcBox.setVisible(isProc);
            hydroProcBox.setManaged(isProc);
            hydroImportBox.setVisible(!isProc);
            hydroImportBox.setManaged(!isProc);
            if (!isProc && customHydroImage == null && hydroSourceCombo.getValue() != null && !"none".equals(hydroSourceCombo.getValue())) {
                fetchOnlineHydroData();
            }
            host.selectViewMode(2);
            if (!host.isUpdatingFromPreset()) {
                host.notifyParamChange();
                host.updatePreviewCanvas();
            }
        });

        hydroDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.hydro", "2. DOMAINE HYDROGRAPHIE & EAU DOUCE"));
        return createSection(hydroDomainSecHeader, new VBox(8,
                radioProcHydro, hydroProcBox, radioImportHydro, hydroImportBox
        ));
    }

    public void loadCustomHydroMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.chooser.hydro", "Load Hydrographic Map (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(host.getWindow());
        if (file != null) {
            try {
                customHydroImage = new Image(new FileInputStream(file));
                if (hydroFileLabel != null) hydroFileLabel.setText("🌊 " + file.getName());
                host.selectViewMode(2);
                host.notifyParamChange();
                host.updatePreviewCanvas();
            } catch (Exception ex) {
                logger.error("Failed to load custom hydro map", ex);
            }
        }
    }

    public void fetchOnlineHydroData() {
        String sourceKey = host.getMapSourceComboValue() != null ? host.getMapSourceComboValue() : "earth";
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };
        host.setMapStatus(I18n.getOrDefault("planet.map.status_fetching", "Downloading satellite hydrography WMS..."));
        host.getOnlineMapService().fetchHydroMapAsync(body).thenAccept(img -> javafx.application.Platform.runLater(() -> {
            if (img != null) {
                customHydroImage = img;
                if (hydroFileLabel != null) hydroFileLabel.setText("🌐 " + body.getName() + " USGS HydroSHEDS / SWBD Hydro WMS");
                host.setMapStatus(I18n.getOrDefault("planet.map.status_success", "Hydrographic map downloaded successfully."));
                host.updatePreviewCanvas();
                host.notifyParamChange();
            } else {
                if ("earth".equals(sourceKey) && radioImportHydro != null && radioImportHydro.isSelected()) {
                    if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.source.earth_hydro_wms", "🌊 Earth SWBD / USGS HydroSHEDS Hydrographie"));
                    host.setMapStatus(I18n.getOrDefault("resource.status.usgs_active", "🌐 Earth Hydrographic Map USGS HydroSHEDS / SWBD active."));
                    host.updatePreviewCanvas();
                    host.notifyParamChange();
                } else {
                    host.setMapStatus(I18n.getOrDefault("resource.map.procedural_fallback", "Info: Falling back to procedural hydrographic map (slope)."));
                    generateProceduralHydrography();
                }
            }
        }));
    }

    public void generateProceduralHydrography() {
        customHydroImage = null;
        if (radioProcHydro != null) radioProcHydro.setSelected(true);
        host.selectViewMode(2);
        if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("resource.mode.procedural_hydro", "⚡ Procedural Hydrography (Rivers/Slope)"));
        host.setMapStatus(I18n.getOrDefault("resource.map.procedural_generated", "⚡ Procedural hydrographic map generated via slope and watershed calculation."));
        if (!host.isUpdatingFromPreset()) host.notifyParamChange();
        host.updatePreviewCanvas();
    }

    public void autoDeriveHydroFromPlanet() {
        PlanetPreset p = host.getActivePlanetPreset();
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        double radiusRatio = p.radiusKm() / 6371.0;
        double areaScale = Math.pow(radiusRatio, 2);
        double waterMod = Math.max(0.05, 1.0 + p.waterLevel());
        double aquifer = Math.round(15000.0 * areaScale * waterMod);
        if (freshwaterAquiferSlider != null) {
            freshwaterAquiferSlider.setValue(aquifer);
        }
        if (radioProcHydro != null) {
            radioProcHydro.setSelected(true);
        }
        if (hydroStatusLabel != null) {
            hydroStatusLabel.setText(String.format(I18n.getOrDefault("resource.status.auto_derived_hydro",
                    "⚡ Auto-derived from %s: Groundwater / Aquifers=%.0f x10³ km³"), p.name(), aquifer));
        }
        if (!host.isUpdatingFromPreset()) host.notifyParamChange();
        host.updatePreviewCanvas();
        logger.info("Auto-derived hydrographical parameters for planet {}", p.name());
    }

    public Image generateProceduralHydroRasterImage(int width, int height) {
        PlanetPreset planet = host.getActivePlanetPreset();
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();
        double wLvl = planet.waterLevel();
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - ((double) y / height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = ((double) x / width) * 360.0 - 180.0;
                var pt = generator.getPlanetPoint(lat, lon, planet);
                Color c;
                if (wLvl > -0.4 && pt.elevation() < wLvl) {
                    c = Color.rgb(15, 23, 42);
                } else {
                    double riverFlow = pt.riverFlow();
                    if (riverFlow > 0.42) c = Color.rgb(2, 132, 199);
                    else if (riverFlow > 0.28) c = Color.rgb(56, 189, 248);
                    else if (riverFlow > 0.16) c = Color.rgb(20, 184, 166);
                    else {
                        int r = (int) Math.min(255, 45 + pt.declivity() * 100);
                        int g = (int) Math.min(255, 60 + pt.declivity() * 80);
                        int b = (int) Math.min(255, 55 + pt.declivity() * 50);
                        c = Color.rgb(r, g, b);
                    }
                }
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    public void exportHydroMap() {
        Image img = (radioImportHydro != null && radioImportHydro.isSelected() && customHydroImage != null)
                ? customHydroImage : generateProceduralHydroRasterImage(1024, 512);
        WindowUtils.exportImageWithChooser(host.getWindow(),
                img, "hydrography_map.png", I18n.getOrDefault("resource.dialog.export_hydro_title", "Export Hydrography Map (PNG / JPEG)"));
    }

    public String getHydroSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "🌊 Mars MARSIS / Ancient Paleochannels Hydrography";
            case "moon" -> "🌊 Moon Polar Traps (Dry Crust Hydrography)";
            case "venus" -> "🌊 Venus Volcanic Channels (Anhydrous Hydrography)";
            case "mercury" -> "🌊 Mercury Polar Cold Traps Hydrography";
            default -> I18n.getOrDefault("resource.source.earth_hydro", "🌊 Earth SWBD Hydrography");
        };
    }

    public void applyPresetHydro(org.ether.society.model.EcologyPreset p, String planetKey, long epochYear) {
        if (p != null && p.customHydroBase64() != null) {
            customHydroImage = ImageMapLoader.base64PngToImage(p.customHydroBase64());
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("planet.preset.hydro", "💧 Preset Hydro"));
            if (radioImportHydro != null) radioImportHydro.setSelected(true);
        } else if ("earth".equals(planetKey)) {
            if (hydroSourceCombo != null) hydroSourceCombo.setValue("earth");
            customHydroImage = ImageMapLoader.loadMapImage("earth", epochYear, "aquifers");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (hydroFileLabel != null) hydroFileLabel.setText(getHydroSourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportHydro != null) radioImportHydro.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            if (hydroSourceCombo != null) hydroSourceCombo.setValue(planetKey);
            customHydroImage = ImageMapLoader.loadMapImage(planetKey + "_aquifers.png");
            if (hydroFileLabel != null) hydroFileLabel.setText(getHydroSourceDisplayName(planetKey));
            if (radioImportHydro != null) radioImportHydro.setSelected(true);
        } else {
            customHydroImage = null;
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
            if (radioProcHydro != null) radioProcHydro.setSelected(true);
            if (hydroSourceCombo != null) hydroSourceCombo.setValue("none");
        }
    }

    public void applyPresetMapSource(String sourceKey, String body) {
        if (sourceKey == null || "none".equals(sourceKey)) {
            customHydroImage = null;
            if (hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
            if (radioProcHydro != null) radioProcHydro.setSelected(true);
            if (hydroSourceCombo != null) hydroSourceCombo.setValue("none");
        } else {
            if (radioImportHydro != null) radioImportHydro.setSelected(true);
            if (hydroSourceCombo != null) hydroSourceCombo.setValue(body);
            customHydroImage = ImageMapLoader.loadMapImage(body + "_aquifers.png");
            if (hydroFileLabel != null) hydroFileLabel.setText(getHydroSourceDisplayName(body));
        }
    }

    public void updateTexts() {
        if (hydroDomainSecHeader != null) hydroDomainSecHeader.setText(I18n.getOrDefault("resource.domain.hydro", "2. DOMAINE HYDROGRAPHIE & EAU DOUCE"));
        if (hydroMapRowLabel != null) hydroMapRowLabel.setText(I18n.getOrDefault("resource.param.hydro_map", "Hydrographic & Rivers Map (PNG Watercourses):"));
        if (radioProcHydro != null) radioProcHydro.setText(I18n.getOrDefault("resource.mode.procedural_hydro", "▶ Procedural Hydrography"));
        if (radioImportHydro != null) radioImportHydro.setText(I18n.getOrDefault("resource.mode.import_hydro", "📂 Hydrographic Map (PNG)"));
        if (loadHydroBtn != null) loadHydroBtn.setText(I18n.get("planet.map.btn_load"));
        if (customHydroImage == null && hydroFileLabel != null) hydroFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "- No file loaded -"));
        if (hydroFormatHintLabel != null) hydroFormatHintLabel.setText(I18n.getOrDefault("resource.format.hydro_hint", "PNG / JPEG (2:1 equirectangular projection):\n  Black (0) = no water | White (255) = river network / max flow."));
        if (exportHydroBtn != null) exportHydroBtn.setText(I18n.getOrDefault("resource.btn.export_hydro", "📤 Export Hydrography Map (PNG / JPEG)"));
        if (hydroSourcePromptLabel != null) hydroSourcePromptLabel.setText(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
        if (freshwaterAquiferRowLabel != null) freshwaterAquiferRowLabel.setText(I18n.getOrDefault("resource.param.freshwater_aquifers", "Groundwater / Continental Aquifers:"));
    }

    private VBox createSection(Label header, VBox content) {
        VBox card = new VBox(8, header, content);
        card.getStyleClass().add("card-section");
        card.setPadding(new Insets(10));
        return card;
    }

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(false);
        slider.getStyleClass().add("slider-modern");
        return slider;
    }

    private VBox createControlRow(Label label, Node control, String tooltipText) {
        label.getStyleClass().add("label-control");
        if (tooltipText != null && !tooltipText.isBlank()) {
            label.setTooltip(new Tooltip(tooltipText));
        }
        VBox row = new VBox(2, label, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox createControlRow(Label label, Slider slider, String formatPattern, String tooltipText) {
        label.getStyleClass().add("label-control");
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("value-label");
        valueLabel.setText(String.format(formatPattern, slider.getValue()));
        slider.valueProperty().addListener((obs, old, val) ->
                valueLabel.setText(String.format(formatPattern, val.doubleValue())));

        HBox header = new HBox(8, label, valueLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(label, Priority.ALWAYS);

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            slider.setTooltip(tt);
        }

        VBox row = new VBox(2, header, slider);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // Getters & Setters
    public Image getCustomHydroImage() { return customHydroImage; }
    public void setCustomHydroImage(Image customHydroImage) { this.customHydroImage = customHydroImage; }
    public boolean isRadioImportHydroSelected() { return radioImportHydro != null && radioImportHydro.isSelected(); }
    public void setRadioProcHydroSelected(boolean selected) { if (radioProcHydro != null) radioProcHydro.setSelected(selected); }
    public void setRadioImportHydroSelected(boolean selected) { if (radioImportHydro != null) radioImportHydro.setSelected(selected); }
    public Slider getFreshwaterAquiferSlider() { return freshwaterAquiferSlider; }
    public TextField getHydroSeedField() { return hydroSeedField; }
    public ComboBox<String> getHydroSourceCombo() { return hydroSourceCombo; }
    public Label getHydroFileLabel() { return hydroFileLabel; }
    public Button getLoadHydroBtn() { return loadHydroBtn; }
}
