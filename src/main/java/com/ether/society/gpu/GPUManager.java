/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package com.ether.society.gpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPUManager {
    private static final Logger logger = LoggerFactory.getLogger(GPUManager.class);
    private boolean gpuAvailable = false;

    public GPUManager() {
        logger.info("GPU Manager - CPU mode for MVP");
    }

    public void executeClimateKernel(int cellCount, double[] latitudes, double[] elevations,
            double[] temperatures, int currentMonth, double globalOffset) {
        for (int i = 0; i < cellCount; i++) {
            double lat = latitudes[i];
            double elev = elevations[i];
            double baseTemp = (1.0 - Math.abs(lat) / 90.0) * 35.0;
            double elevTemp = -elev * 0.006;
            double seasonalVar = Math.sin((currentMonth / 12.0) * 2 * Math.PI) * 10.0;
            temperatures[i] = baseTemp + elevTemp + seasonalVar + globalOffset;
        }
    }

    public boolean isGpuAvailable() {
        return gpuAvailable;
    }

}
