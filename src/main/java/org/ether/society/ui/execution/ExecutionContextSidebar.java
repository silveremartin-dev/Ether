/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.execution;

import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.ExecutionContextPanel.ClusterNode;
import org.ether.society.ui.ExecutionContextPanel.ExecutionTopology;
import org.ether.society.ui.ExecutionContextPanel.HardwareMode;
import org.ether.society.ui.ExecutionContextPanel.RenderingMode;

/**
 * Right sidebar controller and UI view for the Execution Context Panel.
 * <p>
 * Displays live host system hardware specifications (OS, logical cores, maximum heap memory,
 * detected GPU) and real-time execution configuration summaries.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class ExecutionContextSidebar extends VBox {

    private Label sidebarSysInfoTitle;
    private Label sysOsHeader;
    private Label sysOsValue;
    private Label sysCpuHeader;
    private Label sysCpuValue;
    private Label sysRamHeader;
    private Label sysRamValue;
    private Label sysGpuHeader;
    private Label sysGpuValue;

    private Label sidebarTitleLabel;
    private Label hdrEngineLabel;
    private Label summaryHardwareLabel;
    private Label hdrTopologyLabel;
    private Label summaryTopologyLabel;
    private Label hdrRenderingLabel;
    private Label summaryRenderingLabel;
    private Label hdrClusterLabel;
    private Label summaryClusterNodesLabel;

    public ExecutionContextSidebar() {
        super(14);
        setPrefWidth(300);
        setMinWidth(290);
        setMaxWidth(320);
        getStyleClass().add("sidebar-card");

        initUI();
        updateTexts();
    }

    private void initUI() {
        sidebarSysInfoTitle = new Label();
        sidebarSysInfoTitle.getStyleClass().add("sidebar-title");

        sysOsHeader = createSmallHeader("");
        sysOsValue = createSysInfoLabel();
        sysCpuHeader = createSmallHeader("");
        sysCpuValue = createSysInfoLabel();
        sysRamHeader = createSmallHeader("");
        sysRamValue = createSysInfoLabel();
        sysGpuHeader = createSmallHeader("");
        sysGpuValue = createSysInfoLabel();

        VBox sysInfoBox = new VBox(6,
                sysOsHeader, sysOsValue,
                sysCpuHeader, sysCpuValue,
                sysRamHeader, sysRamValue,
                sysGpuHeader, sysGpuValue
        );
        sysInfoBox.getStyleClass().add("sidebar-recap-box");

        sidebarTitleLabel = new Label();
        sidebarTitleLabel.getStyleClass().add("sidebar-title");

        hdrEngineLabel = createSmallHeader("");
        summaryHardwareLabel = new Label();
        summaryHardwareLabel.getStyleClass().add("sidebar-recap-text");
        summaryHardwareLabel.setWrapText(true);

        hdrTopologyLabel = createSmallHeader("");
        summaryTopologyLabel = new Label();
        summaryTopologyLabel.getStyleClass().add("sidebar-recap-text");
        summaryTopologyLabel.setWrapText(true);

        hdrRenderingLabel = createSmallHeader("");
        summaryRenderingLabel = new Label();
        summaryRenderingLabel.getStyleClass().add("sidebar-recap-text");
        summaryRenderingLabel.setWrapText(true);

        hdrClusterLabel = createSmallHeader("");
        summaryClusterNodesLabel = new Label();
        summaryClusterNodesLabel.getStyleClass().addAll("sidebar-recap-text", "sidebar-recap-highlight");
        summaryClusterNodesLabel.setWrapText(true);

        VBox recapBox = new VBox(8,
                hdrEngineLabel, summaryHardwareLabel,
                hdrTopologyLabel, summaryTopologyLabel,
                hdrRenderingLabel, summaryRenderingLabel,
                hdrClusterLabel, summaryClusterNodesLabel
        );
        recapBox.getStyleClass().add("sidebar-recap-box");

        getChildren().addAll(sidebarSysInfoTitle, sysInfoBox, new Separator(), sidebarTitleLabel, recapBox);
        updateSystemSpecs();
    }

    private Label createSmallHeader(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("sidebar-recap-header");
        return lbl;
    }

    private Label createSysInfoLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px; -fx-padding: 0 0 4 4;");
        lbl.setWrapText(true);
        return lbl;
    }

    public void updateSystemSpecs() {
        int cpus = Runtime.getRuntime().availableProcessors();
        long maxMemMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        String osName = System.getProperty("os.name", "Unknown OS");
        String osArch = System.getProperty("os.arch", "x64");
        String gpuName = ExecutionContextHardwareHelper.getDetectedGpuName();

        if (sysOsValue != null) sysOsValue.setText(osName + " (" + osArch + ")");
        if (sysCpuValue != null) sysCpuValue.setText(cpus + " " + I18n.getOrDefault("exec.summary.cores_detected", "Cores Detected"));
        if (sysRamValue != null) sysRamValue.setText(String.format("%,d MB", maxMemMB));
        if (sysGpuValue != null) sysGpuValue.setText(gpuName);
    }

    public void updateRightSummary(HardwareMode hw, ExecutionTopology top, RenderingMode ren,
                                   boolean isMasterRunning, boolean isConnectedCluster,
                                   int targetYear, ObservableList<ClusterNode> nodeList) {
        if (summaryHardwareLabel == null) return;

        switch (hw) {
            case NATIVE_RUST -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.rust_native", "Native Rust Core Engine") + "\n(" + I18n.getOrDefault("exec.summary.rust_sub", "Rayon + AVX-512 Fused SIMD") + ")");
            case GPU_SHADERS -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.gpu_shaders", "GPU Compute Shaders (OpenCL)") + "\n(" + ExecutionContextHardwareHelper.getDetectedGpuName() + ")");
            case JAVA_VECTOR_SIMD -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.java_vector_simd", "Java 21 Incubator Vector SIMD") + "\n(" + Runtime.getRuntime().availableProcessors() + " " + I18n.getOrDefault("exec.summary.cores_detected", "Cores Detected") + ")");
            case CPU_JIT -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.cpu_jit", "CPU Multi-Thread JIT") + "\n(" + Runtime.getRuntime().availableProcessors() + " " + I18n.getOrDefault("exec.summary.cores_detected", "Cores Detected") + ")");
            case GPU_OFF -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.gpu_off", "Single-Thread SW Safe Fallback"));
        }

        if (top == ExecutionTopology.CLUSTER) {
            String roleStr = isMasterRunning ? I18n.getOrDefault("exec.summary.master_active", "Master Server Active (Port 9090)") : (isConnectedCluster ? I18n.getOrDefault("exec.summary.worker_connected", "Worker Connected") : I18n.getOrDefault("exec.summary.cluster_configured", "Cluster Configured (Pending)"));
            summaryTopologyLabel.setText("• " + I18n.getOrDefault("exec.summary.cluster_mode", "gRPC Distributed Cluster Mode") + "\n(" + roleStr + ")");
        } else {
            summaryTopologyLabel.setText("• " + I18n.getOrDefault("exec.summary.local_mode", "Local Standalone Mode") + "\n(" + I18n.getOrDefault("exec.summary.standalone_host", "Standalone Host Machine") + ")");
        }

        if (ren == RenderingMode.HEADLESS) {
            summaryRenderingLabel.setText("• " + I18n.getOrDefault("exec.summary.headless_mode", "Headless Batch Mode") + "\n(" + I18n.getOrDefault("exec.headless.target_year", "Target Year:") + " " + targetYear + ")");
        } else {
            summaryRenderingLabel.setText("• " + I18n.getOrDefault("exec.summary.gui_mode", "Interactive GUI Mode") + "\n(" + I18n.getOrDefault("exec.summary.realtime_2d3d", "Real-Time 2D/3D Visual") + ")");
        }

        if (nodeList != null && !nodeList.isEmpty()) {
            summaryClusterNodesLabel.setText("• " + nodeList.size() + " " + I18n.getOrDefault("exec.summary.nodes_registered", "Registered Node(s)"));
        } else {
            summaryClusterNodesLabel.setText("• 1 " + I18n.getOrDefault("exec.summary.single_node", "Local Node (Standalone)"));
        }
    }

    public void updateTexts() {
        if (sidebarSysInfoTitle != null) sidebarSysInfoTitle.setText(I18n.getOrDefault("exec.sidebar.sysinfo_title", "💻 SYSTEM SPECIFICATIONS"));
        if (sysOsHeader != null) sysOsHeader.setText(I18n.getOrDefault("exec.sidebar.sys_os", "OS & Architecture:"));
        if (sysCpuHeader != null) sysCpuHeader.setText(I18n.getOrDefault("exec.sidebar.sys_cpu", "CPU Logical Cores:"));
        if (sysRamHeader != null) sysRamHeader.setText(I18n.getOrDefault("exec.sidebar.sys_ram", "Max Heap Memory:"));
        if (sysGpuHeader != null) sysGpuHeader.setText(I18n.getOrDefault("exec.sidebar.sys_gpu", "Detected GPU:"));

        if (sidebarTitleLabel != null) sidebarTitleLabel.setText(I18n.getOrDefault("exec.sidebar.title", "🚀 LIVE RECAP"));
        if (hdrEngineLabel != null) hdrEngineLabel.setText(I18n.getOrDefault("exec.sidebar.header.engine", "🖥️ Compute Engine:"));
        if (hdrTopologyLabel != null) hdrTopologyLabel.setText(I18n.getOrDefault("exec.sidebar.header.topology", "🌐 Network Topology:"));
        if (hdrRenderingLabel != null) hdrRenderingLabel.setText(I18n.getOrDefault("exec.sidebar.header.rendering", "🖼️ Visual Rendering:"));
        if (hdrClusterLabel != null) hdrClusterLabel.setText(I18n.getOrDefault("exec.sidebar.header.cluster", "📊 Cluster Nodes:"));

        updateSystemSpecs();
    }
}
