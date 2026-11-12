/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
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
 * Hotelling Non-Renewable Resource Depletion & Scarcity Rent Engine (Hotelling, 1931).
 *
 * <p>Models the economic extraction dynamics and price path of exhaustible mineral and fossil reserves:</p>
 * <pre>
 *   [P(t) - MC(Q)] / [P_0 - MC(Q_0)] = exp(r · t)
 *   MC(Q) = MC_0 / (1 - Q_cumul / Q_total)^λ
 * </pre>
 * where:
 * <ul>
 *   <li><b>P(t) - MC</b>: Scarcity rent (net marginal profit) growing at the discount rate $r$.</li>
 *   <li><b>MC(Q)</b>: Marginal extraction cost escalating asymptotically as accessible high-grade reserves deplete.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class HotellingResourceDepletionEngine extends AbstractSimulationEngine {
    public HotellingResourceDepletionEngine() {
        super("HotellingResourceDepletionEngine", "HotellingResourceDepletionEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }
    private static final Logger logger = LoggerFactory.getLogger(HotellingResourceDepletionEngine.class);

    public static final double DISCOUNT_RATE_R = 0.03; // Real interest rate (3% per year)

    @Override
    public String getName() {
        return org.ether.society.i18n.I18n.getOrDefault(
                "engine.hotelling_depletion.name",
                "Hotelling Resource Depletion & Scarcity Rent");
    }

    @Override
    public String getDescription() {
        return org.ether.society.i18n.I18n.getOrDefault(
                "engine.hotelling_depletion.desc",
                "Models the depletion economics, Hill pyrometallurgical kiln activation, and escalating scarcity rents of non-renewable mineral assets.");
    }

    @Override
    public String getEquationsTooltip() {
        return org.ether.society.i18n.I18n.getOrDefault(
                "engine.hotelling_depletion.tooltip",
                """
                [Hotelling Exhaustible Resource Valuation & Chalcolithic Kiln Rule]
                • Kiln Hill Envelope:  E_kiln(k_pc) = k_pc^3 / (k_pc^3 + k_chalcolithic^3),  k_chalcolithic = 65 kg/cap
                • Extraction Flux:     R_extract = 0.02 · N · E_kiln(k_pc) · 2.5·tanh(ln(1 + k_pc/50)/3.5) · M_ore/(M_ore + 100)
                • Scarcity Rent:       MC(M_ore) = 1 + 4 · tanh((M_0 / M_rem)^0.5 / 5)
                Units: M_ore [tonnes ore], k_pc = K/N [kg/capita], r [yr^-1]
                Ref: H. Hotelling (1931, JPE); M. Radivojević et al. (2010, J. Archaeol. Sci.)
                """);
    }

    @Override
    public String getCategory() {
        return "Tier 2: Resource Economics";
    }

    /**
     * Minimum physical smelting &amp; furnace capital threshold $k_{\text{chalcolithic}} = 65.0\,\text{kg/capita}$
     * required for Chalcolithic/Bronze Age extractive metallurgy (Tylecote 1992, <i>A History of Metallurgy</i>;
     * Craddock 1995). Below this physical furnace stock, ore remains unmined in deep time ($\text{AGENTS.md}$ §8).
     */
    public static final double CHALCOLITHIC_FURNACE_CAPITAL_KG_PC = 65.0;

    /**
     * Sigmoid scale width $s_k = 20.0\,\text{kg/capita}$ for smooth $C^1$ onset of metallurgical ore extraction.
     */
    public static final double FURNACE_CAPITAL_SCALE_KG_PC = 20.0;

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double metalDeposit = cell.getResourceMetalOr(0.0);
            int pop = cell.getPopulationOr(0);
            if (pop <= 0 || metalDeposit <= 0.0) continue;

            double currentCapital = cell.getResourceCapitalOr(0.0);
            double kPerCapita = currentCapital / (double) pop;

            // Smooth C^infinity Hill pyrometallurgical kiln activation (n = 3, k_chalcolithic = 65 kg/cap):
            // Identically 0 at k_pc = 0 (~1e-4 in Paleolithic foraging, ~0.5 at 65 kg/cap, -> 1.0 in industrial regimes)
            // with zero if-then-else threshold cutoffs.
            double kRatioCubed = Math.pow(kPerCapita / CHALCOLITHIC_FURNACE_CAPITAL_KG_PC, 3.0);
            double smeltingActivation = kRatioCubed / (1.0 + kRatioCubed);

            // Industrial ore demand scales smoothly with physical machinery per capita (Ashby 2012, Materials and the Environment)
            double demandSaturation = 2.5 * Math.tanh(Math.log1p(kPerCapita / 50.0) / 3.5);
            double oreAvailability = metalDeposit / (metalDeposit + 100.0);
            double extraction = pop * 0.02 * smeltingActivation * demandSaturation * oreAvailability * deltaYears;
            extraction = Math.min(metalDeposit, extraction);

            cell.setResourceMetal(Math.max(0.0, metalDeposit - extraction));

            // Scarcity rent escalates infrastructure/capital creation as remaining reserves dwindle (Hotelling 1931)
            double remainingFraction = (metalDeposit - extraction + 1.0) / (metalDeposit + 101.0);
            double costMultiplier = 1.0 + 4.0 * Math.tanh(Math.pow(1.0 / remainingFraction, 0.5) / 5.0);

            // Resource scarcity rents flow into local extraction capital accumulation
            cell.setResourceCapital(currentCapital + (extraction * 2.0 * costMultiplier));
        }
    }
}
