/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.database.components;

/**
 * High-performance Data-Oriented immutable component encapsulating ecological, agricultural, livestock and biotic exergy tensors.
 *
 * @param biomassHuman human biomass in metric tonnes
 * @param biomassLivestock domesticated livestock biomass in metric tonnes
 * @param biomassFish marine and freshwater aquatic biomass in metric tonnes
 * @param biomassAgriculture cultigen and cropland biomass in metric tonnes
 * @param biomassNatural wild floral and faunal biomass in metric tonnes
 * @param foodResource caloric stockpile in Gigajoules (GJ)
 * @param waterResource available freshwater in thousand cubic meters (10³ m³)
 * @param woodResource standing timber biomass in metric tonnes (t)
 * @param soilOrganicCarbon soil organic carbon fertility in tC/km²
 * @param soilSalinity soil salinity / electrical conductivity in dS/m
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public record BiomassState(
        double biomassHuman,
        double biomassLivestock,
        double biomassFish,
        double biomassAgriculture,
        double biomassNatural,
        double foodResource,
        double waterResource,
        double woodResource,
        double soilOrganicCarbon,
        double soilSalinity
) {
    /** Standard terrestrial baseline biomass state. */
    public static final BiomassState DEFAULT = new BiomassState(
            0.0, 0.0, 0.0, 0.0, 100.0, 50.0, 50.0, 50.0, 10.0, 0.0
    );
}
