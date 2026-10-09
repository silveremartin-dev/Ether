# ==============================================================================
# Ether 1.0 — Launch Master Overnight Simulation Suite on GCP (PowerShell / Windows)
# Builds shaded JAR, uploads to GCP VM, runs detached in background, auto-shutdowns.
# ==============================================================================

param (
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [string]$InstanceName = "ether-master",
    [string]$JarPath = "target/society-simulation-1.0.0-beta.2-executable.jar"
)

$ErrorActionPreference = "Stop"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " [ETHER ENGINE] MASTER OVERNIGHT BATCH CAMPAIGN (GCP SPOT)                      " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  Project ID      : $ProjectId"
Write-Host "  Zone            : $Zone"
Write-Host "  Instance Target : $InstanceName"
Write-Host "  Jar Path        : $JarPath"
Write-Host "--------------------------------------------------------------------------------"

# Step 1: Ensure JAR is built
if (-not (Test-Path $JarPath)) {
    Write-Host "[1/5] Executable shaded JAR not found. Building now with Maven..." -ForegroundColor Yellow
    mvn clean package "-Dmaven.test.skip=true" "-Djacoco.skip=true"
} else {
    Write-Host "[1/5] Using existing shaded JAR: $JarPath" -ForegroundColor Green
}

# Step 2: Ensure GCP VM exists and is running
$vmExists = $false
try {
    $describe = (gcloud compute instances describe $InstanceName --zone=$Zone --project=$ProjectId --format="get(status)" 2>$null)
    if ($describe) { $vmExists = $true }
} catch {
    $vmExists = $false
}

if (-not $vmExists) {
    Write-Host "[2/5] Provisioning new Compute Instance ($InstanceName) with e2-standard-4 & 50GB disk..." -ForegroundColor Yellow
    gcloud compute instances create $InstanceName --zone=$Zone --project=$ProjectId --machine-type="e2-standard-4" --image-family="ubuntu-2204-lts" --image-project="ubuntu-os-cloud" --boot-disk-size="50GB" --boot-disk-type="pd-balanced" --quiet

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to provision GCP VM instance $InstanceName. Check gcloud quotas or permissions."
        exit 1
    }

    Write-Host "Waiting 35 seconds for initial OS boot and network setup..." -ForegroundColor Yellow
    Start-Sleep -Seconds 35

    Write-Host "Installing OpenJDK 21 on $InstanceName..." -ForegroundColor Cyan
    gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="sudo apt-get update && sudo apt-get install -y openjdk-21-jdk-headless htop curl && sudo mkdir -p /opt/ether/saves /opt/ether/logs /opt/ether/data && sudo chmod -R 777 /opt/ether"
} else {
    Write-Host "[2/5] Starting existing Compute Instance ($InstanceName)..." -ForegroundColor Yellow
    gcloud compute instances start $InstanceName --zone=$Zone --project=$ProjectId --quiet
    Write-Host "Waiting 20 seconds for SSH availability..." -ForegroundColor Yellow
    Start-Sleep -Seconds 20
}

# Step 3: Ensure remote directory structure
Write-Host "[3/5] Setting up remote directory structure on $InstanceName..." -ForegroundColor Cyan
gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="sudo mkdir -p /opt/ether/logs/calibration /opt/ether/logs/gcp_batch /opt/ether/logs/spatial_runs /opt/ether/saves /opt/ether/data && sudo chmod -R 777 /opt/ether"

# Step 4: Upload shaded JAR & scripts
Write-Host "[4/5] Uploading shaded JAR and map presets to $InstanceName..." -ForegroundColor Cyan
gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="sudo mkdir -p /opt/ether/data/maps && sudo chmod -R 777 /opt/ether/data"
gcloud compute scp $JarPath "${InstanceName}:/opt/ether/society-simulation.jar" --zone=$Zone --project=$ProjectId --quiet
gcloud compute scp --recurse data/presets "${InstanceName}:/opt/ether/data/" --zone=$Zone --project=$ProjectId --quiet
if (Test-Path "data/events") {
    gcloud compute scp --recurse data/events "${InstanceName}:/opt/ether/data/" --zone=$Zone --project=$ProjectId --quiet
}
if (Test-Path "data/history") {
    gcloud compute scp --recurse data/history "${InstanceName}:/opt/ether/data/" --zone=$Zone --project=$ProjectId --quiet
}
if (Test-Path "data/maps/ether") {
    Write-Host "Uploading raster maps tensors (data/maps/ether)..." -ForegroundColor Cyan
    gcloud compute scp --recurse "data/maps/ether" "${InstanceName}:/opt/ether/data/maps/" --zone=$Zone --project=$ProjectId --quiet
}
gcloud compute scp "scripts/gcp/run-true-27-spatial-scenarios.sh" "${InstanceName}:/opt/ether/run_scenarios.sh" --zone=$Zone --project=$ProjectId --quiet

# Step 5: Launch detached background execution
Write-Host "[5/5] Launching background execution with Auto-Shutdown Guard..." -ForegroundColor Green
$remoteLaunch = "chmod +x /opt/ether/run_scenarios.sh && nohup /opt/ether/run_scenarios.sh > /opt/ether/logs/gcp_batch/overnight.log 2>&1 &"
gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="$remoteLaunch"

Write-Host "================================================================================" -ForegroundColor Green
Write-Host " [SUCCESS] OVERNIGHT BATCH CAMPAIGN IS RUNNING AUTONOMOUSLY ON GOOGLE CLOUD!     " -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Green
Write-Host "  You can now turn off your local PC safely." -ForegroundColor White
Write-Host "  The cloud VM will compute all trajectories, save snapshots, and automatically shut down." -ForegroundColor White
Write-Host "  Tomorrow, simply run to retrieve all results:" -ForegroundColor Yellow
Write-Host "    .\scripts\gcp\fetch-results.ps1" -ForegroundColor Yellow
Write-Host "================================================================================" -ForegroundColor Green
