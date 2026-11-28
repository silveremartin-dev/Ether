/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.engines.tier2.theories;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.core.engine.AbstractSimulationEngine;
import org.ether.society.core.engine.DerivativeAccumulator;
import org.ether.society.core.engine.EngineTier;

/**
 * Krugman Core-Periphery &amp; New Economic Geography Engine (Krugman 1991; Fujita, Krugman &amp; Venables 1999).
 *
 * <p>Implements Paul Krugman's canonical Dixit-Stiglitz monopolistic competition and Samuelson iceberg
 * transport model in \(O(1)\) per cell over the local H3 hexagonal neighborhood
 * \(\mathcal{N}_i = \{i\} \cup \text{Ring}_1(i) \cup \{0_{\text{RoW}}\}\) in strictly resolution-invariant
 * areal density shares ({@code AGENTS.md} Contract 3):</p>
 * <ul>
 *   <li><b>Samuelson Iceberg Transport Factor</b>: \(T_{ij} = \exp(\tau_0 \, \bar{\mu}_{ij} \, d_{ij}) \ge 1\),
 *       with hexagonal centroid spacing \(d_{ij} = \sqrt{\frac{2}{\sqrt{3}} A_i}\).</li>
 *   <li><b>Dixit-Stiglitz Manufacturing Price Index (Forward Linkage)</b>:
 *       \(G_i = \left(\sum_{j \in \mathcal{N}_i} \lambda_j \, T_{ji}^{1 - \sigma}\right)^{\frac{1}{1 - \sigma}}\).</li>
 *   <li><b>Nominal Wage / Market Potential Equation (Backward Linkage)</b>:
 *       \(w_i = \left(\sum_{j \in \mathcal{N}_i} Y_j \, T_{ij}^{1 - \sigma} \, G_j^{\sigma - 1}\right)^{\frac{1}{\sigma}}\).</li>
 *   <li><b>Real Wage &amp; Replicator Capital Dynamics</b>:
 *       \(\omega_i = w_i \, G_i^{-\mu_M}\), \(\frac{dK_i}{dt} = \gamma_K \, K_i \left(\frac{\omega_i}{\omega_{\text{sym},i}} - 1\right)\).</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.5
 */
public class KrugmanCorePeripheryEngine extends AbstractSimulationEngine {

    /** Dixit-Stiglitz elasticity of substitution across varieties (\(\sigma > 1\), Krugman 1991 canonical benchmark). */
    public static final double ELASTICITY_OF_SUBSTITUTION_SIGMA = 4.0;

    /** Expenditure share on differentiated manufactured goods (\(\mu_M \in (0, 1)\), Krugman 1991 canonical benchmark). */
    public static final double MANUFACTURING_SHARE_MU = 0.40;

    /** Base Samuelson iceberg transport decay rate per kilometer (\(\tau_0\), \(\text{km}^{-1}\)). */
    public static final double ICEBERG_TRANSPORT_RATE_PER_KM = 0.004;

    /** Krugman replicator capital adjustment rate (\(\gamma_K\), \(\text{yr}^{-1}\)). */
    public static final double REPLICATOR_ADJUSTMENT_RATE = 0.12;

    /** Regular hexagon centroid-to-centroid geometric constant \(\sqrt{2 / \sqrt{3}} \approx 1.07457\). */
    private static final double HEX_CENTROID_DISTANCE_FACTOR = Math.sqrt(2.0 / Math.sqrt(3.0));

    private transient double meanCapitalDensityPerKm2 = 10.0;
    private transient double meanIncomeDensityPerKm2 = 10.0;

    /**
     * Constructs the Krugman Core-Periphery NEG engine.
     */
    public KrugmanCorePeripheryEngine() {
        super("KrugmanCorePeripheryEngine", "KrugmanCorePeripheryEngine", EngineTier.TIER_2_CLIODYNAMIC);
    }

    @Override
    public String getName() {
        return "Krugman Core-Periphery Spatial Agglomeration (NEG)";
    }

    @Override
    public String getDescription() {
        return "Models Krugman (1991) Dixit-Stiglitz price index, iceberg transport costs, nominal/real wage linkages, and core-periphery replicator bifurcation.";
    }

    @Override
    public String getEquationsTooltip() {
        return """
               [Krugman New Economic Geography (Krugman 1991, JPE 99:483)]
               • Iceberg Transport:     T_ij = exp(τ · d_ij),  T_ij >= 1
               • Price Index (Forward): G_i = [ Σ_j λ_j · T_ji^(1 - σ) ]^(1 / (1 - σ))
               • Wage Eq. (Backward):   w_i = [ Σ_j Y_j · T_ij^(1 - σ) · G_j^(σ - 1) ]^(1 / σ)
               • Real Wage & Replicator: ω_i = w_i · G_i^(-μ_M),  dK_i/dt = γ_K · K_i · (ω_i / ω_bar - 1)
               Parameters: σ = 4.0, μ_M = 0.40, τ [km^-1], d_ij [km]
               Ref: P. Krugman (1991) "Increasing Returns and Economic Geography", Journal of Political Economy 99(3): 483-499
               """;
    }

    @Override
    public String getCategory() {
        return "Tier 2: Spatial Economics";
    }

    @Override
    public boolean supportsCellularDerivatives() {
        return true;
    }

    @Override
    public void prepareTick(WorldBuffer buffer, double dtYears) {
        meanCapitalDensityPerKm2 = 10.0;
        meanIncomeDensityPerKm2 = 10.0;
        if (buffer == null || buffer.getResourceCapital() == null || buffer.getHumanPopulation() == null) return;

        float[] capArr = buffer.getResourceCapital();
        float[] popArr = buffer.getHumanPopulation();
        double capSum = 0.0;
        double incSum = 0.0;
        double areaSumKm2 = 0.0;
        int n = buffer.getCapacity();
        for (int i = 0; i < n; i++) {
            double pop = popArr[i];
            if (pop > 1.0 && capArr[i] > 0.0f) {
                double cap = capArr[i];
                double area = Math.max(0.01, buffer.getCellAreaKm2(i));
                double kpc = cap / pop;
                capSum += cap;
                incSum += pop * (1.0 + kpc / 250.0);
                areaSumKm2 += area;
            }
        }
        if (areaSumKm2 > 0.0) {
            meanCapitalDensityPerKm2 = Math.max(1.0e-4, capSum / areaSumKm2);
            meanIncomeDensityPerKm2 = Math.max(1.0e-4, incSum / areaSumKm2);
        }
    }

    @Override
    public void accumulateCellularDerivatives(WorldBuffer buffer, int i, double dtYears, double dtMonthly, DerivativeAccumulator acc) {
        if (buffer == null || buffer.getHumanPopulation() == null || dtYears <= 0.0) return;
        double popI = buffer.getHumanPopulation()[i];
        if (popI <= 1.0) return;

        float[] popArr = buffer.getHumanPopulation();
        float[] capArr = buffer.getResourceCapital();
        float[] fluxArr = buffer.getFluxPressure();
        double capI = capArr != null ? Math.max(0.0, capArr[i]) : 10.0;
        if (capI <= 0.0) return;

        double areaI = Math.max(0.01, buffer.getCellAreaKm2(i));
        double dNeighborKm = HEX_CENTROID_DISTANCE_FACTOR * Math.sqrt(areaI);
        double muI = (fluxArr != null && fluxArr[i] > 0.0f) ? Math.max(0.20, fluxArr[i]) : 1.0;

        final double oneMinusSigma = 1.0 - ELASTICITY_OF_SUBSTITUTION_SIGMA; // -3.0

        // Accumulate unnormalized manufacturing shares (sK) and income shares (sY) over {i} U Ring1(i) U {RoW}
        double sKi = (capI / areaI) / meanCapitalDensityPerKm2;
        double sYi = ((popI / areaI) * (1.0 + (capI / popI) / 250.0)) / meanIncomeDensityPerKm2;

        double sumSK = sKi + 1.0; // +1.0 for Rest-of-World mean-field sector
        double sumSY = sYi + 1.0;
        int nodeCount = 2;

        int[][] neighborIndexes = buffer.getNeighborIndexes();
        int[] ring = (neighborIndexes != null && i < neighborIndexes.length) ? neighborIndexes[i] : null;
        int cap = buffer.getCapacity();

        if (ring != null) {
            for (int nIdx : ring) {
                if (nIdx < 0 || nIdx >= cap || popArr[nIdx] <= 1.0f) continue;
                double areaJ = Math.max(0.01, buffer.getCellAreaKm2(nIdx));
                double capJ = capArr != null ? Math.max(0.0, capArr[nIdx]) : 0.0;
                double popJ = popArr[nIdx];
                sumSK += (capJ / areaJ) / meanCapitalDensityPerKm2;
                sumSY += ((popJ / areaJ) * (1.0 + (capJ / popJ) / 250.0)) / meanIncomeDensityPerKm2;
                nodeCount++;
            }
        }

        double invSumSK = 1.0 / Math.max(1.0e-9, sumSK);
        double invSumSY = 1.0 / Math.max(1.0e-9, sumSY);
        double uniformShare = 1.0 / nodeCount;

        // 1. Self term (j = i, d_ii = 0 => T_ii = 1)
        double priceSumActual = (sKi * invSumSK);
        double priceSumSym = uniformShare;

        // 2. Rest-of-World mean-field trading partner (d_0 = 2.5 * dNeighborKm, G_0 / G_sym = 1)
        double tRoW = Math.exp(Math.min(12.0, ICEBERG_TRANSPORT_RATE_PER_KM * muI * (2.5 * dNeighborKm)));
        double phiRoW = Math.pow(tRoW, oneMinusSigma); // T^(1 - sigma) in (0, 1]
        priceSumActual += (1.0 * invSumSK) * phiRoW;
        priceSumSym += uniformShare * phiRoW;

        // 3. Contiguous H3 Ring-1 neighbors
        if (ring != null) {
            for (int nIdx : ring) {
                if (nIdx < 0 || nIdx >= cap || popArr[nIdx] <= 1.0f) continue;
                double areaJ = Math.max(0.01, buffer.getCellAreaKm2(nIdx));
                double capJ = capArr != null ? Math.max(0.0, capArr[nIdx]) : 0.0;
                double muJ = (fluxArr != null && fluxArr[nIdx] > 0.0f) ? Math.max(0.20, fluxArr[nIdx]) : 1.0;
                double muEdge = 0.5 * (muI + muJ);
                double tIJ = Math.exp(Math.min(12.0, ICEBERG_TRANSPORT_RATE_PER_KM * muEdge * dNeighborKm));
                double phiIJ = Math.pow(tIJ, oneMinusSigma);
                double lambdaJ = ((capJ / areaJ) / meanCapitalDensityPerKm2) * invSumSK;
                priceSumActual += lambdaJ * phiIJ;
                priceSumSym += uniformShare * phiIJ;
            }
        }

        // Normalized Dixit-Stiglitz price index G_tilde_i = G_i / G_sym (< 1 in manufacturing cores)
        double gTildeI = Math.pow(Math.max(1.0e-9, priceSumActual) / Math.max(1.0e-9, priceSumSym), 1.0 / oneMinusSigma);
        double gFactorSelf = Math.pow(gTildeI, ELASTICITY_OF_SUBSTITUTION_SIGMA - 1.0);

        // Nominal wage equation w_i = [ sum_j Y_j * T_ij^(1 - sigma) * G_j^(sigma - 1) ]^(1 / sigma)
        double wageSumActual = (sYi * invSumSY) * gFactorSelf + (1.0 * invSumSY) * phiRoW;
        double wageSumSym = uniformShare + uniformShare * phiRoW;

        if (ring != null) {
            for (int nIdx : ring) {
                if (nIdx < 0 || nIdx >= cap || popArr[nIdx] <= 1.0f) continue;
                double areaJ = Math.max(0.01, buffer.getCellAreaKm2(nIdx));
                double capJ = capArr != null ? Math.max(0.0, capArr[nIdx]) : 0.0;
                double popJ = popArr[nIdx];
                double muJ = (fluxArr != null && fluxArr[nIdx] > 0.0f) ? Math.max(0.20, fluxArr[nIdx]) : 1.0;
                double muEdge = 0.5 * (muI + muJ);
                double tIJ = Math.exp(Math.min(12.0, ICEBERG_TRANSPORT_RATE_PER_KM * muEdge * dNeighborKm));
                double phiIJ = Math.pow(tIJ, oneMinusSigma);
                double yJ = (((popJ / areaJ) * (1.0 + (capJ / popJ) / 250.0)) / meanIncomeDensityPerKm2) * invSumSY;
                wageSumActual += yJ * phiIJ;
                wageSumSym += uniformShare * phiIJ;
            }
        }

        double wTildeI = Math.pow(Math.max(1.0e-9, wageSumActual) / Math.max(1.0e-9, wageSumSym), 1.0 / ELASTICITY_OF_SUBSTITUTION_SIGMA);

        // Krugman real wage omega_i = w_i * G_i^(-mu_M) and replicator dynamics dK_i/dt = gamma_K * K_i * (omega_i - 1)
        double realWageRatio = wTildeI * Math.pow(Math.max(0.10, gTildeI), -MANUFACTURING_SHARE_MU);
        double replicatorSignal = realWageRatio - 1.0;

        if (replicatorSignal > 0.0) {
            acc.dFluxPressure -= (float) Math.min(0.04 * dtYears, 0.08 * replicatorSignal * dtYears);
        }
        double netAgglomerationRate = Math.clamp(REPLICATOR_ADJUSTMENT_RATE * replicatorSignal * dtYears, -0.05 * dtYears, 0.08 * dtYears);
        acc.dResourceCapital += (float) (capI * netAgglomerationRate);
    }
}
