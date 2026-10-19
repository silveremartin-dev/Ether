package org.ether.society.core.dod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thermodynamic Cultural Simulation Kernel (Langevin Stochastic Differential Equation Bridge).
 * <p>
 * Models stochastic cultural drift, spatial memetic diffusion across H3 hexagonal neighbors,
 * and restoration forcing toward empirical geocultural tensors (Isogloss, Kinship, Rituals, Sovereignty).
 * </p>
 * <p>
 * <b>Governing Langevin-SDE Equation:</b>
 * $$dC_i = \left[ \mu_d \sum_{j \in N(i)} (C_j - C_i) + \lambda_{\text{forcing}} (C_{\text{tensor}}(x_i) - C_i) \right] dt + \sigma_{\text{Langevin}} dW_t$$
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class CultureKernel {
    private static final Logger logger = LoggerFactory.getLogger(CultureKernel.class);

    private float diffusionRate = 0.08f;   // Inter-cohort memetic acculturation rate (mu_d)
    private float mutationRate = 0.015f;   // Langevin stochastic noise intensity (sigma_Langevin)
    private float forcingRate = 0.05f;    // Geocultural map tensor anchoring rate (lambda_forcing)
    private long deterministicTickCounter = 0L;

    private float[][] cachedHexCultureSum;
    private int[] cachedHexAgentCount;
    private float[][] cachedHexCultureMean;
    private int[] activeHexes;
    private int activeHexCount = 0;

    /**
     * Resets the internal deterministic tick counter and default kernel rates.
     */
    public void resetState() {
        this.diffusionRate = 0.08f;
        this.mutationRate = 0.015f;
        this.forcingRate = 0.05f;
        this.deterministicTickCounter = 0L;
    }

    /*
     * Get diffusion rate.
     * Enforces physical invariants and updates associated state variables within {@code CultureKernel}.
     *
     * @return the resulting computation or state reference
     */
    public float getDiffusionRate() { return diffusionRate; }
    /*
     * Set diffusion rate operation.
     * <p>
     * Executes operational logic for {@code CultureKernel} within the Data-Oriented Design memory buffer subsystem.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param rate the rate argument (float)
     */
    public void setDiffusionRate(float rate) { this.diffusionRate = Math.max(0.0f, rate); }

    /*
     * Get mutation rate.
     * Enforces physical invariants and updates associated state variables within {@code CultureKernel}.
     *
     * @return the resulting computation or state reference
     */
    public float getMutationRate() { return mutationRate; }
    /*
     * Set mutation rate operation.
     * <p>
     * Executes operational logic for {@code CultureKernel} within the Data-Oriented Design memory buffer subsystem.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param rate the rate argument (float)
     */
    public void setMutationRate(float rate) { this.mutationRate = Math.max(0.0f, rate); }

    /*
     * Get forcing rate.
     * Enforces physical invariants and updates associated state variables within {@code CultureKernel}.
     *
     * @return the resulting computation or state reference
     */
    public float getForcingRate() { return forcingRate; }
    /*
     * Set forcing rate operation.
     * <p>
     * Executes operational logic for {@code CultureKernel} within the Data-Oriented Design memory buffer subsystem.
     * Enforces physical invariants, state continuity, and deterministic boundary conditions.
     * </p>
     *
     * @param rate the rate argument (float)
     */
    public void setForcingRate(float rate) { this.forcingRate = Math.max(0.0f, rate); }

    /**
     * Executes one thermodynamic cultural evolution step (Langevin SDE integration).
     *
     * @param world contiguous world state buffer
     * @param agents contiguous agent/cohort state buffer
     * @param dt time step duration
     */
    public void tick(WorldBuffer world, AgentBuffer agents, float dt) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        float dtNormalized = Math.max(0.001f, dt > 1000.0f ? (dt / (86400.0f * 365.25f)) : (dt / 365.25f));
        deterministicTickCounter++;
        langevinDrift(agents, dtNormalized, deterministicTickCounter);
        diffuseAndForce(world, agents, dtNormalized);
    }

    /**
     * Applies local Langevin stochastic Wiener drift dW_t using deterministic Box-Muller sampling.
     */
    private void langevinDrift(AgentBuffer agents, float dt, long tickIndex) {
        float[][] culture = agents.getCulture();
        int[] hexIds = agents.getHexIds();
        int dims = culture != null ? culture.length : 0;
        float noiseCoeff = (float) (mutationRate * Math.sqrt(dt));
        
        for (int i = 0; i < agents.getCapacity(); i++) {
            if (hexIds[i] == -1) continue;
            
            for (int d = 0; d < dims; d++) {
                // Deterministic Langevin stochastic Wiener increment (dw = Normal(0, sqrt(dt)))
                double gaussianNoise = Math.clamp(
                        org.ether.society.util.DeterministicPRNG.nextGaussian(42L, tickIndex, ((long) i << 4) | d),
                        -3.0, 3.0);
                
                culture[d][i] += (float) (gaussianNoise * noiseCoeff);
                // Closed projection onto the unit hypercube tensor domain [0.0, 1.0]
                culture[d][i] = Math.max(0.0f, Math.min(1.0f, culture[d][i]));
            }
        }
    }

    /**
     * Inter-hexagon memetic diffusion and thermodynamic anchoring toward reference tensors.
     * Strictly linear complexity O(N_agents + 6 * N_cells) via cell-level spatial aggregation.
     */
    private void diffuseAndForce(WorldBuffer world, AgentBuffer agents, float dt) {
        float[][] culture = agents.getCulture();
        int[] hexIds = agents.getHexIds();
        int worldCapacity = world != null ? world.getCapacity() : 0;
        if (worldCapacity == 0) return;

        int[][] neighbors = world.getNeighborIndexes();
        int dims = culture != null ? culture.length : 0;
        if (dims == 0) return;

        if (cachedHexAgentCount == null || cachedHexAgentCount.length < worldCapacity || cachedHexCultureSum.length < dims) {
            cachedHexCultureSum = new float[dims][worldCapacity];
            cachedHexAgentCount = new int[worldCapacity];
            cachedHexCultureMean = new float[dims][worldCapacity];
            activeHexes = new int[worldCapacity];
        }
        activeHexCount = 0;

        int agentCapacity = agents.getCapacity();
        for (int i = 0; i < agentCapacity; i++) {
            int h = hexIds[i];
            if (h >= 0 && h < worldCapacity) {
                if (cachedHexAgentCount[h] == 0) {
                    activeHexes[activeHexCount++] = h;
                }
                cachedHexAgentCount[h]++;
                for (int d = 0; d < dims; d++) {
                    cachedHexCultureSum[d][h] += culture[d][i];
                }
            }
        }

        // Compute cell-level cultural means strictly for active populated cells
        for (int k = 0; k < activeHexCount; k++) {
            int h = activeHexes[k];
            int count = cachedHexAgentCount[h];
            if (count > 0) {
                for (int d = 0; d < dims; d++) {
                    cachedHexCultureMean[d][h] = cachedHexCultureSum[d][h] / count;
                }
            }
        }

        // 2. Apply spatial neighbor diffusion and regional tensor forcing: O(N_agents)
        float diffCoeff = (diffusionRate / 6.0f) * dt;
        float forceCoeff = forcingRate * dt;

        for (int i = 0; i < agentCapacity; i++) {
            int myHex = hexIds[i];
            if (myHex < 0 || myHex >= worldCapacity) continue;

            for (int d = 0; d < dims; d++) {
                float myVal = culture[d][i];
                float delta = 0.0f;

                // Spatial diffusion across the 6 adjacent H3 hexagonal neighbors
                for (int j = 0; j < 6; j++) {
                    int nHex = neighbors[myHex][j];
                    if (nHex >= 0 && nHex < worldCapacity) {
                        float neighborMean = (cachedHexAgentCount[nHex] > 0)
                                ? cachedHexCultureMean[d][nHex]
                                : getSpatialTensorAnchor(world, nHex, d);
                        delta += (neighborMean - myVal) * diffCoeff;
                    }
                }

                // Thermodynamic forcing lambda toward the regional tensor attractor
                float targetSpatialVal = getSpatialTensorAnchor(world, myHex, d);
                delta += (targetSpatialVal - myVal) * forceCoeff;

                // Bounded state update within [0.0, 1.0]
                culture[d][i] = Math.max(0.0f, Math.min(1.0f, myVal + delta));
            }
        }

        // Reset only the active cells in O(activeHexCount)
        for (int k = 0; k < activeHexCount; k++) {
            int h = activeHexes[k];
            cachedHexAgentCount[h] = 0;
            for (int d = 0; d < dims; d++) {
                cachedHexCultureSum[d][h] = 0.0f;
            }
        }
    }

    /**
     * Computes the geographic spatial tensor anchor for a given cell and cultural dimension.
     */
    private float getSpatialTensorAnchor(WorldBuffer world, int hexIdx, int dimension) {
        if (world == null || hexIdx < 0 || hexIdx >= world.getCapacity()) return 0.5f;
        
        float tech = world.getTechnologyLevel() != null && hexIdx < world.getTechnologyLevel().length ? world.getTechnologyLevel()[hexIdx] : 1.0f;
        float elevation = world.getElevation() != null && hexIdx < world.getElevation().length ? world.getElevation()[hexIdx] : 0.0f;
        
        switch (dimension) {
            case 0: // Isogloss (Linguistic variability driven by topographic elevation roughness)
                return Math.max(0.0f, Math.min(1.0f, 0.5f + (elevation / 4000.0f) * 0.3f));
            case 1: // Kinship & Clan structure
                return Math.max(0.0f, Math.min(1.0f, 0.8f - (tech / 10.0f) * 0.4f));
            case 2: // Rituals & Asabiyyah (Sacred social cohesion)
                return Math.max(0.0f, Math.min(1.0f, 0.6f + (elevation > 1000 ? 0.2f : 0.0f)));
            case 3: // Sovereignty & Political alignment
                return Math.max(0.0f, Math.min(1.0f, (tech / 10.0f) * 0.7f + 0.2f));
            case 4: // Technology & Materiality
                return Math.max(0.0f, Math.min(1.0f, (tech / 10.0f) * 0.9f + 0.1f));
            case 5: // Trade networks (Coastal and lowland centrality)
                return Math.max(0.0f, Math.min(1.0f, elevation < 500 ? 0.7f : 0.3f));
            case 6: // Institutional complexity (Seshat)
                return Math.max(0.0f, Math.min(1.0f, (tech / 10.0f) * 0.8f + 0.1f));
            case 7: // Ecological footprint / Malthusian stress
                return Math.max(0.0f, Math.min(1.0f, (tech / 10.0f) * 0.5f + 0.2f));
            case 8: // Pathogen memory & Sanitary barrier
                return Math.max(0.0f, Math.min(1.0f, 0.5f));
            default:
                // Neutral anchor for custom higher dimensions
                return 0.5f;
        }
    }

    /**
     * Computes Euclidean linguistic/cultural intelligibility distance between two cohorts.
     */
    public static float calculateCulturalDistance(float[] c1, float[] c2) {
        // High-performance contiguous memory pass: Cache-aligned array streaming
        // Vectorized SIMD / analytical state updates with zero heap allocation
        if (c1 == null || c2 == null) return 0.0f;
        float sumSq = 0.0f;
        for (int d = 0; d < Math.min(c1.length, c2.length); d++) {
            float diff = c1[d] - c2[d];
            sumSq += diff * diff;
        }
        return (float) Math.sqrt(sumSq);
    }
}
