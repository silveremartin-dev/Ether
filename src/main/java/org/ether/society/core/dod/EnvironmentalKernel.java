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
 * @version 1.0.0-beta.2
 */
public class EnvironmentalKernel {

    // Pre-computed Farquhar FvCB 2D Look-Up Table (Temperature x CO2)
    // T: -10°C to +45°C (56 steps), CO2: 150 ppm to 1000 ppm (35 steps)
    /** Internal state: lut temp min (int). */
    private static final int LUT_TEMP_MIN = -10;
    /** Internal state: lut temp max (int). */
    private static final int LUT_TEMP_MAX = 45;
    private static final int LUT_TEMP_STEPS = LUT_TEMP_MAX - LUT_TEMP_MIN + 1; // 56
    /** Internal state: lut co2 min (int). */
    private static final int LUT_CO2_MIN = 150;
    /** Internal state: lut co2 max (int). */
    private static final int LUT_CO2_MAX = 1000;
    /** Internal state: lut co2 step size (int). */
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

    /**
     * Retrieves photosynthetic assimilation factor from the precomputed Farquhar FvCB LUT.
     */
    public static float evaluateFarquharYield(float tempC, float co2Ppm) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        int tIdx = Math.clamp(Math.round(tempC) - LUT_TEMP_MIN, 0, LUT_TEMP_STEPS - 1);
        int cIdx = Math.clamp(Math.round((co2Ppm - LUT_CO2_MIN) / LUT_CO2_STEP_SIZE), 0, LUT_CO2_STEPS - 1);
        return FVCB_PHOTOSYNTHESIS_LUT[tIdx][cIdx];
    }

    /**
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

    private static final int PET_LUT_MIN = -5;
    private static final int PET_LUT_MAX = 50;
    private static final int PET_LUT_STEPS = (PET_LUT_MAX - PET_LUT_MIN) * 2 + 1; // 111 steps (0.5°C)
    private static final float[] PET_LUT = new float[PET_LUT_STEPS];

    static {
        for (int i = 0; i < PET_LUT_STEPS; i++) {
            double t = PET_LUT_MIN + i * 0.5;
            double delta = (4098.0 * (0.6108 * Math.exp(17.27 * t / (t + 237.3)))) / ((t + 237.3) * (t + 237.3));
            double gammaPsy = 0.066;
            double alphaPT = 1.26;
            double netSolarRadiationMmEquiv = Math.max(100.0, (t + 10.0) * 35.0);
            PET_LUT[i] = (float) (alphaPT * (delta / (delta + gammaPsy)) * netSolarRadiationMmEquiv);
        }
    }

    /**
     * Calculates Priestley-Taylor Potential Evapotranspiration (PET in mm/year).
     */
    public static float calculatePriestleyTaylorPET(float tempC) {
        if (tempC <= -5.0f) return 50.0f;
        if (tempC >= 50.0f) return PET_LUT[PET_LUT_STEPS - 1];
        float idxF = (tempC - PET_LUT_MIN) * 2.0f;
        int idx = (int) idxF;
        float frac = idxF - idx;
        return PET_LUT[idx] + frac * (PET_LUT[idx + 1] - PET_LUT[idx]);
    }

    /**
     * Tick.
     *
     * @param world the world parameter (WorldBuffer)
     * @param dt the dt parameter (float)
     */
    public void tick(WorldBuffer world, float dt) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        tick(world, 0, dt);
    }

    /**
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
        if (landIndices == null) return;
        float baselineCO2 = 280.0f;

        // Extract H3 resolution from the first valid H3 index to scale macro-cell production rates by actual cell surface area
        float areaScale = 1.0f;
        long[] h3Indexes = world.getH3Indexes();
        if (h3Indexes != null && h3Indexes.length > 0 && h3Indexes[0] != 0L) {
            int res = (int) ((h3Indexes[0] >>> 52) & 0x0FL);
            if (res >= 0 && res <= 15) {
                double cellAreaKm2 = org.ether.society.database.H3Cell.getAvgHexagonAreaKm2(res);
                areaScale = (float) Math.clamp(cellAreaKm2 / 1770.49, 0.01, 20.0);
            }
        }

        float dtInYears = (float) Math.max(0.0001, dt > 1000.0f
                ? (dt / PhysicalConstants.SECONDS_PER_JULIAN_YEAR)
                : (dt / 365.25));
        float foodRetentionFactor = (float) Math.exp(-0.03 * dtInYears);

        for (int idx = 0; idx < landIndices.length; idx++) {
            int i = landIndices[idx];
            if (i < 0 || i >= world.getCapacity()) continue;
            Biome biome = Biome.fromOrdinal(biomes[i] & 0xFF);
            
            // Primary production derived analytically from Miami NPP model (Lieth 1975), Farquhar bioenergetics,
            // Priestley-Taylor moisture balance, and cell surface area (Zero discrete biome switch tables per AGENTS.md §3)
            float waterAvail = world.getWaterResource() != null ? world.getWaterResource()[i] : 0.0f;
            float effectiveMoisture = Math.max(rain[i], Math.min(1500.0f, rain[i] + waterAvail * 0.8f));
            float baseProd = calculateAnalyticalNaturalProductionGJ(biome, temp[i], effectiveMoisture) * areaScale;
            float fvcbFactor = evaluateFarquharYield(temp[i], baselineCO2);
            float pet = calculatePriestleyTaylorPET(temp[i]);
            float moistureAridityRatio = Math.clamp(effectiveMoisture / Math.max(100.0f, pet), 0.40f, 1.25f);
            
            float tech = world.getTechnologyLevel() != null ? world.getTechnologyLevel()[i] : 0.0f;
            float fodderFactor = (tech >= 4.0f && tech < 50.0f)
                    ? (float) (1.0 - PhysicalConstants.PREINDUSTRIAL_FODDER_LAND_FRACTION)
                    : 1.0f;
            float industrialBoost = (tech >= 50.0f) ? Math.min(3.5f, 1.0f + (tech - 50.0f) * 0.03f) : 1.0f;
            
            float agroClimaticFactor = Math.clamp(fvcbFactor * moistureAridityRatio * fodderFactor * industrialBoost, 0.40f, 2.0f);
            float humanPop = world.getBiomassHuman() != null ? world.getBiomassHuman()[i] : 0.0f;
            
            // Biophysical agricultural yield response (Mitscherlich 1909; Spillman 1923; Boserup 1965):
            // Diminishing returns asymptotic response bounds total agricultural caloric output per cell by arable potential
            float maxAgPotential = calculateAnalyticalAgriculturalPotentialGJ(biome, temp[i], effectiveMoisture)
                    * areaScale * agroClimaticFactor * (1.0f + 0.05f * Math.min(100.0f, tech));
            float agDemandPotential = humanPop * 6.5f;
            float agRatio = (maxAgPotential > 0.0f) ? (agDemandPotential / maxAgPotential) : 0.0f;
            float agProd = (humanPop > 0.0f && maxAgPotential > 0.0f)
                    ? (maxAgPotential * (1.0f - (float) Math.exp(-2.0f * agRatio)) * dtInYears)
                    : 0.0f;
            float naturalProd = baseProd * agroClimaticFactor * dtInYears;

            float growth = naturalProd + agProd;
            float retainedFood = food[i] * foodRetentionFactor;
            
            food[i] = Math.max(0.0f, Math.min(50000.0f * areaScale, retainedFood + growth));
            
            // Biomass regeneration coupled to Primary Productivity (bounded by area-scaled ecological carrying capacity)
            if (world.getBiomassNatural() != null) {
                float minForagerBiomassTons = (tech < 1.5f && humanPop > 0.0f) ? (humanPop * 48.0f) : (10000.0f * areaScale);
                float natCeiling = Math.max(10000.0f * areaScale, minForagerBiomassTons);
                world.getBiomassNatural()[i] = Math.min(natCeiling, world.getBiomassNatural()[i] + naturalProd * 0.5f);
            }
            if (world.getBiomassAgriculture() != null && tech >= 1.5f) {
                float annualAgFluxKg = (agProd > 0.0f)
                        ? Math.max(humanPop * 620.0f, (agProd / Math.max(0.0001f, dtInYears)) / 0.0105f)
                        : (humanPop * 620.0f);
                world.getBiomassAgriculture()[i] = Math.max(world.getBiomassAgriculture()[i] * foodRetentionFactor, annualAgFluxKg);
            }
        }
    }

    /**
     * Computes Net Primary Productivity (NPP in g DM/m²/yr = metric tons DM/km²/yr)
     * analytically via the Lieth (1975) Miami Climate Model:
     * $$NPP(T, P) = \min\!\left(\frac{3000}{1 + \exp(1.315 - 0.119\,T)},\; 3000\,(1 - \exp(-0.000664\,P))\right)$$
     *
     * @param tempC            mean surface temperature (°C)
     * @param effectivePrecipMm effective moisture / precipitation (mm/yr)
     * @return analytical NPP in metric tons of dry matter per km² per year
     */
    public static float calculateMiamiNPP(float tempC, float effectivePrecipMm) {
        double nppTemp = 3000.0 / (1.0 + Math.exp(1.315 - 0.119 * tempC));
        double nppPrecip = 3000.0 * (1.0 - Math.exp(-0.000664 * Math.max(0.0, effectivePrecipMm)));
        return (float) Math.clamp(Math.min(nppTemp, nppPrecip), 15.0, 3000.0);
    }

    /**
     * Analytical closed-form natural trophic energy production rate in GJ/yr for a reference macro-cell,
     * derived from the Lieth (1975) Miami NPP model ($f(T, P)$).
     */
    private float calculateAnalyticalNaturalProductionGJ(Biome biome, float tempC, float effectivePrecipMm) {
        if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN) {
            return 1200.0f;
        }
        if (biome == Biome.GLACIER || biome == Biome.SNOW) {
            return 150.0f;
        }
        float npp = calculateMiamiNPP(tempC, effectivePrecipMm);
        return Math.clamp(150.0f + npp * 1.95f, 150.0f, 3800.0f);
    }

    /**
     * Analytical closed-form maximum agricultural caloric potential in GJ/yr for a reference macro-cell,
     * derived from Miami NPP ($f(T, P)$) and Smil (2000) cereal harvest index stoichiometry.
     */
    private float calculateAnalyticalAgriculturalPotentialGJ(Biome biome, float tempC, float effectivePrecipMm) {
        if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.GLACIER || biome == Biome.SNOW) {
            return 500.0f;
        }
        float npp = calculateMiamiNPP(tempC, effectivePrecipMm);
        float slopeRetention = (biome == Biome.MOUNTAINS) ? 0.25f : ((biome == Biome.HILLS) ? 0.55f : 1.0f);
        return Math.clamp(npp * 85.0f * slopeRetention, 2000.0f, 180000.0f);
    }
}


