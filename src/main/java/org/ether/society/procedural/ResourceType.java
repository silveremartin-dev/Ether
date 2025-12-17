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
    COAL("Coal", javafx.scene.paint.Color.BLACK),
    OIL("Oil", javafx.scene.paint.Color.DARKSLATEGRAY),
    GOLD("Gold", javafx.scene.paint.Color.GOLD);

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

