# ==============================================================================
# Ether 1.0 — Automated Multi-Scenario x Multi-Resolution Benchmark Suite
# Measures steady-state TPS, per-node throughput, and memory scaling.
# ==============================================================================
param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [string]$ScenariosList = "OUT_OF_AFRICA,NEOLITHIZATION,CLASSICAL,INDUSTRIAL,MODERN",
    [string]$ResolutionsList = "2,3",
    [int]$Ticks = 24,
    [string]$OutputFile = "logs/benchmarks/matrix_benchmark_results.json"
)

$ErrorActionPreference = "Continue"

$Scenarios = $ScenariosList.Split(",")
$Resolutions = $ResolutionsList.Split(",") | ForEach-Object { [int]$_ }

Write-Host "=========================================================="
Write-Host "     ETHER -- MULTI-DIMENSIONAL BENCHMARK SUITE MATRIX     "
Write-Host "=========================================================="
Write-Host "  Project ID    : $ProjectId"
Write-Host "  Zone          : $Zone"
Write-Host "  Ticks per Run : $Ticks"
Write-Host "  Scenarios     : $($Scenarios -join ', ')"
Write-Host "  Resolutions   : $($Resolutions -join ', ')"
Write-Host "----------------------------------------------------------"

$results = @()

$outputDir = [System.IO.Path]::GetDirectoryName($OutputFile)
if (-not [string]::IsNullOrEmpty($outputDir) -and -not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

foreach ($scenario in $Scenarios) {
    foreach ($res in $Resolutions) {
        Write-Host "`n--- [BENCHMARK] Scenario: $scenario | H3 Res: $res | Ticks: $Ticks ---"
        
        $startTime = Get-Date
        $output = & powershell.exe -ExecutionPolicy Bypass -File .\scripts\gcp\deploy-and-run.ps1 -Scenario "$scenario" -Ticks $Ticks -Cells 0 -Resolution $res -ClusterMode -SkipBuild 2>&1 | Out-String
        $duration = ((Get-Date) - $startTime).TotalSeconds

        $tps = 0.0
        if ($output -match "Throughput:\s+(\d+(\.\d+)?)\s+TPS") {
            $tps = [double]$Matches[1]
        }
        $cellCount = [int](2 + 120 * [Math]::Pow(7, $res))
        if ($output -match "Initializing grid with (\d+) cells") {
            $cellCount = [int]$Matches[1]
        }
        $cohortCount = 0
        if ($output -match "Initialized (\d+) demographic agent cohort nodes") {
            $cohortCount = [int]$Matches[1]
        }

        $entry = [PSCustomObject]@{
            Scenario = $scenario
            H3Resolution = $res
            PlanetaryCells = $cellCount
            CohortAgents = $cohortCount
            TargetTicks = $Ticks
            TotalDurationSeconds = [Math]::Round($duration, 2)
            MeasuredTPS = $tps
            PerNodeThroughputCellsSec = [Math]::Round(($cellCount * $tps / 2.0), 1)
            Timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        }
        $results += $entry

        Write-Host "--- Result: $tps TPS | Duration: $($entry.TotalDurationSeconds)s | Cohorts: $cohortCount ---"
    }
}

$results | ConvertTo-Json -Depth 4 | Set-Content -Path $OutputFile -Encoding UTF8
Write-Host "`n=========================================================="
Write-Host " Benchmark Suite Complete. Results saved to $OutputFile"
Write-Host "=========================================================="
