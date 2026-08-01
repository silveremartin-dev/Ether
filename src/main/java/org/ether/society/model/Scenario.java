/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import org.ether.society.procedural.PlanetPreset;
import java.io.Serializable;

/**
 * Configuration for a simulation scenario.
 */
public class Scenario implements Serializable {
    private String name;
    private Long id;

    // Planet Configuration
    private PlanetPreset planetPreset;
    private boolean useRealEarthData;

    // Planet Physics
    private double planetRadiusKm; // Size
    private double rotationPeriodHours;
    private double revolutionPeriodDays;
    private double axialTiltDegrees; // Inclination

    // Human Start Conditions
    private long initialHumanCount;
    private double initialTechLevel;
    private String populationDensityType; // "ONE_CONTINENT", "DENSE", "SPARSE", "RIVER_VALLEYS"

    // Simulation Parameters
    private double cellSizeKm2;
    private double climateHarshness; // 0.0 to 1.0 (storms, droughts)
    private long startDateYear; // e.g. -100000
    private long seed = 12345L;
    private boolean randomEventsEnabled = true;
    private String customDensityBase64;

    // Spatial Clipping & Boundary Conditions
    private boolean clippingEnabled = false;
    private double minLat = -90.0;
    private double maxLat = 90.0;
    private double minLng = -180.0;
    private double maxLng = 180.0;
    private String boundaryMode = "DYNAMIC_RESERVOIR"; // "DYNAMIC_RESERVOIR", "CLOSED_BARRIER", "PERIODIC_WRAP"

    public Scenario() {
        // Defaults
        this.name = "New Scenario";
        this.planetRadiusKm = 6371.0;
        this.rotationPeriodHours = 24.0;
        this.revolutionPeriodDays = 365.25;
        this.axialTiltDegrees = 23.5;

        this.initialHumanCount = 1000;
        this.initialTechLevel = 0.0;
        this.populationDensityType = "SPARSE";

        this.cellSizeKm2 = 100.0;
        this.climateHarshness = 0.5;
        this.startDateYear = -100000;
        this.useRealEarthData = false;
        this.seed = 12345L;
        this.randomEventsEnabled = true;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlanetPreset getPlanetPreset() {
        return planetPreset;
    }

    public void setPlanetPreset(PlanetPreset planetPreset) {
        this.planetPreset = planetPreset;
    }

    public boolean isUseRealEarthData() {
        return useRealEarthData;
    }

    public void setUseRealEarthData(boolean useRealEarthData) {
        this.useRealEarthData = useRealEarthData;
    }

    public double getPlanetRadiusKm() {
        return planetRadiusKm;
    }

    public void setPlanetRadiusKm(double planetRadiusKm) {
        this.planetRadiusKm = planetRadiusKm;
    }

    public double getRotationPeriodHours() {
        return rotationPeriodHours;
    }

    public void setRotationPeriodHours(double rotationPeriodHours) {
        this.rotationPeriodHours = rotationPeriodHours;
    }

    public double getRevolutionPeriodDays() {
        return revolutionPeriodDays;
    }

    public void setRevolutionPeriodDays(double revolutionPeriodDays) {
        this.revolutionPeriodDays = revolutionPeriodDays;
    }

    public double getAxialTiltDegrees() {
        return axialTiltDegrees;
    }

    public void setAxialTiltDegrees(double axialTiltDegrees) {
        this.axialTiltDegrees = axialTiltDegrees;
    }

    public long getInitialHumanCount() {
        return initialHumanCount;
    }

    public void setInitialHumanCount(long initialHumanCount) {
        this.initialHumanCount = initialHumanCount;
    }

    public double getInitialTechLevel() {
        return initialTechLevel;
    }

    public void setInitialTechLevel(double initialTechLevel) {
        this.initialTechLevel = initialTechLevel;
    }

    public String getPopulationDensityType() {
        return populationDensityType;
    }

    public void setPopulationDensityType(String populationDensityType) {
        this.populationDensityType = populationDensityType;
    }

    public double getCellSizeKm2() {
        return cellSizeKm2;
    }

    public void setCellSizeKm2(double cellSizeKm2) {
        this.cellSizeKm2 = cellSizeKm2;
    }

    public double getClimateHarshness() {
        return climateHarshness;
    }

    public void setClimateHarshness(double climateHarshness) {
        this.climateHarshness = climateHarshness;
    }

    public long getStartDateYear() {
        return startDateYear;
    }

    public void setStartDateYear(long startDateYear) {
        this.startDateYear = startDateYear;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public boolean isRandomEventsEnabled() {
        return randomEventsEnabled;
    }

    public void setRandomEventsEnabled(boolean randomEventsEnabled) {
        this.randomEventsEnabled = randomEventsEnabled;
    }

    public String getCustomDensityBase64() {
        return customDensityBase64;
    }

    public void setCustomDensityBase64(String customDensityBase64) {
        this.customDensityBase64 = customDensityBase64;
    }

    public boolean isClippingEnabled() {
        return clippingEnabled;
    }

    public void setClippingEnabled(boolean clippingEnabled) {
        this.clippingEnabled = clippingEnabled;
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public double getMinLng() {
        return minLng;
    }

    public void setMinLng(double minLng) {
        this.minLng = minLng;
    }

    public double getMaxLng() {
        return maxLng;
    }

    public void setMaxLng(double maxLng) {
        this.maxLng = maxLng;
    }

    public String getBoundaryMode() {
        return boundaryMode;
    }

    public void setBoundaryMode(String boundaryMode) {
        this.boundaryMode = boundaryMode;
    }
}
