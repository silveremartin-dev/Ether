/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import org.ether.society.database.H3Cell;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.WindowUtils;
import org.ether.society.ui.util.ScenarioCellPopulationDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller and renderer for the interactive cartographic preview pane in the Scenario Setup Panel.
 * <p>
 * Supports multi-layer rendering across equirectangular 2D and orthographic spherical representations:
 * \[
 *   (x, y) = \mathcal{P}(\lambda, \phi) = \left( R (\lambda - \lambda_0) \cos \phi_0, R \phi \right)
 * \]
 * with dynamic hillshade elevation blending:
 * \[
 *   I_{\text{shade}} = \max\left(0, \cos \theta_z \cos \beta + \sin \theta_z \sin \beta \cos(\phi_{\text{sun}} - \alpha)\right)
 * \]
 * and real-time demographic and cultural tensor visualization.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioPreviewPaneBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioPreviewPaneBuilder.class);

    /**
     * Interface providing host context properties and callbacks.
     */
    public interface HostContext {
        Scenario getScenario();
        PlanetPreset getActivePlanetPreset();
        long getStartYear();
        int getCultureVectorDimensions();
        double getCulturalDiffusionRate();
        double getCulturalMutationRate();
        Image getCustomTensorImage(int index);
        boolean isTensorImportActive(int index);
        double getTensorParam1(int index);
        double getTensorParam2(int index);
        double getTensorParam3(int index);
        long getTensorSeed(int index);
        Color evaluateProceduralTensorColor(int tIdx, double lat, double lon, double elev, double p1, double p2, double p3, long seed);
        String getCulturalTensorTitle(int index);
        String getCulturalTensorPreviewName(int index);
        Image getCustomDensityImage();
        boolean isDensityImportActive();
        long getInitialHumanCount();
        String getDensityPattern();
        String getDemoSeed();
        String getCultSeed();
        int getH3Resolution();
        boolean isUpdatingFromPreset();
        double computeAutoCapitalFromYear(long year);
        Window getWindow();
        void notifyParamChange();
        void applySpatialClippingBounds(double minLat, double maxLat, double minLon, double maxLon);
        boolean isGraphicSelectActive();
        void setGraphicSelectActive(boolean active);
        double getClippingMinLat();
        double getClippingMaxLat();
        double getClippingMinLng();
        double getClippingMaxLng();
        boolean isClippingEnabled();
        Image getElevationImageForPreset(PlanetPreset preset);
        void applyPresetMapToCells(List<H3Cell> cells, PlanetPreset cfg);
        default void onPreviewCellsGenerated(List<H3Cell> cells) {}
    }

    private final HostContext context;

    // UI Controls
    private Label previewTitleLabel;
    private ComboBox<String> previewModeCombo;
    private ToggleButton btnReliefOverlay;
    private Button btnExportPreviewRaster;
    private ToggleButton btnGraphicSelect;
    private Label previewStatusLabel;
    private Canvas previewCanvas;
    private HBox legendItemsContainer;

    // Interactive Camera & Selection State
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;
    private boolean isDragging = false;
    private double selStartX = 0.0;
    private double selStartY = 0.0;
    private double selEndX = 0.0;
    private double selEndY = 0.0;
    private boolean isAreaSelecting = false;

    // Cache & Generation State
    private static final Map<String, List<H3Cell>> previewMeshCache = new ConcurrentHashMap<>();
    private final Map<String, WritableImage> proceduralImageCache = new ConcurrentHashMap<>();
    private final AtomicLong previewSeq = new AtomicLong(0);
    private final AtomicBoolean isPreviewGenerating = new AtomicBoolean(false);
    private List<H3Cell> currentPreviewCells = new ArrayList<>();

    public ScenarioPreviewPaneBuilder(HostContext context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    /**
     * Builds and returns the cartographic preview pane.
     *
     * @return VBox container containing the preview controls and canvas.
     */
    public VBox buildPreviewPane() {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10));
        root.getStyleClass().add("subcard-section");

        previewTitleLabel = new Label(I18n.getOrDefault("scenario.title.right_view", "🗺️ Resource Cartography & Display"));
        previewTitleLabel.getStyleClass().add("label-header");
        previewTitleLabel.setMaxWidth(Double.MAX_VALUE);
        previewTitleLabel.setAlignment(Pos.CENTER);
        previewTitleLabel.setStyle("-fx-alignment: center; -fx-text-alignment: center; -fx-font-weight: bold;");

        HBox titleBox = new HBox(previewTitleLabel);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setMaxWidth(Double.MAX_VALUE);

        previewModeCombo = new ComboBox<>();
        updatePreviewModesCombo();
        previewModeCombo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        previewModeCombo.setMaxWidth(380);
        previewModeCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else if (item.startsWith("──────────")) {
                    setText(item);
                    setDisable(true);
                    setStyle("-fx-font-weight: bold; -fx-opacity: 0.6; -fx-padding: 4 8;");
                } else {
                    setText(item);
                    setDisable(false);
                    setStyle("-fx-font-weight: normal;");
                }
            }
        });
        previewModeCombo.setOnAction(e -> {
            String val = previewModeCombo.getValue();
            if (val != null && (val.startsWith("──────────") || val.startsWith("─") || val.contains("─────"))) {
                previewModeCombo.getSelectionModel().select(0);
                return;
            }
            updatePreviewTitleText();
            updateBottomLegend();
            drawPreview();
        });

        btnReliefOverlay = new ToggleButton("🏔️ Relief Hypsométrique");
        btnReliefOverlay.setSelected(true);
        btnReliefOverlay.getStyleClass().add("button-secondary");
        btnReliefOverlay.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.relief_overlay", "Active/désactive le relief topographique ombré en arrière-plan")));
        btnReliefOverlay.setOnAction(e -> drawPreview());

        btnExportPreviewRaster = new Button(I18n.getOrDefault("scenario.btn.export_preview_raster", "💾 Exporter Image"));
        btnExportPreviewRaster.getStyleClass().add("button-secondary");
        btnExportPreviewRaster.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.export_preview_raster", "Exporte l'aperçu cartographique actuel en haute résolution (PNG)")));
        btnExportPreviewRaster.setOnAction(e -> exportPreviewImage());

        btnGraphicSelect = new ToggleButton("📐 Sélection Zone");
        btnGraphicSelect.getStyleClass().add("button-secondary");
        btnGraphicSelect.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.graphic_select", "Activer la sélection rectangulaire sur la carte pour définir les limites de découpage")));
        btnGraphicSelect.selectedProperty().addListener((obs, oldV, newV) -> {
            context.setGraphicSelectActive(newV);
            drawPreview();
        });

        HBox modeRow = new HBox(8, previewModeCombo, btnReliefOverlay, btnGraphicSelect, btnExportPreviewRaster);
        modeRow.setAlignment(Pos.CENTER);

        previewCanvas = new Canvas(760, 480);
        previewCanvas.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 8, 0, 0, 2);");
        setupCanvasInteractions();

        legendItemsContainer = new HBox(12);
        legendItemsContainer.setAlignment(Pos.CENTER);
        legendItemsContainer.setStyle("-fx-padding: 6 12; -fx-background-color: rgba(15, 23, 42, 0.7); -fx-background-radius: 6; -fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 6;");
        updateBottomLegend();

        previewStatusLabel = new Label(I18n.getOrDefault("scenario.label.preview_init", "En attente du calcul de la carte H3..."));
        previewStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        root.getChildren().addAll(titleBox, modeRow, previewCanvas, legendItemsContainer, previewStatusLabel);
        return root;
    }

    private void setupCanvasInteractions() {
        previewCanvas.setOnScroll(e -> {
            if (e.getDeltaY() > 0) zoomFactor = Math.min(zoomFactor * 1.15, 12.0);
            else if (e.getDeltaY() < 0) zoomFactor = Math.max(zoomFactor / 1.15, 0.8);
            drawPreview(); e.consume();
        });
        previewCanvas.setOnMousePressed(e -> {
            lastMouseX = e.getX(); lastMouseY = e.getY();
            if (context.isGraphicSelectActive() && e.isPrimaryButtonDown()) {
                isAreaSelecting = true; selStartX = selEndX = e.getX(); selStartY = selEndY = e.getY();
            } else if (e.isPrimaryButtonDown() || e.isSecondaryButtonDown()) {
                isDragging = true;
            }
        });
        previewCanvas.setOnMouseDragged(e -> {
            if (isAreaSelecting) {
                selEndX = e.getX(); selEndY = e.getY(); drawPreview();
            } else if (isDragging) {
                panX += (e.getX() - lastMouseX); panY += (e.getY() - lastMouseY);
                lastMouseX = e.getX(); lastMouseY = e.getY(); drawPreview();
            }
        });
        previewCanvas.setOnMouseReleased(e -> {
            if (isAreaSelecting) { isAreaSelecting = false; applySelectionBounds(); drawPreview(); }
            isDragging = false;
        });
        previewCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) { zoomFactor = 1.0; panX = 0.0; panY = 0.0; drawPreview(); }
        });
    }

    private void applySelectionBounds() {
        double w = previewCanvas.getWidth(), h = previewCanvas.getHeight();
        if (w < 1 || h < 1) return;
        double scale = Math.min(w / 360.0, h / 180.0) * zoomFactor;
        double offX = (w / 2.0) - 180.0 * scale + panX, offY = (h / 2.0) - 90.0 * scale + panY;
        double x1 = Math.min(selStartX, selEndX), x2 = Math.max(selStartX, selEndX);
        double y1 = Math.min(selStartY, selEndY), y2 = Math.max(selStartY, selEndY);
        double lMinLng = Math.clamp(((x1 - offX) / scale) - 180.0, -180.0, 180.0);
        double lMaxLng = Math.clamp(((x2 - offX) / scale) - 180.0, -180.0, 180.0);
        double lMaxLat = Math.clamp(90.0 - ((y1 - offY) / scale), -90.0, 90.0);
        double lMinLat = Math.clamp(90.0 - ((y2 - offY) / scale), -90.0, 90.0);
        context.applySpatialClippingBounds(lMinLat, lMaxLat, lMinLng, lMaxLng);
        if (btnGraphicSelect != null) btnGraphicSelect.setSelected(false);
    }

    public void updatePreviewModesCombo() {
        if (previewModeCombo == null) return;
        String prevSel = previewModeCombo.getValue();
        int prevIdx = previewModeCombo.getSelectionModel().getSelectedIndex();

        previewModeCombo.getItems().clear();
        previewModeCombo.getItems().add(I18n.getOrDefault("scenario.preview_mode.density", "👥 Densité Démographique & Biomes"));

        int dims = context.getCultureVectorDimensions();
        if (dims > 0) {
            previewModeCombo.getItems().add("────────── " + I18n.getOrDefault("scenario.preview_group.tensors", "VECTEURS CULTURELS") + " ──────────");
            for (int i = 0; i < dims; i++) {
                previewModeCombo.getItems().add(context.getCulturalTensorPreviewName(i));
            }
        }

        previewModeCombo.getItems().add("────────── " + I18n.getOrDefault("scenario.preview_group.derived", "CALQUES DÉDUITS & PHYSIQUE") + " ──────────");
        previewModeCombo.getItems().add(I18n.getOrDefault("scenario.preview_mode.capital", "🏛️ Capital Productif K(x) [kg/hab]"));
        previewModeCombo.getItems().add(I18n.getOrDefault("scenario.preview_mode.energy", "⚡ Régime Énergétique E(x) [MJ/hab]"));
        previewModeCombo.getItems().add(I18n.getOrDefault("scenario.preview_mode.food", "🌾 Stocks Alimentaires F(x) [mois]"));
        previewModeCombo.getItems().add(I18n.getOrDefault("scenario.preview_mode.info", "🧠 Stock Informationnel I(x) [bits/hab]"));
        previewModeCombo.getItems().add(I18n.getOrDefault("scenario.preview_mode.footprint", "🌱 Empreinte Écologique & Pression Malthusienne"));
        previewModeCombo.getItems().add(I18n.getOrDefault("scenario.preview_mode.friction", "🏔️ Friction aux Frontières & Obstacles"));

        if (prevIdx >= 0 && prevIdx < previewModeCombo.getItems().size() && !previewModeCombo.getItems().get(prevIdx).startsWith("──")) {
            previewModeCombo.getSelectionModel().select(prevIdx);
        } else if (prevSel != null && previewModeCombo.getItems().contains(prevSel)) {
            previewModeCombo.getSelectionModel().select(prevSel);
        } else {
            previewModeCombo.getSelectionModel().select(0);
        }
    }

    public void updatePreviewTitleText() {
        if (previewTitleLabel == null || previewModeCombo == null) return;
        String sel = previewModeCombo.getValue();
        if (sel == null || sel.isBlank() || sel.startsWith("──")) {
            previewTitleLabel.setText(I18n.getOrDefault("scenario.title.right_view", "🗺️ Resource Cartography & Display"));
            return;
        }
        previewTitleLabel.setText("🗺️ " + sel);
    }

    public void updateBottomLegend() {
        if (legendItemsContainer == null) return;
        int curSel = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";
        ScenarioPreviewLegendBuilder.populateLegend(curSel, mode, context.getCultureVectorDimensions(), legendItemsContainer);
    }

    public void clearCache() {
        proceduralImageCache.clear();
    }

    public void generatePreview() {
        if (previewStatusLabel != null) {
            previewStatusLabel.setText(I18n.getOrDefault("scenario.status.calc_preview", "⚡ Calculating demographic preview in background..."));
        }
        isPreviewGenerating.set(true);
        final long mySeq = previewSeq.incrementAndGet();

        new Thread(() -> {
            try {
                PlanetPreset cfg = context.getActivePlanetPreset();
                if (cfg == null) cfg = PlanetPreset.EARTH_LIKE;

                String meshKey = cfg.name() + "_res3_" + cfg.seed();
                List<H3Cell> baseCells = previewMeshCache.get(meshKey);
                if (baseCells == null) {
                    baseCells = new ProceduralGenerator().generatePlanet(buildRes3Preset(cfg));
                    context.applyPresetMapToCells(baseCells, cfg);
                    previewMeshCache.put(meshKey, baseCells);
                }
                if (mySeq != previewSeq.get()) return;

                List<H3Cell> previewCells = new ArrayList<>(baseCells.size());
                for (H3Cell c : baseCells) {
                    H3Cell copy = new H3Cell(c.getH3Index(), c.getLatitude(), c.getLongitude());
                    copy.setElevation(c.getElevation());
                    copy.setBiome(c.getBiome());
                    copy.setTemperature(c.getTemperature());
                    copy.setRainfall(c.getRainfall());
                    previewCells.add(copy);
                }

                distributeInitialPopulation(previewCells);
                if (mySeq != previewSeq.get()) return;

                javafx.application.Platform.runLater(() -> {
                    if (mySeq != previewSeq.get()) return;
                    currentPreviewCells = previewCells;
                    context.onPreviewCellsGenerated(previewCells);
                    isPreviewGenerating.set(false);
                    drawPreview();
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText(I18n.getOrDefault("scenario.status.preview_generated", "Preview generated: ")
                                + previewCells.size() + I18n.getOrDefault("scenario.status.cells_suffix", " cells."));
                    }
                });
            } catch (Exception ex) {
                logger.error("Failed to generate preview", ex);
                javafx.application.Platform.runLater(() -> {
                    if (mySeq != previewSeq.get()) return;
                    isPreviewGenerating.set(false);
                    if (previewStatusLabel != null) {
                        previewStatusLabel.setText(I18n.getOrDefault("scenario.status.preview_error", "Preview error: ") + ex.getMessage());
                    }
                    drawPreview();
                });
            }
        }, "H3-Preview-Async-Thread").start();
    }

    private static PlanetPreset buildRes3Preset(PlanetPreset cfg) {
        return new PlanetPreset(
                cfg.name(), 3, cfg.radiusKm(), cfg.dayLengthHours(), cfg.axialTiltDegrees(), cfg.yearLengthDays(), cfg.distanceToSunAU(),
                cfg.solarLuminosity(), cfg.minAltitudeMeters(), cfg.maxAltitudeMeters(), cfg.averageTempC(), cfg.seed(), cfg.noiseFrequency(),
                cfg.noiseScale(), cfg.waterLevel(), cfg.temperatureGradient(), cfg.oxygenPercentage(), cfg.albedo(), cfg.atmospherePressureAtm(),
                cfg.isSatellite(), cfg.parentPlanetMassEarthMasses(), cfg.orbitalDistanceToParentKm(), cfg.co2Ppm(), cfg.seismicActivityLevel(),
                cfg.volcanicActivityLevel(), cfg.customElevBase64(), cfg.customBiomeBase64(), cfg.customResourceBase64(), cfg.customClimateBase64(),
                cfg.customRainfallBase64(), cfg.customSeasonalityBase64(), cfg.elevationUseImport(), cfg.elevationMapSource(), cfg.tempUseImport(),
                cfg.tempSource(), cfg.tempSeed(), cfg.precipUseImport(), cfg.precipSource(), cfg.precipSeed(), cfg.seasonUseImport(), cfg.seasonSource(), cfg.seasonSeed());
    }

    private void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;
        boolean isEarthPreset = context.getActivePlanetPreset() != null
                && context.getActivePlanetPreset().elevationUseImport()
                && "earth".equalsIgnoreCase(context.getActivePlanetPreset().elevationMapSource());
        ScenarioCellPopulationDistributor.distributeInitialPopulation(
                cells,
                context.getInitialHumanCount(),
                context.computeAutoCapitalFromYear(context.getStartYear()),
                context.getDensityPattern(),
                isEarthPreset,
                context.getStartYear(),
                context.getCustomDensityImage(),
                context.getScenario());
    }

    public Color getPreviewColorForCell(H3Cell c) {
        int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";
        return ScenarioPreviewCellColorHelper.getPreviewColorForCell(c, idx, mode, context);
    }

    public Color sampleImageColorAtLatLon(Image img, double lat, double lon) {
        return ScenarioPreviewRasterRenderer.sampleImageColorAtLatLon(img, lat, lon);
    }

    public Color blendColors(Color base, Color overlay, double opacity) {
        return ScenarioPreviewRasterRenderer.blendColors(base, overlay, opacity);
    }

    public void drawPreview() {
        if (context.isUpdatingFromPreset() || previewCanvas == null) return;
        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();
        if (w < 1.0 || h < 1.0) return;

        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        double scale = Math.min(w / 360.0, h / 180.0) * zoomFactor;
        double offX = (w / 2.0) - 180.0 * scale + panX;
        double offY = (h / 2.0) - 90.0 * scale + panY;

        int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String rawLayer = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue() : "";
        String mode = rawLayer.toLowerCase();
        boolean isReliefActive = btnReliefOverlay != null && btnReliefOverlay.isSelected();

        if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
            ScenarioPreviewCellColorHelper.drawCellMesh(
                    gc, w, h, scale, offX, offY, zoomFactor, currentPreviewCells, idx, mode, isReliefActive, context);
        } else {
            ScenarioPreviewRasterRenderer.drawInstant2DDensityPreview(
                    gc, w, h, scale, offX, offY, context, idx, mode, isReliefActive, proceduralImageCache);
        }

        if (context.isClippingEnabled()) {
            double cMinLat = context.getClippingMinLat();
            double cMaxLat = context.getClippingMaxLat();
            double cMinLng = context.getClippingMinLng();
            double cMaxLng = context.getClippingMaxLng();

            double clipX = (cMinLng + 180.0) * scale + offX, clipY = (90.0 - cMaxLat) * scale + offY;
            double clipW = (cMaxLng - cMinLng) * scale, clipH = (cMaxLat - cMinLat) * scale;
            gc.setStroke(Color.rgb(239, 68, 68, 0.95)); gc.setLineWidth(2.0);
            gc.strokeRect(clipX, clipY, clipW, clipH);
            gc.setFill(Color.rgb(239, 68, 68, 0.12)); gc.fillRect(clipX, clipY, clipW, clipH);
        }
        if (isAreaSelecting) {
            double rx = Math.min(selStartX, selEndX), ry = Math.min(selStartY, selEndY);
            double rw = Math.abs(selEndX - selStartX), rh = Math.abs(selEndY - selStartY);
            gc.setStroke(Color.rgb(56, 189, 248, 0.95)); gc.setLineWidth(1.5);
            gc.strokeRect(rx, ry, rw, rh);
            gc.setFill(Color.rgb(56, 189, 248, 0.18)); gc.fillRect(rx, ry, rw, rh);
        }

        ScenarioPreviewCellColorHelper.updatePreviewDiagnostics(
                previewStatusLabel, currentPreviewCells, idx, rawLayer, context);
    }

    private void exportPreviewImage() {
        if (previewCanvas == null) return;
        try {
            WritableImage wImg = new WritableImage((int) previewCanvas.getWidth(), (int) previewCanvas.getHeight());
            previewCanvas.snapshot(null, wImg);
            WindowUtils.exportImageWithChooser(
                    context.getWindow(), wImg, "ether-map-preview.png",
                    I18n.getOrDefault("scenario.title.export_preview_dialog", "Exporter l'Aperçu Cartographique (PNG)"));
        } catch (Exception ex) {
            logger.error("Failed to export preview canvas image", ex);
        }
    }

    // Getters and Setters
    public List<H3Cell> getCurrentPreviewCells() { return currentPreviewCells; }
    public void setCurrentPreviewCells(List<H3Cell> cells) { this.currentPreviewCells = cells; }
    public ComboBox<String> getPreviewModeCombo() { return previewModeCombo; }
    public ToggleButton getBtnReliefOverlay() { return btnReliefOverlay; }
    public Label getPreviewStatusLabel() { return previewStatusLabel; }
    public Canvas getPreviewCanvas() { return previewCanvas; }
}
