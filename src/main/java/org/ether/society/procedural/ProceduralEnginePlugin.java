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
 * or socio-economic simulation logic into the H3 tick loop.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
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
}
