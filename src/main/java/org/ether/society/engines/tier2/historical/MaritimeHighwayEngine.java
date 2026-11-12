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
 * Generic Hydrodynamic Maritime Highway & Coastal Trade Engine.
 * Modifies transport friction and capital accumulation based on hydrodynamic fluid efficiency
 * (Archimedes buoyancy vs dry land friction) and thermodynamic ice constraints.
 *
 * Replaces geographic latitude heuristics with universal topological and physical properties.
 * Applicable to any global, regional, or alien sea basin.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class MaritimeHighwayEngine extends AbstractSimulationEngine {
    public MaritimeHighwayEngine() {
        super("MaritimeHighwayEngine", "MaritimeHighwayEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    public static final double DEFAULT_CAPITAL_BOOST_RATE = 0.05;

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double pop = buffer.getBiomassHuman()[i];
        if (pop <= 0.0 || dtYears <= 0.0) return;

        byte biome = buffer.getBiomes()[i];
        boolean isMaritimeBasin = biome == Biome.OCEAN.ordinal()
                || biome == Biome.DEEP_OCEAN.ordinal()
                || buffer.getElevation()[i] <= 0.0f;

        if (isMaritimeBasin && buffer.getTemperature()[i] > 0.0f) {
            double currentCap = buffer.getResourceCapital()[i];
            double capPerCapita = currentCap / Math.max(1.0, pop);
            double totalWatts = buffer.getEnergyFire()[i] + buffer.getEnergyWind()[i] + buffer.getEnergySolar()[i]
                    + (buffer.getResourceCoal()[i] * 24.0) + (buffer.getResourceOil()[i] * 42.0);
            double powerW = totalWatts / Math.max(1.0, pop);
            double propulsionFactor = 1.0 + Math.log10(1.0 + Math.max(0.0, powerW - 300.0) / 150.0);
            double maritimeEquilibriumCap = 120.0 * propulsionFactor * propulsionFactor;
            double saturation = Math.clamp(1.0 - (capPerCapita / maritimeEquilibriumCap), 0.02, 1.0);
            acc.dResourceCapital += (float) (currentCap * DEFAULT_CAPITAL_BOOST_RATE * dtYears * saturation);
        }
    }
}


