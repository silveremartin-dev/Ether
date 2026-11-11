/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.engine;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.model.Scenario;

/**
 * Common operational contract for all planetary simulation engines across Tier 1 (biophysical
 * conservation laws) and Tier 2 (cliodynamic, macroeconomic, and historical scenario hypotheses).
 *
 * <p>Enforces zero static state leaks, standardized per-tick execution over continuous
 * Data-Oriented Design {@link WorldBuffer} primitive memory tensors, and deterministic resets.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public interface SimulationEngineModule {

    /**
     * Unique alphanumeric identifier for this engine module, matching scenario engine state keys.
     *
     * @return unique engine identifier (e.g., {@code "KummelAyresExergyEngine"})
     */
    String id();

    /**
     * Human-readable title of this engine module.
     *
     * @return display title
     */
    String name();

    /**
     * Categorical classification tier of this simulation engine.
     *
     * @return {@link EngineTier#TIER_1_BIOPHYSICAL} or {@link EngineTier#TIER_2_CLIODYNAMIC}
     */
    EngineTier tier();

    /**
     * Determines whether this engine is active for the specified scenario.
     *
     * @param scenario active scenario configuration, or {@code null}
     * @return {@code true} if this engine should participate in the current tick
     */
    boolean isEnabled(Scenario scenario);

    /**
     * Executes one temporal simulation step over the data-oriented planetary buffers.
     *
     * @param buffer    data-oriented world memory buffer
     * @param dtYears   elapsed time step in Julian years
     * @param dtMonthly elapsed time step as a monthly fraction
     */
    void tick(WorldBuffer buffer, double dtYears, double dtMonthly);

    /**
     * Resets all internal instance states, cumulative accumulators, and caches.
     * Guarantees zero cross-scenario state leakage.
     */
    void reset();

    /**
     * Determines whether this engine supports cell-local derivative accumulation
     * for single-pass Kernel Fusion over {@link WorldBuffer}.
     *
     * @return {@code true} if this engine implements {@link #accumulateCellularDerivatives}
     */
    default boolean supportsCellularDerivatives() {
        return false;
    }

    /**
     * Accumulates local derivative increments for a single cell slot during the fused DoD pass.
     *
     * @param buffer      contiguous world memory buffer
     * @param cellIndex   index of the cell in the contiguous buffer
     * @param dtYears     elapsed time step in Julian years
     * @param dtMonthly   elapsed time step as a monthly fraction
     * @param accumulator target derivative accumulator
     */
    default void accumulateCellularDerivatives(WorldBuffer buffer, int cellIndex, double dtYears, double dtMonthly, DerivativeAccumulator accumulator) {
        // Default no-op for non-cellular or whole-buffer stencil engines
    }
}
