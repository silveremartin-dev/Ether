# ==============================================================================
# Ether 1.0 - Master White Paper & Academic Manuscript Compiler
# Recompiles charts, WHITE_PAPER_ALL_SCENARIOS_HISTORICAL_BENCHMARK.md, and
# refreshes the empirical synthesis in ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md.
# ==============================================================================

param (
    [string]$OutputDir = "docs",
    [switch]$WithCalibrationAndFreeRuns = $false
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " [ETHER] MASTER WHITE PAPER & ACADEMIC MANUSCRIPT COMPILER                      " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  White Paper Output : $OutputDir/WHITE_PAPER_ALL_SCENARIOS_HISTORICAL_BENCHMARK.md"
Write-Host "  Academic Paper     : $OutputDir/paper/ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md"
Write-Host "--------------------------------------------------------------------------------"

$sw = [System.Diagnostics.Stopwatch]::StartNew()

if ($WithCalibrationAndFreeRuns) {
    Write-Host "[1/2] Executing Calibration, Counterfactuals, Free-Runs & WhitePaperCompiler..." -ForegroundColor Yellow
    mvn test -Pacademic-paper "-Dtest=HistoricalScenarioCalibrationHarnessTest,CounterfactualInverseFalsificationTest,LongHorizonFreeRunFalsificationTest,WhitePaperCompiler#compileMasterWhitePaper" "-Djacoco.skip=true" -B
} else {
    Write-Host "[1/2] Invoking WhitePaperCompiler harness from persisted saves/..." -ForegroundColor Yellow
    mvn test "-Dtest=WhitePaperCompiler#compileMasterWhitePaper" "-Djacoco.skip=true" -B
}
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven WhitePaperCompiler execution failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

$sw.Stop()
$elapsedSeconds = [math]::Round($sw.Elapsed.TotalSeconds, 2)

$targetPath = Join-Path $OutputDir "WHITE_PAPER_ALL_SCENARIOS_HISTORICAL_BENCHMARK.md"
$academicPath = Join-Path $OutputDir "paper/ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md"
if ((Test-Path $targetPath) -and (Test-Path $academicPath)) {
    $kbWp = [math]::Round((Get-Item $targetPath).Length / 1024, 1)
    $kbAp = [math]::Round((Get-Item $academicPath).Length / 1024, 1)
    Write-Host "================================================================================" -ForegroundColor Green
    Write-Host " [SUCCESS] SCIENTIFIC DOCUMENTS UPDATED IN $elapsedSeconds SECONDS!" -ForegroundColor Green
    Write-Host "   White Paper    : $targetPath ($kbWp KB)" -ForegroundColor Green
    Write-Host "   Academic Paper : $academicPath ($kbAp KB)" -ForegroundColor Green
    Write-Host "================================================================================" -ForegroundColor Green
} else {
    Write-Error "Failed to locate generated White Paper ($targetPath) or Academic Paper ($academicPath)"
}
