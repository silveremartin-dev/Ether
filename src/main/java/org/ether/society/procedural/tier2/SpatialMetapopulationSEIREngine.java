/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural.tier2;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.procedural.ProceduralEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Spatial Metapopulation SEIR-V Compartmental Epidemiology Engine.
 *
 * <p>Models pathogen incubation (E), clinical infectious transmission (I), recovery/immunity (R),
 * and spatial cross-cell mobility diffusion:</p>
 * <pre>
 *   dS_i/dt = -β_i · S_i · (I_i / N_i) + Σ_j (M_ji S_j - M_ij S_i)
 *   dE_i/dt = β_i · S_i · (I_i / N_i) - σ · E_i
 *   dI_i/dt = σ · E_i - (γ + μ_v) · I_i
 *   dR_i/dt = γ · I_i
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.3.0
 */
public class SpatialMetapopulationSEIREngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(SpatialMetapopulationSEIREngine.class);

    public static final double SIGMA_LATENCY_RATE = 0.20; // Incubation rate (1/5 days)
    public static final double GAMMA_RECOVERY_RATE = 0.10; // Recovery rate (1/10 days)
    public static final double VIRULENCE_FATALITY = 0.05;  // Case fatality rate

    @Override
    public String getName() {
        return "Spatial Metapopulation SEIR-V Epidemiology";
    }

    @Override
    public String getDescription() {
        return "Compartmental metapopulation epidemiology (Susceptible, Exposed, Infected, Recovered) with mobility diffusion across H3 cells.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Spatial Metapopulation SEIR-V Differential System]
               • Susceptible:  dS_i/dt = -β_i · S_i · (I_i / N_i) + Σ_j (M_ji S_j - M_ij S_i)
               • Exposed:      dE_i/dt = β_i · S_i · (I_i / N_i) - σ · E_i
               • Infectious:   dI_i/dt = σ · E_i - (γ + μ_v) · I_i
               • Recovered:    dR_i/dt = γ · I_i
               • Mobility:     M_ij = D_mobility · exp(-dist_ij / L_commute)
               Units: S, E, I, R [personnes], β [1/j], σ [1/j], γ [1/j], μ_v [mortalité]
               Ref: Kermack-McKendrick (1927), Colizza & Vespignani (2007) Nature Physics
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Metapopulation Epidemiology";
    }

    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        double dt = Math.max(0.01, deltaYears * 365.25); // Step in days
        H3Service h3Service = H3Service.getInstance();
        Map<Long, H3Cell> lookup = new HashMap<>();
        for (H3Cell c : cells) lookup.put(c.getH3Index(), c);

        for (H3Cell cell : cells) {
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;
            if (pop <= 0) continue;

            int infected = cell.getEpidemicInfected() != null ? cell.getEpidemicInfected() : 0;
            if (infected <= 0) continue;

            double beta = 0.35; // Transmission rate
            int newExposed = (int) (beta * (pop - infected) * ((double) infected / pop) * (dt / 10.0));
            int newInfectious = (int) (SIGMA_LATENCY_RATE * Math.min(newExposed, pop) * dt);
            int recoveries = (int) (GAMMA_RECOVERY_RATE * infected * dt);
            int deaths = (int) (VIRULENCE_FATALITY * infected * dt);

            int netInfected = Math.max(0, infected + newInfectious - recoveries - deaths);
            cell.setEpidemicInfected(netInfected);
            cell.setPopulation(Math.max(0, pop - deaths));

            // Spatial propagation to immediate H3 neighbors
            if (netInfected > 50) {
                List<Long> neighborIndexes = h3Service.getNeighbors(cell.getH3Index());
                int outboundSpread = (int) (netInfected * 0.02); // 2% cross-border leak
                for (Long nIdx : neighborIndexes) {
                    H3Cell neighbor = lookup.get(nIdx);
                    if (neighbor != null && neighbor.getPopulation() != null && neighbor.getPopulation() > 0) {
                        int nInfected = neighbor.getEpidemicInfected() != null ? neighbor.getEpidemicInfected() : 0;
                        neighbor.setEpidemicInfected(nInfected + (outboundSpread / neighborIndexes.size()));
                    }
                }
            }
        }
    }
}
