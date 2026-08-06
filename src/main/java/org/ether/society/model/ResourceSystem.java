package org.ether.society.model;

/**
 * Manages resource regeneration across the world based on biome and climate.
 */
public class ResourceSystem {

    public void update(World world) {
        int width = world.getWidth();
        int height = world.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Cell cell = world.getCell(x, y);
                if (cell == null)
                    continue;

                regenerateResources(cell);
            }
        }
    }

    private void regenerateResources(Cell cell) {
        Biome biome = cell.getBiome();
        double temperature = cell.getTemperature();
        double rainfall = cell.getRainfall();

        // Food regeneration based on biome
        double foodRegen = 0.0;
        double waterRegen = 0.0;
        double woodRegen = 0.0;

        switch (biome) {
            case OCEAN:
            case DEEP_OCEAN:
                foodRegen = 2.0; // Fish
                waterRegen = 0.0; // Salt water
                break;
            case BEACH:
                foodRegen = 0.5;
                waterRegen = 0.2;
                break;
            case PLAINS:
                foodRegen = 3.0; // Grass, berries
                waterRegen = rainfall * 2.0;
                woodRegen = 0.5;
                break;
            case SAVANNAH:
                foodRegen = 4.5; // Abundant game & wild cereals
                waterRegen = rainfall * 2.5;
                woodRegen = 1.0;
                break;
            case FOREST:
                foodRegen = 4.0; // Abundant wildlife and plants
                waterRegen = rainfall * 3.0;
                woodRegen = 2.0;
                break;
            case JUNGLE:
                foodRegen = 5.0; // Very abundant
                waterRegen = rainfall * 4.0;
                woodRegen = 3.0;
                break;
            case DESERT:
                foodRegen = 0.2;
                waterRegen = rainfall * 0.5;
                break;
            case HILLS:
                foodRegen = 2.0;
                waterRegen = rainfall * 1.5;
                woodRegen = 1.0;
                break;
            case MOUNTAINS:
                foodRegen = 1.0;
                waterRegen = rainfall * 1.0;
                woodRegen = 0.5;
                break;
            case SNOW:
                foodRegen = 0.1;
                waterRegen = 0.5; // Snow melt
                break;
            case GLACIER:
                foodRegen = 0.05;
                waterRegen = 0.2;
                break;
            case TUNDRA:
                foodRegen = 0.5;
                waterRegen = 1.0;
                break;
        }

        // Temperature affects food production
        if (temperature < -10) {
            foodRegen *= 0.2; // Very cold
        } else if (temperature < 0) {
            foodRegen *= 0.5; // Cold
        } else if (temperature > 40) {
            foodRegen *= 0.5; // Too hot
        }

        // Add resources with caps to prevent infinite accumulation
        double maxFood = 100.0;
        double maxWater = 100.0;
        double maxWood = 50.0;

        double currentFood = cell.getResource("Food");
        double currentWater = cell.getResource("Water");
        double currentWood = cell.getResource("Wood");

        if (currentFood < maxFood) {
            cell.addResource("Food", Math.min(foodRegen, maxFood - currentFood));
        }
        if (currentWater < maxWater) {
            cell.addResource("Water", Math.min(waterRegen, maxWater - currentWater));
        }
        if (currentWood < maxWood) {
            cell.addResource("Wood", Math.min(woodRegen, maxWood - currentWood));
        }
    }
}
