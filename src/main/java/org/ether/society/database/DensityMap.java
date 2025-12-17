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

/**
 * Represents density data for a specific resource in an H3 cell.
 * Stores quantities in gigajoules per kmÂ² for energy-based resources.
 *
 * <p>
 * Resource categories:
 * </p>
 * <ul>
 * <li><strong>Food</strong>: Fish, animals, cultivated/wild vegetables</li>
 * <li><strong>Minerals</strong>: Copper, iron, gold, rare earths</li>
 * <li><strong>Biomass</strong>: Wood, organic matter</li>
 * </ul>
 *
 * <p>
 * Fractal distribution support for realistic clustering (e.g., cities, ore
 * deposits).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 2.0.0
 */
@Entity
@Table(name = "density_maps", indexes = {
        @Index(name = "idx_density_simulation", columnList = "simulation_id"),
        @Index(name = "idx_density_h3", columnList = "h3_index"),
        @Index(name = "idx_density_resource", columnList = "resource_category, resource_type")
})
public class DensityMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to simulation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    /**
     * H3 cell index.
     */
    @Column(name = "h3_index", nullable = false)
    private Long h3Index;

    /**
     * Resource category: POPULATION, FOOD, MINERALS, BIOMASS.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResourceCategory resourceCategory;

    /**
     * Specific resource type (e.g., "FISH", "COPPER", "WHEAT").
     */
    @Column(nullable = false, length = 50)
    private String resourceType;

    /**
     * Density value in gigajoules per kmÂ² (or count/kmÂ² for population).
     */
    @Column(nullable = false)
    private Double densityValue;

    /**
     * Unit of measurement: GJ_PER_KM2, COUNT_PER_KM2, KG_PER_KM2.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DensityUnit densityUnit;

    /**
     * Distribution pattern: UNIFORM, FRACTAL, CLUSTERED, RANDOM.
     */
    @Enumerated(EnumType.STRING)
    @Column
    private DistributionPattern distributionPattern;

    /**
     * Fractal dimension (1.0-2.0) for fractal distributions.
     * Higher values = more clustered (cities, ore deposits).
     */
    @Column
    private Double fractalDimension;

    // Enums

    public enum ResourceCategory {
        /** Human population density. */
        POPULATION,

        /** Edible resources (fish, game, crops). */
        FOOD,

        /** Mineral resources (metals, ores). */
        MINERALS,

        /** Living biomass (wood, vegetation). */
        BIOMASS,

        /** Water resources. */
        WATER
    }

    public enum DensityUnit {
        /** Gigajoules per square kilometer. */
        GJ_PER_KM2,

        /** Count per square kilometer (population). */
        COUNT_PER_KM2,

        /** Kilograms per square kilometer. */
        KG_PER_KM2,

        /** Cubic meters per square kilometer (water). */
        M3_PER_KM2
    }

    public enum DistributionPattern {
        /** Evenly distributed. */
        UNIFORM,

        /** Fractal/self-similar (realistic for cities, resources). */
        FRACTAL,

        /** Strongly clustered (ore veins, oases). */
        CLUSTERED,

        /** Random distribution. */
        RANDOM
    }

    // Constructors

    public DensityMap() {
    }

    public DensityMap(Simulation simulation, Long h3Index,
            ResourceCategory category, String type, Double value) {
        this.simulation = simulation;
        this.h3Index = h3Index;
        this.resourceCategory = category;
        this.resourceType = type;
        this.densityValue = value;
        this.densityUnit = DensityUnit.GJ_PER_KM2;
    }

    // Getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public Long getH3Index() {
        return h3Index;
    }

    public void setH3Index(Long h3Index) {
        this.h3Index = h3Index;
    }

    public ResourceCategory getResourceCategory() {
        return resourceCategory;
    }

    public void setResourceCategory(ResourceCategory category) {
        this.resourceCategory = category;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Double getDensityValue() {
        return densityValue;
    }

    public void setDensityValue(Double densityValue) {
        this.densityValue = densityValue;
    }

    public DensityUnit getDensityUnit() {
        return densityUnit;
    }

    public void setDensityUnit(DensityUnit densityUnit) {
        this.densityUnit = densityUnit;
    }

    public DistributionPattern getDistributionPattern() {
        return distributionPattern;
    }

    public void setDistributionPattern(DistributionPattern pattern) {
        this.distributionPattern = pattern;
    }

    public Double getFractalDimension() {
        return fractalDimension;
    }

    public void setFractalDimension(Double fractalDimension) {
        this.fractalDimension = fractalDimension;
    }
}

