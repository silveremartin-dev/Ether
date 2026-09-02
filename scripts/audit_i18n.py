import glob
import os
import re
import sys

sys.stdout.reconfigure(encoding='utf-8')

i18n_dir = os.path.join('src', 'main', 'resources', 'i18n')
prop_files = {
    'en': os.path.join(i18n_dir, 'messages_en.properties'),
    'fr': os.path.join(i18n_dir, 'messages_fr.properties'),
    'es': os.path.join(i18n_dir, 'messages_es.properties'),
    'de': os.path.join(i18n_dir, 'messages_de.properties'),
    'zh': os.path.join(i18n_dir, 'messages_zh.properties'),
}

def load_properties(filepath):
    keys = {}
    if not os.path.exists(filepath):
        return keys
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                k, v = line.split('=', 1)
                keys[k.strip()] = v.strip()
    return keys

data = {lang: load_properties(path) for lang, path in prop_files.items()}

all_prop_keys = set()
for lang, keys in data.items():
    all_prop_keys.update(keys.keys())

print("=== Property File Audit ===")
for lang, keys in data.items():
    print(f"Lang '{lang}': {len(keys)} keys")

print(f"Total unique keys across all properties files: {len(all_prop_keys)}")

missing = {lang: [] for lang in data}
for key in sorted(all_prop_keys):
    for lang in data:
        if key not in data[lang] or not data[lang][key]:
            missing[lang].append(key)

for lang, mkeys in missing.items():
    print(f"Lang '{lang}' missing {len(mkeys)} keys vs all properties files")

# Scan Java UI files for I18n calls and getOrDefault calls
java_files = glob.glob('src/main/java/**/*.java', recursive=True)

# Find all I18n.get("key") and I18n.getOrDefault("key", "default", ...)
i18n_get_pat = re.compile(r'I18n\.get\(\s*"([^"]+)"')
i18n_getordefault_pat = re.compile(r'I18n\.getOrDefault\(\s*"([^"]+)"\s*,\s*"((?:[^"\\]|\\.)*)"')

code_keys_with_defaults = {} # key -> default_val

for fpath in java_files:
    with open(fpath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    for m in i18n_get_pat.finditer(content):
        key = m.group(1)
        if key not in code_keys_with_defaults:
            code_keys_with_defaults[key] = None
    for m in i18n_getordefault_pat.finditer(content):
        key = m.group(1)
        default_val = m.group(2).replace(r'\"', '"')
        code_keys_with_defaults[key] = default_val

print(f"\nTotal unique I18n keys referenced in code: {len(code_keys_with_defaults)}")

missing_in_en_props = [k for k in code_keys_with_defaults if k not in data['en']]
print(f"Keys in code BUT missing in messages_en.properties: {len(missing_in_en_props)}")

for k in sorted(missing_in_en_props)[:30]:
    print(f"  Missing key: '{k}' (Default in code: '{code_keys_with_defaults[k]}')")
