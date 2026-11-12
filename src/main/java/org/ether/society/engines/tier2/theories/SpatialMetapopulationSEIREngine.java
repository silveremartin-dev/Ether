/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.EngineTier;
import org.ether.society.database.H3Cell;
import org.ether.society.database.H3CellPhysicsAndStateHelper;
import org.ether.society.h3.H3Service;

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
 * @version 1.0.0-beta.2
 */
public class SpatialMetapopulationSEIREngine extends AbstractSimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(SpatialMetapopulationSEIREngine.class);

    public static final double SIGMA_LATENCY_RATE = 0.20; // Incubation rate (1/5 days)
    public static final double GAMMA_RECOVERY_RATE = 0.10; // Recovery rate (1/10 days)
    public static final double VIRULENCE_FATALITY = 0.05;  // Case fatality rate

    private float[] infectedArray;
    private float[] recoveredArray;
    private int cachedCapacity = 0;

    public SpatialMetapopulationSEIREngine() {
        super("SpatialMetapopulationSEIREngine", "Spatial Metapopulation SEIR-V Epidemiology", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public void reset() {
        if (infectedArray != null) java.util.Arrays.fill(infectedArray, 0.0f);
        if (recoveredArray != null) java.util.Arrays.fill(recoveredArray, 0.0f);
    }

    /**
     * Seeds initial infectious cases in a specific buffer cell slot.
     *
     * @param cellIndex target cell buffer index
     * @param count     number of initial active infectious cases
     */
    public void seedInfection(int cellIndex, float count) {
        if (infectedArray != null && cellIndex >= 0 && cellIndex < infectedArray.length) {
            infectedArray[cellIndex] = Math.max(infectedArray[cellIndex], count);
        }
    }

    @Override
    public void tick(WorldBuffer buffer, double dtYears, double dtMonthly) {
        if (buffer == null) return;
        int capacity = buffer.getCapacity();
        if (capacity <= 0) return;

        if (infectedArray == null || cachedCapacity != capacity) {
            infectedArray = new float[capacity];
            recoveredArray = new float[capacity];
            cachedCapacity = capacity;
        }

        double dtDays = Math.max(0.01, dtYears * 365.25);
        float[] popArr = buffer.getBiomassHuman();
        int[][] neighbors = buffer.getNeighborIndexes();

        float[] nextInfected = infectedArray.clone();
        float[] nextRecovered = recoveredArray.clone();

        for (int i = 0; i < capacity; i++) {
            float pop = popArr[i];
            if (pop <= 0.0f) continue;
            float inf = infectedArray[i];
            if (inf <= 0.0f) continue;

            inf = Math.min(pop, inf);
            float rec = Math.min(pop, recoveredArray[i]);
            float sus = Math.max(0.0f, pop - inf - rec);

            double beta = 0.35;
            double lambda = beta * ((double) inf / pop);
            double probExposure = 1.0 - Math.exp(-lambda * (dtDays / 10.0));
            float newExposed = (float) Math.clamp(sus * probExposure, 0.0, sus);

            double probInfectious = 1.0 - Math.exp(-SIGMA_LATENCY_RATE * dtDays);
            float newInfectious = (float) Math.clamp(newExposed * probInfectious, 0.0, newExposed);

            double totalRemovalRate = GAMMA_RECOVERY_RATE + VIRULENCE_FATALITY;
            double probRemoval = 1.0 - Math.exp(-totalRemovalRate * dtDays);
            float totalRemovals = (float) Math.clamp(inf * probRemoval, 0.0, inf);

            float recoveries = (float) (totalRemovals * (GAMMA_RECOVERY_RATE / totalRemovalRate));
            float deaths = totalRemovals - recoveries;

            float netInfected = (float) Math.clamp(inf + newInfectious - totalRemovals, 0.0, pop);
            nextInfected[i] = netInfected;
            nextRecovered[i] = Math.min(Math.max(0.0f, pop - deaths), rec + recoveries);

            // Directly remove mortality from somatic human population
            popArr[i] = Math.max(0.0f, pop - deaths);

            // Spatial propagation to adjacent topological neighbors
            if (netInfected > 50.0f && neighbors != null && i < neighbors.length) {
                int[] nSlots = neighbors[i];
                if (nSlots != null && nSlots.length > 0) {
                    float outboundSpread = netInfected * 0.02f;
                    float perNeighborSpread = outboundSpread / nSlots.length;
                    if (perNeighborSpread > 0.001f) {
                        for (int nIdx : nSlots) {
                            if (nIdx >= 0 && nIdx < capacity && popArr[nIdx] > 0.0f) {
                                float nPop = popArr[nIdx];
                                float nInf = nextInfected[nIdx];
                                float nRec = nextRecovered[nIdx];
                                float nSus = Math.max(0.0f, nPop - nInf - nRec);
                                float actualSpread = Math.min(nSus, perNeighborSpread);
                                if (actualSpread > 0.0f) {
                                    nextInfected[nIdx] = Math.min(nPop, nInf + actualSpread);
                                }
                            }
                        }
                    }
                }
            }
        }

        System.arraycopy(nextInfected, 0, infectedArray, 0, capacity);
        System.arraycopy(nextRecovered, 0, recoveredArray, 0, capacity);
    }

    /**
     * Get name.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getName() {
        return "Spatial Metapopulation SEIR-V Epidemiology";
    }

    /**
     * Get description.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getDescription() {
        return "Compartmental metapopulation epidemiology (Susceptible, Exposed, Infected, Recovered) with mobility diffusion across H3 cells.";
    }

    /**
     * Get equations tooltip.
     *
     * @return the resulting computation or state reference
     */
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

    /**
     * Get category.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String getCategory() {
        return "Tier 2: Metapopulation Epidemiology";
    }

    /**
     * Process.
     *
     * @param cells the cells parameter (List&lt;H3Cell&gt;)
     * @param deltaYears the delta years parameter (double)
     */
    @Override
    public void process(List<H3Cell> cells, double deltaYears) {
        // Step 1: Read institutional, demographic, and economic state tensors
        // Step 2: Evaluate non-linear cliodynamic feedback equations and threshold conditions
        // Step 3: Apply state transitions and update local cell attributes
        if (cells == null || cells.isEmpty()) return;

        double dt = Math.max(0.01, deltaYears * 365.25); // Step in days
        H3Service h3Service = H3Service.getInstance();
        Map<Long, H3Cell> lookup = new HashMap<>();
        for (H3Cell c : cells) {
            if (c != null && c.getH3Index() != null) {
                lookup.put(c.getH3Index(), c);
            }
        }

        for (H3Cell cell : cells) {
            if (cell == null) continue;
            int pop = cell.getPopulationOr(0);
            if (pop <= 0) continue;

            int infected = cell.getEpidemicInfectedOr(0);
            if (infected <= 0) continue;
            infected = Math.min(pop, infected);

            double beta = 0.35; // Transmission rate
            int recovered = Math.min(pop, cell.getEpidemicRecoveredOr(0));
            int susceptible = Math.max(0, pop - infected - recovered);
            double lambda = beta * ((double) infected / pop);
            double probExposure = 1.0 - Math.exp(-lambda * (dt / 10.0));
            int newExposed = (int) Math.round(susceptible * probExposure);
            newExposed = Math.clamp(newExposed, 0, susceptible);

            double probInfectious = 1.0 - Math.exp(-SIGMA_LATENCY_RATE * dt);
            int newInfectious = (int) Math.round(newExposed * probInfectious);
            newInfectious = Math.clamp(newInfectious, 0, newExposed);

            double totalRemovalRate = GAMMA_RECOVERY_RATE + VIRULENCE_FATALITY;
            double probRemoval = 1.0 - Math.exp(-totalRemovalRate * dt);
            int totalRemovals = (int) Math.round(infected * probRemoval);
            totalRemovals = Math.clamp(totalRemovals, 0, infected);

            int recoveries = (int) Math.round(totalRemovals * (GAMMA_RECOVERY_RATE / totalRemovalRate));
            int deaths = totalRemovals - recoveries;

            int netInfected = Math.clamp(infected + newInfectious - totalRemovals, 0, pop);
            cell.setEpidemicInfected(netInfected);
            cell.setEpidemicRecovered(Math.min(Math.max(0, pop - deaths), recovered + recoveries));
            H3CellPhysicsAndStateHelper.synchronizeCohortsToPopulation(cell, Math.max(0, pop - deaths));

            // Spatial propagation to immediate H3 neighbors (only into susceptible hosts)
            if (netInfected > 50 && cell.getH3Index() != null) {
                List<Long> neighborIndexes = h3Service.getNeighbors(cell.getH3Index());
                if (neighborIndexes != null && !neighborIndexes.isEmpty()) {
                    int outboundSpread = (int) (netInfected * 0.02); // 2% cross-border leak
                    int perNeighborSpread = outboundSpread / neighborIndexes.size();
                    if (perNeighborSpread > 0) {
                        // Traverse hexagonal topological neighbor ring for spatial diffusion / flux
                        for (Long nIdx : neighborIndexes) {
                            H3Cell neighbor = lookup.get(nIdx);
                            if (neighbor != null && neighbor.getPopulation() != null && neighbor.getPopulation() > 0) {
                                int nPop = neighbor.getPopulation();
                                int nInfected = neighbor.getEpidemicInfectedOr(0);
                                int nRecovered = neighbor.getEpidemicRecoveredOr(0);
                                int nSusceptible = Math.max(0, nPop - nInfected - nRecovered);
                                int actualSpread = Math.min(nSusceptible, perNeighborSpread);
                                if (actualSpread > 0) {
                                    neighbor.setEpidemicInfected(nInfected + actualSpread);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



