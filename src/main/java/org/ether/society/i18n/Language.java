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
    FRENCH("fr", "FranÃ§ais", Locale.FRENCH),
    SPANISH("es", "EspaÃ±ol", new Locale("es")),
    GERMAN("de", "Deutsch", Locale.GERMAN);

    private final String code;
    private final String displayName;
    private final Locale locale;

    Language(String code, String displayName, Locale locale) {
        this.code = code;
        this.displayName = displayName;
        this.locale = locale;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Locale getLocale() {
        return locale;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

