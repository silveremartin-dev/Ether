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

import org.ether.society.core.math.SmoothMath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Harold Innis Staples Thesis &amp; Export-Led Frontier Dependency Engine.
 *
 * <p>Implements Harold Innis's staples thesis of economic history
 * (1930, <i>The Fur Trade in Canada: An Introduction to Canadian Economic History</i>;
 * 1940, <i>The Cod Fisheries</i>; W. A. Mackintosh 1923):</p>
 * <ul>
 *   <li><b>Staples Export Trap:</b> Peripheral frontier regions develop around the extraction and export
 *       of raw, unprocessed natural "staples" (fish, fur, timber, grain, minerals) to an imperial metropolitan core.</li>
 *   <li><b>Linear Export Infrastructure Bias:</b> Transport investments (canals, railways, portage rivers)
 *       radiate unidirectionally toward export terminals rather than fostering circular domestic trade integration.</li>
 *   <li><b>Metropolitan Demand Vulnerability:</b> When the resource is depleted or metropolitan fashion/demand shifts,
 *       the frontier economy experiences severe debt overhang and capital obsolescence (the "staples trap").</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see <a href="https://www.utorontopress.com">Innis, H. A. (1930), The Fur Trade in Canada</a>
 * @see KrugmanCorePeripheryEngine
 * @see CordenNearyDutchDiseaseEngine
 */
public class InnisStaplesThesisEngine extends AbstractSimulationEngine {
    public InnisStaplesThesisEngine() {
        super("InnisStaplesThesisEngine", "InnisStaplesThesisEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(InnisStaplesThesisEngine.class);

    @Override
    public String getName() {
        return "InnisStaplesThesisEngine";
    }

    @Override
    public String getDescription() {
        return "Models Harold Innis (1930) staples thesis: export-led frontier exploitation creates unidirectional transport bias and vulnerability to external demand shifts.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Harold Innis Staples Thesis (Innis 1930, Mackintosh 1923)]
               • Staple Index:        S_ratio = (Biomass_Nat + Metal + Coal) / (Pop · 10 + 1)
               • Linear Port Bias:    ΔTrade_Export = +0.05 · S_ratio · dt
               • Domestic Lock-in:    If S_ratio > 2.0 -> Local diversification drag ΔAgri_div = -0.02 · dt
               • Staples Trap:        Depletion without secondary manufacturing causes infrastructure debt overhang
               Ref: H. A. Innis (1930), "The Fur Trade in Canada", University of Toronto Press.
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Institutional Macroeconomics";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getBiomassHuman() == null || dtYears <= 0.0) return;
        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop <= 10.0) return;

        double naturalBiomass = buffer.getBiomassNatural() != null ? buffer.getBiomassNatural()[i] : 0.0;
        double metal = buffer.getMetalResource() != null ? buffer.getMetalResource()[i] : 0.0;
        double timberAndFish = naturalBiomass * 0.40;

        double stapleAbundance = (timberAndFish + metal) / pop;
        double stapleIntensity = SmoothMath.sigmoid(stapleAbundance, 25.0, 8.0);

        if (stapleIntensity > 0.05) {
            acc.dCommercialFluxVolume += (float) (0.04 * stapleIntensity * dtYears);
            double harvestExtraction = 0.01 * stapleIntensity * naturalBiomass * dtYears;
            acc.dBiomassNatural -= (float) harvestExtraction;
            acc.dResourceCapital += (float) (harvestExtraction * 1.5);
        }
    }
}
