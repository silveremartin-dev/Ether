# ==============================================================================
# Ether 1.0 — Monitor 9-Scenario Historical Campaign on GCP
# ==============================================================================
param (
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [string]$InstanceName = "ether-master",
    [switch]$Follow = $false
)

$ErrorActionPreference = "Continue"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " [ETHER] MONITORING 9-SCENARIO HISTORICAL BENCHMARK CAMPAIGN ON GCP           " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# Check VM status
$status = (gcloud compute instances describe $InstanceName --zone=$Zone --project=$ProjectId --format="get(status)" 2>$null).Trim()
Write-Host "Instance Status : $status" -ForegroundColor ($status -eq "RUNNING" ? "Green" : "Yellow")

if ($status -ne "RUNNING") {
    Write-Host "VM is not running. It may have completed with auto-shutdown." -ForegroundColor White
    exit 0
}

# Check if Java/Maven test process is active
$psOut = (gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="ps aux | grep -E '(surefirebooter|MacroHistoricalEpochs)' | grep -v grep" 2>$null)
if ($psOut) {
    Write-Host "Benchmark Process: RUNNING" -ForegroundColor Green
    Write-Host $psOut -ForegroundColor Gray
} else {
    Write-Host "Benchmark Process: COMPLETED or NOT RUNNING" -ForegroundColor Yellow
}

# Show last 25 lines of log
Write-Host "`n--- Recent Log Lines (/home/silve/campaign_9_scenarios.log) ---" -ForegroundColor Cyan
gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="tail -n 25 /home/silve/campaign_9_scenarios.log 2>/dev/null || echo 'No log file found yet.'"

if ($Follow) {
    Write-Host "`nStreaming log (Ctrl+C to stop)..." -ForegroundColor Cyan
    gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --command="tail -f /home/silve/campaign_9_scenarios.log"
}
