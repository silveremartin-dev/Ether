/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 * SINCE: 2.0
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

import org.ether.society.model.Biome;
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

    // --- Detailed Tracking (Biomass) ---
    @Column(nullable = false)
    private Double biomassHuman = 0.0;
    @Column(nullable = false)
    private Double biomassLivestock = 0.0;
    @Column(nullable = false)
    private Double biomassFish = 0.0;
    @Column(nullable = false)
    private Double biomassAgriculture = 0.0;
    @Column(nullable = false)
    private Double biomassNatural = 0.0;

    // --- Detailed Tracking (Energy) ---
    @Column(nullable = false)
    private Double energyWind = 0.0;
    @Column(nullable = false)
    private Double energySolar = 0.0;
    @Column(nullable = false)
    private Double energyFire = 0.0;
    @Column(nullable = false)
    private Double energySlaves = 0.0; // Human labor treated as energy source
    @Column(nullable = false)
    private Double energyFoodConsumed = 0.0;

    // --- Detailed Tracking (Resources) ---
    // Note: Food and Wood already exist above, explicitly adding others
    @Column(nullable = false)
    private Double resourceMetal = 0.0;
    @Column(nullable = false)
    private Double resourcePreciousMetal = 0.0; // Precious ores (kg/km²)
    @Column(nullable = false)
    private Double resourceClay = 0.0;
    @Column(nullable = false)
    private Double resourceWork = 0.0; // Available labor
    @Column(nullable = false)
    private Double resourceCapital = 0.0; // Infrastructure/Tools
    @Column(nullable = false)
    private Double soilOrganicCarbon = 0.0; // Soil carbon fertility (tC/km²)
    @Column(nullable = false)
    private Double mantleHeatFlow = 87.0; // Geothermal heat flow & tectonic index (mW/m²)
    @Column(nullable = false)
    private Double freshwaterAquifer = 0.0; // Total groundwater table volume (m³/km²)
    @Column(nullable = false)
    private Double accessibleAquifer = 0.0; // Accessible groundwater table volume (m³/km²) for shallow wells, springs & oasis

    // --- Socio-Economic Indices ---
    @Column(nullable = false)
    private Double lifespan = 40.0;
    @Column(nullable = false)
    private Double fertility = 6.0;
    @Column(nullable = false)
    private Double giniIndex = 0.0;
    // --- Movement & Terrain Friction Matrix ---
    @Column(nullable = false)
    private Double movementFriction = 1.0; // 1.0 = ideal flat plain, 10.0+ = high resistance mountain/swamp/desert

    // --- Demographics: Age Pyramid / Cohort-Component Model ---
    @Column(nullable = false)
    private Integer popYouth = 0;   // 0-14 years old (Youth dependency cohort)
    @Column(nullable = false)
    private Integer popAdult = 0;   // 15-64 years old (Active labor & reproductive cohort)
    @Column(nullable = false)
    private Integer popElderly = 0; // 65+ years old (Elderly dependency cohort)

    // Analytics / Simulation State
    @Transient
    private double fluxPressure = 0.0;

    @Transient // Not persisting political ownership yet
    private org.ether.society.model.Nation owner;

    @Transient
    private boolean boundaryCell = false;

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

    public double getFluxPressure() {
        return fluxPressure;
    }

    public void setFluxPressure(double fluxPressure) {
        this.fluxPressure = fluxPressure;
    }

    public org.ether.society.model.Nation getOwner() {
        return owner;
    }

    public void setOwner(org.ether.society.model.Nation owner) {
        this.owner = owner;
    }

    public Double getWoodResource() {
        return woodResource;
    }

    public void setWoodResource(Double woodResource) {
        this.woodResource = woodResource;
    }

    // New Fields Getters/Setters

    public Double getBiomassHuman() {
        return biomassHuman;
    }

    public void setBiomassHuman(Double biomassHuman) {
        this.biomassHuman = biomassHuman;
    }

    public Double getBiomassLivestock() {
        return biomassLivestock;
    }

    public void setBiomassLivestock(Double val) {
        this.biomassLivestock = val;
    }

    public Double getBiomassFish() {
        return biomassFish;
    }

    public void setBiomassFish(Double val) {
        this.biomassFish = val;
    }

    public Double getBiomassAgriculture() {
        return biomassAgriculture;
    }

    public void setBiomassAgriculture(Double val) {
        this.biomassAgriculture = val;
    }

    public Double getBiomassNatural() {
        return biomassNatural;
    }

    public void setBiomassNatural(Double val) {
        this.biomassNatural = val;
    }

    public Double getEnergyWind() {
        return energyWind;
    }

    public void setEnergyWind(Double val) {
        this.energyWind = val;
    }

    public Double getEnergySolar() {
        return energySolar;
    }

    public void setEnergySolar(Double val) {
        this.energySolar = val;
    }

    public Double getEnergyFire() {
        return energyFire;
    }

    public void setEnergyFire(Double val) {
        this.energyFire = val;
    }

    public Double getEnergySlaves() {
        return energySlaves;
    }

    public void setEnergySlaves(Double val) {
        this.energySlaves = val;
    }

    public Double getEnergyFoodConsumed() {
        return energyFoodConsumed;
    }

    public void setEnergyFoodConsumed(Double val) {
        this.energyFoodConsumed = val;
    }

    public Double getResourceMetal() {
        return resourceMetal;
    }

    public void setResourceMetal(Double val) {
        this.resourceMetal = val;
    }

    public Double getResourcePreciousMetal() {
        return resourcePreciousMetal;
    }

    public void setResourcePreciousMetal(Double val) {
        this.resourcePreciousMetal = val;
    }

    public Double getSoilOrganicCarbon() {
        return soilOrganicCarbon;
    }

    public void setSoilOrganicCarbon(Double val) {
        this.soilOrganicCarbon = val;
    }

    public Double getMantleHeatFlow() {
        return mantleHeatFlow;
    }

    public void setMantleHeatFlow(Double val) {
        this.mantleHeatFlow = val;
    }

    public Double getFreshwaterAquifer() {
        return freshwaterAquifer;
    }

    public void setFreshwaterAquifer(Double val) {
        this.freshwaterAquifer = val;
    }

    public Double getResourceClay() {
        return resourceClay;
    }

    public void setResourceClay(Double val) {
        this.resourceClay = val;
    }

    public Double getResourceWork() {
        return resourceWork;
    }

    public void setResourceWork(Double val) {
        this.resourceWork = val;
    }

    public Double getResourceCapital() {
        return resourceCapital;
    }

    public void setResourceCapital(Double val) {
        this.resourceCapital = val;
    }

    public Double getLifespan() {
        return lifespan;
    }

    public void setLifespan(Double val) {
        this.lifespan = val;
    }

    public Double getFertility() {
        return fertility;
    }

    public void setFertility(Double val) {
        this.fertility = val;
    }

    public Double getGiniIndex() {
        return giniIndex;
    }

    public void setGiniIndex(Double val) {
        this.giniIndex = val;
    }

    public Double getTechnologyLevel() {
        return technologyLevel;
    }

    public void setTechnologyLevel(Double val) {
        this.technologyLevel = val;
    }
    public Double getAccessibleAquifer() {
        return accessibleAquifer;
    }

    public void setAccessibleAquifer(Double accessibleAquifer) {
        this.accessibleAquifer = accessibleAquifer;
    }

    public boolean isBoundaryCell() {
        return boundaryCell;
    }

    public void setBoundaryCell(boolean boundaryCell) {
        this.boundaryCell = boundaryCell;
    }

    public Double getMovementFriction() {
        return movementFriction;
    }

    public void setMovementFriction(Double movementFriction) {
        this.movementFriction = movementFriction;
    }

    public Integer getPopYouth() {
        return popYouth;
    }

    public void setPopYouth(Integer popYouth) {
        this.popYouth = popYouth;
    }

    public Integer getPopAdult() {
        return popAdult;
    }

    public void setPopAdult(Integer popAdult) {
        this.popAdult = popAdult;
    }

    public Integer getPopElderly() {
        return popElderly;
    }

    public void setPopElderly(Integer popElderly) {
        this.popElderly = popElderly;
    }

    /**
     * Calculates terrain movement friction (cost-distance factor) based on elevation,
     * biome, water resources and current technology level.
     * @param techLevel technology level (1.0 = ancient, 10.0 = modern)
     * @return movement friction multiplier (1.0 = baseline flat plain, >10.0 = extreme mountain/desert/swamp)
     */
    public double calculateMovementFriction(double techLevel) {
        double baseFriction = 1.0;

        // Elevation / Ruggedness penalty
        if (elevation > 3000) baseFriction += 8.0;
        else if (elevation > 1500) baseFriction += 4.0;
        else if (elevation > 500) baseFriction += 1.5;
        else if (elevation < 0) {
            // Marine navigation: requires maritime technology
            return techLevel >= 3.0 ? Math.max(0.5, 3.0 - (techLevel * 0.25)) : 25.0; // High friction for ancient land dwellers
        }

        // Biome friction
        if (biome != null) {
            switch (biome) {
                case TUNDRA, SNOW -> baseFriction += 3.5;
                case TAIGA -> baseFriction += 2.0;
                case DESERT -> baseFriction += 4.5;
                case TROPICAL_RAINFOREST -> baseFriction += 5.0;
                case SWAMP -> baseFriction += 4.0;
                case MOUNTAIN -> baseFriction += 6.0;
                case HILLS -> baseFriction += 2.0;
                case RIVERS -> baseFriction = 0.6; // River highways reduce movement friction
                default -> {}
            }
        }

        // Technology infrastructure mitigation (roads, vehicles, navigation)
        double techMitigation = Math.max(0.2, 1.0 - (techLevel * 0.08));
        this.movementFriction = Math.max(0.3, baseFriction * techMitigation);
        return this.movementFriction;
    }

    /**
     * Updates the age pyramid (Youth 0-14, Adult 15-64, Elderly 65+) based on total population
     * and technological demographic transition stage.
     */
    public void updateAgePyramidFromTotal(double techLevel) {
        if (population == null || population <= 0) {
            popYouth = 0;
            popAdult = 0;
            popElderly = 0;
            return;
        }

        // Demographic transition model:
        // Ancient/Pre-Industrial (Tech <= 3.0): High birth rate, high infant mortality -> 42% youth, 52% adult, 6% elderly
        // Modern (Tech >= 8.0): Low birth rate, high lifespan -> 18% youth, 64% adult, 18% elderly
        double techFactor = Math.clamp((techLevel - 1.0) / 7.0, 0.0, 1.0);

        double youthShare = 0.42 - (techFactor * 0.24);  // 42% -> 18%
        double elderlyShare = 0.06 + (techFactor * 0.12); // 6% -> 18%

        this.popYouth = (int) Math.round(population * youthShare);
        this.popElderly = (int) Math.round(population * elderlyShare);
        this.popAdult = population - (popYouth + popElderly);
    }

    /**
     * Create a snapshot copy of this cell.
     */
    public H3Cell snapshot() {
        H3Cell copy = new H3Cell(this.h3Index, this.latitude, this.longitude);
        copy.setId(this.id);
        copy.setElevation(this.elevation);
        copy.setTemperature(this.temperature);
        copy.setRainfall(this.rainfall);
        copy.setBiome(this.biome);
        copy.setPopulation(this.population);
        copy.setFoodResource(this.foodResource);
        copy.setWaterResource(this.waterResource);
        copy.setWoodResource(this.woodResource);
        copy.setResourceMetal(this.resourceMetal);
        copy.setResourcePreciousMetal(this.resourcePreciousMetal);
        copy.setSoilOrganicCarbon(this.soilOrganicCarbon);
        copy.setMantleHeatFlow(this.mantleHeatFlow);
        copy.setFreshwaterAquifer(this.freshwaterAquifer);
        copy.setAccessibleAquifer(this.accessibleAquifer);
        copy.setResourceClay(this.resourceClay);
        copy.setResourceWork(this.resourceWork);
        copy.setResourceCapital(this.resourceCapital);
        copy.setLifespan(this.lifespan);
        copy.setFertility(this.fertility);
        copy.setGiniIndex(this.giniIndex);
        copy.setTechnologyLevel(this.technologyLevel);
        copy.setOwner(this.owner); // Shared reference for now
        copy.setFluxPressure(this.fluxPressure);
        copy.setBoundaryCell(this.boundaryCell);
        copy.setMovementFriction(this.movementFriction);
        copy.setPopYouth(this.popYouth);
        copy.setPopAdult(this.popAdult);
        copy.setPopElderly(this.popElderly);
        
        // Biomass & Energy
        copy.setBiomassHuman(this.biomassHuman);
        copy.setBiomassLivestock(this.biomassLivestock);
        copy.setBiomassFish(this.biomassFish);
        copy.setBiomassAgriculture(this.biomassAgriculture);
        copy.setBiomassNatural(this.biomassNatural);
        copy.setEnergyWind(this.energyWind);
        copy.setEnergySolar(this.energySolar);
        copy.setEnergyFire(this.energyFire);
        copy.setEnergySlaves(this.energySlaves);
        copy.setEnergyFoodConsumed(this.energyFoodConsumed);
        
        return copy;
    }
}
