#!/usr/bin/env bash
# ==============================================================================
# Ether — Submit a Google Cloud Batch Simulation Job (Linux / macOS)
#
# Submits a Cloud Batch job that:
#   - Pulls the Ether Docker image from Artifact Registry
#   - Runs the simulation in headless mode
#   - Uploads results to GCS bucket gs://ether-simulations/<job-name>/
#   - Uses Spot/Preemptible instances for up to 80% cost reduction
#   - Automatically terminates the VM on completion (zero idle cost)
#
# Usage:
#   ./scripts/gcp/batch/submit-batch-job.sh \
#       [SCENARIO] [TICKS] [CELLS] [WORKERS] [MACHINE_TYPE] [USE_SPOT] \
#       [PROJECT_ID] [REGION] [IMAGE_TAG]
#
# Examples:
#   # Simple single run with defaults:
#   ./scripts/gcp/batch/submit-batch-job.sh
#
#   # Custom heavy run with Spot instance:
#   ./scripts/gcp/batch/submit-batch-job.sh MESOPOTAMIA_BRONZE_AGE 5000 50000 1 e2-standard-8 true
#
#   # 4 parallel runs with different seeds (task array):
#   ./scripts/gcp/batch/submit-batch-job.sh OUT_OF_AFRICA 1000 10000 4
# ==============================================================================

set -euo pipefail

SCENARIO="${1:-OUT_OF_AFRICA}"
TICKS="${2:-1000}"
CELLS="${3:-5000}"
WORKERS="${4:-1}"
MACHINE_TYPE="${5:-e2-standard-4}"
USE_SPOT="${6:-true}"
PROJECT_ID="${7:-ether-509812}"
REGION="${8:-europe-west1}"
IMAGE_TAG="${9:-latest}"

GCS_BUCKET="ether-simulations"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/ether-registry/ether-engine:${IMAGE_TAG}"
SA_EMAIL="ether-batch-runner@${PROJECT_ID}.iam.gserviceaccount.com"

# Generate a unique job name (Cloud Batch job names must be lowercase alphanumeric + hyphens)
SCENARIO_SLUG=$(echo "${SCENARIO}" | tr '[:upper:]_' '[:lower:]-' | tr -cd '[:alnum:]-')
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
JOB_NAME="ether-${SCENARIO_SLUG:0:20}-${TIMESTAMP}"

echo "=========================================================="
echo "     ETHER — SUBMITTING CLOUD BATCH JOB"
echo "=========================================================="
echo "  Job Name     : ${JOB_NAME}"
echo "  Scenario     : ${SCENARIO}"
echo "  Ticks        : ${TICKS}"
echo "  Cells        : ${CELLS}"
echo "  Task replicas: ${WORKERS}"
echo "  Machine Type : ${MACHINE_TYPE}"
echo "  Spot/Preempt : ${USE_SPOT}"
echo "  Image        : ${IMAGE}"
echo "  Results GCS  : gs://${GCS_BUCKET}/${JOB_NAME}/"
echo "----------------------------------------------------------"

# ── Build Cloud Batch job JSON spec ──────────────────────────────────────────
# Provisioning model: SPOT for cost savings, STANDARD for reliability
if [ "${USE_SPOT}" = "true" ]; then
  PROVISIONING_MODEL="SPOT"
else
  PROVISIONING_MODEL="STANDARD"
fi

JOB_SPEC=$(cat << EOF
{
  "taskGroups": [
    {
      "taskSpec": {
        "runnables": [
          {
            "container": {
              "imageUri": "${IMAGE}",
              "entrypoint": "/bin/sh",
              "commands": ["-c",
                "java -Xms2g -Xmx10g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar ether.jar --headless --scenario=\${SCENARIO} --ticks=\${TICKS} --cells=\${CELLS} --profile && gsutil -m rsync -r /app/saves/ gs://${GCS_BUCKET}/${JOB_NAME}/task-\${BATCH_TASK_INDEX}/"
              ],
              "volumes": []
            },
            "environment": {
              "variables": {
                "MODE": "headless",
                "SCENARIO": "${SCENARIO}",
                "TICKS": "${TICKS}",
                "CELLS": "${CELLS}"
              }
            }
          }
        ],
        "computeResource": {
          "cpuMilli": 4000,
          "memoryMib": 12288
        },
        "maxRetryCount": 1,
        "maxRunDuration": "86400s"
      },
      "taskCount": ${WORKERS},
      "parallelism": ${WORKERS}
    }
  ],
  "allocationPolicy": {
    "instances": [
      {
        "policy": {
          "machineType": "${MACHINE_TYPE}",
          "provisioningModel": "${PROVISIONING_MODEL}"
        }
      }
    ],
    "serviceAccount": {
      "email": "${SA_EMAIL}"
    },
    "location": {
      "allowedLocations": ["regions/${REGION}"]
    }
  },
  "logsPolicy": {
    "destination": "CLOUD_LOGGING"
  }
}
EOF
)

# ── Submit the job ────────────────────────────────────────────────────────────
echo "[1/1] Submitting job '${JOB_NAME}' to Cloud Batch..."
echo "${JOB_SPEC}" | gcloud batch jobs submit "${JOB_NAME}" \
  --project="${PROJECT_ID}" \
  --location="${REGION}" \
  --config=-

echo ""
echo "=========================================================="
echo "✅ Job submitted!"
echo ""
echo "  Job Name : ${JOB_NAME}"
echo "  Results  : gs://${GCS_BUCKET}/${JOB_NAME}/"
echo ""
echo "Monitor progress:"
echo "  ./scripts/gcp/batch/monitor-batch-job.sh ${JOB_NAME} ${PROJECT_ID} ${REGION}"
echo ""
echo "Fetch results when complete:"
echo "  ./scripts/gcp/batch/fetch-batch-results.sh ${JOB_NAME}"
echo ""
echo "GCP Console:"
echo "  https://console.cloud.google.com/batch/jobs/${JOB_NAME}?project=${PROJECT_ID}"
echo "=========================================================="
