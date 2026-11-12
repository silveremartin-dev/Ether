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
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.scenario.ScenarioEngineRegistry.EngineMeta;
import org.ether.society.ui.scenario.ScenarioEngineParamControlsHelper.CoreEngineRow;
import org.ether.society.ui.scenario.ScenarioEngineParamControlsHelper.OptionalEngineMeta;

import java.util.*;
import java.util.function.Consumer;

/**
 * <h2>Scenario Engine Config Section Builder</h2>
 * <p>
 * Modular UI builder for configuring simulation engine architecture:
 * <ul>
 *   <li>Tier 1: 24 Permanent Core Physical Law Engines (Mass, thermodynamics, hydrology, energetics)</li>
 *   <li>Tier 2: Optional/Hypothetical Cliodynamic &amp; Macroeconomic Engines (Boserup, Turchin SDT, Smil, Ostrom)</li>
 *   <li>Performance Optimizations &amp; Strict Determinism (Delegated to {@link ScenarioPerformanceOptionsSection})</li>
 *   <li>Dynamic Custom Engine Compiler &amp; Import/Export (Delegated to {@link ScenarioCustomEngineHandler})</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class ScenarioEngineConfigSectionBuilder {

    private final Runnable onParamChangedCallback;
    private final Consumer<String> onEngineHoveredCallback;

    // Extracted modules
    private final ScenarioPerformanceOptionsSection performanceSection;
    private ScenarioCustomEngineHandler customEngineHandler;

    // Engine Inspector Card Components
    private Label engineInspectorTitle;
    private Label engineInspectorText;
    private Label engineInspectorEquationsTitle;
    private Label engineInspectorEquations;
    private Label engineInspectorRef;
    private Button exportSelectedEngineBtn;
    private Button exportCoreTemplateBtn;
    private Label coreExplanationLabel;
    private String selectedEngineClassName = "FrontierAsabiyyahEngine";

    // Core Tier 1 Engines
    private final List<CoreEngineRow> coreEngineRows = new ArrayList<>();
    private TitledPane corePane;

    // Optional Tier 2 Engines
    private final Map<String, CheckBox> typeBCheckBoxMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Spinner<Double>>> typeBParamSpinnersMap = new LinkedHashMap<>();
    private final List<OptionalEngineMeta> optionalEngineMetas = new ArrayList<>();
    private final Map<String, VBox> engineContainers = new LinkedHashMap<>();
    private VBox typeBBoxContainer;
    private TitledPane typeBPane;

    // Custom Engine controls
    private Button autoSelectEnginesForYearBtn;
    private Button exportTemplateBtn;
    private Button importEngineBtn;
    private ComboBox<String> engineSortCombo;
    private Label engineSortLabel;

    /**
     * Constructs the scenario engine configuration section builder.
     *
     * @param onParamChangedCallback callback invoked when parameters change
     * @param onEngineHoveredCallback callback invoked when an engine card is hovered
     */
    public ScenarioEngineConfigSectionBuilder(Runnable onParamChangedCallback, Consumer<String> onEngineHoveredCallback) {
        this.onParamChangedCallback = onParamChangedCallback;
        this.onEngineHoveredCallback = onEngineHoveredCallback;
        this.performanceSection = new ScenarioPerformanceOptionsSection(onParamChangedCallback);
    }

    /**
     * Builds and returns the entire engine configuration section.
     *
     * @return JavaFX VBox container
     */
    public VBox buildSection() {
        VBox section = new VBox(12);

        VBox perfBox = performanceSection.buildPerformanceSection();
        VBox engineInspectorCard = buildEngineInspectorCard();
        buildCoreEnginesPane();
        buildOptionalEnginesPane();

        section.getChildren().addAll(perfBox, engineInspectorCard, corePane, typeBPane);
        return section;
    }

    private VBox buildEngineInspectorCard() {
        engineInspectorTitle = new Label(I18n.getOrDefault("scenario.inspector.default_title", "⚙️ FrontierAsabiyyahEngine (Turchin Structural-Demographic Theory)"));
        engineInspectorTitle.getStyleClass().add("card-header");

        engineInspectorText = new Label(I18n.getOrDefault("scenario.inspector.default_text",
                "Simulates the expansion, decay, and cyclical collapse of state solidarity and collective capacity (Asabiyyah) along meta-ethnic fault lines."));
        engineInspectorText.setWrapText(true);
        engineInspectorText.getStyleClass().add("card-description");

        engineInspectorEquationsTitle = new Label(I18n.getOrDefault("scenario.inspector.equations_title", "📐 Formal Cliodynamic Equations:"));
        engineInspectorEquationsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-padding: 4 0 0 0;");

        engineInspectorEquations = new Label("dS/dt = r_s · S · (1 - S/K_s) - δ · S · Instability");
        engineInspectorEquations.setWrapText(true);
        engineInspectorEquations.getStyleClass().add("equation-badge");

        engineInspectorRef = new Label("📚 Reference: Turchin, P. (2003). Historical Dynamics: Why States Rise and Fall. Princeton University Press.");
        engineInspectorRef.setWrapText(true);
        engineInspectorRef.getStyleClass().add("hint-label");

        exportSelectedEngineBtn = new Button(I18n.getOrDefault("scenario.btn.export_engine_template", "📤 Export Custom Engine Template (.java)"));
        exportSelectedEngineBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 4 10; -fx-background-radius: 4;");
        exportSelectedEngineBtn.setOnAction(e -> {
            if (customEngineHandler != null) {
                customEngineHandler.exportCustomEngineTemplate();
            }
        });

        VBox card = new VBox(5, engineInspectorTitle, engineInspectorText, engineInspectorEquationsTitle, engineInspectorEquations, engineInspectorRef, exportSelectedEngineBtn);
        card.getStyleClass().add("engine-inspector-card");
        card.setMinHeight(160);
        return card;
    }

    private void buildCoreEnginesPane() {
        VBox typeABox = new VBox(6);
        typeABox.getStyleClass().add("card-section");

        exportCoreTemplateBtn = new Button(I18n.getOrDefault("scenario.btn.export_law_engine", "📤 Export Physical Law Engine (.java)"));
        exportCoreTemplateBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 4;");
        exportCoreTemplateBtn.setOnAction(e -> {
            if (customEngineHandler != null) {
                customEngineHandler.exportPhysicalLawEngineTemplate(selectedEngineClassName != null ? selectedEngineClassName : "PhysicalLawEngine");
            }
        });

        coreExplanationLabel = new Label(I18n.getOrDefault("scenario.info.core_engines",
                "ℹ️ Why are Ether Core engines permanent? They enforce physical conservation laws (mass & energy, thermodynamics, hydrology, H3 insolation, metabolism) required for basic world survival."));
        coreExplanationLabel.getStyleClass().add("control-note");
        coreExplanationLabel.setWrapText(true);
        typeABox.getChildren().addAll(exportCoreTemplateBtn, coreExplanationLabel);

        ScenarioEngineParamControlsHelper.populateCoreEngineRows(typeABox, coreEngineRows, this);

        corePane = new TitledPane(I18n.getOrDefault("scenario.header.core_engines", "🔒 ARCHITECTURE CŒUR ETHER (24 MOTEURS PERMANENTS)"), typeABox);
        corePane.setExpanded(false);
        corePane.getStyleClass().add("titled-pane-primary");
    }

    private void buildOptionalEnginesPane() {
        typeBBoxContainer = new VBox(8);
        typeBBoxContainer.getStyleClass().add("custom-module-card");
        customEngineHandler = new ScenarioCustomEngineHandler(typeBCheckBoxMap, typeBBoxContainer, onParamChangedCallback);

        exportTemplateBtn = new Button(I18n.getOrDefault("scenario.btn.export_java_template", "📤 Export Java Engine Template (.java)"));
        exportTemplateBtn.setStyle("-fx-background-color: #4c1d95; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        exportTemplateBtn.setOnAction(e -> customEngineHandler.exportCustomEngineTemplate());

        importEngineBtn = new Button(I18n.getOrDefault("scenario.btn.import_compile_engine", "📥 Import & Compile Java Engine (.java / .class)"));
        importEngineBtn.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 6 12; -fx-background-radius: 4;");
        importEngineBtn.setOnAction(e -> customEngineHandler.importCustomEngineFile());

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

        List<EngineMeta> optionalEngines = ScenarioEngineRegistry.getOptionalEngines();
        typeBCheckBoxMap.clear();
        typeBParamSpinnersMap.clear();
        optionalEngineMetas.clear();
        engineContainers.clear();

        for (EngineMeta eng : optionalEngines) {
            String engineTitle = I18n.getEngineTitle(eng.id(), eng.defaultTitle());
            CheckBox cb = new CheckBox(engineTitle);
            cb.setSelected("FrontierAsabiyyahEngine".equals(eng.id()));
            cb.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            cb.setTooltip(ScenarioEngineParamControlsHelper.buildEngineTooltip(
                    eng.id(), engineTitle, eng.defaultDesc(), eng.defaultRef(), eng.defaultEq(), false));
            cb.setOnAction(e -> notifyParamChange());

            typeBCheckBoxMap.put(eng.id(), cb);

            HBox row = new HBox(8, cb);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setOnMouseEntered(e -> {
                String curTitle = I18n.getEngineTitle(eng.id(), eng.defaultTitle());
                String curDesc = I18n.getEngineDescription(eng.id(), eng.defaultDesc());
                String curRef = I18n.getEngineReference(eng.id(), eng.defaultRef());
                String curEq = ScenarioEngineParamControlsHelper.resolveEquationText(eng.id(), eng.defaultEq());
                updateEngineInspector(eng.id(), curTitle, curDesc, curRef, curEq);
            });
            cb.setOnMouseEntered(row.getOnMouseEntered());

            optionalEngineMetas.add(new OptionalEngineMeta(eng.id(), cb, eng.defaultTitle(), eng.defaultDesc(), eng.defaultRef(), eng.defaultEq()));

            VBox engContainer = new VBox(4, row);
            javafx.scene.Node paramBox = ScenarioEngineParamControlsHelper.createEngineParameterBox(
                    eng.id(), typeBParamSpinnersMap, onParamChangedCallback);
            if (paramBox != null) {
                paramBox.visibleProperty().bind(cb.selectedProperty());
                paramBox.managedProperty().bind(cb.selectedProperty());
                engContainer.getChildren().add(paramBox);
            }

            engineContainers.put(eng.id(), engContainer);
            typeBBoxContainer.getChildren().add(engContainer);
        }

        engineSortCombo.getSelectionModel().selectedIndexProperty().addListener((obs, oldV, newV) ->
                sortOptionalEngines(newV != null ? newV.intValue() : 0));

        typeBPane = new TitledPane(String.format(
                I18n.getOrDefault("scenario.header.opt_engines_format", "⚙️ MODULES OPTIONNELS (%d MOTEURS EXTENSIBLES & IMPORT/EXPORT)"),
                optionalEngines.size()), typeBBoxContainer);
        typeBPane.setExpanded(true);
        typeBPane.getStyleClass().add("titled-pane-secondary");
    }

    private void notifyParamChange() {
        if (onParamChangedCallback != null) {
            onParamChangedCallback.run();
        }
    }

    public void updateEngineInspector(String className, String title, String description, String reference, String equations) {
        this.selectedEngineClassName = className;
        if (engineInspectorTitle != null) engineInspectorTitle.setText("⚙️ " + className + " — " + title);
        if (engineInspectorText != null) engineInspectorText.setText(description);
        if (engineInspectorRef != null) engineInspectorRef.setText("📚 Reference: " + reference);
        if (engineInspectorEquations != null) {
            engineInspectorEquations.setText(equations != null ? equations : "dX/dt = f(X, t) + Σ F_coupled");
        }
        if (onEngineHoveredCallback != null) {
            onEngineHoveredCallback.accept(className);
        }
    }

    public void autoSelectEnginesForYear(long year) {
        Map<String, Boolean> calibrated = org.ether.society.model.AutoEpochScenarioHelper.calibrateTypeBEngines("earth", year);
        for (Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
            Boolean state = calibrated.get(entry.getKey());
            if (state != null) {
                entry.getValue().setSelected(state);
            } else {
                long appYear = ScenarioEngineRegistry.getEngineApparitionYear(entry.getKey());
                entry.getValue().setSelected(year >= appYear);
            }
        }
        notifyParamChange();
    }

    public void sortOptionalEngines(int sortIdx) {
        ScenarioEngineParamControlsHelper.sortOptionalEngines(sortIdx, engineContainers, typeBBoxContainer);
    }

    public void applyScenario(Scenario s) {
        if (s == null) return;
        performanceSection.applyScenario(s);
        ScenarioEngineParamControlsHelper.applyScenarioEngines(s, typeBCheckBoxMap, typeBParamSpinnersMap);
    }

    public void exportToScenario(Scenario s) {
        if (s == null) return;
        performanceSection.exportToScenario(s);
        ScenarioEngineParamControlsHelper.exportScenarioEngines(s, typeBCheckBoxMap, typeBParamSpinnersMap);
    }

    public void updateTexts() {
        performanceSection.updateTexts();

        for (CoreEngineRow row : coreEngineRows) {
            String title = I18n.getEngineTitle(row.id, row.defaultTitle);
            row.descLabel.setText("— " + title);
            Tooltip.install(row.row, ScenarioEngineParamControlsHelper.buildEngineTooltip(
                    row.id, title, row.defaultDesc, row.defaultRef, row.defaultEq, true));
        }

        for (OptionalEngineMeta meta : optionalEngineMetas) {
            String title = I18n.getEngineTitle(meta.id, meta.defaultTitle);
            meta.cb.setText(title);
            meta.cb.setTooltip(ScenarioEngineParamControlsHelper.buildEngineTooltip(
                    meta.id, title, meta.defaultDesc, meta.defaultRef, meta.defaultEq, false));
        }

        if (corePane != null) corePane.setText(I18n.getOrDefault("scenario.header.core_engines", "🔒 ARCHITECTURE CŒUR ETHER (24 MOTEURS PERMANENTS)"));
        if (typeBPane != null) typeBPane.setText(String.format(I18n.getOrDefault("scenario.header.opt_engines_format", "⚙️ MODULES OPTIONNELS (%d MOTEURS EXTENSIBLES & IMPORT/EXPORT)"), optionalEngineMetas.size()));
        if (exportTemplateBtn != null) exportTemplateBtn.setText(I18n.getOrDefault("scenario.btn.export_java_template", "📤 Export Java Engine Template (.java)"));
        if (importEngineBtn != null) importEngineBtn.setText(I18n.getOrDefault("scenario.btn.import_compile_engine", "📥 Import & Compile Java Engine (.java / .class)"));
        if (exportCoreTemplateBtn != null) exportCoreTemplateBtn.setText(I18n.getOrDefault("scenario.btn.export_law_engine", "📤 Export Physical Law Engine (.java)"));
        if (exportSelectedEngineBtn != null) exportSelectedEngineBtn.setText(I18n.getOrDefault("scenario.btn.export_engine_template", "📤 Export Custom Engine Template (.java)"));
        if (autoSelectEnginesForYearBtn != null) autoSelectEnginesForYearBtn.setText(I18n.getOrDefault("scenario.btn.autoselect_engines", "⚡ Check Engines Based on T₀"));
        if (engineSortLabel != null) engineSortLabel.setText(I18n.getOrDefault("scenario.sort.label", "🔀 Engine Sorting:"));
        if (coreExplanationLabel != null) coreExplanationLabel.setText(I18n.getOrDefault("scenario.info.core_engines",
                "ℹ️ Why are Ether Core engines permanent? They enforce physical conservation laws (mass & energy, thermodynamics, hydrology, H3 insolation, metabolism) required for basic world survival."));
    }

    public void updatePerformanceControlsState() { performanceSection.updatePerformanceControlsState(); }
    public void addCustomEngineCheckBoxToUI(String engineKey, String labelText, String tooltipText, boolean defaultSelected) {
        if (customEngineHandler != null) customEngineHandler.addCustomEngineCheckBoxToUI(engineKey, labelText, tooltipText, defaultSelected);
    }
    public boolean isStrictDeterminism() { return performanceSection.isStrictDeterminism(); }
    public Map<String, CheckBox> getTypeBCheckBoxMap() { return typeBCheckBoxMap; }
    public Map<String, Map<String, Spinner<Double>>> getTypeBParamSpinnersMap() { return typeBParamSpinnersMap; }
    public CheckBox getStrictDeterminismCheckBox() { return performanceSection.getStrictDeterminismCheckBox(); }
    public Slider getClimateTickFreqSlider() { return performanceSection.getClimateTickFreqSlider(); }
    public Slider getThreadCountSlider() { return performanceSection.getThreadCountSlider(); }
    public Button getAutoSelectEnginesForYearBtn() { return autoSelectEnginesForYearBtn; }
    public ScenarioPerformanceOptionsSection getPerformanceSection() { return performanceSection; }
}
