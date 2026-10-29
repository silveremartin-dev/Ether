# ==============================================================================
# Ether 1.0 - Standardized Cliodynamic Scenario Falsification Report
# Generates the epistemic falsification report from snapshots in saves/
# ==============================================================================

param (
    [string]$Runs = "RUN-LA-PESTE-NOIRE---INVERSION--CONOMIQUE-POST-F-ODALE--1347--2282 RUN-GUERRES-MONDIALES--RUPTURE-TOTALITAIRE----RE-NUCL-AIRE--1914--5889 RUN-RAMS-S-II--PAX-AEGYPTIACA---B-TISSEURS-MONUMENTAUX---1279--3618",
    [string]$OutputDir = "scratch"
)

$ErrorActionPreference = "Stop"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " [ETHER] SCENARIO FALSIFICATION AUDIT GENERATOR (HYDE 3.4 / MADDISON / EPICA)   " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  Target Scenarios : $Runs"
Write-Host "  Output Directory : $OutputDir"
Write-Host "--------------------------------------------------------------------------------"

$sw = [System.Diagnostics.Stopwatch]::StartNew()

mvn test "-Dtest=ScenarioFalsificationReportTest" "-Druns=$Runs" "-Djacoco.skip=true"

$sw.Stop()
$elapsed = [math]::Round($sw.Elapsed.TotalSeconds, 2)

$reportPath = Join-Path $OutputDir "scenario_falsification_report.md"
if (Test-Path $reportPath) {
    Write-Host "================================================================================" -ForegroundColor Green
    Write-Host " [SUCCESS] FALSIFICATION REPORT GENERATED IN $elapsed SECONDS!" -ForegroundColor Green
    Write-Host "   Path: $reportPath" -ForegroundColor Green
    Write-Host "================================================================================" -ForegroundColor Green
} else {
    Write-Error "Report generation failed at $reportPath"
}
