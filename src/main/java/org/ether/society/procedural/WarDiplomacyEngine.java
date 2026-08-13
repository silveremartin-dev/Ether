/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.ether.society.model.Nation;
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
 * @version 2.7.0
 */
public class WarDiplomacyEngine {
    private static final Logger logger = LoggerFactory.getLogger(WarDiplomacyEngine.class);

    /**
     * Calculates the boundary friction (sigma_friction) between two adjacent cells.
     * Incorporates sovereignty gradient mismatch, terrain movement friction, and elevation declivity.
     */
    public static double calculateBoundaryFriction(H3Cell c1, H3Cell c2) {
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
    public static void processGeopoliticalConflicts(List<Nation> nations, List<H3Cell> cells) {
        if (nations == null || nations.size() < 2) return;

        for (int i = 0; i < nations.size() - 1; i++) {
            Nation aggressor = nations.get(i);
            if (aggressor.getTerritory().isEmpty() || aggressor.getAsabiyyah() < 0.40) continue;

            for (int j = i + 1; j < nations.size(); j++) {
                Nation defender = nations.get(j);
                if (defender.getTerritory().isEmpty()) continue;

                // Check territorial adjacency & compute average boundary friction
                double avgBorderFriction = getAverageBorderFriction(aggressor, defender);
                if (avgBorderFriction > 0) {
                    // War trigger condition: High aggressor Asabiyyah + Weak defender State Capacity / High Instability, modulated by boundary friction
                    double baseWarDesire = (aggressor.getAsabiyyah() * 0.6) + (defender.getPoliticalInstability() * 0.4) - (defender.getStateCapacity() * 0.3);
                    double warDesire = baseWarDesire / Math.sqrt(avgBorderFriction);

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
    private static double getAverageBorderFriction(Nation n1, Nation n2) {
        double totalFriction = 0.0;
        int count = 0;

        for (H3Cell c1 : n1.getTerritory()) {
            for (H3Cell c2 : n2.getTerritory()) {
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
     * Resolves military engagement over a border cell using the 15-24 age military cohort with boundary friction resistance.
     */
    private static void resolveBorderBattle(Nation aggressor, Nation defender, double borderFriction) {
        // Aggregate military workforce (pop15to24)
        long aggressorForce = aggressor.getTerritory().stream().mapToLong(c -> c.getPop15to24() != null ? c.getPop15to24() : 0).sum();
        long defenderForce = defender.getTerritory().stream().mapToLong(c -> c.getPop15to24() != null ? c.getPop15to24() : 0).sum();

        if (aggressorForce <= 0 || defenderForce <= 0) return;

        // Force ratio weighted by State Capacity and penalized by boundary friction
        double aggressorPower = (aggressorForce * (0.5 + aggressor.getStateCapacity())) / borderFriction;
        double defenderPower = defenderForce * (0.5 + defender.getStateCapacity());

        if (aggressorPower > defenderPower * 1.25) {
            // Aggressor conquers one border cell of defender
            H3Cell conqueredCell = defender.getTerritory().stream()
                    .filter(c -> c != defender.getCapital())
                    .findAny().orElse(null);

            if (conqueredCell != null) {
                defender.removeCell(conqueredCell);
                aggressor.addCell(conqueredCell);

                // War casualties in 15-24 military cohort scale with boundary friction resistance
                double casualtyRate = Math.clamp(0.15 * Math.sqrt(borderFriction), 0.10, 0.45);
                int casualties = (int) (conqueredCell.getPop15to24() * casualtyRate);
                conqueredCell.setPop15to24(Math.max(0, conqueredCell.getPop15to24() - casualties));
                conqueredCell.setPopulation(Math.max(0, conqueredCell.getPopulation() - casualties));

                logger.info("War Resolution: Nation '{}' conquered cell {} from Nation '{}' (Boundary Friction: {:.2f}). Military casualties: {}.",
                        aggressor.getName(), conqueredCell.getH3Index(), defender.getName(), borderFriction, casualties);
            }
        }
    }
}
