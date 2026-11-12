/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ether.society.i18n.I18n;

/**
 * Encapsulates an exogenous or procedural historical leader intervention.
 * Modifies local geographic physics and sovereign cliodynamic properties
 * across an active spatio-temporal radius with anisotropic cultural and watershed coupling.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see HistoricalInterventionPhysicsHelper
 */
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * HistoricalIntervention.
 * <p>
 * Component for the Ether planetary simulation platform.
 * Adheres to the zero-defect code excellence standards and 500-line limit.
 * </p>
 *
 * @author Ether Architectural Working Group
 * @see org.ether.society.core.engine.SimulationEngineModule
 */
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
    private double magnitude = 6.0;

    private double movementFrictionMultiplier = 1.0;
    private double stateCapacityDelta = 0.0;
    private double asabiyyahDelta = 0.0;
    private double eliteOverproductionDelta = 0.0;
    private double politicalInstabilityDelta = 0.0;
    private double capitalBonusGJ = 0.0;
    private double carryingCapacityMultiplier = 1.0;
    private double conquestSpeedMultiplier = 1.0;
    private boolean triggerSuccessionCrisisAtEnd = false;

    private boolean activated = false;
    private boolean completed = false;

    /** Default no-arg constructor for Jackson deserialization. */
    public HistoricalIntervention() {
    }

    /**
     * Constructs a parameterized historical leader intervention and initializes archetype modifiers.
     *
     * @param id            unique intervention identifier
     * @param name          display name
     * @param description   historical summary
     * @param yearStart     onset year (CE/BCE)
     * @param durationYears active duration in years (>= 1)
     * @param latitude      epicenter latitude in degrees
     * @param longitude     epicenter longitude in degrees
     * @param radiusKm      influence cutoff radius in kilometers (>= 50 km)
     * @param archetype     catalytic leader archetype
     * @param magnitude     intervention intensity on [1.0, 10.0]
     */
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

    /** Initializes default socio-physical modifier intensities scaled by magnitude. */
    public void initDefaultModifiersByArchetype() {
        HistoricalInterventionPhysicsHelper.applyDefaultArchetypeModifiers(this);
    }

    /**
     * Checks whether the intervention is active during {@code currentYear}.
     *
     * @param currentYear calendar year
     * @return {@code true} if active
     */
    public boolean isActive(int currentYear) {
        return currentYear >= yearStart && currentYear < (yearStart + durationYears);
    }

    /**
     * Checks whether the intervention has expired at or after {@code currentYear}.
     *
     * @param currentYear calendar year
     * @return {@code true} if expired
     */
    public boolean isExpired(int currentYear) {
        return currentYear >= (yearStart + durationYears);
    }

    /**
     * Computes great-circle Haversine distance in kilometers to a target cell.
     *
     * @param cellLat target cell latitude in degrees
     * @param cellLng target cell longitude in degrees
     * @return distance in kilometers
     */
    public double getDistanceKm(double cellLat, double cellLng) {
        return HistoricalInterventionPhysicsHelper.computeHaversineDistanceKm(latitude, longitude, cellLat, cellLng);
    }

    /**
     * Checks whether the target cell lies within {@code radiusKm}.
     *
     * @param cellLat target cell latitude in degrees
     * @param cellLng target cell longitude in degrees
     * @return {@code true} if inside radius
     */
    public boolean isInsideRadius(double cellLat, double cellLng) {
        return getDistanceKm(cellLat, cellLng) <= radiusKm;
    }

    /**
     * Computes isotropic Gaussian spatial attenuation weight at target coordinates.
     *
     * @param cellLat target cell latitude in degrees
     * @param cellLng target cell longitude in degrees
     * @return weight in [0.0, 1.0]
     */
    public double getSpatialAttenuationWeight(double cellLat, double cellLng) {
        return HistoricalInterventionPhysicsHelper.computeSpatialAttenuation(getDistanceKm(cellLat, cellLng), radiusKm);
    }

    /**
     * Computes anisotropic spatial attenuation coupled with Cultural Isogloss (Tensor 0)
     * and Hydrographic Drainage Basin topology.
     *
     * @param cellLat           target cell latitude in degrees
     * @param cellLng           target cell longitude in degrees
     * @param cellCultureId     target cell culture identifier
     * @param leaderCultureId   leader epicenter culture identifier
     * @param cellWatershedId   target cell watershed identifier
     * @param leaderWatershedId leader epicenter watershed identifier
     * @return coupled transmission weight in [0.0, 1.0]
     */
    public double getCulturalAndWatershedCoupledWeight(double cellLat, double cellLng,
                                                       Integer cellCultureId, Integer leaderCultureId,
                                                       Integer cellWatershedId, Integer leaderWatershedId) {
        double spatialWeight = getSpatialAttenuationWeight(cellLat, cellLng);
        return HistoricalInterventionPhysicsHelper.computeCoupledWeight(
                spatialWeight, archetype, cellCultureId, leaderCultureId, cellWatershedId, leaderWatershedId);
    }

    /** @return unique intervention identifier */
    public String getId() { return id; }
    /** @param id unique intervention identifier */
    public void setId(String id) { this.id = id; }

    /** @return localized display name */
    public String getName() {
        if (id != null && !id.isBlank()) {
            return I18n.getOrDefault("leader.event." + id.toLowerCase() + ".name",
                   I18n.getOrDefault("leader.event." + id + ".name", name));
        }
        return name;
    }
    /** @param name raw display name */
    public void setName(String name) { this.name = name; }

    /** @return localized intervention description */
    public String getDescription() {
        if (id != null && !id.isBlank()) {
            return I18n.getOrDefault("leader.event." + id.toLowerCase() + ".desc",
                   I18n.getOrDefault("leader.event." + id + ".desc", description));
        }
        return description;
    }
    /** @param description raw intervention description */
    public void setDescription(String description) { this.description = description; }

    /** @return onset calendar year */
    public int getYearStart() { return yearStart; }
    /** @param yearStart onset calendar year */
    public void setYearStart(int yearStart) { this.yearStart = yearStart; }

    /** @return active duration in years */
    public int getDurationYears() { return durationYears; }
    /** @param durationYears active duration in years */
    public void setDurationYears(int durationYears) { this.durationYears = durationYears; }

    /** @return epicenter latitude in degrees */
    public double getLatitude() { return latitude; }
    /** @param latitude epicenter latitude in degrees */
    public void setLatitude(double latitude) { this.latitude = latitude; }

    /** @return epicenter longitude in degrees */
    public double getLongitude() { return longitude; }
    /** @param longitude epicenter longitude in degrees */
    public void setLongitude(double longitude) { this.longitude = longitude; }

    /** @return spatial cutoff radius in kilometers */
    public double getRadiusKm() { return radiusKm; }
    /** @param radiusKm spatial cutoff radius in kilometers */
    public void setRadiusKm(double radiusKm) { this.radiusKm = radiusKm; }

    /** @return leader archetype */
    public LeaderArchetype getArchetype() { return archetype; }
    /** @param archetype leader archetype */
    public void setArchetype(LeaderArchetype archetype) { this.archetype = archetype; }

    /** @return intervention magnitude on [1.0, 10.0] */
    public double getMagnitude() { return magnitude; }
    /** @param magnitude intervention magnitude on [1.0, 10.0] */
    public void setMagnitude(double magnitude) { this.magnitude = magnitude; }

    /** @return transport friction multiplier */
    public double getMovementFrictionMultiplier() { return movementFrictionMultiplier; }
    /** @param movementFrictionMultiplier transport friction multiplier */
    public void setMovementFrictionMultiplier(double movementFrictionMultiplier) { this.movementFrictionMultiplier = movementFrictionMultiplier; }

    /** @return state administrative capacity delta */
    public double getStateCapacityDelta() { return stateCapacityDelta; }
    /** @param stateCapacityDelta state administrative capacity delta */
    public void setStateCapacityDelta(double stateCapacityDelta) { this.stateCapacityDelta = stateCapacityDelta; }

    /** @return social cohesion (asabiyyah) delta */
    public double getAsabiyyahDelta() { return asabiyyahDelta; }
    /** @param asabiyyahDelta social cohesion (asabiyyah) delta */
    public void setAsabiyyahDelta(double asabiyyahDelta) { this.asabiyyahDelta = asabiyyahDelta; }

    /** @return elite overproduction index delta */
    public double getEliteOverproductionDelta() { return eliteOverproductionDelta; }
    /** @param eliteOverproductionDelta elite overproduction index delta */
    public void setEliteOverproductionDelta(double eliteOverproductionDelta) { this.eliteOverproductionDelta = eliteOverproductionDelta; }

    /** @return political instability index delta */
    public double getPoliticalInstabilityDelta() { return politicalInstabilityDelta; }
    /** @param politicalInstabilityDelta political instability index delta */
    public void setPoliticalInstabilityDelta(double politicalInstabilityDelta) { this.politicalInstabilityDelta = politicalInstabilityDelta; }

    /** @return physical infrastructure capital bonus in GJ */
    public double getCapitalBonusGJ() { return capitalBonusGJ; }
    /** @param capitalBonusGJ physical infrastructure capital bonus in GJ */
    public void setCapitalBonusGJ(double capitalBonusGJ) { this.capitalBonusGJ = capitalBonusGJ; }

    /** @return local carrying capacity multiplier */
    public double getCarryingCapacityMultiplier() { return carryingCapacityMultiplier; }
    /** @param carryingCapacityMultiplier local carrying capacity multiplier */
    public void setCarryingCapacityMultiplier(double carryingCapacityMultiplier) { this.carryingCapacityMultiplier = carryingCapacityMultiplier; }

    /** @return military conquest speed multiplier */
    public double getConquestSpeedMultiplier() { return conquestSpeedMultiplier; }
    /** @param conquestSpeedMultiplier military conquest speed multiplier */
    public void setConquestSpeedMultiplier(double conquestSpeedMultiplier) { this.conquestSpeedMultiplier = conquestSpeedMultiplier; }

    /** @return whether expiration triggers a succession crisis */
    public boolean isTriggerSuccessionCrisisAtEnd() { return triggerSuccessionCrisisAtEnd; }
    /** @param triggerSuccessionCrisisAtEnd whether expiration triggers a succession crisis */
    public void setTriggerSuccessionCrisisAtEnd(boolean triggerSuccessionCrisisAtEnd) { this.triggerSuccessionCrisisAtEnd = triggerSuccessionCrisisAtEnd; }

    /** @return runtime activation flag */
    public boolean isActivated() { return activated; }
    /** @param activated runtime activation flag */
    public void setActivated(boolean activated) { this.activated = activated; }

    /** @return runtime completion flag */
    public boolean isCompleted() { return completed; }
    /** @param completed runtime completion flag */
    public void setCompleted(boolean completed) { this.completed = completed; }
}
