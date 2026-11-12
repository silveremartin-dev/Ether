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
public final class ScenarioEpochRegistry {

    private ScenarioEpochRegistry() {
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
                case 0 -> "ðŸ”´ Mars â€” Cartographie Linguistique Coloniale Martienne [PlanÃ©taire (Mars), +2060 AD Ã  Futur]";
                case 1 -> "ðŸ”´ Mars â€” Structures de ParentÃ© & Cohortes PionniÃ¨res [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
                case 2 -> "ðŸ”´ Mars â€” Mythologie Martienne & Cultes de la FrontiÃ¨re [PlanÃ©taire (Mars), +2060 AD Ã  Futur]";
                case 3 -> "ðŸ”´ Mars â€” Juridictions Consulaires & TraitÃ©s Martiens [PlanÃ©taire (Mars), +2060 AD Ã  Futur]";
                case 4 -> "ðŸ”´ Mars â€” Niveau Technologique Industriel & Robotique ISRU [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
                case 5 -> "ðŸ”´ Mars â€” RÃ©seau Ferroviaire Maglev Sub-Surface [PlanÃ©taire (Mars), +2070 AD Ã  Futur]";
                case 6 -> "ðŸ”´ Mars â€” Conseil Spatial & Chartes Constitutionnelles [PlanÃ©taire (Mars), +2060 AD Ã  Futur]";
                case 7 -> "ðŸ”´ Mars â€” Bioregenerative Life Support (BLSS) & DÃ©gradation [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
                case 8 -> "ðŸ”´ Mars â€” Microbiome Artificiel ConfinÃ© & RÃ©sistance [PlanÃ©taire (Mars), +2050 AD Ã  Futur]";
                default -> null;
            };
        }
        if (pName.contains("vÃ©nus") || pName.contains("venus") || pName.contains("hesperos")) {
            return switch (tensorIdx) {
                case 0 -> "ðŸŸ¡ VÃ©nus â€” RÃ©seau Isogloss des CitÃ©s AÃ©rostatiques [PlanÃ©taire (VÃ©nus), +2120 AD Ã  Futur]";
                case 1 -> "ðŸŸ¡ VÃ©nus â€” Guildes & Lignages Technologiques Flottants [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]";
                case 2 -> "ðŸŸ¡ VÃ©nus â€” Rituels Solaires & CÃ©rÃ©monies de Nuages [PlanÃ©taire (VÃ©nus), +2120 AD Ã  Futur]";
                case 3 -> "ðŸŸ¡ VÃ©nus â€” FÃ©dÃ©ration des Stations StratosphÃ©riques [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]";
                case 4 -> "ðŸŸ¡ VÃ©nus â€” SynthÃ¨se AÃ©rostatique & IngÃ©nierie Acide [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]";
                case 5 -> "ðŸŸ¡ VÃ©nus â€” Navettes StratosphÃ©riques Inter-Stations [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]";
                case 6 -> "ðŸŸ¡ VÃ©nus â€” Syndicats Flottants & Corporations AÃ©rostats [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]";
                case 7 -> "ðŸŸ¡ VÃ©nus â€” Ã‰rosion Chimique & Recyclage FermÃ© [PlanÃ©taire (VÃ©nus), +2100 AD Ã  Futur]";
                case 8 -> "ðŸŸ¡ VÃ©nus â€” Immunologie en AtmosphÃ¨re ConfinÃ©e [PlanÃ©taire (VÃ©nus), +2110 AD Ã  Futur]";
                default -> null;
            };
        }
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene")) {
            return switch (tensorIdx) {
                case 0 -> "âšª Lune â€” Dialectes SÃ©lÃ©nites des Stations CratÃ©riques [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 1 -> "âšª Lune â€” Associations d'Ã‰quipages & Clans SÃ©lÃ©nites [PlanÃ©taire (Lune), +2045 AD Ã  Futur]";
                case 2 -> "âšª Lune â€” Philosophie Cosmique & Rituels du Clair de Terre [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 3 -> "âšª Lune â€” Secteurs TraitÃ© de l'Espace & Bases Nationales [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 4 -> "âšª Lune â€” Fonderies RÃ©golithes & Extraction SÃ©lÃ©nite [PlanÃ©taire (Lune), +2045 AD Ã  Futur]";
                case 5 -> "âšª Lune â€” Tunnels de Transport MagnÃ©tique SÃ©lÃ©nite [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 6 -> "âšª Lune â€” Protocoles LÃ©gaux des Habitats SÃ©lÃ©nites [PlanÃ©taire (Lune), +2050 AD Ã  Futur]";
                case 7 -> "âšª Lune â€” Ã‰puisement des Volatils & PoussiÃ¨re RÃ©golithe [PlanÃ©taire (Lune), +2045 AD Ã  Futur]";
                case 8 -> "âšª Lune â€” PathogÃ¨nes d'Isolement & RÃ©gime StÃ©rile [PlanÃ©taire (Lune), +2045 AD Ã  Futur]";
                default -> null;
            };
        }
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes")) {
            return switch (tensorIdx) {
                case 0 -> "âšª Mercure â€” Protocoles HermÃ©ens & Terminologie d'Ombre [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]";
                case 1 -> "âšª Mercure â€” ConfrÃ©ries de Maintenance & Lignages Thermiques [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
                case 2 -> "âšª Mercure â€” Ordres d'Ã‰nergie & Croyances de Haute Radiation [PlanÃ©taire (Mercure), +2170 AD Ã  Futur]";
                case 3 -> "âšª Mercure â€” Domaines Miniers & Enclaves Polaires [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
                case 4 -> "âšª Mercure â€” Collecteurs Haute Ã‰nergie & Fours Directs [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
                case 5 -> "âšª Mercure â€” RÃ©seau de Convois Ã‰lectromagnÃ©tiques [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]";
                case 6 -> "âšª Mercure â€” Administration Thermique & Urgences [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]";
                case 7 -> "âšª Mercure â€” Usure Thermique & Contraintes MatÃ©rielles [PlanÃ©taire (Mercure), +2150 AD Ã  Futur]";
                case 8 -> "âšª Mercure â€” Filtrage Radiatif & Microbiote SynthÃ©tique [PlanÃ©taire (Mercure), +2160 AD Ã  Futur]";
                default -> null;
            };
        }
        // Earth a€” pick based on epoch
        if (startYear < -10000) {
            return switch (tensorIdx) {
                case 0 -> "ðŸŒ Terre â€” Automated Phonological Distance Model (ASJP) [Global, -10 000 BC Ã  Actuel (Macro-Familles)]";
                case 1 -> "ðŸŒ Terre â€” Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC Ã  Actuel (186 Cultures)]";
                case 2 -> "ðŸŒ Terre â€” Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC Ã  +2000 AD]";
                case 3 -> "ðŸŒ Terre â€” GADM Administrative Sovereign Centers [Global, 1950 AD Ã  Actuel]";
                case 4 -> "ðŸŒ Terre â€” Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC Ã  +2000 AD]";
                case 5 -> "ðŸŒ Terre â€” Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC Ã  +1800 AD]";
                case 6 -> "ðŸŒ Terre â€” Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC Ã  +1800 AD]";
                case 7 -> "ðŸŒ Terre â€” Malthusian Carrying Capacity Model [Global, -100 000 BC Ã  +2100 AD]";
                case 8 -> "ðŸŒ Terre â€” Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC Ã  +2100 AD]";
                default -> null;
            };
        }
        // Default Earth modern/historical: first real item per tensor
        return switch (tensorIdx) {
            case 0 -> "ðŸŒ Terre â€” Glottolog 4.8 / WALS Language Families [Global, -10 000 BC Ã  Actuel (8 500+ Langues)]";
            case 1 -> "ðŸŒ Terre â€” Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC Ã  Actuel (1 267 SociÃ©tÃ©s)]";
            case 2 -> "ðŸŒ Terre â€” Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC Ã  +1900 AD]";
            case 3 -> "ðŸŒ Terre â€” Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / AmÃ©riques, -1000 BC Ã  +2000 AD]";
            case 4 -> "ðŸŒ Terre â€” ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC Ã  +1850 AD]";
            case 5 -> "ðŸŒ Terre â€” ORBIS Stanford Geospatial Network (Trade Routes) [Bassin MÃ©diterranÃ©en & Proche-Orient, -300 BC Ã  +500 AD]";
            case 6 -> "ðŸŒ Terre â€” Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC Ã  +1900 AD]";
            case 7 -> "ðŸŒ Terre â€” HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC Ã  +2023 AD]";
            case 8 -> "ðŸŒ Terre â€” GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC Ã  +2023 AD]";
            default -> null;
        };
    }
}
