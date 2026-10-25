/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.WindowUtils;
import org.ether.society.ui.util.UIControlsHelper;

import java.util.Map;
import java.util.Random;

/**
 * Builds and coordinates individual cartographic sub-blocks for each cultural tensor dimension.
 * Manages procedural parameter sliders, seed fields, toggle groups, and map import controls.
 */
public class ScenarioCulturalSubBlockBuilder {

    private final ScenarioCulturalSectionBuilder.HostContext context;
    private final ScenarioCulturalSectionBuilder parentBuilder;

    // References to shared state maps
    private final Map<Integer, Image> customTensorImages;
    private final Map<Integer, String> customTensorNames;
    private final Map<Integer, Label> tensorSubTitles;
    private final Map<Integer, RadioButton> tensorProcRadios;
    private final Map<Integer, RadioButton> tensorImportRadios;
    private final Map<Integer, ComboBox<String>> tensorSourceCombos;
    private final Map<Integer, Label> tensorSourceLabels;
    private final Map<Integer, Label> tensorFileLabels;
    private final Map<Integer, Button> tensorLoadBtns;
    private final Map<Integer, Label> tensorFormatLabels;
    private final Map<Integer, TextField> tensorSeedFields;
    private final Map<Integer, Slider> tensorParam1Sliders;
    private final Map<Integer, Slider> tensorParam2Sliders;
    private final Map<Integer, Slider> tensorParam3Sliders;
    private final Map<Integer, Label> tensorParam1ValueLabels;
    private final Map<Integer, Label> tensorParam2ValueLabels;
    private final Map<Integer, Label> tensorParam3ValueLabels;
    private final Map<Integer, Button> tensorGenSingleBtns;

    public ScenarioCulturalSubBlockBuilder(ScenarioCulturalSectionBuilder parentBuilder,
                                          ScenarioCulturalSectionBuilder.HostContext context,
                                          Map<Integer, Image> customTensorImages,
                                          Map<Integer, String> customTensorNames,
                                          Map<Integer, Label> tensorSubTitles,
                                          Map<Integer, RadioButton> tensorProcRadios,
                                          Map<Integer, RadioButton> tensorImportRadios,
                                          Map<Integer, ComboBox<String>> tensorSourceCombos,
                                          Map<Integer, Label> tensorSourceLabels,
                                          Map<Integer, Label> tensorFileLabels,
                                          Map<Integer, Button> tensorLoadBtns,
                                          Map<Integer, Label> tensorFormatLabels,
                                          Map<Integer, TextField> tensorSeedFields,
                                          Map<Integer, Slider> tensorParam1Sliders,
                                          Map<Integer, Slider> tensorParam2Sliders,
                                          Map<Integer, Slider> tensorParam3Sliders,
                                          Map<Integer, Label> tensorParam1ValueLabels,
                                          Map<Integer, Label> tensorParam2ValueLabels,
                                          Map<Integer, Label> tensorParam3ValueLabels,
                                          Map<Integer, Button> tensorGenSingleBtns) {
        this.parentBuilder = parentBuilder;
        this.context = context;
        this.customTensorImages = customTensorImages;
        this.customTensorNames = customTensorNames;
        this.tensorSubTitles = tensorSubTitles;
        this.tensorProcRadios = tensorProcRadios;
        this.tensorImportRadios = tensorImportRadios;
        this.tensorSourceCombos = tensorSourceCombos;
        this.tensorSourceLabels = tensorSourceLabels;
        this.tensorFileLabels = tensorFileLabels;
        this.tensorLoadBtns = tensorLoadBtns;
        this.tensorFormatLabels = tensorFormatLabels;
        this.tensorSeedFields = tensorSeedFields;
        this.tensorParam1Sliders = tensorParam1Sliders;
        this.tensorParam2Sliders = tensorParam2Sliders;
        this.tensorParam3Sliders = tensorParam3Sliders;
        this.tensorParam1ValueLabels = tensorParam1ValueLabels;
        this.tensorParam2ValueLabels = tensorParam2ValueLabels;
        this.tensorParam3ValueLabels = tensorParam3ValueLabels;
        this.tensorGenSingleBtns = tensorGenSingleBtns;
    }

    public void rebuildSubBlocks(VBox container, int dimCount, Button btnRegen) {
        if (btnRegen != null) {
            btnRegen.setText(I18n.getOrDefault("scenario.btn.regen_tensors", "⚡ Régénérer les Tenseurs"));
            btnRegen.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.regen_tensors",
                    "Bascule tous les tenseurs en mode procédural et régénère les cartes selon les paramètres et la graine stochastique.")));
        }
        if (container == null) return;
        container.getChildren().clear();

        for (int i = 0; i < dimCount; i++) {
            final int tensorIdx = i;

            VBox subBlock = new VBox(6);
            subBlock.setStyle("-fx-padding: 10; -fx-background-color: rgba(167,139,250,0.04); -fx-background-radius: 6; -fx-border-color: rgba(167,139,250,0.15); -fx-border-radius: 6;");

            HBox titleRow = new HBox(8);
            titleRow.setAlignment(Pos.CENTER_LEFT);

            Label subTitle = new Label(parentBuilder.getCulturalTensorTitle(i));
            subTitle.getStyleClass().add("control-label");
            subTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            subTitle.setWrapText(true);
            HBox.setHgrow(subTitle, Priority.ALWAYS);
            Tooltip.install(subTitle, new Tooltip(parentBuilder.getCulturalTensorTooltip(i)));
            tensorSubTitles.put(tensorIdx, subTitle);

            Button renameBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.rename", "✏ Renommer"));
            renameBtn.getStyleClass().add("button-secondary");
            renameBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            renameBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.rename_tooltip",
                    "Cliquez pour personnaliser le nom de ce calque de tenseur culturel.")));
            renameBtn.setOnAction(e -> {
                String cur = customTensorNames.getOrDefault(tensorIdx, "");
                TextInputDialog dlg = new TextInputDialog(cur);
                dlg.setTitle(I18n.getOrDefault("scenario.tensor.rename.title", "Personnaliser le Nom du Tenseur"));
                dlg.setHeaderText(I18n.getOrDefault("scenario.tensor.rename.prompt", "Nom personnalisé du Tenseur N°{0} :", (tensorIdx + 1)));
                WindowUtils.applyWindowIcon(dlg);
                dlg.showAndWait().ifPresent(strVal -> {
                    if (strVal.isBlank()) {
                        customTensorNames.remove(tensorIdx);
                    } else {
                        customTensorNames.put(tensorIdx, strVal.trim());
                    }
                    subTitle.setText(parentBuilder.getCulturalTensorTitle(tensorIdx));
                    context.updatePreviewModesCombo();
                    context.notifyParamChange();
                });
            });

            Button deleteBtn = new Button(I18n.getOrDefault("scenario.tensor.btn.delete", "✕"));
            deleteBtn.getStyleClass().add("button-secondary");
            deleteBtn.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-padding: 2 6; -fx-font-weight: bold; -fx-cursor: hand;");
            deleteBtn.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.delete_tooltip",
                    "Supprimer ce tenseur culturel (décrémente le nombre total de tenseurs).")));
            deleteBtn.setOnAction(e -> parentBuilder.deleteCulturalTensor(tensorIdx));

            titleRow.getChildren().addAll(subTitle, renameBtn, deleteBtn);
            subBlock.getChildren().add(titleRow);

            ToggleGroup tg = new ToggleGroup();
            RadioButton radioProc = new RadioButton(I18n.getOrDefault("scenario.mode.procedural_sde", "⚡ Mode Procédural (Paramètres adaptatifs & Graine)"));
            RadioButton radioImport = new RadioButton(I18n.getOrDefault("scenario.mode.spatial_import", "📁 Importation Carte Spatiale (PNG / GeoJSON)"));
            radioProc.setToggleGroup(tg);
            radioImport.setToggleGroup(tg);
            radioProc.getStyleClass().add("radio-proc");
            radioImport.getStyleClass().add("radio-import");
            radioProc.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_proc",
                    "Génère procéduralement ce tenseur culturel via des équations stochastiques et les paramètres ci-dessous.")));
            radioImport.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.cultural_radio_import",
                    "Importe une image matricielle ou une couche SIG externe pour modéliser ce tenseur culturel.")));

            boolean hasImage = customTensorImages.containsKey(tensorIdx) && customTensorImages.get(tensorIdx) != null;
            if (hasImage) {
                radioImport.setSelected(true);
            } else {
                radioProc.setSelected(true);
            }
            tensorProcRadios.put(tensorIdx, radioProc);
            tensorImportRadios.put(tensorIdx, radioImport);

            // --- 1. Procedural Sub-Box ---
            TextField seedField = tensorSeedFields.computeIfAbsent(tensorIdx, k -> new TextField(parentBuilder.getDefaultTensorSeed(tensorIdx)));
            seedField.setPrefWidth(80);
            seedField.setStyle("-fx-font-size: 10px;");
            seedField.textProperty().addListener((obs, oldV, newV) -> {
                context.notifyParamChange();
                context.drawPreview();
            });

            Button randSeedBtn = new Button("🎲");
            randSeedBtn.getStyleClass().add("button-secondary");
            randSeedBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            randSeedBtn.setOnAction(e -> {
                seedField.setText(String.valueOf(new Random().nextLong(1000000)));
                context.notifyParamChange();
                context.drawPreview();
            });

            Button btnExportSingle = new Button(I18n.getOrDefault("scenario.tensor.btn.export_single", "💾 Export"));
            btnExportSingle.getStyleClass().add("button-secondary");
            btnExportSingle.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
            btnExportSingle.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tensor.btn.export_single_tooltip",
                    "Exporte l'image haute définition (2048x1024) de ce tenseur culturel.")));
            btnExportSingle.setOnAction(e -> parentBuilder.exportCulturalTensor(tensorIdx));
            tensorGenSingleBtns.put(tensorIdx, btnExportSingle);

            HBox seedRow = new HBox(4, new Label(I18n.getOrDefault("scenario.label.tensor_seed", "Graine :")), new Label("🎲"), seedField, randSeedBtn, btnExportSingle);
            seedRow.setAlignment(Pos.CENTER_LEFT);

            Slider p1Slider = tensorParam1Sliders.computeIfAbsent(tensorIdx, k -> new Slider());
            Label p1Val = tensorParam1ValueLabels.computeIfAbsent(tensorIdx, k -> new Label());
            HBox p1Row = createTensorParamRow(parentBuilder.getTensorParam1Descriptor(tensorIdx), p1Slider, p1Val, () -> { context.notifyParamChange(); context.drawPreview(); });

            Slider p2Slider = tensorParam2Sliders.computeIfAbsent(tensorIdx, k -> new Slider());
            Label p2Val = tensorParam2ValueLabels.computeIfAbsent(tensorIdx, k -> new Label());
            HBox p2Row = createTensorParamRow(parentBuilder.getTensorParam2Descriptor(tensorIdx), p2Slider, p2Val, () -> { context.notifyParamChange(); context.drawPreview(); });

            Slider p3Slider = tensorParam3Sliders.computeIfAbsent(tensorIdx, k -> new Slider());
            Label p3Val = tensorParam3ValueLabels.computeIfAbsent(tensorIdx, k -> new Label());
            HBox p3Row = createTensorParamRow(parentBuilder.getTensorParam3Descriptor(tensorIdx), p3Slider, p3Val, () -> { context.notifyParamChange(); context.drawPreview(); });

            Label procStatusLbl = new Label(I18n.getOrDefault("scenario.status.proc_gen_tensor_prefix", "✔ Modélisation procédurale dynamique active (Tenseur ") + (tensorIdx + 1) + ")");
            procStatusLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #10b981;");

            VBox procBox = new VBox(5, seedRow, p1Row, p2Row, p3Row, procStatusLbl);
            procBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(56,189,248,0.25); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            procBox.setVisible(!hasImage);
            procBox.setManaged(!hasImage);

            // --- 2. Import Configuration Sub-Box ---
            Label sourceLbl = new Label(I18n.getOrDefault("resource.label.reference_source", "Reference Source:"));
            tensorSourceLabels.put(tensorIdx, sourceLbl);

            ComboBox<String> sourceCombo = parentBuilder.buildCulturalSourceCombo(tensorIdx);
            tensorSourceCombos.put(tensorIdx, sourceCombo);

            Button btnLoad = new Button(I18n.getOrDefault("resource.btn.load_map", "Load Map"));
            btnLoad.getStyleClass().add("button-secondary");
            btnLoad.setStyle("-fx-font-size: 11px;");
            btnLoad.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.load_cultural_tensor_map",
                    "Ouvre un sélecteur de fichier pour importer une carte raster externe (PNG/GeoTIFF) pour ce tenseur.")));
            tensorLoadBtns.put(tensorIdx, btnLoad);

            Button btnClear = new Button("🗑");
            btnClear.getStyleClass().add("button-secondary");
            btnClear.setStyle("-fx-font-size: 11px;");
            btnClear.setTooltip(new Tooltip(I18n.getOrDefault("scenario.tooltip.clear_cultural_tensor_map",
                    "Efface l'image importée et réinitialise le tenseur au mode procédural par défaut.")));

            HBox btnBox = new HBox(6, btnLoad, btnClear);
            btnBox.setAlignment(Pos.CENTER_LEFT);

            Label fileLbl = new Label();
            fileLbl.getStyleClass().add("value-label");
            fileLbl.setStyle("-fx-font-size: 10px;");
            tensorFileLabels.put(tensorIdx, fileLbl);
            parentBuilder.updateTensorFileLabel(tensorIdx);

            Label formatHintLbl = new Label(parentBuilder.getCulturalFormatHint(tensorIdx));
            formatHintLbl.getStyleClass().add("card-description-muted");
            formatHintLbl.setStyle("-fx-font-size: 9px; -fx-font-style: italic;");
            formatHintLbl.setWrapText(true);
            tensorFormatLabels.put(tensorIdx, formatHintLbl);

            VBox importBox = new VBox(6, sourceLbl, sourceCombo, btnBox, fileLbl, formatHintLbl);
            importBox.setStyle("-fx-padding: 6 0 0 12; -fx-border-color: rgba(167,139,250,0.25); -fx-border-width: 0 0 0 3; -fx-border-radius: 4;");
            importBox.setVisible(hasImage);
            importBox.setManaged(hasImage);

            tg.selectedToggleProperty().addListener((obs, oldV, sel) -> {
                boolean isProc = sel == radioProc;
                procBox.setVisible(isProc);
                procBox.setManaged(isProc);
                importBox.setVisible(!isProc);
                importBox.setManaged(!isProc);
                parentBuilder.updateTensorFileLabel(tensorIdx);
                context.selectPreviewMode(tensorIdx + 1);
                if (!context.isUpdatingFromPreset()) {
                    context.notifyParamChange();
                    context.drawPreview();
                }
            });

            btnLoad.setOnAction(e -> parentBuilder.loadCustomCultureLayerForTensor(tensorIdx, img -> {
                customTensorImages.put(tensorIdx, img);
                parentBuilder.updateTensorFileLabel(tensorIdx);
                context.selectPreviewMode(tensorIdx + 1);
                radioImport.setSelected(true);
                context.notifyParamChange();
                context.drawPreview();
            }));

            btnClear.setOnAction(e -> {
                customTensorImages.remove(tensorIdx);
                parentBuilder.updateTensorFileLabel(tensorIdx);
                context.selectPreviewMode(tensorIdx + 1);
                radioProc.setSelected(true);
                context.notifyParamChange();
                context.drawPreview();
            });

            subBlock.getChildren().addAll(subTitle, radioProc, procBox, radioImport, importBox);
            container.getChildren().add(subBlock);
        }
    }

    private HBox createTensorParamRow(ScenarioCulturalTensorCatalog.TensorParamDescriptor desc, Slider slider, Label valueLabel, Runnable onChange) {
        Label label = new Label(I18n.getOrDefault(desc.labelKey, desc.defaultLabel));
        label.getStyleClass().add("control-label");
        label.setStyle("-fx-font-size: 10px;");
        label.setMinWidth(140);
        label.setMaxWidth(140);
        label.setWrapText(true);

        slider.setMin(desc.min);
        slider.setMax(desc.max);
        slider.setValue(desc.defVal);
        slider.setBlockIncrement(desc.step);
        slider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(slider, Priority.ALWAYS);

        valueLabel.setText(String.format(java.util.Locale.ROOT, desc.format, desc.defVal));
        valueLabel.getStyleClass().add("value-label");
        valueLabel.setStyle("-fx-font-size: 10px;");
        valueLabel.setMinWidth(65);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            valueLabel.setText(String.format(java.util.Locale.ROOT, desc.format, newVal.doubleValue()));
            if (!context.isUpdatingFromPreset()) {
                onChange.run();
            }
        });

        String tip = I18n.getOrDefault(desc.tooltipKey, desc.defaultTooltip);
        if (tip != null && !tip.isEmpty()) {
            Tooltip.install(label, new Tooltip(tip));
            Tooltip.install(slider, new Tooltip(tip));
        }

        UIControlsHelper.attachDefaultValueHandling(slider, desc.defVal, () -> slider.setValue(desc.defVal));

        HBox row = new HBox(6, label, slider, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
