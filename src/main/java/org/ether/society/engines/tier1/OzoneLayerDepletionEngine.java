/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stratospheric Ozone Layer Depletion & CFC Photolysis Engine.
 * Models:
 * 1. <b>CFC Photolysis & Stratospheric Ozone Loss</b>: Industrial chlorofluorocarbons destroy O3, causing UV-B radiation influx.
 * 2. <b>UV-B Biological Impact</b>: UV-B influx damages crop photosynthetic DNA and increases human mortality.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class OzoneLayerDepletionEngine extends AbstractSimulationEngine {
    public OzoneLayerDepletionEngine() {
        super("OzoneLayerDepletionEngine", "OzoneLayerDepletionEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(OzoneLayerDepletionEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        double pollution = buffer.getPollutionLevel()[i];
        if (pollution > 5000.0) {
            double dtSafe = Math.max(0.0001, dtYears);
            double cropDecay = Math.exp(-0.999 * dtSafe);
            double currentAgri = buffer.getBiomassAgriculture()[i];
            if (currentAgri > 0.0) {
                double nextAgri = Math.max(1.0, currentAgri * cropDecay);
                acc.dBiomassAgriculture += (float) (nextAgri - currentAgri);
            }
        }
    }

    /**
     * Executes stratospheric ozone depletion and UV-B radiation influx tick with default monthly step.
     */
    public static void processOzoneLayerDepletion(WorldBuffer buffer) {
        processOzoneLayerDepletion(buffer, 1.0 / 12.0);
    }

    /**
     * Executes stratospheric ozone depletion and UV-B radiation influx tick scaled by dtYears.
     */
    public static void processOzoneLayerDepletion(WorldBuffer buffer, double dtYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        double dtSafe = Math.max(0.0001, dtYears);
        double cropDecay = Math.exp(-0.999 * dtSafe);
        int ozoneDepletionEvents = 0;

        float[] pollutionArr = buffer.getPollutionLevel();
        float[] agriArr = buffer.getBiomassAgriculture();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double pollution = pollutionArr[i];

            // High industrial chemical pollution releases CFCs destroying O3
            if (pollution > 5000.0) {
                ozoneDepletionEvents++;
                // UV-B damage to crop DNA
                double currentAgri = agriArr[i];
                if (currentAgri > 0.0) {
                    agriArr[i] = (float) Math.max(1.0, currentAgri * cropDecay);
                }
            }
        }

        if (ozoneDepletionEvents > 0) {
            logger.info("Ozone Engine: Stratospheric O3 depletion and UV-B influx active across {} industrial cells.", ozoneDepletionEvents);
        }
    }
}
