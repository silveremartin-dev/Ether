*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

/**
 * Represents a spatially-anchored simulation event with geographic coordinates,
 * temporal resolution timestamp (Year/Month/Day), and visual beacon metadata.
 */
public class ActiveEvent {
    /** Internal state: id (String). */
    private final String id;
    /** Internal state: title (String). */
    private final String title;
    private final String type; // FLOOD, VOLCANO, FAMINE, PANDEMIC, HEATWAVE, METEOR, EARTHQUAKE, ECOLOGICAL, HISTORICAL, HISTORICAL_LEADER, GOD_MODE, etc.
    /** Internal state: latitude (double). */
    private final double latitude;
    /** Internal state: longitude (double). */
    private final double longitude;
    /** Internal state: year (int). */
    private final int year;
    private final int month; // 0-11
    private final int day;   // 1-30
    /** Internal state: created at ms (long). */
    private final long createdAtMs;
    /** Internal state: duration seconds (double). */
    private final double durationSeconds;
    /** Internal state: magnitude (double). */
    private final double magnitude;
    /** Internal state: duration days (int). */
    private final int durationDays;
    private LeaderArchetype leaderArchetype;
    private HistoricalIntervention intervention;

    /**
     * Active event.
     *
     * @param id the id parameter (String)
     * @param title the title parameter (String)
     * @param type the type parameter (String)
     * @param latitude the latitude parameter (double)
     * @param longitude the longitude parameter (double)
     * @param year the year parameter (int)
     * @param month the month parameter (int)
     * @param day the day parameter (int)
     * @param durationSeconds the duration seconds parameter (double)
     * @param magnitude the magnitude parameter (double)
     * @param durationDays the duration days parameter (int)
     */
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

    /**
     * Active event.
     *
     * @param id the id parameter (String)
     * @param title the title parameter (String)
     * @param type the type parameter (String)
     * @param latitude the latitude parameter (double)
     * @param longitude the longitude parameter (double)
     * @param year the year parameter (int)
     * @param month the month parameter (int)
     * @param day the day parameter (int)
     * @param durationSeconds the duration seconds parameter (double)
     * @param magnitude the magnitude parameter (double)
     */
    public ActiveEvent(String id, String title, String type, double latitude, double longitude, int year, int month, int day, double durationSeconds, double magnitude) {
        this(id, title, type, latitude, longitude, year, month, day, durationSeconds, magnitude, 30);
    }

    /**
     * Active event.
     *
     * @param id the id parameter (String)
     * @param title the title parameter (String)
     * @param type the type parameter (String)
     * @param latitude the latitude parameter (double)
     * @param longitude the longitude parameter (double)
     * @param year the year parameter (int)
     * @param month the month parameter (int)
     * @param day the day parameter (int)
     * @param durationSeconds the duration seconds parameter (double)
     */
    public ActiveEvent(String id, String title, String type, double latitude, double longitude, int year, int month, int day, double durationSeconds) {
        this(id, title, type, latitude, longitude, year, month, day, durationSeconds, 5.0, 30);
    }

    /**
     * Active event.
     *
     * @param intervention the intervention parameter (HistoricalIntervention)
     * @param year the year parameter (int)
     * @param month the month parameter (int)
     * @param day the day parameter (int)
     */
    public ActiveEvent(HistoricalIntervention intervention, int year, int month, int day) {
        this(
            intervention.getId(),
            intervention.getArchetype().getIcon() + " " + intervention.getName() + " : " + intervention.getDescription(),
            "HISTORICAL_LEADER",
            intervention.getLatitude(),
            intervention.getLongitude(),
            year,
            month,
            day,
            30.0,
            intervention.getMagnitude(),
            intervention.getDurationYears() * 365
        );
        this.leaderArchetype = intervention.getArchetype();
        this.intervention = intervention;
    }

    /**
     * Get id.
     *
     * @return the resulting computation or state reference
     */
    public String getId() { return id; }
    /**
     * Get title.
     *
     * @return the resulting computation or state reference
     */
    public String getTitle() { return title; }
    /**
     * Get type.
     *
     * @return the resulting computation or state reference
     */
    public String getType() { return type; }
    /**
     * Get latitude.
     *
     * @return the resulting computation or state reference
     */
    public double getLatitude() { return latitude; }
    /**
     * Get longitude.
     *
     * @return the resulting computation or state reference
     */
    public double getLongitude() { return longitude; }
    /**
     * Get year.
     *
     * @return the resulting computation or state reference
     */
    public int getYear() { return year; }
    /**
     * Get month.
     *
     * @return the resulting computation or state reference
     */
    public int getMonth() { return month; }
    /**
     * Get day.
     *
     * @return the resulting computation or state reference
     */
    public int getDay() { return day; }
    /**
     * Get created at ms.
     *
     * @return the resulting computation or state reference
     */
    public long getCreatedAtMs() { return createdAtMs; }
    /**
     * Get duration seconds.
     *
     * @return the resulting computation or state reference
     */
    public double getDurationSeconds() { return durationSeconds; }
    /**
     * Get magnitude.
     *
     * @return the resulting computation or state reference
     */
    public double getMagnitude() { return magnitude; }
    /**
     * Get duration days.
     *
     * @return the resulting computation or state reference
     */
    public int getDurationDays() { return durationDays; }

    /**
     * Get leader archetype.
     *
     * @return the resulting computation or state reference
     */
    public LeaderArchetype getLeaderArchetype() { return leaderArchetype; }
    /**
     * Set leader archetype operation.
     * <p>
     * Executes operational logic for {@code ActiveEvent} within the simulation engine framework.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param leaderArchetype the leader archetype argument (LeaderArchetype)
     */
    public void setLeaderArchetype(LeaderArchetype leaderArchetype) { this.leaderArchetype = leaderArchetype; }

    /**
     * Get intervention.
     *
     * @return the resulting computation or state reference
     */
    public HistoricalIntervention getIntervention() { return intervention; }
    /**
     * Set intervention operation.
     * <p>
     * Executes operational logic for {@code ActiveEvent} within the simulation engine framework.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param intervention the intervention argument (HistoricalIntervention)
     */
    public void setIntervention(HistoricalIntervention intervention) { this.intervention = intervention; }

    /**
     * Is geophysical.
     *
     * @return the resulting computation or state reference
     */
    public boolean isGeophysical() {
        if (type == null) return true;
        String upper = type.toUpperCase();
        if (isHistoricalLeader() || isProceduralEmergence()) return false;
        return upper.contains("FLOOD") || upper.contains("VOLCANO") || upper.contains("FAMINE") ||
               upper.contains("PLAGUE") || upper.contains("PANDEMIC") || upper.contains("DROUGHT") ||
               upper.contains("EARTHQUAKE") || upper.contains("METEOR") || upper.contains("HEATWAVE") ||
               upper.contains("TSUNAMI") || upper.contains("ICE_AGE") || upper.contains("ECOLOGICAL") ||
               upper.contains("DISASTER") || upper.contains("ECO_") || upper.contains("NUCLEAR");
    }

    /**
     * Is historical leader.
     *
     * @return the resulting computation or state reference
     */
    public boolean isHistoricalLeader() {
        if (type == null) return false;
        String upper = type.toUpperCase();
        return upper.contains("HISTORICAL_LEADER") || upper.contains("LEADER") || upper.contains("ARCHETYPE") ||
               upper.contains("PROC_LEADER") || leaderArchetype != null || intervention != null;
    }

    /**
     * Is procedural emergence.
     *
     * @return the resulting computation or state reference
     */
    public boolean isProceduralEmergence() {
        if (type == null) return false;
        String upper = type.toUpperCase();
        return upper.contains("PROCEDURAL") || upper.contains("CHRONICLE") || upper.contains("EMERGENCE") ||
               upper.contains("NARRATIVE") || upper.contains("MILESTONE") || upper.contains("POLITY_EMERGENCE");
    }

    /**
     * Is expired.
     *
     * @return the resulting computation or state reference
     */
    public boolean isExpired() {
        return (System.currentTimeMillis() - createdAtMs) > (durationSeconds * 1000L);
    }

    /**
     * Get formatted date.
     *
     * @return the resulting computation or state reference
     */
    public String getFormattedDate() {
        String template = org.ether.society.i18n.I18n.getOrDefault("event.date_format", "Yr %d - M.%02d D.%02d");
        return String.format(template, year, month + 1, day);
    }

    /**
     * Get formatted location.
     *
     * @return the resulting computation or state reference
     */
    public String getFormattedLocation() {
        if (Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001) {
            return "—";
        }
        String latDir = latitude >= 0 ? "N" : "S";
        String lngDir = longitude >= 0 ? "E" : "W";
        return String.format("Lat: %.2f°%s, Lng: %.2f°%s", Math.abs(latitude), latDir, Math.abs(longitude), lngDir);
    }

    /**
     * Get intensity label.
     *
     * @return the resulting computation or state reference
     */
    public String getIntensityLabel() {
        if (magnitude >= 8.0) return org.ether.society.i18n.I18n.getOrDefault("event.intensity.critical", "Critical");
        if (magnitude >= 6.5) return org.ether.society.i18n.I18n.getOrDefault("event.intensity.high", "High");
        if (magnitude >= 4.5) return org.ether.society.i18n.I18n.getOrDefault("event.intensity.moderate", "Moderate");
        return org.ether.society.i18n.I18n.getOrDefault("event.intensity.low", "Low");
    }

    /**
     * Get intensity badge color.
     *
     * @return the resulting computation or state reference
     */
    public String getIntensityBadgeColor() {
        if (isHistoricalLeader()) {
            if (magnitude >= 8.5) return "#8b5cf6"; // Purple for legendary leaders
            return "#3b82f6"; // Blue for leaders
        }
        if (isProceduralEmergence()) {
            return "#10b981"; // Emerald green for world narrative emergences
        }
        if (magnitude >= 8.0) return "#ef4444";
        if (magnitude >= 6.5) return "#f97316";
        if (magnitude >= 4.5) return "#eab308";
        return "#22c55e";
    }

    /**
     * Get formatted coordinates.
     *
     * @return the resulting computation or state reference
     */
    public String getFormattedCoordinates() {
        if (Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001) {
            return "—";
        }
        String latDir = latitude >= 0 ? "N" : "S";
        String lngDir = longitude >= 0 ? "E" : "W";
        return String.format(java.util.Locale.ROOT, "Lat: %.2f°%s, Lng: %.2f°%s", Math.abs(latitude), latDir, Math.abs(longitude), lngDir);
    }

    /**
     * Get full message.
     *
     * @return the resulting computation or state reference
     */
    public String getFullMessage() {
        return String.format("[%s] %s (%s)", getFormattedDate(), title, getFormattedLocation());
    }
}
