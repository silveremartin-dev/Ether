/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.density;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.model.Biome;
import org.ether.society.model.TechnologyTree;
import org.ether.society.events.EventSystem;
import org.ether.society.flux.FluxEngine;
import org.ether.society.culture.CultureEngine;
import org.ether.society.model.CivilizationAge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Density-based simulation engine.
 * 
 * Simulates population and resource dynamics at the cell level:
 * - Food resources grow/decay based on season and biome
 * - Population grows when resources are abundant
 * - Population declines (deaths) when resources are scarce
 * - Population migrates to neighboring cells (density flow)
 * 
 * All values are densities (per kmÂ²) not absolute counts.
 */
public class DensitySimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(DensitySimulationEngine.class);

    private final H3Service h3Service;
    private final BiomeEvolution biomeEvolution;
    private final ResourceEvolution resourceEvolution;
    private EventSystem eventSystem; // Injected
    private CivilizationAge maxAchievedAge = CivilizationAge.STONE_AGE;

    // New Engines
    private final FluxEngine fluxEngine;
    private final CultureEngine cultureEngine;

    public DensitySimulationEngine() {
        this.h3Service = H3Service.getInstance();
        this.biomeEvolution = new BiomeEvolution();
        this.resourceEvolution = new ResourceEvolution();
        this.fluxEngine = new FluxEngine();
        this.cultureEngine = new CultureEngine();
    }

    private double foodGrowthRate = 0.1; // Base food growth per month
    private double foodDecayRate = 0.05; // Base food decay per month
    private double populationGrowthRate = 0.02; // Max monthly population growth
    private double startvationRate = 0.1; // Death rate when starving
    private double migrationRate = 0.05; // Max % that can migrate per month
    private double consumptionPerCapita = 2.0; // Food units consumed per person per month

    public void setEventSystem(EventSystem eventSystem) {
        this.eventSystem = eventSystem;
    }

    public CivilizationAge getMaxAchievedAge() {
        return maxAchievedAge;
    }

    public FluxEngine getFluxEngine() {
        return fluxEngine;
    }

    public CultureEngine getCultureEngine() {
        return cultureEngine;
    }

    /**
     * Simulate one month tick on all cells.
     * 
     * @param cells All H3 cells in the simulation
     * @param month Current month (0-11, 0=January)
     * @param year  Current year
     */
    public void tick(List<H3Cell> cells, int month, int year) {
        // Build neighbor map for migration
        Map<Long, List<Long>> neighborMap = buildNeighborMap(cells);

        // Phase 1: Update food and detailed resources (seasonal production)
        cells.parallelStream().forEach(cell -> {
            updateFoodProduction(cell, month);
            updateBiomassAndResources(cell, month); // New: Track specific biomass
            updateEnergy(cell); // New: Calculate available energy
        });

        // Phase 2: Population consumption, civilization, and growth/decline
        cells.parallelStream().forEach(cell -> {
            updateCivilization(cell, year); // New: Tech, Indices, Civ rules
            updatePopulation(cell);
        });

        // Phase 3: Migration (density flow to neighbors)
        Map<Long, H3Cell> cellMap = new HashMap<>();
        for (H3Cell cell : cells) {
            cellMap.put(cell.getH3Index(), cell);
        }

        for (H3Cell cell : cells) {
            processMigration(cell, neighborMap, cellMap);
        }

        // Phase 4: Flux (Resource Diffusion)
        fluxEngine.calculatePressures(cells);
        fluxEngine.processFlux(cells, neighborMap, cellMap);

        // Phase 5: Culture (Memetic Diffusion)
        cultureEngine.updateCultures(cells, neighborMap);

        // Phase 6: Resource evolution (monthly)
        resourceEvolution.tick(cells, month);

        // Phase 5: Biome evolution (yearly, check on January)
        if (month == 0) {
            biomeEvolution.tick(cells, year);
        }

        // Log summary periodically
        if (month == 0) {
            logYearlySummary(cells, year);
        }
    }

    /**
     * Update Biomass and Base Resources.
     */
    private void updateBiomassAndResources(H3Cell cell, int month) {
        Biome biome = cell.getBiome();

        // Natural Biomass regeneration
        double naturalRegen = getBiomeProductionRate(biome) * 0.1;
        cell.setBiomassNatural(Math.min(1000.0, cell.getBiomassNatural() + naturalRegen));

        // Fish (if water or coastal)
        if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN || biome == Biome.BEACH) {
            cell.setBiomassFish(Math.min(500.0, cell.getBiomassFish() + 5.0));
        }

        // Resources (Wood/Metal/Clay) replenishment or discovery
        if (biome == Biome.FOREST || biome == Biome.JUNGLE) {
            cell.setWoodResource(Math.min(1000.0, cell.getWoodResource() + 2.0));
        }

        // Sync Human Biomass with Population
        cell.setBiomassHuman(cell.getPopulation() * 0.06); // ~60kg avg -> tons? Let's say units. 0.06 per person
    }

    /**
     * Update Energy production.
     */
    private void updateEnergy(H3Cell cell) {
        // Work Energy (Slaves/Labor)
        double laborEnergy = cell.getPopulation() * 0.1; // Base labor
        if (cell.getTechnologyLevel() > 2.0)
            laborEnergy *= 1.2; // Better tools
        cell.setResourceWork(laborEnergy);
        cell.setEnergySlaves(laborEnergy); // For now equate labor to "slave/human energy"

        // Fire Energy (Wood burning)
        double woodBurned = Math.min(cell.getWoodResource(), cell.getPopulation() * 0.01);
        cell.setWoodResource(cell.getWoodResource() - woodBurned);
        cell.setEnergyFire(woodBurned * 5.0);

        // Solar/Wind (Tech dependent)
        if (cell.getTechnologyLevel() > 5.0) {
            cell.setEnergyWind(cell.getBiome() == Biome.PLAINS || cell.getBiome() == Biome.OCEAN ? 10.0 : 2.0);
        } else {
            cell.setEnergyWind(0.0);
        }
    }

    /**
     * Update Civilization stats (Indices, Tech).
     */
    private void updateCivilization(H3Cell cell, int year) {
        if (cell.getPopulation() <= 0)
            return;

        // Tech Growth (function of population density and surplus food/energy)
        double currentTech = cell.getTechnologyLevel();
        double potentialGrowth = 0.0;

        // Density Bonus - more people = more innovation
        if (cell.getPopulation() > 100)
            potentialGrowth += 0.001;
        if (cell.getPopulation() > 1000)
            potentialGrowth += 0.005;
        if (cell.getPopulation() > 10000)
            potentialGrowth += 0.01; // Cities accelerate progress

        // Surplus Bonus - food security enables specialization
        if (cell.getFoodResource() > cell.getPopulation() * 5)
            potentialGrowth += 0.001;
        if (cell.getFoodResource() > cell.getPopulation() * 10)
            potentialGrowth += 0.002; // Large surplus = leisure for innovation

        // Random Innovation / Diffusion
        if (Math.random() < 0.01)
            potentialGrowth += 0.01;

        // Apply tech growth (capped at 10)
        cell.setTechnologyLevel(Math.min(10.0, currentTech + potentialGrowth));

        // Create tech tree for this cell's tech level
        TechnologyTree techTree = new TechnologyTree();
        techTree.setOverallLevel(cell.getTechnologyLevel());

        // Check for Age Transition (Global)
        CivilizationAge cellAge = techTree.getCurrentAge();
        if (cellAge.ordinal() > maxAchievedAge.ordinal()) {
            maxAchievedAge = cellAge;
            if (eventSystem != null) {
                eventSystem.triggerEvent("🎉 GLOBAL ERA: The world enters the " + cellAge.getDisplayName() + "!");
                logger.info("New Age Reached: {}", cellAge);
            }
        }

        // Lifespan increases with Tech (medicine) and Food Security
        double baseLifespan = 30.0; // Base hunter-gatherer lifespan
        baseLifespan += techTree.getLifespanBonus();
        if (cell.getFoodResource() < cell.getPopulation())
            baseLifespan -= 10.0; // Famine reduces lifespan
        cell.setLifespan(Math.max(20.0, Math.min(85.0, baseLifespan)));

        // Fertility decreases with high tech (demographic transition model)
        double baseFertility = 6.5; // Pre-modern fertility
        if (cell.getTechnologyLevel() > 4.0)
            baseFertility -= (cell.getTechnologyLevel() - 4.0) * 0.4;
        cell.setFertility(Math.max(1.8, baseFertility));

        // Gini (Inequality) - increases with complexity
        double capital = cell.getResourceCapital();
        double gini = 0.2 + (Math.log10(Math.max(1, cell.getPopulation())) * 0.05) + (capital * 0.001);
        // High tech societies can reduce inequality
        if (cell.getTechnologyLevel() > 7.0)
            gini -= (cell.getTechnologyLevel() - 7.0) * 0.05;
        cell.setGiniIndex(Math.max(0.1, Math.min(0.9, gini)));

        // Accumulate Capital based on surplus labor
        if (cell.getResourceWork() > cell.getPopulation()) {
            double capitalGrowth = cell.getResourceWork() * 0.01 * techTree.getCarryingCapacityMultiplier();
            cell.setResourceCapital(cell.getResourceCapital() + capitalGrowth);
        }
    }

    /**
     * Update food production based on season and biome.
     */
    private void updateFoodProduction(H3Cell cell, int month) {
        Biome biome = cell.getBiome();
        double lat = cell.getLatitude();

        // Calculate seasonal factor
        // Northern hemisphere: summer = June-Aug (months 5-7), winter = Dec-Feb
        // (11,0,1)
        // Southern hemisphere: opposite
        double seasonalFactor = calculateSeasonalFactor(lat, month);

        // Base production rate by biome
        double baseProduction = getBiomeProductionRate(biome);

        // Temperature effect (too cold or too hot reduces production)
        double temp = cell.getTemperature();
        double tempFactor = calculateTemperatureFactor(temp);

        // Rainfall effect
        double rainfall = cell.getRainfall();
        double rainFactor = calculateRainfallFactor(rainfall);

        // Calculate net food change
        double foodProduction = baseProduction * seasonalFactor * tempFactor * rainFactor * foodGrowthRate;
        double foodDecay = cell.getFoodResource() * foodDecayRate;

        // Apply food change, capped at capacity
        double maxCapacity = getMaxFoodCapacity(biome);
        double newFood = Math.max(0, Math.min(maxCapacity,
                cell.getFoodResource() + foodProduction - foodDecay));

        cell.setFoodResource(newFood);

        // Biome Alteration (Deforestation)
        if ((biome == Biome.FOREST || biome == Biome.JUNGLE) && cell.getWoodResource() < 10.0
                && cell.getPopulation() > 100) {
            // If wood is depleted and population is high, forest becomes plains
            // (deforestation)
            if (Math.random() < 0.05) { // Small chance per month to transition
                cell.setBiome(Biome.PLAINS);
                logger.info("Deforestation occurred at cell {}", cell.getH3Index());
            }
        }
    }

    /**
     * Calculate seasonal modifier based on hemisphere and month.
     * Returns 0.2 (winter) to 1.0 (peak summer)
     */
    private double calculateSeasonalFactor(double latitude, int month) {
        // Determine if northern or southern hemisphere
        boolean isNorthern = latitude >= 0;

        // Months from peak summer (July for N, January for S)
        int peakMonth = isNorthern ? 6 : 0;
        int monthsFromPeak = Math.abs(month - peakMonth);
        if (monthsFromPeak > 6)
            monthsFromPeak = 12 - monthsFromPeak;

        // Sinusoidal factor: 1.0 at peak, 0.2 in deep winter
        double factor = 0.6 + 0.4 * Math.cos(monthsFromPeak * Math.PI / 6);

        // Equatorial regions have less seasonal variation
        double latEffect = Math.abs(latitude) / 90.0;
        factor = 1.0 - latEffect * (1.0 - factor);

        return Math.max(0.2, Math.min(1.0, factor));
    }

    /**
     * Get base food production rate by biome.
     */
    private double getBiomeProductionRate(Biome biome) {
        return switch (biome) {
            case JUNGLE -> 100.0; // Highest productivity
            case FOREST -> 80.0; // High productivity
            case PLAINS -> 60.0; // Good for agriculture
            case HILLS -> 40.0; // Moderate
            case BEACH -> 30.0; // Fishing + some plants
            case MOUNTAINS -> 20.0; // Limited grazing
            case TUNDRA -> 15.0; // Very limited
            case DESERT -> 10.0; // Oasis only
            case OCEAN, DEEP_OCEAN -> 50.0; // Fish!
            case SNOW -> 5.0; // Minimal
        };
    }

    /**
     * Get maximum food capacity by biome.
     */
    private double getMaxFoodCapacity(Biome biome) {
        return switch (biome) {
            case JUNGLE -> 1000.0;
            case FOREST -> 800.0;
            case PLAINS -> 600.0;
            case HILLS -> 400.0;
            case BEACH -> 300.0;
            case MOUNTAINS -> 200.0;
            case OCEAN, DEEP_OCEAN -> 500.0;
            case TUNDRA -> 150.0;
            case DESERT -> 100.0;
            case SNOW -> 50.0;
        };
    }

    /**
     * Temperature factor for food production.
     * Optimal around 15-25Â°C, decreases outside this range.
     */
    private double calculateTemperatureFactor(double temp) {
        if (temp < -10)
            return 0.1;
        if (temp < 0)
            return 0.3;
        if (temp < 10)
            return 0.6;
        if (temp < 15)
            return 0.8;
        if (temp <= 25)
            return 1.0;
        if (temp <= 35)
            return 0.8;
        if (temp <= 45)
            return 0.4;
        return 0.1;
    }

    /**
     * Rainfall factor for food production.
     * Optimal around 500-1500mm, extremes reduce production.
     */
    private double calculateRainfallFactor(double rainfall) {
        if (rainfall < 100)
            return 0.2; // Arid
        if (rainfall < 300)
            return 0.5; // Semi-arid
        if (rainfall < 500)
            return 0.8;
        if (rainfall <= 1500)
            return 1.0; // Optimal
        if (rainfall <= 2500)
            return 0.9;
        return 0.7; // Too wet
    }

    /**
     * Update population based on food availability.
     */
    private void updatePopulation(H3Cell cell) {
        int pop = cell.getPopulation();
        double food = cell.getFoodResource();

        if (pop == 0)
            return;

        // Food consumption
        // Food consumption
        CivilizationAge age = CivilizationAge.fromTechLevel(cell.getTechnologyLevel());
        double consumption = pop * consumptionPerCapita * age.getResourceConsumptionMultiplier();

        if (food >= consumption) {
            // Enough food - consume and potentially grow
            cell.setFoodResource(food - consumption);

            // Growth rate depends on surplus
            double surplus = (food - consumption) / consumption;
            double growthFactor = Math.min(populationGrowthRate, surplus * 0.1);

            int births = (int) (pop * growthFactor);
            cell.setPopulation(pop + births);

        } else {
            // Not enough food - consume what's available, population declines
            cell.setFoodResource(0.0);

            double deficit = (consumption - food) / consumption;
            double deaths = pop * deficit * startvationRate;

            cell.setPopulation(Math.max(0, pop - (int) Math.ceil(deaths)));
        }
    }

    /**
     * Process migration as density flow to neighboring cells.
     */
    private void processMigration(H3Cell cell, Map<Long, List<Long>> neighborMap,
            Map<Long, H3Cell> cellMap) {
        int pop = cell.getPopulation();
        if (pop < 10)
            return; // Minimum population to migrate

        List<Long> neighbors = neighborMap.get(cell.getH3Index());
        if (neighbors == null || neighbors.isEmpty())
            return;

        // Calculate migration pressure (high population + low food = more migration)
        double foodPerCapita = cell.getFoodResource() / Math.max(1, pop);
        double migrationPressure = Math.max(0, 1.0 - foodPerCapita / 10.0);

        // Find best neighbor (highest food per capita)
        H3Cell bestNeighbor = null;
        double bestScore = -1;

        for (Long neighborIdx : neighbors) {
            H3Cell neighbor = cellMap.get(neighborIdx);
            if (neighbor == null)
                continue;

            // Can't migrate to ocean
            Biome biome = neighbor.getBiome();
            if (biome == Biome.OCEAN || biome == Biome.DEEP_OCEAN)
                continue;

            double neighborFood = neighbor.getFoodResource();
            int neighborPop = neighbor.getPopulation();
            double neighborScore = neighborFood / Math.max(1, neighborPop);

            // Prefer cells with more food per person than current
            if (neighborScore > foodPerCapita && neighborScore > bestScore) {
                bestScore = neighborScore;
                bestNeighbor = neighbor;
            }
        }

        // Migrate to best neighbor
        if (bestNeighbor != null && migrationPressure > 0.1) {
            int migrants = (int) (pop * migrationRate * migrationPressure);
            migrants = Math.max(1, Math.min(migrants, pop / 4)); // Cap at 25% of population

            cell.setPopulation(pop - migrants);
            bestNeighbor.setPopulation(bestNeighbor.getPopulation() + migrants);
        }
    }

    /**
     * Build a map of H3 index to neighbor indices.
     */
    private Map<Long, List<Long>> buildNeighborMap(List<H3Cell> cells) {
        // Use concurrent map for parallel collection if needed, but sequential
        // generation is safer for H3Core
        // optimization: H3Core is likely thread-safe for calculations, so we can try
        // parallel if H3Service supports it
        // For now, let's keep map building sequential or simple parallel collect
        return cells.parallelStream().collect(java.util.stream.Collectors.toConcurrentMap(
                H3Cell::getH3Index,
                cell -> {
                    try {
                        return h3Service.getNeighbors(cell.getH3Index());
                    } catch (Exception e) {
                        return List.of();
                    }
                }));
    }

    /**
     * Log yearly summary statistics.
     */
    private void logYearlySummary(List<H3Cell> cells, int year) {
        long totalPop = cells.stream().mapToLong(H3Cell::getPopulation).sum();
        double totalFood = cells.stream().mapToDouble(H3Cell::getFoodResource).sum();
        long populatedCells = cells.stream().filter(c -> c.getPopulation() > 0).count();

        logger.info("Year {}: Population={}, Food={:.0f}, PopulatedCells={}",
                year, totalPop, totalFood, populatedCells);
    }

    // Setters for tuning parameters

    public void setFoodGrowthRate(double rate) {
        this.foodGrowthRate = rate;
    }

    public void setFoodDecayRate(double rate) {
        this.foodDecayRate = rate;
    }

    public void setPopulationGrowthRate(double rate) {
        this.populationGrowthRate = rate;
    }

    public void setStarvationRate(double rate) {
        this.startvationRate = rate;
    }

    public void setMigrationRate(double rate) {
        this.migrationRate = rate;
    }

    public void setConsumptionPerCapita(double rate) {
        this.consumptionPerCapita = rate;
    }
}
