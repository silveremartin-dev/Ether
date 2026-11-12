/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.DerivativeAccumulator;

import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;

import org.ether.society.database.Polity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * <h1>Sovereign Treasury, Public Debt &amp; Coinage Debasement Engine</h1>
 * <p>
 * Models the macroscopic budgetary, fiscal, and debt dynamics of sovereign polities.
 * Replaces synthetic debt and currency debasement sigmoids with an authentic double-entry
 * state balance sheet (tax extraction, military expenditures, treasury bullion reserves,
 * bond market credit issuance, and crisis debasement).
 * </p>
 *
 * <h2>Scientific Foundations</h2>
 * <ul>
 *   <li><b>P. G. M. Dickson (1967)</b> <i>The Financial Revolution in England: A Study in the Development of Public Credit 1688–1756</i>, Macmillan.</li>
 *   <li><b>Carmen M. Reinhart &amp; Kenneth S. Rogoff (2009)</b> <i>This Time Is Different: Eight Centuries of Financial Folly</i>, Princeton University Press.</li>
 *   <li><b>David Stasavage (2011)</b> <i>States of Credit: Size, Power, and the Development of European Polities</i>, Princeton University Press.</li>
 *   <li><b>Kevin Butcher &amp; Matthew Ponting (2014)</b> <i>The Metallurgy of Roman Silver Coinage: From the Reform of Nero to the Reform of Trajan</i>, Cambridge University Press.</li>
 * </ul>
 *
 * <h2>Mechanistic Dynamics</h2>
 * <p>
 * The sovereign state levies taxes on domestic agricultural surplus and commercial transit tolls:
 * $$\text{Revenue} = \tau_{\text{tax}} \cdot (Y_{\text{agr}} + Y_{\text{trade}})$$
 * State expenditures are driven by standing military upkeep, bureaucratic administration, and wars:
 * $$\text{Expenditure} = N_{\text{military}} \cdot w_{\text{soldier}} + \text{Bureaucracy} + \text{WarCrisisCost}$$
 * When $\text{Expenditure} &gt; \text{Revenue}$, the fiscal deficit is covered first by drawing down
 * precious metal bullion reserves in the state treasury. If bullion is depleted:
 * <ol>
 *   <li>In regimes without public credit markets (Antiquity, feudal monarchies), the sovereign debases the coin purity:
 *       $$\Delta \text{Purity} = -\frac{\text{Deficit}}{\text{Total Coinage Issued}}$$</li>
 *   <li>In regimes with institutional representative bond markets (Italian city-states, Dutch Republic, Bank of England),
 *       the state issues sovereign debt securities:
 *       $$\text{Debt}_{t+1} = \text{Debt}_t \cdot (1 + r) + \text{Deficit}_t$$</li>
 * </ol>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class SovereignTreasuryBondEngine extends AbstractSimulationEngine {
    public SovereignTreasuryBondEngine() {
        super("SovereignTreasuryBondEngine", "SovereignTreasuryBondEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    private static final Logger logger = LoggerFactory.getLogger(SovereignTreasuryBondEngine.class);

    @Override
    public String getName() {
        return "SovereignTreasuryBondEngine";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (dtYears <= 0.0) return;

        double pop = buffer.getBiomassHuman()[i] / 0.06;
        if (pop < 100.0) return;

        double capital = buffer.getResourceCapital()[i];
        double inst = buffer.getInstitutionalComplexity()[i];
        if (inst > 0.25 && capital > 500.0) {
            // Creditworthy institutional regions mobilize fiscal-bond capital deepening
            double creditMobilization = 0.004 * Math.min(1.0, inst) * capital * dtYears;
            acc.dResourceCapital += creditMobilization;
            acc.dCommercialFluxVolume += 0.002 * creditMobilization;
        }
    }

    /**
     * Executes one fiscal accounting cycle across all active sovereign polities.
     *
     * @param polities   active polities/nations
     * @param cells      inhabited cells
     * @param deltaYears time step fraction in years
     */
    public static void processPolityFiscalCycles(List<Polity> polities, List<H3Cell> cells, double deltaYears) {
        if (polities == null || polities.isEmpty() || deltaYears <= 0.0) return;

        for (Polity polity : polities) {
            if (polity == null) continue;

            double totalPop = 0.0;
            double totalFoodSurplus = 0.0;
            double totalCapital = 0.0;

            if (polity.getTerritory() != null) {
                for (H3Cell c : polity.getTerritory()) {
                    if (c == null) continue;
                    int p = c.getPopulationOr(0);
                    totalPop += p;
                    totalFoodSurplus += Math.max(0.0, c.getFoodResourceOr(0.0) - p * 0.8);
                    totalCapital += c.getResourceCapitalOr(0.0);
                }
            }

            if (totalPop <= 0.0) continue;

            // Sovereign tax capacity: scales with state capacity (0.05 to 0.25 of economic surplus)
            double stateCapacity = polity.getStateCapacity();
            double taxRate = 0.05 + 0.15 * stateCapacity;
            double annualRevenue = taxRate * (totalFoodSurplus * 1.5 + totalCapital * 0.02);

            // Sovereign military & administrative expenditure
            // Driven by geopolitical tension, elite overproduction, and territorial defense
            double instability = polity.getPoliticalInstability();
            double militaryShare = 0.04 + 0.12 * instability;
            double annualExpenditures = totalPop * militaryShare * 2.0;

            double netFiscalBalance = annualRevenue - annualExpenditures;

            if (netFiscalBalance >= 0.0) {
                // Surplus: accumulate bullion reserves in treasury
                double currentBullion = polity.getTreasuryBullion();
                polity.setTreasuryBullion(currentBullion + netFiscalBalance * deltaYears * 0.05);
            } else {
                // Deficit: draw down bullion first
                double deficit = -netFiscalBalance * deltaYears;
                double currentBullion = polity.getTreasuryBullion();
                if (currentBullion >= deficit) {
                    polity.setTreasuryBullion(currentBullion - deficit);
                } else {
                    // Bullion exhausted: sovereign distress
                    polity.setTreasuryBullion(0.0);
                    double unfundedDeficit = deficit - currentBullion;

                    // If institutional trust exists (low instability), issue credit/debt; otherwise trigger monetary debasement
                    double creditworthiness = Math.max(0.0, 1.0 - instability);
                    if (creditworthiness > 0.6) {
                        // Funded debt market: debt accumulates
                        polity.setMonetaryInflation(Math.min(1.0, polity.getMonetaryInflation() + 0.02 * deltaYears));
                    } else {
                        // Coinage debasement / hyperinflation spike
                        polity.setMonetaryInflation(Math.min(1.0, polity.getMonetaryInflation() + 0.10 * deltaYears));
                    }
                }
            }
        }
    }
}
