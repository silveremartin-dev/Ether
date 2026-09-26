#!/usr/bin/env bash
# ==============================================================================
# Ether — Fetch Batch Simulation Results from GCS (Linux / macOS)
#
# Downloads simulation snapshots and saves produced by a Cloud Batch job
# from Google Cloud Storage to a local directory.
#
# Usage:
#   ./scripts/gcp/batch/fetch-batch-results.sh <JOB_NAME> [LOCAL_DIR] [PROJECT_ID]
#
# Example:
#   ./scripts/gcp/batch/fetch-batch-results.sh ether-out-of-africa-20260926-183000
#   ./scripts/gcp/batch/fetch-batch-results.sh ether-out-of-africa-20260926-183000 ./saves/batch/run-01
# ==============================================================================

set -euo pipefail

JOB_NAME="${1:?ERROR: JOB_NAME is required. Usage: $0 <JOB_NAME> [LOCAL_DIR] [PROJECT_ID]}"
LOCAL_DIR="${2:-./saves/batch/${JOB_NAME}}"
PROJECT_ID="${3:-ether-509812}"
GCS_PATH="gs://ether-simulations/${JOB_NAME}/"

echo "=========================================================="
echo "     ETHER — FETCH BATCH RESULTS FROM GCS"
echo "=========================================================="
echo "  Job Name   : ${JOB_NAME}"
echo "  GCS Source : ${GCS_PATH}"
echo "  Local Dest : ${LOCAL_DIR}"
echo "----------------------------------------------------------"

mkdir -p "${LOCAL_DIR}"

echo "Downloading results..."
gsutil -m rsync -r "${GCS_PATH}" "${LOCAL_DIR}/"

echo ""
echo "=========================================================="
echo "✅ Results downloaded to: ${LOCAL_DIR}"
ls -lh "${LOCAL_DIR}" 2>/dev/null || true
echo ""
echo "To replay locally:"
echo "  ./scripts/docker-deploy.sh dev"
echo "  # Then open Ether UI and load saves from ${LOCAL_DIR}"
echo "=========================================================="
