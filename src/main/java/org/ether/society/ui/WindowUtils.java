*
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
 * <h1>Window Utils</h1>
 * <p>
 * User interface component and visualization panel for the Ether simulation platform.<br>
 * Provides interactive rendering, real-time spatial heatmaps, parameter controls, and multi-language localized analytics.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public class WindowUtils {
    private static final Logger logger = LoggerFactory.getLogger(WindowUtils.class);
    private static final List<Image> cachedIcons = new ArrayList<>();
    /** Internal state: taskbar icon set (boolean). */
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

    /**
     * Computes an optimal, human-friendly tick unit for a {@link javafx.scene.chart.NumberAxis} given a numeric span.
     * <p>
     * Generates approximately 8 to 12 major divisions based on 1-2-5-10 multiples of powers of 10:
     * \[\text{unit} = c \times 10^{\lfloor \log_{10}(\text{raw}) \rfloor}, \quad c \in \{1, 2, 5, 10\}\]
     * Guarantees that major tick mark allocations remain well below the JavaFX internal limit of 2,000 marks,
     * preventing UI thread lockups and "Warning we tried to create more than 2000 major tick marks" warnings.
     * </p>
     *
     * @param range the span between lower and upper bounds of the axis (in years, units, etc.)
     * @return a clean, optimal tick unit greater than or equal to 1.0
     */
    public static double computeNiceTickUnit(double range) {
        if (range <= 0.0 || Double.isNaN(range) || Double.isInfinite(range)) {
            return 10.0;
        }
        double raw = range / 10.0;
        double exponent = Math.floor(Math.log10(raw));
        double fraction = raw / Math.pow(10.0, exponent);
        double niceFraction;
        if (fraction <= 1.5) {
            niceFraction = 1.0;
        } else if (fraction <= 3.0) {
            niceFraction = 2.0;
        } else if (fraction <= 7.0) {
            niceFraction = 5.0;
        } else {
            niceFraction = 10.0;
        }
        return Math.max(1.0, niceFraction * Math.pow(10.0, exponent));
    }

    /**
     * Safely prompts the user for a destination file path using JavaFX {@link javafx.stage.FileChooser},
     * with graceful, fail-safe degradation to {@link javax.swing.JFileChooser} or local default directory
     * in the event of a {@link NoClassDefFoundError}, {@link LinkageError}, or headless environment error.
     *
     * @param parentWindow the parent JavaFX window
     * @param dialogTitle dialog window title
     * @param defaultFileName suggested filename
     * @param extensionDescription description of the extension filter (e.g. "Markdown (*.md)")
     * @param extensionFilter filter extension pattern (e.g. "*.md")
     * @return the selected destination file, or a fallback output file, or {@code null} if cancelled by user
     */
    public static java.io.File showSaveFileDialog(
            javafx.stage.Window parentWindow,
            String dialogTitle,
            String defaultFileName,
            String extensionDescription,
            String extensionFilter) {
        // Step 1: Attempt native JavaFX FileChooser
        try {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            if (dialogTitle != null && !dialogTitle.isBlank()) {
                chooser.setTitle(dialogTitle);
            }
            if (defaultFileName != null && !defaultFileName.isBlank()) {
                chooser.setInitialFileName(defaultFileName);
            }
            if (extensionFilter != null && !extensionFilter.isBlank()) {
                String desc = (extensionDescription != null && !extensionDescription.isBlank())
                        ? extensionDescription : extensionFilter;
                chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(desc, extensionFilter));
            }
            return chooser.showSaveDialog(parentWindow);
        } catch (Throwable t) {
            logger.warn("JavaFX FileChooser invocation failed ({}); initiating Swing JFileChooser fallback", t.toString());
        }

        // Step 2: Fallback to Swing JFileChooser on EDT if display environment exists
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            try {
                final java.io.File[] result = new java.io.File[1];
                final boolean[] approved = new boolean[1];
                Runnable showDialog = () -> {
                    javax.swing.JFileChooser jfc = new javax.swing.JFileChooser();
                    if (dialogTitle != null && !dialogTitle.isBlank()) {
                        jfc.setDialogTitle(dialogTitle);
                    }
                    if (defaultFileName != null && !defaultFileName.isBlank()) {
                        jfc.setSelectedFile(new java.io.File(defaultFileName));
                    }
                    if (extensionFilter != null && extensionFilter.contains("*.")) {
                        String ext = extensionFilter.substring(extensionFilter.indexOf("*.") + 2).replace("*", "").trim();
                        if (!ext.isBlank()) {
                            String desc = (extensionDescription != null && !extensionDescription.isBlank())
                                    ? extensionDescription : ext;
                            jfc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(desc, ext));
                        }
                    }
                    int ret = jfc.showSaveDialog(null);
                    if (ret == javax.swing.JFileChooser.APPROVE_OPTION) {
                        result[0] = jfc.getSelectedFile();
                        approved[0] = true;
                    }
                };

                if (java.awt.EventQueue.isDispatchThread()) {
                    showDialog.run();
                } else {
                    java.awt.EventQueue.invokeAndWait(showDialog);
                }
                if (approved[0]) {
                    return result[0];
                } else {
                    return null; // Explicitly cancelled by user
                }
            } catch (Throwable t2) {
                logger.warn("Swing JFileChooser fallback encountered an error ({}); resolving to local default export", t2.toString());
            }
        }

        // Step 3: Emergency fallback to ./exports/
        java.io.File exportDir = new java.io.File("exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        String safeName = (defaultFileName != null && !defaultFileName.isBlank())
                ? defaultFileName
                : "export_" + System.currentTimeMillis() + (extensionFilter != null && extensionFilter.contains(".") ? extensionFilter.substring(extensionFilter.lastIndexOf('.')) : ".txt");
        return new java.io.File(exportDir, safeName);
    }

    /**
     * Safely prompts the user to select an existing file using JavaFX {@link javafx.stage.FileChooser},
     * with graceful, fail-safe degradation to {@link javax.swing.JFileChooser} in the event of a
     * {@link NoClassDefFoundError}, {@link LinkageError}, or headless environment error.
     *
     * @param parentWindow the parent JavaFX window
     * @param dialogTitle dialog window title
     * @param extensionDescription description of the extension filter
     * @param extensionFilters array of filter extension patterns (e.g. "*.json", "*.ether")
     * @return the selected file, or {@code null} if cancelled
     */
    public static java.io.File showOpenFileDialog(
            javafx.stage.Window parentWindow,
            String dialogTitle,
            String extensionDescription,
            String... extensionFilters) {
        // Step 1: Attempt native JavaFX FileChooser
        try {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            if (dialogTitle != null && !dialogTitle.isBlank()) {
                chooser.setTitle(dialogTitle);
            }
            if (extensionFilters != null && extensionFilters.length > 0) {
                String desc = (extensionDescription != null && !extensionDescription.isBlank())
                        ? extensionDescription : "Files";
                chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(desc, extensionFilters));
            }
            return chooser.showOpenDialog(parentWindow);
        } catch (Throwable t) {
            logger.warn("JavaFX FileChooser open dialog failed ({}); falling back to Swing JFileChooser", t.toString());
        }

        // Step 2: Fallback to Swing JFileChooser on EDT if display environment exists
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            try {
                final java.io.File[] result = new java.io.File[1];
                final boolean[] approved = new boolean[1];
                Runnable showDialog = () -> {
                    javax.swing.JFileChooser jfc = new javax.swing.JFileChooser();
                    if (dialogTitle != null && !dialogTitle.isBlank()) {
                        jfc.setDialogTitle(dialogTitle);
                    }
                    if (extensionFilters != null && extensionFilters.length > 0) {
                        List<String> rawExts = new ArrayList<>();
                        for (String f : extensionFilters) {
                            if (f != null && f.contains("*.")) {
                                rawExts.add(f.substring(f.indexOf("*.") + 2).replace("*", "").trim());
                            }
                        }
                        if (!rawExts.isEmpty()) {
                            String desc = (extensionDescription != null && !extensionDescription.isBlank())
                                    ? extensionDescription : "Allowed Files";
                            jfc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(desc, rawExts.toArray(new String[0])));
                        }
                    }
                    int ret = jfc.showOpenDialog(null);
                    if (ret == javax.swing.JFileChooser.APPROVE_OPTION) {
                        result[0] = jfc.getSelectedFile();
                        approved[0] = true;
                    }
                };

                if (java.awt.EventQueue.isDispatchThread()) {
                    showDialog.run();
                } else {
                    java.awt.EventQueue.invokeAndWait(showDialog);
                }
                return approved[0] ? result[0] : null;
            } catch (Throwable t2) {
                logger.warn("Swing JFileChooser open dialog fallback failed ({})", t2.toString());
                return null;
            }
        }
        return null;
    }

    /**
     * Prompts the user to save a JavaFX Image to a raster file (PNG or JPEG) and writes it out.
     *
     * @param parentWindow the parent window/stage
     * @param image the image to export
     * @param defaultFileName the suggested filename (e.g. "density_map.png")
     * @param dialogTitle the dialog title
     */
    public static void exportImageWithChooser(javafx.stage.Window parentWindow, Image image, String defaultFileName, String dialogTitle) {
        if (image == null) return;
        java.io.File file = showSaveFileDialog(parentWindow, dialogTitle, defaultFileName, "Portable Network Graphics (*.png)", "*.png");
        if (file != null) {
            try {
                int w = (int) image.getWidth();
                int h = (int) image.getHeight();
                java.awt.image.BufferedImage bImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                javafx.scene.image.PixelReader pr = image.getPixelReader();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        bImg.setRGB(x, y, pr.getArgb(x, y));
                    }
                }
                String name = file.getName().toLowerCase();
                String format = (name.endsWith(".jpg") || name.endsWith(".jpeg")) ? "jpg" : "png";
                if ("jpg".equals(format)) {
                    java.awt.image.BufferedImage rgbImg = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g2 = rgbImg.createGraphics();
                    g2.drawImage(bImg, 0, 0, java.awt.Color.BLACK, null);
                    g2.dispose();
                    javax.imageio.ImageIO.write(rgbImg, "jpg", file);
                } else {
                    javax.imageio.ImageIO.write(bImg, "png", file);
                }
                logger.info("Successfully exported image map to {}", file.getAbsolutePath());
            } catch (Exception ex) {
                logger.error("Failed to export image map", ex);
            }
        }
    }
}

