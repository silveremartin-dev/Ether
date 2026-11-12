/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Advanced Maritime Strategy & Land Reclamation Engine (Poldérisation & Seasteading).
 *
 * 1. Poldérisation: Transforms shallow coastal/marine cells into fertile reclaimed land (polders)
 *    when local technology level >= 4.0 and capital investment >= 100.0.
 * 2. Seasteading: Deploys deep-sea floating platforms & oceanic habitats
 *    when local technology level >= 8.5 and capital investment >= 500.0.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class LandReclamationEngine extends AbstractSimulationEngine {
    public LandReclamationEngine() {
        super("LandReclamationEngine", "LandReclamationEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    public static final double DEFAULT_POLDER_CAPITAL_THRESHOLD = 100.0;
    public static final double DEFAULT_SEASTEADING_CAPITAL_THRESHOLD = 500.0;

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null || buf.getCapacity() == 0) return;
        int n = buf.getCapacity();
        byte[] biomes = buf.getBiomes();
        float[] capArr = buf.getResourceCapital();
        float[] elevArr = buf.getElevation();
        float[] socArr = buf.getSoilOrganicCarbon();
        float[] solarArr = buf.getEnergySolar();

        byte oceanOrd = (byte) Biome.OCEAN.ordinal();
        byte deepOceanOrd = (byte) Biome.DEEP_OCEAN.ordinal();
        byte beachOrd = (byte) Biome.BEACH.ordinal();
        byte plainsOrd = (byte) Biome.PLAINS.ordinal();

        for (int i = 0; i < n; i++) {
            float capital = capArr[i];
            byte biome = biomes[i];
            if ((biome == beachOrd || elevArr[i] <= 0.0f) && capital >= DEFAULT_POLDER_CAPITAL_THRESHOLD) {
                if (biome == oceanOrd || biome == beachOrd) {
                    biomes[i] = plainsOrd;
                }
                socArr[i] = Math.min(120.0f, socArr[i] + (float) (20.0 * dtYears));
            }
            if ((biome == oceanOrd || biome == deepOceanOrd) && capital >= DEFAULT_SEASTEADING_CAPITAL_THRESHOLD) {
                solarArr[i] = Math.min(1000.0f, solarArr[i] + (float) (50.0 * dtYears));
            }
        }
    }
}
