package org.ether.society.core.dod;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.model.PhysicalConstants;

/**
 * Data-Oriented Environmental Kernel.
 * <p>
 * Models ecological biomass regeneration, agricultural yields, and natural resource decay
 * governed by smooth ($C^\infty$) differential equations anchored in Farquhar C3/C4 Photosynthesis
 * kinetics (FvCB), Priestley-Taylor Potential Evapotranspiration (PET), Lieth (1975) Miami NPP,
 * continuous topographic slope retention, and physical capital intensification ({@code AGENTS.md} Â§3, Â§10.1, Â§10.2).
 * </p>
 * <p>
 * Mathematically synchronized with {@code native/ether-core-native/src/environmental.rs}.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EnvironmentalKernel {

    private static final int LUT_TEMP_MIN = -10;
    private static final int LUT_TEMP_MAX = 45;
    private static final int LUT_TEMP_STEPS = LUT_TEMP_MAX - LUT_TEMP_MIN + 1; // 56
    private static final int LUT_CO2_MIN = 150;
    private static final int LUT_CO2_MAX = 1000;
    private static final int LUT_CO2_STEP_SIZE = 25;
    private static final int LUT_CO2_STEPS = (LUT_CO2_MAX - LUT_CO2_MIN) / LUT_CO2_STEP_SIZE + 1; // 35

    private static final float[][] FVCB_PHOTOSYNTHESIS_LUT = new float[LUT_TEMP_STEPS][LUT_CO2_STEPS];

    static {
        // Pre-compute C^infty smooth Farquhar FvCB assimilation LUT (Medlyn et al. 2002 peaked response)
        for (int tIdx = 0; tIdx < LUT_TEMP_STEPS; tIdx++) {
            double tempC = LUT_TEMP_MIN + tIdx;
            double tempK = tempC + PhysicalConstants.KELVIN_ZERO_CELSIUS;
            double chillActivation = SmoothMath.sigmoid(tempC, -1.0, 1.8);
            double heatPreservation = 1.0 - SmoothMath.sigmoid(tempC, 39.5, 2.5);
            double invDiff = (1.0 / 298.15) - (1.0 / tempK);

            double vcmax = 60.0 * Math.exp((65000.0 / PhysicalConstants.R_GAS_CONSTANT) * invDiff);
            double gammaStar = 42.75 * Math.exp((37830.0 / PhysicalConstants.R_GAS_CONSTANT) * invDiff);
            double kc = 404.9 * Math.exp((79430.0 / PhysicalConstants.R_GAS_CONSTANT) * invDiff);
            double ko = 278.4 * Math.exp((36380.0 / PhysicalConstants.R_GAS_CONSTANT) * invDiff);
            double km = kc * (1.0 + 210.0 / ko);

            for (int cIdx = 0; cIdx < LUT_CO2_STEPS; cIdx++) {
                double co2Ppm = LUT_CO2_MIN + cIdx * LUT_CO2_STEP_SIZE;
                double ci = co2Ppm * 0.70;
                double netCi = SmoothMath.softplus(ci - gammaStar, 1.0);
                double ac = vcmax * netCi / SmoothMath.softmax(1.0, ci + km, 2.0);
                double aj = 0.5 * vcmax * netCi / SmoothMath.softmax(1.0, ci + 2.0 * gammaStar, 2.0);
                double anet = SmoothMath.softmin(ac, aj, 0.5) * chillActivation * heatPreservation;
                float normalizedYield = (float) Math.clamp(0.05 + anet / 25.0, 0.05, 1.50);
                FVCB_PHOTOSYNTHESIS_LUT[tIdx][cIdx] = normalizedYield;
            }
        }
    }

    /**
     * Evaluates photosynthetic assimilation factor via bilinear interpolation over the precomputed
     * Farquhar FvCB LUT for smooth $C^1$ temperature and $\text{CO}_2$ sensitivity.
     *
     * @param tempC  surface air temperature (Â°C)
     * @param co2Ppm atmospheric $\text{CO}_2$ concentration (ppm)
     * @return normalized photosynthetic assimilation multiplier in $[0.05, 1.50]$
     */
    public static float evaluateFarquharYield(float tempC, float co2Ppm) {
        float tPos = Math.clamp(tempC - LUT_TEMP_MIN, 0.0f, (float) (LUT_TEMP_STEPS - 1));
        float cPos = Math.clamp((co2Ppm - LUT_CO2_MIN) / (float) LUT_CO2_STEP_SIZE, 0.0f, (float) (LUT_CO2_STEPS - 1));
        int t0 = Math.min((int) tPos, LUT_TEMP_STEPS - 2);
        int c0 = Math.min((int) cPos, LUT_CO2_STEPS - 2);
        float ft = tPos - t0;
        float fc = cPos - c0;
        float v00 = FVCB_PHOTOSYNTHESIS_LUT[t0][c0];
        float v10 = FVCB_PHOTOSYNTHESIS_LUT[t0 + 1][c0];
        float v01 = FVCB_PHOTOSYNTHESIS_LUT[t0][c0 + 1];
        float v11 = FVCB_PHOTOSYNTHESIS_LUT[t0 + 1][c0 + 1];
        float v0 = v00 + ft * (v10 - v00);
        float v1 = v01 + ft * (v11 - v01);
        return v0 + fc * (v1 - v0);
    }

    /**
     * Calculates Beer-Lambert canopy light transmission fraction $I / I_0 = \exp(-k_{\text{ext}} \cdot \text{LAI})$.
     *
     * @param leafAreaIndex   Leaf Area Index ($\text{m}^2/\text{m}^2$)
     * @param extinctionCoeff canopy light extinction coefficient $k_{\text{ext}}$
     * @return light transmission fraction to understory in $[0.0, 1.0]$
     */
    public static float calculateBeerLambertCanopyTransmission(float leafAreaIndex, float extinctionCoeff) {
        if (leafAreaIndex <= 0.0f) return 1.0f;
        return (float) Math.exp(-extinctionCoeff * leafAreaIndex);
    }

    private static final int PET_LUT_MIN = -5;
    private static final int PET_LUT_MAX = 50;
    private static final int PET_LUT_STEPS = (PET_LUT_MAX - PET_LUT_MIN) * 2 + 1; // 111 steps (0.5A°C)
    private static final float[] PET_LUT = new float[PET_LUT_STEPS];

    static {
        for (int i = 0; i < PET_LUT_STEPS; i++) {
            double t = PET_LUT_MIN + i * 0.5;
            double delta = (4098.0 * (0.6108 * Math.exp(17.27 * t / (t + 237.3)))) / ((t + 237.3) * (t + 237.3));
            double gammaPsy = 0.066;
            double alphaPT = 1.26;
            double netSolarRadiationMmEquiv = SmoothMath.softmax(100.0, (t + 10.0) * 35.0, 0.05);
            PET_LUT[i] = (float) (alphaPT * (delta / (delta + gammaPsy)) * netSolarRadiationMmEquiv);
        }
    }

    /**
     * Calculates Priestley-Taylor Potential Evapotranspiration (PET in mm/year).
     *
     * @param tempC surface air temperature (Â°C)
     * @return annual potential evapotranspiration (mm/yr)
     */
    public static float calculatePriestleyTaylorPET(float tempC) {
        if (tempC <= -5.0f) return PET_LUT[0];
        if (tempC >= 50.0f) return PET_LUT[PET_LUT_STEPS - 1];
        float idxF = (tempC - PET_LUT_MIN) * 2.0f;
        int idx = Math.min((int) idxF, PET_LUT_STEPS - 2);
        float frac = idxF - idx;
        return PET_LUT[idx] + frac * (PET_LUT[idx + 1] - PET_LUT[idx]);
    }

    /**
     * Executes one environmental update tick with default month {@code 0}.
     */
    public void tick(WorldBuffer world, float dt) {
        tick(world, 0, dt);
    }

    /**
     * Executes one environmental update tick over all active land cells in {@link WorldBuffer}.
     *
     * @param world DOD buffer container for world state
     * @param month current simulation month
     * @param dt    time delta in seconds (if &gt; 1000) or days
     */
    public void tick(WorldBuffer world, int month, float dt) {
        float[] food = world.getFoodResource();
        float[] temp = world.getTemperature();
        float[] rain = world.getRainfall();
        float[] elev = world.getElevation();

        int[] landIndices = world.getLandIndices();
        if (landIndices == null) return;
        float baselineCO2 = 280.0f;

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

        float[] waterArr = world.getWaterResource();
        float[] humanArr = world.getBiomassHuman();
        float[] capArr = world.getResourceCapital();
        float[] livestockArr = world.getBiomassLivestock();
        float[] bioNatArr = world.getBiomassNatural();
        float[] bioAgriArr = world.getBiomassAgriculture();

        for (int idx = 0; idx < landIndices.length; idx++) {
            int i = landIndices[idx];
            if (i < 0 || i >= world.getCapacity()) continue;

            float tempC = temp[i];
            float elevationM = (elev != null && i < elev.length) ? elev[i] : 0.0f;
            float waterAvail = (waterArr != null && i < waterArr.length) ? waterArr[i] : 0.0f;
            float effectiveMoisture = (float) SmoothMath.softmax(
                    rain[i],
                    SmoothMath.softmin(1500.0, rain[i] + waterAvail * 0.8, 0.02),
                    0.05);

            float npp = calculateMiamiNPP(tempC, effectiveMoisture);
            float frostFactor = SmoothMath.sigmoidF(tempC, -4.0f, 2.5f);
            float posElev = Math.max(0.0f, elevationM);
            float elevNorm = posElev / 1800.0f;
            float slopeRetention = 1.0f / (1.0f + elevNorm * elevNorm);

            float baseProd = Math.clamp(150.0f + npp * 1.95f * frostFactor, 150.0f, 3800.0f) * areaScale;
            float fvcbFactor = evaluateFarquharYield(tempC, baselineCO2);
            float pet = calculatePriestleyTaylorPET(tempC);
            float moistureAridityRatio = (float) SmoothMath.smoothClamp(effectiveMoisture / Math.max(100.0f, pet), 0.40, 1.25, 8.0);

            float humanPop = (humanArr != null && i < humanArr.length) ? humanArr[i] : 0.0f;
            float capital = (capArr != null && i < capArr.length) ? capArr[i] : 0.0f;
            float capPerCap = humanPop > 0.001f ? (capital / humanPop) : 0.0f;

            // Pre-industrial draft animal traction land share (Wrigley 1988, Smil 2008) from domesticated livestock biomass
            float bioDom = (livestockArr != null && i < livestockArr.length) ? livestockArr[i] : 0.0f;
            float animalTractionRatio = humanPop > 0.0f
                    ? (float) Math.tanh(bioDom / (humanPop * 0.35f + 1.0f))
                    : 0.0f;
            float fodderFactor = 1.0f - (float) PhysicalConstants.PREINDUSTRIAL_FODDER_LAND_FRACTION * animalTractionRatio;

            // Smooth C^infty physical capital intensification (tools, irrigation, machinery, nutrient cycling)
            float industrialBoost = 1.0f + 2.5f * (float) Math.tanh(capPerCap / 600.0f);
            float agroClimaticFactor = (float) SmoothMath.smoothClamp(
                    fvcbFactor * moistureAridityRatio * fodderFactor * industrialBoost, 0.40, 2.0, 6.0);

            float arableLandCap = 0.08f + 0.92f * (float) Math.tanh(capPerCap / 80.0f);
            float baseAgPotential = Math.clamp(npp * 85.0f * slopeRetention * frostFactor, 500.0f, 180000.0f);
            float maxAgPotential = baseAgPotential * areaScale * agroClimaticFactor * arableLandCap
                    * (1.0f + 0.20f * (float) Math.log1p(capPerCap / 50.0f));
            float agDemandPotential = humanPop * 6.5f;
            float agRatio = (maxAgPotential > 0.0f) ? (agDemandPotential / maxAgPotential) : 0.0f;
            float agProd = (humanPop > 0.0f && maxAgPotential > 0.0f)
                    ? (maxAgPotential * (1.0f - (float) Math.exp(-2.0f * agRatio)) * dtInYears)
                    : 0.0f;
            float naturalProd = baseProd * agroClimaticFactor * dtInYears;

            float growth = naturalProd + agProd;
            float retainedFood = food[i] * foodRetentionFactor;
            food[i] = Math.clamp(retainedFood + growth, 0.0f, 50000.0f * areaScale);

            if (bioNatArr != null && i < bioNatArr.length) {
                double forestAffinity = SmoothMath.sigmoid(effectiveMoisture, 450.0, 166.67)
                        * SmoothMath.sigmoid(tempC, 2.0, 4.0);
                float residenceYears = (float) (25.0 * forestAffinity + 4.0 * (1.0 - forestAffinity));
                float analyticalCapacityTons = Math.max(10000.0f * areaScale, npp * 1770.49f * areaScale * residenceYears * 0.025f);
                float foragerDependence = 1.0f - (float) Math.tanh(capPerCap / 25.0f);
                float minForagerBiomassTons = humanPop * 48.0f * foragerDependence;
                float natCeiling = Math.max(bioNatArr[i], Math.max(analyticalCapacityTons, minForagerBiomassTons));
                bioNatArr[i] = Math.min(natCeiling, bioNatArr[i] + naturalProd * 0.5f);
            }
            if (bioAgriArr != null && i < bioAgriArr.length) {
                float cultivationShare = (float) Math.tanh(capPerCap / 15.0f);
                float perCapitaAgTarget = humanPop * (250.0f + 370.0f * cultivationShare) * cultivationShare;
                float annualAgFluxKg = (agProd > 0.0f)
                        ? Math.max(perCapitaAgTarget, (agProd / Math.max(0.0001f, dtInYears)) / 0.0105f * cultivationShare)
                        : perCapitaAgTarget;
                bioAgriArr[i] = Math.max(bioAgriArr[i] * foodRetentionFactor, annualAgFluxKg);
            }
        }
    }

    /**
     * Computes smooth ($C^\infty$) Net Primary Productivity (NPP in $\text{g DM}/\text{m}^2/\text{yr}$)
     * analytically via the Lieth (1975) Miami Climate Model using soft-minimum coupling:
     * $$\text{NPP}(T, P) = \text{softmin}\!\left(\frac{3000}{1 + \exp(1.315 - 0.119\,T)},\; 3000\,(1 - \exp(-0.000664\,P))\right)$$
     *
     * @param tempC             mean surface temperature (Â°C)
     * @param effectivePrecipMm effective moisture / precipitation (mm/yr)
     * @return analytical NPP in metric tons of dry matter per $\text{km}^2$ per year
     */
    public static float calculateMiamiNPP(float tempC, float effectivePrecipMm) {
        double nppTemp = 3000.0 / (1.0 + Math.exp(1.315 - 0.119 * tempC));
        double posPrecip = SmoothMath.softplus(effectivePrecipMm, 0.2);
        double nppPrecip = 3000.0 * (1.0 - Math.exp(-0.000664 * posPrecip));
        return (float) Math.clamp(SmoothMath.softmin(nppTemp, nppPrecip, 0.02), 15.0, 3000.0);
    }
}
