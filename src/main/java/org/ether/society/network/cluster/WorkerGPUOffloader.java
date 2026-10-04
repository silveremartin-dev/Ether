/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.cluster;

import org.ether.society.core.dod.EnvironmentalKernel;
import org.ether.society.core.dod.WorldBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker-level Compute Accelerator and Partition Task Offloader.
 * Dispatches sub-matrix domain calculations to high-performance SIMD vector units.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class WorkerGPUOffloader {
    private static final Logger logger = LoggerFactory.getLogger(WorkerGPUOffloader.class);

    private final EnvironmentalKernel environmentalKernel;
    private long totalTicksComputed = 0;
    private long totalComputeNanos = 0;

    public WorkerGPUOffloader() {
        this.environmentalKernel = new EnvironmentalKernel();
    }

    public boolean isGPUAvailable() {
        return false;
    }

    public boolean isGPUEnabled() {
        return false;
    }

    public void setGPUEnabled(boolean enabled) {
        // GPU acceleration replaced by pure deterministic SIMD
    }

    /**
     * Executes local chunk dynamics across partitioned WorldBuffer.
     */
    public void computeChunk(WorldBuffer buffer, float dt) {
        if (buffer == null) return;
        long start = System.nanoTime();

        // Run local physical/socio-economic flux computations
        environmentalKernel.tick(buffer, dt);

        long elapsed = System.nanoTime() - start;
        totalComputeNanos += elapsed;
        totalTicksComputed++;
    }

    public double getAverageComputeTimeMs() {
        return totalTicksComputed > 0 ? (totalComputeNanos / (double) totalTicksComputed) / 1_000_000.0 : 0.0;
    }

    public long getTotalTicksComputed() {
        return totalTicksComputed;
    }
}
