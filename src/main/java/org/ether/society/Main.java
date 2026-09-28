/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society;

import org.ether.society.ui.ExecutionContextPanel;
import java.util.prefs.Preferences;

/**
 * Main application entry point for Ether.
 * Parses CLI configuration options (Single-Core, Multi-Core, CPU, GPU, Cluster, Java/Rust engine)
 * and dispatches to HeadlessRunner or JavaFX GUI.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class Main {
    public static void main(String[] args) {
        boolean headless = false;
        if (args != null) {
            Preferences prefs = Preferences.userNodeForPackage(ExecutionContextPanel.class);
            for (String arg : args) {
                String a = arg.trim().toLowerCase();
                if (a.equals("--headless") || a.equals("-h")
                        || a.equals("--mode=cluster") || a.equals("--cluster")
                        || a.startsWith("--role=") || a.equals("--help") || a.equals("-help") || a.equals("-?")) {
                    headless = true;
                } else if (a.equals("--rust") || a.equals("--native") || a.equals("--engine=rust")) {
                    prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.NATIVE_RUST.name());
                    prefs.putBoolean("ether_gpu_enabled", false);
                } else if (a.equals("--gpu") || a.equals("--opencl") || a.equals("--engine=gpu")) {
                    prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.GPU_SHADERS.name());
                    prefs.putBoolean("ether_gpu_enabled", true);
                } else if (a.equals("--simd") || a.equals("--vector") || a.equals("--engine=simd")) {
                    prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.JAVA_VECTOR_SIMD.name());
                    prefs.putBoolean("ether_gpu_enabled", false);
                } else if (a.equals("--cpu") || a.equals("--engine=cpu")) {
                    prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.CPU_JIT.name());
                    prefs.putBoolean("ether_gpu_enabled", false);
                } else if (a.equals("--safe") || a.equals("--fallback") || a.equals("--engine=safe")) {
                    prefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.GPU_OFF.name());
                    prefs.putBoolean("ether_gpu_enabled", false);
                    System.setProperty("prism.order", "sw");
                } else if (a.equals("--single-core") || a.equals("--monocoeur")) {
                    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
                } else if (a.startsWith("--threads=") || a.startsWith("--cores=")) {
                    int threads = Integer.parseInt(a.substring(a.indexOf('=') + 1));
                    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(threads));
                }
            }
        }

        if (headless) {
            org.ether.society.cli.HeadlessRunner.run(args);
        } else {
            EtherApp.main(args);
        }
    }
}
