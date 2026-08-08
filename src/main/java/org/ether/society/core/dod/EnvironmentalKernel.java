package org.ether.society.core.dod;

import org.ether.society.model.Biome;
import org.ether.society.model.PhysicalConstants;

/**
 * Data-Oriented Environmental Kernel.
 *
 * Models ecological biomass regeneration, agricultural yields, and natural resource decay
 * governed by differential equations anchored in Arrhenius thermal reaction kinetics and
 * physical environmental constants.
 *
 * <h2>Differential State Equations</h2>
 * <pre>
 *   d(Food_i)/dt = Prod_biome * f_Arrhenius(T_i) * f_Hydrological(R_i) - decay_rate * Food_i
 *   d(BiomassNatural_i)/dt = 0.5 * Growth_i
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 3.0.0
 */
public class EnvironmentalKernel {

    /**
     * Executes one environmental update tick over all active land cells in WorldBuffer.
     *
     * @param world DOD buffer container for world state
     * @param month Current simulation month
     * @param dt Time delta in seconds or days
     */
    public void tick(WorldBuffer world, int month, float dt) {
        float[] food = world.getFoodResource();
        float[] temp = world.getTemperature();
        float[] rain = world.getRainfall();
        byte[] biomes = world.getBiomes();
        
        int[] landIndices = world.getLandIndices();
        for (int idx = 0; idx < landIndices.length; idx++) {
            int i = landIndices[idx];
            Biome biome = Biome.values()[biomes[i]];
            
            // Primary production based on biome, Arrhenius thermal kinetics, and moisture availability
            float baseProd = getBiomeProductionRate(biome);
            float tempFactor = calculateArrheniusThermalFactor(temp[i]);
            float rainFactor = calculateRainfallFactor(rain[i]);
            
            float dtInYears = (float) Math.max(0.0001, dt > 1000.0f
                    ? (dt / PhysicalConstants.SECONDS_PER_JULIAN_YEAR)
                    : (dt / 365.25));
            
            float growth = baseProd * tempFactor * rainFactor * dtInYears;
            float decay = (float) (food[i] * 0.05 * dtInYears);
            
            food[i] = Math.max(0.0f, Math.min(1000.0f, food[i] + growth - decay));
            
            // Natural biomass regeneration
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

    /**
     * Calculates thermal reaction efficiency using the Arrhenius Enzymatic Reaction Kinetics equation:
     * <pre>
     *   k(T) = exp(-E_a / (R * T_kelvin)) / exp(-E_a / (R * T_opt))
     * </pre>
     */
    private float calculateArrheniusThermalFactor(float tempCelsius) {
        double tempK = tempCelsius + PhysicalConstants.KELVIN_ZERO_CELSIUS;
        if (tempK <= 235.0) return 0.05f; // Cryogenic freezing limit (-38°C)

        double eaOverR = PhysicalConstants.ENZYMATIC_ACTIVATION_ENERGY_J / PhysicalConstants.R_GAS_CONSTANT;
        double optK = PhysicalConstants.OPTIMAL_BIOLOGICAL_TEMP_KELVIN; // 298.15 K (25°C)

        double arrheniusRate = Math.exp(-eaOverR / tempK) / Math.exp(-eaOverR / optK);

        // Thermal denaturation factor above 35°C (308.15 K)
        if (tempK > 308.15) {
            double denaturation = Math.exp(- (tempK - 308.15) * 0.1);
            arrheniusRate *= denaturation;
        }

        return (float) Math.clamp(arrheniusRate, 0.05, 1.0);
    }

    private float calculateRainfallFactor(float rainMmPerYr) {
        if (rainMmPerYr < 200.0f) return (float) Math.max(0.1, rainMmPerYr / 200.0f * 0.5);
        if (rainMmPerYr <= 1500.0f) return 1.0f;
        return (float) Math.max(0.5, 1.0 - (rainMmPerYr - 1500.0f) / 3000.0f * 0.5);
    }
}
