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
        double co2Ppm) {

    /** Overloaded constructor for 19-param compatibility */
    public PlanetPreset(String name, int resolution, double radiusKm, double dayLengthHours, double axialTiltDegrees,
            double yearLengthDays, double distanceToSunAU, double solarLuminosity, double minAltitudeMeters,
            double maxAltitudeMeters, double averageTempC, long seed, double noiseFrequency, double noiseScale,
            double waterLevel, double temperatureGradient, double oxygenPercentage, double albedo, double atmospherePressureAtm) {
        this(name, resolution, radiusKm, dayLengthHours, axialTiltDegrees, yearLengthDays, distanceToSunAU, solarLuminosity,
                minAltitudeMeters, maxAltitudeMeters, averageTempC, seed, noiseFrequency, noiseScale, waterLevel,
                temperatureGradient, oxygenPercentage, albedo, atmospherePressureAtm, false, 1.0, 384400.0, 420.0);
    }

    /** Overloaded constructor for 16-param compatibility */
    public PlanetPreset(String name, int resolution, double radiusKm, double dayLengthHours, double axialTiltDegrees,
            double yearLengthDays, double distanceToSunAU, double solarLuminosity, double minAltitudeMeters,
            double maxAltitudeMeters, double averageTempC, long seed, double noiseFrequency, double noiseScale,
            double waterLevel, double temperatureGradient) {
        this(name, resolution, radiusKm, dayLengthHours, axialTiltDegrees, yearLengthDays, distanceToSunAU, solarLuminosity,
                minAltitudeMeters, maxAltitudeMeters, averageTempC, seed, noiseFrequency, noiseScale, waterLevel,
                temperatureGradient, 21.0, 0.30, 1.0, false, 1.0, 384400.0, 420.0);
    }

    /** Overloaded constructor for 12-param compatibility */
    public PlanetPreset(String name, int resolution, double radiusKm, double dayLengthHours, double axialTiltDegrees,
            double yearLengthDays, double averageTempC, long seed, double noiseFrequency, double noiseScale,
            double waterLevel, double temperatureGradient) {
        this(name, resolution, radiusKm, dayLengthHours, axialTiltDegrees, yearLengthDays, 1.0, 1.0, -11000.0, 8848.0,
                averageTempC, seed, noiseFrequency, noiseScale, waterLevel, temperatureGradient, 21.0, 0.30, 1.0, false, 1.0, 384400.0, 420.0);
    }

    /** Overloaded constructor for 7-param compatibility */
    public PlanetPreset(String name, int resolution, long seed, double noiseFrequency, double noiseScale,
            double waterLevel, double temperatureGradient) {
        this(name, resolution, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, seed, noiseFrequency,
                noiseScale, waterLevel, temperatureGradient, 21.0, 0.30, 1.0, false, 1.0, 384400.0, 420.0);
    }

    /** Default Terran / Earth-like settings */
    public static final PlanetPreset EARTH_LIKE = new PlanetPreset(
            "Terre (Terran)", 6, 6371.0, 24.0, 23.5, 365.25, 1.0, 1.0, -11000.0, 8848.0, 15.0, 12345L, 1.0, 1.0, 0.0, 40.0, 21.0, 0.30, 1.0, false, 1.0, 0.0, 420.0);

    /** Mars-like settings */
    public static final PlanetPreset MARS_LIKE = new PlanetPreset(
            "Mars (Ares)", 6, 3389.0, 24.6, 25.2, 687.0, 1.52, 1.0, -8000.0, 21229.0, -60.0, 98765L, 1.2, 1.2, -0.4, 50.0, 0.13, 0.25, 0.006, false, 1.0, 0.0, 950000.0);

    public static final PlanetPreset DESERT_WORLD = MARS_LIKE;

    /** Venusian settings */
    public static final PlanetPreset VENUS_LIKE = new PlanetPreset(
            "Vénus (Hesperos)", 6, 6051.0, 2802.0, 177.3, 224.7, 0.72, 1.0, -3000.0, 11000.0, 464.0, 55555L, 0.6, 0.7, -0.5, 20.0, 0.0, 0.75, 92.0, false, 1.0, 0.0, 965000.0);

    /** Titan-like moon settings */
    public static final PlanetPreset TITAN_LIKE = new PlanetPreset(
            "Titan (Cryo-Lune)", 6, 2574.0, 382.0, 26.7, 10759.0, 9.5, 1.0, -2000.0, 5000.0, -179.0, 77711L, 0.8, 1.0, 0.2, 25.0, 0.0, 0.22, 1.45, true, 317.8, 1221870.0, 5000.0);

    /** Moon-like satellite settings */
    public static final PlanetPreset MOON_LIKE = new PlanetPreset(
            "Lune (Selene)", 6, 1737.0, 708.0, 1.5, 365.25, 1.0, 1.0, -9000.0, 10700.0, -20.0, 88888L, 0.9, 1.1, -0.5, 60.0, 0.0, 0.12, 0.0, true, 1.0, 384400.0, 0.0);

    /** Super-Earth settings */
    public static final PlanetPreset SUPER_EARTH = new PlanetPreset(
            "Super-Terre (Gaia Prime)", 7, 11000.0, 16.0, 12.0, 480.0, 1.0, 1.2, -14000.0, 12000.0, 22.0, 44444L, 1.3, 1.2, 0.1, 45.0, 25.0, 0.28, 1.5, false, 1.0, 0.0, 600.0);

    /** Tidally locked Eyeball world */
    public static final PlanetPreset EYEBALL_WORLD = new PlanetPreset(
            "Monde Synchrone (Eyeball)", 6, 5500.0, 720.0, 0.0, 30.0, 0.15, 0.05, -10000.0, 9000.0, 20.0, 33333L, 1.0, 1.0, 0.0, 90.0, 18.0, 0.35, 0.8, false, 1.0, 0.0, 1200.0);

    /** Water world settings */
    public static final PlanetPreset WATER_WORLD = new PlanetPreset(
            "Monde Océan (Oceania)", 6, 7000.0, 21.0, 18.0, 410.0, 1.0, 1.1, -12000.0, 3000.0, 25.0, 54321L, 0.8, 0.8, 0.35, 30.0, 23.0, 0.25, 1.2, false, 1.0, 0.0, 500.0);

    /** Ice world settings */
    public static final PlanetPreset ICE_WORLD = new PlanetPreset(
            "Monde Glaciaire (Boreas)", 6, 4800.0, 32.0, 45.0, 520.0, 2.5, 0.9, -6000.0, 7000.0, -45.0, 11111L, 0.5, 1.5, 0.1, 70.0, 15.0, 0.60, 0.7, false, 1.0, 0.0, 300.0);

    public static final PlanetPreset ARCHIPELAGO = new PlanetPreset(
            "Archipel", 6, 6371.0, 24.0, 23.5, 365.0, 1.0, 1.0, -11000.0, 8848.0, 18.0, 77777L, 1.5, 1.5, 0.6, 45.0, 21.0, 0.30, 1.0, false, 1.0, 0.0, 420.0);

    public static List<PlanetPreset> getPresets() {
        return List.of(EARTH_LIKE, MARS_LIKE, VENUS_LIKE, MOON_LIKE, TITAN_LIKE, SUPER_EARTH, EYEBALL_WORLD, WATER_WORLD, ICE_WORLD, ARCHIPELAGO);
    }

    public long cellCount() {
        return 2 + 120 * (long) Math.pow(7, resolution);
    }

    @Override
    public String toString() {
        return name;
    }
}

