package org.ether.society.util;

import org.ether.society.model.Biome;
import org.ether.society.model.Cell;
import org.ether.society.model.World;



public class TerrainGenerator {

    private final long seed;

    public TerrainGenerator(long seed) {
        this.seed = seed;
        // this.random = new Random(seed);
    }

    public void generate(World world) {
        int width = world.getWidth();
        int height = world.getHeight();
        Cell[][] grid = new Cell[width][height];

        // Simple noise simulation for now (placeholder for Perlin)
        // In a real implementation, we'd use a proper Perlin/Simplex noise library

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Cell cell = new Cell(x, y);

                // Generate Elevation (-1.0 to 1.0)
                double elevation = generateNoise(x, y, 0.1);
                cell.setElevation(elevation);

                // Generate Rainfall (0.0 to 1.0)
                double rainfall = (generateNoise(x + 1000, y + 1000, 0.1) + 1.0) / 2.0;
                cell.setRainfall(rainfall);

                // Determine Biome
                cell.setBiome(determineBiome(elevation, rainfall));

                // Base Temperature based on latitude (y) and elevation
                double latitudeFactor = 1.0 - (double) Math.abs(y - height / 2) / (height / 2); // 1.0 at equator, 0.0
                                                                                                // at poles
                double baseTemp = (latitudeFactor * 40.0) - 10.0; // -10 to 30 degrees C
                baseTemp -= elevation * 10.0; // Colder at higher altitudes
                cell.setTemperature(baseTemp);

                // Add Resources based on biome
                populateResources(cell);

                grid[x][y] = cell;
            }
        }

        world.setGrid(grid);
    }

    private double generateNoise(int x, int y, double scale) {
        // Simple seeded noise
        double noise = Math.sin((x + seed) * scale) * Math.cos((y + seed) * scale);
        return noise;
    }

    private Biome determineBiome(double elevation, double rainfall) {
        if (elevation < -0.2)
            return Biome.OCEAN;
        if (elevation < 0.0)
            return Biome.BEACH;

        if (elevation > 0.8) {
            if (rainfall > 0.5)
                return Biome.SNOW;
            return Biome.MOUNTAINS;
        }

        if (rainfall < 0.2)
            return Biome.DESERT;
        if (rainfall < 0.5)
            return Biome.PLAINS;
        if (rainfall < 0.8)
            return Biome.FOREST;

        return Biome.JUNGLE;
    }

    private void populateResources(Cell cell) {
        Biome biome = cell.getBiome();
        if (biome == Biome.FOREST || biome == Biome.JUNGLE) {
            cell.addResource("Wood", 1000.0);
        }
        if (biome == Biome.PLAINS || biome == Biome.FOREST) {
            cell.addResource("Food", 500.0); // Wild food
        }
        if (biome == Biome.MOUNTAINS) {
            cell.addResource("Stone", 1000.0);
            cell.addResource("Metal", 500.0);
        }
        if (biome == Biome.OCEAN) {
            cell.addResource("Fish", 1000.0);
        }
    }
}
