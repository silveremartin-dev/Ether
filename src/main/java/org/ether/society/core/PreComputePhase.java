/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.analytics.HistoricalValidationKernel;
import org.ether.society.analytics.HistoricalValidationKernel.InterpolationMethod;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.data.ResourceDepositMapReader;
import org.ether.society.database.H3Cell;
import org.ether.society.engines.FutureScenarioRegistry;
import org.ether.society.engines.tier2.theories.ProceduralPopulationGenerator;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.generation.ProceduralGenerator;
import org.ether.society.model.Biome;
import org.ether.society.model.PhysicalConstants;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Pre-computation phase that runs BEFORE simulation ticks begin.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Register canonical historical geophysical disasters</li>
 *   <li>Compute initial paleoclimate and glaciological forcings</li>
 *   <li>Initialize biophysical resource stockpiles via Miami NPP (Lieth 1975)</li>
 *   <li>Distribute initial human population (empirical HYDE/WorldPop dasymetric downscaling for Earth)</li>
 *   <li>Seed initial physical capital ($K_0$), metal, technology, friction, and demographic cohorts</li>
 * </ol>
 * </p>
 *
 * <p>Strictly compliant with Ether Determinism Directives ({@code AGENTS.md} Â§3 &amp; Â§8):
 * empirical data sources are preserved as authoritative invariants, and procedural derivations
 * employ closed-form physical equations with zero pseudo-random noise.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see PreComputeClimateAndResourceHelper
 */
public class PreComputePhase {
    private static final Logger logger = LoggerFactory.getLogger(PreComputePhase.class);

    /** Scenario configuration driving boundary conditions for pre-computation. */
    private final Scenario scenario;

    /**
     * Constructs a pre-computation phase bound to the given scenario.
     *
     * @param scenario target simulation scenario
     */
    public PreComputePhase(Scenario scenario) {
        this.scenario = scenario;
    }

    /**
     * Executes all pre-computation steps sequentially on the given H3 cell grid.
     *
     * @param cells spatial H3 cell list
     */
    public void execute(List<H3Cell> cells) {
        logger.info("=== PreCompute Phase Starting ===");
        logger.info("Processing {} cells for scenario: {}", cells.size(), scenario.getName());
        long startTime = System.currentTimeMillis();

        ensureCanonicalHistoricalEvents(scenario);
        PreComputeClimateAndResourceHelper.computeClimate(scenario, cells);

        try {
            FutureScenarioRegistry.PhysicalScenarioPreset futurePreset =
                    FutureScenarioRegistry.findPresetForScenario(scenario);
            if (futurePreset != null) {
                FutureScenarioRegistry.applyScenarioForcing(futurePreset, cells);
            }
        } catch (NoClassDefFoundError | Exception e) {
            logger.warn("Could not apply future scenario forcing for scenario {}: {}", scenario.getName(), e.getMessage());
        }

        PreComputeClimateAndResourceHelper.initializeResources(scenario, cells);
        distributeInitialPopulation(cells);
        seedInitialPhysicalState(cells);

        long elapsed = System.currentTimeMillis() - startTime;
        logger.info("=== PreCompute Phase Complete in {}ms ===", elapsed);
    }

    /**
     * Computes Net Primary Productivity (NPP) in $\text{g dry matter/m}^2\text{/yr}$ using the Miami Model (Lieth 1975).
     *
     * @param temp   mean annual temperature in $^\circ\text{C}$
     * @param precip annual precipitation in $\text{mm}$
     * @return NPP in $\text{g dry matter/m}^2\text{/yr}$
     */
    public static double calculateMiamiNPP(double temp, double precip) {
        return PreComputeClimateAndResourceHelper.calculateMiamiNPP(temp, precip);
    }

    /**
     * Calculates herbaceous pasture carrying capacity for wild grazing herbivores (McNaughton 1985; Sala et al. 1988).
     *
     * @param temp      surface temperature in $^\circ\text{C}$
     * @param rain      precipitation in $\text{mm}$
     * @param elev      elevation in meters
     * @param areaScale spatial scaling factor relative to base H3 resolution 3
     * @return wild livestock carrying capacity in metric tons
     */
    public static double calculateHerbaceousPastureCarryingCapacity(double temp, double rain, double elev, double areaScale) {
        return PreComputeClimateAndResourceHelper.calculateHerbaceousPastureCarryingCapacity(temp, rain, elev, areaScale);
    }

    /**
     * Injects canonical empirical historical disasters into the scenario if not already present.
     *
     * @param scenario scenario to inspect and populate
     */
    public static void ensureCanonicalHistoricalEvents(Scenario scenario) {
        PreComputeClimateAndResourceHelper.ensureCanonicalHistoricalEvents(scenario);
    }

    /**
     * Distributes initial human population based on scenario settings, preserving already-configured populations
     * or applying empirical raster downscaling (Earth) / procedural distribution (non-Earth).
     */
    private void distributeInitialPopulation(List<H3Cell> cells) {
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

        long totalPop = scenario.getInitialHumanCount();
        double capitalK0 = scenario.getInitialCapitalPerCapita();
        double techLevel = scenario.getInitialTechLevel() > 0.0
                ? scenario.getInitialTechLevel()
                : ((capitalK0 >= 5.0 && capitalK0 < 50.0)
                ? (1.50 + 0.30 * Math.log10(capitalK0 / 4.0))
                : Math.clamp(1.0 + Math.log10(Math.max(1.0, capitalK0 / 50.0)) * 1.4, 0.2, 10.0));
        String pattern = scenario.getPopulationDensityType() != null ? scenario.getPopulationDensityType() : "UNBIASED_NATURAL";
        long startYear = scenario.getStartDateYear();

        boolean isEarth = scenario.getPlanetPreset() != null
                && ("earth".equalsIgnoreCase(scenario.getPlanetPreset().getCanonicalPlanet())
                || "earth".equalsIgnoreCase(scenario.getPlanetPreset().elevationMapSource()));

        if (isEarth || (scenario.getCustomDensityBase64() != null && !scenario.getCustomDensityBase64().isBlank())) {
            distributeEmpiricalEarthPopulation(cells, totalPop, startYear);
            return;
        }

        logger.info("Distributing initial population via ProceduralPopulationGenerator (non-Earth planet): {} humans", totalPop);
        ProceduralPopulationGenerator.distributePopulation(cells, scenario, totalPop, techLevel, pattern, false, startYear);
    }

    private static final java.util.Map<String, Double> GLOBAL_RASTER_WEIGHT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private double getOrComputeGlobalRasterWeight(BufferedImage densityImg, long startYear) {
        if (densityImg == null) return 0.0;
        String key = "earth_globe_" + startYear + "_" + densityImg.getWidth() + "x" + densityImg.getHeight();
        Double cached = GLOBAL_RASTER_WEIGHT_CACHE.get(key);
        if (cached != null && cached > 0.0) {
            return cached;
        }
        double totalSum = org.ether.society.data.H3RasterAggregator.computeGlobalDensityIntegral(densityImg);
        if (totalSum > 0.0) {
            GLOBAL_RASTER_WEIGHT_CACHE.put(key, totalSum);
        }
        return totalSum;
    }

    /**
     * Distributes initial Earth population strictly proportionally to the empirical epoch density raster
     * integrated over each H3 cell's spherical hexagonal footprint:
     * $$P_i = P_{\text{tot}} \cdot \frac{\iint_{\text{hex}_i} \rho\,dA}{\iint_{\mathbb{S}^2} \rho\,dA}$$
     * conserving total population identically across resolutions and regional windows.
     */
    private void distributeEmpiricalEarthPopulation(List<H3Cell> cells, long totalPop, long startYear) {
        if (scenario.getCustomDensityBase64() == null || scenario.getCustomDensityBase64().isBlank()) {
            HistoricalMapGenerator.populateScenarioHistoricalMaps(scenario);
        }
        String b64 = scenario.getCustomDensityBase64();
        BufferedImage densityImg = null;
        if (b64 != null && !b64.isBlank()) {
            try {
                String payload = b64.contains(",") ? b64.substring(b64.indexOf(',') + 1) : b64;
                densityImg = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(payload)));
            } catch (Exception e) {
                logger.error("Failed to decode empirical Earth density raster: {}", e.getMessage());
            }
        }
        if (densityImg == null) {
            throw new IllegalStateException("No empirical density raster found for Earth scenario '" + scenario.getName()
                    + "' (year " + startYear + "). Procedural population fallback is forbidden for Earth.");
        }

        long effectiveWorldPop = totalPop;
        if (effectiveWorldPop <= 0) {
            double wp = HistoricalValidationKernel.getInterpolatedBenchmarkValue(
                    "worldPopulation", (double) startYear, InterpolationMethod.PCHIP_MONOTONE_CUBIC);
            if (wp > 0.0 && !Double.isNaN(wp)) {
                effectiveWorldPop = Math.round(wp * 1_000_000.0);
            } else {
                effectiveWorldPop = 5_000_000L;
            }
        }

        double[] weights = org.ether.society.data.H3RasterAggregator.aggregateDensityWeights(densityImg, cells);
        double total = 0.0;
        for (int i = 0; i < cells.size(); i++) {
            H3Cell c = cells.get(i);
            c.setPopulation(0);
            c.setBiomassHuman(0.0);
            double w = weights[i];
            if (w > 0.0) {
                if (c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN || (c.getElevation() != null && c.getElevation() < 0.0)) {
                    c.setElevation(Math.max(15.0, c.getElevation() != null ? Math.abs(c.getElevation()) : 50.0));
                    c.setBiome(Biome.PLAINS);
                }
                total += w;
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
                for (int i = 0; i < Math.min(cells.size(), 100); i++) {
                    H3Cell c = cells.get(i);
                    c.setBiome(Biome.PLAINS);
                    c.setElevation(50.0);
                    weights[i] = 1.0;
                    total += 1.0;
                }
            }
        }

        String cacheKey = "earth_globe_" + startYear + "_" + densityImg.getWidth() + "x" + densityImg.getHeight();
        if (!scenario.isClippingEnabled() && total > 0.0) {
            GLOBAL_RASTER_WEIGHT_CACHE.put(cacheKey, total);
        }

        double denominator = total;
        if (scenario.isClippingEnabled()) {
            double gTotal = getOrComputeGlobalRasterWeight(densityImg, startYear);
            if (gTotal > 0.0) {
                denominator = gTotal;
            }
        }

        long assigned = 0;
        int populated = 0;
        for (int i = 0; i < cells.size(); i++) {
            if (weights[i] <= 0.0) continue;
            long p = Math.round(effectiveWorldPop * (weights[i] / denominator));
            if (p <= 0) {
                p = 1L;
            }
            H3Cell c = cells.get(i);
            c.setPopulation((int) Math.min(Integer.MAX_VALUE, p));
            c.setBiomassHuman((double) p);
            assigned += p;
            populated++;
        }
        // Continuous proportional dasymetric adjustment across all populated cells (Stevens et al. 2015)
        // Eliminates single-cell dumping artifacts while conserving total population exactly.
        if (!scenario.isClippingEnabled() && assigned != effectiveWorldPop && effectiveWorldPop > 0 && assigned > 0) {
            double adjRatio = (double) effectiveWorldPop / (double) assigned;
            long reAssigned = 0;
            for (int i = 0; i < cells.size(); i++) {
                if (weights[i] <= 0.0) continue;
                H3Cell c = cells.get(i);
                int curP = c.getPopulationOr(0);
                if (curP > 0) {
                    long newP = Math.max(1L, Math.round(curP * adjRatio));
                    c.setPopulation((int) Math.min(Integer.MAX_VALUE, newP));
                    c.setBiomassHuman((double) newP);
                    reAssigned += newP;
                }
            }
            assigned = reAssigned;
        }
        logger.info("Empirical Earth density raster applied: {} humans across {} cells (year {}, clipping={})",
                assigned, populated, startYear, scenario.isClippingEnabled());
    }

    /**
     * Evaluates cell-scale biophysical habitability based on freshwater hydrology and elevation
     * following dasymetric downscaling (Stevens et al. 2015, WorldPop methodology).
     *
     * @param c candidate hexagonal cell
     * @return habitability weighting factor in $[0.0, 1.0]$
     */
    public static double computeCellHabitability(H3Cell c) {
        if (c == null) return 0.0;
        if (c.getBiome() == Biome.OCEAN || c.getBiome() == Biome.DEEP_OCEAN || c.getBiome() == Biome.GLACIER) {
            return 0.0;
        }
        double totalWater = Math.max(c.getRainfallOr(0.0), Math.max(c.getWaterResourceOr(0.0), c.getAccessibleAquiferOr(0.0) * 200.0));
        if (totalWater < 25.0) return 0.0;
        double waterFactor = Math.min(1.0, (totalWater - 25.0) / 200.0);
        double elev = c.getElevationOr(0.0);
        double elevFactor = (elev > 4200.0) ? Math.max(0.05, 1.0 - (elev - 4200.0) / 3000.0) : 1.0;
        return waterFactor * elevFactor;
    }

    /**
     * Seeds initial physical state parameters ($K_0$, metal, friction, cohorts) across cells.
     */
    private void seedInitialPhysicalState(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        double capitalK0 = scenario.getInitialCapitalPerCapita();
        double foodReserveMonths = scenario.getInitialFoodReserveMonths() > 0 ? scenario.getInitialFoodReserveMonths() : 6.0;

        for (H3Cell c : cells) {
            if (c == null) continue;
            long pop = c.getPopulation() != null ? c.getPopulation() : 0;
            double effectiveK0 = (capitalK0 > 15000.0) ? (capitalK0 / 1.85) : capitalK0;
            double cellCapital = pop * effectiveK0;
            if (c.getResourceCapital() == null || c.getResourceCapital() <= 0.0) {
                c.setResourceCapital(cellCapital);
            }
            if (c.getResourceMetal() == null || c.getResourceMetal() <= 0.0) {
                c.setResourceMetal(cellCapital * 0.15);
            }
            double effectiveKPerCapita = pop > 0 ? (cellCapital / pop) : capitalK0;
            if (pop > 0 && (c.getWaterResource() == null || c.getWaterResource() < 300.0)) {
                c.setWaterResource(Math.max(c.getWaterResourceOr(0.0), 500.0));
            }
            double neolithicShare = org.ether.society.core.math.SmoothMath.sigmoid(effectiveKPerCapita, 11.0, 2.0);
            if (c.getBiomassLivestock() == null || c.getBiomassLivestock() <= 0.0) {
                c.setBiomassLivestock(pop > 0 ? (pop * 0.25 * neolithicShare) : 0.0);
            }
            if (c.getBiomassAgriculture() == null || c.getBiomassAgriculture() == 0.0) {
                double annualFoodNeedGJ = pop * PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
                double initialFoodGJ = annualFoodNeedGJ * (foodReserveMonths / 12.0);
                if (c.getFoodResource() == null || c.getFoodResource() < initialFoodGJ) {
                    c.setFoodResource(initialFoodGJ);
                }
                double minNaturalBiomassTons = (effectiveKPerCapita < 500.0) ? (pop * 48.0) : initialFoodGJ;
                if (c.getBiomassNatural() == null || c.getBiomassNatural() < minNaturalBiomassTons) {
                    c.setBiomassNatural(minNaturalBiomassTons);
                }
                double grainKgPerPerson = 620.0 * neolithicShare * (1.0 + 0.05 * org.ether.society.core.math.SmoothMath.softplus(effectiveKPerCapita - 500.0, 200.0));
                c.setBiomassAgriculture(pop > 0 ? (pop * grainKgPerPerson) : 0.0);
            }

            c.calculateMovementFriction();
            c.updateAgePyramidFromTotal();
        }
    }
}
