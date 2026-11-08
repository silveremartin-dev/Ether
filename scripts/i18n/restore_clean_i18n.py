import subprocess
import re
import sys
from pathlib import Path

# Force UTF-8 on Windows
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_DIR = Path(__file__).resolve().parent.parent
I18N_DIR = BASE_DIR / "src" / "main" / "resources" / "i18n"
JAVA_SRC_DIR = BASE_DIR / "src" / "main" / "java"

LANGUAGES = ["en", "fr", "de", "es", "zh"]
PROP_FILES = {
    "en": I18N_DIR / "messages_en.properties",
    "fr": I18N_DIR / "messages_fr.properties",
    "de": I18N_DIR / "messages_de.properties",
    "es": I18N_DIR / "messages_es.properties",
    "zh": I18N_DIR / "messages_zh.properties",
    "default": I18N_DIR / "messages.properties",
}

# 1. Load clean base properties from commit 8a17e294
def get_clean_git_props(lang: str) -> dict[str, str]:
    filename = f"src/main/resources/i18n/messages_{lang}.properties" if lang != "default" else "src/main/resources/i18n/messages.properties"
    try:
        proc = subprocess.run(["git", "show", f"8a17e294:{filename}"], capture_output=True, check=True)
        text = proc.stdout.decode("utf-8", errors="replace")
        props = {}
        for line in text.splitlines():
            line = line.strip()
            if line and not line.startswith("#") and not line.startswith("!") and "=" in line:
                k, v = line.split("=", 1)
                props[k.strip()] = v.strip()
        return props
    except Exception as e:
        print(f"Error reading 8a17e294 for {lang}: {e}")
        return {}

# 2. Additional clean translations for keys added recently
NEW_TRANSLATIONS = {
    "analytics.audit.col_align_status": {
        "en": "Alignment Status",
        "fr": "Statut d'Alignement",
        "de": "Ausrichtungsstatus",
        "es": "Estado de Alineación",
        "zh": "对齐状态"
    },
    "analytics.audit.col_clio_var": {
        "en": "Cliodynamic Variable",
        "fr": "Variable Cliodynamique",
        "de": "Kliodynamische Variable",
        "es": "Variable Cliodinámica",
        "zh": "历史动力学变量"
    },
    "analytics.audit.col_mape": {
        "en": "Mean Absolute Error (MAPE)",
        "fr": "Erreur Moyenne (MAPE)",
        "de": "Mittlerer Absoluter Fehler (MAPE)",
        "es": "Error Absoluto Medio (MAPE)",
        "zh": "平均绝对误差 (MAPE)"
    },
    "analytics.audit.col_suspect_engine": {
        "en": "Suspect M3 Engine Module",
        "fr": "Module Moteur M3 Suspect",
        "de": "Verdächtiges M3-Modul",
        "es": "Módulo de Motor M3 Sospechoso",
        "zh": "疑似 M3 引擎模块"
    },
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
        "es": "Cerrar cuadro de diálogo de vista détaillée.",
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
    },
    "analytics.all_executed_title": {
        "en": "Scenarios Already Simulated",
        "fr": "Scénarios Déjà Simulés",
        "de": "Szenarien Bereits Simuliert",
        "es": "Escenarios Ya Simulados",
        "zh": "情景已全部完成模拟"
    },
    "analytics.all_executed_header": {
        "en": "All scenarios have already been executed",
        "fr": "Tous les scénarios ont déjà été exécutés",
        "de": "Alle Szenarien wurden bereits ausgeführt",
        "es": "Todos los escenarios ya han sido ejecutados",
        "zh": "所有情景轨迹已完成模拟"
    },
    "analytics.all_executed_content": {
        "en": "All simulation trajectories are complete and indexed in disk cache. Check specific scenarios in the table to force re-execution.",
        "fr": "Toutes les trajectoires de simulation sont complètes et indexées en cache. Cochez des scénarios spécifiques dans le tableau si vous souhaitez forcer leur réexécution.",
        "de": "Alle Simulationspfade sind vollständig und auf der Festplatte zwischengespeichert. Markieren Sie bestimmte Szenarien in der Tabelle, um eine erneute Ausführung zu erzwingen.",
        "es": "Todas las trayectorias de simulación están completas e indexadas en la caché. Marque escenarios específicos en la tabla para forzar su reejecución.",
        "zh": "所有模拟轨迹均已完整计算并索引至磁盘缓存中。如需强制重新计算，请在表格中勾选特定情景。"
    }
}

# 3. Clean binary mojibake from all Java source files
MOJIBAKE_MAP = {
    "âœ–": "✖",
    "âœ”": "✔",
    "âš™ï¸ ": "⚙️",
    "âš ": "⚠️",
    "âš¡": "⚡",
    "âœ ï¸ ": "✏️",
    "â³": "⏳",
    "â¸ï¸ ": "⏸️",
    "â–¶ï¸ ": "▶️",
    "â¹ï¸ ": "⏹️",
    "âœ…": "✅",
    "âŒ": "❌",
    "Ã©": "é",
    "Ã¨": "è",
    "Ã ": "à",
    "Ã§": "ç",
    "Ã´": "ô",
    "Ãª": "ê",
    "Ã®": "î",
    "Ã¯": "ï",
    "Ã¹": "ù",
    "Ã»": "û",
    "Ã¢": "â",
    "Ã‹": "Ë",
    "Ã‰": "É",
    "Ãˆ": "È",
    "Ã€": "À",
    "Ã‡": "Ç",
    "Ã”": "Ô",
    "ÃŠ": "Ê",
    "ÃŽ": "Î",
    "Â°": "°",
    "Â ": " ",
    "Â«": "«",
    "Â»": "»",
}

def clean_java_files():
    count = 0
    for java_file in JAVA_SRC_DIR.rglob("*.java"):
        with open(java_file, "r", encoding="utf-8", errors="replace") as f:
            content = f.read()

        new_content = content
        for bad, good in MOJIBAKE_MAP.items():
            new_content = new_content.replace(bad, good)

        if new_content != content:
            with open(java_file, "w", encoding="utf-8", newline="\n") as f:
                f.write(new_content)
            count += 1
    print(f"Cleaned mojibake in {count} Java source files.")

def save_clean_properties(path: Path, props: dict[str, str], lang: str):
    sorted_keys = sorted(props.keys())
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("# Ether Society Simulation - I18n Resource Bundle (UTF-8)\n")
        f.write(f"# Language: {lang}\n")
        f.write(f"# Total Keys: {len(sorted_keys)}\n\n")

        current_prefix = ""
        for k in sorted_keys:
            # Skip corrupted non-identifier keys
            if " " in k or "%" in k or "\ufffd" in k or "\x00" in k:
                continue
            prefix = k.split(".")[0] if "." in k else "general"
            if prefix != current_prefix:
                current_prefix = prefix
                f.write(f"\n# --- {current_prefix.upper()} ---\n")
            val = props[k]
            f.write(f"{k}={val}\n")

def main():
    print("Step 1: Cleaning Java files...")
    clean_java_files()

    print("Step 2: Restoring properties from 8a17e294...")
    all_props = {}
    for lang in LANGUAGES:
        all_props[lang] = get_clean_git_props(lang)
    all_props["default"] = dict(all_props["en"])

    print("Step 3: Injecting clean new keys...")
    for key, trans in NEW_TRANSLATIONS.items():
        for lang in LANGUAGES:
            if lang in trans:
                all_props[lang][key] = trans[lang]
        all_props["default"][key] = trans.get("en", "")

    print("Step 4: Writing UTF-8 properties files...")
    for lang in LANGUAGES:
        save_clean_properties(PROP_FILES[lang], all_props[lang], lang)
        print(f"  - Wrote {len(all_props[lang])} keys to {PROP_FILES[lang].name}")
    save_clean_properties(PROP_FILES["default"], all_props["default"], "default")
    print(f"  - Wrote {len(all_props['default'])} keys to messages.properties")

    print("\n[SUCCESS] All properties and Java files restored with 100% pristine UTF-8 encoding.")

if __name__ == "__main__":
    main()
