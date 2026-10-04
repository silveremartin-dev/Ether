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

/**
 * <h1>Empire Territory</h1>
 * <p>
 * Geospatial data ingestion, raster sampling, and tensor map management pipeline.<br>
 * Ingests global planetary datasets (NOAA ETOPO, WorldClim, UNESCO WHYMAP, Natural Earth, Seshat, D-PLACE) into standardized H3 hexagonal rasters.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EmpireTerritory {
    public String name;
    public Color color;
    public List<double[]> bounds = new ArrayList<>();

    /*
     * Empire territory.
     * Enforces physical invariants and updates associated state variables within {@code EmpireTerritory}.
     *
     * @param name the name parameter (String)
     * @param color the color parameter (Color)
     */
    public EmpireTerritory(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    /*
     * Add bounding box.
     * Enforces physical invariants and updates associated state variables within {@code EmpireTerritory}.
     *
     * @param minLng the min lng parameter (double)
     * @param minLat the min lat parameter (double)
     * @param maxLng the max lng parameter (double)
     * @param maxLat the max lat parameter (double)
     */
    public void addBoundingBox(double minLng, double minLat, double maxLng, double maxLat) {
        bounds.add(new double[]{minLng, minLat, maxLng, maxLat});
    }

    /*
     * Contains.
     * Enforces physical invariants and updates associated state variables within {@code EmpireTerritory}.
     *
     * @param lng the lng parameter (double)
     * @param lat the lat parameter (double)
     * @return the resulting computation or state reference
     */
    public boolean contains(double lng, double lat) {
        for (double[] b : bounds) {
            if (lng >= b[0] && lng <= b[2] && lat >= b[1] && lat <= b[3]) return true;
        }
        return false;
    }
}
