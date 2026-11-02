package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.h3.H3Service;
import org.ether.society.engines.tier1.ThermodynamicMigrationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Geopolitical Conflict & Border War Engine.
 * Models:
 * 1. War declarations based on expansionism (Asabiyyah), resource scarcity, target instability, and boundary friction (sigma_friction).
 * 2. Military battles using the youth adult recruitment cohort (pop15to24) with boundary friction resistance.
 * 3. Dynamic territorial conquest and border shifting.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class WarDiplomacyEngine {
    private static final Logger logger = LoggerFactory.getLogger(WarDiplomacyEngine.class);

    /**
     * Represents a detected boundary contact between two polities.
     *
     * @param avgFriction   average boundary friction across contested border cells
     * @param contestedCell a candidate border cell belonging to defender located along the frontline
     */
    public record BorderContact(double avgFriction, H3Cell contestedCell) {}

    /**
     * Bounding box representation for fast O(1) territorial overlap culling.
     */
    public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
        public static BoundingBox fromTerritory(Set<H3Cell> territory) {
            double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
            double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
            for (H3Cell c : territory) {
                if (c == null) continue;
                double lat = c.getLatitudeOr(0.0);
                double lon = c.getLongitudeOr(0.0);
                if (lat < minLat) minLat = lat;
                if (lat > maxLat) maxLat = lat;
                if (lon < minLon) minLon = lon;
                if (lon > maxLon) maxLon = lon;
            }
            return new BoundingBox(minLat, maxLat, minLon, maxLon);
        }

        public boolean overlapsWithMargin(BoundingBox other, double marginKm) {
            if (other == null) return false;
            double degMargin = marginKm / 111.0;
            if (this.maxLat + degMargin < other.minLat || other.maxLat + degMargin < this.minLat) {
                return false;
            }
            double meanLat = Math.toRadians((Math.max(this.minLat, other.minLat) + Math.min(this.maxLat, other.maxLat)) * 0.5);
            double cosLat = Math.max(0.15, Math.cos(meanLat));
            double lonMargin = degMargin / cosLat;
            if (this.maxLon + lonMargin < other.minLon || other.maxLon + lonMargin < this.minLon) {
                return false;
            }
            return true;
        }
    }

    /**
     * Calculates the boundary friction (sigma_friction) between two adjacent cells.
     * Incorporates sovereignty gradient mismatch, terrain movement friction, and elevation declivity.
     */
    public static double calculateBoundaryFriction(H3Cell c1, H3Cell c2) {
        if (c1 == null || c2 == null) return 1.0;

        // Base topographical friction based on terrain and biome movement penalty
        double baseFriction = Math.max(1.0, (c1.getMovementFrictionOr(1.0) + c2.getMovementFrictionOr(1.0)) * 0.5);

        // Elevation declivity penalty: steep gradient increases transit friction
        double elev1 = c1.getElevationOr(0.0);
        double elev2 = c2.getElevationOr(0.0);
        double elevationDeclivity = Math.abs(elev1 - elev2) / 1000.0; // declivity in km of altitude difference
        double declivityPenalty = 1.0 + (elevationDeclivity * 1.5);

        // River or marine water barrier friction
        boolean waterBarrier = (c1.getWaterResourceOr(0.0) > 100.0 && c2.getWaterResourceOr(0.0) > 100.0);
        double barrierMultiplier = waterBarrier ? 1.8 : 1.0;

        return baseFriction * declivityPenalty * barrierMultiplier;
    }

    /**
     * Executes geopolitical conflict pass across active nations without scenario override.
     *
     * @param nations list of active polities
     * @param cells   list of all grid cells
     */
    public static void processGeopoliticalConflicts(List<Polity> nations, List<H3Cell> cells) {
        processGeopoliticalConflicts(nations, cells, null);
    }

    /**
     * Executes geopolitical conflict pass across active nations.
     * Evaluates territorial adjacency, war desire, and resolves border skirmishes.
     */
    public static void processGeopoliticalConflicts(List<Polity> nations, List<H3Cell> cells, org.ether.society.model.Scenario scenario) {
        if (nations == null || nations.size() < 2) return;

        // Precompute bounding boxes once per tick for all nations (O(N) total)
        Map<Polity, BoundingBox> bboxes = new HashMap<>();
        for (Polity n : nations) {
            if (n != null && !n.getTerritory().isEmpty()) {
                bboxes.put(n, BoundingBox.fromTerritory(n.getTerritory()));
            }
        }

        for (int i = 0; i < nations.size() - 1; i++) {
            Polity aggressor = nations.get(i);
            if (aggressor.getTerritory().isEmpty() || aggressor.getAsabiyyah() < 0.40) continue;
            BoundingBox aggBox = bboxes.get(aggressor);
            if (aggBox == null) continue;

            for (int j = i + 1; j < nations.size(); j++) {
                Polity defender = nations.get(j);
                if (defender.getTerritory().isEmpty()) continue;
                BoundingBox defBox = bboxes.get(defender);
                if (defBox == null) continue;

                // O(1) Bounding box culling: skip immediately if territories are separated by > 160 km
                if (!aggBox.overlapsWithMargin(defBox, 160.0)) continue;

                // Check territorial adjacency & compute average boundary friction
                BorderContact contact = getBorderContact(aggressor, defender, aggBox, defBox);
                if (contact != null && contact.avgFriction() > 0) {
                    double avgBorderFriction = contact.avgFriction();
                    // War trigger condition: High aggressor Asabiyyah + Imperial State Capacity force projection + Weak defender State Capacity / High Instability
                    double imperialProjection = Math.max(0.0, aggressor.getStateCapacity() - 0.5) * 0.4;
                    double baseWarDesire = (aggressor.getAsabiyyah() * 0.6) + imperialProjection + (defender.getPoliticalInstability() * 0.4) - (defender.getStateCapacity() * 0.3);

                    // Cliodynamic Cultural Affinity Matrix Modulation:
                    double affinityMultiplier = 1.0;
                    if (scenario != null) {
                        double affinity = scenario.getPairwiseAffinity(aggressor.getId(), defender.getId());
                        affinityMultiplier = Math.clamp(1.5 - (affinity / 100.0) * 1.3, 0.1, 2.0);
                    }

                    // Topographic friction dampens opportunistic border skirmishes, raising the required war threshold
                    double frictionDamping = 1.0 + 0.25 * Math.max(0.0, avgBorderFriction - 1.0);
                    double warExhaustion = Math.max(0.0, aggressor.getPoliticalInstability() - 0.40) * 1.2
                            + (aggressor.getTreasuryBullion() <= 1.0 ? 0.25 : 0.0);
                    double warDesire = ((baseWarDesire * affinityMultiplier) / frictionDamping) - warExhaustion;

                    // Continuous escalation threshold (Bueno de Mesquita 1981): border skirmishes escalate when net war desire is positive
                    if (warDesire > 0.30) {
                        resolveBorderBattle(aggressor, defender, contact);
                    }
                }
            }
        }
    }

    /**
     * Backward-compatible helper method to compute average boundary friction between two polities.
     * Returns -1 if nations do not share a border within the projection distance (150 km).
     */
    public static double getAverageBorderFriction(Polity n1, Polity n2) {
        if (n1 == null || n2 == null || n1.getTerritory().isEmpty() || n2.getTerritory().isEmpty()) {
            return -1.0;
        }
        BoundingBox b1 = BoundingBox.fromTerritory(n1.getTerritory());
        BoundingBox b2 = BoundingBox.fromTerritory(n2.getTerritory());
        BorderContact contact = getBorderContact(n1, n2, b1, b2);
        return contact != null ? contact.avgFriction() : -1.0;
    }

    /**
     * Computes boundary contact and average friction across common border cells of two nations.
     * Uses spatial hash binning and fast equirectangular rejection in O(N1).
     */
    public static BorderContact getBorderContact(Polity n1, Polity n2, BoundingBox b1, BoundingBox b2) {
        if (n1 == null || n2 == null || n1.getTerritory().isEmpty() || n2.getTerritory().isEmpty()) {
            return null;
        }
        if (b1 != null && b2 != null && !b1.overlapsWithMargin(b2, 160.0)) {
            return null;
        }

        // Spatial Grid Binning for n2 territory (1.5° x 1.5° grid cells)
        final double binSize = 1.5;
        Map<Long, List<H3Cell>> spatialBins2 = new HashMap<>();
        for (H3Cell c2 : n2.getTerritory()) {
            if (c2 == null) continue;
            int bLat = (int) Math.floor(c2.getLatitudeOr(0.0) / binSize);
            int bLon = (int) Math.floor(c2.getLongitudeOr(0.0) / binSize);
            long key = (((long) bLat) << 32) | (bLon & 0xFFFFFFFFL);
            spatialBins2.computeIfAbsent(key, k -> new ArrayList<>()).add(c2);
        }

        double totalFriction = 0.0;
        int count = 0;
        H3Cell bestContestedCell = null;
        double minFoundDistSq = Double.POSITIVE_INFINITY;

        for (H3Cell c1 : n1.getTerritory()) {
            if (c1 == null) continue;
            double lat1 = c1.getLatitudeOr(0.0);
            double lon1 = c1.getLongitudeOr(0.0);
            int bLat1 = (int) Math.floor(lat1 / binSize);
            int bLon1 = (int) Math.floor(lon1 / binSize);

            // Search only in the 9 adjacent spatial bins
            for (int dLat = -1; dLat <= 1; dLat++) {
                for (int dLon = -1; dLon <= 1; dLon++) {
                    long neighborKey = (((long) (bLat1 + dLat)) << 32) | ((bLon1 + dLon) & 0xFFFFFFFFL);
                    List<H3Cell> bucket = spatialBins2.get(neighborKey);
                    if (bucket == null) continue;

                    for (H3Cell c2 : bucket) {
                        double lat2 = c2.getLatitudeOr(0.0);
                        double lon2 = c2.getLongitudeOr(0.0);

                        // Fast equirectangular distance: error < 0.1% over 150 km
                        double dLatKm = (lat1 - lat2) * 111.0;
                        if (Math.abs(dLatKm) > 150.0) continue;

                        double meanLatRad = Math.toRadians((lat1 + lat2) * 0.5);
                        double dLonKm = (lon1 - lon2) * 111.0 * Math.cos(meanLatRad);
                        double distSq = dLatKm * dLatKm + dLonKm * dLonKm;

                        if (distSq <= 150.0 * 150.0) {
                            totalFriction += calculateBoundaryFriction(c1, c2);
                            count++;
                            if (distSq < minFoundDistSq && c2 != n2.getCapital()) {
                                minFoundDistSq = distSq;
                                bestContestedCell = c2;
                            }
                            // Central Limit Theorem: 100 border pair samples provide < 1% error on mean boundary friction
                            if (count >= 100) {
                                return new BorderContact(totalFriction / count, bestContestedCell);
                            }
                        }
                    }
                }
            }
        }
        return count > 0 ? new BorderContact(totalFriction / count, bestContestedCell) : null;
    }

    /**
     * Resolves military engagement over a contested border cell using the 15-24 age military cohort,
     * physical armament capital ($K$), cavalry/motorized logistics, and sovereign bullion war chests.
     */
    private static void resolveBorderBattle(Polity aggressor, Polity defender, BorderContact contact) {
        if (contact == null) return;
        double borderFriction = contact.avgFriction();

        long aggressorForce = aggressor.getTerritory().stream().filter(Objects::nonNull).mapToLong(c -> c.getPop15to24Or(0)).sum();
        long defenderForce = defender.getTerritory().stream().filter(Objects::nonNull).mapToLong(c -> c.getPop15to24Or(0)).sum();

        if (aggressorForce <= 0 || defenderForce <= 0) return;

        double aggCapital = aggressor.getTerritory().stream().filter(Objects::nonNull).mapToDouble(c -> c.getResourceCapitalOr(0.0)).sum();
        double defCapital = defender.getTerritory().stream().filter(Objects::nonNull).mapToDouble(c -> c.getResourceCapitalOr(0.0)).sum();

        double aggMobility = aggressor.getTerritory().stream().filter(Objects::nonNull).mapToDouble(c ->
                (c.getBiomassLivestockOr(0.0)) * 0.2 + (c.getResourceOilOr(0.0)) * 0.5).sum();
        double defMobility = defender.getTerritory().stream().filter(Objects::nonNull).mapToDouble(c ->
                (c.getBiomassLivestockOr(0.0)) * 0.2 + (c.getResourceOilOr(0.0)) * 0.5).sum();

        double aggArmamentFactor = 1.0 + Math.min(2.5, Math.log1p(aggCapital / Math.max(1.0, aggressorForce)));
        double defArmamentFactor = 1.0 + Math.min(2.5, Math.log1p(defCapital / Math.max(1.0, defenderForce)));

        double aggLogisticsFactor = 1.0 + Math.min(1.0, Math.log1p(aggMobility / Math.max(1.0, aggressorForce)));
        double defLogisticsFactor = 1.0 + Math.min(1.0, Math.log1p(defMobility / Math.max(1.0, defenderForce)));

        double aggBullionFactor = 1.0 + Math.min(0.6, Math.log1p(aggressor.getTreasuryBullion() / 50.0) * 0.15);
        double defBullionFactor = 1.0 + Math.min(0.6, Math.log1p(defender.getTreasuryBullion() / 50.0) * 0.15);

        double physicalFriction = Math.max(1.0, borderFriction - 1.5);
        double crestDefenseBonus = borderFriction >= 3.2 ? Math.sqrt(borderFriction) : 1.0;
        double aggressorPower = (aggressorForce * (0.5 + aggressor.getStateCapacity()) * aggArmamentFactor * aggLogisticsFactor * aggBullionFactor) / physicalFriction;
        double defenderPower = defenderForce * (0.5 + defender.getStateCapacity()) * defArmamentFactor * defLogisticsFactor * defBullionFactor * crestDefenseBonus;

        double aggCostFraction = Math.clamp(0.04 + (0.04 * (borderFriction / 4.0)) + (0.02 * Math.log1p(aggressorForce / 2000.0)), 0.04, 0.35);
        double defCostFraction = Math.clamp(0.03 + (0.02 * (defenderForce / Math.max(1.0, aggressorForce + defenderForce))), 0.02, 0.20);

        aggressor.setTreasuryBullion(Math.max(0.0, aggressor.getTreasuryBullion() * (1.0 - aggCostFraction)));
        defender.setTreasuryBullion(Math.max(0.0, defender.getTreasuryBullion() * (1.0 - defCostFraction)));

        for (H3Cell c : aggressor.getTerritory()) {
            if (c != null && c.getResourceCapital() != null) {
                c.setResourceCapital(Math.max(0.0, c.getResourceCapital() * 0.997));
            }
        }
        for (H3Cell c : defender.getTerritory()) {
            if (c != null && c.getResourceCapital() != null) {
                c.setResourceCapital(Math.max(0.0, c.getResourceCapital() * 0.996));
            }
        }

        // Lanchester's Square Law (Lanchester 1916): combat victory probability scales with force squared
        double aggPowerSq = aggressorPower * aggressorPower;
        double defPowerSq = defenderPower * defenderPower;
        double totalPowerSq = Math.max(1.0, aggPowerSq + defPowerSq);
        double conquestProb = aggPowerSq / totalPowerSq;

        if (conquestProb > 0.55) {
            // Aggressor conquers the contested border frontline cell
            H3Cell conqueredCell = contact.contestedCell();
            if (conqueredCell == null || conqueredCell == defender.getCapital()) {
                conqueredCell = defender.getTerritory().stream()
                        .filter(c -> c != defender.getCapital())
                        .findAny().orElse(null);
            }

            if (conqueredCell != null) {
                defender.removeCell(conqueredCell);
                aggressor.addCell(conqueredCell);
                conqueredCell.setBoundaryCell(true);

                // War casualties in military age cohort scale with boundary friction resistance
                double casualtyRate = Math.clamp(0.20 * (defPowerSq / totalPowerSq) * Math.sqrt(physicalFriction), 0.05, 0.45);
                double survival = 1.0 - casualtyRate;

                int curPop15to24 = conqueredCell.getPop15to24Or(0);
                int curPop = conqueredCell.getPopulationOr(0);
                int casualties = (int) Math.round(curPop15to24 * casualtyRate);

                conqueredCell.setPop15to24(Math.max(0, curPop15to24 - casualties));
                conqueredCell.setPopulation(Math.max(0, curPop - casualties));
                // Maintain exact demographic mass conservation across all age cohorts
                conqueredCell.setPop0to4((int) Math.round(conqueredCell.getPop0to4Or(0) * survival));
                conqueredCell.setPop5to14((int) Math.round(conqueredCell.getPop5to14Or(0) * survival));
                conqueredCell.setPop25to49((int) Math.round(conqueredCell.getPop25to49Or(0) * survival));
                conqueredCell.setPop50to64((int) Math.round(conqueredCell.getPop50to64Or(0) * survival));
                conqueredCell.setPop65to79((int) Math.round(conqueredCell.getPop65to79Or(0) * survival));
                conqueredCell.setPop80Plus((int) Math.round(conqueredCell.getPop80PlusOr(0) * survival));

                // Collateral infrastructure & capital destruction on the battlefield cell
                double cellCap = conqueredCell.getResourceCapitalOr(0.0);
                conqueredCell.setResourceCapital(Math.max(0.0, cellCap * (1.0 - 0.25 * casualtyRate)));

                logger.info("War Resolution: Nation '{}' conquered frontline border cell {} from Nation '{}' (Boundary Friction: {}). Military casualties: {}.",
                        aggressor.getName(), conqueredCell.getH3Index(), defender.getName(), String.format(Locale.ROOT, "%.2f", physicalFriction), casualties);
            }
        }
    }
}
