package org.ether.society.procedural;

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
 *   <li><b>Gravity & Altitude Lapse Rate</b>: Surface gravity $g_{rel}$ scales dynamically with planetary radius
 *       and body type. The environmental lapse rate $\Gamma = 6.5 \times g_{rel}$ (°C/km) scales with gravity.</li>
 *   <li><b>Atmospheric Pressure & CO₂ Partial Pressure</b>: Greenhouse forcing is computed from absolute
 *       CO₂ partial pressure $P_{CO₂} = P_{atmo} \times [CO₂]/10⁶$ rather than Earth-bound ppm. Airless worlds ($P_{atmo} < 0.01$ atm)
 *       have zero greenhouse forcing and zero precipitation.</li>
 *   <li><b>Rotation & Heat Transport</b>: Day length modulates the equator-to-pole thermal gradient. Slow-rotating
 *       or tidally-locked worlds exhibit global heat redistribution (flattened latitudinal gradient).</li>
 *   <li><b>Astronomical Tidal Forces & Dissipation</b>: Satellites orbiting massive parent planets experience
 *       gravitational tidal flexing ($F_{tidal} \propto M_{parent} R / d^3$). Tidal energy dissipation generates
 *       internal geothermal heating, amplifies volcanism/seismicity, and expands intertidal coastal zones.</li>
 * </ul>
 */
public class ProceduralGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ProceduralGenerator.class);

    /** Pre-industrial Earth reference CO₂ partial pressure (1.0 atm × 280 ppm = 0.00028 atm). */
    private static final double CO2_REF_PARTIAL_PRESSURE_ATM = 0.00028;
    /** Radiative sensitivity: +3.0 °C per doubling of CO₂ partial pressure. */
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

    public PlanetPoint getPlanetPoint(double lat, double lng, PlanetPreset preset) {
        return getPlanetPoint(lat, lng, preset, preset.seed(), preset.seed() + 1000L, preset.seed() + 2000L);
    }

    public PlanetPoint getPlanetPoint(double lat, double lng, PlanetPreset preset,
                                       long tempSeed, long precipSeed, long seasonSeed) {

        SimplexNoise elevNoise     = new SimplexNoise(preset.seed());
        SimplexNoise rainfallNoise = new SimplexNoise(precipSeed);
        SimplexNoise seasonNoise   = new SimplexNoise(seasonSeed);

        double freq  = preset.noiseFrequency();
        double scale = preset.noiseScale();

        // ── 1. Elevation ───────────────────────────────────────────────────────
        double e = computeElevationAt(lat, lng, elevNoise, freq, scale);

        double elevMeters;
        if (e >= 0) {
            elevMeters = e * preset.maxAltitudeMeters();
        } else {
            elevMeters = e * Math.abs(preset.minAltitudeMeters());
        }

        // ── 2. Slope / Declivity ───────────────────────────────────────────────
        double dDeg = 0.3;
        double eN = computeElevationAt(lat + dDeg, lng,       elevNoise, freq, scale);
        double eS = computeElevationAt(lat - dDeg, lng,       elevNoise, freq, scale);
        double eE = computeElevationAt(lat,         lng + dDeg, elevNoise, freq, scale);
        double eW = computeElevationAt(lat,         lng - dDeg, elevNoise, freq, scale);

        double dLat      = (eN - eS) / (2.0 * dDeg);
        double dLng      = (eE - eW) / (2.0 * dDeg);
        double declivity = Math.sqrt(dLat * dLat + dLng * dLng);

        // ── 3. Tidal Acceleration & Internal Dissipation ───────────────────────
        double tidalForce = computeTidalForceIntensity(preset);

        // ── 4. Rainfall ────────────────────────────────────────────────────────
        double r = computeRainfallAt(lat, lng, rainfallNoise, freq, preset);

        // ── 5. Temperature (with Tidal Geothermal Heating) ──────────────────────
        double tempC = computeTemperatureAt(lat, lng, e, elevMeters, r, preset, tidalForce);

        // ── 6. Seasonality ─────────────────────────────────────────────────────
        double seasonality = computeSeasonalityAt(lat, lng, seasonNoise, preset);

        // ── 7. Biome ───────────────────────────────────────────────────────────
        Biome biome = determineBiome(e, elevMeters, tempC, r, preset, tidalForce);

        // ── 8. Hydrography ─────────────────────────────────────────────────────
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

        // ── 9. Crustal Metal Density (Modulated by Seismic/Volcanic/Tidal Flexure)
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

    public List<H3Cell> generatePlanet(PlanetPreset preset) {
        return generatePlanet(preset, null, null);
    }

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

        cells.parallelStream().forEach(cell -> {
            if (cancelSupplier != null && cancelSupplier.getAsBoolean()) {
                throw new java.util.concurrent.CancellationException("Planet generation cancelled by user");
            }
            PlanetPoint p = getPlanetPoint(cell.getLatitude(), cell.getLongitude(), preset);
            cell.setElevation(p.elevation());
            cell.setTemperature(p.temperature());
            cell.setRainfall(p.rainfall());
            cell.setBiome(p.biome());
            populateResources(cell, p, preset);

            int done = counter.incrementAndGet();
            if (progressCallback != null && (done % 50 == 0 || done == total)) {
                progressCallback.accept(done, total);
            }
        });

        if (cancelSupplier != null && cancelSupplier.getAsBoolean()) {
            throw new java.util.concurrent.CancellationException("Planet generation cancelled by user");
        }

        accumulateHydrographyFlow(cells, preset, cancelSupplier);

        return cells;
    }

    // -------------------------------------------------------------------------
    // Private: Physical Algorithms
    // -------------------------------------------------------------------------

    private double computeElevationAt(double lat, double lng,
                                       SimplexNoise noise, double freq, double scale) {
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
        // ── 1. Relative Surface Gravity & Dynamic Lapse Rate ───────────────────
        double rRel = Math.max(0.1, preset.radiusKm() / 6371.0);
        double gRel = preset.isSatellite() ? Math.max(0.05, rRel * 0.4) : Math.max(0.1, rRel);
        double lapseRateCPerKm = 6.5 * gRel;

        // ── 2. Rotation & Latitudinal Gradient Damping / Tidal Locking ─────────
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

        // ── 3. CO₂ Partial Pressure Greenhouse Forcing ────────────────────────
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

        // ── 4. Geothermal Heating from Internal Tidal Dissipation ─────────────
        double tidalGeothermalWarming = preset.isSatellite() ? Math.min(45.0, 10.0 * Math.sqrt(tidalForce)) : 0.0;

        // ── 5. Altitude Lapse Rate ─────────────────────────────────────────────
        double lapseCorrection = 0.0;
        if (elevMeters > 0) {
            lapseCorrection = (elevMeters / 1000.0) * lapseRateCPerKm;
        }

        // ── 6. Evaporative Cooling ─────────────────────────────────────────────
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

    private Biome determineBiome(double elevation, double elevMeters,
                                  double tempC, double rainfall, PlanetPreset preset, double tidalForce) {
        double waterLevel = preset.waterLevel();
        double pAtmo = preset.atmospherePressureAtm();

        // Airless Worlds
        if (pAtmo < 0.01) {
            if (tempC < -50.0) return Biome.SNOW;
            if (elevation > 0.6) return Biome.MOUNTAINS;
            if (elevation > 0.3) return Biome.HILLS;
            return Biome.DESERT;
        }

        // Ocean biomes
        if (elevation < waterLevel) {
            return (elevation < waterLevel - 0.40) ? Biome.DEEP_OCEAN : Biome.OCEAN;
        }

        // Coastal intertidal beach strip (width expands with gravitational tidal forces)
        double intertidalWidth = 0.03 + 0.02 * Math.min(3.0, tidalForce);
        if (elevation < waterLevel + intertidalWidth) {
            return Biome.BEACH;
        }

        if (tempC < -15.0 || elevation > 0.80) {
            return Biome.SNOW;
        }

        if (elevation > 0.55) return Biome.MOUNTAINS;
        if (elevation > 0.35) return Biome.HILLS;

        if (tempC < 0.0) return Biome.TUNDRA;
        if (rainfall < 0.20) return Biome.DESERT;

        if (tempC > 18.0 && rainfall > 0.70) return Biome.JUNGLE;
        if (rainfall > 0.50) return Biome.FOREST;

        return Biome.PLAINS;
    }

    private void populateResources(H3Cell cell, PlanetPoint p, PlanetPreset preset) {
        Biome b = cell.getBiome();
        if (b == null) return;

        double tidalForce = computeTidalForceIntensity(preset);

        if (preset.atmospherePressureAtm() < 0.01) {
            if (b == Biome.MOUNTAINS || b == Biome.HILLS) {
                cell.setResourceMetal(600.0 + p.metalDensity() * 800.0);
            } else {
                cell.setResourceMetal(250.0);
            }
            return;
        }

        // Ocean nutrient upwelling driven by tidal mixing increases fish biomass
        double tidalBiomassUpwelling = 1.0 + 0.3 * Math.min(3.0, tidalForce);

        switch (b) {
            case FOREST  -> cell.setWoodResource(1000.0);
            case JUNGLE  -> { cell.setWoodResource(2000.0); cell.setFoodResource(400.0); }
            case PLAINS  -> cell.setFoodResource(600.0);
            case HILLS   -> { cell.setFoodResource(200.0); cell.setWoodResource(400.0); }
            case MOUNTAINS -> {
                double metalBase = 400.0 + p.metalDensity() * 600.0;
                cell.setResourceMetal(metalBase);
            }
            case OCEAN, DEEP_OCEAN -> {
                cell.setBiomassFish(800.0 * tidalBiomassUpwelling);
                cell.setFoodResource(200.0 * tidalBiomassUpwelling);
            }
            case BEACH -> {
                cell.setFoodResource(150.0 * tidalBiomassUpwelling);
                cell.setBiomassFish(300.0 * tidalBiomassUpwelling);
            }
            case DESERT -> cell.setResourceMetal(200.0);
            default -> { }
        }
    }

    private void accumulateHydrographyFlow(List<H3Cell> cells, PlanetPreset preset, java.util.function.BooleanSupplier cancelSupplier) {
        if (cells == null || cells.isEmpty() || preset.atmospherePressureAtm() < 0.01) return;

        double waterLvl = preset.waterLevel();
        List<H3Cell> landCells = cells.stream()
                .filter(c -> c.getElevation() != null && c.getElevation() > waterLvl)
                .sorted((c1, c2) -> Double.compare(c2.getElevation(), c1.getElevation()))
                .toList();

        if (landCells.isEmpty()) return;

        Map<Long, H3Cell> cellMap = new java.util.HashMap<>(cells.size());
        for (H3Cell c : cells) {
            cellMap.put(c.getH3Index(), c);
        }

        Map<Long, Double> flowAccum = new java.util.concurrent.ConcurrentHashMap<>();
        for (H3Cell c : landCells) {
            flowAccum.put(c.getH3Index(), c.getRainfall() != null ? c.getRainfall() : 0.2);
        }

        H3Service h3Service = H3Service.getInstance();

        int step = 0;
        for (H3Cell c : landCells) {
            if (cancelSupplier != null && step++ % 100 == 0 && cancelSupplier.getAsBoolean()) {
                throw new java.util.concurrent.CancellationException("Hydrography flow calculation cancelled by user");
            }
            double curElev = c.getElevation();
            double curFlow = flowAccum.getOrDefault(c.getH3Index(), 0.2);

            H3Cell lowestNeighbor = null;
            double minElev = curElev;

            List<Long> neighborIndices = h3Service.getNeighbors(c.getH3Index());
            for (Long nIdx : neighborIndices) {
                H3Cell n = cellMap.get(nIdx);
                if (n != null && n.getElevation() != null && n.getElevation() < minElev) {
                    minElev = n.getElevation();
                    lowestNeighbor = n;
                }
            }

            if (lowestNeighbor != null && lowestNeighbor.getElevation() > waterLvl) {
                long nIdx = lowestNeighbor.getH3Index();
                flowAccum.put(nIdx, flowAccum.getOrDefault(nIdx, 0.2) + curFlow * 0.85);
            }
        }

        for (H3Cell c : landCells) {
            double rawFlow = flowAccum.getOrDefault(c.getH3Index(), 0.0);
            double normalizedRiver = Math.min(1.0, Math.max(0.0, Math.log1p(rawFlow) / 3.5));
            c.setWaterResource(normalizedRiver * 1000.0);

            double baseAquifer = c.getFreshwaterAquifer() != null ? c.getFreshwaterAquifer() : 200.0;
            c.setFreshwaterAquifer(Math.min(1000.0, Math.max(0.0, baseAquifer + normalizedRiver * 400.0)));
        }
    }
}
