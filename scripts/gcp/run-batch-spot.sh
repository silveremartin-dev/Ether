#!/bin/bash
# ==============================================================================
# Ether 1.0 — Automated GCP Batch Spot Execution Suite (Bash)
# ==============================================================================
PROJECT_ID="ether-509812"
ZONE="europe-west1-b"
MASTER_VM="ether-master"
WORKER_VM="ether-worker"
SCENARIOS="INDUSTRIAL,MODERN"
RESOLUTIONS="4,5"
TICKS=24

echo "=========================================================="
echo "     ETHER -- GCP BATCH SPOT SIMULATION ENGINE (Linux)    "
echo "=========================================================="

JAR_PATH="target/society-simulation-1.0.0-beta.2-executable.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo "Executable JAR not found. Please run: mvn package -Dmaven.test.skip=true"
    exit 1
fi

echo "[1/3] Uploading updated binaries and discrete maps..."
gcloud compute scp "$JAR_PATH" "${MASTER_VM}:~/target/society-simulation-1.0.0-beta.2-executable.jar" --project="$PROJECT_ID" --zone="$ZONE" --quiet
gcloud compute scp "$JAR_PATH" "${WORKER_VM}:~/target/society-simulation-1.0.0-beta.2-executable.jar" --project="$PROJECT_ID" --zone="$ZONE" --quiet
gcloud compute scp --recurse data/maps/ether/earth/1800 "${MASTER_VM}:~/data/maps/ether/earth/" --project="$PROJECT_ID" --zone="$ZONE" --quiet

echo "[2/3] Launching background worker and master runners..."
gcloud compute ssh "$WORKER_VM" --project="$PROJECT_ID" --zone="$ZONE" --command="nohup java -Xms4g -Xmx28g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar target/society-simulation-1.0.0-beta.2-executable.jar --headless --mode=cluster --role=worker --master-host=10.132.0.3 --port=9090 --secret=EtherClusterSecret2026 > worker_batch.log 2>&1 &" --quiet
gcloud compute ssh "$MASTER_VM" --project="$PROJECT_ID" --zone="$ZONE" --command="nohup java -Xms4g -Xmx28g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar target/society-simulation-1.0.0-beta.2-executable.jar --headless --mode=cluster --role=master --port=9090 --secret=EtherClusterSecret2026 --scenario=INDUSTRIAL --ticks=24 --cells=0 --res=4 --profile > master_batch_stdout.log 2>&1 &" --quiet

echo "[3/3] Detached simulation successfully running in cloud!"
