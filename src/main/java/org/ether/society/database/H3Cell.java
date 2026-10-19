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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ether.society.model.Biome;
import jakarta.persistence.*;

/**
 * <h1>H3Cell</h1>
 * <p>
 * JPA Entity representing an H3 hexagonal cell.
 * Stores terrain, climate, ecological, demographic, and socio-economic tensor data for a single hexagonal cell.
 * Ingests global planetary datasets (NOAA ETOPO, WorldClim, UNESCO WHYMAP, Natural Earth, Seshat, D-PLACE) into standardized H3 hexagonal rasters.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @since 1.0.0
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
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
    /** Soil salinity / electrical conductivity EC_e in deciSiemens per meter (dS/m). */
    @Column(nullable = false)
    private Double soilSalinity = 0.0;
    /** Subsoil extractable coal reserves in Gigatonnes or metric tonnes (t). */
    @Column(nullable = false)
    private Double resourceCoal = 0.0;
    /** Subsoil extractable crude oil reserves in barrels-equivalent or metric tonnes (t). */
    @Column(nullable = false)
    private Double resourceOil = 0.0;
    /** Subsoil extractable natural gas reserves in cubic meters or energy equivalent (t). */
    @Column(nullable = false)
    private Double resourceGas = 0.0;
    /** Subsoil fissile uranium / thorium ore deposits in metric tonnes (t). */
    @Column(nullable = false)
    private Double resourceUranium = 0.0;
    /** Subsoil rare earth elements (REE), lithium, and critical mineral deposits in metric tonnes (t). */
    @Column(nullable = false)
    private Double resourceRareEarths = 0.0;
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
    /** Hominin species clade: 1 = Homo Sapiens (default), 2 = Homo Neanderthalensis, 3 = Denisovan / Archaic. */
    @Column(nullable = false)
    private Integer homininSpecies = 1;

    // --- Movement & Terrain Friction Matrix ---
    @Column(nullable = false)
    private Double movementFriction = 1.0; // 1.0 = ideal flat plain, 10.0+ = high resistance mountain/swamp/desert
    /** Topological trade corridor centrality and bottleneck funneling score (0.0 to 1.0). */
    @Column(nullable = false)
    private Double chokePointScore = 0.0;
    /** Cumulative economic transit toll and tariff revenue collected in capital units. */
    @Column(nullable = false)
    private Double transitTollRevenue = 0.0;

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

    // --- Extraterrestrial Pressurized Habitats & Infrastructure ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private org.ether.society.model.HabitatType habitatType = org.ether.society.model.HabitatType.NONE;
    @Column(nullable = false)
    private Integer habitatTier = 0; // Tier 0: None, Tier 1: Outpost (50-500), Tier 2: Habitat (5k-50k), Tier 3: Megastructure (500k-5M)
    @Column(nullable = false)
    private Double habitatCapacity = 0.0; // Max pressurized human capacity
    @Column(nullable = false)
    private Double habitatIntegrity = 1.0; // Structural integrity (1.0 = brand new, 0.0 = total breach)
    @Column(nullable = false)
    private Double habitatEnergyKw = 0.0; // Total ECLSS power consumption in kW
    @Column(nullable = false)
    private Double habitatAgeYears = 0.0; // Accumulated age in Earth years

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

    // Demographic cohort aggregations
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
    @JsonIgnore
    private Polity owner;

    @Transient
    private boolean boundaryCell = false;

    // Constructors

    /**
     * H3cell.
     *
     */
    public H3Cell() {
    }

    /**
     * H3cell.
     *
     * @param h3Index the h3index parameter (Long)
     * @param latitude the latitude parameter (Double)
     * @param longitude the longitude parameter (Double)
     */
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

    /**
     * Get id.
     *
     * @return the resulting computation or state reference
     */
    public Long getId() {
        return id;
    }

    /**
     * Set id.
     *
     * @param id the id parameter (Long)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get h3index.
     *
     * @return the resulting computation or state reference
     */
    public Long getH3Index() {
        return h3Index;
    }

    /**
     * Set h3index.
     *
     * @param h3Index the h3index parameter (Long)
     */
    public void setH3Index(Long h3Index) {
        this.h3Index = h3Index;
    }

    /**
     * Get latitude.
     *
     * @return the resulting computation or state reference
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Set latitude.
     *
     * @param latitude the latitude parameter (Double)
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * Get longitude.
     *
     * @return the resulting computation or state reference
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * Set longitude.
     *
     * @param longitude the longitude parameter (Double)
     */
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * Get elevation.
     *
     * @return the resulting computation or state reference
     */
    public Double getElevation() { return elevation; }

    /**
     * Primitive-safe elevation getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if elevation is null
     * @return mean elevation in meters
     */
    public double getElevationOr(double defaultValue) { return elevation != null ? elevation : defaultValue; }

    /**
     * Set elevation.
     *
     * @param elevation the elevation parameter (Double)
     */
    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    /**
     * Get temperature.
     *
     * @return the resulting computation or state reference
     */
    public Double getTemperature() { return temperature; }

    /**
     * Primitive-safe temperature getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if temperature is null
     * @return surface temperature in Celsius
     */
    public double getTemperatureOr(double defaultValue) { return temperature != null ? temperature : defaultValue; }

    /**
     * Set temperature.
     *
     * @param temperature the temperature parameter (Double)
     */
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    /**
     * Get rainfall.
     *
     * @return the resulting computation or state reference
     */
    public Double getRainfall() { return rainfall; }

    /**
     * Primitive-safe rainfall getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if rainfall is null
     * @return annual rainfall in mm
     */
    public double getRainfallOr(double defaultValue) { return rainfall != null ? rainfall : defaultValue; }

    /**
     * Set rainfall.
     *
     * @param rainfall the rainfall parameter (Double)
     */
    public void setRainfall(Double rainfall) {
        this.rainfall = rainfall;
    }

    /**
     * Get biome.
     *
     * @return the resulting computation or state reference
     */
    public Biome getBiome() {
        return biome;
    }

    /**
     * Set biome.
     *
     * @param biome the biome parameter (Biome)
     */
    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    /**
     * Get population.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPopulation() { return population; }

    /**
     * Primitive-safe population getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if population is null
     * @return population count
     */
    public int getPopulationOr(int defaultValue) { return population != null ? population : defaultValue; }

    /**
     * Set population.
     *
     * @param population the population parameter (Integer)
     */
    public void setPopulation(Integer population) {
        int newPop = population != null ? population : 0;
        if (this.population == null || this.population != newPop) {
            this.popYouth = null;
            this.popAdult = null;
            this.popElderly = null;
            this.pop0to4 = null;
        }
        this.population = newPop;
        this.biomassHuman = (double) newPop;
    }

    /**
     * Get food resource.
     *
     * @return the resulting computation or state reference
     */
    public Double getFoodResource() { return foodResource; }

    /**
     * Primitive-safe food resource getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if foodResource is null
     * @return food resource in GJ
     */
    public double getFoodResourceOr(double defaultValue) { return foodResource != null ? foodResource : defaultValue; }

    /**
     * Set food resource.
     *
     * @param foodResource the food resource parameter (Double)
     */
    public void setFoodResource(Double foodResource) {
        this.foodResource = foodResource;
    }

    /**
     * Get water resource.
     *
     * @return the resulting computation or state reference
     */
    public Double getWaterResource() { return waterResource; }

    /**
     * Primitive-safe water resource getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if waterResource is null
     * @return water resource in 1000 m³
     */
    public double getWaterResourceOr(double defaultValue) { return waterResource != null ? waterResource : defaultValue; }

    /**
     * Set water resource.
     *
     * @param waterResource the water resource parameter (Double)
     */
    public void setWaterResource(Double waterResource) {
        this.waterResource = waterResource;
    }

    /**
     * Get flux pressure.
     *
     * @return the resulting computation or state reference
     */
    public double getFluxPressure() {
        return fluxPressure;
    }

    /**
     * Set flux pressure.
     *
     * @param fluxPressure the flux pressure parameter (double)
     */
    public void setFluxPressure(double fluxPressure) {
        this.fluxPressure = fluxPressure;
    }

    /**
     * Get owner.
     *
     * @return the resulting computation or state reference
     */
    @JsonIgnore
    public Polity getOwner() {
        return owner;
    }

    /**
     * Set owner.
     *
     * @param owner the owner parameter (Polity)
     */
    @JsonIgnore
    public void setOwner(Polity owner) {
        this.owner = owner;
    }

    /**
     * Get wood resource.
     *
     * @return the resulting computation or state reference
     */
    public Double getWoodResource() {
        return woodResource;
    }

    /**
     * Set wood resource.
     *
     * @param woodResource the wood resource parameter (Double)
     */
    public void setWoodResource(Double woodResource) {
        this.woodResource = woodResource;
    }

    // New Fields Getters/Setters

    /**
     * Get biomass human.
     *
     * @return the resulting computation or state reference
     */
    public Double getBiomassHuman() {
        return biomassHuman != null ? biomassHuman : (population != null ? population.doubleValue() : 0.0);
    }

    /**
     * Set biomass human.
     *
     * @param biomassHuman the biomass human parameter (Double)
     */
    public void setBiomassHuman(Double biomassHuman) {
        this.biomassHuman = biomassHuman != null ? biomassHuman : 0.0;
        this.population = (int) Math.round(this.biomassHuman);
    }

    /**
     * Get biomass livestock.
     *
     * @return the resulting computation or state reference
     */
    public Double getBiomassLivestock() {
        return biomassLivestock;
    }

    /**
     * Set biomass livestock.
     *
     * @param val the val parameter (Double)
     */
    public void setBiomassLivestock(Double val) {
        this.biomassLivestock = val;
    }

    /**
     * Get biomass fish.
     *
     * @return the resulting computation or state reference
     */
    public Double getBiomassFish() {
        return biomassFish;
    }

    /**
     * Set biomass fish.
     *
     * @param val the val parameter (Double)
     */
    public void setBiomassFish(Double val) {
        this.biomassFish = val;
    }

    /**
     * Get biomass agriculture.
     *
     * @return the resulting computation or state reference
     */
    public Double getBiomassAgriculture() {
        return biomassAgriculture;
    }

    /**
     * Set biomass agriculture.
     *
     * @param val the val parameter (Double)
     */
    public void setBiomassAgriculture(Double val) {
        this.biomassAgriculture = val;
    }

    /**
     * Get biomass natural.
     *
     * @return the resulting computation or state reference
     */
    public Double getBiomassNatural() {
        return biomassNatural;
    }

    /**
     * Set biomass natural.
     *
     * @param val the val parameter (Double)
     */
    public void setBiomassNatural(Double val) {
        this.biomassNatural = val;
    }

    /**
     * Get energy wind.
     *
     * @return the resulting computation or state reference
     */
    public Double getEnergyWind() {
        return energyWind;
    }

    /**
     * Set energy wind.
     *
     * @param val the val parameter (Double)
     */
    public void setEnergyWind(Double val) {
        this.energyWind = val;
    }

    /**
     * Get energy solar.
     *
     * @return the resulting computation or state reference
     */
    public Double getEnergySolar() {
        return energySolar;
    }

    /**
     * Set energy solar.
     *
     * @param val the val parameter (Double)
     */
    public void setEnergySolar(Double val) {
        this.energySolar = val;
    }

    /**
     * Get energy fire.
     *
     * @return the resulting computation or state reference
     */
    public Double getEnergyFire() {
        return energyFire;
    }

    /**
     * Set energy fire.
     *
     * @param val the val parameter (Double)
     */
    public void setEnergyFire(Double val) {
        this.energyFire = val;
    }

    /**
     * Get energy slaves.
     *
     * @return the resulting computation or state reference
     */
    public Double getEnergySlaves() {
        return energySlaves;
    }

    /**
     * Set energy slaves.
     *
     * @param val the val parameter (Double)
     */
    public void setEnergySlaves(Double val) {
        this.energySlaves = val;
    }

    /**
     * Get energy food consumed.
     *
     * @return the resulting computation or state reference
     */
    public Double getEnergyFoodConsumed() {
        return energyFoodConsumed;
    }

    /**
     * Set energy food consumed.
     *
     * @param val the val parameter (Double)
     */
    public void setEnergyFoodConsumed(Double val) {
        this.energyFoodConsumed = val;
    }

    /**
     * Get resource metal.
     *
     * @return the resulting computation or state reference
     */
    public Double getResourceMetal() {
        return resourceMetal;
    }

    /**
     * Set resource metal.
     *
     * @param val the val parameter (Double)
     */
    public void setResourceMetal(Double val) {
        this.resourceMetal = val;
    }

    /**
     * Get resource precious metal.
     *
     * @return the resulting computation or state reference
     */
    public Double getResourcePreciousMetal() {
        return resourcePreciousMetal;
    }

    /**
     * Set resource precious metal.
     *
     * @param val the val parameter (Double)
     */
    public void setResourcePreciousMetal(Double val) {
        this.resourcePreciousMetal = val;
    }

    /**
     * Get soil organic carbon.
     *
     * @return the resulting computation or state reference
     */
    public Double getSoilOrganicCarbon() {
        return soilOrganicCarbon;
    }

    /**
     * Set soil organic carbon.
     *
     * @param val the val parameter (Double)
     */
    public void setSoilOrganicCarbon(Double val) {
        this.soilOrganicCarbon = val;
    }

    /**
     * Get soil salinity.
     *
     * @return the soil salinity / electrical conductivity EC_e in dS/m
     */
    public Double getSoilSalinity() {
        return soilSalinity != null ? soilSalinity : 0.0;
    }

    /**
     * Set soil salinity.
     *
     * @param val the soil salinity in dS/m
     */
    public void setSoilSalinity(Double val) {
        this.soilSalinity = val != null ? val : 0.0;
    }

    /**
     * Get resource coal.
     *
     * @return extractable coal reserve in metric tonnes
     */
    public Double getResourceCoal() {
        return resourceCoal != null ? resourceCoal : 0.0;
    }

    /**
     * Set resource coal.
     *
     * @param val extractable coal reserve in metric tonnes
     */
    public void setResourceCoal(Double val) {
        this.resourceCoal = val != null ? Math.max(0.0, val) : 0.0;
    }

    /**
     * Get resource crude oil.
     *
     * @return extractable oil reserve in metric tonnes
     */
    public Double getResourceOil() {
        return resourceOil != null ? resourceOil : 0.0;
    }

    /**
     * Set resource crude oil.
     *
     * @param val extractable oil reserve in metric tonnes
     */
    public void setResourceOil(Double val) {
        this.resourceOil = val != null ? Math.max(0.0, val) : 0.0;
    }

    /**
     * Get resource natural gas.
     *
     * @return extractable natural gas reserve in energy equivalent tonnes
     */
    public Double getResourceGas() {
        return resourceGas != null ? resourceGas : 0.0;
    }

    /**
     * Set resource natural gas.
     *
     * @param val extractable natural gas reserve in energy equivalent tonnes
     */
    public void setResourceGas(Double val) {
        this.resourceGas = val != null ? Math.max(0.0, val) : 0.0;
    }

    /**
     * Get resource fissile uranium.
     *
     * @return extractable fissile ore reserve in metric tonnes
     */
    public Double getResourceUranium() {
        return resourceUranium != null ? resourceUranium : 0.0;
    }

    /**
     * Set resource fissile uranium.
     *
     * @param val extractable fissile ore reserve in metric tonnes
     */
    public void setResourceUranium(Double val) {
        this.resourceUranium = val != null ? Math.max(0.0, val) : 0.0;
    }

    /**
     * Get resource rare earth elements and lithium.
     *
     * @return extractable critical mineral reserve in metric tonnes
     */
    public Double getResourceRareEarths() {
        return resourceRareEarths != null ? resourceRareEarths : 0.0;
    }

    /**
     * Set resource rare earth elements and lithium.
     *
     * @param val extractable critical mineral reserve in metric tonnes
     */
    public void setResourceRareEarths(Double val) {
        this.resourceRareEarths = val != null ? Math.max(0.0, val) : 0.0;
    }

    /**
     * Get mantle heat flow.
     *
     * @return the resulting computation or state reference
     */
    public Double getMantleHeatFlow() {
        return mantleHeatFlow;
    }

    /**
     * Set mantle heat flow.
     *
     * @param val the val parameter (Double)
     */
    public void setMantleHeatFlow(Double val) {
        this.mantleHeatFlow = val;
    }

    /**
     * Get freshwater aquifer.
     *
     * @return the resulting computation or state reference
     */
    public Double getFreshwaterAquifer() {
        return freshwaterAquifer;
    }

    /**
     * Set freshwater aquifer.
     *
     * @param val the val parameter (Double)
     */
    public void setFreshwaterAquifer(Double val) {
        this.freshwaterAquifer = val;
    }

    /**
     * Get resource clay.
     *
     * @return the resulting computation or state reference
     */
    public Double getResourceClay() {
        return resourceClay;
    }

    /**
     * Set resource clay.
     *
     * @param val the val parameter (Double)
     */
    public void setResourceClay(Double val) {
        this.resourceClay = val;
    }

    /**
     * Get resource work.
     *
     * @return the resulting computation or state reference
     */
    public Double getResourceWork() {
        return resourceWork;
    }

    /**
     * Set resource work.
     *
     * @param val the val parameter (Double)
     */
    public void setResourceWork(Double val) {
        this.resourceWork = val;
    }

    /**
     * Get resource capital.
     *
     * @return the resulting computation or state reference
     */
    public Double getResourceCapital() {
        return resourceCapital;
    }

    /**
     * Set resource capital.
     *
     * @param val the val parameter (Double)
     */
    public void setResourceCapital(Double val) {
        this.resourceCapital = val;
    }

    /**
     * Get lifespan.
     *
     * @return the resulting computation or state reference
     */
    public Double getLifespan() {
        return lifespan;
    }

    /**
     * Set lifespan.
     *
     * @param val the val parameter (Double)
     */
    public void setLifespan(Double val) {
        this.lifespan = val;
    }

    /**
     * Get fertility.
     *
     * @return the resulting computation or state reference
     */
    public Double getFertility() {
        return fertility;
    }

    /**
     * Set fertility.
     *
     * @param val the val parameter (Double)
     */
    public void setFertility(Double val) {
        this.fertility = val;
    }

    /**
     * Get gini index.
     *
     * @return the resulting computation or state reference
     */
    public Double getGiniIndex() {
        return giniIndex;
    }

    /**
     * Set gini index.
     *
     * @param val the val parameter (Double)
     */
    public void setGiniIndex(Double val) {
        this.giniIndex = val;
    }

    /**
     * Get technology level.
     *
     * @return the resulting computation or state reference
     */
    public Double getTechnologyLevel() {
        return technologyLevel;
    }

    /**
     * Set technology level.
     *
     * @param val the val parameter (Double)
     */
    public void setTechnologyLevel(Double val) {
        this.technologyLevel = val;
    }

    /**
     * Gets the hominin species clade: 1 = Homo Sapiens, 2 = Neanderthal, 3 = Denisovan/Archaic.
     *
     * @return hominin species code
     */
    public Integer getHomininSpecies() {
        return homininSpecies != null ? homininSpecies : 1;
    }

    /**
     * Sets the hominin species clade.
     *
     * @param val hominin species code
     */
    public void setHomininSpecies(Integer val) {
        this.homininSpecies = val != null ? val : 1;
    }
    /**
     * Get accessible aquifer.
     *
     * @return the resulting computation or state reference
     */
    public Double getAccessibleAquifer() {
        return accessibleAquifer;
    }

    /**
     * Set accessible aquifer.
     *
     * @param accessibleAquifer the accessible aquifer parameter (Double)
     */
    public void setAccessibleAquifer(Double accessibleAquifer) {
        this.accessibleAquifer = accessibleAquifer;
    }

    /**
     * Get pollution level.
     *
     * @return the resulting computation or state reference
     */
    public Double getPollutionLevel() {
        return pollutionLevel;
    }

    /**
     * Set pollution level.
     *
     * @param pollutionLevel the pollution level parameter (Double)
     */
    public void setPollutionLevel(Double pollutionLevel) {
        this.pollutionLevel = pollutionLevel;
    }

    /**
     * Is boundary cell.
     *
     * @return the resulting computation or state reference
     */
    public boolean isBoundaryCell() {
        return boundaryCell;
    }

    /**
     * Set boundary cell.
     *
     * @param boundaryCell the boundary cell parameter (boolean)
     */
    public void setBoundaryCell(boolean boundaryCell) {
        this.boundaryCell = boundaryCell;
    }

    /**
     * Get movement friction.
     *
     * @return the resulting computation or state reference
     */
    public Double getMovementFriction() {
        return movementFriction;
    }

    /**
     * Set movement friction.
     *
     * @param movementFriction the movement friction parameter (Double)
     */
    public void setMovementFriction(Double movementFriction) {
        this.movementFriction = movementFriction;
    }

    /**
     * Get choke point score.
     *
     * @return topological trade bottleneck funneling score (0.0 to 1.0)
     */
    public Double getChokePointScore() {
        return chokePointScore != null ? chokePointScore : 0.0;
    }

    /**
     * Set choke point score.
     *
     * @param chokePointScore the choke point score (0.0 to 1.0)
     */
    public void setChokePointScore(Double chokePointScore) {
        this.chokePointScore = chokePointScore != null ? Math.clamp(chokePointScore, 0.0, 1.0) : 0.0;
    }

    /**
     * Get transit toll revenue.
     *
     * @return cumulative economic transit tariff rent collected in capital units
     */
    public Double getTransitTollRevenue() {
        return transitTollRevenue != null ? transitTollRevenue : 0.0;
    }

    /**
     * Set transit toll revenue.
     *
     * @param transitTollRevenue the transit toll revenue
     */
    public void setTransitTollRevenue(Double transitTollRevenue) {
        this.transitTollRevenue = transitTollRevenue != null ? Math.max(0.0, transitTollRevenue) : 0.0;
    }

    /**
     * Get pop youth.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPopYouth() {
        if (popYouth == null && population != null && population > 0) {
            updateAgePyramidFromTotal(technologyLevel != null && technologyLevel > 0 ? technologyLevel : 1.0);
        }
        return popYouth;
    }

    /**
     * Set pop youth.
     *
     * @param popYouth the pop youth parameter (Integer)
     */
    public void setPopYouth(Integer popYouth) {
        this.popYouth = popYouth;
    }

    /**
     * Get pop adult.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPopAdult() {
        if (popAdult == null && population != null && population > 0) {
            updateAgePyramidFromTotal(technologyLevel != null && technologyLevel > 0 ? technologyLevel : 1.0);
        }
        return popAdult;
    }

    /**
     * Set pop adult.
     *
     * @param popAdult the pop adult parameter (Integer)
     */
    public void setPopAdult(Integer popAdult) {
        this.popAdult = popAdult;
    }

    /**
     * Get pop elderly.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPopElderly() {
        if (popElderly == null && population != null && population > 0) {
            updateAgePyramidFromTotal(technologyLevel != null && technologyLevel > 0 ? technologyLevel : 1.0);
        }
        return popElderly;
    }

    /**
     * Set pop elderly.
     *
     * @param popElderly the pop elderly parameter (Integer)
     */
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
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
        double baseFriction = 1.0;

        // Elevation / Ruggedness penalty
        double elev = elevation != null ? elevation : 0.0;
        if (elev > 3000) baseFriction += 8.0;
        else if (elev > 1500) baseFriction += 4.0;
        else if (elev > 500) baseFriction += 1.5;
        else if (elev < 0) {
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

    /**
     * Get dynamic albedo.
     *
     * @return the resulting computation or state reference
     */
    public Double getDynamicAlbedo() {
        return dynamicAlbedo;
    }

    /**
     * Set dynamic albedo.
     *
     * @param dynamicAlbedo the dynamic albedo parameter (Double)
     */
    public void setDynamicAlbedo(Double dynamicAlbedo) {
        this.dynamicAlbedo = dynamicAlbedo;
    }

    /**
     * Get pop0to4.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPop0to4() { return pop0to4; }
    /**
     * Set pop0to4 operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param val the val argument (Integer)
     */
    public void setPop0to4(Integer val) { this.pop0to4 = val; }

    /**
     * Get pop5to14.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPop5to14() { return pop5to14; }
    /**
     * Set pop5to14 operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param val the val argument (Integer)
     */
    public void setPop5to14(Integer val) { this.pop5to14 = val; }

    /**
     * Get pop15to24.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPop15to24() { return pop15to24; }
    /**
     * Set pop15to24 operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param val the val argument (Integer)
     */
    public void setPop15to24(Integer val) { this.pop15to24 = val; }

    /**
     * Get pop25to49.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPop25to49() { return pop25to49; }
    /**
     * Set pop25to49 operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param val the val argument (Integer)
     */
    public void setPop25to49(Integer val) { this.pop25to49 = val; }

    /**
     * Get pop50to64.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPop50to64() { return pop50to64; }
    /**
     * Set pop50to64 operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param val the val argument (Integer)
     */
    public void setPop50to64(Integer val) { this.pop50to64 = val; }

    /**
     * Get pop65to79.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPop65to79() { return pop65to79; }
    /**
     * Set pop65to79 operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param val the val argument (Integer)
     */
    public void setPop65to79(Integer val) { this.pop65to79 = val; }

    /**
     * Get pop80plus.
     *
     * @return the resulting computation or state reference
     */
    public Integer getPop80Plus() { return pop80Plus; }
    /**
     * Set pop80plus operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param val the val argument (Integer)
     */
    public void setPop80Plus(Integer val) { this.pop80Plus = val; }

    /**
     * Get epidemic infected.
     *
     * @return the resulting computation or state reference
     */
    public Integer getEpidemicInfected() { return epidemicInfected; }
    /**
     * Set epidemic infected operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param val the val argument (Integer)
     */
    public void setEpidemicInfected(Integer val) { this.epidemicInfected = val; }

    /**
     * Get epidemic recovered.
     *
     * @return the resulting computation or state reference
     */
    public Integer getEpidemicRecovered() { return epidemicRecovered; }
    /**
     * Set epidemic recovered operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param val the val argument (Integer)
     */
    public void setEpidemicRecovered(Integer val) { this.epidemicRecovered = val; }

    /**
     * Get active pathogen name.
     *
     * @return the resulting computation or state reference
     */
    public String getActivePathogenName() { return activePathogenName; }
    /**
     * Set active pathogen name operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param name the name argument (String)
     */
    public void setActivePathogenName(String name) { this.activePathogenName = name; }

    /**
     * Get language group.
     *
     * @return the resulting computation or state reference
     */
    public String getLanguageGroup() { return languageGroup; }
    /**
     * Set language group operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param lang the lang argument (String)
     */
    public void setLanguageGroup(String lang) { this.languageGroup = lang; }

    /**
     * Get linguistic drift.
     *
     * @return the resulting computation or state reference
     */
    public Double getLinguisticDrift() { return linguisticDrift; }
    /**
     * Set linguistic drift operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param drift the drift argument (Double)
     */
    public void setLinguisticDrift(Double drift) { this.linguisticDrift = drift; }

    /**
     * Calculates dynamic surface albedo based on snow cover, biome, and natural vegetation density.
     */
    public double calculateDynamicAlbedo() {
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
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
        // Ingestion & Transformation: Parse raw geospatial/tabular records
        // Standardize coordinates, normalize projection tensors, and populate spatial index
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

    /**
     * Gets the 7-segment demographic age pyramid cohorts as an array:
     * [0..4, 5..14, 15..24, 25..49, 50..64, 65..79, 80+].
     *
     * @return 7-element array containing population counts for each age cohort
     */
    public int[] getAgePyramid() {
        if (pop0to4 == null && population != null && population > 0) {
            updateAgePyramidFromTotal(technologyLevel != null && technologyLevel > 0 ? technologyLevel : 1.0);
        }
        return new int[] {
            pop0to4 != null ? pop0to4 : 0,
            pop5to14 != null ? pop5to14 : 0,
            pop15to24 != null ? pop15to24 : 0,
            pop25to49 != null ? pop25to49 : 0,
            pop50to64 != null ? pop50to64 : 0,
            pop65to79 != null ? pop65to79 : 0,
            pop80Plus != null ? pop80Plus : 0
        };
    }

    /**
     * Calculates the total annual metabolic subsistence energy required by the cell's population (in GJ/year),
     * rigorously taking into account:
     * 1. Fine-grained 7-cohort age pyramid (infants, children, young adults, prime labor, mature adults, seniors, elderly).
     * 2. Physical Activity Level (PAL) modulated by manual agricultural/mining labor vs industrial mechanization (K/N).
     * 3. Hominin species metabolic differences (e.g., Neanderthals requiring ~40% more calories due to lean muscle mass and cold adaptation).
     *
     * @return annual caloric requirement in Gigajoules (GJ)
     */
    public double calculateAnnualMetabolicNeedGJ() {
        int totalPop = population != null ? population : 0;
        if (totalPop <= 0) return 0.0;

        // 1. Age-structured baseline consumption (FAO/WHO human dietary reference standards)
        int[] cohorts = getAgePyramid();
        // Annual GJ per capita by cohort for standard baseline sedentary human:
        // [0..4: 1.60 GJ (47%), 5..14: 2.50 GJ (74%), 15..24: 3.80 GJ (113%),
        //  25..49: 3.50 GJ (104%), 50..64: 3.20 GJ (95%), 65..79: 2.70 GJ (80%), 80+: 2.20 GJ (65%)]
        double cohortMetabolicSum = (cohorts[0] * 1.60)
                                  + (cohorts[1] * 2.50)
                                  + (cohorts[2] * 3.80)
                                  + (cohorts[3] * 3.50)
                                  + (cohorts[4] * 3.20)
                                  + (cohorts[5] * 2.70)
                                  + (cohorts[6] * 2.20);

        // Base average per capita metabolic intake
        double basePerCapitaGJ = cohortMetabolicSum / totalPop;

        // 2. Physical Activity Level (PAL) & Work Intensity:
        // Pre-industrial agrarian & manual societies require intense muscle power (PAL 1.8 - 2.1).
        // Industrial capital (K/N) and mechanization substitute fossil/electric work for muscle, lowering PAL to 1.4 - 1.5.
        // Heavy mineral extraction (ore/coal mining) adds metabolic strain.
        double cap = resourceCapital != null ? resourceCapital : 0.0;
        double capPerCapita = cap / totalPop;
        double tech = technologyLevel != null ? technologyLevel : 1.0;
        double mechanizationFactor = Math.clamp(capPerCapita / 40.0 + (tech >= 3.0 ? (tech - 3.0) * 0.2 : 0.0), 0.0, 1.0);

        // Labor intensity multiplier: 1.25 (heavy manual agrarian/pre-industrial) down to 0.95 (mechanized/sedentary)
        double laborMultiplier = 1.25 - (0.30 * mechanizationFactor);

        // Mining & metallurgical heavy labor surcharge:
        double metalStock = resourceMetal != null ? resourceMetal : 0.0;
        double coalStock = resourceCoal != null ? resourceCoal : 0.0;
        if (metalStock > 10.0 || coalStock > 50.0) {
            laborMultiplier += 0.08; // Heavy mining labor expenditure
        }

        // 3. Species / Hominin Clade Multiplier (Kleiber's Law, muscle density, and cold thermoregulation):
        // 1 = Homo Sapiens (1.0x baseline)
        // 2 = Homo Neanderthalensis (1.40x: 4,000-5,000 kcal/day, robust morphology, cold adaptation - Churchill 2006, Sorensen 2011)
        // 3 = Denisovan / Archaic (1.20x: robust archaic morphology & high altitude adaptation)
        double speciesMultiplier = 1.0;
        int species = getHomininSpecies();
        if (species == 2) {
            speciesMultiplier = 1.40;
        } else if (species == 3) {
            speciesMultiplier = 1.20;
        }

        return totalPop * basePerCapitaGJ * laborMultiplier * speciesMultiplier;
    }

    /**
     * Get ice sheet thickness meters operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @return the resulting computation or updated state reference
     */
    public Double getIceSheetThicknessMeters() { return iceSheetThicknessMeters != null ? iceSheetThicknessMeters : 0.0; }
    /**
     * Set ice sheet thickness meters operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param iceSheetThicknessMeters the ice sheet thickness meters argument (Double)
     */
    public void setIceSheetThicknessMeters(Double iceSheetThicknessMeters) { this.iceSheetThicknessMeters = iceSheetThicknessMeters; }

    /**
     * Get sea level offset meters operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @return the resulting computation or updated state reference
     */
    public Double getSeaLevelOffsetMeters() { return seaLevelOffsetMeters != null ? seaLevelOffsetMeters : 0.0; }
    /**
     * Set sea level offset meters operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param seaLevelOffsetMeters the sea level offset meters argument (Double)
     */
    public void setSeaLevelOffsetMeters(Double seaLevelOffsetMeters) { this.seaLevelOffsetMeters = seaLevelOffsetMeters; }

    /**
     * Get is coastal operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @return the resulting computation or updated state reference
     */
    public Boolean getIsCoastal() { return isCoastal != null ? isCoastal : false; }
    /**
     * Set is coastal operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param isCoastal the is coastal argument (Boolean)
     */
    public void setIsCoastal(Boolean isCoastal) { this.isCoastal = isCoastal; }

    /**
     * Get coastal marine resource operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @return the resulting computation or updated state reference
     */
    public Double getCoastalMarineResource() { return coastalMarineResource != null ? coastalMarineResource : 0.0; }
    /**
     * Set coastal marine resource operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param coastalMarineResource the coastal marine resource argument (Double)
     */
    public void setCoastalMarineResource(Double coastalMarineResource) { this.coastalMarineResource = coastalMarineResource; }

    /**
     * Get is polder operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @return the resulting computation or updated state reference
     */
    public Boolean getIsPolder() { return isPolder != null ? isPolder : false; }
    /**
     * Set is polder operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param isPolder the is polder argument (Boolean)
     */
    public void setIsPolder(Boolean isPolder) { this.isPolder = isPolder; }

    /**
     * Get has floating infrastructure operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @return the resulting computation or updated state reference
     */
    public Boolean getHasFloatingInfrastructure() { return hasFloatingInfrastructure != null ? hasFloatingInfrastructure : false; }
    /**
     * Set has floating infrastructure operation.
     * <p>
     * Executes operational logic for {@code H3Cell} within the geospatial raster and tensor ingestion pipeline.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param hasFloatingInfrastructure the has floating infrastructure argument (Boolean)
     */
    public void setHasFloatingInfrastructure(Boolean hasFloatingInfrastructure) { this.hasFloatingInfrastructure = hasFloatingInfrastructure; }

    /**
     * Standard average H3 hexagon area in km² according to resolution level (0 to 15).
     */
    public static double getAvgHexagonAreaKm2(int resolution) {
        return switch (resolution) {
            case 0 -> 4357449.42;
            case 1 -> 609788.44;
            case 2 -> 86801.78;
            case 3 -> 12393.43;
            case 4 -> 1770.49;
            case 5 -> 252.93;
            case 6 -> 36.1329;
            case 7 -> 5.1618;
            case 8 -> 0.7373276;
            case 9 -> 0.1053325;
            case 10 -> 0.0150475;
            default -> (resolution < 0) ? 4357449.42 : (0.7373276 * Math.pow(7.0, 8 - resolution));
        };
    }

    /**
     * Calculates the effective 3D real surface area of the cell in km², taking into account:
     * 1. H3 grid resolution level embedded in the H3 64-bit index.
     * 2. Topographical 3D slope expansion derived from terrain ruggedness / movement friction.
     * 
     * @return Effective 3D surface area in km² calibrated to actual grid resolution.
     */
    public double getEffectiveSurfaceAreaKm2() {
        int res = 3;
        if (h3Index != 0) {
            int r = (int) ((h3Index >>> 52) & 0xF);
            if (r >= 0 && r <= 15) {
                res = r;
            }
        }
        double baseArea = getAvgHexagonAreaKm2(res);
        
        // 3D Topographical slope expansion factor: slope declivity derived from movement friction
        double friction = movementFriction != null ? movementFriction : 1.0;
        double slopeFactor3D = Math.sqrt(1.0 + 0.15 * Math.pow(Math.max(0.0, friction - 1.0), 1.8));
        
        return baseArea * slopeFactor3D;
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
        copy.setSoilSalinity(this.soilSalinity);
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
        copy.setHomininSpecies(this.homininSpecies);
        copy.setOwner(this.owner); // Shared reference for now
        copy.setFluxPressure(this.fluxPressure);
        copy.setBoundaryCell(this.boundaryCell);
        copy.setMovementFriction(this.movementFriction);
        copy.setChokePointScore(this.chokePointScore);
        copy.setTransitTollRevenue(this.transitTollRevenue);
        copy.setPopYouth(this.getPopYouth());
        copy.setPopAdult(this.getPopAdult());
        copy.setPopElderly(this.getPopElderly());
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

        // Extraterrestrial Pressurized Habitats
        copy.setHabitatType(this.habitatType);
        copy.setHabitatTier(this.habitatTier);
        copy.setHabitatCapacity(this.habitatCapacity);
        copy.setHabitatIntegrity(this.habitatIntegrity);
        copy.setHabitatEnergyKw(this.habitatEnergyKw);
        copy.setHabitatAgeYears(this.habitatAgeYears);
        
        return copy;
    }

    /**
     * Get habitat type.
     *
     * @return the resulting computation or state reference
     */
    public org.ether.society.model.HabitatType getHabitatType() {
        return habitatType != null ? habitatType : org.ether.society.model.HabitatType.NONE;
    }

    /**
     * Set habitat type.
     *
     * @param habitatType the habitat type parameter (org.ether.society.model.HabitatType)
     */
    public void setHabitatType(org.ether.society.model.HabitatType habitatType) {
        this.habitatType = habitatType != null ? habitatType : org.ether.society.model.HabitatType.NONE;
    }

    /**
     * Get habitat tier.
     *
     * @return the resulting computation or state reference
     */
    public Integer getHabitatTier() {
        return habitatTier != null ? habitatTier : 0;
    }

    /**
     * Set habitat tier.
     *
     * @param habitatTier the habitat tier parameter (Integer)
     */
    public void setHabitatTier(Integer habitatTier) {
        this.habitatTier = habitatTier != null ? habitatTier : 0;
    }

    /**
     * Get habitat capacity.
     *
     * @return the resulting computation or state reference
     */
    public Double getHabitatCapacity() {
        return habitatCapacity != null ? habitatCapacity : 0.0;
    }

    /**
     * Set habitat capacity.
     *
     * @param habitatCapacity the habitat capacity parameter (Double)
     */
    public void setHabitatCapacity(Double habitatCapacity) {
        this.habitatCapacity = habitatCapacity != null ? Math.max(0.0, habitatCapacity) : 0.0;
    }

    /**
     * Get habitat integrity.
     *
     * @return the resulting computation or state reference
     */
    public Double getHabitatIntegrity() {
        return habitatIntegrity != null ? habitatIntegrity : 1.0;
    }

    /**
     * Set habitat integrity.
     *
     * @param habitatIntegrity the habitat integrity parameter (Double)
     */
    public void setHabitatIntegrity(Double habitatIntegrity) {
        this.habitatIntegrity = habitatIntegrity != null ? Math.clamp(habitatIntegrity, 0.0, 1.0) : 1.0;
    }

    /**
     * Get habitat energy kw.
     *
     * @return the resulting computation or state reference
     */
    public Double getHabitatEnergyKw() {
        return habitatEnergyKw != null ? habitatEnergyKw : 0.0;
    }

    /**
     * Set habitat energy kw.
     *
     * @param habitatEnergyKw the habitat energy kw parameter (Double)
     */
    public void setHabitatEnergyKw(Double habitatEnergyKw) {
        this.habitatEnergyKw = habitatEnergyKw != null ? Math.max(0.0, habitatEnergyKw) : 0.0;
    }

    /**
     * Get habitat age years.
     *
     * @return the resulting computation or state reference
     */
    public Double getHabitatAgeYears() {
        return habitatAgeYears != null ? habitatAgeYears : 0.0;
    }

    /**
     * Set habitat age years.
     *
     * @param habitatAgeYears the habitat age years parameter (Double)
     */
    public void setHabitatAgeYears(Double habitatAgeYears) {
        this.habitatAgeYears = habitatAgeYears != null ? Math.max(0.0, habitatAgeYears) : 0.0;
    }

    /**
     * Primitive-safe latitude getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if latitude is null
     * @return current value of latitude as primitive double
     */
    public double getLatitudeOr(double defaultValue) {
        return this.latitude != null ? this.latitude : defaultValue;
    }

    /**
     * Primitive-safe longitude getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if longitude is null
     * @return current value of longitude as primitive double
     */
    public double getLongitudeOr(double defaultValue) {
        return this.longitude != null ? this.longitude : defaultValue;
    }

    /**
     * Primitive-safe woodResource getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if woodResource is null
     * @return current value of woodResource as primitive double
     */
    public double getWoodResourceOr(double defaultValue) {
        return this.woodResource != null ? this.woodResource : defaultValue;
    }

    /**
     * Primitive-safe biomassHuman getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if biomassHuman is null
     * @return current value of biomassHuman as primitive double
     */
    public double getBiomassHumanOr(double defaultValue) {
        return this.biomassHuman != null ? this.biomassHuman : defaultValue;
    }

    /**
     * Primitive-safe biomassLivestock getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if biomassLivestock is null
     * @return current value of biomassLivestock as primitive double
     */
    public double getBiomassLivestockOr(double defaultValue) {
        return this.biomassLivestock != null ? this.biomassLivestock : defaultValue;
    }

    /**
     * Primitive-safe biomassFish getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if biomassFish is null
     * @return current value of biomassFish as primitive double
     */
    public double getBiomassFishOr(double defaultValue) {
        return this.biomassFish != null ? this.biomassFish : defaultValue;
    }

    /**
     * Primitive-safe biomassAgriculture getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if biomassAgriculture is null
     * @return current value of biomassAgriculture as primitive double
     */
    public double getBiomassAgricultureOr(double defaultValue) {
        return this.biomassAgriculture != null ? this.biomassAgriculture : defaultValue;
    }

    /**
     * Primitive-safe biomassNatural getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if biomassNatural is null
     * @return current value of biomassNatural as primitive double
     */
    public double getBiomassNaturalOr(double defaultValue) {
        return this.biomassNatural != null ? this.biomassNatural : defaultValue;
    }

    /**
     * Primitive-safe energyWind getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if energyWind is null
     * @return current value of energyWind as primitive double
     */
    public double getEnergyWindOr(double defaultValue) {
        return this.energyWind != null ? this.energyWind : defaultValue;
    }

    /**
     * Primitive-safe energySolar getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if energySolar is null
     * @return current value of energySolar as primitive double
     */
    public double getEnergySolarOr(double defaultValue) {
        return this.energySolar != null ? this.energySolar : defaultValue;
    }

    /**
     * Primitive-safe energyFire getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if energyFire is null
     * @return current value of energyFire as primitive double
     */
    public double getEnergyFireOr(double defaultValue) {
        return this.energyFire != null ? this.energyFire : defaultValue;
    }

    /**
     * Primitive-safe energySlaves getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if energySlaves is null
     * @return current value of energySlaves as primitive double
     */
    public double getEnergySlavesOr(double defaultValue) {
        return this.energySlaves != null ? this.energySlaves : defaultValue;
    }

    /**
     * Primitive-safe energyFoodConsumed getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if energyFoodConsumed is null
     * @return current value of energyFoodConsumed as primitive double
     */
    public double getEnergyFoodConsumedOr(double defaultValue) {
        return this.energyFoodConsumed != null ? this.energyFoodConsumed : defaultValue;
    }

    /**
     * Primitive-safe resourceMetal getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourceMetal is null
     * @return current value of resourceMetal as primitive double
     */
    public double getResourceMetalOr(double defaultValue) {
        return this.resourceMetal != null ? this.resourceMetal : defaultValue;
    }

    /**
     * Primitive-safe resourcePreciousMetal getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourcePreciousMetal is null
     * @return current value of resourcePreciousMetal as primitive double
     */
    public double getResourcePreciousMetalOr(double defaultValue) {
        return this.resourcePreciousMetal != null ? this.resourcePreciousMetal : defaultValue;
    }

    /**
     * Primitive-safe resourceClay getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourceClay is null
     * @return current value of resourceClay as primitive double
     */
    public double getResourceClayOr(double defaultValue) {
        return this.resourceClay != null ? this.resourceClay : defaultValue;
    }

    /**
     * Primitive-safe resourceWork getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourceWork is null
     * @return current value of resourceWork as primitive double
     */
    public double getResourceWorkOr(double defaultValue) {
        return this.resourceWork != null ? this.resourceWork : defaultValue;
    }

    /**
     * Primitive-safe resourceCapital getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourceCapital is null
     * @return current value of resourceCapital as primitive double
     */
    public double getResourceCapitalOr(double defaultValue) {
        return this.resourceCapital != null ? this.resourceCapital : defaultValue;
    }

    /**
     * Primitive-safe soilOrganicCarbon getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if soilOrganicCarbon is null
     * @return current value of soilOrganicCarbon as primitive double
     */
    public double getSoilOrganicCarbonOr(double defaultValue) {
        return this.soilOrganicCarbon != null ? this.soilOrganicCarbon : defaultValue;
    }

    /**
     * Primitive-safe soilSalinity getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if soilSalinity is null
     * @return current value of soilSalinity as primitive double
     */
    public double getSoilSalinityOr(double defaultValue) {
        return this.soilSalinity != null ? this.soilSalinity : defaultValue;
    }

    /**
     * Primitive-safe resourceCoal getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourceCoal is null
     * @return current value of resourceCoal as primitive double
     */
    public double getResourceCoalOr(double defaultValue) {
        return this.resourceCoal != null ? this.resourceCoal : defaultValue;
    }

    /**
     * Primitive-safe resourceOil getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourceOil is null
     * @return current value of resourceOil as primitive double
     */
    public double getResourceOilOr(double defaultValue) {
        return this.resourceOil != null ? this.resourceOil : defaultValue;
    }

    /**
     * Primitive-safe resourceGas getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourceGas is null
     * @return current value of resourceGas as primitive double
     */
    public double getResourceGasOr(double defaultValue) {
        return this.resourceGas != null ? this.resourceGas : defaultValue;
    }

    /**
     * Primitive-safe resourceUranium getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourceUranium is null
     * @return current value of resourceUranium as primitive double
     */
    public double getResourceUraniumOr(double defaultValue) {
        return this.resourceUranium != null ? this.resourceUranium : defaultValue;
    }

    /**
     * Primitive-safe resourceRareEarths getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if resourceRareEarths is null
     * @return current value of resourceRareEarths as primitive double
     */
    public double getResourceRareEarthsOr(double defaultValue) {
        return this.resourceRareEarths != null ? this.resourceRareEarths : defaultValue;
    }

    /**
     * Primitive-safe mantleHeatFlow getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if mantleHeatFlow is null
     * @return current value of mantleHeatFlow as primitive double
     */
    public double getMantleHeatFlowOr(double defaultValue) {
        return this.mantleHeatFlow != null ? this.mantleHeatFlow : defaultValue;
    }

    /**
     * Primitive-safe freshwaterAquifer getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if freshwaterAquifer is null
     * @return current value of freshwaterAquifer as primitive double
     */
    public double getFreshwaterAquiferOr(double defaultValue) {
        return this.freshwaterAquifer != null ? this.freshwaterAquifer : defaultValue;
    }

    /**
     * Primitive-safe accessibleAquifer getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if accessibleAquifer is null
     * @return current value of accessibleAquifer as primitive double
     */
    public double getAccessibleAquiferOr(double defaultValue) {
        return this.accessibleAquifer != null ? this.accessibleAquifer : defaultValue;
    }

    /**
     * Primitive-safe pollutionLevel getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if pollutionLevel is null
     * @return current value of pollutionLevel as primitive double
     */
    public double getPollutionLevelOr(double defaultValue) {
        return this.pollutionLevel != null ? this.pollutionLevel : defaultValue;
    }

    /**
     * Primitive-safe lifespan getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if lifespan is null
     * @return current value of lifespan as primitive double
     */
    public double getLifespanOr(double defaultValue) {
        return this.lifespan != null ? this.lifespan : defaultValue;
    }

    /**
     * Primitive-safe fertility getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if fertility is null
     * @return current value of fertility as primitive double
     */
    public double getFertilityOr(double defaultValue) {
        return this.fertility != null ? this.fertility : defaultValue;
    }

    /**
     * Primitive-safe giniIndex getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if giniIndex is null
     * @return current value of giniIndex as primitive double
     */
    public double getGiniIndexOr(double defaultValue) {
        return this.giniIndex != null ? this.giniIndex : defaultValue;
    }

    /**
     * Primitive-safe technologyLevel getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if technologyLevel is null
     * @return current value of technologyLevel as primitive double
     */
    public double getTechnologyLevelOr(double defaultValue) {
        return this.technologyLevel != null ? this.technologyLevel : defaultValue;
    }

    /**
     * Primitive-safe movementFriction getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if movementFriction is null
     * @return current value of movementFriction as primitive double
     */
    public double getMovementFrictionOr(double defaultValue) {
        return this.movementFriction != null ? this.movementFriction : defaultValue;
    }

    /**
     * Primitive-safe chokePointScore getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if chokePointScore is null
     * @return current value of chokePointScore as primitive double
     */
    public double getChokePointScoreOr(double defaultValue) {
        return this.chokePointScore != null ? this.chokePointScore : defaultValue;
    }

    /**
     * Primitive-safe transitTollRevenue getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if transitTollRevenue is null
     * @return current value of transitTollRevenue as primitive double
     */
    public double getTransitTollRevenueOr(double defaultValue) {
        return this.transitTollRevenue != null ? this.transitTollRevenue : defaultValue;
    }

    /**
     * Primitive-safe dynamicAlbedo getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if dynamicAlbedo is null
     * @return current value of dynamicAlbedo as primitive double
     */
    public double getDynamicAlbedoOr(double defaultValue) {
        return this.dynamicAlbedo != null ? this.dynamicAlbedo : defaultValue;
    }

    /**
     * Primitive-safe iceSheetThicknessMeters getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if iceSheetThicknessMeters is null
     * @return current value of iceSheetThicknessMeters as primitive double
     */
    public double getIceSheetThicknessMetersOr(double defaultValue) {
        return this.iceSheetThicknessMeters != null ? this.iceSheetThicknessMeters : defaultValue;
    }

    /**
     * Primitive-safe seaLevelOffsetMeters getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if seaLevelOffsetMeters is null
     * @return current value of seaLevelOffsetMeters as primitive double
     */
    public double getSeaLevelOffsetMetersOr(double defaultValue) {
        return this.seaLevelOffsetMeters != null ? this.seaLevelOffsetMeters : defaultValue;
    }

    /**
     * Primitive-safe coastalMarineResource getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if coastalMarineResource is null
     * @return current value of coastalMarineResource as primitive double
     */
    public double getCoastalMarineResourceOr(double defaultValue) {
        return this.coastalMarineResource != null ? this.coastalMarineResource : defaultValue;
    }

    /**
     * Primitive-safe habitatCapacity getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if habitatCapacity is null
     * @return current value of habitatCapacity as primitive double
     */
    public double getHabitatCapacityOr(double defaultValue) {
        return this.habitatCapacity != null ? this.habitatCapacity : defaultValue;
    }

    /**
     * Primitive-safe habitatIntegrity getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if habitatIntegrity is null
     * @return current value of habitatIntegrity as primitive double
     */
    public double getHabitatIntegrityOr(double defaultValue) {
        return this.habitatIntegrity != null ? this.habitatIntegrity : defaultValue;
    }

    /**
     * Primitive-safe habitatEnergyKw getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if habitatEnergyKw is null
     * @return current value of habitatEnergyKw as primitive double
     */
    public double getHabitatEnergyKwOr(double defaultValue) {
        return this.habitatEnergyKw != null ? this.habitatEnergyKw : defaultValue;
    }

    /**
     * Primitive-safe habitatAgeYears getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if habitatAgeYears is null
     * @return current value of habitatAgeYears as primitive double
     */
    public double getHabitatAgeYearsOr(double defaultValue) {
        return this.habitatAgeYears != null ? this.habitatAgeYears : defaultValue;
    }

    /**
     * Primitive-safe linguisticDrift getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if linguisticDrift is null
     * @return current value of linguisticDrift as primitive double
     */
    public double getLinguisticDriftOr(double defaultValue) {
        return this.linguisticDrift != null ? this.linguisticDrift : defaultValue;
    }

    /**
     * Primitive-safe homininSpecies getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if homininSpecies is null
     * @return current value of homininSpecies as primitive int
     */
    public int getHomininSpeciesOr(int defaultValue) {
        return this.homininSpecies != null ? this.homininSpecies : defaultValue;
    }

    /**
     * Primitive-safe habitatTier getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if habitatTier is null
     * @return current value of habitatTier as primitive int
     */
    public int getHabitatTierOr(int defaultValue) {
        return this.habitatTier != null ? this.habitatTier : defaultValue;
    }

    /**
     * Primitive-safe pop0to4 getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if pop0to4 is null
     * @return current value of pop0to4 as primitive int
     */
    public int getPop0to4Or(int defaultValue) {
        return this.pop0to4 != null ? this.pop0to4 : defaultValue;
    }

    /**
     * Primitive-safe pop5to14 getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if pop5to14 is null
     * @return current value of pop5to14 as primitive int
     */
    public int getPop5to14Or(int defaultValue) {
        return this.pop5to14 != null ? this.pop5to14 : defaultValue;
    }

    /**
     * Primitive-safe pop15to24 getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if pop15to24 is null
     * @return current value of pop15to24 as primitive int
     */
    public int getPop15to24Or(int defaultValue) {
        return this.pop15to24 != null ? this.pop15to24 : defaultValue;
    }

    /**
     * Primitive-safe pop25to49 getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if pop25to49 is null
     * @return current value of pop25to49 as primitive int
     */
    public int getPop25to49Or(int defaultValue) {
        return this.pop25to49 != null ? this.pop25to49 : defaultValue;
    }

    /**
     * Primitive-safe pop50to64 getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if pop50to64 is null
     * @return current value of pop50to64 as primitive int
     */
    public int getPop50to64Or(int defaultValue) {
        return this.pop50to64 != null ? this.pop50to64 : defaultValue;
    }

    /**
     * Primitive-safe pop65to79 getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if pop65to79 is null
     * @return current value of pop65to79 as primitive int
     */
    public int getPop65to79Or(int defaultValue) {
        return this.pop65to79 != null ? this.pop65to79 : defaultValue;
    }

    /**
     * Primitive-safe pop80Plus getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if pop80Plus is null
     * @return current value of pop80Plus as primitive int
     */
    public int getPop80PlusOr(int defaultValue) {
        return this.pop80Plus != null ? this.pop80Plus : defaultValue;
    }

    /**
     * Primitive-safe popYouth getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if popYouth is null
     * @return current value of popYouth as primitive int
     */
    public int getPopYouthOr(int defaultValue) {
        return this.popYouth != null ? this.popYouth : defaultValue;
    }

    /**
     * Primitive-safe popAdult getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if popAdult is null
     * @return current value of popAdult as primitive int
     */
    public int getPopAdultOr(int defaultValue) {
        return this.popAdult != null ? this.popAdult : defaultValue;
    }

    /**
     * Primitive-safe popElderly getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if popElderly is null
     * @return current value of popElderly as primitive int
     */
    public int getPopElderlyOr(int defaultValue) {
        return this.popElderly != null ? this.popElderly : defaultValue;
    }

    /**
     * Primitive-safe epidemicInfected getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if epidemicInfected is null
     * @return current value of epidemicInfected as primitive int
     */
    public int getEpidemicInfectedOr(int defaultValue) {
        return this.epidemicInfected != null ? this.epidemicInfected : defaultValue;
    }

    /**
     * Primitive-safe epidemicRecovered getter avoiding NPEs and unboxing overhead.
     *
     * @param defaultValue fallback value if epidemicRecovered is null
     * @return current value of epidemicRecovered as primitive int
     */
    public int getEpidemicRecoveredOr(int defaultValue) {
        return this.epidemicRecovered != null ? this.epidemicRecovered : defaultValue;
    }
}
