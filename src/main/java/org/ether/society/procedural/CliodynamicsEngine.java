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

import java.util.ArrayList;
import java.util.List;

/**
 * Cliodynamics Engine (Peter Turchin Secular Cycles & Structural-Demographic Theory).
 * Simulates:
 * 1. Asabiyyah (Social Cohesion / Solidarity Dynamics)
 * 2. Elite Overproduction & Intra-Elite Competition
 * 3. Political Instability Index (PSI)
 * 4. State Fragmentation & Rebellions when instability reaches critical thresholds.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.4.0
 */
public class CliodynamicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(CliodynamicsEngine.class);

    /**
     * Executes one tick of Cliodynamics update across all nations and territory cells.
     */
    public static void updateCliodynamics(List<Nation> nations, List<H3Cell> cells) {
        if (nations == null || nations.isEmpty()) return;

        for (Nation nation : nations) {
            nation.updateCliodynamicsCycle();

            double psi = nation.getPoliticalInstability();
            double asabiyyah = nation.getAsabiyyah();

            if (psi > 0.75) {
                logger.warn("High Political Instability in nation '{}' (PSI: {}, Asabiyyah: {}). Triggering social unrest.",
                        nation.getName(), String.format("%.2f", psi), String.format("%.2f", asabiyyah));
                handlePoliticalUnrest(nation);
            }
        }
    }

    /**
     * Handles social unrest, riots, or balkanization when political instability crosses critical thresholds.
     */
    private static void handlePoliticalUnrest(Nation nation) {
        if (nation == null || nation.getTerritory().isEmpty()) return;

        List<H3Cell> territory = new ArrayList<>(nation.getTerritory());
        int countToRemove = Math.max(1, (int) (territory.size() * 0.15));

        // Peripheral cells defect or break away
        for (int i = 0; i < countToRemove && i < territory.size(); i++) {
            H3Cell cell = territory.get(territory.size() - 1 - i);
            if (cell != nation.getCapital()) {
                nation.removeCell(cell);
                // Population loss from civil strife
                cell.setPopulation((int) (cell.getPopulation() * 0.90));
            }
        }

        // Restores some Asabiyyah post-crisis (resetting secular cycle)
        nation.setAsabiyyah(Math.min(1.0, nation.getAsabiyyah() + 0.15));
        nation.setEliteOverproduction(Math.max(0.1, nation.getEliteOverproduction() - 0.20));
    }
}
