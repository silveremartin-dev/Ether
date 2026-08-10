/*
 * MIT License
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.i18n;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Unit Test Suite verifying internationalization completeness, bundle keys integrity,
 * and lack of hardcoded fallback errors across all 5 supported locales (EN, FR, ES, DE, ZH).
 */
public class I18nAndLocalizationTest {

    private static ResourceBundle bundleEn;
    private static ResourceBundle bundleFr;
    private static ResourceBundle bundleEs;
    private static ResourceBundle bundleDe;
    private static ResourceBundle bundleZh;

    @BeforeAll
    public static void setUp() {
        bundleEn = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        bundleFr = ResourceBundle.getBundle("i18n.messages", Locale.FRENCH);
        bundleEs = ResourceBundle.getBundle("i18n.messages", new Locale("es"));
        bundleDe = ResourceBundle.getBundle("i18n.messages", Locale.GERMAN);
        bundleZh = ResourceBundle.getBundle("i18n.messages", Locale.CHINESE);
    }

    @Test
    @DisplayName("Verify bundles load for all 5 supported languages")
    public void testBundlesExistAndLoad() {
        assertNotNull(bundleEn, "English resource bundle must be loaded");
        assertNotNull(bundleFr, "French resource bundle must be loaded");
        assertNotNull(bundleEs, "Spanish resource bundle must be loaded");
        assertNotNull(bundleDe, "German resource bundle must be loaded");
        assertNotNull(bundleZh, "Chinese resource bundle must be loaded");
    }

    @Test
    @DisplayName("Verify key parity: All keys in English must exist in FR, ES, DE, and ZH")
    public void testKeyParityAcrossLocales() {
        Set<String> enKeys = bundleEn.keySet();
        assertFalse(enKeys.isEmpty(), "English resource bundle must not be empty");

        List<String> missingInFr = new ArrayList<>();
        List<String> missingInEs = new ArrayList<>();
        List<String> missingInDe = new ArrayList<>();
        List<String> missingInZh = new ArrayList<>();

        for (String key : enKeys) {
            if (!bundleFr.containsKey(key) || bundleFr.getString(key).trim().isEmpty()) {
                missingInFr.add(key);
            }
            if (!bundleEs.containsKey(key) || bundleEs.getString(key).trim().isEmpty()) {
                missingInEs.add(key);
            }
            if (!bundleDe.containsKey(key) || bundleDe.getString(key).trim().isEmpty()) {
                missingInDe.add(key);
            }
            if (!bundleZh.containsKey(key) || bundleZh.getString(key).trim().isEmpty()) {
                missingInZh.add(key);
            }
        }

        assertTrue(missingInFr.isEmpty(), "Missing or empty FR keys: " + missingInFr);
        assertTrue(missingInEs.isEmpty(), "Missing or empty ES keys: " + missingInEs);
        assertTrue(missingInDe.isEmpty(), "Missing or empty DE keys: " + missingInDe);
        assertTrue(missingInZh.isEmpty(), "Missing or empty ZH keys: " + missingInZh);
    }

    @Test
    @DisplayName("Verify critical execution context keys exist across all bundles")
    public void testExecutionContextKeys() {
        String[] criticalKeys = {
            "tab.execution_context",
            "exec.title",
            "exec.mode",
            "exec.mode.cpu",
            "exec.mode.gpu",
            "exec.mode.cluster",
            "exec.cluster.title",
            "exec.cluster.start_master",
            "exec.cluster.join",
            "exec.cluster.test",
            "exec.cluster.refresh",
            "exec.btn.launch"
        };

        for (String key : criticalKeys) {
            assertTrue(bundleEn.containsKey(key), "EN bundle missing: " + key);
            assertTrue(bundleFr.containsKey(key), "FR bundle missing: " + key);
            assertTrue(bundleEs.containsKey(key), "ES bundle missing: " + key);
            assertTrue(bundleDe.containsKey(key), "DE bundle missing: " + key);
            assertTrue(bundleZh.containsKey(key), "ZH bundle missing: " + key);
        }
    }

    @Test
    @DisplayName("Test I18n manager utility class methods")
    public void testI18nManagerClass() {
        I18n.setLanguage(Language.ENGLISH);
        assertEquals(Language.ENGLISH, I18n.getCurrentLanguage());
        assertNotNull(I18n.get("app.title"));

        I18n.setLanguage(Language.FRENCH);
        assertEquals(Language.FRENCH, I18n.getCurrentLanguage());
        assertNotNull(I18n.get("app.title"));

        String resultWithDefault = I18n.getOrDefault("non_existent_key_xyz", "Default Custom Fallback");
        assertEquals("Default Custom Fallback", resultWithDefault);
    }
}
