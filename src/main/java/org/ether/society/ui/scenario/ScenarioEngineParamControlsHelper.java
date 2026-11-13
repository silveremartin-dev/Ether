/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.ui.scenario.ScenarioEngineRegistry.EngineMeta;
import org.ether.society.ui.util.UIControlsHelper;

import java.util.*;

/**
 * Helper delegate managing parameter spinner grids, sorting comparators, Core Tier 1 row assembly,
 * and {@link Scenario} persistence for {@link ScenarioEngineConfigSectionBuilder}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
final class ScenarioEngineParamControlsHelper {

    /**
     * Metadata container binding a permanent Tier 1 Core engine row to its localized labels.
     */
    static final class CoreEngineRow {
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

    /**
     * Metadata container binding an optional Tier 2 engine checkbox to its localized labels.
     */
    static final class OptionalEngineMeta {
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

    private ScenarioEngineParamControlsHelper() {
        // Utility delegate class
    }

    /**
     * Populates Tier 1 Core engine rows into the target container VBox.
     *
     * @param typeABox       target Core engines VBox container
     * @param coreEngineRows output list populated with {@link CoreEngineRow} metadata
     * @param builder        owning engine config section builder
     */
    static void populateCoreEngineRows(
            VBox typeABox,
            List<CoreEngineRow> coreEngineRows,
            ScenarioEngineConfigSectionBuilder builder) {
        List<EngineMeta> coreEngines = ScenarioEngineRegistry.getCoreEngines();
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

            Tooltip.install(row, buildEngineTooltip(eng.id(), title, eng.defaultDesc(), eng.defaultRef(), eng.defaultEq(), true));

            row.setOnMouseEntered(e -> {
                String curTitle = I18n.getEngineTitle(eng.id(), eng.defaultTitle());
                String curDesc = I18n.getEngineDescription(eng.id(), eng.defaultDesc());
                String curRef = I18n.getEngineReference(eng.id(), eng.defaultRef());
                String curEq = resolveEquationText(eng.id(), eng.defaultEq());
                builder.updateEngineInspector(eng.id(), curTitle, curDesc, curRef, curEq);
            });

            coreEngineRows.add(new CoreEngineRow(eng.id(), descLbl, row, eng.defaultTitle(), eng.defaultDesc(), eng.defaultRef(), eng.defaultEq()));
            typeABox.getChildren().add(row);
        }
    }

    /**
     * Builds a formatted technical tooltip for either a permanent Core engine or an optional Tier 2 engine.
     *
     * @param id          engine identifier
     * @param title       localized engine title
     * @param defaultDesc fallback engine description
     * @param defaultRef  fallback academic citation
     * @param defaultEq   fallback mathematical equation
     * @param permanent   true for permanent Tier 1 Core engine, false for optional Tier 2 engine
     * @return configured JavaFX {@link Tooltip}
     */
    static Tooltip buildEngineTooltip(
            String id,
            String title,
            String defaultDesc,
            String defaultRef,
            String defaultEq,
            boolean permanent) {
        String desc = I18n.getEngineDescription(id, defaultDesc);
        String ref = I18n.getEngineReference(id, defaultRef);
        String eqText = resolveEquationText(id, defaultEq);
        String prefix = permanent
                ? I18n.getOrDefault("scenario.engine.perm_prefix", "🔒 [MOTEUR PERMANENT]\n")
                : I18n.getOrDefault("scenario.engine.opt_prefix", "⚙️ [MOTEUR OPTIONNEL]\n");
        Tooltip tooltip = new Tooltip(prefix + id + " — " + title + "\n\n" + desc + "\n\n" + eqText + "\n\n📚 " + ref);
        tooltip.setStyle("-fx-font-size: 11px; -fx-max-width: 500px;");
        return tooltip;
    }

    /**
     * Resolves the localized equation string for an engine or returns the standard differential state fallback.
     *
     * @param id        engine identifier
     * @param defaultEq fallback equation string
     * @return localized equation string
     */
    static String resolveEquationText(String id, String defaultEq) {
        return defaultEq != null
                ? I18n.getEngineEquation(id, defaultEq)
                : I18n.getOrDefault("scenario.engine.state_eq_default", "📋 Équation d'État : dX/dt = f(X, t) + Σ F_inter-cellulaire");
    }

    /**
     * Creates an inline parameter configuration GridPane for configurable Tier 2 engines, or {@code null} if none.
     *
     * @param engineKey               unique engine identifier
     * @param typeBParamSpinnersMap   registry map storing created parameter spinners by engine and parameter key
     * @param onParamChangedCallback  callback notified on spinner edits
     * @return parameter {@link Node} or {@code null}
     */
    static Node createEngineParameterBox(
            String engineKey,
            Map<String, Map<String, Spinner<Double>>> typeBParamSpinnersMap,
            Runnable onParamChangedCallback) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setStyle("-fx-padding: 4 8 8 24; -fx-background-color: rgba(30, 41, 59, 0.4); -fx-background-radius: 4;");

        Map<String, Spinner<Double>> spinners = new HashMap<>();

        if ("MaritimeHighwayEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "capitalBoostRate", "Capital Boost Rate:", 0.05, 0.00, 0.20, 0.01,
                    "Taux d'accroissement du capital par le commerce maritime", onParamChangedCallback);
            addDoubleParamSpinner(grid, spinners, 1, "frictionMultiplier", "Friction Multiplier:", 0.20, 0.05, 1.00, 0.05,
                    "Multiplicateur de friction sur l'eau", onParamChangedCallback);
        } else if ("HydrologicalEngineeringEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "tenochtitlanTech", "Tech Min Drainage Lacs:", 3.5, 1.0, 10.0, 0.5,
                    "Niveau technique minimum pour assécher un lac urbain", onParamChangedCallback);
            addDoubleParamSpinner(grid, spinners, 1, "tenochtitlanCapital", "Capital Min Drainage (kg):", 150.0, 10.0, 1000.0, 50.0,
                    "Capital par habitant requis pour le drainage", onParamChangedCallback);
            addDoubleParamSpinner(grid, spinners, 2, "aralRainfallThreshold", "Seuil Pluie Aral (mm):", 300.0, 50.0, 1000.0, 50.0,
                    "Seuil pluviométrique sous lequel l'assèchement démarre", onParamChangedCallback);
            addDoubleParamSpinner(grid, spinners, 3, "damMinElevation", "Altitude Min Barrage (m):", 300.0, 50.0, 2000.0, 50.0,
                    "Altitude minimale pour construire un barrage de montagne", onParamChangedCallback);
        } else if ("SoilSalinizationHydrologyEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "salinizationRate", "Taux Salinisation / Siècle:", 0.02, 0.001, 0.10, 0.005,
                    "Vitesse d'accumulation des sels par irrigation intensive", onParamChangedCallback);
        } else if ("LandReclamationEngine".equals(engineKey)) {
            addDoubleParamSpinner(grid, spinners, 0, "polderTechThreshold", "Tech Seuil Poldérisation:", 4.0, 1.0, 10.0, 0.5,
                    "Niveau technique requis pour conquérir des terres sur la mer", onParamChangedCallback);
            addDoubleParamSpinner(grid, spinners, 1, "polderCapitalThreshold", "Capital Seuil Polders (kg):", 100.0, 10.0, 500.0, 20.0,
                    "Capital d'infrastructure minimum requis pour les digues", onParamChangedCallback);
        }

        if (!spinners.isEmpty()) {
            typeBParamSpinnersMap.put(engineKey, spinners);
            return grid;
        }
        return null;
    }

    private static void addDoubleParamSpinner(
            GridPane grid,
            Map<String, Spinner<Double>> spinners,
            int row,
            String paramKey,
            String labelText,
            double defVal,
            double min,
            double max,
            double step,
            String tooltip,
            Runnable onParamChangedCallback) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        Spinner<Double> sp = new Spinner<>(min, max, defVal, step);
        sp.setEditable(true);
        sp.setPrefWidth(90);
        sp.setStyle("-fx-font-size: 10px;");
        sp.setTooltip(new Tooltip(tooltip));
        sp.valueProperty().addListener((obs, oldV, newV) -> {
            if (onParamChangedCallback != null) onParamChangedCallback.run();
        });
        UIControlsHelper.attachDefaultValueHandling(sp, defVal, () -> sp.getValueFactory().setValue(defVal), onParamChangedCallback);
        grid.add(lbl, 0, row);
        grid.add(sp, 1, row);
        spinners.put(paramKey, sp);
    }

    /**
     * Reorders optional Tier 2 engine UI cards according to the selected sort mode.
     *
     * @param sortIdx           0 = category order, 1 = chronological ascending, 2 = chronological descending, 3 = alphabetical
     * @param engineContainers  map of engine card VBoxes keyed by engine ID
     * @param typeBBoxContainer parent container holding the engine cards
     */
    static void sortOptionalEngines(
            int sortIdx,
            Map<String, VBox> engineContainers,
            VBox typeBBoxContainer) {
        List<EngineMeta> sorted = new ArrayList<>(ScenarioEngineRegistry.getOptionalEngines());
        if (sortIdx == 1) {
            sorted.sort(Comparator.comparingLong(e -> ScenarioEngineRegistry.getEngineApparitionYear(e.id())));
        } else if (sortIdx == 2) {
            sorted.sort((a, b) -> Long.compare(
                    ScenarioEngineRegistry.getEngineApparitionYear(b.id()),
                    ScenarioEngineRegistry.getEngineApparitionYear(a.id())));
        } else if (sortIdx == 3) {
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

    /**
     * Applies scenario optional engine selection states and numerical parameters to UI controls.
     *
     * @param s                     source scenario model
     * @param typeBCheckBoxMap      map of optional engine checkboxes
     * @param typeBParamSpinnersMap map of optional engine parameter spinners
     */
    static void applyScenarioEngines(
            Scenario s,
            Map<String, CheckBox> typeBCheckBoxMap,
            Map<String, Map<String, Spinner<Double>>> typeBParamSpinnersMap) {
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

    /**
     * Exports UI optional engine selection states and numerical parameters into a {@link Scenario}.
     *
     * @param s                     target scenario model
     * @param typeBCheckBoxMap      map of optional engine checkboxes
     * @param typeBParamSpinnersMap map of optional engine parameter spinners
     */
    static void exportScenarioEngines(
            Scenario s,
            Map<String, CheckBox> typeBCheckBoxMap,
            Map<String, Map<String, Spinner<Double>>> typeBParamSpinnersMap) {
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
}
