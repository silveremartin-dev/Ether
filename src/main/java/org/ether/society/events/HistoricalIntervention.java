/*
 * MIT License
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Nation;

/**
 * Encapsulates an exogenous or procedural historical leader intervention.
 * Modifies local geographic physics (H3Cell) and sovereign cliodynamic properties (Nation)
 * across an active spatio-temporal radius.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricalIntervention {
    private String id;
    private String name;
    private String description;
    private int yearStart;
    private int durationYears = 25;
    private double latitude;
    private double longitude;
    private double radiusKm = 1000.0;
    private LeaderArchetype archetype = LeaderArchetype.INSTITUTIONAL_REFORMER;
    private double magnitude = 6.0; // Scale 1.0 to 10.0

    // Physical & Socio-Economic Modifiers
    private double movementFrictionMultiplier = 1.0;
    private double stateCapacityDelta = 0.0;
    private double asabiyyahDelta = 0.0;
    private double eliteOverproductionDelta = 0.0;
    private double politicalInstabilityDelta = 0.0;
    private double capitalBonusGJ = 0.0;
    private double carryingCapacityMultiplier = 1.0;
    private double conquestSpeedMultiplier = 1.0;
    private boolean triggerSuccessionCrisisAtEnd = false;

    // Runtime state tracking
    private boolean activated = false;
    private boolean completed = false;

    public HistoricalIntervention() {
    }

    public HistoricalIntervention(String id, String name, String description, int yearStart, int durationYears,
                                  double latitude, double longitude, double radiusKm,
                                  LeaderArchetype archetype, double magnitude) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.yearStart = yearStart;
        this.durationYears = Math.max(1, durationYears);
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = Math.max(50.0, radiusKm);
        this.archetype = archetype != null ? archetype : LeaderArchetype.INSTITUTIONAL_REFORMER;
        this.magnitude = Math.max(1.0, Math.min(10.0, magnitude));
        initDefaultModifiersByArchetype();
    }

    /**
     * Initializes typical modifier intensities scaled by magnitude.
     */
    public void initDefaultModifiersByArchetype() {
        double intensity = magnitude / 10.0; // 0.1 to 1.0
        switch (archetype) {
            case MILITARY_CONQUEROR -> {
                this.conquestSpeedMultiplier = 1.0 + (intensity * 4.0); // up to 5x
                this.movementFrictionMultiplier = Math.max(0.3, 1.0 - (intensity * 0.5));
                this.asabiyyahDelta = intensity * 0.25;
                this.triggerSuccessionCrisisAtEnd = true;
            }
            case INFRASTRUCTURE_BUILDER -> {
                this.movementFrictionMultiplier = Math.max(0.2, 1.0 - (intensity * 0.7)); // roads / routes
                this.capitalBonusGJ = intensity * 100_000.0;
                this.stateCapacityDelta = intensity * 0.15;
            }
            case INSTITUTIONAL_REFORMER -> {
                this.stateCapacityDelta = intensity * 0.35;
                this.politicalInstabilityDelta = -intensity * 0.30;
                this.eliteOverproductionDelta = -intensity * 0.20;
            }
            case HYDRAULIC_AGRARIAN_INNOVATOR -> {
                this.carryingCapacityMultiplier = 1.0 + (intensity * 0.80);
                this.capitalBonusGJ = intensity * 50_000.0;
            }
            case MORAL_RELIGIOUS_SAGE -> {
                this.asabiyyahDelta = intensity * 0.40;
                this.politicalInstabilityDelta = -intensity * 0.35;
            }
            case TOTALITARIAN_PURGER -> {
                this.eliteOverproductionDelta = -intensity * 0.50;
                this.stateCapacityDelta = -intensity * 0.20;
                this.politicalInstabilityDelta = intensity * 0.40;
            }
        }
    }

    public boolean isActive(int currentYear) {
        return currentYear >= yearStart && currentYear < (yearStart + durationYears);
    }

    public boolean isExpired(int currentYear) {
        return currentYear >= (yearStart + durationYears);
    }

    public boolean isInsideRadius(double cellLat, double cellLng) {
        double dLat = Math.toRadians(cellLat - latitude);
        double dLng = Math.toRadians(cellLng - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(cellLat)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distKm = 6371.0 * c;
        return distKm <= radiusKm;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getYearStart() { return yearStart; }
    public void setYearStart(int yearStart) { this.yearStart = yearStart; }

    public int getDurationYears() { return durationYears; }
    public void setDurationYears(int durationYears) { this.durationYears = durationYears; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(double radiusKm) { this.radiusKm = radiusKm; }

    public LeaderArchetype getArchetype() { return archetype; }
    public void setArchetype(LeaderArchetype archetype) { this.archetype = archetype; }

    public double getMagnitude() { return magnitude; }
    public void setMagnitude(double magnitude) { this.magnitude = magnitude; }

    public double getMovementFrictionMultiplier() { return movementFrictionMultiplier; }
    public void setMovementFrictionMultiplier(double movementFrictionMultiplier) { this.movementFrictionMultiplier = movementFrictionMultiplier; }

    public double getStateCapacityDelta() { return stateCapacityDelta; }
    public void setStateCapacityDelta(double stateCapacityDelta) { this.stateCapacityDelta = stateCapacityDelta; }

    public double getAsabiyyahDelta() { return asabiyyahDelta; }
    public void setAsabiyyahDelta(double asabiyyahDelta) { this.asabiyyahDelta = asabiyyahDelta; }

    public double getEliteOverproductionDelta() { return eliteOverproductionDelta; }
    public void setEliteOverproductionDelta(double eliteOverproductionDelta) { this.eliteOverproductionDelta = eliteOverproductionDelta; }

    public double getPoliticalInstabilityDelta() { return politicalInstabilityDelta; }
    public void setPoliticalInstabilityDelta(double politicalInstabilityDelta) { this.politicalInstabilityDelta = politicalInstabilityDelta; }

    public double getCapitalBonusGJ() { return capitalBonusGJ; }
    public void setCapitalBonusGJ(double capitalBonusGJ) { this.capitalBonusGJ = capitalBonusGJ; }

    public double getCarryingCapacityMultiplier() { return carryingCapacityMultiplier; }
    public void setCarryingCapacityMultiplier(double carryingCapacityMultiplier) { this.carryingCapacityMultiplier = carryingCapacityMultiplier; }

    public double getConquestSpeedMultiplier() { return conquestSpeedMultiplier; }
    public void setConquestSpeedMultiplier(double conquestSpeedMultiplier) { this.conquestSpeedMultiplier = conquestSpeedMultiplier; }

    public boolean isTriggerSuccessionCrisisAtEnd() { return triggerSuccessionCrisisAtEnd; }
    public void setTriggerSuccessionCrisisAtEnd(boolean triggerSuccessionCrisisAtEnd) { this.triggerSuccessionCrisisAtEnd = triggerSuccessionCrisisAtEnd; }

    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
