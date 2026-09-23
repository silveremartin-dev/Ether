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
    private static ResourceBundle defaultBundle;

    private static final ResourceBundle.Control NO_DEFAULT_LOCALE_CONTROL = new ResourceBundle.Control() {
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            return List.of(locale, Locale.ROOT);
        }
    };

    static {
        try {
            defaultBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH, NO_DEFAULT_LOCALE_CONTROL);
        } catch (Exception ignored) {}
        // Load saved language preference or fallback to ENGLISH
        setLanguage(loadSavedLanguage());
    }

    private static Language loadSavedLanguage() {
        String code = prefs.get(PREF_LANG_KEY, null);
        if (code != null && !code.isBlank()) {
            for (Language lang : Language.values()) {
                if (lang.getCode().equalsIgnoreCase(code)) {
                    return lang;
                }
            }
        }
        // Fallback to system default locale if supported, otherwise ENGLISH
        String sysLang = Locale.getDefault().getLanguage();
        for (Language lang : Language.values()) {
            if (lang.getCode().equalsIgnoreCase(sysLang)) {
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
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        prefs.flush();
                    } catch (Exception ignored) {}
                });
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
            if (bundle != null && bundle.containsKey(key)) {
                return sanitize(bundle.getString(key));
            } else if (defaultBundle != null && defaultBundle.containsKey(key)) {
                return sanitize(defaultBundle.getString(key));
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
            } else if (defaultBundle != null && defaultBundle.containsKey(key)) {
                return sanitize(defaultBundle.getString(key));
            }
        } catch (Exception ignored) {}
        return sanitize(defaultValue);
    }

    /**
     * Get a localized string for the key with format arguments, or return fallback default value formatted with args if missing.
     */
    public static String getOrDefault(String key, String defaultValue, Object... args) {
        String pattern = defaultValue;
        try {
            if (bundle != null && bundle.containsKey(key)) {
                pattern = bundle.getString(key);
            } else if (defaultBundle != null && defaultBundle.containsKey(key)) {
                pattern = defaultBundle.getString(key);
            }
        } catch (Exception ignored) {}
        if (pattern == null) pattern = defaultValue;
        pattern = sanitize(pattern);
        try {
            if (pattern.contains("{0}") || pattern.contains("{1}") || pattern.contains("{2}")) {
                return MessageFormat.format(pattern, args);
            } else if (pattern.contains("%")) {
                return String.format(pattern, args);
            } else {
                return MessageFormat.format(pattern, args);
            }
        } catch (Exception e) {
            return pattern;
        }
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
        if (pattern == null) return key;
        try {
            if (pattern.contains("{0}") || pattern.contains("{1}") || pattern.contains("{2}")) {
                return MessageFormat.format(pattern, args);
            } else if (pattern.contains("%")) {
                return String.format(pattern, args);
            } else {
                return MessageFormat.format(pattern, args);
            }
        } catch (Exception e) {
            logger.error("Failed to format string key: {}", key, e);
            return pattern;
        }
    }

    /**
     * Get localized display name for a planet preset.
     */
    public static String getPlanetPresetDisplayName(String name) {
        if (name == null || name.isBlank()) return getOrDefault("planet.preset.earth", "Terre (Terran)");
        String lower = name.toLowerCase();
        if (lower.contains("super-terre") || lower.contains("super-earth") || lower.contains("gaia")) {
            return getOrDefault("planet.preset.super_earth", "Super-Terre (Gaia Prime)");
        } else if (lower.contains("-100") || lower.contains("lig") || lower.contains("interglaciaire") || lower.contains("eemian")) {
            return getOrDefault("planet.preset.earth_lig", "Terre (-100 000 / Dernier Interglaciaire)");
        } else if (lower.contains("-50") || lower.contains("mis3") || lower.contains("mis 3") || lower.contains("sahul")) {
            return getOrDefault("planet.preset.earth_mis3", "Terre (-50 000 / Stade Isotopique 3 & Sahul)");
        } else if (lower.contains("-25") || lower.contains("béringie") || lower.contains("beringia")) {
            return getOrDefault("planet.preset.earth_lgm_onset", "Terre (-25 000 / Début LGM & Béringie)");
        } else if (lower.contains("-20") || lower.contains("lgm") || lower.contains("maximum glaciaire") || lower.contains("last glacial maximum") || lower.contains("末次盛冰期") || lower.contains("letztes glaziales maximum")) {
            return getOrDefault("planet.preset.earth_lgm", "Terre (-20 000 / Maximum Glaciaire)");
        } else if (lower.contains("-10000") || lower.contains("-10 000") || lower.contains("eh") || lower.contains("précoce") || lower.contains("early holocene") || lower.contains("frühholozän") || lower.contains("早全新世")) {
            return getOrDefault("planet.preset.earth_eh", "Terre (-10 000 / Holocène Précoce)");
        } else if (lower.contains("-6000") || lower.contains("-6 000") || lower.contains("mh") || lower.contains("sahara") || lower.contains("mid holocene") || lower.contains("绿色撒哈拉") || lower.contains("grüne sahara")) {
            return getOrDefault("planet.preset.earth_mh", "Terre (-6 000 / Sahara Vert)");
        } else if (lower.contains("-3000") || lower.contains("-3 000") || lower.contains("lh") || lower.contains("tardif") || lower.contains("late holocene") || lower.contains("spätholozän") || lower.contains("晚全新世")) {
            return getOrDefault("planet.preset.earth_lh", "Terre (-3 000 / Holocène Tardif)");
        } else if (lower.contains("-1900") || lower.contains("-1 900") || lower.contains("bronze") || lower.contains("青铜时代")) {
            return getOrDefault("planet.preset.earth_bronze", "Terre (-1 900 / Âge du Bronze Moyen)");
        } else if (lower.contains("-1000") || lower.contains("-1 000") || lower.contains("iron") || lower.contains("fer") || lower.contains("eisenzeit") || lower.contains("hierro") || lower.contains("铁器时代")) {
            return getOrDefault("planet.preset.earth_iron", "Terre (-1 000 / Début Âge du Fer)");
        } else if (lower.contains("2026") || lower.contains("moderne") || lower.contains("modern") || lower.contains("anthropocène") || lower.contains("anthropocene")) {
            return getOrDefault("planet.preset.earth_modern", "Terre (2026 / Moderne)");
        } else if (lower.contains("terre") || lower.contains("terran") || lower.contains("earth")) {
            return getOrDefault("planet.preset.earth", "Terre (Terran)");
        } else if (lower.contains("mars") || lower.contains("ares")) {
            return getOrDefault("planet.preset.mars", "Mars (Ares)");
        } else if (lower.contains("vénus") || lower.contains("venus") || lower.contains("hesperos")) {
            return getOrDefault("planet.preset.venus", "Vénus (Hesperos)");
        } else if (lower.contains("titan")) {
            return getOrDefault("planet.preset.titan", "Titan (Cryo-Lune)");
        } else if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene")) {
            return getOrDefault("planet.preset.moon", "Lune (Selene)");
        } else if (lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes")) {
            return getOrDefault("planet.preset.mercury", "Mercure (Hermes)");
        } else if (lower.contains("synchrone") || lower.contains("eyeball")) {
            return getOrDefault("planet.preset.eyeball", "Monde Synchrone (Eyeball)");
        } else if (lower.contains("océan") || lower.contains("ocean") || lower.contains("oceania")) {
            return getOrDefault("planet.preset.water", "Monde Océan (Oceania)");
        } else if (lower.contains("glaciaire") || lower.contains("ice") || lower.contains("boreas")) {
            return getOrDefault("planet.preset.ice", "Monde Glaciaire (Boreas)");
        } else if (lower.contains("archipel") || lower.contains("archipelago")) {
            return getOrDefault("planet.preset.archipelago", "Archipel");
        }
        return name;
    }

    /**
     * Get localized description for a planet preset.
     * Falls back to the display name if no dedicated description key exists.
     */
    public static String getPlanetPresetDescription(String name) {
        String displayName = getPlanetPresetDisplayName(name);
        // Try a dedicated description key derived from the display name key
        String lower = name == null ? "" : name.toLowerCase();
        String descKey = null;
        if (lower.contains("-100") || lower.contains("lig")) descKey = "planet.preset.earth_lig.desc";
        else if (lower.contains("-50") || lower.contains("mis3")) descKey = "planet.preset.earth_mis3.desc";
        else if (lower.contains("-25") || lower.contains("beringia") || lower.contains("béringie")) descKey = "planet.preset.earth_lgm_onset.desc";
        else if (lower.contains("-20") || lower.contains("lgm")) descKey = "planet.preset.earth_lgm.desc";
        else if (lower.contains("-10000") || lower.contains("early holocene") || lower.contains("précoce")) descKey = "planet.preset.earth_eh.desc";
        else if (lower.contains("-6000") || lower.contains("sahara")) descKey = "planet.preset.earth_mh.desc";
        else if (lower.contains("-3000") || lower.contains("late holocene") || lower.contains("tardif")) descKey = "planet.preset.earth_lh.desc";
        else if (lower.contains("-1900") || lower.contains("bronze")) descKey = "planet.preset.earth_bronze.desc";
        else if (lower.contains("-1000") || lower.contains("iron") || lower.contains("fer")) descKey = "planet.preset.earth_iron.desc";
        else if (lower.contains("2026") || lower.contains("modern")) descKey = "planet.preset.earth_modern.desc";
        else if (lower.contains("mars")) descKey = "planet.preset.mars.desc";
        else if (lower.contains("venus") || lower.contains("vénus")) descKey = "planet.preset.venus.desc";
        else if (lower.contains("titan")) descKey = "planet.preset.titan.desc";
        else if (lower.contains("moon") || lower.contains("lune")) descKey = "planet.preset.moon.desc";
        else if (lower.contains("mercury") || lower.contains("mercure")) descKey = "planet.preset.mercury.desc";
        if (descKey != null && bundle != null && bundle.containsKey(descKey)) {
            return sanitize(bundle.getString(descKey));
        }
        if (descKey != null && defaultBundle != null && defaultBundle.containsKey(descKey)) {
            return sanitize(defaultBundle.getString(descKey));
        }
        return displayName;
    }

    /**
     * Get localized description for an ecology preset.
     * Falls back to the planet preset description for the same epoch.
     */
    public static String getEcologyPresetDescription(String name) {
        // Ecology presets share the same epoch naming as planet presets
        return getPlanetPresetDescription(name);
    }

    /**
     * Get localized display name for a biome.
     */
    public static String getBiomeDisplayName(org.ether.society.model.Biome biome) {
        if (biome == null) return "";
        return switch (biome) {
            case OCEAN -> getOrDefault("biome.ocean", "Océan");
            case DEEP_OCEAN -> getOrDefault("biome.deep_ocean", "Océan Profond");
            case PLAINS -> getOrDefault("biome.plains", "Plaine");
            case FOREST -> getOrDefault("biome.forest", "Forêt");
            case DESERT -> getOrDefault("biome.desert", "Désert");
            case SNOW -> getOrDefault("biome.snow", "Neige");
            case TUNDRA -> getOrDefault("biome.tundra", "Toundra");
            case HILLS -> getOrDefault("biome.hills", "Collines");
            case MOUNTAINS -> getOrDefault("biome.mountains", "Montagnes");
            case JUNGLE -> getOrDefault("biome.jungle", "Jungle");
            case BEACH -> getOrDefault("biome.beach", "Plage");
            default -> biome.name();
        };
    }
}

