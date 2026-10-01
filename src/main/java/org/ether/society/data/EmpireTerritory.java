/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class EmpireTerritory {
    public String name;
    public Color color;
    public List<double[]> bounds = new ArrayList<>();

    public EmpireTerritory(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public void addBoundingBox(double minLng, double minLat, double maxLng, double maxLat) {
        bounds.add(new double[]{minLng, minLat, maxLng, maxLat});
    }

    public boolean contains(double lng, double lat) {
        for (double[] b : bounds) {
            if (lng >= b[0] && lng <= b[2] && lat >= b[1] && lat <= b[3]) return true;
        }
        return false;
    }
}
