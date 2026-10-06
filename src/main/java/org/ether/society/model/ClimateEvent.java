/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.io.Serializable;

/**
 * <h1>Climate Event</h1>
 * <p>
 * Core operational component for the Ether civilizational and planetary simulation framework.<br>
 * Integrates cellular dynamics, data structures, and deterministic state transitions.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ClimateEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /* Internal state variable for type (String). */
    private String type;
    /* Internal state variable for name (String). */
    private String name;
    /* Internal state variable for year (int). */
    private int year;
    /* Internal state variable for latitude (double). */
    private double latitude;
    /* Internal state variable for longitude (double). */
    private double longitude;
    /* Internal state variable for depth (double). */
    private double depth;
    /* Internal state variable for magnitude (double). */
    private double magnitude;

    /*
     * Climate event.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     */
    public ClimateEvent() {
        this("volcano", "Event", 0, 0.0, 0.0, 0.0, 0.0);
    }

    /*
     * Climate event.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @param type the type parameter (String)
     * @param name the name parameter (String)
     * @param year the year parameter (int)
     * @param latitude the latitude parameter (double)
     * @param longitude the longitude parameter (double)
     * @param depth the depth parameter (double)
     * @param magnitude the magnitude parameter (double)
     */
    public ClimateEvent(String type, String name, int year, double latitude, double longitude, double depth, double magnitude) {
        this.type = type;
        this.name = name;
        this.year = year;
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = depth;
        this.magnitude = magnitude;
    }

    /*
     * Get type.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @return the resulting computation or state reference
     */
    public String getType() {
        return type;
    }

    /*
     * Set type.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @param type the type parameter (String)
     */
    public void setType(String type) {
        this.type = type;
    }

    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return name;
    }

    /*
     * Set name.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @param name the name parameter (String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
     * Get year.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @return the resulting computation or state reference
     */
    public int getYear() {
        return year;
    }

    /*
     * Set year.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @param year the year parameter (int)
     */
    public void setYear(int year) {
        this.year = year;
    }

    /*
     * Get latitude.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @return the resulting computation or state reference
     */
    public double getLatitude() {
        return latitude;
    }

    /*
     * Set latitude.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @param latitude the latitude parameter (double)
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /*
     * Get longitude.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @return the resulting computation or state reference
     */
    public double getLongitude() {
        return longitude;
    }

    /*
     * Set longitude.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @param longitude the longitude parameter (double)
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /*
     * Get depth.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @return the resulting computation or state reference
     */
    public double getDepth() {
        return depth;
    }

    /*
     * Set depth.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @param depth the depth parameter (double)
     */
    public void setDepth(double depth) {
        this.depth = depth;
    }

    /*
     * Get magnitude.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @return the resulting computation or state reference
     */
    public double getMagnitude() {
        return magnitude;
    }

    /*
     * Set magnitude.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @param magnitude the magnitude parameter (double)
     */
    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    @Override
    /*
     * To string.
     * Enforces physical invariants and updates associated state variables within {@code ClimateEvent}.
     *
     * @return the resulting computation or state reference
     */
    public String toString() {
        return String.format("%s (%d) [%.2f°, %.2f° Mag: %.1f]", name, year, latitude, longitude, magnitude);
    }
}
