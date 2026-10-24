/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.control;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.H3MapCanvas;
import org.ether.society.ui.ScientificColorMap;
import org.ether.society.ui.ViewMode;
import org.ether.society.ui.ColorLegend;

import java.util.function.Supplier;

/**
 * 3D/2D rendering and projection parameter card for {@link org.ether.society.ui.ControlPanel}.
 *
 * <p>Extracted from {@code ControlPanel} to satisfy the Single Responsibility Principle (SRP).
 * Manages 3D spherical globe transformations, topographic vertical exaggeration $\sigma_{\text{relief}}$,
 * Lambertian hillshading, solar day/night terminator overlays, perceptually uniform palettes, and fullscreen mode.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class ControlPanelRenderingSection {

    private final VBox card;
    private final Label renderTitle;
    private final CheckBox mode3dCheck;
    private final Label reliefLabel;
    private final Slider reliefSlider;
    private final CheckBox autoRotateCheck;
    private final CheckBox hillshadingCheck;
    private final CheckBox solarTerminatorCheck;
    private final CheckBox smoothMapCheck;
    private final CheckBox hexGridCheck;
    private final Label paletteLabel;
    private final ComboBox<ScientificColorMap> paletteCombo;
    private final Button fullScreenBtn;

    private Runnable onFullScreen;
    private Supplier<H3MapCanvas> mapCanvasSupplier;
    private Supplier<ColorLegend> colorLegendSupplier;

    /**
     * Initializes projection and rendering parameter controls.
     */
    public ControlPanelRenderingSection() {
        renderTitle = createCardTitle(I18n.getOrDefault("sim.card.rendering", "🌐 3. PROJECTION & RENDERING PARAMETERS"));

        mode3dCheck = new CheckBox(I18n.getOrDefault("sim.layer.mode3d", "🌐 3D H3 Globe"));
        mode3dCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.mode3d", "Toggles between 3D spherical globe and 2D flat map")));
        mode3dCheck.getStyleClass().add("opt-sub-checkbox");
        mode3dCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");

        reliefLabel = new Label(I18n.getOrDefault("sim.layer.relief3d", "⛰️ 3D Relief") + " : 25x");
        reliefLabel.getStyleClass().add("control-label");
        reliefLabel.setDisable(true);

        reliefSlider = new Slider(0.0, 50.0, 25.0);
        reliefSlider.setBlockIncrement(5.0);
        reliefSlider.setMajorTickUnit(15.0);
        reliefSlider.setMinorTickCount(2);
        reliefSlider.setShowTickMarks(true);
        reliefSlider.setShowTickLabels(false);
        reliefSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.relief3d", "Adjusts topographic elevation height in 3D Globe mode (0.0x to 50x)")));
        reliefSlider.setDisable(true);
        reliefSlider.valueProperty().addListener((obs, oldV, newV) -> {
            double val = newV.doubleValue();
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.setVerticalExaggeration(val);
            }
            reliefLabel.setText(String.format(java.util.Locale.ROOT, "%s : %.0fx", I18n.getOrDefault("sim.layer.relief3d", "⛰️ 3D Relief"), val));
        });

        autoRotateCheck = new CheckBox(I18n.getOrDefault("sim.layer.autorotate", "🔄 Auto-rotate Globe"));
        autoRotateCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.autorotate", "Auto-rotates 3D spherical globe")));
        autoRotateCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        autoRotateCheck.setDisable(true);
        autoRotateCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.setAutoRotating(autoRotateCheck.isSelected());
            }
        });

        hillshadingCheck = new CheckBox(I18n.getOrDefault("sim.layer.hillshading", "⛰️ Topographic Hillshading"));
        hillshadingCheck.setSelected(false);
        hillshadingCheck.setDisable(true);
        hillshadingCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hillshading", "Applies Lambertian topographic shading based on relief slope")));
        hillshadingCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        hillshadingCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.setShowHillshading(hillshadingCheck.isSelected());
            }
        });

        solarTerminatorCheck = new CheckBox(I18n.getOrDefault("sim.layer.solarterminator", "☀️ Solar Day/Night Terminator"));
        solarTerminatorCheck.setSelected(false);
        solarTerminatorCheck.setDisable(true);
        solarTerminatorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.solarterminator", "Displays night and twilight shadow overlay")));
        solarTerminatorCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        solarTerminatorCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.setShowSolarTerminator(solarTerminatorCheck.isSelected());
            }
        });

        mode3dCheck.setOnAction(e -> {
            boolean is3D = mode3dCheck.isSelected();
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.setViewMode(is3D ? ViewMode.VIEW_3D : ViewMode.VIEW_2D);
            }
            update3DControlsState(is3D);
        });

        VBox globe3dSubBox = new VBox(6, reliefLabel, reliefSlider, autoRotateCheck, hillshadingCheck, solarTerminatorCheck);
        globe3dSubBox.setStyle("-fx-padding: 4 0 4 12; -fx-border-color: rgba(56, 189, 248, 0.2); -fx-border-width: 0 0 0 2;");

        smoothMapCheck = new CheckBox(I18n.getOrDefault("sim.layer.smoothmap", "🎨 Smooth Map (Continuous Heatmap)"));
        smoothMapCheck.setSelected(true);
        smoothMapCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.smoothmap", "Displays continuous map with smooth gradients instead of individual hexagons")));
        smoothMapCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        smoothMapCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.setSmoothMap(smoothMapCheck.isSelected());
            }
        });

        hexGridCheck = new CheckBox(I18n.getOrDefault("sim.layer.hexgrid", "⬡ H3 Hexagon Borders"));
        hexGridCheck.setSelected(false);
        hexGridCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hexgrid", "Shows or hides H3 hexagon grid (smooth view without borders vs grid view)")));
        hexGridCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand;");
        hexGridCheck.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null) {
                canvas.setShowHexGrid(hexGridCheck.isSelected());
            }
        });

        paletteLabel = new Label(I18n.getOrDefault("sim.render.palette", "🎨 Scientific Palette:"));
        paletteLabel.getStyleClass().add("control-label");

        paletteCombo = new ComboBox<>();
        paletteCombo.getItems().addAll(ScientificColorMap.values());
        paletteCombo.setValue(ScientificColorMap.TURBO);
        paletteCombo.setMaxWidth(Double.MAX_VALUE);
        paletteCombo.setStyle("-fx-font-size: 11px;");
        paletteCombo.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.palette", "Selects perceptually uniform color palette")));
        paletteCombo.setOnAction(e -> {
            H3MapCanvas canvas = getCanvas();
            if (canvas != null && paletteCombo.getValue() != null) {
                canvas.setScientificColorMap(paletteCombo.getValue());
                ColorLegend legend = getLegend();
                if (legend != null) legend.updateFromCanvas(canvas);
            }
        });

        fullScreenBtn = new Button(I18n.getOrDefault("sim.btn.fullscreen", "🖥️ Plein Écran (Carte)"));
        fullScreenBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fullscreen", "Affiche la carte en plein écran sans aucun autre élément (Touche Échap pour quitter)")));
        fullScreenBtn.setMaxWidth(Double.MAX_VALUE);
        fullScreenBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 7 12; -fx-background-radius: 6; -fx-cursor: hand;");
        fullScreenBtn.setOnAction(e -> {
            if (onFullScreen != null) onFullScreen.run();
        });

        card = new VBox(8, renderTitle, mode3dCheck, globe3dSubBox, paletteLabel, paletteCombo, smoothMapCheck, hexGridCheck, fullScreenBtn);
        card.getStyleClass().add("card-section");
    }

    private H3MapCanvas getCanvas() {
        return mapCanvasSupplier != null ? mapCanvasSupplier.get() : null;
    }

    private ColorLegend getLegend() {
        return colorLegendSupplier != null ? colorLegendSupplier.get() : null;
    }

    public void setMapCanvasSupplier(Supplier<H3MapCanvas> mapCanvasSupplier) {
        this.mapCanvasSupplier = mapCanvasSupplier;
    }

    public void setColorLegendSupplier(Supplier<ColorLegend> colorLegendSupplier) {
        this.colorLegendSupplier = colorLegendSupplier;
    }

    public void setOnFullScreen(Runnable onFullScreen) {
        this.onFullScreen = onFullScreen;
    }

    public VBox getCard() {
        return card;
    }

    public CheckBox getMode3dCheck() {
        return mode3dCheck;
    }

    public CheckBox getHexGridCheck() {
        return hexGridCheck;
    }

    public CheckBox getSmoothMapCheck() {
        return smoothMapCheck;
    }

    public Slider getReliefSlider() {
        return reliefSlider;
    }

    public void update3DControlsState(boolean is3D) {
        reliefSlider.setDisable(!is3D);
        reliefLabel.setDisable(!is3D);
        autoRotateCheck.setDisable(!is3D);
        hillshadingCheck.setDisable(!is3D);
        solarTerminatorCheck.setDisable(!is3D);
    }

    public void updateViewToggleButton(H3MapCanvas canvas) {
        if (canvas != null && mode3dCheck != null) {
            boolean is3D = canvas.getViewMode() == ViewMode.VIEW_3D;
            mode3dCheck.setSelected(is3D);
            update3DControlsState(is3D);
            if (hexGridCheck != null) hexGridCheck.setSelected(canvas.isShowHexGrid());
            if (smoothMapCheck != null) smoothMapCheck.setSelected(canvas.isSmoothMap());
        }
    }

    public void updateTexts() {
        if (renderTitle != null) {
            renderTitle.setText(I18n.getOrDefault("sim.card.rendering", "🌐 3. PROJECTION & RENDERING PARAMETERS"));
        }
        if (paletteLabel != null) {
            paletteLabel.setText(I18n.getOrDefault("sim.render.palette", "🎨 Scientific Palette:"));
        }
        if (paletteCombo != null) {
            paletteCombo.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.palette", "Selects perceptually uniform color palette")));
        }
        mode3dCheck.setText(I18n.getOrDefault("sim.layer.mode3d", "🌐 3D H3 Globe"));
        mode3dCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.mode3d", "Toggles between 3D spherical globe and 2D flat map")));
        reliefLabel.setText(String.format(java.util.Locale.ROOT, "%s : %.0fx", I18n.getOrDefault("sim.layer.relief3d", "⛰️ 3D Relief"), reliefSlider != null ? reliefSlider.getValue() : 25.0));
        reliefSlider.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.relief3d", "Adjusts topographic elevation height in 3D Globe mode (0.0x to 50x)")));
        autoRotateCheck.setText(I18n.getOrDefault("sim.layer.autorotate", "🔄 Auto-rotate Globe"));
        autoRotateCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.autorotate", "Auto-rotates 3D spherical globe")));
        if (hillshadingCheck != null) {
            hillshadingCheck.setText(I18n.getOrDefault("sim.layer.hillshading", "⛰️ Topographic Hillshading"));
            hillshadingCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hillshading", "Applies Lambertian topographic shading based on relief slope")));
        }
        if (solarTerminatorCheck != null) {
            solarTerminatorCheck.setText(I18n.getOrDefault("sim.layer.solarterminator", "☀️ Solar Day/Night Terminator"));
            solarTerminatorCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.solarterminator", "Displays night and twilight shadow overlay")));
        }
        if (smoothMapCheck != null) {
            smoothMapCheck.setText(I18n.getOrDefault("sim.layer.smoothmap", "🎨 Smooth Map (Continuous Heatmap)"));
            smoothMapCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.smoothmap", "Displays continuous map with smooth gradients instead of individual hexagons")));
        }
        hexGridCheck.setText(I18n.getOrDefault("sim.layer.hexgrid", "⬡ H3 Hexagon Borders"));
        hexGridCheck.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.hexgrid", "Shows or hides H3 hexagon grid (smooth view without borders vs grid view)")));
        if (fullScreenBtn != null) {
            fullScreenBtn.setText(I18n.getOrDefault("sim.btn.fullscreen", "🖥️ Fullscreen (Map)"));
            fullScreenBtn.setTooltip(new Tooltip(I18n.getOrDefault("sim.tooltip.fullscreen", "Displays the map in fullscreen mode without other UI panels (Press Esc to exit)")));
        }
    }

    private static Label createCardTitle(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");
        return label;
    }
}
