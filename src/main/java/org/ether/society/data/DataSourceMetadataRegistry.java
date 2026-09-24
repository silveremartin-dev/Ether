/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 */
package org.ether.society.data;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import org.ether.society.i18n.I18n;

import java.util.HashMap;
import java.util.Map;

/**
 * Exhaustive Technical Metadata & Peer-Reviewed Dataset Registry.
 * Provides rich multi-line descriptions for every celestial body, historical epoch,
 * cartographic raster, demographic model, and cultural / geological tensor.
 */
public final class DataSourceMetadataRegistry {

    private DataSourceMetadataRegistry() {
        // Utility class
    }

    /**
     * Resolves the full scientific and technical description for a given source key or label.
     * Supports both raw keys (e.g., "earth", "mars", "hyde", "glottolog") and fully qualified UI labels.
     *
     * @param sourceKeyOrLabel The dataset name, label, or canonical key
     * @return Formatted multi-line technical description including origin, variables, resolution, and citations.
     */
    public static String getDetailedSourceDescription(String sourceKeyOrLabel) {
        if (sourceKeyOrLabel == null || sourceKeyOrLabel.isBlank() || "none".equalsIgnoreCase(sourceKeyOrLabel.trim())) {
            return I18n.getOrDefault("common.combo.prompt_source.desc",
                    "ℹ️ Aucune source de données sélectionnée.\nChoisissez une référence scientifique prédéfinie ou chargez un fichier matriciel local personnalisé.");
        }

        String raw = sourceKeyOrLabel.trim();
        String lower = raw.toLowerCase();

        // 1. Check direct i18n key lookup if available
        String i18nKey = getI18nKeyForSource(lower);
        if (i18nKey != null) {
            String desc = I18n.get(i18nKey + ".desc");
            if (desc != null && !desc.equals(i18nKey + ".desc") && !desc.isBlank()) {
                return desc;
            }
        }

        // 2. Structured fallback matching by domain and keywords
        return buildStructuredDescription(raw, lower);
    }

    /**
     * Configures any JavaFX ComboBox for scientific data sources:
     * - Configures each dropdown ListCell with its formatted label AND its dedicated technical Tooltip.
     * - Configures the ComboBox's own Tooltip to dynamically reflect the selected item's technical metadata.
     * - Initializes the ComboBox's tooltip immediately based on its current value.
     */
    public static void setupDetailedSourceCombo(ComboBox<String> combo, String promptKey, String defaultTooltipKey) {
        if (combo == null) return;

        combo.setCellFactory(p -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank() || "none".equalsIgnoreCase(item.trim())) {
                    setText(I18n.getOrDefault(promptKey, "— Select Data Source —"));
                    setTooltip(null);
                } else {
                    String label = item;
                    if ("earth".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.earth", "🌍 Terre — NOAA ETOPO2022 / GMTED2010 [Global, -100ka à Actuel]");
                    else if ("mars".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.mars", "🔴 Mars — MGS MOLA Global Elevation [Planétaire, Éon Actuel]");
                    else if ("venus".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.venus", "🟡 Vénus — Magellan SAR & VIRTIS [Planétaire, Éon Actuel]");
                    else if ("moon".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.moon", "⚪ Lune — LRO LOLA Altimetry [Planétaire, Éon Actuel]");
                    else if ("mercury".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.mercury", "⚪ Mercure — MESSENGER MLA Topography [Planétaire, Éon Actuel]");

                    setText(label);
                    String desc = getDetailedSourceDescription(item);
                    if (desc != null && !desc.isBlank()) {
                        Tooltip cellTooltip = new Tooltip(desc);
                        cellTooltip.setWrapText(true);
                        cellTooltip.setMaxWidth(480);
                        setTooltip(cellTooltip);
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });

        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank() || "none".equalsIgnoreCase(item.trim())) {
                    setText(I18n.getOrDefault(promptKey, "— Select Data Source —"));
                } else {
                    String label = item;
                    if ("earth".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.earth", "🌍 Terre — NOAA ETOPO2022 / GMTED2010 [Global, -100ka à Actuel]");
                    else if ("mars".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.mars", "🔴 Mars — MGS MOLA Global Elevation [Planétaire, Éon Actuel]");
                    else if ("venus".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.venus", "🟡 Vénus — Magellan SAR & VIRTIS [Planétaire, Éon Actuel]");
                    else if ("moon".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.moon", "⚪ Lune — LRO LOLA Altimetry [Planétaire, Éon Actuel]");
                    else if ("mercury".equalsIgnoreCase(item)) label = I18n.getOrDefault("planet.map.mercury", "⚪ Mercure — MESSENGER MLA Topography [Planétaire, Éon Actuel]");
                    setText(label);
                }
            }
        });

        // Dynamic update of the ComboBox's tooltip on selection change
        Runnable updateComboTooltip = () -> {
            String val = combo.getValue();
            if (val == null || val.isBlank() || "none".equalsIgnoreCase(val.trim())) {
                Tooltip tip = new Tooltip(I18n.getOrDefault(defaultTooltipKey,
                        "Sélectionnez une source de données de référence dans la liste déroulante."));
                tip.setWrapText(true);
                tip.setMaxWidth(480);
                combo.setTooltip(tip);
            } else {
                String desc = getDetailedSourceDescription(val);
                Tooltip tip = new Tooltip(desc);
                tip.setWrapText(true);
                tip.setMaxWidth(480);
                combo.setTooltip(tip);
            }
        };

        combo.valueProperty().addListener((obs, oldV, newV) -> updateComboTooltip.run());
        updateComboTooltip.run();
    }

    private static String getI18nKeyForSource(String lower) {
        // --- TOPOGRAPHY ---
        if (lower.equals("earth") || lower.contains("etopo") || lower.contains("gmted")) return "source.topo.earth";
        if (lower.equals("mars") || lower.contains("mola")) return "source.topo.mars";
        if (lower.equals("venus") || lower.equals("vénus") || lower.contains("magellan")) return "source.topo.venus";
        if (lower.equals("moon") || lower.equals("lune") || lower.contains("lola")) return "source.topo.moon";
        if (lower.equals("mercury") || lower.equals("mercure") || lower.contains("messenger")) return "source.topo.mercury";

        // --- CLIMATE : TEMPERATURE ---
        if (lower.contains("bio1") || (lower.contains("era5") && lower.contains("temp"))) return "source.climate.temp.earth";
        if (lower.contains("merra-2") || lower.contains("merra")) return "source.climate.temp.wms";
        if (lower.contains("mgs tes") || (lower.contains("mars") && lower.contains("thermal"))) return "source.climate.temp.mars";
        if (lower.contains("virtis") || (lower.contains("vénus") && lower.contains("hypsometric")) || (lower.contains("venus") && lower.contains("hypsometric"))) return "source.climate.temp.venus";
        if (lower.contains("diviner")) return "source.climate.temp.moon";
        if (lower.contains("messenger mla extreme")) return "source.climate.temp.mercury";

        // --- CLIMATE : PRECIPITATION ---
        if (lower.contains("gpcp") || (lower.contains("worldclim") && lower.contains("precip"))) return "source.climate.precip.earth";
        if (lower.contains("gpm") || lower.contains("imerg")) return "source.climate.precip.wms";
        if (lower.contains("frost & sublimation")) return "source.climate.precip.mars";
        if (lower.contains("h2so4") || lower.contains("virga")) return "source.climate.precip.venus";
        if (lower.contains("vacuum exosphere") || lower.contains("lend")) return "source.climate.precip.moon";
        if (lower.contains("exospheric vacuum")) return "source.climate.precip.mercury";

        // --- CLIMATE : SEASONALITY ---
        if (lower.contains("bio4") || (lower.contains("era5") && lower.contains("season"))) return "source.climate.season.earth";
        if (lower.contains("modis lst")) return "source.climate.season.wms";
        if (lower.contains("orbital eccentricity")) return "source.climate.season.mars";
        if (lower.contains("super-rotation")) return "source.climate.season.venus";
        if (lower.contains("diurnal insolation")) return "source.climate.season.moon";
        if (lower.contains("3:2 spin-orbit")) return "source.climate.season.mercury";

        // --- BIOMES & HYDROGRAPHY ---
        if (lower.contains("modis") || lower.contains("biome")) return "source.biome.earth";
        if (lower.contains("hydrosheds") || lower.contains("swbd") || lower.contains("hydro")) return "source.hydro.earth";

        // --- DEMOGRAPHY ---
        if (lower.contains("hyde 3.4") || lower.contains("hyde")) return "source.demo.hyde";
        if (lower.contains("paléo-démographie") || lower.contains("paleo-demography") || lower.contains("-100000")) return "source.demo.paleo";
        if (lower.contains("cshapes") || lower.contains("centennia")) return "source.demo.cshapes";

        // --- CULTURAL TENSORS (0-8) ---
        if (lower.contains("glottolog") || lower.contains("wals")) return "source.tensor.0.glottolog";
        if (lower.contains("murdock") || lower.contains("kinship") || lower.contains("sccs")) return "source.tensor.1.murdock";
        if (lower.contains("seshat") && (lower.contains("sacred") || lower.contains("rituals") || lower.contains("asabiyyah"))) return "source.tensor.2.seshat";
        if (lower.contains("centennia") && lower.contains("sovereignty")) return "source.tensor.3.centennia";
        if (lower.contains("archaeoglobe") || lower.contains("lithic")) return "source.tensor.4.archaeoglobe";
        if (lower.contains("orbis") || lower.contains("silk road") || lower.contains("caravan")) return "source.tensor.5.orbis";
        if (lower.contains("seshat") && lower.contains("law")) return "source.tensor.6.seshat_law";
        if (lower.contains("anthromes") || (lower.contains("hyde") && lower.contains("stress"))) return "source.tensor.7.anthromes";
        if (lower.contains("gadm") && lower.contains("pathogen")) return "source.tensor.8.pathogens";

        // --- GEOLOGICAL TENSORS (0-9) ---
        if (lower.contains("coal") || lower.contains("charbon")) return "source.geo.coal";
        if (lower.contains("oil") || lower.contains("pétrole") || lower.contains("wpa")) return "source.geo.oil";
        if (lower.contains("natural gas") || lower.contains("gaz naturel") || (lower.contains("gas") && !lower.contains("degas"))) return "source.geo.gas";
        if (lower.contains("iaea") || lower.contains("uranium") || lower.contains("udepo")) return "source.geo.uranium";
        if (lower.contains("helium-3") || lower.contains("hélium-3") || lower.contains("lunar prospector")) return "source.geo.helium3";
        if (lower.contains("banded iron") || lower.contains("bif") || lower.contains("iron_copper") || lower.contains("fer / cuivre")) return "source.geo.iron";
        if (lower.contains("precious metals") || lower.contains("or/argent") || lower.contains("métaux précieux") || lower.contains("pgm")) return "source.geo.precious";
        if (lower.contains("rare earth") || lower.contains("terres rares") || lower.contains("ree") || lower.contains("lithium")) return "source.geo.ree";
        if (lower.contains("ihfc") || lower.contains("heat flow") || lower.contains("mantle heat") || lower.contains("chaleur mantellique") || lower.contains("géothermie")) return "source.geo.heat";
        if (lower.contains("whymap") || lower.contains("aquifer") || lower.contains("groundwater") || lower.contains("nappes")) return "source.geo.aquifers";
        return null;
    }

    private static String buildStructuredDescription(String raw, String lower) {
        StringBuilder sb = new StringBuilder();

        // --- TAB 1 & 2 : TOPOGRAPHIE ---
        if (lower.equals("earth") || lower.contains("etopo") || lower.contains("gmted") || (lower.contains("terre") && lower.contains("satellit"))) {
            sb.append("📡 SOURCE : NOAA ETOPO2022 Global Relief & USGS GMTED2010\n");
            sb.append("🏛️ ORGANISATION : National Oceanic and Atmospheric Administration (NOAA) & USGS\n");
            sb.append("🔬 VARIABLES : Topographie continentale et bathymétrie océanique combinées (-11 000m à +8 848m)\n");
            sb.append("🌐 RÉSOLUTION : Modèle d'élévation global 1 arc-minute calibré au niveau marin de référence (0m = 0.478)\n");
            sb.append("📚 RÉFÉRENCE : NOAA NCEI (2022) ETOPO 2022 15 Arc-Second Global Relief Model.");
            return sb.toString();
        }

        // --- TAB 3 : DÉMOGRAPHIE ---
        if (lower.contains("hyde 3.4") || (lower.contains("terre") && lower.contains("anthropocène"))) {
            sb.append("📡 SOURCE : History Database of the Global Environment (HYDE 3.4)\n");
            sb.append("🏛️ ORGANISATION : PBL Netherlands Environmental Assessment Agency & Université d'Utrecht\n");
            sb.append("🔬 VARIABLES : Densité de population humaine, répartition urbaine/rurale, allocations agraires\n");
            sb.append("🌐 RÉSOLUTION : 5 arc-min (~8.5 km à l'équateur) en projection équirectangulaire 2:1\n");
            sb.append("📅 PLAGE TEMPORELLE : -10 000 BC à 2023 AD (séries chronologiques holocènes calibrées)\n");
            sb.append("📚 RÉFÉRENCE : Klein Goldewijk et al. (2017), Earth System Science Data, 9(2), 927-953.");
            return sb.toString();
        }
        if (lower.contains("paléo-démographie") || lower.contains("expansion sapiens") || lower.contains("-100000")) {
            sb.append("📡 SOURCE : Modèle Paléo-Démographique d'Expansion d'Homo Sapiens\n");
            sb.append("🏛️ ORGANISATION : Max Planck Institute for Evolutionary Anthropology & CalPal Database\n");
            sb.append("🔬 VARIABLES : Densité initiale de chasseurs-cueilleurs forrageurs (hab/km²), fronts de dispersion côtiers\n");
            sb.append("🌐 RÉSOLUTION : Modèle multirésolution H3 calibré sur le climat MIS 5/MIS 4 (-100k à -50k BP)\n");
            sb.append("📚 RÉFÉRENCE : Tallavaara et al. (2015) PNAS; Henn et al. (2012) PLOS Genetics.");
            return sb.toString();
        }
        if (lower.contains("cshapes") || (lower.contains("centennia") && lower.contains("démographiques"))) {
            sb.append("📡 SOURCE : CShapes 2.0 & Centennia Historical Political Demography\n");
            sb.append("🏛️ ORGANISATION : ETH Zürich & International Peace Research Institute Oslo (PRIO)\n");
            sb.append("🔬 VARIABLES : Pôles démographiques des capitales historiques, limites souveraines et densités régionales\n");
            sb.append("🌐 RÉSOLUTION : Grille vectorielle géoréférencée recalibrée en 2:1 equirectangulaire\n");
            sb.append("📚 RÉFÉRENCE : Schvitz et al. (2022) International Interactions; Weidmann et al. (2010).");
            return sb.toString();
        }

        // --- TAB 3 : TENSEURS CULTURELS (0-8) ---
        if (lower.contains("glottolog") || lower.contains("wals") || lower.contains("linguistique") || lower.contains("language")) {
            sb.append("📡 SOURCE : Glottolog 4.8 & World Atlas of Language Structures (WALS)\n");
            sb.append("🏛️ ORGANISATION : Max Planck Institute for Evolutionary Anthropology (Leipzig)\n");
            sb.append("🔬 VARIABLES : Tenseur 0 — Familles linguistiques (IDs 24-bit RGB) & gradient continu de distance dialectale\n");
            sb.append("🌐 COUVERTURE : 8 500+ variétés linguistiques et dialectes répertoriés à l'échelle globale\n");
            sb.append("📚 RÉFÉRENCE : Hammarström, Forkel & Haspelmath (2023) Glottolog 4.8; Dryer & Haspelmath (2013).");
            return sb.toString();
        }
        if (lower.contains("murdock") || lower.contains("sccs") || lower.contains("parenté") || lower.contains("kinship")) {
            sb.append("📡 SOURCE : Murdock Ethnographic Atlas & Standard Cross-Cultural Sample (SCCS)\n");
            sb.append("🏛️ ORGANISATION : University of Pittsburgh & Yale Human Relations Area Files (HRAF)\n");
            sb.append("🔬 VARIABLES : Tenseur 1 — Règles de descendance (patrilinéaire, matrilinéaire, bilatérale) et exogamie\n");
            sb.append("🌐 ÉCHANTILLON : 1 167 sociétés préindustrielles et traditionnelles répertoriées\n");
            sb.append("📚 RÉFÉRENCE : Murdock (1967) Ethnographic Atlas; Murdock & White (1969) SCCS.");
            return sb.toString();
        }
        if (lower.contains("seshat") && (lower.contains("sacred") || lower.contains("rituel") || lower.contains("asabiyyah") || lower.contains("religion") || lower.contains("wrd"))) {
            sb.append("📡 SOURCE : Seshat Global History Databank & Cliodynamics Project\n");
            sb.append("🏛️ ORGANISATION : Evolution Institute & Oxford University (Cliodynamics Lab)\n");
            sb.append("🔬 VARIABLES : Tenseur 2 — Fréquence des rituels collectifs, dieux moralisateurs et cohésion sacrée (Asabiyyah)\n");
            sb.append("🌐 ANALYSE : Évaluation diachronique sur 400+ sociétés historiques sur 5 000 ans\n");
            sb.append("📚 RÉFÉRENCE : Turchin et al. (2018) PNAS, 115(2), E144-E151; Whitehouse et al. (2019) Nature.");
            return sb.toString();
        }
        if (lower.contains("centennia") || (lower.contains("sovereignty") || lower.contains("souverainet") || lower.contains("borders"))) {
            sb.append("📡 SOURCE : Centennia Historical Atlas & CShapes Geopolitical Domains\n");
            sb.append("🏛️ ORGANISATION : Clockwork Software & University of Essex\n");
            sb.append("🔬 VARIABLES : Tenseur 3 — Domaines de souveraineté politique (24-bit ID polities), allégeances et marches frontières\n");
            sb.append("📅 PLAGE TEMPORELLE : -1000 BC à 2026 AD avec pas temporel décennal\n");
            sb.append("📚 RÉFÉRENCE : Reed (2014) Centennia Historical Atlas; CShapes 2.0 (2022).");
            return sb.toString();
        }
        if (lower.contains("archaeoglobe") || lower.contains("material") || lower.contains("lithic") || lower.contains("metallurgy") || lower.contains("technolog")) {
            sb.append("📡 SOURCE : ArchaeoGLOBE Global Land-Use & Technological Transitions Project\n");
            sb.append("🏛️ ORGANISATION : ArchaeoGLOBE Collaboration & University of Maryland\n");
            sb.append("🔬 VARIABLES : Tenseur 4 — Frontières technologiques (lithique, fonderie bronze, fer haut fourneau, vapeur)\n");
            sb.append("🌐 ÉTENDUE : 255 régions archéologiques couvrant 10 000 ans de transition matérielle\n");
            sb.append("📚 RÉFÉRENCE : Stephens et al. (2019), Science, 365(6456), 897-902.");
            return sb.toString();
        }
        if (lower.contains("orbis") || lower.contains("trade") || lower.contains("silk road") || lower.contains("commerce") || lower.contains("caravan")) {
            sb.append("📡 SOURCE : Stanford ORBIS Geospatial Transportation Network & Silk Road Corridors\n");
            sb.append("🏛️ ORGANISATION : Stanford University & UNESCO Silk Roads Programme\n");
            sb.append("🔬 VARIABLES : Tenseur 5 — Réseaux marchands multimodaux, friction des coûts de transport et carrefours d'échanges\n");
            sb.append("📚 RÉFÉRENCE : Scheidel & Meeks (2012) ORBIS Stanford; de La Vaissière (2005) Histoire des marchands sogdiens.");
            return sb.toString();
        }
        if (lower.contains("institutional") || lower.contains("jurisprudence") || (lower.contains("seshat") && lower.contains("law")) || lower.contains("cnts")) {
            sb.append("📡 SOURCE : Seshat Institutional Complexity & Cross-National Time-Series (CNTS)\n");
            sb.append("🏛️ ORGANISATION : Seshat Databank & Banks International Cross-Polity Survey\n");
            sb.append("🔬 VARIABLES : Tenseur 6 — Niveaux de hiérarchie administrative, codification juridique et bureaucratie fiscale\n");
            sb.append("📚 RÉFÉRENCE : Turchin, Currie, Turner et al. (2018) PNAS; Banks & Wilson (2020) CNTS.");
            return sb.toString();
        }
        if (lower.contains("anthromes") || lower.contains("malthusian") || (lower.contains("hyde") && lower.contains("stress")) || lower.contains("degradation")) {
            sb.append("📡 SOURCE : Anthromes 2.0 Global Anthropogenic Biomes & HYDE 3.4 Stress Matrix\n");
            sb.append("🏛️ ORGANISATION : University of Maryland & PBL Netherlands\n");
            sb.append("🔬 VARIABLES : Tenseur 7 — Forçage anthropique, déforestation, surpâturage et érosion des sols agricoles\n");
            sb.append("📚 RÉFÉRENCE : Ellis & Ramankutty (2008) Frontiers in Ecology; Ellis et al. (2010) Global Ecology.");
            return sb.toString();
        }
        if (lower.contains("pathogen") || lower.contains("gadm / historical") || lower.contains("immunity") || lower.contains("epidemic")) {
            sb.append("📡 SOURCE : GADM Historical Epidemiological Memory & Vector Suitability Index\n");
            sb.append("🏛️ ORGANISATION : Institute for Health Metrics and Evaluation (IHME) & WHO Historical Archives\n");
            sb.append("🔬 VARIABLES : Tenseur 8 — Barrières immunitaires acquises, foyers zoonotiques et vulnérabilité pandémique\n");
            sb.append("📚 RÉFÉRENCE : Dobson & Carper (1996); Hay et al. (2009) Global Malaria Distribution Atlas.");
            return sb.toString();
        }

        // --- TAB 2 : DOMAINE GÉOLOGIQUE & MINERAIS (0-9) ---
        if (lower.contains("coal") || lower.contains("charbon") || (lower.contains("usgs") && lower.contains("mrds") && lower.contains("bgr"))) {
            sb.append("📡 SOURCE : USGS Mineral Resources Data System (MRDS) & BGR Global Coal Basins\n");
            sb.append("🏛️ ORGANISATION : United States Geological Survey & German Federal Institute for Geosciences (BGR)\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 0 — Bassins houillers, gisements de lignite et d'anthracite (Gisement prouvé en Gt)\n");
            sb.append("📚 RÉFÉRENCE : USGS MRDS Database (2023); BGR Energy Study Reserves (2022).");
            return sb.toString();
        }
        if (lower.contains("oil") || lower.contains("pétrole") || lower.contains("wpa")) {
            sb.append("📡 SOURCE : USGS World Petroleum Assessment (WPA) & BGR Crude Oil Assessment\n");
            sb.append("🏛️ ORGANISATION : USGS Energy Resources Program & World Energy Council (WEC)\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 1 — Réserves conventionnelles et non-conventionnelles d'hydrocarbures liquides (Gbbl)\n");
            sb.append("📚 RÉFÉRENCE : USGS WPA Bulletin 2201; BGR Commodity Top 50.");
            return sb.toString();
        }
        if (lower.contains("gas") || lower.contains("gaz naturel") || lower.contains("methane")) {
            sb.append("📡 SOURCE : USGS Global Gas Assessment & Seafloor Clathrate Survey\n");
            sb.append("🏛️ ORGANISATION : USGS & International Energy Agency (IEA)\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 2 — Gisements de gaz naturel et hydrates de méthane océaniques (Tcm / Gm³)\n");
            sb.append("📚 RÉFÉRENCE : IEA World Energy Outlook (2023); Ruppel & Kessler (2017) Reviews of Geophysics.");
            return sb.toString();
        }
        if (lower.contains("uranium") || lower.contains("iaea") || lower.contains("udepo")) {
            sb.append("📡 SOURCE : IAEA UDEPO (World Distribution of Uranium Deposits) & OECD Red Book\n");
            sb.append("🏛️ ORGANISATION : International Atomic Energy Agency (AIEA) & Nuclear Energy Agency (NEA)\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 3 — Concentrations de pechblende, minerais d'uranium et thorium fissile (kt U)\n");
            sb.append("📚 RÉFÉRENCE : IAEA TECDOC-1843 UDEPO (2018); OECD/NEA Uranium Red Book (2022).");
            return sb.toString();
        }
        if (lower.contains("helium-3") || lower.contains("hélium-3") || lower.contains("lunar prospector")) {
            sb.append("📡 SOURCE : NASA Lunar Prospector GRS & LRO Regolith Volatiles Survey\n");
            sb.append("🏛️ ORGANISATION : NASA Planetary Data System (PDS) & Lunar and Planetary Institute (LPI)\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 4 — Implantation du vent solaire en Hélium-3 dans les basaltes riches en ilménite (ppb)\n");
            sb.append("📚 RÉFÉRENCE : Schmitt (2006) Return to the Moon; Wittenberg et al. (1986) Fusion Technology.");
            return sb.toString();
        }
        if (lower.contains("iron") || lower.contains("fer") || lower.contains("copper") || lower.contains("cuivre") || lower.contains("bif")) {
            sb.append("📡 SOURCE : USGS Banded Iron Formations (BIF) & Porphyry Copper World Database\n");
            sb.append("🏛️ ORGANISATION : United States Geological Survey (USGS Mineral Resources Program)\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 5 — Formations ferrifères rubanées et gisements porphyriques de cuivre/nickel (Mt)\n");
            sb.append("📚 RÉFÉRENCE : Bekker et al. (2010) Economic Geology; Singer et al. (2008) USGS Scientific Inv. Report.");
            return sb.toString();
        }
        if (lower.contains("precious") || lower.contains("gold") || lower.contains("or") || lower.contains("silver") || lower.contains("pgm")) {
            sb.append("📡 SOURCE : USGS Precious Metal Deposits (Au / Ag / Platinum Group Metals)\n");
            sb.append("🏛️ ORGANISATION : USGS & World Gold Council (WGC)\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 6 — Gisements filoniens aurifères, salars argentifères et intrusions PGM de Bushveld\n");
            sb.append("📚 RÉFÉRENCE : USGS Mineral Commodity Summaries (2024); Goldfarb et al. (2001) Ore Geology Reviews.");
            return sb.toString();
        }
        if (lower.contains("rare earth") || lower.contains("terres rares") || lower.contains("ree") || lower.contains("lithium") || lower.contains("salars")) {
            sb.append("📡 SOURCE : USGS Rare Earth Elements & Global Lithium Salars Inventory\n");
            sb.append("🏛️ ORGANISATION : USGS Critical Minerals Review & Geoscience Australia\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 7 — Carbonatites (Bastnäsite/Monazite), pegmatites à spodumène et saumures de lithium\n");
            sb.append("📚 RÉFÉRENCE : Bradley et al. (2017) USGS Lithium Review; Long et al. (2010) USGS REE Report.");
            return sb.toString();
        }
        if (lower.contains("heat flow") || lower.contains("chaleur mantellique") || lower.contains("ihfc") || lower.contains("mantle heat")) {
            sb.append("📡 SOURCE : International Heat Flow Commission (IHFC) Global Crustal Heat Flow\n");
            sb.append("🏛️ ORGANISATION : International Union of Geodesy and Geophysics (IUGG / IHFC)\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 8 — Flux thermique mantellique et conduction lithosphérique (mW/m²)\n");
            sb.append("📚 RÉFÉRENCE : Davies (2013) Geochemistry Geophysics Geosystems, 14(10), 4608-4622.");
            return sb.toString();
        }
        if (lower.contains("aquifer") || lower.contains("groundwater") || lower.contains("eau souterraine") || lower.contains("whymap")) {
            sb.append("📡 SOURCE : UNESCO / BGR Worldwide Hydrogeological Mapping Assessment (WHYMAP)\n");
            sb.append("🏛️ ORGANISATION : UNESCO International Hydrological Programme & BGR Germany\n");
            sb.append("🔬 VARIABLES : Tenseur Géol 9 — Nappes aquifères régionales, réservoirs fossiles profonds et taux de recharge (km³)\n");
            sb.append("📚 RÉFÉRENCE : Margat & van der Gun (2013) UNESCO Groundwater Resources of the World; WHYMAP 2022.");
            return sb.toString();
        }

        // --- TAB 1 & TAB 2 : CLIMAT & BIOMES ---
        if (lower.contains("era5") || (lower.contains("terre") && (lower.contains("temp") || lower.contains("climat") || lower.contains("precip")))) {
            sb.append("📡 SOURCE : ECMWF ERA5 Climate Reanalysis & NASA GPM IMERG Precipitation\n");
            sb.append("🏛️ ORGANISATION : European Centre for Medium-Range Weather Forecasts & NASA Earth Data\n");
            sb.append("🔬 VARIABLES : Température de surface à 2m (°C), précipitations annuelles (mm/an) et amplitude thermique saisonnière\n");
            sb.append("🌐 RÉSOLUTION : 0.1° x 0.1° (~11 km) rééchantillonnée en grille equirectangulaire 2:1\n");
            sb.append("📚 RÉFÉRENCE : Hersbach et al. (2020) Quarterly Journal of the Royal Meteorological Society, 146(730).");
            return sb.toString();
        }
        if (lower.contains("mars") || lower.contains("ares") || lower.contains("tes") || lower.contains("mgs")) {
            sb.append("📡 SOURCE : Mars Global Surveyor TES & Mars Climate Database (MCD 6.1)\n");
            sb.append("🏛️ ORGANISATION : NASA JPL, ESA & Laboratoire de Météorologie Dynamique (CNRS/LMD)\n");
            sb.append("🔬 VARIABLES : Température radiative martienne, distribution des phyllosilicates et glaces subsurfaciques (MARSIS)\n");
            sb.append("📚 RÉFÉRENCE : Forget et al. (1999) JGR Planets; Millour et al. (2023) MCD 6.1 Documentation.");
            return sb.toString();
        }
        if (lower.contains("venus") || lower.contains("vénus") || lower.contains("magellan") || lower.contains("virtis")) {
            sb.append("📡 SOURCE : Magellan SAR Radar & Venus Express VIRTIS Thermal Spectrometry\n");
            sb.append("🏛️ ORGANISATION : NASA JPL & European Space Agency (ESA)\n");
            sb.append("🔬 VARIABLES : Réflectivité radar SAR, altitude diélectrique et profil de super-rotation mésosphérique (50 km)\n");
            sb.append("📚 RÉFÉRENCE : Saunders et al. (1992) JGR Planets; Svedhem et al. (2007) Nature.");
            return sb.toString();
        }
        if (lower.contains("lune") || lower.contains("moon") || lower.contains("selene") || lower.contains("lro") || lower.contains("diviner")) {
            sb.append("📡 SOURCE : NASA LRO Diviner Thermal Radiometer & Clementine Multispectral UVVIS\n");
            sb.append("🏛️ ORGANISATION : NASA Goddard Space Flight Center & UCLA Planetary Science\n");
            sb.append("🔬 VARIABLES : Températures extrêmes lunaires (40K à 390K), pièges froids polaires et albédo optique LOLA\n");
            sb.append("📚 RÉFÉRENCE : Paige et al. (2010) Science; Chin et al. (2007) Space Science Reviews.");
            return sb.toString();
        }
        if (lower.contains("mercure") || lower.contains("mercury") || lower.contains("hermes") || lower.contains("messenger")) {
            sb.append("📡 SOURCE : NASA MESSENGER MLA (Mercury Laser Altimeter) & MDIS Camera Mosaic\n");
            sb.append("🏛️ ORGANISATION : NASA & Johns Hopkins Applied Physics Laboratory (JHU/APL)\n");
            sb.append("🔬 VARIABLES : Résonance spin-orbite 3:2, dépôts de glace d'eau dans les cratères d'ombre et composition XRS\n");
            sb.append("📚 RÉFÉRENCE : Solomon et al. (2007) Space Science Reviews; Lawrence et al. (2013) Science.");
            return sb.toString();
        }

        // Generic fallback
        sb.append("📡 DONNÉES SCIENTIFIQUES : ").append(raw).append("\n");
        sb.append("🔬 SPÉCIFICATION : Jeu de données cartographiques standard calibré pour la simulation cliodynamique Ether.\n");
        sb.append("🌐 FORMAT : Projection équirectangulaire 2:1 normalisée [0.0 - 1.0].");
        return sb.toString();
    }
}
