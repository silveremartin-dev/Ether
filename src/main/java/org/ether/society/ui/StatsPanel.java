/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;
import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.i18n.I18n;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import javafx.util.StringConverter;

import java.util.*;

/**
 * Enhanced Cliodynamic & Physicalist Statistics Dashboard.
 * Features categorized metrics, dynamic historical time-series graphing,
 * search filtering, age pyramid visualization, and CSV export.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.0.0
 */
public class StatsPanel extends VBox {
    private final H3SimulationEngine engine;

    // Controls Header
    private final ComboBox<String> categoryFilterCombo;
    private final TextField searchField;
    private final ComboBox<String> chartMetricCombo;

    // Line Chart for selected metric
    private final LineChart<Number, Number> lineChart;
    private final XYChart.Series<Number, Number> chartSeries = new XYChart.Series<>();

    // Bar Chart for Age Pyramid / Distribution
    private final BarChart<String, Number> barChart;
    private final XYChart.Series<String, Number> barSeries = new XYChart.Series<>();

    // Metric Labels Registry
    private final Map<String, MetricCard> metricCards = new LinkedHashMap<>();

    // Pluggable Formula & Variance Analytics Engine
    private final PluggableStatEngine pluggableStatEngine = new PluggableStatEngine();
    private final VarianceDistributionPanel variancePanel;
    private final SpatialHeatmapPanel spatialHeatmapPanel = new SpatialHeatmapPanel();
    private boolean isLiveCollectionActive = true;
    private int samplingIntervalTicks = 1;
    private long tickCounter = 0;
    private java.util.function.Consumer<DisplayMode> onDisplayModeRequested;

    public void setOnDisplayModeRequested(java.util.function.Consumer<DisplayMode> listener) {
        this.onDisplayModeRequested = listener;
    }

    public void setSelectedMetricByDisplayMode(DisplayMode mode) {
        if (mode == null) return;
        String metricId = mode.getMetricId();
        org.ether.society.analytics.MetricDescriptor desc = org.ether.society.analytics.MetricRegistry.getInstance().getDescriptor(metricId);
        if (desc != null) {
            String title = desc.getDisplayName();
            if (chartMetricCombo != null && chartMetricCombo.getItems().contains(title)) {
                chartMetricCombo.setValue(title);
                resetChartSeries();
            }
        }
    }

    // Container for metric sections
    private final VBox metricsContainer;

    /** Class to hold UI elements for a single metric card */
    private static class MetricCard extends HBox {
        private final String key;
        private final String title;
        private final String category;
        private final Label valueLabel;
        private final ProgressBar progressBar;
        private final String unit;

        public MetricCard(String key, String title, String category, String unit, String tooltipText) {
            this.key = key;
            this.title = title;
            this.category = category;
            this.unit = unit;

            setPadding(new Insets(6, 10, 6, 10));
            setSpacing(10);
            setAlignment(Pos.CENTER_LEFT);
            getStyleClass().add("card-section");

            Label titleLabel = new Label(title + ":");
            titleLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");
            titleLabel.setMinWidth(170);
            titleLabel.setMaxWidth(170);

            valueLabel = new Label("-- " + unit);
            valueLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: bold;");
            valueLabel.setMinWidth(110);

            progressBar = new ProgressBar(0.0);
            progressBar.setPrefWidth(80);
            progressBar.setPrefHeight(8);
            progressBar.setStyle("-fx-accent: #38bdf8;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            getChildren().addAll(titleLabel, valueLabel, spacer, progressBar);

            if (tooltipText != null && !tooltipText.isBlank()) {
                Tooltip.install(this, new Tooltip(tooltipText));
            }
        }

        public void updateValue(String displayValue, double progressRatio) {
            valueLabel.setText(displayValue + (unit.isEmpty() ? "" : " " + unit));
            if (progressRatio >= 0) {
                progressBar.setProgress(Math.max(0.0, Math.min(1.0, progressRatio)));
                progressBar.setVisible(true);
            } else {
                progressBar.setVisible(false);
            }
        }

        public String getCategory() { return category; }
        public String getTitle() { return title; }
    }

    // Sliding Time Window size (-1 for unlimited)
    private int timeWindowSize = 200;

    public StatsPanel(H3SimulationEngine engine) {
        this.engine = engine;
        this.variancePanel = new VarianceDistributionPanel(pluggableStatEngine);

        setPadding(new Insets(12));
        setSpacing(12);
        getStyleClass().add("glass-panel");

        // --- TOP HEADER TOOLBAR ---
        Label headerTitle = new Label(I18n.getOrDefault("stats.header", "📊 STATISTICS & CLIODYNAMICS DASHBOARD"));
        headerTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        Label cpuNoticeLabel = new Label(
            "⚠️ EXPLICATION PERFORMANCE CPU : L'agrégation statistique en temps réel (indices Gini, entropie thermodynamique, " +
            "analyse Turchin, variances et maillages H3) effectue des calculs intensifs sur chaque cellule à chaque cycle. " +
            "Si la simulation ralentit, réduisez la fréquence d'échantillonnage ci-dessous (ex: 5 ou 20 ticks) ou mettez la collecte en pause."
        );
        cpuNoticeLabel.setWrapText(true);
        cpuNoticeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #93c5fd; -fx-padding: 4 8; -fx-background-color: rgba(30, 58, 138, 0.4); -fx-background-radius: 4; -fx-border-color: rgba(59, 130, 246, 0.4); -fx-border-radius: 4;");

        ToggleButton btnLiveCollection = new ToggleButton(I18n.getOrDefault("stats.btn.live_collection_active", "⚡ Collecte Stats : ACTIF"));
        btnLiveCollection.setSelected(true);
        btnLiveCollection.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.live_collection", "Toggle dynamic background statistics calculation to save CPU.")));
        btnLiveCollection.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        btnLiveCollection.setOnAction(e -> {
            isLiveCollectionActive = btnLiveCollection.isSelected();
            btnLiveCollection.setText(isLiveCollectionActive ? I18n.getOrDefault("stats.btn.live_collection_active", "⚡ Collecte Stats : ACTIF") : I18n.getOrDefault("stats.btn.live_collection_paused", "⏸️ Collecte Stats : EN PAUSE"));
            btnLiveCollection.setStyle(isLiveCollectionActive
                    ? "-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;"
                    : "-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 8;");
        });

        ComboBox<String> samplingCombo = new ComboBox<>();
        samplingCombo.getItems().addAll("1 Tick (Chaque Cycle)", "5 Ticks", "20 Ticks (~1 Secondes)", "100 Ticks (~5 Secondes)");
        samplingCombo.setValue("1 Tick (Chaque Cycle)");
        samplingCombo.setStyle("-fx-font-size: 10px;");
        samplingCombo.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.sampling", "Sampling rate and metrics refresh frequency.")));
        samplingCombo.setOnAction(e -> {
            int idx = samplingCombo.getSelectionModel().getSelectedIndex();
            samplingIntervalTicks = switch (idx) {
                case 1 -> 5;
                case 2 -> 20;
                case 3 -> 100;
                default -> 1;
            };
        });

        Button btnFormulaEditor = new Button(I18n.getOrDefault("stats.btn.formula_editor", "🧮 Formula & Variables Editor"));
        btnFormulaEditor.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.formula_editor", "Open interactive pluggable formula editor (SUM, AVG, MEDIAN, VAR, STDDEV, GINI, custom expressions).")));
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

        // --- SECTION 1: GRAPH SELECTION & TIME SERIES ---
        Label chartHeaderLabel = new Label(I18n.getOrDefault("stats.section.time_evolution", "📈 TIME EVOLUTION CHRONOLOGY (Sliding Curve)"));
        chartHeaderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        Label comboLabel = new Label(I18n.getOrDefault("stats.label.traced_stat", "Traced Statistic:"));
        comboLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: bold;");

        chartMetricCombo = new ComboBox<>();
        chartMetricCombo.getItems().addAll(
            "Population Humaine",
            "Survie de la population (%)",
            "Cohésion sociale (Asabiyyah %)",
            "Échelle de Kardashev (Type K)",
            "Énergie Captée (MW)",
            "Indice de Gini (Inégalités)",
            "Indice de Bonheur (%)",
            "Taux de Conflits (%)",
            "PIB Global (GDP)",
            "Espérance de Vie (ans)",
            "Division du Travail & Spécialisation",
            "Matière Déplacée & Capital Bâti",
            "Risque d'Effondrement Systémique (%)",
            "Pression Élitaire (Indice Turchin)",
            "Empreinte Carbone (GtCO2)",
            "Stock de Mémoire Collective (TB)",
            "Interdépendance & Complexité Systémique"
        );
        chartMetricCombo.setValue("Population Humaine");
        chartMetricCombo.setMaxWidth(Double.MAX_VALUE);
        chartMetricCombo.setOnAction(e -> resetChartSeries());

        // Sliding Time Window Toggle Buttons
        Label windowLabel = new Label(I18n.getOrDefault("stats.label.window", "Window:"));
        windowLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: bold;");

        ToggleGroup windowGroup = new ToggleGroup();
        ToggleButton btn1Yr = new ToggleButton("1 An");
        ToggleButton btn10Yr = new ToggleButton("10 Ans");
        ToggleButton btn100Yr = new ToggleButton("100 Ans");
        ToggleButton btn1000Yr = new ToggleButton("1000 Ans");
        ToggleButton btnAll = new ToggleButton("Tout");

        for (ToggleButton b : new ToggleButton[]{btn1Yr, btn10Yr, btn100Yr, btn1000Yr, btnAll}) {
            b.setToggleGroup(windowGroup);
            b.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 4;");
        }
        btn10Yr.setSelected(true);
        this.timeWindowSize = 120; // 10 years = 120 ticks (1 tick = 1 month)

        btn1Yr.setOnAction(e -> setTimeWindow(12));
        btn10Yr.setOnAction(e -> setTimeWindow(120));
        btn100Yr.setOnAction(e -> setTimeWindow(1200));
        btn1000Yr.setOnAction(e -> setTimeWindow(12000));
        btnAll.setOnAction(e -> setTimeWindow(-1));

        HBox windowBox = new HBox(6, windowLabel, btn1Yr, btn10Yr, btn100Yr, btn1000Yr, btnAll);
        windowBox.setAlignment(Pos.CENTER_LEFT);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(I18n.getOrDefault("stats.chart.time_axis", "Time (Year)"));
        xAxis.setTickLabelFill(Color.GRAY);
        xAxis.setAutoRanging(true);
        xAxis.setForceZeroInRange(false);
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object == null) return "";
                double val = object.doubleValue();
                long y = (long) Math.floor(val);
                long absY = Math.abs(y);
                if (y < 0) {
                    return String.format("%,d av. J.-C.", absY);
                } else if (y > 0) {
                    return String.format("%,d ap. J.-C.", y);
                } else {
                    return "An 0";
                }
            }
            @Override
            public Number fromString(String string) { return 0; }
        });

        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelFill(Color.GRAY);
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(I18n.getOrDefault("stats.chart.title", "Time Evolution Curve (💡 [CTRL] + Scroll to Zoom | [CTRL] + Drag to Pan | Double-Click to Reset)"));
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);
        lineChart.setPrefHeight(160);
        lineChart.getData().add(chartSeries);

        // Interactive Mouse Zoom (CTRL + Scroll Wheel) & Pan (CTRL + Drag)
        final double[] dragAnchor = new double[2];
        lineChart.setOnMousePressed(e -> {
            dragAnchor[0] = e.getX();
            dragAnchor[1] = e.getY();
        });
        lineChart.setOnMouseDragged(e -> {
            if (!e.isControlDown()) return;
            if (xAxis.isAutoRanging()) xAxis.setAutoRanging(false);
            double dx = e.getX() - dragAnchor[0];
            dragAnchor[0] = e.getX();
            double range = xAxis.getUpperBound() - xAxis.getLowerBound();
            double shift = (dx / Math.max(1.0, lineChart.getWidth())) * range;
            xAxis.setLowerBound(xAxis.getLowerBound() - shift);
            xAxis.setUpperBound(xAxis.getUpperBound() - shift);
        });
        lineChart.setOnScroll(e -> {
            if (!e.isControlDown()) return;
            e.consume();
            if (xAxis.isAutoRanging()) xAxis.setAutoRanging(false);
            double zoomFactor = e.getDeltaY() > 0 ? 0.85 : 1.15;
            double center = (xAxis.getLowerBound() + xAxis.getUpperBound()) / 2.0;
            double halfSpan = Math.max(1.0, ((xAxis.getUpperBound() - xAxis.getLowerBound()) / 2.0) * zoomFactor);
            xAxis.setLowerBound(center - halfSpan);
            xAxis.setUpperBound(center + halfSpan);
        });
        lineChart.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 || (e.isControlDown() && e.getButton() == javafx.scene.input.MouseButton.SECONDARY)) {
                xAxis.setAutoRanging(true);
                yAxis.setAutoRanging(true);
            }
        });

        VBox chartBox = new VBox(6, chartHeaderLabel, comboLabel, chartMetricCombo, windowBox, lineChart);
        chartBox.getStyleClass().add("card-section");

        // --- SECTION 2: DEMOGRAPHICS (AGE PYRAMID) ---
        Label barHeaderLabel = new Label(I18n.getOrDefault("stats.section.age_pyramid", "📊 DEMOGRAPHIC BREAKDOWN (Age Pyramid)"));
        barHeaderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        CategoryAxis barX = new CategoryAxis();
        barX.setTickLabelFill(Color.GRAY);
        NumberAxis barY = new NumberAxis();
        barY.setTickLabelFill(Color.GRAY);
        barChart = new BarChart<>(barX, barY);
        barChart.setTitle(I18n.getOrDefault("stats.chart.age_pyramid_title", "Age Pyramid"));
        barChart.setAnimated(false);
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(130);
        barChart.getData().add(barSeries);

        VBox barBox = new VBox(6, barHeaderLabel, barChart);
        barBox.getStyleClass().add("card-section");

        // --- SECTION 3: METRIC CARDS & CATEGORY FILTER ---
        Label metricsHeaderLabel = new Label(I18n.getOrDefault("stats.section.detailed_metrics", "📋 DETAILED METRICS & CLIODYNAMIC INDICATORS"));
        metricsHeaderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        // Interactive Metric Inspector Panel
        Label metricInspectorTitle = new Label(I18n.getOrDefault("stats.label.metric_explanation", "🔎 Cliodynamic Metric Explanation:"));
        metricInspectorTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ffd700;");
        Label metricInspectorText = new Label(I18n.getOrDefault("stats.desc.hover_metric", "Hover over or click a statistic card below to display its mathematical formula, calculation method, and societal meaning."));
        metricInspectorText.setWrapText(true);
        metricInspectorText.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

        VBox metricInspectorCard = new VBox(4, metricInspectorTitle, metricInspectorText);
        metricInspectorCard.setStyle("-fx-padding: 8 10; -fx-background-color: rgba(255, 215, 0, 0.08); -fx-background-radius: 6; -fx-border-color: rgba(255, 215, 0, 0.25); -fx-border-radius: 6;");

        // Category Filter
        categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.getItems().addAll(
            "Toutes les Catégories",
            "⚡ Énergie & Matière",
            "👥 Démographie & Santé",
            "🏛️ Société & Institutions",
            I18n.getOrDefault("formula_editor.default.category", "💎 Economy & Wealth"),
            "🧠 Cognition & Information",
            "🌍 Écologie & Frontières Planétaires",
            "⏳ Cliodynamique & Risques Systémiques",
            "⚙️ Complexité Systémique",
            "💻 Performances Techniques"
        );
        categoryFilterCombo.setValue("Toutes les Catégories");
        categoryFilterCombo.setMaxWidth(Double.MAX_VALUE);
        categoryFilterCombo.setOnAction(e -> filterMetrics());

        // Search Field
        searchField = new TextField();
        searchField.setPromptText(I18n.getOrDefault("stats.prompt.filter", "🔍 Filter a statistic (e.g. Kardashev, Gini, Collapse, NPK)..."));
        searchField.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-text-fill: white; -fx-prompt-text-fill: #64748b;");
        searchField.textProperty().addListener((obs, oldV, newV) -> filterMetrics());

        // Export Button
        Button exportBtn = new Button(I18n.getOrDefault("stats.btn.export_csv", "📥 Export Data (CSV)"));
        exportBtn.setTooltip(new Tooltip(I18n.getOrDefault("stats.tooltip.export_csv", "Export complete history of societal, energy, and economic metrics as CSV.")));
        exportBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6;");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> exportCsv());

        VBox cardsControlBox = new VBox(6, metricsHeaderLabel, metricInspectorCard, categoryFilterCombo, searchField, exportBtn);
        cardsControlBox.getStyleClass().add("card-section");

        // Container for metric sections
        metricsContainer = new VBox(6);

        registerMetricCards(metricInspectorTitle, metricInspectorText);

        // Populate Chart Metric Combo with ALL registered metrics dynamically
        chartMetricCombo.getItems().clear();
        for (MetricCard card : metricCards.values()) {
            chartMetricCombo.getItems().add(card.getTitle());
        }
        chartMetricCombo.setValue("Population Humaine");
        chartMetricCombo.setOnAction(e -> {
            resetChartSeries();
            String title = chartMetricCombo.getValue();
            if (title != null && onDisplayModeRequested != null) {
                org.ether.society.analytics.MetricDescriptor desc = org.ether.society.analytics.MetricRegistry.getInstance().getDescriptorByName(title);
                if (desc != null) {
                    onDisplayModeRequested.accept(DisplayMode.fromMetricId(desc.getId()));
                }
            }
        });

        getChildren().addAll(topControlsBox, chartBox, barBox, spatialHeatmapPanel, variancePanel, cardsControlBox, metricsContainer);
    }

    private void setTimeWindow(int window) {
        this.timeWindowSize = window;
        while (timeWindowSize > 0 && chartSeries.getData().size() > timeWindowSize) {
            chartSeries.getData().remove(0);
        }
    }

    private void registerMetricCards(Label inspectorTitle, Label inspectorText) {
        // Category 1: Énergie & Matière
        addCard("energyCaptured", "Énergie Captée", "⚡ Énergie & Matière", "MW",
            "P_tot = ∑ (P_solaire + P_biomasse + P_géothermie). Total de la puissance brute extraite du milieu physique par la société.", inspectorTitle, inspectorText);
        addCard("resourceDepletion", "Déplétion des Ressources", "⚡ Énergie & Matière", "%",
            "D = (Stock_initial - Stock_actuel) / Stock_initial. Pourcentage cumulé de consommation des réserves minérales non-renouvelables.", inspectorTitle, inspectorText);
        addCard("energyPerCapita", "Énergie / Individu", "⚡ Énergie & Matière", "MJ/hab",
            "E_cap = P_tot / N_pop. Énergie primaire utilisable disponible par habitant selon la loi de Leslie White (Culture = E × T).", inspectorTitle, inspectorText);
        addCard("foodPerCapita", "Nourriture / Individu", "⚡ Énergie & Matière", "mois/hab",
            "F_cap = Stock_Alimentaire / (N_pop × Consommation_mensuelle). Autonomie métabolique résiduelle sans nouvelle récolte.", inspectorTitle, inspectorText);
        addCard("pibMaterialFlow", "PIB Flux de Matière", "⚡ Énergie & Matière", "Mt/an", "Volume total de biomasse et de minerais déplacé par le métabolisme industriel.", inspectorTitle, inspectorText);
        addCard("biomassNatural", "Biomasse Naturelle", "⚡ Énergie & Matière", "GtC", "Stock total de carbone végétal et faunique sauvage préservé.", inspectorTitle, inspectorText);
        addCard("biomassDomesticated", "Biomasse Domestiquée", "⚡ Énergie & Matière", "GtC", "Biomasse totale des cultures agricoles et du bétail domestique.", inspectorTitle, inspectorText);
        addCard("potableWater", "Eau Douce & Aquifères", "⚡ Énergie & Matière", "10³ km³", "Réserves globales d'eau potable et nappe phréatique continentale.", inspectorTitle, inspectorText);
        addCard("remainingResources", "Ressources Restantes", "⚡ Énergie & Matière", "%", "Capital minéral et géologique non-extrait restant au sol.", inspectorTitle, inspectorText);
        addCard("entropyPollution", "Entropie & Pollution", "⚡ Énergie & Matière", "Idx", "Génération d'entropie thermodynamique et rejets polluants.", inspectorTitle, inspectorText);
        addCard("occupiedTerritory", "Territoire Occupé", "⚡ Énergie & Matière", "km²", "Surface géographique totale colonisée ou exploitée.", inspectorTitle, inspectorText);

        // Category 2: Démographie & Santé
        addCard("population", "Population Humaine", "👥 Démographie & Santé", "hab", "Population totale d'habitants sur la planète.", inspectorTitle, inspectorText);
        addCard("fertilityRate", "Taux de Fertilité", "👥 Démographie & Santé", "enf/femme", "Nombre moyen d'enfants par femme en âge de procréer.", inspectorTitle, inspectorText);
        addCard("offspringPct", "Taux avec Descendance", "👥 Démographie & Santé", "%", "Proportion d'adultes ayant au moins un descendant.", inspectorTitle, inspectorText);
        addCard("ageFirstChild", "Âge au 1er Enfant", "👥 Démographie & Santé", "ans", "Âge moyen de la mère à la naissance du premier enfant.", inspectorTitle, inspectorText);
        addCard("immigrationRate", "Taux d'Immigration", "👥 Démographie & Santé", "‰", "Flux migratoires nets inter-régionaux.", inspectorTitle, inspectorText);
        addCard("lifeExpectancy", "Espérance de Vie", "👥 Démographie & Santé", "ans", "Espérance de vie moyenne à la naissance.", inspectorTitle, inspectorText);
        addCard("healthIndex", "Niveau de Santé Global", "👥 Démographie & Santé", "%", "Indice global de résistance sanitaire et d'immunité.", inspectorTitle, inspectorText);
        addCard("educationLevel", "Niveau d'Éducation", "👥 Démographie & Santé", "%", "Taux d'instruction et capital de savoir accumulé.", inspectorTitle, inspectorText);

        // Category 3: Société & Institutions
        addCard("happinessIndex", "Indice de Bonheur", "🏛️ Société & Institutions", "%", "Niveau de satisfaction globale et de bien-être mesuré.", inspectorTitle, inspectorText);
        addCard("conflictLevel", "Taux de Conflits", "🏛️ Société & Institutions", "%", "Intensité des frictions sociales, guerres et violence.", inspectorTitle, inspectorText);
        addCard("cityStates", "Nombre de Cités-États", "🏛️ Société & Institutions", "cités", "Pôles autonomes d'administration et institutions urbaines.", inspectorTitle, inspectorText);
        addCard("institutionalMaturity", "Naissance des Institutions", "🏛️ Société & Institutions", "Idx", "Maturité juridique, administrative et étatiste.", inspectorTitle, inspectorText);
        addCard("divisionLabor", "Division du Travail", "🏛️ Société & Institutions", "Idx", "Niveau de spécialisation des métiers et de différenciation sociale.", inspectorTitle, inspectorText);
        addCard("maxHierarchy", "Niveau Max Hiérarchique", "🏛️ Société & Institutions", "Niv", "Niveau d'empilement institutionnel (Tribu 1 ➔ Empire/Réseau 6).", inspectorTitle, inspectorText);
        addCard("largestCulture", "Plus Grande Unité Culturelle", "🏛️ Société & Institutions", "hab", "Taille de la plus vaste confédération culturelle/politique.", inspectorTitle, inspectorText);
        addCard("largestOrgComplexity", "Complexité Max Organisation", "🏛️ Société & Institutions", "Idx", "Complexité sociétale de la plus grande organisation/empire (Population × Tech × Capacités d'État × Hiérarchie).", inspectorTitle, inspectorText);
        addCard("largestOrgEntropy", "Entropie Max Civilisation", "⚡ Énergie & Matière", "J/K", "Génération d'entropie thermodynamique et rejet de chaleur résiduelle de la plus grande civilisation.", inspectorTitle, inspectorText);
        addCard("avgTechLevel", "Niveau Technologique Moyen", "🏛️ Société & Institutions", "Niv", "Moyenne globale du niveau d'avancement scientifique et technologique.", inspectorTitle, inspectorText);
        addCard("kardashevScale", "Échelle de Kardashev", "🏛️ Société & Institutions", "Type K", "K = (log10(P_watts) - 6) / 10. Niveau de maîtrise énergétique globale (Type 0.0 à 1.0+).", inspectorTitle, inspectorText);

        // Category 4: Économie & Richesse
        addCard("giniIndex", "Indice de Gini (Inégalité)", I18n.getOrDefault("formula_editor.default.category", "💎 Economy & Wealth"), "Coeff", "G = A / (A + B). Mesure de concentration des richesses (0 = égalité, 1 = inégalité absolue).", inspectorTitle, inspectorText);
        addCard("gdpTotal", "PIB Global (GDP)", I18n.getOrDefault("formula_editor.default.category", "💎 Economy & Wealth"), "G$", "Produit Intérieur Brut total converti en monnaie constante.", inspectorTitle, inspectorText);
        addCard("builtCapital", "Capital Bâti & Outillage", I18n.getOrDefault("formula_editor.default.category", "💎 Economy & Wealth"), "kg/hab", "Stock total d'infrastructures physiques et de machines.", inspectorTitle, inspectorText);
        addCard("eliteFormation", "Formation d'Élite", I18n.getOrDefault("formula_editor.default.category", "💎 Economy & Wealth"), "%", "Proportion de la population détenant les fonctions de commandement.", inspectorTitle, inspectorText);
        addCard("elderCapitalShare", "Possession Capital (Aînés)", I18n.getOrDefault("formula_editor.default.category", "💎 Economy & Wealth"), "%", "Part de la richesse foncière détenue par la tranche d'âge senior.", inspectorTitle, inspectorText);
        addCard("landRent", "Rente Foncière & Immobilière", I18n.getOrDefault("formula_editor.default.category", "💎 Economy & Wealth"), "Idx", "Valorisation de la rente du sol liée à la densité et aux infrastructures.", inspectorTitle, inspectorText);
        addCard("toolsCount", "Nombre d'Outils en Service", I18n.getOrDefault("formula_editor.default.category", "💎 Economy & Wealth"), "unités", "Quantité totale d'outils et équipements de production.", inspectorTitle, inspectorText);
        addCard("productsCount", "Variété de Produits", I18n.getOrDefault("formula_editor.default.category", "💎 Economy & Wealth"), "types", "Diversité des produits manufacturés au catalogue technique.", inspectorTitle, inspectorText);

        // Category 5: Cognition & Information
        addCard("shannonBandwidth", "Bande Passante Shannon", "🧠 Cognition & Information", "Gbps", "Débit maximal de transmission d'information à travers le réseau civilisationnel.", inspectorTitle, inspectorText);
        addCard("collectiveMemory", "Stock Mémoire Collective", "🧠 Cognition & Information", "TB", "Volume cumulé des connaissances, données et patrimoines écrits.", inspectorTitle, inspectorText);
        addCard("innovationDiffusion", "Vitesse de Diffusion Tech", "🧠 Cognition & Information", "km/an", "Vitesse de propagation spatiale des nouvelles technologies.", inspectorTitle, inspectorText);
        addCard("knowledgeDecay", "Taux d'Amnésie Historique", "🧠 Cognition & Information", "%/décade", "Vitesse de déperdition ou d'oubli du savoir lors des crises.", inspectorTitle, inspectorText);

        // Category 6: Écologie & Frontières Planétaires
        addCard("soilNPK", "Qualité NPK des Sols", "🌍 Écologie & Frontières Planétaires", "%", "Indice de fertilité et teneur en nutriments organiques des sols cultivés.", inspectorTitle, inspectorText);
        addCard("carbonFootprint", "Empreinte Carbone", "🌍 Écologie & Frontières Planétaires", "GtCO₂", "Émissions annuelles de gaz à effet de serre et carbone fossile.", inspectorTitle, inspectorText);
        addCard("wildBiodiversity", "Biodiversité Sauvage", "🌍 Écologie & Frontières Planétaires", "%", "Part de la biomasse faunique et florale sauvage préservée.", inspectorTitle, inspectorText);
        addCard("wetBulbSafety", "Marge Sécurité Bulbe Humide", "🌍 Écologie & Frontières Planétaires", "°C", "Écart de température avec le seuil létal de bulbe humide (35°C).", inspectorTitle, inspectorText);

        // Category 7: Cliodynamique & Risques Systémiques
        addCard("eliteOverproduction", "Surproduction Élitaire (Turchin)", "⏳ Cliodynamique & Risques Systémiques", "Idx", "PSI = (Élites_aspirantes / Postes_disponibles) × Inégalité. Ratio de compétition pour le pouvoir (Indice PSI de Turchin).", inspectorTitle, inspectorText);
        addCard("fiscalStress", "Pression & Stress Fiscal", "⏳ Cliodynamique & Risques Systémiques", "%", "Stress financier et charge de maintien des institutions publiques.", inspectorTitle, inspectorText);
        addCard("geopoliticalTension", "Tension Géopolitique", "⏳ Cliodynamique & Risques Systémiques", "%", "Friction diplomatique et risque d'escalade guerrière multipolaire.", inspectorTitle, inspectorText);
        addCard("collapseVulnerability", "Risque d'Effondrement", "⏳ Cliodynamique & Risques Systémiques", "%", "Probabilité d'effondrement systémique ou de boucle d'entropie.", inspectorTitle, inspectorText);

        // Category 8: Complexité Systémique
        addCard("systemComplexity", "Complexité Systémique", "⚙️ Complexité Systémique", "Idx", "Indice d'interconnexion des rouages économiques et sociaux.", inspectorTitle, inspectorText);
        addCard("reconstructionCapability", "Capacité à Reconstruire", "⚙️ Complexité Systémique", "%", "Résilience et capacité à rebâtir la civilisation à partir de zéro.", inspectorTitle, inspectorText);
        addCard("systemInterdependence", "Interdépendance (Rouages)", "⚙️ Complexité Systémique", "%", "Fragilité systémique liée à l'interdépendance des chaînes logistiques.", inspectorTitle, inspectorText);

        // Category 9: Performances Engine
        addCard("engineTPS", "Fréquence de Calcul (Ticks/s)", "💻 Performances Techniques", "ticks/s", "Fréquence réelle de calcul du moteur de simulation (ticks par seconde).", inspectorTitle, inspectorText);
        addCard("ramMemory", "Utilisation Mémoire RAM", "💻 Performances Techniques", "MB", "Consommation mémoire vive du moteur.", inspectorTitle, inspectorText);
        addCard("cellCount", "Cellules Hexagonales H3", "💻 Performances Techniques", "hex", "Nombre total de mailles hexagonales chargées en mémoire.", inspectorTitle, inspectorText);
    }

    private void addCard(String key, String title, String category, String unit, String tooltip, Label inspectorTitle, Label inspectorText) {
        MetricCard card = new MetricCard(key, title, category, unit, tooltip);
        card.setOnMouseEntered(e -> {
            inspectorTitle.setText("🔎 " + title + " [" + category + "]");
            inspectorText.setText(tooltip);
        });
        card.setOnMouseClicked(e -> {
            inspectorTitle.setText("🔎 " + title + " [" + category + "]");
            inspectorText.setText(tooltip);
            if (chartMetricCombo != null) {
                chartMetricCombo.setValue(title);
                resetChartSeries();
            }
            if (onDisplayModeRequested != null) {
                onDisplayModeRequested.accept(DisplayMode.fromMetricId(key));
            }
        });
        metricCards.put(key, card);
    }

    private void buildMetricsList() {
        metricsContainer.getChildren().clear();
        String currentCat = "";

        for (MetricCard card : metricCards.values()) {
            if (!card.getCategory().equals(currentCat)) {
                currentCat = card.getCategory();
                Label catHeader = new Label(currentCat.toUpperCase());
                catHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-padding: 8 0 2 0;");
                metricsContainer.getChildren().add(catHeader);
            }
            metricsContainer.getChildren().add(card);
        }
    }

    private void filterMetrics() {
        String catFilter = categoryFilterCombo.getValue();
        String searchText = searchField.getText().toLowerCase().trim();

        metricsContainer.getChildren().clear();
        String currentCat = "";

        for (MetricCard card : metricCards.values()) {
            boolean matchesCat = "Toutes les Catégories".equals(catFilter) || card.getCategory().equalsIgnoreCase(catFilter);
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

    public void reset() {
        chartSeries.getData().clear();
        barSeries.getData().clear();
        tickCounter = 0;
    }

    public void resetChartSeries() {
        chartSeries.getData().clear();
        if (engine != null && engine.getHistoryManager() != null && engine.getHistoryManager().getHistory() != null) {
            String selectedMetric = chartMetricCombo != null ? chartMetricCombo.getValue() : "Population Humaine";
            double currentTime = engine.getTimeManager().getCurrentYear() + (engine.getTimeManager().getCurrentMonth() / 12.0);
            List<org.ether.society.analytics.HistorySnapshot> snapshots = engine.getHistoryManager().getHistory().getSnapshots();
            for (org.ether.society.analytics.HistorySnapshot snap : snapshots) {
                double snapTime = snap.year() + (snap.month() / 12.0);
                if (snapTime <= currentTime + 0.0001) {
                    double val = snap.getMetricValue(selectedMetric);
                    chartSeries.getData().add(new XYChart.Data<>(snapTime, val));
                }
            }
            while (timeWindowSize > 0 && chartSeries.getData().size() > timeWindowSize) {
                chartSeries.getData().remove(0);
            }
        }
        update();
    }

    private final java.util.concurrent.atomic.AtomicLong lastStatsUiUpdateNanos = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicBoolean statsUpdatePending = new java.util.concurrent.atomic.AtomicBoolean(false);

    public void update() {
        if (engine == null || !isLiveCollectionActive) return;
        tickCounter++;
        if (tickCounter % samplingIntervalTicks != 0) return;

        long nowNanos = System.nanoTime();
        if (nowNanos - lastStatsUiUpdateNanos.get() < 100_000_000L) { // Throttle to max 10 UI updates per second
            return;
        }
        if (!statsUpdatePending.compareAndSet(false, true)) {
            return;
        }
        lastStatsUiUpdateNanos.set(nowNanos);

        // Extract values from engine
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

                // Update cards
                setCardVal("energyCaptured", String.format("%,.1f", energyCap), energyCap / 100000.0);
                setCardVal("resourceDepletion", String.format("%.1f", resDep), resDep / 100.0);
                setCardVal("energyPerCapita", String.format("%.1f", energyPerCap), energyPerCap / 500.0);
                setCardVal("foodPerCapita", String.format("%.2f", foodPerCap), foodPerCap / 24.0);
                setCardVal("pibMaterialFlow", String.format("%.1f", resDep * 5.2), resDep / 100.0);
                setCardVal("biomassNatural", String.format("%,.0f", bio), bio / 5000.0);
                setCardVal("biomassDomesticated", String.format("%,.1f", bioDom), bioDom / 1000.0);
                setCardVal("potableWater", String.format("%,.0f", water), water / 50000.0);
                setCardVal("remainingResources", String.format("%.1f", remRes), remRes / 100.0);
                setCardVal("entropyPollution", String.format("%.1f", entropy), entropy / 1000.0);
                setCardVal("occupiedTerritory", String.format("%,.0f", territory), territory / 1000000.0);

                setCardVal("population", String.format("%,d", pop), pop / 10000000.0);
                setCardVal("fertilityRate", String.format("%.1f", fert), fert / 7.0);
                setCardVal("offspringPct", String.format("%.1f", offspring), offspring / 100.0);
                setCardVal("ageFirstChild", String.format("%.1f", ageFirstChild), ageFirstChild / 40.0);
                setCardVal("immigrationRate", String.format("%.1f", immigration), immigration / 50.0);
                setCardVal("lifeExpectancy", String.format("%.1f", life), life / 100.0);
                setCardVal("healthIndex", String.format("%.1f", Math.min(100.0, life * 1.1)), life / 100.0);
                setCardVal("educationLevel", String.format("%.1f", education), education / 100.0);

                double largestOrgComp = engine.getLargestOrganizationComplexity();
                double largestOrgEnt = engine.getLargestOrganizationEntropy();

                setCardVal("happinessIndex", String.format("%.1f", happiness), happiness / 100.0);
                setCardVal("conflictLevel", String.format("%.1f", conflict), conflict / 100.0);
                setCardVal("cityStates", String.format("%d", cityStates), cityStates / 50.0);
                setCardVal("institutionalMaturity", String.format("%.1f", instMaturity), instMaturity / 100.0);
                setCardVal("divisionLabor", String.format("%.1f", divLabor), divLabor / 100.0);
                setCardVal("maxHierarchy", String.format("Niv %d", maxHier), maxHier / 6.0);
                setCardVal("largestCulture", String.format("%,d", largestCult), largestCult / Math.max(1.0, (double)pop));
                setCardVal("largestOrgComplexity", String.format("%,.0f", largestOrgComp), largestOrgComp / 100000.0);
                setCardVal("largestOrgEntropy", String.format("%,.1f", largestOrgEnt), largestOrgEnt / 100000.0);
                setCardVal("avgTechLevel", String.format("%.2f", tech), tech / 100.0);
                setCardVal("kardashevScale", String.format("%.2f", kardashev), kardashev / 2.0);

                setCardVal("giniIndex", String.format("%.2f", gini), gini);
                setCardVal("gdpTotal", String.format("%,.0f", gdp), gdp / 1000000.0);
                setCardVal("builtCapital", String.format("%,.0f", builtCap / Math.max(1, pop)), builtCap / (pop * 5000.0 + 1));
                setCardVal("eliteFormation", String.format("%.1f", eliteForm), eliteForm / 25.0);
                setCardVal("elderCapitalShare", String.format("%.1f", elderCap), elderCap / 100.0);
                setCardVal("landRent", String.format("%.1f", landRent), landRent / 100.0);
                setCardVal("toolsCount", String.format("%,d", tools), tools / 10000000.0);
                setCardVal("productsCount", String.format("%,d", products), products / 10000.0);

                setCardVal("shannonBandwidth", String.format("%.1f", shannonBw), shannonBw / 100.0);
                setCardVal("collectiveMemory", String.format("%,.0f", memoryStock), memoryStock / 10000.0);
                setCardVal("innovationDiffusion", String.format("%.1f", innovSpeed), innovSpeed / 100.0);
                setCardVal("knowledgeDecay", String.format("%.1f", knowDecay), knowDecay / 100.0);

                setCardVal("soilNPK", String.format("%.1f", soilNPK), soilNPK / 100.0);
                setCardVal("carbonFootprint", String.format("%.2f", carbonFp), carbonFp / 50.0);
                setCardVal("wildBiodiversity", String.format("%.1f", wildBio), wildBio / 100.0);
                setCardVal("wetBulbSafety", String.format("%.1f", wetBulb), wetBulb / 35.0);

                setCardVal("eliteOverproduction", String.format("%.2f", eliteOver), eliteOver / 10.0);
                setCardVal("fiscalStress", String.format("%.1f", fiscalStress), fiscalStress / 100.0);
                setCardVal("geopoliticalTension", String.format("%.1f", geoTension), geoTension / 100.0);
                setCardVal("collapseVulnerability", String.format("%.1f", collapseVuln), collapseVuln / 100.0);

                setCardVal("systemComplexity", String.format("%.1f", sysComp), sysComp / 100.0);
                setCardVal("reconstructionCapability", String.format("%.1f", reconCap), reconCap / 100.0);
                setCardVal("systemInterdependence", String.format("%.1f", sysInter), sysInter / 100.0);

                setCardVal("engineTPS", String.format("%.1f", tps), tps / 60.0);
                setCardVal("ramMemory", String.format("%d", usedMem), usedMem / 4096.0);
                setCardVal("cellCount", String.format("%,d", totalCells), totalCells / 50000.0);

                // Update Time Series Chart
                String selectedMetric = chartMetricCombo.getValue();
                double yVal = switch (selectedMetric) {
                    // Category 1: Énergie & Matière
                    case "Énergie Captée" -> energyCap;
                    case "Déplétion des Ressources" -> resDep;
                    case "Énergie / Individu" -> energyPerCap;
                    case "Nourriture / Individu" -> foodPerCap;
                    case "PIB Flux de Matière" -> resDep * 5.2;
                    case "Biomasse Naturelle" -> bio;
                    case "Biomasse Domestiquée" -> bioDom;
                    case "Eau Douce & Aquifères" -> water;
                    case "Ressources Restantes" -> remRes;
                    case "Entropie & Pollution" -> entropy;
                    case "Territoire Occupé" -> territory;

                    // Category 2: Démographie & Santé
                    case "Population Humaine" -> pop;
                    case "Survie de la population (%)" -> engine.getPopulationSurvivalRate();
                    case "Cohésion sociale (Asabiyyah %)" -> engine.getAverageAsabiyyah();
                    case "Taux de Fertilité" -> fert;
                    case "Taux avec Descendance" -> offspring;
                    case "Âge au 1er Enfant" -> ageFirstChild;
                    case "Taux d'Immigration" -> immigration;
                    case "Espérance de Vie", "Espérance de Vie (ans)" -> life;
                    case "Niveau de Santé Global" -> Math.min(100.0, life * 1.1);
                    case "Niveau d'Éducation" -> education;

                    // Category 3: Société & Institutions
                    case "Indice de Bonheur" -> happiness;
                    case "Taux de Conflits" -> conflict;
                    case "Nombre de Cités-États" -> cityStates;
                    case "Naissance des Institutions" -> instMaturity;
                    case "Division du Travail" -> divLabor;
                    case "Niveau Max Hiérarchique" -> maxHier;
                    case "Plus Grande Unité Culturelle" -> largestCult;
                    case "Complexité Max Organisation" -> largestOrgComp;
                    case "Entropie Max Civilisation" -> largestOrgEnt;
                    case "Niveau Technologique Moyen" -> tech;
                    case "Échelle de Kardashev" -> kardashev;

                    // Category 4: Économie & Richesse
                    case "Indice de Gini (Inégalité)" -> gini;
                    case "PIB Global (GDP)" -> gdp;
                    case "Capital Bâti & Outillage" -> builtCap / Math.max(1, pop);
                    case "Formation d'Élite" -> eliteForm;
                    case "Possession Capital (Aînés)" -> elderCap;
                    case "Rente Foncière & Immobilière" -> landRent;
                    case "Nombre d'Outils en Service" -> tools;
                    case "Variété de Produits" -> products;

                    // Category 5: Cognition & Information
                    case "Bande Passante Shannon" -> shannonBw;
                    case "Stock Mémoire Collective" -> memoryStock;
                    case "Vitesse de Diffusion Tech" -> innovSpeed;
                    case "Taux d'Amnésie Historique" -> knowDecay;

                    // Category 6: Écologie & Frontières Planétaires
                    case "Qualité NPK des Sols" -> soilNPK;
                    case "Empreinte Carbone" -> carbonFp;
                    case "Biodiversité Sauvage" -> wildBio;
                    case "Marge Sécurité Bulbe Humide" -> wetBulb;

                    // Category 7: Cliodynamique & Risques Systémiques
                    case "Surproduction Élitaire (Turchin)" -> eliteOver;
                    case "Pression & Stress Fiscal" -> fiscalStress;
                    case "Tension Géopolitique" -> geoTension;
                    case "Risque d'Effondrement" -> collapseVuln;

                    // Category 8: Complexité Systémique
                    case "Complexité Systémique" -> sysComp;
                    case "Capacité à Reconstruire" -> reconCap;
                    case "Interdépendance (Rouages)" -> sysInter;

                    // Category 9: Performances
                    case "Fréquence de Calcul (Ticks/s)", "Fréquence de Calcul (ticks/sec)", "TPS (Images/s)", "TPS", "tps" -> tps;
                    case "Utilisation Mémoire RAM" -> usedMem;
                    case "Cellules Hexagonales H3" -> totalCells;

                    default -> pop;
                };

                double currentTime = year + (engine.getTimeManager().getCurrentMonth() / 12.0);

                // If user rewound/stepped back in time, prune future data points from the active chart
                if (!chartSeries.getData().isEmpty()) {
                    chartSeries.getData().removeIf(data -> data.getXValue().doubleValue() > currentTime + 0.0001);
                }

                // If chart is empty but history snapshots exist (e.g. loaded game / initial metric selection), preload past snapshots
                if (chartSeries.getData().isEmpty() && engine.getHistoryManager() != null && engine.getHistoryManager().getHistory() != null) {
                    List<org.ether.society.analytics.HistorySnapshot> snapshots = engine.getHistoryManager().getHistory().getSnapshots();
                    for (org.ether.society.analytics.HistorySnapshot snap : snapshots) {
                        double snapTime = snap.year() + (snap.month() / 12.0);
                        if (snapTime < currentTime - 0.0001) {
                            double val = snap.getMetricValue(selectedMetric);
                            chartSeries.getData().add(new XYChart.Data<>(snapTime, val));
                        }
                    }
                }

                if (chartSeries.getData().isEmpty() || Math.abs(chartSeries.getData().get(chartSeries.getData().size() - 1).getXValue().doubleValue() - currentTime) >= 0.001) {
                    chartSeries.getData().add(new XYChart.Data<>(currentTime, yVal));
                    while (timeWindowSize > 0 && chartSeries.getData().size() > timeWindowSize) {
                        chartSeries.getData().remove(0);
                    }

                    // Update Age Pyramid Bar Chart (7 Cohorts)
                    int[] pyramid = engine.getAgePyramid();
                    barSeries.getData().clear();
                    if (pyramid != null && pyramid.length >= 7) {
                        barSeries.getData().add(new XYChart.Data<>("0-14 ans", pyramid[0]));
                        barSeries.getData().add(new XYChart.Data<>("15-24 ans", pyramid[1]));
                        barSeries.getData().add(new XYChart.Data<>("25-39 ans", pyramid[2]));
                        barSeries.getData().add(new XYChart.Data<>("40-54 ans", pyramid[3]));
                        barSeries.getData().add(new XYChart.Data<>("55-69 ans", pyramid[4]));
                        barSeries.getData().add(new XYChart.Data<>("70-84 ans", pyramid[5]));
                        barSeries.getData().add(new XYChart.Data<>("85+ ans", pyramid[6]));
                    } else if (pyramid != null && pyramid.length >= 3) {
                        barSeries.getData().add(new XYChart.Data<>("Jeunes (<15ans)", pyramid[0]));
                        barSeries.getData().add(new XYChart.Data<>("Adultes (15-60ans)", pyramid[1]));
                        barSeries.getData().add(new XYChart.Data<>("Aînés (>60ans)", pyramid[2]));
                    }

                    // Update Variance & Distribution Panel
                    variancePanel.updateData(engine.getCells());
                }
            } finally {
                statsUpdatePending.set(false);
            }
        });
    }

    private void setCardVal(String key, String val, double ratio) {
        MetricCard card = metricCards.get(key);
        if (card != null) {
            card.updateValue(val, ratio);
        }
    }

    private void exportCsv() {
        if (engine == null) return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(I18n.getOrDefault("stats.title.export_dialog", "Export Cliodynamic Statistics (CSV / Excel)"));
        fileChooser.setInitialFileName("ether_statistiques_cliodynamiques.csv");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Fichiers CSV (*.csv)", "*.csv"));

        java.io.File file = fileChooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file == null) return;

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
            writer.println("Annee;Mois;Population;Biomasse;Energie_Captee;Kardashev;Gini;PIB;Bonheur;Conflits;Esperance_Vie;Fecondite;Division_Travail;Complexite");

            int y = engine.getTimeManager().getCurrentYear();
            int m = engine.getTimeManager().getCurrentMonth();
            writer.println(String.format(java.util.Locale.US, "%d;%d;%d;%.2f;%.2f;%.3f;%.4f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f",
                y, m, engine.getTotalPopulation(), engine.getTotalBiomassNatural(), engine.getEnergyCaptured(),
                engine.getKardashevScale(), engine.getCurrentGini(), engine.getCurrentGDP(), engine.getHappinessIndex(),
                engine.getConflictLevel(), engine.getCurrentLifeExpectancy(), engine.getCurrentFertility(),
                engine.getDivisionOfLaborIndex(), engine.getSystemComplexityIndex()));

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(I18n.getOrDefault("stats.title.export_success", "Export Successful"));
            alert.setContentText("Données statistiques exportées avec succès dans :\n" + file.getAbsolutePath());
            alert.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur d'Exportation");
            alert.setContentText("Impossible d'exporter les statistiques : " + ex.getMessage());
            alert.showAndWait();
        }
    }
}
