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


import org.ether.society.engines.tier2.historical.CulturalMaterialismPureEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Marvin Harris Cultural Materialism Engine.
 *
 * <p>Implements Marvin Harris's cultural materialism paradigm (1979, <i>Cultural Materialism: The Struggle for a Science of Culture</i>):
 * societal evolution follows a deterministic primacy of infrastructure:
 * <b>Infrastructure (Techno-Environmental/Caloric Base) &rarr; Structure (Economy/Polity) &rarr; Superstructure (Ideology/Taboos)</b>.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see CulturalMaterialismPureEngine
 */
public class HarrisCulturalMaterialismEngine extends AbstractSimulationEngine {
    public HarrisCulturalMaterialismEngine() {
        super("HarrisCulturalMaterialismEngine", "HarrisCulturalMaterialismEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(HarrisCulturalMaterialismEngine.class);

    @Override
    public String getName() {
        return "HarrisCulturalMaterialismEngine";
    }

    @Override
    public String getDescription() {
        return "Models Marvin Harris (1979) cultural materialism: infrastructural caloric and ecological deficits force structural political collapse.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Marvin Harris Cultural Materialism (Harris 1979)]
               • Infrastructural Primacy: Infrastructure -> Structure -> Superstructure
               • Caloric Threshold:     If Food/Pop < 0.05 -> Structural Capital decay at 10%/year
               Ref: M. Harris (1979), "Cultural Materialism", Random House.
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
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 1.0) return;

        double food = buffer.getFoodResource() != null ? buffer.getFoodResource()[i] : 0.0;
        double foodPerCap = food / pop;
        double deficitPressure = 1.0 / (1.0 + Math.exp(40.0 * (foodPerCap - 0.05)));
        if (deficitPressure > 0.02) {
            double cap = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0;
            acc.dResourceCapital -= (float) (cap * 0.10 * deficitPressure * dtYears);
            acc.dInstitutionalComplexity -= (float) (0.05 * deficitPressure * dtYears);
        }
    }
}
