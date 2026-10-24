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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final Button btnFormulaEditor;

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

        btnFormulaEditor = new Button();
        btnFormulaEditor.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        btnFormulaEditor.setOnAction(e -> {
            PluggableFormulaEditorDialog dlg = new PluggableFormulaEditorDialog(pluggableStatEngine, engine != null ? engine.getCells() : null);
            dlg.showAndWait();
            update();
        });

        HBox perfToolbar = new HBox(8, btnLiveCollection, samplingCombo, btnFormulaEditor);
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

        long pop = engine.getTotalPopulation();
        float bio = engine.getTotalBiomassNatural();
        double food = engine.getTotalFood();
        float gini = engine.getCurrentGini();
        float gdp = engine.getCurrentGDP();
        float fert = engine.getCurrentFertility();
        float life = engine.getCurrentLifeExpectancy();
        float tech = engine.getAverageTechnology();

        double energyCap = engine.getEnergyCaptured();
        double resDep = engine.getResourceDepletionRate();
        double energyPerCap = engine.getEnergyPerCapita();
        double foodPerCap = engine.getFoodPerCapita();
        double eroiAlim = engine.getEroiAlimentaire();
        double netSurplus = engine.getNetSurplusFraction() * 100.0;
        double trophicMul = engine.getTrophicMultiplier();
        double bioMobilized = engine.getBiomassMobilizedPerCapitaKg();
        double bioDom = engine.getBiomassDomesticated();
        double water = engine.getPotableWaterTotal();
        double remRes = engine.getRemainingResourcesRatio();
        double entropy = engine.getSystemicEntropy();
        double territory = engine.getOccupiedTerritoryArea();

        double offspring = engine.getOffspringPercentage();
        double ageFirstChild = engine.getAgeAtFirstChild();
        double immigration = engine.getImmigrationRate();
        double education = engine.getEducationLevel();

        double happiness = engine.getHappinessIndex();
        double conflict = engine.getConflictLevel();
        int cityStates = engine.getCityStatesCount();
        double instMaturity = engine.getInstitutionalMaturity();
        double divLabor = engine.getDivisionOfLaborIndex();
        int maxHier = engine.getMaxHierarchyLevel();
        long largestCult = engine.getLargestCulturalUnitSize();
        double kardashev = engine.getKardashevScale();

        double builtCap = engine.getBuiltCapitalTotal();
        double eliteForm = engine.getEliteFormationRatio();
        double elderCap = engine.getElderCapitalShare();
        double landRent = engine.getLandRentIndex();
        long tools = engine.getToolsCount();
        long products = engine.getProductsCount();

        double sysComp = engine.getSystemComplexityIndex();
        double reconCap = engine.getReconstructionCapabilityIndex();
        double sysInter = engine.getSystemInterdependenceIndex();

        double shannonBw = engine.getShannonBandwidth();
        double memoryStock = engine.getCollectiveMemoryStock();
        double innovSpeed = engine.getInnovationDiffusionSpeed();
        double knowDecay = engine.getKnowledgeDecayRate();

        double soilNPK = engine.getSoilNPKQuality();
        double carbonFp = engine.getCarbonFootprint();
        double wildBio = engine.getWildBiodiversityIndex();
        double wetBulb = engine.getWetBulbSafetyMargin();

        double eliteOver = engine.getEliteOverproductionIndex();
        double fiscalStress = engine.getFiscalStressIndex();
        double geoTension = engine.getGeopoliticalTension();
        double collapseVuln = engine.getCollapseVulnerability();

        Runtime rt = Runtime.getRuntime();
        long usedMem = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        double tps = engine.getCurrentTPS();
        int totalCells = engine.getCells() != null ? engine.getCells().size() : 0;
        int year = engine.getTimeManager().getCurrentYear();

        Platform.runLater(() -> {
            try {
                spatialHeatmapPanel.setHistoryManager(engine.getHistoryManager());
                spatialHeatmapPanel.updateCells(engine.getCells());
                spatialHeatmapPanel.setDateLabel(String.format("📅 An %d", year));

                setCardVal("energyCaptured", String.format("%,.1f", energyCap), energyCap);
                setCardVal("resourceDepletion", String.format("%.1f", resDep), resDep);
                setCardVal("energyPerCapita", String.format("%.1f", energyPerCap), energyPerCap);
                setCardVal("foodPerCapita", String.format("%.2f", foodPerCap), foodPerCap);
                setCardVal("eroiAlim", String.format("%.2f : 1", eroiAlim), eroiAlim);
                setCardVal("netSurplus", String.format("%.1f%%", netSurplus), netSurplus);
                setCardVal("trophicMultiplier", String.format("%.2fx", trophicMul), trophicMul);
                setCardVal("biomassMobilized", String.format("%,.0f", bioMobilized), bioMobilized);
                setCardVal("pibMaterialFlow", String.format("%.1f", resDep * 5.2), resDep * 5.2);
                setCardVal("biomassNatural", String.format("%,.0f", bio), bio);
                setCardVal("biomassDomesticated", String.format("%,.1f", bioDom), bioDom);
                setCardVal("potableWater", String.format("%,.0f", water), water);
                setCardVal("remainingResources", String.format("%.1f", remRes), remRes);
                setCardVal("entropyPollution", String.format("%.1f", entropy), entropy);
                setCardVal("occupiedTerritory", String.format("%,.0f", territory), territory);

                setCardVal("population", String.format("%,d", pop), (double) pop);
                setCardVal("fertilityRate", String.format("%.1f", fert), (double) fert);
                setCardVal("offspringPct", String.format("%.1f", offspring), offspring);
                setCardVal("ageFirstChild", String.format("%.1f", ageFirstChild), ageFirstChild);
                setCardVal("immigrationRate", String.format("%.1f", immigration), immigration);
                setCardVal("lifeExpectancy", String.format("%.1f", life), (double) life);
                setCardVal("healthIndex", String.format("%.1f", Math.min(100.0, life * 1.1)), Math.min(100.0, life * 1.1));
                setCardVal("educationLevel", String.format("%.1f", education), education);

                double largestOrgComp = engine.getLargestOrganizationComplexity();
                double largestOrgEnt = engine.getLargestOrganizationEntropy();

                setCardVal("happinessIndex", String.format("%.1f", happiness), happiness);
                setCardVal("conflictLevel", String.format("%.1f", conflict), conflict);
                setCardVal("cityStates", String.format("%d", cityStates), (double) cityStates);
                setCardVal("institutionalMaturity", String.format("%.1f", instMaturity), instMaturity);
                setCardVal("divisionLabor", String.format("%.1f", divLabor), divLabor);
                setCardVal("maxHierarchy", String.format("Niv %d", maxHier), (double) maxHier);
                setCardVal("largestCulture", String.format("%,d", largestCult), (double) largestCult);
                setCardVal("largestOrgComplexity", String.format("%,.0f", largestOrgComp), largestOrgComp);
                setCardVal("largestOrgEntropy", String.format("%,.1f", largestOrgEnt), largestOrgEnt);
                setCardVal("avgTechLevel", String.format("%.2f", tech), (double) tech);
                setCardVal("kardashevScale", String.format("%.2f", kardashev), kardashev);

                setCardVal("giniIndex", String.format("%.2f", gini), (double) gini);
                setCardVal("gdpTotal", String.format("%,.0f", gdp), (double) gdp);
                setCardVal("builtCapital", String.format("%,.0f", builtCap / Math.max(1, pop)), builtCap / Math.max(1, pop));
                setCardVal("eliteFormation", String.format("%.1f", eliteForm), eliteForm);
                setCardVal("elderCapitalShare", String.format("%.1f", elderCap), elderCap);
                setCardVal("landRent", String.format("%.1f", landRent), landRent);
                setCardVal("toolsCount", String.format("%,d", tools), (double) tools);
                setCardVal("productsCount", String.format("%,d", products), (double) products);

                setCardVal("shannonBandwidth", String.format("%.1f", shannonBw), shannonBw);
                setCardVal("collectiveMemory", String.format("%,.0f", memoryStock), memoryStock);
                setCardVal("innovationDiffusion", String.format("%.1f", innovSpeed), innovSpeed);
                setCardVal("knowledgeDecay", String.format("%.1f", knowDecay), knowDecay);

                setCardVal("soilNPK", String.format("%.1f", soilNPK), soilNPK);
                setCardVal("carbonFootprint", String.format("%.2f", carbonFp), carbonFp);
                setCardVal("wildBiodiversity", String.format("%.1f", wildBio), wildBio);
                setCardVal("wetBulbSafety", String.format("%.1f", wetBulb), wetBulb);

                setCardVal("eliteOverproduction", String.format("%.2f", eliteOver), eliteOver);
                setCardVal("fiscalStress", String.format("%.1f", fiscalStress), fiscalStress);
                setCardVal("geopoliticalTension", String.format("%.1f", geoTension), geoTension);
                setCardVal("collapseVulnerability", String.format("%.1f", collapseVuln), collapseVuln);

                setCardVal("systemComplexity", String.format("%.1f", sysComp), sysComp);
                setCardVal("reconstructionCapability", String.format("%.1f", reconCap), reconCap);
                setCardVal("systemInterdependence", String.format("%.1f", sysInter), sysInter);

                setCardVal("engineTPS", String.format("%.1f", tps), tps);
                setCardVal("ramMemory", String.format("%d", usedMem), (double) usedMem);
                setCardVal("cellCount", String.format("%,d", totalCells), (double) totalCells);

                ComboBox<String> combo = chartsSection.getChartMetricCombo();
                String selectedMetric = combo != null ? combo.getValue() : "Population Humaine";
                if (selectedMetric == null || selectedMetric.startsWith("─── ")) {
                    selectedMetric = "Population Humaine";
                }
                double yVal = extractMetricValue(selectedMetric, energyCap, resDep, energyPerCap, foodPerCap, bio, bioDom,
                        water, remRes, entropy, territory, pop, fert, offspring, ageFirstChild, immigration, life,
                        education, happiness, conflict, cityStates, instMaturity, divLabor, maxHier, largestCult,
                        largestOrgComp, largestOrgEnt, tech, kardashev, gini, gdp, builtCap, eliteForm, elderCap,
                        landRent, tools, products, shannonBw, memoryStock, innovSpeed, knowDecay, soilNPK, carbonFp,
                        wildBio, wetBulb, eliteOver, fiscalStress, geoTension, collapseVuln, sysComp, reconCap,
                        sysInter, tps, usedMem, totalCells);

                double currentTime = year + (engine.getTimeManager().getCurrentMonth() / 12.0);
                long minYear = engine.getCurrentScenario() != null ? engine.getCurrentScenario().getStartDateYear() : 0L;
                chartsSection.appendTimeSeriesPoint(currentTime, yVal, minYear);
                chartsSection.updateAgePyramid(engine.getAgePyramid());

                variancePanel.updateData(engine.getCells());
            } finally {
                statsUpdatePending.set(false);
            }
        });
    }

    private double extractMetricValue(String selectedMetric, double energyCap, double resDep, double energyPerCap,
            double foodPerCap, float bio, double bioDom, double water, double remRes, double entropy, double territory,
            long pop, float fert, double offspring, double ageFirstChild, double immigration, float life,
            double education, double happiness, double conflict, int cityStates, double instMaturity, double divLabor,
            int maxHier, long largestCult, double largestOrgComp, double largestOrgEnt, float tech, double kardashev,
            float gini, float gdp, double builtCap, double eliteForm, double elderCap, double landRent, long tools,
            long products, double shannonBw, double memoryStock, double innovSpeed, double knowDecay, double soilNPK,
            double carbonFp, double wildBio, double wetBulb, double eliteOver, double fiscalStress, double geoTension,
            double collapseVuln, double sysComp, double reconCap, double sysInter, double tps, long usedMem, int totalCells) {
        if (selectedMetric == null) return pop;

        String key = null;
        for (MetricCard card : metricCards.values()) {
            if (card.getTitle().equalsIgnoreCase(selectedMetric)) {
                key = card.getKey();
                break;
            }
        }

        if (key != null) {
            return switch (key) {
                case "energyCaptured" -> energyCap;
                case "resourceDepletion" -> resDep;
                case "energyPerCapita" -> energyPerCap;
                case "foodPerCapita" -> foodPerCap;
                case "eroiAlim" -> engine.getEroiAlimentaire();
                case "netSurplus" -> engine.getNetSurplusFraction() * 100.0;
                case "trophicMultiplier" -> engine.getTrophicMultiplier();
                case "biomassMobilized" -> engine.getBiomassMobilizedPerCapitaKg();
                case "pibMaterialFlow" -> resDep * 5.2;
                case "biomassNatural" -> bio;
                case "biomassDomesticated" -> bioDom;
                case "potableWater" -> water;
                case "remainingResources" -> remRes;
                case "entropyPollution" -> entropy;
                case "occupiedTerritory" -> territory;
                case "population" -> pop;
                case "fertilityRate" -> fert;
                case "offspringPct" -> offspring;
                case "ageFirstChild" -> ageFirstChild;
                case "immigrationRate" -> immigration;
                case "lifeExpectancy" -> life;
                case "healthIndex" -> Math.min(100.0, life * 1.1);
                case "educationLevel" -> education;
                case "happinessIndex" -> happiness;
                case "conflictLevel" -> conflict;
                case "cityStates" -> cityStates;
                case "institutionalMaturity" -> instMaturity;
                case "divisionLabor" -> divLabor;
                case "maxHierarchy" -> maxHier;
                case "largestCulture" -> largestCult;
                case "largestOrgComplexity" -> largestOrgComp;
                case "largestOrgEntropy" -> largestOrgEnt;
                case "avgTechLevel" -> tech;
                case "kardashevScale" -> kardashev;
                case "giniIndex" -> gini;
                case "gdpTotal" -> gdp;
                case "builtCapital" -> builtCap / Math.max(1, pop);
                case "eliteFormation" -> eliteForm;
                case "elderCapitalShare" -> elderCap;
                case "landRent" -> landRent;
                case "toolsCount" -> tools;
                case "productsCount" -> products;
                case "shannonBandwidth" -> shannonBw;
                case "collectiveMemory" -> memoryStock;
                case "innovationDiffusion" -> innovSpeed;
                case "knowledgeDecay" -> knowDecay;
                case "soilNPK" -> soilNPK;
                case "carbonFootprint" -> carbonFp;
                case "wildBiodiversity" -> wildBio;
                case "wetBulbSafety" -> wetBulb;
                case "eliteOverproduction" -> eliteOver;
                case "fiscalStress" -> fiscalStress;
                case "geopoliticalTension" -> geoTension;
                case "collapseVulnerability" -> collapseVuln;
                case "systemComplexity" -> sysComp;
                case "reconstructionCapability" -> reconCap;
                case "systemInterdependence" -> sysInter;
                case "engineTPS" -> tps;
                case "ramMemory" -> usedMem;
                case "cellCount" -> totalCells;
                default -> pop;
            };
        }
        return pop;
    }

    private void setCardVal(String key, String val, double ratio) {
        MetricCard c = metricCards.get(key);
        if (c != null) {
            c.updateValue(val, ratio);
        }
    }

    private void exportCsv() {
        StatsCsvExporter.exportCsv(engine, getScene() != null ? getScene().getWindow() : null);
    }
}
