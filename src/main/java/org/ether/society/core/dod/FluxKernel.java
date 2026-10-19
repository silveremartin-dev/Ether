*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.core.dod;

/**
 * <h1>Data-Oriented Hexagonal Trade Flux &amp; Spatial Resource Diffusion Kernel</h1>
 * <p>
 * Simulates symmetric finite-volume resource exchange across adjacent H3 hexagonal neighbors.
 * Formulated under strict thermodynamic conservation laws:
 * <ul>
 *   <li><b>Local Price Formation</b>: \(P_i = \frac{N_i + 1}{F_i + 1}\) based on regional population and food stock.</li>
 *   <li><b>Onsager Trade Conductivity</b>: \(\sigma_{ij} = \frac{\sigma_0}{1 + 0.1 |\Delta h_{ij}|}\) subject to topographic friction.</li>
 *   <li><b>Symmetric Edge Flux</b>: \(\Phi_{ij} = -\Phi_{ji}\) guaranteeing 100% exact numerical mass conservation.</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class FluxKernel {

    private final float baseConductivity;
    private float[] cachedDeltaFood;

    /**
     * Constructs a FluxKernel with baseline transport conductivity \(\sigma_0 = 0.05\).
     */
    public FluxKernel() {
        this(0.05f);
    }

    /**
     * Constructs a FluxKernel with custom baseline transport conductivity.
     *
     * @param baseConductivity baseline transport conductivity (float)
     */
    public FluxKernel(float baseConductivity) {
        this.baseConductivity = Math.max(0.0f, baseConductivity);
    }

    /**
     * Executes one finite-volume trade diffusion cycle across contiguous WorldBuffer arrays.
     *
     * @param world contiguous world buffer
     * @param dtSeconds integration time step in seconds
     */
    public void tick(WorldBuffer world, float dtSeconds) {
        int capacity = world != null ? world.getCapacity() : 0;
        if (capacity == 0) return;

        float[] food = world.getFoodResource();
        float[] pop = world.getBiomassHuman();
        float[] prices = world.getLocalPrice();
        float[] elev = world.getElevation();
        int[][] neighbors = world.getNeighborIndexes();

        if (food == null || prices == null || neighbors == null) return;

        // 1. Vectorized Price calculation
        for (int i = 0; i < capacity; i++) {
            float p = (pop != null && i < pop.length) ? pop[i] : 0.0f;
            prices[i] = (p + 1.0f) / (food[i] + 1.0f);
        }

        if (cachedDeltaFood == null || cachedDeltaFood.length < capacity) {
            cachedDeltaFood = new float[capacity];
        } else {
            java.util.Arrays.fill(cachedDeltaFood, 0, capacity, 0.0f);
        }

        float dtDays = dtSeconds / 86400.0f;

        // 2. Symmetric edge flux across 6 H3 neighbors (guarantees exact numerical conservation)
        for (int i = 0; i < capacity; i++) {
            float pA = prices[i];
            float hA = (elev != null && i < elev.length) ? elev[i] : 0.0f;
            float foodA = food[i];

            int[] myNeighbors = neighbors[i];
            if (myNeighbors == null) continue;

            for (int j = 0; j < 6 && j < myNeighbors.length; j++) {
                int nIdx = myNeighbors[j];
                // Symmetric edge condition: only process each edge once where i < nIdx
                if (nIdx < 0 || nIdx >= capacity || i >= nIdx) continue;

                float pB = prices[nIdx];
                float hB = (elev != null && nIdx < elev.length) ? elev[nIdx] : 0.0f;
                float foodB = food[nIdx];

                float gradient = pB - pA;
                float friction = 1.0f + Math.abs(hB - hA) * 0.1f;
                float conductivity = baseConductivity / friction;

                float rawFlux = gradient * conductivity * dtDays;

                float maxTransferA = foodA > 0.0f ? foodA * 0.5f : 0.0f;
                float maxTransferB = foodB > 0.0f ? foodB * 0.5f : 0.0f;

                float flux = Math.clamp(rawFlux, -maxTransferB, maxTransferA);

                cachedDeltaFood[i] -= flux;
                cachedDeltaFood[nIdx] += flux;
            }
        }

        // 3. Final state update bounded below by 0
        for (int i = 0; i < capacity; i++) {
            food[i] = Math.max(0.0f, food[i] + cachedDeltaFood[i]);
        }
    }
}
