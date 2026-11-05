/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;

import java.util.List;

/**
 * <h1>Scenario</h1>
 * <p>
 * Core operational model for an Ether planetary and civilizational simulation configuration.<br>
 * Encapsulates planetary boundary conditions, demographic initial stocks ($N_0, K_0, E_0, F_0, I_0$),
 * H3 spatial resolution, temporal step size ($\Delta t$), cultural and geological tensor configurations,
 * Tier 2 cliodynamic engine toggles, and determinism settings.
 * </p>
 * <p>
 * Canonical built-in historical, future, and exoplanetary scenarios are loaded directly from
 * {@code data/presets/scenarios/*.json} via {@link org.ether.society.persistence.PresetStorageService}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
@SuppressWarnings("serial")
public class Scenario extends AbstractScenarioTensorAndEngineConfig {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private Long id;
    private String presetKey;
    private String referenceTrajectoryModel;

    // Planet Configuration
    private PlanetPreset planetPreset = PlanetPreset.EARTH_LIKE;
    private EcologyPreset ecologyPreset = EcologyPreset.EARTH_STANDARD;
    private String ecologyPresetName = "Earth Standard Baseline";
    private boolean useRealEarthData;

    // Planet Physics
    private double planetRadiusKm;
    private double rotationPeriodHours;
    private double revolutionPeriodDays;
    private double axialTiltDegrees;

    /**
     * Technological stock preset defining initial per-capita capital ($K_0$), energy ($E_0$),
     * food reserves ($F_0$), and archived information ($I_0$).
     */
    public enum TechPreset {
        AUTO_FROM_YEAR("Auto (Calculated from T0 Year)", -1, -1, -1, -1),
        PALEOLITHIC("Hunter-Gatherers / Paleolithic", 5.0, 10.0, 2.0, 5.0),
        NEOLITHIC_BRONZE("Bronze Age and City-States", 25.0, 30.0, 4.0, 40.0),
        ANTIQUITY("Classical Antiquity and Empire", 100.0, 60.0, 6.0, 200.0),
        RENAISSANCE("Renaissance and Printing Press", 500.0, 300.0, 8.0, 2000.0),
        INDUSTRIAL("Industrial Revolution and Steam", 2500.0, 2500.0, 10.0, 15000.0),
        CONTEMPORARY("Contemporary and Digital Metropolis", 15000.0, 50000.0, 18.0, 5000000.0),
        SPACE_COLONY_MARS("Space Colony / Mars (Low Pop / Ultra High-Tech)", 50000.0, 200000.0, 24.0, 50000000.0),
        CUSTOM("Custom (Manual Stock Inputs)", -1, -1, -1, -1);

        private final String label;
        private final double capital;
        private final double energy;
        private final double foodMonths;
        private final double info;

        TechPreset(String label, double capital, double energy, double foodMonths, double info) {
            this.label = label;
            this.capital = capital;
            this.energy = energy;
            this.foodMonths = foodMonths;
            this.info = info;
        }

        /** @return display label for the tech preset */
        public String getLabel() { return label; }
        /** @return initial physical capital $K_0$ in $\text{kg/capita}$ */
        public double getCapital() { return capital; }
        /** @return initial stored energy $E_0$ in $\text{MJ/capita}$ */
        public double getEnergy() { return energy; }
        /** @return initial food reserves $F_0$ in months of consumption */
        public double getFoodMonths() { return foodMonths; }
        /** @return initial archived information $I_0$ in $\text{bits/capita}$ */
        public double getInfo() { return info; }

        @Override
        public String toString() { return label; }
    }

    private TechPreset techPreset = TechPreset.AUTO_FROM_YEAR;

    // Human Start Conditions
    private long initialHumanCount;
    private double initialTechLevel;
    private double initialCapitalPerCapita = 10.0;
    private double initialEnergyPerCapita = 50.0;
    private double initialFoodReserveMonths = 6.0;
    private double initialInformationPerCapita = 100.0;
    private String populationDensityType;

    // Simulation Parameters
    private double cellSizeKm2;
    private int targetCohortSize = 150;
    private double temporalResolutionDays = 30.0;
    private double climateHarshness;
    private long startDateYear;
    private long endDateYear = 2100;
    private long seed = 12345L;
    private long culturalSeed = 54321L;
    private boolean randomEventsEnabled = true;
    private boolean earthHistoricalLeadersEnabled = true;
    private boolean proceduralLeadersEnabled = true;
    private String customDensityBase64;

    /**
     * Constructs a default Scenario initialized with Earth baseline parameters at $-100\,000\,\text{BP}$.
     */
    public Scenario() {
        this.name = "New Scenario";
        this.planetPreset = PlanetPreset.EARTH_LIKE;
        this.ecologyPreset = EcologyPreset.EARTH_STANDARD;
        this.ecologyPresetName = EcologyPreset.EARTH_STANDARD.name();
        this.planetRadiusKm = 6371.0;
        this.rotationPeriodHours = 24.0;
        this.revolutionPeriodDays = 365.25;
        this.axialTiltDegrees = 23.5;

        this.initialHumanCount = 1000;
        this.initialTechLevel = 0.0;
        this.populationDensityType = "SPARSE";

        this.cellSizeKm2 = 100.0;
        this.climateHarshness = 0.5;
        this.startDateYear = -100000;
        this.useRealEarthData = false;
        this.seed = 12345L;
        this.randomEventsEnabled = true;
    }

    public static Scenario createDefaultScenario() {
        return new Scenario();
    }

    public static Scenario createBenchmarkStandardScenario() {
        return ScenarioOperationsHelper.createBenchmarkStandardScenario();
    }

    public static List<Scenario> getBuiltInScenarios() {
        return FutureAndExoplanetScenarioCatalog.getBuiltInScenarios();
    }

    public String resolvePresetKey() {
        if (presetKey != null && !presetKey.isBlank()) return presetKey.trim();
        if (name != null && !name.isBlank()) return org.ether.society.persistence.PresetStorageService.slugify(name);
        return "unnamed_scenario";
    }

    /**
     * Returns the canonical reference trajectory model key ({@code "PALEO_SOURCES"},
     * {@code "HYDE_MADDISON"}, {@code "SSP1"}..{@code "SSP5"}, {@code "ETHER_CALIBRATED"}).
     *
     * @return the reference model identifier
     */
    public String getReferenceTrajectoryModel() {
        if (referenceTrajectoryModel != null && !referenceTrajectoryModel.isBlank()) {
            return referenceTrajectoryModel;
        }
        if (startDateYear < -10000) return "PALEO_SOURCES";
        if (startDateYear < 2025) return "HYDE_MADDISON";
        return "SSP2";
    }

    public void setReferenceTrajectoryModel(String referenceTrajectoryModel) {
        this.referenceTrajectoryModel = referenceTrajectoryModel;
    }

    String rawReferenceTrajectoryModel() {
        return referenceTrajectoryModel;
    }

    String rawDescription() {
        return description;
    }

    public String getDisplayName() {
        String key = resolvePresetKey();
        if (key != null && !key.isBlank()) {
            String localized = I18n.getOrDefault("scenario.preset." + key + ".name", name);
            if (localized != null && !localized.isBlank()) return localized;
        }
        return name != null ? name : "Unnamed Scenario";
    }

    public String getDisplayDescription() {
        String key = resolvePresetKey();
        if (key != null && !key.isBlank()) {
            String localized = I18n.getOrDefault("scenario.preset." + key + ".desc", description);
            if (localized != null && !localized.isBlank()) return localized;
        }
        return description != null ? description : "";
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public Scenario copy() {
        return ScenarioOperationsHelper.copy(this);
    }

    /**
     * Calculates the default number of simulation ticks required to run this scenario:
     * $$N_{\text{ticks}} = \text{clamp}\left(\Delta Y \cdot \frac{365.25}{\Delta t_{\text{days}}}, 100, 10^6\right)$$
     *
     * @return clamped total tick count
     */
    public int calculateScenarioTicks() {
        long durationYears = Math.max(1, endDateYear - startDateYear);
        double ticksPerYear = 365.25 / Math.max(1.0, temporalResolutionDays);
        return (int) Math.clamp((long) (durationYears * ticksPerYear), 100L, 1000000L);
    }

    public String getDescription() {
        if (description == null || description.isBlank()) {
            return ScenarioOperationsHelper.formatDefaultDescription(this);
        }
        return description;
    }

    public String getPresetKey() { return presetKey; }
    public void setPresetKey(String presetKey) { this.presetKey = presetKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void setDescription(String description) { this.description = description; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlanetPreset getPlanetPreset() { return planetPreset; }
    public void setPlanetPreset(PlanetPreset planetPreset) { this.planetPreset = planetPreset; }

    public EcologyPreset getEcologyPreset() { return ecologyPreset; }
    public void setEcologyPreset(EcologyPreset ecologyPreset) { this.ecologyPreset = ecologyPreset; }

    public String getEcologyPresetName() { return ecologyPresetName; }
    public void setEcologyPresetName(String ecologyPresetName) { this.ecologyPresetName = ecologyPresetName; }

    public boolean isUseRealEarthData() { return useRealEarthData; }
    public void setUseRealEarthData(boolean useRealEarthData) { this.useRealEarthData = useRealEarthData; }

    public double getPlanetRadiusKm() { return planetRadiusKm; }
    public void setPlanetRadiusKm(double planetRadiusKm) { this.planetRadiusKm = planetRadiusKm; }

    public double getRotationPeriodHours() { return rotationPeriodHours; }
    public void setRotationPeriodHours(double rotationPeriodHours) { this.rotationPeriodHours = rotationPeriodHours; }

    public double getRevolutionPeriodDays() { return revolutionPeriodDays; }
    public void setRevolutionPeriodDays(double revolutionPeriodDays) { this.revolutionPeriodDays = revolutionPeriodDays; }

    public double getAxialTiltDegrees() { return axialTiltDegrees; }
    public void setAxialTiltDegrees(double axialTiltDegrees) { this.axialTiltDegrees = axialTiltDegrees; }

    public long getInitialHumanCount() { return initialHumanCount; }
    public void setInitialHumanCount(long initialHumanCount) { this.initialHumanCount = initialHumanCount; }

    public TechPreset getTechPreset() { return techPreset != null ? techPreset : TechPreset.AUTO_FROM_YEAR; }
    public void setTechPreset(TechPreset techPreset) { this.techPreset = techPreset != null ? techPreset : TechPreset.AUTO_FROM_YEAR; }

    public double getInitialTechLevel() { return initialTechLevel; }
    public void setInitialTechLevel(double initialTechLevel) { this.initialTechLevel = initialTechLevel; }

    public double getInitialCapitalPerCapita() { return initialCapitalPerCapita; }
    public void setInitialCapitalPerCapita(double initialCapitalPerCapita) { this.initialCapitalPerCapita = initialCapitalPerCapita; }

    public double getInitialEnergyPerCapita() { return initialEnergyPerCapita; }
    public void setInitialEnergyPerCapita(double initialEnergyPerCapita) { this.initialEnergyPerCapita = initialEnergyPerCapita; }

    public double getInitialFoodReserveMonths() { return initialFoodReserveMonths; }
    public void setInitialFoodReserveMonths(double initialFoodReserveMonths) { this.initialFoodReserveMonths = initialFoodReserveMonths; }

    public double getInitialInformationPerCapita() { return initialInformationPerCapita; }
    public void setInitialInformationPerCapita(double initialInformationPerCapita) { this.initialInformationPerCapita = initialInformationPerCapita; }

    public String getPopulationDensityType() { return populationDensityType; }
    public void setPopulationDensityType(String populationDensityType) { this.populationDensityType = populationDensityType; }

    public double getCellSizeKm2() { return cellSizeKm2; }
    public void setCellSizeKm2(double cellSizeKm2) { this.cellSizeKm2 = cellSizeKm2; }

    public int getTargetCohortSize() { return targetCohortSize; }
    public void setTargetCohortSize(int targetCohortSize) { this.targetCohortSize = targetCohortSize; }

    public double getTemporalResolutionDays() { return temporalResolutionDays; }
    public void setTemporalResolutionDays(double temporalResolutionDays) { this.temporalResolutionDays = temporalResolutionDays; }

    public double getClimateHarshness() { return climateHarshness; }
    public void setClimateHarshness(double climateHarshness) { this.climateHarshness = climateHarshness; }

    public long getStartDateYear() { return startDateYear; }
    public void setStartDateYear(long startDateYear) {
        this.startDateYear = startDateYear;
        if (this.endDateYear <= startDateYear) {
            this.endDateYear = startDateYear + 50;
        }
    }

    public long getEndDateYear() { return endDateYear; }
    public void setEndDateYear(long endDateYear) { this.endDateYear = endDateYear; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public long getCulturalSeed() { return culturalSeed; }
    public void setCulturalSeed(long culturalSeed) { this.culturalSeed = culturalSeed; }

    public boolean isRandomEventsEnabled() { return randomEventsEnabled; }
    public void setRandomEventsEnabled(boolean randomEventsEnabled) { this.randomEventsEnabled = randomEventsEnabled; }

    public boolean isEarthHistoricalLeadersEnabled() { return earthHistoricalLeadersEnabled; }
    public void setEarthHistoricalLeadersEnabled(boolean earthHistoricalLeadersEnabled) { this.earthHistoricalLeadersEnabled = earthHistoricalLeadersEnabled; }

    public boolean isProceduralLeadersEnabled() { return proceduralLeadersEnabled; }
    public void setProceduralLeadersEnabled(boolean proceduralLeadersEnabled) { this.proceduralLeadersEnabled = proceduralLeadersEnabled; }

    public String getCustomDensityBase64() { return customDensityBase64; }
    public void setCustomDensityBase64(String customDensityBase64) { this.customDensityBase64 = customDensityBase64; }
}
