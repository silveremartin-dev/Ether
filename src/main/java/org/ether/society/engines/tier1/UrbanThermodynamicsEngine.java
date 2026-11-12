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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Urban Thermodynamics & Infrastructure Grid Engine.
 * Models urban heat island effect (T_uhi, Oke 1982), high-voltage grid transmission losses (I^2 * R Joule heating),
 * and localized aquifer drawdowns in dense population centers.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class UrbanThermodynamicsEngine extends AbstractSimulationEngine {
    public UrbanThermodynamicsEngine() {
        super("UrbanThermodynamicsEngine", "UrbanThermodynamicsEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(UrbanThermodynamicsEngine.class);

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || acc == null) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop > 50000.0) {
            double dtSafe = Math.max(0.0001, dtYears);
            double aquifer = buffer.getFreshwaterAquifer()[i];
            double nextAquifer = Math.max(0.0, aquifer - (pop * 0.001) * dtSafe);
            acc.dFreshwaterAquifer += (float) (nextAquifer - aquifer);
        }
    }

    @Override
    public void tick(WorldBuffer buf, double dtYears, double dtMonthly) {
        processUrbanThermodynamics(buf, dtYears);
    }

    /**
     * Process urban thermodynamics across DoD buffer.
     *
     * @param buffer WorldBuffer DoD state
     * @param deltaYears time step in years
     */
    public static void processUrbanThermodynamics(WorldBuffer buffer, double deltaYears) {
        if (buffer == null || buffer.getCapacity() == 0) return;

        double dtSafe = Math.max(0.0001, deltaYears);
        double thermalRelaxationFactor = 1.0 - Math.exp(-dtSafe / 0.5);

        float[] bioHuman = buffer.getBiomassHuman();
        float[] tempArr = buffer.getTemperature();
        float[] aquiferArr = buffer.getFreshwaterAquifer();
        int n = buffer.getCapacity();

        for (int i = 0; i < n; i++) {
            double pop = bioHuman[i] / 0.06;
            if (pop < 5000.0) continue;

            double uhiDeltaT = 0.5 * Math.log10(pop / 1000.0);
            tempArr[i] = (float) (tempArr[i] + uhiDeltaT * thermalRelaxationFactor);

            if (pop > 50000.0) {
                aquiferArr[i] = (float) Math.max(0.0, aquiferArr[i] - (pop * 0.001) * dtSafe);
            }
        }
    }
}
