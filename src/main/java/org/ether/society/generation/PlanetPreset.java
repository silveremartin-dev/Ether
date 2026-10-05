package org.ether.society.generation;

import java.util.List;

/**
 * Configuration preset for procedural planet generation.
 * 
 * @param name                  Name of the preset (e.g., "Terran")
 * @param resolution            H3 resolution (e.g., 5, 6, 7, 8)
 * @param radiusKm              Planetary radius in kilometers (e.g., 6371.0 for Earth)
 * @param dayLengthHours        Duration of one day in hours (e.g., 24.0 for Earth)
 * @param axialTiltDegrees      Axial tilt / inclination in degrees (e.g., 23.5° for Earth)
 * @param yearLengthDays        Duration of one year in Earth days (e.g., 365.25 for Earth)
 * @param distanceToSunAU       Distance from star in Astronomical Units (e.g., 1.0 AU)
 * @param solarLuminosity       Star luminosity relative to Sun (e.g., 1.0 L_sun)
 * @param minAltitudeMeters     Lowest elevation / ocean trench depth (e.g., -11000.0 m)
 * @param maxAltitudeMeters     Highest mountain peak elevation (e.g., 8848.0 m)
 * @param averageTempC          Mean global surface temperature in °C (e.g., 15.0°C for Earth)
 * @param seed                  Random seed
 * @param noiseFrequency        Base frequency of the noise (terrain detail)
 * @param noiseScale            Vertical scale of the noise (elevation range)
 * @param waterLevel            Elevation threshold for ocean (-0.5 to 1.0)
 * @param temperatureGradient   Temperature difference between equator and poles (°C)
 * @param oxygenPercentage      Atmospheric oxygen content O2 percentage (e.g., 21.0%)
 * @param albedo                Planetary surface reflectivity albedo (0.0 to 1.0, e.g. 0.30)
 * @param atmospherePressureAtm Surface atmospheric pressure in atm (e.g. 1.0)
 * @param isSatellite           True if the body is a moon orbiting a parent gas giant or planet
 * @param parentPlanetMassEarthMasses Parent planet mass in Earth masses (for tidal heating calculations)
 * @param orbitalDistanceToParentKm Orbital semi-major axis to parent in km
 * @param co2Ppm                Atmospheric CO2 concentration in ppm
 * @param seismicActivityLevel  Base seismic activity level [0.0, 1.0]
 * @param volcanicActivityLevel Base volcanic activity level [0.0, 1.0]
 * @param customElevBase64      Base64 raster map for custom elevation
 * @param customBiomeBase64     Base64 raster map for custom biomes
 * @param customResourceBase64  Base64 raster map for custom minerals and resources
 * @param customClimateBase64   Base64 raster map for custom temperature
 * @param customRainfallBase64  Base64 raster map for custom precipitation
 * @param customSeasonalityBase64 Base64 raster map for custom seasonality
 * @param elevationUseImport    Flag whether imported elevation tensor map should override noise
 * @param biomeUseImport        Flag whether imported biome tensor map should override noise
 * @param resourceUseImport     Flag whether imported resource tensor map should override noise
 * @param climateUseImport      Flag whether imported climate tensor map should override noise
 * @param rainfallUseImport     Flag whether imported rainfall tensor map should override noise
 * @param seasonalityUseImport  Flag whether imported seasonality tensor map should override noise
 * @param mineralRichness       Mineral resource richness scaling factor
 * @param rareEarthRichness     Rare Earth Elements (REE) and critical mineral richness scaling factor
 */
public record PlanetPreset(
        String name,
        int resolution,
        double radiusKm,
        double dayLengthHours,
        double axialTiltDegrees,
        double yearLengthDays,
        double distanceToSunAU,
        double solarLuminosity,
        double minAltitudeMeters,
        double maxAltitudeMeters,
        double averageTempC,
        long seed,
        double noiseFrequency,
        double noiseScale,
        double waterLevel,
        double temperatureGradient,
        double oxygenPercentage,
        double albedo,
        double atmospherePressureAtm,
        boolean isSatellite,
        double parentPlanetMassEarthMasses,
        double orbitalDistanceToParentKm,
        double co2Ppm,
        double seismicActivityLevel,
        double volcanicActivityLevel,
        String customElevBase64,
        String customBiomeBase64,
        String customResourceBase64,
        String customClimateBase64,
        String customRainfallBase64,
        String customSeasonalityBase64,
        boolean elevationUseImport,
        String elevationMapSource,
        boolean tempUseImport,
        String tempSource,
        long tempSeed,
        boolean precipUseImport,
        String precipSource,
        long precipSeed,
        boolean seasonUseImport,
        String seasonSource,
        long seasonSeed) {

    /* Compact constructor for Jackson deserialization normalization */
    public PlanetPreset {
        if (elevationMapSource == null) {
            String lower = name != null ? name.toLowerCase() : "";
            boolean isSuperEarth = lower.contains("super-terre") || lower.contains("super-earth") || lower.contains("gaia");
            elevationMapSource = isSuperEarth ? "none"
                    : (customElevBase64 != null || lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) ? "earth"
                    : lower.contains("mars") || lower.contains("ares") ? "mars"
                    : lower.contains("vÃ©nus") || lower.contains("venus") || lower.contains("hesperos") ? "venus"
                    : lower.contains("lune") || lower.contains("moon") || lower.contains("selene") ? "moon"
                    : lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes") ? "mercury" : "none";
        }
        if (tempSource == null) tempSource = "";
        if (precipSource == null) precipSource = "";
        if (seasonSource == null) seasonSource = "";
        if (tempSeed == 0L) tempSeed = seed + 100L;
        if (precipSeed == 0L) precipSeed = seed + 1000L;
        if (seasonSeed == 0L) seasonSeed = seed + 2000L;
    }

    /* Modern Earth (2026 baseline) */
    public static final PlanetPreset EARTH_MODERN = new PlanetPreset(
            "Terre (Terran)", 4, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, 12345L, 1.0, 1.0, 0.478, 40.0, 21.0, 0.30, 1.0,
            false, 1.0, 0.0, 420.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre â€” WorldClim v2.1 Bio1 & ERA5 (Composite)", 12445L,
            true, "ðŸŒ Terre â€” WorldClim v2.1 & GPCP v2.3 (Composite)", 13345L,
            true, "ðŸŒ Terre â€” WorldClim v2.1 Bio4 & ERA5 (Composite)", 14345L);

    /* Default Terran / Earth-like settings (alias for modern) */
    public static final PlanetPreset EARTH_LIKE = EARTH_MODERN;

    /* -1 000 ans : DÃ©but Ã‚ge du Fer */
    public static final PlanetPreset EARTH_IRON_1000BP = new PlanetPreset(
            "Terre (-1 000 / DÃ©but Ã‚ge du Fer)", 4, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 14.7, 12344L, 1.0, 1.0, 0.478, 40.0, 20.95, 0.305, 1.0,
            false, 1.0, 0.0, 278.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre (-1000 BP) â€” Ã‚ge du Fer Ancien", 12444L,
            true, "ðŸŒ Terre (-1000 BP) â€” PrÃ©cipitations", 13344L,
            true, "ðŸŒ Terre (-1000 BP) â€” SaisonnalitÃ©", 14344L);

    /* -1 900 ans : Ã‚ge du Bronze Moyen */
    public static final PlanetPreset EARTH_BRONZE_1900BP = new PlanetPreset(
            "Terre (-1 900 / Ã‚ge du Bronze Moyen)", 4, 6371.0, 24.0, 23.6, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, 12345L, 1.0, 1.0, 0.478, 39.5, 20.95, 0.30, 1.0,
            false, 1.0, 0.0, 275.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre (-1900 BP) â€” Ã‚ge du Bronze Moyen", 12445L,
            true, "ðŸŒ Terre (-1900 BP) â€” PrÃ©cipitations", 13345L,
            true, "ðŸŒ Terre (-1900 BP) â€” SaisonnalitÃ©", 14345L);

    /* -3 000 ans (LH) : HolocÃ¨ne tardif */
    public static final PlanetPreset EARTH_LH_3000BP = new PlanetPreset(
            "Terre (-3 000 / HolocÃ¨ne Tardif)", 4, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 14.8, 12346L, 1.0, 1.0, 0.478, 40.0, 20.95, 0.305, 1.0,
            false, 1.0, 0.0, 275.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre (-3000 BP) â€” PalÃ©oclimat Tardif", 12446L,
            true, "ðŸŒ Terre (-3000 BP) â€” PrÃ©cipitations NÃ©oglaciaires", 13346L,
            true, "ðŸŒ Terre (-3000 BP) â€” SaisonnalitÃ©", 14346L);

    /* -6 000 ans (MH) : HolocÃ¨ne moyen avec le Sahara Vert et le lac MÃ©ga-Tchad */
    public static final PlanetPreset EARTH_MH_6000BP = new PlanetPreset(
            "Terre (-6 000 / Sahara Vert)", 4, 6371.0, 24.0, 24.1, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.8, 12347L, 1.0, 1.0, 0.478, 38.0, 20.95, 0.29, 1.0,
            false, 1.0, 0.0, 265.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre (-6000 BP) â€” Optimum Climatique & Sahara Vert", 12447L,
            true, "ðŸŒ Terre (-6000 BP) â€” Mousson Africaine & Lac MÃ©ga-Tchad", 13347L,
            true, "ðŸŒ Terre (-6000 BP) â€” SaisonnalitÃ© HolocÃ¨ne Moyen", 14347L);

    /* -10 000 ans (EH) : HolocÃ¨ne prÃ©coce (-35m niveau de la mer dÃ©glaciation) */
    public static final PlanetPreset EARTH_EH_10000BP = new PlanetPreset(
            "Terre (-10 000 / HolocÃ¨ne PrÃ©coce)", 4, 6371.0, 24.0, 24.2, 365.25, 1.0, 1.0, -11000.0, 8848.0, 13.5, 12348L, 1.0, 1.0, 0.476479, 42.0, 20.9, 0.32, 1.0,
            false, 1.0, 0.0, 260.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre (-10000 BP) â€” PalÃ©oclimat DÃ©glaciation", 12448L,
            true, "ðŸŒ Terre (-10000 BP) â€” PrÃ©cipitations HolocÃ¨ne PrÃ©coce", 13348L,
            true, "ðŸŒ Terre (-10000 BP) â€” SaisonnalitÃ©", 14348L);

    /* -20 000 ans (LGM) : Dernier Maximum Glaciaire (-125m niveau marin eustatique bas) */
    public static final PlanetPreset EARTH_LGM_20000BP = new PlanetPreset(
            "Terre (-20 000 / Maximum Glaciaire)", 4, 6371.0, 24.0, 23.0, 365.25, 1.0, 1.0, -11000.0, 8848.0, 9.0, 12349L, 1.0, 1.0, 0.472568, 55.0, 20.9, 0.36, 1.0,
            false, 1.0, 0.0, 190.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre (-20000 BP) â€” Climat Glaciaire LGM & Inlandsis", 12449L,
            true, "ðŸŒ Terre (-20000 BP) â€” AriditÃ© Glaciaire & Steppe Ã  Mammouths", 13349L,
            true, "ðŸŒ Terre (-20000 BP) â€” Forte SaisonnalitÃ© Glaciaire", 14349L);

    /* -25 000 ans : DÃ©but LGM & BÃ©ringie (-100m niveau marin) */
    public static final PlanetPreset EARTH_LGM_ONSET_25000BP = new PlanetPreset(
            "Terre (-25 000 / DÃ©but LGM & BÃ©ringie)", 4, 6371.0, 24.0, 23.2, 365.25, 1.0, 1.0, -11000.0, 8848.0, 10.5, 12351L, 1.0, 1.0, 0.473655, 52.0, 20.9, 0.35, 1.0,
            false, 1.0, 0.0, 205.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre (-25000 BP) â€” DÃ©but LGM & BÃ©ringie", 12451L,
            true, "ðŸŒ Terre (-25000 BP) â€” AriditÃ© & Steppe", 13351L,
            true, "ðŸŒ Terre (-25000 BP) â€” SaisonnalitÃ© Glaciaire", 14351L);

    /* -50 000 ans : Stade Isotopique 3 & Sahul (-60m niveau marin) */
    public static final PlanetPreset EARTH_MIS3_50000BP = new PlanetPreset(
            "Terre (-50 000 / Stade Isotopique 3 & Sahul)", 4, 6371.0, 24.0, 23.4, 365.25, 1.0, 1.0, -11000.0, 8848.0, 12.0, 12352L, 1.0, 1.0, 0.475393, 48.0, 20.9, 0.335, 1.0,
            false, 1.0, 0.0, 220.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre (-50000 BP) â€” Stade Isotopique 3 & Sahul", 12452L,
            true, "ðŸŒ Terre (-50000 BP) â€” PrÃ©cipitations IntermÃ©diaires", 13352L,
            true, "ðŸŒ Terre (-50000 BP) â€” SaisonnalitÃ© MIS 3", 14352L);

    /* -100 000 ans (LIG) : Dernier Interglaciaire / EÃ©mien (0m datum datum / +6m stand) */
    public static final PlanetPreset EARTH_LIG_100000BP = new PlanetPreset(
            "Terre (-100 000 / Dernier Interglaciaire)", 4, 6371.0, 24.0, 23.8, 365.25, 1.0, 1.0, -11000.0, 8848.0, 16.2, 12350L, 1.0, 1.0, 0.478, 36.0, 20.95, 0.295, 1.0,
            false, 1.0, 0.0, 280.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "ðŸŒ Terre (-100000 BP) â€” Dernier Interglaciaire EÃ©mien", 12450L,
            true, "ðŸŒ Terre (-100000 BP) â€” HumiditÃ© & Savane Trans-saharienne", 13350L,
            true, "ðŸŒ Terre (-100000 BP) â€” SaisonnalitÃ© EÃ©mienne", 14350L);

    /* Mars-like settings */
    public static final PlanetPreset MARS_LIKE = new PlanetPreset(
            "Mars (Ares)", 4, 3389.5, 24.6, 25.2, 687.0, 1.52, 1.0, -8000.0, 21229.0, -60.0, 98765L, 1.2, 1.2, -0.4, 50.0, 0.13, 0.25, 0.006,
            false, 1.0, 0.0, 950000.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "mars",
            true, "ðŸ”´ Mars â€” MGS TES Thermal Radiometry", 98865L,
            true, "ðŸ”´ Mars â€” Frost & Sublimation Model", 99765L,
            true, "ðŸ”´ Mars â€” Orbital Eccentricity Insolation Model", 100765L);

    public static final PlanetPreset DESERT_WORLD = MARS_LIKE;

    /* Venusian settings */
    public static final PlanetPreset VENUS_LIKE = new PlanetPreset(
            "VÃ©nus (Hesperos)", 4, 6051.8, 2802.0, 177.3, 224.7, 0.72, 1.0, -3000.0, 11000.0, 464.0, 55555L, 0.6, 0.7, -0.5, 20.0, 0.0, 0.75, 92.0,
            false, 1.0, 0.0, 965000.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "venus",
            true, "ðŸŸ¡ VÃ©nus â€” Magellan SAR & Hypsometric Model", 55655L,
            true, "ðŸŸ¡ VÃ©nus â€” H2SO4 Virga Cycle Model", 56555L,
            true, "ðŸŸ¡ VÃ©nus â€” Super-Rotation Low Variance Model", 57555L);

    /* Moon-like satellite settings */
    public static final PlanetPreset MOON_LIKE = new PlanetPreset(
            "Lune (Selene)", 4, 1737.4, 708.0, 1.5, 365.25, 1.0, 1.0, -9000.0, 10700.0, -20.0, 88888L, 0.9, 1.1, -0.5, 60.0, 0.0, 0.12, 0.0,
            true, 1.0, 384400.0, 0.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "moon",
            true, "âšª Lune â€” LRO Diviner Thermal Radiometer", 88988L,
            true, "âšª Lune â€” LRO LEND Vacuum Exosphere", 89888L,
            true, "âšª Lune â€” Diurnal Insolation Amplitude Model", 90888L);

    /* Mercury settings */
    public static final PlanetPreset MERCURY_LIKE = new PlanetPreset(
            "Mercure (Hermes)", 4, 2439.7, 4222.6, 0.034, 87.97, 0.387, 1.0, -5000.0, 4480.0, 167.0, 66666L, 0.9, 1.0, -0.5, 90.0, 0.0, 0.14, 0.0,
            false, 1.0, 0.0, 0.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "mercury",
            true, "âšª Mercure â€” MESSENGER MLA Extreme Thermal Model", 66766L,
            true, "âšª Mercure â€” MESSENGER Exospheric Vacuum Model", 67666L,
            true, "âšª Mercure â€” 3:2 Spin-Orbit Thermal Variance Model", 68666L);

    /* Titan-like moon settings */
    public static final PlanetPreset TITAN_LIKE = new PlanetPreset(
            "Titan (Cryo-Lune)", 4, 2574.0, 382.0, 26.7, 10759.0, 9.5, 1.0, -2000.0, 5000.0, -179.0, 77711L, 0.8, 1.0, 0.2, 25.0, 0.0, 0.22, 1.45,
            true, 317.8, 1221870.0, 5000.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 77811L,
            false, "", 78711L,
            false, "", 79711L);

    /* Super-Earth settings */
    public static final PlanetPreset SUPER_EARTH = new PlanetPreset(
            "Super-Terre (Gaia Prime)", 4, 11000.0, 16.0, 12.0, 480.0, 1.0, 1.2, -14000.0, 12000.0, 22.0, 44444L, 1.3, 1.2, 0.1, 45.0, 25.0, 0.28, 1.5,
            false, 1.0, 0.0, 600.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 44544L,
            false, "", 45444L,
            false, "", 46444L);

    /* Tidally locked Eyeball world */
    public static final PlanetPreset EYEBALL_WORLD = new PlanetPreset(
            "Monde Synchrone (Eyeball)", 4, 5500.0, 720.0, 0.0, 30.0, 0.15, 0.05, -10000.0, 9000.0, 20.0, 33333L, 1.0, 1.0, 0.0, 90.0, 18.0, 0.35, 0.8,
            false, 1.0, 0.0, 1200.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 33433L,
            false, "", 34333L,
            false, "", 35333L);

    /* Water world settings */
    public static final PlanetPreset WATER_WORLD = new PlanetPreset(
            "Monde OcÃ©an (Oceania)", 4, 7000.0, 21.0, 18.0, 410.0, 1.0, 1.1, -12000.0, 3000.0, 25.0, 54321L, 0.8, 0.8, 0.35, 30.0, 23.0, 0.25, 1.2,
            false, 1.0, 0.0, 500.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 54421L,
            false, "", 55321L,
            false, "", 56321L);

    /* Ice world settings */
    public static final PlanetPreset ICE_WORLD = new PlanetPreset(
            "Monde Glaciaire (Boreas)", 4, 4800.0, 32.0, 45.0, 520.0, 2.5, 0.9, -6000.0, 7000.0, -45.0, 11111L, 0.5, 1.5, 0.1, 70.0, 15.0, 0.60, 0.7,
            false, 1.0, 0.0, 300.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 11211L,
            false, "", 12111L,
            false, "", 13111L);

    public static final PlanetPreset ARCHIPELAGO = new PlanetPreset(
            "Archipel", 4, 6371.0, 24.0, 23.5, 365.0, 1.0, 1.0, -11000.0, 8848.0, 18.0, 77777L, 1.5, 1.5, 0.6, 45.0, 21.0, 0.30, 1.0,
            false, 1.0, 0.0, 420.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 77877L,
            false, "", 78777L,
            false, "", 79777L);

    /*
     * With name.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @param newName the new name parameter (String)
     * @return the resulting computation or state reference
     */
    public PlanetPreset withName(String newName) {
        return new PlanetPreset(
                newName, resolution, radiusKm, dayLengthHours, axialTiltDegrees, yearLengthDays,
                distanceToSunAU, solarLuminosity, minAltitudeMeters, maxAltitudeMeters, averageTempC,
                seed, noiseFrequency, noiseScale, waterLevel, temperatureGradient, oxygenPercentage,
                albedo, atmospherePressureAtm, isSatellite, parentPlanetMassEarthMasses,
                orbitalDistanceToParentKm, co2Ppm, seismicActivityLevel, volcanicActivityLevel,
                customElevBase64, customBiomeBase64, customResourceBase64, customClimateBase64,
                customRainfallBase64, customSeasonalityBase64, elevationUseImport, elevationMapSource,
                tempUseImport, tempSource, tempSeed, precipUseImport, precipSource, precipSeed,
                seasonUseImport, seasonSource, seasonSeed
        );
    }

    /*
     * With seismic and volcanic.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @param sVal the s val parameter (double)
     * @param vVal the v val parameter (double)
     * @return the resulting computation or state reference
     */
    public PlanetPreset withSeismicAndVolcanic(double sVal, double vVal) {
        return new PlanetPreset(
                name, resolution, radiusKm, dayLengthHours, axialTiltDegrees, yearLengthDays,
                distanceToSunAU, solarLuminosity, minAltitudeMeters, maxAltitudeMeters, averageTempC,
                seed, noiseFrequency, noiseScale, waterLevel, temperatureGradient, oxygenPercentage,
                albedo, atmospherePressureAtm, isSatellite, parentPlanetMassEarthMasses,
                orbitalDistanceToParentKm, co2Ppm, sVal, vVal, customElevBase64, customBiomeBase64,
                customResourceBase64, customClimateBase64, customRainfallBase64, customSeasonalityBase64,
                elevationUseImport, elevationMapSource, tempUseImport, tempSource, tempSeed,
                precipUseImport, precipSource, precipSeed, seasonUseImport, seasonSource, seasonSeed
        );
    }

    /*
     * Get presets.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @return the resulting computation or state reference
     */
    public static List<PlanetPreset> getPresets() {
        return List.of(
                EARTH_MODERN,
                EARTH_IRON_1000BP,
                EARTH_BRONZE_1900BP,
                EARTH_LH_3000BP,
                EARTH_MH_6000BP,
                EARTH_EH_10000BP,
                EARTH_LGM_20000BP,
                EARTH_LGM_ONSET_25000BP,
                EARTH_MIS3_50000BP,
                EARTH_LIG_100000BP,
                MARS_LIKE,
                VENUS_LIKE,
                MOON_LIKE,
                MERCURY_LIKE,
                TITAN_LIKE,
                SUPER_EARTH,
                EYEBALL_WORLD,
                WATER_WORLD,
                ICE_WORLD,
                ARCHIPELAGO
        );
    }

    /*
     * Get associated epoch year.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @return the resulting computation or state reference
     */
    public long getAssociatedEpochYear() {
        String lower = name != null ? name.toLowerCase() : "";
        if (lower.contains("-100") || lower.contains("lig") || lower.contains("interglaciaire") || lower.contains("eemian")) return -100000L;
        if (lower.contains("-50") || lower.contains("sahul") || lower.contains("mis3") || lower.contains("mis 3")) return -50000L;
        if (lower.contains("-25") || lower.contains("beringia") || lower.contains("bÃ©ringie")) return -25000L;
        if (lower.contains("-20") || lower.contains("lgm") || lower.contains("glaciaire")) return -20000L;
        if (lower.contains("-10") || lower.contains("eh") || lower.contains("prÃ©coce") || lower.contains("early holocene")) return -10000L;
        if (lower.contains("-6") || lower.contains("mh") || lower.contains("sahara") || lower.contains("mid holocene")) return -6000L;
        if (lower.contains("-3") || lower.contains("lh") || lower.contains("tardif") || lower.contains("late holocene")) return -3000L;
        if (lower.contains("-1900") || lower.contains("bronze")) return -1900L;
        if (lower.contains("-1000") || lower.contains("iron") || lower.contains("fer")) return -1000L;
        return 2026L;
    }

    /*
     * Get canonical planet.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @return the resulting computation or state reference
     */
    public String getCanonicalPlanet() {
        String lower = name != null ? name.toLowerCase() : "";
        if (lower.contains("super-terre") || lower.contains("super-earth") || lower.contains("gaia")) return "none";
        if (lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) return "earth";
        if (lower.contains("mars") || lower.contains("ares")) return "mars";
        if (lower.contains("vÃ©nus") || lower.contains("venus") || lower.contains("hesperos")) return "venus";
        if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene")) return "moon";
        if (lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes")) return "mercury";
        return "none";
    }

    /*
     * Get preset description.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @return the resulting computation or state reference
     */
    public String getPresetDescription() {
        return org.ether.society.i18n.I18n.getPlanetPresetDescription(name);
    }

    /*
     * Is tidal locked.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @return the resulting computation or state reference
     */
    public boolean isTidalLocked() {
        return name != null && (name.contains("Eyeball") || name.contains("Synchrone") || name.toLowerCase().contains("tidally locked"));
    }

    /*
     * Cell count.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @return the resulting computation or state reference
     */
    public long cellCount() {
        return 2 + 120 * (long) Math.pow(7, resolution);
    }

    /*
     * Converts a normalized waterLevel [0..1] into physical sea level elevation in meters.
     * Uses the calibrated NOAA ETOPO datum 0.478 corresponding to 0m MSL for Earth topography.
     */
    public static double waterLevelToMeters(double waterLevel, double minAltMeters, double maxAltMeters) {
        if (minAltMeters < 0 && maxAltMeters > 0) {
            double datum = 0.478;
            if (waterLevel <= datum) {
                return (waterLevel / datum - 1.0) * Math.abs(minAltMeters);
            } else {
                return ((waterLevel - datum) / (1.0 - datum)) * maxAltMeters;
            }
        } else {
            return minAltMeters + waterLevel * (maxAltMeters - minAltMeters);
        }
    }

    /*
     * Converts a physical sea level in meters into a normalized waterLevel threshold [0..1].
     */
    public static double metersToWaterLevel(double seaLevelMeters, double minAltMeters, double maxAltMeters) {
        if (minAltMeters < 0 && maxAltMeters > 0) {
            double datum = 0.478;
            if (seaLevelMeters <= 0.0) {
                double fraction = 1.0 + Math.max(minAltMeters, seaLevelMeters) / Math.abs(minAltMeters);
                return datum * fraction;
            } else {
                double fraction = Math.min(maxAltMeters, seaLevelMeters) / maxAltMeters;
                return datum + (1.0 - datum) * fraction;
            }
        } else {
            if (Math.abs(maxAltMeters - minAltMeters) < 1e-6) return 0.0;
            return (seaLevelMeters - minAltMeters) / (maxAltMeters - minAltMeters);
        }
    }

    /*
     * Returns the physical sea level in meters for this preset.
     */
    public double seaLevelMeters() {
        return waterLevelToMeters(waterLevel, minAltitudeMeters, maxAltitudeMeters);
    }

    /*
     * With resolution.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @param newResolution the new resolution parameter (int)
     * @return the resulting computation or state reference
     */
    public PlanetPreset withResolution(int newResolution) {
        newResolution = Math.max(4, newResolution);
        return new PlanetPreset(name, newResolution, radiusKm, dayLengthHours, axialTiltDegrees, yearLengthDays,
                distanceToSunAU, solarLuminosity, minAltitudeMeters, maxAltitudeMeters, averageTempC, seed,
                noiseFrequency, noiseScale, waterLevel, temperatureGradient, oxygenPercentage, albedo,
                atmospherePressureAtm, isSatellite, parentPlanetMassEarthMasses, orbitalDistanceToParentKm,
                co2Ppm, seismicActivityLevel, volcanicActivityLevel, customElevBase64, customBiomeBase64,
                customResourceBase64, customClimateBase64, customRainfallBase64, customSeasonalityBase64,
                elevationUseImport, elevationMapSource, tempUseImport, tempSource, tempSeed,
                precipUseImport, precipSource, precipSeed, seasonUseImport, seasonSource, seasonSeed);
    }

    @Override
    /*
     * To string.
     * Enforces physical invariants and updates associated state variables within {@code PlanetPreset}.
     *
     * @return the resulting computation or state reference
     */
    public String toString() {
        return name;
    }
}



