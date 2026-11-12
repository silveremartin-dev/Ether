/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
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
 * Transport Infrastructure & Spatial Friction Attenuation Engine (Allen &amp; Arkolakis 2014; Donaldson 2018).
 * <p>
 * Models the continuous state-dependent reduction of geographic movement friction $\tau(K, T)$
 * derived from road, canal, and rail physical infrastructure capital density $k_{\text{infra}} = K / A_{\text{cell}}$,
 * replacing unbounded multiplicative degradation loops with closed-form thermodynamic state functions.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class InfrastructureEnergyEngine extends AbstractSimulationEngine {
    public InfrastructureEnergyEngine() {
        super("InfrastructureEnergyEngine", "InfrastructureEnergyEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureEnergyEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        double capital = buffer.getResourceCapital()[i];
        if (capital > 200.0) {
            double friction = buffer.getFluxPressure()[i];
            double targetFriction = Math.max(0.25, 1.0 / (1.0 + Math.log1p((capital - 200.0) / 500.0)));
            if (friction > targetFriction) {
                acc.dFluxPressure += (float) ((targetFriction - friction) * Math.min(1.0, dtYears * 0.2));
            }
        }
    }
}
