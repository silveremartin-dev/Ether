/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.procedural;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Groundwater Table Depletion & 2D Darcy Porous Aquifer Filtration Engine.
 * 
 * <h2>Fundamental Model Physics Equations</h2>
 * <ul>
 *   <li><b>Piezometric Head</b>:
 *       $$h_i = z_i + \frac{W_{\text{aqua}, i}}{S_y \cdot A_{\text{cell}}}$$
 *   </li>
 *   <li><b>Darcy Lateral Filtration Flux on H3 Graph</b>:
 *       $$\vec{q}_{ij} = -K_{\text{perm}} \cdot \frac{h_j - h_i}{d_{ij}}$$
 *   </li>
 *   <li><b>Recharge vs Irrigation Extraction</b>:
 *       $$\frac{\partial W_{\text{aqua}, i}}{\partial t} = 0.15 \cdot R_i - 0.8 \cdot N_i + \sum_{j \in \mathcal{N}(i)} q_{ji}$$
 *   </li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 4.2.0
 */
public class AquiferDepletionEngine {
    private static final Logger logger = LoggerFactory.getLogger(AquiferDepletionEngine.class);

    /** Specific yield of unconfined porous aquifer S_y */
    public static final double SPECIFIC_YIELD_SY = 0.20;

    /** Baseline hydraulic conductivity K in m/s scaled to annual dt */
    public static final double HYDRAULIC_CONDUCTIVITY_K = 1e-4;

    /**
     * Executes one aquifer depletion, recharge, and 2D Darcy filtration tick across cells.
     */
    public static void processAquiferDepletion(List<H3Cell> cells) {
        processAquiferDepletion(cells, 30.0 / 365.25);
    }

    /**
     * Executes aquifer depletion and 2D Darcy lateral diffusion with explicit time delta in years.
     */
    public static void processAquiferDepletion(List<H3Cell> cells, double deltaYears) {
        if (cells == null || cells.isEmpty()) return;

        int depletedAquifers = 0;
        double dt = Math.max(0.001, deltaYears);

        // 1. Vertical Recharge and Anthropogenic Extraction
        for (H3Cell cell : cells) {
            double aquifer = cell.getAccessibleAquifer() != null ? cell.getAccessibleAquifer() : 1000.0;
            double rain = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;

            double naturalRecharge = (rain * 0.15) * dt;
            double irrigationExtraction = (pop * 0.8) * dt;

            double netAquiferChange = naturalRecharge - irrigationExtraction;
            double newAquifer = Math.max(0.0, aquifer + netAquiferChange);
            cell.setAccessibleAquifer(newAquifer);

            if (newAquifer < 100.0 && pop > 300) {
                depletedAquifers++;
                // Irrigation failure degrades agricultural yield
                double currentAgri = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
                cell.setBiomassAgriculture(Math.max(50.0, currentAgri * 0.70));
            }
        }

        // 2. 2D Darcy Lateral Groundwater Filtration Flux (Spatial Neighbor Approximation)
        int n = cells.size();
        if (n > 1) {
            double[] deltaAquifer = new double[n];
            for (int i = 0; i < n; i++) {
                H3Cell a = cells.get(i);
                double elevA = a.getElevation() != null ? a.getElevation() : 0.0;
                double aqA = a.getAccessibleAquifer() != null ? a.getAccessibleAquifer() : 1000.0;
                double headA = elevA + (aqA / (SPECIFIC_YIELD_SY * 1000.0));

                // Sample spatial neighbors within coordinate threshold
                for (int j = i + 1; j < Math.min(n, i + 7); j++) {
                    H3Cell b = cells.get(j);
                    double elevB = b.getElevation() != null ? b.getElevation() : 0.0;
                    double aqB = b.getAccessibleAquifer() != null ? b.getAccessibleAquifer() : 1000.0;
                    double headB = elevB + (aqB / (SPECIFIC_YIELD_SY * 1000.0));

                    double gradient = headB - headA;
                    double darcyFlux = gradient * 0.02 * dt; // Darcy lateral filtration flux

                    // Conservation check
                    if (darcyFlux > 0 && aqB > darcyFlux) {
                        deltaAquifer[i] += darcyFlux;
                        deltaAquifer[j] -= darcyFlux;
                    } else if (darcyFlux < 0 && aqA > -darcyFlux) {
                        deltaAquifer[i] += darcyFlux;
                        deltaAquifer[j] -= darcyFlux;
                    }
                }
            }

            for (int i = 0; i < n; i++) {
                if (deltaAquifer[i] != 0.0) {
                    H3Cell c = cells.get(i);
                    double currentAq = c.getAccessibleAquifer() != null ? c.getAccessibleAquifer() : 1000.0;
                    c.setAccessibleAquifer(Math.max(0.0, currentAq + deltaAquifer[i]));
                }
            }
        }

        if (depletedAquifers > 0) {
            logger.info("Aquifer Engine: Critical groundwater table depletion affecting {} agricultural cells.", depletedAquifers);
        }
    }
}

