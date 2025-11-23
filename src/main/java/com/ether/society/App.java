
/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package com.ether.society;

import com.ether.society.config.Configuration;
import com.ether.society.config.ConfigurationLoader;
import com.ether.society.core.SimulationEngine;
import com.ether.society.ui.ControlPanel;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JavaFX Application entry point.
 * Initializes and displays the simulation UI.
 *
 * @author Silvere Martin-Michiellot
 * @version 2.0.0
 * @since 1.0.0
 */
public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Human Society Simulation...");
        primaryStage.setTitle("Human Society Simulation - Ether");

        // Load configuration
        Configuration config;
        try {
            config = ConfigurationLoader.loadDefault();
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            return;
        }

        SimulationEngine engine = new SimulationEngine(config);

        BorderPane root = new BorderPane();

        // MapCanvas mapCanvas = new MapCanvas(engine.getWorld());
        // ScrollPane scrollPane = new ScrollPane(mapCanvas);
        // scrollPane.setFitToWidth(true);
        // scrollPane.setFitToHeight(true);
        // scrollPane.setPannable(true);

        ControlPanel controlPanel = new ControlPanel(engine);
        // StatsPanel statsPanel = new StatsPanel(engine);

        // root.setCenter(scrollPane);
        root.setBottom(controlPanel);
        // root.setRight(statsPanel);

        // Info label with tooltips
        Label infoLabel = new Label("Simulation Running");
        infoLabel.setPadding(new javafx.geometry.Insets(5));
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: white;");
        root.setTop(infoLabel);

        // mapCanvas.setOnCellHover(cell -> {
        // String info = String.format(
        // "Biome: %s | Elev: %.2f | Temp: %.1f°C | Rain: %.2f | Humans: %d",
        // cell.getBiome(), cell.getElevation(), cell.getTemperature(),
        // cell.getRainfall(), cell.getHumanCount());
        // infoLabel.setText(info);
        // });

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        // Render loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // mapCanvas.draw();
                controlPanel.updateYear(engine.getTimeManager().getFormattedDate());
                // statsPanel.update();
            }
        };
        timer.start();

        logger.info("Application started successfully");
    }
}
