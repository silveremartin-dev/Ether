#!/usr/bin/env bash
# ==============================================================================
# Ether 1.0 — GCP Infrastructure Provisioning (Linux / macOS / POSIX)
# Sets up GCP VPC, firewall rules, and creates Master & Worker VMs.
# ==============================================================================

set -euo pipefail

PROJECT_ID="${1:-ether-509812}"
ZONE="${2:-europe-west1-b}"
REGION="${3:-europe-west1}"
MASTER_TYPE="${4:-e2-standard-4}"
WORKER_TYPE="${5:-e2-standard-4}"
CREATE_WORKER="${6:-true}"

echo "=========================================================="
echo "     ETHER — GOOGLE CLOUD INFRASTRUCTURE PROVISIONING     "
echo "=========================================================="
echo "  Project ID     : ${PROJECT_ID}"
echo "  Target Zone    : ${ZONE}"
echo "  Target Region  : ${REGION}"
echo "  Master Machine : ${MASTER_TYPE}"
echo "  Worker Machine : ${WORKER_TYPE}"
echo "----------------------------------------------------------"

unset CLOUDSDK_CORE_PROJECT || true

echo "[1/5] Configuring active gcloud project..."
gcloud config set project "${PROJECT_ID}"
gcloud config set compute/zone "${ZONE}"
gcloud config set compute/region "${REGION}"

echo "[2/5] Enabling Compute Engine API..."
gcloud services enable compute.googleapis.com --project="${PROJECT_ID}"

echo "[3/5] Configuring VPC firewall rules..."
if ! gcloud compute firewall-rules describe ether-cluster-allow --project="${PROJECT_ID}" >/dev/null 2>&1; then
    gcloud compute firewall-rules create ether-cluster-allow \
        --project="${PROJECT_ID}" \
        --direction=INGRESS \
        --priority=1000 \
        --network=default \
        --action=ALLOW \
        --rules="tcp:9090,tcp:54320" \
        --source-ranges="0.0.0.0/0" \
        --description="Allow Ether cluster sync 9090 and Postgres 54320"
    echo "Firewall rule created."
else
    echo "Firewall rule already exists."
fi

STARTUP_SCRIPT=$(mktemp /tmp/ether-startup.XXXXXX.sh)
cat << 'EOF' > "${STARTUP_SCRIPT}"
#!/usr/bin/env bash
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y openjdk-21-jdk-headless docker.io docker-compose git htop rsync curl
systemctl enable docker
systemctl start docker
mkdir -p /opt/ether/saves /opt/ether/logs /opt/ether/scripts /opt/ether/data /opt/ether/target
chmod -R 777 /opt/ether
echo "Ether VM Ready" > /opt/ether/ready.txt
EOF

echo "[4/5] Provisioning Master VM (ether-master)..."
if ! gcloud compute instances describe ether-master --zone="${ZONE}" --project="${PROJECT_ID}" >/dev/null 2>&1; then
    gcloud compute instances create ether-master \
        --zone="${ZONE}" \
        --project="${PROJECT_ID}" \
        --machine-type="${MASTER_TYPE}" \
        --image-family=ubuntu-2204-lts \
        --image-project=ubuntu-os-cloud \
        --boot-disk-size=40GB \
        --boot-disk-type=pd-balanced \
        --tags=ether-node \
        --metadata-from-file="startup-script=${STARTUP_SCRIPT}"
    echo "VM ether-master created."
else
    echo "VM ether-master already exists."
fi

if [ "${CREATE_WORKER}" = "true" ]; then
    echo "[5/5] Provisioning Worker VM (ether-worker)..."
    if ! gcloud compute instances describe ether-worker --zone="${ZONE}" --project="${PROJECT_ID}" >/dev/null 2>&1; then
        gcloud compute instances create ether-worker \
            --zone="${ZONE}" \
            --project="${PROJECT_ID}" \
            --machine-type="${WORKER_TYPE}" \
            --image-family=ubuntu-2204-lts \
            --image-project=ubuntu-os-cloud \
            --boot-disk-size=30GB \
            --boot-disk-type=pd-balanced \
            --tags=ether-node \
            --metadata-from-file="startup-script=${STARTUP_SCRIPT}"
        echo "VM ether-worker created."
    else
        echo "VM ether-worker already exists."
    fi
fi

rm -f "${STARTUP_SCRIPT}"

echo ""
echo "✅ Infrastructure ready. Listing VMs:"
gcloud compute instances list --project="${PROJECT_ID}"
