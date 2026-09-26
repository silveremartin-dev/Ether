#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Stop Simulation VMs on GCP (Linux / macOS / POSIX)
# Halts VM compute instances immediately to stop billing while preserving data.
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"

echo "=========================================================="
echo "          ETHER — STOPPING GCP SIMULATION VMs             "
echo "=========================================================="
echo "  Project ID : ${PROJECT_ID}"
echo "  Zone       : ${ZONE}"
echo "----------------------------------------------------------"
echo "Stopping 'ether-master' and 'ether-worker'..."
echo "NOTE: All vCPU/RAM billing stops immediately."
echo "Disks and data (PostgreSQL database, savegames) are preserved."
echo ""

unset CLOUDSDK_CORE_PROJECT || true

gcloud compute instances stop ether-master ether-worker --zone="${ZONE}" --project="${PROJECT_ID}" --quiet

echo ""
echo "✅ All simulation VMs are STOPPED."
echo "Run ./scripts/gcp/start-vms.sh to power them back on anytime."
