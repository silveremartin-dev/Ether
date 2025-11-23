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
package com.ether.society.database;

import com.ether.society.model.Biome;
import jakarta.persistence.*;

/**
 * JPA Entity representing an H3 Level 8 hexagon cell (~1 km² area).
 * Stores terrain and climate data for a single hexagonal cell on Earth.
 *
 * <p>
 * This entity is designed for GPU-parallel processing: each cell can be
 * updated independently on the GPU using TornadoVM.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "h3_cells_l8", indexes = {
        @Index(name = "idx_h3_index", columnList = "h3_index", unique = true),
        @Index(name = "idx_lat_lng", columnList = "latitude, longitude"),
        @Index(name = "idx_biome", columnList = "biome")
})
public class H3Cell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * H3 hexagon index (Level 8, ~1 km² resolution).
     * This is the primary geospatial identifier.
     */
    @Column(name = "h3_index", nullable = false, unique = true)
    private Long h3Index;

    /**
     * Center latitude in degrees.
     */
    @Column(nullable = false)
    private Double latitude;

    /**
     * Center longitude in degrees.
     */
    @Column(nullable = false)
    private Double longitude;

    /**
     * Mean elevation in meters (from SRTM data).
     */
    @Column(nullable = false)
    private Double elevation;

    /**
     * Current temperature in Celsius.
     * Updated each simulation tick by GPU kernel.
     */
    @Column(nullable = false)
    private Double temperature;

    /**
     * Annual rainfall in millimeters.
     */
    @Column(nullable = false)
    private Double rainfall;

    /**
     * Biome classification.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Biome biome;

    /**
     * Human population density (count per hexagon).
     * Updated each simulation tick by GPU kernel.
     */
    @Column(nullable = false)
    private Integer population = 0;

    /**
     * Food resource availability (0-1000).
     */
    @Column(nullable = false)
    private Double foodResource = 0.0;

    /**
     * Water resource availability (0-1000).
     */
    @Column(nullable = false)
    private Double waterResource = 0.0;

    /**
     * Wood resource availability (0-1000).
     */
    @Column(nullable = false)
    private Double woodResource = 0.0;

    // Constructors

    public H3Cell() {
    }

    public H3Cell(Long h3Index, Double latitude, Double longitude) {
        this.h3Index = h3Index;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = 0.0;
        this.temperature = 15.0;
        this.rainfall = 500.0;
        this.biome = Biome.PLAINS;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getH3Index() {
        return h3Index;
    }

    public void setH3Index(Long h3Index) {
        this.h3Index = h3Index;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getRainfall() {
        return rainfall;
    }

    public void setRainfall(Double rainfall) {
        this.rainfall = rainfall;
    }

    public Biome getBiome() {
        return biome;
    }

    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    public Integer getPopulation() {
        return population;
    }

    public void setPopulation(Integer population) {
        this.population = population;
    }

    public Double getFoodResource() {
        return foodResource;
    }

    public void setFoodResource(Double foodResource) {
        this.foodResource = foodResource;
    }

    public Double getWaterResource() {
        return waterResource;
    }

    public void setWaterResource(Double waterResource) {
        this.waterResource = waterResource;
    }

    public Double getWoodResource() {
        return woodResource;
    }

    public void setWoodResource(Double woodResource) {
        this.woodResource = woodResource;
    }
}
