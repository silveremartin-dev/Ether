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

/**
 * Utility for window management and icon configuration.
 */
public class WindowUtils {
    private static final Logger logger = LoggerFactory.getLogger(WindowUtils.class);
    private static Image cachedIcon = null;
    private static boolean taskbarIconSet = false;

    /**
     * Applies icon.png to the JavaFX Stage and Windows OS Taskbar.
     *
     * @param stage target Stage window
     */
    public static void applyWindowIcon(Stage stage) {
        if (stage == null) return;

        try {
            if (cachedIcon == null) {
                InputStream iconStream = WindowUtils.class.getResourceAsStream("/icons/icon.png");
                if (iconStream != null) {
                    cachedIcon = new Image(iconStream);
                }
            }

            if (cachedIcon != null && !cachedIcon.isError()) {
                stage.getIcons().clear();
                stage.getIcons().add(cachedIcon);
            }

            // Set OS Taskbar icon once for the process (Windows / macOS)
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
}
