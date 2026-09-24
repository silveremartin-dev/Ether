/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for window management and icon configuration.
 */
public class WindowUtils {
    private static final Logger logger = LoggerFactory.getLogger(WindowUtils.class);
    private static final List<Image> cachedIcons = new ArrayList<>();
    private static boolean taskbarIconSet = false;

    /**
     * Applies icon.png to the JavaFX Stage and Windows OS Taskbar.
     *
     * @param stage target Stage window
     */
    public static void applyWindowIcon(Stage stage) {
        if (stage == null) return;

        try {
            // Set Windows AppUserModelID so taskbar groups and renders application icon correctly
            setWindowsAppUserModelID("Ether.SocietySimulation.App");

            if (cachedIcons.isEmpty()) {
                try (InputStream iconStream = WindowUtils.class.getResourceAsStream("/icons/icon.png")) {
                    if (iconStream != null) {
                        Image mainImg = new Image(iconStream);
                        if (!mainImg.isError()) {
                            cachedIcons.add(mainImg);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not read main icon.png", e);
                }
            }

            if (!cachedIcons.isEmpty()) {
                stage.getIcons().setAll(cachedIcons);
            }

            // Set OS Taskbar icon once for the process (macOS Dock / AWT Taskbar) asynchronously to avoid UI freeze
            if (!taskbarIconSet) {
                new Thread(() -> {
                    try {
                        if (java.awt.Taskbar.isTaskbarSupported()) {
                            var taskbar = java.awt.Taskbar.getTaskbar();
                            if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                                try (InputStream awtStream = WindowUtils.class.getResourceAsStream("/icons/icon.png")) {
                                    if (awtStream != null) {
                                        java.awt.Image awtImage = javax.imageio.ImageIO.read(awtStream);
                                        if (awtImage != null) {
                                            taskbar.setIconImage(awtImage);
                                            taskbarIconSet = true;
                                            logger.info("AWT Taskbar icon updated successfully.");
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        logger.debug("Could not set AWT Taskbar icon", ex);
                    }
                }, "AWT-Taskbar-Init").start();
            }
        } catch (Exception e) {
            logger.warn("Could not load application icon for stage", e);
        }
    }

    /**
     * Applies icon.png to a JavaFX Dialog or Alert window and styles its dialog pane with theme CSS.
     *
     * @param dialog target Dialog window
     */
    public static void applyWindowIcon(javafx.scene.control.Dialog<?> dialog) {
        if (dialog == null) return;
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) dialog.getDialogPane().getScene().getWindow();
            applyWindowIcon(stage);
            styleDialogPane(dialog.getDialogPane());
        } catch (Exception e) {
            logger.debug("Could not apply icon/style to dialog", e);
        }
    }

    /**
     * Styles a DialogPane with application CSS stylesheets so popup modals respect the active UI theme.
     *
     * @param pane target DialogPane
     */
    public static void styleDialogPane(javafx.scene.control.DialogPane pane) {
        if (pane == null) return;
        try {
            String activeCss = Theme.getCurrentTheme().getStylesheetPath();
            var activeRes = WindowUtils.class.getResource(activeCss);
            if (activeRes != null) {
                String cssForm = activeRes.toExternalForm();
                pane.getStylesheets().clear();
                pane.getStylesheets().add(cssForm);
            }
        } catch (Exception e) {
            logger.debug("Could not style dialog pane", e);
        }
    }

    /**
     * Sets the Windows AppUserModelID via Win32 Shell32 API if running on Windows OS.
     * This prevents Windows Taskbar from falling back to generic javaw.exe icon or delaying icon rendering.
     */
    private static void setWindowsAppUserModelID(String appId) {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            try {
                // JNA Shell32 fallback using Win32 API via reflection if JNA is available, or process execution
                Class<?> shell32Class = Class.forName("com.sun.jna.platform.win32.Shell32");
                Object instance = shell32Class.getField("INSTANCE").get(null);
                java.lang.reflect.Method method = shell32Class.getMethod("SetCurrentProcessExplicitAppUserModelID", String.class);
                method.invoke(instance, appId);
                logger.info("SetCurrentProcessExplicitAppUserModelID set successfully to {}", appId);
            } catch (ClassNotFoundException e) {
                // JNA not on classpath, AppUserModelID set via JavaFX multi-resolution icons
                logger.debug("JNA not present, relying on JavaFX multi-resolution icons for Windows taskbar.");
            } catch (Exception e) {
                logger.debug("Could not set AppUserModelID: {}", e.getMessage());
            }
        }
    }

    /**
     * Displays a scrollable information dialog with styled scroll pane and pinned OK button.
     * Prevents tall dialogs from overflowing the screen and hiding action buttons.
     */
    public static void showScrollableInfoDialog(String title, String header, String contentText) {
        javafx.scene.control.Alert dialog = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        javafx.scene.control.Label textLabel = new javafx.scene.control.Label(contentText);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 13px; -fx-line-spacing: 3px; -fx-padding: 10px;");

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(textLabel);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(660);
        scrollPane.setPrefViewportHeight(420);
        scrollPane.setMaxHeight(520);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.setResizable(true);
        applyWindowIcon(dialog);
        dialog.showAndWait();
    }

    /**
     * Sets or resets the busy / wait cursor on a JavaFX Node or its containing Scene.
     *
     * @param node target node
     * @param busy true to display WAIT cursor, false for DEFAULT
     */
    public static void setBusyCursor(javafx.scene.Node node, boolean busy) {
        if (node == null) return;
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.Scene scene = node.getScene();
                if (scene != null) {
                    scene.setCursor(busy ? javafx.scene.Cursor.WAIT : javafx.scene.Cursor.DEFAULT);
                } else {
                    node.setCursor(busy ? javafx.scene.Cursor.WAIT : javafx.scene.Cursor.DEFAULT);
                }
            } catch (Exception ignored) {}
        });
    }

    /**
     * Sets or resets the busy / wait cursor on a JavaFX Scene.
     *
     * @param scene target scene
     * @param busy true to display WAIT cursor, false for DEFAULT
     */
    public static void setBusyCursor(javafx.scene.Scene scene, boolean busy) {
        if (scene == null) return;
        javafx.application.Platform.runLater(() -> {
            try {
                scene.setCursor(busy ? javafx.scene.Cursor.WAIT : javafx.scene.Cursor.DEFAULT);
            } catch (Exception ignored) {}
        });
    }
}

