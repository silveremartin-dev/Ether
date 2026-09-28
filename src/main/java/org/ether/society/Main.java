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
            Preferences execPrefs = Preferences.userNodeForPackage(ExecutionContextPanel.class);
            Preferences prefPrefs = Preferences.userNodeForPackage(org.ether.society.ui.PreferencesPanel.class);
            Preferences i18nPrefs = Preferences.userNodeForPackage(org.ether.society.i18n.I18n.class);
            Preferences themePrefs = Preferences.userNodeForPackage(org.ether.society.ui.Theme.class);

            for (int i = 0; i < args.length; i++) {
                String arg = args[i].trim();
                String a = arg.toLowerCase();

                if (a.equals("--headless") || a.equals("-h")
                        || a.equals("--mode=cluster") || a.equals("--cluster")
                        || a.startsWith("--role=") || a.equals("--master") || a.equals("--worker")
                        || a.equals("--server") || a.equals("--node")
                        || a.equals("--benchmark") || a.equals("--help") || a.equals("-help") || a.equals("-?")) {
                    headless = true;
                } else if (a.equals("--rust") || a.equals("--native") || a.equals("--engine=rust")) {
                    execPrefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.NATIVE_RUST.name());
                    execPrefs.putBoolean("ether_gpu_enabled", false);
                    prefPrefs.putBoolean("ether_gpu_enabled", false);
                } else if (a.equals("--gpu") || a.equals("--opencl") || a.equals("--engine=gpu")) {
                    execPrefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.GPU_SHADERS.name());
                    execPrefs.putBoolean("ether_gpu_enabled", true);
                    prefPrefs.putBoolean("ether_gpu_enabled", true);
                } else if (a.equals("--simd") || a.equals("--vector") || a.equals("--engine=simd")) {
                    execPrefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.JAVA_VECTOR_SIMD.name());
                    execPrefs.putBoolean("ether_gpu_enabled", false);
                    prefPrefs.putBoolean("ether_gpu_enabled", false);
                } else if (a.equals("--cpu") || a.equals("--engine=cpu")) {
                    execPrefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.CPU_JIT.name());
                    execPrefs.putBoolean("ether_gpu_enabled", false);
                    prefPrefs.putBoolean("ether_gpu_enabled", false);
                } else if (a.equals("--safe") || a.equals("--fallback") || a.equals("--engine=safe")) {
                    execPrefs.put("ether_hardware_mode", ExecutionContextPanel.HardwareMode.GPU_OFF.name());
                    execPrefs.putBoolean("ether_gpu_enabled", false);
                    prefPrefs.putBoolean("ether_gpu_enabled", false);
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
                    themePrefs.put("ether_theme", themeVal);
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
