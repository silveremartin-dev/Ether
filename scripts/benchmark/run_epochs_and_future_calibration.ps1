# ==============================================================================
# Ether 1.0 - 9 Continuous Macro-Epochs (-100,000 BP -> 2026 CE)
#             & Future Biophysical Calibration (2025 -> 2200 CE) Runner
#
# Executes:
#   1. All 9 Continuous Gapless Macro-Historical Epochs (EPOCH_1 .. EPOCH_9)
#      in their predefined H3 Res 4 configuration -> saves/RUN-EPOCH_* &
#      target/reports/SCIENTIFIC_CALIBRATION_REPORT.md
#   2. Flagship Future Earth Calibration (ETHER_CALIBRATION_2025_2200,
#      2025 -> 2200 CE, H3 Res 4, dt = 30.0 days) -> saves/RUN-ETHER-* &
#      data/variables/projections/ether_calibrated_projection_1d.json
# ==============================================================================
param(
    [switch]$SkipEpochs = $false,
    [switch]$SkipFutureCalibration = $false,
    [int]$FutureEndYear = 2200
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "🌍 ETHER: 9 CONTINUOUS EPOCHS (-100,000 BP -> 2026 CE) & FUTURE CALIBRATION (2025 -> $FutureEndYear CE)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

# Ensure project classes are compiled
Write-Host ">>> [0/2] Compiling production classes..." -ForegroundColor Yellow
mvn test-compile "-Djacoco.skip=true" -B
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven compilation failed."
    exit $LASTEXITCODE
}

$env:MAVEN_OPTS = "-Xms4g -Xmx12g --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED"

if (-not $SkipEpochs) {
    Write-Host "`n>>> [1/2] Executing All 9 Continuous Macro-Historical Epochs (EPOCH_1 .. EPOCH_9)..." -ForegroundColor Yellow
    mvn exec:java "-Dexec.mainClass=org.ether.society.analytics.HistoricalScenarioCalibrationHarness" "-Dexec.args=--epochs-only" -B
    if ($LASTEXITCODE -ne 0) {
        Write-Error "9-Epoch execution failed."
        exit $LASTEXITCODE
    }
}

if (-not $SkipFutureCalibration) {
    Write-Host "`n>>> [2/2] Executing Future Earth Biophysical Calibration (2025 -> $FutureEndYear CE, H3 Res 4, dt = 30d)..." -ForegroundColor Yellow
    mvn exec:java "-Dexec.mainClass=org.ether.society.analytics.EtherFutureCalibrationRunner" "-Dfuture.calibration.endYear=$FutureEndYear" -B
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Future calibration execution failed."
        exit $LASTEXITCODE
    }
}

Write-Host "`n================================================================================" -ForegroundColor Green
Write-Host "✅ 9 CONTINUOUS EPOCHS & FUTURE CALIBRATION COMPLETED SUCCESSFULLY" -ForegroundColor Green
Write-Host "   - Epoch Saves       : saves/RUN-EPOCH_1_PALEOLITHIC_DISPERSAL .. RUN-EPOCH_9_INDUSTRIAL_ANTHROPOCENE" -ForegroundColor Green
Write-Host "   - Calibration Report: target/reports/SCIENTIFIC_CALIBRATION_REPORT.md" -ForegroundColor Green
Write-Host "   - 1D Future Baseline: data/variables/projections/ether_calibrated_projection_1d.json" -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Green
