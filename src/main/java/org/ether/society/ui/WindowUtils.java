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

            // Set OS Taskbar icon once for the process (macOS Dock / AWT Taskbar)
            if (!taskbarIconSet && java.awt.Taskbar.isTaskbarSupported()) {
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
                    } catch (Exception ex) {
                        logger.debug("Could not set AWT Taskbar icon", ex);
                    }
                }
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
            String lightCss = WindowUtils.class.getResource("/css/light.css") != null ? 
                WindowUtils.class.getResource("/css/light.css").toExternalForm() : null;
            String darkCss = WindowUtils.class.getResource("/css/index.css") != null ? 
                WindowUtils.class.getResource("/css/index.css").toExternalForm() : null;

            if (darkCss != null && !pane.getStylesheets().contains(darkCss)) {
                pane.getStylesheets().add(darkCss);
            }
            if (lightCss != null && !pane.getStylesheets().contains(lightCss)) {
                pane.getStylesheets().add(lightCss);
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
}

