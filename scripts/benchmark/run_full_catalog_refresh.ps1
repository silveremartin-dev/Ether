# Full Catalog Refresh & Benchmark Suite Runner
param(
    [string]$Stages = "1,2,3,4",
    [switch]$DeduplicateOnly = $false,
    [switch]$SkipUpload = $false
)

$ErrorActionPreference = "Continue"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "🚀 FULL CATALOG REFRESH & ASYNCHRONOUS SNAPSHOT STREAMING RUNNER" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

if (-not $DeduplicateOnly) {
    # Stage 1: Phase 1 remaining scenarios (Islam 632, Carolingian 800, Song 1000, Fertile Crescent, LGM Solutrean)
    if ($Stages -match "1") {
        Write-Host "`n>>> [Stage 1/4] Running Phase 1 Remaining Historical Epochs..." -ForegroundColor Yellow
        mvn test -Pacademic-paper -Dtest=MacroHistoricalEpochsFalsificationTest "-Dphase1.scenario=islamic_expansion_632,carolingian_800,song_dynasty,fertile_crescent,lgm_solutrean" -DtestFailureIgnore=true -B
    }

    # Stage 2: Phase 2 remaining scenarios (Pax Romana, Ramesses, Columbian, Tokugawa, 1950, Out of Africa)
    if ($Stages -match "2") {
        Write-Host "`n>>> [Stage 2/4] Running Phase 2 Remaining Historical Epochs..." -ForegroundColor Yellow
        mvn test -Pacademic-paper -Dtest=MacroHistoricalPhase2FalsificationTest "-Dphase2.scenario=pax_romana_augustus_0,ramesses_ii,columbian_contact,tokugawa_japan,great_acceleration_1950,out_of_africa" -DtestFailureIgnore=true -B
    }

    # Stage 3: Phase 3 remaining scenarios (Deep Prehistory & Classical Hegemonies)
    if ($Stages -match "3") {
        Write-Host "`n>>> [Stage 3/4] Running Phase 3 Remaining Historical Scenarios..." -ForegroundColor Yellow
        mvn test -Pacademic-paper -Dtest=MacroHistoricalPhase3FalsificationTest "-Dphase3.scenario=sahul,beringia,green_sahara,ancient_egypt,assyrian_empire,mesoamerica,early_iron_age,alexander_hellenistic_334bc,maurya_empire,mali_empire,americas_1491,anthropocene_2000" -DtestFailureIgnore=true -B
    }

    # Stage 4: Future Earth & Exoplanet Scenarios (17 scenarios)
    if ($Stages -match "4") {
        Write-Host "`n>>> [Stage 4/4] Running Future Earth & Exoplanet Scenarios..." -ForegroundColor Yellow
        mvn test -Pacademic-paper -Dtest=MacroFutureAndExoFalsificationTest -DtestFailureIgnore=true -B
    }
}

# Deduplicate saves: keep only the newest run for each scenario preset
Write-Host "`n>>> Cleaning up redundant save runs in saves/..." -ForegroundColor Yellow
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

# Recompile Master White Paper
Write-Host "`n>>> Recompiling 45-Scenario Master White Paper..." -ForegroundColor Yellow
mvn test -Dtest=WhitePaperCompiler -DfailIfNoTests=false -B

# Package and Upload to GitHub Release
if (-not $SkipUpload) {
    Write-Host "`n>>> Packaging updated saves and uploading to GitHub Release..." -ForegroundColor Yellow
    powershell -ExecutionPolicy Bypass -File scripts/release/package-and-upload-saves.ps1 -Upload
}

Write-Host "`n================================================================================" -ForegroundColor Cyan
Write-Host "✔ FULL CATALOG REFRESH & GITHUB RELEASE COMPLETED SUCCESSFULLY" -ForegroundColor Green
Write-Host "================================================================================" -ForegroundColor Cyan
