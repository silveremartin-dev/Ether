package org.ether.society.procedural;

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
 * @param solarLuminosity       Star luminosity relative to Sun (e.g., 1.0 L☉)
 * @param minAltitudeMeters     Lowest elevation / ocean trench depth (e.g., -11000.0 m)
 * @param maxAltitudeMeters     Highest mountain peak elevation (e.g., 8848.0 m)
 * @param averageTempC          Mean global surface temperature in °C (e.g., 15.0°C for Earth)
 * @param seed                  Random seed
 * @param noiseFrequency        Base frequency of the noise (terrain detail)
 * @param noiseScale            Vertical scale of the noise (elevation range)
 * @param waterLevel            Elevation threshold for ocean (-0.5 to 1.0)
 * @param temperatureGradient   Temperature difference between equator and poles (°C)
 * @param oxygenPercentage      Atmospheric oxygen content O₂ percentage (e.g., 21.0%)
 * @param albedo                Planetary surface reflectivity albedo (0.0 to 1.0, e.g. 0.30)
 * @param atmospherePressureAtm Surface atmospheric pressure in atm (e.g. 1.0)
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

    /** Compact constructor for Jackson deserialization normalization */
    public PlanetPreset {
        if (elevationMapSource == null) {
            String lower = name != null ? name.toLowerCase() : "";
            boolean isSuperEarth = lower.contains("super-terre") || lower.contains("super-earth") || lower.contains("gaia");
            elevationMapSource = isSuperEarth ? "none"
                    : (customElevBase64 != null || lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) ? "earth"
                    : lower.contains("mars") || lower.contains("ares") ? "mars"
                    : lower.contains("vénus") || lower.contains("venus") || lower.contains("hesperos") ? "venus"
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

    /** Modern Earth (2026 baseline) */
    public static final PlanetPreset EARTH_MODERN = new PlanetPreset(
            "Terre (Terran)", 3, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, 12345L, 1.0, 1.0, 0.38, 40.0, 21.0, 0.30, 1.0,
            false, 1.0, 0.0, 420.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre — WorldClim v2.1 Bio1 & ERA5 (Composite)", 12445L,
            true, "🌍 Terre — WorldClim v2.1 & GPCP v2.3 (Composite)", 13345L,
            true, "🌍 Terre — WorldClim v2.1 Bio4 & ERA5 (Composite)", 14345L);

    /** Default Terran / Earth-like settings (alias for modern) */
    public static final PlanetPreset EARTH_LIKE = EARTH_MODERN;

    /** -1 000 ans : Début Âge du Fer */
    public static final PlanetPreset EARTH_IRON_1000BP = new PlanetPreset(
            "Terre (-1 000 / Début Âge du Fer)", 3, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 14.7, 12344L, 1.0, 1.0, 0.38, 40.0, 20.95, 0.305, 1.0,
            false, 1.0, 0.0, 278.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre (-1000 BP) — Âge du Fer Ancien", 12444L,
            true, "🌍 Terre (-1000 BP) — Précipitations", 13344L,
            true, "🌍 Terre (-1000 BP) — Saisonnalité", 14344L);

    /** -1 900 ans : Âge du Bronze Moyen */
    public static final PlanetPreset EARTH_BRONZE_1900BP = new PlanetPreset(
            "Terre (-1 900 / Âge du Bronze Moyen)", 3, 6371.0, 24.0, 23.6, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, 12345L, 1.0, 1.0, 0.38, 39.5, 20.95, 0.30, 1.0,
            false, 1.0, 0.0, 275.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre (-1900 BP) — Âge du Bronze Moyen", 12445L,
            true, "🌍 Terre (-1900 BP) — Précipitations", 13345L,
            true, "🌍 Terre (-1900 BP) — Saisonnalité", 14345L);

    /** -3 000 ans (LH) : Holocène tardif */
    public static final PlanetPreset EARTH_LH_3000BP = new PlanetPreset(
            "Terre (-3 000 / Holocène Tardif)", 3, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 14.8, 12346L, 1.0, 1.0, 0.38, 40.0, 20.95, 0.305, 1.0,
            false, 1.0, 0.0, 275.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre (-3000 BP) — Paléoclimat Tardif", 12446L,
            true, "🌍 Terre (-3000 BP) — Précipitations Néoglaciaires", 13346L,
            true, "🌍 Terre (-3000 BP) — Saisonnalité", 14346L);

    /** -6 000 ans (MH) : Holocène moyen avec le Sahara Vert et le lac Méga-Tchad */
    public static final PlanetPreset EARTH_MH_6000BP = new PlanetPreset(
            "Terre (-6 000 / Sahara Vert)", 3, 6371.0, 24.0, 24.1, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.8, 12347L, 1.0, 1.0, 0.38, 38.0, 20.95, 0.29, 1.0,
            false, 1.0, 0.0, 265.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre (-6000 BP) — Optimum Climatique & Sahara Vert", 12447L,
            true, "🌍 Terre (-6000 BP) — Mousson Africaine & Lac Méga-Tchad", 13347L,
            true, "🌍 Terre (-6000 BP) — Saisonnalité Holocène Moyen", 14347L);

    /** -10 000 ans (EH) : Holocène précoce */
    public static final PlanetPreset EARTH_EH_10000BP = new PlanetPreset(
            "Terre (-10 000 / Holocène Précoce)", 3, 6371.0, 24.0, 24.2, 365.25, 1.0, 1.0, -11000.0, 8848.0, 13.5, 12348L, 1.0, 1.0, 0.36, 42.0, 20.9, 0.32, 1.0,
            false, 1.0, 0.0, 260.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre (-10000 BP) — Paléoclimat Déglaciation", 12448L,
            true, "🌍 Terre (-10000 BP) — Précipitations Holocène Précoce", 13348L,
            true, "🌍 Terre (-10000 BP) — Saisonnalité", 14348L);

    /** -20 000 ans (LGM) : Dernier Maximum Glaciaire */
    public static final PlanetPreset EARTH_LGM_20000BP = new PlanetPreset(
            "Terre (-20 000 / Maximum Glaciaire)", 3, 6371.0, 24.0, 23.0, 365.25, 1.0, 1.0, -11000.0, 8848.0, 9.0, 12349L, 1.0, 1.0, 0.32, 55.0, 20.9, 0.36, 1.0,
            false, 1.0, 0.0, 190.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre (-20000 BP) — Climat Glaciaire LGM & Inlandsis", 12449L,
            true, "🌍 Terre (-20000 BP) — Aridité Glaciaire & Steppe à Mammouths", 13349L,
            true, "🌍 Terre (-20000 BP) — Forte Saisonnalité Glaciaire", 14349L);

    /** -25 000 ans : Début LGM & Béringie */
    public static final PlanetPreset EARTH_LGM_ONSET_25000BP = new PlanetPreset(
            "Terre (-25 000 / Début LGM & Béringie)", 3, 6371.0, 24.0, 23.2, 365.25, 1.0, 1.0, -11000.0, 8848.0, 10.5, 12351L, 1.0, 1.0, 0.33, 52.0, 20.9, 0.35, 1.0,
            false, 1.0, 0.0, 205.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre (-25000 BP) — Début LGM & Béringie", 12451L,
            true, "🌍 Terre (-25000 BP) — Aridité & Steppe", 13351L,
            true, "🌍 Terre (-25000 BP) — Saisonnalité Glaciaire", 14351L);

    /** -50 000 ans : Stade Isotopique 3 & Sahul */
    public static final PlanetPreset EARTH_MIS3_50000BP = new PlanetPreset(
            "Terre (-50 000 / Stade Isotopique 3 & Sahul)", 3, 6371.0, 24.0, 23.4, 365.25, 1.0, 1.0, -11000.0, 8848.0, 12.0, 12352L, 1.0, 1.0, 0.34, 48.0, 20.9, 0.335, 1.0,
            false, 1.0, 0.0, 220.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre (-50000 BP) — Stade Isotopique 3 & Sahul", 12452L,
            true, "🌍 Terre (-50000 BP) — Précipitations Intermédiaires", 13352L,
            true, "🌍 Terre (-50000 BP) — Saisonnalité MIS 3", 14352L);

    /** -100 000 ans (LIG) : Dernier Interglaciaire / Eémien */
    public static final PlanetPreset EARTH_LIG_100000BP = new PlanetPreset(
            "Terre (-100 000 / Dernier Interglaciaire)", 3, 6371.0, 24.0, 23.8, 365.25, 1.0, 1.0, -11000.0, 8848.0, 16.2, 12350L, 1.0, 1.0, 0.39, 36.0, 20.95, 0.295, 1.0,
            false, 1.0, 0.0, 280.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "earth",
            true, "🌍 Terre (-100000 BP) — Dernier Interglaciaire Eémien", 12450L,
            true, "🌍 Terre (-100000 BP) — Humidité & Savane Trans-saharienne", 13350L,
            true, "🌍 Terre (-100000 BP) — Saisonnalité Eémienne", 14350L);

    /** Mars-like settings */
    public static final PlanetPreset MARS_LIKE = new PlanetPreset(
            "Mars (Ares)", 3, 3389.5, 24.6, 25.2, 687.0, 1.52, 1.0, -8000.0, 21229.0, -60.0, 98765L, 1.2, 1.2, -0.4, 50.0, 0.13, 0.25, 0.006,
            false, 1.0, 0.0, 950000.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "mars",
            true, "🔴 Mars — MGS TES Thermal Radiometry", 98865L,
            true, "🔴 Mars — Frost & Sublimation Model", 99765L,
            true, "🔴 Mars — Orbital Eccentricity Insolation Model", 100765L);

    public static final PlanetPreset DESERT_WORLD = MARS_LIKE;

    /** Venusian settings */
    public static final PlanetPreset VENUS_LIKE = new PlanetPreset(
            "Vénus (Hesperos)", 3, 6051.8, 2802.0, 177.3, 224.7, 0.72, 1.0, -3000.0, 11000.0, 464.0, 55555L, 0.6, 0.7, -0.5, 20.0, 0.0, 0.75, 92.0,
            false, 1.0, 0.0, 965000.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "venus",
            true, "🟡 Vénus — Magellan SAR & Hypsometric Model", 55655L,
            true, "🟡 Vénus — H2SO4 Virga Cycle Model", 56555L,
            true, "🟡 Vénus — Super-Rotation Low Variance Model", 57555L);

    /** Moon-like satellite settings */
    public static final PlanetPreset MOON_LIKE = new PlanetPreset(
            "Lune (Selene)", 3, 1737.4, 708.0, 1.5, 365.25, 1.0, 1.0, -9000.0, 10700.0, -20.0, 88888L, 0.9, 1.1, -0.5, 60.0, 0.0, 0.12, 0.0,
            true, 1.0, 384400.0, 0.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "moon",
            true, "⚪ Lune — LRO Diviner Thermal Radiometer", 88988L,
            true, "⚪ Lune — LRO LEND Vacuum Exosphere", 89888L,
            true, "⚪ Lune — Diurnal Insolation Amplitude Model", 90888L);

    /** Mercury settings */
    public static final PlanetPreset MERCURY_LIKE = new PlanetPreset(
            "Mercure (Hermes)", 3, 2439.7, 4222.6, 0.034, 87.97, 0.387, 1.0, -5000.0, 4480.0, 167.0, 66666L, 0.9, 1.0, -0.5, 90.0, 0.0, 0.14, 0.0,
            false, 1.0, 0.0, 0.0, 2.5, 1.5, null, null, null, null, null, null,
            true, "mercury",
            true, "⚪ Mercure — MESSENGER MLA Extreme Thermal Model", 66766L,
            true, "⚪ Mercure — MESSENGER Exospheric Vacuum Model", 67666L,
            true, "⚪ Mercure — 3:2 Spin-Orbit Thermal Variance Model", 68666L);

    /** Titan-like moon settings */
    public static final PlanetPreset TITAN_LIKE = new PlanetPreset(
            "Titan (Cryo-Lune)", 3, 2574.0, 382.0, 26.7, 10759.0, 9.5, 1.0, -2000.0, 5000.0, -179.0, 77711L, 0.8, 1.0, 0.2, 25.0, 0.0, 0.22, 1.45,
            true, 317.8, 1221870.0, 5000.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 77811L,
            false, "", 78711L,
            false, "", 79711L);

    /** Super-Earth settings */
    public static final PlanetPreset SUPER_EARTH = new PlanetPreset(
            "Super-Terre (Gaia Prime)", 3, 11000.0, 16.0, 12.0, 480.0, 1.0, 1.2, -14000.0, 12000.0, 22.0, 44444L, 1.3, 1.2, 0.1, 45.0, 25.0, 0.28, 1.5,
            false, 1.0, 0.0, 600.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 44544L,
            false, "", 45444L,
            false, "", 46444L);

    /** Tidally locked Eyeball world */
    public static final PlanetPreset EYEBALL_WORLD = new PlanetPreset(
            "Monde Synchrone (Eyeball)", 3, 5500.0, 720.0, 0.0, 30.0, 0.15, 0.05, -10000.0, 9000.0, 20.0, 33333L, 1.0, 1.0, 0.0, 90.0, 18.0, 0.35, 0.8,
            false, 1.0, 0.0, 1200.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 33433L,
            false, "", 34333L,
            false, "", 35333L);

    /** Water world settings */
    public static final PlanetPreset WATER_WORLD = new PlanetPreset(
            "Monde Océan (Oceania)", 3, 7000.0, 21.0, 18.0, 410.0, 1.0, 1.1, -12000.0, 3000.0, 25.0, 54321L, 0.8, 0.8, 0.35, 30.0, 23.0, 0.25, 1.2,
            false, 1.0, 0.0, 500.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 54421L,
            false, "", 55321L,
            false, "", 56321L);

    /** Ice world settings */
    public static final PlanetPreset ICE_WORLD = new PlanetPreset(
            "Monde Glaciaire (Boreas)", 3, 4800.0, 32.0, 45.0, 520.0, 2.5, 0.9, -6000.0, 7000.0, -45.0, 11111L, 0.5, 1.5, 0.1, 70.0, 15.0, 0.60, 0.7,
            false, 1.0, 0.0, 300.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 11211L,
            false, "", 12111L,
            false, "", 13111L);

    public static final PlanetPreset ARCHIPELAGO = new PlanetPreset(
            "Archipel", 3, 6371.0, 24.0, 23.5, 365.0, 1.0, 1.0, -11000.0, 8848.0, 18.0, 77777L, 1.5, 1.5, 0.6, 45.0, 21.0, 0.30, 1.0,
            false, 1.0, 0.0, 420.0, 2.5, 1.5, null, null, null, null, null, null,
            false, "none",
            false, "", 77877L,
            false, "", 78777L,
            false, "", 79777L);

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

    public long getAssociatedEpochYear() {
        String lower = name != null ? name.toLowerCase() : "";
        if (lower.contains("-100") || lower.contains("lig") || lower.contains("interglaciaire") || lower.contains("eemian")) return -100000L;
        if (lower.contains("-50") || lower.contains("sahul") || lower.contains("mis3") || lower.contains("mis 3")) return -50000L;
        if (lower.contains("-25") || lower.contains("beringia") || lower.contains("béringie")) return -25000L;
        if (lower.contains("-20") || lower.contains("lgm") || lower.contains("glaciaire")) return -20000L;
        if (lower.contains("-10") || lower.contains("eh") || lower.contains("précoce") || lower.contains("early holocene")) return -10000L;
        if (lower.contains("-6") || lower.contains("mh") || lower.contains("sahara") || lower.contains("mid holocene")) return -6000L;
        if (lower.contains("-3") || lower.contains("lh") || lower.contains("tardif") || lower.contains("late holocene")) return -3000L;
        if (lower.contains("-1900") || lower.contains("bronze")) return -1900L;
        if (lower.contains("-1000") || lower.contains("iron") || lower.contains("fer")) return -1000L;
        return 2026L;
    }

    public String getCanonicalPlanet() {
        String lower = name != null ? name.toLowerCase() : "";
        if (lower.contains("super-terre") || lower.contains("super-earth") || lower.contains("gaia")) return "none";
        if (lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) return "earth";
        if (lower.contains("mars") || lower.contains("ares")) return "mars";
        if (lower.contains("vénus") || lower.contains("venus") || lower.contains("hesperos")) return "venus";
        if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene")) return "moon";
        if (lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes")) return "mercury";
        return "none";
    }

    public boolean isTidalLocked() {
        return name != null && (name.contains("Eyeball") || name.contains("Synchrone") || name.toLowerCase().contains("tidally locked"));
    }

    public long cellCount() {
        return 2 + 120 * (long) Math.pow(7, resolution);
    }

    @Override
    public String toString() {
        return name;
    }
}

