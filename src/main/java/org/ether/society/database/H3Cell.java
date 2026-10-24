/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.ether.society.database.components.BiomassState;
import org.ether.society.database.components.ClimateState;
import org.ether.society.database.components.DemographicState;
import org.ether.society.database.components.MineralState;
import org.ether.society.model.Biome;
import org.ether.society.model.HabitatType;

/**
 * <h1>H3Cell</h1>
 * <p>
 * JPA Entity representing an H3 hexagonal cell.<br>
 * Stores terrain, climate, ecological, demographic, and socio-economic tensor data for a single hexagonal cell.
 * Ingests global planetary datasets (NOAA ETOPO, WorldClim, UNESCO WHYMAP, Natural Earth, Seshat, D-PLACE) into standardized H3 hexagonal rasters.
 * </p>
 * <p>
 * Analytical equations (movement friction, dynamic surface albedo, 7-cohort demographic age pyramid,
 * metabolic subsistence needs, 3D surface area, and DOD component transformations) are delegated to
 * {@link H3CellPhysicsAndStateHelper}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @since 1.0.0
 * @see H3CellPhysicsAndStateHelper
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

    /** H3 hexagon index (primary geospatial identifier). */
    @Column(name = "h3_index", nullable = false, unique = true)
    private Long h3Index;

    /** Center latitude in degrees. */
    @Column(nullable = false)
    private Double latitude;

    /** Center longitude in degrees. */
    @Column(nullable = false)
    private Double longitude;

    /** Mean elevation in meters (from SRTM/ETOPO data). */
    @Column(nullable = false)
    private Double elevation;

    /** Current surface temperature in Celsius ($^\circ\text{C}$). */
    @Column(nullable = false)
    private Double temperature;

    /** Annual rainfall in millimeters ($\text{mm/yr}$). */
    @Column(nullable = false)
    private Double rainfall;

    /** Biome classification. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Biome biome;

    /** Human population density (headcount per hexagon). */
    @Column(nullable = false)
    private Integer population = 0;

    /** Food resource availability / caloric stockpile in Gigajoules ($\text{GJ}$). */
    @Column(nullable = false)
    private Double foodResource = 0.0;

    /** Water resource availability in thousand cubic meters ($10^3\,\text{m}^3$). */
    @Column(nullable = false)
    private Double waterResource = 0.0;

    /** Wood and combustible forestry biomass stockpile in metric tonnes ($\text{t}$). */
    @Column(nullable = false)
    private Double woodResource = 0.0;

    // --- Detailed Tracking (Biomass in metric tonnes [t]) ---
    @Column(nullable = false) private Double biomassHuman = 0.0;
    @Column(nullable = false) private Double biomassLivestock = 0.0;
    @Column(nullable = false) private Double biomassFish = 0.0;
    @Column(nullable = false) private Double biomassAgriculture = 0.0;
    @Column(nullable = false) private Double biomassNatural = 0.0;

    // --- Detailed Tracking (Energy in Gigajoules [GJ]) ---
    @Column(nullable = false) private Double energyWind = 0.0;
    @Column(nullable = false) private Double energySolar = 0.0;
    @Column(nullable = false) private Double energyFire = 0.0;
    @Column(nullable = false) private Double energySlaves = 0.0;
    @Column(nullable = false) private Double energyFoodConsumed = 0.0;

    // --- Detailed Tracking (Resources) ---
    @Column(nullable = false) private Double resourceMetal = 0.0;
    @Column(nullable = false) private Double resourcePreciousMetal = 0.0;
    @Column(nullable = false) private Double resourceClay = 0.0;
    @Column(nullable = false) private Double resourceWork = 0.0;
    @Column(nullable = false) private Double resourceCapital = 0.0;
    @Column(nullable = false) private Double soilOrganicCarbon = 0.0;
    @Column(nullable = false) private Double soilSalinity = 0.0;
    @Column(nullable = false) private Double resourceCoal = 0.0;
    @Column(nullable = false) private Double resourceOil = 0.0;
    @Column(nullable = false) private Double resourceGas = 0.0;
    @Column(nullable = false) private Double resourceUranium = 0.0;
    @Column(nullable = false) private Double resourceRareEarths = 0.0;
    @Column(nullable = false) private Double mantleHeatFlow = 87.0;
    @Column(nullable = false) private Double freshwaterAquifer = 0.0;
    @Column(nullable = false) private Double accessibleAquifer = 0.0;
    @Column(nullable = false) private Double pollutionLevel = 0.0;

    // --- Socio-Economic Indices ---
    @Column(nullable = false) private Double lifespan = 40.0;
    @Column(nullable = false) private Double fertility = 6.0;
    @Column(nullable = false) private Double giniIndex = 0.0;
    @Column(nullable = false) private Double technologyLevel = 0.0;
    /** Hominin species clade: 1 = Homo Sapiens, 2 = Homo Neanderthalensis, 3 = Denisovan / Archaic. */
    @Column(nullable = false) private Integer homininSpecies = 1;

    // --- Movement & Terrain Friction Matrix ---
    @Column(nullable = false) private Double movementFriction = 1.0;
    @Column(nullable = false) private Double chokePointScore = 0.0;
    @Column(nullable = false) private Double transitTollRevenue = 0.0;

    // --- Dynamic Climate & Surface Physics ---
    @Column(nullable = false) private Double dynamicAlbedo = 0.30;
    @Column(nullable = false) private Double iceSheetThicknessMeters = 0.0;
    @Column(nullable = false) private Double seaLevelOffsetMeters = 0.0;
    @Column(nullable = false) private Boolean isCoastal = false;
    @Column(nullable = false) private Double coastalMarineResource = 0.0;
    @Column(nullable = false) private Boolean isPolder = false;
    @Column(nullable = false) private Boolean hasFloatingInfrastructure = false;

    // --- Extraterrestrial Pressurized Habitats & Infrastructure ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HabitatType habitatType = HabitatType.NONE;
    @Column(nullable = false) private Integer habitatTier = 0;
    @Column(nullable = false) private Double habitatCapacity = 0.0;
    @Column(nullable = false) private Double habitatIntegrity = 1.0;
    @Column(nullable = false) private Double habitatEnergyKw = 0.0;
    @Column(nullable = false) private Double habitatAgeYears = 0.0;

    // --- Detailed Demographic Age Pyramid (7 Fine-Grained Cohorts) ---
    @Column(nullable = false) private Integer pop0to4 = 0;
    @Column(nullable = false) private Integer pop5to14 = 0;
    @Column(nullable = false) private Integer pop15to24 = 0;
    @Column(nullable = false) private Integer pop25to49 = 0;
    @Column(nullable = false) private Integer pop50to64 = 0;
    @Column(nullable = false) private Integer pop65to79 = 0;
    @Column(nullable = false) private Integer pop80Plus = 0;

    // Demographic cohort aggregations
    @Column(nullable = false) private Integer popYouth = 0;
    @Column(nullable = false) private Integer popAdult = 0;
    @Column(nullable = false) private Integer popElderly = 0;

    // --- Epidemiological SEIR Model State ---
    @Column(nullable = false) private Integer epidemicInfected = 0;
    @Column(nullable = false) private Integer epidemicRecovered = 0;
    @Column(length = 50) private String activePathogenName = null;

    // --- Linguistic & Cultural Diffusion ---
    @Column(length = 50) private String languageGroup = "Proto-Human";
    @Column(nullable = false) private Double linguisticDrift = 0.0;

    // Analytics / Simulation State
    @Transient private double fluxPressure = 0.0;
    @Transient @JsonIgnore private Polity owner;
    @Transient private boolean boundaryCell = false;

    /** Default JPA constructor. */
    public H3Cell() {}

    /**
     * Constructs an H3Cell at the given index and geographic coordinates.
     *
     * @param h3Index   H3 64-bit hexagonal index
     * @param latitude  center latitude in degrees
     * @param longitude center longitude in degrees
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

    // =========================================================================
    // ANALYTICAL & DOMAIN METHODS (DELEGATED TO H3CellPhysicsAndStateHelper)
    // =========================================================================

    /**
     * Calculates terrain movement friction (cost-distance factor) based on elevation, biome, and technology level.
     *
     * @param techLevel technology level ($1.0$ = ancient, $10.0$ = modern)
     * @return movement friction multiplier
     */
    public double calculateMovementFriction(double techLevel) {
        return H3CellPhysicsAndStateHelper.calculateMovementFriction(this, techLevel);
    }

    /**
     * Calculates dynamic surface albedo based on snow cover, biome, and natural vegetation density.
     *
     * @return updated dynamic albedo $\alpha \in [0.06, 0.85]$
     */
    public double calculateDynamicAlbedo() {
        return H3CellPhysicsAndStateHelper.calculateDynamicAlbedo(this);
    }

    /**
     * Updates the detailed 7-segment age pyramid based on total population and demographic transition stage.
     *
     * @param techLevel technological level
     */
    public void updateAgePyramidFromTotal(double techLevel) {
        H3CellPhysicsAndStateHelper.updateAgePyramidFromTotal(this, techLevel);
    }

    /**
     * Gets the 7-segment demographic age pyramid cohorts as an array:
     * {@code [0..4, 5..14, 15..24, 25..49, 50..64, 65..79, 80+]}.
     *
     * @return 7-element array containing population counts for each age cohort
     */
    public int[] getAgePyramid() {
        if (pop0to4 == null && population != null && population > 0) {
            updateAgePyramidFromTotal(technologyLevel != null && technologyLevel > 0 ? technologyLevel : 1.0);
        }
        return new int[] {
            getPop0to4Or(0), getPop5to14Or(0), getPop15to24Or(0),
            getPop25to49Or(0), getPop50to64Or(0), getPop65to79Or(0), getPop80PlusOr(0)
        };
    }

    /**
     * Calculates the total annual metabolic subsistence energy required by the cell's population in $\text{GJ/year}$.
     *
     * @return annual caloric requirement in Gigajoules ($\text{GJ}$)
     */
    public double calculateAnnualMetabolicNeedGJ() {
        return H3CellPhysicsAndStateHelper.calculateAnnualMetabolicNeedGJ(this);
    }

    /**
     * Standard average H3 hexagon area in $\text{km}^2$ according to resolution level ($0$ to $15$).
     *
     * @param resolution H3 resolution level
     * @return average area in $\text{km}^2$
     */
    public static double getAvgHexagonAreaKm2(int resolution) {
        return H3CellPhysicsAndStateHelper.getAvgHexagonAreaKm2(resolution);
    }

    /**
     * Calculates the effective 3D real surface area of the cell in $\text{km}^2$.
     *
     * @return effective 3D surface area in $\text{km}^2$
     */
    public double getEffectiveSurfaceAreaKm2() {
        return H3CellPhysicsAndStateHelper.getEffectiveSurfaceAreaKm2(this);
    }

    /**
     * Creates a deep snapshot copy of this cell.
     *
     * @return cloned {@link H3Cell} snapshot
     */
    public H3Cell snapshot() {
        return H3CellPhysicsAndStateHelper.snapshot(this);
    }

    // =========================================================================
    // DATA-ORIENTED DESIGN (DOD) COMPONENT ACCESSORS
    // =========================================================================

    public ClimateState getClimateState() { return H3CellPhysicsAndStateHelper.extractClimateState(this); }
    public void setClimateState(ClimateState state) { H3CellPhysicsAndStateHelper.applyClimateState(this, state); }

    public BiomassState getBiomassState() { return H3CellPhysicsAndStateHelper.extractBiomassState(this); }
    public void setBiomassState(BiomassState state) { H3CellPhysicsAndStateHelper.applyBiomassState(this, state); }

    public DemographicState getDemographicState() { return H3CellPhysicsAndStateHelper.extractDemographicState(this); }
    public void setDemographicState(DemographicState state) { H3CellPhysicsAndStateHelper.applyDemographicState(this, state); }

    public MineralState getMineralState() { return H3CellPhysicsAndStateHelper.extractMineralState(this); }
    public void setMineralState(MineralState state) { H3CellPhysicsAndStateHelper.applyMineralState(this, state); }

    // =========================================================================
    // STANDARD GETTERS & SETTERS
    // =========================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getH3Index() { return h3Index; }
    public void setH3Index(Long h3Index) { this.h3Index = h3Index; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getElevation() { return elevation; }
    public void setElevation(Double elevation) { this.elevation = elevation; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getRainfall() { return rainfall; }
    public void setRainfall(Double rainfall) { this.rainfall = rainfall; }

    public Biome getBiome() { return biome; }
    public void setBiome(Biome biome) { this.biome = biome; }

    public Integer getPopulation() { return population; }
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

    public Double getFoodResource() { return foodResource; }
    public void setFoodResource(Double foodResource) { this.foodResource = foodResource; }

    public Double getWaterResource() { return waterResource; }
    public void setWaterResource(Double waterResource) { this.waterResource = waterResource; }

    public Double getWoodResource() { return woodResource; }
    public void setWoodResource(Double woodResource) { this.woodResource = woodResource; }

    public double getFluxPressure() { return fluxPressure; }
    public void setFluxPressure(double fluxPressure) { this.fluxPressure = fluxPressure; }

    @JsonIgnore public Polity getOwner() { return owner; }
    @JsonIgnore public void setOwner(Polity owner) { this.owner = owner; }

    public Double getBiomassHuman() {
        return biomassHuman != null ? biomassHuman : (population != null ? population.doubleValue() : 0.0);
    }
    public void setBiomassHuman(Double biomassHuman) {
        this.biomassHuman = biomassHuman != null ? biomassHuman : 0.0;
        this.population = (int) Math.round(this.biomassHuman);
    }

    public Double getBiomassLivestock() { return biomassLivestock; }
    public void setBiomassLivestock(Double val) { this.biomassLivestock = val; }

    public Double getBiomassFish() { return biomassFish; }
    public void setBiomassFish(Double val) { this.biomassFish = val; }

    public Double getBiomassAgriculture() { return biomassAgriculture; }
    public void setBiomassAgriculture(Double val) { this.biomassAgriculture = val; }

    public Double getBiomassNatural() { return biomassNatural; }
    public void setBiomassNatural(Double val) { this.biomassNatural = val; }

    public Double getEnergyWind() { return energyWind; }
    public void setEnergyWind(Double val) { this.energyWind = val; }

    public Double getEnergySolar() { return energySolar; }
    public void setEnergySolar(Double val) { this.energySolar = val; }

    public Double getEnergyFire() { return energyFire; }
    public void setEnergyFire(Double val) { this.energyFire = val; }

    public Double getEnergySlaves() { return energySlaves; }
    public void setEnergySlaves(Double val) { this.energySlaves = val; }

    public Double getEnergyFoodConsumed() { return energyFoodConsumed; }
    public void setEnergyFoodConsumed(Double val) { this.energyFoodConsumed = val; }

    public Double getResourceMetal() { return resourceMetal; }
    public void setResourceMetal(Double val) { this.resourceMetal = val; }

    public Double getResourcePreciousMetal() { return resourcePreciousMetal; }
    public void setResourcePreciousMetal(Double val) { this.resourcePreciousMetal = val; }

    public Double getSoilOrganicCarbon() { return soilOrganicCarbon; }
    public void setSoilOrganicCarbon(Double val) { this.soilOrganicCarbon = val; }

    public Double getSoilSalinity() { return soilSalinity != null ? soilSalinity : 0.0; }
    public void setSoilSalinity(Double val) { this.soilSalinity = val != null ? val : 0.0; }

    public Double getResourceCoal() { return resourceCoal != null ? resourceCoal : 0.0; }
    public void setResourceCoal(Double val) { this.resourceCoal = val != null ? Math.max(0.0, val) : 0.0; }

    public Double getResourceOil() { return resourceOil != null ? resourceOil : 0.0; }
    public void setResourceOil(Double val) { this.resourceOil = val != null ? Math.max(0.0, val) : 0.0; }

    public Double getResourceGas() { return resourceGas != null ? resourceGas : 0.0; }
    public void setResourceGas(Double val) { this.resourceGas = val != null ? Math.max(0.0, val) : 0.0; }

    public Double getResourceUranium() { return resourceUranium != null ? resourceUranium : 0.0; }
    public void setResourceUranium(Double val) { this.resourceUranium = val != null ? Math.max(0.0, val) : 0.0; }

    public Double getResourceRareEarths() { return resourceRareEarths != null ? resourceRareEarths : 0.0; }
    public void setResourceRareEarths(Double val) { this.resourceRareEarths = val != null ? Math.max(0.0, val) : 0.0; }

    public Double getMantleHeatFlow() { return mantleHeatFlow; }
    public void setMantleHeatFlow(Double val) { this.mantleHeatFlow = val; }

    public Double getFreshwaterAquifer() { return freshwaterAquifer; }
    public void setFreshwaterAquifer(Double val) { this.freshwaterAquifer = val; }

    public Double getAccessibleAquifer() { return accessibleAquifer; }
    public void setAccessibleAquifer(Double accessibleAquifer) { this.accessibleAquifer = accessibleAquifer; }

    public Double getPollutionLevel() { return pollutionLevel; }
    public void setPollutionLevel(Double pollutionLevel) { this.pollutionLevel = pollutionLevel; }

    public Double getResourceClay() { return resourceClay; }
    public void setResourceClay(Double val) { this.resourceClay = val; }

    public Double getResourceWork() { return resourceWork; }
    public void setResourceWork(Double val) { this.resourceWork = val; }

    public Double getResourceCapital() { return resourceCapital; }
    public void setResourceCapital(Double val) { this.resourceCapital = val; }

    public Double getLifespan() { return lifespan; }
    public void setLifespan(Double val) { this.lifespan = val; }

    public Double getFertility() { return fertility; }
    public void setFertility(Double val) { this.fertility = val; }

    public Double getGiniIndex() { return giniIndex; }
    public void setGiniIndex(Double val) { this.giniIndex = val; }

    public Double getTechnologyLevel() { return technologyLevel; }
    public void setTechnologyLevel(Double val) { this.technologyLevel = val; }

    public Integer getHomininSpecies() { return homininSpecies != null ? homininSpecies : 1; }
    public void setHomininSpecies(Integer val) { this.homininSpecies = val != null ? val : 1; }

    public boolean isBoundaryCell() { return boundaryCell; }
    public void setBoundaryCell(boolean boundaryCell) { this.boundaryCell = boundaryCell; }

    public Double getMovementFriction() { return movementFriction; }
    public void setMovementFriction(Double movementFriction) { this.movementFriction = movementFriction; }

    public Double getChokePointScore() { return chokePointScore != null ? chokePointScore : 0.0; }
    public void setChokePointScore(Double chokePointScore) {
        this.chokePointScore = chokePointScore != null ? Math.clamp(chokePointScore, 0.0, 1.0) : 0.0;
    }

    public Double getTransitTollRevenue() { return transitTollRevenue != null ? transitTollRevenue : 0.0; }
    public void setTransitTollRevenue(Double transitTollRevenue) {
        this.transitTollRevenue = transitTollRevenue != null ? Math.max(0.0, transitTollRevenue) : 0.0;
    }

    public Integer getPopYouth() {
        if (popYouth == null && population != null && population > 0) {
            updateAgePyramidFromTotal(technologyLevel != null && technologyLevel > 0 ? technologyLevel : 1.0);
        }
        return popYouth;
    }
    public void setPopYouth(Integer popYouth) { this.popYouth = popYouth; }

    public Integer getPopAdult() {
        if (popAdult == null && population != null && population > 0) {
            updateAgePyramidFromTotal(technologyLevel != null && technologyLevel > 0 ? technologyLevel : 1.0);
        }
        return popAdult;
    }
    public void setPopAdult(Integer popAdult) { this.popAdult = popAdult; }

    public Integer getPopElderly() {
        if (popElderly == null && population != null && population > 0) {
            updateAgePyramidFromTotal(technologyLevel != null && technologyLevel > 0 ? technologyLevel : 1.0);
        }
        return popElderly;
    }
    public void setPopElderly(Integer popElderly) { this.popElderly = popElderly; }

    public Double getDynamicAlbedo() { return dynamicAlbedo; }
    public void setDynamicAlbedo(Double dynamicAlbedo) { this.dynamicAlbedo = dynamicAlbedo; }

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

    public HabitatType getHabitatType() { return habitatType != null ? habitatType : HabitatType.NONE; }
    public void setHabitatType(HabitatType habitatType) { this.habitatType = habitatType != null ? habitatType : HabitatType.NONE; }

    public Integer getHabitatTier() { return habitatTier != null ? habitatTier : 0; }
    public void setHabitatTier(Integer habitatTier) { this.habitatTier = habitatTier != null ? habitatTier : 0; }

    public Double getHabitatCapacity() { return habitatCapacity != null ? habitatCapacity : 0.0; }
    public void setHabitatCapacity(Double habitatCapacity) { this.habitatCapacity = habitatCapacity != null ? Math.max(0.0, habitatCapacity) : 0.0; }

    public Double getHabitatIntegrity() { return habitatIntegrity != null ? habitatIntegrity : 1.0; }
    public void setHabitatIntegrity(Double habitatIntegrity) { this.habitatIntegrity = habitatIntegrity != null ? Math.clamp(habitatIntegrity, 0.0, 1.0) : 1.0; }

    public Double getHabitatEnergyKw() { return habitatEnergyKw != null ? habitatEnergyKw : 0.0; }
    public void setHabitatEnergyKw(Double habitatEnergyKw) { this.habitatEnergyKw = habitatEnergyKw != null ? Math.max(0.0, habitatEnergyKw) : 0.0; }

    public Double getHabitatAgeYears() { return habitatAgeYears != null ? habitatAgeYears : 0.0; }
    public void setHabitatAgeYears(Double habitatAgeYears) { this.habitatAgeYears = habitatAgeYears != null ? Math.max(0.0, habitatAgeYears) : 0.0; }

    // =========================================================================
    // PRIMITIVE-SAFE UNBOXING ACCESSORS (ZERO-OVERHEAD HOT-PATH GETTERS)
    // =========================================================================

    public double getElevationOr(double defaultValue) { return elevation != null ? elevation : defaultValue; }
    public double getTemperatureOr(double defaultValue) { return temperature != null ? temperature : defaultValue; }
    public double getRainfallOr(double defaultValue) { return rainfall != null ? rainfall : defaultValue; }
    public int getPopulationOr(int defaultValue) { return population != null ? population : defaultValue; }
    public double getPopulationOr(double defaultValue) { return population != null ? population.doubleValue() : defaultValue; }
    public double getFoodResourceOr(double defaultValue) { return foodResource != null ? foodResource : defaultValue; }
    public double getWaterResourceOr(double defaultValue) { return waterResource != null ? waterResource : defaultValue; }
    public double getLatitudeOr(double defaultValue) { return latitude != null ? latitude : defaultValue; }
    public double getLongitudeOr(double defaultValue) { return longitude != null ? longitude : defaultValue; }
    public double getWoodResourceOr(double defaultValue) { return woodResource != null ? woodResource : defaultValue; }
    public double getBiomassHumanOr(double defaultValue) { return biomassHuman != null ? biomassHuman : defaultValue; }
    public double getBiomassLivestockOr(double defaultValue) { return biomassLivestock != null ? biomassLivestock : defaultValue; }
    public double getBiomassFishOr(double defaultValue) { return biomassFish != null ? biomassFish : defaultValue; }
    public double getBiomassAgricultureOr(double defaultValue) { return biomassAgriculture != null ? biomassAgriculture : defaultValue; }
    public double getBiomassNaturalOr(double defaultValue) { return biomassNatural != null ? biomassNatural : defaultValue; }
    public double getEnergyWindOr(double defaultValue) { return energyWind != null ? energyWind : defaultValue; }
    public double getEnergySolarOr(double defaultValue) { return energySolar != null ? energySolar : defaultValue; }
    public double getEnergyFireOr(double defaultValue) { return energyFire != null ? energyFire : defaultValue; }
    public double getEnergySlavesOr(double defaultValue) { return energySlaves != null ? energySlaves : defaultValue; }
    public double getEnergyFoodConsumedOr(double defaultValue) { return energyFoodConsumed != null ? energyFoodConsumed : defaultValue; }
    public double getResourceMetalOr(double defaultValue) { return resourceMetal != null ? resourceMetal : defaultValue; }
    public double getResourcePreciousMetalOr(double defaultValue) { return resourcePreciousMetal != null ? resourcePreciousMetal : defaultValue; }
    public double getResourceClayOr(double defaultValue) { return resourceClay != null ? resourceClay : defaultValue; }
    public double getResourceWorkOr(double defaultValue) { return resourceWork != null ? resourceWork : defaultValue; }
    public double getResourceCapitalOr(double defaultValue) { return resourceCapital != null ? resourceCapital : defaultValue; }
    public double getSoilOrganicCarbonOr(double defaultValue) { return soilOrganicCarbon != null ? soilOrganicCarbon : defaultValue; }
    public double getSoilSalinityOr(double defaultValue) { return soilSalinity != null ? soilSalinity : defaultValue; }
    public double getResourceCoalOr(double defaultValue) { return resourceCoal != null ? resourceCoal : defaultValue; }
    public double getResourceOilOr(double defaultValue) { return resourceOil != null ? resourceOil : defaultValue; }
    public double getResourceGasOr(double defaultValue) { return resourceGas != null ? resourceGas : defaultValue; }
    public double getResourceUraniumOr(double defaultValue) { return resourceUranium != null ? resourceUranium : defaultValue; }
    public double getResourceRareEarthsOr(double defaultValue) { return resourceRareEarths != null ? resourceRareEarths : defaultValue; }
    public double getMantleHeatFlowOr(double defaultValue) { return mantleHeatFlow != null ? mantleHeatFlow : defaultValue; }
    public double getFreshwaterAquiferOr(double defaultValue) { return freshwaterAquifer != null ? freshwaterAquifer : defaultValue; }
    public double getAccessibleAquiferOr(double defaultValue) { return accessibleAquifer != null ? accessibleAquifer : defaultValue; }
    public double getPollutionLevelOr(double defaultValue) { return pollutionLevel != null ? pollutionLevel : defaultValue; }
    public double getLifespanOr(double defaultValue) { return lifespan != null ? lifespan : defaultValue; }
    public double getFertilityOr(double defaultValue) { return fertility != null ? fertility : defaultValue; }
    public double getGiniIndexOr(double defaultValue) { return giniIndex != null ? giniIndex : defaultValue; }
    public double getTechnologyLevelOr(double defaultValue) { return technologyLevel != null ? technologyLevel : defaultValue; }
    public double getMovementFrictionOr(double defaultValue) { return movementFriction != null ? movementFriction : defaultValue; }
    public double getChokePointScoreOr(double defaultValue) { return chokePointScore != null ? chokePointScore : defaultValue; }
    public double getTransitTollRevenueOr(double defaultValue) { return transitTollRevenue != null ? transitTollRevenue : defaultValue; }
    public double getDynamicAlbedoOr(double defaultValue) { return dynamicAlbedo != null ? dynamicAlbedo : defaultValue; }
    public double getIceSheetThicknessMetersOr(double defaultValue) { return iceSheetThicknessMeters != null ? iceSheetThicknessMeters : defaultValue; }
    public double getSeaLevelOffsetMetersOr(double defaultValue) { return seaLevelOffsetMeters != null ? seaLevelOffsetMeters : defaultValue; }
    public double getCoastalMarineResourceOr(double defaultValue) { return coastalMarineResource != null ? coastalMarineResource : defaultValue; }
    public double getHabitatCapacityOr(double defaultValue) { return habitatCapacity != null ? habitatCapacity : defaultValue; }
    public double getHabitatIntegrityOr(double defaultValue) { return habitatIntegrity != null ? habitatIntegrity : defaultValue; }
    public double getHabitatEnergyKwOr(double defaultValue) { return habitatEnergyKw != null ? habitatEnergyKw : defaultValue; }
    public double getHabitatAgeYearsOr(double defaultValue) { return habitatAgeYears != null ? habitatAgeYears : defaultValue; }
    public double getLinguisticDriftOr(double defaultValue) { return linguisticDrift != null ? linguisticDrift : defaultValue; }
    public int getHomininSpeciesOr(int defaultValue) { return homininSpecies != null ? homininSpecies : defaultValue; }
    public int getHabitatTierOr(int defaultValue) { return habitatTier != null ? habitatTier : defaultValue; }
    public int getPop0to4Or(int defaultValue) { return pop0to4 != null ? pop0to4 : defaultValue; }
    public int getPop5to14Or(int defaultValue) { return pop5to14 != null ? pop5to14 : defaultValue; }
    public int getPop15to24Or(int defaultValue) { return pop15to24 != null ? pop15to24 : defaultValue; }
    public int getPop25to49Or(int defaultValue) { return pop25to49 != null ? pop25to49 : defaultValue; }
    public int getPop50to64Or(int defaultValue) { return pop50to64 != null ? pop50to64 : defaultValue; }
    public int getPop65to79Or(int defaultValue) { return pop65to79 != null ? pop65to79 : defaultValue; }
    public int getPop80PlusOr(int defaultValue) { return pop80Plus != null ? pop80Plus : defaultValue; }
    public int getPopYouthOr(int defaultValue) { return popYouth != null ? popYouth : defaultValue; }
    public int getPopAdultOr(int defaultValue) { return popAdult != null ? popAdult : defaultValue; }
    public int getPopElderlyOr(int defaultValue) { return popElderly != null ? popElderly : defaultValue; }
    public int getEpidemicInfectedOr(int defaultValue) { return epidemicInfected != null ? epidemicInfected : defaultValue; }
    public int getEpidemicRecoveredOr(int defaultValue) { return epidemicRecovered != null ? epidemicRecovered : defaultValue; }
}
