package org.ether.society;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.ui.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * High-fidelity JavaFX screenshot generator for Ether simulation engine.
 * Captures real UI screenshots for all 7 main tabs into docs/images/real_shots.
 */
public class CaptureRealShots extends Application {

    private MainView mainView;
    private H3SimulationEngine h3Engine;
    private Stage stage;
    private TabPane mainTabPane;
    private ScenarioSetupPanel setupPanel;
    private H3MapCanvas mapCanvas;
    private MiniMap miniMap;

    public static void main(String[] args) {
        System.err.println(">>> CaptureRealShots main() started <<<");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.err.println(">>> CaptureRealShots start() called <<<");
        this.stage = primaryStage;
        Path outDir = Paths.get("docs/images/real_shots");
        Files.createDirectories(outDir);

        Configuration config = ConfigurationLoader.loadDefault();
        h3Engine = new H3SimulationEngine(config);

        mapCanvas = new H3MapCanvas(1440, 900);
        miniMap = new MiniMap();
        PerformanceHUD hud = new PerformanceHUD();
        ControlPanel controlPanel = new ControlPanel(h3Engine);
        mainView = new MainView(h3Engine, controlPanel, mapCanvas, miniMap, hud);
        controlPanel.setMapCanvas(mapCanvas);
        controlPanel.setMiniMap(miniMap);

        // Reflectively access private UI fields from MainView
        Field tabPaneField = MainView.class.getDeclaredField("tabPane");
        tabPaneField.setAccessible(true);
        mainTabPane = (TabPane) tabPaneField.get(mainView);

        Field setupPanelField = MainView.class.getDeclaredField("setupPanel");
        setupPanelField.setAccessible(true);
        setupPanel = (ScenarioSetupPanel) setupPanelField.get(mainView);

        Scene scene = new Scene(mainView, 1440, 900);
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load styles.css: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        Theme.applyCurrentTheme(scene);
        primaryStage.setTitle("Ether Simulation Engine");
        primaryStage.show();

        Platform.runLater(() -> {
            new Thread(this::runCaptureWorkflow, "Screenshot-Capture-Thread").start();
        });
    }

    private void runCaptureWorkflow() {
        try {
            System.err.println("=== Starting Ether Real UI Screenshots Capture Workflow ===");

            // Enable all tabs
            runOnFx(() -> {
                System.err.println("Enabling all tabs...");
                for (Tab t : mainTabPane.getTabs()) {
                    t.setDisable(false);
                }
            });

            // 1. Tab 1: Planet Generator
            System.err.println("Capturing Tab 1: Planet Generator...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(0));
            sleep(1000);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab1_planet_generator.png");
                saveNodeSnapshot(mainView, "planet_generator_editor.png");
            });

            // 2. Tab 2: Resources & Ecology
            System.err.println("Capturing Tab 2: Resources & Ecology...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(1));
            sleep(1000);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab2_resources.png");
                saveNodeSnapshot(mainView, "resources_editor.png");
            });

            // 3. Tab 3: Scenario Setup
            System.err.println("Capturing Tab 3: Scenario Setup...");
            runOnFx(() -> {
                mainTabPane.getSelectionModel().select(2);
                try {
                    Field resComboField = ScenarioSetupPanel.class.getDeclaredField("h3ResolutionCombo");
                    resComboField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    ComboBox<Integer> resCombo = (ComboBox<Integer>) resComboField.get(setupPanel);
                    if (resCombo != null) {
                        resCombo.setValue(3);
                    }
                } catch (Exception e) {
                    System.err.println("Could not set resolution combo: " + e.getMessage());
                }
            });
            sleep(1000);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab3_scenario_setup.png");
                saveNodeSnapshot(mainView, "scenario_setup_editor.png");
            });

            // 4. Tab 4: Execution Context
            System.err.println("Capturing Tab 4: Execution Context...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(3));
            sleep(1000);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab4_execution_context.png");
                saveNodeSnapshot(mainView, "execution_context_panel.png");
            });

            // 5. Tab 5: Simulation View (Main Map View)
            System.err.println("Capturing Tab 5: Simulation (Main 3D/2D H3 Globe Canvas)...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(4));
            sleep(1500);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab5_simulation.png");
                saveNodeSnapshot(mainView, "h3_map_simulation_3d.png");
            });

            // Sub-view: God Mode Panel in Tab 5 Left Sidebar
            System.err.println("Capturing Tab 5 Sub-View: God Mode Panel...");
            runOnFx(() -> {
                try {
                    Tab simTab = mainTabPane.getTabs().get(4);
                    BorderPane root = (BorderPane) simTab.getContent();
                    HBox leftContainer = (HBox) root.getLeft();
                    TabPane leftSidebar = (TabPane) leftContainer.getChildren().get(0);
                    leftSidebar.getSelectionModel().select(2); // Select God Mode tab
                } catch (Exception e) {
                    System.err.println("Could not select God Mode sub-tab: " + e.getMessage());
                }
            });
            sleep(1000);
            runOnFx(() -> saveNodeSnapshot(mainView, "god_mode_panel.png"));

            // 6. Tab 6: Comparative Analytics
            System.err.println("Capturing Tab 6: Comparative Analytics...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(5));
            sleep(1000);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab6_comparative_analytics.png");
                saveNodeSnapshot(mainView, "analytics_dashboard.png");
            });

            // 7. Tab 7: Preferences
            System.err.println("Capturing Tab 7: Preferences...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(6));
            sleep(1000);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab7_preferences.png");
                saveNodeSnapshot(mainView, "preferences_panel.png");
            });

            System.err.println("=== SUCCESS: All real screenshots captured into docs/images/real_shots ===");
            sleep(500);

            runOnFx(() -> {
                stage.close();
                Platform.exit();
                System.exit(0);
            });

        } catch (Exception ex) {
            System.err.println("Fatal error in screenshot capture workflow:");
            ex.printStackTrace();
            Platform.exit();
            System.exit(1);
        }
    }

    private void runOnFx(Runnable action) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                System.err.println("Exception in runOnFx task: " + t);
                t.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                System.err.println("WARNING: runOnFx timed out after 10 seconds!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void saveNodeSnapshot(javafx.scene.Node node, String filename) {
        try {
            System.err.println("Taking snapshot for " + filename + "...");
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.valueOf("#1e293b"));
            WritableImage fxImage = node.snapshot(params, null);
            if (fxImage == null) {
                System.err.println("ERROR: fxImage is null for " + filename);
                return;
            }
            int w = (int) fxImage.getWidth();
            int h = (int) fxImage.getHeight();
            BufferedImage bImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = fxImage.getPixelReader();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    bImg.setRGB(x, y, reader.getArgb(x, y));
                }
            }
            File outFile = new File("docs/images/real_shots/" + filename);
            ImageIO.write(bImg, "png", outFile);
            System.err.println(" Saved screenshot: " + outFile.getAbsolutePath() + " (" + w + "x" + h + ")");
        } catch (Throwable t) {
            System.err.println("EXCEPTION saving snapshot " + filename + ": " + t.getMessage());
            t.printStackTrace();
        }
    }
}
