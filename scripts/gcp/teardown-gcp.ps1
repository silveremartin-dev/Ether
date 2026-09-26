param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [ValidateSet("stop", "start", "delete", "status")]
    [string]$Action = "status"
)

Remove-Item Env:\CLOUDSDK_CORE_PROJECT -ErrorAction SilentlyContinue

Write-Host "=========================================================="
Write-Host "       ETHER - GCP INSTANCE MANAGEMENT AND COST           "
Write-Host "=========================================================="
Write-Host "  Project ID : $ProjectId"
Write-Host "  Action     : $Action"
Write-Host "  Zone       : $Zone"
Write-Host "----------------------------------------------------------"

switch ($Action) {
    "status" {
        gcloud compute instances list --project=$ProjectId
    }
    "stop" {
        Write-Host "Stopping VMs (billing stops for vCPU/RAM)..."
        gcloud compute instances stop ether-master ether-worker --zone=$Zone --project=$ProjectId --quiet
        Write-Host "Instances stopped."
    }
    "start" {
        Write-Host "Restarting VMs..."
        gcloud compute instances start ether-master ether-worker --zone=$Zone --project=$ProjectId --quiet
        Write-Host "Instances started."
    }
    "delete" {
        Write-Host "Deleting VMs..."
        gcloud compute instances delete ether-master ether-worker --zone=$Zone --project=$ProjectId --quiet
        Write-Host "Instances deleted."
    }
}
