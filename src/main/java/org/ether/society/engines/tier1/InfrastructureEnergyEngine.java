/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

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
public class InfrastructureEnergyEngine {
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureEnergyEngine.class);

    /**
     * Executes one infrastructure investment and friction reduction tick across cells.
     */
    public static void processInfrastructureEnergy(List<H3Cell> cells) {
        if (cells == null || cells.isEmpty()) return;

        int pavedCells = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double capital = cell.getResourceCapitalOr(0.0);
            double tech = cell.getTechnologyLevelOr(0.0);

            // Continuous transport friction reduction from transport infrastructure capital (Donaldson 2018; Allen & Arkolakis 2014):
            // Effective friction tau(K, T) is a closed-form state function of terrain, technology, and capital density
            if (tech >= 2.0 && capital > 200.0) {
                pavedCells++;
                double baseFriction = cell.getMovementFrictionOr(1.0);
                double capitalDensity = capital / Math.max(10.0, cell.getEffectiveSurfaceAreaKm2());
                double infrastructureDiscount = 0.10 + 0.90 / (1.0 + 0.05 * (tech - 1.8) * Math.log1p(capitalDensity));
                cell.setMovementFriction(Math.max(0.05, Math.min(baseFriction, baseFriction * infrastructureDiscount)));
            }
        }

        if (pavedCells > 0) {
            logger.info("Infrastructure Engine: Transport friction reduction active across {} paved infrastructure cells.", pavedCells);
        }
    }
}
