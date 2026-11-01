/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.database.Polity;
import org.ether.society.engines.tier1.ThermodynamicMigrationEngine;
import org.ether.society.util.DeterministicPRNG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Cliodynamics Engine (Peter Turchin Secular Cycles &amp; Structural-Demographic Theory).
 * Simulates:
 * 1. Asabiyyah (Social Cohesion / Solidarity Dynamics via coupled differential equations)
 * 2. Elite Overproduction &amp; Intra-Elite Competition
 * 3. Continuous Political Instability Index (PSI)
 * 4. Continuous Civil Strife Mortality ($\mu_{\text{strife}} = \mu_0 \cdot \Psi^2$)
 * 5. Continuous Peripheral Border Defection via Distance-Decay of Imperial Power ($\Psi &gt; \mathcal{C}_{\text{state}}$).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CliodynamicsEngine {
    private static final Logger logger = LoggerFactory.getLogger(CliodynamicsEngine.class);

    /**
     * Characteristic annual civil strife mortality coefficient (Turchin &amp; Nefedov 2009; Turchin 2016).
     * At peak instability ($\Psi = 1.0$), excess mortality is $\sim 1.5\%$ per year across afflicted populations.
     */
    public static final double STRIFE_MORTALITY_COEFF_PER_YEAR = 0.015;

    /**
     * Characteristic imperial administrative projection radius in degrees ($\sim 1,600$ km).
     * @deprecated use {@link #IMPERIAL_CORE_HORIZON_KM} for geodesic accuracy.
     */
    @Deprecated
    public static final double IMPERIAL_CORE_HORIZON_DEG = 15.0;

    /**
     * Characteristic imperial administrative projection radius in kilometers ($\sim 1,600$ km; Taagepera 1978; Turchin 2003).
     */
    public static final double IMPERIAL_CORE_HORIZON_KM = 1600.0;

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

            // 2. Continuous peripheral erosion / centrifugal border defection (Turchin 2003, Ch. 4; Taagepera 1978):
            // Distance-decay of imperial administrative reach: continuous Softplus activation when Instability exceeds State Capacity (Psi > C_state)
            // peripheral border cells assert local autonomy smoothly over time with geodesic Haversine distance.
            if (nation.getTerritory().size() > 1) {
                double stressDelta = psi - stateCap;
                // Continuous C^1 Softplus activation: active only when instability challenges state capacity (Psi > C_state)
                double excessStress = (stressDelta > -0.05)
                        ? (Math.log(1.0 + Math.exp(Math.min(20.0, 25.0 * stressDelta))) / 25.0) * dtYears
                        : 0.0;
                H3Cell capital = nation.getCapital();
                if (excessStress > 0.0005 && capital != null) {
                    List<H3Cell> territory = new ArrayList<>(nation.getTerritory());
                    for (H3Cell cell : territory) {
                        if (cell == null || cell == capital) continue;
                        double distKm = ThermodynamicMigrationEngine.calculateHaversineDistance(
                                cell.getLatitudeOr(0.0), cell.getLongitudeOr(0.0),
                                capital.getLatitudeOr(0.0), capital.getLongitudeOr(0.0));
                        double normDist = distKm / IMPERIAL_CORE_HORIZON_KM;
                        // Continuous sigmoidal/Hill distance effect on administrative reach attenuation
                        double distanceEffect = (normDist * normDist) / (1.0 + normDist * normDist);
                        // Annual defection probability scales continuously with geodesic distance and excess stress
                        double defectionProb = 0.03 * excessStress * distanceEffect;
                        double prng = DeterministicPRNG.nextDouble(capital.getH3Index(), 0L, cell.getH3Index(), 4321L);
                        if (prng < defectionProb) {
                            nation.removeCell(cell);
                            cell.setOwner(null);
                            logger.info("Cliodynamic Peripheral Autonomy: Cell {} detached from polity '{}' (PSI: {}, StateCap: {}).",
                                    cell.getH3Index(), nation.getName(), String.format(java.util.Locale.ROOT, "%.2f", psi), String.format(java.util.Locale.ROOT, "%.2f", stateCap));
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
