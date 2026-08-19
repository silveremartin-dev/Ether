/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.io.Serializable;

/**
 * Data model representing a cataclysmic, historical, or climatological planetary event.
 */
public class ClimateEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String name;
    private int year;
    private double latitude;
    private double longitude;
    private double depth;
    private double magnitude;

    public ClimateEvent() {
        this("volcano", "Event", 0, 0.0, 0.0, 0.0, 0.0);
    }

    public ClimateEvent(String type, String name, int year, double latitude, double longitude, double depth, double magnitude) {
        this.type = type;
        this.name = name;
        this.year = year;
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = depth;
        this.magnitude = magnitude;
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

    @Override
    public String toString() {
        return String.format("%s (%d) [%.2f°, %.2f° Mag: %.1f]", name, year, latitude, longitude, magnitude);
    }
}
