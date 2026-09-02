import os
import re
import sys
import glob

sys.stdout.reconfigure(encoding='utf-8')

PROPERTIES_DIR = os.path.join("src", "main", "resources", "i18n")
JAVA_DIR = os.path.join("src", "main", "java")

LOCALES = ["en", "fr", "es", "de", "zh"]

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

# Load existing properties
existing = {loc: load_properties(os.path.join(PROPERTIES_DIR, f"messages_{loc}.properties")) for loc in LOCALES}

# Extract keys from Java files
code_keys_defaults = {}

get_or_default_regex = re.compile(r'I18n\.getOrDefault\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"')
get_regex = re.compile(r'I18n\.get\s*\(\s*"([^"]+)"\s*\)')

for root, _, files in os.walk(JAVA_DIR):
    for f in files:
        if f.endswith(".java"):
            path = os.path.join(root, f)
            with open(path, "r", encoding="utf-8", errors="ignore") as jf:
                content = jf.read()
                for match in get_or_default_regex.finditer(content):
                    key, default_val = match.groups()
                    # Clean up escaped quotes or format strings if needed
                    code_keys_defaults[key] = default_val
                for match in get_regex.finditer(content):
                    key = match.group(1)
                    if key not in code_keys_defaults:
                        code_keys_defaults[key] = key

print(f"Total keys found in Java code: {len(code_keys_defaults)}")
print(f"Total keys in messages_en.properties: {len(existing['en'])}")

# Master set of all keys
all_keys = set(existing['en'].keys()).union(set(code_keys_defaults.keys()))
for loc in LOCALES:
    all_keys.update(existing[loc].keys())

print(f"Total unique keys across code and all property files: {len(all_keys)}")
