/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.culture.CultureEngine;
import org.ether.society.culture.CultureVector;
import org.ether.society.flux.FluxEngine;
import org.ether.society.model.Biome;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import com.uber.h3core.util.LatLng;

/**
 * Canvas for rendering H3 hexagonal cells.
 * Supports both 2D (flat) and 3D (isometric with elevation) rendering modes.
 * 
 * Mouse Controls:
 * - Left Click + Drag: Pan
 * - Right Click + Drag: Rotate (3D only)
 * - Mouse Wheel: Zoom
 */
public class H3MapCanvas extends Canvas {
    private static final Logger logger = LoggerFactory.getLogger(H3MapCanvas.class);

    private List<H3Cell> cells;
    private org.ether.society.core.dod.WorldBuffer worldBuffer;
    private Map<Long, H3Cell> cellMap; // Fast lookup for neighbors
    private ViewMode viewMode = ViewMode.VIEW_2D;
    private DisplayMode displayMode = DisplayMode.BIOME;
    private boolean showContours = false; // Toggle for contour lines
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    // Bounds for coordinate conversion
    private double minLat;
    private double maxLat;
    private double minLng;
    private double maxLng;

    // Mouse interaction state
    private double zoomFactor = 1.0;
    private double dragStartX;
    private double dragStartY;
    
    // 3D camera rotation


    // Tooltip
    private CellTooltip tooltip;
    private Pane tooltipContainer;
    private H3Service h3Service;
    private H3Cell hoveredCell = null;

    // 3D isometric parameters
    // private static final double ISO_ANGLE = Math.toRadians(30);
    private static final double ELEVATION_SCALE = 0.05; // px per meter

    // Center view tracking
    private double centerLat;
    private double centerLng;

    // Mini-map reference (for updates)
    private MiniMap miniMap;

    // Engines for visualization

    private CultureEngine cultureEngine;
    private org.ether.society.agents.AgentManager agentManager;

    public H3MapCanvas(double width, double height) {
        super(width, height);
        this.h3Service = new H3Service(8); // Resolution 8
        setupMouseHandlers();
    }

    /**
     * Set the tooltip container (must be called after construction).
     */
    public void setTooltipContainer(Pane container) {
        this.tooltipContainer = container;
        this.tooltip = new CellTooltip();
        container.getChildren().add(tooltip);
    }

    public void setEngines(FluxEngine fluxEngine, CultureEngine cultureEngine,
            org.ether.society.agents.AgentManager agentManager) {

        this.cultureEngine = cultureEngine;
        this.agentManager = agentManager;
    }

    private void setupMouseHandlers() {
        // Mouse wheel zoom (Google Maps style towards mouse position)
        setOnScroll(event -> {
            double delta = event.getDeltaY();
            double zoomChange = delta > 0 ? 1.15 : 0.85;
            double oldZoom = zoomFactor;
            double newZoom = Math.max(0.5, Math.min(20.0, oldZoom * zoomChange));

            if (newZoom != oldZoom) {
                double mouseX = event.getX();
                double mouseY = event.getY();
                double w = getWidth();
                double h = getHeight();

                double latRange = maxLat - minLat;
                double lngRange = maxLng - minLng;
                if (latRange > 0 && lngRange > 0 && w > 0 && h > 0) {
                    double baseScale = Math.min(w / lngRange, h / latRange) * 0.9;
                    double oldScale = baseScale * oldZoom;
                    double newScale = baseScale * newZoom;

                    double mouseLng = centerLng + (mouseX - w / 2.0) / oldScale;
                    double mouseLat = centerLat - (mouseY - h / 2.0) / oldScale;

                    centerLng = mouseLng - (mouseX - w / 2.0) / newScale;
                    centerLat = mouseLat + (mouseY - h / 2.0) / newScale;
                }

                zoomFactor = newZoom;
                draw();
                notifyMiniMap();
                logger.debug("Zoom: {}x (Center: {}, {})", String.format("%.2f", zoomFactor), centerLat, centerLng);
            }
        });

        // Mouse press - determine mode
        setOnMousePressed(event -> {
            dragStartX = event.getX();
            dragStartY = event.getY();

            if (event.isSecondaryButtonDown()) {
                // Right click = unused for now
                setCursor(javafx.scene.Cursor.CROSSHAIR);
            } else if (event.isPrimaryButtonDown()) {
                // Left click = pan/rotate

                setCursor(javafx.scene.Cursor.MOVE);
            }
        });

        // Mouse drag
        setOnMouseDragged(event -> {
            double dx = event.getX() - dragStartX;
            double dy = event.getY() - dragStartY;

            // Unified Panning/Rotation
            centerLng -= dx / scale;
            centerLat += dy / scale;
            
            // Clamp to world bounds
            centerLat = Math.max(minLat, Math.min(maxLat, centerLat));
            centerLng = Math.max(minLng, Math.min(maxLng, centerLng));

            dragStartX = event.getX();
            dragStartY = event.getY();
            draw();
            notifyMiniMap();
        });

        // Mouse release
        setOnMouseReleased(event -> {

            setCursor(javafx.scene.Cursor.DEFAULT);
        });

        // Double-click to reset view to full centered perspective
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                zoomFactor = 1.0;
                centerLat = (minLat + maxLat) / 2.0;
                centerLng = (minLng + maxLng) / 2.0;
                draw();
                notifyMiniMap();
                logger.info("Double-click: reset H3 map view to full centered perspective.");
            }
        });

        // Mouse move for tooltip
        setOnMouseMoved(event -> {
            if (tooltip != null && tooltipContainer != null) {
                updateTooltip(event.getX(), event.getY(),
                        event.getSceneX(), event.getSceneY());
            }
        });

        // Hide tooltip when leaving canvas
        setOnMouseExited(event -> {
            if (tooltip != null) {
                tooltip.hide();
                hoveredCell = null;
            }
        });
    }

    public void setViewMode(ViewMode mode) {
        this.viewMode = mode;
        draw();
        logger.info("View mode changed to: {}", mode);
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public void setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
        draw();
        logger.info("Display mode changed to: {}", mode);
    }

    public DisplayMode getDisplayMode() {
        return displayMode;
    }

    // Toggle for contours
    public void toggleContours(boolean show) {
        this.showContours = show;
        draw();
    }

    public void setCells(List<H3Cell> cells) {
        // Sort by latitude for faster culling
        this.cells = new ArrayList<>(cells); // Copy to allow sorting
        this.cells.sort(Comparator.comparingDouble(H3Cell::getLatitude));
        
        // Build map for fast lookup
        this.cellMap = new HashMap<>(); // Standard mapping in O(N)
        for (H3Cell c : cells) {
            cellMap.put(c.getH3Index(), c);
        }

        // Compute lat/lng bounds
        minLat = cells.stream().mapToDouble(H3Cell::getLatitude).min().orElse(0);
        maxLat = cells.stream().mapToDouble(H3Cell::getLatitude).max().orElse(0);
        minLng = cells.stream().mapToDouble(H3Cell::getLongitude).min().orElse(0);
        maxLng = cells.stream().mapToDouble(H3Cell::getLongitude).max().orElse(0);

        // Initialize center to midpoint
        centerLat = (minLat + maxLat) / 2.0;
        centerLng = (minLng + maxLng) / 2.0;

        logger.info("H3 Canvas initialized with {} cells. Bounds: lat[{}, {}], lng[{}, {}]",
                cells.size(), minLat, maxLat, minLng, maxLng);
        draw();
    }

    public void setWorldBuffer(org.ether.society.core.dod.WorldBuffer buffer) {
        this.worldBuffer = buffer;
        draw();
    }

    public void draw() {
        if (getWidth() < 1.0 || getHeight() < 1.0) return;
        if (cells == null || cells.isEmpty()) {
            return;
        }

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // Draw background
        gc.setFill(Color.rgb(15, 20, 25));
        gc.fillRect(0, 0, getWidth(), getHeight());

        // Calculate scale to fit all cells with zoom
        double latRange = maxLat - minLat;
        double lngRange = maxLng - minLng;
        double scaleX = getWidth() / lngRange;
        double scaleY = getHeight() / latRange;
        scale = Math.min(scaleX, scaleY) * 0.9 * zoomFactor; // Apply zoom

        // Calculate offsets to center the view on centerLat/centerLng
        // Formula: ScreenPos = (WorldPos - WorldMin) * Scale + Offset
        // We want ScreenPos(Center) = ScreenCenter
        // Offset = ScreenCenter - (WorldCenter - WorldMin) * Scale
        
        offsetX = (getWidth() / 2.0) - (centerLng - minLng) * scale;
        // height/2 = (maxLat - centerLat) * scale + offsetY
        offsetY = (getHeight() / 2.0) - (maxLat - centerLat) * scale;

        if (viewMode == ViewMode.VIEW_3D) {
            draw3D(gc, minLat, maxLat, minLng, maxLng);
        } else {
            draw2D(gc, minLat, maxLat, minLng, maxLng);
        }

        if (agentManager != null) {
            drawAgents(gc);
        }
        
        if (showContours) {
            drawContours(gc);
        }

        // Draw legend overlay box with rainbow spectrum, min/max, mean (μ) and median (M) indicators
        drawLegendOverlay(gc);

        logger.debug("Drew {} cells in {} mode", cells.size(), viewMode);
    }

    /**
     * Update tooltip based on mouse position.
     */
    private void updateTooltip(double canvasX, double canvasY, double sceneX, double sceneY) {
        H3Cell cell = findCellAt(canvasX, canvasY);

        if (cell != null && cell != hoveredCell) {
            hoveredCell = cell;
            if (worldBuffer != null) {
                // Find index of this cell in the buffer
                // Note: we assume the list 'cells' is the same one used to populate the buffer
                int index = cells.indexOf(cell); 
                tooltip.updateFromBuffer(worldBuffer, index);
            } else {
                tooltip.updateCell(cell);
            }
            tooltip.position(sceneX, sceneY,
                    tooltipContainer.getWidth(),
                    tooltipContainer.getHeight());
        } else if (cell == null) {
            tooltip.hide();
            hoveredCell = null;
        } else if (cell == hoveredCell) {
            // Same cell, just update position
            tooltip.position(sceneX, sceneY,
                    tooltipContainer.getWidth(),
                    tooltipContainer.getHeight());
        }
    }

    /**
     * Find the H3 cell at the given canvas coordinates.
     */
    private H3Cell findCellAt(double mouseX, double mouseY) {
        if (cells == null || cells.isEmpty()) {
            return null;
        }

        // Convert canvas coordinates to lat/lng (reverse of draw() transform)
        double lng = ((mouseX - offsetX) / scale) + minLng;
        double lat = maxLat - ((mouseY - offsetY) / scale);

        // Check if within bounds
        if (lat < minLat || lat > maxLat || lng < minLng || lng > maxLng) {
            return null;
        }

        // Get H3 index at this location
        long h3Index = h3Service.latLngToCell(lat, lng);

        // Find matching cell in our dataset
        return cells.stream()
                .filter(c -> c.getH3Index() == h3Index)
                .findFirst()
                .orElse(null);
    }

    private void draw2D(GraphicsContext gc, double minLat, double maxLat, double minLng, double maxLng) {
        // Culling: Calculate visible latitude range

        // Actually simpler: Reverse map bounds
        // screenY = (maxLat - lat) * scale + offsetY
        // lat = maxLat - (screenY - offsetY) / scale

        double viewTopLat = maxLat - (0 - offsetY) / scale;
        double viewBottomLat = maxLat - (getHeight() - offsetY) / scale;

        // Clamp to world bounds
        // Note: viewTopLat is greater than viewBottomLat in value (North is positive)
        double cullMaxLat = Math.min(90, viewTopLat + 10.0); // +10 buffer
        double cullMinLat = Math.max(-90, viewBottomLat - 10.0);

        // Binary search for start index
        int startIndex = findLatIndex(cullMinLat);
        // End index is implicitly handled by loop condition or finding end

        // Draw loop
        for (int i = startIndex; i < cells.size(); i++) {
            H3Cell cell = cells.get(i);
            if (cell.getLatitude() > cullMaxLat)
                break; // Sorted, so we can stop

            double x = (cell.getLongitude() - minLng) * scale + offsetX;
            double y = (maxLat - cell.getLatitude()) * scale + offsetY;

            // Screen bounds check (extra safety)
            if (x < -5 || x > getWidth() + 5 || y < -5 || y > getHeight() + 5) {
                continue;
            }

            Color color;
            if (worldBuffer != null) {
                color = getBufferCellColor(i);
            } else {
                color = getCellColor(cell);
            }
            
            gc.setFill(color);
            gc.fillOval(x - 1, y - 1, 2, 2);
        }
    }

    private int findLatIndex(double targetLat) {
        // Binary search for insertion point
        int low = 0;
        int high = cells.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            double midVal = cells.get(mid).getLatitude();

            if (midVal < targetLat)
                low = mid + 1;
            else if (midVal > targetLat)
                high = mid - 1;
            else
                return mid; // key found
        }
        return low; // key not found, return insertion point
    }

    private void draw3D(GraphicsContext gc, double minLat, double maxLat, double minLng, double maxLng) {
        // Sphere rendering - ignore viewport bounds, render entire globe
        double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
        
        // Center of screen
        double cx = getWidth() / 2;
        double cy = getHeight() / 2;

        // Rotation from centerLng (Longitude)
        // We want centerLng to be at rotation 0 (facing screen)
        // Standard spherical: x is lng=0. Z is axis? No, Z is depth.
        // Let's assume lng=0 is facing us at rot=0.
        // If centerLng changes, we rotate the world.
        // Rotation = -centerLng.
        double radRotationY = Math.toRadians(-centerLng - 90); // -90 adjustment might be needed to align Prime Meridian
        
        // Tilt from centerLat (Latitude)
        // Map centerLat to tilt. 
        // centerLat = 0 (Equator) -> Tilt = 0? Or 15 deg default?
        // Let's allow dragging to tilt lightly.
        // centerLat varies roughly -90 to 90.
        // Let's map it to tilt around X axis.
        double radTilt = Math.toRadians(15 + centerLat * 0.5); // Base 15 deg + effect of latitude

        // RenderPoint for sorting
        class RenderPoint {
            double z; // Depth for sorting
            double screenX, screenY;
            Color color;
            double dotSize;
        }

        // Pre-calculate constants for lambda
        final double finalRadRotationY = radRotationY;
        final double finalRadTilt = radTilt;
        final double finalRadius = radius;
        final double finalCx = cx;
        final double finalCy = cy;

        // Optimization: Don't stream parallel if small count? 
        // Parallel stream overhead for small N.
        
        List<RenderPoint> points = java.util.stream.IntStream.range(0, cells.size()).parallel().mapToObj(i -> {
            H3Cell cell = cells.get(i);
            ProjectedPoint pp = project3D(cell.getLatitude(), cell.getLongitude(), 
                                         cell.getElevation() != null ? cell.getElevation() : 0,
                                         finalRadRotationY, finalRadTilt, finalRadius, finalCx, finalCy);
            
            if (pp == null) return null;

            // Color shading from WorldBuffer if active, or H3Cell
            Color baseColor = (worldBuffer != null && i < worldBuffer.getCapacity()) ? getBufferCellColor(i) : getCellColor(cell);
            double lightFactor = 0.5 + 0.5 * Math.max(0, pp.z);
            Color shadedColor = Color.color(
                    Math.min(1.0, baseColor.getRed() * lightFactor),
                    Math.min(1.0, baseColor.getGreen() * lightFactor),
                    Math.min(1.0, baseColor.getBlue() * lightFactor));

            double dotSize = 1.5 + pp.z * 1.5;
            dotSize = Math.max(1.0, dotSize * zoomFactor * 0.5);

            RenderPoint p = new RenderPoint();
            p.z = pp.z;
            p.screenX = pp.screenX;
            p.screenY = pp.screenY;
            p.color = shadedColor;
            p.dotSize = dotSize;
            return p;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        // Sort by depth: draw furthest (smallest z) first
        points.sort((a, b) -> Double.compare(a.z, b.z));

        // Draw all visible points
        for (RenderPoint p : points) {
            gc.setFill(p.color);
            gc.fillOval(p.screenX - p.dotSize / 2, p.screenY - p.dotSize / 2, p.dotSize, p.dotSize);
        }

        // Draw atmosphere glow effect
        gc.setStroke(Color.rgb(100, 150, 255, 0.2));
        gc.setLineWidth(3);
        gc.strokeOval(cx - radius - 5, cy - radius - 5, (radius + 5) * 2, (radius + 5) * 2);
        gc.strokeOval(cx - radius - 5, cy - radius - 5, (radius + 5) * 2, (radius + 5) * 2);
    }

    private void drawAgents(GraphicsContext gc) {
        for (org.ether.society.agents.Agent agent : agentManager.getAgents()) {
            H3Cell cell = cells.stream()
                    .filter(c -> c.getH3Index() == agent.getH3Index()) // Optimized lookup needed later
                    .findFirst()
                    .orElse(null);

            if (cell == null)
                continue;

            double x, y;
            if (viewMode == ViewMode.VIEW_3D) {
                // Simplified 3D projection for agents (reuse logic or approximate)
                // For MVP, skip 3D agents or just draw flat over center
                // Better: Reuse 3D projection logic.
                // For now, only 2D agents supported to save complexity in this step
                continue;
            } else {
                x = (cell.getLongitude() - minLng) * scale + offsetX;
                y = (maxLat - cell.getLatitude()) * scale + offsetY;
            }

            // Draw Agent
            gc.setFill(getAgentColor(agent.getType()));
            gc.fillOval(x - 3, y - 3, 6, 6);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1);
            gc.strokeOval(x - 3, y - 3, 6, 6);
        }
    }

    private Color getAgentColor(org.ether.society.agents.AgentType type) {
        return switch (type) {
            case SCOUT -> Color.YELLOW;
            case TRADER -> Color.ORANGE;
            case SETTLER -> Color.MAGENTA;
        };
    }

    /**
     * Get cell color based on current display mode.
     */
    private Color getCellColor(H3Cell cell) {
        return switch (displayMode) {
            case BIOME -> getBiomeColor(cell.getBiome());
            case POPULATION -> getPopulationColor(cell.getPopulation());
            case FOOD -> getFoodColor(cell.getFoodResource());
            case TEMPERATURE -> getTemperatureColor(cell.getTemperature());
            case TECHNOLOGY -> getTechColor(cell.getTechnologyLevel());
            case WATER -> getWaterColor(cell.getWaterResource());
            case WOOD -> getWoodColor(cell.getWoodResource());
            case INEQUALITY -> getGiniColor(cell.getGiniIndex());
            case CAPACITY -> getCapacityColor(cell);
            case MIGRATION -> getMigrationColor(cell);
            case FLUX -> getFluxPressureColor(cell);
            case CULTURE -> getCultureColor(cell);
            case POLITICAL -> getPoliticalColor(cell);
            case ASABIYYAH -> getAsabiyyahColor(cell);
            case AGE_PYRAMID -> getAgePyramidColor(cell);
            case ALBEDO -> getAlbedoColor(cell);
            case EPIDEMIC -> getEpidemicColor(cell);
            case FRICTION -> getFrictionColor(cell);
        };
    }

    private Color getAsabiyyahColor(H3Cell cell) {
        if (cell.getOwner() == null) return Color.rgb(40, 40, 50);
        double asabiyyah = cell.getOwner().getAsabiyyah();
        return Color.color(1.0 - asabiyyah, asabiyyah, 0.2); // Green (high cohesion) to Red (instability)
    }

    private Color getAgePyramidColor(H3Cell cell) {
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        if (pop == 0) return Color.rgb(30, 30, 40);
        double seniorRatio = (double) (cell.getPop65to79() + cell.getPop80Plus()) / pop;
        return Color.color(seniorRatio, 0.4, 1.0 - seniorRatio); // Blue (young) to Magenta/Purple (aging)
    }

    private Color getAlbedoColor(H3Cell cell) {
        double albedo = cell.getDynamicAlbedo() != null ? cell.getDynamicAlbedo() : 0.30;
        return Color.gray(Math.clamp(albedo, 0.05, 0.95)); // Grayscale reflectance
    }

    private Color getEpidemicColor(H3Cell cell) {
        int infected = cell.getEpidemicInfected() != null ? cell.getEpidemicInfected() : 0;
        if (infected == 0) return getBiomeColor(cell.getBiome()).desaturate();
        double norm = Math.min(1.0, infected / 200.0);
        return Color.rgb(255, (int) ((1 - norm) * 100), (int) ((1 - norm) * 100)); // Vivid red outbreak
    }

    private Color getFrictionColor(H3Cell cell) {
        double friction = cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
        double norm = Math.min(1.0, (friction - 0.5) / 8.0);
        return Color.color(norm, 1.0 - norm, 0.1); // Green (easy plain) to Red (impassable mountain)
    }

    private Color getBufferCellColor(int index) {
        if (worldBuffer == null) return Color.BLACK;

        return switch (displayMode) {
            case BIOME -> getBiomeColor(org.ether.society.model.Biome.values()[worldBuffer.getBiomes()[index]]);
            case POPULATION -> getPopulationColor((int)worldBuffer.getBiomassHuman()[index]);
            case FOOD -> getFoodColor(worldBuffer.getFoodResource()[index]);
            case TEMPERATURE -> getTemperatureColor(worldBuffer.getTemperature()[index]);
            case TECHNOLOGY -> getTechColor((double)worldBuffer.getTechnologyLevel()[index]);
            case WATER -> getWaterColor((double)worldBuffer.getWaterResource()[index]);
            case WOOD -> getWoodColor((double)worldBuffer.getWoodResource()[index]);
            case INEQUALITY -> getGiniColor((double)worldBuffer.getGiniIndex()[index]);
            case FLUX -> getPriceColor(worldBuffer.getLocalPrice()[index]);
            default -> (cells != null && index >= 0 && index < cells.size()) ? getCellColor(cells.get(index)) : Color.BLACK;
        };
    }

    private Color getPriceColor(float price) {
        // Higher price = more red
        float normalized = Math.min(1.0f, price / 10.0f);
        return Color.color(normalized, 1.0f - normalized, 0);
    }

    private Color getPoliticalColor(H3Cell cell) {
        if (cell.getOwner() == null) {
            // Unclaimed territory - just biome but dimmer or specific color
            return getBiomeColor(cell.getBiome()).desaturate().darker();
            // Or Color.GRAY for neutral? Let's use darker biome to show it's 'wild'.
        }
        return cell.getOwner().getColor();
    }

    private Color getBiomeColor(Biome biome) {
        if (biome == null)
            return Color.GRAY;

        return switch (biome) {
            case OCEAN -> Color.rgb(25, 50, 150);
            case DEEP_OCEAN -> Color.rgb(15, 30, 100);
            case BEACH -> Color.rgb(238, 214, 175);
            case DESERT -> Color.rgb(237, 201, 175);
            case PLAINS -> Color.rgb(124, 252, 0);
            case SAVANNAH -> Color.rgb(180, 200, 70);
            case FOREST -> Color.rgb(34, 139, 34);
            case JUNGLE -> Color.rgb(0, 100, 0);
            case MOUNTAINS -> Color.rgb(139, 137, 137);
            case HILLS -> Color.rgb(160, 160, 120);
            case TUNDRA -> Color.rgb(221, 221, 187);
            case SNOW -> Color.rgb(255, 250, 250);
            case GLACIER -> Color.rgb(220, 240, 255);
        };
    }

    /**
     * Get population density color (blue -> cyan -> green -> yellow -> red).
     */
    private Color getPopulationColor(int population) {
        if (population == 0) {
            return Color.rgb(30, 30, 40); // Dark for unpopulated
        }

        // Log scale for better visualization (most populations < 100)
        double normalized = Math.log1p(population) / Math.log1p(500); // 500 as max expected
        normalized = Math.min(1.0, normalized);

        // Heat map: blue -> cyan -> green -> yellow -> red
        if (normalized < 0.25) {
            double t = normalized / 0.25;
            return Color.rgb(0, (int) (t * 255), 255); // Blue to Cyan
        } else if (normalized < 0.5) {
            double t = (normalized - 0.25) / 0.25;
            return Color.rgb(0, 255, (int) (255 * (1 - t))); // Cyan to Green
        } else if (normalized < 0.75) {
            double t = (normalized - 0.5) / 0.25;
            return Color.rgb((int) (t * 255), 255, 0); // Green to Yellow
        } else {
            double t = (normalized - 0.75) / 0.25;
            return Color.rgb(255, (int) (255 * (1 - t)), 0); // Yellow to Red
        }
    }

    /**
     * Get food resource color (brown -> yellow -> green).
     */
    private Color getFoodColor(double food) {
        if (food < 10) {
            return Color.rgb(60, 40, 20); // Dark brown for no food
        }

        double normalized = Math.min(1.0, food / 800.0); // 800 as high food level

        if (normalized < 0.5) {
            double t = normalized / 0.5;
            return Color.rgb((int) (139 - t * 100), (int) (69 + t * 186), 19); // Brown to Yellow-green
        } else {
            double t = (normalized - 0.5) / 0.5;
            return Color.rgb((int) (39 * (1 - t)), (int) (139 + t * 116), (int) (34 * (1 - t))); // Yellow-green to
                                                                                                 // Bright green
        }
    }

    /**
     * Get temperature color (cool blue -> warm red).
     */
    private Color getTemperatureColor(double temp) {
        // Normalize -30 to +45Â°C to 0-1 range
        double normalized = (temp + 30) / 75.0;
        normalized = Math.max(0, Math.min(1.0, normalized));

        if (normalized < 0.25) {
            return Color.rgb(0, 0, (int) (128 + normalized * 4 * 127)); // Dark blue to bright blue
        } else if (normalized < 0.5) {
            double t = (normalized - 0.25) / 0.25;
            return Color.rgb(0, (int) (t * 255), 255); // Blue to Cyan
        } else if (normalized < 0.75) {
            double t = (normalized - 0.5) / 0.25;
            return Color.rgb((int) (t * 255), 255, (int) (255 * (1 - t))); // Cyan to Yellow
        } else {
            double t = (normalized - 0.75) / 0.25;
            return Color.rgb(255, (int) (255 * (1 - t)), 0); // Yellow to Red
        }
    }

    /**
     * Get technology level color (dark -> purple -> bright).
     */
    private Color getTechColor(Double techLevel) {
        if (techLevel == null || techLevel <= 0) {
            return Color.rgb(30, 30, 30); // Dark for no tech
        }

        // Tech levels 0-10 mapped to color gradient
        double normalized = Math.min(1.0, techLevel / 10.0);

        // Dark purple -> bright cyan progression
        int r = (int) (80 + normalized * 100);
        int g = (int) (20 + normalized * 235);
        int b = (int) (150 + normalized * 105);

        return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    /**
     * Get water resource color (brown -> blue).
     */
    private Color getWaterColor(Double water) {
        if (water == null || water < 10) {
            return Color.rgb(139, 90, 43); // Dry brown
        }

        double normalized = Math.min(1.0, water / 1000.0);

        // Brown -> Light blue -> Deep blue
        int r = (int) (139 - normalized * 119);
        int g = (int) (90 + normalized * 100);
        int b = (int) (43 + normalized * 172);

        return Color.rgb(Math.max(0, r), Math.min(255, g), Math.min(255, b));
    }

    /**
     * Get wood resource color (light -> dark green).
     */
    private Color getWoodColor(Double wood) {
        if (wood == null || wood < 10) {
            return Color.rgb(100, 80, 60); // Barren brown
        }

        double normalized = Math.min(1.0, wood / 1000.0);

        // Light brown -> Green -> Dark green
        int r = (int) (100 - normalized * 80);
        int g = (int) (80 + normalized * 100);
        int b = (int) (60 - normalized * 40);

        return Color.rgb(Math.max(0, r), Math.min(180, g), Math.max(0, b));
    }

    /**
     * Get Gini inequality color (green=equal, red=unequal).
     */
    private Color getGiniColor(Double gini) {
        if (gini == null || gini < 0.01) {
            return Color.rgb(50, 50, 50); // No data
        }

        // Gini 0 (perfect equality) = green, Gini 1 (maximum inequality) = red
        double normalized = Math.min(1.0, gini);

        int r = (int) (normalized * 255);
        int g = (int) ((1 - normalized) * 200);
        int b = 50;

        return Color.rgb(r, g, b);
    }

    /**
     * Get carrying capacity color.
     */
    private Color getCapacityColor(H3Cell cell) {
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0;
        double water = cell.getWaterResource() != null ? cell.getWaterResource() : 0;

        // Capacity based on food and water
        double capacity = (food / 100.0) * (0.3 + Math.min(1.0, water / 1000.0) * 0.7);
        double normalized = Math.min(1.0, capacity / 10.0);

        if (normalized < 0.2) {
            return Color.rgb(80, 40, 40); // Low capacity - dark red
        } else if (normalized < 0.5) {
            double t = (normalized - 0.2) / 0.3;
            return Color.rgb(80 + (int) (t * 100), 40 + (int) (t * 80), 40); // Orange
        } else {
            double t = (normalized - 0.5) / 0.5;
            return Color.rgb((int) (180 - t * 130), (int) (120 + t * 100), (int) (40 + t * 40)); // Yellow to Green
        }
    }

    /**
     * Get migration pressure color.
     */
    private Color getMigrationColor(H3Cell cell) {
        int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
        double food = cell.getFoodResource() != null ? cell.getFoodResource() : 0;

        if (pop == 0) {
            return Color.rgb(30, 30, 30); // No population
        }

        // Migration pressure: high population, low food = outbound (red)
        // Low population, high food = inbound attraction (blue)
        double foodPerCapita = food / Math.max(1, pop);
        double pressure = 1.0 - Math.min(1.0, foodPerCapita / 10.0);

        if (pressure > 0.7) {
            // High outbound pressure - red
            return Color.rgb(200, 50, 50);
        } else if (pressure > 0.4) {
            // Moderate - yellow
            return Color.rgb(200, 200, 50);
        } else if (pressure > 0.2) {
            // Low - neutral
            return Color.rgb(100, 150, 100);
        } else {
            // Attractive - blue (people want to come here)
            return Color.rgb(50, 100, 200);

        }
    }

    /**
     * Get flux pressure color (Blue=Supply, Red=Demand).
     */
    private Color getFluxPressureColor(H3Cell cell) {
        // We assume pressure is roughly -1 to 1.
        // But we don't have access to pressure directly unless we query FluxEngine.
        // Actually, FluxEngine stores it in a map. We didn't expose a getter for
        // pressureMap.
        // Workaround: Calculate it on the fly or just visualize Food/Pop ratio as
        // proxy.
        // Real implementation should expose pressure.
        // For now, visualize Food / Consumption ratio.

        double food = cell.getFoodResource();
        double consumption = cell.getPopulation() * 2.0; // approx

        if (consumption == 0)
            return Color.GREEN; // Surplus potential

        double ratio = food / consumption;
        // Ratio > 1.0 = Surplus (Blue/Green)
        // Ratio < 1.0 = Deficit (Red)

        if (ratio >= 2.0)
            return Color.BLUE; // High surplus
        if (ratio >= 1.0) {
            // 1.0 to 2.0 blends Green to Blue
            double t = Math.min(1.0, ratio - 1.0);
            return Color.rgb(0, (int) ((1 - t) * 255), (int) (t * 255));
        }

        // Deficit: 0.0 to 1.0 blends Red to Yellow/Green
        // 0.0 = Starvation (Red)
        // 1.0 = Subsistence (Green)
        double t = Math.max(0, ratio);
        return Color.rgb((int) ((1 - t) * 255), (int) (t * 255), 0);
    }

    /**
     * Get cultural identity color (RGB vector).
     */
    private Color getCultureColor(H3Cell cell) {
        if (cultureEngine == null)
            return Color.GRAY;

        CultureVector vec = cultureEngine.getCulture(cell.getH3Index());
        if (vec == null)
            return Color.GRAY;

        return Color.color(vec.getRed(), vec.getGreen(), vec.getBlue());
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public double getCenterLat() {
        return centerLat;
    }

    public double getCenterLng() {
        return centerLng;
    }

    /**
     * Set the mini-map reference for synchronization.
     */
    public void setMiniMap(MiniMap miniMap) {
        this.miniMap = miniMap;
    }

    /**
     * Set the center view position (called by mini-map click).
     */
    public void setCenterView(double lat, double lng) {
        this.centerLat = lat;
        this.centerLng = lng;

        draw();

        // Update mini-map
        if (miniMap != null) {
            miniMap.updateViewport(zoomFactor, centerLat, centerLng);
        }

        logger.debug("Center view set to: {}, {}", lat, lng);
    }

    /**
     * Notify mini-map of viewport changes.
     */
    private void notifyMiniMap() {
        if (miniMap != null) {
            miniMap.updateViewport(zoomFactor, centerLat, centerLng);
        }
    }
    /**
     * Draw subtle coordinate overlay badge (Center Lat/Lng, Bounds, Zoom) on canvas.
     */
    private void drawCoordinateOverlay(GraphicsContext gc) {
        double viewTopLat = maxLat - (0 - offsetY) / scale;
        double viewBottomLat = maxLat - (getHeight() - offsetY) / scale;
        double viewLeftLng = minLng + (0 - offsetX) / scale;
        double viewRightLng = minLng + (getWidth() - offsetX) / scale;

        double cTopLat = Math.min(90.0, Math.max(-90.0, viewTopLat));
        double cBotLat = Math.min(90.0, Math.max(-90.0, viewBottomLat));
        double cLeftLng = Math.min(180.0, Math.max(-180.0, viewLeftLng));
        double cRightLng = Math.min(180.0, Math.max(-180.0, viewRightLng));

        String coordsText = String.format(
            "📍 Centre: %.2f°N, %.2f°E | Bornes: [%.2f°N, %.2f°E] → [%.2f°N, %.2f°E] | Zoom: %.1fx",
            centerLat, centerLng, cTopLat, cLeftLng, cBotLat, cRightLng, zoomFactor
        );

        gc.setFont(javafx.scene.text.Font.font("Consolas", 11));
        gc.setFill(Color.rgb(15, 23, 42, 0.8));
        gc.fillRoundRect(12, 12, 540, 24, 8, 8);
        gc.setStroke(Color.rgb(56, 189, 248, 0.4));
        gc.setLineWidth(1);
        gc.strokeRoundRect(12, 12, 540, 24, 8, 8);

        gc.setFill(Color.rgb(226, 232, 240));
        gc.fillText(coordsText, 20, 28);
    }

    private void drawContours(GraphicsContext gc) {
        if (cells == null || cellMap == null) return;
        
        gc.setStroke(Color.rgb(56, 189, 248, 0.75));
        gc.setLineWidth(Math.max(1.0, 1.2 * Math.sqrt(zoomFactor)));
        
        double maxElev = 0;
        for (H3Cell c : cells) {
            double e = c.getElevation() != null ? c.getElevation() : 0;
            if (e > maxElev) maxElev = e;
        }
        double step = (maxElev > 100) ? Math.min(500.0, maxElev / 8.0) : 50.0;
        if (step < 10) step = 10;
        
        // Optimize: Iterate only visible cells if possible, but O(N) is fast enough for N<100k
        for (H3Cell cell : cells) {
            // Culling for 2D
             if (viewMode == ViewMode.VIEW_2D) {
                 double lat = cell.getLatitude();
                 double top = maxLat - (0 - offsetY) / scale;
                 double bottom = maxLat - (getHeight() - offsetY) / scale;
                 if (lat > top + 10 || lat < bottom - 10) continue;
            }

            double elev = cell.getElevation() != null ? cell.getElevation() : 0;
            int level = (int) (elev / step);
            
            try {
                // We need to check all neighbors to find boundaries
                List<Long> neighbors = h3Service.getNeighbors(cell.getH3Index());
                for (Long nIdx : neighbors) {
                    H3Cell neighbor = cellMap.get(nIdx);
                    // If neighbor is null (out of map) or has different level -> Draw edge
                    // Logic: Draw edge if level > nLevel (to draw once) OR neighbor is null (map edge)
                    
                    int nLevel = -1000;
                    if (neighbor != null) {
                        double nElev = neighbor.getElevation() != null ? neighbor.getElevation() : 0;
                        nLevel = (int) (nElev / step);
                    }
                    
                    if (level > nLevel) {
                        long edge = h3Service.getDirectedEdge(cell.getH3Index(), nIdx);
                        List<LatLng> boundary = h3Service.getEdgeBoundary(edge);
                        
                        if (boundary != null && boundary.size() >= 2) {
                            if (viewMode == ViewMode.VIEW_2D) {
                                LatLng p1 = boundary.get(0);
                                LatLng p2 = boundary.get(1);
                                
                                double x1 = (p1.lng - minLng) * scale + offsetX;
                                double y1 = (maxLat - p1.lat) * scale + offsetY;
                                double x2 = (p2.lng - minLng) * scale + offsetX;
                                double y2 = (maxLat - p2.lat) * scale + offsetY;
                                
                                gc.strokeLine(x1, y1, x2, y2);
                            } else if (viewMode == ViewMode.VIEW_3D) {
                                double radius = Math.min(getWidth(), getHeight()) * 0.45 * zoomFactor;
                                double cx = getWidth() / 2;
                                double cy = getHeight() / 2;
                                double radRotationY = Math.toRadians(-centerLng - 90);
                                double radTilt = Math.toRadians(15 + centerLat * 0.5);

                                LatLng p1 = boundary.get(0);
                                LatLng p2 = boundary.get(1);

                                ProjectedPoint pp1 = project3D(p1.lat, p1.lng, level * step, radRotationY, radTilt, radius, cx, cy);
                                ProjectedPoint pp2 = project3D(p2.lat, p2.lng, level * step, radRotationY, radTilt, radius, cx, cy);

                                if (pp1 != null && pp2 != null) {
                                    gc.strokeLine(pp1.screenX, pp1.screenY, pp2.screenX, pp2.screenY);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore H3 errors
            }
        }
    }

    private static class ProjectedPoint {
        double screenX, screenY, z;
    }

    private ProjectedPoint project3D(double lat, double lng, double elevation, double radRotationY, double radTilt, double radius, double cx, double cy) {
        double latRad = Math.toRadians(lat);
        double lngRad = Math.toRadians(lng);

        // Spherical to Cartesian
        double x = Math.cos(latRad) * Math.sin(lngRad);
        double y = Math.sin(latRad);
        double z = Math.cos(latRad) * Math.cos(lngRad);

        // Apply Y-axis rotation (Longitude)
        double cosR = Math.cos(radRotationY);
        double sinR = Math.sin(radRotationY);
        double xr = x * cosR + z * sinR;
        double zr = -x * sinR + z * cosR;

        // Apply X-axis tilt (Latitude)
        double cosT = Math.cos(radTilt);
        double sinT = Math.sin(radTilt);
        double yrt = y * cosT - zr * sinT;
        double zrt = y * sinT + zr * cosT;

        // Backface culling
        if (zrt <= -0.1) return null;

        // Screen projection
        double r = radius * (1.0 + elevation * ELEVATION_SCALE * 0.00001);

        ProjectedPoint p = new ProjectedPoint();
        p.screenX = cx + xr * r;
        p.screenY = cy - yrt * r;
        p.z = zrt;
        return p;
    }

    private void drawLegendOverlay(GraphicsContext gc) {
        if (cells == null || cells.isEmpty()) return;

        double w = getWidth();
        double h = getHeight();

        double legendWidth = 250.0;
        double legendHeight = 65.0;
        double margin = 15.0;
        double lx = w - legendWidth - margin;
        double ly = h - legendHeight - margin;

        // Background box with glassmorphism styling
        gc.setFill(Color.rgb(15, 23, 42, 0.88));
        gc.fillRoundRect(lx, ly, legendWidth, legendHeight, 10, 10);
        gc.setStroke(Color.rgb(56, 189, 248, 0.45));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(lx, ly, legendWidth, legendHeight, 10, 10);

        // Header label
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("SansSerif", javafx.scene.text.FontWeight.BOLD, 10));
        gc.fillText("LÉGENDE — " + displayMode.name(), lx + 10, ly + 15);

        // Compute stats (Min, Max, Mean, Median)
        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;
        double sum = 0.0;
        double[] values = new double[cells.size()];

        for (int i = 0; i < cells.size(); i++) {
            double v = getCellDisplayValue(cells.get(i), i);
            values[i] = v;
            if (v < minVal) minVal = v;
            if (v > maxVal) maxVal = v;
            sum += v;
        }

        if (minVal == Double.MAX_VALUE) minVal = 0.0;
        if (maxVal == -Double.MAX_VALUE) maxVal = 1.0;
        double meanVal = sum / cells.size();

        java.util.Arrays.sort(values);
        double medianVal = (values.length % 2 == 0)
                ? (values[values.length / 2 - 1] + values[values.length / 2]) / 2.0
                : values[values.length / 2];

        // Spectrum color bar
        double barX = lx + 10;
        double barY = ly + 24;
        double barWidth = legendWidth - 20;
        double barHeight = 12;

        javafx.scene.paint.LinearGradient grad = new javafx.scene.paint.LinearGradient(
                barX, barY, barX + barWidth, barY, false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.0, Color.rgb(30, 58, 138)),
                new javafx.scene.paint.Stop(0.25, Color.rgb(6, 182, 212)),
                new javafx.scene.paint.Stop(0.50, Color.rgb(34, 197, 94)),
                new javafx.scene.paint.Stop(0.75, Color.rgb(234, 179, 8)),
                new javafx.scene.paint.Stop(1.0, Color.rgb(239, 68, 68))
        );

        gc.setFill(grad);
        gc.fillRoundRect(barX, barY, barWidth, barHeight, 4, 4);
        gc.setStroke(Color.rgb(255, 255, 255, 0.3));
        gc.strokeRoundRect(barX, barY, barWidth, barHeight, 4, 4);

        // Min & Max text labels
        gc.setFont(javafx.scene.text.Font.font("SansSerif", 9));
        gc.setFill(Color.rgb(148, 163, 184));
        gc.fillText(String.format("%.1f", minVal), barX, ly + 52);
        String maxStr = String.format("%.1f", maxVal);
        gc.fillText(maxStr, barX + barWidth - (maxStr.length() * 5), ly + 52);

        // Range for normalization
        double range = Math.max(1e-6, maxVal - minVal);

        // Mean Indicator (Cyan triangle pointing down)
        double meanNorm = Math.clamp((meanVal - minVal) / range, 0.0, 1.0);
        double meanX = barX + meanNorm * barWidth;
        gc.setFill(Color.rgb(56, 189, 248));
        gc.fillPolygon(new double[]{meanX - 3, meanX + 3, meanX}, new double[]{barY - 5, barY - 5, barY}, 3);
        gc.fillText(String.format("μ:%.1f", meanVal), Math.clamp(meanX - 12, barX, barX + barWidth - 30), ly + 15);

        // Median Indicator (Amber triangle pointing up)
        double medNorm = Math.clamp((medianVal - minVal) / range, 0.0, 1.0);
        double medX = barX + medNorm * barWidth;
        gc.setFill(Color.rgb(245, 158, 11));
        gc.fillPolygon(new double[]{medX - 3, medX + 3, medX}, new double[]{barY + barHeight + 5, barY + barHeight + 5, barY + barHeight}, 3);
        gc.fillText(String.format("M:%.1f", medianVal), Math.clamp(medX - 12, barX + 30, barX + barWidth - 30), ly + 52);
    }

    private double getCellDisplayValue(H3Cell cell, int index) {
        if (worldBuffer != null && index >= 0 && index < worldBuffer.getCapacity()) {
            return switch (displayMode) {
                case POPULATION -> worldBuffer.getBiomassHuman()[index];
                case FOOD -> worldBuffer.getFoodResource()[index];
                case TEMPERATURE -> worldBuffer.getTemperature()[index];
                case TECHNOLOGY -> worldBuffer.getTechnologyLevel()[index];
                case WATER -> worldBuffer.getWaterResource()[index];
                case WOOD -> worldBuffer.getWoodResource()[index];
                case INEQUALITY -> worldBuffer.getGiniIndex()[index];
                case FLUX -> worldBuffer.getLocalPrice()[index];
                default -> cell != null && cell.getElevation() != null ? cell.getElevation() : 0.0;
            };
        }
        if (cell == null) return 0.0;
        return switch (displayMode) {
            case POPULATION -> cell.getPopulation() != null ? cell.getPopulation().doubleValue() : 0.0;
            case FOOD -> cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            case TEMPERATURE -> cell.getTemperature() != null ? cell.getTemperature() : 15.0;
            case TECHNOLOGY -> cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
            case WATER -> cell.getWaterResource() != null ? cell.getWaterResource() : 0.0;
            case WOOD -> cell.getWoodResource() != null ? cell.getWoodResource() : 0.0;
            case INEQUALITY -> cell.getGiniIndex() != null ? cell.getGiniIndex() : 0.3;
            case FLUX -> cell.getFluxPressure();
            case ALBEDO -> cell.getDynamicAlbedo() != null ? cell.getDynamicAlbedo() : 0.30;
            case FRICTION -> cell.getMovementFriction() != null ? cell.getMovementFriction() : 1.0;
            case EPIDEMIC -> cell.getEpidemicInfected() != null ? cell.getEpidemicInfected().doubleValue() : 0.0;
            case AGE_PYRAMID -> (cell.getPop65to79() != null && cell.getPop80Plus() != null) ? (double)(cell.getPop65to79() + cell.getPop80Plus()) : 0.0;
            case ASABIYYAH -> cell.getOwner() != null ? cell.getOwner().getAsabiyyah() : 0.0;
            default -> cell.getElevation() != null ? cell.getElevation() : 0.0;
        };
    }
}
