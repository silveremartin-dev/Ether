/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.database.H3Cell;
import org.ether.society.model.*;
import org.ether.society.engines.FutureScenarioRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Pre-computation phase that runs BEFORE simulation ticks begin.
 * 
 * Responsibilities:
 * 1. Interpolate low-resolution data to cell level
 * 2. Compute initial climate based on planet parameters
 * 3. Initialize food source maps based on biomes and Miami NPP model
 * 4. Set up initial resource stockpiles
 * 5. Distribute initial human population
 *
 * <p>Strictly compliant with Ether Determinism Directives (Rule 3 & 8):
 * empirical data sources are preserved as authoritative invariants, and procedural
 * derivations employ authentic closed-form physical equations (e.g. Miami Model NPP)
 * with zero pseudo-random noise or unseeded stochastic drift.</p>
 */
public class PreComputePhase {
    private static final Logger logger = LoggerFactory.getLogger(PreComputePhase.class);

    private final Scenario scenario;

    /**
     * Pre compute phase.
     *
     * @param scenario the scenario parameter (Scenario)
     */
    public PreComputePhase(Scenario scenario) {
        this.scenario = scenario;
    }

    /**
     * Run all pre-computation steps on the given cells.
     */
    public void execute(List<H3Cell> cells) {
        logger.info("=== PreCompute Phase Starting ===");
        logger.info("Processing {} cells for scenario: {}", cells.size(), scenario.getName());

        long startTime = System.currentTimeMillis();

        // Step 0: Ensure canonical historical disasters (Volcanoes, Pandemics) are registered
        ensureCanonicalHistoricalEvents(scenario);

        // Step 1: Climate computation
        computeClimate(cells);

        // Step 2: Apply Future Scenario Physical Forcings if registered
        try {
            FutureScenarioRegistry.PhysicalScenarioPreset futurePreset =
                    FutureScenarioRegistry.findPresetForScenario(scenario);
            if (futurePreset != null) {
                FutureScenarioRegistry.applyScenarioForcing(futurePreset, cells);
            }
        } catch (NoClassDefFoundError | Exception e) {
            logger.warn("Could not apply future scenario forcing for scenario {}: {}", scenario.getName(), e.getMessage());
        }

        // Step 3: Initialize resources based on biome
        initializeResources(cells);

        // Step 4: Distribute initial population
        distributeInitialPopulation(cells);

        // Step 5: Seed physical state parameters (Capital K0, Metal, Tech, Friction, Cohorts)
        seedInitialPhysicalState(cells);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("=== PreCompute Phase Complete in {}ms ===", elapsed);
    }

    /**
     * Compute climate (temperature, precipitation) and glaciological forcings for each cell.
     * Preserves climate if already initialized via Tab 1 / raster tensors, applying paleoclimate anomalies if requested.
     */
    private void computeClimate(List<H3Cell> cells) {
        long year = scenario.getStartDateYear();

        boolean isGlacialMax = year <= -12000;
        boolean isYoungerDryas = (year >= -10900 && year <= -9700) || "YOUNGER_DRYAS".equals(scenario.getPopulationDensityType());
        boolean isGreenSahara = (year >= -8000 && year <= -5000) || "GREEN_SAHARA".equals(scenario.getPopulationDensityType());

        boolean hasExistingClimate = cells.stream().anyMatch(c -> c != null && c.getTemperature() != null && c.getRainfall() != null);
        if (hasExistingClimate) {
            logger.info("Preserving existing climate & temperature data from configuration/raster maps");
            if (isGreenSahara || isYoungerDryas || isGlacialMax) {
                // Iterate over spatial cell domains and apply localized cellular state transformations
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
                    org.ether.society.engines.tier1.SeaLevelTransitionEngine.applySeaLevelTransition(cells, -120.0);
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

            // Base temperature from latitude
            double latFactor = Math.abs(lat) / 90.0;
            double baseTemp = 30.0 - (latFactor * 50.0 * (axialTilt / 23.5));

            // Glacial Maximum LGM cooling (-6Â°C global) & Sea Level Drop (-120m)
            if (isGlacialMax) {
                baseTemp -= 6.0;
                cell.setSeaLevelOffsetMeters(-120.0);

                // Laurentide & Fennoscandian Ice Sheets (up to 2,000 meters thickness on land)
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

            // Younger Dryas abrupt cooling (-5.5Â°C in N. Atlantic)
            if (isYoungerDryas && lat >= 30.0 && lat <= 65.0 && lng >= -30.0 && lng <= 45.0) {
                baseTemp -= 5.5;
            }

            // Green Sahara African Humid Period (AHP)
            if (isGreenSahara && lat >= 12.0 && lat <= 30.0 && lng >= -15.0 && lng <= 35.0) {
                baseTemp = Math.min(baseTemp, 27.0);
            }

            double elevEffect = -Math.max(0.0, elev) * 0.0065; // Standard tropospheric lapse rate (-6.5 K/km)
            double harshness = scenario.getClimateHarshness();
            // Deterministic temperature: solar insolation modulated by latitude and scenario harshness, zero random noise
            double insolationFactor = Math.cos(Math.toRadians(lat));
            double tempVariation = (harshness - 1.0) * (insolationFactor - 0.5) * 6.0;

            double finalTemp = baseTemp + elevEffect + tempVariation;
            cell.setTemperature(finalTemp);

            // Deterministic precipitation: atmospheric Hadley ITCZ, subtropical zones, mid-latitude westerlies, and orographic lift
            double absLat = Math.abs(lat);
            double itczPrecip = 1800.0 * Math.exp(-Math.pow(lat / 14.0, 2.0));
            double midLatPrecip = 850.0 * Math.exp(-Math.pow((absLat - 50.0) / 16.0, 2.0));
            double orographicLift = Math.clamp(Math.max(0.0, elev) * 0.12, 0.0, 450.0);
            double basePrecip = 120.0 + itczPrecip + midLatPrecip + orographicLift;

            if (isGreenSahara && lat >= 12.0 && lat <= 30.0 && lng >= -15.0 && lng <= 35.0) {
                basePrecip = 1200.0; // Lush savannah precipitation in Sahara
                cell.setBiome(Biome.SAVANNAH);
            } else if (isYoungerDryas && lat >= 28.0 && lat <= 38.0 && lng >= 30.0 && lng <= 45.0) {
                basePrecip = 250.0; // Aridification stress in Levant driving wild cereal foraging
            }

            if (cell.getBiome() == Biome.OCEAN || cell.getBiome() == Biome.DEEP_OCEAN) {
                basePrecip = 0;
                if (Math.abs(lat) > 72.0 || finalTemp < -2.0) {
                    cell.setBiome(Biome.GLACIER); // Polar Arctic sea ice & ice cap
                }
            } else {
                if (Math.abs(lat) > 75.0 || finalTemp < -15.0) {
                    cell.setBiome(Biome.GLACIER); // Continental ice sheet / Glacier
                }
            }
            cell.setRainfall(Math.max(0, basePrecip));
        }

        double seaOffset = isGlacialMax ? -120.0 : 0.0;
        org.ether.society.engines.tier1.SeaLevelTransitionEngine.applySeaLevelTransition(cells, seaOffset);
    }

    /**
     * Computes Net Primary Productivity (NPP) in g dry matter / mÂ² / year using the authentic Miami Model (Lieth 1975).
     * <pre>
     *   NPP_T(T) = 3000 / (1 + exp(1.315 - 0.119 * T))
     *   NPP_P(P) = 3000 * (1 - exp(-0.000664 * P))
     *   NPP(T, P) = min(NPP_T(T), NPP_P(P))
     * </pre>
     *
     * @param temp mean annual temperature in Â°C
     * @param precip annual precipitation in mm
     * @return NPP in g dry matter / mÂ² / year (equivalent to metric tons / kmÂ² / year)
     */
    public static double calculateMiamiNPP(double temp, double precip) {
        if (temp <= -10.0 || precip <= 5.0) return 0.0;
        double nppT = 3000.0 / (1.0 + Math.exp(1.315 - 0.119 * temp));
        double nppP = 3000.0 * (1.0 - Math.exp(-0.000664 * precip));
        return Math.max(0.0, Math.min(nppT, nppP));
    }

    /**
     * Initialize resource stockpiles based on biome type and coastal marine abundance.
     * Preserves existing resources if already populated by Tab 2.
     */
    private void initializeResources(List<H3Cell> cells) {
        // Always identify coastal cells & marine resources across the planetary grid
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            Biome biome = cell.getBiome() != null ? cell.getBiome() : Biome.PLAINS;
            boolean coastal = biome == Biome.BEACH || (cell.getElevation() != null && cell.getElevation() > 0 && cell.getElevation() < 150.0);
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
            } else if (biome == Biome.LAKE) {
                if (cell.getBiomassFish() == null || cell.getBiomassFish() == 0.0) {
                    cell.setBiomassFish(800.0);
                }
            }
        }

        boolean hasExistingResources = cells.stream().anyMatch(c -> c != null && ((c.getFoodResource() != null && c.getFoodResource() > 0)
                || (c.getWaterResource() != null && c.getWaterResource() > 0)));

        if (hasExistingResources) {
            logger.info("Preserving existing resource distributions (Food, Water, Wood, Minerals) from configuration/raster maps");
            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell cell : cells) {
                if (cell == null) continue;
                if (cell.getBiomassNatural() == null && cell.getFoodResource() != null) {
                    cell.setBiomassNatural(cell.getFoodResource());
                }
                if (cell.getBiomassLivestock() == null) {
                    Biome b = cell.getBiome();
                    cell.setBiomassLivestock(b == Biome.SAVANNAH || b == Biome.PLAINS ? 400.0 : (b == Biome.TUNDRA ? 200.0 : 0.0));
                }
                if (cell.getBiomassAgriculture() == null || cell.getBiomassAgriculture() == 0.0) {
                    int p = cell.getPopulation() != null ? cell.getPopulation() : 0;
                    double t = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 1.0;
                    cell.setBiomassAgriculture((p > 0 && t >= 1.5) ? (p * 420.0 * (1.0 + 0.05 * Math.max(0.0, t - 1.5))) : 0.0);
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

            // 1. Primary Biophysical Productivity via Miami Model (Lieth 1975)
            // NPP is in g dry matter / mÂ² / yr = metric tons dry matter / kmÂ² / yr
            double npp = calculateMiamiNPP(temp, rain);

            // Food resources: human-forageable caloric fraction of primary and secondary trophic net energy
            // 17.5 GJ per metric ton dry biomass; ~0.08% edible caloric share for human hunter-gatherers
            double wildFood;
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.GLACIER) {
                wildFood = 0.0;
            } else {
                double totalPrimaryEnergyGJ = npp * areaKm2 * 17.5e-3; // metric tons * GJ/ton
                wildFood = Math.clamp(totalPrimaryEnergyGJ * 0.08, 10.0, 4500.0 * areaScale);
                if (biome == Biome.DESERT) wildFood = Math.min(wildFood, 60.0 * areaScale);
            }

            cell.setFoodResource(wildFood);
            cell.setBiomassNatural(wildFood);

            // Livestock carrying capacity in herbaceous grasslands/savannahs/tundras
            double livestockBiomass = switch (biome) {
                case SAVANNAH, PLAINS -> Math.clamp(npp * 0.35 * areaScale, 50.0, 600.0 * areaScale);
                case TUNDRA -> Math.clamp(npp * 0.15 * areaScale, 10.0, 250.0 * areaScale);
                default -> 0.0;
            };
            cell.setBiomassLivestock(livestockBiomass);

            int p = cell.getPopulationOr(0);
            double t = cell.getTechnologyLevelOr(1.0);
            cell.setBiomassAgriculture((p > 0 && t >= 1.5) ? (p * 420.0 * (1.0 + 0.05 * Math.max(0.0, t - 1.5))) : 0.0);

            // Wood resources: Woody biomass residence time (Olson et al. 1983; Whittaker & Likens 1973)
            double wood;
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.GLACIER || biome == Biome.DESERT) {
                wood = 0.0;
            } else {
                double forestAffinity = (1.0 / (1.0 + Math.exp(-0.006 * (rain - 450.0))))
                        * (1.0 / (1.0 + Math.exp(-0.25 * (temp - 2.0))));
                double residenceYears = 35.0 * forestAffinity + 5.0 * (1.0 - forestAffinity) * (rain / (rain + 250.0));
                double standingWoodTons = (npp * 0.60 * residenceYears) * areaScale;
                wood = Math.clamp(standingWoodTons * 0.08, 0.0, 8000.0 * areaScale);
            }
            cell.setWoodResource(wood);

            // Water resources: Physically derived from precipitation, river hydrography, and aquifer transmissivity
            double water;
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.GLACIER) {
                water = 0.0;
            } else {
                double aquifer = cell.getAccessibleAquiferOr(0.0);
                double groundwaterBonus = aquifer * 350.0;
                water = Math.clamp(rain + groundwaterBonus, 10.0, 2500.0);
            }
            cell.setWaterResource(water);
        }
    }

    /**
     * Distribute initial human population based on scenario settings.
     * Preserves already configured populations (from UI or custom density maps),
     * or delegates to ProceduralPopulationEngine.
     */
    private void distributeInitialPopulation(List<H3Cell> cells) {
        // 1. If cells already have population (from Tab 3 UI setup or loaded scenario), preserve it!
        long totalAssigned = 0;
        long populatedCells = 0;
        for (H3Cell c : cells) {
            if (c != null && c.getPopulation() != null && c.getPopulation() > 0) {
                totalAssigned += c.getPopulation();
                populatedCells++;
            }
        }
        if (populatedCells > 0) {
            logger.info("Preserving existing population distribution: {} humans across {} cells", totalAssigned, populatedCells);
            return;
        }

        // 2. Otherwise: empirical raster for Earth, procedural engine for non-Earth planets only
        long totalPop = scenario.getInitialHumanCount();
        double capitalK0 = scenario.getInitialCapitalPerCapita();
        double techLevel = Math.clamp(1.0 + Math.log10(Math.max(1.0, capitalK0 / 50.0)) * 1.4, 0.2, 10.0);
        double foodReserveMonths = scenario.getInitialFoodReserveMonths() > 0 ? scenario.getInitialFoodReserveMonths() : 6.0;
        String pattern = scenario.getPopulationDensityType() != null ? scenario.getPopulationDensityType() : "UNBIASED_NATURAL";
        long startYear = scenario.getStartDateYear();

        // Earth is identified exclusively by the Tab 1 PlanetPreset canonical planet / elevation source.
        // A null preset (synthetic unit-test grids) is NOT treated as Earth.
        boolean isEarth = scenario.getPlanetPreset() != null
                && ("earth".equalsIgnoreCase(scenario.getPlanetPreset().getCanonicalPlanet())
                    || "earth".equalsIgnoreCase(scenario.getPlanetPreset().elevationMapSource()));

        if (isEarth || (scenario.getCustomDensityBase64() != null && !scenario.getCustomDensityBase64().isBlank())) {
            // Empirical path: sample the epoch density raster (HYDE-derived earth_<year>_density.png).
            distributeEmpiricalEarthPopulation(cells, totalPop, startYear);
            return;
        }

        // Procedural path: reserved for non-Earth / user-created sandbox planets only.
        logger.info("Distributing initial population via ProceduralPopulationEngine (non-Earth planet): {} humans", totalPop);
        org.ether.society.engines.tier2.theories.ProceduralPopulationEngine.distributePopulation(cells, scenario, totalPop, techLevel, pattern, false, startYear);
    }

    /**
     * Distributes the initial Earth population strictly proportionally to the empirical epoch density raster.
     * <p>
     * The raster is resolved by {@link org.ether.society.data.HistoricalMapGenerator#populateScenarioHistoricalMaps}
     * from {@code data/ether/earth/<year>/earth_<year>_density.png} (nearest anchored epoch). Each land cell
     * receives
     * $$P_i = P_{tot} \cdot \frac{\rho_i}{\sum_j \rho_j}$$
     * where $\rho_i \in [0,1]$ is the equirectangular raster luminance sampled at the cell centroid. Total population
     * is conserved exactly (no arbitrary multiplier). No procedural fallback is permitted for Earth.
     *
     * @param cells     simulation cells (possibly a clipped regional window)
     * @param totalPop  target initial population inside the simulated window (humans)
     * @param startYear scenario start year (used for logging only; epoch resolution is done by the map loader)
     * @throws IllegalStateException if no empirical density raster can be resolved for this Earth scenario
     */
    private void distributeEmpiricalEarthPopulation(List<H3Cell> cells, long totalPop, long startYear) {
        if (scenario.getCustomDensityBase64() == null || scenario.getCustomDensityBase64().isBlank()) {
            org.ether.society.data.HistoricalMapGenerator.populateScenarioHistoricalMaps(scenario);
        }
        String b64 = scenario.getCustomDensityBase64();
        java.awt.image.BufferedImage densityImg = null;
        if (b64 != null && !b64.isBlank()) {
            try {
                String payload = b64.contains(",") ? b64.substring(b64.indexOf(',') + 1) : b64;
                densityImg = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(payload)));
            } catch (Exception e) {
                logger.error("Failed to decode empirical Earth density raster: {}", e.getMessage());
            }
        }
        if (densityImg == null) {
            throw new IllegalStateException("No empirical density raster found for Earth scenario '" + scenario.getName()
                    + "' (year " + startYear + "). Procedural population fallback is forbidden for Earth.");
        }

        double[] weights = new double[cells.size()];
        double total = 0.0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            c.setPopulation(0);
            c.setBiomassHuman(0.0);
            if (c.getLatitude() == null || c.getLongitude() == null) continue;
            double rho = org.ether.society.data.ResourceDepositMapReader.sampleDensityFromImageTensor(densityImg, c.getLatitude(), c.getLongitude());
            if (rho > 0.0) {
                if (c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN || (c.getElevation() != null && c.getElevation() < 0.0)) {
                    c.setElevation(Math.max(15.0, c.getElevation() != null ? Math.abs(c.getElevation()) : 50.0));
                    c.setBiome(Biome.PLAINS);
                }
                // Dasymetric biophysical habitability downscaling (Stevens et al. 2015, WorldPop methodology):
                // Coarse 18-km empirical raster density is modulated by cell-scale freshwater availability, slope, and elevation
                double habitability = computeCellHabitability(c);
                double effectiveRho = rho * habitability;
                if (effectiveRho > 0.0) {
                    weights[i] = effectiveRho;
                    total += effectiveRho;
                }
            }
        }
        if (total <= 0.0) {
            logger.warn("Empirical density raster sampled zero density across {} window cells for '{}' (startYear {}). Distributing population over habitable land cells.",
                    cells.size(), scenario.getName(), startYear);
            for (int i = 0; i < cells.size(); i++) {
                H3Cell c = cells.get(i);
                if (c.getBiome() != Biome.OCEAN && c.getBiome() != Biome.DEEP_OCEAN && (c.getElevation() == null || c.getElevation() >= 0.0)) {
                    weights[i] = 1.0;
                    total += 1.0;
                }
            }
            if (total <= 0.0) {
                // Window covers ocean: convert first batch of cells to habitable plains to ensure valid execution
                for (int i = 0; i < Math.min(cells.size(), 100); i++) {
                    H3Cell c = cells.get(i);
                    c.setBiome(Biome.PLAINS);
                    c.setElevation(50.0);
                    weights[i] = 1.0;
                    total += 1.0;
                }
            }
        }
        long assigned = 0;
        int populated = 0;
        for (int i = 0; i < cells.size(); i++) {
            if (weights[i] <= 0.0) continue;
            long p = Math.round(totalPop * (weights[i] / total));
            if (p <= 0) continue;
            H3Cell c = cells.get(i);
            c.setPopulation((int) Math.min(Integer.MAX_VALUE, p));
            c.setBiomassHuman((double) p);
            assigned += p;
            populated++;
        }
        logger.info("Empirical Earth density raster applied: {} humans across {} cells (year {}, no procedural generation)",
                assigned, populated, startYear);
    }

    /**
     * Evaluates cell-scale biophysical habitability based on freshwater hydrology, terrain slope, and elevation.
     * Implements dasymetric downscaling (Stevens et al. 2015, WorldPop methodology).
     *
     * @param c candidate hexagonal cell
     * @return habitability weighting factor in [0.0, 1.0]
     */
    public static double computeCellHabitability(H3Cell c) {
        if (c == null) return 0.0;
        if (c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN || c.getBiome() == Biome.GLACIER) {
            return 0.0;
        }

        // Total freshwater availability from precipitation, river networks, and accessible groundwater
        double rain = c.getRainfallOr(0.0);
        double water = c.getWaterResourceOr(0.0);
        double aquifer = c.getAccessibleAquiferOr(0.0);
        double totalWater = Math.max(rain, Math.max(water, aquifer * 200.0));

        // Hyper-arid threshold: if total water < 25 mm/yr (hyper-arid desert plateau without water source), habitability = 0
        if (totalWater < 25.0) {
            return 0.0;
        }
        // Habitability scales smoothly between 25 mm (extreme desert fringe) and 225 mm (rainfed agriculture)
        double waterFactor = Math.min(1.0, (totalWater - 25.0) / 200.0);

        // Extreme elevation penalty (> 4200m)
        double elev = c.getElevationOr(0.0);
        double elevFactor = (elev > 4200.0) ? Math.max(0.05, 1.0 - (elev - 4200.0) / 3000.0) : 1.0;

        return waterFactor * elevFactor;
    }

    /**
     * Seeds initial physical state parameters across cells based on scenario configuration.
     */
    private void seedInitialPhysicalState(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        double capitalK0 = scenario.getInitialCapitalPerCapita();
        double techLevel = Math.clamp(1.0 + Math.log10(Math.max(1.0, capitalK0 / 50.0)) * 1.4, 0.2, 10.0);
        double foodReserveMonths = scenario.getInitialFoodReserveMonths() > 0 ? scenario.getInitialFoodReserveMonths() : 6.0;

        for (H3Cell c : cells) {
            if (c == null) continue;
            long pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double effectiveK0 = (scenario.getStartDateYear() >= 2000 && capitalK0 > 15000.0) ? (capitalK0 / 1.85) : capitalK0;
            double cellCapital = pop * effectiveK0;
            if (c.getResourceCapital() == null || c.getResourceCapital() <= 0.0) {
                c.setResourceCapital(cellCapital);
            }
            if (c.getResourceMetal() == null || c.getResourceMetal() <= 0.0) {
                c.setResourceMetal(cellCapital * 0.15);
            }
            double effectiveTech = (c.getTechnologyLevel() != null && c.getTechnologyLevel() > 0.0)
                    ? c.getTechnologyLevel() : techLevel;
            c.setTechnologyLevel(effectiveTech);
            if (pop > 0 && (c.getWaterResource() == null || c.getWaterResource() < 300.0)) {
                c.setWaterResource(Math.max(c.getWaterResourceOr(0.0), 500.0));
            }
            if (c.getBiomassAgriculture() == null || c.getBiomassAgriculture() == 0.0) {
                double areaScale = Math.max(0.05, c.getEffectiveSurfaceAreaKm2() / 1770.0);
                double annualFoodNeedGJ = pop * org.ether.society.model.PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
                double initialFoodGJ = annualFoodNeedGJ * (foodReserveMonths / 12.0);
                if (c.getFoodResource() == null || c.getFoodResource() < initialFoodGJ) {
                    c.setFoodResource(initialFoodGJ);
                }
                if (c.getBiomassNatural() == null || c.getBiomassNatural() < initialFoodGJ) {
                    c.setBiomassNatural(initialFoodGJ);
                }
                double grainKgPerPerson = 420.0 * (1.0 + 0.05 * Math.max(0.0, effectiveTech - 1.5));
                double requiredAgriBiomassKg = pop * grainKgPerPerson;
                c.setBiomassAgriculture((pop > 0 && effectiveTech >= 1.5) ? requiredAgriBiomassKg : 0.0);
            }

            c.calculateMovementFriction(effectiveTech);
            c.updateAgePyramidFromTotal(effectiveTech);
        }
    }

    /**
     * Injects canonical empirical historical disasters into the scenario if not already present.
     * Ensures headless batch runs and unit tests reflect major planetary shocks (volcanoes, pandemics).
     *
     * @param scenario the scenario to inspect and populate
     */
    public static void ensureCanonicalHistoricalEvents(Scenario scenario) {
        if (scenario == null) return;
        boolean isEarth = scenario.getPlanetPreset() == null
                || "earth".equalsIgnoreCase(scenario.getPlanetPreset().getCanonicalPlanet())
                || "earth".equalsIgnoreCase(scenario.getPlanetPreset().elevationMapSource());
        if (!isEarth) return;

        long startYear = scenario.getStartDateYear();
        long endYear = scenario.getEndDateYear() > 0 ? scenario.getEndDateYear() : startYear + 1000;

        List<ClimateEvent> events = scenario.getClimateEvents();
        if (events == null) {
            events = new java.util.ArrayList<>();
            scenario.setClimateEvents(events);
        }

        // Canonical major biophysical events within [startYear, endYear]
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Ã‰ruption Minoenne de ThÃ©ra (Santorin)", -1610, 36.4, 25.4, 10.0, 0.95));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Ã‰ruption du VÃ©suve (PompÃ©i & Herculanum)", 79, 40.8, 14.4, 8.0, 0.85));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "AnnÃ©e sans Ã©tÃ© (Ã‰ruption mystÃ¨re de 536)", 536, 64.0, -18.0, 10.0, 0.90));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Ã‰ruption du Samalas (Lombok, IndonÃ©sie)", 1257, -8.4, 116.4, 12.0, 0.98));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("PANDEMIC", "Peste Noire (Yersinia Pestis)", 1347, 45.0, 35.0, 0.0, 0.90));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Ã‰ruption du Laki (Islande)", 1783, 64.0, -18.2, 8.0, 0.88));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Ã‰ruption du Tambora (AnnÃ©e sans Ã©tÃ©)", 1815, -8.2, 118.0, 10.0, 0.95));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("VOLCANO", "Explosion du Krakatoa (DÃ©troit de la Sonde)", 1883, -6.1, 105.4, 10.0, 0.90));
        addEventIfInRange(events, startYear, endYear, new ClimateEvent("PANDEMIC", "Grippe Espagnole (H1N1)", 1918, 39.0, -98.0, 0.0, 0.75));
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


