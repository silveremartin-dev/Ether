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
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.ether.society.database.DatabaseConfig;
import org.ether.society.i18n.I18n;
import org.ether.society.network.ClusterManager;
import org.ether.society.ui.execution.ExecutionContextClusterSection;
import org.ether.society.ui.execution.ExecutionContextHardwareHelper;
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

    private static final Logger logger = LoggerFactory.getLogger(ExecutionContextPanel.class);
    private static final Preferences prefs = Preferences.userNodeForPackage(PreferencesPanel.class);
    private static final String PREF_HARDWARE_MODE_KEY = "ether_hardware_mode";
    private static final String PREF_GPU_KEY = "ether_gpu_enabled";

    public enum ExecutionMode {
        RUST_NATIVE,
        GPU,
        CPU_SIMD,
        CPU,
        CLUSTER,
        HEADLESS
    }

    public enum HardwareMode {
        NATIVE_RUST,
        GPU_SHADERS,
        JAVA_VECTOR_SIMD,
        CPU_JIT,
        GPU_OFF
    }

    public enum ExecutionTopology {
        LOCAL,
        CLUSTER
    }

    public enum RenderingMode {
        GUI,
        HEADLESS
    }

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

    private final Runnable onLaunchSimulationCallback;

    // 1. Hardware Engine Controls
    private Label hardwareSectionHeader;
    private ToggleGroup hardwareGroup;
    private RadioButton rustNativeRadio;
    private RadioButton gpuShadersRadio;
    private RadioButton javaVectorSimdRadio;
    private RadioButton cpuJitRadio;
    private RadioButton gpuOffRadio;

    private Label rustNativeDescLabel;
    private Label gpuShadersDescLabel;
    private Label javaVectorSimdDescLabel;
    private Label cpuJitDescLabel;
    private Label gpuOffDescLabel;
    private VBox determinismDisclaimerCard;
    private Label determinismDisclaimerTitle;
    private Label determinismDisclaimerText;

    // 2. Execution Topology Controls
    private Label topologySectionHeader;
    private ToggleGroup topologyGroup;
    private RadioButton localTopologyRadio;
    private RadioButton clusterTopologyRadio;
    private Label localTopologyDescLabel;
    private Label clusterTopologyDescLabel;
    private Label dbSectionHeader;
    private Label dbStatusLabel;

    // Cluster Config Section
    private ExecutionContextClusterSection clusterSection;

    // 3. Rendering / Display Mode Controls
    private Label renderingSectionHeader;
    private ToggleGroup renderingGroup;
    private RadioButton guiRenderingRadio;
    private RadioButton headlessRenderingRadio;
    private Label guiRenderingDescLabel;
    private Label headlessRenderingDescLabel;

    // Headless Config Section
    private VBox headlessConfigCard;
    private Label lblTargetYear;
    private Label lblTargetYearNote;
    private Spinner<Integer> targetYearSpinner;

    // 4. Right Sidebar Summary & Hardware Specifications
    private Label titleHeader;
    private Label liveBannerLabel;
    private java.util.function.Consumer<HardwareMode> onLiveConfigChangedCallback;
    private java.util.function.Consumer<RenderingMode> onLiveRenderingModeChangedCallback;
    private ExecutionContextSidebar sidebar;

    public ExecutionContextPanel() {
        this(null);
    }

    public ExecutionContextPanel(Runnable onLaunchSimulationCallback) {
        this.onLaunchSimulationCallback = onLaunchSimulationCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());

        // Automatic 3-second background polling for live node discovery & health check
        Timeline autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (clusterSection != null && (clusterSection.isMasterRunning() || clusterSection.isConnectedCluster())) {
                clusterSection.refreshNodes();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void initUI() {
        HBox mainLayout = new HBox(20);
        mainLayout.setAlignment(Pos.TOP_LEFT);

        VBox leftColumn = new VBox(20);
        leftColumn.setMaxWidth(800);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        titleHeader = new Label();
        titleHeader.getStyleClass().add("label-title");

        liveBannerLabel = new Label();
        liveBannerLabel.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-border-color: #38bdf8; -fx-border-radius: 6; -fx-border-width: 1;");
        liveBannerLabel.setWrapText(true);

        // --- SECTION 1: Hardware Compute Engine ---
        hardwareSectionHeader = createSectionHeader("");
        hardwareGroup = new ToggleGroup();

        rustNativeRadio = new RadioButton();
        gpuShadersRadio = new RadioButton();
        javaVectorSimdRadio = new RadioButton();
        cpuJitRadio = new RadioButton();
        gpuOffRadio = new RadioButton();

        rustNativeRadio.setToggleGroup(hardwareGroup);
        gpuShadersRadio.setToggleGroup(hardwareGroup);
        javaVectorSimdRadio.setToggleGroup(hardwareGroup);
        cpuJitRadio.setToggleGroup(hardwareGroup);
        gpuOffRadio.setToggleGroup(hardwareGroup);

        rustNativeDescLabel = createDescLabel();
        gpuShadersDescLabel = createDescLabel();
        javaVectorSimdDescLabel = createDescLabel();
        cpuJitDescLabel = createDescLabel();
        gpuOffDescLabel = createDescLabel();

        // Hardware Availability Check
        boolean rustAvailable = isRustAvailable();
        boolean gpuAvailable = false;
        boolean simdAvailable = isSimdAvailable();

        rustNativeRadio.setDisable(!rustAvailable);
        gpuShadersRadio.setDisable(!gpuAvailable);
        javaVectorSimdRadio.setDisable(!simdAvailable);
        cpuJitRadio.setDisable(false);
        gpuOffRadio.setDisable(false);

        HardwareMode activeMode = getActiveHardwareMode();
        switch (activeMode) {
            case NATIVE_RUST -> rustNativeRadio.setSelected(true);
            case GPU_SHADERS -> gpuShadersRadio.setSelected(true);
            case JAVA_VECTOR_SIMD -> javaVectorSimdRadio.setSelected(true);
            case CPU_JIT -> cpuJitRadio.setSelected(true);
            case GPU_OFF -> gpuOffRadio.setSelected(true);
        }

        rustNativeRadio.setOnAction(e -> {
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.NATIVE_RUST.name());
            prefs.putBoolean(PREF_GPU_KEY, false);
            logger.info("Hardware acceleration mode set to NATIVE RUST (Rayon + AVX-512)");
            updateRightSummary();
            notifyLiveConfigChange(HardwareMode.NATIVE_RUST);
        });

        gpuShadersRadio.setOnAction(e -> {
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.GPU_SHADERS.name());
            prefs.putBoolean(PREF_GPU_KEY, true);
            logger.info("Hardware acceleration mode set to GPU COMPUTE SHADERS (OpenCL)");
            updateRightSummary();
            notifyLiveConfigChange(HardwareMode.GPU_SHADERS);
        });

        javaVectorSimdRadio.setOnAction(e -> {
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.JAVA_VECTOR_SIMD.name());
            prefs.putBoolean(PREF_GPU_KEY, false);
            logger.info("Hardware acceleration mode set to JAVA 21 VECTOR SIMD");
            updateRightSummary();
            notifyLiveConfigChange(HardwareMode.JAVA_VECTOR_SIMD);
        });

        cpuJitRadio.setOnAction(e -> {
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.CPU_JIT.name());
            prefs.putBoolean(PREF_GPU_KEY, false);
            logger.info("Hardware acceleration mode set to CPU JIT");
            updateRightSummary();
            notifyLiveConfigChange(HardwareMode.CPU_JIT);
        });

        gpuOffRadio.setOnAction(e -> {
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.GPU_OFF.name());
            prefs.putBoolean(PREF_GPU_KEY, false);
            logger.info("Hardware acceleration mode set to GPU OFF (Software Prism Safe Fallback)");
            updateRightSummary();
            notifyLiveConfigChange(HardwareMode.GPU_OFF);
        });

        VBox rustNativeCard = new VBox(6, rustNativeRadio, rustNativeDescLabel);
        rustNativeCard.getStyleClass().add("card-section");

        VBox gpuShadersCard = new VBox(6, gpuShadersRadio, gpuShadersDescLabel);
        gpuShadersCard.getStyleClass().add("card-section");

        VBox javaVectorSimdCard = new VBox(6, javaVectorSimdRadio, javaVectorSimdDescLabel);
        javaVectorSimdCard.getStyleClass().add("card-section");

        VBox cpuJitCard = new VBox(6, cpuJitRadio, cpuJitDescLabel);
        cpuJitCard.getStyleClass().add("card-section");

        VBox gpuOffCard = new VBox(6, gpuOffRadio, gpuOffDescLabel);
        gpuOffCard.getStyleClass().add("card-section");

        determinismDisclaimerTitle = new Label();
        determinismDisclaimerTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 12px;");
        determinismDisclaimerText = new Label();
        determinismDisclaimerText.setWrapText(true);
        determinismDisclaimerText.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-line-spacing: 2px;");
        determinismDisclaimerCard = new VBox(6, determinismDisclaimerTitle, determinismDisclaimerText);
        determinismDisclaimerCard.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-border-color: rgba(56, 189, 248, 0.4); -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10 14;");

        VBox hardwareSection = createCardSection(hardwareSectionHeader, new VBox(10, rustNativeCard, gpuShadersCard, javaVectorSimdCard, cpuJitCard, gpuOffCard, determinismDisclaimerCard));

        // --- SECTION 2: Execution Topology & Persistence ---
        topologySectionHeader = createSectionHeader("");
        topologyGroup = new ToggleGroup();

        localTopologyRadio = new RadioButton();
        clusterTopologyRadio = new RadioButton();
        localTopologyRadio.setToggleGroup(topologyGroup);
        clusterTopologyRadio.setToggleGroup(topologyGroup);
        localTopologyRadio.setSelected(true);

        localTopologyDescLabel = createDescLabel();
        clusterTopologyDescLabel = createDescLabel();

        dbSectionHeader = createSmallHeader("");
        dbStatusLabel = new Label();
        dbStatusLabel.getStyleClass().add("info-badge");
        dbStatusLabel.setWrapText(true);

        VBox dbStatusBox = new VBox(6, dbSectionHeader, dbStatusLabel);
        dbStatusBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-padding: 10 12; -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 6;");

        VBox localCard = new VBox(8, localTopologyRadio, localTopologyDescLabel, dbStatusBox);
        localCard.getStyleClass().add("card-section");

        clusterSection = new ExecutionContextClusterSection(this::updateRightSummary);

        VBox clusterCard = new VBox(8, clusterTopologyRadio, clusterTopologyDescLabel, clusterSection);
        clusterCard.getStyleClass().add("card-section");

        localTopologyRadio.setOnAction(e -> {
            clusterSection.setVisible(false);
            clusterSection.setManaged(false);
            updateRightSummary();
        });

        clusterTopologyRadio.setOnAction(e -> {
            clusterSection.setVisible(true);
            clusterSection.setManaged(true);
            updateRightSummary();
        });

        VBox topologySection = createCardSection(topologySectionHeader, new VBox(10, localCard, clusterCard));

        // --- SECTION 3: Rendering & Output Mode ---
        renderingSectionHeader = createSectionHeader("");
        renderingGroup = new ToggleGroup();

        guiRenderingRadio = new RadioButton();
        headlessRenderingRadio = new RadioButton();
        guiRenderingRadio.setToggleGroup(renderingGroup);
        headlessRenderingRadio.setToggleGroup(renderingGroup);
        guiRenderingRadio.setSelected(true);

        guiRenderingDescLabel = createDescLabel();
        headlessRenderingDescLabel = createDescLabel();

        VBox guiCard = new VBox(6, guiRenderingRadio, guiRenderingDescLabel);
        guiCard.getStyleClass().add("card-section");

        targetYearSpinner = new Spinner<>(-100_000, 10_000, 2100, 50);
        targetYearSpinner.setEditable(true);
        targetYearSpinner.setPrefWidth(140);
        targetYearSpinner.valueProperty().addListener((obs, oldV, newV) -> updateRightSummary());

        lblTargetYear = new Label();
        lblTargetYear.getStyleClass().add("control-label");

        lblTargetYearNote = new Label();
        lblTargetYearNote.getStyleClass().add("hint-label");
        lblTargetYearNote.setWrapText(true);

        HBox targetYearRow = new HBox(12, lblTargetYear, targetYearSpinner);
        targetYearRow.setAlignment(Pos.CENTER_LEFT);

        headlessConfigCard = new VBox(8, targetYearRow, lblTargetYearNote);
        headlessConfigCard.getStyleClass().add("subcard-section");
        headlessConfigCard.setVisible(false);
        headlessConfigCard.setManaged(false);

        VBox headlessCard = new VBox(8, headlessRenderingRadio, headlessRenderingDescLabel, headlessConfigCard);
        headlessCard.getStyleClass().add("card-section");

        guiRenderingRadio.setOnAction(e -> {
            headlessConfigCard.setVisible(false);
            headlessConfigCard.setManaged(false);
            updateRightSummary();
            notifyLiveRenderingModeChange(RenderingMode.GUI);
        });

        headlessRenderingRadio.setOnAction(e -> {
            headlessConfigCard.setVisible(true);
            headlessConfigCard.setManaged(true);
            updateRightSummary();
            notifyLiveRenderingModeChange(RenderingMode.HEADLESS);
        });

        VBox renderingSection = createCardSection(renderingSectionHeader, new VBox(10, guiCard, headlessCard));

        leftColumn.getChildren().addAll(titleHeader, liveBannerLabel, hardwareSection, topologySection, renderingSection);

        // --- RIGHT COLUMN: Sidebar ---
        sidebar = new ExecutionContextSidebar();

        updateDatabaseStatus();
        updateRightSummary();

        mainLayout.getChildren().addAll(leftColumn, sidebar);

        ScrollPane scroll = new ScrollPane(mainLayout);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        setCenter(scroll);
    }

    private Label createSmallHeader(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("sidebar-recap-header");
        return lbl;
    }

    private Label createDescLabel() {
        Label lbl = new Label();
        lbl.setWrapText(true);
        lbl.getStyleClass().add("hint-label");
        return lbl;
    }

    private Label createSectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("label-section-header");
        return lbl;
    }

    private VBox createCardSection(Label header, VBox content) {
        VBox card = new VBox(10, header, content);
        card.getStyleClass().add("card-section");
        return card;
    }

    private void updateDatabaseStatus() {
        if (dbStatusLabel == null) return;
        try {
            var emf = DatabaseConfig.getEntityManagerFactory();
            if (emf != null && emf.isOpen()) {
                dbStatusLabel.setText(I18n.getOrDefault("exec.topology.db_status_ok", "🟢 Base de données : Connectée & Opérationnelle (Persistance active)"));
                dbStatusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                return;
            }
        } catch (Throwable t) {
            String msg = I18n.getOrDefault("exec.topology.db_error_prefix", "🔴 Diagnostic base : {0}", t.getMessage());
            dbStatusLabel.setText(msg);
            dbStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            return;
        }
        dbStatusLabel.setText(I18n.getOrDefault("exec.topology.db_status_offline", "🟡 Base de données : Mode autonome en mémoire (WorldBuffer non persisté sur serveur distant)"));
        dbStatusLabel.setStyle("-fx-text-fill: #f59e0b;");
    }

    private void updateRightSummary() {
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
        HardwareMode hw = getHardwareMode();
        return switch (hw) {
            case NATIVE_RUST -> ExecutionMode.RUST_NATIVE;
            case GPU_SHADERS -> ExecutionMode.GPU;
            case JAVA_VECTOR_SIMD -> ExecutionMode.CPU_SIMD;
            case CPU_JIT, GPU_OFF -> ExecutionMode.CPU;
        };
    }

    public void setMode(ExecutionMode mode) {
        if (mode == ExecutionMode.RUST_NATIVE && isRustAvailable()) {
            rustNativeRadio.setSelected(true);
            localTopologyRadio.setSelected(true);
            guiRenderingRadio.setSelected(true);
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.NATIVE_RUST.name());
        } else if (mode == ExecutionMode.GPU && ExecutionContextHardwareHelper.isModeSupported(HardwareMode.GPU_SHADERS)) {
            gpuShadersRadio.setSelected(true);
            localTopologyRadio.setSelected(true);
            guiRenderingRadio.setSelected(true);
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.GPU_SHADERS.name());
            prefs.putBoolean(PREF_GPU_KEY, true);
        } else if (mode == ExecutionMode.CPU_SIMD && isSimdAvailable()) {
            javaVectorSimdRadio.setSelected(true);
            localTopologyRadio.setSelected(true);
            guiRenderingRadio.setSelected(true);
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.JAVA_VECTOR_SIMD.name());
            prefs.putBoolean(PREF_GPU_KEY, false);
        } else if (mode == ExecutionMode.CPU) {
            cpuJitRadio.setSelected(true);
            localTopologyRadio.setSelected(true);
            guiRenderingRadio.setSelected(true);
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.CPU_JIT.name());
            prefs.putBoolean(PREF_GPU_KEY, false);
        } else if (mode == ExecutionMode.CLUSTER) {
            clusterTopologyRadio.setSelected(true);
            guiRenderingRadio.setSelected(true);
            if (clusterSection != null) {
                clusterSection.setVisible(true);
                clusterSection.setManaged(true);
            }
        } else if (mode == ExecutionMode.HEADLESS) {
            headlessRenderingRadio.setSelected(true);
            headlessConfigCard.setVisible(true);
            headlessConfigCard.setManaged(true);
        }
        updateRightSummary();
    }

    public void setRenderingMode(RenderingMode mode) {
        Runnable updateUI = () -> {
            if (mode == RenderingMode.HEADLESS) {
                if (renderingGroup != null) renderingGroup.selectToggle(headlessRenderingRadio);
                if (headlessRenderingRadio != null) headlessRenderingRadio.setSelected(true);
                if (headlessConfigCard != null) {
                    headlessConfigCard.setVisible(true);
                    headlessConfigCard.setManaged(true);
                }
            } else {
                if (renderingGroup != null) renderingGroup.selectToggle(guiRenderingRadio);
                if (guiRenderingRadio != null) guiRenderingRadio.setSelected(true);
                if (headlessConfigCard != null) {
                    headlessConfigCard.setVisible(false);
                    headlessConfigCard.setManaged(false);
                }
            }
            updateRightSummary();
            notifyLiveRenderingModeChange(mode);
        };
        if (javafx.application.Platform.isFxApplicationThread()) {
            updateUI.run();
        } else {
            javafx.application.Platform.runLater(updateUI);
        }
    }

    public void setOnLiveConfigChangedCallback(java.util.function.Consumer<HardwareMode> callback) {
        this.onLiveConfigChangedCallback = callback;
    }

    public void setOnLiveRenderingModeChangedCallback(java.util.function.Consumer<RenderingMode> callback) {
        this.onLiveRenderingModeChangedCallback = callback;
    }

    private void notifyLiveConfigChange(HardwareMode mode) {
        if (onLiveConfigChangedCallback != null) {
            onLiveConfigChangedCallback.accept(mode);
        }
    }

    private void notifyLiveRenderingModeChange(RenderingMode mode) {
        if (onLiveRenderingModeChangedCallback != null) {
            onLiveRenderingModeChangedCallback.accept(mode);
        }
    }

    public void updateTexts() {
        titleHeader.setText(I18n.getOrDefault("exec.title", "⚡ Execution Context & Compute Infrastructure"));
        if (liveBannerLabel != null) {
            liveBannerLabel.setText(I18n.getOrDefault("exec.live_notice", "⚡ MODIFICATIONS EN DIRECT : Tout changement de moteur de calcul ou de configuration s'applique instantanément à la simulation en cours sans interruption."));
        }

        hardwareSectionHeader.setText(I18n.getOrDefault("exec.section.hardware", "1. 🖥️ COMPUTE ENGINE & HARDWARE ACCELERATION"));
        topologySectionHeader.setText(I18n.getOrDefault("exec.section.topology", "2. 🌐 EXECUTION TOPOLOGY & PERSISTENCE"));
        renderingSectionHeader.setText(I18n.getOrDefault("exec.section.rendering", "3. 🚀 RENDERING & DISPLAY MODE (GUI VS HEADLESS)"));

        boolean rustAvailable = isRustAvailable();
        boolean gpuAvailable = false;
        boolean simdAvailable = isSimdAvailable();
        HardwareMode recommended = getRecommendedHardwareMode();
        String recTag = " " + I18n.getOrDefault("exec.badge.recommended", "★ [Recommended]");

        String rustTitle = I18n.getOrDefault("exec.hardware.rust_native", "🦀 Native Rust Multi-Core Engine (Rayon + AVX-512 Fused SIMD)");
        if (rustAvailable) {
            rustNativeRadio.setDisable(false);
            if (recommended == HardwareMode.NATIVE_RUST) rustTitle += recTag;
            rustNativeDescLabel.setText(I18n.getOrDefault("exec.hardware.rust_native.desc", "Ultra-fast compiled native Rust kernel leveraging multi-threaded Rayon work-stealing, zero-copy buffers, and AVX-512 vectorization."));
        } else {
            rustNativeRadio.setDisable(true);
            rustTitle += " " + I18n.getOrDefault("exec.status.unavailable_rust", "(Unavailable — Native Rust DLL not found)");
            rustNativeDescLabel.setText(I18n.getOrDefault("exec.status.unavailable_rust", "(Unavailable — Native Rust DLL not found)"));
        }
        rustNativeRadio.setText(rustTitle);
        rustNativeRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.rust_native.tooltip", "Native compiled shared library with FFM dynamic linkage, SIMD instruction sets (AVX-512/AVX2), and lock-free parallel execution.")));

        String gpuTitle = I18n.getOrDefault("exec.hardware.gpu_shaders", "⚡ GPU Compute Shaders (OpenCL / Dedicated & Integrated GPU)");
        if (gpuAvailable) {
            gpuShadersRadio.setDisable(false);
            if (recommended == HardwareMode.GPU_SHADERS) gpuTitle += recTag;
            gpuShadersDescLabel.setText(I18n.getOrDefault("exec.hardware.gpu_shaders.desc", "Hardware-accelerated OpenCL C compute kernels executing Radiative Equilibrium and Photosynthesis across GPU compute units."));
        } else {
            gpuShadersRadio.setDisable(true);
            gpuTitle += " " + I18n.getOrDefault("exec.status.unavailable_gpu", "(Unavailable — No OpenCL/TornadoVM GPU detected)");
            gpuShadersDescLabel.setText(I18n.getOrDefault("exec.status.unavailable_gpu", "(Unavailable — No OpenCL/TornadoVM GPU detected)"));
        }
        gpuShadersRadio.setText(gpuTitle);
        gpuShadersRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.gpu_shaders.tooltip", "Hardware-accelerated OpenCL C compute kernels executing Radiative Equilibrium and Farquhar FvCB Photosynthesis across GPU compute units.")));

        String simdTitle = I18n.getOrDefault("exec.hardware.java_vector_simd", "☕ Java 21 Incubator Vector SIMD & ForkJoin Engine");
        if (simdAvailable) {
            javaVectorSimdRadio.setDisable(false);
            if (recommended == HardwareMode.JAVA_VECTOR_SIMD) simdTitle += recTag;
            javaVectorSimdDescLabel.setText(I18n.getOrDefault("exec.hardware.java_vector_simd.desc", "Direct hardware SIMD (AVX2/AVX-512 256/512-bit registers) executing parallel Java Vector API kernels with ForkJoinPool work-stealing."));
        } else {
            javaVectorSimdRadio.setDisable(true);
            simdTitle += " " + I18n.getOrDefault("exec.status.unavailable_simd", "(Unavailable — CPU SIMD Vector registers not supported)");
            javaVectorSimdDescLabel.setText(I18n.getOrDefault("exec.status.unavailable_simd", "(Unavailable — CPU SIMD Vector registers not supported)"));
        }
        javaVectorSimdRadio.setText(simdTitle);
        javaVectorSimdRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.java_vector_simd.tooltip", "Direct CPU vector registers via jdk.incubator.vector intrinsics and SuperWord loop unrolling.")));

        String cpuTitle = I18n.getOrDefault("exec.hardware.cpu_jit", "💻 Standard CPU Multi-Thread JIT (HotSpot Compiler)");
        if (recommended == HardwareMode.CPU_JIT) cpuTitle += recTag;
        cpuJitRadio.setText(cpuTitle);
        cpuJitRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.cpu_jit.tooltip", "High-throughput multi-threaded Java execution utilizing all available CPU logical cores.")));
        cpuJitDescLabel.setText(I18n.getOrDefault("exec.hardware.cpu_jit.desc", "Standard JVM HotSpot JIT multi-threaded loops over all detected CPU cores."));

        gpuOffRadio.setText(I18n.getOrDefault("exec.hardware.off", "🛡️ Software Safe Fallback (Single-Thread SW Rendering)"));
        gpuOffRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.off.tooltip", "Safe mode isolating execution from GPU drivers and hardware acceleration layers.")));
        gpuOffDescLabel.setText(I18n.getOrDefault("exec.hardware.off.desc", "Pure single-threaded software fallback mode with no GPU/SIMD dependencies for debugging or resource-constrained hosts."));

        if (determinismDisclaimerTitle != null) {
            determinismDisclaimerTitle.setText(I18n.getOrDefault("exec.hardware.determinism_disclaimer.title", "ℹ️ Cross-Engine Determinism & Floating-Point Convergence Notice"));
        }
        if (determinismDisclaimerText != null) {
            determinismDisclaimerText.setText(I18n.getOrDefault("exec.hardware.determinism_disclaimer", "Each engine operates in 100% deterministic mode from the scenario PRNG seed. However, due to low-level differences in mathematical transcendental libraries (JVM FDLIBM vs LLVM/libm), FMA contractions, and SIMD reduction ordering, trajectories between Java Standard, Java SIMD, and Native Rust are analytically convergent but exhibit minor floating-point mantissa variances (~10⁻⁵ to 10⁻²). Run identical engines for exact bit-for-bit repeatability."));
        }

        localTopologyRadio.setText(I18n.getOrDefault("exec.topology.local", "🏢 Local Standalone Mode (Host Machine Cores & Memory)"));
        localTopologyRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.topology.local.tooltip", "Executes entire planetary simulation locally on the workstation.")));
        localTopologyDescLabel.setText(I18n.getOrDefault("exec.topology.local.desc", "Simulation executes entirely on local computer."));

        clusterTopologyRadio.setText(I18n.getOrDefault("exec.topology.cluster", "🌐 Distributed Cluster Mode (gRPC/TCP Multi-Machine Nodes)"));
        clusterTopologyRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.topology.cluster.tooltip", "Partition H3 spatial grids and compute workloads across network-connected cluster nodes.")));
        clusterTopologyDescLabel.setText(I18n.getOrDefault("exec.topology.cluster.desc", "Distributes H3 grid and computations across multiple machines connected on local network or cloud."));

        if (dbSectionHeader != null) dbSectionHeader.setText(I18n.getOrDefault("exec.topology.db_title", "Database & Persistence Infrastructure:"));
        updateDatabaseStatus();

        if (clusterSection != null) {
            clusterSection.updateTexts();
        }

        guiRenderingRadio.setText(I18n.getOrDefault("exec.rendering.gui", "🖼️ Interactive GUI Mode (Real-Time JavaFX Visual)"));
        guiRenderingRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.rendering.gui.tooltip", "Full real-time 2D/3D visual rendering with live charts and maps.")));
        guiRenderingDescLabel.setText(I18n.getOrDefault("exec.rendering.gui.desc", "Dynamic 2D/3D cartographic rendering. Note: Graphical pipeline consumes 30% to 60% of CPU/GPU cycles."));

        headlessRenderingRadio.setText(I18n.getOrDefault("exec.rendering.headless", "🚀 Headless Mode (Async Background - High Throughput Batch)"));
        headlessRenderingRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.rendering.headless.tooltip", "Run purely in compute background without GUI rendering overhead.")));
        headlessRenderingDescLabel.setText(I18n.getOrDefault("exec.rendering.headless.desc", "Disables visual rendering. Performance gain: Maximum compute throughput (+300% to +1000%)."));

        lblTargetYear.setText(I18n.getOrDefault("exec.headless.target_year", "Target Simulation End Year:"));
        targetYearSpinner.setTooltip(new Tooltip(I18n.getOrDefault("exec.headless.target_year.tooltip", "Simulation halts automatically upon reaching this target year.")));
        lblTargetYearNote.setText(I18n.getOrDefault("exec.headless.target_year_note", "ℹ️ Note: The actual number of computed steps depends on the time step duration (Δt) configured in the scenario."));

        if (sidebar != null) {
            sidebar.updateTexts();
        }
        updateRightSummary();
    }
}
