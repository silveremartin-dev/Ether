#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — GCP Instance Management and Teardown (Linux / macOS / POSIX)
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ACTION="${2:-status}"
ZONE="${3:-europe-west1-b}"

echo "=========================================================="
echo "       ETHER — GCP INSTANCE MANAGEMENT AND COST           "
echo "=========================================================="
echo "  Project ID : ${PROJECT_ID}"
echo "  Action     : ${ACTION}"
echo "  Zone       : ${ZONE}"
echo "----------------------------------------------------------"

unset CLOUDSDK_CORE_PROJECT || true

case "${ACTION}" in
    status)
        gcloud compute instances list --project="${PROJECT_ID}" --zone="${ZONE}"
        ;;
    stop)
        echo "Stopping VMs (billing stops for vCPU/RAM)..."
        gcloud compute instances stop ether-master ether-worker --zone="${ZONE}" --project="${PROJECT_ID}" --quiet
        echo "Instances stopped."
        ;;
    start)
        echo "Restarting VMs..."
        gcloud compute instances start ether-master ether-worker --zone="${ZONE}" --project="${PROJECT_ID}" --quiet
        echo "Instances started."
        ;;
    delete)
        echo "Deleting VMs..."
        gcloud compute instances delete ether-master ether-worker --zone="${ZONE}" --project="${PROJECT_ID}" --quiet
        echo "Instances deleted."
        ;;
    *)
        echo "Unknown action: ${ACTION}. Available: status, stop, start, delete"
        exit 1
        ;;
esac
