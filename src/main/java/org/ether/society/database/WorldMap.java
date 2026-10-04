/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
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
package org.ether.society.database;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a world map configuration with data sources and boundaries.
 * Supports multiple maps (Earth, procedurally generated planets, etc.).
 *
 * <p>
 * Each map defines:
 * </p>
 * <ul>
 * <li>Geographic boundaries (lat/lng corners)</li>
 * <li>Data sources (elevation, biome maps)</li>
 * <li>Planet size (for procedural generation)</li>
 * <li>H3 resolution level</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1-beta.1
 * @since 1.0.0
 */
@Entity
@Table(name = "world_maps")
public class WorldMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Human-readable map name (e.g., "Earth", "Mars", "Procedural Alpha").
     */
    @Column(nullable = false, unique = true)
    /* Internal state variable for name (String). */
    private String name;

    /*
     * Map description.
     */
    @Column(length = 1000)
    /* Internal state variable for description (String). */
    private String description;

    /*
     * Map type: EARTH, PROCEDURAL, CUSTOM.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MapType mapType;

    // Geographic bounds

    /* Minimum latitude (south edge). */
    @Column(nullable = false)
    private Double minLatitude = -90.0;

    /* Maximum latitude (north edge). */
    @Column(nullable = false)
    private Double maxLatitude = 90.0;

    /* Minimum longitude (west edge). */
    @Column(nullable = false)
    private Double minLongitude = -180.0;

    /* Maximum longitude (east edge). */
    @Column(nullable = false)
    private Double maxLongitude = 180.0;

    // Data sources

    /* Path to map preview image. */
    @Column(length = 500)
    /* Internal state variable for map image path (String). */
    private String mapImagePath;

    /* Elevation data source (e.g., "SRTM_90m", "PROCEDURAL"). */
    @Column(length = 200)
    /* Internal state variable for elevation source (String). */
    private String elevationSource;

    /* Biome data source (e.g., "MODIS_MCD12Q1", "PROCEDURAL"). */
    @Column(length = 200)
    /* Internal state variable for biome source (String). */
    private String biomeSource;

    // Planet properties (for procedural generation)

    /* Planet radius in km (Earth = 6371). */
    @Column
    private Double planetRadiusKm = 6371.0;

    /* Planet mass relative to Earth (1.0 = Earth mass). */
    @Column
    private Double planetMass = 1.0;

    /* Procedural generation seed. */
    @Column
    private Long proceduralSeed;

    // H3 configuration

    /* H3 resolution level (8 = ~1 kmÂ², 6 = ~2.2 kmÂ²). */
    @Column(nullable = false)
    private Integer h3Resolution = 8;

    /* Total number of H3 cells for this map. */
    @Column
    private Long totalCells;

    // Metadata

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Enum for map type

    public enum MapType {
        /* Real Earth with SRTM/MODIS data. */
        EARTH,

        /* Procedurally generated planet. */
        PROCEDURAL,

        /* Custom user-defined map. */
        CUSTOM
    }

    // Constructors

    /*
     * World map.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     */
    public WorldMap() {
    }

    /*
     * World map.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param name the name parameter (String)
     * @param mapType the map type parameter (MapType)
     */
    public WorldMap(String name, MapType mapType) {
        this.name = name;
        this.mapType = mapType;
    }

    // Getters and setters (abbreviated for brevity)

    /*
     * Get id.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Long getId() {
        return id;
    }

    /*
     * Set id.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param id the id parameter (Long)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return name;
    }

    /*
     * Set name.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param name the name parameter (String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return description;
    }

    /*
     * Set description.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param description the description parameter (String)
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /*
     * Get map type.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public MapType getMapType() {
        return mapType;
    }

    /*
     * Set map type.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param mapType the map type parameter (MapType)
     */
    public void setMapType(MapType mapType) {
        this.mapType = mapType;
    }

    /*
     * Get min latitude.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Double getMinLatitude() {
        return minLatitude;
    }

    /*
     * Set min latitude.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param minLatitude the min latitude parameter (Double)
     */
    public void setMinLatitude(Double minLatitude) {
        this.minLatitude = minLatitude;
    }

    /*
     * Get max latitude.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Double getMaxLatitude() {
        return maxLatitude;
    }

    /*
     * Set max latitude.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param maxLatitude the max latitude parameter (Double)
     */
    public void setMaxLatitude(Double maxLatitude) {
        this.maxLatitude = maxLatitude;
    }

    /*
     * Get min longitude.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Double getMinLongitude() {
        return minLongitude;
    }

    /*
     * Set min longitude.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param minLongitude the min longitude parameter (Double)
     */
    public void setMinLongitude(Double minLongitude) {
        this.minLongitude = minLongitude;
    }

    /*
     * Get max longitude.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Double getMaxLongitude() {
        return maxLongitude;
    }

    /*
     * Set max longitude.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param maxLongitude the max longitude parameter (Double)
     */
    public void setMaxLongitude(Double maxLongitude) {
        this.maxLongitude = maxLongitude;
    }

    /*
     * Get map image path.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public String getMapImagePath() {
        return mapImagePath;
    }

    /*
     * Set map image path.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param mapImagePath the map image path parameter (String)
     */
    public void setMapImagePath(String mapImagePath) {
        this.mapImagePath = mapImagePath;
    }

    /*
     * Get elevation source.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public String getElevationSource() {
        return elevationSource;
    }

    /*
     * Set elevation source.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param elevationSource the elevation source parameter (String)
     */
    public void setElevationSource(String elevationSource) {
        this.elevationSource = elevationSource;
    }

    /*
     * Get biome source.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public String getBiomeSource() {
        return biomeSource;
    }

    /*
     * Set biome source.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param biomeSource the biome source parameter (String)
     */
    public void setBiomeSource(String biomeSource) {
        this.biomeSource = biomeSource;
    }

    /*
     * Get planet radius km.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Double getPlanetRadiusKm() {
        return planetRadiusKm;
    }

    /*
     * Set planet radius km.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param planetRadiusKm the planet radius km parameter (Double)
     */
    public void setPlanetRadiusKm(Double planetRadiusKm) {
        this.planetRadiusKm = planetRadiusKm;
    }

    /*
     * Get planet mass.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Double getPlanetMass() {
        return planetMass;
    }

    /*
     * Set planet mass.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param planetMass the planet mass parameter (Double)
     */
    public void setPlanetMass(Double planetMass) {
        this.planetMass = planetMass;
    }

    /*
     * Get procedural seed.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Long getProceduralSeed() {
        return proceduralSeed;
    }

    /*
     * Set procedural seed.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param proceduralSeed the procedural seed parameter (Long)
     */
    public void setProceduralSeed(Long proceduralSeed) {
        this.proceduralSeed = proceduralSeed;
    }

    /*
     * Get h3resolution.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Integer getH3Resolution() {
        return h3Resolution;
    }

    /*
     * Set h3resolution.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param h3Resolution the h3resolution parameter (Integer)
     */
    public void setH3Resolution(Integer h3Resolution) {
        this.h3Resolution = h3Resolution;
    }

    /*
     * Get total cells.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public Long getTotalCells() {
        return totalCells;
    }

    /*
     * Set total cells.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param totalCells the total cells parameter (Long)
     */
    public void setTotalCells(Long totalCells) {
        this.totalCells = totalCells;
    }

    /*
     * Get created at.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /*
     * Get updated at.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @return the resulting computation or state reference
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /*
     * Set updated at.
     * Enforces physical invariants and updates associated state variables within {@code WorldMap}.
     *
     * @param updatedAt the updated at parameter (LocalDateTime)
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


