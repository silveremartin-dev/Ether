/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 * AUTHOR: Silvere Martin-Michiellot
 */
package org.ether.society.ui.scenario;

import javafx.scene.control.ComboBox;
import org.ether.society.generation.PlanetPreset;
import org.ether.society.i18n.I18n;
import org.ether.society.ui.util.DataSourceUIHelper;

import java.util.Map;

/**
 * Catalog and descriptor registry for multi-dimensional cultural tensors.
 * <p>
 * Provides localized metadata, parameter descriptors, baseline dataset references,
 * and planetary source suggestions across all cultural dimensions.
 * </p>
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0
 */
public final class ScenarioCulturalTensorCatalog {

    private ScenarioCulturalTensorCatalog() {
        // Utility class
    }

    /**
     * Descriptor for a single adjustable slider parameter of a cultural tensor dimension.
     */
    public static class TensorParamDescriptor {
        public final String labelKey;
        public final String defaultLabel;
        public final String tooltipKey;
        public final String defaultTooltip;
        public final double min;
        public final double max;
        public final double defVal;
        public final double step;
        public final String format;

        public TensorParamDescriptor(String labelKey, String defaultLabel, String tooltipKey, String defaultTooltip,
                                     double min, double max, double defVal, double step, String format) {
            this.labelKey = labelKey;
            this.defaultLabel = defaultLabel;
            this.tooltipKey = tooltipKey;
            this.defaultTooltip = defaultTooltip;
            this.min = min;
            this.max = max;
            this.defVal = defVal;
            this.step = step;
            this.format = format;
        }
    }

    /**
     * Gets default tensor name based on index.
     *
     * @param index cultural tensor index
     * @return localized or default tensor name
     */
    public static String getDefaultTensorName(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.name.0", "Isoglosses & Continua Linguistiques");
            case 1 -> I18n.getOrDefault("scenario.tensor.name.1", "Structures de Parenté & Lignages");
            case 2 -> I18n.getOrDefault("scenario.tensor.name.2", "Rituels, Croyances & Sacré");
            case 3 -> I18n.getOrDefault("scenario.tensor.name.3", "Souveraineté Politico-Militaire & Capitales");
            case 4 -> I18n.getOrDefault("scenario.tensor.name.4", "Outillage, Matérialité & Technologies");
            case 5 -> I18n.getOrDefault("scenario.tensor.name.5", "Corridors Commerciaux & Réseaux d'Échange");
            case 6 -> I18n.getOrDefault("scenario.tensor.name.6", "Complexité Institutionnelle & Normes");
            case 7 -> I18n.getOrDefault("scenario.tensor.name.7", "Empreinte Écologique & Pression Malthusienne");
            case 8 -> I18n.getOrDefault("scenario.tensor.name.8", "Immunité aux Pathogènes & Mémoire Sanitaire");
            default -> I18n.getOrDefault("scenario.tensor.name.ext", "Substrat Culturel Extensible {0}", (index + 1));
        };
    }

    /**
     * Formats cultural tensor title.
     *
     * @param index zero-based cultural tensor index
     * @param customTensorNames optional custom names map
     * @return localized title string
     */
    public static String getCulturalTensorTitle(int index, Map<Integer, String> customTensorNames) {
        if (customTensorNames != null && customTensorNames.containsKey(index) && !customTensorNames.get(index).isBlank()) {
            return "🏛 3.2." + (index + 1) + " " + I18n.getOrDefault("scenario.tensor.label_prefix", "Tenseur ") + (index + 1) + " : " + customTensorNames.get(index);
        }
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.title.0", "🗣 3.2.1 Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.title.1", "👨‍👩‍👧 3.2.2 Tensor 2: Kinship & Clan Structures (Lineage)");
            case 2 -> I18n.getOrDefault("scenario.tensor.title.2", "🕯 3.2.3 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.title.3", "👑 3.2.4 Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.title.4", "⚒ 3.2.5 Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.title.5", "⚖ 3.2.6 Tensor 6: Trade Corridors & Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.title.6", "📜 3.2.7 Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.title.7", "🌾 3.2.8 Tensor 8: Ecological Footprint & Malthusian Stress (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.title.8", "🧪 3.2.9 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> I18n.getOrDefault("scenario.tensor.title.ext", "🏛 3.2.{0} Tensor {0}: Extensible Cultural Substrate {0}", (index + 1));
        };
    }

    /**
     * Formats cultural tensor tooltip.
     *
     * @param index tensor index
     * @return localized tooltip
     */
    public static String getCulturalTensorTooltip(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.tooltip.0", "Linguistic component: Dialect continua, mutual intelligibility, and phonetic barriers.");
            case 1 -> I18n.getOrDefault("scenario.tensor.tooltip.1", "Organizational component: Lineage structures, exogamy, clan networks, and alliances.");
            case 2 -> I18n.getOrDefault("scenario.tensor.tooltip.2", "Sacred component: Religious norms, integration rites, taboos, and Asabiyyah social cohesion.");
            case 3 -> I18n.getOrDefault("scenario.tensor.tooltip.3", "Geopolitical component: Administrative centers, allegiance to capitals, and spheres of influence.");
            case 4 -> I18n.getOrDefault("scenario.tensor.tooltip.4", "Material component: Craft traditions, domestic tools, pottery, metallurgy, and architecture.");
            case 5 -> I18n.getOrDefault("scenario.tensor.tooltip.5", "Economic component: Trade corridors (Silk Road, Trans-Sahara, Ocean), trade hubs, and fairs.");
            case 6 -> I18n.getOrDefault("scenario.tensor.tooltip.6", "Institutional component (Seshat): Administrative complexity, codification of customary law, and bureaucracy.");
            case 7 -> I18n.getOrDefault("scenario.tensor.tooltip.7", "Ecological component: Soil degradation, deforestation, salinization, and Malthusian pressure.");
            case 8 -> I18n.getOrDefault("scenario.tensor.tooltip.8", "Sanitary component: Acquired immunity barriers, zoonotic reservoirs, and epidemic vulnerability.");
            default -> String.format(I18n.getOrDefault("scenario.tensor.tooltip.ext", "Composante culturelle multichamp extensible N°%d."), (index + 1));
        };
    }

    /**
     * Formats cultural tensor title for preview dropdown.
     *
     * @param index tensor index
     * @param customTensorNames custom names map
     * @return preview display name
     */
    public static String getCulturalTensorPreviewName(int index, Map<Integer, String> customTensorNames) {
        if (customTensorNames != null && customTensorNames.containsKey(index) && !customTensorNames.get(index).isBlank()) {
            return "🏛 " + I18n.getOrDefault("scenario.tensor.custom.preview_prefix", "Tensor ") + (index + 1) + " : " + customTensorNames.get(index);
        }
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.1.preview", "🗣 Tensor 1: Isoglosses & Linguistic Continua (Languages)");
            case 1 -> I18n.getOrDefault("scenario.tensor.2.preview", "👨‍👩‍👧 Tensor 2: Kinship & Clan Structures (Kinship)");
            case 2 -> I18n.getOrDefault("scenario.tensor.3.preview", "🕯 Tensor 3: Rituals, Beliefs & Sacred (Asabiyyah)");
            case 3 -> I18n.getOrDefault("scenario.tensor.4.preview", "👑 Tensor 4: Politico-Military Sovereignty & Capitals");
            case 4 -> I18n.getOrDefault("scenario.tensor.5.preview", "⚒ Tensor 5: Tooling, Materiality & Technologies (Artifacts)");
            case 5 -> I18n.getOrDefault("scenario.tensor.6.preview", "⚖ Tensor 6: Corridors & Trade Networks (Economic Routes)");
            case 6 -> I18n.getOrDefault("scenario.tensor.7.preview", "📜 Tensor 7: Institutional Complexity & Norms (Seshat & Law)");
            case 7 -> I18n.getOrDefault("scenario.tensor.8.preview", "🌾 Tensor 8: Ecological Footprint & Malthusian Tension (Degradation)");
            case 8 -> I18n.getOrDefault("scenario.tensor.9.preview", "🧪 Tensor 9: Pathogen Immunity & Health Memory (Epidemiology)");
            default -> I18n.getOrDefault("scenario.tensor.custom.preview_prefix", "🏛 Tensor ") + (index + 1) + I18n.getOrDefault("scenario.tensor.custom.preview_mid", " : Cultural Substrate ") + (index + 1);
        };
    }

    public static String getCulturalBaselineName(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.0.source.baseline", "Glottolog 4.8 / Linguistic Continua (Earth Baseline)");
            case 1 -> I18n.getOrDefault("scenario.tensor.1.source.baseline", "Murdock Ethnographic Atlas / Kinship (Earth Baseline)");
            case 2 -> I18n.getOrDefault("scenario.tensor.2.source.baseline", "Seshat Historical Databank / Sacred (Earth Baseline)");
            case 3 -> I18n.getOrDefault("scenario.tensor.3.source.baseline", "Centennia Historical Atlas / Sovereignty (Earth Baseline)");
            case 4 -> I18n.getOrDefault("scenario.tensor.4.source.baseline", "ArchaeoGLOBE Project / Material Culture (Earth Baseline)");
            case 5 -> I18n.getOrDefault("scenario.tensor.5.source.baseline", "ORBIS / Old World Trade Networks (Earth Baseline)");
            case 6 -> I18n.getOrDefault("scenario.tensor.6.source.baseline", "Seshat Historical Databank / Law & Norms (Earth Baseline)");
            case 7 -> I18n.getOrDefault("scenario.tensor.7.source.baseline", "HYDE 3.4 / Historical Land Use & Degradation (Earth Baseline)");
            case 8 -> I18n.getOrDefault("scenario.tensor.8.source.baseline", "GADM / Historical Pathogen Memory (Earth Baseline)");
            default -> I18n.getOrDefault("scenario.status.empirical_loaded", "📁 Empirical baseline layer loaded");
        };
    }

    public static String getCulturalFormatHint(int index) {
        return switch (index) {
            case 0 -> I18n.getOrDefault("scenario.tensor.0.format", "RGB 24-bit Catégoriel + Dithering Spatial (2:1 équirectangulaire) :\n  Valeurs RGB = Familles linguistiques pures & continuums dialectaux (Glottolog 5.0 / WALS)");
            case 1 -> I18n.getOrDefault("scenario.tensor.1.format", "RGB 24-bit Catégoriel (2:1 équirectangulaire) :\n  Valeurs RGB = Organisation sociale & familiale (Todd / Murdock : Souche #8B5CF6, Nucléaire #2563EB, Communautaire #DC2626, Matrilinéaire #F43F5E)");
            case 2 -> I18n.getOrDefault("scenario.tensor.2.format", "RGB 24-bit Catégoriel + Mosaïque Multiconfessionnelle (2:1 équirectangulaire) :\n  Valeurs RGB = Systèmes sacrés (Islam #10B981, Catholicisme #EC4899, Orthodoxie #8B5CF6, Culte Gréco-Romain #EA580C, Hindouisme #F59E0B, Bouddhisme #EAB308, Judaïsme #2563EB)");
            case 3 -> I18n.getOrDefault("scenario.tensor.3.format", "RGB 24-bit Catégoriel (2:1 équirectangulaire) :\n  Valeurs RGB = Souveraineté politique étatique & 22 domaines claniques régionaux (Seshat ClioPatria / CShapes)");
            case 4 -> I18n.getOrDefault("scenario.tensor.4.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Noir (0) = Prélèvement / Faible capture exergétique | Blanc (255) = Métallurgie & outillage avancé (ArchaeoGLOBE)");
            case 5 -> I18n.getOrDefault("scenario.tensor.5.format", "RGB 24-bit Réseau Multi-Modal & Emporia Hiérarchisés (2:1 équirectangulaire) :\n  Cyan = Fleuves navigables (#00D2E6) | Or = Routes de la Soie (#FFC832) | Terracotta = Voies Impériales (#FFAA28) | Bleu = Maritime (#1EB4D2) | Disques blancs = Emporia");
            case 6 -> I18n.getOrDefault("scenario.tensor.6.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Valeurs = Portée administrative SESHAT (Rome/Han 160, 2026 ≈ 220, headroom [226-255]) avec extension infrastructurelle");
            case 7 -> I18n.getOrDefault("scenario.tensor.7.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Noir (0) = Biome intact | Blanc (255) = Dégradation malthusienne & empreinte agricole (HYDE 3.4)");
            case 8 -> I18n.getOrDefault("scenario.tensor.8.format", "Niveaux de Gris 8-bit Continu [0..255] (2:1 équirectangulaire) :\n  Valeurs = Potentiel de transmission vectorielle R₀, foyers zoonotiques et promiscuité urbaine (Mordecai / CDC)");
            default -> I18n.getOrDefault("scenario.hint.cultural_format", "PNG / GeoJSON en projection équirectangulaire 2:1 (RGB Catégoriel ou Niveaux de Gris 8-bit)");
        };
    }

    public static String pickCulturalSourceForEpoch(int tensorIdx, long startYear, PlanetPreset planet) {
        String pName = planet != null ? planet.name().toLowerCase() : "";
        if (pName.contains("mars") || pName.contains("ares")) {
            return switch (tensorIdx) {
                case 0 -> "🔴 Mars — Cartographie Linguistique Coloniale Martienne [Planétaire (Mars), +2060 AD → Futur]";
                case 1 -> "🔴 Mars — Démographie & Lignages de Première Vague [Planétaire (Mars), +2050 AD → Futur]";
                case 2 -> "🔴 Mars — Asabiyyah & Éthique Martienne de Survie [Planétaire (Mars), +2060 AD → Futur]";
                case 3 -> "🔴 Mars — Conseil Spatial & Chartes Constitutionnelles [Planétaire (Mars), +2060 AD → Futur]";
                case 4 -> "🔴 Mars — Fonderies In-Situ (ISRU) & FabLabs Métalliques [Planétaire (Mars), +2050 AD → Futur]";
                case 5 -> "🔴 Mars — Réseau de Maglevs des Tubes de Lave [Planétaire (Mars), +2060 AD → Futur]";
                case 6 -> "🔴 Mars — Administration des Dômes & Quotas d'Oxygène [Planétaire (Mars), +2050 AD → Futur]";
                case 7 -> "🔴 Mars — Bioregenerative Life Support (BLSS) & Dégradation [Planétaire (Mars), +2050 AD → Futur]";
                case 8 -> "🔴 Mars — Microbiome Artificiel Confiné & Résistance [Planétaire (Mars), +2050 AD → Futur]";
                default -> null;
            };
        }
        if (pName.contains("vénus") || pName.contains("venus") || pName.contains("hesperos")) {
            return switch (tensorIdx) {
                case 0 -> "🟡 Vénus — Jargons Stratosphériques des Stations Flottantes [Planétaire (Vénus), +2110 AD → Futur]";
                case 1 -> "🟡 Vénus — Castes Techniques & Syndicats Flottants [Planétaire (Vénus), +2100 AD → Futur]";
                case 2 -> "🟡 Vénus — Culte Acidophile & Idéologie Flottante [Planétaire (Vénus), +2120 AD → Futur]";
                case 3 -> "🟡 Vénus — Syndicats Flottants & Corporations Aérostats [Planétaire (Vénus), +2110 AD → Futur]";
                case 4 -> "🟡 Vénus — Raffineries d'Acide & Métallurgie Aérostatique [Planétaire (Vénus), +2100 AD → Futur]";
                case 5 -> "🟡 Vénus — Lignes Aériennes & Corridors Thermiques [Planétaire (Vénus), +2110 AD → Futur]";
                case 6 -> "🟡 Vénus — Droit Aérostatique & Sécurité de Pression [Planétaire (Vénus), +2100 AD → Futur]";
                case 7 -> "🟡 Vénus — Érosion Chimique & Recyclage Fermé [Planétaire (Vénus), +2100 AD → Futur]";
                case 8 -> "🟡 Vénus — Immunologie en Atmosphère Confinée [Planétaire (Vénus), +2110 AD → Futur]";
                default -> null;
            };
        }
        if (pName.contains("lune") || pName.contains("moon") || pName.contains("selene")) {
            return switch (tensorIdx) {
                case 0 -> "🌕 Lune — Dialectes Sélénites des Stations Cratériques [Planétaire (Lune), +2050 AD → Futur]";
                case 1 -> "🌕 Lune — Associations d'Équipages & Clans Sélénites [Planétaire (Lune), +2045 AD → Futur]";
                case 2 -> "🌕 Lune — Philosophie Cosmique & Rituels du Clair de Terre [Planétaire (Lune), +2050 AD → Futur]";
                case 3 -> "🌕 Lune — Secteurs Traité de l'Espace & Bases Nationales [Planétaire (Lune), +2050 AD → Futur]";
                case 4 -> "🌕 Lune — Fonderies Régolithes & Extraction Sélénite [Planétaire (Lune), +2045 AD → Futur]";
                case 5 -> "🌕 Lune — Tunnels de Transport Magnétique Sélénite [Planétaire (Lune), +2050 AD → Futur]";
                case 6 -> "🌕 Lune — Protocoles Légaux des Habitats Sélénites [Planétaire (Lune), +2050 AD → Futur]";
                case 7 -> "🌕 Lune — Épuisement des Volatils & Poussière Régolithe [Planétaire (Lune), +2045 AD → Futur]";
                case 8 -> "🌕 Lune — Pathogènes d'Isolement & Régime Stérile [Planétaire (Lune), +2045 AD → Futur]";
                default -> null;
            };
        }
        if (pName.contains("mercure") || pName.contains("mercury") || pName.contains("hermes")) {
            return switch (tensorIdx) {
                case 0 -> "🌑 Mercure — Protocoles Herméens & Terminologie d'Ombre [Planétaire (Mercure), +2160 AD → Futur]";
                case 1 -> "🌑 Mercure — Confréries de Maintenance & Lignages Thermiques [Planétaire (Mercure), +2150 AD → Futur]";
                case 2 -> "🌑 Mercure — Ordres d'Énergie & Croyances de Haute Radiation [Planétaire (Mercure), +2170 AD → Futur]";
                case 3 -> "🌑 Mercure — Domaines Miniers & Enclaves Polaires [Planétaire (Mercure), +2150 AD → Futur]";
                case 4 -> "🌑 Mercure — Collecteurs Haute Énergie & Fours Directs [Planétaire (Mercure), +2150 AD → Futur]";
                case 5 -> "🌑 Mercure — Réseau de Convois Électromagnétiques [Planétaire (Mercure), +2160 AD → Futur]";
                case 6 -> "🌑 Mercure — Administration Thermique & Urgences [Planétaire (Mercure), +2160 AD → Futur]";
                case 7 -> "🌑 Mercure — Usure Thermique & Contraintes Matérielles [Planétaire (Mercure), +2150 AD → Futur]";
                case 8 -> "🌑 Mercure — Filtrage Radiatif & Microbiote Synthétique [Planétaire (Mercure), +2160 AD → Futur]";
                default -> null;
            };
        }
        // Earth based on epoch
        if (startYear < -10000) {
            return switch (tensorIdx) {
                case 0 -> "🌍 Terre — Automated Phonological Distance Model (ASJP) [Global, -10 000 BC → Actuel (Macro-Familles)]";
                case 1 -> "🌍 Terre — Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC → Actuel (186 Cultures)]";
                case 2 -> "🌍 Terre — Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC → +2000 AD]";
                case 3 -> "🌍 Terre — GADM Administrative Sovereign Centers [Global, 1950 AD → Actuel]";
                case 4 -> "🌍 Terre — Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC → +2000 AD]";
                case 5 -> "🌍 Terre — Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC → +1800 AD]";
                case 6 -> "🌍 Terre — Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC → +1800 AD]";
                case 7 -> "🌍 Terre — Malthusian Carrying Capacity Model [Global, -100 000 BC → +2100 AD]";
                case 8 -> "🌍 Terre — Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC → +2100 AD]";
                default -> null;
            };
        }
        return switch (tensorIdx) {
            case 0 -> "🌍 Terre — Glottolog 4.8 / WALS Language Families [Global, -10 000 BC → Actuel (8 500+ Langues)]";
            case 1 -> "🌍 Terre — Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC → Actuel (1 267 Sociétés)]";
            case 2 -> "🌍 Terre — Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC → +1900 AD]";
            case 3 -> "🌍 Terre — Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / Amériques, -1000 BC → +2000 AD]";
            case 4 -> "🌍 Terre — ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC → +1850 AD]";
            case 5 -> "🌍 Terre — ORBIS Stanford Geospatial Network (Trade Routes) [Bassin Méditerranéen & Proche-Orient, -300 BC → +500 AD]";
            case 6 -> "🌍 Terre — Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC → +1900 AD]";
            case 7 -> "🌍 Terre — HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC → +2023 AD]";
            case 8 -> "🌍 Terre — GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC → +2023 AD]";
            default -> null;
        };
    }

    public static ComboBox<String> buildCulturalSourceCombo(int tensorIdx) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.getItems().add("");
        switch (tensorIdx) {
            case 0 -> combo.getItems().addAll(
                    "🌍 Terre — Glottolog 4.8 / WALS Language Families [Global, -10 000 BC → Actuel (8 500+ Langues)]",
                    "🌍 Terre — Ethnologue World Linguistic Tree [Global, -3000 BC → Actuel (7 100+ Langues)]",
                    "🌍 Terre — Automated Phonological Distance Model (ASJP) [Global, -10 000 BC → Actuel (Macro-Familles)]",
                    "🔴 Mars — Cartographie Linguistique Coloniale Martienne [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Jargons Stratosphériques des Stations Flottantes [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Dialectes Sélénites des Stations Cratériques [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Protocoles Herméens & Terminologie d'Ombre [Planétaire (Mercure), +2160 AD → Futur]"
            );
            case 1 -> combo.getItems().addAll(
                    "🌍 Terre — Murdock Ethnographic Atlas (Kinship Systems) [Global, -4000 BC → Actuel (1 267 Sociétés)]",
                    "🌍 Terre — Standard Cross-Cultural Sample (SCCS) [Global, -4000 BC → Actuel (186 Cultures)]",
                    "🌍 Terre — Todd Family Systems Historical Typology [Eurasie & Afrique, -1000 BC → +2000 AD]",
                    "🔴 Mars — Démographie & Lignages de Première Vague [Planétaire (Mars), +2050 AD → Futur]",
                    "🟡 Vénus — Castes Techniques & Syndicats Flottants [Planétaire (Vénus), +2100 AD → Futur]",
                    "🌕 Lune — Associations d'Équipages & Clans Sélénites [Planétaire (Lune), +2045 AD → Futur]",
                    "🌑 Mercure — Confréries de Maintenance & Lignages Thermiques [Planétaire (Mercure), +2150 AD → Futur]"
            );
            case 2 -> combo.getItems().addAll(
                    "🌍 Terre — Seshat Global History Databank (Rituals & Sacred) [Global, -5000 BC → +1900 AD]",
                    "🌍 Terre — World Religion Database (WRD) Historical Faiths [Global, -2000 BC → +2020 AD]",
                    "🌍 Terre — Turchin Asabiyyah Cohesion Metric (Cliodynamics) [Global, -3000 BC → +2000 AD]",
                    "🔴 Mars — Asabiyyah & Éthique Martienne de Survie [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Culte Acidophile & Idéologie Flottante [Planétaire (Vénus), +2120 AD → Futur]",
                    "🌕 Lune — Philosophie Cosmique & Rituels du Clair de Terre [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Ordres d'Énergie & Croyances de Haute Radiation [Planétaire (Mercure), +2170 AD → Futur]"
            );
            case 3 -> combo.getItems().addAll(
                    "🌍 Terre — Centennia Historical Atlas (Sovereignty Boundaries) [Eurasie / Afrique / Amériques, -1000 BC → +2000 AD]",
                    "🌍 Terre — CShapes 2.0 Historical State Boundaries [Global, 1886 AD → 2019 AD]",
                    "🌍 Terre — GADM Administrative Sovereign Centers [Global, 1950 AD → Actuel]",
                    "🔴 Mars — Conseil Spatial & Chartes Constitutionnelles [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Syndicats Flottants & Corporations Aérostats [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Secteurs Traité de l'Espace & Bases Nationales [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Domaines Miniers & Enclaves Polaires [Planétaire (Mercure), +2150 AD → Futur]"
            );
            case 4 -> combo.getItems().addAll(
                    "🌍 Terre — ArchaeoGLOBE Project (Land Use & Material Tools) [Global, -10 000 BC → +1850 AD]",
                    "🌍 Terre — Neolithic & Bronze Age Lithic/Metallurgy Databases [Eurasie, -8000 BC → -500 BC]",
                    "🌍 Terre — Lithic-to-Metallurgy Technology Frontier Model [Global, -100 000 BC → +2000 AD]",
                    "🔴 Mars — Fonderies In-Situ (ISRU) & FabLabs Métalliques [Planétaire (Mars), +2050 AD → Futur]",
                    "🟡 Vénus — Raffineries d'Acide & Métallurgie Aérostatique [Planétaire (Vénus), +2100 AD → Futur]",
                    "🌕 Lune — Fonderies Régolithes & Extraction Sélénite [Planétaire (Lune), +2045 AD → Futur]",
                    "🌑 Mercure — Collecteurs Haute Énergie & Fours Directs [Planétaire (Mercure), +2150 AD → Futur]"
            );
            case 5 -> combo.getItems().addAll(
                    "🌍 Terre — ORBIS Stanford Geospatial Network (Trade Routes) [Bassin Méditerranéen & Proche-Orient, -300 BC → +500 AD]",
                    "🌍 Terre — Maritime Silk Road & Ancient Navigation Hubs [Océan Indien & Asie, -200 BC → +1500 AD]",
                    "🌍 Terre — Old World Overland Caravan Network [Sahara & Asie Centrale, -1000 BC → +1800 AD]",
                    "🔴 Mars — Réseau de Maglevs des Tubes de Lave [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Lignes Aériennes & Corridors Thermiques [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Tunnels de Transport Magnétique Sélénite [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Réseau de Convois Électromagnétiques [Planétaire (Mercure), +2160 AD → Futur]"
            );
            case 6 -> combo.getItems().addAll(
                    "🌍 Terre — Seshat Databank (Institutional Complexity & Law) [Global, -4000 BC → +1900 AD]",
                    "🌍 Terre — Cross-National Time-Series Data (CNTS Bureaucracy) [Global, 1815 AD → 2022 AD]",
                    "🌍 Terre — Historical Jurisprudence & Administration Matrix [Europe & Asie, -2000 BC → +1800 AD]",
                    "🔴 Mars — Conseil Spatial & Chartes Constitutionnelles [Planétaire (Mars), +2060 AD → Futur]",
                    "🟡 Vénus — Syndicats Flottants & Corporations Aérostats [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Protocoles Légaux des Habitats Sélénites [Planétaire (Lune), +2050 AD → Futur]",
                    "🌑 Mercure — Administration Thermique & Urgences [Planétaire (Mercure), +2160 AD → Futur]"
            );
            case 7 -> combo.getItems().addAll(
                    "🌍 Terre — HYDE 3.4 Historical Land Use & Anthropogenic Stress [Global, -10 000 BC → +2023 AD]",
                    "🌍 Terre — Anthromes 2.0 Global Anthropogenic Biomes [Global, -8000 BC → +2000 AD]",
                    "🌍 Terre — Malthusian Carrying Capacity Model [Global, -100 000 BC → +2100 AD]",
                    "🔴 Mars — Bioregenerative Life Support (BLSS) & Dégradation [Planétaire (Mars), +2050 AD → Futur]",
                    "🟡 Vénus — Érosion Chimique & Recyclage Fermé [Planétaire (Vénus), +2100 AD → Futur]",
                    "🌕 Lune — Épuisement des Volatils & Poussière Régolithe [Planétaire (Lune), +2045 AD → Futur]",
                    "🌑 Mercure — Usure Thermique & Contraintes Matérielles [Planétaire (Mercure), +2150 AD → Futur]"
            );
            case 8 -> combo.getItems().addAll(
                    "🌍 Terre — GADM / Historical Pathogen Memory & Epidemics [Global, -3000 BC → +2023 AD]",
                    "🌍 Terre — Global Infectious Disease Vector Database [Zones Tropicales & Tempérées, -1000 BC → +2020 AD]",
                    "🌍 Terre — Host-Pathogen Coevolution & Immunity Model [Global, -100 000 BC → +2100 AD]",
                    "🔴 Mars — Microbiome Artificiel Confiné & Résistance [Planétaire (Mars), +2050 AD → Futur]",
                    "🟡 Vénus — Immunologie en Atmosphère Confinée [Planétaire (Vénus), +2110 AD → Futur]",
                    "🌕 Lune — Pathogènes d'Isolement & Régime Stérile [Planétaire (Lune), +2045 AD → Futur]",
                    "🌑 Mercure — Filtrage Radiatif & Microbiote Synthétique [Planétaire (Mercure), +2160 AD → Futur]"
            );
            default -> combo.getItems().addAll(
                    "🌍 Terre — Seshat / Global Databank Substrate [Global, -5000 BC → +2000 AD]",
                    "🌍 Terre — Historical Empirical Baseline [Global, -100 000 BP → Actuel]",
                    "🔴 Mars — Modèle Cartographique Martien Dérivé [Planétaire (Mars), +2050 AD → Futur]"
            );
        }
        DataSourceUIHelper.setupDetailedSourceCombo(combo, "common.combo.prompt_source", "planet.tooltip.map_source_hint");
        return combo;
    }

    public static TensorParamDescriptor getTensorParam1Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p1.label", "Dispersion dialectale (α) :", "scenario.tensor.0.p1.tooltip", "Échelle spatiale de diffusion des variantes phonétiques et lexicales.", 0.01, 0.25, 0.05, 0.01, "%.2f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p1.label", "Rayon clanique :", "scenario.tensor.1.p1.tooltip", "Rayon spatial d'influence et de solidarité des lignages et clans.", 10.0, 500.0, 120.0, 10.0, "%.0f km");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p1.label", "Force Asabiyyah :", "scenario.tensor.2.p1.tooltip", "Niveau de cohésion sociale et de solidarité sacrée (Asabiyyah d'Ibn Khaldoun).", 0.10, 1.00, 0.70, 0.05, "%.2f");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p1.label", "Portée des capitales :", "scenario.tensor.3.p1.tooltip", "Rayon d'action direct de l'autorité politico-militaire centrale.", 50.0, 2500.0, 600.0, 50.0, "%.0f km");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p1.label", "Foyers d'innovation :", "scenario.tensor.4.p1.tooltip", "Nombre de centres artisanaux et métallurgiques initiaux.", 1.0, 20.0, 5.0, 1.0, "%.0f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p1.label", "Comptoirs & carrefours :", "scenario.tensor.5.p1.tooltip", "Nombre de carrefours marchands et comptoirs d'échange.", 2.0, 30.0, 8.0, 1.0, "%.0f");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p1.label", "Niveaux bureaucratiques :", "scenario.tensor.6.p1.tooltip", "Profondeur de la hiérarchie administrative (Seshat Databank).", 1.0, 8.0, 3.0, 1.0, "%.0f niv.");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p1.label", "Surexploitation des sols :", "scenario.tensor.7.p1.tooltip", "Intensité du forçage anthropique et de la déforestation.", 0.0, 1.00, 0.40, 0.05, "%.2f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p1.label", "Pression pathogène :", "scenario.tensor.8.p1.tooltip", "Pression endémique virale et bactérienne régionale.", 0.0, 1.00, 0.35, 0.05, "%.2f");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p1.label", "Fréquence spatiale :", "scenario.tensor.ext.p1.tooltip", "Échelle d'ondulation du substrat procédural.", 0.001, 0.05, 0.01, 0.001, "%.3f");
        };
    }

    public static TensorParamDescriptor getTensorParam2Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p2.label", "Foyers linguistiques :", "scenario.tensor.0.p2.tooltip", "Nombre de foyers indépendants et familles linguistiques initiales.", 1.0, 16.0, 4.0, 1.0, "%.0f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p2.label", "Perméabilité exogamique :", "scenario.tensor.1.p2.tooltip", "Taux d'alliances matrimoniales inter-clans et exogamie.", 0.0, 1.00, 0.35, 0.05, "%.2f");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p2.label", "Sanctuaires sacrés :", "scenario.tensor.2.p2.tooltip", "Nombre de hauts lieux rituels et sanctuaires émergents.", 1.0, 25.0, 6.0, 1.0, "%.0f");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p2.label", "Centralisation régalienne :", "scenario.tensor.3.p2.tooltip", "Degré de concentration du pouvoir politique et fiscal.", 0.0, 1.00, 0.75, 0.05, "%.2f");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p2.label", "Technicité matérielle :", "scenario.tensor.4.p2.tooltip", "Niveau initial de complexité des artéfacts et outillages.", 0.0, 10.0, 2.5, 0.5, "%.1f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p2.label", "Portée des routes :", "scenario.tensor.5.p2.tooltip", "Longueur maximale des routes commerciales et caravanes.", 100.0, 4000.0, 1200.0, 100.0, "%.0f km");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p2.label", "Codification juridique :", "scenario.tensor.6.p2.tooltip", "Niveau de formalisation et de codification des lois écrites.", 0.0, 1.00, 0.45, 0.05, "%.2f");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p2.label", "Taux d'épuisement :", "scenario.tensor.7.p2.tooltip", "Vitesse d'érosion des sols et d'amenuisement des ressources.", 0.001, 0.10, 0.02, 0.005, "%.3f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p2.label", "Réservoirs zoonotiques :", "scenario.tensor.8.p2.tooltip", "Rayon d'influence des foyers sauvages et réservoirs animaux.", 20.0, 600.0, 180.0, 20.0, "%.0f km");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p2.label", "Amplitude du signal :", "scenario.tensor.ext.p2.tooltip", "Intensité relative du tenseur extensible.", 0.10, 1.00, 0.80, 0.05, "%.2f");
        };
    }

    public static TensorParamDescriptor getTensorParam3Descriptor(int tensorIdx) {
        return switch (tensorIdx) {
            case 0 -> new TensorParamDescriptor("scenario.tensor.0.p3.label", "Barrière de relief :", "scenario.tensor.0.p3.tooltip", "Impact du relief et des chaînes de montagnes sur l'isolation linguistique.", 0.0, 1.00, 0.60, 0.05, "%.2f");
            case 1 -> new TensorParamDescriptor("scenario.tensor.1.p3.label", "Hiérarchie lignagère :", "scenario.tensor.1.p3.tooltip", "Degré de structuration et de segmentation patriarcale/matriarcale.", 0.0, 1.00, 0.50, 0.05, "%.2f");
            case 2 -> new TensorParamDescriptor("scenario.tensor.2.p3.label", "Diffusion théologique :", "scenario.tensor.2.p3.tooltip", "Portée spatiale d'expansion des doctrines et rites sacrés.", 50.0, 2000.0, 400.0, 50.0, "%.0f km");
            case 3 -> new TensorParamDescriptor("scenario.tensor.3.p3.label", "Friction frontalière :", "scenario.tensor.3.p3.tooltip", "Tension militaire et friction aux marches de l'empire.", 0.0, 1.00, 0.40, 0.05, "%.2f");
            case 4 -> new TensorParamDescriptor("scenario.tensor.4.p3.label", "Diffusion technique :", "scenario.tensor.4.p3.tooltip", "Conductance de propagation des savoirs artisanaux.", 0.01, 0.30, 0.08, 0.01, "%.2f");
            case 5 -> new TensorParamDescriptor("scenario.tensor.5.p3.label", "Pondération maritime :", "scenario.tensor.5.p3.tooltip", "Attractivité des voies navigables, côtières et maritimes.", 0.0, 1.00, 0.65, 0.05, "%.2f");
            case 6 -> new TensorParamDescriptor("scenario.tensor.6.p3.label", "Seuil d'intégration :", "scenario.tensor.6.p3.tooltip", "Taille de population déclenchant l'émergence des institutions.", 100.0, 10000.0, 1500.0, 100.0, "%.0f hab.");
            case 7 -> new TensorParamDescriptor("scenario.tensor.7.p3.label", "Résilience biocapacité :", "scenario.tensor.7.p3.tooltip", "Capacité de régénération naturelle du biome.", 0.10, 2.00, 1.00, 0.10, "%.2f");
            case 8 -> new TensorParamDescriptor("scenario.tensor.8.p3.label", "Mémoire immunitaire :", "scenario.tensor.8.p3.tooltip", "Vitesse d'acquisition et persistance de l'immunité de groupe.", 0.01, 0.25, 0.06, 0.01, "%.2f");
            default -> new TensorParamDescriptor("scenario.tensor.ext.p3.label", "Taux de diffusion :", "scenario.tensor.ext.p3.tooltip", "Conductance de diffusion spatiale.", 0.01, 0.20, 0.05, 0.01, "%.2f");
        };
    }

    public static String getDefaultTensorSeed(int tensorIdx) {
        return String.valueOf(11235L + tensorIdx * 11111L);
    }
}
