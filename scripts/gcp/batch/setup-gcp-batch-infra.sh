#!/usr/bin/env bash
# ==============================================================================
# Ether — Google Cloud Batch: One-Time Infrastructure Setup (Linux / macOS)
#
# Creates all GCP resources required to run Ether simulations via Cloud Batch:
#   1. Enables required GCP APIs
#   2. Creates Artifact Registry repository (Docker image storage)
#   3. Creates GCS bucket (simulation results storage)
#   4. Creates service account with minimal required permissions
#
# This script is idempotent — safe to run multiple times.
#
# Usage:
#   ./scripts/gcp/batch/setup-gcp-batch-infra.sh [PROJECT_ID] [REGION]
#
# Example:
#   ./scripts/gcp/batch/setup-gcp-batch-infra.sh ether-509812 europe-west1
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
REGION="${2:-europe-west1}"
REGISTRY_NAME="ether-registry"
GCS_BUCKET="gs://ether-simulations"
SA_NAME="ether-batch-runner"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

echo "=========================================================="
echo "     ETHER — GCP BATCH INFRASTRUCTURE SETUP"
echo "=========================================================="
echo "  Project ID   : ${PROJECT_ID}"
echo "  Region       : ${REGION}"
echo "  Registry     : ${REGISTRY_NAME}"
echo "  GCS Bucket   : ${GCS_BUCKET}"
echo "  Service Acct : ${SA_EMAIL}"
echo "----------------------------------------------------------"

# ── Step 1: Enable required APIs ─────────────────────────────────────────────
echo "[1/4] Enabling GCP APIs..."
gcloud services enable \
  batch.googleapis.com \
  artifactregistry.googleapis.com \
  storage.googleapis.com \
  logging.googleapis.com \
  --project="${PROJECT_ID}"
echo "      APIs enabled."

# ── Step 2: Create Artifact Registry repository ───────────────────────────────
echo "[2/4] Creating Artifact Registry repository '${REGISTRY_NAME}'..."
if ! gcloud artifacts repositories describe "${REGISTRY_NAME}" \
    --location="${REGION}" --project="${PROJECT_ID}" > /dev/null 2>&1; then
  gcloud artifacts repositories create "${REGISTRY_NAME}" \
    --repository-format=docker \
    --location="${REGION}" \
    --project="${PROJECT_ID}" \
    --description="Ether simulation engine Docker images"
  echo "      Repository created."
else
  echo "      Repository already exists — skipping."
fi

# ── Step 3: Create GCS bucket for simulation results ─────────────────────────
echo "[3/4] Creating GCS bucket '${GCS_BUCKET}'..."
if ! gsutil ls -b "${GCS_BUCKET}" > /dev/null 2>&1; then
  gsutil mb -p "${PROJECT_ID}" -l "${REGION}" -b on "${GCS_BUCKET}"

  # 90-day lifecycle rule to auto-delete old simulation results (cost saving)
  cat > /tmp/ether-lifecycle.json << 'LIFECYCLE'
{
  "lifecycle": {
    "rule": [{
      "action": { "type": "Delete" },
      "condition": { "age": 90 }
    }]
  }
}
LIFECYCLE
  gsutil lifecycle set /tmp/ether-lifecycle.json "${GCS_BUCKET}"
  rm -f /tmp/ether-lifecycle.json

  echo "      Bucket created with 90-day auto-delete lifecycle."
else
  echo "      Bucket already exists — skipping."
fi

# ── Step 4: Create service account with minimal permissions ───────────────────
echo "[4/4] Creating service account '${SA_NAME}'..."
if ! gcloud iam service-accounts describe "${SA_EMAIL}" \
    --project="${PROJECT_ID}" > /dev/null 2>&1; then
  gcloud iam service-accounts create "${SA_NAME}" \
    --project="${PROJECT_ID}" \
    --display-name="Ether Batch Runner" \
    --description="Service account for Ether Cloud Batch simulation jobs"
  echo "      Service account created."
else
  echo "      Service account already exists — updating roles."
fi

# Bind required roles
for ROLE in \
  "roles/batch.jobsEditor" \
  "roles/storage.objectAdmin" \
  "roles/artifactregistry.reader" \
  "roles/logging.logWriter"; do
  gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${SA_EMAIL}" \
    --role="${ROLE}" \
    --quiet > /dev/null
done
echo "      Roles assigned."

echo ""
echo "=========================================================="
echo "✅ GCP Batch infrastructure is ready!"
echo ""
echo "Next steps:"
echo "  1. Build & push the Docker image:"
echo "     ./scripts/gcp/batch/build-and-push.sh ${PROJECT_ID} ${REGION}"
echo ""
echo "  2. Submit a simulation job:"
echo "     ./scripts/gcp/batch/submit-batch-job.sh OUT_OF_AFRICA 1000 5000"
echo "=========================================================="
