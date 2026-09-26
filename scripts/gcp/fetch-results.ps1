param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [string]$LocalSavesDir = "saves"
)

$ErrorActionPreference = "Stop"
Remove-Item Env:\CLOUDSDK_CORE_PROJECT -ErrorAction SilentlyContinue

Write-Host "=========================================================="
Write-Host "       ETHER - FETCH SIMULATION RESULTS FROM GCP          "
Write-Host "=========================================================="
Write-Host "  Project ID    : $ProjectId"
Write-Host "  Zone          : $Zone"
Write-Host "  Destination   : $LocalSavesDir"
Write-Host "----------------------------------------------------------"

if (-not (Test-Path $LocalSavesDir)) {
    New-Item -ItemType Directory -Path $LocalSavesDir -Force | Out-Null
}

Write-Host "Downloading saves and logs from ether-master..."
gcloud compute scp --recurse --zone=$Zone --project=$ProjectId --quiet ether-master:/opt/ether/saves .
gcloud compute scp --recurse --zone=$Zone --project=$ProjectId --quiet ether-master:/opt/ether/logs .

Write-Host "Saves and logs synchronized to local workspace."
Write-Host "Launch Ether in local mode (run.bat or Ether_Windows.bat) to view the replay."
