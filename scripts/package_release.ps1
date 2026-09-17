<#
.SYNOPSIS
    Packages Ether into a self-contained, standalone distribution archive.
.DESCRIPTION
    Builds the executable shaded fat JAR with JavaFX runtime support, gathers documentation,
    maps, presets, icons, and launcher scripts into a clean dist/ directory, and creates
    a portable ZIP archive ready for immediate end-user download and 1-click execution.
#>

[CmdletBinding()]
param(
    [string]$Version = "2.0.0",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$DistDir = Join-Path $RootDir "dist"
$PackageName = "Ether-v$Version-standalone"
$TargetDir = Join-Path $DistDir $PackageName

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " 🚀 Packaging Ether Planetary Simulation v$Version" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Build Fat JAR if not skipped
if (-not $SkipBuild) {
    Write-Host "[1/5] Building shaded executable JAR via Maven..." -ForegroundColor Yellow
    Push-Location $RootDir
    try {
        & mvn clean package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Host "[1/5] Skipping Maven build as requested (-SkipBuild)..." -ForegroundColor Gray
}

# 2. Prepare Distribution Directory
Write-Host "[2/5] Preparing output directory: $TargetDir..." -ForegroundColor Yellow
if (Test-Path $TargetDir) {
    Remove-Item -Recurse -Force $TargetDir
}
New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $TargetDir "bin") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $TargetDir "data") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $TargetDir "docs") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $TargetDir "saves") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $TargetDir "logs") -Force | Out-Null

# 3. Copy Executable JAR
Write-Host "[3/5] Copying application binaries and resources..." -ForegroundColor Yellow
$JarSource = Join-Path $RootDir "target\society-simulation-2.0.0-SNAPSHOT-executable.jar"
if (-not (Test-Path $JarSource)) {
    # Fallback to any executable jar in target
    $JarSource = Get-ChildItem (Join-Path $RootDir "target") -Filter "*executable.jar" | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $JarSource -or -not (Test-Path $JarSource)) {
    throw "Executable JAR not found in target/. Please run without -SkipBuild."
}
Copy-Item $JarSource (Join-Path $TargetDir "bin\ether.jar")

# Copy Data & Maps
if (Test-Path (Join-Path $RootDir "data")) {
    Copy-Item -Recurse (Join-Path $RootDir "data\*") (Join-Path $TargetDir "data\") -ErrorAction SilentlyContinue
}

# Copy Documentation
$DocFiles = @(
    "README.md",
    "LICENSE",
    "AGENT.md",
    "docs\SIMULATION_EQUATIONS_AND_VARIABLES.md",
    "docs\ARCHITECTURE.md",
    "docs\CREDITS.md",
    "docs\SETUP.md",
    "docs\DEPLOYMENT.md"
)
foreach ($doc in $DocFiles) {
    $srcDoc = Join-Path $RootDir $doc
    if (Test-Path $srcDoc) {
        $destDoc = Join-Path $TargetDir $doc
        $destParent = Split-Path -Parent $destDoc
        if (-not (Test-Path $destParent)) { New-Item -ItemType Directory -Path $destParent -Force | Out-Null }
        Copy-Item $srcDoc $destDoc
    }
}

# 4. Generate Instant Standalone Launchers in Root of Distribution
Write-Host "[4/5] Generating standalone launcher scripts..." -ForegroundColor Yellow

# Windows Batch Launcher (run.bat)
$RunBatContent = @"
@echo off
setlocal
cd /d "%~dp0"
title Ether Planetary Simulation v$Version

echo ============================================================
echo   ETHER - Planetary Cliodynamics Simulation Engine v$Version
echo ============================================================
echo.

:: Check Java 21+
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java runtime not found in PATH.
    echo Please install OpenJDK 21 or higher: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo [INFO] Launching Ether in Standalone Mode...
java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar bin\ether.jar %*
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [INFO] Retrying with fallback JVM parameters...
    java -Xmx4g -jar bin\ether.jar %*
)
endlocal
"@
Set-Content -Path (Join-Path $TargetDir "run.bat") -Value $RunBatContent -Encoding ASCII

# Windows PowerShell Launcher (run.ps1)
$RunPs1Content = @"
# Ether Planetary Simulation Launcher
`$Host.UI.RawUI.WindowTitle = "Ether Planetary Simulation v$Version"
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ETHER - Planetary Cliodynamics Simulation Engine v$Version" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

`$JavaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not `$JavaCmd) {
    Write-Host "[ERROR] Java runtime not found in PATH." -ForegroundColor Red
    Write-Host "Please install OpenJDK 21 or higher from: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit..."
    exit 1
}

`$JarPath = Join-Path `$PSScriptRoot "bin\ether.jar"
`$JvmArgs = @(
    "--add-modules=jdk.incubator.vector",
    "--enable-native-access=ALL-UNNAMED",
    "-Xmx4g",
    "-jar",
    "`$JarPath"
) + `$args

Write-Host "[INFO] Starting Ether simulation engine..." -ForegroundColor Green
& java @JvmArgs
"@
Set-Content -Path (Join-Path $TargetDir "run.ps1") -Value $RunPs1Content -Encoding UTF8

# Linux/macOS Shell Launcher (run.sh)
$RunShContent = @"
#!/usr/bin/env bash
set -e
DIR="`$( cd "`$( dirname "`${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "`$DIR"

echo "============================================================"
echo "  ETHER - Planetary Cliodynamics Simulation Engine v$Version"
echo "============================================================"
echo ""

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java runtime not found in PATH."
    echo "Please install OpenJDK 21 or higher (e.g., sudo apt install openjdk-21-jre or brew install openjdk@21)."
    exit 1
fi

echo "[INFO] Starting Ether simulation engine..."
java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xmx4g -jar bin/ether.jar "`$@" || \
java -Xmx4g -jar bin/ether.jar "`$@"
"@
Set-Content -Path (Join-Path $TargetDir "run.sh") -Value ($RunShContent -replace "`r`n", "`n") -Encoding UTF8

# Installer / Quick Setup Script (install.bat)
$InstallBatContent = @"
@echo off
setlocal
cd /d "%~dp0"
echo ============================================================
echo   Ether Instant Setup & Environment Verification
echo ============================================================
echo.

where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [!] Java 21+ was not detected on your system.
    echo Opening OpenJDK download page in browser...
    start https://adoptium.net/
    echo.
    echo Please install JDK 21+, then double-click 'run.bat' to launch Ether.
    echo.
    pause
    exit /b 1
)

echo [OK] Java detected successfully:
java -version
echo.
echo [OK] Ether is ready for instant autonomous execution!
echo Launching Ether now...
echo.
call run.bat
"@
Set-Content -Path (Join-Path $TargetDir "install.bat") -Value $InstallBatContent -Encoding ASCII

# 5. Create Standalone ZIP Archive
Write-Host "[5/5] Creating portable distribution archive ($PackageName.zip)..." -ForegroundColor Yellow
$ZipPath = Join-Path $DistDir "$PackageName.zip"
if (Test-Path $ZipPath) { Remove-Item -Force $ZipPath }
Compress-Archive -Path "$TargetDir\*" -DestinationPath $ZipPath

$ZipSizeMB = [math]::Round(((Get-Item $ZipPath).Length / 1MB), 2)
Write-Host "============================================================" -ForegroundColor Green
Write-Host " Package created successfully!" -ForegroundColor Green
Write-Host " Distribution Folder: $TargetDir" -ForegroundColor Cyan
Write-Host " Distribution Archive: $ZipPath ($($ZipSizeMB) MB)" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Green
