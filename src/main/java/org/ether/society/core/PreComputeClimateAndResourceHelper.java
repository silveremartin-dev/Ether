/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.tier1.SeaLevelTransitionEngine;
import org.ether.society.model.Biome;
import org.ether.society.model.ClimateEvent;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Package-private helper computing initial paleoclimate forcings, Miami Net Primary Production (NPP),
 * McNaughton/Sala herbaceous pasture carrying capacity, biophysical resource stocks, and canonical
 * historical disaster schedules for {@link PreComputePhase}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see PreComputePhase
 */
final class PreComputeClimateAndResourceHelper {

    private static final Logger logger = LoggerFactory.getLogger(PreComputeClimateAndResourceHelper.class);

    private PreComputeClimateAndResourceHelper() {
        // Static package-private helper
    }

    /**
     * Computes climate (temperature, precipitation) and glaciological forcings for each cell.
     * Preserves climate if already initialized via configuration/raster maps, applying paleoclimate anomalies if requested.
     *
     * @param scenario active scenario configuration
     * @param cells    spatial H3 cell list
     */
    static void computeClimate(Scenario scenario, List<H3Cell> cells) {
        long year = scenario.getStartDateYear();

        boolean isGlacialMax = year <= -12000;
        String pKey = scenario.getPresetKey() != null ? scenario.getPresetKey().toLowerCase(Locale.ROOT) : "";
        String sName = scenario.getName() != null ? scenario.getName().toLowerCase(Locale.ROOT) : "";
        boolean isYoungerDryas = (year >= -10900 && year <= -9700)
                || pKey.contains("younger_dryas") || sName.contains("younger dryas") || sName.contains("dryas");
        boolean isGreenSahara = (year >= -8000 && year <= -5000)
                || pKey.contains("green_sahara") || sName.contains("green sahara") || sName.contains("sahara vert");

        boolean hasExistingClimate = cells.stream().anyMatch(c -> c != null && c.getTemperature() != null && c.getRainfall() != null);
        if (hasExistingClimate) {
            logger.info("Preserving existing climate & temperature data from configuration/raster maps");
            if (isGreenSahara || isYoungerDryas || isGlacialMax) {
                for (H3Cell cell : cells) {
                    if (cell == null) continue;
                    double lat = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
                    double lng = cell.getLongitude() != null ? cell.getLongitude() : 0.0;
                    if (isGreenSahara && lat >= 12.0 && lat <= 30.0 && lng >= -15.0 && lng <= 35.0) {
                        cell.setRainfall(Math.max(cell.getRainfall() != null ? cell.getRainfall() : 0.0, 1200.0));
                        if (cell.getBiome() == Biome.DESERT) cell.setBiome(Biome.SAVANNAH);
                    }
                    if (isYoungerDryas && lat >= 30.0 && lat <= 65.0 && lng >= -30.0 && lng <= 45.0) {
                        if (cell.getTemperature() != null) cell.setTemperature(cell.getTemperature() - 5.5);
                    }
                    if (isGlacialMax) {
                        cell.setSeaLevelOffsetMeters(-120.0);
                    }
                }
                if (isGlacialMax) {
                    for (H3Cell cell : cells) {
                        SeaLevelTransitionEngine.applySeaLevelTransitionCell(cell, -120.0);
                    }
                }
            }
            return;
        }

        logger.info("Computing climate and glaciological forcings procedurally...");
        double axialTilt = scenario.getAxialTiltDegrees();

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double lat = cell.getLatitude() != null ? cell.getLatitude() : 0.0;
            double lng = cell.getLongitude() != null ? cell.getLongitude() : 0.0;
            double elev = cell.getElevation() != null ? cell.getElevation() : 0.0;

            double latFactor = Math.abs(lat) / 90.0;
            double baseTemp = 30.0 - (latFactor * 50.0 * (axialTilt / 23.5));

            if (isGlacialMax) {
                baseTemp -= 6.0;
                cell.setSeaLevelOffsetMeters(-120.0);
                boolean isLand = elev > 0 && cell.getBiome() != Biome.OCEAN && cell.getBiome() != Biome.DEEP_OCEAN;
                if (isLand) {
                    boolean isLaurentide = (lat > 54.0 && lng >= -160.0 && lng <= -55.0);
                    boolean isFennoscandian = (lat > 58.0 && lng >= 5.0 && lng <= 50.0);
                    boolean isGreenlandIceland = (lat > 64.0 && lng >= -55.0 && lng < 5.0);
                    if (isLaurentide || isFennoscandian || isGreenlandIceland) {
                        double baseLat = isLaurentide ? 54.0 : (isFennoscandian ? 58.0 : 64.0);
                        double iceMeters = Math.min(2000.0, (lat - baseLat) * 120.0);
                        cell.setIceSheetThicknessMeters(iceMeters);
                        cell.setDynamicAlbedo(0.80);
                        cell.setBiome(Biome.GLACIER);
                        baseTemp -= (iceMeters * 0.005);
                    }
                }
            } else {
                cell.setSeaLevelOffsetMeters(0.0);
                cell.setIceSheetThicknessMeters(0.0);
            }

            if (isYoungerDryas && lat >= 30.0 && lat <= 65.0 && lng >= -30.0 && lng <= 45.0) {
                baseTemp -= 5.5;
            }
            if (isGreenSahara && lat >= 12.0 && lat <= 30.0 && lng >= -15.0 && lng <= 35.0) {
                baseTemp = Math.min(baseTemp, 27.0);
            }

            double elevEffect = -Math.max(0.0, elev) * 0.0065; // Standard tropospheric lapse rate (-6.5 K/km)
            double harshness = scenario.getClimateHarshness();
            double insolationFactor = Math.cos(Math.toRadians(lat));
            double tempVariation = (harshness - 1.0) * (insolationFactor - 0.5) * 6.0;

            double finalTemp = baseTemp + elevEffect + tempVariation;
            cell.setTemperature(finalTemp);

            double absLat = Math.abs(lat);
            double itczPrecip = 1800.0 * Math.exp(-Math.pow(lat / 14.0, 2.0));
            double midLatPrecip = 850.0 * Math.exp(-Math.pow((absLat - 50.0) / 16.0, 2.0));
            double orographicLift = Math.clamp(Math.max(0.0, elev) * 0.12, 0.0, 450.0);
            double basePrecip = 120.0 + itczPrecip + midLatPrecip + orographicLift;

            if (isGreenSahara && lat >= 12.0 && lat <= 30.0 && lng >= -15.0 && lng <= 35.0) {
                basePrecip = 1200.0;
                cell.setBiome(Biome.SAVANNAH);
            } else if (isYoungerDryas && lat >= 28.0 && lat <= 38.0 && lng >= 30.0 && lng <= 45.0) {
                basePrecip = 250.0;
            }

            if (cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) {
                basePrecip = 0;
                if (Math.abs(lat) > 72.0 || finalTemp < -2.0) {
                    cell.setBiome(Biome.GLACIER);
                }
            } else if (Math.abs(lat) > 75.0 || finalTemp < -15.0) {
                cell.setBiome(Biome.GLACIER);
            }
            cell.setRainfall(Math.max(0, basePrecip));
        }

        double seaOffset = isGlacialMax ? -120.0 : 0.0;
        for (H3Cell cell : cells) {
            SeaLevelTransitionEngine.applySeaLevelTransitionCell(cell, seaOffset);
        }
    }

    /**
     * Computes Net Primary Productivity (NPP) in $\text{g dry matter/m}^2\text{/yr}$ using the Miami Model (Lieth 1975):
     * $$\text{NPP}_T(T) = \frac{3000}{1 + \exp(1.315 - 0.119 \cdot T)}, \quad \text{NPP}_P(P) = 3000 \cdot (1 - \exp(-0.000664 \cdot P))$$
     * $$\text{NPP}(T, P) = \min(\text{NPP}_T(T), \text{NPP}_P(P))$$
     *
     * @param temp   mean annual temperature in $^\circ\text{C}$
     * @param precip annual precipitation in $\text{mm}$
     * @return NPP in $\text{g dry matter/m}^2\text{/yr}$ (metric tons / $\text{km}^2$ / yr)
     */
    static double calculateMiamiNPP(double temp, double precip) {
        if (temp <= -10.0 || precip <= 5.0) return 0.0;
        double nppT = 3000.0 / (1.0 + Math.exp(1.315 - 0.119 * temp));
        double nppP = 3000.0 * (1.0 - Math.exp(-0.000664 * precip));
        return Math.max(0.0, Math.min(nppT, nppP));
    }

    /**
     * Calculates herbaceous pasture carrying capacity for wild grazing herbivores based on
     * McNaughton (1985) and Sala et al. (1988).
     *
     * @param temp      surface temperature in $^\circ\text{C}$
     * @param rain      precipitation in $\text{mm}$
     * @param elev      elevation in meters
     * @param areaScale spatial scaling factor relative to base H3 resolution 3
     * @return wild livestock carrying capacity in metric tons
     */
    static double calculateHerbaceousPastureCarryingCapacity(double temp, double rain, double elev, double areaScale) {
        if (elev < 0.0 || temp < -20.0 || rain < 20.0) return 0.0;
        double npp = calculateMiamiNPP(temp, rain);
        double forestCanopy = (1.0 / (1.0 + Math.exp(-0.006 * (rain - 450.0))))
                * (1.0 / (1.0 + Math.exp(-0.25 * (temp - 2.0))));
        double herbaceousFraction = (1.0 - forestCanopy) * (rain / (rain + 100.0));
        return Math.clamp(npp * 0.35 * herbaceousFraction * areaScale, 0.0, 600.0 * areaScale);
    }

    /**
     * Initializes resource stockpiles based on biome type, coastal marine upwelling, and Miami NPP.
     * Preserves existing resources if already populated via configuration/rasters.
     *
     * @param scenario active scenario configuration
     * @param cells    spatial H3 cell list
     */
    static void initializeResources(Scenario scenario, List<H3Cell> cells) {
        java.util.Set<Long> oceanIndices = new java.util.HashSet<>();
        for (H3Cell c : cells) {
            if (c != null && c.getH3Index() != null && c.getH3Index() != 0L) {
                double e = c.getElevationOr(0.0);
                Biome b = c.getBiome();
                if (e <= 0.0 || b == Biome.OCEAN || b == Biome.DEEP_OCEAN) {
                    oceanIndices.add(c.getH3Index());
                }
            }
        }

        org.ether.society.h3.H3Service h3Service = null;
        try {
            h3Service = org.ether.society.h3.H3Service.getInstance();
        } catch (Exception ex) {
            logger.debug("H3Service instance unavailable during coastal cell identification: {}", ex.getMessage());
        }

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            Biome biome = cell.getBiome() != null ? cell.getBiome() : Biome.PLAINS;
            boolean coastal = Boolean.TRUE.equals(cell.getIsCoastal()) || biome == Biome.BEACH;
            if (!coastal && h3Service != null && cell.getH3Index() != null && cell.getH3Index() != 0L && !oceanIndices.contains(cell.getH3Index())) {
                try {
                    List<Long> neighbors = h3Service.gridDisk(cell.getH3Index(), 1);
                    for (Long n : neighbors) {
                        if (oceanIndices.contains(n)) {
                            coastal = true;
                            break;
                        }
                    }
                } catch (Exception ex) {
                    logger.debug("Failed neighbor inspection for H3 index {}: {}", cell.getH3Index(), ex.getMessage());
                }
            }
            if (!coastal && (cell.getElevation() != null && cell.getElevation() > 0 && cell.getElevation() < 50.0 && cell.getCoastalMarineResourceOr(0.0) > 0.0)) {
                coastal = true;
            }
            cell.setIsCoastal(coastal);
            if (coastal) {
                if (cell.getCoastalMarineResource() == null || cell.getCoastalMarineResource() == 0.0) {
                    double bathyElevation = cell.getElevationOr(0.0);
                    double shelfUpwelling = 800.0 + (1.0 - Math.clamp(bathyElevation / 150.0, 0.0, 1.0)) * 200.0;
                    cell.setCoastalMarineResource(shelfUpwelling);
                }
                if (cell.getBiomassFish() == null || cell.getBiomassFish() == 0.0) {
                    cell.setBiomassFish(1200.0);
                }
            } else if (biome == Biome.LAKE && (cell.getBiomassFish() == null || cell.getBiomassFish() == 0.0)) {
                cell.setBiomassFish(800.0);
            }
        }

        boolean hasExistingResources = cells.stream().anyMatch(c -> c != null && ((c.getFoodResource() != null && c.getFoodResource() > 0)
                || (c.getWaterResource() != null && c.getWaterResource() > 0)));

        if (hasExistingResources) {
            logger.info("Preserving existing resource distributions (Food, Water, Wood, Minerals) from configuration/raster maps");
            for (H3Cell cell : cells) {
                if (cell == null) continue;
                if (cell.getBiomassNatural() == null && cell.getFoodResource() != null) {
                    cell.setBiomassNatural(cell.getFoodResource());
                }
                if (cell.getBiomassLivestock() == null || cell.getBiomassLivestock() <= 0.0) {
                    double areaKm2 = cell.getEffectiveSurfaceAreaKm2() > 0 ? cell.getEffectiveSurfaceAreaKm2() : 1770.49;
                    double areaScale = Math.max(0.05, areaKm2 / 1770.49);
                    double wildPasture = calculateHerbaceousPastureCarryingCapacity(
                            cell.getTemperatureOr(15.0), cell.getRainfallOr(500.0), cell.getElevationOr(0.0), areaScale);
                    int p = cell.getPopulation() != null ? cell.getPopulation() : 0;
                    double kPerCapita = cell.getResourceCapitalOr(0.0) / Math.max(1.0, (double) p);
                    // Smooth C^1 Neolithic domestication envelope: activates above Paleolithic somatic tool stock (k_pc >= 15 kg/cap)
                    double neolithicShare = 1.0 / (1.0 + Math.exp(-(kPerCapita - 15.0) / 5.0));
                    double domesticLivestock = (p > 0 && kPerCapita >= 6.0) ? (p * 0.25 * neolithicShare) : 0.0;
                    cell.setBiomassLivestock(Math.max(wildPasture, domesticLivestock));
                }
                if (cell.getBiomassAgriculture() == null || cell.getBiomassAgriculture() == 0.0) {
                    int p = cell.getPopulation() != null ? cell.getPopulation() : 0;
                    double kPerCapita = cell.getResourceCapitalOr(0.0) / Math.max(1.0, (double) p);
                    double neolithicShare = 1.0 / (1.0 + Math.exp(-(kPerCapita - 15.0) / 5.0));
                    double agrYieldBonus = 1.0 + 0.05 * Math.log1p(Math.max(0.0, kPerCapita - 15.0) / 25.0);
                    cell.setBiomassAgriculture((p > 0 && kPerCapita >= 6.0) ? (p * 420.0 * neolithicShare * agrYieldBonus) : 0.0);
                }
            }
            return;
        }

        logger.info("Initializing fallback resources via Miami NPP & physical hydrology models (Zero Random)...");

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            Biome biome = cell.getBiome() != null ? cell.getBiome() : Biome.PLAINS;
            double elev = cell.getElevationOr(0.0);
            double temp = cell.getTemperatureOr(15.0);
            double rain = cell.getRainfallOr(150.0);
            double areaKm2 = cell.getEffectiveSurfaceAreaKm2() > 0 ? cell.getEffectiveSurfaceAreaKm2() : 1770.49;
            double areaScale = Math.max(0.05, areaKm2 / 1770.49);

            double npp = calculateMiamiNPP(temp, rain);

            double wildFood;
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.GLACIER) {
                wildFood = 0.0;
            } else {
                double totalPrimaryEnergyGJ = npp * areaKm2 * 17.5;
                wildFood = Math.clamp(totalPrimaryEnergyGJ * 0.008, 10.0, 4500.0 * areaScale);
                if (biome == Biome.DESERT) wildFood = Math.min(wildFood, 60.0 * areaScale);
            }

            cell.setFoodResource(wildFood);
            double standingWildBiomassTons = (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.GLACIER)
                    ? 0.0
                    : Math.clamp(npp * areaKm2 * 0.12, 150.0 * areaScale, 9000.0 * areaScale);
            cell.setBiomassNatural(standingWildBiomassTons);

            double wildLivestock = calculateHerbaceousPastureCarryingCapacity(temp, rain, elev, areaScale);
            int p = cell.getPopulationOr(0);
            double kPerCapita = cell.getResourceCapitalOr(0.0) / Math.max(1.0, (double) p);
            double neolithicShare = 1.0 / (1.0 + Math.exp(-(kPerCapita - 15.0) / 5.0));
            double domesticLivestock = (p > 0 && kPerCapita >= 6.0) ? (p * 0.25 * neolithicShare) : 0.0;
            cell.setBiomassLivestock(Math.max(wildLivestock, domesticLivestock));
            double agrYieldBonus = 1.0 + 0.05 * Math.log1p(Math.max(0.0, kPerCapita - 15.0) / 25.0);
            cell.setBiomassAgriculture((p > 0 && kPerCapita >= 6.0) ? (p * 420.0 * neolithicShare * agrYieldBonus) : 0.0);

            double wood;
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.GLACIER || biome == Biome.DESERT) {
                wood = 0.0;
            } else {
                // Woody biomass residence time (Olson et al. 1983; Whittaker & Likens 1973)
                double forestAffinity = (1.0 / (1.0 + Math.exp(-0.006 * (rain - 450.0))))
                        * (1.0 / (1.0 + Math.exp(-0.25 * (temp - 2.0))));
                double residenceYears = 35.0 * forestAffinity + 5.0 * (1.0 - forestAffinity) * (rain / (rain + 250.0));
                double standingWoodTons = (npp * 0.60 * residenceYears) * areaScale;
                wood = Math.clamp(standingWoodTons * 0.08, 0.0, 8000.0 * areaScale);
            }
            cell.setWoodResource(wood);

            double water;
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.GLACIER) {
                water = 0.0;
            } else {
                double aquifer = cell.getAccessibleAquiferOr(0.0);
                water = Math.clamp(rain + aquifer * 350.0, 10.0, 2500.0);
            }
            cell.setWaterResource(water);
        }
    }

    /**
     * Injects canonical empirical historical disasters into the scenario if not already present.
     *
     * @param scenario scenario to inspect and populate
     */
    static void ensureCanonicalHistoricalEvents(Scenario scenario) {
        if (scenario == null) return;
        boolean isEarth = scenario.getPlanetPreset() == null
                || "earth".equalsIgnoreCase(scenario.getPlanetPreset().getCanonicalPlanet())
                || "earth".equalsIgnoreCase(scenario.getPlanetPreset().elevationMapSource());
        if (!isEarth) return;

        long startYear = scenario.getStartDateYear();
        long endYear = scenario.getEndDateYear() > 0 ? scenario.getEndDateYear() : startYear + 1000;

        List<ClimateEvent> events = scenario.getClimateEvents();
        if (events == null) {
            events = new ArrayList<>();
            scenario.setClimateEvents(events);
        }

        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Éruption Minoenne de Théra (Santorin)", -1610, 36.4, 25.4, 10.0, 0.95, 365));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Éruption du Vésuve (Pompéi & Herculanum)", 79, 40.8, 14.4, 8.0, 0.85, 365));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("PANDEMIC", "Peste Antonine (Variole dans l'Empire Romain)", 165, 41.9, 12.5, 0.0, 0.85, 730));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Année sans été (Éruption mystère de 536)", 536, 64.0, -18.0, 10.0, 0.90, 365));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("PANDEMIC", "Peste de Justinien (Yersinia Pestis)", 541, 41.0, 29.0, 0.0, 0.90, 730));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Éruption du Samalas (Lombok, Indonésie)", 1257, -8.4, 116.4, 12.0, 0.98, 365));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("PANDEMIC", "Peste Noire (Yersinia Pestis)", 1347, 45.0, 35.0, 0.0, 0.90, 730));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Éruption du Laki (Islande)", 1783, 64.0, -18.2, 8.0, 0.88, 365));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Éruption du Tambora (Année sans été)", 1815, -8.2, 118.0, 10.0, 0.95, 365));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Explosion du Krakatoa (Détroit de la Sonde)", 1883, -6.1, 105.4, 10.0, 0.90, 365));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("PANDEMIC", "Grippe Espagnole (H1N1)", 1918, 39.0, -98.0, 0.0, 0.75, 730));
    }

    private static void addEventIfInRange(List<ClimateEvent> events, long startYear, long endYear, ClimateEvent ev) {
        if (ev.getYear() >= startYear && ev.getYear() <= endYear) {
            boolean exists = events.stream().anyMatch(e -> e.getName().equalsIgnoreCase(ev.getName()) && e.getYear() == ev.getYear());
            if (!exists) {
                events.add(ev);
            }
        }
    }
}
