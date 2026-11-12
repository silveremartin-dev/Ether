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
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Permafrost Thaw & Methane (CHâ‚„) Outgassing Engine.
 * Models:
 * 1. Polar & tundra permafrost thaw when temperature exceeds 0Â°C.
 * 2. Methane gas (CHâ‚„) release into atmosphere, accelerating global greenhouse warming.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PermafrostThawEngine extends AbstractSimulationEngine {
    public PermafrostThawEngine() {
        super("PermafrostThawEngine", "PermafrostThawEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(PermafrostThawEngine.class);

    /**
     * Executes one permafrost thaw update step with default monthly step.
     *
     * @param cells List of H3 cells
     * @return Total methane outgassing amount in abstract units (ppm equivalent)
     */
    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        byte biomeOrd = buffer.getBiomes()[i];
        if (biomeOrd != (byte) Biome.SNOW.ordinal() && biomeOrd != (byte) Biome.TUNDRA.ordinal()) {
            return;
        }
        double temp = buffer.getTemperature()[i];
        if (temp > 0.0) {
            double dtSafe = Math.max(0.0001, dtYears);
            double soc = buffer.getSoilOrganicCarbon()[i];
            double nextSoc = Math.max(5.0, soc - 0.2 * dtSafe);
            acc.dSoilOrganicCarbon += (float) (nextSoc - soc);
        }
    }

    /**
     * Executes one permafrost thaw update step with default monthly step.
     *
     * @param buffer WorldBuffer DoD state
     * @return Total methane outgassing amount in abstract units (ppm equivalent)
     */
    public static double processPermafrostThaw(WorldBuffer buffer) {
        return processPermafrostThaw(buffer, 1.0 / 12.0);
    }

    /**
     * Executes one permafrost thaw update step scaled by dtYears.
     *
     * @param buffer WorldBuffer DoD state
     * @param dtYears Time step in years
     * @return Total methane outgassing amount in abstract units (ppm equivalent)
     */
    public static double processPermafrostThaw(WorldBuffer buffer, double dtYears) {
        if (buffer == null || buffer.getCapacity() == 0) return 0.0;

        double dtSafe = Math.max(0.0001, dtYears);
        double totalMethaneOutgassing = 0.0;
        int thawingCells = 0;

        byte[] biomes = buffer.getBiomes();
        float[] tempArr = buffer.getTemperature();
        float[] socArr = buffer.getSoilOrganicCarbon();
        byte snowOrd = (byte) Biome.SNOW.ordinal();
        byte tundraOrd = (byte) Biome.TUNDRA.ordinal();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            byte biomeOrd = biomes[i];
            double temp = tempArr[i];

            // Polar / Tundra permafrost thawing condition (T > 0 C)
            if ((biomeOrd == snowOrd || biomeOrd == tundraOrd) && temp > 0.0) {
                thawingCells++;
                double methaneRelease = temp * 0.15 * dtSafe;
                totalMethaneOutgassing += methaneRelease;

                // Ground thaw converts tundra to wetland / taiga, boosting soil organic carbon loss
                double soc = socArr[i];
                socArr[i] = (float) Math.max(5.0, soc - 0.2 * dtSafe);
            }
        }

        if (thawingCells > 0) {
            logger.info("Permafrost Engine: Thawing permafrost across {} polar/tundra cells released {} ppm CH4 equivalent.",
                    thawingCells, String.format(java.util.Locale.ROOT, "%.2f", totalMethaneOutgassing));
        }

        return totalMethaneOutgassing;
    }
}
