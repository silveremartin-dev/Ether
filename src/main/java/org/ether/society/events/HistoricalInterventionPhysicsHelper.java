/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.events;

/**
 * Stateless spatial geodesy, Gaussian attenuation, and cultural-hydrographic coupling helper
 * for {@link HistoricalIntervention} events.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see HistoricalIntervention
 */
public final class HistoricalInterventionPhysicsHelper {

    /** Mean volumetric Earth radius in kilometers (IUGG). */
    private static final double EARTH_MEAN_RADIUS_KM = 6371.0;

    private HistoricalInterventionPhysicsHelper() {
        // Stateless helper class
    }

    /**
     * Initializes default socio-physical modifier intensities on the given intervention scaled by magnitude.
     *
     * @param intervention target historical intervention instance
     */
    public static void applyDefaultArchetypeModifiers(HistoricalIntervention intervention) {
        double intensity = intervention.getMagnitude() / 10.0;
        LeaderArchetype archetype = intervention.getArchetype() != null
                ? intervention.getArchetype()
                : LeaderArchetype.INSTITUTIONAL_REFORMER;

        switch (archetype) {
            case MILITARY_CONQUEROR -> {
                intervention.setConquestSpeedMultiplier(1.0 + (intensity * 4.0));
                intervention.setMovementFrictionMultiplier(Math.max(0.3, 1.0 - (intensity * 0.5)));
                intervention.setAsabiyyahDelta(intensity * 0.25);
                intervention.setTriggerSuccessionCrisisAtEnd(true);
            }
            case INFRASTRUCTURE_BUILDER -> {
                intervention.setMovementFrictionMultiplier(Math.max(0.2, 1.0 - (intensity * 0.7)));
                intervention.setCapitalBonusGJ(intensity * 100_000.0);
                intervention.setStateCapacityDelta(intensity * 0.15);
            }
            case INSTITUTIONAL_REFORMER -> {
                intervention.setStateCapacityDelta(intensity * 0.35);
                intervention.setPoliticalInstabilityDelta(-intensity * 0.30);
                intervention.setEliteOverproductionDelta(-intensity * 0.20);
            }
            case HYDRAULIC_AGRARIAN_INNOVATOR -> {
                intervention.setCarryingCapacityMultiplier(1.0 + (intensity * 0.80));
                intervention.setCapitalBonusGJ(intensity * 50_000.0);
            }
            case MORAL_RELIGIOUS_SAGE -> {
                intervention.setAsabiyyahDelta(intensity * 0.40);
                intervention.setPoliticalInstabilityDelta(-intensity * 0.35);
            }
            case TOTALITARIAN_PURGER -> {
                intervention.setEliteOverproductionDelta(-intensity * 0.50);
                intervention.setStateCapacityDelta(-intensity * 0.20);
                intervention.setPoliticalInstabilityDelta(intensity * 0.40);
            }
        }
    }

    /**
     * Computes great-circle Haversine distance in kilometers between two geographic coordinates.
     *
     * @param lat1 epicenter latitude in degrees
     * @param lng1 epicenter longitude in degrees
     * @param lat2 target cell latitude in degrees
     * @param lng2 target cell longitude in degrees
     * @return geodesic distance in kilometers
     */
    public static double computeHaversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2.0) * Math.sin(dLng / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_MEAN_RADIUS_KM * c;
    }

    /**
     * Computes radial Gaussian spatial attenuation weight within {@code radiusKm}:
     * $$w(d) = \exp\!\left(-\frac{d^2}{2\,\sigma^2}\right), \quad \sigma = \max(10,\; R / 2.5)$$
     *
     * @param distKm   great-circle distance in kilometers
     * @param radiusKm maximum cutoff radius in kilometers
     * @return spatial weight in {@code [0.0, 1.0]}
     */
    public static double computeSpatialAttenuation(double distKm, double radiusKm) {
        if (distKm > radiusKm) return 0.0;
        double sigma = Math.max(10.0, radiusKm / 2.5);
        return Math.exp(-0.5 * (distKm * distKm) / (sigma * sigma));
    }

    /**
     * Computes anisotropic spatial attenuation coupled with Cultural Isogloss (Tensor 0)
     * and Hydrographic Drainage Basin topology.
     *
     * @param spatialWeight     isotropic radial Gaussian weight
     * @param archetype         leader archetype governing cross-border and trans-basin friction
     * @param cellCultureId     target cell culture identifier ({@code null} or {@code < 0} if unspecified)
     * @param leaderCultureId   epicenter culture identifier ({@code null} or {@code < 0} if unspecified)
     * @param cellWatershedId   target cell watershed identifier ({@code null} or {@code < 0} if unspecified)
     * @param leaderWatershedId epicenter watershed identifier ({@code null} or {@code < 0} if unspecified)
     * @return coupled transmission weight in {@code [0.0, 1.0]}
     */
    public static double computeCoupledWeight(double spatialWeight,
                                              LeaderArchetype archetype,
                                              Integer cellCultureId, Integer leaderCultureId,
                                              Integer cellWatershedId, Integer leaderWatershedId) {
        if (spatialWeight <= 0.0) return 0.0;
        LeaderArchetype effectiveArchetype = archetype != null ? archetype : LeaderArchetype.INSTITUTIONAL_REFORMER;

        double culturalFactor = 1.0;
        if (cellCultureId != null && leaderCultureId != null && cellCultureId >= 0 && leaderCultureId >= 0
                && !cellCultureId.equals(leaderCultureId)) {
            culturalFactor = switch (effectiveArchetype) {
                case MILITARY_CONQUEROR -> 0.60;
                case INFRASTRUCTURE_BUILDER -> 0.45;
                case HYDRAULIC_AGRARIAN_INNOVATOR -> 0.50;
                case INSTITUTIONAL_REFORMER -> 0.35;
                case MORAL_RELIGIOUS_SAGE -> 0.30;
                case TOTALITARIAN_PURGER -> 0.20;
            };
        }

        double watershedFactor = 1.0;
        if (cellWatershedId != null && leaderWatershedId != null && cellWatershedId >= 0 && leaderWatershedId >= 0
                && !cellWatershedId.equals(leaderWatershedId)) {
            watershedFactor = switch (effectiveArchetype) {
                case HYDRAULIC_AGRARIAN_INNOVATOR -> 0.35;
                case INFRASTRUCTURE_BUILDER -> 0.60;
                default -> 0.80;
            };
        }

        return spatialWeight * culturalFactor * watershedFactor;
    }
}
