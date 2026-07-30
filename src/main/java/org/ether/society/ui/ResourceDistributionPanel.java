/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.model.EcologyPreset;
import org.ether.society.procedural.PlanetPreset;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * UI Panel for editing planet-wide ecological and resource distribution (flora, fauna, minerals, aquatic life).
 * Includes custom map image import for ecology layers and standardized PresetBar.
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.1.0
 */
public class ResourceDistributionPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ResourceDistributionPanel.class);

    private List<H3Cell> activeCells;
    private PlanetPreset activePlanetPreset;
    private final Consumer<List<H3Cell>> onResourcesAppliedCallback;
    private final ImageMapLoader mapLoader = new ImageMapLoader();

    // Custom Ecology Image Maps
    private Image customBiomeImage;
    private Image customResourceImage;

    // UI Controls
    private Label planetContextLabel;
    private PresetControlBar<EcologyPreset> ecologyPresetBar;

    private Slider woodDensitySlider;
    private Slider cropYieldSlider;
    private Slider gameFaunaSlider;
    private Slider livestockCapSlider;
    private Slider metalOresSlider;
    private Slider preciousOresSlider;
    private Slider stoneQualitySlider;
    private Slider fishAbundanceSlider;

    // Custom Map Labels
    private Label biomeFileLabel;
    private Label resourceFileLabel;

    private Label headerLabel;
    private Label floraSecHeader;
    private Label faunaSecHeader;
    private Label mineralSecHeader;
    private Label aquaticSecHeader;
    private Label mapsSecHeader;

    private Label woodRowLabel;
    private Label cropRowLabel;
    private Label gameRowLabel;
    private Label livestockRowLabel;
    private Label metalsRowLabel;
    private Label preciousRowLabel;
    private Label stoneRowLabel;
    private Label fishRowLabel;

    private Button applyBtn;
    private Label summaryLabel;

    public ResourceDistributionPanel(Consumer<List<H3Cell>> onResourcesAppliedCallback) {
        this.onResourcesAppliedCallback = onResourcesAppliedCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void setActivePlanetPreset(PlanetPreset planetPreset) {
        this.activePlanetPreset = planetPreset;
        if (planetContextLabel != null) {
            String planetName = planetPreset != null ? planetPreset.name() : "Standard Earth-Like";
            planetContextLabel.setText("🪐 Territoire & Terrain hérité : " + planetName);
        }
    }

    public void setActiveCells(List<H3Cell> cells) {
        this.activeCells = cells;
        updateSummary();
    }

    private void initUI() {
        VBox controlsBox = new VBox(15);
        controlsBox.setPrefWidth(440);
        controlsBox.setPadding(new Insets(10));

        headerLabel = new Label();
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        planetContextLabel = new Label("🪐 Territoire & Terrain hérité : Terrestre");
        planetContextLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 4 8; -fx-background-color: rgba(56, 189, 248, 0.1); -fx-background-radius: 4;");

        // 1. Standardized Preset Control Bar
        ecologyPresetBar = new PresetControlBar<>("Preset Écologie");
        ecologyPresetBar.setPresets(EcologyPreset.getBuiltInPresets(), EcologyPreset.getBuiltInPresets().get(0));
        ecologyPresetBar.setListener(new PresetControlBar.PresetActionsListener<EcologyPreset>() {
            @Override
            public void onPresetSelected(EcologyPreset preset) {
                applyEcologyPreset(preset);
            }

            @Override
            public void onSavePreset(String name) {
                EcologyPreset custom = new EcologyPreset(
                        name,
                        woodDensitySlider.getValue(),
                        cropYieldSlider.getValue(),
                        gameFaunaSlider.getValue(),
                        livestockCapSlider.getValue(),
                        metalOresSlider.getValue(),
                        preciousOresSlider.getValue(),
                        stoneQualitySlider.getValue(),
                        fishAbundanceSlider.getValue()
                );
                ecologyPresetBar.getPresetCombo().getItems().add(custom);
                ecologyPresetBar.getPresetCombo().setValue(custom);
            }

            @Override
            public void onDeletePreset(EcologyPreset preset) {
                ecologyPresetBar.getPresetCombo().getItems().remove(preset);
            }

            @Override
            public void onExportPreset(File targetFile, EcologyPreset preset) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, preset);
                    logger.info("Exported ecology preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export ecology preset", ex);
                }
            }

            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    EcologyPreset preset = mapper.readValue(sourceFile, EcologyPreset.class);
                    ecologyPresetBar.getPresetCombo().getItems().add(preset);
                    ecologyPresetBar.getPresetCombo().setValue(preset);
                    applyEcologyPreset(preset);
                    logger.info("Imported ecology preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import ecology preset", ex);
                }
            }
        });

        // 2. Custom Maps Import Section (Biomes & Ores Image Maps)
        mapsSecHeader = new Label("CARTES D'ÉCOLOGIE PERSONNALISÉES (PNG)");
        biomeFileLabel = new Label("Aucune carte de biome");
        biomeFileLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        Button loadBiomeBtn = new Button("📷 Charger Biomes");
        loadBiomeBtn.setOnAction(e -> loadCustomBiomeMap());

        resourceFileLabel = new Label("Aucune carte de minerais");
        resourceFileLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        Button loadResourceBtn = new Button("🪨 Charger Minerais");
        loadResourceBtn.setOnAction(e -> loadCustomResourceMap());

        VBox customMapsSection = createSection(mapsSecHeader, new VBox(6,
                new HBox(8, loadBiomeBtn, biomeFileLabel),
                new HBox(8, loadResourceBtn, resourceFileLabel)
        ));

        // 3. Flora & Plant Resources
        woodDensitySlider = createSlider(0.1, 5.0, 1.0);
        cropYieldSlider = createSlider(0.1, 5.0, 1.0);
        woodRowLabel = new Label();
        cropRowLabel = new Label();
        floraSecHeader = new Label();

        VBox floraSection = createSection(floraSecHeader, new VBox(8,
                createControlRow(woodRowLabel, woodDensitySlider, "%.2fx"),
                createControlRow(cropRowLabel, cropYieldSlider, "%.2fx")
        ));

        // 4. Fauna & Animal Resources
        gameFaunaSlider = createSlider(0.1, 5.0, 1.0);
        livestockCapSlider = createSlider(0.1, 5.0, 1.0);
        gameRowLabel = new Label();
        livestockRowLabel = new Label();
        faunaSecHeader = new Label();

        VBox faunaSection = createSection(faunaSecHeader, new VBox(8,
                createControlRow(gameRowLabel, gameFaunaSlider, "%.2fx"),
                createControlRow(livestockRowLabel, livestockCapSlider, "%.2fx")
        ));

        // 5. Minerals & Underground Deposits
        metalOresSlider = createSlider(0.1, 5.0, 1.0);
        preciousOresSlider = createSlider(0.1, 5.0, 1.0);
        stoneQualitySlider = createSlider(0.1, 5.0, 1.0);
        metalsRowLabel = new Label();
        preciousRowLabel = new Label();
        stoneRowLabel = new Label();
        mineralSecHeader = new Label();

        VBox mineralSection = createSection(mineralSecHeader, new VBox(8,
                createControlRow(metalsRowLabel, metalOresSlider, "%.2fx"),
                createControlRow(preciousRowLabel, preciousOresSlider, "%.2fx"),
                createControlRow(stoneRowLabel, stoneQualitySlider, "%.2fx")
        ));

        // 6. Aquatic & Marine Resources
        fishAbundanceSlider = createSlider(0.1, 5.0, 1.0);
        fishRowLabel = new Label();
        aquaticSecHeader = new Label();

        VBox aquaticSection = createSection(aquaticSecHeader, new VBox(8,
                createControlRow(fishRowLabel, fishAbundanceSlider, "%.2fx")
        ));

        controlsBox.getChildren().addAll(
                headerLabel, planetContextLabel, ecologyPresetBar, customMapsSection, floraSection, faunaSection, mineralSection, aquaticSection
        );

        ScrollPane scrollControls = new ScrollPane(controlsBox);
        scrollControls.setFitToWidth(true);
        scrollControls.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Center Pane
        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(20));

        summaryLabel = new Label("Aucune planète active. Générez une planète dans l'Onglet 1.");
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        applyBtn = new Button();
        applyBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 24; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        applyBtn.setOnAction(e -> applyResourceDistribution());

        centerBox.getChildren().addAll(summaryLabel, applyBtn);

        setLeft(scrollControls);
        setCenter(centerBox);
    }

    private void applyEcologyPreset(EcologyPreset p) {
        if (p == null) return;
        woodDensitySlider.setValue(p.woodDensityMultiplier());
        cropYieldSlider.setValue(p.cropYieldMultiplier());
        gameFaunaSlider.setValue(p.gameFaunaMultiplier());
        livestockCapSlider.setValue(p.livestockCapacityMultiplier());
        metalOresSlider.setValue(p.metalOresMultiplier());
        preciousOresSlider.setValue(p.preciousOresMultiplier());
        stoneQualitySlider.setValue(p.stoneQualityMultiplier());
        fishAbundanceSlider.setValue(p.fishAbundanceMultiplier());
        updateSummary();
    }

    private void loadCustomBiomeMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Charger une carte de biomes (PNG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG", "*.png", "*.jpg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customBiomeImage = new Image(new FileInputStream(file));
                biomeFileLabel.setText("📷 " + file.getName());
                updateSummary();
            } catch (Exception ex) {
                logger.error("Failed to load custom biome map", ex);
            }
        }
    }

    private void loadCustomResourceMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Charger une carte de minerais/ressources (PNG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG", "*.png", "*.jpg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customResourceImage = new Image(new FileInputStream(file));
                resourceFileLabel.setText("🪨 " + file.getName());
                updateSummary();
            } catch (Exception ex) {
                logger.error("Failed to load custom resource map", ex);
            }
        }
    }

    private VBox createSection(Label header, VBox content) {
        header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #34d399;");
        VBox box = new VBox(8, header, content);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.03); -fx-background-radius: 8; -fx-padding: 10;");
        return box;
    }

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.valueProperty().addListener((obs, old, val) -> updateSummary());
        return slider;
    }

    private VBox createControlRow(Label label, Slider slider, String formatPattern) {
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1;");
        Label valLabel = new Label(String.format(formatPattern, slider.getValue()));
        valLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #34d399;");
        slider.valueProperty().addListener((obs, old, val) ->
                valLabel.setText(String.format(formatPattern, val.doubleValue()))
        );

        HBox header = new HBox(label, new Pane(), valLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
    }

    private void updateSummary() {
        if (activeCells == null || activeCells.isEmpty()) {
            summaryLabel.setText("Aucune cellule active. Générez une planète dans l'Onglet 1.");
            return;
        }

        summaryLabel.setText(String.format(
                "🌍 Cellules planétaires actives : %,d\n\n" +
                "🌲 Densité du bois : %.2fx\n" +
                "🌾 Rendement agricole : %.2fx\n" +
                "🦌 Faune de chasse : %.2fx\n" +
                "⛏️ Minerais métalliques : %.2fx\n" +
                "🐟 Poissonnerie marine : %.2fx\n\n" +
                "📷 Carte Biome custom : %s\n" +
                "🪨 Carte Minerais custom : %s",
                activeCells.size(),
                woodDensitySlider.getValue(),
                cropYieldSlider.getValue(),
                gameFaunaSlider.getValue(),
                metalOresSlider.getValue(),
                fishAbundanceSlider.getValue(),
                customBiomeImage != null ? "Chargée" : "Aucune",
                customResourceImage != null ? "Chargée" : "Aucune"
        ));
    }

    public void updateTexts() {
        if (headerLabel != null) headerLabel.setText(I18n.get("resource.title"));
        if (floraSecHeader != null) floraSecHeader.setText(I18n.get("resource.section.flora"));
        if (faunaSecHeader != null) faunaSecHeader.setText(I18n.get("resource.section.fauna"));
        if (mineralSecHeader != null) mineralSecHeader.setText(I18n.get("resource.section.minerals"));
        if (aquaticSecHeader != null) aquaticSecHeader.setText(I18n.get("resource.section.aquatic"));

        if (woodRowLabel != null) woodRowLabel.setText(I18n.get("resource.param.wood"));
        if (cropRowLabel != null) cropRowLabel.setText(I18n.get("resource.param.food_crop"));
        if (gameRowLabel != null) gameRowLabel.setText(I18n.get("resource.param.game_animal"));
        if (livestockRowLabel != null) livestockRowLabel.setText(I18n.get("resource.param.livestock"));
        if (metalsRowLabel != null) metalsRowLabel.setText(I18n.get("resource.param.metals"));
        if (preciousRowLabel != null) preciousRowLabel.setText(I18n.get("resource.param.precious"));
        if (stoneRowLabel != null) stoneRowLabel.setText(I18n.get("resource.param.stone"));
        if (fishRowLabel != null) fishRowLabel.setText(I18n.get("resource.param.fish"));

        if (applyBtn != null) applyBtn.setText(I18n.get("resource.btn.apply"));

        updateSummary();
    }

    private void applyResourceDistribution() {
        if (activeCells == null || activeCells.isEmpty()) return;

        double wMult = woodDensitySlider.getValue();
        double cMult = cropYieldSlider.getValue();
        double mMult = metalOresSlider.getValue();
        double fMult = fishAbundanceSlider.getValue();

        activeCells.parallelStream().forEach(cell -> {
            Biome b = cell.getBiome();
            if (b == null) return;

            switch (b) {
                case FOREST, JUNGLE -> cell.setWoodResource(1000.0 * wMult);
                case PLAINS -> cell.setFoodResource(500.0 * cMult);
                case MOUNTAINS -> cell.setResourceMetal(500.0 * mMult);
                case OCEAN, DEEP_OCEAN -> {
                    cell.setBiomassFish(800.0 * fMult);
                    cell.setFoodResource(200.0 * fMult);
                }
                default -> {}
            }
        });

        // Apply custom map images if loaded
        if (customBiomeImage != null || customResourceImage != null) {
            mapLoader.mapImagesToCells(activeCells, null, customBiomeImage, customResourceImage, -11000, 8848);
        }

        logger.info("Applied ecological resource distribution to {} cells", activeCells.size());
        if (onResourcesAppliedCallback != null) {
            onResourcesAppliedCallback.accept(activeCells);
        }
    }
}
