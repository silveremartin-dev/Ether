package org.ether.society.gpu;

import org.ether.society.database.H3Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

import java.util.List;

/**
 * Manages GPU acceleration using TornadoVM.
 * Handles extracting object data to primitive arrays (SoA), executing kernels,
 * and injecting results back.
 */
public class GPUManager {
    private static final Logger logger = LoggerFactory.getLogger(GPUManager.class);
    private boolean gpuAvailable = false;

    // Kernel data buffers
    private float[] temps;
    private float[] lats;
    private float[] elevs;
    private float[] seasonBaseArgs = new float[1];

    // TornadoVM structures
    private TaskGraph taskGraph;
    private TornadoExecutionPlan executionPlan;

    public GPUManager() {
        checkAvailability();
    }

    private void checkAvailability() {
        try {
            // Simple check to see if TornadoVM SDK is loadable
            Class.forName("uk.ac.manchester.tornado.api.TaskGraph");
            // If we are here, the API is present.
            // Note: Runtime capability usually needs a specific JDK.
            // We'll try to init a dummy task later to verify.
            this.gpuAvailable = true;
            logger.info("TornadoVM API found. GPU acceleration candidate.");
        } catch (ClassNotFoundException e) {
            logger.warn("TornadoVM API not found. Running in CPU-only mode.");
            this.gpuAvailable = false;
        }
    }

    public boolean isGpuAvailable() {
        return gpuAvailable;
    }

    /**
     * computeClimate on GPU.
     */
    /**
     * computeClimate on GPU.
     */
    public void executeClimateKernel(List<H3Cell> cells, float globalOffset) {
        int size = cells.size();

        // 1. Data Transfer (Objects -> SoA)
        // Reallocate only if size changes
        if (temps == null || temps.length != size) {
            temps = new float[size];
            lats = new float[size];
            elevs = new float[size];
        }

        // Parallel extract to arrays
        for (int i = 0; i < size; i++) {
            H3Cell c = cells.get(i);
            lats[i] = c.getLatitude().floatValue();
            elevs[i] = c.getElevation() != null ? c.getElevation().floatValue() : 0.0f;
            temps[i] = 0; // Output buffer
        }

        // Update scalar arguments (using array to allow dynamic update in TornadoVM)
        seasonBaseArgs[0] = globalOffset;

        // 2. Execute Kernel
        if (gpuAvailable) {
            try {
                // Construct TaskGraph on first run (or if changed)
                if (taskGraph == null) {
                    taskGraph = new TaskGraph("climate")
                            .transferToDevice(DataTransferMode.EVERY_EXECUTION, lats, elevs, seasonBaseArgs)
                            .task("compute", SimulationKernel::computeClimate, temps, lats, elevs, seasonBaseArgs)
                            .transferToHost(DataTransferMode.EVERY_EXECUTION, temps);

                    executionPlan = new TornadoExecutionPlan(taskGraph);
                }

                executionPlan.execute();
            } catch (Exception e) {
                logger.error("GPU Execution failed, falling back to CPU", e);
                fallbackClimate(cells);
                return;
            }
        } else {
            fallbackClimate(cells);
            return;
        }

        // 3. Data Transfer Back (SoA -> Objects)
        for (int i = 0; i < size; i++) {
            cells.get(i).setTemperature((double) temps[i]);
        }
    }

    private void fallbackClimate(List<H3Cell> cells) {
        // Run kernel logic on CPU
        SimulationKernel.computeClimate(temps, lats, elevs, seasonBaseArgs);

        // Write back
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setTemperature((double) temps[i]);
        }
    }
}
