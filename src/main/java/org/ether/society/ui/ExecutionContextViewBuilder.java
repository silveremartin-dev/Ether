/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.ether.society.ui.ExecutionContextPanel.ExecutionMode;
import org.ether.society.ui.ExecutionContextPanel.HardwareMode;
import org.ether.society.ui.ExecutionContextPanel.RenderingMode;
import org.ether.society.ui.execution.ExecutionContextClusterSection;
import org.ether.society.ui.execution.ExecutionContextHardwareHelper;
import org.ether.society.ui.execution.ExecutionContextI18nHelper;
import org.ether.society.ui.execution.ExecutionContextSidebar;

/**
 * Package-private view builder and mode mutator for {@link ExecutionContextPanel}.
 *
 * <p>Assembles the Hardware Compute Engine, Execution Topology, and Rendering Mode card sections,
 * binds radio button preference persistence, and applies programmatic {@link ExecutionMode} /
 * {@link RenderingMode} transitions.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 * @see ExecutionContextPanel
 */
final class ExecutionContextViewBuilder {

    private ExecutionContextViewBuilder() {
        // Stateless package-private UI helper
    }

    /**
     * Builds and mounts the two-column layout inside {@link ExecutionContextPanel}.
     *
     * @param p host execution context panel
     */
    static void initUI(ExecutionContextPanel p) {
        HBox mainLayout = new HBox(20);
        mainLayout.setAlignment(Pos.TOP_LEFT);

        VBox leftColumn = new VBox(20);
        leftColumn.setMaxWidth(800);
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        p.titleHeader = new Label();
        p.titleHeader.getStyleClass().add("label-title");

        p.liveBannerLabel = new Label();
        p.liveBannerLabel.setStyle("-fx-background-color: rgba(56, 189, 248, 0.15); -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-padding: 8 14; -fx-background-radius: 6; -fx-border-color: #38bdf8; -fx-border-radius: 6; -fx-border-width: 1;");
        p.liveBannerLabel.setWrapText(true);

        VBox hardwareSection = buildHardwareSection(p);
        VBox topologySection = buildTopologySection(p);
        VBox renderingSection = buildRenderingSection(p);

        leftColumn.getChildren().addAll(p.titleHeader, p.liveBannerLabel, hardwareSection, topologySection, renderingSection);

        p.sidebar = new ExecutionContextSidebar();
        ExecutionContextI18nHelper.updateDatabaseStatus(p.dbStatusLabel);
        p.updateRightSummary();

        mainLayout.getChildren().addAll(leftColumn, p.sidebar);

        ScrollPane scroll = new ScrollPane(mainLayout);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        p.setCenter(scroll);
    }

    private static VBox buildHardwareSection(ExecutionContextPanel p) {
        p.hardwareSectionHeader = createSectionHeader("");
        p.hardwareGroup = new ToggleGroup();

        p.rustNativeRadio = createGroupRadio(p.hardwareGroup);
        p.gpuShadersRadio = createGroupRadio(p.hardwareGroup);
        p.javaVectorSimdRadio = createGroupRadio(p.hardwareGroup);
        p.cpuJitRadio = createGroupRadio(p.hardwareGroup);
        p.gpuOffRadio = createGroupRadio(p.hardwareGroup);

        p.rustNativeDescLabel = createDescLabel();
        p.gpuShadersDescLabel = createDescLabel();
        p.javaVectorSimdDescLabel = createDescLabel();
        p.cpuJitDescLabel = createDescLabel();
        p.gpuOffDescLabel = createDescLabel();

        p.rustNativeRadio.setDisable(!ExecutionContextPanel.isRustAvailable());
        p.gpuShadersRadio.setDisable(true);
        p.javaVectorSimdRadio.setDisable(!ExecutionContextPanel.isSimdAvailable());
        p.cpuJitRadio.setDisable(false);
        p.gpuOffRadio.setDisable(false);

        switch (ExecutionContextPanel.getActiveHardwareMode()) {
            case NATIVE_RUST -> p.rustNativeRadio.setSelected(true);
            case GPU_SHADERS -> p.gpuShadersRadio.setSelected(true);
            case JAVA_VECTOR_SIMD -> p.javaVectorSimdRadio.setSelected(true);
            case CPU_JIT -> p.cpuJitRadio.setSelected(true);
            case GPU_OFF -> p.gpuOffRadio.setSelected(true);
        }

        bindHardwareRadio(p, p.rustNativeRadio, HardwareMode.NATIVE_RUST, false, "NATIVE RUST (Rayon + AVX-512)");
        bindHardwareRadio(p, p.gpuShadersRadio, HardwareMode.GPU_SHADERS, true, "GPU COMPUTE SHADERS (OpenCL)");
        bindHardwareRadio(p, p.javaVectorSimdRadio, HardwareMode.JAVA_VECTOR_SIMD, false, "JAVA 21 VECTOR SIMD");
        bindHardwareRadio(p, p.cpuJitRadio, HardwareMode.CPU_JIT, false, "CPU JIT");
        bindHardwareRadio(p, p.gpuOffRadio, HardwareMode.GPU_OFF, false, "GPU OFF (Software Prism Safe Fallback)");

        p.determinismDisclaimerTitle = new Label();
        p.determinismDisclaimerTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 12px;");
        p.determinismDisclaimerText = new Label();
        p.determinismDisclaimerText.setWrapText(true);
        p.determinismDisclaimerText.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-line-spacing: 2px;");
        p.determinismDisclaimerCard = new VBox(6, p.determinismDisclaimerTitle, p.determinismDisclaimerText);
        p.determinismDisclaimerCard.setStyle("-fx-background-color: rgba(30, 41, 59, 0.7); -fx-border-color: rgba(56, 189, 248, 0.4); -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10 14;");

        return createCardSection(p.hardwareSectionHeader, new VBox(10,
                createRadioCard(p.rustNativeRadio, p.rustNativeDescLabel),
                createRadioCard(p.gpuShadersRadio, p.gpuShadersDescLabel),
                createRadioCard(p.javaVectorSimdRadio, p.javaVectorSimdDescLabel),
                createRadioCard(p.cpuJitRadio, p.cpuJitDescLabel),
                createRadioCard(p.gpuOffRadio, p.gpuOffDescLabel),
                p.determinismDisclaimerCard));
    }

    private static void bindHardwareRadio(ExecutionContextPanel p, RadioButton radio, HardwareMode mode, boolean gpuEnabled, String logName) {
        radio.setOnAction(e -> {
            ExecutionContextPanel.prefs.put(ExecutionContextPanel.PREF_HARDWARE_MODE_KEY, mode.name());
            ExecutionContextPanel.prefs.putBoolean(ExecutionContextPanel.PREF_GPU_KEY, gpuEnabled);
            ExecutionContextPanel.logger.info("Hardware acceleration mode set to {}", logName);
            p.updateRightSummary();
            p.notifyLiveConfigChange(mode);
        });
    }

    private static VBox buildTopologySection(ExecutionContextPanel p) {
        p.topologySectionHeader = createSectionHeader("");
        p.topologyGroup = new ToggleGroup();

        p.localTopologyRadio = createGroupRadio(p.topologyGroup);
        p.clusterTopologyRadio = createGroupRadio(p.topologyGroup);
        p.localTopologyRadio.setSelected(true);

        p.localTopologyDescLabel = createDescLabel();
        p.clusterTopologyDescLabel = createDescLabel();

        p.dbSectionHeader = new Label("");
        p.dbSectionHeader.getStyleClass().add("sidebar-recap-header");
        p.dbStatusLabel = new Label();
        p.dbStatusLabel.getStyleClass().add("info-badge");
        p.dbStatusLabel.setWrapText(true);

        VBox dbStatusBox = new VBox(6, p.dbSectionHeader, p.dbStatusLabel);
        dbStatusBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-padding: 10 12; -fx-background-radius: 6; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 6;");

        VBox localCard = new VBox(8, p.localTopologyRadio, p.localTopologyDescLabel, dbStatusBox);
        localCard.getStyleClass().add("card-section");

        p.clusterSection = new ExecutionContextClusterSection(p::updateRightSummary);
        VBox clusterCard = new VBox(8, p.clusterTopologyRadio, p.clusterTopologyDescLabel, p.clusterSection);
        clusterCard.getStyleClass().add("card-section");

        p.localTopologyRadio.setOnAction(e -> {
            p.clusterSection.setVisible(false);
            p.clusterSection.setManaged(false);
            p.updateRightSummary();
        });
        p.clusterTopologyRadio.setOnAction(e -> {
            p.clusterSection.setVisible(true);
            p.clusterSection.setManaged(true);
            p.updateRightSummary();
        });

        return createCardSection(p.topologySectionHeader, new VBox(10, localCard, clusterCard));
    }

    private static VBox buildRenderingSection(ExecutionContextPanel p) {
        p.renderingSectionHeader = createSectionHeader("");
        p.renderingGroup = new ToggleGroup();

        p.guiRenderingRadio = createGroupRadio(p.renderingGroup);
        p.headlessRenderingRadio = createGroupRadio(p.renderingGroup);
        p.guiRenderingRadio.setSelected(true);

        p.guiRenderingDescLabel = createDescLabel();
        p.headlessRenderingDescLabel = createDescLabel();
        VBox guiCard = createRadioCard(p.guiRenderingRadio, p.guiRenderingDescLabel);

        p.targetYearSpinner = new Spinner<>(-100_000, 10_000, 2100, 50);
        p.targetYearSpinner.setEditable(true);
        p.targetYearSpinner.setPrefWidth(140);
        p.targetYearSpinner.valueProperty().addListener((obs, oldV, newV) -> p.updateRightSummary());

        p.lblTargetYear = new Label();
        p.lblTargetYear.getStyleClass().add("control-label");
        p.lblTargetYearNote = createDescLabel();

        HBox targetYearRow = new HBox(12, p.lblTargetYear, p.targetYearSpinner);
        targetYearRow.setAlignment(Pos.CENTER_LEFT);

        p.headlessConfigCard = new VBox(8, targetYearRow, p.lblTargetYearNote);
        p.headlessConfigCard.getStyleClass().add("subcard-section");
        p.headlessConfigCard.setVisible(false);
        p.headlessConfigCard.setManaged(false);

        VBox headlessCard = new VBox(8, p.headlessRenderingRadio, p.headlessRenderingDescLabel, p.headlessConfigCard);
        headlessCard.getStyleClass().add("card-section");

        p.guiRenderingRadio.setOnAction(e -> {
            p.headlessConfigCard.setVisible(false);
            p.headlessConfigCard.setManaged(false);
            p.updateRightSummary();
            p.notifyLiveRenderingModeChange(RenderingMode.GUI);
        });
        p.headlessRenderingRadio.setOnAction(e -> {
            p.headlessConfigCard.setVisible(true);
            p.headlessConfigCard.setManaged(true);
            p.updateRightSummary();
            p.notifyLiveRenderingModeChange(RenderingMode.HEADLESS);
        });

        return createCardSection(p.renderingSectionHeader, new VBox(10, guiCard, headlessCard));
    }

    private static RadioButton createGroupRadio(ToggleGroup group) {
        RadioButton rb = new RadioButton();
        rb.setToggleGroup(group);
        return rb;
    }

    private static VBox createRadioCard(RadioButton radio, Label desc) {
        VBox card = new VBox(6, radio, desc);
        card.getStyleClass().add("card-section");
        return card;
    }

    private static Label createDescLabel() {
        Label lbl = new Label();
        lbl.setWrapText(true);
        lbl.getStyleClass().add("hint-label");
        return lbl;
    }

    private static Label createSectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("label-section-header");
        return lbl;
    }

    private static VBox createCardSection(Label header, VBox content) {
        VBox card = new VBox(10, header, content);
        card.getStyleClass().add("card-section");
        return card;
    }

    static void applyExecutionMode(ExecutionContextPanel p, ExecutionMode mode) {
        if (mode == ExecutionMode.RUST_NATIVE && ExecutionContextPanel.isRustAvailable()) {
            p.rustNativeRadio.setSelected(true);
            p.localTopologyRadio.setSelected(true);
            p.guiRenderingRadio.setSelected(true);
            ExecutionContextPanel.prefs.put(ExecutionContextPanel.PREF_HARDWARE_MODE_KEY, HardwareMode.NATIVE_RUST.name());
        } else if (mode == ExecutionMode.GPU && ExecutionContextHardwareHelper.isModeSupported(HardwareMode.GPU_SHADERS)) {
            p.gpuShadersRadio.setSelected(true);
            p.localTopologyRadio.setSelected(true);
            p.guiRenderingRadio.setSelected(true);
            ExecutionContextPanel.prefs.put(ExecutionContextPanel.PREF_HARDWARE_MODE_KEY, HardwareMode.GPU_SHADERS.name());
            ExecutionContextPanel.prefs.putBoolean(ExecutionContextPanel.PREF_GPU_KEY, true);
        } else if (mode == ExecutionMode.CPU_SIMD && ExecutionContextPanel.isSimdAvailable()) {
            p.javaVectorSimdRadio.setSelected(true);
            p.localTopologyRadio.setSelected(true);
            p.guiRenderingRadio.setSelected(true);
            ExecutionContextPanel.prefs.put(ExecutionContextPanel.PREF_HARDWARE_MODE_KEY, HardwareMode.JAVA_VECTOR_SIMD.name());
            ExecutionContextPanel.prefs.putBoolean(ExecutionContextPanel.PREF_GPU_KEY, false);
        } else if (mode == ExecutionMode.CPU) {
            p.cpuJitRadio.setSelected(true);
            p.localTopologyRadio.setSelected(true);
            p.guiRenderingRadio.setSelected(true);
            ExecutionContextPanel.prefs.put(ExecutionContextPanel.PREF_HARDWARE_MODE_KEY, HardwareMode.CPU_JIT.name());
            ExecutionContextPanel.prefs.putBoolean(ExecutionContextPanel.PREF_GPU_KEY, false);
        } else if (mode == ExecutionMode.CLUSTER) {
            p.clusterTopologyRadio.setSelected(true);
            p.guiRenderingRadio.setSelected(true);
            if (p.clusterSection != null) {
                p.clusterSection.setVisible(true);
                p.clusterSection.setManaged(true);
            }
        } else if (mode == ExecutionMode.HEADLESS) {
            p.headlessRenderingRadio.setSelected(true);
            p.headlessConfigCard.setVisible(true);
            p.headlessConfigCard.setManaged(true);
        }
        p.updateRightSummary();
    }

    static void applyRenderingMode(ExecutionContextPanel p, RenderingMode mode) {
        Runnable updateUI = () -> {
            boolean headless = (mode == RenderingMode.HEADLESS);
            RadioButton target = headless ? p.headlessRenderingRadio : p.guiRenderingRadio;
            if (p.renderingGroup != null) p.renderingGroup.selectToggle(target);
            if (target != null) target.setSelected(true);
            if (p.headlessConfigCard != null) {
                p.headlessConfigCard.setVisible(headless);
                p.headlessConfigCard.setManaged(headless);
            }
            p.updateRightSummary();
            p.notifyLiveRenderingModeChange(mode);
        };
        if (Platform.isFxApplicationThread()) {
            updateUI.run();
        } else {
            Platform.runLater(updateUI);
        }
    }
}
