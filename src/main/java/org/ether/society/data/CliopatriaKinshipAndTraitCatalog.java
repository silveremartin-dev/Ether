/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.data;

import java.awt.Color;

import static org.ether.society.data.CliopatriaCulturalTaxonomyCatalog.normalize;

/**
 * Kinship family systems and 4D cliodynamic trait vector mappings for historical polities.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class CliopatriaKinshipAndTraitCatalog {

    private CliopatriaKinshipAndTraitCatalog() {}

    /**
     * Map historical polity name to authentic Kinship / Family System color (Todd / Murdock taxonomy).
     */
    public static Color getPolityKinshipColor(String name, String seshatId) {
        if (name == null) return new Color(0x64748B);
        String lower = normalize(name);

        // 1. Absolute Nuclear Family (#3B82F6)
        if (lower.contains("great britain") || lower.contains("kingdom of great britain") ||
            lower.contains("british") || lower.contains("england") || lower.contains("united kingdom") ||
            lower.contains("denmark") || lower.contains("netherland") || lower.contains("dutch") ||
            lower.contains("batavia") || lower.contains("new south wales") || lower.contains("cape colony") ||
            lower.contains("united states") || lower.contains("canada")) return new Color(0x3B82F6);

        // 2. Egalitarian Nuclear Family (#EC4899)
        if (lower.contains("french") || lower.contains("france") || lower.contains("consulate") ||
            lower.contains("spain") || lower.contains("spanish") || lower.contains("portug") ||
            lower.contains("italy") || lower.contains("sardinia") || lower.contains("naples") ||
            lower.contains("tuscany") || lower.contains("papal") || lower.contains("cisalpine") ||
            lower.contains("cispadane") || lower.contains("poland") || lower.contains("romania") ||
            lower.contains("greece") || lower.contains("mexico") || lower.contains("colombia") ||
            lower.contains("peru") || lower.contains("chile") || lower.contains("brazil")) return new Color(0xEC4899);

        // 3. Stem Family (Famille Souche / Patrilocal Primogeniture) (#EAB308)
        if (lower.contains("prussia") || lower.contains("brandenburg") || lower.contains("germany") ||
            lower.contains("bavaria") || lower.contains("saxony") || lower.contains("hanover") ||
            lower.contains("austria") || lower.contains("habsburg") || lower.contains("sweden") ||
            lower.contains("norway") || lower.contains("switzerland") || lower.contains("swiss") ||
            lower.contains("japan") || lower.contains("tokugawa") || lower.contains("edo") ||
            lower.contains("korea") || lower.contains("joseon") || lower.contains("finland")) return new Color(0xEAB308);

        // 4. Communitarian Exogamous Family (#EF4444)
        if (lower.contains("russia") || lower.contains("russian") || lower.contains("muscovy") ||
            lower.contains("serbia") || lower.contains("bulgaria") || lower.contains("china") ||
            lower.contains("qing") || lower.contains("vietnam") || lower.contains("albania") ||
            lower.contains("mongol") || lower.contains("tibet") || lower.contains("manchu")) return new Color(0xEF4444);

        // 5. Anomic Family (#8B5CF6)
        if (lower.contains("cambodia") || lower.contains("khmer") || lower.contains("laos") ||
            lower.contains("thailand") || lower.contains("siam") || lower.contains("burma") ||
            lower.contains("indonesia") || lower.contains("philippines")) return new Color(0x8B5CF6);

        // 6. Communitarian Endogamous Family (#10B981)
        if (lower.contains("ottoman") || lower.contains("turk") || lower.contains("egypt") ||
            lower.contains("morocco") || lower.contains("algeria") || lower.contains("dey") ||
            lower.contains("tunis") || lower.contains("tripolitania") || lower.contains("karamanli") ||
            lower.contains("persia") || lower.contains("qajar") || lower.contains("iran") ||
            lower.contains("durrani") || lower.contains("afghan") || lower.contains("kazakh") ||
            lower.contains("bukhara") || lower.contains("khiva") || lower.contains("kokand")) return new Color(0x10B981);

        // 7. Gotra Exogamous Patrilineal Joint Family (#F59E0B)
        if (lower.contains("maratha") || lower.contains("mughal") || lower.contains("sikh") ||
            lower.contains("awadh") || lower.contains("bengal") || lower.contains("carnatic") ||
            lower.contains("mysore") || lower.contains("travancore") || lower.contains("nepal") ||
            lower.contains("india") || lower.contains("hyderabad")) return new Color(0xF59E0B);

        // 8. Bilateral Matrifocal & Malay Nusantara (#06B6D4)
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

        // 9. African Segmentary Lineages (#15803D)
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
     * Derives four-dimensional continuous cliodynamic trait vector for historical tensor rasters.
     */
    public static double[] getPolityCliodynamicTraits(String name, String seshatId, long targetYear) {
        if (name == null) {
            return new double[]{0.5, 0.5, 0.5, 0.5};
        }
        String lower = normalize(name);

        double t1;
        double t2 = 0.60;
        double t3 = 0.65;
        double t4 = 0.50;

        if (lower.contains("roman") || lower.contains("rome") || lower.contains("ital") || lower.contains("latin") ||
            lower.contains("france") || lower.contains("french") || lower.contains("spain") || lower.contains("spanish") ||
            lower.contains("portug") || lower.contains("gaul") || lower.contains("castil")) {
            t1 = 0.20; t2 = 0.85; t3 = 0.78;
            t4 = (targetYear >= 380L) ? 0.92 : 0.40;
        } else if (lower.contains("greek") || lower.contains("hellen") || lower.contains("athen") || lower.contains("sparta") ||
                   lower.contains("macedon") || lower.contains("seleucid") || lower.contains("ptolem") || lower.contains("byzant")) {
            t1 = 0.22; t2 = 0.80; t3 = 0.76;
            t4 = (targetYear >= 380L) ? 0.90 : 0.42;
        } else if (lower.contains("german") || lower.contains("prussia") || lower.contains("saxony") || lower.contains("bavaria") ||
                   lower.contains("england") || lower.contains("brit") || lower.contains("sweden") || lower.contains("denmark") ||
                   lower.contains("norse") || lower.contains("dutch") || lower.contains("austria") || lower.contains("habsburg")) {
            t1 = 0.24; t2 = (targetYear >= 1500L) ? 0.85 : 0.60; t3 = 0.72;
            t4 = (targetYear >= 800L) ? 0.90 : 0.25;
        } else if (lower.contains("russia") || lower.contains("rus") || lower.contains("poland") || lower.contains("bohemia") ||
                   lower.contains("serbia") || lower.contains("bulgar") || lower.contains("croat") || lower.contains("slav")) {
            t1 = 0.28; t2 = 0.65; t3 = 0.68;
            t4 = (targetYear >= 988L) ? 0.90 : 0.28;
        } else if (lower.contains("persia") || lower.contains("parth") || lower.contains("sasan") || lower.contains("achaemen") ||
                   lower.contains("iran") || lower.contains("qajar") || lower.contains("safavid") || lower.contains("sogd") ||
                   lower.contains("bactria") || lower.contains("kushan") || lower.contains("indo-greek")) {
            t1 = 0.33; t2 = 0.70; t3 = 0.70;
            t4 = (targetYear >= 650L) ? 0.92 : 0.82;
        } else if (lower.contains("india") || lower.contains("maurya") || lower.contains("gupta") || lower.contains("chola") ||
                   lower.contains("harsha") || lower.contains("maratha") || lower.contains("mughal") || lower.contains("sikh") ||
                   lower.contains("cheras") || lower.contains("pandyas") || lower.contains("satavahana") || lower.contains("anuradhapura")) {
            t1 = (lower.contains("cheras") || lower.contains("pandyas") || lower.contains("chola") || lower.contains("anuradhapura")) ? 0.46 : 0.42;
            t2 = 0.75; t3 = 0.82;
            t4 = (lower.contains("mughal")) ? 0.92 : 0.58;
        } else if (lower.contains("judea") || lower.contains("nabataea") || lower.contains("himyar") || lower.contains("hadhramaut") ||
                   lower.contains("arab") || lower.contains("caliph") || lower.contains("umayyad") || lower.contains("abbasid") ||
                   lower.contains("fatimid") || lower.contains("mamluk") || lower.contains("phoenic") || lower.contains("carthag") ||
                   lower.contains("assyria") || lower.contains("babylon") || lower.contains("egypt") || lower.contains("kush") ||
                   lower.contains("axum") || lower.contains("morocco") || lower.contains("ottoman")) {
            t1 = 0.12;
            t2 = (lower.contains("egypt") || lower.contains("caliph") || lower.contains("ottoman")) ? 0.88 : 0.55;
            t3 = (lower.contains("egypt") || lower.contains("babylon") || lower.contains("assyria")) ? 0.90 : 0.65;
            t4 = (targetYear >= 600L || lower.contains("judea") || lower.contains("axum") || lower.contains("himyar")) ? 0.95 : 0.45;
        } else if (lower.contains("mali") || lower.contains("songhai") || lower.contains("ghana") || lower.contains("kanem") ||
                   lower.contains("oyo") || lower.contains("benin") || lower.contains("kongo") || lower.contains("zulu") ||
                   lower.contains("ashanti") || lower.contains("ethiopia")) {
            t1 = 0.08; t2 = 0.60; t3 = 0.62;
            t4 = (lower.contains("mali") || lower.contains("songhai")) ? 0.90 : (lower.contains("ethiopia") ? 0.92 : 0.22);
        } else if (lower.contains("mongol") || lower.contains("xiongnu") || lower.contains("hun") || lower.contains("turk") ||
                   lower.contains("gokturk") || lower.contains("khazar") || lower.contains("scyth") || lower.contains("sarmat") ||
                   lower.contains("kazakh") || lower.contains("jurchen") || lower.contains("manchu")) {
            t1 = 0.58; t2 = 0.40; t3 = 0.32;
            t4 = (lower.contains("khazar") ? 0.92 : (lower.contains("mongol") && targetYear >= 1260L ? 0.60 : 0.18));
        } else if (lower.contains("china") || lower.contains("han") || lower.contains("tang") || lower.contains("song") ||
                   lower.contains("ming") || lower.contains("qing") || lower.contains("zhou") || lower.contains("shang") ||
                   lower.contains("qin") || lower.contains("wei") || lower.contains("jin") || lower.contains("tibet")) {
            t1 = 0.72; t2 = 0.92; t3 = 0.90; t4 = 0.76;
        } else if (lower.contains("japan") || lower.contains("yamato") || lower.contains("tokugawa") || lower.contains("edo") ||
                   lower.contains("korea") || lower.contains("goguryeo") || lower.contains("baekje") || lower.contains("silla") ||
                   lower.contains("joseon") || lower.contains("goryeo")) {
            t1 = 0.76; t2 = 0.85; t3 = 0.85; t4 = 0.72;
        } else if (lower.contains("vietnam") || lower.contains("dai viet") || lower.contains("champa") || lower.contains("funan") ||
                   lower.contains("khmer") || lower.contains("siam") || lower.contains("ayutthaya") || lower.contains("burma") ||
                   lower.contains("srivijaya") || lower.contains("majapahit") || lower.contains("malacca") || lower.contains("aceh") ||
                   lower.contains("brunei") || lower.contains("mataram")) {
            t1 = (lower.contains("srivijaya") || lower.contains("majapahit") || lower.contains("malacca") || lower.contains("aceh")) ? 0.84 : 0.79;
            t2 = 0.72; t3 = 0.82;
            t4 = (lower.contains("malacca") || lower.contains("aceh")) ? 0.92 : 0.60;
        } else if (lower.contains("maya") || lower.contains("aztec") || lower.contains("inca") || lower.contains("olmec") ||
                   lower.contains("teotihuacan") || lower.contains("toltec") || lower.contains("zapotec") || lower.contains("moche") ||
                   lower.contains("tiwanaku") || lower.contains("chimor") || lower.contains("mississippian") || lower.contains("pueblo")) {
            t1 = 0.92;
            t2 = (lower.contains("inca") || lower.contains("aztec") || lower.contains("teotihuacan")) ? 0.88 : 0.70;
            t3 = 0.78; t4 = 0.38;
        } else {
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
