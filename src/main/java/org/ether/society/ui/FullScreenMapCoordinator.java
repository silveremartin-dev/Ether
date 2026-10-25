/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Coordinates switching between tabbed window layout and distraction-free full-screen map mode.
 * Handles key filters (Escape key), stage fullscreen events, and scene graph reparenting.
 */
public class FullScreenMapCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(FullScreenMapCoordinator.class);

    private final StackPane mainViewRoot;
    private final TabPane tabPane;
    private final BorderPane simulationRoot;
    private final StackPane mapStack;
    private final H3MapCanvas mapCanvas;

    private boolean isFullScreen = false;
    private EventHandler<KeyEvent> escapeKeyFilter;

    public FullScreenMapCoordinator(StackPane mainViewRoot, TabPane tabPane, BorderPane simulationRoot,
                                    StackPane mapStack, H3MapCanvas mapCanvas) {
        this.mainViewRoot = mainViewRoot;
        this.tabPane = tabPane;
        this.simulationRoot = simulationRoot;
        this.mapStack = mapStack;
        this.mapCanvas = mapCanvas;
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void toggleFullScreen() {
        if (isFullScreen) {
            exitFullScreen();
        } else {
            enterFullScreen();
        }
    }

    public void enterFullScreen() {
        if (isFullScreen || mapStack == null || simulationRoot == null) return;
        isFullScreen = true;

        // 1. Detach mapStack from simulationRoot
        simulationRoot.setCenter(null);

        // 2. Hide main TabPane so nothing else is visible on screen
        tabPane.setVisible(false);
        tabPane.setManaged(false);

        // 3. Add mapStack directly to MainView StackPane to fill 100% of the window
        if (!mainViewRoot.getChildren().contains(mapStack)) {
            mainViewRoot.getChildren().add(mapStack);
        }

        // 4. Set JavaFX Stage to FullScreen if scene is attached
        if (mainViewRoot.getScene() != null && mainViewRoot.getScene().getWindow() instanceof Stage stage) {
            stage.setFullScreen(true);

            // Listen to OS-level fullscreen exit (e.g. default Escape handling by JavaFX)
            stage.fullScreenProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> obs, Boolean oldVal, Boolean newVal) {
                    if (!newVal && isFullScreen) {
                        stage.fullScreenProperty().removeListener(this);
                        exitFullScreen();
                    }
                }
            });
        }

        // 5. Add key event filter for Escape key
        if (escapeKeyFilter == null) {
            escapeKeyFilter = event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    exitFullScreen();
                    event.consume();
                }
            };
        }
        if (mainViewRoot.getScene() != null) {
            mainViewRoot.getScene().addEventFilter(KeyEvent.KEY_PRESSED, escapeKeyFilter);
        }

        if (mapCanvas != null) {
            mapCanvas.draw();
        }
        logger.info("Entered Full Screen map view mode");
    }

    public void exitFullScreen() {
        if (!isFullScreen || mapStack == null || simulationRoot == null) return;
        isFullScreen = false;

        // 1. Remove Escape key filter
        if (escapeKeyFilter != null && mainViewRoot.getScene() != null) {
            mainViewRoot.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, escapeKeyFilter);
        }

        // 2. Exit OS stage fullscreen if still active
        if (mainViewRoot.getScene() != null && mainViewRoot.getScene().getWindow() instanceof Stage stage) {
            if (stage.isFullScreen()) {
                stage.setFullScreen(false);
            }
        }

        // 3. Remove mapStack from MainView StackPane root
        mainViewRoot.getChildren().remove(mapStack);

        // 4. Restore tabPane visibility
        tabPane.setManaged(true);
        tabPane.setVisible(true);

        // 5. Restore mapStack to simulationRoot center
        simulationRoot.setCenter(mapStack);

        if (mapCanvas != null) {
            mapCanvas.resetView();
        }

        Platform.runLater(() -> {
            if (mapCanvas != null) {
                mapCanvas.draw();
            }
        });

        logger.info("Exited Full Screen map view mode");
    }
}
