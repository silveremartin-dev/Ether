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
 * @version 1.0.0-beta.1-beta.1
 * @since 1.0.0
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
     * Food resource availability / caloric stockpile in Gigajoules (GJ).
     * 1 human annual metabolic requirement is ~3.362 GJ/yr (9,205 kJ/day).
     */
    @Column(nullable = false)
    private Double foodResource = 0.0;

    /**
     * Water resource availability in thousand cubic meters (10³ m³).
     */
    @Column(nullable = false)
    private Double waterResource = 0.0;

    /**
     * Wood and combustible forestry biomass stockpile in metric tonnes (t).
     */
    @Column(nullable = false)
    private Double woodResource = 0.0;

    // --- Detailed Tracking (Biomass in metric tonnes [t]) ---
    /** Human biomass in metric tonnes (t). */
    @Column(nullable = false)
    private Double biomassHuman = 0.0;
    /** Livestock biomass in metric tonnes (t). */
    @Column(nullable = false)
    private Double biomassLivestock = 0.0;
    /** Marine & freshwater aquatic biomass in metric tonnes (t). */
    @Column(nullable = false)
    private Double biomassFish = 0.0;
    /** Cultivated agricultural crop biomass in metric tonnes (t). */
    @Column(nullable = false)
    private Double biomassAgriculture = 0.0;
    /** Natural terrestrial wild flora and fauna biomass in metric tonnes (t). */
    @Column(nullable = false)
    private Double biomassNatural = 0.0;

    // --- Detailed Tracking (Energy in Gigajoules [GJ]) ---
    /** Harvested wind energy flow in Gigajoules (GJ). */
    @Column(nullable = false)
    private Double energyWind = 0.0;
    /** Harvested direct solar energy flow in Gigajoules (GJ). */
    @Column(nullable = false)
    private Double energySolar = 0.0;
    /** Fire / thermal combustion energy in Gigajoules (GJ). */
    @Column(nullable = false)
    private Double energyFire = 0.0;
    /** Human physical labor treated as energetic work in Gigajoules (GJ). */
    @Column(nullable = false)
    private Double energySlaves = 0.0;
    /** Total food consumed for metabolism in Gigajoules (GJ). */
    @Column(nullable = false)
    private Double energyFoodConsumed = 0.0;

    // --- Detailed Tracking (Resources) ---
    /** Extractable base metal resources in metric tonnes (t). */
    @Column(nullable = false)
    private Double resourceMetal = 0.0;
    /** Precious ore deposits in kilograms per square kilometer (kg/km²). */
    @Column(nullable = false)
    private Double resourcePreciousMetal = 0.0;
    /** Clay and construction earthen material in metric tonnes (t). */
    @Column(nullable = false)
    private Double resourceClay = 0.0;
    /** Available labor force (person-years equivalent). */
    @Column(nullable = false)
    private Double resourceWork = 0.0;
    /** Accumulated infrastructure and productive capital stock. */
    @Column(nullable = false)
    private Double resourceCapital = 0.0;
    /** Soil organic carbon fertility in metric tonnes of Carbon per square kilometer (tC/km²). */
    @Column(nullable = false)
    private Double soilOrganicCarbon = 0.0;
    /** Geothermal heat flow & tectonic baseline in milliwatts per square meter (mW/m²). */
    @Column(nullable = false)
    private Double mantleHeatFlow = 87.0;
    /** Total groundwater table volume in cubic meters per square kilometer (m³/km²). */
    @Column(nullable = false)
    private Double freshwaterAquifer = 0.0;
    /** Accessible shallow aquifer / springs volume in cubic meters per square kilometer (m³/km²). */
    @Column(nullable = false)
    private Double accessibleAquifer = 0.0;
    /** Environmental pollution index (0.0 clean to 1000.0 toxic contamination). */
    @Column(nullable = false)
    private Double pollutionLevel = 0.0;

    // --- Socio-Economic Indices ---
    @Column(nullable = false)
    private Double lifespan = 40.0;
    @Column(nullable = false)
    private Double fertility = 6.0;
    @Column(nullable = false)
    private Double giniIndex = 0.0;
    @Column(nullable = false)
    private Double technologyLevel = 0.0;

    // --- Movement & Terrain Friction Matrix ---
    @Column(nullable = false)
    private Double movementFriction = 1.0; // 1.0 = ideal flat plain, 10.0+ = high resistance mountain/swamp/desert

    // --- Dynamic Climate & Surface Physics ---
    @Column(nullable = false)
    private Double dynamicAlbedo = 0.30; // Dynamic albedo (0.10 dark forest to 0.85 fresh snow/ice)
    @Column(nullable = false)
    private Double iceSheetThicknessMeters = 0.0; // Ice sheet thickness in meters (up to 2000.0 m during LGM)
    @Column(nullable = false)
    private Double seaLevelOffsetMeters = 0.0; // Global sea level offset in meters (-120.0 m during LGM)
    @Column(nullable = false)
    private Boolean isCoastal = false; // True if cell borders ocean/sea coast
    @Column(nullable = false)
    private Double coastalMarineResource = 0.0; // Marine/coastal fish & shellfish biomass (boosts carrying capacity)
    @Column(nullable = false)
    private Boolean isPolder = false; // True if cell is reclaimed land from ocean/sea
    @Column(nullable = false)
    private Boolean hasFloatingInfrastructure = false; // True if cell hosts seasteading / floating habitats

    // --- Detailed Demographic Age Pyramid (7 Fine-Grained Cohorts) ---
    @Column(nullable = false)
    private Integer pop0to4 = 0;   // 0-4 years (Infant cohort)
    @Column(nullable = false)
    private Integer pop5to14 = 0;  // 5-14 years (Child cohort)
    @Column(nullable = false)
    private Integer pop15to24 = 0; // 15-24 years (Youth adult / military cohort)
    @Column(nullable = false)
    private Integer pop25to49 = 0; // 25-49 years (Prime labor & fertility cohort)
    @Column(nullable = false)
    private Integer pop50to64 = 0; // 50-64 years (Mature adult / leadership cohort)
    @Column(nullable = false)
    private Integer pop65to79 = 0; // 65-79 years (Senior cohort)
    @Column(nullable = false)
    private Integer pop80Plus = 0; // 80+ years (Vulnerable elderly cohort)

    // Legacy cohort aggregations
    @Column(nullable = false)
    private Integer popYouth = 0;   // 0-14 aggregate
    @Column(nullable = false)
    private Integer popAdult = 0;   // 15-64 aggregate
    @Column(nullable = false)
    private Integer popElderly = 0; // 65+ aggregate

    // --- Epidemiological SEIR Model State ---
    @Column(nullable = false)
    private Integer epidemicInfected = 0;
    @Column(nullable = false)
    private Integer epidemicRecovered = 0;
    @Column(length = 50)
    private String activePathogenName = null;

    // --- Linguistic & Cultural Diffusion ---
    @Column(length = 50)
    private String languageGroup = "Proto-Human";
    @Column(nullable = false)
    private Double linguisticDrift = 0.0;

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
        this.population = population != null ? population : 0;
        this.biomassHuman = this.population.doubleValue();
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
        return biomassHuman != null ? biomassHuman : (population != null ? population.doubleValue() : 0.0);
    }

    public void setBiomassHuman(Double biomassHuman) {
        this.biomassHuman = biomassHuman != null ? biomassHuman : 0.0;
        this.population = (int) Math.round(this.biomassHuman);
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

    public Double getPollutionLevel() {
        return pollutionLevel;
    }

    public void setPollutionLevel(Double pollutionLevel) {
        this.pollutionLevel = pollutionLevel;
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
                case GLACIER -> baseFriction += 8.0;
                case FOREST -> baseFriction += 1.5;
                case SAVANNAH -> baseFriction += 0.8;
                case JUNGLE -> baseFriction += 4.5;
                case DESERT -> baseFriction += 4.0;
                case MOUNTAINS -> baseFriction += 6.0;
                case HILLS -> baseFriction += 2.0;
                case BEACH -> baseFriction += 1.2;
                default -> {}
            }
        }

        // Technology infrastructure mitigation (roads, vehicles, navigation)
        double techMitigation = Math.max(0.2, 1.0 - (techLevel * 0.08));
        this.movementFriction = Math.max(0.3, baseFriction * techMitigation);
        return this.movementFriction;
    }

    public Double getDynamicAlbedo() {
        return dynamicAlbedo;
    }

    public void setDynamicAlbedo(Double dynamicAlbedo) {
        this.dynamicAlbedo = dynamicAlbedo;
    }

    public Integer getPop0to4() { return pop0to4; }
    public void setPop0to4(Integer val) { this.pop0to4 = val; }

    public Integer getPop5to14() { return pop5to14; }
    public void setPop5to14(Integer val) { this.pop5to14 = val; }

    public Integer getPop15to24() { return pop15to24; }
    public void setPop15to24(Integer val) { this.pop15to24 = val; }

    public Integer getPop25to49() { return pop25to49; }
    public void setPop25to49(Integer val) { this.pop25to49 = val; }

    public Integer getPop50to64() { return pop50to64; }
    public void setPop50to64(Integer val) { this.pop50to64 = val; }

    public Integer getPop65to79() { return pop65to79; }
    public void setPop65to79(Integer val) { this.pop65to79 = val; }

    public Integer getPop80Plus() { return pop80Plus; }
    public void setPop80Plus(Integer val) { this.pop80Plus = val; }

    public Integer getEpidemicInfected() { return epidemicInfected; }
    public void setEpidemicInfected(Integer val) { this.epidemicInfected = val; }

    public Integer getEpidemicRecovered() { return epidemicRecovered; }
    public void setEpidemicRecovered(Integer val) { this.epidemicRecovered = val; }

    public String getActivePathogenName() { return activePathogenName; }
    public void setActivePathogenName(String name) { this.activePathogenName = name; }

    public String getLanguageGroup() { return languageGroup; }
    public void setLanguageGroup(String lang) { this.languageGroup = lang; }

    public Double getLinguisticDrift() { return linguisticDrift; }
    public void setLinguisticDrift(Double drift) { this.linguisticDrift = drift; }

    /**
     * Calculates dynamic surface albedo based on snow cover, biome, and natural vegetation density.
     */
    public double calculateDynamicAlbedo() {
        if (elevation != null && elevation < 0) {
            this.dynamicAlbedo = 0.06; // Water body baseline
            return this.dynamicAlbedo;
        }

        double baseAlbedo = 0.22; // Default grassland/plains
        if (biome != null) {
            switch (biome) {
                case SNOW, TUNDRA -> baseAlbedo = 0.78;
                case GLACIER -> baseAlbedo = 0.85;
                case SAVANNAH -> baseAlbedo = 0.25;
                case DESERT -> baseAlbedo = 0.40;
                case FOREST, JUNGLE -> baseAlbedo = 0.12;
                case MOUNTAINS -> baseAlbedo = 0.35;
                default -> baseAlbedo = 0.22;
            }
        }

        // Deforestation / Soil exposure effect: Depleting biomassNatural increases bare soil exposure (~0.30)
        double vegDensity = Math.clamp(biomassNatural / 1000.0, 0.0, 1.0);
        this.dynamicAlbedo = (baseAlbedo * vegDensity) + (0.30 * (1.0 - vegDensity));
        return this.dynamicAlbedo;
    }

    /**
     * Updates the detailed 7-segment age pyramid based on total population
     * and technological demographic transition stage.
     */
    public void updateAgePyramidFromTotal(double techLevel) {
        if (population == null || population <= 0) {
            pop0to4 = pop5to14 = pop15to24 = pop25to49 = pop50to64 = pop65to79 = pop80Plus = 0;
            popYouth = popAdult = popElderly = 0;
            return;
        }

        double tf = Math.clamp((techLevel - 1.0) / 7.0, 0.0, 1.0);

        // Interpolate 7 cohort shares (Pre-industrial -> Modern)
        double share0to4   = 0.15 - (tf * 0.10); // 15% -> 5%
        double share5to14  = 0.27 - (tf * 0.14); // 27% -> 13%
        double share15to24 = 0.18 - (tf * 0.04); // 18% -> 14%
        double share25to49 = 0.25 + (tf * 0.09); // 25% -> 34%
        double share50to64 = 0.09 + (tf * 0.07); // 9%  -> 16%
        double share65to79 = 0.05 + (tf * 0.09); // 5%  -> 14%
        double share80Plus = 0.01 + (tf * 0.03); // 1%  -> 4%

        this.pop0to4   = (int) Math.round(population * share0to4);
        this.pop5to14  = (int) Math.round(population * share5to14);
        this.pop15to24 = (int) Math.round(population * share15to24);
        this.pop25to49 = (int) Math.round(population * share25to49);
        this.pop50to64 = (int) Math.round(population * share50to64);
        this.pop65to79 = (int) Math.round(population * share65to79);

        // Adjust remaining rounding difference in 80+ cohort
        int sum6 = pop0to4 + pop5to14 + pop15to24 + pop25to49 + pop50to64 + pop65to79;
        this.pop80Plus = Math.max(0, population - sum6);

        // Aggregates for backward compatibility
        this.popYouth = pop0to4 + pop5to14;
        this.popAdult = pop15to24 + pop25to49 + pop50to64;
        this.popElderly = pop65to79 + pop80Plus;
    }

    public Double getIceSheetThicknessMeters() { return iceSheetThicknessMeters != null ? iceSheetThicknessMeters : 0.0; }
    public void setIceSheetThicknessMeters(Double iceSheetThicknessMeters) { this.iceSheetThicknessMeters = iceSheetThicknessMeters; }

    public Double getSeaLevelOffsetMeters() { return seaLevelOffsetMeters != null ? seaLevelOffsetMeters : 0.0; }
    public void setSeaLevelOffsetMeters(Double seaLevelOffsetMeters) { this.seaLevelOffsetMeters = seaLevelOffsetMeters; }

    public Boolean getIsCoastal() { return isCoastal != null ? isCoastal : false; }
    public void setIsCoastal(Boolean isCoastal) { this.isCoastal = isCoastal; }

    public Double getCoastalMarineResource() { return coastalMarineResource != null ? coastalMarineResource : 0.0; }
    public void setCoastalMarineResource(Double coastalMarineResource) { this.coastalMarineResource = coastalMarineResource; }

    public Boolean getIsPolder() { return isPolder != null ? isPolder : false; }
    public void setIsPolder(Boolean isPolder) { this.isPolder = isPolder; }

    public Boolean getHasFloatingInfrastructure() { return hasFloatingInfrastructure != null ? hasFloatingInfrastructure : false; }
    public void setHasFloatingInfrastructure(Boolean hasFloatingInfrastructure) { this.hasFloatingInfrastructure = hasFloatingInfrastructure; }

    /**
     * Calculates the effective 3D real surface area of the cell in km², taking into account:
     * 1. Spherical projection latitude distortion (Equal-Area H3 base planimetric projection).
     * 2. Topographical 3D slope expansion derived from terrain ruggedness / movement friction.
     * 
     * @return Effective 3D surface area in km² (>= 0.737 km² base).
     */
    public double getEffectiveSurfaceAreaKm2() {
        double latRad = Math.toRadians(latitude != null ? latitude : 0.0);
        // Base planimetric area for H3 Level 8 (~0.737 km² near equator with spherical latitudinal cosine scaling)
        double baseArea2D = 0.7373276 * Math.max(0.20, Math.cos(latRad));
        
        // 3D Topographical slope expansion factor: slope declivity derived from movement friction
        double friction = movementFriction != null ? movementFriction : 1.0;
        double slopeFactor3D = Math.sqrt(1.0 + 0.15 * Math.pow(Math.max(0.0, friction - 1.0), 1.8));
        
        return baseArea2D * slopeFactor3D;
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
        copy.setPollutionLevel(this.pollutionLevel);
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
        copy.setIsCoastal(this.isCoastal);
        copy.setCoastalMarineResource(this.coastalMarineResource);
        copy.setIsPolder(this.isPolder);
        copy.setHasFloatingInfrastructure(this.hasFloatingInfrastructure);
        
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

