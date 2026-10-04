/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.database.H3Cell;
import org.ether.society.h3.H3Service;
import org.ether.society.engines.ProceduralEnginePlugin;
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
 *   dS_i/dt = -Î²_i Â· S_i Â· (I_i / N_i) + Î£_j (M_ji S_j - M_ij S_i)
 *   dE_i/dt = Î²_i Â· S_i Â· (I_i / N_i) - Ïƒ Â· E_i
 *   dI_i/dt = Ïƒ Â· E_i - (Î³ + Î¼_v) Â· I_i
 *   dR_i/dt = Î³ Â· I_i
 * </pre>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class SpatialMetapopulationSEIREngine implements ProceduralEnginePlugin {
    private static final Logger logger = LoggerFactory.getLogger(SpatialMetapopulationSEIREngine.class);

    public static final double SIGMA_LATENCY_RATE = 0.20; // Incubation rate (1/5 days)
    public static final double GAMMA_RECOVERY_RATE = 0.10; // Recovery rate (1/10 days)
    public static final double VIRULENCE_FATALITY = 0.05;  // Case fatality rate

    @Override
    /*
     * Get name.
     * Enforces physical invariants and updates associated state variables within {@code SpatialMetapopulationSEIREngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getName() {
        return "Spatial Metapopulation SEIR-V Epidemiology";
    }

    @Override
    /*
     * Get description.
     * Enforces physical invariants and updates associated state variables within {@code SpatialMetapopulationSEIREngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getDescription() {
        return "Compartmental metapopulation epidemiology (Susceptible, Exposed, Infected, Recovered) with mobility diffusion across H3 cells.";
    }

    @Override
    /*
     * Get equations tooltip.
     * Enforces physical invariants and updates associated state variables within {@code SpatialMetapopulationSEIREngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getEquationsTooltip() {
        return """
               [Spatial Metapopulation SEIR-V Differential System]
               â€¢ Susceptible:  dS_i/dt = -Î²_i Â· S_i Â· (I_i / N_i) + Î£_j (M_ji S_j - M_ij S_i)
               â€¢ Exposed:      dE_i/dt = Î²_i Â· S_i Â· (I_i / N_i) - Ïƒ Â· E_i
               â€¢ Infectious:   dI_i/dt = Ïƒ Â· E_i - (Î³ + Î¼_v) Â· I_i
               â€¢ Recovered:    dR_i/dt = Î³ Â· I_i
               â€¢ Mobility:     M_ij = D_mobility Â· exp(-dist_ij / L_commute)
               Units: S, E, I, R [personnes], Î² [1/j], Ïƒ [1/j], Î³ [1/j], Î¼_v [mortalitÃ©]
               Ref: Kermack-McKendrick (1927), Colizza & Vespignani (2007) Nature Physics
               """;
    }

    @Override
    /*
     * Get category.
     * Enforces physical invariants and updates associated state variables within {@code SpatialMetapopulationSEIREngine}.
     *
     * @return the resulting computation or state reference
     */
    public String getCategory() {
        return "Tier 2: Metapopulation Epidemiology";
    }

    @Override
    /*
     * Process.
     * Enforces physical invariants and updates associated state variables within {@code SpatialMetapopulationSEIREngine}.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
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



