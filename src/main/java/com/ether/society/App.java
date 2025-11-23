package com.ether.society;

import com.ether.society.ui.ControlPanel;
import com.ether.society.ui.MapCanvas;
import com.ether.society.ui.StatsPanel;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Human Society Simulation...");
        primaryStage.setTitle("Human Society Simulation");

        SimulationEngine engine = new SimulationEngine();

        BorderPane root = new BorderPane();

        MapCanvas mapCanvas = new MapCanvas(engine.getWorld());
        ScrollPane scrollPane = new ScrollPane(mapCanvas);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        ControlPanel controlPanel = new ControlPanel(engine);
        StatsPanel statsPanel = new StatsPanel(engine);

        root.setCenter(scrollPane);
        root.setBottom(controlPanel);
        root.setRight(statsPanel);

        // Tooltip/Info logic
        Label infoLabel = new Label("Hover over map for info");
        infoLabel.setPadding(new javafx.geometry.Insets(5));
        root.setTop(infoLabel);

        mapCanvas.setOnCellHover(cell -> {
            String info = String.format(
                    "Biome: %s | Elev: %.2f | Temp: %.1f°C | Rain: %.2f | Humans: %d | Food: %.1f | Water: %.1f | Wood: %.1f",
                    cell.getBiome(), cell.getElevation(), cell.getTemperature(), cell.getRainfall(),
                    cell.getHumanCount(),
                    cell.getResource("Food"), cell.getResource("Water"), cell.getResource("Wood"));
            infoLabel.setText(info);
        });

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        // Render loop
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                mapCanvas.draw();
                controlPanel.updateYear(engine.getTimeManager().getFormattedYear());
                statsPanel.update();
            }
        };
        timer.start();
    }
}
