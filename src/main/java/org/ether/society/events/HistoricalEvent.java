/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

/**
 * Represents a predefined historical event.
 */
public record HistoricalEvent(int year, String title, String message, double latitude, double longitude) {
    public HistoricalEvent(int year, String title, String message) {
        this(year, title, message, 0.0, 0.0);
    }
}

