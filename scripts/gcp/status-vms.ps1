param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b"
)

Remove-Item Env:\CLOUDSDK_CORE_PROJECT -ErrorAction SilentlyContinue

Write-Host "=========================================================="
Write-Host "          ETHER - GCP SIMULATION CLUSTER STATUS           "
Write-Host "=========================================================="
Write-Host "  Project ID : $ProjectId"
Write-Host "  Zone       : $Zone"
Write-Host "----------------------------------------------------------"

gcloud compute instances list --project=$ProjectId --zone=$Zone
