/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.ether.society.data.HistoricalMapGenerator;
import org.ether.society.data.ImageMapLoader;
import org.ether.society.i18n.I18n;
import org.ether.society.model.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Helper delegate managing state serialization, asynchronous procedural tensor generation,
 * raster file loading, and dynamic sub-block localization for {@link ScenarioCulturalSectionBuilder}.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
final class ScenarioCulturalStateIOHelper {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioCulturalStateIOHelper.class);

    private ScenarioCulturalStateIOHelper() {
        // Utility delegate class
    }

    /**
     * Updates the validation status label and border styling for a single cultural tensor sub-block.
     *
     * @param tensorIdx          zero-based tensor dimension index
     * @param tensorFileLabels   map of status labels per tensor index
     * @param tensorLoadBtns     map of file load buttons per tensor index
     * @param tensorProcRadios   map of procedural radio buttons per tensor index
     * @param customTensorImages map of loaded raster images per tensor index
     */
    static void updateTensorFileLabel(
            int tensorIdx,
            Map<Integer, Label> tensorFileLabels,
            Map<Integer, Button> tensorLoadBtns,
            Map<Integer, RadioButton> tensorProcRadios,
            Map<Integer, Image> customTensorImages) {
        Label fileLbl = tensorFileLabels.get(tensorIdx);
        Button loadBtn = tensorLoadBtns.get(tensorIdx);
        if (fileLbl == null) return;
        boolean isProc = tensorProcRadios.containsKey(tensorIdx) && tensorProcRadios.get(tensorIdx).isSelected();
        Image img = customTensorImages.get(tensorIdx);

        if (isProc) {
            fileLbl.setText(I18n.getOrDefault("scenario.tensor.status.procedural", "⚡ Mode procédural actif"));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            if (loadBtn != null) loadBtn.setStyle("");
            return;
        }

        if (img == null) {
            fileLbl.setText(I18n.getOrDefault("scenario.tensor.file.none", "⚠ Aucune carte chargée — Fichier requis en mode import"));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #f59e0b;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
            return;
        }

        ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(img);
        if (!val.valid()) {
            fileLbl.setText(String.format(I18n.getOrDefault("scenario.tensor.file.invalid", "⚠ Carte incompatible : %s"), val.message()));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            if (loadBtn != null) loadBtn.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;");
        } else {
            fileLbl.setText(String.format(
                    I18n.getOrDefault("scenario.tensor.file.loaded_res", "✔ Carte chargée : %s (%dx%d)"),
                    ScenarioCulturalTensorCatalog.getCulturalBaselineName(tensorIdx),
                    val.width(), val.height()));
            fileLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #38bdf8;");
            if (loadBtn != null) loadBtn.setStyle("");
        }
    }

    /**
     * Deletes a cultural tensor dimension and shifts all higher-indexed tensor configurations down by one index.
     *
     * @param indexToDelete           zero-based tensor index to remove
     * @param builder                 owning cultural section builder
     * @param context                 host panel callback context
     * @param tensorSubTitles         map of sub-block header labels
     * @param tensorSourceLabels      map of source prompt labels
     * @param tensorFileLabels        map of file status labels
     * @param tensorFormatLabels      map of format hint labels
     * @param tensorParam1ValueLabels map of parameter 1 value labels
     * @param tensorParam2ValueLabels map of parameter 2 value labels
     * @param tensorParam3ValueLabels map of parameter 3 value labels
     * @param tensorGenSingleBtns     map of single-tensor export buttons
     */
    static void deleteCulturalTensor(
            int indexToDelete,
            ScenarioCulturalSectionBuilder builder,
            ScenarioCulturalSectionBuilder.HostContext context,
            Map<Integer, Label> tensorSubTitles,
            Map<Integer, Label> tensorSourceLabels,
            Map<Integer, Label> tensorFileLabels,
            Map<Integer, Label> tensorFormatLabels,
            Map<Integer, Label> tensorParam1ValueLabels,
            Map<Integer, Label> tensorParam2ValueLabels,
            Map<Integer, Label> tensorParam3ValueLabels,
            Map<Integer, Button> tensorGenSingleBtns) {
        Spinner<Integer> dimSpinner = builder.getCultureVectorDimSpinner();
        int curDims = dimSpinner != null && dimSpinner.getValue() != null ? dimSpinner.getValue() : 9;
        if (indexToDelete < 0 || indexToDelete >= curDims) return;

        Map<Integer, String> customNames = builder.getCustomTensorNames();
        Map<Integer, Image> customImages = builder.getCustomTensorImages();
        Map<Integer, TextField> seedFields = builder.getTensorSeedFields();
        Map<Integer, Slider> p1Sliders = builder.getTensorParam1Sliders();
        Map<Integer, Slider> p2Sliders = builder.getTensorParam2Sliders();
        Map<Integer, Slider> p3Sliders = builder.getTensorParam3Sliders();
        Map<Integer, RadioButton> procRadios = builder.getTensorProcRadios();
        Map<Integer, RadioButton> importRadios = builder.getTensorImportRadios();

        for (int j = indexToDelete; j < curDims - 1; j++) {
            if (customNames.containsKey(j + 1)) customNames.put(j, customNames.get(j + 1));
            else customNames.remove(j);

            if (customImages.containsKey(j + 1)) customImages.put(j, customImages.get(j + 1));
            else customImages.remove(j);

            if (seedFields.containsKey(j + 1)) {
                String s = seedFields.get(j + 1).getText();
                if (seedFields.containsKey(j)) seedFields.get(j).setText(s);
                else seedFields.put(j, new TextField(s));
            }
            if (p1Sliders.containsKey(j + 1) && p1Sliders.containsKey(j)) {
                p1Sliders.get(j).setValue(p1Sliders.get(j + 1).getValue());
            }
            if (p2Sliders.containsKey(j + 1) && p2Sliders.containsKey(j)) {
                p2Sliders.get(j).setValue(p2Sliders.get(j + 1).getValue());
            }
            if (p3Sliders.containsKey(j + 1) && p3Sliders.containsKey(j)) {
                p3Sliders.get(j).setValue(p3Sliders.get(j + 1).getValue());
            }
            if (importRadios.containsKey(j + 1) && importRadios.get(j + 1).isSelected()) {
                if (importRadios.containsKey(j)) importRadios.get(j).setSelected(true);
            } else {
                if (procRadios.containsKey(j)) procRadios.get(j).setSelected(true);
            }
        }

        int lastIdx = curDims - 1;
        customNames.remove(lastIdx);
        customImages.remove(lastIdx);
        seedFields.remove(lastIdx);
        p1Sliders.remove(lastIdx);
        p2Sliders.remove(lastIdx);
        p3Sliders.remove(lastIdx);
        tensorParam1ValueLabels.remove(lastIdx);
        tensorParam2ValueLabels.remove(lastIdx);
        tensorParam3ValueLabels.remove(lastIdx);
        procRadios.remove(lastIdx);
        importRadios.remove(lastIdx);
        builder.getTensorSourceCombos().remove(lastIdx);
        tensorSourceLabels.remove(lastIdx);
        tensorFileLabels.remove(lastIdx);
        builder.getTensorLoadBtns().remove(lastIdx);
        tensorFormatLabels.remove(lastIdx);
        tensorGenSingleBtns.remove(lastIdx);
        tensorSubTitles.remove(lastIdx);

        int newDims = Math.max(0, curDims - 1);
        if (dimSpinner != null && dimSpinner.getValueFactory() != null) {
            dimSpinner.getValueFactory().setValue(newDims);
        } else {
            builder.rebuildCulturalTensorSubBlocks(newDims);
            context.updatePreviewModesCombo();
            context.notifyParamChange();
            context.drawPreview();
        }
    }

    /**
     * Refreshes localized labels and tooltips across all dynamic cultural tensor sub-blocks.
     *
     * @param builder             owning cultural section builder
     * @param layersContainer     dynamic container VBox
     * @param btnRegen            global regenerate button
     * @param tensorSubTitles     map of sub-block title labels
     * @param tensorSourceLabels  map of reference source labels
     * @param tensorFormatLabels  map of format hint labels
     * @param tensorGenSingleBtns map of single-tensor export buttons
     */
    static void updateCulturalTensorSubBlockTexts(
            ScenarioCulturalSectionBuilder builder,
            VBox layersContainer,
            Button btnRegen,
            Map<Integer, Label> tensorSubTitles,
            Map<Integer, Label> tensorSourceLabels,
            Map<Integer, Label> tensorFormatLabels,
            Map<Integer, Button> tensorGenSingleBtns) {
        if (btnRegen != null) {
            btnRegen.setText(I18n.getOrDefault("scenario.btn.regen_tensors", "⚡ Régénérer les Tenseurs"));
            btnRegen.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors",
                    "Bascule tous les tenseurs en mode procédural et régénère les cartes selon les paramètres et la graine stochastique.")));
        }
        int dims = builder.getCultureVectorDimensions();
        if (layersContainer == null || layersContainer.getChildren().size() != dims) {
            builder.rebuildCulturalTensorSubBlocks(dims);
            return;
        }
        for (int i = 0; i < dims; i++) {
            Label subTitle = tensorSubTitles.get(i);
            if (subTitle != null) {
                subTitle.setText(builder.getCulturalTensorTitle(i));
                Tooltip.install(subTitle, new Tooltip(builder.getCulturalTensorTooltip(i)));
            }
            RadioButton radioProc = builder.getTensorProcRadios().get(i);
            if (radioProc != null) {
                radioProc.setText(I18n.getOrDefault("scenario.mode.procedural_sde", "⚡ Mode Procédural (Paramètres adaptatifs & Graine)"));
                radioProc.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_proc",
                        "Génère procéduralement ce tenseur culturel via des équations stochastiques et les paramètres ci-dessous.")));
            }
            RadioButton radioImport = builder.getTensorImportRadios().get(i);
            if (radioImport != null) {
                radioImport.setText(I18n.getOrDefault("scenario.mode.spatial_import", "📁 Importation Carte Spatiale (PNG / GeoJSON)"));
                radioImport.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_import",
                        "Importe une image matricielle ou une couche SIG externe pour modéliser ce tenseur culturel.")));
            }
            Label srcLbl = tensorSourceLabels.get(i);
            if (srcLbl != null) {
                srcLbl.setText(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            }
            Button loadBtn = builder.getTensorLoadBtns().get(i);
            if (loadBtn != null) {
                loadBtn.setText(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
            }
            Button genBtn = tensorGenSingleBtns.get(i);
            if (genBtn != null) {
                genBtn.setText(I18n.getOrDefault("scenario.tensor.btn.export_single", "💾 Export"));
            }
            Label fmtLbl = tensorFormatLabels.get(i);
            if (fmtLbl != null) {
                fmtLbl.setText(builder.getCulturalFormatHint(i));
            }
            ComboBox<String> sc = builder.getTensorSourceCombos().get(i);
            if (sc != null && sc.getCellFactory() != null) {
                sc.setButtonCell(sc.getCellFactory().call(null));
            }
        }
    }

    /**
     * Opens a file chooser dialog to load and validate an external equirectangular cultural layer image.
     *
     * @param window    parent owner window
     * @param layerName human-readable layer name for dialog title
     * @param onLoaded  callback invoked when a valid map image is loaded
     */
    static void loadCustomCultureLayer(Window window, String layerName, Consumer<Image> onLoaded) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.getOrDefault("scenario.dialog.import_layer_title", "Import layer ") + layerName + " (PNG/GeoJSON/JPG)");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images & Cartes", "*.png", "*.jpg", "*.jpeg", "*.geojson", "*.json")
        );
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            try {
                Image img = new Image(new FileInputStream(file));
                ImageMapLoader.ImageValidationResult val = ImageMapLoader.validateMapImage(img);
                if (!val.valid()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(I18n.getOrDefault("scenario.dialog.invalid_map_title", "Carte Incompatible ou Invalide"));
                    alert.setHeaderText(I18n.getOrDefault("scenario.dialog.invalid_map_header", "Fichier de carte non supporté ou illisible"));
                    alert.setContentText(file.getName() + " :\n" + val.message() + "\n\n"
                            + I18n.getOrDefault("scenario.dialog.invalid_map_hint", "Veuillez fournir une image raster valide (PNG/JPG) en projection équirectangulaire (2:1)."));
                    alert.showAndWait();
                    return;
                }
                onLoaded.accept(img);
            } catch (Exception ex) {
                logger.error("Failed to load culture layer {}", layerName, ex);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur de chargement: " + ex.getMessage());
                alert.showAndWait();
            }
        }
    }

    /**
     * Switches all cultural tensors to procedural mode and asynchronously regenerates scenario tensor maps.
     *
     * @param builder          owning cultural section builder
     * @param context          host panel callback context
     * @param btnRegen         regenerate button disabled during async generation
     * @param tensorFileLabels map of status labels per tensor index
     */
    static void generateProceduralCulturalTensors(
            ScenarioCulturalSectionBuilder builder,
            ScenarioCulturalSectionBuilder.HostContext context,
            Button btnRegen,
            Map<Integer, Label> tensorFileLabels) {
        for (RadioButton rb : builder.getTensorProcRadios().values()) {
            if (rb != null) rb.setSelected(true);
        }
        builder.getCustomTensorImages().clear();

        context.selectPreviewMode(1);
        if (tensorFileLabels != null) {
            for (Integer idx : tensorFileLabels.keySet()) {
                builder.updateTensorFileLabel(idx);
            }
        }
        context.notifyParamChange();

        Scenario s = context.getScenario();
        if (s == null) return;
        if (btnRegen != null) btnRegen.setDisable(true);
        CompletableFuture.runAsync(() -> {
            HistoricalMapGenerator.forceGenerateCulturalTensorsOnly(s);
            HistoricalMapGenerator.generateProceduralMapsForScenario(s);
        }).thenRun(() -> javafx.application.Platform.runLater(() -> {
            if (btnRegen != null) btnRegen.setDisable(false);
            int dims = builder.getCultureVectorDimensions();
            for (int i = 0; i < dims; i++) {
                String b64 = s.getCustomTensorMapBase64(i);
                if (b64 != null && !b64.isBlank()) {
                    Image img = ImageMapLoader.base64PngToImage(b64);
                    if (img != null) {
                        builder.getCustomTensorImages().put(i, img);
                    }
                }
                builder.updateTensorFileLabel(i);
            }
            context.notifyParamChange();
            context.drawPreview();
        }));
    }

    /**
     * Populates UI controls and tensor sub-blocks from a {@link Scenario} model instance.
     *
     * @param s       source scenario model
     * @param builder target cultural section builder
     */
    static void applyScenario(Scenario s, ScenarioCulturalSectionBuilder builder) {
        if (s == null) return;
        Spinner<Integer> dimSp = builder.getCultureVectorDimSpinner();
        Spinner<Double> diffSp = builder.getCulturalDiffusionRateSpinner();
        Spinner<Double> mutSp = builder.getCulturalMutationRateSpinner();

        if (s.getCultureVectorDimensions() > 0 && dimSp != null && dimSp.getValueFactory() != null) {
            dimSp.getValueFactory().setValue(s.getCultureVectorDimensions());
        }
        if (s.getCulturalDiffusionRate() > 0 && diffSp != null && diffSp.getValueFactory() != null) {
            diffSp.getValueFactory().setValue(s.getCulturalDiffusionRate());
        }
        if (s.getCulturalMutationRate() > 0 && mutSp != null && mutSp.getValueFactory() != null) {
            mutSp.getValueFactory().setValue(s.getCulturalMutationRate());
        }
        int dims = builder.getCultureVectorDimensions();
        builder.getCustomTensorImages().clear();
        builder.getCustomTensorNames().clear();
        if (s.getCustomTensorNames() != null) {
            builder.getCustomTensorNames().putAll(s.getCustomTensorNames());
        }

        for (int i = 0; i < dims; i++) {
            String b64 = s.getCustomTensorMapBase64(i);
            if (b64 != null && !b64.isBlank()) {
                Image img = ImageMapLoader.base64PngToImage(b64);
                if (img != null) {
                    builder.getCustomTensorImages().put(i, img);
                }
            }
        }

        builder.rebuildCulturalTensorSubBlocks(dims);

        if (s.getTensorProceduralModes() != null) {
            for (int i = 0; i < Math.min(dims, s.getTensorProceduralModes().size()); i++) {
                boolean isProc = s.getTensorProceduralModes().get(i);
                if (isProc && builder.getTensorProcRadios().containsKey(i)) {
                    builder.getTensorProcRadios().get(i).setSelected(true);
                } else if (!isProc && builder.getTensorImportRadios().containsKey(i)) {
                    builder.getTensorImportRadios().get(i).setSelected(true);
                }
            }
        }

        if (s.getTensorSeeds() != null) {
            for (var entry : s.getTensorSeeds().entrySet()) {
                if (builder.getTensorSeedFields().containsKey(entry.getKey())) {
                    builder.getTensorSeedFields().get(entry.getKey()).setText(String.valueOf(entry.getValue()));
                }
            }
        }

        if (s.getTensorProceduralParameters() != null) {
            for (var entry : s.getTensorProceduralParameters().entrySet()) {
                int tIdx = entry.getKey();
                var pMap = entry.getValue();
                if (pMap == null) continue;
                var d1 = builder.getTensorParam1Descriptor(tIdx);
                var d2 = builder.getTensorParam2Descriptor(tIdx);
                var d3 = builder.getTensorParam3Descriptor(tIdx);
                if (pMap.containsKey(d1.labelKey) && builder.getTensorParam1Sliders().containsKey(tIdx)) {
                    builder.getTensorParam1Sliders().get(tIdx).setValue(pMap.get(d1.labelKey));
                }
                if (pMap.containsKey(d2.labelKey) && builder.getTensorParam2Sliders().containsKey(tIdx)) {
                    builder.getTensorParam2Sliders().get(tIdx).setValue(pMap.get(d2.labelKey));
                }
                if (pMap.containsKey(d3.labelKey) && builder.getTensorParam3Sliders().containsKey(tIdx)) {
                    builder.getTensorParam3Sliders().get(tIdx).setValue(pMap.get(d3.labelKey));
                }
            }
        }

        for (int i = 0; i < dims; i++) {
            builder.updateTensorFileLabel(i);
        }
    }

    /**
     * Exports all cultural vector dimensions, Langevin-SDE parameters, seeds, and custom rasters into {@link Scenario}.
     *
     * @param s       target scenario model
     * @param builder source cultural section builder
     */
    static void exportToScenario(Scenario s, ScenarioCulturalSectionBuilder builder) {
        if (s == null) return;
        int dims = builder.getCultureVectorDimensions();
        s.setCultureVectorDimensions(dims);
        s.setCustomTensorNames(new HashMap<>(builder.getCustomTensorNames()));
        if (builder.getCulturalDiffusionRateSpinner() != null) {
            s.setCulturalDiffusionRate(builder.getCulturalDiffusionRateSpinner().getValue());
        }
        if (builder.getCulturalMutationRateSpinner() != null) {
            s.setCulturalMutationRate(builder.getCulturalMutationRateSpinner().getValue());
        }
        Map<Integer, Long> tSeeds = new HashMap<>();
        Map<Integer, Map<String, Double>> tProcParams = new HashMap<>();

        for (int i = 0; i < dims; i++) {
            Image customImg = builder.getCustomTensorImages().get(i);
            if (customImg != null) {
                s.setCustomTensorMapBase64(i, ImageMapLoader.imageToBase64Png(customImg));
            }
            boolean isProc = builder.getTensorProcRadios().containsKey(i) && builder.getTensorProcRadios().get(i).isSelected();
            while (s.getTensorProceduralModes().size() <= i) {
                s.getTensorProceduralModes().add(true);
            }
            s.getTensorProceduralModes().set(i, isProc);

            TextField seedTf = builder.getTensorSeedFields().get(i);
            tSeeds.put(i, parseTensorSeedOrDefault(seedTf != null ? seedTf.getText() : null, builder.getDefaultTensorSeed(i)));

            Map<String, Double> pMap = new HashMap<>();
            var d1 = builder.getTensorParam1Descriptor(i);
            var d2 = builder.getTensorParam2Descriptor(i);
            var d3 = builder.getTensorParam3Descriptor(i);
            if (builder.getTensorParam1Sliders().get(i) != null) {
                pMap.put(d1.labelKey, builder.getTensorParam1Sliders().get(i).getValue());
            }
            if (builder.getTensorParam2Sliders().get(i) != null) {
                pMap.put(d2.labelKey, builder.getTensorParam2Sliders().get(i).getValue());
            }
            if (builder.getTensorParam3Sliders().get(i) != null) {
                pMap.put(d3.labelKey, builder.getTensorParam3Sliders().get(i).getValue());
            }
            tProcParams.put(i, pMap);
        }
        s.setTensorSeeds(tSeeds);
        s.setTensorProceduralParameters(tProcParams);
    }

    private static long parseTensorSeedOrDefault(String rawText, String fallbackText) {
        if (rawText != null) {
            try {
                return Long.parseLong(rawText.trim());
            } catch (NumberFormatException ignored) {
                // Fall back below
            }
        }
        try {
            return Long.parseLong(fallbackText);
        } catch (Exception e) {
            return 12345L;
        }
    }
}
