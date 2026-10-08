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
 * <h1>Scenario</h1>
 * <p>
 * Core operational component for the Ether civilizational and planetary simulation framework.<br>
 * Integrates cellular dynamics, data structures, and deterministic state transitions.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class Scenario implements Serializable {
    private static final long serialVersionUID = 1L;

    /* Internal state variable for name (String). */
    private String name;
    /* Internal state variable for description (String). */
    private String description;
    private Long id;
    /* Internal state variable for preset key (String). */
    private String presetKey;

    /*
     * Get preset key.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public String getPresetKey() {
        return presetKey;
    }

    /*
     * Set preset key.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param presetKey the preset key parameter (String)
     */
    public void setPresetKey(String presetKey) {
        this.presetKey = presetKey;
    }

    /*
     * Get display name.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
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

    /*
     * Get display description.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
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

    /**
     * Resolves the canonical preset key for this scenario.
     *
     * @return normalized preset key string
     */
    public String resolvePresetKey() {
        if (presetKey != null && !presetKey.isBlank()) return presetKey;
        if (name == null) return null;
        String n = name.toLowerCase();
        if (n.contains("out of africa") || n.contains("sortie d'afrique")) return "out_of_africa";
        if (n.contains("toba")) return "toba_cataclysm_74k";
        if (n.contains("sahul")) return "sahul";
        if (n.contains("beringia") || n.contains("béringie")) return "beringia";
        if (n.contains("solutrean") || n.contains("solutréen") || n.contains("lgm")) return "lgm_solutrean";
        if (n.contains("dryas")) return "younger_dryas";
        if (n.contains("fertile crescent") || n.contains("croissant fertile")) return "fertile_crescent";
        if (n.contains("vert") || n.contains("green sahara")) return "green_sahara";
        if (n.contains("egypte") || n.contains("égypte") || n.contains("egypt")) return "ancient_egypt";
        if (n.contains("ramses") || n.contains("ramsès") || n.contains("ramesses")) return "ramesses_ii";
        if (n.contains("assyrian") || n.contains("assyrien")) return "assyrian_empire";
        if (n.contains("mesoamerica") || n.contains("mésoamérique") || n.contains("olmeques") || n.contains("olmèques")) return "mesoamerica";
        if (n.contains("bronze age collapse") || (n.contains("effondrement") && n.contains("bronze"))) return "bronze_age_collapse_1200bc";
        if (n.contains("early iron age") || n.contains("premier age du fer") || n.contains("premier âge du fer")) return "early_iron_age";
        if (n.contains("alexander") || n.contains("alexandre")) return "alexander_hellenistic_334bc";
        if (n.contains("maurya")) return "maurya_empire";
        if (n.contains("roman empire") || n.contains("empire romain")) return "roman_empire";
        if (n.contains("late antique") || n.contains("glaciaire antique") || n.contains("536")) return "late_antique_ice_age";
        if (n.contains("islamic") || n.contains("islamique")) return "islamic_expansion_632";
        if (n.contains("song")) return "song_dynasty";
        if (n.contains("mongol")) return "mongol_conquest_1206";
        if (n.contains("mali")) return "mali_empire";
        if (n.contains("black death") || n.contains("peste noire")) return "black_death_1347";
        if (n.contains("1491") || n.contains("americas") || n.contains("amériques")) return "americas_1491";
        if (n.contains("columbian") || n.contains("colombien")) return "columbian_contact";
        if (n.contains("sakoku") || n.contains("tokugawa")) return "tokugawa_japan";
        if (n.contains("industrial") || n.contains("industrielle")) return "industrial_1800";
        if (n.contains("totalitarian") || n.contains("totalitaire") || n.contains("1914")) return "world_wars_totalitarian_1914";
        if (n.contains("business as usual") || n.contains("tendancielle") || n.contains("2025") || n.contains("zero drilling") || n.contains("sans forage") || n.contains("moratoire")) return "earth_2025_business_as_usual";
        if (n.contains("ssp5") || n.contains("ssp5-8.5")) return "ssp5_85";
        if (n.contains("hiver nucleaire") || n.contains("hiver nucléaire") || n.contains("nuclear winter")) return "nuclear_winter_2035";
        if (n.contains("singularity") || n.contains("singularite") || n.contains("singularité")) return "singularity_2045";
        if (n.contains("phosphate")) return "peak_phosphate_2050";
        if (n.contains("supervolcan") || n.contains("supervolcano")) return "supervolcano_2060";
        if (n.contains("shackleton") || n.contains("marius") || n.contains("lune") || n.contains("moon")) return "moon_shackleton_2035";
        if (n.contains("mars")) return "mars_colony_2050";
        if (n.contains("venus") || n.contains("vénus") || n.contains("aerostat") || n.contains("aérostat") || n.contains("hesperos")) return "venus_cloud_cities_2080";
        if (n.contains("mercure") || n.contains("mercury") || n.contains("caloris") || n.contains("hermes")) return "mercury_caloris_forge_2120";
        if (n.contains("titan") || n.contains("kraken")) return "titan_cryo_methane_2150";
        if (n.contains("super-terre") || n.contains("super-earth") || n.contains("gaia")) return "super_earth_gaia_2200";
        if (n.contains("synchrone") || n.contains("eyeball") || n.contains("crépuscule") || n.contains("twilight")) return "eyeball_world_twilight_2220";
        if (n.contains("oceania") || n.contains("aquapolis") || n.contains("monde océan") || n.contains("water world")) return "oceania_aquapolis_2100";
        if (n.contains("boreas") || n.contains("glaciaire") || n.contains("ice world") || n.contains("subglacial")) return "boreas_subglacial_2120";
        if (n.contains("archipel") || n.contains("archipelago") || n.contains("seasteading")) return "archipelago_seasteading_2055";
        return null;
    }

    @Override
    /*
     * To string.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public String toString() {
        return getDisplayName();
    }

    // Planet Configuration
    private PlanetPreset planetPreset = PlanetPreset.EARTH_LIKE;
    private EcologyPreset ecologyPreset = EcologyPreset.EARTH_STANDARD;
    /* Internal state variable for ecology preset name (String). */
    private String ecologyPresetName = "Earth Standard Baseline";
    /* Internal state variable for use real earth data (boolean). */
    private boolean useRealEarthData;

    // Planet Physics
    private double planetRadiusKm; // Size
    /* Internal state variable for rotation period hours (double). */
    private double rotationPeriodHours;
    /* Internal state variable for revolution period days (double). */
    private double revolutionPeriodDays;
    private double axialTiltDegrees; // Inclination

    public enum TechPreset {
        AUTO_FROM_YEAR("â³ Automatique (CalculÃ© selon l'annÃ©e Tâ‚€)", -1, -1, -1, -1),
        PALEOLITHIC("ðŸ¹ Chasseurs-Cueilleurs / NÃ©olithique", 5.0, 10.0, 2.0, 5.0),
        NEOLITHIC_BRONZE("ðŸ›¡ï¸ Ã‚ge du Bronze & CitÃ©s-Ã‰tats", 25.0, 30.0, 4.0, 40.0),
        ANTIQUITY("ðŸ›ï¸ AntiquitÃ© Classique & Empire", 100.0, 60.0, 6.0, 200.0),
        RENAISSANCE("⛵ Renaissance & Imprimerie", 500.0, 300.0, 8.0, 2000.0),
        INDUSTRIAL("âš™ï¸ RÃ©volution Industrielle & Vapeur", 2500.0, 2500.0, 10.0, 15000.0),
        CONTEMPORARY("ðŸŒ Contemporain & MÃ©tropole NumÃ©rique", 15000.0, 50000.0, 18.0, 5000000.0),
        SPACE_COLONY_MARS("🚀 Colonie Spatiale / Mars (Faible Pop / Ultra High-Tech)", 50000.0, 200000.0, 24.0, 50000000.0),
        CUSTOM("âš™ï¸ PersonnalisÃ© (Saisie Libre des 4 Stocks)", -1, -1, -1, -1);

        /* Internal state variable for label (String). */
        private final String label;
        /* Internal state variable for capital (double). */
        private final double capital;
        /* Internal state variable for energy (double). */
        private final double energy;
        /* Internal state variable for food months (double). */
        private final double foodMonths;
        /* Internal state variable for info (double). */
        private final double info;

        TechPreset(String label, double capital, double energy, double foodMonths, double info) {
            this.label = label;
            this.capital = capital;
            this.energy = energy;
            this.foodMonths = foodMonths;
            this.info = info;
        }

        /*
         * Get label.
         * Enforces physical invariants and updates associated state variables within {@code Scenario}.
         *
         * @return the resulting computation or state reference
         */
        public String getLabel() { return label; }
        /*
         * Get capital.
         * Enforces physical invariants and updates associated state variables within {@code Scenario}.
         *
         * @return the resulting computation or state reference
         */
        public double getCapital() { return capital; }
        /*
         * Get energy.
         * Enforces physical invariants and updates associated state variables within {@code Scenario}.
         *
         * @return the resulting computation or state reference
         */
        public double getEnergy() { return energy; }
        /*
         * Get food months.
         * Enforces physical invariants and updates associated state variables within {@code Scenario}.
         *
         * @return the resulting computation or state reference
         */
        public double getFoodMonths() { return foodMonths; }
        /*
         * Get info.
         * Enforces physical invariants and updates associated state variables within {@code Scenario}.
         *
         * @return the resulting computation or state reference
         */
        public double getInfo() { return info; }

        @Override
        /*
         * To string.
         * Enforces physical invariants and updates associated state variables within {@code Scenario}.
         *
         * @return the resulting computation or state reference
         */
        public String toString() { return label; }
    }

    private TechPreset techPreset = TechPreset.AUTO_FROM_YEAR;

    // Human Start Conditions
    /* Internal state variable for initial human count (long). */
    private long initialHumanCount;
    /* Internal state variable for initial tech level (double). */
    private double initialTechLevel;
    private double initialCapitalPerCapita = 10.0; // Physical capital & tools in kg/capita
    private double initialEnergyPerCapita = 50.0; // Fuel & stored energy in MJ/capita
    private double initialFoodReserveMonths = 6.0; // Stored food reserves in months of consumption
    private double initialInformationPerCapita = 100.0; // Stored knowledge/archive in bits/capita
    private String populationDensityType; // "ONE_CONTINENT", "DENSE", "SPARSE", "RIVER_VALLEYS"

    // Simulation Parameters
    /* Internal state variable for cell size km2 (double). */
    private double cellSizeKm2;
    private int targetCohortSize = 150; // Target population per demographic cohort node (1 to 10,000+, default 150 = Dunbar pivot)
    private double temporalResolutionDays = 30.0; // Temporal resolution time step Δt in days (default: 30.0 days = 1 month)
    private double climateHarshness; // 0.0 to 1.0 (storms, droughts)
    private long startDateYear; // e.g. -100000
    private long endDateYear = 100; // e.g. 100
    private long seed = 12345L; // Demographic density seed
    private long culturalSeed = 54321L; // Cultural tensor suite seed
    /* Internal state variable for random events enabled (boolean). */
    private boolean randomEventsEnabled = true;
    /* Internal state variable for earth historical leaders enabled (boolean). */
    private boolean earthHistoricalLeadersEnabled = true;
    /* Internal state variable for procedural leaders enabled (boolean). */
    private boolean proceduralLeadersEnabled = true;
    /* Internal state variable for custom density base64 (String). */
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
    /* Internal state variable for strict determinism (boolean). */
    private boolean strictDeterminism = true;
    /* Internal state variable for sparse cell skipping enabled (boolean). */
    private boolean sparseCellSkippingEnabled = false;
    /* Internal state variable for ocean macro aggregation enabled (boolean). */
    private boolean oceanMacroAggregationEnabled = false;
    /* Internal state variable for coastal navigation only enabled (boolean). */
    private boolean coastalNavigationOnlyEnabled = false;
    /* Internal state variable for ocean multi rate ticking enabled (boolean). */
    private boolean oceanMultiRateTickingEnabled = false;
    /* Internal state variable for parallel execution enabled (boolean). */
    private boolean parallelExecutionEnabled = false;
    /* Internal state variable for spatial range truncation enabled (boolean). */
    private boolean spatialRangeTruncationEnabled = false;

    // Type B Procedural & Cliodynamic Engine Checkbox States & Parameters (Persisted per Scenario)
    private java.util.Map<String, Boolean> typeBEngineStates = new java.util.HashMap<>();
    private java.util.Map<String, java.util.Map<String, Double>> typeBEngineParameters = new java.util.HashMap<>();

    // Scheduled Climate and Planetary Cataclysm Events
    private java.util.List<ClimateEvent> climateEvents = new java.util.ArrayList<>();

    // Spatial Clipping & Boundary Conditions
    /* Internal state variable for clipping enabled (boolean). */
    private boolean clippingEnabled = false;
    /* Internal state variable for min lat (double). */
    private double minLat = -90.0;
    /* Internal state variable for max lat (double). */
    private double maxLat = 90.0;
    /* Internal state variable for min lng (double). */
    private double minLng = -180.0;
    /* Internal state variable for max lng (double). */
    private double maxLng = 180.0;
    private String boundaryMode = "DYNAMIC_RESERVOIR"; // "DYNAMIC_RESERVOIR", "CLOSED_BARRIER", "PERIODIC_WRAP"

    /*
     * Scenario.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     */
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

    /*
     * Create default scenario.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     */
    public static Scenario createDefaultScenario() {
        return new Scenario();
    }

    /*
     * Copy.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
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

    /*
     * Calculates the default number of simulation ticks required to run this scenario from startDateYear to endDateYear.
     */
    public int calculateScenarioTicks() {
        long durationYears = Math.max(1, endDateYear - startDateYear);
        double ticksPerYear = 365.25 / Math.max(1.0, temporalResolutionDays);
        return (int) Math.clamp((long) (durationYears * ticksPerYear), 100L, 1000000L);
    }

    // Getters and Setters

    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return name;
    }

    /*
     * Set name.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param name the name parameter (String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
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

    /*
     * Set description.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param description the description parameter (String)
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /*
     * Get id.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public Long getId() {
        return id;
    }

    /*
     * Set id.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param id the id parameter (Long)
     */
    public void setId(Long id) {
        this.id = id;
    }

    /*
     * Get h3resolution.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public int getH3Resolution() {
        return h3Resolution > 0 ? h3Resolution : 3;
    }

    /*
     * Set h3resolution.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param h3Resolution the h3resolution parameter (int)
     */
    public void setH3Resolution(int h3Resolution) {
        this.h3Resolution = h3Resolution;
    }

    /*
     * Get planet preset.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public PlanetPreset getPlanetPreset() {
        return planetPreset;
    }

    /*
     * Set planet preset.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param planetPreset the planet preset parameter (PlanetPreset)
     */
    public void setPlanetPreset(PlanetPreset planetPreset) {
        this.planetPreset = planetPreset;
    }

    /*
     * Get ecology preset.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public EcologyPreset getEcologyPreset() {
        return ecologyPreset;
    }

    /*
     * Set ecology preset.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param ecologyPreset the ecology preset parameter (EcologyPreset)
     */
    public void setEcologyPreset(EcologyPreset ecologyPreset) {
        this.ecologyPreset = ecologyPreset;
    }

    /*
     * Get ecology preset name.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public String getEcologyPresetName() {
        return ecologyPresetName;
    }

    /*
     * Set ecology preset name.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param ecologyPresetName the ecology preset name parameter (String)
     */
    public void setEcologyPresetName(String ecologyPresetName) {
        this.ecologyPresetName = ecologyPresetName;
    }

    /*
     * Is use real earth data.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isUseRealEarthData() {
        return useRealEarthData;
    }

    /*
     * Set use real earth data.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param useRealEarthData the use real earth data parameter (boolean)
     */
    public void setUseRealEarthData(boolean useRealEarthData) {
        this.useRealEarthData = useRealEarthData;
    }

    /*
     * Get planet radius km.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getPlanetRadiusKm() {
        return planetRadiusKm;
    }

    /*
     * Set planet radius km.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param planetRadiusKm the planet radius km parameter (double)
     */
    public void setPlanetRadiusKm(double planetRadiusKm) {
        this.planetRadiusKm = planetRadiusKm;
    }

    /*
     * Get rotation period hours.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getRotationPeriodHours() {
        return rotationPeriodHours;
    }

    /*
     * Set rotation period hours.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param rotationPeriodHours the rotation period hours parameter (double)
     */
    public void setRotationPeriodHours(double rotationPeriodHours) {
        this.rotationPeriodHours = rotationPeriodHours;
    }

    /*
     * Get revolution period days.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getRevolutionPeriodDays() {
        return revolutionPeriodDays;
    }

    /*
     * Set revolution period days.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param revolutionPeriodDays the revolution period days parameter (double)
     */
    public void setRevolutionPeriodDays(double revolutionPeriodDays) {
        this.revolutionPeriodDays = revolutionPeriodDays;
    }

    /*
     * Get axial tilt degrees.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getAxialTiltDegrees() {
        return axialTiltDegrees;
    }

    /*
     * Set axial tilt degrees.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param axialTiltDegrees the axial tilt degrees parameter (double)
     */
    public void setAxialTiltDegrees(double axialTiltDegrees) {
        this.axialTiltDegrees = axialTiltDegrees;
    }

    /*
     * Get initial human count.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public long getInitialHumanCount() {
        return initialHumanCount;
    }

    /*
     * Set initial human count.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param initialHumanCount the initial human count parameter (long)
     */
    public void setInitialHumanCount(long initialHumanCount) {
        this.initialHumanCount = initialHumanCount;
    }

    /*
     * Get tech preset.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public TechPreset getTechPreset() {
        return techPreset != null ? techPreset : TechPreset.AUTO_FROM_YEAR;
    }

    /*
     * Set tech preset.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param techPreset the tech preset parameter (TechPreset)
     */
    public void setTechPreset(TechPreset techPreset) {
        this.techPreset = techPreset != null ? techPreset : TechPreset.AUTO_FROM_YEAR;
    }

    /*
     * Get initial tech level.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getInitialTechLevel() {
        return initialTechLevel;
    }

    /*
     * Set initial tech level.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param initialTechLevel the initial tech level parameter (double)
     */
    public void setInitialTechLevel(double initialTechLevel) {
        this.initialTechLevel = initialTechLevel;
    }

    /*
     * Get initial capital per capita.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getInitialCapitalPerCapita() {
        return initialCapitalPerCapita;
    }

    /*
     * Set initial capital per capita.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param initialCapitalPerCapita the initial capital per capita parameter (double)
     */
    public void setInitialCapitalPerCapita(double initialCapitalPerCapita) {
        this.initialCapitalPerCapita = initialCapitalPerCapita;
    }

    /*
     * Get initial energy per capita.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getInitialEnergyPerCapita() {
        return initialEnergyPerCapita;
    }

    /*
     * Set initial energy per capita.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param initialEnergyPerCapita the initial energy per capita parameter (double)
     */
    public void setInitialEnergyPerCapita(double initialEnergyPerCapita) {
        this.initialEnergyPerCapita = initialEnergyPerCapita;
    }

    /*
     * Get initial food reserve months.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getInitialFoodReserveMonths() {
        return initialFoodReserveMonths;
    }

    /*
     * Set initial food reserve months.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param initialFoodReserveMonths the initial food reserve months parameter (double)
     */
    public void setInitialFoodReserveMonths(double initialFoodReserveMonths) {
        this.initialFoodReserveMonths = initialFoodReserveMonths;
    }

    /*
     * Get initial information per capita.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getInitialInformationPerCapita() {
        return initialInformationPerCapita;
    }

    /*
     * Set initial information per capita.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param initialInformationPerCapita the initial information per capita parameter (double)
     */
    public void setInitialInformationPerCapita(double initialInformationPerCapita) {
        this.initialInformationPerCapita = initialInformationPerCapita;
    }

    /*
     * Get population density type.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public String getPopulationDensityType() {
        return populationDensityType;
    }

    /*
     * Set population density type.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param populationDensityType the population density type parameter (String)
     */
    public void setPopulationDensityType(String populationDensityType) {
        this.populationDensityType = populationDensityType;
    }

    /*
     * Get cell size km2.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getCellSizeKm2() {
        return cellSizeKm2;
    }

    /*
     * Set cell size km2.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param cellSizeKm2 the cell size km2 parameter (double)
     */
    public void setCellSizeKm2(double cellSizeKm2) {
        this.cellSizeKm2 = cellSizeKm2;
    }

    /*
     * Get target cohort size.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public int getTargetCohortSize() {
        return targetCohortSize;
    }

    /*
     * Set target cohort size.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param targetCohortSize the target cohort size parameter (int)
     */
    public void setTargetCohortSize(int targetCohortSize) {
        this.targetCohortSize = targetCohortSize;
    }

    /*
     * Get climate harshness.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getClimateHarshness() {
        return climateHarshness;
    }

    /*
     * Set climate harshness.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param climateHarshness the climate harshness parameter (double)
     */
    public void setClimateHarshness(double climateHarshness) {
        this.climateHarshness = climateHarshness;
    }

    /*
     * Get start date year.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public long getStartDateYear() {
        return startDateYear;
    }

    /*
     * Set start date year.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param startDateYear the start date year parameter (long)
     */
    public void setStartDateYear(long startDateYear) {
        this.startDateYear = startDateYear;
    }

    /*
     * Get end date year.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public long getEndDateYear() {
        return endDateYear;
    }

    /*
     * Set end date year.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param endDateYear the end date year parameter (long)
     */
    public void setEndDateYear(long endDateYear) {
        this.endDateYear = endDateYear;
    }

    /*
     * Get seed.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public long getSeed() {
        return seed;
    }

    /*
     * Set seed.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param seed the seed parameter (long)
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }

    /*
     * Get cultural seed.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public long getCulturalSeed() {
        return culturalSeed;
    }

    /*
     * Set cultural seed.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param culturalSeed the cultural seed parameter (long)
     */
    public void setCulturalSeed(long culturalSeed) {
        this.culturalSeed = culturalSeed;
    }

    /*
     * Is random events enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isRandomEventsEnabled() {
        return randomEventsEnabled;
    }

    /*
     * Set random events enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param randomEventsEnabled the random events enabled parameter (boolean)
     */
    public void setRandomEventsEnabled(boolean randomEventsEnabled) {
        this.randomEventsEnabled = randomEventsEnabled;
    }

    /*
     * Is earth historical leaders enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isEarthHistoricalLeadersEnabled() {
        return earthHistoricalLeadersEnabled;
    }

    /*
     * Set earth historical leaders enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param earthHistoricalLeadersEnabled the earth historical leaders enabled parameter (boolean)
     */
    public void setEarthHistoricalLeadersEnabled(boolean earthHistoricalLeadersEnabled) {
        this.earthHistoricalLeadersEnabled = earthHistoricalLeadersEnabled;
    }

    /*
     * Is procedural leaders enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isProceduralLeadersEnabled() {
        return proceduralLeadersEnabled;
    }

    /*
     * Set procedural leaders enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param proceduralLeadersEnabled the procedural leaders enabled parameter (boolean)
     */
    public void setProceduralLeadersEnabled(boolean proceduralLeadersEnabled) {
        this.proceduralLeadersEnabled = proceduralLeadersEnabled;
    }

    /*
     * Get custom density base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public String getCustomDensityBase64() {
        return customDensityBase64;
    }

    /*
     * Set custom density base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param customDensityBase64 the custom density base64 parameter (String)
     */
    public void setCustomDensityBase64(String customDensityBase64) {
        this.customDensityBase64 = customDensityBase64;
    }

    /*
     * Is clipping enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isClippingEnabled() {
        return clippingEnabled;
    }

    /*
     * Set clipping enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param clippingEnabled the clipping enabled parameter (boolean)
     */
    public void setClippingEnabled(boolean clippingEnabled) {
        this.clippingEnabled = clippingEnabled;
    }

    /*
     * Get min lat.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getMinLat() {
        return minLat;
    }

    /*
     * Set min lat.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param minLat the min lat parameter (double)
     */
    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    /*
     * Get max lat.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getMaxLat() {
        return maxLat;
    }

    /*
     * Set max lat.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param maxLat the max lat parameter (double)
     */
    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    /*
     * Get min lng.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getMinLng() {
        return minLng;
    }

    /*
     * Set min lng.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param minLng the min lng parameter (double)
     */
    public void setMinLng(double minLng) {
        this.minLng = minLng;
    }

    /*
     * Get max lng.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getMaxLng() {
        return maxLng;
    }

    /*
     * Set max lng.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param maxLng the max lng parameter (double)
     */
    public void setMaxLng(double maxLng) {
        this.maxLng = maxLng;
    }

    /*
     * Get boundary mode.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public String getBoundaryMode() {
        return boundaryMode;
    }

    /*
     * Set boundary mode.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param boundaryMode the boundary mode parameter (String)
     */
    public void setBoundaryMode(String boundaryMode) {
        this.boundaryMode = boundaryMode;
    }

    /*
     * Is strict determinism.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isStrictDeterminism() {
        return strictDeterminism;
    }

    /*
     * Set strict determinism.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param strictDeterminism the strict determinism parameter (boolean)
     */
    public void setStrictDeterminism(boolean strictDeterminism) {
        this.strictDeterminism = strictDeterminism;
        if (strictDeterminism) {
            this.sparseCellSkippingEnabled = false;
            this.oceanMultiRateTickingEnabled = false;
            this.parallelExecutionEnabled = false;
            this.spatialRangeTruncationEnabled = false;
        }
    }

    /*
     * Is sparse cell skipping enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isSparseCellSkippingEnabled() {
        return sparseCellSkippingEnabled;
    }

    /*
     * Set sparse cell skipping enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param sparseCellSkippingEnabled the sparse cell skipping enabled parameter (boolean)
     */
    public void setSparseCellSkippingEnabled(boolean sparseCellSkippingEnabled) {
        this.sparseCellSkippingEnabled = sparseCellSkippingEnabled;
        if (sparseCellSkippingEnabled) {
            this.strictDeterminism = false;
        }
    }

    /*
     * Is ocean macro aggregation enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isOceanMacroAggregationEnabled() {
        return oceanMacroAggregationEnabled;
    }

    /*
     * Set ocean macro aggregation enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param oceanMacroAggregationEnabled the ocean macro aggregation enabled parameter (boolean)
     */
    public void setOceanMacroAggregationEnabled(boolean oceanMacroAggregationEnabled) {
        this.oceanMacroAggregationEnabled = oceanMacroAggregationEnabled;
    }

    /*
     * Is coastal navigation only enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isCoastalNavigationOnlyEnabled() {
        return coastalNavigationOnlyEnabled;
    }

    /*
     * Set coastal navigation only enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param coastalNavigationOnlyEnabled the coastal navigation only enabled parameter (boolean)
     */
    public void setCoastalNavigationOnlyEnabled(boolean coastalNavigationOnlyEnabled) {
        this.coastalNavigationOnlyEnabled = coastalNavigationOnlyEnabled;
    }

    /*
     * Is ocean multi rate ticking enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isOceanMultiRateTickingEnabled() {
        return oceanMultiRateTickingEnabled;
    }

    /*
     * Set ocean multi rate ticking enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param oceanMultiRateTickingEnabled the ocean multi rate ticking enabled parameter (boolean)
     */
    public void setOceanMultiRateTickingEnabled(boolean oceanMultiRateTickingEnabled) {
        this.oceanMultiRateTickingEnabled = oceanMultiRateTickingEnabled;
        if (oceanMultiRateTickingEnabled) {
            this.strictDeterminism = false;
        }
    }

    /*
     * Is parallel execution enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isParallelExecutionEnabled() {
        return parallelExecutionEnabled;
    }

    /*
     * Set parallel execution enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param parallelExecutionEnabled the parallel execution enabled parameter (boolean)
     */
    public void setParallelExecutionEnabled(boolean parallelExecutionEnabled) {
        this.parallelExecutionEnabled = parallelExecutionEnabled;
        if (parallelExecutionEnabled) {
            this.strictDeterminism = false;
        }
    }

    /*
     * Is spatial range truncation enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isSpatialRangeTruncationEnabled() {
        return spatialRangeTruncationEnabled;
    }

    /*
     * Set spatial range truncation enabled.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param spatialRangeTruncationEnabled the spatial range truncation enabled parameter (boolean)
     */
    public void setSpatialRangeTruncationEnabled(boolean spatialRangeTruncationEnabled) {
        this.spatialRangeTruncationEnabled = spatialRangeTruncationEnabled;
        if (spatialRangeTruncationEnabled) {
            this.strictDeterminism = false;
        }
    }

    /* Internal state variable for climate tick frequency (int). */
    private int climateTickFrequency = 5;
    /* Internal state variable for parallel thread count (int). */
    private int parallelThreadCount = 0;

    /*
     * Get climate tick frequency.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public int getClimateTickFrequency() {
        return climateTickFrequency;
    }

    /*
     * Set climate tick frequency.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param climateTickFrequency the climate tick frequency parameter (int)
     */
    public void setClimateTickFrequency(int climateTickFrequency) {
        this.climateTickFrequency = Math.max(1, climateTickFrequency);
    }

    /*
     * Get parallel thread count.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public int getParallelThreadCount() {
        return parallelThreadCount;
    }

    /*
     * Set parallel thread count.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param parallelThreadCount the parallel thread count parameter (int)
     */
    public void setParallelThreadCount(int parallelThreadCount) {
        this.parallelThreadCount = Math.max(0, parallelThreadCount);
    }

    /*
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

    /*
     * Get temporal resolution days.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getTemporalResolutionDays() {
        return temporalResolutionDays;
    }

    /*
     * Set temporal resolution days.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param temporalResolutionDays the temporal resolution days parameter (double)
     */
    public void setTemporalResolutionDays(double temporalResolutionDays) {
        this.temporalResolutionDays = temporalResolutionDays;
    }

    /*
     * Get type bengine states.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.Map<String, Boolean> getTypeBEngineStates() {
        if (typeBEngineStates == null) {
            typeBEngineStates = new java.util.HashMap<>();
        }
        return typeBEngineStates;
    }



    /*
     * Set type bengine states.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param typeBEngineStates the type bengine states parameter (Boolean&gt;)
     */
    public void setTypeBEngineStates(java.util.Map<String, Boolean> typeBEngineStates) {
        this.typeBEngineStates = typeBEngineStates != null ? typeBEngineStates : new java.util.HashMap<>();
    }

    /*
     * Get type bengine parameters.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.Map<String, java.util.Map<String, Double>> getTypeBEngineParameters() {
        if (typeBEngineParameters == null) {
            typeBEngineParameters = new java.util.HashMap<>();
        }
        return typeBEngineParameters;
    }

    /*
     * Set type bengine parameters.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param typeBEngineParameters the type bengine parameters parameter (Double&gt;&gt;)
     */
    public void setTypeBEngineParameters(java.util.Map<String, java.util.Map<String, Double>> typeBEngineParameters) {
        this.typeBEngineParameters = typeBEngineParameters != null ? typeBEngineParameters : new java.util.HashMap<>();
    }

    /*
     * Get culture vector dimensions.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public int getCultureVectorDimensions() {
        return cultureVectorDimensions;
    }

    /*
     * Set culture vector dimensions.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param cultureVectorDimensions the culture vector dimensions parameter (int)
     */
    public void setCultureVectorDimensions(int cultureVectorDimensions) {
        this.cultureVectorDimensions = cultureVectorDimensions;
    }

    /*
     * Get custom tensor names.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.Map<Integer, String> getCustomTensorNames() {
        return customTensorNames;
    }

    /*
     * Set custom tensor names.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param customTensorNames the custom tensor names parameter (String&gt;)
     */
    public void setCustomTensorNames(java.util.Map<Integer, String> customTensorNames) {
        this.customTensorNames = customTensorNames != null ? customTensorNames : new java.util.HashMap<>();
    }

    /*
     * Get custom tensor name.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param index the index parameter (int)
     * @return the resulting computation or state reference
     */
    public String getCustomTensorName(int index) {
        return customTensorNames != null ? customTensorNames.get(index) : null;
    }

    /*
     * Set custom tensor name.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param index the index parameter (int)
     * @param name the name parameter (String)
     */
    public void setCustomTensorName(int index, String name) {
        if (this.customTensorNames == null) this.customTensorNames = new java.util.HashMap<>();
        if (name == null || name.isBlank()) {
            this.customTensorNames.remove(index);
        } else {
            this.customTensorNames.put(index, name.trim());
        }
    }

    /*
     * Get cultural diffusion rate.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getCulturalDiffusionRate() {
        return culturalDiffusionRate;
    }

    /*
     * Set cultural diffusion rate.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param culturalDiffusionRate the cultural diffusion rate parameter (double)
     */
    public void setCulturalDiffusionRate(double culturalDiffusionRate) {
        this.culturalDiffusionRate = culturalDiffusionRate;
    }

    /*
     * Get cultural mutation rate.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public double getCulturalMutationRate() {
        return culturalMutationRate;
    }

    /*
     * Set cultural mutation rate.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param culturalMutationRate the cultural mutation rate parameter (double)
     */
    public void setCulturalMutationRate(double culturalMutationRate) {
        this.culturalMutationRate = culturalMutationRate;
    }

    /*
     * Get custom tensor maps base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.List<String> getCustomTensorMapsBase64() {
        if (customTensorMapsBase64 == null) {
            customTensorMapsBase64 = new java.util.ArrayList<>();
        }
        return customTensorMapsBase64;
    }

    /*
     * Set custom tensor maps base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param customTensorMapsBase64 the custom tensor maps base64 parameter (java.util.List&lt;String&gt;)
     */
    public void setCustomTensorMapsBase64(java.util.List<String> customTensorMapsBase64) {
        this.customTensorMapsBase64 = customTensorMapsBase64 != null ? customTensorMapsBase64 : new java.util.ArrayList<>();
    }

    /*
     * Get tensor procedural modes.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.List<Boolean> getTensorProceduralModes() {
        if (tensorProceduralModes == null) {
            tensorProceduralModes = new java.util.ArrayList<>();
        }
        return tensorProceduralModes;
    }

    /*
     * Set tensor procedural modes.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param tensorProceduralModes the tensor procedural modes parameter (java.util.List&lt;Boolean&gt;)
     */
    public void setTensorProceduralModes(java.util.List<Boolean> tensorProceduralModes) {
        this.tensorProceduralModes = tensorProceduralModes != null ? tensorProceduralModes : new java.util.ArrayList<>();
    }

    /*
     * Get custom tensor map base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param index the index parameter (int)
     * @return the resulting computation or state reference
     */
    public String getCustomTensorMapBase64(int index) {
        java.util.List<String> list = getCustomTensorMapsBase64();
        if (index >= 0 && index < list.size() && list.get(index) != null && !list.get(index).isBlank()) {
            return list.get(index);
        }
        return null;
    }

    /*
     * Set custom tensor map base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param index the index parameter (int)
     * @param base64 the base64 parameter (String)
     */
    public void setCustomTensorMapBase64(int index, String base64) {
        java.util.List<String> list = getCustomTensorMapsBase64();
        while (list.size() <= index) {
            list.add(null);
        }
        list.set(index, base64);
    }

    /*
     * Get tensor seeds.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.Map<Integer, Long> getTensorSeeds() {
        if (tensorSeeds == null) {
            tensorSeeds = new java.util.HashMap<>();
        }
        return tensorSeeds;
    }

    /*
     * Set tensor seeds.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param tensorSeeds the tensor seeds parameter (Long&gt;)
     */
    public void setTensorSeeds(java.util.Map<Integer, Long> tensorSeeds) {
        this.tensorSeeds = tensorSeeds != null ? tensorSeeds : new java.util.HashMap<>();
    }

    /*
     * Get tensor procedural parameters.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.Map<Integer, java.util.Map<String, Double>> getTensorProceduralParameters() {
        if (tensorProceduralParameters == null) {
            tensorProceduralParameters = new java.util.HashMap<>();
        }
        return tensorProceduralParameters;
    }

    /*
     * Set tensor procedural parameters.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param tensorProceduralParameters the tensor procedural parameters parameter (Double&gt;&gt;)
     */
    public void setTensorProceduralParameters(java.util.Map<Integer, java.util.Map<String, Double>> tensorProceduralParameters) {
        this.tensorProceduralParameters = tensorProceduralParameters != null ? tensorProceduralParameters : new java.util.HashMap<>();
    }

    /*
     * Get resource vector dimensions.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public int getResourceVectorDimensions() {
        return resourceVectorDimensions;
    }

    /*
     * Set resource vector dimensions.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param resourceVectorDimensions the resource vector dimensions parameter (int)
     */
    public void setResourceVectorDimensions(int resourceVectorDimensions) {
        this.resourceVectorDimensions = resourceVectorDimensions;
    }

    /*
     * Get custom geology tensor maps base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.List<String> getCustomGeologyTensorMapsBase64() {
        if (customGeologyTensorMapsBase64 == null) {
            customGeologyTensorMapsBase64 = new java.util.ArrayList<>();
        }
        return customGeologyTensorMapsBase64;
    }

    /*
     * Set custom geology tensor maps base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param customGeologyTensorMapsBase64 the custom geology tensor maps base64 parameter (java.util.List&lt;String&gt;)
     */
    public void setCustomGeologyTensorMapsBase64(java.util.List<String> customGeologyTensorMapsBase64) {
        this.customGeologyTensorMapsBase64 = customGeologyTensorMapsBase64 != null ? customGeologyTensorMapsBase64 : new java.util.ArrayList<>();
    }

    /*
     * Get geology tensor procedural modes.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.List<Boolean> getGeologyTensorProceduralModes() {
        if (geologyTensorProceduralModes == null) {
            geologyTensorProceduralModes = new java.util.ArrayList<>();
        }
        return geologyTensorProceduralModes;
    }

    /*
     * Set geology tensor procedural modes.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param geologyTensorProceduralModes the geology tensor procedural modes parameter (java.util.List&lt;Boolean&gt;)
     */
    public void setGeologyTensorProceduralModes(java.util.List<Boolean> geologyTensorProceduralModes) {
        this.geologyTensorProceduralModes = geologyTensorProceduralModes != null ? geologyTensorProceduralModes : new java.util.ArrayList<>();
    }

    /*
     * Get custom geology tensor map base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param index the index parameter (int)
     * @return the resulting computation or state reference
     */
    public String getCustomGeologyTensorMapBase64(int index) {
        java.util.List<String> list = getCustomGeologyTensorMapsBase64();
        if (index >= 0 && index < list.size() && list.get(index) != null && !list.get(index).isBlank()) {
            return list.get(index);
        }
        return null;
    }

    /*
     * Set custom geology tensor map base64.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param index the index parameter (int)
     * @param base64 the base64 parameter (String)
     */
    public void setCustomGeologyTensorMapBase64(int index, String base64) {
        java.util.List<String> list = getCustomGeologyTensorMapsBase64();
        while (list.size() <= index) {
            list.add(null);
        }
        list.set(index, base64);
    }

    /*
     * Get climate events.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
    public java.util.List<ClimateEvent> getClimateEvents() {
        return climateEvents;
    }

    /*
     * Set climate events.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @param climateEvents the climate events parameter (java.util.List&lt;ClimateEvent&gt;)
     */
    public void setClimateEvents(java.util.List<ClimateEvent> climateEvents) {
        this.climateEvents = climateEvents != null ? climateEvents : new java.util.ArrayList<>();
    }

    /*
     * Get built in scenarios.
     * Enforces physical invariants and updates associated state variables within {@code Scenario}.
     *
     * @return the resulting computation or state reference
     */
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
        s0.setPlanetPreset(PlanetPreset.EARTH_LIG_100000BP);
        s0.setEcologyPreset(EcologyPreset.EARTH_LIG_100000BP);
        s0.setDescription("""
            ðŸŒ SCÃ‰NARIO PALÃ‰OLITHIQUE : Berceau Africain, TraversÃ©e des Continents & Out of Africa (-100 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la dynamique démographique et l'expansion spatiale des premières populations d'Homo Sapiens depuis l'Afrique de l'Est à travers le Moyen-Orient, l'Eurasie, l'Océanie et les Amériques.
            
            [CONDITIONS INITIALES PHYSIQUES (T₀)]
            • Population Initiale : 50 000 individus (Capacité nomade pré-agricole).
            • Stock Capital Physique (K₀) : 2 kg/habitant (bifaces en pierre, javelots).
            • Énergie Stockée (E₀) : 5 MJ/habitant (maîtrise du feu et combustible bois).
            • Réserves Alimentaires (F₀) : 2 mois de subsistance en chasse-cueillette.
            • Savoir Archivé (I₀) : 2 bits/habitant (traditions orales paléolithiques & langage).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Vitesse de dispersion géographique vers le Proche-Orient, l'Asie du Sud et l'Europe.
            • Survie démographique nomade face aux glaciations et évènements de Dansgaard-Oeschger.
            • Dérive linguistique paléolithique et innovations lithiques (Levallois, emmanchement à la résine).
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

        // --- SCÉNARIO : CATACLYSME DU SUPERVOLCAN TOBA (-74000) ---
        Scenario sToba = new Scenario();
        sToba.setPresetKey("toba_cataclysm_74k");
        sToba.setName("Cataclysme du Supervolcan Toba & Goulot Démographique (-74000)");
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
            🌋 SCÉNARIO PALÉOCLIMATIQUE : Super-Éruption du Mont Toba & Hiver Volcanique (-74 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'impact biosphérique de la super-éruption du Toba (Sumatra, VEI-8) ayant éjecté 2 800 km³ de téphras. Chute thermique globale de 3 à 5°C pendant plusieurs années, créant un goulot d'étranglement génétique majeur chez Homo sapiens.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Goulot d'étranglement démographique sévère (population reproductive mondiale réduite à quelques milliers d'individus).
            • Réfuges écologiques côtiers en Afrique australe et en Inde méridionale.
            • Rebond démographique et innovations techniques lithiques post-crise (Mode 3 / Middle Stone Age).
            """);
        java.util.Map<String, Boolean> sTobaEngines = sToba.getTypeBEngineStates();
        sTobaEngines.put("VolcanicTephraRefugiaEngine", true);
        sTobaEngines.put("BifurcationChaosEngine", true);
        sTobaEngines.put("DemographicLifeTableEngine", true);
        sTobaEngines.put("HomininCompetitiveExclusionEngine", true);
        sTobaEngines.put("TasmanianCulturalRegressionEngine", true);
        sTobaEngines.put("KinSelectionHamiltonEngine", true);
        sToba.getClimateEvents().add(new ClimateEvent("VOLCANIC_ERUPTION", "Super-Éruption VEI-8 du Mont Toba", -74000, 2.58, 98.83, -5.0, 9.5));
        list.add(sToba);

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
        sSahul.setPlanetPreset(PlanetPreset.EARTH_MIS3_50000BP);
        sSahul.setEcologyPreset(EcologyPreset.EARTH_MIS3_50000BP);
        sSahul.setClippingEnabled(true);
        sSahul.setMinLat(-42.0); sSahul.setMaxLat(-10.0); sSahul.setMinLng(112.0); sSahul.setMaxLng(155.0);
        sSahul.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSahul.setDescription("""
            🦘 SCÉNARIO PALÉOLITHIQUE : Traversée Maritime & Incursion dans le Sahul (-50 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Premier franchissement maritime majeur de la ligne de Wallace par les ancêtres des Aborigènes d'Australie. Modélise la colonisation du continent Sahul (Australie, Tasmanie, Nouvelle-Guinée réunies) et l'adaptation aux écosystèmes arides.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Franchissement maritime de la ligne de Wallace et navigation côtière insulaire.
            • Adaptation aux régimes arides intérieurs et gestion des paysages par brûlis (fire-stick farming).
            • Stabilité démographique à long terme et réseaux d'alliances à l'ocre à travers le désert.
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
        sBeringia.setPlanetPreset(PlanetPreset.EARTH_LGM_ONSET_25000BP);
        sBeringia.setEcologyPreset(EcologyPreset.EARTH_LGM_ONSET_25000BP);
        sBeringia.setClippingEnabled(true);
        sBeringia.setMinLat(45.0); sBeringia.setMaxLat(75.0); sBeringia.setMinLng(140.0); sBeringia.setMaxLng(-120.0);
        sBeringia.setBoundaryMode("DYNAMIC_RESERVOIR");
        sBeringia.setDescription("""
            ðŸ”ï¸ SCÃ‰NARIO PALÃ‰OLITHIQUE : Le Pont Terrestre de BÃ©ringie & Incursion AmÃ©ricaine (-25 000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'isolation des populations paléolithiques sur le pont terrestre de Béringie pendant le Dernier Maximum Glaciaire (LGM), suivie de leur dispersion à travers le corridor libre de glace et la route côtière du Pacifique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Temps de pause/stase génétique sur le pont de Béringie (Beringian Standstill).
            • Franchissement du corridor libre de glace ou de la route côtière du varech (Kelp Highway).
            • Taux d'expansion démographique rapide vers l'Amérique du Nord puis du Sud.
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

        // --- SCÉNARIO : DERNIER MAXIMUM GLACIAIRE & SOLUTRÉEN (-20000) ---
        Scenario sLGM = new Scenario();
        sLGM.setPresetKey("lgm_solutrean");
        sLGM.setName("Dernier Maximum Glaciaire & Solutréen (-20000)");
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
            Modélise le paroxysme du Dernier Maximum Glaciaire (LGM) avec un niveau marin abaissé de 120 mètres (exposant le Doggerland, le Sundaland, le Sahul et la Béringie), les inlandsis massifs (Laurentide, Fennoscandie) et l'industrie lithique foliacée solutréenne.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Concentration des populations dans les refuges thermiques (péninsule Ibérique, zone franco-cantabrique, Balkans).
            • Maîtrise technologique du froid extrême (vêtements ajustés à l'aiguille à chas, fosses-congélateurs périglaciaires).
            • Réexpansion démographique post-glaciaire rapide lors du réchauffement de Bølling-Allerød.
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
        sYoungerDryas.setPlanetPreset(PlanetPreset.EARTH_EH_10000BP);
        sYoungerDryas.setEcologyPreset(EcologyPreset.EARTH_EH_10000BP);
        sYoungerDryas.setDescription("""
            â„ï¸ SCÃ‰NARIO PALÃ‰OCLIMATIQUE : Le RÃ©cents Dryas & Pression ForagÃ¨re Au Levant (-10 900 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Refroidissement brutal de 5 à 8°C de l'Atlantique Nord déclenché par le déversement d'eau douce du Lac Agassiz. Au Levant, la sécheresse aiguë réduit les céréales sauvages, contraignant les populations Natufiennes à la sédentarisation pré-agricole et au contrôle des graines.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute brutale des rendements foragers de céréales sauvages au Levant suite à l'aridification.
            • Émergence des premiers hameaux sédentaires natoufiens et stockage intensif des grains en silos.
            • Pression sélective poussant à la domestication du chien et aux premiers semis intentionnels.
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
        s1.setPlanetPreset(PlanetPreset.EARTH_EH_10000BP);
        s1.setEcologyPreset(EcologyPreset.EARTH_EH_10000BP);
        s1.setClippingEnabled(true);
        s1.setMinLat(25.0); s1.setMaxLat(42.0); s1.setMinLng(25.0); s1.setMaxLng(55.0);
        s1.setBoundaryMode("DYNAMIC_RESERVOIR");
        s1.setDescription("""
            🌾 SCÉNARIO HISTORIQUE : L'Aube de l'Agriculture au Croissant Fertile (-8000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Ce scénario modélise la transition majeure du Néolithique entre l'économie de subsistance des chasseurs-cueilleurs et l'émergence des premières communautés agricoles sédentaires le long du Tigre, de l'Euphrate, du Nil et de la côte Levantine.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Explosion démographique liée aux surplus agricoles céréaliers (engrain, amidonnier, orge).
            • Émergence des premières cités-États mésopotamiennes, chefferies et différenciation sociale.
            • Dégradation environnementale précoce (déforestation des piémonts, érosion des sols, début de salinisation).
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
        sGreenSahara.setPlanetPreset(PlanetPreset.EARTH_MH_6000BP);
        sGreenSahara.setEcologyPreset(EcologyPreset.EARTH_MH_6000BP);
        sGreenSahara.setClippingEnabled(true);
        sGreenSahara.setMinLat(0.0); sGreenSahara.setMaxLat(42.0); sGreenSahara.setMinLng(-20.0); sGreenSahara.setMaxLng(45.0);
        sGreenSahara.setBoundaryMode("DYNAMIC_RESERVOIR");
        sGreenSahara.setDescription("""
            🌴 SCÉNARIO PALÉOCLIMATIQUE : Le Sahara Vert & Période Humide Africaine (-6000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise la Période Humide Africaine (AHP) où l'insolation printanière amplifiée par l'orbite terrestre a intensifié la mousson africaine. Le désert du Sahara était alors une savane verdoyante parsemée de lac majeurs (Lac Méga-Tchad), peuplée d'éleveurs néolithiques et de chasseurs-cueilleurs.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Densité humaine élevée et corridor trans-saharien autour du Lac Méga-Tchad et des oueds.
            • Économie mixte : pastoralisme bovin néolithique, chasse et pêche pélagique lacustre.
            • Migration et repli massif des populations vers la vallée du Nil lors de la désertification vers -3500.
            """);
        java.util.Map<String, Boolean> sGreenSaharaEngines = sGreenSahara.getTypeBEngineStates();
        sGreenSaharaEngines.put("LakeChadWadiMigrationEngine", true);
        sGreenSaharaEngines.put("AridWaterStorageStashEngine", true);
        sGreenSaharaEngines.put("PelagicFishingHookEngine", true);
        sGreenSaharaEngines.put("BoserupAgriculturalIntensificationEngine", true);
        sGreenSaharaEngines.put("OstromCommonsPureEngine", true);
        sGreenSaharaEngines.put("CulturalSociologyEngine", true);
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
        sEgypt.setPlanetPreset(PlanetPreset.EARTH_LH_3000BP);
        sEgypt.setEcologyPreset(EcologyPreset.EARTH_LH_3000BP);
        sEgypt.setClippingEnabled(true);
        sEgypt.setMinLat(21.0); sEgypt.setMaxLat(32.0); sEgypt.setMinLng(24.0); sEgypt.setMaxLng(36.0);
        sEgypt.setBoundaryMode("DYNAMIC_RESERVOIR");
        sEgypt.setDescription("""
            𓀀 SCÉNARIO HISTORIQUE : Unification Thinite & Crues du Nil (-3000 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'émergence de la première civilisation pharaonique unifiée. Dépendance absolue vis-à-vis du rythme annuel du Nil, de la gestion du bassin d'irrigation et de l'administration hiéroglyphique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Gestion hydraulique étatique centralisée des crues annuelles et du limon fertile.
            • Accumulation monumentale de capital physique et symbolique (pyramides, canaux, greniers d'État).
            • Vulnérabilité systémique aux sécheresses prolongées (chute de l'Ancien Empire / 1ère Période Intermédiaire).
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

        // --- SCÉNARIO : ÂGE DU BRONZE MOYEN & MÉSOPOTAMIE (-1900) ---
        Scenario s3 = new Scenario();
        s3.setPresetKey("assyrian_empire");
        s3.setName("Âge du Bronze Moyen & Mésopotamie (-1900)");
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
            Modélise l'apogée du Bronze Moyen (Code d'Hammurabi, première dynastie de Babylone, dynastie Shang en Chine, fin de la civilisation de l'Indus) et les vulnérabilités écologiques d'irrigation intensive.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Salinisation progressive des sols irrigués de basse Mésopotamie (baisse du blé au profit de l'orge).
            • Dynamique d'Asabiyyah des peuples périphériques et militarisation de l'Empire Néo-Assyrien.
            • Réseaux urbains denses le long des canaux et dépendance au commerce du cuivre et de l'étain.
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
        sMeso.setPlanetPreset(PlanetPreset.EARTH_BRONZE_1900BP);
        sMeso.setEcologyPreset(EcologyPreset.EARTH_BRONZE_1900BP);
        sMeso.setClippingEnabled(true);
        sMeso.setMinLat(12.0); sMeso.setMaxLat(24.0); sMeso.setMinLng(-105.0); sMeso.setMaxLng(-85.0);
        sMeso.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMeso.setDescription("""
            🌽 SCÉNARIO HISTORIQUE : Culture Mère Olmèque & Cités-États Mayas (-1500 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Émergence des centres cérémoniels de San Lorenzo et La Venta, puis essor de la civilisation maya classique. Modélise la maïsiculture intensive, les réservoirs d'eau pluviale et l'astronomie de précision.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Intensification agricole sur terrasses et chinampas sans bêtes de trait ni métallurgie du fer.
            • Cyclicités de sécheresse mésoaméricaines et résilience des réservoirs d'eau (aguadas, chultuns, cénotes).
            • Évolution fractale et dispersion des cités cérémonielles mayas suivies d'effondrements régionaux.
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

        // --- SCÉNARIO HISTORIQUE : RAMSÈS II, PAX AEGYPTIACA & BÂTISSEURS (-1279) ---
        Scenario sRamesses = new Scenario();
        sRamesses.setPresetKey("ramesses_ii");
        sRamesses.setName("Ramsès II, Pax Aegyptiaca & Bâtisseurs Monumentaux (-1279)");
        sRamesses.setStartDateYear(-1279);
        sRamesses.setEndDateYear(-1200);
        sRamesses.setInitialHumanCount(3500000L);
        sRamesses.setInitialCapitalPerCapita(140.0);
        sRamesses.setInitialEnergyPerCapita(75.0);
        sRamesses.setInitialFoodReserveMonths(12.0);
        sRamesses.setInitialInformationPerCapita(15.0);
        sRamesses.setPopulationDensityType("EGYPT_NILE");
        sRamesses.setTargetCohortSize(150);
        sRamesses.setH3Resolution(5);
        sRamesses.setTemporalResolutionDays(30.0);
        sRamesses.setClippingEnabled(true);
        sRamesses.setMinLat(18.0);
        sRamesses.setMaxLat(36.0);
        sRamesses.setMinLng(24.0);
        sRamesses.setMaxLng(42.0);
        sRamesses.setPlanetPreset(PlanetPreset.EARTH_LH_3000BP);
        sRamesses.setEcologyPreset(EcologyPreset.EARTH_LH_3000BP);
        sRamesses.setEarthHistoricalLeadersEnabled(true);
        sRamesses.setDescription("""
            🏛️ SCÉNARIO HISTORIQUE : Règne de Ramsès II, Traité de Qadesh & Architecture Monumentale (-1279 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise le long règne de 66 ans de Ramsès II (XIXe dynastie), le traité de paix de Qadesh (-1259) avec l'Empire Hittite, et les chantiers monumentaux majeurs (Abou Simbel, Ramesseum, Pi-Ramsès).
            
            [THÉORIE DE L'INFLUENCE DES GRANDS HOMMES & OBSERVABLES]
            • Effet transitoire mais puissant d'un leadership exceptionnellement long : centralisation de la capacité étatique et sur-accumulation de capital physique (canaux, temples, greniers).
            • Stabilité géopolitique régionale et commerce méditerranéen sécurisé (Pax Aegyptiaca).
            • Vulnérabilité de succession post-recul du grand règne à l'approche de la crise systémique de l'Âge du Bronze (-1200).
            """);
        java.util.Map<String, Boolean> sRamessesEngines = sRamesses.getTypeBEngineStates();
        sRamessesEngines.put("HydrologicalEngineeringEngine", true);
        sRamessesEngines.put("DemographicLifeTableEngine", true);
        sRamessesEngines.put("TradeNetworkEngine", true);
        sRamessesEngines.put("WarDiplomacyEngine", true);
        sRamessesEngines.put("StructuralDemographicBifurcationEngine", true);
        sRamessesEngines.put("MaritimeHighwayEngine", true);
        sRamessesEngines.put("TurchinGoldstoneSDTEngine", true);
        sRamessesEngines.put("FertileCrescentSalinizationEngine", true);
        sRamessesEngines.put("ScottAgainstTheGrainPureEngine", true);
        sRamessesEngines.put("BoserupAgriculturalIntensificationEngine", true);
        list.add(sRamesses);

        // --- SCÉNARIO : EFFONDREMENT DE L'ÂGE DU BRONZE RÉCENT (-1200) ---
        Scenario sBronzeCollapse = new Scenario();
        sBronzeCollapse.setPresetKey("bronze_age_collapse_1200bc");
        sBronzeCollapse.setName("Effondrement de l'Âge du Bronze Récent & Peuples de la Mer (-1200)");
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
            Modélise l'effondrement simultané en cascade des civilisations palatiales de Méditerranée orientale (Mycènes, Ugarit, Empire Hittite, affaiblissement de l'Égypte). Combinaison d'une méga-sécheresse centennale, de ruptures des routes de l'étain et des invasions des Peuples de la Mer.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Effondrement en chaîne des réseaux commerciaux interconnectés du bronze.
            • Chute brutale de la complexité institutionnelle (disparition de l'écriture Linéaire B, dépopulation urbaine).
            • Période d'Âges Sombres méditerranéens préparant la transition décentralisée vers le fer.
            """);
        java.util.Map<String, Boolean> sBronzeCollapseEngines = sBronzeCollapse.getTypeBEngineStates();
        sBronzeCollapseEngines.put("TainterComplexityCollapseEngine", true);
        sBronzeCollapseEngines.put("SpatialMetapopulationSEIREngine", true);
        sBronzeCollapseEngines.put("FertileCrescentSalinizationEngine", true);
        sBronzeCollapseEngines.put("TurchinGoldstoneSDTEngine", true);
        sBronzeCollapseEngines.put("LanchesterKineticWarfareEngine", true);
        sBronzeCollapseEngines.put("GranovetterThresholdCascadeEngine", true);
        sBronzeCollapse.getClimateEvents().add(new ClimateEvent("MEGADROUGHT", "Méga-Sécheresse Centennale de Méditerranée Orientale", -1200, 35.0, 33.0, 0.0, 8.5));
        list.add(sBronzeCollapse);

        // --- SCÉNARIO : DÉBUT ÂGE DU FER & MÉDITERRANÉE ANTIQUE (-1000) ---
        Scenario sIron = new Scenario();
        sIron.setPresetKey("early_iron_age");
        sIron.setName("Début de l'Âge du Fer & Méditerranée Antique (-1000)");
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
            Modélise la transition de l'Âge du Bronze vers la métallurgie du fer après l'effondrement du Bronze Récent. Diffusion de l'alphabet phénicien, essor des cités-États grecques, expansion de l'Empire Néo-Assyrien et dynastie Zhou en Chine.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Démocratisation de l'outillage et des armes grâce à l'abondance géologique du minerai de fer.
            • Essor du commerce maritime thalassocratique en Méditerranée (Phéniciens, Grecs, Étrusques).
            • Rupture de la dépendance stratégique au cuivre/étain et multiplication des centres de pouvoir régionaux.
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

        // --- SCÉNARIO : EXPANSION HELLÉNISTIQUE & CHOC D'ALEXANDRE (-334) ---
        Scenario sAlexander = new Scenario();
        sAlexander.setPresetKey("alexander_hellenistic_334bc");
        sAlexander.setName("Expansion Hellénistique & Choc d'Alexandre le Grand (-334)");
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
            ⚡ SCÉNARIO HISTORIQUE : Conquête Macédonienne & Mondialisation Hellénistique (-334 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'effondrement foudroyant de l'Empire Achéménide sous la phalange macédonienne d'Alexandre. Intégration d'un corridor urbain et monétaire unifié de la Grèce à l'Indus (fondation d'Alexandries, monétarisation de l'or perse, koinè grecque).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Vitesse de projection militaire et réduction spectaculaire de la friction logistique le long des routes royales perses.
            • Monétarisation massive et urbanisation grecque en Orient (Alexandrie, Séleucie, Antioche).
            • Fragmentation politique immédiate post-Alexandre (guerres des Diadoques) sans destruction du réseau urbain commercial.
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
        sMaurya.setPlanetPreset(PlanetPreset.EARTH_IRON_1000BP);
        sMaurya.setEcologyPreset(EcologyPreset.EARTH_IRON_1000BP);
        sMaurya.setClippingEnabled(true);
        sMaurya.setMinLat(8.0); sMaurya.setMaxLat(35.0); sMaurya.setMinLng(68.0); sMaurya.setMaxLng(90.0);
        sMaurya.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMaurya.setDescription("""
            â˜¸ï¸ SCÃ‰NARIO HISTORIQUE : L'Empire Maurya d'Ashoka & La VallÃ©e du Gange (-300 av. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Unification du sous-continent indien sous Chandragupta et Ashoka. Modélise l'agriculture rizicole de la plaine gângétique, les routes commerciales de la Soie et le réseau urbain autour de Pataliputra et Taxila.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Densification démographique extrême de la plaine indo-gangétique grâce au riz irrigué.
            • Diffusion des édits impériaux et baisse institutionnelle de la violence (pacification d'Ashoka).
            • Intégration commerciale transasiatique le long de la Grand Trunk Road et des ports de l'océan Indien.
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
            Modélise le bassin méditerranéen au moment de la Pax Romana sous Auguste. Intègre les données démographiques historiques (55 millions d'habitants), les réseaux d'infrastructures (viae, aqueducs) et les dynamiques cliodynamiques de Turchin.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Déploiement des infrastructures de transport (réseau viaire terrestre et autoroutes maritimes méditerranéennes).
            • Cycles cliodynamiques de Turchin (surproduction des élites, instabilité politique, dévaluation monétaire du denier).
            • Impact conjugué des chocs climatiques (Optimum Romain vers refroidissement) et des épidémies (Peste Antonine).
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
        s2.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s2.setDescription("""
            🌋 SCÉNARIO HISTORIQUE : L'Anomalie Climatique Volcanique de 536 & Choc Sanitaire
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            L'année 536 est considérée par les historiens du climat comme "la pire année de l'histoire humaine". Deux éruptions volcaniques super-massives consécutives ont injecté un voile d'aérosols stratosphériques occultant le Soleil pendant 18 mois.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute brutale des températures estivales (-2°C à -3°C) et effondrement des récoltes céréalières eurasiennes.
            • Propagation fulgurante de la Peste de Justinien (Yersinia pestis) le long des routes de commerce maritime.
            • Déstabilisation militaire des frontières impériales byzantines et grandes migrations de peuples des steppes.
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

        // --- SCÉNARIO : EXPANSION ISLAMIQUE & RÉVOLUTION COMMERCIALE CALIFALE (632) ---
        Scenario sIslam = new Scenario();
        sIslam.setPresetKey("islamic_expansion_632");
        sIslam.setName("Expansion Islamique & Révolution Commerciale Califale (632)");
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
            🌙 SCÉNARIO HISTORIQUE : Unification Califale & Révolution Agricole Arabe (632 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'expansion foudroyante des Califats Omeyyade et Abbasside de l'Espagne à l'Indus. Effondrement de l'Empire Sassanide, diffusion massive des techniques d'irrigation (qanats, norias) et des cultures tropicales (canne à sucre, coton, agrumes), essor des réseaux maritimes de l'Océan Indien.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Vitesse d'unification géopolitique portée par la haute Asabiyyah bédouine et la doctrine califale.
            • Révolution agricole arabe : diversification agronomique et maîtrise hydraulique des zones arides.
            • Essor de Bagdad comme métropole mondiale (Maison de la Sagesse) et intégration commerciale transcontinentale.
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
        s4.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s4.setDescription("""
            ðŸ® SCÃ‰NARIO HISTORIQUE : Le SiÃ¨cle d'Or de la Dynastie Song (1000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            La Chine des Song a connu la première pré-industrialisation de l'histoire, avec une utilisation massive du charbon de terre pour la fonte du fer et des réseaux de transport fluviaux ultra-efficaces.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Révolution énergétique pré-industrielle : transition massive vers le charbon minéral et hauts-fourneaux au coke.
            • Révolution monétaire et commerciale : première monnaie fiduciaire en papier (Jiaozi) et navigation à la boussole.
            • Hyper-urbanisation fluviale (Kaifeng, Hangzhou) et vulnérabilité militaire face aux cavaliers nomades du Nord.
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

        // --- SCÉNARIO : L'EMPIRE MONGOL & LA GRANDE RUPTURE EURASIENNE (1206) ---
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
            Modélise le plus grand empire territorial contigu de l'histoire humaine. Choc cinétique et démographique majeur en Asie centrale, Perse et Chine, suivi de l'unification sécurisée de la Route de la Soie (Pax Mongolica) qui servira de vecteur à la Peste Noire.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Projection militaire nomade ultra-rapide (cavalerie légère, réseau de relais de poste Yam).
            • Choc de mortalité urbaine et désertification de certaines oasis irriguées de Perse/Transoxiane.
            • Intégration économique trans-eurasienne de Pékin à Tabriz et la mer Noire.
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
        sMali.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sMali.setClippingEnabled(true);
        sMali.setMinLat(5.0); sMali.setMaxLat(25.0); sMali.setMinLng(-18.0); sMali.setMaxLng(15.0);
        sMali.setBoundaryMode("DYNAMIC_RESERVOIR");
        sMali.setDescription("""
            🕌 SCÉNARIO HISTORIQUE : L'Apogée de l'Empire du Mali sous Mansa Musa (1324 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise le réseau urbain et marchand trans-saharien de la boucle du Niger (Tombouctou, Gao, Djenné). Contrôle des mines d'or de Bambouk/Boure et des salines de Teghaza.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Flux caravaniers trans-sahariens de métaux précieux (or) contre sel gemme, textiles et manuscrits.
            • Essor académique et théologique de Tombouctou (Université Sankoré) et sédentarisation sahélienne.
            • Choc monétaire mondial provoqué par les dépenses d'or de Mansa Musa lors de son pèlerinage au Caire.
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

        // --- SCÉNARIO : LA PESTE NOIRE & INVERSION POST-FÉODALE (1347) ---
        Scenario sBlackDeath = new Scenario();
        sBlackDeath.setPresetKey("black_death_1347");
        sBlackDeath.setName("La Peste Noire & Inversion Économique Post-Féodale (1347)");
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
            💀 SCÉNARIO HISTORIQUE : La Pandémie de Peste Noire & Inversion du Rapport Capital/Travail (1347 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Vague pandémique foudroyante de Yersinia pestis anéantissant 35% à 60% de la population européenne et moyen-orientale en moins de 5 ans. Choc démographique provoquant la rareté soudaine de la main-d'œuvre, la hausse spectaculaire des salaires réels et l'effondrement du servage.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Vitesse de contagion métapopulationnelle le long des routes de commerce génoises et vénitiennes.
            • Inversion institutionnelle : émancipation paysanne en Europe occidentale vs second servage à l'Est.
            • Déprise agricole temporaire, reforestation spontanée et baisse des rentes foncières seigneuriales.
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
        sAmericas1491.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sAmericas1491.setClippingEnabled(true);
        sAmericas1491.setMinLat(-45.0); sAmericas1491.setMaxLat(30.0); sAmericas1491.setMinLng(-110.0); sAmericas1491.setMaxLng(-35.0);
        sAmericas1491.setBoundaryMode("DYNAMIC_RESERVOIR");
        sAmericas1491.setDescription("""
            🌽 SCÉNARIO HISTORIQUE : Les Amériques à la Veille du Contact (1491 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise les grands empires précolombiens (Empire Inca du Tawantinsuyu, Empire Aztèque de la Triple Alliance) et les sociétés Mississippiennes avant la rupture épidémique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Optimisation écologique des paysages anthropiques (terra preta amazonienne, terrasses andines, canaux chinampas).
            • Densités urbaines précolombiennes culminantes dans les bassins de Mexico (Tenochtitlan) et de Cuzco.
            • Absence d'immunité croisée et vulnérabilité maximale face aux pathogènes de l'Ancien Monde.
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
        sColumbian.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sColumbian.setDescription("""
            ⛵ SCÉNARIO HISTORIQUE : Le Choc du Contact d'Échange Colombien & Effondrement Épidémique (1492)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Modélise l'impact bio-démographique mondial de la rencontre entre l'Ancien et le Nouveau Monde. Trajectoire de choc microbiologique (chute démographique de 80-90% du continent américain) et réorganisation commerciale transatlantique.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute démographique cataclysmique (80 à 90%) des populations amérindiennes sous l'effet de la variole et de la rougeole.
            • Échange colombien global : diffusion mondiale du maïs, de la pomme de terre, du manioc et de l'argent du Potosí.
            • Reforestation spontanée des terres abandonnées et baisse temporaire du CO₂ atmosphérique mondial (Orbis Spike).
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
        sSakoku.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sSakoku.setClippingEnabled(true);
        sSakoku.setMinLat(30.0); sSakoku.setMaxLat(45.0); sSakoku.setMinLng(128.0); sSakoku.setMaxLng(146.0);
        sSakoku.setBoundaryMode("DYNAMIC_RESERVOIR");
        sSakoku.setDescription("""
            â›©ï¸ SCÃ‰NARIO HISTORIQUE : L'Ãˆre d'Isolement Autarcique Tokugawa (Sakoku, 1639 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Fermeture des frontières de l'archipel japonais décrétée par le Shogunat Tokugawa. Modélise une économie circulaire hautement autarcique, l'urbanisation géante d'Edo (Tokyo, 1 million d'habitants) et l'absence d'intrants extérieurs jusqu'à l'arrivée des bateaux noirs du Commandant Perry en 1853.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Stabilité démographique et gestion rigoureuse des ressources forestières et agricoles en boucle fermée.
            • Modèle de croissance zéro durable sans dépendance aux importations énergétiques ou minérales extérieures.
            • Urbanisation pacifiée d'Edo et floraison artisanale et culturelle sous autarcie stricte.
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
        sIndustrial1800.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sIndustrial1800.setDescription("""
            âš™ï¸ SCÃ‰NARIO HISTORIQUE : La Machine Ã  Vapeur & L'Ã‰mergence du Charbon (1800 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Basculement énergétique mondial du régime organique vers le régime minéral fossile (charbon de terre, machine à vapeur de Watt).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Rupture du piège malthusien : découplage entre croissance démographique et contrainte surfacique des sols.
            • Paradoxe de Jevons : les gains d'efficacité des machines à vapeur décuplent la consommation globale de charbon.
            • Urbanisation industrielle ultra-rapide (exode rural) et amorce des émissions anthropiques massives de CO₂.
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

        // --- SCÉNARIO : GUERRES MONDIALES, RUPTURE TOTALITAIRE & ÈRE NUCLÉAIRE (1914) ---
        Scenario sWW = new Scenario();
        sWW.setPresetKey("world_wars_totalitarian_1914");
        sWW.setName("Guerres Mondiales, Rupture Totalitaire & Ère Nucléaire (1914)");
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
            Modélise la période de crise systémique paroxystique de la modernité industrielle (1914-1945). Mobilisation intégrale de l'exergie fossile et chimique (synthèse Haber-Bosch), ruptures totalitaires (1917, 1933), destruction massive de capital en Europe/Asie, et franchissement du seuil de destruction thermonucléaire (1945).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Déploiement de la guerre industrielle et de la puissance de feu cinétique (artillerie lourde, aviation, blindés).
            • Ruptures de régime idéologiques extrêmes (collectivisme soviétique, militarisme fasciste) et purges démographiques.
            • Chute brutale du capital en 1939-1945 suivie de la reconstruction fordiste accélérée des Trente Glorieuses.
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
        sModern2000.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sModern2000.setDescription("""
            ðŸŒ SCÃ‰NARIO HISTORIQUE : L'Ãˆre NumÃ©rique & La Grande AccÃ©lÃ©ration (2000 ap. J.-C.)
            
            [CONTEXTE HISTORIQUE & PHYSIQUE]
            Consolidation du système économique mondial interconnecté, essor des microprocesseurs en silicium, de l'Internet mondial et de l'urbanisation globale.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Grande Accélération : croissance exponentielle des flux de matière, d'énergie fossile et d'information numérique.
            • Franchissement des limites planétaires (Cycle de l'azote/phosphore, forçage radiatif CO₂, érosion de biodiversité).
            • Rendements décroissants de la complexité institutionnelle et fragilité des chaînes logistiques just-in-time.
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

        // --- SCÉNARIO DU FUTUR : TRAJECTOIRE TENDANCIELLE BIOPHYSIQUE 2025-2055 (BUSINESS AS USUAL) ---
        Scenario sEarthFutureBAU = new Scenario();
        sEarthFutureBAU.setPresetKey("business_as_usual");
        sEarthFutureBAU.setName("Terre 2025-2055 : Trajectoire Tendancielle Biophysique (Business As Usual)");
        sEarthFutureBAU.setStartDateYear(2025);
        sEarthFutureBAU.setEndDateYear(2055);
        sEarthFutureBAU.setInitialHumanCount(8100000000L);
        sEarthFutureBAU.setInitialCapitalPerCapita(24000.0);
        sEarthFutureBAU.setInitialEnergyPerCapita(18000.0);
        sEarthFutureBAU.setInitialFoodReserveMonths(6.0);
        sEarthFutureBAU.setInitialInformationPerCapita(10000000.0);
        sEarthFutureBAU.setPopulationDensityType("URBAN_CLUSTERS");
        sEarthFutureBAU.setTargetCohortSize(5000);
        sEarthFutureBAU.setH3Resolution(4);
        sEarthFutureBAU.setTemporalResolutionDays(30.0);
        sEarthFutureBAU.setPlanetPreset(PlanetPreset.EARTH_MODERN);
        sEarthFutureBAU.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        sEarthFutureBAU.setDescription("""
            🌐 SCÉNARIO FUTUR : Trajectoire Tendancielle Biophysique & Ressources Réelles (2025-2055)
            
            [DESCRIPTION DES CONDITIONS INITIALES & FRICTIONS (T₀)]
            Modélisation de la poursuite de l'histoire humaine à partir de l'état macro-physique réel de 2025 sans présumer d'aucun moratoire normatif ni plafond artificiel. Intègre les frictions géopolitiques contemporaines de transit (détroit d'Ormuz, mer Rouge, sanctions d'Europe orientale), la déplétion cumulative des gisements conventionnels (~48% du pétrole déjà extrait, transition vers le schiste et l'offshore profond), et le taux d'apprentissage industriel des énergies bas-carbone.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Extraction continue des combustibles fossiles et des minerais gouvernée par la thermodynamique d'abaissement des teneurs (Bihouix) et la rente d'Hotelling.
            • Ajustement endogène de l'exergue utile (Kümmel-Ayres) entre capital fossile et déploiement spontané des renouvelables/nucléaire.
            • Évolution non-contrainte des émissions de CO₂, forçage radiatif continu et rétroactions climatiques sur les rendements agricoles et le stress hydrique.
            """);
        java.util.Map<String, Boolean> sEarthFutureEngines = sEarthFutureBAU.getTypeBEngineStates();
        sEarthFutureEngines.put("KummelAyresExergyEngine", true);
        sEarthFutureEngines.put("SmilMaterialTransitionsPureEngine", true);
        sEarthFutureEngines.put("HotellingResourceDepletionEngine", true);
        sEarthFutureEngines.put("RenewableEnergyPhysicsEngine", true);
        sEarthFutureEngines.put("NetEnergyEROEIEngine", true);
        sEarthFutureEngines.put("PhysicalEnergyGridEngine", true);
        sEarthFutureEngines.put("ResourceRecyclingEngine", true);
        sEarthFutureEngines.put("JevonsParadoxEngine", true);
        sEarthFutureEngines.put("World3HybridEngine", true);
        sEarthFutureEngines.put("OreGradeThermodynamicsEngine", true);
        list.add(sEarthFutureBAU);

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
        s5.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s5.setDescription("""
            📉 SCÉNARIO FUTUR : Business As Usual (Trajectoire GIEC SSP5-8.5)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Poursuite de l'extraction des combustibles fossiles traditionnels sans déploiement massif de la fusion ni captage du carbone.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Trajectoire de concentration CO₂ non régulée (>1000 ppm en 2100) et hausse thermique globale >4°C.
            • Submersion marine des métropoles côtières et stress thermique létal (température thermomètre mouillé Tw > 35°C).
            • Effondrement des rendements agricoles tropicaux et vagues de réfugiés climatiques vers les hautes latitudes.
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
        s7.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s7.setDescription("""
            â˜¢ï¸ SCÃ‰NARIO FUTUR : Catastrophe de la Guerre NuclÃ©aire & Hiver StratosphÃ©rique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Conflit nucléaire à haute intensité déclenchant d'immenses tempêtes de feu urbaines et l'injection massive de carbone suie dans la stratosphère.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute brutale de l'insolation solaire au sol (-70%) et refroidissement planétaire de -15°C à -25°C.
            • Effondrement total de la photosynthèse et rupture généralisée des chaînes alimentaires en moins de 60 jours.
            • Survie démographique résiduelle restreinte aux refuges souterrains, biomes marins profonds et serres protégées.
            """);
        java.util.Map<String, Boolean> s7Engines = s7.getTypeBEngineStates();
        s7Engines.put("NuclearWarfareClimateEngine", true);
        s7Engines.put("NuclearSafetyRadiotoxicityEngine", true);
        s7Engines.put("BifurcationChaosEngine", true);
        s7Engines.put("VolcanicTephraRefugiaEngine", true);

        // Targeted Strategic Nuclear Strike Package (Anti-Forces & Anti-Cities)
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Washington D.C. & Pentagone", 2035, 38.88, -77.05, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Silos Minot AFB (Dakota du Nord)", 2035, 48.41, -101.35, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Silos Malmstrom AFB (Montana)", 2035, 47.50, -111.18, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Bunker : Complexe Cheyenne Mountain / NORAD", 2035, 38.74, -104.84, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Cités : Agglomération New York & Hub Maritime", 2035, 40.71, -74.00, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Cités : Mégalopole Los Angeles / Long Beach", 2035, 34.05, -118.24, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Moscou & Centre de Commandement", 2035, 55.75, 37.61, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Base Bombardiers Engels-2 (Saratov)", 2035, 51.48, 46.21, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Champs de Silos ICBM Kozelsk", 2035, 54.04, 35.80, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Navale : Chantiers Sous-Marins Severodvinsk", 2035, 64.56, 39.83, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Cités : Métropole Saint-Pétersbourg & Baltique", 2035, 59.93, 30.33, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Londres & Amirauté Britannique", 2035, 51.50, -0.12, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Navale : Base SNLE Faslane / HMNB Clyde", 2035, 56.06, -4.81, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Paris & Commandement des Forces Aériennes", 2035, 48.85, 2.35, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Navale : Base SNLE Île Longue (Brest)", 2035, 48.30, -4.50, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Pékin & Commission Militaire Centrale", 2035, 39.90, 116.40, 0.0, 8.5));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Silos DF-41 Yumen / Hami (Gansu)", 2035, 40.28, 97.04, 0.0, 9.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Cités : Mégalopole Industrielle Shanghai / Yangtze", 2035, 31.23, 121.47, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Tokyo & Complexe Industriel Kanto", 2035, 35.68, 139.69, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Base Stratégique Andersen (Guam)", 2035, 13.58, 144.92, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Anti-Forces : Base Aérienne Ramstein (OTAN)", 2035, 49.43, 7.60, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : New Delhi & Centres Stratégiques", 2035, 28.61, 77.20, 0.0, 8.0));
        s7.getClimateEvents().add(new ClimateEvent("NUCLEAR_STRIKE", "Frappe Stratégique : Islamabad & Complexe Nucléaire Kahuta", 2035, 33.68, 73.04, 0.0, 8.0));

        list.add(s7);

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
        s6.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s6.setDescription("""
            🤖 SCÉNARIO FUTUR : Singularité Technologique & Énergie de Fusion D-T
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Franchissement du seuil d'émergence d'une Super-Intelligence Artificielle (ASI) et maîtrise industrielle de la fusion nucléaire deutérium-tritium.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Croissance exponentielle des rendements de la recherche et automatisation intégrale du travail physique.
            • Déploiement massif de réacteurs à fusion deutérium-tritium conférant une abondance énergétique quasi-infinie.
            • Élimination des pénuries matérielles, transition vers l'échelle de Kardashev Type I et régulation systémique.
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
        s8.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s8.setDescription("""
            â›ï¸ SCÃ‰NARIO FUTUR : Ã‰puisement du Phosphate de Roche (Peak P 2050)
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Épuisement géologique complet des gisements de phosphate de roche bon marché sans transition vers un recyclage circulaire intégral.
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Chute inexorable des rendements agricoles sous déficit de fertilisation phosphatée minérale (stœchiométrie N-P-K).
            • Flambée des prix alimentaires mondiaux, crises de famine urbaine et tensions géopolitiques autour des derniers gisements.
            • Re-localisation agricole d'urgence, baisse de la population vers la capacité de charge organique et recyclage des flux.
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
        s9.setEcologyPreset(EcologyPreset.EARTH_STANDARD);
        s9.setDescription("""
            🌋 SCÉNARIO FUTUR : Super-Volcan VEI-8 & Refroidissement Vulcanologique
            
            [DESCRIPTION DES TERMES DE FORÇAGE PHYSIQUE (T₀)]
            Éruption super-volcanique de degré VEI-8 éjectant plus de 1000 km³ de cendres et de dioxyde de soufre (SO₂) dans la haute atmosphère.
            
            [CONDITIONS INITIALES PHYSIQUES (T₀)]
            • Stock Capital Physique (K₀) : 25 000 kg/habitant (infrastructures avancées et serres automatisées).
            • Énergie Stockée (E₀) : 20 000 MJ/habitant (centrales nucléaires et géothermiques).
            • Réserves Alimentaires (F₀) : 3,0 mois (destructions agricoles par cendres).
            • Savoir Archivé (I₀) : 5 000 000 bits/habitant (savoir automatisé & archives).
            
            [OBSERVABLES CLÉS DU SCÉNARIO]
            • Hiver volcanique pluriannuel (baisse de 8°C à 12°C des températures globales pendant 5 à 10 ans).
            • Dépôts massifs de téphras toxiques détruisant les sols agricoles, les toitures urbaines et les réseaux électriques.
            • Déploiement d'une logistique de crise alimentaire basée sur les stocks stratégiques et les cultures protégées.
            """);
        java.util.Map<String, Boolean> s9Engines = s9.getTypeBEngineStates();
        s9Engines.put("VolcanicTephraRefugiaEngine", true);
        s9Engines.put("BifurcationChaosEngine", true);
        s9Engines.put("World3HybridEngine", true);
        list.add(s9);

        // --- EXOPLANETARY & FUTURE EXPANSION SCENARIOS (2050+) ---

        // 1. MOON (2035)
        Scenario sMoon = new Scenario();
        sMoon.setPresetKey("moon_shackleton_2035");
        sMoon.setName("Base Lunaire Shackleton & Tubes de Lave Marius Hills (2035)");
        sMoon.setStartDateYear(2035);
        sMoon.setEndDateYear(2085);
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
            ⚪ SCÉNARIO EXOPLANÉTAIRE : Base Lunaire Polaire & Tubes de Lave Sous-Terrain (2035 ap. J.-C.)
            
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

        // 2. MARS (2050)
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

        // 3. VENUS (2080)
        Scenario sVenus = new Scenario();
        sVenus.setPresetKey("venus_cloud_cities_2080");
        sVenus.setName("Cités Flottantes Vénusiennes : Aérostats de Haute Altitude (2080)");
        sVenus.setStartDateYear(2080);
        sVenus.setEndDateYear(2180);
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
            🟡 SCÉNARIO EXOPLANÉTAIRE : Aérostats Flottants & Extraction Atmosphérique Vénusienne (2080 ap. J.-C.)
            
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

        // 4. MERCURY (2120)
        Scenario sMercury = new Scenario();
        sMercury.setPresetKey("mercury_caloris_forge_2120");
        sMercury.setName("Forge Solaire de Mercure & Bassin Caloris (2120)");
        sMercury.setStartDateYear(2120);
        sMercury.setEndDateYear(2220);
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
            ⚪ SCÉNARIO EXOPLANÉTAIRE : Forges Héliothermiques de Mercure & Métaux Lourds de Caloris (2120 ap. J.-C.)
            
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

        // 5. TITAN (2150)
        Scenario sTitan = new Scenario();
        sTitan.setPresetKey("titan_cryo_methane_2150");
        sTitan.setName("Colonie Cryogénique de Titan & Hydrocarbures Kraken Mare (2150)");
        sTitan.setStartDateYear(2150);
        sTitan.setEndDateYear(2250);
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
            🪐 SCÉNARIO EXOPLANÉTAIRE : Cryocités de Titan & Mers d'Hydrocarbures de Kraken Mare (2150 ap. J.-C.)
            
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

        // 6. SUPER-EARTH (2200)
        Scenario sSuperEarth = new Scenario();
        sSuperEarth.setPresetKey("super_earth_gaia_2200");
        sSuperEarth.setName("Arche Interstellaire : Première Colonisation de Gaia Prime (2200)");
        sSuperEarth.setStartDateYear(2200);
        sSuperEarth.setEndDateYear(2300);
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
            🌍 SCÉNARIO EXOPLANÉTAIRE : Colonisation d'une Super-Terre à Forte Gravité (2200 ap. J.-C.)
            
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

        // 7. EYEBALL WORLD (2220)
        Scenario sEyeball = new Scenario();
        sEyeball.setPresetKey("eyeball_world_twilight_2220");
        sEyeball.setName("Monde Synchrone : L'Anneau du Crépuscule & Terminateur (2220)");
        sEyeball.setStartDateYear(2220);
        sEyeball.setEndDateYear(2320);
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
            👁️ SCÉNARIO EXOPLANÉTAIRE : Civilisation de l'Anneau Crépusculaire Synchrone (2220 ap. J.-C.)
            
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

        // 8. WATER WORLD / OCEANIA (2100)
        Scenario sOceania = new Scenario();
        sOceania.setPresetKey("oceania_aquapolis_2100");
        sOceania.setName("Monde Océan : Aquapolis & Cités Abyssales d'Oceania (2100)");
        sOceania.setStartDateYear(2100);
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
            🌊 SCÉNARIO EXOPLANÉTAIRE : Planète Océan & Mégastructures Flottantes (2100 ap. J.-C.)
            
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

        // 9. ICE WORLD / BOREAS (2120)
        Scenario sBoreas = new Scenario();
        sBoreas.setPresetKey("boreas_subglacial_2120");
        sBoreas.setName("Monde Glaciaire Boreas : Havres Géothermiques & Cités Sous Glace (2120)");
        sBoreas.setStartDateYear(2120);
        sBoreas.setEndDateYear(2220);
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
            ❄️ SCÉNARIO EXOPLANÉTAIRE : Cités Sous-Glaciaires & Puits Géothermiques de Boreas (2120 ap. J.-C.)
            
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

