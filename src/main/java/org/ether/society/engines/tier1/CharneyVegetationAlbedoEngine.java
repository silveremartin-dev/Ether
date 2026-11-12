/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Charney Vegetation-Albedo Dynamic Bistability &amp; Abrupt Desertification Engine.
 *
 * <p>Models the coupled non-linear feedback between surface albedo, monsoonal precipitation,
 * and vegetation cover across the Saharo-Sahelian belt, based on the canonical models of
 * Jule Charney (1975), Victor Brovkin et al. (1998), and Martin Claussen et al. (1999):</p>
 *
 * <p><b>Charney Radiative Subsidence Mechanism:</b>
 * High surface albedo of bare desert sand ($\alpha_{\text{sand}} \approx 0.38$) enhances net radiative cooling
 * of the tropospheric column, driving atmospheric subsidence (sinking motion), suppressing convective precipitation,
 * and maintaining hyper-aridity. Conversely, dense vegetation cover ($V \in [0, 1]$, $\alpha_{\text{veg}} \approx 0.16$)
 * absorbs solar radiation, fueling thermal convection and monsoonal rainfall:
 * $$\alpha(V) = \alpha_{\text{sand}} (1 - V) + \alpha_{\text{veg}} V$$
 * $$P = \max\left(20.0, P_{\text{base}} + \Delta P_{\text{orbital}}(\varpi) + \gamma_{\text{albedo}} (\alpha_{\text{crit}} - \alpha)\right)$$
 * </p>
 *
 * <p><b>Cusp Catastrophe &amp; Mid-Holocene Green Sahara Collapse (~-4,100 BCE):</b>
 * The equilibrium vegetation cover obeys the analytical saturation formulation:
 * $$V^*(P) = 1.0 - \exp\left(-\frac{\max(0.0, P - P_{\text{crit}})}{P_{\text{scale}}}\right)$$
 * Driven by the Milankovitch precession cycle ($\varpi$, 23 ka), as orbital insolation declines past the
 * critical saddle-node bifurcation threshold around $-4\,100$ BCE (6,000 BP), the system undergoes an abrupt
 * transition from the "Green Sahara" state ($V > 0.60$) to the modern hyper-arid desert ($V < 0.05$) within ~200-300 years,
 * catalyzing the pastoralist migration into the Nile Valley and the genesis of the Egyptian Old Kingdom.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class CharneyVegetationAlbedoEngine extends AbstractSimulationEngine {
    public CharneyVegetationAlbedoEngine() {
        super("CharneyVegetationAlbedoEngine", "CharneyVegetationAlbedoEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(CharneyVegetationAlbedoEngine.class);

    /** Albedo of dry bare desert sand */
    public static final double ALBEDO_DESERT_SAND = 0.38;

    /** Albedo of dense semi-arid savanna / steppe vegetation */
    public static final double ALBEDO_SAVANNA_VEGETATION = 0.16;

    /** Critical precipitation threshold below which vegetation cannot establish (mm/year) */
    public static final double PRECIP_CRITICAL_MM = 100.0;

    /** Characteristic precipitation scale for vegetation saturation (mm/year) */
    public static final double PRECIP_SCALE_MM = 220.0;

    /** Timescale for vegetation biome adaptation / succession (years) */
    public static final double TAU_VEGETATION_YEARS = 25.0;

    /** Albedo radiative feedback coupling coefficient (mm/year per unit albedo drop) */
    public static final double ALBEDO_FEEDBACK_GAMMA = 350.0;

    /** Pristine biomass density of wet savanna in g/m² (or kg/ha) */
    public static final double SAVANNA_MAX_BIOMASS = 400.0;

    /** Precession cycle period in years */
    public static final double PRECESSION_CYCLE_YEARS = 23_000.0;

    /** African Humid Period optimum alignment year (~-7,000 BCE / 9,000 BP) */
    public static final double AHP_PEAK_YEAR = -7000.0;

    /**
     * Calculates the Milankovitch precession orbital monsoon rainfall anomaly in mm/year.
     * During the Holocene Thermal Maximum (~9,000 to ~6,000 BP), precession placed perihelion in boreal summer,
     * strengthening the African monsoon by up to ~180 mm/year (Kutzbach 1981; deMenocal et al. 2000; Claussen et al. 1999).
     *
     * @param currentYear simulation year (negative for BCE/BP)
     * @return orbital rainfall perturbation in mm/year
     */
    public static double calculateOrbitalMonsoonAnomalyMm(double currentYear) {
        double phase = 2.0 * Math.PI * (currentYear - AHP_PEAK_YEAR) / PRECESSION_CYCLE_YEARS;
        return 180.0 * Math.cos(phase);
    }

    /**
     * Calculates the equilibrium vegetation fraction $V^*(P) \in [0, 1]$.
     *
     * @param precipMm local annual precipitation in mm/year
     * @return equilibrium vegetation fraction
     */
    public static double calculateEquilibriumVegetation(double precipMm) {
        if (precipMm <= PRECIP_CRITICAL_MM) return 0.0;
        double excess = precipMm - PRECIP_CRITICAL_MM;
        return 1.0 - Math.exp(-excess / PRECIP_SCALE_MM);
    }

    /**
     * Calculates surface dynamic albedo from vegetation fraction $V$.
     *
     * @param vegFraction vegetation cover fraction in $[0, 1]$
     * @return surface albedo
     */
    public static double calculateSurfaceAlbedo(double vegFraction) {
        double v = Math.clamp(vegFraction, 0.0, 1.0);
        return ALBEDO_DESERT_SAND * (1.0 - v) + ALBEDO_SAVANNA_VEGETATION * v;
    }

    /**
     * Evaluates whether a cell falls within the Saharo-Sahelian domain (12°N-32°N, -18°E to +40°E).
     *
     * @param lat latitude in degrees
     * @param lng longitude in degrees
     * @return dimensionless spatial weight in $[0, 1]$
     */
    public static double calculateSaharaSpatialWeight(double lat, double lng) {
        double latWeight = SmoothMath.sigmoid(lat, 12.0, 2.5) * (1.0 - SmoothMath.sigmoid(lat, 32.0, 2.5));
        double lonWeight = SmoothMath.sigmoid(lng, -18.0, 3.0) * (1.0 - SmoothMath.sigmoid(lng, 40.0, 3.0));
        return latWeight * lonWeight;
    }

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (buffer == null || buffer.getCapacity() == 0 || dtYears <= 0.0) return;
        double dtSafe = Math.clamp(dtYears, 0.001, 50.0);
        double relaxationFactor = 1.0 - Math.exp(-dtSafe / TAU_VEGETATION_YEARS);
        float[] rainArr = buffer.getRainfall();
        float[] bioNatArr = buffer.getBiomassNatural();
        int n = buffer.getCapacity();
        for (int i = 0; i < n; i++) {
            double curRain = rainArr[i];
            double curBiomass = bioNatArr[i];
            double curVeg = Math.clamp(curBiomass / SAVANNA_MAX_BIOMASS, 0.0, 1.0);
            double curAlbedo = calculateSurfaceAlbedo(curVeg);

            double basePrecip = Math.max(80.0, curRain);
            double albedoConvectiveBonus = ALBEDO_FEEDBACK_GAMMA * (0.35 - curAlbedo);
            double targetPrecip = Math.max(15.0, basePrecip + albedoConvectiveBonus);
            double targetVeg = calculateEquilibriumVegetation(targetPrecip);

            double newVeg = curVeg + (targetVeg - curVeg) * relaxationFactor;
            double newBiomass = Math.max(5.0, newVeg * SAVANNA_MAX_BIOMASS);

            bioNatArr[i] = (float) newBiomass;
            rainArr[i] = (float) (curRain + (targetPrecip - curRain) * relaxationFactor);
        }
    }
}
