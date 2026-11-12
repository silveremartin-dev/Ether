/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.resource;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.ether.society.i18n.I18n;

/**
 * <h1>ResourcePreviewLegendBuilder</h1>
 * <p>
 * Builder responsible for generating and rendering localized color swatches and
 * thematic legend indicators for the cartographic planetary resource preview pane.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public final class ResourcePreviewLegendBuilder {

    private ResourcePreviewLegendBuilder() {
        // Utility / Helper class
    }

    /**
     * Updates the horizontal color legend bar matching the active view mode.
     *
     * @param legendBar   the target HBox container
     * @param selectedIdx the thematic layer index (0..14)
     */
    public static void updateLegend(HBox legendBar, int selectedIdx) {
        if (legendBar == null) return;
        legendBar.getChildren().clear();

        if (selectedIdx == 0) { // Biomes
            addLegendItem(legendBar, "DEEP_OCEAN", Color.rgb(0, 0, 100), I18n.getOrDefault("resource.legend.deep_ocean", "Deep Ocean / Abyss"));
            addLegendItem(legendBar, "OCEAN", Color.rgb(0, 50, 200), I18n.getOrDefault("resource.legend.ocean", "Ocean / Continental Shelf"));
            addLegendItem(legendBar, "PLAINS", Color.rgb(100, 200, 50), I18n.getOrDefault("resource.legend.plains", "Grasslands & Plains"));
            addLegendItem(legendBar, "FOREST", Color.rgb(20, 120, 20), I18n.getOrDefault("resource.legend.forest", "Temperate & Boreal Forest"));
            addLegendItem(legendBar, "JUNGLE", Color.rgb(0, 80, 0), I18n.getOrDefault("resource.legend.jungle", "Tropical Rainforest / Jungle"));
            addLegendItem(legendBar, "DESERT", Color.rgb(255, 200, 50), I18n.getOrDefault("resource.legend.desert", "Arid Desert & Scrub"));
            addLegendItem(legendBar, "MOUNTAINS", Color.rgb(100, 100, 100), I18n.getOrDefault("resource.legend.mountains", "High Altitude / Mountains"));
        } else if (selectedIdx == 1) { // Hydrography & River Networks
            addLegendItem(legendBar, "DEEP_OCEAN", Color.rgb(15, 23, 42), I18n.getOrDefault("resource.legend.hydro_ocean", "Ocean / Abyss"));
            addLegendItem(legendBar, "MAJOR_RIVER", Color.rgb(2, 132, 199), I18n.getOrDefault("resource.legend.major_river", "Major Rivers"));
            addLegendItem(legendBar, "RIVER_STREAM", Color.rgb(56, 189, 248), I18n.getOrDefault("resource.legend.river_stream", "Streams & Watercourses"));
            addLegendItem(legendBar, "TRIBUTARY", Color.rgb(20, 184, 166), I18n.getOrDefault("resource.legend.tributary", "Tributaries & Drainage"));
            addLegendItem(legendBar, "LAND_SLOPE", Color.rgb(75, 85, 99), I18n.getOrDefault("resource.legend.land_slope", "Continental Relief"));
        } else if (selectedIdx == 2) { // COAL
            addLegendItem(legendBar, "LOW", Color.rgb(30, 30, 30), I18n.getOrDefault("resource.legend.coal_low", "Sterile / Traces"));
            addLegendItem(legendBar, "MED", Color.rgb(180, 100, 20), I18n.getOrDefault("resource.legend.coal_med", "Coal Basin"));
            addLegendItem(legendBar, "HIGH", Color.rgb(255, 140, 0), I18n.getOrDefault("resource.legend.coal_high", "Major Anthracite Deposit"));
        } else if (selectedIdx == 3) { // CRUDE OIL
            addLegendItem(legendBar, "LOW", Color.rgb(30, 20, 20), I18n.getOrDefault("resource.legend.oil_low", "Sterile / Traces"));
            addLegendItem(legendBar, "MED", Color.rgb(180, 40, 40), I18n.getOrDefault("resource.legend.oil_med", "Oil Field"));
            addLegendItem(legendBar, "HIGH", Color.rgb(220, 38, 38), I18n.getOrDefault("resource.legend.oil_high", "Supergiant Bituminous Basin"));
        } else if (selectedIdx == 4) { // NATURAL GAS
            addLegendItem(legendBar, "LOW", Color.rgb(10, 30, 40), I18n.getOrDefault("resource.legend.gas_low", "Sterile / Traces"));
            addLegendItem(legendBar, "MED", Color.rgb(20, 140, 160), I18n.getOrDefault("resource.legend.gas_med", "Gas Field"));
            addLegendItem(legendBar, "HIGH", Color.rgb(6, 182, 212), I18n.getOrDefault("resource.legend.gas_high", "Natural Gas Superfield"));
        } else if (selectedIdx == 5) { // URANIUM
            addLegendItem(legendBar, "LOW", Color.rgb(10, 40, 20), I18n.getOrDefault("resource.legend.uranium_low", "Traces"));
            addLegendItem(legendBar, "MED", Color.rgb(20, 140, 60), I18n.getOrDefault("resource.legend.uranium_med", "Uranium Ore Deposit"));
            addLegendItem(legendBar, "HIGH", Color.rgb(34, 197, 94), I18n.getOrDefault("resource.legend.uranium_high", "High-Grade Pitchblende"));
        } else if (selectedIdx == 6) { // HELIUM-3
            addLegendItem(legendBar, "LOW", Color.rgb(30, 10, 30), I18n.getOrDefault("resource.legend.he3_low", "Regolith Traces"));
            addLegendItem(legendBar, "MED", Color.rgb(140, 30, 140), I18n.getOrDefault("resource.legend.he3_med", "Enriched Regolith"));
            addLegendItem(legendBar, "HIGH", Color.rgb(217, 70, 239), I18n.getOrDefault("resource.legend.he3_high", "Lunar Helium-3 Concentrate"));
        } else if (selectedIdx == 7) { // IRON & COPPER
            addLegendItem(legendBar, "LOW", Color.rgb(40, 30, 20), I18n.getOrDefault("resource.legend.iron_low", "Low Grade"));
            addLegendItem(legendBar, "MED", Color.rgb(180, 90, 20), I18n.getOrDefault("resource.legend.iron_med", "Banded Iron Formation"));
            addLegendItem(legendBar, "HIGH", Color.rgb(249, 115, 22), I18n.getOrDefault("resource.legend.iron_high", "Massive Iron & Copper Deposit"));
        } else if (selectedIdx == 8) { // PRECIOUS METALS
            addLegendItem(legendBar, "LOW", Color.rgb(40, 35, 10), I18n.getOrDefault("resource.legend.precious_low", "Gold/Silver Traces"));
            addLegendItem(legendBar, "MED", Color.rgb(170, 130, 20), I18n.getOrDefault("resource.legend.precious_med", "Placer & Gold Vein"));
            addLegendItem(legendBar, "HIGH", Color.rgb(234, 179, 8), I18n.getOrDefault("resource.legend.precious_high", "Giant Gold/Silver Deposit"));
        } else if (selectedIdx == 9) { // RARE EARTHS & CRITICAL MINERALS
            addLegendItem(legendBar, "LOW", Color.rgb(10, 35, 35), I18n.getOrDefault("resource.legend.ree_low", "REE/Li Traces"));
            addLegendItem(legendBar, "MED", Color.rgb(20, 120, 110), I18n.getOrDefault("resource.legend.ree_med", "Carbonatite / Salar"));
            addLegendItem(legendBar, "HIGH", Color.rgb(45, 212, 191), I18n.getOrDefault("resource.legend.ree_high", "Major Rare Earth Basin (Bayan Obo)"));
        } else if (selectedIdx == 10) { // MANTLE HEAT FLUX
            addLegendItem(legendBar, "LOW", Color.rgb(40, 40, 60), I18n.getOrDefault("resource.legend.heat_low", "Inert / Stable Craton"));
            addLegendItem(legendBar, "MED", Color.rgb(180, 80, 30), I18n.getOrDefault("resource.legend.heat_med", "Mean Geothermal Flux"));
            addLegendItem(legendBar, "HIGH", Color.rgb(240, 20, 20), I18n.getOrDefault("resource.legend.heat_high", "High Magmatism & Tectonic Rifts"));
        } else if (selectedIdx == 11) { // AQUIFERS & FRESHWATER
            addLegendItem(legendBar, "LOW", Color.rgb(20, 40, 80), I18n.getOrDefault("resource.legend.aquifer_low", "Arid / Dry"));
            addLegendItem(legendBar, "MED", Color.rgb(40, 120, 200), I18n.getOrDefault("resource.legend.aquifer_med", "Moderate Aquifer"));
            addLegendItem(legendBar, "HIGH", Color.rgb(0, 220, 255), I18n.getOrDefault("resource.legend.aquifer_high", "Giant Basin Aquifer"));
        } else if (selectedIdx == 12) { // SURFACE TEMPERATURE & MICROCLIMATES
            addLegendItem(legendBar, "POLAR", Color.rgb(35, 120, 230), I18n.getOrDefault("resource.legend.temp_polar", "Polar / Glacial (< 0°C)"));
            addLegendItem(legendBar, "TEMPERATE", Color.rgb(80, 200, 120), I18n.getOrDefault("resource.legend.temp_temperate", "Temperate (10°C - 22°C)"));
            addLegendItem(legendBar, "TROPICAL", Color.rgb(250, 130, 20), I18n.getOrDefault("resource.legend.temp_tropical", "Tropical / Warm (22°C - 34°C)"));
            addLegendItem(legendBar, "TORRID", Color.rgb(245, 20, 40), I18n.getOrDefault("resource.legend.temp_torrid", "Torrid Extreme (> 34°C)"));
        } else if (selectedIdx == 13) { // SEISMIC & VOLCANIC TECTONISM
            addLegendItem(legendBar, "QUIET", Color.rgb(40, 45, 55), I18n.getOrDefault("resource.legend.seismic_quiet", "Stable Shield / Craton"));
            addLegendItem(legendBar, "MODERATE", Color.rgb(200, 100, 30), I18n.getOrDefault("resource.legend.seismic_med", "Active Fault / Orogeny"));
            addLegendItem(legendBar, "HIGH", Color.rgb(245, 30, 30), I18n.getOrDefault("resource.legend.seismic_high", "Subduction & Volcanic Arc"));
        } else if (selectedIdx == 14) { // ARIDITY & SOIL SALINIZATION
            addLegendItem(legendBar, "HUMID", Color.rgb(30, 140, 160), I18n.getOrDefault("resource.legend.aridity_humid", "Humid / Fertile Soils"));
            addLegendItem(legendBar, "SEMI_ARID", Color.rgb(210, 150, 50), I18n.getOrDefault("resource.legend.aridity_semi", "Semi-Arid Steppe"));
            addLegendItem(legendBar, "HYPER_ARID", Color.rgb(230, 60, 20), I18n.getOrDefault("resource.legend.aridity_hyper", "Hyper-Arid Salt Desert"));
        }
    }

    private static void addLegendItem(HBox legendBar, String id, Color col, String text) {
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
}
