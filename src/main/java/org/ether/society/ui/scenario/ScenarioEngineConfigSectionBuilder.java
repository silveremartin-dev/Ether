/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.scenario.ScenarioEngineCatalog.EngineMeta;
import org.ether.society.ui.util.UIControlsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * <h2>Scenario Engine Config Section Builder</h2>
 * <p>
 * Modular UI builder for configuring simulation engine architecture:
 * <ul>
 *   <li>Tier 1: 24 Permanent Core Physical Law Engines (Mass, thermodynamics, hydrology, energetics)</li>
 *   <li>Tier 2: 68 Optional/Hypothetical Cliodynamic & Macroeconomic Engines (Boserup, Turchin SDT, Smil, Ostrom)</li>
 *   <li>Performance Optimizations & Strict Determinism (Delegated to {@link ScenarioPerformanceOptionsSection})</li>
 *   <li>Dynamic Custom Engine Compiler & Import/Export (Delegated to {@link ScenarioCustomEngineHandler})</li>
 * </ul>
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public class ScenarioEngineConfigSectionBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioEngineConfigSectionBuilder.class);

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

    // Optional Tier 2 Engines
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

    // Custom Engine controls
    private Button autoSelectEnginesForYearBtn;
    private Button exportTemplateBtn;
    private Button importEngineBtn;
    private ComboBox<String> engineSortCombo;
    private Label engineSortLabel;

    private final Map<String, VBox> engineContainers = new LinkedHashMap<>();

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

        // 1. Performance Options & Master Determinism (Extracted)
        VBox perfBox = performanceSection.buildPerformanceSection();

        // 2. Engine Inspector Card
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

        VBox engineInspectorCard = new VBox(5, engineInspectorTitle, engineInspectorText, engineInspectorEquationsTitle, engineInspectorEquations, engineInspectorRef, exportSelectedEngineBtn);
        engineInspectorCard.getStyleClass().add("engine-inspector-card");
        engineInspectorCard.setMinHeight(160);

        // 3. Tier 1 Core Engines
        VBox typeABox = new VBox(6);
        typeABox.getStyleClass().add("card-section");

        exportCoreTemplateBtn = new Button(I18n.getOrDefault("scenario.btn.export_law_engine", "📤 Export Physical Law Engine (.java)"));
        exportCoreTemplateBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 4;");
        exportCoreTemplateBtn.setOnAction(e -> {
            if (customEngineHandler != null) {
                customEngineHandler.exportPhysicalLawEngineTemplate(selectedEngineClassName != null ? selectedEngineClassName : "PhysicalLawEngine");
            }
        });

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

        // 4. Tier 2 Optional Engines & Dynamic Compiler
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

        section.getChildren().addAll(perfBox, engineInspectorCard, corePane, this.typeBPane);
        return section;
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
        for (Map.Entry<String, CheckBox> entry : typeBCheckBoxMap.entrySet()) {
            long appYear = ScenarioEngineCatalog.getEngineApparitionYear(entry.getKey());
            entry.getValue().setSelected(year >= appYear);
        }
        notifyParamChange();
    }

    public void sortOptionalEngines(int sortIdx) {
        List<EngineMeta> sorted = new ArrayList<>(ScenarioEngineCatalog.getOptionalEngines());
        if (sortIdx == 1) { // Chronological (Oldest -> Newest)
            sorted.sort(Comparator.comparingLong(e -> ScenarioEngineCatalog.getEngineApparitionYear(e.id())));
        } else if (sortIdx == 2) { // Reverse Chronological (Newest -> Oldest)
            sorted.sort((a, b) -> Long.compare(ScenarioEngineCatalog.getEngineApparitionYear(b.id()), ScenarioEngineCatalog.getEngineApparitionYear(a.id())));
        } else if (sortIdx == 3) { // Alphabetical (A - Z)
            sorted.sort(Comparator.comparing(e -> I18n.getEngineTitle(e.id(), e.defaultTitle())));
        }

        for (EngineMeta eng : sorted) {
            VBox box = engineContainers.get(eng.id());
            if (box != null && typeBBoxContainer != null) {
                typeBBoxContainer.getChildren().remove(box);
                typeBBoxContainer.getChildren().add(box);
            }
        }
    }

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
                    "Niveau technique minimum pour assÃ©cher un lac urbain");
            addDoubleParamSpinner(grid, spinners, 1, "tenochtitlanCapital", "Capital Min Drainage (kg):", 150.0, 10.0, 1000.0, 50.0,
                    "Capital par habitant requis pour le drainage");
            addDoubleParamSpinner(grid, spinners, 2, "aralRainfallThreshold", "Seuil Pluie Aral (mm):", 300.0, 50.0, 1000.0, 50.0,
                    "Seuil pluviomÃ©trique sous lequel l'assÃ¨chement dÃ©marre");
            addDoubleParamSpinner(grid, spinners, 3, "damMinElevation", "Altitude Min Barrage (m):", 300.0, 50.0, 2000.0, 50.0,
                    "Altitude minimale pour construire un barrage de montagne");
        } else if ("FertileCrescentSalinizationEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "salinizationRate", "Taux Salinisation / SiÃ¨cle:", 0.02, 0.001, 0.10, 0.005,
                    "Vitesse d'accumulation des sels par irrigation intensive");
        } else if ("LandReclamationEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "polderTechThreshold", "Tech Seuil PoldÃ©risation:", 4.0, 1.0, 10.0, 0.5,
                    "Niveau technique requis pour conquÃ©rir des terres sur la mer");
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

    public void applyScenario(Scenario s) {
        if (s == null) return;
        performanceSection.applyScenario(s);

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
    }

    public void exportToScenario(Scenario s) {
        if (s == null) return;
        performanceSection.exportToScenario(s);

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

    public void updateTexts() {
        performanceSection.updateTexts();

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
        if (exportTemplateBtn != null) exportTemplateBtn.setText(I18n.getOrDefault("scenario.btn.export_java_template", "📤 Export Java Engine Template (.java)"));
        if (importEngineBtn != null) importEngineBtn.setText(I18n.getOrDefault("scenario.btn.import_compile_engine", "📥 Import & Compile Java Engine (.java / .class)"));
        if (exportCoreTemplateBtn != null) exportCoreTemplateBtn.setText(I18n.getOrDefault("scenario.btn.export_law_engine", "📤 Export Physical Law Engine (.java)"));
        if (exportSelectedEngineBtn != null) exportSelectedEngineBtn.setText(I18n.getOrDefault("scenario.btn.export_engine_template", "📤 Export Custom Engine Template (.java)"));
        if (autoSelectEnginesForYearBtn != null) autoSelectEnginesForYearBtn.setText(I18n.getOrDefault("scenario.btn.autoselect_engines", "⚡ Check Engines Based on T₀"));
        if (engineSortLabel != null) engineSortLabel.setText(I18n.getOrDefault("scenario.sort.label", "🔀 Engine Sorting:"));
        if (coreExplanationLabel != null) coreExplanationLabel.setText(I18n.getOrDefault("scenario.info.core_engines", "ℹ️ Why are Ether Core engines permanent? They enforce physical conservation laws (mass & energy, thermodynamics, hydrology, H3 insolation, metabolism) required for basic world survival."));
    }

    public void updatePerformanceControlsState() {
        performanceSection.updatePerformanceControlsState();
    }

    public void addCustomEngineCheckBoxToUI(String engineKey, String labelText, String tooltipText, boolean defaultSelected) {
        if (customEngineHandler != null) {
            customEngineHandler.addCustomEngineCheckBoxToUI(engineKey, labelText, tooltipText, defaultSelected);
        }
    }

    public boolean isStrictDeterminism() {
        return performanceSection.isStrictDeterminism();
    }

    public Map<String, CheckBox> getTypeBCheckBoxMap() {
        return typeBCheckBoxMap;
    }

    public Map<String, Map<String, Spinner<Double>>> getTypeBParamSpinnersMap() {
        return typeBParamSpinnersMap;
    }

    public CheckBox getStrictDeterminismCheckBox() {
        return performanceSection.getStrictDeterminismCheckBox();
    }

    public Slider getClimateTickFreqSlider() {
        return performanceSection.getClimateTickFreqSlider();
    }

    public Slider getThreadCountSlider() {
        return performanceSection.getThreadCountSlider();
    }

    public Button getAutoSelectEnginesForYearBtn() {
        return autoSelectEnginesForYearBtn;
    }

    public ScenarioPerformanceOptionsSection getPerformanceSection() {
        return performanceSection;
    }
}
