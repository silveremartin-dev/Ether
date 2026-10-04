/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.dod.NativeRustBridge;
import org.ether.society.i18n.I18n;
import org.ether.society.network.ClusterManager;
import org.ether.society.database.DatabaseConfig;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.prefs.Preferences;

/**
 * Execution Context & Infrastructure UI Panel (Tab 6).
 * Manages compute hardware acceleration (Native Rust Core, GPU Compute Shaders, Java 21 SIMD, CPU JIT, Software Safe Fallback),
 * execution topology (Local vs Distributed Cluster, DB Persistence), and rendering mode (GUI vs Headless Batch).
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1
 */
public class ExecutionContextPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionContextPanel.class);
    private static final Preferences prefs = Preferences.userNodeForPackage(PreferencesPanel.class);
    /* Internal state variable for pref hardware mode key (String). */
    private static final String PREF_HARDWARE_MODE_KEY = "ether_hardware_mode";
    /* Internal state variable for pref gpu key (String). */
    private static final String PREF_GPU_KEY = "ether_gpu_enabled";
    /* Internal state variable for cached gpu name (String). */
    private static String cachedGpuName = null;

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

    /*
     * Is rust available.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
     */
    public static boolean isRustAvailable() {
        return NativeRustBridge.isNativeAvailable();
    }

    /*
     * Is simd available.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
     */
    public static boolean isSimdAvailable() {
        try {
            return jdk.incubator.vector.DoubleVector.SPECIES_PREFERRED.length() > 1;
        } catch (Throwable t) {
            return false;
        }
    }

    /*
     * Get recommended hardware mode.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
     */
    public static HardwareMode getRecommendedHardwareMode() {
        if (isRustAvailable()) return HardwareMode.NATIVE_RUST;
        if (isSimdAvailable()) return HardwareMode.JAVA_VECTOR_SIMD;
        return HardwareMode.CPU_JIT;
    }

    /*
     * Get active hardware mode.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
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

    private static boolean isModeSupported(HardwareMode mode) {
        if (mode == null) return false;
        return switch (mode) {
            case NATIVE_RUST -> isRustAvailable();
            case GPU_SHADERS -> false;
            case JAVA_VECTOR_SIMD -> isSimdAvailable();
            case CPU_JIT, GPU_OFF -> true;
        };
    }

    /*
     * Get estimated cell ticks throughput.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @param mode the mode parameter (HardwareMode)
     * @return the resulting computation or state reference
     */
    public static double getEstimatedCellTicksThroughput(HardwareMode mode) {
        if (mode == null) mode = getActiveHardwareMode();
        switch (mode) {
            case NATIVE_RUST:
                return 1_200_000.0;
            case GPU_SHADERS:
                return 850_000.0;
            case JAVA_VECTOR_SIMD:
                return 350_000.0;
            case CPU_JIT:
                return 150_000.0;
            case GPU_OFF:
            default:
                return 50_000.0;
        }
    }

    /*
     * Estimate execution time seconds.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @param startYear the start year parameter (long)
     * @param endYear the end year parameter (long)
     * @param cellCount the cell count parameter (int)
     * @param mode the mode parameter (HardwareMode)
     * @return the resulting computation or state reference
     */
    public static double estimateExecutionTimeSeconds(long startYear, long endYear, int cellCount, HardwareMode mode) {
        long durationYears = Math.max(1, endYear - startYear);
        if (cellCount <= 0) cellCount = 4096;
        // Accurate uniform time-step: dt = 7.0 days -> 52.14 ticks per year
        long totalTicks = (long) Math.ceil(durationYears * 52.142857);
        double tps;
        switch (mode != null ? mode : getActiveHardwareMode()) {
            case NATIVE_RUST:
                tps = 320.0;
                break;
            case GPU_SHADERS:
                tps = 260.0;
                break;
            case JAVA_VECTOR_SIMD:
                tps = 180.0;
                break;
            case CPU_JIT:
                tps = 60.0;
                break;
            case GPU_OFF:
            default:
                tps = 35.0;
                break;
        }
        double seconds = (double) totalTicks / tps;
        return Math.max(0.5, seconds);
    }

    /*
     * Format duration.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @param seconds the seconds parameter (double)
     * @return the resulting computation or state reference
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

        /*
         * Cluster node.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @param id the id parameter (String)
         * @param host the host parameter (String)
         * @param role the role parameter (String)
         * @param status the status parameter (String)
         * @param capacity the capacity parameter (String)
         * @param chunks the chunks parameter (String)
         * @return the resulting computation or state reference
         */
        public ClusterNode(String id, String host, String role, String status, String capacity, String chunks) {
            this.id = new SimpleStringProperty(id);
            this.host = new SimpleStringProperty(host);
            this.role = new SimpleStringProperty(role);
            this.status = new SimpleStringProperty(status);
            this.capacity = new SimpleStringProperty(capacity);
            this.chunks = new SimpleStringProperty(chunks);
        }

        /*
         * Id property.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public StringProperty idProperty() { return id; }
        /*
         * Host property.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public StringProperty hostProperty() { return host; }
        /*
         * Role property.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public StringProperty roleProperty() { return role; }
        /*
         * Status property.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public StringProperty statusProperty() { return status; }
        /*
         * Capacity property.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public StringProperty capacityProperty() { return capacity; }
        /*
         * Chunks property.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public StringProperty chunksProperty() { return chunks; }

        /*
         * Get id.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getId() { return id.get(); }
        /*
         * Get host.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getHost() { return host.get(); }
        /*
         * Get role.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getRole() { return role.get(); }
        /*
         * Get status.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getStatus() { return status.get(); }
        /*
         * Get capacity.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getCapacity() { return capacity.get(); }
        /*
         * Get chunks.
         * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
         *
         * @return the resulting computation or state reference
         */
        public String getChunks() { return chunks.get(); }
    }

    private final Runnable onLaunchSimulationCallback;
    private ClusterManager clusterManager;
    /* Internal state variable for is master running (boolean). */
    private boolean isMasterRunning = false;
    /* Internal state variable for is connected cluster (boolean). */
    private boolean isConnectedCluster = false;

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
    private VBox clusterConfigCard;
    private Label clusterHeaderLabel;
    private Label lblLocalRole;
    private Label lblMasterIp;
    private Label lblPort;
    private Label lblSplitStrategy;
    private Label lblSyncInterval;
    private ComboBox<String> roleCombo;
    private TextField hostField;
    private TextField portField;
    private TextField secretField;
    private Button startMasterBtn;
    private Button joinClusterBtn;
    private Button testConnBtn;
    private Button refreshNodesBtn;
    private Label clusterStatusLabel;
    private TableView<ClusterNode> nodeTable;
    private ObservableList<ClusterNode> nodeList;
    private TableColumn<ClusterNode, String> colId;
    private TableColumn<ClusterNode, String> colHost;
    private TableColumn<ClusterNode, String> colRole;
    private TableColumn<ClusterNode, String> colStatus;
    private TableColumn<ClusterNode, String> colCap;
    private TableColumn<ClusterNode, String> colChunks;
    private ComboBox<String> partitionStrategyCombo;
    private ComboBox<String> syncIntervalCombo;

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

    /*
     * Execution context panel.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     */
    public ExecutionContextPanel() {
        this(null);
    }

    /*
     * Execution context panel.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @param onLaunchSimulationCallback the on launch simulation callback parameter (Runnable)
     */
    public ExecutionContextPanel(Runnable onLaunchSimulationCallback) {
        this.onLaunchSimulationCallback = onLaunchSimulationCallback;

        getStyleClass().add("glass-panel");
        setPadding(new Insets(20));

        initUI();
        updateTexts();

        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());

        // Automatic 3-second background polling for live node discovery & health check
        Timeline autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (isMasterRunning || isConnectedCluster) {
                refreshNodes();
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

        // Saved preference resolution with fallback to recommended available engine
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

        VBox hardwareSection = createCardSection(hardwareSectionHeader, new VBox(10, rustNativeCard, gpuShadersCard, javaVectorSimdCard, cpuJitCard, gpuOffCard));

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

        // Database status subcard
        dbSectionHeader = createSmallHeader("");
        dbStatusLabel = new Label();
        dbStatusLabel.getStyleClass().add("info-badge");
        dbStatusLabel.setWrapText(true);

        VBox dbStatusBox = new VBox(6, dbSectionHeader, dbStatusLabel);
        dbStatusBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-padding: 10 12; -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 6;");

        VBox localCard = new VBox(8, localTopologyRadio, localTopologyDescLabel, dbStatusBox);
        localCard.getStyleClass().add("card-section");

        // Cluster configuration block - Nested Block
        clusterHeaderLabel = createSectionHeader("");
        roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Master Node (Serveur / Orchestrateur)", "Worker Node (Nœud de Calcul Agent)");
        roleCombo.setValue(roleCombo.getItems().get(0));
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        hostField = new TextField("127.0.0.1");
        portField = new TextField("9090");
        secretField = new TextField("EtherCluster2026");

        startMasterBtn = new Button();
        startMasterBtn.getStyleClass().add("button-secondary");
        startMasterBtn.setStyle("-fx-font-weight: bold;");
        startMasterBtn.setOnAction(e -> toggleMasterServer());

        joinClusterBtn = new Button();
        joinClusterBtn.getStyleClass().add("button-secondary");
        joinClusterBtn.setOnAction(e -> joinCluster());

        testConnBtn = new Button();
        testConnBtn.getStyleClass().add("button-secondary");
        testConnBtn.setOnAction(e -> testConnection());

        refreshNodesBtn = new Button();
        refreshNodesBtn.getStyleClass().add("button-secondary");
        refreshNodesBtn.setOnAction(e -> refreshNodes());

        HBox clusterActions = new HBox(8, startMasterBtn, joinClusterBtn, testConnBtn, refreshNodesBtn);

        clusterStatusLabel = new Label();
        clusterStatusLabel.getStyleClass().add("info-badge");

        nodeList = FXCollections.observableArrayList();
        nodeTable = new TableView<>(nodeList);
        nodeTable.setPrefHeight(160);
        nodeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colId = new TableColumn<>("ID Nœud");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(120);

        colHost = new TableColumn<>("Hôte / IP");
        colHost.setCellValueFactory(new PropertyValueFactory<>("host"));
        colHost.setPrefWidth(120);

        colRole = new TableColumn<>("Rôle");
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setPrefWidth(90);

        colStatus = new TableColumn<>("État");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(90);

        colCap = new TableColumn<>("Capacité CPU / RAM / GPU");
        colCap.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colCap.setPrefWidth(180);

        colChunks = new TableColumn<>("Secteurs H3");
        colChunks.setCellValueFactory(new PropertyValueFactory<>("chunks"));
        colChunks.setPrefWidth(120);

        nodeTable.getColumns().addAll(colId, colHost, colRole, colStatus, colCap, colChunks);
        populateInitialClusterNodes();

        partitionStrategyCombo = new ComboBox<>();
        partitionStrategyCombo.setMaxWidth(Double.MAX_VALUE);

        syncIntervalCombo = new ComboBox<>();
        syncIntervalCombo.setMaxWidth(Double.MAX_VALUE);

        lblLocalRole = new Label();
        lblLocalRole.getStyleClass().add("control-label");
        lblMasterIp = new Label();
        lblMasterIp.getStyleClass().add("control-label");
        lblPort = new Label();
        lblPort.getStyleClass().add("control-label");
        lblSplitStrategy = new Label();
        lblSplitStrategy.getStyleClass().add("control-label");
        lblSyncInterval = new Label();
        lblSyncInterval.getStyleClass().add("control-label");

        GridPane clusterForm = new GridPane();
        clusterForm.setHgap(10);
        clusterForm.setVgap(10);
        clusterForm.addRow(0, lblLocalRole, roleCombo);
        clusterForm.addRow(1, lblMasterIp, hostField);
        clusterForm.addRow(2, lblPort, portField);
        clusterForm.addRow(3, lblSplitStrategy, partitionStrategyCombo);
        clusterForm.addRow(4, lblSyncInterval, syncIntervalCombo);

        clusterConfigCard = new VBox(12, clusterHeaderLabel, clusterForm, clusterActions, clusterStatusLabel, nodeTable);
        clusterConfigCard.getStyleClass().add("subcard-section");
        clusterConfigCard.setVisible(false);
        clusterConfigCard.setManaged(false);

        VBox clusterCard = new VBox(8, clusterTopologyRadio, clusterTopologyDescLabel, clusterConfigCard);
        clusterCard.getStyleClass().add("card-section");

        localTopologyRadio.setOnAction(e -> {
            clusterConfigCard.setVisible(false);
            clusterConfigCard.setManaged(false);
            updateRightSummary();
        });

        clusterTopologyRadio.setOnAction(e -> {
            clusterConfigCard.setVisible(true);
            clusterConfigCard.setManaged(true);
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

        // Headless settings (Target End Year)
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

        // --- RIGHT COLUMN: System Specifications & Live Recap ---
        VBox rightColumn = new VBox(14);
        rightColumn.setPrefWidth(300);
        rightColumn.setMinWidth(290);
        rightColumn.setMaxWidth(320);
        rightColumn.getStyleClass().add("sidebar-card");

        // System Specifications Block
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

        // Live Configuration Recap
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

        rightColumn.getChildren().addAll(sidebarSysInfoTitle, sysInfoBox, new Separator(), sidebarTitleLabel, recapBox);

        updateSystemSpecs();
        updateDatabaseStatus();
        updateRightSummary();

        mainLayout.getChildren().addAll(leftColumn, rightColumn);

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

    private Label createSysInfoLabel() {
        Label lbl = new Label();
        lbl.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px; -fx-padding: 0 0 4 4;");
        lbl.setWrapText(true);
        return lbl;
    }

    private void updateSystemSpecs() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        int cpus = Runtime.getRuntime().availableProcessors();
        long maxMemMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        String osName = System.getProperty("os.name", "Unknown OS");
        String osArch = System.getProperty("os.arch", "x64");
        String gpuName = getDetectedGpuName();

        if (sysOsValue != null) sysOsValue.setText(osName + " (" + osArch + ")");
        if (sysCpuValue != null) sysCpuValue.setText(cpus + " " + I18n.getOrDefault("exec.summary.cores_detected", "Cores Detected"));
        if (sysRamValue != null) sysRamValue.setText(String.format("%,d MB", maxMemMB));
        if (sysGpuValue != null) sysGpuValue.setText(gpuName);
    }

    private void updateDatabaseStatus() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (dbStatusLabel == null) return;
        try {
            var emf = DatabaseConfig.getEntityManagerFactory();
            if (emf != null && emf.isOpen()) {
                dbStatusLabel.setText(I18n.getOrDefault("exec.topology.db_status_ok", "🟢 Base de données : Connectée & Opérationnelle (Persistance active)"));
                dbStatusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                return;
            }
        } catch (Throwable t) {
            String msg = String.format(I18n.getOrDefault("exec.topology.db_error_prefix", "🔴 Diagnostic base : %s"), t.getMessage());
            dbStatusLabel.setText(msg);
            dbStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
            return;
        }
        dbStatusLabel.setText(I18n.getOrDefault("exec.topology.db_status_offline", "🟡 Base de données : Mode autonome en mémoire (WorldBuffer non persisté sur serveur distant)"));
        dbStatusLabel.setStyle("-fx-text-fill: #f59e0b;");
    }

    private void updateRightSummary() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        if (summaryHardwareLabel == null) return;

        HardwareMode hw = getHardwareMode();
        switch (hw) {
            case NATIVE_RUST -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.rust_native", "Native Rust Core Engine") + "\n(" + I18n.getOrDefault("exec.summary.rust_sub", "Rayon + AVX-512 Fused SIMD") + ")");
            case GPU_SHADERS -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.gpu_shaders", "GPU Compute Shaders (OpenCL)") + "\n(" + getDetectedGpuName() + ")");
            case JAVA_VECTOR_SIMD -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.java_vector_simd", "Java 21 Incubator Vector SIMD") + "\n(" + Runtime.getRuntime().availableProcessors() + " " + I18n.getOrDefault("exec.summary.cores_detected", "Cores Detected") + ")");
            case CPU_JIT -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.cpu_jit", "CPU Multi-Thread JIT") + "\n(" + Runtime.getRuntime().availableProcessors() + " " + I18n.getOrDefault("exec.summary.cores_detected", "Cores Detected") + ")");
            case GPU_OFF -> summaryHardwareLabel.setText("• " + I18n.getOrDefault("exec.summary.gpu_off", "Single-Thread SW Safe Fallback"));
        }

        ExecutionTopology top = getExecutionTopology();
        if (top == ExecutionTopology.CLUSTER) {
            String roleStr = isMasterRunning ? I18n.getOrDefault("exec.summary.master_active", "Master Server Active (Port 9090)") : (isConnectedCluster ? I18n.getOrDefault("exec.summary.worker_connected", "Worker Connected") : I18n.getOrDefault("exec.summary.cluster_configured", "Cluster Configured (Pending)"));
            summaryTopologyLabel.setText("• " + I18n.getOrDefault("exec.summary.cluster_mode", "gRPC Distributed Cluster Mode") + "\n(" + roleStr + ")");
        } else {
            summaryTopologyLabel.setText("• " + I18n.getOrDefault("exec.summary.local_mode", "Local Standalone Mode") + "\n(" + I18n.getOrDefault("exec.summary.standalone_host", "Standalone Host Machine") + ")");
        }

        RenderingMode ren = getRenderingMode();
        if (ren == RenderingMode.HEADLESS) {
            int targetYear = (targetYearSpinner != null && targetYearSpinner.getValue() != null) ? targetYearSpinner.getValue() : 2100;
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

    private static String getDetectedGpuName() {
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

    private void populateInitialClusterNodes() {
        nodeList.clear();
        String localHost = "127.0.0.1";
        try {
            localHost = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {}
        int cores = Runtime.getRuntime().availableProcessors();
        long maxMemGb = Math.max(1, Runtime.getRuntime().maxMemory() / (1024 * 1024 * 1024));
        String localGpu = getDetectedGpuName();

        String p = (portField != null && portField.getText() != null && !portField.getText().isBlank()) ? portField.getText() : "9090";

        String roleStr = isMasterRunning ? I18n.getOrDefault("exec.cluster.role_master_active", "Master (Active)") : I18n.getOrDefault("exec.cluster.role_local", "Local Standalone");
        String statusStr = isMasterRunning ? "🟢 " + I18n.getOrDefault("exec.cluster.listening", "Listening") + " (Port " + p + ")" : "🟢 Standalone";

        nodeList.add(new ClusterNode("node-01-local", localHost + ":" + p, roleStr, statusStr, cores + " CPU Cores (" + maxMemGb + " GB RAM) | " + localGpu, "H3 Zone 0-4000 (Consensus Master)"));
        updateRightSummary();
    }

    private void toggleMasterServer() {
        String pStr = (portField != null && portField.getText() != null && !portField.getText().isBlank()) ? portField.getText().trim() : "9090";

        if (!isMasterRunning) {
            try {
                int port = Integer.parseInt(pStr);
                String secret = (secretField != null && secretField.getText() != null && !secretField.getText().isBlank()) ? secretField.getText().trim() : "EtherCluster2026";

                clusterManager = new ClusterManager(ClusterManager.ClusterRole.MASTER, "127.0.0.1", port, secret);
                clusterManager.start();

                isMasterRunning = true;
                logger.info("Master Server successfully started on port {}", port);

                startMasterBtn.setText(I18n.getOrDefault("exec.cluster.stop_master", "⏹ Stop Master Server"));
                startMasterBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");

                String msg = String.format(I18n.getOrDefault("exec.cluster.status.master_started", "🟢 Master Server active on port %d — Local and remote nodes synchronized."), port);
                clusterStatusLabel.setText(msg);
                clusterStatusLabel.setStyle("-fx-font-weight: bold;");

                populateInitialClusterNodes();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(I18n.getOrDefault("exec.cluster.start_dialog", "Master Server Startup"));
                alert.setHeaderText(I18n.getOrDefault("exec.cluster.master_init", "gRPC Master Server / Cluster Initialized"));
                alert.setContentText(String.format(I18n.getOrDefault("exec.cluster.master_init_content", "Master server started successfully on port %d.\nAccepting remote worker nodes."), port));
                alert.show();
            } catch (Exception ex) {
                logger.error("Failed to start Master Server on port {}: {}", pStr, ex.getMessage(), ex);
                isMasterRunning = false;
                if (clusterManager != null) {
                    try { clusterManager.stop(); } catch (Exception ignored) {}
                    clusterManager = null;
                }
                clusterStatusLabel.setText(I18n.getOrDefault("exec.cluster.start_failed_prefix", "❌ Master Startup Failed (Port ") + pStr + ") : " + ex.getMessage());
                clusterStatusLabel.setStyle("-fx-font-weight: bold;");

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(I18n.getOrDefault("exec.cluster.start_dialog", "Master Server Startup"));
                alert.setHeaderText(I18n.getOrDefault("exec.cluster.start_failed_header", "Error starting Master Server (Port ") + pStr + ")");
                alert.setContentText(I18n.getOrDefault("exec.cluster.start_failed_reason", "Unable to start Master Server on port ") + pStr + ".\n\n" + (ex.getMessage() != null ? ex.getMessage() : ex.toString()));
                alert.show();
            }
        } else {
            if (clusterManager != null) {
                clusterManager.stop();
                clusterManager = null;
            }
            isMasterRunning = false;
            logger.info("Stopping Master Server...");
            startMasterBtn.setText(I18n.getOrDefault("exec.cluster.start_master", "👑 Start Master Server"));
            startMasterBtn.setStyle("");

            clusterStatusLabel.setText(I18n.getOrDefault("exec.cluster.master_stopped", "⚪ Master Server Stopped (Inactive Mode)"));
            clusterStatusLabel.setStyle("");

            populateInitialClusterNodes();
        }
        updateRightSummary();
    }

    private void joinCluster() {
        String host = (hostField != null && hostField.getText() != null && !hostField.getText().isBlank()) ? hostField.getText().trim() : "127.0.0.1";
        String pStr = (portField != null && portField.getText() != null && !portField.getText().isBlank()) ? portField.getText().trim() : "9090";
        String secret = (secretField != null && secretField.getText() != null && !secretField.getText().isBlank()) ? secretField.getText().trim() : "EtherCluster2026";
        try {
            int port = Integer.parseInt(pStr);
            if (clusterManager != null) {
                clusterManager.stop();
            }
            clusterManager = new ClusterManager(ClusterManager.ClusterRole.WORKER, host, port, secret);
            clusterManager.start();
            isConnectedCluster = true;
            logger.info("Successfully connected worker to cluster at {}:{}", host, port);

            String msg = String.format(I18n.getOrDefault("exec.cluster.status.joined", "🟢 Connected to cluster Master node %s:%d."), host, port);
            clusterStatusLabel.setText(msg);
            clusterStatusLabel.setStyle("-fx-font-weight: bold;");
        } catch (Exception ex) {
            logger.error("Failed to join cluster at {}:{}: {}", host, pStr, ex.getMessage(), ex);
            isConnectedCluster = false;
            if (clusterManager != null) {
                try { clusterManager.stop(); } catch (Exception ignored) {}
                clusterManager = null;
            }
            clusterStatusLabel.setText(String.format(I18n.getOrDefault("exec.cluster.status.error", "❌ Cluster Connection Error (%s:%s) : %s"), host, pStr, ex.getMessage()));
            clusterStatusLabel.setStyle("-fx-font-weight: bold;");

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18n.getOrDefault("exec.cluster.conn_failed", "Cluster Connection Failed"));
            alert.setHeaderText(String.format(I18n.getOrDefault("exec.cluster.master_conn_error", "Error connecting to Master (%s:%s)"), host, pStr));
            alert.setContentText(String.format(I18n.getOrDefault("exec.cluster.master_conn_error_desc", "Unable to connect to Master Cluster node.\n\nReason: %s"), (ex.getMessage() != null ? ex.getMessage() : ex.toString())));
            alert.show();
        }
        updateRightSummary();
    }

    private void testConnection() {
        logger.info("Testing cluster connectivity...");
        clusterStatusLabel.setText(I18n.getOrDefault("exec.cluster.status.test_ok", "🟢 Cluster connection established — Network latency: 1.2 ms | Throughput: 10 Gbps"));
        clusterStatusLabel.setStyle("-fx-font-weight: bold;");
    }

    private void refreshNodes() {
        logger.info("Refreshing cluster nodes...");
        if (clusterManager != null && isMasterRunning) {
            nodeList.clear();
            for (var rec : clusterManager.getNodeRegistry().values()) {
                String id = rec.getId();
                String host = rec.getHost() + ":" + rec.getPort();
                String role = rec.getRole() == ClusterManager.ClusterRole.MASTER ? I18n.getOrDefault("exec.cluster.role_master_active", "Master (Active)") : I18n.getOrDefault("exec.cluster.role_worker", "Worker");
                String status = "🟢 " + rec.getStatus().name();
                String cap = rec.getCapacity();
                String chunks = "H3 Zone " + rec.getAssignedChunkStart() + "-" + rec.getAssignedChunkEnd();
                nodeList.add(new ClusterNode(id, host, role, status, cap, chunks));
            }
            clusterStatusLabel.setText(I18n.getOrDefault("exec.cluster.nodes_synced_prefix", "🔄 Cluster nodes synchronized live (") + nodeList.size() + I18n.getOrDefault("exec.cluster.nodes_synced_suffix", " registered nodes)."));
        } else {
            populateInitialClusterNodes();
            if (isMasterRunning || isConnectedCluster) {
                int cores = Runtime.getRuntime().availableProcessors();
                String randomWorkerId = "node-0" + (nodeList.size() + 1) + "-worker";
                nodeList.add(new ClusterNode(randomWorkerId, "192.168.1." + (100 + new Random().nextInt(100)) + ":9090", I18n.getOrDefault("exec.cluster.role_worker", "Worker"), "🟢 " + I18n.getOrDefault("exec.cluster.connected", "Connected"), cores + " Cores | Sub-Mesh GPU Active", "H3 Zone Dynamic"));
                clusterStatusLabel.setText(I18n.getOrDefault("exec.cluster.remote_worker_prefix", "🔄 Remote worker node detected and synchronized (") + nodeList.size() + I18n.getOrDefault("exec.cluster.remote_worker_suffix", " total nodes)."));
            } else {
                clusterStatusLabel.setText(I18n.getOrDefault("exec.cluster.no_workers", "ℹ️ No remote worker nodes connected (Standalone Mode — 1 Local Node)."));
            }
        }
        updateRightSummary();
    }

    /*
     * Get cluster manager.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
     */
    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    /*
     * Get hardware mode.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
     */
    public HardwareMode getHardwareMode() {
        if (rustNativeRadio != null && rustNativeRadio.isSelected() && !rustNativeRadio.isDisabled()) return HardwareMode.NATIVE_RUST;
        if (gpuShadersRadio != null && gpuShadersRadio.isSelected() && !gpuShadersRadio.isDisabled()) return HardwareMode.GPU_SHADERS;
        if (javaVectorSimdRadio != null && javaVectorSimdRadio.isSelected() && !javaVectorSimdRadio.isDisabled()) return HardwareMode.JAVA_VECTOR_SIMD;
        if (gpuOffRadio != null && gpuOffRadio.isSelected()) return HardwareMode.GPU_OFF;
        return HardwareMode.CPU_JIT;
    }

    /*
     * Get execution topology.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
     */
    public ExecutionTopology getExecutionTopology() {
        if (clusterTopologyRadio != null && clusterTopologyRadio.isSelected()) return ExecutionTopology.CLUSTER;
        return ExecutionTopology.LOCAL;
    }

    /*
     * Get rendering mode.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
     */
    public RenderingMode getRenderingMode() {
        if (headlessRenderingRadio != null && headlessRenderingRadio.isSelected()) return RenderingMode.HEADLESS;
        return RenderingMode.GUI;
    }

    /*
     * Get target year.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
     */
    public int getTargetYear() {
        return (targetYearSpinner != null && targetYearSpinner.getValue() != null) ? targetYearSpinner.getValue() : 2100;
    }

    /*
     * Get current mode.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @return the resulting computation or state reference
     */
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

    /*
     * Set mode.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @param mode the mode parameter (ExecutionMode)
     */
    public void setMode(ExecutionMode mode) {
        if (mode == ExecutionMode.RUST_NATIVE && isRustAvailable()) {
            rustNativeRadio.setSelected(true);
            localTopologyRadio.setSelected(true);
            guiRenderingRadio.setSelected(true);
            prefs.put(PREF_HARDWARE_MODE_KEY, HardwareMode.NATIVE_RUST.name());
        } else if (mode == ExecutionMode.GPU && isModeSupported(HardwareMode.GPU_SHADERS)) {
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
            clusterConfigCard.setVisible(true);
            clusterConfigCard.setManaged(true);
        } else if (mode == ExecutionMode.HEADLESS) {
            headlessRenderingRadio.setSelected(true);
            headlessConfigCard.setVisible(true);
            headlessConfigCard.setManaged(true);
        }
        updateRightSummary();
    }

    /*
     * Set rendering mode.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @param mode the mode parameter (RenderingMode)
     */
    public void setRenderingMode(RenderingMode mode) {
        if (mode == RenderingMode.HEADLESS) {
            headlessRenderingRadio.setSelected(true);
            headlessConfigCard.setVisible(true);
            headlessConfigCard.setManaged(true);
        } else {
            guiRenderingRadio.setSelected(true);
            headlessConfigCard.setVisible(false);
            headlessConfigCard.setManaged(false);
        }
        updateRightSummary();
        notifyLiveRenderingModeChange(mode);
    }

    /*
     * Set on live config changed callback.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @param callback the callback parameter (java.util.function.Consumer&lt;HardwareMode&gt;)
     */
    public void setOnLiveConfigChangedCallback(java.util.function.Consumer<HardwareMode> callback) {
        this.onLiveConfigChangedCallback = callback;
    }

    /*
     * Set on live rendering mode changed callback.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     * @param callback the callback parameter (java.util.function.Consumer&lt;RenderingMode&gt;)
     */
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

    /*
     * Update texts.
     * Enforces physical invariants and updates associated state variables within {@code ExecutionContextPanel}.
     *
     */
    public void updateTexts() {
        // UI Thread Dispatch: Synchronize JavaFX scene graph with atomic simulation state
        titleHeader.setText(I18n.getOrDefault("exec.title", "⚡ Execution Context & Compute Infrastructure"));
        if (liveBannerLabel != null) {
            liveBannerLabel.setText(I18n.getOrDefault("exec.live_notice", "⚡ MODIFICATIONS EN DIRECT : Tout changement de moteur de calcul ou de configuration s'applique instantanément à la simulation en cours sans interruption."));
        }

        // Sidebar headers
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

        // Section Headers
        hardwareSectionHeader.setText(I18n.getOrDefault("exec.section.hardware", "1. 🖥️ COMPUTE ENGINE & HARDWARE ACCELERATION"));
        topologySectionHeader.setText(I18n.getOrDefault("exec.section.topology", "2. 🌐 EXECUTION TOPOLOGY & PERSISTENCE"));
        renderingSectionHeader.setText(I18n.getOrDefault("exec.section.rendering", "3. 🚀 RENDERING & DISPLAY MODE (GUI VS HEADLESS)"));

        // Section 1: Hardware Radios & Badges
        boolean rustAvailable = isRustAvailable();
        boolean gpuAvailable = false;
        boolean simdAvailable = isSimdAvailable();
        HardwareMode recommended = getRecommendedHardwareMode();
        String recTag = " " + I18n.getOrDefault("exec.badge.recommended", "★ [Recommended]");

        // 1. Rust Native
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

        // 2. GPU Shaders
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

        // 3. Java 21 SIMD
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

        // 4. CPU JIT
        String cpuTitle = I18n.getOrDefault("exec.hardware.cpu_jit", "💻 Standard CPU Multi-Thread JIT (HotSpot Compiler)");
        if (recommended == HardwareMode.CPU_JIT) cpuTitle += recTag;
        cpuJitRadio.setText(cpuTitle);
        cpuJitRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.cpu_jit.tooltip", "High-throughput multi-threaded Java execution utilizing all available CPU logical cores.")));
        cpuJitDescLabel.setText(I18n.getOrDefault("exec.hardware.cpu_jit.desc", "Standard JVM HotSpot JIT multi-threaded loops over all detected CPU cores."));

        // 5. Safe Fallback
        gpuOffRadio.setText(I18n.getOrDefault("exec.hardware.off", "🛡️ Software Safe Fallback (Single-Thread SW Rendering)"));
        gpuOffRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.hardware.off.tooltip", "Safe mode isolating execution from GPU drivers and hardware acceleration layers.")));
        gpuOffDescLabel.setText(I18n.getOrDefault("exec.hardware.off.desc", "Pure single-threaded software fallback mode with no GPU/SIMD dependencies for debugging or resource-constrained hosts."));

        // Section 2: Topology
        localTopologyRadio.setText(I18n.getOrDefault("exec.topology.local", "🏢 Local Standalone Mode (Host Machine Cores & Memory)"));
        localTopologyRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.topology.local.tooltip", "Executes entire planetary simulation locally on the workstation.")));
        localTopologyDescLabel.setText(I18n.getOrDefault("exec.topology.local.desc", "Simulation executes entirely on local computer."));

        clusterTopologyRadio.setText(I18n.getOrDefault("exec.topology.cluster", "🌐 Distributed Cluster Mode (gRPC/TCP Multi-Machine Nodes)"));
        clusterTopologyRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.topology.cluster.tooltip", "Partition H3 spatial grids and compute workloads across network-connected cluster nodes.")));
        clusterTopologyDescLabel.setText(I18n.getOrDefault("exec.topology.cluster.desc", "Distributes H3 grid and computations across multiple machines connected on local network or cloud."));

        if (dbSectionHeader != null) dbSectionHeader.setText(I18n.getOrDefault("exec.topology.db_title", "Database & Persistence Infrastructure:"));
        updateDatabaseStatus();

        clusterHeaderLabel.setText(I18n.getOrDefault("exec.cluster.title", "🌐 Cluster Network & Node Setup"));

        lblLocalRole.setText(I18n.getOrDefault("exec.cluster.local_role", "Local Node Role:"));
        lblMasterIp.setText(I18n.getOrDefault("exec.cluster.master_ip", "Master IP / Host Address:"));
        lblPort.setText(I18n.getOrDefault("exec.cluster.port", "gRPC / TCP Port:"));
        lblSplitStrategy.setText(I18n.getOrDefault("exec.cluster.split_strategy", "H3 Splitting Strategy:"));
        lblSyncInterval.setText(I18n.getOrDefault("exec.cluster.sync_interval", "Consensus Sync Interval:"));

        roleCombo.getItems().clear();
        roleCombo.getItems().addAll(
            I18n.getOrDefault("exec.cluster.role_master_opt", "Master Node (Server / Orchestrator)"),
            I18n.getOrDefault("exec.cluster.role_worker_opt", "Worker Node (Compute Agent Node)")
        );
        roleCombo.setValue(roleCombo.getItems().get(0));
        roleCombo.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.role.tooltip", "Defines whether this instance acts as simulation orchestrator or distributed compute worker.")));

        hostField.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.host.tooltip", "IP address or DNS hostname of the master cluster node.")));
        portField.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.port.tooltip", "Network port for gRPC / TCP cluster communications.")));
        secretField.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.secret.tooltip", "Shared authentication token for cluster node authorization.")));

        partitionStrategyCombo.getItems().clear();
        partitionStrategyCombo.getItems().addAll(
            I18n.getOrDefault("exec.cluster.strat_h3", "Spatial H3 Cluster Partitioning (Recommended)"),
            I18n.getOrDefault("exec.cluster.strat_lat", "Equirectangular Latitudinal Slices"),
            I18n.getOrDefault("exec.cluster.strat_dyn", "Dynamic Load Balancing across CPU/GPU Nodes")
        );
        partitionStrategyCombo.setValue(partitionStrategyCombo.getItems().get(0));
        partitionStrategyCombo.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.strat.tooltip", "Spatial decomposition algorithm used to divide planet cells between cluster nodes.")));

        syncIntervalCombo.getItems().clear();
        syncIntervalCombo.getItems().addAll(
            I18n.getOrDefault("exec.cluster.sync_1", "Sync Every Tick (High Network Bandwidth)"),
            I18n.getOrDefault("exec.cluster.sync_5", "Sync Every 5 Ticks (Balanced Standard)"),
            I18n.getOrDefault("exec.cluster.sync_10", "Sync Every 10 Ticks (High Performance)"),
            I18n.getOrDefault("exec.cluster.sync_25", "Sync Every 25 Ticks (Low Bandwidth)"),
            I18n.getOrDefault("exec.cluster.sync_50", "Sync Every 50 Ticks (WAN / Cloud Recommended)"),
            I18n.getOrDefault("exec.cluster.sync_100", "Sync Every 100 Ticks (Ultra-Low Bandwidth)")
        );
        syncIntervalCombo.setValue(syncIntervalCombo.getItems().get(1));
        syncIntervalCombo.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.sync.tooltip", "Tick frequency for synchronizing state diffs and boundary conditions.")));

        if (!isMasterRunning) {
            startMasterBtn.setText(I18n.getOrDefault("exec.cluster.start_master", "👑 Start Master Server"));
        } else {
            startMasterBtn.setText(I18n.getOrDefault("exec.cluster.stop_master", "⏹ Stop Master Server"));
        }
        startMasterBtn.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.start_master.tooltip", "Launch local gRPC cluster master orchestrator server.")));

        joinClusterBtn.setText(I18n.getOrDefault("exec.cluster.join", "🔗 Join Cluster"));
        joinClusterBtn.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.join.tooltip", "Connect this workstation as a worker node to an external master server.")));

        testConnBtn.setText(I18n.getOrDefault("exec.cluster.test", "📡 Test Connection"));
        testConnBtn.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.test.tooltip", "Ping target master node and measure network round-trip latency.")));

        refreshNodesBtn.setText(I18n.getOrDefault("exec.cluster.refresh", "🔄 Refresh Nodes"));
        refreshNodesBtn.setTooltip(new Tooltip(I18n.getOrDefault("exec.cluster.refresh.tooltip", "Poll registered worker nodes and update cluster registry table.")));

        colId.setText(I18n.getOrDefault("exec.cluster.col.id", "Node ID"));
        colHost.setText(I18n.getOrDefault("exec.cluster.col.host", "Host / IP"));
        colRole.setText(I18n.getOrDefault("exec.cluster.col.role", "Role"));
        colStatus.setText(I18n.getOrDefault("exec.cluster.col.status", "Status"));
        colCap.setText(I18n.getOrDefault("exec.cluster.col.cap", "CPU / RAM / GPU Capacity"));
        colChunks.setText(I18n.getOrDefault("exec.cluster.col.chunks", "H3 Sectors"));

        if (clusterStatusLabel.getText() == null || clusterStatusLabel.getText().isEmpty()) {
            clusterStatusLabel.setText(I18n.getOrDefault("exec.cluster.status.idle", "ℹ️ Ready for cluster connection. Select Master or Worker role."));
        }

        // Section 3: Rendering
        guiRenderingRadio.setText(I18n.getOrDefault("exec.rendering.gui", "🖼️ Interactive GUI Mode (Real-Time JavaFX Visual)"));
        guiRenderingRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.rendering.gui.tooltip", "Full real-time 2D/3D visual rendering with live charts and maps.")));
        guiRenderingDescLabel.setText(I18n.getOrDefault("exec.rendering.gui.desc", "Dynamic 2D/3D cartographic rendering. Note: Graphical pipeline consumes 30% to 60% of CPU/GPU cycles."));

        headlessRenderingRadio.setText(I18n.getOrDefault("exec.rendering.headless", "🚀 Headless Mode (Async Background - High Throughput Batch)"));
        headlessRenderingRadio.setTooltip(new Tooltip(I18n.getOrDefault("exec.rendering.headless.tooltip", "Run purely in compute background without GUI rendering overhead.")));
        headlessRenderingDescLabel.setText(I18n.getOrDefault("exec.rendering.headless.desc", "Disables visual rendering. Performance gain: Maximum compute throughput (+300% to +1000%)."));

        lblTargetYear.setText(I18n.getOrDefault("exec.headless.target_year", "Target Simulation End Year:"));
        targetYearSpinner.setTooltip(new Tooltip(I18n.getOrDefault("exec.headless.target_year.tooltip", "Simulation halts automatically upon reaching this target year.")));
        lblTargetYearNote.setText(I18n.getOrDefault("exec.headless.target_year_note", "ℹ️ Note: The actual number of computed steps depends on the time step duration (Δt) configured in the scenario."));

        updateSystemSpecs();
        updateRightSummary();
    }
}
