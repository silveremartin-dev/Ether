/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package org.ether.society.core;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Manages simulation time progression with year and month tracking.
 * Starts at 20,000 BC by default.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public class TimeManager {
    private int currentYear;
    private int currentMonth; // 0-11 (0 = January)
    private long totalTicks;

    /**
     * Creates a TimeManager starting at the specified year.
     *
     * @param startYear The starting year (negative for BC)
     */
    public TimeManager(int startYear) {
        this.currentYear = startYear;
        this.currentMonth = 0;
        this.totalTicks = 0;
    }

    /**
     * Advances time by one month.
     */
    public void advanceMonth() {
        currentMonth++;
        if (currentMonth >= 12) {
            currentMonth = 0;
            currentYear++;
        }
        totalTicks++;
    }

    /**
     * Advances time by the specified number of years.
     *
     * @param years Number of years to advance
     */
    public void advanceYears(int years) {
        for (int i = 0; i < years * 12; i++) {
            advanceMonth();
        }
    }

    /**
     * Gets the current year.
     *
     * @return Current year (negative = BC, positive = AD)
     */
    public int getCurrentYear() {
        return currentYear;
    }

    /**
     * Gets the current month (0-11).
     *
     * @return Current month index
     */
    public int getCurrentMonth() {
        return currentMonth;
    }

    /**
     * Gets the current day of the month (1-30).
     *
     * @return Current day (default 1)
     */
    public int getCurrentDay() {
        return 1;
    }

    /**
     * Gets total simulation ticks.
     *
     * @return Total ticks (months)
     */
    public long getTotalTicks() {
        return totalTicks;
    }

    /**
     * Returns a formatted date string with era (BC/AD).
     *
     * @param locale Locale for month formatting
     * @return Formatted date (e.g., "Jan 20000 BC")
     */
    public String getFormattedDate(Locale locale) {
        String era = currentYear < 0 ? "BC" : "AD";
        int year = Math.abs(currentYear);
        Month month = Month.of(currentMonth + 1);
        String monthName = month.getDisplayName(TextStyle.SHORT, locale);
        return String.format("%s %d %s", monthName, year, era);
    }

    /**
     * Returns a formatted date string with default locale (English).
     *
     * @return Formatted date
     */
    public String getFormattedDate() {
        return getFormattedDate(Locale.ENGLISH);
    }

    /**
     * Resets time to the initial starting year.
     *
     * @param startYear The year to reset to
     */
    public void reset(int startYear) {
        this.currentYear = startYear;
        this.currentMonth = 0;
        this.totalTicks = 0;
    }
}

