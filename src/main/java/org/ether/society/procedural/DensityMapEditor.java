/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.h3.H3Service;
import org.ether.society.i18n.I18n;
import com.uber.h3core.util.LatLng;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Interactive editor for painting resource density maps and placing cities.
 */
public class DensityMapEditor extends Stage {
    // private static final Logger logger =
    // LoggerFactory.getLogger(DensityMapEditor.class); // Unused

    private static final int CANVAS_SIZE = 600;
    private static final int MIN_BRUSH_SIZE = 10;
    private static final int MAX_BRUSH_SIZE = 200;

    // Tools
    private enum Tool {
        PAINT, ERASE, CITY
    }

    // Controls
    private ToggleGroup toolGroup;
    private RadioButton paintTool;
    private RadioButton eraseTool;
    private RadioButton cityTool;
    private ComboBox<ResourceType> resourceSelector;
    private Slider brushSizeSlider;
    private Slider intensitySlider;
    private Canvas editorCanvas;
    private Label statsLabel;

    // State
    private DensityMap densityMap;
    private Tool currentTool = Tool.PAINT;
    private ResourceType currentResource = ResourceType.FOOD;
    private double brushSize = 50;
    private double intensity = 0.8;
    private boolean isApplied = false;
    private boolean isPainting = false;

    // Map bounds for Europe
    private final double minLat = 35.0;
    private final double maxLat = 70.0;
    private final double minLng = -10.0;
    private final double maxLng = 40.0;

    public DensityMapEditor() {
        this.densityMap = new DensityMap();
        initUI();
    }

    private void initUI() {
        setTitle(I18n.get("density.title"));
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Left: Tools panel
        VBox leftPanel = createToolsPanel();

        // Center: Canvas
        VBox centerPanel = createCanvasPanel();

        HBox content = new HBox(20, leftPanel, centerPanel);
        root.setCenter(content);

        // Bottom: Buttons
        HBox buttons = createButtons();
        root.setBottom(buttons);

        Scene scene = new Scene(root);
        setScene(scene);
    }

    private VBox createToolsPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(200);
        panel.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");

        Label title = new Label(I18n.get("density.tools"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Tool selection
        toolGroup = new ToggleGroup();

        paintTool = new RadioButton(I18n.get("density.tool.paint"));
        paintTool.setToggleGroup(toolGroup);
        paintTool.setSelected(true);
        paintTool.setOnAction(e -> currentTool = Tool.PAINT);

        eraseTool = new RadioButton(I18n.get("density.tool.erase"));
        eraseTool.setToggleGroup(toolGroup);
        eraseTool.setOnAction(e -> currentTool = Tool.ERASE);

        cityTool = new RadioButton(I18n.get("density.tool.city"));
        cityTool.setToggleGroup(toolGroup);
        cityTool.setOnAction(e -> currentTool = Tool.CITY);

        // Resource type
        Label resourceLabel = new Label(I18n.get("density.resource_type"));
        resourceSelector = new ComboBox<>();
        resourceSelector.getItems().addAll(ResourceType.values());
        resourceSelector.setValue(ResourceType.FOOD);
        resourceSelector.setOnAction(e -> currentResource = resourceSelector.getValue());
        resourceSelector.disableProperty().bind(cityTool.selectedProperty());

        // Brush size
        Label brushLabel = new Label(I18n.get("density.brush_size"));
        brushSizeSlider = new Slider(MIN_BRUSH_SIZE, MAX_BRUSH_SIZE, brushSize);
        brushSizeSlider.setShowTickMarks(true);
        brushSizeSlider.setMajorTickUnit(50);
        Label brushValue = new Label(String.format("%.0f px", brushSizeSlider.getValue()));
        brushSizeSlider.valueProperty().addListener((obs, old, val) -> {
            brushSize = val.doubleValue();
            brushValue.setText(String.format("%.0f px", val.doubleValue()));
        });

        // Intensity
        Label intensityLabel = new Label(I18n.get("density.intensity"));
        intensitySlider = new Slider(0.1, 1.0, intensity);
        intensitySlider.setShowTickMarks(true);
        intensitySlider.setMajorTickUnit(0.3);
        Label intensityValue = new Label(String.format("%.2f", intensitySlider.getValue()));
        intensitySlider.valueProperty().addListener((obs, old, val) -> {
            intensity = val.doubleValue();
            intensityValue.setText(String.format("%.2f", val.doubleValue()));
        });
        intensitySlider.disableProperty().bind(cityTool.selectedProperty());

        panel.getChildren().addAll(
                title,
                new Separator(),
                new Label(I18n.get("density.select_tool")),
                paintTool,
                eraseTool,
                cityTool,
                new Separator(),
                resourceLabel, resourceSelector,
                new Separator(),
                brushLabel, new HBox(5, brushSizeSlider, brushValue),
                intensityLabel, new HBox(5, intensitySlider, intensityValue));

        return panel;
    }

    private VBox createCanvasPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);

        Label title = new Label(I18n.get("density.canvas"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Editor canvas
        editorCanvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        editorCanvas.setStyle("-fx-border-color: black;");
        drawBaseMap();

        // Mouse handlers
        editorCanvas.setOnMousePressed(this::handleMousePressed);
        editorCanvas.setOnMouseDragged(this::handleMouseDragged);
        editorCanvas.setOnMouseReleased(this::handleMouseReleased);

        // Stats
        statsLabel = new Label(I18n.get("density.stats_empty"));
        statsLabel.setWrapText(true);
        statsLabel.setPrefWidth(CANVAS_SIZE);
        statsLabel.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(title, editorCanvas, statsLabel);
        return panel;
    }

    private HBox createButtons() {
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        Button clearButton = new Button(I18n.get("density.clear"));
        clearButton.setOnAction(e -> clearMap());

        Button saveButton = new Button(I18n.get("density.save"));
        saveButton.setOnAction(e -> {
            isApplied = true;
            close();
        });

        Button closeButton = new Button(I18n.get("density.close"));
        closeButton.setOnAction(e -> {
            isApplied = false;
            close();
        });

        buttons.getChildren().addAll(clearButton, saveButton, closeButton);
        return buttons;
    }

    private void drawBaseMap() {
        GraphicsContext gc = editorCanvas.getGraphicsContext2D();
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        gc.setFill(Color.DARKGRAY);
        gc.setFont(javafx.scene.text.Font.font(12));
        gc.fillText(I18n.get("density.hint"), 200, CANVAS_SIZE / 2);
    }

    private void handleMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            isPainting = true;
            applyTool(e.getX(), e.getY());
        }
    }

    private void handleMouseDragged(MouseEvent e) {
        if (isPainting && e.getButton() == MouseButton.PRIMARY) {
            applyTool(e.getX(), e.getY());
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        isPainting = false;
        updateStats();
    }

    private void applyTool(double canvasX, double canvasY) {
        // Convert canvas coords to lat/lng
        double lat = maxLat - (canvasY / CANVAS_SIZE) * (maxLat - minLat);
        double lng = minLng + (canvasX / CANVAS_SIZE) * (maxLng - minLng);

        H3Service h3 = H3Service.getInstance();
        long h3Index = h3.latLngToH3(lat, lng, 8);

        switch (currentTool) {
            case PAINT -> paintDensity(h3Index, canvasX, canvasY);
            case ERASE -> eraseDensity(h3Index, canvasX, canvasY);
            case CITY -> placeCity(h3Index, canvasX, canvasY);
        }

        redrawCanvas();
    }

    private void paintDensity(long centerH3, double canvasX, double canvasY) {
        H3Service h3 = H3Service.getInstance();

        // Get cells within brush radius
        List<Long> affectedCells = getCellsInRadius(canvasX, canvasY, brushSize);

        for (long h3Index : affectedCells) {
            LatLng center = h3.h3ToLatLng(h3Index);
            double distance = calculateDistance(canvasX, canvasY, center.lat, center.lng);

            // Falloff based on distance
            double falloff = 1.0 - (distance / brushSize);
            falloff = Math.max(0, falloff);

            double densityDelta = intensity * falloff * 0.1; // Small increments
            densityMap.addDensity(h3Index, currentResource, densityDelta);
        }
    }

    private void eraseDensity(long centerH3, double canvasX, double canvasY) {
        List<Long> affectedCells = getCellsInRadius(canvasX, canvasY, brushSize);

        for (long h3Index : affectedCells) {
            densityMap.clearCell(h3Index);
        }
    }

    private void placeCity(long h3Index, double canvasX, double canvasY) {
        // Fractal city placement
        Random random = new Random();
        placeCityCluster(h3Index, 1, 3, random);
    }

    /**
     * Fractal city placement algorithm.
     */
    private void placeCityCluster(long centerIndex, int tier, int maxDepth, Random random) {
        if (tier > maxDepth)
            return;

        // Place city at center
        densityMap.addCity(centerIndex, tier);

        if (tier < maxDepth) {
            H3Service h3 = H3Service.getInstance();
            LatLng center = h3.h3ToLatLng(centerIndex);

            // Generate 2-4 satellite cities
            int satelliteCount = 2 + random.nextInt(3);
            double radius = 0.5 / tier; // Decreasing radius for each tier

            for (int i = 0; i < satelliteCount; i++) {
                double angle = (2 * Math.PI * i) / satelliteCount + random.nextDouble() * 0.5;
                double lat = center.lat + radius * Math.cos(angle);
                double lng = center.lng + radius * Math.sin(angle);

                long satelliteIndex = h3.latLngToH3(lat, lng, 8);

                // Recursive placement
                if (!densityMap.hasCity(satelliteIndex)) {
                    placeCityCluster(satelliteIndex, tier + 1, maxDepth, random);
                }
            }
        }
    }

    private List<Long> getCellsInRadius(double canvasX, double canvasY, double radius) {
        List<Long> cells = new ArrayList<>();
        H3Service h3 = H3Service.getInstance();

        // Sample points in a circle
        int samples = (int) (radius / 5); // More samples for larger brushes
        for (int i = 0; i < samples; i++) {
            for (int j = 0; j < samples; j++) {
                double dx = (i - samples / 2.0) * 2;
                double dy = (j - samples / 2.0) * 2;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist <= radius) {
                    double x = canvasX + dx;
                    double y = canvasY + dy;

                    if (x >= 0 && x < CANVAS_SIZE && y >= 0 && y < CANVAS_SIZE) {
                        double lat = maxLat - (y / CANVAS_SIZE) * (maxLat - minLat);
                        double lng = minLng + (x / CANVAS_SIZE) * (maxLng - minLng);
                        long h3Index = h3.latLngToH3(lat, lng, 8);

                        if (!cells.contains(h3Index)) {
                            cells.add(h3Index);
                        }
                    }
                }
            }
        }

        return cells;
    }

    private double calculateDistance(double canvasX, double canvasY, double lat, double lng) {
        double x = (lng - minLng) / (maxLng - minLng) * CANVAS_SIZE;
        double y = (maxLat - lat) / (maxLat - minLat) * CANVAS_SIZE;

        double dx = canvasX - x;
        double dy = canvasY - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void redrawCanvas() {
        GraphicsContext gc = editorCanvas.getGraphicsContext2D();
        drawBaseMap();

        H3Service h3 = H3Service.getInstance();

        // Draw density (simplified - just show painted cells)
        for (long h3Index : h3.getH3IndexesInBounds(minLat, maxLat, minLng, maxLng, 8)) {
            double maxDensity = 0;
            ResourceType maxResource = null;

            for (ResourceType type : ResourceType.values()) {
                double density = densityMap.getDensity(h3Index, type);
                if (density > maxDensity) {
                    maxDensity = density;
                    maxResource = type;
                }
            }

            if (maxDensity > 0.1 && maxResource != null) {
                LatLng center = h3.h3ToLatLng(h3Index);
                double x = (center.lng - minLng) / (maxLng - minLng) * CANVAS_SIZE;
                double y = (maxLat - center.lat) / (maxLat - minLat) * CANVAS_SIZE;

                gc.setFill(maxResource.getColor().deriveColor(0, 1, 1, maxDensity));
                gc.fillOval(x - 2, y - 2, 4, 4);
            }
        }

        // Draw cities
        for (var entry : densityMap.getCities().entrySet()) {
            LatLng center = h3.h3ToLatLng(entry.getKey());
            double x = (center.lng - minLng) / (maxLng - minLng) * CANVAS_SIZE;
            double y = (maxLat - center.lat) / (maxLat - minLat) * CANVAS_SIZE;

            int tier = entry.getValue();
            double size = 10.0 / tier; // Larger cities for tier 1

            gc.setFill(Color.RED);
            gc.fillRect(x - size / 2, y - size / 2, size, size);
        }
    }

    private void updateStats() {
        int cellsWithData = densityMap.getCellCount();
        int cityCount = densityMap.getCities().size();

        statsLabel.setText(String.format(
                I18n.get("density.stats"),
                cellsWithData,
                cityCount));
    }

    private void clearMap() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(I18n.get("density.clear"));
        confirm.setContentText(I18n.get("density.confirm_clear"));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                densityMap.clear();
                drawBaseMap();
                updateStats();
            }
        });
    }

    /**
     * Show dialog and return density map if saved.
     */
    public java.util.Optional<DensityMap> showAndWaitForMap() {
        showAndWait();
        return isApplied ? java.util.Optional.of(densityMap) : java.util.Optional.empty();
    }
}

