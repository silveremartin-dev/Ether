/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.core.time.SimulationDate;
import org.ether.society.model.EventCategory;

/**
 * <h1>SimulationTemporalEvent</h1>
 * <p>
 * Universal standard interface contract for all temporal and spatial simulation events
 * (climate cataclysms, historical milestones, leader interventions, anthropic disasters).
 * Enforces unified monotonic linear temporal triggering, active window evaluation,
 * and clean expiration criteria across all discrete simulation time steps.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public interface SimulationTemporalEvent {

    /**
     * Unique identifier of the event.
     *
     * @return unique string identifier
     */
    String getId();

    /**
     * Descriptive title or label of the event.
     *
     * @return human-readable title
     */
    String getTitle();

    /**
     * Event category (NATURAL, ANTHROPIC, CHRONICLE_MILESTONE).
     *
     * @return non-null category
     */
    EventCategory getCategory();

    /**
     * Dedicated start date object.
     *
     * @return non-null start SimulationDate
     */
    SimulationDate getStartDate();

    /**
     * Dedicated expiration date object.
     *
     * @return non-null expiration SimulationDate
     */
    SimulationDate getEndDate();

    /**
     * Event start year (negative for BCE).
     *
     * @return start year
     */
    int getYear();

    /**
     * Event start month (1-12).
     *
     * @return start month
     */
    int getMonth();

    /**
     * Event start day of month (1-30/31).
     *
     * @return start day
     */
    int getDay();

    /**
     * Active physical duration in simulation days (&lt;= 0 for permanent milestones).
     *
     * @return duration in simulation days
     */
    int getDurationDays();

    /**
     * Absolute linear simulation start day index.
     *
     * @return absolute start day index
     */
    long getAbsoluteStartDays();

    /**
     * Absolute linear simulation expiration day index.
     *
     * @return absolute expiration day index
     */
    long getAbsoluteEndDays();

    /**
     * Checks if this event is due to trigger at or after the given simulation date.
     *
     * @param currentYear simulation calendar year
     * @param currentMonth simulation month index (0-11)
     * @param currentDay simulation day of month (1-30)
     * @return {@code true} if current date reaches or passes the start date
     */
    boolean isDue(int currentYear, int currentMonth, int currentDay);

    /**
     * Checks if this event is actively ongoing during the given simulation date.
     *
     * @param currentYear simulation calendar year
     * @param currentMonth simulation month index (0-11)
     * @param currentDay simulation day of month (1-30)
     * @return {@code true} if current date is within the active window [start, end[
     */
    boolean isActive(int currentYear, int currentMonth, int currentDay);

    /**
     * Checks if this event has fully expired and ceased physical impact.
     *
     * @param currentYear simulation calendar year
     * @param currentMonth simulation month index (0-11)
     * @param currentDay simulation day of month (1-30)
     * @return {@code true} if simulation date has reached or passed expiration date
     */
    boolean isExpired(int currentYear, int currentMonth, int currentDay);

    /**
     * Calculates the continuous temporal intensity envelope [0.0, 1.0] at the given date.
     * Evaluates continuous smooth profiles (bell curve, exponential decay, or sigmoidal plateau).
     *
     * @param currentYear simulation calendar year
     * @param currentMonth simulation month index (0-11)
     * @param currentDay simulation day of month (1-30)
     * @return normalized intensity multiplier between 0.0 and 1.0
     */
    double getTemporalIntensity(int currentYear, int currentMonth, int currentDay);
}
