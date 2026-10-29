package org.ether.society.core.dod;

import org.ether.society.data.DODDataGenerator;
import org.ether.society.model.Biome;
import org.ether.society.database.H3Cell;
import org.ether.society.model.PhysicalConstants;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DemographicKernelDiagnosticTest {

    @Test
    public void testDemographicTickSingleCell() {
        WorldBuffer world = new WorldBuffer(1);
        world.getCapacity();
        world.getBiomes()[0] = (byte) Biome.PLAINS.ordinal();
        world.getElevation()[0] = 50.0f;
        world.getRainfall()[0] = 800.0f;
        world.getTemperature()[0] = 15.0f;
        world.getWaterResource()[0] = 500.0f;
        world.getTechnologyLevel()[0] = 8.0f;
        world.getLifespan()[0] = 55.0f;
        world.getResourceCapital()[0] = 10_000.0f * 3500.0f;

        float pop = 10_000.0f;
        world.getBiomassHuman()[0] = pop;

        // Food matching PreComputePhase
        double annualFoodNeedGJ = pop * PhysicalConstants.HUMAN_ANNUAL_METABOLIC_ENERGY_GJ;
        float initialFoodGJ = (float) (annualFoodNeedGJ * (6.0 / 12.0));
        world.getFoodResource()[0] = initialFoodGJ;

        float grainKgPerPerson = 420.0f * (1.0f + 0.05f * (8.0f - 1.5f));
        world.getBiomassAgriculture()[0] = pop * grainKgPerPerson;

        AgentBuffer agents = new AgentBuffer(100, 9);
        DODDataGenerator.initializeAgentBuffer(world, agents, 5000, 42L);

        System.out.println("--- INITIAL STATE ---");
        System.out.println("Pop: " + pop);
        System.out.println("Food: " + world.getFoodResource()[0]);
        System.out.println("AgriBiomass: " + world.getBiomassAgriculture()[0]);
        System.out.println("Agent 0 mass: " + agents.getMass()[0] + ", age: " + agents.getAge()[0] + ", energy: " + agents.getEnergy()[0]);

        DemographicKernel kernel = new DemographicKernel();
        kernel.setTargetCohortSize(5000.0f);
        kernel.setBaseSeed(42L);

        // Run 1 tick (30 days)
        float dtSeconds = 30.0f * 86400.0f;
        kernel.tick(world, agents, (long) dtSeconds);

        System.out.println("\n--- AFTER 1 TICK ---");
        System.out.println("Pop in world: " + world.getBiomassHuman()[0]);
        System.out.println("Food in world: " + world.getFoodResource()[0]);
        System.out.println("Agent 0 mass: " + agents.getMass()[0] + ", deaths: " + agents.getDeaths()[0]);
    }
}
