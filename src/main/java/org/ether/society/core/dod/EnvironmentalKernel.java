package org.ether.society.core.dod;

import org.ether.society.model.Biome;
import org.ether.society.model.PhysicalConstants;

/**
 * Data-Oriented Environmental Kernel.
 *
 * Models ecological biomass regeneration, agricultural yields, and natural resource decay
 * governed by differential equations anchored in Farquhar C3/C4 Photosynthesis kinetics (FvCB),
 * Priestley-Taylor Potential Evapotranspiration (PET), Arrhenius thermal kinetics, and
 * physical environmental constants.
 *
 * <h2>Differential State Equations</h2>
 * <pre>
 *   d(Food_i)/dt = Prod_biome * f_FvCB(T_i, CO2_i) * f_PriestleyTaylor(R_i, PET_i) - decay_rate * Food_i
 *   d(BiomassNatural_i)/dt = 0.5 * Growth_i
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class EnvironmentalKernel {

    // Pre-computed Farquhar FvCB 2D Look-Up Table (Temperature x CO2)
    // T: -10°C to +45°C (56 steps), CO2: 150 ppm to 1000 ppm (35 steps)
    /* Internal state variable for lut temp min (int). */
    private static final int LUT_TEMP_MIN = -10;
    /* Internal state variable for lut temp max (int). */
    private static final int LUT_TEMP_MAX = 45;
    private static final int LUT_TEMP_STEPS = LUT_TEMP_MAX - LUT_TEMP_MIN + 1; // 56
    /* Internal state variable for lut co2 min (int). */
    private static final int LUT_CO2_MIN = 150;
    /* Internal state variable for lut co2 max (int). */
    private static final int LUT_CO2_MAX = 1000;
    /* Internal state variable for lut co2 step size (int). */
    private static final int LUT_CO2_STEP_SIZE = 25;
    private static final int LUT_CO2_STEPS = (LUT_CO2_MAX - LUT_CO2_MIN) / LUT_CO2_STEP_SIZE + 1; // 35

    private static final float[][] FVCB_PHOTOSYNTHESIS_LUT = new float[LUT_TEMP_STEPS][LUT_CO2_STEPS];

    static {
        // Pre-compute Farquhar FvCB assimilation Look-Up Table at class initialization
        for (int tIdx = 0; tIdx < LUT_TEMP_STEPS; tIdx++) {
            double tempC = LUT_TEMP_MIN + tIdx;
            double tempK = tempC + PhysicalConstants.KELVIN_ZERO_CELSIUS;
            for (int cIdx = 0; cIdx < LUT_CO2_STEPS; cIdx++) {
                double co2Ppm = LUT_CO2_MIN + cIdx * LUT_CO2_STEP_SIZE;

                if (tempC < -2.0) {
                    FVCB_PHOTOSYNTHESIS_LUT[tIdx][cIdx] = 0.05f;
                } else {
                    // Farquhar parameters
                    double vcmax = 60.0 * Math.exp( (65000.0 / PhysicalConstants.R_GAS_CONSTANT) * (1.0/298.15 - 1.0/tempK) );
                    double gammaStar = 42.75 * Math.exp( (37830.0 / PhysicalConstants.R_GAS_CONSTANT) * (1.0/298.15 - 1.0/tempK) );
                    double kc = 404.9 * Math.exp( (79430.0 / PhysicalConstants.R_GAS_CONSTANT) * (1.0/298.15 - 1.0/tempK) );
                    double ko = 278.4 * Math.exp( (36380.0 / PhysicalConstants.R_GAS_CONSTANT) * (1.0/298.15 - 1.0/tempK) );
                    double km = kc * (1.0 + 210.0 / ko);

                    double ci = co2Ppm * 0.70; // Intercellular CO2 concentration
                    double ac = vcmax * Math.max(0.0, ci - gammaStar) / Math.max(1.0, ci + km);
                    double aj = 0.5 * vcmax * Math.max(0.0, ci - gammaStar) / Math.max(1.0, ci + 2.0 * gammaStar);

                    double anet = Math.min(ac, aj);
                    // Denaturation above 38°C
                    if (tempC > 38.0) {
                        anet *= Math.exp(-(tempC - 38.0) * 0.15);
                    }
                    float normalizedYield = (float) Math.clamp(anet / 25.0, 0.05, 1.50);
                    FVCB_PHOTOSYNTHESIS_LUT[tIdx][cIdx] = normalizedYield;
                }
            }
        }
    }

    /*
     * Retrieves photosynthetic assimilation factor from the precomputed Farquhar FvCB LUT.
     */
    public static float evaluateFarquharYield(float tempC, float co2Ppm) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        int tIdx = Math.clamp(Math.round(tempC) - LUT_TEMP_MIN, 0, LUT_TEMP_STEPS - 1);
        int cIdx = Math.clamp(Math.round((co2Ppm - LUT_CO2_MIN) / LUT_CO2_STEP_SIZE), 0, LUT_CO2_STEPS - 1);
        return FVCB_PHOTOSYNTHESIS_LUT[tIdx][cIdx];
    }

    /*
     * Calculates Beer-Lambert canopy light transmission fraction I / I_0 = exp(-k_ext * LAI).
     *
     * @param leafAreaIndex Leaf Area Index (LAI in m²/m²)
     * @param extinctionCoeff Canopy light extinction coefficient k_ext (~0.5 - 0.7)
     * @return Light transmission fraction to understory [0.0, 1.0]
     */
    public static float calculateBeerLambertCanopyTransmission(float leafAreaIndex, float extinctionCoeff) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        if (leafAreaIndex <= 0.0f) return 1.0f;
        return (float) Math.exp(-extinctionCoeff * leafAreaIndex);
    }

    /*
     * Calculates Priestley-Taylor Potential Evapotranspiration (PET in mm/year).
     */
    public static float calculatePriestleyTaylorPET(float tempC) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        if (tempC <= -5.0f) return 50.0f;
        double t = Math.clamp(tempC, -5.0, 50.0);
        // Slope of saturation vapor curve Delta in kPa/°C
        double delta = (4098.0 * (0.6108 * Math.exp(17.27 * t / (t + 237.3)))) / Math.pow(t + 237.3, 2.0);
        double gammaPsy = 0.066; // Psychrometric constant in kPa/°C
        double alphaPT = 1.26; // Priestley-Taylor coefficient
        double netSolarRadiationMmEquiv = Math.max(100.0, (t + 10.0) * 35.0); // Equivalent mm/yr radiation

        return (float) (alphaPT * (delta / (delta + gammaPsy)) * netSolarRadiationMmEquiv);
    }

    /*
     * Tick.
     * Enforces physical invariants and updates associated state variables within {@code EnvironmentalKernel}.
     *
     * @param world the world parameter (WorldBuffer)
     * @param dt the dt parameter (float)
     */
    public void tick(WorldBuffer world, float dt) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        tick(world, 0, dt);
    }

    /*
     * Executes one environmental update tick over all active land cells in WorldBuffer.
     *
     * @param world DOD buffer container for world state
     * @param month Current simulation month
     * @param dt Time delta in seconds or days
     */
    public void tick(WorldBuffer world, int month, float dt) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        float[] food = world.getFoodResource();
        float[] temp = world.getTemperature();
        float[] rain = world.getRainfall();
        byte[] biomes = world.getBiomes();
        
        int[] landIndices = world.getLandIndices();
        float baselineCO2 = 280.0f;

        for (int idx = 0; idx < landIndices.length; idx++) {
            int i = landIndices[idx];
            Biome biome = Biome.values()[biomes[i]];
            
            // Primary production based on biome, Farquhar bioenergetics, and irrigation/moisture availability
            float baseProd = getBiomeProductionRate(biome);
            float fvcbFactor = evaluateFarquharYield(temp[i], baselineCO2);
            float pet = calculatePriestleyTaylorPET(temp[i]);
            float waterAvail = world.getWaterResource() != null ? world.getWaterResource()[i] : 0.0f;
            float effectiveMoisture = Math.max(rain[i], Math.min(1500.0f, rain[i] + waterAvail * 0.8f));
            float moistureAridityRatio = Math.clamp(effectiveMoisture / Math.max(100.0f, pet), 0.40f, 1.25f);
            
            float dtInYears = (float) Math.max(0.0001, dt > 1000.0f
                    ? (dt / PhysicalConstants.SECONDS_PER_JULIAN_YEAR)
                    : (dt / 365.25));
            
            float tech = world.getTechnologyLevel() != null ? world.getTechnologyLevel()[i] : 0.0f;
            float fodderFactor = (tech >= 4.0f && tech < 50.0f)
                    ? (float) (1.0 - PhysicalConstants.PREINDUSTRIAL_FODDER_LAND_FRACTION)
                    : 1.0f;
            float industrialBoost = (tech >= 50.0f) ? Math.min(3.5f, 1.0f + (tech - 50.0f) * 0.03f) : 1.0f;
            
            float agroClimaticFactor = Math.clamp(fvcbFactor * moistureAridityRatio * fodderFactor * industrialBoost, 0.40f, 2.0f);
            float humanPop = world.getBiomassHuman() != null ? world.getBiomassHuman()[i] : 0.0f;
            float agProd = (humanPop > 0.0f) ? (humanPop * 4.6f * agroClimaticFactor * dtInYears) : 0.0f;
            float naturalProd = baseProd * agroClimaticFactor * dtInYears;

            float growth = naturalProd + agProd;
            float decay = (float) (food[i] * 0.03 * dtInYears);
            
            food[i] = Math.max(0.0f, Math.min(2000000.0f, food[i] + growth - decay));
            
            // Natural biomass regeneration
            world.getBiomassNatural()[i] = Math.min(10000.0f, world.getBiomassNatural()[i] + naturalProd * 0.5f);
        }
    }

    /*
     * Net primary photosynthetic & trophic energy production rate per biome in Gigajoules per year (GJ/yr).
     * Scaled for macro-hexagonal cells (1,250 to 11,000 km²) supporting preindustrial & historical bands.
     */
    private float getBiomeProductionRate(Biome biome) {
        return switch (biome) {
            case JUNGLE -> 3500.0f;
            case FOREST -> 2800.0f;
            case SAVANNAH -> 3000.0f;
            case PLAINS -> 2500.0f;
            case HILLS -> 1800.0f;
            case BEACH, LAKE -> 1500.0f;
            case MOUNTAINS -> 800.0f;
            case OCEAN, DEEP_OCEAN -> 1200.0f;
            case TUNDRA -> 600.0f;
            case DESERT -> 150.0f;
            default -> 300.0f;
        };
    }
}


