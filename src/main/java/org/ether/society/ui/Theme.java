/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UI Theme manager (Dark Glass vs Light Glass).
 */
public enum Theme {
    DARK("Dark", "/css/index.css"),
    LIGHT("Light", "/css/light.css"),
    PRESENTATION("Presentation", "/css/presentation.css");

    private static final Logger logger = LoggerFactory.getLogger(Theme.class);
    private static final java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(Theme.class);
    private static final String PREF_THEME_KEY = "ether_theme";
    private static final ObjectProperty<Theme> currentTheme = new SimpleObjectProperty<>();

    static {
        String savedTheme = prefs.get(PREF_THEME_KEY, DARK.name());
        try {
            currentTheme.set(Theme.valueOf(savedTheme));
        } catch (Exception e) {
            currentTheme.set(DARK);
        }
    }

    private final String displayName;
    private final String stylesheetPath;

    Theme(String displayName, String stylesheetPath) {
        this.displayName = displayName;
        this.stylesheetPath = stylesheetPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStylesheetPath() {
        return stylesheetPath;
    }

    public static ObjectProperty<Theme> themeProperty() {
        return currentTheme;
    }

    public static Theme getCurrentTheme() {
        return currentTheme.get();
    }

    public static void setTheme(Scene scene, Theme theme) {
        if (theme == null) return;
        currentTheme.set(theme);
        try {
            prefs.put(PREF_THEME_KEY, theme.name());
            prefs.flush();
        } catch (Exception e) {
            logger.warn("Could not flush theme preference: {}", e.getMessage());
        }
        if (scene != null) {
            applyCurrentTheme(scene);
        }
    }

    public static void applyCurrentTheme(Scene scene) {
        if (scene == null) return;
        Theme theme = getCurrentTheme();
        try {
            scene.getStylesheets().clear();
            var res = Theme.class.getResource(theme.getStylesheetPath());
            if (res != null) {
                scene.getStylesheets().add(res.toExternalForm());
                if (scene.getRoot() != null) {
                    scene.getRoot().applyCss();
                    scene.getRoot().layout();
                }
                logger.info("UI Theme applied to scene: {}", theme);
            } else {
                logger.error("Theme stylesheet resource not found: {}", theme.getStylesheetPath());
            }
        } catch (Exception e) {
            logger.error("Failed to apply theme: {}", theme, e);
        }
    }
}
