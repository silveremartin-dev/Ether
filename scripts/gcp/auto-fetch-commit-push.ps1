# ==============================================================================
# Ether 1.0 — Automated Pipeline: Wait for Completion, Fetch, Commit, Push
# ==============================================================================

param (
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [string]$InstanceName = "ether-master"
)

$ErrorActionPreference = "Stop"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " 🚀 ETHER ENGINE: AUTO-PIPELINE (MONITOR -> FETCH -> COMMIT -> PUSH)            " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# Step 1: Monitor remote simulation until finished
Write-Host "[1/4] Monitoring remote simulation on $InstanceName until complete..." -ForegroundColor Yellow
$isRunning = $true

while ($isRunning) {
    try {
        $status = (gcloud compute instances describe $InstanceName --zone=$Zone --project=$ProjectId --format="get(status)" 2>$null).Trim()
        if ($status -ne "RUNNING") {
            Write-Host "  ✔ Remote VM $InstanceName has finished and shut down." -ForegroundColor Green
            $isRunning = $false
            break
        }
        
        $javaProc = (gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="ps aux | grep java | grep -v grep | wc -l" 2>$null).Trim()
        if ($javaProc -eq "0") {
            Write-Host "  ✔ Simulation batch runner has finished on $InstanceName." -ForegroundColor Green
            $isRunning = $false
            break
        } else {
            $latestLog = (gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="tail -n 1 /opt/ether/logs/spatial_runs/*.log 2>/dev/null | tail -n 1" 2>$null)
            Write-Host "  ⏳ [$(Get-Date -Format 'HH:mm:ss')] Simulation still in progress... $latestLog" -ForegroundColor Cyan
            Start-Sleep -Seconds 30
        }
    } catch {
        Write-Host "  [!] Exception during polling: $_" -ForegroundColor DarkGray
        Start-Sleep -Seconds 20
    }
}

# Step 2: Fetch all results
Write-Host "`n[2/4] Fetching all simulation saves and snapshots from GCP..." -ForegroundColor Yellow
& ".\scripts\gcp\fetch-results.ps1" -ProjectId $ProjectId -Zone $Zone -InstanceName $InstanceName

# Step 3: Git stage and commit
Write-Host "`n[3/4] Staging and committing snapshots and scenario presets..." -ForegroundColor Yellow
git add saves/
git add data/presets/
git add src/main/resources/data/presets/
git add scripts/gcp/

$statusOutput = (git status -s)
if ($statusOutput) {
    git commit -m "feat(saves): sync high-fidelity historical snapshots from GCP batch execution"
    Write-Host "  ✔ Git commit created successfully." -ForegroundColor Green
} else {
    Write-Host "  ℹ No changes to commit." -ForegroundColor DarkGray
}

# Step 4: Push to remote repository
Write-Host "`n[4/4] Pushing changes to remote Git repository..." -ForegroundColor Yellow
git push

Write-Host "================================================================================" -ForegroundColor Green
Write-Host " ✅ PIPELINE COMPLETE: ALL SNAPSHOTS SYNCHRONIZED AND PUSHED TO GIT!            " -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Green
