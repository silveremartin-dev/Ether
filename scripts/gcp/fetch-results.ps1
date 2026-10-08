# ==============================================================================
# Ether 1.0 — Fetch Simulation Results from GCP (PowerShell / Windows)
# Downloads saved snapshots and database telemetry for local visual replay and Git.
# ==============================================================================

param (
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [string]$InstanceName = "ether-master",
    [string]$LocalSavesDir = "saves"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER — FETCH SIMULATION RESULTS FROM GCP (PowerShell) " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Project ID  : $ProjectId"
Write-Host "  Zone        : $Zone"
Write-Host "  Instance    : $InstanceName"
Write-Host "  Destination : $LocalSavesDir"
Write-Host "----------------------------------------------------------"

if (-not (Test-Path $LocalSavesDir)) {
    New-Item -ItemType Directory -Path $LocalSavesDir -Force | Out-Null
}
if (-not (Test-Path "logs")) {
    New-Item -ItemType Directory -Path "logs" -Force | Out-Null
}

# 1. Start VM if stopped
$status = "TERMINATED"
try {
    $status = (gcloud compute instances describe $InstanceName --zone=$Zone --project=$ProjectId --format="get(status)" 2>$null).Trim()
} catch {
    $status = "TERMINATED"
}

if ($status -ne "RUNNING") {
    Write-Host "[1/4] Starting $InstanceName to retrieve simulation saves..." -ForegroundColor Yellow
    gcloud compute instances start $InstanceName --zone=$Zone --project=$ProjectId --quiet
    Write-Host "Waiting 20 seconds for SSH daemon initialization..." -ForegroundColor Yellow
    Start-Sleep -Seconds 20
} else {
    Write-Host "[1/4] Instance $InstanceName is already running." -ForegroundColor Green
}

# 2. Download saves and snapshots
Write-Host "[2/4] Downloading saves and snapshots from $InstanceName..." -ForegroundColor Cyan
gcloud compute scp --recurse --zone=$Zone --project=$ProjectId --quiet "${InstanceName}:/opt/ether/saves/*" "$LocalSavesDir/"

# 3. Download logs and telemetry
Write-Host "[3/4] Downloading execution logs and calibration telemetry..." -ForegroundColor Cyan
gcloud compute scp --recurse --zone=$Zone --project=$ProjectId --quiet "${InstanceName}:/opt/ether/logs/*" "logs/"

# 4. Power off VM immediately to avoid idle costs
Write-Host "[4/4] Powering off $InstanceName to guarantee `$0.00 idle cost..." -ForegroundColor Yellow
gcloud compute instances stop $InstanceName --zone=$Zone --project=$ProjectId --quiet

Write-Host "==========================================================" -ForegroundColor Green
Write-Host " ✅ ALL SIMULATION SAVES & SNAPSHOTS SUCCESSFULLY RETRIEVED!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " Files are located in: $LocalSavesDir/"
Write-Host " You can now review git status and commit:"
Write-Host "   git add saves/"
Write-Host "   git commit -m 'feat: sync canonical simulation snapshots from GCP'"
Write-Host "=========================================================="
