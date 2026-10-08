<#
.SYNOPSIS
    Automated multi-platform release packager for Ether Simulation Engine.
.DESCRIPTION
    Builds the fat/shaded JAR and bundles self-contained, multi-OS distribution packages
    (Windows, Linux, macOS, and Universal All-in-One) with launchers and SHA256 checksums.
.PARAMETER Version
    The release version string. If omitted, extracted dynamically from pom.xml.
.PARAMETER Platform
    Target platform: 'all', 'windows', 'linux', 'macos', or 'all-in-one'. Default is 'all'.
.PARAMETER SkipBuild
    If set, skips the 'mvn clean package' step and uses existing target JAR.
#>
[CmdletBinding()]
param(
    [string]$Version,
    [ValidateSet("all", "windows", "linux", "macos", "all-in-one", "standalone")]
    [string]$Platform = "all",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = (Get-Item "$ScriptDir\..\..").FullName
$DistDir = Join-Path $RootDir "dist"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " 🪐 ETHER SIMULATION ENGINE -- MULTI-OS RELEASE PACKAGER    " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Resolve Version from pom.xml if not provided
if (-not $Version) {
    $PomPath = Join-Path $RootDir "pom.xml"
    if (Test-Path $PomPath) {
        [xml]$PomXml = Get-Content $PomPath
        $Version = $PomXml.project.version
    }
    if (-not $Version) {
        $Version = "1.0.0-beta.2"
    }
}
Write-Host "[INFO] Target Version  : v$Version" -ForegroundColor Green
Write-Host "[INFO] Target Platform : $Platform" -ForegroundColor Green
Write-Host "[INFO] Output Directory: $DistDir" -ForegroundColor Green

# 2. Build via Maven if requested
Set-Location $RootDir
if (-not $SkipBuild) {
    Write-Host "`n[1/4] Building shaded executable JAR via Maven..." -ForegroundColor Yellow
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed with exit code $LASTEXITCODE"
    }
} else {
    Write-Host "`n[1/4] Skipping Maven build (using existing target artifacts)..." -ForegroundColor Yellow
}

# 3. Locate generated executable JAR
$TargetJars = Get-ChildItem -Path (Join-Path $RootDir "target") -Filter "*executable.jar" -File
if ($TargetJars.Count -eq 0) {
    $TargetJars = Get-ChildItem -Path (Join-Path $RootDir "target") -Filter "society-simulation-*.jar" -File | Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" }
}
if ($TargetJars.Count -eq 0) {
    Write-Error "Could not find built JAR artifact in $RootDir\target. Please run without -SkipBuild."
}
$JarSource = $TargetJars[0].FullName
Write-Host "[INFO] Using JAR artifact: $($TargetJars[0].Name)" -ForegroundColor Green

# 4. Prepare Output Directories
if (-not (Test-Path $DistDir)) {
    New-Item -ItemType Directory -Path $DistDir -Force | Out-Null
}

function Copy-RuntimeData {
    param([string]$DestDataDir)

    New-Item -ItemType Directory -Path $DestDataDir -Force | Out-Null

    # Copy official scenarios, presets, events, and chronicles
    @("events", "history", "presets") | ForEach-Object {
        $Src = Join-Path $RootDir "data\$_"
        if (Test-Path $Src) {
            Copy-Item -Path $Src -Destination (Join-Path $DestDataDir $_) -Recurse -Force
        }
    }

    # Copy all planetary maps (Mars, Moon, Venus, Mercury)
    @("mars", "mercury", "moon", "venus") | ForEach-Object {
        $PlanetSrc = Join-Path $RootDir "data\maps\ether\$_"
        if (Test-Path $PlanetSrc) {
            $PlanetDest = Join-Path $DestDataDir "maps\ether\$_"
            New-Item -ItemType Directory -Path $PlanetDest -Force | Out-Null
            Copy-Item -Path "$PlanetSrc\*" -Destination $PlanetDest -Recurse -Force
        }
    }

    # Copy all 36 milestone Earth historical epochs (-100,000 BP to 2060)
    $Milestones = @("-100000", "-74000", "-50000", "-25000", "-20000", "-10900", "-10000", "-8000", "-6000", "-3000", "-1900", "-1500", "-1200", "-1000", "-334", "-300", "0", "536", "632", "1000", "1206", "1324", "1347", "1491", "1492", "1639", "1800", "1900", "1914", "1950", "2000", "2026", "2035", "2045", "2050", "2060")
    $EarthDest = Join-Path $DestDataDir "maps\ether\earth"
    New-Item -ItemType Directory -Path $EarthDest -Force | Out-Null
    foreach ($m in $Milestones) {
        $mSrc = Join-Path $RootDir "data\maps\ether\earth\$m"
        if (Test-Path $mSrc) {
            $mDest = Join-Path $EarthDest $m
            New-Item -ItemType Directory -Path $mDest -Force | Out-Null
            Copy-Item -Path "$mSrc\*" -Destination $mDest -Recurse -Force
        }
    }

    # Copy maps metadata and download status
    @("download_status.json", "README.md") | ForEach-Object {
        $MetaSrc = Join-Path $RootDir "data\maps\$_"
        if (Test-Path $MetaSrc) {
            $MapsDest = Join-Path $DestDataDir "maps"
            New-Item -ItemType Directory -Path $MapsDest -Force | Out-Null
            Copy-Item -Path $MetaSrc -Destination $MapsDest -Force
        }
    }
}

function Build-Package {
    param(
        [string]$PkgName,
        [string[]]$Launchers,
        [string[]]$Formats
    )

    $PkgDir = Join-Path $DistDir $PkgName
    Write-Host "`n[2/4] Assembling directory structure for $PkgName..." -ForegroundColor Yellow
    if (Test-Path $PkgDir) { Remove-Item -Path $PkgDir -Recurse -Force }
    
    New-Item -ItemType Directory -Path (Join-Path $PkgDir "bin") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $PkgDir "docs") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $PkgDir "saves") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $PkgDir "logs") -Force | Out-Null

    # Copy JAR
    Copy-Item -Path $JarSource -Destination (Join-Path $PkgDir "bin\ether.jar") -Force

    # Copy Filtered Runtime Data
    Copy-RuntimeData -DestDataDir (Join-Path $PkgDir "data")

    # Copy Docs
    if (Test-Path (Join-Path $RootDir "docs")) {
        Copy-Item -Path (Join-Path $RootDir "docs\*") -Destination (Join-Path $PkgDir "docs") -Recurse -Force
    }

    # Copy Project Meta
    @("README.md", "LICENSE", "AGENTS.md", ".env.example") | ForEach-Object {
        $Src = Join-Path $RootDir $_
        if (Test-Path $Src) { Copy-Item -Path $Src -Destination $PkgDir -Force }
    }

    # Generate OS Launchers
    if ($Launchers -contains "windows") {
        $WinLauncher = @"
@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
title Ether Planetary Simulation v$Version

echo ============================================================
echo   ETHER - Planetary Cliodynamics Simulation Engine v$Version
echo ============================================================
echo.

where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [!] Java 21+ is not installed or not in your system PATH.
    echo Opening OpenJDK 21 download page in your browser...
    start https://adoptium.net/temurin/releases/?version=21
    pause
    exit /b 1
)

java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xms2g -Xmx12g -XX:+UseG1GC -jar bin\ether.jar %*
if %ERRORLEVEL% NEQ 0 (
    java -Xms2g -Xmx8g -jar bin\ether.jar %*
)
endlocal
"@
        Set-Content -Path (Join-Path $PkgDir "run.bat") -Value $WinLauncher -Encoding ASCII
    }

    if ($Launchers -contains "linux" -or $Launchers -contains "macos") {
        $UnixLauncher = @'
#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "============================================================"
echo "  ETHER - Planetary Cliodynamics Simulation Engine"
echo "============================================================"
echo ""

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java 21+ is required but not found in PATH."
    echo "Please install OpenJDK 21 or higher (e.g. sudo apt install openjdk-21-jre or brew install openjdk@21)."
    exit 1
fi

java --add-modules=jdk.incubator.vector --enable-native-access=ALL-UNNAMED -Xms2g -Xmx12g -XX:+UseG1GC -jar bin/ether.jar "$@" || \
java -Xms2g -Xmx8g -jar bin/ether.jar "$@"
'@
        $UnixLauncherPath = Join-Path $PkgDir "run.sh"
        [System.IO.File]::WriteAllText($UnixLauncherPath, $UnixLauncher.Replace("`r`n", "`n"))
    }

    if ($Launchers -contains "macos") {
        $MacCommand = @'
#!/usr/bin/env bash
set -e
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"
exec ./run.sh "$@"
'@
        $MacCommandPath = Join-Path $PkgDir "run.command"
        [System.IO.File]::WriteAllText($MacCommandPath, $MacCommand.Replace("`r`n", "`n"))
    }

    # 4. Create Archives and Hashes via native tar / bsdtar
    Write-Host "[3/4] Creating distribution archives for $PkgName..." -ForegroundColor Yellow
    foreach ($fmt in $Formats) {
        if ($fmt -eq "zip") {
            $ZipPath = Join-Path $DistDir "$PkgName.zip"
            if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }
            where.exe tar >$null 2>&1
            if ($LASTEXITCODE -eq 0) {
                & tar.exe -a -c -f "$ZipPath" -C "$DistDir" "$PkgName"
            } else {
                Add-Type -AssemblyName System.IO.Compression.FileSystem
                [System.IO.Compression.ZipFile]::CreateFromDirectory($PkgDir, $ZipPath, [System.IO.Compression.CompressionLevel]::Fastest, $false)
            }
            $Hash = (Get-FileHash -Path $ZipPath -Algorithm SHA256).Hash
            Set-Content -Path "$ZipPath.sha256" -Value "$Hash  $PkgName.zip"
            Write-Host "  -> Created: $PkgName.zip ($Hash)" -ForegroundColor Green
        }
        if ($fmt -eq "tar.gz") {
            $TarPath = Join-Path $DistDir "$PkgName.tar.gz"
            if (Test-Path $TarPath) { Remove-Item $TarPath -Force }
            where.exe tar >$null 2>&1
            if ($LASTEXITCODE -eq 0) {
                & tar.exe -czf "$TarPath" -C "$DistDir" "$PkgName"
                $Hash = (Get-FileHash -Path $TarPath -Algorithm SHA256).Hash
                Set-Content -Path "$TarPath.sha256" -Value "$Hash  $PkgName.tar.gz"
                Write-Host "  -> Created: $PkgName.tar.gz ($Hash)" -ForegroundColor Green
            }
        }
    }
}

# Execute packaging by platform
if ($Platform -eq "windows" -or $Platform -eq "all") {
    Build-Package -PkgName "Ether-v$Version-windows-x64" -Launchers @("windows") -Formats @("zip")
}
if ($Platform -eq "linux" -or $Platform -eq "all") {
    Build-Package -PkgName "Ether-v$Version-linux-x64" -Launchers @("linux") -Formats @("tar.gz", "zip")
}
if ($Platform -eq "macos" -or $Platform -eq "all") {
    Build-Package -PkgName "Ether-v$Version-macos-universal" -Launchers @("linux", "macos") -Formats @("tar.gz", "zip")
}
if ($Platform -eq "standalone" -or $Platform -eq "all-in-one" -or $Platform -eq "all") {
    Build-Package -PkgName "Ether-v$Version-standalone" -Launchers @("windows", "linux", "macos") -Formats @("zip", "tar.gz")
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host " 🎉 RELEASE PACKAGING COMPLETED SUCCESSFULLY!" -ForegroundColor Cyan
Write-Host " Output artifacts stored in: $DistDir" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Get-ChildItem -Path $DistDir -Filter "Ether-v*" | Where-Object { -not $_.PSIsContainer } | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize
