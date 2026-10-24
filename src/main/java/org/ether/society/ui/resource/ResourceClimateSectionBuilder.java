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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.EcologyPreset;
import org.ether.society.ui.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Random;

/**
 * Autonomous UI Controller and Section Builder for Climate, Atmospheric, and Precipitation Domains.
 *
 * <p>Extracted from {@code ResourceDistributionPanel} to satisfy the Single Responsibility Principle (SRP).
 * Encapsulates the macro-climatic modeling and observation layer:</p>
 * <ul>
 *   <li><b>Surface Temperature Field</b>: Mean Annual Temperature (MAT, \(-50^\circ\mathrm{C}\) to \(+50^\circ\mathrm{C}\)), thermal inertia, and latitudinal insolation gradients.</li>
 *   <li><b>Hydrological Precipitation</b>: Mean Annual Precipitation (MAP, \(0\) to \(3\,000\ \mathrm{mm/yr}\)) and humidity regimes.</li>
 *   <li><b>Thermal Seasonality</b>: Seasonal temperature variance (\(\Delta T_{\mathrm{annual}}\), \(0^\circ\mathrm{C}\) to \(50^\circ\mathrm{C}\)) driven by orbital obliquity and spin-orbit resonances.</li>
 *   <li><b>Empirical WMS Ingestion</b>: Satellite ingestion from NASA MODIS (Land Surface Temperature) and NASA/JAXA GPM IMERG (Global Precipitation Measurement).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ResourceClimateSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResourceClimateSectionBuilder.class);

    /**
     * Host context interface decoupling the climate builder from {@code ResourceDistributionPanel}.
     */
    public interface HostContext {
        void notifyParamChange();
        void updatePreviewCanvas();
        PlanetPreset getActivePlanetPreset();
        Window getWindow();
        boolean isUpdatingFromPreset();
        OnlineMapService getOnlineMapService();
        String getMapSourceComboValue();
        void setMapStatus(String status);
    }

    private final HostContext host;

    // Climate Map Rasters
    private Image customClimateImage;     // Temperature / Combined RGB
    private Image customRainfallImage;    // Precipitation
    private Image customSeasonalityImage; // Seasonality

    // UI Controls
    private RadioButton radioProcClimate;
    private RadioButton radioImportClimate;
    private TextField climateSeedField;
    private ComboBox<String> climateSourceCombo;
    private Label climateMapRowLabel;
    private Label rainfallMapRowLabel;
    private Label seasonalityMapRowLabel;
    private Label climateFileLabel;
    private Label rainfallFileLabel;
    private Label seasonalityFileLabel;
    private Button loadClimateBtn;
    private Button clearClimateBtn;
    private Button loadRainfallBtn;
    private Button clearRainfallBtn;
    private Button loadSeasonalityBtn;
    private Button clearSeasonalityBtn;
    private Button fetchOnlineClimateBtn;
    private Button climateHelpBtn;
    private Label climateStatusLabel;
    private Label climateSummaryLabel;
    private Label climateDomainSecHeader;
    private Label climateSourcePromptLabel;

    /**
     * Constructs a climate section builder attached to a host panel context.
     *
     * @param host host panel callback interface
     */
    public ResourceClimateSectionBuilder(HostContext host) {
        this.host = host;
    }

    /**
     * Builds and returns the complete Climate & Atmosphere Domain section container.
     *
     * @return JavaFX VBox layout container
     */
    public VBox buildClimateSection() {
        ToggleGroup climateGroup = new ToggleGroup();
        radioProcClimate = new RadioButton(I18n.getOrDefault("resource.mode.procedural_climate", "▶ Procedural Climate"));
        radioImportClimate = new RadioButton(I18n.getOrDefault("resource.mode.import_climate", "📂 Climate Maps (PNG)"));
        radioProcClimate.setToggleGroup(climateGroup);
        radioImportClimate.setToggleGroup(climateGroup);
        radioProcClimate.setSelected(true);
        radioProcClimate.getStyleClass().add("radio-proc");
        radioImportClimate.getStyleClass().add("radio-import");

        climateSeedField = new TextField("34567");
        climateSeedField.textProperty().addListener((obs, old, val) -> {
            if (!host.isUpdatingFromPreset()) host.notifyParamChange();
            host.updatePreviewCanvas();
        });

        Button climateRandBtn = new Button("🎲");
        climateRandBtn.getStyleClass().add("button-secondary");
        climateRandBtn.setOnAction(e -> {
            climateSeedField.setText(String.valueOf(new Random().nextLong(1000000)));
            if (!host.isUpdatingFromPreset()) host.notifyParamChange();
            host.updatePreviewCanvas();
        });

        HBox climateSeedBox = new HBox(5, climateSeedField, climateRandBtn);
        HBox.setHgrow(climateSeedField, Priority.ALWAYS);

        climateSummaryLabel = new Label(I18n.getOrDefault("resource.climate.summary",
                "🌡️ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, O₂, CO₂, Albedo)"));
        climateSummaryLabel.getStyleClass().add("subcard-status-muted");
        climateSummaryLabel.setWrapText(true);

        VBox climateProcBox = new VBox(8,
                createControlRow(new Label(I18n.getOrDefault("resource.seed.label", "Generation Seed:")), climateSeedBox,
                        I18n.getOrDefault("resource.tooltip.seed_climate", "Random seed for procedural climate model")),
                climateSummaryLabel
        );
        climateProcBox.getStyleClass().add("subcard-procedural-box");

        climateSourceCombo = new ComboBox<>();
        climateSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon", "mercury");
        climateSourceCombo.setValue("none");
        org.ether.society.ui.util.DataSourceUIHelper.setupDetailedSourceCombo(
                climateSourceCombo, "common.combo.prompt_source", "planet.tooltip.climate_map");
        climateSourceCombo.setMaxWidth(Double.MAX_VALUE);
        climateSourceCombo.setOnAction(e -> {
            if (host.isUpdatingFromPreset()) return;
            String val = climateSourceCombo.getValue();
            if (val != null && !"none".equals(val)) {
                radioImportClimate.setSelected(true);
                fetchOnlineClimateData();
            }
        });

        climateSourcePromptLabel = new Label(I18n.getOrDefault("planet.climate.source_label", "Reference Source:"));

        // File controls
        climateMapRowLabel = new Label(I18n.getOrDefault("planet.map.climate", "Climate Map (Thermal / RGB):"));
        climateFileLabel = new Label(I18n.get("planet.map.none"));
        climateFileLabel.getStyleClass().add("value-label");
        loadClimateBtn = new Button(I18n.get("planet.map.btn_load"));
        loadClimateBtn.getStyleClass().add("button-secondary");
        loadClimateBtn.setOnAction(e -> chooseClimateMapFile());
        clearClimateBtn = new Button("❌");
        clearClimateBtn.getStyleClass().add("button-secondary");
        clearClimateBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_climate", "Do you really want to clear the imported climate map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.initOwner(host.getWindow());
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customClimateImage = null;
                    climateFileLabel.setText(I18n.get("planet.map.none"));
                    host.updatePreviewCanvas();
                    host.notifyParamChange();
                }
            });
        });
        HBox climateBox = new HBox(5, loadClimateBtn, clearClimateBtn);

        rainfallMapRowLabel = new Label(I18n.getOrDefault("planet.map.rainfall", "Precipitation Map (Humidity):"));
        rainfallFileLabel = new Label(I18n.get("planet.map.none"));
        rainfallFileLabel.getStyleClass().add("value-label");
        loadRainfallBtn = new Button(I18n.get("planet.map.btn_load"));
        loadRainfallBtn.getStyleClass().add("button-secondary");
        loadRainfallBtn.setOnAction(e -> chooseRainfallMapFile());
        clearRainfallBtn = new Button("❌");
        clearRainfallBtn.getStyleClass().add("button-secondary");
        clearRainfallBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_rainfall", "Do you really want to clear the imported precipitation map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.initOwner(host.getWindow());
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customRainfallImage = null;
                    rainfallFileLabel.setText(I18n.get("planet.map.none"));
                    host.updatePreviewCanvas();
                    host.notifyParamChange();
                }
            });
        });
        HBox rainfallBox = new HBox(5, loadRainfallBtn, clearRainfallBtn);

        seasonalityMapRowLabel = new Label(I18n.getOrDefault("planet.map.seasonality", "Seasonality / Variance Map:"));
        seasonalityFileLabel = new Label(I18n.get("planet.map.none"));
        seasonalityFileLabel.getStyleClass().add("value-label");
        loadSeasonalityBtn = new Button(I18n.get("planet.map.btn_load"));
        loadSeasonalityBtn.getStyleClass().add("button-secondary");
        loadSeasonalityBtn.setOnAction(e -> chooseSeasonalityMapFile());
        clearSeasonalityBtn = new Button("❌");
        clearSeasonalityBtn.getStyleClass().add("button-secondary");
        clearSeasonalityBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18n.getOrDefault("dialog.confirm.title", "Delete Confirmation"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.getOrDefault("resource.confirm.clear_seasonality", "Do you really want to clear the imported seasonality map?"));
            WindowUtils.applyWindowIcon(alert);
            alert.initOwner(host.getWindow());
            alert.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    customSeasonalityImage = null;
                    seasonalityFileLabel.setText(I18n.get("planet.map.none"));
                    host.updatePreviewCanvas();
                    host.notifyParamChange();
                }
            });
        });
        HBox seasonalityBox = new HBox(5, loadSeasonalityBtn, clearSeasonalityBtn);

        fetchOnlineClimateBtn = new Button(I18n.getOrDefault("planet.map.btn_fetch_online_climate", "🌐 Download Satellite Climate NASA/USGS (WMS)"));
        fetchOnlineClimateBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineClimateBtn.getStyleClass().add("button-secondary");
        fetchOnlineClimateBtn.setOnAction(e -> fetchOnlineClimateData());

        climateHelpBtn = new Button(I18n.getOrDefault("planet.map.btn_climate_help", "ℹ️ Climate Map Specifications"));
        climateHelpBtn.setMaxWidth(Double.MAX_VALUE);
        climateHelpBtn.getStyleClass().add("button-secondary");
        climateHelpBtn.setOnAction(e -> showClimateImportFormatHelp());

        climateStatusLabel = new Label();
        climateStatusLabel.getStyleClass().add("value-label");
        climateStatusLabel.setWrapText(true);

        VBox climateImportBox = new VBox(8,
                createControlRow(climateSourcePromptLabel, climateSourceCombo,
                        I18n.getOrDefault("planet.tooltip.climate_map", "Reference climate data source")),
                createControlRow(climateMapRowLabel, new VBox(3, climateBox, climateFileLabel),
                        I18n.getOrDefault("planet.tooltip.climate_map", "Thermal or combined RGB map import")),
                createControlRow(rainfallMapRowLabel, new VBox(3, rainfallBox, rainfallFileLabel),
                        I18n.getOrDefault("planet.tooltip.rainfall_map", "Precipitation / humidity map import")),
                createControlRow(seasonalityMapRowLabel, new VBox(3, seasonalityBox, seasonalityFileLabel),
                        I18n.getOrDefault("planet.tooltip.seasonality_map", "Import a seasonal variance map")),
                fetchOnlineClimateBtn,
                climateHelpBtn,
                climateStatusLabel
        );
        climateImportBox.getStyleClass().add("subcard-import-box");
        climateImportBox.setVisible(false);
        climateImportBox.setManaged(false);

        climateGroup.selectedToggleProperty().addListener((obs, old, sel) -> {
            boolean isProc = sel == radioProcClimate;
            climateProcBox.setVisible(isProc);
            climateProcBox.setManaged(isProc);
            climateImportBox.setVisible(!isProc);
            climateImportBox.setManaged(!isProc);
            if (!isProc && customClimateImage == null && climateSourceCombo.getValue() != null && !"none".equals(climateSourceCombo.getValue())) {
                fetchOnlineClimateData();
            }
            if (!host.isUpdatingFromPreset()) {
                host.notifyParamChange();
                host.updatePreviewCanvas();
            }
        });

        climateDomainSecHeader = new Label(I18n.getOrDefault("resource.domain.climate", "3. CLIMATE & ATMOSPHERE DOMAIN"));
        return createSection(climateDomainSecHeader, new VBox(8,
                radioProcClimate, climateProcBox, radioImportClimate, climateImportBox
        ));
    }

    public void chooseClimateMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.dialog.climate_load", "Load Climate / Thermal Map (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(host.getWindow());
        if (file != null) {
            try {
                customClimateImage = new Image(new FileInputStream(file));
                climateFileLabel.setText("🌡️ " + file.getName());
                host.updatePreviewCanvas();
                host.notifyParamChange();
            } catch (Exception ex) {
                logger.error("Failed to load climate map", ex);
            }
        }
    }

    public void chooseRainfallMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.dialog.rainfall_load", "Load Precipitation Map (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(host.getWindow());
        if (file != null) {
            try {
                customRainfallImage = new Image(new FileInputStream(file));
                rainfallFileLabel.setText("🌧️ " + file.getName());
                host.updatePreviewCanvas();
                host.notifyParamChange();
            } catch (Exception ex) {
                logger.error("Failed to load rainfall map", ex);
            }
        }
    }

    public void chooseSeasonalityMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.dialog.seasonality_load", "Load Seasonality Map (PNG/JPEG)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(host.getWindow());
        if (file != null) {
            try {
                customSeasonalityImage = new Image(new FileInputStream(file));
                seasonalityFileLabel.setText("🍂 " + file.getName());
                host.updatePreviewCanvas();
                host.notifyParamChange();
            } catch (Exception ex) {
                logger.error("Failed to load seasonality map", ex);
            }
        }
    }

    public void fetchOnlineClimateData() {
        String sourceKey = host.getMapSourceComboValue();
        if (sourceKey == null) sourceKey = "earth";
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };
        if (climateStatusLabel != null) {
            climateStatusLabel.setText(I18n.getOrDefault("planet.map.status_fetching", "WMS Download in Progress..."));
        }
        host.getOnlineMapService().fetchClimateMapAsync(body).thenAccept(img -> javafx.application.Platform.runLater(() -> {
            if (img != null) {
                customClimateImage = img;
                if (climateFileLabel != null) climateFileLabel.setText("🌐 " + body.getName() + " MODIS Thermal WMS");
                if (climateStatusLabel != null) climateStatusLabel.setText(I18n.get("planet.map.status_success"));
                host.updatePreviewCanvas();
                host.notifyParamChange();
            }
        }));
        host.getOnlineMapService().fetchRainfallMapAsync(body).thenAccept(img -> javafx.application.Platform.runLater(() -> {
            if (img != null) {
                customRainfallImage = img;
                if (rainfallFileLabel != null) rainfallFileLabel.setText("🌐 " + body.getName() + " GPM Rainfall WMS");
                host.updatePreviewCanvas();
                host.notifyParamChange();
            }
        }));
    }

    public void showClimateImportFormatHelp() {
        WindowUtils.showScrollableInfoDialog(
                I18n.getOrDefault("planet.dialog.climate_help_title", "Climate Map Specifications"),
                I18n.getOrDefault("planet.dialog.climate_help_header", "Supported Image Formats for Climate Data"),
                "Images PNG/JPEG in 2:1 ratio (e.g. 2048x1024 px, Equirectangular Plate Carree projection).\n\n" +
                "1. SEPARATE MAPS IN GRAYSCALE OR THERMAL GRADIENTS:\n" +
                "   • Temperature: Black / Blue = -50°C, White / Red = +50°C\n" +
                "   • Precipitation: Black = 0 mm/yr, White / Dark Blue = 3000 mm/yr\n" +
                "   • Seasonality: Black = 0°C, White / Magenta = 50°C annual amplitude\n\n" +
                "2. COMBINED MULTI-CHANNEL RGB MAP:\n" +
                "   • R (Red) = Temperature (-50°C to +50°C)\n" +
                "   • G (Green) = Precipitation (0 to 3000 mm/yr)\n" +
                "   • B (Blue) = Seasonality / Thermal amplitude (0 to 50°C)\n\n" +
                "3. WMS SATELLITE DOWNLOAD:\n" +
                "   • NASA MODIS (thermal) and NASA/JAXA GPM (precipitation) live layers."
        );
    }

    public String getClimateSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "🌡️ Mars Viking/MGS Thermal Inertia Model";
            case "moon" -> "🌡️ Moon Diviner Lunar Radiometer Experiment";
            case "venus" -> "🌡️ Venus Venera/VIRTIS Thermal Greenhouse Model";
            case "mercury" -> "🌡️ Mercury MESSENGER Thermal Radiometry";
            default -> "🌡️ Earth ERA5 / MODIS Land Surface Temp";
        };
    }

    public String getRainfallSourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "🌧️ Mars Atmospheric Water Vapor & Sublimation";
            case "moon" -> "🌧️ Moon Vacuum Exosphere (0 mm/yr Precipitation)";
            case "venus" -> "🌧️ Venus Sulfuric Acid Cloud Condensation & Virga";
            case "mercury" -> "🌧️ Mercury Exospheric Zero Precipitation";
            default -> "🌧️ Earth NASA GPM IMERG Precipitation";
        };
    }

    public String getSeasonalitySourceDisplayName(String planetKey) {
        if (planetKey == null) planetKey = "earth";
        return switch (planetKey.toLowerCase(Locale.ROOT)) {
            case "mars" -> "☀️ Mars Obliquity (25.2°) Thermal Seasonality";
            case "moon" -> "☀️ Moon Diurnal Day/Night Extreme Seasonality";
            case "venus" -> "☀️ Venus Slow Retrograde Thermal Inertia";
            case "mercury" -> "☀️ Mercury 3:2 Spin-Orbit Thermal Resonance";
            default -> "☀️ Earth Milankovitch Axial Obliquity Seasonality";
        };
    }

    public void applyPresetClimate(EcologyPreset p, String planetKey, long epochYear) {
        if (p != null && p.customClimateBase64() != null) {
            customClimateImage = ImageMapLoader.base64PngToImage(p.customClimateBase64());
            if (climateFileLabel != null) climateFileLabel.setText(I18n.getOrDefault("planet.preset.climate", "🌡️ Preset Climate"));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if ("earth".equals(planetKey)) {
            customClimateImage = ImageMapLoader.loadMapImage("earth", epochYear, "temperature");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (climateFileLabel != null) climateFileLabel.setText(getClimateSourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            customClimateImage = ImageMapLoader.loadMapImage(planetKey + "_temperature.png");
            if (climateFileLabel != null) climateFileLabel.setText(getClimateSourceDisplayName(planetKey));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else {
            customClimateImage = null;
            if (climateFileLabel != null) climateFileLabel.setText("—");
            if (radioProcClimate != null) radioProcClimate.setSelected(true);
        }

        if (p != null && p.customRainfallBase64() != null) {
            customRainfallImage = ImageMapLoader.base64PngToImage(p.customRainfallBase64());
            if (rainfallFileLabel != null) rainfallFileLabel.setText(I18n.getOrDefault("planet.preset.rainfall", "🌧️ Preset Rainfall"));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if ("earth".equals(planetKey)) {
            customRainfallImage = ImageMapLoader.loadMapImage("earth", epochYear, "precipitation");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (rainfallFileLabel != null) rainfallFileLabel.setText(getRainfallSourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            customRainfallImage = ImageMapLoader.loadMapImage(planetKey + "_precipitation.png");
            if (rainfallFileLabel != null) rainfallFileLabel.setText(getRainfallSourceDisplayName(planetKey));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else {
            customRainfallImage = null;
            if (rainfallFileLabel != null) rainfallFileLabel.setText("—");
        }

        if (p != null && p.customSeasonalityBase64() != null) {
            customSeasonalityImage = ImageMapLoader.base64PngToImage(p.customSeasonalityBase64());
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.getOrDefault("planet.preset.seasonality", "☀️ Preset Seasonality"));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if ("earth".equals(planetKey)) {
            customSeasonalityImage = ImageMapLoader.loadMapImage("earth", epochYear, "seasonality");
            String yearStr = (epochYear <= 0) ? Math.abs(epochYear) + " BP" : epochYear + " AD";
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(getSeasonalitySourceDisplayName("earth") + " (" + yearStr + ")");
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else if (planetKey != null && !"none".equals(planetKey)) {
            customSeasonalityImage = ImageMapLoader.loadMapImage(planetKey + "_seasonality.png");
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(getSeasonalitySourceDisplayName(planetKey));
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
        } else {
            customSeasonalityImage = null;
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText("—");
        }
    }

    public void applyPresetMapSource(String sourceKey, String body) {
        if ("none".equals(sourceKey) || sourceKey == null) {
            customClimateImage = null;
            customRainfallImage = null;
            customSeasonalityImage = null;
            if (climateFileLabel != null) climateFileLabel.setText(I18n.get("planet.map.none"));
            if (rainfallFileLabel != null) rainfallFileLabel.setText(I18n.get("planet.map.none"));
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.get("planet.map.none"));
            if (radioProcClimate != null) radioProcClimate.setSelected(true);
            if (climateSourceCombo != null) climateSourceCombo.setValue("none");
        } else {
            if (radioImportClimate != null) radioImportClimate.setSelected(true);
            if (climateSourceCombo != null) climateSourceCombo.setValue(body);
            customClimateImage = ImageMapLoader.loadMapImage(body + "_temperature.png");
            customRainfallImage = ImageMapLoader.loadMapImage(body + "_precipitation.png");
            customSeasonalityImage = ImageMapLoader.loadMapImage(body + "_seasonality.png");
            if (climateFileLabel != null) climateFileLabel.setText(getClimateSourceDisplayName(body));
            if (rainfallFileLabel != null) rainfallFileLabel.setText(getRainfallSourceDisplayName(body));
            if (seasonalityFileLabel != null) seasonalityFileLabel.setText(getSeasonalitySourceDisplayName(body));
        }
    }

    public void updateTexts() {
        if (climateDomainSecHeader != null) climateDomainSecHeader.setText(I18n.getOrDefault("resource.domain.climate", "3. CLIMATE & ATMOSPHERE DOMAIN"));
        if (radioProcClimate != null) radioProcClimate.setText(I18n.getOrDefault("resource.mode.procedural_climate", "▶ Procedural Climate"));
        if (radioImportClimate != null) radioImportClimate.setText(I18n.getOrDefault("resource.mode.import_climate", "📂 Climate Maps (PNG)"));
        if (climateMapRowLabel != null) climateMapRowLabel.setText(I18n.getOrDefault("planet.map.climate", "Climate Map (Thermal / RGB):"));
        if (rainfallMapRowLabel != null) rainfallMapRowLabel.setText(I18n.getOrDefault("planet.map.rainfall", "Precipitation Map (Humidity):"));
        if (seasonalityMapRowLabel != null) seasonalityMapRowLabel.setText(I18n.getOrDefault("planet.map.seasonality", "Seasonality / Variance Map:"));
        if (loadClimateBtn != null) loadClimateBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadRainfallBtn != null) loadRainfallBtn.setText(I18n.get("planet.map.btn_load"));
        if (loadSeasonalityBtn != null) loadSeasonalityBtn.setText(I18n.get("planet.map.btn_load"));
        if (customClimateImage == null && climateFileLabel != null) climateFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
        if (customRainfallImage == null && rainfallFileLabel != null) rainfallFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
        if (customSeasonalityImage == null && seasonalityFileLabel != null) seasonalityFileLabel.setText(I18n.getOrDefault("planet.map.none_file", "— No file loaded —"));
        if (fetchOnlineClimateBtn != null) fetchOnlineClimateBtn.setText(I18n.getOrDefault("planet.map.btn_fetch_online_climate", "🌐 Download Satellite Climate NASA/USGS (WMS)"));
        if (climateHelpBtn != null) climateHelpBtn.setText(I18n.getOrDefault("planet.map.btn_climate_help", "ℹ️ Climate Map Specifications"));
        if (climateSummaryLabel != null) climateSummaryLabel.setText(I18n.getOrDefault("resource.climate.summary", "🌡️ Climate Model: Inherited from Tab 1 planet parameters (Temperature, Gradient, Pressure, O₂, CO₂, Albedo)"));
        if (climateSourcePromptLabel != null) climateSourcePromptLabel.setText(I18n.getOrDefault("planet.climate.source_label", "Reference Source:"));
        if (climateSourceCombo != null && climateSourceCombo.getCellFactory() != null) {
            climateSourceCombo.setButtonCell(climateSourceCombo.getCellFactory().call(null));
        }
    }

    private VBox createSection(Label header, VBox content) {
        VBox card = new VBox(8, header, content);
        card.getStyleClass().add("card-section");
        card.setPadding(new Insets(10));
        return card;
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

    // Getters and Setters
    public Image getCustomClimateImage() { return customClimateImage; }
    public void setCustomClimateImage(Image img) { this.customClimateImage = img; }
    public Image getCustomRainfallImage() { return customRainfallImage; }
    public void setCustomRainfallImage(Image img) { this.customRainfallImage = img; }
    public Image getCustomSeasonalityImage() { return customSeasonalityImage; }
    public void setCustomSeasonalityImage(Image img) { this.customSeasonalityImage = img; }

    public boolean isRadioImportClimateSelected() { return radioImportClimate != null && radioImportClimate.isSelected(); }
    public void setRadioProcClimateSelected(boolean selected) { if (radioProcClimate != null) radioProcClimate.setSelected(selected); }
    public void setRadioImportClimateSelected(boolean selected) { if (radioImportClimate != null) radioImportClimate.setSelected(selected); }

    public TextField getClimateSeedField() { return climateSeedField; }
    public ComboBox<String> getClimateSourceCombo() { return climateSourceCombo; }
    public Label getClimateFileLabel() { return climateFileLabel; }
    public Label getRainfallFileLabel() { return rainfallFileLabel; }
    public Label getSeasonalityFileLabel() { return seasonalityFileLabel; }
    public Button getLoadClimateBtn() { return loadClimateBtn; }
    public Button getLoadRainfallBtn() { return loadRainfallBtn; }
    public Button getLoadSeasonalityBtn() { return loadSeasonalityBtn; }
}
