package org.ether.society.gpu;

import uk.ac.manchester.tornado.api.annotations.Parallel;

/**
 * GPU-accelerated simulation kernels.
 * The static methods here are designed to be compiled to OpenCL/SPIR-V by
 * TornadoVM.
 * 
 * Logic must be simple (primitive arrays, no objects).
 */
public class SimulationKernel {

    /**
     * Update Climate (Temperature).
     * 
     * @param temps      output temperature array
     * @param lats       latitude array
     * @param elevs      elevation array
     * @param seasonBase pre-calculated seasonal cosine value * seasonalVariation
     */
    public static void computeClimate(float[] temps, float[] lats, float[] elevs, float[] seasonBase) {
        for (@Parallel
        int i = 0; i < temps.length; i++) {
            float lat = lats[i];
            float elev = elevs[i];

            // 1. Base Temp
            float latFactor = Math.abs(lat) / 90.0f;
            float base = 30.0f - latFactor * 50.0f; // 30 down to -20 range matches CPU roughly

            // 2. Seasonal Effect
            float sBase = seasonBase[0];
            float seasonal = (lat >= 0) ? sBase : -sBase;
            seasonal *= latFactor;

            // 3. Elevation Effect
            float lapse = -(elev * 0.006f);

            temps[i] = base + seasonal + lapse;
        }
    }

    /**
     * Compute Food Growth.
     * 
     * @param food        OUT: Updated food
     * @param currentFood IN: Current food
     * @param maxCapacity IN: Max capacity per cell
     * @param growthRate  IN: Growth rate coefficient
     */
    public static void computeFood(float[] food, float[] currentFood, float[] maxCapacity, float growthRate) {
        for (@Parallel
        int i = 0; i < food.length; i++) {
            float current = currentFood[i];
            float max = maxCapacity[i];

            // Logistic growth
            // dF = r * F * (1 - F/K)
            // But simplified: growth proportional to space

            float growth = growthRate * current * (1.0f - (current / Math.max(1.0f, max)));
            float next = current + growth;

            // Clamp
            if (next > max)
                next = max;
            if (next < 0)
                next = 0;

            food[i] = next;
        }
    }
}
