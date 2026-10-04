/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.ether.society.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Interactive Dialog & Heatmap Inspector for Cliodynamic Cultural Affinity & Distance Matrices.
 * Displays discrete 24-bit RGB Entity traits, technocomplexes, and pairwise cultural affinities (N x N).
 * Supports in-place symmetric cell editing, procedural auto-generation, and full 5-language localization.
 *
 * @author Silvere Martin-Michiellot
 */
public class CulturalAffinityMatrixDialog extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(CulturalAffinityMatrixDialog.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record CulturalEntity(
            String id,
            String colorHex,
            int[] colorRgb,
            double[] traits,
            String nameEn,
            String nameFr,
            String nameDe,
            String nameEs,
            String nameZh,
            String kinshipType,
            String lithicTechnocomplex
    ) {
        /*
         * Get localized name.
         * Enforces physical invariants and updates associated state variables within {@code CulturalAffinityMatrixDialog}.
         *
         * @return the resulting computation or state reference
         */
        public String getLocalizedName() {
            String lang = I18n.getCurrentLanguage() != null ? I18n.getCurrentLanguage().getCode() : "en";
            if ("fr".equalsIgnoreCase(lang)) return nameFr != null ? nameFr : nameEn;
            if ("de".equalsIgnoreCase(lang)) return nameDe != null ? nameDe : nameEn;
            if ("es".equalsIgnoreCase(lang)) return nameEs != null ? nameEs : nameEn;
            if ("zh".equalsIgnoreCase(lang)) return nameZh != null ? nameZh : nameEn;
            return nameEn != null ? nameEn : id;
        }

        /*
         * Distance to.
         * Enforces physical invariants and updates associated state variables within {@code CulturalAffinityMatrixDialog}.
         *
         * @param other the other parameter (CulturalEntity)
         * @return the resulting computation or state reference
         */
        public double distanceTo(CulturalEntity other) {
            if (other == null || this.traits == null || other.traits == null) return 1.0;
            double sumSq = 0.0;
            int n = Math.min(this.traits.length, other.traits.length);
            for (int i = 0; i < n; i++) {
                double diff = this.traits[i] - other.traits[i];
                sumSq += diff * diff;
            }
            return Math.sqrt(sumSq);
        }

        /*
         * Affinity with.
         * Enforces physical invariants and updates associated state variables within {@code CulturalAffinityMatrixDialog}.
         *
         * @param other the other parameter (CulturalEntity)
         * @return the resulting computation or state reference
         */
        public double affinityWith(CulturalEntity other) {
            if (other != null && this.id.equals(other.id)) return 1.0;
            double dist = distanceTo(other);
            return Math.exp(-3.5 * dist);
        }
    }

    private final ComboBox<Long> epochSelector = new ComboBox<>();
    private final Label titleLabel = new Label();
    private final Label subtitleLabel = new Label();
    private final Label scenarioEpochBadge = new Label();
    private final VBox entitiesBox = new VBox(8);
    private final GridPane matrixGrid = new GridPane();
    private final ScrollPane matrixScroll = new ScrollPane(matrixGrid);

    // Procedural Generation Toolbar
    private final Button btnGenGeo = new Button();
    private final Button btnGenTraits = new Button();
    private final Button btnGenStochastic = new Button();
    private final Button btnResetDefaults = new Button();

    // Quick-Editor Panel at Bottom of Matrix Tab
    private final VBox quickEditorCard = new VBox(8);
    private final Label quickEditorTitle = new Label();
    private final Label quickEditorPairLabel = new Label();
    private final Slider quickAffinitySlider = new Slider(0.0, 100.0, 50.0);
    private final Spinner<Double> quickAffinitySpinner = new Spinner<>(0.0, 100.0, 50.0, 1.0);
    private final HBox presetChipsBox = new HBox(6);

    // Footer Actions
    private final Button btnExportCsv = new Button();
    private final Button btnExportJson = new Button();
    private final Button btnClose = new Button();

    private List<CulturalEntity> activeEntities = new ArrayList<>();
    private final java.util.Map<String, Double> customAffinityOverrides = new java.util.HashMap<>();
    private final StackPane[][] cellPanes = new StackPane[32][32];
    private final Label[][] cellLabels = new Label[32][32];

    /* Internal state variable for current epoch (long). */
    private long currentEpoch = -100000L;
    /* Internal state variable for scenario start year (long). */
    private final long scenarioStartYear;
    /* Internal state variable for selected i (int). */
    private int selectedI = -1;
    /* Internal state variable for selected j (int). */
    private int selectedJ = -1;
    /* Internal state variable for is updating editor (boolean). */
    private boolean isUpdatingEditor = false;

    /*
     * Cultural affinity matrix dialog.
     * Enforces physical invariants and updates associated state variables within {@code CulturalAffinityMatrixDialog}.
     *
     * @param initialEpoch the initial epoch parameter (long)
     */
    public CulturalAffinityMatrixDialog(long initialEpoch) {
        this.scenarioStartYear = initialEpoch;
        this.currentEpoch = initialEpoch;
        initModality(Modality.APPLICATION_MODAL);
        setTitle(I18n.getOrDefault("cultural.matrix.window_title", "🧩 Matrice d'Affinité & Distances Culturelles"));

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("glass-panel");

        // 1. Header Section
        titleLabel.setText(I18n.getOrDefault("cultural.matrix.header_title", "🏛️ Matrice d'Affinité Culturelle Cliodynamique (N × N)"));
        titleLabel.getStyleClass().add("panel-header");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        subtitleLabel.setText(I18n.getOrDefault("cultural.matrix.header_desc", "Inspecter et éditer la matrice N×N des distances de Mahalanobis, continuités linguistiques et coefficients d'assimilation."));
        subtitleLabel.getStyleClass().add("card-description-muted");
        subtitleLabel.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #94a3b8;");

        // Scenario Target Badge + Epoch Switcher
        HBox epochRow = new HBox(12);
        epochRow.setAlignment(Pos.CENTER_LEFT);

        scenarioEpochBadge.setText(String.format(I18n.getOrDefault("cultural.matrix.current_scenario_epoch", "🎯 Époque du Scénario : %s"), formatEpochName(initialEpoch)));
        scenarioEpochBadge.setStyle("-fx-font-weight: bold; -fx-font-size: 11.5px; -fx-text-fill: #f59e0b; -fx-background-color: rgba(245, 158, 11, 0.12); -fx-padding: 4 10; -fx-background-radius: 6; -fx-border-color: rgba(245, 158, 11, 0.35); -fx-border-radius: 6;");

        Label lblEpoch = new Label(I18n.getOrDefault("cultural.matrix.select_epoch", "Époque Historique :"));
        lblEpoch.getStyleClass().add("control-label");
        lblEpoch.setStyle("-fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

        epochSelector.getItems().addAll(-100000L, -50000L, -25000L, -20000L, -10900L, -10000L, -8000L, -6000L, -3000L, -1900L, -1000L, 0L, 1000L, 2026L);
        epochSelector.setValue(closestSupportedEpoch(initialEpoch));
        epochSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            /*
             * Update item.
             * Enforces physical invariants and updates associated state variables within {@code CulturalAffinityMatrixDialog}.
             *
             * @param item the item parameter (Long)
             * @param empty the empty parameter (boolean)
             */
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(formatEpochName(item));
                }
            }
        });
        epochSelector.setButtonCell(epochSelector.getCellFactory().call(null));
        epochSelector.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.epoch", "Sélectionner l'époque historique pour inspecter les entités ethnolinguistiques et matrices d'affinité.")));
        epochSelector.setOnAction(e -> {
            Long val = epochSelector.getValue();
            if (val != null) {
                loadEpochRegistry(val);
            }
        });
        epochRow.getChildren().addAll(scenarioEpochBadge, new Region(), lblEpoch, epochSelector);
        HBox.setHgrow(epochRow.getChildren().get(1), Priority.ALWAYS);

        // 2. TabPane: Tab 1 = Affinity Heatmap Matrix, Tab 2 = Cultural Entities Registry
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tab 1 Layout: Procedural Toolbar + Matrix Grid + Quick-Editor Bar
        VBox matrixTabRoot = new VBox(10);
        matrixTabRoot.setPadding(new Insets(8));

        // Procedural Toolbar
        HBox proceduralToolbar = new HBox(8);
        proceduralToolbar.setAlignment(Pos.CENTER_LEFT);
        proceduralToolbar.setPadding(new Insets(6, 10, 6, 10));
        proceduralToolbar.setStyle("-fx-background-color: rgba(15, 23, 42, 0.4); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

        Label procLabel = new Label(I18n.getOrDefault("cultural.matrix.procedural_header", "🪄 Génération Procédurale & Heuristiques :"));
        procLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        btnGenGeo.setText(I18n.getOrDefault("cultural.matrix.btn_gen_geo", "🌐 Distance Géo & Isoglosses"));
        btnGenGeo.getStyleClass().add("button-secondary");
        btnGenGeo.setStyle("-fx-font-size: 10.5px;");
        btnGenGeo.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.btn_gen_geo_tt", "Calculer les affinités par décroissance exponentielle de distance spatiale et linguistique.")));
        btnGenGeo.setOnAction(e -> generateAffinitiesByGeography());

        btnGenTraits.setText(I18n.getOrDefault("cultural.matrix.btn_gen_traits", "🧬 Similarité des Traits (Mahalanobis)"));
        btnGenTraits.getStyleClass().add("button-secondary");
        btnGenTraits.setStyle("-fx-font-size: 10.5px;");
        btnGenTraits.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.btn_gen_traits_tt", "Calculer les affinités basées sur la distance euclidienne des traits ethnographiques multidimensionnels.")));
        btnGenTraits.setOnAction(e -> generateAffinitiesByTraits());

        btnGenStochastic.setText(I18n.getOrDefault("cultural.matrix.btn_gen_stochastic", "🎲 Bruit Stochastique"));
        btnGenStochastic.getStyleClass().add("button-secondary");
        btnGenStochastic.setStyle("-fx-font-size: 10.5px;");
        btnGenStochastic.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.btn_gen_stochastic_tt", "Appliquer une variation aléatoire réaliste gaussienne symétrique.")));
        btnGenStochastic.setOnAction(e -> generateAffinitiesStochastic());

        btnResetDefaults.setText(I18n.getOrDefault("cultural.matrix.btn_reset_defaults", "🔄 Défauts"));
        btnResetDefaults.getStyleClass().add("button-secondary");
        btnResetDefaults.setStyle("-fx-font-size: 10.5px;");
        btnResetDefaults.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.btn_reset_defaults_tt", "Restaurer les affinités historiques canoniques de cette époque.")));
        btnResetDefaults.setOnAction(e -> {
            customAffinityOverrides.clear();
            renderMatrixHeatmap();
            updateQuickEditor();
        });

        proceduralToolbar.getChildren().addAll(procLabel, btnGenGeo, btnGenTraits, btnGenStochastic, btnResetDefaults);

        // Matrix Grid inside ScrollPane
        matrixGrid.setHgap(4);
        matrixGrid.setVgap(4);
        matrixGrid.setPadding(new Insets(8));
        matrixScroll.setFitToWidth(true);
        matrixScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(matrixScroll, Priority.ALWAYS);

        // Quick Editor Panel setup
        setupQuickEditorPanel();

        matrixTabRoot.getChildren().addAll(proceduralToolbar, matrixScroll, quickEditorCard);

        Tab tabMatrix = new Tab(I18n.getOrDefault("cultural.matrix.tab_heatmap", "📊 Carte Thermique d'Affinité (N × N)"), matrixTabRoot);
        tabMatrix.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.tab_heatmap", "Matrice N×N des coefficients d'affinité symétriques éditables en direct.")));

        // Tab 2: Entities List
        ScrollPane entitiesScroll = new ScrollPane(entitiesBox);
        entitiesScroll.setFitToWidth(true);
        entitiesScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        entitiesBox.setPadding(new Insets(10));
        Tab tabEntities = new Tab(I18n.getOrDefault("cultural.matrix.tab_entities", "📜 Registre Culturel & Technocomplexes"), entitiesScroll);
        tabEntities.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.tab_entities", "Entités ethnolinguistiques discrètes avec codage couleur 24-bit, régimes de parenté et technocomplexes lithiques.")));

        tabPane.getTabs().addAll(tabMatrix, tabEntities);

        // 3. Footer Actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        btnExportCsv.setText(I18n.getOrDefault("cultural.matrix.btn_export_csv", "💾 Exporter Matrice (CSV)"));
        btnExportCsv.getStyleClass().add("button-secondary");
        btnExportCsv.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.export_csv", "Exporter la matrice complète d'affinité culturelle N×N sous format CSV standard.")));
        btnExportCsv.setOnAction(e -> exportMatrixCsv());

        btnExportJson.setText(I18n.getOrDefault("cultural.matrix.btn_export_json", "📄 Exporter Matrice (JSON)"));
        btnExportJson.getStyleClass().add("button-secondary");
        btnExportJson.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.export_json", "Exporter le registre culturel complet et la matrice d'affinité sous format JSON.")));
        btnExportJson.setOnAction(e -> exportMatrixJson());

        btnClose.setText(I18n.getOrDefault("common.btn.close", "Fermer"));
        btnClose.getStyleClass().add("button-primary");
        btnClose.setStyle("-fx-font-weight: bold;");
        btnClose.setTooltip(new Tooltip(I18n.getOrDefault("cultural.matrix.tooltip.close", "Fermer l'inspecteur des affinités culturelles.")));
        btnClose.setOnAction(e -> close());

        footer.getChildren().addAll(btnExportCsv, btnExportJson, btnClose);

        root.getChildren().addAll(titleLabel, subtitleLabel, epochRow, tabPane, footer);

        Scene scene = new Scene(root, 960, 720);
        setScene(scene);
        Theme.applyCurrentTheme(scene);
        Theme.themeProperty().addListener((obs, oldV, newV) -> Theme.applyCurrentTheme(scene));
        WindowUtils.applyWindowIcon(this);

        loadEpochRegistry(epochSelector.getValue());
    }

    private void setupQuickEditorPanel() {
        quickEditorCard.setPadding(new Insets(8, 12, 8, 12));
        quickEditorCard.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-background-radius: 8; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 8;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        quickEditorTitle.setText(I18n.getOrDefault("cultural.matrix.quick_editor_title", "✏️ Éditeur d'Affinité Symétrique Directe :"));
        quickEditorTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        quickEditorPairLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11.5px; -fx-text-fill: #f8fafc;");
        topRow.getChildren().addAll(quickEditorTitle, quickEditorPairLabel);

        HBox controlsRow = new HBox(12);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        quickAffinitySlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(quickAffinitySlider, Priority.ALWAYS);
        quickAffinitySlider.setShowTickMarks(true);
        quickAffinitySlider.setMajorTickUnit(25);
        quickAffinitySlider.setBlockIncrement(5);

        quickAffinitySpinner.setEditable(true);
        quickAffinitySpinner.setPrefWidth(90);

        quickAffinitySlider.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isUpdatingEditor && newV != null && selectedI >= 0 && selectedJ >= 0) {
                isUpdatingEditor = true;
                quickAffinitySpinner.getValueFactory().setValue(Math.round(newV.doubleValue() * 10.0) / 10.0);
                applyQuickEdit(newV.doubleValue() / 100.0);
                isUpdatingEditor = false;
            }
        });

        quickAffinitySpinner.valueProperty().addListener((obs, oldV, newV) -> {
            if (!isUpdatingEditor && newV != null && selectedI >= 0 && selectedJ >= 0) {
                isUpdatingEditor = true;
                quickAffinitySlider.setValue(newV);
                applyQuickEdit(newV / 100.0);
                isUpdatingEditor = false;
            }
        });

        presetChipsBox.setAlignment(Pos.CENTER_LEFT);
        addPresetChip(I18n.getOrDefault("cultural.matrix.preset_hostile", "⚔️ 0%"), 0.0);
        addPresetChip(I18n.getOrDefault("cultural.matrix.preset_refractory", "🛡️ 25%"), 25.0);
        addPresetChip(I18n.getOrDefault("cultural.matrix.preset_peaceful", "🤝 50%"), 50.0);
        addPresetChip(I18n.getOrDefault("cultural.matrix.preset_allied", "🏛️ 75%"), 75.0);
        addPresetChip(I18n.getOrDefault("cultural.matrix.preset_assimilated", "💍 100%"), 100.0);

        controlsRow.getChildren().addAll(new Label("0%"), quickAffinitySlider, new Label("100%"), quickAffinitySpinner, presetChipsBox);
        quickEditorCard.getChildren().addAll(topRow, controlsRow);
    }

    private void addPresetChip(String label, double val) {
        Button btn = new Button(label);
        btn.getStyleClass().add("button-secondary");
        btn.setStyle("-fx-font-size: 10px; -fx-padding: 3 7;");
        btn.setOnAction(e -> {
            if (selectedI >= 0 && selectedJ >= 0) {
                isUpdatingEditor = true;
                quickAffinitySlider.setValue(val);
                quickAffinitySpinner.getValueFactory().setValue(val);
                applyQuickEdit(val / 100.0);
                isUpdatingEditor = false;
            }
        });
        presetChipsBox.getChildren().add(btn);
    }

    private void applyQuickEdit(double affinity) {
        if (selectedI < 0 || selectedJ < 0 || selectedI >= activeEntities.size() || selectedJ >= activeEntities.size()) return;
        CulturalEntity e1 = activeEntities.get(selectedI);
        CulturalEntity e2 = activeEntities.get(selectedJ);

        if (selectedI == selectedJ) {
            affinity = 1.0;
        }

        double finalAff = Math.clamp(affinity, 0.0, 1.0);
        String k1 = e1.id() + ":" + e2.id();
        String k2 = e2.id() + ":" + e1.id();
        customAffinityOverrides.put(k1, finalAff);
        customAffinityOverrides.put(k2, finalAff);

        updateCellDisplay(selectedI, selectedJ);
        updateCellDisplay(selectedJ, selectedI);
    }

    private void updateCellDisplay(int i, int j) {
        if (i >= cellPanes.length || j >= cellPanes[0].length) return;
        StackPane cell = cellPanes[i][j];
        Label lbl = cellLabels[i][j];
        if (cell == null || lbl == null) return;

        CulturalEntity rowEnt = activeEntities.get(i);
        CulturalEntity colEnt = activeEntities.get(j);
        double aff = getAffinity(rowEnt, colEnt);
        double dist = rowEnt.distanceTo(colEnt);

        Color bg = computeHeatmapColor(aff);
        boolean isSel = (i == selectedI && j == selectedJ) || (i == selectedJ && j == selectedI);

        String borderStyle = isSel
                ? "-fx-border-color: #38bdf8; -fx-border-width: 2px; -fx-border-radius: 4;"
                : (i == j ? "-fx-border-color: #64748b; -fx-border-width: 1px; -fx-border-radius: 4;" : "-fx-border-color: #334155; -fx-border-width: 1px; -fx-border-radius: 4;");

        cell.setStyle(String.format("-fx-background-color: #%02X%02X%02X; -fx-background-radius: 4; %s",
                (int) (bg.getRed() * 255), (int) (bg.getGreen() * 255), (int) (bg.getBlue() * 255), borderStyle));

        lbl.setText(String.format("%.1f%%", aff * 100.0));
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ffffff;");

        String tooltipText = String.format(
                "🔍 %s ↔ %s\n" +
                "• %s : %.1f%%\n" +
                "• %s : %.3f\n" +
                "• %s : %.2f\n" +
                "• %s : %.2f\n\n" +
                "👉 %s",
                rowEnt.getLocalizedName(), colEnt.getLocalizedName(),
                I18n.getOrDefault("cultural.tooltip.affinity", "Affinité Culturelle"), aff * 100.0,
                I18n.getOrDefault("cultural.tooltip.distance", "Distance Euclidienne"), dist,
                I18n.getOrDefault("cultural.tooltip.trade_flow", "Facteur Contagion Commerciale"), aff * 0.85,
                I18n.getOrDefault("cultural.tooltip.intermarriage", "Probabilité d'Intermariage"), Math.pow(aff, 1.5),
                (i == j ? I18n.getOrDefault("cultural.matrix.diagonal_locked", "🔒 Auto-identité (100% invariant)") : I18n.getOrDefault("cultural.tooltip.click_edit", "Cliquez pour modifier directement dans l'éditeur rapide."))
        );
        Tooltip.install(cell, new Tooltip(tooltipText));
    }

    private static long closestSupportedEpoch(long yr) {
        long[] supported = {-100000L, -50000L, -25000L, -20000L, -10900L, -10000L, -8000L, -6000L, -3000L, -1900L, -1000L, 0L, 1000L, 2026L};
        long best = supported[0];
        long minDiff = Math.abs(yr - best);
        for (long s : supported) {
            long d = Math.abs(yr - s);
            if (d < minDiff) {
                minDiff = d;
                best = s;
            }
        }
        return best;
    }

    private static String formatEpochName(long yr) {
        if (yr == -100000L) return I18n.getOrDefault("cultural.epoch.eemian", "–100 000 BP (Sortie d'Afrique / Éémien)");
        if (yr == -50000L)  return I18n.getOrDefault("cultural.epoch.sahul", "–50 000 BP (Peuplement Maritime du Sahul)");
        if (yr == -25000L)  return I18n.getOrDefault("cultural.epoch.beringia", "–25 000 BP (Pause Béringienne & Gravettien)");
        if (yr == -20000L)  return I18n.getOrDefault("cultural.epoch.lgm", "–20 000 BP (Dernier Maximum Glaciaire & Solutréen)");
        if (yr == -8000L)   return I18n.getOrDefault("cultural.epoch.neolithic", "–8 000 BP (Néolithique / Çatalhöyük & Jéricho)");
        if (yr == -1900L)   return I18n.getOrDefault("cultural.epoch.bronze", "–1 900 AEC (Âge du Bronze Moyen / Babylone & Shang)");
        if (yr == -1000L)   return I18n.getOrDefault("cultural.epoch.iron", "–1 000 AEC (Premier Âge du Fer / Phéniciens & Zhou)");
        if (yr == 0L)       return "0 CE / AD (Pax Romana & Dynastie Han)";
        if (yr == 1000L)    return "+1 000 CE (Moyen Âge / Song & Califats)";
        if (yr == 2026L)    return "+2 026 CE (Anthropocène Contemporain)";
        return (yr < 0 ? Math.abs(yr) + " BP / AEC" : yr + " CE / AD");
    }

    private void loadEpochRegistry(long epoch) {
        this.currentEpoch = epoch;
        activeEntities.clear();
        customAffinityOverrides.clear();
        selectedI = -1;
        selectedJ = -1;

        Path p = Paths.get("data", "maps", "ether", "earth", String.valueOf(epoch), "cultural_registry.json");
        if (!Files.exists(p)) {
            long closest = closestSupportedEpoch(epoch);
            p = Paths.get("data", "maps", "ether", "earth", String.valueOf(closest), "cultural_registry.json");
        }

        if (Files.exists(p)) {
            try {
                JsonNode root = MAPPER.readTree(p.toFile());
                JsonNode entitiesNode = root.get("entities");
                if (entitiesNode != null && entitiesNode.isArray()) {
                    for (JsonNode eNode : entitiesNode) {
                        String id = eNode.path("id").asText("unknown");
                        String colorHex = eNode.path("colorHex").asText("#FFFFFF");
                        int[] colorRgb = new int[]{255, 255, 255};
                        if (eNode.has("colorRgb") && eNode.get("colorRgb").isArray()) {
                            colorRgb[0] = eNode.get("colorRgb").get(0).asInt(255);
                            colorRgb[1] = eNode.get("colorRgb").get(1).asInt(255);
                            colorRgb[2] = eNode.get("colorRgb").get(2).asInt(255);
                        }
                        double[] traits = new double[4];
                        if (eNode.has("traits") && eNode.get("traits").isArray()) {
                            for (int i = 0; i < 4 && i < eNode.get("traits").size(); i++) {
                                traits[i] = eNode.get("traits").get(i).asDouble(0.0);
                            }
                        }
                        JsonNode nameNode = eNode.path("name");
                        String nameEn = nameNode.path("en").asText(id);
                        String nameFr = nameNode.path("fr").asText(nameEn);
                        String nameDe = nameNode.path("de").asText(nameEn);
                        String nameEs = nameNode.path("es").asText(nameEn);
                        String nameZh = nameNode.path("zh").asText(nameEn);
                        String kinship = eNode.path("kinshipType").asText("Bilateral Foragers");
                        String lithic = eNode.path("lithicTechnocomplex").asText("Middle Paleolithic");

                        activeEntities.add(new CulturalEntity(id, colorHex, colorRgb, traits, nameEn, nameFr, nameDe, nameEs, nameZh, kinship, lithic));
                    }
                }
            } catch (Exception ex) {
                logger.warn("Failed to parse cultural_registry.json for epoch {}: {}", epoch, ex.getMessage());
            }
        }

        if (activeEntities.isEmpty()) {
            buildProceduralEntitiesFallback(epoch);
        }

        renderEntitiesTab();
        renderMatrixHeatmap();

        if (!activeEntities.isEmpty()) {
            selectCell(0, 1 < activeEntities.size() ? 1 : 0);
        } else {
            updateQuickEditor();
        }
    }

    private void buildProceduralEntitiesFallback(long epoch) {
        activeEntities.clear();
        String[] colors = {"#E67E22", "#D35400", "#F39C12", "#2980B9", "#1F4788", "#27AE60", "#8E44AD", "#E11D48", "#0EA5E9"};
        int[][] rgb = {
            {230, 126, 34}, {211, 84, 0}, {243, 156, 18}, {41, 128, 185}, {31, 71, 136}, {39, 174, 96}, {142, 68, 173}, {225, 29, 72}, {14, 165, 233}
        };
        String[] ids = {"clade_alpha", "clade_beta", "clade_gamma", "clade_delta", "clade_epsilon", "clade_zeta", "clade_eta", "clade_theta", "clade_iota"};
        String[] namesEn = {"Equatorial Basin Clade", "Rift & Valley Lineage", "Maritime Coastal Foragers", "Highland Mountain Tribe", "Savanna Nomad Confederacy", "Lacustrine Forest Dwellers", "Arid Steppe Clan", "Alluvial Delta Society", "Tundra Boreal Band"};
        String[] namesFr = {"Clade du Bassin Équatorial", "Lignée du Rift & Vallées", "Chasseurs-Cueilleurs Côtiers", "Tribu des Hauts Plateaux", "Confédération Nomade des Savanes", "Société Lacustre des Forêts", "Clan des Steppes Arides", "Société des Deltas Alluviaux", "Bande Boréale de Toundra"};
        String[] namesDe = {"Äquatoriales Becken-Klade", "Grabenbruch-Linie", "Küstensammler-Gemeinschaft", "Hochland-Bergstamm", "Savannen-Nomadenbund", "See- und Waldbewohner", "Arider Steppen-Klan", "Alluviale Delta-Gemeinschaft", "Boreale Tundragruppe"};
        String[] namesEs = {"Clado de la Cuenca Ecuatorial", "Linaje del Rift y Valles", "Recolectores Costeros Marítimos", "Tribu de las Altas Montañas", "Confederación Nómada de la Sabana", "Habitantes Lacustres del Bosque", "Clan de la Estepa Árida", "Sociedad del Delta Aluvial", "Banda Boreal de Tundra"};
        String[] namesZh = {"赤道盆地演化支", "大裂谷与山谷世系", "海洋沿海采集者", "高山山地部落", "稀树草原游牧联盟", "湖泊森林居民", "干旱草原部落", "冲积三角洲社会", "苔原北方群体"};
        double[][] traits = {
            {0.10, 0.15, 0.20, 0.40},
            {0.18, 0.22, 0.28, 0.48},
            {0.30, 0.35, 0.50, 0.60},
            {0.75, 0.68, 0.55, 0.20},
            {0.82, 0.74, 0.62, 0.22},
            {0.50, 0.45, 0.40, 0.30},
            {0.62, 0.58, 0.48, 0.15},
            {0.40, 0.60, 0.70, 0.80},
            {0.88, 0.30, 0.25, 0.10}
        };

        for (int i = 0; i < ids.length; i++) {
            activeEntities.add(new CulturalEntity(
                ids[i], colors[i], rgb[i], traits[i],
                namesEn[i], namesFr[i], namesDe[i], namesEs[i], namesZh[i],
                "Bilateral Exogamous Bands", "Mode 3 Technocomplex"
            ));
        }
    }

    private double getAffinity(CulturalEntity e1, CulturalEntity e2) {
        if (e1 == null || e2 == null) return 0.0;
        if (e1.id().equals(e2.id())) return 1.0;
        String key1 = e1.id() + ":" + e2.id();
        String key2 = e2.id() + ":" + e1.id();
        if (customAffinityOverrides.containsKey(key1)) return customAffinityOverrides.get(key1);
        if (customAffinityOverrides.containsKey(key2)) return customAffinityOverrides.get(key2);
        return e1.affinityWith(e2);
    }

    private void renderEntitiesTab() {
        entitiesBox.getChildren().clear();
        if (activeEntities.isEmpty()) {
            Label empty = new Label(I18n.getOrDefault("cultural.matrix.no_data", "Aucune entité culturelle trouvée pour cette époque."));
            empty.getStyleClass().add("card-description-muted");
            empty.setStyle("-fx-text-fill: #94a3b8;");
            entitiesBox.getChildren().add(empty);
            return;
        }

        for (CulturalEntity entity : activeEntities) {
            VBox card = new VBox(4);
            card.setPadding(new Insets(8, 12, 8, 12));
            card.getStyleClass().add("card-section");
            card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.6); -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 6;");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);

            Rectangle colorBadge = new Rectangle(16, 16);
            try {
                colorBadge.setFill(Color.web(entity.colorHex()));
            } catch (Exception ignored) {
                colorBadge.setFill(Color.WHITE);
            }
            colorBadge.setArcWidth(4);
            colorBadge.setArcHeight(4);
            colorBadge.setStroke(Color.rgb(100, 116, 139, 0.8));

            Label nameLbl = new Label(entity.getLocalizedName());
            nameLbl.getStyleClass().add("card-title");
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #f8fafc;");

            Label idLbl = new Label("[" + entity.id() + "]");
            idLbl.getStyleClass().add("card-description-muted");
            idLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

            header.getChildren().addAll(colorBadge, nameLbl, idLbl);

            Label kinshipLbl = new Label("• " + I18n.getOrDefault("cultural.entity.kinship", "Structure Sociale & Parenté : ") + entity.kinshipType());
            kinshipLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

            Label lithicLbl = new Label("• " + I18n.getOrDefault("cultural.entity.techno", "Technocomplexe Lithique / Métallurgique : ") + entity.lithicTechnocomplex());
            lithicLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

            StringBuilder traitsSb = new StringBuilder("• Traits: [");
            for (int i = 0; i < entity.traits().length; i++) {
                if (i > 0) traitsSb.append(", ");
                traitsSb.append(String.format("%.2f", entity.traits()[i]));
            }
            traitsSb.append("]");
            Label traitsLbl = new Label(traitsSb.toString());
            traitsLbl.setStyle("-fx-font-size: 10.5px; -fx-font-family: monospace; -fx-text-fill: #38bdf8;");

            card.getChildren().addAll(header, kinshipLbl, lithicLbl, traitsLbl);
            entitiesBox.getChildren().add(card);
        }
    }

    private void renderMatrixHeatmap() {
        matrixGrid.getChildren().clear();
        int n = activeEntities.size();
        if (n == 0) return;

        // Top-Left corner header
        Label corner = new Label(I18n.getOrDefault("cultural.matrix.entities_label", "Entités \\ Entités"));
        corner.getStyleClass().add("control-label");
        corner.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-padding: 4;");
        matrixGrid.add(corner, 0, 0);

        // Column headers (Wider and non-truncated)
        for (int j = 0; j < n; j++) {
            CulturalEntity colEnt = activeEntities.get(j);
            VBox colHeader = new VBox(3);
            colHeader.setAlignment(Pos.CENTER);
            colHeader.setPrefWidth(115);
            colHeader.setMinWidth(110);
            colHeader.setPadding(new Insets(4, 2, 4, 2));

            Rectangle badge = new Rectangle(12, 12);
            try { badge.setFill(Color.web(colEnt.colorHex())); } catch (Exception ignored) { badge.setFill(Color.WHITE); }
            badge.setArcWidth(3); badge.setArcHeight(3);
            badge.setStroke(Color.rgb(100, 116, 139, 0.8));

            Label lbl = new Label(colEnt.getLocalizedName());
            lbl.getStyleClass().add("control-label");
            lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-text-alignment: center;");
            lbl.setWrapText(true);
            lbl.setAlignment(Pos.CENTER);
            lbl.setTooltip(new Tooltip(colEnt.getLocalizedName() + " [" + colEnt.id() + "]"));

            colHeader.getChildren().addAll(badge, lbl);
            matrixGrid.add(colHeader, j + 1, 0);
        }

        // Rows and Cells
        for (int i = 0; i < n; i++) {
            CulturalEntity rowEnt = activeEntities.get(i);
            HBox rowHeader = new HBox(6);
            rowHeader.setAlignment(Pos.CENTER_LEFT);
            rowHeader.setPrefWidth(190);
            rowHeader.setMinWidth(180);
            rowHeader.setPadding(new Insets(2, 6, 2, 6));

            Rectangle badge = new Rectangle(12, 12);
            try { badge.setFill(Color.web(rowEnt.colorHex())); } catch (Exception ignored) { badge.setFill(Color.WHITE); }
            badge.setArcWidth(3); badge.setArcHeight(3);
            badge.setStroke(Color.rgb(100, 116, 139, 0.8));

            Label lbl = new Label(rowEnt.getLocalizedName());
            lbl.getStyleClass().add("control-label");
            lbl.setStyle("-fx-font-size: 10.5px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
            lbl.setWrapText(true);
            lbl.setTooltip(new Tooltip(rowEnt.getLocalizedName() + " [" + rowEnt.id() + "]"));

            rowHeader.getChildren().addAll(badge, lbl);
            matrixGrid.add(rowHeader, 0, i + 1);

            // Cells
            for (int j = 0; j < n; j++) {
                final int finalI = i;
                final int finalJ = j;

                StackPane cell = new StackPane();
                cell.setPrefSize(115, 36);
                cell.setCursor(javafx.scene.Cursor.HAND);

                Label valLbl = new Label();
                cell.getChildren().add(valLbl);

                if (i < cellPanes.length && j < cellPanes[0].length) {
                    cellPanes[i][j] = cell;
                    cellLabels[i][j] = valLbl;
                }

                updateCellDisplay(i, j);

                cell.setOnMouseClicked(e -> selectCell(finalI, finalJ));
                matrixGrid.add(cell, j + 1, i + 1);
            }
        }
    }

    private void selectCell(int i, int j) {
        int oldI = selectedI, oldJ = selectedJ;
        selectedI = i;
        selectedJ = j;

        if (oldI >= 0 && oldJ >= 0) {
            updateCellDisplay(oldI, oldJ);
            updateCellDisplay(oldJ, oldI);
        }
        if (selectedI >= 0 && selectedJ >= 0) {
            updateCellDisplay(selectedI, selectedJ);
            updateCellDisplay(selectedJ, selectedI);
        }

        updateQuickEditor();
    }

    private void updateQuickEditor() {
        if (selectedI < 0 || selectedJ < 0 || selectedI >= activeEntities.size() || selectedJ >= activeEntities.size()) {
            quickEditorPairLabel.setText("— " + I18n.getOrDefault("cultural.matrix.select_cell_hint", "Sélectionnez une cellule de la matrice ci-dessus pour l'éditer."));
            quickAffinitySlider.setDisable(true);
            quickAffinitySpinner.setDisable(true);
            presetChipsBox.setDisable(true);
            return;
        }

        CulturalEntity e1 = activeEntities.get(selectedI);
        CulturalEntity e2 = activeEntities.get(selectedJ);
        double aff = getAffinity(e1, e2);

        boolean isDiagonal = (selectedI == selectedJ);
        quickEditorPairLabel.setText(String.format("🏷️ %s ↔ %s %s", e1.getLocalizedName(), e2.getLocalizedName(), isDiagonal ? "(🔒 Auto-identité)" : ""));

        isUpdatingEditor = true;
        quickAffinitySlider.setValue(aff * 100.0);
        quickAffinitySpinner.getValueFactory().setValue(Math.round(aff * 1000.0) / 10.0);
        quickAffinitySlider.setDisable(isDiagonal);
        quickAffinitySpinner.setDisable(isDiagonal);
        presetChipsBox.setDisable(isDiagonal);
        isUpdatingEditor = false;
    }

    private void generateAffinitiesByGeography() {
        int n = activeEntities.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                CulturalEntity e1 = activeEntities.get(i);
                CulturalEntity e2 = activeEntities.get(j);
                double dist = Math.abs(i - j) / (double) n; // Spatial rank distance
                double aff = Math.exp(-2.2 * dist);
                String k1 = e1.id() + ":" + e2.id();
                String k2 = e2.id() + ":" + e1.id();
                customAffinityOverrides.put(k1, aff);
                customAffinityOverrides.put(k2, aff);
            }
        }
        renderMatrixHeatmap();
        updateQuickEditor();
    }

    private void generateAffinitiesByTraits() {
        int n = activeEntities.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                CulturalEntity e1 = activeEntities.get(i);
                CulturalEntity e2 = activeEntities.get(j);
                double aff = e1.affinityWith(e2);
                String k1 = e1.id() + ":" + e2.id();
                String k2 = e2.id() + ":" + e1.id();
                customAffinityOverrides.put(k1, aff);
                customAffinityOverrides.put(k2, aff);
            }
        }
        renderMatrixHeatmap();
        updateQuickEditor();
    }

    private void generateAffinitiesStochastic() {
        int n = activeEntities.size();
        Random rng = new Random();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                CulturalEntity e1 = activeEntities.get(i);
                CulturalEntity e2 = activeEntities.get(j);
                double base = getAffinity(e1, e2);
                double jitter = (rng.nextGaussian() * 0.12);
                double aff = Math.clamp(base + jitter, 0.02, 0.98);
                String k1 = e1.id() + ":" + e2.id();
                String k2 = e2.id() + ":" + e1.id();
                customAffinityOverrides.put(k1, aff);
                customAffinityOverrides.put(k2, aff);
            }
        }
        renderMatrixHeatmap();
        updateQuickEditor();
    }

    private static Color computeHeatmapColor(double val) {
        val = Math.clamp(val, 0.0, 1.0);
        if (val < 0.5) {
            double t = val / 0.5;
            // Dark navy/slate to rich teal/cyan
            return Color.rgb((int)(20 + t * (15 - 20)), (int)(30 + t * (110 - 30)), (int)(65 + t * (140 - 65)));
        } else {
            double t = (val - 0.5) / 0.5;
            // Rich teal to bright emerald green/gold
            return Color.rgb((int)(15 + t * (30 - 15)), (int)(110 + t * (175 - 110)), (int)(140 + t * (110 - 140)));
        }
    }

    private void exportMatrixCsv() {
        if (activeEntities.isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("cultural.matrix.export_title", "Exporter la Matrice d'Affinité Culturelle (CSV)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName("cultural_affinity_matrix_" + Math.abs(currentEpoch) + ".csv");
        File file = chooser.showSaveDialog(this);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.print("EntityID");
                for (CulturalEntity col : activeEntities) {
                    pw.print("," + col.id());
                }
                pw.println();

                for (CulturalEntity row : activeEntities) {
                    pw.print(row.id());
                    for (CulturalEntity col : activeEntities) {
                        pw.printf(java.util.Locale.ROOT, ",%.4f", getAffinity(row, col));
                    }
                    pw.println();
                }
                logger.info("Exported cultural affinity matrix to {}", file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to export matrix to CSV: {}", ex.getMessage());
            }
        }
    }

    private void exportMatrixJson() {
        if (activeEntities.isEmpty()) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("cultural.matrix.export_json_title", "Exporter la Matrice d'Affinité Culturelle (JSON)"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"));
        chooser.setInitialFileName("cultural_affinity_matrix_" + Math.abs(currentEpoch) + ".json");
        File file = chooser.showSaveDialog(this);
        if (file != null) {
            try {
                java.util.Map<String, Object> root = new java.util.LinkedHashMap<>();
                root.put("epoch", currentEpoch);
                root.put("scenarioEpoch", scenarioStartYear);
                root.put("entitiesCount", activeEntities.size());
                root.put("entities", activeEntities);

                java.util.Map<String, java.util.Map<String, Double>> matrix = new java.util.LinkedHashMap<>();
                for (CulturalEntity row : activeEntities) {
                    java.util.Map<String, Double> rowMap = new java.util.LinkedHashMap<>();
                    for (CulturalEntity col : activeEntities) {
                        rowMap.put(col.id(), getAffinity(row, col));
                    }
                    matrix.put(row.id(), rowMap);
                }
                root.put("affinityMatrix", matrix);

                MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, root);
                logger.info("Exported cultural affinity matrix to JSON: {}", file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to export matrix to JSON: {}", ex.getMessage());
            }
        }
    }
}
