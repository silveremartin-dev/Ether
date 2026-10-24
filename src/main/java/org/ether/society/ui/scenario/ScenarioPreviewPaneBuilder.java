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
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
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
        legendItemsContainer.getChildren().clear();

        int curSel = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";
        int dims = context.getCultureVectorDimensions();

        Color[] colors;
        String[] labels;
        String[] fullTooltips;

        if (curSel == 0) {
            colors = new Color[]{
                Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
            };
            labels = new String[]{
                I18n.getOrDefault("setup.legend.demo.0", "Inhabité (0)"),
                I18n.getOrDefault("setup.legend.demo.1", "Faible (< 5/km²)"),
                I18n.getOrDefault("setup.legend.demo.2", "Moyen (5-25)"),
                I18n.getOrDefault("setup.legend.demo.3", "Dense (25-100)"),
                I18n.getOrDefault("setup.legend.demo.4", "Métropole (> 100)")
            };
            fullTooltips = new String[]{
                I18n.getOrDefault("setup.legend.demo.0.desc", "Cellules sans présence humaine détectée"),
                I18n.getOrDefault("setup.legend.demo.1.desc", "Communautés de chasseurs-cueilleurs ou pasteurs nomades"),
                I18n.getOrDefault("setup.legend.demo.2.desc", "Villages agricoles et bassins d'agriculture pluviale"),
                I18n.getOrDefault("setup.legend.demo.3.desc", "Grandes vallées alluviales et terroirs d'agriculture intensive"),
                I18n.getOrDefault("setup.legend.demo.4.desc", "Foyers urbains majeurs, cités-États et conurbations denses")
            };
        } else if (curSel >= 1 && curSel <= dims) {
            colors = new Color[]{
                Color.rgb(15, 23, 42), Color.rgb(30, 58, 138), Color.rgb(59, 130, 246), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
            };
            labels = new String[]{
                I18n.getOrDefault("setup.legend.cult.0", "Min (0.0)"),
                I18n.getOrDefault("setup.legend.cult.1", "Faible (0.25)"),
                I18n.getOrDefault("setup.legend.cult.2", "Médian (0.50)"),
                I18n.getOrDefault("setup.legend.cult.3", "Élevé (0.75)"),
                I18n.getOrDefault("setup.legend.cult.4", "Max (1.0)")
            };
            fullTooltips = new String[]{
                I18n.getOrDefault("setup.legend.cult.0.desc", "Intensité nulle ou pôle opposé du trait culturel"),
                I18n.getOrDefault("setup.legend.cult.1.desc", "Faible expression ou pratique culturelle diffuse"),
                I18n.getOrDefault("setup.legend.cult.2.desc", "Valeur médiane, équilibre culturel"),
                I18n.getOrDefault("setup.legend.cult.3.desc", "Expression forte du trait dans la société"),
                I18n.getOrDefault("setup.legend.cult.4.desc", "Polarisation maximale du trait culturel dans la population")
            };
        } else {
            int derivedOffset = curSel - (dims + 1);
            if (derivedOffset == 1 || mode.contains("capital") || mode.contains("k(x)")) {
                colors = new Color[]{
                    Color.rgb(30, 41, 59), Color.rgb(14, 116, 144), Color.rgb(16, 185, 129), Color.rgb(245, 158, 11), Color.rgb(239, 68, 68)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.capital.0", "Rudimentaire (< 50 kg)"),
                    I18n.getOrDefault("setup.legend.capital.1", "Agraire (50-200 kg)"),
                    I18n.getOrDefault("setup.legend.capital.2", "Artisanal (200-500 kg)"),
                    I18n.getOrDefault("setup.legend.capital.3", "Manufacturier (500-2k)"),
                    I18n.getOrDefault("setup.legend.capital.4", "Industriel (> 2 000 kg)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.capital.0.desc", "Outils de pierre et de bois de subsistance"),
                    I18n.getOrDefault("setup.legend.capital.1.desc", "Socles d'araire, céramique, meules dormantes et silos en terre"),
                    I18n.getOrDefault("setup.legend.capital.2.desc", "Forges, moulins à eau, pressoirs et ateliers de tissage"),
                    I18n.getOrDefault("setup.legend.capital.3.desc", "Manufactures royales, hauts fourneaux et canaux de navigation"),
                    I18n.getOrDefault("setup.legend.capital.4.desc", "Machinisme lourd à vapeur, réseau ferré et aciéries")
                };
            } else if (derivedOffset == 2 || mode.contains("énergétique") || mode.contains("energy") || mode.contains("energie") || mode.contains("energético") || mode.contains("e(x)")) {
                colors = new Color[]{
                    Color.rgb(69, 26, 3), Color.rgb(180, 83, 9), Color.rgb(234, 88, 12), Color.rgb(250, 204, 21), Color.rgb(254, 240, 138)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.energy.0", "Biomasse (< 10 MJ/hab)"),
                    I18n.getOrDefault("setup.legend.energy.1", "Traction (10-50 MJ)"),
                    I18n.getOrDefault("setup.legend.energy.2", "Hydraulique/Charbon (50-200)"),
                    I18n.getOrDefault("setup.legend.energy.3", "Fossile (200-500 MJ)"),
                    I18n.getOrDefault("setup.legend.energy.4", "Électrique (> 500 MJ)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.energy.0.desc", "Régime Biomasse : Chaleur du bois et travail musculaire"),
                    I18n.getOrDefault("setup.legend.energy.1.desc", "Régime Traction Animale : Attelages, bœufs et chevaux"),
                    I18n.getOrDefault("setup.legend.energy.2.desc", "Régime Mécanique : Énergie hydraulique, éolienne et débuts du charbon"),
                    I18n.getOrDefault("setup.legend.energy.3.desc", "Régime Hydrocarbures : Pétrole, gaz et thermodynamique industrielle"),
                    I18n.getOrDefault("setup.legend.energy.4.desc", "Régime Électrique Massif : Réseaux de puissance et transition énergétique")
                };
            } else if (derivedOffset == 3 || mode.contains("alimentaires") || mode.contains("food") || mode.contains("nahrung") || mode.contains("alimentos") || mode.contains("f(x)")) {
                colors = new Color[]{
                    Color.rgb(163, 230, 53), Color.rgb(132, 204, 22), Color.rgb(34, 197, 94), Color.rgb(5, 150, 105), Color.rgb(6, 78, 59)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.food.0", "< 1 Mois (Critique)"),
                    I18n.getOrDefault("setup.legend.food.1", "1–3 Mois (Faible)"),
                    I18n.getOrDefault("setup.legend.food.2", "3–6 Mois (Moyen)"),
                    I18n.getOrDefault("setup.legend.food.3", "6–12 Mois (Sécurisé)"),
                    I18n.getOrDefault("setup.legend.food.4", "> 12 Mois (Abondance)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.food.0.desc", "Réserves Critiques : Vulnérabilité immédiate à la moindre mauvaise récolte"),
                    I18n.getOrDefault("setup.legend.food.1.desc", "Réserves de Subsistance : Stocks saisonniers d'appoint"),
                    I18n.getOrDefault("setup.legend.food.2.desc", "Greniers Traditionnels : Capacité de soudure inter-annuelle"),
                    I18n.getOrDefault("setup.legend.food.3.desc", "Réserves Stratégiques : Silos régionaux prévenant toute disette"),
                    I18n.getOrDefault("setup.legend.food.4.desc", "Surplus Systémique : Chaînes logistiques agroalimentaires pérennes")
                };
            } else if (derivedOffset == 4 || mode.contains("informationnel") || mode.contains("info") || mode.contains("i(x)")) {
                colors = new Color[]{
                    Color.rgb(55, 48, 163), Color.rgb(124, 58, 237), Color.rgb(219, 39, 119), Color.rgb(6, 182, 212), Color.rgb(224, 242, 254)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.info.0", "Oral (< 10 bits/hab)"),
                    I18n.getOrDefault("setup.legend.info.1", "Écrit & Parchemin (10-100)"),
                    I18n.getOrDefault("setup.legend.info.2", "Imprimerie (100-1k)"),
                    I18n.getOrDefault("setup.legend.info.3", "Médias Masse (1k-10k)"),
                    I18n.getOrDefault("setup.legend.info.4", "Numérique (> 10k)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.info.0.desc", "Tradition Orale : Savoirs transmis par la mémoire et le chant"),
                    I18n.getOrDefault("setup.legend.info.1.desc", "Écrit & Manuscrits : Enregistrement sur argile, papyrus et parchemins"),
                    I18n.getOrDefault("setup.legend.info.2.desc", "Imprimerie Mécanique : Diffusion élargie des traités et encyclopédies"),
                    I18n.getOrDefault("setup.legend.info.3.desc", "Télécommunications : Presse quotidienne, télégraphe, radio et cinéma"),
                    I18n.getOrDefault("setup.legend.info.4.desc", "Société Numérique : Internet, calcul haute performance et bases de données")
                };
            } else if (derivedOffset == 5 || mode.contains("empreinte") || mode.contains("footprint") || mode.contains("fußabdruck") || mode.contains("huella") || mode.contains("malthus")) {
                colors = new Color[]{
                    Color.rgb(16, 185, 129), Color.rgb(132, 204, 22), Color.rgb(245, 158, 11), Color.rgb(239, 68, 68), Color.rgb(127, 29, 29)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0", "Biome Vierge (C₇=0)"),
                    I18n.getOrDefault("setup.legend.eco.1", "Pression Modérée (0.25)"),
                    I18n.getOrDefault("setup.legend.eco.2", "Surexploitation (0.50)"),
                    I18n.getOrDefault("setup.legend.eco.3", "Tension Critique (0.75)"),
                    I18n.getOrDefault("setup.legend.eco.4", "Effondrement (1.0)")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.eco.0.desc", "Biome Intact : Écosystèmes à l'équilibre sans perturbation anthropique"),
                    I18n.getOrDefault("setup.legend.eco.1.desc", "Pression Légère : Forêt gérée, chasse et élevage durable"),
                    I18n.getOrDefault("setup.legend.eco.2.desc", "Surexploitation Agricole : Déforestation, érosion des sols, baisse des rendements"),
                    I18n.getOrDefault("setup.legend.eco.3.desc", "Tension Malthusienne Sévère : Pénuries de bois, raréfaction du gibier et des nappes"),
                    I18n.getOrDefault("setup.legend.eco.4.desc", "Effondrement Écologique : Désertification irréversible et disette systémique")
                };
            } else if (derivedOffset == 6 || mode.contains("friction") || mode.contains("reibung") || mode.contains("fricción")) {
                colors = new Color[]{
                    Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68), Color.rgb(167, 139, 250)
                };
                labels = new String[]{
                    I18n.getOrDefault("setup.legend.friction.0", "Plaine (Faible σ)"),
                    I18n.getOrDefault("setup.legend.friction.1", "Colline / Fleuve (Moyen)"),
                    I18n.getOrDefault("setup.legend.friction.2", "Montagne (Fort)"),
                    I18n.getOrDefault("setup.legend.friction.3", "Désert / Extrême"),
                    I18n.getOrDefault("setup.legend.friction.4", "Barrière Absolue")
                };
                fullTooltips = new String[]{
                    I18n.getOrDefault("setup.legend.friction.0.desc", "Plaine Alluviale : Friction minimale à la mobilité (σ ≈ 0.1)"),
                    I18n.getOrDefault("setup.legend.friction.1.desc", "Colline / Fleuve : Obstacle naturel mineur franchissable"),
                    I18n.getOrDefault("setup.legend.friction.2.desc", "Chaîne Montagneuse : Transports ralentis, cols escarpés"),
                    I18n.getOrDefault("setup.legend.friction.3.desc", "Désert Extrême : Zone aride exigeant des convois spécialisés"),
                    I18n.getOrDefault("setup.legend.friction.4.desc", "Haute Altitude / Falaise : Barrière infranchissable pour les armées")
                };
            } else {
                colors = new Color[]{
                    Color.rgb(30, 95, 165), Color.rgb(16, 185, 129), Color.rgb(234, 179, 8), Color.rgb(249, 115, 22), Color.rgb(239, 68, 68)
                };
                labels = new String[]{"0.0", "0.25", "0.50", "0.75", "1.0"};
                fullTooltips = new String[]{"0.0", "0.25", "0.50", "0.75", "1.0"};
            }
        }

        for (int i = 0; i < labels.length; i++) {
            javafx.scene.shape.Rectangle colorBox = new javafx.scene.shape.Rectangle(14, 10);
            colorBox.setFill(colors[i]);
            colorBox.setStroke(Color.GRAY);
            colorBox.setArcWidth(3);
            colorBox.setArcHeight(3);

            Label lbl = new Label(labels[i]);
            lbl.getStyleClass().add("control-label");
            lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");

            HBox item = new HBox(4, colorBox, lbl);
            item.setAlignment(Pos.CENTER);
            Tooltip.install(item, new Tooltip(fullTooltips[i]));
            legendItemsContainer.getChildren().add(item);
        }
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
        if (img == null || img.getWidth() <= 1 || img.getHeight() <= 1) return null;
        PixelReader reader = img.getPixelReader();
        if (reader == null) return null;

        int px = (int) Math.clamp(((lon + 180.0) / 360.0) * img.getWidth(), 0, img.getWidth() - 1);
        int py = (int) Math.clamp(((90.0 - lat) / 180.0) * img.getHeight(), 0, img.getHeight() - 1);
        return reader.getColor(px, py);
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
            drawInstant2DDensityPreview(gc, w, h, scale, offX, offY);
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

    private Color blendColors(Color base, Color overlay, double opacity) {
        if (base == null) return overlay;
        if (overlay == null) return base;
        double r = base.getRed() * (1.0 - opacity) + overlay.getRed() * opacity;
        double g = base.getGreen() * (1.0 - opacity) + overlay.getGreen() * opacity;
        double b = base.getBlue() * (1.0 - opacity) + overlay.getBlue() * opacity;
        return Color.color(Math.clamp(r, 0.0, 1.0), Math.clamp(g, 0.0, 1.0), Math.clamp(b, 0.0, 1.0));
    }

    private void drawInstant2DDensityPreview(GraphicsContext gc, double w, double h, double scale, double offX, double offY) {
        int pwWidth = 320;
        int pwHeight = 160;

        PlanetPreset planet = context.getActivePlanetPreset() != null ? context.getActivePlanetPreset() : PlanetPreset.EARTH_LIKE;
        Image bgImage = null;
        if (planet.customElevBase64() != null && !planet.customElevBase64().isBlank()) {
            bgImage = org.ether.society.data.ImageMapLoader.base64PngToImage(planet.customElevBase64());
        } else if (planet.elevationUseImport()) {
            bgImage = org.ether.society.data.ImageMapLoader.loadMapImage(planet.elevationMapSource(), planet.getAssociatedEpochYear(), "elevation.png");
        }
        if (bgImage == null && "earth".equalsIgnoreCase(planet.elevationMapSource())) {
            bgImage = context.getElevationImageForPreset(planet);
        }
        PixelReader bgReader = bgImage != null ? bgImage.getPixelReader() : null;

        int idx = previewModeCombo != null ? previewModeCombo.getSelectionModel().getSelectedIndex() : 0;
        String mode = previewModeCombo != null && previewModeCombo.getValue() != null ? previewModeCombo.getValue().toLowerCase() : "";
        int dims = context.getCultureVectorDimensions();
        Image activeCustomImage = null;
        boolean isImportActive = false;
        if (idx >= 1 && idx <= dims) {
            int tIndex = idx - 1;
            isImportActive = context.isTensorImportActive(tIndex);
            activeCustomImage = context.getCustomTensorImage(tIndex);
        } else if (idx == 0) {
            isImportActive = context.isDensityImportActive();
            activeCustomImage = context.getCustomDensityImage();
        }

        PixelReader customReader = activeCustomImage != null ? activeCustomImage.getPixelReader() : null;
        boolean isFootprintMode = idx == 6 || mode.contains("empreinte");
        boolean isReliefOverlay = btnReliefOverlay != null && btnReliefOverlay.isSelected();

        String cacheKey = "sc_mode=" + idx + "_w=" + pwWidth + "_h=" + pwHeight + "_relief=" + isReliefOverlay
                + "_import=" + isImportActive
                + "_pName=" + (planet != null ? planet.name() : "")
                + "_startYear=" + context.getStartYear()
                + "_pop=" + context.getInitialHumanCount()
                + "_pattern=" + context.getDensityPattern()
                + "_demoSeed=" + context.getDemoSeed()
                + "_cultSeed=" + context.getCultSeed()
                + "_hasCustomDensity=" + (context.getCustomDensityImage() != null);

        WritableImage img = proceduralImageCache.get(cacheKey);
        if (img == null) {
            img = new WritableImage(pwWidth, pwHeight);
            PixelWriter writer = img.getPixelWriter();
            ProceduralGenerator generator = new ProceduralGenerator();

            double minAlt = planet != null ? planet.minAltitudeMeters() : -11000.0;
            double maxAlt = planet != null ? planet.maxAltitudeMeters() : 8848.0;
            double wLevel = planet != null ? planet.waterLevel() : 0.478;
            boolean hasOcean = wLevel > -0.4;

            double cutThreshold;
            if (hasOcean) {
                cutThreshold = Math.clamp(wLevel, 0.01, 0.99);
            } else {
                double altRange = Math.max(100.0, maxAlt - minAlt);
                cutThreshold = Math.clamp((-minAlt) / altRange, 0.05, 0.95);
            }

            for (int py = 0; py < pwHeight; py++) {
                for (int px = 0; px < pwWidth; px++) {
                    double lat = 90.0 - (py / (double) pwHeight) * 180.0;
                    double lon = -180.0 + (px / (double) pwWidth) * 360.0;

                    double elevVal;
                    boolean isLand;
                    boolean isCoast;
                    double hillshade;

                    if (bgReader != null && bgImage != null) {
                        double bgW = bgImage.getWidth();
                        double bgH = bgImage.getHeight();
                        int bx = (int) Math.clamp(((px / (double) pwWidth) * bgW), 0, bgW - 1);
                        int by = (int) Math.clamp(((py / (double) pwHeight) * bgH), 0, bgH - 1);
                        elevVal = bgReader.getColor(bx, by).getRed();
                        boolean isAboveCut = elevVal >= cutThreshold;
                        isLand = hasOcean ? isAboveCut : true;

                        int bxE = (bx + 1) % (int) bgW;
                        int bxW = (bx - 1 + (int) bgW) % (int) bgW;
                        int byN = Math.max(0, by - 1);
                        int byS = Math.min((int) bgH - 1, by + 1);

                        double eE = bgReader.getColor(bxE, by).getRed();
                        double eW = bgReader.getColor(bxW, by).getRed();
                        double eN = bgReader.getColor(bx, byN).getRed();
                        double eS = bgReader.getColor(bx, byS).getRed();

                        isCoast = (isAboveCut != (eE >= cutThreshold)) || (isAboveCut != (eW >= cutThreshold))
                                || (isAboveCut != (eN >= cutThreshold)) || (isAboveCut != (eS >= cutThreshold));

                        double dLng = (eE - eW) * 16.0;
                        double dLat = (eN - eS) * 16.0;
                        hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                    } else {
                        var pt = generator.getPlanetPoint(lat, lon, planet);
                        elevVal = pt.elevation();

                        double dDeg = 0.5;
                        var ptE = generator.getPlanetPoint(lat, lon + dDeg, planet);
                        var ptW = generator.getPlanetPoint(lat, lon - dDeg, planet);
                        var ptN = generator.getPlanetPoint(lat + dDeg, lon, planet);
                        var ptS = generator.getPlanetPoint(lat - dDeg, lon, planet);

                        if (hasOcean) {
                            isLand = elevVal >= wLevel;
                            isCoast = (isLand != (ptE.elevation() >= wLevel)) || (isLand != (ptW.elevation() >= wLevel))
                                    || (isLand != (ptN.elevation() >= wLevel)) || (isLand != (ptS.elevation() >= wLevel));
                        } else {
                            isLand = true;
                            isCoast = Math.abs(elevVal) < 350.0;
                        }

                        double dLng = (ptE.elevation() - ptW.elevation()) * 4.0;
                        double dLat = (ptN.elevation() - ptS.elevation()) * 4.0;
                        hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                    }

                    Color baseReliefColor;
                    if (!isLand) {
                        double depth = Math.clamp((cutThreshold - elevVal) / Math.max(0.01, cutThreshold), 0.0, 1.0);
                        baseReliefColor = blendColors(Color.rgb(15, 23, 42), Color.rgb(30, 58, 138), 1.0 - depth);
                    } else {
                        double landNorm = Math.clamp((elevVal - cutThreshold) / Math.max(0.01, 1.0 - cutThreshold), 0.0, 1.0);
                        if (landNorm < 0.20) {
                            double t = landNorm / 0.20;
                            baseReliefColor = Color.rgb((int)(34 + 60 * t), (int)(139 + 30 * t), (int)(34 + 20 * t));
                        } else if (landNorm < 0.40) {
                            double t = (landNorm - 0.20) / 0.20;
                            baseReliefColor = Color.rgb((int)(94 + 86 * t), (int)(169 + 16 * t), (int)(54 + 21 * t));
                        } else if (landNorm < 0.65) {
                            double t = (landNorm - 0.40) / 0.25;
                            baseReliefColor = Color.rgb((int)(180 - 10 * t), (int)(185 - 65 * t), (int)(75 - 15 * t));
                        } else if (landNorm < 0.85) {
                            double t = (landNorm - 0.65) / 0.20;
                            baseReliefColor = Color.rgb((int)(170 - 20 * t), (int)(120 + 40 * t), (int)(60 + 80 * t));
                        } else {
                            double t = (landNorm - 0.85) / 0.15;
                            baseReliefColor = Color.rgb((int)(150 + 95 * t), (int)(160 + 90 * t), (int)(140 + 115 * t));
                        }
                    }

                    Color pxColor;
                    if (idx >= 1 && idx <= dims) {
                        int tIndex = idx - 1;
                        if (isImportActive) {
                            if (customReader != null && activeCustomImage.getWidth() > 0 && activeCustomImage.getHeight() > 0) {
                                int imgX = (int) Math.clamp(((px / (double) pwWidth) * activeCustomImage.getWidth()), 0, activeCustomImage.getWidth() - 1);
                                int imgY = (int) Math.clamp(((py / (double) pwHeight) * activeCustomImage.getHeight()), 0, activeCustomImage.getHeight() - 1);
                                pxColor = customReader.getColor(imgX, imgY);
                            } else {
                                pxColor = Color.BLACK;
                            }
                        } else {
                            double p1 = context.getTensorParam1(tIndex);
                            double p2 = context.getTensorParam2(tIndex);
                            double p3 = context.getTensorParam3(tIndex);
                            long seed = context.getTensorSeed(tIndex);
                            pxColor = context.evaluateProceduralTensorColor(tIndex, lat, lon, elevVal, p1, p2, p3, seed);
                        }
                    } else if (isImportActive) {
                        if (customReader != null && activeCustomImage.getWidth() > 0 && activeCustomImage.getHeight() > 0) {
                            int imgX = (int) Math.clamp(((px / (double) pwWidth) * activeCustomImage.getWidth()), 0, activeCustomImage.getWidth() - 1);
                            int imgY = (int) Math.clamp(((py / (double) pwHeight) * activeCustomImage.getHeight()), 0, activeCustomImage.getHeight() - 1);
                            pxColor = customReader.getColor(imgX, imgY);
                            if (!isLand) {
                                pxColor = baseReliefColor;
                            }
                        } else {
                            pxColor = Color.BLACK;
                        }
                    } else if (!isLand) {
                        pxColor = baseReliefColor;
                    } else {
                        long startYear = context.getStartYear();
                        boolean isAmericas = lon < -25.0;
                        boolean isSahul = (lat < 10.0 && lon > 95.0) || (lat < -10.0 && lon > 110.0);

                        boolean isHumanSettled = true;
                        if (startYear <= -50000 && (isAmericas || isSahul)) {
                            isHumanSettled = false;
                        } else if (startYear <= -25000 && isAmericas) {
                            isHumanSettled = false;
                        }

                        if (!isHumanSettled) {
                            pxColor = blendColors(baseReliefColor, Color.rgb(30, 95, 165), 0.35);
                        } else {
                            boolean isEastAfrica = (lat >= -15 && lat <= 15) && (lon >= 25 && lon <= 45);
                            boolean isFertileCrescent = (lat >= 20 && lat <= 38) && (lon >= 25 && lon <= 90);
                            boolean isChinaIndus = (lat >= 10 && lat <= 42) && (lon >= 65 && lon <= 125);

                            if (isEastAfrica || isFertileCrescent || isChinaIndus) {
                                Color heatCol = isFootprintMode ? Color.rgb(239, 68, 68) : Color.rgb(249, 115, 22);
                                pxColor = blendColors(baseReliefColor, heatCol, 0.70);
                            } else {
                                pxColor = blendColors(baseReliefColor, Color.rgb(16, 185, 129), 0.50);
                            }
                        }
                    }

                    if (isReliefOverlay && pxColor != Color.BLACK) {
                        if (isCoast) {
                            pxColor = hasOcean ? Color.rgb(240, 249, 255) : Color.rgb(251, 191, 36);
                        } else {
                            double mult = 0.68 + 0.45 * hillshade;
                            pxColor = Color.color(
                                Math.clamp(pxColor.getRed() * mult, 0.0, 1.0),
                                Math.clamp(pxColor.getGreen() * mult, 0.0, 1.0),
                                Math.clamp(pxColor.getBlue() * mult, 0.0, 1.0)
                            );
                        }
                    }

                    writer.setColor(px, py, pxColor != null ? pxColor : Color.BLACK);
                }
            }
            proceduralImageCache.put(cacheKey, img);
        }

        double drawW = 360.0 * scale;
        double drawH = 180.0 * scale;
        gc.drawImage(img, offX, offY, drawW, drawH);

        gc.setFill(Color.rgb(56, 189, 248, 0.95));
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 12));
        gc.fillText("⚡ Aperçu 2D dynamique instantané (" + (mode.isEmpty() ? "Relief/Densité" : mode.toUpperCase()) + ")...", 20, h - 15);
    }

    private void exportPreviewImage() {
        if (previewCanvas == null) return;
        try {
            javafx.scene.image.WritableImage wImg = new javafx.scene.image.WritableImage((int) previewCanvas.getWidth(), (int) previewCanvas.getHeight());
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
