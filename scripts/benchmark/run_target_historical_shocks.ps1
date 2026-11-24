# Target Historical Shocks Falsification Benchmark Runner
param(
    [string]$Phases = "1,2,3"
)

$ErrorActionPreference = "Continue"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "🚀 LAUNCHING TARGET HISTORICAL SHOCKS FALSIFICATION SUITE" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# Phase 1: Late Antique Ice Age / Justinian (536 CE), Industrial / Tambora (1800 CE)
if ($Phases -match "1") {
    Write-Host ">>> [Phase 1/3] Running Late Antique 536 & Industrial 1800..." -ForegroundColor Yellow
    mvn test -Pacademic-paper -Dtest=MacroHistoricalEpochsFalsificationTest "-Dphase1.scenario=late_antique_ice_age,industrial_1800" -DtestFailureIgnore=true
}

# Phase 2: Bronze Age Collapse (-1200 BCE), Mongol / Samalas (-1206 CE)
if ($Phases -match "2") {
    Write-Host ">>> [Phase 2/3] Running Bronze Age Collapse & Mongol Samalas..." -ForegroundColor Yellow
    mvn test -Pacademic-paper -Dtest=MacroHistoricalPhase2FalsificationTest "-Dphase2.scenario=bronze_age_collapse_1200bc,mongol_conquest_1206" -DtestFailureIgnore=true
}

# Phase 3: Toba Cataclysm (-74,000 BP), Younger Dryas (-10,900 BP)
if ($Phases -match "3") {
    Write-Host ">>> [Phase 3/3] Running Toba Cataclysm & Younger Dryas..." -ForegroundColor Yellow
    mvn test -Pacademic-paper -Dtest=MacroHistoricalPhase3FalsificationTest "-Dphase3.scenario=toba_cataclysm_74k,younger_dryas" -DtestFailureIgnore=true
}

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "✔ TARGET HISTORICAL SHOCKS BENCHMARK RUNS COMPLETED" -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Cyan
