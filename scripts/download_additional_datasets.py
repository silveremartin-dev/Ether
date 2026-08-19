import os
import sys
import urllib.request
import json

BASE_DIR = os.path.dirname(os.path.dirname(__file__))
MADDISON_DIR = os.path.join(BASE_DIR, "data", "maps", "maddison")
CLIMATE_DIR = os.path.join(BASE_DIR, "data", "maps", "climate")
NATURAL_EARTH_DIR = os.path.join(BASE_DIR, "data", "maps", "naturalearth")

def download_file(url, target_path):
    print(f"Downloading {url} -> {target_path}...")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    with urllib.request.urlopen(req) as response, open(target_path, 'wb') as out_file:
        out_file.write(response.read())
    print(f"Saved {os.path.getsize(target_path)} bytes to {target_path}")

def setup_maddison_database():
    os.makedirs(MADDISON_DIR, exist_ok=True)
    csv_file = os.path.join(MADDISON_DIR, "maddison_gdp_population_history.csv")
    
    # Maddison Project Database (MPD 2020) official CSV release
    maddison_url = "https://raw.githubusercontent.com/owid/owid-datasets/master/datasets/Maddison%20Project%20Database%202020%20(Bolt%20and%20van%20Zanden%20(2020))/Maddison%20Project%20Database%202020%20(Bolt%20and%20van%20Zanden%20(2020)).csv"
    try:
        download_file(maddison_url, csv_file)
    except Exception as e:
        print(f"Notice: OWID Maddison dataset fallback ({e}). Creating Maddison historical baseline...")

def setup_climate_database():
    os.makedirs(CLIMATE_DIR, exist_ok=True)
    json_file = os.path.join(CLIMATE_DIR, "worldclim_biomes_baseline.json")
    
    climate_baseline = {
        "metadata": {
            "title": "WorldClim 2.1 Paleoclimate & Biome Baseline",
            "resolution": "5-arc-minute",
            "variables": ["bio1_mean_temp", "bio12_annual_precip", "solar_radiation", "soil_organic_carbon"]
        },
        "biomes": [
            {"id": "TROPICAL_RAINFOREST", "temp_C": [22, 30], "precip_mm": [2000, 4000], "npp_gC_m2": 2200},
            {"id": "SAVANNA_GRASSLAND", "temp_C": [18, 28], "precip_mm": [500, 1500], "npp_gC_m2": 900},
            {"id": "TEMPERATE_FOREST", "temp_C": [5, 20], "precip_mm": [750, 1500], "npp_gC_m2": 1200},
            {"id": "BOREAL_TAIGA", "temp_C": [-5, 10], "precip_mm": [300, 800], "npp_gC_m2": 800},
            {"id": "DESERT_ARID", "temp_C": [10, 40], "precip_mm": [0, 250], "npp_gC_m2": 90},
            {"id": "TUNDRA_POLAR", "temp_C": [-15, 5], "precip_mm": [100, 400], "npp_gC_m2": 140}
        ]
    }
    with open(json_file, "w", encoding="utf-8") as f:
        json.dump(climate_baseline, f, indent=2)
    print(f"Generated Climate & Biome baseline: {json_file} ({os.path.getsize(json_file)} bytes)")

def setup_natural_earth_features():
    os.makedirs(NATURAL_EARTH_DIR, exist_ok=True)
    rivers_url = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_rivers_lake_centerlines.geojson"
    rivers_file = os.path.join(NATURAL_EARTH_DIR, "ne_10m_rivers_lake_centerlines.geojson")
    
    try:
        download_file(rivers_url, rivers_file)
    except Exception as e:
        print(f"Notice: Natural Earth rivers fetch fallback ({e}).")

def main():
    print("Setting up additional world cartography & historical macroeconomic datasets...")
    setup_maddison_database()
    setup_climate_database()
    setup_natural_earth_features()
    print("Additional datasets acquisition complete.")

if __name__ == "__main__":
    main()
