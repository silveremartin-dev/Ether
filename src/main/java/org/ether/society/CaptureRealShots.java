/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.ether.society.config.Configuration;
import org.ether.society.config.ConfigurationLoader;
import org.ether.society.core.H3SimulationEngine;
import org.ether.society.i18n.I18n;
import org.ether.society.i18n.Language;
import org.ether.society.model.Scenario;
import org.ether.society.ui.*;
import org.ether.society.ui.scenario.ScenarioPresetRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <h1>Capture Real UI Screenshots</h1>
 * <p>
 * High-fidelity JavaFX screenshot generator for Ether simulation engine.
 * Captures real UI screenshots for all 7 main tabs in English across target documentation
 * directories, including multi-layer 3D globe projections and 2D planar map layouts.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.3
 */
public class CaptureRealShots extends Application {

    private MainView mainView;
    private H3SimulationEngine h3Engine;
    private Stage stage;
    private TabPane mainTabPane;
    private ScenarioSetupPanel setupPanel;
    private ExecutionContextPanel executionContextPanel;
    private H3MapCanvas mapCanvas;
    private MiniMap miniMap;
    private ControlPanel controlPanel;

    private static final List<String> TARGET_DIRS = List.of(
            "docs/images/screenshots",
            "dist/Ether-v1.0.0-beta.3-windows-x64/docs/images/screenshots",
            "dist/Ether-v1.0.0-beta.3-linux-x64/docs/images/screenshots",
            "dist/Ether-v1.0.0-beta.3-macos-universal/docs/images/screenshots",
            "dist/Ether-v1.0.0-beta.3-standalone/docs/images/screenshots"
    );

    /**
     * Main entry point for standalone screenshot execution.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        System.err.println(">>> CaptureRealShots main() started <<<");
        Locale.setDefault(Locale.ENGLISH);
        I18n.setLanguage(Language.ENGLISH);
        launch(args);
    }

    /**
     * JavaFX application initialization and stage lifecycle startup.
     *
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.err.println(">>> CaptureRealShots start() called <<<");
        Locale.setDefault(Locale.ENGLISH);
        I18n.setLanguage(Language.ENGLISH);

        this.stage = primaryStage;
        for (String dirPath : TARGET_DIRS) {
            File d = new File(dirPath);
            if (d.exists() || dirPath.equals("docs/images/screenshots")) {
                Files.createDirectories(Paths.get(dirPath));
            }
        }

        Configuration config = ConfigurationLoader.loadDefault();
        h3Engine = new H3SimulationEngine(config);

        mapCanvas = new H3MapCanvas(1440, 900);
        miniMap = new MiniMap();
        PerformanceHUD hud = new PerformanceHUD();
        controlPanel = new ControlPanel(h3Engine);
        mainView = new MainView(h3Engine, controlPanel, mapCanvas, miniMap, hud);
        controlPanel.setMapCanvas(mapCanvas);
        controlPanel.setMiniMap(miniMap);

        Field tabPaneField = MainView.class.getDeclaredField("tabPane");
        tabPaneField.setAccessible(true);
        mainTabPane = (TabPane) tabPaneField.get(mainView);

        Field setupPanelField = MainView.class.getDeclaredField("setupPanel");
        setupPanelField.setAccessible(true);
        setupPanel = (ScenarioSetupPanel) setupPanelField.get(mainView);

        Field execField = MainView.class.getDeclaredField("executionContextPanel");
        execField.setAccessible(true);
        executionContextPanel = (ExecutionContextPanel) execField.get(mainView);

        Scene scene = new Scene(mainView, 1440, 900);
        try {
            String css = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load styles.css: " + e.getMessage());
        }

        primaryStage.setScene(scene);
        Theme.applyCurrentTheme(scene);
        primaryStage.setTitle("Ether - Cliodynamic & Thermodynamic Simulation Studio");
        primaryStage.setWidth(1440);
        primaryStage.setHeight(900);
        primaryStage.show();

        Platform.runLater(() -> new Thread(this::runCaptureWorkflow, "Screenshot-Capture-Thread").start());
    }

    private void runCaptureWorkflow() {
        try {
            System.err.println("=== Starting Ether Real UI Screenshots Capture Workflow (Language: EN) ===");

            runOnFx(() -> {
                System.err.println("Enabling all tabs for sequential snapshot inspection...");
                for (Tab t : mainTabPane.getTabs()) {
                    t.setDisable(false);
                }
                mainView.updateTabTitles();
                if (setupPanel != null) {
                    setupPanel.markClean();
                }
                if (executionContextPanel != null) {
                    executionContextPanel.setRenderingMode(ExecutionContextPanel.RenderingMode.GUI);
                }
            });

            // 1. Tab 0: Planet Generator
            System.err.println("[1/7] Capturing Tab 0: Planet Generator...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(0));
            sleep(1500);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab1_planet_generator.png");
                saveNodeSnapshot(mainView, "planet_generator_editor.png");
            });

            // 2. Tab 1: Resources & Ecology
            System.err.println("[2/7] Capturing Tab 1: Resources & Geology...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(1));
            sleep(1500);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab2_resources.png");
                saveNodeSnapshot(mainView, "resources_editor.png");
            });

            // 3. Tab 2: Scenario Setup
            System.err.println("[3/7] Capturing Tab 2: Scenario Setup...");
            runOnFx(() -> {
                mainTabPane.getSelectionModel().select(2);
                try {
                    Field presetMgrField = ScenarioSetupPanel.class.getDeclaredField("presetManager");
                    presetMgrField.setAccessible(true);
                    ScenarioPresetRepository presetMgr = (ScenarioPresetRepository) presetMgrField.get(setupPanel);
                    Scenario targetSc = null;
                    for (Scenario s : presetMgr.getBuiltInScenarios()) {
                        if (s != null && (s.getStartDateYear() == 1950 || (s.getName() != null && s.getName().contains("Great Acceleration")))) {
                            targetSc = s;
                            break;
                        }
                    }
                    if (targetSc != null) {
                        System.err.println("Applying scenario directly: " + targetSc.getName());
                        setupPanel.applyScenarioToUI(targetSc);
                        presetMgr.markClean(targetSc);
                        setupPanel.markClean();
                    }

                    Field resComboField = ScenarioSetupPanel.class.getDeclaredField("h3ResolutionCombo");
                    resComboField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    ComboBox<Integer> resCombo = (ComboBox<Integer>) resComboField.get(setupPanel);
                    if (resCombo != null) {
                        resCombo.setValue(4);
                    }
                    // Select cold start to ensure fresh computation with full layers
                    Field snapField = ScenarioSetupPanel.class.getDeclaredField("snapshotInspector");
                    snapField.setAccessible(true);
                    Object inspector = snapField.get(setupPanel);
                    Field coldField = inspector.getClass().getDeclaredField("radioColdStart");
                    coldField.setAccessible(true);
                    RadioButton coldRadio = (RadioButton) coldField.get(inspector);
                    if (coldRadio != null) {
                        coldRadio.setSelected(true);
                    }
                } catch (Exception e) {
                    System.err.println("Could not configure scenario setup options: " + e.getMessage());
                }
                setupPanel.markClean();
            });
            sleep(1500);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab3_scenario_setup.png");
                saveNodeSnapshot(mainView, "scenario_setup_editor.png");
            });

            // 4. Tab 5: Execution Context
            System.err.println("[4/7] Capturing Tab 5: Execution Context...");
            runOnFx(() -> {
                if (setupPanel != null) {
                    setupPanel.markClean();
                }
                mainTabPane.getSelectionModel().select(5);
                if (executionContextPanel != null) {
                    executionContextPanel.setRenderingMode(ExecutionContextPanel.RenderingMode.GUI);
                }
            });
            sleep(1500);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab4_execution_context.png");
                saveNodeSnapshot(mainView, "execution_context_panel.png");
            });

            // 5. Trigger Scenario Generation from Tab 2
            System.err.println("Launching scenario calculation from Tab 2 (forcing H3 Resolution 4 & cold start)...");
            runOnFx(() -> {
                mainTabPane.getSelectionModel().select(2);
                try {
                    Field resComboField = ScenarioSetupPanel.class.getDeclaredField("h3ResolutionCombo");
                    resComboField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    ComboBox<Integer> resCombo = (ComboBox<Integer>) resComboField.get(setupPanel);
                    if (resCombo != null) {
                        resCombo.setValue(4);
                    }
                } catch (Exception ex) {
                    System.err.println("Could not set H3 resolution combo: " + ex.getMessage());
                }
                setupPanel.markClean();
                setupPanel.startSimulationDeferred();
            });

            // Wait until Tab 3 (Simulation View) is selected upon completion and cells are populated
            System.err.println("Waiting for scenario calculation to finish and switch to Simulation Tab (Index 3)...");
            long startWait = System.currentTimeMillis();
            while (true) {
                final int[] activeIndex = new int[1];
                final int[] cellCount = new int[1];
                runOnFx(() -> {
                    activeIndex[0] = mainTabPane.getSelectionModel().getSelectedIndex();
                    cellCount[0] = (h3Engine.getCells() != null) ? h3Engine.getCells().size() : 0;
                });
                if (activeIndex[0] == 3 && cellCount[0] > 0) {
                    System.err.println("Simulation Tab reached with " + cellCount[0] + " cells!");
                    break;
                }
                if (System.currentTimeMillis() - startWait > 300000) {
                    System.err.println("Timed out waiting for Simulation Tab (300s). Forcing selection of Tab 3.");
                    runOnFx(() -> mainTabPane.getSelectionModel().select(3));
                    break;
                }
                sleep(1000);
            }

            // Ensure rendering mode is GUI so mapCanvas is visible and managed
            runOnFx(() -> {
                if (executionContextPanel != null) {
                    executionContextPanel.setRenderingMode(ExecutionContextPanel.RenderingMode.GUI);
                }
                mapCanvas.setVisible(true);
                mapCanvas.setTabVisible(true);
                mapCanvas.setShowHillshading(true);
                mapCanvas.setShowSolarTerminator(true);
                try {
                    h3Engine.stepForward(1);
                    mapCanvas.setWorldBuffer(h3Engine.getWorldBuffer());
                    mapCanvas.setCells(h3Engine.getCells());
                } catch (Exception e) {
                    System.err.println("Non-fatal step exception: " + e.getMessage());
                }
            });
            sleep(2000);
            sleep(2000);

            // 5a. Simulation Tab - 3D Globe with Population Density Layer
            System.err.println("[5a/7] Capturing Tab 3: Simulation - 3D Globe with Population Density Layer...");
            runOnFx(() -> {
                try {
                    Tab simTab = mainTabPane.getTabs().get(3);
                    BorderPane root = (BorderPane) simTab.getContent();
                    TabPane leftSidebar = (TabPane) root.getLeft();
                    leftSidebar.getSelectionModel().select(0); // 🎛️ 3D Render & Controls
                } catch (Exception ex) {
                    System.err.println("Could not select 3D render tab: " + ex.getMessage());
                }

                mapCanvas.setViewMode(ViewMode.VIEW_3D);
                mapCanvas.setSmoothMap(false);
                mapCanvas.setVerticalExaggeration(25.0);
                mapCanvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.POPULATION));
                mapCanvas.setShowHexGrid(true);
                mapCanvas.setShowContours(false);
                mapCanvas.setShowHillshading(true);
                mapCanvas.setShowSolarTerminator(true);
                mapCanvas.setCenterView(28.0, 35.0);
                mapCanvas.setZoomFactor(0.98);
                controlPanel.updateActiveLayerUI();
                mapCanvas.draw();
            });
            sleep(2500);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "h3_map_simulation_3d_density.png");
                saveNodeSnapshot(mainView, "h3_map_simulation_3d.png");
                saveNodeSnapshot(mainView, "tab5_simulation.png");
            });

            // 5b. Simulation Tab - 3D Globe with Biomes & Relief Layer
            System.err.println("[5b/7] Capturing Tab 3: Simulation - 3D Globe with Biomes & Relief Layer...");
            runOnFx(() -> {
                mapCanvas.setViewMode(ViewMode.VIEW_3D);
                mapCanvas.setSmoothMap(false);
                mapCanvas.setVerticalExaggeration(30.0);
                mapCanvas.setActiveDisplayModes(List.of(DisplayMode.BIOME));
                mapCanvas.setShowHexGrid(true);
                mapCanvas.setShowContours(true);
                mapCanvas.setShowHillshading(true);
                mapCanvas.setShowSolarTerminator(true);
                mapCanvas.setCenterView(35.0, 60.0);
                mapCanvas.setZoomFactor(0.98);
                controlPanel.updateActiveLayerUI();
                mapCanvas.draw();
            });
            sleep(2000);
            runOnFx(() -> saveNodeSnapshot(mainView, "h3_map_simulation_3d_biomes.png"));

            // 5c. Simulation Tab - 3D Globe with Climate & Temperature Layer
            System.err.println("[5c/7] Capturing Tab 3: Simulation - 3D Globe with Climate / Temperature Layer...");
            runOnFx(() -> {
                mapCanvas.setViewMode(ViewMode.VIEW_3D);
                mapCanvas.setSmoothMap(false);
                mapCanvas.setVerticalExaggeration(20.0);
                mapCanvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.TEMPERATURE));
                mapCanvas.setShowHexGrid(true);
                mapCanvas.setShowContours(false);
                mapCanvas.setShowHillshading(true);
                mapCanvas.setShowSolarTerminator(true);
                mapCanvas.setCenterView(20.0, 15.0);
                mapCanvas.setZoomFactor(0.98);
                controlPanel.updateActiveLayerUI();
                mapCanvas.draw();
            });
            sleep(2000);
            runOnFx(() -> saveNodeSnapshot(mainView, "h3_map_simulation_3d_climate.png"));

            // 5d. Simulation Tab - 2D Cylindrical Map View with Population Density & Contours
            System.err.println("[5d/7] Capturing Tab 3: Simulation - 2D Map View (Flat Cylindrical Projection)...");
            runOnFx(() -> {
                mapCanvas.setViewMode(ViewMode.VIEW_2D);
                mapCanvas.resetView();
                mapCanvas.setActiveDisplayModes(List.of(DisplayMode.BIOME, DisplayMode.POPULATION));
                mapCanvas.setShowHexGrid(false);
                mapCanvas.setShowContours(false);
                mapCanvas.setShowHillshading(true);
                mapCanvas.setShowSolarTerminator(false);
                controlPanel.updateActiveLayerUI();
                mapCanvas.draw();
            });
            sleep(2500);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "h3_map_simulation_2d.png");
                saveNodeSnapshot(mainView, "tab5_simulation_2d.png");
            });

            // 5e. Simulation Tab - Sub-view: God Mode / Perturbation Console
            System.err.println("[5e/7] Capturing Tab 3: Simulation - God Mode & Perturbation Console...");
            runOnFx(() -> {
                try {
                    Tab simTab = mainTabPane.getTabs().get(3);
                    BorderPane root = (BorderPane) simTab.getContent();
                    TabPane leftSidebar = (TabPane) root.getLeft();
                    leftSidebar.getSelectionModel().select(2); // Select God Mode / Perturbation tab
                } catch (Exception e) {
                    System.err.println("Could not select God Mode sub-tab: " + e.getMessage());
                }
            });
            sleep(1500);
            runOnFx(() -> saveNodeSnapshot(mainView, "god_mode_panel.png"));

            // 6. Tab 4: Comparative Analytics
            System.err.println("[6/7] Capturing Tab 4: Comparative Analytics...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(4));
            sleep(1500);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab6_comparative_analytics.png");
                saveNodeSnapshot(mainView, "analytics_dashboard.png");
            });

            // 7. Tab 6: Preferences
            System.err.println("[7/7] Capturing Tab 6: Preferences...");
            runOnFx(() -> mainTabPane.getSelectionModel().select(6));
            sleep(1500);
            runOnFx(() -> {
                saveNodeSnapshot(mainView, "tab7_preferences.png");
                saveNodeSnapshot(mainView, "preferences_panel.png");
            });

            System.err.println("=== SUCCESS: All real screenshots captured in English across target directories! ===");
            sleep(1000);

            Platform.runLater(stage::close);
            sleep(500);
            System.exit(0);

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
            if (!latch.await(120, TimeUnit.SECONDS)) {
                System.err.println("WARNING: runOnFx timed out after 120 seconds!");
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

            for (String dir : TARGET_DIRS) {
                File dirFile = new File(dir);
                if (dirFile.exists() || dir.equals("docs/images/screenshots")) {
                    if (!dirFile.exists()) {
                        dirFile.mkdirs();
                    }
                    File outFile = new File(dirFile, filename);
                    ImageIO.write(bImg, "png", outFile);
                    System.err.println(" Saved: " + outFile.getAbsolutePath() + " (" + w + "x" + h + ")");
                }
            }
        } catch (Throwable t) {
            System.err.println("EXCEPTION saving snapshot " + filename + ": " + t.getMessage());
            t.printStackTrace();
        }
    }
}
