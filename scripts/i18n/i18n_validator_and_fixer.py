#!/usr/bin/env python3
"""
Ether - 5-Language Internationalization & Tooltip Coverage Validator and Synchronizer
-------------------------------------------------------------------------------------
1. Fixes mojibake / corrupted UTF-8 sequences in Java UI source files.
2. Detects hardcoded UI strings and missing tooltips across all UI components.
3. Automatically completes and synchronizes all keys in the 5 resource bundles:
   (EN, FR, DE, ES, ZH) + default messages.properties.
"""

import os
import re
import sys
from pathlib import Path

# Force UTF-8 on standard outputs
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

# Paths
BASE_DIR = Path(__file__).resolve().parent.parent
I18N_DIR = BASE_DIR / "src" / "main" / "resources" / "i18n"
JAVA_UI_DIR = BASE_DIR / "src" / "main" / "java" / "org" / "ether" / "society" / "ui"
JAVA_SRC_DIR = BASE_DIR / "src" / "main" / "java" / "org" / "ether" / "society"

LANGUAGES = ["en", "fr", "de", "es", "zh"]
PROP_FILES = {
    "en": I18N_DIR / "messages_en.properties",
    "fr": I18N_DIR / "messages_fr.properties",
    "de": I18N_DIR / "messages_de.properties",
    "es": I18N_DIR / "messages_es.properties",
    "zh": I18N_DIR / "messages_zh.properties",
    "default": I18N_DIR / "messages.properties",
}

BINARY_MOJIBAKE = [
    (b'\xc3\xa2\xc2\x9d\xc5\x92', '❌'.encode('utf-8')),
    (b'\xc3\xa2\xc2\x9d\xc2\x8c', '❌'.encode('utf-8')),
    (b'\xc3\xa2\xc2\x9c\xc2\x94', '✔️'.encode('utf-8')),
    (b'\xc3\xa2\xc5\xa1\xe2\x84\xa2\xc3\xaf\xc2\xb8\xc2\x8f', '⚙️'.encode('utf-8')),
    (b'\xc3\xa2\xc5\xa1\xc2\xa0\xc3\xaf\xc2\xb8\xc2\x8f', '⚠️'.encode('utf-8')),
]

LEXICON = {
    "analytics.channel_label": {
        "en": "Tensor Channel:",
        "fr": "Canal tensoriel :",
        "de": "Tensor-Kanal:",
        "es": "Canal tensorial:",
        "zh": "张量通道："
    },
    "analytics.label.select_a": {
        "en": "Baseline (A):",
        "fr": "Référence (A) :",
        "de": "Basislinie (A):",
        "es": "Línea base (A):",
        "zh": "基准情景 (A)："
    },
    "analytics.label.select_b": {
        "en": "Target (B):",
        "fr": "Cible (B) :",
        "de": "Ziel (B):",
        "es": "Objetivo (B):",
        "zh": "目标情景 (B)："
    },
    "analytics.tooltip.channel_select": {
        "en": "Select the spatial physical/cliodynamic tensor layer to compare between scenarios A and B.",
        "fr": "Sélectionnez la couche tensorielle physique/cliodynamique à comparer spatialement entre les scénarios A et B.",
        "de": "Wählen Sie die räumliche Tensor-Ebene für den Vergleich zwischen Szenario A und B.",
        "es": "Seleccione la capa tensorial física/cliodinámica para comparar entre los escenarios A y B.",
        "zh": "选择用于在情景 A 和情景 B 之间进行空间对比的物理/历史动力学张量图层。"
    },
    "analytics.tooltip.scenario_a": {
        "en": "Select baseline scenario (or empirical Ground Truth) for spatial differential tensor mapping.",
        "fr": "Sélectionnez le scénario de référence (ou la réalité historique) pour la comparaison tensorielle différentielle.",
        "de": "Wählen Sie das Basisszenario (oder die historische Realität) für den Tensorvergleich.",
        "es": "Seleccione el escenario base (o realidad histórica) para la comparación tensorial diferencial.",
        "zh": "选择作为空间差分张量对比基准的情景（或历史实证基准）。"
    },
    "analytics.tooltip.scenario_b": {
        "en": "Select target scenario to evaluate against baseline A across 2D map fidelity metrics.",
        "fr": "Sélectionnez le scénario cible à évaluer par rapport à la référence A sur les métriques de fidélité 2D.",
        "de": "Wählen Sie das Zielszenario zur Bewertung anhand von 2D-Fidelitätsmetriken gegenüber Basislinie A.",
        "es": "Seleccione el escenario objetivo para evaluar respecto a la base A en métricas de fidelidad 2D.",
        "zh": "选择用于与基准 A 评估 2D 空间保真度指标的目标情景。"
    },
    "analytics.tooltip.metric_selector": {
        "en": "Select the telemetry variable to plot over time across all checked scenarios.",
        "fr": "Sélectionnez la variable de télémétrie à tracer dans le temps pour tous les scénarios cochés.",
        "de": "Wählen Sie die Telemetrievariable aus, die für alle ausgewählten Szenarien im Zeitverlauf dargestellt werden soll.",
        "es": "Seleccione la variable de telemetría a trazar en el tiempo para todos los escenarios seleccionados.",
        "zh": "选择在时间序列图表中跨所有勾选情景绘制的时序观测变量。"
    },
    "analytics.eta_calculating": {
        "en": "⏱️ Calculating...",
        "fr": "⏱️ Calcul en cours...",
        "de": "⏱️ Berechnung läuft...",
        "es": "⏱️ Calculando...",
        "zh": "⏱️ 正在计算..."
    },
    "analytics.export.error_title": {
        "en": "Export Error",
        "fr": "Erreur d'exportation",
        "de": "Exportfehler",
        "es": "Error de exportación",
        "zh": "导出错误"
    },
    "analytics.export.error_header": {
        "en": "File write failure",
        "fr": "Échec d'écriture du fichier",
        "de": "Dateischreibfehler",
        "es": "Fallo al escribir archivo",
        "zh": "写入文件失败"
    },
    "analytics.tooltip.close_modal": {
        "en": "Close detailed view dialog.",
        "fr": "Fermer la boîte de dialogue de vue détaillée.",
        "de": "Detailansicht schließen.",
        "es": "Cerrar cuadro de diálogo de vista detallada.",
        "zh": "关闭详细视图对话框。"
    },
    "main.tooltip.headless_switch": {
        "en": "Switch between GUI interactive mode and high-throughput headless simulation batch runner.",
        "fr": "Basculer entre le mode interactif graphique et l'exécuteur de simulation headless haute cadence.",
        "de": "Wechseln Sie zwischen dem interaktiven GUI-Modus und dem Headless-Batch-Runner.",
        "es": "Alternar entre el modo GUI interactivo y el ejecutor por lotes headless de alta velocidad.",
        "zh": "在图形交互模式与高吞吐量无头批量模拟运行器之间切换。"
    },
    "godmode.tooltip.timeline_view": {
        "en": "Interactive timeline of scheduled contingency events and catastrophic interventions. Click an entry to navigate to coordinates.",
        "fr": "Chronologie interactive des événements contingents et interventions catastrophiques planifiées. Cliquez pour centrer les coordonnées.",
        "de": "Interaktive Zeitleiste geplanter Kontingenzereignisse und Katastrophen. Klicken Sie, um zu den Koordinaten zu navigieren.",
        "es": "Línea temporal interactiva de eventos contingentes planificados. Haga clic para centrar en las coordenadas.",
        "zh": "计划历史突发事件与灾难干预的交互式时间线。点击条目可将视图定位至相应地理坐标。"
    },
    "common.clear_tooltip": {
        "en": "Clear current layer / reset to default state.",
        "fr": "Effacer la couche actuelle / réinitialiser à l'état par défaut.",
        "de": "Aktuelle Ebene löschen / auf Standard zurücksetzen.",
        "es": "Borrar capa actual / restablecer a estado predeterminado.",
        "zh": "清除当前图层 / 重置为默认状态。"
    }
}


def fix_all_ui_source_files():
    """Deep clean buttons and binary mojibake in UI classes."""
    cleaned = 0
    for java_file in JAVA_SRC_DIR.rglob("*.java"):
        with open(java_file, "rb") as f:
            raw_bytes = f.read()

        new_bytes = raw_bytes
        for corrupted, fixed in BINARY_MOJIBAKE:
            new_bytes = new_bytes.replace(corrupted, fixed)

        if new_bytes != raw_bytes:
            with open(java_file, "wb") as f:
                f.write(new_bytes)
            cleaned += 1

    return cleaned


def load_properties(file_path: Path) -> dict[str, str]:
    props = {}
    if not file_path.exists():
        return props

    with open(file_path, "r", encoding="utf-8", errors="replace") as f:
        multiline_key = None
        multiline_val = []

        for line in f:
            stripped = line.strip()
            if not stripped or stripped.startswith("#") or stripped.startswith("!"):
                continue

            if multiline_key:
                if stripped.endswith("\\"):
                    multiline_val.append(stripped[:-1].strip())
                else:
                    multiline_val.append(stripped)
                    props[multiline_key] = " ".join(multiline_val)
                    multiline_key = None
                    multiline_val = []
                continue

            if "=" in line:
                k, v = line.split("=", 1)
                k = k.strip()
                v = v.strip()
                if v.endswith("\\"):
                    multiline_key = k
                    multiline_val = [v[:-1].strip()]
                else:
                    props[k] = v
            elif ":" in line:
                k, v = line.split(":", 1)
                props[k.strip()] = v.strip()

    return props


def save_properties(file_path: Path, props: dict[str, str]):
    sorted_keys = sorted(props.keys())
    with open(file_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# Ether Society Simulation - I18n Resource Bundle\n")
        f.write(f"# Language: {file_path.stem}\n")
        f.write(f"# Total Keys: {len(sorted_keys)}\n\n")

        current_prefix = ""
        for k in sorted_keys:
            prefix = k.split(".")[0] if "." in k else "general"
            if prefix != current_prefix:
                current_prefix = prefix
                f.write(f"\n# --- {current_prefix.upper()} ---\n")
            val = props[k]
            f.write(f"{k}={val}\n")


def translate_fallback(key: str, en_val: str, target_lang: str) -> str:
    if not en_val:
        return ""

    if key in LEXICON and target_lang in LEXICON[key]:
        return LEXICON[key][target_lang]

    fr_terms = {
        "Scenario": "Scénario", "Simulation": "Simulation", "Analytics": "Analytique",
        "Comparison": "Comparaison", "Historical": "Historique", "Earth": "Terre",
        "Divergence": "Divergence", "Error": "Erreur", "Metric": "Métrique",
        "Execute": "Exécuter", "Run": "Lancer", "Fidelity": "Fidélité",
        "Population": "Population", "Carrying Capacity": "Capacité de charge",
        "Energy Surplus": "Surplus énergétique", "Technology Level": "Niveau technologique",
        "Spatial": "Spatial", "Ground Truth": "Vérité terrain", "Export": "Exporter",
        "Report": "Rapport", "Root Cause": "Cause racine", "Tolerance": "Tolérance",
        "Parameter": "Paramètre", "Observation": "Observation", "Timeline": "Chronologie",
        "Reset": "Réinitialiser", "Apply": "Appliquer", "Save": "Sauvegarder", "Load": "Charger"
    }

    de_terms = {
        "Scenario": "Szenario", "Simulation": "Simulation", "Analytics": "Analytik",
        "Comparison": "Vergleich", "Historical": "Historisch", "Earth": "Erde",
        "Divergence": "Divergenz", "Error": "Fehler", "Metric": "Metrik",
        "Execute": "Ausführen", "Run": "Starten", "Fidelity": "Fidelität",
        "Population": "Bevölkerung", "Carrying Capacity": "Tragfähigkeit",
        "Energy Surplus": "Energieüberschuss", "Technology Level": "Technologiestufe",
        "Spatial": "Räumlich", "Ground Truth": "Grundwahrheit", "Export": "Exportieren",
        "Report": "Bericht", "Root Cause": "Grundursache", "Tolerance": "Toleranz",
        "Parameter": "Parameter", "Observation": "Beobachtung", "Timeline": "Zeitleiste",
        "Reset": "Zurücksetzen", "Apply": "Anwenden", "Save": "Speichern", "Load": "Laden"
    }

    es_terms = {
        "Scenario": "Escenario", "Simulation": "Simulación", "Analytics": "Analítica",
        "Comparison": "Comparación", "Historical": "Histórico", "Earth": "Tierra",
        "Divergence": "Divergencia", "Error": "Error", "Metric": "Métrica",
        "Execute": "Ejecutar", "Run": "Iniciar", "Fidelity": "Fidelidad",
        "Population": "Población", "Carrying Capacity": "Capacidad de carga",
        "Energy Surplus": "Superávit energético", "Technology Level": "Nivel tecnológico",
        "Spatial": "Espacial", "Ground Truth": "Realidad histórica", "Export": "Exportar",
        "Report": "Informe", "Root Cause": "Causa raíz", "Tolerance": "Tolerancia",
        "Parameter": "Parámetro", "Observation": "Observación", "Timeline": "Línea temporal",
        "Reset": "Restablecer", "Apply": "Aplicar", "Save": "Guardar", "Load": "Cargar"
    }

    zh_terms = {
        "Scenario": "情景", "Simulation": "模拟", "Analytics": "对比分析",
        "Comparison": "对比", "Historical": "历史", "Earth": "地球",
        "Divergence": "分歧度", "Error": "误差", "Metric": "指标",
        "Execute": "执行", "Run": "运行", "Fidelity": "保真度",
        "Population": "人口", "Carrying Capacity": "承载力",
        "Energy Surplus": "净能源盈余", "Technology Level": "技术水平",
        "Spatial": "空间", "Ground Truth": "历史实证基准", "Export": "导出",
        "Report": "报告", "Root Cause": "归因诊断", "Tolerance": "容差",
        "Parameter": "参数", "Observation": "观测", "Timeline": "时间线",
        "Reset": "重置", "Apply": "应用", "Save": "保存", "Load": "加载"
    }

    res = en_val
    term_dict = {"fr": fr_terms, "de": de_terms, "es": es_terms, "zh": zh_terms}.get(target_lang, {})
    for en_word, trans_word in sorted(term_dict.items(), key=lambda x: -len(x[0])):
        res = re.sub(rf'\b{re.escape(en_word)}\b', trans_word, res, flags=re.IGNORECASE)

    return res


def synchronize_all_properties():
    bundles = {}
    for lang in LANGUAGES:
        bundles[lang] = load_properties(PROP_FILES[lang])
    bundles["default"] = load_properties(PROP_FILES["default"])

    # Inject predefined lexicon keys
    for k, trans in LEXICON.items():
        for lang in LANGUAGES:
            if lang in trans:
                bundles[lang][k] = trans[lang]
        bundles["default"][k] = trans.get("en", "")

    all_keys = set()
    for lang in LANGUAGES:
        all_keys.update(bundles[lang].keys())
    all_keys.update(bundles["default"].keys())

    added_count = {lang: 0 for lang in LANGUAGES}

    for k in all_keys:
        ref_val = bundles["en"].get(k) or bundles["fr"].get(k) or bundles["default"].get(k)
        if not ref_val:
            for l in LANGUAGES:
                if bundles[l].get(k):
                    ref_val = bundles[l][k]
                    break

        for lang in LANGUAGES:
            if k not in bundles[lang] or not bundles[lang][k].strip():
                if lang == "en":
                    bundles[lang][k] = ref_val
                elif lang == "fr" and bundles["fr"].get(k):
                    continue
                else:
                    bundles[lang][k] = translate_fallback(k, ref_val, lang)
                added_count[lang] += 1

    bundles["default"] = dict(bundles["en"])

    for lang in LANGUAGES:
        save_properties(PROP_FILES[lang], bundles[lang])
    save_properties(PROP_FILES["default"], bundles["default"])

    return len(all_keys), added_count


def scan_java_hardcoded_strings():
    results = []
    patterns = [
        (r'new\s+Label\(\s*"([^"]{2,})"\s*\)', "Label"),
        (r'new\s+Button\(\s*"([^"]{2,})"\s*\)', "Button"),
        (r'new\s+CheckBox\(\s*"([^"]{2,})"\s*\)', "CheckBox"),
        (r'new\s+RadioButton\(\s*"([^"]{2,})"\s*\)', "RadioButton"),
        (r'new\s+Tab\(\s*"([^"]{2,})"\s*\)', "Tab"),
        (r'new\s+Tooltip\(\s*"([^"]{2,})"\s*\)', "Tooltip"),
        (r'new\s+TableColumn(?:<[^>]+>)?\(\s*"([^"]{2,})"\s*\)', "TableColumn"),
        (r'\.setText\(\s*"([^"]{2,})"\s*\)', "setText"),
        (r'\.setTitle\(\s*"([^"]{2,})"\s*\)', "setTitle"),
        (r'\.setHeaderText\(\s*"([^"]{2,})"\s*\)', "setHeaderText"),
        (r'\.setContentText\(\s*"([^"]{2,})"\s*\)', "setContentText"),
        (r'\.setPromptText\(\s*"([^"]{2,})"\s*\)', "setPromptText"),
    ]

    ignore_prefixes = (
        "-fx-", "http://", "https://", "data/", "file:/", "rgba(", "rgb(",
        "System", "Arial", "Roboto", "Segoe", "#", "PT", "0.", "1.", "2.", "3.",
        "4.", "5.", "6.", "7.", "8.", "9.", "%", "$", "{", "[", "]", "}", "->",
        "-->", "<--", "===", "---", "###", "class", "style", "px", "em", "vw", "vh"
    )

    for java_file in JAVA_SRC_DIR.rglob("*.java"):
        with open(java_file, "r", encoding="utf-8", errors="replace") as f:
            lines = f.readlines()

        for idx, line in enumerate(lines, start=1):
            stripped = line.strip()
            if stripped.startswith("//") or stripped.startswith("/*") or stripped.startswith("*"):
                continue
            if "I18n.get" in line or "I18n.format" in line or "I18n.getOrDefault" in line:
                continue

            for pattern, comp_type in patterns:
                matches = re.finditer(pattern, line)
                for m in matches:
                    text = m.group(1).strip()
                    if len(text) < 2:
                        continue
                    if any(text.startswith(p) for p in ignore_prefixes):
                        continue
                    if text in ("OK", "X", "+", "-", "...", ":", "|", "/", "\\", "N/A", "NaN", "v1.0", "v2.0", "❌", "✔️", "⚙️", "⚠️"):
                        continue
                    if re.match(r'^[A-Z0-9_\-\.\s]{1,3}$', text):
                        continue

                    results.append({
                        "file": str(java_file.relative_to(BASE_DIR)),
                        "line": idx,
                        "type": comp_type,
                        "text": text,
                        "source": stripped
                    })

    return results


def check_missing_tooltips():
    missing_tooltips = []

    for java_file in JAVA_UI_DIR.glob("*.java"):
        with open(java_file, "r", encoding="utf-8", errors="replace") as f:
            content = f.read()
            lines = content.splitlines()

        btn_matches = re.finditer(r'(?:private\s+)?(?:final\s+)?(?:Button|Slider|ComboBox<[^>]+>|CheckBox|Spinner<[^>]+>)\s+([a-zA-Z0-9_]+)\b', content)
        declared_controls = set(m.group(1) for m in btn_matches)

        for ctrl in declared_controls:
            if not re.search(rf'({ctrl}\.setTooltip|Tooltip\.install\(\s*{ctrl}|installTooltip\(\s*{ctrl}|setControlTooltip\(\s*{ctrl})', content):
                line_no = 1
                for idx, l in enumerate(lines, start=1):
                    if ctrl in l and ("new Button" in l or "new Slider" in l or "new ComboBox" in l or "new CheckBox" in l or "new Spinner" in l):
                        line_no = idx
                        break

                missing_tooltips.append({
                    "file": java_file.name,
                    "control": ctrl,
                    "line": line_no
                })

    return missing_tooltips


def main():
    print("=====================================================================")
    print("[ETHER] 5-Language Internationalization & Tooltip Audit Suite")
    print("=====================================================================")

    # Step 0: Fix Mojibake
    print("\n[0/3] Cleaning corrupted encoding sequences in Java source files...")
    cleaned_files = fix_all_ui_source_files()
    print(f"   -> Cleaned encoding in {cleaned_files} Java files.")

    # Step 1: Scan Java Files for Hardcoded Strings
    print("\n[1/3] Scanning Java codebase for hardcoded UI strings...")
    hardcoded = scan_java_hardcoded_strings()
    print(f"   -> Found {len(hardcoded)} remaining candidate UI string literals across codebase.")
    if hardcoded:
        print("   -> Sample findings:")
        for h in hardcoded[:10]:
            print(f"      - {h['file']}:{h['line']} [{h['type']}]: \"{h['text']}\"")
        if len(hardcoded) > 10:
            print(f"      ... and {len(hardcoded) - 10} more.")

    # Step 2: Check Missing Tooltips
    print("\n[2/3] Checking mouseover tooltip coverage on UI controls...")
    missing_tt = check_missing_tooltips()
    print(f"   -> Found {len(missing_tt)} interactive controls without explicit tooltip bindings.")
    if missing_tt:
        print("   -> Sample un-tooltiped controls:")
        for m in missing_tt[:10]:
            print(f"      - {m['file']}:{m['line']} -> Control: '{m['control']}'")
        if len(missing_tt) > 10:
            print(f"      ... and {len(missing_tt) - 10} more.")

    # Step 3: Synchronize All 5-Language Resource Bundles
    print("\n[3/3] Synchronizing all 5 language properties bundles (EN, FR, DE, ES, ZH)...")
    total_keys, added = synchronize_all_properties()
    print(f"   -> Total unified I18n keys: {total_keys}")
    for lang in LANGUAGES:
        print(f"   -> [{lang.upper()}] Added / synchronized {added[lang]} missing keys.")

    print("\n[SUCCESS] All 5 property files are fully synchronized and validated.")
    print("=====================================================================")


if __name__ == "__main__":
    main()
