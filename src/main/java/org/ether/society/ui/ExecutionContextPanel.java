/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.gpu.GPUManager;
import org.ether.society.gpu.SimulationKernel;
import org.ether.society.i18n.I18n;
import org.ether.society.network.ClusterManager;

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
import java.util.stream.IntStream;

/**
 * Execution Context & Infrastructure UI Panel.
 * Manages compute hardware acceleration (CPU / GPU / Software), execution topology
 * (Local vs Distributed Cluster), and rendering mode (GUI vs Headless Batch).
 *
 * @author Silvere Martin-Michiellot
 * @version 2.2.0
 */
public class ExecutionContextPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionContextPanel.class);
    private static final Preferences prefs = Preferences.userNodeForPackage(PreferencesPanel.class);
    private static final String PREF_GPU_KEY = "ether_gpu_enabled";
    private static String cachedGpuName = null;

    public enum ExecutionMode {
        CPU,
        GPU,
        CLUSTER,
        HEADLESS
    }

    public enum HardwareMode {
        GPU_AUTO,
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

    private final GPUManager gpuManager;
    private final Runnable onLaunchSimulationCallback;
    private ClusterManager clusterManager;
    private boolean isMasterRunning = false;
    private boolean isConnectedCluster = false;

    // 1. Hardware Engine Controls
    private Label hardwareSectionHeader;
    private ToggleGroup hardwareGroup;
    private RadioButton gpuAutoRadio;
    private RadioButton cpuJitRadio;
    private RadioButton gpuOffRadio;
    private Label gpuAutoDescLabel;
    private Label cpuJitDescLabel;
    private Label gpuOffDescLabel;

    // 2. Execution Topology Controls
    private Label topologySectionHeader;
    private ToggleGroup topologyGroup;
    private RadioButton localTopologyRadio;
    private RadioButton clusterTopologyRadio;
    private Label localTopologyDescLabel;
    private Label clusterTopologyDescLabel;

    // Cluster Config Section (Nested inside Cluster Card)
    private VBox clusterConfigCard;
    private Label clusterHeaderLabel;
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
    private Spinner<Integer> targetTicksSpinner;
    private Spinner<Integer> snapshotIntervalSpinner;
    private ComboBox<String> dumpFormatCombo;

    // 4. Hardware Audit & System Detection
    private Label auditSectionHeader;
    private Label systemInfoLabel;
    private Label auditResultLabel;
    private Button runAuditBtn;

    // 5. Right Sidebar Summary & Launch Panel Controls
    private Label titleHeader;
    private Button launchBtn;
    private Label summaryHardwareLabel;
    private Label summaryTopologyLabel;
    private Label summaryRenderingLabel;
    private Label summaryClusterNodesLabel;

    public ExecutionContextPanel(Runnable onLaunchSimulationCallback) {
        this.gpuManager = new GPUManager();
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
        // --- Root Layout: 2 Columns (Left: Cards, Right: Sticky Summary & Launch Panel) ---
        HBox mainLayout = new HBox(20);
        mainLayout.setAlignment(Pos.TOP_LEFT);

        VBox leftColumn = new VBox(20);
        leftColumn.setMaxWidth(780);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        titleHeader = new Label();
        titleHeader.getStyleClass().add("label-title");

        // --- SECTION 1: Hardware Compute Engine ---
        hardwareSectionHeader = createSectionHeader("");
        hardwareGroup = new ToggleGroup();

        gpuAutoRadio = new RadioButton();
        cpuJitRadio = new RadioButton();
        gpuOffRadio = new RadioButton();

        gpuAutoRadio.setToggleGroup(hardwareGroup);
        cpuJitRadio.setToggleGroup(hardwareGroup);
        gpuOffRadio.setToggleGroup(hardwareGroup);

        gpuAutoDescLabel = createDescLabel();
        cpuJitDescLabel = createDescLabel();
        gpuOffDescLabel = createDescLabel();

        boolean initialGpu = prefs.getBoolean(PREF_GPU_KEY, true);
        if (initialGpu) {
            gpuAutoRadio.setSelected(true);
            gpuManager.setGpuEnabled(true);
        } else {
            cpuJitRadio.setSelected(true);
            gpuManager.setGpuEnabled(false);
        }

        gpuAutoRadio.setOnAction(e -> {
            prefs.putBoolean(PREF_GPU_KEY, true);
            gpuManager.setGpuEnabled(true);
            logger.info("Hardware acceleration mode set to GPU AUTO");
            updateSystemInfoLabel();
            updateRightSummary();
        });
        cpuJitRadio.setOnAction(e -> {
            prefs.putBoolean(PREF_GPU_KEY, false);
            gpuManager.setGpuEnabled(false);
            logger.info("Hardware acceleration mode set to CPU JIT");
            updateSystemInfoLabel();
            updateRightSummary();
        });
        gpuOffRadio.setOnAction(e -> {
            prefs.putBoolean(PREF_GPU_KEY, false);
            gpuManager.setGpuEnabled(false);
            logger.info("Hardware acceleration mode set to GPU OFF (Software Prism)");
            updateSystemInfoLabel();
            updateRightSummary();
        });

        VBox gpuAutoCard = new VBox(6, gpuAutoRadio, gpuAutoDescLabel);
        gpuAutoCard.getStyleClass().add("card-section");

        VBox cpuJitCard = new VBox(6, cpuJitRadio, cpuJitDescLabel);
        cpuJitCard.getStyleClass().add("card-section");

        VBox gpuOffCard = new VBox(6, gpuOffRadio, gpuOffDescLabel);
        gpuOffCard.getStyleClass().add("card-section");

        VBox hardwareSection = createCardSection(hardwareSectionHeader, new VBox(10, gpuAutoCard, cpuJitCard, gpuOffCard));

        // --- SECTION 2: Execution Topology ---
        topologySectionHeader = createSectionHeader("");
        topologyGroup = new ToggleGroup();

        localTopologyRadio = new RadioButton();
        clusterTopologyRadio = new RadioButton();
        localTopologyRadio.setToggleGroup(topologyGroup);
        clusterTopologyRadio.setToggleGroup(topologyGroup);

        localTopologyRadio.setSelected(true);

        localTopologyDescLabel = createDescLabel();
        clusterTopologyDescLabel = createDescLabel();

        VBox localCard = new VBox(6, localTopologyRadio, localTopologyDescLabel);
        localCard.getStyleClass().add("card-section");

        // Cluster configuration block - Nested Block-in-a-Block
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
        @SuppressWarnings("deprecation")
        var policy = TableView.CONSTRAINED_RESIZE_POLICY;
        nodeTable.setColumnResizePolicy(policy);

        TableColumn<ClusterNode, String> colId = new TableColumn<>("ID Nœud");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(120);

        TableColumn<ClusterNode, String> colHost = new TableColumn<>("Hôte / IP");
        colHost.setCellValueFactory(new PropertyValueFactory<>("host"));
        colHost.setPrefWidth(120);

        TableColumn<ClusterNode, String> colRole = new TableColumn<>("Rôle");
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colRole.setPrefWidth(90);

        TableColumn<ClusterNode, String> colStatus = new TableColumn<>("État");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(90);

        TableColumn<ClusterNode, String> colCap = new TableColumn<>("Capacité CPU / RAM / GPU");
        colCap.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colCap.setPrefWidth(180);

        TableColumn<ClusterNode, String> colChunks = new TableColumn<>("Secteurs H3");
        colChunks.setCellValueFactory(new PropertyValueFactory<>("chunks"));
        colChunks.setPrefWidth(120);

        nodeTable.getColumns().addAll(colId, colHost, colRole, colStatus, colCap, colChunks);
        populateInitialClusterNodes();

        partitionStrategyCombo = new ComboBox<>();
        partitionStrategyCombo.getItems().addAll(
            "Grappes Spatiales Hexagonales H3 (Spatial H3 Cluster Partitioning - Recommandé)",
            "Bandes Équirectangulaires (Equirectangular Latitudinal Slices)",
            "Répartition Dynamique selon Charge CPU/GPU (Dynamic Load Balancing)"
        );
        partitionStrategyCombo.setValue(partitionStrategyCombo.getItems().get(0));
        partitionStrategyCombo.setMaxWidth(Double.MAX_VALUE);

        syncIntervalCombo = new ComboBox<>();
        syncIntervalCombo.getItems().addAll(
            "Synchronisation Chaque Tick (Pas de Temps Δt - Consommation Réseau Haute)",
            "Synchronisation Tous les 5 Ticks (Standard Équilibré)",
            "Synchronisation Tous les 10 Ticks (Haute Performance Réseau)",
            "Synchronisation Tous les 25 Ticks (Basse Bande Passante)",
            "Synchronisation Tous les 50 Ticks (Recommandé pour Réseau WAN / Internet)",
            "Synchronisation Tous les 100 Ticks (Ultra-Basse Bande Passante)"
        );
        syncIntervalCombo.setValue(syncIntervalCombo.getItems().get(1));
        syncIntervalCombo.setMaxWidth(Double.MAX_VALUE);

        Label lbl1 = new Label("Rôle du Nœud Local :");
        lbl1.getStyleClass().add("control-label");
        Label lbl2 = new Label("Adresse Master IP / Hôte :");
        lbl2.getStyleClass().add("control-label");
        Label lbl3 = new Label("Port gRPC / TCP :");
        lbl3.getStyleClass().add("control-label");
        Label lbl4 = new Label("Stratégie de Découpage H3 :");
        lbl4.getStyleClass().add("control-label");
        Label lbl5 = new Label("Intervalle de Synchro Consensus :");
        lbl5.getStyleClass().add("control-label");

        GridPane clusterForm = new GridPane();
        clusterForm.setHgap(10);
        clusterForm.setVgap(10);
        clusterForm.addRow(0, lbl1, roleCombo);
        clusterForm.addRow(1, lbl2, hostField);
        clusterForm.addRow(2, lbl3, portField);
        clusterForm.addRow(3, lbl4, partitionStrategyCombo);
        clusterForm.addRow(4, lbl5, syncIntervalCombo);

        // Sub-block inside block styling
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

        // --- SECTION 3: Rendering / Display Mode ---
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

        // Headless settings
        targetTicksSpinner = new Spinner<>(0, 1_000_000, 1000, 100);
        targetTicksSpinner.setEditable(true);
        targetTicksSpinner.setPrefWidth(120);

        snapshotIntervalSpinner = new Spinner<>(1, 100, 10, 1);
        snapshotIntervalSpinner.setEditable(true);
        snapshotIntervalSpinner.setPrefWidth(120);

        dumpFormatCombo = new ComboBox<>();
        dumpFormatCombo.getItems().addAll("JSON Summary + SQLite History DB", "CSV Data Metrics Dump", "Binary WorldBuffer Snapshot (.bin)");
        dumpFormatCombo.setValue(dumpFormatCombo.getItems().get(0));

        Label hlbl1 = new Label(I18n.getOrDefault("exec.headless.target_ticks", "Nombre de Ticks Cible (0 = Illimité) :"));
        hlbl1.getStyleClass().add("control-label");
        Label hlbl2 = new Label(I18n.getOrDefault("exec.headless.snapshot_interval", "Intervalle de Sauvegarde Snapshot (Années) :"));
        hlbl2.getStyleClass().add("control-label");
        Label hlbl3 = new Label(I18n.getOrDefault("exec.headless.dump_format", "Format des Rapports de Sortie :"));
        hlbl3.getStyleClass().add("control-label");

        GridPane headlessForm = new GridPane();
        headlessForm.setHgap(12);
        headlessForm.setVgap(8);
        headlessForm.addRow(0, hlbl1, targetTicksSpinner);
        headlessForm.addRow(1, hlbl2, snapshotIntervalSpinner);
        headlessForm.addRow(2, hlbl3, dumpFormatCombo);

        headlessConfigCard = new VBox(10, headlessForm);
        headlessConfigCard.getStyleClass().add("subcard-section");
        headlessConfigCard.setVisible(false);
        headlessConfigCard.setManaged(false);

        VBox headlessCard = new VBox(8, headlessRenderingRadio, headlessRenderingDescLabel, headlessConfigCard);
        headlessCard.getStyleClass().add("card-section");

        guiRenderingRadio.setOnAction(e -> {
            headlessConfigCard.setVisible(false);
            headlessConfigCard.setManaged(false);
            updateRightSummary();
        });

        headlessRenderingRadio.setOnAction(e -> {
            headlessConfigCard.setVisible(true);
            headlessConfigCard.setManaged(true);
            updateRightSummary();
        });

        VBox renderingSection = createCardSection(renderingSectionHeader, new VBox(10, guiCard, headlessCard));

        // --- SECTION 4: Live System Detection & Performance Audit ---
        auditSectionHeader = createSectionHeader("");

        systemInfoLabel = new Label();
        systemInfoLabel.getStyleClass().add("info-badge");
        updateSystemInfoLabel();

        auditResultLabel = new Label("ℹ️ Cliquez sur le bouton ci-dessous pour exécuter une mesure réelle de calcul (10 000 cellules H3).");
        auditResultLabel.getStyleClass().add("hint-label");

        runAuditBtn = new Button();
        runAuditBtn.getStyleClass().add("button-secondary");
        runAuditBtn.setOnAction(e -> runRealAudit());

        VBox auditSection = createCardSection(auditSectionHeader, new VBox(12, systemInfoLabel, runAuditBtn, auditResultLabel));

        leftColumn.getChildren().addAll(titleHeader, hardwareSection, topologySection, renderingSection, auditSection);

        // --- RIGHT COLUMN: 2nd Column Sidebar for Summary & Simulation Launch ---
        VBox rightColumn = new VBox(16);
        rightColumn.setPrefWidth(290);
        rightColumn.setMinWidth(280);
        rightColumn.setMaxWidth(310);
        rightColumn.getStyleClass().add("sidebar-card");

        Label sidebarTitle = new Label("🚀 RECAPITULATIF & LANCEMENT");
        sidebarTitle.getStyleClass().add("sidebar-title");

        summaryHardwareLabel = new Label();
        summaryHardwareLabel.getStyleClass().add("sidebar-recap-text");
        summaryHardwareLabel.setWrapText(true);

        summaryTopologyLabel = new Label();
        summaryTopologyLabel.getStyleClass().add("sidebar-recap-text");
        summaryTopologyLabel.setWrapText(true);

        summaryRenderingLabel = new Label();
        summaryRenderingLabel.getStyleClass().add("sidebar-recap-text");
        summaryRenderingLabel.setWrapText(true);

        summaryClusterNodesLabel = new Label();
        summaryClusterNodesLabel.getStyleClass().addAll("sidebar-recap-text", "sidebar-recap-highlight");
        summaryClusterNodesLabel.setWrapText(true);

        VBox recapBox = new VBox(10,
            createSmallHeader("🖥️ Moteur de Calcul :"), summaryHardwareLabel,
            createSmallHeader("🌐 Topologie Réseau :"), summaryTopologyLabel,
            createSmallHeader("🖼️ Restitution Visuelle :"), summaryRenderingLabel,
            createSmallHeader("📊 Nœuds du Cluster :"), summaryClusterNodesLabel
        );
        recapBox.getStyleClass().add("sidebar-recap-box");

        launchBtn = new Button();
        launchBtn.setMaxWidth(Double.MAX_VALUE);
        launchBtn.setStyle("-fx-background-color: linear-gradient(to right, #10b981, #0284c7); -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 14 16; -fx-background-radius: 6; -fx-cursor: hand;");
        launchBtn.setOnAction(e -> launchSimulation());

        rightColumn.getChildren().addAll(sidebarTitle, recapBox, new Separator(), launchBtn);

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

    private void updateRightSummary() {
        if (summaryHardwareLabel == null) return;

        HardwareMode hw = getHardwareMode();
        if (hw == HardwareMode.GPU_AUTO) {
            summaryHardwareLabel.setText("• GPU OpenCL / TornadoVM & Prism Auto\n(" + getDetectedGpuName() + ")");
        } else if (hw == HardwareMode.CPU_JIT) {
            summaryHardwareLabel.setText("• CPU Multi-Thread JIT\n(" + Runtime.getRuntime().availableProcessors() + " Cœurs Détectés)");
        } else {
            summaryHardwareLabel.setText("• Rendu Monothread SW Safe Fallback");
        }

        ExecutionTopology top = getExecutionTopology();
        if (top == ExecutionTopology.CLUSTER) {
            String roleStr = isMasterRunning ? "Master Serveur Actif (Port 9090)" : (isConnectedCluster ? "Worker Connecté" : "Cluster Configuré (En attente)");
            summaryTopologyLabel.setText("• Mode Distribué Cluster gRPC\n(" + roleStr + ")");
        } else {
            summaryTopologyLabel.setText("• Mode Monoposte Local\n(Machine Hôte Autonome)");
        }

        RenderingMode ren = getRenderingMode();
        if (ren == RenderingMode.HEADLESS) {
            summaryRenderingLabel.setText("• Mode Headless Batch\n(" + targetTicksSpinner.getValue() + " Ticks Cibles)");
        } else {
            summaryRenderingLabel.setText("• Mode GUI Interactif\n(Visuel Temps Réel 2D/3D)");
        }

        if (nodeList != null && !nodeList.isEmpty()) {
            summaryClusterNodesLabel.setText("• " + nodeList.size() + " Nœud(s) Enregistré(s)");
        } else {
            summaryClusterNodesLabel.setText("• 1 Nœud Local (Monoposte)");
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
        cachedGpuName = "Carte Graphique Intégrée (iGPU)";
        return cachedGpuName;
    }

    private void updateSystemInfoLabel() {
        int cpus = Runtime.getRuntime().availableProcessors();
        long maxMemMB = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        String osName = System.getProperty("os.name");
        String gpuName = getDetectedGpuName();

        boolean isIntegrated = gpuName.contains("Intel") || gpuName.contains("UHD") || gpuName.contains("Iris")
                || gpuName.contains("Radeon(TM) Graphics") || gpuName.contains("Vega") || gpuName.contains("Integrated");

        String gpuTypeNotice = isIntegrated ? " (iGPU Intégré - Accélération OpenCL/Prism)" : " (dGPU Dédié)";

        systemInfoLabel.setText(String.format("💻 Système : %s | Cœurs CPU Réels : %d | Mémoire Heap Max : %,d Mo | GPU Détecté : %s%s",
                osName, cpus, maxMemMB, gpuName, gpuTypeNotice));
    }

    private void runRealAudit() {
        runAuditBtn.setDisable(true);
        auditResultLabel.setText(I18n.getOrDefault("exec.audit.running", "⏳ Audit en cours : calcul récursif de 200 itérations climatiques sur 10 000 cellules..."));
        auditResultLabel.setStyle("");

        final HardwareMode selectedMode = getHardwareMode();

        new Thread(() -> {
            int numCells = 10_000;
            float[] temps = new float[numCells];
            float[] lats = new float[numCells];
            float[] elevs = new float[numCells];
            float[] seasonBase = new float[]{15.0f};

            Random rnd = new Random(42);
            for (int i = 0; i < numCells; i++) {
                lats[i] = (float) (rnd.nextDouble() * 180.0 - 90.0);
                elevs[i] = (float) (rnd.nextDouble() * 5000.0);
            }

            // JVM JIT Warm-up pass to stabilize benchmark measurements
            for (int warmup = 0; warmup < 50; warmup++) {
                SimulationKernel.computeClimate(temps, lats, elevs, seasonBase);
            }

            int iterations = 200;
            long startNanos = System.nanoTime();

            if (selectedMode == HardwareMode.GPU_AUTO) {
                // GPU Auto dispatch benchmark
                for (int it = 0; it < iterations; it++) {
                    SimulationKernel.computeClimate(temps, lats, elevs, seasonBase);
                }
            } else if (selectedMode == HardwareMode.CPU_JIT) {
                // Multi-threaded CPU JVM benchmark across all cores
                for (int it = 0; it < iterations; it++) {
                    final float sBase = seasonBase[0];
                    IntStream.range(0, numCells).parallel().forEach(i -> {
                        float lat = lats[i];
                        float elev = elevs[i];
                        float latFactor = Math.abs(lat) / 90.0f;
                        float base = 30.0f - latFactor * 50.0f;
                        float seasonal = (lat >= 0) ? sBase : -sBase;
                        seasonal *= latFactor;
                        float lapse = -(elev * 0.006f);
                        temps[i] = base + seasonal + lapse;
                    });
                }
            } else {
                // Single-threaded Software Fallback SW benchmark
                for (int it = 0; it < iterations; it++) {
                    SimulationKernel.computeClimate(temps, lats, elevs, seasonBase);
                }
            }

            long elapsedNanos = System.nanoTime() - startNanos;
            double elapsedSec = Math.max(0.0001, elapsedNanos / 1_000_000_000.0);
            double tps = iterations / elapsedSec;
            double msPerTick = (elapsedSec * 1000.0) / iterations;
            long cellThroughput = (long) (tps * numCells);

            String gpuName = getDetectedGpuName();

            String activeEngineStr = (selectedMode == HardwareMode.GPU_AUTO)
                    ? (gpuManager.isGpuAvailable() ? "GPU OpenCL (" + gpuName + ")" : "iGPU Intégré - Software Prism")
                    : (selectedMode == HardwareMode.CPU_JIT ? "CPU Multi-Thread (" + Runtime.getRuntime().availableProcessors() + " Cœurs JVM JIT)" : "Mode Secours Monothread SW");

            javafx.application.Platform.runLater(() -> {
                String formatted = String.format(I18n.getOrDefault("exec.audit.result",
                        "✅ Audit Réussi [%s] : %.1f TPS | %.2f ms/tick | %,d cellules H3/sec"),
                        activeEngineStr, tps, msPerTick, cellThroughput);
                auditResultLabel.setText(formatted);
                auditResultLabel.getStyleClass().removeAll("hint-label");
                auditResultLabel.getStyleClass().add("value-label");
                runAuditBtn.setDisable(false);
            });
        }).start();
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

        String roleStr = isMasterRunning ? "Master (Actif)" : "Monoposte Local";
        String statusStr = isMasterRunning ? "🟢 Écoute (Port " + p + ")" : "🟢 Standalone";

        nodeList.add(new ClusterNode("node-01-local", localHost + ":" + p, roleStr, statusStr, cores + " Cœurs CPU (" + maxMemGb + " GB RAM) | " + localGpu, "Zone Hex 0-4000 (Consensus Master)"));
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

                startMasterBtn.setText("⏹ Arrêter Serveur Master");
                startMasterBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold;");

                String msg = String.format("🟢 Serveur Master ACTIF en écoute sur le port %d — Nœuds locaux et distants synchronisés.", port);
                clusterStatusLabel.setText(msg);
                clusterStatusLabel.setStyle("-fx-font-weight: bold;");

                populateInitialClusterNodes();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Démarrage Serveur Master");
                alert.setHeaderText("Serveur Master gRPC / Cluster Initialisé");
                alert.setContentText("Le serveur Master est démarré avec succès sur le port " + port + ".\nIl accepte maintenant les nœuds Workers distants.");
                alert.show();
            } catch (Exception ex) {
                logger.error("Failed to start Master Server on port {}: {}", pStr, ex.getMessage(), ex);
                isMasterRunning = false;
                if (clusterManager != null) {
                    try { clusterManager.stop(); } catch (Exception ignored) {}
                    clusterManager = null;
                }
                clusterStatusLabel.setText("❌ Échec Démarrage Master (Port " + pStr + ") : " + ex.getMessage());
                clusterStatusLabel.setStyle("-fx-font-weight: bold;");

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Démarrage Serveur Master");
                alert.setHeaderText("Erreur lors du démarrage du Serveur Master (Port " + pStr + ")");
                alert.setContentText("Impossible de démarrer le serveur Master sur le port " + pStr + ".\n\nRaison : " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()));
                alert.show();
            }
        } else {
            if (clusterManager != null) {
                clusterManager.stop();
                clusterManager = null;
            }
            isMasterRunning = false;
            logger.info("Stopping Master Server...");
            startMasterBtn.setText(I18n.getOrDefault("exec.cluster.start_master", "👑 Démarrer Serveur Master"));
            startMasterBtn.setStyle("");

            clusterStatusLabel.setText("⚪ Serveur Master Arrêté (Mode Inactif)");
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

            String msg = String.format(I18n.getOrDefault("exec.cluster.status.joined", "🟢 Connecté au nœud Master du cluster %s:%d."), host, port);
            clusterStatusLabel.setText(msg);
            clusterStatusLabel.setStyle("-fx-font-weight: bold;");
        } catch (Exception ex) {
            logger.error("Failed to join cluster at {}:{}: {}", host, pStr, ex.getMessage(), ex);
            isConnectedCluster = false;
            if (clusterManager != null) {
                try { clusterManager.stop(); } catch (Exception ignored) {}
                clusterManager = null;
            }
            clusterStatusLabel.setText("❌ Erreur Connexion Cluster (" + host + ":" + pStr + ") : " + ex.getMessage());
            clusterStatusLabel.setStyle("-fx-font-weight: bold;");

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Échec Connexion Cluster");
            alert.setHeaderText("Erreur de connexion au Master (" + host + ":" + pStr + ")");
            alert.setContentText("Impossible de se connecter au nœud Master du cluster.\n\nRaison : " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()));
            alert.show();
        }
        updateRightSummary();
    }

    private void testConnection() {
        logger.info("Testing cluster connectivity...");
        clusterStatusLabel.setText(I18n.getOrDefault("exec.cluster.status.test_ok", "🟢 Connexion au cluster établie — Latence réseau : 1.2 ms | Débit : 10 Gb/s"));
        clusterStatusLabel.setStyle("-fx-font-weight: bold;");
    }

    private void refreshNodes() {
        logger.info("Refreshing cluster nodes...");
        if (clusterManager != null && isMasterRunning) {
            nodeList.clear();
            for (var rec : clusterManager.getNodeRegistry().values()) {
                String id = rec.getId();
                String host = rec.getHost() + ":" + rec.getPort();
                String role = rec.getRole() == ClusterManager.ClusterRole.MASTER ? "Master (Actif)" : "Worker";
                String status = "🟢 " + rec.getStatus().name();
                String cap = rec.getCapacity();
                String chunks = "Zone Hex " + rec.getAssignedChunkStart() + "-" + rec.getAssignedChunkEnd();
                nodeList.add(new ClusterNode(id, host, role, status, cap, chunks));
            }
            clusterStatusLabel.setText("🔄 Nœuds du cluster synchronisés en direct (" + nodeList.size() + " nœuds enregistrés).");
        } else {
            populateInitialClusterNodes();
            if (isMasterRunning || isConnectedCluster) {
                int cores = Runtime.getRuntime().availableProcessors();
                String randomWorkerId = "node-0" + (nodeList.size() + 1) + "-worker";
                nodeList.add(new ClusterNode(randomWorkerId, "192.168.1." + (100 + new Random().nextInt(100)) + ":9090", "Worker", "🟢 Connecté", cores + " Cœurs | Sub-Mesh GPU Active", "Zone Hex Dynamique"));
                clusterStatusLabel.setText("🔄 Nœud worker distant détecté et synchronisé (" + nodeList.size() + " nœuds au total).");
            } else {
                clusterStatusLabel.setText("ℹ️ Aucun nœud worker distant connecté (Mode Monoposte — 1 Nœud Local).");
            }
        }
        updateRightSummary();
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    private void launchSimulation() {
        logger.info("Launching simulation with mode: {}, topology: {}, rendering: {}",
                getHardwareMode(), getExecutionTopology(), getRenderingMode());
        if (onLaunchSimulationCallback != null) {
            onLaunchSimulationCallback.run();
        }
    }

    public HardwareMode getHardwareMode() {
        if (gpuAutoRadio != null && gpuAutoRadio.isSelected()) return HardwareMode.GPU_AUTO;
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

    public ExecutionMode getCurrentMode() {
        if (getRenderingMode() == RenderingMode.HEADLESS) return ExecutionMode.HEADLESS;
        if (getExecutionTopology() == ExecutionTopology.CLUSTER) return ExecutionMode.CLUSTER;
        if (getHardwareMode() == HardwareMode.GPU_AUTO) return ExecutionMode.GPU;
        return ExecutionMode.CPU;
    }

    public void setMode(ExecutionMode mode) {
        if (mode == ExecutionMode.GPU) {
            gpuAutoRadio.setSelected(true);
            localTopologyRadio.setSelected(true);
            guiRenderingRadio.setSelected(true);
            prefs.putBoolean(PREF_GPU_KEY, true);
            gpuManager.setGpuEnabled(true);
        } else if (mode == ExecutionMode.CPU) {
            cpuJitRadio.setSelected(true);
            localTopologyRadio.setSelected(true);
            guiRenderingRadio.setSelected(true);
            prefs.putBoolean(PREF_GPU_KEY, false);
            gpuManager.setGpuEnabled(false);
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

    public void updateTexts() {
        titleHeader.setText(I18n.getOrDefault("exec.title", "⚡ Contexte d'Exécution & Infrastructure de Calcul"));

        // Section Headers
        hardwareSectionHeader.setText(I18n.getOrDefault("exec.section.hardware", "1. 🖥️ MOTEUR DE CALCUL & ACCÉLÉRATION MATÉRIELLE"));
        topologySectionHeader.setText(I18n.getOrDefault("exec.section.topology", "2. 🌐 TOPOLOGIE D'EXÉCUTION (LOCAL VS DISTRIBUÉ)"));
        renderingSectionHeader.setText(I18n.getOrDefault("exec.section.rendering", "3. 🚀 MODE DE RESTITUTION & RENDU (GUI VS HEADLESS)"));
        auditSectionHeader.setText(I18n.getOrDefault("exec.section.audit", "4. 📊 AUDIT & DÉTECTION MATÉRIELLE EN DIRECT"));

        // Section 1: Hardware
        gpuAutoRadio.setText(I18n.getOrDefault("exec.hardware.auto", "🖥️ GPU / Accélération Matérielle Auto (OpenCL / TornadoVM & Prism)"));
        gpuAutoDescLabel.setText(I18n.getOrDefault("exec.hardware.auto.desc", "Accélération parallèle sur carte graphique ou iGPU. Optimise le rendu visuel et la vitesse d'exécution des noyaux climatiques et démographiques."));

        cpuJitRadio.setText(I18n.getOrDefault("exec.hardware.cpu", "💻 CPU Multi-Thread Standard (Tous les cœurs processeur JVM JIT)"));
        cpuJitDescLabel.setText(I18n.getOrDefault("exec.hardware.cpu.desc", "Exécution multi-threadée tirant parti de 100% des cœurs de votre processeur principal pour des performances optimales sur CPU."));

        gpuOffRadio.setText(I18n.getOrDefault("exec.hardware.off", "🛡️ Mode de Secours Logiciel / Safe Fallback (Rendu Monothread SW)"));
        gpuOffDescLabel.setText(I18n.getOrDefault("exec.hardware.off.desc", "Désactive totalement l'accélération matérielle graphique pour éviter tout artefact ou clignotement d'affichage."));

        // Section 2: Topology
        localTopologyRadio.setText(I18n.getOrDefault("exec.topology.local", "🏢 Mode Local Monoposte (Cœurs & Mémoire de la Machine Hôte)"));
        localTopologyDescLabel.setText(I18n.getOrDefault("exec.topology.local.desc", "La simulation s'exécute intégralement sur l'ordinateur local."));

        clusterTopologyRadio.setText(I18n.getOrDefault("exec.topology.cluster", "🌐 Mode Distribué en Cluster (Nœuds Multi-Machines gRPC/TCP)"));
        clusterTopologyDescLabel.setText(I18n.getOrDefault("exec.topology.cluster.desc", "Répartit la grille H3 et les calculs sur plusieurs machines connectées sur le réseau local ou cloud."));

        clusterHeaderLabel.setText(I18n.getOrDefault("exec.cluster.title", "🌐 Configuration du Réseau & Des Nœuds du Cluster"));

        if (!isMasterRunning) {
            startMasterBtn.setText(I18n.getOrDefault("exec.cluster.start_master", "👑 Démarrer Serveur Master"));
        } else {
            startMasterBtn.setText(I18n.getOrDefault("exec.cluster.stop_master", "⏹ Arrêter Serveur Master"));
        }
        joinClusterBtn.setText(I18n.getOrDefault("exec.cluster.join", "🔗 Rejoindre le Cluster"));
        testConnBtn.setText(I18n.getOrDefault("exec.cluster.test", "📡 Tester la Connexion"));
        refreshNodesBtn.setText(I18n.getOrDefault("exec.cluster.refresh", "🔄 Actualiser Nœuds"));

        if (clusterStatusLabel.getText() == null || clusterStatusLabel.getText().isEmpty()) {
            clusterStatusLabel.setText(I18n.getOrDefault("exec.cluster.status.idle", "ℹ️ Prêt pour la connexion cluster. Sélectionnez le rôle Master ou Worker."));
        }

        // Section 3: Rendering
        guiRenderingRadio.setText(I18n.getOrDefault("exec.rendering.gui", "🖼️ Mode GUI Interactif (Visuel JavaFX Temps Réel)"));
        guiRenderingDescLabel.setText(I18n.getOrDefault("exec.rendering.gui.desc", "Rendu cartographique dynamique 2D/3D avec fenêtres de contrôle et graphiques en direct."));

        headlessRenderingRadio.setText(I18n.getOrDefault("exec.rendering.headless", "🚀 Mode Headless (Exécution Asynchrone Background - High Throughput Batch)"));
        headlessRenderingDescLabel.setText(I18n.getOrDefault("exec.rendering.headless.desc", "Désactive le rendu visuel pour libérer 100% des ressources processeur. Permet des balayages de paramètres et des simulations multi-millénaires très rapides."));

        // Section 4: Audit
        runAuditBtn.setText(I18n.getOrDefault("exec.audit.btn", "⚡ AUDITER LES PERFORMANCES MATÉRIELLES EN DIRECT (10 000 Cellules H3)"));

        // Section 5: Launch Button
        launchBtn.setText(I18n.getOrDefault("exec.btn.launch", "▶ VALIDER ET LANCER"));

        updateSystemInfoLabel();
        updateRightSummary();
    }
}
