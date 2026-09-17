package org.ether.society.data;

import org.ether.society.core.dod.AgentBuffer;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Utility to migrate existing H3Cell lists into the high-performance DOD buffers.
 */
public class DODDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DODDataGenerator.class);

    /**
     * Populates a WorldBuffer from a list of H3Cells.
     */
    public static void populateWorldBuffer(List<H3Cell> cells, WorldBuffer buffer) {
        logger.info("Populating WorldBuffer with {} cells...", cells.size());
        
        for (int i = 0; i < cells.size() && i < buffer.getCapacity(); i++) {
            H3Cell cell = cells.get(i);
            
            buffer.getH3Indexes()[i] = cell.getH3Index();
            buffer.getElevation()[i] = cell.getElevation().floatValue();
            buffer.getTemperature()[i] = cell.getTemperature().floatValue();
            buffer.getRainfall()[i] = cell.getRainfall().floatValue();
            buffer.getBiomes()[i] = (byte) cell.getBiome().ordinal();
            
            buffer.getFoodResource()[i] = cell.getFoodResource().floatValue();
            buffer.getWaterResource()[i] = cell.getWaterResource().floatValue();
            buffer.getWoodResource()[i] = cell.getWoodResource().floatValue();
            buffer.getMetalResource()[i] = cell.getResourceMetal().floatValue();
            buffer.getClayResource()[i] = cell.getResourceClay().floatValue();
            
            boolean isWater = (cell.getBiome() == org.ether.society.model.Biome.OCEAN || cell.getBiome() == org.ether.society.model.Biome.DEEP_OCEAN || (cell.getElevation() != null && cell.getElevation() <= 0));
            double humanPop = isWater ? 0.0 : ((cell.getBiomassHuman() != null && cell.getBiomassHuman() > 0) ? cell.getBiomassHuman() : (cell.getPopulation() != null ? cell.getPopulation().doubleValue() : 0.0));
            buffer.getBiomassHuman()[i] = (float) humanPop;
            buffer.getBiomassLivestock()[i] = cell.getBiomassLivestock().floatValue();
            buffer.getBiomassFish()[i] = cell.getBiomassFish().floatValue();
            buffer.getBiomassAgriculture()[i] = cell.getBiomassAgriculture().floatValue();
            buffer.getBiomassNatural()[i] = cell.getBiomassNatural().floatValue();
            
            buffer.getEnergyWind()[i] = cell.getEnergyWind().floatValue();
            buffer.getEnergySolar()[i] = cell.getEnergySolar().floatValue();
            buffer.getEnergyFire()[i] = cell.getEnergyFire().floatValue();
            buffer.getEnergySlaves()[i] = cell.getEnergySlaves().floatValue();
            buffer.getEnergyFoodConsumed()[i] = cell.getEnergyFoodConsumed().floatValue();
            
            buffer.getLifespan()[i] = cell.getLifespan().floatValue();
            buffer.getFertility()[i] = cell.getFertility().floatValue();
            buffer.getGiniIndex()[i] = cell.getGiniIndex().floatValue();
            buffer.getTechnologyLevel()[i] = cell.getTechnologyLevel().floatValue();
        }
        
        // Populate neighbors in parallel for instant initialization
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
        
        logger.info("WorldBuffer population complete.");
    }

    /**
     * Initializes the AgentBuffer based on the population density in the WorldBuffer.
     * This creates "Demographic Nodes" (cohorts) for cells with population.
     */
    public static void initializeAgentBuffer(WorldBuffer world, AgentBuffer agents) {
        initializeAgentBuffer(world, agents, 500);
    }

    public static void initializeAgentBuffer(WorldBuffer world, AgentBuffer agents, int targetCohortSize) {
        logger.info("Initializing AgentBuffer from population density (target cohort size: {})...", targetCohortSize);
        int agentIndex = 0;
        int effectiveTarget = Math.max(1, targetCohortSize);
        
        for (int i = 0; i < world.getCapacity(); i++) {
            float totalCellPop = world.getBiomassHuman()[i];
            if (totalCellPop <= 0 || agentIndex >= agents.getCapacity()) continue;
            
            // Subdivide population into cohorts of targetCohortSize
            int numCohorts = Math.max(1, (int) Math.ceil(totalCellPop / (float) effectiveTarget));
            float cohortMass = totalCellPop / (float) numCohorts;

            for (int c = 0; c < numCohorts && agentIndex < agents.getCapacity(); c++) {
                agents.getHexIds()[agentIndex] = i;
                agents.getH3Indexes()[agentIndex] = world.getH3Indexes()[i];
                agents.getMass()[agentIndex] = cohortMass;
                agents.getEnergy()[agentIndex] = 100.0f; // Baseline energy
                agents.getSigmaCost()[agentIndex] = 0.1f; // Initial structure cost
                agents.getTechLevel()[agentIndex] = world.getTechnologyLevel()[i];
                
                // Initialize identity tensors with some baseline/noise
                for (int d = 0; d < 4; d++) {
                    agents.getGenetics()[d][agentIndex] = (float) Math.random();
                    agents.getCulture()[d][agentIndex] = (float) Math.random();
                }
                
                agentIndex++;
            }
        }
        
        logger.info("Initialized {} demographic agent cohort nodes.", agentIndex);
    }
}
