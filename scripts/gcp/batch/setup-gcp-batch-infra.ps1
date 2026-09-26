# ==============================================================================
# Ether — Google Cloud Batch: One-Time Infrastructure Setup (Windows PowerShell)
#
# Creates all GCP resources required to run Ether simulations via Cloud Batch.
# This script is idempotent — safe to run multiple times.
#
# Usage:
#   .\scripts\gcp\batch\setup-gcp-batch-infra.ps1 [-ProjectId <id>] [-Region <region>]
# ==============================================================================

param(
    [string]$ProjectId      = "ether-509812",
    [string]$Region         = "europe-west1",
    [string]$RegistryName   = "ether-registry",
    [string]$GcsBucket      = "ether-simulations",
    [string]$SaName         = "ether-batch-runner"
)

$ErrorActionPreference = 'Stop'
$SaEmail = "${SaName}@${ProjectId}.iam.gserviceaccount.com"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER — GCP BATCH INFRASTRUCTURE SETUP" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Project ID   : $ProjectId"
Write-Host "  Region       : $Region"
Write-Host "  Registry     : $RegistryName"
Write-Host "  GCS Bucket   : gs://$GcsBucket"
Write-Host "  Service Acct : $SaEmail"
Write-Host "----------------------------------------------------------"

# ── Step 1: Enable required APIs ─────────────────────────────────────────────
Write-Host "[1/4] Enabling GCP APIs..."
gcloud services enable `
    batch.googleapis.com `
    artifactregistry.googleapis.com `
    storage.googleapis.com `
    logging.googleapis.com `
    --project=$ProjectId
Write-Host "      APIs enabled."

# ── Step 2: Create Artifact Registry repository ───────────────────────────────
Write-Host "[2/4] Creating Artifact Registry repository '$RegistryName'..."
$repoExists = $null
try {
    gcloud artifacts repositories describe $RegistryName `
        --location=$Region --project=$ProjectId 2>$null | Out-Null
    $repoExists = $true
} catch { $repoExists = $false }

if (-not $repoExists) {
    gcloud artifacts repositories create $RegistryName `
        --repository-format=docker `
        --location=$Region `
        --project=$ProjectId `
        --description="Ether simulation engine Docker images"
    Write-Host "      Repository created."
} else {
    Write-Host "      Repository already exists — skipping."
}

# ── Step 3: Create GCS bucket for simulation results ─────────────────────────
Write-Host "[3/4] Creating GCS bucket 'gs://$GcsBucket'..."
$bucketExists = $null
try {
    gsutil ls -b "gs://$GcsBucket" 2>$null | Out-Null
    $bucketExists = $true
} catch { $bucketExists = $false }

if (-not $bucketExists) {
    gsutil mb -p $ProjectId -l $Region -b on "gs://$GcsBucket"

    # 90-day lifecycle rule to auto-delete old results (cost saving)
    $lifecycle = @'
{"lifecycle":{"rule":[{"action":{"type":"Delete"},"condition":{"age":90}}]}}
'@
    $tmpFile = [System.IO.Path]::GetTempFileName()
    $lifecycle | Set-Content $tmpFile -Encoding utf8
    gsutil lifecycle set $tmpFile "gs://$GcsBucket"
    Remove-Item $tmpFile

    Write-Host "      Bucket created with 90-day auto-delete lifecycle."
} else {
    Write-Host "      Bucket already exists — skipping."
}

# ── Step 4: Create service account ───────────────────────────────────────────
Write-Host "[4/4] Creating service account '$SaName'..."
$saExists = $null
try {
    gcloud iam service-accounts describe $SaEmail --project=$ProjectId 2>$null | Out-Null
    $saExists = $true
} catch { $saExists = $false }

if (-not $saExists) {
    gcloud iam service-accounts create $SaName `
        --project=$ProjectId `
        --display-name="Ether Batch Runner" `
        --description="Service account for Ether Cloud Batch simulation jobs"
    Write-Host "      Service account created."
} else {
    Write-Host "      Service account already exists — updating roles."
}

$roles = @(
    "roles/batch.jobsEditor",
    "roles/storage.objectAdmin",
    "roles/artifactregistry.reader",
    "roles/logging.logWriter"
)
foreach ($role in $roles) {
    gcloud projects add-iam-policy-binding $ProjectId `
        --member="serviceAccount:$SaEmail" `
        --role=$role --quiet | Out-Null
}
Write-Host "      Roles assigned."

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "✅ GCP Batch infrastructure is ready!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Build & push the Docker image:"
Write-Host "       .\scripts\gcp\batch\build-and-push.ps1 -ProjectId $ProjectId -Region $Region"
Write-Host ""
Write-Host "  2. Submit a simulation job:"
Write-Host "       .\scripts\gcp\batch\submit-batch-job.ps1 -Scenario OUT_OF_AFRICA -Ticks 1000 -Cells 5000"
Write-Host "==========================================================" -ForegroundColor Green
