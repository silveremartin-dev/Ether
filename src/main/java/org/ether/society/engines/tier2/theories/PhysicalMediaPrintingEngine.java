/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
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
 * <h1>Physical Media Printing &amp; Information Transmission Engine</h1>
 * <p>
 * Models the material economics of textual transmission and knowledge accumulation across civilizational eras.
 * Replaces synthetic multi-stage literacy sigmoids with the physical production cost of information carriers
 * (parchment, rag paper, movable lead type, and high-speed rotary presses).
 * </p>
 *
 * <h2>Scientific Foundations</h2>
 * <ul>
 *   <li><b>Eltjo Buringh &amp; Jan Luiten van Zanden (2009)</b> <i>Charting the “Rise of the West”: Manuscripts and Printed Books in Europe, a Long-Term Perspective from the Sixth through Eighteenth Centuries</i>, J. Econ. Hist. 69(2): 409–445.</li>
 *   <li><b>Elizabeth L. Eisenstein (1979)</b> <i>The Printing Press as an Agent of Change</i>, Cambridge University Press.</li>
 *   <li><b>Jack Goody (1986)</b> <i>The Logic of Writing and the Organization of Society</i>, Cambridge University Press.</li>
 * </ul>
 *
 * <h2>Material Mechanics of Information Reproduction</h2>
 * <ul>
 *   <li><b>Manuscript Scriptoria (Parchment/Papyrus)</b>: Animal hide/reed input, manual labor $\sim 2\text{--}3$ pages/day per scribe.
 *       Extreme per-book cost ($\approx 1\text{--}3$ years of unskilled wages). Limits literacy to a scribal/clerical elite ($2\text{--}6\%$).</li>
 *   <li><b>Water-Powered Paper Mills + Movable Lead/Tin Type (Gutenberg)</b>: Rag pulp beating mills and lead casting capital ($k_{\text{pc}} \ge 400$).
 *       Reduces book reproduction costs by a factor of $100\times$, expanding vernacular literacy to $15\text{--}25\%$.</li>
 *   <li><b>Steam Rotary Cylinder Press &amp; Wood Pulp Paper (19th Century)</b>: Mechanical power ($P \ge 800\text{ W}$) and chemical pulping.
 *       Mass circulation newspapers, primary school textbooks, driving mass literacy to $> 80\%$.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class PhysicalMediaPrintingEngine extends AbstractSimulationEngine {
    public PhysicalMediaPrintingEngine() {
        super("PhysicalMediaPrintingEngine", "PhysicalMediaPrintingEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(PhysicalMediaPrintingEngine.class);

    @Override
    public String getName() {
        return "PhysicalMediaPrintingEngine";
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

        double capital = buffer.getResourceCapital() != null ? buffer.getResourceCapital()[i] : 0.0;
        double capitalPerCapita = capital / pop;
        double woodResource = buffer.getWoodResource() != null ? buffer.getWoodResource()[i] : 0.0;
        double metalStock = buffer.getMetalResource() != null ? buffer.getMetalResource()[i] : 0.0;

        double scribal = capitalPerCapita > 25.0 ? Math.min(1.0, (capitalPerCapita - 25.0) / 100.0) * 0.05 : 0.0;
        double print = (capitalPerCapita > 350.0 && metalStock > 0.1) ? 0.25 * Math.min(1.0, (capitalPerCapita - 350.0) / 800.0) : 0.0;
        double industrial = (capitalPerCapita > 1500.0 && woodResource > 0.5) ? 0.65 * Math.min(1.0, (capitalPerCapita - 1500.0) / 3000.0) : 0.0;

        double totalMediaCapacity = scribal + print + industrial;
        if (totalMediaCapacity > 0.01) {
            acc.dInstitutionalComplexity += (float) (0.01 * totalMediaCapacity * dtYears);
        }
    }
}
