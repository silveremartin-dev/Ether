/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.ether.society.config.SimulationPerformanceConfig;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.util.UIControlsHelper;

import java.util.List;

/**
 * <h2>Scenario Performance Options Section</h2>
 * <p>
 * Modular UI builder for simulation performance optimizations, approximations,
 * multi-threaded execution, and strict academic bit-to-bit determinism toggle.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class ScenarioPerformanceOptionsSection {

    private final Runnable onParamChangedCallback;

    private Label oceanOptHeader;
    private Label oceanOptDesc;
    private CheckBox strictDeterminismCheckBox;
    private Label optSubHeader;

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

    /**
     * Constructs the performance options section.
     *
     * @param onParamChangedCallback callback invoked whenever an option changes
     */
    public ScenarioPerformanceOptionsSection(Runnable onParamChangedCallback) {
        this.onParamChangedCallback = onParamChangedCallback;
    }

    /**
     * Builds and returns the container VBox containing determinism and performance controls.
     *
     * @return JavaFX VBox container
     */
    public VBox buildPerformanceSection() {
        oceanOptHeader = new Label(I18n.getOrDefault("scenario.ocean_opt.header", "⚡ ACCÉLÉRATION & APPROXIMATIONS DE CALCUL (OPTIONS DE PERFORMANCE)"));
        oceanOptHeader.getStyleClass().add("card-header-muted");

        oceanOptDesc = new Label(I18n.getOrDefault("scenario.ocean_opt.desc",
            "Activez les raccourcis algorithmiques pour accélérer la simulation sur de très grandes grilles mondiales. " +
            "Le mode Déterminisme Strict désactive toutes les approximations pour garantir une reproductibilité bit-à-bit absolue."));
        oceanOptDesc.getStyleClass().add("card-description-muted");
        oceanOptDesc.setWrapText(true);

        // --- MASTER CONTROL : STRICT DETERMINISM MODE ---
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

        // --- INDIVIDUAL OPTIMIZATIONS & APPROXIMATIONS ---
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
                    cb.setDisable(true);
                }
                climateTickFreqSlider.setDisable(true);
                climateTickFreqSlider.setValue(1);
                parallelExecutionCheckBox.setSelected(false);
                parallelExecutionCheckBox.setDisable(true);
                threadCountSlider.setDisable(true);
                threadCountSlider.setValue(1);
                spatialRangeTruncationCheckBox.setSelected(false);
                spatialRangeTruncationCheckBox.setDisable(true);
            } else {
                for (CheckBox cb : subOpts) {
                    cb.setDisable(false);
                }
                climateTickFreqSlider.setDisable(false);
                parallelExecutionCheckBox.setDisable(false);
                threadCountSlider.setDisable(false);
                spatialRangeTruncationCheckBox.setDisable(false);
            }
            notifyParamChange();
        });

        // Initialize disabled states if strict determinism starts selected
        if (strictDeterminismCheckBox.isSelected()) {
            for (CheckBox cb : subOpts) {
                cb.setDisable(true);
            }
            climateTickFreqSlider.setDisable(true);
            parallelExecutionCheckBox.setDisable(true);
            threadCountSlider.setDisable(true);
            spatialRangeTruncationCheckBox.setDisable(true);
        }

        VBox optBox = new VBox(6,
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
        optBox.getStyleClass().add("opt-sub-box");

        return new VBox(10, oceanOptHeader, oceanOptDesc, masterBox, optBox);
    }

    private void notifyParamChange() {
        if (onParamChangedCallback != null) {
            onParamChangedCallback.run();
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

    public void applyScenario(Scenario s) {
        if (s == null) return;
        if (strictDeterminismCheckBox != null) strictDeterminismCheckBox.setSelected(s.isStrictDeterminism());
        if (sparseCellSkippingCheckBox != null) sparseCellSkippingCheckBox.setSelected(s.isSparseCellSkippingEnabled());
        if (oceanMacroAggregationCheckBox != null) oceanMacroAggregationCheckBox.setSelected(s.isOceanMacroAggregationEnabled());
        if (coastalNavigationOnlyCheckBox != null) coastalNavigationOnlyCheckBox.setSelected(s.isCoastalNavigationOnlyEnabled());
        if (oceanMultiRateTickingCheckBox != null) oceanMultiRateTickingCheckBox.setSelected(s.isOceanMultiRateTickingEnabled());
        if (climateTickFreqSlider != null) climateTickFreqSlider.setValue(s.getClimateTickFrequency());
        if (parallelExecutionCheckBox != null) parallelExecutionCheckBox.setSelected(s.isParallelExecutionEnabled());
        if (threadCountSlider != null) threadCountSlider.setValue(s.getParallelThreadCount());
        if (spatialRangeTruncationCheckBox != null) spatialRangeTruncationCheckBox.setSelected(s.isSpatialRangeTruncationEnabled());
        updatePerformanceControlsState();
    }

    public void exportToScenario(Scenario s) {
        if (s == null) return;
        s.setStrictDeterminism(isStrictDeterminism());
        if (sparseCellSkippingCheckBox != null) s.setSparseCellSkippingEnabled(sparseCellSkippingCheckBox.isSelected());
        if (oceanMacroAggregationCheckBox != null) s.setOceanMacroAggregationEnabled(oceanMacroAggregationCheckBox.isSelected());
        if (coastalNavigationOnlyCheckBox != null) s.setCoastalNavigationOnlyEnabled(coastalNavigationOnlyCheckBox.isSelected());
        if (oceanMultiRateTickingCheckBox != null) s.setOceanMultiRateTickingEnabled(oceanMultiRateTickingCheckBox.isSelected());
        if (climateTickFreqSlider != null) s.setClimateTickFrequency((int) climateTickFreqSlider.getValue());
        if (parallelExecutionCheckBox != null) s.setParallelExecutionEnabled(parallelExecutionCheckBox.isSelected());
        if (threadCountSlider != null) s.setParallelThreadCount((int) threadCountSlider.getValue());
        if (spatialRangeTruncationCheckBox != null) s.setSpatialRangeTruncationEnabled(spatialRangeTruncationCheckBox.isSelected());
    }

    public void updateTexts() {
        if (oceanOptHeader != null) oceanOptHeader.setText(I18n.getOrDefault("scenario.ocean_opt.header", "⚡ ACCÉLÉRATION & APPROXIMATIONS DE CALCUL (OPTIONS DE PERFORMANCE)"));
        if (oceanOptDesc != null) oceanOptDesc.setText(I18n.getOrDefault("scenario.ocean_opt.desc",
            "Activez les raccourcis algorithmiques pour accélérer la simulation sur de très grandes grilles mondiales. " +
            "Le mode Déterminisme Strict désactive toutes les approximations pour garantir une reproductibilité bit-à-bit absolue."));
        if (strictDeterminismCheckBox != null) strictDeterminismCheckBox.setText(I18n.getOrDefault("scenario.opt.strict_determinism", "🔒 STRICT DETERMINISM MODE (0% Approximation / 100% Bit-to-Bit Reproducibility)"));
        if (optSubHeader != null) optSubHeader.setText(I18n.getOrDefault("scenario.opt.sub_header", "⚡ ALGORITHMIC OPTIMIZATIONS & PERFORMANCE SHORTCUTS:"));
        if (sparseCellSkippingCheckBox != null) sparseCellSkippingCheckBox.setText(I18n.getOrDefault("scenario.opt.sparse_cell_skipping", "🦗 Sparse / Uninhabited Cell Skipping (Deserts & Abysses)"));
        if (oceanMacroAggregationCheckBox != null) oceanMacroAggregationCheckBox.setText(I18n.getOrDefault("scenario.ocean_opt.macro_aggregation", "🌊 Abyssal Ocean Macro-Aggregation (Deep Basins z < -200m in Blocks)"));
        if (coastalNavigationOnlyCheckBox != null) coastalNavigationOnlyCheckBox.setText(I18n.getOrDefault("scenario.ocean_opt.coastal_nav", "⚓ Exclusive Coastal Navigation (Pathfinding Focused on Coasts & Straits)"));
        if (oceanMultiRateTickingCheckBox != null) oceanMultiRateTickingCheckBox.setText(I18n.getOrDefault("scenario.ocean_opt.multi_rate_ticking", "⏱ Oceanic & Multi-Rate Climate Ticking (Updated Every N Ticks)"));
        if (parallelExecutionCheckBox != null) parallelExecutionCheckBox.setText(I18n.getOrDefault("scenario.opt.parallel_execution", "🚀 Async Multi-Thread Parallelization (CompletableFuture / AVX)"));
        if (spatialRangeTruncationCheckBox != null) spatialRangeTruncationCheckBox.setText(I18n.getOrDefault("scenario.opt.spatial_truncation", "💨 Spatial Range Truncation of Plumes & Diffusions (10⁻⁶ Cutoff)"));
    }

    public boolean isStrictDeterminism() {
        return strictDeterminismCheckBox == null || strictDeterminismCheckBox.isSelected();
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
}
