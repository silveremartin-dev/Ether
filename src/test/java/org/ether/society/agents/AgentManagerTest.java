package org.ether.society.agents;

import org.ether.society.h3.H3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentManagerTest {

    private AgentManager agentManager;

    @BeforeEach
    void setUp() {
        agentManager = new AgentManager(new H3Service(8));
    }

    @Test
    @DisplayName("Spawn agent and verify state")
    void testSpawnAgent() {
        Agent agent = agentManager.spawnAgent(AgentType.SCOUT, 0x8828308281fffffL);

        assertNotNull(agent);
        assertEquals(AgentType.SCOUT, agent.getType());
        assertEquals(0x8828308281fffffL, agent.getH3Index());
        assertEquals(1, agentManager.getAgents().size());
    }

    @Test
    @DisplayName("Clear agent manager resets agent list")
    void testClear() {
        agentManager.spawnAgent(AgentType.TRADER, 0x8828308281fffffL);
        agentManager.clear();

        assertTrue(agentManager.getAgents().isEmpty());
    }
}
