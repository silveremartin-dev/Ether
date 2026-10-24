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
import org.ether.society.ui.WindowUtils;

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
 * @version 1.0.0-beta.2
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
                } else if (item.startsWith("──────")) {
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
            if (val != null && val.startsWith("──────")) {
                viewModeCombo.setValue(viewModeCombo.getItems().get(1));
                return;
            }
            Node hostNode = host.getHostNode();
            if (hostNode != null) WindowUtils.setBusyCursor(hostNode, true);
            try {
                updateLegend();
                updatePreviewCanvas();
            } finally {
                if (hostNode != null) WindowUtils.setBusyCursor(hostNode, false);
            }
        });

        btnReliefOverlay = new ToggleButton(I18n.getOrDefault("resource.btn.relief_overlay", "⛏️ Relief"));
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
                I18n.getOrDefault("resource.view.section_base", "────────── 12 CARTES CANONIQUES DE BASE ──────────"),
                I18n.getOrDefault("resource.view.biomes", "🌿 1. Biomes & Couverture Végétale"),
                I18n.getOrDefault("resource.view.hydro", "🌊 2. Hydrographie & Réseau Fluvial"),
                I18n.getOrDefault("resource.view.coal", "⛏️ 3. Gisements de Charbon"),
                I18n.getOrDefault("resource.view.oil", "🛢️ 4. Réserves de Pétrole Brut"),
                I18n.getOrDefault("resource.view.gas", "🔥 5. Champs de Gaz Naturel"),
                I18n.getOrDefault("resource.view.uranium", "☢️ 6. Minerais d'Uranium & Fission"),
                I18n.getOrDefault("resource.view.helium3", "🌌 7. Hélium-3 & Fusion Lunaires"),
                I18n.getOrDefault("resource.view.iron_copper", "⛓️ 8. Métaux Fer BIF & Cuivre"),
                I18n.getOrDefault("resource.view.precious_metals", "💎 9. Métaux Précieux (Or, Argent, PGM)"),
                I18n.getOrDefault("resource.view.rare_earths", "🔋 10. Terres Rares & Minéraux Critiques / Lithium"),
                I18n.getOrDefault("resource.view.heat", "🌋 11. Flux Thermique du Manteau"),
                I18n.getOrDefault("resource.view.aquifer", "💧 12. Aquifères & Eau Douce"),
                I18n.getOrDefault("resource.view.section_derived", "────────── CARTES DÉDUITES / ANOMALIES ──────────"),
                I18n.getOrDefault("resource.view.temp", "🌡️ 13. Températures Surface & Microclimats"),
                I18n.getOrDefault("resource.view.seismic", "🌋 14. Tectonique & Aléa Sismique/Volcanique"),
                I18n.getOrDefault("resource.view.aridity", "🏜️ 15. Aridité & Salinisation des Sols")
        );
    }

    /**
     * Renders the thematic resource raster onto the 2D equirectangular preview canvas.
     */
    public void updatePreviewCanvas() {
        if (mapPreviewCanvas == null) return;

        GraphicsContext gc = mapPreviewCanvas.getGraphicsContext2D();

        int canvasW = (int) mapPreviewCanvas.getWidth();
        int canvasH = (int) mapPreviewCanvas.getHeight();
        if (canvasW < 1 || canvasH < 1) return;

        int mode = getViewModeIndex();
        PlanetPreset planet = host.getActivePlanetPreset();
        if (planet == null) planet = PlanetPreset.EARTH_LIKE;
        double sVal = host.getSeismicActivityValue();
        double vVal = host.getVolcanicActivityValue();
        planet = planet.withSeismicAndVolcanic(sVal, vVal);

        int w = 320;
        int h = 160;

        WritableImage buffer = new WritableImage(w, h);
        PixelWriter pw = buffer.getPixelWriter();

        Image customBiomeImage = host.getCustomBiomeImage();
        Image customHydroImage = host.getCustomHydroImage();
        PixelReader customBiomeReader = customBiomeImage != null ? customBiomeImage.getPixelReader() : null;
        PixelReader customHydroReader = customHydroImage != null ? customHydroImage.getPixelReader() : null;
        double wBio = customBiomeImage != null ? customBiomeImage.getWidth() : 0;
        double hBio = customBiomeImage != null ? customBiomeImage.getHeight() : 0;
        double wHydro = customHydroImage != null ? customHydroImage.getWidth() : 0;
        double hHydro = customHydroImage != null ? customHydroImage.getHeight() : 0;

        boolean isRelief = btnReliefOverlay != null && btnReliefOverlay.isSelected();
        Image elevImg = null;
        PixelReader elevReader = null;
        if (planet != null) {
            if (planet.customElevBase64() != null && !planet.customElevBase64().isBlank()) {
                elevImg = ImageMapLoader.base64PngToImage(planet.customElevBase64());
            } else {
                String pName = planet.elevationMapSource() != null && !planet.elevationMapSource().equalsIgnoreCase("none")
                        ? planet.elevationMapSource().toLowerCase()
                        : (planet.name() != null ? planet.name().toLowerCase() : "earth");
                if (pName.contains("earth") || pName.contains("terre")) {
                    elevImg = ImageMapLoader.loadMapImage("earth_elevation.png");
                } else if (pName.contains("mars")) {
                    elevImg = ImageMapLoader.loadMapImage("mars_elevation.png");
                } else if (pName.contains("moon") || pName.contains("lune")) {
                    elevImg = ImageMapLoader.loadMapImage("moon_elevation.png");
                } else if (pName.contains("venus")) {
                    elevImg = ImageMapLoader.loadMapImage("venus_elevation.png");
                } else if (pName.contains("mercury") || pName.contains("mercure")) {
                    elevImg = ImageMapLoader.loadMapImage("mercury_elevation.png");
                }
            }
            if (elevImg != null && elevImg.getWidth() > 0) {
                elevReader = elevImg.getPixelReader();
            }
        }
        double elevW = elevImg != null ? elevImg.getWidth() : 0;
        double elevH = elevImg != null ? elevImg.getHeight() : 0;

        ProceduralGenerator generator = host.getProceduralGenerator();
        ImageMapLoader mapLoader = host.getMapLoader();
        ResourceGeologySectionBuilder geologyBuilder = host.getGeologySectionBuilder();

        for (int py = 0; py < h; py++) {
            double y_base = (py - h / 2.0 - panY) / zoomFactor + h / 2.0;
            if (y_base < 0 || y_base >= h) {
                for (int px = 0; px < w; px++) pw.setColor(px, py, Color.rgb(15, 23, 42));
                continue;
            }
            double lat = 90.0 - (y_base / (double) h) * 180.0;

            for (int px = 0; px < w; px++) {
                double x_base = (px - w / 2.0 - panX) / zoomFactor + w / 2.0;
                if (x_base < 0 || x_base >= w) {
                    pw.setColor(px, py, Color.rgb(15, 23, 42));
                    continue;
                }
                double lon = -180.0 + (x_base / (double) w) * 360.0;

                boolean isLandHere;
                boolean isCoast = false;
                double hillshade = 0.707;
                double declivityVal = 0.0;

                double minAlt = planet != null ? planet.minAltitudeMeters() : -11000.0;
                double maxAlt = planet != null ? planet.maxAltitudeMeters() : 8848.0;
                double wLvl = planet != null ? planet.waterLevel() : 0.48;
                boolean hasOcean = wLvl > -0.4;

                double cutThreshold;
                if (hasOcean) {
                    cutThreshold = Math.clamp(wLvl, 0.01, 0.99);
                } else {
                    double altRange = Math.max(100.0, maxAlt - minAlt);
                    cutThreshold = Math.clamp((-minAlt) / altRange, 0.05, 0.95);
                }

                if (elevReader != null && elevW > 0 && elevH > 0) {
                    int ex = (int) Math.clamp((x_base / (double) w) * elevW, 0, elevW - 1);
                    int ey = (int) Math.clamp((y_base / (double) h) * elevH, 0, elevH - 1);
                    double eVal = elevReader.getColor(ex, ey).getRed();
                    boolean isAboveCut = eVal >= cutThreshold;
                    isLandHere = hasOcean ? isAboveCut : true;

                    int exE = (ex + 1) % (int) elevW;
                    int exW = (ex - 1 + (int) elevW) % (int) elevW;
                    int eyN = Math.max(0, ey - 1);
                    int eyS = Math.min((int) elevH - 1, ey + 1);

                    double eE = elevReader.getColor(exE, ey).getRed();
                    double eW = elevReader.getColor(exW, ey).getRed();
                    double eN = elevReader.getColor(ex, eyN).getRed();
                    double eS = elevReader.getColor(ex, eyS).getRed();

                    isCoast = (isAboveCut != (eE >= cutThreshold)) || (isAboveCut != (eW >= cutThreshold))
                            || (isAboveCut != (eN >= cutThreshold)) || (isAboveCut != (eS >= cutThreshold));

                    double dLng = (eE - eW) * 16.0;
                    double dLat = (eN - eS) * 16.0;
                    hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                    declivityVal = Math.sqrt(dLng * dLng + dLat * dLat);
                } else {
                    var pt = generator.getPlanetPoint(lat, lon, planet);
                    double elev = pt.elevation();
                    declivityVal = pt.declivity();

                    double dDeg = 0.5;
                    var ptE = generator.getPlanetPoint(lat, lon + dDeg, planet);
                    var ptW = generator.getPlanetPoint(lat, lon - dDeg, planet);
                    var ptN = generator.getPlanetPoint(lat + dDeg, lon, planet);
                    var ptS = generator.getPlanetPoint(lat - dDeg, lon, planet);

                    if (hasOcean) {
                        isLandHere = elev >= wLvl;
                        isCoast = (isLandHere != (ptE.elevation() >= wLvl))
                                || (isLandHere != (ptW.elevation() >= wLvl))
                                || (isLandHere != (ptN.elevation() >= wLvl))
                                || (isLandHere != (ptS.elevation() >= wLvl));
                    } else {
                        isLandHere = true;
                        double datumCut = 0.0;
                        boolean isAboveDatum = elev >= datumCut;
                        isCoast = (isAboveDatum != (ptE.elevation() >= datumCut))
                                || (isAboveDatum != (ptW.elevation() >= datumCut))
                                || (isAboveDatum != (ptN.elevation() >= datumCut))
                                || (isAboveDatum != (ptS.elevation() >= datumCut));
                    }

                    double dLng = ((ptE.elevation() - ptW.elevation()) / 1200.0);
                    double dLat = ((ptN.elevation() - ptS.elevation()) / 1200.0);
                    hillshade = (0.5 * dLng + 0.5 * dLat + 0.707) / Math.sqrt(dLng * dLng + dLat * dLat + 1.0);
                }

                Color pxColor;

                if (mode == 0) { // Biome Map
                    if (host.isRadioImportBiomeSelected()) {
                        if (customBiomeReader != null && wBio > 0 && hBio > 0) {
                            int bx = (int) Math.min((x_base / (double) w) * wBio, wBio - 1);
                            int by = (int) Math.min((y_base / (double) h) * hBio, hBio - 1);
                            Color rawC = customBiomeReader.getColor(bx, by);
                            pxColor = mapLoader.getBiomeTargetColor(mapLoader.matchBiomeColor(rawC));
                        } else {
                            pxColor = Color.BLACK;
                        }
                    } else if (!isLandHere) {
                        pxColor = Color.rgb(0, 50, 200); // Ocean
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        pxColor = mapLoader.getBiomeTargetColor(point.biome());
                    }
                } else if (mode == 1) { // Hydrography & River Networks Map
                    if (host.isRadioImportHydroSelected()) {
                        if (customHydroReader != null && wHydro > 0 && hHydro > 0) {
                            int hx = (int) Math.min((x_base / (double) w) * wHydro, wHydro - 1);
                            int hy = (int) Math.min((y_base / (double) h) * hHydro, hHydro - 1);
                            Color c = customHydroReader.getColor(hx, hy);
                            double waterIntensity = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;
                            if (waterIntensity > 0.6) {
                                pxColor = Color.rgb(2, 132, 199);
                            } else if (waterIntensity > 0.3) {
                                pxColor = Color.rgb(56, 189, 248);
                            } else if (!isLandHere) {
                                pxColor = Color.rgb(15, 23, 42);
                            } else {
                                int r = (int) Math.min(255, 45 + declivityVal * 100);
                                int g = (int) Math.min(255, 60 + declivityVal * 80);
                                int b = (int) Math.min(255, 55 + declivityVal * 50);
                                pxColor = Color.rgb(r, g, b);
                            }
                        } else {
                            pxColor = Color.BLACK;
                        }
                    } else if (!isLandHere) {
                        pxColor = Color.rgb(15, 23, 42);
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        double riverFlow = point.riverFlow();
                        if (riverFlow > 0.42) {
                            pxColor = Color.rgb(2, 132, 199);
                        } else if (riverFlow > 0.28) {
                            pxColor = Color.rgb(56, 189, 248);
                        } else if (riverFlow > 0.16) {
                            pxColor = Color.rgb(20, 184, 166);
                        } else {
                            int r = (int) Math.min(255, 45 + declivityVal * 100);
                            int g = (int) Math.min(255, 60 + declivityVal * 80);
                            int b = (int) Math.min(255, 55 + declivityVal * 50);
                            pxColor = Color.rgb(r, g, b);
                        }
                    }
                } else if (mode >= 2 && mode <= 11) { // GEOLOGICAL & ENERGY TENSORS (Modes 2..11 for Layers 0..9)
                    int layerIdx = mode - 2;
                    boolean useImport = geologyBuilder != null && geologyBuilder.isGeologyImportActive(layerIdx);
                    Image gImg = geologyBuilder != null ? geologyBuilder.getGeologyLayerImage(layerIdx) : null;

                    Color baseBackground = isLandHere ? Color.rgb(30, 41, 59) : Color.rgb(15, 23, 42);

                    if (useImport) {
                        if (gImg != null && gImg.getPixelReader() != null && gImg.getWidth() > 0 && gImg.getHeight() > 0) {
                            int gx = (int) Math.clamp((x_base / (double) w) * gImg.getWidth(), 0, gImg.getWidth() - 1);
                            int gy = (int) Math.clamp((y_base / (double) h) * gImg.getHeight(), 0, gImg.getHeight() - 1);
                            Color c = gImg.getPixelReader().getColor(gx, gy);
                            double alpha = c.getOpacity();
                            double brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3.0;

                            if (alpha > 0.05 && brightness > 0.02) {
                                pxColor = blendColors(baseBackground, Color.color(c.getRed(), c.getGreen(), c.getBlue()), alpha);
                            } else {
                                pxColor = baseBackground;
                            }
                        } else {
                            pxColor = Color.BLACK;
                        }
                    } else {
                        double val = geologyBuilder != null ? geologyBuilder.sampleProceduralGeologyTensor(layerIdx, lon, lat, planet) : 0.0;
                        val = Math.clamp(val, 0.0, 1.0);
                        if (val > 0.02) {
                            Color resourceColor = geologyBuilder != null ? geologyBuilder.getGeologyResourceColor(layerIdx, val) : Color.WHITE;
                            pxColor = blendColors(baseBackground, resourceColor, Math.min(1.0, val * 1.3));
                        } else {
                            pxColor = baseBackground;
                        }
                    }
                } else if (mode == 12) { // SURFACE TEMPERATURE & MICROCLIMATES
                    var point = generator.getPlanetPoint(lat, lon, planet);
                    double tempC = point.temperature();
                    double tNorm = Math.clamp((tempC + 25.0) / 70.0, 0.0, 1.0);
                    int r, g, b;
                    if (tNorm < 0.25) {
                        double n = tNorm / 0.25;
                        r = (int) (15 + n * 40); g = (int) (100 + n * 80); b = (int) (220 + n * 35);
                    } else if (tNorm < 0.5) {
                        double n = (tNorm - 0.25) / 0.25;
                        r = (int) (55 + n * 50); g = (int) (180 + n * 30); b = (int) (255 - n * 180);
                    } else if (tNorm < 0.75) {
                        double n = (tNorm - 0.5) / 0.25;
                        r = (int) (105 + n * 140); g = (int) (210 - n * 60); b = (int) (75 - n * 60);
                    } else {
                        double n = (tNorm - 0.75) / 0.25;
                        r = (int) (245 + n * 10); g = (int) (150 - n * 130); b = (int) (15 + n * 20);
                    }
                    Color tColor = Color.rgb(r, g, b);
                    if (!isLandHere) {
                        pxColor = blendColors(tColor, Color.rgb(15, 23, 42), 0.35);
                    } else {
                        pxColor = tColor;
                    }
                } else if (mode == 13) { // SEISMIC & VOLCANIC TECTONISM
                    double heatVal = geologyBuilder != null ? geologyBuilder.sampleProceduralGeologyTensor(8, lon, lat, planet) : 0.0;
                    double sLevel = planet.seismicActivityLevel() / 10.0;
                    double vLevel = planet.volcanicActivityLevel() / 8.0;

                    double risk = Math.clamp(heatVal * 0.5 + declivityVal * 2.0 * sLevel + vLevel * 0.3, 0.0, 1.0);
                    if (!isLandHere) {
                        int r = (int) (15 + risk * 230);
                        int g = (int) (23 + risk * 40);
                        int b = (int) (42 + (1.0 - risk) * 50);
                        pxColor = Color.rgb(r, g, b);
                    } else {
                        int r = (int) (40 + risk * 215);
                        int g = (int) (45 + risk * 90);
                        int b = (int) (55 + (1.0 - risk) * 20);
                        pxColor = Color.rgb(r, g, b);
                    }
                } else { // Mode 14: ARIDITY & SOIL SALINIZATION
                    if (!isLandHere) {
                        pxColor = Color.rgb(15, 23, 42); // Clean ocean basemap
                    } else {
                        var point = generator.getPlanetPoint(lat, lon, planet);
                        double rainfall = point.rainfall();
                        double tempC = point.temperature();
                        double aridityIndex = rainfall / Math.max(1.0, tempC + 10.0);
                        double dryness = Math.clamp(1.0 - (aridityIndex / 50.0), 0.0, 1.0);

                        int r = (int) Math.clamp(30 + dryness * 215, 0, 255);
                        int g = (int) Math.clamp(145 - dryness * 85, 0, 255);
                        int b = (int) Math.clamp(160 - dryness * 145, 0, 255);
                        pxColor = Color.rgb(r, g, b);
                    }
                }

                if (isRelief) {
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

                pw.setColor(px, py, pxColor);
            }
        }

        gc.setImageSmoothing(true);
        gc.drawImage(buffer, 0, 0, canvasW, canvasH);
    }

    /**
     * Updates the horizontal color legend bar matching the active view mode.
     */
    public void updateLegend() {
        if (legendBar == null) return;
        legendBar.getChildren().clear();

        int selectedIdx = getViewModeIndex();
        if (selectedIdx == 0) { // Biomes
            addLegendItem("DEEP_OCEAN", Color.rgb(0, 0, 100), I18n.getOrDefault("resource.legend.deep_ocean", "Deep Ocean / Abyss"));
            addLegendItem("OCEAN", Color.rgb(0, 50, 200), I18n.getOrDefault("resource.legend.ocean", "Ocean / Continental Shelf"));
            addLegendItem("PLAINS", Color.rgb(100, 200, 50), I18n.getOrDefault("resource.legend.plains", "Grasslands & Plains"));
            addLegendItem("FOREST", Color.rgb(20, 120, 20), I18n.getOrDefault("resource.legend.forest", "Temperate & Boreal Forest"));
            addLegendItem("JUNGLE", Color.rgb(0, 80, 0), I18n.getOrDefault("resource.legend.jungle", "Tropical Rainforest / Jungle"));
            addLegendItem("DESERT", Color.rgb(255, 200, 50), I18n.getOrDefault("resource.legend.desert", "Arid Desert & Scrub"));
            addLegendItem("MOUNTAINS", Color.rgb(100, 100, 100), I18n.getOrDefault("resource.legend.mountains", "High Altitude / Mountains"));
        } else if (selectedIdx == 1) { // Hydrography & River Networks
            addLegendItem("DEEP_OCEAN", Color.rgb(15, 23, 42), I18n.getOrDefault("resource.legend.hydro_ocean", "Ocean / Abyss"));
            addLegendItem("MAJOR_RIVER", Color.rgb(2, 132, 199), I18n.getOrDefault("resource.legend.major_river", "Major Rivers"));
            addLegendItem("RIVER_STREAM", Color.rgb(56, 189, 248), I18n.getOrDefault("resource.legend.river_stream", "Streams & Watercourses"));
            addLegendItem("TRIBUTARY", Color.rgb(20, 184, 166), I18n.getOrDefault("resource.legend.tributary", "Tributaries & Drainage"));
            addLegendItem("LAND_SLOPE", Color.rgb(75, 85, 99), I18n.getOrDefault("resource.legend.land_slope", "Continental Relief"));
        } else if (selectedIdx == 2) { // COAL
            addLegendItem("LOW", Color.rgb(30, 30, 30), I18n.getOrDefault("resource.legend.coal_low", "Sterile / Traces"));
            addLegendItem("MED", Color.rgb(180, 100, 20), I18n.getOrDefault("resource.legend.coal_med", "Coal Basin"));
            addLegendItem("HIGH", Color.rgb(255, 140, 0), I18n.getOrDefault("resource.legend.coal_high", "Major Anthracite Deposit"));
        } else if (selectedIdx == 3) { // CRUDE OIL
            addLegendItem("LOW", Color.rgb(30, 20, 20), I18n.getOrDefault("resource.legend.oil_low", "Sterile / Traces"));
            addLegendItem("MED", Color.rgb(180, 40, 40), I18n.getOrDefault("resource.legend.oil_med", "Oil Field"));
            addLegendItem("HIGH", Color.rgb(220, 38, 38), I18n.getOrDefault("resource.legend.oil_high", "Supergiant Bituminous Basin"));
        } else if (selectedIdx == 4) { // NATURAL GAS
            addLegendItem("LOW", Color.rgb(10, 30, 40), I18n.getOrDefault("resource.legend.gas_low", "Sterile / Traces"));
            addLegendItem("MED", Color.rgb(20, 140, 160), I18n.getOrDefault("resource.legend.gas_med", "Gas Field"));
            addLegendItem("HIGH", Color.rgb(6, 182, 212), I18n.getOrDefault("resource.legend.gas_high", "Natural Gas Superfield"));
        } else if (selectedIdx == 5) { // URANIUM
            addLegendItem("LOW", Color.rgb(10, 40, 20), I18n.getOrDefault("resource.legend.uranium_low", "Traces"));
            addLegendItem("MED", Color.rgb(20, 140, 60), I18n.getOrDefault("resource.legend.uranium_med", "Uranium Ore Deposit"));
            addLegendItem("HIGH", Color.rgb(34, 197, 94), I18n.getOrDefault("resource.legend.uranium_high", "High-Grade Pitchblende"));
        } else if (selectedIdx == 6) { // HELIUM-3
            addLegendItem("LOW", Color.rgb(30, 10, 30), I18n.getOrDefault("resource.legend.he3_low", "Regolith Traces"));
            addLegendItem("MED", Color.rgb(140, 30, 140), I18n.getOrDefault("resource.legend.he3_med", "Enriched Regolith"));
            addLegendItem("HIGH", Color.rgb(217, 70, 239), I18n.getOrDefault("resource.legend.he3_high", "Lunar Helium-3 Concentrate"));
        } else if (selectedIdx == 7) { // IRON & COPPER
            addLegendItem("LOW", Color.rgb(40, 30, 20), I18n.getOrDefault("resource.legend.iron_low", "Low Grade"));
            addLegendItem("MED", Color.rgb(180, 90, 20), I18n.getOrDefault("resource.legend.iron_med", "Banded Iron Formation"));
            addLegendItem("HIGH", Color.rgb(249, 115, 22), I18n.getOrDefault("resource.legend.iron_high", "Massive Iron & Copper Deposit"));
        } else if (selectedIdx == 8) { // PRECIOUS METALS
            addLegendItem("LOW", Color.rgb(40, 35, 10), I18n.getOrDefault("resource.legend.precious_low", "Gold/Silver Traces"));
            addLegendItem("MED", Color.rgb(170, 130, 20), I18n.getOrDefault("resource.legend.precious_med", "Placer & Gold Vein"));
            addLegendItem("HIGH", Color.rgb(234, 179, 8), I18n.getOrDefault("resource.legend.precious_high", "Giant Gold/Silver Deposit"));
        } else if (selectedIdx == 9) { // RARE EARTHS & CRITICAL MINERALS
            addLegendItem("LOW", Color.rgb(10, 35, 35), I18n.getOrDefault("resource.legend.ree_low", "REE/Li Traces"));
            addLegendItem("MED", Color.rgb(20, 120, 110), I18n.getOrDefault("resource.legend.ree_med", "Carbonatite / Salar"));
            addLegendItem("HIGH", Color.rgb(45, 212, 191), I18n.getOrDefault("resource.legend.ree_high", "Major Rare Earth Basin (Bayan Obo)"));
        } else if (selectedIdx == 10) { // MANTLE HEAT FLUX
            addLegendItem("LOW", Color.rgb(40, 40, 60), I18n.getOrDefault("resource.legend.heat_low", "Inert / Stable Craton"));
            addLegendItem("MED", Color.rgb(180, 80, 30), I18n.getOrDefault("resource.legend.heat_med", "Mean Geothermal Flux"));
            addLegendItem("HIGH", Color.rgb(240, 20, 20), I18n.getOrDefault("resource.legend.heat_high", "High Magmatism & Tectonic Rifts"));
        } else if (selectedIdx == 11) { // AQUIFERS & FRESHWATER
            addLegendItem("LOW", Color.rgb(20, 40, 80), I18n.getOrDefault("resource.legend.aquifer_low", "Arid / Dry"));
            addLegendItem("MED", Color.rgb(40, 120, 200), I18n.getOrDefault("resource.legend.aquifer_med", "Moderate Aquifer"));
            addLegendItem("HIGH", Color.rgb(0, 220, 255), I18n.getOrDefault("resource.legend.aquifer_high", "Giant Basin Aquifer"));
        } else if (selectedIdx == 12) { // SURFACE TEMPERATURE & MICROCLIMATES
            addLegendItem("POLAR", Color.rgb(35, 120, 230), I18n.getOrDefault("resource.legend.temp_polar", "Polar / Glacial (< 0°C)"));
            addLegendItem("TEMPERATE", Color.rgb(80, 200, 120), I18n.getOrDefault("resource.legend.temp_temperate", "Temperate (10°C - 22°C)"));
            addLegendItem("TROPICAL", Color.rgb(250, 130, 20), I18n.getOrDefault("resource.legend.temp_tropical", "Tropical / Warm (22°C - 34°C)"));
            addLegendItem("TORRID", Color.rgb(245, 20, 40), I18n.getOrDefault("resource.legend.temp_torrid", "Torrid Extreme (> 34°C)"));
        } else if (selectedIdx == 13) { // SEISMIC & VOLCANIC TECTONISM
            addLegendItem("QUIET", Color.rgb(40, 45, 55), I18n.getOrDefault("resource.legend.seismic_quiet", "Stable Shield / Craton"));
            addLegendItem("MODERATE", Color.rgb(200, 100, 30), I18n.getOrDefault("resource.legend.seismic_med", "Active Fault / Orogeny"));
            addLegendItem("HIGH", Color.rgb(245, 30, 30), I18n.getOrDefault("resource.legend.seismic_high", "Subduction & Volcanic Arc"));
        } else if (selectedIdx == 14) { // ARIDITY & SOIL SALINIZATION
            addLegendItem("HUMID", Color.rgb(30, 140, 160), I18n.getOrDefault("resource.legend.aridity_humid", "Humid / Fertile Soils"));
            addLegendItem("SEMI_ARID", Color.rgb(210, 150, 50), I18n.getOrDefault("resource.legend.aridity_semi", "Semi-Arid Steppe"));
            addLegendItem("HYPER_ARID", Color.rgb(230, 60, 20), I18n.getOrDefault("resource.legend.aridity_hyper", "Hyper-Arid Salt Desert"));
        }
    }

    private void addLegendItem(String id, Color col, String text) {
        Pane colorSwatch = new Pane();
        colorSwatch.setPrefSize(14, 14);
        colorSwatch.setStyle(String.format("-fx-background-color: rgba(%d,%d,%d,1); -fx-border-color: #ffffff; -fx-border-width: 1; -fx-background-radius: 3;",
                (int) (col.getRed() * 255), (int) (col.getGreen() * 255), (int) (col.getBlue() * 255)));
        Label lbl = new Label(text);
        lbl.getStyleClass().add("control-label");
        HBox itemBox = new HBox(4, colorSwatch, lbl);
        itemBox.setAlignment(Pos.CENTER);
        legendBar.getChildren().add(itemBox);
    }

    /**
     * Blends two colors according to alpha overlay weight:
     * $$C_{\text{blend}} = C_{\text{base}} \cdot (1 - \alpha) + C_{\text{overlay}} \cdot \alpha$$
     *
     * @param base the background base color
     * @param overlay the overlay tint color
     * @param opacity the blending opacity parameter \(\alpha \in [0, 1]\)
     * @return the composite interpolated color
     */
    public Color blendColors(Color base, Color overlay, double opacity) {
        if (base == null) return overlay;
        double r = base.getRed() * (1.0 - opacity) + overlay.getRed() * opacity;
        double g = base.getGreen() * (1.0 - opacity) + overlay.getGreen() * opacity;
        double b = base.getBlue() * (1.0 - opacity) + overlay.getBlue() * opacity;
        return Color.color(Math.clamp(r, 0.0, 1.0), Math.clamp(g, 0.0, 1.0), Math.clamp(b, 0.0, 1.0));
    }

    /**
     * Computes relief shade color from elevation, sea level, and declivity slope.
     *
     * @param elevation the point elevation in meters
     * @param waterLevel the planet sea level datum
     * @param declivity the surface slope gradient
     * @return the resulting shade color
     */
    public Color getReliefShadeColor(double elevation, double waterLevel, double declivity) {
        if (elevation < waterLevel) {
            double depthNorm = Math.clamp((waterLevel - elevation) / 2000.0, 0.0, 1.0);
            double val = 0.05 + 0.15 * (1.0 - depthNorm);
            return Color.color(val * 0.5, val * 0.7, val);
        } else {
            double hNorm = Math.clamp((elevation - waterLevel) / 3000.0, 0.0, 1.0);
            double shade = 0.2 + 0.6 * hNorm + 0.2 * Math.min(1.0, declivity * 2.0);
            return Color.color(shade, shade, shade);
        }
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
            btnReliefOverlay.setText(I18n.getOrDefault("resource.btn.relief_overlay", "⛏️ Relief"));
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
