/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.persistence.EtherJsonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class Scenario implements Serializable {
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

    // Cultural Vector & Multi-Field Layers (Persisted per Scenario)
    private int cultureVectorDimensions = 9;
    private double culturalDiffusionRate = 0.05;
    private double culturalMutationRate = 0.01;
    private List<String> customTensorMapsBase64 = new ArrayList<>();
    private List<Boolean> tensorProceduralModes = new ArrayList<>();
    private Map<Integer, Long> tensorSeeds = new HashMap<>();
    private Map<Integer, Map<String, Double>> tensorProceduralParameters = new HashMap<>();
    private Map<Integer, String> customTensorNames = new HashMap<>();
    private Map<String, Double> culturalAffinityMatrix = new HashMap<>();

    // Geological & Mineral Energy Extensible Tensor Layers (Persisted per Scenario)
    private int resourceVectorDimensions = 10;
    private List<String> customGeologyTensorMapsBase64 = new ArrayList<>();
    private List<Boolean> geologyTensorProceduralModes = new ArrayList<>();

    // Engine Optimization & Determinism Controls
    private int h3Resolution = 3;
    private boolean strictDeterminism = true;
    private boolean sparseCellSkippingEnabled = false;
    private boolean oceanMacroAggregationEnabled = false;
    private boolean coastalNavigationOnlyEnabled = false;
    private boolean oceanMultiRateTickingEnabled = false;
    private boolean parallelExecutionEnabled = false;
    private boolean spatialRangeTruncationEnabled = false;
    private int climateTickFrequency = 5;
    private int parallelThreadCount = 0;
    private boolean tier2CliodynamicsEnabled = true;

    // Type B Procedural & Cliodynamic Engine Checkbox States & Parameters
    private Map<String, Boolean> typeBEngineStates = new HashMap<>();
    private Map<String, Map<String, Double>> typeBEngineParameters = new HashMap<>();

    // Scheduled Climate and Planetary Cataclysm Events
    private List<ClimateEvent> climateEvents = new ArrayList<>();

    // Spatial Clipping & Boundary Conditions
    private boolean clippingEnabled = false;
    private double minLat = -90.0;
    private double maxLat = 90.0;
    private double minLng = -180.0;
    private double maxLng = 180.0;
    private String boundaryMode = "DYNAMIC_RESERVOIR";

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

    /**
     * Creates a default scenario instance.
     *
     * @return new default {@link Scenario}
     */
    public static Scenario createDefaultScenario() {
        return new Scenario();
    }

    /**
     * Creates the official reference Benchmark Standard Scenario.
     *
     * @return standardized benchmark scenario instance
     */
    public static Scenario createBenchmarkStandardScenario() {
        for (Scenario s : getBuiltInScenarios()) {
            if ("benchmark_standard".equals(s.getPresetKey()) || "out_of_africa".equals(s.getPresetKey())) {
                return s.copy();
            }
        }
        return createDefaultScenario();
    }

    /**
     * Returns all canonical built-in historical, future, and exoplanetary scenarios.
     *
     * @return chronologically sorted list of built-in scenarios
     */
    public static List<Scenario> getBuiltInScenarios() {
        return FutureAndExoplanetScenarioCatalog.getBuiltInScenarios();
    }

    /**
     * Resolves the canonical preset key for this scenario.
     *
     * @return normalized preset key string
     */
    public String resolvePresetKey() {
        if (presetKey != null && !presetKey.isBlank()) {
            return presetKey.trim();
        }
        if (name != null && !name.isBlank()) {
            return org.ether.society.persistence.PresetStorageService.slugify(name);
        }
        return "unnamed_scenario";
    }

    /**
     * Returns the canonical reference trajectory model key (e.g. {@code "PALEO_SOURCES"},
     * {@code "HYDE_MADDISON"}, {@code "SSP1"}, {@code "SSP2"}, {@code "SSP3"}, {@code "SSP4"},
     * {@code "SSP5"}, {@code "ETHER_CALIBRATED"}).
     *
     * @return the reference model identifier
     */
    public String getReferenceTrajectoryModel() {
        if (referenceTrajectoryModel != null && !referenceTrajectoryModel.isBlank()) {
            return referenceTrajectoryModel;
        }
        if (startDateYear < -10000) {
            return "PALEO_SOURCES";
        }
        if (startDateYear < 2025) {
            return "HYDE_MADDISON";
        }
        return "SSP2";
    }

    /**
     * Sets the reference trajectory model key.
     *
     * @param referenceTrajectoryModel reference model identifier
     */
    public void setReferenceTrajectoryModel(String referenceTrajectoryModel) {
        this.referenceTrajectoryModel = referenceTrajectoryModel;
    }

    /**
     * Returns the localized display name for this scenario.
     *
     * @return localized scenario title
     */
    public String getDisplayName() {
        String key = resolvePresetKey();
        if (key != null && !key.isBlank()) {
            String localized = I18n.getOrDefault("scenario.preset." + key + ".name", name);
            if (localized != null && !localized.isBlank()) {
                return localized;
            }
        }
        return name != null ? name : "Unnamed Scenario";
    }

    /**
     * Returns the localized description for this scenario.
     *
     * @return localized scenario description
     */
    public String getDisplayDescription() {
        String key = resolvePresetKey();
        if (key != null && !key.isBlank()) {
            String localized = I18n.getOrDefault("scenario.preset." + key + ".desc", description);
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

    /**
     * Creates a deep copy of this scenario via JSON serialization or field-by-field fallback.
     *
     * @return deep copy of this {@link Scenario}
     */
    public Scenario copy() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = EtherJsonUtil.getMapper();
            String json = mapper.writeValueAsString(this);
            return mapper.readValue(json, Scenario.class);
        } catch (Exception e) {
            Scenario copy = new Scenario();
            copy.name = this.name;
            copy.presetKey = this.presetKey;
            copy.referenceTrajectoryModel = this.referenceTrajectoryModel;
            copy.description = this.description;
            copy.planetPreset = this.planetPreset;
            copy.ecologyPreset = this.ecologyPreset;
            copy.ecologyPresetName = this.ecologyPresetName;
            copy.planetRadiusKm = this.planetRadiusKm;
            copy.rotationPeriodHours = this.rotationPeriodHours;
            copy.revolutionPeriodDays = this.revolutionPeriodDays;
            copy.axialTiltDegrees = this.axialTiltDegrees;
            copy.initialHumanCount = this.initialHumanCount;
            copy.initialTechLevel = this.initialTechLevel;
            copy.techPreset = this.techPreset;
            copy.initialCapitalPerCapita = this.initialCapitalPerCapita;
            copy.initialEnergyPerCapita = this.initialEnergyPerCapita;
            copy.initialFoodReserveMonths = this.initialFoodReserveMonths;
            copy.initialInformationPerCapita = this.initialInformationPerCapita;
            copy.populationDensityType = this.populationDensityType;
            copy.cellSizeKm2 = this.cellSizeKm2;
            copy.targetCohortSize = this.targetCohortSize;
            copy.temporalResolutionDays = this.temporalResolutionDays;
            copy.climateHarshness = this.climateHarshness;
            copy.startDateYear = this.startDateYear;
            copy.endDateYear = this.endDateYear;
            copy.seed = this.seed;
            copy.culturalSeed = this.culturalSeed;
            copy.randomEventsEnabled = this.randomEventsEnabled;
            copy.earthHistoricalLeadersEnabled = this.earthHistoricalLeadersEnabled;
            copy.proceduralLeadersEnabled = this.proceduralLeadersEnabled;
            copy.customDensityBase64 = this.customDensityBase64;
            copy.cultureVectorDimensions = this.cultureVectorDimensions;
            copy.culturalDiffusionRate = this.culturalDiffusionRate;
            copy.culturalMutationRate = this.culturalMutationRate;
            copy.customTensorMapsBase64 = new ArrayList<>(this.customTensorMapsBase64);
            copy.tensorProceduralModes = new ArrayList<>(this.tensorProceduralModes);
            copy.tensorSeeds = new HashMap<>(this.tensorSeeds);
            copy.tensorProceduralParameters = new HashMap<>(this.tensorProceduralParameters);
            copy.customTensorNames = new HashMap<>(this.customTensorNames);
            copy.culturalAffinityMatrix = new HashMap<>(this.culturalAffinityMatrix);
            copy.resourceVectorDimensions = this.resourceVectorDimensions;
            copy.customGeologyTensorMapsBase64 = new ArrayList<>(this.customGeologyTensorMapsBase64);
            copy.geologyTensorProceduralModes = new ArrayList<>(this.geologyTensorProceduralModes);
            copy.h3Resolution = this.h3Resolution;
            copy.strictDeterminism = this.strictDeterminism;
            copy.sparseCellSkippingEnabled = this.sparseCellSkippingEnabled;
            copy.oceanMacroAggregationEnabled = this.oceanMacroAggregationEnabled;
            copy.coastalNavigationOnlyEnabled = this.coastalNavigationOnlyEnabled;
            copy.oceanMultiRateTickingEnabled = this.oceanMultiRateTickingEnabled;
            copy.parallelExecutionEnabled = this.parallelExecutionEnabled;
            copy.spatialRangeTruncationEnabled = this.spatialRangeTruncationEnabled;
            copy.typeBEngineStates = new HashMap<>(this.typeBEngineStates);
            copy.typeBEngineParameters = new HashMap<>(this.typeBEngineParameters);
            copy.climateEvents = new ArrayList<>(this.climateEvents);
            copy.clippingEnabled = this.clippingEnabled;
            copy.minLat = this.minLat;
            copy.maxLat = this.maxLat;
            copy.minLng = this.minLng;
            copy.maxLng = this.maxLng;
            copy.boundaryMode = this.boundaryMode;
            copy.useRealEarthData = this.useRealEarthData;
            return copy;
        }
    }

    /**
     * Calculates the default number of simulation ticks required to run this scenario
     * from {@code startDateYear} to {@code endDateYear}:
     * $$N_{\text{ticks}} = \text{clamp}\left(\Delta Y \cdot \frac{365.25}{\Delta t_{\text{days}}}, 100, 10^6\right)$$
     *
     * @return clamped total tick count
     */
    public int calculateScenarioTicks() {
        long durationYears = Math.max(1, endDateYear - startDateYear);
        double ticksPerYear = 365.25 / Math.max(1.0, temporalResolutionDays);
        return (int) Math.clamp((long) (durationYears * ticksPerYear), 100L, 1000000L);
    }

    /**
     * Converts this scenario's optimization and determinism settings into a runtime {@link SimulationPerformanceConfig}.
     *
     * @return configured {@link SimulationPerformanceConfig}
     */
    public SimulationPerformanceConfig toPerformanceConfig() {
        SimulationPerformanceConfig config = new SimulationPerformanceConfig();
        config.setStrictDeterminism(strictDeterminism);
        config.setEnableSparseCellSkipping(!strictDeterminism && sparseCellSkippingEnabled);
        config.setEnableOceanMacroAggregation(!strictDeterminism && oceanMacroAggregationEnabled);
        config.setEnableCoastalNavigationOnly(!strictDeterminism && coastalNavigationOnlyEnabled);
        config.setEnableMultiFreqClimateTicks(!strictDeterminism && oceanMultiRateTickingEnabled);
        config.setClimateTickFrequency(strictDeterminism ? 1 : climateTickFrequency);
        config.setEnableParallelExecution(!strictDeterminism && parallelExecutionEnabled);
        config.setParallelThreadCount(parallelExecutionEnabled ? parallelThreadCount : 1);
        config.setEnableSpatialRangeTruncation(!strictDeterminism && spatialRangeTruncationEnabled);
        config.setEnableTier2Cliodynamics(tier2CliodynamicsEnabled);
        return config;
    }

    /**
     * Returns the scenario description, generating a formatted physical summary if unset.
     *
     * @return scenario description text
     */
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

    // --- Standard Getters and Setters ---

    public String getPresetKey() { return presetKey; }
    public void setPresetKey(String presetKey) { this.presetKey = presetKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void setDescription(String description) { this.description = description; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getH3Resolution() { return h3Resolution > 0 ? h3Resolution : 3; }
    public void setH3Resolution(int h3Resolution) { this.h3Resolution = h3Resolution; }

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

    public boolean isClippingEnabled() { return clippingEnabled; }
    public void setClippingEnabled(boolean clippingEnabled) { this.clippingEnabled = clippingEnabled; }

    public double getMinLat() { return minLat; }
    public void setMinLat(double minLat) { this.minLat = minLat; }

    public double getMaxLat() { return maxLat; }
    public void setMaxLat(double maxLat) { this.maxLat = maxLat; }

    public double getMinLng() { return minLng; }
    public void setMinLng(double minLng) { this.minLng = minLng; }

    public double getMaxLng() { return maxLng; }
    public void setMaxLng(double maxLng) { this.maxLng = maxLng; }

    public String getBoundaryMode() { return boundaryMode; }
    public void setBoundaryMode(String boundaryMode) { this.boundaryMode = boundaryMode; }

    public boolean isStrictDeterminism() { return strictDeterminism; }
    public void setStrictDeterminism(boolean strictDeterminism) {
        this.strictDeterminism = strictDeterminism;
        if (strictDeterminism) {
            this.sparseCellSkippingEnabled = false;
            this.oceanMultiRateTickingEnabled = false;
            this.parallelExecutionEnabled = false;
            this.spatialRangeTruncationEnabled = false;
        }
    }

    public boolean isSparseCellSkippingEnabled() { return sparseCellSkippingEnabled; }
    public void setSparseCellSkippingEnabled(boolean sparseCellSkippingEnabled) {
        this.sparseCellSkippingEnabled = sparseCellSkippingEnabled;
        if (sparseCellSkippingEnabled) {
            this.strictDeterminism = false;
        }
    }

    public boolean isOceanMacroAggregationEnabled() { return oceanMacroAggregationEnabled; }
    public void setOceanMacroAggregationEnabled(boolean oceanMacroAggregationEnabled) { this.oceanMacroAggregationEnabled = oceanMacroAggregationEnabled; }

    public boolean isCoastalNavigationOnlyEnabled() { return coastalNavigationOnlyEnabled; }
    public void setCoastalNavigationOnlyEnabled(boolean coastalNavigationOnlyEnabled) { this.coastalNavigationOnlyEnabled = coastalNavigationOnlyEnabled; }

    public boolean isOceanMultiRateTickingEnabled() { return oceanMultiRateTickingEnabled; }
    public void setOceanMultiRateTickingEnabled(boolean oceanMultiRateTickingEnabled) {
        this.oceanMultiRateTickingEnabled = oceanMultiRateTickingEnabled;
        if (oceanMultiRateTickingEnabled) {
            this.strictDeterminism = false;
        }
    }

    public boolean isParallelExecutionEnabled() { return parallelExecutionEnabled; }
    public void setParallelExecutionEnabled(boolean parallelExecutionEnabled) {
        this.parallelExecutionEnabled = parallelExecutionEnabled;
        if (parallelExecutionEnabled) {
            this.strictDeterminism = false;
        }
    }

    public boolean isSpatialRangeTruncationEnabled() { return spatialRangeTruncationEnabled; }
    public void setSpatialRangeTruncationEnabled(boolean spatialRangeTruncationEnabled) {
        this.spatialRangeTruncationEnabled = spatialRangeTruncationEnabled;
        if (spatialRangeTruncationEnabled) {
            this.strictDeterminism = false;
        }
    }

    public int getClimateTickFrequency() { return climateTickFrequency; }
    public void setClimateTickFrequency(int climateTickFrequency) { this.climateTickFrequency = Math.max(1, climateTickFrequency); }

    public int getParallelThreadCount() { return parallelThreadCount; }
    public void setParallelThreadCount(int parallelThreadCount) { this.parallelThreadCount = Math.max(0, parallelThreadCount); }

    public boolean isTier2CliodynamicsEnabled() { return tier2CliodynamicsEnabled; }
    public void setTier2CliodynamicsEnabled(boolean tier2CliodynamicsEnabled) { this.tier2CliodynamicsEnabled = tier2CliodynamicsEnabled; }

    public double getTemporalResolutionDays() { return temporalResolutionDays; }
    public void setTemporalResolutionDays(double temporalResolutionDays) { this.temporalResolutionDays = temporalResolutionDays; }

    public Map<String, Boolean> getTypeBEngineStates() {
        if (typeBEngineStates == null) typeBEngineStates = new HashMap<>();
        return typeBEngineStates;
    }
    public void setTypeBEngineStates(Map<String, Boolean> typeBEngineStates) {
        this.typeBEngineStates = typeBEngineStates != null ? typeBEngineStates : new HashMap<>();
    }

    public Map<String, Map<String, Double>> getTypeBEngineParameters() {
        if (typeBEngineParameters == null) typeBEngineParameters = new HashMap<>();
        return typeBEngineParameters;
    }
    public void setTypeBEngineParameters(Map<String, Map<String, Double>> typeBEngineParameters) {
        this.typeBEngineParameters = typeBEngineParameters != null ? typeBEngineParameters : new HashMap<>();
    }

    public int getCultureVectorDimensions() { return cultureVectorDimensions; }
    public void setCultureVectorDimensions(int cultureVectorDimensions) { this.cultureVectorDimensions = cultureVectorDimensions; }

    public Map<Integer, String> getCustomTensorNames() { return customTensorNames; }
    public void setCustomTensorNames(Map<Integer, String> customTensorNames) {
        this.customTensorNames = customTensorNames != null ? customTensorNames : new HashMap<>();
    }

    public String getCustomTensorName(int index) {
        return customTensorNames != null ? customTensorNames.get(index) : null;
    }
    public void setCustomTensorName(int index, String name) {
        if (this.customTensorNames == null) this.customTensorNames = new HashMap<>();
        if (name == null || name.isBlank()) {
            this.customTensorNames.remove(index);
        } else {
            this.customTensorNames.put(index, name.trim());
        }
    }

    /**
     * Gets the custom cliodynamic pairwise cultural affinity matrix overrides ($A_{i,j} \in [0.0, 100.0]$).
     * Keys are formatted as {@code "idA:idB"}.
     *
     * @return map of affinity overrides
     */
    public Map<String, Double> getCulturalAffinityMatrix() {
        if (culturalAffinityMatrix == null) culturalAffinityMatrix = new HashMap<>();
        return culturalAffinityMatrix;
    }
    public void setCulturalAffinityMatrix(Map<String, Double> culturalAffinityMatrix) {
        this.culturalAffinityMatrix = culturalAffinityMatrix != null ? new HashMap<>(culturalAffinityMatrix) : new HashMap<>();
    }

    /**
     * Gets pairwise cultural affinity between two polities, defaulting to $50.0$ (neutral coexistence) if unspecified.
     *
     * @param idA first polity or cultural entity identifier
     * @param idB second polity or cultural entity identifier
     * @return affinity value in $[0.0, 100.0]$
     */
    public double getPairwiseAffinity(String idA, String idB) {
        if (idA == null || idB == null) return 50.0;
        if (idA.equalsIgnoreCase(idB)) return 100.0;
        String key1 = idA + ":" + idB;
        String key2 = idB + ":" + idA;
        if (culturalAffinityMatrix != null) {
            if (culturalAffinityMatrix.containsKey(key1)) return culturalAffinityMatrix.get(key1);
            if (culturalAffinityMatrix.containsKey(key2)) return culturalAffinityMatrix.get(key2);
        }
        return 50.0;
    }

    public double getCulturalDiffusionRate() { return culturalDiffusionRate; }
    public void setCulturalDiffusionRate(double culturalDiffusionRate) { this.culturalDiffusionRate = culturalDiffusionRate; }

    public double getCulturalMutationRate() { return culturalMutationRate; }
    public void setCulturalMutationRate(double culturalMutationRate) { this.culturalMutationRate = culturalMutationRate; }

    public List<String> getCustomTensorMapsBase64() {
        if (customTensorMapsBase64 == null) customTensorMapsBase64 = new ArrayList<>();
        return customTensorMapsBase64;
    }
    public void setCustomTensorMapsBase64(List<String> customTensorMapsBase64) {
        this.customTensorMapsBase64 = customTensorMapsBase64 != null ? customTensorMapsBase64 : new ArrayList<>();
    }

    public List<Boolean> getTensorProceduralModes() {
        if (tensorProceduralModes == null) tensorProceduralModes = new ArrayList<>();
        return tensorProceduralModes;
    }
    public void setTensorProceduralModes(List<Boolean> tensorProceduralModes) {
        this.tensorProceduralModes = tensorProceduralModes != null ? tensorProceduralModes : new ArrayList<>();
    }

    public String getCustomTensorMapBase64(int index) {
        List<String> list = getCustomTensorMapsBase64();
        if (index >= 0 && index < list.size() && list.get(index) != null && !list.get(index).isBlank()) {
            return list.get(index);
        }
        return null;
    }
    public void setCustomTensorMapBase64(int index, String base64) {
        List<String> list = getCustomTensorMapsBase64();
        while (list.size() <= index) list.add(null);
        list.set(index, base64);
    }

    public Map<Integer, Long> getTensorSeeds() {
        if (tensorSeeds == null) tensorSeeds = new HashMap<>();
        return tensorSeeds;
    }
    public void setTensorSeeds(Map<Integer, Long> tensorSeeds) {
        this.tensorSeeds = tensorSeeds != null ? tensorSeeds : new HashMap<>();
    }

    public Map<Integer, Map<String, Double>> getTensorProceduralParameters() {
        if (tensorProceduralParameters == null) tensorProceduralParameters = new HashMap<>();
        return tensorProceduralParameters;
    }
    public void setTensorProceduralParameters(Map<Integer, Map<String, Double>> tensorProceduralParameters) {
        this.tensorProceduralParameters = tensorProceduralParameters != null ? tensorProceduralParameters : new HashMap<>();
    }

    public int getResourceVectorDimensions() { return resourceVectorDimensions; }
    public void setResourceVectorDimensions(int resourceVectorDimensions) { this.resourceVectorDimensions = resourceVectorDimensions; }

    public List<String> getCustomGeologyTensorMapsBase64() {
        if (customGeologyTensorMapsBase64 == null) customGeologyTensorMapsBase64 = new ArrayList<>();
        return customGeologyTensorMapsBase64;
    }
    public void setCustomGeologyTensorMapsBase64(List<String> customGeologyTensorMapsBase64) {
        this.customGeologyTensorMapsBase64 = customGeologyTensorMapsBase64 != null ? customGeologyTensorMapsBase64 : new ArrayList<>();
    }

    public List<Boolean> getGeologyTensorProceduralModes() {
        if (geologyTensorProceduralModes == null) geologyTensorProceduralModes = new ArrayList<>();
        return geologyTensorProceduralModes;
    }
    public void setGeologyTensorProceduralModes(List<Boolean> geologyTensorProceduralModes) {
        this.geologyTensorProceduralModes = geologyTensorProceduralModes != null ? geologyTensorProceduralModes : new ArrayList<>();
    }

    public String getCustomGeologyTensorMapBase64(int index) {
        List<String> list = getCustomGeologyTensorMapsBase64();
        if (index >= 0 && index < list.size() && list.get(index) != null && !list.get(index).isBlank()) {
            return list.get(index);
        }
        return null;
    }
    public void setCustomGeologyTensorMapBase64(int index, String base64) {
        List<String> list = getCustomGeologyTensorMapsBase64();
        while (list.size() <= index) list.add(null);
        list.set(index, base64);
    }

    public List<ClimateEvent> getClimateEvents() { return climateEvents; }
    public void setClimateEvents(List<ClimateEvent> climateEvents) {
        this.climateEvents = climateEvents != null ? climateEvents : new ArrayList<>();
    }
}
