#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Fetch Simulation Results from GCP (Linux / macOS / POSIX)
# Downloads saved snapshots and database telemetry for local visual replay.
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"
LOCAL_SAVES_DIR="${3:-saves}"

echo "=========================================================="
echo "     ETHER — FETCH SIMULATION RESULTS FROM GCP            "
echo "=========================================================="
echo "  Project ID  : ${PROJECT_ID}"
echo "  Zone        : ${ZONE}"
echo "  Destination : ${LOCAL_SAVES_DIR}"
echo "----------------------------------------------------------"

unset CLOUDSDK_CORE_PROJECT || true

mkdir -p "${LOCAL_SAVES_DIR}"

echo "Downloading saves and logs from ether-master..."
gcloud compute scp --recurse --zone="${ZONE}" --project="${PROJECT_ID}" --quiet ether-master:/opt/ether/saves . || true
gcloud compute scp --recurse --zone="${ZONE}" --project="${PROJECT_ID}" --quiet ether-master:/opt/ether/logs . || true

echo ""
echo "✅ Saves and logs synchronized to local workspace."
echo "Launch Ether in local mode (./scripts/run.sh) to view the replay."
