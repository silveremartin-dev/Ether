/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.database.H3Cell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
 * @version 1.0.0-beta.2
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
    public void process(List<H3Cell> cells, double deltaYears) {
        processMediaProduction(cells, deltaYears);
    }

    /**
     * Evaluates physical book/media throughput per capita and drives endogenous educational capacity.
     *
     * @param cells      inhabited cells
     * @param deltaYears time step fraction in years
     */
    public static void processMediaProduction(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty() || deltaYears <= 0.0) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            double capital = cell.getResourceCapitalOr(0.0);
            double capitalPerCapita = capital / pop;
            double woodResource = cell.getWoodResourceOr(0.0);
            double metalStock = cell.getResourceMetalOr(0.0);

            // 1. Scribal Baseline: parchment / manuscript production (requires basic settled capital k_pc >= 25 kg/cap)
            double scribalMediaCapacity = 0.0;
            if (capitalPerCapita > 25.0) {
                scribalMediaCapacity = Math.min(1.0, (capitalPerCapita - 25.0) / 100.0) * 0.05; // ~5% literacy cap
            }

            // 2. Movable-Type Print Revolution: requires lead/tin metallurgy (metalStock > 0.1) and capital k_pc >= 350
            double printMediaCapacity = 0.0;
            if (capitalPerCapita > 350.0 && metalStock > 0.1) {
                double pressAdoption = Math.min(1.0, (capitalPerCapita - 350.0) / 800.0);
                printMediaCapacity = 0.25 * pressAdoption; // lifts literacy capacity to ~30%
            }

            // 3. Mass Industrial Paper & Steam Printing: requires wood pulp and industrial capital k_pc >= 1500
            double massIndustrialMediaCapacity = 0.0;
            if (capitalPerCapita > 1500.0 && woodResource > 0.5) {
                double industrialAdoption = Math.min(1.0, (capitalPerCapita - 1500.0) / 3000.0);
                massIndustrialMediaCapacity = 0.65 * industrialAdoption; // lifts literacy to ~95%
            }

            double totalMediaCapacity = scribalMediaCapacity + printMediaCapacity + massIndustrialMediaCapacity;
            double targetLiteracyRate = Math.clamp(totalMediaCapacity * 100.0, 0.0, 99.0);

            // Store emergent knowledge transmission capacity onto transit/toll revenue or trade buffer
            cell.setTransitTollRevenue(Math.max(cell.getTransitTollRevenueOr(0.0), targetLiteracyRate));
        }
    }
}
