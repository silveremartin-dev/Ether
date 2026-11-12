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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Multilevel Cultural Selection & Price Equation Engine (George R. Price, 1970).
 *
 * <p>Models the evolution of altruistic cooperation, prosocial norms, and military solidarity
 * through between-group vs within-group cultural selection pressures:</p>
 * <pre>
 *   Δz̄ = Cov(w_i, z_i) / w̄  +  E[w_i · Δz_i] / w̄
 * </pre>
 * where:
 * <ul>
 *   <li><b>Cov(w_i, z_i) / w̄</b>: Between-group selection term (prosocial/altruistic groups win inter-polity competition).</li>
 *   <li><b>E[w_i · Δz_i] / w̄</b>: Within-group individual selection term (free-riders / selfish rent-seekers erode civic solidarity).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PriceMultilevelSelectionEngine extends AbstractSimulationEngine {
    public PriceMultilevelSelectionEngine() {
        super("PriceMultilevelSelectionEngine", "PriceMultilevelSelectionEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(PriceMultilevelSelectionEngine.class);

    @Override
    public String getName() {
        return "Price Equation Multilevel Cultural Selection";
    }

    @Override
    public String getDescription() {
        return "Models the evolution of prosocial cooperation, military solidarity, and altruism via Price's multilevel cultural selection theorem.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Price Equation Multilevel Selection (Nature 1970)]
               • Full Equation:       Δz̄ = [ Cov(w_g, z_g) + E(w_g · Δz_g) ] / w̄
               • Between-Group Term:  Cov(w_g, z_g) > 0 (Cooperative tribes triumph in inter-polity warfare)
               • Within-Group Term:   E(w_g · Δz_g) < 0 (Free-riders exploit altruists internally)
               • Evolutionary Stable: Altruism spreads when Between-Group Covariance > Within-Group Defection
               Units: z [degré d'altruisme / civisme [0, 1]], w [fitness culturelle / survie démographique]
               Ref: G. R. Price (1970) "Selection and Covariance", Nature; Bowles & Gintis (2011) "A Cooperative Species"
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Evolutionary Cliodynamics";
    }

    private transient double meanWealth = 10.0;

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void prepareTick(WorldBuffer buffer, double dtYears) {
        meanWealth = 10.0;
        if (buffer == null || buffer.getResourceCapital() == null) return;
        float[] capArr = buffer.getResourceCapital();
        double sum = 0.0;
        int n = buffer.getCapacity();
        for (int i = 0; i < n; i++) sum += capArr[i];
        meanWealth = n > 0 ? Math.max(1.0, sum / n) : 10.0;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 1.0) return;

        double gini = buffer.getGiniIndex() != null ? buffer.getGiniIndex()[i] : 0.35;
        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 10.0;

        double prosociality = Math.clamp(1.0 - gini, 0.0, 1.0);
        double relativeCapital = (2.0 * capital) / (capital + meanWealth);
        double betweenGroupAdvantage = prosociality * 0.03 * relativeCapital;
        double withinGroupErosion = 0.02 * (1.0 / (1.0 + Math.exp(-12.0 * (gini - 0.45)))) * Math.max(0.0, gini - 0.35);

        double netAltruismShift = Math.clamp((betweenGroupAdvantage - withinGroupErosion) * dtYears, -0.50, 0.50);
        if (netAltruismShift > 0.0) {
            acc.dResourceCapital += (float) (capital * netAltruismShift * 0.10);
        }
        acc.dInstitutionalComplexity += (float) (netAltruismShift * 0.05);
    }
}
