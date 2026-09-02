import os
import re
import glob

PROPERTIES_DIR = os.path.join("src", "main", "resources", "i18n")
LOCALES = ["en", "fr", "es", "de", "zh"]

# Basic dictionary of known French/English to target language translations for system keys
TRANSLATIONS = {
    # Common words
    "Abysses": {"en": "Abysses", "fr": "Abysses", "es": "Abismos", "de": "Abgründe", "zh": "深渊"},
    "Ocean": {"en": "Ocean", "fr": "Océan", "es": "Océano", "de": "Ozean", "zh": "海洋"},
    "Ocean / Abyss": {"en": "Ocean / Abyss", "fr": "Océan / Abysses", "es": "Océano / Abismo", "de": "Ozean / Abgrund", "zh": "海洋 / 深渊"},
    "Plaines": {"en": "Plains", "fr": "Plaines", "es": "Llanuras", "de": "Ebenen", "zh": "平原"},
    "Forest": {"en": "Forest", "fr": "Forêt", "es": "Bosque", "de": "Wald", "zh": "森林"},
    "Jungle": {"en": "Jungle", "fr": "Jungle", "es": "Selva", "de": "Dschungel", "zh": "丛林"},
    "Desert": {"en": "Desert", "fr": "Désert", "es": "Desierto", "de": "Wüste", "zh": "沙漠"},
    "Montagnes": {"en": "Mountains", "fr": "Montagnes", "es": "Montañas", "de": "Berge", "zh": "山脉"},
    "Fleuves Majeurs": {"en": "Major Rivers", "fr": "Fleuves Majeurs", "es": "Ríos Principales", "de": "Hauptflüsse", "zh": "主要河流"},
    "Streams & Rivers": {"en": "Streams & Rivers", "fr": "Cours d'eau & Rivières", "es": "Arroyos y Ríos", "de": "Bäche & Flüsse", "zh": "溪流与河流"},
    "Affluents & Ruisseaux": {"en": "Tributaries & Streams", "fr": "Affluents & Ruisseaux", "es": "Afluentes y Arroyos", "de": "Nebenflüsse & Bäche", "zh": "支流与小溪"},
    "Relief Continental": {"en": "Continental Relief", "fr": "Relief Continental", "es": "Relieve Continental", "de": "Kontinentales Relief", "zh": "大陆地形"},
    "Inerte": {"en": "Inert", "fr": "Inerte", "es": "Inerte", "de": "Inert", "zh": "惰性"},
    "Moderate Tectonics": {"en": "Moderate Tectonics", "fr": "Tectonique Modérée", "es": "Tectónica Moderada", "de": "Moderate Tektonik", "zh": "中等地质构造"},
    "High Magmatism": {"en": "High Magmatism", "fr": "Magmatisme Élevé", "es": "Magmatismo Alto", "de": "Hoher Magmatismus", "zh": "高岩浆活动"},
}

def load_properties(filepath):
    props = {}
    if not os.path.exists(filepath):
        return props
    with open(filepath, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or line.startswith("!"):
                continue
            if "=" in line:
                key, val = line.split("=", 1)
                props[key.strip()] = val.strip()
    return props

def save_properties(filepath, props):
    with open(filepath, "w", encoding="utf-8") as f:
        f.write("# Ether Society Simulation Infrastructure Localized Properties\n")
        f.write("# Auto-generated & synchronized with zero-fallback integrity\n\n")
        for key in sorted(props.keys()):
            # Escape newlines if present
            val = props[key].replace("\n", "\\n")
            f.write(f"{key}={val}\n")

print("Syncing properties...")
