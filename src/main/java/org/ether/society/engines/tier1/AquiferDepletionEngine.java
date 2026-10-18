/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier1;

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
 * @version 1.0.0-beta.2
 */
public class AquiferDepletionEngine {
    private static final Logger logger = LoggerFactory.getLogger(AquiferDepletionEngine.class);

    /* Specific yield of unconfined porous aquifer S_y */
    public static final double SPECIFIC_YIELD_SY = 0.20;

    /* Baseline hydraulic conductivity K in m/s scaled to annual dt */
    public static final double HYDRAULIC_CONDUCTIVITY_K = 1e-4;

    /*
     * Executes one aquifer depletion, recharge, and 2D Darcy filtration tick across cells.
     */
    public static void processAquiferDepletion(List<H3Cell> cells) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        processAquiferDepletion(cells, 30.0 / 365.25);
    }

    /*
     * Executes aquifer depletion and 2D Darcy lateral diffusion with explicit time delta in years.
     */
    public static void processAquiferDepletion(List<H3Cell> cells, double deltaYears) {
        // Phase 1: Invariant state validation and environmental boundary initialization
        // Phase 2: Numerical evaluation of differential conservation equations
        // Phase 3: Spatial coupling and local thermodynamic state update
        if (cells == null || cells.isEmpty()) return;

        int depletedAquifers = 0;
        double dt = Math.max(0.001, deltaYears);

        int size = cells.size();
        java.util.concurrent.atomic.AtomicInteger depletedCounter = new java.util.concurrent.atomic.AtomicInteger(0);

        java.util.function.Consumer<H3Cell> rechargeProcessor = cell -> {
            double aquifer = cell.getAccessibleAquifer() != null ? cell.getAccessibleAquifer() : 1000.0;
            double rain = cell.getRainfall() != null ? cell.getRainfall() : 500.0;
            int pop = cell.getPopulation() != null ? cell.getPopulation() : 0;

            double naturalRecharge = (rain * 0.15) * dt;
            double perCapitaGroundwaterDemand = (rain >= 800.0) ? 0.05 : (rain >= 400.0 ? 0.35 : 0.80);
            double irrigationExtraction = (pop * perCapitaGroundwaterDemand) * dt;

            double netAquiferChange = naturalRecharge - irrigationExtraction;
            double newAquifer = Math.clamp(aquifer + netAquiferChange, 0.0, 2000.0);
            cell.setAccessibleAquifer(newAquifer);

            if (newAquifer < 100.0 && pop > 300) {
                depletedCounter.incrementAndGet();
                double currentAgri = cell.getBiomassAgriculture() != null ? cell.getBiomassAgriculture() : 0.0;
                cell.setBiomassAgriculture(Math.max(50.0, currentAgri * (1.0 - 0.30 * dt)));
            }
        };

        if (size >= 10000) {
            cells.parallelStream().forEach(rechargeProcessor);
        } else {
            for (int i = 0; i < size; i++) {
                rechargeProcessor.accept(cells.get(i));
            }
        }

        // 2. 2D Darcy Lateral Groundwater Filtration Flux (Spatial Neighbor Approximation)
        int n = cells.size();
        if (n > 1) {
            double[] deltaAquifer = new double[n];
            double[] headArray = new double[n];
            double[] latArray = new double[n];
            double[] lonArray = new double[n];
            double[] aqArray = new double[n];

            for (int i = 0; i < n; i++) {
                H3Cell c = cells.get(i);
                double elev = c.getElevation() != null ? c.getElevation() : 0.0;
                double aq = c.getAccessibleAquifer() != null ? c.getAccessibleAquifer() : 1000.0;
                aqArray[i] = aq;
                headArray[i] = elev + (aq / (SPECIFIC_YIELD_SY * 1000.0));
                latArray[i] = c.getLatitude() != null ? c.getLatitude() : 0.0;
                lonArray[i] = c.getLongitude() != null ? c.getLongitude() : 0.0;
            }

            for (int i = 0; i < n; i++) {
                double headA = headArray[i];
                double latA = latArray[i];
                double lonA = lonArray[i];
                double cosLatA = Math.cos(Math.toRadians(latA));

                for (int j = i + 1; j < Math.min(n, i + 7); j++) {
                    double dLatDeg = latArray[j] - latA;
                    double dLonDeg = (lonArray[j] - lonA) * cosLatA;
                    double dy = dLatDeg * 111.0;
                    double dx = dLonDeg * 111.0;
                    double distKmSq = dx * dx + dy * dy;

                    // Strictly gate lateral Darcy aquifer exchange to adjacent local hydrological basin cells (dist <= 150 km)
                    if (distKmSq > 22500.0 || distKmSq < 0.01) continue;
                    double distKm = Math.sqrt(distKmSq);

                    double headB = headArray[j];
                    double gradient = (headB - headA) / distKm;
                    double darcyFlux = gradient * 0.5 * dt;

                    // Conservation check
                    if (darcyFlux > 0 && aqArray[j] > darcyFlux) {
                        deltaAquifer[i] += darcyFlux;
                        deltaAquifer[j] -= darcyFlux;
                    } else if (darcyFlux < 0 && aqArray[i] > -darcyFlux) {
                        deltaAquifer[i] += darcyFlux;
                        deltaAquifer[j] -= darcyFlux;
                    }
                }
            }

            for (int i = 0; i < n; i++) {
                if (deltaAquifer[i] != 0.0) {
                    H3Cell c = cells.get(i);
                    c.setAccessibleAquifer(Math.max(0.0, aqArray[i] + deltaAquifer[i]));
                }
            }
        }

        if (depletedAquifers > 0) {
            logger.info("Aquifer Engine: Critical groundwater table depletion affecting {} agricultural cells.", depletedAquifers);
        }
    }
}



