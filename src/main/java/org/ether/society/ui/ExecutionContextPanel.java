/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.ether.society.i18n.I18n;
import org.ether.society.network.ClusterManager;
import org.ether.society.ui.execution.ExecutionContextClusterSection;
import org.ether.society.ui.execution.ExecutionContextHardwareHelper;
import org.ether.society.ui.execution.ExecutionContextI18nHelper;
import org.ether.society.ui.execution.ExecutionContextSidebar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * Execution Context & Infrastructure UI Panel (Tab 6).
 * Manages compute hardware acceleration (Native Rust Core, GPU Compute Shaders, Java 21 SIMD, CPU JIT, Software Safe Fallback),
 * execution topology (Local vs Distributed Cluster, DB Persistence), and rendering mode (GUI vs Headless Batch).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ExecutionContextPanel extends BorderPane {

    static final Logger logger = LoggerFactory.getLogger(ExecutionContextPanel.class);
    static final Preferences prefs = Preferences.userNodeForPackage(PreferencesPanel.class);
    static final String PREF_HARDWARE_MODE_KEY = "ether_hardware_mode";
    static final String PREF_GPU_KEY = "ether_gpu_enabled";

    public enum ExecutionMode {
        RUST_NATIVE, GPU, CPU_SIMD, CPU, CLUSTER, HEADLESS
    }

    public enum HardwareMode {
        NATIVE_RUST, GPU_SHADERS, JAVA_VECTOR_SIMD, CPU_JIT, GPU_OFF
    }

    public enum ExecutionTopology {
        LOCAL, CLUSTER
    }

    public enum RenderingMode {
        GUI, HEADLESS
    }

    /** Observable row model for cluster compute nodes. */
    public static class ClusterNode {
        private final StringProperty id;
        private final StringProperty host;
        private final StringProperty role;
        private final StringProperty status;
        private final StringProperty capacity;
        private final StringProperty chunks;

        public ClusterNode(String id, String host, String role, String status, String capacity, String chunks) {
            this.id = new SimpleStringProperty(id);
            this.host = new SimpleStringProperty(host);
            this.role = new SimpleStringProperty(role);
            this.status = new SimpleStringProperty(status);
            this.capacity = new SimpleStringProperty(capacity);
            this.chunks = new SimpleStringProperty(chunks);
        }

        public StringProperty idProperty() { return id; }
        public StringProperty hostProperty() { return host; }
        public StringProperty roleProperty() { return role; }
        public StringProperty statusProperty() { return status; }
        public StringProperty capacityProperty() { return capacity; }
        public StringProperty chunksProperty() { return chunks; }

        public String getId() { return id.get(); }
        public String getHost() { return host.get(); }
        public String getRole() { return role.get(); }
        public String getStatus() { return status.get(); }
        public String getCapacity() { return capacity.get(); }
        public String getChunks() { return chunks.get(); }
    }

    public static boolean isRustAvailable() {
        return ExecutionContextHardwareHelper.isRustAvailable();
    }

    public static boolean isSimdAvailable() {
        return ExecutionContextHardwareHelper.isSimdAvailable();
    }

    public static HardwareMode getRecommendedHardwareMode() {
        return ExecutionContextHardwareHelper.getRecommendedHardwareMode();
    }

    public static HardwareMode getActiveHardwareMode() {
        return ExecutionContextHardwareHelper.getActiveHardwareMode();
    }

    public static double getEstimatedCellTicksThroughput(HardwareMode mode) {
        return ExecutionContextHardwareHelper.getEstimatedCellTicksThroughput(mode);
    }

    public static double estimateExecutionTimeSeconds(long startYear, long endYear, int cellCount, HardwareMode mode) {
        return ExecutionContextHardwareHelper.estimateExecutionTimeSeconds(startYear, endYear, cellCount, mode);
    }

    public static String formatDuration(double seconds) {
        return ExecutionContextHardwareHelper.formatDuration(seconds);
    }

    public static String getDetectedGpuName() {
        return ExecutionContextHardwareHelper.getDetectedGpuName();
    }

    @SuppressWarnings("unused")
    private final Runnable onLaunchSimulationCallback;

    // 1. Hardware Engine Controls
    Label hardwareSectionHeader;
    ToggleGroup hardwareGroup;
    RadioButton rustNativeRadio;
    RadioButton gpuShadersRadio;
    RadioButton javaVectorSimdRadio;
    RadioButton cpuJitRadio;
    RadioButton gpuOffRadio;

    Label rustNativeDescLabel;
    Label gpuShadersDescLabel;
    Label javaVectorSimdDescLabel;
    Label cpuJitDescLabel;
    Label gpuOffDescLabel;
    VBox determinismDisclaimerCard;
    Label determinismDisclaimerTitle;
    Label determinismDisclaimerText;

    // 2. Execution Topology Controls
    Label topologySectionHeader;
    ToggleGroup topologyGroup;
    RadioButton localTopologyRadio;
    RadioButton clusterTopologyRadio;
    Label localTopologyDescLabel;
    Label clusterTopologyDescLabel;
    Label dbSectionHeader;
    Label dbStatusLabel;
    ExecutionContextClusterSection clusterSection;

    // 3. Rendering / Display Mode Controls
    Label renderingSectionHeader;
    ToggleGroup renderingGroup;
    RadioButton guiRenderingRadio;
    RadioButton headlessRenderingRadio;
    Label guiRenderingDescLabel;
    Label headlessRenderingDescLabel;

    VBox headlessConfigCard;
    Label lblTargetYear;
    Label lblTargetYearNote;
    Spinner<Integer> targetYearSpinner;

    // 4. Right Sidebar Summary & Hardware Specifications
    Label titleHeader;
    Label liveBannerLabel;
    private java.util.function.Consumer<HardwareMode> onLiveConfigChangedCallback;
    private java.util.function.Consumer<RenderingMode> onLiveRenderingModeChangedCallback;
    ExecutionContextSidebar sidebar;

    public ExecutionContextPanel() {
        this(null);
    }

    public ExecutionContextPanel(Runnable onLaunchSimulationCallback) {
        this.onLaunchSimulationCallback = onLaunchSimulationCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        ExecutionContextViewBuilder.initUI(this);
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());

        Timeline autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (clusterSection != null && (clusterSection.isMasterRunning() || clusterSection.isConnectedCluster())) {
                clusterSection.refreshNodes();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    void updateRightSummary() {
        if (sidebar == null) return;
        HardwareMode hw = getHardwareMode();
        ExecutionTopology top = getExecutionTopology();
        RenderingMode ren = getRenderingMode();
        boolean isMaster = clusterSection != null && clusterSection.isMasterRunning();
        boolean isConnected = clusterSection != null && clusterSection.isConnectedCluster();
        int targetYear = getTargetYear();
        var nodes = clusterSection != null ? clusterSection.getNodeList() : null;

        sidebar.updateRightSummary(hw, top, ren, isMaster, isConnected, targetYear, nodes);
    }

    public ClusterManager getClusterManager() {
        return clusterSection != null ? clusterSection.getClusterManager() : null;
    }

    public HardwareMode getHardwareMode() {
        if (rustNativeRadio != null && rustNativeRadio.isSelected() && !rustNativeRadio.isDisabled()) return HardwareMode.NATIVE_RUST;
        if (gpuShadersRadio != null && gpuShadersRadio.isSelected() && !gpuShadersRadio.isDisabled()) return HardwareMode.GPU_SHADERS;
        if (javaVectorSimdRadio != null && javaVectorSimdRadio.isSelected() && !javaVectorSimdRadio.isDisabled()) return HardwareMode.JAVA_VECTOR_SIMD;
        if (gpuOffRadio != null && gpuOffRadio.isSelected()) return HardwareMode.GPU_OFF;
        return HardwareMode.CPU_JIT;
    }

    public ExecutionTopology getExecutionTopology() {
        if (clusterTopologyRadio != null && clusterTopologyRadio.isSelected()) return ExecutionTopology.CLUSTER;
        return ExecutionTopology.LOCAL;
    }

    public RenderingMode getRenderingMode() {
        if (headlessRenderingRadio != null && headlessRenderingRadio.isSelected()) return RenderingMode.HEADLESS;
        return RenderingMode.GUI;
    }

    public int getTargetYear() {
        return (targetYearSpinner != null && targetYearSpinner.getValue() != null) ? targetYearSpinner.getValue() : 2100;
    }

    public ExecutionMode getCurrentMode() {
        if (getRenderingMode() == RenderingMode.HEADLESS) return ExecutionMode.HEADLESS;
        if (getExecutionTopology() == ExecutionTopology.CLUSTER) return ExecutionMode.CLUSTER;
        return switch (getHardwareMode()) {
            case NATIVE_RUST -> ExecutionMode.RUST_NATIVE;
            case GPU_SHADERS -> ExecutionMode.GPU;
            case JAVA_VECTOR_SIMD -> ExecutionMode.CPU_SIMD;
            case CPU_JIT, GPU_OFF -> ExecutionMode.CPU;
        };
    }

    public void setMode(ExecutionMode mode) {
        ExecutionContextViewBuilder.applyExecutionMode(this, mode);
    }

    public void setRenderingMode(RenderingMode mode) {
        ExecutionContextViewBuilder.applyRenderingMode(this, mode);
    }

    public void setOnLiveConfigChangedCallback(java.util.function.Consumer<HardwareMode> callback) {
        this.onLiveConfigChangedCallback = callback;
    }

    public void setOnLiveRenderingModeChangedCallback(java.util.function.Consumer<RenderingMode> callback) {
        this.onLiveRenderingModeChangedCallback = callback;
    }

    void notifyLiveConfigChange(HardwareMode mode) {
        if (onLiveConfigChangedCallback != null) {
            onLiveConfigChangedCallback.accept(mode);
        }
    }

    void notifyLiveRenderingModeChange(RenderingMode mode) {
        if (onLiveRenderingModeChangedCallback != null) {
            onLiveRenderingModeChangedCallback.accept(mode);
        }
    }

    public void updateTexts() {
        ExecutionContextI18nHelper.updatePanelTexts(
                titleHeader, liveBannerLabel,
                hardwareSectionHeader, topologySectionHeader, renderingSectionHeader,
                rustNativeRadio, rustNativeDescLabel,
                gpuShadersRadio, gpuShadersDescLabel,
                javaVectorSimdRadio, javaVectorSimdDescLabel,
                cpuJitRadio, cpuJitDescLabel,
                gpuOffRadio, gpuOffDescLabel,
                determinismDisclaimerTitle, determinismDisclaimerText,
                localTopologyRadio, localTopologyDescLabel,
                clusterTopologyRadio, clusterTopologyDescLabel,
                dbSectionHeader, dbStatusLabel, clusterSection,
                guiRenderingRadio, guiRenderingDescLabel,
                headlessRenderingRadio, headlessRenderingDescLabel,
                lblTargetYear, targetYearSpinner, lblTargetYearNote,
                sidebar);
        updateRightSummary();
    }
}
