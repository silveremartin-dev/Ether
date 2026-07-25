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
    LIGHT("Light", "/css/light.css");

    private static final Logger logger = LoggerFactory.getLogger(Theme.class);
    private static final ObjectProperty<Theme> currentTheme = new SimpleObjectProperty<>(DARK);

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
        if (theme == null || scene == null) return;
        currentTheme.set(theme);
        
        try {
            scene.getStylesheets().clear();
            String css = Theme.class.getResource(theme.getStylesheetPath()).toExternalForm();
            scene.getStylesheets().add(css);
            logger.info("UI Theme switched to: {}", theme);
        } catch (Exception e) {
            logger.error("Failed to apply theme: {}", theme, e);
        }
    }
}
