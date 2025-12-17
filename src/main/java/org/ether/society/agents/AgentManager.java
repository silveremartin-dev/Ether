package org.ether.society.agents;

import org.ether.society.h3.H3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage all active agents in the simulation.
 */
public class AgentManager {
    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);

    // Thread-safe list of agents
    private final List<Agent> agents = Collections.synchronizedList(new ArrayList<>());

    // Map of Agent ID to current Task
    private final Map<Long, AgentTask> activeTasks = new ConcurrentHashMap<>();

    private final H3Service h3Service;

    public AgentManager(H3Service h3Service) {
        this.h3Service = h3Service;
    }

    /**
     * Spawn a new agent at the specified location.
     */
    public Agent spawnAgent(AgentType type, long h3Index) {
        Agent agent = new Agent(type, h3Index);
        agents.add(agent);
        logger.debug("Spawned Agent {} ({}) at {}", agent.getId(), type, h3Index);
        return agent;
    }

    /**
     * Assign a task to an agent.
     */
    public void assignTask(Agent agent, AgentTask task) {
        activeTasks.put(agent.getId(), task);
        agent.setState(AgentState.PERFORMING_TASK);
    }

    /**
     * Main update loop for all agents.
     * Should be called once per simulation tick.
     */
    public void update() {
        if (agents.isEmpty())
            return;

        // Process in parallel for scalability
        agents.parallelStream().forEach(agent -> {
            AgentTask task = activeTasks.get(agent.getId());
            if (task != null) {
                task.update(agent);
                if (task.isComplete()) {
                    activeTasks.remove(agent.getId());
                    agent.setState(AgentState.IDLE);
                }
            } else {
                // Default idle behavior could go here
                agent.update();
            }
        });
    }

    /**
     * @return Immutable copy of current agents list
     */
    public List<Agent> getAgents() {
        return new ArrayList<>(agents);
    }

    public void clear() {
        agents.clear();
        activeTasks.clear();
    }
}
