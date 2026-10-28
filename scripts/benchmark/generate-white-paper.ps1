# ==============================================================================
# Ether 1.0 - Master White Paper Generator for 45 Cliodynamic Scenarios
# Compiles the comprehensive empirical audit report against historical ground truth.
# ==============================================================================

param (
    [string]$OutputDir = "docs",
    [switch]$SkipCompile = $false
)

$ErrorActionPreference = "Stop"

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " [ETHER] MASTER WHITE PAPER GENERATION: 45 SCENARIO AUDIT SUITE                 " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  Target Output  : $OutputDir/WHITE_PAPER_45_SCENARIOS_HISTORICAL_BENCHMARK.md"
Write-Host "  Execution Engine: Maven Surefire / WhitePaperCompiler"
Write-Host "--------------------------------------------------------------------------------"

$sw = [System.Diagnostics.Stopwatch]::StartNew()

# Run the automated White Paper Compiler harness
Write-Host "[1/2] Invoking WhitePaperCompiler test harness..." -ForegroundColor Yellow
mvn test "-Dtest=WhitePaperCompiler" "-Djacoco.skip=true"

$sw.Stop()
$elapsedSeconds = [math]::Round($sw.Elapsed.TotalSeconds, 2)

$targetPath = Join-Path $OutputDir "WHITE_PAPER_45_SCENARIOS_HISTORICAL_BENCHMARK.md"
if (Test-Path $targetPath) {
    $bytes = (Get-Item $targetPath).Length
    $kb = [math]::Round($bytes / 1024, 1)
    Write-Host "================================================================================" -ForegroundColor Green
    Write-Host " [SUCCESS] WHITE PAPER GENERATED IN $elapsedSeconds SECONDS!" -ForegroundColor Green
    Write-Host "   File: $targetPath ($kb KB)" -ForegroundColor Green
    Write-Host "================================================================================" -ForegroundColor Green
} else {
    Write-Error "Failed to generate White Paper at expected location: $targetPath"
}
