/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.database.H3Cell;
import org.ether.society.events.ActiveEvent;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Biome;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Tooltip panel that displays detailed information about an H3 cell.
 * Shows when hovering over cells in the map view.
 */
public class CellTooltip extends VBox {

    private final Label biomeLabel;
    private final Label elevationLabel;
    private final Label temperatureLabel;
    private final Label rainfallLabel;
    private final Label popLabel;
    private final Label densityLabel;
    private final Label foodLabel;
    private final Label waterLabel;
    private final Label techLabel;
    private final Label wealthVarianceLabel;
    private final Label malthusLabel;
    private final Label eventLabel;
    private final Label coordLabel;
    private final Label h3Label;

    public CellTooltip() {
        // Container styling - frosted glass dark HUD panel
        setStyle("-fx-background-color: rgba(15, 23, 42, 0.88);" +
                "-fx-padding: 8 12;" +
                "-fx-background-radius: 8;" +
                "-fx-border-color: rgba(56, 189, 248, 0.40);" +
                "-fx-border-radius: 8;" +
                "-fx-border-width: 1.2;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0, 0, 3);");

        setPadding(new Insets(10, 14, 10, 14));
        setSpacing(4);
        setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        setPrefWidth(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);

        // Create labels with consistent styling
        biomeLabel = createLabel();
        biomeLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        elevationLabel = createLabel();
        temperatureLabel = createLabel();
        rainfallLabel = createLabel();
        
        popLabel = createLabel();
        popLabel.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        
        densityLabel = createLabel();
        foodLabel = createLabel();
        waterLabel = createLabel();
        techLabel = createLabel();
        
        wealthVarianceLabel = createLabel();
        wealthVarianceLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-size: 11px; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        
        malthusLabel = createLabel();
        
        eventLabel = createLabel();
        eventLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-size: 11px; -fx-font-weight: bold; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        eventLabel.setVisible(false);
        eventLabel.setManaged(false);

        coordLabel = createLabel();
        coordLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        
        h3Label = createLabel();
        h3Label.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-family: 'Consolas', 'Monaco', monospace;");

        // Add all labels to container
        getChildren().addAll(
                biomeLabel,
                elevationLabel,
                temperatureLabel,
                rainfallLabel,
                popLabel,
                densityLabel,
                foodLabel,
                waterLabel,
                techLabel,
                wealthVarianceLabel,
                malthusLabel,
                eventLabel,
                coordLabel,
                h3Label);

        // Initially hidden and non-blocking for mouse events
        setVisible(false);
        setManaged(false);
        setMouseTransparent(true);
    }

    private Label createLabel() {
        Label label = new Label();
        label.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        label.setWrapText(false);
        label.setEllipsisString("");
        label.setStyle("-fx-text-fill: #e2e8f0;" +
                "-fx-font-size: 11px;" +
                "-fx-font-family: 'Consolas', 'Monaco', monospace;");
        return label;
    }

    /**
     * Unified comprehensive cell update method.
     */
    public void updateCell(H3Cell cell, org.ether.society.core.dod.WorldBuffer world, Integer bufferIndex, Integer prevPop, List<ActiveEvent> activeEvents) {
        if (cell == null && (world == null || bufferIndex == null || bufferIndex < 0)) {
            setVisible(false);
            return;
        }

        // Biome
        Biome biome = cell != null ? cell.getBiome() : null;
        if (biome == null && world != null && bufferIndex != null && bufferIndex >= 0 && bufferIndex < world.getCapacity()) {
            biome = Biome.values()[world.getBiomes()[bufferIndex]];
        }
        if (biome == null) biome = Biome.PLAINS;
        biomeLabel.setText(String.format("%-14s %s %s", I18n.getOrDefault("ui.tooltip.biome", "Biomes :"),
                getBiomeIcon(biome), getBiomeName(biome.name())));

        // Elevation
        int elev = 0;
        if (world != null && bufferIndex != null && bufferIndex >= 0 && bufferIndex < world.getCapacity()) {
            elev = (int) world.getElevation()[bufferIndex];
        } else if (cell != null && cell.getElevation() != null) {
            elev = cell.getElevation().intValue();
        }
        elevationLabel.setText(String.format("%-14s %,d m", I18n.getOrDefault("ui.tooltip.elevation", "Altitude :"), elev));

        // Temperature
        double temp = 15.0;
        if (world != null && bufferIndex != null && bufferIndex >= 0 && bufferIndex < world.getCapacity()) {
            temp = world.getTemperature()[bufferIndex];
        } else if (cell != null && cell.getTemperature() != null) {
            temp = cell.getTemperature();
        }
        temperatureLabel.setText(String.format("%-14s %.1f°C", I18n.getOrDefault("ui.tooltip.temperature", "Temperature:"), temp));

        // Rainfall
        int rain = 800;
        if (world != null && bufferIndex != null && bufferIndex >= 0 && bufferIndex < world.getCapacity()) {
            rain = (int) world.getRainfall()[bufferIndex];
        } else if (cell != null && cell.getRainfall() != null) {
            rain = cell.getRainfall().intValue();
        }
        rainfallLabel.setText(String.format("%-14s %,d mm/an", I18n.getOrDefault("ui.tooltip.rainfall", "Precipitation:"), rain));

        // Population
        int currentPop = 0;
        if (world != null && bufferIndex != null && bufferIndex >= 0 && bufferIndex < world.getCapacity()) {
            currentPop = (int) world.getBiomassHuman()[bufferIndex];
        } else if (cell != null && cell.getPopulation() != null) {
            currentPop = cell.getPopulation();
        }

        String trendStr = "";
        if (prevPop != null && prevPop > 0) {
            int diff = currentPop - prevPop;
            double pct = ((double) diff / prevPop) * 100.0;
            if (diff > 0) {
                trendStr = String.format(" (🟢 ⬆ +%,d | +%.1f%%)", diff, pct);
            } else if (diff < 0) {
                trendStr = String.format(" (🔴 ⬇ %,d | %.1f%%)", diff, pct);
            } else {
                trendStr = " (⚪ ➡ 0.0%)";
            }
        }
        popLabel.setText(String.format("%-14s %,d hab%s", "Population :", currentPop, trendStr));

        // Density (H3 Level 8 cell area ~ 0.737 km²)
        double density = currentPop / 0.737;
        densityLabel.setText(String.format("%-14s %,.1f hab/km²", "Densité :", density));

        // Resources
        double food = 0.0;
        double water = 0.0;
        if (world != null && bufferIndex != null && bufferIndex >= 0 && bufferIndex < world.getCapacity()) {
            food = world.getFoodResource()[bufferIndex];
            water = world.getWaterResource()[bufferIndex];
        } else if (cell != null) {
            food = cell.getFoodResource() != null ? cell.getFoodResource() : 0.0;
            water = cell.getWaterResource() != null ? cell.getWaterResource() : 0.0;
        }
        foodLabel.setText(String.format("%-14s %.1f t", "Alimentation :", food));

        double aquifer = cell != null && cell.getFreshwaterAquifer() != null ? cell.getFreshwaterAquifer() : 0.0;
        waterLabel.setText(String.format("%-14s %.1f m³ (Aquifère: %.0f)", I18n.getOrDefault("tooltip.water_aquifer", "Eau / Aquifère:"), water, aquifer));

        // Tech Level
        double tech = 1.0;
        if (world != null && bufferIndex != null && bufferIndex >= 0 && bufferIndex < world.getCapacity() && world.getTechnologyLevel()[bufferIndex] > 0) {
            tech = world.getTechnologyLevel()[bufferIndex];
        } else if (cell != null && cell.getTechnologyLevel() != null && cell.getTechnologyLevel() > 0) {
            tech = cell.getTechnologyLevel();
        }
        techLabel.setText(String.format("%-14s Niv. %.2f", "Technologie :", tech));

        // Wealth
        double wealth = currentPop * tech * 15.0;
        wealthVarianceLabel.setText(String.format("%-14s G$ %,.0f", I18n.getOrDefault("tooltip.mesh_wealth", "Richesse PIB :"), wealth));

        // Carrying Capacity (K) & Malthusian Ratio
        double capK = computeCarryingCapacity(biome, elev, water, aquifer, tech);
        double ratio = capK > 0 ? ((double) currentPop / capK) * 100.0 : 0.0;
        String malthusStatus = ratio > 150.0 ? "🚨 SURPOPULATION" : ratio > 100.0 ? "⚠️ TENSION" : "✅ SOUTENABLE";
        malthusLabel.setText(String.format("%-14s %.0f hab (Charge: %.1f%% %s)", I18n.getOrDefault("tooltip.capacity_k", "Capacité K :"), capK, ratio, malthusStatus));

        // Coordinates & H3
        double lat = cell != null ? cell.getLatitude() : 0.0;
        double lng = cell != null ? cell.getLongitude() : 0.0;
        long h3Idx = 0L;
        if (cell != null) {
            h3Idx = cell.getH3Index();
        } else if (world != null && bufferIndex != null && bufferIndex >= 0 && bufferIndex < world.getCapacity()) {
            h3Idx = world.getH3Indexes()[bufferIndex];
        }

        // Active events nearby
        if (activeEvents != null && !activeEvents.isEmpty()) {
            ActiveEvent nearestEvt = null;
            for (ActiveEvent ev : activeEvents) {
                if (!ev.isExpired()) {
                    double dLat = Math.abs(ev.getLatitude() - lat);
                    double dLng = Math.abs(ev.getLongitude() - lng);
                    if (dLat < 6.0 && dLng < 6.0) {
                        nearestEvt = ev;
                        break;
                    }
                }
            }
            if (nearestEvt != null) {
                eventLabel.setText(String.format("🚨 %s (Mag %.1f)", nearestEvt.getTitle(), nearestEvt.getMagnitude()));
                eventLabel.setVisible(true);
                eventLabel.setManaged(true);
            } else {
                eventLabel.setVisible(false);
                eventLabel.setManaged(false);
            }
        } else {
            eventLabel.setVisible(false);
            eventLabel.setManaged(false);
        }

        String latDir = lat >= 0 ? "N" : "S";
        String lngDir = lng >= 0 ? "E" : "W";
        coordLabel.setText(String.format("%-14s %.4f°%s, %.4f°%s", I18n.getOrDefault("ui.tooltip.coords", "Coordonnées :"),
                Math.abs(lat), latDir,
                Math.abs(lng), lngDir));

        h3Label.setText(String.format("%-14s %s", I18n.getOrDefault("ui.tooltip.h3", "H3 Index :"),
                Long.toHexString(h3Idx).toUpperCase()));

        applyCss();
        layout();
        autosize();
        setVisible(true);
    }

    public void updateCell(H3Cell cell, Integer prevPop) {
        updateCell(cell, null, null, prevPop, null);
    }

    public void updateCell(H3Cell cell) {
        updateCell(cell, null, null, null, null);
    }

    public void updateFromBuffer(org.ether.society.core.dod.WorldBuffer world, int index) {
        updateCell(null, world, index, null, null);
    }

    private double computeCarryingCapacity(Biome biome, int elevation, double water, double aquifer, double tech) {
        if (elevation <= 0 && biome == Biome.OCEAN) return 0.0;
        double baseCap = 250.0;
        if (biome != null) {
            switch (biome) {
                case DESERT, TUNDRA, SNOW -> baseCap *= 0.1;
                case PLAINS, FOREST -> baseCap *= 1.5;
                case JUNGLE -> baseCap *= 0.8;
                case OCEAN -> baseCap = 0.0;
                default -> {}
            }
        }
        if (water > 0.1) {
            baseCap *= (1.0 + 3.0 * (water / 1000.0));
        }
        if (aquifer > 0.1) {
            baseCap *= (1.0 + 1.5 * (aquifer / 1000.0));
        }
        baseCap *= Math.max(0.5, tech * 0.8);
        return Math.max(10.0, baseCap);
    }

    private String getBiomeName(String biomeEnumName) {
        String key = "biome." + biomeEnumName.toLowerCase();
        return I18n.getOrDefault(key, biomeEnumName);
    }

    private String getBiomeIcon(Biome biome) {
        if (biome == null) return "🌍";
        return switch (biome) {
            case OCEAN -> "🌊";
            case PLAINS -> "🌾";
            case FOREST -> "🌲";
            case DESERT -> "🏜️";
            case SNOW, TUNDRA -> "❄️";
            case JUNGLE -> "🌴";
            case MOUNTAINS -> "⛰️";
            default -> "🌍";
        };
    }

    public void position(double canvasX, double canvasY, double maxX, double maxY) {
        applyCss();
        layout();
        autosize();

        double offsetX = 16;
        double offsetY = 16;

        double width = prefWidth(-1);
        if (width <= 0) width = getWidth() > 0 ? getWidth() : 320;
        double height = prefHeight(-1);
        if (height <= 0) height = getHeight() > 0 ? getHeight() : 250;

        resize(width, height);

        double x = canvasX + offsetX;
        double y = canvasY + offsetY;

        if (x + width > maxX - 10) {
            x = canvasX - width - offsetX;
        }
        if (y + height > maxY - 10) {
            y = canvasY - height - offsetY;
        }

        x = Math.max(10, Math.min(x, Math.max(10, maxX - width - 10)));
        y = Math.max(10, Math.min(y, Math.max(10, maxY - height - 10)));

        relocate(x, y);
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }
}
