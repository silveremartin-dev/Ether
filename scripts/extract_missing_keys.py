#!/usr/bin/env python3
"""Extract all missing I18n keys grouped by prefix and output the fallback defaults."""
import re, glob, os, sys
from collections import defaultdict

SRC_DIR  = r"src\main\java"
I18N_DIR = r"src\main\resources\i18n"
LANGS    = ["en", "fr", "de", "es", "zh"]

# Pattern captures both the key AND the optional fallback default
KEY_PATTERN = re.compile(
    r'I18n\.getOrDefault\(\s*"([^"]+)"\s*,\s*"([^"]+)"'
)
KEY_ONLY    = re.compile(r'I18n\.get\(\s*"([^"]+)"')

used_keys   = {}  # key -> set of basenames
defaults    = {}  # key -> fallback string (from getOrDefault)

for path in glob.glob(os.path.join(SRC_DIR, "**", "*.java"), recursive=True):
    bn = os.path.basename(path)
    with open(path, encoding="utf-8", errors="replace") as f:
        content = f.read()
    for key, fallback in KEY_PATTERN.findall(content):
        used_keys.setdefault(key, set()).add(bn)
        defaults[key] = fallback
    for key in KEY_ONLY.findall(content):
        used_keys.setdefault(key, set()).add(bn)

# Load bundles
bundles = {}
for lang in LANGS:
    path = os.path.join(I18N_DIR, f"messages_{lang}.properties")
    bundle = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.rstrip("\r\n")
            if line.startswith("#") or "=" not in line:
                continue
            k, _, v = line.partition("=")
            bundle[k.strip()] = v.strip()
    bundles[lang] = bundle

en = bundles["en"]
missing = sorted(k for k in used_keys if k not in en)
print(f"Missing from ALL languages: {len(missing)}")
print(f"Total I18n keys in Java: {len(used_keys)}")
print(f"Total keys in messages_en.properties: {len(en)}")
print()

groups = defaultdict(list)
for k in missing:
    prefix = k.split(".")[0]
    groups[prefix].append(k)

for prefix in sorted(groups.keys()):
    keys = sorted(groups[prefix])
    print(f"# [{prefix.upper()}] — {len(keys)} keys")
    for k in keys:
        fb = defaults.get(k, "")
        src = sorted(used_keys.get(k, []))[0]
        print(f"{k} = {fb}")
    print()
