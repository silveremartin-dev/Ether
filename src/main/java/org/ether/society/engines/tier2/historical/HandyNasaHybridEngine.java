/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * NASA HANDY Hybrid Model Variant B7.2 (Motesharrei, Rivas &amp; Kalnay, 2014 + Ether H3 Grid Integration).
 *
 * <p>Models coupled human-nature ecological regeneration and bilinear carrying-capacity depletion
 * directly across H3 spatial cells:</p>
 * $$\frac{dB_{\text{nat}, i}}{dt} = r \cdot B_{\text{nat}, i} \left(1 - \frac{B_{\text{nat}, i}}{B_{\max}}\right) - \delta \cdot \rho_i \cdot \frac{B_{\text{nat}, i}}{B_{\max}}$$
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class HandyNasaHybridEngine extends AbstractSimulationEngine {
    /**
     * Constructs the NASA HANDY hybrid cliodynamic engine.
     */
    public HandyNasaHybridEngine() {
        super("HandyNasaHybridEngine", "HandyNasaHybridEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(HandyNasaHybridEngine.class);

    /**
     * Indicates that this engine operates directly on Data-Oriented Design (DOD) cellular derivatives.
     *
     * @return {@code true} since cellular derivatives are supported
     */
    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    /**
     * Accumulates logistic natural biomass regeneration and bilinear anthropogenic depletion for cell {@code i}.
     *
     * @param buffer    contiguous DOD world state buffer
     * @param i         target H3 cell index
     * @param dtYears   integration time step in Julian years [$\text{yr}$]
     * @param dtMonthly integration time step in calendar months [$\text{mo}$]
     * @param acc       cellular derivative accumulator
     */
    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double pop = buffer.getHumanPopulation()[i];
        if (pop <= 0.0) return;

        double biomass = buffer.getBiomassNatural()[i];
        double density = pop / 100.0;
        double maxBiomass = 100.0;
        double regeneration = 0.02 * biomass * Math.max(0.0, 1.0 - biomass / maxBiomass);
        // Motesharrei et al. (2014) HANDY bilinear depletion term (delta * x * y) without artificial cap
        double depletion = 0.005 * density * (biomass / maxBiomass);
        acc.dBiomassNatural += (float) ((regeneration - depletion) * dtYears);
    }
}
