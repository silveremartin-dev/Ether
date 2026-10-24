/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.execution;

import org.ether.society.core.dod.NativeRustBridge;
import org.ether.society.ui.ExecutionContextPanel.HardwareMode;
import org.ether.society.ui.PreferencesPanel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.prefs.Preferences;

/**
 * Hardware capability detector and analytical execution throughput calculator.
 * <p>
 * Provides detection for Native Rust (Rayon + AVX-512 FFM bindings), Java 21 Vector SIMD,
 * and GPU compute shaders, along with analytical tick throughput approximations:
 * \[
 *   \tau_{\text{exec}} = \frac{N_{\text{ticks}}}{\text{TPS}(\text{HardwareMode})}
 * \]
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class ExecutionContextHardwareHelper {

    private static final Preferences prefs = Preferences.userNodeForPackage(PreferencesPanel.class);
    private static final String PREF_HARDWARE_MODE_KEY = "ether_hardware_mode";
    private static String cachedGpuName = null;

    private ExecutionContextHardwareHelper() {
        // Utility class
    }

    /**
     * Checks if Native Rust FFM shared library is available.
     *
     * @return true if available
     */
    public static boolean isRustAvailable() {
        return NativeRustBridge.isNativeAvailable();
    }

    /**
     * Checks if Java 21 Incubator Vector SIMD is supported on the host CPU.
     *
     * @return true if SIMD registers are present
     */
    public static boolean isSimdAvailable() {
        try {
            return jdk.incubator.vector.DoubleVector.SPECIES_PREFERRED.length() > 1;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Recommends the optimal hardware mode based on host capabilities.
     *
     * @return recommended hardware acceleration mode
     */
    public static HardwareMode getRecommendedHardwareMode() {
        if (isRustAvailable()) return HardwareMode.NATIVE_RUST;
        if (isSimdAvailable()) return HardwareMode.JAVA_VECTOR_SIMD;
        return HardwareMode.CPU_JIT;
    }

    /**
     * Checks whether a specific hardware mode is supported.
     *
     * @param mode hardware mode
     * @return true if supported
     */
    public static boolean isModeSupported(HardwareMode mode) {
        if (mode == null) return false;
        return switch (mode) {
            case NATIVE_RUST -> isRustAvailable();
            case GPU_SHADERS -> false;
            case JAVA_VECTOR_SIMD -> isSimdAvailable();
            case CPU_JIT, GPU_OFF -> true;
        };
    }

    /**
     * Retrieves the active hardware mode from preferences or recommended fallback.
     *
     * @return active hardware mode
     */
    public static HardwareMode getActiveHardwareMode() {
        String savedMode = prefs.get(PREF_HARDWARE_MODE_KEY, null);
        if (savedMode != null) {
            try {
                HardwareMode mode = HardwareMode.valueOf(savedMode);
                if (isModeSupported(mode)) {
                    return mode;
                }
            } catch (Exception ignored) {}
        }
        return getRecommendedHardwareMode();
    }

    /**
     * Estimates cell ticks throughput per second for a given hardware mode.
     *
     * @param mode hardware mode
     * @return estimated cells processed per second
     */
    public static double getEstimatedCellTicksThroughput(HardwareMode mode) {
        if (mode == null) mode = getActiveHardwareMode();
        return switch (mode) {
            case NATIVE_RUST -> 1_200_000.0;
            case GPU_SHADERS -> 850_000.0;
            case JAVA_VECTOR_SIMD -> 350_000.0;
            case CPU_JIT -> 150_000.0;
            case GPU_OFF -> 50_000.0;
        };
    }

    /**
     * Estimates execution wall-clock time in seconds for a given simulation period and cell count.
     *
     * @param startYear start year
     * @param endYear target end year
     * @param cellCount total cell count
     * @param mode hardware mode
     * @return estimated duration in seconds
     */
    public static double estimateExecutionTimeSeconds(long startYear, long endYear, int cellCount, HardwareMode mode) {
        long durationYears = Math.max(1, endYear - startYear);
        if (cellCount <= 0) cellCount = 4096;
        long totalTicks = (long) Math.ceil(durationYears * 52.142857);
        double tps = switch (mode != null ? mode : getActiveHardwareMode()) {
            case NATIVE_RUST -> 320.0;
            case GPU_SHADERS -> 260.0;
            case JAVA_VECTOR_SIMD -> 180.0;
            case CPU_JIT -> 60.0;
            case GPU_OFF -> 35.0;
        };
        double seconds = (double) totalTicks / tps;
        return Math.max(0.5, seconds);
    }

    /**
     * Formats duration in seconds into human-readable representation.
     *
     * @param seconds duration in seconds
     * @return localized formatted duration string
     */
    public static String formatDuration(double seconds) {
        if (seconds < 1.0) {
            return "< 1 s";
        } else if (seconds < 60.0) {
            return String.format(java.util.Locale.US, "~%.0f s", seconds);
        } else if (seconds < 3600.0) {
            int mins = (int) (seconds / 60.0);
            int secs = (int) (seconds % 60.0);
            return secs > 0 ? String.format("~%d min %d s", mins, secs) : String.format("~%d min", mins);
        } else {
            int hours = (int) (seconds / 3600.0);
            int mins = (int) ((seconds % 3600.0) / 60.0);
            return mins > 0 ? String.format("~%d h %d min", hours, mins) : String.format("~%d h", hours);
        }
    }

    /**
     * Detects host GPU controller name via system queries.
     *
     * @return GPU adapter name string
     */
    public static String getDetectedGpuName() {
        if (cachedGpuName != null) return cachedGpuName;
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                Process p = new ProcessBuilder("powershell", "-Command", "(Get-CimInstance Win32_VideoController).Name").start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.isBlank()) {
                        cachedGpuName = line.trim();
                        return cachedGpuName;
                    }
                }
            }
        } catch (Exception ignored) {}
        cachedGpuName = "Integrated Graphics (iGPU)";
        return cachedGpuName;
    }
}
