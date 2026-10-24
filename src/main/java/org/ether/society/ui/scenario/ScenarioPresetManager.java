/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.ether.society.persistence.EtherJsonUtil;
import org.ether.society.persistence.PresetStorageService;
import org.ether.society.persistence.ScenarioRepository;
import org.ether.society.ui.PresetControlBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Controller managing Scenario preset selection, JSON export/import, persistence into the
 * scenario repository, and dirty state tracking.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioPresetManager {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioPresetManager.class);

    /**
     * Host context contract connecting the preset manager to the parent setup panel.
     */
    public interface HostContext {
        Window getWindow();
        boolean validateScenarioSetup();
        Scenario getScenario();
        void applyScenarioToUI(Scenario s);
        void selectPreviewMode(int modeIndex);
        void refreshSnapshotList();
        void refreshSnapshotListForScenario(Scenario s);
        void updateStartButtonLabel();
        void notifyParamChange();
    }

    private final HostContext host;
    private final ScenarioRepository scenarioRepo;

    private PresetControlBar<Scenario> scenarioPresetBar;
    private Label scenarioPresetHeader;
    private Label scenarioDescLabel;
    private TextArea scenarioDescriptionArea;
    private boolean isUpdatingFromPreset = false;

    /**
     * Constructs a scenario preset manager.
     *
     * @param host callback context
     * @param scenarioRepo scenario database repository
     */
    public ScenarioPresetManager(HostContext host, ScenarioRepository scenarioRepo) {
        this.host = host;
        this.scenarioRepo = scenarioRepo;
        initPresetBar();
    }

    private void initPresetBar() {
        scenarioPresetBar = new PresetControlBar<>("scenario.preset_bar", "1. PRÉRÉGLAGES DE SCÉNARIOS");
        scenarioPresetBar.setExportCategory("scenario");

        List<Scenario> builtInScenarios = getBuiltInScenarios();
        Scenario defaultScenario = builtInScenarios.isEmpty() ? null : builtInScenarios.get(0);

        scenarioPresetBar.setListener(new PresetControlBar.PresetActionsListener<Scenario>() {
            @Override
            public void onPresetSelected(Scenario scenario) {
                if (host != null) {
                    host.applyScenarioToUI(scenario);
                    host.selectPreviewMode(0);
                }
            }

            @Override
            public void onSavePreset(String name) {
                if (host == null || !host.validateScenarioSetup()) {
                    return;
                }
                Scenario custom = host.getScenario();
                custom.setName(name);
                if (scenarioRepo != null) {
                    scenarioRepo.saveOrUpdate(custom);
                }
                scenarioPresetBar.getPresetCombo().getItems().removeIf(s -> s != null && name.equalsIgnoreCase(s.getName()));
                scenarioPresetBar.getPresetCombo().getItems().add(custom);
                scenarioPresetBar.getPresetCombo().setValue(custom);
                scenarioPresetBar.setNameText(name);
            }

            @Override
            public void onDeletePreset(Scenario scenario) {
                scenarioPresetBar.getPresetCombo().getItems().remove(scenario);
            }

            @Override
            public void onExportPreset(File targetFile, Scenario scenario) {
                try {
                    ObjectMapper mapper = EtherJsonUtil.getMapper();
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                    mapper.writeValue(targetFile, scenario);
                    logger.info("Exported scenario preset to {}", targetFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to export scenario preset", ex);
                }
            }

            @Override
            public void onImportPreset(File sourceFile) {
                try {
                    ObjectMapper mapper = EtherJsonUtil.getMapper();
                    Scenario scenario = mapper.readValue(sourceFile, Scenario.class);
                    scenarioPresetBar.getPresetCombo().getItems().add(scenario);
                    scenarioPresetBar.getPresetCombo().setValue(scenario);
                    if (host != null) {
                        host.applyScenarioToUI(scenario);
                    }
                    logger.info("Imported scenario preset from {}", sourceFile.getAbsolutePath());
                } catch (IOException ex) {
                    logger.error("Failed to import scenario preset", ex);
                }
            }
        });

        scenarioPresetBar.setPresets(builtInScenarios, defaultScenario);
    }

    /**
     * Loads all canonical built-in and saved scenarios.
     *
     * @return list of scenarios
     */
    public List<Scenario> getBuiltInScenarios() {
        return scenarioRepo != null ? scenarioRepo.getAllScenarios() : PresetStorageService.loadAllScenarios();
    }

    /**
     * Refreshes the list of scenario presets on the JavaFX UI thread.
     */
    public void refreshScenarioPresets() {
        Platform.runLater(() -> {
            try {
                List<Scenario> all = getBuiltInScenarios();
                Scenario current = scenarioPresetBar != null && scenarioPresetBar.getPresetCombo() != null
                        ? scenarioPresetBar.getPresetCombo().getValue() : null;
                if (scenarioPresetBar != null) {
                    scenarioPresetBar.setPresets(all, current != null ? current : (all.isEmpty() ? null : all.get(0)));
                }
                if (host != null) {
                    host.refreshSnapshotList();
                }
                logger.info("Successfully refreshed {} scenario preset(s) into UI.", all.size());
            } catch (Exception e) {
                logger.warn("Could not refresh scenario presets: {}", e.getMessage());
            }
        });
    }

    /**
     * Prompts the user to save unsaved modifications if dirty.
     *
     * @param owner parent window
     * @return {@code true} to proceed, {@code false} if aborted
     */
    public boolean promptSaveIfDirty(Window owner) {
        if (scenarioPresetBar == null) return true;
        return scenarioPresetBar.promptSavePresetIfDirty(owner);
    }

    /**
     * Checks if current scenario parameters differ from the active saved preset.
     *
     * @return {@code true} if modified, {@code false} otherwise
     */
    public boolean isDirty() {
        return scenarioPresetBar != null && scenarioPresetBar.isDirty();
    }

    /**
     * Marks the preset state as clean with the current scenario.
     *
     * @param s scenario model
     */
    public void markClean(Scenario s) {
        if (scenarioPresetBar != null) {
            scenarioPresetBar.markClean(s);
        }
    }

    /**
     * Marks the preset state as clean.
     */
    public void markClean() {
        if (scenarioPresetBar != null) {
            scenarioPresetBar.markClean();
        }
    }

    /**
     * Updates localized UI strings for preset bar and description area.
     */
    public void updateTexts() {
        if (scenarioPresetHeader != null) {
            scenarioPresetHeader.setText(I18n.getOrDefault("scenario.section.presets", "🎛️ GLOBAL PRESETS & SCENARIO SAVE"));
        }
        if (scenarioPresetBar != null) {
            scenarioPresetBar.updateTexts();
        }
        if (scenarioDescLabel != null) {
            scenarioDescLabel.setText(I18n.getOrDefault("scenario.section.description", "📖 Detailed Description, Initial Conditions & Key Observables:"));
        }
    }

    public PresetControlBar<Scenario> getPresetBar() { return scenarioPresetBar; }
    public Label getScenarioPresetHeader() { return scenarioPresetHeader; }
    public void setScenarioPresetHeader(Label header) { this.scenarioPresetHeader = header; }
    public Label getScenarioDescLabel() { return scenarioDescLabel; }
    public void setScenarioDescLabel(Label label) { this.scenarioDescLabel = label; }
    public TextArea getScenarioDescriptionArea() { return scenarioDescriptionArea; }
    public void setScenarioDescriptionArea(TextArea area) { this.scenarioDescriptionArea = area; }
    public boolean isUpdatingFromPreset() { return isUpdatingFromPreset; }
    public void setUpdatingFromPreset(boolean updating) { this.isUpdatingFromPreset = updating; }
}
