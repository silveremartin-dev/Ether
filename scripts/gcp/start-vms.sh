#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Start Simulation VMs on GCP (Linux / macOS / POSIX)
# Powers on VM compute instances and displays their running state.
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"

echo "=========================================================="
echo "          ETHER — STARTING GCP SIMULATION VMs             "
echo "=========================================================="
echo "  Project ID : ${PROJECT_ID}"
echo "  Zone       : ${ZONE}"
echo "----------------------------------------------------------"
echo "Starting 'ether-master' and 'ether-worker'..."

unset CLOUDSDK_CORE_PROJECT || true

gcloud compute instances start ether-master ether-worker --zone="${ZONE}" --project="${PROJECT_ID}" --quiet

echo ""
echo "✅ All simulation VMs are RUNNING."
echo "Instances:"
gcloud compute instances list --project="${PROJECT_ID}" --zone="${ZONE}"
