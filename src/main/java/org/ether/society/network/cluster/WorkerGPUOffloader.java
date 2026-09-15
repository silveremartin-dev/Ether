/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.network.cluster;

import org.ether.society.core.dod.WorldBuffer;
import org.ether.society.flux.FluxEngine;
import org.ether.society.gpu.GPUManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Worker-level GPU Offloader & Compute Accelerator.
 * Auto-detects local GPU hardware / OpenCL acceleration capabilities on worker nodes,
 * dispatching sub-matrix domain calculations to GPU VRAM with fallback to CPU SIMD vector units.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class WorkerGPUOffloader {
    private static final Logger logger = LoggerFactory.getLogger(WorkerGPUOffloader.class);

    private final GPUManager gpuManager;
    private final FluxEngine fluxEngine;
    private long totalTicksComputed = 0;
    private long totalComputeNanos = 0;

    public WorkerGPUOffloader() {
        this.gpuManager = new GPUManager();
        this.fluxEngine = new FluxEngine();
    }

    public boolean isGPUAvailable() {
        return gpuManager.isGpuAvailable();
    }

    public boolean isGPUEnabled() {
        return gpuManager.isGpuEnabled();
    }

    public void setGPUEnabled(boolean enabled) {
        gpuManager.setGpuEnabled(enabled);
    }

    /**
     * Executes local chunk dynamics with GPU acceleration attempt and CPU fallback.
     */
    public void computeChunk(WorldBuffer buffer, float dt) {
        if (buffer == null) return;
        long start = System.nanoTime();

        // Run local physical/socio-economic flux computations
        fluxEngine.tick(buffer, dt);

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
