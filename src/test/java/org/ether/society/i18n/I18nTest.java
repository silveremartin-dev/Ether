/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.i18n;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class I18nTest {

    @Test
    void testLanguageSwitching() {
        // Default is English
        I18n.setLanguage(Language.ENGLISH);
        assertEquals("Start", I18n.get("ui.control.start"));
        assertEquals("Biome:", I18n.get("ui.tooltip.biome"));

        // Switch to French
        I18n.setLanguage(Language.FRENCH);
        assertEquals("Démarrer", I18n.get("ui.control.start"));
        assertEquals("Biome :", I18n.get("ui.tooltip.biome"));

        // Switch to Spanish
        I18n.setLanguage(Language.SPANISH);
        assertEquals("Iniciar", I18n.get("ui.control.start"));
        assertEquals("Bioma:", I18n.get("ui.tooltip.biome"));

        // Switch to German
        I18n.setLanguage(Language.GERMAN);
        assertEquals("Start", I18n.get("ui.control.start"));
        assertEquals("Biom:", I18n.get("ui.tooltip.biome"));
    }

    @Test
    void testFormatting() {
        I18n.setLanguage(Language.ENGLISH);
        String formatted = I18n.get("app.info", 100);
        assertTrue(formatted.contains("100 cells loaded"));

        I18n.setLanguage(Language.FRENCH);
        formatted = I18n.get("app.info", 100);
        assertTrue(formatted.contains("100 cellules chargées"));
    }

    @Test
    void testMissingKey() {
        String key = "non.existent.key";
        assertEquals(key, I18n.get(key));
    }
}
