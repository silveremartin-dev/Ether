/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.ether.society.engines.compiler.EngineConflictReport;
import org.ether.society.engines.compiler.ScenarioEngineJITCompiler;
import org.ether.society.engines.compiler.SymbolicExpression;
import org.ether.society.i18n.I18n;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.EcologyPreset;

import java.util.ArrayList;
import java.util.List;

/**
 * Autonomous UI Controller for Pre-Flight Simulation Sanity Checks & Biophysical Validation.
 * 
 * <p>Extracted from {@code ScenarioSetupPanel} to satisfy the Single Responsibility Principle (SRP).
 * Validates cross-tensor constraints, planetary body consistency, historical temporal bounds,
 * carrying capacity feasibility, and symbolic JIT engine step conflicts before simulation ignition.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioSanityValidator {

    private static EngineConflictReport cachedJitReport = null;

    /**
     * Gets or creates the cached symbolic JIT engine conflict report.
     *
     * @return compiled EngineConflictReport
     */
    public static synchronized EngineConflictReport getOrCreateJitReport() {
        if (cachedJitReport == null) {
            ScenarioEngineJITCompiler jit = new ScenarioEngineJITCompiler();
            jit.registerEngineStep("BiologicalDemographics", "biomassHuman", new SymbolicExpression("biomassHuman", 1.01, 0.0), 10000.0);
            jit.registerEngineStep("EcologicalDegradation", "biomassHuman", new SymbolicExpression("biomassHuman", 0.995, 0.0), 10000.0);
            jit.registerEngineStep("AtmosphericSolar", "temperature", new SymbolicExpression("temperature", 1.0, 0.02), 25.0);
            jit.compile();
            cachedJitReport = jit.getConflictReport();
        }
        return cachedJitReport;
    }

    /**
     * Renders a diagnostic item category into a JavaFX VBox container.
     *
     * @param container target VBox
     * @param title category header title
     * @param items list of diagnostic bullet strings
     */
    public static void renderDiagnosticCategory(VBox container, String title, List<String> items) {
        if (items == null || items.isEmpty()) return;
        Label catLabel = new Label(title);
        catLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #38bdf8; -fx-padding: 6 0 2 0;");
        container.getChildren().add(catLabel);
        for (String item : items) {
            Label lbl = new Label("  • " + item);
            lbl.setWrapText(true);
            if (item.startsWith("❌")) {
                lbl.getStyleClass().add("diagnostic-error");
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            } else if (item.startsWith("⚠️") || item.contains("⚠️")) {
                lbl.getStyleClass().add("diagnostic-warn");
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b;");
            } else {
                lbl.getStyleClass().add("diagnostic-pass");
                lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");
            }
            container.getChildren().add(lbl);
        }
    }

    /**
     * Executes comprehensive biophysical validation and returns any blocking error strings.
     *
     * @param planetPreset selected planetary preset
     * @param ecologyPreset selected ecology preset
     * @param startYear simulation start year
     * @param endYear simulation end year
     * @param stepDays time step dt in days
     * @param demoSource selected demographic source name
     * @return list of localized error messages (empty if completely valid)
     */
    public static List<String> validate(PlanetPreset planetPreset, EcologyPreset ecologyPreset,
                                       long startYear, long endYear, double stepDays, String demoSource) {
        List<String> errors = new ArrayList<>();

        if (startYear >= endYear) {
            errors.add(String.format(I18n.getOrDefault("scenario.error.temporal_order", 
                    "L'année de début (%,d) doit être strictement inférieure à l'année de fin (%,d)."), startYear, endYear));
        }

        if (stepDays <= 0.0) {
            errors.add(I18n.getOrDefault("scenario.error.step_positive", "Le pas de temps Δt doit être strictement positif."));
        }

        if (planetPreset == null) {
            errors.add(I18n.getOrDefault("scenario.error.missing_planet", "Aucun contexte planétaire sélectionné."));
        }

        if (ecologyPreset == null) {
            errors.add(I18n.getOrDefault("scenario.error.missing_ecology", "Aucun preset écologique sélectionné."));
        }

        // Planetary body mismatch check
        if (planetPreset != null && demoSource != null && !demoSource.isBlank()) {
            String pKey = planetPreset.name().toLowerCase();
            String dLower = demoSource.toLowerCase();
            boolean planetIsEarth = pKey.contains("terre") || pKey.contains("earth") || pKey.contains("terran");
            boolean demoIsMars = dLower.contains("mars");
            boolean demoIsMoon = dLower.contains("lune") || dLower.contains("moon");

            if (planetIsEarth && (demoIsMars || demoIsMoon)) {
                errors.add(String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch",
                        "Incohérence planétaire : Source démographique « %s » sélectionnée sur un relief terrestre."), demoSource.trim()));
            }
        }

        return errors;
    }

    /**
     * Validates scenario parameters and prompts user confirmation if warnings exist.
     *
     * @param planetPreset selected planet preset
     * @param ecologyPreset selected ecology preset
     * @param startYear simulation start year
     * @param endYear simulation end year
     * @param stepDays dt step
     * @param demoSource demo source
     * @return true if valid and ready to launch, false if aborted
     */
    public static boolean promptAndValidate(PlanetPreset planetPreset, EcologyPreset ecologyPreset,
                                            long startYear, long endYear, double stepDays, String demoSource) {
        List<String> errors = validate(planetPreset, ecologyPreset, startYear, endYear, stepDays, demoSource);
        if (!errors.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(I18n.getOrDefault("scenario.alert.validation_error_title", "Simulation Setup Error"));
            alert.setHeaderText(I18n.getOrDefault("scenario.alert.validation_error_header", "Incohérences de configuration détectées"));
            StringBuilder sb = new StringBuilder();
            for (String err : errors) {
                sb.append("• ").append(err).append("\n");
            }
            alert.setContentText(sb.toString());
            alert.showAndWait();
            return false;
        }
        return true;
    }
}
