#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — Remote GCP Deployment & Simulation Runner (Linux / macOS / POSIX)
# Builds (optional), uploads, configures PostgreSQL, and executes simulation.
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"
SCENARIO="${3:-OUT_OF_AFRICA}"
TICKS="${4:-1000}"
CELLS="${5:-5000}"
CLUSTER_MODE="${6:-false}"
SKIP_BUILD="${7:-false}"
RESOLUTION="${8:-}"

echo "=========================================================="
echo "     ETHER — REMOTE GCP DEPLOYMENT AND EXECUTION          "
echo "=========================================================="
echo "  Project ID     : ${PROJECT_ID}"
echo "  Target Zone    : ${ZONE}"
echo "  Scenario       : ${SCENARIO}"
echo "  Ticks to Run   : ${TICKS}"
echo "  H3 Grid Cells  : ${CELLS}"
echo "  H3 Resolution  : ${RESOLUTION:-Default}"
echo "  Cluster Mode   : ${CLUSTER_MODE}"
echo "----------------------------------------------------------"

unset CLOUDSDK_CORE_PROJECT || true
JAR_PATH="target/society-simulation-1.0.0-beta.1-executable.jar"

if [ "${SKIP_BUILD}" != "true" ] || [ ! -f "${JAR_PATH}" ]; then
    echo "[1/5] Building executable JAR locally..."
    mvn clean package "-Dmaven.test.skip=true"
else
    echo "[1/5] Using existing executable JAR (${JAR_PATH})..."
fi

echo "[2/5] Preparing remote directories on ether-master..."
gcloud compute ssh ether-master --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --command="sudo mkdir -p /opt/ether/target /opt/ether/scripts /opt/ether/saves /opt/ether/logs /opt/ether/data && sudo chmod -R 777 /opt/ether && touch /opt/ether/.env && sudo pkill -9 -f society-simulation || true"

echo "[3/5] Uploading JAR and configuration files to ether-master..."
gcloud compute scp --zone="${ZONE}" --project="${PROJECT_ID}" --quiet "${JAR_PATH}" ether-master:/opt/ether/target/society-simulation-1.0.0-beta.1-executable.jar
gcloud compute scp --zone="${ZONE}" --project="${PROJECT_ID}" --quiet docker-compose.yml ether-master:/opt/ether/docker-compose.yml
gcloud compute scp --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --recurse scripts/init-db.sql scripts/start-headless.sh scripts/start-master.sh scripts/start-worker.sh ether-master:/opt/ether/scripts/

echo "[4/5] Ensuring PostgreSQL container is running on ether-master..."
gcloud compute ssh ether-master --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --command="sudo docker start ether_postgres 2>/dev/null || sudo docker run -d --name ether_postgres -p 54320:5432 -e POSTGRES_DB=ether_simulation -e POSTGRES_USER=ether -e POSTGRES_PASSWORD=dev_password --restart unless-stopped postgis/postgis:15-3.3"

if [ "${CLUSTER_MODE}" = "true" ]; then
    echo "[5/5] Cluster mode enabled: setting up ether-worker..."
    MASTER_INTERNAL_IP=$(gcloud compute instances describe ether-master --zone="${ZONE}" --project="${PROJECT_ID}" --format="get(networkInterfaces[0].networkIP)" | tr -d '[:space:]')
    echo "Master Internal IP: ${MASTER_INTERNAL_IP}"

    gcloud compute ssh ether-worker --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --command="sudo mkdir -p /opt/ether/target /opt/ether/scripts /opt/ether/saves /opt/ether/logs && sudo chmod -R 777 /opt/ether && touch /opt/ether/.env && sudo pkill -9 -f society-simulation || true"
    gcloud compute scp --zone="${ZONE}" --project="${PROJECT_ID}" --quiet "${JAR_PATH}" ether-worker:/opt/ether/target/society-simulation-1.0.0-beta.1-executable.jar
    gcloud compute scp --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --recurse scripts/start-worker.sh ether-worker:/opt/ether/scripts/

    echo "Launching Worker node in background..."
    gcloud compute ssh ether-worker --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --command="cd /opt/ether && chmod +x scripts/*.sh && ( nohup ./scripts/start-worker.sh ${MASTER_INTERNAL_IP} 9090 EtherClusterSecret2026 > logs/worker.log 2>&1 & ) < /dev/null > /dev/null 2>&1"

    sleep 4

    echo "Executing Master Simulation Node in Cluster Mode..."
    gcloud compute ssh ether-master --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --command="cd /opt/ether && chmod +x scripts/*.sh && ./scripts/start-master.sh '${SCENARIO}' 9090 EtherClusterSecret2026 ${TICKS} ${CELLS} '${RESOLUTION}'"
else
    echo "[5/5] Launching Headless Simulation on ether-master..."
    RES_FLAG=""
    if [ -n "${RESOLUTION}" ]; then
        RES_FLAG="--res=${RESOLUTION}"
    fi
    gcloud compute ssh ether-master --zone="${ZONE}" --project="${PROJECT_ID}" --quiet --command="cd /opt/ether && chmod +x scripts/*.sh && ./scripts/start-headless.sh '${SCENARIO}' ${TICKS} ${CELLS} ${RES_FLAG}"
fi

echo ""
echo "✅ Simulation execution finished."
echo "Run ./scripts/gcp/fetch-results.sh to download snapshots for local replay."
