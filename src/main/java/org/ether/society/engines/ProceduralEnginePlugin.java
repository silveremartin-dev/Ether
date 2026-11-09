/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.database.H3Cell;
import java.util.List;

/**
 * <h1>Procedural Engine Plugin</h1>
 * <p>
 * Functional Interface for custom pluggable procedural engines in Ether.
 * Users can implement this interface to inject custom physical, biological,
 * or socio-economic cliodynamic simulation logic into the H3 tick loop.
 * Supports both high-performance zero-copy Data-Oriented Design (DoD) buffers
 * and legacy cell object collections.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
@FunctionalInterface
public interface ProceduralEnginePlugin {

    /**
     * Executes custom procedural simulation step for the tick over legacy cell objects.
     *
     * @param cells List of active H3Cell instances in the simulation grid
     * @param deltaYears Step size in simulation years (typically 1.0)
     */
    void process(List<H3Cell> cells, double deltaYears);

    /**
     * Executes custom procedural simulation step directly over the contiguous Data-Oriented WorldBuffer.
     * High-speed DoD path with zero heap object allocations and cache-friendly primitive access.
     *
     * @param buffer contiguous world buffer containing all primitive arrays
     * @param deltaYears step size in simulation years
     */
    default void processBuffer(WorldBuffer buffer, double deltaYears) {
        // Fallback for custom plugins that only implement the cell-list method
    }

    /**
     * Returns human-readable engine name.
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Returns brief description of the engine's purpose.
     */
    default String getDescription() {
        return "Procedural simulation plugin module.";
    }

    /**
     * Returns mathematical equations, units, parameters and mouseover tooltip.
     */
    default String getEquationsTooltip() {
        return "Tier 2 Optional Model\nMathematical differential equations and empirical laws.";
    }

    /**
     * Returns the ontological category (e.g. "Tier 2: Cliodynamics", "Tier 2: Biophysical Economics").
     */
    default String getCategory() {
        return "Tier 2: Cliodynamics";
    }
}


