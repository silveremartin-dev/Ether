/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.i18n;

import java.util.Locale;

/**
 * <h1>Language</h1>
 * <p>
 * Core operational component for the Ether civilizational and planetary simulation framework.<br>
 * Integrates cellular dynamics, data structures, and deterministic state transitions.
 * </p>
 * 
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public enum Language {
    ENGLISH("en", "English", Locale.ENGLISH),
    FRENCH("fr", "Français", Locale.FRENCH),
    SPANISH("es", "Español", Locale.of("es")),
    GERMAN("de", "Deutsch", Locale.GERMAN),
    CHINESE("zh", "中文", Locale.CHINESE);

    /** Internal state: code (String). */
    private final String code;
    /** Internal state: display name (String). */
    private final String displayName;
    private final Locale locale;

    Language(String code, String displayName, Locale locale) {
        this.code = code;
        this.displayName = displayName;
        this.locale = locale;
    }

    /**
     * Get code.
     *
     * @return the resulting computation or state reference
     */
    public String getCode() {
        return code;
    }

    /**
     * Get display name.
     *
     * @return the resulting computation or state reference
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get locale.
     *
     * @return the resulting computation or state reference
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * To string.
     *
     * @return the resulting computation or state reference
     */
    @Override
    public String toString() {
        return displayName;
    }
}

