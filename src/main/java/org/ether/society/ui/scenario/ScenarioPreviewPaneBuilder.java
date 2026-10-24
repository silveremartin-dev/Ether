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
import org.ether.society.model.Biome;
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
            double delta = e.getDeltaY();
            if (delta > 0) zoomFactor = Math.min(zoomFactor * 1.15, 12.0);
            else if (delta < 0) zoomFactor = Math.max(zoomFactor / 1.15, 0.8);
            drawPreview();
            e.consume();
        });

        previewCanvas.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            if (context.isGraphicSelectActive() && e.isPrimaryButtonDown()) {
                isAreaSelecting = true;
                selStartX = e.getX();
                selStartY = e.getY();
                selEndX = e.getX();
                selEndY = e.getY();
            } else if (e.isPrimaryButtonDown() || e.isSecondaryButtonDown()) {
                isDragging = true;
            }
        });

        previewCanvas.setOnMouseDragged(e -> {
            if (isAreaSelecting) {
                selEndX = e.getX();
                selEndY = e.getY();
                drawPreview();
            } else if (isDragging) {
                panX += (e.getX() - lastMouseX);
                panY += (e.getY() - lastMouseY);
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                drawPreview();
            }
        });

        previewCanvas.setOnMouseReleased(e -> {
            if (isAreaSelecting) {
                isAreaSelecting = false;
                applySelectionBounds();
                drawPreview();
            }
            isDragging = false;
        });

        previewCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                zoomFactor = 1.0;
                panX = 0.0;
                panY = 0.0;
                drawPreview();
            }
        });
    }

    private void applySelectionBounds() {
        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();
        if (w < 1 || h < 1) return;

        double minLat = -90, maxLat = 90;
        double minLng = -180, maxLng = 180;
        double scale = Math.min(w / (maxLng - minLng), h / (maxLat - minLat)) * zoomFactor;
        double offX = (w / 2.0) - (0.0 - minLng) * scale + panX;
        double offY = (h / 2.0) - (maxLat - 0.0) * scale + panY;

        double x1 = Math.min(selStartX, selEndX);
        double x2 = Math.max(selStartX, selEndX);
        double y1 = Math.min(selStartY, selEndY);
        double y2 = Math.max(selStartY, selEndY);

        double lMinLng = Math.clamp(((x1 - offX) / scale) + minLng, -180.0, 180.0);
        double lMaxLng = Math.clamp(((x2 - offX) / scale) + minLng, -180.0, 180.0);
        double lMaxLat = Math.clamp(maxLat - ((y1 - offY) / scale), -90.0, 90.0);
        double lMinLat = Math.clamp(maxLat - ((y2 - offY) / scale), -90.0, 90.0);

        context.applySpatialClippingBounds(lMinLat, lMaxLat, lMinLng, lMaxLng);
        if (btnGraphicSelect != null) {
            btnGraphicSelect.setSelected(false);
        }
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
        int dims = context.getCultureVectorDimensions();
        ScenarioPreviewLegendBuilder.populateLegend(curSel, mode, dims, legendItemsContainer);
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
                List<H3Cell> previewCells;

                if (baseCells == null) {
                    baseCells = new ProceduralGenerator().generatePlanet(
                            new PlanetPreset(
                                    cfg.name(), 3, cfg.radiusKm(), cfg.dayLengthHours(),
                                    cfg.axialTiltDegrees(), cfg.yearLengthDays(), cfg.distanceToSunAU(),
                                    cfg.solarLuminosity(), cfg.minAltitudeMeters(), cfg.maxAltitudeMeters(),
                                    cfg.averageTempC(), cfg.seed(), cfg.noiseFrequency(),
                                    cfg.noiseScale(), cfg.waterLevel(), cfg.temperatureGradient(),
                                    cfg.oxygenPercentage(), cfg.albedo(), cfg.atmospherePressureAtm(),
                                    cfg.isSatellite(), cfg.parentPlanetMassEarthMasses(),
                                    cfg.orbitalDistanceToParentKm(), cfg.co2Ppm(),
                                    cfg.seismicActivityLevel(), cfg.volcanicActivityLevel(),
                                    cfg.customElevBase64(), cfg.customBiomeBase64(), cfg.customResourceBase64(),
                                    cfg.customClimateBase64(), cfg.customRainfallBase64(), cfg.customSeasonalityBase64(),
                                    cfg.elevationUseImport(), cfg.elevationMapSource(),
                                    cfg.tempUseImport(), cfg.tempSource(), cfg.tempSeed(),
                                    cfg.precipUseImport(), cfg.precipSource(), cfg.precipSeed(),
                                    cfg.seasonUseImport(), cfg.seasonSource(), cfg.seasonSeed()
                            )
                    );
                    context.applyPresetMapToCells(baseCells, cfg);
                    previewMeshCache.put(meshKey, baseCells);
                }

                if (mySeq != previewSeq.get()) return;

                previewCells = new ArrayList<>(baseCells.size());
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
                        previewStatusLabel.setText(I18n.getOrDefault("scenario.status.preview_generated", "Preview generated: ") + previewCells.size() + I18n.getOrDefault("scenario.status.cells_suffix", " cells."));
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

    private void distributeInitialPopulation(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        long totalPop = context.getInitialHumanCount();
        double capitalK0 = context.computeAutoCapitalFromYear(context.getStartYear());
        String pattern = context.getDensityPattern();
        boolean isEarthPreset = context.getActivePlanetPreset() != null 
                && context.getActivePlanetPreset().elevationUseImport() 
                && "earth".equalsIgnoreCase(context.getActivePlanetPreset().elevationMapSource());
        long startYear = context.getStartYear();

        ScenarioCellPopulationDistributor.distributeInitialPopulation(
                cells, totalPop, capitalK0, pattern, isEarthPreset, startYear, context.getCustomDensityImage(), context.getScenario()
        );
    }

    private Color getReliefAndDensityColor(H3Cell c) {
        return ScenarioCellPopulationDistributor.getReliefAndDensityColor(c);
    }

    private double computeCellCarryingCapacity(H3Cell c) {
        double capK0 = context.computeAutoCapitalFromYear(context.getStartYear());
        return ScenarioCellPopulationDistributor.computeCellCarryingCapacity(c, capK0);
    }

    public Color getPreviewColorForCell(H3Cell c) {
        if (c == null) return Color.BLACK;

        int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";

        double lat = c.getLatitude() != null ? c.getLatitude() : 0.0;
        double lon = c.getLongitude() != null ? c.getLongitude() : 0.0;

        double elev = c.getElevation() != null ? c.getElevation() : 0.0;
        boolean isLand = elev > 0.0 || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN);

        if (idx == 0) {
            if (context.isDensityImportActive()) {
                Image customDensityImage = context.getCustomDensityImage();
                if (customDensityImage != null && customDensityImage.getWidth() > 0) {
                    if (!isLand) return getReliefAndDensityColor(c);
                    Color customCol = sampleImageColorAtLatLon(customDensityImage, lat, lon);
                    if (customCol != null) {
                        double brightness = (customCol.getRed() + customCol.getGreen() + customCol.getBlue()) / 3.0;
                        if (Math.abs(customCol.getRed() - customCol.getGreen()) < 0.02 && Math.abs(customCol.getGreen() - customCol.getBlue()) < 0.02) {
                            Color heatCol;
                            if (brightness <= 0.02) heatCol = Color.rgb(30, 95, 165);
                            else if (brightness < 0.20) heatCol = Color.rgb(16, 185, 129);
                            else if (brightness < 0.45) heatCol = Color.rgb(234, 179, 8);
                            else if (brightness < 0.75) heatCol = Color.rgb(249, 115, 22);
                            else heatCol = Color.rgb(239, 68, 68);
                            return blendColors(getReliefAndDensityColor(c), heatCol, Math.max(0.65, brightness));
                        }
                        return customCol;
                    }
                }
                return getReliefAndDensityColor(c);
            }
            return getReliefAndDensityColor(c);
        }

        if (!isLand) {
            return getReliefAndDensityColor(c);
        }

        int dims = context.getCultureVectorDimensions();
        if (idx >= 1 && idx <= dims) {
            int tIndex = idx - 1;
            boolean isImport = context.isTensorImportActive(tIndex);
            if (isImport) {
                Image img = context.getCustomTensorImage(tIndex);
                if (img != null && img.getWidth() > 0) {
                    Color customCol = sampleImageColorAtLatLon(img, lat, lon);
                    if (customCol != null) return customCol;
                }
                return getReliefAndDensityColor(c);
            }

            double p1 = context.getTensorParam1(tIndex);
            double p2 = context.getTensorParam2(tIndex);
            double p3 = context.getTensorParam3(tIndex);
            long seed = context.getTensorSeed(tIndex);
            return context.evaluateProceduralTensorColor(tIndex, lat, lon, elev, p1, p2, p3, seed);
        }

        int derivedOffset = idx - (dims + 1);
        if (derivedOffset == 1 || mode.contains("capital") || mode.contains("k(x)")) {
            double cap = c.getResourceCapital() != null ? c.getResourceCapital() : 0.0;
            double norm = Math.clamp(cap / 500000.0, 0.0, 1.0);
            return Color.hsb((1.0 - norm) * 240.0, 0.85, 0.90);
        } else if (derivedOffset == 2 || mode.contains("énergétique") || mode.contains("energy") || mode.contains("energie") || mode.contains("energético") || mode.contains("e(x)")) {
            double energy = c.getEnergyFire() != null ? c.getEnergyFire() : 0.0;
            double norm = Math.clamp(energy / 1000000.0, 0.0, 1.0);
            return Color.hsb(30.0 + norm * 30.0, 0.90, 0.95);
        } else if (derivedOffset == 3 || mode.contains("alimentaires") || mode.contains("food") || mode.contains("nahrung") || mode.contains("alimentos") || mode.contains("f(x)")) {
            double food = c.getFoodResource() != null ? c.getFoodResource() : 0.0;
            double norm = Math.clamp(food / 100000.0, 0.0, 1.0);
            return Color.hsb(120.0, 0.70 + norm * 0.30, 0.60 + norm * 0.35);
        } else if (derivedOffset == 4 || mode.contains("informationnel") || mode.contains("info") || mode.contains("i(x)")) {
            double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
            double norm = Math.clamp(tech / 10.0, 0.0, 1.0);
            return Color.hsb(270.0 + norm * 60.0, 0.85, 0.90);
        } else if (derivedOffset == 5 || mode.contains("empreinte") || mode.contains("footprint") || mode.contains("fußabdruck") || mode.contains("huella") || mode.contains("malthus")) {
            double cap = computeCellCarryingCapacity(c);
            long pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double ratio = Math.clamp(pop / Math.max(1.0, cap), 0.0, 1.5);
            return Color.hsb((1.0 - Math.min(1.0, ratio)) * 120.0, 0.85, 0.90);
        } else if (derivedOffset == 6 || mode.contains("friction") || mode.contains("reibung") || mode.contains("fricción")) {
            double fric = c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
            double norm = Math.clamp((fric - 1.0) / 4.0, 0.0, 1.0);
            return Color.rgb((int)(norm * 255), (int)((1.0 - norm) * 200), 50);
        }

        return getReliefAndDensityColor(c);
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

        double minLat = -90, maxLat = 90;
        double minLng = -180, maxLng = 180;

        double scaleX = w / (maxLng - minLng);
        double scaleY = h / (maxLat - minLat);
        double baseScale = Math.min(scaleX, scaleY);
        double scale = baseScale * zoomFactor;

        double offX = (w / 2.0) - (0.0 - minLng) * scale + panX;
        double offY = (h / 2.0) - (maxLat - 0.0) * scale + panY;

        if (currentPreviewCells != null && !currentPreviewCells.isEmpty()) {
            double cellCount = currentPreviewCells.size();
            double areaSqDeg = (maxLat - minLat) * (maxLng - minLng);
            double avgAreaPerCell = areaSqDeg / Math.max(1.0, cellCount);
            double dynamicCellDeg = Math.sqrt(avgAreaPerCell) * 1.08;
            double cellSize = Math.max(1.5, dynamicCellDeg * scale);

            PlanetPreset activePlanet = context.getActivePlanetPreset() != null ? context.getActivePlanetPreset() : PlanetPreset.EARTH_LIKE;
            double wLvl = activePlanet.waterLevel();
            boolean hasOcean = wLvl > -0.4;
            boolean isReliefActive = (btnReliefOverlay != null && btnReliefOverlay.isSelected());

            for (H3Cell c : currentPreviewCells) {
                double x = (c.getLongitude() - minLng) * scale + offX;
                double y = (maxLat - c.getLatitude()) * scale + offY;

                if (x < -cellSize || x > w + cellSize || y < -cellSize || y > h + cellSize) continue;

                Color col = getPreviewColorForCell(c);
                boolean isCoastalCell = Boolean.TRUE.equals(c.getIsCoastal());
                double elev = c.getElevation() != null ? c.getElevation() : 0.0;
                boolean isDatumCell = !hasOcean && Math.abs(elev) < 350.0;

                if (isReliefActive) {
                    if (hasOcean && elev <= 0) {
                        double depthRatio = Math.clamp(Math.abs(elev) / 4000.0, 0.0, 1.0);
                        double mult = 0.85 + 0.15 * (1.0 - depthRatio);
                        col = Color.color(
                            Math.clamp(col.getRed() * mult, 0.0, 1.0),
                            Math.clamp(col.getGreen() * mult, 0.0, 1.0),
                            Math.clamp(col.getBlue() * mult, 0.0, 1.0)
                        );
                    } else {
                        double decl = c.getMovementFriction() != null ? Math.max(0.0, c.getMovementFriction() - 1.0) : 0.0;
                        double normElev = Math.clamp(elev / 4000.0, 0.0, 1.0);
                        double mult = 0.75 + 0.30 * normElev + 0.15 * Math.min(1.0, decl);
                        col = Color.color(
                            Math.clamp(col.getRed() * mult, 0.0, 1.0),
                            Math.clamp(col.getGreen() * mult, 0.0, 1.0),
                            Math.clamp(col.getBlue() * mult, 0.0, 1.0)
                        );
                    }
                }
                gc.setFill(col);
                double absLat = Math.abs(c.getLatitude());
                double latDistort = Math.cos(Math.toRadians(absLat));
                double cellW = Math.max(1.2, cellSize / Math.max(0.18, latDistort));
                double cellH = Math.max(1.2, cellSize * 1.05);

                gc.fillRect(x - cellW * 0.5, y - cellH * 0.5, cellW + 0.6, cellH + 0.6);

                if (isCoastalCell && hasOcean) {
                    gc.setStroke(Color.rgb(254, 240, 138, 0.85));
                    gc.setLineWidth(Math.max(0.5, 1.0 * zoomFactor));
                    gc.strokeLine(x - cellW * 0.5, y, x + cellW * 0.5, y);
                } else if (isDatumCell) {
                    gc.setStroke(Color.rgb(251, 191, 36, 0.85));
                    gc.setLineWidth(Math.max(0.5, 0.8 * zoomFactor));
                    gc.strokeLine(x - cellW * 0.4, y, x + cellW * 0.4, y);
                }
            }
        } else {
            int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
            String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";
            boolean isReliefOverlay = btnReliefOverlay != null && btnReliefOverlay.isSelected();
            ScenarioPreviewRasterRenderer.drawInstant2DDensityPreview(
                    gc, w, h, scale, offX, offY, context, idx, mode, isReliefOverlay, proceduralImageCache
            );
        }

        // Draw spatial clipping rectangle if enabled
        if (context.isClippingEnabled()) {
            double cMinLat = context.getClippingMinLat();
            double cMaxLat = context.getClippingMaxLat();
            double cMinLng = context.getClippingMinLng();
            double cMaxLng = context.getClippingMaxLng();

            double clipX = (cMinLng - minLng) * scale + offX;
            double clipY = (maxLat - cMaxLat) * scale + offY;
            double clipW = (cMaxLng - cMinLng) * scale;
            double clipH = (cMaxLat - cMinLat) * scale;

            gc.setStroke(Color.rgb(239, 68, 68, 0.95));
            gc.setLineWidth(2.0);
            gc.strokeRect(clipX, clipY, clipW, clipH);
            gc.setFill(Color.rgb(239, 68, 68, 0.12));
            gc.fillRect(clipX, clipY, clipW, clipH);
        }

        // Draw user drag selection box
        if (isAreaSelecting) {
            double rx = Math.min(selStartX, selEndX);
            double ry = Math.min(selStartY, selEndY);
            double rw = Math.abs(selEndX - selStartX);
            double rh = Math.abs(selEndY - selStartY);

            gc.setStroke(Color.rgb(56, 189, 248, 0.95));
            gc.setLineWidth(1.5);
            gc.strokeRect(rx, ry, rw, rh);
            gc.setFill(Color.rgb(56, 189, 248, 0.18));
            gc.fillRect(rx, ry, rw, rh);
        }

        updatePreviewDiagnostics();
    }

    private void updatePreviewDiagnostics() {
        if (previewStatusLabel == null || currentPreviewCells == null || currentPreviewCells.isEmpty()) return;

        int curSel = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        int dims = context.getCultureVectorDimensions();

        if (curSel == 0) {
            long totalHuman = 0;
            double maxDens = 0.0;
            int countLand = 0;
            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                long pop = c.getPopulation() != null ? c.getPopulation() : 0;
                totalHuman += pop;
                if (pop > maxDens) maxDens = pop;
                if ((c.getElevation() != null && c.getElevation() > 0.0) || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN)) {
                    countLand++;
                }
            }
            double avgPerLand = countLand > 0 ? (double) totalHuman / countLand : 0;
            previewStatusLabel.setText(String.format(
                I18n.getOrDefault("scenario.map_info.demography", "👥 Pop. globale : %s hab.  |  Moyenne/cellule : %.0f  |  Max locale : %s  |  Terres émergées : %d"),
                String.format(Locale.FRANCE, "%,d", totalHuman),
                avgPerLand,
                String.format(Locale.FRANCE, "%,.0f", maxDens),
                countLand
            ));
        } else if (curSel >= 1 && curSel <= dims) {
            int tIndex = curSel - 1;
            String modeStr = context.getCulturalTensorTitle(tIndex);
            double diff = context.getCulturalDiffusionRate();
            double mut = context.getCulturalMutationRate();

            double sumVal = 0.0;
            double sumSq = 0.0;
            double minVal = Double.MAX_VALUE;
            double maxVal = -Double.MAX_VALUE;
            int count = 0;

            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                boolean isLand = (c.getElevation() != null && c.getElevation() > 0.0) || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN);
                if (isLand) {
                    Color col = getPreviewColorForCell(c);
                    double val = (col.getRed() + col.getGreen() + col.getBlue()) / 3.0;
                    sumVal += val;
                    sumSq += val * val;
                    if (val < minVal) minVal = val;
                    if (val > maxVal) maxVal = val;
                    count++;
                }
            }
            double mean = count > 0 ? (sumVal / count) : 0.5;
            double variance = count > 1 ? Math.max(0.0, (sumSq - (sumVal * sumVal) / count) / (count - 1)) : 0.0;
            double stdDev = Math.sqrt(variance);
            if (count == 0) { minVal = 0.0; maxVal = 1.0; }

            previewStatusLabel.setText(String.format(
                I18n.getOrDefault("scenario.map_info.tensor", "⚙️ Mode : %s  |  📊 Dispersion : μ=%.2f ± %.2f [min=%.2f, max=%.2f]  |  🌀 Diffusion D=%.3f  |  🎲 Bruit=%.3f"),
                modeStr, mean, stdDev, minVal, maxVal, diff, mut
            ));
        } else {
            int derivedOffset = curSel - (dims + 1);
            String layerName = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue() : "";
            double sum = 0.0;
            int count = 0;
            for (H3Cell c : currentPreviewCells) {
                if (c == null) continue;
                boolean isLand = (c.getElevation() != null && c.getElevation() > 0.0) || (c.getBiome() != null && c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN);
                if (isLand) {
                    count++;
                    if (derivedOffset == 1 || layerName.contains("Capital") || layerName.contains("K(x)")) sum += (c.getResourceCapital() != null ? c.getResourceCapital() : 0.0);
                    else if (derivedOffset == 2 || layerName.contains("Energy") || layerName.contains("E(x)") || layerName.contains("énergétique") || layerName.contains("Energie") || layerName.contains("Energético")) sum += (c.getEnergyFire() != null ? c.getEnergyFire() : 0.0);
                    else if (derivedOffset == 3 || layerName.contains("Food") || layerName.contains("F(x)") || layerName.contains("alimentaires") || layerName.contains("Nahrung") || layerName.contains("Alimentos")) sum += (c.getFoodResource() != null ? c.getFoodResource() : 0.0);
                    else if (derivedOffset == 4 || layerName.contains("Info") || layerName.contains("I(x)") || layerName.contains("informationnel") || layerName.contains("Information")) sum += (c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 0.0);
                    else if (derivedOffset == 6 || layerName.contains("Friction") || layerName.contains("friction") || layerName.contains("Reibung") || layerName.contains("Fricción")) sum += (c.getMovementFriction() != null ? c.getMovementFriction() : 1.0);
                    else sum += computeCellCarryingCapacity(c);
                }
            }
            double mean = count > 0 ? (sum / count) : 0.0;
            previewStatusLabel.setText(String.format(
                I18n.getOrDefault("scenario.map_info.derived", "📊 Moyenne : %.2f  |  ⚡ Stock global : %s  |  📍 Cellules actives : %s"),
                mean,
                String.format(Locale.FRANCE, "%,.0f", sum),
                String.format(Locale.FRANCE, "%,d", count)
            ));
        }
    }

    private void exportPreviewImage() {
        if (previewCanvas == null) return;
        try {
            WritableImage wImg = new WritableImage((int) previewCanvas.getWidth(), (int) previewCanvas.getHeight());
            previewCanvas.snapshot(null, wImg);
            WindowUtils.exportImageWithChooser(context.getWindow(), wImg, "ether-map-preview.png", I18n.getOrDefault("scenario.title.export_preview_dialog", "Exporter l'Aperçu Cartographique (PNG)"));
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
