/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.analytics;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.ether.society.analytics.HeadlessBatchRunner;
import org.ether.society.analytics.SimulationRunRecord;
import org.ether.society.analytics.SimulationRunRepository;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.ExecutionContextPanel;
import org.ether.society.ui.Theme;
import org.ether.society.ui.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Headless Simulation Batch Execution and Queue Coordinator.
 * 
 * <p>Coordinates multithreaded deterministic headless batch runs, cancellation tokens,
 * interactive simulation collision avoidance, ETA calculations, and confirmation dialogs.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ComparativeBatchExecutionCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(ComparativeBatchExecutionCoordinator.class);

    private ComparativeBatchExecutionCoordinator() {
        // Static utility class
    }

    /**
     * Checks selection and execution status, updating warning and action button labels accordingly.
     */
    public static void checkExecutionStatus(
            List<ScenarioSelectableItem> scenarioList,
            Label warningLabel,
            Button executeMissingBtn,
            Button cancelBatchBtn,
            AtomicBoolean isBatchRunning) {

        List<ScenarioSelectableItem> selected = scenarioList.stream()
            .filter(ScenarioSelectableItem::isSelected)
            .toList();

        long unexecutedCount = selected.stream().filter(i -> !i.isExecuted() && !i.isExternalBaseline()).count();

        if (selected.isEmpty()) {
            warningLabel.setText(I18n.getOrDefault("analytics.warning.none_selected", "ℹ️ Aucun scénario sélectionné pour la comparaison."));
            warningLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-padding: 0; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: default;");
            executeMissingBtn.setText(I18n.getOrDefault("analytics.btn.execute_scenarios", "🚀 Exécuter les scénarios"));
            executeMissingBtn.setDisable(true);
            cancelBatchBtn.setVisible(false);
            cancelBatchBtn.setManaged(false);
        } else if (unexecutedCount > 0) {
            long totalUnexecutedYears = selected.stream()
                .filter(i -> !i.isExecuted() && !i.isExternalBaseline())
                .mapToLong(i -> Math.max(1, (long)((1.0 - i.getProgress()) * (i.getScenario().getEndDateYear() - i.getScenario().getStartDateYear()))))
                .sum();
            double totalEstSeconds = selected.stream()
                .filter(i -> !i.isExecuted() && !i.isExternalBaseline())
                .mapToDouble(ScenarioSelectableItem::getRemainingDurationSec)
                .sum();
            String durStr = ExecutionContextPanel.formatDuration(totalEstSeconds);
            String hwName = ExecutionContextPanel.getActiveHardwareMode().name();

            if (unexecutedCount == 1) {
                warningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.unexecuted_single_est", "⚠️ 1 scénario sélectionné non simulé | %,d ans | ⏱️ Temps estimé : %s (%s)"), totalUnexecutedYears, durStr, hwName));
            } else {
                warningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.unexecuted_plural_est", "⚠️ %d scénarios sélectionnés non simulés | Horizon total : %,d ans | ⏱️ Temps estimé : %s (%s)"), unexecutedCount, totalUnexecutedYears, durStr, hwName));
            }
            warningLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f59e0b; -fx-padding: 0; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: default;");
            
            if (!isBatchRunning.get()) {
                executeMissingBtn.setText(unexecutedCount == 1
                    ? String.format(I18n.getOrDefault("analytics.btn.execute_missing_single_fmt", "🚀 Lancer le scénario (%s)"), durStr)
                    : String.format(I18n.getOrDefault("analytics.btn.execute_missing_plural_fmt", "🚀 Lancer %d scénarios en file (%s)"), unexecutedCount, durStr));
                executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
                executeMissingBtn.setDisable(false);
                cancelBatchBtn.setVisible(false);
                cancelBatchBtn.setManaged(false);
            }
        } else {
            warningLabel.setText(String.format(I18n.getOrDefault("analytics.warning.ready", "✅ Tous les scénarios sélectionnés (%d) sont prêts pour l'audit et la comparaison."), selected.size()));
            warningLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #10b981; -fx-padding: 0; -fx-background-color: transparent; -fx-border-color: transparent; -fx-cursor: default;");
            long simulatedCount = selected.stream().filter(i -> !i.isExternalBaseline()).count();
            if (!isBatchRunning.get()) {
                if (simulatedCount == 0) {
                    executeMissingBtn.setText(I18n.getOrDefault("analytics.btn.reexecute_single", "🔄 Réexécuter le scénario"));
                    executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #64748b; -fx-text-fill: white; -fx-padding: 6 14;");
                    executeMissingBtn.setDisable(true);
                } else {
                    executeMissingBtn.setText(simulatedCount == 1
                        ? I18n.getOrDefault("analytics.btn.reexecute_single", "🔄 Réexécuter le scénario")
                        : String.format(I18n.getOrDefault("analytics.btn.reexecute_plural", "🔄 Réexécuter les %d scénarios"), simulatedCount));
                    executeMissingBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 6 14; -fx-cursor: hand;");
                    executeMissingBtn.setDisable(false);
                }
                cancelBatchBtn.setVisible(false);
                cancelBatchBtn.setManaged(false);
            }
        }
    }

    /**
     * Executes missing or queued scenarios with confirmation dialog and parallel worker thread.
     */
    public static void executeMissingScenarios(
            List<ScenarioSelectableItem> scenarioList,
            TableView<ScenarioSelectableItem> scenarioTable,
            Button executeMissingBtn,
            Button cancelBatchBtn,
            ProgressBar batchProgressBar,
            Label etaLabel,
            AtomicBoolean isBatchRunning,
            AtomicBoolean isBatchCancelled,
            AtomicBoolean isBatchPaused,
            SimulationRunRepository runRepository,
            BooleanSupplier isInteractiveSimulationRunningSupplier,
            Runnable onPauseInteractiveSimulationCallback,
            Runnable onBatchCompletedCallback) {

        if (isBatchRunning.get()) return;

        List<ScenarioSelectableItem> targetItems = scenarioList.stream()
            .filter(i -> i.isSelected() && !i.isExternalBaseline())
            .toList();

        if (targetItems.isEmpty()) return;

        boolean allAlreadyExecuted = targetItems.stream().allMatch(ScenarioSelectableItem::isExecuted);

        Dialog<ButtonType> confirmDialog = new Dialog<>();
        confirmDialog.setTitle(allAlreadyExecuted
            ? I18n.getOrDefault("analytics.confirm.reexecute_title", "Confirmation de réexécution")
            : I18n.getOrDefault("analytics.confirm.execute_title", "Confirmation du calcul de simulation"));
        confirmDialog.setHeaderText(null);

        double totalEstSeconds = targetItems.stream().mapToDouble(ScenarioSelectableItem::getRemainingDurationSec).sum();
        if (totalEstSeconds <= 0) {
            totalEstSeconds = targetItems.stream().mapToDouble(ScenarioSelectableItem::getEstimatedDurationSec).sum();
        }
        String durStr = ExecutionContextPanel.formatDuration(totalEstSeconds);
        String hwName = ExecutionContextPanel.getActiveHardwareMode().name();
        int cores = Runtime.getRuntime().availableProcessors();

        boolean isDark = Theme.getCurrentTheme() == Theme.DARK;
        String bgCard = isDark ? "#1e293b" : "#f8fafc";
        String textTitleColor = isDark ? "#38bdf8" : "#0284c7";
        String textBodyColor = isDark ? "#f1f5f9" : "#0f172a";
        String textMutedColor = isDark ? "#94a3b8" : "#64748b";
        String borderColor = isDark ? "rgba(56, 189, 248, 0.3)" : "rgba(2, 132, 199, 0.3)";

        VBox dialogContent = new VBox(12);
        dialogContent.setPadding(new Insets(16));
        dialogContent.setPrefWidth(480);
        dialogContent.setMaxWidth(480);

        Label headerTitleLabel = new Label(allAlreadyExecuted
            ? I18n.getOrDefault("analytics.confirm.reexecute_header", "🔄 Réexécuter les scénarios sélectionnés ?")
            : I18n.getOrDefault("analytics.confirm.execute_header", "🚀 Lancer la simulation physique en tâche de fond"));
        headerTitleLabel.setWrapText(true);
        headerTitleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + textTitleColor + ";");

        Label introLabel = new Label(allAlreadyExecuted
            ? I18n.getOrDefault("analytics.confirm.reexecute_content", "Les scénarios sélectionnés ont déjà été simulés et enregistrés sur disque. Les réexécuter régénérera l'intégralité de la télémétrie de manière déterministe. Souhaitez-vous continuer ?")
            : I18n.getOrDefault("analytics.confirm.execute_intro", "Vous êtes sur le point de lancer l'exécution physique déterministe des scénarios sélectionnés :"));
        introLabel.setWrapText(true);
        introLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + textBodyColor + "; -fx-line-spacing: 2px;");

        VBox recapBox = new VBox(6);
        recapBox.setPadding(new Insets(10, 12, 10, 12));
        recapBox.setStyle("-fx-background-color: " + bgCard + "; -fx-border-color: " + borderColor + "; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label item1 = new Label(String.format("• %s : %d", I18n.getOrDefault("analytics.confirm.scenarios_count", "Scénarios"), targetItems.size()));
        item1.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textBodyColor + "; -fx-font-weight: bold;");

        Label item2 = new Label(String.format("• %s : %s", I18n.getOrDefault("analytics.confirm.est_time", "Temps total estimé"), durStr));
        item2.setStyle("-fx-font-size: 11px; -fx-text-fill: #f59e0b; -fx-font-weight: bold;");

        Label item3 = new Label(String.format("• %s : %s (%d cœurs)", I18n.getOrDefault("analytics.confirm.hardware", "Moteur matériel"), hwName, cores));
        item3.setStyle("-fx-font-size: 11px; -fx-text-fill: " + textMutedColor + ";");

        Label item4 = new Label(String.format("• %s : %s", I18n.getOrDefault("analytics.confirm.determinism", "Reproductibilité"), "100% bit-à-bit déterministe"));
        item4.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");

        recapBox.getChildren().addAll(item1, item2, item3, item4);

        Label questionLabel = new Label(I18n.getOrDefault("analytics.confirm.start_question", "Confirmez-vous le démarrage immédiat ?"));
        questionLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + textBodyColor + ";");

        dialogContent.getChildren().addAll(headerTitleLabel, introLabel, recapBox, questionLabel);

        ButtonType confirmBtnType = new ButtonType(I18n.getOrDefault("analytics.dialog.btn_confirm", "Confirmer et lancer"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtnType = new ButtonType(I18n.getOrDefault("analytics.dialog.btn_cancel", "Annuler"), ButtonBar.ButtonData.CANCEL_CLOSE);
        
        DialogPane pane = confirmDialog.getDialogPane();
        pane.setContent(dialogContent);
        pane.setPrefWidth(490);
        pane.setMaxWidth(500);
        pane.setStyle("-fx-background-color: " + (isDark ? "#0f172a" : "#ffffff") + "; -fx-border-color: " + (isDark ? "#38bdf8" : "#0284c7") + "; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        pane.getButtonTypes().setAll(confirmBtnType, cancelBtnType);

        try {
            WindowUtils.styleDialogPane(pane);
            WindowUtils.applyWindowIcon(confirmDialog);
            
            Button confirmBtn = (Button) pane.lookupButton(confirmBtnType);
            if (confirmBtn != null) {
                confirmBtn.setStyle("-fx-font-weight: bold; -fx-background-color: #0284c7; -fx-text-fill: white; -fx-padding: 7 16; -fx-cursor: hand;");
            }
            Button cancelBtn = (Button) pane.lookupButton(cancelBtnType);
            if (cancelBtn != null) {
                cancelBtn.setStyle("-fx-background-color: " + (isDark ? "rgba(255,255,255,0.08)" : "#e2e8f0") + "; -fx-text-fill: " + textBodyColor + "; -fx-padding: 7 16; -fx-cursor: hand;");
            }
        } catch (Exception ex) {
            logger.debug("Failed applying theme to confirm dialog", ex);
        }

        Optional<ButtonType> choice = confirmDialog.showAndWait();
        if (choice.isEmpty() || choice.get() != confirmBtnType) {
            logger.info("Batch execution aborted by user.");
            return;
        }

        if (isInteractiveSimulationRunningSupplier != null && isInteractiveSimulationRunningSupplier.getAsBoolean()) {
            if (onPauseInteractiveSimulationCallback != null) {
                logger.info("Auto-pausing interactive simulation to prioritize comparative batch execution");
                onPauseInteractiveSimulationCallback.run();
            }
        }

        isBatchRunning.set(true);
        isBatchCancelled.set(false);
        isBatchPaused.set(false);

        ExecutionContextPanel.HardwareMode activeHw = ExecutionContextPanel.getActiveHardwareMode();
        int availableCores = Runtime.getRuntime().availableProcessors();
        int threadCount = switch (activeHw) {
            case NATIVE_RUST, JAVA_VECTOR_SIMD -> Math.max(1, availableCores);
            case GPU_SHADERS -> 2;
            case CPU_JIT -> Math.max(1, availableCores);
            case GPU_OFF -> 1;
        };

        executeMissingBtn.setDisable(true);
        executeMissingBtn.setText(I18n.getOrDefault("analytics.btn.executing", "⏳ Processing Batch Queue..."));
        cancelBatchBtn.setVisible(true);
        cancelBatchBtn.setManaged(true);
        cancelBatchBtn.setDisable(false);
        cancelBatchBtn.setText(I18n.getOrDefault("analytics.btn.cancel_batch", "🛑 Cancel Batch"));

        batchProgressBar.setProgress(0.0);
        batchProgressBar.setVisible(true);
        batchProgressBar.setManaged(true);
        etaLabel.setText(I18n.getOrDefault("analytics.eta_calculating", "⏱️ Calculating..."));
        etaLabel.setVisible(true);
        etaLabel.setManaged(true);

        int queuePos = 1;
        for (ScenarioSelectableItem item : targetItems) {
            item.setBatchState(BatchState.QUEUED);
            item.setQueueIndex(queuePos++);
            item.setProgress(0.0);
        }
        scenarioTable.refresh();

        long startTimeMs = System.currentTimeMillis();

        Thread batchWorkerThread = new Thread(() -> {
            logger.info("Starting parallel batch execution with {} threads for {} scenarios...", threadCount, targetItems.size());
            ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threadCount));
            AtomicInteger finishedCount = new AtomicInteger(0);
            List<Future<?>> futures = new ArrayList<>();

            for (ScenarioSelectableItem item : targetItems) {
                futures.add(pool.submit(() -> {
                    if (isBatchCancelled.get()) {
                        javafx.application.Platform.runLater(() -> item.setBatchState(BatchState.NOT_EXECUTED));
                        return;
                    }

                    javafx.application.Platform.runLater(() -> {
                        item.setBatchState(BatchState.RUNNING);
                        scenarioTable.refresh();
                    });

                    SimulationRunRecord record = HeadlessBatchRunner.executeScenarioHeadless(
                        item.getScenario(),
                        (sc, prog, yr, endYr) -> {
                            javafx.application.Platform.runLater(() -> {
                                item.setProgress(prog);
                                scenarioTable.refresh();
                                updateLiveBatchProgressAndEta(targetItems, startTimeMs, batchProgressBar, etaLabel);
                            });
                        },
                        isBatchCancelled::get
                    );

                    if (record != null && !isBatchCancelled.get()) {
                        runRepository.saveRun(record);
                        finishedCount.incrementAndGet();
                        javafx.application.Platform.runLater(() -> {
                            item.setExecuted(true);
                            item.setRunId(record.getRunId());
                            item.setBatchState(BatchState.EXECUTED);
                            item.setProgress(1.0);
                            scenarioTable.refresh();
                            updateLiveBatchProgressAndEta(targetItems, startTimeMs, batchProgressBar, etaLabel);
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            if (!item.isExecuted()) {
                                item.setBatchState(BatchState.NOT_EXECUTED);
                            }
                            scenarioTable.refresh();
                        });
                    }
                }));
            }

            pool.shutdown();
            try {
                for (var f : futures) {
                    f.get();
                }
            } catch (Exception ex) {
                logger.warn("Batch thread pool interrupted: {}", ex.getMessage());
            }

            long totalElapsedSec = Math.max(1, (System.currentTimeMillis() - startTimeMs) / 1000);

            javafx.application.Platform.runLater(() -> {
                isBatchRunning.set(false);
                batchProgressBar.setVisible(false);
                batchProgressBar.setManaged(false);
                etaLabel.setText(String.format(I18n.getOrDefault("analytics.status.batch_completed_fmt", "✅ Completed in %ds (%d scenarios)"), totalElapsedSec, finishedCount.get()));
                scenarioTable.refresh();
                if (onBatchCompletedCallback != null) {
                    onBatchCompletedCallback.run();
                }
                logger.info("Batch execution sequence completed in {}s for {} scenarios.", totalElapsedSec, finishedCount.get());
            });
        }, "ComparativeAnalyticsBatchParallelWorker");

        batchWorkerThread.setDaemon(true);
        batchWorkerThread.start();
    }

    public static void updateLiveBatchProgressAndEta(List<ScenarioSelectableItem> items, long startTimeMs, ProgressBar batchProgressBar, Label etaLabel) {
        if (items == null || items.isEmpty()) return;
        double sumProgress = 0.0;
        for (ScenarioSelectableItem it : items) {
            sumProgress += it.getProgress();
        }
        double overallProgress = sumProgress / items.size();
        batchProgressBar.setProgress(overallProgress);

        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        if (overallProgress > 0.02 && elapsedMs > 400) {
            long totalEstMs = (long) (elapsedMs / overallProgress);
            long remainingMs = Math.max(0, totalEstMs - elapsedMs);
            String elapsedStr = ExecutionContextPanel.formatDuration(elapsedMs / 1000.0);
            String remainingStr = ExecutionContextPanel.formatDuration(remainingMs / 1000.0);
            etaLabel.setText(String.format("⏱️ %s | %s: %s (%.0f%%)", elapsedStr, I18n.getOrDefault("analytics.label.remaining", "Remaining"), remainingStr, overallProgress * 100.0));
        }
    }
}
