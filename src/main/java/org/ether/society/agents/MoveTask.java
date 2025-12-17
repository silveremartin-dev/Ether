package org.ether.society.agents;

import org.ether.society.h3.H3Service;
import java.util.List;

/**
 * Task that moves an agent from current location to target location.
 * Uses H3 pathfinding.
 */
public class MoveTask implements AgentTask {
    private final long targetIndex;
    private final H3Service h3Service;

    // Path cache (could be optimized)
    private List<Long> currentPath;
    private int pathIndex = 0;

    private boolean completed = false;

    // Movement speed simulated by ticks
    private int ticksPerMove = 2; // Move every 2 ticks
    private int tickCounter = 0;

    public MoveTask(long targetIndex, H3Service h3Service) {
        this.targetIndex = targetIndex;
        this.h3Service = h3Service;
    }

    @Override
    public boolean isComplete() {
        return completed;
    }

    @Override
    public void update(Agent agent) {
        if (completed)
            return;

        // Initial pathfinding
        if (currentPath == null) {
            try {
                // Using kRing or gridDistance for simple navigation first
                // Ideally this would be A* on the graph
                // For now, simple neighbor stepping (greedy)

                // Note: Real pathfinding needed if obstacles exist.
                // Assuming fully traversable globe for MVP.
                currentPath = h3Service.gridPathCells(agent.getH3Index(), targetIndex);
                pathIndex = 0;
            } catch (Exception e) {
                // Path not found or error
                completed = true;
                return;
            }
        }

        // Check if finished
        if (pathIndex >= currentPath.size()) {
            completed = true;
            return;
        }

        // Move logic
        tickCounter++;
        if (tickCounter >= ticksPerMove) {
            tickCounter = 0;

            // Move to next cell in path
            long nextH3 = currentPath.get(pathIndex);
            agent.setH3Index(nextH3);
            pathIndex++;

            agent.setState(AgentState.MOVING); // Update state visual
        }
    }
}
