#!/usr/bin/env python3
import os
import sys

I18N_DIR = os.path.join("src", "main", "resources", "i18n")
LANGS = ["en", "fr", "de", "es", "zh"]

NEW_KEYS = {
    "dynamic_engine.error.file_not_found": {
        "en": "The specified Java source file does not exist.",
        "fr": "Le fichier source Java spécifié n'existe pas.",
        "de": "Die angegebene Java-Quelldatei existiert nicht.",
        "es": "El archivo de código fuente Java especificado no existe.",
        "zh": "指定的 Java 源文件不存在。"
    },
    "dynamic_engine.error.not_java": {
        "en": "The file must have a .java extension.",
        "fr": "Le fichier doit avoir l'extension .java.",
        "de": "Die Datei muss die Erweiterung .java haben.",
        "es": "El archivo debe tener una extensión .java.",
        "zh": "文件必须具有 .java 扩展名。"
    },
    "dynamic_engine.error.no_jdk": {
        "en": "Java JDK Compiler (javac) not detected in current execution environment. JDK 21+ required for on-the-fly compilation.",
        "fr": "Compilateur Java JDK (javac) non détecté dans l'environnement d'exécution actuel. JDK 21+ requis pour la compilation à la volée.",
        "de": "Java JDK-Compiler (javac) in der aktuellen Laufzeitumgebung nicht gefunden. JDK 21+ für On-the-Fly-Kompilierung erforderlich.",
        "es": "Compilador Java JDK (javac) no detectado en el entorno actual. Se requiere JDK 21+ para la compilación en caliente.",
        "zh": "当前执行环境中未检测到 Java JDK 编译器 (javac)。实时编译需要 JDK 21+。"
    },
    "dynamic_engine.error.javac_failed": {
        "en": "Javac compilation error for: ",
        "fr": "Erreur de compilation javac pour : ",
        "de": "Javac-Kompilierungsfehler für: ",
        "es": "Error de compilación de javac para: ",
        "zh": "Javac 编译错误："
    },
    "dynamic_engine.error.not_plugin": {
        "en": "Class must implement org.ether.society.procedural.ProceduralEnginePlugin",
        "fr": "La classe doit implémenter org.ether.society.procedural.ProceduralEnginePlugin",
        "de": "Klasse muss org.ether.society.procedural.ProceduralEnginePlugin implementieren",
        "es": "La clase debe implementar org.ether.society.procedural.ProceduralEnginePlugin",
        "zh": "类必须实现 org.ether.society.procedural.ProceduralEnginePlugin"
    },
    "dynamic_engine.status.success": {
        "en": "Engine compiled and registered successfully!",
        "fr": "Moteur compilé et enregistré avec succès !",
        "de": "Engine erfolgreich kompiliert und registriert!",
        "es": "¡Motor compilado y registrado con éxito!",
        "zh": "引擎编译并注册成功！"
    },
    "dynamic_engine.error.generic": {
        "en": "Execution error: ",
        "fr": "Erreur d'exécution : ",
        "de": "Ausführungsfehler: ",
        "es": "Error de ejecución: ",
        "zh": "执行错误："
    },
    "planet.source.mars_biome": {
        "en": "🌿 Mars Planetary Terrains & Volcanic Plains",
        "fr": "🌿 Terrains planétaires de Mars & Plaines volcaniques",
        "de": "🌿 Mars Planetare Gelände & Vulkanische Ebenen",
        "es": "🌿 Terrenos planetarios de Marte y llanuras volcánicas",
        "zh": "🌿 火星行星地貌与火山平原"
    },
    "planet.source.venus_biome": {
        "en": "🌿 Venus Volcanic Plains & Tesserae",
        "fr": "🌿 Plaines volcaniques de Vénus & Tesserae",
        "de": "🌿 Venus Vulkanische Ebenen & Tesserae",
        "es": "🌿 Llanuras volcánicas de Venus y Tesserae",
        "zh": "🌿 金星火山平原与镶嵌地形"
    },
    "resource.source.venus_geology": {
        "en": "🪨 Venus Magellan Pyrite / Radar Minerals",
        "fr": "🪨 Pyrite de Magellan / Minéraux radar de Vénus",
        "de": "🪨 Venus Magellan Pyrit / Radar-Mineralien",
        "es": "🪨 Pirita de Magallanes de Venus / Minerales radar",
        "zh": "🪨 金星麦哲伦黄铁矿 / 雷达反射矿物"
    },
    "planet.source.moon_biome": {
        "en": "🌿 Moon Lunar Maria & Anorthosite Highlands",
        "fr": "🌿 Mers lunaires (Maria) & Hauts plateaux d'anorthosite",
        "de": "🌿 Mondmare & Anorthosit-Hochland",
        "es": "🌿 Mares lunares (Maria) y tierras altas de anortosita",
        "zh": "🌿 月球月海与斜长岩高地"
    },
    "planet.source.mercury_dem": {
        "en": "📷 Mercury MESSENGER MLA DEM (NASA PDS)",
        "fr": "📷 MNE MLA de Mercure MESSENGER (NASA PDS)",
        "de": "📷 Merkur MESSENGER MLA DEM (NASA PDS)",
        "es": "📷 DEM MLA de Mercurio MESSENGER (NASA PDS)",
        "zh": "📷 水星信使号 MLA 数字高程模型 (NASA PDS)"
    },
    "planet.source.mercury_biome": {
        "en": "🌿 Mercury Smooth & Intercrater Plains",
        "fr": "🌿 Plaines lisses et intercratères de Mercure",
        "de": "🌿 Merkur Glatte & Interkrater-Ebenen",
        "es": "🌿 Llanuras lisas e intercráteres de Mercurio",
        "zh": "🌿 水星光滑平原与撞击坑间平原"
    },
    "resource.source.mercury_geology": {
        "en": "🪨 Mercury High-Iron & PSR Ice Deposits",
        "fr": "🪨 Dépôts de glace PSR & Fer élevé de Mercure",
        "de": "🪨 Merkur Eis- und Hoch-Eisen-Vorkommen",
        "es": "🪨 Depósitos de hielo en sombra permanente y alto contenido de hierro de Mercurio",
        "zh": "🪨 水星高铁矿与极区永久阴影区水冰沉积"
    },
    "planet.legend.depressions": {
        "en": "Depressions / Abysses",
        "fr": "Dépressions / Abysses",
        "de": "Senken / Tiefebenen",
        "es": "Depresiones / Abismos",
        "zh": "低洼 / 深渊"
    },
    "planet.legend.plains": {
        "en": "Plains / Sea Level 0",
        "fr": "Plaines / Niveau 0",
        "de": "Ebenen / Meereshöhe 0",
        "es": "Llanuras / Nivel del mar 0",
        "zh": "平原 / 基准海平面 0"
    },
    "planet.legend.highlands": {
        "en": "Highlands & Plateaus",
        "fr": "Reliefs & Plateaux",
        "de": "Hochland & Plateaus",
        "es": "Tierras altas y mesetas",
        "zh": "高地与高原"
    },
    "preset.dialog.save_label": {
        "en": "Name:",
        "fr": "Nom :",
        "de": "Name:",
        "es": "Nombre:",
        "zh": "名称："
    },
    "scenario.label.load_map_section": {
        "en": "Load Map Layer:",
        "fr": "Charger la Carte :",
        "de": "Kartenlayer laden:",
        "es": "Cargar capa de mapa:",
        "zh": "加载地图图层："
    },
    "scenario.label.format_section": {
        "en": "Map Format:",
        "fr": "Format de Carte :",
        "de": "Kartenformat:",
        "es": "Formato del mapa:",
        "zh": "地图格式："
    },
    "scenario.alert.export_bundle_desc": {
        "en": "Unified simulation bundle signed & saved to:\n",
        "fr": "Pack de simulation unifié signé et enregistré sous :\n",
        "de": "Einheitliches Simulationspaket signiert und gespeichert unter:\n",
        "es": "Paquete unificado de simulación firmado y guardado en:\n",
        "zh": "统一模拟配置包已签名并保存至：\n"
    },
    "scenario.title.export_error": {
        "en": "Export Error",
        "fr": "Erreur d'exportation",
        "de": "Exportfehler",
        "es": "Error de exportación",
        "zh": "导出错误"
    },
    "scenario.alert.export_failed": {
        "en": "Cannot save bundle: ",
        "fr": "Impossible d'enregistrer le pack : ",
        "de": "Paket kann nicht gespeichert werden: ",
        "es": "No se পারে guardar el paquete: ",
        "zh": "无法保存配置包："
    },
    "scenario.alert.import_bundle_success": {
        "en": "Physical, ecological, and demographic parameters loaded successfully from bundle.",
        "fr": "Paramètres physiques, écologiques et démographiques chargés avec succès depuis le pack.",
        "de": "Physikalische, ökologische und demografische Parameter erfolgreich aus dem Paket geladen.",
        "es": "Parámetros físicos, ecológicos y demográficos cargados con éxito desde el paquete.",
        "zh": "物理、生态和人口参数已成功从配置包加载。"
    },
    "scenario.title.import_error": {
        "en": "Import Error",
        "fr": "Erreur d'importation",
        "de": "Importfehler",
        "es": "Error de importación",
        "zh": "导入错误"
    },
    "scenario.alert.import_bundle_invalid": {
        "en": "Invalid bundle file: ",
        "fr": "Fichier de pack invalide : ",
        "de": "Ungültige Paketdatei: ",
        "es": "Archivo de paquete no válido: ",
        "zh": "无效的配置包文件："
    },
    "scenario.status.empirical_loaded": {
        "en": "📷 Empirical baseline layer loaded",
        "fr": "📷 Couche empirique de référence chargée",
        "de": "📷 Empirische Basisschicht geladen",
        "es": "📷 Capa empírica de referencia cargada",
        "zh": "📷 经验基准图层已加载"
    },
    "scenario.hint.cultural_format": {
        "en": "PNG / GeoJSON in 2:1 equirectangular projection",
        "fr": "PNG / GeoJSON en projection équirectangulaire 2:1",
        "de": "PNG / GeoJSON in 2:1 Plattkarte-Projektion",
        "es": "PNG / GeoJSON en proyección equirrectangular 2:1",
        "zh": "PNG / GeoJSON（2:1 正距圆柱投影）"
    },
    "scenario.status.no_cells": {
        "en": "🪐 Preview pending generation...",
        "fr": "🪐 Aperçu en attente de génération...",
        "de": "🪐 Vorschau wartet auf Generierung...",
        "es": "🪐 Vista previa pendiente de generación...",
        "zh": "🪐 预览等待生成中..."
    },
    "scenario.confirm_restart.title": {
        "en": "Simulation In Progress",
        "fr": "Simulation en cours",
        "de": "Simulation läuft",
        "es": "Simulación en curso",
        "zh": "模拟正在运行"
    },
    "scenario.confirm_restart.header": {
        "en": "A simulation is currently active",
        "fr": "Une simulation est actuellement active",
        "de": "Eine Simulation ist derzeit aktiv",
        "es": "Actualmente hay una simulación activa",
        "zh": "当前有一个模拟正在运行"
    },
    "scenario.confirm_restart.content": {
        "en": "Launching this new configuration will reset the active simulation. Do you wish to proceed?",
        "fr": "Lancer cette nouvelle configuration réinitialisera la simulation en cours. Souhaitez-vous continuer ?",
        "de": "Das Starten dieser neuen Konfiguration setzt die aktive Simulation zurück. Möchten Sie fortfahren?",
        "es": "Iniciar esta nueva configuración restablecerá la simulación activa. ¿Desea continuar?",
        "zh": "启动此新配置将重置当前活动的模拟。是否继续？"
    },
    "scenario.alert.export_template_success": {
        "en": "Custom engine template exported successfully:\n",
        "fr": "Modèle de moteur personnalisé exporté avec succès :\n",
        "de": "Vorlage für benutzerdefinierte Engine erfolgreich exportiert:\n",
        "es": "Plantilla de motor personalizada exportada con éxito:\n",
        "zh": "自定义引擎模板成功导出：\n"
    },
    "scenario.engine.custom_badge": {
        "en": "Custom Engine",
        "fr": "Moteur Personnalisé",
        "de": "Benutzerdefinierte Engine",
        "es": "Motor personalizado",
        "zh": "自定义引擎"
    },
    "scenario.warning.demo_body_mismatch": {
        "en": "⚠️ Planetary Inconsistency: Demographic source \"%s\" selected on terrestrial Earth relief (Tab 1).",
        "fr": "⚠️ Incohérence planétaire : Source démographique « %s » sélectionnée sur un relief terrestre (Onglet 1).",
        "de": "⚠️ Planetare Inkonsistenz: Demografische Quelle \"%s\" auf irdischem Relief ausgewählt (Tab 1).",
        "es": "⚠️ Inconsistencia planetaria: Fuente demográfica \"%s\" seleccionada en relieve terrestre (Pestaña 1).",
        "zh": "⚠️ 行星不一致：在地球地形上选择了人口来源“%s”（标签页 1）。"
    },
    "scenario.warning.culture_body_mismatch": {
        "en": "⚠️ Cultural Inconsistency: Source \"%s\" (%s) applied to terrestrial Earth relief.",
        "fr": "⚠️ Incohérence culturelle : Source « %s » (%s) appliquée sur la Terre.",
        "de": "⚠️ Kulturelle Inkonsistenz: Quelle \"%s\" (%s) auf Erde angewendet.",
        "es": "⚠️ Inconsistencia cultural: Fuente \"%s\" (%s) aplicada a la Tierra.",
        "zh": "⚠️ 文化不一致：在地球地形上应用了来源“%s”（%s）。"
    },
    "scenario.warning.ocean_density": {
        "en": "⚠️ Geographic Incompatibility: %.1f%% of imported demographic density is located in ocean/underwater zones (%s).",
        "fr": "⚠️ Incompatibilité géographique : %.1f%% de la densité démographique importée se trouve en zone océanique / sous-marine (%s).",
        "de": "⚠️ Geografische Inkompatibilität: %.1f%% der importierten Bevölkerungsdichte liegt im Ozeanbereich (%s).",
        "es": "⚠️ Incompatibilidad geográfica: El %.1f%% de la densidad demográfica importada se encuentra en zonas oceánicas (%s).",
        "zh": "⚠️ 地理不兼容：导入的人口密度有 %.1f%% 位于海洋/水下区域（%s）。"
    },
    "scenario.warning.ocean_culture": {
        "en": "⚠️ Cultural Incompatibility: %.1f%% of tensor intensity \"%s\" is located on open ocean (%s).",
        "fr": "⚠️ Incompatibilité culturelle : %.1f%% de l'intensité du tenseur « %s » est située sur l'océan (%s).",
        "de": "⚠️ Kulturelle Inkompatibilität: %.1f%% der Tensorintensität \"%s\" liegt auf dem offenen Meer (%s).",
        "es": "⚠️ Incompatibilidad cultural: El %.1f%% de la intensidad del tensor \"%s\" está en océano abierto (%s).",
        "zh": "⚠️ 文化不兼容：张量“%s”强度的 %.1f%% 位于开阔海洋上（%s）。"
    },
    "scenario.warning.hostile_environment": {
        "en": "⚠️ Hostile Environment (Tab 1): Extreme Pressure (%.2f atm) or Temperature (%.1f °C) — Human survival requires sealed dome habitats.",
        "fr": "⚠️ Environnement hostile (Onglet 1) : Pression (%.2f atm) ou Température (%.1f °C) extrême — Survie humaine conditionnée à des habitats scellés.",
        "de": "⚠️ Lebensfeindliche Umwelt (Tab 1): Extremer Druck (%.2f atm) oder Temperatur (%.1f °C) — Menschliches Überleben erfordert versiegelte Kuppelhabitate.",
        "es": "⚠️ Entorno hostil (Pestaña 1): Presión (%.2f atm) o Temperatura (%.1f °C) extremas — La supervivencia humana requiere hábitats de cúpula sellados.",
        "zh": "⚠️ 恶劣环境（标签页 1）：极端气压 (%.2f atm) 或温度 (%.1f °C) — 人类生存需要密封穹顶栖息地。"
    },
    "scenario.validation.subhead_warnings": {
        "en": "Compatibility Warnings:",
        "fr": "Avertissements de compatibilité :",
        "de": "Kompatibilitätswarnungen:",
        "es": "Advertencias de compatibilidad:",
        "zh": "兼容性警告："
    },
    "scenario.validation.header_warnings": {
        "en": "GEOGRAPHIC & ENVIRONMENTAL COMPATIBILITY WARNINGS (TABS 1 & 2):",
        "fr": "AVERTISSEMENTS DE COMPATIBILITÉ GÉOGRAPHIQUE & ENVIRONNEMENTALE (ONGLETS 1 & 2) :",
        "de": "GEOGRAFISCHE & UMWELTKOMPATIBILITÄTSWARNUNGEN (TABS 1 & 2):",
        "es": "ADVERTENCIAS DE COMPATIBILIDAD GEOGRÁFICA Y AMBIENTAL (PESTAÑAS 1 Y 2):",
        "zh": "地理与环境兼容性警告（标签页 1 与 2）："
    },
    "common.combo.prompt_source": {
        "en": "-- Select Source Dataset --",
        "fr": "-- Sélectionner le Jeu de Données Source --",
        "de": "-- Quelldatensatz auswählen --",
        "es": "-- Seleccionar conjunto de datos de origen --",
        "zh": "-- 选择源数据集 --"
    },
    "sim.layer.datalayers": {
        "en": "Simulation Data Layers",
        "fr": "Couches de Données de Simulation",
        "de": "Simulations-Datenebenen",
        "es": "Capas de datos de simulación",
        "zh": "模拟数据图层"
    },
    "sim.layer.uioverlays": {
        "en": "UI & Vector Overlays",
        "fr": "Superpositions Vectorielles & UI",
        "de": "UI & Vektor-Overlays",
        "es": "Superposiciones vectoriales y de interfaz",
        "zh": "UI 与矢量叠加层"
    },
    "sim.tooltip.fastforward_end": {
        "en": "Fast-Forward to Simulation End Year (Run at Maximum Simulation Tick Velocity)",
        "fr": "Avance Rapide jusqu'à l'Année de Fin de Simulation (Exécution à Vitesse Maximale)",
        "de": "Schnellvorlauf zum Simulations-Endjahr (Ausführung mit maximaler Tick-Geschwindigkeit)",
        "es": "Avance rápido hasta el año de finalización de la simulación (a máxima velocidad)",
        "zh": "快进至模拟结束年份（以最高物理演算速度全速推进）"
    }
}

def load_bundle(filepath):
    bundle = {}
    if not os.path.exists(filepath):
        return bundle
    with open(filepath, "r", encoding="utf-8", errors="replace") as f:
        for line in f:
            line_str = line.strip()
            if line_str and not line_str.startswith("#") and "=" in line_str:
                k, _, v = line_str.partition("=")
                bundle[k.strip()] = v.strip()
    return bundle

def save_bundle(filepath, bundle):
    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write("# Ether Internationalization Resource Bundle (UTF-8)\n")
        f.write(f"# Total Keys: {len(bundle)}\n\n")
        for k in sorted(bundle.keys()):
            f.write(f"{k}={bundle[k]}\n")

bundles = {lang: load_bundle(os.path.join(I18N_DIR, f"messages_{lang}.properties")) for lang in LANGS}

# Inject new keys
for key, trans in NEW_KEYS.items():
    for lang in LANGS:
        if lang in trans:
            bundles[lang][key] = trans[lang]

# Ensure complete key union across all languages
all_keys = set()
for lang in LANGS:
    all_keys.update(bundles[lang].keys())

for key in sorted(all_keys):
    for lang in LANGS:
        if key not in bundles[lang]:
            # fallback to en or fr
            fallback = bundles["en"].get(key, bundles["fr"].get(key, key))
            bundles[lang][key] = fallback

# Save each language bundle
for lang in LANGS:
    target_file = os.path.join(I18N_DIR, f"messages_{lang}.properties")
    save_bundle(target_file, bundles[lang])
    print(f"Saved {target_file} with {len(bundles[lang])} keys.")

# Save default messages.properties (matching messages_en.properties)
default_file = os.path.join(I18N_DIR, "messages.properties")
save_bundle(default_file, bundles["en"])
print(f"Saved {default_file} with {len(bundles['en'])} keys.")
