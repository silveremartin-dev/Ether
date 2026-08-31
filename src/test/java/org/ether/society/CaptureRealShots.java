package org.ether.society;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.ui.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * Headless/Automated JavaFX screenshot generator for Ether simulation engine.
 * Captures real UI screenshots for all 7 tabs and saves them into docs/images/real_shots.
 */
public class CaptureRealShots extends Application {

    private MainView mainView;
    private H3SimulationEngine h3Engine;
    private Stage stage;
    private TabPane mainTabPane;
    private ScenarioSetupPanel setupPanel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        Path outDir = Paths.get("docs/images/real_shots");
        Files.createDirectories(outDir);

        Configuration config = ConfigurationLoader.loadDefault();
        h3Engine = new H3SimulationEngine(config);

        H3MapCanvas mapCanvas = new H3MapCanvas(1440, 900);
        MiniMap miniMap = new MiniMap();
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

        new Thread(this::runCaptureWorkflow).start();
    }

    private void runCaptureWorkflow() {
        try {
            System.out.println("=== Starting Ether Real UI Screenshots Capture Workflow ===");

            // 1. Tab 1: Planet Generator
            System.out.println("Capturing Tab 1: Planet Generator...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(0));
            sleep(600);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab1_planet_generator.png");
                saveNodeSnapshot(mainView, "planet_generator_editor.png");
            });

            // 2. Tab 2: Resources & Ecology
            System.out.println("Capturing Tab 2: Resources & Ecology...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(1));
            sleep(600);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab2_resources.png");
                saveNodeSnapshot(mainView, "resources_editor.png");
            });

            // 3. Tab 3: Scenario Setup (Set Resolution to Level 3)
            System.out.println("Capturing Tab 3: Scenario Setup with Resolution 3...");
            runOnFx(() -> {
                mainTabPane.getSelectionModel().select(2);
                try {
                    Field resComboField = ScenarioSetupPanel.class.getDeclaredField("h3ResolutionCombo");
                    resComboField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    ComboBox<Integer> resCombo = (ComboBox<Integer>) resComboField.get(setupPanel);
                    if (resCombo != null) {
                        resCombo.setValue(3);
                        System.out.println("Set H3 resolution mesh to Level 3.");
                    }
                } catch (Exception e) {
                    System.err.println("Failed to set H3 resolution: " + e.getMessage());
                }
            });
            sleep(600);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab3_scenario_setup.png");
                saveNodeSnapshot(mainView, "scenario_setup_editor.png");
            });

            // Trigger Scenario Generation on Tab 3
            System.out.println("Launching scenario calculation on Tab 3...");
            runOnFx(() -> {
                try {
                    Method startMethod = ScenarioSetupPanel.class.getDeclaredMethod("startSimulationDeferred");
                    startMethod.setAccessible(true);
                    startMethod.invoke(setupPanel);
                } catch (Exception e) {
                    System.err.println("Failed to trigger scenario generation: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            // Wait until Tab 4 (Execution Context) is automatically selected upon scenario completion
            System.out.println("Waiting for scenario calculation to finish and transition to Tab 4...");
            long startWait = System.currentTimeMillis();
            while (true) {
                final int[] activeIndex = new int[1];
                runOnFx(() -> activeIndex[0] = mainTabPane.getSelectionModel().getSelectedIndex());
                if (activeIndex[0] == 3) {
                    System.out.println("Tab 4 (Execution Context) reached!");
                    break;
                }
                if (System.currentTimeMillis() - startWait > 45000) {
                    System.err.println("Timed out waiting for Tab 4 transition!");
                    break;
                }
                sleep(300);
            }

            // 4. Tab 4: Execution Context
            System.out.println("Capturing Tab 4: Execution Context...");
            sleep(800);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab4_execution_context.png");
                saveNodeSnapshot(mainView, "execution_context_panel.png");
            });

            // Launch Simulation from Tab 4 (Transitioning to Tab 5)
            System.out.println("Validating Tab 4 execution context and launching simulation...");
            runOnFx(() -> {
                try {
                    Method launchMethod = MainView.class.getDeclaredMethod("launchSimulationFromContext");
                    launchMethod.setAccessible(true);
                    launchMethod.invoke(mainView);
                } catch (Exception e) {
                    System.err.println("Failed to launch simulation from context: " + e.getMessage());
                }
            });

            // Wait until Tab 5 (Simulation View) is active
            startWait = System.currentTimeMillis();
            while (true) {
                final int[] activeIndex = new int[1];
                runOnFx(() -> activeIndex[0] = mainTabPane.getSelectionModel().getSelectedIndex());
                if (activeIndex[0] == 4) {
                    System.out.println("Tab 5 (Simulation) active!");
                    break;
                }
                if (System.currentTimeMillis() - startWait > 15000) {
                    break;
                }
                sleep(200);
            }

            // 5. Tab 5: Simulation View (Main Map View)
            System.out.println("Capturing Tab 5: Simulation (Main 3D/2D H3 Globe Canvas)...");
            sleep(1800); // Allow render pass
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab5_simulation.png");
                saveNodeSnapshot(mainView, "h3_map_simulation_3d.png");
            });

            // Sub-view: God Mode Panel in Tab 5 Left Sidebar
            System.out.println("Capturing Tab 5 Sub-View: God Mode Panel...");
            runOnFx(() -> {
                try {
                    Tab simTab = mainTabPane.getTabs().get(4);
                    BorderPane root = (BorderPane) simTab.getContent();
                    HBox leftContainer = (HBox) root.getLeft();
                    TabPane leftSidebar = (TabPane) leftContainer.getChildren().get(0);
                    leftSidebar.getSelectionModel().select(2); // Select God Mode tab (index 2)
                } catch (Exception e) {
                    System.err.println("Could not select God Mode sub-tab: " + e.getMessage());
                }
            });
            sleep(600);
            runOnFx(() -> saveNodeSnapshot(mainView, "god_mode_panel.png"));

            // 6. Tab 6: Comparative Analytics
            System.out.println("Capturing Tab 6: Comparative Analytics...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(5));
            sleep(800);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab6_comparative_analytics.png");
                saveNodeSnapshot(mainView, "analytics_dashboard.png");
            });

            // 7. Tab 7: Preferences
            System.out.println("Capturing Tab 7: Preferences...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(6));
            sleep(600);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab7_preferences.png");
                saveNodeSnapshot(mainView, "preferences_panel.png");
            });

            System.out.println("=== All real screenshots captured successfully! ===");
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
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await();
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
        WritableImage fxImage = node.snapshot(null, null);
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
        try {
            ImageIO.write(bImg, "png", outFile);
            System.out.println(" Saved: " + outFile.getAbsolutePath() + " (" + w + "x" + h + ")");
        } catch (Exception e) {
            System.err.println("Failed to write PNG file " + filename + ": " + e.getMessage());
        }
    }
}
