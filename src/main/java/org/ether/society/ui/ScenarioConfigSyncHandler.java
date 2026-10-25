/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.generation.PlanetPreset;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.ui.scenario.ScenarioEpochCatalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles bidirectional synchronization between {@link Scenario} data models and {@link ScenarioSetupPanel} UI controls.
 *
 * <p>Extracted from {@code ScenarioSetupPanel} to satisfy the Single Responsibility Principle (SRP)
 * and isolate the serialization/deserialization mapping between scenario domain objects and UI form fields.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ScenarioConfigSyncHandler {

    private final ScenarioSetupPanel panel;

    /**
     * Constructs a synchronization handler bound to the specified panel.
     *
     * @param panel the hosting scenario setup panel
     */
    public ScenarioConfigSyncHandler(ScenarioSetupPanel panel) {
        this.panel = panel;
    }

    /**
     * Applies a {@link Scenario} data model onto the panel controls.
     *
     * @param s the scenario to populate the UI with
     */
    public void applyScenarioToUI(Scenario s) {
        if (s == null) return;
        panel.isUpdatingFromPreset = true;
        try {
            if (panel.scenarioPresetBar != null) {
                panel.scenarioPresetBar.setNameText(s.getName());
            }
            if (panel.scenarioDescriptionArea != null) {
                panel.scenarioDescriptionArea.setText(s.getDescription() != null ? s.getDescription() : "");
            }

            boolean isEarth = true;
            if (s.getPlanetPreset() != null) {
                isEarth = "earth".equalsIgnoreCase(s.getPlanetPreset().getCanonicalPlanet());
            } else if (panel.activePlanetPreset != null) {
                isEarth = "earth".equalsIgnoreCase(panel.activePlanetPreset.getCanonicalPlanet());
            }

            if (isEarth && s.getCustomDensityBase64() == null) {
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    org.ether.society.data.HistoricalMapGenerator.populateScenarioHistoricalMaps(s);
                });
            }

            if (s.getEcologyPreset() != null && panel.ecologyPresetCombo != null) {
                panel.ecologyPresetCombo.setValue(s.getEcologyPreset());
            } else if (s.getEcologyPresetName() != null && panel.ecologyPresetCombo != null) {
                for (EcologyPreset eco : panel.ecologyPresetCombo.getItems()) {
                    if (eco.name().equalsIgnoreCase(s.getEcologyPresetName())) {
                        panel.ecologyPresetCombo.setValue(eco);
                        break;
                    }
                }
            }

            EcologyPreset activeEco = panel.ecologyPresetCombo != null ? panel.ecologyPresetCombo.getValue() : null;
            PlanetPreset cascaded = activeEco != null ? panel.findPlanetPresetByName(activeEco.planetPresetName()) : s.getPlanetPreset();
            if (cascaded != null) {
                panel.activePlanetPreset = cascaded;
                if (panel.planetPresetCombo != null) {
                    panel.planetPresetCombo.setValue(cascaded);
                }
            }
            panel.planetSyncManager.updateInheritedContextDisplay(activeEco != null ? activeEco.name() : null);

            panel.spatiotemporalSectionBuilder.applyScenario(s);
            panel.demographicSectionBuilder.applyScenario(s);
            panel.culturalSectionBuilder.applyScenario(s);
            panel.spatialBoundsBuilder.applyScenario(s);
            if (panel.engineConfigSectionBuilder != null) {
                panel.engineConfigSectionBuilder.applyScenario(s);
            }

            if (s.getSeed() != 0 && panel.demoSeedField != null) {
                panel.demoSeedField.setText(String.valueOf(s.getSeed()));
            }
            if (s.getCulturalSeed() != 0 && panel.cultSeedField != null) {
                panel.cultSeedField.setText(String.valueOf(s.getCulturalSeed()));
            }
            if (panel.randomEventsCheckBox != null) {
                panel.randomEventsCheckBox.setSelected(s.isRandomEventsEnabled());
            }
            if (panel.earthLeadersCheckBox != null) {
                panel.earthLeadersCheckBox.setSelected(s.isEarthHistoricalLeadersEnabled());
            }
            if (panel.proceduralLeadersCheckBox != null) {
                panel.proceduralLeadersCheckBox.setSelected(s.isProceduralLeadersEnabled());
            }

            if (panel.demographicSectionBuilder != null && panel.demographicSectionBuilder.getDemoSourceCombo() != null) {
                String autoSrc = panel.demographicSectionBuilder.pickDemoSourceForEpoch(s.getStartDateYear(), panel.activePlanetPreset);
                ScenarioEpochCatalog.selectBestSource(panel.demographicSectionBuilder.getDemoSourceCombo(), autoSrc, "Terre");
            }

            if (panel.eventsList != null) {
                panel.eventsList.clear();
                if (s.getClimateEvents() != null && !s.getClimateEvents().isEmpty()) {
                    for (org.ether.society.model.ClimateEvent ce : s.getClimateEvents()) {
                        panel.eventsList.add(new ScenarioSetupPanel.ClimateEvent(ce.getType(), ce.getName(), ce.getYear(), ce.getLatitude(), ce.getLongitude(), ce.getDepth(), ce.getMagnitude()));
                    }
                }
            }

            panel.updatePreviewModesCombo();
            if (panel.previewModeCombo != null) {
                panel.previewModeCombo.getSelectionModel().select(0);
            }
            panel.currentPreviewCells = null;
            panel.generatePreview();
            if (panel.onScenarioLoadedCallback != null) {
                panel.onScenarioLoadedCallback.accept(panel.activePlanetPreset, activeEco != null ? activeEco : s.getEcologyPreset());
            }
            panel.updatePerformanceControlsState();
            if (panel.scenarioPresetBar != null) {
                panel.scenarioPresetBar.markClean(s);
            }
            panel.refreshSnapshotListForScenario(s);
        } finally {
            panel.isUpdatingFromPreset = false;
        }
    }

    /**
     * Constructs and serializes a new {@link Scenario} data model from current UI control values.
     *
     * @return populated {@link Scenario}
     */
    public Scenario exportScenario() {
        Scenario s = new Scenario();
        s.setName(panel.scenarioPresetBar != null ? panel.scenarioPresetBar.getCurrentName() : "New Scenario");
        if (panel.scenarioDescriptionArea != null) {
            s.setDescription(panel.scenarioDescriptionArea.getText());
        }
        panel.spatiotemporalSectionBuilder.exportToScenario(s);
        panel.demographicSectionBuilder.exportToScenario(s);
        panel.culturalSectionBuilder.exportToScenario(s);
        panel.spatialBoundsBuilder.exportToScenario(s);
        if (panel.engineConfigSectionBuilder != null) {
            panel.engineConfigSectionBuilder.exportToScenario(s);
        }

        s.setEcologyPreset(panel.ecologyPresetCombo != null ? panel.ecologyPresetCombo.getValue() : null);
        if (panel.ecologyPresetCombo != null && panel.ecologyPresetCombo.getValue() != null) {
            s.setEcologyPresetName(panel.ecologyPresetCombo.getValue().name());
        }
        s.setPlanetPreset(panel.activePlanetPreset != null ? panel.activePlanetPreset : (panel.planetPresetCombo != null ? panel.planetPresetCombo.getValue() : null));

        long seedVal = 12345L;
        try {
            if (panel.demoSeedField != null) seedVal = Long.parseLong(panel.demoSeedField.getText());
        } catch (NumberFormatException ignored) {}
        s.setSeed(seedVal);

        long cultSeedVal = 54321L;
        try {
            if (panel.cultSeedField != null) cultSeedVal = Long.parseLong(panel.cultSeedField.getText());
        } catch (NumberFormatException ignored) {}
        s.setCulturalSeed(cultSeedVal);
        s.setRandomEventsEnabled(panel.randomEventsCheckBox == null || panel.randomEventsCheckBox.isSelected());
        s.setEarthHistoricalLeadersEnabled(panel.earthLeadersCheckBox == null || panel.earthLeadersCheckBox.isSelected());
        s.setProceduralLeadersEnabled(panel.proceduralLeadersCheckBox == null || panel.proceduralLeadersCheckBox.isSelected());

        if (panel.eventsList != null) {
            List<org.ether.society.model.ClimateEvent> sEvents = new ArrayList<>();
            for (ScenarioSetupPanel.ClimateEvent ce : panel.eventsList) {
                sEvents.add(new org.ether.society.model.ClimateEvent(ce.getType(), ce.getName(), ce.getYear(), ce.getLatitude(), ce.getLongitude(), ce.getDepth(), ce.getMagnitude()));
            }
            s.setClimateEvents(sEvents);
        }

        return s;
    }
}
