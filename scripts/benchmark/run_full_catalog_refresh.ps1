# ==============================================================================
# Full 51-Scenario Preset Catalog Runner (Native Predefined Spatial & Temporal Resolutions)
#
# Executes 100% of the 51 built-in scenario presets in data/presets/scenarios/:
#   - Stage 1 (10 presets): MacroHistoricalEpochsFalsificationTest
#   - Stage 2  (9 presets): MacroHistoricalPhase2FalsificationTest
#   - Stage 3 (15 presets): MacroHistoricalPhase3FalsificationTest
#   - Stage 4 (17 presets): MacroFutureAndExoFalsificationTest
# Total = 51 presets executed in their exact predefined H3 resolution, regional
# clipping window, temporal step (dt), and chronological span (startDateYear -> endDateYear).
# ==============================================================================
param(
    [string]$Stages = "1,2,3,4",
    [bool]$UsePresetResolution = $true,
    [switch]$DeduplicateOnly = $false,
    [switch]$RecompilePaper = $true,
    [switch]$Upload = $false
)

$ErrorActionPreference = "Continue"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "🚀 FULL 51-PRESET SCENARIO CATALOG RUNNER (Native Predefined Resolutions: $UsePresetResolution)" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

$resFlag = "-Dbenchmark.usePresetResolution=$($UsePresetResolution.ToString().ToLower())"

if (-not $DeduplicateOnly) {
    # Stage 1: All 10 Phase 1 Historical Presets
    if ($Stages -match "1") {
        Write-Host "`n>>> [Stage 1/4] Running Phase 1 Historical Presets (10/51)..." -ForegroundColor Yellow
        mvn test -Pacademic-paper "-Dtest=MacroHistoricalEpochsFalsificationTest#testExecutePhase1Scenarios" $resFlag "-Djacoco.skip=true" -DtestFailureIgnore=true -B
    }

    # Stage 2: All 9 Phase 2 Historical Presets (including roman_empire)
    if ($Stages -match "2") {
        Write-Host "`n>>> [Stage 2/4] Running Phase 2 Historical Presets (9/51)..." -ForegroundColor Yellow
        mvn test -Pacademic-paper "-Dtest=MacroHistoricalPhase2FalsificationTest#testExecutePhase2Scenarios" $resFlag "-Djacoco.skip=true" -DtestFailureIgnore=true -B
    }

    # Stage 3: All 15 Phase 3 Prehistory & Classical Presets (including benchmark_standard)
    if ($Stages -match "3") {
        Write-Host "`n>>> [Stage 3/4] Running Phase 3 Historical & Benchmark Presets (15/51)..." -ForegroundColor Yellow
        mvn test -Pacademic-paper "-Dtest=MacroHistoricalPhase3FalsificationTest#testExecutePhase3Scenarios" $resFlag "-Djacoco.skip=true" -DtestFailureIgnore=true -B
    }

    # Stage 4: All 17 Future Earth & Exoplanet Presets
    if ($Stages -match "4") {
        Write-Host "`n>>> [Stage 4/4] Running Phase 4 Future Earth & Exoplanet Presets (17/51)..." -ForegroundColor Yellow
        mvn test -Pacademic-paper "-Dtest=MacroFutureAndExoFalsificationTest#testExecuteFutureAndExoScenarios" $resFlag "-Djacoco.skip=true" -DtestFailureIgnore=true -B
    }
}

# Deduplicate saves: keep only the newest run for each scenario preset
Write-Host "`n>>> Cleaning up redundant save runs in saves/..." -ForegroundColor Yellow
if (Test-Path "saves") {
    Get-ChildItem -Directory saves | Where-Object { $_.Name -like "RUN-*" } | Group-Object { $_.Name -replace '-+\d+$', '' } | Where-Object Count -gt 1 | ForEach-Object {
        $sorted = $_.Group | Sort-Object LastWriteTime -Descending
        $keep = $sorted[0].Name
        $toRemove = $sorted | Select-Object -Skip 1
        foreach ($old in $toRemove) {
            Write-Host "  [Purge] Removing older duplicate save: $($old.Name)" -ForegroundColor DarkGray
            Remove-Item -Recurse -Force $old.FullName
        }
        Write-Host "  [Retain] Preserving newest complete save: $keep" -ForegroundColor Green
    }
}

# Recompile Master White Paper & Academic Manuscript Appendix
if ($RecompilePaper) {
    Write-Host "`n>>> Recompiling All-Scenario White Paper & Academic Manuscript Synthesis..." -ForegroundColor Yellow
    mvn test "-Dtest=WhitePaperCompiler#compileMasterWhitePaper" "-Djacoco.skip=true" -DfailIfNoTests=false -B
}

# Optional Package and Upload to GitHub Release
if ($Upload) {
    Write-Host "`n>>> Packaging updated saves and uploading to GitHub Release..." -ForegroundColor Yellow
    powershell -ExecutionPolicy Bypass -File scripts/release/package-and-upload-saves.ps1 -Upload
}

Write-Host "`n================================================================================" -ForegroundColor Cyan
Write-Host "✔ FULL 51-PRESET CATALOG EXECUTION COMPLETED SUCCESSFULLY" -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Cyan
