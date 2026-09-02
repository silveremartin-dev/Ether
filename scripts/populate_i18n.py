#!/usr/bin/env python3
"""
Populate all missing I18n keys for EN, FR, DE, ES, ZH in messages_*.properties
"""
import os, re, glob

SRC_DIR = r"src\main\java"
I18N_DIR = r"src\main\resources\i18n"
LANGS = ["en", "fr", "de", "es", "zh"]

KEY_DEF_PATTERN = re.compile(r'I18n\.getOrDefault\(\s*"([^"]+)"\s*,\s*"((?:[^"\\]|\\.)*)"\s*\)')
KEY_ONLY_PATTERN = re.compile(r'I18n\.get\(\s*"([^"]+)"\s*\)')

key_fallbacks = {}

for path in glob.glob(os.path.join(SRC_DIR, "**", "*.java"), recursive=True):
    with open(path, encoding="utf-8", errors="replace") as f:
        content = f.read()
    for k, v in KEY_DEF_PATTERN.findall(content):
        v_clean = v.replace('\\"', '"').replace('\\n', '\n')
        if k not in key_fallbacks:
            key_fallbacks[k] = v_clean
    for k in KEY_ONLY_PATTERN.findall(content):
        if k not in key_fallbacks:
            key_fallbacks[k] = k

# Translation mapping dictionaries per language
TRANSLATIONS = {
    "en": {
        "resource.view.section_base": "────────── 11 CANONICAL BASE MAPS ──────────",
        "resource.view.section_derived": "────────── DERIVED MAPS & ANOMALIES ──────────",
        "resource.view.biomes": "🌿 1. Biome & Vegetation Cover Map (GtC)",
        "resource.view.hydro": "🌊 2. Hydrography & River Network (SWBD / Slope)",
        "resource.view.coal": "⛏️ 3. Coal Deposits (4.1 COAL)",
        "resource.view.oil": "🛢️ 4. Crude Oil Reserves (4.2 CRUDE_OIL)",
        "resource.view.gas": "🔥 5. Natural Gas Fields (4.3 NATURAL_GAS)",
        "resource.view.uranium": "⚛️ 6. Uranium Ores & Fission (4.4 URANIUM)",
        "resource.view.helium3": "🌌 7. Helium-3 & Lunar Fusion (4.5 HELIUM_3)",
        "resource.view.iron_copper": "⛓️ 8. BIF Iron & Copper Metals (4.6 IRON_COPPER)",
        "resource.view.precious": "💎 9. Rare Earths & Precious Metals (4.7 PRECIOUS_REE / Li)",
        "resource.view.heat": "🌋 10. Mantle Heat Flux (4.8 MANTLE_HEAT)",
        "resource.view.aquifer": "💧 11. Aquifers & Freshwater (4.9 FRESHWATER_AQUIFERS)",
        "resource.view.temp": "🌡️ 12. Surface Temperatures & Microclimates",
        "resource.view.seismic": "🌋 13. Tectonics & Seismic/Volcanic Hazards",
        "resource.view.aridity": "🏜️ 14. Aridity & Soil Salinization",
        "resource.tensor.1.title": "⛏️ 4.1 Coal Deposits (COAL — USGS / BGR)",
        "resource.tensor.2.title": "🛢️ 4.2 Crude Oil & Fuel Reserves (CRUDE_OIL — WEP)",
        "resource.tensor.3.title": "🔥 4.3 Natural Gas Fields (NATURAL_GAS — WEP / BGR)",
        "resource.tensor.4.title": "⚛️ 4.4 Uranium & Fission Ores (URANIUM — IAEA UDEPO)",
        "resource.tensor.5.title": "🌌 4.5 Helium-3 & Lunar Fusion (HELIUM_3 — NASA / LPI)",
        "resource.tensor.6.title": "⛓️ 4.6 BIF Iron & Porphyry Copper Metals (IRON_COPPER)",
        "resource.tensor.7.title": "💎 4.7 Rare Earths, Lithium Brines & Spodumene (PRECIOUS_REE / Li)",
        "resource.tensor.8.title": "🌋 4.8 Mantle Heat Flux & Geothermal (MANTLE_HEAT — IHFC / Davies 2013)",
        "resource.tensor.9.title": "💧 4.9 Basin Aquifers & Freshwater (FRESHWATER_AQUIFERS — WHYMAP)",
        "resource.tensor.1.param1": "Coal Seam Abundance & Thickness (Gt)",
        "resource.tensor.2.param1": "Oil Reserves & Tectonic Traps (Gt)",
        "resource.tensor.3.param1": "Reservoir Pressure & Gas Abundance (10¹² m³)",
        "resource.tensor.4.param1": "Uranium Pegmatite Crustal Grade (ppm)",
        "resource.tensor.5.param1": "Helium-3 Regolith Concentration (ppb)",
        "resource.tensor.6.param1": "BIF Iron & Copper Abundance (Gt)",
        "resource.tensor.7.param1": "Rare Earth & Precious Metal Grade (Mt)",
        "resource.tensor.8.param1": "Mean Mantle Heat Flux (mW/m²)",
        "resource.tensor.9.param1": "Subsurface Aquifer Capacity (10³ km³)",
        "resource.tensor.1.param2": "Swamp Paleo-Biomass Threshold (%)",
        "resource.tensor.2.param2": "Anoxic Source Rock Maturation (%)",
        "resource.tensor.3.param2": "Thermogenic Shale Gas Fraction (%)",
        "resource.tensor.4.param2": "Hydrothermal Infiltration Rate (%)",
        "resource.tensor.5.param2": "Solar Wind Ilmenite Maturation (%)",
        "resource.tensor.6.param2": "Metallogenic Magmatic Activity (%)",
        "resource.tensor.7.param2": "Lithium Brine Salar Grade (%)",
        "resource.tensor.8.param2": "Rifts & Hotspot Intensity (%)",
        "resource.tensor.9.param2": "Subsurface Cryosphere Permeability (%)",
        "resource.mode.procedural_hotspots": "▶ Procedural Mode (Hotspots & Physical Model)",
        "resource.mode.import_file": "📂 Imported Map (PNG / GeoJSON / ESRI .asc)",
        "resource.section.geological_tensors": "4. GEOLOGY, TECTONICS AND ORE DOMAIN",
        "resource.btn.gen_single_tensor": "🪄 Generate",
        "resource.label.tensor_seed": "🎲 Seed:",
        "resource.hint.geo_format": "PNG/GeoTIFF image in 2:1 equirectangular projection",
        "scenario.tensor.status.procedural": "✅ Procedural mode active",
        "resource.status.procedural_fallback": "⚠️ Procedural fallback active",
        "scenario.status.no_file": "— No file loaded —",
        "resource.status.geo_loaded": "📷 Geological map loaded",
        "scenario.status.map_synced": "✅ Map loaded and synchronized"
    },
    "fr": {
        "resource.view.section_base": "────────── 11 CARTES CANONIQUES DE BASE ──────────",
        "resource.view.section_derived": "────────── CARTES DÉDUITES / ANOMALIES ──────────",
        "resource.view.biomes": "🌿 1. Biomes & Couverture Végétale (GtC)",
        "resource.view.hydro": "🌊 2. Hydrographie & Réseau Fluvial (SWBD / Slope)",
        "resource.view.coal": "⛏️ 3. Gisements de Charbon (4.1 COAL)",
        "resource.view.oil": "🛢️ 4. Réserves de Pétrole Brut (4.2 CRUDE_OIL)",
        "resource.view.gas": "🔥 5. Champs de Gaz Naturel (4.3 NATURAL_GAS)",
        "resource.view.uranium": "⚛️ 6. Minerais d'Uranium & Fission (4.4 URANIUM)",
        "resource.view.helium3": "🌌 7. Hélium-3 & Fusion Lunaires (4.5 HELIUM_3)",
        "resource.view.iron_copper": "⛓️ 8. Métaux Fer BIF & Cuivre (4.6 IRON_COPPER)",
        "resource.view.precious": "💎 9. Terres Rares & Métaux Précieux (4.7 PRECIOUS_REE / Li)",
        "resource.view.heat": "🌋 10. Flux Thermique du Manteau (4.8 MANTLE_HEAT)",
        "resource.view.aquifer": "💧 11. Aquifères & Eau Douce (4.9 FRESHWATER_AQUIFERS)",
        "resource.view.temp": "🌡️ 12. Températures Surface & Microclimats",
        "resource.view.seismic": "🌋 13. Tectonique & Aléa Sismique/Volcanique",
        "resource.view.aridity": "🏜️ 14. Aridité & Salinisation des Sols",
        "resource.tensor.1.title": "⛏️ 4.1 Gisements de Charbon (COAL — USGS / BGR)",
        "resource.tensor.2.title": "🛢️ 4.2 Réserves de Pétrole Brut & Fuel (CRUDE_OIL — WEP)",
        "resource.tensor.3.title": "🔥 4.3 Champs de Gaz Naturel (NATURAL_GAS — WEP / BGR)",
        "resource.tensor.4.title": "⚛️ 4.4 Minerais d'Uranium & Fission (URANIUM — IAEA UDEPO)",
        "resource.tensor.5.title": "🌌 4.5 Hélium-3 & Fusion Lunaires (HELIUM_3 — NASA / LPI)",
        "resource.tensor.6.title": "⛓️ 4.6 Métaux Industriels Fer BIF & Cuivre Porphyrique (IRON_COPPER)",
        "resource.tensor.7.title": "💎 4.7 Terres Rares, Lithium Brines & Spodumène (PRECIOUS_REE / Li)",
        "resource.tensor.8.title": "🌋 4.8 Flux Thermique du Manteau & Géothermie (MANTLE_HEAT — IHFC / Davies 2013)",
        "resource.tensor.9.title": "💧 4.9 Aquifères & Eau Douce Grands Bassins (FRESHWATER_AQUIFERS — WHYMAP)",
        "resource.tensor.1.param1": "Abondance & Épaisseur Veines Charbon (Gt)",
        "resource.tensor.2.param1": "Réserves & Pièges Tectoniques Pétrole (Gt)",
        "resource.tensor.3.param1": "Pression Réservoir & Abondance Gaz (10¹² m³)",
        "resource.tensor.4.param1": "Teneur Crustale Pegmatites Uranium (ppm)",
        "resource.tensor.5.param1": "Concentration Régolithe Hélium-3 (ppb)",
        "resource.tensor.6.param1": "Abondance Fer BIF & Cuivre (Gt)",
        "resource.tensor.7.param1": "Teneur Terres Rares & Métaux Précieux (Mt)",
        "resource.tensor.8.param1": "Flux Thermique Moyen Manteau (mW/m²)",
        "resource.tensor.9.param1": "Capacité Aquifères Subsurface (10³ km³)",
        "resource.tensor.1.param2": "Seuil Paléo-Biomasse Marécages (%)",
        "resource.tensor.2.param2": "Maturation Roche-Mère Anoxique (%)",
        "resource.tensor.3.param2": "Fraction Gaz Schisteux Thermogène (%)",
        "resource.tensor.4.param2": "Infiltration Hydrothermale (%)",
        "resource.tensor.5.param2": "Maturation Ilménite Vent Solaire (%)",
        "resource.tensor.6.param2": "Activité Magmatique Métallogénique (%)",
        "resource.tensor.7.param2": "Teneur Salars Brines Lithium (%)",
        "resource.tensor.8.param2": "Intensité Rifts & Points Chauds (%)",
        "resource.tensor.9.param2": "Perméabilité Cryosphère Subsurface (%)",
        "resource.mode.procedural_hotspots": "▶ Mode Procédural (Hotspots & Modèle Physique)",
        "resource.mode.import_file": "📂 Carte Importée (PNG / GeoJSON / ESRI .asc)",
        "resource.section.geological_tensors": "4. DOMAINE GÉOLOGIE, TECTONIQUE ET MINERAIS",
        "resource.btn.gen_single_tensor": "🪄 Générer",
        "resource.label.tensor_seed": "🎲 Graine:",
        "resource.hint.geo_format": "Image PNG/GeoTIFF en projection équirectangulaire 2:1",
        "scenario.tensor.status.procedural": "✅ Mode procédural actif",
        "resource.status.procedural_fallback": "⚠️ Repli procédural actif",
        "scenario.status.no_file": "— Aucun fichier chargé —",
        "resource.status.geo_loaded": "📷 Carte géologique chargée",
        "scenario.status.map_synced": "✅ Carte chargée et synchronisée"
    },
    "de": {
        "resource.view.section_base": "────────── 11 KANONISCHE BASISKARTEN ──────────",
        "resource.view.section_derived": "────────── ABGELEITETE KARTEN & ANOMALIEN ──────────",
        "resource.view.biomes": "🌿 1. Biome & Vegetationsdecke (GtC)",
        "resource.view.hydro": "🌊 2. Hydrographie & Flussnetz (SWBD / Slope)",
        "resource.view.coal": "⛏️ 3. Kohlevorkommen (4.1 COAL)",
        "resource.view.oil": "🛢️ 4. Erdölreserven (4.2 CRUDE_OIL)",
        "resource.view.gas": "🔥 5. Erdgasfelder (4.3 NATURAL_GAS)",
        "resource.view.uranium": "⚛️ 6. Uranerze & Kernspaltung (4.4 URANIUM)",
        "resource.view.helium3": "🌌 7. Helium-3 & Mondfusion (4.5 HELIUM_3)",
        "resource.view.iron_copper": "⛓️ 8. BIF-Eisen & Kupfermetalle (4.6 IRON_COPPER)",
        "resource.view.precious": "💎 9. Seltene Erden & Edelmetalle (4.7 PRECIOUS_REE / Li)",
        "resource.view.heat": "🌋 10. Mantelwärmefluss (4.8 MANTLE_HEAT)",
        "resource.view.aquifer": "💧 11. Aquifere & Süßwasser (4.9 FRESHWATER_AQUIFERS)",
        "resource.view.temp": "🌡️ 12. Oberflächentemperaturen & Mikroklima",
        "resource.view.seismic": "🌋 13. Tektonik & Seismische/Vulkanische Gefahren",
        "resource.view.aridity": "🏜️ 14. Aridität & Bodenversalzung",
        "resource.tensor.1.title": "⛏️ 4.1 Kohlevorkommen (COAL — USGS / BGR)",
        "resource.tensor.2.title": "🛢️ 4.2 Erdölreserven & Treibstoff (CRUDE_OIL — WEP)",
        "resource.tensor.3.title": "🔥 4.3 Erdgasfelder (NATURAL_GAS — WEP / BGR)",
        "resource.tensor.4.title": "⚛️ 4.4 Uranerze & Kernspaltung (URANIUM — IAEA UDEPO)",
        "resource.tensor.5.title": "🌌 4.5 Helium-3 & Mondfusion (HELIUM_3 — NASA / LPI)",
        "resource.tensor.6.title": "⛓️ 4.6 BIF-Eisen & Porphyr-Kupfermetalle (IRON_COPPER)",
        "resource.tensor.7.title": "💎 4.7 Seltene Erden, Lithium-Sole & Spodumen (PRECIOUS_REE / Li)",
        "resource.tensor.8.title": "🌋 4.8 Mantelwärmefluss & Geothermie (MANTLE_HEAT — IHFC / Davies 2013)",
        "resource.tensor.9.title": "💧 4.9 Beckenaquifere & Süßwasser (FRESHWATER_AQUIFERS — WHYMAP)",
        "resource.tensor.1.param1": "Kohlenflöz-Häufigkeit & Mächtigkeit (Gt)",
        "resource.tensor.2.param1": "Ölreserven & Tektonische Fallen (Gt)",
        "resource.tensor.3.param1": "Lagerstättendruck & Gashäufigkeit (10¹² m³)",
        "resource.tensor.4.param1": "Uran-Pegmatit Krustengehalt (ppm)",
        "resource.tensor.5.param1": "Helium-3 Regolith-Konzentration (ppb)",
        "resource.tensor.6.param1": "BIF-Eisen & Kupfer-Häufigkeit (Gt)",
        "resource.tensor.7.param1": "Seltene Erden & Edelmetallgehalt (Mt)",
        "resource.tensor.8.param1": "Mittlerer Mantelwärmefluss (mW/m²)",
        "resource.tensor.9.param1": "Unterirdische Aquiferkapazität (10³ km³)",
        "resource.tensor.1.param2": "Sumpf-Paläobiomasse-Schwellenwert (%)",
        "resource.tensor.2.param2": "Anoxische Muttergesteins-Reifung (%)",
        "resource.tensor.3.param2": "Thermogener Schiefergas-Anteil (%)",
        "resource.tensor.4.param2": "Hydrothermale Infiltrationsrate (%)",
        "resource.tensor.5.param2": "Sonnenwind-Ilmenit-Reifung (%)",
        "resource.tensor.6.param2": "Metallogenetische magmatische Aktivität (%)",
        "resource.tensor.7.param2": "Lithium-Sole Salargehalt (%)",
        "resource.tensor.8.param2": "Rift- & Hotspot-Intensität (%)",
        "resource.tensor.9.param2": "Kryosphären-Permeabilität im Untergrund (%)",
        "resource.mode.procedural_hotspots": "▶ Prozeduraler Modus (Hotspots & Physikmodell)",
        "resource.mode.import_file": "📂 Importierte Karte (PNG / GeoJSON / ESRI .asc)",
        "resource.section.geological_tensors": "4. GEOLOGIE, TEKTONIK UND ERZDOMAIN",
        "resource.btn.gen_single_tensor": "🪄 Generieren",
        "resource.label.tensor_seed": "🎲 Saatgut:",
        "resource.hint.geo_format": "PNG/GeoTIFF-Bild in 2:1 zylindrischer Äquidistantprojektion",
        "scenario.tensor.status.procedural": "✅ Prozeduraler Modus aktiv",
        "resource.status.procedural_fallback": "⚠️ Prozeduraler Rückfall aktiv",
        "scenario.status.no_file": "— Keine Datei geladen —",
        "resource.status.geo_loaded": "📷 Geologische Karte geladen",
        "scenario.status.map_synced": "✅ Karte geladen und synchronisiert"
    },
    "es": {
        "resource.view.section_base": "────────── 11 MAPAS CANÓNICOS BASE ──────────",
        "resource.view.section_derived": "────────── MAPAS DERIVADOS Y ANOMALÍAS ──────────",
        "resource.view.biomes": "🌿 1. Biomas y Cobertura Vegetal (GtC)",
        "resource.view.hydro": "🌊 2. Hidrografía y Red Fluvial (SWBD / Slope)",
        "resource.view.coal": "⛏️ 3. Yacimientos de Carbón (4.1 COAL)",
        "resource.view.oil": "🛢️ 4. Reservas de Petróleo Crudo (4.2 CRUDE_OIL)",
        "resource.view.gas": "🔥 5. Campos de Gas Natural (4.3 NATURAL_GAS)",
        "resource.view.uranium": "⚛️ 6. Minerales de Uranio y Fisión (4.4 URANIUM)",
        "resource.view.helium3": "🌌 7. Helio-3 y Fusión Lunar (4.5 HELIUM_3)",
        "resource.view.iron_copper": "⛓️ 8. Metales de Hierro BIF y Cobre (4.6 IRON_COPPER)",
        "resource.view.precious": "💎 9. Tierras Raras y Metales Preciosos (4.7 PRECIOUS_REE / Li)",
        "resource.view.heat": "🌋 10. Flujo Térmico del Manto (4.8 MANTLE_HEAT)",
        "resource.view.aquifer": "💧 11. Acuíferos y Agua Dulce (4.9 FRESHWATER_AQUIFERS)",
        "resource.view.temp": "🌡️ 12. Temperaturas de Superficie y Microclimas",
        "resource.view.seismic": "🌋 13. Tectónica y Riesgos Sísmicos/Volcánicos",
        "resource.view.aridity": "🏜️ 14. Aridez y Salinización del Suelo",
        "resource.tensor.1.title": "⛏️ 4.1 Yacimientos de Carbón (COAL — USGS / BGR)",
        "resource.tensor.2.title": "🛢️ 4.2 Reservas de Petróleo Crudo (CRUDE_OIL — WEP)",
        "resource.tensor.3.title": "🔥 4.3 Campos de Gas Natural (NATURAL_GAS — WEP / BGR)",
        "resource.tensor.4.title": "⚛️ 4.4 Minerales de Uranio y Fisión (URANIUM — IAEA UDEPO)",
        "resource.tensor.5.title": "🌌 4.5 Helio-3 y Fusión Lunar (HELIUM_3 — NASA / LPI)",
        "resource.tensor.6.title": "⛓️ 4.6 Metales de Hierro BIF y Cobre Porfídico (IRON_COPPER)",
        "resource.tensor.7.title": "💎 4.7 Tierras Raras, Salmueras de Litio y Espodumena (PRECIOUS_REE / Li)",
        "resource.tensor.8.title": "🌋 4.8 Flujo Térmico del Manto y Geotermia (MANTLE_HEAT — IHFC / Davies 2013)",
        "resource.tensor.9.title": "💧 4.9 Acuíferos de Cuenca y Agua Dulce (FRESHWATER_AQUIFERS — WHYMAP)",
        "resource.tensor.1.param1": "Abundancia y Espesor de Capas de Carbón (Gt)",
        "resource.tensor.2.param1": "Reservas de Petróleo y Trampas Tectónicas (Gt)",
        "resource.tensor.3.param1": "Presión del Yacimiento y Abundancia de Gas (10¹² m³)",
        "resource.tensor.4.param1": "Ley de Pegmatita de Uranio en la Corteza (ppm)",
        "resource.tensor.5.param1": "Concentración de Helio-3 en el Regolito (ppb)",
        "resource.tensor.6.param1": "Abundancia de Hierro BIF y Cobre (Gt)",
        "resource.tensor.7.param1": "Ley de Tierras Raras y Metales Preciosos (Mt)",
        "resource.tensor.8.param1": "Flujo Térmico Medio del Manto (mW/m²)",
        "resource.tensor.9.param1": "Capacidad del Acuífero Subsuperficial (10³ km³)",
        "resource.tensor.1.param2": "Umbral de Paleobiomasa de Pantano (%)",
        "resource.tensor.2.param2": "Maduración de Roca Madre Anóxica (%)",
        "resource.tensor.3.param2": "Fracción de Gas de Esquisot Termogénico (%)",
        "resource.tensor.4.param2": "Tasa de Infiltración Hidrotermal (%)",
        "resource.tensor.5.param2": "Maduración de Ilmenita por Viento Solar (%)",
        "resource.tensor.6.param2": "Actividad Magmática Metalogénica (%)",
        "resource.tensor.7.param2": "Ley de Salmuera de Litio en Salar (%)",
        "resource.tensor.8.param2": "Intensidad de Rifts y Puntos Calientes (%)",
        "resource.tensor.9.param2": "Permeabilidad de la Criosfera Subsuperficial (%)",
        "resource.mode.procedural_hotspots": "▶ Modo Procedimental (Puntos Calientes y Modelo Físico)",
        "resource.mode.import_file": "📂 Mapa Importado (PNG / GeoJSON / ESRI .asc)",
        "resource.section.geological_tensors": "4. DOMINIO DE GEOLOGÍA, TECTÓNICA Y MINERALES",
        "resource.btn.gen_single_tensor": "🪄 Generar",
        "resource.label.tensor_seed": "🎲 Semilla:",
        "resource.hint.geo_format": "Imagen PNG/GeoTIFF en proyección equirrectangular 2:1",
        "scenario.tensor.status.procedural": "✅ Modo procedimental activo",
        "resource.status.procedural_fallback": "⚠️ Alternativa procedimental activa",
        "scenario.status.no_file": "— Ningún archivo cargado —",
        "resource.status.geo_loaded": "📷 Mapa geológico cargado",
        "scenario.status.map_synced": "✅ Mapa cargado y sincronizado"
    },
    "zh": {
        "resource.view.section_base": "────────── 11 个基准规范地图 ──────────",
        "resource.view.section_derived": "────────── 衍生地图与异常区 ──────────",
        "resource.view.biomes": "🌿 1. 生物群落与植被覆盖图 (GtC)",
        "resource.view.hydro": "🌊 2. 水文与水系网络图 (SWBD / Slope)",
        "resource.view.coal": "⛏️ 3. 煤炭矿床 (4.1 COAL)",
        "resource.view.oil": "🛢️ 4. 原油储备 (4.2 CRUDE_OIL)",
        "resource.view.gas": "🔥 5. 天然气田 (4.3 NATURAL_GAS)",
        "resource.view.uranium": "⚛️ 6. 铀矿石与裂变资源 (4.4 URANIUM)",
        "resource.view.helium3": "🌌 7. 氦-3与月球聚变 (4.5 HELIUM_3)",
        "resource.view.iron_copper": "⛓️ 8. BIF铁矿与铜矿 (4.6 IRON_COPPER)",
        "resource.view.precious": "💎 9. 稀土与贵金属 (4.7 PRECIOUS_REE / Li)",
        "resource.view.heat": "🌋 10. 地幔热通量 (4.8 MANTLE_HEAT)",
        "resource.view.aquifer": "💧 11. 蓄水层与淡水 (4.9 FRESHWATER_AQUIFERS)",
        "resource.view.temp": "🌡️ 12. 地表温度与微气候",
        "resource.view.seismic": "🌋 13. 构造与地震/火山灾害",
        "resource.view.aridity": "🏜️ 14. 干旱度与土壤盐碱化",
        "resource.tensor.1.title": "⛏️ 4.1 煤炭矿床 (COAL — USGS / BGR)",
        "resource.tensor.2.title": "🛢️ 4.2 原油与燃料储备 (CRUDE_OIL — WEP)",
        "resource.tensor.3.title": "🔥 4.3 天然气田 (NATURAL_GAS — WEP / BGR)",
        "resource.tensor.4.title": "⚛️ 4.4 铀矿石与裂变资源 (URANIUM — IAEA UDEPO)",
        "resource.tensor.5.title": "🌌 4.5 氦-3与月球聚变 (HELIUM_3 — NASA / LPI)",
        "resource.tensor.6.title": "⛓️ 4.6 BIF铁矿与斑岩铜矿 (IRON_COPPER)",
        "resource.tensor.7.title": "💎 4.7 稀土、锂盐湖与锂辉石 (PRECIOUS_REE / Li)",
        "resource.tensor.8.title": "🌋 4.8 地幔热通量与地热 (MANTLE_HEAT — IHFC / Davies 2013)",
        "resource.tensor.9.title": "💧 4.9 流域蓄水层与淡水 (FRESHWATER_AQUIFERS — WHYMAP)",
        "resource.tensor.1.param1": "煤层丰度与厚度 (Gt)",
        "resource.tensor.2.param1": "石油储备与构造圈闭 (Gt)",
        "resource.tensor.3.param1": "储层压力与天然气丰度 (10¹² m³)",
        "resource.tensor.4.param1": "伟晶岩铀地壳品位 (ppm)",
        "resource.tensor.5.param1": "月壤氦-3浓度 (ppb)",
        "resource.tensor.6.param1": "BIF铁矿与铜矿丰度 (Gt)",
        "resource.tensor.7.param1": "稀土与贵金属品位 (Mt)",
        "resource.tensor.8.param1": "平均地幔热通量 (mW/m²)",
        "resource.tensor.9.param1": "地下蓄水层容量 (10³ km³)",
        "resource.tensor.1.param2": "沼泽古生物量阈值 (%)",
        "resource.tensor.2.param2": "缺氧烃源岩成熟度 (%)",
        "resource.tensor.3.param2": "热成因页岩气比例 (%)",
        "resource.tensor.4.param2": "热液渗滤率 (%)",
        "resource.tensor.5.param2": "太阳风钛铁矿成熟度 (%)",
        "resource.tensor.6.param2": "成矿岩浆活动度 (%)",
        "resource.tensor.7.param2": "盐湖锂盐水品位 (%)",
        "resource.tensor.8.param2": "裂谷与热点强度 (%)",
        "resource.tensor.9.param2": "地下冻土层渗透性 (%)",
        "resource.mode.procedural_hotspots": "▶ 过程化模式 (热点与物理模型)",
        "resource.mode.import_file": "📂 导入地图 (PNG / GeoJSON / ESRI .asc)",
        "resource.section.geological_tensors": "4. 地质、构造与矿产领域",
        "resource.btn.gen_single_tensor": "🪄 生成",
        "resource.label.tensor_seed": "🎲 随机种子:",
        "resource.hint.geo_format": "2:1 等角圆柱投影 PNG/GeoTIFF 图像",
        "scenario.tensor.status.procedural": "✅ 过程化模式激活",
        "resource.status.procedural_fallback": "⚠️ 过程化回退模式激活",
        "scenario.status.no_file": "— 未加载文件 —",
        "resource.status.geo_loaded": "📷 地质地图已加载",
        "scenario.status.map_synced": "✅ 地图已加载并同步"
    }
}

# Process each language bundle
for lang in LANGS:
    p = os.path.join(I18N_DIR, f"messages_{lang}.properties")
    existing = {}
    if os.path.exists(p):
        with open(p, encoding="utf-8", errors="replace") as f:
            for line in f:
                l = line.strip()
                if l and not l.startswith("#") and "=" in l:
                    k, _, v = l.partition("=")
                    existing[k.strip()] = v.strip()
    
    missing_keys = [k for k in key_fallbacks if k not in existing]
    
    # Also update any key present in TRANSLATIONS[lang]
    updated_count = 0
    added_count = 0
    
    # Read full file lines to overwrite or append
    lines = []
    if os.path.exists(p):
        with open(p, encoding="utf-8", errors="replace") as f:
            lines = f.readlines()
            
    # Check if keys in TRANSLATIONS need updating in existing file
    key_line_map = {}
    for idx, l in enumerate(lines):
        l_str = l.strip()
        if l_str and not l_str.startswith("#") and "=" in l_str:
            k = l_str.split("=")[0].strip()
            key_line_map[k] = idx

    # Apply updates for known TRANSLATIONS
    trans_map = TRANSLATIONS.get(lang, {})
    for k, v in trans_map.items():
        v_escaped = v.replace("\n", "\\n")
        if k in key_line_map:
            lines[key_line_map[k]] = f"{k}={v_escaped}\n"
            updated_count += 1
        else:
            lines.append(f"{k}={v_escaped}\n")
            added_count += 1
            existing[k] = v_escaped

    # Append any remaining missing keys from key_fallbacks
    remaining_missing = [k for k in key_fallbacks if k not in existing]
    if remaining_missing:
        lines.append(f"\n# Synchronized I18n additions ({lang.upper()})\n")
        for k in sorted(remaining_missing):
            fallback = key_fallbacks[k]
            val = trans_map.get(k, fallback)
            val_escaped = val.replace("\n", "\\n")
            lines.append(f"{k}={val_escaped}\n")
            added_count += 1

    with open(p, "w", encoding="utf-8") as f:
        f.writelines(lines)

    print(f"[{lang.upper()}] Updated {updated_count} keys, added {added_count} new keys to messages_{lang}.properties")

print("All language properties updated successfully!")
