package org.ether.society.generation;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Physically-based procedural planet generator using Simplex Noise on a sphere.
 *
 * <p>Fully parameterised for non-Earth worlds (Mars, Venus, Moon, Titan, Super-Earths, Eyeball worlds):</p>
 * <ul>
 *   <li><b>Gravity &amp; Altitude Lapse Rate</b>: Surface gravity $g_{rel}$ scales dynamically with planetary radius
 *       and body type. The environmental lapse rate $\Gamma = 6.5 \times g_{rel}$ (°C/km) scales with gravity.</li>
 *   <li><b>Atmospheric Pressure &amp; CO2 Partial Pressure</b>: Greenhouse forcing is computed from absolute
 *       CO2 partial pressure $P_{CO2} = P_{atmo} \times [CO2]/10^6$ rather than Earth-bound ppm. Airless worlds ($P_{atmo} &lt; 0.01$ atm)
 *       have zero greenhouse forcing and zero precipitation.</li>
 *   <li><b>Rotation &amp; Heat Transport</b>: Day length modulates the equator-to-pole thermal gradient. Slow-rotating
 *       or tidally-locked worlds exhibit global heat redistribution (flattened latitudinal gradient).</li>
 *   <li><b>Astronomical Tidal Forces &amp; Dissipation</b>: Satellites orbiting massive parent planets experience
 *       gravitational tidal flexing ($F_{tidal} \propto M_{parent} R / d^3$). Tidal energy dissipation generates
 *       internal geothermal heating, amplifies volcanism/seismicity, and expands intertidal coastal zones.</li>
 * </ul>
 */
public class ProceduralGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ProceduralGenerator.class);
    private static final ProceduralGenerator INSTANCE = new ProceduralGenerator();

    /**
     * Get instance.
     *
     * @return the resulting computation or state reference
     */
    public static ProceduralGenerator getInstance() {
        return INSTANCE;
    }

    /** Pre-industrial Earth reference CO2 partial pressure (1.0 atm * 280 ppm = 0.00028 atm). */
    private static final double CO2_REF_PARTIAL_PRESSURE_ATM = 0.00028;
    /** Radiative sensitivity: +3.0 °C per doubling of CO2 partial pressure. */
    private static final double CLIMATE_SENSITIVITY_K = 3.0;

    // -------------------------------------------------------------------------
    // Data Records
    // -------------------------------------------------------------------------

    public record PlanetPoint(
            double elevation,
            double elevationMeters,
            double temperature,
            double rainfall,
            double seasonality,
            Biome biome,
            double declivity,
            double riverFlow,
            double accessibleAquifer,
            double metalDensity) {

        public PlanetPoint(double elevation, double temperature, double rainfall,
                           Biome biome, double declivity, double riverFlow, double accessibleAquifer) {
            this(elevation, 0.0, temperature, rainfall, 0.0, biome, declivity, riverFlow, accessibleAquifer, 0.0);
        }

        /**
         * Planet point.
         *
         * @param elevation the elevation parameter (double)
         * @param temperature the temperature parameter (double)
         * @param rainfall the rainfall parameter (double)
         * @param biome the biome parameter (Biome)
         * @return the resulting computation or state reference
         */
        public PlanetPoint(double elevation, double temperature, double rainfall, Biome biome) {
            this(elevation, 0.0, temperature, rainfall, 0.0, biome, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Compute normalized gravitational tidal force intensity ($F_{tidal}$) exerted on the body.
     *
     * <p>Formula:</p>
     * $$F_{tidal} = \frac{(M_{parent} / M_\oplus) \times (R_{sat} / 1737\text{ km})}{(d_{orbit} / 384,400\text{ km})^3}$$
     */
    public static double computeTidalForceIntensity(PlanetPreset preset) {
        if (preset.isSatellite()) {
            double mParentEarthMasses = Math.max(0.01, preset.parentPlanetMassEarthMasses());
            double dKm = Math.max(10_000.0, preset.orbitalDistanceToParentKm());
            double rSatKm = Math.max(10.0, preset.radiusKm());

            double refM = 1.0;
            double refR = 1737.0;
            double refD = 384400.0;

            double fTidal = (mParentEarthMasses / refM) * (rSatKm / refR) / Math.pow(dKm / refD, 3);
            return Math.max(0.0, Math.min(20.0, fTidal));
        } else {
            double dAU = Math.max(0.01, preset.distanceToSunAU());
            double lum = Math.max(0.01, preset.solarLuminosity());
            double fSolarTidal = lum / Math.pow(dAU, 3);
            return Math.max(0.0, Math.min(5.0, fSolarTidal * 0.1));
        }
    }

    /**
     * Get planet point.
     *
     * @param lat the lat parameter (double)
     * @param lng the lng parameter (double)
     * @param preset the preset parameter (PlanetPreset)
     * @return the resulting computation or state reference
     */
    public PlanetPoint getPlanetPoint(double lat, double lng, PlanetPreset preset) {
        return getPlanetPoint(lat, lng, preset, preset.seed(), preset.seed() + 1000L, preset.seed() + 2000L);
    }

    public PlanetPoint getPlanetPoint(double lat, double lng, PlanetPreset preset,
                                       long elevSeed, long precipSeed, long seasonSeed) {

        SimplexNoise elevNoise     = new SimplexNoise(elevSeed != 0L ? elevSeed : preset.seed());
        SimplexNoise rainfallNoise = new SimplexNoise(precipSeed);
        SimplexNoise seasonNoise   = new SimplexNoise(seasonSeed);

        double freq  = preset.noiseFrequency();
        double scale = preset.noiseScale();

        // â”€â”€ 1. Elevation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double e = computeElevationAt(lat, lng, elevNoise, freq, scale, preset);

        double elevMeters;
        if (e >= 0) {
            elevMeters = e * preset.maxAltitudeMeters();
        } else {
            elevMeters = e * Math.abs(preset.minAltitudeMeters());
        }

        // â”€â”€ 2. Slope / Declivity â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double dDeg = 0.3;
        double eN = computeElevationAt(lat + dDeg, lng,       elevNoise, freq, scale, preset);
        double eS = computeElevationAt(lat - dDeg, lng,       elevNoise, freq, scale, preset);
        double eE = computeElevationAt(lat,         lng + dDeg, elevNoise, freq, scale, preset);
        double eW = computeElevationAt(lat,         lng - dDeg, elevNoise, freq, scale, preset);

        double dLat      = (eN - eS) / (2.0 * dDeg);
        double dLng      = (eE - eW) / (2.0 * dDeg);
        double declivity = Math.sqrt(dLat * dLat + dLng * dLng);

        // â”€â”€ 3. Tidal Acceleration & Internal Dissipation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double tidalForce = computeTidalForceIntensity(preset);

        // â”€â”€ 4. Rainfall â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double r = computeRainfallAt(lat, lng, rainfallNoise, freq, preset);

        // â”€â”€ 5. Temperature (with Tidal Geothermal Heating) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double tempC = computeTemperatureAt(lat, lng, e, elevMeters, r, preset, tidalForce);

        // â”€â”€ 6. Seasonality â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double seasonality = computeSeasonalityAt(lat, lng, seasonNoise, preset);

        // â”€â”€ 7. Biome â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Biome biome = determineBiome(lat, e, elevMeters, tempC, r, preset, tidalForce);

        // â”€â”€ 8. Hydrography â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double avgSurround  = (eN + eS + eE + eW) / 4.0;
        double valleyDepth  = Math.max(0.0, avgSurround - e);
        double riverFlow    = 0.0;
        double aquifer      = 0.0;

        if (e > preset.waterLevel() && preset.atmospherePressureAtm() >= 0.01) {
            riverFlow = r * (0.3 + 2.5 * declivity + 18.0 * valleyDepth);
            riverFlow = Math.min(1.0, Math.max(0.0, riverFlow));

            double flatBonus = Math.max(0.1, 1.0 - 2.5 * declivity);
            aquifer = r * flatBonus * (0.3 + 1.5 * valleyDepth) + 0.35 * riverFlow;
            aquifer = Math.min(1.0, Math.max(0.0, aquifer));
        }

        // â”€â”€ 9. Crustal Metal Density (Modulated by Seismic/Volcanic/Tidal Flexure)
        double effectiveSeismic  = preset.seismicActivityLevel() + Math.min(5.0, 1.5 * tidalForce);
        double effectiveVolcanic = preset.volcanicActivityLevel() + Math.min(5.0, 2.0 * tidalForce);
        double seismicBoost  = Math.min(1.0, effectiveSeismic  / 5.0);
        double volcanicBoost = Math.min(1.0, effectiveVolcanic / 4.0);

        double metalDensity  = 0.0;
        if (e > preset.waterLevel()) {
            double altitudeFactor = Math.min(1.0, Math.max(0.0, e + 0.3));
            metalDensity = altitudeFactor * (0.4 + 0.4 * seismicBoost + 0.2 * volcanicBoost);
            metalDensity = Math.min(1.0, Math.max(0.0, metalDensity));
        }

        return new PlanetPoint(e, elevMeters, tempC, r, seasonality,
                biome, declivity, riverFlow, aquifer, metalDensity);
    }

    /**
     * Generate planet.
     *
     * @param preset the preset parameter (PlanetPreset)
     * @return the resulting computation or state reference
     */
    public List<H3Cell> generatePlanet(PlanetPreset preset) {
        return generatePlanet(preset, null, null);
    }

    /**
     * Generate planet.
     *
     * @param preset the preset parameter (PlanetPreset)
     * @param progressCallback the progress callback parameter (Integer&gt;)
     * @return the resulting computation or state reference
     */
    public List<H3Cell> generatePlanet(PlanetPreset preset, java.util.function.BiConsumer<Integer, Integer> progressCallback) {
        return generatePlanet(preset, progressCallback, null);
    }

    public List<H3Cell> generatePlanet(PlanetPreset preset,
                                       java.util.function.BiConsumer<Integer, Integer> progressCallback,
                                       java.util.function.BooleanSupplier cancelSupplier) {
        logger.info("Generating planet: {} (Res: {}, Seed: {}, Radius: {} km, Atmo: {} atm, Satellite: {}, TidalForce: {})",
                preset.name(), preset.resolution(), preset.seed(), preset.radiusKm(), preset.atmospherePressureAtm(),
                preset.isSatellite(), computeTidalForceIntensity(preset));

        List<H3Cell> cells = H3Service.getInstance().generateGlobalMetadata(preset.resolution());
        logger.info("Base grid: {} H3 cells", cells.size());

        int total = cells.size();
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
        int progressInterval = Math.max(100, total / 30);

        cells.parallelStream().forEach(cell -> {
            if (cancelSupplier != null && cancelSupplier.getAsBoolean()) {
                throw new java.util.concurrent.CancellationException("Planet generation cancelled by user");
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new java.util.concurrent.CancellationException("Planet generation thread interrupted");
            }
            PlanetPoint p = getPlanetPoint(cell.getLatitude(), cell.getLongitude(), preset);
            cell.setElevation(p.elevationMeters());
            cell.setTemperature(p.temperature());
            cell.setRainfall(p.rainfall());
            cell.setBiome(p.biome());
            populateResources(cell, p, preset);

            int done = counter.incrementAndGet();
            if (progressCallback != null && (done % progressInterval == 0 || done == total)) {
                progressCallback.accept(done, total);
            }
        });

        if (cancelSupplier != null && cancelSupplier.getAsBoolean()) {
            throw new java.util.concurrent.CancellationException("Planet generation cancelled by user");
        }

        accumulateHydrographyFlow(cells, preset, cancelSupplier);

        return cells;
    }

    /**
     * Generate regional planet.
     *
     * @param preset the preset parameter (PlanetPreset)
     * @param minLat the min lat parameter (double)
     * @param maxLat the max lat parameter (double)
     * @param minLng the min lng parameter (double)
     * @param maxLng the max lng parameter (double)
     * @return the resulting computation or state reference
     */
    public List<H3Cell> generateRegionalPlanet(PlanetPreset preset, double minLat, double maxLat, double minLng, double maxLng) {
        logger.info("Generating regional planet window: {} (Res: {}, Lat[{}..{}], Lng[{}..{}])",
                preset.name(), preset.resolution(), minLat, maxLat, minLng, maxLng);

        List<H3Cell> cells = H3Service.getInstance().generateRegionalMetadata(preset.resolution(), minLat, maxLat, minLng, maxLng);
        logger.info("Regional grid: {} H3 cells retained in memory", cells.size());

        cells.parallelStream().forEach(cell -> {
            PlanetPoint p = getPlanetPoint(cell.getLatitude(), cell.getLongitude(), preset);
            cell.setElevation(p.elevationMeters());
            cell.setTemperature(p.temperature());
            cell.setRainfall(p.rainfall());
            cell.setBiome(p.biome());
            populateResources(cell, p, preset);
        });

        accumulateHydrographyFlow(cells, preset, null);
        return cells;
    }

    // -------------------------------------------------------------------------
    // Private: Physical Algorithms
    // -------------------------------------------------------------------------

    private double computeElevationAt(double lat, double lng,
                                       SimplexNoise noise, double freq, double scale, PlanetPreset preset) {
        double latR = Math.toRadians(lat);
        double lngR = Math.toRadians(lng);
        double x = Math.cos(latR) * Math.cos(lngR);
        double y = Math.cos(latR) * Math.sin(lngR);
        double z = Math.sin(latR);

        double e  = 1.00 * noise.noise(freq       * x, freq       * y, freq       * z);
        e        += 0.50 * noise.noise(freq * 2.0 * x, freq * 2.0 * y, freq * 2.0 * z);
        e        += 0.25 * noise.noise(freq * 4.0 * x, freq * 4.0 * y, freq * 4.0 * z);
        e /= 1.75;
        e *= scale;
        return Math.max(-1.0, Math.min(1.0, e));
    }

    private double computeTemperatureAt(double lat, double lng, double normElev, double elevMeters,
                                         double rainfall, PlanetPreset preset, double tidalForce) {
        // â”€â”€ 1. Relative Surface Gravity & Dynamic Lapse Rate â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double rRel = Math.max(0.1, preset.radiusKm() / 6371.0);
        double gRel = preset.isSatellite() ? Math.max(0.05, rRel * 0.4) : Math.max(0.1, rRel);
        double lapseRateCPerKm = 6.5 * gRel;

        // â”€â”€ 2. Rotation & Latitudinal Gradient Damping / Tidal Locking â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double dayHours = Math.max(1.0, preset.dayLengthHours());
        double rotationDamping = Math.min(1.2, Math.max(0.15, Math.pow(24.0 / dayHours, 0.35)));

        double latTemp;
        if (preset.isTidalLocked()) {
            // Tidally locked / Eyeball World: subsolar hot spot at (0,0), dark cold side at (0,180)
            double cosTheta = Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lng));
            latTemp = preset.averageTempC() + preset.temperatureGradient() * (cosTheta - 0.2);
        } else {
            double cosLat  = Math.cos(Math.toRadians(lat));
            latTemp = preset.averageTempC() + (preset.temperatureGradient() * rotationDamping) * (cosLat - 0.5);
        }

        // â”€â”€ 3. COâ‚‚ Partial Pressure Greenhouse Forcing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double pAtmo = Math.max(0.0, preset.atmospherePressureAtm());
        double co2Forcing = 0.0;
        double pressureBoost = 0.0;

        if (pAtmo >= 0.01) {
            double co2Ppm = Math.max(0.0, preset.co2Ppm());
            double co2PartialPressureAtm = pAtmo * (co2Ppm / 1_000_000.0);

            if (co2PartialPressureAtm > 0) {
                double ratio = co2PartialPressureAtm / CO2_REF_PARTIAL_PRESSURE_ATM;
                co2Forcing = CLIMATE_SENSITIVITY_K * (Math.log(Math.max(0.0001, ratio)) / Math.log(2.0));
            }

            if (pAtmo > 1.0) {
                pressureBoost = 8.0 * Math.log10(pAtmo);
            } else {
                pressureBoost = (pAtmo - 1.0) * 8.0;
            }
        }

        // â”€â”€ 4. Geothermal Heating from Internal Tidal Dissipation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double tidalGeothermalWarming = preset.isSatellite() ? Math.min(45.0, 10.0 * Math.sqrt(tidalForce)) : 0.0;

        // â”€â”€ 5. Altitude Lapse Rate â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double lapseCorrection = 0.0;
        if (elevMeters > 0) {
            lapseCorrection = (elevMeters / 1000.0) * lapseRateCPerKm;
        }

        // â”€â”€ 6. Evaporative Cooling â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double evapCooling = 0.0;
        if (latTemp > 20.0 && rainfall > 0.5 && pAtmo >= 0.1) {
            evapCooling = (latTemp - 20.0) * (rainfall - 0.5) * 0.4;
        }

        double tempC = latTemp + co2Forcing + pressureBoost + tidalGeothermalWarming - lapseCorrection - evapCooling;
        return Math.max(-250.0, Math.min(600.0, tempC));
    }

    private double computeRainfallAt(double lat, double lng,
                                      SimplexNoise noise, double freq,
                                      PlanetPreset preset) {
        double pAtmo = Math.max(0.0, preset.atmospherePressureAtm());
        if (pAtmo < 0.01) return 0.0;

        double latR = Math.toRadians(lat);
        double lngR = Math.toRadians(lng);
        double x = Math.cos(latR) * Math.cos(lngR);
        double y = Math.cos(latR) * Math.sin(lngR);
        double z = Math.sin(latR);

        double r  = 1.00 * noise.noise(freq * 1.5 * x, freq * 1.5 * y, freq * 1.5 * z);
        r        += 0.50 * noise.noise(freq * 3.0 * x, freq * 3.0 * y, freq * 3.0 * z);
        r /= 1.50;
        r = (r + 1.0) / 2.0;

        double absLat = Math.abs(lat);
        double latMod;
        if (absLat <= 10.0) {
            latMod = 1.0;
        } else if (absLat <= 30.0) {
            double t = (absLat - 10.0) / 20.0;
            latMod = 1.0 - t * 0.75;
        } else if (absLat <= 45.0) {
            double t = (absLat - 30.0) / 15.0;
            latMod = 0.25 + t * 0.50;
        } else if (absLat <= 65.0) {
            double t = (absLat - 45.0) / 20.0;
            latMod = 0.75 - t * 0.30;
        } else {
            double t = (absLat - 65.0) / 25.0;
            latMod = 0.45 - t * 0.38;
        }

        r = r * 0.70 + latMod * 0.30;
        double pressureFactor = Math.min(2.0, Math.sqrt(pAtmo));
        r = r * (0.5 + 0.5 * pressureFactor);

        return Math.max(0.0, Math.min(1.0, r));
    }

    private double computeSeasonalityAt(double lat, double lng,
                                         SimplexNoise seasonNoise, PlanetPreset preset) {
        if (preset.axialTiltDegrees() < 0.5 || preset.atmospherePressureAtm() < 0.01) return 0.0;

        double tiltFactor = Math.min(1.0, preset.axialTiltDegrees() / 45.0);
        double latFraction = Math.abs(lat) / 90.0;
        double latAmplitude = Math.pow(latFraction, 0.7);

        double latR = Math.toRadians(lat);
        double lngR = Math.toRadians(lng);
        double x = Math.cos(latR) * Math.cos(lngR);
        double y = Math.cos(latR) * Math.sin(lngR);
        double z = Math.sin(latR);
        double noise = (seasonNoise.noise(x, y, z) + 1.0) / 2.0;

        double seasonNorm = tiltFactor * (latAmplitude * 0.70 + noise * 0.30);
        return Math.max(0.0, Math.min(1.0, seasonNorm));
    }

    private Biome determineBiome(double lat, double elevation, double elevMeters,
                                  double tempC, double rainfall, PlanetPreset preset, double tidalForce) {
        return ProceduralPlanetRasterSynthesizer.determineBiome(lat, elevation, elevMeters, tempC, rainfall, preset, tidalForce);
    }

    public record ProceduralRasterSuite(
            java.awt.image.BufferedImage elevation,
            java.awt.image.BufferedImage biomes,
            java.awt.image.BufferedImage temperature,
            java.awt.image.BufferedImage rainfall,
            java.awt.image.BufferedImage seasonality) {
        public ProceduralRasterSuite(ProceduralPlanetRasterSynthesizer.ProceduralRasterSuite s) {
            this(s.elevation(), s.biomes(), s.temperature(), s.rainfall(), s.seasonality());
        }
    }

    /**
     * Synthesizes a complete set of procedural equirectangular rasters ($2048 \times 1024$)
     * using deterministic spherical Simplex noise.
     */
    public ProceduralRasterSuite synthesizeProceduralPlanetRasters(PlanetPreset preset, int width, int height) {
        return new ProceduralRasterSuite(
                ProceduralPlanetRasterSynthesizer.synthesizeProceduralPlanetRasters(this, preset, width, height));
    }

    public static int biomeToStandardRgb(Biome b) {
        return ProceduralPlanetRasterSynthesizer.biomeToStandardRgb(b);
    }

    private void populateResources(H3Cell cell, PlanetPoint p, PlanetPreset preset) {
        PlanetResourcePopulator.populateResources(cell, p, preset);
    }

    private void accumulateHydrographyFlow(List<H3Cell> cells, PlanetPreset preset, java.util.function.BooleanSupplier cancelSupplier) {
        PlanetHydrographyFlowCalculator.accumulateHydrographyFlow(cells, preset, cancelSupplier);
    }
}