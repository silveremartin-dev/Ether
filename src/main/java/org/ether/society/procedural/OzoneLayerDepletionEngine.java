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
 * Stratospheric Ozone Layer Depletion & CFC Photolysis Engine.
 * Models:
 * 1. <b>CFC Photolysis & Stratospheric Ozone Loss</b>: Industrial chlorofluorocarbons destroy O3, causing UV-B radiation influx.
 * 2. <b>UV-B Biological Impact</b>: UV-B influx damages crop photosynthetic DNA and increases human mortality.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class OzoneLayerDepletionEngine {
    private static final Logger logger = LoggerFactory.getLogger(OzoneLayerDepletionEngine.class);

    /**
     * Executes stratospheric ozone depletion and UV-B radiation influx tick.
     */
    public static void processOzoneLayerDepletion(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int ozoneDepletionEvents = 0;

        for (H3Cell cell : cells) {
            double pollution = cell.getPollutionLevel() != null ? cell.getPollutionLevel() : 0.0;

            // High industrial chemical pollution releases CFCs destroying O3
            if (pollution > 5000.0) {
                ozoneDepletionEvents++;
                // UV-B damage to crop DNA
                double currentAgri = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
                cell.setBiomassAgriculture(Math.max(10.0, currentAgri * 0.92));
            }
        }

        if (ozoneDepletionEvents > 0) {
            logger.info("Ozone Engine: Stratospheric O3 depletion and UV-B influx active across {} industrial cells.", ozoneDepletionEvents);
        }
    }
}

