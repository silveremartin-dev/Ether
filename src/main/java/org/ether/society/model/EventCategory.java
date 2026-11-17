/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.ether.society.i18n.I18n;

import java.util.Locale;

/**
 * <h1>Event Category</h1>
 * <p>
 * Fundamental ontological classification separating exogenous planetary forcings,
 * human-caused catastrophes, and pure historical milestones.
 * </p>
 * <ul>
 *   <li><b>NATURAL</b>: Exogenous physical, geological, and astronomical shocks independent of human agency
 *       (volcanoes, earthquakes, tsunamis, meteors, solar storms).</li>
 *   <li><b>ANTHROPIC</b>: Artificial, technological, industrial, and warfare disasters caused by human society
 *       (nuclear strikes, industrial chemical leaks, dam failures, urban fires).</li>
 *   <li><b>CHRONICLE_MILESTONE</b>: Purely informative cultural, scientific, and institutional markers
 *       (discoveries, inventions, treaties, space missions) with ZERO physical shock on simulation cells.</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public enum EventCategory {

    /** Exogenous natural, geophysical, and astronomical events. */
    NATURAL("NATURAL", "event.category.natural", "Naturel"),

    /** Human-caused, technological, industrial, or warfare catastrophes. */
    ANTHROPIC("ANTHROPIC", "event.category.anthropic", "Anthropique"),

    /** Purely informative historical milestones for the chronicle (zero physical shock). */
    CHRONICLE_MILESTONE("CHRONICLE_MILESTONE", "event.category.milestone", "Repère Historique");

    private final String code;
    private final String i18nKey;
    private final String defaultLabel;

    EventCategory(String code, String i18nKey, String defaultLabel) {
        this.code = code;
        this.i18nKey = i18nKey;
        this.defaultLabel = defaultLabel;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public String getDisplayName() {
        return I18n.getOrDefault(i18nKey, defaultLabel);
    }

    public boolean isPhysicalCataclysm() {
        return this == NATURAL || this == ANTHROPIC;
    }

    public boolean isInformativeOnly() {
        return this == CHRONICLE_MILESTONE;
    }

    @JsonCreator
    public static EventCategory fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return NATURAL;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "NATURAL", "NATURE", "PHYSIQUE", "GEOPHYSICAL", "DISASTER_NATURAL" -> NATURAL;
            case "ANTHROPIC", "ANTHROPIQUE", "HUMAN", "ARTIFICIEL", "TECHNOLOGICAL", "DISASTER_ANTHROPIC" -> ANTHROPIC;
            case "CHRONICLE_MILESTONE", "MILESTONE", "REPERE", "HISTORIQUE", "CHRONICLE", "INFORMATIVE" -> CHRONICLE_MILESTONE;
            default -> {
                if (upper.contains("MILESTONE") || upper.contains("HIST")) yield CHRONICLE_MILESTONE;
                if (upper.contains("NUCLEAR") || upper.contains("WAR") || upper.contains("CRASH") || upper.contains("CYBER") || upper.contains("ACCIDENT")) yield ANTHROPIC;
                yield NATURAL;
            }
        };
    }
}
