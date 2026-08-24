/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

/**
 * Resource types for density map painting.
 */
public enum ResourceType {
    FOOD("Food", javafx.scene.paint.Color.YELLOWGREEN),
    WOOD("Wood", javafx.scene.paint.Color.BROWN),
    STONE("Stone", javafx.scene.paint.Color.GRAY),
    COPPER("Copper", javafx.scene.paint.Color.ORANGE),
    IRON("Iron", javafx.scene.paint.Color.DARKGRAY),
    IRON_COPPER("Iron & Copper", javafx.scene.paint.Color.web("#f97316")),
    COAL("Coal", javafx.scene.paint.Color.BLACK),
    OIL("Oil", javafx.scene.paint.Color.DARKSLATEGRAY),
    CRUDE_OIL("Crude Oil & Fuel", javafx.scene.paint.Color.web("#dc2626")),
    GOLD("Gold", javafx.scene.paint.Color.GOLD),
    NATURAL_GAS("Natural Gas", javafx.scene.paint.Color.CYAN),
    URANIUM("Uranium Ore", javafx.scene.paint.Color.LIMEGREEN),
    THORIUM("Thorium Ore", javafx.scene.paint.Color.web("#10b981")),
    HELIUM_3("Helium-3", javafx.scene.paint.Color.MAGENTA),
    HYDROGEN("Hydrogen", javafx.scene.paint.Color.DEEPSKYBLUE),
    FRESHWATER_AQUIFER("Freshwater Aquifers", javafx.scene.paint.Color.DODGERBLUE),
    RARE_EARTHS("Rare Earth Elements", javafx.scene.paint.Color.PURPLE),
    LITHIUM("Lithium Brines & Spodumene", javafx.scene.paint.Color.web("#a855f7")),
    BAUXITE("Bauxite / Aluminum Crusts", javafx.scene.paint.Color.web("#d97706"));

    private final String displayName;
    private final javafx.scene.paint.Color color;

    ResourceType(String displayName, javafx.scene.paint.Color color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public javafx.scene.paint.Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

