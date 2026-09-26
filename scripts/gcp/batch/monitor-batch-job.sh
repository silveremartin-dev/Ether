#!/usr/bin/env bash
# ==============================================================================
# Ether — Monitor a Cloud Batch Job (Linux / macOS)
#
# Usage:
#   ./scripts/gcp/batch/monitor-batch-job.sh <JOB_NAME> [PROJECT_ID] [REGION]
# ==============================================================================

set -euo pipefail

JOB_NAME="${1:?ERROR: JOB_NAME is required. Usage: $0 <JOB_NAME> [PROJECT_ID] [REGION]}"
PROJECT_ID="${2:-ether-509812}"
REGION="${3:-europe-west1}"

echo "=========================================================="
echo "     ETHER — CLOUD BATCH JOB MONITOR"
echo "=========================================================="
echo "  Job Name   : ${JOB_NAME}"
echo "  Project ID : ${PROJECT_ID}"
echo "  Region     : ${REGION}"
echo "----------------------------------------------------------"

echo ""
echo "── Job Status ────────────────────────────────────────────"
gcloud batch jobs describe "${JOB_NAME}" \
  --project="${PROJECT_ID}" \
  --location="${REGION}" \
  --format="yaml(status,createTime,updateTime)"

echo ""
echo "── Recent Logs (last 50 lines) ───────────────────────────"
gcloud logging read \
  "resource.type=batch.googleapis.com/Job AND resource.labels.job_id=${JOB_NAME}" \
  --project="${PROJECT_ID}" \
  --limit=50 \
  --format="value(timestamp, textPayload)" \
  --order=asc 2>/dev/null || echo "  (No logs yet — job may still be starting)"

echo ""
echo "GCP Console:"
echo "  https://console.cloud.google.com/batch/jobs/${JOB_NAME}?project=${PROJECT_ID}"
