import os
import sys
import json
import base64
import glob

BASE_DIR = os.path.dirname(os.path.dirname(__file__))
CACHE_DIR = os.path.join(BASE_DIR, "data", "cache")
MAPS_DIR = os.path.join(BASE_DIR, "data", "maps")
SCENARIOS_JSON = os.path.join(BASE_DIR, "src", "main", "resources", "scenarios", "ScenarioRegistry.json")
BACKUP_DIR = os.path.join(BASE_DIR, "data", "vault_backups")

def create_full_vault_export():
    os.makedirs(BACKUP_DIR, exist_ok=True)
    print("Packing full Ether Vault (Scenarios, Maps, Benchmarks, Snapshots)...")
    
    vault_manifest = {
        "metadata": {
            "title": "Ether Complete Simulation Vault",
            "version": "2.0.0",
            "epochs_count": 14
        },
        "scenarios": [],
        "maps": [],
        "benchmarks": []
    }
    
    # 1. Export Scenario Metadata
    if os.path.exists(SCENARIOS_JSON):
        with open(SCENARIOS_JSON, "r", encoding="utf-8") as f:
            vault_manifest["scenarios"] = json.load(f)
            print(f"Loaded scenario definitions from {SCENARIOS_JSON}")
            
    # 2. Export All Cached Maps to Base64
    map_files = glob.glob(os.path.join(CACHE_DIR, "*.png"))
    for map_path in map_files:
        filename = os.path.basename(map_path)
        with open(map_path, "rb") as img_f:
            b64_data = base64.b64encode(img_f.read()).decode("utf-8")
            vault_manifest["maps"].append({
                "filename": filename,
                "size_bytes": os.path.getsize(map_path),
                "image_base64": b64_data
            })
    print(f"Encoded {len(vault_manifest['maps'])} map rasters into Vault package.")
    
    # 3. Save Vault Dump JSON & SQL statements
    dump_json_path = os.path.join(BACKUP_DIR, "ether_full_vault_dump.json")
    with open(dump_json_path, "w", encoding="utf-8") as out:
        json.dump(vault_manifest, out)
    print(f"Saved complete Vault JSON dump: {dump_json_path} ({os.path.getsize(dump_json_path)} bytes)")
    
    # 4. Generate SQL dump for direct PostgreSQL import
    sql_dump_path = os.path.join(BACKUP_DIR, "ether_postgres_import.sql")
    with open(sql_dump_path, "w", encoding="utf-8") as sql_out:
        sql_out.write("-- ETHER SIMULATION ENGINE - POSTGRESQL VAULT IMPORT\n")
        sql_out.write("CREATE TABLE IF NOT EXISTS ether_scenarios (id SERIAL PRIMARY KEY, scenario_key VARCHAR(100) UNIQUE, name VARCHAR(255), year_tag BIGINT, data JSONB);\n")
        sql_out.write("CREATE TABLE IF NOT EXISTS ether_scenario_maps (id SERIAL PRIMARY KEY, filename VARCHAR(255) UNIQUE, size_bytes BIGINT, image_base64 TEXT);\n\n")
        
        for m in vault_manifest["maps"]:
            sql_out.write(f"INSERT INTO ether_scenario_maps (filename, size_bytes, image_base64) VALUES ('{m['filename']}', {m['size_bytes']}, '{m['image_base64']}') ON CONFLICT (filename) DO UPDATE SET image_base64 = EXCLUDED.image_base64;\n")
            
    print(f"Generated PostgreSQL SQL import script: {sql_dump_path} ({os.path.getsize(sql_dump_path)} bytes)")

def import_full_vault_from_dump(dump_json_path):
    print(f"Restoring Ether Vault from dump {dump_json_path}...")
    if not os.path.exists(dump_json_path):
        print(f"Error: Dump file {dump_json_path} not found.")
        return
        
    with open(dump_json_path, "r", encoding="utf-8") as f:
        manifest = json.load(f)
        
    os.makedirs(CACHE_DIR, exist_ok=True)
    restored_count = 0
    for map_item in manifest.get("maps", []):
        target_file = os.path.join(CACHE_DIR, map_item["filename"])
        img_bytes = base64.b64decode(map_item["image_base64"])
        with open(target_file, "wb") as out_img:
            out_img.write(img_bytes)
        restored_count += 1
        
    print(f"Successfully restored {restored_count} map rasters into {CACHE_DIR}")

def main():
    if len(sys.argv) > 1 and sys.argv[1] == "download":
        dump_path = sys.argv[2] if len(sys.argv) > 2 else os.path.join(BACKUP_DIR, "ether_full_vault_dump.json")
        import_full_vault_from_dump(dump_path)
    else:
        create_full_vault_export()

if __name__ == "__main__":
    main()
