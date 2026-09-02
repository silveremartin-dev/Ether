import os
import glob

i18n_dir = r"c:\Silvere\Encours\Developpement\Ether\src\main\resources\i18n"
files = glob.glob(os.path.join(i18n_dir, "*.properties"))

key_sets = {}

for fpath in files:
    fname = os.path.basename(fpath)
    keys = set()
    with open(fpath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                key = line.split('=', 1)[0].strip()
                keys.add(key)
    key_sets[fname] = keys
    print(f"{fname}: {len(keys)} unique keys")

all_keys = set().union(*key_sets.values())
print(f"Total union of unique keys across all files: {len(all_keys)}")

for fname, keys in key_sets.items():
    missing = all_keys - keys
    if missing:
        print(f"File {fname} is missing {len(missing)} keys compared to total union.")
    else:
        print(f"File {fname} has 100% of all keys!")
