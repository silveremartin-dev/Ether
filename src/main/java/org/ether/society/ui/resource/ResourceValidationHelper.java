package org.ether.society.ui.resource;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.i18n.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * <h2>Resource Validation Helper</h2>
 * <p>
 * Validates domain-specific imported cartographic rasters (biomes, hydrology, geology, and climate)
 * against resolution, aspect ratio, and channel requirements, and manages the visual warning banner.
 * </p>
 *
 * @author Ether Architectural Refactoring Engine
 * @since 1.0.0
 */
public final class ResourceValidationHelper {

    private ResourceValidationHelper() {
        // Utility class
    }

    /**
     * Aggregates validation errors across all domain section builders.
     *
     * @param biomeBuilder biome section builder
     * @param hydroBuilder hydro section builder
     * @param climateBuilder climate section builder
     * @param geologyBuilder geology section builder
     * @param isGeologyImportSelected whether legacy/domain geology import is active
     * @param customResourceImage legacy multi-channel geology image
     * @return list of localized error messages
     */
    public static List<String> getValidationErrors(
            ResourceBiomeSectionBuilder biomeBuilder,
            ResourceHydroSectionBuilder hydroBuilder,
            ResourceClimateSectionBuilder climateBuilder,
            ResourceGeologySectionBuilder geologyBuilder,
            boolean isGeologyImportSelected,
            Image customResourceImage) {

        List<String> errors = new ArrayList<>();

        if (biomeBuilder != null && biomeBuilder.isRadioImportBiomeSelected()) {
            if (biomeBuilder.getCustomBiomeImage() == null) {
                errors.add(I18n.getOrDefault("resource.validation.missing_biome_map", "Missing biome distribution map in import mode (Tab 2)."));
            } else {
                ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(biomeBuilder.getCustomBiomeImage());
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("resource.validation.invalid_biome_map", "Biome map incompatible (Tab 2): ") + val.message());
                }
            }
        }

        if (hydroBuilder != null && hydroBuilder.isRadioImportHydroSelected()) {
            if (hydroBuilder.getCustomHydroImage() == null) {
                errors.add(I18n.getOrDefault("resource.validation.missing_hydro_map", "Missing hydrographic map in import mode (Tab 2)."));
            } else {
                ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(hydroBuilder.getCustomHydroImage());
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("resource.validation.invalid_hydro_map", "Hydrographic map incompatible (Tab 2): ") + val.message());
                }
            }
        }

        if (isGeologyImportSelected) {
            boolean hasGeologyLayers = geologyBuilder != null && geologyBuilder.getCustomGeologyLayerImages() != null && !geologyBuilder.getCustomGeologyLayerImages().isEmpty();
            if (customResourceImage == null && !hasGeologyLayers) {
                errors.add(I18n.getOrDefault("resource.validation.missing_geology_map", "Missing geological & deposit map in import mode (Tab 2)."));
            } else if (customResourceImage != null) {
                ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(customResourceImage);
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("resource.validation.invalid_geology_map", "Geological map incompatible (Tab 2): ") + val.message());
                }
            }
        }

        if (climateBuilder != null && climateBuilder.isRadioImportClimateSelected()) {
            if (climateBuilder.getCustomClimateImage() == null) {
                errors.add(I18n.getOrDefault("resource.validation.missing_climate_map", "Missing macro-climate map in import mode (Tab 2)."));
            } else {
                ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(climateBuilder.getCustomClimateImage());
                if (!val.valid()) {
                    errors.add(I18n.getOrDefault("resource.validation.invalid_climate_map", "Macro-climate map incompatible (Tab 2): ") + val.message());
                }
            }
        }

        return errors;
    }

    /**
     * Validates resource configuration, highlights invalid controls, and displays the warning banner.
     *
     * @param errors list of validation errors
     * @param showDialog whether to update the warning banner
     * @param biomeBuilder biome section builder
     * @param hydroBuilder hydro section builder
     * @param climateBuilder climate section builder
     * @param geologyBuilder geology section builder
     * @param isGeologyImportSelected whether geology import is selected
     * @param customResourceImage legacy multi-channel geology image
     * @param loadResourceBtn load button for legacy resource image
     * @param banner warning banner container
     * @param warningLabel warning text label
     * @return true if valid (no errors), false otherwise
     */
    public static boolean applyValidationVisuals(
            List<String> errors,
            boolean showDialog,
            ResourceBiomeSectionBuilder biomeBuilder,
            ResourceHydroSectionBuilder hydroBuilder,
            ResourceClimateSectionBuilder climateBuilder,
            ResourceGeologySectionBuilder geologyBuilder,
            boolean isGeologyImportSelected,
            Image customResourceImage,
            Button loadResourceBtn,
            VBox banner,
            Label warningLabel) {

        boolean isValid = errors.isEmpty();

        if (biomeBuilder != null) {
            boolean isBiomeImport = biomeBuilder.isRadioImportBiomeSelected();
            boolean biomeBad = isBiomeImport && (biomeBuilder.getCustomBiomeImage() == null || !ImageMapLoader.validateMapImage(biomeBuilder.getCustomBiomeImage()).valid());
            if (biomeBuilder.getLoadBiomeBtn() != null) {
                biomeBuilder.getLoadBiomeBtn().setStyle(biomeBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");
            }
        }

        if (hydroBuilder != null) {
            boolean hydroBad = hydroBuilder.isRadioImportHydroSelected() && (hydroBuilder.getCustomHydroImage() == null || !ImageMapLoader.validateMapImage(hydroBuilder.getCustomHydroImage()).valid());
            if (hydroBuilder.getLoadHydroBtn() != null) {
                hydroBuilder.getLoadHydroBtn().setStyle(hydroBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");
            }
        }

        if (loadResourceBtn != null) {
            boolean hasLayers = geologyBuilder != null && geologyBuilder.getCustomGeologyLayerImages() != null && !geologyBuilder.getCustomGeologyLayerImages().isEmpty();
            boolean geoBad = isGeologyImportSelected && (customResourceImage == null || !ImageMapLoader.validateMapImage(customResourceImage).valid()) && !hasLayers;
            loadResourceBtn.setStyle(geoBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");
        }

        if (climateBuilder != null) {
            boolean climateBad = climateBuilder.isRadioImportClimateSelected() && (climateBuilder.getCustomClimateImage() == null || !ImageMapLoader.validateMapImage(climateBuilder.getCustomClimateImage()).valid());
            if (climateBuilder.getLoadClimateBtn() != null) {
                climateBuilder.getLoadClimateBtn().setStyle(climateBad ? "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;" : "");
            }
        }

        if (!isValid) {
            if (showDialog && warningLabel != null && banner != null) {
                StringBuilder errorMsg = new StringBuilder();
                for (String err : errors) {
                    errorMsg.append("• ").append(err).append("\n");
                }
                warningLabel.setText(errorMsg.toString().trim());
                banner.setVisible(true);
                banner.setManaged(true);
            }
        } else {
            if (banner != null) {
                banner.setVisible(false);
                banner.setManaged(false);
            }
        }

        return isValid;
    }
}
