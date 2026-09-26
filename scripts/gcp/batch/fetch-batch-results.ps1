# ==============================================================================
# Ether — Fetch Batch Simulation Results from GCS (Windows PowerShell)
#
# Usage:
#   .\scripts\gcp\batch\fetch-batch-results.ps1 -JobName <name> [-LocalDir <path>] [-ProjectId <id>]
# ==============================================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$JobName,
    [string]$LocalDir  = ".\saves\batch\$JobName",
    [string]$ProjectId = "ether-509812"
)

$ErrorActionPreference = 'Stop'
$GcsPath = "gs://ether-simulations/$JobName/"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER — FETCH BATCH RESULTS FROM GCS" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Job Name   : $JobName"
Write-Host "  GCS Source : $GcsPath"
Write-Host "  Local Dest : $LocalDir"
Write-Host "----------------------------------------------------------"

New-Item -ItemType Directory -Force -Path $LocalDir | Out-Null

Write-Host "Downloading results..."
gsutil -m rsync -r $GcsPath "$LocalDir\"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "✅ Results downloaded to: $LocalDir" -ForegroundColor Green
Get-ChildItem $LocalDir -ErrorAction SilentlyContinue | Format-Table Name, Length, LastWriteTime
Write-Host ""
Write-Host "To replay locally:"
Write-Host "  .\scripts\docker-deploy.ps1 -Mode dev"
Write-Host "  # Then open Ether UI and load saves from $LocalDir"
Write-Host "==========================================================" -ForegroundColor Green
