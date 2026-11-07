/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.io.Serializable;
import java.util.Locale;

/**
 * <h1>ClimateEvent</h1>
 * <p>
 * Core operational data model representing a scheduled biophysical cataclysm,
 * climatic shock, sociological intervention, or technological milestone in the simulation calendar.
 * </p>
 * <p>
 * Fully supports active duration modeling:
 * <ul>
 *   <li><b>Permanent events</b> ({@code durationDays <= 0}, e.g. technological breakthroughs, institutional reforms):
 *       induces irreversible structural state changes in physical capital \(K\), energy grid capacity, or state capacity.</li>
 *   <li><b>Transient cataclysms</b> ({@code durationDays > 0}, e.g. volcanic eruptions, earthquakes, pandemics):
 *       active over physical time with atmospheric residence e-folding half-lives or SEIR epidemic curves.</li>
 * </ul>
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ClimateEvent implements Serializable {
    private static final long serialVersionUID = 2L;

    /** Internal state: canonical type identifier (VOLCANO, EARTHQUAKE, PANDEMIC, TECH_SINGULARITY, etc.). */
    private String type;
    /** Internal state: descriptive name or historical title. */
    private String name;
    /** Internal state: target activation calendar year CE/BCE. */
    private int year;
    /** Internal state: geographic epicenter latitude [-90.0, +90.0]. */
    private double latitude;
    /** Internal state: geographic epicenter longitude [-180.0, +180.0]. */
    private double longitude;
    /** Internal state: hypocentral depth in km (for seismic and volcanic phenomena). */
    private double depth;
    /** Internal state: normalized magnitude or intensity scale [0.0, 10.0]. */
    private double magnitude;
    /** Internal state: active duration in simulation days (<= 0 indicates permanent milestone). */
    private int durationDays;

    /**
     * Default constructor defaulting to a standard volcanic eruption.
     */
    public ClimateEvent() {
        this("VOLCANO", "Volcanic Eruption", 0, 0.0, 0.0, 10.0, 5.0, getDefaultDurationDaysForType("VOLCANO"));
    }

    /**
     * Constructs a scheduled event with automatic type-based default duration.
     *
     * @param type event typology
     * @param name descriptive name
     * @param year chronological year
     * @param latitude epicenter latitude
     * @param longitude epicenter longitude
     * @param depth hypocentral depth in km
     * @param magnitude intensity or magnitude
     */
    public ClimateEvent(String type, String name, int year, double latitude, double longitude, double depth, double magnitude) {
        this(type, name, year, latitude, longitude, depth, magnitude, getDefaultDurationDaysForType(type));
    }

    /**
     * Constructs a scheduled event with explicit duration.
     *
     * @param type event typology
     * @param name descriptive name
     * @param year chronological year
     * @param latitude epicenter latitude
     * @param longitude epicenter longitude
     * @param depth hypocentral depth in km
     * @param magnitude intensity or magnitude
     * @param durationDays active duration in simulation days (<= 0 for permanent)
     */
    public ClimateEvent(String type, String name, int year, double latitude, double longitude, double depth, double magnitude, int durationDays) {
        this.type = type != null ? type : "GENERIC";
        this.name = name != null ? name : "Event";
        this.year = year;
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = depth;
        this.magnitude = magnitude;
        this.durationDays = durationDays;
    }

    /**
     * Evaluates default physical active duration in days from event typology.
     *
     * @param type event type
     * @return duration in days (-1 for permanent milestones)
     */
    public static int getDefaultDurationDaysForType(String type) {
        if (type == null) return 30;
        String upper = type.toUpperCase(Locale.ROOT);

        // Permanent technological, civilizational, and institutional milestones (Ratchet effect)
        if (upper.contains("TECH_SINGULARITY") || upper.contains("FUSION") || upper.contains("RENAISSANCE")
                || upper.contains("MILESTONE") || upper.contains("HISTORICAL") || upper.contains("GOLDEN_AGE")) {
            return -1;
        }

        return switch (upper) {
            case "VOLCANO", "DISASTER_VOLCANO" -> 365; // Active volcanic eruptive year with stratospheric SO2 injection
            case "EARTHQUAKE", "DISASTER_EARTHQUAKE" -> 3; // Mechanical seismic shock and main aftershocks
            case "TSUNAMI", "DISASTER_TSUNAMI" -> 1; // Instantaneous coastal ocean wave submersion
            case "FLOOD", "DISASTER_FLOOD" -> 30; // Seasonal alluvial riverine cresting
            case "DROUGHT", "DISASTER_DROUGHT" -> 180; // Extended 6-month seasonal drought
            case "FAMINE", "DISASTER_FAMINE" -> 180; // Crop deficit season
            case "HEATWAVE", "DISASTER_HEATWAVE" -> 30; // Severe monthly heat dome
            case "PANDEMIC", "DISASTER_PANDEMIC", "EPIDEMIC", "PLAGUE" -> 730; // 2-year SEIR transmission curve
            case "METEOR", "DISASTER_METEOR" -> 7; // Prompt atmospheric shockwave and cratering
            case "ICE_AGE", "DISASTER_ICE_AGE", "NUCLEAR_WINTER", "DISASTER_NUCLEAR_WINTER" -> 3650; // Decadal decrescendo
            case "NUCLEAR_STRIKE", "DISASTER_NUCLEAR_STRIKE" -> 0; // Prompt nuclear blast (winter handled via soot)
            case "SOLAR_EMP", "DISASTER_SOLAR_EMP" -> 30; // Geomagnetic grid disruption
            case "CYBER_ATTACK", "DISASTER_CYBER_ATTACK" -> 14; // Telecommunication outage
            case "ECONOMIC_CRASH", "DISASTER_ECONOMIC_CRASH" -> 365; // Financial liquidity freeze
            case "BIODIVERSITY_COLLAPSE", "DISASTER_BIODIVERSITY_COLLAPSE" -> 3650; // Ecological extinction horizon
            case "GEOENGINEERING", "DISASTER_GEOENGINEERING" -> 730; // Stratospheric aerosol deployment
            case "ALIEN_CONTACT", "DISASTER_ALIEN_CONTACT" -> -1; // Permanent civilizational paradigm shift
            case "DOME_BREACH", "LAVA_TUBE_COLLAPSE" -> 1; // Prompt habitat structural failure
            case "ECLSS_FAILURE" -> 30; // Life support system degradation
            case "AEROSTAT_PUNCTURE" -> 14; // Buoyancy loss
            case "LEADER_MILITARY_CONQUEROR", "LEADER_TOTALITARIAN_PURGER" -> 7300; // 20-year reign
            case "LEADER_INFRASTRUCTURE_BUILDER", "LEADER_HYDRAULIC_AGRARIAN_INNOVATOR" -> 10950; // 30-year infrastructure lifecycle
            case "LEADER_INSTITUTIONAL_REFORMER", "LEADER_MORAL_RELIGIOUS_SAGE" -> 14600; // 40-year generational reform
            default -> 30;
        };
    }

    /**
     * Checks if this event represents an irreversible permanent physical or institutional milestone.
     *
     * @return {@code true} if duration is permanent (<= 0)
     */
    public boolean isPermanent() {
        return durationDays <= 0;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDepth() {
        return depth;
    }

    public void setDepth(double depth) {
        this.depth = depth;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    @Override
    public String toString() {
        String durStr = isPermanent() ? "Permanent (∞)" : durationDays + " d";
        return String.format(Locale.ROOT, "%s (%d) [%.2f°, %.2f° Mag: %.1f, Dur: %s]", name, year, latitude, longitude, magnitude, durStr);
    }
}
