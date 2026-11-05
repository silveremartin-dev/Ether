/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.ether.society.analytics.MetricDescriptor;
import org.ether.society.analytics.MetricRegistry;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.stats.MetricCard;
import org.ether.society.ui.stats.MetricCardMeta;
import org.ether.society.ui.stats.StatsChartsSection;
import org.ether.society.ui.stats.StatsCsvExporter;
import org.ether.society.ui.stats.StatsTelemetryUpdater;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Enhanced Cliodynamic & Physicalist Statistics Dashboard.
 *
 * <p>Refactored according to the Single Responsibility Principle (SRP). Coordinates live statistical telemetry
 * across categorized metrics, delegates time-series chart rendering to {@link StatsChartsSection},
 * card layouts to {@link MetricCard}, and data export to {@link StatsCsvExporter}.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class StatsPanel extends VBox {

    private final H3SimulationEngine engine;

    // Controls Header
    private final Label headerTitle;
    private final Label cpuNoticeLabel;
    private final ToggleButton btnLiveCollection;
    private final ComboBox<String> samplingCombo;
    private final Button btnConfigureTelemetry;
    private final Button btnFormulaEditor;
    private final Set<String> activeTelemetryVariables = new LinkedHashSet<>(org.ether.society.ui.analytics.TelemetryVariableSelectorDialog.getDefaultActiveVariableIds());

    // Charts Section
    private final StatsChartsSection chartsSection;

    // Filter Controls
    private final ComboBox<String> categoryFilterCombo;
    private final TextField searchField;
    private final Button exportBtn;

    // Metric Labels Registry
    private final Map<String, MetricCard> metricCards = new LinkedHashMap<>();

    // Pluggable Formula & Variance Analytics Engine
    private final PluggableStatEngine pluggableStatEngine = new PluggableStatEngine();
    private final VarianceDistributionPanel variancePanel;
    private final SpatialHeatmapPanel spatialHeatmapPanel = new SpatialHeatmapPanel();

    private boolean isLiveCollectionActive = true;
    private int samplingIntervalTicks = 1;
    private long tickCounter = 0;
    private Consumer<DisplayMode> onDisplayModeRequested;

    private final Label metricsHeaderLabel;
    private final Label metricInspectorTitle;
    private final Label metricInspectorText;

    private boolean isUpdatingTexts = false;
    private final VBox metricsContainer;

    private final AtomicLong lastStatsUiUpdateNanos = new AtomicLong(0);
    private final AtomicBoolean statsUpdatePending = new AtomicBoolean(false);

    /**
     * Backward-compatible alias for sparkline canvas.
     */
    public static class SparklineCanvas extends org.ether.society.ui.stats.SparklineCanvas {}

    /**
     * Registers listener invoked when user requests a thematic map layer from a metric card.
     *
     * @param listener consumer for display mode
     */
    public void setOnDisplayModeRequested(Consumer<DisplayMode> listener) {
        this.onDisplayModeRequested = listener;
        if (chartsSection != null) {
            chartsSection.setOnDisplayModeRequested(listener);
        }
    }

    /**
     * Sets selected metric tracing in chart according to active display mode.
     *
     * @param mode active display mode
     */
    public void setSelectedMetricByDisplayMode(DisplayMode mode) {
        if (mode == null) return;
        String metricId = mode.getMetricId();
        MetricDescriptor desc = MetricRegistry.getInstance().getDescriptor(metricId);
        if (desc != null) {
            String title = desc.getDisplayName();
            ComboBox<String> combo = chartsSection.getChartMetricCombo();
            if (combo != null && combo.getItems().contains(title)) {
                combo.setValue(title);
                resetChartSeries();
            }
        }
    }

    /**
     * Constructs the statistics dashboard panel.
     *
     * @param engine active simulation engine
     */
    public StatsPanel(H3SimulationEngine engine) {
        this.engine = engine;
        this.variancePanel = new VarianceDistributionPanel(pluggableStatEngine);

        setPadding(new Insets(12));
        setSpacing(12);
        getStyleClass().add("glass-panel");

        // --- TOP HEADER TOOLBAR ---
        headerTitle = new Label();
        headerTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        cpuNoticeLabel = new Label();
        cpuNoticeLabel.setWrapText(true);
        cpuNoticeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #93c5fd; -fx-padding: 4 8; -fx-background-color: rgba(30, 58, 138, 0.4); -fx-background-radius: 4; -fx-border-color: rgba(59, 130, 246, 0.4); -fx-border-radius: 4;");

        btnLiveCollection = new ToggleButton();
        btnLiveCollection.setSelected(true);
        btnLiveCollection.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        btnLiveCollection.setOnAction(e -> {
            isLiveCollectionActive = btnLiveCollection.isSelected();
            btnLiveCollection.setText(isLiveCollectionActive
                    ? I18n.getOrDefault("stats.btn.live_collection_active", "⚡ Collecte Stats : ACTIF")
                    : I18n.getOrDefault("stats.btn.live_collection_paused", "⏸️ Collecte Stats : EN PAUSE"));
            btnLiveCollection.setStyle(isLiveCollectionActive
                    ? "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;"
                    : "-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        });

        samplingCombo = new ComboBox<>();
        samplingCombo.setStyle("-fx-font-size: 10px;");
        samplingCombo.setOnAction(e -> {
            int idx = samplingCombo.getSelectionModel().getSelectedIndex();
            samplingIntervalTicks = switch (idx) {
                case 1 -> 5;
                case 2 -> 20;
                case 3 -> 100;
                default -> 1;
            };
        });

        btnConfigureTelemetry = new Button();
        btnConfigureTelemetry.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        btnConfigureTelemetry.setText("⚙️ Variables");
        btnConfigureTelemetry.setTooltip(new Tooltip("Choisir les variables calculées et affichées en direct"));
        btnConfigureTelemetry.setOnAction(e -> {
            var dlg = new org.ether.society.ui.analytics.TelemetryVariableSelectorDialog(activeTelemetryVariables);
            dlg.showAndWait().ifPresent(selected -> {
                activeTelemetryVariables.clear();
                activeTelemetryVariables.addAll(selected);
                update();
            });
        });

        btnFormulaEditor = new Button();
        btnFormulaEditor.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        btnFormulaEditor.setOnAction(e -> {
            PluggableFormulaEditorDialog dlg = new PluggableFormulaEditorDialog(pluggableStatEngine, engine != null ? engine.getCells() : null);
            dlg.showAndWait();
            update();
        });

        HBox perfToolbar = new HBox(8, btnLiveCollection, samplingCombo, btnConfigureTelemetry, btnFormulaEditor);
        perfToolbar.setAlignment(Pos.CENTER_LEFT);

        VBox topControlsBox = new VBox(6, headerTitle, cpuNoticeLabel, perfToolbar);
        topControlsBox.getStyleClass().add("card-section");

        // --- CHARTS SECTION (LineChart & BarChart) ---
        chartsSection = new StatsChartsSection();
        chartsSection.setEngineSupplier(() -> this.engine);
        chartsSection.setOnDisplayModeRequested(mode -> {
            if (onDisplayModeRequested != null) onDisplayModeRequested.accept(mode);
        });

        // --- SECTION 3: METRIC CARDS & CATEGORY FILTER ---
        metricsHeaderLabel = new Label();
        metricsHeaderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        metricInspectorTitle = new Label();
        metricInspectorTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ffd700;");
        metricInspectorText = new Label();
        metricInspectorText.setWrapText(true);
        metricInspectorText.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

        VBox metricInspectorCard = new VBox(4, metricInspectorTitle, metricInspectorText);
        metricInspectorCard.setStyle("-fx-padding: 8 10; -fx-background-color: rgba(255, 215, 0, 0.08); -fx-background-radius: 6; -fx-border-color: rgba(255, 215, 0, 0.25); -fx-border-radius: 6;");

        categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.setMaxWidth(Double.MAX_VALUE);
        categoryFilterCombo.setOnAction(e -> filterMetrics());

        searchField = new TextField();
        searchField.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-text-fill: white; -fx-prompt-text-fill: #64748b;");
        searchField.textProperty().addListener((obs, oldV, newV) -> filterMetrics());

        exportBtn = new Button();
        exportBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6;");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> exportCsv());

        VBox cardsControlBox = new VBox(6, metricsHeaderLabel, metricInspectorCard, categoryFilterCombo, searchField, exportBtn);
        cardsControlBox.getStyleClass().add("card-section");

        metricsContainer = new VBox(6);

        registerMetricCards(metricInspectorTitle, metricInspectorText);

        getChildren().addAll(topControlsBox, chartsSection.getChartBox(), chartsSection.getBarBox(),
                spatialHeatmapPanel, variancePanel, cardsControlBox, metricsContainer);

        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    /**
     * Updates all localized UI texts across dashboard.
     */
    public void updateTexts() {
        isUpdatingTexts = true;
        try {
            headerTitle.setText(I18n.getOrDefault("stats.header", "📊 CLIODYNAMIC & PHYSICALIST STATISTICS DASHBOARD"));
            cpuNoticeLabel.setText(I18n.getOrDefault("stats.notice.cpu",
                "⚠️ CPU PERFORMANCE NOTICE: Real-time statistical aggregation (Gini indices, thermodynamic entropy, " +
                "Turchin structural-demographic analysis, variance and H3 spatial meshes) runs intensive per-cell compute cycles. " +
                "If simulation rate drops, decrease sampling frequency below (e.g. 5 or 20 ticks) or pause live collection."));

            btnLiveCollection.setText(isLiveCollectionActive
                    ? I18n.getOrDefault("stats.btn.live_collection_active", "⚡ Live Stats: ACTIVE")
                    : I18n.getOrDefault("stats.btn.live_collection_paused", "⏸️ Live Stats: PAUSED"));
            btnLiveCollection.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.live_collection", "Toggle dynamic statistics computation to conserve CPU resources.")));

            int selectedSampling = samplingCombo.getSelectionModel().getSelectedIndex();
            samplingCombo.getItems().clear();
            samplingCombo.getItems().addAll(
                I18n.getOrDefault("stats.sampling.1tick", "1 Tick (Every Cycle)"),
                I18n.getOrDefault("stats.sampling.5ticks", "5 Ticks"),
                I18n.getOrDefault("stats.sampling.20ticks", "20 Ticks"),
                I18n.getOrDefault("stats.sampling.100ticks", "100 Ticks")
            );
            samplingCombo.getSelectionModel().select(selectedSampling >= 0 ? selectedSampling : 0);
            samplingCombo.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.sampling", "Sampling and refresh rate for all spatial metrics.")));

            btnFormulaEditor.setText(I18n.getOrDefault("stats.btn.formula_editor", "🧮 Custom Formulas & Variables"));
            btnFormulaEditor.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.formula_editor", "Open pluggable formula editor (SUM, AVG, GINI, etc.).")));

            chartsSection.updateTexts(isUpdatingTexts);

            metricsHeaderLabel.setText(I18n.getOrDefault("stats.section.detailed_metrics", "📋 CLIODYNAMIC INDICATORS & DETAILED METRICS"));
            metricInspectorTitle.setText(I18n.getOrDefault("stats.label.metric_explanation", "🔎 Cliodynamic Metric Explanation:"));
            metricInspectorText.setText(I18n.getOrDefault("stats.desc.hover_metric", "Hover or click on any statistic card below to view its mathematical formula, underlying parameters, and sociological interpretation."));

            int catIdx = categoryFilterCombo.getSelectionModel().getSelectedIndex();
            categoryFilterCombo.getItems().clear();
            categoryFilterCombo.getItems().addAll(
                I18n.getOrDefault("stats.cat.all", "All Categories"),
                I18n.getOrDefault("stats.cat.energy", "⚡ Energy & Matter"),
                I18n.getOrDefault("stats.cat.demography", "👥 Demography & Health"),
                I18n.getOrDefault("stats.cat.society", "🏛️ Society & Institutions"),
                I18n.getOrDefault("stats.cat.economy", "💎 Economy & Wealth"),
                I18n.getOrDefault("stats.cat.cognition", "🧠 Cognition & Information"),
                I18n.getOrDefault("stats.cat.ecology", "🌍 Ecology & Planetary Boundaries"),
                I18n.getOrDefault("stats.cat.cliodynamics", "⏳ Cliodynamics & Systemic Risks"),
                I18n.getOrDefault("stats.cat.complexity", "⚙️ Systemic Complexity"),
                I18n.getOrDefault("stats.cat.performance", "💻 Technical Performance")
            );
            categoryFilterCombo.getSelectionModel().select(catIdx >= 0 ? catIdx : 0);

            searchField.setPromptText(I18n.getOrDefault("stats.prompt.filter", "🔍 Filter metric (e.g. Kardashev, Gini, Collapse, NPK)..."));

            exportBtn.setText(I18n.getOrDefault("stats.btn.export_csv", "📥 Export Dataset (CSV)"));
            exportBtn.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.export_csv", "Export complete history of societal, energetic, and economic metrics as CSV.")));

            for (MetricCardMeta meta : MetricCardMeta.ALL) {
                MetricCard card = metricCards.get(meta.key());
                if (card != null) {
                    String title = I18n.getOrDefault("stat.metric." + meta.key() + ".title", meta.defaultTitle());
                    String cat = I18n.getOrDefault("stat.metric." + meta.key() + ".cat", meta.defaultCategory());
                    String unit = I18n.getOrDefault("stat.metric." + meta.key() + ".unit", meta.defaultUnit());
                    String tooltip = I18n.getOrDefault("stat.metric." + meta.key() + ".tooltip", meta.defaultTooltip());
                    card.updateTexts(title, cat, unit, tooltip);
                }
            }

            ComboBox<String> combo = chartsSection.getChartMetricCombo();
            String prevSelected = combo.getValue();
            combo.getItems().clear();

            Map<String, List<MetricCard>> cardsByCategory = new LinkedHashMap<>();
            for (MetricCard card : metricCards.values()) {
                cardsByCategory.computeIfAbsent(card.getCategory(), k -> new ArrayList<>()).add(card);
            }

            for (Map.Entry<String, List<MetricCard>> entry : cardsByCategory.entrySet()) {
                String catName = entry.getKey();
                List<MetricCard> cardsInCat = entry.getValue();
                cardsInCat.sort(Comparator.comparing(MetricCard::getTitle, String.CASE_INSENSITIVE_ORDER));

                combo.getItems().add("─── " + catName + " ───");
                for (MetricCard card : cardsInCat) {
                    combo.getItems().add(card.getTitle());
                }
            }

            if (prevSelected != null && combo.getItems().contains(prevSelected)) {
                combo.setValue(prevSelected);
            } else {
                for (String it : combo.getItems()) {
                    if (!it.startsWith("─── ")) {
                        combo.setValue(it);
                        break;
                    }
                }
            }

            filterMetrics();
            chartsSection.reloadChartData();
        } finally {
            isUpdatingTexts = false;
        }
    }

    private void registerMetricCards(Label inspectorTitle, Label inspectorText) {
        metricCards.clear();
        for (MetricCardMeta meta : MetricCardMeta.ALL) {
            String title = I18n.getOrDefault("stat.metric." + meta.key() + ".title", meta.defaultTitle());
            String cat = I18n.getOrDefault("stat.metric." + meta.key() + ".cat", meta.defaultCategory());
            String unit = I18n.getOrDefault("stat.metric." + meta.key() + ".unit", meta.defaultUnit());
            String tooltip = I18n.getOrDefault("stat.metric." + meta.key() + ".tooltip", meta.defaultTooltip());
            addCard(meta.key(), title, cat, unit, tooltip, inspectorTitle, inspectorText);
        }
    }

    private void addCard(String key, String title, String category, String unit, String tooltip, Label inspectorTitle, Label inspectorText) {
        MetricCard card = new MetricCard(key, title, category, unit, tooltip);
        card.setOnMouseEntered(e -> {
            inspectorTitle.setText("🔎 " + card.getTitle() + " [" + card.getCategory() + "]");
            inspectorText.setText(card.getTooltipText());
        });
        card.setOnMouseClicked(e -> {
            inspectorTitle.setText("🔎 " + card.getTitle() + " [" + card.getCategory() + "]");
            inspectorText.setText(card.getTooltipText());
            ComboBox<String> combo = chartsSection.getChartMetricCombo();
            if (combo != null) {
                combo.setValue(card.getTitle());
                resetChartSeries();
            }
            if (onDisplayModeRequested != null) {
                onDisplayModeRequested.accept(DisplayMode.fromMetricId(key));
            }
        });
        metricCards.put(key, card);
    }

    private void filterMetrics() {
        String catFilter = categoryFilterCombo.getValue();
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";

        metricsContainer.getChildren().clear();
        String currentCat = "";

        for (MetricCard card : metricCards.values()) {
            boolean matchesCat = catFilter == null || catFilter.contains("Toutes") || catFilter.contains("All") || card.getCategory().equalsIgnoreCase(catFilter);
            boolean matchesSearch = searchText.isEmpty() || card.getTitle().toLowerCase().contains(searchText) || card.getCategory().toLowerCase().contains(searchText);

            if (matchesCat && matchesSearch) {
                if (!card.getCategory().equals(currentCat)) {
                    currentCat = card.getCategory();
                    Label catHeader = new Label(currentCat.toUpperCase());
                    catHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-padding: 8 0 2 0;");
                    metricsContainer.getChildren().add(catHeader);
                }
                metricsContainer.getChildren().add(card);
            }
        }
    }

    /**
     * Resets line and bar chart data.
     */
    public void reset() {
        chartsSection.reset();
        tickCounter = 0;
    }

    /**
     * Resets chart time-series.
     */
    public void resetChartSeries() {
        chartsSection.resetChartSeries();
    }

    /**
     * Updates all metric values, trend sparklines, and charts.
     */
    public void update() {
        if (engine == null || !isLiveCollectionActive) return;
        if (engine.getCells() == null || engine.getCells().isEmpty()) return;

        tickCounter++;
        if (tickCounter % samplingIntervalTicks != 0) return;

        long nowNanos = System.nanoTime();
        if (nowNanos - lastStatsUiUpdateNanos.get() < 100_000_000L) {
            return;
        }
        if (!statsUpdatePending.compareAndSet(false, true)) {
            return;
        }
        lastStatsUiUpdateNanos.set(nowNanos);

        StatsTelemetryUpdater.captureAndDispatchUpdate(
                engine, metricCards, activeTelemetryVariables, chartsSection,
                spatialHeatmapPanel, variancePanel, statsUpdatePending);
    }

    private void exportCsv() {
        StatsCsvExporter.exportCsv(engine, getScene() != null ? getScene().getWindow() : null);
    }
}
