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

                    // Cliodynamic Cultural Affinity Matrix Modulation (by Name and by ID):
                    double affinityMultiplier = 1.0;
                    if (scenario != null) {
                        double affinity = scenario.getPairwiseAffinity(aggressor.getName(), defender.getName());
                        if (affinity == 50.0) {
                            affinity = scenario.getPairwiseAffinity(aggressor.getId(), defender.getId());
                        }
                        affinityMultiplier = Math.clamp(1.5 - (affinity / 100.0) * 1.3, 0.1, 2.0);
                    }

                    // Topographic friction dampens opportunistic border skirmishes; high state capacity & bullion logistics overcome friction (Pax Romana engineering)
                    double logisticalEngineeringFactor = 1.0 + (aggressor.getStateCapacity() * 0.75) + Math.min(1.0, aggressor.getTreasuryBullion() / 2000.0);
                    double frictionDamping = 1.0 + (0.25 * Math.max(0.0, avgBorderFriction - 1.0)) / logisticalEngineeringFactor;
                    double warExhaustion = Math.max(0.0, aggressor.getPoliticalInstability() - 0.40) * 1.2
                            + (aggressor.getTreasuryBullion() <= 1.0 ? 0.25 : 0.0);
                    double warDesire = ((baseWarDesire * affinityMultiplier) / frictionDamping) - warExhaustion;

                    // Continuous escalation threshold (Bueno de Mesquita 1981): border skirmishes escalate when net war desire is positive
                    if (warDesire > 0.30) {
                        resolveBorderBattle(aggressor, defender, contact);
                        if (scenario != null) {
                            // Border clash degrades bilateral diplomatic affinity dynamically
                            scenario.adjustPairwiseAffinity(aggressor.getName(), defender.getName(), -10.0);
                            scenario.adjustPairwiseAffinity(aggressor.getId(), defender.getId(), -10.0);
                        }
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
        // Topographic friction dampening; imperial state capacity and bullion finance military road engineering (Pax Romana)
        double engineeringMitigation = 1.0 + (aggressor.getStateCapacity() * 1.5) + Math.min(2.0, aggressor.getTreasuryBullion() / 2000.0);
        double effectiveFriction = 1.0 + Math.max(0.0, physicalFriction - 1.0) / engineeringMitigation;
        double crestDefenseBonus = borderFriction >= 3.2 ? Math.sqrt(borderFriction) : 1.0;
        double aggressorPower = (aggressorForce * (0.5 + aggressor.getStateCapacity()) * aggArmamentFactor * aggLogisticsFactor * aggBullionFactor) / effectiveFriction;
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

                // Cliodynamic Warfare Demographics:
                // 1. Direct combat casualties fall predominantly on the young recruitment cohorts (males 15-24 and 25-49)
                // 2. Era-dependent & conflict-type civilian collateral attrition (Eckhardt 1989; Valentino 2004)
                //    - Feudal/Mercenary peak (Thirty Years' War plundering & chevauchées at T ~ 3.4)
                //    - 18th-century linear army lull (Cabinet wars at T ~ 4.2)
                //    - Industrial total war & urban siege surge (strategic bombing & mass artillery at T >= 5.0)
                // 3. Post-battle camp disease outbreak (Zinsser 1935; Smallman-Raynor & Cliff 2004)
                // 4. Male reproductive cohort decimation dampens fertility, inducing a historical wartime birth deficit
                double casualtyRate = Math.clamp(0.20 * (defPowerSq / totalPowerSq) * Math.sqrt(physicalFriction), 0.05, 0.45);

                int pop15 = conqueredCell.getPop15to24Or(0);
                int pop25 = conqueredCell.getPop25to49Or(0);
                int militaryCohort = pop15 + pop25;
                int militaryLosses = 0;
                if (militaryCohort > 0) {
                    int combatCasualties = (int) Math.round(militaryCohort * casualtyRate);
                    int cas15 = Math.min(pop15, (int) Math.round(combatCasualties * 0.75));
                    int cas25 = Math.min(pop25, combatCasualties - cas15);
                    conqueredCell.setPop15to24(pop15 - cas15);
                    conqueredCell.setPop25to49(pop25 - cas25);
                    militaryLosses = cas15 + cas25;
                }

                // Physical ordnance & logistics capital per capita of belligerents (Dupuy 1987, Understanding War;
                // Eckhardt 1989; Valentino 2004):
                // - Early gunpowder & mercenary siege logistics peak around k_pc ~ 550 kg/cap (Thirty Years' War chevauchées)
                // - 18th-century linear magazine supply lull around k_pc ~ 950 kg/cap (Cabinet wars)
                // - Industrial high-explosive artillery & aerial bombardment surge for k_pc >= 2800 kg/cap (Total War)
                double aggCapitalPerCapita = aggressor.getTerritory().stream().filter(Objects::nonNull)
                        .mapToDouble(c -> c.getResourceCapitalOr(0.0) / Math.max(1.0, (double) c.getPopulationOr(1)))
                        .average().orElse(20.0);
                double defCapitalPerCapita = defender.getTerritory().stream().filter(Objects::nonNull)
                        .mapToDouble(c -> c.getResourceCapitalOr(0.0) / Math.max(1.0, (double) c.getPopulationOr(1)))
                        .average().orElse(20.0);
                double meanOrdnanceCapital = Math.max(1.0, (aggCapitalPerCapita + defCapitalPerCapita) * 0.5);
                double logOrdnance = Math.log1p(meanOrdnanceCapital / 25.0);

                // Urbanization density ratio of contested frontline cell
                int cellPop = conqueredCell.getPopulationOr(0);
                double urbanFactor = Math.clamp(cellPop / 40000.0, 0.0, 1.0);

                // Physical ordnance-dependent civilian collateral envelope (Eckhardt 1989; Valentino 2004)
                // logOrdnance ~ 3.1 corresponds to k_pc ~ 550 kg/cap (Early Modern mercenary siege peak)
                // logOrdnance ~ 4.7 corresponds to k_pc ~ 2800 kg/cap (Industrial total war artillery/bombardment)
                double feudalMercenaryPeak = 0.28 * Math.exp(-Math.pow(logOrdnance - 3.1, 2.0) / (2.0 * 0.55 * 0.55));
                double industrialSurge = 0.45 / (1.0 + Math.exp(-2.2 * (logOrdnance - 4.7)));
                double baseCivRatio = Math.clamp(0.06 + feudalMercenaryPeak + industrialSurge, 0.05, 0.65);
                double civilianAttrition = casualtyRate * Math.clamp(baseCivRatio * (1.0 + 1.2 * urbanFactor), 0.04, 0.80);

                // Vulnerable cohorts (infants 0-4, elderly 65+) suffer 1.4x heightened mortality from famine/exposure
                double vulnerableAttrition = Math.min(0.90, civilianAttrition * 1.4);
                int cas0 = (int) Math.round(conqueredCell.getPop0to4Or(0) * vulnerableAttrition);
                int cas5 = (int) Math.round(conqueredCell.getPop5to14Or(0) * civilianAttrition);
                int cas50 = (int) Math.round(conqueredCell.getPop50to64Or(0) * civilianAttrition);
                int cas65 = (int) Math.round(conqueredCell.getPop65to79Or(0) - 0 + 0) > 0
                        ? (int) Math.round(conqueredCell.getPop65to79Or(0) * vulnerableAttrition) : 0;
                int cas80 = (int) Math.round(conqueredCell.getPop80PlusOr(0) * vulnerableAttrition);

                conqueredCell.setPop0to4(Math.max(0, conqueredCell.getPop0to4Or(0) - cas0));
                conqueredCell.setPop5to14(Math.max(0, conqueredCell.getPop5to14Or(0) - cas5));
                conqueredCell.setPop50to64(Math.max(0, conqueredCell.getPop50to64Or(0) - cas50));
                conqueredCell.setPop65to79(Math.max(0, conqueredCell.getPop65to79Or(0) - cas65));
                conqueredCell.setPop80Plus(Math.max(0, conqueredCell.getPop80PlusOr(0) - cas80));

                int civilianLosses = cas0 + cas5 + cas50 + cas65 + cas80;

                // Maintain exact closure of the demographic mass balance across all cohorts
                int newTotal = conqueredCell.getPop0to4Or(0) + conqueredCell.getPop5to14Or(0)
                        + conqueredCell.getPop15to24Or(0) + conqueredCell.getPop25to49Or(0)
                        + conqueredCell.getPop50to64Or(0) + conqueredCell.getPop65to79Or(0)
                        + conqueredCell.getPop80PlusOr(0);
                conqueredCell.setPopulation(Math.max(10, newTotal));

                // Male reproductive cohort decimation dampens fertility, inducing a historical wartime birth deficit
                double curFertility = conqueredCell.getFertilityOr(4.0);
                double maleLossRatio = militaryCohort > 0 ? (double) militaryLosses / militaryCohort : 0.0;
                double sexRatioBirthPenalty = 1.0 - (casualtyRate * 0.45 * Math.max(0.1, maleLossRatio));
                conqueredCell.setFertility(Math.max(1.1, curFertility * sexRatioBirthPenalty));

                // Post-Battle Camp Epidemics & Morbidity (Zinsser 1935; Smallman-Raynor & Cliff 2004)
                int totalLosses = militaryLosses + civilianLosses;
                if (totalLosses > 10) {
                    double contagionLoad = totalLosses * (0.25 + 0.15 * urbanFactor);
                    int newInfected = Math.max(5, (int) Math.round(contagionLoad));
                    int currentInfected = conqueredCell.getEpidemicInfectedOr(0);
                    int maxInfectable = Math.max(1, (int) (newTotal * 0.45));
                    conqueredCell.setEpidemicInfected(Math.min(maxInfectable, currentInfected + newInfected));

                    String epidemicPathogen;
                    if (meanOrdnanceCapital < 800.0) {
                        epidemicPathogen = "Camp Fever (Typhus & Dysentery)";
                    } else if (meanOrdnanceCapital < 5000.0) {
                        epidemicPathogen = "Cholera & Trench Typhus";
                    } else {
                        epidemicPathogen = "Wartime Pathogen & Epidemic Sepsis";
                    }
                    conqueredCell.setActivePathogenName(epidemicPathogen);

                    // Acute sanitation & public health shock depressing life expectancy
                    double curLifespan = conqueredCell.getLifespanOr(40.0);
                    conqueredCell.setLifespan(Math.max(15.0, curLifespan - (2.5 * casualtyRate * (1.0 + urbanFactor))));
                }

                // Collateral infrastructure & capital destruction on the battlefield cell
                double cellCap = conqueredCell.getResourceCapitalOr(0.0);
                conqueredCell.setResourceCapital(Math.max(0.0, cellCap * (1.0 - 0.25 * casualtyRate)));

                logger.info("War Resolution: Nation '{}' conquered frontline border cell {} from Nation '{}' (Boundary Friction: {}). Casualties: {} military, {} civilian.",
                        aggressor.getName(), conqueredCell.getH3Index(), defender.getName(),
                        String.format(Locale.ROOT, "%.2f", physicalFriction), militaryLosses, civilianLosses);
            }
        }
    }
}
