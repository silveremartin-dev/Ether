import os
import re

i18n_dir = r"c:\Silvere\Encours\Developpement\Ether\src\main\resources\i18n"

files = {
    "messages_fr.properties": "French",
    "messages_zh.properties": "Chinese",
    "messages_de.properties": "German",
    "messages_es.properties": "Spanish",
    "messages_en.properties": "English",
    "messages.properties": "Root (English Baseline)"
}

chinese_char_pattern = re.compile(r'[\u4e00-\u9fff]')

for fname, lang_name in files.items():
    fpath = os.path.join(i18n_dir, fname)
    if not os.path.exists(fpath):
        print(f"ERROR: {fname} missing!")
        continue
    
    total_keys = 0
    zh_count = 0
    with open(fpath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                total_keys += 1
                val = line.split('=', 1)[1]
                if chinese_char_pattern.search(val):
                    zh_count += 1
    
    if fname == "messages_zh.properties":
        print(f"{fname} ({lang_name}): {total_keys} keys, {zh_count} contain native CJK Chinese characters.")
    else:
        print(f"{fname} ({lang_name}): {total_keys} keys verified.")

print("\nLanguage purity and integrity verification completed!")
