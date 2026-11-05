/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.execution;

import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import org.ether.society.database.DatabaseConfig;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.ExecutionContextPanel.HardwareMode;

/**
 * Stateless localization, tooltip, and persistence status helper for
 * {@link org.ether.society.ui.ExecutionContextPanel}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see org.ether.society.ui.ExecutionContextPanel
 */
public final class ExecutionContextI18nHelper {

    private ExecutionContextI18nHelper() {
        // Stateless UI helper
    }

    /**
     * Updates the database EntityManagerFactory status indicator badge.
     *
     * @param dbStatusLabel target status label
     */
    public static void updateDatabaseStatus(Label dbStatusLabel) {
        if (dbStatusLabel == null) return;
        try {
            var emf = DatabaseConfig.getEntityManagerFactory();
            if (emf != null && emf.isOpen()) {
                dbStatusLabel.setText(I18n.getOrDefault("exec.topology.db_status_ok",
                        "🟢 Base de données : Connectée & Opérationnelle (Persistance active)"));
                dbStatusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                return;
            }
        } catch (Throwable t) {
            String msg = I18n.getOrDefault("exec.topology.db_error_prefix", "🔴 Diagnostic base : {0}", t.getMessage());
            dbStatusLabel.setText(msg);
            dbStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            return;
        }
        dbStatusLabel.setText(I18n.getOrDefault("exec.topology.db_status_offline",
                "🟡 Base de données : Mode autonome en mémoire (WorldBuffer non persisté sur serveur distant)"));
        dbStatusLabel.setStyle("-fx-text-fill: #f59e0b;");
    }

    /**
     * Updates all localized labels and technical tooltips across the hardware, topology, and rendering sections.
     */
    public static void updatePanelTexts(
            Label titleHeader,
            Label liveBannerLabel,
            Label hardwareSectionHeader,
            Label topologySectionHeader,
            Label renderingSectionHeader,
            RadioButton rustNativeRadio, Label rustNativeDescLabel,
            RadioButton gpuShadersRadio, Label gpuShadersDescLabel,
            RadioButton javaVectorSimdRadio, Label javaVectorSimdDescLabel,
            RadioButton cpuJitRadio, Label cpuJitDescLabel,
            RadioButton gpuOffRadio, Label gpuOffDescLabel,
            Label determinismDisclaimerTitle, Label determinismDisclaimerText,
            RadioButton localTopologyRadio, Label localTopologyDescLabel,
            RadioButton clusterTopologyRadio, Label clusterTopologyDescLabel,
            Label dbSectionHeader, Label dbStatusLabel,
            ExecutionContextClusterSection clusterSection,
            RadioButton guiRenderingRadio, Label guiRenderingDescLabel,
            RadioButton headlessRenderingRadio, Label headlessRenderingDescLabel,
            Label lblTargetYear, Spinner<Integer> targetYearSpinner, Label lblTargetYearNote,
            ExecutionContextSidebar sidebar) {

        titleHeader.setText(I18n.getOrDefault("exec.title", "⚡ Execution Context & Compute Infrastructure"));
        if (liveBannerLabel != null) {
            liveBannerLabel.setText(I18n.getOrDefault("exec.live_notice",
                    "⚡ MODIFICATIONS EN DIRECT : Tout changement de moteur de calcul ou de configuration s'applique instantanément à la simulation en cours sans interruption."));
        }

        hardwareSectionHeader.setText(I18n.getOrDefault("exec.section.hardware", "1. 🖥️ COMPUTE ENGINE & HARDWARE ACCELERATION"));
        topologySectionHeader.setText(I18n.getOrDefault("exec.section.topology", "2. 🌐 EXECUTION TOPOLOGY & PERSISTENCE"));
        renderingSectionHeader.setText(I18n.getOrDefault("exec.section.rendering", "3. 🚀 RENDERING & DISPLAY MODE (GUI VS HEADLESS)"));

        boolean rustAvailable = ExecutionContextHardwareHelper.isRustAvailable();
        boolean gpuAvailable = false;
        boolean simdAvailable = ExecutionContextHardwareHelper.isSimdAvailable();
        HardwareMode recommended = ExecutionContextHardwareHelper.getRecommendedHardwareMode();
        String recTag = " " + I18n.getOrDefault("exec.badge.recommended", "★ [Recommended]");

        String rustTitle = I18n.getOrDefault("exec.hardware.rust_native", "🦀 Native Rust Multi-Core Engine (Rayon + AVX-512 Fused SIMD)");
        if (rustAvailable) {
            rustNativeRadio.setDisable(false);
            if (recommended == HardwareMode.NATIVE_RUST) rustTitle += recTag;
            rustNativeDescLabel.setText(I18n.getOrDefault("exec.hardware.rust_native.desc",
                    "Ultra-fast compiled native Rust kernel leveraging multi-threaded Rayon work-stealing, zero-copy buffers, and AVX-512 vectorization."));
        } else {
            rustNativeRadio.setDisable(true);
            rustTitle += " " + I18n.getOrDefault("exec.status.unavailable_rust", "(Unavailable — Native Rust DLL not found)");
            rustNativeDescLabel.setText(I18n.getOrDefault("exec.status.unavailable_rust", "(Unavailable — Native Rust DLL not found)"));
        }
        rustNativeRadio.setText(rustTitle);
        rustNativeRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.rust_native.tooltip",
                "Native compiled shared library with FFM dynamic linkage, SIMD instruction sets (AVX-512/AVX2), and lock-free parallel execution.")));

        String gpuTitle = I18n.getOrDefault("exec.hardware.gpu_shaders", "⚡ GPU Compute Shaders (OpenCL / Dedicated & Integrated GPU)");
        if (gpuAvailable) {
            gpuShadersRadio.setDisable(false);
            if (recommended == HardwareMode.GPU_SHADERS) gpuTitle += recTag;
            gpuShadersDescLabel.setText(I18n.getOrDefault("exec.hardware.gpu_shaders.desc",
                    "Hardware-accelerated OpenCL C compute kernels executing Radiative Equilibrium and Photosynthesis across GPU compute units."));
        } else {
            gpuShadersRadio.setDisable(true);
            gpuTitle += " " + I18n.getOrDefault("exec.status.unavailable_gpu", "(Unavailable — No OpenCL/TornadoVM GPU detected)");
            gpuShadersDescLabel.setText(I18n.getOrDefault("exec.status.unavailable_gpu", "(Unavailable — No OpenCL/TornadoVM GPU detected)"));
        }
        gpuShadersRadio.setText(gpuTitle);
        gpuShadersRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.gpu_shaders.tooltip",
                "Hardware-accelerated OpenCL C compute kernels executing Radiative Equilibrium and Farquhar FvCB Photosynthesis across GPU compute units.")));

        String simdTitle = I18n.getOrDefault("exec.hardware.java_vector_simd", "☕ Java 21 Incubator Vector SIMD & ForkJoin Engine");
        if (simdAvailable) {
            javaVectorSimdRadio.setDisable(false);
            if (recommended == HardwareMode.JAVA_VECTOR_SIMD) simdTitle += recTag;
            javaVectorSimdDescLabel.setText(I18n.getOrDefault("exec.hardware.java_vector_simd.desc",
                    "Direct hardware SIMD (AVX2/AVX-512 256/512-bit registers) executing parallel Java Vector API kernels with ForkJoinPool work-stealing."));
        } else {
            javaVectorSimdRadio.setDisable(true);
            simdTitle += " " + I18n.getOrDefault("exec.status.unavailable_simd", "(Unavailable — CPU SIMD Vector registers not supported)");
            javaVectorSimdDescLabel.setText(I18n.getOrDefault("exec.status.unavailable_simd", "(Unavailable — CPU SIMD Vector registers not supported)"));
        }
        javaVectorSimdRadio.setText(simdTitle);
        javaVectorSimdRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.java_vector_simd.tooltip",
                "Direct CPU vector registers via jdk.incubator.vector intrinsics and SuperWord loop unrolling.")));

        String cpuTitle = I18n.getOrDefault("exec.hardware.cpu_jit", "💻 Standard CPU Multi-Thread JIT (HotSpot Compiler)");
        if (recommended == HardwareMode.CPU_JIT) cpuTitle += recTag;
        cpuJitRadio.setText(cpuTitle);
        cpuJitRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.cpu_jit.tooltip",
                "High-throughput multi-threaded Java execution utilizing all available CPU logical cores.")));
        cpuJitDescLabel.setText(I18n.getOrDefault("exec.hardware.cpu_jit.desc",
                "Standard JVM HotSpot JIT multi-threaded loops over all detected CPU cores."));

        gpuOffRadio.setText(I18n.getOrDefault("exec.hardware.off", "🛡️ Software Safe Fallback (Single-Thread SW Rendering)"));
        gpuOffRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.off.tooltip",
                "Safe mode isolating execution from GPU drivers and hardware acceleration layers.")));
        gpuOffDescLabel.setText(I18n.getOrDefault("exec.hardware.off.desc",
                "Pure single-threaded software fallback mode with no GPU/SIMD dependencies for debugging or resource-constrained hosts."));

        if (determinismDisclaimerTitle != null) {
            determinismDisclaimerTitle.setText(I18n.getOrDefault("exec.hardware.determinism_disclaimer.title",
                    "ℹ️ Cross-Engine Determinism & Floating-Point Convergence Notice"));
        }
        if (determinismDisclaimerText != null) {
            determinismDisclaimerText.setText(I18n.getOrDefault("exec.hardware.determinism_disclaimer",
                    "Each engine operates in 100% deterministic mode from the scenario PRNG seed. However, due to low-level differences in mathematical transcendental libraries (JVM FDLIBM vs LLVM/libm), FMA contractions, and SIMD reduction ordering, trajectories between Java Standard, Java SIMD, and Native Rust are analytically convergent but exhibit minor floating-point mantissa variances (~10⁻⁵ to 10⁻²). Run identical engines for exact bit-for-bit repeatability."));
        }

        localTopologyRadio.setText(I18n.getOrDefault("exec.topology.local", "🏢 Local Standalone Mode (Host Machine Cores & Memory)"));
        localTopologyRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.topology.local.tooltip",
                "Executes entire planetary simulation locally on the workstation.")));
        localTopologyDescLabel.setText(I18n.getOrDefault("exec.topology.local.desc", "Simulation executes entirely on local computer."));

        clusterTopologyRadio.setText(I18n.getOrDefault("exec.topology.cluster", "🌐 Distributed Cluster Mode (gRPC/TCP Multi-Machine Nodes)"));
        clusterTopologyRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.topology.cluster.tooltip",
                "Partition H3 spatial grids and compute workloads across network-connected cluster nodes.")));
        clusterTopologyDescLabel.setText(I18n.getOrDefault("exec.topology.cluster.desc",
                "Distributes H3 grid and computations across multiple machines connected on local network or cloud."));

        if (dbSectionHeader != null) {
            dbSectionHeader.setText(I18n.getOrDefault("exec.topology.db_title", "Database & Persistence Infrastructure:"));
        }
        updateDatabaseStatus(dbStatusLabel);

        if (clusterSection != null) clusterSection.updateTexts();

        guiRenderingRadio.setText(I18n.getOrDefault("exec.rendering.gui", "🖼️ Interactive GUI Mode (Real-Time JavaFX Visual)"));
        guiRenderingRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.rendering.gui.tooltip",
                "Full real-time 2D/3D visual rendering with live charts and maps.")));
        guiRenderingDescLabel.setText(I18n.getOrDefault("exec.rendering.gui.desc",
                "Dynamic 2D/3D cartographic rendering. Note: Graphical pipeline consumes 30% to 60% of CPU/GPU cycles."));

        headlessRenderingRadio.setText(I18n.getOrDefault("exec.rendering.headless", "🚀 Headless Mode (Async Background - High Throughput Batch)"));
        headlessRenderingRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.rendering.headless.tooltip",
                "Run purely in compute background without GUI rendering overhead.")));
        headlessRenderingDescLabel.setText(I18n.getOrDefault("exec.rendering.headless.desc",
                "Disables visual rendering. Performance gain: Maximum compute throughput (+300% to +1000%)."));

        lblTargetYear.setText(I18n.getOrDefault("exec.headless.target_year", "Target Simulation End Year:"));
        targetYearSpinner.setTooltip(new Tooltip(I18n.getOrDefault("exec.headless.target_year.tooltip",
                "Simulation halts automatically upon reaching this target year.")));
        lblTargetYearNote.setText(I18n.getOrDefault("exec.headless.target_year_note",
                "ℹ️ Note: The actual number of computed steps depends on the time step duration (Δt) configured in the scenario."));

        if (sidebar != null) sidebar.updateTexts();
    }
}
