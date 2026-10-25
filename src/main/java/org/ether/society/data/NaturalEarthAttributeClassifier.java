/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.Color;
import java.util.Locale;
import java.util.Set;

/**
 * Cliodynamic and Anthropological Attribute Classifier for Natural Earth Country Features.
 * Provides lookup and heuristic mapping for sovereignty, isogloss branches, Todd/Murdock kinship typologies,
 * confessional distributions, state capacity, technology, and ecological footprints.
 */
public final class NaturalEarthAttributeClassifier {

    private NaturalEarthAttributeClassifier() {
        // Utility class
    }

    /**
     * Computes sovereign color for a given country feature and historical epoch year.
     *
     * @param c country feature
     * @param year historical year
     * @return sovereign border / fill color
     */
    public static Color computeSovereignColorForYear(NaturalEarthVectorIngestor.CountryFeature c, long year) {
        if (c == null) return Color.BLACK;
        String iso = (c.isoA3 != null) ? c.isoA3.toUpperCase(Locale.ROOT) : "";

        // Historical imperial mapping for Belle Epoque & WWI (1900 / 1914)
        if (year >= 1886 && year <= 1920) {
            // British Empire
            if (Set.of("GBR", "IND", "PAK", "BGD", "LKA", "MMR", "MYS", "SGP", "EGY", "SDN", "KEN", "UGA", "NGA", "GHA", "ZAF", "ZMB", "ZWE", "BWA", "BLZ", "GUY", "AUS", "NZL", "CAN", "CYP", "HKG").contains(iso)) {
                return new Color(0xB91C1C);
            }
            // Russian Empire
            if (Set.of("RUS", "UKR", "BLR", "MDA", "EST", "LVA", "LTU", "GEO", "ARM", "AZE", "KAZ", "UZB", "TKM", "KGZ", "TJK", "FIN").contains(iso)) {
                return new Color(0x7C3AED);
            }
            // Austro-Hungarian Empire
            if (Set.of("AUT", "HUN", "CZE", "SVK", "HRV", "BIH", "SVN").contains(iso)) {
                return new Color(0xD97706);
            }
            // German Empire
            if (Set.of("DEU", "CMR", "TGO", "NAM", "TZA", "PNG").contains(iso)) {
                return new Color(0x1E293B);
            }
            // Ottoman Empire
            if (Set.of("TUR", "SYR", "IRQ", "LBN", "JOR", "PSE", "ISR").contains(iso)) {
                return new Color(0x047857);
            }
            // French Colonial Empire
            if (Set.of("FRA", "DZA", "TUN", "MAR", "SEN", "MLI", "GIN", "CIV", "BFA", "BEN", "NER", "TCD", "CAF", "COG", "GAB", "MDG", "VNM", "LAO", "KHM").contains(iso)) {
                return new Color(0x1D4ED8);
            }
            // Dutch Empire
            if (Set.of("NLD", "IDN", "SUR").contains(iso)) {
                return new Color(0xF97316);
            }
            // Portuguese Empire
            if (Set.of("PRT", "AGO", "MOZ", "GNB", "CPV", "STP", "TLS").contains(iso)) {
                return new Color(0x047857);
            }
            // Belgian Congo
            if (Set.of("BEL", "COD").contains(iso)) {
                return new Color(0xF59E0B);
            }
            // Empire of Japan
            if (Set.of("JPN", "KOR", "TWN").contains(iso)) {
                return new Color(0xE11D48);
            }
            // Qing / ROC
            if (Set.of("CHN", "MNG").contains(iso)) {
                return new Color(0xEF4444);
            }
            // Kingdom of Italy
            if (Set.of("ITA", "LBY", "ERI", "SOM").contains(iso)) {
                return new Color(0x059669);
            }
        } else if (year > 1920 && year <= 1955) {
            // Post-WWII / 1950 cold war configuration
            if (Set.of("RUS", "UKR", "BLR", "MDA", "EST", "LVA", "LTU", "GEO", "ARM", "AZE", "KAZ", "UZB", "TKM", "KGZ", "TJK").contains(iso)) {
                return new Color(0xDC2626);
            }
            if (Set.of("GBR", "KEN", "UGA", "NGA", "GHA", "MYS", "HKG", "CYP").contains(iso)) {
                return new Color(0xB91C1C);
            }
            if (Set.of("FRA", "DZA", "SEN", "MLI", "MDG", "VNM", "LAO", "KHM").contains(iso)) {
                return new Color(0x1D4ED8);
            }
        }

        return c.sovereignColor != null ? c.sovereignColor : computeSovereignColor(c.isoA3, c.name);
    }

    public static Color computeSovereignColor(String iso, String name) {
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "USA" -> new Color(0x2563EB);
            case "CAN" -> new Color(0xDC2626);
            case "MEX" -> new Color(0x16A34A);
            case "FRA" -> new Color(0x1D4ED8);
            case "DEU" -> new Color(0x1E293B);
            case "GBR" -> new Color(0xB91C1C);
            case "ITA" -> new Color(0x059669);
            case "ESP" -> new Color(0xEA580C);
            case "PRT" -> new Color(0x047857);
            case "RUS" -> new Color(0x7C3AED);
            case "CHN" -> new Color(0xEF4444);
            case "JPN" -> new Color(0xE11D48);
            case "KOR" -> new Color(0x0284C7);
            case "IND" -> new Color(0xF59E0B);
            case "AUS" -> new Color(0x0D9488);
            case "NZL" -> new Color(0x0F766E);
            case "BRA" -> new Color(0x10B981);
            case "ARG" -> new Color(0x38BDF8);
            case "ECU" -> new Color(0xFBBF24);
            case "COL" -> new Color(0xF59E0B);
            case "PER" -> new Color(0xDC2626);
            case "CHL" -> new Color(0xDC2626);
            case "ZAF" -> new Color(0xF59E0B);
            case "EGY" -> new Color(0xD97706);
            case "NGA" -> new Color(0x15803D);
            case "IDN" -> new Color(0xDC2626);
            case "PHL" -> new Color(0x0284C7);
            case "VNM" -> new Color(0xEF4444);
            case "THA" -> new Color(0x8B5CF6);
            case "SAU" -> new Color(0x15803D);
            case "IRN" -> new Color(0x0D9488);
            case "TUR" -> new Color(0xDC2626);
            case "UKR" -> new Color(0x0284C7);
            case "POL" -> new Color(0xBE185D);
            case "SWE" -> new Color(0x0284C7);
            case "NOR" -> new Color(0xDC2626);
            case "FIN" -> new Color(0x0D9488);
            case "DNK" -> new Color(0xB91C1C);
            case "NLD" -> new Color(0xF97316);
            case "BEL" -> new Color(0xF59E0B);
            case "CHE" -> new Color(0xEF4444);
            case "AUT" -> new Color(0xDC2626);
            case "GRC" -> new Color(0x06B6D4);
            case "IRL" -> new Color(0x10B981);
            default -> {
                int hash = Math.abs(name != null ? name.hashCode() : iso.hashCode());
                float hue = (hash % 360) / 360.0f;
                float sat = 0.70f + (hash % 25) * 0.01f;
                float bri = 0.65f + (hash % 30) * 0.01f;
                yield Color.getHSBColor(hue, sat, bri);
            }
        };
    }

    public static Color computeIsoglossColor(String iso, String name, String subregion, String continent) {
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "FRA", "MCO", "BEL", "LUX", "HTI" -> new Color(0xEC4899);
            case "ESP", "MEX", "COL", "ARG", "PER", "CHL", "ECU", "GTM", "CUB", "BOL", "DOM", "HND", "PRY", "SLV", "NIC", "CRI", "PAN", "URY" -> new Color(0xE11D48);
            case "PRT", "BRA", "AGO", "MOZ", "CPV", "STP", "GNB", "TLS" -> new Color(0x9D174D);
            case "ITA", "SMR", "VAT" -> new Color(0xE879F9);
            case "ROU", "MDA" -> new Color(0xD946EF);

            case "GBR", "USA", "CAN", "AUS", "NZL", "IRL", "JAM", "BHS", "BRB", "GUY" -> new Color(0x2563EB);
            case "DEU", "AUT", "CHE", "LIE" -> new Color(0x3B82F6);
            case "NLD", "SUR" -> new Color(0x60A5FA);
            case "SWE", "NOR", "DNK", "ISL" -> new Color(0x93C5FD);

            case "RUS", "BLR", "UKR" -> new Color(0x8B5CF6);
            case "POL", "CZE", "SVK" -> new Color(0xA855F7);
            case "SRB", "HRV", "BIH", "MNE", "BGR", "MKD", "SVN" -> new Color(0x7C3AED);

            case "LTU", "LVA" -> new Color(0x6366F1);
            case "GRC", "CYP" -> new Color(0x06B6D4);
            case "ALB", "XKX" -> new Color(0x0284C7);

            case "FIN", "EST" -> new Color(0x0D9488);
            case "HUN" -> new Color(0x14B8A6);

            case "CHN", "TWN", "SGP", "HKG", "MAC" -> new Color(0xEF4444);
            case "JPN" -> new Color(0xF43F5E);
            case "KOR", "PRK" -> new Color(0xA855F7);

            case "IND", "PAK", "BGD", "NPL", "LKA" -> new Color(0xF59E0B);

            case "IRN", "AFG", "TJK" -> new Color(0xD97706);
            case "TUR", "AZE", "KAZ", "UZB", "TKM", "KGZ" -> new Color(0x0EA5E9);
            case "MNG" -> new Color(0x2ECC71);

            case "VNM", "KHM" -> new Color(0x16A085);
            case "THA", "LAO" -> new Color(0xEAB308);
            case "MMR" -> new Color(0xDC2626);
            case "IDN", "MYS", "PHL", "BRN", "MDG" -> new Color(0x06B6D4);
            case "PNG" -> new Color(0x9C27B0);

            case "SAU", "EGY", "DZA", "MAR", "IRQ", "SDN", "SYR", "YEM", "TUN", "JOR", "ARE", "LBN", "LBY", "OMN", "KWT", "QAT", "BHR", "MRT" -> new Color(0x15803D);
            case "ISR" -> new Color(0x3B82F6);
            case "ETH", "ERI" -> new Color(0x84CC16);

            case "NGA", "COD", "TZA", "KEN", "ZAF", "GHA", "CIV", "CMR", "UGA", "ZMB", "SEN", "ZWE", "MLI", "GIN", "RWA", "BEN", "BDI", "TGO", "BFA", "COG", "GAB" -> new Color(0x22C55E);

            default -> new Color(0x2563EB);
        };
    }

    public static Color computeKinshipColor(String iso, String name, String subregion, String continent) {
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "GBR", "USA", "CAN", "AUS", "NZL", "NLD" -> new Color(0x3B82F6);
            case "FRA", "ESP", "PRT", "ITA", "BEL", "ROU", "MEX", "COL", "ARG", "PER", "CHL", "ECU", "GTM", "CUB", "BOL", "DOM", "HND", "PRY", "SLV", "NIC", "CRI", "PAN", "URY", "VEN" -> new Color(0xEC4899);
            case "DEU", "AUT", "CHE", "SWE", "NOR", "DNK", "FIN", "ISL", "JPN", "KOR" -> new Color(0x8B5CF6);
            case "RUS", "BLR", "UKR", "CHN", "VNM", "SRB", "BGR", "MKD", "BIH", "MNE", "KAZ", "UZB", "TKM", "KGZ", "TJK", "MNG" -> new Color(0xDC2626);
            case "SAU", "EGY", "DZA", "MAR", "IRQ", "SYR", "YEM", "TUN", "JOR", "ARE", "LBN", "LBY", "OMN", "KWT", "QAT", "BHR", "AFG", "PAK", "IRN", "TUR", "MRT", "SDN", "SOM" -> new Color(0x10B981);
            case "NGA", "COD", "TZA", "KEN", "ZAF", "CIV", "CMR", "UGA", "ZMB", "SEN", "ZWE", "MLI", "GIN", "RWA", "BEN", "BDI", "TGO", "BFA", "COG", "GAB", "ETH" -> new Color(0x15803D);
            case "GHA", "MOZ" -> new Color(0xF43F5E);
            case "IND", "BGD", "NPL" -> new Color(0xF59E0B);
            case "IDN", "MYS", "PHL", "THA", "MMR", "KHM", "LAO", "MDG", "BRN" -> new Color(0x06B6D4);
            default -> new Color(0x8B5CF6);
        };
    }

    public static Color computeRitualColor(String iso, String name, String subregion, String continent) {
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "FRA", "ITA", "ESP", "PRT", "POL", "IRL", "AUT", "BEL", "LTU", "HRV", "SVN", "SVK", "HUN",
                 "MEX", "BRA", "COL", "ARG", "PER", "CHL", "ECU", "GTM", "CUB", "BOL", "DOM", "HND", "PRY", "SLV", "NIC", "CRI", "PAN", "URY", "VEN", "PHL", "TLS" -> new Color(0xEC4899);
            case "USA", "GBR", "DEU", "CAN", "AUS", "NZL", "SWE", "NOR", "DNK", "FIN", "ISL", "NLD", "CHE", "ZAF", "KEN", "NGA", "GHA", "UGA", "ZWE", "ZMB", "PNG" -> new Color(0x38BDF8);
            case "RUS", "UKR", "BLR", "GRC", "ROU", "BGR", "SRB", "GEO", "MDA", "CYP", "MNE", "MKD", "ETH", "ERI" -> new Color(0x8B5CF6);
            case "SAU", "EGY", "TUR", "IDN", "PAK", "BGD", "DZA", "MAR", "SDN", "SYR", "YEM", "TUN", "JOR", "ARE", "LBY", "OMN", "KWT", "QAT", "BHR", "MYS", "KAZ", "UZB", "TKM", "KGZ", "TJK", "AFG", "SEN", "MLI", "NER", "SOM", "MRT" -> new Color(0x10B981);
            case "IRN", "IRQ", "AZE" -> new Color(0x0D9488);
            case "IND", "NPL", "MUS" -> new Color(0xF59E0B);
            case "THA", "MMR", "LKA", "KHM", "LAO" -> new Color(0xEAB308);
            case "CHN", "JPN", "KOR", "TWN", "VNM", "SGP", "HKG" -> new Color(0xEF4444);
            case "MNG", "BTN" -> new Color(0xD97706);
            case "ISR" -> new Color(0x2563EB);
            default -> new Color(0x64748B);
        };
    }

    public static int computeInstitutionalLevel(String iso, String name) {
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "USA", "CAN", "GBR", "FRA", "DEU", "JPN", "CHE", "SWE", "NOR", "DNK", "FIN", "NLD", "AUS", "NZL", "SGP" -> 220;
            case "ITA", "ESP", "PRT", "AUT", "BEL", "IRL", "KOR", "TWN", "ISR", "POL", "CZE", "EST" -> 205;
            case "CHN", "RUS", "IND", "BRA", "MEX", "TUR", "SAU", "ZAF", "MYS", "CHL", "ARG", "ROU", "GRC", "IDN", "THA", "VNM" -> 175;
            case "EGY", "NGA", "PAK", "BGD", "COL", "PER", "PHL", "UKR", "KAZ", "DZA", "MAR", "KEN" -> 135;
            default -> 95;
        };
    }

    public static int computeTechnologyLevel(String iso, String name) {
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "USA", "JPN", "DEU", "KOR", "TWN", "CHE", "GBR", "FRA", "SWE", "NLD", "SGP", "ISR", "CHN" -> 250;
            case "CAN", "AUS", "AUT", "BEL", "DNK", "FIN", "NOR", "ITA", "ESP", "IRL", "NZL" -> 220;
            case "RUS", "IND", "BRA", "POL", "CZE", "TUR", "SAU", "MYS", "MEX", "EST", "HUN", "PRT" -> 175;
            case "IDN", "THA", "VNM", "ZAF", "EGY", "ARG", "CHL", "COL", "PHL", "ROU", "GRC", "UKR" -> 130;
            default -> 80;
        };
    }

    public static int computeEcologicalFootprint(String iso, String name) {
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        return switch (code) {
            case "USA", "CHN", "IND", "RUS", "DEU", "JPN", "KOR", "SAU", "CAN", "AUS" -> 240;
            case "GBR", "FRA", "ITA", "BRA", "MEX", "IDN", "TUR", "IRN", "ZAF", "POL", "NLD", "ESP" -> 190;
            case "EGY", "PAK", "BGD", "VNM", "THA", "NGA", "ARG", "COL", "UKR", "MYS", "PHL", "BEL" -> 140;
            default -> 80;
        };
    }

    public static int computePathogenStress(String iso, String name, String continent) {
        String code = iso != null ? iso.toUpperCase(Locale.ROOT) : "";
        if ("Africa".equalsIgnoreCase(continent)) return 210;
        if ("South America".equalsIgnoreCase(continent)) return 140;
        return switch (code) {
            case "IND", "BGD", "PAK", "IDN", "PHL", "VNM", "MMR", "KHM", "PNG" -> 180;
            case "CHN", "BRA", "MEX", "EGY", "THA", "MYS", "COL", "PER" -> 110;
            case "USA", "CAN", "GBR", "FRA", "DEU", "JPN", "AUS", "NZL", "SWE", "NOR", "DNK", "FIN", "CHE", "NLD", "ITA", "ESP", "RUS", "KOR" -> 40;
            default -> 90;
        };
    }
}
