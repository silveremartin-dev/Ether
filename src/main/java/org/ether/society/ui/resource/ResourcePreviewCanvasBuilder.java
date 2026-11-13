/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowHelper;

/**
 * Cartographic Preview Canvas Controller and View Renderer for Resource Distribution.
 *
 * <p>Extracted from {@code ResourceDistributionPanel} to satisfy the Single Responsibility Principle (SRP).
 * Responsible for:
 * <ul>
 *   <li>Interactive pan and multi-resolution zoom (\( 0.5\times \dots 20.0\times \)) over equirectangular projections.</li>
 *   <li>Relief hillshading computation:
 *       $$\text{Hillshade} = \frac{0.5 \cdot \Delta\text{Lng} + 0.5 \cdot \Delta\text{Lat} + 0.707}{\sqrt{\Delta\text{Lng}^2 + \Delta\text{Lat}^2 + 1.0}}$$</li>
 *   <li>Dynamic rendering across 16 thematic cartographic layers:
 *       Biomes, Hydrography, 10 Geological Tensors, Thermal flux, Seismic hazard, and Aridity index.</li>
 *   <li>High-contrast localized legend generation and color-swatch rendering.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class ResourcePreviewCanvasBuilder {

    /**
     * Host interface supplying active planetary context, custom image rasters, and geological builder access.
     */
    public interface HostContext {
        PlanetPreset getActivePlanetPreset();
        double getSeismicActivityValue();
        double getVolcanicActivityValue();
        boolean isRadioImportBiomeSelected();
        Image getCustomBiomeImage();
        boolean isRadioImportHydroSelected();
        Image getCustomHydroImage();
        ResourceGeologySectionBuilder getGeologySectionBuilder();
        ProceduralGenerator getProceduralGenerator();
        ImageMapLoader getMapLoader();
        Node getHostNode();
    }

    private final HostContext host;

    // Cartographic & View Mode Controls
    private Label rightViewTitle;
    private ComboBox<String> viewModeCombo;
    private ToggleButton btnReliefOverlay;
    private Canvas mapPreviewCanvas;
    private HBox legendBar;
    private Label summaryLabel;
    private StackPane canvasContainer;

    // Interactive Zoom / Pan State
    private double zoomFactor = 1.0;
    private double panX = 0.0;
    private double panY = 0.0;
    private double dragStartX = 0.0;
    private double dragStartY = 0.0;

    /**
     * Constructs the cartographic preview builder bound to the given host context.
     *
     * @param host the host context interface
     */
    public ResourcePreviewCanvasBuilder(HostContext host) {
        this.host = host;
    }

    /**
     * Builds and returns the right-hand preview panel containing view selector, relief toggle,
     * interactive canvas, legend bar, and summary display.
     *
     * @return the fully configured preview VBox layout
     */
    public VBox buildPreviewPane() {
        VBox centerBox = new VBox(8);
        centerBox.setPadding(new Insets(10));
        centerBox.setStyle("-fx-background-color: transparent;");

        rightViewTitle = new Label(I18n.getOrDefault("resource.title.right_view", "Dynamic 2D Equirectangular Planetary Preview"));
        rightViewTitle.getStyleClass().add("label-header");

        // View Mode Selector
        viewModeCombo = new ComboBox<>();
        populateViewModeCombo();
        viewModeCombo.setValue(viewModeCombo.getItems().get(1));
        viewModeCombo.setMaxWidth(380);
        viewModeCombo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                } else if (item.startsWith("â”€â”€â”€â”€â”€â”€")) {
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
        viewModeCombo.setOnAction(e -> {
            String val = viewModeCombo.getValue();
            if (val != null && val.startsWith("â”€â”€â”€â”€â”€â”€")) {
                viewModeCombo.setValue(viewModeCombo.getItems().get(1));
                return;
            }
            Node hostNode = host.getHostNode();
            if (hostNode != null) WindowHelper.setBusyCursor(hostNode, true);
            try {
                updateLegend();
                updatePreviewCanvas();
            } finally {
                if (hostNode != null) WindowHelper.setBusyCursor(hostNode, false);
            }
        });

        btnReliefOverlay = new ToggleButton(I18n.getOrDefault("resource.btn.relief_overlay", "â›ï¸ Relief"));
        btnReliefOverlay.setSelected(false);
        btnReliefOverlay.getStyleClass().add("button-secondary");
        btnReliefOverlay.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.relief_overlay", "Superposer l'ombrage du relief topographique et des pentes avec délimitation du trait de côte.")));
        btnReliefOverlay.setOnAction(e -> updatePreviewCanvas());

        HBox viewModeControlBar = new HBox(8, viewModeCombo, btnReliefOverlay);
        viewModeControlBar.setAlignment(Pos.CENTER);

        canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-background-color: black; -fx-border-color: #475569; -fx-border-radius: 6; -fx-background-radius: 6;");

        mapPreviewCanvas = new Canvas(640, 360);
        mapPreviewCanvas.getStyleClass().add("map-canvas-shadow");
        Tooltip.install(mapPreviewCanvas, new Tooltip(I18n.getOrDefault("resource.tooltip.preview", "Dynamic preview of geographical resource distribution")));

        // Clip container to prevent JavaFX Prism NGCanvas renderForClip 0x0 NPE
        javafx.scene.shape.Rectangle containerClip = new javafx.scene.shape.Rectangle();
        containerClip.widthProperty().bind(canvasContainer.widthProperty());
        containerClip.heightProperty().bind(canvasContainer.heightProperty());
        canvasContainer.setClip(containerClip);

        canvasContainer.widthProperty().addListener((obs, oldV, newV) -> {
            double w = Math.floor(newV.doubleValue());
            if (w >= 10.0 && Math.abs(w - mapPreviewCanvas.getWidth()) >= 4.0) {
                mapPreviewCanvas.setWidth(w);
                updatePreviewCanvas();
            }
        });
        canvasContainer.heightProperty().addListener((obs, oldV, newV) -> {
            double h = Math.floor(newV.doubleValue());
            if (h >= 10.0 && Math.abs(h - mapPreviewCanvas.getHeight()) >= 4.0) {
                mapPreviewCanvas.setHeight(h);
                updatePreviewCanvas();
            }
        });

        // Interactive Zoom & Pan Handlers
        mapPreviewCanvas.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double factor = delta > 0 ? 1.15 : 0.85;
            double oldZoom = zoomFactor;
            double newZoom = Math.max(0.5, Math.min(20.0, oldZoom * factor));

            if (newZoom != oldZoom) {
                double mouseX = e.getX();
                double mouseY = e.getY();
                double w = mapPreviewCanvas.getWidth();
                double h = mapPreviewCanvas.getHeight();
                double baseScale = Math.min(w / 360.0, h / 180.0) * 0.9;
                double oldScale = baseScale * oldZoom;
                double newScale = baseScale * newZoom;

                if (oldScale > 0 && newScale > 0) {
                    double mouseLng = (mouseX - w / 2.0 - panX) / oldScale;
                    double mouseLat = (h / 2.0 + panY - mouseY) / oldScale;

                    zoomFactor = newZoom;
                    panX = mouseX - w / 2.0 - mouseLng * newScale;
                    panY = mouseY - h / 2.0 + mouseLat * newScale;
                } else {
                    zoomFactor = newZoom;
                }
                updatePreviewCanvas();
            }
        });
        mapPreviewCanvas.setOnMousePressed(e -> {
            dragStartX = e.getX();
            dragStartY = e.getY();
        });
        mapPreviewCanvas.setOnMouseDragged(e -> {
            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;
            panX += dx;
            panY += dy;
            dragStartX = e.getX();
            dragStartY = e.getY();
            updatePreviewCanvas();
        });
        mapPreviewCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                zoomFactor = 1.0;
                panX = 0.0;
                panY = 0.0;
                updatePreviewCanvas();
            }
        });

        canvasContainer.getChildren().add(mapPreviewCanvas);
        VBox.setVgrow(canvasContainer, Priority.ALWAYS);

        legendBar = new HBox(10);
        legendBar.setAlignment(Pos.CENTER);
        legendBar.setPadding(new Insets(5, 10, 5, 10));
        legendBar.getStyleClass().add("card-section");
        updateLegend();

        summaryLabel = new Label(I18n.getOrDefault("resource.prompt.load_cells", "🌍 Generate or load planet cells to activate map."));
        summaryLabel.getStyleClass().addAll("control-label", "summary-centered-label");
        summaryLabel.setWrapText(true);

        centerBox.getChildren().addAll(rightViewTitle, viewModeControlBar, canvasContainer, legendBar, summaryLabel);
        VBox.setVgrow(centerBox, Priority.ALWAYS);

        return centerBox;
    }

    private void populateViewModeCombo() {
        if (viewModeCombo == null) return;
        viewModeCombo.getItems().setAll(
                I18n.getOrDefault("resource.view.section_base", "â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ 12 CARTES CANONIQUES DE BASE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€"),
                I18n.getOrDefault("resource.view.biomes", "🌿 1. Biomes & Couverture Végétale"),
                I18n.getOrDefault("resource.view.hydro", " 2. Hydrographie & Réseau Fluvial"),
                I18n.getOrDefault("resource.view.coal", "â›ï¸ 3. Gisements de Charbon"),
                I18n.getOrDefault("resource.view.oil", "¸ 4. Réserves de Pétrole Brut"),
                I18n.getOrDefault("resource.view.gas", "🔥 5. Champs de Gaz Naturel"),
                I18n.getOrDefault("resource.view.uranium", "â˜¢ï¸ 6. Minerais d'Uranium & Fission"),
                I18n.getOrDefault("resource.view.helium3", " 7. Hélium-3 & Fusion Lunaires"),
                I18n.getOrDefault("resource.view.iron_copper", "â›“ï¸ 8. Métaux Fer BIF & Cuivre"),
                I18n.getOrDefault("resource.view.precious_metals", " 9. Métaux Précieux (Or, Argent, PGM)"),
                I18n.getOrDefault("resource.view.rare_earths", " 10. Terres Rares & Minéraux Critiques / Lithium"),
                I18n.getOrDefault("resource.view.heat", " 11. Flux Thermique du Manteau"),
                I18n.getOrDefault("resource.view.aquifer", " 12. Aquifères & Eau Douce"),
                I18n.getOrDefault("resource.view.section_derived", "â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ CARTES DÉDUITES / ANOMALIES â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€"),
                I18n.getOrDefault("resource.view.temp", "¸ 13. Températures Surface & Microclimats"),
                I18n.getOrDefault("resource.view.seismic", " 14. Tectonique & Aléa Sismique/Volcanique"),
                I18n.getOrDefault("resource.view.aridity", "¸ 15. Aridité & Salinisation des Sols")
        );
    }

    /**
     * Renders the thematic resource raster onto the 2D equirectangular preview canvas.
     */
    public void updatePreviewCanvas() {
        if (mapPreviewCanvas == null) return;
        int canvasW = (int) mapPreviewCanvas.getWidth();
        int canvasH = (int) mapPreviewCanvas.getHeight();
        if (canvasW < 1 || canvasH < 1) return;

        boolean isRelief = btnReliefOverlay != null && btnReliefOverlay.isSelected();
        WritableImage buffer = ResourcePreviewPixelRenderer.renderPreviewBuffer(
                host, getViewModeIndex(), isRelief, zoomFactor, panX, panY);
        GraphicsContext gc = mapPreviewCanvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.drawImage(buffer, 0, 0, canvasW, canvasH);
    }

    /**
     * Updates the horizontal color legend bar matching the active view mode.
     */
    public void updateLegend() {
        ResourcePreviewLegendBuilder.updateLegend(legendBar, getViewModeIndex());
    }

    public Color blendColors(Color base, Color overlay, double opacity) {
        return ResourcePreviewPixelRenderer.blendColors(base, overlay, opacity);
    }

    public Color getReliefShadeColor(double elevation, double waterLevel, double declivity) {
        return ResourcePreviewPixelRenderer.getReliefShadeColor(elevation, waterLevel, declivity);
    }

    /**
     * Returns the combo box selection index corresponding to a geological layer index (0..9).
     *
     * @param layerIdx the geological layer index
     * @return the view mode combo index
     */
    public int getGeologyViewComboIndex(int layerIdx) {
        return switch (layerIdx) {
            case 0 -> 3;  // COAL
            case 1 -> 4;  // OIL
            case 2 -> 5;  // GAS
            case 3 -> 6;  // URANIUM
            case 4 -> 7;  // HELIUM_3
            case 5 -> 8;  // IRON_COPPER
            case 6 -> 9;  // PRECIOUS_METALS
            case 7 -> 10; // CRITICAL_REE
            case 8 -> 11; // GEOTHERMAL (Heat Flux)
            case 9 -> 12; // FRESHWATER_AQUIFERS
            default -> 1;
        };
    }

    /**
     * Translates the combo box selected index into a thematic mode index (0..14).
     *
     * @return the thematic mode index
     */
    public int getViewModeIndex() {
        if (viewModeCombo == null) return 0;
        int selected = viewModeCombo.getSelectionModel().getSelectedIndex();
        return switch (selected) {
            case 1 -> 0;   // 1. Biomes
            case 2 -> 1;   // 2. Hydrography
            case 3 -> 2;   // 3. Coal (Geology layer 0)
            case 4 -> 3;   // 4. Oil (Geology layer 1)
            case 5 -> 4;   // 5. Gas (Geology layer 2)
            case 6 -> 5;   // 6. Uranium (Geology layer 3)
            case 7 -> 6;   // 7. Helium-3 (Geology layer 4)
            case 8 -> 7;   // 8. Iron & Copper (Geology layer 5)
            case 9 -> 8;   // 9. Precious metals (Geology layer 6)
            case 10 -> 9;  // 10. Rare Earths & Critical Minerals (Geology layer 7)
            case 11 -> 10; // 11. Mantle Heat Flux (Geology layer 8)
            case 12 -> 11; // 12. Aquifers (Geology layer 9)
            case 14 -> 12; // 13. Surface Temp & Microclimates
            case 15 -> 13; // 14. Seismic & Volcanic Tectonism
            case 16 -> 14; // 15. Aridity & Soil Salinization
            default -> 0;
        };
    }

    /**
     * Programmatically selects the view mode index.
     *
     * @param viewModeIndex the combo box view mode index
     */
    public void selectViewMode(int viewModeIndex) {
        if (viewModeCombo != null && viewModeIndex >= 0 && viewModeIndex < viewModeCombo.getItems().size()) {
            viewModeCombo.getSelectionModel().select(viewModeIndex);
        }
    }

    /**
     * Updates localized texts for title, relief toggle, view modes combo, and legend.
     */
    public void updateTexts() {
        if (rightViewTitle != null) {
            rightViewTitle.setText(I18n.getOrDefault("resource.title.right_view", "Dynamic 2D Equirectangular Planetary Preview"));
        }
        if (btnReliefOverlay != null) {
            btnReliefOverlay.setText(I18n.getOrDefault("resource.btn.relief_overlay", "â›ï¸ Relief"));
            btnReliefOverlay.setTooltip(new Tooltip(I18n.getOrDefault("resource.tooltip.relief_overlay", "Superposer l'ombrage du relief topographique et des pentes avec délimitation du trait de côte.")));
        }
        if (viewModeCombo != null) {
            int selected = viewModeCombo.getSelectionModel().getSelectedIndex();
            populateViewModeCombo();
            if (selected >= 0 && selected < viewModeCombo.getItems().size()) {
                viewModeCombo.getSelectionModel().select(selected);
            } else {
                viewModeCombo.getSelectionModel().select(1);
            }
        }
        updateLegend();
    }

    public ComboBox<String> getViewModeCombo() {
        return viewModeCombo;
    }

    public Canvas getMapPreviewCanvas() {
        return mapPreviewCanvas;
    }

    public Label getSummaryLabel() {
        return summaryLabel;
    }

    public void setSummaryText(String text) {
        if (summaryLabel != null) {
            summaryLabel.setText(text);
        }
    }
}

