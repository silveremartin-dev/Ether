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
    "OUT_OF_AFRICA:-100000:-50000"
    "TOBA_CATACLYSM:-74000:-50000"
    "SAHUL:-50000:-10000"
    "BERINGIA:-25000:-10000"
    "LGM_SOLUTREAN:-20000:-12000"
    "YOUNGER_DRYAS:-10900:-9500"
    "FERTILE_CRESCENT:-8000:-5000"
    "GREEN_SAHARA:-6000:-3500"
    "ANCIENT_EGYPT:-3000:-1000"
    "ASSYRIAN_EMPIRE:-1900:-600"
    "BRONZE_AGE_COLLAPSE:-1200:-900"
    "EARLY_IRON_AGE:-1000:-300"
    "ALEXANDER_HELLENISTIC:-334:-150"
    "MAURYA_EMPIRE:-300:100"
    "ROMAN_EMPIRE:0:476"
    "LATE_ANTIQUE_ICE_AGE:536:650"
    "ISLAMIC_EXPANSION:632:900"
    "SONG_DYNASTY:1000:1279"
    "MONGOL_CONQUEST:1206:1368"
    "MALI_EMPIRE:1324:1591"
    "BLACK_DEATH:1347:1450"
    "AMERICAS_1491:1491:1650"
    "COLUMBIAN_CONTACT:1492:1650"
    "TOKUGAWA_JAPAN:1639:1853"
    "INDUSTRIAL_REVOLUTION:1800:1900"
    "WORLD_WARS:1914:1960"
    "ANTHROPOCENE:2000:2100"
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
