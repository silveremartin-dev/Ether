import os
import re

PROPERTIES_DIR = os.path.join("src", "main", "resources", "i18n")

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

en_props = load_properties(os.path.join(PROPERTIES_DIR, "messages_en.properties"))
fr_props = load_properties(os.path.join(PROPERTIES_DIR, "messages_fr.properties"))
zh_props = load_properties(os.path.join(PROPERTIES_DIR, "messages_zh.properties"))

print(f"EN count: {len(en_props)}")
print(f"FR count: {len(fr_props)}")
print(f"ZH count: {len(zh_props)}")

# Find keys present in EN but missing in FR/ZH
missing_in_fr = set(en_props.keys()) - set(fr_props.keys())
missing_in_zh = set(en_props.keys()) - set(zh_props.keys())

print(f"Missing in FR: {len(missing_in_fr)}")
print(f"Missing in ZH: {len(missing_in_zh)}")

# Sample missing keys
print("\nSample missing keys in ZH:")
for k in sorted(list(missing_in_zh))[:20]:
    print(f"  {k} = {en_props[k]}")
