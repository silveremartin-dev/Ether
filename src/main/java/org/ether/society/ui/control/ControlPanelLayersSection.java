/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.control;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.ether.society.core.ISimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.DisplayMode;
import org.ether.society.ui.H3MapCanvas;
import org.ether.society.ui.ColorLegend;
import org.ether.society.ui.ViewMode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Layer stack and scientific presets card for {@link org.ether.society.ui.ControlPanel}.
 *
 * <p>Extracted from {@code ControlPanel} to satisfy the Single Responsibility Principle (SRP).
 * Manages thematic cartographic data layer multi-selection, 1-click cliodynamic scientific presets,
 * elevation contour overlays, transport flux vectors, and 2.5D altitude floating projection.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ControlPanelLayersSection {

    private final VBox card;
    private final Label layersTitle;
    private final Label presetsTitle;
    private final Button presetSynth;
    private final Button presetEcon;
    private final Button presetClim;
    private final Button presetCliodyn;

    private final Label layerComboLabel;
    private final MenuButton activeLayersMenuBtn;
    private final FlowPane activeLayersChipsBox;
    private final Map<DisplayMode, CheckBox> layerCheckBoxMap = new EnumMap<>(DisplayMode.class);

    private final Label dataLayersTitle;
    private final CheckBox contourCheck;
    private final CheckBox fluxVectorCheck;
    private final CheckBox resourceOverlayCheck;
    private final CheckBox floatingLayerCheck;

    private final Label uiOverlaysTitle;
    private final CheckBox legendCheck;
    private final CheckBox dateOverlayCheck;
    private final CheckBox cellInfoCheck;

    private Supplier<H3MapCanvas> mapCanvasSupplier;
    private Supplier<ColorLegend> colorLegendSupplier;
    private Supplier<ISimulationEngine> engineSupplier;
    private Supplier<ControlPanelRenderingSection> renderingSectionSupplier;
    private Supplier<CheckBox> pauseOnEventCheckSupplier;

    private Consumer<Boolean> onContourToggle;

    /**
     * Initializes layer stack and scientific presets card.
     */
    public ControlPanelLayersSection() {
        layersTitle = createCardTitle(I18n.getOrDefault("sim.card.layers_presets", "🗺️ 2. LAYER STACK & SCIENTIFIC PRESETS"));

        presetsTitle = new Label(I18n.getOrDefault("sim.presets.1click", "⚡ 1-Click Scientific Presets:"));
        presetsTitle.getStyleClass().add("opt-subheader");
        presetsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        presetSynth = createPresetBtn(I18n.getOrDefault("sim.preset.synthesis", "🌍 Synthesis"),
                I18n.getOrDefault("sim.preset.synthesis.desc", "3D Globe Mode + Biomes + Relief + Contour Lines"),
                this::applyPresetSynthesis);
        presetEcon = createPresetBtn(I18n.getOrDefault("sim.preset.economy", "📈 Economy"),
                I18n.getOrDefault("sim.preset.economy.desc", "Flux / GDP Map + Transport Vectors + Ore Deposits"),
                this::applyPresetEcon);
        presetClim = createPresetBtn(I18n.getOrDefault("sim.preset.climate", "🌡️ Climate"),
                I18n.getOrDefault("sim.preset.climate.desc", "Temperature Map + Aquifers + Relief Contour Lines"),
                this::applyPresetClimate);
        presetCliodyn = createPresetBtn(I18n.getOrDefault("sim.preset.cliodynamics", "🏛️ Cliodynamics"),
                I18n.getOrDefault("sim.preset.cliodynamics.desc", "Demographic Map + H3 Mesh + Event Pause"),
                this::applyPresetCliodynamics);

        GridPane presetGrid = new GridPane();
        presetGrid.setHgap(4);
        presetGrid.setVgap(4);
        presetGrid.add(presetSynth, 0, 0);
        presetGrid.add(presetEcon, 1, 0);
        presetGrid.add(presetClim, 0, 1);
        presetGrid.add(presetCliodyn, 1, 1);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        presetGrid.getColumnConstraints().addAll(col1, col2);

        VBox presetBox = new VBox(4, presetsTitle, presetGrid);
        presetBox.getStyleClass().add("subcard-section");

        layerComboLabel = new Label(I18n.getOrDefault("sim.layer.datacategory", "Active Data Layers (Multi-Select):"));
        layerComboLabel.getStyleClass().add("card-description-muted");
        layerComboLabel.setStyle("-fx-font-size: 11px;");

        activeLayersMenuBtn = new MenuButton();
        activeLayersMenuBtn.setMaxWidth(Double.MAX_VALUE);
        activeLayersMenuBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.datacategory", "Toggle one or more layers to superimpose on the map")));
        activeLayersMenuBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: #38bdf8; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");

        activeLayersChipsBox = new FlowPane(4, 4);
        activeLayersChipsBox.setStyle("-fx-padding: 2 0;");

        contourCheck = new CheckBox(I18n.getOrDefault("sim.layer.contours", "📈 Elevation Contour Lines"));
        contourCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.contours", "Displays elevation contour lines on H3 cells")));
        contourCheck.getStyleClass().add("opt-sub-checkbox");
        contourCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        contourCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) canvas.setShowContours(contourCheck.isSelected());
            if (onContourToggle != null) onContourToggle.accept(contourCheck.isSelected());
        });

        fluxVectorCheck = new CheckBox(I18n.getOrDefault("sim.layer.fluxvectors", "🌊 Flux & Transports (Vectors)"));
        fluxVectorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fluxvectors", "Overlays material flux and population transport vectors")));
        fluxVectorCheck.getStyleClass().add("opt-sub-checkbox");
        fluxVectorCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        fluxVectorCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) canvas.setShowFlowVectors(fluxVectorCheck.isSelected());
        });

        resourceOverlayCheck = new CheckBox(I18n.getOrDefault("sim.layer.resources", "💎 Deposits & Capital (Overlays)"));
        resourceOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.resources", "Displays metal, aquifer, and infrastructure markers")));
        resourceOverlayCheck.getStyleClass().add("opt-sub-checkbox");
        resourceOverlayCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        resourceOverlayCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) canvas.setShowResourceOverlay(resourceOverlayCheck.isSelected());
        });

        floatingLayerCheck = new CheckBox(I18n.getOrDefault("sim.layer.floating", "☁️ 2.5D Floating Layers (Altitude)"));
        floatingLayerCheck.setSelected(true);
        floatingLayerCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.floating", "Projects thematic layers in 2.5D elevation floating above biome topography")));
        floatingLayerCheck.getStyleClass().add("opt-sub-checkbox");
        floatingLayerCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        floatingLayerCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) canvas.setShowFloatingLayers(floatingLayerCheck.isSelected());
        });

        legendCheck = new CheckBox(I18n.getOrDefault("sim.layer.legend", "🗺️ Color Legend (Overlay)"));
        legendCheck.setSelected(true);
        legendCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.legend", "Shows or hides the map color legend pane")));
        legendCheck.getStyleClass().add("opt-sub-checkbox");
        legendCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        legendCheck.setOnAction(e -> {
            ColorLegend legend = getLegend();
            if (legend != null) {
                legend.setVisible(legendCheck.isSelected());
                legend.setManaged(legendCheck.isSelected());
            }
        });

        dateOverlayCheck = new CheckBox(I18n.getOrDefault("sim.layer.date_overlay", "📅 Date & Scenario Inset"));
        dateOverlayCheck.setSelected(true);
        dateOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.date_overlay", "Shows or hides time and scenario overlay badges on the map")));
        dateOverlayCheck.getStyleClass().add("opt-sub-checkbox");
        dateOverlayCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        dateOverlayCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) canvas.setShowCornerOverlays(dateOverlayCheck.isSelected());
        });

        cellInfoCheck = new CheckBox(I18n.getOrDefault("sim.layer.cell_info", "ℹ️ Hover Cell Info (Tooltip)"));
        cellInfoCheck.setSelected(true);
        cellInfoCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.cell_info", "Enables or disables inspection tooltip when hovering over hexagons")));
        cellInfoCheck.getStyleClass().add("opt-sub-checkbox");
        cellInfoCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        cellInfoCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) canvas.setShowMouseOverInfo(cellInfoCheck.isSelected());
        });

        dataLayersTitle = new Label(I18n.getOrDefault("sim.layer.datalayers", "📊 Data Layers & Vectors:"));
        dataLayersTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        VBox dataLayersBox = new VBox(4, dataLayersTitle, contourCheck, fluxVectorCheck, resourceOverlayCheck, floatingLayerCheck);
        dataLayersBox.getStyleClass().add("subcard-section");

        uiOverlaysTitle = new Label(I18n.getOrDefault("sim.layer.uioverlays", "🖥️ UI Overlays & Insets:"));
        uiOverlaysTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");

        VBox uiOverlaysBox = new VBox(4, uiOverlaysTitle, legendCheck, dateOverlayCheck, cellInfoCheck);
        uiOverlaysBox.getStyleClass().add("subcard-section");

        card = new VBox(8, layersTitle, presetBox, layerComboLabel, activeLayersMenuBtn, activeLayersChipsBox, dataLayersBox, uiOverlaysBox);
        card.getStyleClass().add("card-section");
    }

    private H3MapCanvas getCanvas() {
        return mapCanvasSupplier != null ? mapCanvasSupplier.get() : null;
    }

    private ColorLegend getLegend() {
        return colorLegendSupplier != null ? colorLegendSupplier.get() : null;
    }

    public void setMapCanvasSupplier(Supplier<H3MapCanvas> supplier) {
        this.mapCanvasSupplier = supplier;
    }

    public void setColorLegendSupplier(Supplier<ColorLegend> supplier) {
        this.colorLegendSupplier = supplier;
    }

    public void setEngineSupplier(Supplier<ISimulationEngine> supplier) {
        this.engineSupplier = supplier;
    }

    public void setRenderingSectionSupplier(Supplier<ControlPanelRenderingSection> supplier) {
        this.renderingSectionSupplier = supplier;
    }

    public void setPauseOnEventCheckSupplier(Supplier<CheckBox> supplier) {
        this.pauseOnEventCheckSupplier = supplier;
    }

    public void setOnContourToggle(Consumer<Boolean> onContourToggle) {
        this.onContourToggle = onContourToggle;
    }

    public VBox getCard() {
        return card;
    }

    public CheckBox getContourCheck() {
        return contourCheck;
    }

    public void rebuildLayerMenuItems() {
        if (activeLayersMenuBtn == null) return;
        H3MapCanvas canvas = getCanvas();
        Set<DisplayMode> active = canvas != null ? canvas.getActiveDisplayModes() : Set.of(DisplayMode.BIOME, DisplayMode.POPULATION);
        activeLayersMenuBtn.getItems().clear();
        layerCheckBoxMap.clear();

        for (DisplayMode.Category cat : DisplayMode.Category.values()) {
            Label catHeader = new Label("─── " + cat.getCategoryName() + " ───");
            catHeader.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 4 8 2 8;");
            CustomMenuItem catItem = new CustomMenuItem(catHeader, false);
            activeLayersMenuBtn.getItems().add(catItem);

            for (DisplayMode dm : DisplayMode.values()) {
                if (dm.getCategory() == cat) {
                    CheckBox cb = new CheckBox(dm.getDisplayName());
                    cb.setTooltip(new Tooltip(dm.getDescription()));
                    cb.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px; -fx-padding: 2 8; -fx-cursor: hand;");
                    cb.setSelected(active.contains(dm));
                    cb.setOnAction(e -> onLayerToggled(dm, cb.isSelected()));
                    layerCheckBoxMap.put(dm, cb);
                    CustomMenuItem mi = new CustomMenuItem(cb, false);
                    activeLayersMenuBtn.getItems().add(mi);
                }
            }
            activeLayersMenuBtn.getItems().add(new SeparatorMenuItem());
        }
    }

    private void onLayerToggled(DisplayMode mode, boolean selected) {
        H3MapCanvas canvas = getCanvas();
        if (canvas != null) {
            canvas.setDisplayModeActive(mode, selected);
            ColorLegend legend = getLegend();
            if (legend != null) legend.updateFromCanvas(canvas);
            updateActiveLayerUI();
        }
    }

    public void updateActiveLayerUI() {
        if (activeLayersMenuBtn == null) return;
        H3MapCanvas canvas = getCanvas();

        if (canvas == null) {
            for (Map.Entry<DisplayMode, CheckBox> entry : layerCheckBoxMap.entrySet()) {
                DisplayMode dm = entry.getKey();
                entry.getValue().setSelected(dm == DisplayMode.BIOME || dm == DisplayMode.POPULATION);
            }
            activeLayersMenuBtn.setText("📊 " + I18n.getOrDefault("sim.layer.datacategory", "Couches Actives") + " (2)");
            return;
        }

        Set<DisplayMode> active = canvas.getActiveDisplayModes();
        for (Map.Entry<DisplayMode, CheckBox> entry : layerCheckBoxMap.entrySet()) {
            entry.getValue().setSelected(active.contains(entry.getKey()));
        }

        int count = active.size();
        if (count == 0) {
            activeLayersMenuBtn.setText("📊 " + I18n.getOrDefault("sim.layer.none", "Aucun calque"));
        } else if (count == 1) {
            activeLayersMenuBtn.setText("📊 " + active.iterator().next().getDisplayName());
        } else {
            activeLayersMenuBtn.setText(String.format(java.util.Locale.ROOT, "📊 %s (%d %s)",
                    I18n.getOrDefault("sim.layer.datacategory", "Couches Actives"), count, I18n.getOrDefault("sim.layer.checked", "cochées")));
        }

        if (activeLayersChipsBox != null) {
            activeLayersChipsBox.getChildren().clear();
            for (DisplayMode dm : active) {
                Button chip = new Button(dm.getDisplayName() + " ✕");
                chip.setStyle("-fx-background-color: rgba(14, 165, 233, 0.18); -fx-text-fill: #38bdf8; -fx-border-color: #38bdf8; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-cursor: hand;");
                chip.setOnAction(e -> {
                    canvas.setDisplayModeActive(dm, false);
                    ColorLegend legend = getLegend();
                    if (legend != null) legend.updateFromCanvas(canvas);
                    updateActiveLayerUI();
                });
                activeLayersChipsBox.getChildren().add(chip);
            }
        }
    }

    public void applyPresetSynthesis() {
        ControlPanelRenderingSection rnd = renderingSectionSupplier != null ? renderingSectionSupplier.get() : null;
        if (rnd != null) {
            rnd.getMode3dCheck().setSelected(true);
            rnd.update3DControlsState(true);
        }
        contourCheck.setSelected(true);
        fluxVectorCheck.setSelected(false);
        resourceOverlayCheck.setSelected(false);

        H3MapCanvas canvas = getCanvas();
        if (canvas != null) {
            canvas.setViewMode(ViewMode.VIEW_3D);
            canvas.setActiveDisplayModes(List.of(DisplayMode.BIOME));
            canvas.setShowContours(true);
            canvas.setShowFlowVectors(false);
            canvas.setShowResourceOverlay(false);
            canvas.setSmoothMap(true);
            ColorLegend legend = getLegend();
            if (legend != null) legend.updateFromCanvas(canvas);
            updateActiveLayerUI();
        }
    }

    public void applyPresetEcon() {
        ControlPanelRenderingSection rnd = renderingSectionSupplier != null ? renderingSectionSupplier.get() : null;
        if (rnd != null) {
            rnd.getMode3dCheck().setSelected(false);
            rnd.update3DControlsState(false);
        }
        contourCheck.setSelected(false);
        fluxVectorCheck.setSelected(true);
        resourceOverlayCheck.setSelected(true);

        H3MapCanvas canvas = getCanvas();
        if (canvas != null) {
            canvas.setViewMode(ViewMode.VIEW_2D);
            canvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.FLUX));
            canvas.setShowContours(false);
            canvas.setShowFlowVectors(true);
            canvas.setShowResourceOverlay(true);
            ColorLegend legend = getLegend();
            if (legend != null) legend.updateFromCanvas(canvas);
            updateActiveLayerUI();
        }
    }

    public void applyPresetClimate() {
        ControlPanelRenderingSection rnd = renderingSectionSupplier != null ? renderingSectionSupplier.get() : null;
        if (rnd != null) {
            rnd.getMode3dCheck().setSelected(false);
            rnd.update3DControlsState(false);
        }
        contourCheck.setSelected(true);
        fluxVectorCheck.setSelected(false);
        resourceOverlayCheck.setSelected(true);

        H3MapCanvas canvas = getCanvas();
        if (canvas != null) {
            canvas.setViewMode(ViewMode.VIEW_2D);
            canvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.TEMPERATURE));
            canvas.setShowContours(true);
            canvas.setShowFlowVectors(false);
            canvas.setShowResourceOverlay(true);
            ColorLegend legend = getLegend();
            if (legend != null) legend.updateFromCanvas(canvas);
            updateActiveLayerUI();
        }
    }

    public void applyPresetCliodynamics() {
        ControlPanelRenderingSection rnd = renderingSectionSupplier != null ? renderingSectionSupplier.get() : null;
        if (rnd != null) {
            rnd.getMode3dCheck().setSelected(false);
            rnd.update3DControlsState(false);
            rnd.getHexGridCheck().setSelected(true);
        }
        contourCheck.setSelected(false);
        fluxVectorCheck.setSelected(false);
        resourceOverlayCheck.setSelected(false);

        CheckBox pauseCheck = pauseOnEventCheckSupplier != null ? pauseOnEventCheckSupplier.get() : null;
        if (pauseCheck != null) pauseCheck.setSelected(true);

        ISimulationEngine engine = engineSupplier != null ? engineSupplier.get() : null;
        if (engine != null) engine.setPauseAtNextEvent(true);

        H3MapCanvas canvas = getCanvas();
        if (canvas != null) {
            canvas.setViewMode(ViewMode.VIEW_2D);
            canvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.POPULATION));
            canvas.setShowContours(false);
            canvas.setShowFlowVectors(false);
            canvas.setShowResourceOverlay(false);
            canvas.setShowHexGrid(true);
            ColorLegend legend = getLegend();
            if (legend != null) legend.updateFromCanvas(canvas);
            updateActiveLayerUI();
        }
    }

    public void updateTexts() {
        if (layersTitle != null) {
            layersTitle.setText(I18n.getOrDefault("sim.card.layers_presets", "🗺️ 2. LAYER STACK & SCIENTIFIC PRESETS"));
        }
        if (presetsTitle != null) {
            presetsTitle.setText(I18n.getOrDefault("sim.presets.1click", "⚡ 1-Click Scientific Presets:"));
        }
        if (presetSynth != null) {
            presetSynth.setText(I18n.getOrDefault("sim.preset.synthesis", "🌍 Synthesis"));
            presetSynth.setTooltip(new Tooltip(I18n.getOrDefault("sim.preset.synthesis.desc", "3D Globe Mode + Biomes + Relief + Contour Lines")));
        }
        if (presetEcon != null) {
            presetEcon.setText(I18n.getOrDefault("sim.preset.economy", "📈 Economy"));
            presetEcon.setTooltip(new Tooltip(I18n.getOrDefault("sim.preset.economy.desc", "Flux / GDP Map + Transport Vectors + Ore Deposits")));
        }
        if (presetClim != null) {
            presetClim.setText(I18n.getOrDefault("sim.preset.climate", "🌡️ Climate"));
            presetClim.setTooltip(new Tooltip(I18n.getOrDefault("sim.preset.climate.desc", "Temperature Map + Aquifers + Relief Contour Lines")));
        }
        if (presetCliodyn != null) {
            presetCliodyn.setText(I18n.getOrDefault("sim.preset.cliodynamics", "🏛️ Cliodynamics"));
            presetCliodyn.setTooltip(new Tooltip(I18n.getOrDefault("sim.preset.cliodynamics.desc", "Demographic Map + H3 Mesh + Event Pause")));
        }
        if (layerComboLabel != null) {
            layerComboLabel.setText(I18n.getOrDefault("sim.layer.datacategory", "Active Data Layers (Multi-Select):"));
        }
        if (dataLayersTitle != null) {
            dataLayersTitle.setText(I18n.getOrDefault("sim.layer.datalayers", "📊 Data Layers & Vectors:"));
        }
        if (uiOverlaysTitle != null) {
            uiOverlaysTitle.setText(I18n.getOrDefault("sim.layer.uioverlays", "🖥️ UI Overlays & Insets:"));
        }

        rebuildLayerMenuItems();

        contourCheck.setText(I18n.getOrDefault("sim.layer.contours", "📈 Elevation Contour Lines"));
        contourCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.contours", "Displays elevation contour lines on H3 cells")));
        fluxVectorCheck.setText(I18n.getOrDefault("sim.layer.fluxvectors", "🌊 Flux & Transports (Vectors)"));
        fluxVectorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fluxvectors", "Overlays material flux and population transport vectors")));
        resourceOverlayCheck.setText(I18n.getOrDefault("sim.layer.resources", "💎 Deposits & Capital (Overlays)"));
        resourceOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.resources", "Displays metal, aquifer, and infrastructure markers")));
        if (floatingLayerCheck != null) {
            floatingLayerCheck.setText(I18n.getOrDefault("sim.layer.floating", "☁️ 2.5D Floating Layers (Altitude)"));
            floatingLayerCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.floating", "Projects thematic layers in 2.5D elevation floating above biome topography")));
        }
        legendCheck.setText(I18n.getOrDefault("sim.layer.legend", "🗺️ Color Legend (Overlay)"));
        legendCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.legend", "Shows or hides the map color legend pane")));
        dateOverlayCheck.setText(I18n.getOrDefault("sim.layer.date_overlay", "📅 Date & Scenario Inset"));
        dateOverlayCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.date_overlay", "Shows or hides time and scenario overlay badges on the map")));
        cellInfoCheck.setText(I18n.getOrDefault("sim.layer.cell_info", "ℹ️ Hover Cell Info (Tooltip)"));
        cellInfoCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.cell_info", "Enables or disables inspection tooltip when hovering over hexagons")));
    }

    private Button createPresetBtn(String label, String tooltip, Runnable action) {
        Button btn = new Button(label);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("button-secondary");
        btn.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-padding: 5; -fx-cursor: hand;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private static Label createCardTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        return label;
    }
}
