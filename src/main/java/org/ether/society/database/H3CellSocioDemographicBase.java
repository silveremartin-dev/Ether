/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.database;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import org.ether.society.model.HabitatType;

/**
 * Mapped JPA superclass encapsulating cryospheric surface dynamics, coastal engineering flags,
 * extraterrestrial pressurized habitat infrastructure, 7-cohort demographic age pyramids,
 * epidemiological SEIR compartments, and linguistic diffusion state for {@link H3Cell}.
 *
 * <p>All persistent fields in this {@link MappedSuperclass} map directly onto the
 * {@code h3_cells_l8} relational table while keeping {@link H3Cell} concise and modular.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see H3Cell
 */
@MappedSuperclass
/**
 * H3CellSocioDemographicBase.
 * <p>
 * Component for the Ether planetary simulation platform.
 * Adheres to the zero-defect code excellence standards and 500-line limit.
 * </p>
 *
 * @author Ether Architectural Working Group
 * @see org.ether.society.core.engine.SimulationEngineModule
 */
public abstract class H3CellSocioDemographicBase {

    // --- Dynamic Climate, Cryosphere & Coastal Surface Physics ---
    /** Dynamic surface broadband albedo $\alpha \in [0.06, 0.85]$ (dimensionless). */
    @Column(nullable = false) private Double dynamicAlbedo = 0.30;
    /** Continental or marine ice sheet thickness in meters ($\text{m}$). */
    @Column(nullable = false) private Double iceSheetThicknessMeters = 0.0;
    /** Glacial isostatic crustal deflection in meters ($\text{m}$). */
    @Column(nullable = false) private Double crustalDeflectionMeters = 0.0;
    /** Eustatic and regional sea-level offset in meters ($\text{m}$). */
    @Column(nullable = false) private Double seaLevelOffsetMeters = 0.0;
    /** Whether this hexagon borders a marine coastline. */
    @Column(nullable = false) private Boolean isCoastal = false;
    /** Intertidal and shelf marine carrying capacity resource ($\text{GJ}$). */
    @Column(nullable = false) private Double coastalMarineResource = 0.0;
    /** Whether coastal land reclamation (polderization) is active. */
    @Column(nullable = false) private Boolean isPolder = false;
    /** Whether major hydraulic dam/reservoir infrastructure is installed. */
    @Column(nullable = false) private Boolean hasDam = false;
    /** Whether floating seasteading / aerostat infrastructure is deployed. */
    @Column(nullable = false) private Boolean hasFloatingInfrastructure = false;

    // --- Extraterrestrial Pressurized Habitats & Infrastructure ---
    /** Pressurized habitat architecture classification for hostile planetary environments. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HabitatType habitatType = HabitatType.NONE;
    /** Engineering technological tier of the pressurized habitat ($0\dots 5$). */
    @Column(nullable = false) private Integer habitatTier = 0;
    /** Maximum life-support population capacity of the habitat (inhabitants). */
    @Column(nullable = false) private Double habitatCapacity = 0.0;
    /** Structural pressure-vessel integrity fraction $\in [0.0, 1.0]$. */
    @Column(nullable = false) private Double habitatIntegrity = 1.0;
    /** Continuous life-support electrical/thermal power draw in kilowatts ($\text{kW}$). */
    @Column(nullable = false) private Double habitatEnergyKw = 0.0;
    /** Cumulative operational age of habitat structure in Julian years. */
    @Column(nullable = false) private Double habitatAgeYears = 0.0;

    // --- Detailed Demographic Age Pyramid (7 Fine-Grained Cohorts) ---
    /** Infant and early-childhood population cohort (ages 0 to 4). */
    @Column(nullable = false) protected Integer pop0to4 = 0;
    /** Juvenile population cohort (ages 5 to 14). */
    @Column(nullable = false) protected Integer pop5to14 = 0;
    /** Young adult population cohort (ages 15 to 24). */
    @Column(nullable = false) protected Integer pop15to24 = 0;
    /** Prime working-age adult population cohort (ages 25 to 49). */
    @Column(nullable = false) protected Integer pop25to49 = 0;
    /** Mature working-age adult population cohort (ages 50 to 64). */
    @Column(nullable = false) protected Integer pop50to64 = 0;
    /** Senior population cohort (ages 65 to 79). */
    @Column(nullable = false) protected Integer pop65to79 = 0;
    /** Octogenarian and older population cohort (ages 80+). */
    @Column(nullable = false) protected Integer pop80Plus = 0;

    // --- Demographic Cohort Aggregations ---
    /** Aggregated youth cohort count (ages 0 to 14). */
    @Column(nullable = false) protected Integer popYouth = 0;
    /** Aggregated working-age adult cohort count (ages 15 to 64). */
    @Column(nullable = false) protected Integer popAdult = 0;
    /** Aggregated elderly cohort count (ages 65+). */
    @Column(nullable = false) protected Integer popElderly = 0;

    // --- Epidemiological SEIR Model State ---
    /** Actively infected headcount in the local SEIR compartment ($I$). */
    @Column(nullable = false) private Integer epidemicInfected = 0;
    /** Recovered/immune headcount in the local SEIR compartment ($R$). */
    @Column(nullable = false) private Integer epidemicRecovered = 0;
    /** Identifier of the dominant circulating pathogen strain, or {@code null} if none. */
    @Column(length = 50) private String activePathogenName = null;

    // --- Linguistic & Cultural Diffusion ---
    /** Macro-family linguistic clade or isogloss group identifier. */
    @Column(length = 50) private String languageGroup = "Proto-Human";
    /** Cumulative phonological/lexical drift index relative to ancestral root. */
    @Column(nullable = false) private Double linguisticDrift = 0.0;

    /**
     * Hook invoked when age-cohort subdivisions are lazily queried after a total population update.
     */
    protected abstract void ensureAgePyramidInitialized();

    /**
     * Gets the 7-segment demographic age pyramid cohorts as an array:
     * {@code [0..4, 5..14, 15..24, 25..49, 50..64, 65..79, 80+]}.
     *
     * @return 7-element array containing population counts for each age cohort
     */
    public int[] getAgePyramid() {
        if (pop0to4 == null) {
            ensureAgePyramidInitialized();
        }
        return new int[] {
            getPop0to4Or(0), getPop5to14Or(0), getPop15to24Or(0),
            getPop25to49Or(0), getPop50to64Or(0), getPop65to79Or(0), getPop80PlusOr(0)
        };
    }

    /** @return youth population count (ages 0â€“14) */
    public Integer getPopYouth() {
        if (popYouth == null) ensureAgePyramidInitialized();
        return popYouth;
    }
    /** @param popYouth youth population count (ages 0â€“14) */
    public void setPopYouth(Integer popYouth) { this.popYouth = popYouth; }

    /** @return adult population count (ages 15â€“64) */
    public Integer getPopAdult() {
        if (popAdult == null) ensureAgePyramidInitialized();
        return popAdult;
    }
    /** @param popAdult adult population count (ages 15â€“64) */
    public void setPopAdult(Integer popAdult) { this.popAdult = popAdult; }

    /** @return elderly population count (ages 65+) */
    public Integer getPopElderly() {
        if (popElderly == null) ensureAgePyramidInitialized();
        return popElderly;
    }
    /** @param popElderly elderly population count (ages 65+) */
    public void setPopElderly(Integer popElderly) { this.popElderly = popElderly; }

    /** @return dynamic surface albedo $\alpha \in [0.06, 0.85]$ */
    public Double getDynamicAlbedo() { return dynamicAlbedo; }
    /** @param dynamicAlbedo dynamic surface albedo */
    public void setDynamicAlbedo(Double dynamicAlbedo) { this.dynamicAlbedo = dynamicAlbedo; }

    /** @return cohort 0â€“4 headcount */
    public Integer getPop0to4() { return pop0to4; }
    /** @param val cohort 0â€“4 headcount */
    public void setPop0to4(Integer val) { this.pop0to4 = val; }

    /** @return cohort 5â€“14 headcount */
    public Integer getPop5to14() { return pop5to14; }
    /** @param val cohort 5â€“14 headcount */
    public void setPop5to14(Integer val) { this.pop5to14 = val; }

    /** @return cohort 15â€“24 headcount */
    public Integer getPop15to24() { return pop15to24; }
    /** @param val cohort 15â€“24 headcount */
    public void setPop15to24(Integer val) { this.pop15to24 = val; }

    /** @return cohort 25â€“49 headcount */
    public Integer getPop25to49() { return pop25to49; }
    /** @param val cohort 25â€“49 headcount */
    public void setPop25to49(Integer val) { this.pop25to49 = val; }

    /** @return cohort 50â€“64 headcount */
    public Integer getPop50to64() { return pop50to64; }
    /** @param val cohort 50â€“64 headcount */
    public void setPop50to64(Integer val) { this.pop50to64 = val; }

    /** @return cohort 65â€“79 headcount */
    public Integer getPop65to79() { return pop65to79; }
    /** @param val cohort 65â€“79 headcount */
    public void setPop65to79(Integer val) { this.pop65to79 = val; }

    /** @return cohort 80+ headcount */
    public Integer getPop80Plus() { return pop80Plus; }
    /** @param val cohort 80+ headcount */
    public void setPop80Plus(Integer val) { this.pop80Plus = val; }

    /** @return SEIR infected headcount */
    public Integer getEpidemicInfected() { return epidemicInfected; }
    /** @param val SEIR infected headcount */
    public void setEpidemicInfected(Integer val) { this.epidemicInfected = val; }

    /** @return SEIR recovered headcount */
    public Integer getEpidemicRecovered() { return epidemicRecovered; }
    /** @param val SEIR recovered headcount */
    public void setEpidemicRecovered(Integer val) { this.epidemicRecovered = val; }

    /** @return active pathogen name or {@code null} */
    public String getActivePathogenName() { return activePathogenName; }
    /** @param name active pathogen name */
    public void setActivePathogenName(String name) { this.activePathogenName = name; }

    /** @return linguistic macro-group name */
    public String getLanguageGroup() { return languageGroup; }
    /** @param lang linguistic macro-group name */
    public void setLanguageGroup(String lang) { this.languageGroup = lang; }

    /** @return cumulative linguistic drift index */
    public Double getLinguisticDrift() { return linguisticDrift; }
    /** @param drift cumulative linguistic drift index */
    public void setLinguisticDrift(Double drift) { this.linguisticDrift = drift; }

    /** @return ice sheet thickness in meters */
    public Double getIceSheetThicknessMeters() { return iceSheetThicknessMeters != null ? iceSheetThicknessMeters : 0.0; }
    /** @param iceSheetThicknessMeters ice sheet thickness in meters */
    public void setIceSheetThicknessMeters(Double iceSheetThicknessMeters) { this.iceSheetThicknessMeters = iceSheetThicknessMeters; }

    /** @return crustal isostatic deflection in meters */
    public Double getCrustalDeflectionMeters() { return crustalDeflectionMeters != null ? crustalDeflectionMeters : 0.0; }
    /** @param crustalDeflectionMeters crustal deflection in meters */
    public void setCrustalDeflectionMeters(Double crustalDeflectionMeters) { this.crustalDeflectionMeters = crustalDeflectionMeters; }

    /** @return sea level offset in meters */
    public Double getSeaLevelOffsetMeters() { return seaLevelOffsetMeters != null ? seaLevelOffsetMeters : 0.0; }
    /** @param seaLevelOffsetMeters sea level offset in meters */
    public void setSeaLevelOffsetMeters(Double seaLevelOffsetMeters) { this.seaLevelOffsetMeters = seaLevelOffsetMeters; }

    /** @return {@code true} if cell is coastal */
    public Boolean getIsCoastal() { return isCoastal != null ? isCoastal : false; }
    /** @param isCoastal whether cell is coastal */
    public void setIsCoastal(Boolean isCoastal) { this.isCoastal = isCoastal; }

    /** @return coastal marine resource stock ($\text{GJ}$) */
    public Double getCoastalMarineResource() { return coastalMarineResource != null ? coastalMarineResource : 0.0; }
    /** @param coastalMarineResource coastal marine resource stock */
    public void setCoastalMarineResource(Double coastalMarineResource) { this.coastalMarineResource = coastalMarineResource; }

    /** @return {@code true} if reclaimed polder */
    public Boolean getIsPolder() { return isPolder != null ? isPolder : false; }
    /** @param isPolder whether cell is reclaimed polder */
    public void setIsPolder(Boolean isPolder) { this.isPolder = isPolder; }

    /** @return {@code true} if hydraulic dam is present */
    public Boolean getHasDam() { return hasDam != null ? hasDam : false; }
    /** @param hasDam whether hydraulic dam is present */
    public void setHasDam(Boolean hasDam) { this.hasDam = hasDam; }

    /** @return {@code true} if floating infrastructure is present */
    public Boolean getHasFloatingInfrastructure() { return hasFloatingInfrastructure != null ? hasFloatingInfrastructure : false; }
    /** @param hasFloatingInfrastructure whether floating infrastructure is present */
    public void setHasFloatingInfrastructure(Boolean hasFloatingInfrastructure) { this.hasFloatingInfrastructure = hasFloatingInfrastructure; }

    /** @return pressurized habitat type */
    public HabitatType getHabitatType() { return habitatType != null ? habitatType : HabitatType.NONE; }
    /** @param habitatType pressurized habitat type */
    public void setHabitatType(HabitatType habitatType) { this.habitatType = habitatType != null ? habitatType : HabitatType.NONE; }

    /** @return habitat engineering tier */
    public Integer getHabitatTier() { return habitatTier != null ? habitatTier : 0; }
    /** @param habitatTier habitat engineering tier */
    public void setHabitatTier(Integer habitatTier) { this.habitatTier = habitatTier != null ? habitatTier : 0; }

    /** @return habitat population capacity */
    public Double getHabitatCapacity() { return habitatCapacity != null ? habitatCapacity : 0.0; }
    /** @param habitatCapacity habitat population capacity */
    public void setHabitatCapacity(Double habitatCapacity) { this.habitatCapacity = habitatCapacity != null ? Math.max(0.0, habitatCapacity) : 0.0; }

    /** @return habitat structural integrity $\in [0.0, 1.0]$ */
    public Double getHabitatIntegrity() { return habitatIntegrity != null ? habitatIntegrity : 1.0; }
    /** @param habitatIntegrity habitat structural integrity */
    public void setHabitatIntegrity(Double habitatIntegrity) { this.habitatIntegrity = habitatIntegrity != null ? Math.clamp(habitatIntegrity, 0.0, 1.0) : 1.0; }

    /** @return habitat power consumption in $\text{kW}$ */
    public Double getHabitatEnergyKw() { return habitatEnergyKw != null ? habitatEnergyKw : 0.0; }
    /** @param habitatEnergyKw habitat power consumption in $\text{kW}$ */
    public void setHabitatEnergyKw(Double habitatEnergyKw) { this.habitatEnergyKw = habitatEnergyKw != null ? Math.max(0.0, habitatEnergyKw) : 0.0; }

    /** @return habitat age in Julian years */
    public Double getHabitatAgeYears() { return habitatAgeYears != null ? habitatAgeYears : 0.0; }
    /** @param habitatAgeYears habitat age in Julian years */
    public void setHabitatAgeYears(Double habitatAgeYears) { this.habitatAgeYears = habitatAgeYears != null ? Math.max(0.0, habitatAgeYears) : 0.0; }

    // --- Primitive-Safe Unboxing Accessors for Socio-Demographic & Surface Substate ---
    /** @param defaultValue fallback albedo @return unboxed albedo */
    public double getDynamicAlbedoOr(double defaultValue) { return dynamicAlbedo != null ? dynamicAlbedo : defaultValue; }
    /** @param defaultValue fallback thickness @return unboxed ice sheet thickness ($\text{m}$) */
    public double getIceSheetThicknessMetersOr(double defaultValue) { return iceSheetThicknessMeters != null ? iceSheetThicknessMeters : defaultValue; }
    /** @param defaultValue fallback deflection @return unboxed crustal deflection ($\text{m}$) */
    public double getCrustalDeflectionMetersOr(double defaultValue) { return crustalDeflectionMeters != null ? crustalDeflectionMeters : defaultValue; }
    /** @param defaultValue fallback offset @return unboxed sea level offset ($\text{m}$) */
    public double getSeaLevelOffsetMetersOr(double defaultValue) { return seaLevelOffsetMeters != null ? seaLevelOffsetMeters : defaultValue; }
    /** @param defaultValue fallback marine resource @return unboxed coastal marine resource */
    public double getCoastalMarineResourceOr(double defaultValue) { return coastalMarineResource != null ? coastalMarineResource : defaultValue; }
    /** @param defaultValue fallback capacity @return unboxed habitat capacity */
    public double getHabitatCapacityOr(double defaultValue) { return habitatCapacity != null ? habitatCapacity : defaultValue; }
    /** @param defaultValue fallback integrity @return unboxed habitat integrity */
    public double getHabitatIntegrityOr(double defaultValue) { return habitatIntegrity != null ? habitatIntegrity : defaultValue; }
    /** @param defaultValue fallback power @return unboxed habitat energy ($\text{kW}$) */
    public double getHabitatEnergyKwOr(double defaultValue) { return habitatEnergyKw != null ? habitatEnergyKw : defaultValue; }
    /** @param defaultValue fallback age @return unboxed habitat age (years) */
    public double getHabitatAgeYearsOr(double defaultValue) { return habitatAgeYears != null ? habitatAgeYears : defaultValue; }
    /** @param defaultValue fallback drift @return unboxed linguistic drift */
    public double getLinguisticDriftOr(double defaultValue) { return linguisticDrift != null ? linguisticDrift : defaultValue; }
    /** @param defaultValue fallback tier @return unboxed habitat tier */
    public int getHabitatTierOr(int defaultValue) { return habitatTier != null ? habitatTier : defaultValue; }
    /** @param defaultValue fallback count @return unboxed cohort 0â€“4 count */
    public int getPop0to4Or(int defaultValue) { return pop0to4 != null ? pop0to4 : defaultValue; }
    /** @param defaultValue fallback count @return unboxed cohort 5â€“14 count */
    public int getPop5to14Or(int defaultValue) { return pop5to14 != null ? pop5to14 : defaultValue; }
    /** @param defaultValue fallback count @return unboxed cohort 15â€“24 count */
    public int getPop15to24Or(int defaultValue) { return pop15to24 != null ? pop15to24 : defaultValue; }
    /** @param defaultValue fallback count @return unboxed cohort 25â€“49 count */
    public int getPop25to49Or(int defaultValue) { return pop25to49 != null ? pop25to49 : defaultValue; }
    /** @param defaultValue fallback count @return unboxed cohort 50â€“64 count */
    public int getPop50to64Or(int defaultValue) { return pop50to64 != null ? pop50to64 : defaultValue; }
    /** @param defaultValue fallback count @return unboxed cohort 65â€“79 count */
    public int getPop65to79Or(int defaultValue) { return pop65to79 != null ? pop65to79 : defaultValue; }
    /** @param defaultValue fallback count @return unboxed cohort 80+ count */
    public int getPop80PlusOr(int defaultValue) { return pop80Plus != null ? pop80Plus : defaultValue; }
    /** @param defaultValue fallback count @return unboxed youth population count */
    public int getPopYouthOr(int defaultValue) { return popYouth != null ? popYouth : defaultValue; }
    /** @param defaultValue fallback count @return unboxed adult population count */
    public int getPopAdultOr(int defaultValue) { return popAdult != null ? popAdult : defaultValue; }
    /** @param defaultValue fallback count @return unboxed elderly population count */
    public int getPopElderlyOr(int defaultValue) { return popElderly != null ? popElderly : defaultValue; }
    /** @param defaultValue fallback count @return unboxed infected count */
    public int getEpidemicInfectedOr(int defaultValue) { return epidemicInfected != null ? epidemicInfected : defaultValue; }
    /** @param defaultValue fallback count @return unboxed recovered count */
    public int getEpidemicRecoveredOr(int defaultValue) { return epidemicRecovered != null ? epidemicRecovered : defaultValue; }
}
