# ==============================================================================
# Ether — Submit a Google Cloud Batch Simulation Job (Windows PowerShell)
#
# Usage:
#   .\scripts\gcp\batch\submit-batch-job.ps1 `
#       [-Scenario <name>] [-Ticks <n>] [-Cells <n>] [-Workers <n>] `
#       [-MachineType <type>] [-UseSpot <bool>] [-ProjectId <id>] `
#       [-Region <region>] [-ImageTag <tag>]
# ==============================================================================

param(
    [string]$Scenario    = "OUT_OF_AFRICA",
    [int]   $Ticks       = 1000,
    [int]   $Cells       = 5000,
    [int]   $Workers     = 1,
    [string]$MachineType = "e2-standard-4",
    [bool]  $UseSpot     = $true,
    [string]$ProjectId   = "ether-509812",
    [string]$Region      = "europe-west1",
    [string]$ImageTag    = "latest"
)

$ErrorActionPreference = 'Stop'

$GcsBucket   = "ether-simulations"
$Image       = "${Region}-docker.pkg.dev/${ProjectId}/ether-registry/ether-engine:${ImageTag}"
$SaEmail     = "ether-batch-runner@${ProjectId}.iam.gserviceaccount.com"

# Generate unique job name (lowercase, alphanumeric + hyphens only)
$ScenarioSlug = $Scenario.ToLower() -replace '[^a-z0-9]', '-'
$ScenarioSlug = $ScenarioSlug.Substring(0, [Math]::Min(20, $ScenarioSlug.Length))
$Timestamp    = (Get-Date -Format "yyyyMMdd-HHmmss")
$JobName      = "ether-${ScenarioSlug}-${Timestamp}"

$ProvisioningModel = if ($UseSpot) { "SPOT" } else { "STANDARD" }

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     ETHER — SUBMITTING CLOUD BATCH JOB" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Job Name     : $JobName"
Write-Host "  Scenario     : $Scenario"
Write-Host "  Ticks        : $Ticks"
Write-Host "  Cells        : $Cells"
Write-Host "  Task replicas: $Workers"
Write-Host "  Machine Type : $MachineType"
Write-Host "  Spot/Preempt : $UseSpot"
Write-Host "  Image        : $Image"
Write-Host "  Results GCS  : gs://$GcsBucket/$JobName/"
Write-Host "----------------------------------------------------------"

# ── Build Cloud Batch job JSON spec ──────────────────────────────────────────
$jobSpec = @"
{
  "taskGroups": [
    {
      "taskSpec": {
        "runnables": [
          {
            "container": {
              "imageUri": "$Image",
              "entrypoint": "/bin/sh",
              "commands": ["-c",
                "java -Xms2g -Xmx10g -XX:+UseG1GC --add-modules jdk.incubator.vector -jar ether.jar --headless --scenario=\${SCENARIO} --ticks=\${TICKS} --cells=\${CELLS} --profile && gsutil -m rsync -r /app/saves/ gs://$GcsBucket/$JobName/task-\${BATCH_TASK_INDEX}/"
              ]
            },
            "environment": {
              "variables": {
                "MODE": "headless",
                "SCENARIO": "$Scenario",
                "TICKS": "$Ticks",
                "CELLS": "$Cells"
              }
            }
          }
        ],
        "computeResource": {
          "cpuMilli": 4000,
          "memoryMib": 12288
        },
        "maxRetryCount": 1,
        "maxRunDuration": "86400s"
      },
      "taskCount": $Workers,
      "parallelism": $Workers
    }
  ],
  "allocationPolicy": {
    "instances": [
      {
        "policy": {
          "machineType": "$MachineType",
          "provisioningModel": "$ProvisioningModel"
        }
      }
    ],
    "serviceAccount": { "email": "$SaEmail" },
    "location": { "allowedLocations": ["regions/$Region"] }
  },
  "logsPolicy": { "destination": "CLOUD_LOGGING" }
}
"@

# ── Write spec to temp file and submit ───────────────────────────────────────
Write-Host "[1/1] Submitting job '$JobName' to Cloud Batch..."
$tmpSpec = [System.IO.Path]::GetTempFileName() + ".json"
$jobSpec | Set-Content $tmpSpec -Encoding utf8

try {
    gcloud batch jobs submit $JobName `
        --project=$ProjectId `
        --location=$Region `
        --config=$tmpSpec
} finally {
    Remove-Item $tmpSpec -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "✅ Job submitted!" -ForegroundColor Green
Write-Host ""
Write-Host "  Job Name : $JobName"
Write-Host "  Results  : gs://$GcsBucket/$JobName/"
Write-Host ""
Write-Host "Monitor progress:"
Write-Host "  .\scripts\gcp\batch\monitor-batch-job.ps1 -JobName $JobName"
Write-Host ""
Write-Host "Fetch results when complete:"
Write-Host "  .\scripts\gcp\batch\fetch-batch-results.ps1 -JobName $JobName"
Write-Host ""
Write-Host "GCP Console:"
Write-Host "  https://console.cloud.google.com/batch/jobs/${JobName}?project=${ProjectId}"
Write-Host "==========================================================" -ForegroundColor Green
