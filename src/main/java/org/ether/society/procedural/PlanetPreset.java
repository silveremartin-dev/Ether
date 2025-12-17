package org.ether.society.procedural;

import java.util.List;

/**
 * Configuration preset for procedural planet generation.
 * 
 * @param name                Name of the preset (e.g., "Earth-Like")
 * @param resolution          H3 resolution (e.g., 6, 7, 8)
 * @param seed                Random seed
 * @param noiseFrequency      Base frequency of the noise (terrain detail)
 * @param noiseScale          Vertical scale of the noise (elevation range)
 * @param waterLevel          Elevation threshold for ocean (0.0 to 1.0)
 * @param temperatureGradient Temperature difference between equator and poles
 */
public record PlanetPreset(
                String name,
                int resolution,
                long seed,
                double noiseFrequency,
                double noiseScale,
                double waterLevel,
                double temperatureGradient) {

        /** Default Earth-like settings */
        public static final PlanetPreset EARTH_LIKE = new PlanetPreset(
                        "Terran", 6, 12345L, 1.0, 1.0, 0.0, 40.0); // Reduced resolution for performance

        /** Water world settings */
        public static final PlanetPreset WATER_WORLD = new PlanetPreset(
                        "Oceanic", 6, 54321L, 0.8, 0.8, 0.3, 30.0);

        /** Desert world settings */
        public static final PlanetPreset DESERT_WORLD = new PlanetPreset(
                        "Arid", 6, 99999L, 1.2, 1.2, -0.2, 50.0);

        /** Ice world settings */
        public static final PlanetPreset ICE_WORLD = new PlanetPreset(
                        "Glacial", 6, 11111L, 0.5, 1.5, 0.1, 80.0);

        public static final PlanetPreset ARCHIPELAGO = new PlanetPreset(
                        "Archipelago", 6, 77777L, 1.5, 1.5, 0.7, 45.0);

        public static List<PlanetPreset> getPresets() {
                return List.of(EARTH_LIKE, WATER_WORLD, DESERT_WORLD, ICE_WORLD, ARCHIPELAGO);
        }

        public long cellCount() {
                // Rough approximation for display
                return 2 + 120 * (long) Math.pow(7, resolution);
        }
}
