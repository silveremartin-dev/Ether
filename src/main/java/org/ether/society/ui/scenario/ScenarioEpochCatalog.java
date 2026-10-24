/*
 * MIT License
 *
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.control.ComboBox;
import org.ether.society.generation.PlanetPreset;

/**
 * Catalog of empirical and procedural cartographic sources aligned with historical epochs
 * and planetary contexts.
 *
 * <p>Resolves matching datasets across archaeological and cliodynamic corpora:
 * <ul>
 *   <li>Glottolog 4.8 / ASJP phonological distance models</li>
 *   <li>Murdock Ethnographic Atlas / Standard Cross-Cultural Sample (SCCS)</li>
 *   <li>Seshat Global History Databank (Rituals, Sacred, Administrative Complexity)</li>
 *   <li>Centennia Historical Atlas / GADM sovereign territories</li>
 *   <li>ArchaeoGLOBE &amp; HYDE 3.4 land use stress models</li>
 *   <li>Stanford ORBIS trade networks &amp; caravan paths</li>
 * </ul>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.2
 */
public final class ScenarioEpochCatalog {

    private ScenarioEpochCatalog() {
        // Utility class
    }

    /**
     * Resiliently selects the best matching item in a source ComboBox based on target string
     * or fallback keyword matching.
     *
     * @param combo the ComboBox to inspect and update
     * @param target the exact or normalized target string
     * @param fallbackKeyword a fallback keyword if target is not matched
     */
    public static void selectBestSource(ComboBox<String> combo, String target, String fallbackKeyword) {
        if (combo == null || combo.getItems() == null || combo.getItems().isEmpty()) return;
        if (target != null && combo.getItems().contains(target)) {
            combo.setValue(target);
            return;
        }
        if (target != null && !target.isBlank()) {
            String cleanTarget = target.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
            for (String it : combo.getItems()) {
                if (it != null && !it.isEmpty()) {
                    String cleanIt = it.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                    if (cleanIt.contains(cleanTarget) || cleanTarget.contains(cleanIt)) {
                        combo.setValue(it);
                        return;
                    }
                }
            }
        }
        if (fallbackKeyword != null && !fallbackKeyword.isBlank()) {
            String kw = fallbackKeyword.toLowerCase();
            for (String it : combo.getItems()) {
                if (it != null && it.toLowerCase().contains(kw)) {
                    combo.setValue(it);
                    return;
                }
            }
        }
        for (String it : combo.getItems()) {
            if (it != null && !it.isEmpty()) {
                combo.setValue(it);
                return;
            }
        }
    }

    /**
     * Given the tensor index, scenario start year, and active planet preset, returns the best-matching
     * entry string for the cultural source combo at that tensor index, or {@code null} if default should be kept.
     *
     * @param tensorIdx cultural tensor index (0 to 8)
     * @param startYear chronological simulation start year in years CE/BCE
     * @param planet active planetary configuration
     * @return recommended catalog entry string or {@code null}
     */
    public static String pickCulturalSourceForEpoch(int tensorIdx, long startYear, PlanetPreset planet) {
        String pName = planet != null ? planet.name().toLowerCase() : "";
        if (pName.contains("mars") || pName.contains("ares")) {
            return switch (tensorIdx) {
                case 0 -> "🔴 Mars — Cartographie Linguistique Coloniale Martienne [Planétaire (Mars), +2060 AD à Futur]";
                case 1 -> "🔴 Mars — Structures de Parenté & Cohortes Pionnières [Planétaire (Mars), +2050 AD à Futur]";
                case 2 -> "🔴 Mars — Mythologie Martienne & Cultes de la Frontière [Planétaire (Mars), +2060 AD à Futur]";
                case 3 -> "🔴 Mars — Juridictions Consulaires & Traités Martiens [Planétaire (Mars), +2060 AD à Futur]";
                case 4 -> "🔴 Mars — Niveau Technologique Industriel & Robotique ISRU [Planétaire (Mars), +2050 AD à Futur]";
                case 5 -> "🔴 Mars — Réseau Ferroviaire Maglev Sub-Surface [Planétaire (Mars), +2070 AD à Futur]";
                case 6 -> "🔴 Mars — Conseil Spatial & Chartes Constitutionnelles [Planétaire (Mars), +2060 AD à Futur]";
                case 7 -> "🔴 Mars — Bioregenerative Life Support (BLSS) & Dégradation [Planétaire (Mars), +2050 AD à Futur]";
                case 8 -> "🔴 Mars — Microbiome Artificiel Confiné & Résistance [Planétaire (Mars), +2050 AD à Futur]";
                default -> null;
            };
        }
        if (pName.contains("vénus") || pName.contains("venus") || pName.contains("hesperos")) {
            return switch (tensorIdx) {
                case 0 -> "🟡 Vénus — Réseau Isogloss des Cités Aérostatiques [Planétaire (Vénus), +2120 AD à Futur]";
                case 1 -> "🟡 Vénus — Guildes & Lignages Technologiques Flottants [Planétaire (Vénus), +2100 AD à Futur]";
                case 2 -> "🟡 Vénus — Rituels Solaires & Cérémonies de Nuages [Planétaire (Vénus), +2120 AD à Futur]";
                case 3 -> "🟡 Vénus — Fédération des Stations Stratosphériques [Planétaire (Vénus), +2110 AD à Futur]";
                case 4 -> "🟡 Vénus — Synthèse Aérostatique & Ingénierie Acide [Planétaire (Vénus), +2100 AD à Futur]";
                case 5 -> "🟡 Vénus — Navettes Stratosphériques Inter-Stations [Planétaire (Vénus), +2110 AD à Futur]";
                case 6 -> "🟡 Vénus — Syndicats Flottants & Corporations Aérostats [Planétaire (Vénus), +2110 AD à Futur]";
                case 7 -> "🟡 Vénus — Érosion Chimique & Recyclage Fermé [Planétaire (Vénus), +2100 AD à Futur]";
                case 8 -> "🟡 Vénus — Immunologie en Atmosphère Confinée [Planétaire (Vénus), +2110 AD à Futur]";
                default -> null;
            };
        }
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene")) {
            return switch (tensorIdx) {
                case 0 -> "⚪ Lune — Dialectes Sélénites des Stations Cratériques [Planétaire (Lune), +2050 AD à Futur]";
                case 1 -> "⚪ Lune — Associations d'Équipages & Clans Sélénites [Planétaire (Lune), +2045 AD à Futur]";
                case 2 -> "⚪ Lune — Philosophie Cosmique & Rituels du Clair de Terre [Planétaire (Lune), +2050 AD à Futur]";
                case 3 -> "⚪ Lune — Secteurs Traité de l'Espace & Bases Nationales [Planétaire (Lune), +2050 AD à Futur]";
                case 4 -> "⚪ Lune — Fonderies Régolithes & Extraction Sélénite [Planétaire (Lune), +2045 AD à Futur]";
                case 5 -> "⚪ Lune — Tunnels de Transport Magnétique Sélénite [Planétaire (Lune), +2050 AD à Futur]";
                case 6 -> "⚪ Lune — Protocoles Légaux des Habitats Sélénites [Planétaire (Lune), +2050 AD à Futur]";
                case 7 -> "⚪ Lune — Épuisement des Volatils & Poussière Régolithe [Planétaire (Lune), +2045 AD à Futur]";
                case 8 -> "⚪ Lune — Pathogènes d'Isolement & Régime Stérile [Planétaire (Lune), +2045 AD à Futur]";
                default -> null;
            };
        }
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes")) {
            return switch (tensorIdx) {
                case 0 -> "⚪ Mercure — Protocoles Herméens & Terminologie d'Ombre [Planétaire (Mercure), +2160 AD à Futur]";
                case 1 -> "⚪ Mercure — Confréries de Maintenance & Lignages Thermiques [Planétaire (Mercure), +2150 AD à Futur]";
                case 2 -> "⚪ Mercure — Ordres d'Énergie & Croyances de Haute Radiation [Planétaire (Mercure), +2170 AD à Futur]";
                case 3 -> "⚪ Mercure — Domaines Miniers & Enclaves Polaires [Planétaire (Mercure), +2150 AD à Futur]";
                case 4 -> "⚪ Mercure — Collecteurs Haute Énergie & Fours Directs [Planétaire (Mercure), +2150 AD à Futur]";
                case 5 -> "⚪ Mercure — Réseau de Convois Électromagnétiques [Planétaire (Mercure), +2160 AD à Futur]";
                case 6 -> "⚪ Mercure — Administration Thermique & Urgences [Planétaire (Mercure), +2160 AD à Futur]";
                case 7 -> "⚪ Mercure — Usure Thermique & Contraintes Matérielles [Planétaire (Mercure), +2150 AD à Futur]";
                case 8 -> "⚪ Mercure — Filtrage Radiatif & Microbiote Synthétique [Planétaire (Mercure), +2160 AD à Futur]";
                default -> null;
            };
        }
        // Earth — pick based on epoch
        if (startYear < -10000) {
            return switch (tensorIdx) {
                case 0 -> "🌍 Terre — Automated Phonological Distance Model (ASJP) [Global, -10 000 BC à Actuel (Macro-Familles)]";
                case 1 -> "🌍 Terre — Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC à Actuel (186 Cultures)]";
                case 2 -> "🌍 Terre — Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC à +2000 AD]";
                case 3 -> "🌍 Terre — GADM Administrative Sovereign Centers [Global, 1950 AD à Actuel]";
                case 4 -> "🌍 Terre — Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC à +2000 AD]";
                case 5 -> "🌍 Terre — Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC à +1800 AD]";
                case 6 -> "🌍 Terre — Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC à +1800 AD]";
                case 7 -> "🌍 Terre — Malthusian Carrying Capacity Model [Global, -100 000 BC à +2100 AD]";
                case 8 -> "🌍 Terre — Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC à +2100 AD]";
                default -> null;
            };
        }
        // Default Earth modern/historical: first real item per tensor
        return switch (tensorIdx) {
            case 0 -> "🌍 Terre — Glottolog 4.8 / WALS Language Families [Global, -10 000 BC à Actuel (8 500+ Langues)]";
            case 1 -> "🌍 Terre — Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC à Actuel (1 267 Sociétés)]";
            case 2 -> "🌍 Terre — Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC à +1900 AD]";
            case 3 -> "🌍 Terre — Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / Amériques, -1000 BC à +2000 AD]";
            case 4 -> "🌍 Terre — ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC à +1850 AD]";
            case 5 -> "🌍 Terre — ORBIS Stanford Geospatial Network (Trade Routes) [Bassin Méditerranéen & Proche-Orient, -300 BC à +500 AD]";
            case 6 -> "🌍 Terre — Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC à +1900 AD]";
            case 7 -> "🌍 Terre — HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC à +2023 AD]";
            case 8 -> "🌍 Terre — GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC à +2023 AD]";
            default -> null;
        };
    }
}
