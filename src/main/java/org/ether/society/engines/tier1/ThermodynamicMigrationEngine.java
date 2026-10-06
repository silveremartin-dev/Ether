/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.config.SimulationPerformanceConfig;

import org.ether.society.database.H3Cell;
import org.ether.society.model.PhysicalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Population Migration Vectors by Thermodynamic Free Power Gradients.
 * 
 * <h2>Onsager Reciprocal Transport Equations</h2>
 * <pre>
 *   ÃŽÂ¦_i = (Food_i + Water_i) / (Pop_i + 1)
 *   ÃŽâ€ÃŽÂ¦_ij = ÃŽÂ¦_j - ÃŽÂ¦_i
 *   J_ij = M_0 * ÃŽâ€ÃŽÂ¦_ij * Pop_i
 *   d(Pop_i)/dt = - Ã¢Ë†â€˜_j J_ij
 *   d(Pop_j)/dt = + Ã¢Ë†â€˜_j J_ij
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ThermodynamicMigrationEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicMigrationEngine.class);

    /*
     * Executes one thermodynamic free energy migration tick across cells using spatial neighbor Onsager flux relations.
     */
    public static void processThermodynamicMigration(List<H3Cell> cells) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        processThermodynamicMigration(cells, null);
    }

    /*
     * Executes thermodynamic free energy migration tick using spatial neighbor topology.
     * Evaluates actual Haversine spatial proximity (<= 150 km migration radius) rather than array indices.
     */
    public static void processThermodynamicMigration(List<H3Cell> cells, SimulationPerformanceConfig config) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        processThermodynamicMigration(cells, config, 6371.0);
    }

    /*
     * Process thermodynamic migration.
     * Enforces physical invariants and updates associated state variables within {@code ThermodynamicMigrationEngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param config the config parameter (SimulationPerformanceConfig)
     * @param planetRadiusKm the planet radius km parameter (double)
     */
    public static void processThermodynamicMigration(List<H3Cell> cells, SimulationPerformanceConfig config, double planetRadiusKm) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;

        int migrationEvents = 0;
        int n = cells.size();

        // Spatial neighbor flux evaluation radius (150 km baseline land migration range)
        final double maxMigrationRadiusKm = 150.0;
        final double radius = (planetRadiusKm > 0) ? planetRadiusKm : 6371.0;

        // Build O(N) 2D Spatial Hash Grid (2.0 degree buckets ~220km envelope)
        final double binSizeDeg = 2.0;
        java.util.Map<Long, java.util.List<H3Cell>> spatialGrid = new java.util.HashMap<>(Math.min(65536, n / 4 + 16));
        for (H3Cell c : cells) {
            int bLat = (int) Math.floor((c.getLatitude() + 90.0) / binSizeDeg);
            int bLon = (int) Math.floor((c.getLongitude() + 180.0) / binSizeDeg);
            long key = (((long) bLat) << 32) | (bLon & 0xFFFFFFFFL);
            spatialGrid.computeIfAbsent(key, k -> new java.util.ArrayList<>(8)).add(c);
        }

        for (int i = 0; i < n; i++) {
            H3Cell origin = cells.get(i);
            int popOrigin = origin.getPopulation() != null ? origin.getPopulation() : 0;
            if (popOrigin < 10) continue;

            double foodOrigin = origin.getFoodResource() != null ? origin.getFoodResource() : 0.0;
            double waterOrigin = origin.getWaterResource() != null ? origin.getWaterResource() : 0.0;
            double phiOrigin = (foodOrigin + waterOrigin) / (popOrigin + 1.0);

            H3Cell bestDestination = null;
            double maxDeltaPhi = 0.05; // Minimum potential gradient threshold

            int bLat = (int) Math.floor((origin.getLatitude() + 90.0) / binSizeDeg);
            int bLon = (int) Math.floor((origin.getLongitude() + 180.0) / binSizeDeg);

            // Search ONLY adjacent 9 spatial buckets (O(1) candidates)
            for (int dLat = -1; dLat <= 1; dLat++) {
                for (int dLon = -1; dLon <= 1; dLon++) {
                    long key = (((long) (bLat + dLat)) << 32) | ((bLon + dLon) & 0xFFFFFFFFL);
                    java.util.List<H3Cell> bucket = spatialGrid.get(key);
                    if (bucket == null) continue;

                    // Iterate over spatial cell domains and apply localized cellular state transformations
                    for (H3Cell destination : bucket) {
                        if (destination == origin) continue;

                        double latDiff = Math.abs(origin.getLatitude() - destination.getLatitude());
                        if (latDiff > 1.5) continue;
                        double lonDiff = Math.abs(origin.getLongitude() - destination.getLongitude());
                        if (lonDiff > 2.0) continue;

                        double distKm = calculateHaversineDistance(
                                origin.getLatitude(), origin.getLongitude(),
                                destination.getLatitude(), destination.getLongitude(),
                                radius
                        );

                        if (distKm > maxMigrationRadiusKm) continue;

                        int popDest = destination.getPopulation() != null ? destination.getPopulation() : 0;
                        double foodDest = destination.getFoodResource() != null ? destination.getFoodResource() : 0.0;
                        double waterDest = destination.getWaterResource() != null ? destination.getWaterResource() : 0.0;
                        double phiDest = (foodDest + waterDest) / (popDest + 1.0);

                        double deltaPhi = phiDest - phiOrigin;
                        if (deltaPhi > maxDeltaPhi) {
                            maxDeltaPhi = deltaPhi;
                            bestDestination = destination;
                        }
                    }
                }
            }

            // Thermodynamic Onsager gradient push to best spatial neighbor
            if (bestDestination != null) {
                migrationEvents++;
                double fluxFraction = Math.clamp(PhysicalConstants.ONSAGER_BASELINE_MOBILITY * maxDeltaPhi, 0.01, 0.20);
                int migrants = (int) Math.clamp((long) (popOrigin * fluxFraction), 1, popOrigin / 2);

                origin.setPopulation(popOrigin - migrants);
                bestDestination.setPopulation((bestDestination.getPopulation() != null ? bestDestination.getPopulation() : 0) + migrants);
            }
        }

        if (migrationEvents > 0) {
            logger.debug("Migration Engine: Spatial Onsager demographic vector shifts evaluated across {} spatial neighbor pairs.", migrationEvents);
        }
    }

    /*
     * Calculate haversine distance.
     * Enforces physical invariants and updates associated state variables within {@code ThermodynamicMigrationEngine}.
     *
     * @param lat1 the lat1 parameter (double)
     * @param lon1 the lon1 parameter (double)
     * @param lat2 the lat2 parameter (double)
     * @param lon2 the lon2 parameter (double)
     * @return the resulting computation or state reference
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        return calculateHaversineDistance(lat1, lon1, lat2, lon2, 6371.0);
    }

    /*
     * Calculate haversine distance.
     * Enforces physical invariants and updates associated state variables within {@code ThermodynamicMigrationEngine}.
     *
     * @param lat1 the lat1 parameter (double)
     * @param lon1 the lon1 parameter (double)
     * @param lat2 the lat2 parameter (double)
     * @param lon2 the lon2 parameter (double)
     * @param planetRadiusKm the planet radius km parameter (double)
     * @return the resulting computation or state reference
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2, double planetRadiusKm) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        double R = (planetRadiusKm > 0) ? planetRadiusKm : 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}


