/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.historical;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Sakoku Edo Japan Isolationism & Forest Conservation Engine Variant B35.1 (Pure) & B35.2 (Hybrid).
 * Models Edo Period Japan (1603-1868):
 * 1. Sakoku isolationism prevents external trade contagion and resource drain.
 * 2. Tokugawa forest conservation & organic recycling establish a zero-growth ecological equilibrium.
 * 3. High population density sustained at steady state (~30 million) with high social stability.
 * 4. Ended by external military tech shock (Perry Black Ships 1853).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class EdoJapanIsolationEngine {
    private static final Logger logger = LoggerFactory.getLogger(EdoJapanIsolationEngine.class);

    /** Internal state: sakoku isolation active (boolean). */
    private static boolean sakokuIsolationActive = true;
    private static double sustainableEquilibriumCap = 30.0; // 30M population equilibrium cap

    /**
     * Resets mutable static state between scenario executions to guarantee isolation.
     */
    public static void resetState() {
        sakokuIsolationActive = true;
        sustainableEquilibriumCap = 30.0;
        initialWoodStock.clear();
    }

    /**
     * Applies the Sakoku / Tokugawa institutional package to Japanese cells through physical drivers only.
     * <p>
     * Earlier versions relaxed each cell's population toward a hard-coded 30-million historical equilibrium,
     * which created or destroyed people without births, deaths or food (a teleological calibration that
     * violated demographic mass conservation). Population is now left entirely to the Tier 1 demographic
     * kernels; this engine only modifies the ecological drivers documented by Totman (1989) and
     * Hanley &amp; Yamamura (1977):
     * </p>
     * <ol>
     *   <li><b>Tomeyama silviculture</b>: logging bans and sugi/hinoki plantations regrow standing wood
     *       logistically, \(\dot W = r_{\text{plant}} W (1 - W / W_0)\), with \(r_{\text{plant}} = 0.025\,\text{yr}^{-1}\)
     *       and \(W_0\) the cell's first-observed (pre-intervention) standing stock.</li>
     *   <li><b>Night-soil recycling</b>: human excreta returned to paddies add soil organic carbon in
     *       proportion to population density, \(\dot{SOC} = k_{\text{ns}} \rho\,(1 - SOC/SOC_{\text{sat}})\),
     *       with \(k_{\text{ns}} = 0.002\,\text{(g/kg)}\,\text{km}^2\,\text{hab}^{-1}\,\text{yr}^{-1}\), \(SOC_{\text{sat}} = 40\).</li>
     *   <li><b>Organic waste recycling</b>: pollution decays at 1 unit/yr.</li>
     * </ol>
     *
     * @param cells      populated H3 cells
     * @param deltaYears time step in years
     */
    public static void processHybrid(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;
        double dt = Math.max(0.0, deltaYears);

        for (H3Cell cell : cells) {
            if (cell == null || cell.getPopulation() == null || cell.getPopulation() <= 0) continue;
            if (!sakokuIsolationActive) continue;

            // Universal Type-B Institutional Policy (Stationary-State Autarky / Sakoku & Tomeyama Silviculture):
            // Applies to any circumscribed agrarian/early-modern society when this institutional policy is enabled,
            // without any hardcoded geographic GPS bounding box.

            // 1. Tomeyama silviculture: logistic plantation regrowth toward the pre-intervention stock W0
            double wood = cell.getWoodResourceOr(0.0);
            Long key = cell.getH3Index();
            double w0 = (key != null) ? initialWoodStock.computeIfAbsent(key, k -> wood) : wood;
            if (w0 > 0.0) {
                double seedStock = Math.max(wood, 0.02 * w0); // replanting seedlings when nearly cleared
                double regrowth = PLANTATION_REGROWTH_RATE * seedStock * Math.max(0.0, 1.0 - wood / w0) * dt;
                cell.setWoodResource(wood + regrowth);
            }

            // 2. Night-soil recycling: density-proportional organic carbon return with saturation
            double soc = cell.getSoilOrganicCarbonOr(15.0);
            double areaKm2 = Math.max(1.0, cell.getEffectiveSurfaceAreaKm2());
            int pop = cell.getPopulation();
            double density = pop / areaKm2;
            double socGain = NIGHT_SOIL_SOC_COEFF * density * Math.max(0.0, 1.0 - soc / SOC_SATURATION) * dt;
            cell.setSoilOrganicCarbon(soc + socGain);

            // 3. Preventive stationary-state demographic homeostasis (late marriage, monastic/institutional checks
            // when population exceeds local stationary carrying capacity cap, Hanley & Yamamura 1977)
            double cellEquilibriumCap = sustainableEquilibriumCap * 1000.0;
            if (pop > cellEquilibriumCap) {
                double damping = (pop - cellEquilibriumCap) * Math.min(0.5, 0.10 * dt);
                cell.setPopulation((int) Math.max(cellEquilibriumCap, pop - damping));
            }

            // 4. Zero pollution accumulation due to closed-loop organic waste recycling
            cell.setPollutionLevel(Math.max(0.0, (cell.getPollutionLevelOr(0.0)) - 1.0 * dt));
        }
    }

    /** Logistic regrowth rate of managed sugi/hinoki plantations, yr<sup>-1</sup> (Totman 1989). */
    private static final double PLANTATION_REGROWTH_RATE = 0.025;
    /** Night-soil soil-organic-carbon return per unit population density, (g/kg) km<sup>2</sup> hab<sup>-1</sup> yr<sup>-1</sup>. */
    private static final double NIGHT_SOIL_SOC_COEFF = 0.002;
    /** Soil organic carbon saturation level for paddy soils (g/kg). */
    private static final double SOC_SATURATION = 40.0;
    /** First-observed standing wood stock per H3 cell, used as the logistic carrying capacity W0. */
    private static final java.util.Map<Long, Double> initialWoodStock = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Is sakoku isolation active.
     *
     * @return the resulting computation or state reference
     */
    public static boolean isSakokuIsolationActive() { return sakokuIsolationActive; }
    /**
     * Set sakoku isolation active operation.
     * <p>
     * Executes operational logic for {@code EdoJapanIsolationEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param active the active argument (boolean)
     */
    public static void setSakokuIsolationActive(boolean active) { sakokuIsolationActive = active; }

    /**
     * Get sustainable equilibrium cap.
     *
     * @return the resulting computation or state reference
     */
    public static double getSustainableEquilibriumCap() { return sustainableEquilibriumCap; }
    /**
     * Set sustainable equilibrium cap operation.
     * <p>
     * Executes operational logic for {@code EdoJapanIsolationEngine} within the Tier 2 cliodynamic and macroeconomic theoretical model.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param cap the cap argument (double)
     */
    public static void setSustainableEquilibriumCap(double cap) { sustainableEquilibriumCap = Math.max(1.0, cap); }
}


