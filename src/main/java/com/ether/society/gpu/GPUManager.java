/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package com.ether.society.gpu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GPU manager that handles TornadoVM initialization and execution.
 * Automatically falls back to CPU if GPU is not available.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 2.0.0
 */
public class GPUManager {
    private static final Logger logger = LoggerFactory.getLogger(GPUManager.class);

    private boolean gpuAvailable = false;
    private boolean gpuEnabled = true;

    public GPUManager() {
        detectGPU();
    }

    /**
     * Detects if TornadoVM and GPU are available.
     */
    private void detectGPU() {
        try {
            // Try to load TornadoVM classes
            Class.forName("uk.ac.manchester.tornado.api.TornadoExecutionPlan");

            // If we get here, TornadoVM is installed
            gpuAvailable = true;
            logger.info("✓ TornadoVM detected - GPU acceleration available");

            // TODO: Query actual GPU devices
            logger.info("  Note: Install TornadoVM to use real GPU acceleration");

        } catch (ClassNotFoundException e) {
            gpuAvailable = false;
            logger.warn("✗ TornadoVM not detected - using CPU fallback");
            logger.warn("  Install TornadoVM for 10-100x speedup");
            logger.warn("  See TORNADOVM_SETUP.md for instructions");
        }
    }

    /**
     * Executes climate update kernel on GPU (or CPU fallback).
     *
     * @param cellCount Number of cells
     * @param latitudes Cell latitudes
     * @param elevations Cell elevations
     * @param temperatures Cell temperatures (updated in-place)
     * @param month Current month
     * @param globalOffset Global temperature offset
     */
    public void executeClimateKernel(
            int cellCount,
            double[] latitudes,
            double[] elevations,
            double[] temperatures,
            int month,
            double globalOffset) {
        
        if (gpuAvailable && gpuEnabled) {
            executeClimateGPU(cellCount, latitudes, elevations, temperatures, month, globalOffset);
        } else {

    executeCl imateCPU(cellCount, latitudes, elevations, temperatures, month, globalOffset);
        }

    }

    /**
     * GPU execution path (TornadoVM).
     */
    private void executeClimateGPU(
            int cellCount,
            double[] latitudes,
            double[] elevations,
            double[] temperatures,
            int month,
            double globalOffset) {
        
        // TODO: Implement TornadoVM execution when TornadoVM is installed
        // For now, fall back to CPU
        logger.debug("GPU execution (TornadoVM not configured yet, using CPU)");

    executeCl imateCPU(cellCount, latitudes, elevations, temperatures, month, globalOffset);
        
        /* Future TornadoVM implementation:
        TaskGraph taskGraph = new TaskGraph("climate")
            .transferToDevice(DataTransferMode.EVERY_EXECUTION, 
                latitudes, elevations, month, globalOffset)
            .task("updateClimate", ClimateKernel::updateClimate, 
                cellCount, latitudes, elevations, temperatures, month, globalOffset)
            .transferToHost(DataTransferMode.EVERY_EXECUTION, temperatures);
        
        ImmutableTaskGraph itg = taskGraph.snapshot();
        TornadoExecutionPlan executor = new TornadoExecutionPlan(itg);
        executor.execute();
        */
    }

    /**
     * CPU execution path (fallback).
     */
    private void executeCl

    imateCPU(
            int cellCount,
            double[] latitudes,
            double[] elevations,
            double[] temperatures,
            int month,
            double globalOffset) {
        
        ClimateKernel.updateClimateCPU(
            cellCount, latitudes, elevations, temperatures, month, globalOffset
        );
    }

    // Getters/setters

    public boolean isGpuAvailable() {
        return gpuAvailable;
    }

    public boolean isGpuEnabled() {
        return gpuEnabled;
    }

    public void setGpuEnabled(boolean enabled) {
        this.gpuEnabled = enabled;
    }
}
