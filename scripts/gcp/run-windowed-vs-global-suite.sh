#!/usr/bin/env bash
# ==============================================================================
# ETHER: WINDOWED VS. GLOBAL FULL-SPHERE GCP VALIDATION HARNESS (Bash)
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"
INSTANCE_NAME="${3:-ether-master}"
AUTO_SHUTDOWN="${4:-true}"

echo "================================================================================"
echo " [ETHER ENGINE] WINDOWED VS GLOBAL FULL-SPHERE VALIDATION HARNESS (GCP)"
echo "================================================================================"
echo "  Project ID      : ${PROJECT_ID}"
echo "  Target Zone     : ${ZONE}"
echo "  Compute VM      : ${INSTANCE_NAME}"
echo "  Target Suite    : WindowedVsGlobalSpatialFalsificationSuiteTest"
echo "  Auto-Shutdown   : ${AUTO_SHUTDOWN}"
echo "--------------------------------------------------------------------------------"

# 1. Start VM
echo "[1/5] Starting GCP Compute Instance (${INSTANCE_NAME})..."
gcloud compute instances start "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet
sleep 10

# 2. Setup remote directory
echo "[2/5] Setting up remote directory structure..."
gcloud compute ssh "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet \
    --command="sudo mkdir -p /opt/ether/logs/windowed_validation && sudo chmod -R 777 /opt/ether"

# 3. Synchronize shaded JAR
echo "[3/5] Synchronizing shaded executable JAR..."
gcloud compute scp "target/society-simulation-1.0.0-beta.2-executable.jar" "${INSTANCE_NAME}:/opt/ether/society-simulation.jar" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet

# 4. Launch remote execution
echo "[4/5] Executing WindowedVsGlobalSpatialFalsificationHarness on ${INSTANCE_NAME}..."
REMOTE_CMD="cd /opt/ether && java -Xms4g -Xmx28g --add-modules=jdk.incubator.vector -cp /opt/ether/society-simulation.jar org.ether.society.analytics.WindowedVsGlobalSpatialFalsificationHarness 2>&1 | tee /opt/ether/logs/windowed_validation/execution.log"
gcloud compute ssh "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --command="${REMOTE_CMD}"

# 5. Fetch logs & shutdown if requested
echo "[5/5] Fetching execution logs..."
mkdir -p logs/windowed_validation
gcloud compute scp "${INSTANCE_NAME}:/opt/ether/logs/windowed_validation/*" logs/windowed_validation/ --zone="${ZONE}" --project="${PROJECT_ID}" --quiet

if [ "${AUTO_SHUTDOWN}" = "true" ]; then
    echo "Stopping instance ${INSTANCE_NAME}..."
    gcloud compute instances stop "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet
fi

echo "================================================================================"
echo " [SUCCESS] GCP VALIDATION COMPLETED"
echo "================================================================================"

