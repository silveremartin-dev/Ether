package org.ether.society.agents;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a "Special Actor" or "Unit" in the simulation.
 *
 * <p>Unlike population densities (handled by {@link org.ether.society.density.ArtemisSimulationEngine} / Artemis),
 * these Agents represent specific, tracked individuals or groups with unique identity and state.
 * Examples: Diplomats, Armies, Traders, Heroes.</p>
 *
 * <p>They move across the H3 grid and interact with the density layer.</p>
 */
public class Agent {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private final long id;
    private final AgentType type;
    private long h3Index; // Current position
    private AgentState state;

    // Movement / Task data (to be expanded)
    private long targetH3Index = 0;

    public Agent(AgentType type, long startH3Index) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.type = type;
        this.h3Index = startH3Index;
        this.state = AgentState.IDLE;
    }

    public void update() {
        // Logic will be handled by tasks or manager for now
    }

    // Getters and Setters

    public long getId() {
        return id;
    }

    public AgentType getType() {
        return type;
    }

    public long getH3Index() {
        return h3Index;
    }

    public void setH3Index(long h3Index) {
        this.h3Index = h3Index;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state;
    }

    public long getTargetH3Index() {
        return targetH3Index;
    }

    public void setTargetH3Index(long targetH3Index) {
        this.targetH3Index = targetH3Index;
    }
}
