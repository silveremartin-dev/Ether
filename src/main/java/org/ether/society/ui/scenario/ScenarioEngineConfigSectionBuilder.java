/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.scenario.ScenarioEngineCatalog.EngineMeta;
import org.ether.society.ui.util.UIControlsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Autonomous UI Controller and Section Builder for Simulation Engines, Determinism & Optimizations.
 *
 * <p>Extracted from {@code ScenarioSetupPanel} to satisfy the Single Responsibility Principle (SRP).
 * Decouples Tier 1 permanent physical conservation laws from Tier 2 pluggable cliodynamic hypotheses,
 * manages strict bit-for-bit reproducibility vs heuristic shortcuts, KaTeX formula inspection,
 * and dynamic Java engine template generation and runtime hot-compilation.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioEngineConfigSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEngineConfigSectionBuilder.class);

    private final Runnable onParamChangedCallback;
    private final Consumer<String> onEngineHoveredCallback;

    // Master Determinism Control
    private CheckBox strictDeterminismCheckBox;
    private Label optSubHeader;

    // 7 Individual Optimizations & Performance Shortcuts
    private CheckBox sparseCellSkippingCheckBox;
    private CheckBox oceanMacroAggregationCheckBox;
    private CheckBox coastalNavigationOnlyCheckBox;
    private CheckBox oceanMultiRateTickingCheckBox;
    private Slider climateTickFreqSlider;
    private Label climateTickFreqValueLabel;
    private CheckBox parallelExecutionCheckBox;
    private Slider threadCountSlider;
    private Label threadCountValueLabel;
    private CheckBox spatialRangeTruncationCheckBox;

    // Engine Inspector Card Controls
    private Label engineInspectorTitle;
    private Label engineInspectorText;
    private Label engineInspectorEquationsTitle;
    private Label engineInspectorEquations;
    private Label engineInspectorRef;
    private Button exportSelectedEngineBtn;
    private Button exportCoreTemplateBtn;
    private Label coreExplanationLabel;
    private String selectedEngineClassName;

    // Core Engines (Tier 1)
    private static class CoreEngineRow {
        final String id;
        final Label descLabel;
        final HBox row;
        final String defaultTitle;
        final String defaultDesc;
        final String defaultRef;
        final String defaultEq;

        CoreEngineRow(String id, Label descLabel, HBox row, String defaultTitle, String defaultDesc, String defaultRef, String defaultEq) {
            this.id = id;
            this.descLabel = descLabel;
            this.row = row;
            this.defaultTitle = defaultTitle;
            this.defaultDesc = defaultDesc;
            this.defaultRef = defaultRef;
            this.defaultEq = defaultEq;
        }
    }

    private final List<CoreEngineRow> coreEngineRows = new ArrayList<>();
    private TitledPane corePane;

    // Optional Engines (Tier 2)
    private static class OptionalEngineMeta {
        final String id;
        final CheckBox cb;
        final String defaultTitle;
        final String defaultDesc;
        final String defaultRef;
        final String defaultEq;

        OptionalEngineMeta(String id, CheckBox cb, String defaultTitle, String defaultDesc, String defaultRef, String defaultEq) {
            this.id = id;
            this.cb = cb;
            this.defaultTitle = defaultTitle;
            this.defaultDesc = defaultDesc;
            this.defaultRef = defaultRef;
            this.defaultEq = defaultEq;
        }
    }

    private final Map<String, CheckBox> typeBCheckBoxMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Spinner<Double>>> typeBParamSpinnersMap = new LinkedHashMap<>();
    private final List<OptionalEngineMeta> optionalEngineMetas = new ArrayList<>();
    private VBox typeBBoxContainer;
    private TitledPane typeBPane;

    // Toolbar Controls
    private Button autoSelectEnginesForYearBtn;
    private Button exportTemplateBtn;
    private Button importEngineBtn;
    private ComboBox<String> engineSortCombo;
    private Label engineSortLabel;

    private final Map<String, VBox> engineContainers = new LinkedHashMap<>();

    /**
     * Constructs the engine section builder.
     *
     * @param onParamChangedCallback callback invoked whenever any setting or engine parameter is modified
     * @param onEngineHoveredCallback callback invoked when an engine is hovered to inspect its formulas
     */
    public ScenarioEngineConfigSectionBuilder(Runnable onParamChangedCallback, Consumer<String> onEngineHoveredCallback) {
        this.onParamChangedCallback = onParamChangedCallback;
        this.onEngineHoveredCallback = onEngineHoveredCallback;
    }

    /**
     * Builds and returns the comprehensive engine architecture and optimizations card section.
     *
     * @return JavaFX VBox container
     */
    public VBox buildSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("card-section");

        Label oceanOptHeader = new Label(I18n.getOrDefault("scenario.ocean_opt.header", "⚙️ 5. ENGINE ARCHITECTURE & OPTIMIZATIONS (ETHER CORE & OPTIONAL)"));
        oceanOptHeader.getStyleClass().add("label-section-header");

        Label oceanOptDesc = new Label(I18n.getOrDefault("scenario.ocean_opt.desc", "Definition and configuration of determinism mode, 7 simulation optimizations, and Ether Core and optional procedural engines. Each scenario embeds its optimization setup to guarantee perfect reproducibility."));
        oceanOptDesc.getStyleClass().add("card-description-muted");
        oceanOptDesc.setWrapText(true);

        // --- 🎯 MASTER CONTROL : STRICT DETERMINISM MODE ---
        strictDeterminismCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.strict_determinism", "🔒 STRICT DETERMINISM MODE (0% Approximation / 100% Bit-to-Bit Reproducibility)"));
        strictDeterminismCheckBox.setSelected(true);
        strictDeterminismCheckBox.getStyleClass().add("radio-proc");
        strictDeterminismCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.strict_determinism",
            "🎯 STRICT BIT-TO-BIT DETERMINISM (ACADEMIC RESEARCH MODE)\n" +
            "• Disables ALL optimizations and algorithmic shortcuts.\n" +
            "• Guarantees 100% bit-identical simulation trajectories on the same seed.\n" +
            "• Recommended for validation tests, benchmarks, and convergence audits.")));
        UIControlsHelper.attachDefaultValueHandling(strictDeterminismCheckBox, true, () -> strictDeterminismCheckBox.setSelected(true), onParamChangedCallback);

        Label determinismNote = new Label(I18n.getOrDefault("scenario.note.strict_determinism", "💡 Note: Checking Strict Determinism disables all heuristic approaches and guarantees bit-identical numerical fidelity."));
        determinismNote.getStyleClass().add("control-note");
        determinismNote.setWrapText(true);

        VBox masterBox = new VBox(4, strictDeterminismCheckBox, determinismNote);
        masterBox.getStyleClass().add("opt-master-box");

        // --- ⚡ INDIVIDUAL OPTIMIZATIONS & APPROXIMATIONS ---
        optSubHeader = new Label(I18n.getOrDefault("scenario.opt.sub_header", "⚡ ALGORITHMIC OPTIMIZATIONS & PERFORMANCE SHORTCUTS:"));
        optSubHeader.getStyleClass().add("opt-subheader");

        sparseCellSkippingCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.sparse_cell_skipping", "🦗 Sparse / Uninhabited Cell Skipping (Deserts & Abysses)"));
        sparseCellSkippingCheckBox.setSelected(false);
        sparseCellSkippingCheckBox.getStyleClass().add("opt-sub-checkbox");
        sparseCellSkippingCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.sparse_cell_skipping",
            "⚡ BENEFIT: +40% to +60% TPS speedup across global grid.\n" +
            "⚠️ PHYSICAL IMPACT: Bypasses evaluation loops on desert/oceanic cells with no human presence or active event.")));
        UIControlsHelper.attachDefaultValueHandling(sparseCellSkippingCheckBox, false, () -> sparseCellSkippingCheckBox.setSelected(false), onParamChangedCallback);

        oceanMacroAggregationCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.macro_aggregation", "🌊 Abyssal Ocean Macro-Aggregation (Deep Basins z < -200m in Blocks)"));
        oceanMacroAggregationCheckBox.setSelected(false);
        oceanMacroAggregationCheckBox.getStyleClass().add("opt-sub-checkbox");
        oceanMacroAggregationCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.ocean_macro_aggregation",
            "⚡ BENEFIT: +25% to +35% TPS speedup by grouping deep water cells.\n" +
            "⚠️ PHYSICAL IMPACT: Smoothing of abyssal micro-currents without impacting terrestrial civilizations.")));
        UIControlsHelper.attachDefaultValueHandling(oceanMacroAggregationCheckBox, false, () -> oceanMacroAggregationCheckBox.setSelected(false), onParamChangedCallback);

        coastalNavigationOnlyCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.coastal_nav", "⚓ Exclusive Coastal Navigation (Pathfinding Focused on Coasts & Straits)"));
        coastalNavigationOnlyCheckBox.setSelected(false);
        coastalNavigationOnlyCheckBox.getStyleClass().add("opt-sub-checkbox");
        coastalNavigationOnlyCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.coastal_nav",
            "⚡ BENEFIT: Major CPU savings on naval and commercial trade pathfinding.\n" +
            "⚠️ PHYSICAL IMPACT: Ships prefer coastal waters; ocean navigation restricted prior to Age of Discovery.")));
        UIControlsHelper.attachDefaultValueHandling(coastalNavigationOnlyCheckBox, false, () -> coastalNavigationOnlyCheckBox.setSelected(false), onParamChangedCallback);

        oceanMultiRateTickingCheckBox = new CheckBox(I18n.getOrDefault("scenario.ocean_opt.multi_rate_ticking", "⏱ Oceanic & Multi-Rate Climate Ticking (Updated Every N Ticks)"));
        oceanMultiRateTickingCheckBox.setSelected(false);
        oceanMultiRateTickingCheckBox.getStyleClass().add("opt-sub-checkbox");
        oceanMultiRateTickingCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.multi_rate_ticking",
            "⚡ BENEFIT: +30% throughput by executing thermohaline circulation and fluid inertia at sub-frequency.\n" +
            "⚠️ PHYSICAL IMPACT: Potential temporal aliasing during ultra-fast atmospheric events.")));
        UIControlsHelper.attachDefaultValueHandling(oceanMultiRateTickingCheckBox, false, () -> oceanMultiRateTickingCheckBox.setSelected(false), onParamChangedCallback);

        climateTickFreqSlider = new Slider(1, 30, 5);
        climateTickFreqSlider.setMajorTickUnit(5);
        climateTickFreqSlider.setMinorTickCount(4);
        climateTickFreqSlider.setSnapToTicks(true);
        climateTickFreqSlider.setShowTickMarks(true);
        climateTickFreqSlider.setStyle("-fx-pref-width: 200px;");
        climateTickFreqSlider.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.climate_tick_freq", "Adjust climate update frequency ratio")));
        climateTickFreqValueLabel = new Label(I18n.getOrDefault("scenario.label.climate_freq_default", "Climate Frequency Ratio: 1:5 ticks"));
        climateTickFreqValueLabel.getStyleClass().add("opt-value-highlight");
        climateTickFreqSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = newV.intValue();
            climateTickFreqValueLabel.setText(val == 1 ? I18n.getOrDefault("scenario.climate_freq.strict", "Climate Frequency Ratio: 1:1 (Strict Bit-for-Bit Cadence)") : I18n.getOrDefault("scenario.climate_freq.ratio_prefix", "Climate Frequency Ratio: 1:") + val + " ticks");
            notifyParamChange();
        });
        UIControlsHelper.attachDefaultValueHandling(climateTickFreqSlider, 6.0, () -> climateTickFreqSlider.setValue(6.0), onParamChangedCallback);

        HBox climateSliderBox = new HBox(10, new Label("   └─"), climateTickFreqValueLabel, climateTickFreqSlider);
        climateSliderBox.setAlignment(Pos.CENTER_LEFT);

        parallelExecutionCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.parallel_execution", "🚀 Async Multi-Thread Parallelization (CompletableFuture / AVX)"));
        parallelExecutionCheckBox.setSelected(false);
        parallelExecutionCheckBox.getStyleClass().add("opt-sub-checkbox");
        parallelExecutionCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.parallel_execution",
            "⚡ BENEFIT: Full exploitation of all available CPU cores.\n" +
            "⚠️ PHYSICAL IMPACT: Floating point sum order may slightly vary across executions.")));
        UIControlsHelper.attachDefaultValueHandling(parallelExecutionCheckBox, false, () -> parallelExecutionCheckBox.setSelected(false), onParamChangedCallback);

        threadCountSlider = new Slider(0, 32, 0);
        threadCountSlider.setMajorTickUnit(8);
        threadCountSlider.setMinorTickCount(7);
        threadCountSlider.setSnapToTicks(true);
        threadCountSlider.setShowTickMarks(true);
        threadCountSlider.setStyle("-fx-pref-width: 200px;");
        threadCountSlider.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.thread_count", "Select number of CPU threads assigned to parallel processing")));
        threadCountValueLabel = new Label(I18n.getOrDefault("scenario.label.threads_auto", "Threads Multi-Thread : Auto (Tous cœurs CPU)"));
        threadCountValueLabel.getStyleClass().add("opt-value-highlight");
        threadCountSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = newV.intValue();
            if (val == 0) {
                threadCountValueLabel.setText(String.format(I18n.getOrDefault("scenario.label.threads_auto_cores", "Threads Multi-Thread : Auto (Tous cœurs %d)"), Runtime.getRuntime().availableProcessors()));
            } else if (val == 1) {
                threadCountValueLabel.setText(I18n.getOrDefault("scenario.label.threads_single", "Multi-Thread Threads: 1 (Single-threaded Deterministic)"));
            } else {
                threadCountValueLabel.setText(String.format(I18n.getOrDefault("scenario.label.threads_count", "Threads Multi-Thread : %d threads"), val));
            }
            notifyParamChange();
        });
        UIControlsHelper.attachDefaultValueHandling(threadCountSlider, 4.0, () -> threadCountSlider.setValue(4.0), onParamChangedCallback);

        HBox threadSliderBox = new HBox(10, new Label("   └─"), threadCountValueLabel, threadCountSlider);
        threadSliderBox.setAlignment(Pos.CENTER_LEFT);

        spatialRangeTruncationCheckBox = new CheckBox(I18n.getOrDefault("scenario.opt.spatial_truncation", "💨 Spatial Range Truncation of Plumes & Diffusions (10⁻⁶ Cutoff)"));
        spatialRangeTruncationCheckBox.setSelected(false);
        spatialRangeTruncationCheckBox.getStyleClass().add("opt-sub-checkbox");
        spatialRangeTruncationCheckBox.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.spatial_truncation",
            "⚡ BENEFIT: Restricts atmospheric dispersion calculation to affected adjacent cells.\n" +
            "⚠️ PHYSICAL IMPACT: Discards ultra-diluted aerosol and soot concentrations below 10⁻⁶ ppm.")));
        UIControlsHelper.attachDefaultValueHandling(spatialRangeTruncationCheckBox, false, () -> spatialRangeTruncationCheckBox.setSelected(false), onParamChangedCallback);

        List<CheckBox> subOpts = List.of(
            sparseCellSkippingCheckBox,
            oceanMacroAggregationCheckBox,
            coastalNavigationOnlyCheckBox,
            oceanMultiRateTickingCheckBox,
            parallelExecutionCheckBox,
            spatialRangeTruncationCheckBox
        );

        strictDeterminismCheckBox.setOnAction(e -> {
            boolean strict = strictDeterminismCheckBox.isSelected();
            if (strict) {
                for (CheckBox cb : subOpts) {
                    cb.setSelected(false);
                }
            }
            updatePerformanceControlsState();
            notifyParamChange();
        });

        for (CheckBox cb : subOpts) {
            cb.setOnAction(e -> {
                if (cb.isSelected() && strictDeterminismCheckBox.isSelected()) {
                    strictDeterminismCheckBox.setSelected(false);
                } else if (subOpts.stream().noneMatch(CheckBox::isSelected)) {
                    strictDeterminismCheckBox.setSelected(true);
                }
                updatePerformanceControlsState();
                notifyParamChange();
            });
        }

        VBox optBox = new VBox(5,
            optSubHeader,
            sparseCellSkippingCheckBox,
            oceanMacroAggregationCheckBox,
            coastalNavigationOnlyCheckBox,
            oceanMultiRateTickingCheckBox,
            climateSliderBox,
            parallelExecutionCheckBox,
            threadSliderBox,
            spatialRangeTruncationCheckBox
        );
        optBox.setStyle("-fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-border-color: rgba(148, 163, 184, 0.2); -fx-border-radius: 6px; -fx-border-width: 1px;");
        optBox.visibleProperty().bind(strictDeterminismCheckBox.selectedProperty().not());
        optBox.managedProperty().bind(strictDeterminismCheckBox.selectedProperty().not());

        // Detail Inspector Card for selected/hovered Engine
        engineInspectorTitle = new Label(I18n.getOrDefault("scenario.header.engine_inspector", "🔎 Cliodynamic Engine Inspector (Hover an engine to inspect)"));
        engineInspectorTitle.getStyleClass().add("engine-inspector-title");
        
        engineInspectorText = new Label(I18n.getOrDefault("scenario.desc.engine_inspector", "Select or hover a Type A (Core) or Type B (Optional) engine to display its state equations, physical principles, and academic references."));
        engineInspectorText.setWrapText(true);
        engineInspectorText.getStyleClass().add("engine-inspector-text");

        engineInspectorEquationsTitle = new Label(I18n.getOrDefault("scenario.label.engine_equations", "📋 Mathematical Equations & Cliodynamic Formulation:"));
        engineInspectorEquationsTitle.getStyleClass().add("engine-inspector-equations-title");

        engineInspectorEquations = new Label("• Formulations mathématiques et bilans de conservation affichés dynamiquement.");
        engineInspectorEquations.setWrapText(true);
        engineInspectorEquations.getStyleClass().add("engine-inspector-equations");

        engineInspectorRef = new Label(I18n.getOrDefault("scenario.label.engine_refs", "📚 Scientific references and academic literature."));
        engineInspectorRef.setWrapText(true);
        engineInspectorRef.getStyleClass().add("hint-label");

        exportSelectedEngineBtn = new Button(I18n.getOrDefault("scenario.btn.export_engine_template", "📤 Export Custom Engine Template (.java)"));
        exportSelectedEngineBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 4;");
        exportSelectedEngineBtn.setOnAction(e -> exportCustomEngineTemplate());

        VBox engineInspectorCard = new VBox(5, engineInspectorTitle, engineInspectorText, engineInspectorEquationsTitle, engineInspectorEquations, engineInspectorRef, exportSelectedEngineBtn);
        engineInspectorCard.getStyleClass().add("engine-inspector-card");
        engineInspectorCard.setMinHeight(160);

        // --- 🔒 ETHER CORE ENGINES SECTION (Tier 1: 24 Permanent Invariant Engines) ---
        VBox typeABox = new VBox(6);
        typeABox.getStyleClass().add("card-section");

        exportCoreTemplateBtn = new Button(I18n.getOrDefault("scenario.btn.export_law_engine", "📤 Export Physical Law Engine (.java)"));
        exportCoreTemplateBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 4;");
        exportCoreTemplateBtn.setOnAction(e -> exportPhysicalLawEngineTemplate("PhysicalLawEngine"));

        coreExplanationLabel = new Label(I18n.getOrDefault("scenario.info.core_engines", "ℹ️ Why are Ether Core engines permanent? They enforce physical conservation laws (mass & energy, thermodynamics, hydrology, H3 insolation, metabolism) required for basic world survival."));
        coreExplanationLabel.getStyleClass().add("control-note");
        coreExplanationLabel.setWrapText(true);
        typeABox.getChildren().addAll(exportCoreTemplateBtn, coreExplanationLabel);

        List<EngineMeta> coreEngines = ScenarioEngineCatalog.getCoreEngines();
        coreEngineRows.clear();
        for (EngineMeta eng : coreEngines) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label iconTitle = new Label("🔒 " + eng.id());
            iconTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            String title = I18n.getEngineTitle(eng.id(), eng.defaultTitle());
            Label descLbl = new Label("— " + title);
            descLbl.setStyle("-fx-font-size: 10px;");
            HBox.setHgrow(descLbl, Priority.ALWAYS);
            row.getChildren().addAll(iconTitle, descLbl);

            String desc = I18n.getEngineDescription(eng.id(), eng.defaultDesc());
            String ref = I18n.getEngineReference(eng.id(), eng.defaultRef());
            String eqText = eng.defaultEq() != null ? I18n.getEngineEquation(eng.id(), eng.defaultEq()) : I18n.getOrDefault("scenario.engine.state_eq_default", "📋 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire");
            String permPrefix = I18n.getOrDefault("scenario.engine.perm_prefix", "🔒 [MOTEUR PERMANENT]\n");
            Tooltip tooltip = new Tooltip(permPrefix + eng.id() + " — " + title + "\n\n" + desc + "\n\n" + eqText + "\n\n📚 " + ref);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            Tooltip.install(row, tooltip);

            row.setOnMouseEntered(e -> {
                String curTitle = I18n.getEngineTitle(eng.id(), eng.defaultTitle());
                String curDesc = I18n.getEngineDescription(eng.id(), eng.defaultDesc());
                String curRef = I18n.getEngineReference(eng.id(), eng.defaultRef());
                String curEq = eng.defaultEq() != null ? I18n.getEngineEquation(eng.id(), eng.defaultEq()) : I18n.getOrDefault("scenario.engine.state_eq_default", "📋 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire");
                updateEngineInspector(eng.id(), curTitle, curDesc, curRef, curEq);
            });

            coreEngineRows.add(new CoreEngineRow(eng.id(), descLbl, row, eng.defaultTitle(), eng.defaultDesc(), eng.defaultRef(), eng.defaultEq()));
            typeABox.getChildren().add(row);
        }

        corePane = new TitledPane(I18n.getOrDefault("scenario.header.core_engines", "🔒 ARCHITECTURE CŒUR ETHER (24 MOTEURS PERMANENTS)"), typeABox);
        corePane.setExpanded(false);
        corePane.getStyleClass().add("titled-pane-primary");

        // --- ⚙️ OPTIONAL & CUSTOM ENGINES SECTION (Tier 2: 68 Hypotheses & Extensible Plugins) ---
        typeBBoxContainer = new VBox(8);
        typeBBoxContainer.getStyleClass().add("custom-module-card");

        exportTemplateBtn = new Button(I18n.getOrDefault("scenario.btn.export_java_template", "📤 Export Java Engine Template (.java)"));
        exportTemplateBtn.setStyle("-fx-background-color: #4c1d95; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        exportTemplateBtn.setOnAction(e -> exportCustomEngineTemplate());

        importEngineBtn = new Button(I18n.getOrDefault("scenario.btn.import_compile_engine", "📥 Import & Compile Java Engine (.java / .class)"));
        importEngineBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        importEngineBtn.setOnAction(e -> importCustomEngineFile());

        autoSelectEnginesForYearBtn = new Button(I18n.getOrDefault("scenario.btn.autoselect_engines", "⚡ Check Engines Based on T₀"));
        autoSelectEnginesForYearBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");

        HBox importExportBox = new HBox(10, autoSelectEnginesForYearBtn, exportTemplateBtn, importEngineBtn);
        importExportBox.setAlignment(Pos.CENTER_LEFT);
        importExportBox.setStyle("-fx-padding: 0 0 8 0;");
        typeBBoxContainer.getChildren().add(importExportBox);

        engineSortCombo = new ComboBox<>();
        engineSortCombo.getItems().addAll(
            I18n.getOrDefault("scenario.sort.default", "⚙️ System Order (By Category)"),
            I18n.getOrDefault("scenario.sort.date_asc", "📅 Chronological Sort (Oldest → Newest)"),
            I18n.getOrDefault("scenario.sort.date_desc", "📅 Reverse Chronological Sort (Newest → Oldest)"),
            I18n.getOrDefault("scenario.sort.alpha_asc", "🔤 Alphabetical Sort (A - Z)")
        );
        engineSortCombo.setValue(I18n.getOrDefault("scenario.sort.default", "⚙️ System Order (By Category)"));
        engineSortCombo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        engineSortLabel = new Label(I18n.getOrDefault("scenario.sort.label", "🔀 Engine Sorting:"));
        engineSortLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        HBox sortBar = new HBox(8, engineSortLabel, engineSortCombo);
        sortBar.setAlignment(Pos.CENTER_LEFT);
        sortBar.setStyle("-fx-padding: 4 0 8 0;");
        typeBBoxContainer.getChildren().add(sortBar);

        List<EngineMeta> optionalEngines = ScenarioEngineCatalog.getOptionalEngines();
        typeBCheckBoxMap.clear();
        typeBParamSpinnersMap.clear();
        optionalEngineMetas.clear();
        engineContainers.clear();

        for (EngineMeta eng : optionalEngines) {
            String engineTitle = I18n.getEngineTitle(eng.id(), eng.defaultTitle());
            CheckBox cb = new CheckBox(engineTitle);
            cb.setSelected("FrontierAsabiyyahEngine".equals(eng.id()));
            cb.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

            String desc = I18n.getEngineDescription(eng.id(), eng.defaultDesc());
            String ref = I18n.getEngineReference(eng.id(), eng.defaultRef());
            String eqText = eng.defaultEq() != null ? I18n.getEngineEquation(eng.id(), eng.defaultEq()) : I18n.getOrDefault("scenario.engine.state_eq_default", "📋 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire");
            String optPrefix = I18n.getOrDefault("scenario.engine.opt_prefix", "⚙️ [MOTEUR OPTIONNEL]\n");
            Tooltip tooltip = new Tooltip(optPrefix + eng.id() + " — " + engineTitle + "\n\n" + desc + "\n\n" + eqText + "\n\n📚 " + ref);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            cb.setTooltip(tooltip);
            cb.setOnAction(e -> notifyParamChange());

            typeBCheckBoxMap.put(eng.id(), cb);

            HBox row = new HBox(8, cb);
            row.setAlignment(Pos.CENTER_LEFT);

            row.setOnMouseEntered(e -> {
                String curTitle = I18n.getEngineTitle(eng.id(), eng.defaultTitle());
                String curDesc = I18n.getEngineDescription(eng.id(), eng.defaultDesc());
                String curRef = I18n.getEngineReference(eng.id(), eng.defaultRef());
                String curEq = eng.defaultEq() != null ? I18n.getEngineEquation(eng.id(), eng.defaultEq()) : I18n.getOrDefault("scenario.engine.state_eq_default", "📋 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire");
                updateEngineInspector(eng.id(), curTitle, curDesc, curRef, curEq);
            });
            cb.setOnMouseEntered(row.getOnMouseEntered());

            optionalEngineMetas.add(new OptionalEngineMeta(eng.id(), cb, eng.defaultTitle(), eng.defaultDesc(), eng.defaultRef(), eng.defaultEq()));

            VBox engContainer = new VBox(4, row);
            javafx.scene.Node paramBox = createEngineParameterBox(eng.id());
            if (paramBox != null) {
                paramBox.visibleProperty().bind(cb.selectedProperty());
                paramBox.managedProperty().bind(cb.selectedProperty());
                engContainer.getChildren().add(paramBox);
            }

            engineContainers.put(eng.id(), engContainer);
            typeBBoxContainer.getChildren().add(engContainer);
        }

        engineSortCombo.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) -> {
            sortOptionalEngines(newV != null ? newV.intValue() : 0);
        });

        this.typeBPane = new TitledPane(String.format(I18n.getOrDefault("scenario.header.opt_engines_format", "⚙️ MODULES OPTIONNELS (%d MOTEURS EXTENSIBLES & IMPORT/EXPORT)"), optionalEngines.size()), typeBBoxContainer);
        this.typeBPane.setExpanded(true);
        this.typeBPane.getStyleClass().add("titled-pane-secondary");

        section.getChildren().addAll(oceanOptHeader, oceanOptDesc, masterBox, optBox, engineInspectorCard, corePane, this.typeBPane);
        return section;
    }

    private void notifyParamChange() {
        if (onParamChangedCallback != null) {
            onParamChangedCallback.run();
        }
    }

    /**
     * Updates the inspector card display for an engine.
     *
     * @param className simple Java class name
     * @param title localized human-readable engine title
     * @param description mechanism and biophysical scope
     * @param references academic references
     * @param equations KaTeX formulas
     */
    public void updateEngineInspector(String className, String title, String description, String references, String equations) {
        this.selectedEngineClassName = className;
        if (engineInspectorTitle != null) {
            engineInspectorTitle.setText("🔎 " + className + " — " + title);
        }
        if (engineInspectorText != null) {
            engineInspectorText.setText(description);
        }
        if (engineInspectorRef != null) {
            engineInspectorRef.setText("📚 " + references);
        }
        if (engineInspectorEquations != null) {
            engineInspectorEquations.setText(equations != null && !equations.isBlank() ? equations : "• Formulation mathématique : dX/dt = f(X, t) + Σ F_inter-cellulaire");
        }
        if (exportSelectedEngineBtn != null) {
            exportSelectedEngineBtn.setText(I18n.getOrDefault("scenario.btn.export_prefix", "📤 Export ") + className + ".java");
            exportSelectedEngineBtn.setVisible(true);
            exportSelectedEngineBtn.setManaged(true);
        }
        if (onEngineHoveredCallback != null) {
            onEngineHoveredCallback.accept(className);
        }
    }

    /**
     * Checks or unchecks optional engines according to simulation starting year.
     *
     * @param year simulation starting year (BP / CE)
     */
    public void autoSelectEnginesForYear(long year) {
        for (Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
            long appYear = ScenarioEngineCatalog.getEngineApparitionYear(entry.getKey());
            entry.getValue().setSelected(year >= appYear);
        }
        notifyParamChange();
    }

    /**
     * Sorts the Type B engine rows in the UI container.
     *
     * @param sortIdx sort mode index (0: Category, 1: Chronological asc, 2: Chronological desc, 3: Alphabetical)
     */
    public void sortOptionalEngines(int sortIdx) {
        if (typeBBoxContainer == null) return;
        List<javafx.scene.Node> headers = new ArrayList<>();
        if (typeBBoxContainer.getChildren().size() >= 2) {
            headers.add(typeBBoxContainer.getChildren().get(0));
            headers.add(typeBBoxContainer.getChildren().get(1));
        }

        List<EngineMeta> sorted = new ArrayList<>(ScenarioEngineCatalog.getOptionalEngines());
        if (sortIdx == 1) { // Chronological (Oldest -> Newest)
            sorted.sort(Comparator.comparingLong(e -> ScenarioEngineCatalog.getEngineApparitionYear(e.id())));
        } else if (sortIdx == 2) { // Reverse Chronological (Newest -> Oldest)
            sorted.sort((a, b) -> Long.compare(ScenarioEngineCatalog.getEngineApparitionYear(b.id()), ScenarioEngineCatalog.getEngineApparitionYear(a.id())));
        } else if (sortIdx == 3) { // Alphabetical (A - Z)
            sorted.sort(Comparator.comparing(e -> I18n.getEngineTitle(e.id(), e.defaultTitle())));
        }

        typeBBoxContainer.getChildren().clear();
        typeBBoxContainer.getChildren().addAll(headers);
        for (EngineMeta eng : sorted) {
            VBox box = engineContainers.get(eng.id());
            if (box != null) {
                typeBBoxContainer.getChildren().add(box);
            }
        }
    }

    /**
     * Creates parameter spinner tuning box for specific cliodynamic engines.
     *
     * @param engineKey engine unique key
     * @return GridPane parameter node or null
     */
    private javafx.scene.Node createEngineParameterBox(String engineKey) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setStyle("-fx-padding: 4 8 8 24; -fx-background-color: rgba(30, 41, 59, 0.4); -fx-background-radius: 4;");

        Map<String, Spinner<Double>> spinners = new HashMap<>();

        if ("MaritimeHighwayEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "capitalBoostRate", "Capital Boost Rate:", 0.05, 0.00, 0.20, 0.01,
                    "Taux d'accroissement du capital par le commerce maritime");
            addDoubleParamSpinner(grid, spinners, 1, "frictionMultiplier", "Friction Multiplier:", 0.20, 0.05, 1.00, 0.05,
                    "Multiplicateur de friction sur l'eau");
        } else if ("HydrologicalEngineeringEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "tenochtitlanTech", "Tech Min Drainage Lacs:", 3.5, 1.0, 10.0, 0.5,
                    "Niveau technique minimum pour assécher un lac urbain");
            addDoubleParamSpinner(grid, spinners, 1, "tenochtitlanCapital", "Capital Min Drainage (kg):", 150.0, 10.0, 1000.0, 50.0,
                    "Capital par habitant requis pour le drainage");
            addDoubleParamSpinner(grid, spinners, 2, "aralRainfallThreshold", "Seuil Pluie Aral (mm):", 300.0, 50.0, 1000.0, 50.0,
                    "Seuil pluviométrique sous lequel l'assèchement démarre");
            addDoubleParamSpinner(grid, spinners, 3, "damMinElevation", "Altitude Min Barrage (m):", 300.0, 50.0, 2000.0, 50.0,
                    "Altitude minimale pour construire un barrage de montagne");
        } else if ("FertileCrescentSalinizationEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "salinizationRate", "Taux Salinisation / Siècle:", 0.02, 0.001, 0.10, 0.005,
                    "Vitesse d'accumulation des sels par irrigation intensive");
        } else if ("LandReclamationEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "polderTechThreshold", "Tech Seuil Poldérisation:", 4.0, 1.0, 10.0, 0.5,
                    "Niveau technique requis pour conquérir des terres sur la mer");
            addDoubleParamSpinner(grid, spinners, 1, "polderCapitalThreshold", "Capital Seuil Polders (kg):", 100.0, 10.0, 500.0, 20.0,
                    "Capital d'infrastructure minimum requis pour les digues");
        }

        if (!spinners.isEmpty()) {
            typeBParamSpinnersMap.put(engineKey, spinners);
            return grid;
        }
        return null;
    }

    private void addDoubleParamSpinner(GridPane grid, Map<String, Spinner<Double>> spinners, int row,
                                       String paramKey, String labelText, double defVal, double min, double max, double step, String tooltip) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        Spinner<Double> sp = new Spinner<>(min, max, defVal, step);
        sp.setEditable(true);
        sp.setPrefWidth(90);
        sp.setStyle("-fx-font-size: 10px;");
        sp.setTooltip(new Tooltip(tooltip));
        sp.valueProperty().addListener((obs, oldV, newV) -> notifyParamChange());
        UIControlsHelper.attachDefaultValueHandling(sp, defVal, () -> sp.getValueFactory().setValue(defVal), onParamChangedCallback);
        grid.add(lbl, 0, row);
        grid.add(sp, 1, row);
        spinners.put(paramKey, sp);
    }

    /**
     * Applies scenario engine architecture and performance parameters to UI controls.
     *
     * @param s scenario model
     */
    public void applyScenario(Scenario s) {
        if (s == null) return;

        if (strictDeterminismCheckBox != null) {
            strictDeterminismCheckBox.setSelected(s.isStrictDeterminism());
        }
        if (sparseCellSkippingCheckBox != null) sparseCellSkippingCheckBox.setSelected(s.isSparseCellSkippingEnabled());
        if (oceanMacroAggregationCheckBox != null) oceanMacroAggregationCheckBox.setSelected(s.isOceanMacroAggregationEnabled());
        if (coastalNavigationOnlyCheckBox != null) coastalNavigationOnlyCheckBox.setSelected(s.isCoastalNavigationOnlyEnabled());
        if (oceanMultiRateTickingCheckBox != null) oceanMultiRateTickingCheckBox.setSelected(s.isOceanMultiRateTickingEnabled());
        if (climateTickFreqSlider != null) climateTickFreqSlider.setValue(s.getClimateTickFrequency());
        if (parallelExecutionCheckBox != null) parallelExecutionCheckBox.setSelected(s.isParallelExecutionEnabled());
        if (threadCountSlider != null) threadCountSlider.setValue(s.getParallelThreadCount());
        if (spatialRangeTruncationCheckBox != null) spatialRangeTruncationCheckBox.setSelected(s.isSpatialRangeTruncationEnabled());

        Map<String, Boolean> activeEngines = s.getTypeBEngineStates();
        if (activeEngines != null) {
            for (Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
                if (activeEngines.containsKey(entry.getKey())) {
                    entry.getValue().setSelected(activeEngines.get(entry.getKey()));
                }
            }
        }

        Map<String, Map<String, Double>> params = s.getTypeBEngineParameters();
        if (params != null) {
            for (Map.Entry<String, Map<String, Double>> engEntry : params.entrySet()) {
                Map<String, Spinner<Double>> spinMap = typeBParamSpinnersMap.get(engEntry.getKey());
                if (spinMap != null) {
                    for (Map.Entry<String, Double> pEntry : engEntry.getValue().entrySet()) {
                        Spinner<Double> sp = spinMap.get(pEntry.getKey());
                        if (sp != null && pEntry.getValue() != null) {
                            sp.getValueFactory().setValue(pEntry.getValue());
                        }
                    }
                }
            }
        }
        updatePerformanceControlsState();
    }

    /**
     * Exports active engine configurations and performance settings into target Scenario.
     *
     * @param s scenario model to populate
     */
    public void exportToScenario(Scenario s) {
        if (s == null) return;
        boolean strict = strictDeterminismCheckBox != null && strictDeterminismCheckBox.isSelected();
        s.setStrictDeterminism(strict);

        if (sparseCellSkippingCheckBox != null) s.setSparseCellSkippingEnabled(sparseCellSkippingCheckBox.isSelected());
        if (oceanMacroAggregationCheckBox != null) s.setOceanMacroAggregationEnabled(oceanMacroAggregationCheckBox.isSelected());
        if (coastalNavigationOnlyCheckBox != null) s.setCoastalNavigationOnlyEnabled(coastalNavigationOnlyCheckBox.isSelected());
        if (oceanMultiRateTickingCheckBox != null) s.setOceanMultiRateTickingEnabled(oceanMultiRateTickingCheckBox.isSelected());
        if (climateTickFreqSlider != null) s.setClimateTickFrequency((int) climateTickFreqSlider.getValue());
        if (parallelExecutionCheckBox != null) s.setParallelExecutionEnabled(parallelExecutionCheckBox.isSelected());
        if (threadCountSlider != null) s.setParallelThreadCount((int) threadCountSlider.getValue());
        if (spatialRangeTruncationCheckBox != null) s.setSpatialRangeTruncationEnabled(spatialRangeTruncationCheckBox.isSelected());

        Map<String, Boolean> typeBStates = new HashMap<>();
        for (Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
            typeBStates.put(entry.getKey(), entry.getValue().isSelected());
        }
        s.setTypeBEngineStates(typeBStates);

        Map<String, Map<String, Double>> engineParams = new HashMap<>();
        for (Map.Entry<String, Map<String, Spinner<Double>>> engEntry : typeBParamSpinnersMap.entrySet()) {
            Map<String, Double> pMap = new HashMap<>();
            for (Map.Entry<String, Spinner<Double>> pEntry : engEntry.getValue().entrySet()) {
                if (pEntry.getValue() != null && pEntry.getValue().getValue() != null) {
                    pMap.put(pEntry.getKey(), pEntry.getValue().getValue());
                }
            }
            if (!pMap.isEmpty()) {
                engineParams.put(engEntry.getKey(), pMap);
            }
        }
        s.setTypeBEngineParameters(engineParams);
    }

    /**
     * Updates all localized UI texts across the engine architecture section.
     */
    public void updateTexts() {
        if (strictDeterminismCheckBox != null) {
            strictDeterminismCheckBox.setText(I18n.getOrDefault("scenario.opt.strict_determinism", "🔒 STRICT DETERMINISM MODE (0% Approximation / 100% Bit-to-Bit Reproducibility)"));
        }
        if (optSubHeader != null) {
            optSubHeader.setText(I18n.getOrDefault("scenario.opt.sub_header", "⚡ ALGORITHMIC OPTIMIZATIONS & PERFORMANCE SHORTCUTS:"));
        }
        if (sparseCellSkippingCheckBox != null) {
            sparseCellSkippingCheckBox.setText(I18n.getOrDefault("scenario.opt.sparse_cell_skipping", "🦗 Sparse / Uninhabited Cell Skipping (Deserts & Abysses)"));
        }
        if (oceanMacroAggregationCheckBox != null) {
            oceanMacroAggregationCheckBox.setText(I18n.getOrDefault("scenario.ocean_opt.macro_aggregation", "🌊 Abyssal Ocean Macro-Aggregation (Deep Basins z < -200m in Blocks)"));
        }
        if (coastalNavigationOnlyCheckBox != null) {
            coastalNavigationOnlyCheckBox.setText(I18n.getOrDefault("scenario.ocean_opt.coastal_nav", "⚓ Exclusive Coastal Navigation (Pathfinding Focused on Coasts & Straits)"));
        }
        if (oceanMultiRateTickingCheckBox != null) {
            oceanMultiRateTickingCheckBox.setText(I18n.getOrDefault("scenario.ocean_opt.multi_rate_ticking", "⏱ Oceanic & Multi-Rate Climate Ticking (Updated Every N Ticks)"));
        }
        if (parallelExecutionCheckBox != null) {
            parallelExecutionCheckBox.setText(I18n.getOrDefault("scenario.opt.parallel_execution", "🚀 Async Multi-Thread Parallelization (CompletableFuture / AVX)"));
        }
        if (spatialRangeTruncationCheckBox != null) {
            spatialRangeTruncationCheckBox.setText(I18n.getOrDefault("scenario.opt.spatial_truncation", "💨 Spatial Range Truncation of Plumes & Diffusions (10⁻⁶ Cutoff)"));
        }

        // Refresh Core engines
        for (CoreEngineRow row : coreEngineRows) {
            String title = I18n.getEngineTitle(row.id, row.defaultTitle);
            row.descLabel.setText("— " + title);
            String desc = I18n.getEngineDescription(row.id, row.defaultDesc);
            String ref = I18n.getEngineReference(row.id, row.defaultRef);
            String eqText = row.defaultEq != null ? I18n.getEngineEquation(row.id, row.defaultEq) : I18n.getOrDefault("scenario.engine.state_eq_default", "📋 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire");
            String permPrefix = I18n.getOrDefault("scenario.engine.perm_prefix", "🔒 [MOTEUR PERMANENT]\n");
            Tooltip tooltip = new Tooltip(permPrefix + row.id + " — " + title + "\n\n" + desc + "\n\n" + eqText + "\n\n📚 " + ref);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            Tooltip.install(row.row, tooltip);
        }

        // Refresh Optional engines
        for (OptionalEngineMeta meta : optionalEngineMetas) {
            String title = I18n.getEngineTitle(meta.id, meta.defaultTitle);
            meta.cb.setText(title);
            String desc = I18n.getEngineDescription(meta.id, meta.defaultDesc);
            String ref = I18n.getEngineReference(meta.id, meta.defaultRef);
            String eqText = meta.defaultEq != null ? I18n.getEngineEquation(meta.id, meta.defaultEq) : I18n.getOrDefault("scenario.engine.state_eq_default", "📋 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire");
            String optPrefix = I18n.getOrDefault("scenario.engine.opt_prefix", "⚙️ [MOTEUR OPTIONNEL]\n");
            Tooltip tooltip = new Tooltip(optPrefix + meta.id + " — " + title + "\n\n" + desc + "\n\n" + eqText + "\n\n📚 " + ref);
            tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
            meta.cb.setTooltip(tooltip);
        }

        if (corePane != null) {
            corePane.setText(I18n.getOrDefault("scenario.header.core_engines", "🔒 ARCHITECTURE CŒUR ETHER (24 MOTEURS PERMANENTS)"));
        }
        if (typeBPane != null) {
            typeBPane.setText(String.format(I18n.getOrDefault("scenario.header.opt_engines_format", "⚙️ MODULES OPTIONNELS (%d MOTEURS EXTENSIBLES & IMPORT/EXPORT)"), optionalEngineMetas.size()));
        }
    }

    public void updatePerformanceControlsState() {
        boolean strict = strictDeterminismCheckBox != null && strictDeterminismCheckBox.isSelected();
        if (sparseCellSkippingCheckBox != null) sparseCellSkippingCheckBox.setDisable(strict);
        if (oceanMacroAggregationCheckBox != null) oceanMacroAggregationCheckBox.setDisable(strict);
        if (coastalNavigationOnlyCheckBox != null) coastalNavigationOnlyCheckBox.setDisable(strict);
        if (oceanMultiRateTickingCheckBox != null) oceanMultiRateTickingCheckBox.setDisable(strict);
        if (climateTickFreqSlider != null) climateTickFreqSlider.setDisable(strict);
        if (parallelExecutionCheckBox != null) parallelExecutionCheckBox.setDisable(strict);
        if (threadCountSlider != null) threadCountSlider.setDisable(strict);
        if (spatialRangeTruncationCheckBox != null) spatialRangeTruncationCheckBox.setDisable(strict);
    }

    /**
     * Dynamically registers a hot-compiled custom engine into the UI.
     *
     * @param engineKey engine class name
     * @param labelText human-readable label
     * @param tooltipText description tooltip
     * @param defaultSelected whether to select immediately
     */
    public void addCustomEngineCheckBoxToUI(String engineKey, String labelText, String tooltipText, boolean defaultSelected) {
        if (typeBCheckBoxMap.containsKey(engineKey)) {
            typeBCheckBoxMap.get(engineKey).setSelected(defaultSelected);
            return;
        }

        CheckBox cb = new CheckBox(labelText);
        cb.setSelected(defaultSelected);
        cb.getStyleClass().add("custom-engine-checkbox");
        cb.setTooltip(new Tooltip(tooltipText));
        cb.setOnAction(e -> notifyParamChange());

        typeBCheckBoxMap.put(engineKey, cb);

        Label badge = new Label(I18n.getOrDefault("scenario.badge.custom", "[🔌 CUSTOM]"));
        badge.getStyleClass().add("custom-engine-badge");

        HBox row = new HBox(8, badge, cb);
        row.setAlignment(Pos.CENTER_LEFT);

        if (typeBBoxContainer != null) {
            typeBBoxContainer.getChildren().add(row);
        }
    }

    private void exportPhysicalLawEngineTemplate(String className) {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.title.export_law_dialog", "Export Physical Law Engine (.java)"));
            fileChooser.setInitialFileName(className + ".java");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Java Source (*.java)", "*.java"));
            java.io.File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                String template = """
                    package org.ether.society.engines.tier1;

                    import org.ether.society.core.H3SimulationEngine;
                    import org.ether.society.database.H3Cell;

                    /**
                     * Physical Law Engine Core: %s
                     */
                    public class %s {
                        public void update(H3SimulationEngine engine, H3Cell cell, double deltaTime) {
                            // Thermodynamic conservation & physical balance laws
                        }
                    }
                    """.formatted(className, className);
                java.nio.file.Files.writeString(file.toPath(), template);
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        I18n.getOrDefault("scenario.alert.export_law_success", "Physical Law Engine template exported successfully:\n") + file.getAbsolutePath());
                alert.show();
            }
        } catch (Exception ex) {
            logger.error("Failed to export physical law engine template", ex);
        }
    }

    private void exportCustomEngineTemplate() {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.title.export_engine_dialog", "Export Custom Ether Engine Template (.java)"));
            fileChooser.setInitialFileName("MyCustomOptionalEngine.java");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Java Source (*.java)", "*.java"));
            java.io.File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                String template = org.ether.society.engines.compiler.DynamicEngineCompiler.generateEngineTemplateCode("MyCustomOptionalEngine");
                java.nio.file.Files.writeString(file.toPath(), template);
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        I18n.getOrDefault("scenario.alert.export_template_success", "Custom engine template exported successfully:\n") + file.getAbsolutePath());
                alert.show();
            }
        } catch (Exception ex) {
            logger.error("Error exporting custom engine template", ex);
        }
    }

    private void importCustomEngineFile() {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle(I18n.getOrDefault("scenario.title.import_engine_dialog", "Import Custom Engine (.java)"));
            fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Java Source (*.java)", "*.java")
            );
            java.io.File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                org.ether.society.engines.compiler.DynamicEngineCompiler.CompilationResult result =
                        org.ether.society.engines.compiler.DynamicEngineCompiler.compileAndLoadEngine(file);

                if (result.success()) {
                    String engineKey = result.engineName();
                    addCustomEngineCheckBoxToUI(engineKey, "🔌 " + engineKey + " (" + I18n.getOrDefault("scenario.engine.custom_badge", "Custom Engine") + ")",
                            result.message(), true);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, result.message());
                    alert.show();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, result.message());
                    alert.show();
                }
            }
        } catch (Exception ex) {
            logger.error("Error importing custom engine file", ex);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Import error: " + ex.getMessage());
            alert.show();
        }
    }

    public boolean isStrictDeterminism() {
        return strictDeterminismCheckBox != null && strictDeterminismCheckBox.isSelected();
    }

    public Map<String, CheckBox> getTypeBCheckBoxMap() {
        return typeBCheckBoxMap;
    }

    public Map<String, Map<String, Spinner<Double>>> getTypeBParamSpinnersMap() {
        return typeBParamSpinnersMap;
    }

    public CheckBox getStrictDeterminismCheckBox() {
        return strictDeterminismCheckBox;
    }

    public Slider getClimateTickFreqSlider() {
        return climateTickFreqSlider;
    }

    public Slider getThreadCountSlider() {
        return threadCountSlider;
    }

    public Button getAutoSelectEnginesForYearBtn() {
        return autoSelectEnginesForYearBtn;
    }
}
