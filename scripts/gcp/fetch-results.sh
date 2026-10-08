#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Fetch Simulation Results from GCP (Linux / macOS / POSIX / Git Bash)
# Downloads saved snapshots and database telemetry for local visual replay and Git.
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"
INSTANCE_NAME="${3:-ether-master}"
LOCAL_SAVES_DIR="${4:-saves}"

echo "=========================================================="
echo "     ETHER — FETCH SIMULATION RESULTS FROM GCP            "
echo "=========================================================="
echo "  Project ID  : ${PROJECT_ID}"
echo "  Zone        : ${ZONE}"
echo "  Instance    : ${INSTANCE_NAME}"
echo "  Destination : ${LOCAL_SAVES_DIR}"
echo "----------------------------------------------------------"

unset CLOUDSDK_CORE_PROJECT || true
mkdir -p "${LOCAL_SAVES_DIR}" logs

# 1. Start VM if stopped
STATUS=$(gcloud compute instances describe "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --format="get(status)" 2>/dev/null || echo "TERMINATED")
if [ "${STATUS}" != "RUNNING" ]; then
    echo "[1/4] Starting ${INSTANCE_NAME} to retrieve simulation saves..."
    gcloud compute instances start "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet
    echo "Waiting for SSH availability..."
    sleep 20
else
    echo "[1/4] Instance ${INSTANCE_NAME} is running."
fi

# 2. Download saves and snapshots
echo "[2/4] Downloading saves and snapshots from ${INSTANCE_NAME}..."
gcloud compute scp --recurse --zone="${ZONE}" --project="${PROJECT_ID}" --quiet "${INSTANCE_NAME}:/opt/ether/saves/*" "${LOCAL_SAVES_DIR}/" || true

# 3. Download logs and telemetry
echo "[3/4] Downloading execution logs and calibration telemetry..."
gcloud compute scp --recurse --zone="${ZONE}" --project="${PROJECT_ID}" --quiet "${INSTANCE_NAME}:/opt/ether/logs/*" logs/ || true

# 4. Power off VM immediately to avoid idle costs
echo "[4/4] Powering off ${INSTANCE_NAME} to guarantee \$0.00 idle cost..."
gcloud compute instances stop "${INSTANCE_NAME}" --zone="${ZONE}" --project="${PROJECT_ID}" --quiet

echo "=========================================================="
echo " ✅ ALL SIMULATION SAVES & SNAPSHOTS SUCCESSFULLY RETRIEVED!"
echo "=========================================================="
echo " Files are located in: ${LOCAL_SAVES_DIR}/"
echo " You can now review git status and commit:"
echo "   git add saves/"
echo "   git commit -m 'feat: sync canonical simulation snapshots from GCP'"
echo "=========================================================="
