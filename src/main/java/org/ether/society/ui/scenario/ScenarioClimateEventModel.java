/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Observable JavaFX model representing a scheduled biophysical or historical planetary event
 * inside the scenario setup calendar table.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioClimateEventModel {
    private final StringProperty type;
    private final StringProperty name;
    private final IntegerProperty year;
    private final DoubleProperty latitude;
    private final DoubleProperty longitude;
    private final DoubleProperty depth;
    private final DoubleProperty magnitude;

    /**
     * Constructs a scheduled climate/historical event.
     *
     * @param type event typology (VOLCANO, PANDEMIC, DROUGHT, LEADER, etc.)
     * @param name descriptive title or historical event name
     * @param year chronological year CE/BCE
     * @param lat latitude coordinate in degrees [-90°, +90°]
     * @param lon longitude coordinate in degrees [-180°, +180°]
     * @param depth hypocenter depth in km (for earthquakes/volcanoes)
     * @param mag magnitude or normalized impact intensity [0.0, 1.0]
     */
    public ScenarioClimateEventModel(String type, String name, int year, double lat, double lon, double depth, double mag) {
        this.type = new SimpleStringProperty(type);
        this.name = new SimpleStringProperty(name);
        this.year = new SimpleIntegerProperty(year);
        this.latitude = new SimpleDoubleProperty(lat);
        this.longitude = new SimpleDoubleProperty(lon);
        this.depth = new SimpleDoubleProperty(depth);
        this.magnitude = new SimpleDoubleProperty(mag);
    }

    public StringProperty typeProperty() { return type; }
    public StringProperty nameProperty() { return name; }
    public IntegerProperty yearProperty() { return year; }
    public DoubleProperty latitudeProperty() { return latitude; }
    public DoubleProperty longitudeProperty() { return longitude; }
    public DoubleProperty depthProperty() { return depth; }
    public DoubleProperty magnitudeProperty() { return magnitude; }

    public String getType() { return type.get(); }
    public void setType(String v) { type.set(v); }
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public int getYear() { return year.get(); }
    public void setYear(int v) { year.set(v); }
    public double getLatitude() { return latitude.get(); }
    public void setLatitude(double v) { latitude.set(v); }
    public double getLongitude() { return longitude.get(); }
    public void setLongitude(double v) { longitude.set(v); }
    public double getDepth() { return depth.get(); }
    public void setDepth(double v) { depth.set(v); }
    public double getMagnitude() { return magnitude.get(); }
    public void setMagnitude(double v) { magnitude.set(v); }
}
