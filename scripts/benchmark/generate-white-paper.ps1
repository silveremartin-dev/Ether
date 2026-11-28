# ==============================================================================
# Ether 1.0 - Master White Paper & Academic Manuscript Compiler (Windows)
#
# Modes:
#   1. Default (fast recompile from existing saves/):
#      .\scripts\benchmark\generate-white-paper.ps1
#   2. Full Academic Falsification Pipeline (Pillars 4, 5, 6, 7 & 8 + White Paper):
#      .\scripts\benchmark\generate-white-paper.ps1 -WithCalibrationAndFreeRuns
#      (or alias -FullAcademicPipeline)
#
# Included suites in full pipeline mode:
#   - HistoricalScenarioCalibrationHarnessTest       (Steady Regimes, Twins & Detective)
#   - CounterfactualInverseFalsificationTest         (Pillar 4: Negative Controls & Knock-Outs)
#   - MasterHistoricalBifurcationSuite               (Pillar 5: Historical Bifurcations)
#   - LongHorizonFreeRunFalsificationTest            (Pillar 6: 500-yr & 250-yr Free-Runs)
#   - SpatialHoldoutCrossValidationTest              (Pillar 7: 7 Non-Eurasian Hold-Outs)
#   - ScientificModelFalsificationAndValidationSuite (Pillar 6 & 8: Benchmarks & Debates)
#   - WhitePaperCompiler#compileMasterWhitePaper     (Charts, White Paper & Manuscript)
# ==============================================================================

param (
    [string]$OutputDir = "target/reports",
    [Alias("FullAcademicPipeline")]
    [switch]$WithCalibrationAndFreeRuns = $false
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " [ETHER] MASTER WHITE PAPER & ACADEMIC MANUSCRIPT COMPILER                      " -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "  White Paper Output : $OutputDir/WHITE_PAPER_ALL_SCENARIOS_HISTORICAL_BENCHMARK.md"
Write-Host "  Academic Paper     : docs/paper/ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md"
Write-Host "  Full Pipeline Mode : $($WithCalibrationAndFreeRuns.IsPresent)"
Write-Host "--------------------------------------------------------------------------------"

$env:MAVEN_OPTS = "-Xms4g -Xmx12g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"
$sw = [System.Diagnostics.Stopwatch]::StartNew()

if ($WithCalibrationAndFreeRuns) {
    $suites = @(
        "HistoricalScenarioCalibrationHarnessTest",
        "CounterfactualInverseFalsificationTest",
        "MasterHistoricalBifurcationSuite",
        "LongHorizonFreeRunFalsificationTest",
        "SpatialHoldoutCrossValidationTest",
        "ScientificModelFalsificationAndValidationSuite",
        "WhitePaperCompiler#compileMasterWhitePaper"
    ) -join ","
    Write-Host "[1/2] Executing Complete Academic Falsification Suites (Pillars 4-8) & WhitePaperCompiler..." -ForegroundColor Yellow
    mvn test -Pacademic-paper "-Dgroups=" "-Dtest=$suites" "-Djacoco.skip=true" -B
} else {
    Write-Host "[1/2] Invoking WhitePaperCompiler harness from persisted saves/..." -ForegroundColor Yellow
    mvn test "-Dgroups=" "-Dtest=WhitePaperCompiler#compileMasterWhitePaper" "-Djacoco.skip=true" -B
}
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven WhitePaperCompiler execution failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

$sw.Stop()
$elapsedSeconds = [math]::Round($sw.Elapsed.TotalSeconds, 2)

$targetPath = Join-Path $OutputDir "WHITE_PAPER_ALL_SCENARIOS_HISTORICAL_BENCHMARK.md"
$academicPath = "docs/paper/ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md"
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
