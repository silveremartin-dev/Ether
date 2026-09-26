# ==============================================================================
# Ether — Monitor a Cloud Batch Job (Windows PowerShell)
#
# Usage:
#   .\scripts\gcp\batch\monitor-batch-job.ps1 -JobName <name> [-ProjectId <id>] [-Region <region>]
# ==============================================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$JobName,
    [string]$ProjectId = "ether-509812",
    [string]$Region    = "europe-west1"
)

$ErrorActionPreference = 'Stop'

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER — CLOUD BATCH JOB MONITOR" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Job Name   : $JobName"
Write-Host "  Project ID : $ProjectId"
Write-Host "  Region     : $Region"
Write-Host "----------------------------------------------------------"

Write-Host ""
Write-Host "── Job Status ────────────────────────────────────────────"
gcloud batch jobs describe $JobName `
    --project=$ProjectId `
    --location=$Region `
    --format="yaml(status,createTime,updateTime)"

Write-Host ""
Write-Host "── Recent Logs (last 50 lines) ───────────────────────────"
try {
    gcloud logging read `
        "resource.type=batch.googleapis.com/Job AND resource.labels.job_id=$JobName" `
        --project=$ProjectId `
        --limit=50 `
        --format="value(timestamp, textPayload)" `
        --order=asc
} catch {
    Write-Host "  (No logs yet — job may still be starting)"
}

Write-Host ""
Write-Host "GCP Console:"
Write-Host "  https://console.cloud.google.com/batch/jobs/${JobName}?project=${ProjectId}"
