/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.events;

/**
 * Functional listener interface for observing simulation tick telemetry events.
 *
 * <p>Enables decoupled observers (headless loggers, analytics exporters, UI chart controllers)
 * to react synchronously or asynchronously to simulation progress.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
@FunctionalInterface
/**
 * SimulationTickListener.
 * <p>
 * Component for the Ether planetary simulation platform.
 * Adheres to the zero-defect code excellence standards and 500-line limit.
 * </p>
 *
 * @author Ether Architectural Working Group
 * @see org.ether.society.core.engine.SimulationEngineModule
 */
public interface SimulationTickListener {

    /**
     * Invoked upon the completion of a simulation tick.
     *
     * @param event telemetry payload containing step metrics and demographic aggregates
     */
    void onTick(SimulationTickEvent event);
}
