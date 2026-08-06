package org.ether.society.core.dod;

import org.ether.society.model.Biome;

/**
 * Kernel gérant l'environnement (croissance de la nourriture, impact climatique).
 */
public class EnvironmentalKernel {

    public void tick(WorldBuffer world, int month, float dt) {
        float[] food = world.getFoodResource();
        float[] temp = world.getTemperature();
        float[] rain = world.getRainfall();
        byte[] biomes = world.getBiomes();
        
        int[] landIndices = world.getLandIndices();
        for (int idx = 0; idx < landIndices.length; idx++) {
            int i = landIndices[idx];
            Biome biome = Biome.values()[biomes[i]];
            
            // Production de nourriture basée sur le biome, la température et la pluie
            float baseProd = getBiomeProductionRate(biome);
            float tempFactor = calculateTemperatureFactor(temp[i]);
            float rainFactor = calculateRainfallFactor(rain[i]);
            
            float growth = baseProd * tempFactor * rainFactor * 0.1f * dt;
            float decay = food[i] * 0.05f * dt;
            
            food[i] = Math.max(0, Math.min(1000.0f, food[i] + growth - decay));
            
            // Régénération naturelle de la biomasse
            world.getBiomassNatural()[i] = Math.min(1000.0f, world.getBiomassNatural()[i] + growth * 0.5f);
        }
    }

    private float getBiomeProductionRate(Biome biome) {
        return switch (biome) {
            case JUNGLE -> 100.0f;
            case FOREST -> 80.0f;
            case PLAINS -> 60.0f;
            case HILLS -> 40.0f;
            case BEACH -> 30.0f;
            case MOUNTAINS -> 20.0f;
            case OCEAN, DEEP_OCEAN -> 50.0f;
            default -> 10.0f;
        };
    }

    private float calculateTemperatureFactor(float temp) {
        if (temp < 0) return 0.2f;
        if (temp < 15) return 0.7f;
        if (temp <= 25) return 1.0f;
        if (temp <= 35) return 0.6f;
        return 0.1f;
    }

    private float calculateRainfallFactor(float rain) {
        if (rain < 200) return 0.3f;
        if (rain <= 1500) return 1.0f;
        return 0.7f;
    }
}
