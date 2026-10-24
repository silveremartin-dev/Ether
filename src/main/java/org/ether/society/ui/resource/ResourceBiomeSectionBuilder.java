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
import org.ether.society.model.Biome;
import org.ether.society.model.EcologyPreset;
import org.ether.society.ui.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Random;

/**
 * Autonomous UI Controller and Section Builder for Biomes, Flora, Soils, and Terrestrial/Aquatic Fauna.
 *
 * <p>Extracted from {@code ResourceDistributionPanel} to satisfy the Single Responsibility Principle (SRP).
 * Encapsulates the biological and ecological modeling layer:</p>
 * <ul>
 *   <li><b>Plant Biomass & Carbon Stocks</b>: Terrestrial plant biomass (\(\mathrm{GtC}\)) and soil organic carbon (\(\mathrm{GtC}\)).</li>
 *   <li><b>Heterotrophic Animal Stocks</b>: Wild terrestrial animal biomass (\(\mathrm{GtC}\)) and marine/estuarine biomass (\(\mathrm{GtC}\)).</li>
 *   <li><b>Categorical Biome Cartography</b>: Polychrome discrete classification of 14 ecological biomes.</li>
 *   <li><b>Empirical WMS Ingestion</b>: Satellite ingestion from NASA MODIS (MCD12Q1 Land Cover Type).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ResourceBiomeSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResourceBiomeSectionBuilder.class);

    /**
     * Host callback context required by the biome builder to decouple from the parent panel.
     */
    public interface HostContext {
        void notifyParamChange();
        void updatePreviewCanvas();
        void updateSummary();
        void selectViewMode(int viewModeIndex);
        PlanetPreset getActivePlanetPreset();
        Window getWindow();
        boolean isUpdatingFromPreset();
        OnlineMapService getOnlineMapService();
        String getMapSourceComboValue();
        void setMapStatus(String status);
        void applyPresetMapSource(String sourceKey);
    }

    private final HostContext host;
    private final ProceduralGenerator generator = new ProceduralGenerator();
    private final ImageMapLoader mapLoader = new ImageMapLoader();

    // Custom Biome Map
    private Image customBiomeImage;

    // UI State & Controls
    private RadioButton radioProcBiome;
    private RadioButton radioImportBiome;
    private TextField biomeSeedField;
    private Button randSeedBtn;
    private Button autoDeriveEcologyBtn;
    private Label biomeStatusLabel;
    private Button exportBiomeBtn;
    private ComboBox<String> biomeSourceCombo;
    private Label biomeSourcePromptLabel;
    private Label biomeMapRowLabel;
    private Label biomeFileLabel;
    private Label biomeFormatHintLabel;
    private Button loadBiomeBtn;
    private Button clearBiomeBtn;
    private Label biomeDomainSecHeader;

    // Sliders for Biological & Soil Metrics
    private Slider terrestrialBiomassSlider;
    private Slider soilCarbonSlider;
    private Slider faunaBiomassSlider;
    private Slider aquaticBiomassSlider;
    private Label terrestrialBiomassRowLabel;
    private Label soilCarbonRowLabel;
    private Label faunaBiomassRowLabel;
    private Label aquaticBiomassRowLabel;
    private Label floraSecHeader;
    private Label faunaSecHeader;

    /**
     * Constructs a biome section builder attached to a host context.
     *
     * @param host host panel callback interface
     */
    public ResourceBiomeSectionBuilder(HostContext host) {
        this.host = host;
    }

    /**
     * Assembles and returns the Biome & Flora Domain section container.
     *
     * @return JavaFX VBox layout container
     */
    public VBox buildBiomeSection() {
        // --- 5. Flora & Terrestrial Plant Resources ---
        terrestrialBiomassSlider = createSlider(1.0, 5000.0, 450.0);
        soilCarbonSlider = createSlider(10.0, 10000.0, 1500.0);
        terrestrialBiomassRowLabel = new Label(I18n.getOrDefault("resource.label.plant_biomass", "Global Plant Biomass & Forests:"));
        soilCarbonRowLabel = new Label(I18n.getOrDefault("resource.param.soil_carbon", "Soil Organic Carbon (GtC):"));
        floraSecHeader = new Label(I18n.getOrDefault("resource.section.vegetation", "VEGETATION & SOILS (METRICS)"));
        VBox floraSection = new VBox(8,
                createControlRow(terrestrialBiomassRowLabel, terrestrialBiomassSlider, "%.0f GtC",
                        I18n.getOrDefault("resource.desc.terrestrial_biomass", "Global stock of plant biomass and forests (Gigatons of Carbon)")),
                createControlRow(soilCarbonRowLabel, soilCarbonSlider, "%.0f GtC",
                        I18n.getOrDefault("resource.desc.soil_carbon", "Soil organic carbon stock and agricultural potential (GtC)"))
        );

        // --- 6. Fauna & Animal Resources ---
        faunaBiomassSlider = createSlider(0.01, 50.0, 2.0);
        aquaticBiomassSlider = createSlider(0.1, 100.0, 6.0);
        faunaBiomassRowLabel = new Label(I18n.getOrDefault("resource.label.wildlife_biomass", "Terrestrial & Wild Animal Biomass:"));
        aquaticBiomassRowLabel = new Label(I18n.getOrDefault("resource.label.marine_biomass", "Marine & Estuarine Biomass:"));
        faunaSecHeader = new Label(I18n.getOrDefault("resource.section.animal_biomass", "ANIMAL BIOMASS (METRICS)"));
        VBox faunaSection = new VBox(8,
                createControlRow(faunaBiomassRowLabel, faunaBiomassSlider, "%.2f GtC",
                        I18n.getOrDefault("resource.desc.fauna_biomass", "Total biomass of wild terrestrial vertebrates (GtC)")),
                createControlRow(aquaticBiomassRowLabel, aquaticBiomassSlider, "%.2f GtC",
                        I18n.getOrDefault("resource.desc.aquatic_biomass", "Marine biomass stock, fish and phytoplankton (GtC)"))
        );

        ToggleGroup biomeGroup = new ToggleGroup();
        radioProcBiome = new RadioButton(I18n.getOrDefault("resource.mode.procedural_biome", "▶ Procedural Biomes"));
        radioImportBiome = new RadioButton(I18n.getOrDefault("resource.mode.import_biome", "📂 Biome Map (PNG)"));
        radioProcBiome.setToggleGroup(biomeGroup);
        radioImportBiome.setToggleGroup(biomeGroup);
        radioProcBiome.setSelected(true);
        radioProcBiome.getStyleClass().add("radio-proc");
        radioImportBiome.getStyleClass().add("radio-import");

        biomeSeedField = new TextField("12345");
        biomeSeedField.textProperty().addListener((obs, old, val) -> {
            if (!host.isUpdatingFromPreset()) host.notifyParamChange();
            host.updatePreviewCanvas();
        });

        randSeedBtn = new Button("🎲");
        randSeedBtn.getStyleClass().add("button-secondary");
        randSeedBtn.setOnAction(e -> {
            biomeSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (!host.isUpdatingFromPreset()) host.notifyParamChange();
            host.updatePreviewCanvas();
        });

        HBox biomeSeedBox = new HBox(5, biomeSeedField, randSeedBtn);
        HBox.setHgrow(biomeSeedField, Priority.ALWAYS);

        exportBiomeBtn = new Button(I18n.getOrDefault("resource.btn.export_biome", "📤 Export Biome Map (PNG / JPEG)"));
        exportBiomeBtn.setMaxWidth(Double.MAX_VALUE);
        exportBiomeBtn.getStyleClass().add("button-secondary");
        exportBiomeBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.export_biome", "Export the active biome map as high-resolution raster (PNG / JPEG).")));
        exportBiomeBtn.setOnAction(e -> exportBiomeMap());

        autoDeriveEcologyBtn = new Button(I18n.getOrDefault("resource.btn.auto_derive_eco", "⚡ Auto-derive Ecology & Biomes from Physics"));
        autoDeriveEcologyBtn.getStyleClass().add("button-secondary");
        autoDeriveEcologyBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveEcologyBtn.setOnAction(e -> autoDeriveEcologyFromPlanet());

        biomeStatusLabel = new Label();
        biomeStatusLabel.getStyleClass().add("subcard-status-label");
        biomeStatusLabel.setWrapText(true);

        VBox biomeProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), biomeSeedBox,
                        I18n.getOrDefault("resource.tooltip.seed", "Random seed for procedural biome distribution")),
                autoDeriveEcologyBtn,
                biomeStatusLabel,
                floraSection, faunaSection, exportBiomeBtn
        );
        biomeProcBox.getStyleClass().add("subcard-procedural-box");

        biomeSourceCombo = new ComboBox<>();
        biomeSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon", "mercury");
        biomeSourceCombo.setValue("none");
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
                biomeSourceCombo, "common.combo.prompt_source", "resource.desc.import_biome_map");
        biomeSourceCombo.setMaxWidth(Double.MAX_VALUE);
        biomeSourceCombo.setOnAction(e -> {
            if (host.isUpdatingFromPreset()) return;
            String val = biomeSourceCombo.getValue();
            if (val != null && !"none".equals(val)) {
                radioImportBiome.setSelected(true);
                host.applyPresetMapSource(val);
                host.selectViewMode(1);
            }
        });

        biomeSourcePromptLabel = new Label(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
        biomeMapRowLabel = new Label(I18n.getOrDefault("resource.param.biome_map", "Biome / Extraterrestrial Vegetation Map (PNG):"));
        biomeFileLabel = new Label(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
        biomeFileLabel.getStyleClass().add("value-label");

        loadBiomeBtn = new Button(I18n.get("planet.map.btn_load"));
        loadBiomeBtn.getStyleClass().add("button-secondary");
        loadBiomeBtn.setOnAction(e -> loadCustomBiomeMap());

        clearBiomeBtn = new Button("❌");
        clearBiomeBtn.getStyleClass().add("button-secondary");
        clearBiomeBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_biome", "Do you really want to clear the imported biome map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.initOwner(host.getWindow());
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customBiomeImage = null;
                    biomeFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
                    host.updatePreviewCanvas();
                    host.updateSummary();
                    host.notifyParamChange();
                }
            });
        });

        HBox biomeBox = new HBox(5, loadBiomeBtn, clearBiomeBtn);
        biomeFormatHintLabel = new Label(I18n.getOrDefault("resource.format.biome_hint",
                "PNG / JPEG (2:1 equirectangular projection):\nBiome image or vegetation cover (grayscale or RGB)."));
        biomeFormatHintLabel.setWrapText(true);
        biomeFormatHintLabel.getStyleClass().add("hint-label");

        VBox biomeImportBox = new VBox(8,
                createControlRow(biomeSourcePromptLabel, biomeSourceCombo,
                        I18n.getOrDefault("resource.desc.import_biome_map", "Select a preset satellite source")),
                createControlRow(biomeMapRowLabel, new VBox(3, biomeBox, biomeFileLabel, biomeFormatHintLabel),
                        I18n.getOrDefault("resource.desc.import_biome_map", "Import a biome map in PNG/JPEG format"))
        );
        biomeImportBox.getStyleClass().add("subcard-import-box");
        biomeImportBox.setVisible(false);
        biomeImportBox.setManaged(false);

        biomeGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcBiome;
            biomeProcBox.setVisible(isProc);
            biomeProcBox.setManaged(isProc);
            biomeImportBox.setVisible(!isProc);
            biomeImportBox.setManaged(!isProc);
            if (!isProc && customBiomeImage == null && biomeSourceCombo.getValue() != null && !"none".equals(biomeSourceCombo.getValue())) {
                host.applyPresetMapSource(biomeSourceCombo.getValue());
            }
            host.selectViewMode(1);
            if (!host.isUpdatingFromPreset()) {
                host.notifyParamChange();
                host.updatePreviewCanvas();
            }
        });

        biomeDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.biome", "1. BIOME & FLORA DOMAIN (VEGETATION)"));
        return createSection(biomeDomainSecHeader, new VBox(8,
                radioProcBiome, biomeProcBox, radioImportBiome, biomeImportBox
        ));
    }

    public void autoDeriveEcologyFromPlanet() {
        PlanetPreset p = host.getActivePlanetPreset();
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        double radiusRatio = p.radiusKm() / 6371.0;
        double areaScale = Math.pow(radiusRatio, 2);
        double waterMod = Math.max(0.05, 1.0 + p.waterLevel());
        double tempMod = Math.max(0.1, 1.0 - Math.abs(p.averageTempC() - 15.0) / 60.0);
        double tBiomass = Math.round(450.0 * areaScale * waterMod * tempMod);
        double sCarbon = Math.round(1500.0 * areaScale * tempMod);
        double fBiomass = Math.round(2.0 * areaScale * waterMod * tempMod * 100.0) / 100.0;
        double aBiomass = Math.round(6.0 * areaScale * (1.0 + p.waterLevel() * 1.5) * 100.0) / 100.0;
        if (terrestrialBiomassSlider != null) terrestrialBiomassSlider.setValue(tBiomass);
        if (soilCarbonSlider != null) soilCarbonSlider.setValue(sCarbon);
        if (faunaBiomassSlider != null) faunaBiomassSlider.setValue(fBiomass);
        if (aquaticBiomassSlider != null) aquaticBiomassSlider.setValue(aBiomass);
        if (biomeStatusLabel != null) {
            biomeStatusLabel.setText(String.format(I18n.getOrDefault("resource.status.auto_derived_biome",
                    "⚡ Auto-derived from %s: Plant=%.0f GtC | Soils=%.0f GtC | Fauna=%.2f GtC | Aquatic=%.2f GtC"),
                    p.name(), tBiomass, sCarbon, fBiomass, aBiomass));
        }
        if (!host.isUpdatingFromPreset()) host.notifyParamChange();
        host.updateSummary();
        host.updatePreviewCanvas();
        logger.info("Auto-derived ecological parameters for planet {}", p.name());
    }

    public void loadCustomBiomeMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("resource.chooser.biome", "Load Biome Map (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(host.getWindow());
        if (file != null) {
            try {
                customBiomeImage = new Image(new FileInputStream(file));
                if (biomeFileLabel != null) biomeFileLabel.setText("📷 " + file.getName());
                host.selectViewMode(1);
                host.updateSummary();
                host.updatePreviewCanvas();
                host.notifyParamChange();
            } catch (Exception ex) {
                logger.error("Failed to load custom biome map", ex);
            }
        }
    }

    public void fetchOnlineSatelliteData() {
        String sourceKey = host.getMapSourceComboValue();
        if (sourceKey == null) sourceKey = "earth";
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };
        host.setMapStatus(I18n.getOrDefault("planet.map.status_fetching", "WMS Download in Progress..."));
        if (body.getBiomeWmsUrl() != null) {
            host.getOnlineMapService().fetchBiomeMapAsync(body).thenAccept(img -> javafx.application.Platform.runLater(() -> {
                if (img != null) {
                    customBiomeImage = img;
                    if (biomeFileLabel != null) biomeFileLabel.setText("🌐 " + body.getName() + " WMS Biomes");
                    host.setMapStatus(I18n.getOrDefault("planet.map.status_success", "Satellite map downloaded successfully."));
                    host.updatePreviewCanvas();
                    host.updateSummary();
                    host.notifyParamChange();
                } else {
                    host.setMapStatus(I18n.getOrDefault("planet.map.status_error", "Download error."));
                }
            }));
        } else {
            host.setMapStatus(I18n.getOrDefault("resource.map.wms_fallback", "Info: No specific WMS for ") + body.getName()
                    + I18n.getOrDefault("resource.map.using_procedural", ", using procedural model."));
        }
    }

    public Image generateProceduralBiomeRasterImage(int width, int height) {
        PlanetPreset planet = host.getActivePlanetPreset();
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < height; y++) {
            double lat = 90.0 - ((double) y / height) * 180.0;
            for (int x = 0; x < width; x++) {
                double lon = ((double) x / width) * 360.0 - 180.0;
                var pt = generator.getPlanetPoint(lat, lon, planet);
                Color c = mapLoader.getBiomeTargetColor(pt.biome());
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    public void exportBiomeMap() {
        Image img = (radioImportBiome != null && radioImportBiome.isSelected() && customBiomeImage != null)
                ? customBiomeImage : generateProceduralBiomeRasterImage(1024, 512);
        WindowUtils.exportImageWithChooser(host.getWindow(),
                img, "biomes_map.png", I18n.getOrDefault("resource.dialog.export_biome_title", "Export Biome Map (PNG / JPEG)"));
    }

    public String getBiomeSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "🌿 Mars MOLA & Biomes Reconstitution";
            case "moon" -> "🌿 Moon Clementine Regolith (Barren Biome)";
            case "venus" -> "🌿 Venus Magellan Radar (Supercritical Biome)";
            case "mercury" -> "🌿 Mercury MESSENGER Surface (Airless Biome)";
            default -> I18n.getOrDefault("planet.source.earth_modis_biomes", "🌿 Earth MODIS Biomes");
        };
    }

    public void applyPresetBiome(EcologyPreset p, String planetKey, long epochYear) {
        if (p != null && p.customBiomeBase64() != null) {
            customBiomeImage = ImageMapLoader.base64PngToImage(p.customBiomeBase64());
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.preset.biomes", "🌿 Preset Biomes"));
            if (radioImportBiome != null) radioImportBiome.setSelected(true);
        } else if ("earth".equals(planetKey)) {
            if (biomeSourceCombo != null) biomeSourceCombo.setValue("earth");
            customBiomeImage = ImageMapLoader.loadMapImage("earth", epochYear, "biomes");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (biomeFileLabel != null) biomeFileLabel.setText(getBiomeSourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportBiome != null) radioImportBiome.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            if (biomeSourceCombo != null) biomeSourceCombo.setValue(planetKey);
            customBiomeImage = ImageMapLoader.loadMapImage(planetKey + "_biomes.png");
            if (biomeFileLabel != null) biomeFileLabel.setText(getBiomeSourceDisplayName(planetKey));
            if (radioImportBiome != null) radioImportBiome.setSelected(true);
        } else {
            customBiomeImage = null;
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
            if (radioProcBiome != null) radioProcBiome.setSelected(true);
            if (biomeSourceCombo != null) biomeSourceCombo.setValue("none");
        }
    }

    public void applyPresetMapSource(String sourceKey, String body) {
        if ("none".equals(sourceKey) || sourceKey == null) {
            customBiomeImage = null;
            if (biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— Aucun fichier —"));
            if (radioProcBiome != null) radioProcBiome.setSelected(true);
            if (biomeSourceCombo != null) biomeSourceCombo.setValue("none");
        } else {
            if (radioImportBiome != null) radioImportBiome.setSelected(true);
            if (biomeSourceCombo != null) biomeSourceCombo.setValue(body);
            customBiomeImage = ImageMapLoader.loadMapImage(body + "_biomes.png");
            if (biomeFileLabel != null) biomeFileLabel.setText(getBiomeSourceDisplayName(body));
        }
    }

    public void updateTexts() {
        if (biomeDomainSecHeader != null) biomeDomainSecHeader.setText(I18n.getOrDefault("resource.domain.biome", "1. BIOME & FLORA DOMAIN (VEGETATION)"));
        if (radioProcBiome != null) radioProcBiome.setText(I18n.getOrDefault("resource.mode.procedural_biome", "▶ Procedural Biomes"));
        if (radioImportBiome != null) radioImportBiome.setText(I18n.getOrDefault("resource.mode.import_biome", "📂 Biome Map (PNG)"));
        if (biomeMapRowLabel != null) biomeMapRowLabel.setText(I18n.getOrDefault("resource.param.biome_map", "Biome / Extraterrestrial Vegetation Map (PNG):"));
        if (loadBiomeBtn != null) loadBiomeBtn.setText(I18n.get("planet.map.btn_load"));
        if (customBiomeImage == null && biomeFileLabel != null) biomeFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
        if (biomeFormatHintLabel != null) biomeFormatHintLabel.setText(I18n.getOrDefault("resource.format.biome_hint", "PNG / JPEG (2:1 equirectangular projection):\n  Biome image or vegetation cover (grayscale or RGB)."));
        if (exportBiomeBtn != null) exportBiomeBtn.setText(I18n.getOrDefault("resource.btn.export_biome", "📤 Export Biome Map (PNG + WorldFile)"));
        if (autoDeriveEcologyBtn != null) autoDeriveEcologyBtn.setText(I18n.getOrDefault("resource.btn.auto_derive_eco", "⚡ Auto-derive Ecology & Biomes from Physics"));
        if (biomeSourcePromptLabel != null) biomeSourcePromptLabel.setText(I18n.getOrDefault("resource.param.map_source", "Celestial Body / Source:"));
        if (terrestrialBiomassRowLabel != null) terrestrialBiomassRowLabel.setText(I18n.getOrDefault("resource.param.terrestrial_biomass", "Terrestrial Plant Biomass (GtC):"));
        if (soilCarbonRowLabel != null) soilCarbonRowLabel.setText(I18n.getOrDefault("resource.param.soil_carbon", "Soil Organic Carbon (GtC):"));
        if (faunaBiomassRowLabel != null) faunaBiomassRowLabel.setText(I18n.getOrDefault("resource.param.fauna_biomass", "Terrestrial Animal Biomass (GtC):"));
        if (aquaticBiomassRowLabel != null) aquaticBiomassRowLabel.setText(I18n.getOrDefault("resource.param.aquatic_biomass", "Aquatic Biomass & Fauna (GtC):"));
        if (floraSecHeader != null) floraSecHeader.setText(I18n.getOrDefault("resource.section.flora", "PLANT ECOLOGY & SOILS (METRICS)"));
        if (faunaSecHeader != null) faunaSecHeader.setText(I18n.getOrDefault("resource.section.fauna", "ANIMAL & AQUATIC BIOMASS (METRICS)"));
        if (biomeSourceCombo != null && biomeSourceCombo.getCellFactory() != null) {
            biomeSourceCombo.setButtonCell(biomeSourceCombo.getCellFactory().call(null));
        }
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

    // Getters and Setters
    public Image getCustomBiomeImage() { return customBiomeImage; }
    public void setCustomBiomeImage(Image img) { this.customBiomeImage = img; }
    public boolean isRadioImportBiomeSelected() { return radioImportBiome != null && radioImportBiome.isSelected(); }
    public void setRadioProcBiomeSelected(boolean selected) { if (radioProcBiome != null) radioProcBiome.setSelected(selected); }
    public void setRadioImportBiomeSelected(boolean selected) { if (radioImportBiome != null) radioImportBiome.setSelected(selected); }

    public Slider getTerrestrialBiomassSlider() { return terrestrialBiomassSlider; }
    public Slider getSoilCarbonSlider() { return soilCarbonSlider; }
    public Slider getFaunaBiomassSlider() { return faunaBiomassSlider; }
    public Slider getAquaticBiomassSlider() { return aquaticBiomassSlider; }

    public TextField getBiomeSeedField() { return biomeSeedField; }
    public ComboBox<String> getBiomeSourceCombo() { return biomeSourceCombo; }
    public Label getBiomeFileLabel() { return biomeFileLabel; }
    public Button getLoadBiomeBtn() { return loadBiomeBtn; }
}
