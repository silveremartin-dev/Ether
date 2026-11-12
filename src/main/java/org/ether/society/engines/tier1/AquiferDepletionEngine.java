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
 * Groundwater Table Depletion & 2D Darcy Porous Aquifer Filtration Engine.
 * 
 * <h2>Fundamental Model Physics Equations</h2>
 * <ul>
 *   <li><b>Piezometric Head</b>:
 *       $$h_i = z_i + \frac{W_{\text{aqua}, i}}{S_y \cdot A_{\text{cell}}}$$
 *   </li>
 *   <li><b>Darcy Lateral Filtration Flux on H3 Graph</b>:
 *       $$\vec{q}_{ij} = -K_{\text{perm}} \cdot \frac{h_j - h_i}{d_{ij}}$$
 *   </li>
 *   <li><b>Recharge vs Irrigation Extraction</b>:
 *       $$\frac{\partial W_{\text{aqua}, i}}{\partial t} = 0.15 \cdot R_i - 0.8 \cdot N_i + \sum_{j \in \mathcal{N}(i)} q_{ji}$$
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class AquiferDepletionEngine extends AbstractSimulationEngine {
    public AquiferDepletionEngine() {
        super("AquiferDepletionEngine", "AquiferDepletionEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(AquiferDepletionEngine.class);

    /** Specific yield of unconfined porous aquifer S_y */
    public static final double SPECIFIC_YIELD_SY = 0.20;

    /** Baseline hydraulic conductivity K in m/s scaled to annual dt */
    public static final double HYDRAULIC_CONDUCTIVITY_K = 1e-4;

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double rawAquifer = buffer.getFreshwaterAquifer()[i];
        double aquifer = (rawAquifer <= 2.0) ? (400.0 + Math.max(0.0, rawAquifer) * 1000.0) : rawAquifer;
        double rain = buffer.getRainfall()[i];
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        double humanDensity = pop / 1770.49;

        double naturalRecharge = (rain * 0.15) * dtYears;
        double groundwaterFraction = 0.05 + 0.75 / (1.0 + Math.exp((rain - 550.0) / 120.0));
        double irrigationExtraction = (humanDensity * 0.80 * groundwaterFraction) * dtYears;

        double newAquifer = Math.clamp(aquifer + naturalRecharge - irrigationExtraction, 0.0, 2000.0);
        acc.dFreshwaterAquifer += (float) (newAquifer - rawAquifer);

        double aquiferStress = 1.0 / (1.0 + Math.exp((newAquifer - 80.0) / 25.0));
        double aridityStress = 1.0 / (1.0 + Math.exp((rain - 400.0) / 60.0));
        double irrigationStress = aquiferStress * aridityStress;
        if (irrigationStress > 0.05 && pop > 0.0) {
            double currentAgri = buffer.getBiomassAgriculture()[i];
            if (currentAgri > 0.0) {
                double newAgri = Math.max(0.0, currentAgri * Math.exp(-0.15 * irrigationStress * dtYears));
                acc.dBiomassAgriculture += (float) (newAgri - currentAgri);
            }
        }
    }
}



