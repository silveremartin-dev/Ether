# ==============================================================================
# ETHER MASTER OVERNIGHT BATCH CAMPAIGN ORCHESTRATOR (GCP SPOT / RES 5)
# ==============================================================================
# Full 4-Axis Planetary Cliodynamics, 27 Bifurcations, and 9-Epoch Baseline Suite
# Resolution: Uber H3 Resolution 5 (2,016,842 hexagons, 8.5 km cell spacing)
# Uniform Time Step: dt = 7.0 days (52.14 ticks/yr) across all scenarios
# ==============================================================================

param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [string]$InstanceName = "ether-master",
    [int]$H3Resolution = 5,
    [double]$DtDays = 7.0,
    [string]$JarPath = "target/society-simulation-1.0.0-beta.1-executable.jar"
)

$ErrorActionPreference = "Continue"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " [ETHER ENGINE] MASTER OVERNIGHT HIGH-FIDELITY BATCH CAMPAIGN (GCP SPOT)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  Project ID         : $ProjectId"
Write-Host "  Zone               : $Zone"
Write-Host "  Instance Target    : $InstanceName"
Write-Host "  Spatial Resolution : Uber H3 Resolution $H3Resolution (2,016,842 cells, ~8.5 km)"
Write-Host "  Uniform Time-Step  : dt = $DtDays days (52.14 ticks/year)"
Write-Host "  Target Suite       : 9 Calibrated Epochs + 27 Bifurcations + 25 Engine Ablations"
Write-Host "--------------------------------------------------------------------------------"

# Step 1: Ensure JAR is built
if (!(Test-Path $JarPath)) {
    Write-Host "Executable shaded JAR not found at $JarPath. Building now..." -ForegroundColor Yellow
    mvn clean package -DskipTests
}

# Step 2: Start the GCP VM
Write-Host "`n[1/5] Starting GCP Compute Instance ($InstanceName)..." -ForegroundColor Yellow
gcloud compute instances start $InstanceName --zone=$Zone --project=$ProjectId
Start-Sleep -Seconds 15

# Step 3: Ensure remote working directory exists
Write-Host "`n[2/5] Creating remote working directory structure on $InstanceName..." -ForegroundColor Yellow
$setupCmd = "sudo mkdir -p /opt/ether/logs/calibration /opt/ether/logs/gcp_batch /opt/ether/saves /opt/ether/data && sudo chmod -R 777 /opt/ether"
gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="$setupCmd"

# Step 4: Upload shaded JAR and runner scripts
Write-Host "`n[3/5] Uploading executable shaded JAR ($JarPath) to $InstanceName..." -ForegroundColor Yellow
gcloud compute scp $JarPath "$($InstanceName):/opt/ether/society-simulation.jar" --zone=$Zone --project=$ProjectId --quiet

# Step 5: Upload map assets if needed
Write-Host "`n[4/5] Synchronizing planetary map assets to $InstanceName..." -ForegroundColor Yellow
if (Test-Path "data/maps") {
    gcloud compute scp --recurse data/maps "$($InstanceName):/opt/ether/data/" --zone=$Zone --project=$ProjectId --quiet
}

# Step 6: Create and launch remote batch execution script with Auto-Shutdown Guard
Write-Host "`n[5/5] Launching High-Fidelity Overnight Execution Pipeline on $InstanceName..." -ForegroundColor Green

$remoteScript = @"
#!/bin/bash
set -e
echo '==============================================================================='
echo '   ETHER PLANETARY SIMULATION ENGINE: OVERNIGHT HIGH-FIDELITY BATCH RUNNER   '
echo '==============================================================================='
echo 'Timestamp: '\$(date -u)
echo 'Working Directory: /opt/ether'
echo 'Spatial Resolution: H3 Res $H3Resolution (2,016,842 cells)'
echo 'Uniform Time Step: dt = $DtDays days'
echo '==============================================================================='

cd /opt/ether

# Run the full 4-Axis & 27 Bifurcation Master Suite
echo '>>> Launching Full Master 4-Axis Suite and 27 Bifurcation Campaign...'
java -Xms8g -Xmx50g \
     --add-modules=jdk.incubator.vector \
     -cp society-simulation.jar \
     org.ether.society.analytics.HistoricalScenarioCalibrationHarness \
     --resolution $H3Resolution \
     --dt $DtDays \
     2>&1 | tee /opt/ether/logs/gcp_batch/overnight_execution.log

echo '>>> Packaging Results Archive...'
tar -czf /opt/ether/logs/gcp_batch/master_results_archive_\$(date +%Y%m%d_%H%M%S).tar.gz \
    /opt/ether/logs/calibration/ \
    /opt/ether/logs/gcp_batch/overnight_execution.log

echo '>>> Computation Complete at: '\$(date -u)
echo '>>> Triggering Immediate Auto-Shutdown Guard (\$0.00 Idle Cost)...'
sudo shutdown -h now
"@

# Write remote script and launch in background via nohup
$scriptBytes = [System.Text.Encoding]::UTF8.GetBytes($remoteScript)
$scriptBase64 = [Convert]::ToBase64String($scriptBytes)

$launchCmd = "echo '$scriptBase64' | base64 -d > /opt/ether/run_batch.sh && chmod +x /opt/ether/run_batch.sh && nohup /opt/ether/run_batch.sh > /opt/ether/logs/gcp_batch/batch_launcher.log 2>&1 &"
gcloud compute ssh $InstanceName --zone=$Zone --project=$ProjectId --quiet --command="$launchCmd"

Write-Host "--------------------------------------------------------------------------------" -ForegroundColor Cyan
Write-Host " [SUCCESS] OVERNIGHT BATCH CAMPAIGN DEPLOYED AND RUNNING IN BACKGROUND!" -ForegroundColor Green
Write-Host "--------------------------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "  Remote Log      : /opt/ether/logs/gcp_batch/overnight_execution.log"
Write-Host "  Results Output  : /opt/ether/logs/calibration/master_calibration_and_falsification_results.json"
Write-Host "  Academic Report : /opt/ether/logs/calibration/calibration_and_falsification_academic_report.md"
Write-Host "  Auto-Shutdown   : ACTIVE (VM will power off immediately upon completion for zero idle cost)"
Write-Host "================================================================================" -ForegroundColor Cyan
