package org.ether.society.core.dod;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemographicKernelTest {

    private DemographicKernel demographicKernel;
    private WorldBuffer worldBuffer;
    private AgentBuffer agentBuffer;

    @BeforeEach
    void setUp() {
        demographicKernel = new DemographicKernel();
        worldBuffer = new WorldBuffer(10);
        agentBuffer = new AgentBuffer(10);

        // Initialize agent 0 on hex 0
        agentBuffer.getHexIds()[0] = 0;
        agentBuffer.getMass()[0] = 100.0f;
        agentBuffer.getEnergy()[0] = 1000.0f;
        agentBuffer.getAge()[0] = 10.0f;

        // Food in world hex 0
        worldBuffer.getFoodResource()[0] = 500000.0f;
    }

    @Test
    @DisplayName("Metabolism consumes local food resource")
    void testMetabolismConsumesFood() {
        float initialFood = worldBuffer.getFoodResource()[0];
        demographicKernel.tick(worldBuffer, agentBuffer, 1.0f);

        assertTrue(worldBuffer.getFoodResource()[0] < initialFood, "Agent should consume food from world");
        assertTrue(worldBuffer.getBiomassHuman()[0] > 0, "Human biomass should be updated");
    }

    @Test
    @DisplayName("Large agents split via mitosis")
    void testMitosis() {
        agentBuffer.getMass()[0] = 2500.0f;
        agentBuffer.getEnergy()[0] = 10_000_000.0f;
        agentBuffer.getAge()[0] = 0.0f;
        worldBuffer.getFoodResource()[0] = 100_000_000.0f;

        demographicKernel.tick(worldBuffer, agentBuffer, 1.0f);

        // Check if a second agent slot was created
        boolean secondAgentExists = false;
        for (int i = 1; i < agentBuffer.getCapacity(); i++) {
            if (agentBuffer.getHexIds()[i] == 0) {
                secondAgentExists = true;
                break;
            }
        }
        assertTrue(secondAgentExists, "Mitosis should create a child cohort in a free slot");
    }
}
