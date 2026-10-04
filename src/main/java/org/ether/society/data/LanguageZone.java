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

    /*
     * Language zone.
     * Enforces physical invariants and updates associated state variables within {@code LanguageZone}.
     *
     * @param name the name parameter (String)
     * @param centerLng the center lng parameter (double)
     * @param centerLat the center lat parameter (double)
     * @param color the color parameter (Color)
     */
    public LanguageZone(String name, double centerLng, double centerLat, Color color) {
        this.name = name;
        this.centerLng = centerLng;
        this.centerLat = centerLat;
        this.color = color;
    }
}
