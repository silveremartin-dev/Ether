/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.core.time;

import java.io.Serializable;
import java.util.Locale;

/**
 * <h1>SimulationDate</h1>
 * <p>
 * Dedicated, immutable, strongly-typed temporal representation for macro-historical
 * and planetary simulation chronologies across multi-millennial timescales (-100,000 BP to 2100+ CE).
 * Uses canonical astronomical 360-day year (12 months of 30 days) ensuring strict bitwise determinism
 * and linear step-independent temporal comparisons.
 * </p>
 *
 * @param year calendar year (negative for BCE/BP, positive for CE)
 * @param month calendar month (1-12)
 * @param day calendar day of month (1-30)
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
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
     * @param year calendar year (negative for BCE)
     * @param month calendar month (1-12)
     * @param day calendar day of month (1-30)
     * @return non-null immutable date
     */
    public static SimulationDate of(int year, int month, int day) {
        return new SimulationDate(year, month, day);
    }

    /**
     * Factory method converting an absolute linear day index into a calendar date.
     *
     * @param absoluteDays absolute linear day count
     * @return non-null calendar date
     */
    public static SimulationDate ofDays(long absoluteDays) {
        long totalDays = absoluteDays;
        long year = totalDays / 360L;
        long rem = totalDays % 360L;
        if (rem < 0) {
            rem += 360L;
            year -= 1;
        }
        int month = (int) (rem / 30L) + 1;
        int day = (int) (rem % 30L) + 1;
        return new SimulationDate((int) year, month, day);
    }

    /**
     * Converts this date into a continuous, linear absolute day scalar.
     *
     * @return absolute linear simulation days
     */
    public long toAbsoluteDays() {
        return ((long) year * 360L) + (((long) month - 1L) * 30L) + ((long) day - 1L);
    }

    /**
     * Checks if this date is strictly before the other date.
     *
     * @param other target date
     * @return {@code true} if this date precedes other
     */
    public boolean isBefore(SimulationDate other) {
        if (other == null) return false;
        return this.toAbsoluteDays() < other.toAbsoluteDays();
    }

    /**
     * Checks if this date is strictly after the other date.
     *
     * @param other target date
     * @return {@code true} if this date is subsequent to other
     */
    public boolean isAfter(SimulationDate other) {
        if (other == null) return false;
        return this.toAbsoluteDays() > other.toAbsoluteDays();
    }

    /**
     * Checks if this date reaches or passes the other date.
     *
     * @param other target date
     * @return {@code true} if this >= other
     */
    public boolean isAtOrAfter(SimulationDate other) {
        if (other == null) return false;
        return this.toAbsoluteDays() >= other.toAbsoluteDays();
    }

    /**
     * Checks if this date is prior to or reaches the other date.
     *
     * @param other target date
     * @return {@code true} if this <= other
     */
    public boolean isAtOrBefore(SimulationDate other) {
        if (other == null) return false;
        return this.toAbsoluteDays() <= other.toAbsoluteDays();
    }

    /**
     * Calculates signed days between this date and target date (target - this).
     *
     * @param target target date
     * @return day count delta
     */
    public long daysUntil(SimulationDate target) {
        if (target == null) return 0L;
        return target.toAbsoluteDays() - this.toAbsoluteDays();
    }

    /**
     * Advances date by specified continuous days.
     *
     * @param days days to add (may be negative)
     * @return new advanced SimulationDate
     */
    public SimulationDate plusDays(long days) {
        return ofDays(this.toAbsoluteDays() + days);
    }

    /**
     * Advances date by specified years.
     *
     * @param years years to add
     * @return new advanced SimulationDate
     */
    public SimulationDate plusYears(int years) {
        return of(this.year + years, this.month, this.day);
    }

    @Override
    public int compareTo(SimulationDate o) {
        if (o == null) return 1;
        return Long.compare(this.toAbsoluteDays(), o.toAbsoluteDays());
    }

    @Override
    public String toString() {
        String era = year < 0 ? "BCE" : "CE";
        return String.format(Locale.ROOT, "%04d-%02d-%02d %s", Math.abs(year), month, day, era);
    }
}
