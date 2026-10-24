/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.ether.society.analytics.MapComparisonMetrics;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.export.ComparativeAnalyticsExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 2D Spatial Tensor & Cartographic Comparison Pane.
 *
 * <p>Provides side-by-side raster visualization, discrepancy heatmapping \(\Delta(A - B)\),
 * chronological timeline scrubbing with interactive playback, multi-channel physical and
 * cliodynamic tensor analysis, and spatial fidelity metrics (SSIM, RMSE, Pearson \(r\),
 * Jaccard index, and Kullback-Leibler divergence).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SpatialTensorComparisonPane extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(SpatialTensorComparisonPane.class);

    // Cached available historical Earth epoch directories for O(1) binary search lookup
    private static final int[] AVAILABLE_EARTH_EPOCHS;
    static {
        File earthDir = new File("data/maps/ether/earth/");
        if (earthDir.exists() && earthDir.isDirectory()) {
            File[] sub = earthDir.listFiles(File::isDirectory);
            if (sub != null) {
                List<Integer> yrs = new ArrayList<>();
                for (File d : sub) {
                    try {
                        yrs.add(Integer.parseInt(d.getName()));
                    } catch (NumberFormatException ignored) {}
                }
                Collections.sort(yrs);
                AVAILABLE_EARTH_EPOCHS = yrs.stream().mapToInt(Integer::intValue).toArray();
            } else {
                AVAILABLE_EARTH_EPOCHS = new int[0];
            }
        } else {
            AVAILABLE_EARTH_EPOCHS = new int[0];
        }
    }

    // High-performance LRU cache for 2D cartographic rasters
    private static final Map<String, BufferedImage> RASTER_IMAGE_CACHE = new ConcurrentHashMap<>();

    private final Function<ScenarioSelectableItem, SimulationRunRecord> runResolver;

    // UI Components
    private Label channelLabel;
    private ComboBox<String> spatialChannelCombo;
    private Label scenarioSelectLabelA;
    private ComboBox<ScenarioSelectableItem> comboScenarioA;
    private Label scenarioSelectLabelB;
    private ComboBox<ScenarioSelectableItem> comboScenarioB;

    private Slider dateSlider;
    private Label currentDateLabel;
    private Button playTimelineBtn;
    private Button export2DZipBtn;

    private Label mapRegenStatusLabel;
    private ProgressBar mapRegenProgress;
    private HBox mapRegenBox;

    private ImageView mapImageViewA;
    private ImageView mapImageViewB;
    private ImageView mapImageViewDiff;
    private Label mapLabelA;
    private Label mapLabelB;
    private Label mapLabelDiff;

    private Label legendBlue;
    private Label legendSlate;
    private Label legendGreen;
    private Label legendRed;

    private TextArea spatialMetricsReportArea;

    // Hover Map Magnifier Popup
    private final Popup hoverMapPopup = new Popup();
    private final ImageView popupImageView = new ImageView();
    private final Label popupTitleLabel = new Label();
    private Timeline popupHideTimer;

    // Animation & Concurrency
    private Timeline timelineAnimation;
    private boolean isPlayingAnimation = false;
    private int currentTimelineFrameIndex = 0;
    private final AtomicLong spatialComparisonRequestId = new AtomicLong(0);
    private final List<Integer> preloadedTimelineYears = new CopyOnWriteArrayList<>();
    private boolean isUpdatingTexts = false;

    /**
     * Constructs a spatial tensor comparison pane.
     *
     * @param runResolver function resolving a scenario item to its simulated telemetry record
     */
    public SpatialTensorComparisonPane(Function<ScenarioSelectableItem, SimulationRunRecord> runResolver) {
        super(8);
        this.runResolver = runResolver;
        setPadding(new Insets(12));
        initUI();
    }

    private void initUI() {
        channelLabel = new Label(I18n.getOrDefault("analytics.channel_label", "Tensor Channel:"));
        channelLabel.getStyleClass().add("control-label");

        spatialChannelCombo = new ComboBox<>();
        spatialChannelCombo.setPrefWidth(240);
        spatialChannelCombo.setOnAction(e -> update2DSpatialComparison());
        spatialChannelCombo.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.channel_select", "Select spatial physical/cliodynamic tensor layer to compare between scenarios A and B.")));

        scenarioSelectLabelA = new Label(I18n.getOrDefault("analytics.label.select_a", "Baseline (A):"));
        scenarioSelectLabelA.getStyleClass().add("control-label");
        comboScenarioA = new ComboBox<>();
        comboScenarioA.setPrefWidth(210);
        comboScenarioA.setOnAction(e -> update2DSpatialComparison());
        comboScenarioA.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.scenario_a", "Select baseline scenario (or empirical Ground Truth) for spatial differential tensor mapping.")));

        scenarioSelectLabelB = new Label(I18n.getOrDefault("analytics.label.select_b", "Target (B):"));
        scenarioSelectLabelB.getStyleClass().add("control-label");
        comboScenarioB = new ComboBox<>();
        comboScenarioB.setPrefWidth(210);
        comboScenarioB.setOnAction(e -> update2DSpatialComparison());
        comboScenarioB.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.scenario_b", "Select target scenario to evaluate against baseline A across 2D map fidelity metrics.")));

        currentDateLabel = new Label(I18n.getOrDefault("analytics.label.year_ad", "📅 Year: 0 AD"));
        currentDateLabel.getStyleClass().add("value-label");

        playTimelineBtn = new Button(I18n.getOrDefault("analytics.btn.play_timeline", "▶️ Play Timeline"));
        playTimelineBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand;");
        playTimelineBtn.setOnAction(e -> toggleDateAnimation());
        playTimelineBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.play_timeline", "Animate chronological evolution of 2D cartographic tensors over historical centuries.")));

        dateSlider = new Slider(0, 2000, 0);
        dateSlider.setBlockIncrement(50);
        dateSlider.setMajorTickUnit(500);
        dateSlider.setMinorTickCount(4);
        dateSlider.setShowTickMarks(true);
        dateSlider.setShowTickLabels(true);
        dateSlider.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.date_slider", "Drag to navigate historical chronological timeline of comparative maps.")));
        HBox.setHgrow(dateSlider, Priority.ALWAYS);

        PauseTransition sliderDebounce = new PauseTransition(Duration.millis(90));
        sliderDebounce.setOnFinished(e -> update2DSpatialComparison());
        dateSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int year = newV.intValue();
            currentDateLabel.setText(String.format(I18n.getOrDefault("analytics.label.year_ad_formatted", "📅 Year: %d AD"), year));
            if (isPlayingAnimation) {
                update2DSpatialComparison();
            } else {
                sliderDebounce.playFromStart();
            }
        });

        HBox scenarioRow = new HBox(10, scenarioSelectLabelA, comboScenarioA, scenarioSelectLabelB, comboScenarioB);
        scenarioRow.setAlignment(Pos.CENTER_LEFT);

        HBox channelRow = new HBox(10, channelLabel, spatialChannelCombo);
        channelRow.setAlignment(Pos.CENTER_LEFT);

        VBox spatialControlsBox = new VBox(6, scenarioRow, channelRow);

        mapRegenStatusLabel = new Label("");
        mapRegenStatusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 11px;");

        mapRegenProgress = new ProgressBar(0.0);
        mapRegenProgress.setPrefWidth(220);
        mapRegenProgress.setStyle("-fx-accent: #38bdf8;");

        mapRegenBox = new HBox(10, mapRegenProgress, mapRegenStatusLabel);
        mapRegenBox.setAlignment(Pos.CENTER_LEFT);
        mapRegenBox.setPadding(new Insets(2, 0, 4, 0));
        mapRegenBox.setVisible(false);
        mapRegenBox.setManaged(false);

        HBox spatialTimelineRow = new HBox(12, dateSlider, currentDateLabel, playTimelineBtn);
        spatialTimelineRow.setAlignment(Pos.CENTER_LEFT);

        export2DZipBtn = new Button();
        export2DZipBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #6366f1; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
        export2DZipBtn.setOnAction(e -> export2DCartographyZip());

        HBox spatialBottomBox = new HBox(10, export2DZipBtn);
        spatialBottomBox.setAlignment(Pos.CENTER_LEFT);
        spatialBottomBox.setPadding(new Insets(6, 0, 0, 0));

        // 3-Map Side-by-Side Spatial Map Viewers (A, B, and Difference Heatmap)
        mapLabelA = new Label(I18n.getOrDefault("analytics.label.scenario_a_ref", "Scenario A (Reference Baseline)"));
        mapLabelA.getStyleClass().add("label-section-header");
        mapImageViewA = new ImageView();
        mapImageViewA.setFitWidth(360);
        mapImageViewA.setFitHeight(180);
        mapImageViewA.setPreserveRatio(true);
        mapImageViewA.getStyleClass().add("card-section");

        VBox mapBoxA = new VBox(4, mapLabelA, mapImageViewA);
        mapBoxA.setAlignment(Pos.CENTER);

        mapLabelB = new Label(I18n.getOrDefault("analytics.label.scenario_b_target", "Scenario B (Comparison Target)"));
        mapLabelB.getStyleClass().add("label-section-header");
        mapImageViewB = new ImageView();
        mapImageViewB.setFitWidth(360);
        mapImageViewB.setFitHeight(180);
        mapImageViewB.setPreserveRatio(true);
        mapImageViewB.getStyleClass().add("card-section");

        VBox mapBoxB = new VBox(4, mapLabelB, mapImageViewB);
        mapBoxB.setAlignment(Pos.CENTER);

        mapLabelDiff = new Label(I18n.getOrDefault("analytics.label.scenario_diff", "Discrepancy Heatmap Δ(A - B)"));
        mapLabelDiff.getStyleClass().add("label-section-header");
        mapLabelDiff.setStyle("-fx-text-fill: #f59e0b;");
        mapImageViewDiff = new ImageView();
        mapImageViewDiff.setFitWidth(360);
        mapImageViewDiff.setFitHeight(180);
        mapImageViewDiff.setPreserveRatio(true);
        mapImageViewDiff.getStyleClass().add("card-section");

        VBox mapBoxDiff = new VBox(4, mapLabelDiff, mapImageViewDiff);
        mapBoxDiff.setAlignment(Pos.CENTER);

        HBox mapPairBox = new HBox(12, mapBoxA, mapBoxB, mapBoxDiff);
        mapPairBox.setAlignment(Pos.CENTER);

        // Setup Hover Magnifier Popup
        initHoverPopup();
        attachHoverZoom(mapImageViewA, () -> mapLabelA.getText());
        attachHoverZoom(mapImageViewB, () -> mapLabelB.getText());
        attachHoverZoom(mapImageViewDiff, () -> mapLabelDiff.getText());

        // Discrepancy Heatmap Color Scale Legend
        HBox discrepancyLegendBox = new HBox(16);
        discrepancyLegendBox.setAlignment(Pos.CENTER);
        discrepancyLegendBox.setPadding(new Insets(4, 12, 4, 12));
        discrepancyLegendBox.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 6;");

        legendBlue = new Label("🟦 " + I18n.getOrDefault("analytics.legend.under", "A < B (Underestimation)"));
        legendBlue.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");

        legendSlate = new Label("⬛ " + I18n.getOrDefault("analytics.legend.zero", "Δ = 0 (Identical / Zero divergence)"));
        legendSlate.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");

        legendGreen = new Label("🟩 " + I18n.getOrDefault("analytics.legend.close", "|Δ| ≤ 5% (Close agreement)"));
        legendGreen.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px;");

        legendRed = new Label("🟥 " + I18n.getOrDefault("analytics.legend.over", "A > B (Overestimation)"));
        legendRed.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");

        discrepancyLegendBox.getChildren().addAll(legendBlue, legendSlate, legendGreen, legendRed);

        // Quantitative Spatial Comparison Metrics Area
        spatialMetricsReportArea = new TextArea();
        spatialMetricsReportArea.setEditable(false);
        spatialMetricsReportArea.setPrefRowCount(6);
        spatialMetricsReportArea.getStyleClass().add("scenario-description-area");
        VBox.setVgrow(spatialMetricsReportArea, Priority.ALWAYS);

        getChildren().addAll(spatialControlsBox, spatialTimelineRow, mapRegenBox, mapPairBox,
                discrepancyLegendBox, spatialMetricsReportArea, spatialBottomBox);
    }

    private void initHoverPopup() {
        popupImageView.setFitWidth(560);
        popupImageView.setFitHeight(280);
        popupImageView.setPreserveRatio(true);
        popupTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 4 8; -fx-font-size: 13px;");

        VBox popupBox = new VBox(6, popupTitleLabel, popupImageView);
        popupBox.setStyle(
                "-fx-background-color: rgba(15, 23, 42, 0.96);" +
                "-fx-padding: 10px;" +
                "-fx-border-color: #38bdf8;" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 12, 0, 0, 4);"
        );
        hoverMapPopup.getContent().add(popupBox);
        hoverMapPopup.setAutoHide(false);
    }

    private void attachHoverZoom(ImageView view, java.util.function.Supplier<String> titleSupplier) {
        view.setCursor(Cursor.HAND);
        view.setOnMouseEntered(e -> {
            if (view.getImage() != null) {
                cancelHideTimer();
                popupImageView.setImage(view.getImage());
                popupTitleLabel.setText(titleSupplier.get());
                Bounds bounds = view.localToScreen(view.getBoundsInLocal());
                if (bounds != null) {
                    double popupX = bounds.getMinX();
                    double popupY = bounds.getMaxY() + 8;
                    Rectangle2D screen = Screen.getPrimary().getVisualBounds();
                    if (popupY + 300 > screen.getMaxY()) {
                        popupY = bounds.getMinY() - 310;
                    }
                    if (popupX + 580 > screen.getMaxX()) {
                        popupX = screen.getMaxX() - 590;
                    }
                    hoverMapPopup.show(view, Math.max(10, popupX), Math.max(10, popupY));
                }
            }
        });
        view.setOnMouseExited(e -> scheduleHidePopup());
        view.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && view.getImage() != null) {
                showFullMapModal(titleSupplier.get(), view.getImage());
            }
        });
    }

    private void scheduleHidePopup() {
        cancelHideTimer();
        popupHideTimer = new Timeline(new KeyFrame(Duration.millis(200), ev -> hoverMapPopup.hide()));
        popupHideTimer.setCycleCount(1);
        popupHideTimer.play();
    }

    private void cancelHideTimer() {
        if (popupHideTimer != null) {
            popupHideTimer.stop();
            popupHideTimer = null;
        }
    }

    private void showFullMapModal(String title, Image img) {
        if (img == null) return;
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle(title != null ? title : I18n.getOrDefault("analytics.preview.modal_title", "2D Cartographic Tensor High-Resolution View"));

        ImageView largeView = new ImageView(img);
        largeView.setFitWidth(800);
        largeView.setFitHeight(400);
        largeView.setPreserveRatio(true);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 15px;");

        Button closeBtn = new Button(I18n.getOrDefault("analytics.preview.close", "Close"));
        closeBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 16; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> modal.close());
        closeBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.close_modal", "Close detailed view dialog.")));

        VBox layout = new VBox(12, titleLbl, largeView, closeBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(16));
        layout.setStyle("-fx-background-color: #0f172a;");

        Scene scene = new Scene(layout, 840, 500);
        modal.setScene(scene);
        modal.show();
    }

    /**
     * Updates scenario pair ComboBoxes with current selection pool.
     *
     * @param scenarioList active list of scenarios
     */
    public void updateScenarioPairCombos(List<ScenarioSelectableItem> scenarioList) {
        if (comboScenarioA == null || comboScenarioB == null || scenarioList == null) return;

        List<ScenarioSelectableItem> selected = scenarioList.stream()
                .filter(ScenarioSelectableItem::isSelected)
                .toList();

        List<ScenarioSelectableItem> pool = (selected.size() >= 2) ? selected : new ArrayList<>(scenarioList);

        ScenarioSelectableItem prevA = comboScenarioA.getValue();
        ScenarioSelectableItem prevB = comboScenarioB.getValue();

        comboScenarioA.getItems().setAll(pool);
        comboScenarioB.getItems().setAll(pool);

        if (prevA != null && pool.contains(prevA)) {
            comboScenarioA.setValue(prevA);
        } else if (!pool.isEmpty()) {
            comboScenarioA.setValue(pool.get(0));
        }

        if (prevB != null && pool.contains(prevB)) {
            comboScenarioB.setValue(prevB);
        } else if (pool.size() > 1) {
            comboScenarioB.setValue(pool.get(1));
        } else if (!pool.isEmpty()) {
            comboScenarioB.setValue(pool.get(0));
        }

        update2DSpatialComparison();
    }

    /**
     * Re-renders the 2D spatial comparison maps, delta heatmap, and fidelity metrics.
     */
    public void update2DSpatialComparison() {
        if (isUpdatingTexts) return;

        ScenarioSelectableItem itemA = comboScenarioA != null ? comboScenarioA.getValue() : null;
        ScenarioSelectableItem itemB = comboScenarioB != null ? comboScenarioB.getValue() : null;

        if (itemA == null && comboScenarioA != null && !comboScenarioA.getItems().isEmpty()) itemA = comboScenarioA.getItems().get(0);
        if (itemB == null && comboScenarioB != null && comboScenarioB.getItems().size() > 1) itemB = comboScenarioB.getItems().get(1);
        if (itemB == null) itemB = itemA;

        if (itemA == null) {
            if (mapLabelA != null) mapLabelA.setText(I18n.getOrDefault("analytics.label.scenario_a_none", "Scenario A (None selected)"));
            if (mapLabelB != null) mapLabelB.setText(I18n.getOrDefault("analytics.label.scenario_b_none", "Scenario B (None selected)"));
            if (mapLabelDiff != null) mapLabelDiff.setText(I18n.getOrDefault("analytics.label.scenario_diff", "Discrepancy Heatmap Δ(A - B)"));
            if (mapImageViewA != null) mapImageViewA.setImage(null);
            if (mapImageViewB != null) mapImageViewB.setImage(null);
            if (mapImageViewDiff != null) mapImageViewDiff.setImage(null);
            if (spatialMetricsReportArea != null) spatialMetricsReportArea.setText(I18n.getOrDefault("analytics.prompt.select_2d", "⚠️ Please select at least 2 scenarios in table to launch 2D map comparison."));
            return;
        }

        Scenario scA = itemA.getScenario();
        Scenario scB = (itemB != null) ? itemB.getScenario() : scA;

        if (scA == null) return;
        if (scB == null) scB = scA;

        if (mapLabelA != null) mapLabelA.setText(I18n.getOrDefault("analytics.label.scenario_a_prefix", "Scenario A: ") + itemA.getName());
        if (mapLabelB != null) mapLabelB.setText(I18n.getOrDefault("analytics.label.scenario_b_prefix", "Scenario B: ") + (itemB != null ? itemB.getName() : scB.getName()));
        if (mapLabelDiff != null) mapLabelDiff.setText(I18n.getOrDefault("analytics.label.scenario_diff", "Discrepancy Heatmap Δ(A - B)"));

        int startA = (int) scA.getStartDateYear();
        int endA = (int) scA.getEndDateYear();
        int startB = (int) scB.getStartDateYear();
        int endB = (int) scB.getEndDateYear();

        boolean isGroundTruthA = "HISTORICAL_GROUND_TRUTH".equals(itemA.getRunId());
        boolean isGroundTruthB = (itemB != null) && "HISTORICAL_GROUND_TRUTH".equals(itemB.getRunId());

        String planetA = itemA.getPlanetDisplayName();
        String planetB = (itemB != null) ? itemB.getPlanetDisplayName() : planetA;
        boolean differentPlanets = !planetA.equalsIgnoreCase(planetB);

        double minLatA = itemA.getMinLat(), maxLatA = itemA.getMaxLat();
        double minLngA = itemA.getMinLng(), maxLngA = itemA.getMaxLng();
        double minLatB = (itemB != null) ? itemB.getMinLat() : minLatA;
        double maxLatB = (itemB != null) ? itemB.getMaxLat() : maxLatA;
        double minLngB = (itemB != null) ? itemB.getMinLng() : minLngA;
        double maxLngB = (itemB != null) ? itemB.getMaxLng() : maxLngA;

        double interMinLat = Math.max(minLatA, minLatB);
        double interMaxLat = Math.min(maxLatA, maxLatB);
        double interMinLng = Math.max(minLngA, minLngB);
        double interMaxLng = Math.min(maxLngA, maxLngB);

        boolean spatialDisjoint = differentPlanets || (interMinLat >= interMaxLat) || (interMinLng >= interMaxLng);

        int minYear;
        int maxYear;
        boolean temporalDisjoint = false;

        if (isGroundTruthA && !isGroundTruthB) {
            minYear = startB;
            maxYear = endB;
        } else if (!isGroundTruthA && isGroundTruthB) {
            minYear = startA;
            maxYear = endA;
        } else if (!isGroundTruthA && !isGroundTruthB) {
            minYear = Math.max(startA, startB);
            maxYear = Math.min(endA, endB);
            if (maxYear < minYear) {
                temporalDisjoint = true;
            }
        } else {
            minYear = -100000;
            maxYear = 2026;
        }

        if (spatialDisjoint || temporalDisjoint) {
            if (mapImageViewDiff != null) mapImageViewDiff.setImage(null);
            if (dateSlider != null) dateSlider.setDisable(true);
            if (playTimelineBtn != null) playTimelineBtn.setDisable(true);
            if (spatialMetricsReportArea != null) {
                if (spatialDisjoint) {
                    spatialMetricsReportArea.setText(differentPlanets
                            ? String.format(I18n.getOrDefault("analytics.spatial.disjoint_planets_fmt", "⚠️ Scénarios sur des planètes distinctes (%s vs %s) : aucune intersection spatiale possible. Données comparatives cartographiques directes indisponibles."), planetA, planetB)
                            : String.format(Locale.US, I18n.getOrDefault("analytics.spatial.disjoint_coords_fmt", "⚠️ Scénarios spatialement disjoints : aucune intersection géographique possible ([%.1f°..%.1f° Lat] vs [%.1f°..%.1f° Lat]). Données comparatives cartographiques indisponibles."), minLatA, maxLatA, minLatB, maxLatB));
                } else {
                    spatialMetricsReportArea.setText(String.format(I18n.getOrDefault("analytics.spatial.disjoint_time_fmt", "⚠️ Scénarios temporellement disjoints : aucune intersection temporelle possible (Scénario A : %d ➔ %d | Scénario B : %d ➔ %d). Données comparatives indisponibles."), startA, endA, startB, endB));
                }
            }
            return;
        }

        if (dateSlider != null) dateSlider.setDisable(false);
        if (playTimelineBtn != null) playTimelineBtn.setDisable(false);

        if (maxYear <= minYear) maxYear = minYear + 100;

        if (dateSlider != null) {
            if (minYear != (int) dateSlider.getMin() || maxYear != (int) dateSlider.getMax()) {
                dateSlider.setMin(minYear);
                dateSlider.setMax(maxYear);
                double tickUnit = Math.max(10.0, (maxYear - minYear) / 5.0);
                dateSlider.setMajorTickUnit(tickUnit);
                if (dateSlider.getValue() < minYear || dateSlider.getValue() > maxYear) {
                    dateSlider.setValue(minYear);
                }
                preloadAll2DTensorsAsync(itemA, itemB, minYear, maxYear);
            }
        }

        String rawChannel = spatialChannelCombo != null ? spatialChannelCombo.getValue() : null;
        final String channelKey = resolveCanonicalChannelKey(rawChannel);

        int targetYear = dateSlider != null ? (int) dateSlider.getValue() : 0;
        final long currentReqId = spatialComparisonRequestId.incrementAndGet();

        if (spatialMetricsReportArea != null && (spatialMetricsReportArea.getText() == null || spatialMetricsReportArea.getText().isBlank())) {
            spatialMetricsReportArea.setText(I18n.getOrDefault("analytics.status.loading_tensors", "ℹ️ Loading cartographic tensors for selected scenarios..."));
        }

        final Scenario finalScA = scA;
        final Scenario finalScB = scB;
        final ScenarioSelectableItem finalItemA = itemA;
        final ScenarioSelectableItem finalItemB = itemB;

        String runIdA = finalItemA != null ? finalItemA.getRunId() : "N/A";
        String runIdB = finalItemB != null ? finalItemB.getRunId() : runIdA;
        String checkKeyA = runIdA + "_" + targetYear + "_" + channelKey;
        String checkKeyB = runIdB + "_" + targetYear + "_" + channelKey;
        boolean alreadyCached = RASTER_IMAGE_CACHE.containsKey(checkKeyA) && RASTER_IMAGE_CACHE.containsKey(checkKeyB);

        if (!alreadyCached && mapRegenBox != null) {
            mapRegenBox.setVisible(true);
            mapRegenBox.setManaged(true);
            mapRegenProgress.setProgress(-1);
            mapRegenStatusLabel.setText(I18n.getOrDefault("analytics.status.loading_tensors", "ℹ️ Loading cartographic tensors..."));
        } else if (alreadyCached && mapRegenBox != null) {
            mapRegenBox.setVisible(false);
            mapRegenBox.setManaged(false);
        }

        CompletableFuture.runAsync(() -> {
            BufferedImage bufA = null;
            BufferedImage bufB = null;
            BufferedImage bufDiff = null;

            try {
                SimulationRunRecord runA = runResolver != null ? runResolver.apply(finalItemA) : null;
                SimulationRunRecord runB = runResolver != null ? runResolver.apply(finalItemB) : null;

                List<H3Cell> snapshotCellsA = runA != null ? runA.getSpatialSnapshotAt(targetYear) : null;
                List<H3Cell> snapshotCellsB = runB != null ? runB.getSpatialSnapshotAt(targetYear) : null;

                String cacheKeyA = checkKeyA;
                String cacheKeyB = checkKeyB;
                int mapsToGenerate = 0;
                if (!RASTER_IMAGE_CACHE.containsKey(cacheKeyA)) mapsToGenerate++;
                if (!RASTER_IMAGE_CACHE.containsKey(cacheKeyB) && !cacheKeyA.equals(cacheKeyB)) mapsToGenerate++;

                final int totalMapsNeeded = mapsToGenerate;
                if (totalMapsNeeded > 0) {
                    Platform.runLater(() -> {
                        if (mapRegenBox != null && mapRegenStatusLabel != null) {
                            mapRegenBox.setVisible(true);
                            mapRegenBox.setManaged(true);
                            mapRegenProgress.setProgress(0.1);
                            mapRegenStatusLabel.setText(String.format(I18n.getOrDefault("analytics.status.regen_maps_fmt", "⚡ Regenerating 2D maps: %d / %d"), 1, totalMapsNeeded));
                        }
                    });
                }

                // 1. Channel A Raster Extraction with In-Memory Cache
                bufA = RASTER_IMAGE_CACHE.get(cacheKeyA);
                if (bufA == null) {
                    File diskRasterA = getDiskRasterForChannel(targetYear, channelKey);
                    if ("HISTORICAL_GROUND_TRUTH".equals(runIdA) && diskRasterA != null && diskRasterA.exists()) {
                        String filePathKey = "FILE_" + diskRasterA.getAbsolutePath();
                        bufA = RASTER_IMAGE_CACHE.get(filePathKey);
                        if (bufA == null) {
                            bufA = ImageIO.read(diskRasterA);
                            if (bufA != null) RASTER_IMAGE_CACHE.put(filePathKey, bufA);
                        }
                    } else if (snapshotCellsA != null && !snapshotCellsA.isEmpty()) {
                        bufA = rasterizeCellsToImage(snapshotCellsA, channelKey, 512, 256);
                    } else if (diskRasterA != null && diskRasterA.exists()) {
                        String filePathKey = "FILE_" + diskRasterA.getAbsolutePath();
                        bufA = RASTER_IMAGE_CACHE.get(filePathKey);
                        if (bufA == null) {
                            bufA = ImageIO.read(diskRasterA);
                            if (bufA != null) RASTER_IMAGE_CACHE.put(filePathKey, bufA);
                        }
                    } else if ("density".equals(channelKey)) {
                        bufA = HistoricalMapGenerator.rasterizeDensityMapForYear(finalScA.getPopulationDensityType(), finalScA, targetYear);
                    } else {
                        HistoricalMapGenerator.populateScenarioHistoricalMaps(finalScA);
                        bufA = base64ToBufferedImage(extractChannelBase64(finalScA, channelKey));
                    }
                    if (bufA != null) {
                        RASTER_IMAGE_CACHE.put(cacheKeyA, bufA);
                    }
                }

                // 2. Channel B Raster Extraction with In-Memory Cache
                if ("HISTORICAL_GROUND_TRUTH".equals(runIdA) && "HISTORICAL_GROUND_TRUTH".equals(runIdB) && bufA != null) {
                    bufB = bufA;
                } else {
                    bufB = RASTER_IMAGE_CACHE.get(cacheKeyB);
                    if (bufB == null) {
                        File diskRasterB = getDiskRasterForChannel(targetYear, channelKey);
                        if ("HISTORICAL_GROUND_TRUTH".equals(runIdB) && diskRasterB != null && diskRasterB.exists()) {
                            String filePathKey = "FILE_" + diskRasterB.getAbsolutePath();
                            bufB = RASTER_IMAGE_CACHE.get(filePathKey);
                            if (bufB == null) {
                                bufB = ImageIO.read(diskRasterB);
                                if (bufB != null) RASTER_IMAGE_CACHE.put(filePathKey, bufB);
                            }
                        } else if (snapshotCellsB != null && !snapshotCellsB.isEmpty()) {
                            int w = computeOptimalRasterWidth(snapshotCellsB.size());
                            bufB = rasterizeCellsToImage(snapshotCellsB, channelKey, w, w / 2);
                        } else if (diskRasterB != null && diskRasterB.exists()) {
                            String filePathKey = "FILE_" + diskRasterB.getAbsolutePath();
                            bufB = RASTER_IMAGE_CACHE.get(filePathKey);
                            if (bufB == null) {
                                bufB = ImageIO.read(diskRasterB);
                                if (bufB != null) RASTER_IMAGE_CACHE.put(filePathKey, bufB);
                            }
                        } else if ("density".equals(channelKey)) {
                            bufB = HistoricalMapGenerator.rasterizeDensityMapForYear(finalScB.getPopulationDensityType(), finalScB, targetYear);
                        } else {
                            HistoricalMapGenerator.populateScenarioHistoricalMaps(finalScB);
                            bufB = base64ToBufferedImage(extractChannelBase64(finalScB, channelKey));
                        }
                        if (bufB != null) {
                            RASTER_IMAGE_CACHE.put(cacheKeyB, bufB);
                        }
                    }
                }

                if (bufA != null && bufB != null) {
                    bufDiff = MapComparisonMetrics.generateDiscrepancyHeatmap(bufA, bufB);
                }
            } catch (Exception ex) {
                logger.warn("Could not generate 2D comparison maps for target year {}: {}", targetYear, ex.getMessage());
            }

            if (currentReqId != spatialComparisonRequestId.get()) return;

            final Image imgFxA = bufferedImageToFxImage(bufA);
            final Image imgFxB = bufferedImageToFxImage(bufB);
            final Image imgFxDiff = bufferedImageToFxImage(bufDiff);

            final MapComparisonMetrics.MapComparisonResult metrics = (bufA != null && bufB != null)
                    ? MapComparisonMetrics.compareImages(bufA, bufB) : null;

            Platform.runLater(() -> {
                if (currentReqId != spatialComparisonRequestId.get()) return;

                if (mapImageViewA != null) mapImageViewA.setImage(imgFxA);
                if (mapImageViewB != null) mapImageViewB.setImage(imgFxB);
                if (mapImageViewDiff != null) mapImageViewDiff.setImage(imgFxDiff);

                if (metrics != null && spatialMetricsReportArea != null) {
                    String pearsonEval = metrics.getPearsonR() >= 0.85
                            ? "✅ (" + I18n.getOrDefault("analytics.spatial.high_fidelity", "High Fidelity") + ")"
                            : "⚠️ (" + I18n.getOrDefault("analytics.spatial.divergence_detected", "Divergence Detected") + ")";

                    spatialMetricsReportArea.setText(String.format(
                            "📊 %s (Year %d AD - %s)\n" +
                            "------------------------------------------------------------------------\n" +
                            "• %s : %.4f\n" +
                            "• %s : %.4f %s\n" +
                            "• %s : %.4f\n" +
                            "• %s : %.4f\n" +
                            "• %s : %.4f\n" +
                            "• %s : %.4f\n" +
                            "• %s : %.4f (%s)\n" +
                            "------------------------------------------------------------------------\n" +
                            "%s",
                            I18n.getOrDefault("analytics.spatial.metrics_header", "2D CARTOGRAPHIC FIDELITY METRICS"),
                            targetYear,
                            channelKey,
                            I18n.getOrDefault("analytics.spatial.rmse", "Spatial Root Mean Square Error (RMSE)"), metrics.getRmse(),
                            I18n.getOrDefault("analytics.spatial.pearson", "Spatial Pearson Correlation (r)"), metrics.getPearsonR(), pearsonEval,
                            I18n.getOrDefault("analytics.spatial.ssim", "2D Structural Similarity (SSIM)"), metrics.getSsim(),
                            I18n.getOrDefault("analytics.spatial.jaccard", "Categorical Jaccard Overlap Index"), metrics.getJaccardIndex(),
                            I18n.getOrDefault("analytics.spatial.dice", "Dice Overlap Coefficient"), metrics.getDiceCoefficient(),
                            I18n.getOrDefault("analytics.spatial.kl", "Spatial Entropy KL Divergence"), metrics.getKlDivergence(),
                            I18n.getOrDefault("analytics.spatial.max_delta", "Maximum Spatial Discrepancy"), metrics.getMaxDeltaValue(),
                            String.format(I18n.getOrDefault("analytics.spatial.coords_fmt", "Lat: %.2f°, Lng: %.2f°"), metrics.getMaxDeltaLat(), metrics.getMaxDeltaLng()),
                            metrics.getFormattedReport()
                    ));
                } else if (spatialMetricsReportArea != null) {
                    spatialMetricsReportArea.setText(I18n.getOrDefault("analytics.status.tensor_unavailable", "⚠️ Cartographic tensor data unavailable for the selected epoch and channel."));
                }

                if (mapRegenBox != null) {
                    mapRegenBox.setVisible(false);
                    mapRegenBox.setManaged(false);
                }
            });
        });
    }

    private void preloadAll2DTensorsAsync(ScenarioSelectableItem itemA, ScenarioSelectableItem itemB, int startYear, int endYear) {
        if (itemA == null && itemB == null) return;
        if (playTimelineBtn != null) {
            playTimelineBtn.setDisable(true);
        }
        if (mapRegenBox != null) {
            mapRegenBox.setVisible(true);
            mapRegenBox.setManaged(true);
            mapRegenProgress.setProgress(0.0);
            mapRegenStatusLabel.setText(I18n.getOrDefault("analytics.status.preloading_frames", "⏳ Preloading 2D temporal frames..."));
        }

        preloadedTimelineYears.clear();
        CompletableFuture.runAsync(() -> {
            try {
                SimulationRunRecord runA = runResolver != null ? runResolver.apply(itemA) : null;
                SimulationRunRecord runB = runResolver != null ? runResolver.apply(itemB) : null;
                int step = Math.max(1, (endYear - startYear) / 12);
                String channelKey = resolveCanonicalChannelKey(spatialChannelCombo != null ? spatialChannelCombo.getValue() : null);

                int totalFrames = ((endYear - startYear) / step) + 1;
                int count = 0;

                for (int yr = startYear; yr <= endYear; yr += step) {
                    preloadedTimelineYears.add(yr);
                    count++;
                    final double progress = (double) count / (double) Math.max(1, totalFrames);
                    final int currentYr = yr;
                    Platform.runLater(() -> {
                        if (mapRegenProgress != null) mapRegenProgress.setProgress(progress);
                        if (mapRegenStatusLabel != null) mapRegenStatusLabel.setText(String.format(I18n.getOrDefault("analytics.status.caching_frame_fmt", "⚡ Caching: Year %d AD (%d%%)"), currentYr, (int) (progress * 100)));
                    });

                    if (itemA != null) {
                        String runIdA = itemA.getRunId();
                        String keyA = runIdA + "_" + yr + "_" + channelKey;
                        if (!RASTER_IMAGE_CACHE.containsKey(keyA)) {
                            BufferedImage imgA = null;
                            List<H3Cell> cellsA = runA != null ? runA.getSpatialSnapshotAt(yr) : null;
                            if (cellsA != null && !cellsA.isEmpty()) {
                                int wA = computeOptimalRasterWidth(cellsA.size());
                                imgA = rasterizeCellsToImage(cellsA, channelKey, wA, wA / 2);
                            } else {
                                File diskA = getDiskRasterForChannel(yr, channelKey);
                                if (diskA != null && diskA.exists()) imgA = ImageIO.read(diskA);
                            }
                            if (imgA != null) RASTER_IMAGE_CACHE.put(keyA, imgA);
                        }
                    }

                    if (itemB != null) {
                        String runIdB = itemB.getRunId();
                        String keyB = runIdB + "_" + yr + "_" + channelKey;
                        if (!RASTER_IMAGE_CACHE.containsKey(keyB)) {
                            BufferedImage imgB = null;
                            List<H3Cell> cellsB = runB != null ? runB.getSpatialSnapshotAt(yr) : null;
                            if (cellsB != null && !cellsB.isEmpty()) {
                                int wB = computeOptimalRasterWidth(cellsB.size());
                                imgB = rasterizeCellsToImage(cellsB, channelKey, wB, wB / 2);
                            } else {
                                File diskB = getDiskRasterForChannel(yr, channelKey);
                                if (diskB != null && diskB.exists()) imgB = ImageIO.read(diskB);
                            }
                            if (imgB != null) RASTER_IMAGE_CACHE.put(keyB, imgB);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warn("Could not preload 2D frames: {}", ex.getMessage());
            }

            Platform.runLater(() -> {
                if (mapRegenBox != null) {
                    mapRegenBox.setVisible(false);
                    mapRegenBox.setManaged(false);
                }
                if (playTimelineBtn != null) {
                    playTimelineBtn.setDisable(false);
                }
            });
        });
    }

    /**
     * Determines optimal raster resolution based on total cell count to maximize visual fidelity.
     *
     * @param cellCount simulation cell count
     * @return recommended image width in pixels
     */
    public static int computeOptimalRasterWidth(int cellCount) {
        if (cellCount > 10_000_000) {
            long maxMem = Runtime.getRuntime().maxMemory();
            if (maxMem >= 8L * 1024 * 1024 * 1024) {
                return 8192;
            }
            return 4096;
        } else if (cellCount > 1_500_000) {
            return 4096;
        } else if (cellCount > 10_000) {
            return 2048;
        } else {
            return 1024;
        }
    }

    private BufferedImage rasterizeCellsToImage(List<H3Cell> cells, String channel, int width, int height) {
        if (cells == null || cells.isEmpty()) return null;
        int targetW = width > 0 ? width : computeOptimalRasterWidth(cells.size());
        int targetH = height > 0 ? height : targetW / 2;

        BufferedImage img = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(15, 23, 42));
        g2.fillRect(0, 0, targetW, targetH);

        int r = Math.max(1, (int) Math.round(targetW / Math.sqrt(cells.size() * 3.5)));
        if (r > 15) r = 15;

        String chLower = channel != null ? channel.toLowerCase(Locale.ROOT) : "";

        for (H3Cell c : cells) {
            double lat = c.getLatitude();
            double lng = c.getLongitude();
            int x = (int) Math.round(((lng + 180.0) / 360.0) * targetW);
            int y = (int) Math.round(((90.0 - lat) / 180.0) * targetH);

            Color col;
            if (chLower.contains("densité") || chLower.contains("demographic") || chLower.contains("density") || chLower.contains("人口")) {
                long pop = c.getPopulation() != null ? c.getPopulation() : 0;
                if (pop > 0) {
                    double norm = Math.min(1.0, Math.log10(pop + 1.0) / 6.0);
                    col = getDensityColor(norm);
                } else {
                    col = new Color(30, 41, 59);
                }
            } else if (chLower.contains("technologie") || chLower.contains("technology") || chLower.contains("outillage") || chLower.contains("科技")) {
                double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
                double norm = Math.min(1.0, tech / 100.0);
                col = getTechColor(norm);
            } else if (chLower.contains("température") || chLower.contains("temperature") || chLower.contains("climat") || chLower.contains("气温")) {
                double temp = c.getTemperature() != null ? c.getTemperature() : 15.0;
                double norm = Math.max(0.0, Math.min(1.0, (temp + 20.0) / 60.0));
                col = getTemperatureColor(norm);
            } else if (chLower.contains("aquifère") || chLower.contains("aquifer") || chLower.contains("eau") || chLower.contains("water") || chLower.contains("含水层")) {
                double aqua = c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer() : 50.0;
                double norm = Math.max(0.0, Math.min(1.0, aqua / 100.0));
                col = getAquiferColor(norm);
            } else if (chLower.contains("biomasse") || chLower.contains("biomass") || chLower.contains("agricole") || chLower.contains("agriculture") || chLower.contains("农业")) {
                double bio = c.getBiomassAgriculture() != null ? c.getBiomassAgriculture() : 0.0;
                double norm = Math.max(0.0, Math.min(1.0, bio / 1000.0));
                col = getAgricultureColor(norm);
            } else if (chLower.contains("souveraineté") || chLower.contains("sovereignty") || chLower.contains("主权")) {
                long polity = (c.getLanguageGroup() != null) ? (long) Math.abs(c.getLanguageGroup().hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode()) : 0L);
                col = getPolityColor(polity);
            } else if (chLower.contains("parent") || chLower.contains("kinship") || chLower.contains("famill") || chLower.contains("亲属")) {
                long hash = c.getLanguageGroup() != null ? (long) Math.abs((c.getLanguageGroup() + "_kin").hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode() * 31) : 0L);
                col = getPolityColor(hash);
            } else if (chLower.contains("rituel") || chLower.contains("ritual") || chLower.contains("croyan") || chLower.contains("belief") || chLower.contains("信仰")) {
                long hash = c.getLanguageGroup() != null ? (long) Math.abs((c.getLanguageGroup() + "_rit").hashCode()) : (c.getId() != null ? (long) Math.abs(c.getId().hashCode() * 73) : 0L);
                col = getPolityColor(hash);
            } else if (chLower.contains("commerc") || chLower.contains("trade") || chLower.contains("route") || chLower.contains("贸易")) {
                double friction = c.getMovementFriction() != null ? c.getMovementFriction() : 1.0;
                double norm = Math.max(0.0, Math.min(1.0, 1.0 - (friction / 5.0)));
                col = getTechColor(norm);
            } else if (chLower.contains("institut") || chLower.contains("seshat") || chLower.contains("制度")) {
                double tech = c.getTechnologyLevel() != null ? c.getTechnologyLevel() : 1.0;
                col = getTechColor(Math.min(1.0, tech / 80.0));
            } else if (chLower.contains("écolog") || chLower.contains("ecolog") || chLower.contains("empreint") || chLower.contains("footprint") || chLower.contains("生态")) {
                double soc = c.getSoilOrganicCarbon() != null ? c.getSoilOrganicCarbon() : 50.0;
                col = getAgricultureColor(Math.max(0.0, Math.min(1.0, soc / 100.0)));
            } else if (chLower.contains("pathog") || chLower.contains("sanit") || chLower.contains("immunit") || chLower.contains("health") || chLower.contains("zoonot") || chLower.contains("病原")) {
                int recovered = c.getEpidemicRecovered() != null ? c.getEpidemicRecovered() : 0;
                double norm = Math.min(1.0, recovered / 500.0);
                col = getDensityColor(norm);
            } else {
                long customHash = Math.abs((chLower + "_" + (c.getLanguageGroup() != null ? c.getLanguageGroup() : c.getId())).hashCode());
                col = getPolityColor(customHash);
            }

            g2.setColor(col);
            g2.fillOval(x - r, y - r, r * 2, r * 2);
        }
        g2.dispose();
        return img;
    }

    private Color getTemperatureColor(double norm) {
        int r = (int) (norm * 255);
        int g = (int) ((1.0 - Math.abs(norm - 0.5) * 2.0) * 200);
        int b = (int) ((1.0 - norm) * 255);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    private Color getAquiferColor(double norm) {
        int r = (int) (20 + norm * 30);
        int g = (int) (100 + norm * 140);
        int b = (int) (200 + norm * 55);
        return new Color(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    private Color getAgricultureColor(double norm) {
        int r = (int) (120 - norm * 80);
        int g = (int) (140 + norm * 100);
        int b = (int) (30 + norm * 20);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    private Color getDensityColor(double norm) {
        if (norm < 0.33) {
            return new Color(16, 185, 129);
        } else if (norm < 0.66) {
            return new Color(245, 158, 11);
        } else {
            return new Color(239, 68, 68);
        }
    }

    private Color getTechColor(double norm) {
        int r = (int) (56 + norm * (245 - 56));
        int g = (int) (189 - norm * 80);
        int b = (int) (248 - norm * 150);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)));
    }

    private Color getPolityColor(long id) {
        if (id <= 0) return new Color(100, 116, 139);
        return Color.getHSBColor((float) ((id * 0.618033988749895) % 1.0), 0.75f, 0.85f);
    }

    /**
     * Resolves human-readable or localized layer names into canonical disk layer keys.
     *
     * @param channel raw channel descriptor
     * @return canonical key (e.g., "density", "technology", "temperature")
     */
    public static String resolveCanonicalChannelKey(String channel) {
        if (channel == null || channel.isBlank()) {
            return "density";
        }
        String ch = channel.trim();
        if (ch.contains("(") && ch.contains(")")) {
            int openIdx = ch.lastIndexOf('(');
            int closeIdx = ch.lastIndexOf(')');
            if (openIdx >= 0 && closeIdx > openIdx) {
                String candidate = ch.substring(openIdx + 1, closeIdx).trim().toLowerCase(Locale.ROOT);
                if (candidate.startsWith("custom_tensor_") || candidate.startsWith("tensor_")) {
                    return candidate.replaceAll("[^a-z0-9_]+", "_");
                }
            }
        }
        String chLower = ch.toLowerCase(Locale.ROOT);
        if (chLower.contains("technolog") || chLower.contains("outillage") || chLower.contains("科技")) return "technology";
        if (chLower.contains("températ") || chLower.contains("temperat") || chLower.contains("climat") || chLower.contains("气温")) return "temperature";
        if (chLower.contains("aquif") || chLower.contains("eau") || chLower.contains("water") || chLower.contains("含水层")) return "aquifers";
        if (chLower.contains("biom") || chLower.contains("agricul") || chLower.contains("biomass") || chLower.contains("农业")) return "biomes";
        if (chLower.contains("souverain") || chLower.contains("sovereign") || chLower.contains("politi") || chLower.contains("fronti") || chLower.contains("border") || chLower.contains("主权")) return "sovereignty";
        if (chLower.contains("isogloss") || chLower.contains("linguist") || chLower.contains("lang") || chLower.contains("语言")) return "isogloss";
        if (chLower.contains("parent") || chLower.contains("kinship") || chLower.contains("famill") || chLower.contains("亲属")) return "kinship";
        if (chLower.contains("rituel") || chLower.contains("ritual") || chLower.contains("croyan") || chLower.contains("belief") || chLower.contains("信仰")) return "rituals";
        if (chLower.contains("commerc") || chLower.contains("trade") || chLower.contains("route") || chLower.contains("贸易")) return "tradenetwork";
        if (chLower.contains("institut") || chLower.contains("seshat") || chLower.contains("制度")) return "institutional";
        if (chLower.contains("écolog") || chLower.contains("ecolog") || chLower.contains("empreint") || chLower.contains("footprint") || chLower.contains("生态")) return "ecological";
        if (chLower.contains("pathog") || chLower.contains("sanit") || chLower.contains("immunit") || chLower.contains("health") || chLower.contains("zoonot") || chLower.contains("病原")) return "pathogen";
        if (chLower.contains("précipit") || chLower.contains("precipit") || chLower.contains("pluie") || chLower.contains("rainfall") || chLower.contains("降水")) return "precipitation";
        if (chLower.contains("élévat") || chLower.contains("elevat") || chLower.contains("altitude") || chLower.contains("relief") || chLower.contains("海拔")) return "elevation";
        if (chLower.contains("saison") || chLower.contains("season") || chLower.contains("季节")) return "seasonality";

        String customSlug = chLower.replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        return customSlug.isBlank() ? "density" : customSlug;
    }

    private File getDiskRasterForChannel(int year, String channel) {
        String baseMapDir = "data/maps/ether/earth/" + year + "/";
        String channelKey = resolveCanonicalChannelKey(channel);
        File rasterFile = new File(baseMapDir, "earth_" + year + "_" + channelKey + ".png");
        if (rasterFile.exists()) return rasterFile;

        File dir = new File(baseMapDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] matches = dir.listFiles((d, name) -> {
                String n = name.toLowerCase(Locale.ROOT);
                return n.endsWith(".png") && (n.contains(channelKey) || n.endsWith("_" + channelKey + ".png"));
            });
            if (matches != null && matches.length > 0) {
                return matches[0];
            }
        }

        if (AVAILABLE_EARTH_EPOCHS.length > 0) {
            int idx = Arrays.binarySearch(AVAILABLE_EARTH_EPOCHS, year);
            int closestYear;
            if (idx >= 0) {
                closestYear = AVAILABLE_EARTH_EPOCHS[idx];
            } else {
                int ins = -idx - 1;
                if (ins == 0) {
                    closestYear = AVAILABLE_EARTH_EPOCHS[0];
                } else if (ins >= AVAILABLE_EARTH_EPOCHS.length) {
                    closestYear = AVAILABLE_EARTH_EPOCHS[AVAILABLE_EARTH_EPOCHS.length - 1];
                } else {
                    int prev = AVAILABLE_EARTH_EPOCHS[ins - 1];
                    int next = AVAILABLE_EARTH_EPOCHS[ins];
                    closestYear = (Math.abs(prev - year) <= Math.abs(next - year)) ? prev : next;
                }
            }
            File candidate = new File("data/maps/ether/earth/" + closestYear + "/earth_" + closestYear + "_" + channelKey + ".png");
            if (candidate.exists()) return candidate;

            File closestDir = new File("data/maps/ether/earth/" + closestYear + "/");
            if (closestDir.exists() && closestDir.isDirectory()) {
                File[] matches = closestDir.listFiles((d, name) -> {
                    String n = name.toLowerCase(Locale.ROOT);
                    return n.endsWith(".png") && (n.contains(channelKey) || n.endsWith("_" + channelKey + ".png"));
                });
                if (matches != null && matches.length > 0) {
                    return matches[0];
                }
            }
        }
        return null;
    }

    private String extractChannelBase64(Scenario sc, String channel) {
        if (sc == null || channel == null) return null;
        String chLower = channel.toLowerCase(Locale.ROOT);
        if (chLower.contains("souveraineté") || chLower.contains("sovereignty")) return sc.getCustomTensorMapBase64(3);
        if (chLower.contains("isoglosses") || chLower.contains("linguistique") || chLower.contains("linguistic") || chLower.contains("langues")) return sc.getCustomTensorMapBase64(0);
        if (chLower.contains("parenté") || chLower.contains("kinship")) return sc.getCustomTensorMapBase64(1);
        if (chLower.contains("rituels") || chLower.contains("rituals") || chLower.contains("croyances")) return sc.getCustomTensorMapBase64(2);
        if (chLower.contains("technologie") || chLower.contains("technology") || chLower.contains("artefacts") || chLower.contains("outillage")) return sc.getCustomTensorMapBase64(4);
        if (chLower.contains("commerce") || chLower.contains("trade") || chLower.contains("corridors")) return sc.getCustomTensorMapBase64(5);
        if (chLower.contains("institution") || chLower.contains("institutional") || chLower.contains("seshat")) return sc.getCustomTensorMapBase64(6);
        if (chLower.contains("écologique") || chLower.contains("ecological") || chLower.contains("empreinte")) return sc.getCustomTensorMapBase64(7);
        if (chLower.contains("pathogène") || chLower.contains("pathogen") || chLower.contains("immunité") || chLower.contains("santé")) return sc.getCustomTensorMapBase64(8);
        return sc.getCustomDensityBase64();
    }

    private Image bufferedImageToFxImage(BufferedImage buf) {
        if (buf == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buf, "PNG", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage base64ToBufferedImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    private void toggleDateAnimation() {
        if (isPlayingAnimation) {
            stopAnimation();
        } else {
            if (preloadedTimelineYears.isEmpty() && dateSlider != null) {
                preloadedTimelineYears.add((int) dateSlider.getValue());
            }
            isPlayingAnimation = true;
            if (playTimelineBtn != null) playTimelineBtn.setText(I18n.getOrDefault("analytics.btn.pause_timeline", "⏸️ Pause"));
            currentTimelineFrameIndex = 0;
            if (dateSlider != null && !preloadedTimelineYears.isEmpty()) {
                int curVal = (int) dateSlider.getValue();
                for (int i = 0; i < preloadedTimelineYears.size(); i++) {
                    if (preloadedTimelineYears.get(i) >= curVal) {
                        currentTimelineFrameIndex = i;
                        break;
                    }
                }
            }
            timelineAnimation = new Timeline(new KeyFrame(Duration.millis(350), evt -> {
                if (dateSlider != null && !preloadedTimelineYears.isEmpty()) {
                    currentTimelineFrameIndex = (currentTimelineFrameIndex + 1) % preloadedTimelineYears.size();
                    int targetYr = preloadedTimelineYears.get(currentTimelineFrameIndex);
                    dateSlider.setValue(targetYr);
                }
            }));
            timelineAnimation.setCycleCount(Timeline.INDEFINITE);
            timelineAnimation.play();
        }
    }

    /**
     * Halts timeline playback animation.
     */
    public void stopAnimation() {
        if (timelineAnimation != null) {
            timelineAnimation.stop();
        }
        isPlayingAnimation = false;
        if (playTimelineBtn != null) {
            playTimelineBtn.setText(I18n.getOrDefault("analytics.btn.play_timeline", "▶️ Play Timeline"));
        }
    }

    private void export2DCartographyZip() {
        ScenarioSelectableItem itemA = comboScenarioA != null ? comboScenarioA.getValue() : null;
        ScenarioSelectableItem itemB = comboScenarioB != null ? comboScenarioB.getValue() : null;
        if (itemA == null && comboScenarioA != null && !comboScenarioA.getItems().isEmpty()) itemA = comboScenarioA.getItems().get(0);
        if (itemB == null && comboScenarioB != null && comboScenarioB.getItems().size() > 1) itemB = comboScenarioB.getItems().get(1);
        if (itemB == null) itemB = itemA;
        if (itemA == null) return;

        final int targetYear = dateSlider != null ? (int) dateSlider.getValue() : 0;
        final String spatialReportText = spatialMetricsReportArea != null ? spatialMetricsReportArea.getText() : "";
        final String channelKey = resolveCanonicalChannelKey(spatialChannelCombo != null ? spatialChannelCombo.getValue() : null);

        ComparativeAnalyticsExporter.export2DCartographyZip(
                export2DZipBtn,
                itemA,
                itemB,
                targetYear,
                channelKey,
                spatialReportText,
                mapImageViewA != null ? mapImageViewA.getImage() : null,
                mapImageViewB != null ? mapImageViewB.getImage() : null,
                mapImageViewDiff != null ? mapImageViewDiff.getImage() : null,
                RASTER_IMAGE_CACHE
        );
    }

    /**
     * Updates all localized labels, tooltips, and combo items.
     *
     * @param scenarioList active scenario list for dynamic custom tensor channels
     */
    public void updateTexts(List<ScenarioSelectableItem> scenarioList) {
        isUpdatingTexts = true;
        try {
            if (channelLabel != null) channelLabel.setText(I18n.getOrDefault("analytics.channel_label", "Tensor Channel:"));
            if (scenarioSelectLabelA != null) scenarioSelectLabelA.setText(I18n.getOrDefault("analytics.label.select_a", "Baseline (A):"));
            if (scenarioSelectLabelB != null) scenarioSelectLabelB.setText(I18n.getOrDefault("analytics.label.select_b", "Target (B):"));

            if (playTimelineBtn != null) {
                playTimelineBtn.setText(isPlayingAnimation
                        ? I18n.getOrDefault("analytics.btn.pause_timeline", "⏸️ Pause")
                        : I18n.getOrDefault("analytics.btn.play_timeline", "▶️ Play Timeline"));
            }

            if (export2DZipBtn != null) {
                export2DZipBtn.setText(I18n.getOrDefault("analytics.btn.export_2d_zip", "📦 Export 2D Tensors (.zip)"));
                export2DZipBtn.setTooltip(new Tooltip(I18n.getOrDefault("analytics.tooltip.export_2d_zip", "Export all 2D comparative maps and spatial metrics report into a timestamped ZIP archive.")));
            }

            if (legendBlue != null) legendBlue.setText("🟦 " + I18n.getOrDefault("analytics.legend.under", "A < B (Underestimation)"));
            if (legendSlate != null) legendSlate.setText("⬛ " + I18n.getOrDefault("analytics.legend.zero", "Δ = 0 (Identical / Zero divergence)"));
            if (legendGreen != null) legendGreen.setText("🟩 " + I18n.getOrDefault("analytics.legend.close", "|Δ| ≤ 5% (Close agreement)"));
            if (legendRed != null) legendRed.setText("🟥 " + I18n.getOrDefault("analytics.legend.over", "A > B (Overestimation)"));

            if (spatialChannelCombo != null) {
                int selectedIdx = spatialChannelCombo.getSelectionModel().getSelectedIndex();
                if (selectedIdx < 0) selectedIdx = 0;
                var oldAction = spatialChannelCombo.getOnAction();
                spatialChannelCombo.setOnAction(null);
                spatialChannelCombo.getItems().clear();
                spatialChannelCombo.getItems().addAll(
                        I18n.getOrDefault("analytics.spatial.density", "👥 Demographic Density"),
                        I18n.getOrDefault("analytics.spatial.technology", "🔬 Technology & Tooling Level"),
                        I18n.getOrDefault("analytics.spatial.temperature", "🌡️ Surface Temperature & Climate"),
                        I18n.getOrDefault("analytics.spatial.aquifers", "💧 Aquifers & Freshwater Tables"),
                        I18n.getOrDefault("analytics.spatial.agriculture", "🌾 Agricultural Biomass & Soils"),
                        I18n.getOrDefault("analytics.spatial.sovereignty", "👑 Political Sovereignty & Borders"),
                        I18n.getOrDefault("analytics.spatial.linguistic", "🗣️ Linguistic Isoglosses (Languages)"),
                        I18n.getOrDefault("analytics.spatial.kinship", "🧬 Kinship & Family Structures"),
                        I18n.getOrDefault("analytics.spatial.rituals", "🔮 Sacred Beliefs & Ritual Practices"),
                        I18n.getOrDefault("analytics.spatial.trade", "🛣️ Trade Corridors & Exchange Routes"),
                        I18n.getOrDefault("analytics.spatial.institutional", "⚖️ Institutional Complexity (Seshat)"),
                        I18n.getOrDefault("analytics.spatial.ecological", "⚠️ Ecological Footprint & Overshoot"),
                        I18n.getOrDefault("analytics.spatial.pathogen", "🧬 Pathogen Burden & Zoonotic Risk")
                );

                Set<String> customChannels = new LinkedHashSet<>();
                if (scenarioList != null) {
                    for (ScenarioSelectableItem item : scenarioList) {
                        if (item != null && item.getScenario() != null) {
                            int dims = item.getScenario().getCultureVectorDimensions();
                            var customNames = item.getScenario().getCustomTensorNames();
                            int maxDim = Math.max(dims, (customNames != null && !customNames.isEmpty()) ? customNames.keySet().stream().mapToInt(k -> k + 1).max().orElse(0) : 0);
                            for (int tIdx = 9; tIdx < maxDim; tIdx++) {
                                int oneBased = tIdx + 1;
                                String rawName = customNames != null ? customNames.get(tIdx) : null;
                                boolean hasCustomName = rawName != null && !rawName.isBlank();
                                String displayName = hasCustomName ? rawName.trim() : (I18n.getOrDefault("scenario.tensor.custom.fallback", "Custom Layer") + " " + oneBased);
                                String sanitized = displayName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
                                String slug = "custom_tensor_" + String.format("%02d", oneBased) + (sanitized.isBlank() ? "" : "_" + sanitized);
                                customChannels.add("🧬 " + I18n.getOrDefault("scenario.tensor.label_prefix", "Tensor ") + "[" + oneBased + "]: " + displayName + " (" + slug + ")");
                            }
                        }
                    }
                }
                for (String customCh : customChannels) {
                    if (!spatialChannelCombo.getItems().contains(customCh)) {
                        spatialChannelCombo.getItems().add(customCh);
                    }
                }

                if (selectedIdx >= spatialChannelCombo.getItems().size()) selectedIdx = 0;
                spatialChannelCombo.getSelectionModel().select(selectedIdx);
                spatialChannelCombo.setOnAction(oldAction);
            }
        } finally {
            isUpdatingTexts = false;
        }

        update2DSpatialComparison();
    }

    public ComboBox<ScenarioSelectableItem> getComboScenarioA() {
        return comboScenarioA;
    }

    public ComboBox<ScenarioSelectableItem> getComboScenarioB() {
        return comboScenarioB;
    }

    public ComboBox<String> getSpatialChannelCombo() {
        return spatialChannelCombo;
    }

    public Slider getDateSlider() {
        return dateSlider;
    }

    public ImageView getMapImageViewA() {
        return mapImageViewA;
    }

    public ImageView getMapImageViewB() {
        return mapImageViewB;
    }

    public ImageView getMapImageViewDiff() {
        return mapImageViewDiff;
    }

    public TextArea getSpatialMetricsReportArea() {
        return spatialMetricsReportArea;
    }

    public Button getExport2DZipBtn() {
        return export2DZipBtn;
    }

    /**
     * Clears in-memory raster cache.
     */
    public static void clearRasterCache() {
        RASTER_IMAGE_CACHE.clear();
    }
}
