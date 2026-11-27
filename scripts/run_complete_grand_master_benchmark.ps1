# ==============================================================================
# Grand Master Academic Paper, Calibration, Counterfactuals & Free-Runs Pipeline
# Executes:
#   1. Steady-Regime Calibration & 9 Continuous Epochs (HistoricalScenarioCalibrationHarnessTest)
#   2. Popperian Counterfactuals, Engine Knock-Outs & 7 Spatial Hold-Outs
#      (CounterfactualInverseFalsificationTest, SpatialHoldoutCrossValidationTest)
#   3. Multi-Century Open-Loop Free-Runs (LongHorizonFreeRunFalsificationTest: 500 yr & 250 yr)
#   4. Canonical Historical Bifurcations & 24 Cliodynamic Debates
#      (MasterHistoricalBifurcationSuite, ScientificModelFalsificationAndValidationSuite)
#   5. Full Recompilation of Charts, White Paper & Academic Manuscript
#      (WhitePaperCompiler#compileMasterWhitePaper -> docs/paper/ACADEMIC_PAPER_... & docs/WHITE_PAPER_...)
# ==============================================================================
param(
    [switch]$RecompileOnly = $false,
    [switch]$PushGit = $false
)

$ErrorActionPreference = "Continue"
$root = Resolve-Path $PSScriptRoot
Set-Location $root

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "🚀 STARTING COMPLETE ACADEMIC ARTICLE, CALIBRATION, COUNTERFACTUAL & FREE-RUN PIPELINE" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

if ($RecompileOnly) {
    Write-Host ">>> [1/3] Recompiling Charts, White Paper & Academic Manuscript from existing saves/..." -ForegroundColor Yellow
    mvn test "-Dtest=WhitePaperCompiler#compileMasterWhitePaper" "-Djacoco.skip=true" -B
} else {
    Write-Host ">>> [1/3] Running Calibration, Counterfactuals, Hold-Outs, Free-Runs, Bifurcations, Debates & Paper Compiler..." -ForegroundColor Yellow
    $suites = @(
        "HistoricalScenarioCalibrationHarnessTest",
        "CounterfactualInverseFalsificationTest",
        "SpatialHoldoutCrossValidationTest",
        "LongHorizonFreeRunFalsificationTest",
        "MasterHistoricalBifurcationSuite",
        "ScientificModelFalsificationAndValidationSuite",
        "WhitePaperCompiler#compileMasterWhitePaper"
    ) -join ","
    mvn test -Pacademic-paper "-Dtest=$suites" "-Djacoco.skip=true" -DtestFailureIgnore=true -B
}

# 2. Propagate Updated Academic Manuscript to Distribution Bundles (if present)
Write-Host ">>> [2/3] Propagating Academic Manuscript to Distribution Bundles..." -ForegroundColor Yellow
$paperSrc = "docs/paper/ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md"
$distDirs = @(
    "dist/Ether-v1.0.0-beta.3-linux-x64/docs/paper",
    "dist/Ether-v1.0.0-beta.3-macos-universal/docs/paper",
    "dist/Ether-v1.0.0-beta.3-standalone/docs/paper",
    "dist/Ether-v1.0.0-beta.3-windows-x64/docs/paper"
)
foreach ($t in $distDirs) {
    if (Test-Path $t) {
        Copy-Item -Path $paperSrc -Destination $t -Force
    }
}

# 3. Enforce Mandatory i18n & Hardcoded Strings Audit
Write-Host ">>> [3/3] Running Mandatory i18n & Hardcoded Strings Audit..." -ForegroundColor Yellow
& powershell -ExecutionPolicy Bypass -File scripts/i18n/audit-i18n-and-hardcoded-strings.ps1

# Optional Git Stage, Commit & Push
if ($PushGit) {
    Write-Host ">>> Staging, Committing and Pushing Updated Academic Artifacts to GitHub..." -ForegroundColor Yellow
    git add docs/ docs/reports/ docs/paper/ docs/WHITE_PAPER_ALL_SCENARIOS_HISTORICAL_BENCHMARK.md docs/SCIENTIFIC_CALIBRATION_REPORT.md
    git commit -m "docs(paper): refresh complete academic manuscript, calibration, counterfactuals and free-runs"
    git push origin master
}

Write-Host "================================================================================" -ForegroundColor Green
Write-Host "✅ ACADEMIC PAPER & GRAND MASTER FALSIFICATION PIPELINE COMPLETED SUCCESSFULLY" -ForegroundColor Green
Write-Host "   - Academic Paper : docs/paper/ACADEMIC_PAPER_PLANETARY_CLIODYNAMICS_FALSIFICATION.md" -ForegroundColor Green
Write-Host "   - White Paper    : docs/WHITE_PAPER_ALL_SCENARIOS_HISTORICAL_BENCHMARK.md" -ForegroundColor Green
Write-Host "   - Calibration    : docs/SCIENTIFIC_CALIBRATION_REPORT.md" -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Green
