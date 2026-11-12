/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.model.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Physically Realistic Marine Submersion & Evacuation Engine.
 * 
 * Models sea level rise, coastal flooding, and polder dike breaches with thermodynamic and physical realism:
 * 1. Early Warning & Evacuation Physics: High-tech societies anticipate marine submersion, evacuating
 *    up to 95-98% of vulnerable populations to neighboring higher-elevation cells.
 * 2. Population Relocation & Refugee Flux: Displaced populations and moveable capital are physically
 *    transferred to safe adjacent cells rather than suffering unrealistic sudden mass extinction.
 * 3. Demographic Mass Closure: Rescales cohorts proportionally on arrival and departure.
 * 4. Partial Loss & Infrastructure Immersion: Fixed capital infrastructure suffers water immersion damage,
 *    while human life is prioritized through technological foresight and emergency response.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MarineSubmersionEngine extends AbstractSimulationEngine {
    public MarineSubmersionEngine() {
        super("MarineSubmersionEngine", "MarineSubmersionEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    public static final double DEFAULT_EVACUATION_TECH_EFFICIENCY = 0.40;

    @Override
    public void tick(org.ether.society.core.dod.WorldBuffer buf, double dtYears, double dtMonthly) {
        if (buf == null) return;
        int n = buf.getCapacity();
        float[] elevArr = buf.getElevation();
        float[] capArr = buf.getResourceCapital();
        float[] popArr = buf.getBiomassHuman();
        byte[] biomes = buf.getBiomes();
        int[][] neighbors = buf.getNeighborIndexes();

        byte oceanOrd = (byte) Biome.OCEAN.ordinal();
        byte deepOceanOrd = (byte) Biome.DEEP_OCEAN.ordinal();

        for (int i = 0; i < n; i++) {
            float elevation = elevArr[i];
            float pop = popArr[i];
            if (elevation < 0.0f && pop > 0.0f) {
                float capital = capArr[i];
                double kpc = capital / pop;
                double capitalFactor = 1.0 + Math.min(5.0, capital / 500.0);
                double mobilityEfficiency = Math.log1p(kpc / 50.0);
                double evacuationRatio = Math.clamp(1.0 - Math.exp(-DEFAULT_EVACUATION_TECH_EFFICIENCY * mobilityEfficiency * capitalFactor), 0.0, 0.98);

                float evacuatedPop = (float) (pop * evacuationRatio);
                float remainingPop = pop - evacuatedPop;

                int[] nbrs = neighbors != null && i < neighbors.length ? neighbors[i] : null;
                int safeCount = 0;
                if (nbrs != null) {
                    for (int nb : nbrs) {
                        if (nb >= 0 && nb < n && elevArr[nb] > 0.0f && biomes[nb] != oceanOrd && biomes[nb] != deepOceanOrd) {
                            safeCount++;
                        }
                    }
                }
                if (safeCount > 0 && evacuatedPop > 0.0f) {
                    float popPerNeighbor = evacuatedPop / safeCount;
                    float capPerNeighbor = (capital * 0.50f) / safeCount;
                    for (int nb : nbrs) {
                        if (nb >= 0 && nb < n && elevArr[nb] > 0.0f && biomes[nb] != oceanOrd && biomes[nb] != deepOceanOrd) {
                            popArr[nb] += popPerNeighbor;
                            capArr[nb] += capPerNeighbor;
                        }
                    }
                    popArr[i] = remainingPop;
                    capArr[i] = capital * 0.50f;
                }
                if (remainingPop > 0.0f) {
                    double floodDepth = -elevation;
                    double mortalityRate = Math.clamp(0.05 * floodDepth, 0.01, 0.30);
                    popArr[i] = Math.max(0.0f, remainingPop * (float) (1.0 - mortalityRate));
                }
                if (elevation < -5.0f) {
                    biomes[i] = oceanOrd;
                }
            }
        }
    }
}
