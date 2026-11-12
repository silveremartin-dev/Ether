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
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Bio-Molecular Epidemiology & Immunoglobulin Synthesis Engine.
 * Replaces abstract health scores with bio-molecular energy allocation and endogenous pathogen dynamics:
 * 1. <b>Metabolic Energy Deficiency & Immune Arrest</b>: When ingested caloric energy falls below basal requirements,
 *    protein immunoglobulin synthesis is suppressed to preserve vital organ metabolism.
 * 2. <b>Endogenous Zoonotic Spillover</b>: Spontaneous pathogen emergence coupling domestic livestock biomass density,
 *    human urban density, and host nutritional immunocompetence:
 *    <pre>
 *      P_spillover = 1.0 - exp( -κ · (ρ_domestic · ρ_urban) / (1.0 + NutritionRatio) · Δt )
 *    </pre>
 * 3. <b>Metapopulation Pathogen Progression</b>: Local transmission, case fatality, and recovery coupled with nutritional state.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class BioMolecularEpidemiologyEngine extends AbstractSimulationEngine {
    public BioMolecularEpidemiologyEngine() {
        super("BioMolecularEpidemiologyEngine", "BioMolecularEpidemiologyEngine", EngineTier.TIER_1_BIOPHYSICAL);
    }

    private static final Logger logger = LoggerFactory.getLogger(BioMolecularEpidemiologyEngine.class);


    /**
     * Mammalian cross-species viral/bacterial phylogenetic transmission coefficient (Plowright et al. 2017; Wolfe et al. 2007).
     * Reflects the molecular compatibility barrier between domestic mammals (ungulates, suids) and human hosts.
     */
    public static final double MAMMALIAN_PHYLOGENETIC_SPILLOVER_COEFF = 0.25;


    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(org.ether.society.core.dod.WorldBuffer buffer, int i, double dtYears, double dtMonthly, org.ether.society.core.engine.DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;
        double biomassHuman = buffer.getBiomassHuman()[i];
        double pop = biomassHuman / 0.06;
        if (pop <= 0.0) return;
        double dtMonthlyRatio = Math.max(0.001, dtYears / (1.0 / 12.0));
        double foodRequired = pop * 1.0 * dtMonthlyRatio;
        double totalFoodAvailable = buffer.getFoodResource()[i] + buffer.getBiomassAgriculture()[i] * 0.70 * dtYears + buffer.getBiomassNatural()[i] * 0.05 * dtYears;
        double immunoglobulinSynthesisRate = Math.clamp(totalFoodAvailable / Math.max(0.1, foodRequired), 0.05, 1.0);
        double curLife = buffer.getLifespan()[i] > 0.0f ? buffer.getLifespan()[i] : 40.0;
        double capPerCapita = buffer.getResourceCapital()[i] / Math.max(1.0, pop);
        double waterSecurity = Math.clamp((buffer.getFreshwaterAquifer()[i] + buffer.getWaterResource()[i]) / Math.max(10.0, pop * 0.05), 0.1, 2.0);
        double devIndex = Math.log1p(capPerCapita / 50.0) * Math.min(1.5, waterSecurity);
        double targetBaselineLife = 32.0 + 50.0 / (1.0 + Math.exp(-0.85 * (devIndex - 3.0)));
        double nutritionDelta = (immunoglobulinSynthesisRate - 0.55) * 1.5 * dtMonthlyRatio;
        double baselineRestoration = 0.08 * (targetBaselineLife - curLife) * dtMonthlyRatio;
        double newLife = Math.clamp(curLife + nutritionDelta + baselineRestoration, 15.0, targetBaselineLife);
        acc.dLifespan += (float) (newLife - curLife);
    }
}
