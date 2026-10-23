/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.database.components;

/**
 * High-performance Data-Oriented immutable component encapsulating human population, epidemiological, inequality and sociopolitical indices.
 *
 * @param population absolute inhabitant count
 * @param lifespan life expectancy at birth in years
 * @param fertility total fertility rate (children per female)
 * @param giniIndex Gini inequality coefficient [0.0..1.0]
 * @param technologyLevel aggregate technological progress index
 * @param homininSpecies hominin clade identifier (1 = Sapiens, 2 = Neanderthal, 3 = Denisovan)
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public record DemographicState(
        int population,
        double lifespan,
        double fertility,
        double giniIndex,
        double technologyLevel,
        int homininSpecies
) {
    /** Baseline demographic state for human agrarian/tribal societies. */
    public static final DemographicState DEFAULT = new DemographicState(
            0, 40.0, 6.0, 0.0, 0.0, 1
    );
}
