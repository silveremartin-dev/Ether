/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.PlanetGeneratorPanel;
import org.ether.society.ui.ResourceDistributionPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Validates scenario parameters across planetary context, resource distribution, demographics,
 * temporal bounds, and cultural tensor raster layers.
 *
 * <p>Enforces physical and simulation invariants before initiating compute passes or saving presets.
 * Highlights invalid controls with visual warnings and maintains the error banner state.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ScenarioValidationHelper {

    private ScenarioValidationHelper() {
        // Utility class
    }

    /**
     * Aggregates validation error messages from all simulation layers.
     *
     * @param planetPanelSupplier supplier for planet generation panel
     * @param resourcePanelSupplier supplier for resource distribution panel
     * @param startYearSpinner chronological start year spinner
     * @param endYearSpinner chronological target end year spinner
     * @param initialHumanCountSpinner initial population count spinner
     * @param radioImportDemo demographic import mode radio button
     * @param customDensityImage imported demographic raster image
     * @param cultureVectorDimSpinner cultural tensor dimension spinner
     * @param tensorImportRadios mapping of tensor index to import radio buttons
     * @param customTensorImages mapping of tensor index to imported raster images
     * @param tensorTitleLookup function resolving tensor title by index
     * @return list of localized validation error descriptions
     */
    public static List<String> getValidationErrors(
            Supplier<PlanetGeneratorPanel> planetPanelSupplier,
            Supplier<ResourceDistributionPanel> resourcePanelSupplier,
            Spinner<Integer> startYearSpinner,
            Spinner<Integer> endYearSpinner,
            Spinner<Long> initialHumanCountSpinner,
            RadioButton radioImportDemo,
            Image customDensityImage,
            Spinner<Integer> cultureVectorDimSpinner,
            Map<Integer, RadioButton> tensorImportRadios,
            Map<Integer, Image> customTensorImages,
            Function<Integer, String> tensorTitleLookup
    ) {
        List<String> errors = new ArrayList<>();
        if (planetPanelSupplier != null && planetPanelSupplier.get() != null) {
            errors.addAll(planetPanelSupplier.get().getValidationErrors());
        }
        if (resourcePanelSupplier != null && resourcePanelSupplier.get() != null) {
            errors.addAll(resourcePanelSupplier.get().getValidationErrors());
        }
        if (startYearSpinner != null && endYearSpinner != null && startYearSpinner.getValue() != null && endYearSpinner.getValue() != null) {
            int startYr = startYearSpinner.getValue();
            int endYr = endYearSpinner.getValue();
            if (startYr >= endYr) {
                errors.add(I18n.getOrDefault("scenario.validation.invalid_years", "Start year must be strictly less than end year (Tab 3)."));
            }
        }
        if (initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null) {
            long count = initialHumanCountSpinner.getValue();
            if (count <= 0) {
                errors.add(I18n.getOrDefault("scenario.validation.invalid_pop", "Initial human population must be greater than 0 (Tab 3)."));
            }
        }
        if (radioImportDemo != null && radioImportDemo.isSelected()) {
            if (customDensityImage == null) {
                errors.add(I18n.getOrDefault("scenario.validation.missing_density_map", "Missing demographic density map in import mode (Tab 3)."));
            } else {
                ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(customDensityImage);
                if (!val.valid()) {
                    errors.add(String.format(I18n.getOrDefault("scenario.validation.invalid_density_map", "Incompatible demographic density map (Tab 3): %s"), val.message()));
                }
            }
        }
        if (cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null) {
            int dims = cultureVectorDimSpinner.getValue();
            if (dims < 0 || dims > 32) {
                errors.add(I18n.getOrDefault("scenario.validation.invalid_tensor_dim", "Cultural tensor dimension must be between 0 and 32 (Tab 3)."));
            }
        }
        int dimsCount = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        if (tensorImportRadios != null && customTensorImages != null) {
            for (int i = 0; i < dimsCount; i++) {
                RadioButton importRadio = tensorImportRadios.get(i);
                if (importRadio != null && importRadio.isSelected()) {
                    Image img = customTensorImages.get(i);
                    String tensorName = tensorTitleLookup != null ? tensorTitleLookup.apply(i) : ("Tensor #" + i);
                    if (img == null) {
                        errors.add(String.format(I18n.getOrDefault("scenario.validation.missing_tensor_map", "Missing external map for %s in import mode (Tab 3)."), tensorName));
                    } else {
                        ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(img);
                        if (!val.valid()) {
                            errors.add(String.format(I18n.getOrDefault("scenario.validation.invalid_tensor_map", "Incompatible or unreadable map for %s (Tab 3): %s"), tensorName, val.message()));
                        }
                    }
                }
            }
        }
        return errors;
    }

    /**
     * Validates scenario controls, applies visual error styles to invalid inputs, and renders
     * the error banner.
     *
     * @return {@code true} if all simulation parameters are valid, {@code false} otherwise
     */
    public static boolean validateAndHighlight(
            List<String> errors,
            Spinner<Integer> startYearSpinner,
            Spinner<Integer> endYearSpinner,
            Spinner<Long> initialHumanCountSpinner,
            RadioButton radioImportDemo,
            Image customDensityImage,
            Button loadDensityMapBtn,
            Spinner<Integer> cultureVectorDimSpinner,
            Map<Integer, RadioButton> tensorImportRadios,
            Map<Integer, Image> customTensorImages,
            Map<Integer, Button> tensorLoadBtns,
            VBox validationErrorBanner,
            Label validationBannerHeaderLabel,
            Label validationErrorLabel,
            ScrollPane configScroll,
            Runnable liveDiagnosticUpdater
    ) {
        boolean isValid = errors == null || errors.isEmpty();

        // Control border highlights
        if (startYearSpinner != null && endYearSpinner != null && startYearSpinner.getValue() != null && endYearSpinner.getValue() != null) {
            int startYr = startYearSpinner.getValue();
            int endYr = endYearSpinner.getValue();
            if (startYr >= endYr) {
                startYearSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
                endYearSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                startYearSpinner.setStyle("");
                endYearSpinner.setStyle("");
            }
        }
        if (initialHumanCountSpinner != null && initialHumanCountSpinner.getValue() != null) {
            long count = initialHumanCountSpinner.getValue();
            if (count <= 0) {
                initialHumanCountSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                initialHumanCountSpinner.setStyle("");
            }
        }
        if (radioImportDemo != null && radioImportDemo.isSelected()) {
            if (customDensityImage == null || !ImageMapLoader.validateMapImage(customDensityImage).valid()) {
                if (loadDensityMapBtn != null) loadDensityMapBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else if (loadDensityMapBtn != null) {
                loadDensityMapBtn.setStyle("");
            }
        } else if (loadDensityMapBtn != null) {
            loadDensityMapBtn.setStyle("");
        }
        if (cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null) {
            int dims = cultureVectorDimSpinner.getValue();
            if (dims < 0 || dims > 32) {
                cultureVectorDimSpinner.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            } else {
                cultureVectorDimSpinner.setStyle("");
            }
        }
        int dimsCount = cultureVectorDimSpinner != null && cultureVectorDimSpinner.getValue() != null ? cultureVectorDimSpinner.getValue() : 9;
        if (tensorImportRadios != null && customTensorImages != null && tensorLoadBtns != null) {
            for (int i = 0; i < dimsCount; i++) {
                RadioButton importRadio = tensorImportRadios.get(i);
                Button loadBtn = tensorLoadBtns.get(i);
                if (importRadio != null && importRadio.isSelected()) {
                    Image img = customTensorImages.get(i);
                    if (img == null || !ImageMapLoader.validateMapImage(img).valid()) {
                        if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
                    } else if (loadBtn != null) {
                        loadBtn.setStyle("");
                    }
                } else if (loadBtn != null) {
                    loadBtn.setStyle("");
                }
            }
        }

        if (!isValid) {
            StringBuilder sb = new StringBuilder();
            for (String err : errors) {
                sb.append("• ").append(err).append("\n");
            }
            if (validationErrorLabel != null && validationErrorBanner != null) {
                if (validationBannerHeaderLabel != null) {
                    validationBannerHeaderLabel.setText("🛑 " + I18n.getOrDefault("scenario.validation.header_errors", "ERREURS DE CONFIGURATION DU SCÉNARIO (ONGLET 3) :"));
                    validationBannerHeaderLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");
                }
                validationErrorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                validationErrorBanner.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");
                validationErrorLabel.setText(sb.toString().trim());
                validationErrorBanner.setVisible(true);
                validationErrorBanner.setManaged(true);
            }
            if (configScroll != null) {
                configScroll.setVvalue(0.0);
            }
        } else {
            if (validationErrorBanner != null) {
                validationErrorBanner.setVisible(false);
                validationErrorBanner.setManaged(false);
            }
        }

        if (liveDiagnosticUpdater != null) {
            liveDiagnosticUpdater.run();
        }
        return isValid;
    }
}
