/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.planet;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.util.DataSourceUIHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Random;

/**
 * Autonomous sub-block controller for climate raster maps (temperature, precipitation, seasonality).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PlanetClimateSubBlock {

    private static final Logger logger = LoggerFactory.getLogger(PlanetClimateSubBlock.class);

    private final String mapType;
    private final int viewMode;
    private final String accentRgba;
    private final String defaultSeedStr;
    private final long defaultSeedOffset;
    private final String iconPrefix;
    private final List<String> sourceItems;
    private final PlanetClimateSectionBuilder.HostContext context;

    private RadioButton radioProc;
    private RadioButton radioImport;
    private TextField seedField;
    private Label seedLabel;
    private Label hintLabel;
    private Button exportBtn;
    private ComboBox<String> sourceCombo;
    private Label fileLabel;
    private Button loadBtn;
    private Button clearBtn;
    private Label formatHintLabel;
    private Label sourceLabel;
    private Label subHeader;
    private VBox procPanel;
    private VBox importPanel;
    private VBox rootBlock;

    private Image customImage;
    private String cachedB64;

    public PlanetClimateSubBlock(
            String mapType,
            int viewMode,
            String accentRgba,
            String defaultSeedStr,
            long defaultSeedOffset,
            String iconPrefix,
            List<String> sourceItems,
            PlanetClimateSectionBuilder.HostContext context) {
        this.mapType = mapType;
        this.viewMode = viewMode;
        this.accentRgba = accentRgba;
        this.defaultSeedStr = defaultSeedStr;
        this.defaultSeedOffset = defaultSeedOffset;
        this.iconPrefix = iconPrefix;
        this.sourceItems = sourceItems;
        this.context = context;
        initUI();
    }

    private void initUI() {
        ToggleGroup toggle = new ToggleGroup();
        radioProc = new RadioButton(I18n.getOrDefault("planet.radio.procedural", "â–¶ Procedural Generation (Perlin Noise)"));
        radioImport = new RadioButton(I18n.getOrDefault("planet.radio.import_wms", "ðŸ“‚ External Source (PNG / WMS)"));
        radioProc.setToggleGroup(toggle);
        radioProc.setSelected(true);
        radioProc.getStyleClass().add("radio-proc");
        radioImport.setToggleGroup(toggle);
        radioImport.getStyleClass().add("radio-import");

        seedField = new TextField(defaultSeedStr);
        seedField.textProperty().addListener((obs, old, val) -> {
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
        });
        Button randBtn = new Button("ðŸŽ²");
        randBtn.getStyleClass().add("button-secondary");
        randBtn.setOnAction(e -> {
            seedField.setText(String.valueOf(new Random().nextLong(1_000_000)));
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
        });
        HBox seedBox = new HBox(5, seedField, randBtn);
        HBox.setHgrow(seedField, Priority.ALWAYS);

        seedLabel = new Label(I18n.getOrDefault("planet.climate.seed_label", "Generation Seed:"));
        hintLabel = new Label(I18n.getOrDefault("planet.climate." + mapType + ".hint", ""));
        hintLabel.setWrapText(true);
        hintLabel.getStyleClass().add("card-description-muted");

        exportBtn = new Button(I18n.getOrDefault("planet.btn.export_climate_" + mapType, "ðŸ“¤ Export Procedural Map (PNG)"));
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.getStyleClass().add("button-secondary");
        exportBtn.setOnAction(e -> context.exportProceduralClimateMap(mapType));

        procPanel = new VBox(6, seedLabel, seedBox, hintLabel, exportBtn);
        procPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(" + accentRgba + ", 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");

        sourceCombo = new ComboBox<>();
        sourceCombo.setMaxWidth(Double.MAX_VALUE);
        sourceCombo.getItems().add("");
        sourceCombo.getItems().addAll(sourceItems);
        DataSourceUIHelper.setupDetailedSourceCombo(sourceCombo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        sourceCombo.setValue("");
        sourceCombo.setOnAction(e -> {
            if (context.isUpdatingFromPreset()) return;
            String val = sourceCombo.getValue();
            if (val != null && !val.isEmpty()) {
                if (val.contains("WMS")) context.fetchOnlineClimateData();
                else applySourceSelection(val);
            }
        });

        fileLabel = new Label("â€”");
        fileLabel.getStyleClass().add("value-label");
        loadBtn = new Button(I18n.get("planet.map.btn_load"));
        loadBtn.getStyleClass().add("button-secondary");
        loadBtn.setOnAction(e -> chooseMapFile());
        clearBtn = new Button("âŒ");
        clearBtn.getStyleClass().add("button-secondary");
        clearBtn.setOnAction(e -> {
            customImage = null;
            cachedB64 = null;
            fileLabel.setText("â€”");
            if (!context.isUpdatingFromPreset()) context.notifyParamChange();
            context.updatePreview();
        });
        HBox importBtns = new HBox(5, loadBtn, clearBtn);

        formatHintLabel = new Label(I18n.getOrDefault("planet.climate." + mapType + ".format", ""));
        formatHintLabel.setWrapText(true);
        formatHintLabel.getStyleClass().add("card-description-muted");
        sourceLabel = new Label(I18n.getOrDefault("planet.climate.source_label", "Reference Source:"));
        importPanel = new VBox(6, sourceLabel, sourceCombo, importBtns, fileLabel, formatHintLabel);
        importPanel.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(" + accentRgba + ", 0.3); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
        importPanel.setVisible(false);
        importPanel.setManaged(false);

        toggle.selectedToggleProperty().addListener((obs, o, sel) -> {
            boolean isProc = sel == radioProc;
            procPanel.setVisible(isProc);
            procPanel.setManaged(isProc);
            importPanel.setVisible(!isProc);
            importPanel.setManaged(!isProc);
            if (!context.isUpdatingFromPreset()) {
                context.selectViewMode(viewMode);
                context.notifyParamChange();
                context.updatePreview();
            }
        });
        radioProc.setOnAction(e -> { context.selectViewMode(viewMode); context.updatePreview(); });
        radioImport.setOnAction(e -> { context.selectViewMode(viewMode); context.updatePreview(); });

        subHeader = new Label(I18n.getOrDefault("planet.climate." + mapType + ".header", ""));
        subHeader.getStyleClass().add("control-label");
        rootBlock = new VBox(6, subHeader, radioProc, procPanel, radioImport, importPanel);
        rootBlock.setStyle("-fx-padding: 10 10 10 10; -fx-background-color: rgba(" + accentRgba + ", 0.08); -fx-background-radius: 6; -fx-border-color: rgba(" + accentRgba + ", 0.25); -fx-border-radius: 6;");
    }

    public VBox getRootBlock() { return rootBlock; }

    public void applySourceSelection(String selectedSource) {
        if (selectedSource == null || selectedSource.isBlank() || context.isUpdatingFromPreset()) return;
        String key = selectedSource.toLowerCase();
        String file = resolveMapFileName(key);
        customImage = ImageMapLoader.loadMapImage(file);
        cachedB64 = null;
        if (radioImport != null) radioImport.setSelected(true);
        if (fileLabel != null) fileLabel.setText(iconPrefix + " " + selectedSource);
        context.selectViewMode(viewMode);
        context.notifyParamChange();
        context.updatePreview();
    }

    private String resolveMapFileName(String key) {
        if (key.contains("mars") || key.contains("tes") || key.contains("mgs")) {
            return switch (mapType) {
                case "temp" -> "mars_temperature.png";
                case "precip" -> "mars_precipitation.png";
                default -> "mars_seasonality.png";
            };
        } else if (key.contains("vÃ©nus") || key.contains("venus") || key.contains("magellan")) {
            return switch (mapType) {
                case "temp" -> "venus_temperature.png";
                case "precip" -> "venus_precipitation.png";
                default -> "venus_seasonality.png";
            };
        } else if (key.contains("lune") || key.contains("moon") || key.contains("lro") || key.contains("diviner") || key.contains("lend")) {
            return switch (mapType) {
                case "temp" -> "moon_temperature.png";
                case "precip" -> "moon_precipitation.png";
                default -> "moon_seasonality.png";
            };
        } else if (key.contains("mercure") || key.contains("mercury") || key.contains("messenger")) {
            return switch (mapType) {
                case "temp" -> "mercury_temperature.png";
                case "precip" -> "mercury_precipitation.png";
                default -> "mercury_seasonality.png";
            };
        } else {
            return switch (mapType) {
                case "temp" -> "earth_temperature.png";
                case "precip" -> "earth_precipitation.png";
                default -> "earth_seasonality.png";
            };
        }
    }

    private void chooseMapFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("planet.chooser." + mapType, "Load map image"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(context.getWindow());
        if (file != null) {
            try {
                customImage = new Image(new FileInputStream(file));
                cachedB64 = null;
                fileLabel.setText(iconPrefix + " " + file.getName());
                setLocalFileInCombo(file.getName());
                context.selectViewMode(viewMode);
                context.notifyParamChange();
                context.updatePreview();
            } catch (Exception ex) {
                logger.error("Failed to load map image for " + mapType, ex);
            }
        }
    }

    public void setLocalFileInCombo(String fileName) {
        String prefix = "ðŸ“Œ Fichier local : ";
        sourceCombo.getItems().removeIf(item -> item != null && item.startsWith(prefix));
        String entry = prefix + fileName;
        sourceCombo.getItems().add(entry);
        sourceCombo.setValue(entry);
    }

    public void applyPreset(String source, String base64, boolean useImport) {
        if (base64 != null) {
            customImage = ImageMapLoader.base64PngToImage(base64);
            cachedB64 = base64;
            if (fileLabel != null) fileLabel.setText(I18n.getOrDefault("planet.preset." + mapType, iconPrefix + " Preset"));
        }
        if (sourceCombo != null) {
            if (source != null && !source.isEmpty()) {
                if (!sourceCombo.getItems().contains(source)) sourceCombo.getItems().add(source);
                sourceCombo.setValue(source);
            } else if (base64 != null) {
                setLocalFileInCombo("Preset " + mapType + " Map");
            } else if (!sourceCombo.getItems().isEmpty()) {
                sourceCombo.setValue(sourceCombo.getItems().get(0));
            }
        }
        if (useImport || base64 != null) {
            radioImport.setSelected(true);
        } else {
            radioProc.setSelected(true);
        }
    }

    public void updateTexts() {
        if (radioProc != null) radioProc.setText(I18n.getOrDefault("planet.radio.procedural", "â–¶ Procedural Generation (Perlin Noise)"));
        if (radioImport != null) radioImport.setText(I18n.getOrDefault("planet.radio.import_wms", "ðŸ“‚ Source Externe (PNG / WMS)"));
        if (subHeader != null) subHeader.setText(I18n.getOrDefault("planet.climate." + mapType + ".header", ""));
        if (seedLabel != null) seedLabel.setText(I18n.getOrDefault("planet.climate.seed_label", "Generation seed:"));
        if (sourceLabel != null) sourceLabel.setText(I18n.getOrDefault("planet.climate.source_label", "Reference source:"));
        if (hintLabel != null) hintLabel.setText(I18n.getOrDefault("planet.climate." + mapType + ".hint", ""));
        if (formatHintLabel != null) formatHintLabel.setText(I18n.getOrDefault("planet.climate." + mapType + ".format", ""));
        if (loadBtn != null) loadBtn.setText(I18n.get("planet.map.btn_load"));
        if (sourceCombo != null && sourceCombo.getCellFactory() != null) sourceCombo.setButtonCell(sourceCombo.getCellFactory().call(null));
    }

    public boolean isImportActive() { return radioImport != null && radioImport.isSelected(); }
    public String getSource() { return sourceCombo != null && sourceCombo.getValue() != null ? sourceCombo.getValue() : ""; }
    public long getSeed(long defaultSeed) {
        try {
            if (seedField != null && seedField.getText() != null && !seedField.getText().isEmpty()) {
                return Long.parseLong(seedField.getText());
            }
        } catch (NumberFormatException ignored) { org.slf4j.LoggerFactory.getLogger(PlanetClimateSubBlock.class).debug("Non-fatal condition in PlanetClimateSubBlock: {}", ignored.getMessage()); }
        return defaultSeed + defaultSeedOffset;
    }

    public Image getCustomImage() { return customImage; }
    public void setCustomImage(Image img) { this.customImage = img; }
    public String getCachedBase64() { return cachedB64; }
    public void setCachedBase64(String b64) { this.cachedB64 = b64; }

    public RadioButton getRadioImport() { return radioImport; }
    public Button getLoadBtn() { return loadBtn; }
    public Label getFileLabel() { return fileLabel; }
    public ComboBox<String> getSourceCombo() { return sourceCombo; }
}

