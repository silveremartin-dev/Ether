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
import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
    public void process(List<H3Cell> cells, double deltaYears) {
        processStaplesDynamics(cells, deltaYears);
    }

    /**
     * Evaluates staples extraction and export-led lock-in across cells.
     *
     * @param cells      simulation cells
     * @param deltaYears time step duration in astronomical Julian years
     */
    public static void processStaplesDynamics(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 10) continue;

            double naturalBiomass = cell.getBiomassNaturalOr(0.0);
            double metal = cell.getResourceMetalOr(0.0);
            double timberAndFish = naturalBiomass * 0.40;

            // Per-capita raw staple abundance
            double stapleAbundance = (timberAndFish + metal) / (double) pop;
            double stapleIntensity = SmoothMath.sigmoid(stapleAbundance, 25.0, 8.0);

            if (stapleIntensity > 0.05) {
                // Directional export trade integration increases
                double trade = cell.getTradeNetworkOr(0.0);
                double exportGrowth = 0.04 * stapleIntensity * deltaYears;
                cell.setTradeNetwork(Math.min(100.0, trade + exportGrowth));

                // Natural staple stock is slowly harvested for export
                double harvestExtraction = 0.01 * stapleIntensity * naturalBiomass * deltaYears;
                cell.setBiomassNatural(Math.max(10.0, naturalBiomass - harvestExtraction));

                // Revenue fuels frontier capital accumulation
                double cap = cell.getResourceCapitalOr(0.0);
                cell.setResourceCapital(cap + (harvestExtraction * 1.5));
            }
        }
    }
}
