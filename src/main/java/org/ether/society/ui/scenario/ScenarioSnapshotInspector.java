/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.ether.society.i18n.I18n;
import org.ether.society.persistence.SaveMetadata;
import org.ether.society.persistence.SimulationSaveManager;
import org.ether.society.ui.SnapshotExplanationDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Autonomous UI Controller for simulation snapshot inspection, browsing, and checkpoint resumption.
 * 
 * <p>Extracted from {@code ScenarioSetupPanel} to satisfy the Single Responsibility Principle (SRP).
 * Discovers existing physical saves and rolling checkpoints in {@code saves/}, visualizes their
 * temporal metadata (epoch, moment, population, byte footprint), and coordinates warm restoration.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioSnapshotInspector {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioSnapshotInspector.class);

    private final Consumer<SaveMetadata> onSnapshotSelectedCallback;
    private final SimulationSaveManager saveManager = new SimulationSaveManager();

    private Label snapshotHeader;
    private Label snapshotSubtitle;
    private RadioButton radioResumeSnapshot;
    private RadioButton radioColdStart;
    private ComboBox<SaveMetadata> snapshotCombo;
    private Label snapshotDateLabel;
    private Label snapshotTimeLabel;
    private Label snapshotScenarioLabel;
    private Label snapshotPathLabel;
    private Label snapshotAutoPruningNotice;
    private Button snapshotExplainBtn;
    private Button snapshotRefreshBtn;
    private VBox snapshotContainer;

    /**
     * Constructs a snapshot inspector controller.
     *
     * @param onSnapshotSelectedCallback callback invoked when a snapshot selection changes
     */
    public ScenarioSnapshotInspector(Consumer<SaveMetadata> onSnapshotSelectedCallback) {
        this.onSnapshotSelectedCallback = onSnapshotSelectedCallback;
    }

    /**
     * Builds and returns the snapshot UI section.
     *
     * @param toggleGroup shared ToggleGroup for start mode (cold start vs resume)
     * @param radioColdStart reference to cold start radio button
     * @return JavaFX VBox container for snapshot controls
     */
    public VBox buildSnapshotSection(ToggleGroup toggleGroup, RadioButton radioColdStart) {
        this.radioColdStart = radioColdStart;

        VBox box = new VBox(10);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-border-color: #475569; -fx-border-radius: 8;");

        snapshotHeader = new Label(I18n.getOrDefault("scenario.snapshot.header", "📸 7. RESUME FROM EXISTING SNAPSHOT (PREVIOUS SESSION)"));
        snapshotHeader.getStyleClass().add("label-section-header");
        snapshotHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-size: 13px;");

        snapshotSubtitle = new Label(I18n.getOrDefault("scenario.desc.snapshot", 
                "If the simulation was previously run and snapshots or checkpoints exist, you can resume directly from that snapshot without starting over."));
        snapshotSubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");
        snapshotSubtitle.setWrapText(true);

        radioResumeSnapshot = new RadioButton(I18n.getOrDefault("scenario.radio.start_snapshot", "📸 Resume from an existing Snapshot (Previous Session / Checkpoint)"));
        radioResumeSnapshot.setToggleGroup(toggleGroup);
        radioResumeSnapshot.setTooltip(new Tooltip(I18n.getOrDefault("scenario.radio.start_snapshot.tooltip", "Load a previously saved physical state or periodic checkpoint directly into memory.")));
        radioResumeSnapshot.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 12px; -fx-font-weight: bold;");

        snapshotCombo = new ComboBox<>();
        snapshotCombo.setMaxWidth(Double.MAX_VALUE);
        snapshotCombo.setTooltip(new Tooltip(I18n.getOrDefault("scenario.snapshot.combo.tooltip", "Select an existing checkpoint or full simulation snapshot from the disk to resume execution.")));
        snapshotCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(SaveMetadata meta) {
                if (meta == null) return I18n.getOrDefault("scenario.snapshot.none", "-- No Snapshot Available --");
                String dtStr = meta.getTimestamp() != null ? meta.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "?";
                String yrStr = meta.getYear() != 0 ? String.format("Year %,d", meta.getYear()) : "Year ?";
                return String.format("[%s] %s — %s", dtStr, yrStr, meta.getName() != null ? meta.getName() : meta.getId());
            }

            @Override
            public SaveMetadata fromString(String string) {
                return null;
            }
        });

        snapshotCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SaveMetadata meta, boolean empty) {
                super.updateItem(meta, empty);
                if (empty || meta == null) {
                    setText(null);
                } else {
                    String dtStr = meta.getTimestamp() != null ? meta.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "?";
                    String yrStr = meta.getYear() != 0 ? String.format("Year %,d", meta.getYear()) : "Year ?";
                    setText(String.format("[%s] %s — %s", dtStr, yrStr, meta.getName() != null ? meta.getName() : meta.getId()));
                }
            }
        });

        snapshotCombo.setOnShowing(e -> refreshSnapshotList());
        snapshotCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            updateSnapshotDetailsDisplay(newV);
            if (onSnapshotSelectedCallback != null) {
                onSnapshotSelectedCallback.accept(newV);
            }
        });

        snapshotDateLabel = new Label(I18n.getOrDefault("scenario.label.timestamp_null", "📅 Timestamp: -"));
        snapshotTimeLabel = new Label(I18n.getOrDefault("scenario.label.moment_null", "⏳ Time: -"));
        snapshotScenarioLabel = new Label(I18n.getOrDefault("scenario.info.scenario_empty", "📜 Scenario: -"));
        snapshotPathLabel = new Label(I18n.getOrDefault("scenario.label.path_null", "📁 Folder: -"));
        snapshotDateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        snapshotTimeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        snapshotScenarioLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        snapshotPathLabel.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #64748b;");

        snapshotExplainBtn = new Button(I18n.getOrDefault("scenario.btn.snapshot_explain", "ℹ️ What is a Snapshot? (Explanations & Mechanics)"));
        snapshotExplainBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #334155; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        snapshotExplainBtn.setOnAction(e -> {
            Window owner = box.getScene() != null ? box.getScene().getWindow() : null;
            SnapshotExplanationDialog dialog = new SnapshotExplanationDialog(owner);
            dialog.showAndWait();
        });

        snapshotRefreshBtn = new Button(I18n.getOrDefault("scenario.btn.refresh", "🔄 Refresh Snapshots"));
        snapshotRefreshBtn.setStyle("-fx-font-size: 11px;");
        snapshotRefreshBtn.setOnAction(e -> refreshSnapshotList());

        HBox btnRow = new HBox(8, snapshotExplainBtn, snapshotRefreshBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        snapshotAutoPruningNotice = new Label(I18n.getOrDefault("scenario.snapshot.auto_pruning_notice",
                "ℹ️ Gestion Automatique : Les snapshots sont générés automatiquement par le moteur à intervalles réguliers et élagués logarithmiquement dans le temps."));
        snapshotAutoPruningNotice.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #64748b; -fx-font-style: italic;");
        snapshotAutoPruningNotice.setWrapText(true);

        snapshotContainer = new VBox(6, snapshotCombo, snapshotDateLabel, snapshotTimeLabel, snapshotScenarioLabel, snapshotPathLabel, btnRow, snapshotAutoPruningNotice);
        snapshotContainer.disableProperty().bind(radioResumeSnapshot.selectedProperty().not());

        box.getChildren().addAll(snapshotHeader, snapshotSubtitle, radioResumeSnapshot, snapshotContainer);
        refreshSnapshotList();
        return box;
    }

    /**
     * Rescans disk directory for simulation saves and checkpoints.
     */
    public void refreshSnapshotList() {
        refreshSnapshotListForScenario(null);
    }

    private List<SaveMetadata> cachedSavesList = null;
    private long lastSavesListFetchTime = 0;

    /**
     * Rescans and filters saves matching the given scenario boundaries and temporal era.
     *
     * @param s target scenario
     */
    public void refreshSnapshotListForScenario(org.ether.society.model.Scenario s) {
        if (snapshotCombo == null) return;
        long now = System.currentTimeMillis();
        if (cachedSavesList == null || (now - lastSavesListFetchTime > 3000)) {
            try {
                cachedSavesList = saveManager.listSaves();
                lastSavesListFetchTime = now;
            } catch (Exception e) {
                cachedSavesList = Collections.emptyList();
            }
        }
        List<SaveMetadata> allSaves = cachedSavesList != null ? cachedSavesList : Collections.emptyList();
        List<SaveMetadata> matchingSaves = new java.util.ArrayList<>();

        if (s != null && !allSaves.isEmpty()) {
            long targetStartYear = s.getStartDateYear();
            long targetEndYear = s.getEndDateYear();
            String scNorm = s.getName() != null ? s.getName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String scKey = s.getPresetKey() != null ? s.getPresetKey().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String scDisp = s.getDisplayName() != null ? s.getDisplayName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
            String scDensity = s.getPopulationDensityType() != null ? s.getPopulationDensityType().toLowerCase().replaceAll("[^a-z0-9]", "") : "";

            for (SaveMetadata save : allSaves) {
                String saveNorm = save.getName() != null ? save.getName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";
                String saveScNorm = save.getScenarioName() != null ? save.getScenarioName().toLowerCase().replaceAll("[^a-z0-9]", "") : "";

                boolean match = (!scKey.isEmpty() && (saveNorm.contains(scKey) || saveScNorm.contains(scKey)))
                             || (!scNorm.isEmpty() && (saveNorm.contains(scNorm) || saveScNorm.contains(scNorm)))
                             || (!scDisp.isEmpty() && (saveNorm.contains(scDisp) || saveScNorm.contains(scDisp)))
                             || (!scDensity.isEmpty() && (saveNorm.contains(scDensity) || saveScNorm.contains(scDensity)))
                             || (Math.abs(save.getYear() - targetStartYear) <= 150)
                             || (Math.abs(save.getYear() - targetEndYear) <= 150);

                if (match) {
                    matchingSaves.add(save);
                }
            }
        } else {
            matchingSaves.addAll(allSaves);
        }

        // Deduplicate snapshots covering the same year / scenario (keep the newest save)
        java.util.Map<Long, SaveMetadata> deduplicatedByYear = new java.util.LinkedHashMap<>();
        matchingSaves.sort((a, b) -> {
            if (a.getTimestamp() != null && b.getTimestamp() != null) {
                return b.getTimestamp().compareTo(a.getTimestamp());
            }
            return 0;
        });
        for (var save : matchingSaves) {
            deduplicatedByYear.putIfAbsent(save.getYear(), save);
        }

        List<SaveMetadata> filteredSaves = new java.util.ArrayList<>(deduplicatedByYear.values());
        filteredSaves.sort((a, b) -> Long.compare(b.getYear(), a.getYear()));

        snapshotCombo.getItems().setAll(filteredSaves);

        if (!filteredSaves.isEmpty()) {
            if (radioResumeSnapshot != null) {
                radioResumeSnapshot.setDisable(false);
                radioResumeSnapshot.setSelected(true);
            }
            snapshotCombo.setValue(filteredSaves.get(0));
            updateSnapshotDetailsDisplay(filteredSaves.get(0));
        } else {
            if (radioResumeSnapshot != null) {
                radioResumeSnapshot.setDisable(true);
                if (radioColdStart != null) {
                    radioColdStart.setSelected(true);
                }
            }
            snapshotCombo.setValue(null);
            snapshotCombo.setPromptText(I18n.getOrDefault("scenario.snapshot.no_saves_for_scenario", "(Aucun snapshot enregistré pour ce scénario)"));
            updateSnapshotDetailsDisplay(null);
        }
        logger.info("Refreshed snapshots list for scenario '{}': {} matching checkpoint(s) found on disk.", s != null ? s.getName() : "N/A", filteredSaves.size());
    }

    /**
     * Updates details display for the currently selected snapshot metadata.
     *
     * @param meta save metadata
     */
    public void updateSnapshotDetailsDisplay(SaveMetadata meta) {
        if (snapshotDateLabel == null) return;
        if (meta == null) {
            snapshotDateLabel.setText(I18n.getOrDefault("scenario.label.timestamp_null", "📅 Timestamp: -"));
            snapshotTimeLabel.setText(I18n.getOrDefault("scenario.label.moment_null", "⏳ Time: -"));
            snapshotScenarioLabel.setText(I18n.getOrDefault("scenario.info.scenario_empty", "📜 Scenario: -"));
            snapshotPathLabel.setText(I18n.getOrDefault("scenario.label.path_null", "📁 Folder: -"));
            return;
        }
        String dtStr = meta.getTimestamp() != null ? meta.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-";
        snapshotDateLabel.setText("📅 " + I18n.getOrDefault("scenario.label.timestamp", "Recorded at:") + " " + dtStr);
        String yrStr = meta.getYear() != 0 ? String.format("Year %,d (Month %02d)", meta.getYear(), meta.getMonth()) : "-";
        snapshotTimeLabel.setText("⏳ " + I18n.getOrDefault("scenario.label.simulation_moment", "Simulation Era:") + " " + yrStr);
        snapshotScenarioLabel.setText("📜 " + I18n.getOrDefault("scenario.label.associated_scenario", "Scenario:") + " " + (meta.getScenarioName() != null ? meta.getScenarioName() : meta.getName()));
        snapshotPathLabel.setText("📁 saves/" + meta.getId());
    }

    /**
     * Adapts start button text based on whether a cold start or resume is active.
     *
     * @param startBtn simulation launch button
     * @param startYr configured starting year
     * @param endYr configured ending year
     */
    public void updateStartButtonContext(Button startBtn, long startYr, long endYr) {
        if (startBtn == null) return;
        if (isResumeFromSnapshotSelected()) {
            SaveMetadata meta = getSelectedSnapshotMetadata();
            long snapYr = (meta != null && meta.getYear() != 0) ? meta.getYear() : startYr;
            if (snapYr >= endYr) {
                startBtn.setText(String.format(I18n.getOrDefault("scenario.btn.load_complete_snapshot", 
                        "⚡ CHARGER L'HISTORIQUE & ÉTAT COMPLET (An %,d — 100%% Calculé)"), snapYr));
            } else {
                startBtn.setText(String.format(I18n.getOrDefault("scenario.btn.resume_partial_snapshot", 
                        "🚀 REPRENDRE LE CHECKPOINT & CONTINUER (An %,d ➔ %,d)"), snapYr, endYr));
            }
        } else {
            startBtn.setText(I18n.getOrDefault("scenario.btn.start_sim", "🚀 LANCER LA SIMULATION"));
        }
    }

    /**
     * Checks if user has toggled resumption from snapshot.
     *
     * @return true if resume radio button is checked
     */
    public boolean isResumeFromSnapshotSelected() {
        return radioResumeSnapshot != null && radioResumeSnapshot.isSelected();
    }

    /**
     * Gets currently selected snapshot metadata.
     *
     * @return save metadata, or null if none selected
     */
    public SaveMetadata getSelectedSnapshotMetadata() {
        return snapshotCombo != null ? snapshotCombo.getValue() : null;
    }

    /**
     * Gets currently selected snapshot folder identifier.
     *
     * @return save ID string, or null
     */
    public String getSelectedSnapshotId() {
        SaveMetadata meta = getSelectedSnapshotMetadata();
        return meta != null ? meta.getId() : null;
    }

    /**
     * Refreshes localized labels across the snapshot section.
     */
    public void updateTexts() {
        if (snapshotHeader != null) snapshotHeader.setText(I18n.getOrDefault("scenario.snapshot.header", "📸 7. RESUME FROM EXISTING SNAPSHOT (PREVIOUS SESSION)"));
        if (snapshotSubtitle != null) snapshotSubtitle.setText(I18n.getOrDefault("scenario.desc.snapshot", "If the simulation was previously run and snapshots or checkpoints exist, you can resume directly from that snapshot without starting over."));
        if (radioResumeSnapshot != null) radioResumeSnapshot.setText(I18n.getOrDefault("scenario.radio.start_snapshot", "📸 Resume from an existing Snapshot (Previous Session / Checkpoint)"));
        if (snapshotExplainBtn != null) snapshotExplainBtn.setText(I18n.getOrDefault("scenario.btn.snapshot_explain", "ℹ️ What is a Snapshot? (Explanations & Mechanics)"));
        if (snapshotRefreshBtn != null) snapshotRefreshBtn.setText(I18n.getOrDefault("scenario.btn.refresh", "🔄 Refresh Snapshots"));
        if (snapshotAutoPruningNotice != null) snapshotAutoPruningNotice.setText(I18n.getOrDefault("scenario.snapshot.auto_pruning_notice", "ℹ️ Gestion Automatique : Les snapshots sont générés automatiquement par le moteur à intervalles réguliers et élagués logarithmiquement dans le temps."));
        if (snapshotCombo != null) updateSnapshotDetailsDisplay(snapshotCombo.getValue());
    }
}
