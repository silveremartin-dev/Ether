/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import java.util.List;

/**
 * Functional Interface for custom pluggable procedural engines in Ether.
 * Users can implement this interface to inject custom physical, biological,
 * or socio-economic cliodynamic simulation logic into the H3 tick loop.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
@FunctionalInterface
public interface ProceduralEnginePlugin {

    /**
     * Executes custom procedural simulation step for the tick.
     *
     * @param cells List of active H3Cell instances in the simulation grid
     * @param deltaYears Step size in simulation years (typically 1.0)
     */
    void process(List<H3Cell> cells, double deltaYears);

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

