param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b"
)

$ErrorActionPreference = "Stop"
Remove-Item Env:\CLOUDSDK_CORE_PROJECT -ErrorAction SilentlyContinue

Write-Host "=========================================================="
Write-Host "          ETHER - STARTING GCP SIMULATION VMs             "
Write-Host "=========================================================="
Write-Host "  Project ID : $ProjectId"
Write-Host "  Zone       : $Zone"
Write-Host "----------------------------------------------------------"
Write-Host "Starting 'ether-master' and 'ether-worker'..."

gcloud compute instances start ether-master ether-worker --zone=$Zone --project=$ProjectId --quiet

Write-Host ""
Write-Host "✅ All simulation VMs are RUNNING."
Write-Host "Instances:"
gcloud compute instances list --project=$ProjectId --zone=$Zone
