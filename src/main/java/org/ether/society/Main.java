/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society;

import java.util.prefs.Preferences;

/**
 * Main application entry point for Ether.
 * Parses CLI configuration options (Single-Core, Multi-Core, CPU, GPU, Cluster, Java/Rust engine)
 * and dispatches to HeadlessRunner or JavaFX GUI.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class Main {
    /**
     * Main.
     *
     * @param args the args parameter (String[])
     */
    public static void main(String[] args) {
        boolean headless = false;
        if (args != null) {
            Preferences uiPrefs = Preferences.userRoot().node("org/ether/society/ui");
            Preferences i18nPrefs = Preferences.userRoot().node("org/ether/society/i18n");

            for (int i = 0; i < args.length; i++) {
                String arg = args[i].trim();
                String a = arg.toLowerCase();

                if (a.equals("--headless") || a.equals("-h")
                        || a.equals("--mode=cluster") || a.equals("--cluster")
                        || a.startsWith("--role=") || a.equals("--master") || a.equals("--worker")
                        || a.equals("--server") || a.equals("--node")
                        || a.equals("--benchmark") || a.equals("--benchmark-real") || a.equals("--benchmark-dod")
                        || a.equals("--benchmark-micro") || a.equals("--help") || a.equals("-help") || a.equals("-?")) {
                    headless = true;
                } else if (a.equals("--rust") || a.equals("--native") || a.equals("--engine=rust")) {
                    uiPrefs.put("ether_hardware_mode", "NATIVE_RUST");
                    uiPrefs.putBoolean("ether_gpu_enabled", false);
                } else if (a.equals("--gpu") || a.equals("--opencl") || a.equals("--engine=gpu")) {
                    uiPrefs.put("ether_hardware_mode", "GPU_SHADERS");
                    uiPrefs.putBoolean("ether_gpu_enabled", true);
                } else if (a.equals("--simd") || a.equals("--vector") || a.equals("--engine=simd")) {
                    uiPrefs.put("ether_hardware_mode", "JAVA_VECTOR_SIMD");
                    uiPrefs.putBoolean("ether_gpu_enabled", false);
                } else if (a.equals("--cpu") || a.equals("--engine=cpu")) {
                    uiPrefs.put("ether_hardware_mode", "CPU_JIT");
                    uiPrefs.putBoolean("ether_gpu_enabled", false);
                } else if (a.equals("--safe") || a.equals("--fallback") || a.equals("--engine=safe")) {
                    uiPrefs.put("ether_hardware_mode", "GPU_OFF");
                    uiPrefs.putBoolean("ether_gpu_enabled", false);
                    System.setProperty("prism.order", "sw");
                } else if (a.equals("--single-core") || a.equals("--monocoeur")) {
                    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
                } else if (a.startsWith("--threads=") || a.startsWith("--cores=")) {
                    int threads = Integer.parseInt(a.substring(a.indexOf('=') + 1));
                    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", String.valueOf(threads));
                } else if (a.startsWith("--lang=") || a.startsWith("--language=")) {
                    String langCode = a.substring(a.indexOf('=') + 1);
                    i18nPrefs.put("ether_language", langCode);
                } else if ((a.equals("-l") || a.equals("--lang") || a.equals("--language")) && i + 1 < args.length) {
                    i18nPrefs.put("ether_language", args[++i].trim().toLowerCase());
                } else if (a.startsWith("--theme=")) {
                    String themeVal = a.substring(a.indexOf('=') + 1).toUpperCase();
                    uiPrefs.put("ether_theme", themeVal);
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
