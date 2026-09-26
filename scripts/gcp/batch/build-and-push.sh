#!/usr/bin/env bash
# ==============================================================================
# Ether — Build Docker Image and Push to Artifact Registry (Linux / macOS)
#
# Builds the Ether simulation engine Docker image locally using the multi-stage
# Dockerfile, then pushes it to Google Artifact Registry.
#
# Usage:
#   ./scripts/gcp/batch/build-and-push.sh [PROJECT_ID] [REGION] [TAG]
#
# Example:
#   ./scripts/gcp/batch/build-and-push.sh ether-509812 europe-west1 latest
#   ./scripts/gcp/batch/build-and-push.sh ether-509812 europe-west1 v1.0.0-beta.1
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
REGION="${2:-europe-west1}"
TAG="${3:-latest}"
JAR_PATH="target/society-simulation-1.0.0-beta.1-executable.jar"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/ether-registry/ether-engine:${TAG}"

# Run from project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}/../../.."

echo "=========================================================="
echo "     ETHER — BUILD & PUSH TO ARTIFACT REGISTRY"
echo "=========================================================="
echo "  Project ID : ${PROJECT_ID}"
echo "  Region     : ${REGION}"
echo "  Image Tag  : ${TAG}"
echo "  Image URI  : ${IMAGE}"
echo "----------------------------------------------------------"

# ── Step 1: Build Maven jar (skip if already built) ───────────────────────────
if [ ! -f "${JAR_PATH}" ]; then
  echo "[1/3] Building executable JAR (mvn clean package)..."
  mvn clean package -DskipTests -q
  echo "      JAR built: ${JAR_PATH}"
else
  echo "[1/3] Executable JAR already exists — skipping Maven build."
  echo "      (Delete '${JAR_PATH}' and re-run to force rebuild)"
fi

# ── Step 2: Configure Docker auth for Artifact Registry ───────────────────────
echo "[2/3] Configuring Docker authentication for Artifact Registry..."
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet
echo "      Docker auth configured."

# ── Step 3: Build and push Docker image ───────────────────────────────────────
echo "[3/3] Building Docker image and pushing to Artifact Registry..."
docker build \
  --tag "${IMAGE}" \
  --label "build.version=${TAG}" \
  --label "build.timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  .

docker push "${IMAGE}"

echo ""
echo "=========================================================="
echo "✅ Image pushed successfully!"
echo "  URI : ${IMAGE}"
echo ""
echo "To submit a batch job with this image:"
echo "  ./scripts/gcp/batch/submit-batch-job.sh \\"
echo "      OUT_OF_AFRICA 1000 5000 1 e2-standard-4 true \\"
echo "      ${PROJECT_ID} ${REGION} ${TAG}"
echo "=========================================================="
