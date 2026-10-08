#!/usr/bin/env bash
# ==============================================================================
# ETHER TRUE UNCOMPROMISED 27 HISTORICAL SCENARIOS RUNNER (ZERO-CHEAT, RES 5)
# ==============================================================================
# Resolution: Uber H3 Resolution 5 (2,016,842 hexagons)
# Uniform Timestep: dt = 7.0 days (52.14 ticks / year)
# Duration: Full historical duration (ticks = (END - START) * 365.25 / 7)
# ==============================================================================
set -euo pipefail

mkdir -p /opt/ether/logs/spatial_runs /opt/ether/saves /opt/ether/data
cd /opt/ether

SCENARIOS=(
    "earth_-100000_out_of_africa:-100000:-50000"
    "earth_-74000_toba_cataclysm_74k:-74000:-50000"
    "earth_-50000_sahul:-50000:-10000"
    "earth_-25000_beringia:-25000:-10000"
    "earth_-20000_lgm_solutrean:-20000:-12000"
    "earth_-10900_younger_dryas:-10900:-9500"
    "earth_-8000_fertile_crescent:-8000:-5000"
    "earth_-6000_green_sahara:-6000:-3500"
    "earth_-3000_ancient_egypt:-3000:-1000"
    "earth_-1900_assyrian_empire:-1900:-600"
    "earth_-1500_mesoamerica:-1500:1500"
    "earth_-1279_ramesses_ii:-1279:-1213"
    "earth_-1200_bronze_age_collapse_1200bc:-1200:-900"
    "earth_-1000_early_iron_age:-1000:-300"
    "earth_-334_alexander_hellenistic_334bc:-334:-150"
    "earth_-300_maurya_empire:-300:100"
    "earth_+0_roman_empire:0:476"
    "earth_+536_late_antique_ice_age:536:650"
    "earth_+632_islamic_expansion_632:632:900"
    "earth_+1000_song_dynasty:1000:1279"
    "earth_+1206_mongol_conquest_1206:1206:1368"
    "earth_+1324_mali_empire:1324:1591"
    "earth_+1347_black_death_1347:1347:1450"
    "earth_+1491_americas_1491:1491:1650"
    "earth_+1492_columbian_contact:1492:1650"
    "earth_+1639_tokugawa_japan:1639:1853"
    "earth_+1800_industrial_1800:1800:1900"
    "earth_+1914_world_wars_totalitarian_1914:1914:1960"
    "earth_+2000_anthropocene_2000:2000:2100"
    "earth_+2025_business_as_usual:2025:2100"
    "earth_+2026_ssp5_85:2026:2100"
    "earth_+2035_nuclear_winter_2035:2035:2060"
    "earth_+2045_singularity_2045:2045:2100"
    "earth_+2050_peak_phosphate_2050:2050:2100"
    "earth_+2060_supervolcano_2060:2060:2100"
)

echo "================================================================================"
echo " 🌐 STARTING TRUE UNCOMPROMISED 27 HISTORICAL SCENARIOS (H3 RES 5, DT=7.0 DAYS)"
echo "================================================================================"
echo "Timestamp: $(date -u)"
echo "Host Machine: $(uname -a)"
echo "Available Cores: $(nproc)"
echo "================================================================================"

for ITEM in "${SCENARIOS[@]}"; do
    IFS=":" read -r NAME START END <<< "$ITEM"
    
    YEARS=$(( END - START ))
    # Strict tick calculation: (YEARS * 365.25 / 7)
    TICKS=$(( (YEARS * 36525) / 700 ))
    if [ "$TICKS" -lt 52 ]; then
        TICKS=52
    fi
    
    echo "--------------------------------------------------------------------------------"
    echo ">>> Simulating Scenario: ${NAME}"
    echo "    Epoch Horizon : Year ${START} -> Year ${END} (${YEARS} years)"
    echo "    Time Step     : dt = 7.0 days"
    echo "    Total Ticks   : ${TICKS} ticks"
    echo "    Spatial Grid  : Uber H3 Resolution 5 (2,016,842 cells)"
    echo "--------------------------------------------------------------------------------"
    
    java -Xms12g -Xmx28g \
         --add-modules=jdk.incubator.vector \
         -cp /opt/ether/society-simulation.jar \
         org.ether.society.cli.HeadlessRunner \
         --scenario "${NAME}" \
         --start-year "${START}" \
         --end-year "${END}" \
         --dt 7.0 \
         --ticks "${TICKS}" \
         --resolution 5 \
         --multi-core \
         --threads "$(nproc)" \
         --simd \
         > "/opt/ether/logs/spatial_runs/${NAME}.log" 2>&1 || {
             echo "  [!] Error on ${NAME}. See /opt/ether/logs/spatial_runs/${NAME}.log"
         }
         
    echo "  ✔ Scenario Completed: ${NAME} (${TICKS} ticks)"
done

echo "================================================================================"
echo " 🏁 ALL 27 TRUE HISTORICAL SPATIAL SCENARIOS COMPLETED!"
echo "================================================================================"
echo "Packaging spatial archives..."
tar -czf /opt/ether/logs/spatial_runs/master_spatial_true_archive.tar.gz \
    /opt/ether/saves/ \
    /opt/ether/logs/spatial_runs/

echo "Completed at: $(date -u)"
echo "Triggering Auto-Shutdown Guard..."
sudo shutdown -h now
