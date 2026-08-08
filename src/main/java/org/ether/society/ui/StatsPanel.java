/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.H3SimulationEngine;
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
            setStyle("-fx-background-color: rgba(30, 41, 59, 0.5); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.05); -fx-border-radius: 6;");

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

    public StatsPanel(H3SimulationEngine engine) {
        this.engine = engine;

        setPadding(new Insets(12));
        setSpacing(12);
        getStyleClass().add("glass-panel");
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.90); -fx-background-radius: 8; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-radius: 8;");

        // --- TOP TOOLBAR & CONTROLS ---
        Label headerTitle = new Label("📊 TABLEAU DE BORD DES STATISTIQUES & CLIODYNAMIQUE");
        headerTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ffd700;");

        // Category Filter
        categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.getItems().addAll(
            "Toutes les Catégories",
            "⚡ Énergie & Matière",
            "👥 Démographie & Santé",
            "🏛️ Société & Institutions",
            "💎 Économie & Richesse",
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
        searchField.setPromptText("🔍 Filtrer une statistique (ex: Kardashev, Gini, Effondrement, NPK)...");
        searchField.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-text-fill: white; -fx-prompt-text-fill: #64748b;");
        searchField.textProperty().addListener((obs, oldV, newV) -> filterMetrics());

        // Export Button
        Button exportBtn = new Button("📥 Exporter Données (CSV)");
        exportBtn.setTooltip(new Tooltip("Exporter l'historique complet des métriques sociétales, énergétiques et économiques au format CSV."));
        exportBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 6;");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> exportCsv());

        VBox topControlsBox = new VBox(6, headerTitle, categoryFilterCombo, searchField, exportBtn);
        topControlsBox.setStyle("-fx-padding: 10; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        // --- GRAPH SELECTION & TIME SERIES ---
        Label chartHeaderLabel = new Label("📈 EVOLUTION CHRONOLOGIQUE TEMPORELLE");
        chartHeaderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        chartMetricCombo = new ComboBox<>();
        chartMetricCombo.getItems().addAll(
            "Population Humaine",
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

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Année");
        xAxis.setTickLabelFill(Color.GRAY);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelFill(Color.GRAY);

        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Courbe d'Évolution Temporelle");
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);
        lineChart.setPrefHeight(160);
        lineChart.getData().add(chartSeries);

        // Bar Chart for Age Pyramid & Distribution
        CategoryAxis barX = new CategoryAxis();
        barX.setTickLabelFill(Color.GRAY);
        NumberAxis barY = new NumberAxis();
        barY.setTickLabelFill(Color.GRAY);
        barChart = new BarChart<>(barX, barY);
        barChart.setTitle("Pyramide des Âges & Répartition");
        barChart.setAnimated(false);
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(140);
        barChart.getData().add(barSeries);

        VBox chartBox = new VBox(6, chartHeaderLabel, chartMetricCombo, lineChart, barChart);
        chartBox.setStyle("-fx-padding: 8; -fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        // --- METRICS LIST CONTAINER ---
        metricsContainer = new VBox(6);

        registerMetricCards();
        buildMetricsList();

        getChildren().addAll(topControlsBox, chartBox, metricsContainer);
    }

    private void registerMetricCards() {
        // Category 1: Énergie & Matière
        addCard("energyCaptured", "Énergie Captée", "⚡ Énergie & Matière", "MW", "Flux d'énergie primaire solaire, géothermique et biomasse capté par les infrastructures.");
        addCard("resourceDepletion", "Déplétion des Ressources", "⚡ Énergie & Matière", "%", "Pourcentage de consommation cumulée des minerais et combustibles finis.");
        addCard("energyPerCapita", "Énergie / Individu", "⚡ Énergie & Matière", "MJ/hab", "Consommation d'énergie primaire moyenne par habitant.");
        addCard("foodPerCapita", "Nourriture / Individu", "⚡ Énergie & Matière", "mois/hab", "Réserve alimentaire et métabolique disponible par individu.");
        addCard("pibMaterialFlow", "PIB Flux de Matière", "⚡ Énergie & Matière", "Mt/an", "Volume de matière physique brute transformée et déplacée par an.");
        addCard("biomassNatural", "Biomasse Naturelle", "⚡ Énergie & Matière", "GtC", "Stock total de carbone végétal et faunique sauvage.");
        addCard("biomassDomesticated", "Biomasse Domestiquée", "⚡ Énergie & Matière", "GtC", "Biomasse totale des cultures agricoles et du bétail domestique.");
        addCard("potableWater", "Eau Douce & Aquifères", "⚡ Énergie & Matière", "10³ km³", "Réserves globales d'eau potable et nappe phréatique continentale.");
        addCard("remainingResources", "Ressources Restantes", "⚡ Énergie & Matière", "%", "Capital minéral et géologique non-extrait restant au sol.");
        addCard("entropyPollution", "Entropie & Pollution", "⚡ Énergie & Matière", "Idx", "Génération d'entropie thermodynamique et rejets polluants.");
        addCard("occupiedTerritory", "Territoire Occupé", "⚡ Énergie & Matière", "km²", "Surface géographique totale colonisée ou exploitée.");

        // Category 2: Démographie & Santé
        addCard("population", "Population Humaine", "👥 Démographie & Santé", "hab", "Population totale d'habitants sur la planète.");
        addCard("fertilityRate", "Taux de Fertilité", "👥 Démographie & Santé", "enf/femme", "Nombre moyen d'enfants par femme en âge de procréer.");
        addCard("offspringPct", "Taux avec Descendance", "👥 Démographie & Santé", "%", "Proportion d'adultes ayant au moins un descendant.");
        addCard("ageFirstChild", "Âge au 1er Enfant", "👥 Démographie & Santé", "ans", "Âge moyen de la mère à la naissance du premier enfant.");
        addCard("immigrationRate", "Taux d'Immigration", "👥 Démographie & Santé", "‰", "Flux migratoires nets inter-régionaux.");
        addCard("lifeExpectancy", "Espérance de Vie", "👥 Démographie & Santé", "ans", "Espérance de vie moyenne à la naissance.");
        addCard("healthIndex", "Niveau de Santé Global", "👥 Démographie & Santé", "%", "Indice global de résistance sanitaire et d'immunité.");
        addCard("educationLevel", "Niveau d'Éducation", "👥 Démographie & Santé", "%", "Taux d'instruction et capital de savoir accumulé.");

        // Category 3: Société & Institutions
        addCard("happinessIndex", "Indice de Bonheur", "🏛️ Société & Institutions", "%", "Niveau de satisfaction globale et de bien-être mesuré.");
        addCard("conflictLevel", "Taux de Conflits", "🏛️ Société & Institutions", "%", "Intensité des frictions sociales, guerres et violence.");
        addCard("cityStates", "Nombre de Cités-États", "🏛️ Société & Institutions", "cités", "Pôles autonomes d'administration et institutions urbaines.");
        addCard("institutionalMaturity", "Naissance des Institutions", "🏛️ Société & Institutions", "Idx", "Maturité juridique, administrative et étatiste.");
        addCard("divisionLabor", "Division du Travail", "🏛️ Société & Institutions", "Idx", "Niveau de spécialisation des métiers et de différenciation sociale.");
        addCard("maxHierarchy", "Niveau Max Hiérarchique", "🏛️ Société & Institutions", "Niv", "Niveau d'empilement institutionnel (Tribu 1 ➔ Empire/Réseau 6).");
        addCard("largestCulture", "Plus Grande Unité Culturelle", "🏛️ Société & Institutions", "hab", "Taille de la plus vaste confédération culturelle/politique.");
        addCard("kardashevScale", "Échelle de Kardashev", "🏛️ Société & Institutions", "Type K", "Niveau de maîtrise énergétique globale (Type 0.0 à 1.0+).");

        // Category 4: Économie & Richesse
        addCard("giniIndex", "Indice de Gini (Inégalité)", "💎 Économie & Richesse", "Coeff", "Mesure de concentration des richesses (0 = égalité, 1 = inégalité absolue).");
        addCard("gdpTotal", "PIB Global (GDP)", "💎 Économie & Richesse", "G$", "Produit Intérieur Brut total converti en monnaie constante.");
        addCard("builtCapital", "Capital Bâti & Outillage", "💎 Économie & Richesse", "kg/hab", "Stock total d'infrastructures physiques et de machines.");
        addCard("eliteFormation", "Formation d'Élite", "💎 Économie & Richesse", "%", "Proportion de la population détenant les fonctions de commandement.");
        addCard("elderCapitalShare", "Possession Capital (Aînés)", "💎 Économie & Richesse", "%", "Part de la richesse foncière détenue par la tranche d'âge senior.");
        addCard("landRent", "Rente Foncière & Immobilière", "💎 Économie & Richesse", "Idx", "Valorisation de la rente du sol liée à la densité et aux infrastructures.");
        addCard("toolsCount", "Nombre d'Outils en Service", "💎 Économie & Richesse", "unités", "Quantité totale d'outils et équipements de production.");
        addCard("productsCount", "Variété de Produits", "💎 Économie & Richesse", "types", "Diversité des produits manufacturés au catalogue technique.");

        // Category 5: Cognition & Information
        addCard("shannonBandwidth", "Bande Passante Shannon", "🧠 Cognition & Information", "Gbps", "Débit maximal de transmission d'information à travers le réseau civilisationnel.");
        addCard("collectiveMemory", "Stock Mémoire Collective", "🧠 Cognition & Information", "TB", "Volume cumulé des connaissances, données et patrimoines écrits.");
        addCard("innovationDiffusion", "Vitesse de Diffusion Tech", "🧠 Cognition & Information", "km/an", "Vitesse de propagation spatiale des nouvelles technologies.");
        addCard("knowledgeDecay", "Taux d'Amnésie Historique", "🧠 Cognition & Information", "%/décade", "Vitesse de déperdition ou d'oubli du savoir lors des crises.");

        // Category 6: Écologie & Frontières Planétaires
        addCard("soilNPK", "Qualité NPK des Sols", "🌍 Écologie & Frontières Planétaires", "%", "Indice de fertilité et teneur en nutriments organiques des sols cultivés.");
        addCard("carbonFootprint", "Empreinte Carbone", "🌍 Écologie & Frontières Planétaires", "GtCO₂", "Émissions annuelles de gaz à effet de serre et carbone fossile.");
        addCard("wildBiodiversity", "Biodiversité Sauvage", "🌍 Écologie & Frontières Planétaires", "%", "Part de la biomasse faunique et florale sauvage préservée.");
        addCard("wetBulbSafety", "Marge Sécurité Bulbe Humide", "🌍 Écologie & Frontières Planétaires", "°C", "Écart de température avec le seuil létal de bulbe humide (35°C).");

        // Category 7: Cliodynamique & Risques Systémiques
        addCard("eliteOverproduction", "Surproduction Élitaire (Turchin)", "⏳ Cliodynamique & Risques Systémiques", "Idx", "Ratio de compétition pour les postes de pouvoir (Indice PSI de Turchin).");
        addCard("fiscalStress", "Pression & Stress Fiscal", "⏳ Cliodynamique & Risques Systémiques", "%", "Stress financier et charge de maintien des institutions publiques.");
        addCard("geopoliticalTension", "Tension Géopolitique", "⏳ Cliodynamique & Risques Systémiques", "%", "Friction diplomatique et risque d'escalade guerrière multipolaire.");
        addCard("collapseVulnerability", "Risque d'Effondrement", "⏳ Cliodynamique & Risques Systémiques", "%", "Probabilité d'effondrement systémique ou de boucle d'entropie.");

        // Category 8: Complexité Systémique
        addCard("systemComplexity", "Complexité Systémique", "⚙️ Complexité Systémique", "Idx", "Indice d'interconnexion des rouages économiques et sociaux.");
        addCard("reconstructionCapability", "Capacité à Reconstruire", "⚙️ Complexité Systémique", "%", "Résilience et capacité à rebâtir la civilisation à partir de zéro.");
        addCard("systemInterdependence", "Interdépendance (Rouages)", "⚙️ Complexité Systémique", "%", "Fragilité systémique liée à l'interdépendance des chaînes logistiques.");

        // Category 9: Performances Engine
        addCard("engineTPS", "TPS (Images/s)", "💻 Performances Techniques", "TPS", "Taux de rafraîchissement des cycles de simulation par seconde.");
        addCard("ramMemory", "Utilisation Mémoire RAM", "💻 Performances Techniques", "MB", "Consommation mémoire vive du moteur.");
        addCard("cellCount", "Cellules Hexagonales H3", "💻 Performances Techniques", "hex", "Nombre total de mailles hexagonales chargées en mémoire.");
    }

    private void addCard(String key, String title, String category, String unit, String tooltip) {
        metricCards.put(key, new MetricCard(key, title, category, unit, tooltip));
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

    private void resetChartSeries() {
        chartSeries.getData().clear();
        update();
    }

    public void update() {
        if (engine == null) return;

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

            setCardVal("happinessIndex", String.format("%.1f", happiness), happiness / 100.0);
            setCardVal("conflictLevel", String.format("%.1f", conflict), conflict / 100.0);
            setCardVal("cityStates", String.format("%d", cityStates), cityStates / 50.0);
            setCardVal("institutionalMaturity", String.format("%.1f", instMaturity), instMaturity / 100.0);
            setCardVal("divisionLabor", String.format("%.1f", divLabor), divLabor / 100.0);
            setCardVal("maxHierarchy", String.format("Niv %d", maxHier), maxHier / 6.0);
            setCardVal("largestCulture", String.format("%,d", largestCult), largestCult / Math.max(1.0, (double)pop));
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
                case "Échelle de Kardashev (Type K)" -> kardashev;
                case "Énergie Captée (MW)" -> energyCap;
                case "Indice de Gini (Inégalités)" -> gini;
                case "Indice de Bonheur (%)" -> happiness;
                case "Taux de Conflits (%)" -> conflict;
                case "PIB Global (GDP)" -> gdp;
                case "Espérance de Vie (ans)" -> life;
                case "Division du Travail & Spécialisation" -> divLabor;
                case "Matière Déplacée & Capital Bâti" -> builtCap / 1000.0;
                case "Risque d'Effondrement Systémique (%)" -> collapseVuln;
                case "Pression Élitaire (Indice Turchin)" -> eliteOver;
                case "Empreinte Carbone (GtCO2)" -> carbonFp;
                case "Stock de Mémoire Collective (TB)" -> memoryStock;
                case "Interdépendance & Complexité Systémique" -> sysComp;
                default -> pop;
            };

            if (chartSeries.getData().isEmpty() || chartSeries.getData().get(chartSeries.getData().size() - 1).getXValue().intValue() != year) {
                chartSeries.getData().add(new XYChart.Data<>(year, yVal));
                if (chartSeries.getData().size() > 60) {
                    chartSeries.getData().remove(0);
                }

                // Update Age Pyramid Bar Chart
                int[] pyramid = engine.getAgePyramid();
                barSeries.getData().clear();
                barSeries.getData().add(new XYChart.Data<>("Jeunes (<15ans)", pyramid[0]));
                barSeries.getData().add(new XYChart.Data<>("Adultes (15-60ans)", pyramid[1]));
                barSeries.getData().add(new XYChart.Data<>("Aînés (>60ans)", pyramid[2]));
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
        fileChooser.setTitle("Exporter les Statistiques Cliodynamiques (CSV / Excel)");
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
            alert.setTitle("Exportation Réussie");
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
