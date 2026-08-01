/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Material Recycling & Circular Economy Engine.
 * Models:
 * 1. <b>Circular Re-use of Materials</b>: Glass (SiO₂), Refined Metals (Fe/Cu), Timber, and Stone.
 * 2. <b>Thermodynamic Enthalpy Discount (up to 80% Energy Savings)</b>: Re-melting scrap glass/metal requires only 20% of the initial raw ore smelting enthalpy.
 *
 * @author Silvere Martin-Michiellot
 * @version 3.4.0
 */
public class ResourceRecyclingEngine {
    private static final Logger logger = LoggerFactory.getLogger(ResourceRecyclingEngine.class);

    /**
     * Executes one material recycling tick across cells.
     */
    public static void processResourceRecycling(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int recyclingHubs = 0;

        for (H3Cell cell : cells) {
            double tech = cell.getTechnologyLevel() != null ? cell.getTechnologyLevel() : 0.0;
            double capital = cell.getResourceCapital() != null ? cell.getResourceCapital() : 0.0;

            // Advanced urban settlements (Tech >= 5.0) recycle scrap metals & glass
            if (tech >= 5.0 && capital > 3000.0) {
                recyclingHubs++;
                // Enthalpy discount converts scrap back into structural capital at 80% energy efficiency
                double recycledCapitalGain = capital * 0.03;
                cell.setResourceCapital(capital + recycledCapitalGain);
            }
        }

        if (recyclingHubs > 0) {
            logger.info("Recycling Engine: Circular economy enthalpy discount active across {} urban recycling hubs.", recyclingHubs);
        }
    }
}
