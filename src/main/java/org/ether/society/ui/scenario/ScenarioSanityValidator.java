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
     * Executes live biophysical diagnostic and updates the visual diagnostic card.
     *
     * @param contentBox target VBox container for diagnostic categories
     * @param headerLabel target header Label to show viability status
     * @param p selected planetary preset
     * @param eco selected ecology preset
     * @param demoSrc demographic source name
     * @param elevImg planetary elevation raster map
     * @param customDensityImage imported custom demographic density raster
     * @param dimsCount cultural vector dimension count
     * @param customTensorImages map of imported custom cultural tensor rasters
     * @param tensorTitleFunc function to retrieve localized cultural tensor title
     * @param startYear simulation start year
     * @param initialPopulation initial global human population
     * @param configErrors list of validation error strings
     * @param jitReport compiled symbolic JIT conflict report
     */
    public static void updateLiveDiagnostics(VBox contentBox, Label headerLabel,
                                             PlanetPreset p, EcologyPreset eco,
                                             String demoSrc, javafx.scene.image.Image elevImg,
                                             javafx.scene.image.Image customDensityImage,
                                             int dimsCount, java.util.Map<Integer, javafx.scene.image.Image> customTensorImages,
                                             java.util.function.IntFunction<String> tensorTitleFunc,
                                             long startYear, long initialPopulation,
                                             List<String> configErrors, EngineConflictReport jitReport) {
        if (contentBox == null) return;
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        String planetName = p.name();
        String planetKey = planetName.toLowerCase();

        List<String> geoItems = new ArrayList<>();
        List<String> ecoItems = new ArrayList<>();
        List<String> engineItems = new ArrayList<>();
        List<String> validationItems = new ArrayList<>();
        int alertCount = 0;

        // =========================================================================
        // 1. CARTOGRAPHIC & GEOGRAPHIC COMPATIBILITY (Tabs 1 & 3)
        // =========================================================================
        org.ether.society.generation.ProceduralGenerator generator = new org.ether.society.generation.ProceduralGenerator();

        // A. Planetary Body Mismatch
        boolean bodyMismatch = false;
        if (demoSrc != null && !demoSrc.isBlank()) {
            String demoLower = demoSrc.toLowerCase();
            boolean planetIsEarth = planetKey.contains("terre") || planetKey.contains("earth") || planetKey.contains("terran");
            boolean demoIsMars = demoLower.contains("mars");
            boolean demoIsMoon = demoLower.contains("lune") || demoLower.contains("moon");
            boolean demoIsVenus = demoLower.contains("vénus") || demoLower.contains("venus");
            boolean demoIsMercury = demoLower.contains("mercure") || demoLower.contains("mercury");
            boolean demoIsEarth = demoLower.contains("terre") || demoLower.contains("earth") || demoLower.contains("hyde");

            if (planetIsEarth && (demoIsMars || demoIsMoon || demoIsVenus || demoIsMercury)) {
                geoItems.add("⚠️ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "Incohérence planétaire : Source démographique « %s » sélectionnée sur un relief terrestre (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            } else if (planetKey.contains("mars") && demoIsEarth) {
                geoItems.add("⚠️ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "Incohérence planétaire : Source démographique terrestre « %s » appliquée sur le relief martien (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            } else if (planetKey.contains("lune") && demoIsEarth) {
                geoItems.add("⚠️ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "Incohérence planétaire : Source démographique terrestre « %s » appliquée sur le relief lunaire (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            } else if (planetKey.contains("vénus") && demoIsEarth) {
                geoItems.add("⚠️ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "Incohérence planétaire : Source démographique terrestre « %s » appliquée sur Vénus (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            } else if (planetKey.contains("mercure") && demoIsEarth) {
                geoItems.add("⚠️ " + String.format(I18n.getOrDefault("scenario.warning.demo_body_mismatch", "Incohérence planétaire : Source démographique terrestre « %s » appliquée sur Mercure (Onglet 1)."), demoSrc.trim()));
                bodyMismatch = true;
                alertCount++;
            }
        }
        if (!bodyMismatch) {
            geoItems.add(String.format(I18n.getOrDefault("scenario.diagnostic.geo_ok", "✅ Relief et corps céleste parfaitement cohérents (%s)"), planetName));
        }

        // B. Demographic Density vs Ocean Immersion
        javafx.scene.image.PixelReader elevReader = elevImg != null ? elevImg.getPixelReader() : null;

        if (customDensityImage != null && customDensityImage.getPixelReader() != null) {
            double totalDensityBrightness = 0.0;
            double oceanDensityBrightness = 0.0;
            int imgW = (int) customDensityImage.getWidth();
            int imgH = (int) customDensityImage.getHeight();
            javafx.scene.image.PixelReader pr = customDensityImage.getPixelReader();
            int sampleSteps = 60;

            for (int sy = 0; sy < sampleSteps; sy++) {
                double normLat = (sy + 0.5) / sampleSteps;
                double lat = 90.0 - normLat * 180.0;
                int py = (int) Math.min(imgH - 1, normLat * imgH);
                for (int sx = 0; sx < sampleSteps * 2; sx++) {
                    double normLon = (sx + 0.5) / (sampleSteps * 2.0);
                    double lon = -180.0 + normLon * 360.0;
                    int px = (int) Math.min(imgW - 1, normLon * imgW);
                    double b = pr.getColor(px, py).getBrightness();
                    if (b > 0.05) {
                        totalDensityBrightness += b;
                        boolean isOceanCell = false;
                        if (elevReader != null && elevImg != null) {
                            int ex = (int) Math.min(elevImg.getWidth() - 1, normLon * elevImg.getWidth());
                            int ey = (int) Math.min(elevImg.getHeight() - 1, normLat * elevImg.getHeight());
                            javafx.scene.paint.Color ec = elevReader.getColor(ex, ey);
                            double eNorm = (ec.getRed() + ec.getGreen() + ec.getBlue()) / 3.0;
                            isOceanCell = (eNorm < p.waterLevel());
                        } else {
                            var pt = generator.getPlanetPoint(lat, lon, p);
                            isOceanCell = (pt.elevation() < p.waterLevel());
                        }
                        if (isOceanCell) {
                            oceanDensityBrightness += b;
                        }
                    }
                }
            }
            if (totalDensityBrightness > 0 && oceanDensityBrightness > 0) {
                double oceanPct = (oceanDensityBrightness * 100.0) / totalDensityBrightness;
                if (oceanPct > 15.0) {
                    geoItems.add(String.format(java.util.Locale.FRANCE,
                        I18n.getOrDefault("scenario.warning.ocean_density", "⚠️ Incompatibilité géographique : %.1f%% de la densité démographique importée se trouve en zone océanique / sous-marine (%s)."),
                        oceanPct, planetName));
                    alertCount++;
                } else {
                    geoItems.add(String.format(I18n.getOrDefault("scenario.diagnostic.demo_coast_ok", "✅ Densité démographique conforme aux terres émergées (%s)"), planetName));
                }
            } else {
                geoItems.add(String.format(I18n.getOrDefault("scenario.diagnostic.demo_coast_ok", "✅ Densité démographique conforme aux terres émergées (%s)"), planetName));
            }
        }

        // C. Cultural Tensors vs Ocean Immersion
        boolean tensorOceanWarn = false;
        if (customTensorImages != null) {
            for (int i = 0; i < dimsCount; i++) {
                javafx.scene.image.Image tImg = customTensorImages.get(i);
                if (tImg != null && tImg.getPixelReader() != null && p.waterLevel() > -0.4) {
                    double totalTB = 0.0;
                    double oceanTB = 0.0;
                    int tw = (int) tImg.getWidth();
                    int th = (int) tImg.getHeight();
                    javafx.scene.image.PixelReader pr = tImg.getPixelReader();
                    int sampleSteps = 40;
                    for (int sy = 0; sy < sampleSteps; sy++) {
                        double normLat = (sy + 0.5) / sampleSteps;
                        double lat = 90.0 - normLat * 180.0;
                        int py = (int) Math.min(th - 1, normLat * th);
                        for (int sx = 0; sx < sampleSteps * 2; sx++) {
                            double normLon = (sx + 0.5) / (sampleSteps * 2.0);
                            double lon = -180.0 + normLon * 360.0;
                            int px = (int) Math.min(tw - 1, normLon * tw);
                            double b = pr.getColor(px, py).getBrightness();
                            if (b > 0.08) {
                                totalTB += b;
                                boolean isOceanCell = false;
                                if (elevReader != null && elevImg != null) {
                                    int ex = (int) Math.min(elevImg.getWidth() - 1, normLon * elevImg.getWidth());
                                    int ey = (int) Math.min(elevImg.getHeight() - 1, normLat * elevImg.getHeight());
                                    javafx.scene.paint.Color ec = elevReader.getColor(ex, ey);
                                    double eNorm = (ec.getRed() + ec.getGreen() + ec.getBlue()) / 3.0;
                                    isOceanCell = (eNorm < p.waterLevel());
                                } else {
                                    var pt = generator.getPlanetPoint(lat, lon, p);
                                    isOceanCell = (pt.elevation() < p.waterLevel());
                                }
                                if (isOceanCell) oceanTB += b;
                            }
                        }
                    }
                    if (totalTB > 0 && oceanTB > 0) {
                        double oceanPct = (oceanTB * 100.0) / totalTB;
                        if (oceanPct > 20.0) {
                            String tensorTitle = tensorTitleFunc != null ? tensorTitleFunc.apply(i) : ("Tensor #" + i);
                            geoItems.add(String.format(java.util.Locale.FRANCE,
                                I18n.getOrDefault("scenario.warning.ocean_culture", "⚠️ Incompatibilité culturelle : %.1f%% de l'intensité du tenseur « %s » est située sur l'océan (%s)."),
                                oceanPct, tensorTitle, planetName));
                            tensorOceanWarn = true;
                            alertCount++;
                        }
                    }
                }
            }
        }
        if (!tensorOceanWarn && customTensorImages != null && !customTensorImages.isEmpty()) {
            geoItems.add(String.format(I18n.getOrDefault("scenario.diagnostic.tensors_ok", "✅ Tenseurs culturels conformes aux terres émergées (%s)"), planetName));
        }

        // D. Extreme Hostile Environment
        double atmoPres = p.atmospherePressureAtm();
        double avgTemp = p.averageTempC();
        boolean isSpaceBody = planetKey.contains("lune") || planetKey.contains("moon") || planetKey.contains("mercure") || planetKey.contains("mercury") || planetKey.contains("mars") || planetKey.contains("venus") || planetKey.contains("vénus") || planetKey.contains("titan");
        if (!isSpaceBody && (atmoPres < 0.1 || atmoPres > 5.0 || avgTemp < -50 || avgTemp > 60)) {
            geoItems.add(String.format(java.util.Locale.FRANCE,
                I18n.getOrDefault("scenario.warning.hostile_environment", "⚠️ Environnement hostile (Onglet 1) : Pression (%.2f atm) ou Température (%.1f °C) extrême — Survie humaine conditionnée à des habitats scellés."),
                atmoPres, avgTemp));
            alertCount++;
        }

        // =========================================================================
        // 2. ECOLOGICAL VIABILITY & PLANETARY RESOURCES (Tabs 2 & 3)
        // =========================================================================
        if (p.atmospherePressureAtm() < 0.01) {
            ecoItems.add("❌ Atmosphère absente/tenue (" + String.format("%.3f", p.atmospherePressureAtm()) + " atm) : L'eau liquide bout à la surface. Survie humaine impossible sans dômes fermés.");
            alertCount++;
        } else if (p.oxygenPercentage() < 10.0) {
            ecoItems.add("⚠️ Atmosphère hypoxique (O2 = " + String.format("%.1f%%", p.oxygenPercentage()) + ") : Insuffisant pour la respiration des organismes complexes.");
            alertCount++;
        } else {
            ecoItems.add("✅ Atmosphère respirable & constante (P = " + String.format("%.2f", p.atmospherePressureAtm()) + " atm, O2 = " + String.format("%.1f%%", p.oxygenPercentage()) + ")");
        }

        if (p.waterLevel() < -0.3) {
            ecoItems.add("⚠️ Ressources en Eau Limitées : Monde très aride. Stress hydrique majeur prévisible.");
            alertCount++;
        } else {
            ecoItems.add("✅ Hydrologie équilibrée (Niveau d'eau = " + String.format("%.0f%%", (1.0 + p.waterLevel()) * 50) + ")");
        }

        double capitalK0 = org.ether.society.ui.ScenarioSetupPanel.computeAutoCapitalFromYear(startYear);
        double crustal = eco != null ? eco.crustalMetalOresGt() : 80.0;
        if (capitalK0 >= 8000.0 && crustal < 20.0) {
            ecoItems.add("⚠️ Déficit en Métaux Industriels : Capital physique " + String.format("%.0f", capitalK0) + " kg/hab configuré mais métaux crustaux faibles (" + String.format("%.1f Gt", crustal) + "). Risque de pénurie industrielle.");
            alertCount++;
        } else {
            ecoItems.add("✅ Compatibilité Matériaux / Capital Physique");
        }

        if (initialPopulation > 5_000_000_000L && p.waterLevel() < -0.2) {
            ecoItems.add("❌ Surpopulation Majeure : " + String.format("%,d", initialPopulation) + " habitants configurés sur un monde aride.");
            alertCount++;
        } else {
            ecoItems.add("✅ Densité Démographique Initiale Réaliste (" + String.format("%,d", initialPopulation) + " hab)");
        }

        // =========================================================================
        // 3. PHYSICAL ENGINES & JIT FUSIONS
        // =========================================================================
        if (jitReport != null && !jitReport.getEntries().isEmpty()) {
            for (org.ether.society.engines.compiler.EngineConflictReport.ConflictEntry entry : jitReport.getEntries()) {
                if (entry.getSeverity() == org.ether.society.engines.compiler.EngineConflictReport.ConflictSeverity.INCOMPATIBLE) {
                    engineItems.add("❌ INCOMPATIBILITÉ MOTEURS (" + entry.getVariableName() + ") : " + entry.getDescription());
                    alertCount++;
                } else {
                    engineItems.add("✅ Fusion JIT Moteurs (" + entry.getVariableName() + ") : " + entry.getDescription());
                }
            }
        } else {
            engineItems.add("✅ Compilation JIT Moteurs : 100% Compatible & Fusions Validées");
        }

        // =========================================================================
        // 4. SCENARIO CONFIGURATION & SETUP INTEGRITY
        // =========================================================================
        if (configErrors == null || configErrors.isEmpty()) {
            validationItems.add(I18n.getOrDefault("scenario.diagnostic.validation_ok", "✅ Configuration et couches du scénario 100% conformes et prêtes à l'exécution"));
        } else {
            for (String err : configErrors) {
                validationItems.add("❌ " + err);
                alertCount++;
            }
        }

        // =========================================================================
        // RENDER CATEGORIZED DIAGNOSTIC BOX
        // =========================================================================
        contentBox.getChildren().clear();

        if (headerLabel != null) {
            headerLabel.getStyleClass().removeAll("diagnostic-header-success", "diagnostic-header-warn");
            if (alertCount == 0) {
                headerLabel.setText(I18n.getOrDefault("scenario.diagnostic.header_viable", "📋 9. CIVILIZATIONAL VIABILITY & COMPATIBILITY DIAGNOSTICS: 100% VIABLE & COMPATIBLE"));
                headerLabel.getStyleClass().add("diagnostic-header-success");
            } else {
                headerLabel.setText(I18n.getOrDefault("scenario.diagnostic.header_alerts", "📋 9. CIVILIZATIONAL VIABILITY & COMPATIBILITY DIAGNOSTICS: ") + alertCount + " " + I18n.getOrDefault("scenario.diagnostic.alert_count_label", "ALERTE(S) / TENSION(S)"));
                headerLabel.getStyleClass().add("diagnostic-header-warn");
            }
        }

        renderDiagnosticCategory(contentBox, I18n.getOrDefault("scenario.diagnostic.cat.geography", "🗺️ Cartographic & Geographic Compatibility (Tabs 1 & 3)"), geoItems);
        renderDiagnosticCategory(contentBox, I18n.getOrDefault("scenario.diagnostic.cat.ecology", "🌿 Ecological Viability & Planetary Resources (Tabs 2 & 3)"), ecoItems);
        renderDiagnosticCategory(contentBox, I18n.getOrDefault("scenario.diagnostic.cat.engines", "⚡ Physical Engines & JIT Fusion Compatibility"), engineItems);
        renderDiagnosticCategory(contentBox, I18n.getOrDefault("scenario.diagnostic.cat.validation", "🛑 Scenario Setup & Configuration Validation"), validationItems);
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
