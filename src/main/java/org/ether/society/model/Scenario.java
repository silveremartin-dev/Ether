/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.generation.PlanetPreset;
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
        String key = resolvePresetKey();
        if (key != null && !key.isBlank()) {
            String localized = org.ether.society.i18n.I18n.getOrDefault("scenario.preset." + key + ".name", name);
            if (localized != null && !localized.isBlank()) {
                return localized;
            }
        }
        return name != null ? name : "Unnamed Scenario";
    }

    public String getDisplayDescription() {
        String key = resolvePresetKey();
        if (key != null && !key.isBlank()) {
            String localized = org.ether.society.i18n.I18n.getOrDefault("scenario.preset." + key + ".desc", description);
            if (localized != null && !localized.isBlank()) {
                return localized;
            }
        }
        return description != null ? description : "";
    }

    private String resolvePresetKey() {
        if (presetKey != null && !presetKey.isBlank()) return presetKey;
        if (name == null) return null;
        String n = name.toLowerCase();
        if (n.contains("out of africa") || n.contains("sortie d'afrique")) return "out_of_africa";
        if (n.contains("toba")) return "toba_cataclysm_74k";
        if (n.contains("sahul")) return "sahul";
        if (n.contains("beringia") || n.contains("bÃ©ringie")) return "beringia";
        if (n.contains("solutrean") || n.contains("solutrÃ©en") || n.contains("lgm")) return "lgm_solutrean";
        if (n.contains("dryas")) return "younger_dryas";
        if (n.contains("fertile crescent") || n.contains("croissant fertile")) return "fertile_crescent";
        if (n.contains("vert") || n.contains("green sahara")) return "green_sahara";
        if (n.contains("egypte") || n.contains("Ã©gypte") || n.contains("egypt")) return "ancient_egypt";
        if (n.contains("assyrian") || n.contains("assyrien")) return "assyrian_empire";
        if (n.contains("mesoamerica") || n.contains("mÃ©soamÃ©rique") || n.contains("olmeques") || n.contains("olmÃ¨ques")) return "mesoamerica";
        if (n.contains("bronze age collapse") || (n.contains("effondrement") && n.contains("bronze"))) return "bronze_age_collapse_1200bc";
        if (n.contains("early iron age") || n.contains("premier age du fer") || n.contains("premier Ã¢ge du fer")) return "early_iron_age";
        if (n.contains("alexander") || n.contains("alexandre")) return "alexander_hellenistic_334bc";
        if (n.contains("maurya")) return "maurya_empire";
        if (n.contains("roman empire") || n.contains("empire romain")) return "roman_empire";
        if (n.contains("late antique") || n.contains("glaciaire antique") || n.contains("536")) return "late_antique_ice_age";
        if (n.contains("islamic") || n.contains("islamique")) return "islamic_expansion_632";
        if (n.contains("song")) return "song_dynasty";
        if (n.contains("mongol")) return "mongol_conquest_1206";
        if (n.contains("mali")) return "mali_empire";
        if (n.contains("black death") || n.contains("peste noire")) return "black_death_1347";
        if (n.contains("1491") || n.contains("americas") || n.contains("amÃ©riques")) return "americas_1491";
        if (n.contains("columbian") || n.contains("colombien")) return "columbian_contact";
        if (n.contains("sakoku") || n.contains("tokugawa")) return "tokugawa_japan";
        if (n.contains("industrial") || n.contains("industrielle")) return "industrial_1800";
        if (n.contains("totalitarian") || n.contains("totalitaire") || n.contains("1914")) return "world_wars_totalitarian_1914";
        if (n.contains("anthropocene") || n.contains("anthropocÃ¨ne") || n.contains("2000")) return "anthropocene_2000";
        if (n.contains("ssp5") || n.contains("ssp5-8.5")) return "ssp5_85";
        if (n.contains("hiver nucleaire") || n.contains("hiver nuclÃ©aire") || n.contains("nuclear winter")) return "nuclear_winter_2035";
        if (n.contains("singularity") || n.contains("singularite") || n.contains("singularité")) return "singularity_2045";
        if (n.contains("phosphate")) return "peak_phosphate_2050";
        if (n.contains("supervolcan") || n.contains("supervolcano")) return "supervolcano_2060";
        if (n.contains("mars")) return "mars_colony_2050";
        if (n.contains("shackleton") || n.contains("marius") || n.contains("lune") || n.contains("moon")) return "moon_shackleton_2050";
        if (n.contains("venus") || n.contains("vénus") || n.contains("aerostat") || n.contains("aérostat") || n.contains("hesperos")) return "venus_cloud_cities_2060";
        if (n.contains("mercure") || n.contains("mercury") || n.contains("caloris") || n.contains("hermes")) return "mercury_caloris_forge_2070";
        if (n.contains("titan") || n.contains("kraken")) return "titan_cryo_methane_2080";
        if (n.contains("super-terre") || n.contains("super-earth") || n.contains("gaia")) return "super_earth_gaia_2100";
        if (n.contains("synchrone") || n.contains("eyeball") || n.contains("crépuscule") || n.contains("twilight")) return "eyeball_world_twilight_2120";
        if (n.contains("oceania") || n.contains("aquapolis") || n.contains("monde océan") || n.contains("water world")) return "oceania_aquapolis_2090";
        if (n.contains("boreas") || n.contains("glaciaire") || n.contains("ice world") || n.contains("subglacial")) return "boreas_subglacial_2075";
        if (n.contains("archipel") || n.contains("archipelago") || n.contains("seasteading")) return "archipelago_seasteading_2055";
        return null;
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
        AUTO_FROM_YEAR("â³ Automatique (CalculÃ© selon l'annÃ©e Tâ‚€)", -1, -1, -1, -1),
        PALEOLITHIC("ðŸ¹ Chasseurs-Cueilleurs / NÃ©olithique", 5.0, 10.0, 2.0, 5.0),
        NEOLITHIC_BRONZE("ðŸ›¡ï¸ Ã‚ge du Bronze & CitÃ©s-Ã‰tats", 25.0, 30.0, 4.0, 40.0),
        ANTIQUITY("ðŸ›ï¸ AntiquitÃ© Classique & Empire", 100.0, 60.0, 6.0, 200.0),
        RENAISSANCE("â›µ Renaissance & Imprimerie", 500.0, 300.0, 8.0, 2000.0),
        INDUSTRIAL("âš™ï¸ RÃ©volution Industrielle & Vapeur", 2500.0, 2500.0, 10.0, 15000.0),
        CONTEMPORARY("ðŸŒ Contemporain & MÃ©tropole NumÃ©rique", 15000.0, 50000.0, 18.0, 5000000.0),
        SPACE_COLONY_MARS("ðŸš€ Colonie Spatiale / Mars (Faible Pop / Ultra High-Tech)", 50000.0, 200000.0, 24.0, 50000000.0),
        CUSTOM("âš™ï¸ PersonnalisÃ© (Saisie Libre des 4 Stocks)", -1, -1, -1, -1);

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
    private double temporalResolutionDays = 30.0; // Temporal resolution time step Î”t in days (default: 30.0 days = 1 month)
    private double climateHarshness; // 0.0 to 1.0 (storms, droughts)
    private long startDateYear; // e.g. -100000
    private long endDateYear = 100; // e.g. 100
    private long seed = 12345L; // Demographic density seed
    private long culturalSeed = 54321L; // Cultural tensor suite seed
    private boolean randomEventsEnabled = true;
    private boolean earthHistoricalLeadersEnabled = true;
    private boolean proceduralLeadersEnabled = true;
    private String customDensityBase64;
    // Cultural Vector & Multi-Field Layers (Persisted per Scenario)
    private int cultureVectorDimensions = 9; // 4D to 32D culture vector dimensions (9D baseline on Earth)
    private double culturalDiffusionRate = 0.05; // Free-energy cultural diffusion conductance
    private double culturalMutationRate = 0.01; // Mutation & innovation noise rate
    private java.util.List<String> customTensorMapsBase64 = new java.util.ArrayList<>();
    private java.util.List<Boolean> tensorProceduralModes = new java.util.ArrayList<>();
    private java.util.Map<Integer, Long> tensorSeeds = new java.util.HashMap<>();
    private java.util.Map<Integer, java.util.Map<String, Double>> tensorProceduralParameters = new java.util.HashMap<>();
    private java.util.Map<Integer, String> customTensorNames = new java.util.HashMap<>();

    // Geological & Mineral Energy Extensible Tensor Layers (Persisted per Scenario)
    private int resourceVectorDimensions = 10; // Extensible resource dimensions (COAL, OIL, GAS, URANIUM, HELIUM_3, IRON_COPPER, PRECIOUS_METALS, CRITICAL_REE, MANTLE_HEAT, AQUIFERS...)
    private java.util.List<String> customGeologyTensorMapsBase64 = new java.util.ArrayList<>();
    private java.util.List<Boolean> geologyTensorProceduralModes = new java.util.ArrayList<>();

    // Engine Optimization & Determinism Controls (Persisted at Scenario Level for Physical Conformance)
    private int h3Resolution = 3; // H3 spatial grid resolution (1 to 8, default: 3)
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

    public Scenario copy() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(this);
            return mapper.readValue(json, Scenario.class);
        } catch (Exception e) {
            Scenario copy = new Scenario();
            copy.name = this.name;
            copy.presetKey = this.presetKey;
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
            copy.customTensorMapsBase64 = new java.util.ArrayList<>(this.customTensorMapsBase64);
            copy.tensorProceduralModes = new java.util.ArrayList<>(this.tensorProceduralModes);
            copy.tensorSeeds = new java.util.HashMap<>(this.tensorSeeds);
            copy.tensorProceduralParameters = new java.util.HashMap<>(this.tensorProceduralParameters);
            copy.customTensorNames = new java.util.HashMap<>(this.customTensorNames);
            copy.resourceVectorDimensions = this.resourceVectorDimensions;
            copy.customGeologyTensorMapsBase64 = new java.util.ArrayList<>(this.customGeologyTensorMapsBase64);
            copy.geologyTensorProceduralModes = new java.util.ArrayList<>(this.geologyTensorProceduralModes);
            copy.h3Resolution = this.h3Resolution;
            copy.strictDeterminism = this.strictDeterminism;
            copy.sparseCellSkippingEnabled = this.sparseCellSkippingEnabled;
            copy.oceanMacroAggregationEnabled = this.oceanMacroAggregationEnabled;
            copy.coastalNavigationOnlyEnabled = this.coastalNavigationOnlyEnabled;
            copy.oceanMultiRateTickingEnabled = this.oceanMultiRateTickingEnabled;
            copy.parallelExecutionEnabled = this.parallelExecutionEnabled;
            copy.spatialRangeTruncationEnabled = this.spatialRangeTruncationEnabled;
            copy.typeBEngineStates = new java.util.HashMap<>(this.typeBEngineStates);
            copy.typeBEngineParameters = new java.util.HashMap<>(this.typeBEngineParameters);
            copy.climateEvents = new java.util.ArrayList<>(this.climateEvents);
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
                ðŸ”¬ SCÃ‰NARIO EXPÃ‰RIMENTAL Ã‰THER : Configuration de Simulation PlanÃ©taire (%s)
                
                [CONTEXTE HISTORIQUE & PHYSIQUE]
                Ce scÃ©nario dÃ©finit les conditions aux limites et les termes de forÃ§age physique initiaux pour la modÃ©lisation multi-Ã©chelle des systÃ¨mes humains, Ã©cologiques et atmosphÃ©riques.
                
                [CONDITIONS INITIALES PHYSIQUES (T_0)]
                â€¢ Population Initiale : %,d individus.
                â€¢ Capital Physique (Kâ‚€) : %.1f kg/habitant (Outillages & machines).
                â€¢ Ã‰nergie StockÃ©e (Eâ‚€) : %.1f MJ/habitant (Stocks Ã©nergÃ©tiques).
                â€¢ RÃ©serves Alimentaires (Fâ‚€) : %.1f mois (Autonomie alimentaire).
                â€¢ Savoir ArchivÃ© (Iâ‚€) : %.1f bits/habitant (MÃ©moire technique).
                â€¢ Rayon PlanÃ©taire : %.0f km (g_rel = %.2f g).
                â€¢ Rotation PlanÃ©taire : %.1f heures | Inclinaison Axiale : %.1fÂ°.
                """,
                name != null ? name : "ScÃ©nario Standard",
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

    public int getH3Resolution() {
        return h3Resolution > 0 ? h3Resolution : 3;
    }

    public void setH3Resolution(int h3Resolution) {
        this.h3Resolution = h3Resolution;
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

    public boolean isEarthHistoricalLeadersEnabled() {
        return earthHistoricalLeadersEnabled;
    }

    public void setEarthHistoricalLeadersEnabled(boolean earthHistoricalLeadersEnabled) {
        this.earthHistoricalLeadersEnabled = earthHistoricalLeadersEnabled;
    }

    public boolean isProceduralLeadersEnabled() {
        return proceduralLeadersEnabled;
    }

    public void setProceduralLeadersEnabled(boolean proceduralLeadersEnabled) {
        this.proceduralLeadersEnabled = proceduralLeadersEnabled;
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
    public org.ether.society.config.SimulationPerformanceConfig toPerformanceConfig() {
        org.ether.society.config.SimulationPerformanceConfig config = new org.ether.society.config.SimulationPerformanceConfig();
        config.setStrictDeterminism(strictDeterminism);
        config.setEnableSparseCellSkipping(!strictDeterminism && sparseCellSkippingEnabled);
        config.setEnableOceanMacroAggregation(!strictDeterminism && oceanMacroAggregationEnabled);
        config.setEnableCoastalNavigationOnly(!strictDeterminism && coastalNavigationOnlyEnabled);
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

    public java.util.Map<Integer, String> getCustomTensorNames() {
        return customTensorNames;
    }

    public void setCustomTensorNames(java.util.Map<Integer, String> customTensorNames) {
        this.customTensorNames = customTensorNames != null ? customTensorNames : new java.util.HashMap<>();
    }

    public String getCustomTensorName(int index) {
        return customTensorNames != null ? customTensorNames.get(index) : null;
    }

    public void setCustomTensorName(int index, String name) {
        if (this.customTensorNames == null) this.customTensorNames = new java.util.HashMap<>();
        if (name == null || name.isBlank()) {
            this.customTensorNames.remove(index);
        } else {
            this.customTensorNames.put(index, name.trim());
        }
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

        // --- SCÃ‰NARIOS DU PASSÃ‰ ---
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
        s0.setPlanetPreset(PlanetPreset.EARTH_LIG_100000BP);
        s0.setEcologyPreset(EcologyPreset.EARTH_LIG_100000BP);
        s0.setDescription("""
            ðŸŒ SCÃ‰NARIO PALÃ‰OLITHIQUE : Berceau Africain, TraversÃ©e des Continents & Out of Africa (-100 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise la dynamique dÃ©mographique et l'expansion spatiale des premiÃ¨res populations d'Homo Sapiens depuis l'Afrique de l'Est Ã  travers le Moyen-Orient, l'Eurasie, l'OcÃ©anie et les AmÃ©riques.
            
            [CONDITIONS INITIALES PHYSIQUES (Tâ‚€)]
            â€¢ Population Initiale : 50 000 individus (CapacitÃ© nomade prÃ©-agricole).
            â€¢ Stock Capital Physique (Kâ‚€) : 2 kg/habitant (bifaces en pierre, javelots).
            â€¢ Ã‰nergie StockÃ©e (Eâ‚€) : 5 MJ/habitant (maÃ®trise du feu et combustible bois).
            â€¢ RÃ©serves Alimentaires (Fâ‚€) : 2 mois de subsistance en chasse-cueillette.
            â€¢ Savoir ArchivÃ© (Iâ‚€) : 2 bits/habitant (traditions orales palÃ©olithiques & langage).
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Vitesse de dispersion gÃ©ographique vers le Proche-Orient, l'Asie du Sud et l'Europe.
            â€¢ Survie dÃ©mographique nomade face aux glaciations et Ã©vÃ¨nements de Dansgaard-Oeschger.
            â€¢ DÃ©rive linguistique palÃ©olithique et innovations lithiques (Levallois, emmanchement Ã  la rÃ©sine).
            """);
        java.util.Map<String, Boolean> s0Engines = s0.getTypeBEngineStates();
        s0Engines.put("PaleoLanguageDriftEngine", true);
        s0Engines.put("KarstCaveShelterEngine", true);
        s0Engines.put("LithicTradeProvenanceEngine", true);
        s0Engines.put("OchreTanningTechnologyEngine", true);
        s0Engines.put("CoastalMarineRefugiaEngine", true);
        s0Engines.put("ResinHaftingAdhesivesEngine", true);
        s0Engines.put("FireHardenedSpearEngine", true);
        s0Engines.put("GeophyteDiggingStickEngine", true);
        s0Engines.put("LevalloisPreparedCoreEngine", true);
        s0Engines.put("AcheuleanBifaceSymmetryEngine", true);
        s0Engines.put("OldowanMarrowPercussionEngine", true);
        s0Engines.put("HomininCompetitiveExclusionEngine", true);
        s0Engines.put("DemographicLifeTableEngine", true);
        s0Engines.put("ToolKitMaintenanceEngine", true);
        s0Engines.put("SelfDomesticationEngine", true);
        s0Engines.put("TasmanianCulturalRegressionEngine", true);
        s0Engines.put("KinSelectionHamiltonEngine", true);
        list.add(s0);

        // --- SCÃ‰NARIO : CATACLYSME DU SUPERVOLCAN TOBA (-74000) ---
        Scenario sToba = new Scenario();
        sToba.setPresetKey("toba_cataclysm_74k");
        sToba.setName("Cataclysme du Supervolcan Toba & Goulot DÃ©mographique (-74000)");
        sToba.setStartDateYear(-74000);
        sToba.setEndDateYear(-50000);
        sToba.setInitialHumanCount(100000);
        sToba.setInitialCapitalPerCapita(2.0);
        sToba.setInitialEnergyPerCapita(4.0);
        sToba.setInitialFoodReserveMonths(1.0);
        sToba.setInitialInformationPerCapita(2.0);
        sToba.setPopulationDensityType("SPARSE");
        sToba.setTargetCohortSize(30);
        sToba.setPlanetPreset(PlanetPreset.EARTH_MIS3_50000BP);
        sToba.setEcologyPreset(EcologyPreset.EARTH_MIS3_50000BP);
        sToba.setDescription("""
            ðŸŒ‹ SCÃ‰NARIO PALÃ‰OCLIMATIQUE : Super-Ã‰ruption du Mont Toba & Hiver Volcanique (-74 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise l'impact biosphÃ©rique de la super-Ã©ruption du Toba (Sumatra, VEI-8) ayant Ã©jectÃ© 2 800 kmÂ³ de tÃ©phras. Chute thermique globale de 3 Ã  5Â°C pendant plusieurs annÃ©es, crÃ©ant un goulot d'Ã©tranglement gÃ©nÃ©tique majeur chez Homo sapiens.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Goulot d'Ã©tranglement dÃ©mographique sÃ©vÃ¨re (population reproductive mondiale rÃ©duite Ã  quelques milliers d'individus).
            â€¢ RÃ©fuges Ã©cologiques cÃ´tiers en Afrique australe et en Inde mÃ©ridionale.
            â€¢ Rebond dÃ©mographique et innovations techniques lithiques post-crise (Mode 3 / Middle Stone Age).
            """);
        java.util.Map<String, Boolean> sTobaEngines = sToba.getTypeBEngineStates();
        sTobaEngines.put("VolcanicTephraRefugiaEngine", true);
        sTobaEngines.put("BifurcationChaosEngine", true);
        sTobaEngines.put("DemographicLifeTableEngine", true);
        sTobaEngines.put("HomininCompetitiveExclusionEngine", true);
        sTobaEngines.put("TasmanianCulturalRegressionEngine", true);
        sTobaEngines.put("KinSelectionHamiltonEngine", true);
        sToba.getClimateEvents().add(new ClimateEvent("VOLCANIC_ERUPTION", "Super-Ã‰ruption VEI-8 du Mont Toba", -74000, 2.58, 98.83, -5.0, 9.5));
        list.add(sToba);

        // --- SCÃ‰NARIO : SAHUL (-50000) ---
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
        sSahul.setPlanetPreset(PlanetPreset.EARTH_MIS3_50000BP);
        sSahul.setEcologyPreset(EcologyPreset.EARTH_MIS3_50000BP);
        sSahul.setClippingEnabled(true);
        sSahul.setMinLat(-42.0); sSahul.setMaxLat(-10.0); sSahul.setMinLng(112.0); sSahul.setMaxLng(155.0);
        sSahul.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSahul.setDescription("""
            ðŸ¦˜ SCÃ‰NARIO PALÃ‰OLITHIQUE : TraversÃ©e Maritime & Incursion dans le Sahul (-50 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Premier franchissement maritime majeur de la ligne de Wallace par les ancÃªtres des AborigÃ¨nes d'Australie. ModÃ©lise la colonisation du continent Sahul (Australie, Tasmanie, Nouvelle-GuinÃ©e rÃ©unies) et l'adaptation aux Ã©cosystÃ¨mes arides.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Franchissement maritime de la ligne de Wallace et navigation cÃ´tiÃ¨re insulaire.
            â€¢ Adaptation aux rÃ©gimes arides intÃ©rieurs et gestion des paysages par brÃ»lis (fire-stick farming).
            â€¢ StabilitÃ© dÃ©mographique Ã  long terme et rÃ©seaux d'alliances Ã  l'ocre Ã  travers le dÃ©sert.
            """);
        java.util.Map<String, Boolean> sSahulEngines = sSahul.getTypeBEngineStates();
        sSahulEngines.put("PaleoLanguageDriftEngine", true);
        sSahulEngines.put("KarstCaveShelterEngine", true);
        sSahulEngines.put("ResinHaftingAdhesivesEngine", true);
        sSahulEngines.put("CoastalMarineRefugiaEngine", true);
        sSahulEngines.put("RiverCanoeTransportEngine", true);
        sSahulEngines.put("AridOasisWellDiggingEngine", true);
        sSahulEngines.put("FireStickFarmingEngine", true);
        sSahulEngines.put("AridWaterStorageStashEngine", true);
        sSahulEngines.put("OchreTradeAllianceEngine", true);
        sSahulEngines.put("ArchaicIntrogressionEngine", true);
        sSahulEngines.put("TasmanianCulturalRegressionEngine", true);
        sSahulEngines.put("MegafaunaEcosystemEngine", true);
        sSahulEngines.put("KinSelectionHamiltonEngine", true);
        list.add(sSahul);

        // --- SCÃ‰NARIO : BÃ‰RINGIE & PEUPLEMENT DES AMÃ‰RIQUES (-25000) ---
        Scenario sBeringia = new Scenario();
        sBeringia.setPresetKey("beringia");
        sBeringia.setName("BÃ©ringie & Peuplement des AmÃ©riques (-25000)");
        sBeringia.setStartDateYear(-25000);
        sBeringia.setEndDateYear(-10000);
        sBeringia.setInitialHumanCount(15000);
        sBeringia.setInitialCapitalPerCapita(3.0);
        sBeringia.setInitialEnergyPerCapita(6.0);
        sBeringia.setInitialFoodReserveMonths(2.0);
        sBeringia.setInitialInformationPerCapita(4.0);
        sBeringia.setPopulationDensityType("BERINGIA_AMERICAS");
        sBeringia.setTargetCohortSize(40);
        sBeringia.setPlanetPreset(PlanetPreset.EARTH_LGM_ONSET_25000BP);
        sBeringia.setEcologyPreset(EcologyPreset.EARTH_LGM_ONSET_25000BP);
        sBeringia.setClippingEnabled(true);
        sBeringia.setMinLat(45.0); sBeringia.setMaxLat(75.0); sBeringia.setMinLng(140.0); sBeringia.setMaxLng(-120.0);
        sBeringia.setBoundaryMode("DYNAMIC_RESERVOIR");
        sBeringia.setDescription("""
            ðŸ”ï¸ SCÃ‰NARIO PALÃ‰OLITHIQUE : Le Pont Terrestre de BÃ©ringie & Incursion AmÃ©ricaine (-25 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise l'isolation des populations palÃ©olithiques sur le pont terrestre de BÃ©ringie pendant le Dernier Maximum Glaciaire (LGM), suivie de leur dispersion Ã  travers le corridor libre de glace et la route cÃ´tiÃ¨re du Pacifique.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Temps de pause/stase gÃ©nÃ©tique sur le pont de BÃ©ringie (Beringian Standstill).
            â€¢ Franchissement du corridor libre de glace ou de la route cÃ´tiÃ¨re du varech (Kelp Highway).
            â€¢ Taux d'expansion dÃ©mographique rapide vers l'AmÃ©rique du Nord puis du Sud.
            """);
        java.util.Map<String, Boolean> sBeringiaEngines = sBeringia.getTypeBEngineStates();
        sBeringiaEngines.put("TailoredClothingThermalEngine", true);
        sBeringiaEngines.put("EyedNeedleSewingEngine", true);
        sBeringiaEngines.put("MeatCuringReservesEngine", true);
        sBeringiaEngines.put("OsseousIndustryCarvingEngine", true);
        sBeringiaEngines.put("MammothBoneHabitationEngine", true);
        sBeringiaEngines.put("PermafrostColdCacheEngine", true);
        sBeringiaEngines.put("LithicTradeProvenanceEngine", true);
        sBeringiaEngines.put("BeringianStandstillIsolationEngine", true);
        sBeringiaEngines.put("PressureFlakerPointEngine", true);
        sBeringiaEngines.put("IvoryHotWaterStraighteningEngine", true);
        sBeringiaEngines.put("PeriglacialLoessDustEngine", true);
        sBeringiaEngines.put("TasmanianCulturalRegressionEngine", true);
        sBeringiaEngines.put("KinSelectionHamiltonEngine", true);
        list.add(sBeringia);

        // --- SCÃ‰NARIO : DERNIER MAXIMUM GLACIAIRE & SOLUTRÃ‰EN (-20000) ---
        Scenario sLGM = new Scenario();
        sLGM.setPresetKey("lgm_solutrean");
        sLGM.setName("Dernier Maximum Glaciaire & SolutrÃ©en (-20000)");
        sLGM.setStartDateYear(-20000);
        sLGM.setEndDateYear(-12000);
        sLGM.setInitialHumanCount(20000);
        sLGM.setInitialCapitalPerCapita(4.0);
        sLGM.setInitialEnergyPerCapita(7.0);
        sLGM.setInitialFoodReserveMonths(2.5);
        sLGM.setInitialInformationPerCapita(5.0);
        sLGM.setPopulationDensityType("LGM_REFUGIA");
        sLGM.setTargetCohortSize(40);
        sLGM.setPlanetPreset(PlanetPreset.EARTH_LGM_20000BP);
        sLGM.setEcologyPreset(EcologyPreset.EARTH_LGM_20000BP);
        sLGM.setDescription("""
            â„ï¸ SCÃ‰NARIO PALÃ‰OLITHIQUE : ApogÃ©e Glaciaire & Refuges SolutrÃ©ens (-20 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise le paroxysme du Dernier Maximum Glaciaire (LGM) avec un niveau marin abaissÃ© de 120 mÃ¨tres (exposant le Doggerland, le Sundaland, le Sahul et la BÃ©ringie), les inlandsis massifs (Laurentide, Fennoscandie) et l'industrie lithique foliacÃ©e solutrÃ©enne.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Concentration des populations dans les refuges thermiques (pÃ©ninsule IbÃ©rique, zone franco-cantabrique, Balkans).
            â€¢ MaÃ®trise technologique du froid extrÃªme (vÃªtements ajustÃ©s Ã  l'aiguille Ã  chas, fosses-congÃ©lateurs pÃ©riglaciaires).
            â€¢ RÃ©expansion dÃ©mographique post-glaciaire rapide lors du rÃ©chauffement de BÃ¸lling-AllerÃ¸d.
            """);
        java.util.Map<String, Boolean> sLGMEngines = sLGM.getTypeBEngineStates();
        sLGMEngines.put("PressureFlakerPointEngine", true);
        sLGMEngines.put("TailoredClothingThermalEngine", true);
        sLGMEngines.put("EyedNeedleSewingEngine", true);
        sLGMEngines.put("MeatCuringReservesEngine", true);
        sLGMEngines.put("PermafrostColdCacheEngine", true);
        sLGMEngines.put("TasmanianCulturalRegressionEngine", true);
        sLGMEngines.put("MarrowFatRenderingEngine", true);
        sLGMEngines.put("SnowTroughRefrigerationEngine", true);
        sLGMEngines.put("IvoryHotWaterStraighteningEngine", true);
        sLGMEngines.put("CaveWallClaySealingEngine", true);
        sLGMEngines.put("KinSelectionHamiltonEngine", true);
        list.add(sLGM);

        // --- SCÃ‰NARIO : RÃ‰CENTS DRYAS (-10900) ---
        Scenario sYoungerDryas = new Scenario();
        sYoungerDryas.setPresetKey("younger_dryas");
        sYoungerDryas.setName("Le RÃ©cents Dryas & Choc Climatique Natufien (-10900)");
        sYoungerDryas.setStartDateYear(-10900);
        sYoungerDryas.setEndDateYear(-9500);
        sYoungerDryas.setInitialHumanCount(40000);
        sYoungerDryas.setInitialCapitalPerCapita(4.0);
        sYoungerDryas.setInitialEnergyPerCapita(8.0);
        sYoungerDryas.setInitialFoodReserveMonths(2.5);
        sYoungerDryas.setInitialInformationPerCapita(8.0);
        sYoungerDryas.setPopulationDensityType("YOUNGER_DRYAS");
        sYoungerDryas.setTargetCohortSize(50);
        sYoungerDryas.setPlanetPreset(PlanetPreset.EARTH_EH_10000BP);
        sYoungerDryas.setEcologyPreset(EcologyPreset.EARTH_EH_10000BP);
        sYoungerDryas.setDescription("""
            â„ï¸ SCÃ‰NARIO PALÃ‰OCLIMATIQUE : Le RÃ©cents Dryas & Pression ForagÃ¨re Au Levant (-10 900 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Refroidissement brutal de 5 Ã  8Â°C de l'Atlantique Nord dÃ©clenchÃ© par le dÃ©versement d'eau douce du Lac Agassiz. Au Levant, la sÃ©cheresse aiguÃ« rÃ©duit les cÃ©rÃ©ales sauvages, contraignant les populations Natufiennes Ã  la sÃ©dentarisation prÃ©-agricole et au contrÃ´le des graines.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Chute brutale des rendements foragers de cÃ©rÃ©ales sauvages au Levant suite Ã  l'aridification.
            â€¢ Ã‰mergence des premiers hameaux sÃ©dentaires natoufiens et stockage intensif des grains en silos.
            â€¢ Pression sÃ©lective poussant Ã  la domestication du chien et aux premiers semis intentionnels.
            """);
        java.util.Map<String, Boolean> sYoungerDryasEngines = sYoungerDryas.getTypeBEngineStates();
        sYoungerDryasEngines.put("WildCerealGrindingEngine", true);
        sYoungerDryasEngines.put("TopographicGameDriveEngine", true);
        sYoungerDryasEngines.put("AtlatlArcheryBallisticsEngine", true);
        sYoungerDryasEngines.put("PassiveSnareSmallGameEngine", true);
        sYoungerDryasEngines.put("EpipaleolithicStorageHamletEngine", true);
        sYoungerDryasEngines.put("SeasonalAggregationSanctuaryEngine", true);
        sYoungerDryasEngines.put("BisonCliffJumpDriveEngine", true);
        sYoungerDryasEngines.put("CanidDomesticationEngine", true);
        sYoungerDryasEngines.put("BasaltGrindingSlabEngine", true);
        sYoungerDryasEngines.put("OstromCommonsPureEngine", true);
        sYoungerDryasEngines.put("TasmanianCulturalRegressionEngine", true);
        list.add(sYoungerDryas);

        Scenario s1 = new Scenario();
        s1.setPresetKey("fertile_crescent");
        s1.setName("Croissant Fertile & NÃ©olithique (-8000)");
        s1.setStartDateYear(-8000);
        s1.setEndDateYear(-5000);
        s1.setInitialHumanCount(25000);
        s1.setInitialCapitalPerCapita(5.0);
        s1.setInitialEnergyPerCapita(10.0);
        s1.setInitialFoodReserveMonths(3.0);
        s1.setInitialInformationPerCapita(5.0);
        s1.setPopulationDensityType("FERTILE_CRESCENT");
        s1.setTargetCohortSize(150);
        s1.setPlanetPreset(PlanetPreset.EARTH_EH_10000BP);
        s1.setEcologyPreset(EcologyPreset.EARTH_EH_10000BP);
        s1.setClippingEnabled(true);
        s1.setMinLat(25.0); s1.setMaxLat(42.0); s1.setMinLng(25.0); s1.setMaxLng(55.0);
        s1.setBoundaryMode("DYNAMIC_RESERVOIR");
        s1.setDescription("""
            ðŸŒ¾ SCÃ‰NARIO HISTORIQUE : L'Aube de l'Agriculture au Croissant Fertile (-8000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Ce scÃ©nario modÃ©lise la transition majeure du NÃ©olithique entre l'Ã©conomie de subsistance des chasseurs-cueilleurs et l'Ã©mergence des premiÃ¨res communautÃ©s agricoles sÃ©dentaires le long du Tigre, de l'Euphrate, du Nil et de la cÃ´te Levantine.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Explosion dÃ©mographique liÃ©e aux surplus agricoles cÃ©rÃ©aliers (engrain, amidonnier, orge).
            â€¢ Ã‰mergence des premiÃ¨res citÃ©s-Ã‰tats mÃ©sopotamiennes, chefferies et diffÃ©renciation sociale.
            â€¢ DÃ©gradation environnementale prÃ©coce (dÃ©forestation des piÃ©monts, Ã©rosion des sols, dÃ©but de salinisation).
            """);
        java.util.Map<String, Boolean> s1Engines = s1.getTypeBEngineStates();
        s1Engines.put("WildCerealGrindingEngine", true);
        s1Engines.put("EpipaleolithicStorageHamletEngine", true);
        s1Engines.put("BasaltGrindingSlabEngine", true);
        s1Engines.put("CanidDomesticationEngine", true);
        s1Engines.put("BoserupAgriculturalIntensificationEngine", true);
        s1Engines.put("ScottAgainstTheGrainPureEngine", true);
        s1Engines.put("OstromCommonsPureEngine", true);
        s1Engines.put("DeforestationErosionEngine", true);
        list.add(s1);

        // --- SCÃ‰NARIO : SAHARA VERT (PÃ‰RIODE HUMIDE AFRICAINE -6000) ---
        Scenario sGreenSahara = new Scenario();
        sGreenSahara.setPresetKey("green_sahara");
        sGreenSahara.setName("Le Sahara Vert & PÃ©riode Humide Africaine (-6000)");
        sGreenSahara.setStartDateYear(-6000);
        sGreenSahara.setEndDateYear(-3500);
        sGreenSahara.setInitialHumanCount(60000);
        sGreenSahara.setInitialCapitalPerCapita(6.0);
        sGreenSahara.setInitialEnergyPerCapita(12.0);
        sGreenSahara.setInitialFoodReserveMonths(4.0);
        sGreenSahara.setInitialInformationPerCapita(10.0);
        sGreenSahara.setPopulationDensityType("GREEN_SAHARA");
        sGreenSahara.setTargetCohortSize(60);
        sGreenSahara.setPlanetPreset(PlanetPreset.EARTH_MH_6000BP);
        sGreenSahara.setEcologyPreset(EcologyPreset.EARTH_MH_6000BP);
        sGreenSahara.setClippingEnabled(true);
        sGreenSahara.setMinLat(0.0); sGreenSahara.setMaxLat(42.0); sGreenSahara.setMinLng(-20.0); sGreenSahara.setMaxLng(45.0);
        sGreenSahara.setBoundaryMode("DYNAMIC_RESERVOIR");
        sGreenSahara.setDescription("""
            ðŸŒ´ SCÃ‰NARIO PALÃ‰OCLIMATIQUE : Le Sahara Vert & PÃ©riode Humide Africaine (-6000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise la PÃ©riode Humide Africaine (AHP) oÃ¹ l'insolation printaniÃ¨re amplifiÃ©e par l'orbite terrestre a intensifiÃ© la mousson africaine. Le dÃ©sert du Sahara Ã©tait alors une savane verdoyante parsemÃ©e de lac majeurs (Lac MÃ©ga-Tchad), peuplÃ©e d'Ã©leveurs nÃ©olithiques et de chasseurs-cueilleurs.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ DensitÃ© humaine Ã©levÃ©e et corridor trans-saharien autour du Lac MÃ©ga-Tchad et des oueds.
            â€¢ Ã‰conomie mixte : pastoralisme bovin nÃ©olithique, chasse et pÃªche pÃ©lagique lacustre.
            â€¢ Migration et repli massif des populations vers la vallÃ©e du Nil lors de la dÃ©sertification vers -3500.
            """);
        java.util.Map<String, Boolean> sGreenSaharaEngines = sGreenSahara.getTypeBEngineStates();
        sGreenSaharaEngines.put("LakeChadWadiMigrationEngine", true);
        sGreenSaharaEngines.put("AridWaterStorageStashEngine", true);
        sGreenSaharaEngines.put("PelagicFishingHookEngine", true);
        sGreenSaharaEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sGreenSaharaEngines.put("OstromCommonsPureEngine", true);
        sGreenSaharaEngines.put("CulturalSociologyEngine", true);
        list.add(sGreenSahara);

        // --- SCÃ‰NARIO : Ã‰GYPTE ANTIQUE (-3000) ---
        Scenario sEgypt = new Scenario();
        sEgypt.setPresetKey("ancient_egypt");
        sEgypt.setName("Ã‰gypte Antique & VallÃ©e du Nil (-3000)");
        sEgypt.setStartDateYear(-3000);
        sEgypt.setEndDateYear(-1000);
        sEgypt.setInitialHumanCount(1500000);
        sEgypt.setInitialCapitalPerCapita(60.0);
        sEgypt.setInitialEnergyPerCapita(40.0);
        sEgypt.setInitialFoodReserveMonths(6.0);
        sEgypt.setInitialInformationPerCapita(40.0);
        sEgypt.setPopulationDensityType("EGYPT_NILE");
        sEgypt.setTargetCohortSize(300);
        sEgypt.setPlanetPreset(PlanetPreset.EARTH_LH_3000BP);
        sEgypt.setEcologyPreset(EcologyPreset.EARTH_LH_3000BP);
        sEgypt.setClippingEnabled(true);
        sEgypt.setMinLat(21.0); sEgypt.setMaxLat(32.0); sEgypt.setMinLng(24.0); sEgypt.setMaxLng(36.0);
        sEgypt.setBoundaryMode("DYNAMIC_RESERVOIR");
        sEgypt.setDescription("""
            ð“€€ SCÃ‰NARIO HISTORIQUE : Unification Thinite & Crues du Nil (-3000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise l'Ã©mergence de la premiÃ¨re civilisation pharaonique unifiÃ©e. DÃ©pendance absolue vis-Ã -vis du rythme annuel du Nil, de la gestion du bassin d'irrigation et de l'administration hiÃ©roglyphique.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Gestion hydraulique Ã©tatique centralisÃ©e des crues annuelles et du limon fertile.
            â€¢ Accumulation monumentale de capital physique et symbolique (pyramides, canaux, greniers d'Ã‰tat).
            â€¢ VulnÃ©rabilitÃ© systÃ©mique aux sÃ©cheresses prolongÃ©es (chute de l'Ancien Empire / 1Ã¨re PÃ©riode IntermÃ©diaire).
            """);
        java.util.Map<String, Boolean> sEgyptEngines = sEgypt.getTypeBEngineStates();
        sEgyptEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sEgyptEngines.put("ScottAgainstTheGrainPureEngine", true);
        sEgyptEngines.put("TurchinGoldstoneSDTEngine", true);
        sEgyptEngines.put("FertileCrescentSalinizationEngine", true);
        sEgyptEngines.put("HydrologicalEngineeringEngine", true);
        sEgyptEngines.put("MaritimeHighwayEngine", true);
        sEgyptEngines.put("SpatialCityFractalEngine", true);
        sEgyptEngines.put("TainterComplexityCollapseEngine", true);
        sEgyptEngines.put("FrontierAsabiyyahEngine", true);
        sEgyptEngines.put("DeforestationErosionEngine", true);
        list.add(sEgypt);

        // --- SCÃ‰NARIO : Ã‚GE DU BRONZE MOYEN & MÃ‰SOPOTAMIE (-1900) ---
        Scenario s3 = new Scenario();
        s3.setPresetKey("assyrian_empire");
        s3.setName("Ã‚ge du Bronze Moyen & MÃ©sopotamie (-1900)");
        s3.setStartDateYear(-1900);
        s3.setEndDateYear(-600);
        s3.setInitialHumanCount(600000);
        s3.setInitialCapitalPerCapita(80.0);
        s3.setInitialEnergyPerCapita(50.0);
        s3.setInitialFoodReserveMonths(6.0);
        s3.setInitialInformationPerCapita(50.0);
        s3.setPopulationDensityType("MESOPOTAMIA_ASSYRIA");
        s3.setTargetCohortSize(500);
        s3.setPlanetPreset(PlanetPreset.EARTH_BRONZE_1900BP);
        s3.setEcologyPreset(EcologyPreset.EARTH_BRONZE_1900BP);
        s3.setClippingEnabled(true);
        s3.setMinLat(28.0); s3.setMaxLat(40.0); s3.setMinLng(38.0); s3.setMaxLng(52.0);
        s3.setBoundaryMode("DYNAMIC_RESERVOIR");
        s3.setDescription("""
            ðŸ›ï¸ SCÃ‰NARIO HISTORIQUE : Hydraulique, Salinisation & CitÃ©s-Ã‰tats de l'Ã‚ge du Bronze (-1900 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise l'apogÃ©e du Bronze Moyen (Code d'Hammurabi, premiÃ¨re dynastie de Babylone, dynastie Shang en Chine, fin de la civilisation de l'Indus) et les vulnÃ©rabilitÃ©s Ã©cologiques d'irrigation intensive.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Salinisation progressive des sols irriguÃ©s de basse MÃ©sopotamie (baisse du blÃ© au profit de l'orge).
            â€¢ Dynamique d'Asabiyyah des peuples pÃ©riphÃ©riques et militarisation de l'Empire NÃ©o-Assyrien.
            â€¢ RÃ©seaux urbains denses le long des canaux et dÃ©pendance au commerce du cuivre et de l'Ã©tain.
            """);
        java.util.Map<String, Boolean> s3Engines = s3.getTypeBEngineStates();
        s3Engines.put("BoserupAgriculturalIntensificationEngine", true);
        s3Engines.put("ScottAgainstTheGrainPureEngine", true);
        s3Engines.put("TurchinGoldstoneSDTEngine", true);
        s3Engines.put("FertileCrescentSalinizationEngine", true);
        s3Engines.put("FrontierAsabiyyahEngine", true);
        s3Engines.put("HydrologicalEngineeringEngine", true);
        s3Engines.put("SpatialCityFractalEngine", true);
        s3Engines.put("TainterComplexityCollapseEngine", true);
        s3Engines.put("DeforestationErosionEngine", true);
        list.add(s3);

        // --- SCÃ‰NARIO : MÃ‰SOAMÃ‰RIQUE (-1500) ---
        Scenario sMeso = new Scenario();
        sMeso.setPresetKey("mesoamerica");
        sMeso.setName("Civilisations MÃ©soamÃ©ricaines (OlmÃ¨ques & Mayas) (-1500)");
        sMeso.setStartDateYear(-1500);
        sMeso.setEndDateYear(900);
        sMeso.setInitialHumanCount(3000000);
        sMeso.setInitialCapitalPerCapita(120.0);
        sMeso.setInitialEnergyPerCapita(80.0);
        sMeso.setInitialFoodReserveMonths(6.0);
        sMeso.setInitialInformationPerCapita(150.0);
        sMeso.setPopulationDensityType("MESOAMERICA");
        sMeso.setTargetCohortSize(250);
        sMeso.setPlanetPreset(PlanetPreset.EARTH_BRONZE_1900BP);
        sMeso.setEcologyPreset(EcologyPreset.EARTH_BRONZE_1900BP);
        sMeso.setClippingEnabled(true);
        sMeso.setMinLat(12.0); sMeso.setMaxLat(24.0); sMeso.setMinLng(-105.0); sMeso.setMaxLng(-85.0);
        sMeso.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMeso.setDescription("""
            ðŸŒ½ SCÃ‰NARIO HISTORIQUE : Culture MÃ¨re OlmÃ¨que & CitÃ©s-Ã‰tats Mayas (-1500 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Ã‰mergence des centres cÃ©rÃ©moniels de San Lorenzo et La Venta, puis essor de la civilisation maya classique. ModÃ©lise la maÃ¯siculture intensive, les rÃ©servoirs d'eau pluviale et l'astronomie de prÃ©cision.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Intensification agricole sur terrasses et chinampas sans bÃªtes de trait ni mÃ©tallurgie du fer.
            â€¢ CyclicitÃ©s de sÃ©cheresse mÃ©soamÃ©ricaines et rÃ©silience des rÃ©servoirs d'eau (aguadas, chultuns, cÃ©notes).
            â€¢ Ã‰volution fractale et dispersion des citÃ©s cÃ©rÃ©monielles mayas suivies d'effondrements rÃ©gionaux.
            """);
        java.util.Map<String, Boolean> sMesoEngines = sMeso.getTypeBEngineStates();
        sMesoEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sMesoEngines.put("ScottAgainstTheGrainPureEngine", true);
        sMesoEngines.put("TurchinGoldstoneSDTEngine", true);
        sMesoEngines.put("AmerindianEcosystemEngine", true);
        sMesoEngines.put("HydrologicalEngineeringEngine", true);
        sMesoEngines.put("SpatialCityFractalEngine", true);
        sMesoEngines.put("TainterComplexityCollapseEngine", true);
        sMesoEngines.put("DeforestationErosionEngine", true);
        sMesoEngines.put("OstromCommonsPureEngine", true);
        list.add(sMeso);

        // --- SCÃ‰NARIO : EFFONDREMENT DE L'Ã‚GE DU BRONZE RÃ‰CENT (-1200) ---
        Scenario sBronzeCollapse = new Scenario();
        sBronzeCollapse.setPresetKey("bronze_age_collapse_1200bc");
        sBronzeCollapse.setName("Effondrement de l'Ã‚ge du Bronze RÃ©cent & Peuples de la Mer (-1200)");
        sBronzeCollapse.setStartDateYear(-1200);
        sBronzeCollapse.setEndDateYear(-900);
        sBronzeCollapse.setInitialHumanCount(25000000);
        sBronzeCollapse.setInitialCapitalPerCapita(120.0);
        sBronzeCollapse.setInitialEnergyPerCapita(65.0);
        sBronzeCollapse.setInitialFoodReserveMonths(3.0);
        sBronzeCollapse.setInitialInformationPerCapita(80.0);
        sBronzeCollapse.setPopulationDensityType("URBAN_CLUSTERS");
        sBronzeCollapse.setTargetCohortSize(400);
        sBronzeCollapse.setPlanetPreset(PlanetPreset.EARTH_BRONZE_1900BP);
        sBronzeCollapse.setEcologyPreset(EcologyPreset.EARTH_BRONZE_1900BP);
        sBronzeCollapse.setDescription("""
            âš”ï¸ SCÃ‰NARIO HISTORIQUE : La Grande Rupture SystÃ©mique de -1200 & Chute des Palais MycÃ©niens et Hittites
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise l'effondrement simultanÃ© en cascade des civilisations palatiales de MÃ©diterranÃ©e orientale (MycÃ¨nes, Ugarit, Empire Hittite, affaiblissement de l'Ã‰gypte). Combinaison d'une mÃ©ga-sÃ©cheresse centennale, de ruptures des routes de l'Ã©tain et des invasions des Peuples de la Mer.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Effondrement en chaÃ®ne des rÃ©seaux commerciaux interconnectÃ©s du bronze.
            â€¢ Chute brutale de la complexitÃ© institutionnelle (disparition de l'Ã©criture LinÃ©aire B, dÃ©population urbaine).
            â€¢ PÃ©riode d'Ã‚ges Sombres mÃ©diterranÃ©ens prÃ©parant la transition dÃ©centralisÃ©e vers le fer.
            """);
        java.util.Map<String, Boolean> sBronzeCollapseEngines = sBronzeCollapse.getTypeBEngineStates();
        sBronzeCollapseEngines.put("TainterComplexityCollapseEngine", true);
        sBronzeCollapseEngines.put("SpatialMetapopulationSEIREngine", true);
        sBronzeCollapseEngines.put("FertileCrescentSalinizationEngine", true);
        sBronzeCollapseEngines.put("TurchinGoldstoneSDTEngine", true);
        sBronzeCollapseEngines.put("LanchesterKineticWarfareEngine", true);
        sBronzeCollapseEngines.put("GranovetterThresholdCascadeEngine", true);
        sBronzeCollapse.getClimateEvents().add(new ClimateEvent("MEGADROUGHT", "MÃ©ga-SÃ©cheresse Centennale de MÃ©diterranÃ©e Orientale", -1200, 35.0, 33.0, 0.0, 8.5));
        list.add(sBronzeCollapse);

        // --- SCÃ‰NARIO : DÃ‰BUT Ã‚GE DU FER & MÃ‰DITERRANÃ‰E ANTIQUE (-1000) ---
        Scenario sIron = new Scenario();
        sIron.setPresetKey("early_iron_age");
        sIron.setName("DÃ©but de l'Ã‚ge du Fer & MÃ©diterranÃ©e Antique (-1000)");
        sIron.setStartDateYear(-1000);
        sIron.setEndDateYear(-300);
        sIron.setInitialHumanCount(20000000);
        sIron.setInitialCapitalPerCapita(150.0);
        sIron.setInitialEnergyPerCapita(70.0);
        sIron.setInitialFoodReserveMonths(6.0);
        sIron.setInitialInformationPerCapita(120.0);
        sIron.setPopulationDensityType("URBAN_CLUSTERS");
        sIron.setTargetCohortSize(500);
        sIron.setPlanetPreset(PlanetPreset.EARTH_IRON_1000BP);
        sIron.setEcologyPreset(EcologyPreset.EARTH_IRON_1000BP);
        sIron.setDescription("""
            âš”ï¸ SCÃ‰NARIO HISTORIQUE : Transition Technologique vers la SidÃ©rurgie & RÃ©seaux PhÃ©niciens (-1000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise la transition de l'Ã‚ge du Bronze vers la mÃ©tallurgie du fer aprÃ¨s l'effondrement du Bronze RÃ©cent. Diffusion de l'alphabet phÃ©nicien, essor des citÃ©s-Ã‰tats grecques, expansion de l'Empire NÃ©o-Assyrien et dynastie Zhou en Chine.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ DÃ©mocratisation de l'outillage et des armes grÃ¢ce Ã  l'abondance gÃ©ologique du minerai de fer.
            â€¢ Essor du commerce maritime thalassocratique en MÃ©diterranÃ©e (PhÃ©niciens, Grecs, Ã‰trusques).
            â€¢ Rupture de la dÃ©pendance stratÃ©gique au cuivre/Ã©tain et multiplication des centres de pouvoir rÃ©gionaux.
            """);
        java.util.Map<String, Boolean> sIronEngines = sIron.getTypeBEngineStates();
        sIronEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sIronEngines.put("TurchinGoldstoneSDTEngine", true);
        sIronEngines.put("FrontierAsabiyyahEngine", true);
        sIronEngines.put("MaritimeHighwayEngine", true);
        sIronEngines.put("DynamicMaritimeRoutingGraph", true);
        sIronEngines.put("SpatialCityFractalEngine", true);
        sIronEngines.put("TainterComplexityCollapseEngine", true);
        sIronEngines.put("KrugmanCorePeripheryEngine", true);
        sIronEngines.put("ArthurCombinatorialTechnologyEngine", true);
        list.add(sIron);

        // --- SCÃ‰NARIO : EXPANSION HELLÃ‰NISTIQUE & CHOC D'ALEXANDRE (-334) ---
        Scenario sAlexander = new Scenario();
        sAlexander.setPresetKey("alexander_hellenistic_334bc");
        sAlexander.setName("Expansion HellÃ©nistique & Choc d'Alexandre le Grand (-334)");
        sAlexander.setStartDateYear(-334);
        sAlexander.setEndDateYear(-150);
        sAlexander.setInitialHumanCount(45000000);
        sAlexander.setInitialCapitalPerCapita(220.0);
        sAlexander.setInitialEnergyPerCapita(95.0);
        sAlexander.setInitialFoodReserveMonths(5.0);
        sAlexander.setInitialInformationPerCapita(250.0);
        sAlexander.setPopulationDensityType("URBAN_CLUSTERS");
        sAlexander.setTargetCohortSize(800);
        sAlexander.setPlanetPreset(PlanetPreset.EARTH_IRON_1000BP);
        sAlexander.setEcologyPreset(EcologyPreset.EARTH_IRON_1000BP);
        sAlexander.setDescription("""
            âš¡ SCÃ‰NARIO HISTORIQUE : ConquÃªte MacÃ©donienne & Mondialisation HellÃ©nistique (-334 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise l'effondrement foudroyant de l'Empire AchÃ©mÃ©nide sous la phalange macÃ©donienne d'Alexandre. IntÃ©gration d'un corridor urbain et monÃ©taire unifiÃ© de la GrÃ¨ce Ã  l'Indus (fondation d'Alexandries, monÃ©tarisation de l'or perse, koinÃ¨ grecque).
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Vitesse de projection militaire et rÃ©duction spectaculaire de la friction logistique le long des routes royales perses.
            â€¢ MonÃ©tarisation massive et urbanisation grecque en Orient (Alexandrie, SÃ©leucie, Antioche).
            â€¢ Fragmentation politique immÃ©diate post-Alexandre (guerres des Diadoques) sans destruction du rÃ©seau urbain commercial.
            """);
        java.util.Map<String, Boolean> sAlexEngines = sAlexander.getTypeBEngineStates();
        sAlexEngines.put("MilitaryTechShockEngine", true);
        sAlexEngines.put("FrontierAsabiyyahEngine", true);
        sAlexEngines.put("DynamicMaritimeRoutingGraph", true);
        sAlexEngines.put("MaritimeHighwayEngine", true);
        sAlexEngines.put("SpatialCityFractalEngine", true);
        sAlexEngines.put("KrugmanCorePeripheryEngine", true);
        sAlexEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sAlexEngines.put("LanchesterKineticWarfareEngine", true);
        list.add(sAlexander);

        // --- SCÃ‰NARIO : EMPIRE MAURYA & INDE (-300) ---
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
        sMaurya.setPlanetPreset(PlanetPreset.EARTH_IRON_1000BP);
        sMaurya.setEcologyPreset(EcologyPreset.EARTH_IRON_1000BP);
        sMaurya.setClippingEnabled(true);
        sMaurya.setMinLat(8.0); sMaurya.setMaxLat(35.0); sMaurya.setMinLng(68.0); sMaurya.setMaxLng(90.0);
        sMaurya.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMaurya.setDescription("""
            â˜¸ï¸ SCÃ‰NARIO HISTORIQUE : L'Empire Maurya d'Ashoka & La VallÃ©e du Gange (-300 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Unification du sous-continent indien sous Chandragupta et Ashoka. ModÃ©lise l'agriculture rizicole de la plaine gÃ¢ngÃ©tique, les routes commerciales de la Soie et le rÃ©seau urbain autour de Pataliputra et Taxila.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Densification dÃ©mographique extrÃªme de la plaine indo-gangÃ©tique grÃ¢ce au riz irriguÃ©.
            â€¢ Diffusion des Ã©dits impÃ©riaux et baisse institutionnelle de la violence (pacification d'Ashoka).
            â€¢ IntÃ©gration commerciale transasiatique le long de la Grand Trunk Road et des ports de l'ocÃ©an Indien.
            """);
        java.util.Map<String, Boolean> sMauryaEngines = sMaurya.getTypeBEngineStates();
        sMauryaEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sMauryaEngines.put("ScottAgainstTheGrainPureEngine", true);
        sMauryaEngines.put("TurchinGoldstoneSDTEngine", true);
        sMauryaEngines.put("FrontierAsabiyyahEngine", true);
        sMauryaEngines.put("SpatialCityFractalEngine", true);
        sMauryaEngines.put("SelfDomesticationEngine", true);
        sMauryaEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sMauryaEngines.put("GranovetterThresholdCascadeEngine", true);
        sMauryaEngines.put("OstromCommonsPureEngine", true);
        list.add(sMaurya);

        // --- SCÃ‰NARIO : EMPIRE ROMAIN & PAX ROMANA (AN 0) ---
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
        sRoman.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sRoman.setClippingEnabled(true);
        sRoman.setMinLat(25.0); sRoman.setMaxLat(55.0); sRoman.setMinLng(-10.0); sRoman.setMaxLng(45.0);
        sRoman.setBoundaryMode("DYNAMIC_RESERVOIR");
        java.util.Map<String, Boolean> sRomanEngines = sRoman.getTypeBEngineStates();
        sRomanEngines.put("RomanImperialCliodynamicEngine", true);
        sRomanEngines.put("TurchinGoldstoneSDTEngine", true);
        sRomanEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sRomanEngines.put("FrontierAsabiyyahEngine", true);
        sRomanEngines.put("MaritimeHighwayEngine", true);
        sRomanEngines.put("DynamicMaritimeRoutingGraph", true);
        sRomanEngines.put("SpatialCityFractalEngine", true);
        sRomanEngines.put("TainterComplexityCollapseEngine", true);
        sRomanEngines.put("BioMolecularEpidemiologyEngine", true);
        sRomanEngines.put("SpatialMetapopulationSEIREngine", true);
        sRomanEngines.put("DeforestationErosionEngine", true);
        sRomanEngines.put("KrugmanCorePeripheryEngine", true);
        sRomanEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sRomanEngines.put("GranovetterThresholdCascadeEngine", true);
        sRoman.setDescription("""
            ðŸ›ï¸ SCÃ‰NARIO HISTORIQUE : L'Empire Romain Ã  son ApogÃ©e (Pax Romana, An 0)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE - SOURCES BESSES & BENCHMARKS CIA / SESHAT / HYDE]
            ModÃ©lise le bassin mÃ©diterranÃ©en au moment de la Pax Romana sous Auguste. IntÃ¨gre les donnÃ©es dÃ©mographiques historiques (55 millions d'habitants), les rÃ©seaux d'infrastructures (viae, aqueducs) et les dynamiques cliodynamiques de Turchin.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ DÃ©ploiement des infrastructures de transport (rÃ©seau viaire terrestre et autoroutes maritimes mÃ©diterranÃ©ennes).
            â€¢ Cycles cliodynamiques de Turchin (surproduction des Ã©lites, instabilitÃ© politique, dÃ©valuation monÃ©taire du denier).
            â€¢ Impact conjuguÃ© des chocs climatiques (Optimum Romain vers refroidissement) et des Ã©pidÃ©mies (Peste Antonine).
            """);
        list.add(sRoman);

        Scenario s2 = new Scenario();
        s2.setPresetKey("late_antique_ice_age");
        s2.setName("Le Petit Ã‚ge Glaciaire de l'AntiquitÃ© Tardive & Peste de Justinien (536)");
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
        s2.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s2.setDescription("""
            ðŸŒ‹ SCÃ‰NARIO HISTORIQUE : L'Anomalie Climatique Volcanique de 536 & Choc Sanitaire
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            L'annÃ©e 536 est considÃ©rÃ©e par les historiens du climat comme "la pire annÃ©e de l'histoire humaine". Deux Ã©ruptions volcaniques super-massives consÃ©cutives ont injectÃ© un voile d'aÃ©rosols stratosphÃ©riques occultant le Soleil pendant 18 mois.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Chute brutale des tempÃ©ratures estivales (-2Â°C Ã  -3Â°C) et effondrement des rÃ©coltes cÃ©rÃ©aliÃ¨res eurasiennes.
            â€¢ Propagation fulgurante de la Peste de Justinien (Yersinia pestis) le long des routes de commerce maritime.
            â€¢ DÃ©stabilisation militaire des frontiÃ¨res impÃ©riales byzantines et grandes migrations de peuples des steppes.
            """);
        java.util.Map<String, Boolean> s2Engines = s2.getTypeBEngineStates();
        s2Engines.put("BioMolecularEpidemiologyEngine", true);
        s2Engines.put("SpatialMetapopulationSEIREngine", true);
        s2Engines.put("FrontierAsabiyyahEngine", true);
        s2Engines.put("MonasticDemographicBufferEngine", true);
        s2Engines.put("TurchinGoldstoneSDTEngine", true);
        s2Engines.put("BoserupAgriculturalIntensificationEngine", true);
        s2Engines.put("TainterComplexityCollapseEngine", true);
        s2Engines.put("MaritimeHighwayEngine", true);
        s2Engines.put("DynamicMaritimeRoutingGraph", true);
        list.add(s2);

        // --- SCÃ‰NARIO : EXPANSION ISLAMIQUE & RÃ‰VOLUTION COMMERCIALE CALIFALE (632) ---
        Scenario sIslam = new Scenario();
        sIslam.setPresetKey("islamic_expansion_632");
        sIslam.setName("Expansion Islamique & RÃ©volution Commerciale Califale (632)");
        sIslam.setStartDateYear(632);
        sIslam.setEndDateYear(900);
        sIslam.setInitialHumanCount(70000000);
        sIslam.setInitialCapitalPerCapita(320.0);
        sIslam.setInitialEnergyPerCapita(140.0);
        sIslam.setInitialFoodReserveMonths(6.0);
        sIslam.setInitialInformationPerCapita(500.0);
        sIslam.setPopulationDensityType("URBAN_CLUSTERS");
        sIslam.setTargetCohortSize(1000);
        sIslam.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sIslam.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sIslam.setDescription("""
            ðŸŒ™ SCÃ‰NARIO HISTORIQUE : Unification Califale & RÃ©volution Agricole Arabe (632 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise l'expansion foudroyante des Califats Omeyyade et Abbasside de l'Espagne Ã  l'Indus. Effondrement de l'Empire Sassanide, diffusion massive des techniques d'irrigation (qanats, norias) et des cultures tropicales (canne Ã  sucre, coton, agrumes), essor des rÃ©seaux maritimes de l'OcÃ©an Indien.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Vitesse d'unification gÃ©opolitique portÃ©e par la haute Asabiyyah bÃ©douine et la doctrine califale.
            â€¢ RÃ©volution agricole arabe : diversification agronomique et maÃ®trise hydraulique des zones arides.
            â€¢ Essor de Bagdad comme mÃ©tropole mondiale (Maison de la Sagesse) et intÃ©gration commerciale transcontinentale.
            """);
        java.util.Map<String, Boolean> sIslamEngines = sIslam.getTypeBEngineStates();
        sIslamEngines.put("FrontierAsabiyyahEngine", true);
        sIslamEngines.put("HydrologicalEngineeringEngine", true);
        sIslamEngines.put("DynamicMaritimeRoutingGraph", true);
        sIslamEngines.put("MaritimeHighwayEngine", true);
        sIslamEngines.put("SpatialCityFractalEngine", true);
        sIslamEngines.put("KrugmanCorePeripheryEngine", true);
        sIslamEngines.put("OstromCommonsPureEngine", true);
        sIslamEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sIslamEngines.put("ArthurCombinatorialTechnologyEngine", true);
        list.add(sIslam);

        Scenario s4 = new Scenario();
        s4.setPresetKey("song_dynasty");
        s4.setName("Dynastie Song & PrÃ©-Industrialisation Hydraulique (1000)");
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
        s4.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s4.setDescription("""
            ðŸ® SCÃ‰NARIO HISTORIQUE : Le SiÃ¨cle d'Or de la Dynastie Song (1000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            La Chine des Song a connu la premiÃ¨re prÃ©-industrialisation de l'histoire, avec une utilisation massive du charbon de terre pour la fonte du fer et des rÃ©seaux de transport fluviaux ultra-efficaces.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ RÃ©volution Ã©nergÃ©tique prÃ©-industrielle : transition massive vers le charbon minÃ©ral et hauts-fourneaux au coke.
            â€¢ RÃ©volution monÃ©taire et commerciale : premiÃ¨re monnaie fiduciaire en papier (Jiaozi) et navigation Ã  la boussole.
            â€¢ Hyper-urbanisation fluviale (Kaifeng, Hangzhou) et vulnÃ©rabilitÃ© militaire face aux cavaliers nomades du Nord.
            """);
        java.util.Map<String, Boolean> s4Engines = s4.getTypeBEngineStates();
        s4Engines.put("BoserupAgriculturalIntensificationEngine", true);
        s4Engines.put("TurchinGoldstoneSDTEngine", true);
        s4Engines.put("AcemogluRobinsonInstitutionsEngine", true);
        s4Engines.put("MilitaryTechShockEngine", true);
        s4Engines.put("DynamicMaritimeRoutingGraph", true);
        s4Engines.put("MaritimeHighwayEngine", true);
        s4Engines.put("SpatialCityFractalEngine", true);
        s4Engines.put("HydrologicalEngineeringEngine", true);
        s4Engines.put("KrugmanCorePeripheryEngine", true);
        s4Engines.put("ArthurCombinatorialTechnologyEngine", true);
        s4Engines.put("GranovetterThresholdCascadeEngine", true);
        s4Engines.put("WestBettencourtAllometryEngine", true);
        list.add(s4);

        // --- SCÃ‰NARIO : L'EMPIRE MONGOL & LA GRANDE RUPTURE EURASIENNE (1206) ---
        Scenario sMongol = new Scenario();
        sMongol.setPresetKey("mongol_conquest_1206");
        sMongol.setName("L'Empire Mongol & la Grande Rupture Eurasienne (1206)");
        sMongol.setStartDateYear(1206);
        sMongol.setEndDateYear(1368);
        sMongol.setInitialHumanCount(110000000);
        sMongol.setInitialCapitalPerCapita(400.0);
        sMongol.setInitialEnergyPerCapita(250.0);
        sMongol.setInitialFoodReserveMonths(5.0);
        sMongol.setInitialInformationPerCapita(600.0);
        sMongol.setPopulationDensityType("EURASIA_STEPPE");
        sMongol.setTargetCohortSize(1500);
        sMongol.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sMongol.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sMongol.setDescription("""
            ðŸ¹ SCÃ‰NARIO HISTORIQUE : ConquÃªte Mongole de Gengis Khan & Pax Mongolica (1206 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise le plus grand empire territorial contigu de l'histoire humaine. Choc cinÃ©tique et dÃ©mographique majeur en Asie centrale, Perse et Chine, suivi de l'unification sÃ©curisÃ©e de la Route de la Soie (Pax Mongolica) qui servira de vecteur Ã  la Peste Noire.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Projection militaire nomade ultra-rapide (cavalerie lÃ©gÃ¨re, rÃ©seau de relais de poste Yam).
            â€¢ Choc de mortalitÃ© urbaine et dÃ©sertification de certaines oasis irriguÃ©es de Perse/Transoxiane.
            â€¢ IntÃ©gration Ã©conomique trans-eurasienne de PÃ©kin Ã  Tabriz et la mer Noire.
            """);
        java.util.Map<String, Boolean> sMongolEngines = sMongol.getTypeBEngineStates();
        sMongolEngines.put("MilitaryTechShockEngine", true);
        sMongolEngines.put("FrontierAsabiyyahEngine", true);
        sMongolEngines.put("TurchinGoldstoneSDTEngine", true);
        sMongolEngines.put("KrugmanCorePeripheryEngine", true);
        sMongolEngines.put("LanchesterKineticWarfareEngine", true);
        sMongolEngines.put("SpatialMetapopulationSEIREngine", true);
        sMongolEngines.put("BioMolecularEpidemiologyEngine", true);
        list.add(sMongol);

        // --- SCÃ‰NARIO : EMPIRE DU MALI (1324) ---
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
        sMali.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sMali.setClippingEnabled(true);
        sMali.setMinLat(5.0); sMali.setMaxLat(25.0); sMali.setMinLng(-18.0); sMali.setMaxLng(15.0);
        sMali.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMali.setDescription("""
            ðŸ•Œ SCÃ‰NARIO HISTORIQUE : L'ApogÃ©e de l'Empire du Mali sous Mansa Musa (1324 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise le rÃ©seau urbain et marchand trans-saharien de la boucle du Niger (Tombouctou, Gao, DjennÃ©). ContrÃ´le des mines d'or de Bambouk/Boure et des salines de Teghaza.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Flux caravaniers trans-sahariens de mÃ©taux prÃ©cieux (or) contre sel gemme, textiles et manuscrits.
            â€¢ Essor acadÃ©mique et thÃ©ologique de Tombouctou (UniversitÃ© SankorÃ©) et sÃ©dentarisation sahÃ©lienne.
            â€¢ Choc monÃ©taire mondial provoquÃ© par les dÃ©penses d'or de Mansa Musa lors de son pÃ¨lerinage au Caire.
            """);
        java.util.Map<String, Boolean> sMaliEngines = sMali.getTypeBEngineStates();
        sMaliEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sMaliEngines.put("TurchinGoldstoneSDTEngine", true);
        sMaliEngines.put("FrontierAsabiyyahEngine", true);
        sMaliEngines.put("AsymmetricColonialTradeEngine", true);
        sMaliEngines.put("SpatialCityFractalEngine", true);
        sMaliEngines.put("OstromCommonsPureEngine", true);
        sMaliEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sMaliEngines.put("GranovetterThresholdCascadeEngine", true);
        list.add(sMali);

        // --- SCÃ‰NARIO : LA PESTE NOIRE & INVERSION POST-FÃ‰ODALE (1347) ---
        Scenario sBlackDeath = new Scenario();
        sBlackDeath.setPresetKey("black_death_1347");
        sBlackDeath.setName("La Peste Noire & Inversion Ã‰conomique Post-FÃ©odale (1347)");
        sBlackDeath.setStartDateYear(1347);
        sBlackDeath.setEndDateYear(1450);
        sBlackDeath.setInitialHumanCount(375000000);
        sBlackDeath.setInitialCapitalPerCapita(350.0);
        sBlackDeath.setInitialEnergyPerCapita(150.0);
        sBlackDeath.setInitialFoodReserveMonths(4.0);
        sBlackDeath.setInitialInformationPerCapita(450.0);
        sBlackDeath.setPopulationDensityType("URBAN_CLUSTERS");
        sBlackDeath.setTargetCohortSize(2000);
        sBlackDeath.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sBlackDeath.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sBlackDeath.setDescription("""
            ðŸ’€ SCÃ‰NARIO HISTORIQUE : La PandÃ©mie de Peste Noire & Inversion du Rapport Capital/Travail (1347 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Vague pandÃ©mique foudroyante de Yersinia pestis anÃ©antissant 35% Ã  60% de la population europÃ©enne et moyen-orientale en moins de 5 ans. Choc dÃ©mographique provoquant la raretÃ© soudaine de la main-d'Å“uvre, la hausse spectaculaire des salaires rÃ©els et l'effondrement du servage.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Vitesse de contagion mÃ©tapopulationnelle le long des routes de commerce gÃ©noises et vÃ©nitiennes.
            â€¢ Inversion institutionnelle : Ã©mancipation paysanne en Europe occidentale vs second servage Ã  l'Est.
            â€¢ DÃ©prise agricole temporaire, reforestation spontanÃ©e et baisse des rentes fonciÃ¨res seigneuriales.
            """);
        java.util.Map<String, Boolean> sBDEngines = sBlackDeath.getTypeBEngineStates();
        sBDEngines.put("SpatialMetapopulationSEIREngine", true);
        sBDEngines.put("BioMolecularEpidemiologyEngine", true);
        sBDEngines.put("TurchinGoldstoneSDTEngine", true);
        sBDEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sBDEngines.put("GranovetterThresholdCascadeEngine", true);
        sBDEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sBDEngines.put("DynamicMaritimeRoutingGraph", true);
        list.add(sBlackDeath);

        // --- SCÃ‰NARIO : AMÃ‰RIQUES PRÃ‰COLOMBIENNES (1491) ---
        Scenario sAmericas1491 = new Scenario();
        sAmericas1491.setPresetKey("americas_1491");
        sAmericas1491.setName("AmÃ©riques PrÃ©colombiennes : Tawantinsuyu & Anahuac (1491)");
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
        sAmericas1491.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sAmericas1491.setClippingEnabled(true);
        sAmericas1491.setMinLat(-45.0); sAmericas1491.setMaxLat(30.0); sAmericas1491.setMinLng(-110.0); sAmericas1491.setMaxLng(-35.0);
        sAmericas1491.setBoundaryMode("DYNAMIC_RESERVOIR");
        sAmericas1491.setDescription("""
            ðŸŒ½ SCÃ‰NARIO HISTORIQUE : Les AmÃ©riques Ã  la Veille du Contact (1491 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise les grands empires prÃ©colombiens (Empire Inca du Tawantinsuyu, Empire AztÃ¨que de la Triple Alliance) et les sociÃ©tÃ©s Mississippiennes avant la rupture Ã©pidÃ©mique.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Optimisation Ã©cologique des paysages anthropiques (terra preta amazonienne, terrasses andines, canaux chinampas).
            â€¢ DensitÃ©s urbaines prÃ©colombiennes culminantes dans les bassins de Mexico (Tenochtitlan) et de Cuzco.
            â€¢ Absence d'immunitÃ© croisÃ©e et vulnÃ©rabilitÃ© maximale face aux pathogÃ¨nes de l'Ancien Monde.
            """);
        java.util.Map<String, Boolean> sAmericas1491Engines = sAmericas1491.getTypeBEngineStates();
        sAmericas1491Engines.put("BoserupAgriculturalIntensificationEngine", true);
        sAmericas1491Engines.put("TurchinGoldstoneSDTEngine", true);
        sAmericas1491Engines.put("AmerindianEcosystemEngine", true);
        sAmericas1491Engines.put("HydrologicalEngineeringEngine", true);
        sAmericas1491Engines.put("SpatialCityFractalEngine", true);
        sAmericas1491Engines.put("OstromCommonsPureEngine", true);
        sAmericas1491Engines.put("TainterComplexityCollapseEngine", true);
        sAmericas1491Engines.put("DeforestationErosionEngine", true);
        list.add(sAmericas1491);

        // --- SCÃ‰NARIO : CHOC DU CONTACT PRÃ‰COLOMBIEN (1492) ---
        Scenario sColumbian = new Scenario();
        sColumbian.setPresetKey("columbian_contact");
        sColumbian.setName("ArrivÃ©e des EuropÃ©ens aux AmÃ©riques & Choc Microbiens (1492)");
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
        sColumbian.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sColumbian.setDescription("""
            â›µ SCÃ‰NARIO HISTORIQUE : Le Choc du Contact d'Ã‰change Colombien & Effondrement Ã‰pidÃ©mique (1492)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise l'impact bio-dÃ©mographique mondial de la rencontre entre l'Ancien et le Nouveau Monde. Trajectoire de choc microbiologique (chute dÃ©mographique de 80-90% du continent amÃ©ricain) et rÃ©organisation commerciale transatlantique.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Chute dÃ©mographique cataclysmique (80 Ã  90%) des populations amÃ©rindiennes sous l'effet de la variole et de la rougeole.
            â€¢ Ã‰change colombien global : diffusion mondiale du maÃ¯s, de la pomme de terre, du manioc et de l'argent du PotosÃ­.
            â€¢ Reforestation spontanÃ©e des terres abandonnÃ©es et baisse temporaire du COâ‚‚ atmosphÃ©rique mondial (Orbis Spike).
            """);
        java.util.Map<String, Boolean> sColumbianEngines = sColumbian.getTypeBEngineStates();
        sColumbianEngines.put("BioMolecularEpidemiologyEngine", true);
        sColumbianEngines.put("SpatialMetapopulationSEIREngine", true);
        sColumbianEngines.put("AsymmetricColonialTradeEngine", true);
        sColumbianEngines.put("DynamicMaritimeRoutingGraph", true);
        sColumbianEngines.put("MaritimeHighwayEngine", true);
        sColumbianEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sColumbianEngines.put("AmerindianEcosystemEngine", true);
        list.add(sColumbian);

        // --- SCÃ‰NARIO : JAPON EDO & SAKOKU (1639) ---
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
        sSakoku.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sSakoku.setClippingEnabled(true);
        sSakoku.setMinLat(30.0); sSakoku.setMaxLat(45.0); sSakoku.setMinLng(128.0); sSakoku.setMaxLng(146.0);
        sSakoku.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSakoku.setDescription("""
            â›©ï¸ SCÃ‰NARIO HISTORIQUE : L'Ãˆre d'Isolement Autarcique Tokugawa (Sakoku, 1639 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Fermeture des frontiÃ¨res de l'archipel japonais dÃ©crÃ©tÃ©e par le Shogunat Tokugawa. ModÃ©lise une Ã©conomie circulaire hautement autarcique, l'urbanisation gÃ©ante d'Edo (Tokyo, 1 million d'habitants) et l'absence d'intrants extÃ©rieurs jusqu'Ã  l'arrivÃ©e des bateaux noirs du Commandant Perry en 1853.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ StabilitÃ© dÃ©mographique et gestion rigoureuse des ressources forestiÃ¨res et agricoles en boucle fermÃ©e.
            â€¢ ModÃ¨le de croissance zÃ©ro durable sans dÃ©pendance aux importations Ã©nergÃ©tiques ou minÃ©rales extÃ©rieures.
            â€¢ Urbanisation pacifiÃ©e d'Edo et floraison artisanale et culturelle sous autarcie stricte.
            """);
        java.util.Map<String, Boolean> sSakokuEngines = sSakoku.getTypeBEngineStates();
        sSakokuEngines.put("EdoJapanIsolationEngine", true);
        sSakokuEngines.put("OstromCommonsPureEngine", true);
        sSakokuEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sSakokuEngines.put("TurchinGoldstoneSDTEngine", true);
        sSakokuEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sSakokuEngines.put("SpatialCityFractalEngine", true);
        sSakokuEngines.put("DeforestationErosionEngine", true);
        list.add(sSakoku);

        // --- SCÃ‰NARIO : RÃ‰VOLUTION INDUSTRIELLE (1800) ---
        Scenario sIndustrial1800 = new Scenario();
        sIndustrial1800.setPresetKey("industrial_1800");
        sIndustrial1800.setName("RÃ©volution Industrielle & Transition CharbonniÃ¨re (1800)");
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
        sIndustrial1800.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sIndustrial1800.setDescription("""
            âš™ï¸ SCÃ‰NARIO HISTORIQUE : La Machine Ã  Vapeur & L'Ã‰mergence du Charbon (1800 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Basculement Ã©nergÃ©tique mondial du rÃ©gime organique vers le rÃ©gime minÃ©ral fossile (charbon de terre, machine Ã  vapeur de Watt).
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Rupture du piÃ¨ge malthusien : dÃ©couplage entre croissance dÃ©mographique et contrainte surfacique des sols.
            â€¢ Paradoxe de Jevons : les gains d'efficacitÃ© des machines Ã  vapeur dÃ©cuplent la consommation globale de charbon.
            â€¢ Urbanisation industrielle ultra-rapide (exode rural) et amorce des Ã©missions anthropiques massives de COâ‚‚.
            """);
        java.util.Map<String, Boolean> sIndustrial1800Engines = sIndustrial1800.getTypeBEngineStates();
        sIndustrial1800Engines.put("KummelAyresExergyEngine", true);
        sIndustrial1800Engines.put("SmilMaterialTransitionsPureEngine", true);
        sIndustrial1800Engines.put("JevonsParadoxEngine", true);
        sIndustrial1800Engines.put("EntropicMetalDissipationEngine", true);
        sIndustrial1800Engines.put("ProtestantWorkEthicEngine", true);
        sIndustrial1800Engines.put("UrbanThermodynamicsEngine", true);
        sIndustrial1800Engines.put("AcemogluRobinsonInstitutionsEngine", true);
        sIndustrial1800Engines.put("ArthurCombinatorialTechnologyEngine", true);
        sIndustrial1800Engines.put("WestBettencourtAllometryEngine", true);
        sIndustrial1800Engines.put("KrugmanCorePeripheryEngine", true);
        sIndustrial1800Engines.put("HotellingResourceDepletionEngine", true);
        list.add(sIndustrial1800);

        // --- SCÃ‰NARIO : GUERRES MONDIALES, RUPTURE TOTALITAIRE & ÃˆRE NUCLÃ‰AIRE (1914) ---
        Scenario sWW = new Scenario();
        sWW.setPresetKey("world_wars_totalitarian_1914");
        sWW.setName("Guerres Mondiales, Rupture Totalitaire & Ãˆre NuclÃ©aire (1914)");
        sWW.setStartDateYear(1914);
        sWW.setEndDateYear(1960);
        sWW.setInitialHumanCount(1750000000L);
        sWW.setInitialCapitalPerCapita(3500.0);
        sWW.setInitialEnergyPerCapita(4500.0);
        sWW.setInitialFoodReserveMonths(5.0);
        sWW.setInitialInformationPerCapita(80000.0);
        sWW.setPopulationDensityType("URBAN_CLUSTERS");
        sWW.setTargetCohortSize(8000);
        sWW.setPlanetPreset(PlanetPreset.EARTH_LIKE);
        sWW.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sWW.setDescription("""
            âš”ï¸ SCÃ‰NARIO HISTORIQUE : Guerre Industrielle Totale, Ruptures IdÃ©ologiques & Bombe Atomique (1914 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            ModÃ©lise la pÃ©riode de crise systÃ©mique paroxystique de la modernitÃ© industrielle (1914-1945). Mobilisation intÃ©grale de l'exergie fossile et chimique (synthÃ¨se Haber-Bosch), ruptures totalitaires (1917, 1933), destruction massive de capital en Europe/Asie, et franchissement du seuil de destruction thermonuclÃ©aire (1945).
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ DÃ©ploiement de la guerre industrielle et de la puissance de feu cinÃ©tique (artillerie lourde, aviation, blindÃ©s).
            â€¢ Ruptures de rÃ©gime idÃ©ologiques extrÃªmes (collectivisme soviÃ©tique, militarisme fasciste) et purges dÃ©mographiques.
            â€¢ Chute brutale du capital en 1939-1945 suivie de la reconstruction fordiste accÃ©lÃ©rÃ©e des Trente Glorieuses.
            """);
        java.util.Map<String, Boolean> sWWEngines = sWW.getTypeBEngineStates();
        sWWEngines.put("LanchesterKineticWarfareEngine", true);
        sWWEngines.put("SmilMaterialTransitionsPureEngine", true);
        sWWEngines.put("KummelAyresExergyEngine", true);
        sWWEngines.put("TurchinGoldstoneSDTEngine", true);
        sWWEngines.put("BifurcationChaosEngine", true);
        sWWEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sWWEngines.put("NuclearSafetyRadiotoxicityEngine", true);
        sWWEngines.put("JevonsParadoxEngine", true);
        sWWEngines.put("WestBettencourtAllometryEngine", true);
        list.add(sWW);

        // --- SCÃ‰NARIO : ANTHROPOCÃˆNE (2000) ---
        Scenario sModern2000 = new Scenario();
        sModern2000.setPresetKey("anthropocene_2000");
        sModern2000.setName("AnthropocÃ¨ne & Grande AccÃ©lÃ©ration Mondiale (2000)");
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
        sModern2000.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sModern2000.setDescription("""
            ðŸŒ SCÃ‰NARIO HISTORIQUE : L'Ãˆre NumÃ©rique & La Grande AccÃ©lÃ©ration (2000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Consolidation du systÃ¨me Ã©conomique mondial interconnectÃ©, essor des microprocesseurs en silicium, de l'Internet mondial et de l'urbanisation globale.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Grande AccÃ©lÃ©ration : croissance exponentielle des flux de matiÃ¨re, d'Ã©nergie fossile et d'information numÃ©rique.
            â€¢ Franchissement des limites planÃ©taires (Cycle de l'azote/phosphore, forÃ§age radiatif COâ‚‚, Ã©rosion de biodiversitÃ©).
            â€¢ Rendements dÃ©croissants de la complexitÃ© institutionnelle et fragilitÃ© des chaÃ®nes logistiques just-in-time.
            """);
        java.util.Map<String, Boolean> sModern2000Engines = sModern2000.getTypeBEngineStates();
        sModern2000Engines.put("KummelAyresExergyEngine", true);
        sModern2000Engines.put("SmilMaterialTransitionsPureEngine", true);
        sModern2000Engines.put("World3HybridEngine", true);
        sModern2000Engines.put("EcotoxicologyFertilityEngine", true);
        sModern2000Engines.put("UrbanThermodynamicsEngine", true);
        sModern2000Engines.put("KurzweilAcceleratingReturnsEngine", true);
        sModern2000Engines.put("AcemogluRobinsonInstitutionsEngine", true);
        sModern2000Engines.put("ArthurCombinatorialTechnologyEngine", true);
        sModern2000Engines.put("WestBettencourtAllometryEngine", true);
        sModern2000Engines.put("KrugmanCorePeripheryEngine", true);
        sModern2000Engines.put("HotellingResourceDepletionEngine", true);
        sModern2000Engines.put("OreGradeThermodynamicsEngine", true);
        sModern2000Engines.put("JevonsParadoxEngine", true);
        sModern2000Engines.put("EntropicMetalDissipationEngine", true);
        sModern2000Engines.put("BioMolecularEpidemiologyEngine", true);
        list.add(sModern2000);

        // --- SCÃ‰NARIOS DU FUTUR ---
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
        s5.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s5.setDescription("""
            ðŸ“‰ SCÃ‰NARIO FUTUR : Business As Usual (Trajectoire GIEC SSP5-8.5)
            
            [DESCRIPTION DES TERMES DE FORÃ‡AGE PHYSIQUE (Tâ‚€)]
            Poursuite de l'extraction des combustibles fossiles traditionnels sans dÃ©ploiement massif de la fusion ni captage du carbone.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Trajectoire de concentration COâ‚‚ non rÃ©gulÃ©e (>1000 ppm en 2100) et hausse thermique globale >4Â°C.
            â€¢ Submersion marine des mÃ©tropoles cÃ´tiÃ¨res et stress thermique lÃ©tal (tempÃ©rature thermomÃ¨tre mouillÃ© Tw > 35Â°C).
            â€¢ Effondrement des rendements agricoles tropicaux et vagues de rÃ©fugiÃ©s climatiques vers les hautes latitudes.
            """);
        java.util.Map<String, Boolean> s5Engines = s5.getTypeBEngineStates();
        s5Engines.put("KummelAyresExergyEngine", true);
        s5Engines.put("SmilMaterialTransitionsPureEngine", true);
        s5Engines.put("World3HybridEngine", true);
        s5Engines.put("MarineSubmersionEngine", true);
        s5Engines.put("GeoengineeringAlbedoFeedbackEngine", true);
        s5Engines.put("UrbanThermodynamicsEngine", true);
        s5Engines.put("EcotoxicologyFertilityEngine", true);
        s5Engines.put("HotellingResourceDepletionEngine", true);
        s5Engines.put("OreGradeThermodynamicsEngine", true);
        s5Engines.put("JevonsParadoxEngine", true);
        list.add(s5);

        Scenario s7 = new Scenario();
        s7.setPresetKey("nuclear_winter_2035");
        s7.setName("Hiver NuclÃ©aire & Ombre StratosphÃ©rique (2035)");
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
        s7.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s7.setDescription("""
            â˜¢ï¸ SCÃ‰NARIO FUTUR : Catastrophe de la Guerre NuclÃ©aire & Hiver StratosphÃ©rique
            
            [DESCRIPTION DES TERMES DE FORÃ‡AGE PHYSIQUE (Tâ‚€)]
            Conflit nuclÃ©aire Ã  haute intensitÃ© dÃ©clenchant d'immenses tempÃªtes de feu urbaines et l'injection massive de carbone suie dans la stratosphÃ¨re.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Chute brutale de l'insolation solaire au sol (-70%) et refroidissement planÃ©taire de -15Â°C Ã  -25Â°C.
            â€¢ Effondrement total de la photosynthÃ¨se et rupture gÃ©nÃ©ralisÃ©e des chaÃ®nes alimentaires en moins de 60 jours.
            â€¢ Survie dÃ©mographique rÃ©siduelle restreinte aux refuges souterrains, biomes marins profonds et serres protÃ©gÃ©es.
            """);
        java.util.Map<String, Boolean> s7Engines = s7.getTypeBEngineStates();
        s7Engines.put("NuclearWarfareClimateEngine", true);
        s7Engines.put("NuclearSafetyRadiotoxicityEngine", true);
        s7Engines.put("BifurcationChaosEngine", true);
        s7Engines.put("VolcanicTephraRefugiaEngine", true);

        // Targeted Strategic Nuclear Strike Package (Anti-Forces & Anti-Cities)
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe StratÃ©gique : Washington D.C. & Pentagone", 2035, 38.88, -77.05, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Silos Minot AFB (Dakota du Nord)", 2035, 48.41, -101.35, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Silos Malmstrom AFB (Montana)", 2035, 47.50, -111.18, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Bunker : Complexe Cheyenne Mountain / NORAD", 2035, 38.74, -104.84, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-CitÃ©s : AgglomÃ©ration New York & Hub Maritime", 2035, 40.71, -74.00, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-CitÃ©s : MÃ©galopole Los Angeles / Long Beach", 2035, 34.05, -118.24, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe StratÃ©gique : Moscou & Centre de Commandement", 2035, 55.75, 37.61, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Base Bombardiers Engels-2 (Saratov)", 2035, 51.48, 46.21, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Champs de Silos ICBM Kozelsk", 2035, 54.04, 35.80, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Navale : Chantiers Sous-Marins Severodvinsk", 2035, 64.56, 39.83, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-CitÃ©s : MÃ©tropole Saint-PÃ©tersbourg & Baltique", 2035, 59.93, 30.33, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe StratÃ©gique : Londres & AmirautÃ© Britannique", 2035, 51.50, -0.12, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Navale : Base SNLE Faslane / HMNB Clyde", 2035, 56.06, -4.81, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe StratÃ©gique : Paris & Commandement des Forces AÃ©riennes", 2035, 48.85, 2.35, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Navale : Base SNLE ÃŽle Longue (Brest)", 2035, 48.30, -4.50, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe StratÃ©gique : PÃ©kin & Commission Militaire Centrale", 2035, 39.90, 116.40, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Silos DF-41 Yumen / Hami (Gansu)", 2035, 40.28, 97.04, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-CitÃ©s : MÃ©galopole Industrielle Shanghai / Yangtze", 2035, 31.23, 121.47, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe StratÃ©gique : Tokyo & Complexe Industriel Kanto", 2035, 35.68, 139.69, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Base StratÃ©gique Andersen (Guam)", 2035, 13.58, 144.92, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Base AÃ©rienne Ramstein (OTAN)", 2035, 49.43, 7.60, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe StratÃ©gique : New Delhi & Centres StratÃ©giques", 2035, 28.61, 77.20, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe StratÃ©gique : Islamabad & Complexe NuclÃ©aire Kahuta", 2035, 33.68, 73.04, 0.0, 8.0));

        list.add(s7);

        Scenario s6 = new Scenario();
        s6.setPresetKey("singularity_2045");
        s6.setName("SingularitÃ© Technologique, ASI & Fusion D-T (2045)");
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
        s6.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s6.setDescription("""
            ðŸ¤– SCÃ‰NARIO FUTUR : SingularitÃ© Technologique & Ã‰nergie de Fusion D-T
            
            [DESCRIPTION DES TERMES DE FORÃ‡AGE PHYSIQUE (Tâ‚€)]
            Franchissement du seuil d'Ã©mergence d'une Super-Intelligence Artificielle (ASI) et maÃ®trise industrielle de la fusion nuclÃ©aire deutÃ©rium-tritium.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Croissance exponentielle des rendements de la recherche et automatisation intÃ©grale du travail physique.
            â€¢ DÃ©ploiement massif de rÃ©acteurs Ã  fusion deutÃ©rium-tritium confÃ©rant une abondance Ã©nergÃ©tique quasi-infinie.
            â€¢ Ã‰limination des pÃ©nuries matÃ©rielles, transition vers l'Ã©chelle de Kardashev Type I et rÃ©gulation systÃ©mique.
            """);
        java.util.Map<String, Boolean> s6Engines = s6.getTypeBEngineStates();
        s6Engines.put("TechnologicalSingularityEngine", true);
        s6Engines.put("AiAutonomousRegulationPureEngine", true);
        s6Engines.put("KurzweilAcceleratingReturnsEngine", true);
        s6Engines.put("KardashevPureEngine", true);
        s6Engines.put("ArthurCombinatorialTechnologyEngine", true);
        s6Engines.put("AcemogluRobinsonInstitutionsEngine", true);
        list.add(s6);

        Scenario s8 = new Scenario();
        s8.setPresetKey("peak_phosphate_2050");
        s8.setName("Falaise du Phosphate MinÃ©ral & Crise N-P-K (2050)");
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
        s8.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s8.setDescription("""
            â›ï¸ SCÃ‰NARIO FUTUR : Ã‰puisement du Phosphate de Roche (Peak P 2050)
            
            [DESCRIPTION DES TERMES DE FORÃ‡AGE PHYSIQUE (Tâ‚€)]
            Ã‰puisement gÃ©ologique complet des gisements de phosphate de roche bon marchÃ© sans transition vers un recyclage circulaire intÃ©gral.
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Chute inexorable des rendements agricoles sous dÃ©ficit de fertilisation phosphatÃ©e minÃ©rale (stÅ“chiomÃ©trie N-P-K).
            â€¢ FlambÃ©e des prix alimentaires mondiaux, crises de famine urbaine et tensions gÃ©opolitiques autour des derniers gisements.
            â€¢ Re-localisation agricole d'urgence, baisse de la population vers la capacitÃ© de charge organique et recyclage des flux.
            """);
        java.util.Map<String, Boolean> s8Engines = s8.getTypeBEngineStates();
        s8Engines.put("World3HybridEngine", true);
        s8Engines.put("EcotoxicologyFertilityEngine", true);
        s8Engines.put("SmilMaterialTransitionsPureEngine", true);
        s8Engines.put("HotellingResourceDepletionEngine", true);
        s8Engines.put("OreGradeThermodynamicsEngine", true);
        s8Engines.put("TainterComplexityCollapseEngine", true);
        list.add(s8);

        Scenario s9 = new Scenario();
        s9.setPresetKey("supervolcano_2060");
        s9.setName("Super-Ã‰ruption Volcanique Toba/Yellowstone (2060)");
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
        s9.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s9.setDescription("""
            ðŸŒ‹ SCÃ‰NARIO FUTUR : Super-Volcan VEI-8 & Refroidissement Vulcanologique
            
            [DESCRIPTION DES TERMES DE FORÃ‡AGE PHYSIQUE (Tâ‚€)]
            Ã‰ruption super-volcanique de degrÃ© VEI-8 Ã©jectant plus de 1000 kmÂ³ de cendres et de dioxyde de soufre (SOâ‚‚) dans la haute atmosphÃ¨re.
            
            [CONDITIONS INITIALES PHYSIQUES (Tâ‚€)]
            â€¢ Stock Capital Physique (Kâ‚€) : 25 000 kg/habitant (infrastructures avancÃ©es et serres automatisÃ©es).
            â€¢ Ã‰nergie StockÃ©e (Eâ‚€) : 20 000 MJ/habitant (centrales nuclÃ©aires et gÃ©othermiques).
            â€¢ RÃ©serves Alimentaires (Fâ‚€) : 3,0 mois (destructions agricoles par cendres).
            â€¢ Savoir ArchivÃ© (Iâ‚€) : 5 000 000 bits/habitant (savoir automatisÃ© & archives).
            
            [OBSERVABLES CLÃ‰S DU SCÃ‰NARIO]
            â€¢ Hiver volcanique pluriannuel (baisse de 8Â°C Ã  12Â°C des tempÃ©ratures globales pendant 5 Ã  10 ans).
            â€¢ DÃ©pÃ´ts massifs de tÃ©phras toxiques dÃ©truisant les sols agricoles, les toitures urbaines et les rÃ©seaux Ã©lectriques.
            â€¢ DÃ©ploiement d'une logistique de crise alimentaire basÃ©e sur les stocks stratÃ©giques et les cultures protÃ©gÃ©es.
            """);
        java.util.Map<String, Boolean> s9Engines = s9.getTypeBEngineStates();
        s9Engines.put("VolcanicTephraRefugiaEngine", true);
        s9Engines.put("BifurcationChaosEngine", true);
        s9Engines.put("World3HybridEngine", true);
        list.add(s9);

        // --- EXOPLANETARY & FUTURE EXPANSION SCENARIOS (2050+) ---

        // 1. MARS (2050)
        Scenario sMars = new Scenario();
        sMars.setPresetKey("mars_colony_2050");
        sMars.setName("Colonisation Martienne : Dômes Chryse & Cratère Jezero (2050)");
        sMars.setStartDateYear(2050);
        sMars.setEndDateYear(2150);
        sMars.setInitialHumanCount(50000L);
        sMars.setInitialCapitalPerCapita(120000.0);
        sMars.setInitialEnergyPerCapita(80000.0);
        sMars.setInitialFoodReserveMonths(36.0);
        sMars.setInitialInformationPerCapita(50000000.0);
        sMars.setPopulationDensityType("URBAN_CLUSTERS");
        sMars.setTargetCohortSize(500);
        sMars.setPlanetPreset(PlanetPreset.MARS_LIKE);
        sMars.setEcologyPreset(EcologyPreset.MARS_LIKE);
        sMars.setDescription("""
            🔴 SCÉNARIO EXOPLANÉTAIRE : Colonisation Martienne & Dômes Sous Régolithe (2050 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Premiers habitats humains permanents au cratère Jezero et Chryse Planitia. Utilisation du cycle de Sabatier (méthanation CO₂), extraction d'eau in-situ, dômes imprimés 3D en basalte et support de vie ECLSS en boucle fermée face à une pression quasi-nulle (0,006 atm) et aux rayonnements ionisants.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Déploiement d'avant-postes pressurisés et croissance du stock de capital technique et d'énergie par habitant.
            • Autarcie progressive grâce à l'ISRU (In-Situ Resource Utilization) et aux technologies combinatoires.
            • Résilience face aux tempêtes de poussière martiennes et aux risques de brèche structurelle.
            """);
        java.util.Map<String, Boolean> sMarsEngines = sMars.getTypeBEngineStates();
        sMarsEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sMarsEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sMarsEngines.put("KurzweilAcceleratingReturnsEngine", true);
        sMarsEngines.put("OreGradeThermodynamicsEngine", true);
        sMarsEngines.put("HotellingResourceDepletionEngine", true);
        list.add(sMars);

        // 2. MOON (2050)
        Scenario sMoon = new Scenario();
        sMoon.setPresetKey("moon_shackleton_2050");
        sMoon.setName("Base Lunaire Shackleton & Tubes de Lave Marius Hills (2050)");
        sMoon.setStartDateYear(2050);
        sMoon.setEndDateYear(2130);
        sMoon.setInitialHumanCount(25000L);
        sMoon.setInitialCapitalPerCapita(150000.0);
        sMoon.setInitialEnergyPerCapita(100000.0);
        sMoon.setInitialFoodReserveMonths(48.0);
        sMoon.setInitialInformationPerCapita(60000000.0);
        sMoon.setPopulationDensityType("URBAN_CLUSTERS");
        sMoon.setTargetCohortSize(250);
        sMoon.setPlanetPreset(PlanetPreset.MOON_LIKE);
        sMoon.setEcologyPreset(EcologyPreset.MOON_LIKE);
        sMoon.setDescription("""
            ⚪ SCÉNARIO EXOPLANÉTAIRE : Base Lunaire Polaire & Tubes de Lave Sous-Terrain (2050 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Implantation humaine permanente au pôle Sud lunaire (cratère Shackleton) pour l'extraction de glace d'eau et sanctuarisation dans les tunnels de lave de Marius Hills sous vide absolu.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Exploitation industrielle de la glace polaire et de l'Hélium-3 pour les réacteurs à fusion.
            • Habitats pressurisés enfouis dans les cavités basaltiques assurant un blindage thermique et radiatif total.
            • Hub logistique pour l'expansion cis-lunaire et les chantiers orbitaux.
            """);
        java.util.Map<String, Boolean> sMoonEngines = sMoon.getTypeBEngineStates();
        sMoonEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sMoonEngines.put("KurzweilAcceleratingReturnsEngine", true);
        sMoonEngines.put("OreGradeThermodynamicsEngine", true);
        sMoonEngines.put("ArthurCombinatorialTechnologyEngine", true);
        list.add(sMoon);

        // 3. VENUS (2060)
        Scenario sVenus = new Scenario();
        sVenus.setPresetKey("venus_cloud_cities_2060");
        sVenus.setName("Cités Flottantes Vénusiennes : Aérostats de Haute Altitude (2060)");
        sVenus.setStartDateYear(2060);
        sVenus.setEndDateYear(2160);
        sVenus.setInitialHumanCount(30000L);
        sVenus.setInitialCapitalPerCapita(180000.0);
        sVenus.setInitialEnergyPerCapita(120000.0);
        sVenus.setInitialFoodReserveMonths(36.0);
        sVenus.setInitialInformationPerCapita(70000000.0);
        sVenus.setPopulationDensityType("URBAN_CLUSTERS");
        sVenus.setTargetCohortSize(300);
        sVenus.setPlanetPreset(PlanetPreset.VENUS_LIKE);
        sVenus.setEcologyPreset(EcologyPreset.VENUS_LIKE);
        sVenus.setDescription("""
            🟡 SCÉNARIO EXOPLANÉTAIRE : Aérostats Flottants & Extraction Atmosphérique Vénusienne (2060 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Cités aérostatiques flottantes à 55 km d'altitude où la pression est de 1,0 atm et la température de 25°C. Enveloppes en PTFE résistant à l'acide sulfurique et raffinage du carbone atmosphérique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Exploitation de la flottabilité naturelle de l'air respirable (N₂/O₂) comme gaz sustentateur dans une atmosphère de CO₂ dense.
            • Récolte de vapeur d'eau et de composés soufrés par condensation en haute altitude.
            • Équilibre aérostatique dynamique et résistance à la super-rotation zonale des vents.
            """);
        java.util.Map<String, Boolean> sVenusEngines = sVenus.getTypeBEngineStates();
        sVenusEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sVenusEngines.put("KurzweilAcceleratingReturnsEngine", true);
        sVenusEngines.put("ArthurCombinatorialTechnologyEngine", true);
        list.add(sVenus);

        // 4. MERCURY (2070)
        Scenario sMercury = new Scenario();
        sMercury.setPresetKey("mercury_caloris_forge_2070");
        sMercury.setName("Forge Solaire de Mercure & Bassin Caloris (2070)");
        sMercury.setStartDateYear(2070);
        sMercury.setEndDateYear(2170);
        sMercury.setInitialHumanCount(15000L);
        sMercury.setInitialCapitalPerCapita(250000.0);
        sMercury.setInitialEnergyPerCapita(300000.0);
        sMercury.setInitialFoodReserveMonths(48.0);
        sMercury.setInitialInformationPerCapita(80000000.0);
        sMercury.setPopulationDensityType("URBAN_CLUSTERS");
        sMercury.setTargetCohortSize(200);
        sMercury.setPlanetPreset(PlanetPreset.MERCURY_LIKE);
        sMercury.setEcologyPreset(EcologyPreset.MERCURY_LIKE);
        sMercury.setDescription("""
            ⚪ SCÉNARIO EXOPLANÉTAIRE : Forges Héliothermiques de Mercure & Métaux Lourds de Caloris (2070 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Avant-postes miniers souterrains exploitant les gisements géants de métaux denses du bassin Caloris et la concentration solaire extrême (10 kW/m²) pour faisceaux d'énergie orbitaux.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Exploitation du flux solaire paroxystique pour la métallurgie lourde et la synthèse de matériaux ultra-denses.
            • Habitats mobiles de terminateur ou cités souterraines profondes protégées des gradients thermiques jour/nuit extrêmes.
            • Exportation d'énergie et de métaux lourds vers le reste du système solaire par catapultes électromagnétiques.
            """);
        java.util.Map<String, Boolean> sMercuryEngines = sMercury.getTypeBEngineStates();
        sMercuryEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sMercuryEngines.put("OreGradeThermodynamicsEngine", true);
        sMercuryEngines.put("HotellingResourceDepletionEngine", true);
        sMercuryEngines.put("KardashevPureEngine", true);
        list.add(sMercury);

        // 5. TITAN (2080)
        Scenario sTitan = new Scenario();
        sTitan.setPresetKey("titan_cryo_methane_2080");
        sTitan.setName("Colonie Cryogénique de Titan & Hydrocarbures Kraken Mare (2080)");
        sTitan.setStartDateYear(2080);
        sTitan.setEndDateYear(2180);
        sTitan.setInitialHumanCount(20000L);
        sTitan.setInitialCapitalPerCapita(220000.0);
        sTitan.setInitialEnergyPerCapita(150000.0);
        sTitan.setInitialFoodReserveMonths(60.0);
        sTitan.setInitialInformationPerCapita(90000000.0);
        sTitan.setPopulationDensityType("URBAN_CLUSTERS");
        sTitan.setTargetCohortSize(250);
        sTitan.setPlanetPreset(PlanetPreset.TITAN_LIKE);
        sTitan.setEcologyPreset(EcologyPreset.TITAN_LIKE);
        sTitan.setDescription("""
            🪐 SCÉNARIO EXOPLANÉTAIRE : Cryocités de Titan & Mers d'Hydrocarbures de Kraken Mare (2080 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Habitats cryogéniques pressurisés au bord de Kraken Mare exploitant les mers de méthane/éthane liquide, l'épaisse couverture atmosphérique d'azote (1,45 atm) et les isotopes de fusion.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Richesse organique illimitée : chimie des tholins, plastiques et carburants de synthèse à coût quasi-nul.
            • Protection radiatif naturelle grâce à l'atmosphère épaisse (1,45 atm) sous très faible gravité (0,14g).
            • Utilisation de générateurs thermoélectriques nucléaires et de réacteurs à fusion deutérium pour le chauffage vital.
            """);
        java.util.Map<String, Boolean> sTitanEngines = sTitan.getTypeBEngineStates();
        sTitanEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sTitanEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sTitanEngines.put("KurzweilAcceleratingReturnsEngine", true);
        list.add(sTitan);

        // 6. SUPER-EARTH (2100)
        Scenario sSuperEarth = new Scenario();
        sSuperEarth.setPresetKey("super_earth_gaia_2100");
        sSuperEarth.setName("Arche Interstellaire : Première Colonisation de Gaia Prime (2100)");
        sSuperEarth.setStartDateYear(2100);
        sSuperEarth.setEndDateYear(2250);
        sSuperEarth.setInitialHumanCount(500000L);
        sSuperEarth.setInitialCapitalPerCapita(80000.0);
        sSuperEarth.setInitialEnergyPerCapita(90000.0);
        sSuperEarth.setInitialFoodReserveMonths(36.0);
        sSuperEarth.setInitialInformationPerCapita(100000000.0);
        sSuperEarth.setPopulationDensityType("URBAN_CLUSTERS");
        sSuperEarth.setTargetCohortSize(1000);
        sSuperEarth.setPlanetPreset(PlanetPreset.SUPER_EARTH);
        sSuperEarth.setEcologyPreset(EcologyPreset.SUPER_EARTH);
        sSuperEarth.setDescription("""
            🌍 SCÉNARIO EXOPLANÉTAIRE : Colonisation d'une Super-Terre à Forte Gravité (2100 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Arrivée d'une flotte d'arches générationnelles sur une Super-Terre massive (gravité 1,5g, pression 1,5 atm, biosphère foisonnante). Implantation modulaire haute résistance face aux contraintes tectoniques.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Adaptation biomécanique et exosquelettes face à une gravité élevée et une densité atmosphérique supérieure.
            • Exploitation d'une biomasse extraterrestre exubérante et de gisements géologiques super-abondants.
            • Essor de cités-dômes modulaires et émergence de nouvelles institutions coloniales autonomes.
            """);
        java.util.Map<String, Boolean> sSuperEarthEngines = sSuperEarth.getTypeBEngineStates();
        sSuperEarthEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sSuperEarthEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sSuperEarthEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sSuperEarthEngines.put("KurzweilAcceleratingReturnsEngine", true);
        list.add(sSuperEarth);

        // 7. EYEBALL WORLD (2120)
        Scenario sEyeball = new Scenario();
        sEyeball.setPresetKey("eyeball_world_twilight_2120");
        sEyeball.setName("Monde Synchrone : L'Anneau du Crépuscule & Terminateur (2120)");
        sEyeball.setStartDateYear(2120);
        sEyeball.setEndDateYear(2250);
        sEyeball.setInitialHumanCount(200000L);
        sEyeball.setInitialCapitalPerCapita(100000.0);
        sEyeball.setInitialEnergyPerCapita(150000.0);
        sEyeball.setInitialFoodReserveMonths(30.0);
        sEyeball.setInitialInformationPerCapita(80000000.0);
        sEyeball.setPopulationDensityType("URBAN_CLUSTERS");
        sEyeball.setTargetCohortSize(1000);
        sEyeball.setPlanetPreset(PlanetPreset.EYEBALL_WORLD);
        sEyeball.setEcologyPreset(EcologyPreset.EYEBALL_WORLD);
        sEyeball.setDescription("""
            👁️ SCÉNARIO EXOPLANÉTAIRE : Civilisation de l'Anneau Crépusculaire Synchrone (2120 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Colonisation d'une planète en verrouillage gravitationnel autour d'une naine rouge. Population concentrée exclusivement sur l'étroite bande tempérée du terminateur entre brasier permanent et nuit glacée.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Climat statique hyper-contrasté avec vents catabatiques puissants redistribuant l'énergie de la face éclairée vers la face sombre.
            • Ceinture de mégalopoles thermiquement isolées exploitant l'énergie solaire ininterrompue côté jour et les glaces permanentes côté nuit.
            • Équilibre précaire des flux hydrologiques et gestion rigoureuse de la capacité de charge locale.
            """);
        java.util.Map<String, Boolean> sEyeballEngines = sEyeball.getTypeBEngineStates();
        sEyeballEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sEyeballEngines.put("KurzweilAcceleratingReturnsEngine", true);
        sEyeballEngines.put("World3HybridEngine", true);
        list.add(sEyeball);

        // 8. WATER WORLD / OCEANIA (2090)
        Scenario sOceania = new Scenario();
        sOceania.setPresetKey("oceania_aquapolis_2090");
        sOceania.setName("Monde Océan : Aquapolis & Cités Abyssales d'Oceania (2090)");
        sOceania.setStartDateYear(2090);
        sOceania.setEndDateYear(2200);
        sOceania.setInitialHumanCount(350000L);
        sOceania.setInitialCapitalPerCapita(90000.0);
        sOceania.setInitialEnergyPerCapita(80000.0);
        sOceania.setInitialFoodReserveMonths(36.0);
        sOceania.setInitialInformationPerCapita(75000000.0);
        sOceania.setPopulationDensityType("URBAN_CLUSTERS");
        sOceania.setTargetCohortSize(1000);
        sOceania.setPlanetPreset(PlanetPreset.WATER_WORLD);
        sOceania.setEcologyPreset(EcologyPreset.WATER_WORLD);
        sOceania.setDescription("""
            🌊 SCÉNARIO EXOPLANÉTAIRE : Planète Océan & Mégastructures Flottantes (2090 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Monde dépourvu de terres émergées couvert d'océans profonds de 10 km. Civilisation organisée en cités flottantes autonomes (Aquapolis), énergie thermique des mers (ETM) et forages hydrothermaux abyssaux.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Mariculture hyper-productive, fermes d'algues génétiquement optimisées et extraction de minéraux dissous.
            • Cités modulaires auto-lestées résistantes aux houles géantes et aux super-tempêtes océaniques.
            • Économie maritime décentralisée et réseaux de transport sous-marin automatisés.
            """);
        java.util.Map<String, Boolean> sOceaniaEngines = sOceania.getTypeBEngineStates();
        sOceaniaEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sOceaniaEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sOceaniaEngines.put("UrbanThermodynamicsEngine", true);
        list.add(sOceania);

        // 9. ICE WORLD / BOREAS (2075)
        Scenario sBoreas = new Scenario();
        sBoreas.setPresetKey("boreas_subglacial_2075");
        sBoreas.setName("Monde Glaciaire Boreas : Havres Géothermiques & Cités Sous Glace (2075)");
        sBoreas.setStartDateYear(2075);
        sBoreas.setEndDateYear(2175);
        sBoreas.setInitialHumanCount(100000L);
        sBoreas.setInitialCapitalPerCapita(110000.0);
        sBoreas.setInitialEnergyPerCapita(140000.0);
        sBoreas.setInitialFoodReserveMonths(40.0);
        sBoreas.setInitialInformationPerCapita(70000000.0);
        sBoreas.setPopulationDensityType("URBAN_CLUSTERS");
        sBoreas.setTargetCohortSize(500);
        sBoreas.setPlanetPreset(PlanetPreset.ICE_WORLD);
        sBoreas.setEcologyPreset(EcologyPreset.ICE_WORLD);
        sBoreas.setDescription("""
            ❄️ SCÉNARIO EXOPLANÉTAIRE : Cités Sous-Glaciaires & Puits Géothermiques de Boreas (2075 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Planète boule de neige cryogénique où l'humanité s'abrite dans des cavités volcaniques sous-glaciaires chauffées par le flux géothermique du manteau sous des kilomètres de banquise.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Exploitation du gradient thermique entre la croûte volcanique profonde et la calotte de surface.
            • Cités troglodytiques taillées dans la glace consolidée et la roche ignée avec biosphères hydroponiques étanches.
            • Économie d'énergie thermique maximale et recyclage thermodynamique intégral.
            """);
        java.util.Map<String, Boolean> sBoreasEngines = sBoreas.getTypeBEngineStates();
        sBoreasEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        sBoreasEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sBoreasEngines.put("HotellingResourceDepletionEngine", true);
        list.add(sBoreas);

        // 10. ARCHIPELAGO / SEASTEADING (2055)
        Scenario sArchipelago = new Scenario();
        sArchipelago.setPresetKey("archipelago_seasteading_2055");
        sArchipelago.setName("Archipel : Confédération Océanique & Seasteading Autonome (2055)");
        sArchipelago.setStartDateYear(2055);
        sArchipelago.setEndDateYear(2150);
        sArchipelago.setInitialHumanCount(2500000L);
        sArchipelago.setInitialCapitalPerCapita(45000.0);
        sArchipelago.setInitialEnergyPerCapita(50000.0);
        sArchipelago.setInitialFoodReserveMonths(18.0);
        sArchipelago.setInitialInformationPerCapita(40000000.0);
        sArchipelago.setPopulationDensityType("URBAN_CLUSTERS");
        sArchipelago.setTargetCohortSize(5000);
        sArchipelago.setPlanetPreset(PlanetPreset.ARCHIPELAGO);
        sArchipelago.setEcologyPreset(EcologyPreset.ARCHIPELAGO);
        sArchipelago.setDescription("""
            🏝️ SCÉNARIO FUTUR : Micro-États Insulaires & Réseaux de Seasteading (2055 ap. J.-C.)
            
            [DESCRIPTION DES CONDITIONS INITIALES (T₀)]
            Monde fragmenté en milliers d'atolls tropicaux et de plates-formes artificielles de seasteading reliées par hydroptères solaires et gouvernance polycentrique décentralisée.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Forte résilience institutionnelle décentralisée et innovation polycentrique (théorie d'Elinor Ostrom).
            • Mix énergétique 100% renouvelable combinant énergie houlomotrice, solaire flottant et biocarburants marins.
            • Équilibre écologique entre préservation des récifs coralliens et densité urbaine insulaire.
            """);
        java.util.Map<String, Boolean> sArchipelagoEngines = sArchipelago.getTypeBEngineStates();
        sArchipelagoEngines.put("AcemogluRobinsonInstitutionsEngine", true);
        sArchipelagoEngines.put("ArthurCombinatorialTechnologyEngine", true);
        sArchipelagoEngines.put("UrbanThermodynamicsEngine", true);
        sArchipelagoEngines.put("IsruAutarkyAndSpaceColonizationEngine", true);
        list.add(sArchipelago);

        // Sort all canonical presets chronologically by start year
        list.sort(java.util.Comparator.comparingLong(Scenario::getStartDateYear));

        // Enforce precalculated map import mode for all canonical built-in scenarios
        for (Scenario sc : list) {
            if (sc.getTensorProceduralModes() == null || sc.getTensorProceduralModes().isEmpty()) {
                java.util.List<Boolean> modes = new java.util.ArrayList<>();
                int dims = sc.getCultureVectorDimensions() > 0 ? sc.getCultureVectorDimensions() : 9;
                for (int i = 0; i < dims; i++) {
                    modes.add(false); // false = Import of precalculated map
                }
                sc.setTensorProceduralModes(modes);
            }
        }

        return list;
    }
}

