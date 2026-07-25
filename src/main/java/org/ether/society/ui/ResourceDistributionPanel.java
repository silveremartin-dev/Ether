/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * UI Panel for editing planet-wide ecological and resource distribution (flora, fauna, minerals, aquatic life).
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 */
public class ResourceDistributionPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ResourceDistributionPanel.class);

    private List<H3Cell> activeCells;
    private final Consumer<List<H3Cell>> onResourcesAppliedCallback;

    // Sliders
    private Slider woodDensitySlider;
    private Slider cropYieldSlider;
    private Slider gameFaunaSlider;
    private Slider livestockCapSlider;
    private Slider metalOresSlider;
    private Slider preciousOresSlider;
    private Slider stoneQualitySlider;
    private Slider fishAbundanceSlider;

    // Labels for i18n
    private Label headerLabel;
    private Label floraSecHeader;
    private Label faunaSecHeader;
    private Label mineralSecHeader;
    private Label aquaticSecHeader;

    private Label woodRowLabel;
    private Label cropRowLabel;
    private Label gameRowLabel;
    private Label livestockRowLabel;
    private Label metalsRowLabel;
    private Label preciousRowLabel;
    private Label stoneRowLabel;
    private Label fishRowLabel;

    private Button applyBtn;
    private Button saveJsonBtn;
    private Button loadJsonBtn;
    private Label summaryLabel;

    public record ResourceConfig(
            double woodDensityMultiplier,
            double cropYieldMultiplier,
            double gameFaunaMultiplier,
            double livestockCapacityMultiplier,
            double metalOresMultiplier,
            double preciousOresMultiplier,
            double stoneQualityMultiplier,
            double fishAbundanceMultiplier
    ) {}

    public ResourceDistributionPanel(Consumer<List<H3Cell>> onResourcesAppliedCallback) {
        this.onResourcesAppliedCallback = onResourcesAppliedCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void setActiveCells(List<H3Cell> cells) {
        this.activeCells = cells;
        updateSummary();
    }

    private void initUI() {
        VBox controlsBox = new VBox(15);
        controlsBox.setPrefWidth(420);
        controlsBox.setPadding(new Insets(10));

        headerLabel = new Label();
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        // 1. Flora & Plant Resources
        woodDensitySlider = createSlider(0.1, 5.0, 1.0);
        cropYieldSlider = createSlider(0.1, 5.0, 1.0);
        woodRowLabel = new Label();
        cropRowLabel = new Label();
        floraSecHeader = new Label();

        VBox floraSection = createSection(floraSecHeader, new VBox(8,
                createControlRow(woodRowLabel, woodDensitySlider, "%.2fx"),
                createControlRow(cropRowLabel, cropYieldSlider, "%.2fx")
        ));

        // 2. Fauna & Animal Resources
        gameFaunaSlider = createSlider(0.1, 5.0, 1.0);
        livestockCapSlider = createSlider(0.1, 5.0, 1.0);
        gameRowLabel = new Label();
        livestockRowLabel = new Label();
        faunaSecHeader = new Label();

        VBox faunaSection = createSection(faunaSecHeader, new VBox(8,
                createControlRow(gameRowLabel, gameFaunaSlider, "%.2fx"),
                createControlRow(livestockRowLabel, livestockCapSlider, "%.2fx")
        ));

        // 3. Minerals & Underground Deposits
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

        // 4. Aquatic & Marine Resources
        fishAbundanceSlider = createSlider(0.1, 5.0, 1.0);
        fishRowLabel = new Label();
        aquaticSecHeader = new Label();

        VBox aquaticSection = createSection(aquaticSecHeader, new VBox(8,
                createControlRow(fishRowLabel, fishAbundanceSlider, "%.2fx")
        ));

        // JSON Actions
        saveJsonBtn = new Button();
        saveJsonBtn.setMaxWidth(Double.MAX_VALUE);
        saveJsonBtn.setStyle("-fx-font-size: 11px; -fx-base: #475569;");
        saveJsonBtn.setOnAction(e -> saveJsonConfig());

        loadJsonBtn = new Button();
        loadJsonBtn.setMaxWidth(Double.MAX_VALUE);
        loadJsonBtn.setStyle("-fx-font-size: 11px; -fx-base: #475569;");
        loadJsonBtn.setOnAction(e -> loadJsonConfig());

        HBox jsonBox = new HBox(8, saveJsonBtn, loadJsonBtn);
        HBox.setHgrow(saveJsonBtn, Priority.ALWAYS);
        HBox.setHgrow(loadJsonBtn, Priority.ALWAYS);

        controlsBox.getChildren().addAll(
                headerLabel, floraSection, faunaSection, mineralSection, aquaticSection, jsonBox
        );

        ScrollPane scrollControls = new ScrollPane(controlsBox);
        scrollControls.setFitToWidth(true);
        scrollControls.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // Center Pane
        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(20));

        summaryLabel = new Label("No planet active. Generate a planet in Tab 1 first.");
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        applyBtn = new Button();
        applyBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 24; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        applyBtn.setOnAction(e -> applyResourceDistribution());

        centerBox.getChildren().addAll(summaryLabel, applyBtn);

        setLeft(scrollControls);
        setCenter(centerBox);
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

    private ResourceConfig buildConfigFromUI() {
        return new ResourceConfig(
                woodDensitySlider.getValue(),
                cropYieldSlider.getValue(),
                gameFaunaSlider.getValue(),
                livestockCapSlider.getValue(),
                metalOresSlider.getValue(),
                preciousOresSlider.getValue(),
                stoneQualitySlider.getValue(),
                fishAbundanceSlider.getValue()
        );
    }

    private void updateSummary() {
        if (activeCells == null || activeCells.isEmpty()) {
            summaryLabel.setText("No active planet cells loaded. Generate a planet in Tab 1.");
            return;
        }

        ResourceConfig c = buildConfigFromUI();
        summaryLabel.setText(String.format(
                "🌍 Active Cells: %,d\n\n" +
                "🌲 Wood Multiplier: %.2fx\n" +
                "🌾 Crop Yield Multiplier: %.2fx\n" +
                "🦌 Game Fauna Multiplier: %.2fx\n" +
                "⛏️ Metals Multiplier: %.2fx\n" +
                "🐟 Marine Fish Multiplier: %.2fx",
                activeCells.size(),
                c.woodDensityMultiplier(),
                c.cropYieldMultiplier(),
                c.gameFaunaMultiplier(),
                c.metalOresMultiplier(),
                c.fishAbundanceMultiplier()
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
        if (saveJsonBtn != null) saveJsonBtn.setText(I18n.get("resource.btn.save_json"));
        if (loadJsonBtn != null) loadJsonBtn.setText(I18n.get("resource.btn.load_json"));

        updateSummary();
    }

    private void applyResourceDistribution() {
        if (activeCells == null || activeCells.isEmpty()) return;

        ResourceConfig c = buildConfigFromUI();
        activeCells.parallelStream().forEach(cell -> {
            Biome b = cell.getBiome();
            if (b == null) return;

            switch (b) {
                case FOREST, JUNGLE -> cell.setWoodResource(1000.0 * c.woodDensityMultiplier());
                case PLAINS -> cell.setFoodResource(500.0 * c.cropYieldMultiplier());
                case MOUNTAINS -> cell.setResourceMetal(500.0 * c.metalOresMultiplier());
                case OCEAN, DEEP_OCEAN -> {
                    cell.setBiomassFish(800.0 * c.fishAbundanceMultiplier());
                    cell.setFoodResource(200.0 * c.fishAbundanceMultiplier());
                }
                default -> {}
            }
        });

        logger.info("Applied ecological resource distribution to {} cells", activeCells.size());
        if (onResourcesAppliedCallback != null) {
            onResourcesAppliedCallback.accept(activeCells);
        }
    }

    private void saveJsonConfig() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Ecology Configuration JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(file, buildConfigFromUI());
                logger.info("Saved ecology configuration JSON to {}", file.getAbsolutePath());
            } catch (IOException ex) {
                logger.error("Failed to save ecology JSON", ex);
            }
        }
    }

    private void loadJsonConfig() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Ecology Configuration JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null && file.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ResourceConfig c = mapper.readValue(file, ResourceConfig.class);
                woodDensitySlider.setValue(c.woodDensityMultiplier());
                cropYieldSlider.setValue(c.cropYieldMultiplier());
                gameFaunaSlider.setValue(c.gameFaunaMultiplier());
                livestockCapSlider.setValue(c.livestockCapacityMultiplier());
                metalOresSlider.setValue(c.metalOresMultiplier());
                preciousOresSlider.setValue(c.preciousOresMultiplier());
                stoneQualitySlider.setValue(c.stoneQualityMultiplier());
                fishAbundanceSlider.setValue(c.fishAbundanceMultiplier());
                logger.info("Loaded ecology configuration JSON from {}", file.getAbsolutePath());
            } catch (IOException ex) {
                logger.error("Failed to load ecology JSON", ex);
            }
        }
    }
}
