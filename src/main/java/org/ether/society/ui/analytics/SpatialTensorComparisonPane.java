/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.export.ComparativeAnalyticsExporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.ether.society.ui.analytics.SpatialTensorDiskRegistry.RASTER_IMAGE_CACHE;

/**
 * 2D Spatial Tensor &amp; Cartographic Comparison Pane.
 *
 * <p>Provides side-by-side raster visualization, discrepancy heatmapping \(\Delta(A - B)\),
 * chronological timeline scrubbing with interactive playback, multi-channel physical and
 * cliodynamic tensor analysis, and spatial fidelity metrics (SSIM, RMSE, Pearson \(r\),
 * Jaccard index, and Kullback-Leibler divergence).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class SpatialTensorComparisonPane extends VBox {

    private final Function<ScenarioSelectableItem, SimulationRunRecord> runResolver;

    Label channelLabel;
    ComboBox<String> spatialChannelCombo;
    Label scenarioSelectLabelA;
    ComboBox<ScenarioSelectableItem> comboScenarioA;
    Label scenarioSelectLabelB;
    ComboBox<ScenarioSelectableItem> comboScenarioB;

    Slider dateSlider;
    Label currentDateLabel;
    Button playTimelineBtn;
    Button export2DZipBtn;

    Label mapRegenStatusLabel;
    ProgressBar mapRegenProgress;
    HBox mapRegenBox;

    ImageView mapImageViewA;
    ImageView mapImageViewB;
    ImageView mapImageViewDiff;
    Label mapLabelA;
    Label mapLabelB;
    Label mapLabelDiff;

    Label legendBlue;
    Label legendSlate;
    Label legendGreen;
    Label legendRed;

    ToggleGroup viewModeGroup;
    RadioButton rbPairwiseMode;
    RadioButton rbMultiMode;
    HBox mapPairBox;
    ScrollPane multiScenarioScrollPane;
    HBox multiScenarioBox;
    private List<ScenarioSelectableItem> activeSelectedScenarios = new ArrayList<>();

    TextArea spatialMetricsReportArea;

    private final SpatialTensorUIHelper uiHelper = new SpatialTensorUIHelper();
    final SpatialTensorTimelineAnimator timelineAnimator = new SpatialTensorTimelineAnimator();
    private final AtomicLong spatialComparisonRequestId = new AtomicLong(0);
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
            if (timelineAnimator.isPlayingAnimation()) {
                update2DSpatialComparison();
            } else {
                sliderDebounce.playFromStart();
            }
        });

        HBox scenarioRow = new HBox(10, scenarioSelectLabelA, comboScenarioA, scenarioSelectLabelB, comboScenarioB);
        scenarioRow.setAlignment(Pos.CENTER_LEFT);
        HBox channelRow = new HBox(10, channelLabel, spatialChannelCombo);
        channelRow.setAlignment(Pos.CENTER_LEFT);

        viewModeGroup = new ToggleGroup();
        rbPairwiseMode = new RadioButton(I18n.getOrDefault("analytics.mode.pairwise", "👥 Pairwise Mode (A vs B)"));
        rbPairwiseMode.setToggleGroup(viewModeGroup);
        rbPairwiseMode.setSelected(true);
        rbPairwiseMode.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        rbMultiMode = new RadioButton(I18n.getOrDefault("analytics.mode.multi_scenarios", "🌐 All Selected Scenarios"));
        rbMultiMode.setToggleGroup(viewModeGroup);
        rbMultiMode.setDisable(true);
        rbMultiMode.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        viewModeGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            boolean isMulti = rbMultiMode.isSelected();
            mapPairBox.setVisible(!isMulti);
            mapPairBox.setManaged(!isMulti);
            multiScenarioScrollPane.setVisible(isMulti);
            multiScenarioScrollPane.setManaged(isMulti);
            scenarioSelectLabelB.setVisible(!isMulti);
            scenarioSelectLabelB.setManaged(!isMulti);
            comboScenarioB.setVisible(!isMulti);
            comboScenarioB.setManaged(!isMulti);
            update2DSpatialComparison();
        });

        HBox modeRow = new HBox(16, rbPairwiseMode, rbMultiMode);
        modeRow.setAlignment(Pos.CENTER_LEFT);

        VBox spatialControlsBox = new VBox(6, scenarioRow, channelRow, modeRow);

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

        mapLabelA = new Label(I18n.getOrDefault("analytics.label.scenario_a_ref", "Scenario A (Reference Baseline)"));
        mapImageViewA = createMapView();
        VBox mapBoxA = createMapBox(mapLabelA, mapImageViewA);

        mapLabelB = new Label(I18n.getOrDefault("analytics.label.scenario_b_target", "Scenario B (Comparison Target)"));
        mapImageViewB = createMapView();
        VBox mapBoxB = createMapBox(mapLabelB, mapImageViewB);

        mapLabelDiff = new Label(I18n.getOrDefault("analytics.label.scenario_diff", "Discrepancy Heatmap Δ(A - B)"));
        mapLabelDiff.setStyle("-fx-text-fill: #f59e0b;");
        mapImageViewDiff = createMapView();
        VBox mapBoxDiff = createMapBox(mapLabelDiff, mapImageViewDiff);

        mapPairBox = new HBox(12, mapBoxA, mapBoxB, mapBoxDiff);
        mapPairBox.setAlignment(Pos.CENTER);

        multiScenarioBox = new HBox(16);
        multiScenarioBox.setAlignment(Pos.CENTER_LEFT);
        multiScenarioBox.setPadding(new Insets(8));

        multiScenarioScrollPane = new ScrollPane(multiScenarioBox);
        multiScenarioScrollPane.setFitToHeight(true);
        multiScenarioScrollPane.setPannable(true);
        multiScenarioScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        multiScenarioScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        multiScenarioScrollPane.setVisible(false);
        multiScenarioScrollPane.setManaged(false);

        uiHelper.attachHoverZoom(mapImageViewA, () -> mapLabelA.getText());
        uiHelper.attachHoverZoom(mapImageViewB, () -> mapLabelB.getText());
        uiHelper.attachHoverZoom(mapImageViewDiff, () -> mapLabelDiff.getText());

        HBox discrepancyLegendBox = buildLegendBar();

        spatialMetricsReportArea = new TextArea();
        spatialMetricsReportArea.setEditable(false);
        spatialMetricsReportArea.setPrefRowCount(6);
        spatialMetricsReportArea.getStyleClass().add("scenario-description-area");
        VBox.setVgrow(spatialMetricsReportArea, Priority.ALWAYS);

        getChildren().addAll(spatialControlsBox, spatialTimelineRow, mapRegenBox, mapPairBox, multiScenarioScrollPane,
                discrepancyLegendBox, spatialMetricsReportArea, spatialBottomBox);
    }

    private static ImageView createMapView() {
        ImageView iv = new ImageView(); iv.setFitWidth(360); iv.setFitHeight(180); iv.setPreserveRatio(true);
        iv.getStyleClass().add("card-section"); return iv;
    }

    private static VBox createMapBox(Label label, ImageView view) {
        label.getStyleClass().add("label-section-header");
        VBox box = new VBox(4, label, view); box.setAlignment(Pos.CENTER); return box;
    }

    private HBox buildLegendBar() {
        HBox box = new HBox(16); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(4, 12, 4, 12));
        box.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 6;");
        legendBlue = new Label("🟦 " + I18n.getOrDefault("analytics.legend.under", "A < B (Underestimation)"));
        legendBlue.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");
        legendSlate = new Label("⬛ " + I18n.getOrDefault("analytics.legend.zero", "Δ = 0 (Identical / Zero divergence)"));
        legendSlate.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
        legendGreen = new Label("🟩 " + I18n.getOrDefault("analytics.legend.close", "|Δ| ≤ 5% (Close agreement)"));
        legendGreen.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px;");
        legendRed = new Label("🟥 " + I18n.getOrDefault("analytics.legend.over", "A > B (Overestimation)"));
        legendRed.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
        box.getChildren().addAll(legendBlue, legendSlate, legendGreen, legendRed);
        return box;
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
        this.activeSelectedScenarios = selected;
        List<ScenarioSelectableItem> pool = (selected.size() >= 2) ? selected : new ArrayList<>(scenarioList);

        ScenarioSelectableItem prevA = comboScenarioA.getValue();
        ScenarioSelectableItem prevB = comboScenarioB.getValue();

        comboScenarioA.getItems().setAll(pool);
        comboScenarioB.getItems().setAll(pool);

        comboScenarioA.setValue(prevA != null && pool.contains(prevA) ? prevA : (!pool.isEmpty() ? pool.get(0) : null));
        comboScenarioB.setValue(prevB != null && pool.contains(prevB) ? prevB : (pool.size() > 1 ? pool.get(1) : (!pool.isEmpty() ? pool.get(0) : null)));

        if (activeSelectedScenarios.size() >= 3) {
            rbMultiMode.setDisable(false);
            rbMultiMode.setText(String.format(I18n.getOrDefault("analytics.mode.multi_scenarios_count", "🌐 Multi-Scénarios (%d sélectionnés)"), activeSelectedScenarios.size()));
        } else {
            rbMultiMode.setDisable(true);
            rbPairwiseMode.setSelected(true);
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
        Scenario scB = (itemB != null && itemB.getScenario() != null) ? itemB.getScenario() : scA;
        if (scA == null) return;

        if (mapLabelA != null) mapLabelA.setText(I18n.getOrDefault("analytics.label.scenario_a_prefix", "Scenario A: ") + itemA.getName());
        if (mapLabelB != null) mapLabelB.setText(I18n.getOrDefault("analytics.label.scenario_b_prefix", "Scenario B: ") + (itemB != null ? itemB.getName() : scB.getName()));
        if (mapLabelDiff != null) mapLabelDiff.setText(I18n.getOrDefault("analytics.label.scenario_diff", "Discrepancy Heatmap Δ(A - B)"));

        int startA = (int) scA.getStartDateYear(), endA = (int) scA.getEndDateYear();
        int startB = (int) scB.getStartDateYear(), endB = (int) scB.getEndDateYear();

        boolean isGtA = "HISTORICAL_GROUND_TRUTH".equals(itemA.getRunId());
        boolean isGtB = (itemB != null) && "HISTORICAL_GROUND_TRUTH".equals(itemB.getRunId());

        String planetA = itemA.getPlanetDisplayName();
        String planetB = (itemB != null) ? itemB.getPlanetDisplayName() : planetA;
        boolean diffPlanets = !planetA.equalsIgnoreCase(planetB);

        double minLatA = itemA.getMinLat(), maxLatA = itemA.getMaxLat();
        double minLatB = (itemB != null) ? itemB.getMinLat() : minLatA;
        double maxLatB = (itemB != null) ? itemB.getMaxLat() : maxLatA;
        double interMinLat = Math.max(minLatA, minLatB), interMaxLat = Math.min(maxLatA, maxLatB);
        double interMinLng = Math.max(itemA.getMinLng(), itemB != null ? itemB.getMinLng() : itemA.getMinLng());
        double interMaxLng = Math.min(itemA.getMaxLng(), itemB != null ? itemB.getMaxLng() : itemA.getMaxLng());

        boolean spatialDisjoint = diffPlanets || (interMinLat >= interMaxLat) || (interMinLng >= interMaxLng);
        int minYear = isGtA && !isGtB ? startB : (!isGtA && isGtB ? startA : (!isGtA ? Math.max(startA, startB) : -100000));
        int maxYear = isGtA && !isGtB ? endB : (!isGtA && isGtB ? endA : (!isGtA ? Math.min(endA, endB) : 2026));
        boolean temporalDisjoint = !isGtA && !isGtB && (maxYear < minYear);

        if (spatialDisjoint || temporalDisjoint) {
            if (mapImageViewDiff != null) mapImageViewDiff.setImage(null);
            if (dateSlider != null) dateSlider.setDisable(true);
            if (playTimelineBtn != null) playTimelineBtn.setDisable(true);
            if (spatialMetricsReportArea != null) {
                spatialMetricsReportArea.setText(spatialDisjoint
                        ? (diffPlanets
                                ? String.format(I18n.getOrDefault("analytics.spatial.disjoint_planets_fmt", "⚠️ Scénarios sur des planètes distinctes (%s vs %s) : aucune intersection spatiale possible. Données comparatives cartographiques directes indisponibles."), planetA, planetB)
                                : String.format(Locale.US, I18n.getOrDefault("analytics.spatial.disjoint_coords_fmt", "⚠️ Scénarios spatialement disjoints : aucune intersection géographique possible ([%.1f°..%.1f° Lat] vs [%.1f°..%.1f° Lat]). Données comparatives cartographiques indisponibles."), minLatA, maxLatA, minLatB, maxLatB))
                        : String.format(I18n.getOrDefault("analytics.spatial.disjoint_time_fmt", "⚠️ Scénarios temporellement disjoints : aucune intersection temporelle possible (Scénario A : %d ➔ %d | Scénario B : %d ➔ %d). Données comparatives indisponibles."), startA, endA, startB, endB));
            }
            return;
        }

        if (dateSlider != null) dateSlider.setDisable(false);
        if (playTimelineBtn != null) playTimelineBtn.setDisable(false);
        if (maxYear <= minYear) maxYear = minYear + 100;

        if (dateSlider != null && (minYear != (int) dateSlider.getMin() || maxYear != (int) dateSlider.getMax())) {
            dateSlider.setMin(minYear);
            dateSlider.setMax(maxYear);
            dateSlider.setMajorTickUnit(Math.max(10.0, (maxYear - minYear) / 5.0));
            if (dateSlider.getValue() < minYear || dateSlider.getValue() > maxYear) dateSlider.setValue(minYear);
            preloadAll2DTensorsAsync(itemA, itemB, minYear, maxYear);
        }

        final String channelKey = SpatialTensorDiskRegistry.resolveCanonicalChannelKey(spatialChannelCombo != null ? spatialChannelCombo.getValue() : null);
        int targetYear = dateSlider != null ? (int) dateSlider.getValue() : 0;
        final long currentReqId = spatialComparisonRequestId.incrementAndGet();

        if (rbMultiMode != null && rbMultiMode.isSelected() && activeSelectedScenarios.size() >= 3) {
            SpatialTensorMultiScenarioRenderer.render(
                    itemA, targetYear, channelKey, currentReqId,
                    activeSelectedScenarios, multiScenarioBox, spatialComparisonRequestId,
                    runResolver, spatialMetricsReportArea, uiHelper);
            return;
        }

        if (spatialMetricsReportArea != null && (spatialMetricsReportArea.getText() == null || spatialMetricsReportArea.getText().isBlank())) {
            spatialMetricsReportArea.setText(I18n.getOrDefault("analytics.status.loading_tensors", "ℹ️ Loading cartographic tensors for selected scenarios..."));
        }

        String runIdA = itemA.getRunId();
        String runIdB = itemB != null ? itemB.getRunId() : runIdA;
        boolean alreadyCached = RASTER_IMAGE_CACHE.containsKey(runIdA + "_" + targetYear + "_" + channelKey)
                && RASTER_IMAGE_CACHE.containsKey(runIdB + "_" + targetYear + "_" + channelKey);

        if (mapRegenBox != null) {
            mapRegenBox.setVisible(!alreadyCached);
            mapRegenBox.setManaged(!alreadyCached);
            if (!alreadyCached) {
                mapRegenProgress.setProgress(-1);
                mapRegenStatusLabel.setText(I18n.getOrDefault("analytics.status.loading_tensors", "ℹ️ Loading cartographic tensors..."));
            }
        }

        SpatialTensorDiffEngine.compute2DComparisonAsync(
                itemA, itemB, targetYear, channelKey, runResolver, RASTER_IMAGE_CACHE,
                totalMapsNeeded -> Platform.runLater(() -> {
                    if (mapRegenBox != null && mapRegenStatusLabel != null) {
                        mapRegenBox.setVisible(true);
                        mapRegenBox.setManaged(true);
                        mapRegenProgress.setProgress(0.1);
                        mapRegenStatusLabel.setText(String.format(I18n.getOrDefault("analytics.status.regen_maps_fmt", "⚡ Regenerating 2D maps: %d / %d"), 1, totalMapsNeeded));
                    }
                })
        ).thenAccept(res -> Platform.runLater(() -> {
            if (currentReqId != spatialComparisonRequestId.get()) return;
            if (mapImageViewA != null) mapImageViewA.setImage(res.imgA());
            if (mapImageViewB != null) mapImageViewB.setImage(res.imgB());
            if (mapImageViewDiff != null) mapImageViewDiff.setImage(res.imgDiff());
            if (spatialMetricsReportArea != null) spatialMetricsReportArea.setText(res.formattedReport());
            if (mapRegenBox != null) {
                mapRegenBox.setVisible(false);
                mapRegenBox.setManaged(false);
            }
        }));
    }

    private void preloadAll2DTensorsAsync(ScenarioSelectableItem itemA, ScenarioSelectableItem itemB, int startYear, int endYear) {
        String channelKey = SpatialTensorDiskRegistry.resolveCanonicalChannelKey(spatialChannelCombo != null ? spatialChannelCombo.getValue() : null);
        timelineAnimator.preloadAll2DTensorsAsync(itemA, itemB, startYear, endYear, channelKey, runResolver,
                playTimelineBtn, mapRegenBox, mapRegenProgress, mapRegenStatusLabel);
    }

    public static int computeOptimalRasterWidth(int cellCount) {
        return SpatialTensorRasterRenderer.computeOptimalRasterWidth(cellCount);
    }

    public static String resolveCanonicalChannelKey(String channel) {
        return SpatialTensorDiskRegistry.resolveCanonicalChannelKey(channel);
    }

    private void toggleDateAnimation() {
        timelineAnimator.toggleDateAnimation(playTimelineBtn, dateSlider);
    }

    public void stopAnimation() {
        timelineAnimator.stopAnimation(playTimelineBtn);
    }

    private void export2DCartographyZip() {
        ScenarioSelectableItem itemA = comboScenarioA != null ? comboScenarioA.getValue() : null;
        ScenarioSelectableItem itemB = comboScenarioB != null ? comboScenarioB.getValue() : null;
        if (itemA == null && comboScenarioA != null && !comboScenarioA.getItems().isEmpty()) itemA = comboScenarioA.getItems().get(0);
        if (itemB == null && comboScenarioB != null && comboScenarioB.getItems().size() > 1) itemB = comboScenarioB.getItems().get(1);
        if (itemB == null) itemB = itemA;
        if (itemA == null) return;
        ComparativeAnalyticsExporter.export2DCartographyZip(
                export2DZipBtn, itemA, itemB, dateSlider != null ? (int) dateSlider.getValue() : 0,
                resolveCanonicalChannelKey(spatialChannelCombo != null ? spatialChannelCombo.getValue() : null),
                spatialMetricsReportArea != null ? spatialMetricsReportArea.getText() : "",
                mapImageViewA != null ? mapImageViewA.getImage() : null,
                mapImageViewB != null ? mapImageViewB.getImage() : null,
                mapImageViewDiff != null ? mapImageViewDiff.getImage() : null, RASTER_IMAGE_CACHE);
    }

    public void updateTexts(List<ScenarioSelectableItem> scenarioList) {
        isUpdatingTexts = true;
        try { SpatialTensorUIHelper.updateLocalizedControls(this, scenarioList); }
        finally { isUpdatingTexts = false; }
        update2DSpatialComparison();
    }

    public ComboBox<ScenarioSelectableItem> getComboScenarioA() { return comboScenarioA; } public ComboBox<ScenarioSelectableItem> getComboScenarioB() { return comboScenarioB; }
    public ComboBox<String> getSpatialChannelCombo() { return spatialChannelCombo; } public Slider getDateSlider() { return dateSlider; }
    public ImageView getMapImageViewA() { return mapImageViewA; } public ImageView getMapImageViewB() { return mapImageViewB; }
    public ImageView getMapImageViewDiff() { return mapImageViewDiff; } public TextArea getSpatialMetricsReportArea() { return spatialMetricsReportArea; }
    public Button getExport2DZipBtn() { return export2DZipBtn; }
    public static void clearRasterCache() { SpatialTensorDiskRegistry.clearRasterCache(); }
}
