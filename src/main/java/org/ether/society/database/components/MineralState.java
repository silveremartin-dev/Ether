/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.database.components;

/**
 * High-performance Data-Oriented immutable component encapsulating mineral, fossil fuel, fissile, and geological reserve tensors.
 *
 * @param resourceCoal extractable coal reserves in metric tonnes
 * @param resourceOil extractable crude oil reserves in barrels-equivalent / tonnes
 * @param resourceGas extractable natural gas reserves in cubic meters
 * @param resourceUranium fissile uranium / thorium ore deposits in metric tonnes
 * @param resourceMetal extractable base metal resources in metric tonnes
 * @param resourcePreciousMetal precious ore deposits in kg/km²
 * @param resourceRareEarths critical mineral and rare earth element deposits in metric tonnes
 * @param mantleHeatFlow geothermal heat flow & tectonic baseline in mW/m²
 * @param freshwaterAquifer total groundwater table volume in m³/km²
 * @param accessibleAquifer accessible shallow groundwater table volume in m³/km²
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public record MineralState(
        double resourceCoal,
        double resourceOil,
        double resourceGas,
        double resourceUranium,
        double resourceMetal,
        double resourcePreciousMetal,
        double resourceRareEarths,
        double mantleHeatFlow,
        double freshwaterAquifer,
        double accessibleAquifer
) {
    /** Baseline unmineralized crustal state. */
    public static final MineralState DEFAULT = new MineralState(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 87.0, 0.0, 0.0
    );
}
