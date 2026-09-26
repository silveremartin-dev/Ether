param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b"
)

$ErrorActionPreference = "Stop"
Remove-Item Env:\CLOUDSDK_CORE_PROJECT -ErrorAction SilentlyContinue

Write-Host "=========================================================="
Write-Host "          ETHER - STOPPING GCP SIMULATION VMs             "
Write-Host "=========================================================="
Write-Host "  Project ID : $ProjectId"
Write-Host "  Zone       : $Zone"
Write-Host "----------------------------------------------------------"
Write-Host "Stopping 'ether-master' and 'ether-worker'..."
Write-Host "NOTE: All vCPU/RAM billing stops immediately."
Write-Host "Disks and data (PostgreSQL database, savegames) are preserved."
Write-Host ""

gcloud compute instances stop ether-master ether-worker --zone=$Zone --project=$ProjectId --quiet

Write-Host ""
Write-Host "✅ All simulation VMs are STOPPED."
Write-Host "Run .\scripts\gcp\start-vms.ps1 to power them back on anytime."
