/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

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
 *   Φ_i = (Food_i + Water_i) / (Pop_i + 1)
 *   ΔΦ_ij = Φ_j - Φ_i
 *   J_ij = M_0 * ΔΦ_ij * Pop_i
 *   d(Pop_i)/dt = - ∑_j J_ij
 *   d(Pop_j)/dt = + ∑_j J_ij
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class ThermodynamicMigrationEngine {
    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicMigrationEngine.class);

    /**
     * Executes one thermodynamic free energy migration tick across cells using spatial neighbor Onsager flux relations.
     */
    public static void processThermodynamicMigration(List<H3Cell> cells) {
        processThermodynamicMigration(cells, null);
    }

    /**
     * Executes thermodynamic free energy migration tick using spatial neighbor topology.
     * Evaluates actual Haversine spatial proximity (<= 150 km migration radius) rather than array indices.
     */
    public static void processThermodynamicMigration(List<H3Cell> cells, SimulationPerformanceConfig config) {
        if (cells == null || cells.isEmpty()) return;

        int migrationEvents = 0;
        int n = cells.size();

        // Spatial neighbor flux evaluation radius (150 km baseline land migration range)
        final double maxMigrationRadiusKm = 150.0;

        for (int i = 0; i < n; i++) {
            H3Cell origin = cells.get(i);
            int popOrigin = origin.getPopulation() != null ? origin.getPopulation() : 0;
            if (popOrigin < 10) continue;

            double foodOrigin = origin.getFoodResource() != null ? origin.getFoodResource() : 0.0;
            double waterOrigin = origin.getWaterResource() != null ? origin.getWaterResource() : 0.0;
            double phiOrigin = (foodOrigin + waterOrigin) / (popOrigin + 1.0);

            H3Cell bestDestination = null;
            double maxDeltaPhi = 0.05; // Minimum potential gradient threshold

            // Search for highest-potential spatial neighbor within physical migration range
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                H3Cell destination = cells.get(j);

                // Spatial bounding box filter (lat/lon within physical migration envelope ~150km)
                double latDiff = Math.abs(origin.getLatitude() - destination.getLatitude());
                if (latDiff > 1.5) continue;
                double lonDiff = Math.abs(origin.getLongitude() - destination.getLongitude());
                if (lonDiff > 2.0) continue;

                double distKm = calculateHaversineDistance(
                        origin.getLatitude(), origin.getLongitude(),
                        destination.getLatitude(), destination.getLongitude()
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
            logger.info("Migration Engine: Spatial Onsager demographic vector shifts evaluated across {} spatial neighbor pairs.", migrationEvents);
        }
    }

    private static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
