/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.ether.society.core.ISimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.i18n.Language;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.List;

public class ControlPanel extends HBox {
    private final ISimulationEngine engine;
    private final Label yearLabel;
    private final Label seasonLabel; // Season indicator
    private final Label ageLabel; // Current Civilization Age
    private H3MapCanvas mapCanvas; // Reference to canvas for view toggle
    private MiniMap miniMap; // Reference to mini-map
    private ColorLegend colorLegend; // Reference to color legend

    // UI Controls that need text updates
    private final Button startBtn;
    private final Button pauseBtn;
    private final Button speed1x;
    private final Button speed5x;
    private final Button speed20x;
    private final Button viewToggle;
    private final Button displayToggle; // Biome/Population/Food/Temp
    private final Button miniMapToggle;
    private final Label langLabel;
    private final ComboBox<Language> langCombo;
    private final Label statsLabel; // Population stats
    private final Label eventLabel; // Last event display
    private final List<String> eventHistory = new ArrayList<>();

    // Analytics
    private Runnable onAnalytics;

    // Callback for actions
    private Runnable onSave;
    private Runnable onLoad;

    public ControlPanel(ISimulationEngine engine) {
        this.engine = engine;
        this.yearLabel = new Label(); // Text set in updateTexts
        this.seasonLabel = new Label("Spring");
        seasonLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 12px; -fx-font-weight: bold;");

        this.ageLabel = new Label("Stone Age");
        ageLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold;");

        setSpacing(10);
        setPadding(new Insets(10));
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #263238; -fx-text-fill: white;");

        // Save/Load Buttons
        Button saveBtn = new Button("Save");
        saveBtn.setTooltip(new Tooltip("Save current simulation state"));
        saveBtn.setOnAction(e -> {
            if (onSave != null)
                onSave.run();
        });

        Button loadBtn = new Button("Load");
        loadBtn.setTooltip(new Tooltip("Load a saved simulation"));
        loadBtn.setOnAction(e -> {
            if (onLoad != null)
                onLoad.run();
        });

        startBtn = new Button();
        startBtn.setTooltip(new Tooltip("Start the simulation"));
        startBtn.setOnAction(e -> engine.start());

        pauseBtn = new Button();
        pauseBtn.setTooltip(new Tooltip("Pause the simulation"));
        pauseBtn.setOnAction(e -> engine.pause());

        speed1x = new Button("1x");
        speed1x.setTooltip(new Tooltip("Normal speed"));
        speed1x.setOnAction(e -> engine.setSpeed(1));

        speed5x = new Button("5x");
        speed5x.setTooltip(new Tooltip("5x speed"));
        speed5x.setOnAction(e -> engine.setSpeed(5));

        speed20x = new Button("20x");
        speed20x.setTooltip(new Tooltip("20x speed (fast forward)"));
        speed20x.setOnAction(e -> engine.setSpeed(20));

        // 2D/3D Toggle Button
        viewToggle = new Button();
        viewToggle.setTooltip(new Tooltip("Toggle between 2D and 3D view"));
        viewToggle.setOnAction(e -> {
            if (mapCanvas != null) {
                ViewMode current = mapCanvas.getViewMode();
                ViewMode next = (current == ViewMode.VIEW_2D) ? ViewMode.VIEW_3D : ViewMode.VIEW_2D;
                mapCanvas.setViewMode(next);
                updateViewToggleButton();
            }
        });

        // Display Mode Toggle Button (Biome -> Population -> Food -> Temperature)
        displayToggle = new Button();
        displayToggle
                .setTooltip(new Tooltip("Cycle through display modes: Biome, Population, Food, Temperature, etc."));
        displayToggle.setOnAction(e -> {
            if (mapCanvas != null) {
                DisplayMode current = mapCanvas.getDisplayMode();
                DisplayMode[] modes = DisplayMode.values();
                int nextIndex = (current.ordinal() + 1) % modes.length;
                mapCanvas.setDisplayMode(modes[nextIndex]);
                updateDisplayToggleButton();

                // Update color legend
                if (colorLegend != null) {
                    colorLegend.setDisplayMode(modes[nextIndex]);
                }
            }
        });

        // Mini-map Toggle Button
        miniMapToggle = new Button();
        miniMapToggle.setTooltip(new Tooltip("Show/hide the mini-map"));
        miniMapToggle.setOnAction(e -> {
            if (miniMap != null) {
                miniMap.setVisible(!miniMap.isVisible());
            }
        });

        // Analytics Button
        Button analyticsBtn = new Button("Analytics");
        analyticsBtn.setTooltip(new Tooltip("Show historical data dashboard"));
        analyticsBtn.setStyle("-fx-base: #673ab7;"); // Distinct color
        analyticsBtn.setOnAction(e -> {
            if (onAnalytics != null)
                onAnalytics.run();
        });

        // Statistics Label
        statsLabel = new Label("Pop: 0");
        statsLabel.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 12px; -fx-font-weight: bold;");
        statsLabel.setTooltip(new Tooltip("Total population | Food resources | Populated cells"));

        // Event Label
        eventLabel = new Label("");
        eventLabel.setStyle("-fx-text-fill: #e91e63; -fx-font-weight: bold;");
        eventLabel.setTooltip(new Tooltip("No events yet"));

        // Language Selector
        langLabel = new Label();
        langLabel.setStyle("-fx-text-fill: white;");

        langCombo = new ComboBox<>();
        langCombo.getItems().addAll(Language.values());
        langCombo.setValue(I18n.getCurrentLanguage());
        langCombo.setTooltip(new Tooltip("Select display language"));
        langCombo.setOnAction(e -> I18n.setLanguage(langCombo.getValue()));

        // Custom cell factory to show display name
        langCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Language item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        langCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Language item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });

        yearLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        getChildren().addAll(yearLabel, seasonLabel, ageLabel, statsLabel, eventLabel, saveBtn, loadBtn, startBtn,
                pauseBtn,
                speed1x, speed5x,
                speed20x,
                viewToggle, displayToggle, miniMapToggle, analyticsBtn, langLabel, langCombo);

        // Initial text update
        updateTexts();

        // Listen for language changes
        I18n.languageProperty().addListener((obs, old, val) -> updateTexts());
    }

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    public void setOnLoad(Runnable onLoad) {
        this.onLoad = onLoad;
    }

    public void setOnAnalytics(Runnable onAnalytics) {
        this.onAnalytics = onAnalytics;
    }

    public void setMapCanvas(H3MapCanvas canvas) {
        this.mapCanvas = canvas;
        updateViewToggleButton();
    }

    public void setMiniMap(MiniMap miniMap) {
        this.miniMap = miniMap;
    }

    public void setColorLegend(ColorLegend legend) {
        this.colorLegend = legend;
    }

    public void updateYear(String year) {
        yearLabel.setText(year);
    }

    private void updateTexts() {
        startBtn.setText(I18n.get("ui.control.start"));
        pauseBtn.setText(I18n.get("ui.control.pause"));
        speed1x.setText(I18n.get("ui.control.speed") + " 1x");
        speed5x.setText(I18n.get("ui.control.speed") + " 5x");
        speed20x.setText(I18n.get("ui.control.speed") + " 20x");
        miniMapToggle.setText(I18n.get("ui.control.minimap"));
        langLabel.setText(I18n.get("ui.control.language"));

        updateViewToggleButton();
        updateDisplayToggleButton();
        updateYear(engine.getTimeManager().getFormattedDate());
    }

    private void updateViewToggleButton() {
        if (mapCanvas != null) {
            ViewMode current = mapCanvas.getViewMode();
            if (current == ViewMode.VIEW_2D) {
                viewToggle.setText(I18n.get("ui.control.view.3d"));
            } else {
                viewToggle.setText(I18n.get("ui.control.view.2d"));
            }
        } else {
            viewToggle.setText(I18n.get("ui.control.view.3d"));
        }
    }

    private void updateDisplayToggleButton() {
        if (mapCanvas != null) {
            DisplayMode current = mapCanvas.getDisplayMode();
            displayToggle.setText(current.getDisplayName());
        } else {
            displayToggle.setText("Biome");
        }
    }

    /**
     * Update population statistics display.
     */
    public void updateStats(long population, double food, long populatedCells) {
        String popStr = formatNumber(population);
        String foodStr = formatNumber((long) food);
        statsLabel.setText(String.format("Pop: %s | Food: %s | Cells: %d", popStr, foodStr, populatedCells));
    }

    private String formatNumber(long num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        }
        return String.valueOf(num);
    }

    /**
     * Update season display based on current month.
     * 
     * @param month Current month (0-11, 0=January)
     */
    public void updateSeason(int month) {
        // Seasons for Northern Hemisphere
        String[] seasonNames = { "Winter", "Spring", "Summer", "Autumn" };
        String[] seasonColors = { "#64b5f6", "#4caf50", "#ff9800", "#ff5722" };

        // Map month to season (Dec-Feb=0, Mar-May=1, Jun-Aug=2, Sep-Nov=3)
        int seasonIndex;
        if (month == 11 || month == 0 || month == 1) {
            seasonIndex = 0; // Winter
        } else if (month >= 2 && month <= 4) {
            seasonIndex = 1; // Spring
        } else if (month >= 5 && month <= 7) {
            seasonIndex = 2; // Summer
        } else {
            seasonIndex = 3; // Autumn
        }

        seasonLabel.setText(seasonNames[seasonIndex]);
        seasonLabel.setStyle(
                "-fx-text-fill: " + seasonColors[seasonIndex] + "; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    /**
     * Log new events to the UI.
     */
    public void logEvents(List<String> events) {
        if (events.isEmpty())
            return;

        // Update history
        eventHistory.addAll(events);

        // Show last event
        String lastEvent = events.get(events.size() - 1);
        eventLabel.setText(lastEvent);

        // Update tooltip
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, eventHistory.size() - 10); i < eventHistory.size(); i++) {
            sb.append(eventHistory.get(i)).append("\n");
        }
        eventLabel.getTooltip().setText(sb.toString());
    }

    public void updateAge(String ageName) {
        ageLabel.setText(ageName);
    }
}
