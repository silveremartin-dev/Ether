/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.model.EcologyPreset;
import org.ether.society.model.Scenario;
import org.ether.society.ui.scenario.ScenarioDefaultValueService;
import org.ether.society.ui.scenario.ScenarioSanityValidator;
import org.ether.society.ui.scenario.ScenarioValidationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Package-private diagnostic, validation, localization, and auto-epoch dialog coordinator
 * extracted from {@link ScenarioSetupPanel} to uphold the Single Responsibility Principle.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 * @see ScenarioSetupPanel
 */
final class ScenarioSetupDiagnosticHelper {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioSetupDiagnosticHelper.class);

    private ScenarioSetupDiagnosticHelper() {
        // Stateless helper class
    }

    /**
     * Highlights modified spinners and combo boxes relative to canonical simulation defaults.
     *
     * @param panel owning {@link ScenarioSetupPanel}
     */
    static void updateDefaultValueIndicators(ScenarioSetupPanel panel) {
        if (panel.isUpdatingFromPreset) return;
        ScenarioDefaultValueService.highlightControlIfModified(panel.startYearSpinner, false);
        ScenarioDefaultValueService.highlightControlIfModified(panel.endYearSpinner, false);
        ScenarioDefaultValueService.highlightControlIfModified(panel.targetCohortSizeSpinner, false);
        ScenarioDefaultValueService.highlightControlIfModified(panel.temporalResolutionCombo, false);
        if (panel.demographicSectionBuilder != null) {
            ScenarioDefaultValueService.highlightControlIfModified(
                    panel.demographicSectionBuilder.getInitialHumanCountSpinner(), false);
        }
        if (panel.culturalSectionBuilder != null) {
            var dimSp = panel.culturalSectionBuilder.getCultureVectorDimSpinner();
            ScenarioDefaultValueService.highlightControlIfModified(
                    dimSp, dimSp != null && dimSp.getValue() != null && dimSp.getValue() != 9);
            var diffSp = panel.culturalSectionBuilder.getCulturalDiffusionRateSpinner();
            ScenarioDefaultValueService.highlightControlIfModified(
                    diffSp, diffSp != null && diffSp.getValue() != null && Math.abs(diffSp.getValue() - 0.05) > 0.001);
            var mutSp = panel.culturalSectionBuilder.getCulturalMutationRateSpinner();
            ScenarioDefaultValueService.highlightControlIfModified(
                    mutSp, mutSp != null && mutSp.getValue() != null && Math.abs(mutSp.getValue() - 0.01) > 0.001);
        }
    }

    /**
     * Refreshes the live sanity diagnostic card with current planetary, demographic, and cultural settings.
     *
     * @param panel owning {@link ScenarioSetupPanel}
     */
    static void updateLiveDiagnosticBlock(ScenarioSetupPanel panel) {
        if (panel.liveDiagnosticSection == null) return;
        PlanetPreset p = panel.activePlanetPreset != null
                ? panel.activePlanetPreset
                : (panel.planetPresetCombo != null ? panel.planetPresetCombo.getValue() : PlanetPreset.EARTH_LIKE);
        if (p == null) p = PlanetPreset.EARTH_LIKE;
        EcologyPreset eco = panel.ecologyPresetCombo != null
                ? panel.ecologyPresetCombo.getValue()
                : EcologyPreset.EARTH_STANDARD;
        String demoSrc = panel.demographicSectionBuilder != null && panel.demographicSectionBuilder.getDemoSourceCombo() != null
                ? panel.demographicSectionBuilder.getDemoSourceCombo().getValue()
                : null;
        Image elevImg = panel.getElevationImageForPreset(p);
        int dimsCount = panel.culturalSectionBuilder != null
                && panel.culturalSectionBuilder.getCultureVectorDimSpinner() != null
                && panel.culturalSectionBuilder.getCultureVectorDimSpinner().getValue() != null
                ? panel.culturalSectionBuilder.getCultureVectorDimSpinner().getValue()
                : 9;
        long startY = panel.startYearSpinner != null && panel.startYearSpinner.getValue() != null
                ? panel.startYearSpinner.getValue()
                : -8000L;
        long initPop = panel.demographicSectionBuilder != null
                ? panel.demographicSectionBuilder.getInitialHumanCount()
                : 1_000_000L;
        int cohortSize = panel.targetCohortSizeSpinner != null && panel.targetCohortSizeSpinner.getValue() != null
                ? panel.targetCohortSizeSpinner.getValue()
                : 150;

        panel.liveDiagnosticSection.updateDiagnostics(
                p,
                eco,
                demoSrc,
                elevImg,
                panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getCustomDensityImage() : null,
                dimsCount,
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCustomTensorImages() : Collections.emptyMap(),
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder::getCulturalTensorTitle : i -> "Cultural Tensor #" + (i + 1),
                startY,
                initPop,
                cohortSize,
                getValidationErrors(panel),
                ScenarioSanityValidator.getOrCreateJitReport()
        );
    }

    /**
     * Collects scenario setup validation errors across planetary, resource, demographic, and cultural inputs.
     *
     * @param panel owning {@link ScenarioSetupPanel}
     * @return list of localized validation error messages
     */
    static List<String> getValidationErrors(ScenarioSetupPanel panel) {
        return ScenarioValidationHelper.getValidationErrors(
                panel.planetPanelSupplier,
                panel.resourcePanelSupplier,
                panel.startYearSpinner,
                panel.endYearSpinner,
                panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getInitialHumanCountSpinner() : null,
                panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getRadioImportDemo() : null,
                panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getCustomDensityImage() : null,
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCultureVectorDimSpinner() : null,
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getTensorImportRadios() : null,
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCustomTensorImages() : null,
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder::getCulturalTensorTitle : i -> ""
        );
    }

    /**
     * Validates the scenario setup and updates error highlighting banners.
     *
     * @param panel owning {@link ScenarioSetupPanel}
     * @return {@code true} if valid with zero errors
     */
    static boolean validateScenarioSetup(ScenarioSetupPanel panel) {
        return ScenarioValidationHelper.validateAndHighlight(
                getValidationErrors(panel),
                panel.startYearSpinner,
                panel.endYearSpinner,
                panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getInitialHumanCountSpinner() : null,
                panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getRadioImportDemo() : null,
                panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getCustomDensityImage() : null,
                panel.demographicSectionBuilder != null ? panel.demographicSectionBuilder.getLoadDensityMapBtn() : null,
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCultureVectorDimSpinner() : null,
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getTensorImportRadios() : null,
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getCustomTensorImages() : null,
                panel.culturalSectionBuilder != null ? panel.culturalSectionBuilder.getTensorLoadBtns() : null,
                panel.validationErrorBanner,
                panel.validationBannerHeaderLabel,
                panel.validationErrorLabel,
                panel.configScroll,
                () -> updateLiveDiagnosticBlock(panel)
        );
    }

    /**
     * Updates localized labels, tooltips, and delegated section builders across {@link ScenarioSetupPanel}.
     *
     * @param panel owning {@link ScenarioSetupPanel}
     */
    static void updatePanelTexts(ScenarioSetupPanel panel) {
        boolean oldUpdating = panel.isUpdatingFromPreset;
        panel.isUpdatingFromPreset = true;
        try {
            if (panel.headerLabel != null) {
                panel.headerLabel.setText(I18n.getOrDefault("scenario.panel.title", "📜 Scenario Configuration & Setup"));
            }
            if (panel.btnAutoEpochScenario != null) {
                panel.btnAutoEpochScenario.setText(I18n.getOrDefault("scenario.btn.auto_epoch_wizard", "✨ Créer un Scénario Automatique par Date..."));
                panel.btnAutoEpochScenario.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.auto_epoch_wizard",
                        "Ouvre l'assistant de scénario temporel pour configurer en un clic les cartes d'élévation, biomes, ressources, démographie et moteurs de Type B compatibles pour n'importe quelle date.")));
            }
            if (panel.scenarioPresetBar != null) panel.scenarioPresetBar.updateTexts();
            if (panel.scenarioDescLabel != null) {
                panel.scenarioDescLabel.setText(I18n.getOrDefault("scenario.section.description", "📖 Detailed Description, Initial Conditions & Key Observables:"));
            }
            if (panel.startBtn != null) panel.startBtn.setText(I18n.get("scenario.start_btn"));
            if (panel.generateBtn != null) {
                panel.generateBtn.setText(I18n.getOrDefault("scenario.preview_btn", "🔄 Preview Distribution"));
            }
            if (panel.btnReliefOverlay != null) {
                panel.btnReliefOverlay.setText(I18n.getOrDefault("scenario.btn.relief_overlay", "⛏️ Relief"));
                panel.btnReliefOverlay.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.relief_overlay",
                        "Superposer l'ombrage du relief topographique et des pentes avec délimitation du trait de côte.")));
            }

            panel.planetSyncManager.updateTexts();
            panel.liveDiagnosticSection.updateTexts();
            panel.spatiotemporalSectionBuilder.updateTexts();
            panel.spatialBoundsBuilder.updateTexts();
            panel.bundleManager.updateTexts();
            if (panel.engineConfigSectionBuilder != null) panel.engineConfigSectionBuilder.updateTexts();
            if (panel.demographicSectionBuilder != null) panel.demographicSectionBuilder.updateTexts();
            if (panel.culturalSectionBuilder != null) panel.culturalSectionBuilder.updateTexts();

            panel.updatePreviewModesCombo();
            panel.updatePreviewTitleText();
        } finally {
            panel.isUpdatingFromPreset = oldUpdating;
        }
    }

    /**
     * Opens the modal {@link AutoEpochScenarioDialog} and synchronizes the generated scenario back to the panel.
     *
     * @param panel owning {@link ScenarioSetupPanel}
     */
    static void openAutoEpochScenarioDialog(ScenarioSetupPanel panel) {
        try {
            String currentPlanet = panel.activePlanetPreset != null ? panel.activePlanetPreset.getCanonicalPlanet() : "earth";
            long currentYear = panel.startYearSpinner != null && panel.startYearSpinner.getValue() != null
                    ? panel.startYearSpinner.getValue().longValue()
                    : -8000L;

            AutoEpochScenarioDialog dialog = new AutoEpochScenarioDialog(currentPlanet, currentYear, (Scenario generatedScenario) -> {
                panel.applyScenarioToUI(generatedScenario);
                if (panel.scenarioPresetBar != null) {
                    panel.scenarioPresetBar.getPresetCombo().setValue(null);
                    panel.scenarioPresetBar.setNameText(generatedScenario.getName());
                    panel.scenarioPresetBar.markClean(generatedScenario);
                }
                panel.notifyParamChange();
            });
            if (panel.getScene() != null && panel.getScene().getWindow() != null) {
                dialog.initOwner(panel.getScene().getWindow());
            }
            dialog.show();
        } catch (Exception ex) {
            logger.error("Failed to open AutoEpochScenarioDialog", ex);
        }
    }
}
