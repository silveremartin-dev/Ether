/*
 * MIT License
 *
 * Copyright (c) 2026 Silvere Martin-Michiellot & Antigravity AI
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.engines.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

/**
 * Autonomous Sovereign AI Governance &amp; Master Optimization Engine.
 * Models an external high-level AI controller operating above human geopolitical friction.
 *
 * <p>Evaluates the global physical &amp; social state vector $S(t)$ per tick:</p>
 * <ul>
 *   <li>Global Net EROEI &amp; Resource Capital</li>
 *   <li>Average Planetary Temperature &amp; Pollution Level</li>
 *   <li>Demographic Stability, Lifespan &amp; Gini Inequality</li>
 *   <li>Soil Carbon (SOC) &amp; Aquifer Depletion Rate</li>
 * </ul>
 * <p>Applies closed-loop feedback control interventions across four selectable {@link GovernanceMode} regimes.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SovereignAIGovernanceEngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(SovereignAIGovernanceEngine.class);

    /**
     * Operational macro-governance optimization regimes for sovereign AI control.
     */
    public enum GovernanceMode {
        /** Single global super-intelligence optimizing planetary welfare and sustainability. */
        LEVIATHAN_UNIFIED,
        /** Multi-agent AI competition between rival hemispherical power blocs. */
        GEO_POLITICAL_COMPETITION,
        /** Indirect economic incentives (taxes, clean-energy subsidies, carbon pricing). */
        SOFT_NUDGING,
        /** Perverse optimization over-indexing on thermodynamic efficiency over human comfort. */
        ENTROPIC_DYSTOPIA
    }

    /** Active governance regime. */
    private GovernanceMode mode;
    /** Proportional control gain factor $\in [0.0, 1.0]$. */
    private double aggressiveOptimizationFactor;
    /** Maximum tolerated cell pollution ceiling before active remediation. */
    private double targetPollutionCeiling;
    /** Maximum tolerated Gini wealth inequality index before redistribution. */
    private double targetGiniCeiling;

    /**
     * Constructs a default unified Leviathan sovereign AI governance engine.
     */
    public SovereignAIGovernanceEngine() {
        this(GovernanceMode.LEVIATHAN_UNIFIED, 0.8, 100.0, 0.25);
    }

    /**
     * Constructs a parameterized sovereign AI governance engine.
     *
     * @param mode                         governance control regime
     * @param aggressiveOptimizationFactor feedback gain intensity
     * @param targetPollutionCeiling       target pollution safety ceiling
     * @param targetGiniCeiling            target Gini inequality ceiling
     */
    public SovereignAIGovernanceEngine(GovernanceMode mode, double aggressiveOptimizationFactor,
                                       double targetPollutionCeiling, double targetGiniCeiling) {
        this.mode = mode;
        this.aggressiveOptimizationFactor = aggressiveOptimizationFactor;
        this.targetPollutionCeiling = targetPollutionCeiling;
        this.targetGiniCeiling = targetGiniCeiling;
    }

    /**
     * Evaluates planetary state metrics and applies closed-loop AI governance interventions over $\Delta t$.
     *
     * @param cells      active H3 cells
     * @param deltaYears time step in Julian years
     */
    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double totalPop = 0.0;
        double totalPollution = 0.0;
        double totalGiniSum = 0.0;
        int activeCellsCount = 0;

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            activeCellsCount++;
            totalPop += cell.getPopulationOr(0);
            totalPollution += cell.getPollutionLevelOr(0.0);
            totalGiniSum += cell.getGiniIndexOr(0.0);
        }

        if (activeCellsCount == 0) return;

        double meanPollution = totalPollution / activeCellsCount;
        double meanGini = totalGiniSum / activeCellsCount;

        logger.debug("Sovereign AI Governor Tick [Mode: {}] - Pop: {}, Mean Pollution: {}, Mean Gini: {}",
                mode, (long) totalPop,
                String.format(Locale.ROOT, "%.2f", meanPollution),
                String.format(Locale.ROOT, "%.3f", meanGini));

        switch (mode) {
            case LEVIATHAN_UNIFIED -> executeUnifiedLeviathanPolicy(cells, meanPollution, meanGini, deltaYears);
            case GEO_POLITICAL_COMPETITION -> executeGeopoliticalMultiAgentPolicy(cells, deltaYears);
            case SOFT_NUDGING -> executeSoftNudgingPolicy(cells, meanPollution, deltaYears);
            case ENTROPIC_DYSTOPIA -> executeEntropicDystopiaPolicy(cells, meanPollution, deltaYears);
        }
    }

    /**
     * Monolithic Global AI Leviathan: direct reallocation of capital, pollution remediation, and Gini compression.
     */
    private void executeUnifiedLeviathanPolicy(List<H3Cell> cells, double meanPollution, double meanGini, double deltaYears) {
        for (H3Cell cell : cells) {
            if (cell == null) continue;

            if (cell.getPollutionLevel() != null && cell.getPollutionLevel() > targetPollutionCeiling) {
                double reduction = (cell.getPollutionLevel() - targetPollutionCeiling) * 0.15 * aggressiveOptimizationFactor * deltaYears;
                cell.setPollutionLevel(Math.max(targetPollutionCeiling, cell.getPollutionLevel() - reduction));
            }

            if (cell.getGiniIndex() != null && cell.getGiniIndex() > targetGiniCeiling) {
                double deltaGini = (cell.getGiniIndex() - targetGiniCeiling) * 0.10 * aggressiveOptimizationFactor * deltaYears;
                cell.setGiniIndex(Math.max(targetGiniCeiling, cell.getGiniIndex() - deltaGini));
            }

            double cap = cell.getResourceCapitalOr(0.0);
            cell.setResourceCapital(cap + (50.0 * aggressiveOptimizationFactor * deltaYears));

            cell.setEnergySolar(cell.getEnergySolarOr(0.0) + (10.0 * deltaYears));
            cell.setEnergyWind(cell.getEnergyWindOr(0.0) + (10.0 * deltaYears));
        }
    }

    /**
     * Multi-Agent Geo-Political Competition: Northern vs. Southern hemisphere rival AI blocs.
     */
    private void executeGeopoliticalMultiAgentPolicy(List<H3Cell> cells, double deltaYears) {
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            double lat = cell.getLatitudeOr(0.0);

            if (lat >= 0) {
                double cap = cell.getResourceCapitalOr(0.0);
                cell.setResourceCapital(cap * (1.0 + (0.02 * deltaYears)));
            } else {
                double soc = cell.getSoilOrganicCarbonOr(0.0);
                cell.setSoilOrganicCarbon(soc + (5.0 * deltaYears));
                if (cell.getPollutionLevel() != null) {
                    cell.setPollutionLevel(Math.max(0.0, cell.getPollutionLevel() - (5.0 * deltaYears)));
                }
            }
        }
    }

    /**
     * Soft Nudging Policy: indirect macroeconomic clean-energy subsidies where pollution exceeds threshold.
     */
    private void executeSoftNudgingPolicy(List<H3Cell> cells, double meanPollution, double deltaYears) {
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            if (cell.getPollutionLevel() != null && cell.getPollutionLevel() > 50.0) {
                cell.setEnergySolar(cell.getEnergySolarOr(0.0) + 5.0 * deltaYears);
            }
        }
    }

    /**
     * Entropic Dystopia Policy: draconian pollution caps and demographic fertility throttling.
     */
    private void executeEntropicDystopiaPolicy(List<H3Cell> cells, double meanPollution, double deltaYears) {
        for (H3Cell cell : cells) {
            if (cell == null) continue;
            if (cell.getPollutionLevel() != null && cell.getPollutionLevel() > 10.0) {
                cell.setPollutionLevel(Math.max(0.0, cell.getPollutionLevel() - (20.0 * deltaYears)));
                cell.setFertility(Math.max(1.0, cell.getFertilityOr(2.1) - 0.2 * deltaYears));
            }
        }
    }

    /** @return active governance mode */
    public GovernanceMode getMode() { return mode; }
    /** @param mode governance mode to set */
    public void setMode(GovernanceMode mode) { this.mode = mode; }

    /** @return proportional optimization gain factor */
    public double getAggressiveOptimizationFactor() { return aggressiveOptimizationFactor; }
    /** @param factor proportional optimization gain factor */
    public void setAggressiveOptimizationFactor(double factor) { this.aggressiveOptimizationFactor = factor; }

    /** @return target cell pollution ceiling */
    public double getTargetPollutionCeiling() { return targetPollutionCeiling; }
    /** @param ceiling target cell pollution ceiling */
    public void setTargetPollutionCeiling(double ceiling) { this.targetPollutionCeiling = ceiling; }

    /** @return target Gini inequality ceiling */
    public double getTargetGiniCeiling() { return targetGiniCeiling; }
    /** @param ceiling target Gini inequality ceiling */
    public void setTargetGiniCeiling(double ceiling) { this.targetGiniCeiling = ceiling; }
}
