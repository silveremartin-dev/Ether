#!/usr/bin/env bash
# ==============================================================================
# ETHER MASTER OVERNIGHT BATCH CAMPAIGN ORCHESTRATOR (BASH / GCP SPOT)
# ==============================================================================
# Full 4-Axis Planetary Cliodynamics, 27 Bifurcations, and 9-Epoch Baseline Suite
# Resolution: Uber H3 Resolution 5 (2,016,842 hexagons, 8.5 km cell spacing)
# Uniform Time Step: dt = 7.0 days (52.14 ticks/yr) across all scenarios
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"
INSTANCE_NAME="${3:-ether-master}"
H3_RESOLUTION="${4:-5}"
DT_DAYS="${5:-7.0}"
JAR_PATH="${6:-target/society-simulation-1.0.0-beta.2-executable.jar}"

echo "================================================================================"
echo " 🚀 ETHER ENGINE: MASTER OVERNIGHT HIGH-FIDELITY BATCH CAMPAIGN (GCP SPOT)     "
echo "================================================================================"
echo "  Project ID         : ${PROJECT_ID}"
echo "  Zone               : ${ZONE}"
echo "  Instance Target    : ${INSTANCE_NAME}"
echo "  Spatial Resolution : Uber H3 Resolution ${H3_RESOLUTION} (2,016,842 cells, ~8.5 km)"
echo "  Uniform Time-Step  : dt = ${DT_DAYS} days (52.14 ticks/year)"
echo "  Target Suite       : 9 Calibrated Epochs + 27 Bifurcations + 25 Engine Ablations"
echo "--------------------------------------------------------------------------------"

# Step 1: Ensure JAR is built
if [ ! -f "${JAR_PATH}" ]; then
    echo "[!] Executable JAR not found. Building now..."
    mvn clean package -DskipTests
fi

# Step 2: Start GCP VM
echo -e "\n[1/5] Starting GCP Compute Instance (${INSTANCE_NAME})..."
gcloud compute instances start "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}"
sleep 15

# Step 3: Ensure remote directory structure
echo -e "\n[2/5] Creating remote working directory structure on ${INSTANCE_NAME}..."
gcloud compute ssh "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet \
    --command="sudo mkdir -p /opt/ether/logs/calibration /opt/ether/logs/gcp_batch /opt/ether/saves /opt/ether/data && sudo chmod -R 777 /opt/ether"

# Step 4: Upload shaded JAR
echo -e "\n[3/5] Uploading executable shaded JAR to ${INSTANCE_NAME}..."
gcloud compute scp "${JAR_PATH}" "${INSTANCE_NAME}:/opt/ether/society-simulation.jar" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet

# Step 5: Upload map assets (only ready-to-use ether tensors)
if [ -d "data/maps/ether" ]; then
    echo -e "\n[4/5] Synchronizing planetary map tensors (data/maps/ether) to ${INSTANCE_NAME}..."
    gcloud compute ssh "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --command="sudo mkdir -p /opt/ether/data/maps && sudo chmod -R 777 /opt/ether/data"
    gcloud compute scp --recurse data/maps/ether "${INSTANCE_NAME}:/opt/ether/data/maps/" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet
fi

# Step 6: Launch background batch execution with auto-shutdown
echo -e "\n[5/5] Launching High-Fidelity Overnight Execution Pipeline on ${INSTANCE_NAME}..."
REMOTE_SCRIPT=$(cat <<EOF
#!/bin/bash
set -e
echo '==============================================================================='
echo '   ETHER PLANETARY SIMULATION ENGINE: OVERNIGHT HIGH-FIDELITY BATCH RUNNER   '
echo '==============================================================================='
echo 'Timestamp: '\$(date -u)
echo 'Working Directory: /opt/ether'
echo 'Spatial Resolution: H3 Res ${H3_RESOLUTION} (2,016,842 cells)'
echo 'Uniform Time Step: dt = ${DT_DAYS} days'
echo '==============================================================================='

cd /opt/ether

# Run the full 4-Axis & 27 Bifurcation Master Suite
echo '>>> Launching Full Master 4-Axis Suite & 27 Bifurcation Campaign...'
java -Xms8g -Xmx50g \
     --add-modules=jdk.incubator.vector \
     -cp society-simulation.jar \
     org.ether.society.analytics.HistoricalScenarioCalibrationHarness \
     --resolution ${H3_RESOLUTION} \
     --dt ${DT_DAYS} \
     2>&1 | tee /opt/ether/logs/gcp_batch/overnight_execution.log

echo '>>> Packaging Results Archive...'
tar -czf /opt/ether/logs/gcp_batch/master_results_archive_\$(date +%Y%m%d_%H%M%S).tar.gz \
    /opt/ether/logs/calibration/ \
    /opt/ether/logs/gcp_batch/overnight_execution.log

echo '>>> Computation Complete at: '\$(date -u)
echo '>>> Triggering Immediate Auto-Shutdown Guard (\$0.00 Idle Cost)...'
sudo shutdown -h now
EOF
)

SCRIPT_B64=$(echo "${REMOTE_SCRIPT}" | base64 -w 0)
LAUNCH_CMD="echo '${SCRIPT_B64}' | base64 -d > /opt/ether/run_batch.sh && chmod +x /opt/ether/run_batch.sh && nohup /opt/ether/run_batch.sh > /opt/ether/logs/gcp_batch/batch_launcher.log 2>&1 &"
gcloud compute ssh "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --command="${LAUNCH_CMD}"

echo "--------------------------------------------------------------------------------"
echo " ✔ OVERNIGHT BATCH CAMPAIGN SUCCESSFULLY DEPLOYED & RUNNING IN BACKGROUND!"
echo "--------------------------------------------------------------------------------"
echo "  Remote Log      : /opt/ether/logs/gcp_batch/overnight_execution.log"
echo "  Results Output  : /opt/ether/logs/calibration/master_calibration_and_falsification_results.json"
echo "  Academic Report : /opt/ether/logs/calibration/calibration_and_falsification_academic_report.md"
echo "  Auto-Shutdown   : ACTIVE (VM will power off immediately upon completion for \$0 idle cost)"
echo "================================================================================"
