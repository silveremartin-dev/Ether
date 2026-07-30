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
import java.util.ResourceBundle;

/**
 * Utility class for internationalization.
 * Manages the current language and resource bundles.
 */
public class I18n {
    private static final Logger logger = LoggerFactory.getLogger(I18n.class);
    private static final String BUNDLE_NAME = "i18n/messages";

    private static final ObjectProperty<Language> currentLanguage = new SimpleObjectProperty<>();
    private static ResourceBundle bundle;

    static {
        // Initialize with default language (English)
        setLanguage(Language.ENGLISH);
    }

    /**
     * Set the current application language.
     * Loads the appropriate resource bundle.
     * 
     * @param language The language to switch to
     */
    public static void setLanguage(Language language) {
        if (currentLanguage.get() != language) {
            try {
                bundle = ResourceBundle.getBundle(BUNDLE_NAME, language.getLocale());
                currentLanguage.set(language);
                logger.info("Language switched to: {}", language);
            } catch (Exception e) {
                logger.error("Failed to load resource bundle for language: {}", language, e);
                // Fallback to English if not already
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

    /**
     * Get a localized string for the given key.
     * 
     * @param key The resource key
     * @return The localized string, or the key if not found
     */
    public static String get(String key) {
        try {
            if (bundle.containsKey(key)) {
                return bundle.getString(key);
            } else {
                logger.warn("Missing translation key: {}", key);
                return key;
            }
        } catch (Exception e) {
            return key;
        }
    }

    /**
     * Get a localized string for the key, or return fallback default value if missing.
     */
    public static String getOrDefault(String key, String defaultValue) {
        try {
            if (bundle != null && bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        } catch (Exception ignored) {}
        return defaultValue;
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

