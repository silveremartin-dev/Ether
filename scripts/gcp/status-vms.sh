#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Check Simulation Cluster Status (Linux / macOS / POSIX)
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"

echo "=========================================================="
echo "          ETHER — GCP SIMULATION CLUSTER STATUS           "
echo "=========================================================="
echo "  Project ID : ${PROJECT_ID}"
echo "  Zone       : ${ZONE}"
echo "----------------------------------------------------------"

unset CLOUDSDK_CORE_PROJECT || true

gcloud compute instances list --project="${PROJECT_ID}" --zones="${ZONE}"
