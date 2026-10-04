/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.i18n;

import java.util.Locale;

/**
 * Supported languages for the application.
 */
public enum Language {
    ENGLISH("en", "English", Locale.ENGLISH),
    FRENCH("fr", "Français", Locale.FRENCH),
    SPANISH("es", "Español", Locale.of("es")),
    GERMAN("de", "Deutsch", Locale.GERMAN),
    CHINESE("zh", "中文", Locale.CHINESE);

    /* Internal state variable for code (String). */
    private final String code;
    /* Internal state variable for display name (String). */
    private final String displayName;
    private final Locale locale;

    Language(String code, String displayName, Locale locale) {
        this.code = code;
        this.displayName = displayName;
        this.locale = locale;
    }

    /*
     * Get code.
     * Enforces physical invariants and updates associated state variables within {@code Language}.
     *
     * @return the resulting computation or state reference
     */
    public String getCode() {
        return code;
    }

    /*
     * Get display name.
     * Enforces physical invariants and updates associated state variables within {@code Language}.
     *
     * @return the resulting computation or state reference
     */
    public String getDisplayName() {
        return displayName;
    }

    /*
     * Get locale.
     * Enforces physical invariants and updates associated state variables within {@code Language}.
     *
     * @return the resulting computation or state reference
     */
    public Locale getLocale() {
        return locale;
    }

    @Override
    /*
     * To string.
     * Enforces physical invariants and updates associated state variables within {@code Language}.
     *
     * @return the resulting computation or state reference
     */
    public String toString() {
        return displayName;
    }
}

