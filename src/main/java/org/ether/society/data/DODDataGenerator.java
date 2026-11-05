package org.ether.society.data;

import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.math.SmoothMath;
import org.ether.society.database.H3Cell;
import org.ether.society.util.DeterministicPRNG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <h1>DODData Generator</h1>
 * <p>
 * Geospatial data ingestion, raster sampling, and Structure-of-Arrays (SoA) buffer initialization
 * pipeline. Populates {@link WorldBuffer} and {@link AgentBuffer} exclusively from conserved
 * biophysical observables ({@code AGENTS.md} §10.1), with deterministic SplitMix64 seeding
 * ({@code AGENTS.md} §3) and smooth $C^1$ demographic boundary initialization ({@code AGENTS.md} §10.2).
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class DODDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DODDataGenerator.class);

    /**
     * Populates a {@link WorldBuffer} from a list of {@link H3Cell}s using exclusively conserved
     * biophysical state variables.
     *
     * @param cells  ordered list of H3 hexagonal cells
     * @param buffer destination contiguous world state buffer
     */
    public static void populateWorldBuffer(List<H3Cell> cells, WorldBuffer buffer) {
        logger.info("Populating WorldBuffer with {} cells...", cells.size());

        for (int i = 0; i < cells.size() && i < buffer.getCapacity(); i++) {
            H3Cell cell = cells.get(i);

            buffer.getH3Indexes()[i] = cell.getH3Index();
            buffer.getElevation()[i] = cell.getElevation() != null ? cell.getElevation().floatValue() : 0.0f;
            buffer.getTemperature()[i] = cell.getTemperature() != null ? cell.getTemperature().floatValue() : 15.0f;
            buffer.getRainfall()[i] = cell.getRainfall() != null ? cell.getRainfall().floatValue() : 500.0f;
            buffer.getBiomes()[i] = (byte) (cell.getBiome() != null ? cell.getBiome().ordinal() : 0);

            buffer.getFoodResource()[i] = cell.getFoodResource() != null ? cell.getFoodResource().floatValue() : 0.0f;
            buffer.getWaterResource()[i] = cell.getWaterResource() != null ? cell.getWaterResource().floatValue() : 0.0f;
            buffer.getWoodResource()[i] = cell.getWoodResource() != null ? cell.getWoodResource().floatValue() : 0.0f;
            buffer.getMetalResource()[i] = cell.getResourceMetal() != null ? cell.getResourceMetal().floatValue() : 0.0f;
            buffer.getClayResource()[i] = cell.getResourceClay() != null ? cell.getResourceClay().floatValue() : 0.0f;

            boolean isWater = (cell.getBiome() == org.ether.society.model.Biome.OCEAN
                    || cell.getBiome() == org.ether.society.model.Biome.DEEP_OCEAN
                    || (cell.getElevation() != null && cell.getElevation() < 0.0));
            double humanPop = isWater ? 0.0
                    : ((cell.getBiomassHuman() != null && cell.getBiomassHuman() > 0)
                            ? cell.getBiomassHuman()
                            : (cell.getPopulation() != null ? cell.getPopulation().doubleValue() : 0.0));
            buffer.getBiomassHuman()[i] = (float) humanPop;
            buffer.getBiomassLivestock()[i] = cell.getBiomassLivestock() != null ? cell.getBiomassLivestock().floatValue() : 0.0f;
            buffer.getBiomassFish()[i] = cell.getBiomassFish() != null ? cell.getBiomassFish().floatValue() : 0.0f;
            buffer.getBiomassAgriculture()[i] = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture().floatValue() : 0.0f;
            buffer.getBiomassNatural()[i] = cell.getBiomassNatural() != null ? cell.getBiomassNatural().floatValue() : 0.0f;

            buffer.getEnergyWind()[i] = cell.getEnergyWind() != null ? cell.getEnergyWind().floatValue() : 0.0f;
            buffer.getEnergySolar()[i] = cell.getEnergySolar() != null ? cell.getEnergySolar().floatValue() : 0.0f;
            buffer.getEnergyFire()[i] = cell.getEnergyFire() != null ? cell.getEnergyFire().floatValue() : 0.0f;
            buffer.getEnergySlaves()[i] = cell.getEnergySlaves() != null ? cell.getEnergySlaves().floatValue() : 0.0f;
            buffer.getEnergyFoodConsumed()[i] = cell.getEnergyFoodConsumed() != null ? cell.getEnergyFoodConsumed().floatValue() : 0.0f;

            buffer.getLifespan()[i] = cell.getLifespan() != null ? cell.getLifespan().floatValue() : 35.0f;
            buffer.getFertility()[i] = cell.getFertility() != null ? cell.getFertility().floatValue() : 4.5f;
            buffer.getGiniIndex()[i] = cell.getGiniIndex() != null ? cell.getGiniIndex().floatValue() : 0.20f;
            buffer.getResourceCapital()[i] = cell.getResourceCapital() != null ? cell.getResourceCapital().floatValue() : 0.0f;
        }

        org.ether.society.h3.H3Service h3 = org.ether.society.h3.H3Service.getInstance();
        java.util.concurrent.ConcurrentHashMap<Long, Integer> indexMap = new java.util.concurrent.ConcurrentHashMap<>(cells.size());
        java.util.stream.IntStream.range(0, cells.size()).parallel().forEach(i -> {
            indexMap.put(cells.get(i).getH3Index(), i);
        });

        java.util.stream.IntStream.range(0, cells.size()).parallel().forEach(i -> {
            List<Long> neighbors = h3.getNeighbors(cells.get(i).getH3Index());
            for (int j = 0; j < 6; j++) {
                if (j < neighbors.size()) {
                    buffer.getNeighborIndexes()[i][j] = indexMap.getOrDefault(neighbors.get(j), -1);
                } else {
                    buffer.getNeighborIndexes()[i][j] = -1;
                }
            }
        });

        buffer.invalidatePartitionIndices();
        logger.info("WorldBuffer population complete.");
    }

    /**
     * Initializes the {@link AgentBuffer} based on population density in the {@link WorldBuffer}.
     */
    public static void initializeAgentBuffer(WorldBuffer world, AgentBuffer agents) {
        initializeAgentBuffer(world, agents, 500);
    }

    /**
     * Initializes the {@link AgentBuffer} with custom target cohort size.
     */
    public static void initializeAgentBuffer(WorldBuffer world, AgentBuffer agents, int targetCohortSize) {
        initializeAgentBuffer(world, agents, targetCohortSize, 0L);
    }

    /**
     * Initializes the {@link AgentBuffer} from conserved cellular population, agricultural biomass,
     * and built physical capital, seeded deterministically via {@link DeterministicPRNG}.
     *
     * @param world            populated world state buffer
     * @param agents           destination demographic cohort buffer
     * @param targetCohortSize target individual mass per cohort
     * @param seed             deterministic scenario seed
     */
    public static void initializeAgentBuffer(WorldBuffer world, AgentBuffer agents, int targetCohortSize, long seed) {
        logger.info("Initializing AgentBuffer from population density (target cohort size: {}, seed: {})...", targetCohortSize, seed);
        int agentIndex = 0;
        int effectiveTarget = Math.max(1, targetCohortSize);

        for (int i = 0; i < world.getCapacity(); i++) {
            float totalCellPop = world.getBiomassHuman()[i];
            if (totalCellPop <= 0 || agentIndex >= agents.getCapacity()) continue;

            int numCohorts = Math.max(1, (int) Math.ceil(totalCellPop / (float) effectiveTarget));
            float cohortMass = totalCellPop / (float) numCohorts;

            float capPerCap = world.getResourceCapital() != null ? (world.getResourceCapital()[i] / Math.max(1.0f, totalCellPop)) : 0.0f;
            float bioAgri = world.getBiomassAgriculture() != null ? world.getBiomassAgriculture()[i] : 0.0f;
            float bioNat = world.getBiomassNatural() != null ? world.getBiomassNatural()[i] : 0.0f;
            double agriShare = bioAgri / (bioAgri + bioNat * 0.01 + 1.0);
            // Smooth C^infty initial crude birth rate transition:
            // Forager ~0.028 -> Neolithic sedentary cereal weaning ~0.038 -> High-capital modern demographic transition ~0.018
            double neolithicWeight = SmoothMath.sigmoid(agriShare, 0.25, 0.10);
            double modernTransitionWeight = SmoothMath.sigmoid(capPerCap, 1200.0, 400.0);
            float initialAnnualCbr = (float) ((0.028 * (1.0 - neolithicWeight) + 0.038 * neolithicWeight)
                    * (1.0 - 0.52 * modernTransitionWeight));

            for (int c = 0; c < numCohorts && agentIndex < agents.getCapacity(); c++) {
                agents.getHexIds()[agentIndex] = i;
                agents.getH3Indexes()[agentIndex] = world.getH3Indexes()[i];
                agents.getMass()[agentIndex] = cohortMass;
                agents.getEnergy()[agentIndex] = 100.0f;
                agents.getSigmaCost()[agentIndex] = 0.1f;

                long seedH3 = (seed != 0L ? (seed ^ world.getH3Indexes()[i]) : world.getH3Indexes()[i]) + (c * 31L);
                float ageRnd = (float) DeterministicPRNG.nextDouble(seedH3, 101);
                float initialAge = 22.0f + ageRnd * 6.0f + 8.0f * (float) modernTransitionWeight;
                agents.getAge()[agentIndex] = initialAge;
                if (agents.getBirths() != null) {
                    agents.getBirths()[agentIndex] = cohortMass * initialAnnualCbr;
                }

                int genDims = agents.getGenetics() != null ? agents.getGenetics().length : 4;
                for (int d = 0; d < genDims; d++) {
                    agents.getGenetics()[d][agentIndex] = (float) DeterministicPRNG.nextDouble(seedH3, 200 + d);
                }
                int cultDims = agents.getCulture() != null ? agents.getCulture().length : 4;
                for (int d = 0; d < cultDims; d++) {
                    agents.getCulture()[d][agentIndex] = (float) DeterministicPRNG.nextDouble(seedH3, 300 + d);
                }

                agentIndex++;
            }
        }

        logger.info("Initialized {} demographic agent cohort nodes.", agentIndex);
    }
}
