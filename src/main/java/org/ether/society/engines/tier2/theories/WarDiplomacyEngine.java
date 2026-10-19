/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
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
     * Calculates the boundary friction (sigma_friction) between two adjacent cells.
     * Incorporates sovereignty gradient mismatch, terrain movement friction, and elevation declivity.
     */
    public static double calculateBoundaryFriction(H3Cell c1, H3Cell c2) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (c1 == null || c2 == null) return 1.0;

        double baseFriction1 = c1.getMovementFriction() != null ? c1.getMovementFriction() : 1.0;
        double baseFriction2 = c2.getMovementFriction() != null ? c2.getMovementFriction() : 1.0;
        double avgTerrainFriction = (baseFriction1 + baseFriction2) / 2.0;

        // Elevation gradient resistance
        double elev1 = c1.getElevation() != null ? c1.getElevation() : 0.0;
        double elev2 = c2.getElevation() != null ? c2.getElevation() : 0.0;
        double elevDiff = Math.abs(elev1 - elev2) / 500.0;

        // Sovereignty mask transition penalty
        double sovereigntyGradient = 0.0;
        if (c1.getOwner() != c2.getOwner()) {
            sovereigntyGradient = 1.5; // High friction across geopolitical borders
        }

        double sigmaFriction = 1.0 + (avgTerrainFriction * 0.4) + (elevDiff * 0.3) + sovereigntyGradient;
        return Math.clamp(sigmaFriction, 1.0, 20.0);
    }

    /**
     * Executes geopolitical evaluation and resolves border conflicts across active nations.
     */
    public static void processGeopoliticalConflicts(List<Polity> nations, List<H3Cell> cells) {
        processGeopoliticalConflicts(nations, cells, null);
    }

    public static void processGeopoliticalConflicts(List<Polity> nations, List<H3Cell> cells, org.ether.society.model.Scenario scenario) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (nations == null || nations.size() < 2) return;

        for (int i = 0; i < nations.size() - 1; i++) {
            Polity aggressor = nations.get(i);
            if (aggressor.getTerritory().isEmpty() || aggressor.getAsabiyyah() < 0.40) continue;

            for (int j = i + 1; j < nations.size(); j++) {
                Polity defender = nations.get(j);
                if (defender.getTerritory().isEmpty()) continue;

                // Check territorial adjacency & compute average boundary friction
                double avgBorderFriction = getAverageBorderFriction(aggressor, defender);
                if (avgBorderFriction > 0) {
                    // War trigger condition: High aggressor Asabiyyah + Weak defender State Capacity / High Instability, modulated by boundary friction
                    double baseWarDesire = (aggressor.getAsabiyyah() * 0.6) + (defender.getPoliticalInstability() * 0.4) - (defender.getStateCapacity() * 0.3);

                    // Cliodynamic Cultural Affinity Matrix Modulation:
                    // High bilateral affinity (e.g. 80-100%) acts as strong pacification factor.
                    // Low affinity (e.g. 0-20% hostile animosity) amplifies belligerence.
                    double affinityMultiplier = 1.0;
                    if (scenario != null) {
                        double affinity = scenario.getPairwiseAffinity(aggressor.getId(), defender.getId());
                        // Affinity 50% => 1.0x, Affinity 0% => 1.5x (hostility), Affinity 100% => 0.2x (pax)
                        affinityMultiplier = Math.clamp(1.5 - (affinity / 100.0) * 1.3, 0.1, 2.0);
                    }

                    double warDesire = (baseWarDesire * affinityMultiplier) / Math.sqrt(avgBorderFriction);

                    if (warDesire > 0.65) {
                        resolveBorderBattle(aggressor, defender, avgBorderFriction);
                    }
                }
            }
        }
    }

    /**
     * Computes average boundary friction across common border cells of two nations.
     * Returns -1 if nations do not share a border.
     */
    private static double getAverageBorderFriction(Polity n1, Polity n2) {
        double totalFriction = 0.0;
        int count = 0;

        for (H3Cell c1 : n1.getTerritory()) {
            if (c1 == null) continue;
            // Iterate over spatial cell domains and apply localized cellular state transformations
            for (H3Cell c2 : n2.getTerritory()) {
                if (c2 == null) continue;
                double dist = Math.hypot(c1.getLatitude() - c2.getLatitude(), c1.getLongitude() - c2.getLongitude());
                if (dist < 2.5) {
                    totalFriction += calculateBoundaryFriction(c1, c2);
                    count++;
                }
            }
        }
        return count > 0 ? (totalFriction / count) : -1.0;
    }

    /**
     * Resolves military engagement over a border cell using the 15-24 age military cohort,
     * physical armament capital ($K$), cavalry/motorized logistics, and sovereign bullion war chests,
     * with boundary friction resistance and post-battle physical capital attrition.
     *
     * @param aggressor      attacking nation
     * @param defender       defending nation
     * @param borderFriction geopolitical and topographic boundary friction $\sigma_{\text{friction}}$
     */
    private static void resolveBorderBattle(Polity aggressor, Polity defender, double borderFriction) {
        // Step 1: Aggregate military recruitment cohort (pop15to24)
        long aggressorForce = aggressor.getTerritory().stream().filter(java.util.Objects::nonNull).mapToLong(c -> c.getPop15to24() != null ? c.getPop15to24() : 0).sum();
        long defenderForce = defender.getTerritory().stream().filter(java.util.Objects::nonNull).mapToLong(c -> c.getPop15to24() != null ? c.getPop15to24() : 0).sum();

        if (aggressorForce <= 0 || defenderForce <= 0) return;

        // Step 2: Aggregate physical armament & fortification capital (K), cavalry/fuel mobility, and metallurgical tech
        double aggCapital = aggressor.getTerritory().stream().filter(java.util.Objects::nonNull).mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).sum();
        double defCapital = defender.getTerritory().stream().filter(java.util.Objects::nonNull).mapToDouble(c -> c.getResourceCapital() != null ? c.getResourceCapital() : 0.0).sum();

        double aggMobility = aggressor.getTerritory().stream().filter(java.util.Objects::nonNull).mapToDouble(c ->
                (c.getBiomassLivestock() != null ? c.getBiomassLivestock() : 0.0) * 0.2
                + (c.getResourceOil() != null ? c.getResourceOil() : 0.0) * 0.5).sum();
        double defMobility = defender.getTerritory().stream().filter(java.util.Objects::nonNull).mapToDouble(c ->
                (c.getBiomassLivestock() != null ? c.getBiomassLivestock() : 0.0) * 0.2
                + (c.getResourceOil() != null ? c.getResourceOil() : 0.0) * 0.5).sum();

        // Armament multiplier per soldier (bronze/iron/steel weapons, armor, siege engines)
        double aggArmamentFactor = 1.0 + Math.min(2.5, Math.log1p(aggCapital / Math.max(1.0, aggressorForce)));
        double defArmamentFactor = 1.0 + Math.min(2.5, Math.log1p(defCapital / Math.max(1.0, defenderForce)));

        // Logistics & cavalry/motorized projection multiplier
        double aggLogisticsFactor = 1.0 + Math.min(1.0, Math.log1p(aggMobility / Math.max(1.0, aggressorForce)));
        double defLogisticsFactor = 1.0 + Math.min(1.0, Math.log1p(defMobility / Math.max(1.0, defenderForce)));

        // Sovereign bullion war chest (mercenary & standing army pay: "nerf de la guerre")
        double aggBullionFactor = 1.0 + Math.min(0.6, Math.log1p(aggressor.getTreasuryBullion() / 50.0) * 0.15);
        double defBullionFactor = 1.0 + Math.min(0.6, Math.log1p(defender.getTreasuryBullion() / 50.0) * 0.15);

        // Force ratio weighted by State Capacity, Armament Capital, Logistics, Bullion, and penalized by boundary friction
        double safeFriction = Math.max(0.1, borderFriction);
        double aggressorPower = (aggressorForce * (0.5 + aggressor.getStateCapacity()) * aggArmamentFactor * aggLogisticsFactor * aggBullionFactor) / safeFriction;
        double defenderPower = defenderForce * (0.5 + defender.getStateCapacity()) * defArmamentFactor * defLogisticsFactor * defBullionFactor;

        // Step 3: Deduct military campaign bullion expenditure and physical weapon attrition for both belligerents
        aggressor.setTreasuryBullion(aggressor.getTreasuryBullion() * 0.92);
        defender.setTreasuryBullion(defender.getTreasuryBullion() * 0.95);
        for (H3Cell c : aggressor.getTerritory()) {
            if (c != null && c.getResourceCapital() != null) {
                c.setResourceCapital(Math.max(0.0, c.getResourceCapital() * 0.97));
            }
        }
        for (H3Cell c : defender.getTerritory()) {
            if (c != null && c.getResourceCapital() != null) {
                c.setResourceCapital(Math.max(0.0, c.getResourceCapital() * 0.96));
            }
        }

        if (aggressorPower > defenderPower * 1.25) {
            // Aggressor conquers one border cell of defender
            H3Cell conqueredCell = defender.getTerritory().stream()
                    .filter(c -> c != defender.getCapital())
                    .findAny().orElse(null);

            if (conqueredCell != null) {
                defender.removeCell(conqueredCell);
                aggressor.addCell(conqueredCell);

                // War casualties in 15-24 military cohort scale with boundary friction resistance
                double casualtyRate = Math.clamp(0.15 * Math.sqrt(safeFriction), 0.10, 0.45);
                int curPop15to24 = conqueredCell.getPop15to24() != null ? conqueredCell.getPop15to24() : 0;
                int curPop = conqueredCell.getPopulation() != null ? conqueredCell.getPopulation() : 0;
                int casualties = (int) (curPop15to24 * casualtyRate);
                conqueredCell.setPop15to24(Math.max(0, curPop15to24 - casualties));
                conqueredCell.setPopulation(Math.max(0, curPop - casualties));

                // Collateral infrastructure & weapon destruction on the battlefield cell
                double cellCap = conqueredCell.getResourceCapital() != null ? conqueredCell.getResourceCapital() : 0.0;
                conqueredCell.setResourceCapital(Math.max(0.0, cellCap * (1.0 - 0.25 * casualtyRate)));

                logger.info("War Resolution: Nation '{}' conquered cell {} from Nation '{}' (Boundary Friction: {}). Military casualties: {}.",
                        aggressor.getName(), conqueredCell.getH3Index(), defender.getName(), String.format(java.util.Locale.ROOT, "%.2f", safeFriction), casualties);
            }
        }
    }
}


