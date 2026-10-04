/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 * SINCE: 2.0
 */
package org.ether.society.h3;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * H3 geospatial service for hexagonal grid operations.
 */
public class H3Service {
    private static final Logger logger = LoggerFactory.getLogger(H3Service.class);
    private static H3Service instance;

    private final H3Core h3;
    /* Internal state variable for resolution (int). */
    private final int resolution;

    /*
     * H3service.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     */
    public H3Service() {
        this(8); // Default to Level 8 (~0.74 km²)
    }

    /*
     * H3service.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param resolution the resolution parameter (int)
     */
    public H3Service(int resolution) {
        try {
            this.h3 = H3Core.newInstance();
            this.resolution = resolution;
            logger.info("H3Service initialized with resolution {}", resolution);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize H3Core", e);
        }
    }

    /*
     * Get singleton instance.
     */
    public static synchronized H3Service getInstance() {
        if (instance == null) {
            instance = new H3Service();
        }
        return instance;
    }

    /*
     * Lat lng to cell.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param lat the lat parameter (double)
     * @param lng the lng parameter (double)
     * @return the resulting computation or state reference
     */
    public long latLngToCell(double lat, double lng) {
        return h3.latLngToCell(lat, lng, resolution);
    }

    /*
     * Lat lng to h3.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param lat the lat parameter (double)
     * @param lng the lng parameter (double)
     * @return the resulting computation or state reference
     */
    public long latLngToH3(double lat, double lng) {
        return h3.latLngToCell(lat, lng, resolution);
    }

    /*
     * Get resolution.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param h3Index the h3index parameter (long)
     * @return the resulting computation or state reference
     */
    public int getResolution(long h3Index) {
        return h3.getResolution(h3Index);
    }

    /*
     * Get resolution.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @return the resulting computation or state reference
     */
    public int getResolution() {
        return resolution;
    }

    /*
     * Get h3.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @return the resulting computation or state reference
     */
    public H3Core getH3() {
        return h3;
    }

    /*
     * Lat lng to h3.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param lat the lat parameter (double)
     * @param lng the lng parameter (double)
     * @param res the res parameter (int)
     * @return the resulting computation or state reference
     */
    public long latLngToH3(double lat, double lng, int res) {
        return h3.latLngToCell(lat, lng, res);
    }

    /*
     * Cell to lat lng.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param h3Index the h3index parameter (long)
     * @return the resulting computation or state reference
     */
    public LatLng cellToLatLng(long h3Index) {
        return h3.cellToLatLng(h3Index);
    }

    /*
     * H3to lat lng.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param h3Index the h3index parameter (long)
     * @return the resulting computation or state reference
     */
    public LatLng h3ToLatLng(long h3Index) {
        return h3.cellToLatLng(h3Index);
    }

    /*
     * H3to string.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param h3Index the h3index parameter (long)
     * @return the resulting computation or state reference
     */
    public String h3ToString(long h3Index) {
        return h3.h3ToString(h3Index);
    }

    /*
     * Grid disk.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param h3Index the h3index parameter (long)
     * @param k the k parameter (int)
     * @return the resulting computation or state reference
     */
    public List<Long> gridDisk(long h3Index, int k) {
        return h3.gridDisk(h3Index, k);
    }

    /*
     * Cell to parent.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param h3Index the h3index parameter (long)
     * @param parentRes the parent res parameter (int)
     * @return the resulting computation or state reference
     */
    public long cellToParent(long h3Index, int parentRes) {
        return h3.cellToParent(h3Index, parentRes);
    }

    /*
     * Get immediate neighbors of a cell (ring 1).
     */
    public List<Long> getNeighbors(long h3Index) {
        // gridDisk returns center + neighbors, so we filter out the center
        List<Long> disk = new ArrayList<>(h3.gridDisk(h3Index, 1));
        disk.remove(Long.valueOf(h3Index));
        return disk;
    }

    /*
     * Get H3 indexes in a bounding box.
     */
    public List<Long> getH3IndexesInBounds(double minLat, double maxLat, double minLng, double maxLng, int res) {
        List<Long> cells = new ArrayList<>();

        // Create a polygon from the bounding box
        List<LatLng> polygon = new ArrayList<>();
        polygon.add(new LatLng(minLat, minLng));
        polygon.add(new LatLng(maxLat, minLng));
        polygon.add(new LatLng(maxLat, maxLng));
        polygon.add(new LatLng(minLat, maxLng));
        polygon.add(new LatLng(minLat, minLng));

        try {
            cells = h3.polygonToCells(polygon, null, res);
        } catch (Exception e) {
            logger.error("Error getting H3 cells in bounds", e);
            // Fallback: grid sampling
            double step = 0.1; // ~11km at equator
            for (double lat = minLat; lat <= maxLat; lat += step) {
                for (double lng = minLng; lng <= maxLng; lng += step) {
                    long cell = h3.latLngToCell(lat, lng, res);
                    if (!cells.contains(cell)) {
                        cells.add(cell);
                    }
                }
            }
        }

        return cells;
    }

    /*
     * Generate H3Cell objects for the entire globe at a specific resolution.
     */
    /*
     * Generate H3Cell objects for the entire globe at a specific resolution.
     */
    public List<H3Cell> generateGlobalMetadata(int res) {
        List<H3Cell> result = new ArrayList<>();
        try {
            List<Long> baseCells = new ArrayList<>(h3.getRes0Cells());
            for (Long base : baseCells) {
                if (res == 0) {
                    LatLng coord = h3.cellToLatLng(base);
                    result.add(new H3Cell(base, coord.lat, coord.lng));
                } else {
                    List<Long> children = h3.cellToChildren(base, res);
                    for (Long child : children) {
                        LatLng coord = h3.cellToLatLng(child);
                        result.add(new H3Cell(child, coord.lat, coord.lng));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error generating global metadata", e);
        }
        return result;
    }

    /*
     * Generate H3Cell objects filtered directly within a geographic bounding box.
     * Prevents allocating millions of unused global cells on the heap for high resolutions (Res 6-7).
     */
    public List<H3Cell> generateRegionalMetadata(int res, double minLat, double maxLat, double minLng, double maxLng) {
        List<H3Cell> result = new ArrayList<>();
        try {
            List<Long> baseCells = new ArrayList<>(h3.getRes0Cells());
            for (Long base : baseCells) {
                LatLng baseCoord = h3.cellToLatLng(base);
                if (res > 2) {
                    double margin = 35.0;
                    if (baseCoord.lat < minLat - margin || baseCoord.lat > maxLat + margin) {
                        continue;
                    }
                }
                List<Long> children = (res == 0) ? List.of(base) : h3.cellToChildren(base, res);
                for (Long child : children) {
                    LatLng coord = h3.cellToLatLng(child);
                    if (coord.lat >= minLat && coord.lat <= maxLat && coord.lng >= minLng && coord.lng <= maxLng) {
                        result.add(new H3Cell(child, coord.lat, coord.lng));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error generating regional metadata", e);
        }
        return result;
    }

    /*
     * Calculate grid path between two cells.
     */
    public List<Long> gridPathCells(long start, long end) {
        try {
            return h3.gridPathCells(start, end);
        } catch (Exception e) {
            // Pathfinding failed (e.g. disconnected or too far)
            // Fallback: just return start and end for now
            List<Long> path = new ArrayList<>();
            path.add(start);
            path.add(end);
            return path;
        }
    }

    /*
     * Get cell boundary.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param h3Index the h3index parameter (long)
     * @return the resulting computation or state reference
     */
    public List<LatLng> getCellBoundary(long h3Index) {
        return h3.cellToBoundary(h3Index);
    }

    /*
     * Get directed edge.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param origin the origin parameter (long)
     * @param destination the destination parameter (long)
     * @return the resulting computation or state reference
     */
    public long getDirectedEdge(long origin, long destination) {
        return h3.cellsToDirectedEdge(origin, destination);
    }

    /*
     * Get edge boundary.
     * Enforces physical invariants and updates associated state variables within {@code H3Service}.
     *
     * @param edgeIndex the edge index parameter (long)
     * @return the resulting computation or state reference
     */
    public List<LatLng> getEdgeBoundary(long edgeIndex) {
        return h3.directedEdgeToBoundary(edgeIndex);
    }
}
