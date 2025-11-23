/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package com.ether.society.h3;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;

import java.io.IOException;
import java.util.List;

/**
 * H3 geospatial service for hexagon operations.
 * Provides utilities for working with H3 hexagonal grid system.
 *
 * <p>
 * This service uses Uber's H3 library for geospatial indexing at Level 8
 * (~0.74 km² per hexagon, ~7.2 million hexagons covering Earth).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 2.0.0
 */
public class H3Service {
    private final H3Core h3;

    /**
     * Primary resolution level (~1 km² per hexagon).
     */
    public static final int RESOLUTION_LEVEL_8 = 8;

    /**
     * Coarser levels for aggregation.
     */
    public static final int RESOLUTION_LEVEL_7 = 7;
    public static final int RESOLUTION_LEVEL_6 = 6;
    public static final int RESOLUTION_LEVEL_4 = 4;

    public H3Service() {
        try {
            this.h3 = H3Core.newInstance();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize H3Core", e);
        }
    }

    /**
     * Converts latitude/longitude to H3 cell index at Level 8.
     *
     * @param lat Latitude in degrees
     * @param lng Longitude in degrees
     * @return H3 cell index (long)
     */
    public long latLngToH3(double lat, double lng) {
        return h3.latLngToCell(lat, lng, RESOLUTION_LEVEL_8);
    }

    /**
     * Converts H3 cell index to lat/lng center point.
     *
     * @param h3Index H3 cell index
     * @return LatLng of cell center
     */
    public LatLng h3ToLatLng(long h3Index) {
        return h3.cellToLatLng(h3Index);
    }

    /**
     * Gets the 6 neighboring hexagons (H3 has 6-sided hexagons).
     *
     * @param h3Index H3 cell index
     * @return List of neighbor H3 indices
     */
    public List<Long> getNeighbors(long h3Index) {
        return h3.gridDisk(h3Index, 1);
    }

    /**
     * Gets all hexagons within k-ring distance.
     *
     * @param h3Index Center H3 cell
     * @param k       Distance (1 = immediate neighbors, 2 = 2 rings, etc.)
     * @return List of H3 indices in the ring
     */
    public List<Long> getKRing(long h3Index, int k) {
        return h3.gridDisk(h3Index, k);
    }

    /**
     * Gets the parent hexagon at a coarser resolution.
     *
     * @param h3Index    Child H3 cell (e.g., Level 8)
     * @param resolution Parent resolution (e.g., Level 7, 6, 4)
     * @return Parent H3 index
     */
    public long getParent(long h3Index, int resolution) {
        return h3.cellToParent(h3Index, resolution);
    }

    /**
     * Gets all children hexagons at a finer resolution.
     *
     * @param h3Index    Parent H3 cell
     * @param resolution Child resolution
     * @return List of children H3 indices
     */
    public List<Long> getChildren(long h3Index, int resolution) {
        return h3.cellToChildren(h3Index, resolution);
    }

    /**
     * Calculates the distance between two H3 cells in hexagons.
     *
     * @param h3Index1 First H3 cell
     * @param h3Index2 Second H3 cell
     * @return Grid distance (number of hexagons)
     */
    public int gridDistance(long h3Index1, long h3Index2) {
        return h3.gridDistance(h3Index1, h3Index2);
    }

    /**
     * Gets the resolution level of an H3 index.
     *
     * @param h3Index H3 cell index
     * @return Resolution level (0-15)
     */
    public int getResolution(long h3Index) {
        return h3.getResolution(h3Index);
    }

    /**
     * Checks if an H3 index is valid.
     *
     * @param h3Index H3 cell index
     * @return true if valid
     */
    public boolean isValid(long h3Index) {
        return h3.isValidCell(h3Index);
    }
}
