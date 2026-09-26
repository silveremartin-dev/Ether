# ==============================================================================
# Ether 1.0 — Automated Multi-Scenario x Multi-Resolution Benchmark Suite
# Measures steady-state TPS, per-node throughput, and memory scaling.
# ==============================================================================
param(
    [string]$ProjectId = "ether-509812",
    [string]$Zone = "europe-west1-b",
    [int[]]$Resolutions = @(2, 3, 4),
    [string[]]$Scenarios = @("OUT_OF_AFRICA", "NEOLITHIZATION", "CLASSICAL", "INDUSTRIAL", "MODERN"),
    [int]$Ticks = 24,
    [switch]$ClusterMode = $true,
    [string]$OutputFile = "logs/benchmarks/matrix_benchmark_results.json"
)

$ErrorActionPreference = "Stop"

Write-Host "=========================================================="
Write-Host "     ETHER — MULTI-DIMENSIONAL BENCHMARK SUITE MATRIX     "
Write-Host "=========================================================="
Write-Host "  Project ID    : $ProjectId"
Write-Host "  Zone          : $Zone"
Write-Host "  Ticks per Run : $Ticks"
Write-Host "  Scenarios     : $($Scenarios -join ', ')"
Write-Host "  Resolutions   : $($Resolutions -join ', ')"
Write-Host "  Cluster Mode  : $ClusterMode"
Write-Host "----------------------------------------------------------"

$results = @()

# Ensure output directory exists
$outputDir = [System.IO.Path]::GetDirectoryName($OutputFile)
if (-not [string]::IsNullOrEmpty($outputDir) -and -not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

foreach ($scenario in $Scenarios) {
    foreach ($res in $Resolutions) {
        Write-Host "`n>>> [BENCHMARK] Scenario: $scenario | H3 Resolution: $res | Ticks: $Ticks <<<"
        
        $startTime = Get-Date
        $output = & .\scripts\gcp\deploy-and-run.ps1 -Scenario "$scenario" -Ticks $Ticks -Cells 0 -Resolution $res -ClusterMode:$ClusterMode -SkipBuild 2>&1 | Out-String
        $duration = ((Get-Date) - $startTime).TotalSeconds

        # Extract TPS and Cell count from output
        $tps = 0.0
        if ($output -match "Throughput:\s+([0-9\.]+)\s+TPS") {
            $tps = [double]$Matches[1]
        }
        $cellCount = 2 + 120 * [Math]::Pow(7, $res)
        if ($output -match "Initializing grid with ([0-9]+) cells") {
            $cellCount = [int]$Matches[1]
        }
        $cohortCount = 0
        if ($output -match "Initialized ([0-9]+) demographic agent cohort nodes") {
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

        Write-Host ">>> Result: $tps TPS | Duration: $($entry.TotalDurationSeconds)s | Cohorts: $cohortCount <<<"
    }
}

$results | ConvertTo-Json -Depth 4 | Set-Content -Path $OutputFile -Encoding UTF8
Write-Host "`n=========================================================="
Write-Host " Benchmark Suite Complete. Results saved to $OutputFile"
Write-Host "=========================================================="
