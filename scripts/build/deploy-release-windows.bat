@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0..\.."

echo ============================================================
echo   ETHER -- WINDOWS RELEASE PACKAGER (ZIP + SHA256)
echo ============================================================

set VERSION=%~1
if "%VERSION%"=="" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "& { [xml]$pom = Get-Content pom.xml; Write-Output $pom.project.version }" > "%TEMP%\ether_ver.tmp" 2>nul
    set /p VERSION=<"%TEMP%\ether_ver.tmp"
    del "%TEMP%\ether_ver.tmp" 2>nul
)
if "%VERSION%"=="" set VERSION=1.0.0-beta.2

echo [INFO] Target Version: v%VERSION%

where powershell >nul 2>&1
if %ERRORLEVEL% equ 0 (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0package-release.ps1" -Version "%VERSION%" -Platform "windows"
) else (
    echo [ERROR] PowerShell is required to build release distribution.
    exit /b 1
)

endlocal
