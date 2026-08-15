/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.i18n;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utility class for internationalization.
 * Manages the current language and resource bundles.
 */
public class I18n {
    private static final Logger logger = LoggerFactory.getLogger(I18n.class);
    private static final String BUNDLE_NAME = "i18n/messages";

    private static final java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(I18n.class);
    private static final String PREF_LANG_KEY = "ether_language";

    private static final ObjectProperty<Language> currentLanguage = new SimpleObjectProperty<>();
    private static ResourceBundle bundle;

    private static final ResourceBundle.Control NO_DEFAULT_LOCALE_CONTROL = new ResourceBundle.Control() {
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            return List.of(locale, Locale.ROOT);
        }
    };

    static {
        // Load saved language preference or fallback to ENGLISH
        setLanguage(loadSavedLanguage());
    }

    private static Language loadSavedLanguage() {
        String code = prefs.get(PREF_LANG_KEY, Language.ENGLISH.getCode());
        for (Language lang : Language.values()) {
            if (lang.getCode().equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return Language.ENGLISH;
    }

    public static void setLanguage(Language language) {
        if (language != null) {
            try {
                bundle = ResourceBundle.getBundle(BUNDLE_NAME, language.getLocale(), NO_DEFAULT_LOCALE_CONTROL);
                currentLanguage.set(language);
                prefs.put(PREF_LANG_KEY, language.getCode());
                try {
                    prefs.flush();
                } catch (Exception ignored) {}
                logger.info("Language switched to: {}", language);
            } catch (Exception e) {
                logger.error("Failed to load resource bundle for language: {}", language, e);
                if (language != Language.ENGLISH) {
                    setLanguage(Language.ENGLISH);
                }
            }
        }
    }

    /**
     * Get the current language property.
     * Listen to this property to update UI when language changes.
     */
    public static ObjectProperty<Language> languageProperty() {
        return currentLanguage;
    }

    public static Language getCurrentLanguage() {
        return currentLanguage.get();
    }

    private static String sanitize(String str) {
        if (str == null) return null;
        return str.replace("\ufe0f", "").replace("\ufe0e", "");
    }

    /**
     * Get a localized string for the given key.
     * 
     * @param key The resource key
     * @return The localized string, or the key if not found
     */
    public static String get(String key) {
        try {
            if (bundle.containsKey(key)) {
                return sanitize(bundle.getString(key));
            } else {
                logger.warn("Missing translation key: {}", key);
                return sanitize(key);
            }
        } catch (Exception e) {
            return sanitize(key);
        }
    }

    /**
     * Get a localized string for the key, or return fallback default value if missing.
     */
    public static String getOrDefault(String key, String defaultValue) {
        try {
            if (bundle != null && bundle.containsKey(key)) {
                return sanitize(bundle.getString(key));
            }
        } catch (Exception ignored) {}
        return sanitize(defaultValue);
    }

    /**
     * Get a localized and formatted string.
     * 
     * @param key  The resource key
     * @param args Arguments for formatting
     * @return The formatted localized string
     */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            logger.error("Failed to format string key: {}", key, e);
            return pattern;
        }
    }
}

