package org.ether.society.ui;

import org.ether.society.core.dod.PluggableStatEngine;
import org.ether.society.core.dod.StatisticsKernel;
import org.ether.society.database.H3Cell;
import org.ether.society.i18n.I18n;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Dedicated Variance & Individual Distribution Analytics Panel.
 * Computes and displays variance between individuals/cells on an exhaustive catalog of simulation variables
 * (Physical, Climate, Demographics, Cohorts, Ecology, Minerals, Energy, Society & Capital),
 * standard deviation bounds (μ ± σ), Gini coefficient, and a 10-bin histogram distribution curve.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 */
public class VarianceDistributionPanel extends VBox {

    public static class VariableEntry {
        private final String key;
        private final String displayName;
        private final String category;
        private final boolean isHeader;

        public VariableEntry(String key, String displayName, String category) {
            this.key = key;
            this.displayName = displayName;
            this.category = category;
            this.isHeader = false;
        }

        private VariableEntry(String headerTitle) {
            this.key = null;
            this.displayName = headerTitle;
            this.category = null;
            this.isHeader = true;
        }

        public static VariableEntry header(String headerTitle) {
            return new VariableEntry(headerTitle);
        }

        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getCategory() { return category; }
        public boolean isHeader() { return isHeader; }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final PluggableStatEngine pluggableStatEngine;
    private final StatisticsKernel statisticsKernel = new StatisticsKernel();

    private final Label headerTitle;
    private final Label subtitle;
    private final Label comboPrompt;
    private final ComboBox<VariableEntry> variableCombo;

    private final Label kpiMeanTitle = new Label();
    private final Label kpiVarTitle = new Label();
    private final Label kpiStdTitle = new Label();
    private final Label kpiGiniTitle = new Label();
    private final Label kpiMinMaxTitle = new Label();
    private final Label kpiCvTitle = new Label();

    private final HBox meanKpiBox;
    private final HBox varKpiBox;
    private final HBox stdKpiBox;
    private final HBox giniKpiBox;
    private final HBox minMaxKpiBox;
    private final HBox cvKpiBox;

    private final Label lblMean = new Label("--");
    private final Label lblVariance = new Label("--");
    private final Label lblStdDev = new Label("--");
    private final Label lblGini = new Label("--");
    private final Label lblMinMax = new Label("-- / --");
    private final Label lblCv = new Label("-- %");
    private final Label lblSpreadDesc = new Label();

    private final CategoryAxis xAxis;
    private final NumberAxis yAxis;
    private final BarChart<String, Number> histogramChart;
    private final XYChart.Series<String, Number> histogramSeries = new XYChart.Series<>();

    private List<H3Cell> currentCells;

    public VarianceDistributionPanel(PluggableStatEngine pluggableStatEngine) {
        this.pluggableStatEngine = pluggableStatEngine;

        getStyleClass().add("card-section");
        setPadding(new Insets(10));
        setSpacing(10);

        // Header
        headerTitle = new Label();
        headerTitle.getStyleClass().add("label-title");
        headerTitle.setWrapText(true);

        subtitle = new Label();
        subtitle.getStyleClass().add("hint-label");
        subtitle.setWrapText(true);

        VBox headerBox = new VBox(3, headerTitle, subtitle);

        // Variable Selector
        comboPrompt = new Label();
        comboPrompt.getStyleClass().add("control-label");

        variableCombo = new ComboBox<>();
        variableCombo.setMaxWidth(Double.MAX_VALUE);
        variableCombo.setCellFactory(lv -> new ListCell<VariableEntry>() {
            @Override
            protected void updateItem(VariableEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if (item.isHeader()) {
                    setDisable(true);
                    setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 6; -fx-opacity: 1.0; -fx-background-color: rgba(30, 41, 59, 0.5);");
                    setText(item.getDisplayName());
                } else {
                    setDisable(false);
                    setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-padding: 3 12;");
                    setText("  " + item.getDisplayName());
                }
            }
        });

        variableCombo.setButtonCell(new ListCell<VariableEntry>() {
            @Override
            protected void updateItem(VariableEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                    setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
                }
            }
        });

        variableCombo.setOnAction(e -> {
            VariableEntry selected = variableCombo.getValue();
            if (selected != null && !selected.isHeader()) {
                updateData(currentCells);
            }
        });

        // KPI Summary Cards: Exactly 3 rows of 2 columns
        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(8);
        kpiGrid.setVgap(6);
        kpiGrid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        kpiGrid.getColumnConstraints().addAll(col1, col2);

        meanKpiBox = createMiniKpi(kpiMeanTitle, lblMean);
        varKpiBox = createMiniKpi(kpiVarTitle, lblVariance);
        stdKpiBox = createMiniKpi(kpiStdTitle, lblStdDev);
        giniKpiBox = createMiniKpi(kpiGiniTitle, lblGini);
        minMaxKpiBox = createMiniKpi(kpiMinMaxTitle, lblMinMax);
        cvKpiBox = createMiniKpi(kpiCvTitle, lblCv);

        // Row 0: Moyenne & Variance
        kpiGrid.add(meanKpiBox, 0, 0);
        kpiGrid.add(varKpiBox, 1, 0);

        // Row 1: Écart-Type & Gini
        kpiGrid.add(stdKpiBox, 0, 1);
        kpiGrid.add(giniKpiBox, 1, 1);

        // Row 2: Min/Max & Coeff de Variation
        kpiGrid.add(minMaxKpiBox, 0, 2);
        kpiGrid.add(cvKpiBox, 1, 2);

        lblSpreadDesc.getStyleClass().add("hint-label");
        lblSpreadDesc.setWrapText(true);
        VBox descCard = new VBox(lblSpreadDesc);
        descCard.getStyleClass().add("hint-card");

        // Histogram BarChart
        xAxis = new CategoryAxis();
        xAxis.setAnimated(false);
        yAxis = new NumberAxis();
        yAxis.setAnimated(false);

        histogramChart = new BarChart<>(xAxis, yAxis);
        histogramChart.setAnimated(false);
        histogramChart.setLegendVisible(false);
        histogramChart.setPrefHeight(180);
        histogramChart.getData().add(histogramSeries);

        getChildren().addAll(headerBox, comboPrompt, variableCombo, kpiGrid, descCard, histogramChart);

        updateTexts();
        I18n.languageProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    public void updateTexts() {
        headerTitle.setText(I18n.getOrDefault("variance.title", "📊 STATISTIQUES DE VARIANCE ENTRE INDIVIDUS & DISTRIBUTION"));
        subtitle.setText(I18n.getOrDefault("variance.subtitle", "Évalue à quel point les individus / mailles s'éloignent du schéma standard (Moyenne μ ± Écart-type σ)"));
        comboPrompt.setText(I18n.getOrDefault("variance.prompt.variable", "Variable analysée :"));

        String prevKey = (variableCombo.getValue() != null && !variableCombo.getValue().isHeader())
                ? variableCombo.getValue().getKey()
                : "wealth";

        variableCombo.getItems().clear();

        // 1. Économie & Société
        variableCombo.getItems().add(VariableEntry.header("─── 💎 " + I18n.getOrDefault("variance.cat.economy", "ÉCONOMIE, CAPITAL & SOCIÉTÉ") + " ───"));
        variableCombo.getItems().add(new VariableEntry("wealth", I18n.getOrDefault("variance.var.wealth", "💎 Richesse & Capital Bâti"), "economy"));
        variableCombo.getItems().add(new VariableEntry("tech", I18n.getOrDefault("variance.var.tech", "⚙️ Niveau Technologique"), "economy"));
        variableCombo.getItems().add(new VariableEntry("giniindex", I18n.getOrDefault("variance.var.gini", "⚖️ Indice d'Inégalité (Gini)"), "economy"));
        variableCombo.getItems().add(new VariableEntry("asabiyyah", I18n.getOrDefault("variance.var.asabiyyah", "⚔️ Cohésion Asabiyyah"), "economy"));

        // 2. Démographie, Santé & Cohortes d'Âge
        variableCombo.getItems().add(VariableEntry.header("─── 👥 " + I18n.getOrDefault("variance.cat.demography", "DÉMOGRAPHIE, SANTÉ & COHORTES") + " ───"));
        variableCombo.getItems().add(new VariableEntry("population", I18n.getOrDefault("variance.var.population", "👥 Densité de Population"), "demography"));
        variableCombo.getItems().add(new VariableEntry("lifespan", I18n.getOrDefault("variance.var.lifespan", "🏥 Espérance de Vie (ans)"), "demography"));
        variableCombo.getItems().add(new VariableEntry("fertility", I18n.getOrDefault("variance.var.fertility", "👶 Taux de Fertilité"), "demography"));
        variableCombo.getItems().add(new VariableEntry("popyouth", I18n.getOrDefault("variance.var.popyouth", "🧒 Cohorte Jeunesse (0-14 ans)"), "demography"));
        variableCombo.getItems().add(new VariableEntry("popadult", I18n.getOrDefault("variance.var.popadult", "🧑 Cohorte Adultes Actifs (15-64 ans)"), "demography"));
        variableCombo.getItems().add(new VariableEntry("popelderly", I18n.getOrDefault("variance.var.popelderly", "👴 Cohorte Aînés (65+ ans)"), "demography"));

        // 3. Écologie, Alimentation & Ressources
        variableCombo.getItems().add(VariableEntry.header("─── 🌾 " + I18n.getOrDefault("variance.cat.resources", "ÉCOLOGIE, SOLS & RESSOURCES") + " ───"));
        variableCombo.getItems().add(new VariableEntry("food", I18n.getOrDefault("variance.var.food", "🍞 Stocks Alimentaires"), "resources"));
        variableCombo.getItems().add(new VariableEntry("food_per_capita", I18n.getOrDefault("variance.var.food_per_capita", "🍞 Nourriture / Habitant"), "resources"));
        variableCombo.getItems().add(new VariableEntry("water", I18n.getOrDefault("variance.var.water", "💧 Ressources en Eau"), "resources"));
        variableCombo.getItems().add(new VariableEntry("aquifer", I18n.getOrDefault("variance.var.aquifer", "🚰 Nappe Aquifère"), "resources"));
        variableCombo.getItems().add(new VariableEntry("wood", I18n.getOrDefault("variance.var.wood", "🌲 Biomasse Forestière / Bois"), "resources"));
        variableCombo.getItems().add(new VariableEntry("soilcarbon", I18n.getOrDefault("variance.var.soil", "🌾 Fertilité Organique des Sols (NPK)"), "resources"));
        variableCombo.getItems().add(new VariableEntry("biomassnatural", I18n.getOrDefault("variance.var.biomassnatural", "🌿 Biomasse Sauvage"), "resources"));
        variableCombo.getItems().add(new VariableEntry("biomasslivestock", I18n.getOrDefault("variance.var.biomasslivestock", "🐄 Biomasse Bétail Domestique"), "resources"));

        // 4. Physique & Climat
        variableCombo.getItems().add(VariableEntry.header("─── 🌍 " + I18n.getOrDefault("variance.cat.physical", "PHYSIQUE, CLIMAT & ENVIRONNEMENT") + " ───"));
        variableCombo.getItems().add(new VariableEntry("temperature", I18n.getOrDefault("variance.var.temperature", "🌡️ Température de Surface"), "physical"));
        variableCombo.getItems().add(new VariableEntry("rainfall", I18n.getOrDefault("variance.var.rainfall", "🌧️ Précipitations Annuelles"), "physical"));
        variableCombo.getItems().add(new VariableEntry("elevation", I18n.getOrDefault("variance.var.elevation", "🏔️ Altitude & Relief Topographique"), "physical"));
        variableCombo.getItems().add(new VariableEntry("pollution", I18n.getOrDefault("variance.var.pollution", "☣️ Entropie & Pollution"), "physical"));
        variableCombo.getItems().add(new VariableEntry("friction", I18n.getOrDefault("variance.var.friction", "🧗 Friction de Déplacement"), "physical"));

        // 5. Énergie & Métabolisme
        variableCombo.getItems().add(VariableEntry.header("─── ⚡ " + I18n.getOrDefault("variance.cat.energy", "ÉNERGIE & MÉTABOLISME") + " ───"));
        variableCombo.getItems().add(new VariableEntry("energysolar", I18n.getOrDefault("variance.var.energysolar", "☀️ Énergie Solaire Captée"), "energy"));
        variableCombo.getItems().add(new VariableEntry("energywind", I18n.getOrDefault("variance.var.energywind", "💨 Énergie Éolienne"), "energy"));
        variableCombo.getItems().add(new VariableEntry("energyfire", I18n.getOrDefault("variance.var.energyfire", "🔥 Énergie Biomasse / Feu"), "energy"));
        variableCombo.getItems().add(new VariableEntry("metal", I18n.getOrDefault("variance.var.metal", "⛏️ Gisements Minéraux & Métaux"), "energy"));

        VariableEntry toSelect = null;
        for (VariableEntry entry : variableCombo.getItems()) {
            if (!entry.isHeader() && entry.getKey().equalsIgnoreCase(prevKey)) {
                toSelect = entry;
                break;
            }
        }
        if (toSelect == null) {
            for (VariableEntry entry : variableCombo.getItems()) {
                if (!entry.isHeader()) {
                    toSelect = entry;
                    break;
                }
            }
        }
        variableCombo.setValue(toSelect);

        kpiMeanTitle.setText(I18n.getOrDefault("variance.kpi.mean", "Moyenne (μ) :"));
        kpiVarTitle.setText(I18n.getOrDefault("variance.kpi.variance", "Variance (σ²) :"));
        kpiStdTitle.setText(I18n.getOrDefault("variance.kpi.stddev", "Écart-Type (σ) :"));
        kpiGiniTitle.setText(I18n.getOrDefault("variance.kpi.gini", "Indice Gini :"));
        kpiMinMaxTitle.setText(I18n.getOrDefault("variance.kpi.minmax", "Min / Max :"));
        kpiCvTitle.setText(I18n.getOrDefault("variance.kpi.cv", "Coeff. Var. (CV) :"));

        Tooltip.install(meanKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.mean", "Valeur moyenne standard de la population")));
        Tooltip.install(varKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.variance", "Mesure de la dispersion quadratique (σ²) des individus par rapport à la moyenne")));
        Tooltip.install(stdKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.stddev", "Écart moyen par rapport au schéma standard (μ ± σ)")));
        Tooltip.install(giniKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.gini", "Mesure d'inégalité (0 = distribution parfaitement égale, 1 = concentration totale)")));
        Tooltip.install(minMaxKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.minmax", "Valeurs extrêmes minimale et maximale enregistrées")));
        Tooltip.install(cvKpiBox, new Tooltip(I18n.getOrDefault("variance.tooltip.cv", "Coefficient de variation (Écart-type / Moyenne × 100). Hétérogénéité relative normalisée")));

        xAxis.setLabel(I18n.getOrDefault("variance.chart.xaxis", "Tranches de valeurs (Intervalles de dispersion)"));
        yAxis.setLabel(I18n.getOrDefault("variance.chart.yaxis", "Nombre d'Individus / Cellules"));
        histogramChart.setTitle(I18n.getOrDefault("variance.chart.title", "Courbe de distribution et variance (Histogramme 10 classes)"));

        if (currentCells != null) {
            updateData(currentCells);
        } else {
            lblSpreadDesc.setText(I18n.getOrDefault("variance.desc.analyzing", "Analyse du schéma de dispersion par rapport à la moyenne standard..."));
        }
    }

    private HBox createMiniKpi(Label titleLabel, Label valLabel) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 6, 4, 6));
        box.getStyleClass().add("info-badge");
        HBox.setHgrow(box, Priority.ALWAYS);

        titleLabel.getStyleClass().add("control-label");
        titleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
        titleLabel.setMinWidth(Region.USE_PREF_SIZE);

        valLabel.getStyleClass().add("value-label");
        valLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        HBox.setHgrow(valLabel, Priority.ALWAYS);

        box.getChildren().addAll(titleLabel, valLabel);
        return box;
    }

    public void updateData(List<H3Cell> cells) {
        this.currentCells = cells;
        VariableEntry entry = variableCombo.getValue();
        if (entry == null || entry.isHeader()) return;

        String varKey = entry.getKey();
        float[] values = pluggableStatEngine.extractVariableArray(varKey, cells, null);

        if (values == null || values.length == 0) {
            lblMean.setText("--");
            lblVariance.setText("--");
            lblStdDev.setText("--");
            lblGini.setText("--");
            lblMinMax.setText("-- / --");
            lblCv.setText("-- %");
            lblSpreadDesc.setText(I18n.getOrDefault("variance.desc.no_data", "Aucune donnée disponible pour ") + entry.getDisplayName());
            histogramSeries.getData().clear();
            return;
        }

        float[] aggs = statisticsKernel.calculateAggregates(values);
        float avg = aggs[0];
        float min = aggs[1];
        float max = aggs[2];
        float stdDev = aggs[3];
        float variance = stdDev * stdDev;
        float gini = statisticsKernel.calculateGini(values);
        double cv = (avg > 0) ? (stdDev / avg) * 100.0 : 0.0;

        lblMean.setText(formatAdaptive(avg));
        lblVariance.setText(formatAdaptive(variance));
        lblStdDev.setText(formatAdaptive(stdDev));
        lblGini.setText(String.format(java.util.Locale.US, "%.3f", gini));
        lblMinMax.setText(formatAdaptive(min) + " / " + formatAdaptive(max));
        lblCv.setText(String.format(java.util.Locale.US, "%.1f %%", cv));

        String spreadText;
        try {
            if (cv > 80.0 || gini > 0.5) {
                spreadText = String.format(I18n.getOrDefault("variance.desc.high", "🚨 Forte dispersion : Les individus s'éloignent très fortement du schéma standard (Écart-Type σ = %s, Gini = %.3f). Inégalité très prononcée."), formatAdaptive(stdDev), gini);
            } else if (cv > 35.0 || gini > 0.25) {
                spreadText = String.format(I18n.getOrDefault("variance.desc.moderate", "⚠️ Dispersion modérée : Écart significatif de la population par rapport au schéma standard (68%% des individus entre %s et %s)."), formatAdaptive(Math.max(0, avg - stdDev)), formatAdaptive(avg + stdDev));
            } else {
                spreadText = String.format(I18n.getOrDefault("variance.desc.low", "✅ Dispersion faible : Population homogène et très proche de la moyenne standard (μ = %s ± %s)."), formatAdaptive(avg), formatAdaptive(stdDev));
            }
        } catch (Exception e) {
            spreadText = String.format(java.util.Locale.ROOT, "μ = %s, σ = %s, Gini = %.3f", formatAdaptive(avg), formatAdaptive(stdDev), gini);
        }
        lblSpreadDesc.setText(spreadText);

        histogramSeries.getData().clear();
        int bins = 10;
        float binMin = (min < 0) ? min : 0.0f;
        float binMax = max;
        if (binMax <= binMin) {
            binMax = binMin + 1.0f;
        }

        int[] dist = statisticsKernel.calculateDistribution(values, bins, binMin, binMax);
        float binWidth = (binMax - binMin) / bins;

        for (int i = 0; i < bins; i++) {
            float rangeStart = binMin + i * binWidth;
            float rangeEnd = binMin + (i + 1) * binWidth;
            String binLabel = formatAdaptive(rangeStart) + "-" + formatAdaptive(rangeEnd);
            histogramSeries.getData().add(new XYChart.Data<>(binLabel, dist[i]));
        }
    }

    private String formatAdaptive(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return "--";
        double abs = Math.abs(val);
        if (abs >= 1_000_000.0 || (abs < 0.001 && abs > 0.0)) {
            return String.format(java.util.Locale.US, "%.2e", val);
        } else if (abs >= 100.0) {
            return String.format(java.util.Locale.US, "%,.1f", val);
        } else {
            return String.format(java.util.Locale.US, "%.2f", val);
        }
    }
}
