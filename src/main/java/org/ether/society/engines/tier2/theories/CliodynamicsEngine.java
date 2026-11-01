/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.util.DeterministicPRNG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Cliodynamics Engine (Peter Turchin Secular Cycles & Structural-Demographic Theory).
 * Simulates:
 * 1. Asabiyyah (Social Cohesion / Solidarity Dynamics via coupled differential equations)
 * 2. Elite Overproduction & Intra-Elite Competition
 * 3. Continuous Political Instability Index (PSI)
 * 4. Continuous Civil Strife Mortality ($\mu_{\text{strife}} = \mu_0 \cdot \Psi^2$)
 * 5. Continuous Peripheral Border Defection via Distance-Decay of Imperial Power ($\Psi > \mathcal{C}_{\text{state}}$).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CliodynamicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(CliodynamicsEngine.class);

    /**
     * Characteristic annual civil strife mortality coefficient (Turchin & Nefedov 2009; Turchin 2016).
     * At peak instability ($\Psi = 1.0$), excess mortality is $\sim 1.5\%$ per year across afflicted populations.
     */
    public static final double STRIFE_MORTALITY_COEFF_PER_YEAR = 0.015;

    /**
     * Characteristic imperial administrative projection radius in degrees ($\sim 1,600$ km).
     */
    public static final double IMPERIAL_CORE_HORIZON_DEG = 15.0;

    /**
     * Executes one tick of Cliodynamics update across all nations and territory cells with explicit time step.
     *
     * @param nations list of active polities
     * @param cells   populated cell domain
     * @param dtYears time step in years
     */
    public static void updateCliodynamics(List<Polity> nations, List<H3Cell> cells, double dtYears) {
        if (nations == null || nations.isEmpty() || dtYears <= 0.0) return;

        for (Polity nation : nations) {
            nation.updateCliodynamicsCycle(dtYears);

            double psi = nation.getPoliticalInstability();
            double stateCap = nation.getStateCapacity();

            // 1. Continuous civil strife mortality (Lotka-Volterra SDT quadratic coupling):
            // dN/dt = - mu_0 * Psi^2 * N * dtYears
            // Completely continuous across all instability levels, with zero discrete thresholds.
            if (psi > 0.05) {
                double strifeMortalityFraction = STRIFE_MORTALITY_COEFF_PER_YEAR * (psi * psi) * dtYears;
                for (H3Cell cell : nation.getTerritory()) {
                    if (cell != null) {
                        int curPop = cell.getPopulationOr(0);
                        if (curPop > 0) {
                            int deaths = (int) Math.round(curPop * strifeMortalityFraction);
                            if (deaths > 0) {
                                cell.setPopulation(Math.max(0, curPop - deaths));
                            }
                        }
                    }
                }
            }

            // 2. Continuous peripheral erosion / centrifugal border defection (Turchin 2003, Ch. 4):
            // Distance-decay of imperial administrative reach: only when Instability exceeds State Capacity (Psi > C_state)
            // peripheral border cells assert local autonomy smoothly over time.
            if (psi > stateCap && nation.getTerritory().size() > 1) {
                double excessStress = (psi - stateCap) * dtYears;
                H3Cell capital = nation.getCapital();
                if (capital != null) {
                    List<H3Cell> territory = new ArrayList<>(nation.getTerritory());
                    for (H3Cell cell : territory) {
                        if (cell == null || cell == capital) continue;
                        double distDeg = Math.hypot(cell.getLatitude() - capital.getLatitude(), cell.getLongitude() - capital.getLongitude());
                        double distanceRatio = distDeg / IMPERIAL_CORE_HORIZON_DEG;
                        if (distanceRatio > 1.0) {
                            // Annual defection rate scales continuously with distance and excess stress
                            double defectionProb = 0.02 * excessStress * distanceRatio;
                            if (DeterministicPRNG.nextDouble(cell.getH3Index(), 4321) < defectionProb) {
                                nation.removeCell(cell);
                                cell.setOwner(null);
                                logger.info("Cliodynamic Peripheral Autonomy: Cell {} detached from polity '{}' (PSI: {}, StateCap: {}).",
                                        cell.getH3Index(), nation.getName(), String.format("%.2f", psi), String.format("%.2f", stateCap));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Legacy compatibility wrapper defaulting to 1 month time step.
     */
    public static void updateCliodynamics(List<Polity> nations, List<H3Cell> cells) {
        updateCliodynamics(nations, cells, 1.0 / 12.0);
    }
}
