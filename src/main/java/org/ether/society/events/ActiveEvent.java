/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

/**
 * Represents a spatially-anchored simulation event with geographic coordinates,
 * temporal resolution timestamp (Year/Month/Day), and visual beacon metadata.
 */
public class ActiveEvent {
    private final String id;
    private final String title;
    private final String type; // FLOOD, VOLCANO, FAMINE, PANDEMIC, HEATWAVE, METEOR, EARTHQUAKE, ECOLOGICAL, GOD_MODE
    private final double latitude;
    private final double longitude;
    private final int year;
    private final int month; // 0-11
    private final int day;   // 1-30
    private final long createdAtMs;
    private final double durationSeconds;
    private final double magnitude;
    private final int durationDays;

    public ActiveEvent(String id, String title, String type, double latitude, double longitude, int year, int month, int day, double durationSeconds, double magnitude, int durationDays) {
        this.id = id;
        this.title = title;
        this.type = type != null ? type : "GENERIC";
        this.latitude = latitude;
        this.longitude = longitude;
        this.year = year;
        this.month = month;
        this.day = Math.max(1, Math.min(30, day));
        this.createdAtMs = System.currentTimeMillis();
        this.durationSeconds = durationSeconds > 0 ? durationSeconds : 15.0;
        this.magnitude = magnitude > 0 ? magnitude : 5.0;
        this.durationDays = durationDays > 0 ? durationDays : 30;
    }

    public ActiveEvent(String id, String title, String type, double latitude, double longitude, int year, int month, int day, double durationSeconds, double magnitude) {
        this(id, title, type, latitude, longitude, year, month, day, durationSeconds, magnitude, 30);
    }

    public ActiveEvent(String id, String title, String type, double latitude, double longitude, int year, int month, int day, double durationSeconds) {
        this(id, title, type, latitude, longitude, year, month, day, durationSeconds, 5.0, 30);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getDay() { return day; }
    public long getCreatedAtMs() { return createdAtMs; }
    public double getDurationSeconds() { return durationSeconds; }
    public double getMagnitude() { return magnitude; }
    public int getDurationDays() { return durationDays; }

    public boolean isExpired() {
        return (System.currentTimeMillis() - createdAtMs) > (durationSeconds * 1000L);
    }

    public String getFormattedDate() {
        return String.format("An %d - M.%02d D.%02d", year, month + 1, day);
    }

    public String getFormattedLocation() {
        String latDir = latitude >= 0 ? "N" : "S";
        String lngDir = longitude >= 0 ? "E" : "W";
        return String.format("Lat: %.2f°%s, Lng: %.2f°%s", Math.abs(latitude), latDir, Math.abs(longitude), lngDir);
    }

    public String getIntensityLabel() {
        if (magnitude >= 8.0) return "Critique";
        if (magnitude >= 6.5) return "Élevée";
        if (magnitude >= 4.5) return "Modérée";
        return "Faible";
    }

    public String getIntensityBadgeColor() {
        if (magnitude >= 8.0) return "#ef4444";
        if (magnitude >= 6.5) return "#f97316";
        if (magnitude >= 4.5) return "#eab308";
        return "#22c55e";
    }

    public String getFormattedCoordinates() {
        String latDir = latitude >= 0 ? "N" : "S";
        String lngDir = longitude >= 0 ? "E" : "W";
        return String.format(java.util.Locale.ROOT, "Lat: %.2f°%s, Lng: %.2f°%s", Math.abs(latitude), latDir, Math.abs(longitude), lngDir);
    }

    public String getFullMessage() {
        return String.format("[%s] %s (%s)", getFormattedDate(), title, getFormattedLocation());
    }
}

