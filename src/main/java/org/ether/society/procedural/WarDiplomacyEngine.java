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
 * 1. War declarations based on expansionism (Asabiyyah), resource scarcity, and target instability.
 * 2. Military battles using the youth adult recruitment cohort (pop15to24).
 * 3. Dynamic territorial conquest and border shifting.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.6.0
 */
public class WarDiplomacyEngine {
    private static final Logger logger = LoggerFactory.getLogger(WarDiplomacyEngine.class);

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

                // Check territorial adjacency
                if (areNationsNeighbors(aggressor, defender)) {
                    // War trigger condition: High aggressor Asabiyyah + Weak defender State Capacity / High Instability
                    double warDesire = (aggressor.getAsabiyyah() * 0.6) + (defender.getPoliticalInstability() * 0.4) - (defender.getStateCapacity() * 0.3);

                    if (warDesire > 0.65) {
                        resolveBorderBattle(aggressor, defender);
                    }
                }
            }
        }
    }

    /**
     * Determines whether two nations share a common border.
     */
    private static boolean areNationsNeighbors(Nation n1, Nation n2) {
        for (H3Cell c1 : n1.getTerritory()) {
            for (H3Cell c2 : n2.getTerritory()) {
                double dist = Math.hypot(c1.getLatitude() - c2.getLatitude(), c1.getLongitude() - c2.getLongitude());
                if (dist < 2.5) return true;
            }
        }
        return false;
    }

    /**
     * Resolves military engagement over a border cell using the 15-24 age military cohort.
     */
    private static void resolveBorderBattle(Nation aggressor, Nation defender) {
        // Aggregate military workforce (pop15to24)
        long aggressorForce = aggressor.getTerritory().stream().mapToLong(c -> c.getPop15to24() != null ? c.getPop15to24() : 0).sum();
        long defenderForce = defender.getTerritory().stream().mapToLong(c -> c.getPop15to24() != null ? c.getPop15to24() : 0).sum();

        if (aggressorForce <= 0 || defenderForce <= 0) return;

        // Force ratio weighted by State Capacity
        double aggressorPower = aggressorForce * (0.5 + aggressor.getStateCapacity());
        double defenderPower = defenderForce * (0.5 + defender.getStateCapacity());

        if (aggressorPower > defenderPower * 1.25) {
            // Aggressor conquers one border cell of defender
            H3Cell conqueredCell = defender.getTerritory().stream()
                    .filter(c -> c != defender.getCapital())
                    .findAny().orElse(null);

            if (conqueredCell != null) {
                defender.removeCell(conqueredCell);
                aggressor.addCell(conqueredCell);

                // War casualties in 15-24 military cohort
                int casualties = (int) (conqueredCell.getPop15to24() * 0.20);
                conqueredCell.setPop15to24(Math.max(0, conqueredCell.getPop15to24() - casualties));
                conqueredCell.setPopulation(Math.max(0, conqueredCell.getPopulation() - casualties));

                logger.info("War Resolution: Nation '{}' conquered cell {} from Nation '{}'. Military casualties: {}.",
                        aggressor.getName(), conqueredCell.getH3Index(), defender.getName(), casualties);
            }
        }
    }
}
