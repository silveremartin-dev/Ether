import os

i18n_dir = r"c:\Silvere\Encours\Developpement\Ether\src\main\resources\i18n"
en_file = os.path.join(i18n_dir, "messages_en.properties")

target_files = [
    "messages.properties",
    "messages_en.properties",
    "messages_fr.properties",
    "messages_de.properties",
    "messages_es.properties",
    "messages_zh.properties"
]

def load_properties(filepath):
    kv = {}
    ordered_keys = []
    if not os.path.exists(filepath):
        return kv, ordered_keys
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            stripped = line.strip()
            if stripped and not stripped.startswith('#') and '=' in line:
                k, v = line.split('=', 1)
                k = k.strip()
                v = v.strip()
                if k not in kv:
                    ordered_keys.append(k)
                kv[k] = v
    return kv, ordered_keys

# Collect all keys and master values from all files (master fallback = en)
all_kv = {}
all_ordered_keys = []

# Load EN first
en_kv, en_keys = load_properties(en_file)
for k in en_keys:
    if k not in all_kv:
        all_ordered_keys.append(k)
    all_kv[k] = en_kv[k]

# Load all other files for unique keys
for fname in target_files:
    fpath = os.path.join(i18n_dir, fname)
    f_kv, f_keys = load_properties(fpath)
    for k in f_keys:
        if k not in all_kv:
            all_ordered_keys.append(k)
            all_kv[k] = f_kv[k]

print(f"Total unique keys collected: {len(all_ordered_keys)}")

# Synchronize each target file
for fname in target_files:
    fpath = os.path.join(i18n_dir, fname)
    f_kv, _ = load_properties(fpath)
    
    missing_count = 0
    with open(fpath, 'w', encoding='utf-8') as f:
        f.write(f"# Ether Human Society Simulation - ResourceBundle ({fname})\n")
        f.write("# UTF-8 Encoded - Synchronized All Keys\n\n")
        
        for k in all_ordered_keys:
            if k in f_kv:
                val = f_kv[k]
            else:
                val = all_kv[k]
                missing_count += 1
            f.write(f"{k}={val}\n")

    print(f"File {fname}: written {len(all_ordered_keys)} keys (added {missing_count} missing entries).")

print("All 6 property files synchronized successfully with identical key sets!")
