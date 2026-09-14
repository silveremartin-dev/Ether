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
    private String presetKey;

    public String getPresetKey() {
        return presetKey;
    }

    public void setPresetKey(String presetKey) {
        this.presetKey = presetKey;
    }

    public String getDisplayName() {
        if (presetKey != null && !presetKey.isBlank()) {
            String localized = org.ether.society.i18n.I18n.getOrDefault("scenario.preset." + presetKey + ".name", name);
            if (localized != null && !localized.isBlank()) {
                return localized;
            }
        }
        return name != null ? name : "Unnamed Scenario";
    }

    public String getDisplayDescription() {
        if (presetKey != null && !presetKey.isBlank()) {
            String localized = org.ether.society.i18n.I18n.getOrDefault("scenario.preset." + presetKey + ".desc", description);
            if (localized != null && !localized.isBlank()) {
                return localized;
            }
        }
        return description != null ? description : "";
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    // Planet Configuration
    private PlanetPreset planetPreset = PlanetPreset.EARTH_LIKE;
    private EcologyPreset ecologyPreset = EcologyPreset.EARTH_STANDARD;
    private String ecologyPresetName = "Earth Standard Baseline";
    private boolean useRealEarthData;

    // Planet Physics
    private double planetRadiusKm; // Size
    private double rotationPeriodHours;
    private double revolutionPeriodDays;
    private double axialTiltDegrees; // Inclination

    public enum TechPreset {
        AUTO_FROM_YEAR("⏳ Automatique (Calculé selon l'année T₀)", -1, -1, -1, -1),
        PALEOLITHIC("🏹 Chasseurs-Cueilleurs / Néolithique", 5.0, 10.0, 2.0, 5.0),
        NEOLITHIC_BRONZE("🛡️ Âge du Bronze & Cités-États", 25.0, 30.0, 4.0, 40.0),
        ANTIQUITY("🏛️ Antiquité Classique & Empire", 100.0, 60.0, 6.0, 200.0),
        RENAISSANCE("⛵ Renaissance & Imprimerie", 500.0, 300.0, 8.0, 2000.0),
        INDUSTRIAL("⚙️ Révolution Industrielle & Vapeur", 2500.0, 2500.0, 10.0, 15000.0),
        CONTEMPORARY("🌐 Contemporain & Métropole Numérique", 15000.0, 50000.0, 18.0, 5000000.0),
        SPACE_COLONY_MARS("🚀 Colonie Spatiale / Mars (Faible Pop / Ultra High-Tech)", 50000.0, 200000.0, 24.0, 50000000.0),
        CUSTOM("⚙️ Personnalisé (Saisie Libre des 4 Stocks)", -1, -1, -1, -1);

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

        public String getLabel() { return label; }
        public double getCapital() { return capital; }
        public double getEnergy() { return energy; }
        public double getFoodMonths() { return foodMonths; }
        public double getInfo() { return info; }

        @Override
        public String toString() { return label; }
    }

    private TechPreset techPreset = TechPreset.AUTO_FROM_YEAR;

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
    private long seed = 12345L; // Demographic density seed
    private long culturalSeed = 54321L; // Cultural tensor suite seed
    private boolean randomEventsEnabled = true;
    private String customDensityBase64;
    // Cultural Vector & Multi-Field Layers (Persisted per Scenario)
    private int cultureVectorDimensions = 8; // 4D to 32D culture vector dimensions
    private double culturalDiffusionRate = 0.05; // Free-energy cultural diffusion conductance
    private double culturalMutationRate = 0.01; // Mutation & innovation noise rate
    private java.util.List<String> customTensorMapsBase64 = new java.util.ArrayList<>();
    private java.util.List<Boolean> tensorProceduralModes = new java.util.ArrayList<>();
    private java.util.Map<Integer, Long> tensorSeeds = new java.util.HashMap<>();
    private java.util.Map<Integer, java.util.Map<String, Double>> tensorProceduralParameters = new java.util.HashMap<>();

    // Geological & Mineral Energy Extensible Tensor Layers (Persisted per Scenario)
    private int resourceVectorDimensions = 8; // Extensible resource dimensions (COAL, OIL, GAS, URANIUM, HELIUM_3, IRON_COPPER, PRECIOUS_REE, AQUIFERS...)
    private java.util.List<String> customGeologyTensorMapsBase64 = new java.util.ArrayList<>();
    private java.util.List<Boolean> geologyTensorProceduralModes = new java.util.ArrayList<>();

    // Engine Optimization & Determinism Controls (Persisted at Scenario Level for Physical Conformance)
    private boolean strictDeterminism = true;
    private boolean sparseCellSkippingEnabled = false;
    private boolean oceanMacroAggregationEnabled = false;
    private boolean coastalNavigationOnlyEnabled = false;
    private boolean oceanMultiRateTickingEnabled = false;
    private boolean parallelExecutionEnabled = false;
    private boolean spatialRangeTruncationEnabled = false;

    // Type B Procedural & Cliodynamic Engine Checkbox States & Parameters (Persisted per Scenario)
    private java.util.Map<String, Boolean> typeBEngineStates = new java.util.HashMap<>();
    private java.util.Map<String, java.util.Map<String, Double>> typeBEngineParameters = new java.util.HashMap<>();

    // Scheduled Climate and Planetary Cataclysm Events
    private java.util.List<ClimateEvent> climateEvents = new java.util.ArrayList<>();

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

    public TechPreset getTechPreset() {
        return techPreset != null ? techPreset : TechPreset.AUTO_FROM_YEAR;
    }

    public void setTechPreset(TechPreset techPreset) {
        this.techPreset = techPreset != null ? techPreset : TechPreset.AUTO_FROM_YEAR;
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

    public long getCulturalSeed() {
        return culturalSeed;
    }

    public void setCulturalSeed(long culturalSeed) {
        this.culturalSeed = culturalSeed;
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

    public java.util.List<String> getCustomTensorMapsBase64() {
        if (customTensorMapsBase64 == null) {
            customTensorMapsBase64 = new java.util.ArrayList<>();
        }
        return customTensorMapsBase64;
    }

    public void setCustomTensorMapsBase64(java.util.List<String> customTensorMapsBase64) {
        this.customTensorMapsBase64 = customTensorMapsBase64 != null ? customTensorMapsBase64 : new java.util.ArrayList<>();
    }

    public java.util.List<Boolean> getTensorProceduralModes() {
        if (tensorProceduralModes == null) {
            tensorProceduralModes = new java.util.ArrayList<>();
        }
        return tensorProceduralModes;
    }

    public void setTensorProceduralModes(java.util.List<Boolean> tensorProceduralModes) {
        this.tensorProceduralModes = tensorProceduralModes != null ? tensorProceduralModes : new java.util.ArrayList<>();
    }

    public String getCustomTensorMapBase64(int index) {
        java.util.List<String> list = getCustomTensorMapsBase64();
        if (index >= 0 && index < list.size() && list.get(index) != null && !list.get(index).isBlank()) {
            return list.get(index);
        }
        return null;
    }

    public void setCustomTensorMapBase64(int index, String base64) {
        java.util.List<String> list = getCustomTensorMapsBase64();
        while (list.size() <= index) {
            list.add(null);
        }
        list.set(index, base64);
    }

    public java.util.Map<Integer, Long> getTensorSeeds() {
        if (tensorSeeds == null) {
            tensorSeeds = new java.util.HashMap<>();
        }
        return tensorSeeds;
    }

    public void setTensorSeeds(java.util.Map<Integer, Long> tensorSeeds) {
        this.tensorSeeds = tensorSeeds != null ? tensorSeeds : new java.util.HashMap<>();
    }

    public java.util.Map<Integer, java.util.Map<String, Double>> getTensorProceduralParameters() {
        if (tensorProceduralParameters == null) {
            tensorProceduralParameters = new java.util.HashMap<>();
        }
        return tensorProceduralParameters;
    }

    public void setTensorProceduralParameters(java.util.Map<Integer, java.util.Map<String, Double>> tensorProceduralParameters) {
        this.tensorProceduralParameters = tensorProceduralParameters != null ? tensorProceduralParameters : new java.util.HashMap<>();
    }

    public int getResourceVectorDimensions() {
        return resourceVectorDimensions;
    }

    public void setResourceVectorDimensions(int resourceVectorDimensions) {
        this.resourceVectorDimensions = resourceVectorDimensions;
    }

    public java.util.List<String> getCustomGeologyTensorMapsBase64() {
        if (customGeologyTensorMapsBase64 == null) {
            customGeologyTensorMapsBase64 = new java.util.ArrayList<>();
        }
        return customGeologyTensorMapsBase64;
    }

    public void setCustomGeologyTensorMapsBase64(java.util.List<String> customGeologyTensorMapsBase64) {
        this.customGeologyTensorMapsBase64 = customGeologyTensorMapsBase64 != null ? customGeologyTensorMapsBase64 : new java.util.ArrayList<>();
    }

    public java.util.List<Boolean> getGeologyTensorProceduralModes() {
        if (geologyTensorProceduralModes == null) {
            geologyTensorProceduralModes = new java.util.ArrayList<>();
        }
        return geologyTensorProceduralModes;
    }

    public void setGeologyTensorProceduralModes(java.util.List<Boolean> geologyTensorProceduralModes) {
        this.geologyTensorProceduralModes = geologyTensorProceduralModes != null ? geologyTensorProceduralModes : new java.util.ArrayList<>();
    }

    public String getCustomGeologyTensorMapBase64(int index) {
        java.util.List<String> list = getCustomGeologyTensorMapsBase64();
        if (index >= 0 && index < list.size() && list.get(index) != null && !list.get(index).isBlank()) {
            return list.get(index);
        }
        return null;
    }

    public void setCustomGeologyTensorMapBase64(int index, String base64) {
        java.util.List<String> list = getCustomGeologyTensorMapsBase64();
        while (list.size() <= index) {
            list.add(null);
        }
        list.set(index, base64);
    }

    public java.util.List<ClimateEvent> getClimateEvents() {
        return climateEvents;
    }

    public void setClimateEvents(java.util.List<ClimateEvent> climateEvents) {
        this.climateEvents = climateEvents != null ? climateEvents : new java.util.ArrayList<>();
    }

    public static java.util.List<Scenario> getBuiltInScenarios() {
        java.util.List<Scenario> list = new java.util.ArrayList<>();

        // --- SCÉNARIOS DU PASSÉ ---
        Scenario s0 = new Scenario();
        s0.setPresetKey("out_of_africa");
        s0.setName("Sortie d'Afrique & Expansion Homo Sapiens (-100000)");
        s0.setStartDateYear(-100000);
        s0.setEndDateYear(-20000);
        s0.setInitialHumanCount(50000);
        s0.setInitialCapitalPerCapita(2.0);
        s0.setInitialEnergyPerCapita(5.0);
        s0.setInitialFoodReserveMonths(2.0);
        s0.setInitialInformationPerCapita(2.0);
        s0.setPopulationDensityType("ONE_CONTINENT");
        s0.setTargetCohortSize(30);
        s0.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s0.setDescription("""
            🌍 SCÉNARIO PALÉOLITHIQUE : Berceau Africain, Traversée des Continents & Out of Africa (-100 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la dynamique démographique et l'expansion spatiale des premières populations d'Homo Sapiens depuis l'Afrique de l'Est à travers le Moyen-Orient, l'Eurasie, l'Océanie et les Amériques.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Population Initiale : 50 000 individus (Capacité nomade pré-agricole).
            • Stock Capital Physique (K₀) : 2 kg/habitant (bifaces en pierre, javelots, bifaces).
            • Énergie Stockée (E₀) : 5 MJ/habitant (maîtrise du feu et combustible bois).
            • Réserves Alimentaires (F₀) : 2 mois de subsistance en chasse-cueillette.
            • Savoir Archivé (I₀) : 2 bits/habitant (traditions orales paléolithiques & langage).
            """);
        java.util.Map<String, Boolean> s0Engines = s0.getTypeBEngineStates();
        s0Engines.put("PaleoLanguageDriftEngine", true);
        s0Engines.put("KarstCaveShelterEngine", true);
        s0Engines.put("ParietalArtAsabiyyahEngine", true);
        s0Engines.put("SnowpackMobilityEngine", true);
        s0Engines.put("MeatCuringReservesEngine", true);
        s0Engines.put("OsseousIndustryCarvingEngine", true);
        s0Engines.put("PlantFiberCordageEngine", true);
        s0Engines.put("CanidDomesticationEngine", true);
        s0Engines.put("LithicTradeProvenanceEngine", true);
        s0Engines.put("ExogamousKinshipEngine", true);
        s0Engines.put("TailoredClothingThermalEngine", true);
        s0Engines.put("AtlatlArcheryBallisticsEngine", true);
        s0Engines.put("PassiveSnareSmallGameEngine", true);
        s0Engines.put("MegafaunaPitfallTrapEngine", true);
        s0Engines.put("ArchaicIntrogressionEngine", true);
        s0Engines.put("OchreTanningTechnologyEngine", true);
        s0Engines.put("CoastalMarineRefugiaEngine", true);
        s0Engines.put("SeasonalAggregationSanctuaryEngine", true);
        s0Engines.put("AridWaterStorageStashEngine", true);
        s0Engines.put("ResinHaftingAdhesivesEngine", true);
        s0Engines.put("MammothBoneHabitationEngine", true);
        s0Engines.put("FireHardenedSpearEngine", true);
        s0Engines.put("VolcanicTephraRefugiaEngine", true);
        s0Engines.put("ParasiteControlRepellentEngine", true);
        list.add(s0);

        // --- SCÉNARIO : SAHUL (-50000) ---
        Scenario sSahul = new Scenario();
        sSahul.setPresetKey("sahul");
        sSahul.setName("Sahul & Premier Peuplement de l'Australie (-50000)");
        sSahul.setStartDateYear(-50000);
        sSahul.setEndDateYear(-10000);
        sSahul.setInitialHumanCount(30000);
        sSahul.setInitialCapitalPerCapita(3.0);
        sSahul.setInitialEnergyPerCapita(6.0);
        sSahul.setInitialFoodReserveMonths(2.0);
        sSahul.setInitialInformationPerCapita(3.0);
        sSahul.setPopulationDensityType("AUSTRALIA_SAHUL");
        sSahul.setTargetCohortSize(35);
        sSahul.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sSahul.setClippingEnabled(true);
        sSahul.setMinLat(-42.0); sSahul.setMaxLat(-10.0); sSahul.setMinLng(112.0); sSahul.setMaxLng(155.0);
        sSahul.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSahul.setDescription("""
            🦘 SCÉNARIO PALÉOLITHIQUE : Traversée Maritime & Incursion dans le Sahul (-50 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Premier franchissement maritime majeur de la ligne de Wallace par les ancêtres des Aborigènes d'Australie. Modélise la colonisation du continent Sahul (Australie, Tasmanie, Nouvelle-Guinée réunies) et l'adaptation aux écosystèmes arides.
            """);
        java.util.Map<String, Boolean> sSahulEngines = sSahul.getTypeBEngineStates();
        sSahulEngines.put("PaleoLanguageDriftEngine", true);
        sSahulEngines.put("KarstCaveShelterEngine", true);
        sSahulEngines.put("ParietalArtAsabiyyahEngine", true);
        sSahulEngines.put("ResinHaftingAdhesivesEngine", true);
        sSahulEngines.put("AridWaterStorageStashEngine", true);
        sSahulEngines.put("SymbolicBeadNetworkEngine", true);
        sSahulEngines.put("RiverCanoeTransportEngine", true);
        sSahulEngines.put("CoastalMarineRefugiaEngine", true);
        sSahulEngines.put("ExogamousKinshipEngine", true);
        list.add(sSahul);

        // --- SCÉNARIO : BÉRINGIE & PEUPLEMENT DES AMÉRIQUES (-25000) ---
        Scenario sBeringia = new Scenario();
        sBeringia.setPresetKey("beringia");
        sBeringia.setName("Béringie & Peuplement des Amériques (-25000)");
        sBeringia.setStartDateYear(-25000);
        sBeringia.setEndDateYear(-10000);
        sBeringia.setInitialHumanCount(15000);
        sBeringia.setInitialCapitalPerCapita(3.0);
        sBeringia.setInitialEnergyPerCapita(6.0);
        sBeringia.setInitialFoodReserveMonths(2.0);
        sBeringia.setInitialInformationPerCapita(4.0);
        sBeringia.setPopulationDensityType("BERINGIA_AMERICAS");
        sBeringia.setTargetCohortSize(40);
        sBeringia.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sBeringia.setClippingEnabled(true);
        sBeringia.setMinLat(45.0); sBeringia.setMaxLat(75.0); sBeringia.setMinLng(140.0); sBeringia.setMaxLng(-120.0);
        sBeringia.setBoundaryMode("DYNAMIC_RESERVOIR");
        sBeringia.setDescription("""
            🏔️ SCÉNARIO PALÉOLITHIQUE : Le Pont Terrestre de Béringie & Incursion Américaine (-25 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'isolation des populations paléolithiques sur le pont terrestre de Béringie pendant le Dernier Maximum Glaciaire (LGM), suivie de leur dispersion à travers le corridor libre de glace et la route côtière du Pacifique.
            """);
        java.util.Map<String, Boolean> sBeringiaEngines = sBeringia.getTypeBEngineStates();
        sBeringiaEngines.put("SnowpackMobilityEngine", true);
        sBeringiaEngines.put("TailoredClothingThermalEngine", true);
        sBeringiaEngines.put("MeatCuringReservesEngine", true);
        sBeringiaEngines.put("OsseousIndustryCarvingEngine", true);
        sBeringiaEngines.put("CanidDomesticationEngine", true);
        sBeringiaEngines.put("MammothBoneHabitationEngine", true);
        sBeringiaEngines.put("PermafrostColdCacheEngine", true);
        sBeringiaEngines.put("LithicTradeProvenanceEngine", true);
        sBeringiaEngines.put("MegafaunaPitfallTrapEngine", true);
        list.add(sBeringia);

        // --- SCÉNARIO : RÉCENTS DRYAS (-10900) ---
        Scenario sYoungerDryas = new Scenario();
        sYoungerDryas.setPresetKey("younger_dryas");
        sYoungerDryas.setName("Le Récents Dryas & Choc Climatique Natufien (-10900)");
        sYoungerDryas.setStartDateYear(-10900);
        sYoungerDryas.setEndDateYear(-9500);
        sYoungerDryas.setInitialHumanCount(40000);
        sYoungerDryas.setInitialCapitalPerCapita(4.0);
        sYoungerDryas.setInitialEnergyPerCapita(8.0);
        sYoungerDryas.setInitialFoodReserveMonths(2.5);
        sYoungerDryas.setInitialInformationPerCapita(8.0);
        sYoungerDryas.setPopulationDensityType("YOUNGER_DRYAS");
        sYoungerDryas.setTargetCohortSize(50);
        sYoungerDryas.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sYoungerDryas.setDescription("""
            ❄️ SCÉNARIO PALÉOCLIMATIQUE : Le Récents Dryas & Pression Foragère Au Levant (-10 900 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Refroidissement brutal de 5 à 8°C de l'Atlantique Nord déclenché par le déversement d'eau douce du Lac Agassiz. Au Levant, la sécheresse aiguë réduit les céréales sauvages, contraignant les populations Natufiennes à la sédentarisation pré-agricole et au contrôle des graines.
            """);
        java.util.Map<String, Boolean> sYoungerDryasEngines = sYoungerDryas.getTypeBEngineStates();
        sYoungerDryasEngines.put("WildCerealGrindingEngine", true);
        sYoungerDryasEngines.put("TopographicGameDriveEngine", true);
        sYoungerDryasEngines.put("AtlatlArcheryBallisticsEngine", true);
        sYoungerDryasEngines.put("PassiveSnareSmallGameEngine", true);
        sYoungerDryasEngines.put("StoneBoilingThermalEngine", true);
        sYoungerDryasEngines.put("HaliteSaltCuringEngine", true);
        sYoungerDryasEngines.put("SeasonalAggregationSanctuaryEngine", true);
        list.add(sYoungerDryas);

        Scenario s1 = new Scenario();
        s1.setPresetKey("fertile_crescent");
        s1.setName("Croissant Fertile & Néolithique (-8000)");
        s1.setStartDateYear(-8000);
        s1.setEndDateYear(-5000);
        s1.setInitialHumanCount(25000);
        s1.setInitialCapitalPerCapita(5.0);
        s1.setInitialEnergyPerCapita(10.0);
        s1.setInitialFoodReserveMonths(3.0);
        s1.setInitialInformationPerCapita(5.0);
        s1.setPopulationDensityType("FERTILE_CRESCENT");
        s1.setTargetCohortSize(150);
        s1.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s1.setClippingEnabled(true);
        s1.setMinLat(25.0); s1.setMaxLat(42.0); s1.setMinLng(25.0); s1.setMaxLng(55.0);
        s1.setBoundaryMode("DYNAMIC_RESERVOIR");
        s1.setDescription("""
            🌾 SCÉNARIO HISTORIQUE : L'Aube de l'Agriculture au Croissant Fertile (-8000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Ce scénario modélise la transition majeure du Néolithique entre l'économie de subsistance des chasseurs-cueilleurs et l'émergence des premières communautés agricoles sédentaires le long du Tigre, de l'Euphrate, du Nil et de la côte Levantine.
            """);
        list.add(s1);

        // --- SCÉNARIO : SAHARA VERT (PÉRIODE HUMIDE AFRICAINE -6000) ---
        Scenario sGreenSahara = new Scenario();
        sGreenSahara.setPresetKey("green_sahara");
        sGreenSahara.setName("Le Sahara Vert & Période Humide Africaine (-6000)");
        sGreenSahara.setStartDateYear(-6000);
        sGreenSahara.setEndDateYear(-3500);
        sGreenSahara.setInitialHumanCount(60000);
        sGreenSahara.setInitialCapitalPerCapita(6.0);
        sGreenSahara.setInitialEnergyPerCapita(12.0);
        sGreenSahara.setInitialFoodReserveMonths(4.0);
        sGreenSahara.setInitialInformationPerCapita(10.0);
        sGreenSahara.setPopulationDensityType("GREEN_SAHARA");
        sGreenSahara.setTargetCohortSize(60);
        sGreenSahara.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sGreenSahara.setDescription("""
            🌴 SCÉNARIO PALÉOCLIMATIQUE : Le Sahara Vert & Période Humide Africaine (-6000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la Période Humide Africaine (AHP) où l'insolation printanière amplifiée par l'orbite terrestre a intensifié la mousson africaine. Le désert du Sahara était alors une savane verdoyante parsemée de lac majeurs (Lac Méga-Tchad), peuplée d'éleveurs néolithiques et de chasseurs-cueilleurs.
            """);
        list.add(sGreenSahara);

        // --- SCÉNARIO : ÉGYPTE ANTIQUE (-3000) ---
        Scenario sEgypt = new Scenario();
        sEgypt.setPresetKey("ancient_egypt");
        sEgypt.setName("Égypte Antique & Vallée du Nil (-3000)");
        sEgypt.setStartDateYear(-3000);
        sEgypt.setEndDateYear(-1000);
        sEgypt.setInitialHumanCount(1500000);
        sEgypt.setInitialCapitalPerCapita(60.0);
        sEgypt.setInitialEnergyPerCapita(40.0);
        sEgypt.setInitialFoodReserveMonths(6.0);
        sEgypt.setInitialInformationPerCapita(40.0);
        sEgypt.setPopulationDensityType("EGYPT_NILE");
        sEgypt.setTargetCohortSize(300);
        sEgypt.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sEgypt.setClippingEnabled(true);
        sEgypt.setMinLat(21.0); sEgypt.setMaxLat(32.0); sEgypt.setMinLng(24.0); sEgypt.setMaxLng(36.0);
        sEgypt.setBoundaryMode("DYNAMIC_RESERVOIR");
        sEgypt.setDescription("""
            𓀀 SCÉNARIO HISTORIQUE : Unification Thinite & Crues du Nil (-3000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'émergence de la première civilisation pharaonique unifiée. Dépendance absolue vis-à-vis du rythme annuel du Nil, de la gestion du bassin d'irrigation et de l'administration hiéroglyphique.
            """);
        list.add(sEgypt);

        Scenario s3 = new Scenario();
        s3.setPresetKey("assyrian_empire");
        s3.setName("Empire Assyrien & Irrigation Mésopotamienne (-2000)");
        s3.setStartDateYear(-2000);
        s3.setEndDateYear(-600);
        s3.setInitialHumanCount(500000);
        s3.setInitialCapitalPerCapita(80.0);
        s3.setInitialEnergyPerCapita(50.0);
        s3.setInitialFoodReserveMonths(6.0);
        s3.setInitialInformationPerCapita(50.0);
        s3.setPopulationDensityType("MESOPOTAMIA_ASSYRIA");
        s3.setTargetCohortSize(500);
        s3.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s3.setClippingEnabled(true);
        s3.setMinLat(28.0); s3.setMaxLat(40.0); s3.setMinLng(38.0); s3.setMaxLng(52.0);
        s3.setBoundaryMode("DYNAMIC_RESERVOIR");
        s3.setDescription("""
            🏛️ SCÉNARIO HISTORIQUE : Hydraulique, Salinisation & Guerre Cinétique Assyrienne (-2000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'apogée et les vulnérabilités de la civilisation mésopotamienne et de l'Empire Assyrien basés sur l'irrigation intensive à partir du Tigre et de l'Euphrate.
            """);
        list.add(s3);

        // --- SCÉNARIO : MÉSOAMÉRIQUE (-1500) ---
        Scenario sMeso = new Scenario();
        sMeso.setPresetKey("mesoamerica");
        sMeso.setName("Civilisations Mésoaméricaines (Olmèques & Mayas) (-1500)");
        sMeso.setStartDateYear(-1500);
        sMeso.setEndDateYear(900);
        sMeso.setInitialHumanCount(3000000);
        sMeso.setInitialCapitalPerCapita(120.0);
        sMeso.setInitialEnergyPerCapita(80.0);
        sMeso.setInitialFoodReserveMonths(6.0);
        sMeso.setInitialInformationPerCapita(150.0);
        sMeso.setPopulationDensityType("MESOAMERICA");
        sMeso.setTargetCohortSize(250);
        sMeso.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sMeso.setClippingEnabled(true);
        sMeso.setMinLat(12.0); sMeso.setMaxLat(24.0); sMeso.setMinLng(-105.0); sMeso.setMaxLng(-85.0);
        sMeso.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMeso.setDescription("""
            🌽 SCÉNARIO HISTORIQUE : Culture Mère Olmèque & Cités-États Mayas (-1500 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Émergence des centres cérémoniels de San Lorenzo et La Venta, puis essor de la civilisation maya classique. Modélise la maïsiculture intensive, les réservoirs d'eau pluviale et l'astronomie de précision.
            """);
        list.add(sMeso);

        // --- SCÉNARIO : EMPIRE MAURYA & INDE (-300) ---
        Scenario sMaurya = new Scenario();
        sMaurya.setPresetKey("maurya_empire");
        sMaurya.setName("Empire Maurya & Civilisation de l'Indus-Gange (-300)");
        sMaurya.setStartDateYear(-300);
        sMaurya.setEndDateYear(100);
        sMaurya.setInitialHumanCount(50000000);
        sMaurya.setInitialCapitalPerCapita(200.0);
        sMaurya.setInitialEnergyPerCapita(90.0);
        sMaurya.setInitialFoodReserveMonths(6.0);
        sMaurya.setInitialInformationPerCapita(300.0);
        sMaurya.setPopulationDensityType("INDIA_MAURYA");
        sMaurya.setTargetCohortSize(1000);
        sMaurya.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sMaurya.setClippingEnabled(true);
        sMaurya.setMinLat(8.0); sMaurya.setMaxLat(35.0); sMaurya.setMinLng(68.0); sMaurya.setMaxLng(90.0);
        sMaurya.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMaurya.setDescription("""
            ☸️ SCÉNARIO HISTORIQUE : L'Empire Maurya d'Ashoka & La Vallée du Gange (-300 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Unification du sous-continent indien sous Chandragupta et Ashoka. Modélise l'agriculture rizicole de la plaine gângétique, les routes commerciales de la Soie et le réseau urbain autour de Pataliputra et Taxila.
            """);
        list.add(sMaurya);

        // --- SCÉNARIO : EMPIRE ROMAIN & PAX ROMANA (AN 0) ---
        Scenario sRoman = new Scenario();
        sRoman.setPresetKey("roman_empire");
        sRoman.setName("Empire Romain & Pax Romana (An 0)");
        sRoman.setStartDateYear(0);
        sRoman.setEndDateYear(476);
        sRoman.setInitialHumanCount(55000000);
        sRoman.setInitialCapitalPerCapita(350.0);
        sRoman.setInitialEnergyPerCapita(120.0);
        sRoman.setInitialFoodReserveMonths(6.0);
        sRoman.setInitialInformationPerCapita(400.0);
        sRoman.setPopulationDensityType("ROMAN_EMPIRE");
        sRoman.setTargetCohortSize(1000);
        sRoman.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sRoman.setClippingEnabled(true);
        sRoman.setMinLat(25.0); sRoman.setMaxLat(55.0); sRoman.setMinLng(-10.0); sRoman.setMaxLng(45.0);
        sRoman.setBoundaryMode("DYNAMIC_RESERVOIR");
        sRoman.getTypeBEngineStates().put("RomanImperialCliodynamicEngine", true);
        sRoman.getTypeBEngineStates().put("FrontierAsabiyyahEngine", true);
        sRoman.setDescription("""
            🏛️ SCÉNARIO HISTORIQUE : L'Empire Romain à son Apogée (Pax Romana, An 0)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE - SOURCES BESSES & BENCHMARKS CIA / SESHAT / HYDE]
            Modélise le bassin méditerranéen au moment de la Pax Romana sous Auguste. Intègre les données démographiques historiques (55 millions d'habitants), les réseaux d'infrastructures (viae, aqueducs) et les dynamiques cliodynamiques de Turchin.
            """);
        list.add(sRoman);

        Scenario s2 = new Scenario();
        s2.setPresetKey("late_antique_ice_age");
        s2.setName("Le Petit Âge Glaciaire de l'Antiquité Tardive & Peste de Justinien (536)");
        s2.setStartDateYear(536);
        s2.setEndDateYear(650);
        s2.setInitialHumanCount(180000000);
        s2.setInitialCapitalPerCapita(250.0);
        s2.setInitialEnergyPerCapita(100.0);
        s2.setInitialFoodReserveMonths(1.5);
        s2.setInitialInformationPerCapita(300.0);
        s2.setPopulationDensityType("URBAN_CLUSTERS");
        s2.setTargetCohortSize(1500);
        s2.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s2.setDescription("""
            🌋 SCÉNARIO HISTORIQUE : L'Anomalie Climatique Volcanique de 536 & Choc Sanitaire
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            L'année 536 est considérée par les historiens du climat comme "la pire année de l'histoire humaine". Deux éruptions volcaniques super-massives consécutives ont injecté un voile d'aérosols stratosphériques occultant le Soleil pendant 18 mois.
            """);
        list.add(s2);

        Scenario s4 = new Scenario();
        s4.setPresetKey("song_dynasty");
        s4.setName("Dynastie Song & Pré-Industrialisation Hydraulique (1000)");
        s4.setStartDateYear(1000);
        s4.setEndDateYear(1279);
        s4.setInitialHumanCount(100000000);
        s4.setInitialCapitalPerCapita(600.0);
        s4.setInitialEnergyPerCapita(500.0);
        s4.setInitialFoodReserveMonths(8.0);
        s4.setInitialInformationPerCapita(1200.0);
        s4.setPopulationDensityType("RIVER_VALLEYS");
        s4.setTargetCohortSize(2000);
        s4.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s4.setDescription("""
            🏮 SCÉNARIO HISTORIQUE : Le Siècle d'Or de la Dynastie Song (1000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            La Chine des Song a connu la première pré-industrialisation de l'histoire, avec une utilisation massive du charbon de terre pour la fonte du fer et des réseaux de transport fluviaux ultra-efficaces.
            """);
        list.add(s4);

        // --- SCÉNARIO : EMPIRE DU MALI (1324) ---
        Scenario sMali = new Scenario();
        sMali.setPresetKey("mali_empire");
        sMali.setName("Empire du Mali & Commerce Trans-Saharien (1324)");
        sMali.setStartDateYear(1324);
        sMali.setEndDateYear(1591);
        sMali.setInitialHumanCount(12000000);
        sMali.setInitialCapitalPerCapita(250.0);
        sMali.setInitialEnergyPerCapita(120.0);
        sMali.setInitialFoodReserveMonths(6.0);
        sMali.setInitialInformationPerCapita(400.0);
        sMali.setPopulationDensityType("WEST_AFRICA_MALI");
        sMali.setTargetCohortSize(500);
        sMali.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sMali.setClippingEnabled(true);
        sMali.setMinLat(5.0); sMali.setMaxLat(25.0); sMali.setMinLng(-18.0); sMali.setMaxLng(15.0);
        sMali.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMali.setDescription("""
            🕌 SCÉNARIO HISTORIQUE : L'Apogée de l'Empire du Mali sous Mansa Musa (1324 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise le réseau urbain et marchand trans-saharien de la boucle du Niger (Tombouctou, Gao, Djenné). Contrôle des mines d'or de Bambouk/Boure et des salines de Teghaza.
            """);
        list.add(sMali);

        // --- SCÉNARIO : AMÉRIQUES PRÉCOLOMBIENNES (1491) ---
        Scenario sAmericas1491 = new Scenario();
        sAmericas1491.setPresetKey("americas_1491");
        sAmericas1491.setName("Amériques Précolombiennes : Tawantinsuyu & Anahuac (1491)");
        sAmericas1491.setStartDateYear(1491);
        sAmericas1491.setEndDateYear(1650);
        sAmericas1491.setInitialHumanCount(60000000);
        sAmericas1491.setInitialCapitalPerCapita(220.0);
        sAmericas1491.setInitialEnergyPerCapita(150.0);
        sAmericas1491.setInitialFoodReserveMonths(6.0);
        sAmericas1491.setInitialInformationPerCapita(250.0);
        sAmericas1491.setPopulationDensityType("AMERICAS_1491");
        sAmericas1491.setTargetCohortSize(1000);
        sAmericas1491.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sAmericas1491.setClippingEnabled(true);
        sAmericas1491.setMinLat(-45.0); sAmericas1491.setMaxLat(30.0); sAmericas1491.setMinLng(-110.0); sAmericas1491.setMaxLng(-35.0);
        sAmericas1491.setBoundaryMode("DYNAMIC_RESERVOIR");
        sAmericas1491.setDescription("""
            🌽 SCÉNARIO HISTORIQUE : Les Amériques à la Veille du Contact (1491 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise les grands empires précolombiens (Empire Inca du Tawantinsuyu, Empire Aztèque de la Triple Alliance) et les sociétés Mississippiennes avant la rupture épidémique.
            """);
        list.add(sAmericas1491);

        // --- SCÉNARIO : CHOC DU CONTACT PRÉCOLOMBIEN (1492) ---
        Scenario sColumbian = new Scenario();
        sColumbian.setPresetKey("columbian_contact");
        sColumbian.setName("Arrivée des Européens aux Amériques & Choc Microbiens (1492)");
        sColumbian.setStartDateYear(1492);
        sColumbian.setEndDateYear(1650);
        sColumbian.setInitialHumanCount(60000000);
        sColumbian.setInitialCapitalPerCapita(250.0);
        sColumbian.setInitialEnergyPerCapita(160.0);
        sColumbian.setInitialFoodReserveMonths(5.0);
        sColumbian.setInitialInformationPerCapita(300.0);
        sColumbian.setPopulationDensityType("COLUMBIAN_CONTACT");
        sColumbian.setTargetCohortSize(1000);
        sColumbian.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sColumbian.setDescription("""
            ⛵ SCÉNARIO HISTORIQUE : Le Choc du Contact d'Échange Colombien & Effondrement Épidémique (1492)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'impact bio-démographique mondial de la rencontre entre l'Ancien et le Nouveau Monde. Trajectoire de choc microbiologique (chute démographique de 80-90% du continent américain) et réorganisation commerciale transatlantique.
            """);
        list.add(sColumbian);

        // --- SCÉNARIO : JAPON EDO & SAKOKU (1639) ---
        Scenario sSakoku = new Scenario();
        sSakoku.setPresetKey("tokugawa_japan");
        sSakoku.setName("Japon Tokugawa & Isolement Sakoku (1639)");
        sSakoku.setStartDateYear(1639);
        sSakoku.setEndDateYear(1853);
        sSakoku.setInitialHumanCount(27000000);
        sSakoku.setInitialCapitalPerCapita(450.0);
        sSakoku.setInitialEnergyPerCapita(200.0);
        sSakoku.setInitialFoodReserveMonths(8.0);
        sSakoku.setInitialInformationPerCapita(800.0);
        sSakoku.setPopulationDensityType("JAPAN_SAKOKU");
        sSakoku.setTargetCohortSize(500);
        sSakoku.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sSakoku.setClippingEnabled(true);
        sSakoku.setMinLat(30.0); sSakoku.setMaxLat(45.0); sSakoku.setMinLng(128.0); sSakoku.setMaxLng(146.0);
        sSakoku.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSakoku.setDescription("""
            ⛩️ SCÉNARIO HISTORIQUE : L'Ère d'Isolement Autarcique Tokugawa (Sakoku, 1639 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Fermeture des frontières de l'archipel japonais décrétée par le Shogunat Tokugawa. Modélise une économie circulaire hautement autarcique, l'urbanisation géante d'Edo (Tokyo, 1 million d'habitants) et l'absence d'intrants extérieurs jusqu'à l'arrivée des bateaux noirs du Commandant Perry en 1853.
            """);
        list.add(sSakoku);

        // --- SCÉNARIO : RÉVOLUTION INDUSTRIELLE (1800) ---
        Scenario sIndustrial1800 = new Scenario();
        sIndustrial1800.setPresetKey("industrial_1800");
        sIndustrial1800.setName("Révolution Industrielle & Transition Charbonnière (1800)");
        sIndustrial1800.setStartDateYear(1800);
        sIndustrial1800.setEndDateYear(1900);
        sIndustrial1800.setInitialHumanCount(900000000);
        sIndustrial1800.setInitialCapitalPerCapita(1200.0);
        sIndustrial1800.setInitialEnergyPerCapita(1500.0);
        sIndustrial1800.setInitialFoodReserveMonths(6.0);
        sIndustrial1800.setInitialInformationPerCapita(15000.0);
        sIndustrial1800.setPopulationDensityType("INDUSTRIAL_1800");
        sIndustrial1800.setTargetCohortSize(5000);
        sIndustrial1800.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sIndustrial1800.setDescription("""
            ⚙️ SCÉNARIO HISTORIQUE : La Machine à Vapeur & L'Émergence du Charbon (1800 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Basculement énergétique mondial du régime organique vers le régime minéral fossile (charbon de terre, machine à vapeur de Watt).
            """);
        list.add(sIndustrial1800);

        // --- SCÉNARIO : ANTHROPOCÈNE (2000) ---
        Scenario sModern2000 = new Scenario();
        sModern2000.setPresetKey("anthropocene_2000");
        sModern2000.setName("Anthropocène & Grande Accélération Mondiale (2000)");
        sModern2000.setStartDateYear(2000);
        sModern2000.setEndDateYear(2100);
        sModern2000.setInitialHumanCount(6127000000L);
        sModern2000.setInitialCapitalPerCapita(12000.0);
        sModern2000.setInitialEnergyPerCapita(20000.0);
        sModern2000.setInitialFoodReserveMonths(8.0);
        sModern2000.setInitialInformationPerCapita(2500000.0);
        sModern2000.setPopulationDensityType("URBAN_CLUSTERS");
        sModern2000.setTargetCohortSize(10000);
        sModern2000.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sModern2000.setDescription("""
            🌐 SCÉNARIO HISTORIQUE : L'Ère Numérique & La Grande Accélération (2000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Consolidation du système économique mondial interconnecté, essor des microprocesseurs en silicium, de l'Internet mondial et de l'urbanisation globale.
            """);
        list.add(sModern2000);

        // --- SCÉNARIOS DU FUTUR ---
        Scenario s5 = new Scenario();
        s5.setPresetKey("ssp5_85");
        s5.setName("Business As Usual : Fossil Fuel Reliance & Warming (SSP5-8.5)");
        s5.setStartDateYear(2026);
        s5.setEndDateYear(2100);
        s5.setInitialHumanCount(8200000000L);
        s5.setInitialCapitalPerCapita(15000.0);
        s5.setInitialEnergyPerCapita(25000.0);
        s5.setInitialFoodReserveMonths(9.0);
        s5.setInitialInformationPerCapita(5000000.0);
        s5.setPopulationDensityType("URBAN_CLUSTERS");
        s5.setTargetCohortSize(10000);
        s5.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s5.setDescription("""
            📉 SCÉNARIO FUTUR : Business As Usual (Trajectoire GIEC SSP5-8.5)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Poursuite de l'extraction des combustibles fossiles traditionnels sans déploiement massif de la fusion ni captage du carbone.
            """);
        list.add(s5);

        Scenario s6 = new Scenario();
        s6.setPresetKey("singularity_2045");
        s6.setName("Singularité Technologique, ASI & Fusion D-T (2045)");
        s6.setStartDateYear(2045);
        s6.setEndDateYear(2100);
        s6.setInitialHumanCount(9000000000L);
        s6.setInitialCapitalPerCapita(50000.0);
        s6.setInitialEnergyPerCapita(100000.0);
        s6.setInitialFoodReserveMonths(24.0);
        s6.setInitialInformationPerCapita(100000000.0);
        s6.setPopulationDensityType("URBAN_CLUSTERS");
        s6.setTargetCohortSize(10000);
        s6.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s6.setDescription("""
            🤖 SCÉNARIO FUTUR : Singularité Technologique & Énergie de Fusion D-T
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Franchissement du seuil d'émergence d'une Super-Intelligence Artificielle (ASI) et maîtrise industrielle de la fusion nucléaire deutérium-tritium.
            """);
        list.add(s6);

        Scenario s7 = new Scenario();
        s7.setPresetKey("nuclear_winter_2035");
        s7.setName("Hiver Nucléaire & Ombre Stratosphérique (2035)");
        s7.setStartDateYear(2035);
        s7.setEndDateYear(2085);
        s7.setInitialHumanCount(8500000000L);
        s7.setInitialCapitalPerCapita(18000.0);
        s7.setInitialEnergyPerCapita(1500.0);
        s7.setInitialFoodReserveMonths(1.5);
        s7.setInitialInformationPerCapita(500000.0);
        s7.setPopulationDensityType("URBAN_CLUSTERS");
        s7.setTargetCohortSize(250);
        s7.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s7.setDescription("""
            ☢️ SCÉNARIO FUTUR : Catastrophe de la Guerre Nucléaire & Hiver Stratosphérique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Conflit nucléaire à haute intensité déclenchant d'immenses tempêtes de feu urbaines et l'injection massive de carbone suie dans la stratosphère.
            """);
        list.add(s7);

        Scenario s8 = new Scenario();
        s8.setPresetKey("peak_phosphate_2050");
        s8.setName("Falaise du Phosphate Minéral & Crise N-P-K (2050)");
        s8.setStartDateYear(2050);
        s8.setEndDateYear(2150);
        s8.setInitialHumanCount(9500000000L);
        s8.setInitialCapitalPerCapita(22000.0);
        s8.setInitialEnergyPerCapita(12000.0);
        s8.setInitialFoodReserveMonths(4.0);
        s8.setInitialInformationPerCapita(2000000.0);
        s8.setPopulationDensityType("URBAN_CLUSTERS");
        s8.setTargetCohortSize(5000);
        s8.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s8.setDescription("""
            ⛏️ SCÉNARIO FUTUR : Épuisement du Phosphate de Roche (Peak P 2050)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Épuisement géologique complet des gisements de phosphate de roche bon marché sans transition vers un recyclage circulaire intégral.
            """);
        list.add(s8);

        Scenario s9 = new Scenario();
        s9.setPresetKey("supervolcano_2060");
        s9.setName("Super-Éruption Volcanique Toba/Yellowstone (2060)");
        s9.setStartDateYear(2060);
        s9.setEndDateYear(2110);
        s9.setInitialHumanCount(9800000000L);
        s9.setInitialCapitalPerCapita(25000.0);
        s9.setInitialEnergyPerCapita(20000.0);
        s9.setInitialFoodReserveMonths(3.0);
        s9.setInitialInformationPerCapita(5000000.0);
        s9.setPopulationDensityType("URBAN_CLUSTERS");
        s9.setTargetCohortSize(250);
        s9.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        s9.setDescription("""
            🌋 SCÉNARIO FUTUR : Super-Volcan VEI-8 & Refroidissement Vulcanologique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T_0)]
            Éruption super-volcanique de degré VEI-8 éjectant plus de 1000 km³ de cendres et de dioxyde de soufre (SO2) dans la haute atmosphère.
            
            [CONDITIONS INITIALES PHYSIQUES (T_0)]
            • Stock Capital Physique (K₀) : 25 000 kg/habitant (infrastructures avancées et serres automatisées).
            • Énergie Stockée (E₀) : 20 000 MJ/habitant (centrales nucléaires et géothermiques).
            • Réserves Alimentaires (F₀) : 3.0 mois (destructions agricoles par cendres).
            • Savoir Archivé (I₀) : 5 000 000 bits/habitant (savoir automatisé & archives).
            """);
        list.add(s9);

        return list;
    }
}
