/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

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
public record HistoricalEvent(int year, int month, int day, String title, String message, double latitude, double longitude) {

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
     * Checks if this historical event matches the given temporal coordinate.
     *
     * @param currentYear current simulation calendar year
     * @param currentMonth current simulation month index (0-11) or (1-12)
     * @param currentDay current simulation day of month (1-31)
     * @return {@code true} if the current tick matches or reaches this event date
     */
    public boolean isDue(int currentYear, int currentMonth, int currentDay) {
        if (this.year != currentYear) {
            return false;
        }
        // Normalize simulation month: SimulationTimeCoordinator uses 0-11, calendar uses 1-12
        int normSimMonth = (currentMonth < 12) ? (currentMonth + 1) : currentMonth;
        return this.month == normSimMonth && (this.day == currentDay || currentDay <= 0);
    }
}

