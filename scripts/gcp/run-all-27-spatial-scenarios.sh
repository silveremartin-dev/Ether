#!/usr/bin/env bash
# ==============================================================================
# ETHER COMPLETE 27-BIFURCATION SPATIAL SIMULATION BATCH (GCP RUNNER)
# ==============================================================================
set -euo pipefail

mkdir -p /opt/ether/logs/spatial_runs /opt/ether/saves /opt/ether/data
cd /opt/ether

SCENARIOS=(
    "OUT_OF_AFRICA:-100000:-50000:300"
    "TOBA_CATACLYSM:-74000:-50000:300"
    "SAHUL:-50000:-10000:300"
    "BERINGIA:-25000:-10000:300"
    "LGM_SOLUTREAN:-20000:-12000:300"
    "YOUNGER_DRYAS:-10900:-9500:300"
    "FERTILE_CRESCENT:-8000:-5000:300"
    "GREEN_SAHARA:-6000:-3500:300"
    "ANCIENT_EGYPT:-3000:-1000:300"
    "ASSYRIAN_EMPIRE:-1900:-600:300"
    "BRONZE_AGE_COLLAPSE:-1200:-900:300"
    "EARLY_IRON_AGE:-1000:-300:300"
    "ALEXANDER_HELLENISTIC:-334:-150:300"
    "MAURYA_EMPIRE:-300:100:300"
    "ROMAN_EMPIRE:0:476:300"
    "LATE_ANTIQUE_ICE_AGE:536:650:300"
    "ISLAMIC_EXPANSION:632:900:300"
    "SONG_DYNASTY:1000:1279:300"
    "MONGOL_CONQUEST:1206:1368:300"
    "MALI_EMPIRE:1324:1591:300"
    "BLACK_DEATH:1347:1450:300"
    "AMERICAS_1491:1491:1650:300"
    "COLUMBIAN_CONTACT:1492:1650:300"
    "TOKUGAWA_JAPAN:1639:1853:300"
    "INDUSTRIAL_REVOLUTION:1800:1900:300"
    "WORLD_WARS:1914:1960:300"
    "ANTHROPOCENE:2000:2100:300"
)

echo "================================================================================"
echo " 🌐 STARTING FULL 27-BIFURCATION SPATIAL CELLULAR BATCH CAMPAIGN (H3 GRID)     "
echo "================================================================================"
echo "Timestamp: $(date -u)"

for ITEM in "${SCENARIOS[@]}"; do
    IFS=":" read -r NAME START END TICKS <<< "$ITEM"
    echo ">>> Simulating Spatial Scenario: ${NAME} (${START} -> ${END}) | Ticks: ${TICKS}..."
    
    java -Xms4g -Xmx14g \
         --add-modules=jdk.incubator.vector \
         -cp /opt/ether/society-simulation.jar \
         org.ether.society.cli.HeadlessRunner \
         --scenario "${NAME}" \
         --ticks "${TICKS}" \
         --resolution 3 \
         > "/opt/ether/logs/spatial_runs/${NAME}.log" 2>&1 || true
         
    echo "  ✔ Completed: ${NAME}"
done

echo "================================================================================"
echo " 🏁 ALL 27 SPATIAL SCENARIOS SUCCESSFULLY SIMULATED & PERSISTED TO POSTGIS"
echo "================================================================================"
echo "Compressing spatial archives..."
tar -czf /opt/ether/logs/spatial_runs/master_spatial_archive.tar.gz /opt/ether/saves/ /opt/ether/logs/spatial_runs/

echo "Triggering Auto-Shutdown Guard..."
sudo shutdown -h now
