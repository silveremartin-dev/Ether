package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service for generating procedural planets using Simplex Noise on a sphere.
 */
public class ProceduralGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ProceduralGenerator.class);

    /**
     * Data record for a single point on the planet.
     */
    public record PlanetPoint(double elevation, double temperature, double rainfall, Biome biome, double declivity, double riverFlow, double accessibleAquifer) {
        public PlanetPoint(double elevation, double temperature, double rainfall, Biome biome, double declivity, double riverFlow) {
            this(elevation, temperature, rainfall, biome, declivity, riverFlow, 0.0);
        }
        public PlanetPoint(double elevation, double temperature, double rainfall, Biome biome) {
            this(elevation, temperature, rainfall, biome, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Calculate terrain data for a specific latitude/longitude.
     * Useful for UI previews without generating full H3 grid.
     */
    public PlanetPoint getPlanetPoint(double lat, double lng, PlanetPreset preset) {
        SimplexNoise elevationNoise = new SimplexNoise(preset.seed());
        SimplexNoise rainfallNoise = new SimplexNoise(preset.seed() + 1000); // Offset

        double freq = preset.noiseFrequency();
        double scale = preset.noiseScale();

        // --- Elevation ---
        double e = computeElevationAt(lat, lng, elevationNoise, freq, scale);

        // --- Declivity (Slope Gradient Magnitude) ---
        double delta = 0.3; // Degree step for finite differences
        double eNorth = computeElevationAt(lat + delta, lng, elevationNoise, freq, scale);
        double eSouth = computeElevationAt(lat - delta, lng, elevationNoise, freq, scale);
        double eEast  = computeElevationAt(lat, lng + delta, elevationNoise, freq, scale);
        double eWest  = computeElevationAt(lat, lng - delta, elevationNoise, freq, scale);

        double dLat = (eNorth - eSouth) / (2.0 * delta);
        double dLng = (eEast - eWest) / (2.0 * delta);
        double declivity = Math.sqrt(dLat * dLat + dLng * dLng);

        // --- Rainfall ---
        double r = computeRainfallAt(lat, lng, rainfallNoise, freq);

        // --- Temperature ---
        double baseTemp = preset.averageTempC();
        double latFactor = Math.cos(Math.toRadians(lat));
        double temp = baseTemp + preset.temperatureGradient() * (latFactor - 0.5);
        if (e > 0)
            temp -= e * 20.0; // Altitude lapse rate

        Biome biome = determineBiome(e, temp, r, preset.waterLevel());

        // --- River Flow / Watercourses (Fleuves et Cours d'Eau) ---
        // Water collects in valleys (where e < average of surrounding points) and drains down declivity slopes
        double avgSurrounding = (eNorth + eSouth + eEast + eWest) / 4.0;
        double valleyDepth = Math.max(0.0, avgSurrounding - e);

        double riverFlow = 0.0;
        double accessibleAquifer = 0.0;
        if (e > preset.waterLevel()) {
            riverFlow = r * (0.3 + 2.5 * declivity + 18.0 * valleyDepth);
            riverFlow = Math.min(1.0, Math.max(0.0, riverFlow));

            // --- Accessible Aquifer / Nappe Phréatique Accessible ---
            // High in flat basins/plains (low declivity), river valleys & rainfall recharge zones
            double flatBonus = Math.max(0.1, 1.0 - 2.5 * declivity);
            accessibleAquifer = r * flatBonus * (0.3 + 1.5 * valleyDepth) + 0.35 * riverFlow;
            accessibleAquifer = Math.min(1.0, Math.max(0.0, accessibleAquifer));
        }

        return new PlanetPoint(e, temp, r, biome, declivity, riverFlow, accessibleAquifer);
    }

    private double computeElevationAt(double lat, double lng, SimplexNoise elevationNoise, double freq, double scale) {
        double latRad = Math.toRadians(lat);
        double lngRad = Math.toRadians(lng);

        double x = Math.cos(latRad) * Math.cos(lngRad);
        double y = Math.cos(latRad) * Math.sin(lngRad);
        double z = Math.sin(latRad);

        double e = 0;
        e += (1.0 / 1.0) * elevationNoise.noise(1.0 * freq * x, 1.0 * freq * y, 1.0 * freq * z);
        e += (1.0 / 2.0) * elevationNoise.noise(2.0 * freq * x, 2.0 * freq * y, 2.0 * freq * z);
        e += (1.0 / 4.0) * elevationNoise.noise(4.0 * freq * x, 4.0 * freq * y, 4.0 * freq * z);

        e = e / 1.75;
        e = e * scale;
        return Math.max(-1.0, Math.min(1.0, e));
    }

    private double computeRainfallAt(double lat, double lng, SimplexNoise rainfallNoise, double freq) {
        double latRad = Math.toRadians(lat);
        double lngRad = Math.toRadians(lng);

        double x = Math.cos(latRad) * Math.cos(lngRad);
        double y = Math.cos(latRad) * Math.sin(lngRad);
        double z = Math.sin(latRad);

        double r = 0;
        r += rainfallNoise.noise(1.5 * freq * x, 1.5 * freq * y, 1.5 * freq * z);
        r = (r + 1.0) / 2.0;

        double absLat = Math.abs(lat);
        double latMod = 1.0;
        if (absLat < 10)
            latMod = 1.2;
        else if (absLat > 20 && absLat < 40)
            latMod = 0.4;
        else if (absLat > 50 && absLat < 70)
            latMod = 0.8;
        else if (absLat > 80)
            latMod = 0.2;

        r = r * 0.7 + latMod * 0.3;
        return Math.max(0.0, Math.min(1.0, r));
    }

    public List<H3Cell> generatePlanet(PlanetPreset preset) {
        logger.info("Generating planet: {} (Res: {}, Seed: {})", preset.name(), preset.resolution(), preset.seed());

        // 1. Generate Base Grid
        List<H3Cell> cells = H3Service.getInstance().generateGlobalMetadata(preset.resolution());
        logger.info("Base grid generated with {} cells", cells.size());

        // 2. Process each cell using shared logic
        cells.parallelStream().forEach(cell -> {
            PlanetPoint p = getPlanetPoint(cell.getLatitude(), cell.getLongitude(), preset);

            cell.setElevation(p.elevation());
            cell.setTemperature(p.temperature());
            cell.setRainfall(p.rainfall());
            cell.setBiome(p.biome());

            populateResources(cell);
        });

        return cells;
    }

    private Biome determineBiome(double elevation, double temp, double rain, double waterLevel) {
        if (elevation < waterLevel) {
            return (elevation < waterLevel - 0.5) ? Biome.DEEP_OCEAN : Biome.OCEAN;
        }

        // Land
        if (elevation > 0.8)
            return Biome.MOUNTAINS; // High peaks
        if (elevation > 0.5)
            return Biome.HILLS;

        if (temp < -5)
            return Biome.SNOW;
        if (temp < 5)
            return Biome.TUNDRA;

        if (rain < 0.2)
            return Biome.DESERT;
        if (rain < 0.5)
            return Biome.PLAINS;
        if (rain < 0.8)
            return Biome.FOREST;

        return Biome.JUNGLE;
    }

    private void populateResources(H3Cell cell) {
        Biome b = cell.getBiome();
        if (b == null)
            return;

        switch (b) {
            case FOREST, JUNGLE -> cell.setWoodResource(1000.0);
            case PLAINS -> cell.setFoodResource(500.0);
            case MOUNTAINS -> {
                cell.setResourceMetal(500.0);
                // No stone field, assuming implicit or part of capital/construction potential
            }
            case OCEAN, DEEP_OCEAN -> {
                cell.setBiomassFish(800.0);
                cell.setFoodResource(200.0); // Accessible food
            }
            default -> {
            }
        }
    }
}
