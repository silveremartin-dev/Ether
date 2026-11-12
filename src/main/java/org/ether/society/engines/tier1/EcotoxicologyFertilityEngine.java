/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ecotoxicology & Pollution Endocrine Stress Meta-Engine.
 * Models:
 * 1. <b>Chemical & Endocrine Toxicity Stress</b>: Industrial chemical pollution accumulation (microplastics, heavy metals).
 * 2. <b>Human Fertility Reduction Law</b>: Effective biological fertility drops exponentially under toxic environmental load:
 *    Fertility_eff = Fertility_base * e^(-k * Pollution).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class EcotoxicologyFertilityEngine extends AbstractSimulationEngine {
    public EcotoxicologyFertilityEngine() {
        super("EcotoxicologyFertilityEngine", "EcotoxicologyFertilityEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(EcotoxicologyFertilityEngine.class);

    /** Endocrine toxicity decay constant k */
    public static final double ENDOCRINE_TOXICITY_DECAY_K = 0.0002;

    /** Baseline human crude birth rate (annual, births per capita) */
    public static final double BASELINE_CRUDE_BIRTH_RATE = 0.035;

    /**
     * Calculates effective biological fertility factor based on chemical pollution load.
     *
     * @param pollutionLevel Pollution load in cell
     * @return Fertility multiplier between 0.35 and 1.0
     */
    public static double calculateEcotoxicFertilityFactor(double pollutionLevel) {
        if (pollutionLevel <= 0.0) return 1.0;
        return Math.max(0.35, Math.exp(-ENDOCRINE_TOXICITY_DECAY_K * pollutionLevel));
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double pollution = buffer.getPollutionLevel()[i];
        if (pollution > 100.0) {
            double fertilityFactor = calculateEcotoxicFertilityFactor(pollution);
            if (fertilityFactor < 0.999) {
                double biomassHuman = buffer.getBiomassHuman()[i];
                if (biomassHuman > 0.0) {
                    acc.dBiomassHuman -= (float) (biomassHuman * BASELINE_CRUDE_BIRTH_RATE * (1.0 - fertilityFactor) * dtYears);
                    acc.dFertility -= (float) (0.2 * (1.0 - fertilityFactor) * dtYears);
                }
            }
        }
    }
}
