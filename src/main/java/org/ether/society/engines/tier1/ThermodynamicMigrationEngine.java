*

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

 *   Φ_i = (Food_i + Water_i) / (Pop_i + 1)

 *   Îâ€ÎÂ¦_ij = ÎÂ¦_j - ÎÂ¦_i

 *   J_ij = M_0 * Îâ€ÎÂ¦_ij * Pop_i

 *   d(Pop_i)/dt = - ∑_j J_ij

 *   d(Pop_j)/dt = + ∑_j J_ij

 * </pre>

 *

 * @author Silvere Martin-Michiellot

 * @version 1.0.0-beta.2

 */

public class ThermodynamicMigrationEngine {

    private static final Logger logger = LoggerFactory.getLogger(ThermodynamicMigrationEngine.class);

    // Cached spatial grid avoiding 288k cell re-hashing and allocations every tick
    private static volatile java.util.Map<Long, java.util.List<H3Cell>> cachedSpatialGrid = null;
    private static volatile java.util.Map<H3Cell, Integer> cachedCellIndexMap = null;
    private static volatile int cachedGridCellCount = 0;
    private static volatile Object cachedCellsIdentity = null;

    public static void clearSpatialCache() {
        cachedSpatialGrid = null;
        cachedCellIndexMap = null;
        cachedGridCellCount = 0;
        cachedCellsIdentity = null;
    }



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


     *

     * @param cells the cells parameter (List&lt;H3Cell&gt;)

     * @param config the config parameter (SimulationPerformanceConfig)

     * @param planetRadiusKm the planet radius km parameter (double)

     */

    public static void processThermodynamicMigration(List<H3Cell> cells, SimulationPerformanceConfig config, double planetRadiusKm) {
        processThermodynamicMigration(cells, config, planetRadiusKm, null, null);
    }

    public static void processThermodynamicMigration(List<H3Cell> cells, SimulationPerformanceConfig config,
                                                     org.ether.society.core.dod.WorldBuffer worldBuffer,
                                                     org.ether.society.core.dod.AgentBuffer agentBuffer) {
        processThermodynamicMigration(cells, config, 6371.0, worldBuffer, agentBuffer);
    }

    /**
     * Process thermodynamic migration across spatial cells and synchronizes cohorts into DOD buffers.
     */
    public static void processThermodynamicMigration(List<H3Cell> cells, SimulationPerformanceConfig config, double planetRadiusKm,
                                                     org.ether.society.core.dod.WorldBuffer worldBuffer,
                                                     org.ether.society.core.dod.AgentBuffer agentBuffer) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;

        int migrationEvents = 0;

        int n = cells.size();



        // Spatial neighbor flux evaluation radius (150 km baseline land migration range)

        final double maxMigrationRadiusKm = 150.0;

        final double radius = (planetRadiusKm > 0) ? planetRadiusKm : 6371.0;



        final double binSizeDeg = 2.0;
        java.util.Map<Long, java.util.List<H3Cell>> spatialGrid = cachedSpatialGrid;
        java.util.Map<H3Cell, Integer> cellIndexMap = cachedCellIndexMap;
        if (spatialGrid == null || cachedGridCellCount != n || cachedCellsIdentity != cells || cellIndexMap == null) {
            spatialGrid = new java.util.HashMap<>(Math.min(65536, n / 4 + 16));
            cellIndexMap = new java.util.IdentityHashMap<>(n);
            for (int idx = 0; idx < n; idx++) {
                H3Cell c = cells.get(idx);
                if (c == null) continue;
                cellIndexMap.put(c, idx);
                double cLat = c.getLatitude() != null ? c.getLatitude() : 0.0;
                double cLon = c.getLongitude() != null ? c.getLongitude() : 0.0;
                int bLat = (int) Math.floor((cLat + 90.0) / binSizeDeg);
                int bLon = (int) Math.floor((cLon + 180.0) / binSizeDeg);
                long key = (((long) bLat) << 32) | (bLon & 0xFFFFFFFFL);
                spatialGrid.computeIfAbsent(key, k -> new java.util.ArrayList<>(8)).add(c);
            }
            cachedSpatialGrid = spatialGrid;
            cachedCellIndexMap = cellIndexMap;
            cachedGridCellCount = n;
            cachedCellsIdentity = cells;
        }



        float[] popBuffer = worldBuffer != null ? worldBuffer.getBiomassHuman() : null;
        double degToKmLat = (Math.PI / 180.0) * radius;
        double maxRadiusSq = maxMigrationRadiusKm * maxMigrationRadiusKm;

        for (int i = 0; i < n; i++) {
            if (popBuffer != null && popBuffer[i] < 10.0f) continue;
            H3Cell origin = cells.get(i);
            if (origin == null) continue;
            int popOrigin = origin.getPopulation() != null ? origin.getPopulation() : 0;
            if (popOrigin < 10) continue;

            double foodOrigin = origin.getFoodResource() != null ? origin.getFoodResource() : 0.0;
            double waterOrigin = origin.getWaterResource() != null ? origin.getWaterResource() : 0.0;
            double phiOrigin = (foodOrigin + waterOrigin) / (popOrigin + 1.0);

            H3Cell bestDestination = null;
            double maxDeltaPhi = 0.05; // Minimum potential gradient threshold

            double latOrigin = origin.getLatitude() != null ? origin.getLatitude() : 0.0;
            double lonOrigin = origin.getLongitude() != null ? origin.getLongitude() : 0.0;
            double degToKmLon = degToKmLat * Math.cos(Math.toRadians(latOrigin));

            int bLat = (int) Math.floor((latOrigin + 90.0) / binSizeDeg);
            int bLon = (int) Math.floor((lonOrigin + 180.0) / binSizeDeg);

            // Search ONLY adjacent 9 spatial buckets (O(1) candidates)
            for (int dLat = -1; dLat <= 1; dLat++) {
                for (int dLon = -1; dLon <= 1; dLon++) {
                    long key = (((long) (bLat + dLat)) << 32) | ((bLon + dLon) & 0xFFFFFFFFL);
                    java.util.List<H3Cell> bucket = spatialGrid.get(key);
                    if (bucket == null) continue;

                    // Iterate over spatial cell domains and apply localized cellular state transformations
                    for (H3Cell destination : bucket) {
                        if (destination == origin) continue;

                        double destLat = destination.getLatitude() != null ? destination.getLatitude() : 0.0;
                        double destLon = destination.getLongitude() != null ? destination.getLongitude() : 0.0;
                        double latDiff = Math.abs(latOrigin - destLat);
                        if (latDiff > 1.5) continue;
                        double lonDiff = Math.abs(lonOrigin - destLon);
                        if (lonDiff > 2.0) continue;

                        double dLatKm = (destLat - latOrigin) * degToKmLat;
                        double dLonKm = (destLon - lonOrigin) * degToKmLon;
                        double distSq = dLatKm * dLatKm + dLonKm * dLonKm;
                        if (distSq > maxRadiusSq) continue;
                        double distKm = Math.sqrt(distSq);



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

                int migrants = Math.clamp((int) (popOrigin * fluxFraction), 1, popOrigin / 2);



                origin.setPopulation(popOrigin - migrants);
                origin.setBiomassHuman((double) (popOrigin - migrants));
                int destPop = (bestDestination.getPopulation() != null ? bestDestination.getPopulation() : 0) + migrants;
                bestDestination.setPopulation(destPop);
                bestDestination.setBiomassHuman((double) destPop);

                // Transfer cohort mass in AgentBuffer and WorldBuffer to prevent split-brain rollback
                if (agentBuffer != null && worldBuffer != null) {
                    int originIdx = i; // cell i is origin
                    int destIdx = cellIndexMap != null ? cellIndexMap.getOrDefault(bestDestination, -1) : cells.indexOf(bestDestination);
                    if (originIdx >= 0 && destIdx >= 0) {
                        transferAgentBufferMass(agentBuffer, worldBuffer, originIdx, destIdx, migrants);
                    }
                }
            }
        }

        if (migrationEvents > 0) {
            logger.debug("Migration Engine: Spatial Onsager demographic vector shifts evaluated across {} spatial neighbor pairs.", migrationEvents);
        }
    }

    private static void transferAgentBufferMass(org.ether.society.core.dod.AgentBuffer agents,
                                                org.ether.society.core.dod.WorldBuffer world,
                                                int originIdx, int destIdx, float migrants) {
        float[] mass = agents.getMass();
        int[] hexIds = agents.getHexIds();
        float remainingToTransfer = migrants;

        for (int i = 0; i < agents.getCapacity() && remainingToTransfer > 0.001f; i++) {
            if (hexIds[i] == originIdx) {
                float deduct = Math.min(mass[i] * 0.5f, remainingToTransfer);
                if (deduct > 0.001f) {
                    mass[i] -= deduct;
                    remainingToTransfer -= deduct;
                }
            }
        }
        float actuallyTransferred = migrants - remainingToTransfer;
        if (actuallyTransferred <= 0.001f) return;

        boolean added = false;
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == destIdx) {
                mass[i] += actuallyTransferred;
                added = true;
                break;
            }
        }
        if (!added) {
            for (int i = 0; i < agents.getCapacity(); i++) {
                if (hexIds[i] == -1) {
                    hexIds[i] = destIdx;
                    mass[i] = actuallyTransferred;
                    agents.getEnergy()[i] = 80.0f;
                    agents.getAge()[i] = 25.0f;
                    float[] techs = world.getTechnologyLevel();
                    agents.getTechLevel()[i] = (techs != null && techs.length > destIdx) ? techs[destIdx] : 1.0f;
                    added = true;
                    break;
                }
            }
        }

        float[] biomassHuman = world.getBiomassHuman();
        if (biomassHuman != null && originIdx < biomassHuman.length && destIdx < biomassHuman.length) {
            biomassHuman[originIdx] = Math.max(0.0f, biomassHuman[originIdx] - actuallyTransferred);
            biomassHuman[destIdx] += actuallyTransferred;
        }
    }



    /*

     * Calculate haversine distance.


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





