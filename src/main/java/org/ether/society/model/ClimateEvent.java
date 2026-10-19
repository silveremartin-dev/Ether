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

    /** Internal state: type (String). */
    private String type;
    /** Internal state: name (String). */
    private String name;
    /** Internal state: year (int). */
    private int year;
    /** Internal state: latitude (double). */
    private double latitude;
    /** Internal state: longitude (double). */
    private double longitude;
    /** Internal state: depth (double). */
    private double depth;
    /** Internal state: magnitude (double). */
    private double magnitude;

    /**
     * Climate event.
     *
     */
    public ClimateEvent() {
        this("volcano", "Event", 0, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Climate event.
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

    /**
     * Get type.
     *
     * @return the resulting computation or state reference
     */
    public String getType() {
        return type;
    }

    /**
     * Set type.
     *
     * @param type the type parameter (String)
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return name;
    }

    /**
     * Set name.
     *
     * @param name the name parameter (String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get year.
     *
     * @return the resulting computation or state reference
     */
    public int getYear() {
        return year;
    }

    /**
     * Set year.
     *
     * @param year the year parameter (int)
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * Get latitude.
     *
     * @return the resulting computation or state reference
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Set latitude.
     *
     * @param latitude the latitude parameter (double)
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Get longitude.
     *
     * @return the resulting computation or state reference
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Set longitude.
     *
     * @param longitude the longitude parameter (double)
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Get depth.
     *
     * @return the resulting computation or state reference
     */
    public double getDepth() {
        return depth;
    }

    /**
     * Set depth.
     *
     * @param depth the depth parameter (double)
     */
    public void setDepth(double depth) {
        this.depth = depth;
    }

    /**
     * Get magnitude.
     *
     * @return the resulting computation or state reference
     */
    public double getMagnitude() {
        return magnitude;
    }

    /**
     * Set magnitude.
     *
     * @param magnitude the magnitude parameter (double)
     */
    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
    }

    /**
     * To string.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String toString() {
        return String.format("%s (%d) [%.2f°, %.2f° Mag: %.1f]", name, year, latitude, longitude, magnitude);
    }
}
