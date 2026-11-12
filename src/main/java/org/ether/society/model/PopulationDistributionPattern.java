/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.model;

import java.util.Locale;

/**
 * <h1>Population Distribution Pattern</h1>
 *
 * <p>Strongly-typed enumeration of procedural spatial settlement priors and historical demographic
 * seed distributions across planetary H3 meshes.</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public enum PopulationDistributionPattern {

    UNBIASED_NATURAL,
    COASTAL_MARITIME,
    RIVER_VALLEYS,
    HIGHLAND_MOUNTAIN,
    INLAND_OASIS,
    EQUATORIAL_BELT,
    EGYPT_NILE,
    GREEN_SAHARA,
    MESOAMERICA,
    ROMAN_EMPIRE,
    AUSTRALIA_SAHUL,
    INDIA_MAURYA,
    FERTILE_CRESCENT,
    BERINGIA_AMERICAS,
    WEST_AFRICA_MALI,
    JAPAN_SAKOKU,
    AMERICAS_1491,
    ONE_CONTINENT,
    INDUSTRIAL_1800,
    SPARSE_NOMADIC;

    /**
     * Resolves a raw string into a typed distribution pattern, accepting historical aliases.
     *
     * @param rawName raw input name or alias
     * @return resolved {@link PopulationDistributionPattern}, defaulting to {@link #UNBIASED_NATURAL}
     */
    public static PopulationDistributionPattern fromString(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return UNBIASED_NATURAL;
        }
        String normalized = rawName.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "UNBIASED", "NONE", "NATURAL_EQUILIBRIUM", "UNBIASED_NATURAL" -> UNBIASED_NATURAL;
            case "COASTAL_MARITIME", "COASTAL", "MARITIME" -> COASTAL_MARITIME;
            case "RIVER_VALLEYS", "RIVER", "VALLEYS" -> RIVER_VALLEYS;
            case "HIGHLAND_MOUNTAIN", "HIGHLAND", "MOUNTAIN" -> HIGHLAND_MOUNTAIN;
            case "INLAND_OASIS", "OASIS" -> INLAND_OASIS;
            case "EQUATORIAL_BELT", "EQUATORIAL" -> EQUATORIAL_BELT;
            case "EGYPT_NILE", "NILE" -> EGYPT_NILE;
            case "GREEN_SAHARA", "SAHARA" -> GREEN_SAHARA;
            case "MESOAMERICA", "MAYA" -> MESOAMERICA;
            case "ROMAN_EMPIRE", "ROME" -> ROMAN_EMPIRE;
            case "AUSTRALIA_SAHUL", "SAHUL", "AUSTRALIA" -> AUSTRALIA_SAHUL;
            case "INDIA_MAURYA", "MAURYA", "INDIA" -> INDIA_MAURYA;
            case "FERTILE_CRESCENT", "MESOPOTAMIA_ASSYRIA", "MESOPOTAMIA" -> FERTILE_CRESCENT;
            case "BERINGIA_AMERICAS", "LGM_REFUGIA", "YOUNGER_DRYAS", "BERINGIA" -> BERINGIA_AMERICAS;
            case "WEST_AFRICA_MALI", "MALI" -> WEST_AFRICA_MALI;
            case "JAPAN_SAKOKU", "JAPAN" -> JAPAN_SAKOKU;
            case "AMERICAS_1491", "COLUMBIAN_CONTACT", "AMERICAS" -> AMERICAS_1491;
            case "ONE_CONTINENT", "PANGEA" -> ONE_CONTINENT;
            case "INDUSTRIAL_1800", "INDUSTRIAL" -> INDUSTRIAL_1800;
            case "SPARSE_NOMADIC", "NOMADIC" -> SPARSE_NOMADIC;
            default -> {
                try {
                    yield PopulationDistributionPattern.valueOf(normalized);
                } catch (IllegalArgumentException ex) {
                    yield UNBIASED_NATURAL;
                }
            }
        };
    }
}
