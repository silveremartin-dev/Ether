/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.database.H3CellPhysicsAndStateHelper;

import org.ether.society.engines.tier1.PhysicalEnergyGridEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Urban Allometry & Metabolic Scaling Engine (West, Bettencourt et al., 2007).
 *
 * <p>Models non-linear fractal power laws governing human urban agglomerations:</p>
 * <ul>
 *   <li><b>Super-linear scaling (Î² â‰ˆ 1.15)</b>: Socio-economic output (GDP, patents, innovation, crime)
 *       scales super-linearly with urban population: $Y \propto N^{1.15}$.</li>
 *   <li><b>Sub-linear scaling (Î³ â‰ˆ 0.85)</b>: Material infrastructure networks (road surface, power grid length, pipes)
 *       scale sub-linearly with urban population: $I \propto N^{0.85}$ (geometry of fractal supply networks).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class WestBettencourtAllometryEngine extends AbstractSimulationEngine {
    public WestBettencourtAllometryEngine() {
        super("WestBettencourtAllometryEngine", "WestBettencourtAllometryEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(WestBettencourtAllometryEngine.class);

    public static final double SUPERLINEAR_EXPONENT = 1.15; // Socioeconomic returns
    public static final double SUBLINEAR_EXPONENT = 0.85;   // Infrastructure efficiency

    @Override
    public String getName() {
        return "West-Bettencourt Urban Allometry";
    }

    @Override
    public String getDescription() {
        return "Models urban scaling laws: super-linear innovation/output (N^1.15) and sub-linear infrastructure network economy (N^0.85).";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [West-Bettencourt Urban Scaling Laws (PNAS 2007)]
               â€¢ Super-Linear Output:  Y_i = Y_0 Â· (N_i / N_ref)^1.15  (GDP, Patents, Wages)
               â€¢ Sub-Linear Network:  I_i = I_0 Â· (N_i / N_ref)^0.85  (Roads, Cables, Energy Grid)
               â€¢ Metabolic Pace:      v_pace âˆ N_i^0.15 (Pace of urban life & interactions)
               Units: N [hab], Y [$/an, brevets], I [km rÃ©seau, J/hab]
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Cliodynamics & Urban Scaling";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop < 100.0) return;

        double refPop = 1000.0;
        double normalizedPop = Math.max(1.0, pop / refPop);

        double superlinearFactor = Math.pow(normalizedPop, SUPERLINEAR_EXPONENT - 1.0);
        double currentCapital = buffer.getResourceCapital()[i];
        double kPerCapita = currentCapital / pop;
        double powerWatts = Math.max(85.0, ((buffer.getEnergyFire()[i] + buffer.getEnergyWind()[i]
                + buffer.getEnergySolar()[i] + buffer.getEnergyFoodConsumed()[i]) * 31.688) / pop);
        double kEnergized = Math.max(120.0, powerWatts / org.ether.society.database.H3CellPhysicsAndStateHelper.GARRETT_CAPITAL_MAINTENANCE_WATTS_PER_UNIT);
        double thermodynamicHeadroom = Math.max(0.0, 1.0 - (kPerCapita / kEnergized));
        acc.dResourceCapital += currentCapital * 0.02 * superlinearFactor * thermodynamicHeadroom * dtYears;

        double sublinearFactor = Math.pow(normalizedPop, SUBLINEAR_EXPONENT - 1.0);
        acc.dFluxPressure -= 0.01 * sublinearFactor * dtYears;
    }
}


