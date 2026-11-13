/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core;

import org.ether.society.model.PhysicalConstants;

/**
 * <h1>Canonical Simulation Time Step Representation</h1>
 *
 * <p>Immutable value record encapsulating the discrete integration time step ($\Delta t$)
 * across multiple physical dimensions (days, Julian years, synodic months, and SI seconds).
 * Instantiated exactly once per simulation tick in the lifecycle coordinator, eliminating
 * ambiguous dual-unit heuristics and redundant per-cell floating point divisions.</p>
 *
 * <h2>Physical Dimension Contracts</h2>
 * <ul>
 *   <li><b>{@link #dtDays()}</b>: Fundamental baseline duration in standard Earth solar days ($1\text{ day} \equiv 86\,400\text{ s}$).</li>
 *   <li><b>{@link #dtYears()}</b>: Astronomical Julian years ($1\text{ yr} \equiv 365.25\text{ days} = 31\,557\,600\text{ s}$).</li>
 *   <li><b>{@link #dtMonths()}</b>: Exact synodic calendar month fraction ($1\text{ month} \equiv 365.25 / 12 = 30.4375\text{ days}$).</li>
 *   <li><b>{@link #dtSeconds()}</b>: SI seconds for high-frequency thermodynamic and physical transport flux ($dtDays \times 86\,400$).</li>
 * </ul>
 *
 * @param dtDays    duration in solar days ($\Delta t_{\text{days}} \ge 10^{-4}$)
 * @param dtYears   duration in Julian years ($\Delta t_{\text{years}} = \Delta t_{\text{days}} / 365.25$)
 * @param dtMonths  duration in synodic calendar months ($\Delta t_{\text{months}} = \Delta t_{\text{days}} / 30.4375$)
 * @param dtSeconds duration in SI seconds ($\Delta t_{\text{seconds}} = \Delta t_{\text{days}} \times 86\,400$)
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public record SimulationTimeStep(
        double dtDays,
        double dtYears,
        double dtMonths,
        double dtSeconds
) {

    /**
     * Constructs a canonical {@link SimulationTimeStep} strictly from duration in solar days.
     * Enforces a strictly positive numerical lower bound ($10^{-4}\text{ days} \approx 8.64\text{ s}$)
     * preventing division-by-zero singularities during stiff differential integrations.
     *
     * @param days duration in solar days
     * @return immutable canonical time step representation
     */
    public static SimulationTimeStep ofDays(double days) {
        double d = Math.max(1e-4, days);
        double y = d / PhysicalConstants.DAYS_PER_JULIAN_YEAR;
        double m = d / PhysicalConstants.DAYS_PER_SYNODIC_MONTH;
        double s = d * PhysicalConstants.SECONDS_PER_DAY;
        return new SimulationTimeStep(d, y, m, s);
    }

    /**
     * Constructs a canonical {@link SimulationTimeStep} from duration in Julian years.
     *
     * @param years duration in astronomical Julian years
     * @return immutable canonical time step representation
     */
    public static SimulationTimeStep ofYears(double years) {
        double y = Math.max(1e-6, years);
        double d = y * PhysicalConstants.DAYS_PER_JULIAN_YEAR;
        double m = y * 12.0;
        double s = d * PhysicalConstants.SECONDS_PER_DAY;
        return new SimulationTimeStep(d, y, m, s);
    }

    /**
     * Constructs a canonical {@link SimulationTimeStep} from duration in SI seconds.
     *
     * @param seconds duration in SI seconds
     * @return immutable canonical time step representation
     */
    public static SimulationTimeStep ofSeconds(double seconds) {
        double s = Math.max(1e-2, seconds);
        double d = s / PhysicalConstants.SECONDS_PER_DAY;
        double y = d / PhysicalConstants.DAYS_PER_JULIAN_YEAR;
        double m = d / PhysicalConstants.DAYS_PER_SYNODIC_MONTH;
        return new SimulationTimeStep(d, y, m, s);
    }

    /**
     * Returns the time step in Julian years as a single-precision float for GPU / SIMD kernel buffers.
     *
     * @return float-precision time step in Julian years
     */
    public float dtYearsF() {
        return (float) dtYears;
    }

    /**
     * Returns the time step in solar days as a single-precision float.
     *
     * @return float-precision time step in solar days
     */
    public float dtDaysF() {
        return (float) dtDays;
    }

    /**
     * Returns the time step in synodic months as a single-precision float.
     *
     * @return float-precision time step in synodic months
     */
    public float dtMonthsF() {
        return (float) dtMonths;
    }

    /**
     * Returns the time step in SI seconds as a single-precision float.
     *
     * @return float-precision time step in SI seconds
     */
    public float dtSecondsF() {
        return (float) dtSeconds;
    }
}
