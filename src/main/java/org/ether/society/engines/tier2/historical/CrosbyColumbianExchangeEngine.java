/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Alfred Crosby Columbian Exchange &amp; Virgin-Soil Epidemics Engine.
 *
 * <p>Models the macro-ecological and epidemiological dynamics of the Columbian Exchange
 * (Alfred W. Crosby 1972, <i>The Columbian Exchange: Biological and Cultural Consequences of 1492</i>;
 * Henry F. Dobyns 1983; Charles C. Mann 2005):</p>
 * <ul>
 *   <li><b>Draft Animal Deficit:</b> Megafauna overkill in pre-Columbian ecosystems leaves human populations
 *       dependent purely on human muscle labor, capping maximum mechanical work EROEI prior to European contact.</li>
 *   <li><b>Virgin-Soil Epidemic Collapse:</b> Introduction of Old World pathogens (smallpox, measles, influenza)
 *       into immunologically naive populations causes catastrophic $85\text{--}95\%$ demographic mortality.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see AmerindianEcosystemEngine
 */
public class CrosbyColumbianExchangeEngine extends AbstractSimulationEngine {
    public CrosbyColumbianExchangeEngine() {
        super("CrosbyColumbianExchangeEngine", "CrosbyColumbianExchangeEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(CrosbyColumbianExchangeEngine.class);

    private static boolean oldWorldContactTriggered = false;
    private static double epidemicMortalityRate = 0.90;

    @Override
    public String getName() {
        return "CrosbyColumbianExchangeEngine";
    }

    @Override
    public String getDescription() {
        return "Models Alfred Crosby (1972) Columbian Exchange: draft megafauna deficits and catastrophic virgin-soil epidemic collapse upon contact.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Crosby Columbian Exchange & Virgin-Soil Epidemics (Crosby 1972, Dobyns 1983)]
               • Draft Deficit:    If Livestock < 1.0 and k_pc < 1500 -> Work <= 40.0 (Muscle-only ceiling)
               • Virgin-Soil Shock: Upon Contact -> N(t+1) = N(t) · (1.0 - 0.90 · min(1.0, 0.10 · dt))
               Ref: A. W. Crosby (1972), "The Columbian Exchange", Greenwood Press.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Historical & Epidemiological Transitions";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double biomassHuman = buffer.getBiomassHuman()[i];
        double pop = biomassHuman / 0.06;
        if (pop <= 0.0) return;

        double livestock = buffer.getBiomassLivestock()[i];
        double kpc = buffer.getResourceCapital()[i] / Math.max(1.0, pop);

        if (livestock < 1.0 && kpc < 1500.0) {
            acc.dResourceCapital -= 0.005 * buffer.getResourceCapital()[i] * dtYears;
        }

        if (AmerindianEcosystemEngine.isOldWorldContactTriggered() && livestock < 5.0) {
            double survivalRate = Math.max(0.01, 1.0 - AmerindianEcosystemEngine.getEpidemicMortalityRate() * Math.min(1.0, 0.10 * dtYears));
            double newBiomass = Math.max(0.60, biomassHuman * survivalRate);
            acc.dBiomassHuman += (newBiomass - biomassHuman);
        }
    }

    public static boolean isOldWorldContactTriggered() {
        return AmerindianEcosystemEngine.isOldWorldContactTriggered();
    }

    public static void setOldWorldContactTriggered(boolean contact) {
        AmerindianEcosystemEngine.setOldWorldContactTriggered(contact);
    }

    public static void resetState() {
        AmerindianEcosystemEngine.resetState();
    }
}
