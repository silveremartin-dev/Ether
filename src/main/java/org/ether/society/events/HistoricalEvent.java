/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.ether.society.core.time.SimulationDate;

/**
 * <h1>Historical Event</h1>
 * <p>
 * Core operational component for the Ether civilizational and planetary simulation framework.<br>
 * Represents an exact temporal milestone with deterministic coordinates and spatiotemporal activation.
 * </p>
 * 
 * @param year the target calendar year (negative for BCE, positive for CE)
 * @param month the calendar month (1-12)
 * @param day the calendar day (1-31)
 * @param title the event title or classification
 * @param message the descriptive summary of the historical event
 * @param latitude the geographic epicenter latitude in degrees
 * @param longitude the geographic epicenter longitude in degrees
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public record HistoricalEvent(int year, int month, int day, String title, String message, double latitude, double longitude) implements SimulationTemporalEvent {

    /**
     * Constructs a historical event with year, title, and message, defaulting to January 1st at coordinates (0.0, 0.0).
     *
     * @param year the year parameter (int)
     * @param title the title parameter (String)
     * @param message the message parameter (String)
     */
    public HistoricalEvent(int year, String title, String message) {
        this(year, 1, 1, title, message, 0.0, 0.0);
    }

    /**
     * Constructs a historical event with year, title, message, and geographic coordinates, defaulting to January 1st.
     *
     * @param year the year parameter (int)
     * @param title the title parameter (String)
     * @param message the message parameter (String)
     * @param latitude the latitude parameter (double)
     * @param longitude the longitude parameter (double)
     */
    public HistoricalEvent(int year, String title, String message, double latitude, double longitude) {
        this(year, 1, 1, title, message, latitude, longitude);
    }

    /**
     * Converts calendar date coordinates into an absolute linear simulation day index.
     *
     * @param year calendar year (negative for BCE)
     * @param month calendar month (1-12)
     * @param day calendar day of month (1-30)
     * @return absolute simulation day index
     */
    public static long toAbsoluteDays(int year, int month, int day) {
        int m = (month > 0 && month <= 12) ? (month - 1) : 0;
        int d = (day > 0 && day <= 30) ? (day - 1) : 0;
        return ((long) year * 360L) + ((long) m * 30L) + d;
    }

    @Override
    public String getId() {
        return "HIST_" + year + "_" + month + "_" + day + "_" + (title != null ? title.hashCode() : 0);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public org.ether.society.model.EventCategory getCategory() {
        return org.ether.society.model.EventCategory.CHRONICLE_MILESTONE;
    }

    @Override
    public SimulationDate getStartDate() {
        return SimulationDate.of(this.year, this.month, this.day);
    }

    @Override
    public SimulationDate getEndDate() {
        return getStartDate().plusDays(1);
    }

    @Override
    public int getYear() {
        return year;
    }

    @Override
    public int getMonth() {
        return month;
    }

    @Override
    public int getDay() {
        return day;
    }

    @Override
    public int getDurationDays() {
        return 1;
    }

    @Override
    public long getAbsoluteStartDays() {
        return toAbsoluteDays(this.year, this.month, this.day);
    }

    @Override
    public long getAbsoluteEndDays() {
        return getAbsoluteStartDays() + 1L;
    }

    @Override
    public boolean isActive(int currentYear, int currentMonth, int currentDay) {
        int normSimMonth = (currentMonth < 12) ? (currentMonth + 1) : currentMonth;
        long currentSimDays = toAbsoluteDays(currentYear, normSimMonth, currentDay);
        return currentSimDays == getAbsoluteStartDays();
    }

    @Override
    public boolean isExpired(int currentYear, int currentMonth, int currentDay) {
        int normSimMonth = (currentMonth < 12) ? (currentMonth + 1) : currentMonth;
        long currentSimDays = toAbsoluteDays(currentYear, normSimMonth, currentDay);
        return currentSimDays > getAbsoluteStartDays();
    }

    @Override
    public double getTemporalIntensity(int currentYear, int currentMonth, int currentDay) {
        return isActive(currentYear, currentMonth, currentDay) ? 1.0 : 0.0;
    }

    /**
     * Checks if this historical milestone is due based on continuous linear date comparison.
     *
     * @param currentYear current simulation calendar year
     * @param currentMonth current simulation month index (0-11) or (1-12)
     * @param currentDay current simulation day of month (1-31)
     * @return {@code true} if the current simulation date has reached or passed this event date
     */
    @Override
    public boolean isDue(int currentYear, int currentMonth, int currentDay) {
        int normSimMonth = (currentMonth < 12) ? (currentMonth + 1) : currentMonth;
        long currentSimDays = toAbsoluteDays(currentYear, normSimMonth, currentDay);
        long startDays = toAbsoluteDays(this.year, this.month, this.day);
        return currentSimDays >= startDays;
    }
}

