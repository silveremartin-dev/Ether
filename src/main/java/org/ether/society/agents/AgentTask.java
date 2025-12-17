package org.ether.society.agents;

/**
 * Interface for tasks that an agent can perform.
 */
public interface AgentTask {
    /**
     * @return true if the task is finished
     */
    boolean isComplete();

    /**
     * Update the task logic for the given agent.
     */
    void update(Agent agent);
}
