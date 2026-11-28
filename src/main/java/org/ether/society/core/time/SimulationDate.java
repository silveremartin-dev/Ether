/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.time;

import org.ether.society.model.PhysicalConstants;

import java.io.Serializable;
import java.util.Locale;

/**
 * <h1>SimulationDate</h1>
 * <p>
 * Immutable, strongly-typed temporal coordinate for macro-historical and planetary simulation
 * chronologies across multi-millennial timescales (\(-100\,000\,\text{BP}\) to \(2100+\,\text{CE}\)).
 * Anchored strictly to the IAU Julian year (\(365.25\,\text{days/yr}\)) and synodic month
 * (\(30.4375\,\text{days/mo}\)) in compliance with {@code AGENTS.md} §3 Contract 1.
 * </p>
 *
 * @param year  calendar year (negative for BCE/BP, positive for CE)
 * @param month calendar month (1–12)
 * @param day   calendar day of month (1–30)
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public record SimulationDate(int year, int month, int day) implements Comparable<SimulationDate>, Serializable {

    private static final long serialVersionUID = 1L;

    public SimulationDate {
        month = Math.max(1, Math.min(12, month));
        day = Math.max(1, Math.min(30, day));
    }

    /**
     * Factory method creating a date with normalized month and day bounds.
     *
     * @param year  calendar year (negative for BCE)
     * @param month calendar month (1–12)
     * @param day   calendar day of month (1–30)
     * @return non-null immutable date
     */
    public static SimulationDate of(int year, int month, int day) {
        return new SimulationDate(year, month, day);
    }

    /**
     * Factory method converting an absolute linear Julian day index into a calendar date.
     *
     * @param absoluteDays absolute linear Julian day count
     * @return non-null calendar date
     */
    public static SimulationDate ofDays(long absoluteDays) {
        double daysPerYear = PhysicalConstants.DAYS_PER_JULIAN_YEAR;
        double daysPerMonth = PhysicalConstants.DAYS_PER_SYNODIC_MONTH;
        long year = (long) Math.floor((absoluteDays + 1.0e-6) / daysPerYear);
        double remYearDays = Math.max(0.0, absoluteDays - year * daysPerYear);
        int monthIndex = Math.min(11, (int) Math.floor((remYearDays + 1.0e-6) / daysPerMonth));
        double remMonthDays = Math.max(0.0, remYearDays - monthIndex * daysPerMonth);
        int day = Math.clamp((int) Math.round(remMonthDays) + 1, 1, 30);
        return new SimulationDate((int) year, monthIndex + 1, day);
    }

    /**
     * Converts this date into a continuous, linear absolute Julian day index.
     *
     * @return absolute linear Julian days
     */
    public long toAbsoluteDays() {
        double exactDays = ((double) year * PhysicalConstants.DAYS_PER_JULIAN_YEAR)
                + (((double) month - 1.0) * PhysicalConstants.DAYS_PER_SYNODIC_MONTH)
                + ((double) day - 1.0);
        return Math.round(exactDays);
    }

    /**
     * Checks if this date is strictly before the other date.
     *
     * @param other target date
     * @return {@code true} if this date precedes other
     */
    public boolean isBefore(SimulationDate other) {
        if (other == null) return false;
        return this.compareTo(other) < 0;
    }

    /**
     * Checks if this date is strictly after the other date.
     *
     * @param other target date
     * @return {@code true} if this date is subsequent to other
     */
    public boolean isAfter(SimulationDate other) {
        if (other == null) return false;
        return this.compareTo(other) > 0;
    }

    /**
     * Checks if this date reaches or passes the other date.
     *
     * @param other target date
     * @return {@code true} if {@code this >= other}
     */
    public boolean isAtOrAfter(SimulationDate other) {
        if (other == null) return false;
        return this.compareTo(other) >= 0;
    }

    /**
     * Checks if this date is prior to or reaches the other date.
     *
     * @param other target date
     * @return {@code true} if {@code this <= other}
     */
    public boolean isAtOrBefore(SimulationDate other) {
        if (other == null) return false;
        return this.compareTo(other) <= 0;
    }

    /**
     * Calculates signed Julian days between this date and target date ({@code target - this}).
     *
     * @param target target date
     * @return day count delta
     */
    public long daysUntil(SimulationDate target) {
        if (target == null) return 0L;
        return target.toAbsoluteDays() - this.toAbsoluteDays();
    }

    /**
     * Advances date by specified continuous Julian days.
     *
     * @param days days to add (may be negative)
     * @return new advanced {@link SimulationDate}
     */
    public SimulationDate plusDays(long days) {
        return ofDays(this.toAbsoluteDays() + days);
    }

    /**
     * Advances date by specified Julian years.
     *
     * @param years years to add
     * @return new advanced {@link SimulationDate}
     */
    public SimulationDate plusYears(int years) {
        return of(this.year + years, this.month, this.day);
    }

    @Override
    public int compareTo(SimulationDate o) {
        if (o == null) return 1;
        int cmpY = Integer.compare(this.year, o.year);
        if (cmpY != 0) return cmpY;
        int cmpM = Integer.compare(this.month, o.month);
        if (cmpM != 0) return cmpM;
        return Integer.compare(this.day, o.day);
    }

    @Override
    public String toString() {
        String era = year < 0 ? "BCE" : "CE";
        return String.format(Locale.ROOT, "%04d-%02d-%02d %s", Math.abs(year), month, day, era);
    }
}
