/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.events;

/**
 * Immutable telemetry record published at the conclusion of each simulation tick.
 *
 * <p>Provides decoupled, synchronous notification of macro-demographic, macroeconomic,
 * temporal, and throughput observables without imposing direct dependencies on UI layers
 * or specific presentation listeners.</p>
 *
 * @param tickIndex       monotonically increasing simulation tick counter
 * @param year            current simulated calendar year
 * @param month           current simulated month (0 to 11)
 * @param totalPopulation aggregate living human population across all cells
 * @param currentTPS      instantaneous simulation ticks per second throughput
 * @param gdp             aggregate gross domestic product computed from physical capital
 * @param gini            Gini inequality coefficient of physical capital distribution (0.0 to 1.0)
 * @param yearChanged     {@code true} if this tick crossed a calendar year boundary
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public record SimulationTickEvent(
    long tickIndex,
    int year,
    int month,
    long totalPopulation,
    double currentTPS,
    double gdp,
    double gini,
    boolean yearChanged
) {}
