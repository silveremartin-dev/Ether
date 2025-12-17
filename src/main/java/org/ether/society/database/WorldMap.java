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
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "world_maps")
public class WorldMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable map name (e.g., "Earth", "Mars", "Procedural Alpha").
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Map description.
     */
    @Column(length = 1000)
    private String description;

    /**
     * Map type: EARTH, PROCEDURAL, CUSTOM.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MapType mapType;

    // Geographic bounds

    /** Minimum latitude (south edge). */
    @Column(nullable = false)
    private Double minLatitude = -90.0;

    /** Maximum latitude (north edge). */
    @Column(nullable = false)
    private Double maxLatitude = 90.0;

    /** Minimum longitude (west edge). */
    @Column(nullable = false)
    private Double minLongitude = -180.0;

    /** Maximum longitude (east edge). */
    @Column(nullable = false)
    private Double maxLongitude = 180.0;

    // Data sources

    /** Path to map preview image. */
    @Column(length = 500)
    private String mapImagePath;

    /** Elevation data source (e.g., "SRTM_90m", "PROCEDURAL"). */
    @Column(length = 200)
    private String elevationSource;

    /** Biome data source (e.g., "MODIS_MCD12Q1", "PROCEDURAL"). */
    @Column(length = 200)
    private String biomeSource;

    // Planet properties (for procedural generation)

    /** Planet radius in km (Earth = 6371). */
    @Column
    private Double planetRadiusKm = 6371.0;

    /** Planet mass relative to Earth (1.0 = Earth mass). */
    @Column
    private Double planetMass = 1.0;

    /** Procedural generation seed. */
    @Column
    private Long proceduralSeed;

    // H3 configuration

    /** H3 resolution level (8 = ~1 kmÂ², 6 = ~2.2 kmÂ²). */
    @Column(nullable = false)
    private Integer h3Resolution = 8;

    /** Total number of H3 cells for this map. */
    @Column
    private Long totalCells;

    // Metadata

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Enum for map type

    public enum MapType {
        /** Real Earth with SRTM/MODIS data. */
        EARTH,

        /** Procedurally generated planet. */
        PROCEDURAL,

        /** Custom user-defined map. */
        CUSTOM
    }

    // Constructors

    public WorldMap() {
    }

    public WorldMap(String name, MapType mapType) {
        this.name = name;
        this.mapType = mapType;
    }

    // Getters and setters (abbreviated for brevity)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MapType getMapType() {
        return mapType;
    }

    public void setMapType(MapType mapType) {
        this.mapType = mapType;
    }

    public Double getMinLatitude() {
        return minLatitude;
    }

    public void setMinLatitude(Double minLatitude) {
        this.minLatitude = minLatitude;
    }

    public Double getMaxLatitude() {
        return maxLatitude;
    }

    public void setMaxLatitude(Double maxLatitude) {
        this.maxLatitude = maxLatitude;
    }

    public Double getMinLongitude() {
        return minLongitude;
    }

    public void setMinLongitude(Double minLongitude) {
        this.minLongitude = minLongitude;
    }

    public Double getMaxLongitude() {
        return maxLongitude;
    }

    public void setMaxLongitude(Double maxLongitude) {
        this.maxLongitude = maxLongitude;
    }

    public String getMapImagePath() {
        return mapImagePath;
    }

    public void setMapImagePath(String mapImagePath) {
        this.mapImagePath = mapImagePath;
    }

    public String getElevationSource() {
        return elevationSource;
    }

    public void setElevationSource(String elevationSource) {
        this.elevationSource = elevationSource;
    }

    public String getBiomeSource() {
        return biomeSource;
    }

    public void setBiomeSource(String biomeSource) {
        this.biomeSource = biomeSource;
    }

    public Double getPlanetRadiusKm() {
        return planetRadiusKm;
    }

    public void setPlanetRadiusKm(Double planetRadiusKm) {
        this.planetRadiusKm = planetRadiusKm;
    }

    public Double getPlanetMass() {
        return planetMass;
    }

    public void setPlanetMass(Double planetMass) {
        this.planetMass = planetMass;
    }

    public Long getProceduralSeed() {
        return proceduralSeed;
    }

    public void setProceduralSeed(Long proceduralSeed) {
        this.proceduralSeed = proceduralSeed;
    }

    public Integer getH3Resolution() {
        return h3Resolution;
    }

    public void setH3Resolution(Integer h3Resolution) {
        this.h3Resolution = h3Resolution;
    }

    public Long getTotalCells() {
        return totalCells;
    }

    public void setTotalCells(Long totalCells) {
        this.totalCells = totalCells;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

