/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.Color;

public class LanguageZone {
    public String name;
    public double centerLng, centerLat;
    public Color color;

    public LanguageZone(String name, double centerLng, double centerLat, Color color) {
        this.name = name;
        this.centerLng = centerLng;
        this.centerLat = centerLat;
        this.color = color;
    }
}
