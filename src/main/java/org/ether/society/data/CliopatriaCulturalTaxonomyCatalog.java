/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.Color;

/**
 * Cultural, Linguistic, Kinship, and Trait Taxonomy Catalog for Seshat ClioPatria Polities.
 *
 * <p>Extracted from {@link CliopatriaPolityVectorReader} to satisfy the Single Responsibility Principle (SRP).
 * Maps historical polity names and Seshat IDs to authentic cartographic color palettes
 * and 4D cliodynamic trait vectors (language, kinship, subsistence, religion).</p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class CliopatriaCulturalTaxonomyCatalog {

    private CliopatriaCulturalTaxonomyCatalog() {
        // Pure static catalog
    }

    /**
     * Normalize strings for resilient historical keyword matching.
     *
     * @param s string to normalize
     * @return normalized lower-case ASCII string without diacritics
     */
    public static String normalize(String s) {
        if (s == null) return "";
        String clean = s.replace('đ', 'd').replace('Đ', 'd');
        String n = java.text.Normalizer.normalize(clean, java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    /**
     * Map historical polity name to authentic Isogloss (Linguistic Phylum / Branch) color.
     *
     * @param name polity name
     * @param seshatId Seshat unique ID
     * @return color corresponding to linguistic phylum
     */
    public static Color getPolityIsoglossColor(String name, String seshatId) {
        if (name == null) return new Color(0x2563EB);
        String lower = normalize(name);

        // 1. Sinitic & Tibeto-Burman
        if (lower.contains("qing") || lower.contains("china") || lower.contains("chinese")) return new Color(0xEF4444); // Sinitic Red
        if (lower.contains("burma") || lower.contains("konbaung") || lower.contains("tibetan") || lower.contains("tibet") ||
            lower.contains("bhutan")) return new Color(0xDC2626); // Tibeto-Burman Brick Red

        // 2. Japonic & Koreanic
        if (lower.contains("japan") || lower.contains("tokugawa") || lower.contains("edo")) return new Color(0xF43F5E); // Japonic Rose
        if (lower.contains("korea") || lower.contains("joseon")) return new Color(0xA855F7); // Koreanic Violet

        // 3. Indo-Aryan & Dravidian
        if (lower.contains("maratha") || lower.contains("mughal") || lower.contains("sikh") ||
            lower.contains("awadh") || lower.contains("bengal") || lower.contains("nepal") || lower.contains("sindh") ||
            lower.contains("india") || lower.contains("ceylon")) return new Color(0xF59E0B); // Indo-Aryan Amber
        if (lower.contains("carnatic") || lower.contains("mysore") || lower.contains("travancore") || lower.contains("hyderabad")) return new Color(0xB45309); // Dravidian Bronze

        // 4. Iranic
        if (lower.contains("persia") || lower.contains("qajar") || lower.contains("iran") ||
            lower.contains("durrani") || lower.contains("afghan") || lower.contains("baloch") || lower.contains("pashto")) return new Color(0xD97706); // Iranic Orange-Brown

        // 5. Turkic
        if (lower.contains("ottoman") || lower.contains("turk") || lower.contains("bukhara") ||
            lower.contains("khiva") || lower.contains("kokand") || lower.contains("kazakh") || lower.contains("tatar")) return new Color(0x0EA5E9); // Turkic Sky Cyan

        // 6. Kartvelian & Caucasian
        if (lower.contains("georgia") || lower.contains("guria") || lower.contains("imereti") ||
            lower.contains("mingrelia") || lower.contains("abkhazia") || lower.contains("svaneti")) return new Color(0x84CC16); // Kartvelian / Caucasian Lime

        // 7. Hellenic
        if (lower.contains("septinsular") || lower.contains("greek") || lower.contains("hellen")) return new Color(0x06B6D4); // Hellenic Cyan

        // 8. Uralic
        if (lower.contains("hungar") || lower.contains("magyar") || lower.contains("finland") || lower.contains("finn") || lower.contains("estonia")) return new Color(0x14B8A6); // Uralic Teal

        // 9. Prussian & German / Scandinavian / Anglo-Saxon (Check Prussia BEFORE Slavic russ!)
        if (lower.contains("prussia") || lower.contains("brandenburg") ||
            lower.contains("brit") || lower.contains("england") || lower.contains("great britain") ||
            lower.contains("united kingdom") || lower.contains("united states") || lower.contains("america") ||
            lower.contains("austria") || lower.contains("habsburg") || lower.contains("holy roman") ||
            lower.contains("bavaria") || lower.contains("saxony") || lower.contains("hanover") ||
            lower.contains("baden") || lower.contains("wurttemberg") || lower.contains("hesse") ||
            lower.contains("netherland") || lower.contains("dutch") || lower.contains("batavia") ||
            lower.contains("sweden") || lower.contains("denmark") || lower.contains("norway") ||
            lower.contains("greenland") || lower.contains("swiss") || lower.contains("helvetic")) return new Color(0x2563EB); // Germanic Royal Blue

        // 10. Slavic (Careful: exclude prussia)
        if (lower.contains("russ") || lower.contains("slav") || lower.contains("serb") ||
            lower.contains("bulgar") || lower.contains("poland") || lower.contains("bohemia") ||
            lower.contains("czech") || lower.contains("ukrain") || lower.contains("belarus")) return new Color(0x8B5CF6); // Slavic Purple

        // 11. Romance / Latin Branch (France, Spain, Portugal, Italy, Romania, etc.)
        if (lower.contains("franc") || lower.contains("french") || lower.contains("consulate") ||
            lower.contains("spain") || lower.contains("spanish") || lower.contains("castil") || lower.contains("urgell") ||
            lower.contains("portug") || lower.contains("brazil") ||
            lower.contains("ital") || lower.contains("naples") || lower.contains("sardinia") ||
            lower.contains("tuscany") || lower.contains("papal") || lower.contains("cispadane") ||
            lower.contains("cisalpine") || lower.contains("haiti") || lower.contains("moldavia") ||
            lower.contains("wallachia") || lower.contains("romania") || lower.contains("catalan") ||
            lower.contains("venice") || lower.contains("genoa") || lower.contains("parma") ||
            lower.contains("modena") || lower.contains("lucca") || lower.contains("ragusa") ||
            lower.contains("louisiana")) return new Color(0xEC4899); // Romance Vibrant Magenta/Rose

        // 12. Celtic & Basque
        if (lower.contains("celt") || lower.contains("irish") || lower.contains("gael") ||
            lower.contains("welsh") || lower.contains("breton")) return new Color(0x10B981); // Celtic Emerald
        if (lower.contains("basque") || lower.contains("eusk")) return new Color(0xF59E0B);

        // 13. Austroasiatic & Tai-Kadai
        if (lower.contains("vietnam") || lower.contains("nguyen") || lower.contains("tay son") ||
            lower.contains("dai viet") || lower.contains("khmer") || lower.contains("cambodia")) return new Color(0x16A085);
        if (lower.contains("siam") || lower.contains("rattanakosin") || lower.contains("chiangmai") ||
            lower.contains("luang phrabang") || lower.contains("vientiane") || lower.contains("champasak") || lower.contains("thai")) return new Color(0xEAB308);

        // 14. Austronesian (Malay, Indonesian, Polynesian, Malagasy)
        if (lower.contains("banjar") || lower.contains("aceh") || lower.contains("perak") ||
            lower.contains("johor") || lower.contains("maguindanao") || lower.contains("sulu") ||
            lower.contains("yogyakarta") || lower.contains("surakarta") || lower.contains("banten") ||
            lower.contains("bone") || lower.contains("palembang") || lower.contains("jambi") ||
            lower.contains("brunei") || lower.contains("bali") || lower.contains("hawai") ||
            lower.contains("merina") || lower.contains("madagascar") || lower.contains("malay") ||
            lower.contains("malacca") || lower.contains("selangor") || lower.contains("kedah") ||
            lower.contains("negeri sembilan") || lower.contains("pontianak") || lower.contains("mempawah") ||
            lower.contains("sambas") || lower.contains("sukadana") || lower.contains("bima")) return new Color(0x06B6D4);

        // 15. Semitic & Afroasiatic
        if (lower.contains("morocco") || lower.contains("alaouite") || lower.contains("oman") ||
            lower.contains("diriyah") || lower.contains("kuwait") || lower.contains("qasimid") ||
            lower.contains("algiers") || lower.contains("tunis") || lower.contains("tripolitania") ||
            lower.contains("karamanli") || lower.contains("ethiopia") || lower.contains("egypt") || lower.contains("arab")) return new Color(0x15803D);

        // 16. Niger-Congo & Nilo-Saharan
        if (lower.contains("sokoto") || lower.contains("bornu") || lower.contains("oyo") ||
            lower.contains("ashanti") || lower.contains("bamana") || lower.contains("wadai") ||
            lower.contains("darfur") || lower.contains("funj") || lower.contains("lunda") ||
            lower.contains("kongo") || lower.contains("zulu") || lower.contains("xhosa") || lower.contains("dahomey")) return new Color(0x22C55E);

        return new Color(0x2563EB);
    }

    /**
     * Map historical polity name to authentic Kinship & Social Structure color.
     *
     * @param name polity name
     * @param seshatId Seshat unique ID
     * @return color corresponding to kinship typology
     */
    public static Color getPolityKinshipColor(String name, String seshatId) {
        if (name == null) return new Color(0x8B5CF6);
        String lower = normalize(name);

        // 1. Egalitarian Nuclear Family & Civil Code / Partible Inheritance (#EC4899)
        if (lower.contains("franc") || lower.contains("french") || lower.contains("consulate") ||
            lower.contains("louisiana") || lower.contains("cispadane") || lower.contains("cisalpine") ||
            lower.contains("tuscany") || lower.contains("papal") || lower.contains("sardinia") ||
            lower.contains("naples") || lower.contains("two sicilies") || lower.contains("spain") ||
            lower.contains("spanish") || lower.contains("castil") || lower.contains("urgell") ||
            lower.contains("portug") || lower.contains("brazil") || lower.contains("haiti") ||
            lower.contains("belgium") || lower.contains("wallonia") || lower.contains("ragusa")) return new Color(0xEC4899);

        // 2. Absolute Nuclear Family & Proletarian Wage Labor (#3B82F6)
        if (lower.contains("brit") || lower.contains("england") || lower.contains("great britain") ||
            lower.contains("united kingdom") || lower.contains("united states") || lower.contains("america") ||
            lower.contains("netherland") || lower.contains("dutch") || lower.contains("batavia") ||
            lower.contains("cape colony") || lower.contains("ceylon")) return new Color(0x3B82F6);

        // 3. Stem Family & Primogeniture (Stammfamilie / Famille Souche / Ie) (#8B5CF6)
        if (lower.contains("prussia") || lower.contains("brandenburg") ||
            lower.contains("austria") || lower.contains("habsburg") || lower.contains("holy roman") ||
            lower.contains("bavaria") || lower.contains("saxony") || lower.contains("baden") ||
            lower.contains("wurttemberg") || lower.contains("hesse") || lower.contains("hanover") ||
            lower.contains("sweden") || lower.contains("denmark") || lower.contains("norway") ||
            lower.contains("greenland") || lower.contains("swiss") || lower.contains("helvetic") ||
            lower.contains("japan") || lower.contains("tokugawa") || lower.contains("edo") ||
            lower.contains("basque")) return new Color(0x8B5CF6);

        // 4. Patriarchal Joint Family & Peasant Commune (Mir / Obshchina / Zadruga) (#DC2626)
        if (lower.contains("russ") || lower.contains("slav") || lower.contains("serb") ||
            lower.contains("bulgar") || lower.contains("poland") || lower.contains("moldavia") ||
            lower.contains("wallachia") || lower.contains("ukrain") || lower.contains("bohemia") ||
            lower.contains("czech")) return new Color(0xDC2626);

        // 5. Agnatic Lineage & Confucian Zongzu Clan (#EF4444)
        if (lower.contains("qing") || lower.contains("china") || lower.contains("chinese") ||
            lower.contains("korea") || lower.contains("joseon") || lower.contains("vietnam") ||
            lower.contains("nguyen") || lower.contains("tay son") || lower.contains("dai viet")) return new Color(0xEF4444);

        // 6. Segmentary Patrilineal Lineage & Endogamous Clan (#10B981)
        if (lower.contains("ottoman") || lower.contains("turk") || lower.contains("morocco") ||
            lower.contains("alaouite") || lower.contains("oman") || lower.contains("diriyah") ||
            lower.contains("kuwait") || lower.contains("qasimid") || lower.contains("algiers") ||
            lower.contains("tunis") || lower.contains("tripolitania") || lower.contains("karamanli") ||
            lower.contains("persia") || lower.contains("qajar") || lower.contains("iran") ||
            lower.contains("durrani") || lower.contains("afghan") || lower.contains("kazakh") ||
            lower.contains("bukhara") || lower.contains("khiva") || lower.contains("kokand")) return new Color(0x10B981);

        // 7. Gotra Exogamous Patrilineal Joint Family & Jati Caste Endogamy (#F59E0B)
        if (lower.contains("maratha") || lower.contains("mughal") || lower.contains("sikh") ||
            lower.contains("awadh") || lower.contains("bengal") || lower.contains("carnatic") ||
            lower.contains("mysore") || lower.contains("travancore") || lower.contains("nepal") ||
            lower.contains("india") || lower.contains("hyderabad")) return new Color(0xF59E0B);

        // 8. Bilateral Matrifocal & Malay Nusantara / Southeast Asia (#06B6D4)
        if (lower.contains("siam") || lower.contains("rattanakosin") || lower.contains("burma") ||
            lower.contains("konbaung") || lower.contains("khmer") || lower.contains("cambodia") ||
            lower.contains("chiangmai") || lower.contains("luang phrabang") || lower.contains("vientiane") ||
            lower.contains("champasak") || lower.contains("banjar") || lower.contains("aceh") ||
            lower.contains("johor") || lower.contains("yogyakarta") || lower.contains("surakarta") ||
            lower.contains("banten") || lower.contains("brunei") || lower.contains("bali") ||
            lower.contains("hawai") || lower.contains("merina") || lower.contains("madagascar") ||
            lower.contains("malay") || lower.contains("malacca") || lower.contains("selangor") ||
            lower.contains("kedah") || lower.contains("negeri sembilan") || lower.contains("pontianak") ||
            lower.contains("mempawah") || lower.contains("sambas") || lower.contains("sukadana") ||
            lower.contains("bima") || lower.contains("bone") || lower.contains("palembang") ||
            lower.contains("jambi") || lower.contains("maguindanao") || lower.contains("sulu") ||
            lower.contains("septinsular")) return new Color(0x06B6D4);

        // 9. African Segmentary Lineages & Age-Set Organizations (#15803D)
        if (lower.contains("sokoto") || lower.contains("bornu") || lower.contains("oyo") ||
            lower.contains("ashanti") || lower.contains("bamana") || lower.contains("wadai") ||
            lower.contains("darfur") || lower.contains("funj") || lower.contains("lunda") ||
            lower.contains("kongo") || lower.contains("zulu") || lower.contains("xhosa") ||
            lower.contains("dahomey") || lower.contains("ethiopia")) return new Color(0x15803D);

        // 10. Caucasian Mountain Clan Lineages (#84CC16)
        if (lower.contains("georgia") || lower.contains("guria") || lower.contains("imereti") ||
            lower.contains("mingrelia") || lower.contains("abkhazia") || lower.contains("svaneti")) return new Color(0x84CC16);

        return new Color(0x8B5CF6);
    }

    /**
     * Map historical polity name to official hex color matching cultural_registry.json.
     *
     * @param name polity name
     * @param seshatId Seshat unique ID
     * @return sovereign color
     */
    public static Color getPolityColor(String name, String seshatId) {
        if (name == null) return new Color(0x64748B);
        String lower = normalize(name);

        // 1. British Empire & Dominions (#DC2626)
        if (lower.contains("great britain") || lower.contains("kingdom of great britain") ||
            lower.contains("british") || lower.contains("england") || lower.contains("united kingdom") ||
            lower.contains("cape colony") || lower.contains("ceylon") || lower.contains("new south wales")) return new Color(0xDC2626);

        // 2. French Republic, Consulate & Dependencies (#2563EB)
        if (lower.contains("french") || lower.contains("france") || lower.contains("consulate") ||
            lower.contains("louisiana") || lower.contains("cispadane") || lower.contains("cisalpine")) return new Color(0x2563EB);

        // 3. Kingdom of Prussia (#1E293B)
        if (lower.contains("prussia") || lower.contains("brandenburg")) return new Color(0x1E293B);

        // 4. Habsburg Monarchy (#F59E0B)
        if (lower.contains("austria") || lower.contains("habsburg") || lower.contains("holy roman")) return new Color(0xF59E0B);

        // 5. German Secondary States
        if (lower.contains("bavaria")) return new Color(0x38BDF8); // Bavarian Blue
        if (lower.contains("saxony")) return new Color(0x4ADE80);  // Saxon Green
        if (lower.contains("hanover")) return new Color(0xFB923C); // Hanover Amber
        if (lower.contains("baden") || lower.contains("wurttemberg") || lower.contains("hesse") ||
            lower.contains("nassau") || lower.contains("brunswick") || lower.contains("anhalt") ||
            lower.contains("liechtenstein")) return new Color(0xFBBF24);

        // 6. Scandinavian Kingdoms
        if (lower.contains("denmark")) return new Color(0xB91C1C); // Danish Crimson
        if (lower.contains("sweden")) return new Color(0x0284C7);  // Swedish Blue
        if (lower.contains("greenland")) return new Color(0x94A3B8);

        // 7. Swiss Confederation / Helvetic Republic (#EF4444)
        if (lower.contains("swiss") || lower.contains("helvetic")) return new Color(0xEF4444);

        // 8. Dutch / Batavian Republic (#F97316)
        if (lower.contains("netherland") || lower.contains("dutch") || lower.contains("batavia") ||
            lower.contains("new netherland")) return new Color(0xF97316);

        // 9. Italian States
        if (lower.contains("sardinia")) return new Color(0xEC4899);
        if (lower.contains("naples") || lower.contains("two sicilies")) return new Color(0xB45309);
        if (lower.contains("papal")) return new Color(0xEAB308);
        if (lower.contains("tuscany")) return new Color(0xA855F7);
        if (lower.contains("ragusa")) return new Color(0x14B8A6);

        // 10. Spanish Empire & Viceroyalties (#EA580C)
        if (lower.contains("spain") || lower.contains("spanish") || lower.contains("castil") ||
            lower.contains("urgell")) return new Color(0xEA580C);

        // 11. Portuguese Empire & Colonies (#10B981)
        if (lower.contains("portug") || lower.contains("brazil")) return new Color(0x10B981);

        // 12. Russian Empire (#7C3AED)
        if (lower.contains("russ") || lower.contains("alaska")) return new Color(0x7C3AED);

        // 13. Caucasian Principalities (#84CC16)
        if (lower.contains("georgia") || lower.contains("guria") || lower.contains("imereti") ||
            lower.contains("mingrelia") || lower.contains("abkhazia") || lower.contains("svaneti")) return new Color(0x84CC16);

        // 14. Ottoman Empire & North African Regencies (#059669)
        if (lower.contains("ottoman") || lower.contains("turk") || lower.contains("algiers") ||
            lower.contains("tunis") || lower.contains("tripolitania") || lower.contains("karamanli") ||
            lower.contains("moldavia") || lower.contains("wallachia") || lower.contains("septinsular")) return new Color(0x059669);

        // 15. United States of America (#3B82F6)
        if (lower.contains("united states") || lower.contains("america") || lower.contains("haiti") ||
            lower.contains("cuba")) return new Color(0x3B82F6);

        // 16. Qing Empire (#EF4444)
        if (lower.contains("qing") || lower.contains("china") || lower.contains("chinese")) return new Color(0xEF4444);

        // 17. Joseon Korea (#8B5CF6)
        if (lower.contains("korea") || lower.contains("joseon")) return new Color(0x8B5CF6);

        // 18. Tokugawa Shogunate (#E11D48)
        if (lower.contains("japan") || lower.contains("tokugawa") || lower.contains("edo")) return new Color(0xE11D48);

        // 19. Indian Subcontinent Polities
        if (lower.contains("maratha")) return new Color(0xF59E0B);
        if (lower.contains("mughal")) return new Color(0xD97706);
        if (lower.contains("sikh")) return new Color(0xFBBF24);
        if (lower.contains("awadh") || lower.contains("carnatic") || lower.contains("mysore") ||
            lower.contains("travancore") || lower.contains("hyderabad") || lower.contains("nepal") ||
            lower.contains("india") || lower.contains("bhutan")) return new Color(0xD97706);

        // 20. Iranian & Central Asian States
        if (lower.contains("persia") || lower.contains("qajar") || lower.contains("iran")) return new Color(0x0891B2);
        if (lower.contains("durrani") || lower.contains("afghan") || lower.contains("bukhara") ||
            lower.contains("khiva") || lower.contains("kokand") || lower.contains("kazakh")) return new Color(0x0284C7);

        // 21. Southeast Asian Kingdoms
        if (lower.contains("siam") || lower.contains("rattanakosin") || lower.contains("chiangmai") ||
            lower.contains("luang phrabang") || lower.contains("vientiane") || lower.contains("champasak") ||
            lower.contains("khmer") || lower.contains("cambodia") || lower.contains("burma") ||
            lower.contains("konbaung") || lower.contains("thai")) return new Color(0xF97316);

        // 22. Vietnam: Nguyen & Tay Son (#16A085)
        if (lower.contains("vietnam") || lower.contains("nguyen") || lower.contains("tay son") ||
            lower.contains("dai viet")) return new Color(0x16A085);

        // 23. Maritime Nusantara Sultanates (#047857)
        if (lower.contains("banjar") || lower.contains("aceh") || lower.contains("perak") ||
            lower.contains("johor") || lower.contains("maguindanao") || lower.contains("sulu") ||
            lower.contains("yogyakarta") || lower.contains("surakarta") || lower.contains("banten") ||
            lower.contains("bone") || lower.contains("palembang") || lower.contains("jambi") ||
            lower.contains("mempawah") || lower.contains("sambas") || lower.contains("sukadana") ||
            lower.contains("bima") || lower.contains("kutai") || lower.contains("pontianak") ||
            lower.contains("selangor") || lower.contains("terengganu") || lower.contains("kelantan") ||
            lower.contains("negeri sembilan") || lower.contains("brunei") || lower.contains("bali") ||
            lower.contains("malacca")) return new Color(0x047857);

        // 24. Sahelian & West/Central African Kingdoms (#15803D)
        if (lower.contains("sokoto") || lower.contains("hausa") || lower.contains("fulani") ||
            lower.contains("bornu") || lower.contains("oyo") || lower.contains("ashanti") ||
            lower.contains("bamana") || lower.contains("funj") || lower.contains("wadai") ||
            lower.contains("darfur") || lower.contains("lunda") || lower.contains("kongo")) return new Color(0x15803D);

        // 25. Ethiopian Empire & Madagascar (#84CC16)
        if (lower.contains("ethiopia") || lower.contains("abyssin") || lower.contains("merina") ||
            lower.contains("madagascar")) return new Color(0x84CC16);

        // 26. Morocco & Arabian Sultanates (#0D9488)
        if (lower.contains("morocco") || lower.contains("alaouite") || lower.contains("oman") ||
            lower.contains("diriyah") || lower.contains("kuwait") || lower.contains("qasimid") ||
            lower.contains("qatar") || lower.contains("shammar") || lower.contains("nejd")) return new Color(0x0D9488);

        // 27. Polynesian & Pacific Island Domains (#E67E22)
        if (lower.contains("hawai") || lower.contains("maori") || lower.contains("polynes")) return new Color(0xE67E22);

        // 28. Indigenous Polar & Arctic Domains (#607D8B)
        if (lower.contains("inuit") || lower.contains("thule")) return new Color(0x607D8B);

        // 29. Southern African Kingdoms & Free States (#F59E0B)
        if (lower.contains("zulu") || lower.contains("xhosa") || lower.contains("khoisan") ||
            lower.contains("orange free") || lower.contains("natalia") || lower.contains("south african republic") ||
            lower.contains("swaziland")) return new Color(0xF59E0B);

        // Deterministic distinct fallback palette based on name hash
        int hash = Math.abs(name.hashCode());
        float hue = (hash % 360) / 360.0f;
        float sat = 0.75f + (hash % 20) * 0.01f;
        float bri = 0.70f + (hash % 25) * 0.01f;
        return Color.getHSBColor(hue, sat, bri);
    }

    /**
     * Computes authentic 4D cliodynamic trait vector [linguisticBranch, socialStructure, subsistenceMode, ritualTradition]
     * for a historical polity based on Glottolog 4.8 linguistic phyla, Murdock D-PLACE kinship structures,
     * ArchaeoGLOBE subsistence regimes, and Seshat Databank institutional norms.
     *
     * @param name Name of historical polity
     * @param seshatId Seshat unique identifier (if present)
     * @param targetYear Target simulation epoch year
     * @return 4-dimensional normalized trait array in range [0.0, 1.0]
     */
    public static double[] computePolityTraits(String name, String seshatId, long targetYear) {
        if (name == null || name.isBlank()) {
            return new double[]{0.50, 0.50, 0.50, 0.50};
        }
        String lower = normalize(name);

        double t1 = 0.50; // Linguistic Phylum / Branch (Glottolog)
        double t2 = 0.60; // Social Structure & Kinship (Murdock SCCS / Todd)
        double t3 = 0.65; // Subsistence & Material Base (ArchaeoGLOBE / FAO)
        double t4 = 0.50; // Ritual & Axial Heritage (Seshat / Religious Systems)

        // 1. Linguistic Phylum (T1)
        if (lower.contains("roman") || lower.contains("rome") || lower.contains("ital") || lower.contains("latin") ||
            lower.contains("france") || lower.contains("french") || lower.contains("spain") || lower.contains("spanish") ||
            lower.contains("portug") || lower.contains("gaul") || lower.contains("castil")) {
            t1 = 0.20;
            t2 = 0.85;
            t3 = 0.78;
            t4 = (targetYear >= 380L) ? 0.92 : 0.40; // Monotheistic Christianity vs Civic Polytheism
        } else if (lower.contains("greek") || lower.contains("hellen") || lower.contains("athen") || lower.contains("sparta") ||
                   lower.contains("macedon") || lower.contains("seleucid") || lower.contains("ptolem") || lower.contains("byzant")) {
            t1 = 0.22;
            t2 = 0.80;
            t3 = 0.76;
            t4 = (targetYear >= 380L) ? 0.90 : 0.42;
        } else if (lower.contains("german") || lower.contains("prussia") || lower.contains("saxony") || lower.contains("bavaria") ||
                   lower.contains("england") || lower.contains("brit") || lower.contains("sweden") || lower.contains("denmark") ||
                   lower.contains("norse") || lower.contains("dutch") || lower.contains("austria") || lower.contains("habsburg")) {
            t1 = 0.24;
            t2 = (targetYear >= 1500L) ? 0.85 : 0.60;
            t3 = 0.72;
            t4 = (targetYear >= 800L) ? 0.90 : 0.25;
        } else if (lower.contains("russia") || lower.contains("rus") || lower.contains("poland") || lower.contains("bohemia") ||
                   lower.contains("serbia") || lower.contains("bulgar") || lower.contains("croat") || lower.contains("slav")) {
            t1 = 0.28;
            t2 = 0.65;
            t3 = 0.68;
            t4 = (targetYear >= 988L) ? 0.90 : 0.28;
        } else if (lower.contains("persia") || lower.contains("parth") || lower.contains("sasan") || lower.contains("achaemen") ||
                   lower.contains("iran") || lower.contains("qajar") || lower.contains("safavid") || lower.contains("sogd") ||
                   lower.contains("bactria") || lower.contains("kushan") || lower.contains("indo-greek")) {
            t1 = 0.33;
            t2 = 0.70;
            t3 = 0.70;
            t4 = (targetYear >= 650L) ? 0.92 : 0.82; // Zoroastrian Mazdayasna / Islamic Monotheism
        } else if (lower.contains("india") || lower.contains("maurya") || lower.contains("gupta") || lower.contains("chola") ||
                   lower.contains("harsha") || lower.contains("maratha") || lower.contains("mughal") || lower.contains("sikh") ||
                   lower.contains("cheras") || lower.contains("pandyas") || lower.contains("satavahana") || lower.contains("anuradhapura")) {
            t1 = (lower.contains("cheras") || lower.contains("pandyas") || lower.contains("chola") || lower.contains("anuradhapura")) ? 0.46 : 0.42;
            t2 = 0.75;
            t3 = 0.82; // Intensive monsoon agriculture
            t4 = (lower.contains("mughal")) ? 0.92 : 0.58; // Dharmic / Vedic / Buddhist vs Islamic
        } else if (lower.contains("judea") || lower.contains("nabataea") || lower.contains("himyar") || lower.contains("hadhramaut") ||
                   lower.contains("arab") || lower.contains("caliph") || lower.contains("umayyad") || lower.contains("abbasid") ||
                   lower.contains("fatimid") || lower.contains("mamluk") || lower.contains("phoenic") || lower.contains("carthag") ||
                   lower.contains("assyria") || lower.contains("babylon") || lower.contains("egypt") || lower.contains("kush") ||
                   lower.contains("axum") || lower.contains("morocco") || lower.contains("ottoman")) {
            t1 = 0.12; // Afroasiatic / Semitic
            t2 = (lower.contains("egypt") || lower.contains("caliph") || lower.contains("ottoman")) ? 0.88 : 0.55;
            t3 = (lower.contains("egypt") || lower.contains("babylon") || lower.contains("assyria")) ? 0.90 : 0.65;
            t4 = (targetYear >= 600L || lower.contains("judea") || lower.contains("axum") || lower.contains("himyar")) ? 0.95 : 0.45;
        } else if (lower.contains("mali") || lower.contains("songhai") || lower.contains("ghana") || lower.contains("kanem") ||
                   lower.contains("oyo") || lower.contains("benin") || lower.contains("kongo") || lower.contains("zulu") ||
                   lower.contains("ashanti") || lower.contains("ethiopia")) {
            t1 = 0.08; // Niger-Congo / Nilo-Saharan
            t2 = 0.60;
            t3 = 0.62;
            t4 = (lower.contains("mali") || lower.contains("songhai")) ? 0.90 : (lower.contains("ethiopia") ? 0.92 : 0.22);
        } else if (lower.contains("mongol") || lower.contains("xiongnu") || lower.contains("hun") || lower.contains("turk") ||
                   lower.contains("gokturk") || lower.contains("khazar") || lower.contains("scyth") || lower.contains("sarmat") ||
                   lower.contains("kazakh") || lower.contains("jurchen") || lower.contains("manchu")) {
            t1 = 0.58; // Steppe Altaic
            t2 = 0.40; // Nomadic Clan Federations
            t3 = 0.32; // Pastoral Transhumance
            t4 = (lower.contains("khazar") ? 0.92 : (lower.contains("mongol") && targetYear >= 1260L ? 0.60 : 0.18)); // Tengrism / Shamanism
        } else if (lower.contains("china") || lower.contains("han") || lower.contains("tang") || lower.contains("song") ||
                   lower.contains("ming") || lower.contains("qing") || lower.contains("zhou") || lower.contains("shang") ||
                   lower.contains("qin") || lower.contains("wei") || lower.contains("jin") || lower.contains("tibet")) {
            t1 = 0.72; // Sino-Tibetan
            t2 = 0.92; // Mandarinate / Bureaucratic state
            t3 = 0.90; // Intensive hydraulic rice/millet agriculture
            t4 = 0.76; // Confucian-Legalist & Taoist
        } else if (lower.contains("japan") || lower.contains("yamato") || lower.contains("tokugawa") || lower.contains("edo") ||
                   lower.contains("korea") || lower.contains("goguryeo") || lower.contains("baekje") || lower.contains("silla") ||
                   lower.contains("joseon") || lower.contains("goryeo")) {
            t1 = 0.76;
            t2 = 0.85;
            t3 = 0.85;
            t4 = 0.72;
        } else if (lower.contains("vietnam") || lower.contains("dai viet") || lower.contains("champa") || lower.contains("funan") ||
                   lower.contains("khmer") || lower.contains("siam") || lower.contains("ayutthaya") || lower.contains("burma") ||
                   lower.contains("srivijaya") || lower.contains("majapahit") || lower.contains("malacca") || lower.contains("aceh") ||
                   lower.contains("brunei") || lower.contains("mataram")) {
            t1 = (lower.contains("srivijaya") || lower.contains("majapahit") || lower.contains("malacca") || lower.contains("aceh")) ? 0.84 : 0.79;
            t2 = 0.72;
            t3 = 0.82; // Maritime trade & intensive wet-rice
            t4 = (lower.contains("malacca") || lower.contains("aceh")) ? 0.92 : 0.60; // Dharmic / Buddhist vs Islamic
        } else if (lower.contains("maya") || lower.contains("aztec") || lower.contains("inca") || lower.contains("olmec") ||
                   lower.contains("teotihuacan") || lower.contains("toltec") || lower.contains("zapotec") || lower.contains("moche") ||
                   lower.contains("tiwanaku") || lower.contains("chimor") || lower.contains("mississippian") || lower.contains("pueblo")) {
            t1 = 0.92; // Indigenous Americas
            t2 = (lower.contains("inca") || lower.contains("aztec") || lower.contains("teotihuacan")) ? 0.88 : 0.70;
            t3 = 0.78; // Terracing, chinampas, maize agriculture
            t4 = 0.38; // Solar / Divine Polytheistic cosmology
        } else {
            // Deterministic hash embedding for long-tail unclassified polities
            int hash = Math.abs(name.hashCode());
            t1 = 0.15 + ((hash % 700) / 1000.0);
            t2 = 0.30 + (((hash / 7) % 600) / 1000.0);
            t3 = 0.35 + (((hash / 49) % 550) / 1000.0);
            t4 = 0.20 + (((hash / 343) % 700) / 1000.0);
        }

        return new double[]{
            Math.clamp(t1, 0.05, 0.98),
            Math.clamp(t2, 0.10, 0.98),
            Math.clamp(t3, 0.10, 0.98),
            Math.clamp(t4, 0.10, 0.98)
        };
    }
}
