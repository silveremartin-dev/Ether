package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;
import org.ether.society.procedural.ProceduralGenerator.PlanetPoint;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

/**
 * Dialog for generating procedural planets.
 */
public class PlanetGeneratorDialog extends Stage {


    private final ProceduralGenerator generator;

    // UI Controls
    private TextField seedField;
    private Slider noiseFreqSlider;
    private Slider noiseScaleSlider;
    private Slider waterSlider;
    private Slider tempSlider;
    private ComboBox<PlanetPreset> presetCombo;

    // Preview
    private Canvas previewCanvas;
    private Label statsLabel;

    // Result
    private List<H3Cell> generatedCells = null;
    private PlanetPreset finalPreset = null;

    public PlanetGeneratorDialog() {
        this.generator = new ProceduralGenerator();

        initModality(Modality.APPLICATION_MODAL);
        setTitle("Planet Generator");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Left: Controls
        VBox controls = new VBox(15);
        controls.setPrefWidth(250);
        controls.setPadding(new Insets(0, 20, 0, 0));

        // Preset Selection
        presetCombo = new ComboBox<>();
        presetCombo.getItems().addAll(
                PlanetPreset.EARTH_LIKE,
                PlanetPreset.WATER_WORLD,
                PlanetPreset.DESERT_WORLD,
                PlanetPreset.ICE_WORLD);
        presetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name());
            }
        });
        presetCombo.setButtonCell(presetCombo.getCellFactory().call(null)); // Button rendering
        presetCombo.setOnAction(e -> applyPreset(presetCombo.getValue()));
        presetCombo.setMaxWidth(Double.MAX_VALUE);

        VBox presetBox = new VBox(5, new Label("Preset"), presetCombo);

        // Parameters
        seedField = new TextField();
        seedField.textProperty().addListener((obs, old, val) -> updatePreview());
        VBox seedBox = new VBox(5, new Label("Seed"), seedField);

        noiseFreqSlider = createSlider("Noise Frequency", 0.1, 2.0, 1.0);
        noiseScaleSlider = createSlider("Vertical Scale", 0.5, 3.0, 1.0);
        waterSlider = createSlider("Water Level", -0.5, 1.0, 0.0);
        tempSlider = createSlider("Temp Gradient", 0, 80, 40);

        controls.getChildren().addAll(presetBox, new Separator(), seedBox, noiseFreqSlider, noiseScaleSlider,
                waterSlider, tempSlider);

        // Center: Preview
        VBox previewBox = new VBox(10);
        previewCanvas = new Canvas(400, 200);
        statsLabel = new Label("Previewing...");

        previewBox.getChildren().addAll(new Label("Equirectangular Preview"), previewCanvas, statsLabel);
        previewBox.setAlignment(Pos.CENTER);

        // Bottom: Actions
        Button generateBtn = new Button("Generate World");
        generateBtn.setDefaultButton(true);
        generateBtn.setOnAction(e -> generateAndClose());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setCancelButton(true);
        cancelBtn.setOnAction(e -> close());

        HBox buttons = new HBox(10, cancelBtn, generateBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(20, 0, 0, 0));

        root.setLeft(controls);
        root.setCenter(previewBox);
        root.setBottom(buttons);

        Scene scene = new Scene(root, 700, 500);
        setScene(scene);

        // Initial state
        presetCombo.getSelectionModel().select(PlanetPreset.EARTH_LIKE);
        applyPreset(PlanetPreset.EARTH_LIKE);
    }

    private Slider createSlider(String label, double min, double max, double initial) {
        Slider slider = new Slider(min, max, initial);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.valueProperty().addListener((obs, old, val) -> {
            if (!slider.isValueChanging()) {
                updatePreview();
            }
        });
        slider.setOnMouseReleased(e -> updatePreview()); // Ensure update on release

        VBox box = new VBox(5, new Label(label), slider);
        ((VBox) ((BorderPane) getScene().getRoot()).getLeft()).getChildren().add(box); // Hacky add, but simplified
        return slider;
    }

    private void applyPreset(PlanetPreset p) {
        if (p == null)
            return;
        seedField.setText(String.valueOf(p.seed()));
        noiseFreqSlider.setValue(p.noiseFrequency());
        noiseScaleSlider.setValue(p.noiseScale());
        waterSlider.setValue(p.waterLevel());
        tempSlider.setValue(p.temperatureGradient());
        updatePreview();
    }

    private PlanetPreset buildPresetFromUI() {
        long seed = 12345;
        try {
            seed = Long.parseLong(seedField.getText());
        } catch (NumberFormatException ignored) {
        }

        return new PlanetPreset(
                "Custom",
                8, // Res 8 default
                seed,
                noiseFreqSlider.getValue(),
                noiseScaleSlider.getValue(),
                waterSlider.getValue(),
                tempSlider.getValue());
    }

    private void updatePreview() {
        if (previewCanvas == null)
            return;

        PlanetPreset preset = buildPresetFromUI();
        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        PixelWriter pw = gc.getPixelWriter();

        int w = (int) previewCanvas.getWidth();
        int h = (int) previewCanvas.getHeight();

        int oceanCount = 0;

        // Render Equirectangular Projection
        // Lat: +90 (Top) to -90 (Bottom)
        // Lng: -180 (Left) to +180 (Right)

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y / (double) h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lng = (x / (double) w) * 360.0 - 180.0;

                PlanetPoint p = generator.getPlanetPoint(lat, lng, preset);

                if (p.biome() == Biome.OCEAN || p.biome() == Biome.DEEP_OCEAN) {
                    oceanCount++;
                }

                pw.setColor(x, y, getBiomeColor(p.biome()));
            }
        }

        int total = w * h;
        int oceanPct = (oceanCount * 100) / total;
        statsLabel.setText(String.format("Ocean: %d%% | Land: %d%%", oceanPct, 100 - oceanPct));
    }

    private Color getBiomeColor(Biome biome) {
        return switch (biome) {
            case OCEAN -> Color.rgb(25, 50, 150);
            case DEEP_OCEAN -> Color.rgb(15, 30, 100);
            case BEACH -> Color.rgb(238, 214, 175);
            case DESERT -> Color.rgb(237, 201, 175);
            case PLAINS -> Color.rgb(124, 252, 0);
            case FOREST -> Color.rgb(34, 139, 34);
            case JUNGLE -> Color.rgb(0, 100, 0);
            case MOUNTAINS -> Color.rgb(139, 137, 137);
            case HILLS -> Color.rgb(160, 160, 120);
            case TUNDRA -> Color.rgb(221, 221, 187);
            case SNOW -> Color.rgb(255, 250, 250);
            default -> Color.BLACK;
        };
    }

    private void generateAndClose() {
        statsLabel.setText("Generating world... Please wait.");
        // We do this sync for MVP, but app might freeze for 2s.
        // Better to return and let caller handle.
        finalPreset = buildPresetFromUI();
        generatedCells = generator.generatePlanet(finalPreset);
        close();
    }

    /**
     * Show dialog and return generated cells (or empty if canceled).
     */
    public Optional<List<H3Cell>> showAndWaitForCells() {
        showAndWait();
        return Optional.ofNullable(generatedCells);
    }

    /**
     * Get the preset used for generation.
     */
    public Optional<PlanetPreset> getUsedPreset() {
        return Optional.ofNullable(finalPreset);
    }
}
