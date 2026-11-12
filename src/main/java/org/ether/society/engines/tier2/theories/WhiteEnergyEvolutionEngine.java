/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;


import org.ether.society.engines.tier2.historical.LeslieWhitePureEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Leslie White Energy and Cultural Evolution Engine.
 *
 * <p>Implements Leslie White's Law of Cultural Evolution (1943, 1949, <i>The Science of Culture</i>):
 * cultural development and structural complexity scale with the per-capita exergy harvested annually
 * multiplied by the physical efficiency of mechanical energy transducers (\(C = E \cdot T\)).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see LeslieWhitePureEngine
 */
public class WhiteEnergyEvolutionEngine extends AbstractSimulationEngine {
    public WhiteEnergyEvolutionEngine() {
        super("WhiteEnergyEvolutionEngine", "WhiteEnergyEvolutionEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(WhiteEnergyEvolutionEngine.class);

    @Override
    public String getName() {
        return "WhiteEnergyEvolutionEngine";
    }

    @Override
    public String getDescription() {
        return "Models Leslie White (1949) law of cultural evolution: societal complexity scales with per-capita energy capture and transducer efficiency.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Leslie White Law of Cultural Evolution (White 1949)]
               • Cultural Complexity: C = E · T
               • Energy Capture:      E = E_food + E_fire + E_wind + E_solar + E_livestock + E_fossil
               • Capital Deepening:   ΔK_active = f(E / Pop) · Efficiency
               Ref: L. A. White (1949), "The Science of Culture", Farrar, Straus and Giroux.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Anthropological Theories";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 0.0) return;

        double foodEnergy = Math.max(3.5 * pop, buffer.getEnergyFoodConsumed()[i]);
        double exosomaticEnergy = buffer.getEnergyFire()[i]
                + buffer.getEnergyWind()[i]
                + buffer.getEnergySolar()[i]
                + (buffer.getBiomassLivestock()[i] * 15.0)
                + (buffer.getResourceCoal()[i] * 24.0)
                + (buffer.getResourceOil()[i] * 42.0);
        double totalEnergy = foodEnergy + exosomaticEnergy;
        double energyPerCapita = totalEnergy / pop;

        acc.dResourceCapital += pop * (energyPerCapita / 50.0) * 0.005 * dtYears;
    }
}
