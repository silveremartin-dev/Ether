# ==============================================================================
# Ether 1.0 — Automated GCP Batch Spot Execution Suite
# Runs Resolution 4 and Resolution 5 Scenarios in Low-Cost Spot Batch Mode
# Writes all snapshots directly to PostGIS and saves benchmark metrics.
# ==============================================================================
param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [string]$MasterVm = "ether-master",
    [string]$WorkerVm = "ether-worker",
    [string]$Scenarios = "INDUSTRIAL,MODERN",
    [string]$Resolutions = "4,5",
    [int]$Ticks = 24,
    [switch]$AutoShutdownWhenDone
)

$ErrorActionPreference = "Continue"

Write-Host "=========================================================="
Write-Host "     ETHER -- GCP BATCH SPOT SIMULATION ENGINE            "
Write-Host "=========================================================="
Write-Host "  Project ID      : $ProjectId"
Write-Host "  Zone            : $Zone"
Write-Host "  Master Instance : $MasterVm"
Write-Host "  Worker Instance : $WorkerVm"
Write-Host "  Scenarios       : $Scenarios"
Write-Host "  Resolutions     : $Resolutions"
Write-Host "  Target Ticks    : $Ticks"
Write-Host "  Auto-Shutdown   : $AutoShutdownWhenDone"
Write-Host "=========================================================="

# 1. Upload updated JAR and discrete maps to master and worker
Write-Host "`n[1/3] Synchronizing updated executable JAR and discrete cartography to GCP..."
$jarPath = "target/society-simulation-1.0.0-beta.1-executable.jar"
if (-not (Test-Path $jarPath)) {
    Write-Error "Executable JAR not found at $jarPath. Please build first."
    exit 1
}

# Ensure destination directories exist
gcloud compute ssh $MasterVm --project=$ProjectId --zone=$Zone --command="mkdir -p ~/target ~/data/maps/ether/earth/1800 ~/logs/benchmarks" --quiet
gcloud compute ssh $WorkerVm --project=$ProjectId --zone=$Zone --command="mkdir -p ~/target" --quiet

gcloud compute scp $jarPath "${MasterVm}:target/society-simulation-1.0.0-beta.1-executable.jar" --project=$ProjectId --zone=$Zone --quiet
gcloud compute scp $jarPath "${WorkerVm}:target/society-simulation-1.0.0-beta.1-executable.jar" --project=$ProjectId --zone=$Zone --quiet

# Upload updated 1800 maps
gcloud compute scp --recurse data/maps/ether/earth/1800/* "${MasterVm}:data/maps/ether/earth/1800/" --project=$ProjectId --zone=$Zone --quiet

# 2. Build the remote execution batch script
$scenariosListFormatted = ($Scenarios.Split(",") | ForEach-Object { "`"$_`"" }) -join " "
$resolutionsListFormatted = ($Resolutions.Split(",") | ForEach-Object { "$_" }) -join " "

$remoteScript = @"
#!/bin/bash
mkdir -p logs/benchmarks
echo "=== ETHER CLOUD BATCH SPOT JOB STARTED: `$(date) ===" > logs/benchmarks/batch_spot_execution.log

SCENARIOS=($scenariosListFormatted)
RESOLUTIONS=($resolutionsListFormatted)
TICKS=$Ticks

for sc in "`\${SCENARIOS[@]}"; do
    for res in "`\${RESOLUTIONS[@]}"; do
        echo "----------------------------------------------------------" >> logs/benchmarks/batch_spot_execution.log
        echo ">>> RUNNING SCENARIO: `$sc | RES: `$res | TICKS: `$TICKS <<<" >> logs/benchmarks/batch_spot_execution.log
        
        HEAP="10g"
        if [ "`$res" -ge 5 ]; then
            HEAP="28g"
        fi
        
        java -Xms4g -Xmx`$HEAP -XX:+UseG1GC --add-modules jdk.incubator.vector \
             -jar target/society-simulation-1.0.0-beta.1-executable.jar \
             --headless --mode=cluster --role=master --port=9090 \
             --secret=EtherClusterSecret2026 \
             --scenario="`$sc" --ticks=`$TICKS --cells=0 --res=`$res --profile \
             >> logs/benchmarks/batch_spot_execution.log 2>&1 || true
        
        echo ">>> FINISHED SCENARIO: `$sc | RES: `$res <<<" >> logs/benchmarks/batch_spot_execution.log
    done
done

echo "=== ETHER CLOUD BATCH SPOT JOB COMPLETED: `$(date) ===" >> logs/benchmarks/batch_spot_execution.log
"@

# Upload remote runner script to master
$scriptFile = "scripts/gcp/_remote_batch_runner.sh"
[System.IO.File]::WriteAllText($scriptFile, $remoteScript.Replace("`r`n", "`n"))
gcloud compute scp $scriptFile "${MasterVm}:run_batch_spot.sh" --project=$ProjectId --zone=$Zone --quiet
Remove-Item $scriptFile -Force -ErrorAction SilentlyContinue

# 3. Launch the worker and master in background daemon mode (tmux/nohup)
Write-Host "`n[2/3] Launching distributed worker and master batch runners in background..."

# Start worker listener in background nohup
gcloud compute ssh $WorkerVm --project=$ProjectId --zone=$Zone --command="nohup java -Xms4g -Xmx28g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar target/society-simulation-1.0.0-beta.1-executable.jar --headless --mode=cluster --role=worker --master-host=10.132.0.3 --port=9090 --secret=EtherClusterSecret2026 > worker_batch.log 2>&1 &" --quiet

# Start master batch job in background nohup
gcloud compute ssh $MasterVm --project=$ProjectId --zone=$Zone --command="chmod +x run_batch_spot.sh && nohup ./run_batch_spot.sh > master_batch_stdout.log 2>&1 &" --quiet

Write-Host "`n[3/3] Cloud Batch Spot Job successfully detached and running!"
Write-Host "=========================================================="
Write-Host "  The simulation is now executing entirely in the cloud.   "
Write-Host "  You can safely power off your local computer.            "
Write-Host "  All data and snapshots are saved to Cloud PostGIS DB.   "
Write-Host "  To fetch results tomorrow morning:                       "
Write-Host "    powershell -File .\scripts\gcp\fetch-results.ps1       "
Write-Host "=========================================================="
