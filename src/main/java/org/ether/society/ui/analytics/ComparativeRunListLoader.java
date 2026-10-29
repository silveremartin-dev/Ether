/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.collections.ObservableList;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.analytics.SimulationRunRepository;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.SaveMetadata;
import org.ether.society.persistence.ScenarioRepository;
import org.ether.society.persistence.SimulationSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Loads, matches, and synthesizes scenario candidate records for comparative analytics.
 *
 * <p>Associates saved simulation runs on disk, in-memory telemetry runs, and active scenarios,
 * and pre-populates the comparative selection table.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ComparativeRunListLoader {

    private static final Logger logger = LoggerFactory.getLogger(ComparativeRunListLoader.class);

    private ComparativeRunListLoader() {
        // Utility class
    }

    /**
     * Refreshes the scenario observable list with baseline and simulated runs.
     */
    public static void refreshRunList(
            ObservableList<ScenarioSelectableItem> scenarioList,
            ScenarioRepository scenarioRepository,
            SimulationSaveManager saveManager,
            SimulationRunRepository runRepository,
            Supplier<H3SimulationEngine> engineSupplier,
            Consumer<ScenarioSelectableItem> itemSelectionListener
    ) {
        Set<String> previousSelectedNames = new HashSet<>();
        for (ScenarioSelectableItem it : scenarioList) {
            if (it.isSelected()) {
                previousSelectedNames.add(it.getName());
                if (it.getScenario() != null && it.getScenario().getId() != null) {
                    previousSelectedNames.add(String.valueOf(it.getScenario().getId()));
                }
                if (it.getRunId() != null) {
                    previousSelectedNames.add(it.getRunId());
                }
            }
        }

        scenarioList.clear();

        // 1. Ground Truth Baseline Scenario
        Scenario histScenario = new Scenario();
        histScenario.setName(I18n.getOrDefault("analytics.scenario.ground_truth", "🌍 Historical Reality (Cliodynamic Ground Truth)"));
        histScenario.setStartDateYear(-100000);
        histScenario.setEndDateYear(2026);
        boolean wasHistSelected = previousSelectedNames.isEmpty() || previousSelectedNames.contains("HISTORICAL_GROUND_TRUTH") || previousSelectedNames.contains(histScenario.getName());
        ScenarioSelectableItem histItem = new ScenarioSelectableItem(histScenario, wasHistSelected, true, "HISTORICAL_GROUND_TRUTH");
        histItem.selectedProperty().addListener((obs, oldV, newV) -> itemSelectionListener.accept(histItem));
        scenarioList.add(histItem);

        // 1b. External Predictive Projections (Post-2025 Multi-Model Benchmarks)
        String[][] predictiveBaselines = {
            {"PREDICTIVE_SSP1", "🟣 " + I18n.getOrDefault("analytics.predictive.ssp1", "Transition Soutenable Net-Zéro (SSP1 / IEA NZE 2050)")},
            {"PREDICTIVE_SSP2", "🟣 " + I18n.getOrDefault("analytics.predictive.ssp2", "Projection Tendancielle Globale (SSP2 / UN WPP 2024 / IEA STEPS)")},
            {"PREDICTIVE_SSP3_WORLD3", "🟣 " + I18n.getOrDefault("analytics.predictive.ssp3", "Limites à la Croissance & Pénurie (SSP3 / Club de Rome World3 BAU2)")},
            {"PREDICTIVE_SSP4", "🟣 " + I18n.getOrDefault("analytics.predictive.ssp4", "Fracture Sociale & Inégalités Extrêmes (SSP4 / IIASA Inequality)")},
            {"PREDICTIVE_SSP5", "🟣 " + I18n.getOrDefault("analytics.predictive.ssp5", "Croissance Fossile Intensive (SSP5 / CMIP6 High-Emissions)")},
            {"PREDICTIVE_UN_WPP_2024", "🟣 " + I18n.getOrDefault("analytics.predictive.un_wpp", "ONU Perspectives Démographiques Mondiales (UN WPP 2024 / DESA)")}
        };

        for (String[] pBase : predictiveBaselines) {
            String pRunId = pBase[0];
            String pName = pBase[1];
            Scenario predScenario = new Scenario();
            predScenario.setName(pName);
            predScenario.setStartDateYear(2026);
            predScenario.setEndDateYear(2100);
            boolean wasPredSelected = previousSelectedNames.contains(pRunId) || previousSelectedNames.contains(pName);
            ScenarioSelectableItem predItem = new ScenarioSelectableItem(predScenario, wasPredSelected, true, pRunId);
            predItem.selectedProperty().addListener((obs, oldV, newV) -> itemSelectionListener.accept(predItem));
            scenarioList.add(predItem);
        }

        List<Scenario> allScenarios = scenarioRepository.getAllScenarios();
        List<SaveMetadata> allSaves = Collections.emptyList();
        try {
            allSaves = saveManager.listSaves();
        } catch (Exception e) {
            logger.warn("Failed to list saves for analytics revalidation: {}", e.getMessage());
        }

        H3SimulationEngine activeEngine = (engineSupplier != null) ? engineSupplier.get() : null;
        Set<String> matchedSaveIds = new HashSet<>();

        // 2. Repository Scenarios
        for (Scenario sc : allScenarios) {
            String scName = sc.getDisplayName();
            if (scName == null || scName.isBlank()) {
                scName = I18n.getOrDefault("analytics.unnamed_scenario", "Unnamed Scenario") + (sc.getId() != null ? " (#" + sc.getId() + ")" : "");
            }

            Optional<SimulationRunRecord> recordOpt = runRepository.getRunByScenarioName(sc.getName());
            if (!recordOpt.isPresent()) recordOpt = runRepository.getRunByScenarioName(scName);
            if (!recordOpt.isPresent() && sc.getId() != null) recordOpt = Optional.ofNullable(runRepository.getRun(String.valueOf(sc.getId())));

            SaveMetadata matchingSave = null;
            String scNorm = sc.getName() != null ? sc.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") : "";
            String scKey = sc.getPresetKey() != null ? sc.getPresetKey().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") : "";
            String scDisp = sc.getDisplayName() != null ? sc.getDisplayName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") : "";
            String scDensity = sc.getPopulationDensityType() != null ? sc.getPopulationDensityType().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") : "";

            for (SaveMetadata save : allSaves) {
                String saveId = save.getId() != null ? save.getId().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") : "";
                String saveNorm = save.getName() != null ? save.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") : "";
                String saveScNorm = save.getScenarioName() != null ? save.getScenarioName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") : "";

                boolean textMatch = (!scKey.isEmpty() && (saveNorm.contains(scKey) || saveScNorm.contains(scKey) || saveId.contains(scKey)))
                             || (!scNorm.isEmpty() && (saveNorm.contains(scNorm) || saveScNorm.contains(scNorm) || saveId.contains(scNorm)))
                             || (!scDisp.isEmpty() && (saveNorm.contains(scDisp) || saveScNorm.contains(scDisp) || saveId.contains(scDisp)))
                             || (!scDensity.isEmpty() && (saveNorm.contains(scDensity) || saveScNorm.contains(scDensity)));

                boolean epochSemanticMatch = false;
                if (!textMatch) {
                    if ((scNorm.contains("afrique") || scNorm.contains("africa") || scNorm.contains("toba") || scNorm.contains("sapiens")) && (saveId.contains("epoch1") || saveId.contains("paleolithic") || saveNorm.contains("africa") || saveScNorm.contains("afrique"))) {
                        epochSemanticMatch = true;
                    } else if ((scNorm.contains("dryas") || scNorm.contains("natufien")) && (saveId.contains("dryas") || saveNorm.contains("dryas") || saveScNorm.contains("dryas"))) {
                        epochSemanticMatch = true;
                    } else if ((scNorm.contains("bering") || scNorm.contains("solutre") || scNorm.contains("lgm") || scNorm.contains("glaciaire")) && (saveId.contains("epoch2") || saveId.contains("lgm") || saveNorm.contains("beringia"))) {
                        epochSemanticMatch = true;
                    } else if ((scNorm.contains("fertile") || scNorm.contains("crescent") || scNorm.contains("sahara") || scNorm.contains("neolith")) && (saveId.contains("epoch3") || saveId.contains("neolithic") || saveNorm.contains("sahara"))) {
                        epochSemanticMatch = true;
                    } else if ((scNorm.contains("bronze") || scNorm.contains("mesopotam") || scNorm.contains("assyri") || scNorm.contains("harappa") || scNorm.contains("egypt")) && (saveId.contains("epoch4") || saveId.contains("bronze") || saveId.contains("harappa") || saveNorm.contains("egypt"))) {
                        epochSemanticMatch = true;
                    }
                }

                if (textMatch || epochSemanticMatch) {
                    matchedSaveIds.add(save.getId());
                    boolean isSaveRun = save.getId() != null && save.getId().startsWith("RUN-");
                    boolean prevIsRun = matchingSave != null && matchingSave.getId() != null && matchingSave.getId().startsWith("RUN-");
                    if (matchingSave == null || (isSaveRun && !prevIsRun) || (isSaveRun == prevIsRun && save.getYear() > matchingSave.getYear())) {
                        matchingSave = save;
                    }
                }
            }

            boolean isInteractiveMatch = (activeEngine != null 
                && activeEngine.getCurrentScenario() != null 
                && (activeEngine.getCurrentScenario().getName().equalsIgnoreCase(sc.getName())
                    || (sc.getId() != null && sc.getId().equals(activeEngine.getCurrentScenario().getId()))));

            long startYear = sc.getStartDateYear();
            long endYear = sc.getEndDateYear();
            long totalYears = Math.max(1, endYear - startYear);
            long maxYearReached = startYear;
            String runId = "N/A";

            if (recordOpt.isPresent() && !recordOpt.get().getTimeSeriesData().isEmpty()) {
                SimulationRunRecord rec = recordOpt.get();
                runId = rec.getRunId();
                maxYearReached = Math.max(maxYearReached, rec.getEndYear());
            }

            if (matchingSave != null) {
                String saveId = matchingSave.getId();
                ComparativeTelemetryDataLoader.ensureHistoryLoadedForSave(saveId, sc.getName(), runRepository);
                recordOpt = Optional.ofNullable(runRepository.getRun(saveId));
                if (recordOpt.isPresent() && !recordOpt.get().getTimeSeriesData().isEmpty()) {
                    runId = saveId;
                    maxYearReached = Math.max(maxYearReached, matchingSave.getYear());
                }
            }

            if (isInteractiveMatch) {
                maxYearReached = Math.max(maxYearReached, activeEngine.getCurrentYear());
            }

            boolean hasTelemetry = recordOpt.isPresent() && !recordOpt.get().getTimeSeriesData().isEmpty();
            double progress = Math.min(1.0, Math.max(0.0, (double) (maxYearReached - startYear) / totalYears));
            if (hasTelemetry && (matchingSave != null || recordOpt.get().getEndYear() >= endYear)) {
                progress = 1.0;
            }
            boolean executed = hasTelemetry && progress >= 1.0;
            if (!hasTelemetry) {
                executed = false;
                runId = "N/A";
            }

            boolean isSelected = previousSelectedNames.contains(sc.getName())
                || previousSelectedNames.contains(scName)
                || (sc.getId() != null && previousSelectedNames.contains(String.valueOf(sc.getId())))
                || (!"N/A".equals(runId) && previousSelectedNames.contains(runId));

            ScenarioSelectableItem item = new ScenarioSelectableItem(sc, isSelected, executed, runId);
            item.setProgress(progress);
            item.recalculateEstimate();
            item.selectedProperty().addListener((obs, oldV, newV) -> itemSelectionListener.accept(item));

            scenarioList.add(item);
            if (item.isExecuted() && item.getRunId() != null && !item.getRunId().equals("N/A")) {
                matchedSaveIds.add(item.getRunId());
            }
        }

        // 3. Independent Saves without matching Scenarios
        for (SaveMetadata save : allSaves) {
            String saveId = save.getId();
            if (saveId == null || matchedSaveIds.contains(saveId)) continue;

            String runName = save.getName();
            if (runName == null || runName.isBlank() || runName.equalsIgnoreCase("Unknown")) {
                runName = save.getScenarioName();
            }
            if (runName == null || runName.isBlank()) runName = saveId;

            String runNameLower = runName.toLowerCase(Locale.ROOT);
            String saveIdLower = saveId.toLowerCase(Locale.ROOT);
            if (runNameLower.contains("test_save") || runNameLower.contains("autosave_exit") ||
                saveIdLower.startsWith("autosave_") || saveIdLower.contains("test_save") ||
                runNameLower.startsWith("test") || runNameLower.equals("null")) {
                continue;
            }

            ComparativeTelemetryDataLoader.ensureHistoryLoadedForSave(saveId, save.getScenarioName(), runRepository);
            SimulationRunRecord runRec = runRepository.getRun(saveId);
            if (runRec != null && !runRec.getTimeSeriesData().isEmpty()) {
                long startY = runRec.getStartYear();
                long endY = Math.max(startY + 1, runRec.getEndYear());

                final String finalRunName = runName;
                boolean alreadyListed = scenarioList.stream().anyMatch(ex -> ex.getName().equalsIgnoreCase(finalRunName));
                if (alreadyListed) {
                    matchedSaveIds.add(saveId);
                    continue;
                }

                Scenario synthScenario = new Scenario();
                synthScenario.setName(runName);
                synthScenario.setStartDateYear(startY);
                synthScenario.setEndDateYear(endY);
                synthScenario.setH3Resolution(save.getH3Resolution() > 0 ? save.getH3Resolution() : 3);

                boolean isSelected = previousSelectedNames.contains(runName) || previousSelectedNames.contains(saveId);
                ScenarioSelectableItem extraItem = new ScenarioSelectableItem(synthScenario, isSelected, true, saveId);
                extraItem.setProgress(1.0);
                extraItem.recalculateEstimate();
                extraItem.selectedProperty().addListener((obs, oldV, newV) -> itemSelectionListener.accept(extraItem));

                scenarioList.add(extraItem);
                matchedSaveIds.add(saveId);
            }
        }
    }
}
