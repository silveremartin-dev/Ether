/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.util.LaTeXFormatter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Autonomous UI Controller for simulation engine selection, Tier 1 vs Tier 2 categorization, and scientific formula inspection.
 * 
 * <p>Extracted from {@code ScenarioSetupPanel} to satisfy the Single Responsibility Principle (SRP).
 * Decouples invariant biophysical engines (Tier 1) from pluggable cliodynamic hypotheses (Tier 2),
 * renders mathematical derivations in KaTeX/LaTeX, and manages fine-grained engine parameter controls.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioEngineSelector {

    private final Label engineInspectorTitle;
    private final Label engineInspectorText;
    private final Label engineInspectorEquationsTitle;
    private final Label engineInspectorEquations;
    private final Label engineInspectorRef;

    /**
     * Constructs the engine selector and formula inspector controller.
     */
    public ScenarioEngineSelector() {
        this.engineInspectorTitle = new Label();
        this.engineInspectorTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #38bdf8;");
        this.engineInspectorTitle.setWrapText(true);

        this.engineInspectorText = new Label();
        this.engineInspectorText.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");
        this.engineInspectorText.setWrapText(true);

        this.engineInspectorEquationsTitle = new Label();
        this.engineInspectorEquationsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-padding: 4 0 0 0;");

        this.engineInspectorEquations = new Label();
        this.engineInspectorEquations.setStyle("-fx-font-size: 11px; -fx-text-fill: #f1f5f9; -fx-font-family: 'Consolas', 'Courier New', monospace; -fx-background-color: #0f172a; -fx-padding: 6; -fx-background-radius: 4;");
        this.engineInspectorEquations.setWrapText(true);

        this.engineInspectorRef = new Label();
        this.engineInspectorRef.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-style: italic;");
        this.engineInspectorRef.setWrapText(true);
    }

    /**
     * Builds and returns the engine mathematical inspector card.
     *
     * @return JavaFX VBox card displaying selected engine formulas and scientific citations
     */
    public VBox buildInspectorCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-border-color: #3b82f6; -fx-border-radius: 8;");
        card.getChildren().addAll(engineInspectorTitle, engineInspectorText, engineInspectorEquationsTitle, engineInspectorEquations, engineInspectorRef);
        return card;
    }

    /**
     * Updates the inspector card display with metadata, KaTeX equations, and literature references.
     *
     * @param className simple Java class name
     * @param title human-readable localized title
     * @param description physical/cliodynamic mechanism description
     * @param equations mathematical LaTeX formulas
     * @param reference primary academic source (e.g. Seshat, EPICA, Farquhar 1980)
     */
    public void inspectEngine(String className, String title, String description, String equations, String reference) {
        if (engineInspectorTitle != null) engineInspectorTitle.setText("🔎 " + className + " — " + title);
        if (engineInspectorText != null) engineInspectorText.setText(LaTeXFormatter.formatLaTeX(description));
        if (engineInspectorEquations != null) engineInspectorEquations.setText(LaTeXFormatter.formatLaTeX(equations));
        if (engineInspectorRef != null) engineInspectorRef.setText("📚 " + reference);
    }

    /**
     * Creates a fine-grained parameter tuning box for an optional cliodynamic engine.
     *
     * @param engineKey engine identifier
     * @param typeBParamSpinnersMap map registering created parameter spinners
     * @return JavaFX GridPane node with parameter controls
     */
    public javafx.scene.Node createEngineParameterBox(String engineKey, Map<String, Map<String, Spinner<Double>>> typeBParamSpinnersMap) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        grid.setPadding(new Insets(6, 6, 6, 24));
        grid.setStyle("-fx-background-color: rgba(15, 23, 42, 0.4); -fx-background-radius: 4;");

        Map<String, Spinner<Double>> spinners = new LinkedHashMap<>();

        if ("MaritimeHighwayEngine".equals(engineKey)) {
            addParamSpinner(grid, spinners, 0, "speedBoostFactor", "speedBoostFactor", 2.5, 1.0, 5.0, 0.1,
                    "Facteur d'accélération des flux maritimes par rapport aux routes terrestres.");
            addParamSpinner(grid, spinners, 1, "offshoreRiskPenalty", "offshoreRiskPenalty", 0.05, 0.0, 0.5, 0.01,
                    "Pénalité de mortalité maritime en haute mer.");
        } else if ("HydrologicalEngineeringEngine".equals(engineKey)) {
            addParamSpinner(grid, spinners, 0, "aqueductEfficiencyBonus", "aqueductEfficiencyBonus", 1.8, 1.0, 3.0, 0.1,
                    "Coefficient de transport hydrologique urbain.");
            addParamSpinner(grid, spinners, 1, "basinCanalizationIndex", "basinCanalizationIndex", 1.5, 1.0, 2.5, 0.1,
                    "Multiplicateur d'irrigation de plaine alluviale.");
        }

        if (!spinners.isEmpty()) {
            typeBParamSpinnersMap.put(engineKey, spinners);
            return grid;
        }
        return null;
    }

    private void addParamSpinner(GridPane grid, Map<String, Spinner<Double>> spinners, int row,
                                String paramKey, String labelText, double defVal, double min, double max, double step, String tooltip) {
        Label lbl = new Label(labelText + ":");
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        Spinner<Double> sp = new Spinner<>(min, max, defVal, step);
        sp.setPrefWidth(90);
        sp.setStyle("-fx-font-size: 10px;");
        sp.setTooltip(new Tooltip(tooltip));
        grid.add(lbl, 0, row);
        grid.add(sp, 1, row);
        spinners.put(paramKey, sp);
    }
}
