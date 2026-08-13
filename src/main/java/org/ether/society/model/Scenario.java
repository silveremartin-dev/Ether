/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.procedural.PlanetPreset;
import java.io.Serializable;

/**
 * Configuration for a simulation scenario.
 */
public class Scenario implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private Long id;

    // Planet Configuration
    private PlanetPreset planetPreset;
    private EcologyPreset ecologyPreset;
    private String ecologyPresetName;
    private boolean useRealEarthData;

    // Planet Physics
    private double planetRadiusKm; // Size
    private double rotationPeriodHours;
    private double revolutionPeriodDays;
    private double axialTiltDegrees; // Inclination

    // Human Start Conditions
    private long initialHumanCount;
    private double initialTechLevel;
    private double initialCapitalPerCapita = 10.0; // Physical capital & tools in kg/capita
    private double initialEnergyPerCapita = 50.0; // Fuel & stored energy in MJ/capita
    private double initialFoodReserveMonths = 6.0; // Stored food reserves in months of consumption
    private double initialInformationPerCapita = 100.0; // Stored knowledge/archive in bits/capita
    private String populationDensityType; // "ONE_CONTINENT", "DENSE", "SPARSE", "RIVER_VALLEYS"

    // Simulation Parameters
    private double cellSizeKm2;
    private int targetCohortSize = 150; // Target population per demographic cohort node (1 to 10,000+, default 150 = Dunbar pivot)
    private double temporalResolutionDays = 30.0; // Temporal resolution time step Δt in days (default: 30.0 days = 1 month)
    private double climateHarshness; // 0.0 to 1.0 (storms, droughts)
    private long startDateYear; // e.g. -100000
    private long endDateYear = 100; // e.g. 100
    private long seed = 12345L;
    private boolean randomEventsEnabled = true;
    private String customDensityBase64;
    // Cultural Vector & Multi-Field Layers (Persisted per Scenario)
    private int cultureVectorDimensions = 8; // 4D to 32D culture vector dimensions
    private double culturalDiffusionRate = 0.05; // Free-energy cultural diffusion conductance
    private double culturalMutationRate = 0.01; // Mutation & innovation noise rate
    private String customIsoglossBase64; // Linguistic / Isogloss map layer
    private String customKinshipBase64; // Kinship & Social structure map layer
    private String customRitualsBase64; // Beliefs & Rituals map layer
    private String customSovereigntyBase64; // State sovereignty & Capital centers map layer

    // Engine Optimization & Determinism Controls (Persisted at Scenario Level for Physical Conformance)
    private boolean strictDeterminism = false;
    private boolean sparseCellSkippingEnabled = true;
    private boolean oceanMacroAggregationEnabled = true;
    private boolean coastalNavigationOnlyEnabled = true;
    private boolean oceanMultiRateTickingEnabled = true;
    private boolean parallelExecutionEnabled = true;
    private boolean spatialRangeTruncationEnabled = true;

    // Type B Procedural & Cliodynamic Engine Checkbox States & Parameters (Persisted per Scenario)
    private java.util.Map<String, Boolean> typeBEngineStates = new java.util.HashMap<>();
    private java.util.Map<String, java.util.Map<String, Double>> typeBEngineParameters = new java.util.HashMap<>();

    // Spatial Clipping & Boundary Conditions
    private boolean clippingEnabled = false;
    private double minLat = -90.0;
    private double maxLat = 90.0;
    private double minLng = -180.0;
    private double maxLng = 180.0;
    private String boundaryMode = "DYNAMIC_RESERVOIR"; // "DYNAMIC_RESERVOIR", "CLOSED_BARRIER", "PERIODIC_WRAP"

    public Scenario() {
        // Defaults
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

    /**
     * Calculates the default number of simulation ticks required to run this scenario from startDateYear to endDateYear.
     */
    public int calculateScenarioTicks() {
        long durationYears = Math.max(1, endDateYear - startDateYear);
        double ticksPerYear = 365.25 / Math.max(1.0, temporalResolutionDays);
        return (int) Math.clamp((long) (durationYears * ticksPerYear), 100L, 1000000L);
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        if (description == null || description.isBlank()) {
            return String.format("""
                🔬 SCÉNARIO EXPÉRIMENTAL ÉTHER : Configuration de Simulation Planétaire (%s)
                
                [CONTEXTE HISTORIQUE & PHYSIQUE]
                Ce scénario définit les conditions aux limites et les termes de forçage physique initiaux pour la modélisation multi-échelle des systèmes humains, écologiques et atmosphériques.
                
                [CONDITIONS INITIALES PHYSIQUES (T_0)]
                • Population Initiale : %,d individus.
                • Capital Physique (K₀) : %.1f kg/habitant (Outillages & machines).
                • Énergie Stockée (E₀) : %.1f MJ/habitant (Stocks énergétiques).
                • Réserves Alimentaires (F₀) : %.1f mois (Autonomie alimentaire).
                • Savoir Archivé (I₀) : %.1f bits/habitant (Mémoire technique).
                • Rayon Planétaire : %.0f km (g_rel = %.2f g).
                • Rotation Planétaire : %.1f heures | Inclinaison Axiale : %.1f°.
                """,
                name != null ? name : "Scénario Standard",
                initialHumanCount,
                initialCapitalPerCapita,
                initialEnergyPerCapita,
                initialFoodReserveMonths,
                initialInformationPerCapita,
                planetRadiusKm,
                planetRadiusKm / 6371.0,
                rotationPeriodHours,
                axialTiltDegrees
            );
        }
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlanetPreset getPlanetPreset() {
        return planetPreset;
    }

    public void setPlanetPreset(PlanetPreset planetPreset) {
        this.planetPreset = planetPreset;
    }

    public EcologyPreset getEcologyPreset() {
        return ecologyPreset;
    }

    public void setEcologyPreset(EcologyPreset ecologyPreset) {
        this.ecologyPreset = ecologyPreset;
    }

    public String getEcologyPresetName() {
        return ecologyPresetName;
    }

    public void setEcologyPresetName(String ecologyPresetName) {
        this.ecologyPresetName = ecologyPresetName;
    }

    public boolean isUseRealEarthData() {
        return useRealEarthData;
    }

    public void setUseRealEarthData(boolean useRealEarthData) {
        this.useRealEarthData = useRealEarthData;
    }

    public double getPlanetRadiusKm() {
        return planetRadiusKm;
    }

    public void setPlanetRadiusKm(double planetRadiusKm) {
        this.planetRadiusKm = planetRadiusKm;
    }

    public double getRotationPeriodHours() {
        return rotationPeriodHours;
    }

    public void setRotationPeriodHours(double rotationPeriodHours) {
        this.rotationPeriodHours = rotationPeriodHours;
    }

    public double getRevolutionPeriodDays() {
        return revolutionPeriodDays;
    }

    public void setRevolutionPeriodDays(double revolutionPeriodDays) {
        this.revolutionPeriodDays = revolutionPeriodDays;
    }

    public double getAxialTiltDegrees() {
        return axialTiltDegrees;
    }

    public void setAxialTiltDegrees(double axialTiltDegrees) {
        this.axialTiltDegrees = axialTiltDegrees;
    }

    public long getInitialHumanCount() {
        return initialHumanCount;
    }

    public void setInitialHumanCount(long initialHumanCount) {
        this.initialHumanCount = initialHumanCount;
    }

    public double getInitialTechLevel() {
        return initialTechLevel;
    }

    public void setInitialTechLevel(double initialTechLevel) {
        this.initialTechLevel = initialTechLevel;
    }

    public double getInitialCapitalPerCapita() {
        return initialCapitalPerCapita;
    }

    public void setInitialCapitalPerCapita(double initialCapitalPerCapita) {
        this.initialCapitalPerCapita = initialCapitalPerCapita;
    }

    public double getInitialEnergyPerCapita() {
        return initialEnergyPerCapita;
    }

    public void setInitialEnergyPerCapita(double initialEnergyPerCapita) {
        this.initialEnergyPerCapita = initialEnergyPerCapita;
    }

    public double getInitialFoodReserveMonths() {
        return initialFoodReserveMonths;
    }

    public void setInitialFoodReserveMonths(double initialFoodReserveMonths) {
        this.initialFoodReserveMonths = initialFoodReserveMonths;
    }

    public double getInitialInformationPerCapita() {
        return initialInformationPerCapita;
    }

    public void setInitialInformationPerCapita(double initialInformationPerCapita) {
        this.initialInformationPerCapita = initialInformationPerCapita;
    }

    public String getPopulationDensityType() {
        return populationDensityType;
    }

    public void setPopulationDensityType(String populationDensityType) {
        this.populationDensityType = populationDensityType;
    }

    public double getCellSizeKm2() {
        return cellSizeKm2;
    }

    public void setCellSizeKm2(double cellSizeKm2) {
        this.cellSizeKm2 = cellSizeKm2;
    }

    public int getTargetCohortSize() {
        return targetCohortSize;
    }

    public void setTargetCohortSize(int targetCohortSize) {
        this.targetCohortSize = targetCohortSize;
    }

    public double getClimateHarshness() {
        return climateHarshness;
    }

    public void setClimateHarshness(double climateHarshness) {
        this.climateHarshness = climateHarshness;
    }

    public long getStartDateYear() {
        return startDateYear;
    }

    public void setStartDateYear(long startDateYear) {
        this.startDateYear = startDateYear;
    }

    public long getEndDateYear() {
        return endDateYear;
    }

    public void setEndDateYear(long endDateYear) {
        this.endDateYear = endDateYear;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public boolean isRandomEventsEnabled() {
        return randomEventsEnabled;
    }

    public void setRandomEventsEnabled(boolean randomEventsEnabled) {
        this.randomEventsEnabled = randomEventsEnabled;
    }

    public String getCustomDensityBase64() {
        return customDensityBase64;
    }

    public void setCustomDensityBase64(String customDensityBase64) {
        this.customDensityBase64 = customDensityBase64;
    }

    public boolean isClippingEnabled() {
        return clippingEnabled;
    }

    public void setClippingEnabled(boolean clippingEnabled) {
        this.clippingEnabled = clippingEnabled;
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public double getMinLng() {
        return minLng;
    }

    public void setMinLng(double minLng) {
        this.minLng = minLng;
    }

    public double getMaxLng() {
        return maxLng;
    }

    public void setMaxLng(double maxLng) {
        this.maxLng = maxLng;
    }

    public String getBoundaryMode() {
        return boundaryMode;
    }

    public void setBoundaryMode(String boundaryMode) {
        this.boundaryMode = boundaryMode;
    }

    public boolean isStrictDeterminism() {
        return strictDeterminism;
    }

    public void setStrictDeterminism(boolean strictDeterminism) {
        this.strictDeterminism = strictDeterminism;
        if (strictDeterminism) {
            this.sparseCellSkippingEnabled = false;
            this.oceanMultiRateTickingEnabled = false;
            this.parallelExecutionEnabled = false;
            this.spatialRangeTruncationEnabled = false;
        }
    }

    public boolean isSparseCellSkippingEnabled() {
        return sparseCellSkippingEnabled;
    }

    public void setSparseCellSkippingEnabled(boolean sparseCellSkippingEnabled) {
        this.sparseCellSkippingEnabled = sparseCellSkippingEnabled;
        if (sparseCellSkippingEnabled) {
            this.strictDeterminism = false;
        }
    }

    public boolean isOceanMacroAggregationEnabled() {
        return oceanMacroAggregationEnabled;
    }

    public void setOceanMacroAggregationEnabled(boolean oceanMacroAggregationEnabled) {
        this.oceanMacroAggregationEnabled = oceanMacroAggregationEnabled;
    }

    public boolean isCoastalNavigationOnlyEnabled() {
        return coastalNavigationOnlyEnabled;
    }

    public void setCoastalNavigationOnlyEnabled(boolean coastalNavigationOnlyEnabled) {
        this.coastalNavigationOnlyEnabled = coastalNavigationOnlyEnabled;
    }

    public boolean isOceanMultiRateTickingEnabled() {
        return oceanMultiRateTickingEnabled;
    }

    public void setOceanMultiRateTickingEnabled(boolean oceanMultiRateTickingEnabled) {
        this.oceanMultiRateTickingEnabled = oceanMultiRateTickingEnabled;
        if (oceanMultiRateTickingEnabled) {
            this.strictDeterminism = false;
        }
    }

    public boolean isParallelExecutionEnabled() {
        return parallelExecutionEnabled;
    }

    public void setParallelExecutionEnabled(boolean parallelExecutionEnabled) {
        this.parallelExecutionEnabled = parallelExecutionEnabled;
        if (parallelExecutionEnabled) {
            this.strictDeterminism = false;
        }
    }

    public boolean isSpatialRangeTruncationEnabled() {
        return spatialRangeTruncationEnabled;
    }

    public void setSpatialRangeTruncationEnabled(boolean spatialRangeTruncationEnabled) {
        this.spatialRangeTruncationEnabled = spatialRangeTruncationEnabled;
        if (spatialRangeTruncationEnabled) {
            this.strictDeterminism = false;
        }
    }

    private int climateTickFrequency = 5;
    private int parallelThreadCount = 0;

    public int getClimateTickFrequency() {
        return climateTickFrequency;
    }

    public void setClimateTickFrequency(int climateTickFrequency) {
        this.climateTickFrequency = Math.max(1, climateTickFrequency);
    }

    public int getParallelThreadCount() {
        return parallelThreadCount;
    }

    public void setParallelThreadCount(int parallelThreadCount) {
        this.parallelThreadCount = Math.max(0, parallelThreadCount);
    }

    /**
     * Converts this scenario's optimization settings into a runtime SimulationPerformanceConfig instance.
     */
    public org.ether.society.procedural.SimulationPerformanceConfig toPerformanceConfig() {
        org.ether.society.procedural.SimulationPerformanceConfig config = new org.ether.society.procedural.SimulationPerformanceConfig();
        config.setStrictDeterminism(strictDeterminism);
        config.setEnableSparseCellSkipping(!strictDeterminism && sparseCellSkippingEnabled);
        config.setEnableMultiFreqClimateTicks(!strictDeterminism && oceanMultiRateTickingEnabled);
        config.setClimateTickFrequency(strictDeterminism ? 1 : climateTickFrequency);
        config.setEnableParallelExecution(!strictDeterminism && parallelExecutionEnabled);
        config.setParallelThreadCount(parallelExecutionEnabled ? parallelThreadCount : 1);
        config.setEnableSpatialRangeTruncation(!strictDeterminism && spatialRangeTruncationEnabled);
        return config;
    }

    public double getTemporalResolutionDays() {
        return temporalResolutionDays;
    }

    public void setTemporalResolutionDays(double temporalResolutionDays) {
        this.temporalResolutionDays = temporalResolutionDays;
    }

    public java.util.Map<String, Boolean> getTypeBEngineStates() {
        if (typeBEngineStates == null) {
            typeBEngineStates = new java.util.HashMap<>();
        }
        return typeBEngineStates;
    }

    public void setTypeBEngineStates(java.util.Map<String, Boolean> typeBEngineStates) {
        this.typeBEngineStates = typeBEngineStates != null ? typeBEngineStates : new java.util.HashMap<>();
    }

    public java.util.Map<String, java.util.Map<String, Double>> getTypeBEngineParameters() {
        if (typeBEngineParameters == null) {
            typeBEngineParameters = new java.util.HashMap<>();
        }
        return typeBEngineParameters;
    }

    public void setTypeBEngineParameters(java.util.Map<String, java.util.Map<String, Double>> typeBEngineParameters) {
        this.typeBEngineParameters = typeBEngineParameters != null ? typeBEngineParameters : new java.util.HashMap<>();
    }

    public int getCultureVectorDimensions() {
        return cultureVectorDimensions;
    }

    public void setCultureVectorDimensions(int cultureVectorDimensions) {
        this.cultureVectorDimensions = cultureVectorDimensions;
    }

    public double getCulturalDiffusionRate() {
        return culturalDiffusionRate;
    }

    public void setCulturalDiffusionRate(double culturalDiffusionRate) {
        this.culturalDiffusionRate = culturalDiffusionRate;
    }

    public double getCulturalMutationRate() {
        return culturalMutationRate;
    }

    public void setCulturalMutationRate(double culturalMutationRate) {
        this.culturalMutationRate = culturalMutationRate;
    }

    public String getCustomIsoglossBase64() {
        return customIsoglossBase64;
    }

    public void setCustomIsoglossBase64(String customIsoglossBase64) {
        this.customIsoglossBase64 = customIsoglossBase64;
    }

    public String getCustomKinshipBase64() {
        return customKinshipBase64;
    }

    public void setCustomKinshipBase64(String customKinshipBase64) {
        this.customKinshipBase64 = customKinshipBase64;
    }

    public String getCustomRitualsBase64() {
        return customRitualsBase64;
    }

    public void setCustomRitualsBase64(String customRitualsBase64) {
        this.customRitualsBase64 = customRitualsBase64;
    }

    public String getCustomSovereigntyBase64() {
        return customSovereigntyBase64;
    }

    public void setCustomSovereigntyBase64(String customSovereigntyBase64) {
        this.customSovereigntyBase64 = customSovereigntyBase64;
    }
}
