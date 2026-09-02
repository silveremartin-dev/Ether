#!/usr/bin/env python3
"""
Full I18n audit for Ether simulation engine.
Extracts all I18n.get / I18n.getOrDefault keys from Java sources,
then reports which keys are missing in each language bundle (en, fr, de, es, zh).
"""
import os, re, glob, sys

SRC_DIR  = r"src\main\java"
I18N_DIR = r"src\main\resources\i18n"
LANGS    = ["en", "fr", "de", "es", "zh"]

# ── 1. Extract all I18n keys from Java source ───────────────────────────────
KEY_PATTERN = re.compile(r'I18n\.(?:get|getOrDefault)\(\s*"([^"]+)"')
used_keys = {}  # key -> set of files

for path in glob.glob(os.path.join(SRC_DIR, "**", "*.java"), recursive=True):
    with open(path, encoding="utf-8", errors="replace") as f:
        content = f.read()
    for key in KEY_PATTERN.findall(content):
        used_keys.setdefault(key, set()).add(os.path.relpath(path, SRC_DIR))

print(f"[INFO] {len(used_keys)} unique I18n keys found across Java sources.\n")

# ── 2. Load each bundle ──────────────────────────────────────────────────────
bundles = {}
for lang in LANGS:
    path = os.path.join(I18N_DIR, f"messages_{lang}.properties")
    if not os.path.exists(path):
        print(f"[WARN] Bundle not found: {path}")
        bundles[lang] = {}
        continue
    bundle = {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.rstrip("\r\n")
            if line.startswith("#") or "=" not in line:
                continue
            k, _, v = line.partition("=")
            bundle[k.strip()] = v.strip()
    bundles[lang] = bundle
    print(f"[INFO] {lang}: {len(bundle)} keys loaded.")

# ── 3. Report missing keys per language ─────────────────────────────────────
print("\n" + "="*80)
missing_report = {}   # lang -> list of (key, representative_file)
for lang in LANGS:
    bundle = bundles[lang]
    missing = [(k, sorted(files)[0]) for k, files in sorted(used_keys.items()) if k not in bundle]
    missing_report[lang] = missing
    print(f"\n[{lang.upper()}] {len(missing)} MISSING keys:")
    for k, f in missing:
        print(f"  MISSING  {k}  (used in: {f})")

# ── 4. Summary table ─────────────────────────────────────────────────────────
print("\n" + "="*80)
print("SUMMARY")
print(f"{'Key':<60} " + "  ".join(f"{l.upper():>5}" for l in LANGS))
print("-"*80)
all_missing = sorted(set(k for missing in missing_report.values() for k, _ in missing))
for key in all_missing:
    row = f"{key:<60} "
    row += "  ".join(f"{'  ✗  ' if key in [k for k,_ in missing_report[l]] else '  ✓  ':>5}" for l in LANGS)
    print(row)

print(f"\nTotal unique missing keys: {len(all_missing)}")

# ── 5. Detect keys ONLY in bundle (orphaned / unused) ───────────────────────
print("\n" + "="*80)
print("ORPHANED keys (in bundles but never used in Java code):")
en_bundle = bundles.get("en", {})
orphaned = [k for k in en_bundle if k not in used_keys]
print(f"  {len(orphaned)} orphaned keys in messages_en.properties (not referenced by Java)")
