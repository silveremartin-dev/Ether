/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.h3.H3Service;
import org.ether.society.model.PhysicalUnitsHelper;
import org.ether.society.model.SpatialSurfaceHelper;

/**
 * Betz Wind Kinetic Power &amp; Solar Radiance Physics Engine.
 *
 * <p>Computes meteorological wind and solar power densities (\(\text{W/m}^2\)) from physical
 * elevation and spherical latitude, and converts only the fraction actually harnessed by installed
 * physical converter capital (\(K/N\)) into cellular anthropogenic {@code energyWind} and
 * {@code energySolar} fluxes in canonical SI units (\(\text{GJ/yr}\), {@code AGENTS.md} Contract 2):</p>
 * <ol>
 *   <li><b>Betz Law Wind Kinetic Power Density</b>:
 *       \(P_{\text{wind}} = \frac{1}{2} \rho_{\text{air}} v^3 C_p\) (\(C_p \le 0.593\)), bounded at macro-scale
 *       by Kleidon's atmospheric kinetic extraction limit (\(\sim 1.25\,\text{W/m}^2\)).</li>
 *   <li><b>Surface Solar Irradiance</b>:
 *       \(I_{\text{surf}}(\phi) = \frac{S_0}{4} \tau_{\text{atm}} \max(0.15, \cos\phi) \cdot 1.27\)
 *       with \(S_0 = 1361\,\text{W/m}^2\).</li>
 *   <li><b>Smooth Exosomatic Converter Coupling (\(C^\infty\))</b>:
 *       Uninhabited or pre-converter cells capture \(0.0\,\text{GJ/yr}\); traditional sails/windmills
 *       activate smoothly above \(k_{\text{pc}} \approx 320\,\text{kg/cap}\), and industrial wind turbines
 *       and semiconductor photovoltaics activate smoothly above \(k_{\text{pc}} \approx 4\,500\text{--}5\,000\,\text{kg/cap}\)
 *       (Smil 2017).</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class RenewableEnergyPhysicsEngine extends AbstractSimulationEngine {

    /** Top-of-atmosphere solar constant \(S_0\) in \(\text{W/m}^2\). */
    public static final double SOLAR_CONSTANT_W_PER_M2 = 1361.0;

    /** Betz Limit theoretical maximum aerodynamic power coefficient \(C_p = 16/27 \approx 0.593\). */
    public static final double BETZ_LIMIT_CP = 0.593;

    /** Standard sea-level dry air density \(\rho_{\text{air}}\) in \(\text{kg/m}^3\). */
    public static final double AIR_DENSITY_KG_PER_M3 = 1.225;

    /** Kleidon (2011) planetary boundary-layer limit on continuous wind power extraction (\(\text{W/m}^2\)). */
    public static final double KLEIDON_MAX_WIND_EXTRACTION_W_PER_M2 = 1.25;

    public RenewableEnergyPhysicsEngine() {
        super("RenewableEnergyPhysicsEngine", "RenewableEnergyPhysicsEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    /**
     * Calculates local kinetic wind power density in \(\text{W/m}^2\) from Betz's Law.
     *
     * @param windSpeedMetersPerSec wind speed in \(\text{m/s}\)
     * @return kinetic wind power density in \(\text{W/m}^2\)
     */
    public static double calculateBetzWindPowerWattsPerM2(double windSpeedMetersPerSec) {
        if (windSpeedMetersPerSec <= 0.0) {
            return 0.0;
        }
        return 0.5 * AIR_DENSITY_KG_PER_M3 * Math.pow(windSpeedMetersPerSec, 3.0) * BETZ_LIMIT_CP;
    }

    /**
     * Calculates annual-mean incident surface solar irradiance in \(\text{W/m}^2\) at latitude \(\phi\).
     *
     * @param latDeg latitude in degrees \([-90, +90]\)
     * @return mean surface solar irradiance in \(\text{W/m}^2\)
     */
    public static double calculateSurfaceSolarIrradianceWattsPerM2(double latDeg) {
        double latRad = Math.toRadians(latDeg);
        return SOLAR_CONSTANT_W_PER_M2 * 0.25 * 1.27 * Math.max(0.15, Math.cos(latRad)) * 0.70;
    }

    /**
     * Computes harnessed anthropogenic aeolian power per capita (\(\text{W/person}\)) from local
     * wind power density (\(\text{W/m}^2\)) and physical converter capital per capita (\(K/N\), \(\text{kg/person}\)).
     *
     * @param windPowerDensityWM2 local Betz kinetic wind power density in \(\text{W/m}^2\)
     * @param capitalPerCapitaKg  physical built capital per capita in \(\text{kg/person}\)
     * @return captured mechanical/electrical wind power in \(\text{W/person}\)
     */
    public static double computeCapturedWindWattsPerCapita(double windPowerDensityWM2, double capitalPerCapitaKg) {
        if (windPowerDensityWM2 <= 0.0 || capitalPerCapitaKg <= 0.0) {
            return 0.0;
        }
        double windResourceFactor = Math.tanh(windPowerDensityWM2 / 120.0);
        double traditionalMillSailW = 18.0 * Math.tanh(SmoothMath.softplus(capitalPerCapitaKg - 320.0, 0.04) / 400.0);
        double modernTurbineW = 220.0 * Math.tanh(SmoothMath.softplus(capitalPerCapitaKg - 4500.0, 0.005) / 8000.0);
        return (traditionalMillSailW + modernTurbineW) * windResourceFactor;
    }

    /**
     * Computes harnessed anthropogenic solar photovoltaic/thermal collector power per capita (\(\text{W/person}\))
     * from surface irradiance (\(\text{W/m}^2\)) and physical converter capital per capita (\(K/N\), \(\text{kg/person}\)).
     *
     * @param surfaceIrradianceWM2 incident surface solar irradiance in \(\text{W/m}^2\)
     * @param capitalPerCapitaKg   physical built capital per capita in \(\text{kg/person}\)
     * @return captured solar power in \(\text{W/person}\)
     */
    public static double computeCapturedSolarWattsPerCapita(double surfaceIrradianceWM2, double capitalPerCapitaKg) {
        if (surfaceIrradianceWM2 <= 0.0 || capitalPerCapitaKg <= 0.0) {
            return 0.0;
        }
        double insolationFactor = Math.clamp(surfaceIrradianceWM2 / 220.0, 0.1, 1.5);
        return 260.0 * Math.tanh(SmoothMath.softplus(capitalPerCapitaKg - 5000.0, 0.005) / 9000.0) * insolationFactor;
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processRenewableEnergyPhysics(buf);
    }

    /**
     * Updates cellular {@code energyWind} and {@code energySolar} arrays in {@link WorldBuffer}
     * strictly in canonical SI annual energy units (\(\text{GJ/yr}\)).
     *
     * @param buffer target world buffer
     */
    public static void processRenewableEnergyPhysics(WorldBuffer buffer) {
        if (buffer == null || buffer.getCapacity() == 0) {
            return;
        }

        float[] elevArr = buffer.getElevation();
        float[] popArr = buffer.getHumanPopulation();
        float[] capArr = buffer.getResourceCapital();
        float[] windArr = buffer.getEnergyWind();
        float[] solarArr = buffer.getEnergySolar();
        long[] h3Arr = buffer.getH3Indexes();
        int n = buffer.getCapacity();

        H3Service h3Service = H3Service.getInstance();
        for (int i = 0; i < n; i++) {
            double pop = popArr != null ? popArr[i] : 0.0;
            if (pop <= 0.0) {
                windArr[i] = 0.0f;
                solarArr[i] = 0.0f;
                continue;
            }
            double kpc = capArr != null ? Math.max(0.0, capArr[i] / pop) : 0.0;
            if (kpc <= 0.0) {
                windArr[i] = 0.0f;
                solarArr[i] = 0.0f;
                continue;
            }

            double elev = elevArr != null ? Math.max(0.0, elevArr[i]) : 0.0;
            double windSpeed = 5.0 + Math.min(10.0, elev / 300.0);
            double betzWindWM2 = calculateBetzWindPowerWattsPerM2(windSpeed);

            double cellAreaM2 = SpatialSurfaceHelper.resolveCellAreaKm2(buffer, i) * PhysicalUnitsHelper.SQ_METERS_PER_SQ_KM;
            double maxKleidonCellWindWatts = KLEIDON_MAX_WIND_EXTRACTION_W_PER_M2 * cellAreaM2;
            double capturedWindCellWatts = Math.min(maxKleidonCellWindWatts, pop * computeCapturedWindWattsPerCapita(betzWindWM2, kpc));
            windArr[i] = (float) PhysicalUnitsHelper.wattsToGjPerYear(capturedWindCellWatts);

            double latDeg = 0.0;
            if (h3Arr != null && h3Arr[i] != 0L) {
                try {
                    latDeg = h3Service.cellToLatLng(h3Arr[i]).lat;
                } catch (Exception ignored) {
                    latDeg = 0.0;
                }
            }
            double solarIrradianceWM2 = calculateSurfaceSolarIrradianceWattsPerM2(latDeg);
            double capturedSolarCellWatts = pop * computeCapturedSolarWattsPerCapita(solarIrradianceWM2, kpc);
            solarArr[i] = (float) PhysicalUnitsHelper.wattsToGjPerYear(capturedSolarCellWatts);
        }
    }
}
