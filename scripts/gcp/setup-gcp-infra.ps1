param(
    [string]$ProjectId = "ether-509812",
    [string]$Region = "europe-west1",
    [string]$Zone = "europe-west1-b",
    [string]$MasterMachineType = "e2-standard-4",
    [string]$WorkerMachineType = "e2-standard-4",
    [switch]$CreateWorker = $true
)

$ErrorActionPreference = "Stop"
Remove-Item Env:\CLOUDSDK_CORE_PROJECT -ErrorAction SilentlyContinue

Write-Host "=========================================================="
Write-Host "     ETHER - GOOGLE CLOUD INFRASTRUCTURE PROVISIONING    "
Write-Host "=========================================================="
Write-Host "  Project ID    : $ProjectId"
Write-Host "  Zone          : $Zone"
Write-Host "  Master Machine: $MasterMachineType"
Write-Host "  Worker Machine: $WorkerMachineType"
Write-Host "----------------------------------------------------------"

Write-Host "[1/5] Configuring active gcloud project to $ProjectId..."
gcloud config set project $ProjectId
gcloud config set compute/zone $Zone
gcloud config set compute/region $Region

Write-Host "[2/5] Enabling Compute Engine API..."
gcloud services enable compute.googleapis.com

Write-Host "[3/5] Configuring VPC firewall rules..."
$fwExists = gcloud compute firewall-rules list --filter="name=ether-cluster-allow" --format="value(name)"
if (-not $fwExists) {
    gcloud compute firewall-rules create ether-cluster-allow --direction=INGRESS --priority=1000 --network=default --action=ALLOW --rules "tcp:9090,tcp:54320" --source-ranges "0.0.0.0/0" --description="Allow Ether cluster sync 9090 and Postgres 54320"
    Write-Host "Firewall rule created."
} else {
    Write-Host "Firewall rule already exists."
}

$startupScript = @'
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
'@

$tempScriptPath = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($tempScriptPath, $startupScript)

Write-Host "[4/5] Provisioning Master VM (ether-master)..."
$masterExists = gcloud compute instances list --filter="name=ether-master" --format="value(name)"
if (-not $masterExists) {
    gcloud compute instances create ether-master --zone=$Zone --machine-type=$MasterMachineType --image-family=ubuntu-2204-lts --image-project=ubuntu-os-cloud --boot-disk-size=40GB --boot-disk-type=pd-balanced --tags ether-node --metadata-from-file="startup-script=$tempScriptPath"
    Write-Host "VM ether-master created."
} else {
    Write-Host "VM ether-master already exists."
}

if ($CreateWorker) {
    Write-Host "[5/5] Provisioning Worker VM (ether-worker)..."
    $workerExists = gcloud compute instances list --filter="name=ether-worker" --format="value(name)"
    if (-not $workerExists) {
        gcloud compute instances create ether-worker --zone=$Zone --machine-type=$WorkerMachineType --image-family=ubuntu-2204-lts --image-project=ubuntu-os-cloud --boot-disk-size=30GB --boot-disk-type=pd-balanced --tags ether-node --metadata-from-file="startup-script=$tempScriptPath"
        Write-Host "VM ether-worker created."
    } else {
        Write-Host "VM ether-worker already exists."
    }
}

Remove-Item -Path $tempScriptPath -Force -ErrorAction SilentlyContinue

Write-Host "Infrastructure ready. Listing VMs:"
gcloud compute instances list
