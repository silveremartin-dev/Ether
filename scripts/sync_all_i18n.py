#!/usr/bin/env python3
import os, re, glob, sys

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

# Load existing keys for each file
existing_keys = {lang: {} for lang in LANGS}
for lang in LANGS:
    p = os.path.join(I18N_DIR, f"messages_{lang}.properties")
    if os.path.exists(p):
        with open(p, encoding="utf-8", errors="replace") as f:
            for line in f:
                line_str = line.strip()
                if line_str and not line_str.startswith("#") and "=" in line_str:
                    k, _, v = line_str.partition("=")
                    existing_keys[lang][k.strip()] = v.strip()

missing = [k for k in key_fallbacks if k not in existing_keys["en"]]
print(f"Missing keys ({len(missing)}):")
for k in missing:
    safe_v = key_fallbacks[k].encode('ascii', 'backslashreplace').decode('ascii')
    print(f"  {k} = {safe_v}")
