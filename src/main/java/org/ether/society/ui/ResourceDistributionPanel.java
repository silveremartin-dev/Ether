/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.data.OnlineMapService;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import org.ether.society.model.EcologyPreset;
import org.ether.society.procedural.PlanetPreset;
import org.ether.society.procedural.ProceduralGenerator;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Enhanced UI Panel for editing planet-wide ecological and resource distribution 
 * using real scientific metric variables (GtC, Gt, Mt, mW/m², 10³ km³).
 * 
 * Features automated planetary derivation from Tab 1 physics, spatial dispersion algorithms,
 * custom biome/geology map imports (PNG / WMS / ESRI World Files), and real-time 2D visualization.
 * 
 * @author Silvere Martin-Michiellot
 * @version 2.5.0
 */
public class ResourceDistributionPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ResourceDistributionPanel.class);

    private List<H3Cell> activeCells;
    private PlanetPreset activePlanetPreset;
    private final Consumer<List<H3Cell>> onResourcesAppliedCallback;
    private final ImageMapLoader mapLoader = new ImageMapLoader();
    private final OnlineMapService onlineMapService = new OnlineMapService();
    private final ProceduralGenerator generator = new ProceduralGenerator();

    // Custom Ecology Image Maps
    private Image customBiomeImage;
    private Image customResourceImage;

    // Inherited Planet Preset & Context Controls
    private Label planetSectionHeader;
    private ComboBox<PlanetPreset> planetPresetCombo;
    private Label planetContextLabel;
    private Button syncPlanetBtn;
    private Button autoDeriveEcologyBtn;
    private Button autoDeriveGeologyBtn;

    // Spatial Distribution Pattern Selector
    private ComboBox<String> distributionPatternCombo;

    // Presets Bar
    private PresetControlBar<EcologyPreset> ecologyPresetBar;

    // Scientific Resource Sliders (Metric Units)
    private Slider terrestrialBiomassSlider;  // GtC (Gigatons of Carbon)
    private Slider soilCarbonSlider;          // GtC (Soil organic carbon / Agriculture)
    private Slider faunaBiomassSlider;        // GtC (Terrestrial animal & game fauna)
    private Slider aquaticBiomassSlider;      // GtC (Marine & freshwater life)
    private Slider crustalMetalSlider;        // Gt (Industrial base metals: Fe, Cu, Al)
    private Slider preciousMetalSlider;       // Mt (Precious & rare earth ores: Au, Pt, REE)
    private Slider mantleHeatSlider;          // mW/m² (Mantle heat flow & geothermal/tectonic index)
    private Slider freshwaterAquiferSlider;   // 10³ km³ (Groundwater & deep aquifer reserves)

    // Custom Map Controls & Buttons
    private Label mapsSecHeader;
    private ComboBox<String> mapSourceCombo;
    private Label mapSourceRowLabel;
    private Label biomeFileLabel;
    private Label resourceFileLabel;
    private Label biomeMapRowLabel;
    private Label resourceMapRowLabel;
    private Button loadBiomeBtn;
    private Button clearBiomeBtn;
    private Button loadResourceBtn;
    private Button clearResourceBtn;
    private Button fetchOnlineBtn;
    private Button exportMapsBtn;
    private Button specsHelpBtn;
    private Label mapStatusLabel;

    // Section & Row Labels for i18n
    private Label headerLabel;
    private Label floraSecHeader;
    private Label faunaSecHeader;
    private Label mineralSecHeader;
    private Label aquaticSecHeader;

    private Label terrestrialBiomassRowLabel;
    private Label soilCarbonRowLabel;
    private Label faunaBiomassRowLabel;
    private Label aquaticBiomassRowLabel;
    private Label crustalMetalRowLabel;
    private Label preciousMetalRowLabel;
    private Label mantleHeatRowLabel;
    private Label freshwaterAquiferRowLabel;

    // Right-Side View Visualizers & Preview
    private Label rightViewTitle;
    private ComboBox<String> viewModeCombo;
    private Canvas mapPreviewCanvas;
    private HBox legendBar;
    private Label summaryLabel;
    private Button applyBtn;

    public ResourceDistributionPanel(Consumer<List<H3Cell>> onResourcesAppliedCallback) {
        this.onResourcesAppliedCallback = onResourcesAppliedCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void setActivePlanetPreset(PlanetPreset planetPreset) {
        this.activePlanetPreset = planetPreset;
        if (planetPreset != null && planetPresetCombo != null) {
            planetPresetCombo.setValue(planetPreset);
        }
        updatePlanetContextDisplay();
        updatePreviewCanvas();
    }

    public void setActiveCells(List<H3Cell> cells) {
        this.activeCells = cells;
        updateSummary();
        updatePreviewCanvas();
    }

    private void initUI() {
        VBox controlsBox = new VBox(15);
        controlsBox.setPrefWidth(460);
        controlsBox.setPadding(new Insets(10));

        headerLabel = new Label();
        headerLabel.getStyleClass().add("label-title");

        // --- 1. Inherited Planet Preset & Territory Section ---
        planetSectionHeader = new Label(I18n.getOrDefault("resource.section.planet_preset", "PLANÈTE & DÉDUCTION SCIENTIFIQUE (ONGLET 1)"));

        planetPresetCombo = new ComboBox<>();
        planetPresetCombo.getItems().setAll(PlanetPreset.getPresets());
        planetPresetCombo.setValue(PlanetPreset.EARTH_LIKE);
        planetPresetCombo.setMaxWidth(Double.MAX_VALUE);
        planetPresetCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(PlanetPreset item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.name());
                }
            }
        });
        planetPresetCombo.setButtonCell(planetPresetCombo.getCellFactory().call(null));
        planetPresetCombo.setOnAction(e -> {
            PlanetPreset selected = planetPresetCombo.getValue();
            if (selected != null) {
                this.activePlanetPreset = selected;
                adaptResourceSlidersToPlanet(selected);
                updatePlanetContextDisplay();
                updatePreviewCanvas();
            }
        });

        planetContextLabel = new Label("🪐 Territoire : Planète Terrestre (Earth-Like)");
        planetContextLabel.setWrapText(true);
        planetContextLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-padding: 6 10; -fx-background-color: rgba(56, 189, 248, 0.12); -fx-background-radius: 6; -fx-border-color: rgba(56, 189, 248, 0.3); -fx-border-radius: 6;");

        syncPlanetBtn = new Button(I18n.getOrDefault("resource.btn.sync_planet", "🔄 Synchroniser avec la Planète de l'Onglet 1"));
        syncPlanetBtn.getStyleClass().add("button-secondary");
        syncPlanetBtn.setMaxWidth(Double.MAX_VALUE);
        syncPlanetBtn.setOnAction(e -> {
            if (activePlanetPreset != null) {
                planetPresetCombo.setValue(activePlanetPreset);
                adaptResourceSlidersToPlanet(activePlanetPreset);
                updatePlanetContextDisplay();
                updatePreviewCanvas();
            }
        });

        autoDeriveEcologyBtn = new Button("⚡ Auto-déduire Écologie & Biomes depuis la Physique");
        autoDeriveEcologyBtn.getStyleClass().add("button-secondary");
        autoDeriveEcologyBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveEcologyBtn.setOnAction(e -> autoDeriveEcologyFromPlanet());

        autoDeriveGeologyBtn = new Button("🌋 Calculer Tectonique & Métaux depuis la Manteau");
        autoDeriveGeologyBtn.getStyleClass().add("button-secondary");
        autoDeriveGeologyBtn.setMaxWidth(Double.MAX_VALUE);
        autoDeriveGeologyBtn.setOnAction(e -> autoDeriveGeologyFromPlanet());

        VBox planetSection = createSection(planetSectionHeader, new VBox(8,
                new Label(I18n.getOrDefault("resource.label.planet_preset_select", "Préréglage de corps céleste hérité :")),
                planetPresetCombo,
                planetContextLabel,
                syncPlanetBtn,
                autoDeriveEcologyBtn,
                autoDeriveGeologyBtn
        ));

        // --- 2. Standardized Preset Control Bar for Ecology ---
        ecologyPresetBar = new PresetControlBar<>(I18n.getOrDefault("resource.preset_title", "Préréglage Écologique & Ressources"));
        ecologyPresetBar.setPresets(EcologyPreset.getBuiltInPresets(), EcologyPreset.getBuiltInPresets().get(0));
        ecologyPresetBar.setListener(new PresetControlBar.PresetActionsListener<EcologyPreset>() {
            @Override
            public void onPresetSelected(EcologyPreset preset) {
                applyEcologyPreset(preset);
            }

            @Override
            public void onSavePreset(String name) {
                EcologyPreset custom = new EcologyPreset(
                        name,
                        terrestrialBiomassSlider.getValue(),
                        soilCarbonSlider.getValue(),
                        faunaBiomassSlider.getValue(),
                        aquaticBiomassSlider.getValue(),
                        crustalMetalSlider.getValue(),
                        preciousMetalSlider.getValue(),
                        mantleHeatSlider.getValue(),
                        freshwaterAquiferSlider.getValue()
                );
                ecologyPresetBar.getPresetCombo().getItems().add(custom);
                ecologyPresetBar.getPresetCombo().setValue(custom);
            }

            @Override
            public void onDeletePreset(EcologyPreset preset) {
                ecologyPresetBar.getPresetCombo().getItems().remove(preset);
            }

            @Override
            public void onExportPreset(File targetFile, EcologyPreset preset) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, preset);
                    logger.info("Exported ecology preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export ecology preset", ex);
                }
            }

            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    EcologyPreset preset = mapper.readValue(sourceFile, EcologyPreset.class);
                    ecologyPresetBar.getPresetCombo().getItems().add(preset);
                    ecologyPresetBar.getPresetCombo().setValue(preset);
                    applyEcologyPreset(preset);
                    logger.info("Imported ecology preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import ecology preset", ex);
                }
            }
        });

        // --- 3. Spatial Resource Distribution Patterns ---
        distributionPatternCombo = new ComboBox<>();
        distributionPatternCombo.getItems().addAll(
                "🗺️ Tectonique & Orogénique (Concentration le long des chaînes de montagnes)",
                "🌐 Gradient Latitudinal & Climatologique (Bandes thermiques & saisons)",
                "💧 Bassins Versants & Sédimentaires (Plaines & nappes phréatiques)",
                "🌊 Upwelling Côtier & Plateaux Continentaux (Richesses marines)"
        );
        distributionPatternCombo.setValue(distributionPatternCombo.getItems().get(0));
        distributionPatternCombo.setMaxWidth(Double.MAX_VALUE);
        distributionPatternCombo.setOnAction(e -> updatePreviewCanvas());

        VBox patternSection = createSection(new Label("PATRONS DE DISTRIBUTION SPATIALE"), new VBox(6,
                new Label("Algorithme de dispersion sur la grille H3 :"),
                distributionPatternCombo
        ));

        // --- 4. Custom Satellite & Geological Map Section (PNG / WMS / WorldFiles) ---
        mapsSecHeader = new Label(I18n.getOrDefault("resource.section.custom_maps", "CARTES SATELLITE & SITES GÉOLOGIQUES (PNG / WMS)"));

        mapSourceRowLabel = new Label(I18n.getOrDefault("resource.param.map_source", "Modèle de corps céleste / Satellite :"));
        mapSourceCombo = new ComboBox<>();
        mapSourceCombo.getItems().addAll("none", "earth", "mars", "venus", "moon");
        mapSourceCombo.setValue("none");
        mapSourceCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(I18n.getOrDefault("planet.map." + item, item));
                }
            }
        });
        mapSourceCombo.setButtonCell(mapSourceCombo.getCellFactory().call(null));
        mapSourceCombo.setMaxWidth(Double.MAX_VALUE);
        mapSourceCombo.setOnAction(e -> applyPresetMapSource(mapSourceCombo.getValue()));

        biomeMapRowLabel = new Label(I18n.getOrDefault("resource.param.biome_map", "Carte de biomes / Végétation extraterrestre (PNG) :"));
        biomeFileLabel = new Label(I18n.get("planet.map.none"));
        biomeFileLabel.getStyleClass().add("value-label");
        loadBiomeBtn = new Button(I18n.get("planet.map.btn_load"));
        loadBiomeBtn.getStyleClass().add("button-secondary");
        loadBiomeBtn.setOnAction(e -> loadCustomBiomeMap());
        clearBiomeBtn = new Button("❌");
        clearBiomeBtn.getStyleClass().add("button-secondary");
        clearBiomeBtn.setOnAction(e -> {
            customBiomeImage = null;
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            updatePreviewCanvas();
            updateSummary();
        });
        HBox biomeBox = new HBox(5, loadBiomeBtn, clearBiomeBtn);

        resourceMapRowLabel = new Label(I18n.getOrDefault("resource.param.resource_map", "Carte géologique & minerais multi-canaux (PNG) :"));
        resourceFileLabel = new Label(I18n.get("planet.map.none"));
        resourceFileLabel.getStyleClass().add("value-label");
        loadResourceBtn = new Button(I18n.get("planet.map.btn_load"));
        loadResourceBtn.getStyleClass().add("button-secondary");
        loadResourceBtn.setOnAction(e -> loadCustomResourceMap());
        clearResourceBtn = new Button("❌");
        clearResourceBtn.getStyleClass().add("button-secondary");
        clearResourceBtn.setOnAction(e -> {
            customResourceImage = null;
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            updatePreviewCanvas();
            updateSummary();
        });
        HBox resourceBox = new HBox(5, loadResourceBtn, clearResourceBtn);

        fetchOnlineBtn = new Button(I18n.getOrDefault("resource.btn.fetch_online", "🌐 Télécharger cartes satellite WMS (USGS / NASA)"));
        fetchOnlineBtn.setMaxWidth(Double.MAX_VALUE);
        fetchOnlineBtn.getStyleClass().add("button-secondary");
        fetchOnlineBtn.setOnAction(e -> fetchOnlineSatelliteData());

        exportMapsBtn = new Button(I18n.getOrDefault("resource.btn.export_maps", "📤 Exporter cartes (PNG + WorldFile .tfw)"));
        exportMapsBtn.setMaxWidth(Double.MAX_VALUE);
        exportMapsBtn.getStyleClass().add("button-secondary");
        exportMapsBtn.setOnAction(e -> exportMapsWithWorldFiles());

        specsHelpBtn = new Button(I18n.getOrDefault("resource.btn.specs_help", "ℹ️ Spécifications des cartes de biomes & géologie"));
        specsHelpBtn.setMaxWidth(Double.MAX_VALUE);
        specsHelpBtn.getStyleClass().add("button-secondary");
        specsHelpBtn.setOnAction(e -> showEcologyImportFormatHelp());

        mapStatusLabel = new Label();
        mapStatusLabel.getStyleClass().add("value-label");
        mapStatusLabel.setWrapText(true);

        VBox customMapsSection = createSection(mapsSecHeader, new VBox(8,
                createControlRow(mapSourceRowLabel, mapSourceCombo, I18n.getOrDefault("resource.tooltip.map_source", "Sélectionner une carte satellite prédéfinie")),
                createControlRow(biomeMapRowLabel, new VBox(3, biomeBox, biomeFileLabel), "Import d'une carte de biomes au format PNG/JPEG"),
                createControlRow(resourceMapRowLabel, new VBox(3, resourceBox, resourceFileLabel), "Import d'une carte de minerais & géologie multi-canaux"),
                fetchOnlineBtn,
                exportMapsBtn,
                specsHelpBtn,
                mapStatusLabel
        ));

        // --- 5. Flora & Terrestrial Plant Resources ---
        terrestrialBiomassSlider = createSlider(1.0, 5000.0, 450.0);
        soilCarbonSlider = createSlider(10.0, 10000.0, 1500.0);
        terrestrialBiomassRowLabel = new Label("Biomasse Végétale & Forêts Globale :");
        soilCarbonRowLabel = new Label("Carbone Organique des Sols & Agriculture :");
        floraSecHeader = new Label("1. ÉCOLOGIE VÉGÉTALE & SOLS (MÉTRIQUES)");

        VBox floraSection = createSection(floraSecHeader, new VBox(8,
                createControlRow(terrestrialBiomassRowLabel, terrestrialBiomassSlider, "%.0f GtC", "Stock global de biomasse végétale et forêts (Gigatonnes de Carbone)"),
                createControlRow(soilCarbonRowLabel, soilCarbonSlider, "%.0f GtC", "Stock de carbone organique dans les sols et potentiel agricole (GtC)")
        ));

        // --- 6. Fauna & Animal Resources ---
        faunaBiomassSlider = createSlider(0.01, 50.0, 2.0);
        aquaticBiomassSlider = createSlider(0.1, 100.0, 6.0);
        faunaBiomassRowLabel = new Label("Biomasse Faunique Terrestre & Sauvage :");
        aquaticBiomassRowLabel = new Label("Biomasse Marine & Estuarienne :");
        faunaSecHeader = new Label("2. BIOMASSE ANIMALE & AQUATIQUE (MÉTRIQUES)");

        VBox faunaSection = createSection(faunaSecHeader, new VBox(8,
                createControlRow(faunaBiomassRowLabel, faunaBiomassSlider, "%.2f GtC", "Biomasse totale des vertébrés et vertébrés terrestres sauvages (GtC)"),
                createControlRow(aquaticBiomassRowLabel, aquaticBiomassSlider, "%.2f GtC", "Stock de biomasse marine, poissons et phytoplancton (GtC)")
        ));

        // --- 7. Minerals & Underground Geology ---
        crustalMetalSlider = createSlider(1.0, 2000.0, 80.0);
        preciousMetalSlider = createSlider(10.0, 50000.0, 1200.0);
        mantleHeatSlider = createSlider(10.0, 400.0, 87.0);
        freshwaterAquiferSlider = createSlider(10.0, 100000.0, 15000.0);
        crustalMetalRowLabel = new Label("Réserves Métalliques Industrielles (Fer/Cuivre) :");
        preciousMetalRowLabel = new Label("Minerais Précieux & Terres Rares (Or/Pt) :");
        mantleHeatRowLabel = new Label("Flux Thermique du Manteau & Tectonique :");
        freshwaterAquiferRowLabel = new Label("Réserves d'Eau Douce & Aquifères :");
        mineralSecHeader = new Label("3. GÉOLOGIE & HYDROLIGIE (MÉTRIQUES)");

        VBox mineralSection = createSection(mineralSecHeader, new VBox(8,
                createControlRow(crustalMetalRowLabel, crustalMetalSlider, "%.0f Gt", "Réserves de métaux industriels dans la croûte (Gigatonnes)"),
                createControlRow(preciousMetalRowLabel, preciousMetalSlider, "%.0f Mt", "Stock d'éléments précieux et terres rares (Mégatonnes)"),
                createControlRow(mantleHeatRowLabel, mantleHeatSlider, "%.1f mW/m²", "Flux thermique du manteau, volcanisme et activité géothermique"),
                createControlRow(freshwaterAquiferRowLabel, freshwaterAquiferSlider, "%.0f x10³ km³", "Volume total des nappes phréatiques et aquifères continentaux")
        ));

        controlsBox.getChildren().addAll(
                headerLabel,
                planetSection,
                ecologyPresetBar,
                patternSection,
                customMapsSection,
                floraSection,
                faunaSection,
                mineralSection
        );

        ScrollPane scrollControls = new ScrollPane(controlsBox);
        scrollControls.setFitToWidth(true);
        scrollControls.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        // --- Center / Right View: Interactive Visualization & Summary ---
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(10));

        rightViewTitle = new Label(I18n.getOrDefault("resource.title.right_view", "AFFICHEUR & CARTOGRAPHIE DES RESSOURCES"));
        rightViewTitle.getStyleClass().add("label-header");

        // View Mode Selector
        viewModeCombo = new ComboBox<>();
        viewModeCombo.getItems().addAll(
                "🌿 Carte des Biomes & Végétation (GtC)",
                "🪨 Carte Géologique & Métaux (Gt Fer/Cuivre)",
                "🌋 Flux Thermique & Ceintures Tectoniques (mW/m²)",
                "💧 Aquifères & Biomasse Aquatique (10³ km³)"
        );
        viewModeCombo.setValue(viewModeCombo.getItems().get(0));
        viewModeCombo.setMaxWidth(380);
        viewModeCombo.setOnAction(e -> {
            updateLegend();
            updatePreviewCanvas();
        });

        // 2D Map Canvas
        mapPreviewCanvas = new Canvas(640, 360);
        mapPreviewCanvas.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 10, 0, 0, 0);");
        Tooltip.install(mapPreviewCanvas, new Tooltip(I18n.getOrDefault("resource.tooltip.preview", "Aperçu dynamique de la répartition géographique des ressources")));

        legendBar = new HBox(10);
        legendBar.setAlignment(Pos.CENTER);
        legendBar.setPadding(new Insets(5, 10, 5, 10));
        legendBar.getStyleClass().add("card-section");
        updateLegend();

        summaryLabel = new Label("🌍 Générez ou chargez des cellules planétaires pour activer la carte.");
        summaryLabel.getStyleClass().add("control-label");
        summaryLabel.setStyle("-fx-alignment: center;");
        summaryLabel.setWrapText(true);

        applyBtn = new Button();
        applyBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 12 24; -fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 6;");
        applyBtn.setOnAction(e -> applyResourceDistribution());
        applyBtn.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.apply", "Appliquer la distribution écologique et passer à l'étape suivante")));

        centerBox.getChildren().addAll(rightViewTitle, viewModeCombo, mapPreviewCanvas, legendBar, summaryLabel, applyBtn);

        setLeft(scrollControls);
        setCenter(centerBox);

        updatePlanetContextDisplay();
        updatePreviewCanvas();
    }

    private void autoDeriveEcologyFromPlanet() {
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (p == null) p = PlanetPreset.EARTH_LIKE;

        // Calculate surface area scaling factor relative to Earth
        double radiusRatio = p.radiusKm() / 6371.0;
        double areaScale = Math.pow(radiusRatio, 2);

        // Water ratio modifier
        double waterMod = Math.max(0.05, 1.0 + p.waterLevel());
        double tempMod = Math.max(0.1, 1.0 - Math.abs(p.averageTempC() - 15.0) / 60.0);

        terrestrialBiomassSlider.setValue(450.0 * areaScale * waterMod * tempMod);
        soilCarbonSlider.setValue(1500.0 * areaScale * tempMod);
        faunaBiomassSlider.setValue(2.0 * areaScale * waterMod * tempMod);
        aquaticBiomassSlider.setValue(6.0 * areaScale * (1.0 + p.waterLevel() * 1.5));
        freshwaterAquiferSlider.setValue(15000.0 * areaScale * waterMod);

        updateSummary();
        updatePreviewCanvas();
        logger.info("Auto-derived ecological parameters for planet {}", p.name());
    }

    private void autoDeriveGeologyFromPlanet() {
        PlanetPreset p = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (p == null) p = PlanetPreset.EARTH_LIKE;

        double radiusRatio = p.radiusKm() / 6371.0;
        double volumeScale = Math.pow(radiusRatio, 3);

        crustalMetalSlider.setValue(80.0 * volumeScale);
        preciousMetalSlider.setValue(1200.0 * volumeScale);
        // Smaller bodies cool faster -> lower mantle heat flow
        mantleHeatSlider.setValue(Math.min(300.0, 87.0 * radiusRatio));

        updateSummary();
        updatePreviewCanvas();
        logger.info("Auto-derived geological parameters for planet {}", p.name());
    }

    private void updatePlanetContextDisplay() {
        if (planetContextLabel == null) return;
        PlanetPreset preset = activePlanetPreset != null ? activePlanetPreset : planetPresetCombo.getValue();
        if (preset == null) preset = PlanetPreset.EARTH_LIKE;

        int cellCount = activeCells != null ? activeCells.size() : 12482;
        String text = String.format(
                "🪐 Planète Héritée : %s | Rayon : %,.0f km | Temp : %.1f°C | Eau : %.0f%% | Cellules : %,d",
                preset.name(),
                preset.radiusKm(),
                preset.averageTempC(),
                preset.waterLevel() * 100.0,
                cellCount
        );
        planetContextLabel.setText(text);
    }

    private void adaptResourceSlidersToPlanet(PlanetPreset p) {
        if (p == null) return;
        if (p.isSatellite() || p.radiusKm() < 3000) { // Moon/Titan
            terrestrialBiomassSlider.setValue(10.0);
            soilCarbonSlider.setValue(50.0);
            faunaBiomassSlider.setValue(0.01);
            crustalMetalSlider.setValue(150.0);
            mantleHeatSlider.setValue(25.0);
        } else if (p.waterLevel() < -0.2) { // Arid / Mars
            terrestrialBiomassSlider.setValue(50.0);
            soilCarbonSlider.setValue(200.0);
            aquaticBiomassSlider.setValue(0.2);
            freshwaterAquiferSlider.setValue(1200.0);
        } else if (p.waterLevel() > 0.4) { // Ocean World
            terrestrialBiomassSlider.setValue(120.0);
            aquaticBiomassSlider.setValue(25.0);
            freshwaterAquiferSlider.setValue(45000.0);
        } else { // Earth-Like
            terrestrialBiomassSlider.setValue(450.0);
            soilCarbonSlider.setValue(1500.0);
            faunaBiomassSlider.setValue(2.0);
            aquaticBiomassSlider.setValue(6.0);
            crustalMetalSlider.setValue(80.0);
            preciousMetalSlider.setValue(1200.0);
            mantleHeatSlider.setValue(87.0);
            freshwaterAquiferSlider.setValue(15000.0);
        }
        updateSummary();
    }

    private void applyEcologyPreset(EcologyPreset p) {
        if (p == null) return;
        terrestrialBiomassSlider.setValue(p.terrestrialBiomassGtC());
        soilCarbonSlider.setValue(p.soilOrganicCarbonGtC());
        faunaBiomassSlider.setValue(p.faunaBiomassGtC());
        aquaticBiomassSlider.setValue(p.aquaticBiomassGtC());
        crustalMetalSlider.setValue(p.crustalMetalOresGt());
        preciousMetalSlider.setValue(p.preciousMetalOresMt());
        mantleHeatSlider.setValue(p.mantleHeatFlowMwM2());
        freshwaterAquiferSlider.setValue(p.freshwaterReserveKm3());
        updateSummary();
        updatePreviewCanvas();
    }

    private void loadCustomBiomeMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Charger une carte de biomes (PNG/JPEG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customBiomeImage = new Image(new FileInputStream(file));
                biomeFileLabel.setText("📷 " + file.getName());
                updateSummary();
                updatePreviewCanvas();
            } catch (Exception ex) {
                logger.error("Failed to load custom biome map", ex);
            }
        }
    }

    private void loadCustomResourceMap() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Charger une carte géologique & minerais (PNG/JPEG)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images PNG/JPG", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            try {
                customResourceImage = new Image(new FileInputStream(file));
                resourceFileLabel.setText("🪨 " + file.getName());
                updateSummary();
                updatePreviewCanvas();
            } catch (Exception ex) {
                logger.error("Failed to load custom resource map", ex);
            }
        }
    }

    private void applyPresetMapSource(String sourceKey) {
        if ("none".equals(sourceKey)) {
            customBiomeImage = null;
            customResourceImage = null;
            biomeFileLabel.setText(I18n.get("planet.map.none"));
            resourceFileLabel.setText(I18n.get("planet.map.none"));
            updatePreviewCanvas();
            updateSummary();
            return;
        }

        if ("earth".equals(sourceKey)) {
            try (var biomeStream = getClass().getResourceAsStream("/maps/earth_biome.png")) {
                if (biomeStream != null) {
                    customBiomeImage = new Image(biomeStream);
                    biomeFileLabel.setText("📷 Earth MODIS Biomes");
                }
            } catch (Exception ex) {
                logger.warn("Could not load default earth biome resource map", ex);
            }
        } else if ("mars".equals(sourceKey)) {
            biomeFileLabel.setText("📷 Mars MOLA Geology");
        }
        updatePreviewCanvas();
        updateSummary();
    }

    private void fetchOnlineSatelliteData() {
        String sourceKey = mapSourceCombo.getValue();
        OnlineMapService.CelestialBody body = switch (sourceKey) {
            case "mars" -> OnlineMapService.CelestialBody.MARS;
            case "moon" -> OnlineMapService.CelestialBody.MOON;
            case "venus" -> OnlineMapService.CelestialBody.VENUS;
            default -> OnlineMapService.CelestialBody.EARTH;
        };

        mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_fetching", "Téléchargement WMS en cours..."));

        if (body.getBiomeWmsUrl() != null) {
            onlineMapService.fetchBiomeMapAsync(body).thenAccept(img -> {
                javafx.application.Platform.runLater(() -> {
                    if (img != null) {
                        customBiomeImage = img;
                        biomeFileLabel.setText("🌐 " + body.getName() + " WMS Biomes");
                        mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_success", "Carte satellite téléchargée avec succès."));
                        updatePreviewCanvas();
                        updateSummary();
                    } else {
                        mapStatusLabel.setText(I18n.getOrDefault("planet.map.status_error", "Erreur lors du téléchargement."));
                    }
                });
            });
        } else {
            mapStatusLabel.setText("Information : Pas de WMS spécifique pour " + body.getName() + ", utilisation du modèle procédural.");
        }
    }

    private void exportMapsWithWorldFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter l'image de carte avec ESRI World File (.tfw)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Carte PNG", "*.png"));
        File file = chooser.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
        if (file != null) {
            WritableImage wimg = new WritableImage((int) mapPreviewCanvas.getWidth(), (int) mapPreviewCanvas.getHeight());
            mapPreviewCanvas.snapshot(null, wimg);
            mapLoader.exportMapToPngAndWorldFile(wimg, file);
        }
    }

    private void showEcologyImportFormatHelp() {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Spécifications des Cartes de Biomes & Géologie");
        dialog.setHeaderText("Formats d'images et données géologiques attendus");
        dialog.setContentText(
                "Vous pouvez importer des cartes de biomes et de géologie sous forme d'images PNG/JPEG au ratio 2:1 (projection équirectangulaire) :\n\n" +
                "1. CARTE DE BIOMES (ÉCOSYSSTÈMES) :\n" +
                "   • Océan Profond : RGB(0, 0, 100)\n" +
                "   • Océan : RGB(0, 50, 200)\n" +
                "   • Plage / Littoral : RGB(240, 220, 150)\n" +
                "   • Plaines / Prairies : RGB(100, 200, 50)\n" +
                "   • Forêt Tempérée : RGB(20, 120, 20)\n" +
                "   • Jungle Tropicale : RGB(0, 80, 0)\n" +
                "   • Désert Aride : RGB(255, 200, 50)\n" +
                "   • Collines : RGB(150, 150, 100)\n" +
                "   • Montagnes : RGB(100, 100, 100)\n" +
                "   • Toundra : RGB(150, 200, 220)\n" +
                "   • Neige / Glaciers : RGB(255, 255, 255)\n\n" +
                "2. CARTE GÉOLOGIQUE ET MINERAIS (MULTI-CANAUX RGB) :\n" +
                "   • Canal Rouge (R) = Gisements Métalliques (Fer, Cuivre, Bronze)\n" +
                "   • Canal Vert (V) = Densité du Bois, Forêts et Biomasse Végétale\n" +
                "   • Canal Bleu (B) = Nappe Phréatique et Poissonnerie Aquatique\n\n" +
                "3. GÉORÉFÉRENCEMENT ESRI WORLD FILE (.tfw) :\n" +
                "   • L'export génère automatiquement un fichier compagnon .tfw pour l'alignement dans les logiciels GIS (QGIS, ArcGIS)."
        );
        dialog.showAndWait();
    }

    private void updateLegend() {
        if (legendBar == null) return;
        legendBar.getChildren().clear();

        int selectedIdx = viewModeCombo != null ? viewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        if (selectedIdx == 0) { // Biomes
            addLegendItem("DEEP_OCEAN", Color.rgb(0, 0, 100), "Abysses");
            addLegendItem("OCEAN", Color.rgb(0, 50, 200), "Océan");
            addLegendItem("PLAINS", Color.rgb(100, 200, 50), "Plaines");
            addLegendItem("FOREST", Color.rgb(20, 120, 20), "Forêt");
            addLegendItem("JUNGLE", Color.rgb(0, 80, 0), "Jungle");
            addLegendItem("DESERT", Color.rgb(255, 200, 50), "Désert");
            addLegendItem("MOUNTAINS", Color.rgb(100, 100, 100), "Montagnes");
        } else if (selectedIdx == 1) { // Minerals / Ores
            addLegendItem("LOW", Color.rgb(30, 40, 50), "Faible Deposit");
            addLegendItem("MED", Color.rgb(180, 100, 40), "Moyen");
            addLegendItem("HIGH", Color.rgb(240, 60, 20), "Riche Gisements");
        } else if (selectedIdx == 2) { // Mantle / Tectonic
            addLegendItem("LOW", Color.rgb(40, 40, 60), "Inerte");
            addLegendItem("MED", Color.rgb(180, 80, 30), "Tectonique Modérée");
            addLegendItem("HIGH", Color.rgb(240, 20, 20), "Magmatisme Élevé");
        } else { // Aquifer / Aquatic
            addLegendItem("LOW", Color.rgb(20, 40, 80), "Aride / Sec");
            addLegendItem("MED", Color.rgb(40, 120, 200), "Abondance Modérée");
            addLegendItem("HIGH", Color.rgb(0, 220, 255), "Aquifère / Poissonnerie");
        }
    }

    private void addLegendItem(String id, Color col, String text) {
        Pane colorSwatch = new Pane();
        colorSwatch.setPrefSize(14, 14);
        colorSwatch.setStyle(String.format("-fx-background-color: rgba(%d,%d,%d,1); -fx-border-color: #ffffff; -fx-border-width: 1; -fx-background-radius: 3;",
                (int)(col.getRed()*255), (int)(col.getGreen()*255), (int)(col.getBlue()*255)));
        Label lbl = new Label(text);
        lbl.getStyleClass().add("control-label");
        HBox itemBox = new HBox(4, colorSwatch, lbl);
        itemBox.setAlignment(Pos.CENTER);
        legendBar.getChildren().add(itemBox);
    }

    private void updatePreviewCanvas() {
        if (mapPreviewCanvas == null) return;

        GraphicsContext gc = mapPreviewCanvas.getGraphicsContext2D();
        PixelWriter pw = gc.getPixelWriter();

        int w = (int) mapPreviewCanvas.getWidth();
        int h = (int) mapPreviewCanvas.getHeight();

        int mode = viewModeCombo != null ? viewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        PlanetPreset planet = activePlanetPreset != null ? activePlanetPreset : (planetPresetCombo != null ? planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;

        PixelReader customBiomeReader = customBiomeImage != null ? customBiomeImage.getPixelReader() : null;
        PixelReader customResReader = customResourceImage != null ? customResourceImage.getPixelReader() : null;
        double wBio = customBiomeImage != null ? customBiomeImage.getWidth() : 0;
        double hBio = customBiomeImage != null ? customBiomeImage.getHeight() : 0;
        double wRes = customResourceImage != null ? customResourceImage.getWidth() : 0;
        double hRes = customResourceImage != null ? customResourceImage.getHeight() : 0;

        double tBiomass = terrestrialBiomassSlider != null ? terrestrialBiomassSlider.getValue() : 450.0;
        double mMetal = crustalMetalSlider != null ? crustalMetalSlider.getValue() : 80.0;
        double mHeat = mantleHeatSlider != null ? mantleHeatSlider.getValue() : 87.0;
        double fAquifer = freshwaterAquiferSlider != null ? freshwaterAquiferSlider.getValue() : 15000.0;

        for (int y = 0; y < h; y++) {
            double lat = 90.0 - (y / (double) h) * 180.0;
            for (int x = 0; x < w; x++) {
                double lon = -180.0 + (x / (double) w) * 360.0;

                Color pxColor;

                if (mode == 0) { // Biome Map
                    if (customBiomeReader != null) {
                        int bx = (int) Math.min((x / (double) w) * wBio, wBio - 1);
                        int by = (int) Math.min((y / (double) h) * hBio, hBio - 1);
                        pxColor = customBiomeReader.getColor(bx, by);
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        pxColor = mapLoader.getBiomeTargetColor(point.biome());
                    }
                } else if (mode == 1) { // Mineral / Geology Map
                    if (customResReader != null) {
                        int rx = (int) Math.min((x / (double) w) * wRes, wRes - 1);
                        int ry = (int) Math.min((y / (double) h) * hRes, hRes - 1);
                        Color c = customResReader.getColor(rx, ry);
                        pxColor = Color.rgb((int)(c.getRed()*255), (int)(c.getRed()*180), (int)(c.getRed()*100));
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        if (point.elevation() < 0) {
                            pxColor = Color.rgb(15, 23, 42); // Slate ocean floor
                        } else {
                            double metalDensity = Math.min(1.0, (Math.abs(point.elevation()) / 8848.0 + 0.2) * (mMetal / 80.0));
                            int r = (int) Math.min(255, 40 + metalDensity * 215);
                            int g = (int) Math.min(255, 30 + metalDensity * 120);
                            int b = (int) Math.min(255, 20 + metalDensity * 40);
                            pxColor = Color.rgb(r, g, b);
                        }
                    }
                } else if (mode == 2) { // Mantle Heat & Tectonic Map
                    var point = generator.generatePoint(lat, lon, planet);
                    double heatDensity = Math.min(1.0, (mHeat / 150.0) * (0.4 + Math.abs(point.elevation()) / 5000.0));
                    int r = (int) Math.min(255, 30 + heatDensity * 220);
                    int g = (int) Math.min(255, 20 + heatDensity * 90);
                    int b = (int) Math.min(255, 40 + (1.0 - heatDensity) * 100);
                    pxColor = Color.rgb(r, g, b);
                } else { // Aquatic & Aquifer Map
                    var point = generator.generatePoint(lat, lon, planet);
                    if (point.elevation() < 0) {
                        double aquaDensity = Math.min(1.0, (Math.abs(point.elevation()) / 5000.0));
                        int r = (int) Math.min(255, 0 + aquaDensity * 40);
                        int g = (int) Math.min(255, 80 + aquaDensity * 160);
                        int b = (int) Math.min(255, 180 + aquaDensity * 75);
                        pxColor = Color.rgb(r, g, b);
                    } else {
                        double waterTable = Math.min(1.0, (point.rainfall() / 2500.0) * (fAquifer / 15000.0));
                        pxColor = Color.rgb((int)(30 + waterTable * 40), (int)(60 + waterTable * 100), (int)(90 + waterTable * 140));
                    }
                }

                pw.setColor(x, y, pxColor);
            }
        }
    }

    private VBox createSection(Label header, VBox content) {
        header.getStyleClass().add("label-section-header");
        VBox box = new VBox(8, header, content);
        box.getStyleClass().add("card-section");
        return box;
    }

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.valueProperty().addListener((obs, old, val) -> {
            updateSummary();
            updatePreviewCanvas();
        });
        return slider;
    }

    private VBox createControlRow(Label label, javafx.scene.Node control, String tooltipText) {
        label.getStyleClass().add("control-label");
        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            if (control instanceof Control ctrl) {
                ctrl.setTooltip(tt);
            }
        }
        return new VBox(4, label, control);
    }

    private VBox createControlRow(Label label, Slider slider, String formatPattern, String tooltipText) {
        label.getStyleClass().add("control-label");
        Label valLabel = new Label(String.format(formatPattern, slider.getValue()));
        valLabel.getStyleClass().add("value-label");

        slider.valueProperty().addListener((obs, old, val) ->
                valLabel.setText(String.format(formatPattern, val.doubleValue()))
        );

        if (tooltipText != null && !tooltipText.isBlank()) {
            Tooltip tt = new Tooltip(tooltipText);
            label.setTooltip(tt);
            slider.setTooltip(tt);
            valLabel.setTooltip(tt);
        }

        HBox header = new HBox(label, new Pane(), valLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        return new VBox(3, header, slider);
    }

    private void updateSummary() {
        int count = activeCells != null ? activeCells.size() : 0;
        if (summaryLabel == null) return;

        summaryLabel.setText(String.format(
                "🌍 Cellules H3 Actives : %,d\n" +
                "🌲 Végétale : %.0f GtC  |  🌾 Sols : %.0f GtC  |  🦌 Faune : %.2f GtC\n" +
                "⛏️ Métaux : %.0f Gt  |  💎 Précieux : %.0f Mt  |  🌋 Manteau : %.1f mW/m²  |  💧 Aquifères : %.0f x10³ km³",
                count,
                terrestrialBiomassSlider != null ? terrestrialBiomassSlider.getValue() : 450.0,
                soilCarbonSlider != null ? soilCarbonSlider.getValue() : 1500.0,
                faunaBiomassSlider != null ? faunaBiomassSlider.getValue() : 2.0,
                crustalMetalSlider != null ? crustalMetalSlider.getValue() : 80.0,
                preciousMetalSlider != null ? preciousMetalSlider.getValue() : 1200.0,
                mantleHeatSlider != null ? mantleHeatSlider.getValue() : 87.0,
                freshwaterAquiferSlider != null ? freshwaterAquiferSlider.getValue() : 15000.0
        ));
    }

    public void updateTexts() {
        if (headerLabel != null) headerLabel.setText(I18n.getOrDefault("resource.title", "DISTRIBUTION DES RESSOURCES & ÉCOLOGIE (MÉTRIQUES SCIENTIFIQUES)"));
        if (planetSectionHeader != null) planetSectionHeader.setText(I18n.getOrDefault("resource.section.planet_preset", "PLANÈTE & DÉDUCTION SCIENTIFIQUE (ONGLET 1)"));
        if (floraSecHeader != null) floraSecHeader.setText("1. ÉCOLOGIE VÉGÉTALE & SOLS (MÉTRIQUES)");
        if (faunaSecHeader != null) faunaSecHeader.setText("2. BIOMASSE ANIMALE & AQUATIQUE (MÉTRIQUES)");
        if (mineralSecHeader != null) mineralSecHeader.setText("3. GÉOLOGIE & HYDROLOGIE (MÉTRIQUES)");

        if (applyBtn != null) applyBtn.setText(I18n.getOrDefault("resource.btn.apply", "✅ Appliquer la distribution scientifique à la simulation"));

        updateSummary();
    }

    private void applyResourceDistribution() {
        if (activeCells == null || activeCells.isEmpty()) return;

        double totalBiomass = terrestrialBiomassSlider.getValue();
        double totalSoilCarbon = soilCarbonSlider.getValue();
        double totalFaunaBiomass = faunaBiomassSlider.getValue();
        double totalAquaticBiomass = aquaticBiomassSlider.getValue();
        double totalMetals = crustalMetalSlider.getValue();
        double totalPrecious = preciousMetalSlider.getValue();
        double heatFlow = mantleHeatSlider.getValue();
        double totalAquifer = freshwaterAquiferSlider.getValue();

        double cellScale = 1.0 / Math.max(1, activeCells.size());

        activeCells.parallelStream().forEach(cell -> {
            Biome b = cell.getBiome();
            if (b == null) return;

            // Distribute global metrics to individual cell attributes (in tonnes & kg)
            cell.setMantleHeatFlow(heatFlow);
            cell.setFreshwaterAquifer(totalAquifer * cellScale * (cell.getRainfall() / 1000.0));

            switch (b) {
                case FOREST, JUNGLE -> {
                    cell.setBiomassNatural(totalBiomass * cellScale * 3.0);
                    cell.setWoodResource(totalBiomass * cellScale * 2500.0);
                }
                case PLAINS -> {
                    cell.setSoilOrganicCarbon(totalSoilCarbon * cellScale * 2.0);
                    cell.setFoodResource(totalSoilCarbon * cellScale * 1000.0);
                    cell.setBiomassLivestock(totalFaunaBiomass * cellScale * 2.0);
                }
                case MOUNTAINS -> {
                    cell.setResourceMetal(totalMetals * cellScale * 4.0);
                    cell.setResourcePreciousMetal(totalPrecious * cellScale * 5.0);
                }
                case OCEAN, DEEP_OCEAN -> {
                    cell.setBiomassFish(totalAquaticBiomass * cellScale * 5.0);
                    cell.setFoodResource(totalAquaticBiomass * cellScale * 500.0);
                }
                default -> {
                    cell.setBiomassNatural(totalBiomass * cellScale * 0.5);
                }
            }
        });

        // Apply custom map images if loaded
        if (customBiomeImage != null || customResourceImage != null) {
            mapLoader.mapImagesToCells(activeCells, null, customBiomeImage, customResourceImage, -11000, 8848);
        }

        logger.info("Applied scientific ecological resource distribution to {} cells", activeCells.size());
        if (onResourcesAppliedCallback != null) {
            onResourcesAppliedCallback.accept(activeCells);
        }
    }
}
