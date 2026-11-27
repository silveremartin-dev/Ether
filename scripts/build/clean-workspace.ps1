<#
.SYNOPSIS
    Cleans temporary artifacts, logs, saves, scratch directories, and build outputs in Ether.

.DESCRIPTION
    Safely purges:
    - Orphan UUID/test saves in saves/ (or all saves when -PurgeAllSaves is specified).
    - Temporary scratch/ directory contents and __pycache__ folders.
    - Rotated logs in logs/ (or all logs when -PurgeAllLogs is specified).
    - Export artifacts in exports/ and transient test reports in target/reports/.
    - Distribution staging/packages in dist/ (when -CleanDist is specified).
    - Maven target/ build directory (when -CleanTarget is specified).
#>

[CmdletBinding()]
param(
    [switch]$All,
    [switch]$CleanSaves,
    [switch]$PurgeAllSaves,
    [switch]$CleanScratch,
    [switch]$CleanLogs,
    [switch]$PurgeAllLogs,
    [switch]$CleanExports,
    [switch]$CleanDist,
    [switch]$CleanTarget,
    [switch]$DryRun
)

$ErrorActionPreference = "Continue"
$rootDir = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   Ether Scientific Platform - Workspace Cleanup Utility  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Workspace Root: $rootDir"
if ($DryRun) {
    Write-Host "[DRY RUN MODE ENABLED - No files will be deleted]" -ForegroundColor Yellow
}

$doSaves = $All -or $CleanSaves -or $PurgeAllSaves
$doScratch = $All -or $CleanScratch
$doLogs = $All -or $CleanLogs -or $PurgeAllLogs
$doExports = $All -or $CleanExports
$doDist = $CleanDist
$doTarget = $CleanTarget

if (-not ($doSaves -or $doScratch -or $doLogs -or $doExports -or $doDist -or $doTarget)) {
    $doSaves = $true
    $doScratch = $true
    $doLogs = $true
    $doExports = $true
}

function Remove-SafeItem {
    param([string]$Path, [string]$Description)
    if (Test-Path $Path) {
        if ($DryRun) {
            Write-Host "  [WOULD DELETE] $($Description): $Path" -ForegroundColor DarkYellow
        } else {
            try {
                Remove-Item -Path $Path -Recurse -Force -ErrorAction Stop
                Write-Host "  [DELETED] $($Description): $Path" -ForegroundColor Green
            } catch {
                Write-Host "  [ERROR] Failed to delete $($Path): $_" -ForegroundColor Red
            }
        }
    }
}

# 1. Clean Saves
if ($doSaves) {
    Write-Host "`n[1/5] Inspecting saves/ directory..." -ForegroundColor White
    $savesDir = Join-Path $rootDir "saves"
    if (Test-Path $savesDir) {
        $items = Get-ChildItem -Path $savesDir -Force
        $deletedCount = 0
        foreach ($d in $items) {
            $isUuid = $d.Name -match '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            $isTestSave = $d.Name -like 'Test_Save_*' -or $d.Name -like 'AUTOSAVE_*' -or $d.Name -eq 'Autosave_Exit'
            if ($PurgeAllSaves -or $isUuid -or $isTestSave) {
                Remove-SafeItem -Path $d.FullName -Description "Save artifact"
                $deletedCount++
            }
        }
        Write-Host "  Evaluated $($items.Count) entries in saves/. Cleaned $deletedCount entries." -ForegroundColor Gray
    }
}

# 2. Clean Scratch & Stray Root Artifacts
if ($doScratch) {
    Write-Host "`n[2/5] Inspecting scratch/ and temporary root files..." -ForegroundColor White
    Remove-SafeItem -Path (Join-Path $rootDir "scratch") -Description "Scratch directory"
    Remove-SafeItem -Path (Join-Path $rootDir "dependency-reduced-pom.xml") -Description "Maven shade POM"
    Get-ChildItem -Path $rootDir -Directory -Recurse -Filter "__pycache__" -ErrorAction SilentlyContinue | ForEach-Object {
        Remove-SafeItem -Path $_.FullName -Description "Python cache"
    }
}

# 3. Clean Logs
if ($doLogs) {
    Write-Host "`n[3/5] Inspecting logs/ directory..." -ForegroundColor White
    $logsDir = Join-Path $rootDir "logs"
    if (Test-Path $logsDir) {
        if ($PurgeAllLogs) {
            Get-ChildItem -Path $logsDir -Force | ForEach-Object {
                Remove-SafeItem -Path $_.FullName -Description "Log file/directory"
            }
        } else {
            Get-ChildItem -Path $logsDir -Recurse -File | Where-Object { $_.Name -ne "society-simulation.log" } | ForEach-Object {
                Remove-SafeItem -Path $_.FullName -Description "Rotated/archived log"
            }
        }
    }
}

# 4. Clean Exports & Transient Reports
if ($doExports) {
    Write-Host "`n[4/5] Inspecting exports/ and target/reports/..." -ForegroundColor White
    $exportsDir = Join-Path $rootDir "exports"
    if (Test-Path $exportsDir) {
        Get-ChildItem -Path $exportsDir -File | ForEach-Object {
            Remove-SafeItem -Path $_.FullName -Description "Export artifact"
        }
    }
    Remove-SafeItem -Path (Join-Path $rootDir "target\reports") -Description "Transient test reports"
}

# 5. Clean Dist & Target
$distDir = Join-Path $rootDir "dist"
if (Test-Path $distDir) {
    Get-ChildItem -Path $distDir -Directory | Where-Object { $_.Name -like "*-standalone*" } | ForEach-Object {
        Remove-SafeItem -Path $_.FullName -Description "Unzipped dist staging directory"
    }
    if ($doDist) {
        Get-ChildItem -Path $distDir -File | ForEach-Object {
            Remove-SafeItem -Path $_.FullName -Description "Distribution archive"
        }
    }
}

if ($doTarget) {
    Remove-SafeItem -Path (Join-Path $rootDir "target") -Description "Maven target directory"
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "   Cleanup Complete! Workspace is sanitized and healthy.   " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
