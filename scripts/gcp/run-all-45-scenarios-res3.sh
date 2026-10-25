#!/usr/bin/env bash
# ==============================================================================
# ETHER MASTER 45 PLANETARY & HISTORICAL SCENARIOS RUNNER (RES 3, N-1 CAMPAIGN)
# ==============================================================================
# Grid Resolution : Uber H3 Resolution 3 (41,162 hexagons)
# Snapshot Limit  : Max 24 snapshots per run
# Naming Pattern  : Appends " (Résolution 3)" to scenario names & run folders
#                   to prevent overwriting High-Res 4/5 snapshots.
# Adaptive dt     :
#   - Paleolithic (< -10,000 BP)          : 365.25 days (1.0 yr)
#   - Neolithic & Bronze (-10,000 to -1000): 60.0 days (bimestriel)
#   - Iron Age (-1,000 BP)                 : 60.0 days (bimestriel)
#   - Antiquity to Medieval (-500 to 1491) : 14.0 days (bimensuel)
#   - 1491+ & Modern & Future Earth        : 7.0 days (hebdomadaire strict)
#   - Extraterrestrial Planets & Moons     : 14.0 days
# ==============================================================================
set -euo pipefail

mkdir -p /opt/ether/logs/spatial_runs /opt/ether/logs/calibration /opt/ether/saves /opt/ether/data
cd /opt/ether

echo "================================================================================"
echo " 🚀 ETHER LIGHTWEIGHT N-1 CLOUD RUN (H3 RES 3, 41,162 CELLS, 24 SNAPSHOTS)"
echo "================================================================================"
echo "Timestamp: $(date -u)"
echo "Host Machine: $(uname -a)"
echo "Available Cores: $(nproc)"
echo "Spatial Level: N-1 (Uber H3 Resolution 3 vs Resolution 4/5)"
echo "Storage Footprint: Reduced by ~7x-49x, 10x Throughput Acceleration"
echo "================================================================================"

# ==============================================================================
# 45 PLANETARY & HISTORICAL SPATIAL SCENARIOS (RESOLUTION 3)
# ==============================================================================
echo "================================================================================"
echo " 🌐 STARTING MASTER 45 PLANETARY & HISTORICAL SUITE (RESOLUTION 3)"
echo "================================================================================"

# Format: SCENARIO_FILE:START:END:DT:RUN_ID
SCENARIOS=(
    # --- PALEOLITHIC (dt = 365.25 days) ---
    "earth_-100000_out_of_africa:-100000:-20000:365.25:RUN-GLOBAL-TERRE_SORTIE_AFRIQUE_-100000 (Résolution 3)"
    "earth_-74000_toba_cataclysm_74k:-74000:-50000:365.25:RUN-GLOBAL-TERRE_TOBA_CATACLYSME_-74000 (Résolution 3)"
    "earth_-50000_sahul:-50000:-10000:365.25:RUN-REGIONAL-TERRE_SAHUL_-50000 (Résolution 3)"
    "earth_-25000_beringia:-25000:-10000:365.25:RUN-REGIONAL-TERRE_BERINGIE_-25000 (Résolution 3)"
    "earth_-20000_lgm_solutrean:-20000:-12000:365.25:RUN-GLOBAL-TERRE_LGM_SOLUTREEN_-20000 (Résolution 3)"
    "earth_-10900_younger_dryas:-10900:-9500:365.25:RUN-GLOBAL-TERRE_RECENTS_DRYAS_-10900 (Résolution 3)"

    # --- NEOLITHIC & BRONZE AGE (dt = 60.0 days) ---
    "earth_-8000_fertile_crescent:-8000:-5000:60.0:RUN-REGIONAL-TERRE_CROISSANT_FERTILE_-8000 (Résolution 3)"
    "earth_-6000_green_sahara:-6000:-3500:60.0:RUN-REGIONAL-TERRE_SAHARA_VERT_-6000 (Résolution 3)"
    "earth_-3000_ancient_egypt:-3000:-1000:60.0:RUN-REGIONAL-TERRE_EGYPTE_ANTIQUE_-3000 (Résolution 3)"
    "earth_-1900_assyrian_empire:-1900:-600:60.0:RUN-REGIONAL-TERRE_EMPIRE_ASSYRIEN_-1900 (Résolution 3)"
    "earth_-1500_mesoamerica:-1500:900:60.0:RUN-REGIONAL-TERRE_MESOAMERIQUE_-1500 (Résolution 3)"
    "earth_-1279_ramesses_ii:-1279:-1200:60.0:RUN-REGIONAL-TERRE_RAMSES_II_-1279 (Résolution 3)"
    "earth_-1200_bronze_age_collapse_1200bc:-1200:-900:60.0:RUN-GLOBAL-TERRE_EFFONDREMENT_BRONZE_-1200 (Résolution 3)"

    # --- IRON AGE (dt = 60.0 days) ---
    "earth_-1000_early_iron_age:-1000:-300:60.0:RUN-GLOBAL-TERRE_AGE_FER_-1000 (Résolution 3)"

    # --- CLASSICAL ANTIQUITY & MEDIEVAL (dt = 14.0 days) ---
    "earth_-334_alexander_hellenistic_334bc:-334:-150:14.0:RUN-GLOBAL-TERRE_ALEXANDRE_HELLENISTIQUE_-334 (Résolution 3)"
    "earth_-300_maurya_empire:-300:100:14.0:RUN-REGIONAL-TERRE_EMPIRE_MAURYA_-300 (Résolution 3)"
    "earth_+0_roman_empire:0:476:14.0:RUN-REGIONAL-TERRE_EMPIRE_ROMAIN_+0 (Résolution 3)"
    "earth_+536_late_antique_ice_age:536:650:14.0:RUN-GLOBAL-TERRE_GLACIAIRE_ANTIQUE_TARDIVE_+536 (Résolution 3)"
    "earth_+632_islamic_expansion_632:632:900:14.0:RUN-GLOBAL-TERRE_EXPANSION_ISLAMIQUE_+632 (Résolution 3)"
    "earth_+1000_song_dynasty:1000:1279:14.0:RUN-GLOBAL-TERRE_DYNASTIE_SONG_+1000 (Résolution 3)"
    "earth_+1206_mongol_conquest_1206:1206:1368:14.0:RUN-GLOBAL-TERRE_CONQUETE_MONGOLE_+1206 (Résolution 3)"
    "earth_+1324_mali_empire:1324:1591:14.0:RUN-REGIONAL-TERRE_EMPIRE_MALI_+1324 (Résolution 3)"
    "earth_+1347_black_death_1347:1347:1492:14.0:RUN-GLOBAL-TERRE_PESTE_NOIRE_+1347 (Résolution 3)"

    # --- EARLY MODERN TO CONTEMPORARY (dt = 7.0 days) ---
    "earth_+1491_americas_1491:1491:1650:7.0:RUN-REGIONAL-TERRE_AMERIQUES_1491 (Résolution 3)"
    "earth_+1492_columbian_contact:1492:1650:7.0:RUN-GLOBAL-TERRE_CHOC_COLOMBIEN_+1492 (Résolution 3)"
    "earth_+1639_tokugawa_japan:1639:1853:7.0:RUN-REGIONAL-TERRE_JAPON_SAKOKU_+1639 (Résolution 3)"
    "earth_+1800_industrial_1800:1800:1900:7.0:RUN-GLOBAL-TERRE_REVOLUTION_INDUSTRIELLE_+1800 (Résolution 3)"
    "earth_+1914_world_wars_totalitarian_1914:1914:1960:7.0:RUN-GLOBAL-TERRE_GUERRES_MONDIALES_+1914 (Résolution 3)"
    "earth_+2000_anthropocene_2000:2000:2100:7.0:RUN-GLOBAL-TERRE_ANTHROPOCENE_+2000 (Résolution 3)"

    # --- FUTURE EARTH SCENARIOS (dt = 7.0 days) ---
    "earth_+2025_business_as_usual:2025:2055:7.0:RUN-GLOBAL-TERRE_TENDANCIELLE_BAU_+2025 (Résolution 3)"
    "earth_+2026_ssp5_85:2026:2100:7.0:RUN-GLOBAL-TERRE_SSP5_85_+2026 (Résolution 3)"
    "earth_+2035_nuclear_winter_2035:2035:2085:7.0:RUN-GLOBAL-TERRE_HIVER_NUCLEAIRE_+2035 (Résolution 3)"
    "earth_+2045_singularity_2045:2045:2100:7.0:RUN-GLOBAL-TERRE_SINGULARITE_ASI_+2045 (Résolution 3)"
    "earth_+2050_peak_phosphate_2050:2050:2150:7.0:RUN-GLOBAL-TERRE_CRISE_PHOSPHATE_+2050 (Résolution 3)"
    "earth_+2060_supervolcano_2060:2060:2110:7.0:RUN-GLOBAL-TERRE_SUPERVOLCAN_+2060 (Résolution 3)"

    # --- EXTRATERRESTRIAL & EXOPLANETARY SCENARIOS (dt = 14.0 days) ---
    "moon_+2035_shackleton_2035:2035:2085:14.0:RUN-PLANET-LUNE_SHACKLETON_+2035 (Résolution 3)"
    "mars_+2050_colony_2050:2050:2150:14.0:RUN-PLANET-MARS_COLONIE_+2050 (Résolution 3)"
    "venus_+2080_cloud_cities_2080:2080:2180:14.0:RUN-PLANET-VENUS_CITIES_+2080 (Résolution 3)"
    "mercury_+2120_caloris_forge_2120:2120:2220:14.0:RUN-PLANET-MERCURE_CALORIS_+2120 (Résolution 3)"
    "moon_+2150_cryo_methane_2150:2150:2250:14.0:RUN-PLANET-TITAN_CRYO_METHANE_+2150 (Résolution 3)"
    "super_earth_+2200_gaia_2200:2200:2300:14.0:RUN-PLANET-SUPER_EARTH_GAIA_+2200 (Résolution 3)"
    "eyeball_world_+2220_twilight_2220:2220:2320:14.0:RUN-PLANET-EYEBALL_WORLD_+2220 (Résolution 3)"
    "oceania_+2100_aquapolis_2100:2100:2200:14.0:RUN-PLANET-OCEANIA_AQUAPOLIS_+2100 (Résolution 3)"
    "boreas_+2120_subglacial_2120:2120:2220:14.0:RUN-PLANET-BOREAS_SUBGLACIAL_+2120 (Résolution 3)"
    "archipelago_+2055_seasteading_2055:2055:2150:14.0:RUN-PLANET-ARCHIPEL_SEASTEADING_+2055 (Résolution 3)"
)

echo "Total Scenarios to Execute: ${#SCENARIOS[@]}"
echo "Spatial Grid Resolution   : Uber H3 Resolution 3 (41,162 cells)"
echo "Snapshots Cap per Run     : 24 snapshots max"
echo "================================================================================"

COMPLETED_COUNT=0
SKIPPED_COUNT=0

for ITEM in "${SCENARIOS[@]}"; do
    IFS=":" read -r NAME START END DT RUN_ID <<< "$ITEM"
    
    # Check if run already exists in /opt/ether/saves
    if [ -d "/opt/ether/saves/Headless_${NAME}" ] || [ -d "/opt/ether/saves/${RUN_ID}" ]; then
        if [ "${FORCE_REGENERATE:-0}" != "1" ]; then
            echo "--------------------------------------------------------------------------------"
            echo ">>> [SKIP] Scenario '${NAME}' already completed on disk. Skipping."
            echo "--------------------------------------------------------------------------------"
            SKIPPED_COUNT=$(( SKIPPED_COUNT + 1 ))
            continue
        fi
    fi
    
    YEARS=$(( END - START ))
    # Calculate exact ticks: (YEARS * 365.25 / DT)
    TICKS=$(awk -v y="$YEARS" -v d="$DT" 'BEGIN { printf "%.0f", (y * 365.25 / d) }')
    if [ "$TICKS" -lt 52 ]; then
        TICKS=52
    fi
    
    echo "--------------------------------------------------------------------------------"
    echo ">>> Simulating Scenario: ${NAME}"
    echo "    Run Identifier: ${RUN_ID}"
    echo "    Epoch Horizon : Year ${START} -> Year ${END} (${YEARS} years)"
    echo "    Time Step     : dt = ${DT} days"
    echo "    Total Ticks   : ${TICKS} ticks"
    echo "    Spatial Grid  : Uber H3 Resolution 3 (41,162 cells)"
    echo "    Snapshots Cap : 24 snapshots max"
    echo "--------------------------------------------------------------------------------"
    
    java -Xms4g -Xmx24g \
         --add-modules=jdk.incubator.vector \
         -cp /opt/ether/society-simulation.jar \
         org.ether.society.cli.HeadlessRunner \
         --scenario "${NAME}" \
         --scenario-suffix " (Résolution 3)" \
         --start-year "${START}" \
         --end-year "${END}" \
         --dt "${DT}" \
         --ticks "${TICKS}" \
         --resolution 3 \
         --max-snapshots 24 \
         --save "${RUN_ID}" \
         --multi-core \
         --threads "$(nproc)" \
         --simd \
         > "/opt/ether/logs/spatial_runs/${RUN_ID}.log" 2>&1 || {
             echo "  [!] Error on ${RUN_ID}. See /opt/ether/logs/spatial_runs/${RUN_ID}.log"
         }
         
    echo "  ✔ Run Completed: ${RUN_ID} (${TICKS} ticks)"
    COMPLETED_COUNT=$(( COMPLETED_COUNT + 1 ))
done

echo "================================================================================"
echo " 🏁 ALL 45 SCENARIOS FINISHED IN RES 3! (${COMPLETED_COUNT} executed, ${SKIPPED_COUNT} skipped)"
echo "================================================================================"
echo "Packaging complete campaign archive..."
mkdir -p /opt/ether/archives
tar -czf /opt/ether/archives/master_campaign_res3_archive.tar.gz \
    /opt/ether/saves/Headless_* \
    /opt/ether/logs/ 2>/dev/null || true

echo "Archive created: /opt/ether/archives/master_campaign_res3_archive.tar.gz"
echo "Completed at: $(date -u)"

# Auto-shutdown guard if requested
if [ "${AUTO_SHUTDOWN:-1}" = "1" ]; then
    echo "Triggering Auto-Shutdown Guard..."
    sudo shutdown -h now
fi
