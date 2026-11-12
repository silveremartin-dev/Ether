/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.core.math.SmoothMath;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Barfield Nomadic Steppe Shadow Empire &amp; Extortion Symbiosis Engine.
 *
 * <p>Implements the foundational cliodynamic model of nomadic pastoral state formation
 * formulated by Thomas J. Barfield (1989, <i>The Perilous Frontier: Nomadic Empires and China</i>)
 * and Nicola Di Cosmo (2002):</p>
 * <ul>
 *   <li><b>Domestic Pastoral Surplus Deficit:</b> Pure nomadic pastoralism yields high mobility
 *       and equestrian martial power, but lacks the fixed taxable agrarian surplus necessary to
 *       finance an autonomous standing bureaucracy or imperial court autarkically.</li>
 *   <li><b>Outer-Directed Shadow Empire:</b> Steppe confederations (e.g. Xiongnu, Göktürks, Mongols)
 *       unify politically not to rule over agrarian land, but to mobilize terrorizing raiding strikes
 *       that force an adjacent wealthy sedentary agrarian empire into continuous tributary extortion
 *       (annual grain, silk, silver, and border trade subsidies).</li>
 *   <li><b>Synchronous Collapse Dynamics:</b> The extorted tribute is redistributed downward by the
 *       Steppe Khagan to buy the allegiance of subordinate tribal chieftains. When the agrarian macro-state
 *       collapses or ceases tribute, the nomadic shadow empire immediately suffers centrifugal fiscal
 *       starvation and fractures back into decentralized feuding clans.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see <a href="https://www.blackwellpublishing.com">Barfield, T. J. (1989), The Perilous Frontier</a>
 * @see FrontierAsabiyyahEngine
 * @see WarDiplomacyEngine
 */
public class BarfieldNomadicSteppeEmpireEngine extends AbstractSimulationEngine {
    public BarfieldNomadicSteppeEmpireEngine() {
        super("BarfieldNomadicSteppeEmpireEngine", "BarfieldNomadicSteppeEmpireEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(BarfieldNomadicSteppeEmpireEngine.class);

    /** Capital density threshold below which an equestrian population operates in the pastoral regime (kg/cap). */
    public static final double NOMADIC_PASTORAL_KPC_CEILING = 100.0;

    /** Minimum sedentary capital density required to support tributary extortion (kg/cap). */
    public static final double SEDENTARY_AGRARIAN_KPC_FLOOR = 150.0;

    /** Base annual extortion extraction rate from accessible sedentary wealth. */
    public static final double BASE_EXTORTION_RATE = 0.035;

    @Override
    public String getName() {
        return "BarfieldNomadicSteppeEmpireEngine";
    }

    @Override
    public String getDescription() {
        return "Models the Barfield (1989) outer-directed nomadic shadow empire: pastoral confederations sustain central unity by extorting agrarian tribute, fracturing when sedentary subsidies cease.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Barfield Nomadic Shadow Empire & Extortion Dynamics (Barfield 1989, Di Cosmo 2002)]
               • Steppe Pastoral Niche: Φ_pastoral = σ((Livestock / (Agri + 1)) - 1.5) · (1.0 - σ(k_pc - 100))
               • Extortion Capacity:   E_strike = N_nomad · (Livestock / Pop)^0.5
               • Extorted Tribute:     Tribute = E_strike · min(K_sedentary · 0.035, N_nomad · 25.0)
               • Chieftain Subsidy:    ΔK_nomad = Tribute · Redistribution_Efficiency - Maintenance_Decay
               • Centrifugal Collapse: If Tribute == 0 -> Centrifugal Clan Fracture (Work_cohesion decays at 5%/yr)
               Ref: T. J. Barfield (1989), "The Perilous Frontier: Nomadic Empires and China", Blackwell.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Geopolitical Cliodynamics";
    }

    private transient double totalSedentarySurplusCapital = 0.0;
    private transient double sedentaryVulnerability = 0.0;

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void prepareTick(WorldBuffer buffer, double dtYears) {
        totalSedentarySurplusCapital = 0.0;
        if (buffer == null || buffer.getBiomassHuman() == null) {
            sedentaryVulnerability = 0.0;
            return;
        }
        int n = buffer.getCapacity();
        float[] popArr = buffer.getBiomassHuman();
        float[] capArr = buffer.getResourceCapital();
        float[] agriArr = buffer.getBiomassAgriculture();
        for (int i = 0; i < n; i++) {
            double pop = popArr[i] / 0.06;
            if (pop <= 1.0) continue;
            double cap = capArr != null ? capArr[i] : 0.0;
            double kpc = cap / pop;
            double agri = agriArr != null ? agriArr[i] : 0.0;
            if (kpc >= SEDENTARY_AGRARIAN_KPC_FLOOR && agri > 50.0) {
                totalSedentarySurplusCapital += cap;
            }
        }
        sedentaryVulnerability = SmoothMath.sigmoid(totalSedentarySurplusCapital, 50000.0, 15000.0);
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop < 20.0) return;

        double cap = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 10.0;
        double kpc = cap / pop;
        double livestock = buffer.getBiomassLivestock() != null ? buffer.getBiomassLivestock()[i] : 0.0;
        double agri = buffer.getBiomassAgriculture() != null ? buffer.getBiomassAgriculture()[i] : 0.0;

        double livestockRatio = livestock / Math.max(1.0, agri + 1.0);
        double isPastoral = SmoothMath.sigmoid(livestockRatio, 1.2, 0.4)
                * (1.0 - SmoothMath.sigmoid(kpc, NOMADIC_PASTORAL_KPC_CEILING, 20.0));

        if (isPastoral > 0.05) {
            if (sedentaryVulnerability > 0.1 && totalSedentarySurplusCapital > 1000.0) {
                double perCapitaTribute = BASE_EXTORTION_RATE * 35.0 * sedentaryVulnerability * isPastoral;
                double extortedTribute = Math.min(pop * perCapitaTribute * dtYears, totalSedentarySurplusCapital * 0.02);
                acc.dResourceCapital += (float) extortedTribute;
                acc.dInstitutionalComplexity += (float) (0.015 * isPastoral * dtYears);
            } else {
                acc.dResourceCapital -= (float) (cap * 0.02 * isPastoral * dtYears);
                acc.dInstitutionalComplexity -= (float) (0.03 * isPastoral * dtYears);
            }
        }
    }
}
