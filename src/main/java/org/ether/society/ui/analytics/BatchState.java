/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

/**
 * Enumeration representing the execution lifecycle state of a scenario within
 * headless batch simulation runs.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public enum BatchState {
    /**
     * Scenario is defined in repository or memory but has not yet been executed.
     */
    NOT_EXECUTED,

    /**
     * Scenario is queued in the batch execution thread pool awaiting simulation.
     */
    QUEUED,

    /**
     * Scenario is actively simulating ticks on worker threads.
     */
    RUNNING,

    /**
     * Scenario execution trajectory is complete with simulation history serialized to disk.
     */
    EXECUTED,

    /**
     * Scenario batch execution is paused due to interactive simulation preemption or user pause.
     */
    PAUSED
}
